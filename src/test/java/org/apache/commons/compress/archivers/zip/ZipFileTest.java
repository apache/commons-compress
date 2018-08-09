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

package org.apache.commons.compress.archivers.zip;

import static org.apache.commons.compress.AbstractTestCase.getFile;
import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class ZipFileTest {
    private ZipFile zf = null;

    @After
    public void tearDown() {
        ZipFile.closeQuietly(zf);
    }

    @Test
    public void testCDOrder() throws Exception {
        readOrderTest();
        final ArrayList<ZipArchiveEntry> l = Collections.list(zf.getEntries());
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
        assertEntryName(l, 19, "ZipUtil");
        assertEntryName(l, 20, "ZipLong");
        assertEntryName(l, 21, "ZipShort");
        assertEntryName(l, 22, "ZipFile");
    }

    @Test
    public void testCDOrderInMemory() throws Exception {
        byte[] data = null;
        try (FileInputStream fis = new FileInputStream(getFile("ordertest.zip"))) {
            data = IOUtils.toByteArray(fis);
        }

        zf = new ZipFile(new SeekableInMemoryByteChannel(data), ZipEncodingHelper.UTF8);
        final ArrayList<ZipArchiveEntry> l = Collections.list(zf.getEntries());
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
        assertEntryName(l, 19, "ZipUtil");
        assertEntryName(l, 20, "ZipLong");
        assertEntryName(l, 21, "ZipShort");
        assertEntryName(l, 22, "ZipFile");
    }

    @Test
    public void testPhysicalOrder() throws Exception {
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
    public void testDoubleClose() throws Exception {
        readOrderTest();
        zf.close();
        try {
            zf.close();
        } catch (final Exception ex) {
            fail("Caught exception of second close");
        }
    }

    @Test
    public void testReadingOfStoredEntry() throws Exception {
        final File f = File.createTempFile("commons-compress-zipfiletest", ".zip");
        f.deleteOnExit();
        OutputStream o = null;
        InputStream i = null;
        try {
            o = new FileOutputStream(f);
            final ZipArchiveOutputStream zo = new ZipArchiveOutputStream(o);
            ZipArchiveEntry ze = new ZipArchiveEntry("foo");
            ze.setMethod(ZipEntry.STORED);
            ze.setSize(4);
            ze.setCrc(0xb63cfbcdl);
            zo.putArchiveEntry(ze);
            zo.write(new byte[] { 1, 2, 3, 4 });
            zo.closeArchiveEntry();
            zo.close();
            o.close();
            o  = null;

            zf = new ZipFile(f);
            ze = zf.getEntry("foo");
            assertNotNull(ze);
            i = zf.getInputStream(ze);
            final byte[] b = new byte[4];
            assertEquals(4, i.read(b));
            assertEquals(-1, i.read());
        } finally {
            if (o != null) {
                o.close();
            }
            if (i != null) {
                i.close();
            }
            f.delete();
        }
    }

    /**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-176"
     */
    @Test
    public void testWinzipBackSlashWorkaround() throws Exception {
        final File archive = getFile("test-winzip.zip");
        zf = new ZipFile(archive);
        assertNull(zf.getEntry("\u00e4\\\u00fc.txt"));
        assertNotNull(zf.getEntry("\u00e4/\u00fc.txt"));
    }

    /**
     * Test case for
     * <a href="https://issues.apache.org/jira/browse/COMPRESS-208"
     * >COMPRESS-208</a>.
     */
    @Test
    public void testSkipsPK00Prefix() throws Exception {
        final File archive = getFile("COMPRESS-208.zip");
        zf = new ZipFile(archive);
        assertNotNull(zf.getEntry("test1.xml"));
        assertNotNull(zf.getEntry("test2.xml"));
    }

    @Test
    public void testUnixSymlinkSampleFile() throws Exception {
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
        // Also note:  symlinks are always stored as files, even if they link to directories.
        expectedVals.put(entryPrefix + "link5", "../COMPRESS-214_unix_symlinks/././a/b");
        expectedVals.put(entryPrefix + "link6", "../COMPRESS-214_unix_symlinks/././a/b/");

        // I looked into creating a test with hard links, but zip does not appear to
        // support hard links, so nevermind.

        final File archive = getFile("COMPRESS-214_unix_symlinks.zip");

        zf = new ZipFile(archive);
        final Enumeration<ZipArchiveEntry> en = zf.getEntries();
        while (en.hasMoreElements()) {
            final ZipArchiveEntry zae = en.nextElement();
            final String link = zf.getUnixSymlink(zae);
            if (zae.isUnixSymlink()) {
                final String name = zae.getName();
                final String expected = expectedVals.get(name);
                assertEquals(expected, link);
            } else {
                // Should be null if it's not a symlink!
                assertNull(link);
            }
        }
    }

    /**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-227"
     */
    @Test
    public void testDuplicateEntry() throws Exception {
        final File archive = getFile("COMPRESS-227.zip");
        zf = new ZipFile(archive);

        final ZipArchiveEntry ze = zf.getEntry("test1.txt");
        assertNotNull(ze);
        assertNotNull(zf.getInputStream(ze));

        int numberOfEntries = 0;
        for (final ZipArchiveEntry entry : zf.getEntries("test1.txt")) {
            numberOfEntries++;
            assertNotNull(zf.getInputStream(entry));
        }
        assertEquals(2, numberOfEntries);
    }

    /**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-228"
     */
    @Test
    public void testExcessDataInZip64ExtraField() throws Exception {
        final File archive = getFile("COMPRESS-228.zip");
        zf = new ZipFile(archive);
        // actually, if we get here, the test already has passed

        final ZipArchiveEntry ze = zf.getEntry("src/main/java/org/apache/commons/compress/archivers/zip/ZipFile.java");
        assertEquals(26101, ze.getSize());
    }

    @Test
    public void testUnshrinking() throws Exception {
        zf = new ZipFile(getFile("SHRUNK.ZIP"));
        ZipArchiveEntry test = zf.getEntry("TEST1.XML");
        FileInputStream original = new FileInputStream(getFile("test1.xml"));
        try {
            assertArrayEquals(IOUtils.toByteArray(original),
                              IOUtils.toByteArray(zf.getInputStream(test)));
        } finally {
            original.close();
        }
        test = zf.getEntry("TEST2.XML");
        original = new FileInputStream(getFile("test2.xml"));
        try {
            assertArrayEquals(IOUtils.toByteArray(original),
                              IOUtils.toByteArray(zf.getInputStream(test)));
        } finally {
            original.close();
        }
    }

    /**
     * Test case for
     * <a href="https://issues.apache.org/jira/browse/COMPRESS-264"
     * >COMPRESS-264</a>.
     */
    @Test
    public void testReadingOfFirstStoredEntry() throws Exception {
        final File archive = getFile("COMPRESS-264.zip");
        zf = new ZipFile(archive);
        final ZipArchiveEntry ze = zf.getEntry("test.txt");
        assertEquals(5, ze.getSize());
        assertArrayEquals(new byte[] {'d', 'a', 't', 'a', '\n'},
                          IOUtils.toByteArray(zf.getInputStream(ze)));
    }

    @Test
    public void testUnzipBZip2CompressedEntry() throws Exception {
        final File archive = getFile("bzip2-zip.zip");
        zf = new ZipFile(archive);
        final ZipArchiveEntry ze = zf.getEntry("lots-of-as");
        assertEquals(42, ze.getSize());
        final byte[] expected = new byte[42];
        Arrays.fill(expected , (byte)'a');
        assertArrayEquals(expected, IOUtils.toByteArray(zf.getInputStream(ze)));
    }

    @Test
    public void testConcurrentReadSeekable() throws Exception {
        // mixed.zip contains both inflated and stored files
        byte[] data = null;
        try (FileInputStream fis = new FileInputStream(getFile("mixed.zip"))) {
            data = IOUtils.toByteArray(fis);
        }
        zf = new ZipFile(new SeekableInMemoryByteChannel(data), ZipEncodingHelper.UTF8);

        final Map<String, byte[]> content = new HashMap<String, byte[]>();
        for (ZipArchiveEntry entry: Collections.list(zf.getEntries())) {
            content.put(entry.getName(), IOUtils.toByteArray(zf.getInputStream(entry)));
        }

        final AtomicInteger passedCount = new AtomicInteger();
        Runnable run = new Runnable() {
            @Override
            public void run() {
                for (ZipArchiveEntry entry: Collections.list(zf.getEntries())) {
                    assertAllReadMethods(content.get(entry.getName()), zf, entry);
                }
                passedCount.incrementAndGet();
            }
        };
        Thread t0 = new Thread(run);
        Thread t1 = new Thread(run);
        t0.start();
        t1.start();
        t0.join();
        t1.join();
        assertEquals(2, passedCount.get());
    }

    @Test
    public void testConcurrentReadFile() throws Exception {
        // mixed.zip contains both inflated and stored files
        final File archive = getFile("mixed.zip");
        zf = new ZipFile(archive);

        final Map<String, byte[]> content = new HashMap<String, byte[]>();
        for (ZipArchiveEntry entry: Collections.list(zf.getEntries())) {
            content.put(entry.getName(), IOUtils.toByteArray(zf.getInputStream(entry)));
        }

        final AtomicInteger passedCount = new AtomicInteger();
        Runnable run = new Runnable() {
            @Override
            public void run() {
                for (ZipArchiveEntry entry: Collections.list(zf.getEntries())) {
                    assertAllReadMethods(content.get(entry.getName()), zf, entry);
                }
                passedCount.incrementAndGet();
            }
        };
        Thread t0 = new Thread(run);
        Thread t1 = new Thread(run);
        t0.start();
        t1.start();
        t0.join();
        t1.join();
        assertEquals(2, passedCount.get());
    }

    /**
     * Test correct population of header and data offsets.
     */
    @Test
    public void testOffsets() throws Exception {
        // mixed.zip contains both inflated and stored files
        final File archive = getFile("mixed.zip");
        try (ZipFile zf = new ZipFile(archive)) {
            ZipArchiveEntry inflatedEntry = zf.getEntry("inflated.txt");
            Assert.assertEquals(0x0000, inflatedEntry.getLocalHeaderOffset());
            Assert.assertEquals(0x0046, inflatedEntry.getDataOffset());
            Assert.assertTrue(inflatedEntry.isStreamContiguous());
            ZipArchiveEntry storedEntry = zf.getEntry("stored.txt");
            Assert.assertEquals(0x5892, storedEntry.getLocalHeaderOffset());
            Assert.assertEquals(0x58d6, storedEntry.getDataOffset());
            Assert.assertTrue(inflatedEntry.isStreamContiguous());
        }
    }

    /**
     * Test correct population of header and data offsets when they are written after stream.
     */
    @Test
    public void testDelayedOffsetsAndSizes() throws Exception {
        ByteArrayOutputStream zipContent = new ByteArrayOutputStream();
        try (ZipArchiveOutputStream zipOutput = new ZipArchiveOutputStream(zipContent)) {
            ZipArchiveEntry inflatedEntry = new ZipArchiveEntry("inflated.txt");
            inflatedEntry.setMethod(ZipEntry.DEFLATED);
            zipOutput.putArchiveEntry(inflatedEntry);
            zipOutput.write("Hello Deflated\n".getBytes());
            zipOutput.closeArchiveEntry();

            byte[] storedContent = "Hello Stored\n".getBytes();
            ZipArchiveEntry storedEntry = new ZipArchiveEntry("stored.txt");
            storedEntry.setMethod(ZipEntry.STORED);
            storedEntry.setSize(storedContent.length);
            storedEntry.setCrc(calculateCrc32(storedContent));
            zipOutput.putArchiveEntry(storedEntry);
            zipOutput.write("Hello Stored\n".getBytes());
            zipOutput.closeArchiveEntry();

        }

        try (ZipFile zf = new ZipFile(new SeekableInMemoryByteChannel(zipContent.toByteArray()))) {
            ZipArchiveEntry inflatedEntry = zf.getEntry("inflated.txt");
            Assert.assertNotEquals(-1L, inflatedEntry.getLocalHeaderOffset());
            Assert.assertNotEquals(-1L, inflatedEntry.getDataOffset());
            Assert.assertTrue(inflatedEntry.isStreamContiguous());
            Assert.assertNotEquals(-1L, inflatedEntry.getCompressedSize());
            Assert.assertNotEquals(-1L, inflatedEntry.getSize());
            ZipArchiveEntry storedEntry = zf.getEntry("stored.txt");
            Assert.assertNotEquals(-1L, storedEntry.getLocalHeaderOffset());
            Assert.assertNotEquals(-1L, storedEntry.getDataOffset());
            Assert.assertTrue(inflatedEntry.isStreamContiguous());
            Assert.assertNotEquals(-1L, storedEntry.getCompressedSize());
            Assert.assertNotEquals(-1L, storedEntry.getSize());
        }
    }

    /**
     * Test entries alignment.
     */
    @Test
    public void testEntryAlignment() throws Exception {
        SeekableInMemoryByteChannel zipContent = new SeekableInMemoryByteChannel();
        try (ZipArchiveOutputStream zipOutput = new ZipArchiveOutputStream(zipContent)) {
            ZipArchiveEntry inflatedEntry = new ZipArchiveEntry("inflated.txt");
            inflatedEntry.setMethod(ZipEntry.DEFLATED);
            inflatedEntry.setAlignment(1024);
            zipOutput.putArchiveEntry(inflatedEntry);
            zipOutput.write("Hello Deflated\n".getBytes(Charset.forName("UTF-8")));
            zipOutput.closeArchiveEntry();

            ZipArchiveEntry storedEntry = new ZipArchiveEntry("stored.txt");
            storedEntry.setMethod(ZipEntry.STORED);
            storedEntry.setAlignment(1024);
            zipOutput.putArchiveEntry(storedEntry);
            zipOutput.write("Hello Stored\n".getBytes(Charset.forName("UTF-8")));
            zipOutput.closeArchiveEntry();

            ZipArchiveEntry storedEntry2 = new ZipArchiveEntry("stored2.txt");
            storedEntry2.setMethod(ZipEntry.STORED);
            storedEntry2.setAlignment(1024);
            storedEntry2.addExtraField(new ResourceAlignmentExtraField(1));
            zipOutput.putArchiveEntry(storedEntry2);
            zipOutput.write("Hello overload-alignment Stored\n".getBytes(Charset.forName("UTF-8")));
            zipOutput.closeArchiveEntry();

            ZipArchiveEntry storedEntry3 = new ZipArchiveEntry("stored3.txt");
            storedEntry3.setMethod(ZipEntry.STORED);
            storedEntry3.addExtraField(new ResourceAlignmentExtraField(1024));
            zipOutput.putArchiveEntry(storedEntry3);
            zipOutput.write("Hello copy-alignment Stored\n".getBytes(Charset.forName("UTF-8")));
            zipOutput.closeArchiveEntry();

        }

        try (ZipFile zf = new ZipFile(new SeekableInMemoryByteChannel(
                        Arrays.copyOfRange(zipContent.array(), 0, (int)zipContent.size())
        ))) {
            ZipArchiveEntry inflatedEntry = zf.getEntry("inflated.txt");
            ResourceAlignmentExtraField inflatedAlignmentEx =
                            (ResourceAlignmentExtraField)inflatedEntry.getExtraField(ResourceAlignmentExtraField.ID);
            assertNotEquals(-1L, inflatedEntry.getCompressedSize());
            assertNotEquals(-1L, inflatedEntry.getSize());
            assertEquals(0L, inflatedEntry.getDataOffset()%1024);
            assertNotNull(inflatedAlignmentEx);
            assertEquals(1024, inflatedAlignmentEx.getAlignment());
            assertFalse(inflatedAlignmentEx.allowMethodChange());
            try (InputStream stream = zf.getInputStream(inflatedEntry)) {
                Assert.assertEquals("Hello Deflated\n",
                                new String(IOUtils.toByteArray(stream), Charset.forName("UTF-8")));
            }
            ZipArchiveEntry storedEntry = zf.getEntry("stored.txt");
            ResourceAlignmentExtraField storedAlignmentEx =
                            (ResourceAlignmentExtraField)storedEntry.getExtraField(ResourceAlignmentExtraField.ID);
            assertNotEquals(-1L, storedEntry.getCompressedSize());
            assertNotEquals(-1L, storedEntry.getSize());
            assertEquals(0L, storedEntry.getDataOffset()%1024);
            assertNotNull(storedAlignmentEx);
            assertEquals(1024, storedAlignmentEx.getAlignment());
            assertFalse(storedAlignmentEx.allowMethodChange());
            try (InputStream stream = zf.getInputStream(storedEntry)) {
                Assert.assertEquals("Hello Stored\n",
                                new String(IOUtils.toByteArray(stream), Charset.forName("UTF-8")));
            }

            ZipArchiveEntry storedEntry2 = zf.getEntry("stored2.txt");
            ResourceAlignmentExtraField stored2AlignmentEx =
                            (ResourceAlignmentExtraField)storedEntry2.getExtraField(ResourceAlignmentExtraField.ID);
            assertNotEquals(-1L, storedEntry2.getCompressedSize());
            assertNotEquals(-1L, storedEntry2.getSize());
            assertEquals(0L, storedEntry2.getDataOffset()%1024);
            assertNotNull(stored2AlignmentEx);
            assertEquals(1024, stored2AlignmentEx.getAlignment());
            assertFalse(stored2AlignmentEx.allowMethodChange());
            try (InputStream stream = zf.getInputStream(storedEntry2)) {
                Assert.assertEquals("Hello overload-alignment Stored\n",
                                new String(IOUtils.toByteArray(stream), Charset.forName("UTF-8")));
            }

            ZipArchiveEntry storedEntry3 = zf.getEntry("stored3.txt");
            ResourceAlignmentExtraField stored3AlignmentEx =
                            (ResourceAlignmentExtraField)storedEntry3.getExtraField(ResourceAlignmentExtraField.ID);
            assertNotEquals(-1L, storedEntry3.getCompressedSize());
            assertNotEquals(-1L, storedEntry3.getSize());
            assertEquals(0L, storedEntry3.getDataOffset()%1024);
            assertNotNull(stored3AlignmentEx);
            assertEquals(1024, stored3AlignmentEx.getAlignment());
            assertFalse(stored3AlignmentEx.allowMethodChange());
            try (InputStream stream = zf.getInputStream(storedEntry3)) {
                Assert.assertEquals("Hello copy-alignment Stored\n",
                                new String(IOUtils.toByteArray(stream), Charset.forName("UTF-8")));
            }
        }
    }

    /**
     * Test too big alignment, resulting into exceeding extra field limit.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testEntryAlignmentExceed() throws Exception {
        SeekableInMemoryByteChannel zipContent = new SeekableInMemoryByteChannel();
        try (ZipArchiveOutputStream zipOutput = new ZipArchiveOutputStream(zipContent)) {
            ZipArchiveEntry inflatedEntry = new ZipArchiveEntry("inflated.txt");
            inflatedEntry.setMethod(ZipEntry.STORED);
            inflatedEntry.setAlignment(0x20000);
        }
    }

    /**
     * Test non power of 2 alignment.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidAlignment() throws Exception {
        ZipArchiveEntry entry = new ZipArchiveEntry("dummy");
        entry.setAlignment(3);
    }

    @Test
    public void nameSourceDefaultsToName() throws Exception {
        nameSource("bla.zip", "test1.xml", ZipArchiveEntry.NameSource.NAME);
    }

    @Test
    public void nameSourceIsSetToUnicodeExtraField() throws Exception {
        nameSource("utf8-winzip-test.zip", "\u20AC_for_Dollar.txt",
                   ZipArchiveEntry.NameSource.UNICODE_EXTRA_FIELD);
    }

    @Test
    public void nameSourceIsSetToEFS() throws Exception {
        nameSource("utf8-7zip-test.zip", "\u20AC_for_Dollar.txt",
                   ZipArchiveEntry.NameSource.NAME_WITH_EFS_FLAG);
    }

    /**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-380"
     */
    @Test
    public void readDeflate64CompressedStream() throws Exception {
        final File input = getFile("COMPRESS-380/COMPRESS-380-input");
        final File archive = getFile("COMPRESS-380/COMPRESS-380.zip");
        try (FileInputStream in = new FileInputStream(input);
             ZipFile zf = new ZipFile(archive)) {
            byte[] orig = IOUtils.toByteArray(in);
            ZipArchiveEntry e = zf.getEntry("input2");
            try (InputStream s = zf.getInputStream(e)) {
                byte[] fromZip = IOUtils.toByteArray(s);
                assertArrayEquals(orig, fromZip);
            }
        }
    }

    @Test
    public void singleByteReadConsistentlyReturnsMinusOneAtEofUsingDeflate() throws Exception {
        singleByteReadConsistentlyReturnsMinusOneAtEof(getFile("bla.zip"));
    }

    @Test
    public void singleByteReadConsistentlyReturnsMinusOneAtEofUsingStore() throws Exception {
        singleByteReadConsistentlyReturnsMinusOneAtEof(getFile("COMPRESS-264.zip"));
    }

    @Test
    public void singleByteReadConsistentlyReturnsMinusOneAtEofUsingUnshrink() throws Exception {
        singleByteReadConsistentlyReturnsMinusOneAtEof(getFile("SHRUNK.ZIP"));
    }

    @Test
    public void singleByteReadConsistentlyReturnsMinusOneAtEofUsingExplode() throws Exception {
        singleByteReadConsistentlyReturnsMinusOneAtEof(getFile("imploding-8Kdict-3trees.zip"));
    }

    @Test
    public void singleByteReadConsistentlyReturnsMinusOneAtEofUsingDeflate64() throws Exception {
        singleByteReadConsistentlyReturnsMinusOneAtEof(getFile("COMPRESS-380/COMPRESS-380.zip"));
    }

    @Test
    public void singleByteReadConsistentlyReturnsMinusOneAtEofUsingBzip2() throws Exception {
        singleByteReadConsistentlyReturnsMinusOneAtEof(getFile("bzip2-zip.zip"));
    }

    private void singleByteReadConsistentlyReturnsMinusOneAtEof(File file) throws Exception {
        try (ZipFile archive = new ZipFile(file)) {
            ZipArchiveEntry e = archive.getEntries().nextElement();
            try (InputStream is = archive.getInputStream(e)) {
                IOUtils.toByteArray(is);
                assertEquals(-1, is.read());
                assertEquals(-1, is.read());
            }
        }
    }

    @Test
    public void multiByteReadConsistentlyReturnsMinusOneAtEofUsingDeflate() throws Exception {
        multiByteReadConsistentlyReturnsMinusOneAtEof(getFile("bla.zip"));
    }

    @Test
    public void multiByteReadConsistentlyReturnsMinusOneAtEofUsingStore() throws Exception {
        multiByteReadConsistentlyReturnsMinusOneAtEof(getFile("COMPRESS-264.zip"));
    }

    @Test
    public void multiByteReadConsistentlyReturnsMinusOneAtEofUsingUnshrink() throws Exception {
        multiByteReadConsistentlyReturnsMinusOneAtEof(getFile("SHRUNK.ZIP"));
    }

    @Test
    public void multiByteReadConsistentlyReturnsMinusOneAtEofUsingExplode() throws Exception {
        multiByteReadConsistentlyReturnsMinusOneAtEof(getFile("imploding-8Kdict-3trees.zip"));
    }

    @Test
    public void multiByteReadConsistentlyReturnsMinusOneAtEofUsingDeflate64() throws Exception {
        multiByteReadConsistentlyReturnsMinusOneAtEof(getFile("COMPRESS-380/COMPRESS-380.zip"));
    }

    @Test
    public void multiByteReadConsistentlyReturnsMinusOneAtEofUsingBzip2() throws Exception {
        multiByteReadConsistentlyReturnsMinusOneAtEof(getFile("bzip2-zip.zip"));
    }

    private void multiByteReadConsistentlyReturnsMinusOneAtEof(File file) throws Exception {
        byte[] buf = new byte[2];
        try (ZipFile archive = new ZipFile(file)) {
            ZipArchiveEntry e = archive.getEntries().nextElement();
            try (InputStream is = archive.getInputStream(e)) {
                IOUtils.toByteArray(is);
                assertEquals(-1, is.read(buf));
                assertEquals(-1, is.read(buf));
            }
        }
    }

    private void assertAllReadMethods(byte[] expected, ZipFile zipFile, ZipArchiveEntry entry) {
        // simple IOUtil read
        try (InputStream stream = zf.getInputStream(entry)) {
            byte[] full = IOUtils.toByteArray(stream);
            assertArrayEquals(expected, full);
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        // big buffer at the beginning and then chunks by IOUtils read
        try (InputStream stream = zf.getInputStream(entry)) {
            byte[] full;
            byte[] bytes = new byte[0x40000];
            int read = stream.read(bytes);
            if (read < 0) {
                full = new byte[0];
            }
            else {
                full = readStreamRest(bytes, read, stream);
            }
            assertArrayEquals(expected, full);
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        // small chunk / single byte and big buffer then
        try (InputStream stream = zf.getInputStream(entry)) {
            byte[] full;
            int single = stream.read();
            if (single < 0) {
                full = new byte[0];
            }
            else {
                byte[] big = new byte[0x40000];
                big[0] = (byte)single;
                int read = stream.read(big, 1, big.length-1);
                if (read < 0) {
                    full = new byte[]{ (byte)single };
                }
                else {
                    full = readStreamRest(big, read+1, stream);
                }
            }
            assertArrayEquals(expected, full);
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Utility to append the rest of the stream to already read data.
     */
    private byte[] readStreamRest(byte[] beginning, int length, InputStream stream) throws IOException {
        byte[] rest = IOUtils.toByteArray(stream);
        byte[] full = new byte[length+rest.length];
        System.arraycopy(beginning, 0, full, 0, length);
        System.arraycopy(rest, 0, full, length, rest.length);
        return full;
    }

    private long calculateCrc32(byte[] content) {
        CRC32 crc = new CRC32();
        crc.update(content);
        return crc.getValue();
    }

    /*
     * ordertest.zip has been handcrafted.
     *
     * It contains enough files so any random coincidence of
     * entries.keySet() and central directory order would be unlikely
     * - in fact testCDOrder fails in svn revision 920284.
     *
     * The central directory has ZipFile and ZipUtil swapped so
     * central directory order is different from entry data order.
     */
    private void readOrderTest() throws Exception {
        final File archive = getFile("ordertest.zip");
        zf = new ZipFile(archive);
    }

    private static void assertEntryName(final ArrayList<ZipArchiveEntry> entries,
                                        final int index,
                                        final String expectedName) {
        final ZipArchiveEntry ze = entries.get(index);
        assertEquals("src/main/java/org/apache/commons/compress/archivers/zip/"
                     + expectedName + ".java",
                     ze.getName());
    }

    private static void nameSource(String archive, String entry, ZipArchiveEntry.NameSource expected) throws Exception {
        try (ZipFile zf = new ZipFile(getFile(archive))) {
            ZipArchiveEntry ze = zf.getEntry(entry);
            assertEquals(entry, ze.getName());
            assertEquals(expected, ze.getNameSource());
        }
    }
}
