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

import org.junit.jupiter.api.Test;

class HuffmanDecoderTest {
    private static final int MIN_CODE_LEN = 1;
    private static final int MAX_CODE_LEN = 20;

    @Test
    void testCreateHuffmanDecodingTablesWithLargeAlphaSize() {
        // Use a codeLengths array with length equal to MAX_ALPHA_SIZE (258) to test array bounds.
        final int[] codeLengths = new int[258];
        for (int i = 0; i < codeLengths.length; i++) {
            // Use all code lengths within valid range [1, 20]
            codeLengths[i] = (char) ((i % MAX_CODE_LEN) + 1);
        }
        final HuffmanDecoder decoder = assertDoesNotThrow(
                () -> new HuffmanDecoder(codeLengths, codeLengths.length, MIN_CODE_LEN, MAX_CODE_LEN),
                "HuffmanDecoder constructor should not throw for valid codeLengths array of MAX_ALPHA_SIZE");
        assertEquals(decoder.getMinLength(), 1, "Minimum code length should be 1");
    }
}
