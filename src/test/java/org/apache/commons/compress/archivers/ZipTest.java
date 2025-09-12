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
package org.apache.commons.compress.archivers;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.archivers.zip.Zip64Mode;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntryPredicate;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.archivers.zip.ZipMethod;
import org.apache.commons.compress.archivers.zip.ZipSplitReadOnlySeekableByteChannel;
import org.apache.commons.compress.utils.InputStreamStatistics;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

public final class ZipTest extends AbstractTest {

    final String first_payload = "ABBA";

    final String second_payload = "AAAAAAAAAAAA";

    final ZipArchiveEntryPredicate allFilesPredicate = zipArchiveEntry -> true;

    private void addFilesToZip(final ZipArchiveOutputStream outputStream, final File fileToAdd) throws IOException {
        if (fileToAdd.isDirectory()) {
            for (final File file : fileToAdd.listFiles()) {
                addFilesToZip(outputStream, file);
            }
        } else {
            final ZipArchiveEntry zipArchiveEntry = new ZipArchiveEntry(fileToAdd.getPath());
            zipArchiveEntry.setMethod(ZipEntry.DEFLATED);

            outputStream.putArchiveEntry(zipArchiveEntry);
            try {
                outputStream.write(fileToAdd);
            } finally {
                outputStream.closeArchiveEntry();
            }
        }
    }

    private void assertSameFileContents(final File expectedFile, final File actualFile) throws IOException {
        final int size = (int) Math.max(expectedFile.length(), actualFile.length());
        try (ZipFile expected = newZipFile(expectedFile);
                ZipFile actual = newZipFile(actualFile)) {
            final byte[] expectedBuf = new byte[size];
            final byte[] actualBuf = new byte[size];

            final Enumeration<ZipArchiveEntry> actualInOrder = actual.getEntriesInPhysicalOrder();
            final Enumeration<ZipArchiveEntry> expectedInOrder = expected.getEntriesInPhysicalOrder();

            while (actualInOrder.hasMoreElements()) {
                final ZipArchiveEntry actualElement = actualInOrder.nextElement();
                final ZipArchiveEntry expectedElement = expectedInOrder.nextElement();
                assertEquals(expectedElement.getName(), actualElement.getName());
                // Don't compare timestamps since they may vary;
                // there's no support for stubbed out clock (TimeSource) in ZipArchiveOutputStream
                assertEquals(expectedElement.getMethod(), actualElement.getMethod());
                assertEquals(expectedElement.getGeneralPurposeBit(), actualElement.getGeneralPurposeBit());
                assertEquals(expectedElement.getCrc(), actualElement.getCrc());
                assertEquals(expectedElement.getCompressedSize(), actualElement.getCompressedSize());
                assertEquals(expectedElement.getSize(), actualElement.getSize());
                assertEquals(expectedElement.getExternalAttributes(), actualElement.getExternalAttributes());
                assertEquals(expectedElement.getInternalAttributes(), actualElement.getInternalAttributes());

                try (InputStream actualIs = actual.getInputStream(actualElement);
                        InputStream expectedIs = expected.getInputStream(expectedElement)) {
                    org.apache.commons.compress.utils.IOUtils.readFully(expectedIs, expectedBuf);
                    org.apache.commons.compress.utils.IOUtils.readFully(actualIs, actualBuf);
                }
                assertArrayEquals(expectedBuf, actualBuf); // Buffers are larger than payload. don't care
            }

        }
    }

    private int countNonDirectories(final File file) {
        if (!file.isDirectory()) {
            return 1;
        }

        int result = 0;
        for (final File fileInDirectory : file.listFiles()) {
            result += countNonDirectories(fileInDirectory);
        }

        return result;
    }

    private void createArchiveEntry(final String payload, final ZipArchiveOutputStream zos, final String name) throws IOException {
        final ZipArchiveEntry in = new ZipArchiveEntry(name);
        zos.putArchiveEntry(in);

        zos.write(payload.getBytes());
        zos.closeArchiveEntry();
    }

    private byte[] createArtificialData(final int size) {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (int i = 0; i < size; i += 1) {
            output.write((byte) ((i & 1) == 0 ? i / 2 % 256 : i / 2 / 256));
        }
        return output.toByteArray();
    }

    private ZipArchiveOutputStream createFirstEntry(final ZipArchiveOutputStream zos) throws IOException {
        createArchiveEntry(first_payload, zos, "file1.txt");
        return zos;
    }

    private File createReferenceFile(final Zip64Mode zipMode, final String prefix) throws IOException {
        final File reference = createTempFile(prefix, ".zip");
        try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(reference)) {
            zos.setUseZip64(zipMode);
            createFirstEntry(zos);
            createSecondEntry(zos);
        }
        return reference;
    }

    private ZipArchiveOutputStream createSecondEntry(final ZipArchiveOutputStream zos) throws IOException {
        createArchiveEntry(second_payload, zos, "file2.txt");
        return zos;
    }

    private void createTestSplitZipSegments() throws IOException {
        final File directoryToZip = getFilesToZip();
        final File outputZipFile = newTempFile("splitZip.zip");
        final long splitSize = 100 * 1024L; /* 100 KB */
        try (ZipArchiveOutputStream zipArchiveOutputStream = new ZipArchiveOutputStream(outputZipFile, splitSize)) {
            addFilesToZip(zipArchiveOutputStream, directoryToZip);
        }
    }

    private File getFilesToZip() throws IOException {
        final File originalZipFile = getFile("COMPRESS-477/split_zip_created_by_zip/zip_to_compare_created_by_zip.zip");
        try (ZipFile zipFile = newZipFile(originalZipFile)) {
            zipFile.stream().filter(e -> !e.isDirectory()).forEach(zipEntry -> {
                Path outputFile = newTempPath(zipEntry.getName());
                if (!Files.exists(outputFile.getParent())) {
                    Files.createDirectories(outputFile.getParent());
                }
                outputFile = newTempPath(zipEntry.getName());
                try (InputStream inputStream = zipFile.getInputStream(zipEntry);
                        OutputStream outputStream = Files.newOutputStream(outputFile)) {
                    IOUtils.copy(inputStream, outputStream);
                }
            });
        }
        return getTempDirFile().listFiles()[0];
    }

    private ZipFile newZipFile(final File file) throws IOException {
        return ZipFile.builder().setFile(file).get();
    }

    private void readStream(final InputStream in, final ArchiveEntry entry, final Map<String, List<List<Long>>> map) throws IOException {
        final InputStreamStatistics stats = (InputStreamStatistics) in;
        IOUtils.consume(in);
        final String name = entry.getName();
        final List<List<Long>> list = map.computeIfAbsent(name, k -> new ArrayList<>());
        final long t = stats.getUncompressedCount();
        final long b = stats.getCompressedCount();
        list.add(Arrays.asList(t, b));
    }

    /**
     * Tests split archive with 32-bit limit, both STORED and DEFLATED.
     */
    @Test
    void testBuildArtificialSplitZip32Test() throws IOException {
        final File outputZipFile = newTempFile("artificialSplitZip.zip");
        final long splitSize = 64 * 1024L; /* 64 KB */
        try (ZipArchiveOutputStream outputStream = new ZipArchiveOutputStream(outputZipFile, splitSize)) {
            outputStream.setUseZip64(Zip64Mode.Never);
            final ZipArchiveEntry ze1 = new ZipArchiveEntry("file01");
            ze1.setMethod(ZipEntry.STORED);
            outputStream.putArchiveEntry(ze1);
            outputStream.write(createArtificialData(65536));
            outputStream.closeArchiveEntry();
            final ZipArchiveEntry ze2 = new ZipArchiveEntry("file02");
            ze2.setMethod(ZipEntry.DEFLATED);
            outputStream.putArchiveEntry(ze2);
            outputStream.write(createArtificialData(65536));
            outputStream.closeArchiveEntry();
        }

        try (ZipFile zipFile = ZipFile.builder()
                .setPath(outputZipFile.toPath())
                .setMaxNumberOfDisks(Integer.MAX_VALUE)
                .get()
        ) {
            assertArrayEquals(createArtificialData(65536), IOUtils.toByteArray(zipFile.getInputStream(zipFile.getEntry("file01"))));
            assertArrayEquals(createArtificialData(65536), IOUtils.toByteArray(zipFile.getInputStream(zipFile.getEntry("file02"))));
        }
    }

    /**
     * Tests split archive with 64-bit limit, both STORED and DEFLATED.
     */
    @Test
    void testBuildArtificialSplitZip64Test() throws IOException {
        final File outputZipFile = newTempFile("artificialSplitZip.zip");
        final long splitSize = 64 * 1024L; /* 64 KB */
        final byte[] data = createArtificialData(128 * 1024);
        try (ZipArchiveOutputStream outputStream = new ZipArchiveOutputStream(outputZipFile, splitSize)) {
            outputStream.setUseZip64(Zip64Mode.Always);
            final ZipArchiveEntry ze1 = new ZipArchiveEntry("file01");
            ze1.setMethod(ZipEntry.STORED);
            outputStream.putArchiveEntry(ze1);
            outputStream.write(data);
            outputStream.closeArchiveEntry();
            final ZipArchiveEntry ze2 = new ZipArchiveEntry("file02");
            ze2.setMethod(ZipEntry.DEFLATED);
            outputStream.putArchiveEntry(ze2);
            outputStream.write(data);
            outputStream.closeArchiveEntry();
        }
        try (ZipFile zipFile = ZipFile.builder().setPath(outputZipFile.toPath()).setMaxNumberOfDisks(Integer.MAX_VALUE).get()) {
            assertArrayEquals(data, IOUtils.toByteArray(zipFile.getInputStream(zipFile.getEntry("file01"))));
            assertArrayEquals(data, IOUtils.toByteArray(zipFile.getInputStream(zipFile.getEntry("file02"))));
        }
    }

    /**
     * Tests split archive with 32-bit limit, with end of central directory skipping lack of space in segment.
     */
    @Test
    void testBuildSplitZip32_endOfCentralDirectorySkipBoundary() throws IOException {
        final File outputZipFile = newTempFile("artificialSplitZip.zip");
        final long splitSize = 64 * 1024L; /* 64 KB */
        // 4 is PK signature, 36 is size of header + local file header,
        // 36 is length of central directory entry
        // 1 is remaining byte in first archive, this should be skipped
        final byte[] data1 = createArtificialData(64 * 1024 - 4 - 36 - 52 - 1);
        try (ZipArchiveOutputStream outputStream = new ZipArchiveOutputStream(outputZipFile, splitSize)) {
            outputStream.setUseZip64(Zip64Mode.Never);
            final ZipArchiveEntry ze1 = new ZipArchiveEntry("file01");
            ze1.setMethod(ZipEntry.STORED);
            outputStream.putArchiveEntry(ze1);
            outputStream.write(data1);
            outputStream.closeArchiveEntry();
        }
        assertEquals(64 * 1024L - 1, Files.size(outputZipFile.toPath().getParent().resolve("artificialSplitZip.z01")));
        try (ZipFile zipFile = ZipFile.builder().setPath(outputZipFile.toPath()).setMaxNumberOfDisks(Integer.MAX_VALUE).get()) {
            assertArrayEquals(data1, IOUtils.toByteArray(zipFile.getInputStream(zipFile.getEntry("file01"))));
        }
    }

    /**
     * Tests split archive with 32-bit limit, with file local headers crossing segment boundaries.
     */
    @Test
    void testBuildSplitZip32_metaCrossBoundary() throws IOException {
        final File outputZipFile = newTempFile("artificialSplitZip.zip");
        final long splitSize = 64 * 1024L; /* 64 KB */
        // 4 is PK signature, 36 is size of header + local file header,
        // 15 is next local file header up to second byte of CRC
        final byte[] data1 = createArtificialData(64 * 1024 - 4 - 36 - 15);
        // 21 is remaining size of second local file header
        // 19 is next local file header up to second byte of compressed size
        final byte[] data2 = createArtificialData(64 * 1024 - 21 - 19);
        // 17 is remaining size of third local file header
        // 23 is next local file header up to second byte of uncompressed size
        final byte[] data3 = createArtificialData(64 * 1024 - 17 - 23);
        // 13 is remaining size of third local file header
        // 1 is to wrap to next part
        final byte[] data4 = createArtificialData(64 * 1024 - 13 + 1);
        try (ZipArchiveOutputStream outputStream = new ZipArchiveOutputStream(outputZipFile, splitSize)) {
            outputStream.setUseZip64(Zip64Mode.Never);
            final ZipArchiveEntry ze1 = new ZipArchiveEntry("file01");
            ze1.setMethod(ZipEntry.STORED);
            outputStream.putArchiveEntry(ze1);
            outputStream.write(data1);
            outputStream.closeArchiveEntry();
            final ZipArchiveEntry ze2 = new ZipArchiveEntry("file02");
            ze2.setMethod(ZipEntry.STORED);
            outputStream.putArchiveEntry(ze2);
            outputStream.write(data2);
            outputStream.closeArchiveEntry();
            final ZipArchiveEntry ze3 = new ZipArchiveEntry("file03");
            ze3.setMethod(ZipEntry.STORED);
            outputStream.putArchiveEntry(ze3);
            outputStream.write(data3);
            outputStream.closeArchiveEntry();
            final ZipArchiveEntry ze4 = new ZipArchiveEntry("file04");
            ze4.setMethod(ZipEntry.STORED);
            outputStream.putArchiveEntry(ze4);
            outputStream.write(data4);
            outputStream.closeArchiveEntry();
        }
        try (ZipFile zipFile = ZipFile.builder().setPath(outputZipFile.toPath()).setMaxNumberOfDisks(Integer.MAX_VALUE).get()) {
            assertArrayEquals(data1, IOUtils.toByteArray(zipFile.getInputStream(zipFile.getEntry("file01"))));
            assertArrayEquals(data2, IOUtils.toByteArray(zipFile.getInputStream(zipFile.getEntry("file02"))));
            assertArrayEquals(data3, IOUtils.toByteArray(zipFile.getInputStream(zipFile.getEntry("file03"))));
            assertArrayEquals(data4, IOUtils.toByteArray(zipFile.getInputStream(zipFile.getEntry("file04"))));
        }
    }

    @Test
    void testBuildSplitZipTest() throws IOException {
        final File directoryToZip = getFilesToZip();
        createTestSplitZipSegments();
        final File lastFile = newTempFile("splitZip.zip");
        try (SeekableByteChannel channel = ZipSplitReadOnlySeekableByteChannel.buildFromLastSplitSegment(lastFile);
                InputStream inputStream = Channels.newInputStream(channel);
                ZipArchiveInputStream splitInputStream = ZipArchiveInputStream.builder()
                        .setInputStream(inputStream)
                        .setSkipSplitSig(true)
                        .get()) {

            ArchiveEntry entry;
            final int filesNum = countNonDirectories(directoryToZip);
            int filesCount = 0;
            while ((entry = splitInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                // compare all files one by one
                assertArrayEquals(IOUtils.toByteArray(splitInputStream), Files.readAllBytes(Paths.get(entry.getName())));
                filesCount++;
            }
            // and the number of files should equal
            assertEquals(filesCount, filesNum);
        }
    }

    @Test
    void testBuildSplitZipWithSegmentAlreadyExistThrowsException() throws IOException {
        final File directoryToZip = getFilesToZip();
        final File outputZipFile = newTempFile("splitZip.zip");
        final long splitSize = 100 * 1024L; /* 100 KB */
        try (ZipArchiveOutputStream zipArchiveOutputStream = new ZipArchiveOutputStream(outputZipFile, splitSize)) {
            // create a file that has the same name of one of the created split segments
            final File sameNameFile = newTempFile("splitZip.z01");
            sameNameFile.createNewFile();
            assertThrows(ArchiveException.class, () -> addFilesToZip(zipArchiveOutputStream, directoryToZip));
        } catch (final Exception e) {
            // Ignore:
            // java.io.IOException: This archive contains unclosed entries.
            // at org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream.finish(ZipArchiveOutputStream.java:563)
            // at org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream.close(ZipArchiveOutputStream.java:1119)
            // at org.apache.commons.compress.archivers.ZipTestCase.buildSplitZipWithSegmentAlreadyExistThrowsException(ZipTestCase.java:715)
        }
    }

    @Test
    void testBuildSplitZipWithTooLargeSizeThrowsException() throws IOException {
        final Path file = Files.createTempFile("temp", "zip");
        try {
            assertThrows(IllegalArgumentException.class, () -> new ZipArchiveOutputStream(file, 4294967295L + 1));
        } finally {
            Files.delete(file);
        }
    }

    @Test
    void testBuildSplitZipWithTooSmallSizeThrowsException() throws IOException {
        createTempFile("temp", "zip").toPath();
        assertThrows(IllegalArgumentException.class, () -> new ZipArchiveOutputStream(createTempFile("temp", "zip"), 64 * 1024 - 1));
    }

    @Test
    void testCopyRawEntriesFromFile() throws IOException {
        final File reference = createReferenceFile(Zip64Mode.Never, "expected.");
        final File file1 = createTempFile("src1.", ".zip");
        try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(file1)) {
            zos.setUseZip64(Zip64Mode.Never);
            createFirstEntry(zos).close();
        }
        final File file2 = createTempFile("src2.", ".zip");
        try (ZipArchiveOutputStream zos1 = new ZipArchiveOutputStream(file2)) {
            zos1.setUseZip64(Zip64Mode.Never);
            createSecondEntry(zos1).close();
        }
        try (ZipFile zipFile1 = newZipFile(file1);
                ZipFile zipFile2 = newZipFile(file2)) {
            final File fileResult = createTempFile("file-actual.", ".zip");
            try (ZipArchiveOutputStream zos2 = new ZipArchiveOutputStream(fileResult)) {
                zipFile1.copyRawEntries(zos2, allFilesPredicate);
                zipFile2.copyRawEntries(zos2, allFilesPredicate);
            }
            // copyRawEntries does not add superfluous zip64 header like regular ZIP output stream
            // does when using Zip64Mode.AsNeeded so all the source material has to be Zip64Mode.Never,
            // if exact binary equality is to be achieved
            assertSameFileContents(reference, fileResult);
        }
    }

    @Test
    void testCopyRawZip64EntryFromFile() throws IOException {
        final File reference = createTempFile("z64reference.", ".zip");
        try (ZipArchiveOutputStream zos1 = new ZipArchiveOutputStream(reference)) {
            zos1.setUseZip64(Zip64Mode.Always);
            createFirstEntry(zos1);
        }
        final File file1 = createTempFile("zip64src.", ".zip");
        try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(file1)) {
            zos.setUseZip64(Zip64Mode.Always);
            createFirstEntry(zos).close();
        }
        final File fileResult = createTempFile("file-actual.", ".zip");
        try (ZipFile zipFile1 = newZipFile(file1)) {
            try (ZipArchiveOutputStream zos2 = new ZipArchiveOutputStream(fileResult)) {
                zos2.setUseZip64(Zip64Mode.Always);
                zipFile1.copyRawEntries(zos2, allFilesPredicate);
            }
            assertSameFileContents(reference, fileResult);
        }
    }

    @Test
    void testDirectoryEntryFromFile() throws Exception {
        final File tmp = getTempDirFile();
        final File archive = createTempFile("test.", ".zip");
        final long beforeArchiveWrite;
        try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(archive)) {
            beforeArchiveWrite = tmp.lastModified();
            final ZipArchiveEntry in = new ZipArchiveEntry(tmp, "foo");
            zos.putArchiveEntry(in);
            zos.closeArchiveEntry();
        }
        try (ZipFile zf = newZipFile(archive)) {
            final ZipArchiveEntry out = zf.getEntry("foo/");
            assertNotNull(out);
            assertEquals("foo/", out.getName());
            assertEquals(0, out.getSize());
            // ZIP stores time with a granularity of 2 seconds
            assertEquals(beforeArchiveWrite / 2000, out.getLastModifiedDate().getTime() / 2000);
            assertTrue(out.isDirectory());
        }
    }

    @Test
    void testExplicitDirectoryEntry() throws Exception {
        final File archive = createTempFile("test.", ".zip");
        final long beforeArchiveWrite;
        try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(archive)) {
            beforeArchiveWrite = getTempDirFile().lastModified();
            final ZipArchiveEntry in = new ZipArchiveEntry("foo/");
            in.setTime(beforeArchiveWrite);
            zos.putArchiveEntry(in);
            zos.closeArchiveEntry();
        }
        try (ZipFile zf = newZipFile(archive)) {
            final ZipArchiveEntry out = zf.getEntry("foo/");
            assertNotNull(out);
            assertEquals("foo/", out.getName());
            assertEquals(0, out.getSize());
            assertEquals(beforeArchiveWrite / 2000, out.getLastModifiedDate().getTime() / 2000);
            assertTrue(out.isDirectory());
        }
    }

    @Test
    void testExplicitFileEntry() throws Exception {
        final File file = createTempFile();
        final File archive = createTempFile("test.", ".zip");
        try (ZipArchiveOutputStream outputStream = new ZipArchiveOutputStream(archive)) {
            final ZipArchiveEntry in = new ZipArchiveEntry("foo");
            in.setTime(file.lastModified());
            in.setSize(file.length());
            outputStream.putArchiveEntry(in);
            outputStream.write(file);
            outputStream.closeArchiveEntry();
        }
        try (ZipFile zf = newZipFile(archive)) {
            final ZipArchiveEntry out = zf.getEntry("foo");
            assertNotNull(out);
            assertEquals("foo", out.getName());
            assertEquals(file.length(), out.getSize());
            assertEquals(file.lastModified() / 2000, out.getLastModifiedDate().getTime() / 2000);
            assertFalse(out.isDirectory());
        }
    }

    @Test
    void testFileEntryFromFile() throws Exception {
        final File file = createTempFile();
        final File archive = createTempFile("test.", ".zip");
        try (ZipArchiveOutputStream outputStream = new ZipArchiveOutputStream(archive)) {
            final ZipArchiveEntry in = new ZipArchiveEntry(file, "foo");
            outputStream.putArchiveEntry(in);
            outputStream.write(file);
            outputStream.closeArchiveEntry();
        }
        try (ZipFile zf = newZipFile(archive)) {
            final ZipArchiveEntry out = zf.getEntry("foo");
            assertNotNull(out);
            assertEquals("foo", out.getName());
            assertEquals(file.length(), out.getSize());
            assertEquals(file.lastModified() / 2000, out.getLastModifiedDate().getTime() / 2000);
            assertFalse(out.isDirectory());
        }
    }

    private void testInputStreamStatistics(final String fileName, final Map<String, List<Long>> expectedStatistics) throws IOException, ArchiveException {
        final File input = getFile(fileName);
        final Map<String, List<List<Long>>> actualStatistics = new HashMap<>();
        // stream access
        try (InputStream fis = Files.newInputStream(input.toPath());
                ArchiveInputStream<?> in = ArchiveStreamFactory.DEFAULT.createArchiveInputStream("zip", fis)) {
            in.forEach(entry -> readStream(in, entry, actualStatistics));
        }
        // file access
        try (ZipFile zipFile = newZipFile(input)) {
            zipFile.stream().forEach(zae -> {
                try (InputStream in = zipFile.getInputStream(zae)) {
                    readStream(in, zae, actualStatistics);
                }
            });
        }
        // compare statistics of stream / file access
        for (final Map.Entry<String, List<List<Long>>> me : actualStatistics.entrySet()) {
            assertEquals(me.getValue().get(0), me.getValue().get(1), "Mismatch of stats for: " + me.getKey());
        }
        for (final Map.Entry<String, List<Long>> me : expectedStatistics.entrySet()) {
            assertEquals(me.getValue(), actualStatistics.get(me.getKey()).get(0), "Mismatch of stats with expected value for: " + me.getKey());
        }
    }

    @Test
    void testInputStreamStatisticsForBzip2Entry() throws IOException, ArchiveException {
        final Map<String, List<Long>> expected = new HashMap<>();
        expected.put("lots-of-as", Arrays.asList(42L, 39L));
        testInputStreamStatistics("bzip2-zip.zip", expected);
    }

    @Test
    void testInputStreamStatisticsForDeflate64Entry() throws IOException, ArchiveException {
        final Map<String, List<Long>> expected = new HashMap<>();
        expected.put("input2", Arrays.asList(3072L, 2111L));
        testInputStreamStatistics("COMPRESS-380/COMPRESS-380.zip", expected);
    }

    @Test
    void testInputStreamStatisticsForImplodedEntry() throws IOException, ArchiveException {
        final Map<String, List<Long>> expected = new HashMap<>();
        expected.put("LICENSE.TXT", Arrays.asList(11560L, 4131L));
        testInputStreamStatistics("imploding-8Kdict-3trees.zip", expected);
    }

    @Test
    void testInputStreamStatisticsForShrunkEntry() throws IOException, ArchiveException {
        final Map<String, List<Long>> expected = new HashMap<>();
        expected.put("TEST1.XML", Arrays.asList(76L, 66L));
        expected.put("TEST2.XML", Arrays.asList(81L, 76L));
        testInputStreamStatistics("SHRUNK.ZIP", expected);
    }

    @Test
    void testInputStreamStatisticsForStoredEntry() throws IOException, ArchiveException {
        final Map<String, List<Long>> expected = new HashMap<>();
        expected.put("test.txt", Arrays.asList(5L, 5L));
        testInputStreamStatistics("COMPRESS-264.zip", expected);
    }

    @Test
    void testInputStreamStatisticsOfZipBombExcel() throws IOException, ArchiveException {
        final Map<String, List<Long>> expected = new HashMap<>();
        expected.put("[Content_Types].xml", Arrays.asList(8390036L, 8600L));
        expected.put("xl/worksheets/sheet1.xml", Arrays.asList(1348L, 508L));
        testInputStreamStatistics("zipbomb.xlsx", expected);
    }

    /**
     * Checks if all entries from a nested archive can be read. The archive: OSX_ArchiveWithNestedArchive.zip contains: NestedArchiv.zip and test.xml3.
     *
     * The nested archive: NestedArchive.zip contains test1.xml and test2.xml
     *
     * @throws Exception
     */
    @Test
    void testListAllFilesWithNestedArchive() throws Exception {
        final File input = getFile("OSX_ArchiveWithNestedArchive.zip");
        final List<String> results = new ArrayList<>();
        final List<ZipException> expectedExceptions = new ArrayList<>();
        try (InputStream fis = Files.newInputStream(input.toPath());
                ZipArchiveInputStream in = ArchiveStreamFactory.DEFAULT.createArchiveInputStream("zip", fis)) {
            ZipArchiveEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                results.add(entry.getName());
                final ZipArchiveInputStream nestedIn = ArchiveStreamFactory.DEFAULT.createArchiveInputStream("zip", in);
                try {
                    ZipArchiveEntry nestedEntry;
                    while ((nestedEntry = nestedIn.getNextEntry()) != null) {
                        results.add(nestedEntry.getName());
                    }
                } catch (final ZipException ex) {
                    // expected since you cannot create a final ArchiveInputStream from test3.xml
                    expectedExceptions.add(ex);
                }
                // nested stream must not be closed here
            }
        }
        assertTrue(results.contains("NestedArchiv.zip"));
        assertTrue(results.contains("test1.xml"));
        assertTrue(results.contains("test2.xml"));
        assertTrue(results.contains("test3.xml"));
        assertEquals(1, expectedExceptions.size());
    }

    /**
     * Test case for being able to skip an entry in an {@link ZipArchiveInputStream} even if the compression method of that entry is unsupported.
     *
     * @see <a href="https://issues.apache.org/jira/browse/COMPRESS-93">COMPRESS-93</a>
     */
    @Test
    void testSkipEntryWithUnsupportedCompressionMethod() throws IOException {
        try (ZipArchiveInputStream zip = ZipArchiveInputStream.builder().setURI(getURI("moby.zip")).get()) {
            final ZipArchiveEntry entry = zip.getNextZipEntry();
            assertEquals(ZipMethod.TOKENIZATION.getCode(), entry.getMethod(), "method");
            assertEquals("README", entry.getName());
            assertFalse(zip.canReadEntryData(entry));
            assertDoesNotThrow(() -> assertNull(zip.getNextZipEntry()), "COMPRESS-93: Unable to skip an unsupported ZIP entry");
        }
    }

    /**
     * Test case for <a href="https://issues.apache.org/jira/browse/COMPRESS-208">COMPRESS-208</a>.
     */
    @Test
    void testSkipsPK00Prefix() throws Exception {
        final ArrayList<String> al = new ArrayList<>();
        al.add("test1.xml");
        al.add("test2.xml");
        try (ZipArchiveInputStream inputStream = ZipArchiveInputStream.builder()
                .setURI(getURI("COMPRESS-208.zip"))
                .get()) {
            checkArchiveContent(inputStream, al);
        }
    }

    /**
     * Test case for <a href="https://issues.apache.org/jira/browse/COMPRESS-93">COMPRESS-93</a>.
     */
    @Test
    void testTokenizationCompressionMethod() throws IOException {
        try (ZipFile moby = ZipFile.builder().setFile(getFile("moby.zip")).get()) {
            final ZipArchiveEntry entry = moby.getEntry("README");
            assertEquals(ZipMethod.TOKENIZATION.getCode(), entry.getMethod(), "method");
            assertFalse(moby.canReadEntryData(entry));
        }
    }

    @Test
    void testUnixModeInAddRaw() throws IOException {
        final File file1 = createTempFile("unixModeBits.", ".zip");
        try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(file1)) {
            final ZipArchiveEntry archiveEntry = new ZipArchiveEntry("fred");
            archiveEntry.setUnixMode(0664);
            archiveEntry.setMethod(ZipEntry.STORED);
            archiveEntry.setSize(3);
            archiveEntry.setCompressedSize(3);
            zos.addRawArchiveEntry(archiveEntry, new ByteArrayInputStream("fud".getBytes()));
        }
        try (ZipFile zf1 = newZipFile(file1)) {
            final ZipArchiveEntry fred = zf1.getEntry("fred");
            assertEquals(0664, fred.getUnixMode());
        }
    }

    @Test
    void testUnsupportedCompressionMethodInAddRaw() throws IOException {
        final File file1 = createTempFile("unsupportedCompressionMethod.", ".zip");
        try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(file1)) {
            final ZipArchiveEntry archiveEntry = new ZipArchiveEntry("fred");
            archiveEntry.setMethod(Integer.MAX_VALUE);
            archiveEntry.setSize(3);
            archiveEntry.setCompressedSize(3);
            archiveEntry.setCrc(0);
            zos.addRawArchiveEntry(archiveEntry, new ByteArrayInputStream("fud".getBytes()));
        }
    }

    /**
     * Archives 2 files and unarchives it again. If the file length of result and source is the same, it looks like the operations have worked
     *
     * @throws Exception
     */
    @Test
    void testZipArchiveCreation() throws Exception {
        // Archive
        final File output = newTempFile("bla.zip");
        final File file1 = getFile("test1.xml");
        final File file2 = getFile("test2.xml");
        try (OutputStream out = Files.newOutputStream(output.toPath())) {
            try (ArchiveOutputStream<ZipArchiveEntry> os = ArchiveStreamFactory.DEFAULT.createArchiveOutputStream("zip", out)) {
                // entry 1
                os.putArchiveEntry(new ZipArchiveEntry("testdata/test1.xml"));
                os.write(file1);
                os.closeArchiveEntry();
                // entry 2
                os.putArchiveEntry(new ZipArchiveEntry("testdata/test2.xml"));
                os.write(file2);
                os.closeArchiveEntry();
            }
        }
        // Unarchive the same
        final List<File> results = new ArrayList<>();
        try (InputStream fileInputStream = Files.newInputStream(output.toPath())) {
            try (ArchiveInputStream<ZipArchiveEntry> archiveInputStream = ArchiveStreamFactory.DEFAULT.createArchiveInputStream("zip", fileInputStream)) {
                archiveInputStream.forEach(entry -> {
                    final File outfile = new File(tempResultDir.getCanonicalPath() + "/result/" + entry.getName());
                    outfile.getParentFile().mkdirs();
                    Files.copy(archiveInputStream, outfile.toPath());
                    results.add(outfile);
                });
            }
        }
        assertEquals(results.size(), 2);
        File result = results.get(0);
        assertEquals(file1.length(), result.length());
        result = results.get(1);
        assertEquals(file2.length(), result.length());
    }

    /**
     * Archives 2 files and unarchives it again. If the file contents of result and source is the same, it looks like the operations have worked
     *
     * @throws Exception
     */
    @Test
    void testZipArchiveCreationInMemory() throws Exception {
        final byte[] file1Contents = readAllBytes("test1.xml");
        final byte[] file2Contents = readAllBytes("test2.xml");
        final List<byte[]> results = new ArrayList<>();
        try (SeekableInMemoryByteChannel channel = new SeekableInMemoryByteChannel()) {
            try (ZipArchiveOutputStream os = new ZipArchiveOutputStream(channel)) {
                os.putArchiveEntry(new ZipArchiveEntry("testdata/test1.xml"));
                os.write(file1Contents);
                os.closeArchiveEntry();

                os.putArchiveEntry(new ZipArchiveEntry("testdata/test2.xml"));
                os.write(file2Contents);
                os.closeArchiveEntry();
            }
            // Unarchive the same
            try (ZipArchiveInputStream inputStream = ArchiveStreamFactory.DEFAULT.createArchiveInputStream("zip", new ByteArrayInputStream(channel.array()))) {
                ZipArchiveEntry entry;
                while ((entry = inputStream.getNextEntry()) != null) {
                    final byte[] result = new byte[(int) entry.getSize()];
                    IOUtils.readFully(inputStream, result);
                    results.add(result);
                }
            }
        }
        assertArrayEquals(results.get(0), file1Contents);
        assertArrayEquals(results.get(1), file2Contents);
    }

    @Test
    void testZipArchiveEntryNewFromPath() throws Exception {
        final Path archivePath;
        final File tmpFile = createTempFile();
        final Path tmpFilePath = tmpFile.toPath();
        final File archiveFile = createTempFile("test.", ".zip");
        archivePath = archiveFile.toPath();
        try (ZipArchiveOutputStream outputStream = new ZipArchiveOutputStream(archivePath)) {
            final ZipArchiveEntry in = outputStream.createArchiveEntry(tmpFilePath, "foo");
            outputStream.putArchiveEntry(in);
            outputStream.write(tmpFilePath);
            outputStream.closeArchiveEntry();
        }
        try (ZipFile zf = newZipFile(archiveFile)) {
            final ZipArchiveEntry out = zf.getEntry("foo");
            assertNotNull(out);
            assertEquals("foo", out.getName());
            assertEquals(tmpFile.length(), out.getSize());
            assertEquals(tmpFile.lastModified() / 2000, out.getLastModifiedDate().getTime() / 2000);
            assertFalse(out.isDirectory());
        }
    }

    /**
     * Simple unarchive test. Asserts nothing.
     *
     * @throws Exception
     */
    @Test
    void testZipUnarchive() throws Exception {
        final File input = getFile("bla.zip");
        try (InputStream is = Files.newInputStream(input.toPath());
                ArchiveInputStream<ZipArchiveEntry> in = ArchiveStreamFactory.DEFAULT.createArchiveInputStream("zip", is)) {
            final ZipArchiveEntry entry = in.getNextEntry();
            Files.copy(in, newTempFile(entry.getName()).toPath());
        }
    }
}
