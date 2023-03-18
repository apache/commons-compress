/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
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

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.junit.jupiter.api.Test;

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder;

public class TarMemoryFileSystemTest {

    @Test
    public void checkUserInformationInTarEntry() throws IOException, ArchiveException {
        final String user = "commons";
        final String group = "compress";
        try (FileSystem fileSystem = MemoryFileSystemBuilder.newLinux().addUser(user).addGroup(group).build()) {
            final Path source = fileSystem.getPath("original-file.txt");
            Files.write(source, "Test".getBytes(UTF_8));
            Files.setAttribute(source, "posix:owner", (UserPrincipal) () -> user);
            Files.setAttribute(source, "posix:group", (GroupPrincipal) () -> group);

            final Path target = fileSystem.getPath("original-file.tar");
            try (final OutputStream out = Files.newOutputStream(target);
                 final ArchiveOutputStream tarOut = ArchiveStreamFactory.DEFAULT.createArchiveOutputStream(ArchiveStreamFactory.TAR, out)) {
                final TarArchiveEntry entry = new TarArchiveEntry(source);
                tarOut.putArchiveEntry(entry);

                Files.copy(source, tarOut);
                tarOut.closeArchiveEntry();
            }

            try (final InputStream input = Files.newInputStream(target);
                 final TarArchiveInputStream tarIn = new TarArchiveInputStream(input)) {
                final TarArchiveEntry nextTarEntry = tarIn.getNextTarEntry();

                assertEquals(user, nextTarEntry.getUserName());
                assertEquals(group, nextTarEntry.getGroupName());
            }
        }
    }

    @Test
    public void tarFromMemoryFileSystem() throws IOException, ArchiveException {
        try (FileSystem fileSystem = MemoryFileSystemBuilder.newLinux().build()) {
            final Path p = fileSystem.getPath("test.txt");
            Files.write(p, "Test".getBytes(UTF_8));

            final File f = File.createTempFile("commons-compress-memoryfs", ".tar");
            try (final OutputStream out = Files.newOutputStream(f.toPath());
                 final ArchiveOutputStream tarOut = ArchiveStreamFactory.DEFAULT.createArchiveOutputStream(ArchiveStreamFactory.TAR, out)) {
                final TarArchiveEntry entry = new TarArchiveEntry(p);
                tarOut.putArchiveEntry(entry);

                Files.copy(p, tarOut);
                tarOut.closeArchiveEntry();
                assertEquals(f.length(), tarOut.getBytesWritten());
            }
        }
    }

    @Test
    public void tarToMemoryFileSystem() throws IOException, ArchiveException {
        try (FileSystem fileSystem = MemoryFileSystemBuilder.newLinux().build()) {
            final Path p = fileSystem.getPath("target.tar");

            try (final OutputStream out = Files.newOutputStream(p);
                 final ArchiveOutputStream tarOut = ArchiveStreamFactory.DEFAULT.createArchiveOutputStream(ArchiveStreamFactory.TAR, out)) {
                final String content = "Test";
                final TarArchiveEntry entry = new TarArchiveEntry("test.txt");
                entry.setSize(content.length());
                tarOut.putArchiveEntry(entry);

                tarOut.write("Test".getBytes(UTF_8));
                tarOut.closeArchiveEntry();

                assertTrue(Files.exists(p));
                assertEquals(Files.size(p), tarOut.getBytesWritten());
            }
        }
    }
}
