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
package org.apache.commons.compress2.archivers.spi;

import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.compress2.archivers.ArchiveEntry;
import org.apache.commons.compress2.archivers.ArchiveEntryParameters;
import org.apache.commons.compress2.archivers.OwnerInformation;

/**
 * Container for the basic information of an {@link ArchiveEntry}.
 * @Immutable
 */
public class SimpleArchiveEntry implements ArchiveEntry {
    private final String name;
    private final long size;
    private final ArchiveEntry.FileType type;
    private final Object fileKey;
    private final FileTime lastModified, lastAccess, created;
    private final Optional<OwnerInformation> owner;
    private final Optional<Set<PosixFilePermission>> permissions;
    private final Optional<Long> mode;

    /**
     * Creates a SimpleArchiveEntry from a parameter object.
     * @param params the parameters describing the archive entry.
     */
    public SimpleArchiveEntry(ArchiveEntryParameters params) {
        this.name = params.getName();
        this.size = params.size();
        this.type = params.getType();
        this.fileKey = params.fileKey();
        this.lastModified = params.lastModifiedTime();
        this.lastAccess = params.lastAccessTime();
        this.created = params.creationTime();
        this.owner = params.getOwnerInformation();
        this.permissions = params.getPermissions().map(Collections::unmodifiableSet);
        this.mode = params.getMode();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long size() {
        return size;
    }

    @Override
    public boolean isDirectory() {
        return type == FileType.DIR;
    }

    @Override
    public boolean isRegularFile() {
        return type == FileType.REGULAR_FILE;
    }

    @Override
    public boolean isSymbolicLink() {
        return type == FileType.SYMLINK;
    }

    @Override
    public boolean isOther() {
        return type == FileType.OTHER;
    }

    @Override
    public FileTime lastModifiedTime() {
        return lastModified;
    }

    @Override
    public FileTime lastAccessTime() {
        return lastAccess;
    }

    @Override
    public FileTime creationTime() {
        return created;
    }

    @Override
    public Object fileKey() {
        return fileKey;
    }

    @Override
    public Optional<OwnerInformation> getOwnerInformation() {
        return owner;
    }

    @Override
    public Optional<Long> getMode() {
        return mode;
    }

    @Override
    public Optional<Set<PosixFilePermission>> getPermissions() {
        return permissions;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (name == null ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        SimpleArchiveEntry other = (SimpleArchiveEntry) obj;
        return Objects.equals(name, other.name)
            && size == other.size
            && type == other.type
            && Objects.equals(fileKey, other.fileKey)
            && Objects.equals(lastModified, other.lastModified)
            && Objects.equals(lastAccess, other.lastAccess)
            && Objects.equals(created, other.created)
            && Objects.equals(mode, other.mode)
            && Objects.equals(permissions, other.permissions)
            && Objects.equals(owner, other.owner);
    }
}
