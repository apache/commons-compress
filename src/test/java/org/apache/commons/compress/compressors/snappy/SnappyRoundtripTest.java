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
package org.apache.commons.compress.compressors.snappy;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.compressors.lz77support.Parameters;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

public final class SnappyRoundtripTest extends AbstractTest {

    private static Parameters newParameters(final int windowSize, final int minBackReferenceLength, final int maxBackReferenceLength, final int maxOffset,
            final int maxLiteralLength) {
        return Parameters.builder(windowSize).withMinBackReferenceLength(minBackReferenceLength).withMaxBackReferenceLength(maxBackReferenceLength)
                .withMaxOffset(maxOffset).withMaxLiteralLength(maxLiteralLength).build();
    }

    private void roundTripTest(final byte[] input, final Parameters params) throws IOException {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        try (SnappyCompressorOutputStream sos = new SnappyCompressorOutputStream(os, input.length, params)) {
            sos.write(input);
        }
        try (SnappyCompressorInputStream sis = new SnappyCompressorInputStream(new ByteArrayInputStream(os.toByteArray()), params.getWindowSize())) {
            final byte[] actual = IOUtils.toByteArray(sis);
            assertArrayEquals(input, actual);
        }
    }

    private void roundTripTest(final Path input, final Parameters params) throws IOException {
        final File outputSz = newTempFile(input.getFileName() + ".raw.sz");
        try (OutputStream os = Files.newOutputStream(outputSz.toPath());
                SnappyCompressorOutputStream sos = new SnappyCompressorOutputStream(os, Files.size(input), params)) {
            sos.write(input);
            sos.close();
            assertTrue(sos.isClosed());
        }
        try (SnappyCompressorInputStream sis = new SnappyCompressorInputStream(Files.newInputStream(outputSz.toPath()), params.getWindowSize())) {
            final byte[] expected = Files.readAllBytes(input);
            final byte[] actual = IOUtils.toByteArray(sis);
            assertArrayEquals(expected, actual);
        }
    }

    private void roundTripTest(final String testFile) throws IOException {
        roundTripTest(getPath(testFile), SnappyCompressorOutputStream.createParameterBuilder(SnappyCompressorInputStream.DEFAULT_BLOCK_SIZE).build());
    }

    // yields no compression at all
    @Test
    void testBiggerFileRoundtrip() throws IOException {
        roundTripTest("COMPRESS-256.7z");
    }

    // should yield decent compression
    @Test
    void testBlaTarRoundtrip() throws IOException {
        // System.err.println("Configuration: default");
        roundTripTest("bla.tar");
    }

    @Test
    void testBlaTarRoundtripTunedForCompressionRatio() throws IOException {
        // System.err.println("Configuration: tuned for compression ratio");
        roundTripTest(getPath("bla.tar"),
                SnappyCompressorOutputStream.createParameterBuilder(SnappyCompressorInputStream.DEFAULT_BLOCK_SIZE).tunedForCompressionRatio().build());
    }

    @Test
    void testBlaTarRoundtripTunedForSpeed() throws IOException {
        // System.err.println("Configuration: tuned for speed");
        roundTripTest(getPath("bla.tar"),
                SnappyCompressorOutputStream.createParameterBuilder(SnappyCompressorInputStream.DEFAULT_BLOCK_SIZE).tunedForSpeed().build());
    }

    // yields no compression at all
    @Test
    void testGzippedLoremIpsumRoundtrip() throws IOException {
        roundTripTest("lorem-ipsum.txt.gz");
    }

    @Test
    void testTryReallyBigOffset() throws IOException {
        // "normal" Snappy files will never reach offsets beyond
        // 16bits (i.e. those using four bytes to encode the length)
        // as the block size is only 32k. This means we never execute
        // the code for four-byte length copies in either stream class
        // using real-world Snappy files.
        // This is an artificial stream using a bigger block size that
        // may not even be expandable by other Snappy implementations.
        // Start with the four byte sequence 0000 after that add > 64k
        // of random noise that doesn't contain any 0000 at all, then
        // add 0000.
        final ByteArrayOutputStream fs = new ByteArrayOutputStream((1 << 16) + 1024);
        fs.write(0);
        fs.write(0);
        fs.write(0);
        fs.write(0);
        final int cnt = 1 << 16 + 5;
        final Random r = new Random();
        for (int i = 0; i < cnt; i++) {
            fs.write(r.nextInt(255) + 1);
        }
        fs.write(0);
        fs.write(0);
        fs.write(0);
        fs.write(0);

        roundTripTest(fs.toByteArray(), newParameters(1 << 17, 4, 64, 1 << 17 - 1, 1 << 17 - 1));
    }

    @Test
    void testTryReallyLongLiterals() throws IOException {
        // "normal" Snappy files will never reach literal blocks with
        // length beyond 16bits (i.e. those using three or four bytes
        // to encode the length) as the block size is only 32k. This
        // means we never execute the code for the three/four byte
        // length literals in either stream class using real-world
        // Snappy files.
        // What we'd need would be a sequence of bytes with no four
        // byte subsequence repeated that is longer than 64k, we try
        // our best with random, but will probably only hit the three byte
        // methods in a few lucky cases.
        // The four byte methods would require even more luck and a
        // buffer (and a file written to disk) that was 2^5 bigger
        // than the buffer used here.
        final Path path = newTempPath("reallyBigLiteralTest");
        try (OutputStream outputStream = Files.newOutputStream(path)) {
            final int cnt = 1 << 19;
            final Random r = new Random();
            for (int i = 0; i < cnt; i++) {
                outputStream.write(r.nextInt(256));
            }
        }
        roundTripTest(path, newParameters(1 << 18, 4, 64, 1 << 16 - 1, 1 << 18 - 1));
    }
}
