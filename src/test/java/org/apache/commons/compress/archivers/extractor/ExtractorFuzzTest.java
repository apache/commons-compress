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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Random;
import java.util.stream.Stream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Property-based adversarial fuzz: hundreds of randomized malicious tars (traversal names, escaping symlink/hardlink targets,
 * special files, mixed Unicode, deep nesting) extracted under the safe policies must never escape the extraction root and must
 * fail only with {@link IOException} (never an unchecked crash). A canary file at the parent level proves no escape occurred.
 * Seeds are fixed so any failure is reproducible. Jazzer-style coverage-guided fuzzing with traversal sanitizers is a CI
 * addition; this gives a deterministic, dependency-free adversarial sweep.
 */
class ExtractorFuzzTest {

    private static final String[] NAMES = {"f", "d/f", "d/e/f", "../escape", "../../escape", "/abs/escape", "café",
            "a/../b", "x", "d"};

    private static final String[] TARGETS = {"../escape", "../../escape", "/etc/passwd", "d", "d/f", "x", "./y"};

    private static final SymlinkPolicy[] SAFE_POLICIES = {SymlinkPolicy.REJECT, SymlinkPolicy.SKIP,
            SymlinkPolicy.ALLOW_WITHIN_ROOT};

    private static byte[] randomTar(final Random r) {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tos = new TarArchiveOutputStream(bos, "UTF-8")) {
            tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            tos.setAddPaxHeadersForNonAsciiNames(true);
            final int count = 1 + r.nextInt(6);
            for (int i = 0; i < count; i++) {
                final String name = NAMES[r.nextInt(NAMES.length)] + (r.nextBoolean() ? "" : "/" + i);
                final TarArchiveEntry entry;
                byte[] content = null;
                switch (r.nextInt(6)) {
                case 0:
                    entry = new TarArchiveEntry(name);
                    content = ("d" + i).getBytes(StandardCharsets.UTF_8);
                    entry.setSize(content.length);
                    break;
                case 1:
                    entry = new TarArchiveEntry(name.endsWith("/") ? name : name + "/");
                    break;
                case 2:
                    entry = new TarArchiveEntry(name, TarConstants.LF_SYMLINK);
                    entry.setLinkName(TARGETS[r.nextInt(TARGETS.length)]);
                    break;
                case 3:
                    entry = new TarArchiveEntry(name, TarConstants.LF_LINK);
                    entry.setLinkName(TARGETS[r.nextInt(TARGETS.length)]);
                    break;
                case 4:
                    entry = new TarArchiveEntry(name, TarConstants.LF_CHR);
                    break;
                default:
                    entry = new TarArchiveEntry(name, TarConstants.LF_FIFO);
                    break;
                }
                tos.putArchiveEntry(entry);
                if (content != null) {
                    tos.write(content);
                }
                tos.closeArchiveEntry();
            }
        } catch (final IOException writerRejectedName) {
            return null;
        }
        return bos.toByteArray();
    }

    @TempDir
    Path target;

    @Test
    void randomAdversarialArchivesNeverEscapeRoot() throws Exception {
        final byte[] canaryBytes = "canary".getBytes(StandardCharsets.UTF_8);
        final Path canary = Files.write(target.resolve("canary"), canaryBytes);
        for (long seed = 0; seed < 384; seed++) {
            final Random r = new Random(seed);
            final Path root = Files.createDirectory(target.resolve("r" + seed));
            final byte[] tar = randomTar(r);
            if (tar == null) {
                continue;
            }
            final Extractor extractor = Extractor.newExtractor(root)
                    .setSymlinkPolicy(SAFE_POLICIES[r.nextInt(SAFE_POLICIES.length)])
                    .setSpecialFilePolicy(r.nextBoolean() ? SpecialFilePolicy.SKIP : SpecialFilePolicy.REJECT)
                    .setOverwrite(r.nextBoolean());
            try (TarArchiveInputStream in = new TarArchiveInputStream(new ByteArrayInputStream(tar))) {
                extractor.extract(in);
            } catch (final IOException refusedOrMalformed) {
                // A refusal (policy/escape/parse) is an acceptable outcome; only an escape or an unchecked crash is a bug.
            }
        }
        assertArrayEquals(canaryBytes, Files.readAllBytes(canary), "the parent-level canary must be untouched");
        final long strayLeaves;
        try (Stream<Path> entries = Files.list(target)) {
            strayLeaves = entries.filter(p -> !Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS)).count();
        }
        assertEquals(1L, strayLeaves, "no file or link escaped to the parent directory (only the canary may be present)");
    }
}
