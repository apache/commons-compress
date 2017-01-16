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
import java.util.zip.ZipEntry;

import org.junit.Test;

/**
 * JUnit testcases for org.apache.commons.compress.archivers.zip.ZipEntry.
 *
 */
public class ZipArchiveEntryTest {

    /**
     * test handling of extra fields
     */
    @Test
    public void testExtraFields() {
        final AsiExtraField a = new AsiExtraField();
        a.setDirectory(true);
        a.setMode(0755);
        final UnrecognizedExtraField u = new UnrecognizedExtraField();
        u.setHeaderId(ExtraFieldUtilsTest.UNRECOGNIZED_HEADER);
        u.setLocalFileDataData(new byte[0]);

        final ZipArchiveEntry ze = new ZipArchiveEntry("test/");
        ze.setExtraFields(new ZipExtraField[] {a, u});
        final byte[] data1 = ze.getExtra();
        ZipExtraField[] result = ze.getExtraFields();
        assertEquals("first pass", 2, result.length);
        assertSame(a, result[0]);
        assertSame(u, result[1]);

        final UnrecognizedExtraField u2 = new UnrecognizedExtraField();
        u2.setHeaderId(ExtraFieldUtilsTest.UNRECOGNIZED_HEADER);
        u2.setLocalFileDataData(new byte[] {1});

        ze.addExtraField(u2);
        final byte[] data2 = ze.getExtra();
        result = ze.getExtraFields();
        assertEquals("second pass", 2, result.length);
        assertSame(a, result[0]);
        assertSame(u2, result[1]);
        assertEquals("length second pass", data1.length+1, data2.length);

        final UnrecognizedExtraField u3 = new UnrecognizedExtraField();
        u3.setHeaderId(new ZipShort(2));
        u3.setLocalFileDataData(new byte[] {1});
        ze.addExtraField(u3);
        result = ze.getExtraFields();
        assertEquals("third pass", 3, result.length);

        ze.removeExtraField(ExtraFieldUtilsTest.UNRECOGNIZED_HEADER);
        final byte[] data3 = ze.getExtra();
        result = ze.getExtraFields();
        assertEquals("fourth pass", 2, result.length);
        assertSame(a, result[0]);
        assertSame(u3, result[1]);
        assertEquals("length fourth pass", data2.length, data3.length);

        try {
            ze.removeExtraField(ExtraFieldUtilsTest.UNRECOGNIZED_HEADER);
            fail("should be no such element");
        } catch (final java.util.NoSuchElementException nse) {
        }
    }

    /**
     * test handling of extra fields via central directory
     */
    @Test
    public void testExtraFieldMerging() {
        final AsiExtraField a = new AsiExtraField();
        a.setDirectory(true);
        a.setMode(0755);
        final UnrecognizedExtraField u = new UnrecognizedExtraField();
        u.setHeaderId(ExtraFieldUtilsTest.UNRECOGNIZED_HEADER);
        u.setLocalFileDataData(new byte[0]);

        final ZipArchiveEntry ze = new ZipArchiveEntry("test/");
        ze.setExtraFields(new ZipExtraField[] {a, u});

        // merge
        // Header-ID 1 + length 1 + one byte of data
        final byte[] b = ExtraFieldUtilsTest.UNRECOGNIZED_HEADER.getBytes();
        ze.setCentralDirectoryExtra(new byte[] {b[0], b[1], 1, 0, 127});

        ZipExtraField[] result = ze.getExtraFields();
        assertEquals("first pass", 2, result.length);
        assertSame(a, result[0]);
        assertEquals(ExtraFieldUtilsTest.UNRECOGNIZED_HEADER,
                     result[1].getHeaderId());
        assertEquals(new ZipShort(0), result[1].getLocalFileDataLength());
        assertEquals(new ZipShort(1), result[1].getCentralDirectoryLength());

        // add new
        // Header-ID 2 + length 0
        ze.setCentralDirectoryExtra(new byte[] {2, 0, 0, 0});

        result = ze.getExtraFields();
        assertEquals("second pass", 3, result.length);

        // merge
        // Header-ID 2 + length 1 + one byte of data
        ze.setExtra(new byte[] {2, 0, 1, 0, 127});

        result = ze.getExtraFields();
        assertEquals("third pass", 3, result.length);
        assertSame(a, result[0]);
        assertEquals(new ZipShort(2), result[2].getHeaderId());
        assertEquals(new ZipShort(1), result[2].getLocalFileDataLength());
        assertEquals(new ZipShort(0), result[2].getCentralDirectoryLength());
    }

    /**
     * test handling of extra fields
     */
    @Test
    public void testAddAsFirstExtraField() {
        final AsiExtraField a = new AsiExtraField();
        a.setDirectory(true);
        a.setMode(0755);
        final UnrecognizedExtraField u = new UnrecognizedExtraField();
        u.setHeaderId(ExtraFieldUtilsTest.UNRECOGNIZED_HEADER);
        u.setLocalFileDataData(new byte[0]);

        final ZipArchiveEntry ze = new ZipArchiveEntry("test/");
        ze.setExtraFields(new ZipExtraField[] {a, u});
        final byte[] data1 = ze.getExtra();

        final UnrecognizedExtraField u2 = new UnrecognizedExtraField();
        u2.setHeaderId(ExtraFieldUtilsTest.UNRECOGNIZED_HEADER);
        u2.setLocalFileDataData(new byte[] {1});

        ze.addAsFirstExtraField(u2);
        final byte[] data2 = ze.getExtra();
        ZipExtraField[] result = ze.getExtraFields();
        assertEquals("second pass", 2, result.length);
        assertSame(u2, result[0]);
        assertSame(a, result[1]);
        assertEquals("length second pass", data1.length + 1, data2.length);

        final UnrecognizedExtraField u3 = new UnrecognizedExtraField();
        u3.setHeaderId(new ZipShort(2));
        u3.setLocalFileDataData(new byte[] {1});
        ze.addAsFirstExtraField(u3);
        result = ze.getExtraFields();
        assertEquals("third pass", 3, result.length);
        assertSame(u3, result[0]);
        assertSame(u2, result[1]);
        assertSame(a, result[2]);
    }

    @Test
    public void testUnixMode() {
        ZipArchiveEntry ze = new ZipArchiveEntry("foo");
        assertEquals(0, ze.getPlatform());
        ze.setUnixMode(0755);
        assertEquals(3, ze.getPlatform());
        assertEquals(0755,
                     (ze.getExternalAttributes() >> 16) & 0xFFFF);
        assertEquals(0, ze.getExternalAttributes()  & 0xFFFF);

        ze.setUnixMode(0444);
        assertEquals(3, ze.getPlatform());
        assertEquals(0444,
                     (ze.getExternalAttributes() >> 16) & 0xFFFF);
        assertEquals(1, ze.getExternalAttributes()  & 0xFFFF);

        ze = new ZipArchiveEntry("foo/");
        assertEquals(0, ze.getPlatform());
        ze.setUnixMode(0777);
        assertEquals(3, ze.getPlatform());
        assertEquals(0777,
                     (ze.getExternalAttributes() >> 16) & 0xFFFF);
        assertEquals(0x10, ze.getExternalAttributes()  & 0xFFFF);

        ze.setUnixMode(0577);
        assertEquals(3, ze.getPlatform());
        assertEquals(0577,
                     (ze.getExternalAttributes() >> 16) & 0xFFFF);
        assertEquals(0x11, ze.getExternalAttributes()  & 0xFFFF);
    }

    /**
     * Test case for
     * <a href="https://issues.apache.org/jira/browse/COMPRESS-93"
     * >COMPRESS-93</a>.
     */
    @Test
    public void testCompressionMethod() throws Exception {
        final ZipArchiveOutputStream zos =
            new ZipArchiveOutputStream(new ByteArrayOutputStream());
        final ZipArchiveEntry entry = new ZipArchiveEntry("foo");
        assertEquals(-1, entry.getMethod());
        assertFalse(zos.canWriteEntryData(entry));

        entry.setMethod(ZipEntry.STORED);
        assertEquals(ZipEntry.STORED, entry.getMethod());
        assertTrue(zos.canWriteEntryData(entry));

        entry.setMethod(ZipEntry.DEFLATED);
        assertEquals(ZipEntry.DEFLATED, entry.getMethod());
        assertTrue(zos.canWriteEntryData(entry));

        // Test the unsupported "imploded" compression method (6)
        entry.setMethod(6);
        assertEquals(6, entry.getMethod());
        assertFalse(zos.canWriteEntryData(entry));
        zos.close();
    }

    /**
     * Test case for
     * <a href="https://issues.apache.org/jira/browse/COMPRESS-94"
     * >COMPRESS-94</a>.
     */
    @Test
    public void testNotEquals() {
        final ZipArchiveEntry entry1 = new ZipArchiveEntry("foo");
        final ZipArchiveEntry entry2 = new ZipArchiveEntry("bar");
        assertFalse(entry1.equals(entry2));
    }

    /**
     * Tests comment's influence on equals comparisons.
     * @see "https://issues.apache.org/jira/browse/COMPRESS-187"
     */
    @Test
    public void testNullCommentEqualsEmptyComment() {
        final ZipArchiveEntry entry1 = new ZipArchiveEntry("foo");
        final ZipArchiveEntry entry2 = new ZipArchiveEntry("foo");
        final ZipArchiveEntry entry3 = new ZipArchiveEntry("foo");
        entry1.setComment(null);
        entry2.setComment("");
        entry3.setComment("bar");
        assertEquals(entry1, entry2);
        assertFalse(entry1.equals(entry3));
        assertFalse(entry2.equals(entry3));
    }

    @Test
    public void testCopyConstructor() throws Exception {
        final ZipArchiveEntry archiveEntry = new ZipArchiveEntry("fred");
        archiveEntry.setUnixMode(0664);
        archiveEntry.setMethod(ZipEntry.DEFLATED);
        archiveEntry.getGeneralPurposeBit().useStrongEncryption(true);
        final ZipArchiveEntry copy = new ZipArchiveEntry(archiveEntry);
        assertEquals(archiveEntry, copy);
    }

    /**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-379"
     */
    @Test
    public void isUnixSymlinkIsFalseIfMoreThanOneFlagIsSet() throws Exception {
        try (ZipFile zf = new ZipFile(getFile("COMPRESS-379.jar"))) {
            ZipArchiveEntry ze = zf.getEntry("META-INF/maven/");
            assertFalse(ze.isUnixSymlink());
        }
    }

    @Test
    public void testIsUnixSymlink() {
        ZipArchiveEntry ze = new ZipArchiveEntry("foo");
        ze.setUnixMode(UnixStat.LINK_FLAG);
        assertTrue(ze.isUnixSymlink());
        ze.setUnixMode(UnixStat.LINK_FLAG | UnixStat.DIR_FLAG);
        assertFalse(ze.isUnixSymlink());
    }

}
