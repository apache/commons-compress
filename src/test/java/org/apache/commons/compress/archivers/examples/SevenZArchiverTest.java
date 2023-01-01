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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.StreamingNotSupportedException;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SevenZArchiverTest extends AbstractTestCase {
    private File target;

    private void assertDir(final String expectedName, final ArchiveEntry entry) {
        assertNotNull(entry, () -> expectedName + " does not exists");
        assertEquals(expectedName + "/", entry.getName());
        assertTrue(entry.isDirectory(), expectedName + " is not a directory");
    }

    private void assertHelloWorld(final String expectedName, final String suffix, final ArchiveEntry entry, final SevenZFile z)
        throws IOException {
        assertNotNull(entry, () -> expectedName + " does not exists");
        assertEquals(expectedName, entry.getName());
        assertFalse(entry.isDirectory(), expectedName + " is a directory");
        final byte[] expected = ("Hello, world " + suffix).getBytes(UTF_8);
        final byte[] actual = new byte[expected.length];
        assertEquals(actual.length, z.read(actual));
        assertEquals(-1, z.read());
        assertArrayEquals(expected, actual);
    }

    @Test
    public void channelVersion() throws IOException, ArchiveException {
        try (SeekableByteChannel c = FileChannel.open(target.toPath(), StandardOpenOption.WRITE,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            new Archiver().create("7z", c, dir);
        }
        verifyContent();
    }

    @Test
    public void fileVersion() throws IOException, ArchiveException {
        new Archiver().create("7z", target, dir);
        verifyContent();
    }

    @Test
    public void outputStreamVersion() throws IOException {
        try (OutputStream os = Files.newOutputStream(target.toPath())) {
            assertThrows(StreamingNotSupportedException.class, () -> new Archiver().create("7z", os, dir));
        }
    }

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
        final File c = new File(dir, "a/b/c");
        c.mkdirs();
        try (OutputStream os = Files.newOutputStream(new File(dir, "a/b/d.txt").toPath())) {
            os.write("Hello, world 1".getBytes(UTF_8));
        }
        try (OutputStream os = Files.newOutputStream(new File(dir, "a/b/c/e.txt").toPath())) {
            os.write("Hello, world 2".getBytes(UTF_8));
        }
        target = new File(resultDir, "test.7z");
    }

    // not really a 7z test but I didn't feel like adding a new test just for this
    @Test
    public void unknownFormat() throws IOException {
        try (SeekableByteChannel c = FileChannel.open(target.toPath(), StandardOpenOption.WRITE, StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING)) {
            assertThrows(ArchiveException.class, () -> new Archiver().create("unknown format", c, dir));
        }
    }

    private void verifyContent() throws IOException {
        try (SevenZFile z = new SevenZFile(target)) {
            assertDir("a", z.getNextEntry());
            assertDir("a/b", z.getNextEntry());
            final ArchiveEntry n = z.getNextEntry();
            assertNotNull(n);
            // File.list may return a/b/c or a/b/d.txt first
            if (n.getName().endsWith("/")) {
                assertDir("a/b/c", n);
                assertHelloWorld("a/b/c/e.txt", "2", z.getNextEntry(), z);
                assertHelloWorld("a/b/d.txt", "1", z.getNextEntry(), z);
            } else {
                assertHelloWorld("a/b/d.txt", "1", n, z);
                assertDir("a/b/c", z.getNextEntry());
                assertHelloWorld("a/b/c/e.txt", "2", z.getNextEntry(), z);
            }
        }
    }
}
