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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Zip-slip regression: entry names that escape the target via {@code ..} or an absolute path must be rejected, preserving the
 * lexical guard that {@code ArchiveEntry.resolveIn} provides today. The guard decides containment from the name alone, so a UNC
 * or drive name on Windows has to fail with the guard's own exception and never with a {@code FileSystemException} from an API
 * that touched the name first.
 */
class ExtractorZipSlipTest {

    /** A UNC path on Windows and an absolute path everywhere else; the guard rejects it lexically on both. */
    private static final String HOST_SHARE_NAME = "//localhost/invalid/evil.txt";

    @TempDir
    Path target;

    /**
     * Asserts that the extraction fails with the zip-slip guard's own exception and leaves the target empty. A
     * {@code FileSystemException} here ("The network name cannot be found" for a UNC name) would mean an API touched the name
     * before containment was decided.
     */
    private void assertRejectedByGuard(final Executable extraction) throws IOException {
        final ArchiveException ex = assertThrows(ArchiveException.class, extraction);
        assertTrue(ex.getMessage().contains("would escape the extraction root"), () -> "expected the zip-slip guard, got: " + ex);
        assertNull(ex.getCause(), () -> "the zip-slip guard throws without a cause, got: " + ex.getCause());
        try (Stream<Path> entries = Files.list(target)) {
            assertEquals(0L, entries.count(), "a rejected entry must not leave anything behind");
        }
    }

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

    @Test
    void rejectsHostShareNameViaZipFile() throws Exception {
        final byte[] data = Fixtures.zip(file(HOST_SHARE_NAME, "x"));
        final Extractor extractor = Extractor.newExtractor(target);
        try (ZipFile zip = Fixtures.openZip(data)) {
            assertRejectedByGuard(() -> extractor.extract(zip));
        }
    }

    @Test
    void rejectsHostShareNameViaZipStream() throws Exception {
        final byte[] data = Fixtures.zip(file(HOST_SHARE_NAME, "x"));
        final Extractor extractor = Extractor.newExtractor(target);
        assertRejectedByGuard(() -> Fixtures.extractZip(extractor, data));
    }

    @Test
    void rejectsHostShareNameViaTar() throws Exception {
        final byte[] data = Fixtures.tar(rawFile(HOST_SHARE_NAME, "x"));
        final Extractor extractor = Extractor.newExtractor(target);
        assertRejectedByGuard(() -> Fixtures.extractTar(extractor, data));
    }

    @ParameterizedTest
    @ValueSource(strings = { "\\\\localhost\\invalid", "\\\\localhost\\invalid\\evil.txt", "\\\\localhost\\invalid/evil.txt",
            "//localhost/invalid/evil.txt", "C:\\Windows\\evil.txt", "C:/Windows/evil.txt" })
    @EnabledOnOs(OS.WINDOWS)
    void rejectsUncOrDriveNameOnWindows(final String name) throws Exception {
        // Each spelling is a UNC or absolute drive path to WindowsPathParser, so the guard rejects it from the name alone. A
        // FileSystemException ("The network name cannot be found") would mean the name reached the file system, and possibly
        // a remote host, before containment was decided. The zip layer folds a backslash-only name to forward slashes, which
        // Windows reads as UNC just the same.
        final byte[] data = Fixtures.zip(file(name, "x"));
        final Extractor extractor = Extractor.newExtractor(target);
        try (ZipFile zip = Fixtures.openZip(data)) {
            assertRejectedByGuard(() -> extractor.extract(zip));
        }
        assertRejectedByGuard(() -> Fixtures.extractZip(Extractor.newExtractor(target), data));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void rejectsDriveRelativeNameOnAnotherDriveOnWindows() throws Exception {
        // "Q:evil.txt" is relative to the current directory of drive Q:, which only the file system (GetFullPathName) can
        // supply. The guard rejects it from the spelling alone, so that drive is never consulted.
        final char rootDrive = Character.toUpperCase(target.toRealPath().getRoot().toString().charAt(0));
        final String name = (rootDrive == 'Q' ? "R" : "Q") + ":evil.txt";
        final byte[] data = Fixtures.zip(file(name, "x"));
        final Extractor extractor = Extractor.newExtractor(target);
        try (ZipFile zip = Fixtures.openZip(data)) {
            assertRejectedByGuard(() -> extractor.extract(zip));
        }
    }
}
