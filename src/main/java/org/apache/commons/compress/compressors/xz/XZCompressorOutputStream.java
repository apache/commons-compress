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
import java.io.OutputStream;

import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.io.build.AbstractStreamBuilder;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZOutputStream;

// @formatter:off
/**
 * Compresses an output stream using the XZ and LZMA2 compression options.
 * <p>
 * For example:
 * </p>
 * <pre>{@code
 * XZCompressorOutputStream s = XZCompressorOutputStream.builder()
 *   .setPath(path)
 *   .setLzma2Options(new LZMA2Options(...))
 *   .get();
 * }
 * </pre>
 *
 * <h2>Calling flush</h2>
 * <p>
 * Calling {@link #flush()} flushes the encoder and calls {@code outputStream.flush()}. All buffered pending data will then be decompressible from the output
 * stream. Calling this function very often may increase the compressed file size a lot.
 * </p>
 *
 * @since 1.4
 */
// @formatter:on
public class XZCompressorOutputStream extends CompressorOutputStream<XZOutputStream> {

    // @formatter:off
    /**
     * Builds a new {@link XZCompressorOutputStream}.
     *
     * <p>
     * For example:
     * </p>
     * <pre>{@code
     * XZCompressorOutputStream s = XZCompressorOutputStream.builder()
     *   .setPath(path)
     *   .setLzma2Options(new LZMA2Options(...))
     *   .get();
     * }
     * </pre>
     *
     * @see #get()
     * @since 1.28.0
     */
    // @formatter:on
    public static class Builder extends AbstractStreamBuilder<XZCompressorOutputStream, Builder> {

        private LZMA2Options lzma2Options = new LZMA2Options();

        /**
         * Constructs a new builder of {@link XZCompressorOutputStream}.
         */
        public Builder() {
            // empty
        }

        @Override
        public XZCompressorOutputStream get() throws IOException {
            return new XZCompressorOutputStream(this);
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
     * Constructs a new builder of {@link XZCompressorOutputStream}.
     *
     * @return a new builder of {@link XZCompressorOutputStream}.
     * @since 1.28.0
     */
    public static Builder builder() {
        return new Builder();
    }

    @SuppressWarnings("resource") // Caller closes
    private XZCompressorOutputStream(final Builder builder) throws IOException {
        super(new XZOutputStream(builder.getOutputStream(), builder.lzma2Options));
        }

    /**
     * Creates a new XZ compressor using the default LZMA2 options. This is equivalent to {@code XZCompressorOutputStream(outputStream, 6)}.
     *
     * @param outputStream the stream to wrap
     * @throws IOException on error
     */
    public XZCompressorOutputStream(final OutputStream outputStream) throws IOException {
        this(builder().setOutputStream(outputStream));
    }

    /**
     * Creates a new XZ compressor using the specified LZMA2 preset level.
     * <p>
     * The presets 0-3 are fast presets with medium compression. The presets 4-6 are fairly slow presets with high compression. The default preset is 6.
     * </p>
     * <p>
     * The presets 7-9 are like the preset 6 but use bigger dictionaries and have higher compressor and decompressor memory requirements. Unless the
     * uncompressed size of the file exceeds 8&nbsp;MiB, 16&nbsp;MiB, or 32&nbsp;MiB, it is waste of memory to use the presets 7, 8, or 9, respectively.
     * </p>
     *
     * @param outputStream the stream to wrap
     * @param preset       the preset
     * @throws IOException on error
     * @deprecated Use {@link #builder()}.
     */
    @Deprecated
    @SuppressWarnings("resource") // Caller closes
    public XZCompressorOutputStream(final OutputStream outputStream, final int preset) throws IOException {
        super(new XZOutputStream(outputStream, new LZMA2Options(preset)));
    }

    /**
     * Finishes compression without closing the underlying stream. No more data can be written to this stream after finishing.
     *
     * @throws IOException on error
     */
    @Override
    @SuppressWarnings("resource") // instance variable access
    public void finish() throws IOException {
        out().finish();
    }

    @Override
    public void write(final byte[] buf, final int off, final int len) throws IOException {
        out.write(buf, off, len);
    }

}
