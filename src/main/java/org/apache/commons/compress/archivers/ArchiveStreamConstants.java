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
package org.apache.commons.compress.archivers;

/**
 * Constants for use by archivers.
 */
public class ArchiveStreamConstants {
    /**
     * Constant (value {@value}) used to identify the APK archive format.
     * <p>
     * APK file extensions are .apk, .xapk, .apks, .apkm
     * </p>
     *
     * @since 1.22
     */
    public static final String APK = "apk";

    /**
     * Constant (value {@value}) used to identify the XAPK archive format.
     * <p>
     * APK file extensions are .apk, .xapk, .apks, .apkm
     * </p>
     *
     * @since 1.22
     */
    public static final String XAPK = "xapk";

    /**
     * Constant (value {@value}) used to identify the APKS archive format.
     * <p>
     * APK file extensions are .apk, .xapk, .apks, .apkm
     * </p>
     *
     * @since 1.22
     */
    public static final String APKS = "apks";

    /**
     * Constant (value {@value}) used to identify the APKM archive format.
     * <p>
     * APK file extensions are .apk, .xapk, .apks, .apkm
     * </p>
     *
     * @since 1.22
     */
    public static final String APKM = "apkm";

    /**
     * Constant (value {@value}) used to identify the AR archive format.
     *
     * @since 1.1
     */
    public static final String AR = "ar";

    /**
     * Constant (value {@value}) used to identify the ARJ archive format. Not supported as an output stream type.
     *
     * @since 1.6
     */
    public static final String ARJ = "arj";

    /**
     * Constant (value {@value}) used to identify the CPIO archive format.
     *
     * @since 1.1
     */
    public static final String CPIO = "cpio";

    /**
     * Constant (value {@value}) used to identify the Unix DUMP archive format. Not supported as an output stream type.
     *
     * @since 1.3
     */
    public static final String DUMP = "dump";

    /**
     * Constant (value {@value}) used to identify the JAR archive format.
     *
     * @since 1.1
     */
    public static final String JAR = "jar";

    /**
     * Constant used to identify the TAR archive format.
     *
     * @since 1.1
     */
    public static final String TAR = "tar";

    /**
     * Constant (value {@value}) used to identify the ZIP archive format.
     *
     * @since 1.1
     */
    public static final String ZIP = "zip";

    /**
     * Constant (value {@value}) used to identify the 7z archive format.
     *
     * @since 1.8
     */
    public static final String SEVEN_Z = "7z";

    private ArchiveStreamConstants() {}
}
