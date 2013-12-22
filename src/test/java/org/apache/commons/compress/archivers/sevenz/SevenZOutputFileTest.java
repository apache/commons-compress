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
import java.util.Calendar;
import java.util.Date;
import org.apache.commons.compress.AbstractTestCase;

public class SevenZOutputFileTest extends AbstractTestCase {

    private File output;

    @Override
    public void tearDown() throws Exception {
        if (output != null && !output.delete()) {
            output.deleteOnExit();
        }
        super.tearDown();
    }

    public void testDirectoriesAndEmptyFiles() throws Exception {
        File output = new File(dir, "empties.7z");

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
        File output = new File(dir, "dirs.7z");
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
        File output = new File(dir, "finish.7z");
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
        SevenZArchiveEntry entry = new SevenZArchiveEntry();
        entry.setName("foo/" + index + ".txt");
        archive.putArchiveEntry(entry);
        archive.write(nonEmpty ? new byte[] { 17 } : new byte[0]);
        archive.closeArchiveEntry();
    }

    private Boolean verifyFile(SevenZFile archive, int index) throws Exception {
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
        assertEquals(17, archive.read());
        assertEquals(-1, archive.read());
        return Boolean.TRUE;
    }

}
