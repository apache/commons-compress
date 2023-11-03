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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream;
import org.apache.commons.compress.archivers.ar.ArArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public final class ArTestCase extends AbstractTestCase {

    @Test
    public void testArArchiveCreation() throws Exception {
        final File output = new File(getTempDirFile(), "bla.ar");

        final File file1 = getFile("test1.xml");
        final File file2 = getFile("test2.xml");

        try (final OutputStream out = Files.newOutputStream(output.toPath());
                ArArchiveOutputStream os = ArchiveStreamFactory.DEFAULT.createArchiveOutputStream("ar", out)) {
            os.putArchiveEntry(new ArArchiveEntry("test1.xml", file1.length()));
            Files.copy(file1.toPath(), os);
            os.closeArchiveEntry();

            os.putArchiveEntry(new ArArchiveEntry("test2.xml", file2.length()));
            Files.copy(file2.toPath(), os);
            os.closeArchiveEntry();
        }
    }

    @Test
    public void testArDelete() throws Exception {
        final File output = new File(getTempDirFile(), "bla.ar");

        final File file1 = getFile("test1.xml");
        final File file2 = getFile("test2.xml");
        {
            // create
            try (OutputStream out = Files.newOutputStream(output.toPath());
                    ArchiveOutputStream<ArArchiveEntry> os = ArchiveStreamFactory.DEFAULT.createArchiveOutputStream("ar", out)) {
                os.putArchiveEntry(new ArArchiveEntry("test1.xml", file1.length()));
                Files.copy(file1.toPath(), os);
                os.closeArchiveEntry();

                os.putArchiveEntry(new ArArchiveEntry("test2.xml", file2.length()));
                Files.copy(file2.toPath(), os);
                os.closeArchiveEntry();
            }
        }

        assertEquals(8 + 60 + file1.length() + file1.length() % 2 + 60 + file2.length() + file2.length() % 2, output.length());

        final File output2 = new File(getTempDirFile(), "bla2.ar");

        int copied = 0;
        int deleted = 0;

        // remove all but one file

        try (OutputStream os = Files.newOutputStream(output2.toPath());
                InputStream is = Files.newInputStream(output.toPath());
                ArchiveOutputStream<ArArchiveEntry> aos = new ArchiveStreamFactory().createArchiveOutputStream("ar", os);
                ArchiveInputStream<ArArchiveEntry> ais = new ArchiveStreamFactory().createArchiveInputStream(new BufferedInputStream(is))) {
            while (true) {
                final ArArchiveEntry entry = ais.getNextEntry();
                if (entry == null) {
                    break;
                }

                if ("test1.xml".equals(entry.getName())) {
                    aos.putArchiveEntry(entry);
                    IOUtils.copy(ais, aos);
                    aos.closeArchiveEntry();
                    copied++;
                } else {
                    IOUtils.copy(ais, new ByteArrayOutputStream());
                    deleted++;
                }
            }
        }

        assertEquals(1, copied);
        assertEquals(1, deleted);
        assertEquals(8 + 60 + file1.length() + file1.length() % 2, output2.length());

        long files = 0;
        long sum = 0;

        {
            try (InputStream is = Files.newInputStream(output2.toPath());
                    ArchiveInputStream<ArArchiveEntry> ais = new ArchiveStreamFactory().createArchiveInputStream(new BufferedInputStream(is))) {
                while (true) {
                    final ArArchiveEntry entry = ais.getNextEntry();
                    if (entry == null) {
                        break;
                    }

                    IOUtils.copy(ais, new ByteArrayOutputStream());

                    sum += entry.getLength();
                    files++;
                }
            }
        }

        assertEquals(1, files);
        assertEquals(file1.length(), sum);
    }

    @Test
    public void testArUnarchive() throws Exception {
        final File output = new File(getTempDirFile(), "bla.ar");
        {
            final File file1 = getFile("test1.xml");
            final File file2 = getFile("test2.xml");

            try (OutputStream out = Files.newOutputStream(output.toPath());
                    ArchiveOutputStream<ArArchiveEntry> os = ArchiveStreamFactory.DEFAULT.createArchiveOutputStream("ar", out)) {
                os.putArchiveEntry(new ArArchiveEntry("test1.xml", file1.length()));
                Files.copy(file1.toPath(), os);
                os.closeArchiveEntry();

                os.putArchiveEntry(new ArArchiveEntry("test2.xml", file2.length()));
                Files.copy(file2.toPath(), os);
                os.closeArchiveEntry();
            }
        }

        // UnArArchive Operation
        try (final InputStream is = Files.newInputStream(output.toPath());
                final ArchiveInputStream<ArArchiveEntry> in = new ArchiveStreamFactory().createArchiveInputStream(new BufferedInputStream(is))) {
            final ArArchiveEntry entry = in.getNextEntry();

            final File target = new File(getTempDirFile(), entry.getName());
            Files.copy(in, target.toPath());
        }
    }

    @Test
    public void testExplicitFileEntry() throws Exception {
        final File file = createTempFile();
        final File archive = createTempFile("test.", ".ar");
        try (ArArchiveOutputStream aos = new ArArchiveOutputStream(Files.newOutputStream(archive.toPath()))) {
            final ArArchiveEntry in = new ArArchiveEntry("foo", file.length(), 0, 0, 0, file.lastModified() / 1000);
            aos.putArchiveEntry(in);
            final byte[] b = new byte[(int) file.length()];
            try (InputStream fis = Files.newInputStream(file.toPath())) {
                while (fis.read(b) > 0) {
                    aos.write(b);
                }
            }
            aos.closeArchiveEntry();
        }
        //
        final ArArchiveEntry out;
        try (ArArchiveInputStream ais = new ArArchiveInputStream(Files.newInputStream(archive.toPath()))) {
            out = ais.getNextArEntry();
        }
        assertNotNull(out);
        assertEquals("foo", out.getName());
        assertEquals(file.length(), out.getSize());
        assertEquals(file.lastModified() / 1000, out.getLastModifiedDate().getTime() / 1000);
        assertFalse(out.isDirectory());
    }

    @Test
    public void testFileEntryFromFile() throws Exception {
        final File file = createTempFile();
        final File archive = createTempFile("test.", ".ar");
        try (ArArchiveOutputStream aos = new ArArchiveOutputStream(Files.newOutputStream(archive.toPath()))) {
            final ArArchiveEntry in = new ArArchiveEntry(file, "foo");
            aos.putArchiveEntry(in);
            final byte[] b = new byte[(int) file.length()];
            try (InputStream fis = Files.newInputStream(file.toPath())) {
                while (fis.read(b) > 0) {
                    aos.write(b);
                }
            }
            aos.closeArchiveEntry();
        }
        final ArArchiveEntry out;
        try (ArArchiveInputStream ais = new ArArchiveInputStream(Files.newInputStream(archive.toPath()))) {
            out = ais.getNextArEntry();
        }
        assertNotNull(out);
        assertEquals("foo", out.getName());
        assertEquals(file.length(), out.getSize());
        // AR stores time with a granularity of 1 second
        assertEquals(file.lastModified() / 1000, out.getLastModifiedDate().getTime() / 1000);
        assertFalse(out.isDirectory());
    }

    @Test
    public void testFileEntryFromPath() throws Exception {
        final File file = createTempFile();
        final File archive = createTempFile("test.", ".ar");
        try (ArArchiveOutputStream aos = new ArArchiveOutputStream(Files.newOutputStream(archive.toPath()))) {
            final ArArchiveEntry in = new ArArchiveEntry(file.toPath(), "foo");
            aos.putArchiveEntry(in);
            final byte[] b = new byte[(int) file.length()];
            try (InputStream fis = Files.newInputStream(file.toPath())) {
                while (fis.read(b) > 0) {
                    aos.write(b);
                }
            }
            aos.closeArchiveEntry();
        }
        final ArArchiveEntry out;
        try (ArArchiveInputStream ais = new ArArchiveInputStream(Files.newInputStream(archive.toPath()))) {
            out = ais.getNextArEntry();
        }
        assertNotNull(out);
        assertEquals("foo", out.getName());
        assertEquals(file.length(), out.getSize());
        // AR stores time with a granularity of 1 second
        assertEquals(file.lastModified() / 1000, out.getLastModifiedDate().getTime() / 1000);
        assertFalse(out.isDirectory());
    }

    // TODO: revisit - does AR not support storing directories?
    @Disabled
    @Test
    public void testXtestDirectoryEntryFromFile() throws Exception {
        final File tmp = createTempFile();
        final File archive = createTempFile("test.", ".ar");
        final long beforeArchiveWrite;
        try (ArArchiveOutputStream aos = new ArArchiveOutputStream(Files.newOutputStream(archive.toPath()))) {
            beforeArchiveWrite = tmp.lastModified();
            final ArArchiveEntry in = new ArArchiveEntry(tmp, "foo");
            aos.putArchiveEntry(in);
            aos.closeArchiveEntry();
        }
        final ArArchiveEntry out;
        try (ArArchiveInputStream ais = new ArArchiveInputStream(Files.newInputStream(archive.toPath()))) {
            out = ais.getNextArEntry();
        }
        assertNotNull(out);
        assertEquals("foo/", out.getName());
        assertEquals(0, out.getSize());
        // AR stores time with a granularity of 1 second
        assertEquals(beforeArchiveWrite / 1000, out.getLastModifiedDate().getTime() / 1000);
        assertTrue(out.isDirectory());
    }

    // TODO: revisit - does AR not support storing directories?
    @Disabled
    @Test
    public void testXtestExplicitDirectoryEntry() throws Exception {
        final File tmp = createTempFile();
        final File archive = createTempFile("test.", ".ar");
        final long beforeArchiveWrite;
        try (ArArchiveOutputStream aos = new ArArchiveOutputStream(Files.newOutputStream(archive.toPath()))) {
            beforeArchiveWrite = getTempDirFile().lastModified();
            final ArArchiveEntry in = new ArArchiveEntry("foo", 0, 0, 0, 0, tmp.lastModified() / 1000);
            aos.putArchiveEntry(in);
            aos.closeArchiveEntry();
        }
        final ArArchiveEntry out;
        try (ArArchiveInputStream ais = new ArArchiveInputStream(Files.newInputStream(archive.toPath()))) {
            out = ais.getNextArEntry();
        }
        assertNotNull(out);
        assertEquals("foo/", out.getName());
        assertEquals(0, out.getSize());
        assertEquals(beforeArchiveWrite / 1000, out.getLastModifiedDate().getTime() / 1000);
        assertTrue(out.isDirectory());
    }
}
