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
package org.apache.commons.compress.changes;

import java.io.InputStream;
import java.util.Objects;

import org.apache.commons.compress.archivers.ArchiveEntry;

/**
 * Change holds meta information about a change.
 *
 * @param <E> The ArchiveEntry type.
 * @Immutable
 */
final class Change<E extends ArchiveEntry> {

    /**
     * Enumerates types of changes.
     */
    enum ChangeType {

        /**
         * Delete.
         */
        DELETE,

        /**
         * Add.
         */
        ADD,

        /**
         * Not used.
         */
        MOVE,

        /**
         * Delete directory.
         */
        DELETE_DIR
    }

    /** Entry name to delete. */
    private final String targetFileName;

    /** New entry to add. */
    private final E entry;

    /** Source for new entry. */
    private final InputStream inputStream;

    /** Change should replaceMode existing entries. */
    private final boolean replaceMode;

    /** Type of change. */
    private final ChangeType type;

    /**
     * Constructs a change which adds an entry.
     *
     * @param archiveEntry the entry details
     * @param inputStream  the InputStream for the entry data
     */
    Change(final E archiveEntry, final InputStream inputStream, final boolean replace) {
        this.entry = Objects.requireNonNull(archiveEntry, "archiveEntry");
        this.inputStream = Objects.requireNonNull(inputStream, "inputStream");
        this.type = ChangeType.ADD;
        this.targetFileName = null;
        this.replaceMode = replace;
    }

    /**
     * Constructs a new instance. Takes the file name of the file to be deleted from the stream as argument.
     *
     * @param fileName the file name of the file to delete
     */
    Change(final String fileName, final ChangeType type) {
        this.targetFileName = Objects.requireNonNull(fileName, "fileName");
        this.type = type;
        this.inputStream = null;
        this.entry = null;
        this.replaceMode = true;
    }

    E getEntry() {
        return entry;
    }

    InputStream getInputStream() {
        return inputStream;
    }

    String getTargetFileName() {
        return targetFileName;
    }

    ChangeType getType() {
        return type;
    }

    boolean isReplaceMode() {
        return replaceMode;
    }
}
