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

package org.apache.commons.compress.archivers.tar;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.compress.AbstractTestCase.getFile;
import static org.apache.commons.compress.AbstractTestCase.getPath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.archivers.zip.ZipEncodingHelper;
import org.apache.commons.compress.utils.CharsetNames;
import org.junit.jupiter.api.Test;

public class TarArchiveEntryTest implements TarConstants {

    private static final String OS =
        System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
    private static final String ROOT =
        OS.startsWith("windows") || OS.startsWith("netware") ? "C:\\" : "/";

    /**
     * JIRA issue SANDBOX-284
     *
     * @see "https://issues.apache.org/jira/browse/SANDBOX-284"
     */
    @Test
    public void testFileSystemRoot() {
        final TarArchiveEntry t = new TarArchiveEntry(new File(ROOT));
        assertEquals("/", t.getName());
    }

    @Test
    public void testTarFileWithFSRoot() throws IOException {
        final File f = File.createTempFile("taetest", ".tar");
        f.deleteOnExit();
        TarArchiveOutputStream tout = null;
        TarArchiveInputStream tin = null;
        try {
            tout = new TarArchiveOutputStream(Files.newOutputStream(f.toPath()));
            TarArchiveEntry t = new TarArchiveEntry(new File(ROOT));
            tout.putArchiveEntry(t);
            tout.closeArchiveEntry();
            t = new TarArchiveEntry(new File(new File(ROOT), "foo.txt"));
            t.setSize(6);
            tout.putArchiveEntry(t);
            tout.write(new byte[] {'h', 'e', 'l', 'l', 'o', ' '});
            tout.closeArchiveEntry();
            t = new TarArchiveEntry(new File(new File(ROOT), "bar.txt")
                                    .getAbsolutePath());
            t.setSize(5);
            tout.putArchiveEntry(t);
            tout.write(new byte[] {'w', 'o', 'r', 'l', 'd'});
            tout.closeArchiveEntry();
            t = new TarArchiveEntry("dummy");
            t.setName(new File(new File(ROOT), "baz.txt").getAbsolutePath());
            t.setSize(1);
            tout.putArchiveEntry(t);
            tout.write(new byte[] {'!'});
            tout.closeArchiveEntry();
            tout.close();
            tout = null;

            tin = new TarArchiveInputStream(Files.newInputStream(f.toPath()));
            //tin.setDebug(true);
            t = tin.getNextTarEntry();
            assertNotNull(t);
            assertEquals("/", t.getName());
            assertTrue(t.isCheckSumOK());
            t = tin.getNextTarEntry();
            assertNotNull(t);
            assertEquals("foo.txt", t.getName());
            assertTrue(t.isCheckSumOK());
            t = tin.getNextTarEntry();
            assertNotNull(t);
            assertEquals("bar.txt", t.getName());
            assertTrue(t.isCheckSumOK());
            t = tin.getNextTarEntry();
            assertNotNull(t);
            assertEquals("baz.txt", t.getName());
            assertTrue(t.isCheckSumOK());
        } finally {
            if (tin != null) {
                tin.close();
            }
            if (tout != null) {
                tout.close();
            }
            AbstractTestCase.tryHardToDelete(f);
        }
    }

    @Test
    public void testMaxFileSize(){
        final TarArchiveEntry t = new TarArchiveEntry("");
        t.setSize(0);
        t.setSize(1);
        assertThrows(IllegalArgumentException.class, () -> t.setSize(-1));
        t.setSize(077777777777L);
        t.setSize(0100000000000L);
    }

    @Test
    public void testExtraPaxHeaders() throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final TarArchiveOutputStream tos = new TarArchiveOutputStream(bos);

        TarArchiveEntry entry = new TarArchiveEntry("./weasels");
        entry.addPaxHeader("APACHE.mustelida", "true");
        entry.addPaxHeader("SCHILY.xattr.user.org.apache.weasels", "maximum weasels");
        entry.addPaxHeader("size", "1");
        assertEquals("extra header count", 2, entry.getExtraPaxHeaders().size());
        assertEquals("APACHE.mustelida", "true", entry.getExtraPaxHeader("APACHE.mustelida"));
        assertEquals("SCHILY.xattr.user.org.apache.weasels", "maximum weasels", entry.getExtraPaxHeader("SCHILY.xattr.user.org.apache.weasels"));
        assertEquals("size", entry.getSize(), 1);

        tos.putArchiveEntry(entry);
        tos.write('W');
        tos.closeArchiveEntry();
        tos.close();
        assertNotEquals("should have extra headers before clear", 0, entry.getExtraPaxHeaders().size());
        entry.clearExtraPaxHeaders();
        assertEquals("extra headers should be empty after clear", 0, entry.getExtraPaxHeaders().size());
        final TarArchiveInputStream tis = new TarArchiveInputStream(new ByteArrayInputStream(bos.toByteArray()));
        entry = tis.getNextTarEntry();
        assertNotNull("couldn't get entry", entry);

        assertEquals("extra header count", 2, entry.getExtraPaxHeaders().size());
        assertEquals("APACHE.mustelida", "true", entry.getExtraPaxHeader("APACHE.mustelida"));
        assertEquals("user.org.apache.weasels", "maximum weasels", entry.getExtraPaxHeader("SCHILY.xattr.user.org.apache.weasels"));

        assertEquals('W', tis.read());
        assertTrue("should be at end of entry", tis.read() < 0);

        assertNull("should be at end of file", tis.getNextTarEntry());
        tis.close();
    }

    @Test
    public void testLinkFlagConstructor() {
        final TarArchiveEntry t = new TarArchiveEntry("/foo", LF_GNUTYPE_LONGNAME);
        assertGnuMagic(t);
        assertEquals("foo", t.getName());
    }

    @Test
    public void testLinkFlagConstructorWithFileFlag() {
        final TarArchiveEntry t = new TarArchiveEntry("/foo", LF_NORMAL);
        assertPosixMagic(t);
        assertEquals("foo", t.getName());
    }

    @Test
    public void testLinkFlagConstructorWithPreserve() {
        final TarArchiveEntry t = new TarArchiveEntry("/foo", LF_GNUTYPE_LONGNAME,
                                                true);
        assertGnuMagic(t);
        assertEquals("/foo", t.getName());
    }

    @Test
    public void preservesDriveSpecOnWindowsAndNetwareIfAskedTo() {
        assumeTrue("C:\\".equals(ROOT));
        TarArchiveEntry t = new TarArchiveEntry(ROOT + "foo.txt", true);
        assertEquals("C:/foo.txt", t.getName());
        t = new TarArchiveEntry(ROOT + "foo.txt", LF_GNUTYPE_LONGNAME, true);
        assertEquals("C:/foo.txt", t.getName());
    }

    @Test
    public void getFileFromNonFileEntry() {
        final TarArchiveEntry entry = new TarArchiveEntry("test.txt");
        assertNull(entry.getFile());
        assertNull(entry.getPath());
    }

    @Test
    public void testLinuxFileInformationFromFile() throws IOException {
        assumeTrue("Information is only available on linux", OS.equals("linux"));
        final TarArchiveEntry entry = new TarArchiveEntry(getFile("test1.xml"));
        assertNotEquals(0, entry.getLongUserId());
        assertNotEquals(0, entry.getLongGroupId());
        assertNotEquals("", entry.getUserName());
    }

    @Test
    public void testLinuxFileInformationFromPath() throws IOException {
        assumeTrue("Information is only available on linux", OS.equals("linux"));
        final TarArchiveEntry entry = new TarArchiveEntry(getPath("test1.xml"));
        assertNotEquals(0, entry.getLongUserId());
        assertNotEquals(0, entry.getLongGroupId());
        assertNotEquals("", entry.getUserName());
    }

    @Test
    public void testWindowsFileInformationFromFile() throws IOException {
        assumeTrue("Information should only be checked on Windows", OS.startsWith("windows"));
        final TarArchiveEntry entry = new TarArchiveEntry(getFile("test1.xml"));
        assertNotEquals("", entry.getUserName());
    }

    @Test
    public void testWindowsFileInformationFromPath() throws IOException {
        assumeTrue("Information should only be checked on Windows", OS.startsWith("windows"));
        final TarArchiveEntry entry = new TarArchiveEntry(getPath("test1.xml"));
        assertNotEquals("", entry.getUserName());
    }

    @Test
    public void negativeOffsetInConstructorNotAllowed() throws IOException {
        byte[] entryContent = ("test1.xml\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
                "\u0000" +
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u00000000644\u00000000765\u00000000765" +
                "\u000000000001142\u000010716545626\u0000012260\u0000 0\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000ustar  " +
                "\u0000tcurdt\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
                "\u0000\u0000\u0000tcurdt\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
                "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000").getBytes(UTF_8);
        new TarArchiveEntry(entryContent, ZipEncodingHelper.getZipEncoding(CharsetNames.ISO_8859_1), false, -1);
    }

    @Test
    public void negativeOffsetInSetterNotAllowed() {
        new TarArchiveEntry("test").setDataOffset(-1);
    }

    @Test
    public void getOrderedSparseHeadersSortsAndFiltersSparseStructs() throws Exception {
        final TarArchiveEntry te = new TarArchiveEntry("test");
        // hacky way to set realSize
        te.fillStarSparseData(Collections.singletonMap("SCHILY.realsize", "201"));
        te.setSparseHeaders(Arrays.asList(new TarArchiveStructSparse(10, 2), new TarArchiveStructSparse(20, 0),
            new TarArchiveStructSparse(15, 1), new TarArchiveStructSparse(0, 0)));
        final List<TarArchiveStructSparse> strs = te.getOrderedSparseHeaders();
        assertEquals(3, strs.size());
        assertEquals(10, strs.get(0).getOffset());
        assertEquals(15, strs.get(1).getOffset());
        assertEquals(20, strs.get(2).getOffset());
    }

    @Test
    public void getOrderedSparseHeadersRejectsOverlappingStructs() throws Exception {
        final TarArchiveEntry te = new TarArchiveEntry("test");
        te.fillStarSparseData(Collections.singletonMap("SCHILY.realsize", "201"));
        te.setSparseHeaders(Arrays.asList(new TarArchiveStructSparse(10, 5), new TarArchiveStructSparse(12, 1)));
        te.getOrderedSparseHeaders();
    }

    @Test
    public void getOrderedSparseHeadersRejectsStructsWithReallyBigNumbers() throws Exception {
        final TarArchiveEntry te = new TarArchiveEntry("test");
        te.fillStarSparseData(Collections.singletonMap("SCHILY.realsize", String.valueOf(Long.MAX_VALUE)));
        te.setSparseHeaders(Arrays.asList(new TarArchiveStructSparse(Long.MAX_VALUE, 2)));
        te.getOrderedSparseHeaders();
    }

    @Test
    public void getOrderedSparseHeadersRejectsStructsPointingBeyondOutputEntry() throws Exception {
        final TarArchiveEntry te = new TarArchiveEntry("test");
        te.setSparseHeaders(Arrays.asList(new TarArchiveStructSparse(200, 2)));
        te.fillStarSparseData(Collections.singletonMap("SCHILY.realsize", "201"));
        te.getOrderedSparseHeaders();
    }

    @Test
    public void shouldParseTimePaxHeadersAndNotCountAsExtraPaxHeaders() {
        final TarArchiveEntry entry = createEntryForTimeTests();
        assertEquals("extra header count", 0, entry.getExtraPaxHeaders().size());
        assertNull("size", entry.getExtraPaxHeader("size"));
        assertNull("mtime", entry.getExtraPaxHeader("mtime"));
        assertNull("atime", entry.getExtraPaxHeader("atime"));
        assertNull("ctime", entry.getExtraPaxHeader("ctime"));
        assertNull("birthtime", entry.getExtraPaxHeader("LIBARCHIVE.creationtime"));
        assertEquals("size", entry.getSize(), 1);
        assertEquals("mtime", toFileTime("2022-03-14T01:25:03.599853900Z"), entry.getLastModifiedTime());
        assertEquals("atime", toFileTime("2022-03-14T01:31:00.706927200Z"), entry.getLastAccessTime());
        assertEquals("ctime", toFileTime("2022-03-14T01:28:59.700505300Z"), entry.getStatusChangeTime());
        assertEquals("birthtime", toFileTime("2022-03-14T01:29:00.723509000Z"), entry.getCreationTime());
    }

    @Test
    public void shouldNotWriteTimePaxHeadersByDefault() throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (final TarArchiveOutputStream tos = new TarArchiveOutputStream(bos)) {
            final TarArchiveEntry entry = createEntryForTimeTests();
            tos.putArchiveEntry(entry);
            tos.write('W');
            tos.closeArchiveEntry();
        }
        try (final TarArchiveInputStream tis = new TarArchiveInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            final TarArchiveEntry entry = tis.getNextTarEntry();
            assertNotNull("couldn't get entry", entry);

            assertEquals("extra header count", 0, entry.getExtraPaxHeaders().size());
            assertNull("mtime", entry.getExtraPaxHeader("mtime"));
            assertNull("atime", entry.getExtraPaxHeader("atime"));
            assertNull("ctime", entry.getExtraPaxHeader("ctime"));
            assertNull("birthtime", entry.getExtraPaxHeader("LIBARCHIVE.creationtime"));
            assertEquals("mtime", toFileTime("2022-03-14T01:25:03Z"), entry.getLastModifiedTime());
            assertNull("atime", entry.getLastAccessTime());
            assertNull("ctime", entry.getStatusChangeTime());
            assertNull("birthtime", entry.getCreationTime());

            assertEquals('W', tis.read());
            assertTrue("should be at end of entry", tis.read() < 0);

            assertNull("should be at end of file", tis.getNextTarEntry());
        }
    }

    @Test
    public void shouldWriteTimesForStarMode() throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (final TarArchiveOutputStream tos = new TarArchiveOutputStream(bos)) {
            final TarArchiveEntry entry = createEntryForTimeTests();
            tos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR);
            tos.putArchiveEntry(entry);
            tos.write('W');
            tos.closeArchiveEntry();
        }
        try (final TarArchiveInputStream tis = new TarArchiveInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            final TarArchiveEntry entry = tis.getNextTarEntry();
            assertNotNull("couldn't get entry", entry);

            assertEquals("extra header count", 0, entry.getExtraPaxHeaders().size());
            assertNull("mtime", entry.getExtraPaxHeader("mtime"));
            assertNull("atime", entry.getExtraPaxHeader("atime"));
            assertNull("ctime", entry.getExtraPaxHeader("ctime"));
            assertNull("birthtime", entry.getExtraPaxHeader("LIBARCHIVE.creationtime"));
            assertEquals("mtime", toFileTime("2022-03-14T01:25:03Z"), entry.getLastModifiedTime());
            assertEquals("atime", toFileTime("2022-03-14T01:31:00Z"), entry.getLastAccessTime());
            assertEquals("ctime", toFileTime("2022-03-14T01:28:59Z"), entry.getStatusChangeTime());
            assertNull("birthtime", entry.getCreationTime());

            assertEquals('W', tis.read());
            assertTrue("should be at end of entry", tis.read() < 0);

            assertNull("should be at end of file", tis.getNextTarEntry());
        }
    }

    @Test
    public void shouldWriteTimesAsPaxHeadersForPosixMode() throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (final TarArchiveOutputStream tos = new TarArchiveOutputStream(bos)) {
            final TarArchiveEntry entry = createEntryForTimeTests();
            tos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
            tos.putArchiveEntry(entry);
            tos.write('W');
            tos.closeArchiveEntry();
        }
        try (final TarArchiveInputStream tis = new TarArchiveInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            final TarArchiveEntry entry = tis.getNextTarEntry();
            assertNotNull("couldn't get entry", entry);

            assertEquals("extra header count", 0, entry.getExtraPaxHeaders().size());
            assertNull("mtime", entry.getExtraPaxHeader("mtime"));
            assertNull("atime", entry.getExtraPaxHeader("atime"));
            assertNull("ctime", entry.getExtraPaxHeader("ctime"));
            assertNull("birthtime", entry.getExtraPaxHeader("LIBARCHIVE.creationtime"));
            assertEquals("mtime", toFileTime("2022-03-14T01:25:03.599853900Z"), entry.getLastModifiedTime());
            assertEquals("atime", toFileTime("2022-03-14T01:31:00.706927200Z"), entry.getLastAccessTime());
            assertEquals("ctime", toFileTime("2022-03-14T01:28:59.700505300Z"), entry.getStatusChangeTime());
            assertEquals("birthtime", toFileTime("2022-03-14T01:29:00.723509000Z"), entry.getCreationTime());

            assertEquals('W', tis.read());
            assertTrue("should be at end of entry", tis.read() < 0);

            assertNull("should be at end of file", tis.getNextTarEntry());
        }
    }

    @Test
    public void shouldWriteTimesAsPaxHeadersForPosixModeAndCreationTimeShouldBeUsedAsCtime() throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (final TarArchiveOutputStream tos = new TarArchiveOutputStream(bos)) {
            final TarArchiveEntry entry = createEntryForTimeTests();
            entry.setStatusChangeTime(null);
            tos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
            tos.putArchiveEntry(entry);
            tos.write('W');
            tos.closeArchiveEntry();
        }
        try (final TarArchiveInputStream tis = new TarArchiveInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            final TarArchiveEntry entry = tis.getNextTarEntry();
            assertNotNull("couldn't get entry", entry);

            assertEquals("extra header count", 0, entry.getExtraPaxHeaders().size());
            assertNull("mtime", entry.getExtraPaxHeader("mtime"));
            assertNull("atime", entry.getExtraPaxHeader("atime"));
            assertNull("ctime", entry.getExtraPaxHeader("ctime"));
            assertNull("birthtime", entry.getExtraPaxHeader("LIBARCHIVE.creationtime"));
            assertEquals("mtime", toFileTime("2022-03-14T01:25:03.599853900Z"), entry.getLastModifiedTime());
            assertEquals("atime", toFileTime("2022-03-14T01:31:00.706927200Z"), entry.getLastAccessTime());
            assertEquals("ctime", toFileTime("2022-03-14T01:29:00.723509000Z"), entry.getStatusChangeTime());
            assertEquals("birthtime", toFileTime("2022-03-14T01:29:00.723509000Z"), entry.getCreationTime());

            assertEquals('W', tis.read());
            assertTrue("should be at end of entry", tis.read() < 0);

            assertNull("should be at end of file", tis.getNextTarEntry());
        }
    }

    private FileTime toFileTime(final String text) {
        return FileTime.from(Instant.parse(text));
    }

    private TarArchiveEntry createEntryForTimeTests() {
        final TarArchiveEntry entry = new TarArchiveEntry("./times.txt");
        entry.addPaxHeader("size", "1");
        entry.addPaxHeader("mtime", "1647221103.5998539");
        entry.addPaxHeader("atime", "1647221460.7069272");
        entry.addPaxHeader("ctime", "1647221339.7005053");
        entry.addPaxHeader("LIBARCHIVE.creationtime", "1647221340.7235090");
        return entry;
    }

    private void assertGnuMagic(final TarArchiveEntry t) {
        assertEquals(MAGIC_GNU + VERSION_GNU_SPACE, readMagic(t));
    }

    private void assertPosixMagic(final TarArchiveEntry t) {
        assertEquals(MAGIC_POSIX + VERSION_POSIX, readMagic(t));
    }

    private String readMagic(final TarArchiveEntry t) {
        final byte[] buf = new byte[512];
        t.writeEntryHeader(buf);
        return new String(buf, MAGIC_OFFSET, MAGICLEN + VERSIONLEN);
    }
}
