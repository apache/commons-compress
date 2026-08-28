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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.input.UnsynchronizedByteArrayInputStream;
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
 * Decompression throughput on large corpora, current implementation vs. {@link LegacyBZip2Decoder}.
 * <p>
 * The corpora are not part of the repository; point {@code -Dbzip2.bench.dir} at a directory containing them (see {@code NOTES.md}). Run with:
 * </p>
 *
 * <pre>
 * mvn -q test-compile dependency:build-classpath -Dmdep.outputFile=target/cp.txt
 * java -cp target/test-classes:target/classes:$(cat target/cp.txt) org.openjdk.jmh.Main BZip2DecompressionBenchmark \
 *     -jvmArgs -Dbzip2.bench.dir=/path/to/corpora [-p corpus=enwiki-64M.9.bz2] [-p impl=current] [-p concatenated=false]
 * </pre>
 * <p>
 * Reported time is per full decode of the corpus; MB/s of decompressed output = corpus size / time.
 * </p>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 10)
@Measurement(iterations = 5, time = 10)
@Fork(1)
@State(Scope.Thread)
public class BZip2DecompressionBenchmark {

    @Param({ "enwiki-64M.9.bz2", "enwiki-64M.1.bz2", "binary-64M.9.bz2" })
    private String corpus;

    @Param({ "current", "legacy" })
    private String impl;

    /**
     * Whether to decompress concatenated streams (the decoder then reads its input in bulk).
     */
    @Param({ "false", "true" })
    private boolean concatenated;

    private byte[] compressed;
    private final byte[] buffer = new byte[65536];

    @Benchmark
    public long decompress() throws IOException {
        try (InputStream in = open()) {
            long total = 0;
            int n;
            while ((n = in.read(buffer, 0, buffer.length)) >= 0) {
                total += n;
            }
            return total;
        }
    }

    private InputStream open() throws IOException {
        final InputStream in = UnsynchronizedByteArrayInputStream.builder().setByteArray(compressed).get();
        return "legacy".equals(impl) ? new LegacyBZip2Decoder(in, concatenated) : new BZip2CompressorInputStream(in, concatenated);
    }

    @Setup(Level.Trial)
    public void setup() throws IOException {
        final Path dir = Paths.get(System.getProperty("bzip2.bench.dir", System.getProperty("user.home") + "/Downloads/bzip2-bench"));
        compressed = Files.readAllBytes(dir.resolve(corpus));
    }
}
