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
package org.apache.commons.compress.compressors.pack200;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.commons.io.output.UnsynchronizedByteArrayOutputStream;
import org.junit.jupiter.api.Test;

public final class Pack200UtilsTest extends AbstractTest {

    @Test
    public void testNormalize() throws Throwable {
        final File input = getFile("bla.jar");
        final File output = createTempFile();
        Pack200Utils.normalize(input, output, new HashMap<>());
        try (InputStream is = Files.newInputStream(output.toPath());
                ArchiveInputStream<?> in = ArchiveStreamFactory.DEFAULT.createArchiveInputStream("jar", is)) {
            ArchiveEntry entry = in.getNextEntry();
            while (entry != null) {
                final File archiveEntry = newTempFile(entry.getName());
                archiveEntry.getParentFile().mkdirs();
                if (entry.isDirectory()) {
                    archiveEntry.mkdir();
                    entry = in.getNextEntry();
                    continue;
                }
                Files.copy(in, archiveEntry.toPath());
                entry = in.getNextEntry();
            }
        }
    }

    @Test
    public void testNormalizeInPlace() throws Throwable {
        final File input = getFile("bla.jar");
        final File output = createTempFile();
        try (InputStream is = Files.newInputStream(input.toPath())) {
            Files.copy(is, output.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        Pack200Utils.normalize(output);
        try (InputStream is = Files.newInputStream(output.toPath());
                ArchiveInputStream<?> in = ArchiveStreamFactory.DEFAULT.createArchiveInputStream("jar", is)) {
            ArchiveEntry entry = in.getNextEntry();
            while (entry != null) {
                final File archiveEntry = newTempFile(entry.getName());
                archiveEntry.getParentFile().mkdirs();
                if (entry.isDirectory()) {
                    archiveEntry.mkdir();
                    entry = in.getNextEntry();
                    continue;
                }
                Files.copy(in, archiveEntry.toPath());
                entry = in.getNextEntry();
            }
        }
    }

    @Test
    public void testCloseShield() throws Exception {
        //COMPRESS-675
        //put 2 pack files inside an archive and then try to unpack them.

        final File pack = getFile("bla.pack");
        final File archiveFile = createTempFile();

        UnsynchronizedByteArrayOutputStream bos = new UnsynchronizedByteArrayOutputStream();
        IOUtils.copy(new Pack200CompressorInputStream(new FileInputStream(pack)), bos);
        long expectedBytes = bos.size() * 2;

        try (OutputStream os = new FileOutputStream(archiveFile);
             ArchiveOutputStream taos = new TarArchiveOutputStream(os)) {

            ArchiveEntry ae = taos.createArchiveEntry(pack, "./bla.pack");
            taos.putArchiveEntry(ae);
            try (FileInputStream in = new FileInputStream(pack)) {
                IOUtils.copy(in, taos);
            }
            taos.closeArchiveEntry();
            ArchiveEntry ae2 = taos.createArchiveEntry(pack, "./bla2.pack");
            taos.putArchiveEntry(ae2);
            try (FileInputStream in = new FileInputStream(pack)) {
                IOUtils.copy(in, taos);
            }
            taos.closeArchiveEntry();
            taos.finish();
            taos.flush();
        }

        //The underlying ChannelInputStream is what causes the problem
        //FileInputStream doesn't show the bug

        //If you use a zip archive instead of a tar archive you
        //get a different number of bytes read, but still not the expected
        try (InputStream is = new FileInputStream(archiveFile);
             //Files.newInputStream(archiveFile.toPath());
             ArchiveInputStream in = new TarArchiveInputStream(is)) {
            ArchiveEntry entry = in.getNextEntry();
            int entries = 0;
            long bytes = 0;
            while (entry != null) {
                if (in.canReadEntryData(entry)) {
                    bytes += parseEntry(CloseShieldInputStream.wrap(in));
                    entries++;
                }
                entry = in.getNextEntry();
            }
            assertEquals(2, entries);
            assertEquals(expectedBytes, bytes);
        }
    }

    private long parseEntry(InputStream is) throws IOException {
        UnsynchronizedByteArrayOutputStream bos = new UnsynchronizedByteArrayOutputStream();
        try (Pack200CompressorInputStream p = new Pack200CompressorInputStream(is)) {
            return IOUtils.copy(p, bos);
        }
    }

}
