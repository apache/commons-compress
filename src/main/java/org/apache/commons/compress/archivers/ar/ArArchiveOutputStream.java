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
package org.apache.commons.compress.archivers.ar;

import static java.nio.charset.StandardCharsets.US_ASCII;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.LinkOption;
import java.nio.file.Path;

import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.utils.ArchiveUtils;

/**
 * Implements the "ar" archive format as an output stream.
 *
 * @NotThreadSafe
 */
public class ArArchiveOutputStream extends ArchiveOutputStream<ArArchiveEntry> {

    private static final char PAD = '\n';

    private static final char SPACE = ' ';

    /** Fail if a long file name is required in the archive. */
    public static final int LONGFILE_ERROR = 0;

    /** BSD ar extensions are used to store long file names in the archive. */
    public static final int LONGFILE_BSD = 1;

    private final OutputStream out;
    private long entryOffset;
    private ArArchiveEntry prevEntry;
    private boolean haveUnclosedEntry;
    private int longFileMode = LONGFILE_ERROR;

    /** Indicates if this archive is finished */
    private boolean finished;

    public ArArchiveOutputStream(final OutputStream out) {
        this.out = out;
    }

    /**
     * @throws IOException
     */
    private void checkFinished() throws IOException {
        if (finished) {
            throw new IOException("Stream has already been finished");
        }
    }

    private String checkLength(final String value, final int max, final String name) throws IOException {
        if (value.length() > max) {
            throw new IOException(name + " too long");
        }
        return value;
    }

    /**
     * Calls finish if necessary, and then closes the OutputStream
     */
    @Override
    public void close() throws IOException {
        try {
            if (!finished) {
                finish();
            }
        } finally {
            out.close();
            prevEntry = null;
        }
    }

    @Override
    public void closeArchiveEntry() throws IOException {
        checkFinished();
        if (prevEntry == null || !haveUnclosedEntry) {
            throw new IOException("No current entry to close");
        }
        if (entryOffset % 2 != 0) {
            out.write(PAD); // Pad byte
        }
        haveUnclosedEntry = false;
    }

    @Override
    public ArArchiveEntry createArchiveEntry(final File inputFile, final String entryName) throws IOException {
        checkFinished();
        return new ArArchiveEntry(inputFile, entryName);
    }

    /**
     * {@inheritDoc}
     *
     * @since 1.21
     */
    @Override
    public ArArchiveEntry createArchiveEntry(final Path inputPath, final String entryName, final LinkOption... options) throws IOException {
        checkFinished();
        return new ArArchiveEntry(inputPath, entryName, options);
    }

    private long fill(final long offset, final long newOffset, final char fill) throws IOException {
        final long diff = newOffset - offset;
        if (diff > 0) {
            for (int i = 0; i < diff; i++) {
                write(fill);
            }
        }
        return newOffset;
    }

    @Override
    public void finish() throws IOException {
        if (haveUnclosedEntry) {
            throw new IOException("This archive contains unclosed entries.");
        }
        checkFinished();
        finished = true;
    }

    @Override
    public void putArchiveEntry(final ArArchiveEntry entry) throws IOException {
        checkFinished();
        if (prevEntry == null) {
            writeArchiveHeader();
        } else {
            if (prevEntry.getLength() != entryOffset) {
                throw new IOException("Length does not match entry (" + prevEntry.getLength() + " != " + entryOffset);
            }
            if (haveUnclosedEntry) {
                closeArchiveEntry();
            }
        }
        prevEntry = entry;
        writeEntryHeader(entry);
        entryOffset = 0;
        haveUnclosedEntry = true;
    }

    /**
     * Sets the long file mode. This can be LONGFILE_ERROR(0) or LONGFILE_BSD(1). This specifies the treatment of long file names (names &gt;= 16). Default is
     * LONGFILE_ERROR.
     *
     * @param longFileMode the mode to use
     * @since 1.3
     */
    public void setLongFileMode(final int longFileMode) {
        this.longFileMode = longFileMode;
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        out.write(b, off, len);
        count(len);
        entryOffset += len;
    }

    private long write(final String data) throws IOException {
        final byte[] bytes = data.getBytes(US_ASCII);
        write(bytes);
        return bytes.length;
    }

    private void writeArchiveHeader() throws IOException {
        out.write(ArchiveUtils.toAsciiBytes(ArArchiveEntry.HEADER));
    }

    private void writeEntryHeader(final ArArchiveEntry entry) throws IOException {
        long offset = 0;
        boolean appendName = false;
        final String eName = entry.getName();
        final int nLength = eName.length();
        if (LONGFILE_ERROR == longFileMode && nLength > 16) {
            throw new IOException("File name too long, > 16 chars: " + eName);
        }
        if (LONGFILE_BSD == longFileMode && (nLength > 16 || eName.contains(" "))) {
            appendName = true;
            offset += write(ArArchiveInputStream.BSD_LONGNAME_PREFIX + nLength);
        } else {
            offset += write(eName);
        }
        // Last modified
        offset = fill(offset, 16, SPACE);
        offset += write(checkLength(String.valueOf(entry.getLastModified()), 12, "Last modified"));
        // User ID
        offset = fill(offset, 28, SPACE);
        offset += write(checkLength(String.valueOf(entry.getUserId()), 6, "User ID"));
        // Group ID
        offset = fill(offset, 34, SPACE);
        offset += write(checkLength(String.valueOf(entry.getGroupId()), 6, "Group ID"));
        // Mode
        offset = fill(offset, 40, SPACE);
        offset += write(checkLength(String.valueOf(Integer.toString(entry.getMode(), 8)), 8, "File mode"));
        // Length
        offset = fill(offset, 48, SPACE);
        offset += write(checkLength(String.valueOf(entry.getLength() + (appendName ? nLength : 0)), 10, "Size"));
        // Trailer
        offset = fill(offset, 58, SPACE);
        offset += write(ArArchiveEntry.TRAILER);
        if (appendName) {
            offset += write(eName);
        }
    }

}
