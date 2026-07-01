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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Random;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.junit.jupiter.api.Test;

/**
 * Resilience cases for {@link BombGuardCompressorInputStream}: the {@code read}/{@code skip}/{@code available}/{@code mark}
 * surface, nesting, integer-boundary limits, grace edge values, concatenated streams, factory type preservation, and large
 * legitimate payloads. These prove the guard is exact and fail-closed without over-blocking or leaking the wrapped stream.
 */
class BombGuardResilienceTest {

    /** A minimal decompressor that emits {@code total} zero bytes, reports a fixed compressed count, and counts closes. */
    private static final class Src extends CompressorInputStream {

        private final long total;
        private long produced;
        private long compressed = -1;
        private int closes;

        Src(final long total) {
            this.total = total;
        }

        @Override
        public int available() {
            return (int) Math.min(Integer.MAX_VALUE, total - produced);
        }

        @Override
        public void close() {
            closes++;
        }

        int closes() {
            return closes;
        }

        Src compressed(final long compressed) {
            this.compressed = compressed;
            return this;
        }

        @Override
        public long getCompressedCount() {
            return compressed;
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

    private static byte[] gzip(final byte[] data) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GzipCompressorOutputStream out = new GzipCompressorOutputStream(bos)) {
            out.write(data);
        }
        return bos.toByteArray();
    }

    private static long drain(final InputStream in) throws IOException {
        final byte[] buf = new byte[4096];
        long total = 0;
        int n;
        while ((n = in.read(buf)) != -1) {
            total += n;
        }
        return total;
    }

    @Test
    void readByteArraySingleArgIsEnforced() throws Exception {
        final BombGuardCompressorInputStream g = new BombGuardCompressorInputStream(new Src(1000), 100);
        assertThrows(DecompressionBombException.class, () -> {
            final byte[] b = new byte[16];
            while (g.read(b) != -1) {
                // discard
            }
        });
    }

    @Test
    void readZeroLengthReturnsZeroWithoutThrow() throws Exception {
        final BombGuardCompressorInputStream g = new BombGuardCompressorInputStream(new Src(10), 10);
        assertEquals(0, g.read(new byte[0]));
    }

    @Test
    void availableDelegatesToWrapped() throws Exception {
        final BombGuardCompressorInputStream g = new BombGuardCompressorInputStream(new Src(50), -1);
        assertEquals(50, g.available());
    }

    @Test
    void skipIsCountedAndEnforced() throws Exception {
        final BombGuardCompressorInputStream g = new BombGuardCompressorInputStream(new Src(1000), 100);
        assertThrows(DecompressionBombException.class, () -> g.skip(1000));
    }

    @Test
    void markIsNotSupported() {
        assertFalse(new BombGuardCompressorInputStream(new Src(10), -1).markSupported());
    }

    @Test
    void doubleWrappedEnforcesOuterCap() throws Exception {
        final BombGuardCompressorInputStream inner = new BombGuardCompressorInputStream(new Src(1000), -1);
        final BombGuardCompressorInputStream outer = new BombGuardCompressorInputStream(inner, 100);
        assertThrows(DecompressionBombException.class, () -> drain(outer));
    }

    @Test
    void maxLongCapNeverFiresAndDoesNotOverflow() throws Exception {
        final BombGuardCompressorInputStream g = new BombGuardCompressorInputStream(new Src(10_000), Long.MAX_VALUE);
        assertDoesNotThrow(() -> drain(g));
    }

    @Test
    void fractionalRatioFires() throws Exception {
        final BombGuardCompressorInputStream g = new BombGuardCompressorInputStream(new Src(1000).compressed(100), -1, 1.5, 0);
        assertThrows(DecompressionBombException.class, () -> {
            while (g.read() != -1) {
                // discard
            }
        });
        assertEquals(151, g.getBytesRead());
    }

    @Test
    void negativeGraceEnforcesRatioImmediately() throws Exception {
        final BombGuardCompressorInputStream g = new BombGuardCompressorInputStream(new Src(1000).compressed(1), -1, 10, -1);
        assertThrows(DecompressionBombException.class, () -> {
            while (g.read() != -1) {
                // discard
            }
        });
        assertEquals(11, g.getBytesRead());
    }

    @Test
    void emptyWrappedStreamDoesNotThrow() throws Exception {
        final BombGuardCompressorInputStream g = new BombGuardCompressorInputStream(new Src(0), 10);
        assertDoesNotThrow(() -> drain(g));
    }

    @Test
    void capBoundaryOnSmallChunks() throws Exception {
        final BombGuardCompressorInputStream g = new BombGuardCompressorInputStream(new Src(1000), 512);
        assertThrows(DecompressionBombException.class, () -> {
            final byte[] b = new byte[100];
            while (g.read(b) != -1) {
                // discard
            }
        });
        assertEquals(512, g.getBytesRead(), "delivers up to the cap, then throws");
    }

    @Test
    void ratioMessageContainsBothCounts() {
        final BombGuardCompressorInputStream g = new BombGuardCompressorInputStream(new Src(10_000).compressed(10), -1, 100, 0);
        final DecompressionBombException ex = assertThrows(DecompressionBombException.class, () -> {
            while (g.read() != -1) {
                // discard
            }
        });
        assertTrue(ex.getMessage().contains("10"), () -> ex.getMessage());
    }

    @Test
    void closePropagatesToWrapped() throws Exception {
        final Src wrapped = new Src(10);
        new BombGuardCompressorInputStream(wrapped, -1).close();
        assertEquals(1, wrapped.closes(), "closing the guard closes the wrapped decompressor");
    }

    @Test
    void getCompressedCountStableAfterEof() throws Exception {
        final BombGuardCompressorInputStream g = new BombGuardCompressorInputStream(new Src(10).compressed(7), -1);
        drain(g);
        assertEquals(7, g.getCompressedCount());
    }

    @Test
    void capAppliesAcrossConcatenatedGzipMembers() throws Exception {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GzipCompressorOutputStream g1 = new GzipCompressorOutputStream(bos)) {
            g1.write(new byte[100 * 1024]);
        }
        try (GzipCompressorOutputStream g2 = new GzipCompressorOutputStream(bos)) {
            g2.write(new byte[100 * 1024]);
        }
        final GzipCompressorInputStream in = new GzipCompressorInputStream(new ByteArrayInputStream(bos.toByteArray()), true);
        final BombGuardCompressorInputStream g = new BombGuardCompressorInputStream(in, 120 * 1024);
        assertThrows(DecompressionBombException.class, () -> drain(g));
    }

    @Test
    void factoryAutoDetectAppliesLimit() throws Exception {
        final byte[] bomb = gzip(new byte[256 * 1024]);
        final CompressorStreamFactory factory = new CompressorStreamFactory().setMaxDecompressedSize(64 * 1024);
        final CompressorInputStream in = factory.createCompressorInputStream(new BufferedInputStream(new ByteArrayInputStream(bomb)));
        assertThrows(DecompressionBombException.class, () -> drain(in));
    }

    @Test
    void factoryUnconfiguredReturnsConcreteType() throws Exception {
        final byte[] data = gzip(new byte[1024]);
        final CompressorInputStream in = new CompressorStreamFactory().createCompressorInputStream(CompressorStreamFactory.GZIP,
                new ByteArrayInputStream(data));
        assertInstanceOf(GzipCompressorInputStream.class, in);
        assertFalse(in instanceof BombGuardCompressorInputStream);
    }

    @Test
    void factoryConfiguredReturnsGuard() throws Exception {
        final byte[] data = gzip(new byte[1024]);
        final CompressorInputStream in = new CompressorStreamFactory().setMaxDecompressedSize(1).createCompressorInputStream(
                CompressorStreamFactory.GZIP, new ByteArrayInputStream(data));
        assertInstanceOf(BombGuardCompressorInputStream.class, in);
    }

    @Test
    void factoryMemoryLimitAndBombLimitBothApply() throws Exception {
        final byte[] bomb = gzip(new byte[256 * 1024]);
        final CompressorStreamFactory factory = new CompressorStreamFactory(false, 1024).setMaxDecompressedSize(64 * 1024);
        final CompressorInputStream in = factory.createCompressorInputStream(CompressorStreamFactory.GZIP, new ByteArrayInputStream(bomb));
        assertThrows(DecompressionBombException.class, () -> drain(in));
    }

    @Test
    void largeLegitimateDataPassesWithExactCount() throws Exception {
        final byte[] data = new byte[1024 * 1024];
        new Random(7).nextBytes(data);
        final BombGuardCompressorInputStream g = new BombGuardCompressorInputStream(
                new GzipCompressorInputStream(new ByteArrayInputStream(gzip(data))), 2L * 1024 * 1024);
        assertEquals(data.length, drain(g));
        assertEquals(data.length, g.getUncompressedCount());
    }
}
