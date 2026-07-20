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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Symlink-slip and duplicate-entry hardening: writes must never traverse a symbolic link in a parent component, whether the
 * link was planted on disk beforehand or created by an earlier archive entry, and a duplicate entry must fail closed rather
 * than overwrite. These are the generic defensive properties that also defeat normalization/case-collision variants, since
 * the defense is a file-system-level no-follow check on the resolved inode, not a lexical name comparison.
 */
class ExtractorSymlinkSlipTest {

    @TempDir
    Path target;

    @TempDir
    Path attacker;

    @Test
    void duplicateEntryFailsClosed() throws Exception {
        final byte[] data = Fixtures.tar(file("a.txt", "first"), file("a.txt", "second"));
        final Extractor extractor = Extractor.newExtractor(target);
        assertThrows(IOException.class, () -> Fixtures.extractTar(extractor, data));
        assertArrayEquals("first".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(target.resolve("a.txt")));
    }

    @Test
    void neverFollowsCreatedSymlinkForLaterWrite() throws Exception {
        final byte[] data = Fixtures.tar(dir("b"), symlink("link", "b"), file("link/evil", "x"));
        final Extractor extractor = Extractor.newExtractor(target).setSymlinkPolicy(SymlinkPolicy.ALLOW_WITHIN_ROOT);
        assertThrows(IOException.class, () -> Fixtures.extractTar(extractor, data));
        assertFalse(Files.exists(target.resolve("b/evil")));
    }

    @Test
    void rejectsWriteThroughPreExistingParentSymlink() throws Exception {
        Files.createSymbolicLink(target.resolve("sub"), attacker);
        final byte[] data = Fixtures.tar(file("sub/file.txt", "x"));
        final Extractor extractor = Extractor.newExtractor(target);
        assertThrows(IOException.class, () -> Fixtures.extractTar(extractor, data));
        assertFalse(Files.exists(attacker.resolve("file.txt")));
    }

    @Test
    void baseRejectsWriteThroughPreExistingParentSymlink() throws Exception {
        Files.createSymbolicLink(target.resolve("sub"), attacker);
        final byte[] data = Fixtures.tar(file("sub/file.txt", "x"));
        final Extractor extractor = new Extractor(target.toRealPath());
        assertThrows(IOException.class, () -> Fixtures.extractTar(extractor, data));
        assertFalse(Files.exists(attacker.resolve("file.txt")));
    }

    @Test
    void rejectsHardLinkTargetThroughSymlink() throws Exception {
        // node-tar CVE-2026-26960 class: a hard link whose target routes through a symlink must not escape the root.
        Files.write(attacker.resolve("secret"), "secret".getBytes(StandardCharsets.UTF_8));
        Files.createSymbolicLink(target.resolve("s"), attacker);
        final byte[] data = Fixtures.tar(hardlink("x", "s/secret"));
        final Extractor extractor = Extractor.newExtractor(target);
        assertThrows(IOException.class, () -> Fixtures.extractTar(extractor, data));
        assertFalse(Files.exists(target.resolve("x")));
    }
}
