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
package org.apache.commons.compress.archivers.ar;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Date;
import java.util.Objects;

import org.apache.commons.compress.archivers.ArchiveEntry;

/**
 * Represents an archive entry in the "ar" format.
 * <p>
 * Each AR archive starts with "!&lt;arch&gt;" followed by a LF. After these 8 bytes the archive entries are listed. The format of an entry header is as it
 * follows:
 * </p>
 *
 * <pre>
 * START BYTE   END BYTE    NAME                    FORMAT      LENGTH
 * 0            15          File name               ASCII       16
 * 16           27          Modification timestamp  Decimal     12
 * 28           33          Owner ID                Decimal     6
 * 34           39          Group ID                Decimal     6
 * 40           47          File mode               Octal       8
 * 48           57          File size (bytes)       Decimal     10
 * 58           59          File magic              \140\012    2
 * </pre>
 * <p>
 * This specifies that an ar archive entry header contains 60 bytes.
 * </p>
 * <p>
 * Due to the limitation of the file name length to 16 bytes GNU and BSD has their own variants of this format. Currently Commons Compress can read but not
 * write the GNU variant. It fully supports the BSD variant.
 * </p>
 *
 * @see <a href="https://www.freebsd.org/cgi/man.cgi?query=ar&sektion=5">ar man page</a>
 * @Immutable
 */
public class ArArchiveEntry implements ArchiveEntry {

    /** The header for each entry */
    public static final String HEADER = "!<arch>\n";

    /** The trailer for each entry {@code 0x60 0x0A} */
    public static final String TRAILER = "`\012";

    private static final int DEFAULT_MODE = 33188; // = (octal) 0100644

    /**
     * SVR4/GNU adds a trailing / to names; BSD does not. They also vary in how names longer than 16 characters are represented. (Not yet fully supported by
     * this implementation)
     */
    private final String name;
    private final int userId;
    private final int groupId;
    private final int mode;
    private final long lastModified;
    private final long length;

    /**
     * Creates a new instance using the attributes of the given file
     *
     * @param inputFile the file to create an entry from
     * @param entryName the name of the entry
     */
    public ArArchiveEntry(final File inputFile, final String entryName) {
        // TODO sort out mode
        this(entryName, inputFile.isFile() ? inputFile.length() : 0, 0, 0, DEFAULT_MODE, inputFile.lastModified() / 1000);
    }

    /**
     * Creates a new instance using the attributes of the given file
     *
     * @param inputPath the file to create an entry from
     * @param entryName the name of the entry
     * @param options   options indicating how symbolic links are handled.
     * @throws IOException if an I/O error occurs.
     * @since 1.21
     */
    public ArArchiveEntry(final Path inputPath, final String entryName, final LinkOption... options) throws IOException {
        this(entryName, Files.isRegularFile(inputPath, options) ? Files.size(inputPath) : 0, 0, 0, DEFAULT_MODE,
                Files.getLastModifiedTime(inputPath, options).toMillis() / 1000);
    }

    /**
     * Constructs a new instance using a couple of default values.
     *
     * <p>
     * Sets userId and groupId to 0, the octal file mode to 644 and the last modified time to the current time.
     * </p>
     *
     * @param name   name of the entry
     * @param length length of the entry in bytes
     */
    public ArArchiveEntry(final String name, final long length) {
        this(name, length, 0, 0, DEFAULT_MODE, System.currentTimeMillis() / 1000);
    }

    /**
     * Constructs a new instance.
     *
     * @param name         name of the entry
     * @param length       length of the entry in bytes
     * @param userId       numeric user id
     * @param groupId      numeric group id
     * @param mode         file mode
     * @param lastModified last modified time in seconds since the epoch
     */
    public ArArchiveEntry(final String name, final long length, final int userId, final int groupId, final int mode, final long lastModified) {
        this.name = name;
        if (length < 0) {
            throw new IllegalArgumentException("length must not be negative");
        }
        this.length = length;
        this.userId = userId;
        this.groupId = groupId;
        this.mode = mode;
        this.lastModified = lastModified;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final ArArchiveEntry other = (ArArchiveEntry) obj;
        return Objects.equals(name, other.name);
    }

    /**
     * Gets the group ID.
     *
     * @return the group ID.
     */
    public int getGroupId() {
        return groupId;
    }

    /**
     * Gets the last modified time in seconds since the epoch.
     *
     * @return the last modified date.
     */
    public long getLastModified() {
        return lastModified;
    }

    @Override
    public Date getLastModifiedDate() {
        return new Date(1000 * getLastModified());
    }

    /**
     * Gets the length.
     *
     * @return the length.
     */
    public long getLength() {
        return length;
    }

    /**
     * Gets the mode.
     *
     * @return the mode.
     */
    public int getMode() {
        return mode;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long getSize() {
        return getLength();
    }

    /**
     * Gets the user ID.
     *
     * @return the user ID.
     */
    public int getUserId() {
        return userId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public boolean isDirectory() {
        return false;
    }
}
