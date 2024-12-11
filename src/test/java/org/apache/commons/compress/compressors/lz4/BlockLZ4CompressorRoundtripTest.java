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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.stream.Stream;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.compressors.lz77support.Parameters;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public final class BlockLZ4CompressorRoundtripTest extends AbstractTest {

    public static Stream<Arguments> factory() {
        return Stream.of(Arguments.of("default", BlockLZ4CompressorOutputStream.createParameterBuilder().build()),
                Arguments.of("tuned for speed", BlockLZ4CompressorOutputStream.createParameterBuilder().tunedForSpeed().build()),
                Arguments.of("tuned for compression ratio", BlockLZ4CompressorOutputStream.createParameterBuilder().tunedForCompressionRatio().build()));
    }

    // yields no compression at all
    @ParameterizedTest
    @MethodSource("factory")
    public void biggerFileRoundtrip(final String config, final Parameters params) throws IOException {
        roundTripTest("COMPRESS-256.7z", config, params);
    }

    // should yield decent compression
    @ParameterizedTest
    @MethodSource("factory")
    public void blaTarRoundtrip(final String config, final Parameters params) throws IOException {
        roundTripTest("bla.tar", config, params);
    }

    // yields no compression at all
    @ParameterizedTest
    @MethodSource("factory")
    public void gzippedLoremIpsumRoundtrip(final String config, final Parameters params) throws IOException {
        roundTripTest("lorem-ipsum.txt.gz", config, params);
    }

    private void roundTripTest(final String testFile, final String config, final Parameters params) throws IOException {
        final File input = getFile(testFile);
        final File outputSz = newTempFile(input.getName() + ".block.lz4");
        try (OutputStream os = Files.newOutputStream(outputSz.toPath());
                BlockLZ4CompressorOutputStream los = new BlockLZ4CompressorOutputStream(os, params)) {
            los.write(input);
        }
        try (InputStream is = Files.newInputStream(input.toPath());
                BlockLZ4CompressorInputStream sis = new BlockLZ4CompressorInputStream(Files.newInputStream(outputSz.toPath()))) {
            final byte[] expected = IOUtils.toByteArray(is);
            final byte[] actual = IOUtils.toByteArray(sis);
            assertArrayEquals(expected, actual);
        }
    }

}
