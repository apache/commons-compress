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

package org.apache.commons.compress.huffman;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.utils.BitInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class HuffmanDecoderTest {

    static Stream<Arguments> testDecodeSymbols() {
        // @formatter:off
        return Stream.of(
                // One level, two symbols
                //   Symbol 0: 0
                //   Symbol 1: 1
                Arguments.of(
                        new int[] {1, 1},
                        new int[] {0b1_0_1_0_0_1_1_1},
                        Arrays.asList(1, 0, 1, 0, 0, 1, 1, 1),
                        ByteOrder.BIG_ENDIAN),
                Arguments.of(
                        new int[] {1, 1},
                        new int[] {0b1_1_1_0_0_1_0_1},
                        Arrays.asList(1, 0, 1, 0, 0, 1, 1, 1),
                        ByteOrder.LITTLE_ENDIAN),
                // Two levels, three symbols
                //   Symbol 0: 10
                //   Symbol 1: 11
                //   Symbol 3: 0
                Arguments.of(
                        new int[] {2, 2, 0, 1},
                        new int[] {0b10_11_0_0_0_0},
                        Arrays.asList(0, 1, 3, 3, 3, 3),
                        ByteOrder.BIG_ENDIAN),
                Arguments.of(
                        new int[] {2, 2, 0, 1},
                        new int[] {0b01_11_0_0_0_0},
                        Arrays.asList(3, 3, 3, 3, 1, 0),
                        ByteOrder.LITTLE_ENDIAN),
                // Two levels, three symbols, decode across byte boundary
                // Symbol 0: 10
                // Symbol 1: 11
                // Symbol 2: 0
                Arguments.of(
                        new int[] {2, 2, 1, 0},
                        new int[] {0b0_11_10_11_1, 0b0_11_10_11_0},
                        Arrays.asList(2, 1, 0, 1, 0, 1, 0, 1, 2),
                        ByteOrder.BIG_ENDIAN),
                Arguments.of(
                        new int[] {2, 2, 1, 0},
                        new int[] {0b1_11_01_11_0, 0b0_11_01_11_0},
                        Arrays.asList(2, 1, 0, 1, 0, 1, 0, 1, 2),
                        ByteOrder.LITTLE_ENDIAN),
                // Five levels, six symbols, decode across byte boundary
                // Symbol 0: 1110
                // Symbol 1: 10
                // Symbol 2: 110
                // Symbol 4: 11110
                // Symbol 5: 11111
                // Symbol 6: 0
                Arguments.of(
                        new int[] {4, 2, 3, 0, 5, 5, 1},
                        new int[] {0b0_10_110_11, 0b10_11110_1, 0b1111_0000},
                        Arrays.asList(6, 1, 2, 0, 4, 5),
                        ByteOrder.BIG_ENDIAN),
                Arguments.of(
                        new int[] {4, 2, 3, 0, 5, 5, 1},
                        new int[] {0b11_011_01_0, 0b1_01111_01, 0b0000_1111},
                        Arrays.asList(6, 1, 2, 0, 4, 5),
                        ByteOrder.LITTLE_ENDIAN));
        // @formatter:on
    }

    private int decodeSymbol(final HuffmanDecoder decoder, final int... data) throws IOException {
        try (BitInputStream in = new BitInputStream(new ByteArrayInputStream(AbstractTest.toByteArray(data)), ByteOrder.BIG_ENDIAN)) {
            return decoder.decodeSymbol(in);
        }
    }

    @Test
    void testCodeLengthBelowMinCodeLength() throws Exception {
        final int[] codeLengths = new int[] {4, 2, 3, 0, 5, 5, 1};
        final CompressorException e = assertThrows(CompressorException.class, () -> new HuffmanDecoder(codeLengths, 1, 5),
                "Expected CompressorException for code length below min code length");
        assertEquals("Invalid code length at symbol 3: 0 (expected in [1, 5])", e.getMessage());
    }

    @Test
    void testCodeLengthExceedingMaxCodeLength() throws Exception {
        final int[] codeLengths = new int[] {4, 2, 3, 0, 5, 5, 1};
        final CompressorException e = assertThrows(CompressorException.class, () -> new HuffmanDecoder(codeLengths, 0, 4),
                "Expected CompressorException for code length exceeding max code length");
        assertEquals("Invalid code length at symbol 4: 5 (expected in [0, 4])", e.getMessage());
    }

    @Test
    void testCreateHuffmanDecodingTablesWithLargeAlphaSize() {
        // Use a codeLengths array with length equal to MAX_ALPHA_SIZE (258) to test array bounds.
        final int[] codeLengths = new int[258];
        for (int i = 0; i < codeLengths.length; i++) {
            // Use all code lengths within valid range [1, 20]
            codeLengths[i] = (char) (i % 20 + 1);
        }
        final HuffmanDecoder decoder = assertDoesNotThrow(() -> new HuffmanDecoder(codeLengths, 1, 20),
                "HuffmanDecoder constructor should not throw for valid codeLengths array of MAX_ALPHA_SIZE");
        assertEquals(decoder.getMinLength(), 1, "Minimum code length should be 1");
        assertEquals(decoder.getMaxLength(), 20, "Maximum code length should be 20");
    }

    @ParameterizedTest
    @MethodSource
    void testDecodeSymbols(final int[] codeLengths, final int[] inputData, final List<Integer> expectedSymbols, final ByteOrder byteOrder) throws IOException {
        final HuffmanDecoder decoder = new HuffmanDecoder(codeLengths);
        final Collection<Integer> actualSymbols = new ArrayList<>();
        try (BitInputStream in = new BitInputStream(new ByteArrayInputStream(AbstractTest.toByteArray(inputData)), byteOrder)) {
            for (int i = 0; i < expectedSymbols.size(); i++) {
                actualSymbols.add(decoder.decodeSymbol(in));
            }
        }
        assertEquals(expectedSymbols, actualSymbols, "Decoded symbols do not match expected symbols");
    }

    @Test
    void testInvalidBitstream() throws Exception {
        final int[] length = { 4, 2, 3, 0, 5, 0, 1 };
        // Value: 0 1 2 3 4 5 6
        final HuffmanDecoder decoder = new HuffmanDecoder(length);
        assertEquals(6, decodeSymbol(decoder, 0x00)); // 0xxx xxxx
        assertEquals(1, decodeSymbol(decoder, 0x80)); // 10xx xxxx
        assertEquals(2, decodeSymbol(decoder, 0xc0)); // 110x xxxx
        assertEquals(0, decodeSymbol(decoder, 0xe0)); // 1110 xxxx
        assertEquals(4, decodeSymbol(decoder, 0xf0)); // 1111 0xxx
        final CompressorException e = assertThrows(CompressorException.class, () -> decodeSymbol(decoder, 0xf8),
                "Expected CompressorException for invalid bitstream");
        assertEquals("Invalid Huffman code: 62", e.getMessage());
    }

    @Test
    void testMaxSupportedCodeLengthExceeded() {
        final int[] codeLengths = new int[] {1, 1};
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new HuffmanDecoder(codeLengths, 0, 31),
                "Expected IllegalArgumentException for max code length exceeding supported limit");
        assertEquals("maxCodeLength (31) exceeds supported limit (30)", e.getMessage());
    }

    @Test
    void testMinCodeLengthBelowSupportedLimit() {
        final int[] codeLengths = new int[] {1, 1};
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new HuffmanDecoder(codeLengths, -1, 30),
                "Expected IllegalArgumentException for min code length below supported limit");
        assertEquals("minCodeLength (-1) not within range [0, 30)", e.getMessage());
    }

    @Test
    void testMinCodeLengthExceedingMaxCodeLength() {
        final int[] codeLengths = new int[] {1, 1};
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new HuffmanDecoder(codeLengths, 17, 16),
                "Expected IllegalArgumentException for min code length exceeding max code length");
        assertEquals("minCodeLength (17) not within range [0, 16)", e.getMessage());
    }

    @Test
    void testNoCodeLengths() throws Exception {
        final CompressorException e = assertThrows(CompressorException.class, () -> new HuffmanDecoder(new int[0]),
                "Expected CompressorException for empty code length list");
        assertEquals("Empty code length list", e.getMessage());
    }

    @Test
    void testNoLeafNodes() throws Exception {
        final HuffmanDecoder decoder = new HuffmanDecoder(new int[] { 0, 0, 0, 0, 0 });
        final CompressorException e = assertThrows(CompressorException.class, () -> decodeSymbol(decoder, 0, 0, 0, 0),
                "Expected CompressorException when decoding symbols for tree with no leaf nodes");
        assertEquals("Invalid Huffman code: 0", e.getMessage());
    }

    @Test
    void testReadEof() throws Exception {
        final int[] length = { 4, 2, 3, 0, 5, 5, 1 };
        // Value: 0 1 2 3 4 5 6
        final HuffmanDecoder decoder = new HuffmanDecoder(length);
        try (BitInputStream in = new BitInputStream(new ByteArrayInputStream(AbstractTest.toByteArray(0b11111_110)), ByteOrder.BIG_ENDIAN)) {
            assertEquals(5, decoder.decodeSymbol(in)); // 1111 1xxx
            assertEquals(2, decoder.decodeSymbol(in)); // 110x xxxx
            final EOFException e = assertThrows(EOFException.class, () -> decoder.decodeSymbol(in),
                    "Expected EOFException for end of stream");
            assertEquals("Truncated Huffman bit stream", e.getMessage());
        }
    }

    @Test
    void testSingleCodeLength() throws Exception {
        final int[] length = { 1 };
        // Value: 0
        final HuffmanDecoder decoder = new HuffmanDecoder(length);
        assertEquals(0, decodeSymbol(decoder, 0x00)); // 0xxx xxxx
        final CompressorException e = assertThrows(CompressorException.class, () -> decodeSymbol(decoder, 0x80),
                "Expected CompressorException for invalid bitstream");
        assertEquals("Invalid Huffman code: 2", e.getMessage());
    }
}
