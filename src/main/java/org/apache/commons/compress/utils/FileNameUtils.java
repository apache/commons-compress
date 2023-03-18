/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.commons.compress.utils;

import java.io.File;
import java.nio.file.Path;

/**
 * Generic file name utilities.
 * @since 1.20
 */
public class FileNameUtils {

    private static String fileNameToBaseName(final String name) {
        final int extensionIndex = name.lastIndexOf('.');
        return extensionIndex < 0 ? name : name.substring(0, extensionIndex);
    }

    private static String fileNameToExtension(final String name) {
        final int extensionIndex = name.lastIndexOf('.');
        return extensionIndex < 0 ? "" : name.substring(extensionIndex + 1);
    }

    /**
     * Gets the basename (i.e. the part up to and not including the
     * last ".") of the last path segment of a filename.
     * <p>Will return the file name itself if it doesn't contain any
     * dots. All leading directories of the {@code filename} parameter
     * are skipped.</p>
     * @return the basename of filename
     * @param path the path of the file to obtain the basename of.
     * @since 1.22
     */
    public static String getBaseName(final Path path) {
        if (path == null) {
            return null;
        }
        final Path fileName = path.getFileName();
        return fileName != null ? fileNameToBaseName(fileName.toString()) : null;
    }

    /**
     * Gets the basename (i.e. the part up to and not including the
     * last ".") of the last path segment of a filename.
     *
     * <p>Will return the file name itself if it doesn't contain any
     * dots. All leading directories of the {@code filename} parameter
     * are skipped.</p>
     *
     * @return the basename of filename
     * @param filename the name of the file to obtain the basename of.
     */
    public static String getBaseName(final String filename) {
        if (filename == null) {
            return null;
        }
        return fileNameToBaseName(new File(filename).getName());
    }

    /**
     * Gets the extension (i.e. the part after the last ".") of a file.
     * <p>Will return an empty string if the file name doesn't contain
     * any dots. Only the last segment of a the file name is consulted
     * - i.e. all leading directories of the {@code filename}
     * parameter are skipped.</p>
     * @return the extension of filename
     * @param path the path of the file to obtain the extension of.
     * @since 1.22
     */
    public static String getExtension(final Path path) {
        if (path == null) {
            return null;
        }
        final Path fileName = path.getFileName();
        return fileName != null ? fileNameToExtension(fileName.toString()) : null;
    }

    /**
     * Gets the extension (i.e. the part after the last ".") of a file.
     *
     * <p>Will return an empty string if the file name doesn't contain
     * any dots. Only the last segment of a the file name is consulted
     * - i.e. all leading directories of the {@code filename}
     * parameter are skipped.</p>
     *
     * @return the extension of filename
     * @param filename the name of the file to obtain the extension of.
     */
    public static String getExtension(final String filename) {
        if (filename == null) {
            return null;
        }
        return fileNameToExtension(new File(filename).getName());
    }
}
