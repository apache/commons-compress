/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

/**
 * A ZipEncoding, which uses a java.nio {@link
 * java.nio.charset.Charset Charset} to encode names.
 * <p>The methods of this class are reentrant.</p>
 * @Immutable
 */
class NioZipEncoding implements ZipEncoding,HasCharset {

    private final Charset charset;
    private  boolean useReplacement= false;
    private static final byte[] REPLACEMENT_BYTES = new byte[]{'?'};
    private static final String REPLACEMENT_STRING = "?";

    /**
     * Construct an NioZipEncoding using the given charset.
     * @param charset  The character set to use.
     * @param useReplacement should invalid characters be replaced, or reported.
     */
    NioZipEncoding(final Charset charset, boolean useReplacement) {
        this.charset = charset;
        this.useReplacement = useReplacement;

    }

    @Override
    public Charset getCharset() {
        return charset;
    }

    /**
     * @see  ZipEncoding#canEncode(java.lang.String)
     */
    @Override
    public boolean canEncode(final String name) {
        final CharsetEncoder enc = newEncoder();

        return enc.canEncode(name);
    }

    private CharsetEncoder newEncoder() {
        if (useReplacement) {
            return charset.newEncoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE)
                .replaceWith(REPLACEMENT_BYTES);
        } else {
            return charset.newEncoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        }
    }

    private CharsetDecoder newDecoder() {
        if (!useReplacement) {
            return this.charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        } else {
            return  charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE)
                .replaceWith(REPLACEMENT_STRING);
        }
    }


    /**
     * @see ZipEncoding#encode(java.lang.String)
     */
    @Override
    public ByteBuffer encode(final String name) {
        final CharsetEncoder enc = newEncoder();

        final CharBuffer cb = CharBuffer.wrap(name);
        CharBuffer tmp=null;
        ByteBuffer out = ByteBuffer.allocate(estimateInitialBufferSize(enc, cb.remaining()));

        while (cb.remaining() > 0) {
            final CoderResult res = enc.encode(cb, out, false);

            if (res.isUnmappable() || res.isMalformed()) {

                // write the unmappable characters in utf-16
                // pseudo-URL encoding style to ByteBuffer.

                int spaceForSurrogate = estimateIncrementalEncodingSize(enc, (6 * res.length()));
                if (spaceForSurrogate > out.remaining()) {
                    // if the destination buffer isn't over sized, assume that the presence of one
                    // unmappable character makes it likely that there will be more. Find all the
                    // un-encoded characters and allocate space based on those estimates.
                    int charCount = 0;
                    for (int i = cb.position() ; i < cb.limit(); i++) {
                        if (!enc.canEncode(cb.get(i))) {
                            charCount+= 6;
                        } else {
                            charCount++;
                        }
                    }
                    int totalExtraSpace = estimateIncrementalEncodingSize(enc, charCount);
                    out = ZipEncodingHelper.growBufferBy(out, totalExtraSpace- out.remaining());
                }
                if(tmp == null) {
                    tmp = CharBuffer.allocate(6);
                }
                for (int i = 0; i < res.length(); ++i) {
                    out = encodeFully(enc, encodeSurrogate(tmp,cb.get()), out);
                }

            } else if (res.isOverflow()) {
                int increment = estimateIncrementalEncodingSize(enc, cb.remaining());
                out = ZipEncodingHelper.growBufferBy(out, increment);
            }
        }
        CoderResult coderResult = enc.encode(cb, out, true);

        assert coderResult.isUnderflow() : "unexpected coder result: " + coderResult;
        

        out.limit(out.position());
        out.rewind();
        return out;
    }

    private static ByteBuffer encodeFully(CharsetEncoder enc, CharBuffer cb, ByteBuffer out) {
        while (cb.hasRemaining()) {
            CoderResult result = enc.encode(cb, out, false);
            if (result.isOverflow()) {
                int increment = estimateIncrementalEncodingSize(enc, cb.remaining());
                out = ZipEncodingHelper.growBufferBy(out, increment);
            } 
        }
        return out;
    }

    static char[] HEX_CHARS = new char[]{
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    private CharBuffer encodeSurrogate( CharBuffer cb,char c) {
        cb.position(0).limit(6);
        cb.put('%');
        cb.put('U');

        cb.put(HEX_CHARS[(c >> 12) & 0x0f]);
        cb.put(HEX_CHARS[(c >> 8) & 0x0f]);
        cb.put(HEX_CHARS[(c >> 4) & 0x0f]);
        cb.put(HEX_CHARS[c & 0x0f]);
        cb.flip();
        return cb;
    }

    /**
     * Estimate the initial encoded size (in bytes) for a character buffer.
     * <p>
     * The estimate assumes that one character consumes uses the maximum length encoding,
     * whilst the rest use an average size encoding. This accounts for any BOM for UTF-16, at
     * the expense of a couple of extra bytes for UTF-8 encoded ASCII.
     * </p>
     *
     * @param enc        encoder to use for estimates
     * @param charChount number of characters in string
     * @return estimated size in bytes.
     */
    private int estimateInitialBufferSize(CharsetEncoder enc, int charChount) {
        float first = enc.maxBytesPerChar();
        float rest = (charChount - 1) * enc.averageBytesPerChar();
        return (int) Math.ceil(first + rest);
    }

    /**
     * Estimate the size needed for remaining characters
     *
     * @param enc       encoder to use for estimates
     * @param charCount number of characters remaining
     * @return estimated size in bytes.
     */
    private static int estimateIncrementalEncodingSize(CharsetEncoder enc, int charCount) {
        return (int) Math.ceil(charCount * enc.averageBytesPerChar());
    }

    /**
     * @see
     * ZipEncoding#decode(byte[])
     */
    @Override
    public String decode(final byte[] data) throws IOException {
        return newDecoder()
            .decode(ByteBuffer.wrap(data)).toString();
    }

}
