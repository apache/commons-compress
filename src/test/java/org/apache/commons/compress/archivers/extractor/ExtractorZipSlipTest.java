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
import static org.apache.commons.compress.archivers.extractor.Fixtures.Entry.rawFile;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Zip-slip regression: entry names that escape the target via {@code ..} or an absolute path must be rejected, preserving the
 * lexical guard that {@code ArchiveEntry.resolveIn} provides today.
 */
class ExtractorZipSlipTest {

    @TempDir
    Path target;

    @Test
    void rejectsAbsoluteName() throws Exception {
        final byte[] data = Fixtures.tar(rawFile("/tmp/compress-extractor-evil", "x"));
        final Extractor extractor = Extractor.newExtractor(target);
        try (TarArchiveInputStream in = new TarArchiveInputStream(new ByteArrayInputStream(data))) {
            assertThrows(IOException.class, () -> extractor.extract(in));
        }
        assertFalse(Files.exists(Paths.get("/tmp/compress-extractor-evil")));
    }

    @Test
    void rejectsParentTraversal() throws Exception {
        final byte[] data = Fixtures.tar(file("../compress-extractor-evil.txt", "x"));
        final Extractor extractor = Extractor.newExtractor(target);
        try (TarArchiveInputStream in = new TarArchiveInputStream(new ByteArrayInputStream(data))) {
            assertThrows(IOException.class, () -> extractor.extract(in));
        }
        assertFalse(Files.exists(target.getParent().resolve("compress-extractor-evil.txt")));
    }
}
