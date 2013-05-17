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
    private int crc;
    private long size;
    
    public SevenZArchiveEntry() {
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public boolean hasStream() {
        return hasStream;
    }

    public void setHasStream(boolean hasStream) {
        this.hasStream = hasStream;
    }

    public boolean isDirectory() {
        return isDirectory;
    }
    
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

    public boolean getHasCreationDate() {
        return hasCreationDate;
    }
    
    public void setHasCreationDate(boolean hasCreationDate) {
        this.hasCreationDate = hasCreationDate;
    }
    
    public Date getCreationDate() {
        if (hasCreationDate) {
            return ntfsTimeToJavaTime(creationDate);
        } else {
            throw new UnsupportedOperationException(
                    "The entry doesn't have this timestamp");
        }
    }
    
    public void setCreationDate(long ntfsCreationDate) {
        this.creationDate = ntfsCreationDate;
    }
    
    public void setCreationDate(Date creationDate) {
        this.creationDate = javaTimeToNtfsTime(creationDate);
    }

    public boolean getHasLastModifiedDate() {
        return hasLastModifiedDate;
    }

    public void setHasLastModifiedDate(boolean hasLastModifiedDate) {
        this.hasLastModifiedDate = hasLastModifiedDate;
    }

    public Date getLastModifiedDate() {
        if (hasLastModifiedDate) {
            return ntfsTimeToJavaTime(lastModifiedDate);
        } else {
            throw new UnsupportedOperationException(
                    "The entry doesn't have this timestamp");
        }
    }
    
    public void setLastModifiedDate(long ntfsLastModifiedDate) {
        this.lastModifiedDate = ntfsLastModifiedDate;
    }
    
    public void setLastModifiedDate(Date lastModifiedDate) {
        this.lastModifiedDate = javaTimeToNtfsTime(lastModifiedDate);
    }
    
    public boolean getHasAccessDate() {
        return hasAccessDate;
    }

    public void setHasAccessDate(boolean hasAcessDate) {
        this.hasAccessDate = hasAcessDate;
    }

    public Date getAccessDate() {
        if (hasAccessDate) {
            return ntfsTimeToJavaTime(accessDate);
        } else {
            throw new UnsupportedOperationException(
                    "The entry doesn't have this timestamp");
        }
    }
    
    public void setAccessDate(long ntfsAccessDate) {
        this.accessDate = ntfsAccessDate;
    }
    
    public void setAccessDate(Date accessDate) {
        this.accessDate = javaTimeToNtfsTime(accessDate);
    }

    public boolean getHasWindowsAttributes() {
        return hasWindowsAttributes;
    }

    public void setHasWindowsAttributes(boolean hasWindowsAttributes) {
        this.hasWindowsAttributes = hasWindowsAttributes;
    }

    public int getWindowsAttributes() {
        return windowsAttributes;
    }

    public void setWindowsAttributes(int windowsAttributes) {
        this.windowsAttributes = windowsAttributes;
    }

    public boolean getHasCrc() {
        return hasCrc;
    }

    public void setHasCrc(boolean hasCrc) {
        this.hasCrc = hasCrc;
    }

    public int getCrc() {
        return crc;
    }

    public void setCrc(int crc) {
        this.crc = crc;
    }

    public long getSize() {
        return size;
    }
    
    public void setSize(long size) {
        this.size = size;
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
