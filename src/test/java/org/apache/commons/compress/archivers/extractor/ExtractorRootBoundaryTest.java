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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Root-boundary invariant: {@code newExtractor} canonicalizes the target with {@code toRealPath}, so the containment guard is
 * not sensitive to how the target is spelled (trailing separator, {@code .} component, or a symlinked path). This is why the
 * extractor uses {@code resolveWithinRoot} against a canonical root rather than the lexical {@code ArchiveEntry.resolveIn},
 * which compares against the parent string as given and misbehaves when that parent is not normalized.
 */
class ExtractorRootBoundaryTest {

    @TempDir
    Path target;

    @Test
    void rejectsTraversalWhenTargetHasTrailingSeparator() throws Exception {
        final Path slashy = Paths.get(target.toString() + "/");
        final Extractor extractor = Extractor.newExtractor(slashy);
        assertThrows(IOException.class, () -> Fixtures.extractTar(extractor, Fixtures.tar(file("../escape", "x"))));
        assertFalse(Files.exists(target.getParent().resolve("escape")));
    }

    @Test
    void rejectsTraversalWhenTargetHasDotComponent() throws Exception {
        final Path dotted = Paths.get(target.toString(), ".");
        final Extractor extractor = Extractor.newExtractor(dotted);
        assertThrows(IOException.class, () -> Fixtures.extractTar(extractor, Fixtures.tar(file("../escape", "x"))));
        assertFalse(Files.exists(target.getParent().resolve("escape")));
    }

    @Test
    void canonicalizesSymlinkedRootAndStaysContained() throws Exception {
        final Path real = Files.createDirectory(target.resolve("real"));
        final Path link = Files.createSymbolicLink(target.resolve("link"), real);
        Fixtures.extractTar(Extractor.newExtractor(link), Fixtures.tar(file("a.txt", "hi")));
        assertArrayEquals("hi".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(real.resolve("a.txt")));
        final Extractor extractor = Extractor.newExtractor(link);
        assertThrows(IOException.class, () -> Fixtures.extractTar(extractor, Fixtures.tar(file("../escape", "x"))));
        assertFalse(Files.exists(target.resolve("escape")));
    }

    @Test
    void canonicalizesTargetWithParentComponentAndStaysContained() throws Exception {
        // A ".."-spelled target must behave like the "." and trailing-separator cases. A bare relative Path.of("..") is
        // avoided on purpose: it would resolve to the parent of the process working directory and extract outside the
        // @TempDir, so the ".." is anchored to a real in-root subdirectory that resolves back to the canonical target.
        final Path sub = Files.createDirectory(target.resolve("sub"));
        final Path viaParent = Paths.get(sub.toString(), "..");
        Fixtures.extractTar(Extractor.newExtractor(viaParent), Fixtures.tar(file("a.txt", "hi")));
        assertArrayEquals("hi".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(target.resolve("a.txt")));
        final Extractor extractor = Extractor.newExtractor(viaParent);
        assertThrows(IOException.class, () -> Fixtures.extractTar(extractor, Fixtures.tar(file("../escape", "x"))));
        assertFalse(Files.exists(target.getParent().resolve("escape")));
    }

    @ParameterizedTest
    @ValueSource(strings = { "sub/..", "a/b/../..", "a/b/c/../../.." })
    void fileResolvingToRootIsSkipped(final String name) throws Exception {
        final Object rootKey = Files.readAttributes(target, BasicFileAttributes.class).fileKey();
        Fixtures.extractTar(Extractor.newExtractor(target), Fixtures.tar(file(name, "x")));
        assertTrue(Files.isDirectory(target));
        assertEquals(rootKey, Files.readAttributes(target, BasicFileAttributes.class).fileKey());
    }

    @Test
    void directoryResolvingToRootIsSkipped() throws Exception {
        final Object rootKey = Files.readAttributes(target, BasicFileAttributes.class).fileKey();
        Fixtures.extractTar(Extractor.newExtractor(target), Fixtures.tar(dir("sub/..")));
        assertTrue(Files.isDirectory(target));
        assertEquals(rootKey, Files.readAttributes(target, BasicFileAttributes.class).fileKey());
    }

    @Test
    void symlinkResolvingToRootIsSkipped() throws Exception {
        final Object rootKey = Files.readAttributes(target, BasicFileAttributes.class).fileKey();
        final Extractor extractor = Extractor.newExtractor(target).setSymlinkPolicy(SymlinkPolicy.ALLOW_WITHIN_ROOT);
        Fixtures.extractTar(extractor, Fixtures.tar(symlink("sub/..", "irrelevant")));
        assertTrue(Files.isDirectory(target));
        assertEquals(rootKey, Files.readAttributes(target, BasicFileAttributes.class).fileKey());
    }

    @Test
    void siblingSharingRootNamePrefixIsRejected() throws Exception {
        final Path sibling = target.resolveSibling(target.getFileName().toString() + "-sibling");
        final String escape = "../" + sibling.getFileName().toString() + "/pwned";
        final Extractor extractor = Extractor.newExtractor(target);
        assertThrows(IOException.class, () -> Fixtures.extractTar(extractor, Fixtures.tar(file(escape, "x"))));
        assertFalse(Files.exists(sibling.resolve("pwned")));
    }

    @Test
    void pathWithParentSegmentStayingInsideRootIsWritten() throws Exception {
        // The skip fires only on an exact-root resolution; a name that merely contains ".." but normalizes to a location
        // inside the root is still written there.
        Fixtures.extractTar(Extractor.newExtractor(target), Fixtures.tar(file("a/b/../c", "hi")));
        assertArrayEquals("hi".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(target.resolve("a/c")));
        assertFalse(Files.exists(target.resolve("a/b")));
    }

    @Test
    void rootResolvingEntryIsSkippedWithoutAbortingTheArchive() throws Exception {
        // Skipping a root-resolving entry is per-entry, not fatal, so later entries still extract.
        Fixtures.extractTar(Extractor.newExtractor(target), Fixtures.tar(file("x/..", "ignored"), file("keep.txt", "hi")));
        assertArrayEquals("hi".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(target.resolve("keep.txt")));
    }

    @Test
    void hardlinkResolvingToRootIsSkipped() throws Exception {
        // A hard link entry has no policy gate, so the root-resolution skip is what keeps its name from targeting the root.
        final Object rootKey = Files.readAttributes(target, BasicFileAttributes.class).fileKey();
        Fixtures.extractTar(Extractor.newExtractor(target), Fixtures.tar(hardlink("sub/..", "whatever")));
        assertTrue(Files.isDirectory(target));
        assertEquals(rootKey, Files.readAttributes(target, BasicFileAttributes.class).fileKey());
    }

    @Test
    void fileResolvingToRootIsSkippedViaZip() throws Exception {
        // Same skip, exercised through the zip path for format parity with the tar cases above.
        final Object rootKey = Files.readAttributes(target, BasicFileAttributes.class).fileKey();
        Fixtures.extractZip(Extractor.newExtractor(target), Fixtures.zip(file("sub/..", "x")));
        assertTrue(Files.isDirectory(target));
        assertEquals(rootKey, Files.readAttributes(target, BasicFileAttributes.class).fileKey());
    }

    @Test
    void currentDirectoryEntryIsSkipped() throws Exception {
        // A "." entry is common in real tar archives and resolves to the root, so it is skipped rather than failing the
        // extraction or targeting the root.
        final Object rootKey = Files.readAttributes(target, BasicFileAttributes.class).fileKey();
        Fixtures.extractTar(Extractor.newExtractor(target), Fixtures.tar(dir(".")));
        assertTrue(Files.isDirectory(target));
        assertEquals(rootKey, Files.readAttributes(target, BasicFileAttributes.class).fileKey());
    }

    @Test
    void dotSegmentMidPathIsWritten() throws Exception {
        // A "." segment in the middle of a name normalizes away; the entry is still written at its real in-root location.
        Fixtures.extractTar(Extractor.newExtractor(target), Fixtures.tar(file("a/./b", "hi")));
        assertArrayEquals("hi".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(target.resolve("a/b")));
    }

    @Test
    void backslashInNameIsLiteralNotSeparatorOnPosix() throws Exception {
        // On POSIX a backslash is an ordinary filename character, not a separator, so a name that would traverse on Windows
        // stays a single in-root component here. Guards against a future change that splits on backslash.
        assumeTrue(File.separatorChar == '/');
        Fixtures.extractTar(Extractor.newExtractor(target), Fixtures.tar(file("a\\..\\..\\etc", "x")));
        assertTrue(Files.exists(target.resolve("a\\..\\..\\etc")));
        assertFalse(Files.exists(target.getParent().resolve("etc")));
    }
}
