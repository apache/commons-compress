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
package org.apache.commons.compress.archivers.sevenz;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.deflate64.Deflate64CompressorInputStream;
import org.apache.commons.compress.utils.FlushShieldFilterOutputStream;
import org.tukaani.xz.ARMOptions;
import org.tukaani.xz.ARMThumbOptions;
import org.tukaani.xz.FilterOptions;
import org.tukaani.xz.FinishableWrapperOutputStream;
import org.tukaani.xz.IA64Options;
import org.tukaani.xz.PowerPCOptions;
import org.tukaani.xz.SPARCOptions;
import org.tukaani.xz.X86Options;

final class Coders {

    static final class BCJDecoder extends AbstractCoder {
        private final FilterOptions opts;

        BCJDecoder(final FilterOptions opts) {
            this.opts = opts;
        }

        @Override
        InputStream decode(final String archiveName, final InputStream in, final long uncompressedLength, final Coder coder, final byte[] password,
                final int maxMemoryLimitKiB) throws IOException {
            try {
                return opts.getInputStream(in);
            } catch (final AssertionError e) {
                throw new IOException("BCJ filter used in " + archiveName + " needs XZ for Java > 1.4 - see "
                        + "https://commons.apache.org/proper/commons-compress/limitations.html#7Z", e);
            }
        }

        @SuppressWarnings("resource")
        @Override
        OutputStream encode(final OutputStream out, final Object options) {
            return new FlushShieldFilterOutputStream(opts.getOutputStream(new FinishableWrapperOutputStream(out)));
        }
    }

    static final class BZIP2Decoder extends AbstractCoder {
        BZIP2Decoder() {
            super(Number.class);
        }

        @Override
        InputStream decode(final String archiveName, final InputStream in, final long uncompressedLength, final Coder coder, final byte[] password,
                final int maxMemoryLimitKiB) throws IOException {
            return new BZip2CompressorInputStream(in);
        }

        @Override
        OutputStream encode(final OutputStream out, final Object options) throws IOException {
            final int blockSize = toInt(options, BZip2CompressorOutputStream.MAX_BLOCKSIZE);
            return new BZip2CompressorOutputStream(out, blockSize);
        }
    }

    static final class CopyDecoder extends AbstractCoder {
        @Override
        InputStream decode(final String archiveName, final InputStream in, final long uncompressedLength, final Coder coder, final byte[] password,
                final int maxMemoryLimitKiB) throws IOException {
            return in;
        }

        @Override
        OutputStream encode(final OutputStream out, final Object options) {
            return out;
        }
    }

    static final class Deflate64Decoder extends AbstractCoder {
        Deflate64Decoder() {
            super(Number.class);
        }

        @Override
        InputStream decode(final String archiveName, final InputStream in, final long uncompressedLength, final Coder coder, final byte[] password,
                final int maxMemoryLimitKiB) throws IOException {
            return new Deflate64CompressorInputStream(in);
        }
    }

    static final class DeflateDecoder extends AbstractCoder {

        static final class DeflateDecoderInputStream extends FilterInputStream {

            Inflater inflater;

            DeflateDecoderInputStream(final InflaterInputStream inflaterInputStream, final Inflater inflater) {
                super(inflaterInputStream);
                this.inflater = inflater;
            }

            @Override
            public void close() throws IOException {
                try {
                    super.close();
                } finally {
                    inflater.end();
                }
            }

        }

        static final class DeflateDecoderOutputStream extends OutputStream {

            final DeflaterOutputStream deflaterOutputStream;
            Deflater deflater;

            DeflateDecoderOutputStream(final DeflaterOutputStream deflaterOutputStream, final Deflater deflater) {
                this.deflaterOutputStream = deflaterOutputStream;
                this.deflater = deflater;
            }

            @Override
            public void close() throws IOException {
                try {
                    deflaterOutputStream.close();
                } finally {
                    deflater.end();
                }
            }

            @Override
            public void write(final byte[] b) throws IOException {
                deflaterOutputStream.write(b);
            }

            @Override
            public void write(final byte[] b, final int off, final int len) throws IOException {
                deflaterOutputStream.write(b, off, len);
            }

            @Override
            public void write(final int b) throws IOException {
                deflaterOutputStream.write(b);
            }
        }

        private static final byte[] ONE_ZERO_BYTE = new byte[1];

        DeflateDecoder() {
            super(Number.class);
        }

        @Override
        InputStream decode(final String archiveName, final InputStream in, final long uncompressedLength, final Coder coder, final byte[] password,
                final int maxMemoryLimitKiB) throws IOException {
            final Inflater inflater = new Inflater(true);
            // Inflater with nowrap=true has this odd contract for a zero padding
            // byte following the data stream; this used to be zlib's requirement
            // and has been fixed a long time ago, but the contract persists so
            // we comply.
            // https://docs.oracle.com/javase/8/docs/api/java/util/zip/Inflater.html#Inflater(boolean)
            final InflaterInputStream inflaterInputStream = new InflaterInputStream(new SequenceInputStream(in, new ByteArrayInputStream(ONE_ZERO_BYTE)),
                    inflater);
            return new DeflateDecoderInputStream(inflaterInputStream, inflater);
        }

        @Override
        OutputStream encode(final OutputStream out, final Object options) {
            final int level = toInt(options, 9);
            final Deflater deflater = new Deflater(level, true);
            final DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(out, deflater);
            return new DeflateDecoderOutputStream(deflaterOutputStream, deflater);
        }
    }

    private static final Map<SevenZMethod, AbstractCoder> CODER_MAP = new HashMap<SevenZMethod, AbstractCoder>() {

        private static final long serialVersionUID = 1664829131806520867L;

        {
            put(SevenZMethod.COPY, new CopyDecoder());
            put(SevenZMethod.LZMA, new LZMADecoder());
            put(SevenZMethod.LZMA2, new LZMA2Decoder());
            put(SevenZMethod.DEFLATE, new DeflateDecoder());
            put(SevenZMethod.DEFLATE64, new Deflate64Decoder());
            put(SevenZMethod.BZIP2, new BZIP2Decoder());
            put(SevenZMethod.AES256SHA256, new AES256SHA256Decoder());
            put(SevenZMethod.BCJ_X86_FILTER, new BCJDecoder(new X86Options()));
            put(SevenZMethod.BCJ_PPC_FILTER, new BCJDecoder(new PowerPCOptions()));
            put(SevenZMethod.BCJ_IA64_FILTER, new BCJDecoder(new IA64Options()));
            put(SevenZMethod.BCJ_ARM_FILTER, new BCJDecoder(new ARMOptions()));
            put(SevenZMethod.BCJ_ARM_THUMB_FILTER, new BCJDecoder(new ARMThumbOptions()));
            put(SevenZMethod.BCJ_SPARC_FILTER, new BCJDecoder(new SPARCOptions()));
            put(SevenZMethod.DELTA_FILTER, new DeltaDecoder());
        }
    };

    static InputStream addDecoder(final String archiveName, final InputStream is, final long uncompressedLength, final Coder coder, final byte[] password,
            final int maxMemoryLimitKiB) throws IOException {
        final AbstractCoder cb = findByMethod(SevenZMethod.byId(coder.decompressionMethodId));
        if (cb == null) {
            throw new IOException("Unsupported compression method " + Arrays.toString(coder.decompressionMethodId) + " used in " + archiveName);
        }
        return cb.decode(archiveName, is, uncompressedLength, coder, password, maxMemoryLimitKiB);
    }

    static OutputStream addEncoder(final OutputStream out, final SevenZMethod method, final Object options) throws IOException {
        final AbstractCoder cb = findByMethod(method);
        if (cb == null) {
            throw new IOException("Unsupported compression method " + method);
        }
        return cb.encode(out, options);
    }

    static AbstractCoder findByMethod(final SevenZMethod method) {
        return CODER_MAP.get(method);
    }

}
