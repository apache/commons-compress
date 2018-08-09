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

package org.apache.commons.compress.compressors.zstandard;

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
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.Assert;
import org.junit.Test;

public class ZstdCompressorInputStreamTest extends AbstractTestCase {

    /**
     * Test bridge works fine.
     * 
     * @throws IOException 
     */
    @Test
    public void testZstdDecode() throws IOException {
        final File input = getFile("zstandard.testdata.zst");
        final File expected = getFile("zstandard.testdata");
        try (InputStream inputStream = new FileInputStream(input);
            InputStream expectedStream = new FileInputStream(expected);
            ZstdCompressorInputStream zstdInputStream = new ZstdCompressorInputStream(inputStream)) {
            final byte[] b = new byte[97];
            IOUtils.readFully(expectedStream, b);
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int readByte = -1;
            while((readByte = zstdInputStream.read()) != -1) {
                bos.write(readByte);
            }
            Assert.assertArrayEquals(b, bos.toByteArray());
        }
    }

    @Test
    public void testCachingIsEnabledByDefaultAndZstdUtilsPresent() {
        assertEquals(ZstdUtils.CachedAvailability.CACHED_AVAILABLE, ZstdUtils.getCachedZstdAvailability());
        assertTrue(ZstdUtils.isZstdCompressionAvailable());
    }

    @Test
    public void testCanTurnOffCaching() {
        try {
            ZstdUtils.setCacheZstdAvailablity(false);
            assertEquals(ZstdUtils.CachedAvailability.DONT_CACHE, ZstdUtils.getCachedZstdAvailability());
            assertTrue(ZstdUtils.isZstdCompressionAvailable());
        } finally {
            ZstdUtils.setCacheZstdAvailablity(true);
        }
    }

    @Test
    public void testTurningOnCachingReEvaluatesAvailability() {
        try {
            ZstdUtils.setCacheZstdAvailablity(false);
            assertEquals(ZstdUtils.CachedAvailability.DONT_CACHE, ZstdUtils.getCachedZstdAvailability());
            ZstdUtils.setCacheZstdAvailablity(true);
            assertEquals(ZstdUtils.CachedAvailability.CACHED_AVAILABLE, ZstdUtils.getCachedZstdAvailability());
        } finally {
            ZstdUtils.setCacheZstdAvailablity(true);
        }
    }

    @Test
    public void shouldBeAbleToSkipAByte() throws IOException {
        final File input = getFile("zstandard.testdata.zst");
        try (InputStream is = new FileInputStream(input)) {
            final ZstdCompressorInputStream in =
                    new ZstdCompressorInputStream(is);
            Assert.assertEquals(1, in.skip(1));
            in.close();
        }
    }

    @Test
    public void singleByteReadWorksAsExpected() throws IOException {

        final File input = getFile("zstandard.testdata.zst");

        final File original = getFile("zstandard.testdata");
        final long originalFileLength = original.length();

        byte[] originalFileContent = new byte[((int) originalFileLength)];

        try (InputStream ois = new FileInputStream(original)) {
            ois.read(originalFileContent);
        }

        try (InputStream is = new FileInputStream(input)) {
            final ZstdCompressorInputStream in =
                    new ZstdCompressorInputStream(is);

            Assert.assertEquals(originalFileContent[0], in.read());
            in.close();
        }
    }

    @Test
    public void singleByteReadConsistentlyReturnsMinusOneAtEof() throws IOException {
        final File input = getFile("zstandard.testdata.zst");
        try (InputStream is = new FileInputStream(input)) {
            final ZstdCompressorInputStream in =
                    new ZstdCompressorInputStream(is);
            IOUtils.toByteArray(in);
            Assert.assertEquals(-1, in.read());
            Assert.assertEquals(-1, in.read());
            in.close();
        }
    }

    @Test
    public void multiByteReadConsistentlyReturnsMinusOneAtEof() throws IOException {
        final File input = getFile("zstandard.testdata.zst");
        byte[] buf = new byte[2];
        try (InputStream is = new FileInputStream(input)) {
            final ZstdCompressorInputStream in =
                    new ZstdCompressorInputStream(is);
            IOUtils.toByteArray(in);
            Assert.assertEquals(-1, in.read(buf));
            Assert.assertEquals(-1, in.read(buf));
            in.close();
        }
    }

    @Test
    public void testZstandardUnarchive() throws Exception {
        final File input = getFile("bla.tar.zst");
        final File output = new File(dir, "bla.tar");
        try (InputStream is = new FileInputStream(input)) {
            final CompressorInputStream in = new CompressorStreamFactory()
                    .createCompressorInputStream("zstd", is);
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
