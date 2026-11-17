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

package org.apache.commons.compress.compressors.zstandard;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.utils.InputStreamStatistics;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.build.AbstractStreamBuilder;
import org.apache.commons.io.input.BoundedInputStream;

import com.github.luben.zstd.BufferPool;
import com.github.luben.zstd.ZstdInputStream;

/**
 * {@link CompressorInputStream} implementation to decode Zstandard encoded stream.
 *
 * <p>
 * This class avoids making the underlying {@code zstd} classes part of the public or protected API. The underlying implementation is provided through the
 * <a href="https://github.com/luben/zstd-jni/">Zstandard JNI</a> library which is based on <a href="https://github.com/facebook/zstd/">zstd</a>.
 * </p>
 *
 * @see <a href="https://github.com/luben/zstd-jni/">Zstandard JNI</a>
 * @see <a href="https://github.com/facebook/zstd/">zstd</a>
 * @since 1.16
 */
public class ZstdCompressorInputStream extends CompressorInputStream implements InputStreamStatistics {

    // @formatter:off
    /**
     * Builds a new {@link ZstdCompressorInputStream}.
     *
     * <p>
     * For example:
     * </p>
     * <pre>{@code
     * ZstdCompressorInputStream s = ZstdCompressorInputStream.builder()
     *   .setPath(path)
     *   .setDict(dictionary)
     *   .get();
     * }
     * </pre>
     *
     * @see #get()
     * @since 1.29.0
     */
    // @formatter:on
    public static final class Builder extends AbstractStreamBuilder<ZstdCompressorInputStream, Builder> {

        private byte[] dict;

        /**
         * Constructs a new builder of {@link ZstdCompressorInputStream}.
         */
        public Builder() {
            // empty
        }

        @Override
        public ZstdCompressorInputStream get() throws IOException {
            return new ZstdCompressorInputStream(this);
        }

        /**
         * Sets the dictionary used to decompress the input.
         * <p>
         * The input must have been compressed with the same dictionary, for example with {@link ZstdCompressorOutputStream.Builder#setDict(byte[])}. The
         * dictionary may be a trained Zstandard dictionary or raw content. The value {@code null} means no dictionary.
         * </p>
         *
         * @param dict The dictionary buffer.
         * @return {@code this} instance.
         * @see <a href="https://facebook.github.io/zstd/zstd_manual.html#Chapter14">Zstd manual Chapter14</a>
         */
        public Builder setDict(final byte[] dict) {
            this.dict = dict;
            return this;
        }
    }

    /**
     * Constructs a new builder of {@link ZstdCompressorInputStream}.
     *
     * @return a new builder of {@link ZstdCompressorInputStream}.
     * @since 1.29.0
     */
    public static Builder builder() {
        return new Builder();
    }

    private final BoundedInputStream countingStream;
    private final ZstdInputStream decIS;

    @SuppressWarnings("resource") // Caller closes
    private ZstdCompressorInputStream(final Builder builder) throws IOException {
        this.decIS = new ZstdInputStream(
                countingStream = BoundedInputStream.builder().setInputStream(builder.getInputStream()).setPropagateClose(false).get());
        if (builder.dict != null) {
            decIS.setDict(builder.dict);
        }
    }

    /**
     * Constructs a new input stream that decompresses zstd-compressed data from the specific input stream.
     *
     * @param in         The input stream of compressed data.
     * @throws IOException if an I/O error occurs.
     */
    public ZstdCompressorInputStream(final InputStream in) throws IOException {
        this.decIS = new ZstdInputStream(countingStream = BoundedInputStream.builder().setInputStream(in).setPropagateClose(false).get());
    }

    /**
     * Constructs a new input stream that decompresses zstd-compressed data from the specific input stream.
     *
     * @param in         The input stream of compressed data.
     * @param bufferPool A configuration of zstd-jni that allows users to customize how buffers are recycled. Either a {@link com.github.luben.zstd.NoPool} or a
     *                   {@link com.github.luben.zstd.RecyclingBufferPool} is allowed here.
     * @throws IOException if an I/O error occurs.
     */
    public ZstdCompressorInputStream(final InputStream in, final BufferPool bufferPool) throws IOException {
        this.decIS = new ZstdInputStream(countingStream = BoundedInputStream.builder().setInputStream(in).setPropagateClose(false).get(), bufferPool);
    }

    @Override
    public int available() throws IOException {
        return decIS.available();
    }

    @Override
    public void close() throws IOException {
        decIS.close();
    }

    /**
     *
     * {@inheritDoc}
     *
     * @since 1.17
     */
    @Override
    public long getCompressedCount() {
        return countingStream.getCount();
    }

    @Override
    public synchronized void mark(final int readLimit) {
        decIS.mark(readLimit);
    }

    @Override
    public boolean markSupported() {
        return decIS.markSupported();
    }

    @Override
    public int read() throws IOException {
        final int ret = decIS.read();
        count(ret == -1 ? 0 : 1);
        return ret;
    }

    @Override
    public int read(final byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(final byte[] buf, final int off, final int len) throws IOException {
        final int ret = decIS.read(buf, off, len);
        count(ret);
        return ret;
    }

    @Override
    public synchronized void reset() throws IOException {
        decIS.reset();
    }

    @Override
    public long skip(final long n) throws IOException {
        return IOUtils.skip(decIS, n);
    }

    @Override
    public String toString() {
        return decIS.toString();
    }
}
