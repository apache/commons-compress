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
     * Construct an NIO based zip encoding, which wraps the given
     * charset.
     *
     * @param charset The NIO charset to wrap.
     */
    NioZipEncoding(final Charset charset) {
        this.charset = charset;
    }

    NioZipEncoding(final Charset charset, boolean useReplacement) {
        this(charset);
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
        int estimatedSize = (int) Math.ceil(name.length() * enc.averageBytesPerChar());
        ByteBuffer out = ByteBuffer.allocate(estimatedSize);

        while (cb.remaining() > 0) {
            final CoderResult res = enc.encode(cb, out,true);

            if (res.isUnmappable() || res.isMalformed()) {

                // write the unmappable characters in utf-16
                // pseudo-URL encoding style to ByteBuffer.
                if (res.length() * 6 > out.remaining()) {
                    out = ZipEncodingHelper.growBuffer(out, out.position()
                                                       + res.length() * 6);
                }

                for (int i=0; i<res.length(); ++i) {
                    ZipEncodingHelper.appendSurrogate(out,cb.get());
                }

            } else if (res.isOverflow()) {

                out = ZipEncodingHelper.growBuffer(out, 0);

            } else if (res.isUnderflow()) {

                enc.flush(out);
                break;

            }
        }

        out.limit(out.position());
        out.rewind();
        return out;
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
