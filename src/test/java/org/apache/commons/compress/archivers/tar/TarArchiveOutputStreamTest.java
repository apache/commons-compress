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
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.Test;

public class TarArchiveOutputStreamTest extends AbstractTestCase {

    private static byte[] createTarArchiveContainingOneDirectory(final String fname, final Date modificationDate) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tarOut = new TarArchiveOutputStream(baos, 1024)) {
            tarOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
            final TarArchiveEntry tarEntry = new TarArchiveEntry("d");
            tarEntry.setModTime(modificationDate);
            tarEntry.setMode(TarArchiveEntry.DEFAULT_DIR_MODE);
            tarEntry.setModTime(modificationDate.getTime());
            tarEntry.setName(fname);
            tarOut.putArchiveEntry(tarEntry);
            tarOut.closeArchiveEntry();
        }

        return baos.toByteArray();
    }

    private byte[] getResourceContents(final String name) throws IOException {
        ByteArrayOutputStream bos;
        try (InputStream resourceAsStream = getClass().getResourceAsStream(name)) {
            bos = new ByteArrayOutputStream();
            IOUtils.copy(resourceAsStream, bos);
        }
        return bos.toByteArray();
    }

    @Test
    public void testBigNumberErrorMode() throws Exception {
        final TarArchiveEntry t = new TarArchiveEntry("foo");
        t.setSize(0100000000000L);
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tos = new TarArchiveOutputStream(bos)) {
            assertThrows(IllegalArgumentException.class, () -> tos.putArchiveEntry(t));
        }
    }

    @Test
    public void testBigNumberPosixMode() throws Exception {
        final TarArchiveEntry t = new TarArchiveEntry("foo");
        t.setSize(0100000000000L);
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final TarArchiveOutputStream tos = new TarArchiveOutputStream(bos);
        tos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
        tos.putArchiveEntry(t);
        // make sure header is written to byte array
        tos.write(new byte[10 * 1024]);
        final byte[] data = bos.toByteArray();
        assertEquals("00000000000 ",
            new String(data,
                1024 + TarConstants.NAMELEN
                    + TarConstants.MODELEN
                    + TarConstants.UIDLEN
                    + TarConstants.GIDLEN, 12,
                    UTF_8));
        try (TarArchiveInputStream tin = new TarArchiveInputStream(new ByteArrayInputStream(data))) {
            final TarArchiveEntry e = tin.getNextTarEntry();
            assertEquals(0100000000000L, e.getSize());
        }
        // generates IOE because of unclosed entries.
        // However we don't really want to create such large entries.
        closeQuietly(tos);
    }

    @Test
    public void testBigNumberStarMode() throws Exception {
        final TarArchiveEntry t = new TarArchiveEntry("foo");
        t.setSize(0100000000000L);
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final TarArchiveOutputStream tos = new TarArchiveOutputStream(bos);
        tos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR);
        tos.putArchiveEntry(t);
        // make sure header is written to byte array
        tos.write(new byte[10 * 1024]);
        final byte[] data = bos.toByteArray();
        assertEquals(0x80, data[TarConstants.NAMELEN + TarConstants.MODELEN + TarConstants.UIDLEN + TarConstants.GIDLEN] & 0x80);
        try (TarArchiveInputStream tin = new TarArchiveInputStream(new ByteArrayInputStream(data))) {
            final TarArchiveEntry e = tin.getNextTarEntry();
            assertEquals(0100000000000L, e.getSize());
        }
        // generates IOE because of unclosed entries.
        // However we don't really want to create such large entries.
        closeQuietly(tos);
    }

    @Test
    public void testBlockSizes() throws Exception {
        final String fileName = "/test1.xml";
        final byte[] contents = getResourceContents(fileName);
        testPadding(TarConstants.DEFAULT_BLKSIZE, fileName, contents); // USTAR / pre-pax
        testPadding(5120, fileName, contents); // PAX default
        testPadding(1<<15, fileName, contents); //PAX max
        testPadding(-2, fileName, contents);    // don't specify a block size -> use minimum length

        // don't specify a block size -> use minimum length
        assertThrows(IllegalArgumentException.class, () -> testPadding(511, fileName, contents));

        // don't specify a block size -> use minimum length
        assertThrows(IllegalArgumentException.class, () -> testPadding(0, fileName, contents));

        // test with "content" that is an exact multiple of record length
        final byte[] contents2 = new byte[2048];
        java.util.Arrays.fill(contents2, (byte) 42);
        testPadding(TarConstants.DEFAULT_BLKSIZE, fileName, contents2);
    }

    @Test
    public void testCount() throws Exception {
        final File f = File.createTempFile("commons-compress-tarcount", ".tar");
        f.deleteOnExit();
        final OutputStream fos = Files.newOutputStream(f.toPath());

        final ArchiveOutputStream tarOut = ArchiveStreamFactory.DEFAULT
            .createArchiveOutputStream(ArchiveStreamFactory.TAR, fos);

        final File file1 = getFile("test1.xml");
        final TarArchiveEntry sEntry = new TarArchiveEntry(file1, file1.getName());
        tarOut.putArchiveEntry(sEntry);

        try (final InputStream in = Files.newInputStream(file1.toPath())) {
            final byte[] buf = new byte[8192];

            int read = 0;
            while ((read = in.read(buf)) > 0) {
                tarOut.write(buf, 0, read);
            }

        }
        tarOut.closeArchiveEntry();
        tarOut.close();

        assertEquals(f.length(), tarOut.getBytesWritten());
    }

    /**
     * When using long file names the longLinkEntry included the current timestamp as the Entry
     * modification date. This was never exposed to the client but it caused identical archives to
     * have different MD5 hashes.
     */
    @Test
    public void testLongNameMd5Hash() throws Exception {
        final String longFileName = "a/considerably/longer/file/name/which/forces/use/of/the/long/link/header/which/appears/to/always/use/the/current/time/as/modification/date";
        final Date modificationDate = new Date();

        final byte[] archive1 = createTarArchiveContainingOneDirectory(longFileName, modificationDate);
        final byte[] digest1 = MessageDigest.getInstance("MD5").digest(archive1);

        // let a second elapse otherwise the modification dates will be equal
        Thread.sleep(1000L);

        // now recreate exactly the same tar file
        final byte[] archive2 = createTarArchiveContainingOneDirectory(longFileName, modificationDate);
        // and I would expect the MD5 hash to be the same, but for long names it isn't
        final byte[] digest2 = MessageDigest.getInstance("MD5").digest(archive2);

        assertArrayEquals(digest1, digest2);

        // do I still have the correct modification date?
        // let a second elapse so we don't get the current time
        Thread.sleep(1000);
        try (TarArchiveInputStream tarIn = new TarArchiveInputStream(new ByteArrayInputStream(archive2))) {
            final ArchiveEntry nextEntry = tarIn.getNextEntry();
            assertEquals(longFileName, nextEntry.getName());
            // tar archive stores modification time to second granularity only (floored)
            assertEquals(modificationDate.getTime() / 1000, nextEntry.getLastModifiedDate().getTime() / 1000);
        }
    }

    @Test
    public void testMaxFileSizeError() throws Exception {
        final TarArchiveEntry t = new TarArchiveEntry("foo");
        t.setSize(077777777777L);
        final TarArchiveOutputStream tos1 =
            new TarArchiveOutputStream(new ByteArrayOutputStream());
        tos1.putArchiveEntry(t);
        t.setSize(0100000000000L);
        final TarArchiveOutputStream tos2 = new TarArchiveOutputStream(new ByteArrayOutputStream());
        assertThrows(RuntimeException.class, () -> tos2.putArchiveEntry(t),
                "Should have generated RuntimeException");
    }

    @Test
    public void testOldEntryError() throws Exception {
        final TarArchiveEntry t = new TarArchiveEntry("foo");
        t.setSize(Integer.MAX_VALUE);
        t.setModTime(-1000);
        try (TarArchiveOutputStream tos = new TarArchiveOutputStream(new ByteArrayOutputStream())) {
            assertThrows(RuntimeException.class, () -> tos.putArchiveEntry(t));
        }
    }

    @Test
    public void testOldEntryPosixMode() throws Exception {
        final TarArchiveEntry t = new TarArchiveEntry("foo");
        t.setSize(Integer.MAX_VALUE);
        t.setModTime(-1000);
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final TarArchiveOutputStream tos = new TarArchiveOutputStream(bos);
        tos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
        tos.putArchiveEntry(t);
        // make sure header is written to byte array
        tos.write(new byte[10 * 1024]);
        final byte[] data = bos.toByteArray();
        assertEquals("00000000000 ",
            new String(data,
                1024 + TarConstants.NAMELEN
                    + TarConstants.MODELEN
                    + TarConstants.UIDLEN
                    + TarConstants.GIDLEN
                    + TarConstants.SIZELEN, 12,
                    UTF_8));
        try (TarArchiveInputStream tin = new TarArchiveInputStream(new ByteArrayInputStream(data))) {
            final TarArchiveEntry e = tin.getNextTarEntry();
            final Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            cal.set(1969, 11, 31, 23, 59, 59);
            cal.set(Calendar.MILLISECOND, 0);
            assertEquals(cal.getTime(), e.getLastModifiedDate());
        }
        // generates IOE because of unclosed entries.
        // However we don't really want to create such large entries.
        closeQuietly(tos);
    }

    @Test
    public void testOldEntryStarMode() throws Exception {
        final TarArchiveEntry t = new TarArchiveEntry("foo");
        t.setSize(Integer.MAX_VALUE);
        t.setModTime(-1000);
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final TarArchiveOutputStream tos = new TarArchiveOutputStream(bos);
        tos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR);
        tos.putArchiveEntry(t);
        // make sure header is written to byte array
        tos.write(new byte[10 * 1024]);
        final byte[] data = bos.toByteArray();
        assertEquals((byte) 0xff,
            data[TarConstants.NAMELEN
                + TarConstants.MODELEN
                + TarConstants.UIDLEN
                + TarConstants.GIDLEN
                + TarConstants.SIZELEN]);
        try (TarArchiveInputStream tin = new TarArchiveInputStream(new ByteArrayInputStream(data))) {
            final TarArchiveEntry e = tin.getNextTarEntry();
            final Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            cal.set(1969, 11, 31, 23, 59, 59);
            cal.set(Calendar.MILLISECOND, 0);
            assertEquals(cal.getTime(), e.getLastModifiedDate());
        }
        // generates IOE because of unclosed entries.
        // However we don't really want to create such large entries.
        closeQuietly(tos);
    }

    private void testPadding(int blockSize, final String fileName, final byte[] contents) throws IOException {
        final File f = File.createTempFile("commons-compress-padding", ".tar");
        f.deleteOnExit();
        final OutputStream fos = Files.newOutputStream(f.toPath());
        final TarArchiveOutputStream tos;
        if (blockSize != -2) {
            tos = new TarArchiveOutputStream(fos, blockSize);
        } else {
            blockSize = 512;
            tos = new TarArchiveOutputStream(fos);
        }
        TarArchiveEntry sEntry;
        sEntry = new TarArchiveEntry(fileName);
        sEntry.setSize(contents.length);
        tos.putArchiveEntry(sEntry);
        tos.write(contents);
        tos.closeArchiveEntry();
        tos.close();
        final int fileRecordsSize = (int) Math.ceil((double) contents.length / 512) * 512;
        final int headerSize = 512;
        final int endOfArchiveSize = 1024;
        final int unpaddedSize = headerSize + fileRecordsSize + endOfArchiveSize;
        final int paddedSize = (int) Math.ceil((double)unpaddedSize/blockSize)*blockSize;
        assertEquals(paddedSize, f.length());
    }

    @Test
    public void testPaxHeadersWithLength101() throws Exception {
        final Map<String, String> m = new HashMap<>();
        m.put("a",
            "0123456789012345678901234567890123456789"
                + "01234567890123456789012345678901234567890123456789"
                + "0123");
        final byte[] data = writePaxHeader(m);
        assertEquals("00000000145 ",
            new String(data, TarConstants.NAMELEN
                + TarConstants.MODELEN
                + TarConstants.UIDLEN
                + TarConstants.GIDLEN, 12,
                    UTF_8));
        assertEquals("101 a=0123456789012345678901234567890123456789"
            + "01234567890123456789012345678901234567890123456789"
            + "0123\n", new String(data, 512, 101, UTF_8));
    }

    @Test
    public void testPaxHeadersWithLength99() throws Exception {
        final Map<String, String> m = new HashMap<>();
        m.put("a",
            "0123456789012345678901234567890123456789"
                + "01234567890123456789012345678901234567890123456789"
                + "012");
        final byte[] data = writePaxHeader(m);
        assertEquals("00000000143 ",
            new String(data, TarConstants.NAMELEN
                + TarConstants.MODELEN
                + TarConstants.UIDLEN
                + TarConstants.GIDLEN, 12,
                    UTF_8));
        assertEquals("99 a=0123456789012345678901234567890123456789"
            + "01234567890123456789012345678901234567890123456789"
            + "012\n", new String(data, 512, 99, UTF_8));
    }

    @Test
	public void testPutGlobalPaxHeaderEntry() throws IOException {
        final String x = "If at first you don't succeed, give up";
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tos = new TarArchiveOutputStream(bos)) {
            final int pid = 73;
            final int globCount = 1;
            final byte lfPaxGlobalExtendedHeader = TarConstants.LF_PAX_GLOBAL_EXTENDED_HEADER;
            final TarArchiveEntry globalHeader = new TarArchiveEntry("/tmp/GlobalHead." + pid + "." + globCount, lfPaxGlobalExtendedHeader);
            globalHeader.addPaxHeader("SCHILLY.xattr.user.org.apache.weasels", "global-weasels");
            tos.putArchiveEntry(globalHeader);
            TarArchiveEntry entry = new TarArchiveEntry("message");
            entry.setSize(x.length());
            tos.putArchiveEntry(entry);
            tos.write(x.getBytes());
            tos.closeArchiveEntry();
            entry = new TarArchiveEntry("counter-message");
            final String y = "Nothing succeeds like excess";
            entry.setSize(y.length());
            entry.addPaxHeader("SCHILLY.xattr.user.org.apache.weasels.species", "unknown");
            tos.putArchiveEntry(entry);
            tos.write(y.getBytes());
            tos.closeArchiveEntry();
        }
		final TarArchiveInputStream in = new TarArchiveInputStream(new ByteArrayInputStream(bos.toByteArray()));
		TarArchiveEntry entryIn = in.getNextTarEntry();
		assertNotNull(entryIn);
		assertEquals("message", entryIn.getName());
		assertEquals("global-weasels", entryIn.getExtraPaxHeader("SCHILLY.xattr.user.org.apache.weasels"));
		final Reader reader = new InputStreamReader(in);
		for (int i = 0; i < x.length(); i++) {
			assertEquals(x.charAt(i), reader.read());
		}
		assertEquals(-1, reader.read());
		entryIn = in.getNextTarEntry();
		assertEquals("counter-message", entryIn.getName());
		assertEquals("global-weasels", entryIn.getExtraPaxHeader("SCHILLY.xattr.user.org.apache.weasels"));
		assertEquals("unknown", entryIn.getExtraPaxHeader("SCHILLY.xattr.user.org.apache.weasels.species"));
		assertNull(in.getNextTarEntry());
	}

    @SuppressWarnings("deprecation")
    @Test public void testRecordSize() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> new TarArchiveOutputStream(new ByteArrayOutputStream(),512,511),
                "should have rejected recordSize of 511");
        try (TarArchiveOutputStream tos = new TarArchiveOutputStream(new ByteArrayOutputStream(),
            512, 512)) {
            assertEquals(512, tos.getRecordSize(), "recordSize");
        }
        try (TarArchiveOutputStream tos = new TarArchiveOutputStream(new ByteArrayOutputStream(),
            512, 512, null)) {
            assertEquals(512, tos.getRecordSize(), "recordSize");
        }
    }

    private void testRoundtripWith67CharFileName(final int mode) throws Exception {
        final String n = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        assertEquals(67, n.length());
        final TarArchiveEntry t = new TarArchiveEntry(n);
        t.setSize(10 * 1024);
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tos = new TarArchiveOutputStream(bos, "ASCII")) {
            tos.setLongFileMode(mode);
            tos.putArchiveEntry(t);
            tos.write(new byte[10 * 1024]);
            tos.closeArchiveEntry();
        }
        final byte[] data = bos.toByteArray();
        try (TarArchiveInputStream tin = new TarArchiveInputStream(new ByteArrayInputStream(data))) {
            assertEquals(n, tin.getNextTarEntry().getName());
        }
    }

    /**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-200"
     */
    @Test
    public void testRoundtripWith67CharFileNameGnu() throws Exception {
        testRoundtripWith67CharFileName(TarArchiveOutputStream.LONGFILE_GNU);
    }

    /**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-200"
     */
    @Test
    public void testRoundtripWith67CharFileNamePosix() throws Exception {
        testRoundtripWith67CharFileName(TarArchiveOutputStream.LONGFILE_POSIX);
    }

    private void testWriteLongDirectoryName(final int mode) throws Exception {
        final String n = "01234567890123456789012345678901234567890123456789"
            + "01234567890123456789012345678901234567890123456789"
            + "01234567890123456789012345678901234567890123456789/";
        final TarArchiveEntry t = new TarArchiveEntry(n);
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tos = new TarArchiveOutputStream(bos, "ASCII")) {
            tos.setLongFileMode(mode);
            tos.putArchiveEntry(t);
            tos.closeArchiveEntry();
        }
        final byte[] data = bos.toByteArray();
        try (TarArchiveInputStream tin = new TarArchiveInputStream(new ByteArrayInputStream(data))) {
            final TarArchiveEntry e = tin.getNextTarEntry();
            assertEquals(n, e.getName());
            assertTrue(e.isDirectory());
        }
    }

    @Test
    public void testWriteLongDirectoryNameErrorMode() throws Exception {
        final String n = "01234567890123456789012345678901234567890123456789"
            + "01234567890123456789012345678901234567890123456789"
            + "01234567890123456789012345678901234567890123456789/";

        assertThrows(RuntimeException.class, () -> {
            final TarArchiveEntry t = new TarArchiveEntry(n);
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (TarArchiveOutputStream tos = new TarArchiveOutputStream(bos, "ASCII")) {
                tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_ERROR);
                tos.putArchiveEntry(t);
                tos.closeArchiveEntry();
            }
        }, "Truncated name didn't throw an exception");
    }

    /**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-203"
     */
    @Test
    public void testWriteLongDirectoryNameGnuMode() throws Exception {
        testWriteLongDirectoryName(TarArchiveOutputStream.LONGFILE_GNU);
    }

    /**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-203"
     */
    @Test
    public void testWriteLongDirectoryNamePosixMode() throws Exception {
        testWriteLongDirectoryName(TarArchiveOutputStream.LONGFILE_POSIX);
    }

    @Test
    public void testWriteLongDirectoryNameTruncateMode() throws Exception {
        final String n = "01234567890123456789012345678901234567890123456789"
            + "01234567890123456789012345678901234567890123456789"
            + "01234567890123456789012345678901234567890123456789/";
        final TarArchiveEntry t = new TarArchiveEntry(n);
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tos = new TarArchiveOutputStream(bos, "ASCII")) {
            tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_TRUNCATE);
            tos.putArchiveEntry(t);
            tos.closeArchiveEntry();
        }
        final byte[] data = bos.toByteArray();
        try (TarArchiveInputStream tin = new TarArchiveInputStream(new ByteArrayInputStream(data))) {
            final TarArchiveEntry e = tin.getNextTarEntry();
            assertEquals(n.substring(0, TarConstants.NAMELEN) + "/", e.getName(), "Entry name");
            assertTrue(e.isDirectory(), "The entry is not a directory");
        }
    }

    @Test
    public void testWriteLongFileNamePosixMode() throws Exception {
        // @formatter:off
        final String n = "01234567890123456789012345678901234567890123456789"
                + "01234567890123456789012345678901234567890123456789"
                + "01234567890123456789012345678901234567890123456789";
        // @formatter:on
        final TarArchiveEntry t = new TarArchiveEntry(n);
        t.setSize(10 * 1024);
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tos = new TarArchiveOutputStream(bos, "ASCII")) {
            tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            tos.putArchiveEntry(t);
            tos.write(new byte[10 * 1024]);
            tos.closeArchiveEntry();
            final byte[] data = bos.toByteArray();
            assertEquals("160 path=" + n + "\n", new String(data, 512, 160, UTF_8));
            try (TarArchiveInputStream tin = new TarArchiveInputStream(new ByteArrayInputStream(data))) {
                assertEquals(n, tin.getNextTarEntry().getName());
            }
        }
    }

    @Test
    public void testWriteLongFileNameThrowsException() throws Exception {
        final String n = "01234567890123456789012345678901234567890123456789"
                + "01234567890123456789012345678901234567890123456789"
                + "01234567890123456789012345678901234567890123456789";
        final TarArchiveEntry t = new TarArchiveEntry(n);
        final TarArchiveOutputStream tos = new TarArchiveOutputStream(new ByteArrayOutputStream(), "ASCII");
        assertThrows(IllegalArgumentException.class, () -> tos.putArchiveEntry(t));
    }

    /**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-237"
     */
    private void testWriteLongLinkName(final int mode) throws Exception {
        final String linkname = "01234567890123456789012345678901234567890123456789"
            + "01234567890123456789012345678901234567890123456789"
            + "01234567890123456789012345678901234567890123456789/test";
        final TarArchiveEntry entry = new TarArchiveEntry("test", TarConstants.LF_SYMLINK);
        entry.setLinkName(linkname);

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tos = new TarArchiveOutputStream(bos, "ASCII")) {
            tos.setLongFileMode(mode);
            tos.putArchiveEntry(entry);
            tos.closeArchiveEntry();
        }

        final byte[] data = bos.toByteArray();
        try (TarArchiveInputStream tin = new TarArchiveInputStream(new ByteArrayInputStream(data))) {
            final TarArchiveEntry e = tin.getNextTarEntry();
            assertEquals("test", e.getName(), "Entry name");
            assertEquals(linkname, e.getLinkName(), "Link name");
            assertTrue(e.isSymbolicLink(), "The entry is not a symbolic link");
        }
    }

    /**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-237"
     */
    @Test
    public void testWriteLongLinkNameErrorMode() throws Exception {
        final String linkname = "01234567890123456789012345678901234567890123456789"
            + "01234567890123456789012345678901234567890123456789"
            + "01234567890123456789012345678901234567890123456789/test";
        final TarArchiveEntry entry = new TarArchiveEntry("test", TarConstants.LF_SYMLINK);
        entry.setLinkName(linkname);

        assertThrows(RuntimeException.class, () -> {
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (TarArchiveOutputStream tos = new TarArchiveOutputStream(bos, "ASCII")) {
                tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_ERROR);
                tos.putArchiveEntry(entry);
                tos.closeArchiveEntry();
            }
        }, "Truncated link name didn't throw an exception");
    }

    /**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-237"
     */
    @Test
    public void testWriteLongLinkNameGnuMode() throws Exception {
        testWriteLongLinkName(TarArchiveOutputStream.LONGFILE_GNU);
    }

    /**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-237"
     */
    @Test
    public void testWriteLongLinkNamePosixMode() throws Exception {
        testWriteLongLinkName(TarArchiveOutputStream.LONGFILE_POSIX);
    }

    @Test
    public void testWriteLongLinkNameTruncateMode() throws Exception {
        final String linkname = "01234567890123456789012345678901234567890123456789"
            + "01234567890123456789012345678901234567890123456789"
            + "01234567890123456789012345678901234567890123456789/";
        final TarArchiveEntry entry = new TarArchiveEntry("test", TarConstants.LF_SYMLINK);
        entry.setLinkName(linkname);

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tos = new TarArchiveOutputStream(bos, "ASCII")) {
            tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_TRUNCATE);
            tos.putArchiveEntry(entry);
            tos.closeArchiveEntry();
        }

        final byte[] data = bos.toByteArray();
        try (TarArchiveInputStream tin = new TarArchiveInputStream(new ByteArrayInputStream(data))) {
            final TarArchiveEntry e = tin.getNextTarEntry();
            assertEquals(linkname.substring(0, TarConstants.NAMELEN), e.getLinkName(), "Link name");
        }
    }

    /**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-203"
     */
    @Test
    public void testWriteNonAsciiDirectoryNamePosixMode() throws Exception {
        final String n = "f\u00f6\u00f6/";
        final TarArchiveEntry t = new TarArchiveEntry(n);
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tos = new TarArchiveOutputStream(bos)) {
            tos.setAddPaxHeadersForNonAsciiNames(true);
            tos.putArchiveEntry(t);
            tos.closeArchiveEntry();
        }
        final byte[] data = bos.toByteArray();
        try (TarArchiveInputStream tin = new TarArchiveInputStream(new ByteArrayInputStream(data))) {
            final TarArchiveEntry e = tin.getNextTarEntry();
            assertEquals(n, e.getName());
            assertTrue(e.isDirectory());
        }
    }

    @Test
    public void testWriteNonAsciiLinkPathNamePaxHeader() throws Exception {
        final String n = "\u00e4";
        final TarArchiveEntry t = new TarArchiveEntry("a", TarConstants.LF_LINK);
        t.setSize(10 * 1024);
        t.setLinkName(n);
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tos = new TarArchiveOutputStream(bos)) {
            tos.setAddPaxHeadersForNonAsciiNames(true);
            tos.putArchiveEntry(t);
            tos.write(new byte[10 * 1024]);
            tos.closeArchiveEntry();
        }
        final byte[] data = bos.toByteArray();
        assertEquals("15 linkpath=" + n + "\n", new String(data, 512, 15, UTF_8));
        try (TarArchiveInputStream tin = new TarArchiveInputStream(new ByteArrayInputStream(data))) {
            final TarArchiveEntry e = tin.getNextTarEntry();
            assertEquals(n, e.getLinkName());
        }
    }

	/**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-265"
     */
    @Test
    public void testWriteNonAsciiNameWithUnfortunateNamePosixMode() throws Exception {
        final String n = "f\u00f6\u00f6\u00dc";
        final TarArchiveEntry t = new TarArchiveEntry(n);
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tos = new TarArchiveOutputStream(bos)) {
            tos.setAddPaxHeadersForNonAsciiNames(true);
            tos.putArchiveEntry(t);
            tos.closeArchiveEntry();
        }
        final byte[] data = bos.toByteArray();
        try (TarArchiveInputStream tin = new TarArchiveInputStream(new ByteArrayInputStream(data))) {
            final TarArchiveEntry e = tin.getNextTarEntry();
            assertEquals(n, e.getName());
            assertFalse(e.isDirectory());
        }
    }

    @Test
    public void testWriteNonAsciiPathNamePaxHeader() throws Exception {
        final String n = "\u00e4";
        final TarArchiveEntry t = new TarArchiveEntry(n);
        t.setSize(10 * 1024);
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tos = new TarArchiveOutputStream(bos)) {
            tos.setAddPaxHeadersForNonAsciiNames(true);
            tos.putArchiveEntry(t);
            tos.write(new byte[10 * 1024]);
            tos.closeArchiveEntry();
        }
        final byte[] data = bos.toByteArray();
        assertEquals("11 path=" + n + "\n", new String(data, 512, 11, UTF_8));
        try (TarArchiveInputStream tin = new TarArchiveInputStream(new ByteArrayInputStream(data))) {
            final TarArchiveEntry e = tin.getNextTarEntry();
            assertEquals(n, e.getName());
        }
    }

    @Test
    public void testWriteSimplePaxHeaders() throws Exception {
        final Map<String, String> m = new HashMap<>();
        m.put("a", "b");
        final byte[] data = writePaxHeader(m);
        assertEquals("00000000006 ",
            new String(data, TarConstants.NAMELEN
                + TarConstants.MODELEN
                + TarConstants.UIDLEN
                + TarConstants.GIDLEN, 12,
                    UTF_8));
        assertEquals("6 a=b\n", new String(data, 512, 6, UTF_8));
    }

    private byte[] writePaxHeader(final Map<String, String> m) throws Exception {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tos = new TarArchiveOutputStream(bos, "ASCII")) {
            tos.writePaxHeaders(new TarArchiveEntry("x"), "foo", m);

            // add a dummy entry so data gets written
            final TarArchiveEntry t = new TarArchiveEntry("foo");
            t.setSize(10 * 1024);
            tos.putArchiveEntry(t);
            tos.write(new byte[10 * 1024]);
            tos.closeArchiveEntry();
        }

        return bos.toByteArray();
    }

}
