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
package org.apache.commons.compress.compressors;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * File name mapping code for the compression formats.
 *
 * @ThreadSafe
 * @since 1.4
 */
public class FileNameUtil {

    /**
     * Map from common file name suffixes to the suffixes that identify compressed versions of those file types. For example: from ".tar" to ".tgz".
     */
    private final Map<String, String> compressSuffix = new HashMap<>();

    /**
     * Map from common file name suffixes of compressed files to the corresponding suffixes of uncompressed files. For example: from ".tgz" to ".tar".
     * <p>
     * This map also contains format-specific suffixes like ".gz" and "-z". These suffixes are mapped to the empty string, as they should simply be removed from
     * the file name when the file is uncompressed.
     */
    private final Map<String, String> uncompressSuffix;

    /**
     * Length of the longest compressed suffix.
     */
    private final int longestCompressedSuffix;

    /**
     * Length of the shortest compressed suffix.
     */
    private final int shortestCompressedSuffix;

    /**
     * Length of the longest uncompressed suffix.
     */
    private final int longestUncompressedSuffix;

    /**
     * Length of the shortest uncompressed suffix longer than the empty string.
     */
    private final int shortestUncompressedSuffix;

    /**
     * The format's default extension.
     */
    private final String defaultExtension;

    /**
     * sets up the utility with a map of known compressed to uncompressed suffix mappings and the default extension of the format.
     *
     * @param uncompressSuffix Map from common file name suffixes of compressed files to the corresponding suffixes of uncompressed files. For example: from
     *                         ".tgz" to ".tar". This map also contains format-specific suffixes like ".gz" and "-z". These suffixes are mapped to the empty
     *                         string, as they should simply be removed from the file name when the file is uncompressed.
     *
     * @param defaultExtension the format's default extension like ".gz"
     */
    public FileNameUtil(final Map<String, String> uncompressSuffix, final String defaultExtension) {
        this.uncompressSuffix = Collections.unmodifiableMap(uncompressSuffix);
        int lc = Integer.MIN_VALUE, sc = Integer.MAX_VALUE;
        int lu = Integer.MIN_VALUE, su = Integer.MAX_VALUE;
        for (final Map.Entry<String, String> ent : uncompressSuffix.entrySet()) {
            final int cl = ent.getKey().length();
            if (cl > lc) {
                lc = cl;
            }
            if (cl < sc) {
                sc = cl;
            }

            final String u = ent.getValue();
            final int ul = u.length();
            if (ul > 0) {
                compressSuffix.computeIfAbsent(u, k -> ent.getKey());
                if (ul > lu) {
                    lu = ul;
                }
                if (ul < su) {
                    su = ul;
                }
            }
        }
        longestCompressedSuffix = lc;
        longestUncompressedSuffix = lu;
        shortestCompressedSuffix = sc;
        shortestUncompressedSuffix = su;
        this.defaultExtension = defaultExtension;
    }

    /**
     * Maps the given file name to the name that the file should have after compression. Common file types with custom suffixes for compressed versions are
     * automatically detected and correctly mapped. For example the name "package.tar" is mapped to "package.tgz". If no custom mapping is applicable, then the
     * default ".gz" suffix is appended to the file name.
     *
     * @param fileName name of a file
     * @return name of the corresponding compressed file
     * @deprecated Use {@link #getCompressedFileName(String)}.
     */
    @Deprecated
    public String getCompressedFilename(final String fileName) {
        return getCompressedFileName(fileName);
    }

    /**
     * Maps the given file name to the name that the file should have after compression. Common file types with custom suffixes for compressed versions are
     * automatically detected and correctly mapped. For example the name "package.tar" is mapped to "package.tgz". If no custom mapping is applicable, then the
     * default ".gz" suffix is appended to the file name.
     *
     * @param fileName name of a file
     * @return name of the corresponding compressed file
     * @since 1.25.0
     */
    public String getCompressedFileName(final String fileName) {
        final String lower = fileName.toLowerCase(Locale.ENGLISH);
        final int n = lower.length();
        for (int i = shortestUncompressedSuffix; i <= longestUncompressedSuffix && i < n; i++) {
            final String suffix = compressSuffix.get(lower.substring(n - i));
            if (suffix != null) {
                return fileName.substring(0, n - i) + suffix;
            }
        }
        // No custom suffix found, just append the default
        return fileName + defaultExtension;
    }

    /**
     * Maps the given name of a compressed file to the name that the file should have after uncompression. Commonly used file type specific suffixes like ".tgz"
     * or ".svgz" are automatically detected and correctly mapped. For example the name "package.tgz" is mapped to "package.tar". And any file names with the
     * generic ".gz" suffix (or any other generic gzip suffix) is mapped to a name without that suffix. If no format suffix is detected, then the file name is
     * returned unmapped.
     *
     * @param fileName name of a file
     * @return name of the corresponding uncompressed file
     * @deprecated Use {@link #getUncompressedFileName(String)}.
     */
    @Deprecated
    public String getUncompressedFilename(final String fileName) {
        return getUncompressedFileName(fileName);
    }

    /**
     * Maps the given name of a compressed file to the name that the file should have after uncompression. Commonly used file type specific suffixes like ".tgz"
     * or ".svgz" are automatically detected and correctly mapped. For example the name "package.tgz" is mapped to "package.tar". And any file names with the
     * generic ".gz" suffix (or any other generic gzip suffix) is mapped to a name without that suffix. If no format suffix is detected, then the file name is
     * returned unmapped.
     *
     * @param fileName name of a file
     * @return name of the corresponding uncompressed file
     * @since 1.25.0
     */
    public String getUncompressedFileName(final String fileName) {
        final String lower = fileName.toLowerCase(Locale.ENGLISH);
        final int n = lower.length();
        for (int i = shortestCompressedSuffix; i <= longestCompressedSuffix && i < n; i++) {
            final String suffix = uncompressSuffix.get(lower.substring(n - i));
            if (suffix != null) {
                return fileName.substring(0, n - i) + suffix;
            }
        }
        return fileName;
    }

    /**
     * Detects common format suffixes in the given file name.
     *
     * @param fileName name of a file
     * @return {@code true} if the file name has a common format suffix, {@code false} otherwise
     * @deprecated Use {@link #isCompressedFileName(String)}.
     */
    @Deprecated
    public boolean isCompressedFilename(final String fileName) {
        return isCompressedFileName(fileName);
    }

    /**
     * Detects common format suffixes in the given file name.
     *
     * @param fileName name of a file
     * @return {@code true} if the file name has a common format suffix, {@code false} otherwise
     * @since 1.25.0
     */
    public boolean isCompressedFileName(final String fileName) {
        final String lower = fileName.toLowerCase(Locale.ENGLISH);
        final int n = lower.length();
        for (int i = shortestCompressedSuffix; i <= longestCompressedSuffix && i < n; i++) {
            if (uncompressSuffix.containsKey(lower.substring(n - i))) {
                return true;
            }
        }
        return false;
    }
}
