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

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Optional;
import java.util.Set;

/**
 * Represents an entry of an archive.
 *
 * <p>The scope of {@link BasicFileAttributes} attributes really
 * supported by an {@link ArchiveEntry} depends on the format. All
 * formats support the mandatory attributes of {@link
 * java.nio.file.attribute.BasicFileAttributes}.</p>
 * @Immutable
 */
public interface ArchiveEntry extends BasicFileAttributes {

    /**
     * The supported types of files, maps to the {@code isFoo} methods
     * in {@link BasicFileAttributes}.
     */
    public static enum FileType {
        /** A regular file. */
        REGULAR_FILE,
        /** A directory. */
        DIR,
        /** A symbolic link. */
        SYMLINK,
        /** Something that is neither a regular file nor a directory nor a symbolic link. */
        OTHER
    };

    /** Special value indicating that the size is unknown */
    static final long SIZE_UNKNOWN = -1;

    /**
     * Gets the name of the entry in this archive. May refer to a file or directory or other item.
     *
     * <p>The name will use '/' as directory separator and end with a '/' if and only if the entry represents a
     * directory.</p>
     *
     * @return The name of this entry in the archive.
     */
    String getName();

    /**
     * Provides information about the owner.
     *
     * @return information about the entry's owner or {@link Optional#empty} if the format doesn't support owner information
     */
    Optional<OwnerInformation> getOwnerInformation();

    /**
     * The "mode" associated with the entry.
     *
     * <p>Many formats support a mode attribute that is inspired by
     * the Unix stat(2) system call. Some even extend it beyond {@code
     * st_mode} which is only 16 bits, therefore a long is used to
     * accomodate these formats.</p>
     *
     * @return the format-specific mode if the format supports modes.
     */
    Optional<Long> getMode();

    /**
     * Permissions associated with the the entry.
     * @return the set of recognized permissions or {@link Optional#empty} if the format doesn't support permissions.
     */
    Optional<Set<PosixFilePermission>> getPermissions();

    /**
     * The type of entry.
     * @return the type of the entry.
     */
    default FileType getType() {
        if (isRegularFile()) {
            return FileType.REGULAR_FILE;
        } else if (isDirectory()) {
            return FileType.DIR;
        } else if (isSymbolicLink()) {
            return FileType.SYMLINK;
        }
        return FileType.OTHER;
    }
}
