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

package org.apache.commons.compress.compressors.brotli;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

public class BrotliCompressorInputStreamTest extends AbstractTest {

    @Test
    public void testAvailableShouldReturnZero() throws IOException {
        try (InputStream is = newInputStream("brotli.testdata.compressed");
                BrotliCompressorInputStream in = new BrotliCompressorInputStream(is)) {
            assertEquals(0, in.available());
        }
    }

    /**
     * Tests bridge.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Test
    public void testBrotliDecode() throws IOException {
        try (InputStream inputStream = newInputStream("brotli.testdata.compressed");
                BrotliCompressorInputStream brotliInputStream = new BrotliCompressorInputStream(inputStream)) {
            final byte[] expected = readAllBytes("brotli.testdata.uncompressed");
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            IOUtils.copy(brotliInputStream, bos);
            assertArrayEquals(expected, bos.toByteArray());
        }
    }

    @Test
    public void testBrotliUnarchive() throws Exception {
        final File output = newTempFile("bla.tar");
        try (InputStream is = newInputStream("bla.tar.br")) {
            try (CompressorInputStream in = new CompressorStreamFactory().createCompressorInputStream("br", is)) {
                Files.copy(in, output.toPath());
            }
        }
    }

    @Test
    public void testCachingIsEnabledByDefaultAndBrotliIsPresent() {
        assertEquals(BrotliUtils.CachedAvailability.CACHED_AVAILABLE, BrotliUtils.getCachedBrotliAvailability());
        assertTrue(BrotliUtils.isBrotliCompressionAvailable());
    }

    @Test
    public void testCanTurnOffCaching() {
        try {
            BrotliUtils.setCacheBrotliAvailablity(false);
            assertEquals(BrotliUtils.CachedAvailability.DONT_CACHE, BrotliUtils.getCachedBrotliAvailability());
            assertTrue(BrotliUtils.isBrotliCompressionAvailable());
        } finally {
            BrotliUtils.setCacheBrotliAvailablity(true);
        }
    }

    @Test
    public void testMultiByteReadConsistentlyReturnsMinusOneAtEof() throws IOException {
        final byte[] buf = new byte[2];
        try (InputStream is = newInputStream("brotli.testdata.compressed");
                BrotliCompressorInputStream in = new BrotliCompressorInputStream(is)) {
            IOUtils.toByteArray(in);
            assertEquals(-1, in.read(buf));
            assertEquals(-1, in.read(buf));
        }
    }

    @Test
    public void testShouldBeAbleToSkipAByte() throws IOException {
        try (InputStream is = newInputStream("brotli.testdata.compressed");
                BrotliCompressorInputStream in = new BrotliCompressorInputStream(is)) {
            assertEquals(1, in.skip(1));
        }
    }

    @Test
    public void testSingleByteReadConsistentlyReturnsMinusOneAtEof() throws IOException {
        try (InputStream is = newInputStream("brotli.testdata.compressed");
                BrotliCompressorInputStream in = new BrotliCompressorInputStream(is)) {
            IOUtils.toByteArray(in);
            assertEquals(-1, in.read());
            assertEquals(-1, in.read());
        }
    }

    @Test
    public void testSingleByteReadWorksAsExpected() throws IOException {
        try (InputStream is = newInputStream("brotli.testdata.compressed");
                BrotliCompressorInputStream in = new BrotliCompressorInputStream(is)) {
            // starts with file name "XXX"
            assertEquals('X', in.read());
        }
    }

    @Test
    public void testTurningOnCachingReEvaluatesAvailability() {
        try {
            BrotliUtils.setCacheBrotliAvailablity(false);
            assertEquals(BrotliUtils.CachedAvailability.DONT_CACHE, BrotliUtils.getCachedBrotliAvailability());
            BrotliUtils.setCacheBrotliAvailablity(true);
            assertEquals(BrotliUtils.CachedAvailability.CACHED_AVAILABLE, BrotliUtils.getCachedBrotliAvailability());
        } finally {
            BrotliUtils.setCacheBrotliAvailablity(true);
        }
    }

}
