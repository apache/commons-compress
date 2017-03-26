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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.compressors.lz77support.Parameters;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runner.RunWith;

@RunWith(Parameterized.class)
public final class BlockLZ4CompressorRoundtripTest extends AbstractTestCase {

    @org.junit.runners.Parameterized.Parameters(name = "using {0}")
    public static Collection<Object[]> factory() {
        return Arrays.asList(new Object[][] {
                new Object[] { "default", BlockLZ4CompressorOutputStream.createParameterBuilder().build() },
                new Object[] { "tuned for speed",
                    BlockLZ4CompressorOutputStream.createParameterBuilder().tunedForSpeed().build() },
                new Object[] { "tuned for compression ratio",
                    BlockLZ4CompressorOutputStream.createParameterBuilder().tunedForCompressionRatio().build() }
            });
    }

    private final String config;
    private final Parameters params;

    public BlockLZ4CompressorRoundtripTest(String config, Parameters params) {
        this.config = config;
        this.params = params;
    }

    private void roundTripTest(String testFile) throws IOException {
        File input = getFile(testFile);
        long start = System.currentTimeMillis();
        final File outputSz = new File(dir, input.getName() + ".block.lz4");
        try (FileInputStream is = new FileInputStream(input);
             FileOutputStream os = new FileOutputStream(outputSz);
             BlockLZ4CompressorOutputStream los = new BlockLZ4CompressorOutputStream(os, params)) {
            IOUtils.copy(is, los);
        }
        System.err.println("Configuration: " + config);
        System.err.println(input.getName() + " written, uncompressed bytes: " + input.length()
            + ", compressed bytes: " + outputSz.length() + " after " + (System.currentTimeMillis() - start) + "ms");
        start = System.currentTimeMillis();
        try (FileInputStream is = new FileInputStream(input);
             BlockLZ4CompressorInputStream sis = new BlockLZ4CompressorInputStream(new FileInputStream(outputSz))) {
            byte[] expected = IOUtils.toByteArray(is);
            byte[] actual = IOUtils.toByteArray(sis);
            Assert.assertArrayEquals(expected, actual);
        }
        System.err.println(outputSz.getName() + " read after " + (System.currentTimeMillis() - start) + "ms");
    }

    // should yield decent compression
    @Test
    public void blaTarRoundtrip() throws IOException {
        roundTripTest("bla.tar");
    }

    // yields no compression at all
    @Test
    public void gzippedLoremIpsumRoundtrip() throws IOException {
        roundTripTest("lorem-ipsum.txt.gz");
    }

    // yields no compression at all
    @Test
    public void biggerFileRoundtrip() throws IOException {
        roundTripTest("COMPRESS-256.7z");
    }

}
