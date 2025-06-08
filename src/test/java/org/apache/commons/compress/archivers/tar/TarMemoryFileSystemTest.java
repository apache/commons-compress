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

package org.apache.commons.compress.archivers.tar;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.UserPrincipal;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.junit.jupiter.api.Test;

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder;

class TarMemoryFileSystemTest {

    @Test
    void testCheckUserInformationInTarEntry() throws IOException, ArchiveException {
        final String user = "commons";
        final String group = "compress";
        try (FileSystem fileSystem = MemoryFileSystemBuilder.newLinux().addUser(user).addGroup(group).build()) {
            final Path pathSource = fileSystem.getPath("original-file.txt");
            Files.write(pathSource, "Test".getBytes(UTF_8));
            Files.setAttribute(pathSource, "posix:owner", (UserPrincipal) () -> user);
            Files.setAttribute(pathSource, "posix:group", (GroupPrincipal) () -> group);

            final Path target = fileSystem.getPath("original-file.tar");
            try (OutputStream out = Files.newOutputStream(target);
                    ArchiveOutputStream<ArchiveEntry> tarOut = ArchiveStreamFactory.DEFAULT.createArchiveOutputStream(ArchiveStreamFactory.TAR, out)) {
                final TarArchiveEntry entry = new TarArchiveEntry(pathSource);
                tarOut.putArchiveEntry(entry);
                tarOut.write(pathSource);
                tarOut.closeArchiveEntry();
            }

            try (InputStream input = Files.newInputStream(target);
                    TarArchiveInputStream tarIn = new TarArchiveInputStream(input)) {
                final TarArchiveEntry nextTarEntry = tarIn.getNextTarEntry();

                assertEquals(user, nextTarEntry.getUserName());
                assertEquals(group, nextTarEntry.getGroupName());
            }
        }
    }

    @Test
    void testTarFromMemoryFileSystem() throws IOException, ArchiveException {
        try (FileSystem fileSystem = MemoryFileSystemBuilder.newLinux().build()) {
            final Path p = fileSystem.getPath("test.txt");
            Files.write(p, "Test".getBytes(UTF_8));
            final File f = File.createTempFile("commons-compress-memoryfs", ".tar");
            try (OutputStream out = Files.newOutputStream(f.toPath());
                    ArchiveOutputStream<ArchiveEntry> tarOut = ArchiveStreamFactory.DEFAULT.createArchiveOutputStream(ArchiveStreamFactory.TAR, out)) {
                final TarArchiveEntry entry = new TarArchiveEntry(p);
                tarOut.putArchiveEntry(entry);
                tarOut.write(p);
                tarOut.closeArchiveEntry();
                assertEquals(f.length(), tarOut.getBytesWritten());
            } finally {
                AbstractTest.forceDelete(f);
            }
        }
    }

    @Test
    void testTarToMemoryFileSystem() throws IOException, ArchiveException {
        try (FileSystem fileSystem = MemoryFileSystemBuilder.newLinux().build()) {
            final Path p = fileSystem.getPath("target.tar");

            try (OutputStream out = Files.newOutputStream(p);
                    ArchiveOutputStream<ArchiveEntry> tarOut = ArchiveStreamFactory.DEFAULT.createArchiveOutputStream(ArchiveStreamFactory.TAR, out)) {
                final String content = "Test";
                final TarArchiveEntry entry = new TarArchiveEntry("test.txt");
                entry.setSize(content.length());
                tarOut.putArchiveEntry(entry);

                tarOut.writeUtf8("Test");
                tarOut.closeArchiveEntry();

                assertTrue(Files.exists(p));
                assertEquals(Files.size(p), tarOut.getBytesWritten());
            }
        }
    }
}
