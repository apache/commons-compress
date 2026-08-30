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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Compression throughput on large corpora, current implementation vs. {@link LegacyBZip2Encoder}; one full compression per measurement. Corpora and
 * invocation as for {@link BZip2DecompressionBenchmark} (the uncompressed files {@code enwiki-64M.txt} and {@code binary-64M.tar}).
 */
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
@Fork(1)
@State(Scope.Thread)
public class BZip2CompressionBenchmark {

    /** Discards everything. */
    private static final class NullOutputStream extends OutputStream {
        @Override
        public void write(final byte[] b, final int off, final int len) {
            // discard
        }

        @Override
        public void write(final int b) {
            // discard
        }
    }

    @Param({ "enwiki-64M.txt", "binary-64M.tar" })
    private String corpus;

    @Param({ "current", "legacy" })
    private String impl;

    @Param({ "9", "1" })
    private int blockSize;

    private byte[] data;

    @Benchmark
    public void compress() throws IOException {
        try (OutputStream out = "legacy".equals(impl) ? new LegacyBZip2Encoder(new NullOutputStream(), blockSize)
                : new BZip2CompressorOutputStream(new NullOutputStream(), blockSize)) {
            out.write(data, 0, data.length);
        }
    }

    @Setup(Level.Trial)
    public void setup() throws IOException {
        final Path dir = Paths.get(System.getProperty("bzip2.bench.dir", System.getProperty("user.home") + "/Downloads/bzip2-bench"));
        data = Files.readAllBytes(dir.resolve(corpus));
    }
}
