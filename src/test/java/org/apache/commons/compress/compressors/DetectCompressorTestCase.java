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

import static org.apache.commons.compress.AbstractTestCase.getFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.apache.commons.compress.MemoryLimitException;
import org.apache.commons.compress.MockEvilInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.deflate.DeflateCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.pack200.Pack200CompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream;
import org.apache.commons.compress.utils.ByteUtils;
import org.junit.jupiter.api.Test;

@SuppressWarnings("deprecation") // deliberately tests setDecompressConcatenated
public final class DetectCompressorTestCase {

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

    final CompressorStreamFactory factory = new CompressorStreamFactory();

    private final TestData[] tests = {
        new TestData("multiple.bz2", new char[]{'a','b'}, factoryTrue, true),
        new TestData("multiple.bz2", new char[]{'a','b'}, factorySetTrue, true),
        new TestData("multiple.bz2", new char[]{'a'}, factoryFalse, false),
        new TestData("multiple.bz2", new char[]{'a'}, factorySetFalse, false),
        new TestData("multiple.bz2", new char[]{'a'}, factory, false),

        new TestData("multiple.gz", new char[]{'a','b'}, factoryTrue, true),
        new TestData("multiple.gz", new char[]{'a','b'}, factorySetTrue, true),
        new TestData("multiple.gz", new char[]{'a'}, factoryFalse, false),
        new TestData("multiple.gz", new char[]{'a'}, factorySetFalse, false),
        new TestData("multiple.gz", new char[]{'a'}, factory, false),

        new TestData("multiple.xz", new char[]{'a','b'}, factoryTrue, true),
        new TestData("multiple.xz", new char[]{'a','b'}, factorySetTrue, true),
        new TestData("multiple.xz", new char[]{'a'}, factoryFalse, false),
        new TestData("multiple.xz", new char[]{'a'}, factorySetFalse, false),
        new TestData("multiple.xz", new char[]{'a'}, factory, false),
    };

    private String detect(final String testFileName) throws IOException, CompressorException {
        String name = null;
        try (InputStream is = new BufferedInputStream(
                Files.newInputStream(getFile(testFileName).toPath()))) {
            name = CompressorStreamFactory.detect(is);
        }
        return name;
    }

    private CompressorInputStream getStreamFor(final String resource)
            throws CompressorException, IOException {
        return factory.createCompressorInputStream(
                   new BufferedInputStream(Files.newInputStream(
                       getFile(resource).toPath())));
    }

    private CompressorInputStream getStreamFor(final String resource, final CompressorStreamFactory factory)
            throws CompressorException, IOException {
        return factory.createCompressorInputStream(
                   new BufferedInputStream(Files.newInputStream(
                       getFile(resource).toPath())));
    }

    private InputStream getStreamFor(final String fileName, final int memoryLimitInKb) throws Exception {
        final CompressorStreamFactory fac = new CompressorStreamFactory(true,
                memoryLimitInKb);
        final InputStream is = new BufferedInputStream(
                Files.newInputStream(getFile(fileName).toPath()));
        try {
            return fac.createCompressorInputStream(is);
        } catch (final CompressorException e) {
            if (e.getCause() != null && e.getCause() instanceof Exception) {
                //unwrap cause to reveal MemoryLimitException
                throw (Exception)e.getCause();
            }
            throw e;
        }

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

        //make sure we don't oom on detect
        assertEquals(CompressorStreamFactory.Z, detect("COMPRESS-386"));
        assertEquals(CompressorStreamFactory.LZMA, detect("COMPRESS-382"));

        assertThrows(CompressorException.class, () -> CompressorStreamFactory.detect(new BufferedInputStream(new ByteArrayInputStream(ByteUtils.EMPTY_BYTE_ARRAY))));

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> CompressorStreamFactory.detect(null),
                "shouldn't be able to detect null stream");
        assertEquals("Stream must not be null.", e.getMessage());

        final CompressorException ce = assertThrows(CompressorException.class, () -> CompressorStreamFactory.detect(new BufferedInputStream(new MockEvilInputStream())),
                "Expected IOException");
        assertEquals("IOException while reading signature.", ce.getMessage());
    }

    @Test
    public void testDetection() throws Exception {
        final CompressorInputStream bzip2 = getStreamFor("bla.txt.bz2");
        assertNotNull(bzip2);
        assertTrue(bzip2 instanceof BZip2CompressorInputStream);

        final CompressorInputStream gzip = getStreamFor("bla.tgz");
        assertNotNull(gzip);
        assertTrue(gzip instanceof GzipCompressorInputStream);

        final CompressorInputStream pack200 = getStreamFor("bla.pack");
        assertNotNull(pack200);
        assertTrue(pack200 instanceof Pack200CompressorInputStream);

        final CompressorInputStream xz = getStreamFor("bla.tar.xz");
        assertNotNull(xz);
        assertTrue(xz instanceof XZCompressorInputStream);

        final CompressorInputStream zlib = getStreamFor("bla.tar.deflatez");
        assertNotNull(zlib);
        assertTrue(zlib instanceof DeflateCompressorInputStream);

        final CompressorInputStream zstd = getStreamFor("bla.tar.zst");
        assertNotNull(zstd);
        assertTrue(zstd instanceof ZstdCompressorInputStream);

        assertThrows(CompressorException.class, () -> factory.createCompressorInputStream(new ByteArrayInputStream(ByteUtils.EMPTY_BYTE_ARRAY)));
    }

    @Test
    public void testLZMAMemoryLimit() throws Exception {
        assertThrows(MemoryLimitException.class, () -> getStreamFor("COMPRESS-382", 100));
    }

    @Test
    public void testMultiples() throws Exception {
        for(int i=0; i <tests.length; i++) {
            final TestData test = tests[i];
            final CompressorStreamFactory fac = test.factory;
            assertNotNull(fac, "Test entry " + i);
            assertEquals(test.concat, fac.getDecompressConcatenated(), "Test entry " + i);
            final CompressorInputStream in = getStreamFor(test.fileName, fac);
            assertNotNull(in, "Test entry " + i);
            for (final char entry : test.entryNames) {
                assertEquals(entry, in.read(), "Test entry" + i);
            }
            assertEquals(0, in.available());
            assertEquals(-1, in.read());
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
        assertThrows(IllegalStateException.class, () -> fac2.setDecompressConcatenated(true),
                "Expected IllegalStateException");

        final CompressorStreamFactory fac3 = new CompressorStreamFactory(true);
        assertTrue(fac3.getDecompressConcatenated());
        assertThrows(IllegalStateException.class, () -> fac3.setDecompressConcatenated(true),
                "Expected IllegalStateException");
    }

    @Test
    public void testXZMemoryLimitOnRead() throws Exception {
        //Even though the file is very small, the memory limit
        //has to be quite large (8296 KiB) because of the dictionary size

        //This is triggered on read(); not during initialization.
        //This test is here instead of the xz unit test to make sure
        //that the parameter is properly passed via the CompressorStreamFactory
        try (InputStream compressorIs = getStreamFor("bla.tar.xz", 100)) {
            assertThrows(MemoryLimitException.class, () -> compressorIs.read());
        }
    }

    @Test
    public void testXZMemoryLimitOnSkip() throws Exception {
        try (InputStream compressorIs = getStreamFor("bla.tar.xz", 100)) {
            assertThrows(MemoryLimitException.class, () -> compressorIs.skip(10));
        }
    }

    @Test
    public void testZMemoryLimit() throws Exception {
        assertThrows(MemoryLimitException.class, () -> getStreamFor("COMPRESS-386", 100));
    }
}
