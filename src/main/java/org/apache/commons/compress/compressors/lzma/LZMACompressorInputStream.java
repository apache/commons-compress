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
package org.apache.commons.compress.compressors.lzma;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.MemoryLimitException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.utils.InputStreamStatistics;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.build.AbstractStreamBuilder;
import org.apache.commons.io.input.BoundedInputStream;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.LZMAInputStream;

/**
 * LZMA decompressor.
 *
 * @since 1.6
 */
public class LZMACompressorInputStream extends CompressorInputStream implements InputStreamStatistics {

    // @formatter:off
    /**
     * Builds a new {@link LZMACompressorInputStream}.
     *
     * <p>
     * For example:
     * </p>
     * <pre>{@code
     * LZMACompressorOutputStream s = LZMACompressorInputStream.builder()
     *   .setPath(path)
     *   .get();
     * }
     * </pre>
     *
     * @see #get()
     * @see LZMA2Options
     * @since 1.28.0
     */
    // @formatter:on
    public static class Builder extends AbstractStreamBuilder<LZMACompressorInputStream, Builder> {

        private int memoryLimitKiB = -1;

        @Override
        public LZMACompressorInputStream get() throws IOException {
            return new LZMACompressorInputStream(this);
        }

        /**
         * Sets a working memory threshold in kibibytes (KiB).
         *
         * @param memoryLimitKiB Sets a working memory threshold in kibibytes (KiB). Processing throws MemoryLimitException if memory use is above this
         *                       threshold.
         * @return {@code this} instance.
         */
        public Builder setMemoryLimitKiB(final int memoryLimitKiB) {
            this.memoryLimitKiB = memoryLimitKiB;
            return this;
        }
    }

    /**
     * Constructs a new builder of {@link LZMACompressorOutputStream}.
     *
     * @return a new builder of {@link LZMACompressorOutputStream}.
     * @since 1.28.0
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Checks if the signature matches what is expected for an LZMA file.
     *
     * @param signature the bytes to check
     * @param length    the number of bytes to check
     * @return true, if this stream is an LZMA compressed stream, false otherwise
     * @since 1.10
     */
    public static boolean matches(final byte[] signature, final int length) {
        return signature != null && length >= 3 && signature[0] == 0x5d && signature[1] == 0 && signature[2] == 0;
    }

    private final BoundedInputStream countingStream;

    private final InputStream in;

    @SuppressWarnings("resource") // Caller closes
    private LZMACompressorInputStream(final Builder builder) throws IOException {
        try {
            in = new LZMAInputStream(countingStream = BoundedInputStream.builder().setInputStream(builder.getInputStream()).get(), builder.memoryLimitKiB);
        } catch (final org.tukaani.xz.MemoryLimitException e) {
            // convert to Commons Compress exception
            throw new MemoryLimitException(e.getMemoryNeeded(), e.getMemoryLimit(), (Throwable) e);
        }
    }

    /**
     * Creates a new input stream that decompresses LZMA-compressed data from the specified input stream.
     *
     * @param inputStream where to read the compressed data
     * @throws IOException if the input is not in the .lzma format, the input is corrupt or truncated, the .lzma headers specify sizes that are not supported by
     *                     this implementation, or the underlying {@code inputStream} throws an exception
     */
    public LZMACompressorInputStream(final InputStream inputStream) throws IOException {
        this(builder().setInputStream(inputStream));
    }

    /**
     * Creates a new input stream that decompresses LZMA-compressed data from the specified input stream.
     *
     * @param inputStream     where to read the compressed data
     * @param memoryLimitKiB Sets a working memory threshold in kibibytes (KiB). Processing throws MemoryLimitException if memory use is above this threshold.
     * @throws IOException if the input is not in the .lzma format, the input is corrupt or truncated, the .lzma headers specify sizes that are not supported by
     *                     this implementation, or the underlying {@code inputStream} throws an exception
     *
     * @since 1.14
     * @deprecated Use {@link #builder()}.
     */
    @Deprecated
    public LZMACompressorInputStream(final InputStream inputStream, final int memoryLimitKiB) throws IOException {
        this(builder().setInputStream(inputStream).setMemoryLimitKiB(memoryLimitKiB));
    }

    /** {@inheritDoc} */
    @Override
    public int available() throws IOException {
        return in.available();
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        in.close();
    }

    /**
     * @since 1.17
     */
    @Override
    public long getCompressedCount() {
        return countingStream.getCount();
    }

    /** {@inheritDoc} */
    @Override
    public int read() throws IOException {
        final int ret = in.read();
        count(ret == -1 ? 0 : 1);
        return ret;
    }

    /** {@inheritDoc} */
    @Override
    public int read(final byte[] buf, final int off, final int len) throws IOException {
        final int ret = in.read(buf, off, len);
        count(ret);
        return ret;
    }

    /** {@inheritDoc} */
    @Override
    public long skip(final long n) throws IOException {
        return IOUtils.skip(in, n);
    }
}
