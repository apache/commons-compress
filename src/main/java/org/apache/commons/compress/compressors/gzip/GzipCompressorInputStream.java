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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.utils.ByteUtils;
import org.apache.commons.compress.utils.InputStreamStatistics;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.build.AbstractOrigin;
import org.apache.commons.io.build.AbstractStreamBuilder;
import org.apache.commons.io.function.IOConsumer;
import org.apache.commons.io.input.BoundedInputStream;

/**
 * Input stream that decompresses GZIP (.gz) files.
 *
 * <p>
 * This supports decompressing concatenated GZIP files which is important when decompressing standalone GZIP files.
 * </p>
 * <p>
 * Instead of using {@code java.util.zip.GZIPInputStream}, this class has its own GZIP member decoder. Internally, decompression is done using
 * {@link java.util.zip.Inflater}.
 * </p>
 * <p>
 * If you use the constructor {@code GzipCompressorInputStream(in)}, {@code Builder.setDecompressConcatenated(false)}, or
 * {@code GzipCompressorInputStream(in, false)}, then {@link #read} will return -1 as soon as the first encoded GZIP member has been completely read. In this
 * case, if the underlying input stream supports {@link InputStream#mark mark()} and {@link InputStream#reset reset()}, then it will be left positioned just
 * after the end of the encoded GZIP member; otherwise, some indeterminate number of extra bytes following the encoded GZIP member will have been consumed and
 * discarded.
 * </p>
 * <p>
 * If you use the {@code Builder.setDecompressConcatenated(true)} or {@code GzipCompressorInputStream(in, true)} then {@link #read} will return -1 only after
 * the entire input stream has been exhausted; any bytes that follow an encoded GZIP member must constitute a new encoded GZIP member, otherwise an
 * {@link IOException} is thrown. The data read from a stream constructed this way will consist of the concatenated data of all of the encoded GZIP members in
 * order.
 * </p>
 * <p>
 * To build an instance, use {@link Builder}.
 * </p>
 *
 * @see Builder
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc1952">RFC 1952 GZIP File Format Specification</a>
 */
public class GzipCompressorInputStream extends CompressorInputStream implements InputStreamStatistics {

    // @formatter:off
    /**
     * Builds a new {@link GzipCompressorInputStream}.
     *
     * <p>
     * For example:
     * </p>
     * <pre>{@code
     * GzipCompressorInputStream s = GzipCompressorInputStream.builder()
     *   .setPath(path)
     *   .setFileNameCharset(StandardCharsets.ISO_8859_1)
     *   .get();}
     * </pre>
     *
     * @see #get()
     * @since 1.28.0
     */
    // @formatter:on
    public static class Builder extends AbstractStreamBuilder<GzipCompressorInputStream, Builder> {

        /** True if decompressing multi-member streams. */
        private boolean decompressConcatenated;

        private boolean ignoreExtraField = true;

        private Charset fileNameCharset = GzipUtils.GZIP_ENCODING;

        private IOConsumer<GzipCompressorInputStream> onMemberStart;

        private IOConsumer<GzipCompressorInputStream> onMemberEnd;

        /**
         * Constructs a new builder of {@link GzipCompressorInputStream}.
         */
        public Builder() {
            // empty
        }

        /**
         * Builds a new {@link GzipCompressorInputStream}.
         * <p>
         * You must set input that supports {@link InputStream}, otherwise, this method throws an exception.
         * </p>
         *
         * @return a new instance.
         * @throws IllegalStateException         if the {@code origin} is {@code null}.
         * @throws UnsupportedOperationException if the origin cannot be converted to an {@link InputStream}.
         * @see AbstractOrigin#getInputStream(java.nio.file.OpenOption...)
         */
        @Override
        public GzipCompressorInputStream get() throws IOException {
            return new GzipCompressorInputStream(this);
        }

        /**
         * Sets whether we should allow decompressing multiple members.
         *
         * @param decompressConcatenated whether we should allow decompressing multiple members.
         * @return {@code this} instance.
         */
        public Builder setDecompressConcatenated(final boolean decompressConcatenated) {
            this.decompressConcatenated = decompressConcatenated;
            return this;
        }

        /**
         * Sets the Charset to use for writing file names and comments, where null maps to {@link GzipUtils#GZIP_ENCODING}.
         * <p>
         * <em>Setting a value other than {@link GzipUtils#GZIP_ENCODING} is not compliant with the <a href="https://datatracker.ietf.org/doc/html/rfc1952">RFC
         * 1952 GZIP File Format Specification</a></em>. Use at your own risk of interoperability issues.
         * </p>
         * <p>
         * The default value is {@link GzipUtils#GZIP_ENCODING}.
         * </p>
         *
         * @param fileNameCharset the Charset to use for writing file names and comments, null maps to {@link GzipUtils#GZIP_ENCODING}.
         * @return {@code this} instance.
         */
        public Builder setFileNameCharset(final Charset fileNameCharset) {
            this.fileNameCharset = fileNameCharset;
            return this;
        }

        /**
         * Sets whether to ignore extra fields. To best comply with gzip, this defaults to true.
         *
         * @param ignoreExtraFields whether to ignore extra fields.
         * @return {@code this} instance.
         * @since 1.29.0
         */
        public Builder setIgnoreExtraField(final boolean ignoreExtraFields) {
            this.ignoreExtraField = ignoreExtraFields;
            return this;
        }

        /**
         * Sets the consumer called when a member <em>trailer</em> is parsed.
         * <p>
         * When a member <em>header</em> is parsed, all {@link GzipParameters} values are initialized except {@code trailerCrc} and {@code trailerISize}.
         * </p>
         * <p>
         * When a member <em>trailer</em> is parsed, the {@link GzipParameters} values {@code trailerCrc} and {@code trailerISize} are set.
         * </p>
         *
         * @param onMemberEnd The consumer.
         * @return {@code this} instance.
         * @see GzipCompressorInputStream#getMetaData()
         */
        public Builder setOnMemberEnd(final IOConsumer<GzipCompressorInputStream> onMemberEnd) {
            this.onMemberEnd = onMemberEnd;
            return this;
        }

        /**
         * Sets the consumer called when a member <em>header</em> is parsed.
         * <p>
         * When a member <em>header</em> is parsed, all {@link GzipParameters} values are initialized except {@code trailerCrc} and {@code trailerISize}.
         * </p>
         * <p>
         * When a member <em>trailer</em> is parsed, the {@link GzipParameters} values {@code trailerCrc} and {@code trailerISize} are set.
         * </p>
         *
         * @param onMemberStart The consumer.
         * @return {@code this} instance.
         * @see GzipCompressorInputStream#getMetaData()
         */
        public Builder setOnMemberStart(final IOConsumer<GzipCompressorInputStream> onMemberStart) {
            this.onMemberStart = onMemberStart;
            return this;
        }
    }

    private static final IOConsumer<GzipCompressorInputStream> NOOP = IOConsumer.noop();

    /**
     * Constructs a new builder of {@link GzipCompressorInputStream}.
     *
     * @return a new builder of {@link GzipCompressorInputStream}.
     * @since 1.28.0
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Checks if the signature matches what is expected for a .gz file.
     *
     * @param signature the bytes to check
     * @param length    the number of bytes to check
     * @return true if this is a .gz stream, false otherwise
     * @since 1.1
     */
    public static boolean matches(final byte[] signature, final int length) {
        return length >= 2 && signature[0] == 31 && signature[1] == -117;
    }

    private static byte[] readToNull(final DataInput inData) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            int b;
            while ((b = inData.readUnsignedByte()) != 0) { // NOSONAR
                bos.write(b);
            }
            return bos.toByteArray();
        }
    }

    /** Buffer to hold the input data. */
    private final byte[] buf = new byte[8192];

    /** Amount of data in buf. */
    private int bufUsed;

    private final BoundedInputStream countingStream;

    /** CRC32 from uncompressed data. */
    private final CRC32 crc = new CRC32();

    /** True if decompressing multi-member streams. */
    private final boolean decompressConcatenated;

    /** True once everything has been decompressed. */
    private boolean endReached;

    private final Charset fileNameCharset;

    /**
     * Compressed input stream, possibly wrapped in a BufferedInputStream, always wrapped in countingStream above
     */
    private final InputStream in;

    /** Decompressor. */
    private Inflater inflater = new Inflater(true);

    /** Buffer for no-argument read method. */
    private final byte[] oneByte = new byte[1];

    private GzipParameters parameters;

    private final IOConsumer<GzipCompressorInputStream> onMemberStart;

    private final IOConsumer<GzipCompressorInputStream> onMemberEnd;

    private final boolean ignoreExtraField;

    @SuppressWarnings("resource") // caller closes
    private GzipCompressorInputStream(final Builder builder) throws IOException {
        countingStream = BoundedInputStream.builder().setInputStream(builder.getInputStream()).get();
        // Mark support is strictly needed for concatenated files only,
        // but it's simpler if it is always available.
        in = countingStream.markSupported() ? countingStream : new BufferedInputStream(countingStream);
        this.decompressConcatenated = builder.decompressConcatenated;
        this.fileNameCharset = builder.fileNameCharset;
        this.onMemberStart = builder.onMemberStart != null ? builder.onMemberStart : NOOP;
        this.onMemberEnd = builder.onMemberEnd != null ? builder.onMemberEnd : NOOP;
        this.ignoreExtraField = builder.ignoreExtraField;
        init(true);
    }

    /**
     * Constructs a new input stream that decompresses gzip-compressed data from the specified input stream.
     * <p>
     * This is equivalent to {@code GzipCompressorInputStream(inputStream, false)} and thus will not decompress concatenated .gz files.
     * </p>
     *
     * @param inputStream the InputStream from which this object should be created of
     * @throws IOException if the stream could not be created
     */
    public GzipCompressorInputStream(final InputStream inputStream) throws IOException {
        this(builder().setInputStream(inputStream));
    }

    /**
     * Constructs a new input stream that decompresses gzip-compressed data from the specified input stream.
     * <p>
     * If {@code decompressConcatenated} is {@code false}: This decompressor might read more input than it will actually use. If {@code inputStream} supports
     * {@code mark} and {@code reset}, then the input position will be adjusted so that it is right after the last byte of the compressed stream. If
     * {@code mark} isn't supported, the input position will be undefined.
     * </p>
     *
     * @param inputStream            the InputStream from which this object should be created of
     * @param decompressConcatenated if true, decompress until the end of the input; if false, stop after the first .gz member
     * @throws IOException if the stream could not be created
     * @deprecated Use {@link Builder#get()}.
     */
    @Deprecated
    public GzipCompressorInputStream(final InputStream inputStream, final boolean decompressConcatenated) throws IOException {
        this(builder().setInputStream(inputStream).setDecompressConcatenated(decompressConcatenated));
    }

    /**
     * Closes the input stream (unless it is System.in).
     *
     * @since 1.2
     */
    @Override
    public void close() throws IOException {
        if (inflater != null) {
            inflater.end();
            inflater = null;
        }
        if (this.in != System.in) {
            this.in.close();
        }
    }

    /**
     * {@inheritDoc}.
     *
     * @since 1.17
     */
    @Override
    public long getCompressedCount() {
        return countingStream.getCount();
    }

    /**
     * Provides the stream's meta data - may change with each stream when decompressing concatenated streams.
     *
     * @return the stream's meta data
     * @since 1.8
     */
    public GzipParameters getMetaData() {
        return parameters;
    }

    private boolean init(final boolean isFirstMember) throws IOException {
        if (!isFirstMember && !decompressConcatenated) { // at least one must be true
            throw new IllegalStateException("Unexpected: isFirstMember and decompressConcatenated are both false.");
        }
        // Check the magic bytes without a possibility of EOFException.
        final int magic0 = in.read();
        // If end of input was reached after decompressing at least
        // one .gz member, we have reached the end of the file successfully.
        if (magic0 == -1 && !isFirstMember) {
            return false;
        }
        if (magic0 != GzipUtils.ID1 || in.read() != GzipUtils.ID2) {
            throw new CompressorException(isFirstMember ? "Input is not in the .gz format." : "Unexpected data after a valid .gz stream.");
        }
        parameters = new GzipParameters();
        parameters.setFileNameCharset(fileNameCharset);
        // Parsing the rest of the header may throw EOFException.
        final DataInput inData = new DataInputStream(in);
        final int method = inData.readUnsignedByte();
        if (method != Deflater.DEFLATED) {
            throw new CompressorException("Unsupported compression method %d in the .gz header", method);
        }
        final int flg = inData.readUnsignedByte();
        if ((flg & FLG.FRESERVED) != 0) {
            throw new CompressorException("Reserved flags are set in the .gz header.");
        }
        parameters.setModificationTime(ByteUtils.fromLittleEndian(inData, 4));
        switch (inData.readUnsignedByte()) { // extra flags
        case XFL.MAX_COMPRESSION:
            parameters.setCompressionLevel(Deflater.BEST_COMPRESSION);
            break;
        case XFL.MAX_SPEED:
            parameters.setCompressionLevel(Deflater.BEST_SPEED);
            break;
        default:
            parameters.setCompressionLevel(Deflater.DEFAULT_COMPRESSION);
            break;
        }
        parameters.setOperatingSystem(inData.readUnsignedByte());
        // Extra field
        if ((flg & FLG.FEXTRA) != 0) {
            int xlen = inData.readUnsignedByte();
            xlen |= inData.readUnsignedByte() << 8;
            parameters.setExtraFieldXlen(xlen);
            final byte[] extra = new byte[xlen];
            inData.readFully(extra);
            if (!ignoreExtraField) {
                // Read the data but ignore it.
                parameters.setExtraField(ExtraField.fromBytes(extra));
            }
        }
        // Original file name
        if ((flg & FLG.FNAME) != 0) {
            parameters.setFileName(new String(readToNull(inData), parameters.getFileNameCharset()));
        }
        // Comment
        if ((flg & FLG.FCOMMENT) != 0) {
            parameters.setComment(new String(readToNull(inData), parameters.getFileNameCharset()));
        }
        // Header "CRC16" which is actually a truncated CRC32 (which isn't
        // as good as real CRC16). I don't know if any encoder implementation
        // sets this, so it's not worth trying to verify it. GNU gzip 1.4
        // doesn't support this field, but zlib seems to be able to at least
        // skip over it.
        if ((flg & FLG.FHCRC) != 0) {
            parameters.setHeaderCRC(true);
            inData.readShort();
        }
        // Reset
        inflater.reset();
        crc.reset();
        onMemberStart.accept(this);
        return true;
    }

    @Override
    public int read() throws IOException {
        return read(oneByte, 0, 1) == -1 ? -1 : oneByte[0] & 0xFF;
    }

    /**
     * {@inheritDoc}
     *
     * @since 1.1
     */
    @Override
    public int read(final byte[] b, int off, int len) throws IOException {
        if (len == 0) {
            return 0;
        }
        if (endReached) {
            return -1;
        }
        int size = 0;
        while (len > 0) {
            if (inflater.needsInput()) {
                // Remember the current position because we may need to
                // rewind after reading too much input.
                in.mark(buf.length);
                bufUsed = in.read(buf);
                if (bufUsed == -1) {
                    throw new EOFException();
                }
                inflater.setInput(buf, 0, bufUsed);
            }
            final int ret;
            try {
                ret = inflater.inflate(b, off, len);
            } catch (final DataFormatException e) { // NOSONAR
                throw new CompressorException("Gzip-compressed data is corrupt.", e);
            }
            crc.update(b, off, ret);
            off += ret;
            len -= ret;
            size += ret;
            count(ret);
            if (inflater.finished()) {
                // We may have read too many bytes. Rewind the read
                // position to match the actual amount used.
                in.reset();
                final int skipAmount = bufUsed - inflater.getRemaining();
                if (IOUtils.skip(in, skipAmount) != skipAmount) {
                    throw new CompressorException("skip");
                }
                bufUsed = 0;
                final DataInput inData = new DataInputStream(in);
                // CRC32
                final long trailerCrc = ByteUtils.fromLittleEndian(inData, 4);
                if (trailerCrc != crc.getValue()) {
                    throw new CompressorException("Gzip-compressed data is corrupt (CRC32 error).");
                }
                // Uncompressed size modulo 2^32, ISIZE in the RFC.
                final long iSize = ByteUtils.fromLittleEndian(inData, 4);
                if (iSize != (inflater.getBytesWritten() & 0xffffffffL)) {
                    throw new CompressorException("Gzip-compressed data is corrupt (uncompressed size mismatch).");
                }
                parameters.setTrailerCrc(trailerCrc);
                parameters.setTrailerISize(iSize);
                onMemberEnd.accept(this);
                // See if this is the end of the file.
                if (!decompressConcatenated || !init(false)) {
                    inflater.end();
                    inflater = null;
                    endReached = true;
                    return size == 0 ? -1 : size;
                }
            }
        }
        return size;
    }
}
