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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.nio.charset.StandardCharsets;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarFile;
import org.apache.commons.compress.utils.CharsetNames;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public final class TarTestCase extends AbstractTestCase {

    @Test
    public void testTarArchiveCreation() throws Exception {
        final File output = new File(dir, "bla.tar");
        final File file1 = getFile("test1.xml");
        final OutputStream out = new FileOutputStream(output);
        final ArchiveOutputStream os = ArchiveStreamFactory.DEFAULT.createArchiveOutputStream("tar", out);
        final TarArchiveEntry entry = new TarArchiveEntry("testdata/test1.xml");
        entry.setModTime(0);
        entry.setSize(file1.length());
        entry.setUserId(0);
        entry.setGroupId(0);
        entry.setUserName("avalon");
        entry.setGroupName("excalibur");
        entry.setMode(0100000);
        os.putArchiveEntry(entry);
        IOUtils.copy(new FileInputStream(file1), os);
        os.closeArchiveEntry();
        os.close();
    }

    @Test
    public void testTarArchiveLongNameCreation() throws Exception {
        final String name = "testdata/12345678901234567890123456789012345678901234567890123456789012345678901234567890123456.xml";
        final byte[] bytes = name.getBytes(StandardCharsets.UTF_8);
        assertEquals(bytes.length, 99);

        final File output = new File(dir, "bla.tar");
        final File file1 = getFile("test1.xml");
        final OutputStream out = new FileOutputStream(output);
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
        final FileInputStream in = new FileInputStream(file1);
        IOUtils.copy(in, os);
        os.closeArchiveEntry();
        os.close();
        out.close();
        in.close();


        ArchiveOutputStream os2 = null;
        try {
            final String toLongName = "testdata/123456789012345678901234567890123456789012345678901234567890123456789012345678901234567.xml";
            final File output2 = new File(dir, "bla.tar");
            final OutputStream out2 = new FileOutputStream(output2);
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
            IOUtils.copy(new FileInputStream(file1), os2);
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
    public void testTarUnarchive() throws Exception {
        final File input = getFile("bla.tar");
        try (final InputStream is = new FileInputStream(input);
             final ArchiveInputStream in = ArchiveStreamFactory.DEFAULT.createArchiveInputStream("tar", is)) {
            final TarArchiveEntry entry = (TarArchiveEntry) in.getNextEntry();
            try (final OutputStream out = new FileOutputStream(new File(dir, entry.getName()))) {
                IOUtils.copy(in, out);
            }
        }
    }

    @Test
    public void testTarFileUnarchive() throws Exception {
        final File file = getFile("bla.tar");
        try (final TarFile tarFile = new TarFile(file)) {
            final TarArchiveEntry entry = tarFile.getEntries().get(0);
            try (final OutputStream out = new FileOutputStream(new File(dir, entry.getName()))) {
                IOUtils.copy(tarFile.getInputStream(entry), out);
            }
        }
    }

    @Test
    public void testCOMPRESS114() throws Exception {
        final File input = getFile("COMPRESS-114.tar");
        try (final InputStream is = new FileInputStream(input);
             final ArchiveInputStream in = new TarArchiveInputStream(is, CharsetNames.ISO_8859_1)) {
            TarArchiveEntry entry = (TarArchiveEntry) in.getNextEntry();
            assertEquals("3\u00b1\u00b1\u00b1F06\u00b1W2345\u00b1ZB\u00b1la\u00b1\u00b1\u00b1\u00b1\u00b1\u00b1\u00b1\u00b1BLA", entry.getName());
            entry = (TarArchiveEntry) in.getNextEntry();
            assertEquals("0302-0601-3\u00b1\u00b1\u00b1F06\u00b1W2345\u00b1ZB\u00b1la\u00b1\u00b1\u00b1\u00b1\u00b1\u00b1\u00b1\u00b1BLA", entry.getName());
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
    public void testDirectoryEntryFromFile() throws Exception {
        final File[] tmp = createTempDirAndFile();
        File archive = null;
        TarArchiveOutputStream tos = null;
        TarArchiveInputStream tis = null;
        try {
            archive = File.createTempFile("test.", ".tar", tmp[0]);
            archive.deleteOnExit();
            tos = new TarArchiveOutputStream(new FileOutputStream(archive));
            final long beforeArchiveWrite = tmp[0].lastModified();
            final TarArchiveEntry in = new TarArchiveEntry(tmp[0], "foo");
            tos.putArchiveEntry(in);
            tos.closeArchiveEntry();
            tos.close();
            tos = null;
            tis = new TarArchiveInputStream(new FileInputStream(archive));
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
    public void testTarFileDirectoryEntryFromFile() throws Exception {
        final File[] tmp = createTempDirAndFile();
        File archive = File.createTempFile("test.", ".tar", tmp[0]);
        archive.deleteOnExit();

        try (final TarArchiveOutputStream tos = new TarArchiveOutputStream(new FileOutputStream(archive))) {
            final long beforeArchiveWrite = tmp[0].lastModified();
            final TarArchiveEntry in = new TarArchiveEntry(tmp[0], "foo");
            tos.putArchiveEntry(in);
            tos.closeArchiveEntry();
            tos.close();
            try (final TarFile tarFile = new TarFile(archive)) {
                TarArchiveEntry entry = tarFile.getEntries().get(0);
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
    public void testExplicitDirectoryEntry() throws Exception {
        final File[] tmp = createTempDirAndFile();
        File archive = null;
        TarArchiveOutputStream tos = null;
        TarArchiveInputStream tis = null;
        try {
            archive = File.createTempFile("test.", ".tar", tmp[0]);
            archive.deleteOnExit();
            tos = new TarArchiveOutputStream(new FileOutputStream(archive));
            final long beforeArchiveWrite = tmp[0].lastModified();
            final TarArchiveEntry in = new TarArchiveEntry("foo/");
            in.setModTime(beforeArchiveWrite);
            tos.putArchiveEntry(in);
            tos.closeArchiveEntry();
            tos.close();
            tos = null;
            tis = new TarArchiveInputStream(new FileInputStream(archive));
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
    public void testTarFileExplicitDirectoryEntry() throws Exception {
        final File[] tmp = createTempDirAndFile();
        File archive = File.createTempFile("test.", ".tar", tmp[0]);
        archive.deleteOnExit();
        try (final TarArchiveOutputStream tos = new TarArchiveOutputStream(new FileOutputStream(archive))){
            final long beforeArchiveWrite = tmp[0].lastModified();
            final TarArchiveEntry in = new TarArchiveEntry("foo/");
            in.setModTime(beforeArchiveWrite);
            tos.putArchiveEntry(in);
            tos.closeArchiveEntry();
            tos.close();
            try (final TarFile tarFile = new TarFile(archive)) {
                TarArchiveEntry entry = tarFile.getEntries().get(0);
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
    public void testFileEntryFromFile() throws Exception {
        final File[] tmp = createTempDirAndFile();
        File archive = null;
        TarArchiveOutputStream tos = null;
        TarArchiveInputStream tis = null;
        FileInputStream fis = null;
        try {
            archive = File.createTempFile("test.", ".tar", tmp[0]);
            archive.deleteOnExit();
            tos = new TarArchiveOutputStream(new FileOutputStream(archive));
            final TarArchiveEntry in = new TarArchiveEntry(tmp[1], "foo");
            tos.putArchiveEntry(in);
            final byte[] b = new byte[(int) tmp[1].length()];
            fis = new FileInputStream(tmp[1]);
            while (fis.read(b) > 0) {
                tos.write(b);
            }
            fis.close();
            fis = null;
            tos.closeArchiveEntry();
            tos.close();
            tos = null;
            tis = new TarArchiveInputStream(new FileInputStream(archive));
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
    public void testTarFileEntryFromFile() throws Exception {
        final File[] tmp = createTempDirAndFile();
        File archive = File.createTempFile("test.", ".tar", tmp[0]);
        archive.deleteOnExit();
        try (final TarArchiveOutputStream tos = new TarArchiveOutputStream(new FileOutputStream(archive))) {
            final TarArchiveEntry in = new TarArchiveEntry(tmp[1], "foo");
            tos.putArchiveEntry(in);
            final byte[] b = new byte[(int) tmp[1].length()];
            try (final FileInputStream fis = new FileInputStream(tmp[1])) {
                while (fis.read(b) > 0) {
                    tos.write(b);
                }
            }
            tos.closeArchiveEntry();
            tos.close();
            try (final TarFile tarFile = new TarFile(archive)) {
                TarArchiveEntry entry = tarFile.getEntries().get(0);
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
    public void testExplicitFileEntry() throws Exception {
        final File[] tmp = createTempDirAndFile();
        File archive = null;
        TarArchiveOutputStream tos = null;
        TarArchiveInputStream tis = null;
        FileInputStream fis = null;
        try {
            archive = File.createTempFile("test.", ".tar", tmp[0]);
            archive.deleteOnExit();
            tos = new TarArchiveOutputStream(new FileOutputStream(archive));
            final TarArchiveEntry in = new TarArchiveEntry("foo");
            in.setModTime(tmp[1].lastModified());
            in.setSize(tmp[1].length());
            tos.putArchiveEntry(in);
            final byte[] b = new byte[(int) tmp[1].length()];
            fis = new FileInputStream(tmp[1]);
            while (fis.read(b) > 0) {
                tos.write(b);
            }
            fis.close();
            fis = null;
            tos.closeArchiveEntry();
            tos.close();
            tos = null;
            tis = new TarArchiveInputStream(new FileInputStream(archive));
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
    public void testTarFileExplicitFileEntry() throws Exception {
        final File[] tmp = createTempDirAndFile();
        File archive = File.createTempFile("test.", ".tar", tmp[0]);
        archive.deleteOnExit();
        try (final TarArchiveOutputStream tos  = new TarArchiveOutputStream(new FileOutputStream(archive))){
            final TarArchiveEntry in = new TarArchiveEntry("foo");
            in.setModTime(tmp[1].lastModified());
            in.setSize(tmp[1].length());
            tos.putArchiveEntry(in);
            final byte[] b = new byte[(int) tmp[1].length()];
            try (final FileInputStream fis = new FileInputStream(tmp[1])) {
                while (fis.read(b) > 0) {
                    tos.write(b);
                }
            }
            tos.closeArchiveEntry();

            try (final TarFile tarFile = new TarFile(archive)) {
                TarArchiveEntry entry = tarFile.getEntries().get(0);
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
    public void testCOMPRESS178() throws Exception {
        final File input = getFile("COMPRESS-178.tar");
        final InputStream is = new FileInputStream(input);
        final ArchiveInputStream in = ArchiveStreamFactory.DEFAULT.createArchiveInputStream("tar", is);
        try {
            in.getNextEntry();
            fail("Expected IOException");
        } catch (final IOException e) {
            final Throwable t = e.getCause();
            assertTrue("Expected cause = IllegalArgumentException", t instanceof IllegalArgumentException);
        }
        in.close();
    }

    @Test
    public void testTarFileCOMPRESS178() throws Exception {
        final File input = getFile("COMPRESS-178.tar");
        try (final TarFile tarFile = new TarFile(input)) {
            // Compared to the TarArchiveInputStream all entries are read when instantiating the tar file
            fail("Expected IOException");
        } catch (final IOException e) {
            final Throwable t = e.getCause();
            assertTrue("Expected cause = IllegalArgumentException", t instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testCOMPRESS178Lenient() throws Exception {
        final File input = getFile("COMPRESS-178.tar");
        try (final InputStream is = new FileInputStream(input);
             final ArchiveInputStream in = new TarArchiveInputStream(is, true)) {
            in.getNextEntry();
        }
    }

    @Test
    public void testTarFileCOMPRESS178Lenient() throws Exception {
        final File input = getFile("COMPRESS-178.tar");
        try (final TarFile tarFile = new TarFile(input, true)) {
            // Compared to the TarArchiveInputStream all entries are read when instantiating the tar file
        }
    }

    @Test
    public void testDirectoryRead() throws IOException {
        final File input = getFile("directory.tar");
        try (final InputStream is = new FileInputStream(input);
             final TarArchiveInputStream in = new TarArchiveInputStream(is)) {
            TarArchiveEntry directoryEntry = in.getNextTarEntry();
            assertEquals("directory/", directoryEntry.getName());
            assertTrue(directoryEntry.isDirectory());
            byte[] directoryRead = IOUtils.toByteArray(in);
            assertArrayEquals(new byte[0], directoryRead);
        }
    }

    @Test
    public void testTarFileDirectoryRead() throws IOException {
        final File input = getFile("directory.tar");
        try (TarFile tarFile = new TarFile(input)) {
            TarArchiveEntry directoryEntry = tarFile.getEntries().get(0);
            assertEquals("directory/", directoryEntry.getName());
            assertTrue(directoryEntry.isDirectory());
            try (InputStream directoryStream = tarFile.getInputStream(directoryEntry)) {
                byte[] directoryRead = IOUtils.toByteArray(directoryStream);
                assertArrayEquals(new byte[0], directoryRead);
            }
        }
    }
}
