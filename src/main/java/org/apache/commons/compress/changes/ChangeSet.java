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

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;

/**
 * ChangeSet collects and performs changes to an archive.
 */
public final class ChangeSet {

    private final Set changes = new LinkedHashSet();

    /**
     * Deletes the file with the filename from the archive
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
     * Performs all changes collected in this ChangeSet on the input stream and
     * streams the result to the output stream.
     * 
     * @param in
     *            the InputStream to perform the changes on
     * @param out
     *            the resulting OutputStream with all modifications
     * @throws IOException
     *             if an read/write error occursâ
     */
    public void perform(ArchiveInputStream in, ArchiveOutputStream out)
            throws IOException {
        for (Iterator it = changes.iterator(); it.hasNext();) {
            Change change = (Change) it.next();

            if (change.type() == Change.TYPE_ADD) {
                copyStream(change.getInput(), out, change.getEntry());
                it.remove();
            }
        }

        ArchiveEntry entry = null;
        while ((entry = in.getNextEntry()) != null) {
            boolean copy = true;

            for (Iterator it = changes.iterator(); it.hasNext();) {
                Change change = (Change) it.next();

                if (change.type() == Change.TYPE_DELETE
                        && entry.getName() != null) {
                    if (entry.getName().equals(change.targetFile())) {
                        copy = false;
                        it.remove();
                        break;
                    } else if (entry.getName().matches(
                            change.targetFile() + "/.*")) {
                        copy = false;
                        break;
                    }
                }
            }

            if (copy) {
                if (!isDeletedLater(entry)) {
                    copyStream(in, out, entry);
                }
            }
        }
    }

    /**
     * Adds an delete change
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
     * Checks if an ArchiveEntry is deleted later in the ChangeSet. This is
     * necessary if an file is added with this ChangeSet, but later became
     * deleted in the same set.
     * 
     * @param entry
     *            the entry to check
     * @return true, if this entry has an deletion change later, false otherwise
     */
    private boolean isDeletedLater(ArchiveEntry entry) {
        String source = entry.getName();

        if (!changes.isEmpty()) {
            for (Iterator it = changes.iterator(); it.hasNext();) {
                Change change = (Change) it.next();
                if (change.type() == Change.TYPE_DELETE) {
                    String target = change.targetFile();

                    if (source.equals(target)) {
                        return true;
                    }

                    return source.matches(target + "/.*");
                }
            }
        }
        return false;
    }

    /**
     * Copies the ArchiveEntry to the Output stream
     * 
     * @param in
     *            the stream to read the data from
     * @param out
     *            the stream to write the data to
     * @param entry
     *            the entry to write
     * @throws IOException
     *             if data cannot be read or written
     */
    private static void copyStream(InputStream in, ArchiveOutputStream out,
            ArchiveEntry entry) throws IOException {
        out.putArchiveEntry(entry);
        IOUtils.copy(in, out);
        out.closeArchiveEntry();
    }
}
