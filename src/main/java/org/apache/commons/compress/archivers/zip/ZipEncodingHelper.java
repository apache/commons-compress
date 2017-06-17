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

package org.apache.commons.compress.archivers.zip;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import org.apache.commons.compress.utils.Charsets;

/**
 * Static helper functions for robustly encoding filenames in zip files.
 */
public abstract class ZipEncodingHelper {

    /**
     * Grow a byte buffer, so it has a minimal capacity or at least
     * the double capacity of the original buffer
     *
     * @param b The original buffer.
     * @param newCapacity The minimal requested new capacity.
     * @return A byte buffer <code>r</code> with
     *         <code>r.capacity() = max(b.capacity()*2,newCapacity)</code> and
     *         all the data contained in <code>b</code> copied to the beginning
     *         of <code>r</code>.
     *
     */
    static ByteBuffer growBuffer(final ByteBuffer b, final int newCapacity) {
        b.limit(b.position());
        b.rewind();

        final int c2 = b.capacity() * 2;
        final ByteBuffer on = ByteBuffer.allocate(c2 < newCapacity ? newCapacity : c2);

        on.put(b);
        return on;
    }


    /**
     * The hexadecimal digits <code>0,...,9,A,...,F</code> encoded as
     * ASCII bytes.
     */
    private static final byte[] HEX_DIGITS =
        new byte [] {
        0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x41,
        0x42, 0x43, 0x44, 0x45, 0x46
    };

    /**
     * Append <code>%Uxxxx</code> to the given byte buffer.
     * The caller must assure, that <code>bb.remaining()&gt;=6</code>.
     *
     * @param bb The byte buffer to write to.
     * @param c The character to write.
     */
    static void appendSurrogate(final ByteBuffer bb, final char c) {

        bb.put((byte) '%');
        bb.put((byte) 'U');

        bb.put(HEX_DIGITS[(c >> 12)&0x0f]);
        bb.put(HEX_DIGITS[(c >> 8)&0x0f]);
        bb.put(HEX_DIGITS[(c >> 4)&0x0f]);
        bb.put(HEX_DIGITS[c & 0x0f]);
    }


    /**
     * name of the encoding UTF-8
     */
    static final String UTF8 = "UTF8";

    /**
     * name of the encoding UTF-8
     */
    static final ZipEncoding UTF8_ZIP_ENCODING = getZipEncoding("UTF-8");

    /**
     * Instantiates a zip encoding. An NIO based character set encoder/decoder will be returned.
     * As a special case, if the character set is UTF-8, the nio encoder will be configured  replace malformed and
     * unmappable characters with '?'. This matches existing behavior from the older fallback encoder.
     * <p>
     *     If the requested characer set cannot be found, the platform default will
     *     be used instead.
     * </p>
     * @param name The name of the zip encoding. Specify {@code null} for
     *             the platform's default encoding.
     * @return A zip encoding for the given encoding name.
     */
    public static ZipEncoding getZipEncoding(final String name) {
        Charset cs = Charset.defaultCharset();
        if (name != null) {
            try {
                cs = Charset.forName(name);
            } catch (UnsupportedCharsetException e) {
            }
        }
        boolean useReplacement = cs.name().equals("UTF-8");
        return new NioZipEncoding(cs, useReplacement);

    }

    /**
     * Returns whether a given encoding is UTF-8. If the given name is null, then check the platform's default encoding.
     *
     * @param charsetName If the given name is null, then check the platform's default encoding.
     */
    static boolean isUTF8(String charsetName) {
        if (charsetName == null) {
            // check platform's default encoding
            charsetName = Charset.defaultCharset().name();
        }
        if (Charsets.UTF_8.name().equalsIgnoreCase(charsetName)) {
            return true;
        }
        for (final String alias : Charsets.UTF_8.aliases()) {
            if (alias.equalsIgnoreCase(charsetName)) {
                return true;
            }
        }
        return false;
    }
}
