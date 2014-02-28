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

import org.tukaani.xz.FinishableWrapperOutputStream;
import org.tukaani.xz.FinishableOutputStream;
import org.tukaani.xz.LZMA2InputStream;
import org.tukaani.xz.LZMA2Options;

class LZMA2Decoder extends CoderBase {
    LZMA2Decoder() {
        super(LZMA2Options.class, Number.class);
    }

    @Override
    InputStream decode(final InputStream in, final Coder coder, byte[] password) throws IOException {
        try {
            int dictionarySize = getDictionarySize(coder);
            return new LZMA2InputStream(in, dictionarySize);
        } catch (IllegalArgumentException ex) {
            throw new IOException(ex.getMessage());
        }
    }

    @Override
    OutputStream encode(final OutputStream out, final Object opts)
        throws IOException {
        LZMA2Options options = getOptions(opts);
        FinishableOutputStream wrapped = new FinishableWrapperOutputStream(out);
        return options.getOutputStream(wrapped);
    }

    @Override
    byte[] getOptionsAsProperties(Object opts) {
        int dictSize = getDictSize(opts);
        int lead = Integer.numberOfLeadingZeros(dictSize);
        int secondBit = (dictSize >>> (30 - lead)) - 2;
        return new byte[] {
            (byte) ((19 - lead) * 2 + secondBit)
        };
    }

    @Override
    Object getOptionsFromCoder(Coder coder, InputStream in) {
        return Integer.valueOf(getDictionarySize(coder));
    }

    private int getDictSize(Object opts) {
        if (opts instanceof LZMA2Options) {
            return ((LZMA2Options) opts).getDictSize();
        }
        return numberOptionOrDefault(opts);
    }

    private int getDictionarySize(Coder coder) throws IllegalArgumentException {
        final int dictionarySizeBits = 0xff & coder.properties[0];
        if ((dictionarySizeBits & (~0x3f)) != 0) {
            throw new IllegalArgumentException("Unsupported LZMA2 property bits");
        }
        if (dictionarySizeBits > 40) {
            throw new IllegalArgumentException("Dictionary larger than 4GiB maximum size");
        }
        if (dictionarySizeBits == 40) {
            return 0xFFFFffff;
        }
        return (2 | (dictionarySizeBits & 0x1)) << (dictionarySizeBits / 2 + 11);
    }

    private LZMA2Options getOptions(Object opts) throws IOException {
        if (opts instanceof LZMA2Options) {
            return (LZMA2Options) opts;
        }
        LZMA2Options options = new LZMA2Options();
        options.setDictSize(numberOptionOrDefault(opts));
        return options;
    }

    private int numberOptionOrDefault(Object opts) {
        return numberOptionOrDefault(opts, LZMA2Options.DICT_SIZE_DEFAULT);
    }
}
