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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.Objects;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;

/**
 * Provides a high level API for creating archives.
 *
 * @since 1.17
 * @since 1.21 Supports {@link Path}.
 */
public class Archiver {

    private static class ArchiverFileVisitor extends SimpleFileVisitor<Path> {

        private final ArchiveOutputStream target;
        private final Path directory;
        private final LinkOption[] linkOptions;

        private ArchiverFileVisitor(final ArchiveOutputStream target, final Path directory,
            final LinkOption... linkOptions) {
            this.target = target;
            this.directory = directory;
            this.linkOptions = linkOptions == null ? IOUtils.EMPTY_LINK_OPTIONS : linkOptions.clone();
        }

        @Override
        public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
            return visit(dir, attrs, false);
        }

        protected FileVisitResult visit(final Path path, final BasicFileAttributes attrs, final boolean isFile)
            throws IOException {
            Objects.requireNonNull(path);
            Objects.requireNonNull(attrs);
            final String name = directory.relativize(path).toString().replace('\\', '/');
            if (!name.isEmpty()) {
                final ArchiveEntry archiveEntry = target.createArchiveEntry(path,
                    isFile || name.endsWith("/") ? name : name + "/", linkOptions);
                target.putArchiveEntry(archiveEntry);
                if (isFile) {
                    // Refactor this as a BiConsumer on Java 8
                    Files.copy(path, target);
                }
                target.closeArchiveEntry();
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
            return visit(file, attrs, true);
        }
    }

    /**
     * No {@link FileVisitOption}.
     */
    public static final EnumSet<FileVisitOption> EMPTY_FileVisitOption = EnumSet.noneOf(FileVisitOption.class);

    /**
     * Creates an archive {@code target} by recursively including all files and directories in {@code directory}.
     *
     * @param target the stream to write the new archive to.
     * @param directory the directory that contains the files to archive.
     * @throws IOException if an I/O error occurs
     */
    public void create(final ArchiveOutputStream target, final File directory) throws IOException {
        create(target, directory.toPath(), EMPTY_FileVisitOption);
    }

    /**
     * Creates an archive {@code target} by recursively including all files and directories in {@code directory}.
     *
     * @param target the stream to write the new archive to.
     * @param directory the directory that contains the files to archive.
     * @throws IOException if an I/O error occurs or the archive cannot be created for other reasons.
     * @since 1.21
     */
    public void create(final ArchiveOutputStream target, final Path directory) throws IOException {
        create(target, directory, EMPTY_FileVisitOption);
    }

    /**
     * Creates an archive {@code target} by recursively including all files and directories in {@code directory}.
     *
     * @param target the stream to write the new archive to.
     * @param directory the directory that contains the files to archive.
     * @param fileVisitOptions linkOptions to configure the traversal of the source {@code directory}.
     * @param linkOptions indicating how symbolic links are handled.
     * @throws IOException if an I/O error occurs or the archive cannot be created for other reasons.
     * @since 1.21
     */
    public void create(final ArchiveOutputStream target, final Path directory,
        final EnumSet<FileVisitOption> fileVisitOptions, final LinkOption... linkOptions) throws IOException {
        Files.walkFileTree(directory, fileVisitOptions, Integer.MAX_VALUE,
            new ArchiverFileVisitor(target, directory, linkOptions));
        target.finish();
    }

    /**
     * Creates an archive {@code target} by recursively including all files and directories in {@code directory}.
     *
     * @param target the file to write the new archive to.
     * @param directory the directory that contains the files to archive.
     * @throws IOException if an I/O error occurs
     */
    public void create(final SevenZOutputFile target, final File directory) throws IOException {
        create(target, directory.toPath());
    }

    /**
     * Creates an archive {@code target} by recursively including all files and directories in {@code directory}.
     *
     * @param target the file to write the new archive to.
     * @param directory the directory that contains the files to archive.
     * @throws IOException if an I/O error occurs
     * @since 1.21
     */
    public void create(final SevenZOutputFile target, final Path directory) throws IOException {
        // This custom SimpleFileVisitor goes away with Java 8's BiConsumer.
        Files.walkFileTree(directory, new ArchiverFileVisitor(null, directory) {

            @Override
            protected FileVisitResult visit(final Path path, final BasicFileAttributes attrs, final boolean isFile)
                throws IOException {
                Objects.requireNonNull(path);
                Objects.requireNonNull(attrs);
                final String name = directory.relativize(path).toString().replace('\\', '/');
                if (!name.isEmpty()) {
                    final ArchiveEntry archiveEntry = target.createArchiveEntry(path,
                        isFile || name.endsWith("/") ? name : name + "/");
                    target.putArchiveEntry(archiveEntry);
                    if (isFile) {
                        // Refactor this as a BiConsumer on Java 8
                        target.write(path);
                    }
                    target.closeArchiveEntry();
                }
                return FileVisitResult.CONTINUE;
            }

        });
        target.finish();
    }

    /**
     * Creates an archive {@code target} using the format {@code
     * format} by recursively including all files and directories in {@code directory}.
     *
     * @param format the archive format. This uses the same format as accepted by {@link ArchiveStreamFactory}.
     * @param target the file to write the new archive to.
     * @param directory the directory that contains the files to archive.
     * @throws IOException if an I/O error occurs
     * @throws ArchiveException if the archive cannot be created for other reasons
     */
    public void create(final String format, final File target, final File directory)
        throws IOException, ArchiveException {
        create(format, target.toPath(), directory.toPath());
    }

    /**
     * Creates an archive {@code target} using the format {@code
     * format} by recursively including all files and directories in {@code directory}.
     *
     * <p>
     * This method creates a wrapper around the target stream which is never closed and thus leaks resources, please use
     * {@link #create(String,OutputStream,File,CloseableConsumer)} instead.
     * </p>
     *
     * @param format the archive format. This uses the same format as accepted by {@link ArchiveStreamFactory}.
     * @param target the stream to write the new archive to.
     * @param directory the directory that contains the files to archive.
     * @throws IOException if an I/O error occurs
     * @throws ArchiveException if the archive cannot be created for other reasons
     * @deprecated this method leaks resources
     */
    @Deprecated
    public void create(final String format, final OutputStream target, final File directory)
        throws IOException, ArchiveException {
        create(format, target, directory, CloseableConsumer.NULL_CONSUMER);
    }

    /**
     * Creates an archive {@code target} using the format {@code
     * format} by recursively including all files and directories in {@code directory}.
     *
     * <p>
     * This method creates a wrapper around the archive stream and the caller of this method is responsible for closing
     * it - probably at the same time as closing the stream itself. The caller is informed about the wrapper object via
     * the {@code
     * closeableConsumer} callback as soon as it is no longer needed by this class.
     * </p>
     *
     * @param format the archive format. This uses the same format as accepted by {@link ArchiveStreamFactory}.
     * @param target the stream to write the new archive to.
     * @param directory the directory that contains the files to archive.
     * @param closeableConsumer is informed about the stream wrapped around the passed in stream
     * @throws IOException if an I/O error occurs
     * @throws ArchiveException if the archive cannot be created for other reasons
     * @since 1.19
     */
    public void create(final String format, final OutputStream target, final File directory,
        final CloseableConsumer closeableConsumer) throws IOException, ArchiveException {
        try (CloseableConsumerAdapter c = new CloseableConsumerAdapter(closeableConsumer)) {
            create(c.track(ArchiveStreamFactory.DEFAULT.createArchiveOutputStream(format, target)), directory);
        }
    }

    /**
     * Creates an archive {@code target} using the format {@code
     * format} by recursively including all files and directories in {@code directory}.
     *
     * @param format the archive format. This uses the same format as accepted by {@link ArchiveStreamFactory}.
     * @param target the file to write the new archive to.
     * @param directory the directory that contains the files to archive.
     * @throws IOException if an I/O error occurs
     * @throws ArchiveException if the archive cannot be created for other reasons
     * @since 1.21
     */
    public void create(final String format, final Path target, final Path directory)
        throws IOException, ArchiveException {
        if (prefersSeekableByteChannel(format)) {
            try (SeekableByteChannel channel = FileChannel.open(target, StandardOpenOption.WRITE,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                create(format, channel, directory);
                return;
            }
        }
        try (@SuppressWarnings("resource") // ArchiveOutputStream wraps newOutputStream result
        ArchiveOutputStream outputStream = ArchiveStreamFactory.DEFAULT.createArchiveOutputStream(format,
            Files.newOutputStream(target))) {
            create(outputStream, directory, EMPTY_FileVisitOption);
        }
    }

    /**
     * Creates an archive {@code target} using the format {@code
     * format} by recursively including all files and directories in {@code directory}.
     *
     * <p>
     * This method creates a wrapper around the target channel which is never closed and thus leaks resources, please
     * use {@link #create(String,SeekableByteChannel,File,CloseableConsumer)} instead.
     * </p>
     *
     * @param format the archive format. This uses the same format as accepted by {@link ArchiveStreamFactory}.
     * @param target the channel to write the new archive to.
     * @param directory the directory that contains the files to archive.
     * @throws IOException if an I/O error occurs
     * @throws ArchiveException if the archive cannot be created for other reasons
     * @deprecated this method leaks resources
     */
    @Deprecated
    public void create(final String format, final SeekableByteChannel target, final File directory)
        throws IOException, ArchiveException {
        create(format, target, directory, CloseableConsumer.NULL_CONSUMER);
    }

    /**
     * Creates an archive {@code target} using the format {@code
     * format} by recursively including all files and directories in {@code directory}.
     *
     * <p>
     * This method creates a wrapper around the archive channel and the caller of this method is responsible for closing
     * it - probably at the same time as closing the channel itself. The caller is informed about the wrapper object via
     * the {@code
     * closeableConsumer} callback as soon as it is no longer needed by this class.
     * </p>
     *
     * @param format the archive format. This uses the same format as accepted by {@link ArchiveStreamFactory}.
     * @param target the channel to write the new archive to.
     * @param directory the directory that contains the files to archive.
     * @param closeableConsumer is informed about the stream wrapped around the passed in stream
     * @throws IOException if an I/O error occurs
     * @throws ArchiveException if the archive cannot be created for other reasons
     * @since 1.19
     */
    public void create(final String format, final SeekableByteChannel target, final File directory,
        final CloseableConsumer closeableConsumer) throws IOException, ArchiveException {
        try (CloseableConsumerAdapter c = new CloseableConsumerAdapter(closeableConsumer)) {
            if (!prefersSeekableByteChannel(format)) {
                create(format, c.track(Channels.newOutputStream(target)), directory);
            } else if (ArchiveStreamFactory.ZIP.equalsIgnoreCase(format)) {
                create(c.track(new ZipArchiveOutputStream(target)), directory);
            } else if (ArchiveStreamFactory.SEVEN_Z.equalsIgnoreCase(format)) {
                create(c.track(new SevenZOutputFile(target)), directory);
            } else {
                // never reached as prefersSeekableByteChannel only returns true for ZIP and 7z
                throw new ArchiveException("Don't know how to handle format " + format);
            }
        }
    }

    /**
     * Creates an archive {@code target} using the format {@code
     * format} by recursively including all files and directories in {@code directory}.
     *
     * @param format the archive format. This uses the same format as accepted by {@link ArchiveStreamFactory}.
     * @param target the channel to write the new archive to.
     * @param directory the directory that contains the files to archive.
     * @throws IOException if an I/O error occurs
     * @throws IllegalStateException if the format does not support {@code SeekableByteChannel}.
     */
    public void create(final String format, final SeekableByteChannel target, final Path directory) throws IOException {
        if (ArchiveStreamFactory.SEVEN_Z.equalsIgnoreCase(format)) {
            try (SevenZOutputFile sevenZFile = new SevenZOutputFile(target)) {
                create(sevenZFile, directory);
            }
        } else if (ArchiveStreamFactory.ZIP.equalsIgnoreCase(format)) {
            try (ArchiveOutputStream archiveOutputStream = new ZipArchiveOutputStream(target)) {
                create(archiveOutputStream, directory, EMPTY_FileVisitOption);
            }
        } else {
            throw new IllegalStateException(format);
        }
    }

    private boolean prefersSeekableByteChannel(final String format) {
        return ArchiveStreamFactory.ZIP.equalsIgnoreCase(format)
            || ArchiveStreamFactory.SEVEN_Z.equalsIgnoreCase(format);
    }
}
