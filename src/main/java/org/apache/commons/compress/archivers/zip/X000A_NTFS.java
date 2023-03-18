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

import java.nio.file.attribute.FileTime;
import java.util.Date;
import java.util.Objects;
import java.util.zip.ZipException;

import org.apache.commons.compress.utils.TimeUtils;

/**
 * NTFS extra field that was thought to store various attributes but
 * in reality only stores timestamps.
 *
 * <pre>
 *    4.5.5 -NTFS Extra Field (0x000a):
 *
 *       The following is the layout of the NTFS attributes
 *       "extra" block. (Note: At this time the Mtime, Atime
 *       and Ctime values MAY be used on any WIN32 system.)
 *
 *       Note: all fields stored in Intel low-byte/high-byte order.
 *
 *         Value      Size       Description
 *         -----      ----       -----------
 * (NTFS)  0x000a     2 bytes    Tag for this "extra" block type
 *         TSize      2 bytes    Size of the total "extra" block
 *         Reserved   4 bytes    Reserved for future use
 *         Tag1       2 bytes    NTFS attribute tag value #1
 *         Size1      2 bytes    Size of attribute #1, in bytes
 *         (var)      Size1      Attribute #1 data
 *          .
 *          .
 *          .
 *          TagN       2 bytes    NTFS attribute tag value #N
 *          SizeN      2 bytes    Size of attribute #N, in bytes
 *          (var)      SizeN      Attribute #N data
 *
 *        For NTFS, values for Tag1 through TagN are as follows:
 *        (currently only one set of attributes is defined for NTFS)
 *
 *          Tag        Size       Description
 *          -----      ----       -----------
 *          0x0001     2 bytes    Tag for attribute #1
 *          Size1      2 bytes    Size of attribute #1, in bytes
 *          Mtime      8 bytes    File last modification time
 *          Atime      8 bytes    File last access time
 *          Ctime      8 bytes    File creation time
 * </pre>
 *
 * @since 1.11
 * @NotThreadSafe
 */
public class X000A_NTFS implements ZipExtraField {

    /**
     * The header ID for this extra field.
     *
     * @since 1.23
     */
    public static final ZipShort HEADER_ID = new ZipShort(0x000a);

    private static final ZipShort TIME_ATTR_TAG = new ZipShort(0x0001);
    private static final ZipShort TIME_ATTR_SIZE = new ZipShort(3 * 8);

    private static ZipEightByteInteger dateToZip(final Date d) {
        if (d == null) {
            return null;
        }
        return new ZipEightByteInteger(TimeUtils.toNtfsTime(d));
    }
    private static ZipEightByteInteger fileTimeToZip(final FileTime time) {
        if (time == null) {
            return null;
        }
        return new ZipEightByteInteger(TimeUtils.toNtfsTime(time));
    }
    private static Date zipToDate(final ZipEightByteInteger z) {
        if (z == null || ZipEightByteInteger.ZERO.equals(z)) {
            return null;
        }
        return TimeUtils.ntfsTimeToDate(z.getLongValue());
    }

    private static FileTime zipToFileTime(final ZipEightByteInteger z) {
        if (z == null || ZipEightByteInteger.ZERO.equals(z)) {
            return null;
        }
        return TimeUtils.ntfsTimeToFileTime(z.getLongValue());
    }

    private ZipEightByteInteger modifyTime = ZipEightByteInteger.ZERO;

    private ZipEightByteInteger accessTime = ZipEightByteInteger.ZERO;

    private ZipEightByteInteger createTime = ZipEightByteInteger.ZERO;

    @Override
    public boolean equals(final Object o) {
        if (o instanceof X000A_NTFS) {
            final X000A_NTFS xf = (X000A_NTFS) o;

            return Objects.equals(modifyTime, xf.modifyTime) &&
                   Objects.equals(accessTime, xf.accessTime) &&
                   Objects.equals(createTime, xf.createTime);
        }
        return false;
    }

    /**
     * Gets the access time as a {@link FileTime}
     * of this ZIP entry, or null if no such timestamp exists in the ZIP entry.
     *
     * @return access time as a {@link FileTime} or null.
     * @since 1.23
     */
    public FileTime getAccessFileTime() {
        return zipToFileTime(accessTime);
    }

    /**
     * Gets the access time as a java.util.Date
     * of this ZIP entry, or null if no such timestamp exists in the ZIP entry.
     *
     * @return access time as java.util.Date or null.
     */
    public Date getAccessJavaTime() {
        return zipToDate(accessTime);
    }

    /**
     * Gets the "File last access time" of this ZIP entry as a
     * ZipEightByteInteger object, or {@link ZipEightByteInteger#ZERO}
     * if no such timestamp exists in the ZIP entry.
     *
     * @return File last access time
     */
    public ZipEightByteInteger getAccessTime() { return accessTime; }

    /**
     * Gets the actual data to put into central directory data - without Header-ID
     * or length specifier.
     *
     * @return the central directory data
     */
    @Override
    public byte[] getCentralDirectoryData() {
        return getLocalFileDataData();
    }

    /**
     * Gets the length of the extra field in the local file data - without
     * Header-ID or length specifier.
     *
     * <p>For X5455 the central length is often smaller than the
     * local length, because central cannot contain access or create
     * timestamps.</p>
     *
     * @return a {@code ZipShort} for the length of the data of this extra field
     */
    @Override
    public ZipShort getCentralDirectoryLength() {
        return getLocalFileDataLength();
    }

    /**
     * Gets the create time as a {@link FileTime}
     * of this ZIP entry, or null if no such timestamp exists in the ZIP entry.
     *
     * @return create time as a {@link FileTime} or null.
     * @since 1.23
     */
    public FileTime getCreateFileTime() {
        return zipToFileTime(createTime);
    }

    /**
     * Gets the create time as a java.util.Date of this ZIP
     * entry, or null if no such timestamp exists in the ZIP entry.
     *
     * @return create time as java.util.Date or null.
     */
    public Date getCreateJavaTime() {
        return zipToDate(createTime);
    }

    /**
     * Gets the "File creation time" of this ZIP entry as a
     * ZipEightByteInteger object, or {@link ZipEightByteInteger#ZERO}
     * if no such timestamp exists in the ZIP entry.
     *
     * @return File creation time
     */
    public ZipEightByteInteger getCreateTime() { return createTime; }

    /**
     * Gets the Header-ID.
     *
     * @return the value for the header id for this extrafield
     */
    @Override
    public ZipShort getHeaderId() {
        return HEADER_ID;
    }

    /**
     * Gets the actual data to put into local file data - without Header-ID
     * or length specifier.
     *
     * @return get the data
     */
    @Override
    public byte[] getLocalFileDataData() {
        final byte[] data = new byte[getLocalFileDataLength().getValue()];
        int pos = 4;
        System.arraycopy(TIME_ATTR_TAG.getBytes(), 0, data, pos, 2);
        pos += 2;
        System.arraycopy(TIME_ATTR_SIZE.getBytes(), 0, data, pos, 2);
        pos += 2;
        System.arraycopy(modifyTime.getBytes(), 0, data, pos, 8);
        pos += 8;
        System.arraycopy(accessTime.getBytes(), 0, data, pos, 8);
        pos += 8;
        System.arraycopy(createTime.getBytes(), 0, data, pos, 8);
        return data;
    }

    /**
     * Gets the length of the extra field in the local file data - without
     * Header-ID or length specifier.
     *
     * @return a {@code ZipShort} for the length of the data of this extra field
     */
    @Override
    public ZipShort getLocalFileDataLength() {
        return new ZipShort(4 /* reserved */
                            + 2 /* Tag#1 */
                            + 2 /* Size#1 */
                            + 3 * 8 /* time values */);
    }

    /**
     * Gets the modify time as a {@link FileTime}
     * of this ZIP entry, or null if no such timestamp exists in the ZIP entry.
     *
     * @return modify time as a {@link FileTime} or null.
     * @since 1.23
     */
    public FileTime getModifyFileTime() {
        return zipToFileTime(modifyTime);
    }

    /**
     * Gets the modify time as a java.util.Date
     * of this ZIP entry, or null if no such timestamp exists in the ZIP entry.
     *
     * @return modify time as java.util.Date or null.
     */
    public Date getModifyJavaTime() {
        return zipToDate(modifyTime);
    }

    /**
     * Gets the "File last modification time" of this ZIP entry as
     * a ZipEightByteInteger object, or {@link
     * ZipEightByteInteger#ZERO} if no such timestamp exists in the
     * ZIP entry.
     *
     * @return File last modification time
     */
    public ZipEightByteInteger getModifyTime() { return modifyTime; }

    @Override
    public int hashCode() {
        int hc = -123;
        if (modifyTime != null) {
            hc ^= modifyTime.hashCode();
        }
        if (accessTime != null) {
            // Since accessTime is often same as modifyTime,
            // this prevents them from XOR negating each other.
            hc ^= Integer.rotateLeft(accessTime.hashCode(), 11);
        }
        if (createTime != null) {
            hc ^= Integer.rotateLeft(createTime.hashCode(), 22);
        }
        return hc;
    }

    /**
     * Doesn't do anything special since this class always uses the
     * same parsing logic for both central directory and local file data.
     */
    @Override
    public void parseFromCentralDirectoryData(
            final byte[] buffer, final int offset, final int length
    ) throws ZipException {
        reset();
        parseFromLocalFileData(buffer, offset, length);
    }

    /**
     * Populate data from this array as if it was in local file data.
     *
     * @param data   an array of bytes
     * @param offset the start offset
     * @param length the number of bytes in the array from offset
     * @throws java.util.zip.ZipException on error
     */
    @Override
    public void parseFromLocalFileData(
            final byte[] data, int offset, final int length
    ) throws ZipException {
        final int len = offset + length;

        // skip reserved
        offset += 4;

        while (offset + 4 <= len) {
            final ZipShort tag = new ZipShort(data, offset);
            offset += 2;
            if (tag.equals(TIME_ATTR_TAG)) {
                readTimeAttr(data, offset, len - offset);
                break;
            }
            final ZipShort size = new ZipShort(data, offset);
            offset += 2 + size.getValue();
        }
    }

    private void readTimeAttr(final byte[] data, int offset, final int length) {
        if (length >= 2 + 3 * 8) {
            final ZipShort tagValueLength = new ZipShort(data, offset);
            if (TIME_ATTR_SIZE.equals(tagValueLength)) {
                offset += 2;
                modifyTime = new ZipEightByteInteger(data, offset);
                offset += 8;
                accessTime = new ZipEightByteInteger(data, offset);
                offset += 8;
                createTime = new ZipEightByteInteger(data, offset);
            }
        }
    }

    /**
     * Reset state back to newly constructed state.  Helps us make sure
     * parse() calls always generate clean results.
     */
    private void reset() {
        this.modifyTime = ZipEightByteInteger.ZERO;
        this.accessTime = ZipEightByteInteger.ZERO;
        this.createTime = ZipEightByteInteger.ZERO;
    }

    /**
     * Sets the access time.
     *
     * @param time access time as a {@link FileTime}
     * @since 1.23
     */
    public void setAccessFileTime(final FileTime time) {
        setAccessTime(fileTimeToZip(time));
    }

    /**
     * Sets the access time as a java.util.Date
     * of this ZIP entry.
     *
     * @param d access time as java.util.Date
     */
    public void setAccessJavaTime(final Date d) { setAccessTime(dateToZip(d)); }

    /**
     * Sets the File last access time of this ZIP entry using a
     * ZipEightByteInteger object.
     *
     * @param t ZipEightByteInteger of the access time
     */
    public void setAccessTime(final ZipEightByteInteger t) {
        accessTime = t == null ? ZipEightByteInteger.ZERO : t;
    }

    /**
     * Sets the create time.
     *
     * @param time create time as a {@link FileTime}
     * @since 1.23
     */
    public void setCreateFileTime(final FileTime time) {
        setCreateTime(fileTimeToZip(time));
    }

    /**
     * <p>
     * Sets the create time as a java.util.Date
     * of this ZIP entry.  Supplied value is truncated to per-second
     * precision (milliseconds zeroed-out).
     * </p><p>
     * Note: the setters for flags and timestamps are decoupled.
     * Even if the timestamp is not-null, it will only be written
     * out if the corresponding bit in the flags is also set.
     * </p>
     *
     * @param d create time as java.util.Date
     */
    public void setCreateJavaTime(final Date d) { setCreateTime(dateToZip(d)); }

    /**
     * Sets the File creation time of this ZIP entry using a
     * ZipEightByteInteger object.
     *
     * @param t ZipEightByteInteger of the create time
     */
    public void setCreateTime(final ZipEightByteInteger t) {
        createTime = t == null ? ZipEightByteInteger.ZERO : t;
    }

    /**
     * Sets the modify time.
     *
     * @param time modify time as a {@link FileTime}
     * @since 1.23
     */
    public void setModifyFileTime(final FileTime time) {
        setModifyTime(fileTimeToZip(time));
    }

    /**
     * Sets the modify time as a java.util.Date of this ZIP entry.
     *
     * @param d modify time as java.util.Date
     */
    public void setModifyJavaTime(final Date d) { setModifyTime(dateToZip(d)); }

    /**
     * Sets the File last modification time of this ZIP entry using a
     * ZipEightByteInteger object.
     *
     * @param t ZipEightByteInteger of the modify time
     */
    public void setModifyTime(final ZipEightByteInteger t) {
        modifyTime = t == null ? ZipEightByteInteger.ZERO : t;
    }

    /**
     * Returns a String representation of this class useful for
     * debugging purposes.
     *
     * @return A String representation of this class useful for
     *         debugging purposes.
     */
    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append("0x000A Zip Extra Field:")
            .append(" Modify:[").append(getModifyFileTime()).append("] ")
            .append(" Access:[").append(getAccessFileTime()).append("] ")
            .append(" Create:[").append(getCreateFileTime()).append("] ");
        return buf.toString();
    }
}
