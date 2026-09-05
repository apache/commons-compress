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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The concurrent compression constructor must produce exactly the bytes of the single-threaded encoder.
 */
class ParallelBZip2CompressionTest {

    private static ExecutorService pool;
    private static ExecutorService singleThread;

    static Stream<Arguments> cases() {
        final List<Arguments> list = new ArrayList<>();
        for (final String name : BZip2DifferentialTest.generatedInputs().keySet()) {
            for (final int blockSize : new int[] { 1, 9 }) {
                list.add(Arguments.of(name, blockSize));
            }
        }
        return list.stream();
    }

    private static byte[] parallel(final byte[] data, final int blockSize, final ExecutorService executor, final int maxInFlight, final int writeSize)
            throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (BZip2CompressorOutputStream os = new BZip2CompressorOutputStream(out, blockSize, executor, maxInFlight)) {
            if (writeSize == 0) {
                for (final byte b : data) {
                    os.write(b);
                }
            } else {
                for (int i = 0; i < data.length; i += writeSize) {
                    os.write(data, i, Math.min(writeSize, data.length - i));
                }
            }
        }
        return out.toByteArray();
    }

    private static byte[] sequential(final byte[] data, final int blockSize) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (BZip2CompressorOutputStream os = new BZip2CompressorOutputStream(out, blockSize)) {
            os.write(data);
        }
        return out.toByteArray();
    }

    @BeforeAll
    static void startPools() {
        pool = Executors.newFixedThreadPool(8);
        singleThread = Executors.newFixedThreadPool(1);
    }

    @AfterAll
    static void stopPools() {
        pool.shutdownNow();
        singleThread.shutdownNow();
    }

    @ParameterizedTest(name = "{0} blockSize={1}")
    @MethodSource("cases")
    void testByteIdentical(final String name, final int blockSize) throws IOException {
        final byte[] data = BZip2DifferentialTest.generatedInputs().get(name);
        final byte[] expected = sequential(data, blockSize);
        assertArrayEquals(expected, parallel(data, blockSize, pool, 4, data.length + 1), name + " whole");
        assertArrayEquals(expected, parallel(data, blockSize, pool, 4, 7), name + " 7-byte writes");
        assertArrayEquals(expected, parallel(data, blockSize, singleThread, 1, 8192), name + " single thread");
    }

    @Test
    void testLongRuns() throws IOException {
        // RLE1 packs runs 255:5, so blocks hold much more raw input than the accumulation buffer's initial size; also crosses block boundaries mid-run.
        final byte[] data = new byte[6_000_000];
        Arrays.fill(data, (byte) 'x');
        for (int i = 0; i < data.length; i += 10_007) {
            data[i] = (byte) (i % 251);
        }
        final byte[] expected = sequential(data, 1);
        assertArrayEquals(expected, parallel(data, 1, pool, 4, 65536));
    }

    @Test
    void testMaxConcurrentInFlightValidation() {
        assertThrows(IllegalArgumentException.class, () -> new BZip2CompressorOutputStream(new ByteArrayOutputStream(), 9, pool, 0));
    }

    @Test
    void testRoundTrip() throws IOException {
        final byte[] data = BZip2DifferentialTest.generatedInputs().get("text-250k-3-blocks-at-1");
        final byte[] compressed = parallel(data, 1, pool, 4, 4096);
        final ByteArrayOutputStream decoded = new ByteArrayOutputStream();
        try (InputStream in = new BZip2CompressorInputStream(new ByteArrayInputStream(compressed), true)) {
            final byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf, 0, buf.length)) >= 0) {
                decoded.write(buf, 0, n);
            }
        }
        assertArrayEquals(data, decoded.toByteArray());
    }

    @Test
    void testSingleByteWritesAndEmpty() throws IOException {
        final byte[] data = BZip2DifferentialTest.generatedInputs().get("random-150k");
        assertArrayEquals(sequential(data, 1), parallel(data, 1, pool, 2, 0));
        final byte[] empty = {};
        assertArrayEquals(sequential(empty, 9), parallel(empty, 9, pool, 2, 0));
    }
}
