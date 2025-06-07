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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.archivers.dump.DumpArchiveInputStream;
import org.junit.jupiter.api.Test;

public final class DumpTest extends AbstractTest {

    private void archiveDetection(final File f) throws Exception {
        try (InputStream is = Files.newInputStream(f.toPath());
                ArchiveInputStream<? extends ArchiveEntry> archiveInputStream = ArchiveStreamFactory.DEFAULT
                        .createArchiveInputStream(new BufferedInputStream(is))) {
            assertEquals(DumpArchiveInputStream.class, archiveInputStream.getClass());
        }
    }

    private void checkDumpArchive(final File f) throws Exception {
        final ArrayList<String> expected = new ArrayList<>();
        expected.add("");
        expected.add("lost+found/");
        expected.add("test1.xml");
        expected.add("test2.xml");
        try (InputStream is = Files.newInputStream(f.toPath());
                DumpArchiveInputStream inputStream = new DumpArchiveInputStream(is);) {
            checkArchiveContent(inputStream, expected);
        }
    }

    @Test
    void testArchiveDetection() throws Exception {
        archiveDetection(getFile("bla.dump"));
    }

    @Test
    void testCheckArchive() throws Exception {
        checkDumpArchive(getFile("bla.dump"));
    }

    @Test
    void testCheckCompressedArchive() throws Exception {
        checkDumpArchive(getFile("bla.z.dump"));
    }

    @Test
    void testCompressedArchiveDetection() throws Exception {
        archiveDetection(getFile("bla.z.dump"));
    }

    @Test
    void testCompressedDumpUnarchiveAll() throws Exception {
        unarchiveAll(getFile("bla.z.dump"));
    }

    @Test
    void testDumpUnarchiveAll() throws Exception {
        unarchiveAll(getFile("bla.dump"));
    }

    private void unarchiveAll(final File input) throws Exception {
        try (InputStream is = Files.newInputStream(input.toPath());
                ArchiveInputStream<?> in = ArchiveStreamFactory.DEFAULT.createArchiveInputStream("dump", is)) {
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
