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
package org.apache.commons.compress.compressors.alg;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.compress.utils.BitInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class HuffmanDecoderTest {

    @Test
    void testCreateHuffmanDecodingTablesWithLargeAlphaSize() {
        // Use a codeLengths array with length equal to MAX_ALPHA_SIZE (258) to test array bounds.
        final int[] codeLengths = new int[258];
        for (int i = 0; i < codeLengths.length; i++) {
            // Use all code lengths within valid range [1, 20]
            codeLengths[i] = (char) ((i % 20) + 1);
        }
        final HuffmanDecoder decoder = assertDoesNotThrow(
                () -> new HuffmanDecoder(codeLengths, codeLengths.length, 1, 20),
                "HuffmanDecoder constructor should not throw for valid codeLengths array of MAX_ALPHA_SIZE");
        assertEquals(decoder.getMinLength(), 1, "Minimum code length should be 1");
        assertEquals(decoder.getMaxLength(), 20, "Maximum code length should be 20");
    }

    static Stream<Arguments> testDecodeSymbols() {
        return Stream.of(
                // Simple case
                //   Symbol 0: 10
                //   Symbol 1: 11
                //   Symbol 3: 0
                Arguments.of(
                        new int[] {2, 2, 0, 1},
                        new byte[] {(byte) 0b10_11_0_0_0_0},
                        Arrays.asList(0, 1, 3, 3, 3, 3),
                        ByteOrder.BIG_ENDIAN),
                Arguments.of(
                        new int[] {2, 2, 0, 1},
                        new byte[] {(byte) 0b01_11_0_0_0_0},
                        Arrays.asList(3, 3, 3, 3, 1, 0),
                        ByteOrder.LITTLE_ENDIAN),

                // Across byte boundary
                // Symbol 0: 10
                // Symbol 1: 11
                // Symbol 2: 0
                Arguments.of(
                        new int[] {2, 2, 1, 0},
                        new byte[] {(byte) 0b0_11_10_11_1, (byte) 0b0_11_10_11_0},
                        Arrays.asList(2, 1, 0, 1, 0, 1, 0, 1, 2),
                        ByteOrder.BIG_ENDIAN),
                Arguments.of(
                        new int[] {2, 2, 1, 0},
                        new byte[] {(byte) 0b1_11_01_11_0, (byte) 0b0_11_01_11_0},
                        Arrays.asList(2, 1, 0, 1, 0, 1, 0, 1, 2),
                        ByteOrder.LITTLE_ENDIAN));
    }

    @ParameterizedTest
    @MethodSource
    void testDecodeSymbols(
            final int[] codeLengths,
            final byte[] inputData,
            final List<Integer> expectedSymbols,
            final ByteOrder byteOrder)
            throws IOException {
        final HuffmanDecoder decoder = new HuffmanDecoder(codeLengths);

        final Collection<Integer> actualSymbols = new ArrayList<>();
        try (BitInputStream in = new BitInputStream(new ByteArrayInputStream(inputData), byteOrder)) {
            for (int i = 0; i < expectedSymbols.size(); i++) {
                actualSymbols.add(decoder.decodeSymbol(in));
            }
        }
        assertEquals(expectedSymbols, actualSymbols, "Decoded symbols do not match expected symbols");
    }
}
