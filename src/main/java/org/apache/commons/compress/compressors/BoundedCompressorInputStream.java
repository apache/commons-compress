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
package org.apache.commons.compress.compressors;

import java.io.IOException;
import java.util.Objects;

import org.apache.commons.io.input.BoundedInputStream;

/**
 * Limits the number of decompressed bytes a {@link CompressorInputStream} returns.
 * <p>
 * The limit is enforced with a {@link BoundedInputStream} over the wrapped stream: at most {@code maxDecompressedSize} bytes are returned, a stream whose
 * output is exactly that size ends normally, and a stream with more output throws a {@link CompressorException} from the read that would exceed the limit.
 * Once the limit has been exceeded, every further read throws. Bytes are counted as they are returned, so sizes declared in the compressed input play no
 * part. {@code mark} and {@code reset} are not supported because they could rewind the count.
 * </p>
 *
 * @see CompressorStreamFactory.Builder#setMaxDecompressedSize(long)
 */
final class BoundedCompressorInputStream extends CompressorInputStream {

    private final CompressorInputStream in;
    private final BoundedInputStream bounded;
    private boolean exceeded;

    BoundedCompressorInputStream(final CompressorInputStream in, final long maxDecompressedSize) throws IOException {
        if (maxDecompressedSize < 0) {
            throw new IllegalArgumentException("maxDecompressedSize must not be negative: " + maxDecompressedSize);
        }
        this.in = Objects.requireNonNull(in, "in");
        // Reaching the limit is fine when the wrapped stream ends there; more output is an error.
        // @formatter:off
        this.bounded = BoundedInputStream.builder()
                .setInputStream(in)
                .setMaxCount(maxDecompressedSize)
                .setOnMaxCount((max, count) -> {
                    if (exceeded || in.read() != -1) {
                        exceeded = true;
                        throw new CompressorException("Decompressed size exceeds the configured maximum of %,d bytes.", max);
                    }
                })
                .get();
        // @formatter:on
    }

    @Override
    public int available() throws IOException {
        return bounded.available();
    }

    @Override
    public void close() throws IOException {
        bounded.close();
    }

    @Override
    public long getCompressedCount() throws IOException {
        return in.getCompressedCount();
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public int read() throws IOException {
        final int b = bounded.read();
        if (b != -1) {
            count(1);
        }
        return b;
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        if (len == 0) {
            return 0;
        }
        final int n = bounded.read(b, off, len);
        count(n);
        return n;
    }
}
