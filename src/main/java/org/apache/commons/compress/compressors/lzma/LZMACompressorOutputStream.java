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
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.LZMAOutputStream;

/**
 * LZMA compressor.
 *
 * @since 1.13
 */
public class LZMACompressorOutputStream extends CompressorOutputStream<LZMAOutputStream> {

    /**
     * Creates a LZMA compressor.
     *
     * @param outputStream the stream to wrap
     * @throws IOException on error
     */
    @SuppressWarnings("resource") // Caller closes
    public LZMACompressorOutputStream(final OutputStream outputStream) throws IOException {
        super(new LZMAOutputStream(outputStream, new LZMA2Options(), -1));
    }

    /**
     * Creates a LZMA compressor using the specified LZMA2 preset level.
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
     */
    @SuppressWarnings("resource") // Caller closes
    public LZMACompressorOutputStream(final OutputStream outputStream, final int preset) throws IOException {
        super(new LZMAOutputStream(outputStream, new LZMA2Options(preset), -1));
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
