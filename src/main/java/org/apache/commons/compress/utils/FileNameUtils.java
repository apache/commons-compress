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

import java.io.File;
import java.nio.file.Path;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.file.PathUtils;

/**
 * Generic file name utilities.
 *
 * @since 1.20
 * @deprecated Use {@link PathUtils} and {@link FilenameUtils}.
 */
@Deprecated
public class FileNameUtils {

    /**
     * Gets the base name (i.e. the part up to and not including the last ".") of the last path segment of a file name.
     * <p>
     * Will return the file name itself if it doesn't contain any dots. All leading directories of the {@code file name} parameter are skipped.
     * </p>
     *
     * @return the base name of file name
     * @param path the path of the file to obtain the base name of.
     * @since 1.22
     * @deprecated Use {@link PathUtils#getBaseName(Path)}.
     */
    @Deprecated
    public static String getBaseName(final Path path) {
        return PathUtils.getBaseName(path);
    }

    /**
     * Gets the base name (i.e. the part up to and not including the last ".") of the last path segment of a file name.
     * <p>
     * Will return the file name itself if it doesn't contain any dots. All leading directories of the {@code file name} parameter are skipped.
     * </p>
     *
     * @return the base name of file name
     * @param fileName the name of the file to obtain the base name of.
     * @deprecated Use {@link FilenameUtils#removeExtension(String)}.
     */
    @Deprecated
    public static String getBaseName(final String fileName) {
        if (fileName == null) {
            return null;
        }
        return FilenameUtils.removeExtension(new File(fileName).getName());
    }

    /**
     * Gets the extension (i.e. the part after the last ".") of a file.
     * <p>
     * Will return an empty string if the file name doesn't contain any dots. Only the last segment of the file name is consulted - i.e. all leading
     * directories of the {@code file name} parameter are skipped.
     * </p>
     *
     * @return the extension of file name
     * @param path the path of the file to obtain the extension of.
     * @since 1.22
     * @deprecated Use {@link PathUtils#getExtension(Path)}.
     */
    @Deprecated
    public static String getExtension(final Path path) {
        return PathUtils.getExtension(path);
    }

    /**
     * Gets the extension (i.e. the part after the last ".") of a file.
     * <p>
     * Will return an empty string if the file name doesn't contain any dots. Only the last segment of the file name is consulted - i.e. all leading
     * directories of the {@code fileName} parameter are skipped.
     * </p>
     *
     * @return the extension of file name
     * @param fileName the name of the file to obtain the extension of.
     * @deprecated Use {@link FilenameUtils#getExtension(String)}.
     */
    @Deprecated
    public static String getExtension(final String fileName) {
        return FilenameUtils.getExtension(fileName);
    }
}
