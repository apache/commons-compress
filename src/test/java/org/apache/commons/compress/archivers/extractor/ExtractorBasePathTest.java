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

import static org.apache.commons.compress.archivers.extractor.Fixtures.Entry.dir;
import static org.apache.commons.compress.archivers.extractor.Fixtures.Entry.file;
import static org.apache.commons.compress.archivers.extractor.Fixtures.Entry.hardlink;
import static org.apache.commons.compress.archivers.extractor.Fixtures.Entry.symlink;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.compress.archivers.tar.TarFile;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Exercises the best-effort base {@link Extractor} directly (not through the factory, which on Linux returns the secure
 * subclass that overrides the file write) so the base path and the {@link TarFile} / {@link ZipFile} entry points are covered
 * on every platform, including the gate platform.
 */
class ExtractorBasePathTest {

    @TempDir
    Path target;

    private Extractor base() throws Exception {
        return new Extractor(target.toRealPath());
    }

    @Test
    void baseCreatesWithinRootSymlink() throws Exception {
        final byte[] data = Fixtures.tar(dir("a"), dir("b"), symlink("a/link", "../b"));
        Fixtures.extractTar(base().setSymlinkPolicy(SymlinkPolicy.ALLOW_WITHIN_ROOT), data);
        assertTrue(Files.isSymbolicLink(target.resolve("a/link")));
    }

    @Test
    void baseOverwriteTrueReplaces() throws Exception {
        Files.write(target.resolve("a.txt"), "old".getBytes(StandardCharsets.UTF_8));
        final byte[] data = Fixtures.tar(file("a.txt", "new"));
        Fixtures.extractTar(base().setOverwrite(true), data);
        assertArrayEquals("new".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(target.resolve("a.txt")));
    }

    @Test
    void extractsViaTarFile() throws Exception {
        final byte[] data = Fixtures.tar(dir("a"), file("a/b.txt", "tarfile"));
        try (TarFile archive = Fixtures.openTar(data)) {
            base().extract(archive);
        }
        assertArrayEquals("tarfile".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(target.resolve("a/b.txt")));
    }

    @Test
    void extractsViaZipFile() throws Exception {
        final byte[] data = Fixtures.zip(dir("a"), file("a/b.txt", "zipfile"));
        try (ZipFile archive = Fixtures.openZip(data)) {
            base().extract(archive);
        }
        assertArrayEquals("zipfile".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(target.resolve("a/b.txt")));
    }

    @Test
    void extractsHardLinkViaTarFile() throws Exception {
        final byte[] data = Fixtures.tar(file("real.txt", "data"), hardlink("link", "real.txt"));
        try (TarFile archive = Fixtures.openTar(data)) {
            base().extract(archive);
        }
        assertTrue(Files.exists(target.resolve("link")));
    }

    @Test
    void extractsSymlinkViaTarFile() throws Exception {
        final byte[] data = Fixtures.tar(dir("b"), symlink("link", "b"));
        try (TarFile archive = Fixtures.openTar(data)) {
            base().setSymlinkPolicy(SymlinkPolicy.ALLOW_WITHIN_ROOT).extract(archive);
        }
        assertTrue(Files.isSymbolicLink(target.resolve("link")));
    }

    @Test
    void extractsSymlinkViaZipFile() throws Exception {
        final byte[] data = Fixtures.zip(dir("b"), symlink("link", "b"));
        try (ZipFile archive = Fixtures.openZip(data)) {
            base().setSymlinkPolicy(SymlinkPolicy.ALLOW_WITHIN_ROOT).extract(archive);
        }
        assertTrue(Files.isSymbolicLink(target.resolve("link")));
    }

    @Test
    void createsStandaloneDirectory() throws Exception {
        final byte[] data = Fixtures.tar(dir("emptydir"));
        Fixtures.extractTar(base(), data);
        assertTrue(Files.isDirectory(target.resolve("emptydir")));
    }
}
