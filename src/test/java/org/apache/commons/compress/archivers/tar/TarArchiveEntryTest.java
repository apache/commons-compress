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
 */

package org.apache.commons.compress.archivers.tar;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.compress.AbstractTest.getFile;
import static org.apache.commons.compress.AbstractTest.getPath;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.archivers.zip.ZipEncodingHelper;
import org.apache.commons.lang3.SystemProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;

public class TarArchiveEntryTest implements TarConstants {

    private static final String OS = SystemProperties.getOsName().toLowerCase(Locale.ROOT);
    private static final String ROOT = OS.startsWith("windows") || OS.startsWith("netware") ? "C:\\" : "/";

    private void assertGnuMagic(final TarArchiveEntry t) {
        assertEquals(MAGIC_GNU + VERSION_GNU_SPACE, readMagic(t));
    }

    private void assertPosixMagic(final TarArchiveEntry t) {
        assertEquals(MAGIC_POSIX + VERSION_POSIX, readMagic(t));
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

    private String readMagic(final TarArchiveEntry t) {
        final byte[] buf = new byte[512];
        t.writeEntryHeader(buf);
        return new String(buf, MAGIC_OFFSET, MAGICLEN + VERSIONLEN);
    }

    @Test
    public void testExtraPaxHeaders() throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        TarArchiveEntry entry = new TarArchiveEntry("./weasels");
        try (TarArchiveOutputStream tos = new TarArchiveOutputStream(bos)) {
            entry.addPaxHeader("APACHE.mustelida", "true");
            entry.addPaxHeader("SCHILY.xattr.user.org.apache.weasels", "maximum weasels");
            entry.addPaxHeader("size", "1");
            assertEquals(2, entry.getExtraPaxHeaders().size(), "extra header count");
            assertEquals("true", entry.getExtraPaxHeader("APACHE.mustelida"), "APACHE.mustelida");
            assertEquals("maximum weasels", entry.getExtraPaxHeader("SCHILY.xattr.user.org.apache.weasels"), "SCHILY.xattr.user.org.apache.weasels");
            assertEquals(entry.getSize(), 1, "size");

            tos.putArchiveEntry(entry);
            tos.write('W');
            tos.closeArchiveEntry();
        }
        assertNotEquals(0, entry.getExtraPaxHeaders().size(), "should have extra headers before clear");
        entry.clearExtraPaxHeaders();
        assertEquals(0, entry.getExtraPaxHeaders().size(), "extra headers should be empty after clear");
        try (TarArchiveInputStream tis = new TarArchiveInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            entry = tis.getNextTarEntry();
            assertNotNull(entry, "couldn't get entry");

            assertEquals(2, entry.getExtraPaxHeaders().size(), "extra header count");
            assertEquals("true", entry.getExtraPaxHeader("APACHE.mustelida"), "APACHE.mustelida");
            assertEquals("maximum weasels", entry.getExtraPaxHeader("SCHILY.xattr.user.org.apache.weasels"), "user.org.apache.weasels");

            assertEquals('W', tis.read());
            assertTrue(tis.read() < 0, "should be at end of entry");

            assertNull(tis.getNextTarEntry(), "should be at end of file");
        }
    }

    /**
     * JIRA issue SANDBOX-284
     *
     * @see "https://issues.apache.org/jira/browse/SANDBOX-284"
     */
    @Test
    public void testFileSystemRoot() {
        final TarArchiveEntry t = new TarArchiveEntry(new File(ROOT));
        assertEquals("/", t.getName());
        assertEquals(TarConstants.LF_DIR, t.getLinkFlag());
    }

    @Test
    public void testGetFileFromNonFileEntry() {
        final TarArchiveEntry entry = new TarArchiveEntry("test.txt");
        assertNull(entry.getFile());
        assertNull(entry.getPath());
    }

    @Test
    public void testGetOrderedSparseHeadersRejectsOverlappingStructs() throws Exception {
        final TarArchiveEntry te = new TarArchiveEntry("test");
        te.fillStarSparseData(Collections.singletonMap("SCHILY.realsize", "201"));
        te.setSparseHeaders(Arrays.asList(new TarArchiveStructSparse(10, 5), new TarArchiveStructSparse(12, 1)));
        assertThrows(IOException.class, () -> te.getOrderedSparseHeaders());
    }

    @Test
    public void testGetOrderedSparseHeadersRejectsStructsPointingBeyondOutputEntry() throws Exception {
        final TarArchiveEntry te = new TarArchiveEntry("test");
        te.setSparseHeaders(Arrays.asList(new TarArchiveStructSparse(200, 2)));
        te.fillStarSparseData(Collections.singletonMap("SCHILY.realsize", "201"));
        assertThrows(IOException.class, () -> te.getOrderedSparseHeaders());
    }

    @Test
    public void testGetOrderedSparseHeadersRejectsStructsWithReallyBigNumbers() throws Exception {
        final TarArchiveEntry te = new TarArchiveEntry("test");
        te.fillStarSparseData(Collections.singletonMap("SCHILY.realsize", String.valueOf(Long.MAX_VALUE)));
        te.setSparseHeaders(Arrays.asList(new TarArchiveStructSparse(Long.MAX_VALUE, 2)));
        assertThrows(IOException.class, () -> te.getOrderedSparseHeaders());
    }

    @Test
    public void testGetOrderedSparseHeadersSortsAndFiltersSparseStructs() throws Exception {
        final TarArchiveEntry te = new TarArchiveEntry("test");
        // hacky way to set realSize
        te.fillStarSparseData(Collections.singletonMap("SCHILY.realsize", "201"));
        te.setSparseHeaders(Arrays.asList(new TarArchiveStructSparse(10, 2), new TarArchiveStructSparse(20, 0), new TarArchiveStructSparse(15, 1),
                new TarArchiveStructSparse(0, 0)));
        final List<TarArchiveStructSparse> strs = te.getOrderedSparseHeaders();
        assertEquals(3, strs.size());
        assertEquals(10, strs.get(0).getOffset());
        assertEquals(15, strs.get(1).getOffset());
        assertEquals(20, strs.get(2).getOffset());
    }

    @Test
    public void testLinkFlagConstructor() {
        final TarArchiveEntry t = new TarArchiveEntry("/foo", LF_GNUTYPE_LONGNAME);
        assertGnuMagic(t);
        assertEquals("foo", t.getName());
        assertEquals(TarConstants.LF_GNUTYPE_LONGNAME, t.getLinkFlag());
    }

    @Test
    public void testLinkFlagConstructorWithFileFlag() {
        final TarArchiveEntry t = new TarArchiveEntry("/foo", LF_NORMAL);
        assertPosixMagic(t);
        assertEquals("foo", t.getName());
        assertEquals(TarConstants.LF_NORMAL, t.getLinkFlag());
    }

    @Test
    public void testLinkFlagConstructorWithPreserve() {
        final TarArchiveEntry t = new TarArchiveEntry("/foo", LF_GNUTYPE_LONGNAME, true);
        assertGnuMagic(t);
        assertEquals("/foo", t.getName());
        assertEquals(TarConstants.LF_GNUTYPE_LONGNAME, t.getLinkFlag());
    }

    @Test
    @EnabledOnOs(org.junit.jupiter.api.condition.OS.LINUX)
    public void testLinuxFileInformationFromFile() throws IOException {
        final TarArchiveEntry entry = new TarArchiveEntry(getFile("test1.xml"));
        assertNotEquals(0, entry.getLongUserId());
        assertNotEquals(0, entry.getLongGroupId());
        assertNotEquals("", entry.getUserName());
    }

    @Test
    @EnabledOnOs(org.junit.jupiter.api.condition.OS.LINUX)
    public void testLinuxFileInformationFromPath() throws IOException {
        final TarArchiveEntry entry = new TarArchiveEntry(getPath("test1.xml"));
        assertNotEquals(0, entry.getLongUserId());
        assertNotEquals(0, entry.getLongGroupId());
        assertNotEquals("", entry.getUserName());
    }

    @Test
    public void testMaxFileSize() {
        final TarArchiveEntry t = new TarArchiveEntry("");
        t.setSize(0);
        t.setSize(1);
        assertThrows(IllegalArgumentException.class, () -> t.setSize(-1));
        t.setSize(077777777777L);
        t.setSize(0100000000000L);
    }

    @Test
    public void testNegativeOffsetInConstructorNotAllowed() {
        // @formatter:off
        final byte[] entryContent = (
            "test1.xml\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"
            + "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"
            + "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"
            + "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"
            + "\u00000000644\u00000000765\u00000000765\u000000000001142\u000010716545626\u0000012260\u0000 0\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"
            + "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"
            + "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"
            + "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"
            + "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"
            + "\u0000ustar  \u0000tcurdt\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"
            + "\u0000\u0000\u0000\u0000\u0000\u0000\u0000tcurdt\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"
            + "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"
            + "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"
            + "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"
            + "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"
            + "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"
            + "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"
            + "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"
            + "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"
            + "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000").getBytes(UTF_8);
        // @formatter:on
        assertThrows(IllegalArgumentException.class,
                () -> new TarArchiveEntry(entryContent, ZipEncodingHelper.getZipEncoding(StandardCharsets.ISO_8859_1.name()), false, -1));
    }

    @Test
    public void testNegativeOffsetInSetterNotAllowed() {
        assertThrows(IllegalArgumentException.class, () -> new TarArchiveEntry("test").setDataOffset(-1));
    }

    @Test
    public void testPaxTimeFieldsForInvalidValues() {
        final String[] headerNames = { "LIBARCHIVE.creationtime", "atime", "mtime", "ctime" };
        // @formatter:off
        final String[] testValues = {
                // Generate a number with a very large integer or fractional component
                new Random().nextLong() + "." + String.join("",
                        Collections.nCopies(15000, String.valueOf(Long.MAX_VALUE))),
                // These two examples use the exponent notation
                "9e9999999",
                "9E9999999",
                // These examples are out of range for java.time.Instant
                String.valueOf(Long.MAX_VALUE),
                String.valueOf(Long.MIN_VALUE)
        };
        // @formatter:on

        final TarArchiveEntry entry = new TarArchiveEntry("test.txt");
        for (final String name : headerNames) {
            for (final String value : testValues) {
                final Exception exp = assertThrows(IllegalArgumentException.class, () -> entry.addPaxHeader(name, value));
                assert exp.getCause().getMessage().startsWith("Corrupted PAX header. Time field value is invalid");
            }
        }
    }

    @Test
    public void testPreservesDriveSpecOnWindowsAndNetwareIfAskedTo() {
        assumeTrue("C:\\".equals(ROOT));
        TarArchiveEntry t = new TarArchiveEntry(ROOT + "foo.txt", true);
        assertEquals("C:/foo.txt", t.getName());
        assertEquals(TarConstants.LF_NORMAL, t.getLinkFlag());
        t = new TarArchiveEntry(ROOT + "foo.txt", LF_GNUTYPE_LONGNAME, true);
        assertEquals("C:/foo.txt", t.getName());
        assertEquals(TarConstants.LF_GNUTYPE_LONGNAME, t.getLinkFlag());
    }

    @Test
    public void testShouldNotWriteTimePaxHeadersByDefault() throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tos = new TarArchiveOutputStream(bos)) {
            final TarArchiveEntry entry = createEntryForTimeTests();
            tos.putArchiveEntry(entry);
            tos.write('W');
            tos.closeArchiveEntry();
        }
        try (TarArchiveInputStream tis = new TarArchiveInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            final TarArchiveEntry entry = tis.getNextTarEntry();
            assertNotNull(entry, "couldn't get entry");

            assertEquals(0, entry.getExtraPaxHeaders().size(), "extra header count");
            assertNull(entry.getExtraPaxHeader("mtime"), "mtime");
            assertNull(entry.getExtraPaxHeader("atime"), "atime");
            assertNull(entry.getExtraPaxHeader("ctime"), "ctime");
            assertNull(entry.getExtraPaxHeader("LIBARCHIVE.creationtime"), "birthtime");
            assertEquals(toFileTime("2022-03-14T01:25:03Z"), entry.getLastModifiedTime(), "mtime");
            assertNull(entry.getLastAccessTime(), "atime");
            assertNull(entry.getStatusChangeTime(), "ctime");
            assertNull(entry.getCreationTime(), "birthtime");

            assertEquals('W', tis.read());
            assertTrue(tis.read() < 0, "should be at end of entry");

            assertNull(tis.getNextTarEntry(), "should be at end of file");
        }
    }

    @Test
    public void testShouldParseTimePaxHeadersAndNotCountAsExtraPaxHeaders() {
        final TarArchiveEntry entry = createEntryForTimeTests();
        assertEquals(0, entry.getExtraPaxHeaders().size(), "extra header count");
        assertNull(entry.getExtraPaxHeader("size"), "size");
        assertNull(entry.getExtraPaxHeader("mtime"), "mtime");
        assertNull(entry.getExtraPaxHeader("atime"), "atime");
        assertNull(entry.getExtraPaxHeader("ctime"), "ctime");
        assertNull(entry.getExtraPaxHeader("LIBARCHIVE.creationtime"), "birthtime");
        assertEquals(entry.getSize(), 1, "size");
        assertEquals(toFileTime("2022-03-14T01:25:03.599853900Z"), entry.getLastModifiedTime(), "mtime");
        assertEquals(toFileTime("2022-03-14T01:31:00.706927200Z"), entry.getLastAccessTime(), "atime");
        assertEquals(toFileTime("2022-03-14T01:28:59.700505300Z"), entry.getStatusChangeTime(), "ctime");
        assertEquals(toFileTime("2022-03-14T01:29:00.723509000Z"), entry.getCreationTime(), "birthtime");
    }

    @Test
    public void testShouldWriteTimesAsPaxHeadersForPosixMode() throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tos = new TarArchiveOutputStream(bos)) {
            final TarArchiveEntry entry = createEntryForTimeTests();
            tos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
            tos.putArchiveEntry(entry);
            tos.write('W');
            tos.closeArchiveEntry();
        }
        try (TarArchiveInputStream tis = new TarArchiveInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            final TarArchiveEntry entry = tis.getNextTarEntry();
            assertNotNull(entry, "couldn't get entry");

            assertEquals(0, entry.getExtraPaxHeaders().size(), "extra header count");
            assertNull(entry.getExtraPaxHeader("mtime"), "mtime");
            assertNull(entry.getExtraPaxHeader("atime"), "atime");
            assertNull(entry.getExtraPaxHeader("ctime"), "ctime");
            assertNull(entry.getExtraPaxHeader("LIBARCHIVE.creationtime"), "birthtime");
            assertEquals(toFileTime("2022-03-14T01:25:03.599853900Z"), entry.getLastModifiedTime(), "mtime");
            assertEquals(toFileTime("2022-03-14T01:31:00.706927200Z"), entry.getLastAccessTime(), "atime");
            assertEquals(toFileTime("2022-03-14T01:28:59.700505300Z"), entry.getStatusChangeTime(), "ctime");
            assertEquals(toFileTime("2022-03-14T01:29:00.723509000Z"), entry.getCreationTime(), "birthtime");

            assertEquals('W', tis.read());
            assertTrue(tis.read() < 0, "should be at end of entry");

            assertNull(tis.getNextTarEntry(), "should be at end of file");
        }
    }

    @Test
    public void testShouldWriteTimesAsPaxHeadersForPosixModeAndCreationTimeShouldBeUsedAsCtime() throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tos = new TarArchiveOutputStream(bos)) {
            final TarArchiveEntry entry = createEntryForTimeTests();
            entry.setStatusChangeTime(null);
            tos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
            tos.putArchiveEntry(entry);
            tos.write('W');
            tos.closeArchiveEntry();
        }
        try (TarArchiveInputStream tis = new TarArchiveInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            final TarArchiveEntry entry = tis.getNextTarEntry();
            assertNotNull(entry, "couldn't get entry");

            assertEquals(0, entry.getExtraPaxHeaders().size(), "extra header count");
            assertNull(entry.getExtraPaxHeader("mtime"), "mtime");
            assertNull(entry.getExtraPaxHeader("atime"), "atime");
            assertNull(entry.getExtraPaxHeader("ctime"), "ctime");
            assertNull(entry.getExtraPaxHeader("LIBARCHIVE.creationtime"), "birthtime");
            assertEquals(toFileTime("2022-03-14T01:25:03.599853900Z"), entry.getLastModifiedTime(), "mtime");
            assertEquals(toFileTime("2022-03-14T01:31:00.706927200Z"), entry.getLastAccessTime(), "atime");
            assertEquals(toFileTime("2022-03-14T01:29:00.723509000Z"), entry.getStatusChangeTime(), "ctime");
            assertEquals(toFileTime("2022-03-14T01:29:00.723509000Z"), entry.getCreationTime(), "birthtime");

            assertEquals('W', tis.read());
            assertTrue(tis.read() < 0, "should be at end of entry");

            assertNull(tis.getNextTarEntry(), "should be at end of file");
        }
    }

    @Test
    public void testShouldWriteTimesForStarMode() throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tos = new TarArchiveOutputStream(bos)) {
            final TarArchiveEntry entry = createEntryForTimeTests();
            tos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR);
            tos.putArchiveEntry(entry);
            tos.write('W');
            tos.closeArchiveEntry();
        }
        try (TarArchiveInputStream tis = new TarArchiveInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            final TarArchiveEntry entry = tis.getNextTarEntry();
            assertNotNull(entry, "couldn't get entry");

            assertEquals(0, entry.getExtraPaxHeaders().size(), "extra header count");
            assertNull(entry.getExtraPaxHeader("mtime"), "mtime");
            assertNull(entry.getExtraPaxHeader("atime"), "atime");
            assertNull(entry.getExtraPaxHeader("ctime"), "ctime");
            assertNull(entry.getExtraPaxHeader("LIBARCHIVE.creationtime"), "birthtime");
            assertEquals(toFileTime("2022-03-14T01:25:03Z"), entry.getLastModifiedTime(), "mtime");
            assertEquals(toFileTime("2022-03-14T01:31:00Z"), entry.getLastAccessTime(), "atime");
            assertEquals(toFileTime("2022-03-14T01:28:59Z"), entry.getStatusChangeTime(), "ctime");
            assertNull(entry.getCreationTime(), "birthtime");

            assertEquals('W', tis.read());
            assertTrue(tis.read() < 0, "should be at end of entry");

            assertNull(tis.getNextTarEntry(), "should be at end of file");
        }
    }

    @Test
    public void testTarFileWithFSRoot() throws IOException {
        final File f = File.createTempFile("taetest", ".tar");
        TarArchiveEntry t = new TarArchiveEntry(new File(ROOT));
        try {
            try (TarArchiveOutputStream tout = new TarArchiveOutputStream(Files.newOutputStream(f.toPath()))) {
                tout.putArchiveEntry(t);
                tout.closeArchiveEntry();
                t = new TarArchiveEntry(new File(new File(ROOT), "foo.txt"));
                t.setSize(6);
                tout.putArchiveEntry(t);
                tout.write(new byte[] { 'h', 'e', 'l', 'l', 'o', ' ' });
                tout.closeArchiveEntry();
                t = new TarArchiveEntry(new File(new File(ROOT), "bar.txt").getAbsolutePath());
                t.setSize(5);
                tout.putArchiveEntry(t);
                tout.write(new byte[] { 'w', 'o', 'r', 'l', 'd' });
                tout.closeArchiveEntry();
                t = new TarArchiveEntry("dummy");
                t.setName(new File(new File(ROOT), "baz.txt").getAbsolutePath());
                t.setSize(1);
                tout.putArchiveEntry(t);
                tout.write(new byte[] { '!' });
                tout.closeArchiveEntry();
            }
            try (TarArchiveInputStream tin = new TarArchiveInputStream(Files.newInputStream(f.toPath()))) {
                // tin.setDebug(true);
                t = tin.getNextTarEntry();
                assertNotNull(t);
                assertEquals("/", t.getName());
                assertEquals(TarConstants.LF_DIR, t.getLinkFlag());
                assertTrue(t.isCheckSumOK());
                t = tin.getNextTarEntry();
                assertNotNull(t);
                assertEquals("foo.txt", t.getName());
                assertEquals(TarConstants.LF_NORMAL, t.getLinkFlag());
                assertTrue(t.isCheckSumOK());
                t = tin.getNextTarEntry();
                assertNotNull(t);
                assertEquals("bar.txt", t.getName());
                assertEquals(TarConstants.LF_NORMAL, t.getLinkFlag());
                assertTrue(t.isCheckSumOK());
                t = tin.getNextTarEntry();
                assertNotNull(t);
                assertEquals("baz.txt", t.getName());
                assertEquals(TarConstants.LF_NORMAL, t.getLinkFlag());
                assertTrue(t.isCheckSumOK());
            }
        } finally {
            AbstractTest.forceDelete(f);
        }
    }

    @Test
    @EnabledOnOs(org.junit.jupiter.api.condition.OS.WINDOWS)
    public void testWindowsFileInformationFromFile() throws IOException {
        final TarArchiveEntry entry = new TarArchiveEntry(getFile("test1.xml"));
        assertNotEquals("", entry.getUserName());
    }

    @Test
    @EnabledOnOs(org.junit.jupiter.api.condition.OS.WINDOWS)
    public void testWindowsFileInformationFromPath() throws IOException {
        final TarArchiveEntry entry = new TarArchiveEntry(getPath("test1.xml"));
        assertNotEquals("", entry.getUserName());
    }

    private FileTime toFileTime(final String text) {
        return FileTime.from(Instant.parse(text));
    }
}
