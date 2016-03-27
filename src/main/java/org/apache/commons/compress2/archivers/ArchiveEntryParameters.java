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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * A parameter object useful for creating new ArchiveEntries.
 * @NotThreadSafe
 */
public class ArchiveEntryParameters implements ArchiveEntry {

    private static final char SLASH = '/';

    private String name;
    private long size = ArchiveEntry.SIZE_UNKNOWN;
    private Object fileKey;
    private FileType type = FileType.REGULAR_FILE;
    private FileTime lastModified, created, lastAccess;
    private Optional<OwnerInformation> owner = Optional.empty();
    private Optional<Long> mode = Optional.empty();
    private Optional<Set<PosixFilePermission>> permissions = Optional.empty();

    /**
     * Creates parameters as a copy of an existing entry.
     * @param otherEntry the other entry.
     * @return parameters copied from the other entry
     */
    public static ArchiveEntryParameters copyOf(ArchiveEntry otherEntry) {
        return new ArchiveEntryParameters()
            .withName(otherEntry.getName())
            .withAttributes(otherEntry)
            .withType(otherEntry.getType())
            .withOwnerInformation(otherEntry.getOwnerInformation())
            .withMode(otherEntry.getMode())
            .withPermissions(otherEntry.getPermissions());
    }

    /**
     * Populates parameters from a Path instance.
     * @param path the Path to read information from
     * @param options options indicating how symbolic links are handled
     * @return parameters populated from the file instance
     */
    public static ArchiveEntryParameters fromPath(Path path, LinkOption... options) throws IOException {
        ArchiveEntryParameters params = new ArchiveEntryParameters()
            .withName(path.getFileName().toString());
        if (Files.exists(path, options)) {
            params = params
                .withAttributes(Files.readAttributes(path, BasicFileAttributes.class, options));
            try {
                params = params.withPermissions(Files.readAttributes(path, PosixFileAttributes.class, options)
                                                .permissions());
            } catch (UnsupportedOperationException ex) {
                // file system without support for POSIX attributes
            }
        }
        return params;
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
     * Sets the creation time of the entry.
     * @param creationTime the creation time for the entry to build
     * @return the parameters object
     */
    public ArchiveEntryParameters withCreationTime(FileTime creationTime) {
        this.created = creationTime;
        return this;
    }

    /**
     * Sets the last access time of the entry.
     * @param lastAccessTime the last access time for the entry to build
     * @return the parameters object
     */
    public ArchiveEntryParameters withLastAccessTime(FileTime lastAccessTime) {
        this.lastAccess = lastAccessTime;
        return this;
    }

    /**
     * Sets the last modified time of the entry.
     * @param lastModifiedTime the last modified time for the entry to build
     * @return the parameters object
     */
    public ArchiveEntryParameters withLastModifiedTime(FileTime lastModifiedTime) {
        this.lastModified = lastModifiedTime;
        return this;
    }

    /**
     * Sets the file key of the entry.
     * @param key the file key for the entry to build
     * @return the parameters object
     */
    public ArchiveEntryParameters withFileKey(Object key) {
        this.fileKey = key;
        return this;
    }

    /**
     * Sets the size of the entry.
     * @param size the size for the entry to build
     * @return the parameters object
     */
    public ArchiveEntryParameters withSize(long size) {
        this.size = size;
        return this;
    }

    /**
     * Sets the type of the entry.
     * @param type the type for the entry to build
     * @return the parameters object
     */
    public ArchiveEntryParameters withType(FileType type) {
        this.type = type;
        return this;
    }

    /**
     * Sets the owner information of the entry.
     * @param owner the owner information for the entry to build
     * @return the parameters object
     */
    public ArchiveEntryParameters withOwnerInformation(Optional<OwnerInformation> owner) {
        this.owner = owner;
        return this;
    }

    /**
     * Sets the owner information of the entry.
     * @param owner the owner information for the entry to build
     * @return the parameters object
     */
    public ArchiveEntryParameters withOwnerInformation(OwnerInformation owner) {
        return withOwnerInformation(Optional.ofNullable(owner));
    }

    /**
     * Sets the "mode" of the entry.
     * @param mode the mode for the entry to build
     * @return the parameters object
     */
    public ArchiveEntryParameters withMode(Optional<Long> mode) {
        this.mode = mode;
        return this;
    }

    /**
     * Sets the "mode" of the entry.
     * @param mode the mode for the entry to build
     * @return the parameters object
     */
    public ArchiveEntryParameters withMode(long mode) {
        return withMode(Optional.of(mode));
    }

    /**
     * Sets the permissions of the entry.
     * @param permissions the permissions for the entry to build
     * @return the parameters object
     */
    public ArchiveEntryParameters withPermissions(Optional<Set<PosixFilePermission>> permissions) {
        this.permissions = permissions;
        return this;
    }

    /**
     * Sets the permissions of the entry.
     * @param permissions the permissions for the entry to build
     * @return the parameters object
     */
    public ArchiveEntryParameters withPermissions(Set<PosixFilePermission> permissions) {
        return withPermissions(Optional.ofNullable(permissions));
    }

    /**
     * Sets the basic attributes.
     * @param attributes the attributes of the entry to build
     * @return the parameters object
     */
    public ArchiveEntryParameters withAttributes(BasicFileAttributes attributes) {
        if (attributes.isRegularFile()) {
            type = FileType.REGULAR_FILE;
        } else if (attributes.isDirectory()) {
            type = FileType.DIR;
        } else if (attributes.isSymbolicLink()) {
            type = FileType.SYMLINK;
        } else {
            type = FileType.OTHER;
        }
        return withCreationTime(attributes.creationTime())
            .withFileKey(attributes.fileKey())
            .withLastAccessTime(attributes.lastAccessTime())
            .withLastModifiedTime(attributes.lastModifiedTime())
            .withSize(attributes.size());
    }

    @Override
    public String getName() {
        return normalize(name, isDirectory());
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
    public boolean isDirectory() {
        return type == FileType.DIR;
    }

    @Override
    public boolean isOther() {
        return type == FileType.OTHER;
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
    public FileTime lastAccessTime() {
        return lastAccess;
    }

    @Override
    public FileTime lastModifiedTime() {
        return lastModified;
    }

    @Override
    public long size() {
        return type == FileType.DIR ? 0 : size;
    }

    @Override
    public Optional<OwnerInformation> getOwnerInformation() {
        return owner;
    }

    @Override
    public Optional<Long> getMode() {
        return mode.isPresent() ? mode : permissions.map(p -> modeFromPermissions(p, type));
    }

    @Override
    public Optional<Set<PosixFilePermission>> getPermissions() {
        return permissions.isPresent() ? permissions
            : mode.map(ArchiveEntryParameters::permissionsFromMode);
    }

    @Override
    public FileType getType() {
        return type;
    }

    /**
     * Translates a set of permissons into a Unix stat(2) {@code st_mode} result
     * @param permissions the permissions
     * @param type the file type
     * @return the "mode"
     */
    public static long modeFromPermissions(Set<PosixFilePermission> permissions, FileType type) {
        long mode;
        switch (type) {
        case SYMLINK:
            mode = 012;
            break;
        case REGULAR_FILE:
            mode = 010;
            break;
        case DIR:
            mode = 004;
            break;
        default:
            // OTHER could be a character or block device, a socket or a FIFO - so don't set anything
            mode = 0;
            break;
        }
        mode <<= 3;
        mode <<= 3; // we don't support sticky, setuid, setgid
        mode |= modeFromPermissions(permissions, "OWNER");
        mode <<= 3;
        mode |= modeFromPermissions(permissions, "GROUP");
        mode <<= 3;
        mode |= modeFromPermissions(permissions, "OTHERS");
        return mode;
    }

    /**
     * Translates a Unix stat(2) {@code st_mode} compatible value into a set of permissions.
     */
    public static Set<PosixFilePermission> permissionsFromMode(long mode) {
        Set<PosixFilePermission> permissions = EnumSet.noneOf(PosixFilePermission.class);
        addPermissions(permissions, "OTHERS", mode);
        addPermissions(permissions, "GROUP", mode >> 3);
        addPermissions(permissions, "OWNER", mode >> 6);
        return permissions;
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

    private static long modeFromPermissions(Set<PosixFilePermission> permissions, String prefix) {
        long mode = 0;
        if (permissions.contains(PosixFilePermission.valueOf(prefix + "_READ"))) {
            mode |= 4;
        }
        if (permissions.contains(PosixFilePermission.valueOf(prefix + "_WRITE"))) {
            mode |= 2;
        }
        if (permissions.contains(PosixFilePermission.valueOf(prefix + "_EXECUTE"))) {
            mode |= 1;
        }
        return mode;
    }

    private static void addPermissions(Set<PosixFilePermission> permissions, String prefix, long mode) {
        if ((mode & 1) == 1) {
            permissions.add(PosixFilePermission.valueOf(prefix + "_EXECUTE"));
        }
        if ((mode & 2) == 2) {
            permissions.add(PosixFilePermission.valueOf(prefix + "_WRITE"));
        }
        if ((mode & 4) == 4) {
            permissions.add(PosixFilePermission.valueOf(prefix + "_READ"));
        }
    }
}
