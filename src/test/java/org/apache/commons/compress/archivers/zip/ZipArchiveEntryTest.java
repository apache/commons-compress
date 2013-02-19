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

import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;

/**
 * JUnit 3 testcases for org.apache.commons.compress.archivers.zip.ZipEntry.
 *
 */
public class ZipArchiveEntryTest extends TestCase {

    public ZipArchiveEntryTest(String name) {
        super(name);
    }

    /**
     * test handling of extra fields
     */
    public void testExtraFields() {
        AsiExtraField a = new AsiExtraField();
        a.setDirectory(true);
        a.setMode(0755);
        UnrecognizedExtraField u = new UnrecognizedExtraField();
        u.setHeaderId(ExtraFieldUtilsTest.UNRECOGNIZED_HEADER);
        u.setLocalFileDataData(new byte[0]);

        ZipArchiveEntry ze = new ZipArchiveEntry("test/");
        ze.setExtraFields(new ZipExtraField[] {a, u});
        byte[] data1 = ze.getExtra();
        ZipExtraField[] result = ze.getExtraFields();
        assertEquals("first pass", 2, result.length);
        assertSame(a, result[0]);
        assertSame(u, result[1]);

        UnrecognizedExtraField u2 = new UnrecognizedExtraField();
        u2.setHeaderId(ExtraFieldUtilsTest.UNRECOGNIZED_HEADER);
        u2.setLocalFileDataData(new byte[] {1});

        ze.addExtraField(u2);
        byte[] data2 = ze.getExtra();
        result = ze.getExtraFields();
        assertEquals("second pass", 2, result.length);
        assertSame(a, result[0]);
        assertSame(u2, result[1]);
        assertEquals("length second pass", data1.length+1, data2.length);

        UnrecognizedExtraField u3 = new UnrecognizedExtraField();
        u3.setHeaderId(new ZipShort(2));
        u3.setLocalFileDataData(new byte[] {1});
        ze.addExtraField(u3);
        result = ze.getExtraFields();
        assertEquals("third pass", 3, result.length);

        ze.removeExtraField(ExtraFieldUtilsTest.UNRECOGNIZED_HEADER);
        byte[] data3 = ze.getExtra();
        result = ze.getExtraFields();
        assertEquals("fourth pass", 2, result.length);
        assertSame(a, result[0]);
        assertSame(u3, result[1]);
        assertEquals("length fourth pass", data2.length, data3.length);

        try {
            ze.removeExtraField(ExtraFieldUtilsTest.UNRECOGNIZED_HEADER);
            fail("should be no such element");
        } catch (java.util.NoSuchElementException nse) {
        }
    }

    /**
     * test handling of extra fields via central directory
     */
    public void testExtraFieldMerging() {
        AsiExtraField a = new AsiExtraField();
        a.setDirectory(true);
        a.setMode(0755);
        UnrecognizedExtraField u = new UnrecognizedExtraField();
        u.setHeaderId(ExtraFieldUtilsTest.UNRECOGNIZED_HEADER);
        u.setLocalFileDataData(new byte[0]);

        ZipArchiveEntry ze = new ZipArchiveEntry("test/");
        ze.setExtraFields(new ZipExtraField[] {a, u});

        // merge
        // Header-ID 1 + length 1 + one byte of data
        byte[] b = ExtraFieldUtilsTest.UNRECOGNIZED_HEADER.getBytes();
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
    public void testAddAsFirstExtraField() {
        AsiExtraField a = new AsiExtraField();
        a.setDirectory(true);
        a.setMode(0755);
        UnrecognizedExtraField u = new UnrecognizedExtraField();
        u.setHeaderId(ExtraFieldUtilsTest.UNRECOGNIZED_HEADER);
        u.setLocalFileDataData(new byte[0]);

        ZipArchiveEntry ze = new ZipArchiveEntry("test/");
        ze.setExtraFields(new ZipExtraField[] {a, u});
        byte[] data1 = ze.getExtra();

        UnrecognizedExtraField u2 = new UnrecognizedExtraField();
        u2.setHeaderId(ExtraFieldUtilsTest.UNRECOGNIZED_HEADER);
        u2.setLocalFileDataData(new byte[] {1});

        ze.addAsFirstExtraField(u2);
        byte[] data2 = ze.getExtra();
        ZipExtraField[] result = ze.getExtraFields();
        assertEquals("second pass", 2, result.length);
        assertSame(u2, result[0]);
        assertSame(a, result[1]);
        assertEquals("length second pass", data1.length + 1, data2.length);

        UnrecognizedExtraField u3 = new UnrecognizedExtraField();
        u3.setHeaderId(new ZipShort(2));
        u3.setLocalFileDataData(new byte[] {1});
        ze.addAsFirstExtraField(u3);
        result = ze.getExtraFields();
        assertEquals("third pass", 3, result.length);
        assertSame(u3, result[0]);
        assertSame(u2, result[1]);
        assertSame(a, result[2]);
    }

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
    public void testCompressionMethod() throws Exception {
        ZipArchiveOutputStream zos =
            new ZipArchiveOutputStream(new ByteArrayOutputStream());
        ZipArchiveEntry entry = new ZipArchiveEntry("foo");
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
    public void testNotEquals() {
        ZipArchiveEntry entry1 = new ZipArchiveEntry("foo");
        ZipArchiveEntry entry2 = new ZipArchiveEntry("bar");
        assertFalse(entry1.equals(entry2));
    }

    /**
     * Tests comment's influence on equals comparisons.
     * @see "https://issues.apache.org/jira/browse/COMPRESS-187"
     */
    public void testNullCommentEqualsEmptyComment() {
        ZipArchiveEntry entry1 = new ZipArchiveEntry("foo");
        ZipArchiveEntry entry2 = new ZipArchiveEntry("foo");
        ZipArchiveEntry entry3 = new ZipArchiveEntry("foo");
        entry1.setComment(null);
        entry2.setComment("");
        entry3.setComment("bar");
        assertEquals(entry1, entry2);
        assertFalse(entry1.equals(entry3));
        assertFalse(entry2.equals(entry3));
    }
}
