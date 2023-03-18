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
package org.apache.commons.compress.compressors.lz4;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.stream.Stream;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public final class FramedLZ4CompressorRoundtripTest extends AbstractTestCase {

    public static Stream<Arguments> factory() {
        return Stream.of(
                Arguments.of(new FramedLZ4CompressorOutputStream.Parameters(FramedLZ4CompressorOutputStream.BlockSize.K64)),
                Arguments.of(new FramedLZ4CompressorOutputStream.Parameters(FramedLZ4CompressorOutputStream.BlockSize.K256)),
                Arguments.of(new FramedLZ4CompressorOutputStream.Parameters(FramedLZ4CompressorOutputStream.BlockSize.M1)),
                Arguments.of(FramedLZ4CompressorOutputStream.Parameters.DEFAULT),
                // default without content checksum
                Arguments.of(new FramedLZ4CompressorOutputStream.Parameters(FramedLZ4CompressorOutputStream.BlockSize.M4,
                    false, false, false)),
                // default with block checksum
                Arguments.of(new FramedLZ4CompressorOutputStream.Parameters(FramedLZ4CompressorOutputStream.BlockSize.M4,
                    true, true, false)),
                // small blocksize (so we get enough blocks) and enabled block dependency, otherwise defaults
                Arguments.of(new FramedLZ4CompressorOutputStream.Parameters(FramedLZ4CompressorOutputStream.BlockSize.K64,
                    true, false, true)),
                // default, tuned for speed
                Arguments.of(new FramedLZ4CompressorOutputStream.Parameters(FramedLZ4CompressorOutputStream.BlockSize.M4,
                    true, false, false, BlockLZ4CompressorOutputStream.createParameterBuilder()
                        .tunedForSpeed().build())));
    }

    @ParameterizedTest
    @MethodSource("factory")
    public void biggerFileRoundtrip(final FramedLZ4CompressorOutputStream.Parameters params) throws IOException {
        roundTripTest("COMPRESS-256.7z", params);
    }

    // should yield decent compression
    @ParameterizedTest
    @MethodSource("factory")
    public void blaTarRoundtrip(final FramedLZ4CompressorOutputStream.Parameters params) throws IOException {
        roundTripTest("bla.tar", params);
    }

    // yields no compression at all
    @ParameterizedTest
    @MethodSource("factory")
   public void gzippedLoremIpsumRoundtrip(final FramedLZ4CompressorOutputStream.Parameters params) throws IOException {
        roundTripTest("lorem-ipsum.txt.gz", params);
    }

    private void roundTripTest(final String testFile, final FramedLZ4CompressorOutputStream.Parameters params) throws IOException {
        final File input = getFile(testFile);
        long start = System.currentTimeMillis();
        final File outputSz = new File(dir, input.getName() + ".framed.lz4");
        byte[] expected;
        try (InputStream is = Files.newInputStream(input.toPath())) {
            expected = IOUtils.toByteArray(is);
        }
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (FramedLZ4CompressorOutputStream los = new FramedLZ4CompressorOutputStream(bos, params)) {
            IOUtils.copy(new ByteArrayInputStream(expected), los);
        }
        // System.err.println(input.getName() + " written, uncompressed bytes: " + input.length()
        // + ", compressed bytes: " + outputSz.length() + " after " + (System.currentTimeMillis() - start) + "ms");
        start = System.currentTimeMillis();
        try (FramedLZ4CompressorInputStream sis = new FramedLZ4CompressorInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            final byte[] actual = IOUtils.toByteArray(sis);
            assertArrayEquals(expected, actual);
        }

        // System.err.println(outputSz.getName() + " read after " + (System.currentTimeMillis() - start) + "ms");
    }

}
