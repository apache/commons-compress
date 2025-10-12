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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Stream;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.MemoryLimitException;
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream;
import org.apache.commons.compress.archivers.arj.ArjArchiveInputStream;
import org.apache.commons.compress.archivers.cpio.CpioArchiveInputStream;
import org.apache.commons.compress.archivers.dump.DumpArchiveEntry;
import org.apache.commons.compress.archivers.dump.DumpArchiveInputStream;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarFile;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.function.IOStream;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests handling of file names limits in various archive formats.
 */
public class MaxNameEntryLengthTest extends AbstractTest {

    private static final int PORTABLE_NAME_LIMIT = 1023;
    private static final int SOFT_MAX_ARRAY_LENGTH = Integer.MAX_VALUE - 8;

    @SuppressWarnings("OctalInteger")
    private static final int CPIO_OLD_ASCII_NAME_LIMIT = 0777_776;

    static Stream<Arguments> testTruncatedStreams() throws IOException {
        return Stream.of(
                Arguments.of(
                        ArArchiveInputStream.builder()
                                .setMaxEntryNameLength(Integer.MAX_VALUE)
                                .setURI(getURI("synthetic/long-name/bsd-fail.ar"))
                                .get(),
                        SOFT_MAX_ARRAY_LENGTH),
                Arguments.of(
                        ArArchiveInputStream.builder()
                                .setMaxEntryNameLength(Integer.MAX_VALUE)
                                .setURI(getURI("synthetic/long-name/gnu-fail.ar"))
                                .get(),
                        SOFT_MAX_ARRAY_LENGTH),
                Arguments.of(
                        CpioArchiveInputStream.builder()
                                .setMaxEntryNameLength(Integer.MAX_VALUE)
                                .setURI(getURI("synthetic/long-name/odc-fail.cpio"))
                                .get(),
                        CPIO_OLD_ASCII_NAME_LIMIT),
                Arguments.of(
                        CpioArchiveInputStream.builder()
                                .setMaxEntryNameLength(Integer.MAX_VALUE)
                                .setURI(getURI("synthetic/long-name/newc-fail.cpio"))
                                .get(),
                        SOFT_MAX_ARRAY_LENGTH),
                Arguments.of(
                        CpioArchiveInputStream.builder()
                                .setMaxEntryNameLength(Integer.MAX_VALUE)
                                .setURI(getURI("synthetic/long-name/crc-fail.cpio"))
                                .get(),
                        SOFT_MAX_ARRAY_LENGTH),
                Arguments.of(
                        TarArchiveInputStream.builder()
                                .setMaxEntryNameLength(Integer.MAX_VALUE)
                                .setURI(getURI("synthetic/long-name/pax-fail.tar"))
                                .get(),
                        // The PAX entry length is the limiting factor: "2147483647 path=...\n"
                        Integer.MAX_VALUE),
                Arguments.of(
                        TarArchiveInputStream.builder()
                                .setMaxEntryNameLength(Integer.MAX_VALUE)
                                .setURI(getURI("synthetic/long-name/gnu-fail.tar"))
                                .get(),
                        SOFT_MAX_ARRAY_LENGTH));
    }

    @ParameterizedTest
    @MethodSource
    void testTruncatedStreams(final ArchiveInputStream<?> archiveInputStream, final long expectedLength) {
        // If the file name length exceeds available memory, the stream fails fast with MemoryLimitException.
        // Otherwise, it fails with EOFException when the stream ends unexpectedly.
        if (Runtime.getRuntime().totalMemory() < expectedLength) {
            final MemoryLimitException exception =
                    assertThrows(MemoryLimitException.class, archiveInputStream::getNextEntry);
            final String message = exception.getMessage();
            assertNotNull(message);
            assertTrue(
                    message.contains(String.format("%,d", expectedLength)),
                    "Message mentions expected length (" + expectedLength + "): " + message);
        } else {
            assertThrows(EOFException.class, archiveInputStream::getNextEntry);
        }
    }

    static Stream<Arguments> testTruncatedTarFiles() throws IOException {
        return Stream.of(
                Arguments.of(TarFile.builder()
                        .setMaxEntryNameLength(Integer.MAX_VALUE)
                        .setURI(getURI("synthetic/long-name/pax-fail.tar"))),
                Arguments.of(TarFile.builder()
                        .setMaxEntryNameLength(Integer.MAX_VALUE)
                        .setURI(getURI("synthetic/long-name/gnu-fail.tar"))));
    }

    @ParameterizedTest
    @MethodSource
    void testTruncatedTarFiles(final TarFile.Builder tarFileBuilder) {
        // Since the real size of the archive is known, the truncation is detected
        // much earlier and before trying to read file names.
        assertThrows(EOFException.class, () -> tarFileBuilder.get().getEntries());
    }

    static Stream<Arguments> testValidStreams() throws IOException {
        return Stream.of(
                Arguments.of(
                        ArArchiveInputStream.builder().setURI(getURI("synthetic/long-name/bsd-short-max-value.ar")),
                        Short.MAX_VALUE),
                Arguments.of(
                        ArArchiveInputStream.builder().setURI(getURI("synthetic/long-name/gnu-short-max-value.ar")),
                        Short.MAX_VALUE),
                Arguments.of(ArjArchiveInputStream.builder().setURI(getURI("synthetic/long-name/long-name.arj")), 2568),
                Arguments.of(
                        CpioArchiveInputStream.builder().setURI(getURI("synthetic/long-name/bin-big-endian.cpio")),
                        Short.MAX_VALUE - 1),
                Arguments.of(
                        CpioArchiveInputStream.builder().setURI(getURI("synthetic/long-name/bin-little-endian.cpio")),
                        Short.MAX_VALUE - 1),
                Arguments.of(
                        CpioArchiveInputStream.builder().setURI(getURI("synthetic/long-name/odc.cpio")),
                        Short.MAX_VALUE),
                Arguments.of(
                        CpioArchiveInputStream.builder().setURI(getURI("synthetic/long-name/newc.cpio")),
                        Short.MAX_VALUE),
                Arguments.of(
                        CpioArchiveInputStream.builder().setURI(getURI("synthetic/long-name/crc.cpio")),
                        Short.MAX_VALUE),
                Arguments.of(
                        TarArchiveInputStream.builder().setURI(getURI("synthetic/long-name/pax.tar")), Short.MAX_VALUE),
                Arguments.of(
                        TarArchiveInputStream.builder().setURI(getURI("synthetic/long-name/gnu.tar")), Short.MAX_VALUE),
                Arguments.of(
                        ZipArchiveInputStream.builder().setURI(getURI("synthetic/long-name/long-name.zip")),
                        Short.MAX_VALUE));
    }

    @ParameterizedTest
    @MethodSource
    void testValidStreams(final AbstractArchiveBuilder<ArchiveInputStream<?>, ?> builder, final int expectedLength)
            throws IOException {
        try (ArchiveInputStream<?> archiveInputStream = builder.get()) {
            final ArchiveEntry entry = archiveInputStream.getNextEntry();
            assertNotNull(entry);
            final String name = entry.getName();
            assertEquals(expectedLength, name.length(), "Unexpected name length");
            final String expected = StringUtils.repeat("a", expectedLength);
            assertEquals(expected, name);
        }
        // Impose a file name length limit and verify that it is enforced.
        builder.setMaxEntryNameLength(PORTABLE_NAME_LIMIT);
        try (ArchiveInputStream<?> archiveInputStream = builder.get()) {
            final ArchiveException exception = assertThrows(ArchiveException.class, archiveInputStream::getNextEntry);
            final String message = exception.getMessage();
            assertNotNull(message);
            assertTrue(message.contains("file name length"));
            assertTrue(message.contains(String.format("%,d", expectedLength)));
        }
    }

    static Stream<Arguments> testValidTarFiles() throws IOException {
        return Stream.of(
                Arguments.of(TarFile.builder().setURI(getURI("synthetic/long-name/pax.tar")), Short.MAX_VALUE),
                Arguments.of(TarFile.builder().setURI(getURI("synthetic/long-name/gnu.tar")), Short.MAX_VALUE));
    }

    @ParameterizedTest
    @MethodSource
    void testValidTarFiles(final TarFile.Builder tarFileBuilder, final int expectedLength) throws IOException {
        try (TarFile tarFile = tarFileBuilder.get()) {
            for (final ArchiveEntry entry : tarFile.getEntries()) {
                assertNotNull(entry);
                final String name = entry.getName();
                assertEquals(expectedLength, name.length(), "Unexpected name length");
                final String expected = StringUtils.repeat("a", expectedLength);
                assertEquals(expected, name);
            }
        }
        // Impose a file name length limit and verify that it is enforced.
        tarFileBuilder.setMaxEntryNameLength(PORTABLE_NAME_LIMIT);
        final ArchiveException exception = assertThrows(ArchiveException.class, () -> tarFileBuilder.get());
        final String message = exception.getMessage();
        assertNotNull(message);
        assertTrue(message.contains("file name length"));
        assertTrue(message.contains(String.format("%,d", expectedLength)));
    }

    @Test
    void testValidZipFile() throws IOException {
        final ZipFile.Builder builder = ZipFile.builder().setURI(getURI("synthetic/long-name/long-name.zip"));
        final int expectedLength = Short.MAX_VALUE;
        try (ZipFile zipFile = builder.get();
                IOStream<? extends ZipArchiveEntry> entries = zipFile.stream()) {
            entries.forEach(entry -> {
                assertNotNull(entry);
                final String name = entry.getName();
                assertEquals(expectedLength, name.length(), "Unexpected name length");
                final String expected = StringUtils.repeat("a", expectedLength);
                assertEquals(expected, name);
            });
        }
        // Impose a file name length limit and verify that it is enforced.
        builder.setMaxEntryNameLength(PORTABLE_NAME_LIMIT);
        final ArchiveException exception = assertThrows(ArchiveException.class, builder::get);
        final String message = exception.getMessage();
        assertNotNull(message);
        assertTrue(message.contains("file name length"), "Message mentions file name length: " + message);
        assertTrue(message.contains(String.format("%,d", expectedLength)));
    }

    @Test
    void testValid7ZipFile() throws IOException {
        final SevenZFile.Builder builder = SevenZFile.builder().setURI(getURI("synthetic/long-name/long-name.7z"));
        final int expectedLength = Short.MAX_VALUE;
        try (SevenZFile sevenZFile = builder.get()) {
            final ArchiveEntry entry = sevenZFile.getNextEntry();
            assertNotNull(entry);
            final String name = entry.getName();
            assertEquals(expectedLength, name.length(), "Unexpected name length");
            final String expected = StringUtils.repeat("a", expectedLength);
            assertEquals(expected, name);
        }
        // SevenZFile parses the whole archive at once, so the builder throws the exception.
        final ArchiveException exception =
                assertThrows(ArchiveException.class, () -> builder.setMaxEntryNameLength(PORTABLE_NAME_LIMIT)
                        .get());
        final String message = exception.getMessage();
        assertNotNull(message);
        assertTrue(message.contains("file name length"));
        assertTrue(message.contains(String.format("%,d", expectedLength)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"synthetic/long-name/long-name.dump", "synthetic/long-name/long-name-reversed.dump"})
    void testValidDumpStreams(final String resourceName) throws IOException {
        final int rootInode = 2;
        final int expectedDepth = 127; // number of nested directories
        final int nameSegmentLength = 255; // length of each segment
        final int totalEntries = 1 + expectedDepth + 1; // root + 127 dirs + 1 file
        final int maxInode = rootInode + totalEntries - 1;

        final String nameSegment = StringUtils.repeat('a', nameSegmentLength);

        final DumpArchiveInputStream.Builder builder =
                DumpArchiveInputStream.builder().setURI(getURI(resourceName));

        try (DumpArchiveInputStream in = builder.get()) {
            for (int expectedInode = rootInode; expectedInode <= maxInode; expectedInode++) {
                final boolean isRegularFile = expectedInode == maxInode;
                final DumpArchiveEntry entry = in.getNextEntry();

                assertNotNull(entry, "Entry " + expectedInode + " should exist");

                // Type checks: root + 127 are directories, last is a regular file.
                assertEquals(!isRegularFile, entry.isDirectory(), "isDirectory() mismatch");

                final int depth = expectedInode - rootInode; // 0 for root, 1..127 for dirs, 128 for file’s dir count

                final String expectedNameDirs = StringUtils.repeat(nameSegment + "/", depth);
                final int expectedLength = (nameSegmentLength + 1) * depth - (isRegularFile ? 1 : 0);

                final String actualName = entry.getName();

                assertEquals(expectedInode, entry.getIno(), "inode");
                assertEquals(expectedLength, actualName.length(), "name length");
                assertEquals(expectedNameDirs.substring(0, expectedLength), actualName, "full name");

                // Structure checks: every path component is exactly 255×'a'

                String[] parts = actualName.split("/");
                if (parts.length > 0 && parts[parts.length - 1].isEmpty()) {
                    // Trailing slash yields an empty final component; ignore it.
                    parts = Arrays.copyOf(parts, parts.length - 1);
                }

                // For directories: depth components; for file: depth components (including file itself)
                assertEquals(depth, parts.length, "component count");
                for (int i = 0; i < parts.length; i++) {
                    assertEquals(nameSegmentLength, parts[i].length(), "segment[" + i + "] length");
                    assertEquals(nameSegment, parts[i], "segment[" + i + "] content");
                }
            }

            // Stream should now be exhausted.
            assertNull(in.getNextEntry(), "No more entries expected after " + totalEntries);
        }

        try (DumpArchiveInputStream in =
                builder.setMaxEntryNameLength(PORTABLE_NAME_LIMIT).get()) {
            int expectedLength;
            for (int depth = 0; ; depth++) {
                expectedLength = depth * (nameSegmentLength + 1);
                if (expectedLength > PORTABLE_NAME_LIMIT) {
                    break;
                }
                assertDoesNotThrow(in::getNextEntry, "Entry " + (rootInode + depth) + " should be readable");
            }
            final ArchiveException exception = assertThrows(ArchiveException.class, in::getNextEntry);
            final String message = exception.getMessage();
            assertNotNull(message);
            assertTrue(message.contains("file name length"));
            assertTrue(message.contains(String.format("%,d", expectedLength)));
        }
    }
}
