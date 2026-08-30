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
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.CRC32;

/**
 * Compresses a file from disk with the sequential or the parallel encoder and prints wall time, throughput and a CRC of the compressed bytes. Not a unit
 * test: a helper for the wall-clock numbers in NOTES.md. Usage: {@code BZip2EncodeMain <file> sequential|parallel<N> [repeat]}.
 */
public final class BZip2EncodeMain {

    public static void main(final String[] args) throws IOException {
        final byte[] base = Files.readAllBytes(Paths.get(args[0]));
        final String impl = args[1];
        final int repeat = args.length > 2 ? Integer.parseInt(args[2]) : 1;
        final byte[] data = new byte[base.length * repeat];
        for (int i = 0; i < repeat; i++) {
            System.arraycopy(base, 0, data, i * base.length, base.length);
        }
        final CRC32 crc = new CRC32();
        final long[] count = { 0 };
        final OutputStream sink = new OutputStream() {

            @Override
            public void write(final byte[] b, final int off, final int len) {
                crc.update(b, off, len);
                count[0] += len;
            }

            @Override
            public void write(final int b) {
                crc.update(b);
                count[0]++;
            }
        };
        ExecutorService executor = null;
        final long start = System.nanoTime();
        try {
            if (impl.startsWith("parallel")) {
                final int threads = Integer.parseInt(impl.substring("parallel".length()));
                executor = Executors.newFixedThreadPool(threads);
                try (OutputStream os = new BZip2CompressorOutputStream(sink, 9, executor, 2 * threads)) {
                    os.write(data);
                }
            } else {
                try (OutputStream os = new BZip2CompressorOutputStream(sink, 9)) {
                    os.write(data);
                }
            }
        } finally {
            if (executor != null) {
                executor.shutdownNow();
            }
        }
        final double seconds = (System.nanoTime() - start) / 1e9;
        System.out.printf("%s: %,d bytes in -> %,d bytes out in %.2f s = %.1f MB/s (input), crc32=%08x%n", impl, data.length, count[0], seconds,
                data.length / seconds / 1e6, crc.getValue());
    }

    private BZip2EncodeMain() {
    }
}
