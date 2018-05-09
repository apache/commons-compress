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
package org.apache.commons.compress.archivers.sevenz;

import static org.junit.Assert.*;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.tukaani.xz.LZMA2Options;

public class SevenZOutputFileTest extends AbstractTestCase {

    private static final boolean XZ_BCJ_IS_BUGGY;

    static {
        final String version = org.tukaani.xz.XZ.class.getPackage().getImplementationVersion();

        XZ_BCJ_IS_BUGGY= version != null && version.equals("1.4");
        if (XZ_BCJ_IS_BUGGY) {
            System.out.println("XZ version is " + version + " - skipping BCJ tests");
        }
    }
    private File output;

    @Override
    public void tearDown() throws Exception {
        if (output != null && !output.delete()) {
            output.deleteOnExit();
        }
        super.tearDown();
    }

    @Test
    public void testDirectoriesAndEmptyFiles() throws Exception {
        output = new File(dir, "empties.7z");

        final Date accessDate = new Date();
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, -1);
        final Date creationDate = cal.getTime();

        try (SevenZOutputFile outArchive = new SevenZOutputFile(output)) {
            SevenZArchiveEntry entry = outArchive.createArchiveEntry(dir, "foo/");
            outArchive.putArchiveEntry(entry);
            outArchive.closeArchiveEntry();

            entry = new SevenZArchiveEntry();
            entry.setName("foo/bar");
            entry.setCreationDate(creationDate);
            entry.setAccessDate(accessDate);
            outArchive.putArchiveEntry(entry);
            outArchive.write(new byte[0]);
            outArchive.closeArchiveEntry();

            entry = new SevenZArchiveEntry();
            entry.setName("xyzzy");
            outArchive.putArchiveEntry(entry);
            outArchive.write(0);
            outArchive.closeArchiveEntry();

            entry = outArchive.createArchiveEntry(dir, "baz/");
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

        try (SevenZFile archive = new SevenZFile(output)) {
            SevenZArchiveEntry entry = archive.getNextEntry();
            assert (entry != null);
            assertEquals("foo/", entry.getName());
            assertTrue(entry.isDirectory());
            assertFalse(entry.isAntiItem());

            entry = archive.getNextEntry();
            assert (entry != null);
            assertEquals("foo/bar", entry.getName());
            assertFalse(entry.isDirectory());
            assertFalse(entry.isAntiItem());
            assertEquals(0, entry.getSize());
            assertFalse(entry.getHasLastModifiedDate());
            assertEquals(accessDate, entry.getAccessDate());
            assertEquals(creationDate, entry.getCreationDate());

            entry = archive.getNextEntry();
            assert (entry != null);
            assertEquals("xyzzy", entry.getName());
            assertEquals(1, entry.getSize());
            assertFalse(entry.getHasAccessDate());
            assertFalse(entry.getHasCreationDate());
            assertEquals(0, archive.read());

            entry = archive.getNextEntry();
            assert (entry != null);
            assertEquals("baz/", entry.getName());
            assertTrue(entry.isDirectory());
            assertTrue(entry.isAntiItem());

            entry = archive.getNextEntry();
            assert (entry != null);
            assertEquals("dada", entry.getName());
            assertEquals(2, entry.getSize());
            final byte[] content = new byte[2];
            assertEquals(2, archive.read(content));
            assertEquals(5, content[0]);
            assertEquals(42, content[1]);
            assertEquals(17, entry.getWindowsAttributes());

            assert (archive.getNextEntry() == null);
        }

    }

    @Test
    public void testDirectoriesOnly() throws Exception {
        output = new File(dir, "dirs.7z");
        try (SevenZOutputFile outArchive = new SevenZOutputFile(output)) {
            final SevenZArchiveEntry entry = new SevenZArchiveEntry();
            entry.setName("foo/");
            entry.setDirectory(true);
            outArchive.putArchiveEntry(entry);
            outArchive.closeArchiveEntry();
        }

        try (SevenZFile archive = new SevenZFile(output)) {
            final SevenZArchiveEntry entry = archive.getNextEntry();
            assert (entry != null);
            assertEquals("foo/", entry.getName());
            assertTrue(entry.isDirectory());
            assertFalse(entry.isAntiItem());

            assert (archive.getNextEntry() == null);
        }

    }

    @Test
    public void testCantFinishTwice() throws Exception {
        output = new File(dir, "finish.7z");
        try (SevenZOutputFile outArchive = new SevenZOutputFile(output)) {
            outArchive.finish();
            outArchive.finish();
            fail("shouldn't be able to call finish twice");
        } catch (final IOException ex) {
            assertEquals("This archive has already been finished", ex.getMessage());
        }
    }

    @Test
    public void testSixEmptyFiles() throws Exception {
        testCompress252(6, 0);
    }

    @Test
    public void testSixFilesSomeNotEmpty() throws Exception {
        testCompress252(6, 2);
    }

    @Test
    public void testSevenEmptyFiles() throws Exception {
        testCompress252(7, 0);
    }

    @Test
    public void testSevenFilesSomeNotEmpty() throws Exception {
        testCompress252(7, 2);
    }

    @Test
    public void testEightEmptyFiles() throws Exception {
        testCompress252(8, 0);
    }

    @Test
    public void testEightFilesSomeNotEmpty() throws Exception {
        testCompress252(8, 2);
    }

    @Test
    public void testNineEmptyFiles() throws Exception {
        testCompress252(9, 0);
    }

    @Test
    public void testNineFilesSomeNotEmpty() throws Exception {
        testCompress252(9, 2);
    }

    @Test
    public void testTwentyNineEmptyFiles() throws Exception {
        testCompress252(29, 0);
    }

    @Test
    public void testTwentyNineFilesSomeNotEmpty() throws Exception {
        testCompress252(29, 7);
    }

    @Test
    public void testCopyRoundtrip() throws Exception {
        testRoundTrip(SevenZMethod.COPY);
    }

    @Test
    public void testBzip2Roundtrip() throws Exception {
        testRoundTrip(SevenZMethod.BZIP2);
    }

    @Test
    public void testLzma2Roundtrip() throws Exception {
        testRoundTrip(SevenZMethod.LZMA2);
    }

    @Test
    public void testDeflateRoundtrip() throws Exception {
        testRoundTrip(SevenZMethod.DEFLATE);
    }

    @Test
    public void testBCJX86Roundtrip() throws Exception {
        if (XZ_BCJ_IS_BUGGY) { return; }
        testFilterRoundTrip(new SevenZMethodConfiguration(SevenZMethod.BCJ_X86_FILTER));
    }

    @Test
    public void testBCJARMRoundtrip() throws Exception {
        if (XZ_BCJ_IS_BUGGY) { return; }
        testFilterRoundTrip(new SevenZMethodConfiguration(SevenZMethod.BCJ_ARM_FILTER));
    }

    @Test
    public void testBCJARMThumbRoundtrip() throws Exception {
        if (XZ_BCJ_IS_BUGGY) { return; }
        testFilterRoundTrip(new SevenZMethodConfiguration(SevenZMethod.BCJ_ARM_THUMB_FILTER));
    }

    @Test
    public void testBCJIA64Roundtrip() throws Exception {
        if (XZ_BCJ_IS_BUGGY) { return; }
        testFilterRoundTrip(new SevenZMethodConfiguration(SevenZMethod.BCJ_IA64_FILTER));
    }

    @Test
    public void testBCJPPCRoundtrip() throws Exception {
        if (XZ_BCJ_IS_BUGGY) { return; }
        testFilterRoundTrip(new SevenZMethodConfiguration(SevenZMethod.BCJ_PPC_FILTER));
    }

    @Test
    public void testBCJSparcRoundtrip() throws Exception {
        if (XZ_BCJ_IS_BUGGY) { return; }
        testFilterRoundTrip(new SevenZMethodConfiguration(SevenZMethod.BCJ_SPARC_FILTER));
    }

    @Test
    public void testDeltaRoundtrip() throws Exception {
        testFilterRoundTrip(new SevenZMethodConfiguration(SevenZMethod.DELTA_FILTER));
    }

    @Test
    public void testStackOfContentCompressions() throws Exception {
        output = new File(dir, "multiple-methods.7z");
        final ArrayList<SevenZMethodConfiguration> methods = new ArrayList<>();
        methods.add(new SevenZMethodConfiguration(SevenZMethod.LZMA2));
        methods.add(new SevenZMethodConfiguration(SevenZMethod.COPY));
        methods.add(new SevenZMethodConfiguration(SevenZMethod.DEFLATE));
        methods.add(new SevenZMethodConfiguration(SevenZMethod.BZIP2));
        createAndReadBack(output, methods);
    }

    @Test
    public void testStackOfContentCompressionsInMemory() throws Exception {
        final ArrayList<SevenZMethodConfiguration> methods = new ArrayList<>();
        methods.add(new SevenZMethodConfiguration(SevenZMethod.LZMA2));
        methods.add(new SevenZMethodConfiguration(SevenZMethod.COPY));
        methods.add(new SevenZMethodConfiguration(SevenZMethod.DEFLATE));
        methods.add(new SevenZMethodConfiguration(SevenZMethod.BZIP2));
        createAndReadBack(new SeekableInMemoryByteChannel(), methods);
    }

    @Test
    public void testDeflateWithConfiguration() throws Exception {
        output = new File(dir, "deflate-options.7z");
        // Deflater.BEST_SPEED
        createAndReadBack(output, Collections
                          .singletonList(new SevenZMethodConfiguration(SevenZMethod.DEFLATE, 1)));
    }

    @Test
    public void testBzip2WithConfiguration() throws Exception {
        output = new File(dir, "bzip2-options.7z");
        // 400k block size
        createAndReadBack(output, Collections
                          .singletonList(new SevenZMethodConfiguration(SevenZMethod.BZIP2, 4)));
    }

    @Test
    public void testLzmaWithIntConfiguration() throws Exception {
        output = new File(dir, "lzma-options.7z");
        // 1 MB dictionary
        createAndReadBack(output, Collections
                          .singletonList(new SevenZMethodConfiguration(SevenZMethod.LZMA, 1 << 20)));
    }

    @Test
    public void testLzmaWithOptionsConfiguration() throws Exception {
        output = new File(dir, "lzma-options2.7z");
        final LZMA2Options opts = new LZMA2Options(1);
        createAndReadBack(output, Collections
                          .singletonList(new SevenZMethodConfiguration(SevenZMethod.LZMA, opts)));
    }

    @Test
    public void testLzma2WithIntConfiguration() throws Exception {
        output = new File(dir, "lzma2-options.7z");
        // 1 MB dictionary
        createAndReadBack(output, Collections
                          .singletonList(new SevenZMethodConfiguration(SevenZMethod.LZMA2, 1 << 20)));
    }

    @Test
    public void testLzma2WithOptionsConfiguration() throws Exception {
        output = new File(dir, "lzma2-options2.7z");
        final LZMA2Options opts = new LZMA2Options(1);
        createAndReadBack(output, Collections
                          .singletonList(new SevenZMethodConfiguration(SevenZMethod.LZMA2, opts)));
    }

    @Test
    public void testArchiveWithMixedMethods() throws Exception {
        output = new File(dir, "mixed-methods.7z");
        try (SevenZOutputFile outArchive = new SevenZOutputFile(output)) {
            addFile(outArchive, 0, true);
            addFile(outArchive, 1, true, Arrays.asList(new SevenZMethodConfiguration(SevenZMethod.BZIP2)));
        }

        try (SevenZFile archive = new SevenZFile(output)) {
            assertEquals(Boolean.TRUE,
                    verifyFile(archive, 0, Arrays.asList(new SevenZMethodConfiguration(SevenZMethod.LZMA2))));
            assertEquals(Boolean.TRUE,
                    verifyFile(archive, 1, Arrays.asList(new SevenZMethodConfiguration(SevenZMethod.BZIP2))));
        }
    }

    private void testCompress252(final int numberOfFiles, final int numberOfNonEmptyFiles)
        throws Exception {
        final int nonEmptyModulus = numberOfNonEmptyFiles != 0
            ? numberOfFiles / numberOfNonEmptyFiles
            : numberOfFiles + 1;
        int nonEmptyFilesAdded = 0;
        output = new File(dir, "COMPRESS252-" + numberOfFiles + "-" + numberOfNonEmptyFiles + ".7z");
        try (SevenZOutputFile archive = new SevenZOutputFile(output)) {
            addDir(archive);
            for (int i = 0; i < numberOfFiles; i++) {
                addFile(archive, i,
                        (i + 1) % nonEmptyModulus == 0 && nonEmptyFilesAdded++ < numberOfNonEmptyFiles);
            }
        }
        verifyCompress252(output, numberOfFiles, numberOfNonEmptyFiles);
    }

    private void verifyCompress252(final File output, final int numberOfFiles, final int numberOfNonEmptyFiles)
        throws Exception {
        int filesFound = 0;
        int nonEmptyFilesFound = 0;
        try (SevenZFile archive = new SevenZFile(output)) {
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

    private void addDir(final SevenZOutputFile archive) throws Exception {
        final SevenZArchiveEntry entry = archive.createArchiveEntry(dir, "foo/");
        archive.putArchiveEntry(entry);
        archive.closeArchiveEntry();
    }

    private void verifyDir(final SevenZFile archive) throws Exception {
        final SevenZArchiveEntry entry = archive.getNextEntry();
        assertNotNull(entry);
        assertEquals("foo/", entry.getName());
        assertTrue(entry.isDirectory());
    }

    private void addFile(final SevenZOutputFile archive, final int index, final boolean nonEmpty)
        throws Exception {
        addFile(archive, index, nonEmpty, null);
    }

    private void addFile(final SevenZOutputFile archive, final int index, final boolean nonEmpty, final Iterable<SevenZMethodConfiguration> methods)
        throws Exception {
        final SevenZArchiveEntry entry = new SevenZArchiveEntry();
        entry.setName("foo/" + index + ".txt");
        entry.setContentMethods(methods);
        archive.putArchiveEntry(entry);
        archive.write(nonEmpty ? new byte[] { 'A' } : new byte[0]);
        archive.closeArchiveEntry();
    }

    private Boolean verifyFile(final SevenZFile archive, final int index) throws Exception {
        return verifyFile(archive, index, null);
    }

    private Boolean verifyFile(final SevenZFile archive, final int index,
                               final Iterable<SevenZMethodConfiguration> methods) throws Exception {
        final SevenZArchiveEntry entry = archive.getNextEntry();
        if (entry == null) {
            return null;
        }
        assertEquals("foo/" + index + ".txt", entry.getName());
        assertEquals(false, entry.isDirectory());
        if (entry.getSize() == 0) {
            return Boolean.FALSE;
        }
        assertEquals(1, entry.getSize());
        assertEquals('A', archive.read());
        assertEquals(-1, archive.read());
        if (methods != null) {
            assertContentMethodsEquals(methods, entry.getContentMethods());
        }
        return Boolean.TRUE;
    }

    private void testRoundTrip(final SevenZMethod method) throws Exception {
        output = new File(dir, method + "-roundtrip.7z");
        final ArrayList<SevenZMethodConfiguration> methods = new ArrayList<>();
        methods.add(new SevenZMethodConfiguration(method));
        createAndReadBack(output, methods);
    }

    private void testFilterRoundTrip(final SevenZMethodConfiguration method) throws Exception {
        output = new File(dir, method.getMethod() + "-roundtrip.7z");
        final ArrayList<SevenZMethodConfiguration> methods = new ArrayList<>();
        methods.add(method);
        methods.add(new SevenZMethodConfiguration(SevenZMethod.LZMA2));
        createAndReadBack(output, methods);
    }

    private void createAndReadBack(final File output, final Iterable<SevenZMethodConfiguration> methods) throws Exception {
        final SevenZOutputFile outArchive = new SevenZOutputFile(output);
        outArchive.setContentMethods(methods);
        try {
            addFile(outArchive, 0, true);
        } finally {
            outArchive.close();
        }

        try (SevenZFile archive = new SevenZFile(output)) {
            assertEquals(Boolean.TRUE, verifyFile(archive, 0, methods));
        }
    }

    private void createAndReadBack(final SeekableInMemoryByteChannel output, final Iterable<SevenZMethodConfiguration> methods) throws Exception {
        final SevenZOutputFile outArchive = new SevenZOutputFile(output);
        outArchive.setContentMethods(methods);
        try {
            addFile(outArchive, 0, true);
        } finally {
            outArchive.close();
        }
        try (SevenZFile archive =
             new SevenZFile(new SeekableInMemoryByteChannel(output.array()), "in memory")) {
            assertEquals(Boolean.TRUE, verifyFile(archive, 0, methods));
        }
    }

    private static void assertContentMethodsEquals(final Iterable<? extends SevenZMethodConfiguration> expected,
                                                   final Iterable<? extends SevenZMethodConfiguration> actual) {
        assertNotNull(actual);
        final Iterator<? extends SevenZMethodConfiguration> expectedIter = expected.iterator();
        final Iterator<? extends SevenZMethodConfiguration> actualIter = actual.iterator();
        while (expectedIter.hasNext()) {
            assertTrue(actualIter.hasNext());
            final SevenZMethodConfiguration expConfig = expectedIter.next();
            final SevenZMethodConfiguration actConfig = actualIter.next();
            assertEquals(expConfig.getMethod(), actConfig.getMethod());
        }
        assertFalse(actualIter.hasNext());
    }
}
