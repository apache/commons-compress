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
package org.apache.commons.compress.archivers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.stream.Stream;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream;
import org.apache.commons.compress.archivers.arj.ArjArchiveInputStream;
import org.apache.commons.compress.archivers.cpio.CpioArchiveInputStream;
import org.apache.commons.compress.archivers.dump.DumpArchiveInputStream;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;
import org.apache.commons.compress.archivers.lha.LhaArchiveInputStream;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarFile;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests {@link AbstractArchiveBuilder#setMaxEntries(int)}.
 */
class MaxEntriesTest extends AbstractTest {

    static Stream<AbstractArchiveBuilder<? extends ArchiveFile<?>, ?>> testArchiveFiles() throws IOException {
        return Stream.of(
                SevenZFile.builder().setURI(getURI("bla.7z")),
                TarFile.builder().setURI(getURI("bla.tar")),
                ZipFile.builder().setURI(getURI("bla.zip")));
    }

    static Stream<AbstractArchiveBuilder<? extends ArchiveInputStream<?>, ?>> testArchiveInputStreams() throws IOException {
        return Stream.of(
                ArArchiveInputStream.builder().setURI(getURI("bla.ar")),
                ArjArchiveInputStream.builder().setURI(getURI("bla.arj")),
                CpioArchiveInputStream.builder().setURI(getURI("bla.cpio")),
                DumpArchiveInputStream.builder().setURI(getURI("bla.dump")),
                JarArchiveInputStream.builder().setURI(getURI("bla.jar")),
                LhaArchiveInputStream.builder().setURI(getURI("test-macos-l0.lha")),
                TarArchiveInputStream.builder().setURI(getURI("bla.tar")),
                ZipArchiveInputStream.builder().setURI(getURI("bla.zip")));
    }

    private static void assertLimitExceeded(final int maxEntries, final Executable executable) {
        final ArchiveException exception = assertThrows(ArchiveException.class, executable);
        assertTrue(exception.getMessage().contains(String.format("more than %,d entries", maxEntries)), exception.getMessage());
    }

    private static int countEntries(final ArchiveFile<?> archiveFile) throws IOException {
        try (ArchiveFile<?> archive = archiveFile) {
            return archive.entries().size();
        }
    }

    private static int countEntries(final ArchiveInputStream<?> archiveInputStream) throws IOException {
        try (ArchiveInputStream<?> archive = archiveInputStream) {
            int count = 0;
            while (archive.getNextEntry() != null) {
                count++;
            }
            return count;
        }
    }

    @ParameterizedTest
    @MethodSource
    void testArchiveFiles(final AbstractArchiveBuilder<? extends ArchiveFile<?>, ?> builder) throws IOException {
        final int entryCount = countEntries(builder.get());
        assertEquals(entryCount, countEntries(builder.setMaxEntries(entryCount).get()));
        assertLimitExceeded(entryCount - 1, builder.setMaxEntries(entryCount - 1)::get);
    }

    @ParameterizedTest
    @MethodSource
    void testArchiveInputStreams(final AbstractArchiveBuilder<? extends ArchiveInputStream<?>, ?> builder) throws IOException {
        final int entryCount = countEntries(builder.get());
        assertEquals(entryCount, countEntries(builder.setMaxEntries(entryCount).get()));
        try (ArchiveInputStream<?> archive = builder.setMaxEntries(entryCount - 1).get()) {
            for (int i = 1; i < entryCount; i++) {
                assertNotNull(archive.getNextEntry());
            }
            assertLimitExceeded(entryCount - 1, archive::getNextEntry);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = { Integer.MIN_VALUE, -1, 0 })
    void testRejectsNonPositiveLimit(final int maxEntries) {
        assertThrows(IllegalArgumentException.class, () -> TarArchiveInputStream.builder().setMaxEntries(maxEntries));
    }
}
