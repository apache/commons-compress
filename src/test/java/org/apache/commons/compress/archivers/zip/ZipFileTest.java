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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.function.IORunnable;
import org.apache.commons.lang3.ArrayFill;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.Assume;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import io.airlift.compress.zstd.ZstdInputStream;

class ZipFileTest extends AbstractTest {

    /**
     * This Class simulates the case where the Zip File uses the aircompressors {@link ZstdInputStream}
     */
    private final class AirliftZstdZipFile extends ZipFile {
        private boolean used;

        private AirliftZstdZipFile(final File file) throws IOException {
            super(file);
        }

        @Override
        protected InputStream createZstdInputStream(final InputStream is) throws IOException {
            return new ZstdInputStream(is) {

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

    private static final int OUT_OF_MEMORY = 137;

    private static void assertEntryName(final ArrayList<ZipArchiveEntry> entries, final int index, final String expectedName) {
        final ZipArchiveEntry ze = entries.get(index);
        assertEquals("src/main/java/org/apache/commons/compress/archivers/zip/" + expectedName + ".java", ze.getName());
    }

    private static void nameSource(final String archive, final String entry, final ZipArchiveEntry.NameSource expected) throws Exception {
        try (ZipFile zf = ZipFile.builder().setFile(getFile(archive)).get()) {
            final ZipArchiveEntry ze = zf.getEntry(entry);
            assertEquals(entry, ze.getName());
            assertEquals(expected, ze.getNameSource());
        }
    }

    private ZipFile zf;

    private void assertAllReadMethods(final byte[] expected, final ZipFile zipFile, final ZipArchiveEntry entry) throws IOException {
        // simple IOUtil read
        try (InputStream stream = zf.getInputStream(entry)) {
            final byte[] full = IOUtils.toByteArray(stream);
            assertArrayEquals(expected, full);
        }

        // big buffer at the beginning and then chunks by IOUtils read
        try (InputStream stream = zf.getInputStream(entry)) {
            final byte[] full;
            final byte[] bytes = new byte[0x40000];
            final int read = stream.read(bytes);
            if (read < 0) {
                full = ArrayUtils.EMPTY_BYTE_ARRAY;
            } else {
                full = readStreamRest(bytes, read, stream);
            }
            assertArrayEquals(expected, full);
        }

        // small chunk / single byte and big buffer then
        try (InputStream stream = zf.getInputStream(entry)) {
            final byte[] full;
            final int single = stream.read();
            if (single < 0) {
                full = ArrayUtils.EMPTY_BYTE_ARRAY;
            } else {
                final byte[] big = new byte[0x40000];
                big[0] = (byte) single;
                final int read = stream.read(big, 1, big.length - 1);
                if (read < 0) {
                    full = new byte[] { (byte) single };
                } else {
                    full = readStreamRest(big, read + 1, stream);
                }
            }
            assertArrayEquals(expected, full);
        }
    }

    private void assertFileEqualIgnoreEndOfLine(final File file1, final File file2) throws IOException {
        final List<String> linesOfFile1 = Files.readAllLines(Paths.get(file1.getCanonicalPath()), UTF_8);
        final List<String> linesOfFile2 = Files.readAllLines(Paths.get(file2.getCanonicalPath()), UTF_8);

        if (linesOfFile1.size() != linesOfFile2.size()) {
            fail("files not equal : " + file1.getName() + " , " + file2.getName());
        }

        String tempLineInFile1;
        String tempLineInFile2;
        for (int i = 0; i < linesOfFile1.size(); i++) {
            tempLineInFile1 = linesOfFile1.get(i).replace("\r\n", "\n");
            tempLineInFile2 = linesOfFile2.get(i).replace("\r\n", "\n");
            assertEquals(tempLineInFile1, tempLineInFile2);
        }
    }

    private void assertFileEqualsToEntry(final File fileToCompare, final ZipArchiveEntry entry, final ZipFile zipFile) throws IOException {
        final File tempFile = createTempFile("temp", "txt");
        try (OutputStream outputStream = Files.newOutputStream(tempFile.toPath());
                InputStream inputStream = zipFile.getInputStream(entry)) {
            IOUtils.copyLarge(inputStream, outputStream, new byte[10240]);
        }
        assertFileEqualIgnoreEndOfLine(fileToCompare, tempFile);
    }

    private long calculateCrc32(final byte[] content) {
        final CRC32 crc = new CRC32();
        crc.update(content);
        return crc.getValue();
    }

    private void multiByteReadConsistentlyReturnsMinusOneAtEof(final File file) throws Exception {
        final byte[] buf = new byte[2];
        try (ZipFile zipFile = ZipFile.builder().setFile(file).get()) {
            final ZipArchiveEntry e = zipFile.getEntries().nextElement();
            try (InputStream is = zipFile.getInputStream(e)) {
                IOUtils.toByteArray(is);
                assertEquals(-1, is.read(buf));
                assertEquals(-1, is.read(buf));
            }
        }
    }

    /*
     * ordertest.zip has been handcrafted.
     *
     * It contains enough files so any random coincidence of entries.keySet() and central directory order would be unlikely - in fact testCDOrder fails in svn
     * revision 920284.
     *
     * The central directory has ZipFile and ZipUtil swapped so central directory order is different from entry data order.
     */
    private void readOrderTest() throws Exception {
        zf = ZipFile.builder().setFile(getFile("ordertest.zip")).get();
    }

    /**
     * Utility to append the rest of the stream to already read data.
     */
    private byte[] readStreamRest(final byte[] beginning, final int length, final InputStream stream) throws IOException {
        final byte[] rest = IOUtils.toByteArray(stream);
        final byte[] full = new byte[length + rest.length];
        System.arraycopy(beginning, 0, full, 0, length);
        System.arraycopy(rest, 0, full, length, rest.length);
        return full;
    }

    private void singleByteReadConsistentlyReturnsMinusOneAtEof(final File file) throws Exception {
        try (ZipFile zipFile = ZipFile.builder().setFile(file).get();) {
            final ZipArchiveEntry e = zipFile.getEntries().nextElement();
            try (InputStream is = zipFile.getInputStream(e)) {
                IOUtils.toByteArray(is);
                assertEquals(-1, is.read());
                assertEquals(-1, is.read());
            }
        }
    }

    @AfterEach
    public void tearDownClose() {
        ZipFile.closeQuietly(zf);
    }

    @Test
    void testAlternativeZstdInputStream() throws Exception {
        final File archive = getFile("COMPRESS-692/compress-692.zip");
        try (AirliftZstdZipFile zf = new AirliftZstdZipFile(archive)) {
            final byte[] buffer = new byte[7000];
            final ZipArchiveEntry ze = zf.getEntry("dolor.txt");
            assertNotNull(ze);
            try (InputStream inputStream = zf.getInputStream(ze)) {
                assertNotNull(inputStream);
                assertFalse(zf.isUsed());
                final int bytesRead = org.apache.commons.compress.utils.IOUtils.readFully(inputStream, buffer);
                assertEquals(6066, bytesRead);
                assertTrue(zf.isUsed());
            }
        }

        try (ZipFile builtZipFile = ZipFile.builder().setPath(archive.getAbsolutePath()).setZstdInputStreamFactory(ZstdInputStream::new).get()) {
            final byte[] buffer = new byte[7000];
            final ZipArchiveEntry ze = builtZipFile.getEntry("dolor.txt");
            assertNotNull(ze);
            try (InputStream inputStream = builtZipFile.getInputStream(ze)) {
                assertTrue(inputStream instanceof ZstdInputStream);
                assertNotNull(inputStream);
                final int bytesRead = org.apache.commons.compress.utils.IOUtils.readFully(inputStream, buffer);
                assertEquals(6066, bytesRead);
            }
        }
    }

    @Test
    void testBuidlerGet() throws IOException {
        assertThrows(ZipException.class, () -> ZipFile.builder().setPath("src/test/resources/org/apache/commons/compress/zip/getNextZipEntry.bin").get());
    }

    @Test
    void testCDOrder() throws Exception {
        readOrderTest();
        testCDOrderInMemory();
    }

    @Test
    void testCDOrderInMemory() throws Exception {
        final byte[] data = readAllBytes("ordertest.zip");
        zf = ZipFile.builder().setByteArray(data).setCharset(StandardCharsets.UTF_8).get();
        testCDOrderInMemory(zf);
        try (SeekableInMemoryByteChannel channel = new SeekableInMemoryByteChannel(data)) {
            zf = ZipFile.builder().setSeekableByteChannel(channel).setCharset(StandardCharsets.UTF_8).get();
            testCDOrderInMemory(zf);
        }
        try (SeekableInMemoryByteChannel channel = new SeekableInMemoryByteChannel(data)) {
            zf = new ZipFile(channel, StandardCharsets.UTF_8.name());
            testCDOrderInMemory(zf);
        }
    }

    private void testCDOrderInMemory(final ZipFile zipFile) {
        final ArrayList<ZipArchiveEntry> list = Collections.list(zipFile.getEntries());
        assertEntryName(list, 0, "AbstractUnicodeExtraField");
        assertEntryName(list, 1, "AsiExtraField");
        assertEntryName(list, 2, "ExtraFieldUtils");
        assertEntryName(list, 3, "FallbackZipEncoding");
        assertEntryName(list, 4, "GeneralPurposeBit");
        assertEntryName(list, 5, "JarMarker");
        assertEntryName(list, 6, "NioZipEncoding");
        assertEntryName(list, 7, "Simple8BitZipEncoding");
        assertEntryName(list, 8, "UnicodeCommentExtraField");
        assertEntryName(list, 9, "UnicodePathExtraField");
        assertEntryName(list, 10, "UnixStat");
        assertEntryName(list, 11, "UnparseableExtraFieldData");
        assertEntryName(list, 12, "UnrecognizedExtraField");
        assertEntryName(list, 13, "ZipArchiveEntry");
        assertEntryName(list, 14, "ZipArchiveInputStream");
        assertEntryName(list, 15, "ZipArchiveOutputStream");
        assertEntryName(list, 16, "ZipEncoding");
        assertEntryName(list, 17, "ZipEncodingHelper");
        assertEntryName(list, 18, "ZipExtraField");
        assertEntryName(list, 19, "ZipUtil");
        assertEntryName(list, 20, "ZipLong");
        assertEntryName(list, 21, "ZipShort");
        assertEntryName(list, 22, "ZipFile");
    }

    @Test
    void testConcurrentReadFile() throws Exception {
        // mixed.zip contains both inflated and stored files
        final File archive = getFile("mixed.zip");
        zf = new ZipFile(archive);
        final Map<String, byte[]> content = new HashMap<>();
        zf.stream().forEach(entry -> {
            try (InputStream inputStream = zf.getInputStream(entry)) {
                content.put(entry.getName(), IOUtils.toByteArray(inputStream));
            }
        });
        final AtomicInteger passedCount = new AtomicInteger();
        final IORunnable run = () -> {
            zf.stream().forEach(entry -> assertAllReadMethods(content.get(entry.getName()), zf, entry));
            passedCount.incrementAndGet();
        };
        final Thread t0 = new Thread(run.asRunnable());
        final Thread t1 = new Thread(run.asRunnable());
        t0.start();
        t1.start();
        t0.join();
        t1.join();
        assertEquals(2, passedCount.get());
    }

    @Test
    void testConcurrentReadSeekable() throws Exception {
        // mixed.zip contains both inflated and stored files
        final byte[] data;
        try (InputStream fis = newInputStream("mixed.zip")) {
            data = IOUtils.toByteArray(fis);
        }
        try (SeekableInMemoryByteChannel channel = new SeekableInMemoryByteChannel(data)) {
            zf = ZipFile.builder().setSeekableByteChannel(channel).setCharset(StandardCharsets.UTF_8).get();
            final Map<String, byte[]> content = new HashMap<>();
            zf.stream().forEach(entry -> {
                try (InputStream inputStream = zf.getInputStream(entry)) {
                    content.put(entry.getName(), IOUtils.toByteArray(inputStream));
                }
            });
            final AtomicInteger passedCount = new AtomicInteger();
            final IORunnable run = () -> {
                zf.stream().forEach(entry -> assertAllReadMethods(content.get(entry.getName()), zf, entry));
                passedCount.incrementAndGet();
            };
            final Thread t0 = new Thread(run.asRunnable());
            final Thread t1 = new Thread(run.asRunnable());
            t0.start();
            t1.start();
            t0.join();
            t1.join();
            assertEquals(2, passedCount.get());
        }
    }

    @Test
    void testCreateBoundedInputStream() throws IOException {
        try (ZipFile zipFile = ZipFile.builder().setPath("src/test/resources/org/apache/commons/compress/zip/createBoundedInputStream.bin").get()) {
            for (final ZipArchiveEntry entry : Collections.list(zipFile.getEntries())) {
                assertThrows(ArchiveException.class, () -> zipFile.getInputStream(entry));
            }
        }
    }

    /**
     * Test correct population of header and data offsets when they are written after stream.
     */
    @Test
    void testDelayedOffsetsAndSizes() throws Exception {
        final ByteArrayOutputStream zipContent = new ByteArrayOutputStream();
        try (ZipArchiveOutputStream zipOutput = new ZipArchiveOutputStream(zipContent)) {
            final ZipArchiveEntry inflatedEntry = new ZipArchiveEntry("inflated.txt");
            inflatedEntry.setMethod(ZipEntry.DEFLATED);
            zipOutput.putArchiveEntry(inflatedEntry);
            zipOutput.write("Hello Deflated\n".getBytes());
            zipOutput.closeArchiveEntry();

            final byte[] storedContent = "Hello Stored\n".getBytes();
            final ZipArchiveEntry storedEntry = new ZipArchiveEntry("stored.txt");
            storedEntry.setMethod(ZipEntry.STORED);
            storedEntry.setSize(storedContent.length);
            storedEntry.setCrc(calculateCrc32(storedContent));
            zipOutput.putArchiveEntry(storedEntry);
            zipOutput.write("Hello Stored\n".getBytes());
            zipOutput.closeArchiveEntry();

        }

        try (ZipFile zf = ZipFile.builder().setByteArray(zipContent.toByteArray()).get()) {
            final ZipArchiveEntry inflatedEntry = zf.getEntry("inflated.txt");
            assertNotEquals(-1L, inflatedEntry.getLocalHeaderOffset());
            assertNotEquals(-1L, inflatedEntry.getDataOffset());
            assertTrue(inflatedEntry.isStreamContiguous());
            assertNotEquals(-1L, inflatedEntry.getCompressedSize());
            assertNotEquals(-1L, inflatedEntry.getSize());
            final ZipArchiveEntry storedEntry = zf.getEntry("stored.txt");
            assertNotEquals(-1L, storedEntry.getLocalHeaderOffset());
            assertNotEquals(-1L, storedEntry.getDataOffset());
            assertTrue(inflatedEntry.isStreamContiguous());
            assertNotEquals(-1L, storedEntry.getCompressedSize());
            assertNotEquals(-1L, storedEntry.getSize());
        }
    }

    @Test
    void testDoubleClose() throws Exception {
        readOrderTest();
        zf.close();
        assertDoesNotThrow(zf::close, "Caught exception of second close");
    }

    /**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-227"
     */
    @Test
    void testDuplicateEntry() throws Exception {
        final File archive = getFile("COMPRESS-227.zip");
        zf = new ZipFile(archive);

        final ZipArchiveEntry ze = zf.getEntry("test1.txt");
        assertNotNull(ze);
        try (InputStream inputStream = zf.getInputStream(ze)) {
            assertNotNull(inputStream);

            int numberOfEntries = 0;
            for (final ZipArchiveEntry entry : zf.getEntries("test1.txt")) {
                numberOfEntries++;
                try (InputStream inputStream2 = zf.getInputStream(entry)) {
                    assertNotNull(inputStream2);
                }
            }
            assertEquals(2, numberOfEntries);
        }
    }

    /**
     * Test entries alignment.
     */
    @Test
    void testEntryAlignment() throws Exception {
        try (SeekableInMemoryByteChannel zipContent = new SeekableInMemoryByteChannel()) {
            try (ZipArchiveOutputStream zipOutput = new ZipArchiveOutputStream(zipContent)) {
                final ZipArchiveEntry inflatedEntry = new ZipArchiveEntry("inflated.txt");
                inflatedEntry.setMethod(ZipEntry.DEFLATED);
                inflatedEntry.setAlignment(1024);
                zipOutput.putArchiveEntry(inflatedEntry);
                zipOutput.writeUtf8("Hello Deflated\n");
                zipOutput.closeArchiveEntry();

                final ZipArchiveEntry storedEntry = new ZipArchiveEntry("stored.txt");
                storedEntry.setMethod(ZipEntry.STORED);
                storedEntry.setAlignment(1024);
                zipOutput.putArchiveEntry(storedEntry);
                zipOutput.writeUtf8("Hello Stored\n");
                zipOutput.closeArchiveEntry();

                final ZipArchiveEntry storedEntry2 = new ZipArchiveEntry("stored2.txt");
                storedEntry2.setMethod(ZipEntry.STORED);
                storedEntry2.setAlignment(1024);
                storedEntry2.addExtraField(new ResourceAlignmentExtraField(1));
                zipOutput.putArchiveEntry(storedEntry2);
                zipOutput.writeUtf8("Hello overload-alignment Stored\n");
                zipOutput.closeArchiveEntry();

                final ZipArchiveEntry storedEntry3 = new ZipArchiveEntry("stored3.txt");
                storedEntry3.setMethod(ZipEntry.STORED);
                storedEntry3.addExtraField(new ResourceAlignmentExtraField(1024));
                zipOutput.putArchiveEntry(storedEntry3);
                zipOutput.writeUtf8("Hello copy-alignment Stored\n");
                zipOutput.closeArchiveEntry();

            }

            try (ZipFile zf = ZipFile.builder().setByteArray(Arrays.copyOfRange(zipContent.array(), 0, (int) zipContent.size())).get()) {
                final ZipArchiveEntry inflatedEntry = zf.getEntry("inflated.txt");
                final ResourceAlignmentExtraField inflatedAlignmentEx = (ResourceAlignmentExtraField) inflatedEntry
                        .getExtraField(ResourceAlignmentExtraField.ID);
                assertNotEquals(-1L, inflatedEntry.getCompressedSize());
                assertNotEquals(-1L, inflatedEntry.getSize());
                assertEquals(0L, inflatedEntry.getDataOffset() % 1024);
                assertNotNull(inflatedAlignmentEx);
                assertEquals(1024, inflatedAlignmentEx.getAlignment());
                assertFalse(inflatedAlignmentEx.allowMethodChange());
                try (InputStream stream = zf.getInputStream(inflatedEntry)) {
                    assertEquals("Hello Deflated\n", new String(IOUtils.toByteArray(stream), UTF_8));
                }
                final ZipArchiveEntry storedEntry = zf.getEntry("stored.txt");
                final ResourceAlignmentExtraField storedAlignmentEx = (ResourceAlignmentExtraField) storedEntry.getExtraField(ResourceAlignmentExtraField.ID);
                assertNotEquals(-1L, storedEntry.getCompressedSize());
                assertNotEquals(-1L, storedEntry.getSize());
                assertEquals(0L, storedEntry.getDataOffset() % 1024);
                assertNotNull(storedAlignmentEx);
                assertEquals(1024, storedAlignmentEx.getAlignment());
                assertFalse(storedAlignmentEx.allowMethodChange());
                try (InputStream stream = zf.getInputStream(storedEntry)) {
                    assertEquals("Hello Stored\n", new String(IOUtils.toByteArray(stream), UTF_8));
                }

                final ZipArchiveEntry storedEntry2 = zf.getEntry("stored2.txt");
                final ResourceAlignmentExtraField stored2AlignmentEx = (ResourceAlignmentExtraField) storedEntry2.getExtraField(ResourceAlignmentExtraField.ID);
                assertNotEquals(-1L, storedEntry2.getCompressedSize());
                assertNotEquals(-1L, storedEntry2.getSize());
                assertEquals(0L, storedEntry2.getDataOffset() % 1024);
                assertNotNull(stored2AlignmentEx);
                assertEquals(1024, stored2AlignmentEx.getAlignment());
                assertFalse(stored2AlignmentEx.allowMethodChange());
                try (InputStream stream = zf.getInputStream(storedEntry2)) {
                    assertEquals("Hello overload-alignment Stored\n", new String(IOUtils.toByteArray(stream), UTF_8));
                }

                final ZipArchiveEntry storedEntry3 = zf.getEntry("stored3.txt");
                final ResourceAlignmentExtraField stored3AlignmentEx = (ResourceAlignmentExtraField) storedEntry3.getExtraField(ResourceAlignmentExtraField.ID);
                assertNotEquals(-1L, storedEntry3.getCompressedSize());
                assertNotEquals(-1L, storedEntry3.getSize());
                assertEquals(0L, storedEntry3.getDataOffset() % 1024);
                assertNotNull(stored3AlignmentEx);
                assertEquals(1024, stored3AlignmentEx.getAlignment());
                assertFalse(stored3AlignmentEx.allowMethodChange());
                try (InputStream stream = zf.getInputStream(storedEntry3)) {
                    assertEquals("Hello copy-alignment Stored\n", new String(IOUtils.toByteArray(stream), UTF_8));
                }
            }
        }
    }

    /**
     * Test too big alignment, resulting into exceeding extra field limit.
     */
    @Test
    void testEntryAlignmentExceed() throws Exception {
        try (SeekableInMemoryByteChannel zipContent = new SeekableInMemoryByteChannel();
                ZipArchiveOutputStream zipOutput = new ZipArchiveOutputStream(zipContent)) {
            final ZipArchiveEntry inflatedEntry = new ZipArchiveEntry("inflated.txt");
            inflatedEntry.setMethod(ZipEntry.STORED);
            assertThrows(IllegalArgumentException.class, () -> inflatedEntry.setAlignment(0x20000));
        }
    }

    /**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-228"
     */
    @Test
    void testExcessDataInZip64ExtraField() throws Exception {
        final File archive = getFile("COMPRESS-228.zip");
        zf = new ZipFile(archive);
        // actually, if we get here, the test already has passed

        final ZipArchiveEntry ze = zf.getEntry("src/main/java/org/apache/commons/compress/archivers/zip/ZipFile.java");
        assertEquals(26101, ze.getSize());
    }

    @Test
    void testExtractFileLiesAcrossSplitZipSegmentsCreatedByWinrar() throws Exception {
        final File lastFile = getFile("COMPRESS-477/split_zip_created_by_winrar/split_zip_created_by_winrar.zip");
        try (SeekableByteChannel channel = ZipSplitReadOnlySeekableByteChannel.buildFromLastSplitSegment(lastFile)) {
            zf = ZipFile.builder().setSeekableByteChannel(channel).get();

            // the compressed content of ZipArchiveInputStream.java lies between .z01 and .z02
            final ZipArchiveEntry zipEntry = zf.getEntry("commons-compress/src/main/java/org/apache/commons/compress/archivers/zip/ZipArchiveInputStream.java");
            final File fileToCompare = getFile("COMPRESS-477/split_zip_created_by_winrar/file_to_compare_1");
            assertFileEqualsToEntry(fileToCompare, zipEntry, zf);
        }
    }

    @Test
    void testExtractFileLiesAcrossSplitZipSegmentsCreatedByZip() throws Exception {
        final File lastFile = getFile("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.zip");
        try (SeekableByteChannel channel = ZipSplitReadOnlySeekableByteChannel.buildFromLastSplitSegment(lastFile)) {
            zf = new ZipFile(channel);

            // the compressed content of UnsupportedCompressionAlgorithmException.java lies between .z01 and .z02
            ZipArchiveEntry zipEntry = zf
                    .getEntry("commons-compress/src/main/java/org/apache/commons/compress/archivers/dump/UnsupportedCompressionAlgorithmException.java");
            File fileToCompare = getFile("COMPRESS-477/split_zip_created_by_zip/file_to_compare_1");
            assertFileEqualsToEntry(fileToCompare, zipEntry, zf);

            // the compressed content of DeflateParameters.java lies between .z02 and .zip
            zipEntry = zf.getEntry("commons-compress/src/main/java/org/apache/commons/compress/compressors/deflate/DeflateParameters.java");
            fileToCompare = getFile("COMPRESS-477/split_zip_created_by_zip/file_to_compare_2");
            assertFileEqualsToEntry(fileToCompare, zipEntry, zf);
        }
    }

    @Test
    void testExtractFileLiesAcrossSplitZipSegmentsCreatedByZipOfZip64() throws Exception {
        final File lastFile = getFile("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip_zip64.zip");
        try (SeekableByteChannel channel = ZipSplitReadOnlySeekableByteChannel.buildFromLastSplitSegment(lastFile)) {
            zf = new ZipFile(channel);

            // the compressed content of UnsupportedCompressionAlgorithmException.java lies between .z01 and .z02
            ZipArchiveEntry zipEntry = zf
                    .getEntry("commons-compress/src/main/java/org/apache/commons/compress/archivers/dump/UnsupportedCompressionAlgorithmException.java");
            File fileToCompare = getFile("COMPRESS-477/split_zip_created_by_zip/file_to_compare_1");
            assertFileEqualsToEntry(fileToCompare, zipEntry, zf);

            // the compressed content of DeflateParameters.java lies between .z02 and .zip
            zipEntry = zf.getEntry("commons-compress/src/main/java/org/apache/commons/compress/compressors/deflate/DeflateParameters.java");
            fileToCompare = getFile("COMPRESS-477/split_zip_created_by_zip/file_to_compare_2");
            assertFileEqualsToEntry(fileToCompare, zipEntry, zf);
        }
    }

    @Test
    void testGetEntries() throws Exception {
        // mixed.zip contains both inflated and stored files
        final File archive = getFile("mixed.zip");
        zf = new ZipFile(archive);
        final Map<String, byte[]> content = new HashMap<>();
        for (final ZipArchiveEntry entry : Collections.list(zf.getEntries())) {
            try (InputStream inputStream = zf.getInputStream(entry)) {
                content.put(entry.getName(), IOUtils.toByteArray(inputStream));
            }
        }
        for (final ZipArchiveEntry entry : Collections.list(zf.getEntries())) {
            assertAllReadMethods(content.get(entry.getName()), zf, entry);
        }
    }

    @Test
    void testGetEntriesInPhysicalOrder() throws Exception {
        // mixed.zip contains both inflated and stored files
        final File archive = getFile("mixed.zip");
        zf = new ZipFile(archive);
        final Map<String, byte[]> content = new HashMap<>();
        for (final ZipArchiveEntry entry : Collections.list(zf.getEntriesInPhysicalOrder())) {
            try (InputStream inputStream = zf.getInputStream(entry)) {
                content.put(entry.getName(), IOUtils.toByteArray(inputStream));
            }
        }
        for (final ZipArchiveEntry entry : Collections.list(zf.getEntries())) {
            assertAllReadMethods(content.get(entry.getName()), zf, entry);
        }
    }

    /**
     * Test non power of 2 alignment.
     */
    @Test
    void testInvalidAlignment() {
        assertThrows(IllegalArgumentException.class, () -> new ZipArchiveEntry("dummy").setAlignment(3));
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
    void testNameSourceDefaultsToName() throws Exception {
        nameSource("bla.zip", "test1.xml", ZipArchiveEntry.NameSource.NAME);
    }

    @Test
    void testNameSourceIsSetToEFS() throws Exception {
        nameSource("utf8-7zip-test.zip", "\u20AC_for_Dollar.txt", ZipArchiveEntry.NameSource.NAME_WITH_EFS_FLAG);
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
        final File archive = getFile("mixed.zip");
        try (ZipFile zf = new ZipFile(archive)) {
            final ZipArchiveEntry inflatedEntry = zf.getEntry("inflated.txt");
            assertEquals(0x0000, inflatedEntry.getLocalHeaderOffset());
            assertEquals(0x0046, inflatedEntry.getDataOffset());
            assertTrue(inflatedEntry.isStreamContiguous());
            final ZipArchiveEntry storedEntry = zf.getEntry("stored.txt");
            assertEquals(0x5892, storedEntry.getLocalHeaderOffset());
            assertEquals(0x58d6, storedEntry.getDataOffset());
            assertTrue(inflatedEntry.isStreamContiguous());
        }
    }

    @Test
    void testPhysicalOrder() throws Exception {
        readOrderTest();
        final ArrayList<ZipArchiveEntry> l = Collections.list(zf.getEntriesInPhysicalOrder());
        assertEntryName(l, 0, "AbstractUnicodeExtraField");
        assertEntryName(l, 1, "AsiExtraField");
        assertEntryName(l, 2, "ExtraFieldUtils");
        assertEntryName(l, 3, "FallbackZipEncoding");
        assertEntryName(l, 4, "GeneralPurposeBit");
        assertEntryName(l, 5, "JarMarker");
        assertEntryName(l, 6, "NioZipEncoding");
        assertEntryName(l, 7, "Simple8BitZipEncoding");
        assertEntryName(l, 8, "UnicodeCommentExtraField");
        assertEntryName(l, 9, "UnicodePathExtraField");
        assertEntryName(l, 10, "UnixStat");
        assertEntryName(l, 11, "UnparseableExtraFieldData");
        assertEntryName(l, 12, "UnrecognizedExtraField");
        assertEntryName(l, 13, "ZipArchiveEntry");
        assertEntryName(l, 14, "ZipArchiveInputStream");
        assertEntryName(l, 15, "ZipArchiveOutputStream");
        assertEntryName(l, 16, "ZipEncoding");
        assertEntryName(l, 17, "ZipEncodingHelper");
        assertEntryName(l, 18, "ZipExtraField");
        assertEntryName(l, 19, "ZipFile");
        assertEntryName(l, 20, "ZipLong");
        assertEntryName(l, 21, "ZipShort");
        assertEntryName(l, 22, "ZipUtil");
    }

    @Test
    void testPhysicalOrderOfSpecificFile() throws Exception {
        readOrderTest();
        final String entryName = "src/main/java/org/apache/commons/compress/archivers/zip/ZipExtraField.java";
        final Iterable<ZipArchiveEntry> entries = zf.getEntriesInPhysicalOrder(entryName);
        final Iterator<ZipArchiveEntry> iter = entries.iterator();
        final ZipArchiveEntry entry = iter.next();

        assertEquals(entryName, entry.getName());
        assertFalse(iter.hasNext());
    }

    /**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-380"
     */
    @Test
    void testReadDeflate64CompressedStream() throws Exception {
        final File input = getFile("COMPRESS-380/COMPRESS-380-input");
        final File archive = getFile("COMPRESS-380/COMPRESS-380.zip");
        try (InputStream in = Files.newInputStream(input.toPath());
                ZipFile zf = new ZipFile(archive)) {
            final byte[] orig = IOUtils.toByteArray(in);
            final ZipArchiveEntry e = zf.getEntry("input2");
            try (InputStream s = zf.getInputStream(e)) {
                final byte[] fromZip = IOUtils.toByteArray(s);
                assertArrayEquals(orig, fromZip);
            }
        }
    }

    /**
     * Test case for <a href="https://issues.apache.org/jira/browse/COMPRESS-621">COMPRESS-621</a>.
     */
    @Test
    void testReadingOfExtraDataBeforeZip() throws IOException {
        final byte[] fileHeader = "Before Zip file".getBytes(UTF_8);
        final String entryName = "COMPRESS-621.txt";
        final byte[] entryContent = "https://issues.apache.org/jira/browse/COMPRESS-621".getBytes(UTF_8);
        try (ZipFile archive = new ZipFile(getFile("COMPRESS-621.zip"))) {
            assertEquals(fileHeader.length, archive.getFirstLocalFileHeaderOffset());
            try (InputStream input = archive.getContentBeforeFirstLocalFileHeader()) {
                assertArrayEquals(fileHeader, IOUtils.toByteArray(input));
            }

            final ZipArchiveEntry e = archive.getEntry(entryName);
            assertEquals(entryContent.length, e.getSize());
            try (InputStream input = archive.getInputStream(e)) {
                assertArrayEquals(entryContent, IOUtils.toByteArray(input));
            }
        }
    }

    /**
     * Test case for <a href="https://issues.apache.org/jira/browse/COMPRESS-264">COMPRESS-264</a>.
     */
    @Test
    void testReadingOfFirstStoredEntry() throws Exception {
        final File archive = getFile("COMPRESS-264.zip");
        zf = new ZipFile(archive);
        final ZipArchiveEntry ze = zf.getEntry("test.txt");
        assertEquals(5, ze.getSize());
        try (InputStream inputStream = zf.getInputStream(ze)) {
            assertArrayEquals(new byte[] { 'd', 'a', 't', 'a', '\n' }, IOUtils.toByteArray(inputStream));
        }
    }

    @Test
    void testReadingOfStoredEntry() throws Exception {
        final File file = createTempFile("commons-compress-zipfiletest", ".zip");
        ZipArchiveEntry ze;
        try (OutputStream o = Files.newOutputStream(file.toPath());
                ZipArchiveOutputStream zo = new ZipArchiveOutputStream(o)) {
            ze = new ZipArchiveEntry("foo");
            ze.setMethod(ZipEntry.STORED);
            ze.setSize(4);
            ze.setCrc(0xb63cfbcdL);
            zo.putArchiveEntry(ze);
            zo.write(new byte[] { 1, 2, 3, 4 });
            zo.closeArchiveEntry();
        }

        zf = new ZipFile(file);
        ze = zf.getEntry("foo");
        assertNotNull(ze);
        try (InputStream i = zf.getInputStream(ze)) {
            final byte[] b = new byte[4];
            assertEquals(4, i.read(b));
            assertEquals(-1, i.read());
        }
    }

    @Test
    void testSelfExtractingZipUsingUnzipsfx() throws IOException, InterruptedException {
        final File unzipsfx = new File("/usr/bin/unzipsfx");
        Assumptions.assumeTrue(unzipsfx.exists());

        final File testZip = createTempFile("commons-compress-selfExtractZipTest", ".zip");

        final String testEntryName = "test_self_extract_zip/foo";
        final File extractedFile = new File(testZip.getParentFile(), testEntryName);

        final byte[] testData = { 1, 2, 3, 4 };
        final byte[] buffer = new byte[512];
        int bytesRead;
        try (InputStream unzipsfxInputStream = Files.newInputStream(unzipsfx.toPath())) {
            try (OutputStream outputStream = Files.newOutputStream(testZip.toPath());
                    ZipArchiveOutputStream zo = new ZipArchiveOutputStream(outputStream)) {

                while ((bytesRead = unzipsfxInputStream.read(buffer)) > 0) {
                    zo.writePreamble(buffer, 0, bytesRead);
                }

                final ZipArchiveEntry ze = new ZipArchiveEntry(testEntryName);
                ze.setMethod(ZipEntry.STORED);
                ze.setSize(4);
                ze.setCrc(0xb63cfbcdL);
                zo.putArchiveEntry(ze);
                zo.write(testData);
                zo.closeArchiveEntry();
            }

            final ProcessBuilder pbChmod = new ProcessBuilder("chmod", "+x", testZip.getPath());
            pbChmod.redirectErrorStream(true);
            final Process processChmod = pbChmod.start();
            try (InputStream processInputStream = processChmod.getInputStream()) {
                assertEquals(0, processChmod.waitFor(), new String(IOUtils.toByteArray(processInputStream)));
            }

            final ProcessBuilder pb = new ProcessBuilder(testZip.getPath());
            pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
            pb.directory(testZip.getParentFile());
            pb.redirectErrorStream(true);
            final Process process = pb.start();
            final int rc = process.waitFor();
            if (rc == OUT_OF_MEMORY && SystemUtils.IS_OS_MAC) {
                // On my old Mac mini, this test runs out of memory, so allow the build to continue.
                Assume.assumeTrue(Boolean.getBoolean("skipReturnCode137"));
                return;
            }
            try (InputStream processInputStream = process.getInputStream()) {
                assertEquals(0, rc, new String(IOUtils.toByteArray(processInputStream)));
            }
            if (!extractedFile.exists()) {
                // fail if extracted file does not exist
                fail("Can not find the extracted file");
            }

            try (InputStream inputStream = Files.newInputStream(extractedFile.toPath())) {
                bytesRead = org.apache.commons.compress.utils.IOUtils.readFully(inputStream, buffer);
                assertEquals(testData.length, bytesRead);
                assertArrayEquals(testData, Arrays.copyOfRange(buffer, 0, bytesRead));
            }
        } finally {
            extractedFile.delete();
            extractedFile.getParentFile().delete();
        }
    }

    @Test
    void testSetLevelTooBigForZipArchiveOutputStream() throws IOException {
        try (ZipArchiveOutputStream fixture = new ZipArchiveOutputStream(new ByteArrayOutputStream())) {
            assertThrows(IllegalArgumentException.class, () -> fixture.setLevel(Deflater.BEST_COMPRESSION + 1));
        }
    }

    @Test
    void testSetLevelTooSmallForZipArchiveOutputStream() throws IOException {
        try (ZipArchiveOutputStream fixture = new ZipArchiveOutputStream(new ByteArrayOutputStream())) {
        assertThrows(IllegalArgumentException.class, () -> fixture.setLevel(Deflater.DEFAULT_COMPRESSION - 1));
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

    /**
     * Test case for <a href="https://issues.apache.org/jira/browse/COMPRESS-208">COMPRESS-208</a>.
     */
    @Test
    void testSkipsPK00Prefix() throws Exception {
        final File archive = getFile("COMPRESS-208.zip");
        zf = new ZipFile(archive);
        assertNotNull(zf.getEntry("test1.xml"));
        assertNotNull(zf.getEntry("test2.xml"));
    }

    @Test
    void testThrowsExceptionWhenWritingPreamble() throws IOException {
        try (ZipArchiveOutputStream outputStream = new ZipArchiveOutputStream(new ByteArrayOutputStream())) {
            outputStream.putArchiveEntry(new ZipArchiveEntry());
            assertThrows(IllegalStateException.class, () -> outputStream.writePreamble(ArrayUtils.EMPTY_BYTE_ARRAY));
            outputStream.closeArchiveEntry();
        }
    }

    @Test
    void testUnixSymlinkSampleFile() throws Exception {
        final String entryPrefix = "COMPRESS-214_unix_symlinks/";
        final TreeMap<String, String> expectedVals = new TreeMap<>();
        // I threw in some Japanese characters to keep things interesting.
        expectedVals.put(entryPrefix + "link1", "../COMPRESS-214_unix_symlinks/./a/b/c/../../../\uF999");
        expectedVals.put(entryPrefix + "link2", "../COMPRESS-214_unix_symlinks/./a/b/c/../../../g");
        expectedVals.put(entryPrefix + "link3", "../COMPRESS-214_unix_symlinks/././a/b/c/../../../\u76F4\u6A39");
        expectedVals.put(entryPrefix + "link4", "\u82B1\u5B50/\u745B\u5B50");
        expectedVals.put(entryPrefix + "\uF999", "./\u82B1\u5B50/\u745B\u5B50/\u5897\u8C37/\uF999");
        expectedVals.put(entryPrefix + "g", "./a/b/c/d/e/f/g");
        expectedVals.put(entryPrefix + "\u76F4\u6A39", "./g");
        // Notice how a directory link might contain a trailing slash, or it might not.
        // Also note: symlinks are always stored as files, even if they link to directories.
        expectedVals.put(entryPrefix + "link5", "../COMPRESS-214_unix_symlinks/././a/b");
        expectedVals.put(entryPrefix + "link6", "../COMPRESS-214_unix_symlinks/././a/b/");
        // I looked into creating a test with hard links, but ZIP does not appear to
        // support hard links, so nevermind.
        final File archive = getFile("COMPRESS-214_unix_symlinks.zip");
        zf = new ZipFile(archive);
        zf.stream().forEach(zae -> {
            final String link = zf.getUnixSymlink(zae);
            if (zae.isUnixSymlink()) {
                final String name = zae.getName();
                final String expected = expectedVals.get(name);
                assertEquals(expected, link);
            } else {
                // Should be null if it's not a symlink!
                assertNull(link);
            }
        });
    }

    @Test
    void testUnshrinking() throws Exception {
        zf = new ZipFile(getFile("SHRUNK.ZIP"));
        ZipArchiveEntry test = zf.getEntry("TEST1.XML");
        try (InputStream original = newInputStream("test1.xml");
                InputStream inputStream = zf.getInputStream(test)) {
            assertArrayEquals(IOUtils.toByteArray(original), IOUtils.toByteArray(inputStream));
        }
        test = zf.getEntry("TEST2.XML");
        try (InputStream original = newInputStream("test2.xml");
                InputStream inputStream = zf.getInputStream(test)) {
            assertArrayEquals(IOUtils.toByteArray(original), IOUtils.toByteArray(inputStream));
        }
    }

    @Test
    void testUnzipBZip2CompressedEntry() throws Exception {
        final File archive = getFile("bzip2-zip.zip");
        zf = new ZipFile(archive);
        final ZipArchiveEntry ze = zf.getEntry("lots-of-as");
        assertEquals(42, ze.getSize());
        final byte[] expected = ArrayFill.fill(new byte[42], (byte) 'a');
        try (InputStream inputStream = zf.getInputStream(ze)) {
            assertArrayEquals(expected, IOUtils.toByteArray(inputStream));
        }
    }

    /**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-176"
     */
    @Test
    void testWinzipBackSlashWorkaround() throws Exception {
        final File archive = getFile("test-winzip.zip");
        zf = new ZipFile(archive);
        assertNull(zf.getEntry("\u00e4\\\u00fc.txt"));
        assertNotNull(zf.getEntry("\u00e4/\u00fc.txt"));
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
        try (ZipFile zipFile = ZipFile.builder().setPath(path).get()) {
            final ZipArchiveEntry entry = zipFile.getEntry("file-1.txt");
            assertEquals("file-1.txt", entry.getName());
            try (InputStream inputStream = zipFile.getInputStream(entry)) {
                final byte[] content = IOUtils.toByteArray(inputStream);
                assertArrayEquals("entry-content\n".getBytes(StandardCharsets.UTF_8), content);
            }
        }
    }

    /*
     * Tests [COMPRESS-708] ZstdCompressorInputStream closes the InputStream held by ZipArchiveInputStream garbage collection.
     */
    @Test
    void testZstdInputStreamErrorCloseWhenGc() throws Exception {
        final File archive = getFile("COMPRESS-692/compress-692.zip");
        for (int i = 0; i < 500; i++) {
            try (FileInputStream fileInputStream = new FileInputStream(archive);
                    ZipArchiveInputStream zipArchiveInputStream = new ZipArchiveInputStream(fileInputStream)) {
                ArchiveEntry entry;
                while ((entry = zipArchiveInputStream.getNextEntry()) != null) {
                    if (entry.isDirectory()) {
                        continue;
                    }
                    System.gc();
                    IOUtils.toByteArray(zipArchiveInputStream);
                }
            } catch (final IOException e) {
                fail("testZstdInputStreamErrorCloseWhenGc error, test error at batch " + (i + 1), e);
            }
        }
    }

}
