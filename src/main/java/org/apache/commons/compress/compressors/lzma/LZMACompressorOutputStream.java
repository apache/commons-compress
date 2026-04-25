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
import java.io.OutputStream;

import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.io.build.AbstractStreamBuilder;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.LZMAOutputStream;

/**
 * LZMA compressor.
 *
 * @since 1.13
 */
public class LZMACompressorOutputStream extends CompressorOutputStream<LZMAOutputStream> {

    // @formatter:off
    /**
     * Builds a new {@link LZMACompressorOutputStream}.
     *
     * <p>
     * For example:
     * </p>
     * <pre>{@code
     * LZMACompressorOutputStream s = LZMACompressorOutputStream.builder()
     *   .setPath(path)
     *   .setLzma2Options(new LZMA2Options(...))
     *   .get();
     * }
     * </pre>
     *
     * @see #get()
     * @see LZMA2Options
     * @since 1.28.0
     */
    // @formatter:on
    public static class Builder extends AbstractStreamBuilder<LZMACompressorOutputStream, Builder> {

        private LZMA2Options lzma2Options = new LZMA2Options();

        /**
         * Constructs a new builder of {@link LZMACompressorOutputStream}.
         */
        public Builder() {
            // empty
        }

        @Override
        public LZMACompressorOutputStream get() throws IOException {
            return new LZMACompressorOutputStream(this);
        }

        /**
         * Sets LZMA options.
         * <p>
         * Passing {@code null} resets to the default value {@link LZMA2Options#LZMA2Options()}.
         * </p>
         *
         * @param lzma2Options LZMA options.
         * @return {@code this} instance.
         */
        public Builder setLzma2Options(final LZMA2Options lzma2Options) {
            this.lzma2Options = lzma2Options != null ? lzma2Options : new LZMA2Options();
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

    @SuppressWarnings("resource") // Caller closes
    private LZMACompressorOutputStream(final Builder builder) throws IOException {
        super(new LZMAOutputStream(builder.getOutputStream(), builder.lzma2Options, -1));
    }

    /**
     * Creates a LZMA compressor.
     *
     * @param outputStream the stream to wrap.
     * @throws IOException on error.
     */
    public LZMACompressorOutputStream(final OutputStream outputStream) throws IOException {
        this(builder().setOutputStream(outputStream));
    }

    /**
     * Finishes compression without closing the underlying stream. No more data can be written to this stream after finishing.
     *
     * @throws IOException on error.
     */
    @Override
    @SuppressWarnings("resource") // instance variable access
    public void finish() throws IOException {
        out().finish();
    }

    /**
     * Doesn't do anything as {@link LZMAOutputStream} doesn't support flushing.
     */
    @Override
    public void flush() throws IOException {
        // noop
    }

    /** {@inheritDoc} */
    @Override
    public void write(final byte[] buf, final int off, final int len) throws IOException {
        out.write(buf, off, len);
    }

}
