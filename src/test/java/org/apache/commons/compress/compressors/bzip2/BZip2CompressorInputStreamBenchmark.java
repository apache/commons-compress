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

package org.apache.commons.compress.compressors.bzip2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.AbstractTest;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.UnsynchronizedByteArrayInputStream;
import org.apache.commons.io.output.NullOutputStream;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

/**
 * Run this test: mvn clean test -Pbenchmark -Dbenchmark=BZip2CompressorInputStreamBenchmark
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class BZip2CompressorInputStreamBenchmark {

    @State(Scope.Thread)
    public static class DecompressionState {
        private byte[] compressedData;

        /**
         * Load the compressed data into memory and verify that it decompresses to the expected output.
         *
         * @throws IOException
         */
        @Setup(Level.Trial)
        public void setup() throws IOException {
            compressedData = AbstractTest.readAllBytes("lorem-ipsum.txt.bz2");

            try (InputStream is = new BZip2CompressorInputStream(UnsynchronizedByteArrayInputStream.builder().setByteArray(compressedData).get())) {
                final byte[] data = IOUtils.toByteArray(is);
                assertEquals(144060, data.length);
                assertEquals("a00c4f3f36515c96b2faef71c054e7f3e86a4f0f4ed4824cb7c5293bb455d28a", DigestUtils.sha256Hex(data));
            }
        }
    }

    @Benchmark
    public void testDecompress(final DecompressionState state) throws IOException {
        try (InputStream is = new BZip2CompressorInputStream(UnsynchronizedByteArrayInputStream.builder().setByteArray(state.compressedData).get())) {
            IOUtils.copy(is, NullOutputStream.INSTANCE);
        }
    }
}
