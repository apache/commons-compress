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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.StreamingNotSupportedException;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SevenZArchiverTest extends AbstractTestCase {
    private File target;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        File c = new File(dir, "a/b/c");
        c.mkdirs();
        try (OutputStream os = Files.newOutputStream(new File(dir, "a/b/d.txt").toPath())) {
            os.write("Hello, world 1".getBytes(StandardCharsets.UTF_8));
        }
        try (OutputStream os = Files.newOutputStream(new File(dir, "a/b/c/e.txt").toPath())) {
            os.write("Hello, world 2".getBytes(StandardCharsets.UTF_8));
        }
        target = new File(resultDir, "test.7z");
    }

    @Test
    public void fileVersion() throws IOException, ArchiveException {
        new Archiver().create("7z", target, dir);
        verifyContent();
    }

    @Test(expected = StreamingNotSupportedException.class)
    public void outputStreamVersion() throws IOException, ArchiveException {
        try (OutputStream os = Files.newOutputStream(target.toPath())) {
            new Archiver().create("7z", os, dir);
        }
    }

    @Test
    public void channelVersion() throws IOException, ArchiveException {
        try (SeekableByteChannel c = FileChannel.open(target.toPath(), StandardOpenOption.WRITE,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            new Archiver().create("7z", c, dir);
        }
        verifyContent();
    }

    // not really a 7z test but I didn't feel like adding a new test just for this
    @Test(expected = ArchiveException.class)
    public void unknownFormat() throws IOException, ArchiveException {
        try (SeekableByteChannel c = FileChannel.open(target.toPath(), StandardOpenOption.WRITE,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            new Archiver().create("unknown format", c, dir);
        }
    }

    private void verifyContent() throws IOException, ArchiveException {
        try (SevenZFile z = new SevenZFile(target)) {
            assertDir("a", z.getNextEntry());
            assertDir("a/b", z.getNextEntry());
            ArchiveEntry n = z.getNextEntry();
            Assert.assertNotNull(n);
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

    private void assertDir(String expectedName, ArchiveEntry entry) {
        Assert.assertNotNull(expectedName + " does not exists", entry);
        Assert.assertEquals(expectedName + "/", entry.getName());
        Assert.assertTrue(expectedName + " is not a directory", entry.isDirectory());
    }

    private void assertHelloWorld(String expectedName, String suffix, ArchiveEntry entry, SevenZFile z)
        throws IOException {
        Assert.assertNotNull(expectedName + " does not exists", entry);
        Assert.assertEquals(expectedName, entry.getName());
        Assert.assertFalse(expectedName + " is a directory", entry.isDirectory());
        byte[] expected = ("Hello, world " + suffix).getBytes(StandardCharsets.UTF_8);
        byte[] actual = new byte[expected.length];
        Assert.assertEquals(actual.length, z.read(actual));
        Assert.assertEquals(-1, z.read());
        Assert.assertArrayEquals(expected, actual);
    }
}
