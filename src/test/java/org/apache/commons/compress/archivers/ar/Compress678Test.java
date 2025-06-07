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

package org.apache.commons.compress.archivers.ar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests COMPRESS-678.
 */
public class Compress678Test {

    @ParameterizedTest
    @ValueSource(ints = { 15, 16, 17, 18, 32, 64, 128 })
    void test_LONGFILE_BSD(final int fileNameLen) throws IOException {
        test_LONGFILE_BSD(StringUtils.repeat('x', fileNameLen));
    }

    /**
     * @param fileName
     * @throws IOException if an I/O error occurs.
     * @throws FileNotFoundException
     */
    private void test_LONGFILE_BSD(final String fileName) throws IOException, FileNotFoundException {
        final File file = new File("target/Compress678Test-b.ar");
        Files.deleteIfExists(file.toPath());
        // First entry's name length is longer than 16 bytes and odd
        // data length is odd.
        final byte[] data = { 1 };
        try (ArArchiveOutputStream arOut = new ArArchiveOutputStream(new FileOutputStream(file))) {
            arOut.setLongFileMode(ArArchiveOutputStream.LONGFILE_BSD);
            // entry 1
            arOut.putArchiveEntry(new ArArchiveEntry(fileName, data.length));
            arOut.write(data);
            arOut.closeArchiveEntry();
            // entry 2
            arOut.putArchiveEntry(new ArArchiveEntry("a", data.length));
            arOut.write(data);
            arOut.closeArchiveEntry();
        }
        try (ArArchiveInputStream arIn = new ArArchiveInputStream(new FileInputStream(file))) {
            final ArArchiveEntry entry = arIn.getNextEntry();
            assertEquals(fileName, entry.getName());
            // Fix
            // ar -tv Compress678Test-b.ar
            // rw-r--r-- 0/0 1 Apr 27 16:10 2024 01234567891234567
            // ar: Compress678Test-b.ar: Inappropriate file type or format
            assertNotNull(arIn.getNextEntry());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = { 15, 16, 17, 18, 32, 64, 128 })
    void test_LONGFILE_BSD_with_spaces(final int fileNameLen) throws IOException {
        test_LONGFILE_BSD(StringUtils.repeat("x y", fileNameLen / 3));
    }

    @Test
    void test_LONGFILE_ERROR() throws IOException {
        final File file = new File("target/Compress678Test-a.ar");
        Files.deleteIfExists(file.toPath());
        // First entry's name length is longer than 16 bytes and odd
        final String name1 = "01234567891234567";
        // data length is odd.
        final byte[] data = { 1 };
        try (ArArchiveOutputStream arOut = new ArArchiveOutputStream(new FileOutputStream(file))) {
            arOut.setLongFileMode(ArArchiveOutputStream.LONGFILE_ERROR);
            // java.io.IOException: File name too long, > 16 chars: 01234567891234567
            assertThrows(IOException.class, () -> arOut.putArchiveEntry(new ArArchiveEntry(name1, data.length)));
        }
    }

    @Test
    void testShortName() throws IOException {
        final File file = new File("target/Compress678Test-c.ar");
        Files.deleteIfExists(file.toPath());
        // First entry's name length is <= than 16 bytes and odd
        final String name1 = "0123456789123456";
        // data length is odd.
        final byte[] data = { 1 };
        try (ArArchiveOutputStream arOut = new ArArchiveOutputStream(new FileOutputStream(file))) {
            arOut.setLongFileMode(ArArchiveOutputStream.LONGFILE_BSD);
            // entry 1
            arOut.putArchiveEntry(new ArArchiveEntry(name1, data.length));
            arOut.write(data);
            arOut.closeArchiveEntry();
            // entry 2
            arOut.putArchiveEntry(new ArArchiveEntry("a", data.length));
            arOut.write(data);
            arOut.closeArchiveEntry();
        }
        try (ArArchiveInputStream arIn = new ArArchiveInputStream(new FileInputStream(file))) {
            final ArArchiveEntry entry = arIn.getNextEntry();
            assertEquals(name1, entry.getName());
            assertNotNull(arIn.getNextEntry());
        }
    }
}
