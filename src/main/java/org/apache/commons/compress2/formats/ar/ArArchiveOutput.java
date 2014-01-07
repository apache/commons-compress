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
package org.apache.commons.compress2.formats.ar;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;

import org.apache.commons.compress2.archivers.ArchiveEntryParameters;
import org.apache.commons.compress2.archivers.OwnerInformation;
import org.apache.commons.compress2.archivers.spi.AbstractArchiveOutput;

/**
 * Implements the "ar" archive format.
 * 
 * @NotThreadSafe
 */
public class ArArchiveOutput extends AbstractArchiveOutput<ArArchiveEntry> {
    /** Fail if a long file name is required in the archive. */
    public static final int LONGFILE_ERROR = 0;

    /** BSD ar extensions are used to store long file names in the archive. */
    public static final int LONGFILE_BSD = 1;

    private final WritableByteChannel out;
    private long entryOffset = 0;
    private ArArchiveEntry prevEntry;
    private boolean haveUnclosedEntry = false;
    private int longFileMode = LONGFILE_ERROR;

    /** indicates if this archive is finished */
    private boolean finished = false;

    public ArArchiveOutput(WritableByteChannel pOut) {
        this.out = pOut;
    }

    /**
     * Set the long file mode.
     * This can be LONGFILE_ERROR(0) or LONGFILE_BSD(1).
     * This specifies the treatment of long file names (names &gt;= 16).
     * Default is LONGFILE_ERROR.
     * @param longFileMode the mode to use
     */
    public void setLongFileMode(int longFileMode) {
        this.longFileMode = longFileMode;
    }

    private long writeArchiveHeader() throws IOException {
        ByteBuffer header = StandardCharsets.US_ASCII.encode(ArArchiveEntry.HEADER);
        int len = header.remaining();
        out.write(header);
        count(len);
        return len;
    }

    @Override
    public void closeEntry() throws IOException {
        if (finished) {
            throw new IOException("Stream has already been finished");
        }
        if (prevEntry == null || !haveUnclosedEntry){
            throw new IOException("No current entry to close");
        }
        if (entryOffset % 2 != 0) {
            out.write(ByteBuffer.wrap(new byte[] { '\n' })); // Pad byte
            count(1);
        }
        haveUnclosedEntry = false;
    }

    @Override
    public void putEntry(final ArArchiveEntry entry) throws IOException {
        if (finished) {
            throw new IOException("Stream has already been finished");
        }

        if (prevEntry == null) {
            writeArchiveHeader();
        } else {
            if (prevEntry.getSize() != entryOffset) {
                throw new IOException("length does not match entry (" + prevEntry.getSize() + " != " + entryOffset);
            }

            if (haveUnclosedEntry) {
                closeEntry();
            }
        }

        prevEntry = entry;

        writeEntryHeader(entry);

        entryOffset = 0;
        haveUnclosedEntry = true;
    }

    private long fill( final long pOffset, final long pNewOffset, byte pFill ) throws IOException { 
        final long diff = pNewOffset - pOffset;
        if (diff > Integer.MAX_VALUE) {
            throw new IOException("filling too much");
        }

        if (diff > 0) {
            ByteBuffer b = ByteBuffer.allocate((int) diff);
            for (int i = 0; i < diff; i++) {
                b.put(pFill);
            }
            b.flip();
            write(b);
        }

        return pNewOffset;
    }

    private long write( final String data ) throws IOException {
        return write(StandardCharsets.US_ASCII.encode(data));
    }

    private long writeEntryHeader( final ArArchiveEntry pEntry ) throws IOException {

        long offset = 0;
        boolean mustAppendName = false;

        final String n = pEntry.getName();
        if (LONGFILE_ERROR == longFileMode && n.length() > 16) {
            throw new IOException("filename too long, > 16 chars: "+n);
        }
        if (LONGFILE_BSD == longFileMode && 
            (n.length() > 16 || n.indexOf(" ") > -1)) {
            mustAppendName = true;
            // TODO re-introduce constant
            offset += write("#1/" + String.valueOf(n.length()));
        } else {
            offset += write(n);
        }

        offset = fill(offset, 16, (byte) ' ');
        final String m = "" + (pEntry.getLastModifiedDate().getTime() / 1000);
        if (m.length() > 12) {
            throw new IOException("modified too long");
        }
        offset += write(m);

        offset = fill(offset, 28, (byte) ' ');
        final String u = "" + getUserId(pEntry.getOwnerInformation());
        if (u.length() > 6) {
            throw new IOException("userid too long");
        }
        offset += write(u);

        offset = fill(offset, 34, (byte) ' ');
        final String g = "" + getGroupId(pEntry.getOwnerInformation());
        if (g.length() > 6) {
            throw new IOException("groupid too long");
        }
        offset += write(g);

        offset = fill(offset, 40, (byte) ' ');
        final String fm = "" + Integer.toString(pEntry.getMode(), 8);
        if (fm.length() > 8) {
            throw new IOException("filemode too long");
        }
        offset += write(fm);

        offset = fill(offset, 48, (byte) ' ');
        final String s =
            String.valueOf(pEntry.getSize()
                           + (mustAppendName ? n.length() : 0));
        if (s.length() > 10) {
            throw new IOException("size too long");
        }
        offset += write(s);

        offset = fill(offset, 58, (byte) ' ');

        offset += write(ArArchiveEntry.TRAILER);

        if (mustAppendName) {
            offset += write(n);
        }

        return offset;
    }

    @Override
    public int write(ByteBuffer b) throws IOException {
        int len = out.write(b);
        count(len);
        entryOffset += len;
        return len;
    }

    /**
     * Calls finish if necessary, and then closes the nested Channel
     */
    @Override
    public void close() throws IOException {
        if (!finished) {
            finish();
        }
        out.close();
        prevEntry = null;
    }

    @Override
    public ArArchiveEntry createEntry(ArchiveEntryParameters params) {
        return new ArArchiveEntry(params);
    }

    @Override
    public void finish() throws IOException {
        if (haveUnclosedEntry) {
            throw new IOException("This archive contains unclosed entries.");
        } else if(finished) {
            throw new IOException("This archive has already been finished");
        }
        finished = true;
    }

    @Override
    public boolean isOpen() {
        return out.isOpen();
    }

    private int getUserId(OwnerInformation info) {
        return info == null ? 0 : info.getUserId();
    }

    private int getGroupId(OwnerInformation info) {
        return info == null ? 0 : info.getGroupId();
    }
}
