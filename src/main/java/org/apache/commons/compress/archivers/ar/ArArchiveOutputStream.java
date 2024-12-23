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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.LinkOption;
import java.nio.file.Path;

import org.apache.commons.compress.archivers.ArchiveOutputStream;

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

    private long entryOffset;
    private int headerPlus;
    private ArArchiveEntry prevEntry;
    private boolean prevEntryOpen;
    private int longFileMode = LONGFILE_ERROR;

    /**
     * Constructs a new instance with the given backing OutputStream.
     *
     * @param out the underlying output stream to be assigned to the field {@code this.out} for later use, or {@code null} if this instance is to be created
     *            without an underlying stream.
     */
    public ArArchiveOutputStream(final OutputStream out) {
        super(out);
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
            if (!isFinished()) {
                finish();
            }
        } finally {
            prevEntry = null;
            super.close();
        }
    }

    @Override
    public void closeArchiveEntry() throws IOException {
        checkFinished();
        if (prevEntry == null || !prevEntryOpen) {
            throw new IOException("No current entry to close");
        }
        if ((headerPlus + entryOffset) % 2 != 0) {
            out.write(PAD); // Pad byte
        }
        prevEntryOpen = false;
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

    @Override
    public void finish() throws IOException {
        if (prevEntryOpen) {
            throw new IOException("This archive contains unclosed entries.");
        }
        checkFinished();
        super.finish();
    }

    private int pad(final int offset, final int newOffset, final char fill) throws IOException {
        final int diff = newOffset - offset;
        if (diff > 0) {
            for (int i = 0; i < diff; i++) {
                write(fill);
            }
        }
        return newOffset;
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
            if (prevEntryOpen) {
                closeArchiveEntry();
            }
        }
        prevEntry = entry;
        headerPlus = writeEntryHeader(entry);
        entryOffset = 0;
        prevEntryOpen = true;
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

    private int write(final String data) throws IOException {
        return writeUsAscii(data).length;
    }

    private byte[] writeArchiveHeader() throws IOException {
        return writeUsAscii(ArArchiveEntry.HEADER);
    }

    private int writeEntryHeader(final ArArchiveEntry entry) throws IOException {
        int offset = 0;
        boolean appendName = false;
        final String eName = entry.getName();
        final int nLength = eName.length();
        if (LONGFILE_ERROR == longFileMode && nLength > 16) {
            throw new IOException("File name too long, > 16 chars: " + eName);
        }
        if (LONGFILE_BSD == longFileMode && (nLength > 16 || eName.indexOf(SPACE) > -1)) {
            appendName = true;
            final String fileNameLen = ArArchiveInputStream.BSD_LONGNAME_PREFIX + nLength;
            if (fileNameLen.length() > 16) {
                throw new IOException("File length too long, > 16 chars: " + eName);
            }
            offset += write(fileNameLen);
        } else {
            offset += write(eName);
        }
        offset = pad(offset, 16, SPACE);
        // Last modified
        offset += write(checkLength(String.valueOf(entry.getLastModified()), 12, "Last modified"));
        offset = pad(offset, 28, SPACE);
        // User ID
        offset += write(checkLength(String.valueOf(entry.getUserId()), 6, "User ID"));
        offset = pad(offset, 34, SPACE);
        // Group ID
        offset += write(checkLength(String.valueOf(entry.getGroupId()), 6, "Group ID"));
        offset = pad(offset, 40, SPACE);
        // Mode
        offset += write(checkLength(String.valueOf(Integer.toString(entry.getMode(), 8)), 8, "File mode"));
        offset = pad(offset, 48, SPACE);
        // Size
        // On overflow, the file size is incremented by the length of the name.
        offset += write(checkLength(String.valueOf(entry.getLength() + (appendName ? nLength : 0)), 10, "Size"));
        offset = pad(offset, 58, SPACE);
        offset += write(ArArchiveEntry.TRAILER);
        // Name
        if (appendName) {
            offset += write(eName);
        }
        return offset;
    }

}
