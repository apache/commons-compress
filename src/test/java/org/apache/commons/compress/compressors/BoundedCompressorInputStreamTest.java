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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link BoundedCompressorInputStream}.
 */
class BoundedCompressorInputStreamTest {

    /**
     * Produces a fixed number of bytes followed by a sticky end of stream.
     */
    private static final class Source extends CompressorInputStream {

        private final long size;
        private long produced;
        private long compressedCount = -1;
        private int closeCount;
        private boolean markSupported;

        Source(final long size) {
            this.size = size;
        }

        @Override
        public int available() {
            return (int) Math.min(size - produced, Integer.MAX_VALUE);
        }

        @Override
        public void close() {
            closeCount++;
        }

        @Override
        public long getCompressedCount() {
            return compressedCount;
        }

        @Override
        public boolean markSupported() {
            return markSupported;
        }

        @Override
        public int read() {
            if (produced >= size) {
                return -1;
            }
            return (int) (produced++ % 251);
        }

        @Override
        public int read(final byte[] b, final int off, final int len) {
            if (len == 0) {
                return 0;
            }
            if (produced >= size) {
                return -1;
            }
            final int n = (int) Math.min(len, size - produced);
            for (int i = 0; i < n; i++) {
                b[off + i] = (byte) (produced++ % 251);
            }
            return n;
        }
    }

    private static final int BUFFER_SIZE = 8192;

    private static byte[] bytes(final int size) {
        final byte[] data = new byte[size];
        for (int i = 0; i < size; i++) {
            data[i] = (byte) (i % 251);
        }
        return data;
    }

    private static long drain(final InputStream in, final int bufferSize) throws IOException {
        final byte[] buffer = new byte[bufferSize];
        long total = 0;
        int n;
        while ((n = in.read(buffer)) != -1) {
            total += n;
        }
        return total;
    }

    private static long drainSingleBytes(final InputStream in) throws IOException {
        long total = 0;
        while (in.read() != -1) {
            total++;
        }
        return total;
    }

    private static byte[] gzip(final byte[]... members) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        for (final byte[] member : members) {
            try (GzipCompressorOutputStream out = new GzipCompressorOutputStream(bos)) {
                out.write(member);
            }
        }
        return bos.toByteArray();
    }

    private static GzipCompressorInputStream gunzip(final byte[] gz, final boolean decompressConcatenated) throws IOException {
        return GzipCompressorInputStream.builder().setInputStream(new ByteArrayInputStream(gz)).setDecompressConcatenated(decompressConcatenated).get();
    }

    @Test
    void testAvailableStaysWithinRemaining() throws Exception {
        try (BoundedCompressorInputStream in = new BoundedCompressorInputStream(new Source(50), 10)) {
            assertEquals(10, in.available());
            assertEquals(4, in.read(new byte[4]));
            assertEquals(6, in.available());
            assertEquals(6, in.read(new byte[6]));
            assertEquals(0, in.available());
        }
        try (BoundedCompressorInputStream in = new BoundedCompressorInputStream(new Source(5), 10)) {
            assertEquals(5, in.available());
        }
    }

    @Test
    void testBelowLimitDeliversEverything() throws Exception {
        try (BoundedCompressorInputStream in = new BoundedCompressorInputStream(new Source(50), 100)) {
            assertEquals(50, drain(in, BUFFER_SIZE));
            assertEquals(50, in.getBytesRead());
            assertEquals(50, in.getUncompressedCount());
        }
    }

    @Test
    void testCloseClosesSourceOnce() throws Exception {
        final Source source = new Source(10);
        new BoundedCompressorInputStream(source, 100).close();
        assertEquals(1, source.closeCount);
    }

    @Test
    void testCompressedCountIsForwarded() throws Exception {
        try (BoundedCompressorInputStream in = new BoundedCompressorInputStream(new Source(10), 100)) {
            assertEquals(-1, in.getCompressedCount());
        }
        final Source source = new Source(10);
        source.compressedCount = 7;
        try (BoundedCompressorInputStream in = new BoundedCompressorInputStream(source, 100)) {
            assertEquals(7, in.getCompressedCount());
        }
    }

    @Test
    void testConcatenatedMembersCountAgainstLimit() throws Exception {
        final byte[] first = bytes(1000);
        final byte[] gz = gzip(first, bytes(10));
        // Only the first member is decompressed, so its exact size passes.
        try (BoundedCompressorInputStream in = new BoundedCompressorInputStream(gunzip(gz, false), first.length)) {
            assertArrayEquals(first, IOUtils.toByteArray(in));
        }
        // With concatenated members the output continues past the limit.
        try (BoundedCompressorInputStream in = new BoundedCompressorInputStream(gunzip(gz, true), first.length)) {
            assertThrows(CompressorException.class, () -> drain(in, BUFFER_SIZE));
            assertEquals(first.length, in.getBytesRead());
        }
    }

    @Test
    void testCorruptTrailerAtExactSizeSurfaces() throws Exception {
        final byte[] data = bytes(1000);
        final byte[] gz = gzip(data);
        // Damage the CRC32 in the member trailer; the limit equals the payload, so the trailer is only checked by the boundary read.
        gz[gz.length - 8] ^= 0x55;
        try (BoundedCompressorInputStream in = new BoundedCompressorInputStream(gunzip(gz, false), data.length)) {
            final CompressorException e = assertThrows(CompressorException.class, () -> drain(in, BUFFER_SIZE));
            assertTrue(e.getMessage().contains("corrupt"), e.getMessage());
        }
    }

    @Test
    void testExactSizePasses() throws Exception {
        try (BoundedCompressorInputStream in = new BoundedCompressorInputStream(new Source(100), 100)) {
            assertEquals(100, drain(in, BUFFER_SIZE));
            assertEquals(-1, in.read());
            assertEquals(-1, in.read(new byte[8]));
            assertEquals(100, in.getBytesRead());
        }
    }

    @Test
    void testExactSizePassesWithSingleByteReads() throws Exception {
        try (BoundedCompressorInputStream in = new BoundedCompressorInputStream(new Source(100), 100)) {
            assertEquals(100, drainSingleBytes(in));
            assertEquals(-1, in.read());
        }
    }

    @Test
    void testLimitZeroAllowsEmptyOutputOnly() throws Exception {
        try (BoundedCompressorInputStream in = new BoundedCompressorInputStream(new Source(0), 0)) {
            assertEquals(0, drain(in, BUFFER_SIZE));
        }
        try (BoundedCompressorInputStream in = new BoundedCompressorInputStream(new Source(1), 0)) {
            assertThrows(CompressorException.class, () -> drain(in, BUFFER_SIZE));
            assertEquals(0, in.getBytesRead());
        }
    }

    @Test
    void testLongMaxValueLimit() throws Exception {
        try (BoundedCompressorInputStream in = new BoundedCompressorInputStream(new Source(1000), Long.MAX_VALUE)) {
            assertEquals(1000, drain(in, BUFFER_SIZE));
        }
    }

    @Test
    void testMarkAndResetNotSupported() throws Exception {
        final Source source = new Source(100);
        source.markSupported = true;
        try (BoundedCompressorInputStream in = new BoundedCompressorInputStream(source, 100)) {
            assertFalse(in.markSupported());
            in.mark(10);
            in.read();
            assertThrows(IOException.class, in::reset);
        }
    }

    @Test
    void testNegativeLimitRejected() {
        assertThrows(IllegalArgumentException.class, () -> new BoundedCompressorInputStream(new Source(10), -1));
    }

    @Test
    void testOneByteOverLimitThrows() throws Exception {
        try (BoundedCompressorInputStream in = new BoundedCompressorInputStream(new Source(101), 100)) {
            assertThrows(CompressorException.class, () -> drain(in, BUFFER_SIZE));
            assertEquals(100, in.getBytesRead());
            assertEquals(100, in.getUncompressedCount());
        }
    }

    @Test
    void testOneByteOverLimitThrowsWithSingleByteReads() throws Exception {
        try (BoundedCompressorInputStream in = new BoundedCompressorInputStream(new Source(101), 100)) {
            assertThrows(CompressorException.class, () -> drainSingleBytes(in));
            assertEquals(100, in.getBytesRead());
        }
    }

    @Test
    void testOverLimitDeliversExactlyTheLimit() throws Exception {
        try (BoundedCompressorInputStream in = new BoundedCompressorInputStream(new Source(1_000_000), 100_000)) {
            final ByteArrayOutputStream delivered = new ByteArrayOutputStream();
            assertThrows(CompressorException.class, () -> IOUtils.copy(in, delivered));
            assertArrayEquals(bytes(100_000), delivered.toByteArray());
        }
    }

    @Test
    void testRandomizedPayloadAndLimit() throws Exception {
        final Random random = new Random(20260920L);
        for (int i = 0; i < 500; i++) {
            final int size = random.nextInt(65_536);
            final int limit = random.nextBoolean() ? size : random.nextInt(65_536);
            final int bufferSize = 1 + random.nextInt(BUFFER_SIZE);
            try (BoundedCompressorInputStream in = new BoundedCompressorInputStream(new Source(size), limit)) {
                if (size > limit) {
                    assertThrows(CompressorException.class, () -> drain(in, bufferSize), () -> "size " + size + " limit " + limit);
                } else {
                    assertEquals(size, drain(in, bufferSize), () -> "size " + size + " limit " + limit);
                }
                assertEquals(Math.min(size, limit), in.getBytesRead(), () -> "size " + size + " limit " + limit);
            }
        }
    }

    @Test
    void testReadAfterLimitKeepsThrowing() throws Exception {
        try (BoundedCompressorInputStream in = new BoundedCompressorInputStream(new Source(101), 100)) {
            assertThrows(CompressorException.class, () -> drain(in, BUFFER_SIZE));
            assertThrows(CompressorException.class, in::read);
            assertThrows(CompressorException.class, () -> in.read(new byte[8]));
            assertThrows(CompressorException.class, () -> in.skip(1));
            assertEquals(100, in.getBytesRead());
        }
    }

    @Test
    void testRealGzipExactSizePasses() throws Exception {
        final byte[] data = bytes(100_000);
        try (BoundedCompressorInputStream in = new BoundedCompressorInputStream(gunzip(gzip(data), false), data.length)) {
            assertArrayEquals(data, IOUtils.toByteArray(in));
        }
    }

    @Test
    void testRealGzipOverLimitThrows() throws Exception {
        final byte[] gz = gzip(new byte[256 * 1024]);
        try (BoundedCompressorInputStream in = new BoundedCompressorInputStream(gunzip(gz, false), 64 * 1024)) {
            assertThrows(CompressorException.class, () -> drain(in, BUFFER_SIZE));
            assertEquals(64 * 1024, in.getBytesRead());
        }
    }

    @Test
    void testSkipCountsAgainstLimit() throws Exception {
        try (BoundedCompressorInputStream in = new BoundedCompressorInputStream(new Source(1000), 100)) {
            assertThrows(CompressorException.class, () -> in.skip(1000));
        }
        try (BoundedCompressorInputStream in = new BoundedCompressorInputStream(new Source(100), 100)) {
            assertEquals(100, in.skip(100));
            assertEquals(-1, in.read());
        }
        try (BoundedCompressorInputStream in = new BoundedCompressorInputStream(new Source(101), 100)) {
            assertEquals(100, in.skip(100));
            assertThrows(CompressorException.class, in::read);
        }
    }

    @Test
    void testThrowsCompressorException() throws Exception {
        try (BoundedCompressorInputStream in = new BoundedCompressorInputStream(new Source(1000), 100)) {
            final IOException e = assertThrows(IOException.class, () -> drain(in, BUFFER_SIZE));
            assertInstanceOf(CompressorException.class, e);
            assertTrue(e.getMessage().contains("maximum"), e.getMessage());
        }
    }

    @Test
    void testZeroLengthReadAtLimitReturnsZero() throws Exception {
        try (BoundedCompressorInputStream in = new BoundedCompressorInputStream(new Source(101), 100)) {
            assertEquals(100, in.read(new byte[100], 0, 100));
            assertEquals(0, in.read(new byte[8], 0, 0));
            assertThrows(CompressorException.class, in::read);
        }
    }
}
