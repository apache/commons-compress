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
 */

package org.apache.commons.compress.archivers.zip;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

import org.apache.commons.compress.utils.CharsetNames;
import org.apache.commons.io.Charsets;

/**
 * Static helper functions for robustly encoding file names in ZIP files.
 */
public abstract class ZipEncodingHelper {

    /**
     * UTF-8.
     */
    static final ZipEncoding ZIP_ENCODING_UTF_8 = getZipEncoding(CharsetNames.UTF_8);

    /**
     * Instantiates a ZIP encoding. An NIO based character set encoder/decoder will be returned. As a special case, if the character set is UTF-8, the NIO
     * encoder will be configured replace malformed and unmappable characters with '?'. This matches existing behavior from the older fallback encoder.
     * <p>
     * If the requested character set cannot be found, the platform default will be used instead.
     * </p>
     *
     * @param charset The charset of the ZIP encoding. Specify {@code null} for the platform's default encoding.
     * @return A ZIP encoding for the given encoding name.
     * @since 1.26.0
     */
    public static ZipEncoding getZipEncoding(final Charset charset) {
        final Charset actual = Charsets.toCharset(charset);
        final boolean useReplacement = isUTF8(actual);
        return new NioZipEncoding(actual, useReplacement);
    }

    /**
     * Instantiates a ZIP encoding. An NIO based character set encoder/decoder will be returned. As a special case, if the character set is UTF-8, the NIO
     * encoder will be configured replace malformed and unmappable characters with '?'. This matches existing behavior from the older fallback encoder.
     * <p>
     * If the requested character set cannot be found, the platform default will be used instead.
     * </p>
     *
     * @param name The name of the ZIP encoding. Specify {@code null} for the platform's default encoding.
     * @return A ZIP encoding for the given encoding name.
     */
    public static ZipEncoding getZipEncoding(final String name) {
        Charset charset = Charset.defaultCharset();
        try {
            charset = Charsets.toCharset(name);
        } catch (final UnsupportedCharsetException ignore) { // NOSONAR we use the default encoding instead
        }
        final boolean useReplacement = isUTF8(charset.name());
        return new NioZipEncoding(charset, useReplacement);
    }

    static ByteBuffer growBufferBy(final ByteBuffer buffer, final int increment) {
        buffer.limit(buffer.position());
        buffer.rewind();
        final ByteBuffer on = ByteBuffer.allocate(buffer.capacity() + increment);
        on.put(buffer);
        return on;
    }

    /**
     * Tests whether a given encoding is UTF-8. If the given name is null, then check the platform's default encoding.
     *
     * @param charset If the given charset is null, then check the platform's default encoding.
     */
    static boolean isUTF8(final Charset charset) {
        return isUTF8Alias(Charsets.toCharset(charset).name());
    }

    /**
     * Tests whether a given encoding is UTF-8. If the given name is null, then check the platform's default encoding.
     *
     * @param charsetName If the given name is null, then check the platform's default encoding.
     */
    static boolean isUTF8(final String charsetName) {
        return isUTF8Alias(charsetName != null ? charsetName : Charset.defaultCharset().name());
    }

    private static boolean isUTF8Alias(final String actual) {
        return UTF_8.name().equalsIgnoreCase(actual) || UTF_8.aliases().stream().anyMatch(alias -> alias.equalsIgnoreCase(actual));
    }
}
