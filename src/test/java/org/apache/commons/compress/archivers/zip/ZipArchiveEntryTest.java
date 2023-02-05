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
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

import org.apache.commons.compress.utils.ByteUtils;
import org.apache.commons.compress.utils.TimeUtils;
import org.junit.jupiter.api.Test;

/**
 * JUnit tests for org.apache.commons.compress.archivers.zip.ZipEntry.
 *
 */
public class ZipArchiveEntryTest {

    @Test
    public void bestEffortIncludesUnparseableExtraData() throws Exception {
        final ZipExtraField[] extraFields = parsingModeBehaviorTestData();
        final ZipArchiveEntry ze = new ZipArchiveEntry("foo");
        ze.setExtraFields(extraFields);
        final ZipExtraField[] read = ze.getExtraFields(ZipArchiveEntry.ExtraFieldParsingMode.BEST_EFFORT);
        assertEquals(extraFields.length, read.length);
    }

    @Test
    public void draconicThrowsOnUnparseableExtraData() throws Exception {
        final ZipExtraField[] extraFields = parsingModeBehaviorTestData();
        final ZipArchiveEntry ze = new ZipArchiveEntry("foo");
        ze.setExtraFields(extraFields);
        assertThrows(ZipException.class, () -> ze.getExtraFields(ZipArchiveEntry.ExtraFieldParsingMode.DRACONIC));
    }

    /**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-379"
     */
    @Test
    public void isUnixSymlinkIsFalseIfMoreThanOneFlagIsSet() throws Exception {
        try (ZipFile zf = new ZipFile(getFile("COMPRESS-379.jar"))) {
            final ZipArchiveEntry ze = zf.getEntry("META-INF/maven/");
            assertFalse(ze.isUnixSymlink());
        }
    }

    @Test
    public void onlyParseableLenientExcludesUnparseableExtraData() throws Exception {
        final ZipExtraField[] extraFields = parsingModeBehaviorTestData();
        final ZipArchiveEntry ze = new ZipArchiveEntry("foo");
        ze.setExtraFields(extraFields);
        final ZipExtraField[] read = ze.getExtraFields(ZipArchiveEntry.ExtraFieldParsingMode.ONLY_PARSEABLE_LENIENT);
        assertEquals(extraFields.length, read.length + 1);
    }

    @Test
    public void onlyParseableStrictExcludesUnparseableExtraData() throws Exception {
        final ZipExtraField[] extraFields = parsingModeBehaviorTestData();
        final ZipArchiveEntry ze = new ZipArchiveEntry("foo");
        ze.setExtraFields(extraFields);
        final ZipExtraField[] read = ze.getExtraFields(ZipArchiveEntry.ExtraFieldParsingMode.ONLY_PARSEABLE_STRICT);
        assertEquals(extraFields.length, read.length + 1);
    }

    private ZipExtraField[] parsingModeBehaviorTestData() {
        final AsiExtraField a = new AsiExtraField();
        a.setDirectory(true);
        a.setMode(0755);
        final UnrecognizedExtraField u = new UnrecognizedExtraField();
        u.setHeaderId(ExtraFieldUtilsTest.UNRECOGNIZED_HEADER);
        u.setLocalFileDataData(ByteUtils.EMPTY_BYTE_ARRAY);
        final UnparseableExtraFieldData x = new UnparseableExtraFieldData();
        final byte[] unparseable = {
            0, 0, (byte) 0xff, (byte) 0xff, 0, 0, 0
        };
        x.parseFromLocalFileData(unparseable, 0, unparseable.length);
        return new ZipExtraField[] { a, u, x };
    }

    @Test
    public void reparsingUnicodeExtraWithUnsupportedversionThrowsInStrictMode()
        throws Exception {
        try (ZipFile zf = new ZipFile(getFile("COMPRESS-479.zip"))) {
            final ZipArchiveEntry ze = zf.getEntry("%U20AC_for_Dollar.txt");
            assertThrows(ZipException.class, () -> ze.getExtraFields(ZipArchiveEntry.ExtraFieldParsingMode.STRICT_FOR_KNOW_EXTRA_FIELDS));
        }
    }

    @Test
    public void strictForKnowExtraFieldsIncludesUnparseableExtraData() throws Exception {
        final ZipExtraField[] extraFields = parsingModeBehaviorTestData();
        final ZipArchiveEntry ze = new ZipArchiveEntry("foo");
        ze.setExtraFields(extraFields);
        final ZipExtraField[] read = ze.getExtraFields(ZipArchiveEntry.ExtraFieldParsingMode.STRICT_FOR_KNOW_EXTRA_FIELDS);
        assertEquals(extraFields.length, read.length);
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
        u.setLocalFileDataData(ByteUtils.EMPTY_BYTE_ARRAY);

        final ZipArchiveEntry ze = new ZipArchiveEntry("test/");
        ze.setExtraFields(new ZipExtraField[] {a, u});
        final byte[] data1 = ze.getExtra();

        final UnrecognizedExtraField u2 = new UnrecognizedExtraField();
        u2.setHeaderId(ExtraFieldUtilsTest.UNRECOGNIZED_HEADER);
        u2.setLocalFileDataData(new byte[] {1});

        ze.addAsFirstExtraField(u2);
        final byte[] data2 = ze.getExtra();
        ZipExtraField[] result = ze.getExtraFields();
        assertEquals(2, result.length, "second pass");
        assertSame(u2, result[0]);
        assertSame(a, result[1]);
        assertEquals(data1.length + 1, data2.length, "length second pass");

        final UnrecognizedExtraField u3 = new UnrecognizedExtraField();
        u3.setHeaderId(new ZipShort(2));
        u3.setLocalFileDataData(new byte[] {1});
        ze.addAsFirstExtraField(u3);
        result = ze.getExtraFields();
        assertEquals(3, result.length, "third pass");
        assertSame(u3, result[0]);
        assertSame(u2, result[1]);
        assertSame(a, result[2]);
    }

    /**
     * Test case for
     * <a href="https://issues.apache.org/jira/browse/COMPRESS-93"
     * >COMPRESS-93</a>.
     */
    @Test
    public void testCompressionMethod() throws Exception {
        try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(new ByteArrayOutputStream())) {
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
        }
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
     * test handling of extra fields via central directory
     */
    @Test
    public void testExtraFieldMerging() {
        final AsiExtraField a = new AsiExtraField();
        a.setDirectory(true);
        a.setMode(0755);
        final UnrecognizedExtraField u = new UnrecognizedExtraField();
        u.setHeaderId(ExtraFieldUtilsTest.UNRECOGNIZED_HEADER);
        u.setLocalFileDataData(ByteUtils.EMPTY_BYTE_ARRAY);

        final ZipArchiveEntry ze = new ZipArchiveEntry("test/");
        ze.setExtraFields(new ZipExtraField[] {a, u});

        // merge
        // Header-ID 1 + length 1 + one byte of data
        final byte[] b = ExtraFieldUtilsTest.UNRECOGNIZED_HEADER.getBytes();
        ze.setCentralDirectoryExtra(new byte[] {b[0], b[1], 1, 0, 127});

        ZipExtraField[] result = ze.getExtraFields();
        assertEquals(2, result.length, "first pass");
        assertSame(a, result[0]);
        assertEquals(ExtraFieldUtilsTest.UNRECOGNIZED_HEADER,
                     result[1].getHeaderId());
        assertEquals(new ZipShort(0), result[1].getLocalFileDataLength());
        assertEquals(new ZipShort(1), result[1].getCentralDirectoryLength());

        // add new
        // Header-ID 2 + length 0
        ze.setCentralDirectoryExtra(new byte[] {2, 0, 0, 0});

        result = ze.getExtraFields();
        assertEquals(3, result.length, "second pass");

        // merge
        // Header-ID 2 + length 1 + one byte of data
        ze.setExtra(new byte[] {2, 0, 1, 0, 127});

        result = ze.getExtraFields();
        assertEquals(3, result.length, "third pass");
        assertSame(a, result[0]);
        assertEquals(new ZipShort(2), result[2].getHeaderId());
        assertEquals(new ZipShort(1), result[2].getLocalFileDataLength());
        assertEquals(new ZipShort(0), result[2].getCentralDirectoryLength());
    }

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
        u.setLocalFileDataData(ByteUtils.EMPTY_BYTE_ARRAY);

        final ZipArchiveEntry ze = new ZipArchiveEntry("test/");
        ze.setExtraFields(new ZipExtraField[] {a, u});
        final byte[] data1 = ze.getExtra();
        ZipExtraField[] result = ze.getExtraFields();
        assertEquals(2, result.length, "first pass");
        assertSame(a, result[0]);
        assertSame(u, result[1]);

        final UnrecognizedExtraField u2 = new UnrecognizedExtraField();
        u2.setHeaderId(ExtraFieldUtilsTest.UNRECOGNIZED_HEADER);
        u2.setLocalFileDataData(new byte[] {1});

        ze.addExtraField(u2);
        final byte[] data2 = ze.getExtra();
        result = ze.getExtraFields();
        assertEquals(2, result.length, "second pass");
        assertSame(a, result[0]);
        assertSame(u2, result[1]);
        assertEquals(data1.length + 1, data2.length, "length second pass");

        final UnrecognizedExtraField u3 = new UnrecognizedExtraField();
        u3.setHeaderId(new ZipShort(2));
        u3.setLocalFileDataData(new byte[] {1});
        ze.addExtraField(u3);
        result = ze.getExtraFields();
        assertEquals(3, result.length, "third pass");

        ze.removeExtraField(ExtraFieldUtilsTest.UNRECOGNIZED_HEADER);
        final byte[] data3 = ze.getExtra();
        result = ze.getExtraFields();
        assertEquals(2, result.length, "fourth pass");
        assertSame(a, result[0]);
        assertSame(u3, result[1]);
        assertEquals(data2.length, data3.length, "length fourth pass");

        assertThrows(NoSuchElementException.class, () -> ze.removeExtraField(ExtraFieldUtilsTest.UNRECOGNIZED_HEADER),
                "should be no such element");
    }

    @Test
    public void testIsUnixSymlink() {
        final ZipArchiveEntry ze = new ZipArchiveEntry("foo");
        ze.setUnixMode(UnixStat.LINK_FLAG);
        assertTrue(ze.isUnixSymlink());
        ze.setUnixMode(UnixStat.LINK_FLAG | UnixStat.DIR_FLAG);
        assertFalse(ze.isUnixSymlink());
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
        assertNotEquals(entry1, entry2);
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
        assertNotEquals(entry1, entry3);
        assertNotEquals(entry2, entry3);
    }

    @Test
    public void testShouldNotSetExtraDateFieldsIfDateFitsInDosDates() {
        final ZipArchiveEntry ze = new ZipArchiveEntry();
        final FileTime time = FileTime.from(ZipUtilTest.toLocalInstant("2022-12-28T20:39:33.1234567"));
        ze.setTime(time);

        assertEquals(time.toMillis(), ze.getTime());
        assertEquals(time.toMillis(), ze.getLastModifiedTime().toMillis());
        assertNull(ze.getExtraField(X5455_ExtendedTimestamp.HEADER_ID));
        assertNull(ze.getExtraField(X000A_NTFS.HEADER_ID));

        final long dosTime = ZipLong.getValue(ZipUtil.toDosTime(ze.getTime()));
        ZipUtilTest.assertDosDate(dosTime, 2022, 12, 28, 20, 39, 32); // DOS dates only store even seconds
    }

    @Test
    public void testShouldNotSetInfoZipFieldIfAnyDatesExceedUnixTime() {
        final ZipArchiveEntry ze = new ZipArchiveEntry();
        final FileTime accessTime = FileTime.from(Instant.parse("2022-12-29T21:40:34.1234567Z"));
        final FileTime creationTime = FileTime.from(Instant.parse("2038-12-28T20:39:33.1234567Z"));
        final long time = Instant.parse("2020-03-04T12:34:56.1234567Z").toEpochMilli();
        ze.setTime(time);
        ze.setLastAccessTime(accessTime);
        ze.setCreationTime(creationTime);

        assertEquals(time, ze.getTime());
        assertEquals(time, ze.getLastModifiedTime().toMillis());
        assertNull(ze.getExtraField(X5455_ExtendedTimestamp.HEADER_ID));
        final X000A_NTFS ntfs = (X000A_NTFS) ze.getExtraField(X000A_NTFS.HEADER_ID);
        assertNotNull(ntfs);
        assertEquals(TimeUtils.toNtfsTime(time), ntfs.getModifyTime().getLongValue());
        assertEquals(TimeUtils.toNtfsTime(accessTime), ntfs.getAccessTime().getLongValue());
        assertEquals(TimeUtils.toNtfsTime(creationTime), ntfs.getCreateTime().getLongValue());
    }

    @Test
    public void testShouldNotSetInfoZipFieldIfDateExceedsUnixTime() {
        final ZipArchiveEntry ze = new ZipArchiveEntry();
        final FileTime time = FileTime.from(ZipUtilTest.toLocalInstant("2138-11-27T00:00:00"));
        ze.setTime(time.toMillis());

        assertEquals(time.toMillis(), ze.getTime());
        assertEquals(time, ze.getLastModifiedTime());
        assertNull(ze.getExtraField(X5455_ExtendedTimestamp.HEADER_ID));
        final X000A_NTFS ntfs = (X000A_NTFS) ze.getExtraField(X000A_NTFS.HEADER_ID);
        assertNotNull(ntfs);
        assertEquals(TimeUtils.toNtfsTime(time), ntfs.getModifyTime().getLongValue());
        assertEquals(0L, ntfs.getAccessTime().getLongValue());
        assertEquals(0L, ntfs.getCreateTime().getLongValue());
    }

    @Test
    public void testShouldSetExtraDateFieldsIfAccessDateIsSet() {
        final ZipArchiveEntry ze = new ZipArchiveEntry();
        final FileTime lastAccessTime = FileTime.from(Instant.parse("2022-12-28T20:39:33.1234567Z"));
        final long time = Instant.parse("2020-03-04T12:34:56.1234567Z").toEpochMilli();
        ze.setTime(time);
        ze.setLastAccessTime(lastAccessTime);

        assertEquals(time, ze.getTime());
        assertEquals(time, ze.getLastModifiedTime().toMillis());
        final X5455_ExtendedTimestamp extendedTimestamp = (X5455_ExtendedTimestamp) ze.getExtraField(X5455_ExtendedTimestamp.HEADER_ID);
        assertNotNull(extendedTimestamp);
        assertEquals(TimeUtils.toUnixTime(lastAccessTime), extendedTimestamp.getAccessTime().getValue());
        assertNull(extendedTimestamp.getCreateTime());
        final X000A_NTFS ntfs = (X000A_NTFS) ze.getExtraField(X000A_NTFS.HEADER_ID);
        assertNotNull(ntfs);
        assertEquals(TimeUtils.toNtfsTime(time), ntfs.getModifyTime().getLongValue());
        assertEquals(TimeUtils.toNtfsTime(lastAccessTime), ntfs.getAccessTime().getLongValue());
        assertEquals(0L, ntfs.getCreateTime().getLongValue());
    }

    @Test
    public void testShouldSetExtraDateFieldsIfAllDatesAreSet() {
        final ZipArchiveEntry ze = new ZipArchiveEntry();
        final FileTime accessTime = FileTime.from(Instant.parse("2022-12-29T21:40:34.1234567Z"));
        final FileTime creationTime = FileTime.from(Instant.parse("2022-12-28T20:39:33.1234567Z"));
        final long time = Instant.parse("2020-03-04T12:34:56.1234567Z").toEpochMilli();
        ze.setTime(time);
        ze.setLastAccessTime(accessTime);
        ze.setCreationTime(creationTime);

        assertEquals(time, ze.getTime());
        assertEquals(time, ze.getLastModifiedTime().toMillis());
        final X5455_ExtendedTimestamp extendedTimestamp = (X5455_ExtendedTimestamp) ze.getExtraField(X5455_ExtendedTimestamp.HEADER_ID);
        assertNotNull(extendedTimestamp);
        assertEquals(TimeUtils.toUnixTime(accessTime), extendedTimestamp.getAccessTime().getValue());
        assertEquals(TimeUtils.toUnixTime(creationTime), extendedTimestamp.getCreateTime().getValue());
        final X000A_NTFS ntfs = (X000A_NTFS) ze.getExtraField(X000A_NTFS.HEADER_ID);
        assertNotNull(ntfs);
        assertEquals(TimeUtils.toNtfsTime(time), ntfs.getModifyTime().getLongValue());
        assertEquals(TimeUtils.toNtfsTime(accessTime), ntfs.getAccessTime().getLongValue());
        assertEquals(TimeUtils.toNtfsTime(creationTime), ntfs.getCreateTime().getLongValue());
    }

    @Test
    public void testShouldSetExtraDateFieldsIfCreationDateIsSet() {
        final ZipArchiveEntry ze = new ZipArchiveEntry();
        final FileTime creationTime = FileTime.from(Instant.parse("2022-12-28T20:39:33.1234567Z"));
        final long time = Instant.parse("2020-03-04T12:34:56.1234567Z").toEpochMilli();
        ze.setTime(time);
        ze.setCreationTime(creationTime);

        assertEquals(time, ze.getTime());
        assertEquals(time, ze.getLastModifiedTime().toMillis());
        final X5455_ExtendedTimestamp extendedTimestamp = (X5455_ExtendedTimestamp) ze.getExtraField(X5455_ExtendedTimestamp.HEADER_ID);
        assertNotNull(extendedTimestamp);
        assertNull(extendedTimestamp.getAccessTime());
        assertEquals(TimeUtils.toUnixTime(creationTime), extendedTimestamp.getCreateTime().getValue());
        final X000A_NTFS ntfs = (X000A_NTFS) ze.getExtraField(X000A_NTFS.HEADER_ID);
        assertNotNull(ntfs);
        assertEquals(TimeUtils.toNtfsTime(time), ntfs.getModifyTime().getLongValue());
        assertEquals(0L, ntfs.getAccessTime().getLongValue());
        assertEquals(TimeUtils.toNtfsTime(creationTime), ntfs.getCreateTime().getLongValue());
    }

    @Test
    public void testShouldSetExtraDateFieldsIfDateExceedsDosDate() {
        final ZipArchiveEntry ze = new ZipArchiveEntry();
        final FileTime time = FileTime.from(ZipUtilTest.toLocalInstant("1975-11-27T00:00:00"));
        ze.setTime(time.toMillis());

        assertEquals(time.toMillis(), ze.getTime());
        assertEquals(time, ze.getLastModifiedTime());
        final X5455_ExtendedTimestamp extendedTimestamp = (X5455_ExtendedTimestamp) ze.getExtraField(X5455_ExtendedTimestamp.HEADER_ID);
        assertNotNull(extendedTimestamp);
        assertEquals(TimeUtils.toUnixTime(time), extendedTimestamp.getModifyTime().getValue());
        assertNull(extendedTimestamp.getAccessTime());
        assertNull(extendedTimestamp.getCreateTime());
        final X000A_NTFS ntfs = (X000A_NTFS) ze.getExtraField(X000A_NTFS.HEADER_ID);
        assertNotNull(ntfs);
        assertEquals(TimeUtils.toNtfsTime(time), ntfs.getModifyTime().getLongValue());
        assertEquals(0L, ntfs.getAccessTime().getLongValue());
        assertEquals(0L, ntfs.getCreateTime().getLongValue());
    }

    @Test
    public void testShouldSetExtraDateFieldsIfModifyDateIsExplicitlySet() {
        final ZipArchiveEntry ze = new ZipArchiveEntry();
        final FileTime time = FileTime.from(Instant.parse("2022-12-28T20:39:33.1234567Z"));
        ze.setLastModifiedTime(time);

        assertEquals(time.toMillis(), ze.getTime());
        assertEquals(time, ze.getLastModifiedTime());
        final X5455_ExtendedTimestamp extendedTimestamp = (X5455_ExtendedTimestamp) ze.getExtraField(X5455_ExtendedTimestamp.HEADER_ID);
        assertNotNull(extendedTimestamp);
        assertEquals(TimeUtils.toUnixTime(time), extendedTimestamp.getModifyTime().getValue());
        assertNull(extendedTimestamp.getAccessTime());
        assertNull(extendedTimestamp.getCreateTime());
        final X000A_NTFS ntfs = (X000A_NTFS) ze.getExtraField(X000A_NTFS.HEADER_ID);
        assertNotNull(ntfs);
        assertEquals(TimeUtils.toNtfsTime(time), ntfs.getModifyTime().getLongValue());
        assertEquals(0L, ntfs.getAccessTime().getLongValue());
        assertEquals(0L, ntfs.getCreateTime().getLongValue());
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

    @Test
    public void testZipArchiveClone() throws Exception {
        try (ZipFile zf = new ZipFile(getFile("COMPRESS-479.zip"))) {
            final ZipArchiveEntry ze = zf.getEntry("%U20AC_for_Dollar.txt");
            final ZipArchiveEntry clonedZe = (ZipArchiveEntry) ze.clone();
            assertEquals(ze, clonedZe);
        }
    }
}
