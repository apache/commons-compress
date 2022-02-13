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
package org.apache.commons.compress.archivers.examples;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Enumeration;
import java.util.Iterator;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarFile;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.IOUtils;

/**
 * Provides a high level API for expanding archives.
 * @since 1.17
 */
public class Expander {

    private interface ArchiveEntrySupplier {
        ArchiveEntry getNextReadableEntry() throws IOException;
    }

    private interface EntryWriter {
        void writeEntryDataTo(ArchiveEntry entry, OutputStream out) throws IOException;
    }

    /**
     * @param targetDirectory May be null to simulate output to dev/null on Linux and NUL on Windows.
     */
    private void expand(final ArchiveEntrySupplier supplier, final EntryWriter writer, final Path targetDirectory)
        throws IOException {
        final boolean nullTarget = targetDirectory == null;
        final Path targetDirPath = nullTarget ? null : targetDirectory.normalize();
        ArchiveEntry nextEntry = supplier.getNextReadableEntry();
        while (nextEntry != null) {
            final Path targetPath = nullTarget ? null : targetDirectory.resolve(nextEntry.getName());
            // check if targetDirectory and f are the same path - this may
            // happen if the nextEntry.getName() is "./"
            if (!nullTarget && !targetPath.normalize().startsWith(targetDirPath) && !Files.isSameFile(targetDirectory, targetPath)) {
                throw new IOException("Expanding " + nextEntry.getName() + " would create file outside of " + targetDirectory);
            }
            if (nextEntry.isDirectory()) {
                if (!nullTarget && !Files.isDirectory(targetPath) && Files.createDirectories(targetPath) == null) {
                    throw new IOException("Failed to create directory " + targetPath);
                }
            } else {
                final Path parent = nullTarget ? null : targetPath.getParent();
                if (!nullTarget && !Files.isDirectory(parent) && Files.createDirectories(parent) == null) {
                    throw new IOException("Failed to create directory " + parent);
                }
                if (nullTarget) {
                    writer.writeEntryDataTo(nextEntry, null);
                } else {
                    try (OutputStream outputStream = Files.newOutputStream(targetPath)) {
                        writer.writeEntryDataTo(nextEntry, outputStream);
                    }
                }
            }
            nextEntry = supplier.getNextReadableEntry();
        }
    }

    /**
     * Expands {@code archive} into {@code targetDirectory}.
     *
     * @param archive the file to expand
     * @param targetDirectory the target directory, may be null to simulate output to dev/null on Linux and NUL on Windows.
     * @throws IOException if an I/O error occurs
     */
    public void expand(final ArchiveInputStream archive, final File targetDirectory) throws IOException {
        expand(archive, toPath(targetDirectory));
    }

    /**
     * Expands {@code archive} into {@code targetDirectory}.
     *
     * @param archive the file to expand
     * @param targetDirectory the target directory, may be null to simulate output to dev/null on Linux and NUL on Windows.
     * @throws IOException if an I/O error occurs
     * @since 1.22
     */
    public void expand(final ArchiveInputStream archive, final Path targetDirectory) throws IOException {
        expand(() -> {
            ArchiveEntry next = archive.getNextEntry();
            while (next != null && !archive.canReadEntryData(next)) {
                next = archive.getNextEntry();
            }
            return next;
        }, (entry, out) -> IOUtils.copy(archive, out), targetDirectory);
    }

    /**
     * Expands {@code archive} into {@code targetDirectory}.
     *
     * <p>Tries to auto-detect the archive's format.</p>
     *
     * @param archive the file to expand
     * @param targetDirectory the target directory
     * @throws IOException if an I/O error occurs
     * @throws ArchiveException if the archive cannot be read for other reasons
     */
    public void expand(final File archive, final File targetDirectory) throws IOException, ArchiveException {
        expand(archive.toPath(), toPath(targetDirectory));
    }

    /**
     * Expands {@code archive} into {@code targetDirectory}.
     *
     * <p>Tries to auto-detect the archive's format.</p>
     *
     * <p>This method creates a wrapper around the archive stream
     * which is never closed and thus leaks resources, please use
     * {@link #expand(InputStream,File,CloseableConsumer)}
     * instead.</p>
     *
     * @param archive the file to expand
     * @param targetDirectory the target directory
     * @throws IOException if an I/O error occurs
     * @throws ArchiveException if the archive cannot be read for other reasons
     * @deprecated this method leaks resources
     */
    @Deprecated
    public void expand(final InputStream archive, final File targetDirectory) throws IOException, ArchiveException {
        expand(archive, targetDirectory, CloseableConsumer.NULL_CONSUMER);
    }

    /**
     * Expands {@code archive} into {@code targetDirectory}.
     *
     * <p>Tries to auto-detect the archive's format.</p>
     *
     * <p>This method creates a wrapper around the archive stream and
     * the caller of this method is responsible for closing it -
     * probably at the same time as closing the stream itself. The
     * caller is informed about the wrapper object via the {@code
     * closeableConsumer} callback as soon as it is no longer needed
     * by this class.</p>
     *
     * @param archive the file to expand
     * @param targetDirectory the target directory
     * @param closeableConsumer is informed about the stream wrapped around the passed in stream
     * @throws IOException if an I/O error occurs
     * @throws ArchiveException if the archive cannot be read for other reasons
     * @since 1.19
     */
    public void expand(final InputStream archive, final File targetDirectory, final CloseableConsumer closeableConsumer)
        throws IOException, ArchiveException {
        try (CloseableConsumerAdapter c = new CloseableConsumerAdapter(closeableConsumer)) {
            expand(c.track(ArchiveStreamFactory.DEFAULT.createArchiveInputStream(archive)),
                targetDirectory);
        }
    }

    /**
     * Expands {@code archive} into {@code targetDirectory}.
     *
     * <p>Tries to auto-detect the archive's format.</p>
     *
     * @param archive the file to expand
     * @param targetDirectory the target directory
     * @throws IOException if an I/O error occurs
     * @throws ArchiveException if the archive cannot be read for other reasons
     * @since 1.22
     */
    public void expand(final Path archive, final Path targetDirectory) throws IOException, ArchiveException {
        String format = null;
        try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(archive))) {
            format = ArchiveStreamFactory.detect(inputStream);
        }
        expand(format, archive, targetDirectory);
    }

    /**
     * Expands {@code archive} into {@code targetDirectory}.
     *
     * @param archive the file to expand
     * @param targetDirectory the target directory, may be null to simulate output to dev/null on Linux and NUL on Windows.
     * @throws IOException if an I/O error occurs
     */
    public void expand(final SevenZFile archive, final File targetDirectory) throws IOException {
        expand(archive, toPath(targetDirectory));
    }

    /**
     * Expands {@code archive} into {@code targetDirectory}.
     *
     * @param archive the file to expand
     * @param targetDirectory the target directory, may be null to simulate output to dev/null on Linux and NUL on Windows.
     * @throws IOException if an I/O error occurs
     * @since 1.22
     */
    public void expand(final SevenZFile archive, final Path targetDirectory)
        throws IOException {
        expand(archive::getNextEntry, (entry, out) -> {
            final byte[] buffer = new byte[8192];
            int n;
            while (-1 != (n = archive.read(buffer))) {
                if (out != null) {
                    out.write(buffer, 0, n);
                }
            }
        }, targetDirectory);
    }

    /**
     * Expands {@code archive} into {@code targetDirectory}.
     *
     * @param archive the file to expand
     * @param targetDirectory the target directory
     * @param format the archive format. This uses the same format as
     * accepted by {@link ArchiveStreamFactory}.
     * @throws IOException if an I/O error occurs
     * @throws ArchiveException if the archive cannot be read for other reasons
     */
    public void expand(final String format, final File archive, final File targetDirectory) throws IOException, ArchiveException {
        expand(format, archive.toPath(), toPath(targetDirectory));
    }

    /**
     * Expands {@code archive} into {@code targetDirectory}.
     *
     * <p>This method creates a wrapper around the archive stream
     * which is never closed and thus leaks resources, please use
     * {@link #expand(String,InputStream,File,CloseableConsumer)}
     * instead.</p>
     *
     * @param archive the file to expand
     * @param targetDirectory the target directory
     * @param format the archive format. This uses the same format as
     * accepted by {@link ArchiveStreamFactory}.
     * @throws IOException if an I/O error occurs
     * @throws ArchiveException if the archive cannot be read for other reasons
     * @deprecated this method leaks resources
     */
    @Deprecated
    public void expand(final String format, final InputStream archive, final File targetDirectory)
        throws IOException, ArchiveException {
        expand(format, archive, targetDirectory, CloseableConsumer.NULL_CONSUMER);
    }

    /**
     * Expands {@code archive} into {@code targetDirectory}.
     *
     * <p>This method creates a wrapper around the archive stream and
     * the caller of this method is responsible for closing it -
     * probably at the same time as closing the stream itself. The
     * caller is informed about the wrapper object via the {@code
     * closeableConsumer} callback as soon as it is no longer needed
     * by this class.</p>
     *
     * @param archive the file to expand
     * @param targetDirectory the target directory
     * @param format the archive format. This uses the same format as
     * accepted by {@link ArchiveStreamFactory}.
     * @param closeableConsumer is informed about the stream wrapped around the passed in stream
     * @throws IOException if an I/O error occurs
     * @throws ArchiveException if the archive cannot be read for other reasons
     * @since 1.19
     */
    public void expand(final String format, final InputStream archive, final File targetDirectory, final CloseableConsumer closeableConsumer)
        throws IOException, ArchiveException {
        expand(format, archive, toPath(targetDirectory), closeableConsumer);
    }

    /**
     * Expands {@code archive} into {@code targetDirectory}.
     *
     * <p>This method creates a wrapper around the archive stream and
     * the caller of this method is responsible for closing it -
     * probably at the same time as closing the stream itself. The
     * caller is informed about the wrapper object via the {@code
     * closeableConsumer} callback as soon as it is no longer needed
     * by this class.</p>
     *
     * @param archive the file to expand
     * @param targetDirectory the target directory
     * @param format the archive format. This uses the same format as
     * accepted by {@link ArchiveStreamFactory}.
     * @param closeableConsumer is informed about the stream wrapped around the passed in stream
     * @throws IOException if an I/O error occurs
     * @throws ArchiveException if the archive cannot be read for other reasons
     * @since 1.22
     */
    public void expand(final String format, final InputStream archive, final Path targetDirectory, final CloseableConsumer closeableConsumer)
        throws IOException, ArchiveException {
        try (CloseableConsumerAdapter c = new CloseableConsumerAdapter(closeableConsumer)) {
            expand(c.track(ArchiveStreamFactory.DEFAULT.createArchiveInputStream(format, archive)),
                targetDirectory);
        }
    }

    /**
     * Expands {@code archive} into {@code targetDirectory}.
     *
     * @param archive the file to expand
     * @param targetDirectory the target directory
     * @param format the archive format. This uses the same format as
     * accepted by {@link ArchiveStreamFactory}.
     * @throws IOException if an I/O error occurs
     * @throws ArchiveException if the archive cannot be read for other reasons
     * @since 1.22
     */
    public void expand(final String format, final Path archive, final Path targetDirectory) throws IOException, ArchiveException {
        if (prefersSeekableByteChannel(format)) {
            try (SeekableByteChannel channel = FileChannel.open(archive, StandardOpenOption.READ)) {
                expand(format, channel, targetDirectory, CloseableConsumer.CLOSING_CONSUMER);
            }
            return;
        }
        try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(archive))) {
            expand(format, inputStream, targetDirectory, CloseableConsumer.CLOSING_CONSUMER);
        }
    }

    /**
     * Expands {@code archive} into {@code targetDirectory}.
     *
     * <p>This method creates a wrapper around the archive channel
     * which is never closed and thus leaks resources, please use
     * {@link #expand(String,SeekableByteChannel,File,CloseableConsumer)}
     * instead.</p>
     *
     * @param archive the file to expand
     * @param targetDirectory the target directory
     * @param format the archive format. This uses the same format as
     * accepted by {@link ArchiveStreamFactory}.
     * @throws IOException if an I/O error occurs
     * @throws ArchiveException if the archive cannot be read for other reasons
     * @deprecated this method leaks resources
     */
    @Deprecated
    public void expand(final String format, final SeekableByteChannel archive, final File targetDirectory)
        throws IOException, ArchiveException {
        expand(format, archive, targetDirectory, CloseableConsumer.NULL_CONSUMER);
    }

    /**
     * Expands {@code archive} into {@code targetDirectory}.
     *
     * <p>This method creates a wrapper around the archive channel and
     * the caller of this method is responsible for closing it -
     * probably at the same time as closing the channel itself. The
     * caller is informed about the wrapper object via the {@code
     * closeableConsumer} callback as soon as it is no longer needed
     * by this class.</p>
     *
     * @param archive the file to expand
     * @param targetDirectory the target directory
     * @param format the archive format. This uses the same format as
     * accepted by {@link ArchiveStreamFactory}.
     * @param closeableConsumer is informed about the stream wrapped around the passed in channel
     * @throws IOException if an I/O error occurs
     * @throws ArchiveException if the archive cannot be read for other reasons
     * @since 1.19
     */
    public void expand(final String format, final SeekableByteChannel archive, final File targetDirectory, final CloseableConsumer closeableConsumer)
        throws IOException, ArchiveException {
        expand(format, archive, toPath(targetDirectory), closeableConsumer);
    }

    /**
     * Expands {@code archive} into {@code targetDirectory}.
     *
     * <p>This method creates a wrapper around the archive channel and
     * the caller of this method is responsible for closing it -
     * probably at the same time as closing the channel itself. The
     * caller is informed about the wrapper object via the {@code
     * closeableConsumer} callback as soon as it is no longer needed
     * by this class.</p>
     *
     * @param archive the file to expand
     * @param targetDirectory the target directory
     * @param format the archive format. This uses the same format as
     * accepted by {@link ArchiveStreamFactory}.
     * @param closeableConsumer is informed about the stream wrapped around the passed in channel
     * @throws IOException if an I/O error occurs
     * @throws ArchiveException if the archive cannot be read for other reasons
     * @since 1.22
     */
    public void expand(final String format, final SeekableByteChannel archive, final Path targetDirectory,
        final CloseableConsumer closeableConsumer)
        throws IOException, ArchiveException {
        try (CloseableConsumerAdapter c = new CloseableConsumerAdapter(closeableConsumer)) {
        if (!prefersSeekableByteChannel(format)) {
            expand(format, c.track(Channels.newInputStream(archive)), targetDirectory, CloseableConsumer.NULL_CONSUMER);
        } else if (ArchiveStreamFactory.TAR.equalsIgnoreCase(format)) {
            expand(c.track(new TarFile(archive)), targetDirectory);
        } else if (ArchiveStreamFactory.ZIP.equalsIgnoreCase(format)) {
            expand(c.track(new ZipFile(archive)), targetDirectory);
        } else if (ArchiveStreamFactory.SEVEN_Z.equalsIgnoreCase(format)) {
            expand(c.track(new SevenZFile(archive)), targetDirectory);
        } else {
            // never reached as prefersSeekableByteChannel only returns true for TAR, ZIP and 7z
            throw new ArchiveException("Don't know how to handle format " + format);
        }
        }
    }

    /**
     * Expands {@code archive} into {@code targetDirectory}.
     *
     * @param archive the file to expand
     * @param targetDirectory the target directory, may be null to simulate output to dev/null on Linux and NUL on Windows.
     * @throws IOException if an I/O error occurs
     * @since 1.21
     */
    public void expand(final TarFile archive, final File targetDirectory) throws IOException {
        expand(archive, toPath(targetDirectory));
    }

    /**
     * Expands {@code archive} into {@code targetDirectory}.
     *
     * @param archive the file to expand
     * @param targetDirectory the target directory, may be null to simulate output to dev/null on Linux and NUL on Windows.
     * @throws IOException if an I/O error occurs
     * @since 1.22
     */
    public void expand(final TarFile archive, final Path targetDirectory)
        throws IOException {
        final Iterator<TarArchiveEntry> entryIterator = archive.getEntries().iterator();
        expand(() -> entryIterator.hasNext() ? entryIterator.next() : null,
            (entry, out) -> {
            try (InputStream in = archive.getInputStream((TarArchiveEntry) entry)) {
                IOUtils.copy(in, out);
            }
        }, targetDirectory);
    }

    /**
     * Expands {@code archive} into {@code targetDirectory}.
     *
     * @param archive the file to expand
     * @param targetDirectory the target directory, may be null to simulate output to dev/null on Linux and NUL on Windows.
     * @throws IOException if an I/O error occurs
     */
    public void expand(final ZipFile archive, final File targetDirectory) throws IOException {
        expand(archive, toPath(targetDirectory));
    }

    /**
     * Expands {@code archive} into {@code targetDirectory}.
     *
     * @param archive the file to expand
     * @param targetDirectory the target directory, may be null to simulate output to dev/null on Linux and NUL on Windows.
     * @throws IOException if an I/O error occurs
     * @since 1.22
     */
    public void expand(final ZipFile archive, final Path targetDirectory)
        throws IOException {
        final Enumeration<ZipArchiveEntry> entries = archive.getEntries();
        expand(() -> {
            ZipArchiveEntry next = entries.hasMoreElements() ? entries.nextElement() : null;
            while (next != null && !archive.canReadEntryData(next)) {
                next = entries.hasMoreElements() ? entries.nextElement() : null;
            }
            return next;
        }, (entry, out) -> {
            try (InputStream in = archive.getInputStream((ZipArchiveEntry) entry)) {
                IOUtils.copy(in, out);
            }
        }, targetDirectory);
    }

    private boolean prefersSeekableByteChannel(final String format) {
        return ArchiveStreamFactory.TAR.equalsIgnoreCase(format)
            || ArchiveStreamFactory.ZIP.equalsIgnoreCase(format)
            || ArchiveStreamFactory.SEVEN_Z.equalsIgnoreCase(format);
    }

    private Path toPath(final File targetDirectory) {
        return targetDirectory != null ? targetDirectory.toPath() : null;
    }

}
