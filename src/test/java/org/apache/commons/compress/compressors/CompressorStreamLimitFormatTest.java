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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;
import java.util.stream.Stream;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.deflate.DeflateCompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorOutputStream;
import org.apache.commons.compress.compressors.snappy.FramedSnappyCompressorOutputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * End-to-end decompression-bomb protection across real codecs. For each format a highly compressible payload (zeros) and an
 * incompressible payload (random) are round-tripped, proving the absolute cap and the ratio guard fire on a real bomb, the cap
 * passes legitimate data, and that nothing fires by default.
 */
class CompressorStreamLimitFormatTest {

    private static final int SIZE = 256 * 1024;

    static Stream<String> formats() {
        return Stream.of(CompressorStreamFactory.GZIP, CompressorStreamFactory.BZIP2, CompressorStreamFactory.XZ,
                CompressorStreamFactory.DEFLATE, CompressorStreamFactory.LZ4_FRAMED, CompressorStreamFactory.SNAPPY_FRAMED);
    }

    private static byte[] compress(final String format, final byte[] data) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (OutputStream out = newOutputStream(format, bos)) {
            out.write(data);
        }
        return bos.toByteArray();
    }

    private static OutputStream newOutputStream(final String format, final OutputStream out) throws IOException {
        switch (format) {
        case CompressorStreamFactory.GZIP:
            return new GzipCompressorOutputStream(out);
        case CompressorStreamFactory.BZIP2:
            return new BZip2CompressorOutputStream(out);
        case CompressorStreamFactory.XZ:
            return new XZCompressorOutputStream(out);
        case CompressorStreamFactory.DEFLATE:
            return new DeflateCompressorOutputStream(out);
        case CompressorStreamFactory.LZ4_FRAMED:
            return new FramedLZ4CompressorOutputStream(out);
        case CompressorStreamFactory.SNAPPY_FRAMED:
            return new FramedSnappyCompressorOutputStream(out);
        default:
            throw new IllegalArgumentException(format);
        }
    }

    private static byte[] zeros() {
        return new byte[SIZE];
    }

    private static byte[] random() {
        final byte[] data = new byte[SIZE];
        new Random(42).nextBytes(data);
        return data;
    }

    private static long decompress(final String format, final byte[] data, final long maxSize, final double maxRatio, final long grace)
            throws IOException {
        final CompressorInputStream raw = new CompressorStreamFactory().createCompressorInputStream(format, new ByteArrayInputStream(data));
        final CompressorInputStream in = new BombGuardCompressorInputStream(raw, maxSize, maxRatio, grace);
        final byte[] buf = new byte[8192];
        long total = 0;
        int n;
        while ((n = in.read(buf)) != -1) {
            total += n;
        }
        return total;
    }

    @ParameterizedTest
    @MethodSource("formats")
    void absoluteCapFiresOnBomb(final String format) throws Exception {
        final byte[] data = compress(format, zeros());
        assertThrows(DecompressionBombException.class, () -> decompress(format, data, 64 * 1024, -1, 0));
    }

    @ParameterizedTest
    @MethodSource("formats")
    void absoluteCapPassesLegitimateData(final String format) throws Exception {
        final byte[] data = compress(format, zeros());
        assertEquals(SIZE, decompress(format, data, 1024L * 1024, -1, 0));
    }

    @ParameterizedTest
    @MethodSource("formats")
    void decompressesFullyWhenUnconfigured(final String format) throws Exception {
        final byte[] data = compress(format, zeros());
        assertEquals(SIZE, decompress(format, data, -1, -1, 0));
    }

    @ParameterizedTest
    @MethodSource("formats")
    void ratioGuardFiresOnBomb(final String format) throws Exception {
        final byte[] data = compress(format, zeros());
        assertThrows(DecompressionBombException.class, () -> decompress(format, data, -1, 10, 4096));
    }

    @ParameterizedTest
    @MethodSource("formats")
    void ratioGuardToleratesIncompressibleData(final String format) throws Exception {
        final byte[] data = compress(format, random());
        assertEquals(SIZE, decompress(format, data, -1, 10, 4096));
    }
}
