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
package org.apache.commons.compress.archivers.sevenz;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.PasswordRequiredException;
import org.apache.commons.compress.utils.ByteUtils;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.apache.commons.compress.utils.TimeUtils;
import org.junit.jupiter.api.Test;
import org.tukaani.xz.LZMA2Options;

public class SevenZOutputFileTest extends AbstractTest {

    private static final boolean XZ_BCJ_IS_BUGGY;

    static {
        final String version = org.tukaani.xz.XZ.class.getPackage().getImplementationVersion();

        XZ_BCJ_IS_BUGGY = version != null && version.equals("1.4");
        if (XZ_BCJ_IS_BUGGY) {
            System.out.println("XZ version is " + version + " - skipping BCJ tests");
        }
    }

    private static void assertContentMethodsEquals(final Iterable<? extends SevenZMethodConfiguration> expected,
            final Iterable<? extends SevenZMethodConfiguration> actual) {
        assertNotNull(actual);
        final Iterator<? extends SevenZMethodConfiguration> actualIter = actual.iterator();
        for (final SevenZMethodConfiguration expConfig : expected) {
            assertTrue(actualIter.hasNext());
            final SevenZMethodConfiguration actConfig = actualIter.next();
            assertEquals(expConfig.getMethod(), actConfig.getMethod());
        }
        assertFalse(actualIter.hasNext());
    }

    private void addDir(final SevenZOutputFile archive) throws Exception {
        final SevenZArchiveEntry entry = archive.createArchiveEntry(getTempDirFile(), "foo/");
        archive.putArchiveEntry(entry);
        archive.closeArchiveEntry();
    }

    private void addFile(final SevenZOutputFile archive, final int index, final boolean nonEmpty) throws Exception {
        addFile(archive, index, nonEmpty, null);
    }

    private void addFile(final SevenZOutputFile archive, final int index, final boolean nonEmpty, final Iterable<SevenZMethodConfiguration> methods)
            throws Exception {
        addFile(archive, index, nonEmpty ? 1 : 0, methods);
    }

    private void addFile(final SevenZOutputFile archive, final int index, final int size, final Iterable<SevenZMethodConfiguration> methods) throws Exception {
        final SevenZArchiveEntry entry = new SevenZArchiveEntry();
        entry.setName("foo/" + index + ".txt");
        entry.setContentMethods(methods);
        archive.putArchiveEntry(entry);
        archive.write(generateFileData(size));
        archive.closeArchiveEntry();
    }

    private void createAndReadBack(final File output, final Iterable<SevenZMethodConfiguration> methods) throws Exception {
        try (SevenZOutputFile outArchive = new SevenZOutputFile(output)) {
            outArchive.setContentMethods(methods);
            addFile(outArchive, 0, true);
        }

        try (SevenZFile archive = SevenZFile.builder().setFile(output).get()) {
            assertEquals(Boolean.TRUE, verifyFile(archive, 0, methods));
        }
    }

    private void createAndReadBack(final SeekableInMemoryByteChannel output, final Iterable<SevenZMethodConfiguration> methods) throws Exception {
        try (SevenZOutputFile outArchive = new SevenZOutputFile(output)) {
            outArchive.setContentMethods(methods);
            addFile(outArchive, 0, true);
        }
        try (SevenZFile archive = SevenZFile.builder().setByteArray(output.array()).setDefaultName("in memory").get()) {
            assertEquals(Boolean.TRUE, verifyFile(archive, 0, methods));
        }
    }

    private byte[] generateFileData(final int size) {
        final byte[] data = new byte[size];
        for (int i = 0; i < size; i++) {
            data[i] = (byte) ('A' + i % 26);
        }
        return data;
    }

    private FileTime getHundredNanosFileTime() {
        final Instant now = Instant.now();
        // In some platforms, Java's Instant has a precision of milliseconds.
        // Add some nanos at the end to test 100ns intervals.
        final FileTime fileTime = FileTime.from(Instant.ofEpochSecond(now.getEpochSecond(), now.getNano() + 999900));
        // However, in some platforms, Java's Instant has a precision of nanoseconds.
        // Truncate the resulting FileTime to 100ns intervals.
        return TimeUtils.truncateToHundredNanos(fileTime);
    }

    @Test
    void testArchiveWithMixedMethods() throws Exception {
        final File output = newTempFile("mixed-methods.7z");
        try (SevenZOutputFile outArchive = new SevenZOutputFile(output)) {
            addFile(outArchive, 0, true);
            addFile(outArchive, 1, true, Arrays.asList(new SevenZMethodConfiguration(SevenZMethod.BZIP2)));
        }

        try (SevenZFile archive = SevenZFile.builder().setFile(output).get()) {
            assertEquals(Boolean.TRUE, verifyFile(archive, 0, Arrays.asList(new SevenZMethodConfiguration(SevenZMethod.LZMA2))));
            assertEquals(Boolean.TRUE, verifyFile(archive, 1, Arrays.asList(new SevenZMethodConfiguration(SevenZMethod.BZIP2))));
        }
    }

    @Test
    void testBCJARMRoundtrip() throws Exception {
        if (XZ_BCJ_IS_BUGGY) {
            return;
        }
        testFilterRoundTrip(new SevenZMethodConfiguration(SevenZMethod.BCJ_ARM_FILTER));
    }

    @Test
    void testBCJARMThumbRoundtrip() throws Exception {
        if (XZ_BCJ_IS_BUGGY) {
            return;
        }
        testFilterRoundTrip(new SevenZMethodConfiguration(SevenZMethod.BCJ_ARM_THUMB_FILTER));
    }

    @Test
    void testBCJIA64Roundtrip() throws Exception {
        if (XZ_BCJ_IS_BUGGY) {
            return;
        }
        testFilterRoundTrip(new SevenZMethodConfiguration(SevenZMethod.BCJ_IA64_FILTER));
    }

    @Test
    void testBCJPPCRoundtrip() throws Exception {
        if (XZ_BCJ_IS_BUGGY) {
            return;
        }
        testFilterRoundTrip(new SevenZMethodConfiguration(SevenZMethod.BCJ_PPC_FILTER));
    }

    @Test
    void testBCJSparcRoundtrip() throws Exception {
        if (XZ_BCJ_IS_BUGGY) {
            return;
        }
        testFilterRoundTrip(new SevenZMethodConfiguration(SevenZMethod.BCJ_SPARC_FILTER));
    }

    @Test
    void testBCJX86Roundtrip() throws Exception {
        if (XZ_BCJ_IS_BUGGY) {
            return;
        }
        testFilterRoundTrip(new SevenZMethodConfiguration(SevenZMethod.BCJ_X86_FILTER));
    }

    @Test
    void testBzip2Roundtrip() throws Exception {
        testRoundTrip(SevenZMethod.BZIP2);
    }

    @Test
    void testBzip2WithConfiguration() throws Exception {
        final File output = newTempFile("bzip2-options.7z");
        // 400k block size
        createAndReadBack(output, Collections.singletonList(new SevenZMethodConfiguration(SevenZMethod.BZIP2, 4)));
    }

    @Test
    void testCantFinishTwice() throws IOException {
        final File output = newTempFile("finish.7z");
        try (SevenZOutputFile outArchive = new SevenZOutputFile(output)) {
            outArchive.finish();
            final IOException ex = assertThrows(IOException.class, outArchive::finish, "shouldn't be able to call finish twice");
            assertEquals("This archive has already been finished", ex.getMessage());
        }
    }

    private void testCompress252(final int numberOfFiles, final int numberOfNonEmptyFiles) throws Exception {
        final int nonEmptyModulus = numberOfNonEmptyFiles != 0 ? numberOfFiles / numberOfNonEmptyFiles : numberOfFiles + 1;
        int nonEmptyFilesAdded = 0;
        final File output = newTempFile("COMPRESS252-" + numberOfFiles + "-" + numberOfNonEmptyFiles + ".7z");
        try (SevenZOutputFile archive = new SevenZOutputFile(output)) {
            addDir(archive);
            for (int i = 0; i < numberOfFiles; i++) {
                addFile(archive, i, (i + 1) % nonEmptyModulus == 0 && nonEmptyFilesAdded++ < numberOfNonEmptyFiles);
            }
        }
        verifyCompress252(output, numberOfFiles, numberOfNonEmptyFiles);
    }

    @Test
    void testCopyRoundtrip() throws Exception {
        testRoundTrip(SevenZMethod.COPY);
    }

    @Test
    void testDeflateRoundtrip() throws Exception {
        testRoundTrip(SevenZMethod.DEFLATE);
    }

    @Test
    void testDeflateWithConfiguration() throws Exception {
        final File output = newTempFile("deflate-options.7z");
        // Deflater.BEST_SPEED
        createAndReadBack(output, Collections.singletonList(new SevenZMethodConfiguration(SevenZMethod.DEFLATE, 1)));
    }

    @Test
    void testDeltaRoundtrip() throws Exception {
        testFilterRoundTrip(new SevenZMethodConfiguration(SevenZMethod.DELTA_FILTER));
    }

    @Test
    void testDirectoriesAndEmptyFiles() throws Exception {
        final File output = newTempFile("empties.7z");

        final FileTime accessTime = getHundredNanosFileTime();
        final Date accessDate = new Date(accessTime.toMillis());
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, -1);
        final Date creationDate = cal.getTime();

        try (SevenZOutputFile outArchive = new SevenZOutputFile(output)) {
            SevenZArchiveEntry entry = outArchive.createArchiveEntry(getTempDirFile(), "foo/");
            outArchive.putArchiveEntry(entry);
            outArchive.closeArchiveEntry();

            entry = new SevenZArchiveEntry();
            entry.setName("foo/bar");
            entry.setCreationDate(creationDate);
            entry.setAccessTime(accessTime);
            outArchive.putArchiveEntry(entry);
            outArchive.write(ByteUtils.EMPTY_BYTE_ARRAY);
            outArchive.closeArchiveEntry();

            entry = new SevenZArchiveEntry();
            entry.setName("foo/bar/boo0");
            entry.setCreationDate(creationDate);
            entry.setAccessTime(accessTime);
            outArchive.putArchiveEntry(entry);
            outArchive.write(new ByteArrayInputStream(ByteUtils.EMPTY_BYTE_ARRAY));
            outArchive.closeArchiveEntry();

            entry = new SevenZArchiveEntry();
            entry.setName("foo/bar/boo1");
            entry.setCreationDate(creationDate);
            entry.setAccessTime(accessTime);
            outArchive.putArchiveEntry(entry);
            outArchive.write(new ByteArrayInputStream(new byte[] { 'a' }));
            outArchive.closeArchiveEntry();

            entry = new SevenZArchiveEntry();
            entry.setName("foo/bar/boo10000");
            entry.setCreationDate(creationDate);
            entry.setAccessTime(accessTime);
            outArchive.putArchiveEntry(entry);
            outArchive.write(new ByteArrayInputStream(new byte[10000]));
            outArchive.closeArchiveEntry();

            entry = new SevenZArchiveEntry();
            entry.setName("foo/bar/test.txt");
            entry.setCreationDate(creationDate);
            entry.setAccessTime(accessTime);
            outArchive.putArchiveEntry(entry);
            outArchive.write(Paths.get("src/test/resources/test.txt"));
            outArchive.closeArchiveEntry();

            entry = new SevenZArchiveEntry();
            entry.setName("xyzzy");
            outArchive.putArchiveEntry(entry);
            outArchive.write(0);
            outArchive.closeArchiveEntry();

            entry = outArchive.createArchiveEntry(getTempDirFile(), "baz/");
            entry.setAntiItem(true);
            outArchive.putArchiveEntry(entry);
            outArchive.closeArchiveEntry();

            entry = outArchive.createArchiveEntry(getTempDirFile().toPath(), "baz2/");
            entry.setAntiItem(true);
            outArchive.putArchiveEntry(entry);
            outArchive.closeArchiveEntry();

            entry = new SevenZArchiveEntry();
            entry.setName("dada");
            entry.setHasWindowsAttributes(true);
            entry.setWindowsAttributes(17);
            outArchive.putArchiveEntry(entry);
            outArchive.write(5);
            outArchive.write(42);
            outArchive.closeArchiveEntry();

            outArchive.finish();
        }

        try (SevenZFile archive = SevenZFile.builder().setFile(output).get()) {
            SevenZArchiveEntry entry = archive.getNextEntry();
            assertNotNull(entry, "Entry should not be null");
            assertEquals("foo/", entry.getName());
            assertTrue(entry.isDirectory());
            assertFalse(entry.isAntiItem());

            entry = archive.getNextEntry();
            assertNotNull(entry, "Entry should not be null");
            assertEquals("foo/bar", entry.getName());
            assertFalse(entry.isDirectory());
            assertFalse(entry.isAntiItem());
            assertEquals(0, entry.getSize());
            assertFalse(entry.getHasLastModifiedDate());
            assertEquals(accessTime, entry.getAccessTime());
            assertEquals(accessDate, entry.getAccessDate());
            assertEquals(creationDate, entry.getCreationDate());

            entry = archive.getNextEntry();
            assertNotNull(entry, "Entry should not be null");
            assertEquals("foo/bar/boo0", entry.getName());
            assertFalse(entry.isDirectory());
            assertFalse(entry.isAntiItem());
            assertEquals(0, entry.getSize());
            assertFalse(entry.getHasLastModifiedDate());
            assertEquals(accessTime, entry.getAccessTime());
            assertEquals(accessDate, entry.getAccessDate());
            assertEquals(creationDate, entry.getCreationDate());

            entry = archive.getNextEntry();
            assertNotNull(entry, "Entry should not be null");
            assertEquals("foo/bar/boo1", entry.getName());
            assertFalse(entry.isDirectory());
            assertFalse(entry.isAntiItem());
            assertEquals(1, entry.getSize());
            assertFalse(entry.getHasLastModifiedDate());
            assertEquals(accessTime, entry.getAccessTime());
            assertEquals(accessDate, entry.getAccessDate());
            assertEquals(creationDate, entry.getCreationDate());

            entry = archive.getNextEntry();
            assertNotNull(entry, "Entry should not be null");
            assertEquals("foo/bar/boo10000", entry.getName());
            assertFalse(entry.isDirectory());
            assertFalse(entry.isAntiItem());
            assertEquals(10000, entry.getSize());
            assertFalse(entry.getHasLastModifiedDate());
            assertEquals(accessTime, entry.getAccessTime());
            assertEquals(accessDate, entry.getAccessDate());
            assertEquals(creationDate, entry.getCreationDate());

            entry = archive.getNextEntry();
            assertNotNull(entry, "Entry should not be null");
            assertEquals("foo/bar/test.txt", entry.getName());
            assertFalse(entry.isDirectory());
            assertFalse(entry.isAntiItem());
            assertEquals(Files.size(Paths.get("src/test/resources/test.txt")), entry.getSize());
            assertFalse(entry.getHasLastModifiedDate());
            assertEquals(accessTime, entry.getAccessTime());
            assertEquals(accessDate, entry.getAccessDate());
            assertEquals(creationDate, entry.getCreationDate());

            entry = archive.getNextEntry();
            assertNotNull(entry, "Entry should not be null");
            assertEquals("xyzzy", entry.getName());
            assertEquals(1, entry.getSize());
            assertFalse(entry.getHasAccessDate());
            assertFalse(entry.getHasCreationDate());
            assertEquals(0, archive.read());

            entry = archive.getNextEntry();
            assertNotNull(entry, "Entry should not be null");
            assertEquals("baz/", entry.getName());
            assertTrue(entry.isDirectory());
            assertTrue(entry.isAntiItem());

            entry = archive.getNextEntry();
            assertNotNull(entry, "Entry should not be null");
            assertEquals("baz2/", entry.getName());
            assertTrue(entry.isDirectory());
            assertTrue(entry.isAntiItem());

            entry = archive.getNextEntry();
            assertNotNull(entry, "Entry should not be null");
            assertEquals("dada", entry.getName());
            assertEquals(2, entry.getSize());
            final byte[] content = new byte[2];
            assertEquals(2, archive.read(content));
            assertEquals(5, content[0]);
            assertEquals(42, content[1]);
            assertEquals(17, entry.getWindowsAttributes());

            assertNull(archive.getNextEntry(), "getNextEntry should return null");
        }

    }

    @Test
    void testDirectoriesOnly() throws Exception {
        final File output = newTempFile("dirs.7z");
        try (SevenZOutputFile outArchive = new SevenZOutputFile(output)) {
            final SevenZArchiveEntry entry = new SevenZArchiveEntry();
            entry.setName("foo/");
            entry.setDirectory(true);
            outArchive.putArchiveEntry(entry);
            outArchive.closeArchiveEntry();
        }

        try (SevenZFile archive = SevenZFile.builder().setFile(output).get()) {
            final SevenZArchiveEntry entry = archive.getNextEntry();
            assertNotNull(entry, "entry should not be null");
            assertEquals("foo/", entry.getName());
            assertTrue(entry.isDirectory());
            assertFalse(entry.isAntiItem());

            assertNull(archive.getNextEntry(), "getnextEntry should return null");
        }

    }

    @Test
    void testEightEmptyFiles() throws Exception {
        testCompress252(8, 0);
    }

    @Test
    void testEightFilesSomeNotEmpty() throws Exception {
        testCompress252(8, 2);
    }

    /**
     * Test password-based encryption
     *
     * <p>
     * As AES/CBC Cipher requires a minimum of 16 bytes file data to be encrypted, some padding logic has been implemented. This test checks different file
     * sizes (1, 16..) to ensure code coverage
     * </p>
     */
    @Test
    void testEncrypt() throws Exception {
        final File output = newTempFile("encrypted.7z");
        try (SevenZOutputFile outArchive = new SevenZOutputFile(output, "foo".toCharArray())) {
            addFile(outArchive, 0, 1, null);
            addFile(outArchive, 1, 16, null);
            addFile(outArchive, 2, 32, null);
            addFile(outArchive, 3, 33, null);
            addFile(outArchive, 4, 10000, null);
        }

        // Is archive really password-based encrypted ?
        try (SevenZFile archive = SevenZFile.builder().setFile(output).get()) {
            assertThrows(
                    PasswordRequiredException.class, () -> verifyFile(archive, 0), "A password should be needed");
        }

        try (SevenZFile archive = SevenZFile.builder().setFile(output).setPassword("foo").get()) {
            assertEquals(Boolean.TRUE, verifyFile(archive, 0, 1, null));
            assertEquals(Boolean.TRUE, verifyFile(archive, 1, 16, null));
            assertEquals(Boolean.TRUE, verifyFile(archive, 2, 32, null));
            assertEquals(Boolean.TRUE, verifyFile(archive, 3, 33, null));
            assertEquals(Boolean.TRUE, verifyFile(archive, 4, 10000, null));
        }
    }

    private void testFilterRoundTrip(final SevenZMethodConfiguration method) throws Exception {
        final File output = newTempFile(method.getMethod() + "-roundtrip.7z");
        final ArrayList<SevenZMethodConfiguration> methods = new ArrayList<>();
        methods.add(method);
        methods.add(new SevenZMethodConfiguration(SevenZMethod.LZMA2));
        createAndReadBack(output, methods);
    }

    @Test
    void testLzma2Roundtrip() throws Exception {
        testRoundTrip(SevenZMethod.LZMA2);
    }

    @Test
    void testLzma2WithIntConfiguration() throws Exception {
        final File output = newTempFile("lzma2-options.7z");
        // 1 MB dictionary
        createAndReadBack(output, Collections.singletonList(new SevenZMethodConfiguration(SevenZMethod.LZMA2, 1 << 20)));
    }

    @Test
    void testLzma2WithOptionsConfiguration() throws Exception {
        final File output = newTempFile("lzma2-options2.7z");
        final LZMA2Options opts = new LZMA2Options(1);
        createAndReadBack(output, Collections.singletonList(new SevenZMethodConfiguration(SevenZMethod.LZMA2, opts)));
    }

    @Test
    void testLzmaWithIntConfiguration() throws Exception {
        final File output = newTempFile("lzma-options.7z");
        // 1 MB dictionary
        createAndReadBack(output, Collections.singletonList(new SevenZMethodConfiguration(SevenZMethod.LZMA, 1 << 20)));
    }

    @Test
    void testLzmaWithOptionsConfiguration() throws Exception {
        final File output = newTempFile("lzma-options2.7z");
        final LZMA2Options opts = new LZMA2Options(1);
        createAndReadBack(output, Collections.singletonList(new SevenZMethodConfiguration(SevenZMethod.LZMA, opts)));
    }

    @Test
    void testNineEmptyFiles() throws Exception {
        testCompress252(9, 0);
    }

    @Test
    void testNineFilesSomeNotEmpty() throws Exception {
        testCompress252(9, 2);
    }

    private void testRoundTrip(final SevenZMethod method) throws Exception {
        final File output = newTempFile(method + "-roundtrip.7z");
        final ArrayList<SevenZMethodConfiguration> methods = new ArrayList<>();
        methods.add(new SevenZMethodConfiguration(method));
        createAndReadBack(output, methods);
    }

    @Test
    void testSevenEmptyFiles() throws Exception {
        testCompress252(7, 0);
    }

    @Test
    void testSevenFilesSomeNotEmpty() throws Exception {
        testCompress252(7, 2);
    }

    @Test
    void testSixEmptyFiles() throws Exception {
        testCompress252(6, 0);
    }

    @Test
    void testSixFilesSomeNotEmpty() throws Exception {
        testCompress252(6, 2);
    }

    @Test
    void testStackOfContentCompressions() throws Exception {
        final File output = newTempFile("multiple-methods.7z");
        final ArrayList<SevenZMethodConfiguration> methods = new ArrayList<>();
        methods.add(new SevenZMethodConfiguration(SevenZMethod.LZMA2));
        methods.add(new SevenZMethodConfiguration(SevenZMethod.COPY));
        methods.add(new SevenZMethodConfiguration(SevenZMethod.DEFLATE));
        methods.add(new SevenZMethodConfiguration(SevenZMethod.BZIP2));
        createAndReadBack(output, methods);
    }

    @Test
    void testStackOfContentCompressionsInMemory() throws Exception {
        final ArrayList<SevenZMethodConfiguration> methods = new ArrayList<>();
        methods.add(new SevenZMethodConfiguration(SevenZMethod.LZMA2));
        methods.add(new SevenZMethodConfiguration(SevenZMethod.COPY));
        methods.add(new SevenZMethodConfiguration(SevenZMethod.DEFLATE));
        methods.add(new SevenZMethodConfiguration(SevenZMethod.BZIP2));
        try (SeekableInMemoryByteChannel channel = new SeekableInMemoryByteChannel()) {
            createAndReadBack(channel, methods);
        }
    }

    @Test
    void testTwentyNineEmptyFiles() throws Exception {
        testCompress252(29, 0);
    }

    @Test
    void testTwentyNineFilesSomeNotEmpty() throws Exception {
        testCompress252(29, 7);
    }

    private void verifyCompress252(final File output, final int numberOfFiles, final int numberOfNonEmptyFiles) throws Exception {
        int filesFound = 0;
        int nonEmptyFilesFound = 0;
        try (SevenZFile archive = SevenZFile.builder().setFile(output).get()) {
            verifyDir(archive);
            Boolean b = verifyFile(archive, filesFound++);
            while (b != null) {
                if (Boolean.TRUE.equals(b)) {
                    nonEmptyFilesFound++;
                }
                b = verifyFile(archive, filesFound++);
            }
        }
        assertEquals(numberOfFiles + 1, filesFound);
        assertEquals(numberOfNonEmptyFiles, nonEmptyFilesFound);
    }

    private void verifyDir(final SevenZFile archive) throws Exception {
        final SevenZArchiveEntry entry = archive.getNextEntry();
        assertNotNull(entry);
        assertEquals("foo/", entry.getName());
        assertTrue(entry.isDirectory());
    }

    private Boolean verifyFile(final SevenZFile archive, final int index) throws Exception {
        return verifyFile(archive, index, null);
    }

    private Boolean verifyFile(final SevenZFile archive, final int index, final int size, final Iterable<SevenZMethodConfiguration> methods) throws Exception {
        final SevenZArchiveEntry entry = archive.getNextEntry();
        if (entry == null) {
            return null;
        }
        assertEquals("foo/" + index + ".txt", entry.getName());
        assertFalse(entry.isDirectory());
        if (entry.getSize() == 0) {
            return Boolean.FALSE;
        }
        assertEquals(size, entry.getSize());

        final byte[] actual = new byte[size];
        int count = 0;
        while (count < size) {
            final int read = archive.read(actual, count, actual.length - count);
            assertNotEquals(-1, read, "EOF reached before reading all expected data");
            count += read;
        }
        assertArrayEquals(generateFileData(size), actual);
        assertEquals(-1, archive.read());
        if (methods != null) {
            assertContentMethodsEquals(methods, entry.getContentMethods());
        }
        return Boolean.TRUE;
    }

    private Boolean verifyFile(final SevenZFile archive, final int index, final Iterable<SevenZMethodConfiguration> methods) throws Exception {
        return verifyFile(archive, index, 1, methods);
    }
}
