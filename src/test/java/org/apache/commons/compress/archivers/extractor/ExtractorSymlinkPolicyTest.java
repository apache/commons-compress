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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

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

    private static final int POC_DEPTH = 20;

    private static String repeat(final String component, final int times) {
        final StringBuilder sb = new StringBuilder(component);
        for (int i = 1; i < times; i++) {
            sb.append('/').append(component);
        }
        return sb.toString();
    }

    /**
     * Builds the indirect (chained) symbolic-link escape from COMPRESS-727: {@code malicious-entry} is written first with a
     * target that is lexically inside the root, while the deeper link it passes through only escapes once the operating system
     * resolves the chain. Each link is individually within-root, which is exactly what {@link SymlinkPolicy#ALLOW_WITHIN_ROOT}
     * cannot see through.
     */
    private static byte[] indirectEscapeZip() throws IOException {
        final String nested = repeat("a", POC_DEPTH);
        final String maliciousTarget = nested + "/" + repeat("..", POC_DEPTH) + "/etc/passwd";
        final String nestedTarget = repeat("..", POC_DEPTH - 1);
        return Fixtures.zip(symlink("malicious-entry", maliciousTarget), symlink(nested, nestedTarget));
    }

    @Test
    void indirectSymlinkEscapeRejectedByDefault() throws Exception {
        final Extractor extractor = Extractor.newExtractor(target);
        try (ZipFile zip = Fixtures.openZip(indirectEscapeZip())) {
            assertThrows(IOException.class, () -> extractor.extract(zip));
        }
        assertFalse(Files.exists(target.resolve("malicious-entry"), LinkOption.NOFOLLOW_LINKS));
    }

    @Test
    void indirectSymlinkEscapeSkipped() throws Exception {
        final Extractor extractor = Extractor.newExtractor(target).setSymlinkPolicy(SymlinkPolicy.SKIP);
        try (ZipFile zip = Fixtures.openZip(indirectEscapeZip())) {
            extractor.extract(zip);
        }
        assertFalse(Files.exists(target.resolve("malicious-entry"), LinkOption.NOFOLLOW_LINKS));
    }

    @ParameterizedTest
    @EnumSource(names = { "ALLOW_WITHIN_ROOT", "ALLOW_ALL" })
    void indirectSymlinkEscapeCreatesInRootLinksButWritesNothingOutside(final SymlinkPolicy policy) throws Exception {
        final String nested = repeat("a", POC_DEPTH);
        final String maliciousTarget = nested + "/" + repeat("..", POC_DEPTH) + "/etc/passwd";
        final String nestedTarget = repeat("..", POC_DEPTH - 1);
        final byte[] data = Fixtures.zip(symlink("malicious-entry", maliciousTarget), symlink(nested, nestedTarget));
        final Extractor extractor = Extractor.newExtractor(target).setSymlinkPolicy(policy);
        try (ZipFile zip = Fixtures.openZip(data)) {
            extractor.extract(zip);
        }
        final Path malicious = target.resolve("malicious-entry");
        final Path deep = target.resolve(nested);
        // Each link passes the per-link lexical containment check, so both are created inside the root. This is the
        // documented best-effort limitation: the escape only occurs if a later consumer follows the chain, never during
        // extraction, which resolves no links.
        assertTrue(Files.isSymbolicLink(malicious));
        assertTrue(Files.isSymbolicLink(deep));
        // The crafted targets are stored verbatim (the fixture is not silently neutered) and only links, no file content, were
        // written, so extraction produced nothing outside the root.
        assertEquals(malicious.getFileSystem().getPath(maliciousTarget), Files.readSymbolicLink(malicious));
        assertEquals(deep.getFileSystem().getPath(nestedTarget), Files.readSymbolicLink(deep));
    }

    @Test
    void legitSymlinkChainWithinRootIsCreated() throws Exception {
        // Positive control for the chain the escape test attacks: when every hop stays inside the root the whole chain is
        // created, so ALLOW_WITHIN_ROOT is not over-rejecting legitimate links.
        final byte[] data = Fixtures.zip(dir("b"), symlink("a", "b"), symlink("c", "a"));
        final Extractor extractor = Extractor.newExtractor(target).setSymlinkPolicy(SymlinkPolicy.ALLOW_WITHIN_ROOT);
        try (ZipFile zip = Fixtures.openZip(data)) {
            extractor.extract(zip);
        }
        assertTrue(Files.isSymbolicLink(target.resolve("a")));
        assertTrue(Files.isSymbolicLink(target.resolve("c")));
        assertEquals(target.getFileSystem().getPath("b"), Files.readSymbolicLink(target.resolve("a")));
        assertEquals(target.getFileSystem().getPath("a"), Files.readSymbolicLink(target.resolve("c")));
    }
}
