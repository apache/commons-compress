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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Happy-path and boundary cases: a legitimate archive extracts faithfully, and an empty archive is a no-op. Proves the
 * extractor does not over-block (no false positives).
 */
class ExtractorHappyPathTest {

    @TempDir
    Path target;

    @Test
    void emptyTarIsNoOp() throws Exception {
        final byte[] data = Fixtures.tar();
        final Extractor extractor = Extractor.newExtractor(target);
        try (TarArchiveInputStream in = new TarArchiveInputStream(new ByteArrayInputStream(data))) {
            extractor.extract(in);
        }
        try (java.util.stream.Stream<Path> entries = Files.list(target)) {
            assertTrue(!entries.findAny().isPresent(), "target should remain empty");
        }
    }

    @Test
    void extractsNestedTarViaArchiveInputStream() throws Exception {
        final byte[] data = Fixtures.tar(dir("a"), file("a/b.txt", "hello"));
        final Extractor extractor = Extractor.newExtractor(target);
        try (TarArchiveInputStream in = new TarArchiveInputStream(new ByteArrayInputStream(data))) {
            extractor.extract(in);
        }
        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(target.resolve("a/b.txt")));
    }

    @Test
    void extractsNestedZipViaArchiveInputStream() throws Exception {
        final byte[] data = Fixtures.zip(dir("a"), file("a/b.txt", "world"));
        final Extractor extractor = Extractor.newExtractor(target);
        try (ZipArchiveInputStream in = new ZipArchiveInputStream(new ByteArrayInputStream(data))) {
            extractor.extract(in);
        }
        assertArrayEquals("world".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(target.resolve("a/b.txt")));
    }
}
