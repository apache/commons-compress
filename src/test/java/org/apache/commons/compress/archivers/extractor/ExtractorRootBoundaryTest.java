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

import static org.apache.commons.compress.archivers.extractor.Fixtures.Entry.file;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
}
