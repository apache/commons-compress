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

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.changes.Change.ChangeType;
import org.apache.commons.io.IOUtils;

/**
 * Performs ChangeSet operations on a stream. This class is thread safe and can be used multiple times. It operates on a copy of the ChangeSet. If the ChangeSet
 * changes, a new Performer must be created.
 *
 * @param <I> The {@link ArchiveInputStream} type.
 * @param <O> The {@link ArchiveOutputStream} type.
 * @param <E> The {@link ArchiveEntry} type, must be compatible between the input {@code I} and output {@code O} stream types.
 * @ThreadSafe
 * @Immutable
 */
public class ChangeSetPerformer<I extends ArchiveInputStream<E>, O extends ArchiveOutputStream<E>, E extends ArchiveEntry> {

    /**
     * Abstracts getting entries and streams for archive entries.
     *
     * <p>
     * Iterator#hasNext is not allowed to throw exceptions that's why we can't use Iterator&lt;ArchiveEntry&gt; directly - otherwise we'd need to convert
     * exceptions thrown in ArchiveInputStream#getNextEntry.
     * </p>
     */
    private interface ArchiveEntryIterator<E extends ArchiveEntry> {

        InputStream getInputStream() throws IOException;

        boolean hasNext() throws IOException;

        E next();
    }

    private static final class ArchiveInputStreamIterator<E extends ArchiveEntry> implements ArchiveEntryIterator<E> {

        private final ArchiveInputStream<E> inputStream;
        private E next;

        ArchiveInputStreamIterator(final ArchiveInputStream<E> inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public InputStream getInputStream() {
            return inputStream;
        }

        @Override
        public boolean hasNext() throws IOException {
            return (next = inputStream.getNextEntry()) != null;
        }

        @Override
        public E next() {
            return next;
        }
    }

    private static final class ZipFileIterator implements ArchiveEntryIterator<ZipArchiveEntry> {

        private final ZipFile zipFile;
        private final Enumeration<ZipArchiveEntry> nestedEnumeration;
        private ZipArchiveEntry currentEntry;

        ZipFileIterator(final ZipFile zipFile) {
            this.zipFile = zipFile;
            this.nestedEnumeration = zipFile.getEntriesInPhysicalOrder();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return zipFile.getInputStream(currentEntry);
        }

        @Override
        public boolean hasNext() {
            return nestedEnumeration.hasMoreElements();
        }

        @Override
        public ZipArchiveEntry next() {
            return currentEntry = nestedEnumeration.nextElement();
        }
    }

    private final Set<Change<E>> changes;

    /**
     * Constructs a ChangeSetPerformer with the changes from this ChangeSet
     *
     * @param changeSet the ChangeSet which operations are used for performing
     */
    public ChangeSetPerformer(final ChangeSet<E> changeSet) {
        this.changes = changeSet.getChanges();
    }

    /**
     * Copies the ArchiveEntry to the Output stream
     *
     * @param inputStream  the stream to read the data from
     * @param outputStream the stream to write the data to
     * @param archiveEntry the entry to write
     * @throws IOException if data cannot be read or written
     */
    private void copyStream(final InputStream inputStream, final O outputStream, final E archiveEntry) throws IOException {
        outputStream.putArchiveEntry(archiveEntry);
        IOUtils.copy(inputStream, outputStream);
        outputStream.closeArchiveEntry();
    }

    /**
     * Checks if an ArchiveEntry is deleted later in the ChangeSet. This is necessary if a file is added with this ChangeSet, but later became deleted in the
     * same set.
     *
     * @param entry the entry to check
     * @return true, if this entry has a deletion change later, false otherwise
     */
    private boolean isDeletedLater(final Set<Change<E>> workingSet, final E entry) {
        final String source = entry.getName();

        if (!workingSet.isEmpty()) {
            for (final Change<E> change : workingSet) {
                final ChangeType type = change.getType();
                final String target = change.getTargetFileName();
                if (type == ChangeType.DELETE && source.equals(target) || type == ChangeType.DELETE_DIR && source.startsWith(target + "/")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Performs all changes collected in this ChangeSet on the input entries and streams the result to the output stream.
     *
     * This method finishes the stream, no other entries should be added after that.
     *
     * @param entryIterator the entries to perform the changes on
     * @param outputStream  the resulting OutputStream with all modifications
     * @throws IOException if a read/write error occurs
     * @return the results of this operation
     */
    private ChangeSetResults perform(final ArchiveEntryIterator<E> entryIterator, final O outputStream) throws IOException {
        final ChangeSetResults results = new ChangeSetResults();

        final Set<Change<E>> workingSet = new LinkedHashSet<>(changes);

        for (final Iterator<Change<E>> it = workingSet.iterator(); it.hasNext();) {
            final Change<E> change = it.next();

            if (change.getType() == ChangeType.ADD && change.isReplaceMode()) {
                @SuppressWarnings("resource") // InputStream not allocated here
                final InputStream inputStream = change.getInputStream();
                copyStream(inputStream, outputStream, change.getEntry());
                it.remove();
                results.addedFromChangeSet(change.getEntry().getName());
            }
        }

        while (entryIterator.hasNext()) {
            final E entry = entryIterator.next();
            boolean copy = true;

            for (final Iterator<Change<E>> it = workingSet.iterator(); it.hasNext();) {
                final Change<E> change = it.next();

                final ChangeType type = change.getType();
                final String name = entry.getName();
                if (type == ChangeType.DELETE && name != null) {
                    if (name.equals(change.getTargetFileName())) {
                        copy = false;
                        it.remove();
                        results.deleted(name);
                        break;
                    }
                } else // don't combine ifs to make future extensions more easy
                if (type == ChangeType.DELETE_DIR && name != null && name.startsWith(change.getTargetFileName() + "/")) { // NOPMD NOSONAR
                    copy = false;
                    results.deleted(name);
                    break;
                }
            }

            if (copy && !isDeletedLater(workingSet, entry) && !results.hasBeenAdded(entry.getName())) {
                @SuppressWarnings("resource") // InputStream not allocated here
                final InputStream inputStream = entryIterator.getInputStream();
                copyStream(inputStream, outputStream, entry);
                results.addedFromStream(entry.getName());
            }
        }

        // Adds files which hasn't been added from the original and do not have replace mode on
        for (final Iterator<Change<E>> it = workingSet.iterator(); it.hasNext();) {
            final Change<E> change = it.next();

            if (change.getType() == ChangeType.ADD && !change.isReplaceMode() && !results.hasBeenAdded(change.getEntry().getName())) {
                @SuppressWarnings("resource")
                final InputStream input = change.getInputStream();
                copyStream(input, outputStream, change.getEntry());
                it.remove();
                results.addedFromChangeSet(change.getEntry().getName());
            }
        }
        outputStream.finish();
        return results;
    }

    /**
     * Performs all changes collected in this ChangeSet on the input stream and streams the result to the output stream. Perform may be called more than once.
     *
     * This method finishes the stream, no other entries should be added after that.
     *
     * @param inputStream  the InputStream to perform the changes on
     * @param outputStream the resulting OutputStream with all modifications
     * @throws IOException if a read/write error occurs
     * @return the results of this operation
     */
    public ChangeSetResults perform(final I inputStream, final O outputStream) throws IOException {
        return perform(new ArchiveInputStreamIterator<>(inputStream), outputStream);
    }

    /**
     * Performs all changes collected in this ChangeSet on the ZipFile and streams the result to the output stream. Perform may be called more than once.
     *
     * This method finishes the stream, no other entries should be added after that.
     *
     * @param zipFile      the ZipFile to perform the changes on
     * @param outputStream the resulting OutputStream with all modifications
     * @throws IOException if a read/write error occurs
     * @return the results of this operation
     * @since 1.5
     */
    public ChangeSetResults perform(final ZipFile zipFile, final O outputStream) throws IOException {
        @SuppressWarnings("unchecked")
        final ArchiveEntryIterator<E> entryIterator = (ArchiveEntryIterator<E>) new ZipFileIterator(zipFile);
        return perform(entryIterator, outputStream);
    }
}
