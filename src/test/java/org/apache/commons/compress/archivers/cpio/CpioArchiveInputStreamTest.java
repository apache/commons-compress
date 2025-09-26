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
package org.apache.commons.compress.archivers.cpio;

import static org.apache.commons.lang3.reflect.FieldUtils.readDeclaredField;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

class CpioArchiveInputStreamTest extends AbstractTest {

    private long consumeEntries(final CpioArchiveInputStream in) throws IOException {
        long count = 0;
        CpioArchiveEntry entry;
        while ((entry = in.getNextEntry()) != null) {
            count++;
            assertNotNull(entry);
        }
        return count;
    }

    @Test
    void testCpioUnarchive() throws Exception {
        final StringBuilder expected = new StringBuilder();
        expected.append("./test1.xml<?xml version=\"1.0\"?>\n");
        expected.append("<empty/>./test2.xml<?xml version=\"1.0\"?>\n");
        expected.append("<empty/>\n");
        final StringBuilder result = new StringBuilder();
        try (CpioArchiveInputStream in =
                CpioArchiveInputStream.builder().setURI(getURI("bla.cpio")).get()) {
            CpioArchiveEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                result.append(entry.getName());
                int tmp;
                while ((tmp = in.read()) != -1) {
                    result.append((char) tmp);
                }
            }
        }
        assertEquals(result.toString(), expected.toString());
    }

    @Test
    void testCpioUnarchiveCreatedByRedlineRpm() throws Exception {
        long count = 0;
        try (CpioArchiveInputStream in = CpioArchiveInputStream.builder()
                .setURI(getURI("redline.cpio"))
                .get()) {
            count = consumeEntries(in);
        }
        assertEquals(count, 1);
    }

    @Test
    void testCpioUnarchiveMultibyteCharName() throws Exception {
        long count = 0;
        try (CpioArchiveInputStream in = CpioArchiveInputStream.builder()
                .setURI(getURI("COMPRESS-459.cpio"))
                .setCharset(StandardCharsets.UTF_8)
                .get()) {
            count = consumeEntries(in);
        }
        assertEquals(2, count);
    }

    @Test
    void testEndOfFileInEntry_c_namesize_0xFFFFFFFF() throws Exception {
        // CPIO header with c_namesize = 0xFFFFFFFF
        // @formatter:off
        final String header =
                "070701" + // c_magic
                "00000000" + // c_ino
                "000081A4" + // c_mode
                "00000000" + // c_uid
                "00000000" + // c_gid
                "00000001" + // c_nlink
                "00000000" + // c_mtime
                "00000000" + // c_filesize
                "00000000" + // c_devmajor
                "00000000" + // c_devminor
                "00000000" + // c_rdevmajor
                "00000000" + // c_rdevminor
                "FFFFFFFF" + // c_namesize
                "00000000"; // c_check
        // @formatter:on
        final byte[] data = new byte[header.getBytes(StandardCharsets.US_ASCII).length + 1];
        System.arraycopy(header.getBytes(), 0, data, 0, header.getBytes().length);
        try (CpioArchiveInputStream cpio = CpioArchiveInputStream.builder().setByteArray(data).get()) {
            assertThrows(ArchiveException.class, () -> cpio.getNextEntry());
        }
    }

    @Test
    void testInvalidLongValueInMetadata() throws Exception {
        try (CpioArchiveInputStream archive = CpioArchiveInputStream.builder()
                .setURI(getURI("org/apache/commons/compress/cpio/bad_long_value.cpio"))
                .get()) {
            assertThrows(IOException.class, archive::getNextEntry);
        }
    }

    @Test
    void testMultiByteReadConsistentlyReturnsMinusOneAtEof() throws Exception {
        final byte[] buf = new byte[2];
        try (CpioArchiveInputStream archive =
                     CpioArchiveInputStream.builder().setURI(getURI("bla.cpio")).get()) {
            assertNotNull(archive.getNextEntry());
            IOUtils.toByteArray(archive);
            assertEquals(-1, archive.read(buf));
            assertEquals(-1, archive.read(buf));
        }
    }

    @Test
    void testSingleArgumentConstructor() throws Exception {
        final InputStream inputStream = mock(InputStream.class);
        try (CpioArchiveInputStream archiveStream = new CpioArchiveInputStream(inputStream)) {
            assertEquals(StandardCharsets.US_ASCII, archiveStream.getCharset());
            assertEquals(512, readDeclaredField(archiveStream, "blockSize", true));
        }
    }

    @Test
    void testSingleByteReadConsistentlyReturnsMinusOneAtEof() throws Exception {
        try (CpioArchiveInputStream archive =
                CpioArchiveInputStream.builder().setURI(getURI("bla.cpio")).get()) {
            assertNotNull(archive.getNextEntry());
            IOUtils.toByteArray(archive);
            assertEquals(-1, archive.read());
            assertEquals(-1, archive.read());
        }
    }
}
