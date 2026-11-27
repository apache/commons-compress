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
package org.apache.commons.compress.archivers.examples;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;

/**
 * Extracts regular-file hard links after their payloads, without resolving targets against pre-existing filesystem objects.
 */
final class ZipHardLinkExpander {

    /**
     * Rejects existing symbolic links and objects of an incompatible destination type.
     */
    private static void checkPath(final Path path, final boolean directory) throws IOException {
        // Inspect the path itself; a missing destination can be created later.
        final BasicFileAttributes attributes;
        try {
            attributes = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        } catch (final NoSuchFileException ex) {
            return;
        }
        if (attributes.isSymbolicLink() || (directory ? !attributes.isDirectory() : !attributes.isRegularFile())) {
            throw new ArchiveException("Incompatible ZIP extraction destination '%s'", path);
        }
    }

    /**
     * Checks the destination and requires every existing ancestor to be a real directory.
     */
    private static void checkPathAndParents(final Path path, final boolean directory) throws IOException {
        checkPath(path, directory);
        // A safe leaf is insufficient if an ancestor redirects extraction through a symlink.
        for (Path parent = path.getParent(); parent != null; parent = parent.getParent()) {
            checkPath(parent, true);
        }
    }

    /**
     * Rejects filesystem aliases of destinations already written by this extraction.
     */
    private static void checkWrittenPath(final Path path, final Set<Path> writtenPaths) throws IOException {
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS) && writtenPaths.contains(path.toRealPath())) {
            throw new ArchiveException("Conflicting ZIP extraction destination '%s'", path);
        }
    }

    /**
     * Extracts payloads before hard links, returning false when ordinary extraction suffices.
     */
    static boolean expandIfNeeded(final ZipFile archive, final Path targetDirectory) throws IOException {
        // Resolve every reference before writing; broken links must not become empty files.
        final List<? extends ZipArchiveEntry> entries = archive.entries();
        final Map<ZipArchiveEntry, ZipArchiveEntry> links = archive.resolveUnixHardLinks();
        for (final ZipArchiveEntry target : links.values()) {
            if (!archive.canReadEntryData(target)) {
                throw new ArchiveException("Unreadable UNIX hard link target '%s'", target.getName());
            }
        }
        if (links.isEmpty()) {
            return false;
        }
        // Validate all destination paths before the first filesystem write.
        final Map<ZipArchiveEntry, Path> paths = targetDirectory == null ? null : preparePaths(entries, targetDirectory);
        // Path.equals need not reflect the filesystem's case or Unicode normalization rules.
        final Set<Path> writtenPaths = new HashSet<>();
        // Write payloads first so both forward references and chains have final targets.
        for (final ZipArchiveEntry entry : entries) {
            if (links.containsKey(entry) || !archive.canReadEntryData(entry)) {
                continue;
            }
            if (targetDirectory == null) {
                // Preserve the null-destination mode by consuming data without creating files.
                if (!entry.isDirectory()) {
                    try (InputStream in = archive.getInputStream(entry)) {
                        IOUtils.copy(in, NullOutputStream.INSTANCE);
                    }
                }
            } else {
                // Recheck destinations and reject aliases before creating each output object.
                final Path path = paths.get(entry);
                checkPathAndParents(path, entry.isDirectory());
                checkWrittenPath(path, writtenPaths);
                if (entry.isDirectory()) {
                    Files.createDirectories(path);
                } else {
                    prepareFile(path);
                    try (InputStream in = archive.getInputStream(entry);
                            OutputStream out = Files.newOutputStream(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
                        IOUtils.copy(in, out);
                    }
                }
                writtenPaths.add(path.toRealPath());
            }
        }
        // All terminal targets now exist; create links without copying bytes as a fallback.
        if (paths != null) {
            for (final ZipArchiveEntry entry : entries) {
                final ZipArchiveEntry target = links.get(entry);
                if (target != null) {
                    final Path path = paths.get(entry);
                    final Path targetPath = paths.get(target);
                    checkPathAndParents(targetPath, false);
                    checkWrittenPath(path, writtenPaths);
                    prepareFile(path);
                    try {
                        Files.createLink(path, targetPath);
                    } catch (final UnsupportedOperationException ex) {
                        throw new IOException("File system cannot create ZIP hard link " + path, ex);
                    }
                    writtenPaths.add(path.toRealPath());
                }
            }
        }
        return true;
    }

    /**
     * Prepares a destination path without changing any existing inode's contents.
     */
    private static void prepareFile(final Path path) throws IOException {
        checkPathAndParents(path, false);
        Files.createDirectories(path.getParent());
        // Unlink rather than truncate: an existing output may itself be linked to an unrelated file.
        Files.deleteIfExists(path);
    }

    /**
     * Resolves archive paths and rejects escapes, collisions and file/directory conflicts.
     */
    private static Map<ZipArchiveEntry, Path> preparePaths(final List<? extends ZipArchiveEntry> entries, final Path targetDirectory)
            throws IOException {
        // Check the normalized root before resolving any archive member beneath it.
        final Path root = targetDirectory.toAbsolutePath().normalize();
        checkPathAndParents(root, true);
        final Map<ZipArchiveEntry, Path> paths = new IdentityHashMap<>();
        final Map<Path, ZipArchiveEntry> destinations = new HashMap<>();
        // Each member must have a unique, compatible destination inside the root.
        for (final ZipArchiveEntry entry : entries) {
            final Path path = entry.resolveIn(root);
            if (path.equals(root) && !entry.isDirectory() || destinations.putIfAbsent(path, entry) != null) {
                throw new ArchiveException("Conflicting ZIP extraction destination '%s'", path);
            }
            checkPathAndParents(path, entry.isDirectory());
            paths.put(entry, path);
        }
        // A member cannot be both a file and another member's parent directory.
        for (final Path path : destinations.keySet()) {
            for (Path parent = path.getParent(); parent != null && parent.startsWith(root); parent = parent.getParent()) {
                final ZipArchiveEntry entry = destinations.get(parent);
                if (entry != null && !entry.isDirectory()) {
                    throw new ArchiveException("ZIP extraction destination '%s' is also a parent directory", parent);
                }
            }
        }
        return paths;
    }

    /**
     * Prevents instantiation of this utility class.
     */
    private ZipHardLinkExpander() {
    }
}
