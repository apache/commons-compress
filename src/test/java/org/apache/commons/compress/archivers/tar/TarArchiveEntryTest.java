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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Locale;
import org.apache.commons.compress.AbstractTestCase;
import org.junit.Test;

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
            tout = new TarArchiveOutputStream(new FileOutputStream(f));
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

            tin = new TarArchiveInputStream(new FileInputStream(f));
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
        try {
            t.setSize(-1);
            fail("Should have generated IllegalArgumentException");
        } catch (final IllegalArgumentException expected) {
        }
        t.setSize(077777777777L);
        t.setSize(0100000000000L);
    }

    @Test public void testExtraPaxHeaders() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        TarArchiveOutputStream tos = new TarArchiveOutputStream(bos);

        TarArchiveEntry entry = new TarArchiveEntry("./weasels");
        entry.addPaxHeader("APACHE.mustelida","true");
        entry.addPaxHeader("SCHILY.xattr.user.org.apache.weasels","maximum weasels");
        entry.addPaxHeader("size","1");
        assertEquals("extra header count",2,entry.getExtraPaxHeaders().size());
        assertEquals("APACHE.mustelida","true",
            entry.getExtraPaxHeader("APACHE.mustelida"));
        assertEquals("SCHILY.xattr.user.org.apache.weasels","maximum weasels",
            entry.getExtraPaxHeader("SCHILY.xattr.user.org.apache.weasels"));
        assertEquals("size",entry.getSize(),1);

        tos.putArchiveEntry(entry);
        tos.write('W');
        tos.closeArchiveEntry();
        tos.close();
        assertNotEquals("should have extra headers before clear",0,entry.getExtraPaxHeaders().size());
        entry.clearExtraPaxHeaders();
        assertEquals("extra headers should be empty after clear",0,entry.getExtraPaxHeaders().size());
        TarArchiveInputStream tis = new TarArchiveInputStream(new ByteArrayInputStream(bos.toByteArray()));
        entry = tis.getNextTarEntry();
        assertNotNull("couldn't get entry",entry);

        assertEquals("extra header count",2,entry.getExtraPaxHeaders().size());
        assertEquals("APACHE.mustelida","true",
            entry.getExtraPaxHeader("APACHE.mustelida"));
        assertEquals("user.org.apache.weasels","maximum weasels",
            entry.getExtraPaxHeader("SCHILY.xattr.user.org.apache.weasels"));

        assertEquals('W',tis.read());
        assertTrue("should be at end of entry",tis.read() <0);

        assertNull("should be at end of file",tis.getNextTarEntry());
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
