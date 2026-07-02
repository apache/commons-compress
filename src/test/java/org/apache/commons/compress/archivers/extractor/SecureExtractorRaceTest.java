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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Concurrent TOCTOU race: a third party swaps a parent directory component for a symbolic link after the extractor has
 * resolved the parent but before the leaf file is written (driven deterministically through the {@code beforeLeafWrite} test
 * seam). The secure extractor, which writes relative to a pinned directory handle, must not let the write escape; the base
 * extractor, which re-resolves the path, follows the swap (the documented residual).
 */
class SecureExtractorRaceTest {

    @TempDir
    Path target;

    @TempDir
    Path attacker;

    private static byte[] entryUnderSub() throws IOException {
        return Fixtures.tar(dir("sub"), file("sub/file.txt", "payload"));
    }

    private Runnable swapSubForSymlink(final Path root) {
        return () -> {
            try {
                final Path sub = root.resolve("sub");
                Files.delete(sub);
                Files.createSymbolicLink(sub, attacker);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }

    @Test
    void baseExtractorDoesNotDefeatParentSwap() throws Exception {
        final Path root = target.toRealPath();
        final Extractor extractor = new Extractor(root);
        extractor.setBeforeLeafWrite(swapSubForSymlink(root));
        Fixtures.extractTar(extractor, entryUnderSub());
        assertTrue(Files.exists(attacker.resolve("file.txt")), "base extractor follows the swapped parent (documented residual)");
    }

    @Test
    void secureExtractorDefeatsParentSwap() throws Exception {
        final Path root = target.toRealPath();
        final Extractor extractor = Extractor.newExtractor(target);
        assumeTrue(extractor instanceof SecureExtractor, "requires a SecureDirectoryStream platform (Linux)");
        extractor.setBeforeLeafWrite(swapSubForSymlink(root));
        try {
            Fixtures.extractTar(extractor, entryUnderSub());
        } catch (final IOException failClosed) {
            // Fail-closed (the pinned parent was unlinked) is an acceptable secure outcome; the escape must not happen.
        }
        assertFalse(Files.exists(attacker.resolve("file.txt")), "secure extractor must not write through the swapped parent");
    }
}
