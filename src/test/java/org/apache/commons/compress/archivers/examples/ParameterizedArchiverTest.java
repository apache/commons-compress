/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.compress.archivers.examples;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.stream.Stream;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ParameterizedArchiverTest extends AbstractTestCase {

    // can't test 7z here as 7z cannot write to non-seekable streams
    // and reading logic would be different as well - see
    // SevenZArchiverTest class
    public static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of("tar"),
                Arguments.of("cpio"),
                Arguments.of("zip")
        );
    }

    private File target;

    @ParameterizedTest
    @MethodSource("data")
    public void archiveStreamVersion(final String format) throws Exception {
        // TODO How to parameterize a BeforeEach method?
        setUp(format);
        try (OutputStream os = Files.newOutputStream(target.toPath());
             ArchiveOutputStream aos = ArchiveStreamFactory.DEFAULT.createArchiveOutputStream(format, os)) {
            new Archiver().create(aos, dir);
        }
        verifyContent(format);
    }

    private void assertDir(final String expectedName, final ArchiveEntry entry) {
        assertNotNull(entry, () -> expectedName + " does not exists");
        assertEquals(expectedName + "/", entry.getName());
        assertTrue(entry.isDirectory(), expectedName + " is not a directory");
    }

    private void assertHelloWorld(final String expectedName, final String suffix, final ArchiveEntry entry, final InputStream is)
        throws IOException {
        assertNotNull(entry, () -> expectedName + " does not exists");
        assertEquals(expectedName, entry.getName());
        assertFalse(entry.isDirectory(), expectedName + " is a directory");
        final byte[] expected = ("Hello, world " + suffix).getBytes(UTF_8);
        final byte[] actual = IOUtils.toByteArray(is);
        assertArrayEquals(expected, actual);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void channelVersion(final String format) throws Exception {
        // TODO How to parameterize a BeforeEach method?
        setUp(format);
        try (SeekableByteChannel c = FileChannel.open(target.toPath(), StandardOpenOption.WRITE,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            new Archiver().create(format, c, dir);
        }
        verifyContent(format);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void fileVersion(final String format) throws Exception {
        // TODO How to parameterize a BeforeEach method?
        setUp(format);
        new Archiver().create(format, target, dir);
        verifyContent(format);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void outputStreamVersion(final String format) throws Exception {
        // TODO How to parameterize a BeforeEach method?
        setUp(format);
        try (OutputStream os = Files.newOutputStream(target.toPath())) {
            new Archiver().create(format, os, dir);
        }
        verifyContent(format);
    }

    public void setUp(final String format) throws Exception {
        super.setUp();
        final File c = new File(dir, "a/b/c");
        c.mkdirs();
        try (OutputStream os = Files.newOutputStream(new File(dir, "a/b/d.txt").toPath())) {
            os.write("Hello, world 1".getBytes(UTF_8));
        }
        try (OutputStream os = Files.newOutputStream(new File(dir, "a/b/c/e.txt").toPath())) {
            os.write("Hello, world 2".getBytes(UTF_8));
        }
        target = new File(resultDir, "test." + format);
    }

    private void verifyContent(final String format) throws IOException, ArchiveException {
        try (InputStream is = Files.newInputStream(target.toPath());
             BufferedInputStream bis = new BufferedInputStream(is);
             ArchiveInputStream ais = ArchiveStreamFactory.DEFAULT.createArchiveInputStream(format, bis)) {
            assertDir("a", ais.getNextEntry());
            assertDir("a/b", ais.getNextEntry());
            final ArchiveEntry n = ais.getNextEntry();
            assertNotNull(n);
            // File.list may return a/b/c or a/b/d.txt first
            if (n.getName().endsWith("/")) {
                assertDir("a/b/c", n);
                assertHelloWorld("a/b/c/e.txt", "2", ais.getNextEntry(), ais);
                assertHelloWorld("a/b/d.txt", "1", ais.getNextEntry(), ais);
            } else {
                assertHelloWorld("a/b/d.txt", "1", n, ais);
                assertDir("a/b/c", ais.getNextEntry());
                assertHelloWorld("a/b/c/e.txt", "2", ais.getNextEntry(), ais);
            }
        }
    }
}
