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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.Test;

import com.github.luben.zstd.NoPool;
import com.github.luben.zstd.RecyclingBufferPool;

public class ZstdCompressorInputStreamTest extends AbstractTestCase {

    @Test
    public void multiByteReadConsistentlyReturnsMinusOneAtEof() throws IOException {
        final File input = getFile("zstandard.testdata.zst");
        final byte[] buf = new byte[2];
        try (InputStream is = Files.newInputStream(input.toPath());
                ZstdCompressorInputStream in = new ZstdCompressorInputStream(is)) {
            IOUtils.toByteArray(in);
            assertEquals(-1, in.read(buf));
            assertEquals(-1, in.read(buf));
        }
    }

    @Test
    public void shouldBeAbleToSkipAByte() throws IOException {
        final File input = getFile("zstandard.testdata.zst");
        try (InputStream is = Files.newInputStream(input.toPath());
                ZstdCompressorInputStream in = new ZstdCompressorInputStream(is)) {
            assertEquals(1, in.skip(1));
        }
    }

    @Test
    public void singleByteReadConsistentlyReturnsMinusOneAtEof() throws IOException {
        final File input = getFile("zstandard.testdata.zst");
        try (InputStream is = Files.newInputStream(input.toPath());
                ZstdCompressorInputStream in = new ZstdCompressorInputStream(is)) {
            IOUtils.toByteArray(in);
            assertEquals(-1, in.read());
            assertEquals(-1, in.read());
        }
    }

    @Test
    public void singleByteReadWorksAsExpected() throws IOException {

        final File input = getFile("zstandard.testdata.zst");

        final File original = getFile("zstandard.testdata");
        final long originalFileLength = original.length();

        final byte[] originalFileContent = new byte[((int) originalFileLength)];

        try (InputStream ois = Files.newInputStream(original.toPath())) {
            ois.read(originalFileContent);
        }

        try (InputStream is = Files.newInputStream(input.toPath());
                ZstdCompressorInputStream in = new ZstdCompressorInputStream(is)) {
            assertEquals(originalFileContent[0], in.read());
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
    public void testZstandardUnarchive() throws Exception {
        final File input = getFile("bla.tar.zst");
        final File output = new File(dir, "bla.tar");
        try (InputStream is = Files.newInputStream(input.toPath())) {
            try (CompressorInputStream in = new CompressorStreamFactory().createCompressorInputStream("zstd", is);) {
                Files.copy(in, output.toPath());
            }
        }
    }

    /**
     * Test bridge works fine.
     *
     * @throws IOException
     */
    @Test
    public void testZstdDecode() throws IOException {
        final File input = getFile("zstandard.testdata.zst");
        final File expected = getFile("zstandard.testdata");
        try (InputStream inputStream = Files.newInputStream(input.toPath());
                ZstdCompressorInputStream zstdInputStream = new ZstdCompressorInputStream(inputStream)) {
            final byte[] b = new byte[97];
            IOUtils.read(expected, b);
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int readByte = -1;
            while ((readByte = zstdInputStream.read()) != -1) {
                bos.write(readByte);
            }
            assertArrayEquals(b, bos.toByteArray());
        }
    }

    @Test
    public void testZstdDecodeWithNoPool() throws IOException {
        final File input = getFile("zstandard.testdata.zst");
        final File expected = getFile("zstandard.testdata");
        try (InputStream inputStream = Files.newInputStream(input.toPath());
                ZstdCompressorInputStream zstdInputStream = new ZstdCompressorInputStream(inputStream, NoPool.INSTANCE)) {
            final byte[] b = new byte[97];
            IOUtils.read(expected, b);
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int readByte = -1;
            while ((readByte = zstdInputStream.read()) != -1) {
                bos.write(readByte);
            }
            assertArrayEquals(b, bos.toByteArray());
        }
    }

    @Test
    public void testZstdDecodeWithRecyclingBufferPool() throws IOException {
        final File input = getFile("zstandard.testdata.zst");
        final File expected = getFile("zstandard.testdata");
        try (InputStream inputStream = Files.newInputStream(input.toPath());
                ZstdCompressorInputStream zstdInputStream = new ZstdCompressorInputStream(inputStream, RecyclingBufferPool.INSTANCE)) {
            final byte[] b = new byte[97];
            IOUtils.read(expected, b);
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int readByte = -1;
            while ((readByte = zstdInputStream.read()) != -1) {
                bos.write(readByte);
            }
            assertArrayEquals(b, bos.toByteArray());
        }
    }

}
