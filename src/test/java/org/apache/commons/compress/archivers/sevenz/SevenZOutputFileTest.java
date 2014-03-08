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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import org.apache.commons.compress.AbstractTestCase;
import org.tukaani.xz.LZMA2Options;

public class SevenZOutputFileTest extends AbstractTestCase {

    private static final boolean XZ_BCJ_IS_BUGGY;

    static {
        final String version = org.tukaani.xz.XZ.class.getPackage().getImplementationVersion();
        XZ_BCJ_IS_BUGGY=version.equals("1.4");
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

    public void testDirectoriesAndEmptyFiles() throws Exception {
        output = new File(dir, "empties.7z");

        Date accessDate = new Date();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, -1);
        Date creationDate = cal.getTime();

        SevenZOutputFile outArchive = new SevenZOutputFile(output);
        try {
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
        } finally {
            outArchive.close();
        }

        final SevenZFile archive = new SevenZFile(output);
        try {
            SevenZArchiveEntry entry = archive.getNextEntry();
            assert(entry != null);
            assertEquals("foo/", entry.getName());
            assertTrue(entry.isDirectory());
            assertFalse(entry.isAntiItem());

            entry = archive.getNextEntry();
            assert(entry != null);
            assertEquals("foo/bar", entry.getName());
            assertFalse(entry.isDirectory());
            assertFalse(entry.isAntiItem());
            assertEquals(0, entry.getSize());
            assertFalse(entry.getHasLastModifiedDate());
            assertEquals(accessDate, entry.getAccessDate());
            assertEquals(creationDate, entry.getCreationDate());

            entry = archive.getNextEntry();
            assert(entry != null);
            assertEquals("xyzzy", entry.getName());
            assertEquals(1, entry.getSize());
            assertFalse(entry.getHasAccessDate());
            assertFalse(entry.getHasCreationDate());
            assertEquals(0, archive.read());

            entry = archive.getNextEntry();
            assert(entry != null);
            assertEquals("baz/", entry.getName());
            assertTrue(entry.isDirectory());
            assertTrue(entry.isAntiItem());

            entry = archive.getNextEntry();
            assert(entry != null);
            assertEquals("dada", entry.getName());
            assertEquals(2, entry.getSize());
            byte[] content = new byte[2];
            assertEquals(2, archive.read(content));
            assertEquals(5, content[0]);
            assertEquals(42, content[1]);
            assertEquals(17, entry.getWindowsAttributes());

            assert(archive.getNextEntry() == null);
        } finally {
            archive.close();
        }

    }

    public void testDirectoriesOnly() throws Exception {
        output = new File(dir, "dirs.7z");
        SevenZOutputFile outArchive = new SevenZOutputFile(output);
        try {
            SevenZArchiveEntry entry = new SevenZArchiveEntry();
            entry.setName("foo/");
            entry.setDirectory(true);
            outArchive.putArchiveEntry(entry);
            outArchive.closeArchiveEntry();
        } finally {
            outArchive.close();
        }

        final SevenZFile archive = new SevenZFile(output);
        try {
            SevenZArchiveEntry entry = archive.getNextEntry();
            assert(entry != null);
            assertEquals("foo/", entry.getName());
            assertTrue(entry.isDirectory());
            assertFalse(entry.isAntiItem());

            assert(archive.getNextEntry() == null);
        } finally {
            archive.close();
        }

    }

    public void testCantFinishTwice() throws Exception {
        output = new File(dir, "finish.7z");
        SevenZOutputFile outArchive = new SevenZOutputFile(output);
        try {
            outArchive.finish();
            outArchive.finish();
            fail("shouldn't be able to call finish twice");
        } catch (IOException ex) {
            assertEquals("This archive has already been finished", ex.getMessage());
        } finally {
            outArchive.close();
        }
    }

    public void testSixEmptyFiles() throws Exception {
        testCompress252(6, 0);
    }

    public void testSixFilesSomeNotEmpty() throws Exception {
        testCompress252(6, 2);
    }

    public void testSevenEmptyFiles() throws Exception {
        testCompress252(7, 0);
    }

    public void testSevenFilesSomeNotEmpty() throws Exception {
        testCompress252(7, 2);
    }

    public void testEightEmptyFiles() throws Exception {
        testCompress252(8, 0);
    }

    public void testEightFilesSomeNotEmpty() throws Exception {
        testCompress252(8, 2);
    }

    public void testNineEmptyFiles() throws Exception {
        testCompress252(9, 0);
    }

    public void testNineFilesSomeNotEmpty() throws Exception {
        testCompress252(9, 2);
    }

    public void testTwentyNineEmptyFiles() throws Exception {
        testCompress252(29, 0);
    }

    public void testTwentyNineFilesSomeNotEmpty() throws Exception {
        testCompress252(29, 7);
    }

    public void testCopyRoundtrip() throws Exception {
        testRoundTrip(SevenZMethod.COPY);
    }

    public void testBzip2Roundtrip() throws Exception {
        testRoundTrip(SevenZMethod.BZIP2);
    }

    public void testLzma2Roundtrip() throws Exception {
        testRoundTrip(SevenZMethod.LZMA2);
    }

    public void testDeflateRoundtrip() throws Exception {
        testRoundTrip(SevenZMethod.DEFLATE);
    }

    public void testBCJX86Roundtrip() throws Exception {
        if (XZ_BCJ_IS_BUGGY) { return; }
        testFilterRoundTrip(new SevenZMethodConfiguration(SevenZMethod.BCJ_X86_FILTER));
    }

    public void testBCJARMRoundtrip() throws Exception {
        if (XZ_BCJ_IS_BUGGY) { return; }
        testFilterRoundTrip(new SevenZMethodConfiguration(SevenZMethod.BCJ_ARM_FILTER));
    }

    public void testBCJARMThumbRoundtrip() throws Exception {
        if (XZ_BCJ_IS_BUGGY) { return; }
        testFilterRoundTrip(new SevenZMethodConfiguration(SevenZMethod.BCJ_ARM_THUMB_FILTER));
    }

    public void testBCJIA64Roundtrip() throws Exception {
        if (XZ_BCJ_IS_BUGGY) { return; }
        testFilterRoundTrip(new SevenZMethodConfiguration(SevenZMethod.BCJ_IA64_FILTER));
    }

    public void testBCJPPCRoundtrip() throws Exception {
        if (XZ_BCJ_IS_BUGGY) { return; }
        testFilterRoundTrip(new SevenZMethodConfiguration(SevenZMethod.BCJ_PPC_FILTER));
    }

    public void testBCJSparcRoundtrip() throws Exception {
        if (XZ_BCJ_IS_BUGGY) { return; }
        testFilterRoundTrip(new SevenZMethodConfiguration(SevenZMethod.BCJ_SPARC_FILTER));
    }

    public void testDeltaRoundtrip() throws Exception {
        testFilterRoundTrip(new SevenZMethodConfiguration(SevenZMethod.DELTA_FILTER));
    }

    public void testStackOfContentCompressions() throws Exception {
        output = new File(dir, "multiple-methods.7z");
        ArrayList<SevenZMethodConfiguration> methods = new ArrayList<SevenZMethodConfiguration>();
        methods.add(new SevenZMethodConfiguration(SevenZMethod.LZMA2));
        methods.add(new SevenZMethodConfiguration(SevenZMethod.COPY));
        methods.add(new SevenZMethodConfiguration(SevenZMethod.DEFLATE));
        methods.add(new SevenZMethodConfiguration(SevenZMethod.BZIP2));
        createAndReadBack(output, methods);
    }

    public void testDeflateWithConfiguration() throws Exception {
        output = new File(dir, "deflate-options.7z");
        // Deflater.BEST_SPEED
        createAndReadBack(output, Collections
                          .singletonList(new SevenZMethodConfiguration(SevenZMethod.DEFLATE, 1)));
    }

    public void testBzip2WithConfiguration() throws Exception {
        output = new File(dir, "bzip2-options.7z");
        // 400k block size
        createAndReadBack(output, Collections
                          .singletonList(new SevenZMethodConfiguration(SevenZMethod.BZIP2, 4)));
    }

    public void testLzma2WithIntConfiguration() throws Exception {
        output = new File(dir, "lzma2-options.7z");
        // 1 MB dictionary
        createAndReadBack(output, Collections
                          .singletonList(new SevenZMethodConfiguration(SevenZMethod.LZMA2, 1 << 20)));
    }

    public void testLzma2WithOptionsConfiguration() throws Exception {
        output = new File(dir, "lzma2-options2.7z");
        LZMA2Options opts = new LZMA2Options(1);
        createAndReadBack(output, Collections
                          .singletonList(new SevenZMethodConfiguration(SevenZMethod.LZMA2, opts)));
    }

    public void testArchiveWithMixedMethods() throws Exception {
        output = new File(dir, "mixed-methods.7z");
        SevenZOutputFile outArchive = new SevenZOutputFile(output);
        try {
            addFile(outArchive, 0, true);
            addFile(outArchive, 1, true, Arrays.asList(new SevenZMethodConfiguration(SevenZMethod.BZIP2)));
        } finally {
            outArchive.close();
        }

        SevenZFile archive = new SevenZFile(output);
        try {
            assertEquals(Boolean.TRUE,
                         verifyFile(archive, 0, Arrays.asList(new SevenZMethodConfiguration(SevenZMethod.LZMA2))));
            assertEquals(Boolean.TRUE,
                         verifyFile(archive, 1, Arrays.asList(new SevenZMethodConfiguration(SevenZMethod.BZIP2))));
        } finally {
            archive.close();
        }
    }

    private void testCompress252(int numberOfFiles, int numberOfNonEmptyFiles)
        throws Exception {
        int nonEmptyModulus = numberOfNonEmptyFiles != 0
            ? numberOfFiles / numberOfNonEmptyFiles
            : numberOfFiles + 1;
        int nonEmptyFilesAdded = 0;
        output = new File(dir, "COMPRESS252-" + numberOfFiles + "-" + numberOfNonEmptyFiles + ".7z");
        SevenZOutputFile archive = new SevenZOutputFile(output);
        try {
            addDir(archive);
            for (int i = 0; i < numberOfFiles; i++) {
                addFile(archive, i,
                        (i + 1) % nonEmptyModulus == 0 && nonEmptyFilesAdded++ < numberOfNonEmptyFiles);
            }
        } finally {
            archive.close();
        }
        verifyCompress252(output, numberOfFiles, numberOfNonEmptyFiles);
    }

    private void verifyCompress252(File output, int numberOfFiles, int numberOfNonEmptyFiles)
        throws Exception {
        SevenZFile archive = new SevenZFile(output);
        int filesFound = 0;
        int nonEmptyFilesFound = 0;
        try {
            verifyDir(archive);
            Boolean b = verifyFile(archive, filesFound++);
            while (b != null) {
                if (Boolean.TRUE.equals(b)) {
                    nonEmptyFilesFound++;
                }
                b = verifyFile(archive, filesFound++);
            }
        } finally {
            archive.close();
        }
        assertEquals(numberOfFiles + 1, filesFound);
        assertEquals(numberOfNonEmptyFiles, nonEmptyFilesFound);
    }

    private void addDir(SevenZOutputFile archive) throws Exception {
        SevenZArchiveEntry entry = archive.createArchiveEntry(dir, "foo/");
        archive.putArchiveEntry(entry);
        archive.closeArchiveEntry();
    }

    private void verifyDir(SevenZFile archive) throws Exception {
        SevenZArchiveEntry entry = archive.getNextEntry();
        assertNotNull(entry);
        assertEquals("foo/", entry.getName());
        assertTrue(entry.isDirectory());
    }

    private void addFile(SevenZOutputFile archive, int index, boolean nonEmpty)
        throws Exception {
        addFile(archive, index, nonEmpty, null);
    }

    private void addFile(SevenZOutputFile archive, int index, boolean nonEmpty, Iterable<SevenZMethodConfiguration> methods)
        throws Exception {
        SevenZArchiveEntry entry = new SevenZArchiveEntry();
        entry.setName("foo/" + index + ".txt");
        entry.setContentMethods(methods);
        archive.putArchiveEntry(entry);
        archive.write(nonEmpty ? new byte[] { 'A' } : new byte[0]);
        archive.closeArchiveEntry();
    }

    private Boolean verifyFile(SevenZFile archive, int index) throws Exception {
        return verifyFile(archive, index, null);
    }

    private Boolean verifyFile(SevenZFile archive, int index,
                               Iterable<SevenZMethodConfiguration> methods) throws Exception {
        SevenZArchiveEntry entry = archive.getNextEntry();
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

    private void testRoundTrip(SevenZMethod method) throws Exception {
        output = new File(dir, method + "-roundtrip.7z");
        ArrayList<SevenZMethodConfiguration> methods = new ArrayList<SevenZMethodConfiguration>();
        methods.add(new SevenZMethodConfiguration(method));
        createAndReadBack(output, methods);
    }

    private void testFilterRoundTrip(SevenZMethodConfiguration method) throws Exception {
        output = new File(dir, method.getMethod() + "-roundtrip.7z");
        ArrayList<SevenZMethodConfiguration> methods = new ArrayList<SevenZMethodConfiguration>();
        methods.add(method);
        methods.add(new SevenZMethodConfiguration(SevenZMethod.LZMA2));
        createAndReadBack(output, methods);
    }

    private void createAndReadBack(File output, Iterable<SevenZMethodConfiguration> methods) throws Exception {
        SevenZOutputFile outArchive = new SevenZOutputFile(output);
        outArchive.setContentMethods(methods);
        try {
            addFile(outArchive, 0, true);
        } finally {
            outArchive.close();
        }

        SevenZFile archive = new SevenZFile(output);
        try {
            assertEquals(Boolean.TRUE, verifyFile(archive, 0, methods));
        } finally {
            archive.close();
        }
    }

    private static void assertContentMethodsEquals(Iterable<? extends SevenZMethodConfiguration> expected,
                                                   Iterable<? extends SevenZMethodConfiguration> actual) {
        assertNotNull(actual);
        Iterator<? extends SevenZMethodConfiguration> expectedIter = expected.iterator();
        Iterator<? extends SevenZMethodConfiguration> actualIter = actual.iterator();
        while (expectedIter.hasNext()) {
            assertTrue(actualIter.hasNext());
            SevenZMethodConfiguration expConfig = expectedIter.next();
            SevenZMethodConfiguration actConfig = actualIter.next();
            assertEquals(expConfig.getMethod(), actConfig.getMethod());
        }
        assertFalse(actualIter.hasNext());
    }
}
