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
import org.apache.commons.compress.archivers.StreamingNotSupportedException;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ExpanderTest extends AbstractTestCase {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private File archive;

    @Test
    public void sevenZTwoFileVersion() throws IOException, ArchiveException {
        setup7z();
        new Expander().expand("7z", archive, resultDir);
        verifyTargetDir();
    }

    @Test
    public void sevenZTwoFileVersionWithAutoDetection() throws IOException, ArchiveException {
        setup7z();
        new Expander().expand(archive, resultDir);
        verifyTargetDir();
    }

    @Test(expected = StreamingNotSupportedException.class)
    public void sevenZInputStreamVersion() throws IOException, ArchiveException {
        setup7z();
        try (InputStream i = new BufferedInputStream(Files.newInputStream(archive.toPath()))) {
            new Expander().expand("7z", i, resultDir);
        }
    }

    @Test(expected = StreamingNotSupportedException.class)
    public void sevenZInputStreamVersionWithAutoDetection() throws IOException, ArchiveException {
        setup7z();
        try (InputStream i = new BufferedInputStream(Files.newInputStream(archive.toPath()))) {
            new Expander().expand(i, resultDir);
        }
    }

    @Test
    public void sevenZChannelVersion() throws IOException, ArchiveException {
        setup7z();
        try (SeekableByteChannel c = FileChannel.open(archive.toPath(), StandardOpenOption.READ)) {
            new Expander().expand("7z", c, resultDir);
        }
        verifyTargetDir();
    }

    @Test
    public void sevenZFileVersion() throws IOException, ArchiveException {
        setup7z();
        try (SevenZFile f = new SevenZFile(archive)) {
            new Expander().expand(f, resultDir);
        }
        verifyTargetDir();
    }

    @Test
    public void zipFileVersion() throws IOException, ArchiveException {
        setupZip();
        try (ZipFile f = new ZipFile(archive)) {
            new Expander().expand(f, resultDir);
        }
        verifyTargetDir();
    }

    @Test
    public void fileCantEscapeViaAbsolutePath() throws IOException, ArchiveException {
        setupZip("/tmp/foo");
        try (ZipFile f = new ZipFile(archive)) {
            new Expander().expand(f, resultDir);
        }
        assertHelloWorld("tmp/foo", "1");
    }

    @Test
    public void fileCantEscapeDoubleDotPath() throws IOException, ArchiveException {
        thrown.expect(IOException.class);
        thrown.expectMessage("expanding ../foo would create file outside of");
        setupZip("../foo");
        try (ZipFile f = new ZipFile(archive)) {
            new Expander().expand(f, resultDir);
        }
    }

    @Test
    public void fileCantEscapeDoubleDotPathWithSimilarSibling() throws IOException, ArchiveException {
        String sibling = resultDir.getName() + "x";
        File s = new File(resultDir.getParentFile(), sibling);
        Assume.assumeFalse(s.exists());
        s.mkdirs();
        Assume.assumeTrue(s.exists());
        s.deleteOnExit();
        try {
            thrown.expect(IOException.class);
            thrown.expectMessage("expanding ../" + sibling + "/a would create file outside of");
            setupZip("../" + sibling + "/a");
            try (ZipFile f = new ZipFile(archive)) {
                new Expander().expand(f, resultDir);
            }
        } finally {
            tryHardToDelete(s);
        }
    }

    private void setup7z() throws IOException, ArchiveException {
        archive = new File(dir, "test.7z");
        File dummy = new File(dir, "x");
        try (OutputStream o = Files.newOutputStream(dummy.toPath())) {
            o.write(new byte[14]);
        }
        try (SevenZOutputFile aos = new SevenZOutputFile(archive)) {
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

    private void setupZip() throws IOException, ArchiveException {
        archive = new File(dir, "test.zip");
        File dummy = new File(dir, "x");
        try (OutputStream o = Files.newOutputStream(dummy.toPath())) {
            o.write(new byte[14]);
        }
        try (ArchiveOutputStream aos = new ArchiveStreamFactory()
             .createArchiveOutputStream("zip", Files.newOutputStream(archive.toPath()))) {
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

    private void setupZip(String entry) throws IOException, ArchiveException {
        archive = new File(dir, "test.zip");
        File dummy = new File(dir, "x");
        try (OutputStream o = Files.newOutputStream(dummy.toPath())) {
            o.write(new byte[14]);
        }
        try (ArchiveOutputStream aos = new ArchiveStreamFactory()
             .createArchiveOutputStream("zip", Files.newOutputStream(archive.toPath()))) {
            aos.putArchiveEntry(aos.createArchiveEntry(dummy, entry));
            aos.write("Hello, world 1".getBytes(StandardCharsets.UTF_8));
            aos.closeArchiveEntry();
            aos.finish();
        }
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
