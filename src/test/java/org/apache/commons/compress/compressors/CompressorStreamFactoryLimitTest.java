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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.junit.jupiter.api.Test;

/**
 * Decompression-bomb limits configured once on a {@link CompressorStreamFactory} must apply to every stream it creates, so a
 * caller is protected without wrapping each result by hand. Off by default, so existing factory behavior is unchanged.
 */
class CompressorStreamFactoryLimitTest {

    private static final int SIZE = 256 * 1024;

    private static byte[] gzippedZeros() throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GzipCompressorOutputStream out = new GzipCompressorOutputStream(bos)) {
            out.write(new byte[SIZE]);
        }
        return bos.toByteArray();
    }

    private static long drain(final CompressorInputStream in) throws IOException {
        final byte[] buf = new byte[8192];
        long total = 0;
        int n;
        while ((n = in.read(buf)) != -1) {
            total += n;
        }
        return total;
    }

    @Test
    void factoryMaxDecompressedSizeFires() throws Exception {
        final byte[] data = gzippedZeros();
        final CompressorStreamFactory factory = new CompressorStreamFactory().setMaxDecompressedSize(64 * 1024);
        final CompressorInputStream in = factory.createCompressorInputStream(CompressorStreamFactory.GZIP, new ByteArrayInputStream(data));
        assertThrows(DecompressionBombException.class, () -> drain(in));
    }

    @Test
    void factoryMaxDecompressedSizePasses() throws Exception {
        final byte[] data = gzippedZeros();
        final CompressorStreamFactory factory = new CompressorStreamFactory().setMaxDecompressedSize(1024L * 1024);
        final CompressorInputStream in = factory.createCompressorInputStream(CompressorStreamFactory.GZIP, new ByteArrayInputStream(data));
        assertDoesNotThrow(() -> drain(in));
    }

    @Test
    void factoryMaxCompressionRatioFires() throws Exception {
        final byte[] data = gzippedZeros();
        final CompressorStreamFactory factory = new CompressorStreamFactory().setMaxCompressionRatio(10).setCompressionRatioGraceBytes(4096);
        final CompressorInputStream in = factory.createCompressorInputStream(CompressorStreamFactory.GZIP, new ByteArrayInputStream(data));
        assertThrows(DecompressionBombException.class, () -> drain(in));
    }

    @Test
    void factoryUnconfiguredDoesNotFire() throws Exception {
        final byte[] data = gzippedZeros();
        final CompressorStreamFactory factory = new CompressorStreamFactory();
        final CompressorInputStream in = factory.createCompressorInputStream(CompressorStreamFactory.GZIP, new ByteArrayInputStream(data));
        assertDoesNotThrow(() -> drain(in));
    }

    @Test
    void setMaxDecompressedSizeReturnsFactory() {
        final CompressorStreamFactory factory = new CompressorStreamFactory();
        assertSame(factory, factory.setMaxDecompressedSize(1));
    }

    @Test
    void setMaxCompressionRatioReturnsFactory() {
        final CompressorStreamFactory factory = new CompressorStreamFactory();
        assertSame(factory, factory.setMaxCompressionRatio(1));
    }

    @Test
    void setCompressionRatioGraceBytesReturnsFactory() {
        final CompressorStreamFactory factory = new CompressorStreamFactory();
        assertSame(factory, factory.setCompressionRatioGraceBytes(1));
    }
}
