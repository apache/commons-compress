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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.utils.CharsetNames;
import org.apache.commons.compress.utils.IOUtils;

public class TarArchiveOutputStreamTest extends AbstractTestCase {

    public void testCount() throws Exception {
        File f = File.createTempFile("commons-compress-tarcount", ".tar");
        f.deleteOnExit();
        FileOutputStream fos = new FileOutputStream(f);

        ArchiveOutputStream tarOut = new ArchiveStreamFactory()
            .createArchiveOutputStream(ArchiveStreamFactory.TAR, fos);

        File file1 = getFile("test1.xml");
        TarArchiveEntry sEntry = new TarArchiveEntry(file1, file1.getName());
        tarOut.putArchiveEntry(sEntry);

        FileInputStream in = new FileInputStream(file1);
        byte[] buf = new byte[8192];

        int read = 0;
        while ((read = in.read(buf)) > 0) {
            tarOut.write(buf, 0, read);
        }

        in.close();
        tarOut.closeArchiveEntry();
        tarOut.close();

        assertEquals(f.length(), tarOut.getBytesWritten());
    }

    public void testMaxFileSizeError() throws Exception {
        TarArchiveEntry t = new TarArchiveEntry("foo");
        t.setSize(077777777777L);
        TarArchiveOutputStream tos =
            new TarArchiveOutputStream(new ByteArrayOutputStream());
        tos.putArchiveEntry(t);
        t.setSize(0100000000000L);
        tos = new TarArchiveOutputStream(new ByteArrayOutputStream());
        try {
            tos.putArchiveEntry(t);
            fail("Should have generated RuntimeException");
        } catch (RuntimeException expected) {
        }
    }

    public void testBigNumberStarMode() throws Exception {
        TarArchiveEntry t = new TarArchiveEntry("foo");
        t.setSize(0100000000000L);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        TarArchiveOutputStream tos = new TarArchiveOutputStream(bos);
        tos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR);
        tos.putArchiveEntry(t);
        // make sure header is written to byte array
        tos.write(new byte[10 * 1024]);
        byte[] data = bos.toByteArray();
        assertEquals(0x80,
                     data[TarConstants.NAMELEN
                        + TarConstants.MODELEN
                        + TarConstants.UIDLEN
                        + TarConstants.GIDLEN] & 0x80);
        TarArchiveInputStream tin =
            new TarArchiveInputStream(new ByteArrayInputStream(data));
        TarArchiveEntry e = tin.getNextTarEntry();
        assertEquals(0100000000000L, e.getSize());
        tin.close();
        // generates IOE because of unclosed entries.
        // However we don't really want to create such large entries.
        closeQuietly(tos);
    }

    public void testBigNumberPosixMode() throws Exception {
        TarArchiveEntry t = new TarArchiveEntry("foo");
        t.setSize(0100000000000L);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        TarArchiveOutputStream tos = new TarArchiveOutputStream(bos);
        tos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
        tos.putArchiveEntry(t);
        // make sure header is written to byte array
        tos.write(new byte[10 * 1024]);
        byte[] data = bos.toByteArray();
        assertEquals("00000000000 ",
                     new String(data,
                                1024 + TarConstants.NAMELEN
                                + TarConstants.MODELEN
                                + TarConstants.UIDLEN
                                + TarConstants.GIDLEN, 12,
                                CharsetNames.UTF_8));
        TarArchiveInputStream tin =
            new TarArchiveInputStream(new ByteArrayInputStream(data));
        TarArchiveEntry e = tin.getNextTarEntry();
        assertEquals(0100000000000L, e.getSize());
        tin.close();
        // generates IOE because of unclosed entries.
        // However we don't really want to create such large entries.
        closeQuietly(tos);
    }

    public void testWriteSimplePaxHeaders() throws Exception {
        Map<String, String> m = new HashMap<String, String>();
        m.put("a", "b");
        byte[] data = writePaxHeader(m);
        assertEquals("00000000006 ",
                     new String(data, TarConstants.NAMELEN
                                + TarConstants.MODELEN
                                + TarConstants.UIDLEN
                                + TarConstants.GIDLEN, 12,
                                CharsetNames.UTF_8));
        assertEquals("6 a=b\n", new String(data, 512, 6, CharsetNames.UTF_8));
    }

    public void testPaxHeadersWithLength99() throws Exception {
        Map<String, String> m = new HashMap<String, String>();
        m.put("a",
              "0123456789012345678901234567890123456789"
              + "01234567890123456789012345678901234567890123456789"
              + "012");
        byte[] data = writePaxHeader(m);
        assertEquals("00000000143 ",
                     new String(data, TarConstants.NAMELEN
                                + TarConstants.MODELEN
                                + TarConstants.UIDLEN
                                + TarConstants.GIDLEN, 12,
                                CharsetNames.UTF_8));
        assertEquals("99 a=0123456789012345678901234567890123456789"
              + "01234567890123456789012345678901234567890123456789"
              + "012\n", new String(data, 512, 99, CharsetNames.UTF_8));
    }

    public void testPaxHeadersWithLength101() throws Exception {
        Map<String, String> m = new HashMap<String, String>();
        m.put("a",
              "0123456789012345678901234567890123456789"
              + "01234567890123456789012345678901234567890123456789"
              + "0123");
        byte[] data = writePaxHeader(m);
        assertEquals("00000000145 ",
                     new String(data, TarConstants.NAMELEN
                                + TarConstants.MODELEN
                                + TarConstants.UIDLEN
                                + TarConstants.GIDLEN, 12,
                                CharsetNames.UTF_8));
        assertEquals("101 a=0123456789012345678901234567890123456789"
              + "01234567890123456789012345678901234567890123456789"
              + "0123\n", new String(data, 512, 101, CharsetNames.UTF_8));
    }

    private byte[] writePaxHeader(Map<String, String> m) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        TarArchiveOutputStream tos = new TarArchiveOutputStream(bos, "ASCII");
        tos.writePaxHeaders("foo", m);

        // add a dummy entry so data gets written
        TarArchiveEntry t = new TarArchiveEntry("foo");
        t.setSize(10 * 1024);
        tos.putArchiveEntry(t);
        tos.write(new byte[10 * 1024]);
        tos.closeArchiveEntry();
        tos.close();

        return bos.toByteArray();
    }

    public void testWriteLongFileNamePosixMode() throws Exception {
        String n = "01234567890123456789012345678901234567890123456789"
            + "01234567890123456789012345678901234567890123456789"
            + "01234567890123456789012345678901234567890123456789";
        TarArchiveEntry t =
            new TarArchiveEntry(n);
        t.setSize(10 * 1024);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        TarArchiveOutputStream tos = new TarArchiveOutputStream(bos, "ASCII");
        tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
        tos.putArchiveEntry(t);
        tos.write(new byte[10 * 1024]);
        tos.closeArchiveEntry();
        byte[] data = bos.toByteArray();
        assertEquals("160 path=" + n + "\n",
                     new String(data, 512, 160, CharsetNames.UTF_8));
        TarArchiveInputStream tin =
            new TarArchiveInputStream(new ByteArrayInputStream(data));
        TarArchiveEntry e = tin.getNextTarEntry();
        assertEquals(n, e.getName());
        tin.close();
        tos.close();
    }

    public void testOldEntryStarMode() throws Exception {
        TarArchiveEntry t = new TarArchiveEntry("foo");
        t.setSize(Integer.MAX_VALUE);
        t.setModTime(-1000);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        TarArchiveOutputStream tos = new TarArchiveOutputStream(bos);
        tos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR);
        tos.putArchiveEntry(t);
        // make sure header is written to byte array
        tos.write(new byte[10 * 1024]);
        byte[] data = bos.toByteArray();
        assertEquals((byte) 0xff,
                     data[TarConstants.NAMELEN
                          + TarConstants.MODELEN
                          + TarConstants.UIDLEN
                          + TarConstants.GIDLEN
                          + TarConstants.SIZELEN]);
        TarArchiveInputStream tin =
            new TarArchiveInputStream(new ByteArrayInputStream(data));
        TarArchiveEntry e = tin.getNextTarEntry();
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        cal.set(1969, 11, 31, 23, 59, 59);
        cal.set(Calendar.MILLISECOND, 0);
        assertEquals(cal.getTime(), e.getLastModifiedDate());
        tin.close();
        // generates IOE because of unclosed entries.
        // However we don't really want to create such large entries.
        closeQuietly(tos);
    }

    public void testOldEntryPosixMode() throws Exception {
        TarArchiveEntry t = new TarArchiveEntry("foo");
        t.setSize(Integer.MAX_VALUE);
        t.setModTime(-1000);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        TarArchiveOutputStream tos = new TarArchiveOutputStream(bos);
        tos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
        tos.putArchiveEntry(t);
        // make sure header is written to byte array
        tos.write(new byte[10 * 1024]);
        byte[] data = bos.toByteArray();
        assertEquals("00000000000 ",
                     new String(data,
                                1024 + TarConstants.NAMELEN
                                + TarConstants.MODELEN
                                + TarConstants.UIDLEN
                                + TarConstants.GIDLEN
                                + TarConstants.SIZELEN, 12,
                                CharsetNames.UTF_8));
        TarArchiveInputStream tin =
            new TarArchiveInputStream(new ByteArrayInputStream(data));
        TarArchiveEntry e = tin.getNextTarEntry();
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        cal.set(1969, 11, 31, 23, 59, 59);
        cal.set(Calendar.MILLISECOND, 0);
        assertEquals(cal.getTime(), e.getLastModifiedDate());
        tin.close();
        // generates IOE because of unclosed entries.
        // However we don't really want to create such large entries.
        closeQuietly(tos);
    }

    public void testOldEntryError() throws Exception {
        TarArchiveEntry t = new TarArchiveEntry("foo");
        t.setSize(Integer.MAX_VALUE);
        t.setModTime(-1000);
        TarArchiveOutputStream tos =
            new TarArchiveOutputStream(new ByteArrayOutputStream());
        try {
            tos.putArchiveEntry(t);
            fail("Should have generated RuntimeException");
        } catch (RuntimeException expected) {
        }
        tos.close();
    }

    public void testWriteNonAsciiPathNamePaxHeader() throws Exception {
        String n = "\u00e4";
        TarArchiveEntry t = new TarArchiveEntry(n);
        t.setSize(10 * 1024);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        TarArchiveOutputStream tos = new TarArchiveOutputStream(bos);
        tos.setAddPaxHeadersForNonAsciiNames(true);
        tos.putArchiveEntry(t);
        tos.write(new byte[10 * 1024]);
        tos.closeArchiveEntry();
        tos.close();
        byte[] data = bos.toByteArray();
        assertEquals("11 path=" + n + "\n",
                     new String(data, 512, 11, CharsetNames.UTF_8));
        TarArchiveInputStream tin =
            new TarArchiveInputStream(new ByteArrayInputStream(data));
        TarArchiveEntry e = tin.getNextTarEntry();
        assertEquals(n, e.getName());
        tin.close();
    }

    public void testWriteNonAsciiLinkPathNamePaxHeader() throws Exception {
        String n = "\u00e4";
        TarArchiveEntry t = new TarArchiveEntry("a", TarConstants.LF_LINK);
        t.setSize(10 * 1024);
        t.setLinkName(n);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        TarArchiveOutputStream tos = new TarArchiveOutputStream(bos);
        tos.setAddPaxHeadersForNonAsciiNames(true);
        tos.putArchiveEntry(t);
        tos.write(new byte[10 * 1024]);
        tos.closeArchiveEntry();
        tos.close();
        byte[] data = bos.toByteArray();
        assertEquals("15 linkpath=" + n + "\n",
                     new String(data, 512, 15, CharsetNames.UTF_8));
        TarArchiveInputStream tin =
            new TarArchiveInputStream(new ByteArrayInputStream(data));
        TarArchiveEntry e = tin.getNextTarEntry();
        assertEquals(n, e.getLinkName());
        tin.close();
    }

    /**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-200"
     */
    public void testRoundtripWith67CharFileNameGnu() throws Exception {
        testRoundtripWith67CharFileName(TarArchiveOutputStream.LONGFILE_GNU);
    }

    /**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-200"
     */
    public void testRoundtripWith67CharFileNamePosix() throws Exception {
        testRoundtripWith67CharFileName(TarArchiveOutputStream.LONGFILE_POSIX);
    }

    private void testRoundtripWith67CharFileName(int mode) throws Exception {
        String n = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAAAAAA";
        assertEquals(67, n.length());
        TarArchiveEntry t = new TarArchiveEntry(n);
        t.setSize(10 * 1024);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        TarArchiveOutputStream tos = new TarArchiveOutputStream(bos, "ASCII");
        tos.setLongFileMode(mode);
        tos.putArchiveEntry(t);
        tos.write(new byte[10 * 1024]);
        tos.closeArchiveEntry();
        tos.close();
        byte[] data = bos.toByteArray();
        TarArchiveInputStream tin =
            new TarArchiveInputStream(new ByteArrayInputStream(data));
        TarArchiveEntry e = tin.getNextTarEntry();
        assertEquals(n, e.getName());
        tin.close();
    }

    public void testWriteLongDirectoryNameErrorMode() throws Exception {
        String n = "01234567890123456789012345678901234567890123456789"
                + "01234567890123456789012345678901234567890123456789"
                + "01234567890123456789012345678901234567890123456789/";

        try {
            TarArchiveEntry t = new TarArchiveEntry(n);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            TarArchiveOutputStream tos = new TarArchiveOutputStream(bos, "ASCII");
            tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_ERROR);
            tos.putArchiveEntry(t);
            tos.closeArchiveEntry();
            tos.close();
            
            fail("Truncated name didn't throw an exception");
        } catch (RuntimeException e) {
            // expected
        }
    }

    public void testWriteLongDirectoryNameTruncateMode() throws Exception {
        String n = "01234567890123456789012345678901234567890123456789"
            + "01234567890123456789012345678901234567890123456789"
            + "01234567890123456789012345678901234567890123456789/";
        TarArchiveEntry t = new TarArchiveEntry(n);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        TarArchiveOutputStream tos = new TarArchiveOutputStream(bos, "ASCII");
        tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_TRUNCATE);
        tos.putArchiveEntry(t);
        tos.closeArchiveEntry();
        tos.close();
        byte[] data = bos.toByteArray();
        TarArchiveInputStream tin =
            new TarArchiveInputStream(new ByteArrayInputStream(data));
        TarArchiveEntry e = tin.getNextTarEntry();
        assertEquals("Entry name", n.substring(0, TarConstants.NAMELEN) + "/", e.getName());
        assertTrue("The entry is not a directory", e.isDirectory());
        tin.close();
    }

    /**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-203"
     */
    public void testWriteLongDirectoryNameGnuMode() throws Exception {
        testWriteLongDirectoryName(TarArchiveOutputStream.LONGFILE_GNU);
    }

    /**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-203"
     */
    public void testWriteLongDirectoryNamePosixMode() throws Exception {
        testWriteLongDirectoryName(TarArchiveOutputStream.LONGFILE_POSIX);
    }

    private void testWriteLongDirectoryName(int mode) throws Exception {
        String n = "01234567890123456789012345678901234567890123456789"
            + "01234567890123456789012345678901234567890123456789"
            + "01234567890123456789012345678901234567890123456789/";
        TarArchiveEntry t = new TarArchiveEntry(n);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        TarArchiveOutputStream tos = new TarArchiveOutputStream(bos, "ASCII");
        tos.setLongFileMode(mode);
        tos.putArchiveEntry(t);
        tos.closeArchiveEntry();
        tos.close();
        byte[] data = bos.toByteArray();
        TarArchiveInputStream tin =
            new TarArchiveInputStream(new ByteArrayInputStream(data));
        TarArchiveEntry e = tin.getNextTarEntry();
        assertEquals(n, e.getName());
        assertTrue(e.isDirectory());
        tin.close();
    }

    /**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-203"
     */
    public void testWriteNonAsciiDirectoryNamePosixMode() throws Exception {
        String n = "f\u00f6\u00f6/";
        TarArchiveEntry t = new TarArchiveEntry(n);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        TarArchiveOutputStream tos = new TarArchiveOutputStream(bos);
        tos.setAddPaxHeadersForNonAsciiNames(true);
        tos.putArchiveEntry(t);
        tos.closeArchiveEntry();
        tos.close();
        byte[] data = bos.toByteArray();
        TarArchiveInputStream tin =
            new TarArchiveInputStream(new ByteArrayInputStream(data));
        TarArchiveEntry e = tin.getNextTarEntry();
        assertEquals(n, e.getName());
        assertTrue(e.isDirectory());
        tin.close();
    }

    /**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-237"
     */
    public void testWriteLongLinkNameErrorMode() throws Exception {
        String linkname = "01234567890123456789012345678901234567890123456789"
                + "01234567890123456789012345678901234567890123456789"
                + "01234567890123456789012345678901234567890123456789/test";
        TarArchiveEntry entry = new TarArchiveEntry("test", TarArchiveEntry.LF_SYMLINK);
        entry.setLinkName(linkname);
        
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            TarArchiveOutputStream tos = new TarArchiveOutputStream(bos, "ASCII");
            tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_ERROR);
            tos.putArchiveEntry(entry);
            tos.closeArchiveEntry();
            tos.close();
            
            fail("Truncated link name didn't throw an exception");
        } catch (RuntimeException e) {
            // expected
        }
    }

    public void testWriteLongLinkNameTruncateMode() throws Exception {
        String linkname = "01234567890123456789012345678901234567890123456789"
            + "01234567890123456789012345678901234567890123456789"
            + "01234567890123456789012345678901234567890123456789/";
        TarArchiveEntry entry = new TarArchiveEntry("test" , TarArchiveEntry.LF_SYMLINK);
        entry.setLinkName(linkname);
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        TarArchiveOutputStream tos = new TarArchiveOutputStream(bos, "ASCII");
        tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_TRUNCATE);
        tos.putArchiveEntry(entry);
        tos.closeArchiveEntry();
        tos.close();
        
        byte[] data = bos.toByteArray();
        TarArchiveInputStream tin = new TarArchiveInputStream(new ByteArrayInputStream(data));
        TarArchiveEntry e = tin.getNextTarEntry();
        assertEquals("Link name", linkname.substring(0, TarConstants.NAMELEN), e.getLinkName());
        tin.close();
    }

    /**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-237"
     */
    public void testWriteLongLinkNameGnuMode() throws Exception {
        testWriteLongLinkName(TarArchiveOutputStream.LONGFILE_GNU);
    }

    /**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-237"
     */
    public void testWriteLongLinkNamePosixMode() throws Exception {
        testWriteLongLinkName(TarArchiveOutputStream.LONGFILE_POSIX);
    }

    /**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-237"
     */
    public void testWriteLongLinkName(int mode) throws Exception {
        String linkname = "01234567890123456789012345678901234567890123456789"
            + "01234567890123456789012345678901234567890123456789"
            + "01234567890123456789012345678901234567890123456789/test";
        TarArchiveEntry entry = new TarArchiveEntry("test", TarArchiveEntry.LF_SYMLINK);
        entry.setLinkName(linkname);
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        TarArchiveOutputStream tos = new TarArchiveOutputStream(bos, "ASCII");
        tos.setLongFileMode(mode);
        tos.putArchiveEntry(entry);
        tos.closeArchiveEntry();
        tos.close();
        
        byte[] data = bos.toByteArray();
        TarArchiveInputStream tin = new TarArchiveInputStream(new ByteArrayInputStream(data));
        TarArchiveEntry e = tin.getNextTarEntry();
        assertEquals("Entry name", "test", e.getName());
        assertEquals("Link name", linkname, e.getLinkName());
        assertTrue("The entry is not a symbolic link", e.isSymbolicLink());
        tin.close();
    }

    public void testPadsOutputToFullBlockLength() throws Exception {
        File f = File.createTempFile("commons-compress-padding", ".tar");
        f.deleteOnExit();
        FileOutputStream fos = new FileOutputStream(f);
        TarArchiveOutputStream tos = new TarArchiveOutputStream(fos);
        File file1 = getFile("test1.xml");
        TarArchiveEntry sEntry = new TarArchiveEntry(file1, file1.getName());
        tos.putArchiveEntry(sEntry);
        FileInputStream in = new FileInputStream(file1);
        IOUtils.copy(in, tos);
        in.close();
        tos.closeArchiveEntry();
        tos.close();
        // test1.xml is small enough to fit into the default block size
        assertEquals(TarConstants.DEFAULT_BLKSIZE, f.length());
    }

}
