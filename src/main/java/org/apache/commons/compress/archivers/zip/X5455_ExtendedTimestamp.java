/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.compress.archivers.zip;

import java.io.Serializable;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.zip.ZipException;

import org.apache.commons.compress.utils.TimeUtils;

/**
 * <p>An extra field that stores additional file and directory timestamp data
 * for ZIP entries.   Each ZIP entry can include up to three timestamps
 * (modify, access, create*).  The timestamps are stored as 32 bit signed
 * integers representing seconds since UNIX epoch (Jan 1st, 1970, UTC).
 * This field improves on ZIP's default timestamp granularity, since it
 * allows one to store additional timestamps, and, in addition, the timestamps
 * are stored using per-second granularity (zip's default behavior can only store
 * timestamps to the nearest <em>even</em> second).
 * </p><p>
 * Unfortunately, 32 (signed) bits can only store dates up to the year 2037,
 * and so this extra field will eventually be obsolete.  Enjoy it while it lasts!
 * </p>
 * <ul>
 * <li><b>modifyTime:</b>
 * most recent time of file/directory modification
 * (or file/dir creation if the entry has not been
 * modified since it was created).
 * </li>
 * <li><b>accessTime:</b>
 * most recent time file/directory was opened
 * (e.g., read from disk).  Many people disable
 * their operating systems from updating this value
 * using the NOATIME mount option to optimize disk behavior,
 * and thus it's not always reliable.  In those cases
 * it's always equal to modifyTime.
 * </li>
 * <li><b>*createTime:</b>
 * modern Linux file systems (e.g., ext2 and newer)
 * do not appear to store a value like this, and so
 * it's usually omitted altogether in the ZIP extra
 * field.  Perhaps other Unix systems track this.
 * </li></ul>
 * <p>
 * We're using the field definition given in Info-Zip's source archive:
 * zip-3.0.tar.gz/proginfo/extrafld.txt
 * </p>
 * <pre>
 * Value         Size        Description
 * -----         ----        -----------
 * 0x5455        Short       tag for this extra block type ("UT")
 * TSize         Short       total data size for this block
 * Flags         Byte        info bits
 * (ModTime)     Long        time of last modification (UTC/GMT)
 * (AcTime)      Long        time of last access (UTC/GMT)
 * (CrTime)      Long        time of original creation (UTC/GMT)
 *
 * Central-header version:
 *
 * Value         Size        Description
 * -----         ----        -----------
 * 0x5455        Short       tag for this extra block type ("UT")
 * TSize         Short       total data size for this block
 * Flags         Byte        info bits (refers to local header!)
 * (ModTime)     Long        time of last modification (UTC/GMT)
 * </pre>
 * @since 1.5
 */
public class X5455_ExtendedTimestamp implements ZipExtraField, Cloneable, Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * The header ID for this extra field.
     *
     * @since 1.23
     */
    public static final ZipShort HEADER_ID = new ZipShort(0x5455);

    /**
     * The bit set inside the flags by when the last modification time
     * is present in this extra field.
     */
    public static final byte MODIFY_TIME_BIT = 1;
    /**
     * The bit set inside the flags by when the lasr access time is
     * present in this extra field.
     */
    public static final byte ACCESS_TIME_BIT = 2;
    /**
     * The bit set inside the flags by when the original creation time
     * is present in this extra field.
     */
    public static final byte CREATE_TIME_BIT = 4;

    /**
     * Utility method converts java.util.Date (milliseconds since epoch)
     * into a ZipLong (seconds since epoch).
     * <p/>
     * Also makes sure the converted ZipLong is not too big to fit
     * in 32 unsigned bits.
     *
     * @param d java.util.Date to convert to ZipLong
     * @return ZipLong
     */
    private static ZipLong dateToZipLong(final Date d) {
        if (d == null) {
            return null;
        }
        return unixTimeToZipLong(d.getTime() / 1000);
    }

    /**
     * Utility method converts {@link FileTime} into a ZipLong (seconds since epoch).
     * <p/>
     * Also makes sure the converted ZipLong is not too big to fit
     * in 32 unsigned bits.
     *
     * @param time {@link FileTime} to convert to ZipLong
     * @return ZipLong
     */
    private static ZipLong fileTimeToZipLong(final FileTime time) {
        return time == null ? null : unixTimeToZipLong(TimeUtils.toUnixTime(time));
    }

    private static FileTime unixTimeToFileTime(final ZipLong unixTime) {
        return unixTime != null ? TimeUtils.unixTimeToFileTime(unixTime.getIntValue()) : null;
    }
    // The 3 boolean fields (below) come from this flag's byte.  The remaining 5 bits
    // are ignored according to the current version of the spec (December 2012).

    private static ZipLong unixTimeToZipLong(final long unixTime) {
        if (!TimeUtils.isUnixTime(unixTime)) {
            throw new IllegalArgumentException("X5455 timestamps must fit in a signed 32 bit integer: " + unixTime);
        }
        return new ZipLong(unixTime);
    }

    private static Date zipLongToDate(final ZipLong unixTime) {
        return unixTime != null ? new Date(unixTime.getIntValue() * 1000L) : null;
    }

    private byte flags;
    // Note: even if bit1 and bit2 are set, the Central data will still not contain
    // access/create fields:  only local data ever holds those!  This causes
    // some of our implementation to look a little odd, with seemingly spurious
    // != null and length checks.
    private boolean bit0_modifyTimePresent;
    private boolean bit1_accessTimePresent;

    private boolean bit2_createTimePresent;

    private ZipLong modifyTime;

    private ZipLong accessTime;

    private ZipLong createTime;

    /**
     * Constructor for X5455_ExtendedTimestamp.
     */
    public X5455_ExtendedTimestamp() {}

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof X5455_ExtendedTimestamp) {
            final X5455_ExtendedTimestamp xf = (X5455_ExtendedTimestamp) o;

            // The ZipLong==ZipLong clauses handle the cases where both are null.
            // and only last 3 bits of flags matter.
            return ((flags & 0x07) == (xf.flags & 0x07)) &&
                    Objects.equals(modifyTime, xf.modifyTime) &&
                    Objects.equals(accessTime, xf.accessTime) &&
                    Objects.equals(createTime, xf.createTime);
        }
        return false;
    }

    /**
     * Gets the access time as a {@link FileTime}
     * of this ZIP entry, or null if no such timestamp exists in the ZIP entry.
     * The milliseconds are always zeroed out, since the underlying data
     * offers only per-second precision.
     *
     * @return modify time as {@link FileTime} or null.
     * @since 1.23
     */
    public FileTime getAccessFileTime() {
        return unixTimeToFileTime(accessTime);
    }

    /**
     * Gets the access time as a java.util.Date
     * of this ZIP entry, or null if no such timestamp exists in the ZIP entry.
     * The milliseconds are always zeroed out, since the underlying data
     * offers only per-second precision.
     *
     * @return access time as java.util.Date or null.
     */
    public Date getAccessJavaTime() {
        return zipLongToDate(accessTime);
    }

    /**
     * Gets the access time (seconds since epoch) of this ZIP entry
     * as a ZipLong object, or null if no such timestamp exists in the
     * ZIP entry.
     *
     * @return access time (seconds since epoch) or null.
     */
    public ZipLong getAccessTime() {
        return accessTime;
    }

    /**
     * Gets the actual data to put into central directory data - without Header-ID
     * or length specifier.
     *
     * @return the central directory data
     */
    @Override
    public byte[] getCentralDirectoryData() {
        // Truncate out create & access time (last 8 bytes) from
        // the copy of the local data we obtained:
        return Arrays.copyOf(getLocalFileDataData(), getCentralDirectoryLength().getValue());
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
        return new ZipShort(1 + (bit0_modifyTimePresent ? 4 : 0));
    }

    /**
     * Gets the create time as a {@link FileTime}
     * of this ZIP entry, or null if no such timestamp exists in the ZIP entry.
     * The milliseconds are always zeroed out, since the underlying data
     * offers only per-second precision.
     *
     * @return modify time as {@link FileTime} or null.
     * @since 1.23
     */
    public FileTime getCreateFileTime() {
        return unixTimeToFileTime(createTime);
    }

    /**
     * <p>
     * Gets the create time as a java.util.Date
     * of this ZIP entry, or null if no such timestamp exists in the ZIP entry.
     * The milliseconds are always zeroed out, since the underlying data
     * offers only per-second precision.
     * </p>
     * <p>
     * Note: modern Linux file systems (e.g., ext2)
     * do not appear to store a "create time" value, and so
     * it's usually omitted altogether in the ZIP extra
     * field. Perhaps other Unix systems track this.
     * </p>
     *
     * @return create time as java.util.Date or null.
     */
    public Date getCreateJavaTime() {
        return zipLongToDate(createTime);
    }

    /**
     * <p>
     * Gets the create time (seconds since epoch) of this ZIP entry
     * as a ZipLong object, or null if no such timestamp exists in the
     * ZIP entry.
     * </p>
     * <p>
     * Note: modern Linux file systems (e.g., ext2)
     * do not appear to store a "create time" value, and so
     * it's usually omitted altogether in the ZIP extra
     * field. Perhaps other Unix systems track this.
     * </p>
     *
     * @return create time (seconds since epoch) or null.
     */
    public ZipLong getCreateTime() {
        return createTime;
    }

    /**
     * Gets flags byte.  The flags byte tells us which of the
     * three datestamp fields are present in the data:
     * <pre>
     * bit0 - modify time
     * bit1 - access time
     * bit2 - create time
     * </pre>
     * Only first 3 bits of flags are used according to the
     * latest version of the spec (December 2012).
     *
     * @return flags byte indicating which of the
     *         three datestamp fields are present.
     */
    public byte getFlags() { return flags; }

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
        int pos = 0;
        data[pos++] = 0;
        if (bit0_modifyTimePresent) {
            data[0] |= MODIFY_TIME_BIT;
            System.arraycopy(modifyTime.getBytes(), 0, data, pos, 4);
            pos += 4;
        }
        if (bit1_accessTimePresent && accessTime != null) {
            data[0] |= ACCESS_TIME_BIT;
            System.arraycopy(accessTime.getBytes(), 0, data, pos, 4);
            pos += 4;
        }
        if (bit2_createTimePresent && createTime != null) {
            data[0] |= CREATE_TIME_BIT;
            System.arraycopy(createTime.getBytes(), 0, data, pos, 4);
            pos += 4; // NOSONAR - assignment as documentation
        }
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
        return new ZipShort(1 +
                (bit0_modifyTimePresent ? 4 : 0) +
                (bit1_accessTimePresent && accessTime != null ? 4 : 0) +
                (bit2_createTimePresent && createTime != null ? 4 : 0)
        );
    }

    /**
     * Gets the modify time as a {@link FileTime}
     * of this ZIP entry, or null if no such timestamp exists in the ZIP entry.
     * The milliseconds are always zeroed out, since the underlying data
     * offers only per-second precision.
     *
     * @return modify time as {@link FileTime} or null.
     * @since 1.23
     */
    public FileTime getModifyFileTime() {
        return unixTimeToFileTime(modifyTime);
    }

    /**
     * Gets the modify time as a java.util.Date
     * of this ZIP entry, or null if no such timestamp exists in the ZIP entry.
     * The milliseconds are always zeroed out, since the underlying data
     * offers only per-second precision.
     *
     * @return modify time as java.util.Date or null.
     */
    public Date getModifyJavaTime() {
        return zipLongToDate(modifyTime);
    }

    /**
     * Gets the modify time (seconds since epoch) of this ZIP entry
     * as a ZipLong object, or null if no such timestamp exists in the
     * ZIP entry.
     *
     * @return modify time (seconds since epoch) or null.
     */
    public ZipLong getModifyTime() { return modifyTime; }

    @Override
    public int hashCode() {
        int hc = (-123 * (flags & 0x07)); // only last 3 bits of flags matter
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
     * Tests whether bit0 of the flags byte is set or not,
     * which should correspond to the presence or absence of
     * a modify timestamp in this particular ZIP entry.
     *
     * @return true if bit0 of the flags byte is set.
     */
    public boolean isBit0_modifyTimePresent() {
        return bit0_modifyTimePresent;
    }

    /**
     * Tests whether bit1 of the flags byte is set or not,
     * which should correspond to the presence or absence of
     * a "last access" timestamp in this particular ZIP entry.
     *
     * @return true if bit1 of the flags byte is set.
     */
    public boolean isBit1_accessTimePresent() {
        return bit1_accessTimePresent;
    }

    /**
     * Tests whether bit2 of the flags byte is set or not,
     * which should correspond to the presence or absence of
     * a create timestamp in this particular ZIP entry.
     *
     * @return true if bit2 of the flags byte is set.
     */
    public boolean isBit2_createTimePresent() {
        return bit2_createTimePresent;
    }

    /**
     * Doesn't do anything special since this class always uses the
     * same parsing logic for both central directory and local file data.
     */
    @Override
    public void parseFromCentralDirectoryData(final byte[] buffer, final int offset, final int length) throws ZipException {
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
    public void parseFromLocalFileData(final byte[] data, int offset, final int length) throws ZipException {
        reset();
        if (length < 1) {
            throw new ZipException("X5455_ExtendedTimestamp too short, only " + length + " bytes");
        }
        final int len = offset + length;
        setFlags(data[offset++]);
        if (bit0_modifyTimePresent && offset + 4 <= len) {
            modifyTime = new ZipLong(data, offset);
            offset += 4;
        } else {
            bit0_modifyTimePresent = false;
        }
        if (bit1_accessTimePresent && offset + 4 <= len) {
            accessTime = new ZipLong(data, offset);
            offset += 4;
        } else {
            bit1_accessTimePresent = false;
        }
        if (bit2_createTimePresent && offset + 4 <= len) {
            createTime = new ZipLong(data, offset);
            offset += 4; // NOSONAR - assignment as documentation
        } else {
            bit2_createTimePresent = false;
        }
    }

    /**
     * Reset state back to newly constructed state.  Helps us make sure
     * parse() calls always generate clean results.
     */
    private void reset() {
        setFlags((byte) 0);
        this.modifyTime = null;
        this.accessTime = null;
        this.createTime = null;
    }

    /**
     * <p>
     * Sets the acccess time as a {@link FileTime}
     * of this ZIP entry. Supplied value is truncated to per-second
     * precision (milliseconds zeroed-out).
     * </p><p>
     * Note: the setters for flags and timestamps are decoupled.
     * Even if the timestamp is not-null, it will only be written
     * out if the corresponding bit in the flags is also set.
     * </p>
     *
     * @param time access time as {@link FileTime}
     * @since 1.23
     */
    public void setAccessFileTime(final FileTime time) {
        setAccessTime(fileTimeToZipLong(time));
    }

    /**
     * <p>
     * Sets the access time as a java.util.Date
     * of this ZIP entry.  Supplied value is truncated to per-second
     * precision (milliseconds zeroed-out).
     * </p><p>
     * Note: the setters for flags and timestamps are decoupled.
     * Even if the timestamp is not-null, it will only be written
     * out if the corresponding bit in the flags is also set.
     * </p>
     *
     * @param d access time as java.util.Date
     */
    public void setAccessJavaTime(final Date d) {
        setAccessTime(dateToZipLong(d));
    }

    /**
     * <p>
     * Sets the access time (seconds since epoch) of this ZIP entry
     * using a ZipLong object
     * </p><p>
     * Note: the setters for flags and timestamps are decoupled.
     * Even if the timestamp is not-null, it will only be written
     * out if the corresponding bit in the flags is also set.
     * </p>
     *
     * @param l ZipLong of the access time (seconds per epoch)
     */
    public void setAccessTime(final ZipLong l) {
        bit1_accessTimePresent = l != null;
        flags = (byte) (l != null ? (flags | ACCESS_TIME_BIT) : (flags & ~ACCESS_TIME_BIT));
        this.accessTime = l;
    }

    /**
     * <p>
     * Sets the create time as a {@link FileTime}
     * of this ZIP entry. Supplied value is truncated to per-second
     * precision (milliseconds zeroed-out).
     * </p><p>
     * Note: the setters for flags and timestamps are decoupled.
     * Even if the timestamp is not-null, it will only be written
     * out if the corresponding bit in the flags is also set.
     * </p>
     *
     * @param time create time as {@link FileTime}
     * @since 1.23
     */
    public void setCreateFileTime(final FileTime time) {
        setCreateTime(fileTimeToZipLong(time));
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
    public void setCreateJavaTime(final Date d) { setCreateTime(dateToZipLong(d)); }

    /**
     * <p>
     * Sets the create time (seconds since epoch) of this ZIP entry
     * using a ZipLong object
     * </p><p>
     * Note: the setters for flags and timestamps are decoupled.
     * Even if the timestamp is not-null, it will only be written
     * out if the corresponding bit in the flags is also set.
     * </p>
     *
     * @param l ZipLong of the create time (seconds per epoch)
     */
    public void setCreateTime(final ZipLong l) {
        bit2_createTimePresent = l != null;
        flags = (byte) (l != null ? (flags | CREATE_TIME_BIT) : (flags & ~CREATE_TIME_BIT));
        this.createTime = l;
    }

    /**
     * Sets flags byte.  The flags byte tells us which of the
     * three datestamp fields are present in the data:
     * <pre>
     * bit0 - modify time
     * bit1 - access time
     * bit2 - create time
     * </pre>
     * Only first 3 bits of flags are used according to the
     * latest version of the spec (December 2012).
     *
     * @param flags flags byte indicating which of the
     *              three datestamp fields are present.
     */
    public void setFlags(final byte flags) {
        this.flags = flags;
        this.bit0_modifyTimePresent = (flags & MODIFY_TIME_BIT) == MODIFY_TIME_BIT;
        this.bit1_accessTimePresent = (flags & ACCESS_TIME_BIT) == ACCESS_TIME_BIT;
        this.bit2_createTimePresent = (flags & CREATE_TIME_BIT) == CREATE_TIME_BIT;
    }

    /**
     * <p>
     * Sets the modify time as a {@link FileTime}
     * of this ZIP entry. Supplied value is truncated to per-second
     * precision (milliseconds zeroed-out).
     * </p><p>
     * Note: the setters for flags and timestamps are decoupled.
     * Even if the timestamp is not-null, it will only be written
     * out if the corresponding bit in the flags is also set.
     * </p>
     *
     * @param time modify time as {@link FileTime}
     * @since 1.23
     */
    public void setModifyFileTime(final FileTime time) {
        setModifyTime(fileTimeToZipLong(time));
    }

    /**
     * <p>
     * Sets the modify time as a java.util.Date
     * of this ZIP entry.  Supplied value is truncated to per-second
     * precision (milliseconds zeroed-out).
     * </p><p>
     * Note: the setters for flags and timestamps are decoupled.
     * Even if the timestamp is not-null, it will only be written
     * out if the corresponding bit in the flags is also set.
     * </p>
     *
     * @param d modify time as java.util.Date
     */
    public void setModifyJavaTime(final Date d) {
        setModifyTime(dateToZipLong(d));
    }

    /**
     * <p>
     * Sets the modify time (seconds since epoch) of this ZIP entry
     * using a ZipLong object.
     * </p><p>
     * Note: the setters for flags and timestamps are decoupled.
     * Even if the timestamp is not-null, it will only be written
     * out if the corresponding bit in the flags is also set.
     * </p>
     *
     * @param l ZipLong of the modify time (seconds per epoch)
     */
    public void setModifyTime(final ZipLong l) {
        bit0_modifyTimePresent = l != null;
        flags = (byte) (l != null ? (flags | MODIFY_TIME_BIT) : (flags & ~MODIFY_TIME_BIT));
        this.modifyTime = l;
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
        buf.append("0x5455 Zip Extra Field: Flags=");
        buf.append(Integer.toBinaryString(ZipUtil.unsignedIntToSignedByte(flags))).append(" ");
        if (bit0_modifyTimePresent && modifyTime != null) {
            final Date m = getModifyJavaTime();
            buf.append(" Modify:[").append(m).append("] ");
        }
        if (bit1_accessTimePresent && accessTime != null) {
            final Date a = getAccessJavaTime();
            buf.append(" Access:[").append(a).append("] ");
        }
        if (bit2_createTimePresent && createTime != null) {
            final Date c = getCreateJavaTime();
            buf.append(" Create:[").append(c).append("] ");
        }
        return buf.toString();
    }

}
