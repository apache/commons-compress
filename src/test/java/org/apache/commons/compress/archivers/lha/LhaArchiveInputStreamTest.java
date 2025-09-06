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

package org.apache.commons.compress.archivers.lha;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

class LhaArchiveInputStreamTest extends AbstractTest {
    private static final int[] VALID_HEADER_LEVEL_0_FILE = new int[] {
        0x2b, 0x70, 0x2d, 0x6c, 0x68, 0x35, 0x2d, 0x34, 0x00, 0x00, 0x00, 0x39, 0x00, 0x00, 0x00, 0x4b, // |+p-lh5-4...9...K|
        0x80, 0x03, 0x5b, 0x20, 0x00, 0x09, 0x74, 0x65, 0x73, 0x74, 0x31, 0x2e, 0x74, 0x78, 0x74, 0x96, // |..[ ..test1.txt.|
        0x64, 0x55, 0x00, 0xef, 0x6b, 0x8f, 0x68, 0xa4, 0x81, 0xf5, 0x01, 0x14, 0x00, 0x00, 0x39, 0x4a, // |dU..k.h.......9J|
        0x8e, 0x8d, 0x33, 0xb7, 0x3e, 0x80, 0x1f, 0xe8, 0x4d, 0x01, 0x3a, 0x00, 0x12, 0xb4, 0xc7, 0x83, // |..3.>...M.:.....|
        0x5a, 0x8d, 0xf4, 0x03, 0xe9, 0xe3, 0xc0, 0x3b, 0xae, 0xc0, 0xc4, 0xe6, 0x78, 0x28, 0xa1, 0x78, // |Z......;....x(.x|
        0x75, 0x60, 0xd3, 0xaa, 0x76, 0x4e, 0xbb, 0xc1, 0x7c, 0x1d, 0x9a, 0x63, 0xaf, 0xc3, 0xe4, 0xaf, // |u`..vN..|..c....|
        0x7c, 0x00                                                                                      // ||.|
    };

    private static final int[] VALID_HEADER_LEVEL_0_FILE_MACOS_UTF8 = new int[] {
        0x31, 0x65, 0x2d, 0x6c, 0x68, 0x30, 0x2d, 0x0d, 0x00, 0x00, 0x00, 0x0d, 0x00, 0x00, 0x00, 0x06, // |1e-lh0-.........|
        0x8c, 0x0d, 0x5b, 0x20, 0x00, 0x0f, 0x74, 0x65, 0x73, 0x74, 0x2d, 0xc3, 0xa5, 0xc3, 0xa4, 0xc3, // |..[ ..test-.....|
        0xb6, 0x2e, 0x74, 0x78, 0x74, 0x57, 0x77, 0x55, 0x00, 0xfc, 0xaf, 0x9c, 0x68, 0xa4, 0x81, 0xf5, // |..txtWwU....h...|
        0x01, 0x14, 0x00, 0x48, 0x65, 0x6c, 0x6c, 0x6f, 0x20, 0x57, 0x6f, 0x72, 0x6c, 0x64, 0x21, 0x0a, // |...Hello World!.|
        0x00                                                                                            // |.|
    };

    private static final int[] VALID_HEADER_LEVEL_0_FILE_MSDOS_ISO8859_1 = new int[] {
        0x22, 0x6b, 0x2d, 0x6c, 0x68, 0x30, 0x2d, 0x0e, 0x00, 0x00, 0x00, 0x0e, 0x00, 0x00, 0x00, 0x52, // |"k-lh0-........R|
        0x54, 0x0d, 0x5b, 0x20, 0x00, 0x0c, 0x74, 0x65, 0x73, 0x74, 0x2d, 0xe5, 0xe4, 0xf6, 0x2e, 0x74, // |T.[ ..test-....t|
        0x78, 0x74, 0xb4, 0xc9, 0x48, 0x65, 0x6c, 0x6c, 0x6f, 0x20, 0x57, 0x6f, 0x72, 0x6c, 0x64, 0x21, // |xt..Hello World!|
        0x0d, 0x0a, 0x00                                                                                // |...|
    };

    private static final int[] VALID_HEADER_LEVEL_1_FILE = new int[] {
        0x22, 0x09, 0x2d, 0x6c, 0x68, 0x35, 0x2d, 0x47, 0x00, 0x00, 0x00, 0x39, 0x00, 0x00, 0x00, 0x4b, // |".-lh5-G...9...K|
        0x80, 0x03, 0x5b, 0x20, 0x01, 0x09, 0x74, 0x65, 0x73, 0x74, 0x31, 0x2e, 0x74, 0x78, 0x74, 0x96, // |..[ ..test1.txt.|
        0x64, 0x55, 0x05, 0x00, 0x50, 0xa4, 0x81, 0x07, 0x00, 0x51, 0x14, 0x00, 0xf5, 0x01, 0x07, 0x00, // |dU..P....Q......|
        0x54, 0xef, 0x6b, 0x8f, 0x68, 0x00, 0x00, 0x00, 0x39, 0x4a, 0x8e, 0x8d, 0x33, 0xb7, 0x3e, 0x80, // |T.k.h...9J..3.>.|
        0x1f, 0xe8, 0x4d, 0x01, 0x3a, 0x00, 0x12, 0xb4, 0xc7, 0x83, 0x5a, 0x8d, 0xf4, 0x03, 0xe9, 0xe3, // |..M.:.....Z.....|
        0xc0, 0x3b, 0xae, 0xc0, 0xc4, 0xe6, 0x78, 0x28, 0xa1, 0x78, 0x75, 0x60, 0xd3, 0xaa, 0x76, 0x4e, // |.;....x(.xu`..vN|
        0xbb, 0xc1, 0x7c, 0x1d, 0x9a, 0x63, 0xaf, 0xc3, 0xe4, 0xaf, 0x7c, 0x00                          // |..|..c....|.|
    };

    private static final int[] VALID_HEADER_LEVEL_1_FILE_MSDOS_WITH_CHECKSUM_AND_CRC = new int[] {
        0x19, 0x36, 0x2d, 0x6c, 0x68, 0x64, 0x2d, 0x12, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x3d, // |.6-lhd-........=|
        0x77, 0x0d, 0x5b, 0x20, 0x01, 0x00, 0x00, 0x00, 0x4d, 0x08, 0x00, 0x02, 0x64, 0x69, 0x72, 0x31, // |w.[ ....M...dir1|
        0xff, 0x05, 0x00, 0x40, 0x10, 0x00, 0x05, 0x00, 0x00, 0x72, 0xb7, 0x00, 0x00, 0x22, 0x10, 0x2d, // |...@.....r...".-|
        0x6c, 0x68, 0x30, 0x2d, 0x1b, 0x00, 0x00, 0x00, 0x0e, 0x00, 0x00, 0x00, 0x52, 0x54, 0x0d, 0x5b, // |lh0-........RT.[|
        0x20, 0x01, 0x09, 0x74, 0x65, 0x73, 0x74, 0x31, 0x2e, 0x74, 0x78, 0x74, 0xb4, 0xc9, 0x4d, 0x08, // | ..test1.txt..M.|
        0x00, 0x02, 0x64, 0x69, 0x72, 0x31, 0xff, 0x05, 0x00, 0x00, 0x71, 0x9b, 0x00, 0x00, 0x48, 0x65, // |..dir1....q...He|
        0x6c, 0x6c, 0x6f, 0x20, 0x57, 0x6f, 0x72, 0x6c, 0x64, 0x21, 0x0d, 0x0a, 0x00                    // |llo World!...|
    };

    private static final int[] VALID_HEADER_LEVEL_2_FILE = new int[] {
        0x37, 0x00, 0x2d, 0x6c, 0x68, 0x35, 0x2d, 0x34, 0x00, 0x00, 0x00, 0x39, 0x00, 0x00, 0x00, 0xef, // |7.-lh5-4...9....|
        0x6b, 0x8f, 0x68, 0x20, 0x02, 0x96, 0x64, 0x55, 0x05, 0x00, 0x00, 0xa5, 0x01, 0x0c, 0x00, 0x01, // |k.h ..dU........|
        0x74, 0x65, 0x73, 0x74, 0x31, 0x2e, 0x74, 0x78, 0x74, 0x05, 0x00, 0x50, 0xa4, 0x81, 0x07, 0x00, // |test1.txt..P....|
        0x51, 0x14, 0x00, 0xf5, 0x01, 0x00, 0x00, 0x00, 0x39, 0x4a, 0x8e, 0x8d, 0x33, 0xb7, 0x3e, 0x80, // |Q.......9J..3.>.|
        0x1f, 0xe8, 0x4d, 0x01, 0x3a, 0x00, 0x12, 0xb4, 0xc7, 0x83, 0x5a, 0x8d, 0xf4, 0x03, 0xe9, 0xe3, // |..M.:.....Z.....|
        0xc0, 0x3b, 0xae, 0xc0, 0xc4, 0xe6, 0x78, 0x28, 0xa1, 0x78, 0x75, 0x60, 0xd3, 0xaa, 0x76, 0x4e, // |.;....x(.xu`..vN|
        0xbb, 0xc1, 0x7c, 0x1d, 0x9a, 0x63, 0xaf, 0xc3, 0xe4, 0xaf, 0x7c, 0x00                          // |..|..c....|.|
    };

    @Test
    void testInvalidHeaderLevelLength() throws IOException {
        final byte[] data = new byte[] { 0x04, 0x00, 0x00, 0x00, 0x00, 0x00 };

        try (LhaArchiveInputStream archive = LhaArchiveInputStream.builder().setInputStream(new ByteArrayInputStream(data)).get()) {
            archive.getNextEntry();
            fail("Expected ArchiveException for invalid header length");
        } catch (ArchiveException e) {
            assertEquals("Invalid header length", e.getMessage());
        }
    }

    @Test
    void testInvalidHeaderLevel() throws IOException {
        final byte[] data = toByteArray(VALID_HEADER_LEVEL_0_FILE);

        data[20] = 4; // Change the header level to an invalid value

        try (LhaArchiveInputStream archive = LhaArchiveInputStream.builder().setInputStream(new ByteArrayInputStream(data)).get()) {
            archive.getNextEntry();
            fail("Expected ArchiveException for invalid header level");
        } catch (ArchiveException e) {
            assertEquals("Invalid header level: 4", e.getMessage());
        }
    }

    @Test
    void testUnsupportedCompressionMethod() throws IOException {
        final byte[] data = toByteArray(VALID_HEADER_LEVEL_0_FILE);

        data[1] = (byte) 0x9c; // Change the header checksum
        data[5] = 'a'; // Change the compression method to an unsupported value

        try (LhaArchiveInputStream archive = LhaArchiveInputStream.builder().setInputStream(new ByteArrayInputStream(data)).get()) {
            final LhaArchiveEntry entry = archive.getNextEntry();
            assertNotNull(entry);
            assertEquals("-lha-", entry.getCompressionMethod());

            assertFalse(archive.canReadEntryData(entry));

            try {
                IOUtils.toByteArray(archive);
                fail("Expected ArchiveException for unsupported compression method");
            } catch (ArchiveException e) {
                assertEquals("Unsupported compression method: -lha-", e.getMessage());
            }
        }
    }

    @Test
    void testReadDataBeforeEntry() throws IOException {
        final byte[] data = toByteArray(VALID_HEADER_LEVEL_0_FILE);

        try (LhaArchiveInputStream archive = LhaArchiveInputStream.builder().setInputStream(new ByteArrayInputStream(data)).get()) {
            try {
                IOUtils.toByteArray(archive);
                fail("Expected IllegalStateException for reading data before entry");
            } catch (IllegalStateException e) {
                assertEquals("No current entry", e.getMessage());
            }
        }
    }

    @Test
    void testParseHeaderLevel0File() throws IOException {
        try (LhaArchiveInputStream archive = LhaArchiveInputStream.builder()
                .setInputStream(new ByteArrayInputStream(toByteArray(VALID_HEADER_LEVEL_0_FILE)))
                .get()) {

            // Entry should be parsed correctly
            final LhaArchiveEntry entry = archive.getNextEntry();
            assertNotNull(entry);
            assertEquals("test1.txt", entry.getName());
            assertFalse(entry.isDirectory());
            assertEquals(57, entry.getSize());
            assertEquals(1754236942000L, convertSystemTimeZoneDateToUTC(entry.getLastModifiedDate()).toInstant().toEpochMilli());
            assertEquals(ZonedDateTime.parse("2025-08-03T16:02:22Z"), convertSystemTimeZoneDateToUTC(entry.getLastModifiedDate()));
            assertEquals(52, entry.getCompressedSize());
            assertEquals("-lh5-", entry.getCompressionMethod());
            assertEquals(0x6496, entry.getCrcValue());
            assertNull(entry.getOsId());
            assertNull(entry.getUnixPermissionMode());
            assertNull(entry.getUnixGroupId());
            assertNull(entry.getUnixUserId());
            assertNull(entry.getMsdosFileAttributes());
            assertNull(entry.getHeaderCrc());

            // No more entries expected
            assertNull(archive.getNextEntry());
        }
    }

    @Test
    void testParseHeaderLevel0FileMacosUtf8() throws IOException {
        try (LhaArchiveInputStream archive = LhaArchiveInputStream.builder()
                .setInputStream(new ByteArrayInputStream(toByteArray(VALID_HEADER_LEVEL_0_FILE_MACOS_UTF8)))
                .setCharset(StandardCharsets.UTF_8)
                .get()) {

            // Entry name should be parsed correctly
            final LhaArchiveEntry entry = archive.getNextEntry();
            assertNotNull(entry);
            assertEquals("test-\u00E5\u00E4\u00F6.txt", entry.getName());

            // No more entries expected
            assertNull(archive.getNextEntry());
        }
    }

    @Test
    void testParseHeaderLevel0FileMsdosIso88591() throws IOException {
        try (LhaArchiveInputStream archive = LhaArchiveInputStream.builder()
                .setInputStream(new ByteArrayInputStream(toByteArray(VALID_HEADER_LEVEL_0_FILE_MSDOS_ISO8859_1)))
                .setCharset(StandardCharsets.ISO_8859_1)
                .get()) {

            // Entry name should be parsed correctly
            final LhaArchiveEntry entry = archive.getNextEntry();
            assertNotNull(entry);
            assertEquals("test-\u00E5\u00E4\u00F6.txt", entry.getName());

            // No more entries expected
            assertNull(archive.getNextEntry());
        }
    }

    @Test
    void testParseHeaderLevel0FileMsdosIso88591DefaultEncoding() throws IOException {
        try (LhaArchiveInputStream archive = LhaArchiveInputStream.builder()
                .setInputStream(new ByteArrayInputStream(toByteArray(VALID_HEADER_LEVEL_0_FILE_MSDOS_ISO8859_1)))
                .get()) {

            // First entry should be with replacement characters for unsupported characters
            final LhaArchiveEntry entry = archive.getNextEntry();
            assertNotNull(entry);
            assertEquals("test-\uFFFD\uFFFD\uFFFD.txt", entry.getName()); // Unicode replacement characters for unsupported characters

            // No more entries expected
            assertNull(archive.getNextEntry());
        }
    }

    @Test
    void testInvalidHeaderLevel0Length() throws IOException {
        final byte[] data = toByteArray(VALID_HEADER_LEVEL_0_FILE);

        data[0] = 0x10; // Change the first byte to an invalid length

        try (LhaArchiveInputStream archive = LhaArchiveInputStream.builder().setInputStream(new ByteArrayInputStream(data)).get()) {
            archive.getNextEntry();
            fail("Expected ArchiveException for invalid header length");
        } catch (ArchiveException e) {
            assertEquals("Invalid header level 0 length: 16", e.getMessage());
        }
    }

    @Test
    void testInvalidHeaderLevel0Checksum() throws IOException {
        final byte[] data = toByteArray(VALID_HEADER_LEVEL_0_FILE);

        data[1] = 0x55; // Change the second byte to an invalid header checksum

        try (LhaArchiveInputStream archive = LhaArchiveInputStream.builder().setInputStream(new ByteArrayInputStream(data)).get()) {
            archive.getNextEntry();
            fail("Expected ArchiveException for invalid header checksum");
        } catch (ArchiveException e) {
            assertEquals("Invalid header level 0 checksum", e.getMessage());
        }
    }

    @Test
    void testParseHeaderLevel0FileWithFoldersMacos() throws IOException {
        // The lha file was generated by LHa for UNIX version 1.14i-ac20211125 for Macos
        try (LhaArchiveInputStream archive = LhaArchiveInputStream.builder()
                .setInputStream(newInputStream("test-macos-l0.lha"))
                .setFileSeparatorChar('/')
                .get()) {

            LhaArchiveEntry entry;

            // Check directory entry
            entry = archive.getNextEntry();
            assertNotNull(entry);
            assertEquals("dir1/", entry.getName());
            assertTrue(entry.isDirectory());
            assertEquals(0, entry.getSize());
            assertEquals(1755090690000L, convertSystemTimeZoneDateToUTC(entry.getLastModifiedDate()).toInstant().toEpochMilli());
            assertEquals(ZonedDateTime.parse("2025-08-13T13:11:30Z"), convertSystemTimeZoneDateToUTC(entry.getLastModifiedDate()));
            assertEquals(0, entry.getCompressedSize());
            assertEquals("-lhd-", entry.getCompressionMethod());
            assertEquals(0x0000, entry.getCrcValue());
            assertNull(entry.getOsId());
            assertNull(entry.getUnixPermissionMode());
            assertNull(entry.getUnixGroupId());
            assertNull(entry.getUnixUserId());
            assertNull(entry.getMsdosFileAttributes());
            assertNull(entry.getHeaderCrc());

            // Check directory entry
            entry = archive.getNextEntry();
            assertNotNull(entry);
            assertEquals("dir1/dir1-1/", entry.getName());
            assertTrue(entry.isDirectory());
            assertEquals(0, entry.getSize());
            assertEquals(1755090728000L, convertSystemTimeZoneDateToUTC(entry.getLastModifiedDate()).toInstant().toEpochMilli());
            assertEquals(ZonedDateTime.parse("2025-08-13T13:12:08Z"), convertSystemTimeZoneDateToUTC(entry.getLastModifiedDate()));
            assertEquals(0, entry.getCompressedSize());
            assertEquals("-lhd-", entry.getCompressionMethod());
            assertEquals(0x0000, entry.getCrcValue());
            assertNull(entry.getOsId());
            assertNull(entry.getUnixPermissionMode());
            assertNull(entry.getUnixGroupId());
            assertNull(entry.getUnixUserId());
            assertNull(entry.getMsdosFileAttributes());
            assertNull(entry.getHeaderCrc());

            // Check file entry
            entry = archive.getNextEntry();
            assertNotNull(entry);
            assertEquals("dir1/dir1-1/test1.txt", entry.getName());
            assertFalse(entry.isDirectory());
            assertEquals(13, entry.getSize());
            assertEquals(1755090728000L, convertSystemTimeZoneDateToUTC(entry.getLastModifiedDate()).toInstant().toEpochMilli());
            assertEquals(ZonedDateTime.parse("2025-08-13T13:12:08Z"), convertSystemTimeZoneDateToUTC(entry.getLastModifiedDate()));
            assertEquals(13, entry.getCompressedSize());
            assertEquals("-lh0-", entry.getCompressionMethod());
            assertEquals(0x7757, entry.getCrcValue());
            assertNull(entry.getOsId());
            assertNull(entry.getUnixPermissionMode());
            assertNull(entry.getUnixGroupId());
            assertNull(entry.getUnixUserId());
            assertNull(entry.getMsdosFileAttributes());
            assertNull(entry.getHeaderCrc());

            // Check directory entry
            entry = archive.getNextEntry();
            assertNotNull(entry);
            assertEquals("dir1/dir1-2/", entry.getName());
            assertTrue(entry.isDirectory());
            assertEquals(0, entry.getSize());
            assertEquals(1755090812000L, convertSystemTimeZoneDateToUTC(entry.getLastModifiedDate()).toInstant().toEpochMilli());
            assertEquals(ZonedDateTime.parse("2025-08-13T13:13:32Z"), convertSystemTimeZoneDateToUTC(entry.getLastModifiedDate()));
            assertEquals(0, entry.getCompressedSize());
            assertEquals("-lhd-", entry.getCompressionMethod());
            assertEquals(0x0000, entry.getCrcValue());
            assertNull(entry.getOsId());
            assertNull(entry.getUnixPermissionMode());
            assertNull(entry.getUnixGroupId());
            assertNull(entry.getUnixUserId());
            assertNull(entry.getMsdosFileAttributes());
            assertNull(entry.getHeaderCrc());

            // Check file entry
            entry = archive.getNextEntry();
            assertNotNull(entry);
            assertEquals("dir1/dir1-2/test2.txt", entry.getName());
            assertFalse(entry.isDirectory());
            assertEquals(13, entry.getSize());
            assertEquals(1755090812000L, convertSystemTimeZoneDateToUTC(entry.getLastModifiedDate()).toInstant().toEpochMilli());
            assertEquals(ZonedDateTime.parse("2025-08-13T13:13:32Z"), convertSystemTimeZoneDateToUTC(entry.getLastModifiedDate()));
            assertEquals(13, entry.getCompressedSize());
            assertEquals("-lh0-", entry.getCompressionMethod());
            assertEquals(0x7757, entry.getCrcValue());
            assertNull(entry.getOsId());
            assertNull(entry.getUnixPermissionMode());
            assertNull(entry.getUnixGroupId());
            assertNull(entry.getUnixUserId());
            assertNull(entry.getMsdosFileAttributes());
            assertNull(entry.getHeaderCrc());

            // No more entries expected
            assertNull(archive.getNextEntry());
        }
    }

    @Test
    void testParseHeaderLevel0FileWithFoldersMsdos() throws IOException {
        // The lha file was generated by LHA32 v2.67.00 for Windows
        try (LhaArchiveInputStream archive = LhaArchiveInputStream.builder()
                .setInputStream(newInputStream("test-msdos-l0.lha"))
                .setFileSeparatorChar('/')
                .get()) {
            LhaArchiveEntry entry;

            // Check directory entry
            entry = archive.getNextEntry();
            assertNotNull(entry);
            assertEquals("dir1/", entry.getName());
            assertTrue(entry.isDirectory());
            assertEquals(0, entry.getSize());
            assertEquals(1755081308000L, convertSystemTimeZoneDateToUTC(entry.getLastModifiedDate()).toInstant().toEpochMilli());
            assertEquals(ZonedDateTime.parse("2025-08-13T10:35:08Z"), convertSystemTimeZoneDateToUTC(entry.getLastModifiedDate()));
            assertEquals(0, entry.getCompressedSize());
            assertEquals("-lhd-", entry.getCompressionMethod());
            assertEquals(0x0000, entry.getCrcValue());
            assertNull(entry.getOsId());
            assertNull(entry.getUnixPermissionMode());
            assertNull(entry.getUnixGroupId());
            assertNull(entry.getUnixUserId());
            assertNull(entry.getMsdosFileAttributes());
            assertNull(entry.getHeaderCrc());

            // Check directory entry
            entry = archive.getNextEntry();
            assertNotNull(entry);
            assertEquals("dir1/dir1-1/", entry.getName());
            assertTrue(entry.isDirectory());
            assertEquals(0, entry.getSize());
            assertEquals(1755081336000L, convertSystemTimeZoneDateToUTC(entry.getLastModifiedDate()).toInstant().toEpochMilli());
            assertEquals(ZonedDateTime.parse("2025-08-13T10:35:36Z"), convertSystemTimeZoneDateToUTC(entry.getLastModifiedDate()));
            assertEquals(0, entry.getCompressedSize());
            assertEquals("-lhd-", entry.getCompressionMethod());
            assertEquals(0x0000, entry.getCrcValue());
            assertNull(entry.getOsId());
            assertNull(entry.getUnixPermissionMode());
            assertNull(entry.getUnixGroupId());
            assertNull(entry.getUnixUserId());
            assertNull(entry.getMsdosFileAttributes());
            assertNull(entry.getHeaderCrc());

            // Check file entry
            entry = archive.getNextEntry();
            assertNotNull(entry);
            assertEquals("dir1/dir1-1/test1.txt", entry.getName());
            assertFalse(entry.isDirectory());
            assertEquals(14, entry.getSize());
            assertEquals(1755081276000L, convertSystemTimeZoneDateToUTC(entry.getLastModifiedDate()).toInstant().toEpochMilli());
            assertEquals(ZonedDateTime.parse("2025-08-13T10:34:36Z"), convertSystemTimeZoneDateToUTC(entry.getLastModifiedDate()));
            assertEquals(14, entry.getCompressedSize());
            assertEquals("-lh0-", entry.getCompressionMethod());
            assertEquals(0xc9b4, entry.getCrcValue());
            assertNull(entry.getOsId());
            assertNull(entry.getUnixPermissionMode());
            assertNull(entry.getUnixGroupId());
            assertNull(entry.getUnixUserId());
            assertNull(entry.getMsdosFileAttributes());
            assertNull(entry.getHeaderCrc());

            // Check directory entry
            entry = archive.getNextEntry();
            assertNotNull(entry);
            assertEquals("dir1/dir1-2/", entry.getName());
            assertTrue(entry.isDirectory());
            assertEquals(0, entry.getSize());
            assertEquals(1755081340000L, convertSystemTimeZoneDateToUTC(entry.getLastModifiedDate()).toInstant().toEpochMilli());
            assertEquals(ZonedDateTime.parse("2025-08-13T10:35:40Z"), convertSystemTimeZoneDateToUTC(entry.getLastModifiedDate()));
            assertEquals(0, entry.getCompressedSize());
            assertEquals("-lhd-", entry.getCompressionMethod());
            assertEquals(0x0000, entry.getCrcValue());
            assertNull(entry.getOsId());
            assertNull(entry.getUnixPermissionMode());
            assertNull(entry.getUnixGroupId());
            assertNull(entry.getUnixUserId());
            assertNull(entry.getMsdosFileAttributes());
            assertNull(entry.getHeaderCrc());

            // Check file entry
            entry = archive.getNextEntry();
            assertNotNull(entry);
            assertEquals("dir1/dir1-2/test2.txt", entry.getName());
            assertFalse(entry.isDirectory());
            assertEquals(14, entry.getSize());
            assertEquals(1755081276000L, convertSystemTimeZoneDateToUTC(entry.getLastModifiedDate()).toInstant().toEpochMilli());
            assertEquals(ZonedDateTime.parse("2025-08-13T10:34:36Z"), convertSystemTimeZoneDateToUTC(entry.getLastModifiedDate()));
            assertEquals(14, entry.getCompressedSize());
            assertEquals("-lh0-", entry.getCompressionMethod());
            assertEquals(0xc9b4, entry.getCrcValue());
            assertNull(entry.getOsId());
            assertNull(entry.getUnixPermissionMode());
            assertNull(entry.getUnixGroupId());
            assertNull(entry.getUnixUserId());
            assertNull(entry.getMsdosFileAttributes());
            assertNull(entry.getHeaderCrc());

            // No more entries expected
            assertNull(archive.getNextEntry());
        }
    }

    @Test
    void testParseHeaderLevel1File() throws IOException {
        try (LhaArchiveInputStream archive = LhaArchiveInputStream.builder()
                .setInputStream(new ByteArrayInputStream(toByteArray(VALID_HEADER_LEVEL_1_FILE)))
                .get()) {

            // Entry should be parsed correctly
            final LhaArchiveEntry entry = archive.getNextEntry();
            assertNotNull(entry);
            assertEquals("test1.txt", entry.getName());
            assertFalse(entry.isDirectory());
            assertEquals(57, entry.getSize());
            assertEquals(1754229743000L, entry.getLastModifiedDate().getTime());
            assertEquals(ZonedDateTime.parse("2025-08-03T14:02:23Z"), entry.getLastModifiedDate().toInstant().atZone(ZoneOffset.UTC));
            assertEquals(52, entry.getCompressedSize());
            assertEquals("-lh5-", entry.getCompressionMethod());
            assertEquals(0x6496, entry.getCrcValue());
            assertEquals(85, entry.getOsId());
            assertEquals(0100644, entry.getUnixPermissionMode());
            assertEquals(20, entry.getUnixGroupId());
            assertEquals(501, entry.getUnixUserId());
            assertNull(entry.getMsdosFileAttributes());
            assertNull(entry.getHeaderCrc());

            // No more entries expected
            assertNull(archive.getNextEntry());
        }
    }

    @Test
    void testParseHeaderLevel1FileMsdosChecksumAndCrc() throws IOException {
        // The lha file was generated by LHA32 v2.67.00 for Windows
        try (LhaArchiveInputStream archive = LhaArchiveInputStream.builder()
                .setInputStream(new ByteArrayInputStream(toByteArray(VALID_HEADER_LEVEL_1_FILE_MSDOS_WITH_CHECKSUM_AND_CRC)))
                .setFileSeparatorChar('/')
                .get()) {

            LhaArchiveEntry entry;

            // Check directory entry
            entry = archive.getNextEntry();
            assertNotNull(entry);
            assertEquals("dir1/", entry.getName());
            assertTrue(entry.isDirectory());
            assertEquals(0, entry.getSize());
            assertEquals(1755097078000L, convertSystemTimeZoneDateToUTC(entry.getLastModifiedDate()).toInstant().toEpochMilli());
            assertEquals(ZonedDateTime.parse("2025-08-13T14:57:58Z"), convertSystemTimeZoneDateToUTC(entry.getLastModifiedDate()));
            assertEquals(0, entry.getCompressedSize());
            assertEquals("-lhd-", entry.getCompressionMethod());
            assertEquals(0x0000, entry.getCrcValue());
            assertEquals(77, entry.getOsId());
            assertNull(entry.getUnixPermissionMode());
            assertNull(entry.getUnixGroupId());
            assertNull(entry.getUnixUserId());
            assertEquals(0x0010, entry.getMsdosFileAttributes());
            assertEquals(0xb772, entry.getHeaderCrc());

            // Check file entry
            entry = archive.getNextEntry();
            assertNotNull(entry);
            assertEquals("dir1/test1.txt", entry.getName());
            assertFalse(entry.isDirectory());
            assertEquals(14, entry.getSize());
            assertEquals(1755081276000L, convertSystemTimeZoneDateToUTC(entry.getLastModifiedDate()).toInstant().toEpochMilli());
            assertEquals(ZonedDateTime.parse("2025-08-13T10:34:36Z"), convertSystemTimeZoneDateToUTC(entry.getLastModifiedDate()));
            assertEquals(14, entry.getCompressedSize());
            assertEquals("-lh0-", entry.getCompressionMethod());
            assertEquals(0xc9b4, entry.getCrcValue());
            assertEquals(77, entry.getOsId());
            assertNull(entry.getUnixPermissionMode());
            assertNull(entry.getUnixGroupId());
            assertNull(entry.getUnixUserId());
            assertNull(entry.getMsdosFileAttributes());
            assertEquals(0x9b71, entry.getHeaderCrc());

            // No more entries expected
            assertNull(archive.getNextEntry());
        }
    }

    @Test
    void testInvalidHeaderLevel1Length() throws IOException {
        final byte[] data = toByteArray(VALID_HEADER_LEVEL_1_FILE);

        data[0] = 0x10; // Change the first byte to an invalid length

        try (LhaArchiveInputStream archive = LhaArchiveInputStream.builder().setInputStream(new ByteArrayInputStream(data)).get()) {
            archive.getNextEntry();
            fail("Expected ArchiveException for invalid header length");
        } catch (ArchiveException e) {
            assertEquals("Invalid header level 1 length: 16", e.getMessage());
        }
    }

    @Test
    void testInvalidHeaderLevel1Checksum() throws IOException {
        final byte[] data = toByteArray(VALID_HEADER_LEVEL_1_FILE);

        data[1] = 0x55; // Change the second byte to an invalid header checksum

        try (LhaArchiveInputStream archive = LhaArchiveInputStream.builder().setInputStream(new ByteArrayInputStream(data)).get()) {
            archive.getNextEntry();
            fail("Expected ArchiveException for invalid header checksum");
        } catch (ArchiveException e) {
            assertEquals("Invalid header level 1 checksum", e.getMessage());
        }
    }

    @Test
    void testInvalidHeaderLevel1Crc() throws IOException {
        final byte[] data = toByteArray(VALID_HEADER_LEVEL_1_FILE_MSDOS_WITH_CHECKSUM_AND_CRC);

        // Change header CRC to an invalid value
        data[41] = 0x33;
        data[42] = 0x22;

        try (LhaArchiveInputStream archive = LhaArchiveInputStream.builder().setInputStream(new ByteArrayInputStream(data)).get()) {
            archive.getNextEntry();
            fail("Expected ArchiveException for invalid header checksum");
        } catch (ArchiveException e) {
            assertEquals("Invalid header CRC expected=0xb772 found=0x2233", e.getMessage());
        }
    }

    @Test
    void testParseHeaderLevel1FileWithFoldersMacos() throws IOException {
        // The lha file was generated by LHa for UNIX version 1.14i-ac20211125 for Macos
        try (LhaArchiveInputStream archive = LhaArchiveInputStream.builder()
                .setInputStream(newInputStream("test-macos-l1.lha"))
                .setFileSeparatorChar('/')
                .get()) {

            LhaArchiveEntry entry;

            // Check directory entry
            entry = archive.getNextEntry();
            assertNotNull(entry);
            assertEquals("dir1/", entry.getName());
            assertTrue(entry.isDirectory());
            assertEquals(0, entry.getSize());
            assertEquals(1755083490000L, entry.getLastModifiedDate().getTime());
            assertEquals(ZonedDateTime.parse("2025-08-13T11:11:30Z"), entry.getLastModifiedDate().toInstant().atZone(ZoneOffset.UTC));
            assertEquals(0, entry.getCompressedSize());
            assertEquals("-lhd-", entry.getCompressionMethod());
            assertEquals(0x0000, entry.getCrcValue());
            assertEquals(85, entry.getOsId());
            assertEquals(040755, entry.getUnixPermissionMode());
            assertEquals(20, entry.getUnixGroupId());
            assertEquals(501, entry.getUnixUserId());
            assertNull(entry.getMsdosFileAttributes());
            assertNull(entry.getHeaderCrc());

            // Check directory entry
            entry = archive.getNextEntry();
            assertNotNull(entry);
            assertEquals("dir1/dir1-1/", entry.getName());
            assertTrue(entry.isDirectory());
            assertEquals(0, entry.getSize());
            assertEquals(1755083529000L, entry.getLastModifiedDate().getTime());
            assertEquals(ZonedDateTime.parse("2025-08-13T11:12:09Z"), entry.getLastModifiedDate().toInstant().atZone(ZoneOffset.UTC));
            assertEquals(0, entry.getCompressedSize());
            assertEquals("-lhd-", entry.getCompressionMethod());
            assertEquals(0x0000, entry.getCrcValue());
            assertEquals(85, entry.getOsId());
            assertEquals(040755, entry.getUnixPermissionMode());
            assertEquals(20, entry.getUnixGroupId());
            assertEquals(501, entry.getUnixUserId());
            assertNull(entry.getMsdosFileAttributes());
            assertNull(entry.getHeaderCrc());

            // Check file entry
            entry = archive.getNextEntry();
            assertNotNull(entry);
            assertEquals("dir1/dir1-1/test1.txt", entry.getName());
            assertFalse(entry.isDirectory());
            assertEquals(13, entry.getSize());
            assertEquals(1755083529000L, entry.getLastModifiedDate().getTime());
            assertEquals(ZonedDateTime.parse("2025-08-13T11:12:09Z"), entry.getLastModifiedDate().toInstant().atZone(ZoneOffset.UTC));
            assertEquals(13, entry.getCompressedSize());
            assertEquals("-lh0-", entry.getCompressionMethod());
            assertEquals(0x7757, entry.getCrcValue());
            assertEquals(85, entry.getOsId());
            assertEquals(0100644, entry.getUnixPermissionMode());
            assertEquals(20, entry.getUnixGroupId());
            assertEquals(501, entry.getUnixUserId());
            assertNull(entry.getMsdosFileAttributes());
            assertNull(entry.getHeaderCrc());

            // Check directory entry
            entry = archive.getNextEntry();
            assertNotNull(entry);
            assertEquals("dir1/dir1-2/", entry.getName());
            assertTrue(entry.isDirectory());
            assertEquals(0, entry.getSize());
            assertEquals(1755083612000L, entry.getLastModifiedDate().getTime());
            assertEquals(ZonedDateTime.parse("2025-08-13T11:13:32Z"), entry.getLastModifiedDate().toInstant().atZone(ZoneOffset.UTC));
            assertEquals(0, entry.getCompressedSize());
            assertEquals("-lhd-", entry.getCompressionMethod());
            assertEquals(0x0000, entry.getCrcValue());
            assertEquals(85, entry.getOsId());
            assertEquals(040755, entry.getUnixPermissionMode());
            assertEquals(20, entry.getUnixGroupId());
            assertEquals(501, entry.getUnixUserId());
            assertNull(entry.getMsdosFileAttributes());
            assertNull(entry.getHeaderCrc());

            // Check file entry
            entry = archive.getNextEntry();
            assertNotNull(entry);
            assertEquals("dir1/dir1-2/test2.txt", entry.getName());
            assertFalse(entry.isDirectory());
            assertEquals(13, entry.getSize());
            assertEquals(1755083612000L, entry.getLastModifiedDate().getTime());
            assertEquals(ZonedDateTime.parse("2025-08-13T11:13:32Z"), entry.getLastModifiedDate().toInstant().atZone(ZoneOffset.UTC));
            assertEquals(13, entry.getCompressedSize());
            assertEquals("-lh0-", entry.getCompressionMethod());
            assertEquals(0x7757, entry.getCrcValue());
            assertEquals(85, entry.getOsId());
            assertEquals(0100644, entry.getUnixPermissionMode());
            assertEquals(20, entry.getUnixGroupId());
            assertEquals(501, entry.getUnixUserId());
            assertNull(entry.getMsdosFileAttributes());
            assertNull(entry.getHeaderCrc());

            // No more entries expected
            assertNull(archive.getNextEntry());
        }
    }

    @Test
    void testParseHeaderLevel1FileWithFoldersMsdos() throws IOException {
        // The lha file was generated by LHA32 v2.67.00 for Windows
        try (LhaArchiveInputStream archive = LhaArchiveInputStream.builder()
                .setInputStream(newInputStream("test-msdos-l1.lha"))
                .setFileSeparatorChar('/')
                .get()) {

            LhaArchiveEntry entry;

            // Check directory entry
            entry = archive.getNextEntry();
            assertNotNull(entry);
            assertEquals("dir1/", entry.getName());
            assertTrue(entry.isDirectory());
            assertEquals(0, entry.getSize());
            assertEquals(1755081308000L, convertSystemTimeZoneDateToUTC(entry.getLastModifiedDate()).toInstant().toEpochMilli());
            assertEquals(ZonedDateTime.parse("2025-08-13T10:35:08Z"), convertSystemTimeZoneDateToUTC(entry.getLastModifiedDate()));
            assertEquals(0, entry.getCompressedSize());
            assertEquals("-lhd-", entry.getCompressionMethod());
            assertEquals(0x0000, entry.getCrcValue());
            assertEquals(77, entry.getOsId());
            assertNull(entry.getUnixPermissionMode());
            assertNull(entry.getUnixGroupId());
            assertNull(entry.getUnixUserId());
            assertEquals(0x0010, entry.getMsdosFileAttributes());
            assertEquals(0xd458, entry.getHeaderCrc());

            // Check directory entry
            entry = archive.getNextEntry();
            assertNotNull(entry);
            assertEquals("dir1/dir1-1/", entry.getName());
            assertTrue(entry.isDirectory());
            assertEquals(0, entry.getSize());
            assertEquals(1755081336000L, convertSystemTimeZoneDateToUTC(entry.getLastModifiedDate()).toInstant().toEpochMilli());
            assertEquals(ZonedDateTime.parse("2025-08-13T10:35:36Z"), convertSystemTimeZoneDateToUTC(entry.getLastModifiedDate()));
            assertEquals(0, entry.getCompressedSize());
            assertEquals("-lhd-", entry.getCompressionMethod());
            assertEquals(0x0000, entry.getCrcValue());
            assertEquals(77, entry.getOsId());
            assertNull(entry.getUnixPermissionMode());
            assertNull(entry.getUnixGroupId());
            assertNull(entry.getUnixUserId());
            assertEquals(0x0010, entry.getMsdosFileAttributes());
            assertEquals(0x40de, entry.getHeaderCrc());

            // Check file entry
            entry = archive.getNextEntry();
            assertNotNull(entry);
            assertEquals("dir1/dir1-1/test1.txt", entry.getName());
            assertFalse(entry.isDirectory());
            assertEquals(14, entry.getSize());
            assertEquals(1755081276000L, convertSystemTimeZoneDateToUTC(entry.getLastModifiedDate()).toInstant().toEpochMilli());
            assertEquals(ZonedDateTime.parse("2025-08-13T10:34:36Z"), convertSystemTimeZoneDateToUTC(entry.getLastModifiedDate()));
            assertEquals(14, entry.getCompressedSize());
            assertEquals("-lh0-", entry.getCompressionMethod());
            assertEquals(0xc9b4, entry.getCrcValue());
            assertEquals(77, entry.getOsId());
            assertNull(entry.getUnixPermissionMode());
            assertNull(entry.getUnixGroupId());
            assertNull(entry.getUnixUserId());
            assertNull(entry.getMsdosFileAttributes());
            assertEquals(0x34b0, entry.getHeaderCrc());

            // Check directory entry
            entry = archive.getNextEntry();
            assertNotNull(entry);
            assertEquals("dir1/dir1-2/", entry.getName());
            assertTrue(entry.isDirectory());
            assertEquals(0, entry.getSize());
            assertEquals(1755081340000L, convertSystemTimeZoneDateToUTC(entry.getLastModifiedDate()).toInstant().toEpochMilli());
            assertEquals(ZonedDateTime.parse("2025-08-13T10:35:40Z"), convertSystemTimeZoneDateToUTC(entry.getLastModifiedDate()));
            assertEquals(0, entry.getCompressedSize());
            assertEquals("-lhd-", entry.getCompressionMethod());
            assertEquals(0x0000, entry.getCrcValue());
            assertEquals(77, entry.getOsId());
            assertNull(entry.getUnixPermissionMode());
            assertNull(entry.getUnixGroupId());
            assertNull(entry.getUnixUserId());
            assertEquals(0x0010, entry.getMsdosFileAttributes());
            assertEquals(0x21b2, entry.getHeaderCrc());

            // Check file entry
            entry = archive.getNextEntry();
            assertNotNull(entry);
            assertEquals("dir1/dir1-2/test2.txt", entry.getName());
            assertFalse(entry.isDirectory());
            assertEquals(14, entry.getSize());
            assertEquals(1755081276000L, convertSystemTimeZoneDateToUTC(entry.getLastModifiedDate()).toInstant().toEpochMilli());
            assertEquals(ZonedDateTime.parse("2025-08-13T10:34:36Z"), convertSystemTimeZoneDateToUTC(entry.getLastModifiedDate()));
            assertEquals(14, entry.getCompressedSize());
            assertEquals("-lh0-", entry.getCompressionMethod());
            assertEquals(0xc9b4, entry.getCrcValue());
            assertEquals(77, entry.getOsId());
            assertNull(entry.getUnixPermissionMode());
            assertNull(entry.getUnixGroupId());
            assertNull(entry.getUnixUserId());
            assertNull(entry.getMsdosFileAttributes());
            assertEquals(0x8f0c, entry.getHeaderCrc());

            // No more entries expected
            assertNull(archive.getNextEntry());
        }
    }

    @Test
    void testParseHeaderLevel2File() throws IOException {
        try (LhaArchiveInputStream archive = LhaArchiveInputStream.builder()
                .setInputStream(new ByteArrayInputStream(toByteArray(VALID_HEADER_LEVEL_2_FILE)))
                .setFileSeparatorChar('/')
                .get()) {

            // Entry should be parsed correctly
            final LhaArchiveEntry entry = archive.getNextEntry();
            assertNotNull(entry);
            assertEquals("test1.txt", entry.getName());
            assertFalse(entry.isDirectory());
            assertEquals(57, entry.getSize());
            assertEquals(1754229743000L, entry.getLastModifiedDate().getTime());
            assertEquals(ZonedDateTime.parse("2025-08-03T14:02:23Z"), entry.getLastModifiedDate().toInstant().atZone(ZoneOffset.UTC));
            assertEquals(52, entry.getCompressedSize());
            assertEquals("-lh5-", entry.getCompressionMethod());
            assertEquals(0x6496, entry.getCrcValue());
            assertEquals(85, entry.getOsId());
            assertEquals(0100644, entry.getUnixPermissionMode());
            assertEquals(20, entry.getUnixGroupId());
            assertEquals(501, entry.getUnixUserId());
            assertNull(entry.getMsdosFileAttributes());
            assertEquals(0x01a5, entry.getHeaderCrc());

            // No more entries expected
            assertNull(archive.getNextEntry());
        }
    }

    @Test
    void testInvalidHeaderLevel2Length() throws IOException {
        final byte[] data = toByteArray(VALID_HEADER_LEVEL_2_FILE);

        data[0] = 0x10; // Change the first byte to an invalid length

        try (LhaArchiveInputStream archive = LhaArchiveInputStream.builder().setInputStream(new ByteArrayInputStream(data)).get()) {
            archive.getNextEntry();
            fail("Expected ArchiveException for invalid header length");
        } catch (ArchiveException e) {
            assertEquals("Invalid header level 2 length: 16", e.getMessage());
        }
    }

    @Test
    void testInvalidHeaderLevel2Checksum() throws IOException {
        final byte[] data = toByteArray(VALID_HEADER_LEVEL_2_FILE);

        // Change header CRC to an invalid value
        data[27] = 0x33;
        data[28] = 0x22;

        try (LhaArchiveInputStream archive = LhaArchiveInputStream.builder().setInputStream(new ByteArrayInputStream(data)).get()) {
            archive.getNextEntry();
            fail("Expected ArchiveException for invalid header checksum");
        } catch (ArchiveException e) {
            assertEquals("Invalid header CRC expected=0x01a5 found=0x2233", e.getMessage());
        }
    }

    @Test
    void testParseHeaderLevel2FileWithFoldersAmiga() throws IOException {
        // The lha file was generated by LhA 2.15 on Amiga
        try (LhaArchiveInputStream archive = LhaArchiveInputStream.builder()
                .setInputStream(newInputStream("test-amiga-l2.lha"))
                .setFileSeparatorChar('/')
                .get()) {

            LhaArchiveEntry entry;

            // No -lhd- directory entries in Amiga LHA files, so we expect only file entries

            // Check file entry
            entry = archive.getNextEntry();
            assertNotNull(entry);
            assertEquals("dir1/dir1-1/test1.txt", entry.getName());
            assertFalse(entry.isDirectory());
            assertEquals(14, entry.getSize());
            assertEquals(1755081276000L, entry.getLastModifiedDate().getTime());
            assertEquals(ZonedDateTime.parse("2025-08-13T10:34:36Z"), entry.getLastModifiedDate().toInstant().atZone(ZoneOffset.UTC));
            assertEquals(14, entry.getCompressedSize());
            assertEquals("-lh0-", entry.getCompressionMethod());
            assertEquals(0xc9b4, entry.getCrcValue());
            assertEquals(65, entry.getOsId());
            assertNull(entry.getUnixPermissionMode());
            assertNull(entry.getUnixGroupId());
            assertNull(entry.getUnixUserId());
            assertNull(entry.getMsdosFileAttributes());
            assertEquals(0xe1a5, entry.getHeaderCrc());

            // Check file entry
            entry = archive.getNextEntry();
            assertNotNull(entry);
            assertEquals("dir1/dir1-2/test2.txt", entry.getName());
            assertFalse(entry.isDirectory());
            assertEquals(14, entry.getSize());
            assertEquals(1755081276000L, entry.getLastModifiedDate().getTime());
            assertEquals(ZonedDateTime.parse("2025-08-13T10:34:36Z"), entry.getLastModifiedDate().toInstant().atZone(ZoneOffset.UTC));
            assertEquals(14, entry.getCompressedSize());
            assertEquals("-lh0-", entry.getCompressionMethod());
            assertEquals(0xc9b4, entry.getCrcValue());
            assertEquals(65, entry.getOsId());
            assertNull(entry.getUnixPermissionMode());
            assertNull(entry.getUnixGroupId());
            assertNull(entry.getUnixUserId());
            assertNull(entry.getMsdosFileAttributes());
            assertEquals(0xd6b0, entry.getHeaderCrc());

            // No more entries expected
            assertNull(archive.getNextEntry());
        }
    }

    @Test
    void testParseHeaderLevel2FileWithFoldersMacos() throws IOException {
        // The lha file was generated by LHa for UNIX version 1.14i-ac20211125 for Macos
        try (LhaArchiveInputStream archive = LhaArchiveInputStream.builder()
                .setInputStream(newInputStream("test-macos-l2.lha"))
                .setFileSeparatorChar('/')
                .get()) {

            LhaArchiveEntry entry;

            // Check directory entry
            entry = archive.getNextEntry();
            assertNotNull(entry);
            assertEquals("dir1/", entry.getName());
            assertTrue(entry.isDirectory());
            assertEquals(0, entry.getSize());
            assertEquals(1755083490000L, entry.getLastModifiedDate().getTime());
            assertEquals(ZonedDateTime.parse("2025-08-13T11:11:30Z"), entry.getLastModifiedDate().toInstant().atZone(ZoneOffset.UTC));
            assertEquals(0, entry.getCompressedSize());
            assertEquals("-lhd-", entry.getCompressionMethod());
            assertEquals(0x0000, entry.getCrcValue());
            assertEquals(85, entry.getOsId());
            assertEquals(040755, entry.getUnixPermissionMode());
            assertEquals(20, entry.getUnixGroupId());
            assertEquals(501, entry.getUnixUserId());
            assertNull(entry.getMsdosFileAttributes());
            assertEquals(0xf3f7, entry.getHeaderCrc());

            // Check directory entry
            entry = archive.getNextEntry();
            assertNotNull(entry);
            assertEquals("dir1/dir1-1/", entry.getName());
            assertTrue(entry.isDirectory());
            assertEquals(0, entry.getSize());
            assertEquals(1755083529000L, entry.getLastModifiedDate().getTime());
            assertEquals(ZonedDateTime.parse("2025-08-13T11:12:09Z"), entry.getLastModifiedDate().toInstant().atZone(ZoneOffset.UTC));
            assertEquals(0, entry.getCompressedSize());
            assertEquals("-lhd-", entry.getCompressionMethod());
            assertEquals(0x0000, entry.getCrcValue());
            assertEquals(85, entry.getOsId());
            assertEquals(040755, entry.getUnixPermissionMode());
            assertEquals(20, entry.getUnixGroupId());
            assertEquals(501, entry.getUnixUserId());
            assertNull(entry.getMsdosFileAttributes());
            assertEquals(0x50d3, entry.getHeaderCrc());

            // Check file entry
            entry = archive.getNextEntry();
            assertNotNull(entry);
            assertEquals("dir1/dir1-1/test1.txt", entry.getName());
            assertFalse(entry.isDirectory());
            assertEquals(13, entry.getSize());
            assertEquals(1755083529000L, entry.getLastModifiedDate().getTime());
            assertEquals(ZonedDateTime.parse("2025-08-13T11:12:09Z"), entry.getLastModifiedDate().toInstant().atZone(ZoneOffset.UTC));
            assertEquals(13, entry.getCompressedSize());
            assertEquals("-lh0-", entry.getCompressionMethod());
            assertEquals(0x7757, entry.getCrcValue());
            assertEquals(85, entry.getOsId());
            assertEquals(0100644, entry.getUnixPermissionMode());
            assertEquals(20, entry.getUnixGroupId());
            assertEquals(501, entry.getUnixUserId());
            assertNull(entry.getMsdosFileAttributes());
            assertEquals(0x589e, entry.getHeaderCrc());

            // Check directory entry
            entry = archive.getNextEntry();
            assertNotNull(entry);
            assertEquals("dir1/dir1-2/", entry.getName());
            assertTrue(entry.isDirectory());
            assertEquals(0, entry.getSize());
            assertEquals(1755083612000L, entry.getLastModifiedDate().getTime());
            assertEquals(ZonedDateTime.parse("2025-08-13T11:13:32Z"), entry.getLastModifiedDate().toInstant().atZone(ZoneOffset.UTC));
            assertEquals(0, entry.getCompressedSize());
            assertEquals("-lhd-", entry.getCompressionMethod());
            assertEquals(0x0000, entry.getCrcValue());
            assertEquals(85, entry.getOsId());
            assertEquals(040755, entry.getUnixPermissionMode());
            assertEquals(20, entry.getUnixGroupId());
            assertEquals(501, entry.getUnixUserId());
            assertNull(entry.getMsdosFileAttributes());
            assertEquals(0x126d, entry.getHeaderCrc());

            // Check file entry
            entry = archive.getNextEntry();
            assertNotNull(entry);
            assertEquals("dir1/dir1-2/test2.txt", entry.getName());
            assertFalse(entry.isDirectory());
            assertEquals(13, entry.getSize());
            assertEquals(1755083612000L, entry.getLastModifiedDate().getTime());
            assertEquals(ZonedDateTime.parse("2025-08-13T11:13:32Z"), entry.getLastModifiedDate().toInstant().atZone(ZoneOffset.UTC));
            assertEquals(13, entry.getCompressedSize());
            assertEquals("-lh0-", entry.getCompressionMethod());
            assertEquals(0x7757, entry.getCrcValue());
            assertEquals(85, entry.getOsId());
            assertEquals(0100644, entry.getUnixPermissionMode());
            assertEquals(20, entry.getUnixGroupId());
            assertEquals(501, entry.getUnixUserId());
            assertNull(entry.getMsdosFileAttributes());
            assertEquals(0xdbdd, entry.getHeaderCrc());

            // No more entries expected
            assertNull(archive.getNextEntry());
        }
    }

    @Test
    void testParseHeaderLevel2FileWithFoldersMsdos() throws IOException {
        // The lha file was generated by LHA32 v2.67.00 for Windows
        try (LhaArchiveInputStream archive = LhaArchiveInputStream.builder()
                .setInputStream(newInputStream("test-msdos-l2.lha"))
                .setFileSeparatorChar('/')
                .get()) {

            LhaArchiveEntry entry;

            // Check directory entry
            entry = archive.getNextEntry();
            assertNotNull(entry);
            assertEquals("dir1/", entry.getName());
            assertTrue(entry.isDirectory());
            assertEquals(0, entry.getSize());
            assertEquals(1755081308000L, entry.getLastModifiedDate().getTime());
            assertEquals(ZonedDateTime.parse("2025-08-13T10:35:08Z"), entry.getLastModifiedDate().toInstant().atZone(ZoneOffset.UTC));
            assertEquals(0, entry.getCompressedSize());
            assertEquals("-lhd-", entry.getCompressionMethod());
            assertEquals(0x0000, entry.getCrcValue());
            assertEquals(77, entry.getOsId());
            assertNull(entry.getUnixPermissionMode());
            assertNull(entry.getUnixGroupId());
            assertNull(entry.getUnixUserId());
            assertEquals(0x0010, entry.getMsdosFileAttributes());
            assertEquals(0x496a, entry.getHeaderCrc());

            // Check directory entry
            entry = archive.getNextEntry();
            assertNotNull(entry);
            assertEquals("dir1/dir1-1/", entry.getName());
            assertTrue(entry.isDirectory());
            assertEquals(0, entry.getSize());
            assertEquals(1755081336000L, entry.getLastModifiedDate().getTime());
            assertEquals(ZonedDateTime.parse("2025-08-13T10:35:36Z"), entry.getLastModifiedDate().toInstant().atZone(ZoneOffset.UTC));
            assertEquals(0, entry.getCompressedSize());
            assertEquals("-lhd-", entry.getCompressionMethod());
            assertEquals(0x0000, entry.getCrcValue());
            assertEquals(77, entry.getOsId());
            assertNull(entry.getUnixPermissionMode());
            assertNull(entry.getUnixGroupId());
            assertNull(entry.getUnixUserId());
            assertEquals(0x0010, entry.getMsdosFileAttributes());
            assertEquals(0xebe7, entry.getHeaderCrc());

            // Check file entry
            entry = archive.getNextEntry();
            assertNotNull(entry);
            assertEquals("dir1/dir1-1/test1.txt", entry.getName());
            assertFalse(entry.isDirectory());
            assertEquals(14, entry.getSize());
            assertEquals(1755081276000L, entry.getLastModifiedDate().getTime());
            assertEquals(ZonedDateTime.parse("2025-08-13T10:34:36Z"), entry.getLastModifiedDate().toInstant().atZone(ZoneOffset.UTC));
            assertEquals(14, entry.getCompressedSize());
            assertEquals("-lh0-", entry.getCompressionMethod());
            assertEquals(0xc9b4, entry.getCrcValue());
            assertEquals(77, entry.getOsId());
            assertNull(entry.getUnixPermissionMode());
            assertNull(entry.getUnixGroupId());
            assertNull(entry.getUnixUserId());
            assertNull(entry.getMsdosFileAttributes());
            assertEquals(0x214a, entry.getHeaderCrc());

            // Check directory entry
            entry = archive.getNextEntry();
            assertNotNull(entry);
            assertEquals("dir1/dir1-2/", entry.getName());
            assertTrue(entry.isDirectory());
            assertEquals(0, entry.getSize());
            assertEquals(1755081341000L, entry.getLastModifiedDate().getTime());
            assertEquals(ZonedDateTime.parse("2025-08-13T10:35:41Z"), entry.getLastModifiedDate().toInstant().atZone(ZoneOffset.UTC));
            assertEquals(0, entry.getCompressedSize());
            assertEquals("-lhd-", entry.getCompressionMethod());
            assertEquals(0x0000, entry.getCrcValue());
            assertEquals(77, entry.getOsId());
            assertNull(entry.getUnixPermissionMode());
            assertNull(entry.getUnixGroupId());
            assertNull(entry.getUnixUserId());
            assertEquals(0x0010, entry.getMsdosFileAttributes());
            assertEquals(0x74ca, entry.getHeaderCrc());

            // Check file entry
            entry = archive.getNextEntry();
            assertNotNull(entry);
            assertEquals("dir1/dir1-2/test2.txt", entry.getName());
            assertFalse(entry.isDirectory());
            assertEquals(14, entry.getSize());
            assertEquals(1755081276000L, entry.getLastModifiedDate().getTime());
            assertEquals(ZonedDateTime.parse("2025-08-13T10:34:36Z"), entry.getLastModifiedDate().toInstant().atZone(ZoneOffset.UTC));
            assertEquals(14, entry.getCompressedSize());
            assertEquals("-lh0-", entry.getCompressionMethod());
            assertEquals(0xc9b4, entry.getCrcValue());
            assertEquals(77, entry.getOsId());
            assertNull(entry.getUnixPermissionMode());
            assertNull(entry.getUnixGroupId());
            assertNull(entry.getUnixUserId());
            assertNull(entry.getMsdosFileAttributes());
            assertEquals(0x165f, entry.getHeaderCrc());

            // No more entries expected
            assertNull(archive.getNextEntry());
        }
    }

    @Test
    void testParseHeaderLevel2FileWithMsdosAttributes() throws IOException {
        // The lha file was generated by LHA32 v2.67.00 for Windows
        try (LhaArchiveInputStream archive = LhaArchiveInputStream.builder().setInputStream(newInputStream("test-msdos-l2-attrib.lha")).get()) {
            // Check file entry
            final LhaArchiveEntry entry = archive.getNextEntry();
            assertNotNull(entry);
            assertEquals("test1.txt", entry.getName());
            assertFalse(entry.isDirectory());
            assertEquals(14, entry.getSize());
            assertEquals(1755081276000L, entry.getLastModifiedDate().getTime());
            assertEquals(ZonedDateTime.parse("2025-08-13T10:34:36Z"), entry.getLastModifiedDate().toInstant().atZone(ZoneOffset.UTC));
            assertEquals(14, entry.getCompressedSize());
            assertEquals("-lh0-", entry.getCompressionMethod());
            assertEquals(0xc9b4, entry.getCrcValue());
            assertEquals(77, entry.getOsId());
            assertNull(entry.getUnixPermissionMode());
            assertNull(entry.getUnixGroupId());
            assertNull(entry.getUnixUserId());
            assertEquals(0x0021, entry.getMsdosFileAttributes());
            assertEquals(0x14bb, entry.getHeaderCrc());

            // No more entries expected
            assertNull(archive.getNextEntry());
        }
    }

    @Test
    void testParseExtendedHeaderCommon() throws IOException {
        try (LhaArchiveInputStream archive = LhaArchiveInputStream.builder().setInputStream(newEmptyInputStream()).get()) {
            final LhaArchiveEntry.Builder entryBuilder = LhaArchiveEntry.builder();
            archive.parseExtendedHeader(toByteBuffer(0x00, 0x22, 0x33, 0x00, 0x00), entryBuilder);
            assertEquals(0x3322, entryBuilder.get().getHeaderCrc());
        }
    }

    @Test
    void testParseExtendedHeaderFilename() throws IOException {
        try (LhaArchiveInputStream archive = LhaArchiveInputStream.builder().setInputStream(newEmptyInputStream()).get()) {
            final LhaArchiveEntry.Builder entryBuilder = LhaArchiveEntry.builder();
            archive.parseExtendedHeader(toByteBuffer(0x01, 't', 'e', 's', 't', '.', 't', 'x', 't', 0x00, 0x00), entryBuilder);
            assertEquals("test.txt", entryBuilder.get().getName());
        }
    }

    @Test
    void testParseExtendedHeaderDirectoryName() throws IOException {
        try (LhaArchiveInputStream archive = LhaArchiveInputStream.builder()
                .setInputStream(newEmptyInputStream())
                .setFileSeparatorChar('/')
                .get()) {

            final LhaArchiveEntry.Builder entryBuilder = LhaArchiveEntry.builder();
            archive.parseExtendedHeader(toByteBuffer(0x02, 'd', 'i', 'r', '1', 0xff, 0x00, 0x00), entryBuilder);
            assertEquals("dir1/", entryBuilder.get().getName());
        }
    }

    @Test
    void testParseExtendedHeaderFilenameAndDirectoryName() throws IOException {
        try (LhaArchiveInputStream archive = LhaArchiveInputStream.builder()
                .setInputStream(newEmptyInputStream())
                .setFileSeparatorChar('/')
                .get()) {

            LhaArchiveEntry.Builder entryBuilder;

            // Test filename and directory name order
            entryBuilder = LhaArchiveEntry.builder();
            archive.parseExtendedHeader(toByteBuffer(0x01, 't', 'e', 's', 't', '.', 't', 'x', 't', 0x00, 0x00), entryBuilder);
            archive.parseExtendedHeader(toByteBuffer(0x02, 'd', 'i', 'r', '1', 0xff, 0x00, 0x00), entryBuilder);
            assertEquals("dir1/test.txt", entryBuilder.get().getName());

            // Test filename and directory name order, no trailing slash
            entryBuilder = LhaArchiveEntry.builder();
            archive.parseExtendedHeader(toByteBuffer(0x01, 't', 'e', 's', 't', '.', 't', 'x', 't', 0x00, 0x00), entryBuilder);
            archive.parseExtendedHeader(toByteBuffer(0x02, 'd', 'i', 'r', '1', 0x00, 0x00), entryBuilder);
            assertEquals("dir1/test.txt", entryBuilder.get().getName());

            // Test directory name and filename order
            entryBuilder = LhaArchiveEntry.builder();
            archive.parseExtendedHeader(toByteBuffer(0x02, 'd', 'i', 'r', '1', 0xff, 0x00, 0x00), entryBuilder);
            archive.parseExtendedHeader(toByteBuffer(0x01, 't', 'e', 's', 't', '.', 't', 'x', 't', 0x00, 0x00), entryBuilder);
            assertEquals("dir1/test.txt", entryBuilder.get().getName());

            // Test directory name and filename order, no trailing slash
            entryBuilder = LhaArchiveEntry.builder();
            archive.parseExtendedHeader(toByteBuffer(0x02, 'd', 'i', 'r', '1', 0x00, 0x00), entryBuilder);
            archive.parseExtendedHeader(toByteBuffer(0x01, 't', 'e', 's', 't', '.', 't', 'x', 't', 0x00, 0x00), entryBuilder);
            assertEquals("dir1/test.txt", entryBuilder.get().getName());
        }
    }

    @Test
    void testParseExtendedHeaderUnixPermission() throws IOException {
        try (LhaArchiveInputStream archive = LhaArchiveInputStream.builder().setInputStream(newEmptyInputStream()).get()) {
            final LhaArchiveEntry.Builder entryBuilder = LhaArchiveEntry.builder();
            archive.parseExtendedHeader(toByteBuffer(0x50, 0xa4, 0x81, 0x00, 0x00), entryBuilder);
            assertEquals(0x81a4, entryBuilder.get().getUnixPermissionMode());
            assertEquals(0100644, entryBuilder.get().getUnixPermissionMode());
        }
    }

    @Test
    void testParseExtendedHeaderUnixUidGid() throws IOException {
        try (LhaArchiveInputStream archive = LhaArchiveInputStream.builder().setInputStream(newEmptyInputStream()).get()) {
            final LhaArchiveEntry.Builder entryBuilder = LhaArchiveEntry.builder();
            archive.parseExtendedHeader(toByteBuffer(0x51, 0x14, 0x00, 0xf5, 0x01, 0x00, 0x00), entryBuilder);
            assertEquals(0x0014, entryBuilder.get().getUnixGroupId());
            assertEquals(0x01f5, entryBuilder.get().getUnixUserId());
        }
    }

    @Test
    void testParseExtendedHeaderUnixTimestamp() throws IOException {
        try (LhaArchiveInputStream archive = LhaArchiveInputStream.builder().setInputStream(newEmptyInputStream()).get()) {
            final LhaArchiveEntry.Builder entryBuilder = LhaArchiveEntry.builder();
            archive.parseExtendedHeader(toByteBuffer(0x54, 0x5c, 0x73, 0x9c, 0x68, 0x00, 0x00), entryBuilder);
            assertEquals(0x689c735cL, entryBuilder.get().getLastModifiedDate().getTime() / 1000);
        }
    }

    @Test
    void testParseExtendedHeaderMSdosFileAttributes() throws IOException {
        try (LhaArchiveInputStream archive = LhaArchiveInputStream.builder().setInputStream(newEmptyInputStream()).get()) {
            final LhaArchiveEntry.Builder entryBuilder = LhaArchiveEntry.builder();
            archive.parseExtendedHeader(toByteBuffer(0x40, 0x10, 0x00, 0x00), entryBuilder);
            assertEquals(0x10, entryBuilder.get().getMsdosFileAttributes());
        }
    }

    @Test
    void testDecompressLh0() throws Exception {
        try (LhaArchiveInputStream archive = LhaArchiveInputStream.builder()
                .setInputStream(newInputStream("test-macos-l0.lha"))
                .get()) {

            final List<String> files = new ArrayList<>();
            files.add("dir1" + File.separatorChar);
            files.add("dir1" + File.separatorChar + "dir1-1" + File.separatorChar);
            files.add("dir1" + File.separatorChar + "dir1-1" + File.separatorChar + "test1.txt");
            files.add("dir1" + File.separatorChar + "dir1-2" + File.separatorChar);
            files.add("dir1" + File.separatorChar + "dir1-2" + File.separatorChar + "test2.txt");
            checkArchiveContent(archive, files);
        }
    }

    @Test
    void testDecompressLh4() throws Exception {
        try (LhaArchiveInputStream archive = LhaArchiveInputStream.builder().setInputStream(newInputStream("test-amiga-l0-lh4.lha")).get()) {
            final List<String> files = new ArrayList<>();
            files.add("lorem-ipsum.txt");
            checkArchiveContent(archive, files);
        }
    }

    @Test
    void testDecompressLh5() throws Exception {
        try (LhaArchiveInputStream archive = LhaArchiveInputStream.builder().setInputStream(newInputStream("test-macos-l0-lh5.lha")).get()) {
            final List<String> files = new ArrayList<>();
            files.add("lorem-ipsum.txt");
            checkArchiveContent(archive, files);
        }
    }

    /**
     * Test decompressing a file with lh5 compression that contains only one characters and thus is
     * basically RLE encoded. The distance tree contains only one entry (root node).
     */
    @Test
    void testDecompressLh5Rle() throws Exception {
        try (LhaArchiveInputStream archive = LhaArchiveInputStream.builder().setInputStream(newInputStream("test-macos-l0-lh5-rle.lha")).get()) {
            final List<String> files = new ArrayList<>();
            files.add("rle.txt");
            checkArchiveContent(archive, files);
        }
    }

    @Test
    void testDecompressLh6() throws Exception {
        try (LhaArchiveInputStream archive = LhaArchiveInputStream.builder().setInputStream(newInputStream("test-macos-l0-lh6.lha")).get()) {
            final List<String> files = new ArrayList<>();
            files.add("lorem-ipsum.txt");
            checkArchiveContent(archive, files);
        }
    }

    @Test
    void testDecompressLh7() throws Exception {
        try (LhaArchiveInputStream archive = LhaArchiveInputStream.builder().setInputStream(newInputStream("test-macos-l0-lh7.lha")).get()) {
            final List<String> files = new ArrayList<>();
            files.add("lorem-ipsum.txt");
            checkArchiveContent(archive, files);
        }
    }

    @Test
    void testMatches() {
        byte[] data;

        assertTrue(LhaArchiveInputStream.matches(toByteArray(VALID_HEADER_LEVEL_0_FILE), VALID_HEADER_LEVEL_0_FILE.length));
        assertTrue(LhaArchiveInputStream.matches(toByteArray(VALID_HEADER_LEVEL_1_FILE), VALID_HEADER_LEVEL_1_FILE.length));
        assertTrue(LhaArchiveInputStream.matches(toByteArray(VALID_HEADER_LEVEL_2_FILE), VALID_HEADER_LEVEL_2_FILE.length));

        // Header to short
        data = toByteArray(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09);
        assertFalse(LhaArchiveInputStream.matches(data, data.length));

        // Change the header level to an invalid value
        data = toByteArray(VALID_HEADER_LEVEL_0_FILE);
        data[20] = 4;
        assertFalse(LhaArchiveInputStream.matches(data, data.length));

        // Change the compression method to an invalid value
        data = toByteArray(VALID_HEADER_LEVEL_0_FILE);
        data[6] = 0x08;
        assertFalse(LhaArchiveInputStream.matches(data, data.length));
    }

    @Test
    void testGetCompressionMethod() throws IOException {
        assertEquals("-lh0-", LhaArchiveInputStream.getCompressionMethod(ByteBuffer.wrap(toByteArray(0x00, 0x00, '-', 'l', 'h', '0', '-'))));
        assertEquals("-lhd-", LhaArchiveInputStream.getCompressionMethod(ByteBuffer.wrap(toByteArray(0x00, 0x00, '-', 'l', 'h', 'd', '-'))));

        try {
            LhaArchiveInputStream.getCompressionMethod(ByteBuffer.wrap(toByteArray(0x00, 0x00, '-', 'l', 'h', '0', 0xff)));
            fail("Expected ArchiveException for invalid compression method");
        } catch (ArchiveException e) {
            assertEquals("Invalid compression method: 0x2d 0x6c 0x68 0x30 0xff", e.getMessage());
        }
    }

    @Test
    void testGetPathnameUnixFileSeparatorCharDefaultEncoding() throws IOException, UnsupportedEncodingException {
        try (LhaArchiveInputStream is = LhaArchiveInputStream.builder().setInputStream(newEmptyInputStream()).setFileSeparatorChar('/').get()) {
            assertEquals("folder/", getPathname(is, 'f', 'o', 'l', 'd', 'e', 'r', 0xff));
            assertEquals("folder/file.txt", getPathname(is, 'f', 'o', 'l', 'd', 'e', 'r', 0xff, 'f', 'i', 'l', 'e', '.', 't', 'x', 't'));
            assertEquals("folder/file.txt", getPathname(is, 0xff, 'f', 'o', 'l', 'd', 'e', 'r', 0xff, 'f', 'i', 'l', 'e', '.', 't', 'x', 't'));
            assertEquals("folder/file.txt", getPathname(is, '\\', 'f', 'o', 'l', 'd', 'e', 'r', '\\', 'f', 'i', 'l', 'e', '.', 't', 'x', 't'));

            // Unicode replacement characters for unsupported characters
            assertEquals("\uFFFD/\uFFFD/\uFFFD.txt", getPathname(is, 0xe5, 0xff, 0xe4, 0xff, 0xf6, '.', 't', 'x', 't'));
            assertEquals("\uFFFD/\uFFFD/\uFFFD.txt", getPathname(is, 0xe5, '\\', 0xe4, '\\', 0xf6, '.', 't', 'x', 't'));
        }
    }

    @Test
    void testGetPathnameUnixFileSeparatorCharIso88591() throws IOException, UnsupportedEncodingException {
        try (LhaArchiveInputStream is = LhaArchiveInputStream.builder()
                .setInputStream(newEmptyInputStream())
                .setCharset(StandardCharsets.ISO_8859_1)
                .setFileSeparatorChar('/')
                .get()) {

            assertEquals("\u00E5/\u00E4/\u00F6.txt", getPathname(is, 0xe5, 0xff, 0xe4, 0xff, 0xf6, '.', 't', 'x', 't'));
            assertEquals("\u00E5/\u00E4/\u00F6.txt", getPathname(is, 0xe5, '\\', 0xe4, '\\', 0xf6, '.', 't', 'x', 't'));
        }
    }

    @Test
    void testGetPathnameWindowsFileSeparatorCharDefaultEncoding() throws IOException, UnsupportedEncodingException {
        try (LhaArchiveInputStream is = LhaArchiveInputStream.builder().setInputStream(newEmptyInputStream()).setFileSeparatorChar('\\').get()) {
            assertEquals("folder\\", getPathname(is, 'f', 'o', 'l', 'd', 'e', 'r', 0xff));
            assertEquals("folder\\file.txt", getPathname(is, 'f', 'o', 'l', 'd', 'e', 'r', 0xff, 'f', 'i', 'l', 'e', '.', 't', 'x', 't'));
            assertEquals("folder\\file.txt", getPathname(is, 0xff, 'f', 'o', 'l', 'd', 'e', 'r', 0xff, 'f', 'i', 'l', 'e', '.', 't', 'x', 't'));
            assertEquals("folder\\file.txt", getPathname(is, '\\', 'f', 'o', 'l', 'd', 'e', 'r', '\\', 'f', 'i', 'l', 'e', '.', 't', 'x', 't'));

            // Unicode replacement characters for unsupported characters
            assertEquals("\uFFFD\\\uFFFD\\\uFFFD.txt", getPathname(is, 0xe5, 0xff, 0xe4, 0xff, 0xf6, '.', 't', 'x', 't'));
            assertEquals("\uFFFD\\\uFFFD\\\uFFFD.txt", getPathname(is, 0xe5, '\\', 0xe4, '\\', 0xf6, '.', 't', 'x', 't'));
        }
    }

    @Test
    void testGetPathnameWindowsFileSeparatorCharIso88591() throws IOException, UnsupportedEncodingException {
        try (LhaArchiveInputStream is = LhaArchiveInputStream.builder()
                .setInputStream(newEmptyInputStream())
                .setCharset(StandardCharsets.ISO_8859_1)
                .setFileSeparatorChar('\\')
                .get()) {

            assertEquals("\u00E5\\\u00E4\\\u00F6.txt", getPathname(is, 0xe5, 0xff, 0xe4, 0xff, 0xf6, '.', 't', 'x', 't'));
            assertEquals("\u00E5\\\u00E4\\\u00F6.txt", getPathname(is, 0xe5, '\\', 0xe4, '\\', 0xf6, '.', 't', 'x', 't'));
        }
    }

    private static byte[] toByteArray(final int... data) {
        final byte[] bytes = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            bytes[i] = (byte) data[i];
        }
        return bytes;
    }

    private static ByteBuffer toByteBuffer(final int... data) {
        return ByteBuffer.wrap(toByteArray(data)).order(ByteOrder.LITTLE_ENDIAN);
    }

    private String getPathname(final LhaArchiveInputStream is, final int... filepathBuffer) throws UnsupportedEncodingException {
        return is.getPathname(ByteBuffer.wrap(toByteArray(filepathBuffer)), filepathBuffer.length);
    }

    private InputStream newEmptyInputStream() {
        return new ByteArrayInputStream(new byte[0]);
    }

    /**
     * The timestamp used in header level 0 and 1 entries has no time zone information and is
     * converted in the system default time zone. This method converts the date to UTC to verify
     * the timestamp in unit tests.
     *
     * @param date the date to convert
     * @return a ZonedDateTime in UTC
     */
    private ZonedDateTime convertSystemTimeZoneDateToUTC(final Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).withZoneSameLocal(ZoneOffset.UTC);
    }
}
