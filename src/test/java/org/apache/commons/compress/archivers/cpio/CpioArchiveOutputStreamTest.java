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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.Test;

class CpioArchiveOutputStreamTest extends AbstractTest {

    /**
     * The checksum of a new-crc entry covers the bytes written to the entry, not the bytes that precede the written slice in the caller's array.
     */
    @Test
    void testWriteFromBufferOffsetChecksumsTheWrittenSlice() throws Exception {
        final byte[] payload = { 1, 2, 3, 4, 5 };
        // The checksum of the payload is 15, the checksum of the first five bytes of the array is 45.
        final byte[] buffer = { 9, 9, 9, 9, 1, 2, 3, 4, 5, 9, 9 };
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            try (CpioArchiveOutputStream outputStream = new CpioArchiveOutputStream(bos, CpioConstants.FORMAT_NEW_CRC)) {
                final CpioArchiveEntry entry = new CpioArchiveEntry(CpioConstants.FORMAT_NEW_CRC, "test.txt", payload.length);
                entry.setChksum(15);
                outputStream.putArchiveEntry(entry);
                outputStream.write(buffer, 4, payload.length);
                assertDoesNotThrow(outputStream::closeArchiveEntry, "writing from a buffer offset must not produce a checksum error");
            }
            try (CpioArchiveInputStream in = CpioArchiveInputStream.builder().setByteArray(bos.toByteArray()).get()) {
                final CpioArchiveEntry entry = in.getNextEntry();
                assertNotNull(entry);
                assertEquals("test.txt", entry.getName());
                assertEquals(15, entry.getChksum());
                assertArrayEquals(payload, IOUtils.toByteArray(in));
            }
        }
    }

    @Test
    void testWriteOldBinary() throws Exception {
        final File file = getFile("test1.xml");
        final File output = newTempFile("test.cpio");
        final CpioArchiveOutputStream ref;
        try (CpioArchiveOutputStream outputStream = new CpioArchiveOutputStream(Files.newOutputStream(output.toPath()), CpioConstants.FORMAT_OLD_BINARY)) {
            ref = outputStream;
            outputStream.putArchiveEntry(new CpioArchiveEntry(CpioConstants.FORMAT_OLD_BINARY, file, "test1.xml"));
            outputStream.write(file);
            outputStream.closeArchiveEntry();
        }
        assertTrue(ref.isClosed());
        try (CpioArchiveInputStream in = CpioArchiveInputStream.builder().setFile(output).get()) {
            final CpioArchiveEntry e = in.getNextCPIOEntry();
            assertEquals("test1.xml", e.getName());
            assertNull(in.getNextEntry());
        }
    }
}
