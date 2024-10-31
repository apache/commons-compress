/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.compress.compressors;

import static org.apache.commons.compress.AbstractTest.getFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.compress.MemoryLimitException;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.deflate.DeflateCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.pack200.Pack200CompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream;
import org.apache.commons.compress.utils.ByteUtils;
import org.apache.commons.io.input.BrokenInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@SuppressWarnings("deprecation") // deliberately tests setDecompressConcatenated
public final class DetectCompressorTest {

    static class TestData {

        final String fileName; // The multiple file name
        final char[] entryNames; // expected entries ...
        final CompressorStreamFactory factory; // ... when using this factory
        final boolean concat; // expected value for decompressConcatenated

        TestData(final String name, final char[] names, final CompressorStreamFactory factory, final boolean concat) {
            this.fileName = name;
            this.entryNames = names;
            this.factory = factory;
            this.concat = concat;
        }
    }

    private static final CompressorStreamFactory factoryTrue = new CompressorStreamFactory(true);
    private static final CompressorStreamFactory factoryFalse = new CompressorStreamFactory(false);

    // Must be static to allow use in the TestData entries
    private static final CompressorStreamFactory factorySetTrue;
    private static final CompressorStreamFactory factorySetFalse;

    static {
        factorySetTrue = new CompressorStreamFactory();
        factorySetTrue.setDecompressConcatenated(true);
        factorySetFalse = new CompressorStreamFactory();
        factorySetFalse.setDecompressConcatenated(false);
    }

    public static Stream<Arguments> getDetectLimitedByNameParams() {
        // @formatter:off
        return Stream.of(
            Arguments.of("bla.txt.bz2", CompressorStreamFactory.BZIP2),
            Arguments.of("bla.tgz", CompressorStreamFactory.GZIP),
            Arguments.of("bla.pack", CompressorStreamFactory.PACK200),
            Arguments.of("bla.tar.xz", CompressorStreamFactory.XZ),
            Arguments.of("bla.tar.deflatez", CompressorStreamFactory.DEFLATE),
            Arguments.of("bla.tar.lz4", CompressorStreamFactory.LZ4_FRAMED),
            Arguments.of("bla.tar.lzma", CompressorStreamFactory.LZMA),
            Arguments.of("bla.tar.sz", CompressorStreamFactory.SNAPPY_FRAMED),
            Arguments.of("bla.tar.Z", CompressorStreamFactory.Z),
            Arguments.of("bla.tar.zst", CompressorStreamFactory.ZSTANDARD)
        );
        // @formatter:on
    }

    final CompressorStreamFactory factory = new CompressorStreamFactory();

    private final TestData[] tests = {
        // @formatter:off
        new TestData("multiple.bz2", new char[] { 'a', 'b' }, factoryTrue, true),
        new TestData("multiple.bz2", new char[] { 'a', 'b' }, factorySetTrue, true),
        new TestData("multiple.bz2", new char[] { 'a' }, factoryFalse, false),
        new TestData("multiple.bz2", new char[] { 'a' }, factorySetFalse, false),
        new TestData("multiple.bz2", new char[] { 'a' }, factory, false),

        new TestData("multiple.gz", new char[] { 'a', 'b' }, factoryTrue, true),
        new TestData("multiple.gz", new char[] { 'a', 'b' }, factorySetTrue, true),
        new TestData("multiple.gz", new char[] { 'a' }, factoryFalse, false),
        new TestData("multiple.gz", new char[] { 'a' }, factorySetFalse, false),
        new TestData("multiple.gz", new char[] { 'a' }, factory, false),

        new TestData("multiple.xz", new char[] { 'a', 'b' }, factoryTrue, true),
        new TestData("multiple.xz", new char[] { 'a', 'b' }, factorySetTrue, true),
        new TestData("multiple.xz", new char[] { 'a' }, factoryFalse, false),
        new TestData("multiple.xz", new char[] { 'a' }, factorySetFalse, false),
        new TestData("multiple.xz", new char[] { 'a' }, factory, false),
        // @formatter:on
    };

    @SuppressWarnings("resource") // Caller closes.
    private CompressorInputStream createCompressorInputStream(final String resource) throws CompressorException, IOException {
        return factory.createCompressorInputStream(new BufferedInputStream(Files.newInputStream(getFile(resource).toPath())));
    }

    @SuppressWarnings("resource") // Caller closes.
    private CompressorInputStream createCompressorInputStream(final String resource, final CompressorStreamFactory factory)
            throws CompressorException, IOException {
        return factory.createCompressorInputStream(new BufferedInputStream(Files.newInputStream(getFile(resource).toPath())));
    }

    @SuppressWarnings("resource") // Caller closes.
    private CompressorInputStream createCompressorInputStream(final String resource, final Set<String> compressorNames)
            throws CompressorException, IOException {
        return factory.createCompressorInputStream(new BufferedInputStream(Files.newInputStream(getFile(resource).toPath())), compressorNames);
    }

    private InputStream createInputStream(final String fileName, final int memoryLimitInKb) throws Exception {
        final CompressorStreamFactory fac = new CompressorStreamFactory(true, memoryLimitInKb);
        final InputStream is = new BufferedInputStream(Files.newInputStream(getFile(fileName).toPath()));
        try {
            return fac.createCompressorInputStream(is);
        } catch (final CompressorException e) {
            if (e.getCause() != null && e.getCause() instanceof Exception) {
                // unwrap cause to reveal MemoryLimitException
                throw (Exception) e.getCause();
            }
            throw e;
        }

    }

    private String detect(final String testFileName) throws IOException, CompressorException {
        return detect(testFileName, null);
    }

    private String detect(final String testFileName, final Set<String> compressorNames) throws IOException, CompressorException {
        try (InputStream is = new BufferedInputStream(Files.newInputStream(getFile(testFileName).toPath()))) {
            return compressorNames != null ? CompressorStreamFactory.detect(is, compressorNames) : CompressorStreamFactory.detect(is);
        }
    }

    @Test
    public void testCreateLimitedByName() throws Exception {
        try (CompressorInputStream bzip2 = createCompressorInputStream("bla.txt.bz2", Collections.singleton(CompressorStreamFactory.BZIP2))) {
            assertNotNull(bzip2);
            assertInstanceOf(BZip2CompressorInputStream.class, bzip2);
        }

        try (CompressorInputStream gzip = createCompressorInputStream("bla.tgz", Collections.singleton(CompressorStreamFactory.GZIP))) {
            assertNotNull(gzip);
            assertInstanceOf(GzipCompressorInputStream.class, gzip);
        }

        try (CompressorInputStream pack200 = createCompressorInputStream("bla.pack", Collections.singleton(CompressorStreamFactory.PACK200))) {
            assertNotNull(pack200);
            assertInstanceOf(Pack200CompressorInputStream.class, pack200);
        }

        try (CompressorInputStream xz = createCompressorInputStream("bla.tar.xz", Collections.singleton(CompressorStreamFactory.XZ))) {
            assertNotNull(xz);
            assertInstanceOf(XZCompressorInputStream.class, xz);
        }

        try (CompressorInputStream zlib = createCompressorInputStream("bla.tar.deflatez", Collections.singleton(CompressorStreamFactory.DEFLATE))) {
            assertNotNull(zlib);
            assertInstanceOf(DeflateCompressorInputStream.class, zlib);
        }

        try (CompressorInputStream zstd = createCompressorInputStream("bla.tar.zst", Collections.singleton(CompressorStreamFactory.ZSTANDARD))) {
            assertNotNull(zstd);
            assertInstanceOf(ZstdCompressorInputStream.class, zstd);
        }
    }

    @Test
    public void testCreateLimitedByNameNotFound() throws Exception {
        assertThrows(CompressorException.class, () -> createCompressorInputStream("bla.txt.bz2", Collections.singleton(CompressorStreamFactory.BROTLI)));
        assertThrows(CompressorException.class, () -> createCompressorInputStream("bla.tgz", Collections.singleton(CompressorStreamFactory.Z)));
        assertThrows(CompressorException.class, () -> createCompressorInputStream("bla.pack", Collections.singleton(CompressorStreamFactory.SNAPPY_FRAMED)));
        assertThrows(CompressorException.class, () -> createCompressorInputStream("bla.tar.xz", Collections.singleton(CompressorStreamFactory.GZIP)));
        assertThrows(CompressorException.class, () -> createCompressorInputStream("bla.tar.deflatez", Collections.singleton(CompressorStreamFactory.PACK200)));
        assertThrows(CompressorException.class, () -> createCompressorInputStream("bla.tar.zst", Collections.singleton(CompressorStreamFactory.LZ4_FRAMED)));
    }

    @Test
    public void testCreateWithAutoDetection() throws Exception {
        try (CompressorInputStream bzip2 = createCompressorInputStream("bla.txt.bz2")) {
            assertNotNull(bzip2);
            assertInstanceOf(BZip2CompressorInputStream.class, bzip2);
        }

        try (CompressorInputStream gzip = createCompressorInputStream("bla.tgz")) {
            assertNotNull(gzip);
            assertInstanceOf(GzipCompressorInputStream.class, gzip);
        }

        try (CompressorInputStream pack200 = createCompressorInputStream("bla.pack")) {
            assertNotNull(pack200);
            assertInstanceOf(Pack200CompressorInputStream.class, pack200);
        }

        try (CompressorInputStream xz = createCompressorInputStream("bla.tar.xz")) {
            assertNotNull(xz);
            assertInstanceOf(XZCompressorInputStream.class, xz);
        }

        try (CompressorInputStream zlib = createCompressorInputStream("bla.tar.deflatez")) {
            assertNotNull(zlib);
            assertInstanceOf(DeflateCompressorInputStream.class, zlib);
        }

        try (CompressorInputStream zstd = createCompressorInputStream("bla.tar.zst")) {
            assertNotNull(zstd);
            assertInstanceOf(ZstdCompressorInputStream.class, zstd);
        }

        assertThrows(CompressorException.class, () -> factory.createCompressorInputStream(new ByteArrayInputStream(ByteUtils.EMPTY_BYTE_ARRAY)));
    }

    @Test
    public void testDetect() throws Exception {
        assertEquals(CompressorStreamFactory.BZIP2, detect("bla.txt.bz2"));
        assertEquals(CompressorStreamFactory.GZIP, detect("bla.tgz"));
        assertEquals(CompressorStreamFactory.PACK200, detect("bla.pack"));
        assertEquals(CompressorStreamFactory.XZ, detect("bla.tar.xz"));
        assertEquals(CompressorStreamFactory.DEFLATE, detect("bla.tar.deflatez"));
        assertEquals(CompressorStreamFactory.LZ4_FRAMED, detect("bla.tar.lz4"));
        assertEquals(CompressorStreamFactory.LZMA, detect("bla.tar.lzma"));
        assertEquals(CompressorStreamFactory.SNAPPY_FRAMED, detect("bla.tar.sz"));
        assertEquals(CompressorStreamFactory.Z, detect("bla.tar.Z"));
        assertEquals(CompressorStreamFactory.ZSTANDARD, detect("bla.tar.zst"));

        // make sure we don't oom on detect
        assertEquals(CompressorStreamFactory.Z, detect("COMPRESS-386"));
        assertEquals(CompressorStreamFactory.LZMA, detect("COMPRESS-382"));

        assertThrows(CompressorException.class,
                () -> CompressorStreamFactory.detect(new BufferedInputStream(new ByteArrayInputStream(ByteUtils.EMPTY_BYTE_ARRAY))));

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> CompressorStreamFactory.detect(null),
                "shouldn't be able to detect null stream");
        assertEquals("Stream must not be null.", e.getMessage());

        final CompressorException ce = assertThrows(CompressorException.class,
                () -> CompressorStreamFactory.detect(new BufferedInputStream(new BrokenInputStream())), "Expected IOException");
        assertEquals("IOException while reading signature.", ce.getMessage());
    }

    @ParameterizedTest
    @MethodSource("getDetectLimitedByNameParams")
    public void testDetectLimitedByName(final String filename, final String compressorName) throws Exception {
        assertEquals(compressorName, detect(filename, Collections.singleton(compressorName)));
    }

    @Test
    public void testDetectLimitedByNameNotFound() throws Exception {
        final Set<String> compressorNames = Collections.singleton(CompressorStreamFactory.DEFLATE);

        assertThrows(CompressorException.class, () -> detect("bla.txt.bz2", compressorNames));
        assertThrows(CompressorException.class, () -> detect("bla.tgz", compressorNames));
        assertThrows(CompressorException.class, () -> detect("bla.pack", compressorNames));
        assertThrows(CompressorException.class, () -> detect("bla.tar.xz", compressorNames));
        assertThrows(CompressorException.class, () -> detect("bla.tar.deflatez", Collections.singleton(CompressorStreamFactory.BZIP2)));
        assertThrows(CompressorException.class, () -> detect("bla.tar.lz4", compressorNames));
        assertThrows(CompressorException.class, () -> detect("bla.tar.lzma", compressorNames));
        assertThrows(CompressorException.class, () -> detect("bla.tar.sz", compressorNames));
        assertThrows(CompressorException.class, () -> detect("bla.tar.Z", compressorNames));
        assertThrows(CompressorException.class, () -> detect("bla.tar.zst", compressorNames));
    }

    @Test
    public void testDetectNullOrEmptyCompressorNames() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> CompressorStreamFactory.detect(createCompressorInputStream("bla.txt.bz2"), (Set<String>) null));
        assertThrows(IllegalArgumentException.class, () -> CompressorStreamFactory.detect(createCompressorInputStream("bla.tgz"), new HashSet<>()));
    }

    @Test
    public void testLZMAMemoryLimit() throws Exception {
        assertThrows(MemoryLimitException.class, () -> createInputStream("COMPRESS-382", 100));
    }

    @Test
    public void testMultiples() throws Exception {
        for (int i = 0; i < tests.length; i++) {
            final TestData test = tests[i];
            final CompressorStreamFactory fac = test.factory;
            assertNotNull(fac, "Test entry " + i);
            assertEquals(test.concat, fac.getDecompressConcatenated(), "Test entry " + i);
            try (CompressorInputStream in = createCompressorInputStream(test.fileName, fac)) {
                assertNotNull(in, "Test entry " + i);
                for (final char entry : test.entryNames) {
                    assertEquals(entry, in.read(), "Test entry" + i);
                }
                assertEquals(0, in.available());
                assertEquals(-1, in.read());
            }
        }
    }

    @Test
    public void testOverride() {
        final CompressorStreamFactory fac1 = new CompressorStreamFactory();
        assertFalse(fac1.getDecompressConcatenated());
        fac1.setDecompressConcatenated(true);
        assertTrue(fac1.getDecompressConcatenated());

        final CompressorStreamFactory fac2 = new CompressorStreamFactory(false);
        assertFalse(fac2.getDecompressConcatenated());
        assertThrows(IllegalStateException.class, () -> fac2.setDecompressConcatenated(true), "Expected IllegalStateException");

        final CompressorStreamFactory fac3 = new CompressorStreamFactory(true);
        assertTrue(fac3.getDecompressConcatenated());
        assertThrows(IllegalStateException.class, () -> fac3.setDecompressConcatenated(true), "Expected IllegalStateException");
    }

    @Test
    public void testXZMemoryLimitOnRead() throws Exception {
        // Even though the file is very small, the memory limit
        // has to be quite large (8296 KiB) because of the dictionary size

        // This is triggered on read(); not during initialization.
        // This test is here instead of the xz unit test to make sure
        // that the parameter is properly passed via the CompressorStreamFactory
        try (InputStream compressorIs = createInputStream("bla.tar.xz", 100)) {
            assertThrows(MemoryLimitException.class, () -> compressorIs.read());
        }
    }

    @Test
    public void testXZMemoryLimitOnSkip() throws Exception {
        try (InputStream compressorIs = createInputStream("bla.tar.xz", 100)) {
            assertThrows(MemoryLimitException.class, () -> compressorIs.skip(10));
        }
    }

    @Test
    public void testZMemoryLimit() throws Exception {
        assertThrows(MemoryLimitException.class, () -> createInputStream("COMPRESS-386", 100));
    }
}
