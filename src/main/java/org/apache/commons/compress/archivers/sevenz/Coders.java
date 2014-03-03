/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.commons.compress.archivers.sevenz;

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.tukaani.xz.ARMOptions;
import org.tukaani.xz.ARMThumbOptions;
import org.tukaani.xz.FilterOptions;
import org.tukaani.xz.FinishableOutputStream;
import org.tukaani.xz.FinishableWrapperOutputStream;
import org.tukaani.xz.IA64Options;
import org.tukaani.xz.LZMAInputStream;
import org.tukaani.xz.PowerPCOptions;
import org.tukaani.xz.SPARCOptions;
import org.tukaani.xz.X86Options;

class Coders {
    private static final Map<SevenZMethod, CoderBase> CODER_MAP = new HashMap<SevenZMethod, CoderBase>() {

        private static final long serialVersionUID = 1664829131806520867L;
    {
            put(SevenZMethod.COPY, new CopyDecoder());
            put(SevenZMethod.LZMA, new LZMADecoder());
            put(SevenZMethod.LZMA2, new LZMA2Decoder());
            put(SevenZMethod.DEFLATE, new DeflateDecoder());
            put(SevenZMethod.BZIP2, new BZIP2Decoder());
            put(SevenZMethod.AES256SHA256, new AES256SHA256Decoder());
            put(SevenZMethod.BCJ_X86_FILTER, new BCJDecoder(new X86Options()));
            put(SevenZMethod.BCJ_PPC_FILTER, new BCJDecoder(new PowerPCOptions()));
            put(SevenZMethod.BCJ_IA64_FILTER, new BCJDecoder(new IA64Options()));
            put(SevenZMethod.BCJ_ARM_FILTER, new BCJDecoder(new ARMOptions()));
            put(SevenZMethod.BCJ_ARM_THUMB_FILTER, new BCJDecoder(new ARMThumbOptions()));
            put(SevenZMethod.BCJ_SPARC_FILTER, new BCJDecoder(new SPARCOptions()));
            put(SevenZMethod.DELTA_FILTER, new DeltaDecoder());
        }};

    static CoderBase findByMethod(SevenZMethod method) {
        return CODER_MAP.get(method);
    }

    static InputStream addDecoder(final InputStream is,
            final Coder coder, final byte[] password) throws IOException {
        CoderBase cb = findByMethod(SevenZMethod.byId(coder.decompressionMethodId));
        if (cb == null) {
            throw new IOException("Unsupported compression method " +
                                  Arrays.toString(coder.decompressionMethodId));
        }
        return cb.decode(is, coder, password);
    }
    
    static OutputStream addEncoder(final OutputStream out, final SevenZMethod method,
                                   Object options) throws IOException {
        CoderBase cb = findByMethod(method);
        if (cb == null) {
            throw new IOException("Unsupported compression method " + method);
        }
        return cb.encode(out, options);
    }

    static class CopyDecoder extends CoderBase {
        @Override
        InputStream decode(final InputStream in, final Coder coder,
                byte[] password) throws IOException {
            return in; 
        }
        @Override
        OutputStream encode(final OutputStream out, final Object _) {
            return out;
        }
    }

    static class LZMADecoder extends CoderBase {
        @Override
        InputStream decode(final InputStream in, final Coder coder,
                byte[] password) throws IOException {
            byte propsByte = coder.properties[0];
            long dictSize = coder.properties[1];
            for (int i = 1; i < 4; i++) {
                dictSize |= (coder.properties[i + 1] & 0xffl) << (8 * i);
            }
            if (dictSize > LZMAInputStream.DICT_SIZE_MAX) {
                throw new IOException("Dictionary larger than 4GiB maximum size");
            }
            return new LZMAInputStream(in, -1, propsByte, (int) dictSize);
        }
    }
    
    static class BCJDecoder extends CoderBase {
        private final FilterOptions opts;
        BCJDecoder(FilterOptions opts) {
            this.opts = opts;
        }

        @Override
        InputStream decode(final InputStream in, final Coder coder,
                byte[] password) throws IOException {
            try {
                return opts.getInputStream(in);
            } catch (AssertionError e) {
                IOException ex = new IOException("BCJ filter needs XZ for Java > 1.4 - see "
                                                 + "http://commons.apache.org/proper/commons-compress/limitations.html#7Z");
                ex.initCause(e);
                throw ex;
            }
        }
        @Override
        OutputStream encode(final OutputStream out, final Object _) {
            final FinishableOutputStream fo = opts.getOutputStream(new FinishableWrapperOutputStream(out));
            return new FilterOutputStream(fo) {
                @Override
                public void flush() {
                }
            };
        }
    }
    
    static class DeflateDecoder extends CoderBase {
        DeflateDecoder() {
            super(Number.class);
        }

        @Override
        InputStream decode(final InputStream in, final Coder coder, final byte[] password)
            throws IOException {
            return new InflaterInputStream(new DummyByteAddingInputStream(in),
                                           new Inflater(true));
        }
        @Override
        OutputStream encode(final OutputStream out, final Object options) {
            int level = numberOptionOrDefault(options, 9);
            return new DeflaterOutputStream(out, new Deflater(level, true));
        }
    }

    static class BZIP2Decoder extends CoderBase {
        BZIP2Decoder() {
            super(Number.class);
        }

        @Override
        InputStream decode(final InputStream in, final Coder coder, final byte[] password)
                throws IOException {
            return new BZip2CompressorInputStream(in);
        }
        @Override
        OutputStream encode(final OutputStream out, final Object options)
                throws IOException {
            int blockSize = numberOptionOrDefault(options, BZip2CompressorOutputStream.MAX_BLOCKSIZE);
            return new BZip2CompressorOutputStream(out, blockSize);
        }
    }

    /**
     * ZLIB requires an extra dummy byte.
     *
     * @see java.util.zip.Inflater#Inflater(boolean)
     * @see org.apache.commons.compress.archivers.zip.ZipFile.BoundedInputStream
     */
    private static class DummyByteAddingInputStream extends FilterInputStream {
        private boolean addDummyByte = true;

        private DummyByteAddingInputStream(InputStream in) {
            super(in);
        }

        @Override
        public int read() throws IOException {
            int result = super.read();
            if (result == -1 && addDummyByte) {
                addDummyByte = false;
                result = 0;
            }
            return result;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int result = super.read(b, off, len);
            if (result == -1 && addDummyByte) {
                addDummyByte = false;
                b[off] = 0;
                return 1;
            }
            return result;
        }
    }
}
