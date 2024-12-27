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

package org.apache.commons.compress.utils;

import java.nio.charset.StandardCharsets;

/**
 * Character encoding names required of every implementation of the Java platform.
 *
 * From the Java documentation <a href="https://docs.oracle.com/javase/8/docs/api/java/nio/charset/Charset.html">Standard charsets</a>:
 * <p>
 * <cite>Every implementation of the Java platform is required to support the following character encodings. Consult the release documentation for your
 * implementation to see if any other encodings are supported. Consult the release documentation for your implementation to see if any other encodings are
 * supported. </cite>
 * </p>
 *
 * <dl>
 * <dt>{@code US-ASCII}</dt>
 * <dd>Seven-bit ASCII, a.k.a. ISO646-US, a.k.a. the Basic Latin block of the Unicode character set.</dd>
 * <dt>{@code ISO-8859-1}</dt>
 * <dd>ISO Latin Alphabet No. 1, a.k.a. ISO-LATIN-1.</dd>
 * <dt>{@code UTF-8}</dt>
 * <dd>Eight-bit Unicode Transformation Format.</dd>
 * <dt>{@code UTF-16BE}</dt>
 * <dd>Sixteen-bit Unicode Transformation Format, big-endian byte order.</dd>
 * <dt>{@code UTF-16LE}</dt>
 * <dd>Sixteen-bit Unicode Transformation Format, little-endian byte order.</dd>
 * <dt>{@code UTF-16}</dt>
 * <dd>Sixteen-bit Unicode Transformation Format, byte order specified by a mandatory initial byte-order mark (either order accepted on input, big-endian used
 * on output.)</dd>
 * </dl>
 *
 * <p>
 * This perhaps would best belong in the [lang] project. Even if a similar interface is defined in [lang], it is not foreseen that [compress] would be made to
 * depend on [lang].
 * </p>
 *
 * @see <a href="https://docs.oracle.com/javase/8/docs/api/java/nio/charset/Charset.html">Standard charsets</a>
 * @since 1.4
 * @deprecated Use {@link StandardCharsets}.
 */
@Deprecated
public class CharsetNames {
    /**
     * ISO Latin Alphabet No. 1, a.k.a. ISO-LATIN-1.
     * <p>
     * Every implementation of the Java platform is required to support this character encoding.
     * </p>
     *
     * @see <a href="https://docs.oracle.com/javase/8/docs/api/java/nio/charset/Charset.html">Standard charsets</a>
     */
    public static final String ISO_8859_1 = StandardCharsets.ISO_8859_1.name();

    /**
     * <p>
     * Seven-bit ASCII, also known as ISO646-US, also known as the Basic Latin block of the Unicode character set.
     * </p>
     * <p>
     * Every implementation of the Java platform is required to support this character encoding.
     * </p>
     *
     * @see <a href="https://docs.oracle.com/javase/8/docs/api/java/nio/charset/Charset.html">Standard charsets</a>
     */
    public static final String US_ASCII = StandardCharsets.US_ASCII.name();

    /**
     * <p>
     * Sixteen-bit Unicode Transformation Format, The byte order specified by a mandatory initial byte-order mark (either order accepted on input, big-endian
     * used on output)
     * </p>
     * <p>
     * Every implementation of the Java platform is required to support this character encoding.
     * </p>
     *
     * @see <a href="https://docs.oracle.com/javase/8/docs/api/java/nio/charset/Charset.html">Standard charsets</a>
     */
    public static final String UTF_16 = StandardCharsets.UTF_16.name();

    /**
     * <p>
     * Sixteen-bit Unicode Transformation Format, big-endian byte order.
     * </p>
     * <p>
     * Every implementation of the Java platform is required to support this character encoding.
     * </p>
     *
     * @see <a href="https://docs.oracle.com/javase/8/docs/api/java/nio/charset/Charset.html">Standard charsets</a>
     */
    public static final String UTF_16BE = StandardCharsets.UTF_16BE.name();

    /**
     * <p>
     * Sixteen-bit Unicode Transformation Format, little-endian byte order.
     * </p>
     * <p>
     * Every implementation of the Java platform is required to support this character encoding.
     * </p>
     *
     * @see <a href="https://docs.oracle.com/javase/8/docs/api/java/nio/charset/Charset.html">Standard charsets</a>
     */
    public static final String UTF_16LE = StandardCharsets.UTF_16LE.name();

    /**
     * <p>
     * Eight-bit Unicode Transformation Format.
     * </p>
     * <p>
     * Every implementation of the Java platform is required to support this character encoding.
     * </p>
     *
     * @see <a href="https://docs.oracle.com/javase/8/docs/api/java/nio/charset/Charset.html">Standard charsets</a>
     */
    public static final String UTF_8 = StandardCharsets.UTF_8.name();
}
