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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.utils.InputStreamStatistics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Differential tests: {@link BZip2CompressorInputStream} must behave exactly like {@link LegacyBZip2Decoder} (the implementation before the performance
 * rewrite) on valid, concatenated, truncated and corrupted input, for every read pattern, including the bytes produced, the return values, the counters,
 * the exception thrown, and what happens on the next read after an exception.
 */
class BZip2DifferentialTest extends AbstractTest {

    /**
     * Records what a decoder did.
     */
    static final class Outcome {
        byte[] output;
        /** Exception class and message, or null. */
        String error;
        /** What one more {@code read()} after the exception did. */
        String errorAfterError;
        long uncompressedCount;
        long compressedCount;
        /** Bytes pulled from the underlying stream. */
        int consumedInput;
        /** Hash over (return value, compressed count) of every read/skip call. */
        long traceHash;
        int operations;
        final StringBuilder traceHead = new StringBuilder();

        void trace(final long value, final long compressed) {
            traceHash = traceHash * 1_000_003L + value;
            traceHash = traceHash * 1_000_003L + compressed;
            if (operations++ < 48) {
                traceHead.append(value).append('/').append(compressed).append(' ');
            }
        }
    }

    /**
     * Hands out bytes from an array and counts them; does not support mark/reset so decoders cannot peek.
     */
    static final class CountingInput extends InputStream {
        private final byte[] data;
        private int pos;

        CountingInput(final byte[] data) {
            this.data = data;
        }

        int position() {
            return pos;
        }

        @Override
        public int read() {
            return pos < data.length ? data[pos++] & 0xff : -1;
        }

        @Override
        public int read(final byte[] b, final int off, final int len) {
            if (len == 0) {
                return 0;
            }
            if (pos >= data.length) {
                return -1;
            }
            final int n = Math.min(len, data.length - pos);
            System.arraycopy(data, pos, b, off, n);
            pos += n;
            return n;
        }
    }

    @FunctionalInterface
    interface DecoderFactory {
        CompressorInputStream create(InputStream in, boolean decompressConcatenated) throws IOException;
    }

    enum ReadPattern {
        SINGLE_BYTE, BUF_1, BUF_7, BUF_64K, MIXED, SKIP
    }

    static final DecoderFactory LEGACY = LegacyBZip2Decoder::new;
    static final DecoderFactory CURRENT = BZip2CompressorInputStream::new;

    private static final ReadPattern[] MAIN_PATTERNS = { ReadPattern.BUF_64K, ReadPattern.SINGLE_BYTE, ReadPattern.MIXED };

    private static final String[] FIXTURES = { "bla.txt.bz2", "bla.tar.bz2", "bla.xml.bz2", "multiple.bz2", "COMPRESS-131.bz2", "lbzip2_32767.bz2",
            "lorem-ipsum.txt.bz2", "empty.txt.bz2", "org/apache/commons/compress/COMPRESS-651/my10m.tar.bz2",
            "org/apache/commons/compress/bzip2/hbCreateDecodeTables.bin" };

    private static final Map<String, byte[]> INPUTS = generatedInputs();
    private static final Map<String, byte[]> COMPRESSED = new ConcurrentHashMap<>();

    static byte[] bytesOf(final int size, final int value) {
        final byte[] b = new byte[size];
        Arrays.fill(b, (byte) value);
        return b;
    }

    static void compare(final Outcome legacy, final Outcome current, final String context) {
        assertEquals(legacy.error, current.error, context + ": exception");
        assertArrayEquals(legacy.output, current.output, context + ": output");
        assertEquals(legacy.operations, current.operations, context + ": number of operations");
        assertEquals(legacy.traceHead.toString(), current.traceHead.toString(), context + ": first operations (return value/compressed count)");
        assertEquals(legacy.traceHash, current.traceHash, context + ": trace of return values and compressed counts");
        assertEquals(legacy.errorAfterError, current.errorAfterError, context + ": read after exception");
        assertEquals(legacy.uncompressedCount, current.uncompressedCount, context + ": uncompressed count");
        assertEquals(legacy.compressedCount, current.compressedCount, context + ": compressed count");
        if (legacy.error == null) {
            assertEquals(legacy.consumedInput, current.consumedInput, context + ": bytes consumed from the underlying stream");
        } else {
            // After a data error inside a block the rewrite may have pulled up to 8 bytes of lookahead more than the previous decoder.
            assertTrue(current.consumedInput >= legacy.consumedInput && current.consumedInput <= legacy.consumedInput + 8,
                    context + ": bytes consumed from the underlying stream after an error: legacy=" + legacy.consumedInput + " current=" + current.consumedInput);
        }
    }

    /**
     * Comparison for corrupted input, where the rewrite is allowed to detect corruption earlier (stricter origPtr validation) than the legacy decoder.
     */
    static void compareRelaxed(final Outcome legacy, final Outcome current, final String context) {
        if (Objects.equals(legacy.error, current.error)) {
            compare(legacy, current, context);
            return;
        }
        assertNotNull(legacy.error, context + ": legacy did not fail but current did: " + current.error);
        assertNotNull(current.error, context + ": current did not fail but legacy did: " + legacy.error);
        assertTrue(current.error.endsWith(": Stream corrupted"), context + ": unexpected divergence: legacy=" + legacy.error + " current=" + current.error);
    }

    static byte[] compress(final byte[] input, final int blockSize) {
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (BZip2CompressorOutputStream out = new BZip2CompressorOutputStream(baos, blockSize)) {
                out.write(input);
            }
            return baos.toByteArray();
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static byte[] compressed(final String name, final int blockSize) {
        return COMPRESSED.computeIfAbsent(name + "@" + blockSize, k -> compress(INPUTS.get(name), blockSize));
    }

    static long compressedCount(final CompressorInputStream dec) {
        try {
            return ((InputStreamStatistics) dec).getCompressedCount();
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static byte[] concat(final byte[]... parts) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (final byte[] p : parts) {
            baos.write(p, 0, p.length);
        }
        return baos.toByteArray();
    }

    static String describe(final Throwable t) {
        return t.getClass().getName() + ": " + t.getMessage();
    }

    static void differential(final byte[] compressed, final boolean concat, final ReadPattern pattern, final String context) {
        compare(run(LEGACY, compressed, concat, pattern), run(CURRENT, compressed, concat, pattern), context + " pattern=" + pattern + " concat=" + concat);
    }

    static Stream<Arguments> fixtureCases() {
        final List<Arguments> list = new ArrayList<>();
        for (final String fixture : FIXTURES) {
            for (final ReadPattern pattern : MAIN_PATTERNS) {
                for (final boolean concat : new boolean[] { false, true }) {
                    list.add(Arguments.of(fixture, pattern, concat));
                }
            }
        }
        return list.stream();
    }

    static Stream<Arguments> generatedCases() {
        final List<Arguments> list = new ArrayList<>();
        for (final String name : INPUTS.keySet()) {
            for (final int blockSize : new int[] { 1, 9 }) {
                for (final ReadPattern pattern : ReadPattern.values()) {
                    list.add(Arguments.of(name, blockSize, pattern));
                }
            }
        }
        return list.stream();
    }

    static Map<String, byte[]> generatedInputs() {
        final Map<String, byte[]> m = new LinkedHashMap<>();
        m.put("empty", new byte[0]);
        m.put("one-byte", new byte[] { 'x' });
        m.put("tiny", "hello, bzip2".getBytes(StandardCharsets.US_ASCII));
        m.put("text-200k", text(new Random(1), 200_000));
        m.put("runs-150k", runs(new Random(2), 150_000));
        m.put("all-same-300k", bytesOf(300_000, 'a'));
        m.put("random-150k", randomBytes(new Random(3), 150_000));
        m.put("periodic-100k", periodic(100_000));
        m.put("sparse-runs-120k", sparseRuns(new Random(4), 120_000));
        m.put("high-bytes-100k", highBytes(new Random(7), 100_000));
        m.put("text-250k-3-blocks-at-1", text(new Random(5), 250_000));
        m.put("text-1100k-2-blocks-at-9", text(new Random(6), 1_100_000));
        // At block size 1 the RLE1 image is limited to 99,980 bytes; a run straddling that limit exercises block-boundary handling.
        m.put("run-at-block-boundary", concat(text(new Random(8), 99_900), bytesOf(300, 'q'), text(new Random(9), 500)));
        return m;
    }

    static byte[] highBytes(final Random rnd, final int size) {
        final byte[] b = new byte[size];
        for (int i = 0; i < size; i++) {
            b[i] = (byte) (rnd.nextInt(4) == 0 ? 0 : 0x80 + rnd.nextInt(128));
        }
        return b;
    }

    /**
     * Returns the next operation for a read pattern: -1 = {@code read()}, {@code <= -2} = {@code skip(-op - 2)}, otherwise the length for
     * {@code read(byte[], int, int)}.
     */
    static int nextOperation(final ReadPattern pattern, final Random rnd) {
        switch (pattern) {
        case SINGLE_BYTE:
            return -1;
        case BUF_1:
            return 1;
        case BUF_7:
            return 7;
        case BUF_64K:
            return 65536;
        case MIXED: {
            final int r = rnd.nextInt(100);
            if (r < 10) {
                return -1;
            }
            if (r < 15) {
                return 0;
            }
            if (r < 45) {
                return 1 + rnd.nextInt(16);
            }
            if (r < 85) {
                return 17 + rnd.nextInt(4080);
            }
            return 4097 + rnd.nextInt(65536 - 4096);
        }
        case SKIP: {
            final int r = rnd.nextInt(100);
            if (r < 30) {
                return -2 - rnd.nextInt(3000);
            }
            return 1 + rnd.nextInt(9000);
        }
        default:
            throw new IllegalArgumentException(pattern.toString());
        }
    }

    static byte[] periodic(final int size) {
        final byte[] b = new byte[size];
        for (int i = 0; i < size; i++) {
            b[i] = (byte) (i % 5000 < 2500 ? "abcabd".charAt(i % 6) : "xy".charAt(i % 2));
        }
        return b;
    }

    static byte[] randomBytes(final Random rnd, final int size) {
        final byte[] b = new byte[size];
        rnd.nextBytes(b);
        return b;
    }

    /**
     * Runs one decoder over {@code compressed} with the given read pattern and records everything observable.
     */
    static Outcome run(final DecoderFactory factory, final byte[] compressed, final boolean concat, final ReadPattern pattern) {
        final Outcome o = new Outcome();
        final CountingInput in = new CountingInput(compressed);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        CompressorInputStream dec = null;
        try {
            dec = factory.create(in, concat);
            final Random rnd = new Random(pattern.ordinal() * 7919L + compressed.length);
            final byte[] buf = new byte[65536 + 16];
            while (true) {
                final int op = nextOperation(pattern, rnd);
                if (op == -1) {
                    final int b = dec.read();
                    o.trace(b, compressedCount(dec));
                    if (b < 0) {
                        break;
                    }
                    out.write(b);
                } else if (op <= -2) {
                    final long skipped = dec.skip(-op - 2);
                    o.trace(skipped, compressedCount(dec));
                } else {
                    final int off = op == 7 ? 3 : 0;
                    final int n = dec.read(buf, off, op);
                    o.trace(n, compressedCount(dec));
                    if (n < 0) {
                        break;
                    }
                    out.write(buf, off, n);
                }
            }
        } catch (final IOException | RuntimeException e) {
            o.error = describe(e);
            if (dec != null) {
                try {
                    o.errorAfterError = "read=" + dec.read();
                } catch (final IOException | RuntimeException e2) {
                    o.errorAfterError = describe(e2);
                }
            }
        }
        if (dec != null) {
            o.uncompressedCount = dec.getUncompressedCount();
            o.compressedCount = compressedCount(dec);
            try {
                dec.close();
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        }
        o.output = out.toByteArray();
        o.consumedInput = in.position();
        return o;
    }

    static byte[] runs(final Random rnd, final int size) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(size + 700);
        while (baos.size() < size) {
            final int b = rnd.nextInt(6) == 0 ? rnd.nextInt(256) : 'a' + rnd.nextInt(6);
            final int r = rnd.nextInt(100);
            final int run;
            if (r < 40) {
                run = 1 + rnd.nextInt(3);
            } else if (r < 70) {
                run = 4 + rnd.nextInt(7);
            } else if (r < 90) {
                run = 11 + rnd.nextInt(250);
            } else {
                run = 261 + rnd.nextInt(340);
            }
            for (int i = 0; i < run; i++) {
                baos.write(b);
            }
        }
        return Arrays.copyOf(baos.toByteArray(), size);
    }

    /**
     * Mostly text with a run of 4..300 identical bytes every ~40 bytes; used for the randomised-block tests so that run-count bytes land on
     * randomisation flip positions.
     */
    static byte[] sparseRuns(final Random rnd, final int size) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(size + 400);
        while (baos.size() < size) {
            final byte[] t = text(rnd, 10 + rnd.nextInt(60));
            baos.write(t, 0, t.length);
            final int b = rnd.nextInt(3) == 0 ? rnd.nextInt(256) : ' ';
            final int run = rnd.nextInt(4) == 0 ? 4 + rnd.nextInt(297) : 4 + rnd.nextInt(6);
            for (int i = 0; i < run; i++) {
                baos.write(b);
            }
        }
        return Arrays.copyOf(baos.toByteArray(), size);
    }

    static byte[] text(final Random rnd, final int size) {
        final String[] words = { "the", "quick", "brown", "fox", "jumps", "over", "lazy", "dog", "bzip2", "Burrows", "Wheeler", "transform", "Huffman",
                "coding", "move", "to", "front", "run", "length", "encoding", "block", "sorting", "compression", "stream", "Apache", "Commons", "Compress",
                "java", "decoder", "random", "data", "text", "corpus", "benchmark", "table", "symbol", "entropy", "and", "of", "in", "a", "is", "it" };
        final StringBuilder sb = new StringBuilder(size + 32);
        while (sb.length() < size) {
            sb.append(words[rnd.nextInt(words.length)]);
            final int r = rnd.nextInt(20);
            if (r == 0) {
                sb.append(".\n");
            } else if (r == 1) {
                sb.append(", ");
            } else if (r == 2) {
                sb.append(rnd.nextInt(10000)).append(' ');
            } else {
                sb.append(' ');
            }
        }
        sb.setLength(size);
        return sb.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    @ParameterizedTest(name = "{0} pattern={1} concat={2}")
    @MethodSource("fixtureCases")
    void testFixtures(final String fixture, final ReadPattern pattern, final boolean concat) throws IOException {
        differential(readAllBytes(fixture), concat, pattern, fixture);
    }

    @ParameterizedTest(name = "{0} blockSize={1} pattern={2}")
    @MethodSource("generatedCases")
    void testGeneratedInputs(final String name, final int blockSize, final ReadPattern pattern) {
        final byte[] compressed = compressed(name, blockSize);
        final Outcome legacy = run(LEGACY, compressed, false, pattern);
        assertNull(legacy.error, name);
        if (pattern != ReadPattern.SKIP) {
            assertArrayEquals(INPUTS.get(name), legacy.output, name);
        }
        compare(legacy, run(CURRENT, compressed, false, pattern), name + " blockSize=" + blockSize + " pattern=" + pattern);
    }

    @Test
    void testConcatenatedStreams() {
        final byte[] a = compress(text(new Random(20), 30_000), 1);
        final byte[] b = compress(new byte[0], 9);
        final byte[] c = compress(runs(new Random(21), 20_000), 9);
        final byte[][] streams = { concat(a, b, c), concat(a, a), concat(b, a), concat(c, b) };
        for (int i = 0; i < streams.length; i++) {
            for (final ReadPattern pattern : MAIN_PATTERNS) {
                differential(streams[i], false, pattern, "concatenated#" + i);
                differential(streams[i], true, pattern, "concatenated#" + i);
            }
        }
    }

    @Test
    void testTrailingGarbage() {
        final byte[] base = compress("hello, world".getBytes(StandardCharsets.US_ASCII), 1);
        final byte[][] garbage = { { 'B' }, { 'B', 'Z' }, { 'B', 'Z', 'h' }, { 'B', 'Z', 'h', '9' }, { 'B', 'Z', 'h', '0' }, { 0 }, { 'B', 'Z', 'h', '1', 0x31, 0x41 },
                { 1, 2, 3, 4, 5, 6, 7, 8 } };
        for (final byte[] g : garbage) {
            final byte[] stream = concat(base, g);
            for (final ReadPattern pattern : MAIN_PATTERNS) {
                final Outcome legacy = run(LEGACY, stream, false, pattern);
                // The documented guarantee: with decompressConcatenated == false the underlying stream is left right after the bzip2 stream.
                assertNull(legacy.error);
                assertEquals(base.length, legacy.consumedInput, "legacy over-read with garbage " + Arrays.toString(g));
                compare(legacy, run(CURRENT, stream, false, pattern), "garbage=" + Arrays.toString(g) + " pattern=" + pattern);
                differential(stream, true, pattern, "garbage=" + Arrays.toString(g));
            }
        }
    }

    @Test
    void testTruncatedSmallStreamEveryOffset() {
        final byte[] full = compress(text(new Random(11), 400), 1);
        for (int cut = 0; cut < full.length; cut++) {
            final byte[] truncated = Arrays.copyOf(full, cut);
            for (final ReadPattern pattern : new ReadPattern[] { ReadPattern.BUF_64K, ReadPattern.SINGLE_BYTE }) {
                differential(truncated, false, pattern, "cut=" + cut + "/" + full.length);
                differential(truncated, true, pattern, "cut=" + cut + "/" + full.length);
            }
        }
    }

    @Test
    void testTruncatedMultiBlockStreamSampledOffsets() {
        final byte[] full = compressed("text-250k-3-blocks-at-1", 1);
        final List<Integer> cuts = new ArrayList<>();
        for (int cut = 0; cut < full.length; cut += Math.max(1, full.length / 40)) {
            cuts.add(cut);
        }
        for (int cut = Math.max(0, full.length - 24); cut < full.length; cut++) {
            cuts.add(cut);
        }
        for (final int cut : cuts) {
            final byte[] truncated = Arrays.copyOf(full, cut);
            differential(truncated, false, ReadPattern.BUF_64K, "cut=" + cut + "/" + full.length);
            differential(truncated, true, ReadPattern.MIXED, "cut=" + cut + "/" + full.length);
        }
    }

    @Test
    void testBitFlips() {
        final byte[][] bases = { compressed("text-250k-3-blocks-at-1", 1), compressed("runs-150k", 9), compressed("sparse-runs-120k", 1) };
        final Random rnd = new Random(99);
        for (int i = 0; i < 150; i++) {
            final byte[] base = bases[i % bases.length];
            final byte[] corrupted = base.clone();
            final int index = rnd.nextInt(base.length);
            corrupted[index] ^= (byte) (1 << rnd.nextInt(8));
            final String context = "flip#" + i + " byte " + index + " of " + base.length;
            compareRelaxed(run(LEGACY, corrupted, false, ReadPattern.BUF_64K), run(CURRENT, corrupted, false, ReadPattern.BUF_64K), context);
            compareRelaxed(run(LEGACY, corrupted, true, ReadPattern.MIXED), run(CURRENT, corrupted, true, ReadPattern.MIXED), context);
        }
    }

    @Test
    void testRandomisedBlocks() throws IOException {
        final Map<String, byte[]> inputs = new LinkedHashMap<>();
        inputs.put("no-flip-position", "short input, no randomisation flip reached".getBytes(StandardCharsets.US_ASCII));
        inputs.put("text-40k", text(new Random(30), 40_000));
        inputs.put("sparse-runs-60k", sparseRuns(new Random(31), 60_000));
        inputs.put("sparse-runs-600k", sparseRuns(new Random(32), 600_000));
        inputs.put("runs-80k", runs(new Random(33), 80_000));
        for (final Map.Entry<String, byte[]> e : inputs.entrySet()) {
            final byte[] original = e.getValue();
            final int blockSize = original.length > 90_000 ? 9 : 1;
            byte[] stream = null;
            for (int attempt = 0; attempt < 20 && stream == null; attempt++) {
                stream = RandomisedBZip2Streams.randomisedStream(original, blockSize);
                if (stream == null) {
                    // Masked RLE1 image was not canonical; perturb the input a little and retry.
                    original[attempt * 7919 % original.length] ^= 0x10;
                }
            }
            assertNotNull(stream, e.getKey() + ": could not build a randomised stream");
            final Outcome legacy = run(LEGACY, stream, false, ReadPattern.BUF_64K);
            assertNull(legacy.error, e.getKey());
            assertArrayEquals(original, legacy.output, e.getKey() + ": legacy decoder must decode the randomised stream to the original");
            for (final ReadPattern pattern : ReadPattern.values()) {
                differential(stream, false, pattern, "randomised " + e.getKey());
                differential(stream, true, pattern, "randomised " + e.getKey());
            }
            // Truncated inside the randomised block.
            differential(Arrays.copyOf(stream, stream.length - 5), false, ReadPattern.BUF_64K, "randomised truncated " + e.getKey());
            differential(Arrays.copyOf(stream, stream.length / 2), false, ReadPattern.SINGLE_BYTE, "randomised truncated " + e.getKey());
        }
    }

    @Test
    void testReadAfterClose() throws IOException {
        final byte[] stream = compress("closed".getBytes(StandardCharsets.US_ASCII), 1);
        final List<String> expected = readAfterClose(LEGACY, stream);
        assertEquals(expected, readAfterClose(CURRENT, stream));
    }

    private static List<String> readAfterClose(final DecoderFactory factory, final byte[] stream) throws IOException {
        final List<String> result = new ArrayList<>();
        final CompressorInputStream dec = factory.create(new CountingInput(stream), false);
        result.add("read=" + dec.read());
        dec.close();
        dec.close();
        final byte[] buf = new byte[4];
        try {
            result.add("read=" + dec.read());
        } catch (final IOException | RuntimeException e) {
            result.add(describe(e));
        }
        try {
            result.add("read=" + dec.read(buf, 0, 4));
        } catch (final IOException | RuntimeException e) {
            result.add(describe(e));
        }
        try {
            result.add("read=" + dec.read(buf, 0, 0));
        } catch (final IOException | RuntimeException e) {
            result.add(describe(e));
        }
        result.add("uncompressed=" + dec.getUncompressedCount());
        return result;
    }
}
