/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.compress.compressors.brotli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.Assert;
import org.junit.Test;

public class BrotliCompressorInputStreamTest extends AbstractTestCase {

    /**
     * Test bridge works fine
     * @throws {@link IOException}
     */
    @Test
    public void testBrotliDecode() throws IOException {
        final File input = getFile("brotli.testdata.compressed");
        final File expected = getFile("brotli.testdata.uncompressed");
        try (InputStream inputStream = new FileInputStream(input);
                InputStream expectedStream = new FileInputStream(expected);
                BrotliCompressorInputStream brotliInputStream = new BrotliCompressorInputStream(inputStream)) {
            final byte[] b = new byte[20];
            IOUtils.readFully(expectedStream, b);
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int readByte = -1;
            while((readByte = brotliInputStream.read()) != -1) {
                bos.write(readByte);
            }
            Assert.assertArrayEquals(b, bos.toByteArray());
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


    @Test
    public void availableShouldReturnZero() throws IOException {
        final File input = getFile("brotli.testdata.compressed");
        try (InputStream is = new FileInputStream(input)) {
            final BrotliCompressorInputStream in =
                    new BrotliCompressorInputStream(is);
            Assert.assertTrue(in.available() == 0);
            in.close();
        }
    }

    @Test
    public void shouldBeAbleToSkipAByte() throws IOException {
        final File input = getFile("brotli.testdata.compressed");
        try (InputStream is = new FileInputStream(input)) {
            final BrotliCompressorInputStream in =
                    new BrotliCompressorInputStream(is);
            Assert.assertEquals(1, in.skip(1));
            in.close();
        }
    }

    @Test
    public void singleByteReadWorksAsExpected() throws IOException {
        final File input = getFile("brotli.testdata.compressed");
        try (InputStream is = new FileInputStream(input)) {
            final BrotliCompressorInputStream in =
                    new BrotliCompressorInputStream(is);
            //  starts with filename "XXX"
            Assert.assertEquals('X', in.read());
            in.close();
        }
    }

    @Test
    public void singleByteReadConsistentlyReturnsMinusOneAtEof() throws IOException {
        final File input = getFile("brotli.testdata.compressed");
        try (InputStream is = new FileInputStream(input)) {
            final BrotliCompressorInputStream in =
                    new BrotliCompressorInputStream(is);
            IOUtils.toByteArray(in);
            Assert.assertEquals(-1, in.read());
            Assert.assertEquals(-1, in.read());
            in.close();
        }
    }

    @Test
    public void multiByteReadConsistentlyReturnsMinusOneAtEof() throws IOException {
        final File input = getFile("brotli.testdata.compressed");
        byte[] buf = new byte[2];
        try (InputStream is = new FileInputStream(input)) {
            final BrotliCompressorInputStream in =
                    new BrotliCompressorInputStream(is);
            IOUtils.toByteArray(in);
            Assert.assertEquals(-1, in.read(buf));
            Assert.assertEquals(-1, in.read(buf));
            in.close();
        }
    }

    @Test
    public void testBrotliUnarchive() throws Exception {
        final File input = getFile("bla.tar.br");
        final File output = new File(dir, "bla.tar");
        try (InputStream is = new FileInputStream(input)) {
            final CompressorInputStream in = new CompressorStreamFactory()
                    .createCompressorInputStream("br", is);
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(output);
                IOUtils.copy(in, out);
            } finally {
                if (out != null) {
                    out.close();
                }
                in.close();
            }
        }
    }

}
