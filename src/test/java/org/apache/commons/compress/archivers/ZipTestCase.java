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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.IOUtils;

public final class ZipTestCase extends AbstractTestCase {
    /**
     * Archives 2 files and unarchives it again. If the file length of result
     * and source is the same, it looks like the operations have worked
     * @throws Exception
     */
    public void testZipArchiveCreation() throws Exception {
        // Archive
        final File output = new File(dir, "bla.zip");
        final File file1 = getFile("test1.xml");
        final File file2 = getFile("test2.xml");

        final OutputStream out = new FileOutputStream(output);
        ArchiveOutputStream os = null;
        try {
            os = new ArchiveStreamFactory()
                .createArchiveOutputStream("zip", out);
            os.putArchiveEntry(new ZipArchiveEntry("testdata/test1.xml"));
            IOUtils.copy(new FileInputStream(file1), os);
            os.closeArchiveEntry();

            os.putArchiveEntry(new ZipArchiveEntry("testdata/test2.xml"));
            IOUtils.copy(new FileInputStream(file2), os);
            os.closeArchiveEntry();
        } finally {
            if (os != null) {
                os.close();
            }
        }
        out.close();

        // Unarchive the same
        List results = new ArrayList();

        final InputStream is = new FileInputStream(output);
        ArchiveInputStream in = null;
        try {
            in = new ArchiveStreamFactory()
                .createArchiveInputStream("zip", is);

            ZipArchiveEntry entry = null;
            while((entry = (ZipArchiveEntry)in.getNextEntry()) != null) {
                File outfile = new File(resultDir.getCanonicalPath() + "/result/" + entry.getName());
                outfile.getParentFile().mkdirs();
                OutputStream o = new FileOutputStream(outfile);
                try {
                    IOUtils.copy(in, o);
                } finally {
                    o.close();
                }
                results.add(outfile);
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
        is.close();

        assertEquals(results.size(), 2);
        File result = (File)results.get(0);
        assertEquals(file1.length(), result.length());
        result = (File)results.get(1);
        assertEquals(file2.length(), result.length());
    }

    /**
     * Simple unarchive test. Asserts nothing.
     * @throws Exception
     */
    public void testZipUnarchive() throws Exception {
        final File input = getFile("bla.zip");
        final InputStream is = new FileInputStream(input);
        final ArchiveInputStream in = new ArchiveStreamFactory().createArchiveInputStream("zip", is);
        final ZipArchiveEntry entry = (ZipArchiveEntry)in.getNextEntry();
        final OutputStream out = new FileOutputStream(new File(dir, entry.getName()));
        IOUtils.copy(in, out);
        out.close();
        in.close();
    }

    /**
     * Test case for
     * <a href="https://issues.apache.org/jira/browse/COMPRESS-93"
     * >COMPRESS-93</a>.
     */
    public void testSupportedCompressionMethod() throws IOException {
        ZipFile bla = new ZipFile(getFile("bla.zip"));
        assertTrue(bla.getEntry("test1.xml").isSupportedCompressionMethod());
        bla.close();

        ZipFile moby = new ZipFile(getFile("moby.zip"));
        assertFalse(moby.getEntry("README").isSupportedCompressionMethod());
        moby.close();
    }

    /**
     * Test case for being able to skip an entry in an 
     * {@link ZipArchiveInputStream} even if the compression method of that
     * entry is unsupported.
     *
     * @see <a href="https://issues.apache.org/jira/browse/COMPRESS-93"
     *        >COMPRESS-93</a>
     */
    public void testSkipEntryWithUnsupportedCompressionMethod()
            throws IOException {
        ZipArchiveInputStream zip =
            new ZipArchiveInputStream(new FileInputStream(getFile("moby.zip")));
        try {
            ZipArchiveEntry entry = zip.getNextZipEntry();
            assertEquals("README", entry.getName());
            assertFalse(entry.isSupportedCompressionMethod());
            try {
                assertNull(zip.getNextZipEntry());
            } catch (IOException e) {
                fail("COMPRESS-93: Unable to skip an unsupported zip entry");
            }
        } finally {
            zip.close();
        }
    }

    /**
     * Checks if all entries from a nested archive can be read.
     * The archive: OSX_ArchiveWithNestedArchive.zip contains:
     * NestedArchiv.zip and test.xml3.
     * 
     * The nested archive:  NestedArchive.zip contains test1.xml and test2.xml
     * 
     * @throws Exception
     */
    public void testListAllFilesWithNestedArchive() throws Exception {
        final File input = getFile("OSX_ArchiveWithNestedArchive.zip");
       
        List results = new ArrayList();

        final InputStream is = new FileInputStream(input);
        ArchiveInputStream in = null;
        try {
            in = new ArchiveStreamFactory().createArchiveInputStream("zip", is);

            ZipArchiveEntry entry = null;
            while((entry = (ZipArchiveEntry)in.getNextEntry()) != null) {
                results.add((entry.getName()));

                ArchiveInputStream nestedIn = new ArchiveStreamFactory().createArchiveInputStream("zip", in);
                ZipArchiveEntry nestedEntry = null;
                while((nestedEntry = (ZipArchiveEntry)nestedIn.getNextEntry()) != null) {
                    results.add(nestedEntry.getName());
                }
               // nested stream must not be closed here
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
        is.close();
        
        results.contains("NestedArchiv.zip");
        results.contains("test1.xml");
        results.contains("test2.xml");
        results.contains("test3.xml");
    }

    public void testDirectoryEntryFromFile() throws Exception {
        File[] tmp = createTempDirAndFile();
        File archive = null;
        ZipArchiveOutputStream zos = null;
        ZipFile zf = null;
        try {
            archive = File.createTempFile("test.", ".zip", tmp[0]);
            archive.deleteOnExit();
            zos = new ZipArchiveOutputStream(archive);
            long beforeArchiveWrite = tmp[0].lastModified();
            ZipArchiveEntry in = new ZipArchiveEntry(tmp[0], "foo");
            zos.putArchiveEntry(in);
            zos.closeArchiveEntry();
            zos.close();
            zos = null;
            zf = new ZipFile(archive);
            ZipArchiveEntry out = zf.getEntry("foo/");
            assertNotNull(out);
            assertEquals("foo/", out.getName());
            assertEquals(0, out.getSize());
            // ZIP stores time with a granularity of 2 seconds
            assertEquals(beforeArchiveWrite / 2000,
                         out.getLastModifiedDate().getTime() / 2000);
            assertTrue(out.isDirectory());
        } finally {
            ZipFile.closeQuietly(zf);
            if (zos != null) {
                zos.close();
            }
            if (archive != null) {
                archive.delete();
            }
            tmp[1].delete();
            tmp[0].delete();
        }
    }

    public void testExplicitDirectoryEntry() throws Exception {
        File[] tmp = createTempDirAndFile();
        File archive = null;
        ZipArchiveOutputStream zos = null;
        ZipFile zf = null;
        try {
            archive = File.createTempFile("test.", ".zip", tmp[0]);
            archive.deleteOnExit();
            zos = new ZipArchiveOutputStream(archive);
            long beforeArchiveWrite = tmp[0].lastModified();
            ZipArchiveEntry in = new ZipArchiveEntry("foo/");
            in.setTime(beforeArchiveWrite);
            zos.putArchiveEntry(in);
            zos.closeArchiveEntry();
            zos.close();
            zos = null;
            zf = new ZipFile(archive);
            ZipArchiveEntry out = zf.getEntry("foo/");
            assertNotNull(out);
            assertEquals("foo/", out.getName());
            assertEquals(0, out.getSize());
            assertEquals(beforeArchiveWrite / 2000,
                         out.getLastModifiedDate().getTime() / 2000);
            assertTrue(out.isDirectory());
        } finally {
            ZipFile.closeQuietly(zf);
            if (zos != null) {
                zos.close();
            }
            if (archive != null) {
                archive.delete();
            }
            tmp[1].delete();
            tmp[0].delete();
        }
    }

    public void testFileEntryFromFile() throws Exception {
        File[] tmp = createTempDirAndFile();
        File archive = null;
        ZipArchiveOutputStream zos = null;
        ZipFile zf = null;
        FileInputStream fis = null;
        try {
            archive = File.createTempFile("test.", ".zip", tmp[0]);
            archive.deleteOnExit();
            zos = new ZipArchiveOutputStream(archive);
            ZipArchiveEntry in = new ZipArchiveEntry(tmp[1], "foo");
            zos.putArchiveEntry(in);
            byte[] b = new byte[(int) tmp[1].length()];
            fis = new FileInputStream(tmp[1]);
            int read;
            while ((read = fis.read(b)) > 0) {
                zos.write(b);
            }
            fis.close();
            fis = null;
            zos.closeArchiveEntry();
            zos.close();
            zos = null;
            zf = new ZipFile(archive);
            ZipArchiveEntry out = zf.getEntry("foo");
            assertNotNull(out);
            assertEquals("foo", out.getName());
            assertEquals(tmp[1].length(), out.getSize());
            assertEquals(tmp[1].lastModified() / 2000,
                         out.getLastModifiedDate().getTime() / 2000);
            assertFalse(out.isDirectory());
        } finally {
            ZipFile.closeQuietly(zf);
            if (zos != null) {
                zos.close();
            }
            if (archive != null) {
                archive.delete();
            }
            if (fis != null) {
                fis.close();
            }
            tmp[1].delete();
            tmp[0].delete();
        }
    }

    public void testExplicitFileEntry() throws Exception {
        File[] tmp = createTempDirAndFile();
        File archive = null;
        ZipArchiveOutputStream zos = null;
        ZipFile zf = null;
        FileInputStream fis = null;
        try {
            archive = File.createTempFile("test.", ".zip", tmp[0]);
            archive.deleteOnExit();
            zos = new ZipArchiveOutputStream(archive);
            ZipArchiveEntry in = new ZipArchiveEntry("foo");
            in.setTime(tmp[1].lastModified());
            in.setSize(tmp[1].length());
            zos.putArchiveEntry(in);
            byte[] b = new byte[(int) tmp[1].length()];
            fis = new FileInputStream(tmp[1]);
            int read;
            while ((read = fis.read(b)) > 0) {
                zos.write(b);
            }
            fis.close();
            fis = null;
            zos.closeArchiveEntry();
            zos.close();
            zos = null;
            zf = new ZipFile(archive);
            ZipArchiveEntry out = zf.getEntry("foo");
            assertNotNull(out);
            assertEquals("foo", out.getName());
            assertEquals(tmp[1].length(), out.getSize());
            assertEquals(tmp[1].lastModified() / 2000,
                         out.getLastModifiedDate().getTime() / 2000);
            assertFalse(out.isDirectory());
        } finally {
            ZipFile.closeQuietly(zf);
            if (zos != null) {
                zos.close();
            }
            if (archive != null) {
                archive.delete();
            }
            if (fis != null) {
                fis.close();
            }
            tmp[1].delete();
            tmp[0].delete();
        }
    }
}
