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
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteOrder;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.utils.BitInputStream;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

class AbstractLhStaticHuffmanCompressorInputStreamTest {
    @Test
    void testInputStreamStatistics() throws IOException {
        final int[] compressedData = {
            0x00, 0x05, 0x28, 0x04, 0x4b, 0xfc, 0x16, 0xed,
            0x37, 0x00, 0x43, 0x00
        };

        try (Lh5CompressorInputStream in = createLh5CompressorInputStream(compressedData)) {
            final byte[] decompressedData = IOUtils.toByteArray(in);

            assertEquals(1024, decompressedData.length);
            for (int i = 0; i < decompressedData.length; i++) {
                assertEquals('A', decompressedData[i], "Byte at position " + i);
            }

            assertEquals(12, in.getCompressedCount());
            assertEquals(1024, in.getUncompressedCount());
        }
    }

    @Test
    void testReadCommandDecodingTreeWithSingleValue() throws IOException {
        final BinaryTree tree = createLh5CompressorInputStream(
            0b00000000, 0b00111111 // 5 bits length (0x00) and 5 bits the root value (0x00)
        ).readCommandDecodingTree();

        assertEquals(0, tree.read(new BitInputStream(new ByteArrayInputStream(new byte[0]), ByteOrder.BIG_ENDIAN)));
    }

    @Test
    void testReadCommandDecodingTreeWithInvalidSize() throws IOException {
        try {
            createLh5CompressorInputStream(
                0b10100000, 0b00000000 // 5 bits length (0x14 = 20)
            ).readCommandDecodingTree();

            fail("Expected CompressorException for table invalid size");
        } catch (CompressorException e) {
            assertEquals("Code length table has invalid size (20 > 19)", e.getMessage());
        }
    }

    @Test
    void testReadCommandTreeWithSingleValue() throws IOException {
        final BinaryTree tree = createLh5CompressorInputStream(
            0b00000000, 0b01111111, 0b01000000 // 9 bits length (0x00) and 9 bits the root value (0x01fd = 509)
        ).readCommandTree(new BinaryTree(new int [] { 0 }));

        assertEquals(0x01fd, tree.read(new BitInputStream(new ByteArrayInputStream(new byte[0]), ByteOrder.BIG_ENDIAN)));
    }

    @Test
    void testReadCommandTreeWithInvalidSize() throws IOException {
        try {
            createLh5CompressorInputStream(
                0b11111111, 0b10000000 // 9 bits length (0x01ff = 511)
            ).readCommandTree(new BinaryTree(new int [] { 0 }));

            fail("Expected CompressorException for table invalid size");
        } catch (CompressorException e) {
            assertEquals("Code length table has invalid size (511 > 510)", e.getMessage());
        }
    }

    @Test
    void testReadCommandTreeUnexpectedEndOfStream() throws IOException {
        try {
            createLh5CompressorInputStream(
                0b00000000, 0b01111111 // 9 bits length (0x00) and only 8 bits instead of expected 9 bits which will cause an unexpected end of stream
            ).readCommandTree(new BinaryTree(new int [] { 0 }));
            fail("Expected CompressorException for unexpected end of stream");
        } catch (CompressorException e) {
            assertEquals("Unexpected end of stream", e.getMessage());
        }
    }

    @Test
    void testReadCodeLength() throws IOException {
        assertEquals(0, createLh5CompressorInputStream(0x00, 0x00).readCodeLength());  // 0000 0000  0000 0000
        assertEquals(1, createLh5CompressorInputStream(0x20, 0x00).readCodeLength());  // 0010 0000  0000 0000
        assertEquals(2, createLh5CompressorInputStream(0x40, 0x00).readCodeLength());  // 0100 0000  0000 0000
        assertEquals(3, createLh5CompressorInputStream(0x60, 0x00).readCodeLength());  // 0110 0000  0000 0000
        assertEquals(4, createLh5CompressorInputStream(0x80, 0x00).readCodeLength());  // 1000 0000  0000 0000
        assertEquals(5, createLh5CompressorInputStream(0xa0, 0x00).readCodeLength());  // 1010 0000  0000 0000
        assertEquals(6, createLh5CompressorInputStream(0xc0, 0x00).readCodeLength());  // 1100 0000  0000 0000
        assertEquals(7, createLh5CompressorInputStream(0xe0, 0x00).readCodeLength());  // 1110 0000  0000 0000
        assertEquals(8, createLh5CompressorInputStream(0xf0, 0x00).readCodeLength());  // 1111 0000  0000 0000
        assertEquals(9, createLh5CompressorInputStream(0xf8, 0x00).readCodeLength());  // 1111 1000  0000 0000
        assertEquals(10, createLh5CompressorInputStream(0xfc, 0x00).readCodeLength()); // 1111 1100  0000 0000
        assertEquals(11, createLh5CompressorInputStream(0xfe, 0x00).readCodeLength()); // 1111 1110  0000 0000
        assertEquals(12, createLh5CompressorInputStream(0xff, 0x00).readCodeLength()); // 1111 1111  0000 0000
        assertEquals(13, createLh5CompressorInputStream(0xff, 0x80).readCodeLength()); // 1111 1111  1000 0000
        assertEquals(14, createLh5CompressorInputStream(0xff, 0xc0).readCodeLength()); // 1111 1111  1100 0000
        assertEquals(15, createLh5CompressorInputStream(0xff, 0xe0).readCodeLength()); // 1111 1111  1110 0000
        assertEquals(16, createLh5CompressorInputStream(0xff, 0xf0).readCodeLength()); // 1111 1111  1111 0000

        try {
            createLh5CompressorInputStream(0xff, 0xf8).readCodeLength(); // 1111 1111  1111 1000
            fail("Expected CompressorException for code length overflow");
        } catch (CompressorException e) {
            assertEquals("Code length overflow", e.getMessage());
        }
    }

    @Test
    void testReadCodeLengthUnexpectedEndOfStream() throws IOException {
        try {
            createLh5CompressorInputStream(0xff).readCodeLength(); // 1111 1111  EOF
            fail("Expected CompressorException for unexpected end of stream");
        } catch (CompressorException e) {
            assertEquals("Unexpected end of stream", e.getMessage());
        }
    }

    private Lh5CompressorInputStream createLh5CompressorInputStream(final int... data) throws IOException {
        final byte[] bytes = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            bytes[i] = (byte) data[i];
        }

        return new Lh5CompressorInputStream(new ByteArrayInputStream(bytes));
    }
}
