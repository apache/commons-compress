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

import org.apache.commons.compress.AbstractTestCase;
import org.junit.jupiter.api.Test;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

import static org.junit.Assert.*;

public class FileTimesIT extends AbstractTestCase {

    @Test
    public void readTimeFromTarV7() throws Exception {
        try (InputStream in = new BufferedInputStream(Files.newInputStream(getPath("COMPRESS-612/test-times-v7.tar")));
             TarArchiveInputStream tin = new TarArchiveInputStream(in)) {
            final TarArchiveEntry e = tin.getNextTarEntry();
            assertNotNull(e);
            assertEquals("mtime", FileTime.from(Instant.parse("2022-03-14T01:25:03Z")), e.getLastModifiedTime());
            assertNull("atime", e.getLastAccessTime());
            assertNull("ctime", e.getCreationTime());
            assertNull("birthtime", e.getCreationTime());
            assertNull(tin.getNextTarEntry());
        }
    }

    @Test
    public void readTimeFromTarOldGnu() throws Exception {
        try (InputStream in = new BufferedInputStream(Files.newInputStream(getPath("COMPRESS-612/test-times-oldgnu.tar")));
             TarArchiveInputStream tin = new TarArchiveInputStream(in)) {
            final TarArchiveEntry e = tin.getNextTarEntry();
            assertNotNull(e);
            assertEquals("mtime", FileTime.from(Instant.parse("2022-03-14T01:25:03Z")), e.getLastModifiedTime());
            assertNull("atime", e.getLastAccessTime());
            assertNull("ctime", e.getCreationTime());
            assertNull("birthtime", e.getCreationTime());
            assertNull(tin.getNextTarEntry());
        }
    }

    @Test
    public void readTimeFromTarOldGnuIncremental() throws Exception {
        try (InputStream in = new BufferedInputStream(Files.newInputStream(getPath("COMPRESS-612/test-times-oldgnu-incremental.tar")));
             TarArchiveInputStream tin = new TarArchiveInputStream(in)) {
            TarArchiveEntry e = tin.getNextTarEntry();
            assertNotNull(e);
            assertEquals("name", "test-times.txt", e.getName());
            assertEquals("mtime", FileTime.from(Instant.parse("2022-03-14T01:25:03Z")), e.getLastModifiedTime());
            assertNull("atime", e.getLastAccessTime());
            assertNull("ctime", e.getCreationTime());
            assertNull("birthtime", e.getCreationTime());
            e = tin.getNextTarEntry();
            assertNotNull(e);
            assertEquals("name", "test-times.txt", e.getName());
            assertEquals("mtime", FileTime.from(Instant.parse("2022-03-14T03:17:05Z")), e.getLastModifiedTime());
            assertEquals("atime", FileTime.from(Instant.parse("2022-03-14T03:17:06Z")), e.getLastAccessTime());
            assertEquals("ctime", FileTime.from(Instant.parse("2022-03-14T03:17:05Z")), e.getStatusChangeTime());
            assertNull("birthtime", e.getCreationTime());
            assertNull(tin.getNextTarEntry());
        }
    }

    @Test
    public void readTimeFromTarGnu() throws Exception {
        try (InputStream in = new BufferedInputStream(Files.newInputStream(getPath("COMPRESS-612/test-times-gnu.tar")));
             TarArchiveInputStream tin = new TarArchiveInputStream(in)) {
            final TarArchiveEntry e = tin.getNextTarEntry();
            assertNotNull(e);
            assertEquals("mtime", FileTime.from(Instant.parse("2022-03-14T01:25:03Z")), e.getLastModifiedTime());
            assertNull("atime", e.getLastAccessTime());
            assertNull("ctime", e.getCreationTime());
            assertNull("birthtime", e.getCreationTime());
            assertNull(tin.getNextTarEntry());
        }
    }

    @Test
    public void readTimeFromTarGnuIncremental() throws Exception {
        try (InputStream in = new BufferedInputStream(Files.newInputStream(getPath("COMPRESS-612/test-times-gnu-incremental.tar")));
             TarArchiveInputStream tin = new TarArchiveInputStream(in)) {
            TarArchiveEntry e = tin.getNextTarEntry();
            assertNotNull(e);
            assertEquals("name", "test-times.txt", e.getName());
            assertEquals("mtime", FileTime.from(Instant.parse("2022-03-14T01:25:03Z")), e.getLastModifiedTime());
            assertNull("atime", e.getLastAccessTime());
            assertNull("ctime", e.getCreationTime());
            assertNull("birthtime", e.getCreationTime());
            e = tin.getNextTarEntry();
            assertNotNull(e);
            assertEquals("name", "test-times.txt", e.getName());
            assertEquals("mtime", FileTime.from(Instant.parse("2022-03-14T03:17:05Z")), e.getLastModifiedTime());
            assertEquals("atime", FileTime.from(Instant.parse("2022-03-14T03:17:10Z")), e.getLastAccessTime());
            assertEquals("ctime", FileTime.from(Instant.parse("2022-03-14T03:17:10Z")), e.getStatusChangeTime());
            assertNull("birthtime", e.getCreationTime());
            assertNull(tin.getNextTarEntry());
        }
    }

    @Test
    public void readTimeFromTarUstar() throws Exception {
        try (InputStream in = new BufferedInputStream(Files.newInputStream(getPath("COMPRESS-612/test-times-ustar.tar")));
             TarArchiveInputStream tin = new TarArchiveInputStream(in)) {
            final TarArchiveEntry e = tin.getNextTarEntry();
            assertNotNull(e);
            assertEquals("mtime", FileTime.from(Instant.parse("2022-03-14T01:25:03Z")), e.getLastModifiedTime());
            assertNull("atime", e.getLastAccessTime());
            assertNull("ctime", e.getCreationTime());
            assertNull("birthtime", e.getCreationTime());
            assertNull(tin.getNextTarEntry());
        }
    }

    @Test
    public void readTimeFromTarXstar() throws Exception {
        try (InputStream in = new BufferedInputStream(Files.newInputStream(getPath("COMPRESS-612/test-times-xstar.tar")));
             TarArchiveInputStream tin = new TarArchiveInputStream(in)) {
            final TarArchiveEntry e = tin.getNextTarEntry();
            assertNotNull(e);
            assertEquals("mtime", FileTime.from(Instant.parse("2022-03-14T04:11:22Z")), e.getLastModifiedTime());
            assertEquals("atime", FileTime.from(Instant.parse("2022-03-14T04:12:48Z")), e.getLastAccessTime());
            assertEquals("ctime", FileTime.from(Instant.parse("2022-03-14T04:12:47Z")), e.getStatusChangeTime());
            assertNull("birthtime", e.getCreationTime());
            assertNull(tin.getNextTarEntry());
        }
    }

    @Test
    public void readTimeFromTarXstarIncremental() throws Exception {
        try (InputStream in = new BufferedInputStream(Files.newInputStream(getPath("COMPRESS-612/test-times-xstar-incremental.tar")));
             TarArchiveInputStream tin = new TarArchiveInputStream(in)) {
            TarArchiveEntry e = tin.getNextTarEntry();
            assertNotNull(e);
            assertEquals("name", "test-times.txt", e.getName());
            assertEquals("mtime", FileTime.from(Instant.parse("2022-03-14T04:03:29Z")), e.getLastModifiedTime());
            assertEquals("atime", FileTime.from(Instant.parse("2022-03-14T04:03:29Z")), e.getLastAccessTime());
            assertEquals("ctime", FileTime.from(Instant.parse("2022-03-14T04:03:29Z")), e.getStatusChangeTime());
            assertNull("birthtime", e.getCreationTime());
            e = tin.getNextTarEntry();
            assertNotNull(e);
            assertEquals("name", "test-times.txt", e.getName());
            assertEquals("mtime", FileTime.from(Instant.parse("2022-03-14T04:11:22Z")), e.getLastModifiedTime());
            assertEquals("atime", FileTime.from(Instant.parse("2022-03-14T04:11:23Z")), e.getLastAccessTime());
            assertEquals("ctime", FileTime.from(Instant.parse("2022-03-14T04:11:22Z")), e.getStatusChangeTime());
            assertNull("birthtime", e.getCreationTime());
            assertNull(tin.getNextTarEntry());
        }
    }

    @Test
    public void readTimeFromTarXustar() throws Exception {
        try (InputStream in = new BufferedInputStream(Files.newInputStream(getPath("COMPRESS-612/test-times-xustar.tar")));
             TarArchiveInputStream tin = new TarArchiveInputStream(in)) {
            final TarArchiveEntry e = tin.getNextTarEntry();
            assertNotNull(e);
            assertEquals("mtime", FileTime.from(Instant.parse("2022-03-14T15:13:15Z")), e.getLastModifiedTime());
            assertEquals("atime", FileTime.from(Instant.parse("2022-03-14T15:13:41Z")), e.getLastAccessTime());
            assertEquals("ctime", FileTime.from(Instant.parse("2022-03-14T15:13:40Z")), e.getStatusChangeTime());
            assertNull("birthtime", e.getCreationTime());
            assertNull(tin.getNextTarEntry());
        }
    }

    @Test
    public void readTimeFromTarXustarIncremental() throws Exception {
        try (InputStream in = new BufferedInputStream(Files.newInputStream(getPath("COMPRESS-612/test-times-xustar-incremental.tar")));
             TarArchiveInputStream tin = new TarArchiveInputStream(in)) {
            TarArchiveEntry e = tin.getNextTarEntry();
            assertNotNull(e);
            assertEquals("name", "test-times.txt", e.getName());
            assertEquals("mtime", FileTime.from(Instant.parse("2022-03-14T04:11:22Z")), e.getLastModifiedTime());
            assertEquals("atime", FileTime.from(Instant.parse("2022-03-14T04:12:48Z")), e.getLastAccessTime());
            assertEquals("ctime", FileTime.from(Instant.parse("2022-03-14T04:12:47Z")), e.getStatusChangeTime());
            assertNull("birthtime", e.getCreationTime());
            e = tin.getNextTarEntry();
            assertNotNull(e);
            assertEquals("name", "test-times.txt", e.getName());
            assertEquals("mtime", FileTime.from(Instant.parse("2022-03-14T15:13:15Z")), e.getLastModifiedTime());
            assertEquals("atime", FileTime.from(Instant.parse("2022-03-14T15:13:16Z")), e.getLastAccessTime());
            assertEquals("ctime", FileTime.from(Instant.parse("2022-03-14T15:13:15Z")), e.getStatusChangeTime());
            assertNull("birthtime", e.getCreationTime());
            assertNull(tin.getNextTarEntry());
        }
    }

    @Test
    public void readTimeFromTarPosix() throws Exception {
        try (InputStream in = new BufferedInputStream(Files.newInputStream(getPath("COMPRESS-612/test-times-posix.tar")));
             TarArchiveInputStream tin = new TarArchiveInputStream(in)) {
            final TarArchiveEntry e = tin.getNextTarEntry();
            assertNotNull(e);
            assertEquals("mtime", FileTime.from(Instant.parse("2022-03-14T01:25:03.599853900Z")), e.getLastModifiedTime());
            assertEquals("atime", FileTime.from(Instant.parse("2022-03-14T01:31:00.706927200Z")), e.getLastAccessTime());
            assertEquals("ctime", FileTime.from(Instant.parse("2022-03-14T01:28:59.700505300Z")), e.getStatusChangeTime());
            assertNull("birthtime", e.getCreationTime());
            assertNull(tin.getNextTarEntry());
        }
    }

    @Test
    public void readTimeFromTarPosixLinux() throws Exception {
        try (InputStream in = new BufferedInputStream(Files.newInputStream(getPath("COMPRESS-612/test-times-posix-linux.tar")));
             TarArchiveInputStream tin = new TarArchiveInputStream(in)) {
            final TarArchiveEntry e = tin.getNextTarEntry();
            assertNotNull(e);
            assertEquals("mtime", FileTime.from(Instant.parse("2022-03-14T01:25:03.599853900Z")), e.getLastModifiedTime());
            assertEquals("atime", FileTime.from(Instant.parse("2022-03-14T01:32:13.837251500Z")), e.getLastAccessTime());
            assertEquals("ctime", FileTime.from(Instant.parse("2022-03-14T01:31:00.706927200Z")), e.getStatusChangeTime());
            assertNull("birthtime", e.getCreationTime());
            assertNull(tin.getNextTarEntry());
        }
    }

    @Test
    public void readTimeFromTarPosixLibArchive() throws Exception {
        try (InputStream in = new BufferedInputStream(Files.newInputStream(getPath("COMPRESS-612/test-times-bsd-folder.tar")));
             TarArchiveInputStream tin = new TarArchiveInputStream(in)) {
            TarArchiveEntry e = tin.getNextTarEntry();
            assertNotNull(e);
            assertEquals("name", "test/", e.getName());
            assertTrue(e.isDirectory());
            assertEquals("mtime", FileTime.from(Instant.parse("2022-03-16T10:19:43.382883700Z")), e.getLastModifiedTime());
            assertEquals("atime", FileTime.from(Instant.parse("2022-03-16T10:21:01.251181000Z")), e.getLastAccessTime());
            assertEquals("ctime", FileTime.from(Instant.parse("2022-03-16T10:19:24.105111500Z")), e.getStatusChangeTime());
            assertEquals("birthtime", FileTime.from(Instant.parse("2022-03-16T10:19:24.105111500Z")), e.getCreationTime());
            e = tin.getNextTarEntry();
            assertEquals("name", "test/test-times.txt", e.getName());
            assertTrue(e.isFile());
            assertEquals("mtime", FileTime.from(Instant.parse("2022-03-16T10:21:00.249238500Z")), e.getLastModifiedTime());
            assertEquals("atime", FileTime.from(Instant.parse("2022-03-16T10:21:01.251181000Z")), e.getLastAccessTime());
            assertEquals("ctime", FileTime.from(Instant.parse("2022-03-14T01:25:03.599853900Z")), e.getStatusChangeTime());
            assertEquals("birthtime", FileTime.from(Instant.parse("2022-03-14T01:25:03.599853900Z")), e.getCreationTime());
            assertNull(tin.getNextTarEntry());
        }
    }
}
