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
package org.apache.commons.compress.archivers.zip;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

/**
 * Enumerates known compression methods.
 *
 * Some of these methods are currently not supported by commons compress.
 *
 * @since 1.5
 */
public enum ZipMethod {

    /**
     * Compression method 0 for uncompressed entries.
     *
     * @see ZipEntry#STORED
     */
    STORED(ZipEntry.STORED),

    /**
     * UnShrinking. dynamic Lempel-Ziv-Welch-Algorithm
     *
     * @see <a href="https://www.pkware.com/documents/casestudies/APPNOTE.TXT">Explanation of fields: compression method: (2 bytes)</a>
     */
    UNSHRINKING(1),

    /**
     * Reduced with compression factor 1.
     *
     * @see <a href="https://www.pkware.com/documents/casestudies/APPNOTE.TXT">Explanation of fields: compression method: (2 bytes)</a>
     */
    EXPANDING_LEVEL_1(2),

    /**
     * Reduced with compression factor 2.
     *
     * @see <a href="https://www.pkware.com/documents/casestudies/APPNOTE.TXT">Explanation of fields: compression method: (2 bytes)</a>
     */
    EXPANDING_LEVEL_2(3),

    /**
     * Reduced with compression factor 3.
     *
     * @see <a href="https://www.pkware.com/documents/casestudies/APPNOTE.TXT">Explanation of fields: compression method: (2 bytes)</a>
     */
    EXPANDING_LEVEL_3(4),

    /**
     * Reduced with compression factor 4.
     *
     * @see <a href="https://www.pkware.com/documents/casestudies/APPNOTE.TXT">Explanation of fields: compression method: (2 bytes)</a>
     */
    EXPANDING_LEVEL_4(5),

    /**
     * Imploding.
     *
     * @see <a href="https://www.pkware.com/documents/casestudies/APPNOTE.TXT">Explanation of fields: compression method: (2 bytes)</a>
     */
    IMPLODING(6),

    /**
     * Tokenization.
     *
     * @see <a href="https://www.pkware.com/documents/casestudies/APPNOTE.TXT">Explanation of fields: compression method: (2 bytes)</a>
     */
    TOKENIZATION(7),

    /**
     * Compression method 8 for compressed (deflated) entries.
     *
     * @see ZipEntry#DEFLATED
     */
    DEFLATED(ZipEntry.DEFLATED),

    /**
     * Compression Method 9 for enhanced deflate.
     *
     * @see <a href="https://www.pkware.com/documents/casestudies/APPNOTE.TXT">Explanation of fields: compression method: (2 bytes)</a>
     */
    ENHANCED_DEFLATED(9),

    /**
     * PKWARE Data Compression Library Imploding.
     *
     * @see <a href="https://www.pkware.com/documents/casestudies/APPNOTE.TXT">Explanation of fields: compression method: (2 bytes)</a>
     */
    PKWARE_IMPLODING(10),

    /**
     * Compression Method 12 for bzip2.
     *
     * @see <a href="https://www.pkware.com/documents/casestudies/APPNOTE.TXT">Explanation of fields: compression method: (2 bytes)</a>
     */
    BZIP2(12),

    /**
     * Compression Method 14 for LZMA.
     *
     * @see <a href="https://www.7-zip.org/sdk.html">https://www.7-zip.org/sdk.html</a>
     * @see <a href="https://www.pkware.com/documents/casestudies/APPNOTE.TXT">Explanation of fields: compression method: (2 bytes)</a>
     */
    LZMA(14),

    /**
     * Compression Method 20 for Zstandard (deprecated).
     *
     * @see <a href="https://github.com/facebook/zstd">Facebook Zstandard source code</a>
     * @see <a href="https://pkwaredownloads.blob.core.windows.net/pkware-general/Documentation/APPNOTE-6.3.7.TXT">.ZIP File Format Specification 6.3.7:
     *      Deprecated zstd compression method id</a>
     * @see <a href="https://www.pkware.com/documents/casestudies/APPNOTE.TXT">.ZIP File Format Specification: Explanation of fields: compression method: (2
     *      bytes)</a>
     * @since 1.28.0
     */
    ZSTD_DEPRECATED(20),

    /**
     * Compression Method 93 for Zstandard.
     *
     * @see <a href="https://github.com/facebook/zstd">Facebook Zstandard source code</a>
     * @see <a href="https://pkwaredownloads.blob.core.windows.net/pkware-general/Documentation/APPNOTE-6.3.8.TXT">.ZIP File Format Specification 6.3.8: Changed
     *      zstd compression method id</a>
     * @see <a href="https://www.pkware.com/documents/casestudies/APPNOTE.TXT">.ZIP File Format Specification: Explanation of fields: compression method: (2
     *      bytes)</a>
     * @since 1.28.0
     */
    ZSTD(93),

    /**
     * Compression Method 95 for XZ.
     *
     * @see <a href="https://www.pkware.com/documents/casestudies/APPNOTE.TXT">Explanation of fields: compression method: (2 bytes)</a>
     */
    XZ(95),

    /**
     * Compression Method 96 for Jpeg compression.
     *
     * @see <a href="https://www.pkware.com/documents/casestudies/APPNOTE.TXT">Explanation of fields: compression method: (2 bytes)</a>
     */
    JPEG(96),

    /**
     * Compression Method 97 for WavPack.
     *
     * @see <a href="https://www.pkware.com/documents/casestudies/APPNOTE.TXT">Explanation of fields: compression method: (2 bytes)</a>
     */
    WAVPACK(97),

    /**
     * Compression Method 98 for PPMd.
     *
     * @see <a href="https://www.pkware.com/documents/casestudies/APPNOTE.TXT">Explanation of fields: compression method: (2 bytes)</a>
     */
    PPMD(98),

    /**
     * Compression Method 99 for AES encryption.
     *
     * @see <a href="https://www.pkware.com/documents/casestudies/APPNOTE.TXT">Explanation of fields: compression method: (2 bytes)</a>
     */
    AES_ENCRYPTED(99),

    /**
     * Unknown compression method.
     */
    UNKNOWN();

    static final int UNKNOWN_CODE = -1;

    private static final Map<Integer, ZipMethod> codeToEnum = Collections
            .unmodifiableMap(Stream.of(values()).collect(Collectors.toMap(ZipMethod::getCode, Function.identity())));

    /**
     * Gets the {@link ZipMethod} for the given code or null if the method is not known.
     *
     * @param code the code.
     * @return the {@link ZipMethod} for the given code or null if the method is not known.
     */
    public static ZipMethod getMethodByCode(final int code) {
        return codeToEnum.get(code);
    }

    /**
     * Tests whether the given ZIP method is a ZStandard method.
     *
     * @param method The method to test.
     * @return Whether the given ZIP method is a ZStandard method.
     */
    static boolean isZstd(final int method) {
        return method == ZSTD.getCode() || method == ZSTD_DEPRECATED.getCode();
    }

    private final int code;

    ZipMethod() {
        this(UNKNOWN_CODE);
    }

    /**
     * Constructs a new instance.
     */
    ZipMethod(final int code) {
        this.code = code;
    }

    /**
     * Gets the code of the compression method.
     *
     * @see ZipArchiveEntry#getMethod()
     * @return an integer code for the method.
     */
    public int getCode() {
        return code;
    }
}
