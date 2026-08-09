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
import static org.apache.commons.compress.archivers.extractor.Fixtures.Entry.hardlink;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Hard-link (tar {@code isLink()}) handling: a link whose target stays within the root is materialized as a hard link to the
 * already-extracted file; a link whose target escapes the root is rejected.
 */
class ExtractorHardLinkTest {

    @TempDir
    Path target;

    @Test
    void createsHardLinkWithinRoot() throws Exception {
        final byte[] data = Fixtures.tar(file("real.txt", "data"), hardlink("link", "real.txt"));
        final Extractor extractor = Extractor.newExtractor(target);
        Fixtures.extractTar(extractor, data);
        assertArrayEquals("data".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(target.resolve("link")));
        final Object realKey = Files.readAttributes(target.resolve("real.txt"), BasicFileAttributes.class).fileKey();
        final Object linkKey = Files.readAttributes(target.resolve("link"), BasicFileAttributes.class).fileKey();
        assertEquals(realKey, linkKey);
    }

    @Test
    void rejectsHardLinkEscapingRoot() throws Exception {
        final byte[] data = Fixtures.tar(hardlink("link", "../../etc/passwd"));
        final Extractor extractor = Extractor.newExtractor(target);
        assertThrows(IOException.class, () -> Fixtures.extractTar(extractor, data));
    }
}
