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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

import org.apache.commons.compress.AbstractTest;
import org.junit.jupiter.api.Test;

public class FileTimesIT extends AbstractTest {

    private void assertGlobalHeaders(final TarArchiveEntry e) {
        assertEquals(5, e.getExtraPaxHeaders().size());
        assertEquals("exustar", e.getExtraPaxHeader("SCHILY.archtype"), "SCHILY.archtype");
        assertEquals("1647478879.579980900", e.getExtraPaxHeader("SCHILY.volhdr.dumpdate"), "SCHILY.volhdr.dumpdate");
        assertEquals("star 1.6 (x86_64-unknown-linux-gnu) 2019/04/01", e.getExtraPaxHeader("SCHILY.release"), "SCHILY.release");
        assertEquals("20", e.getExtraPaxHeader("SCHILY.volhdr.blocksize"), "SCHILY.volhdr.blocksize");
        assertEquals("1", e.getExtraPaxHeader("SCHILY.volhdr.volno"), "SCHILY.volhdr.volno");
    }

    // Extended POSIX.1-2001 standard tar + x-header
    // Created using s-tar 1.6
    @Test
    void testReadTimeFromTarEpax() throws Exception {
        final String file = "COMPRESS-612/test-times-epax-folder.tar";
        try (InputStream in = new BufferedInputStream(Files.newInputStream(getPath(file)));
                TarArchiveInputStream tin = new TarArchiveInputStream(in)) {
            TarArchiveEntry e = tin.getNextTarEntry();
            assertNotNull(e);
            assertTrue(e.getExtraPaxHeaders().isEmpty());
            assertEquals("test/", e.getName());
            assertEquals(TarConstants.LF_DIR, e.getLinkFlag());
            assertTrue(e.isDirectory());
            assertEquals(toFileTime("2022-03-17T00:24:44.147126600Z"), e.getLastModifiedTime(), "mtime");
            assertEquals(toFileTime("2022-03-17T01:02:11.910960100Z"), e.getLastAccessTime(), "atime");
            assertEquals(toFileTime("2022-03-17T00:24:44.147126600Z"), e.getStatusChangeTime(), "ctime");
            assertNull(e.getCreationTime(), "birthtime");
            e = tin.getNextTarEntry();
            assertNotNull(e);
            assertTrue(e.getExtraPaxHeaders().isEmpty());
            assertEquals("test/test-times.txt", e.getName());
            assertEquals(TarConstants.LF_NORMAL, e.getLinkFlag());
            assertTrue(e.isFile());
            assertEquals(toFileTime("2022-03-17T00:38:20.470751500Z"), e.getLastModifiedTime(), "mtime");
            assertEquals(toFileTime("2022-03-17T00:38:20.536752000Z"), e.getLastAccessTime(), "atime");
            assertEquals(toFileTime("2022-03-17T00:38:20.470751500Z"), e.getStatusChangeTime(), "ctime");
            assertNull(e.getCreationTime(), "birthtime");
            assertNull(tin.getNextTarEntry());
        }
    }

    // 'xustar' format - always x-header
    @Test
    void testReadTimeFromTarExustar() throws Exception {
        final String file = "COMPRESS-612/test-times-exustar-folder.tar";
        try (InputStream in = new BufferedInputStream(Files.newInputStream(getPath(file)));
                TarArchiveInputStream tin = new TarArchiveInputStream(in)) {
            TarArchiveEntry e = tin.getNextTarEntry();
            assertNotNull(e);
            assertEquals("test/", e.getName());
            assertEquals(TarConstants.LF_DIR, e.getLinkFlag());
            assertTrue(e.isDirectory());
            assertEquals(toFileTime("2022-03-17T00:24:44.147126600Z"), e.getLastModifiedTime(), "mtime");
            assertEquals(toFileTime("2022-03-17T00:47:00.367783300Z"), e.getLastAccessTime(), "atime");
            assertEquals(toFileTime("2022-03-17T00:24:44.147126600Z"), e.getStatusChangeTime(), "ctime");
            assertNull(e.getCreationTime(), "birthtime");
            assertGlobalHeaders(e);
            e = tin.getNextTarEntry();
            assertEquals("test/test-times.txt", e.getName());
            assertEquals(TarConstants.LF_NORMAL, e.getLinkFlag());
            assertTrue(e.isFile());
            assertEquals(toFileTime("2022-03-17T00:38:20.470751500Z"), e.getLastModifiedTime(), "mtime");
            assertEquals(toFileTime("2022-03-17T00:38:20.536752000Z"), e.getLastAccessTime(), "atime");
            assertEquals(toFileTime("2022-03-17T00:38:20.470751500Z"), e.getStatusChangeTime(), "ctime");
            assertNull(e.getCreationTime(), "birthtime");
            assertGlobalHeaders(e);
            assertNull(tin.getNextTarEntry());
        }
    }

    // GNU tar format 1989 (violates POSIX)
    // Created using GNU tar
    @Test
    void testReadTimeFromTarGnu() throws Exception {
        final String file = "COMPRESS-612/test-times-gnu.tar";
        try (InputStream in = new BufferedInputStream(Files.newInputStream(getPath(file)));
                TarArchiveInputStream tin = new TarArchiveInputStream(in)) {
            final TarArchiveEntry e = tin.getNextTarEntry();
            assertNotNull(e);
            assertTrue(e.getExtraPaxHeaders().isEmpty());
            assertEquals(toFileTime("2022-03-14T01:25:03Z"), e.getLastModifiedTime(), "mtime");
            assertNull(e.getLastAccessTime(), "atime");
            assertNull(e.getStatusChangeTime(), "ctime");
            assertNull(e.getCreationTime(), "birthtime");
            assertNull(tin.getNextTarEntry());
        }
    }

    // GNU tar format 1989 (violates POSIX)
    // Created using GNU tar
    @Test
    void testReadTimeFromTarGnuIncremental() throws Exception {
        final String file = "COMPRESS-612/test-times-gnu-incremental.tar";
        try (InputStream in = new BufferedInputStream(Files.newInputStream(getPath(file)));
                TarArchiveInputStream tin = new TarArchiveInputStream(in)) {
            TarArchiveEntry e = tin.getNextTarEntry();
            assertNotNull(e);
            assertTrue(e.getExtraPaxHeaders().isEmpty());
            assertEquals("test-times.txt", e.getName());
            assertEquals(toFileTime("2022-03-14T01:25:03Z"), e.getLastModifiedTime(), "mtime");
            assertNull(e.getLastAccessTime(), "atime");
            assertNull(e.getStatusChangeTime(), "ctime");
            assertNull(e.getCreationTime(), "birthtime");
            e = tin.getNextTarEntry();
            assertNotNull(e);
            assertTrue(e.getExtraPaxHeaders().isEmpty());
            assertEquals("test-times.txt", e.getName());
            assertEquals(toFileTime("2022-03-14T03:17:05Z"), e.getLastModifiedTime(), "mtime");
            assertEquals(toFileTime("2022-03-14T03:17:10Z"), e.getLastAccessTime(), "atime");
            assertEquals(toFileTime("2022-03-14T03:17:10Z"), e.getStatusChangeTime(), "ctime");
            assertNull(e.getCreationTime(), "birthtime");
            assertNull(tin.getNextTarEntry());
        }
    }

    // GNU tar format 1989 (violates POSIX)
    // Created using s-tar 1.6, which somehow differs from GNU tar's.
    @Test
    void testReadTimeFromTarGnuTar() throws Exception {
        final String file = "COMPRESS-612/test-times-gnutar.tar";
        try (InputStream in = new BufferedInputStream(Files.newInputStream(getPath(file)));
                TarArchiveInputStream tin = new TarArchiveInputStream(in)) {
            final TarArchiveEntry e = tin.getNextTarEntry();
            assertNotNull(e);
            assertTrue(e.getExtraPaxHeaders().isEmpty());
            assertEquals(toFileTime("2022-03-17T01:52:25Z"), e.getLastModifiedTime(), "mtime");
            assertEquals(toFileTime("2022-03-17T01:52:25Z"), e.getLastAccessTime(), "atime");
            assertEquals(toFileTime("2022-03-17T01:52:25Z"), e.getStatusChangeTime(), "ctime");
            assertNull(e.getCreationTime(), "birthtime");
            assertNull(tin.getNextTarEntry());
        }
    }

    // Old BSD tar format
    @Test
    void testReadTimeFromTarOldBsdTar() throws Exception {
        final String file = "COMPRESS-612/test-times-oldbsdtar.tar";
        try (InputStream in = new BufferedInputStream(Files.newInputStream(getPath(file)));
                TarArchiveInputStream tin = new TarArchiveInputStream(in)) {
            final TarArchiveEntry e = tin.getNextTarEntry();
            assertNotNull(e);
            assertTrue(e.getExtraPaxHeaders().isEmpty());
            assertEquals(toFileTime("2022-03-17T01:52:25Z"), e.getLastModifiedTime(), "mtime");
            assertNull(e.getLastAccessTime(), "atime");
            assertNull(e.getStatusChangeTime(), "ctime");
            assertNull(e.getCreationTime(), "birthtime");
            assertNull(tin.getNextTarEntry());
        }
    }

    // Format used by GNU tar of versions prior to 1.12
    // Created using GNU tar
    @Test
    void testReadTimeFromTarOldGnu() throws Exception {
        final String file = "COMPRESS-612/test-times-oldgnu.tar";
        try (InputStream in = new BufferedInputStream(Files.newInputStream(getPath(file)));
                TarArchiveInputStream tin = new TarArchiveInputStream(in)) {
            final TarArchiveEntry e = tin.getNextTarEntry();
            assertNotNull(e);
            assertTrue(e.getExtraPaxHeaders().isEmpty());
            assertEquals(toFileTime("2022-03-14T01:25:03Z"), e.getLastModifiedTime(), "mtime");
            assertNull(e.getLastAccessTime(), "atime");
            assertNull(e.getStatusChangeTime(), "ctime");
            assertNull(e.getCreationTime(), "birthtime");
            assertNull(tin.getNextTarEntry());
        }
    }

    // Format used by GNU tar of versions prior to 1.12
    // Created using GNU tar
    @Test
    void testReadTimeFromTarOldGnuIncremental() throws Exception {
        final String file = "COMPRESS-612/test-times-oldgnu-incremental.tar";
        try (InputStream in = new BufferedInputStream(Files.newInputStream(getPath(file)));
                TarArchiveInputStream tin = new TarArchiveInputStream(in)) {
            TarArchiveEntry e = tin.getNextTarEntry();
            assertNotNull(e);
            assertTrue(e.getExtraPaxHeaders().isEmpty());
            assertEquals("test-times.txt", e.getName());
            assertEquals(toFileTime("2022-03-14T01:25:03Z"), e.getLastModifiedTime(), "mtime");
            assertNull(e.getLastAccessTime(), "atime");
            assertNull(e.getStatusChangeTime(), "ctime");
            assertNull(e.getCreationTime(), "birthtime");
            e = tin.getNextTarEntry();
            assertNotNull(e);
            assertTrue(e.getExtraPaxHeaders().isEmpty());
            assertEquals("test-times.txt", e.getName());
            assertEquals(toFileTime("2022-03-14T03:17:05Z"), e.getLastModifiedTime(), "mtime");
            assertEquals(toFileTime("2022-03-14T03:17:06Z"), e.getLastAccessTime(), "atime");
            assertEquals(toFileTime("2022-03-14T03:17:05Z"), e.getStatusChangeTime(), "ctime");
            assertNull(e.getCreationTime(), "birthtime");
            assertNull(tin.getNextTarEntry());
        }
    }

    // Extended POSIX.1-2001 standard tar
    // Created using s-tar 1.6, which somehow differs from GNU tar's.
    @Test
    void testReadTimeFromTarPax() throws Exception {
        final String file = "COMPRESS-612/test-times-pax-folder.tar";
        try (InputStream in = new BufferedInputStream(Files.newInputStream(getPath(file)));
                TarArchiveInputStream tin = new TarArchiveInputStream(in)) {
            TarArchiveEntry e = tin.getNextTarEntry();
            assertNotNull(e);
            assertTrue(e.getExtraPaxHeaders().isEmpty());
            assertEquals("test/", e.getName());
            assertEquals(TarConstants.LF_DIR, e.getLinkFlag());
            assertTrue(e.isDirectory());
            assertEquals(toFileTime("2022-03-17T00:24:44.147126600Z"), e.getLastModifiedTime(), "mtime");
            assertEquals(toFileTime("2022-03-17T01:01:53.369146300Z"), e.getLastAccessTime(), "atime");
            assertEquals(toFileTime("2022-03-17T00:24:44.147126600Z"), e.getStatusChangeTime(), "ctime");
            assertNull(e.getCreationTime(), "birthtime");
            e = tin.getNextTarEntry();
            assertNotNull(e);
            assertTrue(e.getExtraPaxHeaders().isEmpty());
            assertEquals("test/test-times.txt", e.getName());
            assertEquals(TarConstants.LF_NORMAL, e.getLinkFlag());
            assertTrue(e.isFile());
            assertEquals(toFileTime("2022-03-17T00:38:20.470751500Z"), e.getLastModifiedTime(), "mtime");
            assertEquals(toFileTime("2022-03-17T00:38:20.536752000Z"), e.getLastAccessTime(), "atime");
            assertEquals(toFileTime("2022-03-17T00:38:20.470751500Z"), e.getStatusChangeTime(), "ctime");
            assertNull(e.getCreationTime(), "birthtime");
            assertNull(tin.getNextTarEntry());
        }
    }

    // Extended POSIX.1-2001 standard tar
    // Created using GNU tar
    @Test
    void testReadTimeFromTarPosix() throws Exception {
        final String file = "COMPRESS-612/test-times-posix.tar";
        try (InputStream in = new BufferedInputStream(Files.newInputStream(getPath(file)));
                TarArchiveInputStream tin = new TarArchiveInputStream(in)) {
            final TarArchiveEntry e = tin.getNextTarEntry();
            assertNotNull(e);
            assertTrue(e.getExtraPaxHeaders().isEmpty());
            assertEquals(toFileTime("2022-03-14T01:25:03.599853900Z"), e.getLastModifiedTime(), "mtime");
            assertEquals(toFileTime("2022-03-14T01:31:00.706927200Z"), e.getLastAccessTime(), "atime");
            assertEquals(toFileTime("2022-03-14T01:28:59.700505300Z"), e.getStatusChangeTime(), "ctime");
            assertNull(e.getCreationTime(), "birthtime");
            assertNull(tin.getNextTarEntry());
        }
    }

    // Extended POSIX.1-2001 standard tar
    // Created using BSD tar on Windows
    @Test
    void testReadTimeFromTarPosixLibArchive() throws Exception {
        final String file = "COMPRESS-612/test-times-bsd-folder.tar";
        try (InputStream in = new BufferedInputStream(Files.newInputStream(getPath(file)));
                TarArchiveInputStream tin = new TarArchiveInputStream(in)) {
            TarArchiveEntry e = tin.getNextTarEntry();
            assertNotNull(e);
            assertTrue(e.getExtraPaxHeaders().isEmpty());
            assertEquals("test/", e.getName());
            assertEquals(TarConstants.LF_DIR, e.getLinkFlag());
            assertTrue(e.isDirectory());
            assertEquals(toFileTime("2022-03-16T10:19:43.382883700Z"), e.getLastModifiedTime(), "mtime");
            assertEquals(toFileTime("2022-03-16T10:21:01.251181000Z"), e.getLastAccessTime(), "atime");
            assertEquals(toFileTime("2022-03-16T10:19:24.105111500Z"), e.getStatusChangeTime(), "ctime");
            assertEquals(toFileTime("2022-03-16T10:19:24.105111500Z"), e.getCreationTime(), "birthtime");
            e = tin.getNextTarEntry();
            assertNotNull(e);
            assertTrue(e.getExtraPaxHeaders().isEmpty());
            assertEquals("test/test-times.txt", e.getName());
            assertEquals(TarConstants.LF_NORMAL, e.getLinkFlag());
            assertTrue(e.isFile());
            assertEquals(toFileTime("2022-03-16T10:21:00.249238500Z"), e.getLastModifiedTime(), "mtime");
            assertEquals(toFileTime("2022-03-16T10:21:01.251181000Z"), e.getLastAccessTime(), "atime");
            assertEquals(toFileTime("2022-03-14T01:25:03.599853900Z"), e.getStatusChangeTime(), "ctime");
            assertEquals(toFileTime("2022-03-14T01:25:03.599853900Z"), e.getCreationTime(), "birthtime");
            assertNull(tin.getNextTarEntry());
        }
    }

    // Extended POSIX.1-2001 standard tar
    // Created using GNU tar on Linux
    @Test
    void testReadTimeFromTarPosixLinux() throws Exception {
        final String file = "COMPRESS-612/test-times-posix-linux.tar";
        try (InputStream in = new BufferedInputStream(Files.newInputStream(getPath(file)));
                TarArchiveInputStream tin = new TarArchiveInputStream(in)) {
            final TarArchiveEntry e = tin.getNextTarEntry();
            assertNotNull(e);
            assertTrue(e.getExtraPaxHeaders().isEmpty());
            assertEquals(toFileTime("2022-03-14T01:25:03.599853900Z"), e.getLastModifiedTime(), "mtime");
            assertEquals(toFileTime("2022-03-14T01:32:13.837251500Z"), e.getLastAccessTime(), "atime");
            assertEquals(toFileTime("2022-03-14T01:31:00.706927200Z"), e.getStatusChangeTime(), "ctime");
            assertNull(e.getCreationTime(), "birthtime");
            assertNull(tin.getNextTarEntry());
        }
    }

    // Old star format from 1985
    @Test
    void testReadTimeFromTarStarFolder() throws Exception {
        final String file = "COMPRESS-612/test-times-star-folder.tar";
        try (InputStream in = new BufferedInputStream(Files.newInputStream(getPath(file)));
                TarArchiveInputStream tin = new TarArchiveInputStream(in)) {
            TarArchiveEntry e = tin.getNextTarEntry();
            assertNotNull(e);
            assertTrue(e.getExtraPaxHeaders().isEmpty());
            assertEquals("test/", e.getName());
            assertEquals(TarConstants.LF_DIR, e.getLinkFlag());
            assertTrue(e.isDirectory());
            assertEquals(toFileTime("2022-03-17T00:24:44Z"), e.getLastModifiedTime(), "mtime");
            assertNull(e.getLastAccessTime(), "atime");
            assertNull(e.getStatusChangeTime(), "ctime");
            assertNull(e.getCreationTime(), "birthtime");
            e = tin.getNextTarEntry();
            assertTrue(e.getExtraPaxHeaders().isEmpty());
            assertEquals("test/test-times.txt", e.getName());
            assertEquals(TarConstants.LF_NORMAL, e.getLinkFlag());
            assertTrue(e.isFile());
            assertEquals(toFileTime("2022-03-17T00:38:20Z"), e.getLastModifiedTime(), "mtime");
            assertNull(e.getLastAccessTime(), "atime");
            assertNull(e.getStatusChangeTime(), "ctime");
            assertNull(e.getCreationTime(), "birthtime");
            assertNull(tin.getNextTarEntry());
        }
    }

    // Standard POSIX.1-1988 tar format
    @Test
    void testReadTimeFromTarUstar() throws Exception {
        final String file = "COMPRESS-612/test-times-ustar.tar";
        try (InputStream in = new BufferedInputStream(Files.newInputStream(getPath(file)));
                TarArchiveInputStream tin = new TarArchiveInputStream(in)) {
            final TarArchiveEntry e = tin.getNextTarEntry();
            assertNotNull(e);
            assertTrue(e.getExtraPaxHeaders().isEmpty());
            assertEquals(toFileTime("2022-03-14T01:25:03Z"), e.getLastModifiedTime(), "mtime");
            assertNull(e.getLastAccessTime(), "atime");
            assertNull(e.getStatusChangeTime(), "ctime");
            assertNull(e.getCreationTime(), "birthtime");
            assertNull(tin.getNextTarEntry());
        }
    }

    // Old Unix V7 tar format
    @Test
    void testReadTimeFromTarV7() throws Exception {
        final String file = "COMPRESS-612/test-times-v7.tar";
        try (InputStream in = new BufferedInputStream(Files.newInputStream(getPath(file)));
                TarArchiveInputStream tin = new TarArchiveInputStream(in)) {
            final TarArchiveEntry e = tin.getNextTarEntry();
            assertNotNull(e);
            assertTrue(e.getExtraPaxHeaders().isEmpty());
            assertEquals(toFileTime("2022-03-14T01:25:03Z"), e.getLastModifiedTime(), "mtime");
            assertNull(e.getLastAccessTime(), "atime");
            assertNull(e.getStatusChangeTime(), "ctime");
            assertNull(e.getCreationTime(), "birthtime");
            assertNull(tin.getNextTarEntry());
        }
    }

    // Extended standard tar (star 1994)
    @Test
    void testReadTimeFromTarXstar() throws Exception {
        final String file = "COMPRESS-612/test-times-xstar.tar";
        try (InputStream in = new BufferedInputStream(Files.newInputStream(getPath(file)));
                TarArchiveInputStream tin = new TarArchiveInputStream(in)) {
            final TarArchiveEntry e = tin.getNextTarEntry();
            assertNotNull(e);
            assertTrue(e.getExtraPaxHeaders().isEmpty());
            assertEquals(toFileTime("2022-03-14T04:11:22Z"), e.getLastModifiedTime(), "mtime");
            assertEquals(toFileTime("2022-03-14T04:12:48Z"), e.getLastAccessTime(), "atime");
            assertEquals(toFileTime("2022-03-14T04:12:47Z"), e.getStatusChangeTime(), "ctime");
            assertNull(e.getCreationTime(), "birthtime");
            assertNull(tin.getNextTarEntry());
        }
    }

    // Extended standard tar (star 1994)
    @Test
    void testReadTimeFromTarXstarFolder() throws Exception {
        final String file = "COMPRESS-612/test-times-xstar-folder.tar";
        try (InputStream in = new BufferedInputStream(Files.newInputStream(getPath(file)));
                TarArchiveInputStream tin = new TarArchiveInputStream(in)) {
            TarArchiveEntry e = tin.getNextTarEntry();
            assertNotNull(e);
            assertTrue(e.getExtraPaxHeaders().isEmpty());
            assertEquals("test/", e.getName());
            assertEquals(TarConstants.LF_DIR, e.getLinkFlag());
            assertTrue(e.isDirectory());
            assertEquals(toFileTime("2022-03-17T00:24:44Z"), e.getLastModifiedTime(), "mtime");
            assertEquals(toFileTime("2022-03-17T01:01:34Z"), e.getLastAccessTime(), "atime");
            assertEquals(toFileTime("2022-03-17T00:24:44Z"), e.getStatusChangeTime(), "ctime");
            assertNull(e.getCreationTime(), "birthtime");
            e = tin.getNextTarEntry();
            assertEquals("test/test-times.txt", e.getName());
            assertEquals(TarConstants.LF_NORMAL, e.getLinkFlag());
            assertTrue(e.isFile());
            assertTrue(e.getExtraPaxHeaders().isEmpty());
            assertEquals(toFileTime("2022-03-17T00:38:20Z"), e.getLastModifiedTime(), "mtime");
            assertEquals(toFileTime("2022-03-17T00:38:20Z"), e.getLastAccessTime(), "atime");
            assertEquals(toFileTime("2022-03-17T00:38:20Z"), e.getStatusChangeTime(), "ctime");
            assertNull(e.getCreationTime(), "birthtime");
            assertNull(tin.getNextTarEntry());
        }
    }

    // Extended standard tar (star 1994)
    @Test
    void testReadTimeFromTarXstarIncremental() throws Exception {
        final String file = "COMPRESS-612/test-times-xstar-incremental.tar";
        try (InputStream in = new BufferedInputStream(Files.newInputStream(getPath(file)));
                TarArchiveInputStream tin = new TarArchiveInputStream(in)) {
            TarArchiveEntry e = tin.getNextTarEntry();
            assertNotNull(e);
            assertTrue(e.getExtraPaxHeaders().isEmpty());
            assertEquals("test-times.txt", e.getName());
            assertEquals(toFileTime("2022-03-14T04:03:29Z"), e.getLastModifiedTime(), "mtime");
            assertEquals(toFileTime("2022-03-14T04:03:29Z"), e.getLastAccessTime(), "atime");
            assertEquals(toFileTime("2022-03-14T04:03:29Z"), e.getStatusChangeTime(), "ctime");
            assertNull(e.getCreationTime(), "birthtime");
            e = tin.getNextTarEntry();
            assertNotNull(e);
            assertTrue(e.getExtraPaxHeaders().isEmpty());
            assertEquals("test-times.txt", e.getName(), "name");
            assertEquals(toFileTime("2022-03-14T04:11:22Z"), e.getLastModifiedTime(), "mtime");
            assertEquals(toFileTime("2022-03-14T04:11:23Z"), e.getLastAccessTime(), "atime");
            assertEquals(toFileTime("2022-03-14T04:11:22Z"), e.getStatusChangeTime(), "ctime");
            assertNull(e.getCreationTime(), "birthtime");
            assertNull(tin.getNextTarEntry());
        }
    }

    // 'xstar' format without tar signature
    @Test
    void testReadTimeFromTarXustar() throws Exception {
        final String file = "COMPRESS-612/test-times-xustar.tar";
        try (InputStream in = new BufferedInputStream(Files.newInputStream(getPath(file)));
                TarArchiveInputStream tin = new TarArchiveInputStream(in)) {
            final TarArchiveEntry e = tin.getNextTarEntry();
            assertNotNull(e);
            assertTrue(e.getExtraPaxHeaders().isEmpty());
            assertEquals(toFileTime("2022-03-17T00:38:20.470751500Z"), e.getLastModifiedTime(), "mtime");
            assertEquals(toFileTime("2022-03-17T00:38:20.536752000Z"), e.getLastAccessTime(), "atime");
            assertEquals(toFileTime("2022-03-17T00:38:20.470751500Z"), e.getStatusChangeTime(), "ctime");
            assertNull(e.getCreationTime(), "birthtime");
            assertNull(tin.getNextTarEntry());
        }
    }

    // 'xstar' format without tar signature
    @Test
    void testReadTimeFromTarXustarFolder() throws Exception {
        final String file = "COMPRESS-612/test-times-xustar-folder.tar";
        try (InputStream in = new BufferedInputStream(Files.newInputStream(getPath(file)));
                TarArchiveInputStream tin = new TarArchiveInputStream(in)) {
            TarArchiveEntry e = tin.getNextTarEntry();
            assertNotNull(e);
            assertTrue(e.getExtraPaxHeaders().isEmpty());
            assertEquals("test/", e.getName());
            assertEquals(TarConstants.LF_DIR, e.getLinkFlag());
            assertTrue(e.isDirectory());
            assertEquals(toFileTime("2022-03-17T00:24:44.147126600Z"), e.getLastModifiedTime(), "mtime");
            assertEquals(toFileTime("2022-03-17T01:01:19.581236400Z"), e.getLastAccessTime(), "atime");
            assertEquals(toFileTime("2022-03-17T00:24:44.147126600Z"), e.getStatusChangeTime(), "ctime");
            assertNull(e.getCreationTime(), "birthtime");
            e = tin.getNextTarEntry();
            assertTrue(e.getExtraPaxHeaders().isEmpty());
            assertEquals("test/test-times.txt", e.getName());
            assertEquals(TarConstants.LF_NORMAL, e.getLinkFlag());
            assertTrue(e.isFile());
            assertEquals(toFileTime("2022-03-17T00:38:20.470751500Z"), e.getLastModifiedTime(), "mtime");
            assertEquals(toFileTime("2022-03-17T00:38:20.536752000Z"), e.getLastAccessTime(), "atime");
            assertEquals(toFileTime("2022-03-17T00:38:20.470751500Z"), e.getStatusChangeTime(), "ctime");
            assertNull(e.getCreationTime(), "birthtime");
            assertNull(tin.getNextTarEntry());
        }
    }

    // 'xstar' format without tar signature
    @Test
    void testReadTimeFromTarXustarIncremental() throws Exception {
        final String file = "COMPRESS-612/test-times-xustar-incremental.tar";
        try (InputStream in = new BufferedInputStream(Files.newInputStream(getPath(file)));
                TarArchiveInputStream tin = new TarArchiveInputStream(in)) {
            TarArchiveEntry e = tin.getNextTarEntry();
            assertNotNull(e);
            assertTrue(e.getExtraPaxHeaders().isEmpty());
            assertEquals("test-times.txt", e.getName(), "name");
            assertEquals(TarConstants.LF_NORMAL, e.getLinkFlag());
            assertEquals(toFileTime("2022-03-17T00:38:20.470751500Z"), e.getLastModifiedTime(), "mtime");
            assertEquals(toFileTime("2022-03-17T00:38:20.536752000Z"), e.getLastAccessTime(), "atime");
            assertEquals(toFileTime("2022-03-17T00:38:20.470751500Z"), e.getStatusChangeTime(), "ctime");
            assertNull(e.getCreationTime(), "birthtime");
            e = tin.getNextTarEntry();
            assertNotNull(e);
            assertTrue(e.getExtraPaxHeaders().isEmpty());
            assertEquals("test-times.txt", e.getName(), "name");
            assertEquals(TarConstants.LF_NORMAL, e.getLinkFlag());
            assertEquals(toFileTime("2022-03-17T01:52:25.592262900Z"), e.getLastModifiedTime(), "mtime");
            assertEquals(toFileTime("2022-03-17T01:52:25.724278500Z"), e.getLastAccessTime(), "atime");
            assertEquals(toFileTime("2022-03-17T01:52:25.592262900Z"), e.getStatusChangeTime(), "ctime");
            assertNull(e.getCreationTime(), "birthtime");
            assertNull(tin.getNextTarEntry());
        }
    }

    private FileTime toFileTime(final String text) {
        return FileTime.from(Instant.parse(text));
    }
}
