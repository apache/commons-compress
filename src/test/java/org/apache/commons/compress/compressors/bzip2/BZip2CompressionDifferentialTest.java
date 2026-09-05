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

package org.apache.commons.compress.compressors.bzip2;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Differential test for the compressor: {@link BZip2CompressorOutputStream} must produce exactly the bytes {@link LegacyBZip2Encoder} (the implementation
 * before the speed-up) produces, for every input and write pattern, and the result must decompress to the input.
 */
class BZip2CompressionDifferentialTest extends AbstractTest {

    enum WritePattern {
        SINGLE_BYTES, WHOLE, CHUNKS_7, MIXED
    }

    private static final String[] FIXTURES = { "lorem-ipsum.txt.bz2", "bla.tar.bz2", "org/apache/commons/compress/COMPRESS-651/my10m.tar.bz2" };

    static byte[] compress(final boolean legacy, final byte[] input, final int blockSize, final WritePattern pattern) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(input.length / 2 + 64);
        try (OutputStream out = legacy ? new LegacyBZip2Encoder(baos, blockSize) : new BZip2CompressorOutputStream(baos, blockSize)) {
            final Random rnd = new Random(pattern.ordinal() * 31L + input.length);
            int pos = 0;
            while (pos < input.length) {
                switch (pattern) {
                case SINGLE_BYTES:
                    out.write(input[pos++]);
                    break;
                case WHOLE:
                    out.write(input, 0, input.length);
                    pos = input.length;
                    break;
                case CHUNKS_7: {
                    final int n = Math.min(7, input.length - pos);
                    out.write(input, pos, n);
                    pos += n;
                    break;
                }
                default: {
                    final int r = rnd.nextInt(100);
                    if (r < 20) {
                        out.write(input[pos++]);
                    } else {
                        final int n = Math.min(input.length - pos, r < 60 ? 1 + rnd.nextInt(64) : 1 + rnd.nextInt(200_000));
                        out.write(input, pos, n);
                        pos += n;
                    }
                }
                }
            }
        }
        return baos.toByteArray();
    }

    static byte[] decompress(final byte[] compressed) throws IOException {
        try (InputStream in = new BZip2CompressorInputStream(new java.io.ByteArrayInputStream(compressed))) {
            return IOUtils.toByteArray(in);
        }
    }

    static Stream<Arguments> fixtureCases() throws IOException {
        final List<Arguments> list = new ArrayList<>();
        for (final String fixture : FIXTURES) {
            for (final int blockSize : new int[] { 1, 9 }) {
                list.add(Arguments.of(fixture, blockSize));
            }
        }
        return list.stream();
    }

    static Stream<Arguments> generatedCases() {
        final List<Arguments> list = new ArrayList<>();
        for (final String name : BZip2DifferentialTest.generatedInputs().keySet()) {
            for (final int blockSize : new int[] { 1, 9 }) {
                for (final WritePattern pattern : WritePattern.values()) {
                    list.add(Arguments.of(name, blockSize, pattern));
                }
            }
        }
        return list.stream();
    }

    @ParameterizedTest(name = "{0} blockSize={1}")
    @MethodSource("fixtureCases")
    void testFixtures(final String fixture, final int blockSize) throws IOException {
        final byte[] input = decompress(readAllBytes(fixture));
        final byte[] expected = compress(true, input, blockSize, WritePattern.WHOLE);
        assertArrayEquals(expected, compress(false, input, blockSize, WritePattern.WHOLE), fixture);
        assertArrayEquals(input, decompress(expected), fixture + " round trip");
    }

    @ParameterizedTest(name = "{0} blockSize={1} pattern={2}")
    @MethodSource("generatedCases")
    void testGeneratedInputs(final String name, final int blockSize, final WritePattern pattern) throws IOException {
        final byte[] input = BZip2DifferentialTest.generatedInputs().get(name);
        final byte[] expected = compress(true, input, blockSize, pattern);
        assertArrayEquals(expected, compress(false, input, blockSize, pattern), name);
        if (pattern == WritePattern.WHOLE) {
            assertArrayEquals(input, decompress(expected), name + " round trip");
        }
    }
}
