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
import org.apache.commons.compress.utils.CharsetNames;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.Test;

public final class TarTest extends AbstractTest {

    private String createLongName(final int nameLength) {
        final StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < nameLength; i++) {
            buffer.append('a');
        }
        return buffer.toString();
    }

    private byte[] createTarWithOneLongNameEntry(final String longName) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (final TarArchiveOutputStream tos = new TarArchiveOutputStream(bos)) {
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
        try (final InputStream is = Files.newInputStream(input.toPath());
                final TarArchiveInputStream in = new TarArchiveInputStream(is, CharsetNames.ISO_8859_1)) {
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
        final File input = getFile("COMPRESS-178.tar");
        try (InputStream is = Files.newInputStream(input.toPath());
                ArchiveInputStream<?> in = ArchiveStreamFactory.DEFAULT.createArchiveInputStream("tar", is)) {
            final IOException e = assertThrows(IOException.class, in::getNextEntry, "Expected IOException");
            final Throwable t = e.getCause();
            assertInstanceOf(IllegalArgumentException.class, t, "Expected cause = IllegalArgumentException");
        }
    }

    @Test
    public void testCOMPRESS178Lenient() throws Exception {
        final File input = getFile("COMPRESS-178.tar");
        try (final InputStream is = Files.newInputStream(input.toPath());
                final ArchiveInputStream<?> in = new TarArchiveInputStream(is, true)) {
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
        try (final InputStream is = Files.newInputStream(input.toPath());
                final TarArchiveInputStream in = new TarArchiveInputStream(is)) {
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
        final File tmp = createTempFile();
        final File archive = createTempFile("test.", ".tar");
        try (TarArchiveOutputStream tos = new TarArchiveOutputStream(Files.newOutputStream(archive.toPath()))) {
            final TarArchiveEntry entryIn = new TarArchiveEntry("foo");
            entryIn.setModTime(tmp.lastModified());
            entryIn.setSize(tmp.length());
            tos.putArchiveEntry(entryIn);
            final byte[] b = new byte[(int) tmp.length()];
            try (InputStream fis = Files.newInputStream(tmp.toPath())) {
                while (fis.read(b) > 0) {
                    tos.write(b);
                }
            }
            tos.closeArchiveEntry();
        }
        final TarArchiveEntry entryOut;
        try (TarArchiveInputStream tis = new TarArchiveInputStream(Files.newInputStream(archive.toPath()))) {
            entryOut = tis.getNextTarEntry();
        }
        assertNotNull(entryOut);
        assertEquals("foo", entryOut.getName());
        assertEquals(TarConstants.LF_NORMAL, entryOut.getLinkFlag());
        assertEquals(tmp.length(), entryOut.getSize());
        assertEquals(tmp.lastModified() / 1000, entryOut.getLastModifiedDate().getTime() / 1000);
        assertFalse(entryOut.isDirectory());
    }

    @Test
    public void testFileEntryFromFile() throws Exception {
        final File tmp = createTempFile();
        final File archive = createTempFile("test.", ".tar");
        final TarArchiveEntry in = new TarArchiveEntry(tmp, "foo");
        try (TarArchiveOutputStream tos = new TarArchiveOutputStream(Files.newOutputStream(archive.toPath()))) {
            tos.putArchiveEntry(in);
            final byte[] b = new byte[(int) tmp.length()];
            try (InputStream fis = Files.newInputStream(tmp.toPath())) {
                while (fis.read(b) > 0) {
                    tos.write(b);
                }
            }
            tos.closeArchiveEntry();
        }
        final TarArchiveEntry out;
        try (TarArchiveInputStream tis = new TarArchiveInputStream(Files.newInputStream(archive.toPath()))) {
            out = tis.getNextTarEntry();
        }
        assertNotNull(out);
        assertEquals("foo", out.getName());
        assertEquals(TarConstants.LF_NORMAL, out.getLinkFlag());
        assertEquals(tmp.length(), out.getSize());
        assertEquals(tmp.lastModified() / 1000, out.getLastModifiedDate().getTime() / 1000);
        assertFalse(out.isDirectory());
    }

    @Test
    public void testLongNameLargerThanBuffer() throws IOException {
        final List<Integer> nameLength = Arrays.asList(300, 4096);

        for (final Integer length : nameLength) {
            final String fileName = createLongName(length);
            assertEquals(length.intValue(), fileName.length());
            final byte[] data = createTarWithOneLongNameEntry(fileName);
            try (final ByteArrayInputStream bis = new ByteArrayInputStream(data);
                    final TarArchiveInputStream tis = new TarArchiveInputStream(bis)) {
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
            Files.copy(file1.toPath(), os);
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
            Files.copy(file1.toPath(), os);
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
            Files.copy(file1.toPath(), os2);
            os2.closeArchiveEntry();
        } catch (final IOException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testTarFileCOMPRESS114() throws Exception {
        final File input = getFile("COMPRESS-114.tar");
        try (final TarFile tarFile = new TarFile(input, CharsetNames.ISO_8859_1)) {
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
        final File input = getFile("COMPRESS-178.tar");
        final IOException e = assertThrows(IOException.class, () -> {
            try (final TarFile tarFile = new TarFile(input)) {
                // Compared to the TarArchiveInputStream all entries are read when instantiating the tar file
            }
        }, "Expected IOException");
        final Throwable t = e.getCause();
        assertInstanceOf(IllegalArgumentException.class, t, "Expected cause = IllegalArgumentException");
    }

    @Test
    public void testTarFileCOMPRESS178Lenient() throws Exception {
        final File input = getFile("COMPRESS-178.tar");
        try (final TarFile tarFile = new TarFile(input, true)) {
            // Compared to the TarArchiveInputStream all entries are read when instantiating the tar file
        }
    }

    @Test
    public void testTarFileDirectoryEntryFromFile() throws Exception {
        final File archive = createTempFile("test.", ".tar");
        final File dir = getTempDirFile();
        try (final TarArchiveOutputStream tos = new TarArchiveOutputStream(Files.newOutputStream(archive.toPath()))) {
            final long beforeArchiveWrite = dir.lastModified();
            final TarArchiveEntry in = new TarArchiveEntry(dir, "foo");
            tos.putArchiveEntry(in);
            tos.closeArchiveEntry();
            tos.close();
            try (final TarFile tarFile = new TarFile(archive)) {
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
        final File tmp = createTempFile();
        final File archive = createTempFile("test.", ".tar");
        try (final TarArchiveOutputStream tos = new TarArchiveOutputStream(Files.newOutputStream(archive.toPath()))) {
            final TarArchiveEntry in = new TarArchiveEntry(tmp, "foo");
            tos.putArchiveEntry(in);
            final byte[] b = new byte[(int) tmp.length()];
            try (final InputStream fis = Files.newInputStream(tmp.toPath())) {
                while (fis.read(b) > 0) {
                    tos.write(b);
                }
            }
            tos.closeArchiveEntry();
            tos.close();
            try (final TarFile tarFile = new TarFile(archive)) {
                final TarArchiveEntry entry = tarFile.getEntries().get(0);
                assertNotNull(entry);
                assertEquals("foo", entry.getName());
                assertEquals(TarConstants.LF_NORMAL, entry.getLinkFlag());
                assertEquals(tmp.length(), entry.getSize());
                assertEquals(tmp.lastModified() / 1000, entry.getLastModifiedDate().getTime() / 1000);
                assertFalse(entry.isDirectory());
            }
        }
    }

    @Test
    public void testTarFileExplicitDirectoryEntry() throws Exception {
        final File archive = createTempFile("test.", ".tar");
        try (final TarArchiveOutputStream tos = new TarArchiveOutputStream(Files.newOutputStream(archive.toPath()))) {
            final long beforeArchiveWrite = getTempDirFile().lastModified();
            final TarArchiveEntry in = new TarArchiveEntry("foo/");
            in.setModTime(beforeArchiveWrite);
            tos.putArchiveEntry(in);
            tos.closeArchiveEntry();
            tos.close();
            try (final TarFile tarFile = new TarFile(archive)) {
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
        final File tmp = createTempFile();
        final File archive = createTempFile("test.", ".tar");
        try (final TarArchiveOutputStream tos = new TarArchiveOutputStream(Files.newOutputStream(archive.toPath()))) {
            final TarArchiveEntry in = new TarArchiveEntry("foo");
            in.setModTime(tmp.lastModified());
            in.setSize(tmp.length());
            tos.putArchiveEntry(in);
            final byte[] b = new byte[(int) tmp.length()];
            try (final InputStream fis = Files.newInputStream(tmp.toPath())) {
                while (fis.read(b) > 0) {
                    tos.write(b);
                }
            }
            tos.closeArchiveEntry();

            try (final TarFile tarFile = new TarFile(archive)) {
                final TarArchiveEntry entry = tarFile.getEntries().get(0);
                assertNotNull(entry);
                assertEquals("foo", entry.getName());
                assertEquals(TarConstants.LF_NORMAL, entry.getLinkFlag());
                assertEquals(tmp.length(), entry.getSize());
                assertEquals(tmp.lastModified() / 1000, entry.getLastModifiedDate().getTime() / 1000);
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
            try (final TarFile tarFile = new TarFile(data)) {
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
        try (final InputStream is = Files.newInputStream(input.toPath());
                final TarArchiveInputStream in = ArchiveStreamFactory.DEFAULT.createArchiveInputStream("tar", is)) {
            final TarArchiveEntry entry = in.getNextEntry();
            Files.copy(in, newTempFile(entry.getName()).toPath());
        }
    }
}
