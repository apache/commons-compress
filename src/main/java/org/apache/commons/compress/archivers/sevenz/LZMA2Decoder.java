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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.compress.MemoryLimitException;
import org.tukaani.xz.FinishableWrapperOutputStream;
import org.tukaani.xz.LZMA2InputStream;
import org.tukaani.xz.LZMA2Options;

class LZMA2Decoder extends AbstractCoder {

    LZMA2Decoder() {
        super(LZMA2Options.class, Number.class);
    }

    @Override
    InputStream decode(final String archiveName, final InputStream in, final long uncompressedLength, final Coder coder, final byte[] password,
            final int maxMemoryLimitInKb) throws IOException {
        try {
            final int dictionarySize = getDictionarySize(coder);
            final int memoryUsageInKb = LZMA2InputStream.getMemoryUsage(dictionarySize);
            if (memoryUsageInKb > maxMemoryLimitInKb) {
                throw new MemoryLimitException(memoryUsageInKb, maxMemoryLimitInKb);
            }
            return new LZMA2InputStream(in, dictionarySize);
        } catch (final IllegalArgumentException ex) { // NOSONAR
            throw new IOException(ex);
        }
    }

    @SuppressWarnings("resource") // Caller closes result.
    @Override
    OutputStream encode(final OutputStream out, final Object opts) throws IOException {
        return getOptions(opts).getOutputStream(new FinishableWrapperOutputStream(out));
    }

    private int getDictionarySize(final Coder coder) throws IOException {
        if (coder.properties == null) {
            throw new IOException("Missing LZMA2 properties");
        }
        if (coder.properties.length < 1) {
            throw new IOException("LZMA2 properties too short");
        }
        final int dictionarySizeBits = 0xff & coder.properties[0];
        if ((dictionarySizeBits & (~0x3f)) != 0) {
            throw new IOException("Unsupported LZMA2 property bits");
        }
        if (dictionarySizeBits > 40) {
            throw new IOException("Dictionary larger than 4GiB maximum size");
        }
        if (dictionarySizeBits == 40) {
            return 0xFFFFffff;
        }
        return (2 | (dictionarySizeBits & 0x1)) << (dictionarySizeBits / 2 + 11);
    }

    private int getDictSize(final Object opts) {
        if (opts instanceof LZMA2Options) {
            return ((LZMA2Options) opts).getDictSize();
        }
        return numberOptionOrDefault(opts);
    }

    private LZMA2Options getOptions(final Object opts) throws IOException {
        if (opts instanceof LZMA2Options) {
            return (LZMA2Options) opts;
        }
        final LZMA2Options options = new LZMA2Options();
        options.setDictSize(numberOptionOrDefault(opts));
        return options;
    }

    @Override
    byte[] getOptionsAsProperties(final Object opts) {
        final int dictSize = getDictSize(opts);
        final int lead = Integer.numberOfLeadingZeros(dictSize);
        final int secondBit = (dictSize >>> (30 - lead)) - 2;
        return new byte[] {
            (byte) ((19 - lead) * 2 + secondBit)
        };
    }

    @Override
    Object getOptionsFromCoder(final Coder coder, final InputStream in)
        throws IOException {
        return getDictionarySize(coder);
    }

    private int numberOptionOrDefault(final Object opts) {
        return toInt(opts, LZMA2Options.DICT_SIZE_DEFAULT);
    }
}
