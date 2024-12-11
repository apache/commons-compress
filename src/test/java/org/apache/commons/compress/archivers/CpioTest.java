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
package org.apache.commons.compress.archivers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.apache.commons.compress.archivers.cpio.CpioArchiveInputStream;
import org.apache.commons.compress.archivers.cpio.CpioArchiveOutputStream;
import org.apache.commons.compress.archivers.cpio.CpioConstants;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

public final class CpioTest extends AbstractTest {

    @Test
    public void testCpioArchiveCreation() throws Exception {
        final File output = newTempFile("bla.cpio");

        final File file1 = getFile("test1.xml");
        final File file2 = getFile("test2.xml");

        try (OutputStream outputStream = Files.newOutputStream(output.toPath());
                ArchiveOutputStream<CpioArchiveEntry> archiveOutputStream = ArchiveStreamFactory.DEFAULT.createArchiveOutputStream("cpio", outputStream)) {
            // file 1
            archiveOutputStream.putArchiveEntry(new CpioArchiveEntry("test1.xml", file1.length()));
            archiveOutputStream.write(file1);
            archiveOutputStream.closeArchiveEntry();
            // file 2
            archiveOutputStream.putArchiveEntry(new CpioArchiveEntry("test2.xml", file2.length()));
            archiveOutputStream.write(file2);
            archiveOutputStream.closeArchiveEntry();
        }
    }

    @Test
    public void testCpioUnarchive() throws Exception {
        final File output = newTempFile("bla.cpio");
        final File file1 = getFile("test1.xml");
        final File file2 = getFile("test2.xml");
        final long file1Length = file1.length();
        final long file2Length = file2.length();

        try (OutputStream outputStream = Files.newOutputStream(output.toPath());
                ArchiveOutputStream<CpioArchiveEntry> archiveOutputStream = ArchiveStreamFactory.DEFAULT.createArchiveOutputStream("cpio", outputStream)) {
            // file 1
            CpioArchiveEntry entry = new CpioArchiveEntry("test1.xml", file1Length);
            entry.setMode(CpioConstants.C_ISREG);
            archiveOutputStream.putArchiveEntry(entry);
            archiveOutputStream.write(file1);
            archiveOutputStream.closeArchiveEntry();
            // file 2
            entry = new CpioArchiveEntry("test2.xml", file2Length);
            entry.setMode(CpioConstants.C_ISREG);
            archiveOutputStream.putArchiveEntry(entry);
            archiveOutputStream.write(file2);
            archiveOutputStream.closeArchiveEntry();
            archiveOutputStream.finish();
            archiveOutputStream.close();
            outputStream.close();
        }

        // Unarchive Operation
        final Map<String, File> result = new HashMap<>();
        try (InputStream inputStream = Files.newInputStream(output.toPath());
                ArchiveInputStream<?> archiveInputStream = ArchiveStreamFactory.DEFAULT.createArchiveInputStream("cpio", inputStream)) {
            archiveInputStream.forEach(entry -> {
                final File cpioget = newTempFile(entry.getName());
                Files.copy(archiveInputStream, cpioget.toPath());
                result.put(entry.getName(), cpioget);
            });
        }

        File testFile = result.get("test1.xml");
        assertTrue(testFile.exists(), "Expected " + testFile.getAbsolutePath() + " to exist");
        assertEquals(file1Length, testFile.length(), "length of " + testFile.getAbsolutePath());

        testFile = result.get("test2.xml");
        assertTrue(testFile.exists(), "Expected " + testFile.getAbsolutePath() + " to exist");
        assertEquals(file2Length, testFile.length(), "length of " + testFile.getAbsolutePath());
    }

    @Test
    public void testDirectoryEntryFromFile() throws Exception {
        final File archive = createTempFile("test.", ".cpio");
        final File dir = getTempDirFile();
        final long beforeArchiveWrite = dir.lastModified();
        final CpioArchiveEntry entryIn = new CpioArchiveEntry(dir, "foo");
        try (CpioArchiveOutputStream tos = new CpioArchiveOutputStream(Files.newOutputStream(archive.toPath()))) {
            tos.putArchiveEntry(entryIn);
            tos.closeArchiveEntry();
        }
        final CpioArchiveEntry entryOut;
        try (CpioArchiveInputStream tis = new CpioArchiveInputStream(Files.newInputStream(archive.toPath()))) {
            entryOut = tis.getNextCPIOEntry();
        }
        assertNotNull(entryOut);
        assertEquals("foo", entryOut.getName());
        assertEquals(0, entryOut.getSize());
        // CPIO stores time with a granularity of 1 second
        assertEquals(beforeArchiveWrite / 1000, entryOut.getLastModifiedDate().getTime() / 1000);
        assertTrue(entryOut.isDirectory());
    }

    @Test
    public void testExplicitDirectoryEntry() throws Exception {
        final File archive = createTempFile("test.", ".cpio");
        final long beforeArchiveWrite;
        try (CpioArchiveOutputStream tos = new CpioArchiveOutputStream(Files.newOutputStream(archive.toPath()))) {
            beforeArchiveWrite = getTempDirFile().lastModified();
            final CpioArchiveEntry in = new CpioArchiveEntry("foo/");
            in.setTime(beforeArchiveWrite / 1000);
            in.setMode(CpioConstants.C_ISDIR);
            tos.putArchiveEntry(in);
            tos.closeArchiveEntry();
        }
        final CpioArchiveEntry out;
        try (CpioArchiveInputStream tis = new CpioArchiveInputStream(Files.newInputStream(archive.toPath()))) {
            out = tis.getNextCPIOEntry();
        }
        assertNotNull(out);
        assertEquals("foo/", out.getName());
        assertEquals(0, out.getSize());
        assertEquals(beforeArchiveWrite / 1000, out.getLastModifiedDate().getTime() / 1000);
        assertTrue(out.isDirectory());
    }

    @Test
    public void testExplicitFileEntry() throws Exception {
        final File tmp = createTempFile();
        final File archive = createTempFile("test.", ".cpio");
        try (CpioArchiveOutputStream tos = new CpioArchiveOutputStream(Files.newOutputStream(archive.toPath()))) {
            final CpioArchiveEntry in = new CpioArchiveEntry("foo");
            in.setTime(tmp.lastModified() / 1000);
            in.setSize(tmp.length());
            in.setMode(CpioConstants.C_ISREG);
            tos.putArchiveEntry(in);
            tos.write(tmp);
            tos.closeArchiveEntry();
        }
        final CpioArchiveEntry out;
        try (CpioArchiveInputStream tis = new CpioArchiveInputStream(Files.newInputStream(archive.toPath()))) {
            out = tis.getNextCPIOEntry();
        }
        assertNotNull(out);
        assertEquals("foo", out.getName());
        assertEquals(tmp.length(), out.getSize());
        assertEquals(tmp.lastModified() / 1000, out.getLastModifiedDate().getTime() / 1000);
        assertFalse(out.isDirectory());
    }

    @Test
    public void testFileEntryFromFile() throws Exception {
        final File tmp = createTempFile();
        final File archive = createTempFile("test.", ".cpio");
        try (CpioArchiveOutputStream tos = new CpioArchiveOutputStream(Files.newOutputStream(archive.toPath()))) {
            final CpioArchiveEntry in = new CpioArchiveEntry(tmp, "foo");
            tos.putArchiveEntry(in);
            tos.write(tmp);
            tos.closeArchiveEntry();
        }
        final CpioArchiveEntry out;
        try (CpioArchiveInputStream tis = new CpioArchiveInputStream(Files.newInputStream(archive.toPath()))) {
            out = tis.getNextCPIOEntry();
        }
        assertNotNull(out);
        assertEquals("foo", out.getName());
        assertEquals(tmp.length(), out.getSize());
        assertEquals(tmp.lastModified() / 1000, out.getLastModifiedDate().getTime() / 1000);
        assertFalse(out.isDirectory());
    }

    /**
     * Tests COMPRESS-690.
     */
    @Test
    public void testSymbolicLinkFileEntry() throws Exception {
        final File tmp = createTempFile();
        final File archive = createTempFile("test.", ".cpio");
        final String name = "EntryName";
        final String nameLink = "LinkName";
        final Charset charset = StandardCharsets.UTF_8;
        try (CpioArchiveOutputStream tos = new CpioArchiveOutputStream(Files.newOutputStream(archive.toPath()))) {
            final CpioArchiveEntry entry = new CpioArchiveEntry(name);
            entry.setTime(tmp.lastModified() / 1000);
            entry.setSize(nameLink.length());
            entry.setMode(CpioConstants.C_ISLNK);
            assertTrue(entry.isSymbolicLink());
            tos.putArchiveEntry(entry);
            tos.write(nameLink.getBytes(charset));
            tos.closeArchiveEntry();
        }
        final CpioArchiveEntry entry;
        try (CpioArchiveInputStream tis = new CpioArchiveInputStream(Files.newInputStream(archive.toPath()))) {
            entry = tis.getNextEntry();
            assertEquals(nameLink, IOUtils.toString(tis, charset));
        }
        assertNotNull(entry);
        assertEquals(name, entry.getName());
        assertTrue(entry.isSymbolicLink());
        assertEquals(nameLink.length(), entry.getSize());
        assertEquals(tmp.lastModified() / 1000, entry.getLastModifiedDate().getTime() / 1000);
        assertFalse(entry.isDirectory());
    }

}
