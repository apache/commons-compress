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
import static org.apache.commons.compress.archivers.extractor.Fixtures.Entry.fifo;
import static org.apache.commons.compress.archivers.extractor.Fixtures.Entry.file;
import static org.apache.commons.compress.archivers.extractor.Fixtures.Entry.hardlink;
import static org.apache.commons.compress.archivers.extractor.Fixtures.Entry.symlink;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.stream.Stream;

import org.apache.commons.compress.archivers.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Resilience and robustness cases beyond the core threat suite: legitimate-but-awkward inputs (empty, Unicode, long, dotted,
 * duplicate names), idempotency and reuse, mixed entry types, partial-state containment when a later entry is rejected, and the
 * fail-closed behavior of forward hard-link references and null configuration. These prove the extractor neither over-blocks
 * legitimate archives nor leaks state on the unhappy paths.
 */
class ExtractorResilienceTest {

    @TempDir
    Path target;

    @TempDir
    Path attacker;

    private static byte[] bytes(final String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void emptyFileEntryExtractsAsEmptyFile() throws Exception {
        Fixtures.extractTar(Extractor.newExtractor(target), Fixtures.tar(file("empty", "")));
        assertTrue(Files.exists(target.resolve("empty")));
        assertEquals(0L, Files.size(target.resolve("empty")));
    }

    @Test
    void unicodeEntryNameExtractsContained() throws Exception {
        Fixtures.extractTar(Extractor.newExtractor(target), Fixtures.tar(file("café/ünï.txt", "hi")));
        assertArrayEquals(bytes("hi"), Files.readAllBytes(target.resolve("café/ünï.txt")));
    }

    @Test
    void deeplyNestedFileCreatesAllParents() throws Exception {
        Fixtures.extractTar(Extractor.newExtractor(target), Fixtures.tar(file("p/q/r/s.txt", "x")));
        assertTrue(Files.isDirectory(target.resolve("p/q/r")));
        assertTrue(Files.exists(target.resolve("p/q/r/s.txt")));
    }

    @Test
    void dotSegmentNameNormalizedAndContained() throws Exception {
        Fixtures.extractTar(Extractor.newExtractor(target), Fixtures.tar(file("a/./b.txt", "x")));
        assertTrue(Files.exists(target.resolve("a/b.txt")));
        assertFalse(Files.exists(target.getParent().resolve("b.txt")));
    }

    @Test
    void setSymlinkPolicyNullRejected() throws Exception {
        final Extractor e = Extractor.newExtractor(target);
        assertThrows(NullPointerException.class, () -> e.setSymlinkPolicy(null));
    }

    @Test
    void setSpecialFilePolicyNullRejected() throws Exception {
        final Extractor e = Extractor.newExtractor(target);
        assertThrows(NullPointerException.class, () -> e.setSpecialFilePolicy(null));
    }

    @Test
    void extractorReusedForTwoArchives() throws Exception {
        final Extractor e = Extractor.newExtractor(target);
        Fixtures.extractTar(e, Fixtures.tar(file("a.txt", "1")));
        Fixtures.extractTar(e, Fixtures.tar(file("b.txt", "2")));
        assertTrue(Files.exists(target.resolve("a.txt")));
        assertTrue(Files.exists(target.resolve("b.txt")));
    }

    @Test
    void mixedEntryTypesAllExtract() throws Exception {
        final byte[] data = Fixtures.tar(dir("d"), file("d/f.txt", "x"), file("real.txt", "r"), symlink("d/link", "../real.txt"),
                hardlink("h.txt", "real.txt"));
        final Extractor e = Extractor.newExtractor(target).setSymlinkPolicy(SymlinkPolicy.ALLOW_WITHIN_ROOT);
        Fixtures.extractTar(e, data);
        assertTrue(Files.exists(target.resolve("d/f.txt")));
        assertTrue(Files.isSymbolicLink(target.resolve("d/link")));
        assertTrue(Files.exists(target.resolve("h.txt")));
    }

    @Test
    void hardLinkForwardReferenceFailsClosed() throws Exception {
        final byte[] data = Fixtures.tar(hardlink("link", "later.txt"));
        final Extractor e = Extractor.newExtractor(target);
        assertThrows(IOException.class, () -> Fixtures.extractTar(e, data));
    }

    @Test
    void createdSymlinkDoesNotBlockLaterLegitWrites() throws Exception {
        final byte[] data = Fixtures.tar(dir("b"), symlink("b/link", "."), file("c.txt", "ok"));
        final Extractor e = Extractor.newExtractor(target).setSymlinkPolicy(SymlinkPolicy.ALLOW_WITHIN_ROOT);
        Fixtures.extractTar(e, data);
        assertTrue(Files.isSymbolicLink(target.resolve("b/link")));
        assertTrue(Files.exists(target.resolve("c.txt")));
    }

    @Test
    void midArchiveEscapeLeavesEarlierEntriesButNoEscape() throws Exception {
        final byte[] data = Fixtures.tar(file("good.txt", "ok"), file("../evil.txt", "bad"));
        final Extractor e = Extractor.newExtractor(target);
        assertThrows(IOException.class, () -> Fixtures.extractTar(e, data));
        assertTrue(Files.exists(target.resolve("good.txt")), "earlier valid entry written");
        assertFalse(Files.exists(target.getParent().resolve("evil.txt")), "escape blocked");
    }

    @Test
    void trailingSlashNameIsDirectory() throws Exception {
        Fixtures.extractTar(Extractor.newExtractor(target), Fixtures.tar(dir("onlydir")));
        assertTrue(Files.isDirectory(target.resolve("onlydir")));
    }

    @Test
    void longSingleComponentNameExtracts() throws Exception {
        final char[] c = new char[150];
        Arrays.fill(c, 'a');
        final String name = new String(c) + ".txt";
        Fixtures.extractTar(Extractor.newExtractor(target), Fixtures.tar(file(name, "x")));
        assertTrue(Files.exists(target.resolve(name)));
    }

    @Test
    void multipleHardLinksShareInode() throws Exception {
        final byte[] data = Fixtures.tar(file("real.txt", "data"), hardlink("h1.txt", "real.txt"), hardlink("h2.txt", "real.txt"));
        Fixtures.extractTar(Extractor.newExtractor(target), data);
        final Object k0 = Files.readAttributes(target.resolve("real.txt"), BasicFileAttributes.class).fileKey();
        assertEquals(k0, Files.readAttributes(target.resolve("h1.txt"), BasicFileAttributes.class).fileKey());
        assertEquals(k0, Files.readAttributes(target.resolve("h2.txt"), BasicFileAttributes.class).fileKey());
    }

    @Test
    void duplicateDirectoryEntryTolerated() throws Exception {
        final byte[] data = Fixtures.tar(dir("d"), dir("d"), file("d/f.txt", "x"));
        Fixtures.extractTar(Extractor.newExtractor(target), data);
        assertTrue(Files.exists(target.resolve("d/f.txt")));
    }

    @Test
    void zipFileSymlinkSlipBlocked() throws Exception {
        final byte[] data = Fixtures.zip(dir("b"), symlink("link", "b"), file("link/evil", "x"));
        final Extractor e = Extractor.newExtractor(target).setSymlinkPolicy(SymlinkPolicy.ALLOW_WITHIN_ROOT);
        try (ZipFile zip = Fixtures.openZip(data)) {
            assertThrows(IOException.class, () -> e.extract(zip));
        }
        assertFalse(Files.exists(target.resolve("b/evil")));
    }

    @Test
    void skipPolicyIgnoresZipSymlinkButExtractsRest() throws Exception {
        final byte[] data = Fixtures.zip(symlink("link", "/etc"), file("real.txt", "ok"));
        final Extractor e = Extractor.newExtractor(target).setSymlinkPolicy(SymlinkPolicy.SKIP);
        try (ZipFile zip = Fixtures.openZip(data)) {
            e.extract(zip);
        }
        assertFalse(Files.exists(target.resolve("link"), LinkOption.NOFOLLOW_LINKS));
        assertTrue(Files.exists(target.resolve("real.txt")));
    }

    @Test
    void specialRejectStopsExtraction() throws Exception {
        final byte[] data = Fixtures.tar(file("a.txt", "ok"), fifo("f"), file("b.txt", "later"));
        final Extractor e = Extractor.newExtractor(target).setSpecialFilePolicy(SpecialFilePolicy.REJECT);
        assertThrows(IOException.class, () -> Fixtures.extractTar(e, data));
        assertTrue(Files.exists(target.resolve("a.txt")));
        assertFalse(Files.exists(target.resolve("b.txt")));
    }

    @Test
    void emptyZipViaZipFileIsNoOp() throws Exception {
        final byte[] data = Fixtures.zip();
        final Extractor e = Extractor.newExtractor(target);
        try (ZipFile zip = Fixtures.openZip(data)) {
            e.extract(zip);
        }
        try (Stream<Path> s = Files.list(target)) {
            assertFalse(s.findAny().isPresent(), "target should remain empty");
        }
    }

    @Test
    void allowAllCreatesRelativeEscapingTargetButLeafStaysInRoot() throws Exception {
        final byte[] data = Fixtures.tar(symlink("escape-link", "../../../../etc/passwd"));
        final Extractor e = Extractor.newExtractor(target).setSymlinkPolicy(SymlinkPolicy.ALLOW_ALL);
        Fixtures.extractTar(e, data);
        final Path link = target.resolve("escape-link");
        assertTrue(Files.isSymbolicLink(link));
        assertEquals(link.getFileSystem().getPath("../../../../etc/passwd"), Files.readSymbolicLink(link));
    }
}
