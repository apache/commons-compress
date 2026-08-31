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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import org.apache.commons.compress.AbstractTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The concurrent decompression constructor must produce exactly the bytes of the single-threaded decoder for valid streams, and fail with an
 * {@link IOException} for corrupt or truncated ones.
 */
class ParallelBZip2DecompressionTest extends AbstractTest {

    private static ExecutorService pool;
    private static ExecutorService singleThread;

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

    static byte[] sequential(final byte[] compressed, final boolean concat) throws IOException {
        try (InputStream in = new BZip2CompressorInputStream(new ByteArrayInputStream(compressed), concat)) {
            return readAll(in);
        }
    }

    static byte[] parallel(final byte[] compressed, final boolean concat, final ExecutorService executor) throws IOException {
        try (InputStream in = new BZip2CompressorInputStream(new ByteArrayInputStream(compressed), concat, executor, 16)) {
            return readAll(in);
        }
    }

    private static byte[] readAll(final InputStream in) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf, 0, buf.length)) >= 0) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    static Stream<Arguments> cases() {
        final List<Arguments> list = new ArrayList<>();
        for (final String name : BZip2DifferentialTest.generatedInputs().keySet()) {
            for (final int blockSize : new int[] { 1, 9 }) {
                list.add(Arguments.of(name, blockSize));
            }
        }
        return list.stream();
    }

    @ParameterizedTest(name = "{0} blockSize={1}")
    @MethodSource("cases")
    void testGeneratedInputs(final String name, final int blockSize) throws IOException {
        final byte[] compressed = BZip2DifferentialTest.compressed(name, blockSize);
        final byte[] expected = BZip2DifferentialTest.generatedInputs().get(name);
        assertArrayEquals(expected, parallel(compressed, false, pool), name);
        assertArrayEquals(expected, parallel(compressed, true, singleThread), name);
    }

    @Test
    void testByteAtATimeAndCounters() throws IOException {
        final byte[] compressed = BZip2DifferentialTest.compressed("text-250k-3-blocks-at-1", 1);
        final byte[] expected = BZip2DifferentialTest.generatedInputs().get("text-250k-3-blocks-at-1");
        try (BZip2CompressorInputStream in = new BZip2CompressorInputStream(new ByteArrayInputStream(compressed), false, pool, 16)) {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            int b;
            while ((b = in.read()) >= 0) {
                out.write(b);
            }
            assertArrayEquals(expected, out.toByteArray());
            assertEquals(expected.length, in.getUncompressedCount());
            assertTrue(in.getCompressedCount() >= compressed.length, "compressed count");
        }
    }

    @Test
    void testMaxConcurrentInFlight() throws IOException {
        final byte[] compressed = BZip2DifferentialTest.compressed("text-250k-3-blocks-at-1", 1);
        final byte[] expected = BZip2DifferentialTest.generatedInputs().get("text-250k-3-blocks-at-1");
        try (InputStream in = new BZip2CompressorInputStream(new ByteArrayInputStream(compressed), true, pool, 1)) {
            assertArrayEquals(expected, readAll(in));
        }
        assertThrows(IllegalArgumentException.class, () -> new BZip2CompressorInputStream(new ByteArrayInputStream(compressed), true, pool, 0));
    }

    @Test
    void testConcatenatedStreams() throws IOException {
        final ByteArrayOutputStream cat = new ByteArrayOutputStream();
        cat.write(BZip2DifferentialTest.compressed("text-250k-3-blocks-at-1", 1));
        cat.write(BZip2DifferentialTest.compressed("empty", 9));
        cat.write(BZip2DifferentialTest.compressed("runs-150k", 9));
        final byte[] compressed = cat.toByteArray();
        assertArrayEquals(sequential(compressed, true), parallel(compressed, true, pool));
        assertArrayEquals(sequential(compressed, false), parallel(compressed, false, pool));
    }

    @Test
    void testCorruptAndTruncated() throws IOException {
        final byte[] base = BZip2DifferentialTest.compressed("text-250k-3-blocks-at-1", 1);
        final Random rnd = new Random(17);
        for (int i = 0; i < 25; i++) {
            final byte[] corrupted = base.clone();
            corrupted[rnd.nextInt(base.length)] ^= (byte) (1 << rnd.nextInt(8));
            assertThrows(IOException.class, () -> parallel(corrupted, false, pool), "flip " + i);
        }
        for (final int cut : new int[] { 0, 1, 3, 5, 100, base.length / 2, base.length - 5, base.length - 1 }) {
            final byte[] truncated = Arrays.copyOf(base, cut);
            assertThrows(IOException.class, () -> parallel(truncated, false, pool), "cut " + cut);
        }
        // last-block CRC corruption must be caught even though the block decodes
        final byte[] crcFlip = base.clone();
        crcFlip[base.length - 6] ^= 4;
        assertThrows(IOException.class, () -> parallel(crcFlip, false, pool));
    }

    @Test
    void testFixtures() throws IOException {
        for (final String fixture : new String[] { "bla.txt.bz2", "bla.tar.bz2", "multiple.bz2", "COMPRESS-131.bz2", "lbzip2_32767.bz2",
                "lorem-ipsum.txt.bz2", "empty.txt.bz2", "org/apache/commons/compress/COMPRESS-651/my10m.tar.bz2" }) {
            final byte[] compressed = readAllBytes(fixture);
            assertArrayEquals(sequential(compressed, true), parallel(compressed, true, pool), fixture);
        }
    }

    @Test
    void testMissingDelimiterScanCap() {
        // A block magic followed by megabytes of garbage holding no delimiter: the scan must give up at its cap instead of buffering all remaining input.
        final byte[] garbage = new byte[5 * 1024 * 1024];
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write('B');
        stream.write('Z');
        stream.write('h');
        stream.write('9');
        for (final int b : new int[] { 0x31, 0x41, 0x59, 0x26, 0x53, 0x59, 0, 0, 0, 0 }) {
            stream.write(b);
        }
        stream.write(garbage, 0, garbage.length);
        final IOException e = assertThrows(IOException.class, () -> parallel(stream.toByteArray(), false, pool));
        assertTrue(e.getMessage().contains("no block delimiter"), e.getMessage());
    }

    @Test
    void testRandomisedBlock() throws IOException {
        final byte[] original = BZip2DifferentialTest.sparseRuns(new Random(31), 60_000);
        byte[] stream = null;
        for (int attempt = 0; attempt < 20 && stream == null; attempt++) {
            stream = RandomisedBZip2Streams.randomisedStream(original, 1);
            if (stream == null) {
                original[attempt * 7919 % original.length] ^= 0x10;
            }
        }
        assertNotNull(stream);
        assertArrayEquals(original, parallel(stream, false, pool));
    }

    @Test
    void testSpuriousMagicRescan() throws IOException {
        final byte[] compressed = BZip2DifferentialTest.compressed("text-250k-3-blocks-at-1", 1);
        final byte[] expected = BZip2DifferentialTest.generatedInputs().get("text-250k-3-blocks-at-1");
        // inject spurious delimiter candidates inside blocks: the decoder must recover by rescanning
        try (BZip2CompressorInputStream in = new BZip2CompressorInputStream(new ByteArrayInputStream(compressed), false, pool, 16)) {
            final ParallelBZip2Decoder decoder = readParallelField(in);
            decoder.spuriousDelimitersForTesting.add(40 * 8 + 3L);
            decoder.spuriousDelimitersForTesting.add(5_000 * 8 + 6L);
            decoder.spuriousDelimitersForTesting.add((long) (compressed.length - 20) * 8);
            assertArrayEquals(expected, readAll(in));
        }
    }

    private static ParallelBZip2Decoder readParallelField(final BZip2CompressorInputStream in) {
        try {
            final java.lang.reflect.Field f = BZip2CompressorInputStream.class.getDeclaredField("parallel");
            f.setAccessible(true);
            return (ParallelBZip2Decoder) f.get(in);
        } catch (final ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
