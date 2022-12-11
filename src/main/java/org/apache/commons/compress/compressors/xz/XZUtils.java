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
import java.util.Map;

import org.apache.commons.compress.compressors.FileNameUtil;
import org.apache.commons.compress.utils.OsgiUtils;

/**
 * Utility code for the xz compression format.
 * @ThreadSafe
 * @since 1.4
 */
public class XZUtils {

    enum CachedAvailability {
        DONT_CACHE, CACHED_AVAILABLE, CACHED_UNAVAILABLE
    }

    private static final FileNameUtil fileNameUtil;

    /**
     * XZ Header Magic Bytes begin a XZ file.
     *
     * <p>This is a copy of {@code org.tukaani.xz.XZ.HEADER_MAGIC} in
     * XZ for Java version 1.5.</p>
     */
    private static final byte[] HEADER_MAGIC = {
        (byte) 0xFD, '7', 'z', 'X', 'Z', '\0'
    };

    private static volatile CachedAvailability cachedXZAvailability;

    static {
        final Map<String, String> uncompressSuffix = new HashMap<>();
        uncompressSuffix.put(".txz", ".tar");
        uncompressSuffix.put(".xz", "");
        uncompressSuffix.put("-xz", "");
        fileNameUtil = new FileNameUtil(uncompressSuffix, ".xz");
        cachedXZAvailability = CachedAvailability.DONT_CACHE;
        setCacheXZAvailablity(!OsgiUtils.isRunningInOsgiEnvironment());
    }

    // only exists to support unit tests
    static CachedAvailability getCachedXZAvailability() {
        return cachedXZAvailability;
    }

    /**
     * Maps the given file name to the name that the file should have after
     * compression with xz. Common file types with custom suffixes for
     * compressed versions are automatically detected and correctly mapped.
     * For example the name "package.tar" is mapped to "package.txz". If no
     * custom mapping is applicable, then the default ".xz" suffix is appended
     * to the file name.
     *
     * @param fileName name of a file
     * @return name of the corresponding compressed file
     */
    public static String getCompressedFilename(final String fileName) {
        return fileNameUtil.getCompressedFilename(fileName);
    }

    /**
     * Maps the given name of a xz-compressed file to the name that the
     * file should have after uncompression. Commonly used file type specific
     * suffixes like ".txz" are automatically detected and
     * correctly mapped. For example the name "package.txz" is mapped to
     * "package.tar". And any file names with the generic ".xz" suffix
     * (or any other generic xz suffix) is mapped to a name without that
     * suffix. If no xz suffix is detected, then the file name is returned
     * unmapped.
     *
     * @param fileName name of a file
     * @return name of the corresponding uncompressed file
     */
    public static String getUncompressedFilename(final String fileName) {
        return fileNameUtil.getUncompressedFilename(fileName);
    }

    private static boolean internalIsXZCompressionAvailable() {
        try {
            XZCompressorInputStream.matches(null, 0);
            return true;
        } catch (final NoClassDefFoundError error) { // NOSONAR
            return false;
        }
    }

    /**
     * Detects common xz suffixes in the given file name.
     *
     * @param fileName name of a file
     * @return {@code true} if the file name has a common xz suffix,
     *         {@code false} otherwise
     */
    public static boolean isCompressedFilename(final String fileName) {
        return fileNameUtil.isCompressedFilename(fileName);
    }

    /**
     * Are the classes required to support XZ compression available?
     * @since 1.5
     * @return true if the classes required to support XZ compression are available
     */
    public static boolean isXZCompressionAvailable() {
        final CachedAvailability cachedResult = cachedXZAvailability;
        if (cachedResult != CachedAvailability.DONT_CACHE) {
            return cachedResult == CachedAvailability.CACHED_AVAILABLE;
        }
        return internalIsXZCompressionAvailable();
    }

    /**
     * Checks if the signature matches what is expected for a .xz file.
     *
     * <p>This is more or less a copy of the version found in {@link
     * XZCompressorInputStream} but doesn't depend on the presence of
     * XZ for Java.</p>
     *
     * @param   signature     the bytes to check
     * @param   length        the number of bytes to check
     * @return  true if signature matches the .xz magic bytes, false otherwise
     * @since 1.9
     */
    public static boolean matches(final byte[] signature, final int length) {
        if (length < HEADER_MAGIC.length) {
            return false;
        }

        for (int i = 0; i < HEADER_MAGIC.length; ++i) {
            if (signature[i] != HEADER_MAGIC[i]) {
                return false;
            }
        }

        return true;
    }

    /**
     * Whether to cache the result of the XZ for Java check.
     *
     * <p>This defaults to {@code false} in an OSGi environment and {@code true} otherwise.</p>
     * @param doCache whether to cache the result
     * @since 1.9
     */
    public static void setCacheXZAvailablity(final boolean doCache) {
        if (!doCache) {
            cachedXZAvailability = CachedAvailability.DONT_CACHE;
        } else if (cachedXZAvailability == CachedAvailability.DONT_CACHE) {
            final boolean hasXz = internalIsXZCompressionAvailable();
            cachedXZAvailability = hasXz ? CachedAvailability.CACHED_AVAILABLE // NOSONAR
                : CachedAvailability.CACHED_UNAVAILABLE;
        }
    }

    /** Private constructor to prevent instantiation of this utility class. */
    private XZUtils() {
    }
}
