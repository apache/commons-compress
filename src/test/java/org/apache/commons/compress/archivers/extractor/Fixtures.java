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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;
import org.apache.commons.compress.archivers.tar.TarFile;
import org.apache.commons.compress.archivers.zip.UnixStat;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;

/**
 * Builds in-memory tar and zip archives with arbitrary entry types (regular, directory, symbolic link, hard link, special)
 * for extractor tests. Entries are written with the public archive writers, which faithfully encode link flags and unix
 * modes. {@link Entry#rawFile} preserves a leading {@code /} (the writer strips it by default), so an absolute-path escape can
 * be exercised. Duplicate-name and collision fixtures (not used in this public suite) would need low-level construction.
 */
final class Fixtures {

    enum Kind {
        FILE, DIR, SYMLINK, HARDLINK, CHARDEV, BLOCKDEV, FIFO
    }

    static final class Entry {

        static Entry blockDev(final String name) {
            return new Entry(Kind.BLOCKDEV, name, null, false);
        }

        static Entry charDev(final String name) {
            return new Entry(Kind.CHARDEV, name, null, false);
        }

        static Entry dir(final String name) {
            return new Entry(Kind.DIR, name, null, false);
        }

        static Entry fifo(final String name) {
            return new Entry(Kind.FIFO, name, null, false);
        }

        static Entry file(final String name, final String content) {
            return new Entry(Kind.FILE, name, content, false);
        }

        static Entry hardlink(final String name, final String target) {
            return new Entry(Kind.HARDLINK, name, target, false);
        }

        static Entry rawFile(final String name, final String content) {
            return new Entry(Kind.FILE, name, content, true);
        }

        static Entry symlink(final String name, final String target) {
            return new Entry(Kind.SYMLINK, name, target, false);
        }

        final Kind kind;
        final String name;
        final String data;
        final boolean preserveAbsolute;

        private Entry(final Kind kind, final String name, final String data, final boolean preserveAbsolute) {
            this.kind = kind;
            this.name = name;
            this.data = data;
            this.preserveAbsolute = preserveAbsolute;
        }
    }

    static void extractTar(final Extractor extractor, final byte[] tar) throws IOException {
        try (TarArchiveInputStream in = new TarArchiveInputStream(new ByteArrayInputStream(tar))) {
            extractor.extract(in);
        }
    }

    static void extractZip(final Extractor extractor, final byte[] zip) throws IOException {
        try (ZipArchiveInputStream in = new ZipArchiveInputStream(new ByteArrayInputStream(zip))) {
            extractor.extract(in);
        }
    }

    static byte[] tar(final Entry... entries) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tos = new TarArchiveOutputStream(bos)) {
            tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            for (final Entry e : entries) {
                final TarArchiveEntry te;
                byte[] content = null;
                switch (e.kind) {
                case FILE:
                    te = e.preserveAbsolute ? new TarArchiveEntry(e.name, TarConstants.LF_NORMAL, true) : new TarArchiveEntry(e.name);
                    content = e.data.getBytes(StandardCharsets.UTF_8);
                    te.setSize(content.length);
                    break;
                case DIR:
                    te = new TarArchiveEntry(e.name.endsWith("/") ? e.name : e.name + "/");
                    break;
                case SYMLINK:
                    te = new TarArchiveEntry(e.name, TarConstants.LF_SYMLINK);
                    te.setLinkName(e.data);
                    break;
                case HARDLINK:
                    te = new TarArchiveEntry(e.name, TarConstants.LF_LINK);
                    te.setLinkName(e.data);
                    break;
                case CHARDEV:
                    te = new TarArchiveEntry(e.name, TarConstants.LF_CHR);
                    break;
                case BLOCKDEV:
                    te = new TarArchiveEntry(e.name, TarConstants.LF_BLK);
                    break;
                case FIFO:
                    te = new TarArchiveEntry(e.name, TarConstants.LF_FIFO);
                    break;
                default:
                    throw new IllegalArgumentException(String.valueOf(e.kind));
                }
                tos.putArchiveEntry(te);
                if (content != null) {
                    tos.write(content);
                }
                tos.closeArchiveEntry();
            }
        }
        return bos.toByteArray();
    }

    static byte[] zip(final Entry... entries) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(bos)) {
            for (final Entry e : entries) {
                final ZipArchiveEntry ze;
                byte[] content = null;
                switch (e.kind) {
                case FILE:
                    ze = new ZipArchiveEntry(e.name);
                    content = e.data.getBytes(StandardCharsets.UTF_8);
                    break;
                case DIR:
                    ze = new ZipArchiveEntry(e.name.endsWith("/") ? e.name : e.name + "/");
                    break;
                case SYMLINK:
                    ze = new ZipArchiveEntry(e.name);
                    ze.setUnixMode(UnixStat.LINK_FLAG | 0777);
                    content = e.data.getBytes(StandardCharsets.UTF_8);
                    break;
                default:
                    throw new IllegalArgumentException("zip fixture does not support " + e.kind);
                }
                zos.putArchiveEntry(ze);
                if (content != null) {
                    zos.write(content);
                }
                zos.closeArchiveEntry();
            }
        }
        return bos.toByteArray();
    }

    static TarFile openTar(final byte[] tar) throws IOException {
        final Path file = Files.createTempFile("compress-extractor-tar", ".tar");
        Files.write(file, tar);
        return TarFile.builder().setPath(file).get();
    }

    static ZipFile openZip(final byte[] zip) throws IOException {
        final Path file = Files.createTempFile("compress-extractor-zip", ".zip");
        Files.write(file, zip);
        return ZipFile.builder().setPath(file).get();
    }

    static Path write(final Path dir, final String name, final byte[] data) throws IOException {
        final Path p = dir.resolve(name);
        Files.write(p, data);
        return p;
    }

    private Fixtures() {
    }
}
