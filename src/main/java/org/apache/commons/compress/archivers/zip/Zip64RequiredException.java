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

import java.util.zip.ZipException;

/**
 * Exception thrown when attempting to write data that requires Zip64 support to an archive and {@link ZipArchiveOutputStream#setUseZip64 UseZip64} has been set
 * to {@link Zip64Mode#Never Never}.
 *
 * @since 1.3
 */
public class Zip64RequiredException extends ZipException {

    private static final long serialVersionUID = 20110809L;

    static final String ARCHIVE_TOO_BIG_MESSAGE = "Archive's size exceeds the limit of 4GByte.";

    static final String CENTRAL_DIRECTORY_DISK_NUMBER_TOO_BIG_MESSAGE = "Number of the disk with the start of Central Directory exceeds the limit of 65535.";

    static final String CENTRAL_DIRECTORY_SIZE_TOO_BIG_MESSAGE = "The size of the entire central directory exceeds the limit of 4GByte.";

    static final String DISK_NUMBER_TOO_BIG_MESSAGE = "Number of the disk of End Of Central Directory exceeds the limit of 65535.";

    static final String TOO_MANY_ENTRIES_MESSAGE = "Archive contains more than 65535 entries.";

    static final String TOO_MANY_ENTRIES_ON_DISK_MESSAGE = "Number of entries on this disk exceeds the limit of 65535.";

    /**
     * Helper to format "entry too big" messages.
     */
    static String getEntryTooBigMessage(final ZipArchiveEntry ze) {
        return ze.getName() + "'s size exceeds the limit of 4GByte.";
    }

    /**
     * Constructs a {@link Zip64RequiredException} with the specified detail message.
     *
     * @param message the detail message.
     */
    public Zip64RequiredException(final String message) {
        super(message);
    }
}
