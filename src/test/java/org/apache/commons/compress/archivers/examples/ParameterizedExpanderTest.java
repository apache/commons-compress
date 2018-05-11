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
public class ParameterizedExpanderTest extends AbstractTestCase {

    // 7z and ZIP using ZipFile is in ExpanderTest
    @Parameters(name = "format={0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(
            new Object[] { "tar" },
            new Object[] { "cpio" },
            new Object[] { "zip" }
        );
    }

    private final String format;
    private File archive;

    public ParameterizedExpanderTest(String format) {
        this.format = format;
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        archive = new File(dir, "test." + format);
        File dummy = new File(dir, "x");
        try (OutputStream o = Files.newOutputStream(dummy.toPath())) {
            o.write(new byte[14]);
        }
        try (ArchiveOutputStream aos = new ArchiveStreamFactory()
             .createArchiveOutputStream(format, Files.newOutputStream(archive.toPath()))) {
            aos.putArchiveEntry(aos.createArchiveEntry(dir, "a"));
            aos.closeArchiveEntry();
            aos.putArchiveEntry(aos.createArchiveEntry(dir, "a/b"));
            aos.closeArchiveEntry();
            aos.putArchiveEntry(aos.createArchiveEntry(dir, "a/b/c"));
            aos.closeArchiveEntry();
            aos.putArchiveEntry(aos.createArchiveEntry(dummy, "a/b/d.txt"));
            aos.write("Hello, world 1".getBytes(StandardCharsets.UTF_8));
            aos.closeArchiveEntry();
            aos.putArchiveEntry(aos.createArchiveEntry(dummy, "a/b/c/e.txt"));
            aos.write("Hello, world 2".getBytes(StandardCharsets.UTF_8));
            aos.closeArchiveEntry();
            aos.finish();
        }
    }

    @Test
    public void fileVersion() throws IOException, ArchiveException {
        new Expander().expand(format, archive, resultDir);
        verifyTargetDir();
    }

    @Test
    public void fileVersionWithAutoDetection() throws IOException, ArchiveException {
        new Expander().expand(archive, resultDir);
        verifyTargetDir();
    }

    @Test
    public void inputStreamVersion() throws IOException, ArchiveException {
        try (InputStream i = new BufferedInputStream(Files.newInputStream(archive.toPath()))) {
            new Expander().expand(format, i, resultDir);
        }
        verifyTargetDir();
    }

    @Test
    public void inputStreamVersionWithAutoDetection() throws IOException, ArchiveException {
        try (InputStream i = new BufferedInputStream(Files.newInputStream(archive.toPath()))) {
            new Expander().expand(i, resultDir);
        }
        verifyTargetDir();
    }

    @Test
    public void channelVersion() throws IOException, ArchiveException {
        try (SeekableByteChannel c = FileChannel.open(archive.toPath(), StandardOpenOption.READ)) {
            new Expander().expand(format, c, resultDir);
        }
        verifyTargetDir();
    }

    @Test
    public void archiveInputStreamVersion() throws IOException, ArchiveException {
        try (InputStream i = new BufferedInputStream(Files.newInputStream(archive.toPath()));
             ArchiveInputStream ais = new ArchiveStreamFactory().createArchiveInputStream(format, i)) {
            new Expander().expand(ais, resultDir);
        }
        verifyTargetDir();
    }

    private void verifyTargetDir() throws IOException {
        Assert.assertTrue("a has not been created", new File(resultDir, "a").isDirectory());
        Assert.assertTrue("a/b has not been created", new File(resultDir, "a/b").isDirectory());
        Assert.assertTrue("a/b/c has not been created", new File(resultDir, "a/b/c").isDirectory());
        assertHelloWorld("a/b/d.txt", "1");
        assertHelloWorld("a/b/c/e.txt", "2");
    }

    private void assertHelloWorld(String fileName, String suffix) throws IOException {
        Assert.assertTrue(fileName + " does not exist", new File(resultDir, fileName).isFile());
        byte[] expected = ("Hello, world " + suffix).getBytes(StandardCharsets.UTF_8);
        try (InputStream is = Files.newInputStream(new File(resultDir, fileName).toPath())) {
            byte[] actual = IOUtils.toByteArray(is);
            Assert.assertArrayEquals(expected, actual);
        }
    }

}
