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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Locale;
import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.archivers.zip.ZipEncodingHelper;
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

    @Test
    public void testWriteEntryHeaderToBufferDefault() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(512);
        TarArchiveEntry entry = createTestEntry(buffer);
        entry.writeEntryHeader(buffer);
        assertEquals(0,buffer.remaining());
        buffer.flip();
        byte tmp[] = new byte[512];
        buffer.get(tmp);
        assertEquals(0,buffer.remaining());
        TarArchiveEntry entryDecoded = new TarArchiveEntry(tmp);
        validateEntry(entry, entryDecoded);

    }
    @Test
    public void testWriteEntryHeaderToBufferNotDefault() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(512);
        TarArchiveEntry entry = createTestEntry(buffer);
        entry.writeEntryHeader(buffer, ZipEncodingHelper.getZipEncoding("US-ASCII"),true);
        assertEquals(0,buffer.remaining());
        buffer.flip();
        byte tmp[] = new byte[512];
        buffer.get(tmp);
        assertEquals(0,buffer.remaining());
        TarArchiveEntry entryDecoded = new TarArchiveEntry(tmp);
        validateEntry(entry, entryDecoded);

    }

    private void validateEntry(TarArchiveEntry entry, TarArchiveEntry entryDecoded) {
        assertEquals(entry.getName(),entryDecoded.getName());
        assertEquals(entry.getSize(),entryDecoded.getSize());
        assertEquals(entry.getModTime(),entryDecoded.getModTime());
        assertEquals(entry.getUserName(),entryDecoded.getUserName());
        assertEquals(entry.getGroupName(),entryDecoded.getGroupName());
        assertEquals(entry.getLongUserId(),entryDecoded.getLongUserId());
        assertEquals(entry.getLongGroupId(),entryDecoded.getLongGroupId());
        assertEquals(entry.getDevMajor(),entryDecoded.getDevMajor());
        assertEquals(entry.getDevMinor(),entryDecoded.getDevMinor());
        assertEquals(entry.getMode(),entryDecoded.getMode());
    }

    private TarArchiveEntry createTestEntry(ByteBuffer buffer) throws IOException {
        TarArchiveEntry entry = new TarArchiveEntry("/shortname.txt");
        entry.setSize(07777);
        entry.setModTime(new Date());
        entry.setUserName("ses");
        entry.setGroupName("wheel");
        entry.setUserId(101);
        entry.setGroupId(0);
        entry.setDevMajor(04);
        entry.setDevMinor(01);
        entry.setMode(0755);
        return entry;
    }
}
