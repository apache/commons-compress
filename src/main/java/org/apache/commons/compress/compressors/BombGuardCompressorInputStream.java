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
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Objects;

import org.apache.commons.io.input.BoundedInputStream;

/**
 * Wraps a {@link CompressorInputStream} and enforces decompression-bomb limits, throwing a {@link DecompressionBombException}
 * when either is exceeded:
 * <ul>
 * <li>an absolute cap on the number of decompressed bytes, and</li>
 * <li>a mean compression-ratio guard (decompressed / compressed), active only after a configurable grace amount of output, so
 * a legitimately high local ratio at the start of a stream (a header, dictionary, or long run) is tolerated.</li>
 * </ul>
 * <p>
 * The absolute cap is enforced by composing Commons IO's {@link BoundedInputStream} over the wrapped decompressor with a
 * throwing callback (it returns end-of-file at the limit by default; here it throws). The cap is inclusive: reaching
 * {@code maxDecompressedSize} decompressed bytes throws, and a {@code maxDecompressedSize} of {@code 0} permits no output. The
 * ratio guard adds a per-read check using the wrapped {@link CompressorInputStream#getCompressedCount()}, and is skipped while
 * that is {@code -1} (unknown, as for Pack200). Both limits are measured against the bytes this guard returns to the caller, so
 * they are exact and uniform across every format. {@code mark}/{@code reset} are not supported, so bytes cannot be replayed
 * past the guard.
 * </p>
 * <p>
 * Protection is off by default and composed for you by {@link CompressorStreamFactory} when a limit is configured on the
 * factory; a value of {@code -1} disables a limit. This mirrors the established Apache pattern, see Apache Tika's
 * {@code SecureContentHandler}: a sensible starting point for the ratio guard is a maximum ratio around {@code 100} with a
 * grace window covering the largest legitimate high-ratio prefix; usual ratios are well under 10, while DEFLATE alone can reach
 * 1032, which is why a non-trivial ratio and grace are advisable and why this is opt-in rather than on by default.
 * </p>
 *
 * @since 1.29.0
 */
public class BombGuardCompressorInputStream extends CompressorInputStream {

    private final CompressorInputStream source;
    private final InputStream in;
    private final double maxCompressionRatio;
    private final long compressionRatioGraceBytes;

    /**
     * Constructs a guard enforcing only an absolute decompressed-size cap.
     *
     * @param in                  the decompressor to guard.
     * @param maxDecompressedSize  the maximum decompressed size in bytes (inclusive), or {@code -1} for no limit.
     */
    public BombGuardCompressorInputStream(final CompressorInputStream in, final long maxDecompressedSize) {
        this(in, maxDecompressedSize, -1, 0);
    }

    /**
     * Constructs a guard enforcing an absolute decompressed-size cap and a compression-ratio guard.
     *
     * @param in                         the decompressor to guard.
     * @param maxDecompressedSize        the maximum decompressed size in bytes (inclusive), or {@code -1} for no limit.
     * @param maxCompressionRatio        the maximum decompressed/compressed ratio, or a non-positive value for no limit.
     * @param compressionRatioGraceBytes the decompressed output below which the ratio guard is not enforced.
     */
    public BombGuardCompressorInputStream(final CompressorInputStream in, final long maxDecompressedSize, final double maxCompressionRatio,
            final long compressionRatioGraceBytes) {
        this.source = Objects.requireNonNull(in, "in");
        this.maxCompressionRatio = maxCompressionRatio;
        this.compressionRatioGraceBytes = compressionRatioGraceBytes;
        this.in = maxDecompressedSize < 0 ? in : cap(in, maxDecompressedSize);
    }

    private static BoundedInputStream cap(final CompressorInputStream in, final long maxDecompressedSize) {
        try {
            return BoundedInputStream.builder().setInputStream(in).setMaxCount(maxDecompressedSize).setOnMaxCount((max, count) -> {
                throw new DecompressionBombException("Decompressed size reached the configured maximum of %,d bytes.", maxDecompressedSize);
            }).get();
        } catch (final IOException e) {
            // A BoundedInputStream over an already-open InputStream performs no I/O while being built, so this cannot happen.
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public int available() throws IOException {
        return in.available();
    }

    private void checkRatio() throws IOException {
        if (maxCompressionRatio > 0) {
            final long produced = getBytesRead();
            if (produced > compressionRatioGraceBytes) {
                final long compressed = source.getCompressedCount();
                if (compressed > 0 && produced > maxCompressionRatio * compressed) {
                    throw new DecompressionBombException("Compression ratio %.1f (%,d decompressed / %,d compressed) exceeds the configured maximum of %.1f.",
                            (double) produced / compressed, produced, compressed, maxCompressionRatio);
                }
            }
        }
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    @Override
    public long getCompressedCount() {
        return source.getCompressedCount();
    }

    @Override
    public int read() throws IOException {
        final int b = in.read();
        if (b != -1) {
            count(1);
            checkRatio();
        }
        return b;
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        final int read = in.read(b, off, len);
        if (read != -1) {
            count(read);
            checkRatio();
        }
        return read;
    }
}
