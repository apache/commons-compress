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

/**
 * Generic file name utilities.
 * @since 1.20
 */
public class FileNameUtils {

    /**
     * Returns the extension (i.e. the part after the last ".") of a file.
     *
     * <p>Will return an empty string if the file name doesn't contain
     * any dots. Only the last segment of a the file name is consulted
     * - i.e. all leading directories of the {@code filename}
     * parameter are skipped.</p>
     *
     * @return the extension of filename
     * @param filename the name of the file to obtain the extension of.
     */
    public static String getExtension(String filename) {
        if (filename == null) {
            return null;
        }

        String name = new File(filename).getName();
        int extensionPosition = name.lastIndexOf('.');
        if (extensionPosition < 0) {
            return "";
        }
        return name.substring(extensionPosition + 1);
    }

    /**
     * Returns the basename (i.e. the part up to and not including the
     * last ".") of the last path segment of a filename.
     *
     * <p>Will return the file name itself if it doesn't contain any
     * dots. All leading directories of the {@code filename} parameter
     * are skipped.</p>
     *
     * @return the basename of filename
     * @param filename the name of the file to obtain the basename of.
     */
    public static String getBaseName(String filename) {
        if (filename == null) {
            return null;
        }

        String name = new File(filename).getName();

        int extensionPosition = name.lastIndexOf('.');
        if (extensionPosition < 0) {
            return name;
        }

        return name.substring(0, extensionPosition);
    }
}
