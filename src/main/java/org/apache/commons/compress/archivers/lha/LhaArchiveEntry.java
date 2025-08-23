/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.commons.compress.archivers.lha;

import java.time.ZoneOffset;
import java.util.Date;

import org.apache.commons.compress.archivers.ArchiveEntry;

/**
 * Represents an entry in a LHA archive.
 *
 * @since 1.29.0
 */
public class LhaArchiveEntry implements ArchiveEntry {
    private String name;
    private boolean directory;
    private long size;
    private Date lastModifiedDate;
    private long compressedSize;
    private String compressionMethod;
    private int crcValue;
    private Integer osId;
    private Integer unixPermissionMode;
    private Integer unixUserId;
    private Integer unixGroupId;
    private Integer msdosFileAttributes;
    private Integer headerCrc;

    public LhaArchiveEntry() {
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer().append("LhaArchiveEntry[")
                .append("name=").append(name)
                .append(",directory=").append(directory)
                .append(",size=").append(size)
                .append(",lastModifiedDate=").append(lastModifiedDate == null ? "" : lastModifiedDate.toInstant().atZone(ZoneOffset.UTC).toString())
                .append(",compressedSize=").append(compressedSize)
                .append(",compressionMethod=").append(compressionMethod)
                .append(",crcValue=").append(String.format("0x%04x", crcValue));

        if (osId != null) {
            sb.append(",osId=").append(osId);
        }

        if (unixPermissionMode != null) {
            sb.append(",unixPermissionMode=").append(String.format("%03o", unixPermissionMode));
        }

        if (msdosFileAttributes != null) {
            sb.append(",msdosFileAttributes=").append(String.format("%04x", msdosFileAttributes));
        }

        if (headerCrc != null) {
            sb.append(",headerCrc=").append(String.format("0x%04x", headerCrc));
        }

        return sb.append("]").toString();
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    @Override
    public Date getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Date lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    /**
     * Returns the compressed size of this entry.
     *
     * @return the compressed size
     */
    public long getCompressedSize() {
        return compressedSize;
    }

    public void setCompressedSize(long compressedSize) {
        this.compressedSize = compressedSize;
    }

    public void setDirectory(boolean directory) {
        this.directory = directory;
    }

    @Override
    public boolean isDirectory() {
        return directory;
    }

    /**
     * Returns the compression method of this entry.
     *
     * @return the compression method
     */
    public String getCompressionMethod() {
        return compressionMethod;
    }

    public void setCompressionMethod(String compressionMethod) {
        this.compressionMethod = compressionMethod;
    }

    /**
     * Returns the CRC-16 checksum of the uncompressed data of this entry.
     *
     * @return CRC-16 checksum of the uncompressed data
     */
    public int getCrcValue() {
        return crcValue;
    }

    public void setCrcValue(int crc) {
        this.crcValue = crc;
    }

    /**
     * Returns the operating system id if available for this entry.
     *
     * @return operating system id if available
     */
    public Integer getOsId() {
        return osId;
    }

    public void setOsId(Integer osId) {
        this.osId = osId;
    }

    public Integer getUnixPermissionMode() {
        return unixPermissionMode;
    }

    public void setUnixPermissionMode(Integer unixPermissionMode) {
        this.unixPermissionMode = unixPermissionMode;
    }

    public Integer getUnixUserId() {
        return unixUserId;
    }

    public void setUnixUserId(Integer unixUserId) {
        this.unixUserId = unixUserId;
    }

    public Integer getUnixGroupId() {
        return unixGroupId;
    }

    public void setUnixGroupId(Integer unixGroupId) {
        this.unixGroupId = unixGroupId;
    }

    /**
     * Returns the MS-DOS file attributes if available for this entry.
     *
     * @return MS-DOS file attributes if available
     */
    public Integer getMsdosFileAttributes() {
        return msdosFileAttributes;
    }

    public void setMsdosFileAttributes(Integer msdosFileAttributes) {
        this.msdosFileAttributes = msdosFileAttributes;
    }

    /**
     * Don't expose the header CRC publicly, as it is of no interest to most users.
     */
    Integer getHeaderCrc() {
        return headerCrc;
    }

    void setHeaderCrc(Integer headerCrc) {
        this.headerCrc = headerCrc;
    }
}
