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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ParameterizedArchiverTest extends AbstractTestCase {

    // can't test 7z here as 7z cannot write to non-seekable streams
    // and reading logic would be different as well - see
    // SevenZArchiverTest class
    @Parameters(name = "format={0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(
            new Object[] { "tar" },
            new Object[] { "cpio" },
            new Object[] { "zip" }
        );
    }

    private final String format;
    private File target;

    public ParameterizedArchiverTest(String format) {
        this.format = format;
    }

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
        target = new File(resultDir, "test." + format);
    }

    @Test
    public void fileVersion() throws IOException, ArchiveException {
        new Archiver().create(format, target, dir);
        verifyContent();
    }

    @Test
    public void outputStreamVersion() throws IOException, ArchiveException {
        try (OutputStream os = Files.newOutputStream(target.toPath())) {
            new Archiver().create(format, os, dir);
        }
        verifyContent();
    }

    @Test
    public void channelVersion() throws IOException, ArchiveException {
        try (SeekableByteChannel c = FileChannel.open(target.toPath(), StandardOpenOption.WRITE,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            new Archiver().create(format, c, dir);
        }
        verifyContent();
    }

    @Test
    public void archiveStreamVersion() throws IOException, ArchiveException {
        try (OutputStream os = Files.newOutputStream(target.toPath());
             ArchiveOutputStream aos = new ArchiveStreamFactory().createArchiveOutputStream(format, os)) {
            new Archiver().create(aos, dir);
        }
        verifyContent();
    }

    private void verifyContent() throws IOException, ArchiveException {
        try (InputStream is = Files.newInputStream(target.toPath());
             BufferedInputStream bis = new BufferedInputStream(is);
             ArchiveInputStream ais = new ArchiveStreamFactory().createArchiveInputStream(format, bis)) {
            assertDir("a", ais.getNextEntry());
            assertDir("a/b", ais.getNextEntry());
            ArchiveEntry n = ais.getNextEntry();
            Assert.assertNotNull(n);
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

    private void assertDir(String expectedName, ArchiveEntry entry) {
        Assert.assertNotNull(expectedName + " does not exists", entry);
        Assert.assertEquals(expectedName + "/", entry.getName());
        Assert.assertTrue(expectedName + " is not a directory", entry.isDirectory());
    }

    private void assertHelloWorld(String expectedName, String suffix, ArchiveEntry entry, InputStream is)
        throws IOException {
        Assert.assertNotNull(expectedName + " does not exists", entry);
        Assert.assertEquals(expectedName, entry.getName());
        Assert.assertFalse(expectedName + " is a directory", entry.isDirectory());
        byte[] expected = ("Hello, world " + suffix).getBytes(StandardCharsets.UTF_8);
        byte[] actual = IOUtils.toByteArray(is);
        Assert.assertArrayEquals(expected, actual);
    }
}
