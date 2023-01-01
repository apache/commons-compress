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

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarFile;
import org.apache.commons.compress.utils.ByteUtils;
import org.apache.commons.compress.utils.CharsetNames;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.Test;

public final class TarTestCase extends AbstractTestCase {

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
             final ArchiveInputStream in = new TarArchiveInputStream(is, CharsetNames.ISO_8859_1)) {
            TarArchiveEntry entry = (TarArchiveEntry) in.getNextEntry();
            assertEquals("3\u00b1\u00b1\u00b1F06\u00b1W2345\u00b1ZB\u00b1la\u00b1\u00b1\u00b1\u00b1\u00b1\u00b1\u00b1\u00b1BLA", entry.getName());
            entry = (TarArchiveEntry) in.getNextEntry();
            assertEquals("0302-0601-3\u00b1\u00b1\u00b1F06\u00b1W2345\u00b1ZB\u00b1la\u00b1\u00b1\u00b1\u00b1\u00b1\u00b1\u00b1\u00b1BLA", entry.getName());
        }
    }

    @Test
    public void testCOMPRESS178() throws Exception {
        final File input = getFile("COMPRESS-178.tar");
        try (InputStream is = Files.newInputStream(input.toPath()); ArchiveInputStream in = ArchiveStreamFactory.DEFAULT.createArchiveInputStream("tar", is)) {
            final IOException e = assertThrows(IOException.class, in::getNextEntry, "Expected IOException");
            final Throwable t = e.getCause();
            assertInstanceOf(IllegalArgumentException.class, t, "Expected cause = IllegalArgumentException");
        }
    }

    @Test
    public void testCOMPRESS178Lenient() throws Exception {
        final File input = getFile("COMPRESS-178.tar");
        try (final InputStream is = Files.newInputStream(input.toPath());
             final ArchiveInputStream in = new TarArchiveInputStream(is, true)) {
            in.getNextEntry();
        }
    }

    @Test
    public void testDirectoryEntryFromFile() throws Exception {
        final File[] tmp = createTempDirAndFile();
        File archive = null;
        TarArchiveOutputStream tos = null;
        TarArchiveInputStream tis = null;
        try {
            archive = File.createTempFile("test.", ".tar", tmp[0]);
            archive.deleteOnExit();
            tos = new TarArchiveOutputStream(Files.newOutputStream(archive.toPath()));
            final long beforeArchiveWrite = tmp[0].lastModified();
            final TarArchiveEntry in = new TarArchiveEntry(tmp[0], "foo");
            tos.putArchiveEntry(in);
            tos.closeArchiveEntry();
            tos.close();
            tos = null;
            tis = new TarArchiveInputStream(Files.newInputStream(archive.toPath()));
            final TarArchiveEntry out = tis.getNextTarEntry();
            tis.close();
            tis = null;
            assertNotNull(out);
            assertEquals("foo/", out.getName());
            assertEquals(0, out.getSize());
            // TAR stores time with a granularity of 1 second
            assertEquals(beforeArchiveWrite / 1000,
                         out.getLastModifiedDate().getTime() / 1000);
            assertTrue(out.isDirectory());
        } finally {
            if (tis != null) {
                tis.close();
            }
            if (tos != null) {
                tos.close();
            }
            tryHardToDelete(archive);
            tryHardToDelete(tmp[1]);
            rmdir(tmp[0]);
        }
    }

    @Test
    public void testDirectoryRead() throws IOException {
        final File input = getFile("directory.tar");
        try (final InputStream is = Files.newInputStream(input.toPath());
             final TarArchiveInputStream in = new TarArchiveInputStream(is)) {
            final TarArchiveEntry directoryEntry = in.getNextTarEntry();
            assertEquals("directory/", directoryEntry.getName());
            assertTrue(directoryEntry.isDirectory());
            final byte[] directoryRead = IOUtils.toByteArray(in);
            assertArrayEquals(ByteUtils.EMPTY_BYTE_ARRAY, directoryRead);
        }
    }

    @Test
    public void testExplicitDirectoryEntry() throws Exception {
        final File[] tmp = createTempDirAndFile();
        File archive = null;
        TarArchiveOutputStream tos = null;
        TarArchiveInputStream tis = null;
        try {
            archive = File.createTempFile("test.", ".tar", tmp[0]);
            archive.deleteOnExit();
            tos = new TarArchiveOutputStream(Files.newOutputStream(archive.toPath()));
            final long beforeArchiveWrite = tmp[0].lastModified();
            final TarArchiveEntry in = new TarArchiveEntry("foo/");
            in.setModTime(beforeArchiveWrite);
            tos.putArchiveEntry(in);
            tos.closeArchiveEntry();
            tos.close();
            tos = null;
            tis = new TarArchiveInputStream(Files.newInputStream(archive.toPath()));
            final TarArchiveEntry out = tis.getNextTarEntry();
            tis.close();
            tis = null;
            assertNotNull(out);
            assertEquals("foo/", out.getName());
            assertEquals(0, out.getSize());
            assertEquals(beforeArchiveWrite / 1000,
                         out.getLastModifiedDate().getTime() / 1000);
            assertTrue(out.isDirectory());
        } finally {
            if (tis != null) {
                tis.close();
            }
            if (tos != null) {
                tos.close();
            }
            tryHardToDelete(archive);
            tryHardToDelete(tmp[1]);
            rmdir(tmp[0]);
        }
    }

    @Test
    public void testExplicitFileEntry() throws Exception {
        final File[] tmp = createTempDirAndFile();
        File archive = null;
        TarArchiveOutputStream tos = null;
        TarArchiveInputStream tis = null;
        InputStream fis = null;
        try {
            archive = File.createTempFile("test.", ".tar", tmp[0]);
            archive.deleteOnExit();
            tos = new TarArchiveOutputStream(Files.newOutputStream(archive.toPath()));
            final TarArchiveEntry in = new TarArchiveEntry("foo");
            in.setModTime(tmp[1].lastModified());
            in.setSize(tmp[1].length());
            tos.putArchiveEntry(in);
            final byte[] b = new byte[(int) tmp[1].length()];
            fis = Files.newInputStream(tmp[1].toPath());
            while (fis.read(b) > 0) {
                tos.write(b);
            }
            fis.close();
            fis = null;
            tos.closeArchiveEntry();
            tos.close();
            tos = null;
            tis = new TarArchiveInputStream(Files.newInputStream(archive.toPath()));
            final TarArchiveEntry out = tis.getNextTarEntry();
            tis.close();
            tis = null;
            assertNotNull(out);
            assertEquals("foo", out.getName());
            assertEquals(tmp[1].length(), out.getSize());
            assertEquals(tmp[1].lastModified() / 1000,
                         out.getLastModifiedDate().getTime() / 1000);
            assertFalse(out.isDirectory());
        } finally {
            if (tis != null) {
                tis.close();
            }
            if (tos != null) {
                tos.close();
            }
            tryHardToDelete(archive);
            if (fis != null) {
                fis.close();
            }
            tryHardToDelete(tmp[1]);
            rmdir(tmp[0]);
        }
    }

    @Test
    public void testFileEntryFromFile() throws Exception {
        final File[] tmp = createTempDirAndFile();
        File archive = null;
        TarArchiveOutputStream tos = null;
        TarArchiveInputStream tis = null;
        InputStream fis = null;
        try {
            archive = File.createTempFile("test.", ".tar", tmp[0]);
            archive.deleteOnExit();
            tos = new TarArchiveOutputStream(Files.newOutputStream(archive.toPath()));
            final TarArchiveEntry in = new TarArchiveEntry(tmp[1], "foo");
            tos.putArchiveEntry(in);
            final byte[] b = new byte[(int) tmp[1].length()];
            fis = Files.newInputStream(tmp[1].toPath());
            while (fis.read(b) > 0) {
                tos.write(b);
            }
            fis.close();
            fis = null;
            tos.closeArchiveEntry();
            tos.close();
            tos = null;
            tis = new TarArchiveInputStream(Files.newInputStream(archive.toPath()));
            final TarArchiveEntry out = tis.getNextTarEntry();
            tis.close();
            tis = null;
            assertNotNull(out);
            assertEquals("foo", out.getName());
            assertEquals(tmp[1].length(), out.getSize());
            assertEquals(tmp[1].lastModified() / 1000,
                         out.getLastModifiedDate().getTime() / 1000);
            assertFalse(out.isDirectory());
        } finally {
            if (tis != null) {
                tis.close();
            }
            if (tos != null) {
                tos.close();
            }
            tryHardToDelete(archive);
            if (fis != null) {
                fis.close();
            }
            tryHardToDelete(tmp[1]);
            rmdir(tmp[0]);
        }
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
        final File output = new File(dir, "bla.tar");
        final File file1 = getFile("test1.xml");
        try (OutputStream out = Files.newOutputStream(output.toPath());
                ArchiveOutputStream os = ArchiveStreamFactory.DEFAULT.createArchiveOutputStream("tar", out)) {
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

        final File output = new File(dir, "bla.tar");
        final File file1 = getFile("test1.xml");
        final OutputStream out = Files.newOutputStream(output.toPath());
        final ArchiveOutputStream os = ArchiveStreamFactory.DEFAULT.createArchiveOutputStream("tar", out);
        final TarArchiveEntry entry = new TarArchiveEntry(name);
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
        os.close();
        out.close();

        ArchiveOutputStream os2 = null;
        try {
            final String toLongName = "testdata/123456789012345678901234567890123456789012345678901234567890123456789012345678901234567.xml";
            final File output2 = new File(dir, "bla.tar");
            final OutputStream out2 = Files.newOutputStream(output2.toPath());
            os2 = ArchiveStreamFactory.DEFAULT.createArchiveOutputStream("tar", out2);
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
        } catch(final IOException e) {
            assertTrue(true);
        } finally {
            if (os2 != null){
                os2.close();
            }
        }
    }

    @Test
    public void testTarFileCOMPRESS114() throws Exception {
        final File input = getFile("COMPRESS-114.tar");
        try (final TarFile tarFile = new TarFile(input, CharsetNames.ISO_8859_1)) {
            final List<TarArchiveEntry> entries = tarFile.getEntries();
            TarArchiveEntry entry = entries.get(0);
            assertEquals("3\u00b1\u00b1\u00b1F06\u00b1W2345\u00b1ZB\u00b1la\u00b1\u00b1\u00b1\u00b1\u00b1\u00b1\u00b1\u00b1BLA", entry.getName());
            entry = entries.get(1);
            assertEquals("0302-0601-3\u00b1\u00b1\u00b1F06\u00b1W2345\u00b1ZB\u00b1la\u00b1\u00b1\u00b1\u00b1\u00b1\u00b1\u00b1\u00b1BLA", entry.getName());
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
        final File[] tmp = createTempDirAndFile();
        final File archive = File.createTempFile("test.", ".tar", tmp[0]);
        archive.deleteOnExit();

        try (final TarArchiveOutputStream tos = new TarArchiveOutputStream(Files.newOutputStream(archive.toPath()))) {
            final long beforeArchiveWrite = tmp[0].lastModified();
            final TarArchiveEntry in = new TarArchiveEntry(tmp[0], "foo");
            tos.putArchiveEntry(in);
            tos.closeArchiveEntry();
            tos.close();
            try (final TarFile tarFile = new TarFile(archive)) {
                final TarArchiveEntry entry = tarFile.getEntries().get(0);
                assertNotNull(entry);
                assertEquals("foo/", entry.getName());
                assertEquals(0, entry.getSize());
                // TAR stores time with a granularity of 1 second
                assertEquals(beforeArchiveWrite / 1000, entry.getLastModifiedDate().getTime() / 1000);
                assertTrue(entry.isDirectory());
            } finally {
                tryHardToDelete(archive);
                tryHardToDelete(tmp[1]);
                rmdir(tmp[0]);
            }
        }
    }

    @Test
    public void testTarFileDirectoryRead() throws IOException {
        final File input = getFile("directory.tar");
        try (TarFile tarFile = new TarFile(input)) {
            final TarArchiveEntry directoryEntry = tarFile.getEntries().get(0);
            assertEquals("directory/", directoryEntry.getName());
            assertTrue(directoryEntry.isDirectory());
            try (InputStream directoryStream = tarFile.getInputStream(directoryEntry)) {
                final byte[] directoryRead = IOUtils.toByteArray(directoryStream);
                assertArrayEquals(ByteUtils.EMPTY_BYTE_ARRAY, directoryRead);
            }
        }
    }

    @Test
    public void testTarFileEntryFromFile() throws Exception {
        final File[] tmp = createTempDirAndFile();
        final File archive = File.createTempFile("test.", ".tar", tmp[0]);
        archive.deleteOnExit();
        try (final TarArchiveOutputStream tos = new TarArchiveOutputStream(Files.newOutputStream(archive.toPath()))) {
            final TarArchiveEntry in = new TarArchiveEntry(tmp[1], "foo");
            tos.putArchiveEntry(in);
            final byte[] b = new byte[(int) tmp[1].length()];
            try (final InputStream fis = Files.newInputStream(tmp[1].toPath())) {
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
                assertEquals(tmp[1].length(), entry.getSize());
                assertEquals(tmp[1].lastModified() / 1000, entry.getLastModifiedDate().getTime() / 1000);
                assertFalse(entry.isDirectory());
            }
        } finally {
            tryHardToDelete(archive);
            tryHardToDelete(tmp[1]);
            rmdir(tmp[0]);
        }
    }

    @Test
    public void testTarFileExplicitDirectoryEntry() throws Exception {
        final File[] tmp = createTempDirAndFile();
        final File archive = File.createTempFile("test.", ".tar", tmp[0]);
        archive.deleteOnExit();
        try (final TarArchiveOutputStream tos = new TarArchiveOutputStream(Files.newOutputStream(archive.toPath()))){
            final long beforeArchiveWrite = tmp[0].lastModified();
            final TarArchiveEntry in = new TarArchiveEntry("foo/");
            in.setModTime(beforeArchiveWrite);
            tos.putArchiveEntry(in);
            tos.closeArchiveEntry();
            tos.close();
            try (final TarFile tarFile = new TarFile(archive)) {
                final TarArchiveEntry entry = tarFile.getEntries().get(0);
                assertNotNull(entry);
                assertEquals("foo/", entry.getName());
                assertEquals(0, entry.getSize());
                assertEquals(beforeArchiveWrite / 1000, entry.getLastModifiedDate().getTime() / 1000);
                assertTrue(entry.isDirectory());
            }
        } finally {
            tryHardToDelete(archive);
            tryHardToDelete(tmp[1]);
            rmdir(tmp[0]);
        }
    }

    @Test
    public void testTarFileExplicitFileEntry() throws Exception {
        final File[] tmp = createTempDirAndFile();
        final File archive = File.createTempFile("test.", ".tar", tmp[0]);
        archive.deleteOnExit();
        try (final TarArchiveOutputStream tos  = new TarArchiveOutputStream(Files.newOutputStream(archive.toPath()))){
            final TarArchiveEntry in = new TarArchiveEntry("foo");
            in.setModTime(tmp[1].lastModified());
            in.setSize(tmp[1].length());
            tos.putArchiveEntry(in);
            final byte[] b = new byte[(int) tmp[1].length()];
            try (final InputStream fis = Files.newInputStream(tmp[1].toPath())) {
                while (fis.read(b) > 0) {
                    tos.write(b);
                }
            }
            tos.closeArchiveEntry();

            try (final TarFile tarFile = new TarFile(archive)) {
                final TarArchiveEntry entry = tarFile.getEntries().get(0);
                assertNotNull(entry);
                assertEquals("foo", entry.getName());
                assertEquals(tmp[1].length(), entry.getSize());
                assertEquals(tmp[1].lastModified() / 1000, entry.getLastModifiedDate().getTime() / 1000);
                assertFalse(entry.isDirectory());
            }
        } finally {
            tryHardToDelete(archive);
            tryHardToDelete(tmp[1]);
            rmdir(tmp[0]);
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
            }
        }
    }

    @Test
    public void testTarFileUnarchive() throws Exception {
        final File file = getFile("bla.tar");
        try (final TarFile tarFile = new TarFile(file)) {
            final TarArchiveEntry entry = tarFile.getEntries().get(0);
            Files.copy(tarFile.getInputStream(entry), new File(dir, entry.getName()).toPath());
        }
    }

    @Test
    public void testTarUnarchive() throws Exception {
        final File input = getFile("bla.tar");
        try (final InputStream is = Files.newInputStream(input.toPath());
                final ArchiveInputStream in = ArchiveStreamFactory.DEFAULT.createArchiveInputStream("tar", is)) {
            final TarArchiveEntry entry = (TarArchiveEntry) in.getNextEntry();
            Files.copy(in, new File(dir, entry.getName()).toPath());
        }
    }
}
