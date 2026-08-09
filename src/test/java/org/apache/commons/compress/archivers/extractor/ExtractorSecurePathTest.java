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
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Positive and adversarial coverage of the {@link SecureExtractor} regular-file write path, which only activates on a platform
 * whose file system provides a {@link java.nio.file.SecureDirectoryStream} (Linux). Each test asserts the secure subclass is in
 * use, so on non-secure platforms (macOS, Windows) the cases are skipped rather than silently exercising the base path.
 */
class ExtractorSecurePathTest {

    @TempDir
    Path target;

    @TempDir
    Path attacker;

    @BeforeEach
    void requireSecurePlatform() throws Exception {
        assumeTrue(Extractor.newExtractor(target) instanceof SecureExtractor, "requires a SecureDirectoryStream platform (Linux)");
    }

    @Test
    void writesNestedFileRelativeToPinnedHandle() throws Exception {
        final byte[] data = Fixtures.tar(dir("a"), dir("a/b"), file("a/b/c.txt", "deep-secure"));
        Fixtures.extractTar(Extractor.newExtractor(target), data);
        assertArrayEquals("deep-secure".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(target.resolve("a/b/c.txt")));
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
    void overwriteReplacesThroughPinnedHandle() throws Exception {
        Files.write(target.resolve("a.txt"), "old".getBytes(StandardCharsets.UTF_8));
        final byte[] data = Fixtures.tar(file("a.txt", "new"));
        Fixtures.extractTar(Extractor.newExtractor(target).setOverwrite(true), data);
        assertArrayEquals("new".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(target.resolve("a.txt")));
    }

    @Test
    void overwriteDoesNotFollowExistingSymlinkThroughPinnedHandle() throws Exception {
        final Path outside = Files.write(attacker.resolve("secret"), "secret".getBytes(StandardCharsets.UTF_8));
        Files.createSymbolicLink(target.resolve("a.txt"), outside);
        final byte[] data = Fixtures.tar(file("a.txt", "attacker"));
        final Extractor extractor = Extractor.newExtractor(target).setOverwrite(true);
        assertThrows(IOException.class, () -> Fixtures.extractTar(extractor, data));
        assertArrayEquals("secret".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(outside));
    }
}
