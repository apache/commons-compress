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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream; 	
import java.io.FileOutputStream; 	
import java.io.IOException; 	
import java.io.InputStream; 	
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.archivers.zip.Zip64Mode;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntryPredicate;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream; 	
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream; 	
import org.apache.commons.compress.archivers.zip.ZipFile; 	
import org.apache.commons.compress.archivers.zip.ZipMethod;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.Assert;

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
        List<File> results = new ArrayList<File>();

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
        File result = results.get(0);
        assertEquals(file1.length(), result.length());
        result = results.get(1);
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
     * <a href="https://issues.apache.org/jira/browse/COMPRESS-208"
     * >COMPRESS-208</a>.
     */
    public void testSkipsPK00Prefix() throws Exception {
        final File input = getFile("COMPRESS-208.zip");
        InputStream is = new FileInputStream(input);
        ArrayList<String> al = new ArrayList<String>();
        al.add("test1.xml");
        al.add("test2.xml");
        try {
            checkArchiveContent(new ZipArchiveInputStream(is), al);
        } finally {
            is.close();
        }
    }

    /**
     * Test case for
     * <a href="https://issues.apache.org/jira/browse/COMPRESS-93"
     * >COMPRESS-93</a>.
     */
    public void testSupportedCompressionMethod() throws IOException {
        /*
        ZipFile bla = new ZipFile(getFile("bla.zip"));
        assertTrue(bla.canReadEntryData(bla.getEntry("test1.xml")));
        bla.close();
        */
        
        ZipFile moby = new ZipFile(getFile("moby.zip"));
        ZipArchiveEntry entry = moby.getEntry("README");
        assertEquals("method", ZipMethod.TOKENIZATION.getCode(), entry.getMethod());
        assertFalse(moby.canReadEntryData(entry));
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
            assertEquals("method", ZipMethod.TOKENIZATION.getCode(), entry.getMethod());
            assertEquals("README", entry.getName());
            assertFalse(zip.canReadEntryData(entry));
            try {
                assertNull(zip.getNextZipEntry());
            } catch (IOException e) {
                e.printStackTrace();
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

        List<String> results = new ArrayList<String>();

        final InputStream is = new FileInputStream(input);
        ArchiveInputStream in = null;
        try {
            in = new ArchiveStreamFactory().createArchiveInputStream("zip", is);

            ZipArchiveEntry entry = null;
            while((entry = (ZipArchiveEntry)in.getNextEntry()) != null) {
                results.add(entry.getName());

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
            tryHardToDelete(archive);
            tryHardToDelete(tmp[1]);
            rmdir(tmp[0]);
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
            tryHardToDelete(archive);
            tryHardToDelete(tmp[1]);
            rmdir(tmp[0]);
        }
    }
    String first_payload = "ABBA";
    String second_payload = "AAAAAAAAAAAA";
    ZipArchiveEntryPredicate allFilesPredicate = new ZipArchiveEntryPredicate() {
        public boolean test(ZipArchiveEntry zipArchiveEntry) {
            return true;
        }
    };

    public void testCopyRawEntriesFromFile()
            throws IOException {

        File[] tmp = createTempDirAndFile();
        File reference = createReferenceFile(tmp[0], Zip64Mode.Never, "expected.");

        File a1 = File.createTempFile("src1.", ".zip", tmp[0]);
        ZipArchiveOutputStream zos = new ZipArchiveOutputStream(a1);
        zos.setUseZip64(Zip64Mode.Never);
        createFirstEntry(zos).close();

        File a2 = File.createTempFile("src2.", ".zip", tmp[0]);
        ZipArchiveOutputStream zos1 = new ZipArchiveOutputStream(a2);
        zos1.setUseZip64(Zip64Mode.Never);
        createSecondEntry(zos1).close();

        ZipFile zf1 = new ZipFile(a1);
        ZipFile zf2 = new ZipFile(a2);
        File fileResult = File.createTempFile("file-actual.", ".zip", tmp[0]);
        ZipArchiveOutputStream zos2 = new ZipArchiveOutputStream(fileResult);
        zf1.copyRawEntries(zos2, allFilesPredicate);
        zf2.copyRawEntries(zos2, allFilesPredicate);
        zos2.close();
        // copyRawEntries does not add superfluous zip64 header like regular zip output stream
        // does when using Zip64Mode.AsNeeded so all the source material has to be Zip64Mode.Never,
        // if exact binary equality is to be achieved
        assertSameFileContents(reference, fileResult);
        zf1.close();
        zf2.close();
    }

    public void testCopyRawZip64EntryFromFile()
            throws IOException {

        File[] tmp = createTempDirAndFile();
        File reference = File.createTempFile("z64reference.", ".zip", tmp[0]);
        ZipArchiveOutputStream zos1 = new ZipArchiveOutputStream(reference);
        zos1.setUseZip64(Zip64Mode.Always);
        createFirstEntry(zos1);
        zos1.close();

        File a1 = File.createTempFile("zip64src.", ".zip", tmp[0]);
        ZipArchiveOutputStream zos = new ZipArchiveOutputStream(a1);
        zos.setUseZip64(Zip64Mode.Always);
        createFirstEntry(zos).close();

        ZipFile zf1 = new ZipFile(a1);
        File fileResult = File.createTempFile("file-actual.", ".zip", tmp[0]);
        ZipArchiveOutputStream zos2 = new ZipArchiveOutputStream(fileResult);
        zos2.setUseZip64(Zip64Mode.Always);
        zf1.copyRawEntries(zos2, allFilesPredicate);
        zos2.close();
        assertSameFileContents(reference, fileResult);
        zf1.close();
    }
    public void testUnixModeInAddRaw() throws IOException {

        File[] tmp = createTempDirAndFile();

        File a1 = File.createTempFile("unixModeBits.", ".zip", tmp[0]);
        ZipArchiveOutputStream zos = new ZipArchiveOutputStream(a1);

        ZipArchiveEntry archiveEntry = new ZipArchiveEntry("fred");
        archiveEntry.setUnixMode(0664);
        archiveEntry.setMethod(ZipEntry.DEFLATED);
        zos.addRawArchiveEntry(archiveEntry, new ByteArrayInputStream("fud".getBytes()));
        zos.close();

        ZipFile zf1 = new ZipFile(a1);
        ZipArchiveEntry fred = zf1.getEntry("fred");
        assertEquals(0664, fred.getUnixMode());
        zf1.close();
    }

    private File createReferenceFile(File directory, Zip64Mode zipMode, String prefix) throws IOException {
        File reference = File.createTempFile(prefix, ".zip", directory);
        ZipArchiveOutputStream zos = new ZipArchiveOutputStream(reference);
        zos.setUseZip64(zipMode);
        createFirstEntry(zos);
        createSecondEntry(zos);
        zos.close();
        return reference;
    }

    private ZipArchiveOutputStream createFirstEntry(ZipArchiveOutputStream zos) throws IOException {
        createArchiveEntry(first_payload, zos, "file1.txt");
        return zos;
    }

    private ZipArchiveOutputStream createSecondEntry(ZipArchiveOutputStream zos) throws IOException {
        createArchiveEntry(second_payload, zos, "file2.txt");
        return zos;
    }


    private void assertSameFileContents(File expectedFile, File actualFile) throws IOException {
        int size = (int) Math.max(expectedFile.length(), actualFile.length());
        ZipFile expected = new ZipFile(expectedFile);
        ZipFile actual = new ZipFile(actualFile);
        byte[] expectedBuf = new byte[size];
        byte[] actualBuf = new byte[size];

        Enumeration<ZipArchiveEntry> actualInOrder = actual.getEntriesInPhysicalOrder();
        Enumeration<ZipArchiveEntry> expectedInOrder = expected.getEntriesInPhysicalOrder();

        while (actualInOrder.hasMoreElements()){
            ZipArchiveEntry actualElement = actualInOrder.nextElement();
            ZipArchiveEntry expectedElement = expectedInOrder.nextElement();
            assertEquals( expectedElement.getName(), actualElement.getName());
            // Don't compare timestamps since they may vary;
            // there's no support for stubbed out clock (TimeSource) in ZipArchiveOutputStream
            assertEquals( expectedElement.getMethod(), actualElement.getMethod());
            assertEquals( expectedElement.getGeneralPurposeBit(), actualElement.getGeneralPurposeBit());
            assertEquals( expectedElement.getCrc(), actualElement.getCrc());
            assertEquals( expectedElement.getCompressedSize(), actualElement.getCompressedSize());
            assertEquals( expectedElement.getSize(), actualElement.getSize());
            assertEquals( expectedElement.getExternalAttributes(), actualElement.getExternalAttributes());
            assertEquals( expectedElement.getInternalAttributes(), actualElement.getInternalAttributes());

            InputStream actualIs = actual.getInputStream(actualElement);
            InputStream expectedIs = expected.getInputStream(expectedElement);
            IOUtils.readFully(expectedIs, expectedBuf);
            IOUtils.readFully(actualIs, actualBuf);
            expectedIs.close();
            actualIs.close();
            Assert.assertArrayEquals(expectedBuf, actualBuf); // Buffers are larger than payload. dont care
        }

        expected.close();
        actual.close();
    }


    private void createArchiveEntry(String payload, ZipArchiveOutputStream zos, String name)
            throws IOException {
        ZipArchiveEntry in = new ZipArchiveEntry(name);
        zos.putArchiveEntry(in);

        zos.write(payload.getBytes());
        zos.closeArchiveEntry();
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
            while (fis.read(b) > 0) {
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
            tryHardToDelete(archive);
            if (fis != null) {
                fis.close();
            }
            tryHardToDelete(tmp[1]);
            rmdir(tmp[0]);
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
            while (fis.read(b) > 0) {
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
            tryHardToDelete(archive);
            if (fis != null) {
                fis.close();
            }
            tryHardToDelete(tmp[1]);
            rmdir(tmp[0]);
        }
    }
}
