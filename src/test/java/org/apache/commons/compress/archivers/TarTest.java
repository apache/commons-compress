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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;
import org.apache.commons.compress.archivers.tar.TarFile;
import org.apache.commons.compress.utils.ByteUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import shaded.org.apache.commons.lang3.StringUtils;

public final class TarTest extends AbstractTest {

    private String createLongName(final int nameLength) {
        return StringUtils.repeat('a', nameLength);
    }

    private byte[] createTarWithOneLongNameEntry(final String longName) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tos = new TarArchiveOutputStream(bos)) {
            tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
            final TarArchiveEntry longFileNameEntry = new TarArchiveEntry(longName);
            tos.putArchiveEntry(longFileNameEntry);
            tos.closeArchiveEntry();
        }
        return bos.toByteArray();
    }

    @Test
    public void testCOMPRESS114() throws Exception {
        final File input = getFile("COMPRESS-114.tar");
        try (InputStream is = Files.newInputStream(input.toPath());
                TarArchiveInputStream in = new TarArchiveInputStream(is, StandardCharsets.ISO_8859_1.name())) {
            TarArchiveEntry entry = in.getNextEntry();
            assertEquals("3\u00b1\u00b1\u00b1F06\u00b1W2345\u00b1ZB\u00b1la\u00b1\u00b1\u00b1\u00b1\u00b1\u00b1\u00b1\u00b1BLA", entry.getName());
            assertEquals(TarConstants.LF_NORMAL, entry.getLinkFlag());
            entry = in.getNextEntry();
            assertEquals("0302-0601-3\u00b1\u00b1\u00b1F06\u00b1W2345\u00b1ZB\u00b1la\u00b1\u00b1\u00b1\u00b1\u00b1\u00b1\u00b1\u00b1BLA", entry.getName());
            assertEquals(TarConstants.LF_NORMAL, entry.getLinkFlag());
        }
    }

    @Test
    public void testCOMPRESS178() throws Exception {
        final File input = getFile("COMPRESS-178-fail.tar");
        try (InputStream is = Files.newInputStream(input.toPath());
                ArchiveInputStream<?> in = ArchiveStreamFactory.DEFAULT.createArchiveInputStream("tar", is)) {
            final IOException e = assertThrows(IOException.class, in::getNextEntry, "Expected IOException");
            final Throwable t = e.getCause();
            assertInstanceOf(IllegalArgumentException.class, t, "Expected cause = IllegalArgumentException");
        }
    }

    @Test
    public void testCOMPRESS178Lenient() throws Exception {
        final File input = getFile("COMPRESS-178-fail.tar");
        try (InputStream is = Files.newInputStream(input.toPath());
                ArchiveInputStream<?> in = new TarArchiveInputStream(is, true)) {
            in.getNextEntry();
        }
    }

    @Test
    public void testDirectoryEntryFromFile() throws Exception {
        final File archive = createTempFile("test.", ".tar");
        final long beforeArchiveWrite;
        try (TarArchiveOutputStream tos = new TarArchiveOutputStream(Files.newOutputStream(archive.toPath()))) {
            final File dir = getTempDirFile();
            beforeArchiveWrite = dir.lastModified();
            final TarArchiveEntry in = new TarArchiveEntry(dir, "foo");
            tos.putArchiveEntry(in);
            tos.closeArchiveEntry();
        }
        final TarArchiveEntry out;
        try (TarArchiveInputStream tis = new TarArchiveInputStream(Files.newInputStream(archive.toPath()))) {
            out = tis.getNextTarEntry();
        }
        assertNotNull(out);
        assertEquals("foo/", out.getName());
        assertEquals(TarConstants.LF_DIR, out.getLinkFlag());
        assertEquals(0, out.getSize());
        // TAR stores time with a granularity of 1 second
        assertEquals(beforeArchiveWrite / 1000, out.getLastModifiedDate().getTime() / 1000);
        assertTrue(out.isDirectory());
    }

    @Test
    public void testDirectoryRead() throws IOException {
        final File input = getFile("directory.tar");
        try (InputStream is = Files.newInputStream(input.toPath());
                TarArchiveInputStream in = new TarArchiveInputStream(is)) {
            final TarArchiveEntry directoryEntry = in.getNextTarEntry();
            assertEquals("directory/", directoryEntry.getName());
            assertEquals(TarConstants.LF_DIR, directoryEntry.getLinkFlag());
            assertTrue(directoryEntry.isDirectory());
            final byte[] directoryRead = IOUtils.toByteArray(in);
            assertArrayEquals(ByteUtils.EMPTY_BYTE_ARRAY, directoryRead);
        }
    }

    @Test
    public void testExplicitDirectoryEntry() throws Exception {
        final File archive = createTempFile("test.", ".tar");
        final long beforeArchiveWrite;
        final TarArchiveEntry in = new TarArchiveEntry("foo/");
        try (TarArchiveOutputStream tos = new TarArchiveOutputStream(Files.newOutputStream(archive.toPath()))) {
            beforeArchiveWrite = getTempDirFile().lastModified();
            in.setModTime(beforeArchiveWrite);
            tos.putArchiveEntry(in);
            tos.closeArchiveEntry();
        }
        final TarArchiveEntry out;
        try (TarArchiveInputStream tis = new TarArchiveInputStream(Files.newInputStream(archive.toPath()))) {
            out = tis.getNextTarEntry();
        }
        assertNotNull(out);
        assertEquals("foo/", out.getName());
        assertEquals(TarConstants.LF_DIR, in.getLinkFlag());
        assertEquals(0, out.getSize());
        assertEquals(beforeArchiveWrite / 1000, out.getLastModifiedDate().getTime() / 1000);
        assertTrue(out.isDirectory());
    }

    @Test
    public void testExplicitFileEntry() throws Exception {
        final File file = createTempFile();
        final File archive = createTempFile("test.", ".tar");
        try (TarArchiveOutputStream outputStream = new TarArchiveOutputStream(Files.newOutputStream(archive.toPath()))) {
            final TarArchiveEntry entryIn = new TarArchiveEntry("foo");
            entryIn.setModTime(file.lastModified());
            entryIn.setSize(file.length());
            outputStream.putArchiveEntry(entryIn);
            outputStream.write(file);
            outputStream.closeArchiveEntry();
        }
        final TarArchiveEntry entryOut;
        try (TarArchiveInputStream tis = new TarArchiveInputStream(Files.newInputStream(archive.toPath()))) {
            entryOut = tis.getNextTarEntry();
        }
        assertNotNull(entryOut);
        assertEquals("foo", entryOut.getName());
        assertEquals(TarConstants.LF_NORMAL, entryOut.getLinkFlag());
        assertEquals(file.length(), entryOut.getSize());
        assertEquals(file.lastModified() / 1000, entryOut.getLastModifiedDate().getTime() / 1000);
        assertFalse(entryOut.isDirectory());
    }

    @Test
    public void testFileEntryFromFile() throws Exception {
        final File file = createTempFile();
        final File archive = createTempFile("test.", ".tar");
        final TarArchiveEntry in = new TarArchiveEntry(file, "foo");
        try (TarArchiveOutputStream outputStream = new TarArchiveOutputStream(Files.newOutputStream(archive.toPath()))) {
            outputStream.putArchiveEntry(in);
            outputStream.write(file);
            outputStream.closeArchiveEntry();
        }
        final TarArchiveEntry out;
        try (TarArchiveInputStream tis = new TarArchiveInputStream(Files.newInputStream(archive.toPath()))) {
            out = tis.getNextTarEntry();
        }
        assertNotNull(out);
        assertEquals("foo", out.getName());
        assertEquals(TarConstants.LF_NORMAL, out.getLinkFlag());
        assertEquals(file.length(), out.getSize());
        assertEquals(file.lastModified() / 1000, out.getLastModifiedDate().getTime() / 1000);
        assertFalse(out.isDirectory());
    }

    @Test
    public void testLongNameLargerThanBuffer() throws IOException {
        final List<Integer> nameLength = Arrays.asList(300, 4096);

        for (final Integer length : nameLength) {
            final String fileName = createLongName(length);
            assertEquals(length.intValue(), fileName.length());
            final byte[] data = createTarWithOneLongNameEntry(fileName);
            try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
                    TarArchiveInputStream tis = new TarArchiveInputStream(bis)) {
                assertEquals(fileName, tis.getNextTarEntry().getName());
            }
        }
    }

    @Test
    public void testTarArchiveCreation() throws Exception {
        final File output = newTempFile("bla.tar");
        final File file1 = getFile("test1.xml");
        try (OutputStream out = Files.newOutputStream(output.toPath());
                TarArchiveOutputStream os = ArchiveStreamFactory.DEFAULT.createArchiveOutputStream("tar", out)) {
            final TarArchiveEntry entry = new TarArchiveEntry("testdata/test1.xml");
            entry.setModTime(0);
            entry.setSize(file1.length());
            entry.setUserId(0);
            entry.setGroupId(0);
            entry.setUserName("avalon");
            entry.setGroupName("excalibur");
            entry.setMode(0100000);
            os.putArchiveEntry(entry);
            os.write(file1);
            os.closeArchiveEntry();
        }
    }

    @Test
    public void testTarArchiveLongNameCreation() throws Exception {
        final String name = "testdata/12345678901234567890123456789012345678901234567890123456789012345678901234567890123456.xml";
        final byte[] bytes = name.getBytes(UTF_8);
        assertEquals(bytes.length, 99);

        final File output = newTempFile("bla.tar");
        final File file1 = getFile("test1.xml");
        final TarArchiveEntry entry = new TarArchiveEntry(name);
        try (OutputStream out = Files.newOutputStream(output.toPath());
                TarArchiveOutputStream os = ArchiveStreamFactory.DEFAULT.createArchiveOutputStream("tar", out)) {
            entry.setModTime(0);
            entry.setSize(file1.length());
            entry.setUserId(0);
            entry.setGroupId(0);
            entry.setUserName("avalon");
            entry.setGroupName("excalibur");
            entry.setMode(0100000);
            os.putArchiveEntry(entry);
            os.write(file1);
            os.closeArchiveEntry();
        }

        final String toLongName = "testdata/123456789012345678901234567890123456789012345678901234567890123456789012345678901234567.xml";
        final File output2 = newTempFile("bla.tar");
        try (OutputStream out2 = Files.newOutputStream(output2.toPath());
                TarArchiveOutputStream os2 = ArchiveStreamFactory.DEFAULT.createArchiveOutputStream("tar", out2);) {
            final TarArchiveEntry entry2 = new TarArchiveEntry(toLongName);
            entry2.setModTime(0);
            entry2.setSize(file1.length());
            entry2.setUserId(0);
            entry2.setGroupId(0);
            entry2.setUserName("avalon");
            entry2.setGroupName("excalibur");
            entry2.setMode(0100000);
            os2.putArchiveEntry(entry);
            os2.write(file1);
            os2.closeArchiveEntry();
        } catch (final IOException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testTarFileCOMPRESS114() throws Exception {
        final File input = getFile("COMPRESS-114.tar");
        try (TarFile tarFile = new TarFile(input, StandardCharsets.ISO_8859_1.name())) {
            final List<TarArchiveEntry> entries = tarFile.getEntries();
            TarArchiveEntry entry = entries.get(0);
            assertEquals("3\u00b1\u00b1\u00b1F06\u00b1W2345\u00b1ZB\u00b1la\u00b1\u00b1\u00b1\u00b1\u00b1\u00b1\u00b1\u00b1BLA", entry.getName());
            assertEquals(TarConstants.LF_NORMAL, entry.getLinkFlag());
            entry = entries.get(1);
            assertEquals("0302-0601-3\u00b1\u00b1\u00b1F06\u00b1W2345\u00b1ZB\u00b1la\u00b1\u00b1\u00b1\u00b1\u00b1\u00b1\u00b1\u00b1BLA", entry.getName());
            assertEquals(TarConstants.LF_NORMAL, entry.getLinkFlag());
        }
    }

    @Test
    public void testTarFileCOMPRESS178() throws Exception {
        final File input = getFile("COMPRESS-178-fail.tar");
        final IOException e = assertThrows(IOException.class, () -> {
            try (TarFile tarFile = new TarFile(input)) {
                // Compared to the TarArchiveInputStream all entries are read when instantiating the tar file
            }
        }, "Expected IOException");
        final Throwable t = e.getCause();
        assertInstanceOf(IllegalArgumentException.class, t, "Expected cause = IllegalArgumentException");
    }

    @Test
    public void testTarFileCOMPRESS178Lenient() throws Exception {
        final File input = getFile("COMPRESS-178-fail.tar");
        try (TarFile tarFile = new TarFile(input, true)) {
            // Compared to the TarArchiveInputStream all entries are read when instantiating the tar file
        }
    }

    @Test
    public void testTarFileDirectoryEntryFromFile() throws Exception {
        final File archive = createTempFile("test.", ".tar");
        final File dir = getTempDirFile();
        try (TarArchiveOutputStream tos = new TarArchiveOutputStream(Files.newOutputStream(archive.toPath()))) {
            final long beforeArchiveWrite = dir.lastModified();
            final TarArchiveEntry in = new TarArchiveEntry(dir, "foo");
            tos.putArchiveEntry(in);
            tos.closeArchiveEntry();
            tos.close();
            try (TarFile tarFile = new TarFile(archive)) {
                final TarArchiveEntry entry = tarFile.getEntries().get(0);
                assertNotNull(entry);
                assertEquals("foo/", entry.getName());
                assertEquals(TarConstants.LF_DIR, entry.getLinkFlag());
                assertEquals(0, entry.getSize());
                // TAR stores time with a granularity of 1 second
                assertEquals(beforeArchiveWrite / 1000, entry.getLastModifiedDate().getTime() / 1000);
                assertTrue(entry.isDirectory());
            }
        }
    }

    @Test
    public void testTarFileDirectoryRead() throws IOException {
        final File input = getFile("directory.tar");
        try (TarFile tarFile = new TarFile(input)) {
            final TarArchiveEntry directoryEntry = tarFile.getEntries().get(0);
            assertEquals("directory/", directoryEntry.getName());
            assertEquals(TarConstants.LF_DIR, directoryEntry.getLinkFlag());
            assertTrue(directoryEntry.isDirectory());
            try (InputStream directoryStream = tarFile.getInputStream(directoryEntry)) {
                final byte[] directoryRead = IOUtils.toByteArray(directoryStream);
                assertArrayEquals(ByteUtils.EMPTY_BYTE_ARRAY, directoryRead);
            }
        }
    }

    @Test
    public void testTarFileEntryFromFile() throws Exception {
        final File file = createTempFile();
        final File archive = createTempFile("test.", ".tar");
        try (TarArchiveOutputStream outputStream = new TarArchiveOutputStream(Files.newOutputStream(archive.toPath()))) {
            final TarArchiveEntry in = new TarArchiveEntry(file, "foo");
            outputStream.putArchiveEntry(in);
            outputStream.write(file);
            outputStream.closeArchiveEntry();
            outputStream.close();
            try (TarFile tarFile = new TarFile(archive)) {
                final TarArchiveEntry entry = tarFile.getEntries().get(0);
                assertNotNull(entry);
                assertEquals("foo", entry.getName());
                assertEquals(TarConstants.LF_NORMAL, entry.getLinkFlag());
                assertEquals(file.length(), entry.getSize());
                assertEquals(file.lastModified() / 1000, entry.getLastModifiedDate().getTime() / 1000);
                assertFalse(entry.isDirectory());
            }
        }
    }

    @Test
    public void testTarFileExplicitDirectoryEntry() throws Exception {
        final File archive = createTempFile("test.", ".tar");
        try (TarArchiveOutputStream tos = new TarArchiveOutputStream(Files.newOutputStream(archive.toPath()))) {
            final long beforeArchiveWrite = getTempDirFile().lastModified();
            final TarArchiveEntry in = new TarArchiveEntry("foo/");
            in.setModTime(beforeArchiveWrite);
            tos.putArchiveEntry(in);
            tos.closeArchiveEntry();
            tos.close();
            try (TarFile tarFile = new TarFile(archive)) {
                final TarArchiveEntry entry = tarFile.getEntries().get(0);
                assertNotNull(entry);
                assertEquals("foo/", entry.getName());
                assertEquals(TarConstants.LF_DIR, entry.getLinkFlag());
                assertEquals(0, entry.getSize());
                assertEquals(beforeArchiveWrite / 1000, entry.getLastModifiedDate().getTime() / 1000);
                assertTrue(entry.isDirectory());
            }
        }
    }

    @Test
    public void testTarFileExplicitFileEntry() throws Exception {
        final File file = createTempFile();
        final File archive = createTempFile("test.", ".tar");
        try (TarArchiveOutputStream outputStream = new TarArchiveOutputStream(Files.newOutputStream(archive.toPath()))) {
            final TarArchiveEntry in = new TarArchiveEntry("foo");
            in.setModTime(file.lastModified());
            in.setSize(file.length());
            outputStream.putArchiveEntry(in);
            outputStream.write(file);
            outputStream.closeArchiveEntry();
            try (TarFile tarFile = new TarFile(archive)) {
                final TarArchiveEntry entry = tarFile.getEntries().get(0);
                assertNotNull(entry);
                assertEquals("foo", entry.getName());
                assertEquals(TarConstants.LF_NORMAL, entry.getLinkFlag());
                assertEquals(file.length(), entry.getSize());
                assertEquals(file.lastModified() / 1000, entry.getLastModifiedDate().getTime() / 1000);
                assertFalse(entry.isDirectory());
            }
        }
    }

    @Test
    public void testTarFileLongNameLargerThanBuffer() throws IOException {
        final List<Integer> nameLength = Arrays.asList(300, 4096);

        for (final Integer length : nameLength) {
            final String fileName = createLongName(length);
            assertEquals(length.intValue(), fileName.length());
            final byte[] data = createTarWithOneLongNameEntry(fileName);
            try (TarFile tarFile = new TarFile(data)) {
                final List<TarArchiveEntry> entries = tarFile.getEntries();
                assertEquals(fileName, entries.get(0).getName());
                assertEquals(TarConstants.LF_NORMAL, entries.get(0).getLinkFlag());
            }
        }
    }

    @Test
    public void testTarFileUnarchive() throws Exception {
        final File file = getFile("bla.tar");
        try (TarFile tarFile = new TarFile(file)) {
            final TarArchiveEntry entry = tarFile.getEntries().get(0);
            try (InputStream inputStream = tarFile.getInputStream(entry)) {
                Files.copy(inputStream, newTempFile(entry.getName()).toPath());
            }
        }
    }

    @Test
    public void testTarUnarchive() throws Exception {
        final File input = getFile("bla.tar");
        try (InputStream is = Files.newInputStream(input.toPath());
                TarArchiveInputStream in = ArchiveStreamFactory.DEFAULT.createArchiveInputStream("tar", is)) {
            final TarArchiveEntry entry = in.getNextEntry();
            Files.copy(in, newTempFile(entry.getName()).toPath());
        }
    }
}
