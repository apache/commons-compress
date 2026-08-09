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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Overwrite policy. The default is fail-closed ({@code CREATE_NEW}): an existing target is never silently clobbered and an
 * existing symbolic link at the target path is never followed.
 */
class ExtractorOverwriteTest {

    @TempDir
    Path target;

    @Test
    void failsClosedWhenTargetExists() throws Exception {
        Files.write(target.resolve("a.txt"), "old".getBytes(StandardCharsets.UTF_8));
        final byte[] data = Fixtures.tar(file("a.txt", "new"));
        final Extractor extractor = Extractor.newExtractor(target);
        assertThrows(IOException.class, () -> Fixtures.extractTar(extractor, data));
        assertArrayEquals("old".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(target.resolve("a.txt")));
    }

    @Test
    void overwriteFalseDoesNotFollowExistingSymlink() throws Exception {
        final Path outside = Files.createTempFile("compress-extractor-outside", ".txt");
        Files.write(outside, "secret".getBytes(StandardCharsets.UTF_8));
        Files.createSymbolicLink(target.resolve("a.txt"), outside);
        final byte[] data = Fixtures.tar(file("a.txt", "attacker"));
        final Extractor extractor = Extractor.newExtractor(target);
        assertThrows(IOException.class, () -> Fixtures.extractTar(extractor, data));
        assertArrayEquals("secret".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(outside));
    }

    @Test
    void overwriteReplacesExisting() throws Exception {
        Files.write(target.resolve("a.txt"), "old".getBytes(StandardCharsets.UTF_8));
        final byte[] data = Fixtures.tar(file("a.txt", "new"));
        final Extractor extractor = Extractor.newExtractor(target).setOverwrite(true);
        Fixtures.extractTar(extractor, data);
        assertArrayEquals("new".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(target.resolve("a.txt")));
    }

    @Test
    void overwriteTrueStillDoesNotFollowExistingSymlink() throws Exception {
        // Even with overwrite enabled the write is no-follow, so an existing symbolic link at the target is never traversed.
        final Path outside = Files.createTempFile("compress-extractor-outside", ".txt");
        Files.write(outside, "secret".getBytes(StandardCharsets.UTF_8));
        Files.createSymbolicLink(target.resolve("a.txt"), outside);
        final byte[] data = Fixtures.tar(file("a.txt", "attacker"));
        final Extractor extractor = Extractor.newExtractor(target).setOverwrite(true);
        assertThrows(IOException.class, () -> Fixtures.extractTar(extractor, data));
        assertArrayEquals("secret".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(outside));
    }
}
