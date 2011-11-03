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
package org.apache.commons.compress.compressors.xz;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Utility code for the xz compression format.
 * @ThreadSafe
 * @since Commons Compress 1.4
 */
public class XZUtils {

    /**
     * Map from common filename suffixes to the suffixes that identify xzped
     * versions of those file types. For example: from ".tar" to ".txz".
     */
    private static final Map<String, String> compressSuffix =
        new HashMap<String, String>();

    /**
     * Map from common filename suffixes of xzped files to the corresponding
     * suffixes of uncompressed files. For example: from ".tgz" to ".tar".
     * <p>
     * This map also contains xz-specific suffixes like ".gz" and "-z".
     * These suffixes are mapped to the empty string, as they should simply
     * be removed from the filename when the file is uncompressed.
     */
    private static final Map<String, String> uncompressSuffix =
        new HashMap<String, String>();

    static {
        compressSuffix.put(".tar", ".txz");

        uncompressSuffix.put(".txz", ".tar");
        uncompressSuffix.put(".xz", "");
        uncompressSuffix.put("-xz", "");
    }
    // N.B. if any shorter or longer keys are added, ensure the for loop limits are changed

    /** Private constructor to prevent instantiation of this utility class. */
    private XZUtils() {
    }

    /**
     * Detects common xz suffixes in the given filename.
     *
     * @param filename name of a file
     * @return <code>true</code> if the filename has a common xz suffix,
     *         <code>false</code> otherwise
     */
    public static boolean isCompressedFilename(String filename) {
        String lower = filename.toLowerCase(Locale.ENGLISH);
        int n = lower.length();
        // Shortest suffix is three letters (.xz), longest is four (.txz)
        for (int i = 3; i <= 4 && i < n; i++) {
            if (uncompressSuffix.containsKey(lower.substring(n - i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Maps the given name of a xz-compressed file to the name that the
     * file should have after uncompression. Commonly used file type specific
     * suffixes like ".txz" are automatically detected and
     * correctly mapped. For example the name "package.txz" is mapped to
     * "package.tar". And any filenames with the generic ".xz" suffix
     * (or any other generic xz suffix) is mapped to a name without that
     * suffix. If no xz suffix is detected, then the filename is returned
     * unmapped.
     *
     * @param filename name of a file
     * @return name of the corresponding uncompressed file
     */
    public static String getUncompressedFilename(String filename) {
        String lower = filename.toLowerCase(Locale.ENGLISH);
        int n = lower.length();
        // Shortest suffix is three letters (.xz), longest is four (.txz)
        for (int i = 3; i <= 4 && i < n; i++) {
            Object suffix = uncompressSuffix.get(lower.substring(n - i));
            if (suffix != null) {
                return filename.substring(0, n - i) + suffix;
            }
        }
        return filename;
    }

    /**
     * Maps the given filename to the name that the file should have after
     * compression with xz. Common file types with custom suffixes for
     * compressed versions are automatically detected and correctly mapped.
     * For example the name "package.tar" is mapped to "package.txz". If no
     * custom mapping is applicable, then the default ".xz" suffix is appended
     * to the filename.
     *
     * @param filename name of a file
     * @return name of the corresponding compressed file
     */
    public static String getCompressedFilename(String filename) {
        String lower = filename.toLowerCase(Locale.ENGLISH);
        int n = lower.length();
        // Shortest suffix is three letters (.xz), longest is four (.txz)
        for (int i = 3; i <= 4 && i < n; i++) {
            Object suffix = compressSuffix.get(lower.substring(n - i));
            if (suffix != null) {
                return filename.substring(0, n - i) + suffix;
            }
        }
        // No custom suffix found, just append the default .gz
        return filename + ".xz";
    }

}
