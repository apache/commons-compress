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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.stream.Stream;

import org.apache.commons.compress.MemoryLimitException;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.snappy.SnappyCompressorOutputStream;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests {@link CompressorStreamFactory}.
 */
class CompressorStreamFactoryTest {

    private static final int SIZE = 256 * 1024;

    private static byte[] compress(final String name, final byte[] data) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (OutputStream out = CompressorStreamFactory.SNAPPY_RAW.equals(name) ? new SnappyCompressorOutputStream(bos, data.length)
                : new CompressorStreamFactory().createCompressorOutputStream(name, bos)) {
            out.write(data);
        }
        return bos.toByteArray();
    }

    private static long drain(final InputStream in) throws IOException {
        final byte[] buffer = new byte[8192];
        long total = 0;
        int n;
        while ((n = in.read(buffer)) != -1) {
            total += n;
        }
        return total;
    }

    static Stream<Arguments> fixtures() {
        // @formatter:off
        return Stream.of(
                Arguments.of(CompressorStreamFactory.BROTLI, "/bla.tar.br"),
                Arguments.of(CompressorStreamFactory.PACK200, "/pack200/HelloWorld.pack"),
                Arguments.of(CompressorStreamFactory.Z, "/bla.tar.Z"));
        // @formatter:on
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

    private static CompressorInputStream open(final CompressorStreamFactory factory, final String name, final byte[] data) throws CompressorException {
        return factory.createCompressorInputStream(name, new ByteArrayInputStream(data));
    }

    private static byte[] resource(final String name) throws IOException {
        try (InputStream in = CompressorStreamFactoryTest.class.getResourceAsStream(name)) {
            return IOUtils.toByteArray(in);
        }
    }

    static Stream<String> writableFormats() {
        // @formatter:off
        return Stream.of(
                CompressorStreamFactory.BZIP2,
                CompressorStreamFactory.DEFLATE,
                CompressorStreamFactory.GZIP,
                CompressorStreamFactory.LZ4_BLOCK,
                CompressorStreamFactory.LZ4_FRAMED,
                CompressorStreamFactory.LZMA,
                CompressorStreamFactory.SNAPPY_FRAMED,
                CompressorStreamFactory.SNAPPY_RAW,
                CompressorStreamFactory.XZ,
                CompressorStreamFactory.ZSTANDARD);
        // @formatter:on
    }

    private static byte[] zeros() {
        return new byte[SIZE];
    }

    @SuppressWarnings("deprecation")
    @Test
    void testBuilderDecompressUntilEOF() throws Exception {
        final byte[] gz = gzip(new byte[1000], new byte[10]);
        assertNull(CompressorStreamFactory.builder().get().getDecompressUntilEOF());
        assertEquals(1000, drain(open(CompressorStreamFactory.builder().get(), CompressorStreamFactory.GZIP, gz)));
        assertEquals(1000, drain(open(CompressorStreamFactory.builder().setDecompressUntilEOF(false).get(), CompressorStreamFactory.GZIP, gz)));
        assertEquals(1010, drain(open(CompressorStreamFactory.builder().setDecompressUntilEOF(true).get(), CompressorStreamFactory.GZIP, gz)));
        final CompressorStreamFactory factory = CompressorStreamFactory.builder().setDecompressUntilEOF(true).get();
        assertEquals(Boolean.TRUE, factory.getDecompressUntilEOF());
        assertThrows(IllegalStateException.class, () -> factory.setDecompressConcatenated(false));
    }

    @Test
    void testBuilderDefaultsHaveNoLimit() throws Exception {
        try (CompressorInputStream in = open(CompressorStreamFactory.builder().get(), CompressorStreamFactory.GZIP, gzip(zeros()))) {
            assertInstanceOf(GzipCompressorInputStream.class, in);
            assertEquals(SIZE, drain(in));
        }
    }

    @Test
    void testBuilderMemoryLimit() throws Exception {
        final byte[] lzma = compress(CompressorStreamFactory.LZMA, zeros());
        final CompressorStreamFactory factory = CompressorStreamFactory.builder().setMemoryLimitKiB(1).get();
        final CompressorException e = assertThrows(CompressorException.class, () -> open(factory, CompressorStreamFactory.LZMA, lzma));
        assertInstanceOf(MemoryLimitException.class, e.getCause());
    }

    @Test
    void testBuilderMemoryLimitAndMaxDecompressedSize() throws Exception {
        final byte[] lzma = compress(CompressorStreamFactory.LZMA, zeros());
        final CompressorStreamFactory factory = CompressorStreamFactory.builder().setMemoryLimitKiB(64 * 1024).setMaxDecompressedSize(64 * 1024).get();
        try (CompressorInputStream in = open(factory, CompressorStreamFactory.LZMA, lzma)) {
            assertThrows(CompressorException.class, () -> drain(in));
            assertEquals(64 * 1024, in.getBytesRead());
        }
    }

    @Test
    void testBuilderMaxDecompressedSizeZeroAllowsEmptyOutputOnly() throws Exception {
        final CompressorStreamFactory factory = CompressorStreamFactory.builder().setMaxDecompressedSize(0).get();
        try (CompressorInputStream in = open(factory, CompressorStreamFactory.GZIP, gzip(new byte[0]))) {
            assertEquals(0, drain(in));
        }
        try (CompressorInputStream in = open(factory, CompressorStreamFactory.GZIP, gzip(new byte[1]))) {
            assertThrows(CompressorException.class, () -> drain(in));
        }
    }

    @Test
    void testBuilderNegativeMaxDecompressedSizeMeansNoLimit() throws Exception {
        final CompressorStreamFactory factory = CompressorStreamFactory.builder().setMaxDecompressedSize(-5).get();
        try (CompressorInputStream in = open(factory, CompressorStreamFactory.GZIP, gzip(zeros()))) {
            assertInstanceOf(GzipCompressorInputStream.class, in);
            assertEquals(SIZE, drain(in));
        }
    }

    @ParameterizedTest
    @MethodSource("fixtures")
    void testFixtureExactSizePasses(final String name, final String resource) throws Exception {
        final byte[] data = resource(resource);
        final long size;
        try (CompressorInputStream in = open(new CompressorStreamFactory(), name, data)) {
            size = drain(in);
        }
        try (CompressorInputStream in = open(CompressorStreamFactory.builder().setMaxDecompressedSize(size).get(), name, data)) {
            assertEquals(size, drain(in));
        }
    }

    @ParameterizedTest
    @MethodSource("fixtures")
    void testFixtureOverLimitThrows(final String name, final String resource) throws Exception {
        final byte[] data = resource(resource);
        final long size;
        try (CompressorInputStream in = open(new CompressorStreamFactory(), name, data)) {
            size = drain(in);
        }
        try (CompressorInputStream in = open(CompressorStreamFactory.builder().setMaxDecompressedSize(size - 1).get(), name, data)) {
            assertThrows(CompressorException.class, () -> drain(in));
            assertEquals(size - 1, in.getBytesRead());
        }
    }

    @Test
    void testMaxDecompressedSizeAppliesToAutoDetectedStream() throws Exception {
        final CompressorStreamFactory factory = CompressorStreamFactory.builder().setMaxDecompressedSize(64 * 1024).get();
        try (CompressorInputStream in = factory.createCompressorInputStream(new BufferedInputStream(new ByteArrayInputStream(gzip(zeros()))))) {
            assertThrows(CompressorException.class, () -> drain(in));
        }
    }

    @Test
    void testMaxDecompressedSizeAppliesToAutoDetectedStreamWithNames() throws Exception {
        final CompressorStreamFactory factory = CompressorStreamFactory.builder().setMaxDecompressedSize(64 * 1024).get();
        try (CompressorInputStream in = factory.createCompressorInputStream(new BufferedInputStream(new ByteArrayInputStream(gzip(zeros()))),
                Collections.singleton(CompressorStreamFactory.GZIP))) {
            assertThrows(CompressorException.class, () -> drain(in));
        }
    }

    @Test
    void testMaxDecompressedSizeAppliesToNamedStream() throws Exception {
        final CompressorStreamFactory factory = CompressorStreamFactory.builder().setMaxDecompressedSize(64 * 1024).get();
        try (CompressorInputStream in = open(factory, CompressorStreamFactory.GZIP, gzip(zeros()))) {
            assertInstanceOf(BoundedCompressorInputStream.class, in);
            assertThrows(CompressorException.class, () -> drain(in));
            assertEquals(64 * 1024, in.getBytesRead());
        }
    }

    @Test
    void testMaxDecompressedSizeExactPayloadPasses() throws Exception {
        final byte[] payload = "The quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.UTF_8);
        final CompressorStreamFactory factory = CompressorStreamFactory.builder().setMaxDecompressedSize(payload.length).get();
        try (CompressorInputStream in = open(factory, CompressorStreamFactory.GZIP, gzip(payload))) {
            assertArrayEquals(payload, IOUtils.toByteArray(in));
        }
        final CompressorStreamFactory smaller = CompressorStreamFactory.builder().setMaxDecompressedSize(payload.length - 1).get();
        try (CompressorInputStream in = open(smaller, CompressorStreamFactory.GZIP, gzip(payload))) {
            assertThrows(CompressorException.class, () -> IOUtils.toByteArray(in));
        }
    }

    @Test
    void testMaxDecompressedSizePreservesContent() throws Exception {
        final byte[] payload = "The quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.UTF_8);
        final CompressorStreamFactory factory = CompressorStreamFactory.builder().setMaxDecompressedSize(1024).get();
        try (CompressorInputStream in = open(factory, CompressorStreamFactory.GZIP, gzip(payload))) {
            assertArrayEquals(payload, IOUtils.toByteArray(in));
        }
    }

    @Test
    void testUnconfiguredReturnsConcreteStream() throws Exception {
        try (CompressorInputStream in = open(new CompressorStreamFactory(), CompressorStreamFactory.GZIP, gzip(zeros()))) {
            assertInstanceOf(GzipCompressorInputStream.class, in);
            assertFalse(in instanceof BoundedCompressorInputStream);
            assertEquals(SIZE, drain(in));
        }
    }

    @ParameterizedTest
    @MethodSource("writableFormats")
    void testWritableFormatExactSizePasses(final String name) throws Exception {
        final byte[] data = compress(name, zeros());
        try (CompressorInputStream in = open(CompressorStreamFactory.builder().setMaxDecompressedSize(SIZE).get(), name, data)) {
            assertEquals(SIZE, drain(in));
        }
    }

    @ParameterizedTest
    @MethodSource("writableFormats")
    void testWritableFormatOverLimitThrows(final String name) throws Exception {
        final byte[] data = compress(name, zeros());
        try (CompressorInputStream in = open(CompressorStreamFactory.builder().setMaxDecompressedSize(64 * 1024).get(), name, data)) {
            assertThrows(CompressorException.class, () -> drain(in));
            assertEquals(64 * 1024, in.getBytesRead());
        }
    }

    @ParameterizedTest
    @MethodSource("writableFormats")
    void testWritableFormatUnconfigured(final String name) throws Exception {
        final byte[] data = compress(name, zeros());
        try (CompressorInputStream in = open(new CompressorStreamFactory(), name, data)) {
            assertEquals(SIZE, drain(in));
        }
    }

    @ParameterizedTest
    @MethodSource("writableFormats")
    void testWritableFormatUnderLimitPasses(final String name) throws Exception {
        final byte[] data = compress(name, zeros());
        try (CompressorInputStream in = open(CompressorStreamFactory.builder().setMaxDecompressedSize(1024L * 1024).get(), name, data)) {
            assertEquals(SIZE, drain(in));
        }
    }
}
