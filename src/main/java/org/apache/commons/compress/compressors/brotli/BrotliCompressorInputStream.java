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

package org.apache.commons.compress.compressors.brotli;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.utils.InputStreamStatistics;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.build.AbstractStreamBuilder;
import org.apache.commons.io.input.BoundedInputStream;
import org.brotli.dec.BrotliInputStream;

/**
 * {@link CompressorInputStream} implementation to decode Brotli encoded stream. Library relies on <a href="https://github.com/google/brotli">Google brotli</a>
 *
 * @since 1.14
 */
public class BrotliCompressorInputStream extends CompressorInputStream implements InputStreamStatistics {

    // @formatter:off
    /**
     * Builds a new {@link BrotliCompressorInputStream}.
     *
     * <p>
     * For example:
     * </p>
     * <pre>{@code
     * BrotliCompressorInputStream s = BrotliCompressorInputStream.builder()
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
    public static final class Builder extends AbstractStreamBuilder<BrotliCompressorInputStream, Builder> {

        private byte[] dict;

        /**
         * Constructs a new builder of {@link BrotliCompressorInputStream}.
         */
        public Builder() {
            // empty
        }

        @Override
        public BrotliCompressorInputStream get() throws IOException {
            return new BrotliCompressorInputStream(this);
        }

        /**
         * Sets the raw (LZ77) dictionary used to decompress the input.
         * <p>
         * The input must have been compressed with the same raw dictionary, which acts as data preceding the stream. The value {@code null} means no
         * dictionary.
         * </p>
         *
         * @param dict The dictionary buffer.
         * @return {@code this} instance.
         */
        public Builder setDict(final byte[] dict) {
            this.dict = dict;
            return this;
        }
    }

    /**
     * Constructs a new builder of {@link BrotliCompressorInputStream}.
     *
     * @return a new builder of {@link BrotliCompressorInputStream}.
     * @since 1.29.0
     */
    public static Builder builder() {
        return new Builder();
    }

    private final BoundedInputStream countingInputStream;
    private final BrotliInputStream brotliInputStream;

    @SuppressWarnings("resource") // Caller closes
    private BrotliCompressorInputStream(final Builder builder) throws IOException {
        brotliInputStream = new BrotliInputStream(countingInputStream = BoundedInputStream.builder().setInputStream(builder.getInputStream()).get(),
                BrotliInputStream.DEFAULT_INTERNAL_BUFFER_SIZE, builder.dict);
    }

    /**
     * Construct a new instance.
     *
     * @param inputStream underlying data source.
     * @throws IOException in case of corrupted data or source stream problems.
     */
    public BrotliCompressorInputStream(final InputStream inputStream) throws IOException {
        brotliInputStream = new BrotliInputStream(countingInputStream = BoundedInputStream.builder().setInputStream(inputStream).get());
    }

    @Override
    public int available() throws IOException {
        return brotliInputStream.available();
    }

    @Override
    public void close() throws IOException {
        brotliInputStream.close();
    }

    /**
     * @since 1.17
     */
    @Override
    public long getCompressedCount() {
        return countingInputStream.getCount();
    }

    @Override
    public synchronized void mark(final int readLimit) {
        brotliInputStream.mark(readLimit);
    }

    @Override
    public boolean markSupported() {
        return brotliInputStream.markSupported();
    }

    @Override
    public int read() throws IOException {
        final int ret = brotliInputStream.read();
        count(ret == -1 ? 0 : 1);
        return ret;
    }

    @Override
    public int read(final byte[] b) throws IOException {
        return brotliInputStream.read(b);
    }

    @Override
    public int read(final byte[] buf, final int off, final int len) throws IOException {
        final int ret = brotliInputStream.read(buf, off, len);
        count(ret);
        return ret;
    }

    @Override
    public synchronized void reset() throws IOException {
        brotliInputStream.reset();
    }

    @Override
    public long skip(final long n) throws IOException {
        return IOUtils.skip(brotliInputStream, n);
    }

    @Override
    public String toString() {
        return brotliInputStream.toString();
    }
}
