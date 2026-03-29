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
package org.apache.commons.compress.archivers.zip;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorOutputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdConstants;
import org.apache.commons.compress.compressors.zstandard.ZstdUtils;

/**
 * Built-in {@link ZipCompressionPayloadWriterFactory} implementations.
 *
 * @since 1.29.0
 */
public final class ZipCompressionPayloadWriters {

    private ZipCompressionPayloadWriters() {
    }

    /**
     * Returns a factory that compresses plain entry data with Zstandard for use with {@link ZipMethod#ZSTD} (or the deprecated method id).
     * <p>
     * Requires {@link ZstdUtils#isZstdCompressionAvailable()} at runtime.
     * </p>
     *
     * @param compressionLevel Zstd level; {@link ZstdConstants#ZSTD_CLEVEL_DEFAULT} for the library default.
     * @return a factory writing Zstd frames with {@code closeFrameOnFlush} enabled so {@link ZipCompressionPayloadWriter#finish()} can end the frame.
     * @throws UnsupportedOperationException if Zstd is not available.
     */
    public static ZipCompressionPayloadWriterFactory zstd(final int compressionLevel) {
        if (!ZstdUtils.isZstdCompressionAvailable()) {
            throw new UnsupportedOperationException("Zstandard compression is not available.");
        }
        return (compressedPayloadSink, entry) -> {
            final ZstdCompressorOutputStream zOut = ZstdCompressorOutputStream.builder()
                    .setOutputStream(nonClosingSink(compressedPayloadSink))
                    .setLevel(compressionLevel)
                    .setCloseFrameOnFlush(true)
                    .get();
            return new ZipCompressionPayloadWriter() {
                @Override
                public void write(final byte[] b, final int off, final int len) throws IOException {
                    if (len > 0) {
                        zOut.write(b, off, len);
                    }
                }

                @Override
                public void finish() throws IOException {
                    zOut.flush();
                    zOut.close();
                }
            };
        };
    }

    /**
     * Returns a factory that compresses plain entry data with BZip2 for use with {@link ZipMethod#BZIP2}.
     * <p>
     * Uses the default BZip2 block size ({@link BZip2CompressorOutputStream#MAX_BLOCKSIZE} × 100k).
     * </p>
     *
     * @return a factory; {@link ZipCompressionPayloadWriter#finish()} ends the BZip2 stream without closing the ZIP payload sink.
     */
    public static ZipCompressionPayloadWriterFactory bzip2() {
        return bzip2(BZip2CompressorOutputStream.MAX_BLOCKSIZE);
    }

    /**
     * Returns a factory that compresses plain entry data with BZip2 for use with {@link ZipMethod#BZIP2}.
     *
     * @param blockSize the block size as 100k units, {@link BZip2CompressorOutputStream#MIN_BLOCKSIZE}–{@link BZip2CompressorOutputStream#MAX_BLOCKSIZE}.
     * @return a factory; {@link ZipCompressionPayloadWriter#finish()} ends the BZip2 stream without closing the ZIP payload sink.
     * @throws IllegalArgumentException if {@code blockSize} is out of range for {@link BZip2CompressorOutputStream}.
     */
    public static ZipCompressionPayloadWriterFactory bzip2(final int blockSize) {
        return (compressedPayloadSink, entry) -> {
            final BZip2CompressorOutputStream bzip2 = new BZip2CompressorOutputStream(nonClosingSink(compressedPayloadSink), blockSize);
            return new ZipCompressionPayloadWriter() {
                @Override
                public void write(final byte[] b, final int off, final int len) throws IOException {
                    if (len > 0) {
                        bzip2.write(b, off, len);
                    }
                }

                @Override
                public void finish() throws IOException {
                    bzip2.close();
                }
            };
        };
    }

    private static OutputStream nonClosingSink(final OutputStream delegate) {
        return new OutputStream() {
            @Override
            public void write(final int b) throws IOException {
                delegate.write(b);
            }

            @Override
            public void write(final byte[] b, final int off, final int len) throws IOException {
                delegate.write(b, off, len);
            }

            @Override
            public void flush() throws IOException {
                delegate.flush();
            }

            @Override
            public void close() {
                // End-of-payload is handled by the payload writer's finish(); never close the ZIP payload sink.
            }
        };
    }
}
