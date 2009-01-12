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
package org.apache.commons.compress.archivers.cpio;

import org.apache.commons.compress.archivers.ArchiveEntry;

/**
 * A cpio archive consists of a sequence of files. There are several types of
 * headers defided in two categories of new and old format. The headers are
 * recognized by magic numbers: "070701" ascii for "new" portable format
 * "070702" ascii for "new" portable format with CRC format "070707" ascii for
 * old ascii "070707" short for old binary CPIO 2.5 knows also about tar, but it
 * is not recognized here.
 * 
 * OLD FORMAT: Each file has a 76(ascii)/26(binary) byte header, a variable
 * length, NUL terminated filename, and variable length file data. A header for
 * a filename "TRAILER!!!" indicates the end of the archive.
 * 
 * All the fields in the header are ISO 646 (approximately ASCII) strings of
 * octal numbers, left padded, not NUL terminated.
 * 
 * Field Name Length in Bytes Notes ASCII / BINARY c_magic 6 / 2 c_dev 6 / 2
 * Device that contains a directory entry for this file c_ino 6 / 2 I-node
 * number that identifies the input file to the file system c_mode 6 / 2 Mode of
 * the input file c_uid 6 / 2 User ID of the owner of the input file c_gid 6 / 2
 * Group ID of the owner of the input file c_nlink 6 / 2 Number of links that
 * are connected to the input file c_rdev 6 / 2 ID of the remote device from
 * which the input file is taken only valid for chr and blk special files
 * c_mtime 11 / 4 Time when data was last modified. For remote files, this field
 * contains the time at the server c_namesize 6 / 2 Length of the path name,
 * including the terminating null byte c_filesize 11 / 4 Length of the file in
 * bytes. This is the length of the data section that follows the header
 * structure. Must be 0 for FIFOs and directories
 * 
 * Special files, directories, and the trailer are recorded with the h_filesize
 * field equal to 0.
 * 
 * NEW FORMAT: Each file has a 110 byte header, a variable length, NUL
 * terminated filename, and variable length file data. A header for a filename
 * "TRAILER!!!" indicates the end of the archive. All the fields in the header
 * are ISO 646 (approximately ASCII) strings of hexadecimal numbers, left
 * padded, not NUL terminated.
 * 
 * Field Name Length in Bytes Notes c_magic 6 c_ino 8 c_mode 8 c_uid 8 c_gid 8
 * c_nlink 8 c_mtime 8 c_filesize 8 must be 0 for FIFOs and directories c_maj 8
 * c_min 8 c_rmaj 8 only valid for chr and blk special files c_rmin 8 only valid
 * for chr and blk special files c_namesize 8 count includes terminating NUL in
 * pathname c_chksum 8 0 for "new" portable format; for CRC format the sum of
 * all the bytes in the file
 * 
 * based on code from the jRPM project (jrpm.sourceforge.net) 
 */
public class CpioArchiveEntry implements CpioConstants, ArchiveEntry {
	
    private long chksum = 0;

    private short fileFormat = -1;

    private long filesize = 0;

    private long gid = 0;

    private long headerSize = -1;

    private long inode = 0;

    private long maj = 0;

    private long min = 0;

    private long mode = -1;

    private long mtime = -1;

    private String name;

    private long nlink = 0;

    private long rmaj = 0;

    private long rmin = 0;

    private long uid = 0;

    /**
     * Ceates a CPIOArchiveEntry without a cpio format.
     */
    public CpioArchiveEntry() {
        // do nothing
    }

    /**
     * Ceates a CPIOArchiveEntry with a specified format.
     * 
     * @param format
     *            The cpio format for this entry.
     */
    public CpioArchiveEntry(final short format) {
        setFormat(format);
    }

    /**
     * Ceates a CPIOArchiveEntry with a specified name. The format of this entry will
     * be the new format.
     * 
     * @param name
     *            The name of this entry.
     */
    public CpioArchiveEntry(final String name) {
        this(FORMAT_NEW);
        this.name = name;
    }

    /**
     * Ceates a CPIOArchiveEntry with a specified name. The format of this entry will
     * be the new format.
     * 
     * @param name
     *            The name of this entry.
     * @param size
     * 			  The size of this entry
     */
    public CpioArchiveEntry(final String name, final long size) {
        this(FORMAT_NEW);
        this.name = name;
        this.setSize(size);
    }
    
    /**
     * Check if the method is allowed for the defined format.
     */
    private void checkNewFormat() {
        if ((this.fileFormat & FORMAT_NEW_MASK) == 0) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Check if the method is allowed for the defined format.
     */
    private void checkOldFormat() {
        if ((this.fileFormat & FORMAT_OLD_MASK) == 0) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Get the checksum.
     * 
     * @return Returns the checksum.
     */
    public long getChksum() {
        checkNewFormat();
        return this.chksum;
    }

    /**
     * Get the device id.
     * 
     * @return Returns the device id.
     * @throws UnsupportedOperationException
     *             if this method is called for a CPIOArchiveEntry with a new format.
     */
    public long getDevice() {
        checkOldFormat();
        return this.min;
    }

    /**
     * Get the major device id.
     * 
     * @return Returns the major device id.
     * @throws UnsupportedOperationException
     *             if this method is called for a CPIOArchiveEntry with an old format.
     */
    public long getDeviceMaj() {
        checkNewFormat();
        return this.maj;
    }

    /**
     * Get the minor device id
     * 
     * @return Returns the minor device id.
     */
    public long getDeviceMin() {
        checkNewFormat();
        return this.min;
    }

    /**
     * Get the filesize.
     * 
     * @return Returns the filesize.
     */
    /* (non-Javadoc)
     * @see org.apache.commons.compress.archivers.ArchiveEntry#getSize()
     */
    public long getSize() {
        return this.filesize;
    }

    /**
     * Get the format for this entry.
     * 
     * @return Returns the format.
     */
    public short getFormat() {
        return this.fileFormat;
    }

    /**
     * Get the group id.
     * 
     * @return Returns the group id.
     */
    public long getGID() {
        return this.gid;
    }

    /**
     * Get the size of this entry on the stream
     * 
     * @return Returns the size.
     */
    public long getHeaderSize() {
        return this.headerSize;
    }

    /**
     * Set the inode.
     * 
     * @return Returns the inode.
     */
    public long getInode() {
        return this.inode;
    }

    /**
     * Get the mode of this entry (e.g. directory, regular file).
     * 
     * @return Returns the mode.
     */
    public long getMode() {
        return this.mode;
    }

    /**
     * Get the name.
     * 
     * @return Returns the name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Get the number of links.
     * 
     * @return Returns the number of links.
     */
    public long getNumberOfLinks() {
        return this.nlink;
    }

    /**
     * Get the remote device id.
     * 
     * @return Returns the remote device id.
     * @throws UnsupportedOperationException
     *             if this method is called for a CPIOArchiveEntry with a new format.
     */
    public long getRemoteDevice() {
        checkOldFormat();
        return this.rmin;
    }

    /**
     * Get the remote major device id.
     * 
     * @return Returns the remote major device id.
     * @throws UnsupportedOperationException
     *             if this method is called for a CPIOArchiveEntry with an old format.
     */
    public long getRemoteDeviceMaj() {
        checkNewFormat();
        return this.rmaj;
    }

    /**
     * Get the remote minor device id.
     * 
     * @return Returns the remote minor device id.
     * @throws UnsupportedOperationException
     *             if this method is called for a CPIOArchiveEntry with an old format.
     */
    public long getRemoteDeviceMin() {
        checkNewFormat();
        return this.rmin;
    }

    /**
     * Get the time in seconds.
     * 
     * @return Returns the time.
     */
    public long getTime() {
        return this.mtime;
    }

    /**
     * Get the user id.
     * 
     * @return Returns the user id.
     */
    public long getUID() {
        return this.uid;
    }

    /**
     * Check if this entry represents a block device.
     * 
     * @return TRUE if this entry is a block device.
     */
    public boolean isBlockDevice() {
        return (this.mode & S_IFMT) == C_ISBLK;
    }

    /**
     * Check if this entry represents a character device.
     * 
     * @return TRUE if this entry is a character device.
     */
    public boolean isCharacterDevice() {
        return (this.mode & S_IFMT) == C_ISCHR;
    }

    /**
     * Check if this entry represents a directory.
     * 
     * @return TRUE if this entry is a directory.
     */
    public boolean isDirectory() {
        return (this.mode & S_IFMT) == C_ISDIR;
    }

    /**
     * Check if this entry represents a network device.
     * 
     * @return TRUE if this entry is a network device.
     */
    public boolean isNetwork() {
        return (this.mode & S_IFMT) == C_ISNWK;
    }

    /**
     * Check if this entry represents a pipe.
     * 
     * @return TRUE if this entry is a pipe.
     */
    public boolean isPipe() {
        return (this.mode & S_IFMT) == C_ISFIFO;
    }

    /**
     * Check if this entry represents a regular file.
     * 
     * @return TRUE if this entry is a regular file.
     */
    public boolean isRegularFile() {
        return (this.mode & S_IFMT) == C_ISREG;
    }

    /**
     * Check if this entry represents a socket.
     * 
     * @return TRUE if this entry is a socket.
     */
    public boolean isSocket() {
        return (this.mode & S_IFMT) == C_ISSOCK;
    }

    /**
     * Check if this entry represents a symbolic link.
     * 
     * @return TRUE if this entry is a symbolic link.
     */
    public boolean isSymbolicLink() {
        return (this.mode & S_IFMT) == C_ISLNK;
    }

    /**
     * Set the checksum. The checksum is calculated by adding all bytes of a
     * file to transfer (crc += buf[pos] & 0xFF).
     * 
     * @param chksum
     *            The checksum to set.
     */
    public void setChksum(final long chksum) {
        checkNewFormat();
        this.chksum = chksum;
    }

    /**
     * Set the device id.
     * 
     * @param device
     *            The device id to set.
     * @throws UnsupportedOperationException
     *             if this method is called for a CPIOArchiveEntry with a new format.
     */
    public void setDevice(final long device) {
        checkOldFormat();
        this.min = device;
    }

    /**
     * Set major device id.
     * 
     * @param maj
     *            The major device id to set.
     */
    public void setDeviceMaj(final long maj) {
        checkNewFormat();
        this.maj = maj;
    }

    /**
     * Set the minor device id
     * 
     * @param min
     *            The minor device id to set.
     */
    public void setDeviceMin(final long min) {
        checkNewFormat();
        this.min = min;
    }

    /**
     * Set the filesize.
     * 
     * @param size
     *            The filesize to set.
     */
    public void setSize(final long size) {
        if (size < 0 || size > 0xFFFFFFFFL) {
            throw new IllegalArgumentException("invalid entry size <" + size
                    + ">");
        }
        this.filesize = size;
    }

    /**
     * Set the format for this entry. Possible values are:
     * CPIOConstants.FORMAT_NEW, CPIOConstants.FORMAT_NEW_CRC,
     * CPIOConstants.FORMAT_OLD_BINARY, CPIOConstants.FORMAT_OLD_ASCII
     * 
     * @param format
     *            The format to set.
     */
    void setFormat(final short format) {
        switch (format) {
        case FORMAT_NEW:
            this.fileFormat = FORMAT_NEW;
            this.headerSize = 110;
            break;
        case FORMAT_NEW_CRC:
            this.fileFormat = FORMAT_NEW_CRC;
            this.headerSize = 110;
            break;
        case FORMAT_OLD_ASCII:
            this.fileFormat = FORMAT_OLD_ASCII;
            this.headerSize = 76;
            break;
        case FORMAT_OLD_BINARY:
            this.fileFormat = FORMAT_OLD_BINARY;
            this.headerSize = 26;
            break;
        default:
            throw new IllegalArgumentException("Unknown header type");
        }
    }

    /**
     * Set the group id.
     * 
     * @param gid
     *            The group id to set.
     */
    public void setGID(final long gid) {
        this.gid = gid;
    }

    /**
     * Set the inode.
     * 
     * @param inode
     *            The inode to set.
     */
    public void setInode(final long inode) {
        this.inode = inode;
    }

    /**
     * Set the mode of this entry (e.g. directory, regular file).
     * 
     * @param mode
     *            The mode to set.
     */
    public void setMode(final long mode) {
        switch ((int) (mode & S_IFMT)) {
        case C_ISDIR:
        case C_ISLNK:
        case C_ISREG:
        case C_ISFIFO:
        case C_ISCHR:
        case C_ISBLK:
        case C_ISSOCK:
        case C_ISNWK:
            break;
        default:
            new IllegalArgumentException("Unknown mode");
        }

        this.mode = mode;
    }

    /**
     * Set the name.
     * 
     * @param name
     *            The name to set.
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Set the number of links.
     * 
     * @param nlink
     *            The number of links to set.
     */
    public void setNumberOfLinks(final long nlink) {
        this.nlink = nlink;
    }

    /**
     * Set the remote device id.
     * 
     * @param device
     *            The remote device id to set.
     * @throws UnsupportedOperationException
     *             if this method is called for a CPIOArchiveEntry with a new format.
     */
    public void setRemoteDevice(final long device) {
        checkOldFormat();
        this.rmin = device;
    }

    /**
     * Set the remote major device id.
     * 
     * @param rmaj
     *            The remote major device id to set.
     * @throws UnsupportedOperationException
     *             if this method is called for a CPIOArchiveEntry with an old format.
     */
    public void setRemoteDeviceMaj(final long rmaj) {
        checkNewFormat();
        this.rmaj = rmaj;
    }

    /**
     * Set the remote minor device id.
     * 
     * @param rmin
     *            The remote minor device id to set.
     * @throws UnsupportedOperationException
     *             if this method is called for a CPIOArchiveEntry with an old format.
     */
    public void setRemoteDeviceMin(final long rmin) {
        checkNewFormat();
        this.rmin = rmin;
    }

    /**
     * Set the time in seconds.
     * 
     * @param time
     *            The time to set.
     */
    public void setTime(final long time) {
        this.mtime = time;
    }

    /**
     * Set the user id.
     * 
     * @param uid
     *            The user id to set.
     */
    public void setUID(final long uid) {
        this.uid = uid;
    }
}
