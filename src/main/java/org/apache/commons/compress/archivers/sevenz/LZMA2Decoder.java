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

class LZMA2Decoder extends Coders.CoderBase {
    @Override
    InputStream decode(final InputStream in, final Coder coder, byte[] password)
        throws IOException {
        final int dictionarySizeBits = 0xff & coder.properties[0];
        if ((dictionarySizeBits & (~0x3f)) != 0) {
            throw new IOException("Unsupported LZMA2 property bits");
        }
        if (dictionarySizeBits > 40) {
            throw new IOException("Dictionary larger than 4GiB maximum size");
        }
        final int dictionarySize;
        if (dictionarySizeBits == 40) {
            dictionarySize = 0xFFFFffff;
        } else {
            dictionarySize = (2 | (dictionarySizeBits & 0x1)) << (dictionarySizeBits / 2 + 11);
        }
        return new LZMA2InputStream(in, dictionarySize);
    }

    @Override
    OutputStream encode(final OutputStream out, final byte[] password)
        throws IOException {
        LZMA2Options options = new LZMA2Options();
        options.setDictSize(LZMA2Options.DICT_SIZE_DEFAULT);
        FinishableOutputStream wrapped = new FinishableWrapperOutputStream(out);
        return options.getOutputStream(wrapped);
    }

}
