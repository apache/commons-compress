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
package org.apache.commons.compress.changes;

import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.changes.Change.ChangeType;

/**
 * ChangeSet collects and performs changes to an archive. Putting delete changes in this ChangeSet from multiple threads can cause conflicts.
 *
 * @param <E> The ArchiveEntry type.
 * @NotThreadSafe
 */
public final class ChangeSet<E extends ArchiveEntry> {

    private final Set<Change<E>> changes = new LinkedHashSet<>();

    /**
     * Constructs a new instance.
     */
    public ChangeSet() {
        // empty
    }

    /**
     * Adds a new archive entry to the archive.
     *
     * @param entry the entry to add
     * @param input the data stream to add
     */
    public void add(final E entry, final InputStream input) {
        this.add(entry, input, true);
    }

    /**
     * Adds a new archive entry to the archive. If replace is set to true, this change will replace all other additions done in this ChangeSet and all existing
     * entries in the original stream.
     *
     * @param entry   the entry to add
     * @param input   the data stream to add
     * @param replace indicates the this change should replace existing entries
     */
    public void add(final E entry, final InputStream input, final boolean replace) {
        addAddition(new Change<>(entry, input, replace));
    }

    /**
     * Adds an addition change.
     *
     * @param addChange the change which should result in an addition
     */
    @SuppressWarnings("resource") // InputStream is NOT allocated
    private void addAddition(final Change<E> addChange) {
        if (Change.ChangeType.ADD != addChange.getType() || addChange.getInputStream() == null) {
            return;
        }

        if (!changes.isEmpty()) {
            for (final Iterator<Change<E>> it = changes.iterator(); it.hasNext();) {
                final Change<E> change = it.next();
                if (change.getType() == Change.ChangeType.ADD && change.getEntry() != null) {
                    final ArchiveEntry entry = change.getEntry();

                    if (entry.equals(addChange.getEntry())) {
                        if (addChange.isReplaceMode()) {
                            it.remove();
                            changes.add(addChange);
                        }
                        // do not add this change
                        return;
                    }
                }
            }
        }
        changes.add(addChange);
    }

    /**
     * Adds an delete change.
     *
     * @param deleteChange the change which should result in a deletion
     */
    private void addDeletion(final Change<E> deleteChange) {
        if (ChangeType.DELETE != deleteChange.getType() && ChangeType.DELETE_DIR != deleteChange.getType() || deleteChange.getTargetFileName() == null) {
            return;
        }
        final String source = deleteChange.getTargetFileName();
        final Pattern pattern = Pattern.compile(source + "/.*");
        if (source != null && !changes.isEmpty()) {
            for (final Iterator<Change<E>> it = changes.iterator(); it.hasNext();) {
                final Change<E> change = it.next();
                if (change.getType() == ChangeType.ADD && change.getEntry() != null) {
                    final String target = change.getEntry().getName();
                    if (target == null) {
                        continue;
                    }
                    if (ChangeType.DELETE == deleteChange.getType() && source.equals(target)
                            || ChangeType.DELETE_DIR == deleteChange.getType() && pattern.matcher(target).matches()) {
                        it.remove();
                    }
                }
            }
        }
        changes.add(deleteChange);
    }

    /**
     * Deletes the file with the file name from the archive.
     *
     * @param fileName the file name of the file to delete
     */
    public void delete(final String fileName) {
        addDeletion(new Change<>(fileName, ChangeType.DELETE));
    }

    /**
     * Deletes the directory tree from the archive.
     *
     * @param dirName the name of the directory tree to delete
     */
    public void deleteDir(final String dirName) {
        addDeletion(new Change<>(dirName, ChangeType.DELETE_DIR));
    }

    /**
     * Gets the list of changes as a copy. Changes on this set are not reflected on this ChangeSet and vice versa.
     *
     * @return the changes as a copy
     */
    Set<Change<E>> getChanges() {
        return new LinkedHashSet<>(changes);
    }
}
