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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.compress.archivers.ArchiveEntry;

/**
 * ChangeSet collects and performs changes to an archive.
 * Putting delete changes in this ChangeSet from multiple threads can
 * cause conflicts.
 * 
 * @NotThreadSafe
 */
public final class ChangeSet {

    private final Set changes = new LinkedHashSet();

    /**
     * Deletes the file with the filename from the archive. 
     * 
     * @param pFilename
     *            the filename of the file to delete
     */
    public void delete(final String pFilename) {
        addDeletion(new Change(pFilename));
    }

    /**
     * Adds a new archive entry to the archive.
     * 
     * @param pEntry
     *            the entry to add
     * @param pInput
     *            the datastream to add
     */
    public void add(final ArchiveEntry pEntry, final InputStream pInput) {
        changes.add(new Change(pEntry, pInput));
    }

    /**
     * Adds an delete change.
     * 
     * @param pChange
     *            the change which should result in a deletion
     */
    private void addDeletion(Change pChange) {
        if (Change.TYPE_DELETE != pChange.type()
                || pChange.targetFile() == null) {
            return;
        }
        String source = pChange.targetFile();

        if (!changes.isEmpty()) {
            for (Iterator it = changes.iterator(); it.hasNext();) {
                Change change = (Change) it.next();
                if (change.type() == Change.TYPE_ADD
                        && change.getEntry() != null) {
                    String target = change.getEntry().getName();

                    if (source.equals(target)) {
                        it.remove();
                    } else if (target.matches(source + "/.*")) {
                        it.remove();
                    }
                }
            }
        }
        changes.add(pChange);
    }

    /**
     * Returns the list of changes as a copy. Changes on this set
     * are not reflected on this ChangeSet and vice versa.
     * @return the changes as a copy
     */
    Set getChanges() {
        return new LinkedHashSet(changes);
    }
}
