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
package org.apache.commons.compress.compressors.gzip;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.compress.compressors.FileNameUtil;

/**
 * Utility code for the GZIP compression format.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc1952">RFC 1952 GZIP File Format Specification</a>
 * @ThreadSafe
 */
public class GzipUtils {

    /** Header flag indicating a comment follows the header. */
    static final int FCOMMENT = 0x10;

    /** Header flag indicating an EXTRA subfields collection follows the header. */
    static final int FEXTRA = 0x04;

    /** Header flag indicating a header CRC follows the header. */
    static final int FHCRC = 0x02;

    /** Header flag indicating a file name follows the header. */
    static final int FNAME = 0x08;

    static final int FRESERVED = 0xE0;

    private static final FileNameUtil fileNameUtil;

    /**
     * Charset for file name and comments per the <a href="https://tools.ietf.org/html/rfc1952">GZIP File Format Specification</a>.
     */
    static final Charset GZIP_ENCODING = StandardCharsets.ISO_8859_1;

    static {
        // using LinkedHashMap so .tgz is preferred over .taz as
        // compressed extension of .tar as FileNameUtil will use the
        // first one found
        final Map<String, String> uncompressSuffix = new LinkedHashMap<>();
        uncompressSuffix.put(".tgz", ".tar");
        uncompressSuffix.put(".taz", ".tar");
        uncompressSuffix.put(".svgz", ".svg");
        uncompressSuffix.put(".cpgz", ".cpio");
        uncompressSuffix.put(".wmz", ".wmf");
        uncompressSuffix.put(".emz", ".emf");
        uncompressSuffix.put(".gz", "");
        uncompressSuffix.put(".z", "");
        uncompressSuffix.put("-gz", "");
        uncompressSuffix.put("-z", "");
        uncompressSuffix.put("_z", "");
        fileNameUtil = new FileNameUtil(uncompressSuffix, ".gz");
    }

    /**
     * Maps the given file name to the name that the file should have after compression with gzip. Common file types with custom suffixes for compressed
     * versions are automatically detected and correctly mapped. For example the name "package.tar" is mapped to "package.tgz". If no custom mapping is
     * applicable, then the default ".gz" suffix is appended to the file name.
     *
     * @param fileName name of a file
     * @return name of the corresponding compressed file
     * @deprecated Use {@link #getCompressedFileName(String)}.
     */
    @Deprecated
    public static String getCompressedFilename(final String fileName) {
        return fileNameUtil.getCompressedFileName(fileName);
    }

    /**
     * Maps the given file name to the name that the file should have after compression with gzip. Common file types with custom suffixes for compressed
     * versions are automatically detected and correctly mapped. For example the name "package.tar" is mapped to "package.tgz". If no custom mapping is
     * applicable, then the default ".gz" suffix is appended to the file name.
     *
     * @param fileName name of a file
     * @return name of the corresponding compressed file
     * @since 1.25.0
     */
    public static String getCompressedFileName(final String fileName) {
        return fileNameUtil.getCompressedFileName(fileName);
    }
    /**
     * Maps the given name of a gzip-compressed file to the name that the file should have after uncompression. Commonly used file type specific suffixes like
     * ".tgz" or ".svgz" are automatically detected and correctly mapped. For example the name "package.tgz" is mapped to "package.tar". And any file names with
     * the generic ".gz" suffix (or any other generic gzip suffix) is mapped to a name without that suffix. If no gzip suffix is detected, then the file name is
     * returned unmapped.
     *
     * @param fileName name of a file
     * @return name of the corresponding uncompressed file
     * @deprecated Use {@link #getUncompressedFileName(String)}.
     */
    @Deprecated
    public static String getUncompressedFilename(final String fileName) {
        return fileNameUtil.getUncompressedFileName(fileName);
    }
    /**
     * Maps the given name of a gzip-compressed file to the name that the file should have after uncompression. Commonly used file type specific suffixes like
     * ".tgz" or ".svgz" are automatically detected and correctly mapped. For example the name "package.tgz" is mapped to "package.tar". And any file names with
     * the generic ".gz" suffix (or any other generic gzip suffix) is mapped to a name without that suffix. If no gzip suffix is detected, then the file name is
     * returned unmapped.
     *
     * @param fileName name of a file
     * @return name of the corresponding uncompressed file
     * @since 1.25.0
     */
    public static String getUncompressedFileName(final String fileName) {
        return fileNameUtil.getUncompressedFileName(fileName);
    }
    /**
     * Detects common gzip suffixes in the given file name.
     *
     * @param fileName name of a file
     * @return {@code true} if the file name has a common gzip suffix, {@code false} otherwise
     * @deprecated Use {@link #isCompressedFileName(String)}.
     */
    @Deprecated
    public static boolean isCompressedFilename(final String fileName) {
        return fileNameUtil.isCompressedFileName(fileName);
    }
    /**
     * Detects common gzip suffixes in the given file name.
     *
     * @param fileName name of a file
     * @return {@code true} if the file name has a common gzip suffix, {@code false} otherwise
     * @since 1.25.0
     */
    public static boolean isCompressedFileName(final String fileName) {
        return fileNameUtil.isCompressedFileName(fileName);
    }

    /** Private constructor to prevent instantiation of this utility class. */
    private GzipUtils() {
    }

}
