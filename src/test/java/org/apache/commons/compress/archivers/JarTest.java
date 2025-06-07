/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.junit.jupiter.api.Test;

public final class JarTest extends AbstractTest {

    @Test
    void testJarArchiveCreation() throws Exception {
        final File output = newTempFile("bla.jar");

        final File file1 = getFile("test1.xml");
        final File file2 = getFile("test2.xml");

        try (OutputStream out = Files.newOutputStream(output.toPath());
                ArchiveOutputStream<ZipArchiveEntry> os = ArchiveStreamFactory.DEFAULT.createArchiveOutputStream("jar", out)) {
            // file 1
            os.putArchiveEntry(new ZipArchiveEntry("testdata/test1.xml"));
            os.write(file1);
            os.closeArchiveEntry();
            // file 2
            os.putArchiveEntry(new ZipArchiveEntry("testdata/test2.xml"));
            os.write(file2);
            os.closeArchiveEntry();
        }
    }

    @Test
    void testJarUnarchive() throws Exception {
        final File input = getFile("bla.jar");
        try (InputStream is = Files.newInputStream(input.toPath());
                ZipArchiveInputStream in = ArchiveStreamFactory.DEFAULT.createArchiveInputStream("jar", is)) {

            ZipArchiveEntry entry = in.getNextEntry();
            File o = newTempFile(entry.getName());
            o.getParentFile().mkdirs();
            Files.copy(in, o.toPath());

            entry = in.getNextEntry();
            o = newTempFile(entry.getName());
            o.getParentFile().mkdirs();
            Files.copy(in, o.toPath());

            entry = in.getNextEntry();
            o = newTempFile(entry.getName());
            o.getParentFile().mkdirs();
            Files.copy(in, o.toPath());
        }
    }

    @Test
    void testJarUnarchiveAll() throws Exception {
        final File input = getFile("bla.jar");
        try (InputStream is = Files.newInputStream(input.toPath());
                ArchiveInputStream<?> in = ArchiveStreamFactory.DEFAULT.createArchiveInputStream("jar", is)) {
            in.forEach(entry -> {
                final File archiveEntry = newTempFile(entry.getName());
                archiveEntry.getParentFile().mkdirs();
                if (entry.isDirectory()) {
                    archiveEntry.mkdir();
                } else {
                    Files.copy(in, archiveEntry.toPath());
                }
            });
        }
    }

}
