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

package org.apache.commons.compress.compressors.lha;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.archivers.lha.LhaArchiveEntry;
import org.apache.commons.compress.archivers.lha.LhaArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

class Lh6CompressorInputStreamTest extends AbstractTest {
    @Test
    void testConfiguration() throws IOException {
        try (Lh6CompressorInputStream in = new Lh6CompressorInputStream(new ByteArrayInputStream(new byte[0]))) {
            assertEquals(15, in.getDictionaryBits());
            assertEquals(32768, in.getDictionarySize());
            assertEquals(5, in.getDistanceBits());
            assertEquals(16, in.getMaxNumberOfDistanceCodes());
            assertEquals(256, in.getMaxMatchLength());
            assertEquals(510, in.getMaxNumberOfCommands());
        }
    }

    @Test
    void testDecompress() throws IOException {
        try (LhaArchiveInputStream archive = LhaArchiveInputStream .builder().setInputStream(newInputStream("test-macos-l0-lh6.lha")).get()) {
            // Check entry
            final LhaArchiveEntry entry = archive.getNextEntry();
            assertNotNull(entry);
            assertEquals("lorem-ipsum.txt", entry.getName());
            assertEquals(144060, entry.getSize());
            assertEquals(38037, entry.getCompressedSize());
            assertEquals("-lh6-", entry.getCompressionMethod());
            assertEquals(0x8c8a, entry.getCrcValue());

            // Decompress entry
            assertTrue(archive.canReadEntryData(entry));
            final byte[] data = IOUtils.toByteArray(archive);

            assertEquals(144060, data.length);
            assertEquals("\nLorem ipsum", new String(data, 0, 12, StandardCharsets.US_ASCII));
        }
    }
}
