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

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.utils.ArchiveUtils;

/**
 * Implements the "ar" archive format as an output stream.
 *
 * @NotThreadSafe
 */
public class ArArchiveOutputStream extends ArchiveOutputStream {
    /** Fail if a long file name is required in the archive. */
    public static final int LONGFILE_ERROR = 0;

    /** BSD ar extensions are used to store long file names in the archive. */
    public static final int LONGFILE_BSD = 1;

    private final OutputStream out;
    private long entryOffset;
    private ArArchiveEntry prevEntry;
    private boolean haveUnclosedEntry;
    private int longFileMode = LONGFILE_ERROR;

    /** indicates if this archive is finished */
    private boolean finished;

    public ArArchiveOutputStream(final OutputStream pOut) {
        this.out = pOut;
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
        if (finished) {
            throw new IOException("Stream has already been finished");
        }
        if (prevEntry == null || !haveUnclosedEntry){
            throw new IOException("No current entry to close");
        }
        if (entryOffset % 2 != 0) {
            out.write('\n'); // Pad byte
        }
        haveUnclosedEntry = false;
    }

    @Override
    public ArchiveEntry createArchiveEntry(final File inputFile, final String entryName)
        throws IOException {
        if (finished) {
            throw new IOException("Stream has already been finished");
        }
        return new ArArchiveEntry(inputFile, entryName);
    }

    /**
     * {@inheritDoc}
     *
     * @since 1.21
     */
    @Override
    public ArchiveEntry createArchiveEntry(final Path inputPath, final String entryName, final LinkOption... options) throws IOException {
        if (finished) {
            throw new IOException("Stream has already been finished");
        }
        return new ArArchiveEntry(inputPath, entryName, options);
    }

    private long fill(final long pOffset, final long pNewOffset, final char pFill) throws IOException {
        final long diff = pNewOffset - pOffset;

        if (diff > 0) {
            for (int i = 0; i < diff; i++) {
                write(pFill);
            }
        }

        return pNewOffset;
    }

    @Override
    public void finish() throws IOException {
        if (haveUnclosedEntry) {
            throw new IOException("This archive contains unclosed entries.");
        }
        if (finished) {
            throw new IOException("This archive has already been finished");
        }
        finished = true;
    }

    @Override
    public void putArchiveEntry(final ArchiveEntry pEntry) throws IOException {
        if (finished) {
            throw new IOException("Stream has already been finished");
        }

        final ArArchiveEntry pArEntry = (ArArchiveEntry)pEntry;
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

        prevEntry = pArEntry;

        writeEntryHeader(pArEntry);

        entryOffset = 0;
        haveUnclosedEntry = true;
    }

    /**
     * Set the long file mode.
     * This can be LONGFILE_ERROR(0) or LONGFILE_BSD(1).
     * This specifies the treatment of long file names (names &gt;= 16).
     * Default is LONGFILE_ERROR.
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
        final byte [] header = ArchiveUtils.toAsciiBytes(ArArchiveEntry.HEADER);
        out.write(header);
    }

    private void writeEntryHeader(final ArArchiveEntry pEntry) throws IOException {

        long offset = 0;
        boolean mustAppendName = false;

        final String n = pEntry.getName();
        final int nLength = n.length();
        if (LONGFILE_ERROR == longFileMode && nLength > 16) {
            throw new IOException("File name too long, > 16 chars: "+n);
        }
        if (LONGFILE_BSD == longFileMode &&
            (nLength > 16 || n.contains(" "))) {
            mustAppendName = true;
            offset += write(ArArchiveInputStream.BSD_LONGNAME_PREFIX + nLength);
        } else {
            offset += write(n);
        }

        offset = fill(offset, 16, ' ');
        final String m = "" + pEntry.getLastModified();
        if (m.length() > 12) {
            throw new IOException("Last modified too long");
        }
        offset += write(m);

        offset = fill(offset, 28, ' ');
        final String u = "" + pEntry.getUserId();
        if (u.length() > 6) {
            throw new IOException("User id too long");
        }
        offset += write(u);

        offset = fill(offset, 34, ' ');
        final String g = "" + pEntry.getGroupId();
        if (g.length() > 6) {
            throw new IOException("Group id too long");
        }
        offset += write(g);

        offset = fill(offset, 40, ' ');
        final String fm = "" + Integer.toString(pEntry.getMode(), 8);
        if (fm.length() > 8) {
            throw new IOException("Filemode too long");
        }
        offset += write(fm);

        offset = fill(offset, 48, ' ');
        final String s =
            String.valueOf(pEntry.getLength()
                           + (mustAppendName ? nLength : 0));
        if (s.length() > 10) {
            throw new IOException("Size too long");
        }
        offset += write(s);

        offset = fill(offset, 58, ' ');

        offset += write(ArArchiveEntry.TRAILER);

        if (mustAppendName) {
            offset += write(n);
        }

    }
}
