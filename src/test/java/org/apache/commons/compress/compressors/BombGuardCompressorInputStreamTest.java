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
package org.apache.commons.compress.compressors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.commons.compress.utils.InputStreamStatistics;
import org.junit.jupiter.api.Test;

/**
 * Deterministic unit tests for {@link BombGuardCompressorInputStream}. A controlled wrapped stream emits a fixed number of zero
 * bytes and reports a fixed compressed count, so the absolute cap, the ratio guard, the grace window and their boundaries are
 * exercised exactly. The guard counts the bytes it returns, so these hold regardless of the wrapped stream's own counters.
 */
class BombGuardCompressorInputStreamTest {

    /** A minimal decompressor: emits {@code total} zero bytes and reports a fixed {@code getCompressedCount()}. */
    private static final class FixedStream extends CompressorInputStream {

        private final long total;
        private long produced;
        private long compressed = -1;
        private boolean closed;

        FixedStream(final long total) {
            this.total = total;
        }

        FixedStream compressed(final long compressed) {
            this.compressed = compressed;
            return this;
        }

        @Override
        public void close() {
            closed = true;
        }

        @Override
        public long getCompressedCount() {
            return compressed;
        }

        boolean isClosed() {
            return closed;
        }

        @Override
        public int read() {
            if (produced >= total) {
                return -1;
            }
            produced++;
            return 0;
        }

        @Override
        public int read(final byte[] b, final int off, final int len) {
            if (produced >= total) {
                return -1;
            }
            final int n = (int) Math.min(len, total - produced);
            produced += n;
            Arrays.fill(b, off, off + n, (byte) 0);
            return n;
        }
    }

    private static BombGuardCompressorInputStream guard(final FixedStream in, final long maxSize, final double maxRatio, final long grace) {
        return new BombGuardCompressorInputStream(in, maxSize, maxRatio, grace);
    }

    private static void drainByteAtATime(final InputStream in) throws IOException {
        while (in.read() != -1) {
            // discard
        }
    }

    private static void drainChunked(final InputStream in) throws IOException {
        final byte[] buf = new byte[8192];
        while (in.read(buf) != -1) {
            // discard
        }
    }

    @Test
    void guardIsInputStreamStatistics() {
        assertInstanceOf(InputStreamStatistics.class, guard(new FixedStream(0), -1, -1, 0));
    }

    @Test
    void absoluteCapThrowsOnReachingLimit() throws Exception {
        final BombGuardCompressorInputStream g = guard(new FixedStream(1000), 100, -1, 0);
        assertThrows(DecompressionBombException.class, () -> drainByteAtATime(g));
        assertEquals(100, g.getBytesRead(), "delivers up to the cap, then throws (inclusive)");
    }

    @Test
    void absoluteCapAllowsOutputBelowLimit() throws Exception {
        final BombGuardCompressorInputStream g = guard(new FixedStream(99), 100, -1, 0);
        assertDoesNotThrow(() -> drainByteAtATime(g));
        assertEquals(99, g.getBytesRead());
    }

    @Test
    void absoluteCapIsInclusiveAtExactlyLimit() throws Exception {
        // The BoundedInputStream-backed cap is inclusive: a stream whose output reaches the cap throws.
        final BombGuardCompressorInputStream g = guard(new FixedStream(100), 100, -1, 0);
        assertThrows(DecompressionBombException.class, () -> drainByteAtATime(g));
    }

    @Test
    void absoluteCapZeroRejectsAllOutput() throws Exception {
        final BombGuardCompressorInputStream g = guard(new FixedStream(5), 0, -1, 0);
        assertThrows(DecompressionBombException.class, () -> drainByteAtATime(g));
        assertEquals(0, g.getBytesRead(), "a cap of 0 permits no output");
    }

    @Test
    void absoluteCapOffWhenNegative() throws Exception {
        final BombGuardCompressorInputStream g = guard(new FixedStream(1000), -1, -1, 0);
        assertDoesNotThrow(() -> drainByteAtATime(g));
        assertEquals(1000, g.getBytesRead());
    }

    @Test
    void absoluteCapEnforcedOnChunkedRead() throws Exception {
        final BombGuardCompressorInputStream g = guard(new FixedStream(1000), 100, -1, 0);
        assertThrows(DecompressionBombException.class, () -> drainChunked(g));
    }

    @Test
    void convenienceConstructorEnforcesAbsoluteCap() throws Exception {
        final BombGuardCompressorInputStream g = new BombGuardCompressorInputStream(new FixedStream(1000), 100);
        assertThrows(DecompressionBombException.class, () -> drainByteAtATime(g));
    }

    @Test
    void ratioGuardThrowsWhenRatioExceeded() throws Exception {
        final BombGuardCompressorInputStream g = guard(new FixedStream(10_000).compressed(10), -1, 100, 0);
        assertThrows(DecompressionBombException.class, () -> drainByteAtATime(g));
    }

    @Test
    void ratioGuardDoesNotThrowUnderRatio() throws Exception {
        final BombGuardCompressorInputStream g = guard(new FixedStream(500).compressed(10), -1, 100, 0); // ratio 50 < 100
        assertDoesNotThrow(() -> drainByteAtATime(g));
    }

    @Test
    void ratioGuardGraceToleratesEarlyHighRatio() throws Exception {
        final BombGuardCompressorInputStream g = guard(new FixedStream(10_000).compressed(10), -1, 100, 5000);
        assertThrows(DecompressionBombException.class, () -> drainByteAtATime(g));
        assertEquals(5001, g.getBytesRead(), "ratio guard does not fire until past the grace size");
    }

    @Test
    void ratioGuardSkippedWhenCompressedCountUnknown() throws Exception {
        final BombGuardCompressorInputStream g = guard(new FixedStream(10_000).compressed(-1), -1, 1, 0);
        assertDoesNotThrow(() -> drainByteAtATime(g));
    }

    @Test
    void ratioGuardSkippedWhenCompressedCountZero() throws Exception {
        final BombGuardCompressorInputStream g = guard(new FixedStream(10_000).compressed(0), -1, 1, 0);
        assertDoesNotThrow(() -> drainByteAtATime(g));
    }

    @Test
    void absoluteCapFiresBeforeRatioWhenSmaller() throws Exception {
        final BombGuardCompressorInputStream g = guard(new FixedStream(10_000).compressed(10), 500, 100, 0); // absolute at 500, ratio at 1001
        assertThrows(DecompressionBombException.class, () -> drainByteAtATime(g));
        assertEquals(500, g.getBytesRead());
    }

    @Test
    void ratioFiresBeforeAbsoluteWhenSmaller() throws Exception {
        final BombGuardCompressorInputStream g = guard(new FixedStream(10_000).compressed(10), 5000, 100, 0); // ratio 1001, absolute 5001
        assertThrows(DecompressionBombException.class, () -> drainByteAtATime(g));
        assertEquals(1001, g.getBytesRead());
    }

    @Test
    void getCompressedCountDelegatesToWrapped() throws Exception {
        final BombGuardCompressorInputStream g = guard(new FixedStream(10).compressed(7), -1, -1, 0);
        assertEquals(7, g.getCompressedCount());
    }

    @Test
    void getUncompressedCountIsTheBytesReturned() throws Exception {
        final BombGuardCompressorInputStream g = guard(new FixedStream(100), -1, -1, 0);
        drainByteAtATime(g);
        assertEquals(100, g.getUncompressedCount());
    }

    @Test
    void closePropagatesToWrapped() throws Exception {
        final FixedStream wrapped = new FixedStream(10);
        try (BombGuardCompressorInputStream g = guard(wrapped, -1, -1, 0)) {
            assertEquals(0, g.read());
        }
        assertTrue(wrapped.isClosed());
    }

    @Test
    void nullWrappedStreamRejected() {
        assertThrows(NullPointerException.class, () -> new BombGuardCompressorInputStream(null, 1));
    }

    @Test
    void thrownExceptionIsIOException() {
        final BombGuardCompressorInputStream g = guard(new FixedStream(10), 1, -1, 0);
        final IOException ex = assertThrows(IOException.class, () -> drainByteAtATime(g));
        assertInstanceOf(DecompressionBombException.class, ex);
    }

    @Test
    void absoluteCapMessageNamesTheLimit() {
        final BombGuardCompressorInputStream g = guard(new FixedStream(10), 4, -1, 0);
        final DecompressionBombException ex = assertThrows(DecompressionBombException.class, () -> drainByteAtATime(g));
        assertTrue(ex.getMessage().contains("4"), () -> "message should name the limit: " + ex.getMessage());
    }

    @Test
    void ratioMessageMentionsRatio() {
        final BombGuardCompressorInputStream g = guard(new FixedStream(10_000).compressed(10), -1, 100, 0);
        final DecompressionBombException ex = assertThrows(DecompressionBombException.class, () -> drainByteAtATime(g));
        assertTrue(ex.getMessage().toLowerCase().contains("ratio"), () -> "message should mention ratio: " + ex.getMessage());
    }
}
