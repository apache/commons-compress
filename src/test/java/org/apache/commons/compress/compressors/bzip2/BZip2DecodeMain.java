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
import java.nio.file.Paths;
import java.util.zip.CRC32;

import org.apache.commons.io.input.UnsynchronizedByteArrayInputStream;

/**
 * Decodes one {@code .bz2} file held in memory, reports wall time, throughput and a CRC-32 of the output.
 * <p>
 * Usage: {@code BZip2DecodeMain <file.bz2> [current|legacy|current-concat|legacy-concat] [repeats]} ({@code -concat}: decompress concatenated
 * streams). Compare the CRC with {@code bzip2 -dc file | python3 -c "import sys,zlib; print('%08x' % (zlib.crc32(sys.stdin.buffer.read()) & 0xffffffff))"}
 * (or any CRC-32 tool).
 * </p>
 */
public final class BZip2DecodeMain {

    @SuppressWarnings("resource")
    public static void main(final String[] args) throws IOException {
        final byte[] compressed = Files.readAllBytes(Paths.get(args[0]));
        final String impl = args.length > 1 ? args[1] : "current";
        final int repeats = args.length > 2 ? Integer.parseInt(args[2]) : 1;
        final byte[] buffer = new byte[1 << 16];
        for (int r = 0; r < repeats; r++) {
            final CRC32 crc = new CRC32();
            long total = 0;
            final long start = System.nanoTime();
            try (InputStream in = open(compressed, impl)) {
                int n;
                while ((n = in.read(buffer, 0, buffer.length)) >= 0) {
                    crc.update(buffer, 0, n);
                    total += n;
                }
            }
            final double seconds = (System.nanoTime() - start) / 1e9;
            System.out.printf("%s: %,d bytes in %.2f s = %.1f MB/s (output), %.1f MB/s (input), crc32=%08x%n", impl, total, seconds, total / seconds / 1e6,
                    compressed.length / seconds / 1e6, crc.getValue());
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    private static InputStream open(final byte[] compressed, final String impl) throws IOException {
        final InputStream in = UnsynchronizedByteArrayInputStream.builder().setByteArray(compressed).get();
        if (impl.startsWith("parallel")) {
            final int threads = Integer.parseInt(impl.substring("parallel".length()));
            if (executor == null) {
                executor = java.util.concurrent.Executors.newFixedThreadPool(threads);
            }
            return new BZip2CompressorInputStream(in, true, executor, 2 * threads);
        }
        final boolean concat = impl.endsWith("-concat");
        return impl.startsWith("legacy") ? new LegacyBZip2Decoder(in, concat) : new BZip2CompressorInputStream(in, concat);
    }

    private static java.util.concurrent.ExecutorService executor;

    private BZip2DecodeMain() {
    }
}
