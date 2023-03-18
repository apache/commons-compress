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
package org.apache.commons.compress.compressors.snappy;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Random;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.compressors.lz77support.Parameters;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.Test;

public final class SnappyRoundtripTest extends AbstractTestCase {

    private static Parameters newParameters(final int windowSize, final int minBackReferenceLength, final int maxBackReferenceLength,
        final int maxOffset, final int maxLiteralLength) {
        return Parameters.builder(windowSize)
            .withMinBackReferenceLength(minBackReferenceLength)
            .withMaxBackReferenceLength(maxBackReferenceLength)
            .withMaxOffset(maxOffset)
            .withMaxLiteralLength(maxLiteralLength)
            .build();
    }

    // yields no compression at all
    @Test
    public void biggerFileRoundtrip() throws IOException {
        roundTripTest("COMPRESS-256.7z");
    }

    // should yield decent compression
    @Test
    public void blaTarRoundtrip() throws IOException {
        // System.err.println("Configuration: default");
        roundTripTest("bla.tar");
    }

    @Test
    public void blaTarRoundtripTunedForCompressionRatio() throws IOException {
        // System.err.println("Configuration: tuned for compression ratio");
        roundTripTest(getFile("bla.tar"),
            SnappyCompressorOutputStream.createParameterBuilder(SnappyCompressorInputStream.DEFAULT_BLOCK_SIZE)
                .tunedForCompressionRatio()
                .build());
    }

    @Test
    public void blaTarRoundtripTunedForSpeed() throws IOException {
        // System.err.println("Configuration: tuned for speed");
        roundTripTest(getFile("bla.tar"),
            SnappyCompressorOutputStream.createParameterBuilder(SnappyCompressorInputStream.DEFAULT_BLOCK_SIZE)
                .tunedForSpeed()
                .build());
    }

    // yields no compression at all
    @Test
    public void gzippedLoremIpsumRoundtrip() throws IOException {
        roundTripTest("lorem-ipsum.txt.gz");
    }

    private void roundTripTest(final byte[] input, final Parameters params) throws IOException {
        long start = System.currentTimeMillis();
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        try (
             SnappyCompressorOutputStream sos = new SnappyCompressorOutputStream(os, input.length, params)) {
            sos.write(input);
        }
        // System.err.println("byte array" + " written, uncompressed bytes: " + input.length
        //    + ", compressed bytes: " + os.size() + " after " + (System.currentTimeMillis() - start) + "ms");
        start = System.currentTimeMillis();
        try (
             SnappyCompressorInputStream sis = new SnappyCompressorInputStream(new ByteArrayInputStream(os.toByteArray()),
                 params.getWindowSize())) {
            final byte[] actual = IOUtils.toByteArray(sis);
            assertArrayEquals(input, actual);
        }
        // System.err.println("byte array" + " read after " + (System.currentTimeMillis() - start) + "ms");
    }

    private void roundTripTest(final File input, final Parameters params) throws IOException {
        long start = System.currentTimeMillis();
        final File outputSz = new File(dir, input.getName() + ".raw.sz");
        try (OutputStream os = Files.newOutputStream(outputSz.toPath());
                SnappyCompressorOutputStream sos = new SnappyCompressorOutputStream(os, input.length(), params)) {
            Files.copy(input.toPath(), sos);
        }
        // System.err.println(input.getName() + " written, uncompressed bytes: " + input.length()
        // + ", compressed bytes: " + outputSz.length() + " after " + (System.currentTimeMillis() - start) + "ms");
        start = System.currentTimeMillis();
        try (InputStream is = Files.newInputStream(input.toPath());
                SnappyCompressorInputStream sis = new SnappyCompressorInputStream(Files.newInputStream(outputSz.toPath()), params.getWindowSize())) {
            final byte[] expected = IOUtils.toByteArray(is);
            final byte[] actual = IOUtils.toByteArray(sis);
            assertArrayEquals(expected, actual);
        }
        // System.err.println(outputSz.getName() + " read after " + (System.currentTimeMillis() - start) + "ms");
    }

    private void roundTripTest(final String testFile) throws IOException {
        roundTripTest(getFile(testFile),
            SnappyCompressorOutputStream.createParameterBuilder(SnappyCompressorInputStream.DEFAULT_BLOCK_SIZE)
                .build());
    }

    @Test
    public void tryReallyBigOffset() throws IOException {
        // "normal" Snappy files will never reach offsets beyond
        // 16bits (i.e. those using four bytes to encode the length)
        // as the block size is only 32k. This means we never execute
        // the code for four-byte length copies in either stream class
        // using real-world Snappy files.
        // This is an artifical stream using a bigger block size that
        // may not even be expandable by other Snappy implementations.
        // Start with the four byte sequence 0000 after that add > 64k
        // of random noise that doesn't contain any 0000 at all, then
        // add 0000.
        final File f = new File(dir, "reallyBigOffsetTest");
        final ByteArrayOutputStream fs = new ByteArrayOutputStream((1<<16) + 1024);
            fs.write(0);
            fs.write(0);
            fs.write(0);
            fs.write(0);
            final int cnt = 1 << 16 + 5;
            final Random r = new Random();
            for (int i = 0 ; i < cnt; i++) {
                fs.write(r.nextInt(255) + 1);
            }
            fs.write(0);
            fs.write(0);
            fs.write(0);
            fs.write(0);

        roundTripTest(fs.toByteArray(), newParameters(1 << 17, 4, 64, 1 << 17 - 1, 1 << 17 - 1));
    }

    @Test
    public void tryReallyLongLiterals() throws IOException {
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
        final File f = new File(dir, "reallyBigLiteralTest");
        try (OutputStream fs = Files.newOutputStream(f.toPath())) {
            final int cnt = 1 << 19;
            final Random r = new Random();
            for (int i = 0 ; i < cnt; i++) {
                fs.write(r.nextInt(256));
            }
        }
        roundTripTest(f, newParameters(1 << 18, 4, 64, 1 << 16 - 1, 1 << 18 - 1));
    }
}
