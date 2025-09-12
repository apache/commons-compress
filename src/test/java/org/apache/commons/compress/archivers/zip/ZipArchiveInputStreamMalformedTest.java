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

package org.apache.commons.compress.archivers.zip;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import org.apache.commons.compress.CompressException;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link ZipArchiveInputStream}.
 */
public class ZipArchiveInputStreamMalformedTest {

    /**
     * If you want to see a MemoryLimitException, then run with {@code -Xmx64m}.
     */
    @Test
    void test() throws IOException {
        final String fixture = "target/ZipArchiveInputStreamMalformedTest.bin";
        try (FileOutputStream fos = new FileOutputStream(fixture)) {
            // Local File Header (LFH).
            final ByteBuffer buffer = ByteBuffer.allocate(30); // size of LFH without filename/extra.
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putInt(0x04034B50); // Local file header signature.
            buffer.putShort((short) 20); // version needed to extract.
            buffer.putShort((short) 0x08); // general purpose bit flag (bit 3 set).
            buffer.putShort((short) 0); // compression method.
            buffer.putShort((short) 0); // last mod file time.
            buffer.putShort((short) 0); // last mod file date.
            buffer.putInt(0); // CRC-32.
            buffer.putInt(0); // compressed size.
            buffer.putInt(0); // uncompressed size.
            buffer.putShort((short) 1); // file name length.
            buffer.putShort((short) 0); // extra field length.
            fos.write(buffer.array());
            // Filename: "a".
            fos.write('a');
            // Payload: 110 MB of 'A'.
            final byte[] chunk = new byte[1024 * 1024]; // 1 MB chunk.
            Arrays.fill(chunk, (byte) 'A');
            for (int i = 0; i < 110; i++) {
                fos.write(chunk);
            }
        }
        try (ZipArchiveInputStream zis = ZipArchiveInputStream.builder()
                .setFile(fixture)
                .setAllowStoredEntriesWithDataDescriptor(true)
                .get()) {
            zis.getNextEntry();
            assertThrows(CompressException.class, () -> IOUtils.toByteArray(zis));
        }
    }
}
