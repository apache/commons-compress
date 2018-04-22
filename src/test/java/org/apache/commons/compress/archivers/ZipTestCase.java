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

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.archivers.zip.Zip64Mode;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntryPredicate;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.archivers.zip.ZipMethod;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.compress.utils.InputStreamStatistics;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.junit.Assert;
import org.junit.Test;

public final class ZipTestCase extends AbstractTestCase {
    /**
     * Archives 2 files and unarchives it again. If the file length of result
     * and source is the same, it looks like the operations have worked
     * @throws Exception
     */
    @Test
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
        final List<File> results = new ArrayList<>();

        final InputStream is = new FileInputStream(output);
        ArchiveInputStream in = null;
        try {
            in = new ArchiveStreamFactory()
                .createArchiveInputStream("zip", is);

            ZipArchiveEntry entry = null;
            while((entry = (ZipArchiveEntry)in.getNextEntry()) != null) {
                final File outfile = new File(resultDir.getCanonicalPath() + "/result/" + entry.getName());
                outfile.getParentFile().mkdirs();
                try (OutputStream o = new FileOutputStream(outfile)) {
                    IOUtils.copy(in, o);
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
     * Archives 2 files and unarchives it again. If the file contents of result
     * and source is the same, it looks like the operations have worked
     * @throws Exception
     */
    @Test
    public void testZipArchiveCreationInMemory() throws Exception {
        final File file1 = getFile("test1.xml");
        final File file2 = getFile("test2.xml");
        final byte[] file1Contents = new byte[(int) file1.length()];
        final byte[] file2Contents = new byte[(int) file2.length()];
        IOUtils.readFully(new FileInputStream(file1), file1Contents);
        IOUtils.readFully(new FileInputStream(file2), file2Contents);

        SeekableInMemoryByteChannel channel = new SeekableInMemoryByteChannel();
        try (ZipArchiveOutputStream os = new ZipArchiveOutputStream(channel)) {
            os.putArchiveEntry(new ZipArchiveEntry("testdata/test1.xml"));
            os.write(file1Contents);
            os.closeArchiveEntry();

            os.putArchiveEntry(new ZipArchiveEntry("testdata/test2.xml"));
            os.write(file2Contents);
            os.closeArchiveEntry();
        }

        // Unarchive the same
        final List<byte[]> results = new ArrayList<>();

        try (ArchiveInputStream in = new ArchiveStreamFactory()
             .createArchiveInputStream("zip", new ByteArrayInputStream(channel.array()))) {

            ZipArchiveEntry entry;
            while((entry = (ZipArchiveEntry)in.getNextEntry()) != null) {
                byte[] result = new byte[(int) entry.getSize()];
                IOUtils.readFully(in, result);
                results.add(result);
            }
        }

        assertArrayEquals(results.get(0), file1Contents);
        assertArrayEquals(results.get(1), file2Contents);
    }

    /**
     * Simple unarchive test. Asserts nothing.
     * @throws Exception
     */
    @Test
    public void testZipUnarchive() throws Exception {
        final File input = getFile("bla.zip");
        try (final InputStream is = new FileInputStream(input);
                final ArchiveInputStream in = new ArchiveStreamFactory().createArchiveInputStream("zip", is)) {
            final ZipArchiveEntry entry = (ZipArchiveEntry) in.getNextEntry();
            try (final OutputStream out = new FileOutputStream(new File(dir, entry.getName()))) {
                IOUtils.copy(in, out);
            }
        }
    }

    /**
     * Test case for
     * <a href="https://issues.apache.org/jira/browse/COMPRESS-208"
     * >COMPRESS-208</a>.
     */
    @Test
    public void testSkipsPK00Prefix() throws Exception {
        final File input = getFile("COMPRESS-208.zip");
        final ArrayList<String> al = new ArrayList<>();
        al.add("test1.xml");
        al.add("test2.xml");
        try (InputStream is = new FileInputStream(input)) {
            checkArchiveContent(new ZipArchiveInputStream(is), al);
        }
    }

    /**
     * Test case for
     * <a href="https://issues.apache.org/jira/browse/COMPRESS-93"
     * >COMPRESS-93</a>.
     */
    @Test
    public void testSupportedCompressionMethod() throws IOException {
        /*
        ZipFile bla = new ZipFile(getFile("bla.zip"));
        assertTrue(bla.canReadEntryData(bla.getEntry("test1.xml")));
        bla.close();
        */

        final ZipFile moby = new ZipFile(getFile("moby.zip"));
        final ZipArchiveEntry entry = moby.getEntry("README");
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
    @Test
    public void testSkipEntryWithUnsupportedCompressionMethod()
            throws IOException {
        try (ZipArchiveInputStream zip = new ZipArchiveInputStream(new FileInputStream(getFile("moby.zip")))) {
            final ZipArchiveEntry entry = zip.getNextZipEntry();
            assertEquals("method", ZipMethod.TOKENIZATION.getCode(), entry.getMethod());
            assertEquals("README", entry.getName());
            assertFalse(zip.canReadEntryData(entry));
            try {
                assertNull(zip.getNextZipEntry());
            } catch (final IOException e) {
                e.printStackTrace();
                fail("COMPRESS-93: Unable to skip an unsupported zip entry");
            }
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
    @Test
    public void testListAllFilesWithNestedArchive() throws Exception {
        final File input = getFile("OSX_ArchiveWithNestedArchive.zip");

        final List<String> results = new ArrayList<>();
        final List<ZipException> expectedExceptions = new ArrayList<>();

        final InputStream is = new FileInputStream(input);
        ArchiveInputStream in = null;
        try {
            in = new ArchiveStreamFactory().createArchiveInputStream("zip", is);

            ZipArchiveEntry entry = null;
            while ((entry = (ZipArchiveEntry) in.getNextEntry()) != null) {
                results.add(entry.getName());

                final ArchiveInputStream nestedIn = new ArchiveStreamFactory().createArchiveInputStream("zip", in);
                try {
                    ZipArchiveEntry nestedEntry = null;
                    while ((nestedEntry = (ZipArchiveEntry) nestedIn.getNextEntry()) != null) {
                        results.add(nestedEntry.getName());
                    }
                } catch (ZipException ex) {
                    // expected since you cannot create a final ArchiveInputStream from test3.xml
                    expectedExceptions.add(ex);
                }
                // nested stream must not be closed here
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
        is.close();

        assertTrue(results.contains("NestedArchiv.zip"));
        assertTrue(results.contains("test1.xml"));
        assertTrue(results.contains("test2.xml"));
        assertTrue(results.contains("test3.xml"));
        assertEquals(1, expectedExceptions.size());
    }

    @Test
    public void testDirectoryEntryFromFile() throws Exception {
        final File[] tmp = createTempDirAndFile();
        File archive = null;
        ZipArchiveOutputStream zos = null;
        ZipFile zf = null;
        try {
            archive = File.createTempFile("test.", ".zip", tmp[0]);
            archive.deleteOnExit();
            zos = new ZipArchiveOutputStream(archive);
            final long beforeArchiveWrite = tmp[0].lastModified();
            final ZipArchiveEntry in = new ZipArchiveEntry(tmp[0], "foo");
            zos.putArchiveEntry(in);
            zos.closeArchiveEntry();
            zos.close();
            zos = null;
            zf = new ZipFile(archive);
            final ZipArchiveEntry out = zf.getEntry("foo/");
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

    @Test
    public void testExplicitDirectoryEntry() throws Exception {
        final File[] tmp = createTempDirAndFile();
        File archive = null;
        ZipArchiveOutputStream zos = null;
        ZipFile zf = null;
        try {
            archive = File.createTempFile("test.", ".zip", tmp[0]);
            archive.deleteOnExit();
            zos = new ZipArchiveOutputStream(archive);
            final long beforeArchiveWrite = tmp[0].lastModified();
            final ZipArchiveEntry in = new ZipArchiveEntry("foo/");
            in.setTime(beforeArchiveWrite);
            zos.putArchiveEntry(in);
            zos.closeArchiveEntry();
            zos.close();
            zos = null;
            zf = new ZipFile(archive);
            final ZipArchiveEntry out = zf.getEntry("foo/");
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
        @Override
        public boolean test(final ZipArchiveEntry zipArchiveEntry) {
            return true;
        }
    };

    @Test
    public void testCopyRawEntriesFromFile()
            throws IOException {

        final File[] tmp = createTempDirAndFile();
        final File reference = createReferenceFile(tmp[0], Zip64Mode.Never, "expected.");

        final File a1 = File.createTempFile("src1.", ".zip", tmp[0]);
        try (final ZipArchiveOutputStream zos = new ZipArchiveOutputStream(a1)) {
            zos.setUseZip64(Zip64Mode.Never);
            createFirstEntry(zos).close();
        }

        final File a2 = File.createTempFile("src2.", ".zip", tmp[0]);
        try (final ZipArchiveOutputStream zos1 = new ZipArchiveOutputStream(a2)) {
            zos1.setUseZip64(Zip64Mode.Never);
            createSecondEntry(zos1).close();
        }

        try (final ZipFile zf1 = new ZipFile(a1); final ZipFile zf2 = new ZipFile(a2)) {
            final File fileResult = File.createTempFile("file-actual.", ".zip", tmp[0]);
            try (final ZipArchiveOutputStream zos2 = new ZipArchiveOutputStream(fileResult)) {
                zf1.copyRawEntries(zos2, allFilesPredicate);
                zf2.copyRawEntries(zos2, allFilesPredicate);
            }
            // copyRawEntries does not add superfluous zip64 header like regular zip output stream
            // does when using Zip64Mode.AsNeeded so all the source material has to be Zip64Mode.Never,
            // if exact binary equality is to be achieved
            assertSameFileContents(reference, fileResult);
        }
    }

    @Test
    public void testCopyRawZip64EntryFromFile()
            throws IOException {

        final File[] tmp = createTempDirAndFile();
        final File reference = File.createTempFile("z64reference.", ".zip", tmp[0]);
        try (final ZipArchiveOutputStream zos1 = new ZipArchiveOutputStream(reference)) {
            zos1.setUseZip64(Zip64Mode.Always);
            createFirstEntry(zos1);
        }

        final File a1 = File.createTempFile("zip64src.", ".zip", tmp[0]);
        try (final ZipArchiveOutputStream zos = new ZipArchiveOutputStream(a1)) {
            zos.setUseZip64(Zip64Mode.Always);
            createFirstEntry(zos).close();
        }

        final File fileResult = File.createTempFile("file-actual.", ".zip", tmp[0]);
        try (final ZipFile zf1 = new ZipFile(a1)) {
            try (final ZipArchiveOutputStream zos2 = new ZipArchiveOutputStream(fileResult)) {
                zos2.setUseZip64(Zip64Mode.Always);
                zf1.copyRawEntries(zos2, allFilesPredicate);
            }
            assertSameFileContents(reference, fileResult);
        }
    }

    @Test
    public void testUnixModeInAddRaw() throws IOException {

        final File[] tmp = createTempDirAndFile();

        final File a1 = File.createTempFile("unixModeBits.", ".zip", tmp[0]);
        try (final ZipArchiveOutputStream zos = new ZipArchiveOutputStream(a1)) {

            final ZipArchiveEntry archiveEntry = new ZipArchiveEntry("fred");
            archiveEntry.setUnixMode(0664);
            archiveEntry.setMethod(ZipEntry.DEFLATED);
            zos.addRawArchiveEntry(archiveEntry, new ByteArrayInputStream("fud".getBytes()));
        }

        try (final ZipFile zf1 = new ZipFile(a1)) {
            final ZipArchiveEntry fred = zf1.getEntry("fred");
            assertEquals(0664, fred.getUnixMode());
        }
    }

    private File createReferenceFile(final File directory, final Zip64Mode zipMode, final String prefix)
            throws IOException {
        final File reference = File.createTempFile(prefix, ".zip", directory);
        try (final ZipArchiveOutputStream zos = new ZipArchiveOutputStream(reference)) {
            zos.setUseZip64(zipMode);
            createFirstEntry(zos);
            createSecondEntry(zos);
        }
        return reference;
    }

    private ZipArchiveOutputStream createFirstEntry(final ZipArchiveOutputStream zos) throws IOException {
        createArchiveEntry(first_payload, zos, "file1.txt");
        return zos;
    }

    private ZipArchiveOutputStream createSecondEntry(final ZipArchiveOutputStream zos) throws IOException {
        createArchiveEntry(second_payload, zos, "file2.txt");
        return zos;
    }


    private void assertSameFileContents(final File expectedFile, final File actualFile) throws IOException {
        final int size = (int) Math.max(expectedFile.length(), actualFile.length());
        try (final ZipFile expected = new ZipFile(expectedFile); final ZipFile actual = new ZipFile(actualFile)) {
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

                final InputStream actualIs = actual.getInputStream(actualElement);
                final InputStream expectedIs = expected.getInputStream(expectedElement);
                IOUtils.readFully(expectedIs, expectedBuf);
                IOUtils.readFully(actualIs, actualBuf);
                expectedIs.close();
                actualIs.close();
                Assert.assertArrayEquals(expectedBuf, actualBuf); // Buffers are larger than payload. dont care
            }

        }
    }


    private void createArchiveEntry(final String payload, final ZipArchiveOutputStream zos, final String name)
            throws IOException {
        final ZipArchiveEntry in = new ZipArchiveEntry(name);
        zos.putArchiveEntry(in);

        zos.write(payload.getBytes());
        zos.closeArchiveEntry();
    }

    @Test
    public void testFileEntryFromFile() throws Exception {
        final File[] tmp = createTempDirAndFile();
        File archive = null;
        ZipArchiveOutputStream zos = null;
        ZipFile zf = null;
        FileInputStream fis = null;
        try {
            archive = File.createTempFile("test.", ".zip", tmp[0]);
            archive.deleteOnExit();
            zos = new ZipArchiveOutputStream(archive);
            final ZipArchiveEntry in = new ZipArchiveEntry(tmp[1], "foo");
            zos.putArchiveEntry(in);
            final byte[] b = new byte[(int) tmp[1].length()];
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
            final ZipArchiveEntry out = zf.getEntry("foo");
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

    @Test
    public void testExplicitFileEntry() throws Exception {
        final File[] tmp = createTempDirAndFile();
        File archive = null;
        ZipArchiveOutputStream zos = null;
        ZipFile zf = null;
        FileInputStream fis = null;
        try {
            archive = File.createTempFile("test.", ".zip", tmp[0]);
            archive.deleteOnExit();
            zos = new ZipArchiveOutputStream(archive);
            final ZipArchiveEntry in = new ZipArchiveEntry("foo");
            in.setTime(tmp[1].lastModified());
            in.setSize(tmp[1].length());
            zos.putArchiveEntry(in);
            final byte[] b = new byte[(int) tmp[1].length()];
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
            final ZipArchiveEntry out = zf.getEntry("foo");
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

    @Test
    public void inputStreamStatisticsOfZipBombExcel() throws IOException, ArchiveException {
        Map<String, List<Long>> expected = new HashMap<String, List<Long>>() {{
            put("[Content_Types].xml", Arrays.asList(8390036L, 8600L));
            put("xl/worksheets/sheet1.xml", Arrays.asList(1348L, 508L));
        }};
        testInputStreamStatistics("zipbomb.xlsx", expected);
    }

    @Test
    public void inputStreamStatisticsForImplodedEntry() throws IOException, ArchiveException {
        Map<String, List<Long>> expected = new HashMap<String, List<Long>>() {{
            put("LICENSE.TXT", Arrays.asList(11560L, 4131L));
        }};
        testInputStreamStatistics("imploding-8Kdict-3trees.zip", expected);
    }

    @Test
    public void inputStreamStatisticsForShrunkEntry() throws IOException, ArchiveException {
        Map<String, List<Long>> expected = new HashMap<String, List<Long>>() {{
            put("TEST1.XML", Arrays.asList(76L, 66L));
            put("TEST2.XML", Arrays.asList(81L, 76L));
        }};
        testInputStreamStatistics("SHRUNK.ZIP", expected);
    }

    @Test
    public void inputStreamStatisticsForStoredEntry() throws IOException, ArchiveException {
        Map<String, List<Long>> expected = new HashMap<String, List<Long>>() {{
            put("test.txt", Arrays.asList(5L, 5L));
        }};
        testInputStreamStatistics("COMPRESS-264.zip", expected);
    }

    @Test
    public void inputStreamStatisticsForBzip2Entry() throws IOException, ArchiveException {
        Map<String, List<Long>> expected = new HashMap<String, List<Long>>() {{
            put("lots-of-as", Arrays.asList(42L, 39L));
        }};
        testInputStreamStatistics("bzip2-zip.zip", expected);
    }

    @Test
    public void inputStreamStatisticsForDeflate64Entry() throws IOException, ArchiveException {
        Map<String, List<Long>> expected = new HashMap<String, List<Long>>() {{
            put("input2", Arrays.asList(3072L, 2111L));
        }};
        testInputStreamStatistics("COMPRESS-380/COMPRESS-380.zip", expected);
    }

    private void testInputStreamStatistics(String fileName, Map<String, List<Long>> expectedStatistics)
        throws IOException, ArchiveException {
        final File input = getFile(fileName);

        final Map<String,List<List<Long>>> actualStatistics = new HashMap<>();

        // stream access
        try (final FileInputStream fis = new FileInputStream(input);
            final ArchiveInputStream in = new ArchiveStreamFactory().createArchiveInputStream("zip", fis)) {
            for (ArchiveEntry entry; (entry = in.getNextEntry()) != null; ) {
                readStream(in, entry, actualStatistics);
            }
        }

        // file access
        try (final ZipFile zf = new ZipFile(input)) {
            final Enumeration<ZipArchiveEntry> entries = zf.getEntries();
            while (entries.hasMoreElements()) {
                final ZipArchiveEntry zae = entries.nextElement();
                try (InputStream in = zf.getInputStream(zae)) {
                    readStream(in, zae, actualStatistics);
                }
            }
        }

        // compare statistics of stream / file access
        for (Map.Entry<String,List<List<Long>>> me : actualStatistics.entrySet()) {
            assertEquals("Mismatch of stats for: " + me.getKey(),
                         me.getValue().get(0), me.getValue().get(1));
        }

        for (Map.Entry<String, List<Long>> me : expectedStatistics.entrySet()) {
            assertEquals("Mismatch of stats with expected value for: " + me.getKey(),
                me.getValue(), actualStatistics.get(me.getKey()).get(0));
        }
    }

    private void readStream(final InputStream in, final ArchiveEntry entry, final Map<String,List<List<Long>>> map) throws IOException {
        final byte[] buf = new byte[4096];
        final InputStreamStatistics stats = (InputStreamStatistics) in;
        while (in.read(buf) != -1);

        final String name = entry.getName();
        final List<List<Long>> l;
        if (map.containsKey(name)) {
            l = map.get(name);
        } else {
            map.put(name, l = new ArrayList<>());
        }

        final long t = stats.getUncompressedCount();
        final long b = stats.getCompressedCount();
        l.add(Arrays.asList(t, b));
    }
}
