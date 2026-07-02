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
package org.apache.commons.compress.archivers.extractor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.Objects;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarFile;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.IOUtils;

/**
 * Safely materializes an archive into a target directory, including symbolic links, while resisting both symlink-slip from
 * the archive itself and concurrent symlink races from third parties.
 * <p>
 * Obtain an instance with {@link #newExtractor(Path)}, which returns the strongest implementation the platform supports: a
 * race-safe extractor backed by {@link SecureDirectoryStream} where the file system provides one (Linux), otherwise this
 * best-effort base implementation. The base implementation resolves every path component with
 * {@link LinkOption#NOFOLLOW_LINKS} and writes regular files {@code CREATE_NEW}, which stops symlink-slip from the archive and
 * silent overwrites, but it cannot fully close a concurrent time-of-check-to-time-of-use race; that residual is documented
 * and only the secure implementation removes it.
 * </p>
 * <p>
 * Defaults are conservative: symbolic-link entries are rejected ({@link SymlinkPolicy#REJECT}), special entries are skipped
 * ({@link SpecialFilePolicy#SKIP}), and existing files are not overwritten. Instances are not thread-safe: configure an
 * instance before extracting and do not share it across concurrent extractions.
 * </p>
 *
 * @since 1.29.0
 */
public class Extractor {

    enum EntryType {
        FILE, DIRECTORY, SYMLINK, HARD_LINK, SPECIAL
    }

    /**
     * Creates an extractor for {@code targetDirectory}, returning a race-safe implementation when the platform's file system
     * provider exposes a {@link SecureDirectoryStream} (Linux), otherwise a best-effort implementation.
     *
     * @param targetDirectory an existing directory into which archives are extracted; resolved to its real path.
     * @return an extractor bound to {@code targetDirectory}.
     * @throws IOException if {@code targetDirectory} does not exist or is not a directory.
     * @since 1.29.0
     */
    public static Extractor newExtractor(final Path targetDirectory) throws IOException {
        final Path root = Objects.requireNonNull(targetDirectory, "targetDirectory").toRealPath();
        if (!Files.isDirectory(root)) {
            throw new IOException("Target is not a directory: " + targetDirectory);
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
            if (stream instanceof SecureDirectoryStream) {
                return new SecureExtractor(root);
            }
        }
        return new Extractor(root);
    }

    /** The validated, canonical extraction root. */
    final Path rootDirectory;

    private SymlinkPolicy symlinkPolicy = SymlinkPolicy.REJECT;

    private SpecialFilePolicy specialFilePolicy = SpecialFilePolicy.SKIP;

    private boolean overwrite;

    private Runnable beforeLeafWrite;

    Extractor(final Path rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    private static EntryType classify(final ArchiveEntry entry) {
        if (entry instanceof TarArchiveEntry) {
            final TarArchiveEntry tar = (TarArchiveEntry) entry;
            if (tar.isSymbolicLink()) {
                return EntryType.SYMLINK;
            }
            if (tar.isLink()) {
                return EntryType.HARD_LINK;
            }
            if (tar.isBlockDevice() || tar.isCharacterDevice() || tar.isFIFO()) {
                return EntryType.SPECIAL;
            }
            return tar.isDirectory() ? EntryType.DIRECTORY : EntryType.FILE;
        }
        if (entry instanceof ZipArchiveEntry && ((ZipArchiveEntry) entry).isUnixSymlink()) {
            return EntryType.SYMLINK;
        }
        return entry.isDirectory() ? EntryType.DIRECTORY : EntryType.FILE;
    }

    /**
     * Extracts the entries of an {@link ArchiveInputStream}.
     * <p>
     * Zip unix symbolic links are recognized only through {@link #extract(ZipFile)}: their unix mode lives in the central
     * directory, which a stream does not expose, so a zip symbolic link presented through a stream is materialized as a
     * regular file (no-follow, create-new) rather than a link.
     * </p>
     *
     * @param archive the archive to extract; not closed by this method.
     * @throws IOException if extraction fails or a policy rejects an entry.
     * @since 1.29.0
     */
    public void extract(final ArchiveInputStream<?> archive) throws IOException {
        ArchiveEntry entry;
        while ((entry = archive.getNextEntry()) != null) {
            if (!archive.canReadEntryData(entry)) {
                continue;
            }
            final EntryType type = classify(entry);
            String linkTarget = null;
            if (type == EntryType.SYMLINK || type == EntryType.HARD_LINK) {
                // Only tar carries link metadata through a stream: a zip unix symbolic link is recognized solely from the
                // central directory (see extract(ZipFile)), which a stream does not expose, so classify never reports a
                // streamed entry as a link unless it is a TarArchiveEntry.
                linkTarget = ((TarArchiveEntry) entry).getLinkName();
            }
            process(entry.getName(), type, linkTarget, type == EntryType.FILE ? archive : null);
        }
    }

    /**
     * Extracts the entries of a {@link TarFile}.
     *
     * @param archive the archive to extract; not closed by this method.
     * @throws IOException if extraction fails or a policy rejects an entry.
     * @since 1.29.0
     */
    public void extract(final TarFile archive) throws IOException {
        for (final TarArchiveEntry entry : archive.getEntries()) {
            final EntryType type = classify(entry);
            if (type == EntryType.FILE) {
                try (InputStream content = archive.getInputStream(entry)) {
                    process(entry.getName(), type, null, content);
                }
            } else {
                final String linkTarget = type == EntryType.SYMLINK || type == EntryType.HARD_LINK ? entry.getLinkName() : null;
                process(entry.getName(), type, linkTarget, null);
            }
        }
    }

    /**
     * Extracts the entries of a {@link ZipFile}.
     *
     * @param archive the archive to extract; not closed by this method.
     * @throws IOException if extraction fails or a policy rejects an entry.
     * @since 1.29.0
     */
    public void extract(final ZipFile archive) throws IOException {
        final Enumeration<ZipArchiveEntry> entries = archive.getEntries();
        while (entries.hasMoreElements()) {
            final ZipArchiveEntry entry = entries.nextElement();
            if (!archive.canReadEntryData(entry)) {
                continue;
            }
            final EntryType type = classify(entry);
            if (type == EntryType.FILE) {
                try (InputStream content = archive.getInputStream(entry)) {
                    process(entry.getName(), type, null, content);
                }
            } else {
                final String linkTarget = type == EntryType.SYMLINK ? archive.getUnixSymlink(entry) : null;
                process(entry.getName(), type, linkTarget, null);
            }
        }
    }

    boolean isOverwrite() {
        return overwrite;
    }

    final void runBeforeLeafWrite() {
        if (beforeLeafWrite != null) {
            beforeLeafWrite.run();
        }
    }

    final void setBeforeLeafWrite(final Runnable hook) {
        this.beforeLeafWrite = hook;
    }

    /**
     * Tests whether {@code path} is contained within the canonical extraction root, comparing component by component so a
     * sibling that merely shares a name prefix (for example {@code root-old} beside {@code root}) is not treated as contained.
     */
    private boolean isWithinRoot(final Path path) {
        return path.startsWith(rootDirectory);
    }

    /**
     * Resolves {@code name} against the extraction root and applies the lexical zip-slip guard.
     *
     * @return the resolved path within the root, or {@code null} if {@code name} resolves to the root itself (for example
     *         {@code a/..}), which carries nothing to materialize; the caller skips such entries rather than writing at or
     *         replacing the root.
     * @throws ArchiveException if the resolved path escapes the extraction root.
     */
    private Path resolveWithinRoot(final String name) throws ArchiveException {
        final Path resolved = rootDirectory.resolve(name).normalize();
        if (resolved.equals(rootDirectory)) {
            return null;
        }
        if (!isWithinRoot(resolved)) {
            throw new ArchiveException("Entry '%s' would escape the extraction root", name);
        }
        return resolved;
    }

    /**
     * Walks the path from the root to {@code directory}, creating missing components and refusing to traverse any existing
     * component that is a symbolic link or a non-directory. This is the best-effort, non-atomic parent resolution; the secure
     * implementation overrides the leaf operations to be descriptor-relative.
     */
    void ensureDirectory(final Path directory) throws IOException {
        if (directory == null || directory.equals(rootDirectory)) {
            return;
        }
        Path current = rootDirectory;
        for (final Path component : rootDirectory.relativize(directory)) {
            current = current.resolve(component);
            final BasicFileAttributes attrs;
            try {
                attrs = Files.readAttributes(current, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            } catch (final NoSuchFileException e) {
                Files.createDirectory(current);
                continue;
            }
            if (attrs.isSymbolicLink()) {
                throw new ArchiveException("Refusing to traverse symbolic link: %s", current);
            }
            if (!attrs.isDirectory()) {
                throw new ArchiveException("Parent path component is not a directory: %s", current);
            }
        }
    }

    private void process(final String name, final EntryType type, final String linkTarget, final InputStream content) throws IOException {
        try {
            processEntry(name, type, linkTarget, content);
        } catch (final InvalidPathException e) {
            // A name or link target that cannot become a Path on this platform (for example one containing a NUL byte) is
            // rejected as a malformed entry rather than propagated as an unchecked exception that would abort extraction.
            throw new ArchiveException("Invalid entry name: " + name, e);
        }
    }

    private void processEntry(final String name, final EntryType type, final String linkTarget, final InputStream content) throws IOException {
        if (type == EntryType.SPECIAL) {
            if (specialFilePolicy == SpecialFilePolicy.REJECT) {
                throw new ArchiveException("Special entry not allowed: %s", name);
            }
            return;
        }
        if (type == EntryType.SYMLINK) {
            if (symlinkPolicy == SymlinkPolicy.REJECT) {
                throw new ArchiveException("Symbolic link not allowed: %s", name);
            }
            if (symlinkPolicy == SymlinkPolicy.SKIP) {
                return;
            }
        }
        final Path leaf = resolveWithinRoot(name);
        if (leaf == null) {
            // The entry resolves to the extraction root itself; there is nothing to materialize and writing here would
            // target or replace the root, so skip it.
            return;
        }
        ensureDirectory(leaf.getParent());
        switch (type) {
        case DIRECTORY:
            ensureDirectory(leaf);
            break;
        case FILE:
            writeFile(leaf, content);
            break;
        case SYMLINK:
            writeSymbolicLink(leaf, linkTarget);
            break;
        case HARD_LINK:
            writeHardLink(leaf, linkTarget);
            break;
        default:
            throw new ArchiveException("Unsupported entry type for '%s'", name);
        }
    }

    /**
     * Sets whether existing files may be overwritten. When {@code false} (the default) an entry whose target already exists
     * fails the extraction; the write never follows an existing symbolic link at the target path.
     *
     * @param overwrite whether to overwrite existing files.
     * @return {@code this}.
     * @since 1.29.0
     */
    public Extractor setOverwrite(final boolean overwrite) {
        this.overwrite = overwrite;
        return this;
    }

    /**
     * Sets the policy for special entries (devices, FIFOs). Defaults to {@link SpecialFilePolicy#SKIP}.
     *
     * @param specialFilePolicy the policy to apply; not null.
     * @return {@code this}.
     * @since 1.29.0
     */
    public Extractor setSpecialFilePolicy(final SpecialFilePolicy specialFilePolicy) {
        this.specialFilePolicy = Objects.requireNonNull(specialFilePolicy, "specialFilePolicy");
        return this;
    }

    /**
     * Sets the policy for symbolic-link entries. Defaults to {@link SymlinkPolicy#REJECT}.
     *
     * @param symlinkPolicy the policy to apply; not null.
     * @return {@code this}.
     * @since 1.29.0
     */
    public Extractor setSymlinkPolicy(final SymlinkPolicy symlinkPolicy) {
        this.symlinkPolicy = Objects.requireNonNull(symlinkPolicy, "symlinkPolicy");
        return this;
    }

    /**
     * Writes a regular file at {@code leaf} with no-follow semantics, refusing to follow an existing symbolic link at that
     * path. With overwrite disabled the open is {@code CREATE_NEW}, so an existing entry fails the extraction.
     */
    void writeFile(final Path leaf, final InputStream content) throws IOException {
        runBeforeLeafWrite();
        final OutputStream out = overwrite
                ? Channels.newOutputStream(Files.newByteChannel(leaf, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                        StandardOpenOption.TRUNCATE_EXISTING, LinkOption.NOFOLLOW_LINKS))
                : Channels.newOutputStream(Files.newByteChannel(leaf, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE,
                        LinkOption.NOFOLLOW_LINKS));
        try (OutputStream sink = out) {
            IOUtils.copy(content, sink);
        }
    }

    /**
     * Creates a hard link at {@code leaf} to {@code linkTarget}, which is resolved against the extraction root and rejected if
     * it escapes. The target is re-resolved with {@link Path#toRealPath} and re-checked before {@link Files#createLink}, which
     * closes the hard-link-through-symlink escape. A residual time-of-check-to-time-of-use window remains between that re-check
     * and the link creation, the same non-atomic residual as symbolic-link creation, as {@code java.nio} exposes no
     * descriptor-relative {@code linkat} for the secure implementation to use.
     */
    void writeHardLink(final Path leaf, final String linkTarget) throws IOException {
        final Path resolved = rootDirectory.resolve(linkTarget).normalize();
        if (!isWithinRoot(resolved)) {
            throw new ArchiveException("Hard link target escapes the extraction root: %s -> %s", leaf, linkTarget);
        }
        // The lexical check is not enough: a target routed through a symbolic link (planted or from an earlier entry) would
        // be followed by createLink and escape the root. Re-resolve on disk and re-check containment, then link the real file.
        final Path real = resolved.toRealPath();
        if (!isWithinRoot(real)) {
            throw new ArchiveException("Hard link target resolves outside the extraction root: %s -> %s", leaf, linkTarget);
        }
        Files.createLink(leaf, real);
    }

    /**
     * Creates a symbolic link at {@code leaf} pointing at {@code linkTarget}. Under {@link SymlinkPolicy#ALLOW_WITHIN_ROOT} a
     * target that resolves outside the root is rejected; under {@link SymlinkPolicy#ALLOW_ALL} the link is created verbatim.
     */
    void writeSymbolicLink(final Path leaf, final String linkTarget) throws IOException {
        if (symlinkPolicy == SymlinkPolicy.ALLOW_WITHIN_ROOT) {
            final Path parent = leaf.getParent();
            final Path resolved = (parent != null ? parent : rootDirectory).resolve(linkTarget).normalize();
            if (!isWithinRoot(resolved)) {
                throw new ArchiveException("Symbolic link target escapes the extraction root: %s -> %s", leaf, linkTarget);
            }
        }
        Files.createSymbolicLink(leaf, leaf.getFileSystem().getPath(linkTarget));
    }
}
