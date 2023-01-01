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
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.compress.harmony.pack200.BHSDCodec;
import org.apache.commons.compress.harmony.pack200.Codec;
import org.apache.commons.compress.harmony.pack200.CodecEncoding;
import org.apache.commons.compress.harmony.pack200.Pack200Exception;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for BHSDCodec
 */
public class BHSDCodecTest {

    static Stream<Arguments> encodeDecodeRange() {
        return IntStream.range(1, 116).mapToObj(Arguments::of);
    }

    @Test
    public void testDeltaEncodings() throws IOException, Pack200Exception {
        final Codec c = Codec.UDELTA5;
        final int[] sequence = {0, 2, 4, 2, 2, 4};
        final byte[] encoded = c.encode(sequence);
        final int[] decoded = c.decodeInts(6, new ByteArrayInputStream(encoded));
        for (int i = 0; i < decoded.length; i++) {
            assertEquals(sequence[i], decoded[i]);
        }
    }

    @ParameterizedTest
    @MethodSource("encodeDecodeRange")
    public void testEncodeDecode(final int i) throws IOException, Pack200Exception {
        final BHSDCodec codec = (BHSDCodec) CodecEncoding.getCodec(i, null, null);

        if (!codec.isDelta()) {
            // Test encode-decode with a selection of numbers within the
            // range of the codec
            final long largest = codec.largest();
            long smallest = codec.isSigned() ? codec.smallest() : 0;
            if (smallest < Integer.MIN_VALUE) {
                smallest = Integer.MIN_VALUE;
            }
            final long difference = (largest - smallest) / 4;
            for (long j = smallest; j <= largest; j += difference) {
                if (j > Integer.MAX_VALUE) {
                    break;
                }
                final byte[] encoded = codec.encode((int) j, 0);
                long decoded = 0;
                try {
                    decoded = codec.decode(
                            new ByteArrayInputStream(encoded), 0);
                } catch (final EOFException e) {
                    System.out.println(e);
                }
                if (j != decoded) {
                    fail("Failed with codec: " + i + ", " + codec
                            + " expected: " + j + ", got: " + decoded);
                }
            }
        }

        // Test encode-decode with 0
        assertEquals(0, codec.decode(new ByteArrayInputStream(codec.encode(
                0, 0)), 0));
    }

}
