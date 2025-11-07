/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.commons.compress.archivers.zip;

import static org.apache.commons.lang3.reflect.FieldUtils.readDeclaredField;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.function.IOConsumer;
import org.apache.commons.lang3.ArrayFill;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.airlift.compress.zstd.ZstdInputStream;

class ZipArchiveInputStreamTest extends AbstractTest {

    private static final class AirliftZipArchiveInputStream extends ZipArchiveInputStream {

        private boolean used;

        private AirliftZipArchiveInputStream(final InputStream inputStream) throws IOException {
            super(inputStream);
        }

        @Override
        protected InputStream createZstdInputStream(final InputStream bis) throws IOException {
            return new ZstdInputStream(bis) {
                @Override
                public int read(final byte[] outputBuffer, final int outputOffset, final int outputLength) throws IOException {
                    used = true;
                    return super.read(outputBuffer, outputOffset, outputLength);
                }
            };
        }

        public boolean isUsed() {
            return used;
        }
    }

    private static void nameSource(final String archive, final String entry, int entryNo, final ZipArchiveEntry.NameSource expected) throws Exception {
        try (ZipArchiveInputStream zis = ZipArchiveInputStream.builder().setURI(getURI(archive)).get()) {
            ZipArchiveEntry ze;
            do {
                ze = zis.getNextZipEntry();
            } while (--entryNo > 0);
            assertEquals(entry, ze.getName());
            assertEquals(expected, ze.getNameSource());
        }
    }

    private static void nameSource(final String archive, final String entry, final ZipArchiveEntry.NameSource expected) throws Exception {
        nameSource(archive, entry, 1, expected);
    }

    private static byte[] readEntry(final ZipArchiveInputStream zip, final ZipArchiveEntry zae) throws IOException {
        final int len = (int) zae.getSize();
        final byte[] buff = new byte[len];
        zip.read(buff, 0, len);

        return buff;
    }

    private void extractZipInputStream(final ZipArchiveInputStream inputStream) throws IOException {
        ZipArchiveEntry zae = inputStream.getNextZipEntry();
        while (zae != null) {
            if (zae.getName().endsWith(".zip")) {
                try (ZipArchiveInputStream innerInputStream = ZipArchiveInputStream.builder()
                        .setInputStream(inputStream)
                        .get()) {
                    extractZipInputStream(innerInputStream);
                }
            }
            zae = inputStream.getNextZipEntry();
        }
    }

    /**
     * Forge a ZIP archive in memory, using STORED and Data Descriptor, and without signature of Data Descriptor.
     *
     * @return the input stream of the generated zip
     * @throws IOException there are problems
     */
    private InputStream forgeZipInputStream() throws IOException {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                ZipArchiveOutputStream zo = new ZipArchiveOutputStream(byteArrayOutputStream)) {

            final ZipArchiveEntry entryA = new ZipArchiveEntry("foo");
            entryA.setMethod(ZipEntry.STORED);
            entryA.setSize(4);
            entryA.setCrc(0xb63cfbcdL);
            // Ensure we won't write extra fields. They are not compatible with the manual edits below.
            entryA.setTime(Instant.parse("2022-12-26T17:01:00Z").toEpochMilli());
            zo.putArchiveEntry(entryA);
            zo.write(new byte[] { 1, 2, 3, 4 });
            zo.closeArchiveEntry();
            zo.close();

            final byte[] zipContent = byteArrayOutputStream.toByteArray();
            final byte[] zipContentWithDataDescriptor = new byte[zipContent.length + 12];
            System.arraycopy(zipContent, 0, zipContentWithDataDescriptor, 0, 37);
            // modify the general purpose bit flag
            zipContentWithDataDescriptor[6] = 8;

            // copy the crc-32, compressed size and uncompressed size to the data descriptor
            System.arraycopy(zipContent, 14, zipContentWithDataDescriptor, 37, 12);

            // and copy the rest of the ZIP content
            System.arraycopy(zipContent, 37, zipContentWithDataDescriptor, 49, zipContent.length - 37);

            return new ByteArrayInputStream(zipContentWithDataDescriptor);
        }
    }

    private void fuzzingTest(final int[] bytes) throws Exception {
        final int len = bytes.length;
        final byte[] input = new byte[len];
        for (int i = 0; i < len; i++) {
            input[i] = (byte) bytes[i];
        }
        try (ArchiveInputStream<?> ais = ArchiveStreamFactory.DEFAULT.createArchiveInputStream("zip", new ByteArrayInputStream(input))) {
            ais.getNextEntry();
            IOUtils.toByteArray(ais);
        }
    }

    private void getAllZipEntries(final ZipArchiveInputStream zipInputStream) throws IOException {
        while (zipInputStream.getNextZipEntry() != null) {
            // noop
        }
    }

    private void multiByteReadConsistentlyReturnsMinusOneAtEof(final File file) throws Exception {
        final byte[] buf = new byte[2];
        try (ZipArchiveInputStream archive = ZipArchiveInputStream.builder().setFile(file).get()) {
            assertEquals(-1, archive.getCompressedCount());
            assertNotNull(archive.getNextEntry());
            IOUtils.toByteArray(archive);
            assertEquals(-1, archive.read(buf));
            assertEquals(-1, archive.read(buf));
        }
    }

    private void singleByteReadConsistentlyReturnsMinusOneAtEof(final File file) throws Exception {
        try (ZipArchiveInputStream archive = ZipArchiveInputStream.builder().setFile(file).get()) {
            assertNotNull(archive.getNextEntry());
            IOUtils.toByteArray(archive);
            assertEquals(-1, archive.read());
            assertEquals(-1, archive.read());
        }
    }

    @Test
    void testDecompressNextSymbol() throws IOException {
        try (ZipArchiveInputStream inputStream = ZipArchiveInputStream.builder()
                .setURI(getURI("org/apache/commons/compress/zip/decompressNextSymbol.bin"))
                .get()) {
            inputStream.getNextEntry();
            assertThrows(IOException.class, inputStream::getNextEntry);
        }
    }

    @Test
    void testGetCompressedCountEmptyZip() throws IOException {
        try (ZipArchiveInputStream zin = ZipArchiveInputStream.builder().setByteArray(ArrayUtils.EMPTY_BYTE_ARRAY).get()) {
            assertEquals(-1, zin.getCompressedCount());
        }
    }

    @Test
    void testGetFirstEntryEmptyZip() throws IOException {
        try (ZipArchiveInputStream zin = ZipArchiveInputStream.builder().setByteArray(ArrayUtils.EMPTY_BYTE_ARRAY).get()) {
            assertNull(zin.getNextEntry());
        }
    }

    @Test
    void testGetNextZipEntry() throws IOException {
        try (ZipArchiveInputStream inputStream = ZipArchiveInputStream.builder()
                .setURI(getURI("org/apache/commons/compress/zip/getNextZipEntry.bin"))
                .get()) {
            assertThrows(IOException.class, () -> inputStream.forEach(IOConsumer.noop()));
        }
    }

    @Test
    void testGetUncompressedCountEmptyZip() throws IOException {
        try (ZipArchiveInputStream zin = ZipArchiveInputStream.builder().setByteArray(ArrayUtils.EMPTY_BYTE_ARRAY).get()) {
            assertEquals(0, zin.getUncompressedCount());
        }
    }

    /**
     * Test case for <a href="https://issues.apache.org/jira/browse/COMPRESS-351">COMPRESS-351</a>.
     */
    @Test
    void testMessageWithCorruptFileName() throws Exception {
        try (ZipArchiveInputStream in = ZipArchiveInputStream.builder().setURI(getURI("COMPRESS-351.zip")).get()) {
            final EOFException ex = assertThrows(EOFException.class, () -> in.forEach(IOConsumer.noop()));
            final String m = ex.getMessage();
            assertTrue(m.startsWith("Truncated ZIP entry: ?2016")); // the first character is not printable
        }
    }

    @Test
    void testMultiByteReadConsistentlyReturnsMinusOneAtEofUsingBzip2() throws Exception {
        multiByteReadConsistentlyReturnsMinusOneAtEof(getFile("bzip2-zip.zip"));
    }

    @Test
    void testMultiByteReadConsistentlyReturnsMinusOneAtEofUsingDeflate() throws Exception {
        multiByteReadConsistentlyReturnsMinusOneAtEof(getFile("bla.zip"));
    }

    @Test
    void testMultiByteReadConsistentlyReturnsMinusOneAtEofUsingDeflate64() throws Exception {
        multiByteReadConsistentlyReturnsMinusOneAtEof(getFile("COMPRESS-380/COMPRESS-380.zip"));
    }

    @Test
    void testMultiByteReadConsistentlyReturnsMinusOneAtEofUsingExplode() throws Exception {
        multiByteReadConsistentlyReturnsMinusOneAtEof(getFile("imploding-8Kdict-3trees.zip"));
    }

    @Test
    void testMultiByteReadConsistentlyReturnsMinusOneAtEofUsingStore() throws Exception {
        multiByteReadConsistentlyReturnsMinusOneAtEof(getFile("COMPRESS-264.zip"));
    }

    @Test
    void testMultiByteReadConsistentlyReturnsMinusOneAtEofUsingUnshrink() throws Exception {
        multiByteReadConsistentlyReturnsMinusOneAtEof(getFile("SHRUNK.ZIP"));
    }

    @Test
    void testMultiByteReadThrowsAtEofForCorruptedStoredEntry() throws Exception {
        final byte[] content = readAllBytes("COMPRESS-264.zip");
        // make size much bigger than entry's real size
        Arrays.fill(content, 17, 26, (byte) 0xff);
        final byte[] buf = new byte[2];
        try (ZipArchiveInputStream archive =
                ZipArchiveInputStream.builder().setByteArray(content).get()) {
            assertNotNull(archive.getNextEntry());
            final IOException ex1 = assertThrows(ArchiveException.class, () -> IOUtils.toByteArray(archive), "expected exception");
            assertEquals("Truncated ZIP file", ex1.getMessage());
            final IOException ex2 = assertThrows(ArchiveException.class, () -> archive.read(buf), "expected exception");
            assertEquals("Truncated ZIP file", ex2.getMessage());
            final IOException ex3 = assertThrows(ArchiveException.class, () -> archive.read(buf), "expected exception");
            assertEquals("Truncated ZIP file", ex3.getMessage());
        }
    }

    @Test
    void testNameSourceDefaultsToName() throws Exception {
        nameSource("bla.zip", "test1.xml", ZipArchiveEntry.NameSource.NAME);
    }

    @Test
    void testNameSourceIsSetToEFS() throws Exception {
        nameSource("utf8-7zip-test.zip", "\u20AC_for_Dollar.txt", 3, ZipArchiveEntry.NameSource.NAME_WITH_EFS_FLAG);
    }

    @Test
    void testNameSourceIsSetToUnicodeExtraField() throws Exception {
        nameSource("utf8-winzip-test.zip", "\u20AC_for_Dollar.txt", ZipArchiveEntry.NameSource.UNICODE_EXTRA_FIELD);
    }

    /**
     * Test correct population of header and data offsets.
     */
    @Test
    void testOffsets() throws Exception {
        // mixed.zip contains both inflated and stored files
        try (ZipArchiveInputStream zipStream = ZipArchiveInputStream.builder().setURI(getURI("mixed.zip")).get()) {
            final ZipArchiveEntry inflatedEntry = zipStream.getNextZipEntry();
            assertEquals("inflated.txt", inflatedEntry.getName());
            assertEquals(0x0000, inflatedEntry.getLocalHeaderOffset());
            assertEquals(0x0046, inflatedEntry.getDataOffset());
            final ZipArchiveEntry storedEntry = zipStream.getNextZipEntry();
            assertEquals("stored.txt", storedEntry.getName());
            assertEquals(0x5892, storedEntry.getLocalHeaderOffset());
            assertEquals(0x58d6, storedEntry.getDataOffset());
            assertNull(zipStream.getNextZipEntry());
        }
    }

    @Test
    void testProperlyMarksEntriesAsUnreadableIfUncompressedSizeIsUnknown() throws Exception {
        // we never read any data
        try (ZipArchiveInputStream zis = ZipArchiveInputStream.builder()
                .setByteArray(ArrayUtils.EMPTY_BYTE_ARRAY)
                .get()) {
            final ZipArchiveEntry e = new ZipArchiveEntry("test");
            e.setMethod(ZipMethod.DEFLATED.getCode());
            assertTrue(zis.canReadEntryData(e));
            e.setMethod(ZipMethod.ENHANCED_DEFLATED.getCode());
            assertTrue(zis.canReadEntryData(e));
            e.setMethod(ZipMethod.BZIP2.getCode());
            assertFalse(zis.canReadEntryData(e));
        }
    }

    @Test
    void testProperlyReadsStoredEntries() throws IOException {
        try (ZipArchiveInputStream archive = ZipArchiveInputStream.builder().setURI(getURI("bla-stored.zip")).get()) {
            ZipArchiveEntry e = archive.getNextZipEntry();
            assertNotNull(e);
            assertEquals("test1.xml", e.getName());
            assertEquals(610, e.getCompressedSize());
            assertEquals(610, e.getSize());
            byte[] data = IOUtils.toByteArray(archive);
            assertEquals(610, data.length);
            e = archive.getNextZipEntry();
            assertNotNull(e);
            assertEquals("test2.xml", e.getName());
            assertEquals(82, e.getCompressedSize());
            assertEquals(82, e.getSize());
            data = IOUtils.toByteArray(archive);
            assertEquals(82, data.length);
            assertNull(archive.getNextEntry());
        }
    }

    @Test
    void testProperlyReadsStoredEntryWithDataDescriptorWithoutSignature() throws IOException {
        try (ZipArchiveInputStream archive = ZipArchiveInputStream.builder()
                .setURI(getURI("bla-stored-dd-nosig.zip"))
                .setSupportStoredEntryDataDescriptor(true)
                .get()) {
            final ZipArchiveEntry e = archive.getNextZipEntry();
            assertNotNull(e);
            assertEquals("test1.xml", e.getName());
            assertEquals(-1, e.getCompressedSize());
            assertEquals(-1, e.getSize());
            final byte[] data = IOUtils.toByteArray(archive);
            assertEquals(610, data.length);
            assertEquals(610, e.getCompressedSize());
            assertEquals(610, e.getSize());
        }
    }

    @Test
    void testProperlyReadsStoredEntryWithDataDescriptorWithSignature() throws IOException {
        try (ZipArchiveInputStream archive = ZipArchiveInputStream.builder()
                .setURI(getURI("bla-stored-dd.zip"))
                .setSupportStoredEntryDataDescriptor(true)
                .get()) {
            final ZipArchiveEntry e = archive.getNextZipEntry();
            assertNotNull(e);
            assertEquals("test1.xml", e.getName());
            assertEquals(-1, e.getCompressedSize());
            assertEquals(-1, e.getSize());
            final byte[] data = IOUtils.toByteArray(archive);
            assertEquals(610, data.length);
            assertEquals(610, e.getCompressedSize());
            assertEquals(610, e.getSize());
        }
    }

    /**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-189"
     */
    @Test
    void testProperUseOfInflater() throws Exception {
        try (ZipFile zf = ZipFile.builder().setFile(getFile("COMPRESS-189.zip")).get()) {
            final ZipArchiveEntry zae = zf.getEntry("USD0558682-20080101.ZIP");
            try (ZipArchiveInputStream in = ZipArchiveInputStream.builder()
                    .setInputStream(new BufferedInputStream(zf.getInputStream(zae)))
                    .get()) {
                ZipArchiveEntry innerEntry;
                while ((innerEntry = in.getNextZipEntry()) != null) {
                    if (innerEntry.getName().endsWith("XML")) {
                        assertTrue(0 < in.read());
                    }
                }
            }
        }
    }

    /**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-380"
     */
    @Test
    void testReadDeflate64CompressedStream() throws Exception {
        final byte[] orig = readAllBytes("COMPRESS-380/COMPRESS-380-input");
        final File archive = getFile("COMPRESS-380/COMPRESS-380.zip");
        try (ZipArchiveInputStream zin = ZipArchiveInputStream.builder().setFile(archive).get()) {
            assertNotNull(zin.getNextZipEntry());
            final byte[] fromZip = IOUtils.toByteArray(zin);
            assertArrayEquals(orig, fromZip);
        }
    }

    @Test
    void testReadDeflate64CompressedStreamWithDataDescriptor() throws Exception {
        // this is a copy of bla.jar with META-INF/MANIFEST.MF's method manually changed to ENHANCED_DEFLATED
        final File archive = getFile("COMPRESS-380/COMPRESS-380-dd.zip");
        try (ZipArchiveInputStream zin = ZipArchiveInputStream.builder().setFile(archive).get()) {
            final ZipArchiveEntry e = zin.getNextZipEntry();
            assertEquals(-1, e.getSize());
            assertEquals(ZipMethod.ENHANCED_DEFLATED.getCode(), e.getMethod());
            final byte[] fromZip = IOUtils.toByteArray(zin);
            final byte[] expected = { 'M', 'a', 'n', 'i', 'f', 'e', 's', 't', '-', 'V', 'e', 'r', 's', 'i', 'o', 'n', ':', ' ', '1', '.', '0', '\r', '\n', '\r',
                    '\n' };
            assertArrayEquals(expected, fromZip);
            zin.getNextZipEntry();
            assertEquals(25, e.getSize());
        }
    }

    /**
     * Test case for <a href="https://issues.apache.org/jira/browse/COMPRESS-264">COMPRESS-264</a>.
     */
    @Test
    void testReadingOfFirstStoredEntry() throws Exception {
        try (ZipArchiveInputStream in = ZipArchiveInputStream.builder().setURI(getURI("COMPRESS-264.zip")).get()) {
            final ZipArchiveEntry ze = in.getNextZipEntry();
            assertEquals(5, ze.getSize());
            assertArrayEquals(new byte[] { 'd', 'a', 't', 'a', '\n' }, IOUtils.toByteArray(in));
        }
    }

    @Test
    void testRejectsStoredEntriesWithDataDescriptorByDefault() throws IOException {
        try (ZipArchiveInputStream archive = ZipArchiveInputStream.builder()
                .setURI(getURI("bla-stored-dd.zip"))
                .get()) {
            final ZipArchiveEntry e = archive.getNextZipEntry();
            assertNotNull(e);
            assertEquals("test1.xml", e.getName());
            assertEquals(-1, e.getCompressedSize());
            assertEquals(-1, e.getSize());
            assertThrows(UnsupportedZipFeatureException.class, () -> IOUtils.toByteArray(archive));
        }
    }

    @Test
    void testShouldConsumeArchiveCompletely() throws Exception {
        try (InputStream is = ZipArchiveInputStreamTest.class.getResourceAsStream("/archive_with_trailer.zip");
                ZipArchiveInputStream zip = ZipArchiveInputStream.builder().setInputStream(is).get()) {
            getAllZipEntries(zip);
            final byte[] expected = { 'H', 'e', 'l', 'l', 'o', ',', ' ', 'w', 'o', 'r', 'l', 'd', '!', '\n' };
            final byte[] actual = new byte[expected.length];
            is.read(actual);
            assertArrayEquals(expected, actual);
        }
    }

    /**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-219"
     */
    @Test
    void testShouldReadNestedZip() throws IOException {
        try (ZipArchiveInputStream in = ZipArchiveInputStream.builder().setURI(getURI("COMPRESS-219.zip")).get()) {
            extractZipInputStream(in);
        }
    }

    @Test
    void testSingleArgumentConstructor() throws Exception {
        final InputStream inputStream = mock(InputStream.class);
        try (ZipArchiveInputStream archiveStream = new ZipArchiveInputStream(inputStream)) {
            assertEquals(StandardCharsets.UTF_8, archiveStream.getCharset());
            assertEquals(true, readDeclaredField(archiveStream, "useUnicodeExtraFields", true));
            assertEquals(false, readDeclaredField(archiveStream, "supportStoredEntryDataDescriptor", true));
            assertEquals(false, readDeclaredField(archiveStream, "skipSplitSignature", true));
        }
    }

    @Test
    void testSingleByteReadConsistentlyReturnsMinusOneAtEofUsingBzip2() throws Exception {
        singleByteReadConsistentlyReturnsMinusOneAtEof(getFile("bzip2-zip.zip"));
    }

    @Test
    void testSingleByteReadConsistentlyReturnsMinusOneAtEofUsingDeflate() throws Exception {
        singleByteReadConsistentlyReturnsMinusOneAtEof(getFile("bla.zip"));
    }

    @Test
    void testSingleByteReadConsistentlyReturnsMinusOneAtEofUsingDeflate64() throws Exception {
        singleByteReadConsistentlyReturnsMinusOneAtEof(getFile("COMPRESS-380/COMPRESS-380.zip"));
    }

    @Test
    void testSingleByteReadConsistentlyReturnsMinusOneAtEofUsingExplode() throws Exception {
        singleByteReadConsistentlyReturnsMinusOneAtEof(getFile("imploding-8Kdict-3trees.zip"));
    }

    @Test
    void testSingleByteReadConsistentlyReturnsMinusOneAtEofUsingStore() throws Exception {
        singleByteReadConsistentlyReturnsMinusOneAtEof(getFile("COMPRESS-264.zip"));
    }

    @Test
    void testSingleByteReadConsistentlyReturnsMinusOneAtEofUsingUnshrink() throws Exception {
        singleByteReadConsistentlyReturnsMinusOneAtEof(getFile("SHRUNK.ZIP"));
    }

    @Test
    void testSingleByteReadThrowsAtEofForCorruptedStoredEntry() throws Exception {
        final byte[] content = readAllBytes("COMPRESS-264.zip");
        // make size much bigger than entry's real size
        for (int i = 17; i < 26; i++) {
            content[i] = (byte) 0xff;
        }
        try (ZipArchiveInputStream archive =
                ZipArchiveInputStream.builder().setByteArray(content).get()) {
            assertNotNull(archive.getNextEntry());
            final IOException ex1 = assertThrows(ArchiveException.class, () -> IOUtils.toByteArray(archive), "expected exception");
            assertEquals("Truncated ZIP file", ex1.getMessage());
            final IOException ex2 = assertThrows(ArchiveException.class, archive::read, "expected exception");
            assertEquals("Truncated ZIP file", ex2.getMessage());
            final IOException ex3 = assertThrows(ArchiveException.class, archive::read, "expected exception");
            assertEquals("Truncated ZIP file", ex3.getMessage());
        }
    }

    @Test
    void testSplitZipCreatedByWinrar() throws IOException {
        final File lastFile = getFile("COMPRESS-477/split_zip_created_by_winrar/split_zip_created_by_winrar.zip");
        try (SeekableByteChannel channel = ZipSplitReadOnlySeekableByteChannel.buildFromLastSplitSegment(lastFile);
                InputStream inputStream = Channels.newInputStream(channel);
                ZipArchiveInputStream splitInputStream = ZipArchiveInputStream.builder()
                        .setInputStream(inputStream)
                        .setSkipSplitSignature(true)
                        .get()) {

            final File fileToCompare = getFile("COMPRESS-477/split_zip_created_by_winrar/zip_to_compare_created_by_winrar.zip");
            try (ZipArchiveInputStream inputStreamToCompare = ZipArchiveInputStream.builder()
                    .setFile(fileToCompare)
                    .setSkipSplitSignature(true)
                    .get()) {
                ArchiveEntry entry;
                while ((entry = splitInputStream.getNextEntry()) != null && inputStreamToCompare.getNextEntry() != null) {
                    if (entry.isDirectory()) {
                        continue;
                    }
                    assertArrayEquals(IOUtils.toByteArray(splitInputStream), IOUtils.toByteArray(inputStreamToCompare));
                }
            }
        }
    }

    @Test
    void testSplitZipCreatedByZip() throws IOException {
        final File lastFile = getFile("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.zip");
        try (SeekableByteChannel channel = ZipSplitReadOnlySeekableByteChannel.buildFromLastSplitSegment(lastFile);
                InputStream inputStream = Channels.newInputStream(channel);
                ZipArchiveInputStream splitInputStream = ZipArchiveInputStream.builder()
                        .setInputStream(inputStream)
                        .setSkipSplitSignature(true)
                        .get()) {

            final Path fileToCompare = getPath("COMPRESS-477/split_zip_created_by_zip/zip_to_compare_created_by_zip.zip");
            try (ZipArchiveInputStream inputStreamToCompare = new ZipArchiveInputStream(Files.newInputStream(fileToCompare), StandardCharsets.UTF_8.name(),
                    true, false, true)) {

                ArchiveEntry entry;
                while ((entry = splitInputStream.getNextEntry()) != null && inputStreamToCompare.getNextEntry() != null) {
                    if (entry.isDirectory()) {
                        continue;
                    }
                    assertArrayEquals(IOUtils.toByteArray(splitInputStream), IOUtils.toByteArray(inputStreamToCompare));
                }
            }
        }
    }

    @Test
    void testSplitZipCreatedByZipOfZip64() throws IOException {
        final File lastFile = getFile("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip_zip64.zip");
        try (SeekableByteChannel channel = ZipSplitReadOnlySeekableByteChannel.buildFromLastSplitSegment(lastFile);
                InputStream inputStream = Channels.newInputStream(channel);
                ZipArchiveInputStream splitInputStream = ZipArchiveInputStream.builder()
                        .setInputStream(inputStream)
                        .setSkipSplitSignature(true)
                        .get()) {

            final Path fileToCompare = getPath("COMPRESS-477/split_zip_created_by_zip/zip_to_compare_created_by_zip_zip64.zip");
            try (ZipArchiveInputStream inputStreamToCompare = ZipArchiveInputStream.builder()
                    .setPath(fileToCompare)
                    .setSkipSplitSignature(true)
                    .get()) {

                ArchiveEntry entry;
                while ((entry = splitInputStream.getNextEntry()) != null && inputStreamToCompare.getNextEntry() != null) {
                    if (entry.isDirectory()) {
                        continue;
                    }
                    assertArrayEquals(IOUtils.toByteArray(splitInputStream), IOUtils.toByteArray(inputStreamToCompare));
                }
            }
        }
    }

    @Test
    void testSplitZipCreatedByZipThrowsException() throws IOException {
        try (ZipArchiveInputStream inputStream = ZipArchiveInputStream.builder()
                .setURI(getURI("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.z01"))
                .setSkipSplitSignature(true)
                .get()) {
            assertThrows(EOFException.class, () -> {
                ArchiveEntry entry = inputStream.getNextEntry();
                while (entry != null) {
                    entry = inputStream.getNextEntry();
                }
            });
        }
    }

    /**
     * {@code getNextZipEntry()} should throw a {@code ZipException} rather than return {@code null} when an unexpected structure is encountered.
     */
    @Test
    void testThrowOnInvalidEntry() throws Exception {
        try (ZipArchiveInputStream zip = ZipArchiveInputStream.builder()
                .setURI(getURI("invalid-zip.zip"))
                .get()) {
            final ZipException expected = assertThrows(ZipException.class, zip::getNextZipEntry, "IOException expected");
            assertTrue(expected.getMessage().contains("Cannot find zip signature"));
        }
    }

    @Test
    void testThrowsIfStoredDDIsDifferentFromLengthRead() throws IOException {
        try (ZipArchiveInputStream archive = ZipArchiveInputStream.builder()
                .setURI(getURI("bla-stored-dd-contradicts-actualsize.zip"))
                .setSupportStoredEntryDataDescriptor(true)
                .get()) {
            final ZipArchiveEntry e = archive.getNextZipEntry();
            assertNotNull(e);
            assertEquals("test1.xml", e.getName());
            assertEquals(-1, e.getCompressedSize());
            assertEquals(-1, e.getSize());
            assertThrows(ZipException.class, () -> IOUtils.toByteArray(archive));
        }
    }

    @Test
    void testThrowsIfStoredDDIsInconsistent() throws IOException {
        try (ZipArchiveInputStream archive = ZipArchiveInputStream.builder()
                .setURI(getURI("bla-stored-dd-sizes-differ.zip"))
                .setSupportStoredEntryDataDescriptor(true)
                .get()) {
            final ZipArchiveEntry e = archive.getNextZipEntry();
            assertNotNull(e);
            assertEquals("test1.xml", e.getName());
            assertEquals(-1, e.getCompressedSize());
            assertEquals(-1, e.getSize());
            assertThrows(ZipException.class, () -> IOUtils.toByteArray(archive));
        }
    }

    /**
     * @see <a href="https://issues.apache.org/jira/browse/COMPRESS-523">COMPRESS-523</a>
     */
    @Test
    void testThrowsIfThereIsNoEocd() {
        assertThrows(IOException.class, () -> fuzzingTest(new int[] { 0x50, 0x4b, 0x01, 0x02, 0x14, 0x00, 0x14, 0x00, 0x08, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x43, 0xbe, 0x00, 0x00, 0x00, 0xb7, 0xe8, 0x07, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00 }));
    }

    /**
     * @see <a href="https://issues.apache.org/jira/browse/COMPRESS-518">COMPRESS-518</a>
     */
    @Test
    void testThrowsIfZip64ExtraCouldNotBeUnderstood() {
        assertThrows(IOException.class,
                () -> fuzzingTest(new int[] { 0x50, 0x4b, 0x03, 0x04, 0x2e, 0x00, 0x00, 0x00, 0x0c, 0x00, 0x84, 0xb6, 0xba, 0x46, 0x72, 0xb6, 0xfe, 0x77, 0x63,
                        0x00, 0x00, 0x00, 0x6b, 0x00, 0x00, 0x00, 0x03, 0x00, 0x1c, 0x00, 0x62, 0x62, 0x62, 0x01, 0x00, 0x09, 0x00, 0x03, 0xe7, 0xce, 0x64,
                        0x55, 0xf3, 0xce, 0x64, 0x55, 0x75, 0x78, 0x0b, 0x00, 0x01, 0x04, 0x5c, 0xf9, 0x01, 0x00, 0x04, 0x88, 0x13, 0x00, 0x00 }));
    }

    @Test
    void testThrowsIOExceptionIfThereIsCorruptedZip64Extra() throws IOException {
        try (ZipArchiveInputStream zipInputStream = ZipArchiveInputStream.builder()
                .setURI(getURI("COMPRESS-546.zip"))
                .get()) {
            assertThrows(IOException.class, () -> getAllZipEntries(zipInputStream));
        }
    }

    @Test
    void testUnshrinkEntry() throws Exception {
        try (ZipArchiveInputStream in = ZipArchiveInputStream.builder().setURI(getURI("SHRUNK.ZIP")).get()) {
            ZipArchiveEntry entry = in.getNextZipEntry();
            assertEquals(ZipMethod.UNSHRINKING.getCode(), entry.getMethod(), "method");
            assertTrue(in.canReadEntryData(entry));
            try (InputStream original = newInputStream("test1.xml")) {
                try {
                    assertArrayEquals(IOUtils.toByteArray(original), IOUtils.toByteArray(in));
                } finally {
                    original.close();
                }
                entry = in.getNextZipEntry();
                assertEquals(ZipMethod.UNSHRINKING.getCode(), entry.getMethod(), "method");
                assertTrue(in.canReadEntryData(entry));
            }
            assertArrayEquals(readAllBytes("test2.xml"), IOUtils.toByteArray(in));
        }
    }

    @Test
    void testUnzipBZip2CompressedEntry() throws Exception {
        try (ZipArchiveInputStream in = ZipArchiveInputStream.builder().setURI(getURI("bzip2-zip.zip")).get()) {
            final ZipArchiveEntry ze = in.getNextZipEntry();
            assertEquals(42, ze.getSize());
            final byte[] expected = ArrayFill.fill(new byte[42], (byte) 'a');
            assertArrayEquals(expected, IOUtils.toByteArray(in));
        }
    }

    /**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-176"
     */
    @Test
    void testWinzipBackSlashWorkaround() throws Exception {
        try (ZipArchiveInputStream in = ZipArchiveInputStream.builder().setURI(getURI("test-winzip.zip")).get()) {
            ZipArchiveEntry zae = in.getNextZipEntry();
            zae = in.getNextZipEntry();
            zae = in.getNextZipEntry();
            assertEquals("\u00e4/", zae.getName());
        }
    }

    /**
     * Test case for <a href="https://issues.apache.org/jira/browse/COMPRESS-364">COMPRESS-364</a>.
     */
    @Test
    void testWithBytesAfterData() throws Exception {
        final int expectedNumEntries = 2;
        try (ZipArchiveInputStream zip = ZipArchiveInputStream.builder()
                .setURI(getURI("archive_with_bytes_after_data.zip"))
                .get()) {
            int actualNumEntries = 0;
            ZipArchiveEntry zae = zip.getNextZipEntry();
            while (zae != null) {
                actualNumEntries++;
                readEntry(zip, zae);
                zae = zip.getNextZipEntry();
            }
            assertEquals(expectedNumEntries, actualNumEntries);
        }
    }

    /**
     * Tests COMPRESS-689.
     */
    @Test
    void testWriteZipWithLinks() throws IOException {
        try (OutputStream output = new FileOutputStream("target/zipWithLinks.zip");
                ZipArchiveOutputStream zipOutputStream = new ZipArchiveOutputStream(output)) {
            zipOutputStream.putArchiveEntry(new ZipArchiveEntry("original"));
            zipOutputStream.write("original content".getBytes());
            zipOutputStream.closeArchiveEntry();
            final ZipArchiveEntry entry = new ZipArchiveEntry("link");
            entry.setUnixMode(UnixStat.LINK_FLAG | 0444);
            assertEquals(ZipArchiveEntry.PLATFORM_UNIX, entry.getPlatform());
            assertTrue(entry.isUnixSymlink());
            zipOutputStream.putArchiveEntry(entry);
            zipOutputStream.write("original".getBytes());
            zipOutputStream.closeArchiveEntry();
        }
        // Reads the central directory
        try (ZipFile zipFile = ZipFile.builder().setFile("target/zipWithLinks.zip").get()) {
            assertTrue(zipFile.getEntry("link").isUnixSymlink(), "'link' detected but it's not sym link");
            assertFalse(zipFile.getEntry("original").isUnixSymlink(), "'original' detected but it's not sym link");
        }
        // Doesn't reads the central directory
        try (ZipArchiveInputStream zipInputStream = ZipArchiveInputStream.builder()
                .setFile("target/zipWithLinks.zip")
                .get()) {
            ZipArchiveEntry entry;
            int entriesCount = 0;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if ("link".equals(entry.getName())) {
                    // This information is only set in the central directory
                    // assertTrue(entry.isUnixSymlink(), "'link' detected but it's not sym link");
                } else {
                    assertFalse(entry.isUnixSymlink(), "'original' detected but it's sym link and should be regular file");
                }
                entriesCount++;
            }
            assertEquals(2, entriesCount);
        }
    }

    @Test
    void testZipArchiveInputStreamSubclassReplacement() throws IOException {
        try (InputStream fs = newInputStream("COMPRESS-692/compress-692.zip");
                AirliftZipArchiveInputStream archive = new AirliftZipArchiveInputStream(fs)) {
            assertFalse(archive.isUsed());
            ZipArchiveEntry e = archive.getNextEntry();
            assertNotNull(e);
            assertEquals(ZipMethod.ZSTD.getCode(), e.getMethod());
            assertEquals("dolor.txt", e.getName());
            assertEquals(635, e.getCompressedSize());
            assertEquals(6066, e.getSize());
            byte[] data = IOUtils.toByteArray(archive);
            assertEquals(6066, data.length);
            assertTrue(archive.isUsed());
            e = archive.getNextEntry();
            assertNotNull(e);
            assertEquals(ZipMethod.ZSTD.getCode(), e.getMethod());
            assertEquals("ipsum.txt", e.getName());
            assertEquals(636, e.getCompressedSize());
            assertEquals(6072, e.getSize());
            data = IOUtils.toByteArray(archive);
            assertEquals(6072, data.length);
            assertNotNull(archive.getNextEntry());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void testZipInputStream(final boolean allowStoredEntriesWithDataDescriptor) {
        try (ZipArchiveInputStream zIn = ZipArchiveInputStream.builder()
                .setURI(getURI("COMPRESS-647/test.zip"))
                .setUseUnicodeExtraFields(false)
                .setSupportStoredEntryDataDescriptor(allowStoredEntriesWithDataDescriptor)
                .get()) {
            ZipArchiveEntry zae = zIn.getNextEntry();
            while (zae != null) {
                zae = zIn.getNextEntry();
            }
        } catch (final IOException e) {
            // Ignore expected exception
        }
    }

    @Test
    void testZipUsingStoredWithDDAndNoDDSignature() throws IOException {
        try (InputStream inputStream = forgeZipInputStream();
                ZipArchiveInputStream zipInputStream = ZipArchiveInputStream.builder()
                        .setInputStream(inputStream)
                        .setSupportStoredEntryDataDescriptor(true)
                        .get()) {
            getAllZipEntries(zipInputStream);
        }
    }

    @Test
    void testZipWithBadExtraFields() throws IOException {
        try (ZipArchiveInputStream zipInputStream = ZipArchiveInputStream.builder()
                .setURI(getURI("COMPRESS-548.zip"))
                .get()) {
            getAllZipEntries(zipInputStream);
        }
    }

    @Test
    void testZipWithLongerBeginningGarbage() throws IOException {
        final Path path = createTempPath("preamble", ".zip");
        try (OutputStream fos = Files.newOutputStream(path)) {
            fos.write("#!/usr/bin/env some-program with quite a few arguments to make it longer than the local header\n".getBytes(StandardCharsets.UTF_8));
            try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(fos)) {
                final ZipArchiveEntry entry = new ZipArchiveEntry("file-1.txt");
                entry.setMethod(ZipEntry.DEFLATED);
                zos.putArchiveEntry(entry);
                zos.writeUtf8("entry-content\n");
                zos.closeArchiveEntry();
            }
        }
        try (ZipArchiveInputStream zis =
                ZipArchiveInputStream.builder().setPath(path).get()) {
            final ZipArchiveEntry entry = zis.getNextEntry();
            assertEquals("file-1.txt", entry.getName());
            final byte[] content = IOUtils.toByteArray(zis);
            assertArrayEquals("entry-content\n".getBytes(StandardCharsets.UTF_8), content);
        }
    }

    @Test
    void testZipWithShortBeginningGarbage() throws IOException {
        final Path path = createTempPath("preamble", ".zip");
        try (OutputStream fos = Files.newOutputStream(path)) {
            fos.write("#!/usr/bin/unzip\n".getBytes(StandardCharsets.UTF_8));
            try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(fos)) {
                final ZipArchiveEntry entry = new ZipArchiveEntry("file-1.txt");
                entry.setMethod(ZipEntry.DEFLATED);
                zos.putArchiveEntry(entry);
                zos.writeUtf8("entry-content\n");
                zos.closeArchiveEntry();
            }
        }
        try (ZipArchiveInputStream zis =
                ZipArchiveInputStream.builder().setPath(path).get()) {
            final ZipArchiveEntry entry = zis.getNextEntry();
            assertEquals("file-1.txt", entry.getName());
            final byte[] content = IOUtils.toByteArray(zis);
            assertArrayEquals("entry-content\n".getBytes(StandardCharsets.UTF_8), content);
        }
    }
}
