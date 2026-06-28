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
import static org.apache.commons.compress.archivers.extractor.Fixtures.Entry.symlink;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

import org.apache.commons.compress.archivers.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Symbolic-link policy behavior. The archive itself is trusted; these prove that the extractor controls link creation and
 * never follows a created link, regardless of platform (base or secure).
 */
class ExtractorSymlinkPolicyTest {

    @TempDir
    Path target;

    @Test
    void allowAllCreatesEscapingLink() throws Exception {
        final byte[] data = Fixtures.tar(symlink("link", "/etc"));
        final Extractor extractor = Extractor.newExtractor(target).setSymlinkPolicy(SymlinkPolicy.ALLOW_ALL);
        Fixtures.extractTar(extractor, data);
        final Path link = target.resolve("link");
        assertTrue(Files.isSymbolicLink(link));
        assertEquals(link.getFileSystem().getPath("/etc"), Files.readSymbolicLink(link));
    }

    @Test
    void allowWithinRootCreatesInRootLink() throws Exception {
        final byte[] data = Fixtures.tar(dir("a"), dir("b"), symlink("a/link", "../b"));
        final Extractor extractor = Extractor.newExtractor(target).setSymlinkPolicy(SymlinkPolicy.ALLOW_WITHIN_ROOT);
        Fixtures.extractTar(extractor, data);
        final Path link = target.resolve("a/link");
        assertTrue(Files.isSymbolicLink(link));
        assertEquals(link.getFileSystem().getPath("../b"), Files.readSymbolicLink(link));
    }

    @Test
    void allowWithinRootRejectsAbsoluteEscape() throws Exception {
        final byte[] data = Fixtures.tar(symlink("link", "/etc"));
        final Extractor extractor = Extractor.newExtractor(target).setSymlinkPolicy(SymlinkPolicy.ALLOW_WITHIN_ROOT);
        assertThrows(IOException.class, () -> Fixtures.extractTar(extractor, data));
        assertFalse(Files.exists(target.resolve("link"), LinkOption.NOFOLLOW_LINKS));
    }

    @Test
    void allowWithinRootRejectsRelativeEscape() throws Exception {
        final byte[] data = Fixtures.tar(symlink("link", "../../etc/passwd"));
        final Extractor extractor = Extractor.newExtractor(target).setSymlinkPolicy(SymlinkPolicy.ALLOW_WITHIN_ROOT);
        assertThrows(IOException.class, () -> Fixtures.extractTar(extractor, data));
        assertFalse(Files.exists(target.resolve("link"), LinkOption.NOFOLLOW_LINKS));
    }

    @Test
    void rejectsSymlinkByDefaultTar() throws Exception {
        final byte[] data = Fixtures.tar(symlink("link", "/etc"));
        final Extractor extractor = Extractor.newExtractor(target);
        assertThrows(IOException.class, () -> Fixtures.extractTar(extractor, data));
    }

    @Test
    void rejectsSymlinkByDefaultZip() throws Exception {
        final byte[] data = Fixtures.zip(symlink("link", "/etc"));
        final Extractor extractor = Extractor.newExtractor(target);
        try (ZipFile zip = Fixtures.openZip(data)) {
            assertThrows(IOException.class, () -> extractor.extract(zip));
        }
    }

    @Test
    void skipSilentlyIgnoresSymlink() throws Exception {
        final byte[] data = Fixtures.tar(symlink("link", "/etc"), file("real.txt", "ok"));
        final Extractor extractor = Extractor.newExtractor(target).setSymlinkPolicy(SymlinkPolicy.SKIP);
        Fixtures.extractTar(extractor, data);
        assertFalse(Files.exists(target.resolve("link"), LinkOption.NOFOLLOW_LINKS));
        assertTrue(Files.exists(target.resolve("real.txt")));
    }

    @Test
    void allowWithinRootCreatesZipSymlinkViaZipFile() throws Exception {
        final byte[] data = Fixtures.zip(dir("a"), dir("b"), symlink("a/link", "../b"));
        final Extractor extractor = Extractor.newExtractor(target).setSymlinkPolicy(SymlinkPolicy.ALLOW_WITHIN_ROOT);
        try (ZipFile zip = Fixtures.openZip(data)) {
            extractor.extract(zip);
        }
        final Path link = target.resolve("a/link");
        assertTrue(Files.isSymbolicLink(link));
        assertEquals(link.getFileSystem().getPath("../b"), Files.readSymbolicLink(link));
    }
}
