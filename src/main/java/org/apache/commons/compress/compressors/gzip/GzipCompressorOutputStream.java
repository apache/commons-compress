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
package org.apache.commons.compress.compressors.gzip;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.compressors.CompressorOutputStream;

/**
 * Compressed output stream using the gzip format. This implementation improves over the standard {@link GZIPOutputStream} class by allowing the configuration
 * of the compression level and the header metadata (file name, comment, modification time, operating system and extra flags).
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc1952">RFC 1952 GZIP File Format Specification</a>
 */
public class GzipCompressorOutputStream extends CompressorOutputStream<OutputStream> {

    /** Deflater used to compress the data */
    private final Deflater deflater;

    /** The buffer receiving the compressed data from the deflater */
    private final byte[] deflateBuffer;

    /** The checksum of the uncompressed data */
    private final CRC32 crc = new CRC32();

    /**
     * Creates a gzip compressed output stream with the default parameters.
     *
     * @param out the stream to compress to
     * @throws IOException if writing fails
     */
    public GzipCompressorOutputStream(final OutputStream out) throws IOException {
        this(out, new GzipParameters());
    }

    /**
     * Creates a gzip compressed output stream with the specified parameters.
     *
     * @param out        the stream to compress to
     * @param parameters the parameters to use
     * @throws IOException if writing fails
     * @since 1.7
     */
    public GzipCompressorOutputStream(final OutputStream out, final GzipParameters parameters) throws IOException {
        super(out);
        this.deflater = new Deflater(parameters.getCompressionLevel(), true);
        this.deflater.setStrategy(parameters.getDeflateStrategy());
        this.deflateBuffer = new byte[parameters.getBufferSize()];
        writeMemberHeader(parameters);
    }

    @Override
    public void close() throws IOException {
        if (!isClosed()) {
            try {
                finish();
            } finally {
                deflater.end();
                super.close();
            }
        }
    }

    private void deflate() throws IOException {
        final int length = deflater.deflate(deflateBuffer, 0, deflateBuffer.length);
        if (length > 0) {
            out.write(deflateBuffer, 0, length);
        }
    }

    /**
     * Finishes writing compressed data to the underlying stream without closing it.
     *
     * @since 1.7
     * @throws IOException on error
     */
    @Override
    public void finish() throws IOException {
        if (!deflater.finished()) {
            deflater.finish();
            while (!deflater.finished()) {
                deflate();
            }
            writeMemberTrailer();
            deflater.reset();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @since 1.1
     */
    @Override
    public void write(final byte[] buffer) throws IOException {
        write(buffer, 0, buffer.length);
    }

    /**
     * {@inheritDoc}
     *
     * @since 1.1
     */
    @Override
    public void write(final byte[] buffer, final int offset, final int length) throws IOException {
        checkOpen();
        if (deflater.finished()) {
            throw new IOException("Cannot write more data, the end of the compressed data stream has been reached.");
        }
        if (length > 0) {
            deflater.setInput(buffer, offset, length);
            while (!deflater.needsInput()) {
                deflate();
            }
            crc.update(buffer, offset, length);
        }
    }

    @Override
    public void write(final int b) throws IOException {
        write(new byte[] { (byte) (b & 0xff) }, 0, 1);
    }

    /**
     * Writes a C-style string, a NUL-terminated string, encoded with the {@code charset}.
     *
     * @param value The String to write.
     * @param charset Specifies the Charset to use.
     * @throws IOException if an I/O error occurs.
     */
    private void writeC(final String value, final Charset charset) throws IOException {
        if (value != null) {
            final byte[] ba = value.getBytes(charset);
            out.write(ba);
            out.write(0);
            crc.update(ba);
            crc.update(0);
        }
    }

    private void writeMemberHeader(final GzipParameters parameters) throws IOException {
        final String fileName = parameters.getFileName();
        final String comment = parameters.getComment();
        final byte[] extra = parameters.getExtraField() != null ? parameters.getExtraField().toByteArray() : null;
        final ByteBuffer buffer = ByteBuffer.allocate(10);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put((byte) GzipUtils.ID1);
        buffer.put((byte) GzipUtils.ID2);
        buffer.put((byte) Deflater.DEFLATED); // compression method (8: deflate)
        buffer.put((byte) ((extra != null ? GzipUtils.FEXTRA : 0)
                | (fileName != null ? GzipUtils.FNAME : 0)
                | (comment != null ? GzipUtils.FCOMMENT : 0)
                | (parameters.getHeaderCRC() ? GzipUtils.FHCRC : 0)
        )); // flags
        buffer.putInt((int) parameters.getModificationInstant().getEpochSecond());
        // extra flags
        final int compressionLevel = parameters.getCompressionLevel();
        if (compressionLevel == Deflater.BEST_COMPRESSION) {
            buffer.put(GzipUtils.XFL_MAX_COMPRESSION);
        } else if (compressionLevel == Deflater.BEST_SPEED) {
            buffer.put(GzipUtils.XFL_MAX_SPEED);
        } else {
            buffer.put(GzipUtils.XFL_UNKNOWN);
        }
        buffer.put((byte) parameters.getOperatingSystem());
        out.write(buffer.array());
        crc.update(buffer.array());
        if (extra != null) {
            out.write(extra.length & 0xff); // little endian
            out.write(extra.length >>> 8 & 0xff);
            out.write(extra);
            crc.update(extra.length & 0xff);
            crc.update(extra.length >>> 8 & 0xff);
            crc.update(extra);
        }
        writeC(fileName, parameters.getFileNameCharset());
        writeC(comment, parameters.getFileNameCharset());
        if (parameters.getHeaderCRC()) {
            final int v = (int) crc.getValue() & 0xffff;
            out.write(v & 0xff);
            out.write(v >>> 8 & 0xff);
        }
        crc.reset();
    }

    /**
     * Writes the member trailer.
     * <pre>
     *      0   1   2   3   4   5   6   7
     *   +---+---+---+---+---+---+---+---+
     *   |     CRC32     |     ISIZE     |
     *   +---+---+---+---+---+---+---+---+
     * </pre>
     *
     * @throws IOException if an I/O error occurs.
     */
    private void writeMemberTrailer() throws IOException {
        final ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt((int) crc.getValue());
        buffer.putInt(deflater.getTotalIn());
        out.write(buffer.array());
    }

}
