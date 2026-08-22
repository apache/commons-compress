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
package org.apache.commons.compress.compressors.snappy;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

/**
 * Tests for class {@link SnappyCompressorInputStream}.
 *
 * @see SnappyCompressorInputStream
 */
class SnappyCompressorInputStreamTest {

    @Test
    void testRejectsOversizedFourByteLiteralLength() throws IOException {
        // uncompressed size varint (1000), then a literal element (tag 0xFC) whose four-byte length is 0xFFFFFFFF.
        // The unsigned length was narrowed to int, so (len - 1) wrapped to a zero-length literal that passed the
        // negative-size guard and produced no output, letting the decoder recurse on itself until the stack blew.
        final byte[] input = { (byte) 0xE8, 0x07, (byte) 0xFC, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };
        try (SnappyCompressorInputStream in = new SnappyCompressorInputStream(new ByteArrayInputStream(input))) {
            final IOException e = assertThrows(CompressorException.class, () -> IOUtils.toByteArray(in));
            assertTrue(e.getMessage().contains("literal length"), e::getMessage);
        }
    }
}
