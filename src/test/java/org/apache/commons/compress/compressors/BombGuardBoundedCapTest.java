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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.junit.jupiter.api.Test;

/**
 * The absolute decompressed-size cap is enforced by composing Apache Commons IO {@code BoundedInputStream} over the
 * decompressor (per the maintainer's "compose with what we have" review note). These exercise the resulting inclusive boundary,
 * close propagation, the cap on real codecs, and the Apache Tika-equivalent ratio-plus-grace configuration end to end.
 */
class BombGuardBoundedCapTest {

    /** A minimal decompressor emitting {@code total} zero bytes; tracks closes; reports a fixed compressed count. */
    private static final class Zeros extends CompressorInputStream {

        private final long total;
        private long produced;
        private long compressed = -1;
        private int closes;

        Zeros(final long total) {
            this.total = total;
        }

        Zeros compressed(final long compressed) {
            this.compressed = compressed;
            return this;
        }

        @Override
        public void close() {
            closes++;
        }

        int closes() {
            return closes;
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

    private static GzipCompressorInputStream gunzip(final byte[] gz) throws IOException {
        return new GzipCompressorInputStream(new ByteArrayInputStream(gz));
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
    void capDeliversUpToLimitThenThrows() throws Exception {
        final BombGuardCompressorInputStream g = new BombGuardCompressorInputStream(new Zeros(1000), 100);
        assertThrows(DecompressionBombException.class, () -> drain(g));
        assertEquals(100, g.getBytesRead());
    }

    @Test
    void capIsInclusiveAtExactSize() throws Exception {
        final BombGuardCompressorInputStream g = new BombGuardCompressorInputStream(new Zeros(100), 100);
        assertThrows(DecompressionBombException.class, () -> drain(g));
    }

    @Test
    void belowCapDeliversEverything() throws Exception {
        final BombGuardCompressorInputStream g = new BombGuardCompressorInputStream(new Zeros(50), 100);
        assertEquals(50, drain(g));
        assertEquals(50, g.getUncompressedCount());
    }

    @Test
    void unboundedCapCountsButNeverThrows() throws Exception {
        final BombGuardCompressorInputStream g = new BombGuardCompressorInputStream(new Zeros(5000), -1);
        assertEquals(5000, drain(g));
        assertEquals(5000, g.getUncompressedCount());
    }

    @Test
    void closePropagatesThroughBoundedInputStream() throws Exception {
        final Zeros source = new Zeros(10);
        new BombGuardCompressorInputStream(source, 100).close();
        assertEquals(1, source.closes(), "closing the guard closes the decompressor through BoundedInputStream");
    }

    @Test
    void capFiresBeforeRatioWhenSmaller() throws Exception {
        final BombGuardCompressorInputStream g = new BombGuardCompressorInputStream(new Zeros(10_000).compressed(10), 500, 100, 0);
        assertThrows(DecompressionBombException.class, () -> drain(g));
        assertEquals(500, g.getBytesRead());
    }

    @Test
    void availableWithCapIsNonNegative() throws Exception {
        final BombGuardCompressorInputStream g = new BombGuardCompressorInputStream(new Zeros(50), 100);
        assertTrue(g.available() >= 0);
    }

    @Test
    void realGzipCapFires() throws Exception {
        final byte[] bomb = gzip(new byte[256 * 1024]);
        final BombGuardCompressorInputStream g = new BombGuardCompressorInputStream(gunzip(bomb), 64 * 1024);
        assertThrows(DecompressionBombException.class, () -> drain(g));
    }

    @Test
    void realGzipCapInclusiveAtExactDecompressedSize() throws Exception {
        final byte[] payload = "hello".getBytes("UTF-8");
        final byte[] gz = gzip(payload);
        final BombGuardCompressorInputStream atLimit = new BombGuardCompressorInputStream(gunzip(gz), payload.length);
        assertThrows(DecompressionBombException.class, () -> drain(atLimit));
        final BombGuardCompressorInputStream aboveLimit = new BombGuardCompressorInputStream(gunzip(gz), payload.length + 1);
        assertEquals(payload.length, drain(aboveLimit));
    }

    @Test
    void realGzipBelowCapPasses() throws Exception {
        final byte[] gz = gzip(new byte[1000]);
        final BombGuardCompressorInputStream g = new BombGuardCompressorInputStream(gunzip(gz), 4096);
        assertEquals(1000, drain(g));
    }

    @Test
    void tikaEquivalentRatioAndGraceFiresOnBomb() throws Exception {
        // Apache Tika SecureContentHandler defaults: ratio 100, output threshold (grace) 1,000,000.
        final byte[] bomb = gzip(new byte[2 * 1024 * 1024]);
        final BombGuardCompressorInputStream g = new BombGuardCompressorInputStream(gunzip(bomb), -1, 100, 1_000_000);
        assertThrows(DecompressionBombException.class, () -> drain(g));
    }

    @Test
    void tikaEquivalentRatioAndGracePassesLegitimateData() throws Exception {
        final byte[] data = new byte[2 * 1024 * 1024];
        new Random(11).nextBytes(data);
        final BombGuardCompressorInputStream g = new BombGuardCompressorInputStream(gunzip(gzip(data)), -1, 100, 1_000_000);
        assertEquals(data.length, drain(g));
    }
}
