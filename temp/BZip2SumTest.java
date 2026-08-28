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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

/**
 * Decompresses each .bz2 file given on the command line and prints the sum of its decompressed bytes (unsigned).
 */
public final class BZip2SumTest {

    public static void main(final String[] args) throws IOException {
        final byte[] buffer = new byte[1 << 16];
        long grandTotal = 0;
        for (final String file : args) {
            long sum = 0;
            long count = 0;
            final long start = System.nanoTime();
            // Files.newInputStream is unbuffered; the decoder refills a few bytes at a time, so buffer the source.
            try (InputStream in = new BZip2CompressorInputStream(new BufferedInputStream(Files.newInputStream(Paths.get(file)), 1 << 16))) {
                int n;
                while ((n = in.read(buffer, 0, buffer.length)) >= 0) {
                    for (int i = 0; i < n; i++) {
                        sum += buffer[i] & 0xff;
                    }
                    count += n;
                }
            }
            final double seconds = (System.nanoTime() - start) / 1e9;
            System.out.printf("%s: %,d bytes, sum = %,d (%.2f s, %.1f MB/s)%n", file, count, sum, seconds, count / seconds / 1e6);
            grandTotal += sum;
        }
        if (args.length > 1) {
            System.out.printf("total sum = %,d%n", grandTotal);
        }
    }

    private BZip2SumTest() {
    }
}
