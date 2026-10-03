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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteOrder;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.utils.BitInputStream;
import org.junit.jupiter.api.Test;

class LhaHuffmanDecoderTest {

    private BitInputStream createBitInputStream(final int... data) throws IOException {
        return new BitInputStream(new ByteArrayInputStream(AbstractTest.toByteArray(data)), ByteOrder.BIG_ENDIAN);
    }

    @Test
    void testCheckMaxDepth() {
        final CompressorException e = assertThrows(CompressorException.class, () -> new LhaHuffmanDecoder(1, 17),
                "Expected CompressorException for depth > 16");
        assertEquals("Invalid code length at symbol 1: 17 (expected in [0, 16])", e.getMessage());
    }

    @Test
    void testInvalidBitstream() throws Exception {
        final int[] length = { 4, 2, 3, 0, 5, 0, 1 };
        // Value: 0 1 2 3 4 5 6
        final LhaHuffmanDecoder decoder = new LhaHuffmanDecoder(length);
        assertEquals(6, decoder.decodeSymbol(createBitInputStream(0x00))); // 0xxx xxxx
        assertEquals(1, decoder.decodeSymbol(createBitInputStream(0x80))); // 10xx xxxx
        assertEquals(2, decoder.decodeSymbol(createBitInputStream(0xc0))); // 110x xxxx
        assertEquals(0, decoder.decodeSymbol(createBitInputStream(0xe0))); // 1110 xxxx
        assertEquals(4, decoder.decodeSymbol(createBitInputStream(0xf0))); // 1111 0xxx
        try {
            assertEquals(5, decoder.decodeSymbol(createBitInputStream(0xf8))); // 1111 1xxx
            fail("Expected CompressorException for invalid bitstream");
        } catch (final CompressorException e) {
            assertEquals("Invalid Huffman code: 62", e.getMessage());
        }
    }

    @Test
    void testNoLeafNodes() {
        final CompressorException e = assertThrows(CompressorException.class, () -> new LhaHuffmanDecoder(0, 0, 0, 0, 0),
                "Expected CompressorException for no leaf nodes");
        assertEquals("All code lengths are zero", e.getMessage());
    }

    @Test
    void testReadEof() throws IOException {
        final int[] length = { 4, 2, 3, 0, 5, 5, 1 };
        // Value: 0 1 2 3 4 5 6
        final LhaHuffmanDecoder decoder = new LhaHuffmanDecoder(length);
        final BitInputStream in = createBitInputStream(0xfe); // 1111 1110
        assertEquals(5, decoder.decodeSymbol(in)); // 1111 1xxx
        assertEquals(2, decoder.decodeSymbol(in)); // 110x xxxx
        assertThrows(EOFException.class, () -> decoder.decodeSymbol(in)); // EOF
    }

    @Test
    void testTooManyLeafNodes() {
        final CompressorException e = assertThrows(CompressorException.class, () -> new LhaHuffmanDecoder(0, 2, 1, 2, 2),
                "Expected CompressorException for too many leaf nodes");
        assertEquals("Tree contains too many leaf nodes for code length 2: 3 leaf nodes, but only 2 nodes available", e.getMessage());
    }

    @Test
    void testSingleCodeLengthHuffmanTree() throws IOException {
        // Special case where the single array value is the root node value and is returned without actually reading any bits
        final LhaHuffmanDecoder decoder = new LhaHuffmanDecoder(4);
        assertEquals(4, decoder.decodeSymbol(createBitInputStream())); // Nothing to read, just return the root value
    }

    @Test
    void testMaxNumberOfSymbolsForCommandDecoder() throws IOException {
        // Maximum length of 510 entries for command tree and maximum supported depth of 16
        final int[] length = { 4, 7, 7, 8, 7, 9, 8, 9, 7, 10, 8, 10, 7, 10, 8, 10, 7, 9, 8, 9, 8, 10, 8, 12, 8, 10, 9, 11, 9, 9, 8, 10, 6, 9, 7, 9, 8, 10, 8,
                11, 7, 9, 8, 9, 8, 9, 8, 9, 7, 9, 8, 8, 8, 10, 9, 11, 8, 9, 8, 10, 8, 9, 8, 9, 7, 7, 7, 8, 8, 8, 8, 9, 7, 8, 7, 9, 8, 9, 8, 8, 8, 10, 7, 7, 8,
                8, 8, 9, 8, 9, 8, 9, 9, 10, 9, 10, 7, 8, 9, 9, 8, 7, 7, 7, 8, 8, 9, 8, 8, 9, 8, 8, 8, 11, 8, 9, 8, 8, 9, 10, 9, 9, 8, 10, 8, 10, 9, 9, 7, 9, 9,
                10, 9, 10, 9, 9, 9, 10, 9, 11, 10, 11, 9, 10, 8, 10, 9, 11, 9, 10, 10, 12, 9, 11, 9, 12, 10, 14, 10, 14, 10, 11, 10, 11, 9, 11, 10, 12, 9, 11,
                10, 11, 9, 10, 10, 11, 9, 11, 10, 12, 10, 13, 11, 13, 10, 11, 10, 13, 10, 15, 10, 14, 8, 10, 9, 10, 9, 10, 10, 11, 9, 11, 10, 12, 10, 13, 10,
                13, 9, 11, 9, 11, 9, 12, 9, 11, 9, 10, 9, 12, 9, 11, 9, 9, 9, 10, 8, 10, 9, 11, 9, 10, 9, 10, 9, 10, 9, 10, 9, 11, 8, 10, 9, 10, 9, 10, 9, 11,
                9, 10, 8, 10, 8, 10, 9, 7, 3, 4, 5, 5, 6, 7, 7, 7, 8, 8, 9, 9, 9, 9, 10, 10, 11, 11, 11, 10, 11, 12, 11, 12, 12, 12, 12, 13, 13, 13, 14, 12, 14,
                13, 16, 14, 16, 13, 15, 14, 13, 15, 14, 15, 14, 15, 14, 14, 0, 14, 15, 14, 0, 14, 0, 0, 0, 0, 0, 0, 0, 15, 0, 15, 0, 0, 15, 15, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 15, 0, 0, 0, 0, 15, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 13, 0, 0, 15, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 15, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 15, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 15, 0, 0, 0, 15, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 15, 0, 0, 0, 0, 0, 0, 0, 0, 15, 10 };
        final LhaHuffmanDecoder decoder = new LhaHuffmanDecoder(length);
        assertEquals(256, decoder.decodeSymbol(createBitInputStream(0x00, 0x00))); // 000x xxxx xxxx xxxx
        assertEquals(0, decoder.decodeSymbol(createBitInputStream(0x20, 0x00))); // 0010 xxxx xxxx xxxx
        assertEquals(257, decoder.decodeSymbol(createBitInputStream(0x30, 0x00))); // 0011 xxxx xxxx xxxx
        assertEquals(258, decoder.decodeSymbol(createBitInputStream(0x40, 0x00))); // 0100 0xxx xxxx xxxx
        assertEquals(259, decoder.decodeSymbol(createBitInputStream(0x48, 0x00))); // 0100 1xxx xxxx xxxx
        assertEquals(32, decoder.decodeSymbol(createBitInputStream(0x50, 0x00))); // 0101 00xx xxxx xxxx
        assertEquals(260, decoder.decodeSymbol(createBitInputStream(0x54, 0x00))); // 0101 01xx xxxx xxxx
        assertEquals(226, decoder.decodeSymbol(createBitInputStream(0xbd, 0x00))); // 1011 1101 xxxx xxxx
        assertEquals(240, decoder.decodeSymbol(createBitInputStream(0xbe, 0x00))); // 1011 1110 xxxx xxxx
        assertEquals(163, decoder.decodeSymbol(createBitInputStream(0xfb, 0xa0))); // 1111 1011 101x xxxx
        assertEquals(165, decoder.decodeSymbol(createBitInputStream(0xfb, 0xc0))); // 1111 1011 110x xxxx
        assertEquals(499, decoder.decodeSymbol(createBitInputStream(0xff, 0xfa))); // 1111 1111 1111 101x
        assertEquals(508, decoder.decodeSymbol(createBitInputStream(0xff, 0xfc))); // 1111 1111 1111 110x
        assertEquals(290, decoder.decodeSymbol(createBitInputStream(0xff, 0xfe))); // 1111 1111 1111 1110
        assertEquals(292, decoder.decodeSymbol(createBitInputStream(0xff, 0xff))); // 1111 1111 1111 1111
    }

    @Test
    void testTree1() throws IOException {
        final int[] length = { 1, 1 };
        // Value: 0 1
        final LhaHuffmanDecoder decoder = new LhaHuffmanDecoder(length);
        assertEquals(0, decoder.decodeSymbol(createBitInputStream(0x00))); // 0xxx xxxx
        assertEquals(1, decoder.decodeSymbol(createBitInputStream(0x80))); // 1xxx xxxx
    }

    @Test
    void testTree2() throws IOException {
        final int[] length = { 1, 0, 1 };
        // Value: 0 1 2
        final LhaHuffmanDecoder decoder = new LhaHuffmanDecoder(length);
        assertEquals(0, decoder.decodeSymbol(createBitInputStream(0x00))); // 0xxx xxxx
        assertEquals(2, decoder.decodeSymbol(createBitInputStream(0x80))); // 1xxx xxxx
    }

    @Test
    void testTree3() throws IOException {
        final int[] length = { 2, 0, 1, 2 };
        // Value: 0 1 2 3
        final LhaHuffmanDecoder decoder = new LhaHuffmanDecoder(length);
        assertEquals(2, decoder.decodeSymbol(createBitInputStream(0x00))); // 0xxx xxxx
        assertEquals(0, decoder.decodeSymbol(createBitInputStream(0x80))); // 10xx xxxx
        assertEquals(3, decoder.decodeSymbol(createBitInputStream(0xc0))); // 11xx xxxx
    }

    @Test
    void testTree4() throws IOException {
        final int[] length = { 2, 0, 0, 2, 1 };
        // Value: 0 1 2 3 4
        final LhaHuffmanDecoder decoder = new LhaHuffmanDecoder(length);
        assertEquals(4, decoder.decodeSymbol(createBitInputStream(0x00))); // 0xxx xxxx
        assertEquals(0, decoder.decodeSymbol(createBitInputStream(0x80))); // 10xx xxxx
        assertEquals(3, decoder.decodeSymbol(createBitInputStream(0xc0))); // 11xx xxxx
    }

    @Test
    void testTree5() throws IOException {
        final int[] length = { 1, 0, 2, 3, 3 };
        // Value: 0 1 2 3 4
        final LhaHuffmanDecoder decoder = new LhaHuffmanDecoder(length);
        assertEquals(0, decoder.decodeSymbol(createBitInputStream(0x00))); // 0xxx xxxx
        assertEquals(2, decoder.decodeSymbol(createBitInputStream(0x80))); // 10xx xxxx
        assertEquals(3, decoder.decodeSymbol(createBitInputStream(0xc0))); // 110x xxxx
        assertEquals(4, decoder.decodeSymbol(createBitInputStream(0xe0))); // 111x xxxx
    }

    @Test
    void testTree6() throws IOException {
        final int[] length = { 0, 0, 0, 0, 1, 1 };
        // Value: 0 1 2 3 4 5
        final LhaHuffmanDecoder decoder = new LhaHuffmanDecoder(length);
        assertEquals(4, decoder.decodeSymbol(createBitInputStream(0x00))); // 0xxx xxxx
        assertEquals(5, decoder.decodeSymbol(createBitInputStream(0x80))); // 1xxx xxxx
    }

    @Test
    void testTree7() throws IOException {
        final int[] length = { 4, 2, 3, 0, 5, 5, 1 };
        // Value: 0 1 2 3 4 5 6
        final LhaHuffmanDecoder decoder = new LhaHuffmanDecoder(length);
        assertEquals(6, decoder.decodeSymbol(createBitInputStream(0x00))); // 0xxx xxxx
        assertEquals(1, decoder.decodeSymbol(createBitInputStream(0x80))); // 10xx xxxx
        assertEquals(2, decoder.decodeSymbol(createBitInputStream(0xc0))); // 110x xxxx
        assertEquals(0, decoder.decodeSymbol(createBitInputStream(0xe0))); // 1110 xxxx
        assertEquals(4, decoder.decodeSymbol(createBitInputStream(0xf0))); // 1111 0xxx
        assertEquals(5, decoder.decodeSymbol(createBitInputStream(0xf8))); // 1111 1xxx
    }

    @Test
    void testTree8() throws IOException {
        final int[] length = { 5, 6, 6, 0, 0, 8, 7, 7, 7, 4, 3, 2, 2, 4, 5, 5, 5, 4, 8 };
        // Value: 0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18
        final LhaHuffmanDecoder decoder = new LhaHuffmanDecoder(length);
        assertEquals(11, decoder.decodeSymbol(createBitInputStream(0x00))); // 00xx xxxx
        assertEquals(12, decoder.decodeSymbol(createBitInputStream(0x40))); // 01xx xxxx
        assertEquals(10, decoder.decodeSymbol(createBitInputStream(0x80))); // 100x xxxx
        assertEquals(9, decoder.decodeSymbol(createBitInputStream(0xa0))); // 1010 xxxx
        assertEquals(13, decoder.decodeSymbol(createBitInputStream(0xb0))); // 1011 xxxx
        assertEquals(17, decoder.decodeSymbol(createBitInputStream(0xc0))); // 1100 xxxx
        assertEquals(0, decoder.decodeSymbol(createBitInputStream(0xd0))); // 1101 0xxx
        assertEquals(14, decoder.decodeSymbol(createBitInputStream(0xd8))); // 1101 1xxx
        assertEquals(15, decoder.decodeSymbol(createBitInputStream(0xe0))); // 1110 0xxx
        assertEquals(16, decoder.decodeSymbol(createBitInputStream(0xe8))); // 1110 1xxx
        assertEquals(1, decoder.decodeSymbol(createBitInputStream(0xf0))); // 1111 00xx
        assertEquals(2, decoder.decodeSymbol(createBitInputStream(0xf4))); // 1111 01xx
        assertEquals(6, decoder.decodeSymbol(createBitInputStream(0xf8))); // 1111 100x
        assertEquals(7, decoder.decodeSymbol(createBitInputStream(0xfa))); // 1111 101x
        assertEquals(8, decoder.decodeSymbol(createBitInputStream(0xfc))); // 1111 110x
        assertEquals(5, decoder.decodeSymbol(createBitInputStream(0xfe))); // 1111 1110
        assertEquals(18, decoder.decodeSymbol(createBitInputStream(0xff))); // 1111 1111
    }
}
