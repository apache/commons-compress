/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.commons.compress.archivers.zip;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.utils.ByteUtils;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.Test;

public class ZipArchiveInputStreamTest extends AbstractTestCase {

    private static void nameSource(final String archive, final String entry, int entryNo, final ZipArchiveEntry.NameSource expected)
        throws Exception {
        try (ZipArchiveInputStream zis = new ZipArchiveInputStream(Files.newInputStream(getFile(archive).toPath()))) {
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
        final int len = (int)zae.getSize();
        final byte[] buff = new byte[len];
        zip.read(buff, 0, len);

        return buff;
    }

    private void extractZipInputStream(final ZipArchiveInputStream in)
        throws IOException {
        ZipArchiveEntry zae = in.getNextZipEntry();
        while (zae != null) {
            if (zae.getName().endsWith(".zip")) {
                extractZipInputStream(new ZipArchiveInputStream(in));
            }
            zae = in.getNextZipEntry();
        }
    }

    /**
     * Forge a ZIP archive in memory, using STORED and
     * Data Descriptor, and without signature of Data
     * Descriptor.
     *
     * @return the input stream of the generated zip
     * @throws IOException there are problems
     */
    private InputStream forgeZipInputStream() throws IOException {
        try (final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             final ZipArchiveOutputStream zo = new ZipArchiveOutputStream(byteArrayOutputStream)){

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
        try (ArchiveInputStream ais = ArchiveStreamFactory.DEFAULT
             .createArchiveInputStream("zip", new ByteArrayInputStream(input))) {
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
        try (InputStream in = newInputStream("bla.zip");
             ZipArchiveInputStream archive = new ZipArchiveInputStream(in)) {
            final ArchiveEntry e = archive.getNextEntry();
            IOUtils.toByteArray(archive);
            assertEquals(-1, archive.read(buf));
            assertEquals(-1, archive.read(buf));
        }
    }

    @Test
    public void multiByteReadConsistentlyReturnsMinusOneAtEofUsingBzip2() throws Exception {
        multiByteReadConsistentlyReturnsMinusOneAtEof(getFile("bzip2-zip.zip"));
    }

    @Test
    public void multiByteReadConsistentlyReturnsMinusOneAtEofUsingDeflate() throws Exception {
        multiByteReadConsistentlyReturnsMinusOneAtEof(getFile("bla.zip"));
    }

    @Test
    public void multiByteReadConsistentlyReturnsMinusOneAtEofUsingDeflate64() throws Exception {
        multiByteReadConsistentlyReturnsMinusOneAtEof(getFile("COMPRESS-380/COMPRESS-380.zip"));
    }

    @Test
    public void multiByteReadConsistentlyReturnsMinusOneAtEofUsingExplode() throws Exception {
        multiByteReadConsistentlyReturnsMinusOneAtEof(getFile("imploding-8Kdict-3trees.zip"));
    }

    @Test
    public void multiByteReadConsistentlyReturnsMinusOneAtEofUsingStore() throws Exception {
        multiByteReadConsistentlyReturnsMinusOneAtEof(getFile("COMPRESS-264.zip"));
    }

    @Test
    public void multiByteReadConsistentlyReturnsMinusOneAtEofUsingUnshrink() throws Exception {
        multiByteReadConsistentlyReturnsMinusOneAtEof(getFile("SHRUNK.ZIP"));
    }

    @Test
    public void multiByteReadThrowsAtEofForCorruptedStoredEntry() throws Exception {
        final byte[] content = Files.readAllBytes(getFile("COMPRESS-264.zip").toPath());
        // make size much bigger than entry's real size
        for (int i = 17; i < 26; i++) {
            content[i] = (byte) 0xff;
        }
        final byte[] buf = new byte[2];
        try (ByteArrayInputStream in = new ByteArrayInputStream(content);
             ZipArchiveInputStream archive = new ZipArchiveInputStream(in)) {
            final ArchiveEntry e = archive.getNextEntry();
            final IOException ex1 = assertThrows(IOException.class, () -> IOUtils.toByteArray(archive), "expected exception");
            assertEquals("Truncated ZIP file", ex1.getMessage());
            final IOException ex2 = assertThrows(IOException.class, () -> archive.read(buf), "expected exception");
            assertEquals("Truncated ZIP file", ex2.getMessage());
            final IOException ex3 = assertThrows(IOException.class, () -> archive.read(buf), "expected exception");
            assertEquals("Truncated ZIP file", ex3.getMessage());
        }
    }

    @Test
    public void nameSourceDefaultsToName() throws Exception {
        nameSource("bla.zip", "test1.xml", ZipArchiveEntry.NameSource.NAME);
    }

    @Test
    public void nameSourceIsSetToEFS() throws Exception {
        nameSource("utf8-7zip-test.zip", "\u20AC_for_Dollar.txt", 3,
                   ZipArchiveEntry.NameSource.NAME_WITH_EFS_FLAG);
    }

    @Test
    public void nameSourceIsSetToUnicodeExtraField() throws Exception {
        nameSource("utf8-winzip-test.zip", "\u20AC_for_Dollar.txt",
                   ZipArchiveEntry.NameSource.UNICODE_EXTRA_FIELD);
    }

    @Test
    public void properlyMarksEntriesAsUnreadableIfUncompressedSizeIsUnknown() throws Exception {
        // we never read any data
        try (ZipArchiveInputStream zis = new ZipArchiveInputStream(new ByteArrayInputStream(ByteUtils.EMPTY_BYTE_ARRAY))) {
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
    public void properlyReadsStoredEntries() throws IOException {
        try (InputStream fs = newInputStream("bla-stored.zip");
             ZipArchiveInputStream archive = new ZipArchiveInputStream(fs)) {
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
    public void properlyReadsStoredEntryWithDataDescriptorWithoutSignature() throws IOException {
        try (InputStream fs = newInputStream("bla-stored-dd-nosig.zip");
             ZipArchiveInputStream archive = new ZipArchiveInputStream(fs, "UTF-8", true, true)) {
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
    public void properlyReadsStoredEntryWithDataDescriptorWithSignature() throws IOException {
        try (InputStream fs = newInputStream("bla-stored-dd.zip");
             ZipArchiveInputStream archive = new ZipArchiveInputStream(fs, "UTF-8", true, true)) {
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
    public void properUseOfInflater() throws Exception {
        ZipFile zf = null;
        ZipArchiveInputStream in = null;
        try {
            zf = new ZipFile(getFile("COMPRESS-189.zip"));
            final ZipArchiveEntry zae = zf.getEntry("USD0558682-20080101.ZIP");
            in = new ZipArchiveInputStream(new BufferedInputStream(zf.getInputStream(zae)));
            ZipArchiveEntry innerEntry;
            while ((innerEntry = in.getNextZipEntry()) != null) {
                if (innerEntry.getName().endsWith("XML")) {
                    assertTrue(0 < in.read());
                }
            }
        } finally {
            if (zf != null) {
                zf.close();
            }
            if (in != null) {
                in.close();
            }
        }
    }

    /**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-380"
     */
    @Test
    public void readDeflate64CompressedStream() throws Exception {
        final File input = getFile("COMPRESS-380/COMPRESS-380-input");
        final File archive = getFile("COMPRESS-380/COMPRESS-380.zip");
        try (InputStream in = Files.newInputStream(input.toPath());
             ZipArchiveInputStream zin = new ZipArchiveInputStream(Files.newInputStream(archive.toPath()))) {
            final byte[] orig = IOUtils.toByteArray(in);
            final ZipArchiveEntry e = zin.getNextZipEntry();
            final byte[] fromZip = IOUtils.toByteArray(zin);
            assertArrayEquals(orig, fromZip);
        }
    }

    @Test
    public void readDeflate64CompressedStreamWithDataDescriptor() throws Exception {
        // this is a copy of bla.jar with META-INF/MANIFEST.MF's method manually changed to ENHANCED_DEFLATED
        final File archive = getFile("COMPRESS-380/COMPRESS-380-dd.zip");
        try (ZipArchiveInputStream zin = new ZipArchiveInputStream(Files.newInputStream(archive.toPath()))) {
            final ZipArchiveEntry e = zin.getNextZipEntry();
            assertEquals(-1, e.getSize());
            assertEquals(ZipMethod.ENHANCED_DEFLATED.getCode(), e.getMethod());
            final byte[] fromZip = IOUtils.toByteArray(zin);
            final byte[] expected = {
                'M', 'a', 'n', 'i', 'f', 'e', 's', 't', '-', 'V', 'e', 'r', 's', 'i', 'o', 'n', ':', ' ', '1', '.', '0',
                '\r', '\n', '\r', '\n'
            };
            assertArrayEquals(expected, fromZip);
            zin.getNextZipEntry();
            assertEquals(25, e.getSize());
        }
    }

    @Test
    public void rejectsStoredEntriesWithDataDescriptorByDefault() throws IOException {
        try (InputStream fs = newInputStream("bla-stored-dd.zip"); ZipArchiveInputStream archive = new ZipArchiveInputStream(fs)) {
            final ZipArchiveEntry e = archive.getNextZipEntry();
            assertNotNull(e);
            assertEquals("test1.xml", e.getName());
            assertEquals(-1, e.getCompressedSize());
            assertEquals(-1, e.getSize());
            assertThrows(UnsupportedZipFeatureException.class, () -> IOUtils.toByteArray(archive));
        }
    }

    @Test
    public void shouldConsumeArchiveCompletely() throws Exception {
        try (InputStream is = ZipArchiveInputStreamTest.class.getResourceAsStream("/archive_with_trailer.zip");
                ZipArchiveInputStream zip = new ZipArchiveInputStream(is)) {
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
    public void shouldReadNestedZip() throws IOException {
        try (ZipArchiveInputStream in = new ZipArchiveInputStream(newInputStream("COMPRESS-219.zip"))) {
            extractZipInputStream(in);
        }
    }

    private void singleByteReadConsistentlyReturnsMinusOneAtEof(final File file) throws Exception {
        try (InputStream in = Files.newInputStream(file.toPath());
             ZipArchiveInputStream archive = new ZipArchiveInputStream(in)) {
            final ArchiveEntry e = archive.getNextEntry();
            IOUtils.toByteArray(archive);
            assertEquals(-1, archive.read());
            assertEquals(-1, archive.read());
        }
    }

    @Test
    public void singleByteReadConsistentlyReturnsMinusOneAtEofUsingBzip2() throws Exception {
        singleByteReadConsistentlyReturnsMinusOneAtEof(getFile("bzip2-zip.zip"));
    }

    @Test
    public void singleByteReadConsistentlyReturnsMinusOneAtEofUsingDeflate() throws Exception {
        singleByteReadConsistentlyReturnsMinusOneAtEof(getFile("bla.zip"));
    }

    @Test
    public void singleByteReadConsistentlyReturnsMinusOneAtEofUsingDeflate64() throws Exception {
        singleByteReadConsistentlyReturnsMinusOneAtEof(getFile("COMPRESS-380/COMPRESS-380.zip"));
    }

    @Test
    public void singleByteReadConsistentlyReturnsMinusOneAtEofUsingExplode() throws Exception {
        singleByteReadConsistentlyReturnsMinusOneAtEof(getFile("imploding-8Kdict-3trees.zip"));
    }

    @Test
    public void singleByteReadConsistentlyReturnsMinusOneAtEofUsingStore() throws Exception {
        singleByteReadConsistentlyReturnsMinusOneAtEof(getFile("COMPRESS-264.zip"));
    }

    @Test
    public void singleByteReadConsistentlyReturnsMinusOneAtEofUsingUnshrink() throws Exception {
        singleByteReadConsistentlyReturnsMinusOneAtEof(getFile("SHRUNK.ZIP"));
    }

    @Test
    public void singleByteReadThrowsAtEofForCorruptedStoredEntry() throws Exception {
        final byte[] content = Files.readAllBytes(getFile("COMPRESS-264.zip").toPath());
        // make size much bigger than entry's real size
        for (int i = 17; i < 26; i++) {
            content[i] = (byte) 0xff;
        }
        try (ByteArrayInputStream in = new ByteArrayInputStream(content);
             ZipArchiveInputStream archive = new ZipArchiveInputStream(in)) {
            final ArchiveEntry e = archive.getNextEntry();
            final IOException ex1 = assertThrows(IOException.class, () -> IOUtils.toByteArray(archive), "expected exception");
            assertEquals("Truncated ZIP file", ex1.getMessage());
            final IOException ex2 = assertThrows(IOException.class, archive::read, "expected exception");
            assertEquals("Truncated ZIP file", ex2.getMessage());
            final IOException ex3 = assertThrows(IOException.class, archive::read, "expected exception");
            assertEquals("Truncated ZIP file", ex3.getMessage());
        }
    }

    /**
     * Test case for
     * <a href="https://issues.apache.org/jira/browse/COMPRESS-351"
     * >COMPRESS-351</a>.
     */
    @Test
    public void testMessageWithCorruptFileName() throws Exception {
        try (ZipArchiveInputStream in = new ZipArchiveInputStream(newInputStream("COMPRESS-351.zip"))) {
            final EOFException ex = assertThrows(EOFException.class, () -> {
                ZipArchiveEntry ze = in.getNextZipEntry();
                while (ze != null) {
                    ze = in.getNextZipEntry();
                }
            }, "expected EOFException");
            final String m = ex.getMessage();
            assertTrue(m.startsWith("Truncated ZIP entry: ?2016")); // the first character is not printable
        }
    }

    /**
     * Test correct population of header and data offsets.
     */
    @Test
    public void testOffsets() throws Exception {
        // mixed.zip contains both inflated and stored files
        try (InputStream archiveStream = ZipArchiveInputStream.class.getResourceAsStream("/mixed.zip");
             ZipArchiveInputStream zipStream =  new ZipArchiveInputStream((archiveStream))
        ) {
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

    /**
     * Test case for
     * <a href="https://issues.apache.org/jira/browse/COMPRESS-264"
     * >COMPRESS-264</a>.
     */
    @Test
    public void testReadingOfFirstStoredEntry() throws Exception {

        try (ZipArchiveInputStream in = new ZipArchiveInputStream(newInputStream("COMPRESS-264.zip"))) {
            final ZipArchiveEntry ze = in.getNextZipEntry();
            assertEquals(5, ze.getSize());
            assertArrayEquals(new byte[] { 'd', 'a', 't', 'a', '\n' },
                    IOUtils.toByteArray(in));
        }
    }

    @Test
    public void testSplitZipCreatedByWinrar() throws IOException {
        final File lastFile = getFile("COMPRESS-477/split_zip_created_by_winrar/split_zip_created_by_winrar.zip");
        try (SeekableByteChannel channel = ZipSplitReadOnlySeekableByteChannel.buildFromLastSplitSegment(lastFile);
             InputStream inputStream = Channels.newInputStream(channel);
             ZipArchiveInputStream splitInputStream = new ZipArchiveInputStream(inputStream, ZipEncodingHelper.UTF8, true, false, true)) {

            final File fileToCompare = getFile("COMPRESS-477/split_zip_created_by_winrar/zip_to_compare_created_by_winrar.zip");
            try (ZipArchiveInputStream inputStreamToCompare = new ZipArchiveInputStream(Files.newInputStream(fileToCompare.toPath()), ZipEncodingHelper.UTF8, true, false, true)) {

                ArchiveEntry entry;
                while((entry = splitInputStream.getNextEntry()) != null && inputStreamToCompare.getNextEntry() != null) {
                    if (entry.isDirectory()) {
                        continue;
                    }
                    assertArrayEquals(IOUtils.toByteArray(splitInputStream),
                        IOUtils.toByteArray(inputStreamToCompare));
                }
            }
        }
    }

    @Test
    public void testSplitZipCreatedByZip() throws IOException {
        final File lastFile = getFile("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.zip");
        try (SeekableByteChannel channel = ZipSplitReadOnlySeekableByteChannel.buildFromLastSplitSegment(lastFile);
             InputStream inputStream = Channels.newInputStream(channel);
             ZipArchiveInputStream splitInputStream = new ZipArchiveInputStream(inputStream, ZipEncodingHelper.UTF8, true, false, true)) {

            final File fileToCompare = getFile("COMPRESS-477/split_zip_created_by_zip/zip_to_compare_created_by_zip.zip");
            try (ZipArchiveInputStream inputStreamToCompare = new ZipArchiveInputStream(Files.newInputStream(fileToCompare.toPath()), ZipEncodingHelper.UTF8, true, false, true)) {

                ArchiveEntry entry;
                while((entry = splitInputStream.getNextEntry()) != null && inputStreamToCompare.getNextEntry() != null) {
                    if (entry.isDirectory()) {
                        continue;
                    }
                    assertArrayEquals(IOUtils.toByteArray(splitInputStream),
                         IOUtils.toByteArray(inputStreamToCompare));
                }
            }
        }
    }

    @Test
    public void testSplitZipCreatedByZipOfZip64() throws IOException {
        final File lastFile = getFile("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip_zip64.zip");
        try (SeekableByteChannel channel = ZipSplitReadOnlySeekableByteChannel.buildFromLastSplitSegment(lastFile);
             InputStream inputStream = Channels.newInputStream(channel);
             ZipArchiveInputStream splitInputStream = new ZipArchiveInputStream(inputStream, ZipEncodingHelper.UTF8, true, false, true)) {

            final File fileToCompare = getFile("COMPRESS-477/split_zip_created_by_zip/zip_to_compare_created_by_zip_zip64.zip");
            try (ZipArchiveInputStream inputStreamToCompare = new ZipArchiveInputStream(Files.newInputStream(fileToCompare.toPath()), ZipEncodingHelper.UTF8, true, false, true)) {

                ArchiveEntry entry;
                while((entry = splitInputStream.getNextEntry()) != null && inputStreamToCompare.getNextEntry() != null) {
                    if (entry.isDirectory()) {
                        continue;
                    }
                    assertArrayEquals(IOUtils.toByteArray(splitInputStream),
                        IOUtils.toByteArray(inputStreamToCompare));
                }
            }
        }
    }

    @Test
    public void testSplitZipCreatedByZipThrowsException() throws IOException {
        final File zipSplitFile = getFile("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.z01");
        final InputStream fileInputStream = Files.newInputStream(zipSplitFile.toPath());
        final ZipArchiveInputStream inputStream = new ZipArchiveInputStream(fileInputStream, ZipEncodingHelper.UTF8, true, false, true);

        assertThrows(EOFException.class, () -> {
            ArchiveEntry entry = inputStream.getNextEntry();
            while (entry != null) {
                entry = inputStream.getNextEntry();
            }
        });
    }

    /**
     * {@code getNextZipEntry()} should throw a {@code ZipException} rather than return
     * {@code null} when an unexpected structure is encountered.
     */
    @Test
    public void testThrowOnInvalidEntry() throws Exception {
        final InputStream is = ZipArchiveInputStreamTest.class
                .getResourceAsStream("/invalid-zip.zip");

        try (final ZipArchiveInputStream zip = new ZipArchiveInputStream(is)) {
            final ZipException expected = assertThrows(ZipException.class, zip::getNextZipEntry, "IOException expected");
            assertTrue(expected.getMessage().contains("Unexpected record signature"));
        }
    }

    @Test
    public void testUnshrinkEntry() throws Exception {
        final ZipArchiveInputStream in = new ZipArchiveInputStream(newInputStream("SHRUNK.ZIP"));

        ZipArchiveEntry entry = in.getNextZipEntry();
        assertEquals(ZipMethod.UNSHRINKING.getCode(), entry.getMethod(), "method");
        assertTrue(in.canReadEntryData(entry));

        InputStream original = newInputStream("test1.xml");
        try {
            assertArrayEquals(IOUtils.toByteArray(original), IOUtils.toByteArray(in));
        } finally {
            original.close();
        }

        entry = in.getNextZipEntry();
        assertEquals(ZipMethod.UNSHRINKING.getCode(), entry.getMethod(), "method");
        assertTrue(in.canReadEntryData(entry));

        original = newInputStream("test2.xml");
        try {
            assertArrayEquals(IOUtils.toByteArray(original), IOUtils.toByteArray(in));
        } finally {
            original.close();
        }
    }

    @Test
    public void testUnzipBZip2CompressedEntry() throws Exception {

        try (ZipArchiveInputStream in = new ZipArchiveInputStream(newInputStream("bzip2-zip.zip"))) {
            final ZipArchiveEntry ze = in.getNextZipEntry();
            assertEquals(42, ze.getSize());
            final byte[] expected = new byte[42];
            Arrays.fill(expected, (byte) 'a');
            assertArrayEquals(expected, IOUtils.toByteArray(in));
        }
    }

    /**
     * Test case for
     * <a href="https://issues.apache.org/jira/browse/COMPRESS-364"
     * >COMPRESS-364</a>.
     */
    @Test
    public void testWithBytesAfterData() throws Exception {
        final int expectedNumEntries = 2;
        final InputStream is = ZipArchiveInputStreamTest.class
                .getResourceAsStream("/archive_with_bytes_after_data.zip");

        try (final ZipArchiveInputStream zip = new ZipArchiveInputStream(is)) {
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

    @Test
    public void testZipUsingStoredWithDDAndNoDDSignature() throws IOException {
        try (InputStream inputStream = forgeZipInputStream();
             ZipArchiveInputStream zipInputStream = new ZipArchiveInputStream(inputStream, "UTF-8", true, true)) {
            getAllZipEntries(zipInputStream);
        }
    }

    @Test
    public void testZipWithBadExtraFields() throws IOException {
        try (InputStream fis = newInputStream("COMPRESS-548.zip");
             ZipArchiveInputStream zipInputStream = new ZipArchiveInputStream(fis)) {
            getAllZipEntries(zipInputStream);
        }
    }

    @Test
    public void throwsIfStoredDDIsDifferentFromLengthRead() throws IOException {
        try (InputStream fs = newInputStream("bla-stored-dd-contradicts-actualsize.zip");
             ZipArchiveInputStream archive = new ZipArchiveInputStream(fs, "UTF-8", true, true)) {
            final ZipArchiveEntry e = archive.getNextZipEntry();
            assertNotNull(e);
            assertEquals("test1.xml", e.getName());
            assertEquals(-1, e.getCompressedSize());
            assertEquals(-1, e.getSize());
            assertThrows(ZipException.class, () -> IOUtils.toByteArray(archive));
        }
    }

    @Test
    public void throwsIfStoredDDIsInconsistent() throws IOException {
        try (InputStream fs = newInputStream("bla-stored-dd-sizes-differ.zip");
             ZipArchiveInputStream archive = new ZipArchiveInputStream(fs, "UTF-8", true, true)) {
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
    public void throwsIfThereIsNoEocd() {
        assertThrows(IOException.class, () -> fuzzingTest(new int[] {
            0x50, 0x4b, 0x01, 0x02, 0x14, 0x00, 0x14, 0x00, 0x08, 0x00,
            0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x43, 0xbe, 0x00, 0x00,
            0x00, 0xb7, 0xe8, 0x07, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00
        }));
    }

    /**
     * @see <a href="https://issues.apache.org/jira/browse/COMPRESS-518">COMPRESS-518</a>
     */
    @Test
    public void throwsIfZip64ExtraCouldNotBeUnderstood() {
        assertThrows(IOException.class, () ->
        fuzzingTest(new int[] {
            0x50, 0x4b, 0x03, 0x04, 0x2e, 0x00, 0x00, 0x00, 0x0c, 0x00,
            0x84, 0xb6, 0xba, 0x46, 0x72, 0xb6, 0xfe, 0x77, 0x63, 0x00,
            0x00, 0x00, 0x6b, 0x00, 0x00, 0x00, 0x03, 0x00, 0x1c, 0x00,
            0x62, 0x62, 0x62, 0x01, 0x00, 0x09, 0x00, 0x03, 0xe7, 0xce,
            0x64, 0x55, 0xf3, 0xce, 0x64, 0x55, 0x75, 0x78, 0x0b, 0x00,
            0x01, 0x04, 0x5c, 0xf9, 0x01, 0x00, 0x04, 0x88, 0x13, 0x00,
            0x00
        }));
    }

    @Test
    public void throwsIOExceptionIfThereIsCorruptedZip64Extra() throws IOException {
        try (InputStream fis = newInputStream("COMPRESS-546.zip");
             ZipArchiveInputStream zipInputStream = new ZipArchiveInputStream(fis)) {
            assertThrows(IOException.class, () -> getAllZipEntries(zipInputStream));
        }
    }

    /**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-176"
     */
    @Test
    public void winzipBackSlashWorkaround() throws Exception {
        try (ZipArchiveInputStream in = new ZipArchiveInputStream(newInputStream("test-winzip.zip"))) {
            ZipArchiveEntry zae = in.getNextZipEntry();
            zae = in.getNextZipEntry();
            zae = in.getNextZipEntry();
            assertEquals("\u00e4/", zae.getName());
        }
    }
}
