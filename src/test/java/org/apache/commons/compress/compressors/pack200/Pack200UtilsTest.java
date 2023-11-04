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

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
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

}
