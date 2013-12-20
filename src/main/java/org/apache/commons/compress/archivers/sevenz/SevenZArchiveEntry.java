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
package org.apache.commons.compress.archivers.sevenz;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.compress.archivers.ArchiveEntry;

/**
 * An entry in a 7z archive.
 * 
 * @NotThreadSafe
 * @since 1.6
 */
public class SevenZArchiveEntry implements ArchiveEntry {
    private String name;
    private boolean hasStream;
    private boolean isDirectory;
    private boolean isAntiItem;
    private boolean hasCreationDate;
    private boolean hasLastModifiedDate;
    private boolean hasAccessDate;
    private long creationDate;
    private long lastModifiedDate;
    private long accessDate;
    private boolean hasWindowsAttributes;
    private int windowsAttributes;
    private boolean hasCrc;
    private long crc, compressedCrc;
    private long size, compressedSize;
    
    public SevenZArchiveEntry() {
    }

    /**
     * Get this entry's name.
     *
     * @return This entry's name.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Set this entry's name.
     *
     * @param name This entry's new name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Whether there is any content associated with this entry.
     * @return whether there is any content associated with this entry.
     */
    public boolean hasStream() {
        return hasStream;
    }

    /**
     * Sets whether there is any content associated with this entry.
     * @param hasStream whether there is any content associated with this entry.
     */
    public void setHasStream(boolean hasStream) {
        this.hasStream = hasStream;
    }

    /**
     * Return whether or not this entry represents a directory.
     *
     * @return True if this entry is a directory.
     */
    public boolean isDirectory() {
        return isDirectory;
    }
    
    /**
     * Sets whether or not this entry represents a directory.
     *
     * @param isDirectory True if this entry is a directory.
     */
    public void setDirectory(boolean isDirectory) {
        this.isDirectory = isDirectory;
    }
    
    /**
     * Indicates whether this is an "anti-item" used in differential backups,
     * meaning it should delete the same file from a previous backup. 
     * @return true if it is an anti-item, false otherwise
     */
    public boolean isAntiItem() {
        return isAntiItem;
    }

    /**
     * Sets whether this is an "anti-item" used in differential backups,
     * meaning it should delete the same file from a previous backup.
     * @param isAntiItem true if it is an ait-item, false otherwise 
     */
    public void setAntiItem(boolean isAntiItem) {
        this.isAntiItem = isAntiItem;
    }

    /**
     * Returns whether this entry has got a creation date at all.
     */
    public boolean getHasCreationDate() {
        return hasCreationDate;
    }
    
    /**
     * Sets whether this entry has got a creation date at all.
     */
    public void setHasCreationDate(boolean hasCreationDate) {
        this.hasCreationDate = hasCreationDate;
    }
    
    /**
     * Gets the creation date.
     * @throws UnsupportedOperationException if the entry hasn't got a
     * creation date.
     */
    public Date getCreationDate() {
        if (hasCreationDate) {
            return ntfsTimeToJavaTime(creationDate);
        } else {
            throw new UnsupportedOperationException(
                    "The entry doesn't have this timestamp");
        }
    }
    
    /**
     * Sets the creation date using NTFS time (100 nanosecond units
     * since 1 January 1601)
     */
    public void setCreationDate(long ntfsCreationDate) {
        this.creationDate = ntfsCreationDate;
    }
    
    /**
     * Sets the creation date,
     */
    public void setCreationDate(Date creationDate) {
        hasCreationDate = creationDate != null;
        if (hasCreationDate) {
            this.creationDate = javaTimeToNtfsTime(creationDate);
        }
    }

    /**
     * Returns whether this entry has got a last modified date at all.
     */
    public boolean getHasLastModifiedDate() {
        return hasLastModifiedDate;
    }

    /**
     * Sets whether this entry has got a last modified date at all.
     */
    public void setHasLastModifiedDate(boolean hasLastModifiedDate) {
        this.hasLastModifiedDate = hasLastModifiedDate;
    }

    /**
     * Gets the last modified date.
     * @throws UnsupportedOperationException if the entry hasn't got a
     * last modified date.
     */
    public Date getLastModifiedDate() {
        if (hasLastModifiedDate) {
            return ntfsTimeToJavaTime(lastModifiedDate);
        } else {
            throw new UnsupportedOperationException(
                    "The entry doesn't have this timestamp");
        }
    }
    
    /**
     * Sets the last modified date using NTFS time (100 nanosecond
     * units since 1 January 1601)
     */
    public void setLastModifiedDate(long ntfsLastModifiedDate) {
        this.lastModifiedDate = ntfsLastModifiedDate;
    }
    
    /**
     * Sets the last modified date,
     */
    public void setLastModifiedDate(Date lastModifiedDate) {
        hasLastModifiedDate = lastModifiedDate != null;
        if (hasLastModifiedDate) {
            this.lastModifiedDate = javaTimeToNtfsTime(lastModifiedDate);
        }
    }
    
    /**
     * Returns whether this entry has got an access date at all.
     */
    public boolean getHasAccessDate() {
        return hasAccessDate;
    }

    /**
     * Sets whether this entry has got an access date at all.
     */
    public void setHasAccessDate(boolean hasAcessDate) {
        this.hasAccessDate = hasAcessDate;
    }

    /**
     * Gets the access date.
     * @throws UnsupportedOperationException if the entry hasn't got a
     * access date.
     */
    public Date getAccessDate() {
        if (hasAccessDate) {
            return ntfsTimeToJavaTime(accessDate);
        } else {
            throw new UnsupportedOperationException(
                    "The entry doesn't have this timestamp");
        }
    }
    
    /**
     * Sets the access date using NTFS time (100 nanosecond units
     * since 1 January 1601)
     */
    public void setAccessDate(long ntfsAccessDate) {
        this.accessDate = ntfsAccessDate;
    }
    
    /**
     * Sets the access date,
     */
    public void setAccessDate(Date accessDate) {
        hasAccessDate = accessDate != null;
        if (hasAccessDate) {
            this.accessDate = javaTimeToNtfsTime(accessDate);
        }
    }

    /**
     * Returns whether this entry has windows attributes.
     */
    public boolean getHasWindowsAttributes() {
        return hasWindowsAttributes;
    }

    /**
     * Sets whether this entry has windows attributes.
     */
    public void setHasWindowsAttributes(boolean hasWindowsAttributes) {
        this.hasWindowsAttributes = hasWindowsAttributes;
    }

    /**
     * Gets the windows attributes.
     */
    public int getWindowsAttributes() {
        return windowsAttributes;
    }

    /**
     * Sets the windows attributes.
     */
    public void setWindowsAttributes(int windowsAttributes) {
        this.windowsAttributes = windowsAttributes;
    }

    /**
     * Returns whether this entry has got a crc.
     *
     * In general entries without streams don't have a CRC either.
     */
    public boolean getHasCrc() {
        return hasCrc;
    }

    /**
     * Sets whether this entry has got a crc.
     */
    public void setHasCrc(boolean hasCrc) {
        this.hasCrc = hasCrc;
    }

    /**
     * Gets the CRC.
     * @deprecated use getCrcValue instead.
     */
    @Deprecated
    public int getCrc() {
        return (int) crc;
    }

    /**
     * Sets the CRC.
     * @deprecated use setCrcValue instead.
     */
    @Deprecated
    public void setCrc(int crc) {
        this.crc = crc;
    }

    /**
     * Gets the CRC.
     * @since Compress 1.7
     */
    public long getCrcValue() {
        return crc;
    }

    /**
     * Sets the CRC.
     * @since Compress 1.7
     */
    public void setCrcValue(long crc) {
        this.crc = crc;
    }

    /**
     * Gets the compressed CRC.
     * @deprecated use getCompressedCrcValue instead.
     */
    @Deprecated
    int getCompressedCrc() {
        return (int) compressedCrc;
    }

    /**
     * Sets the compressed CRC.
     * @deprecated use setCompressedCrcValue instead.
     */
    @Deprecated
    void setCompressedCrc(int crc) {
        this.compressedCrc = crc;
    }

    /**
     * Gets the compressed CRC.
     * @since Compress 1.7
     */
    long getCompressedCrcValue() {
        return compressedCrc;
    }

    /**
     * Sets the compressed CRC.
     * @since Compress 1.7
     */
    void setCompressedCrcValue(long crc) {
        this.compressedCrc = crc;
    }

    /**
     * Get this entry's file size.
     *
     * @return This entry's file size.
     */
    public long getSize() {
        return size;
    }
    
    /**
     * Set this entry's file size.
     *
     * @param size This entry's new file size.
     */
    public void setSize(long size) {
        this.size = size;
    }

    /**
     * Get this entry's compressed file size.
     *
     * @return This entry's compressed file size.
     */
    long getCompressedSize() {
        return compressedSize;
    }
    
    /**
     * Set this entry's compressed file size.
     *
     * @param size This entry's new compressed file size.
     */
    void setCompressedSize(long size) {
        this.compressedSize = size;
    }

    /**
     * Converts NTFS time (100 nanosecond units since 1 January 1601)
     * to Java time.
     * @param ntfsTime the NTFS time in 100 nanosecond units
     * @return the Java time
     */
    public static Date ntfsTimeToJavaTime(final long ntfsTime) {
        final Calendar ntfsEpoch = Calendar.getInstance();
        ntfsEpoch.setTimeZone(TimeZone.getTimeZone("GMT+0"));
        ntfsEpoch.set(1601, 0, 1, 0, 0, 0);
        ntfsEpoch.set(Calendar.MILLISECOND, 0);
        final long realTime = ntfsEpoch.getTimeInMillis() + (ntfsTime / (10*1000));
        return new Date(realTime);
    }
    
    /**
     * Converts Java time to NTFS time.
     * @param date the Java time
     * @return the NTFS time
     */
    public static long javaTimeToNtfsTime(final Date date) {
        final Calendar ntfsEpoch = Calendar.getInstance();
        ntfsEpoch.setTimeZone(TimeZone.getTimeZone("GMT+0"));
        ntfsEpoch.set(1601, 0, 1, 0, 0, 0);
        ntfsEpoch.set(Calendar.MILLISECOND, 0);
        return ((date.getTime() - ntfsEpoch.getTimeInMillis())* 1000 * 10);
    }
}
