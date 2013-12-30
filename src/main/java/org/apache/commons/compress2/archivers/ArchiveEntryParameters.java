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
package org.apache.commons.compress2.archivers;

import java.io.File;
import java.util.Date;

/**
 * A parameter object useful for creating new ArchiveEntries.
 * @NotThreadSafe
 */
public class ArchiveEntryParameters {

    private static final char SLASH = '/';

    private String name;
    private long size = ArchiveEntry.SIZE_UNKNOWN;
    private boolean dirFlag = false;
    private Date lastModified;
    private OwnerInformation owner;

    /**
     * Creates parameters as a copy of an existing entry.
     * @param otherEntry the other entry.
     * @return parameters copied from the other entry
     */
    public static ArchiveEntryParameters copyOf(ArchiveEntry otherEntry) {
        return new ArchiveEntryParameters()
            .withName(otherEntry.getName())
            .asDirectory(otherEntry.isDirectory())
            .withSize(otherEntry.getSize())
            .withLastModifiedDate(otherEntry.getLastModifiedDate())
            .withOwnerInformation(otherEntry.getOwnerInformation());
    }

    /**
     * Populates parameters from a File instance.
     * @param file the File to read information from
     * @return parameters populated from the file instance
     */
    public static ArchiveEntryParameters fromFile(File file) {
        return new ArchiveEntryParameters()
            .withName(file.getName())
            .asDirectory(file.isDirectory())
            .withSize(file.exists() ? file.length() : ArchiveEntry.SIZE_UNKNOWN)
            .withLastModifiedDate(new Date(file.lastModified()));
    }

    /**
     * Sets the name.
     *
     * <p>The name will be normalized to only contain '/' separators and end with a '/' if and only if the entry
     * represents a directory.</p>
     *
     * @param name the name of the entry to build
     * @return the parameters object
     */
    public ArchiveEntryParameters withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets the size of the entry.
     * @param size the size of the entry to build
     * @return the parameters object
     */
    public ArchiveEntryParameters withSize(long size) {
        this.size = size;
        return this;
    }

    /**
     * Marks the entry to build as a directory.
     * @param b whether the entry is supposed to represent a directory
     * @return the parameters object
     */
    public ArchiveEntryParameters asDirectory(boolean b) {
        this.dirFlag = b;
        return this;
    }

    /**
     * Sets the last modified date of the entry.
     * @param lastModified the last modified date of the entry to build
     * @return the parameters object
     */
    public ArchiveEntryParameters withLastModifiedDate(Date lastModified) {
        this.lastModified = clone(lastModified);
        return this;
    }

    /**
     * Sets the owner information of the entry.
     * @param owner the owner information for the entry to build
     * @return the parameters object
     */
    public ArchiveEntryParameters withOwnerInformation(OwnerInformation owner) {
        this.owner = owner;
        return this;
    }

    /**
     * Gets the configured name.
     *
     * <p>The name will use '/' as directory separator and end with a '/' if and only if the entry represents a
     * directory.</p>
     *
     * @return the normalized name
     */
    public String getName() {
        return normalize(name, dirFlag);
    }

    /**
     * Gets the configured size or {@link #SIZE_UNKNOWN}) if the size is not configured.
     * 
     * @return the configured size
     */
    public long getSize() {
        return dirFlag ? 0 : size;
    }

    /**
     * Returns true if parameters are configured to represent a directory.
     * 
     * @return true if this parameters refer to a directory.
     */
    public boolean isDirectory() {
        return dirFlag;
    }

    /**
     * Gets the configured last modified date.
     * 
     * @return the configured last modified date or null if no date was configured.
     */
    public Date getLastModifiedDate() {
        return clone(lastModified);
    }

    /**
     * Gets the configured information about the owner.
     *
     * @return information about the entry's owner or null if no information was configured
     */
    public OwnerInformation getOwnerInformation() {
        return owner;
    }

    private static String normalize(String name, boolean dirFlag) {
        if (name != null) {
            name = name.replace('\\', SLASH);
            int nameLength = name.length();
            boolean endsWithSlash = nameLength > 0 && name.charAt(nameLength - 1) == SLASH;
            if (endsWithSlash != dirFlag) {
                if (dirFlag) {
                    name += SLASH;
                } else {
                    name = name.substring(0, nameLength - 1);
                }
            }
        }
        return name;
    }

    private static Date clone(Date d) {
        return d == null ? null : (Date) d.clone();
    }
}
