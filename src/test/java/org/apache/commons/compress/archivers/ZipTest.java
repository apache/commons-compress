/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.compress.archivers;

import static java.nio.charset.StandardCharsets.UTF_8;
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
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.compress.utils.InputStreamStatistics;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.junit.jupiter.api.Test;

public final class ZipTest extends AbstractTest {

    final String first_payload = "ABBA";

    final String second_payload = "AAAAAAAAAAAA";

    final ZipArchiveEntryPredicate allFilesPredicate = zipArchiveEntry -> true;

    private void addFilesToZip(final ZipArchiveOutputStream zipArchiveOutputStream, final File fileToAdd) throws IOException {
        if (fileToAdd.isDirectory()) {
            for (final File file : fileToAdd.listFiles()) {
                addFilesToZip(zipArchiveOutputStream, file);
            }
        } else {
            final ZipArchiveEntry zipArchiveEntry = new ZipArchiveEntry(fileToAdd.getPath());
            zipArchiveEntry.setMethod(ZipEntry.DEFLATED);

            zipArchiveOutputStream.putArchiveEntry(zipArchiveEntry);
            try {
                Files.copy(fileToAdd.toPath(), zipArchiveOutputStream);
            } finally {
                zipArchiveOutputStream.closeArchiveEntry();
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
                    IOUtils.readFully(expectedIs, expectedBuf);
                    IOUtils.readFully(actualIs, actualBuf);
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

    private byte[] createArtificialData(int size) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (int i = 0; i < size; i += 1) {
            output.write((byte) ((i & 1) == 0 ? (i / 2 % 256) : (i / 2 / 256)));
        }
        return output.toByteArray();
    }

    private void createArchiveEntry(final String payload, final ZipArchiveOutputStream zos, final String name) throws IOException {
        final ZipArchiveEntry in = new ZipArchiveEntry(name);
        zos.putArchiveEntry(in);

        zos.write(payload.getBytes());
        zos.closeArchiveEntry();
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
            final Enumeration<ZipArchiveEntry> zipEntries = zipFile.getEntries();
            ZipArchiveEntry zipEntry;
            File outputFile;
            byte[] buffer;
            int readLen;

            while (zipEntries.hasMoreElements()) {
                zipEntry = zipEntries.nextElement();
                if (zipEntry.isDirectory()) {
                    continue;
                }

                outputFile = newTempFile(zipEntry.getName());
                if (!outputFile.getParentFile().exists()) {
                    outputFile.getParentFile().mkdirs();
                }
                outputFile = newTempFile(zipEntry.getName());

                try (InputStream inputStream = zipFile.getInputStream(zipEntry);
                        OutputStream outputStream = Files.newOutputStream(outputFile.toPath())) {
                    buffer = new byte[(int) zipEntry.getSize()];
                    while ((readLen = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, readLen);
                    }
                }
            }
        }
        return getTempDirFile().listFiles()[0];
    }

    private ZipFile newZipFile(final File file) throws IOException {
        return ZipFile.builder().setFile(file).get();
    }

    private void readStream(final InputStream in, final ArchiveEntry entry, final Map<String, List<List<Long>>> map) throws IOException {
        final byte[] buf = new byte[4096];
        final InputStreamStatistics stats = (InputStreamStatistics) in;
        while (in.read(buf) != -1) {
            // consume all.
        }

        final String name = entry.getName();
        final List<List<Long>> list = map.computeIfAbsent(name, k -> new ArrayList<>());

        final long t = stats.getUncompressedCount();
        final long b = stats.getCompressedCount();
        list.add(Arrays.asList(t, b));
    }

    @Test
    public void testBuildSplitZipTest() throws IOException {
        final File directoryToZip = getFilesToZip();
        createTestSplitZipSegments();

        final File lastFile = newTempFile("splitZip.zip");
        try (SeekableByteChannel channel = ZipSplitReadOnlySeekableByteChannel.buildFromLastSplitSegment(lastFile);
                InputStream inputStream = Channels.newInputStream(channel);
                ZipArchiveInputStream splitInputStream = new ZipArchiveInputStream(inputStream, UTF_8.toString(), true, false, true)) {

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

    /**
     * Tests split archive with 32-bit limit, both STORED and DEFLATED.
     */
    @Test
    public void testBuildArtificialSplitZip32Test() throws IOException {
        final File outputZipFile = newTempFile("artificialSplitZip.zip");
        final long splitSize = 64 * 1024L; /* 64 KB */
        try (ZipArchiveOutputStream zipArchiveOutputStream = new ZipArchiveOutputStream(outputZipFile, splitSize)) {
            zipArchiveOutputStream.setUseZip64(Zip64Mode.Never);
            ZipArchiveEntry ze1 = new ZipArchiveEntry("file01");
            ze1.setMethod(ZipEntry.STORED);
            zipArchiveOutputStream.putArchiveEntry(ze1);
            zipArchiveOutputStream.write(createArtificialData(65536));
            zipArchiveOutputStream.closeArchiveEntry();
            ZipArchiveEntry ze2 = new ZipArchiveEntry("file02");
            ze2.setMethod(ZipEntry.DEFLATED);
            zipArchiveOutputStream.putArchiveEntry(ze2);
            zipArchiveOutputStream.write(createArtificialData(65536));
            zipArchiveOutputStream.closeArchiveEntry();
        }

        try (ZipFile zipFile = new ZipFile(outputZipFile)) {
            assertArrayEquals(createArtificialData(65536), IOUtils.toByteArray(zipFile.getInputStream(zipFile.getEntry("file01"))));
            assertArrayEquals(createArtificialData(65536), IOUtils.toByteArray(zipFile.getInputStream(zipFile.getEntry("file02"))));
        }
    }

    /**
     * Tests split archive with 64-bit limit, both STORED and DEFLATED.
     */
    @Test
    public void testBuildArtificialSplitZip64Test() throws IOException {
        final File outputZipFile = newTempFile("artificialSplitZip.zip");
        final long splitSize = 64 * 1024L; /* 64 KB */
        byte[] data = createArtificialData(128 * 1024);
        try (ZipArchiveOutputStream zipArchiveOutputStream = new ZipArchiveOutputStream(outputZipFile, splitSize)) {
            zipArchiveOutputStream.setUseZip64(Zip64Mode.Always);
            ZipArchiveEntry ze1 = new ZipArchiveEntry("file01");
            ze1.setMethod(ZipEntry.STORED);
            zipArchiveOutputStream.putArchiveEntry(ze1);
            zipArchiveOutputStream.write(data);
            zipArchiveOutputStream.closeArchiveEntry();
            ZipArchiveEntry ze2 = new ZipArchiveEntry("file02");
            ze2.setMethod(ZipEntry.DEFLATED);
            zipArchiveOutputStream.putArchiveEntry(ze2);
            zipArchiveOutputStream.write(data);
            zipArchiveOutputStream.closeArchiveEntry();
        }

        try (ZipFile zipFile = new ZipFile(outputZipFile)) {
            assertArrayEquals(data, IOUtils.toByteArray(zipFile.getInputStream(zipFile.getEntry("file01"))));
            assertArrayEquals(data, IOUtils.toByteArray(zipFile.getInputStream(zipFile.getEntry("file02"))));
        }
    }

    /**
     * Tests split archive with 32-bit limit, with file local headers crossing segment boundaries.
     */
    @Test
    public void testBuildSplitZip32_metaCrossBoundary() throws IOException {
        final File outputZipFile = newTempFile("artificialSplitZip.zip");
        final long splitSize = 64 * 1024L; /* 64 KB */
        // 4 is PK signature, 36 is size of header + local file header,
        // 15 is next local file header up to second byte of CRC
        byte[] data1 = createArtificialData(64 * 1024 - 4 - 36 - 15);
        // 21 is remaining size of second local file header
        // 19 is next local file header up to second byte of compressed size
        byte[] data2 = createArtificialData(64 * 1024 - 21 - 19);
        // 17 is remaining size of third local file header
        // 23 is next local file header up to second byte of uncompressed size
        byte[] data3 = createArtificialData(64 * 1024 - 17 - 23);
        // 13 is remaining size of third local file header
        // 1 is to wrap to next part
        byte[] data4 = createArtificialData(64 * 1024 - 13 + 1);
        try (ZipArchiveOutputStream zipArchiveOutputStream = new ZipArchiveOutputStream(outputZipFile, splitSize)) {
            zipArchiveOutputStream.setUseZip64(Zip64Mode.Never);
            ZipArchiveEntry ze1 = new ZipArchiveEntry("file01");
            ze1.setMethod(ZipEntry.STORED);
            zipArchiveOutputStream.putArchiveEntry(ze1);
            zipArchiveOutputStream.write(data1);
            zipArchiveOutputStream.closeArchiveEntry();
            ZipArchiveEntry ze2 = new ZipArchiveEntry("file02");
            ze2.setMethod(ZipEntry.STORED);
            zipArchiveOutputStream.putArchiveEntry(ze2);
            zipArchiveOutputStream.write(data2);
            zipArchiveOutputStream.closeArchiveEntry();
            ZipArchiveEntry ze3 = new ZipArchiveEntry("file03");
            ze3.setMethod(ZipEntry.STORED);
            zipArchiveOutputStream.putArchiveEntry(ze3);
            zipArchiveOutputStream.write(data3);
            zipArchiveOutputStream.closeArchiveEntry();
            ZipArchiveEntry ze4 = new ZipArchiveEntry("file04");
            ze4.setMethod(ZipEntry.STORED);
            zipArchiveOutputStream.putArchiveEntry(ze4);
            zipArchiveOutputStream.write(data4);
            zipArchiveOutputStream.closeArchiveEntry();
        }

        try (ZipFile zipFile = new ZipFile(outputZipFile)) {
            assertArrayEquals(data1, IOUtils.toByteArray(zipFile.getInputStream(zipFile.getEntry("file01"))));
            assertArrayEquals(data2, IOUtils.toByteArray(zipFile.getInputStream(zipFile.getEntry("file02"))));
            assertArrayEquals(data3, IOUtils.toByteArray(zipFile.getInputStream(zipFile.getEntry("file03"))));
            assertArrayEquals(data4, IOUtils.toByteArray(zipFile.getInputStream(zipFile.getEntry("file04"))));
        }
    }

    /**
     * Tests split archive with 32-bit limit, with end of central directory skipping lack of space in segment.
     */
    @Test
    public void testBuildSplitZip32_endOfCentralDirectorySkipBoundary() throws IOException {
        final File outputZipFile = newTempFile("artificialSplitZip.zip");
        final long splitSize = 64 * 1024L; /* 64 KB */
        // 4 is PK signature, 36 is size of header + local file header,
        // 36 is length of central directory entry
        // 1 is remaining byte in first archive, this should be skipped
        byte[] data1 = createArtificialData(64 * 1024 - 4 - 36 - 52 - 1);
        try (ZipArchiveOutputStream zipArchiveOutputStream = new ZipArchiveOutputStream(outputZipFile, splitSize)) {
            zipArchiveOutputStream.setUseZip64(Zip64Mode.Never);
            ZipArchiveEntry ze1 = new ZipArchiveEntry("file01");
            ze1.setMethod(ZipEntry.STORED);
            zipArchiveOutputStream.putArchiveEntry(ze1);
            zipArchiveOutputStream.write(data1);
            zipArchiveOutputStream.closeArchiveEntry();
        }

        assertEquals(64 * 1024L - 1, Files.size(outputZipFile.toPath().getParent().resolve("artificialSplitZip.z01")));

        try (ZipFile zipFile = new ZipFile(outputZipFile)) {
            assertArrayEquals(data1, IOUtils.toByteArray(zipFile.getInputStream(zipFile.getEntry("file01"))));
        }
    }

    @Test
    public void testBuildSplitZipWithSegmentAlreadyExistThrowsException() throws IOException {
        final File directoryToZip = getFilesToZip();
        final File outputZipFile = newTempFile("splitZip.zip");
        final long splitSize = 100 * 1024L; /* 100 KB */
        try (ZipArchiveOutputStream zipArchiveOutputStream = new ZipArchiveOutputStream(outputZipFile, splitSize)) {

            // create a file that has the same name of one of the created split segments
            final File sameNameFile = newTempFile("splitZip.z01");
            sameNameFile.createNewFile();

            assertThrows(IOException.class, () -> addFilesToZip(zipArchiveOutputStream, directoryToZip));
        } catch (Exception e) {
            // Ignore:
            // java.io.IOException: This archive contains unclosed entries.
            // at org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream.finish(ZipArchiveOutputStream.java:563)
            // at org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream.close(ZipArchiveOutputStream.java:1119)
            // at org.apache.commons.compress.archivers.ZipTestCase.buildSplitZipWithSegmentAlreadyExistThrowsException(ZipTestCase.java:715)
        }
    }

    @Test
    public void testBuildSplitZipWithTooLargeSizeThrowsException() throws IOException {
        final Path file = Files.createTempFile("temp", "zip");
        try {
            assertThrows(IllegalArgumentException.class, () -> new ZipArchiveOutputStream(file, 4294967295L + 1));
        } finally {
            Files.delete(file);
        }
    }

    @Test
    public void testBuildSplitZipWithTooSmallSizeThrowsException() throws IOException {
        createTempFile("temp", "zip").toPath();
        assertThrows(IllegalArgumentException.class, () -> new ZipArchiveOutputStream(createTempFile("temp", "zip"), 64 * 1024 - 1));
    }

    @Test
    public void testCopyRawEntriesFromFile() throws IOException {

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
    public void testCopyRawZip64EntryFromFile() throws IOException {

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
    public void testDirectoryEntryFromFile() throws Exception {
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
    public void testExplicitDirectoryEntry() throws Exception {
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
    public void testExplicitFileEntry() throws Exception {
        final File tmp = createTempFile();
        final File archive = createTempFile("test.", ".zip");
        try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(archive)) {
            final ZipArchiveEntry in = new ZipArchiveEntry("foo");
            in.setTime(tmp.lastModified());
            in.setSize(tmp.length());
            zos.putArchiveEntry(in);
            final byte[] b = new byte[(int) tmp.length()];
            try (InputStream fis = Files.newInputStream(tmp.toPath())) {
                while (fis.read(b) > 0) {
                    zos.write(b);
                }
            }
            zos.closeArchiveEntry();
        }
        try (ZipFile zf = newZipFile(archive)) {
            final ZipArchiveEntry out = zf.getEntry("foo");
            assertNotNull(out);
            assertEquals("foo", out.getName());
            assertEquals(tmp.length(), out.getSize());
            assertEquals(tmp.lastModified() / 2000, out.getLastModifiedDate().getTime() / 2000);
            assertFalse(out.isDirectory());
        }
    }

    @Test
    public void testFileEntryFromFile() throws Exception {
        final File tmpFile = createTempFile();
        final File archive = createTempFile("test.", ".zip");
        try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(archive)) {
            final ZipArchiveEntry in = new ZipArchiveEntry(tmpFile, "foo");
            zos.putArchiveEntry(in);
            final byte[] b = new byte[(int) tmpFile.length()];
            try (InputStream fis = Files.newInputStream(tmpFile.toPath())) {
                while (fis.read(b) > 0) {
                    zos.write(b);
                }
            }
            zos.closeArchiveEntry();
        }
        try (ZipFile zf = newZipFile(archive)) {
            final ZipArchiveEntry out = zf.getEntry("foo");
            assertNotNull(out);
            assertEquals("foo", out.getName());
            assertEquals(tmpFile.length(), out.getSize());
            assertEquals(tmpFile.lastModified() / 2000, out.getLastModifiedDate().getTime() / 2000);
            assertFalse(out.isDirectory());
        }
    }

    private void testInputStreamStatistics(final String fileName, final Map<String, List<Long>> expectedStatistics) throws IOException, ArchiveException {
        final File input = getFile(fileName);

        final Map<String, List<List<Long>>> actualStatistics = new HashMap<>();

        // stream access
        try (InputStream fis = Files.newInputStream(input.toPath());
                ArchiveInputStream<?> in = ArchiveStreamFactory.DEFAULT.createArchiveInputStream("zip", fis)) {
            for (ArchiveEntry entry; (entry = in.getNextEntry()) != null;) {
                readStream(in, entry, actualStatistics);
            }
        }

        // file access
        try (ZipFile zf = newZipFile(input)) {
            final Enumeration<ZipArchiveEntry> entries = zf.getEntries();
            while (entries.hasMoreElements()) {
                final ZipArchiveEntry zae = entries.nextElement();
                try (InputStream in = zf.getInputStream(zae)) {
                    readStream(in, zae, actualStatistics);
                }
            }
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
    public void testInputStreamStatisticsForBzip2Entry() throws IOException, ArchiveException {
        final Map<String, List<Long>> expected = new HashMap<>();
        expected.put("lots-of-as", Arrays.asList(42L, 39L));
        testInputStreamStatistics("bzip2-zip.zip", expected);
    }

    @Test
    public void testInputStreamStatisticsForDeflate64Entry() throws IOException, ArchiveException {
        final Map<String, List<Long>> expected = new HashMap<>();
        expected.put("input2", Arrays.asList(3072L, 2111L));
        testInputStreamStatistics("COMPRESS-380/COMPRESS-380.zip", expected);
    }

    @Test
    public void testInputStreamStatisticsForImplodedEntry() throws IOException, ArchiveException {
        final Map<String, List<Long>> expected = new HashMap<>();
        expected.put("LICENSE.TXT", Arrays.asList(11560L, 4131L));
        testInputStreamStatistics("imploding-8Kdict-3trees.zip", expected);
    }

    @Test
    public void testInputStreamStatisticsForShrunkEntry() throws IOException, ArchiveException {
        final Map<String, List<Long>> expected = new HashMap<>();
        expected.put("TEST1.XML", Arrays.asList(76L, 66L));
        expected.put("TEST2.XML", Arrays.asList(81L, 76L));
        testInputStreamStatistics("SHRUNK.ZIP", expected);
    }

    @Test
    public void testInputStreamStatisticsForStoredEntry() throws IOException, ArchiveException {
        final Map<String, List<Long>> expected = new HashMap<>();
        expected.put("test.txt", Arrays.asList(5L, 5L));
        testInputStreamStatistics("COMPRESS-264.zip", expected);
    }

    @Test
    public void testInputStreamStatisticsOfZipBombExcel() throws IOException, ArchiveException {
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
    public void testListAllFilesWithNestedArchive() throws Exception {
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
                } catch (ZipException ex) {
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
     * @see <a href="https://issues.apache.org/jira/browse/COMPRESS-93" >COMPRESS-93</a>
     */
    @Test
    public void testSkipEntryWithUnsupportedCompressionMethod() throws IOException {
        try (ZipArchiveInputStream zip = new ZipArchiveInputStream(newInputStream("moby.zip"))) {
            final ZipArchiveEntry entry = zip.getNextZipEntry();
            assertEquals(ZipMethod.TOKENIZATION.getCode(), entry.getMethod(), "method");
            assertEquals("README", entry.getName());
            assertFalse(zip.canReadEntryData(entry));
            assertDoesNotThrow(() -> assertNull(zip.getNextZipEntry()), "COMPRESS-93: Unable to skip an unsupported ZIP entry");
        }
    }

    /**
     * Test case for <a href="https://issues.apache.org/jira/browse/COMPRESS-208" >COMPRESS-208</a>.
     */
    @Test
    public void testSkipsPK00Prefix() throws Exception {
        final File input = getFile("COMPRESS-208.zip");
        final ArrayList<String> al = new ArrayList<>();
        al.add("test1.xml");
        al.add("test2.xml");
        try (InputStream fis = Files.newInputStream(input.toPath());
                ZipArchiveInputStream inputStream = new ZipArchiveInputStream(fis)) {
            checkArchiveContent(inputStream, al);
        }
    }

    /**
     * Test case for <a href="https://issues.apache.org/jira/browse/COMPRESS-93" >COMPRESS-93</a>.
     */
    @Test
    public void testTokenizationCompressionMethod() throws IOException {
        try (ZipFile moby = ZipFile.builder().setFile(getFile("moby.zip")).get()) {
            final ZipArchiveEntry entry = moby.getEntry("README");
            assertEquals(ZipMethod.TOKENIZATION.getCode(), entry.getMethod(), "method");
            assertFalse(moby.canReadEntryData(entry));
        }
    }

    @Test
    public void testUnixModeInAddRaw() throws IOException {
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

    /**
     * Archives 2 files and unarchives it again. If the file length of result and source is the same, it looks like the operations have worked
     *
     * @throws Exception
     */
    @Test
    public void testZipArchiveCreation() throws Exception {
        // Archive
        final File output = newTempFile("bla.zip");
        final File file1 = getFile("test1.xml");
        final File file2 = getFile("test2.xml");

        try (OutputStream out = Files.newOutputStream(output.toPath())) {
            try (ArchiveOutputStream<ZipArchiveEntry> os = ArchiveStreamFactory.DEFAULT.createArchiveOutputStream("zip", out)) {
                os.putArchiveEntry(new ZipArchiveEntry("testdata/test1.xml"));
                Files.copy(file1.toPath(), os);
                os.closeArchiveEntry();

                os.putArchiveEntry(new ZipArchiveEntry("testdata/test2.xml"));
                Files.copy(file2.toPath(), os);
                os.closeArchiveEntry();
            }
        }

        // Unarchive the same
        final List<File> results = new ArrayList<>();

        try (InputStream fileInputStream = Files.newInputStream(output.toPath())) {
            try (ArchiveInputStream<ZipArchiveEntry> archiveInputStream = ArchiveStreamFactory.DEFAULT.createArchiveInputStream("zip", fileInputStream)) {
                ZipArchiveEntry entry;
                while ((entry = archiveInputStream.getNextEntry()) != null) {
                    final File outfile = new File(tempResultDir.getCanonicalPath() + "/result/" + entry.getName());
                    outfile.getParentFile().mkdirs();
                    Files.copy(archiveInputStream, outfile.toPath());
                    results.add(outfile);
                }
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
    public void testZipArchiveCreationInMemory() throws Exception {
        final File file1 = getFile("test1.xml");
        final File file2 = getFile("test2.xml");
        final byte[] file1Contents = new byte[(int) file1.length()];
        final byte[] file2Contents = new byte[(int) file2.length()];
        IOUtils.read(file1, file1Contents);
        IOUtils.read(file2, file2Contents);
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
    public void testZipArchiveEntryNewFromPath() throws Exception {
        Path archivePath;
        final File tmpFile = createTempFile();
        final Path tmpFilePath = tmpFile.toPath();
        final File archiveFile = createTempFile("test.", ".zip");
        archivePath = archiveFile.toPath();
        try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(archivePath)) {
            final ZipArchiveEntry in = zos.createArchiveEntry(tmpFilePath, "foo");
            zos.putArchiveEntry(in);
            final byte[] b = new byte[(int) tmpFile.length()];
            try (InputStream fis = Files.newInputStream(tmpFile.toPath())) {
                while (fis.read(b) > 0) {
                    zos.write(b);
                }
            }
            zos.closeArchiveEntry();
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
    public void testZipUnarchive() throws Exception {
        final File input = getFile("bla.zip");
        try (InputStream is = Files.newInputStream(input.toPath());
                ArchiveInputStream<ZipArchiveEntry> in = ArchiveStreamFactory.DEFAULT.createArchiveInputStream("zip", is)) {
            final ZipArchiveEntry entry = in.getNextEntry();
            Files.copy(in, newTempFile(entry.getName()).toPath());
        }
    }
}
