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
import java.util.Objects;

import org.apache.commons.compress.archivers.ArchiveEntry;

/**
 * An entry in a LHA archive.
 *
 * @since 1.29.0
 */
public class LhaArchiveEntry implements ArchiveEntry {

    static class Builder {

        private String fileName;

        private String directoryName;

        private boolean directory;

        private long size;

        private Date lastModifiedDate;

        private long compressedSize;

        private String compressionMethod;

        private int crc;

        private Integer osId;

        private Integer unixPermissionMode;

        private Integer unixUserId;

        private Integer unixGroupId;

        private Integer msdosFileAttributes;

        private Integer headerCrc;

        Builder() {
        }

        LhaArchiveEntry get() {
            return new LhaArchiveEntry(this);
        }

        Builder setCompressedSize(final long compressedSize) {
            this.compressedSize = compressedSize;
            return this;
        }

        Builder setCompressionMethod(final String compressionMethod) {
            this.compressionMethod = compressionMethod;
            return this;
        }

        Builder setCrc(final int crc) {
            this.crc = crc;
            return this;
        }

        Builder setDirectory(final boolean directory) {
            this.directory = directory;
            return this;
        }

        Builder setDirectoryName(final String directoryName) {
            this.directoryName = directoryName;
            return this;
        }

        Builder setFileName(final String fileName) {
            this.fileName = fileName;
            return this;
        }

        Builder setHeaderCrc(final Integer headerCrc) {
            this.headerCrc = headerCrc;
            return this;
        }

        Builder setLastModifiedDate(final Date lastModifiedDate) {
            this.lastModifiedDate = lastModifiedDate;
            return this;
        }

        Builder setMsdosFileAttributes(final Integer msdosFileAttributes) {
            this.msdosFileAttributes = msdosFileAttributes;
            return this;
        }

        Builder setOsId(final Integer osId) {
            this.osId = osId;
            return this;
        }

        Builder setSize(final long size) {
            this.size = size;
            return this;
        }

        Builder setUnixGroupId(final Integer unixGroupId) {
            this.unixGroupId = unixGroupId;
            return this;
        }

        Builder setUnixPermissionMode(final Integer unixPermissionMode) {
            this.unixPermissionMode = unixPermissionMode;
            return this;
        }

        Builder setUnixUserId(final Integer unixUserId) {
            this.unixUserId = unixUserId;
            return this;
        }
    }

    static Builder builder() {
        return new Builder();
    }

    private final String name;

    private final boolean directory;

    private final long size;

    private final Date lastModifiedDate;

    private final long compressedSize;

    private final String compressionMethod;

    private final int crcValue;

    private final Integer osId;

    private final Integer unixPermissionMode;

    private final Integer unixUserId;

    private final Integer unixGroupId;

    private final Integer msdosFileAttributes;

    private final Integer headerCrc;

    LhaArchiveEntry(final Builder builder) {
        this.name = new StringBuilder().append(Objects.toString(builder.directoryName, "")).append(Objects.toString(builder.fileName, "")).toString();
        this.directory = builder.directory;
        this.size = builder.size;
        this.lastModifiedDate = builder.lastModifiedDate;
        this.compressedSize = builder.compressedSize;
        this.compressionMethod = builder.compressionMethod;
        this.crcValue = builder.crc;
        this.osId = builder.osId;
        this.unixPermissionMode = builder.unixPermissionMode;
        this.unixUserId = builder.unixUserId;
        this.unixGroupId = builder.unixGroupId;
        this.msdosFileAttributes = builder.msdosFileAttributes;
        this.headerCrc = builder.headerCrc;
    }

    /**
     * Gets the compressed size of this entry.
     *
     * @return the compressed size.
     */
    public long getCompressedSize() {
        return compressedSize;
    }

    /**
     * Gets the compression method of this entry.
     *
     * @return the compression method.
     */
    public String getCompressionMethod() {
        return compressionMethod;
    }

    /**
     * Gets the CRC-16 checksum of the uncompressed data of this entry.
     *
     * @return CRC-16 checksum of the uncompressed data.
     */
    public int getCrcValue() {
        return crcValue;
    }

    /**
     * Gets the header CRC if available for this entry. This method is package private, as it is of no interest to most users.
     *
     * @return header CRC or null if not available.
     */
    Integer getHeaderCrc() {
        return headerCrc;
    }

    @Override
    public Date getLastModifiedDate() {
        return lastModifiedDate;
    }

    /**
     * Gets the MS-DOS file attributes if available for this entry.
     *
     * @return MS-DOS file attributes or null if not available.
     */
    public Integer getMsdosFileAttributes() {
        return msdosFileAttributes;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Gets the operating system id if available for this entry.
     *
     * @return operating system id or null if not available.
     */
    public Integer getOsId() {
        return osId;
    }

    @Override
    public long getSize() {
        return size;
    }

    /**
     * Gets the Unix group id if available for this entry.
     *
     * @return Unix group id or null if not available.
     */
    public Integer getUnixGroupId() {
        return unixGroupId;
    }

    /**
     * Gets the Unix permission mode if available for this entry.
     *
     * @return Unix permission mode or null if not available.
     */
    public Integer getUnixPermissionMode() {
        return unixPermissionMode;
    }

    /**
     * Gets the Unix user id if available for this entry.
     *
     * @return Unix user id or null if not available.
     */
    public Integer getUnixUserId() {
        return unixUserId;
    }

    @Override
    public boolean isDirectory() {
        return directory;
    }

    @Override
    public String toString() {
        return String.format(
                "LhaArchiveEntry[name=%s, directory=%s, size=%s, lastModifiedDate=%s, compressedSize=%s, compressionMethod=%s, crc=0x%04x, osId=%s, "
                        + "unixPermissionMode=%03o, unixUserId=%s, unixGroupId=%s, msdosFileAttributes=%04x, headerCrc=%04x]",
                name, directory, size, lastModifiedDate == null ? "" : lastModifiedDate.toInstant().atZone(ZoneOffset.UTC).toString(), compressedSize,
                compressionMethod, crcValue, osId, unixPermissionMode, unixUserId, unixGroupId, msdosFileAttributes, headerCrc);
    }
}
