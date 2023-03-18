/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.commons.compress.harmony.pack200.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;

import org.apache.commons.compress.harmony.pack200.Codec;
import org.apache.commons.compress.harmony.pack200.Pack200Exception;
import org.apache.commons.compress.harmony.pack200.PopulationCodec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class PopulationCodecTest {

    static Stream<Arguments> populationCodec() {
        return Stream.of(
                Arguments.of(new byte[] { 4, 5, 6, 4, 2, 1, 3, 0, 7 }, new long[] { 5, 4, 6, 7 }, Codec.BYTE1),
                // Codec.SIGNED5 can be trivial for small n, because the encoding is 2n
                // if even, 2n-1 if odd
                // Therefore they're left here to explain what the values are :-)
                Arguments.of(new byte[] { 4 * 2, 4 * 2 - 1, 6 * 2, 4 * 2, 2 * 2, 1 * 2, 3 * 2, 0, 7 * 2 }, new long[] { -4, 4, 6, 7 }, Codec.SIGNED5),
                Arguments.of(new byte[] { 4 * 2 - 1, 4 * 2, 6 * 2, 4 * 2, 2 * 2, 1 * 2, 3 * 2, 0, 7 * 2 }, new long[] { 4, -4, 6, 7 }, Codec.SIGNED5),
                Arguments.of(new byte[] { 1, 1, 1 }, new long[] { 1 }, Codec.BYTE1),
                Arguments.of(new byte[] { 2, 2, 1 }, new long[] { 2 }, Codec.BYTE1),
                Arguments.of(new byte[] { 1, 1, 2 }, new long[] { -1 }, Codec.SIGNED5),
                Arguments.of(new byte[] { 2, 2, 0, 1, 3 }, new long[] { 3, 2 }, Codec.BYTE1),
                Arguments.of(new byte[] { 1, 2, 3, 4, 4, 2, 3, 4, 0, 1 }, new long[] { 2, 3, 4, 1 }, Codec.BYTE1),
                Arguments.of(new byte[] { 3, 2, 1, 4, 4, 2, 3, 4, 0, 1 }, new long[] { 2, 1, 4, 1 }, Codec.BYTE1),
                Arguments.of(new byte[] { 3, 2, 1, 4, 1, 2, 3, 4, 0, 1 }, new long[] { 2, 1, 4, 1 }, Codec.BYTE1)
        );
    }

    @Test
    public void testEncodeSingleValue() {
        assertThrows(Pack200Exception.class, () -> new PopulationCodec(Codec.SIGNED5, Codec.SIGNED5, Codec.UDELTA5).encode(5),
                "Should not allow a single value to be encoded as we don't know which codec to use");
        assertThrows(Pack200Exception.class, () -> new PopulationCodec(Codec.SIGNED5, Codec.SIGNED5, Codec.UDELTA5).encode(5, 8),
                "Should not allow a single value to be encoded as we don't know which codec to use");
    }

    @ParameterizedTest
    @MethodSource("populationCodec")
    public void testPopulationCodec(final byte[] data, final long[] expectedResult, final Codec codec) throws IOException, Pack200Exception {
        try (InputStream in = new ByteArrayInputStream(data)) {
            final int[] result = new PopulationCodec(codec, codec, codec).decodeInts(
                    expectedResult.length, in);
            assertEquals(expectedResult.length, result.length);
            for (int i = 0; i < expectedResult.length; i++) {
                assertEquals(expectedResult[i], result[i]);
            }
            assertEquals(0, in.available());
        }
    }

}
