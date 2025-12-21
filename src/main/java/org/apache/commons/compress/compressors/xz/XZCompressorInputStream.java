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
package org.apache.commons.compress.compressors.xz;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.MemoryLimitException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.lzma.LZMACompressorInputStream;
import org.apache.commons.compress.compressors.lzma.LZMACompressorOutputStream;
import org.apache.commons.compress.utils.InputStreamStatistics;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.build.AbstractStreamBuilder;
import org.apache.commons.io.input.BoundedInputStream;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.SingleXZInputStream;
import org.tukaani.xz.XZ;
import org.tukaani.xz.XZInputStream;

// @formatter:off
/**
 * XZ decompressor.
 * <p>
 * For example:
 * </p>
 * <pre>{@code
 * XZCompressorInputStream s = XZCompressorInputStream.builder()
 *   .setPath(path)
 *   .setDecompressConcatenated(false)
 *   .setMemoryLimitKiB(-1)
 *   .get();
 * }
 * </pre>
 * @since 1.4
 */
// @formatter:on
public class XZCompressorInputStream extends CompressorInputStream implements InputStreamStatistics {

    // @formatter:off
    /**
     * Builds a new {@link LZMACompressorInputStream}.
     *
     * <p>
     * For example:
     * </p>
     * <pre>{@code
     * XZCompressorInputStream s = XZCompressorInputStream.builder()
     *   .setPath(path)
     *   .setDecompressConcatenated(false)
     *   .setMemoryLimitKiB(-1)
     *   .get();
     * }
     * </pre>
     *
     * @see #get()
     * @see LZMA2Options
     * @since 1.28.0
     */
    // @formatter:on
    public static class Builder extends AbstractStreamBuilder<XZCompressorInputStream, Builder> {

        private int memoryLimitKiB = -1;
        private boolean decompressConcatenated;

        /**
         * Constructs a new instance.
         */
        public Builder() {
            // Default constructor
        }

        @Override
        public XZCompressorInputStream get() throws IOException {
            return new XZCompressorInputStream(this);
        }

        /**
         * Whether to decompress until the end of the input.
         *
         * @param decompressConcatenated if true, decompress until the end of the input; if false, stop after the first .xz stream and leave the input position
         *                               to point to the next byte after the .xz stream
         * @return {@code this} instance.
         */
        public Builder setDecompressConcatenated(final boolean decompressConcatenated) {
            this.decompressConcatenated = decompressConcatenated;
            return this;
        }


        /**
         * Sets a working memory threshold in kibibytes (KiB).
         *
         * @param memoryLimitKiB The memory limit used when reading blocks. The memory usage limit is expressed in kibibytes (KiB) or {@code -1} to impose no
         *                       memory usage limit. If the estimated memory limit is exceeded on {@link #read()}, a {@link MemoryLimitException} is thrown.
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
     * Checks if the signature matches what is expected for a .xz file.
     *
     * @param signature the bytes to check
     * @param length    the number of bytes to check
     * @return true if signature matches the .xz magic bytes, false otherwise
     */
    public static boolean matches(final byte[] signature, final int length) {
        if (length < XZ.HEADER_MAGIC.length) {
            return false;
        }

        for (int i = 0; i < XZ.HEADER_MAGIC.length; ++i) {
            if (signature[i] != XZ.HEADER_MAGIC[i]) {
                return false;
            }
        }

        return true;
    }

    private final BoundedInputStream countingStream;

    private final InputStream in;

    @SuppressWarnings("resource") // Caller closes
    private XZCompressorInputStream(final Builder builder) throws IOException {
        countingStream = BoundedInputStream.builder().setInputStream(builder.getInputStream()).get();
        if (builder.decompressConcatenated) {
            in = new XZInputStream(countingStream, builder.memoryLimitKiB);
        } else {
            in = new SingleXZInputStream(countingStream, builder.memoryLimitKiB);
        }
    }

    /**
     * Creates a new input stream that decompresses XZ-compressed data from the specified input stream. This doesn't support concatenated .xz files.
     *
     * @param inputStream where to read the compressed data
     * @throws IOException if the input is not in the .xz format, the input is corrupt or truncated, the .xz headers specify options that are not supported by
     *                     this implementation, or the underlying {@code inputStream} throws an exception
     */
    public XZCompressorInputStream(final InputStream inputStream) throws IOException {
        this(builder().setInputStream(inputStream));
    }

    /**
     * Creates a new input stream that decompresses XZ-compressed data from the specified input stream.
     *
     * @param inputStream            where to read the compressed data
     * @param decompressConcatenated if true, decompress until the end of the input; if false, stop after the first .xz stream and leave the input position to
     *                               point to the next byte after the .xz stream
     *
     * @throws IOException if the input is not in the .xz format, the input is corrupt or truncated, the .xz headers specify options that are not supported by
     *                     this implementation, or the underlying {@code inputStream} throws an exception
     * @deprecated Use {@link #builder()}.
     */
    @Deprecated
    public XZCompressorInputStream(final InputStream inputStream, final boolean decompressConcatenated) throws IOException {
        this(builder().setInputStream(inputStream).setDecompressConcatenated(decompressConcatenated));
    }

    /**
     * Creates a new input stream that decompresses XZ-compressed data from the specified input stream.
     *
     * @param inputStream            where to read the compressed data
     * @param decompressConcatenated if true, decompress until the end of the input; if false, stop after the first .xz stream and leave the input position to
     *                               point to the next byte after the .xz stream
     * @param memoryLimitKiB         The memory limit used when reading blocks. The memory usage limit is expressed in kibibytes (KiB) or {@code -1} to impose
     *                               no memory usage limit. If the estimated memory limit is exceeded on {@link #read()}, a {@link MemoryLimitException} is
     *                               thrown.
     *
     * @throws IOException if the input is not in the .xz format, the input is corrupt or truncated, the .xz headers specify options that are not supported by
     *                     this implementation, or the underlying {@code inputStream} throws an exception
     *
     * @deprecated Use {@link #builder()}.
     * @since 1.14
     */
    @Deprecated
    public XZCompressorInputStream(final InputStream inputStream, final boolean decompressConcatenated, final int memoryLimitKiB) throws IOException {
        this(builder().setInputStream(inputStream).setDecompressConcatenated(decompressConcatenated).setMemoryLimitKiB(memoryLimitKiB));
    }


    @Override
    public int available() throws IOException {
        return in.available();
    }

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

    MemoryLimitException newMemoryLimitException(final org.tukaani.xz.MemoryLimitException e) {
        return new MemoryLimitException(e.getMemoryNeeded(), e.getMemoryLimit(), (Throwable) e);
    }

    @Override
    public int read() throws IOException {
        try {
            final int ret = in.read();
            count(ret == -1 ? -1 : 1);
            return ret;
        } catch (final org.tukaani.xz.MemoryLimitException e) {
            // Convert to Commons Compress MemoryLimtException
            throw newMemoryLimitException(e);
        }
    }

    @Override
    public int read(final byte[] buf, final int off, final int len) throws IOException {
        try {
            final int ret = in.read(buf, off, len);
            count(ret);
            return ret;
        } catch (final org.tukaani.xz.MemoryLimitException e) {
            // Convert to Commons Compress MemoryLimtException
            throw newMemoryLimitException(e);
        }
    }

    @Override
    public long skip(final long n) throws IOException {
        try {
            return IOUtils.skip(in, n);
        } catch (final org.tukaani.xz.MemoryLimitException e) {
            // Convert to Commons Compress MemoryLimtException
            throw newMemoryLimitException(e);
        }
    }
}
