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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.StreamingNotSupportedException;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;
import org.apache.commons.compress.archivers.tar.TarFile;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.Test;

public class ExpanderTest extends AbstractTestCase {

    private File archive;

    private void assertHelloWorld(final String fileName, final String suffix) throws IOException {
        assertTrue(new File(tempResultDir, fileName).isFile(), fileName + " does not exist");
        final byte[] expected = ("Hello, world " + suffix).getBytes(UTF_8);
        try (InputStream is = Files.newInputStream(new File(tempResultDir, fileName).toPath())) {
            final byte[] actual = IOUtils.toByteArray(is);
            assertArrayEquals(expected, actual);
        }
    }

    private void setup7z() throws IOException {
        archive = new File(getTempDirFile(), "test.7z");
        final File dummy = new File(getTempDirFile(), "x");
        try (OutputStream o = Files.newOutputStream(dummy.toPath())) {
            o.write(new byte[14]);
        }
        try (SevenZOutputFile aos = new SevenZOutputFile(archive)) {
            aos.putArchiveEntry(aos.createArchiveEntry(getTempDirFile(), "a"));
            aos.closeArchiveEntry();
            aos.putArchiveEntry(aos.createArchiveEntry(getTempDirFile(), "a/b"));
            aos.closeArchiveEntry();
            aos.putArchiveEntry(aos.createArchiveEntry(getTempDirFile(), "a/b/c"));
            aos.closeArchiveEntry();
            aos.putArchiveEntry(aos.createArchiveEntry(dummy, "a/b/d.txt"));
            aos.write("Hello, world 1".getBytes(UTF_8));
            aos.closeArchiveEntry();
            aos.putArchiveEntry(aos.createArchiveEntry(dummy, "a/b/c/e.txt"));
            aos.write("Hello, world 2".getBytes(UTF_8));
            aos.closeArchiveEntry();
            aos.finish();
        }
    }

    private void setupTar() throws IOException, ArchiveException {
        archive = new File(getTempDirFile(), "test.tar");
        final File dummy = new File(getTempDirFile(), "x");
        try (OutputStream o = Files.newOutputStream(dummy.toPath())) {
            o.write(new byte[14]);
        }
        try (@SuppressWarnings("resource") // Files.newOutputStream result closed by ArchiveOutputStream
        ArchiveOutputStream<ArchiveEntry> aos = ArchiveStreamFactory.DEFAULT.createArchiveOutputStream("tar", Files.newOutputStream(archive.toPath()))) {
            aos.putArchiveEntry(aos.createArchiveEntry(getTempDirFile(), "a"));
            aos.closeArchiveEntry();
            aos.putArchiveEntry(aos.createArchiveEntry(getTempDirFile(), "a/b"));
            aos.closeArchiveEntry();
            aos.putArchiveEntry(aos.createArchiveEntry(getTempDirFile(), "a/b/c"));
            aos.closeArchiveEntry();
            aos.putArchiveEntry(aos.createArchiveEntry(dummy, "a/b/d.txt"));
            aos.write("Hello, world 1".getBytes(UTF_8));
            aos.closeArchiveEntry();
            aos.putArchiveEntry(aos.createArchiveEntry(dummy, "a/b/c/e.txt"));
            aos.write("Hello, world 2".getBytes(UTF_8));
            aos.closeArchiveEntry();
            aos.finish();
        }
    }

    private void setupTarForCompress603() throws IOException, ArchiveException {
        archive = new File(getTempDirFile(), "test.tar");
        final File dummy = new File(getTempDirFile(), "x");
        try (OutputStream o = Files.newOutputStream(dummy.toPath())) {
            o.write(new byte[14]);
        }
        try (@SuppressWarnings("resource") // Files.newOutputStream result closed by ArchiveOutputStream
        ArchiveOutputStream<ArchiveEntry> aos = ArchiveStreamFactory.DEFAULT.createArchiveOutputStream("tar", Files.newOutputStream(archive.toPath()))) {
            aos.putArchiveEntry(aos.createArchiveEntry(getTempDirFile(), "./"));
            aos.closeArchiveEntry();
            aos.putArchiveEntry(aos.createArchiveEntry(getTempDirFile(), "./a"));
            aos.closeArchiveEntry();
            aos.putArchiveEntry(aos.createArchiveEntry(getTempDirFile(), "./a/b"));
            aos.closeArchiveEntry();
            aos.putArchiveEntry(aos.createArchiveEntry(getTempDirFile(), "./a/b/c"));
            aos.closeArchiveEntry();
            aos.putArchiveEntry(aos.createArchiveEntry(dummy, "./a/b/d.txt"));
            aos.write("Hello, world 1".getBytes(UTF_8));
            aos.closeArchiveEntry();
            aos.putArchiveEntry(aos.createArchiveEntry(dummy, "./a/b/c/e.txt"));
            aos.write("Hello, world 2".getBytes(UTF_8));
            aos.closeArchiveEntry();
            aos.finish();
        }
    }

    private void setupZip() throws IOException, ArchiveException {
        archive = new File(getTempDirFile(), "test.zip");
        final File dummy = new File(getTempDirFile(), "x");
        try (OutputStream o = Files.newOutputStream(dummy.toPath())) {
            o.write(new byte[14]);
        }
        try (@SuppressWarnings("resource") // // Files.newOutputStream result closed by ArchiveOutputStream
        ArchiveOutputStream<ArchiveEntry> aos = ArchiveStreamFactory.DEFAULT.createArchiveOutputStream("zip", Files.newOutputStream(archive.toPath()))) {
            aos.putArchiveEntry(aos.createArchiveEntry(getTempDirFile(), "a"));
            aos.closeArchiveEntry();
            aos.putArchiveEntry(aos.createArchiveEntry(getTempDirFile(), "a/b"));
            aos.closeArchiveEntry();
            aos.putArchiveEntry(aos.createArchiveEntry(getTempDirFile(), "a/b/c"));
            aos.closeArchiveEntry();
            aos.putArchiveEntry(aos.createArchiveEntry(dummy, "a/b/d.txt"));
            aos.write("Hello, world 1".getBytes(UTF_8));
            aos.closeArchiveEntry();
            aos.putArchiveEntry(aos.createArchiveEntry(dummy, "a/b/c/e.txt"));
            aos.write("Hello, world 2".getBytes(UTF_8));
            aos.closeArchiveEntry();
            aos.finish();
        }
    }

    private void setupZip(final String entry) throws IOException, ArchiveException {
        archive = new File(getTempDirFile(), "test.zip");
        final File dummy = new File(getTempDirFile(), "x");
        try (OutputStream o = Files.newOutputStream(dummy.toPath())) {
            o.write(new byte[14]);
        }
        try (@SuppressWarnings("resource") // Files.newOutputStream result closed by ArchiveOutputStream
        ArchiveOutputStream<ArchiveEntry> aos = ArchiveStreamFactory.DEFAULT.createArchiveOutputStream("zip", Files.newOutputStream(archive.toPath()))) {
            aos.putArchiveEntry(aos.createArchiveEntry(dummy, entry));
            aos.write("Hello, world 1".getBytes(UTF_8));
            aos.closeArchiveEntry();
            aos.finish();
        }
    }

    @Test
    public void tarFileVersion() throws IOException, ArchiveException {
        setupTar();
        try (TarFile f = new TarFile(archive)) {
            new Expander().expand(f, tempResultDir);
        }
        verifyTargetDir();
    }

    @Test
    public void testCompress603Tar() throws IOException, ArchiveException {
        setupTarForCompress603();
        try (TarFile f = new TarFile(archive)) {
            new Expander().expand(f, tempResultDir);
        }
        verifyTargetDir();
    }

    @Test
    public void testFileCantEscapeDoubleDotPath() throws IOException, ArchiveException {
        setupZip("../foo");
        try (ZipFile f = new ZipFile(archive)) {
            assertThrows(IOException.class, () -> new Expander().expand(f, tempResultDir));
        }
    }

    @Test
    public void testFileCantEscapeDoubleDotPathWithSimilarSibling() throws IOException, ArchiveException {
        final String sibling = tempResultDir.getName() + "x";
        final File s = new File(tempResultDir.getParentFile(), sibling);
        assumeFalse(s.exists());
        s.mkdirs();
        assumeTrue(s.exists());
        s.deleteOnExit();
        try {
            setupZip("../" + sibling + "/a");
            try (ZipFile f = new ZipFile(archive)) {
                assertThrows(IOException.class, () -> new Expander().expand(f, tempResultDir));
            }
        } finally {
            forceDelete(s);
        }
    }

    @Test
    public void testFileCantEscapeViaAbsolutePath() throws IOException, ArchiveException {
        setupZip("/tmp/foo");
        try (ZipFile f = new ZipFile(archive)) {
            assertThrows(IOException.class, () -> new Expander().expand(f, tempResultDir));
        }
        assertFalse(new File(tempResultDir, "tmp/foo").isFile());
    }

    @Test
    public void testSevenZChannelVersion() throws IOException, ArchiveException {
        setup7z();
        try (SeekableByteChannel c = FileChannel.open(archive.toPath(), StandardOpenOption.READ)) {
            new Expander().expand("7z", c, tempResultDir);
        }
        verifyTargetDir();
    }

    @Test
    public void testSevenZFileVersion() throws IOException {
        setup7z();
        try (SevenZFile f = new SevenZFile(archive)) {
            new Expander().expand(f, tempResultDir);
        }
        verifyTargetDir();
    }

    @Test
    public void testSevenZInputStreamVersion() throws IOException {
        setup7z();
        try (InputStream i = new BufferedInputStream(Files.newInputStream(archive.toPath()))) {
            assertThrows(StreamingNotSupportedException.class, () -> new Expander().expand("7z", i, tempResultDir));
        }
    }

    @Test
    public void testSevenZInputStreamVersionWithAutoDetection() throws IOException {
        setup7z();
        try (InputStream i = new BufferedInputStream(Files.newInputStream(archive.toPath()))) {
            assertThrows(StreamingNotSupportedException.class, () -> new Expander().expand(i, tempResultDir));
        }
    }

    @Test
    public void testSevenZTwoFileVersion() throws IOException, ArchiveException {
        setup7z();
        new Expander().expand("7z", archive, tempResultDir);
        verifyTargetDir();
    }

    @Test
    public void testSevenZTwoFileVersionWithAutoDetection() throws IOException, ArchiveException {
        setup7z();
        new Expander().expand(archive, tempResultDir);
        verifyTargetDir();
    }

    @Test
    public void testZipFileVersion() throws IOException, ArchiveException {
        setupZip();
        try (ZipFile f = new ZipFile(archive)) {
            new Expander().expand(f, tempResultDir);
        }
        verifyTargetDir();
    }

    private void verifyTargetDir() throws IOException {
        assertTrue(new File(tempResultDir, "a").isDirectory(), "a has not been created");
        assertTrue(new File(tempResultDir, "a/b").isDirectory(), "a/b has not been created");
        assertTrue(new File(tempResultDir, "a/b/c").isDirectory(), "a/b/c has not been created");
        assertHelloWorld("a/b/d.txt", "1");
        assertHelloWorld("a/b/c/e.txt", "2");
    }

}
