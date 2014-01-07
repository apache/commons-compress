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

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.apache.commons.compress2.archivers.ArchiveEntryParameters;
import org.apache.commons.compress2.archivers.OwnerInformation;
import org.apache.commons.compress2.archivers.spi.AbstractArchiveInput;

/**
 * Implements the "ar" archive format.
 * 
 * @NotThreadSafe
 * 
 */
public class ArArchiveInput extends AbstractArchiveInput<ArArchiveEntry> {

    private final WrappedStream wrappedStream;
    private long offset = 0;
    private boolean closed;

    /*
     * If next has been called, the entry metadata is stored in
     * currentEntry.
     */
    private ArArchiveEntry currentEntry = null;

    // Storage area for extra long names (GNU ar)
    private byte[] namebuffer = null;

    /*
     * The offset where the current entry started. -1 if no entry has been
     * called
     */
    private long entryOffset = -1;

    // cached buffers - must only be used locally in the class (COMPRESS-172 - reduce garbage collection)
    private final byte[] NAME_BUF = new byte[16];
    private final byte[] LAST_MODIFIED_BUF = new byte[12];
    private final byte[] ID_BUF = new byte[6];
    private final byte[] FILE_MODE_BUF = new byte[8];
    private final byte[] LENGTH_BUF = new byte[10];

    /**
     * Constructs an Ar input with the referenced channel
     * 
     * @param pInput
     *            the ar input
     */
    public ArArchiveInput(final ReadableByteChannel pInput) {
        wrappedStream = new WrappedStream(Channels.newInputStream(pInput));
        closed = false;
    }

    /**
     * Returns the next AR entry in this stream.
     * 
     * @return the next AR entry.
     * @throws IOException
     *             if the entry could not be read
     */
    @Override
    public ArArchiveEntry next() throws IOException {
        if (currentEntry != null) {
            final long entryEnd = entryOffset + currentEntry.getSize();
            IOUtils.skip(wrappedStream, entryEnd - offset);
            currentEntry = null;
        }

        if (offset == 0) {
            final byte[] expected = StandardCharsets.US_ASCII.encode(ArArchiveEntry.HEADER).array();
            final byte[] realized = new byte[expected.length];
            final int read = IOUtils.readFully(wrappedStream, realized);
            if (read != expected.length) {
                throw new IOException("failed to read header. Occured at byte: " + getBytesRead());
            }
            for (int i = 0; i < expected.length; i++) {
                if (expected[i] != realized[i]) {
                    throw new IOException("invalid header " + toAsciiString(realized));
                }
            }
        }

        if (offset % 2 != 0 && wrappedStream.read() < 0) {
            // hit eof
            return null;
        }

        if (wrappedStream.available() == 0) {
            return null;
        }

        IOUtils.readFully(wrappedStream, NAME_BUF);
        IOUtils.readFully(wrappedStream, LAST_MODIFIED_BUF);
        IOUtils.readFully(wrappedStream, ID_BUF);
        int userId = asInt(ID_BUF, true);
        IOUtils.readFully(wrappedStream, ID_BUF);
        IOUtils.readFully(wrappedStream, FILE_MODE_BUF);
        IOUtils.readFully(wrappedStream, LENGTH_BUF);

        {
            final byte[] expected = StandardCharsets.US_ASCII.encode(ArArchiveEntry.TRAILER).array();
            final byte[] realized = new byte[expected.length];
            final int read = IOUtils.readFully(wrappedStream, realized);
            if (read != expected.length) {
                throw new IOException("failed to read entry trailer. Occured at byte: " + getBytesRead());
            }
            for (int i = 0; i < expected.length; i++) {
                if (expected[i] != realized[i]) {
                    throw new IOException("invalid entry trailer. not read the content? Occured at byte: " + getBytesRead());
                }
            }
        }

        entryOffset = offset;

//        GNU ar uses a '/' to mark the end of the filename; this allows for the use of spaces without the use of an extended filename.

        // entry name is stored as ASCII string
        String temp = toAsciiString(NAME_BUF).trim();
        if (isGNUStringTable(temp)) { // GNU extended filenames entry
            currentEntry = readGNUStringTable(LENGTH_BUF);
            return next();
        }

        long len = asLong(LENGTH_BUF);
        if (temp.endsWith("/")) { // GNU terminator
            temp = temp.substring(0, temp.length() - 1);
        } else if (isGNULongName(temp)) {
            int off = Integer.parseInt(temp.substring(1));// get the offset
            temp = getExtendedName(off); // convert to the long name
        } else if (isBSDLongName(temp)) {
            temp = getBSDLongName(temp);
            // entry length contained the length of the file name in
            // addition to the real length of the entry.
            // assume file name was ASCII, there is no "standard" otherwise
            int nameLen = temp.length();
            len -= nameLen;
            entryOffset += nameLen;
        }

        currentEntry = new ArArchiveEntry(new ArchiveEntryParameters().withName(temp).withSize(len)
                                          .withOwnerInformation(new OwnerInformation(userId, asInt(ID_BUF, true)))
                                          .withLastModifiedDate(new Date(asLong(LAST_MODIFIED_BUF) * 1000)),
                                          asInt(FILE_MODE_BUF, 8));
        return currentEntry;
    }

    /**
     * Get an extended name from the GNU extended name buffer.
     * 
     * @param offset pointer to entry within the buffer
     * @return the extended file name; without trailing "/" if present.
     * @throws IOException if name not found or buffer not set up
     */
    private String getExtendedName(int offset) throws IOException{
        if (namebuffer == null) {
            throw new IOException("Cannot process GNU long filename as no // record was found");
        }
        for(int i=offset; i < namebuffer.length; i++){
            if (namebuffer[i]=='\012'){
                if (namebuffer[i-1]=='/') {
                    i--; // drop trailing /
                }
                return toAsciiString(namebuffer, offset, i-offset);
            }
        }
        throw new IOException("Failed to read entry: "+offset);
    }
    private long asLong(byte[] input) {
        return Long.parseLong(toAsciiString(input).trim());
    }

    private int asInt(byte[] input) {
        return asInt(input, 10, false);
    }

    private int asInt(byte[] input, boolean treatBlankAsZero) {
        return asInt(input, 10, treatBlankAsZero);
    }

    private int asInt(byte[] input, int base) {
        return asInt(input, base, false);
    }

    private int asInt(byte[] input, int base, boolean treatBlankAsZero) {
        String string = toAsciiString(input).trim();
        if (string.length() == 0 && treatBlankAsZero) {
            return 0;
        }
        return Integer.parseInt(string, base);
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            closed = true;
            wrappedStream.close();
        }
        currentEntry = null;
    }

    @Override
    public boolean isOpen() {
        return !closed;
    }

    @Override
    public int read(ByteBuffer b) throws IOException {
        byte[] tmp = new byte[b.remaining()];
        int read = wrappedStream.read(tmp);
        if (read > 0) {
            b.put(tmp, 0, read);
        }
        return read;
    }

    private class WrappedStream extends FilterInputStream {
        private WrappedStream(InputStream i) {
            super(i);
        }

        private InputStream getIn() {
            return in;
        }

    /*
     * (non-Javadoc)
     * 
     * @see java.io.InputStream#read(byte[], int, int)
     */
    @Override
    public int read(byte[] b, final int off, final int len) throws IOException {
        int toRead = len;
        if (currentEntry != null) {
            final long entryEnd = entryOffset + currentEntry.getSize();
            if (len > 0 && entryEnd > offset) {
                toRead = (int) Math.min(len, entryEnd - offset);
            } else {
                return -1;
            }
        }
        final int ret = in.read(b, off, toRead);
        count(ret);
        offset += ret > 0 ? ret : 0;
        return ret;
    }

        private final byte[] SINGLE = new byte[1];
        private static final int BYTE_MASK = 0xFF;

        @Override
        public int read() throws IOException {
            int num = read(SINGLE, 0, 1);
            return num == -1 ? -1 : SINGLE[0] & BYTE_MASK;
        }
    }

    /**
     * Checks if the signature matches ASCII "!&lt;arch&gt;" followed by a single LF
     * control character
     * 
     * @param signature
     *            the bytes to check
     * @param length
     *            the number of bytes to check
     * @return true, if this stream is an Ar archive stream, false otherwise
     */
    public static boolean matches(byte[] signature, int length) {
        // 3c21 7261 6863 0a3e

        if (length < 8) {
            return false;
        }
        if (signature[0] != 0x21) {
            return false;
        }
        if (signature[1] != 0x3c) {
            return false;
        }
        if (signature[2] != 0x61) {
            return false;
        }
        if (signature[3] != 0x72) {
            return false;
        }
        if (signature[4] != 0x63) {
            return false;
        }
        if (signature[5] != 0x68) {
            return false;
        }
        if (signature[6] != 0x3e) {
            return false;
        }
        if (signature[7] != 0x0a) {
            return false;
        }

        return true;
    }

    static final String BSD_LONGNAME_PREFIX = "#1/";
    private static final int BSD_LONGNAME_PREFIX_LEN =
        BSD_LONGNAME_PREFIX.length();
    private static final String BSD_LONGNAME_PATTERN =
        "^" + BSD_LONGNAME_PREFIX + "\\d+";

    /**
     * Does the name look like it is a long name (or a name containing
     * spaces) as encoded by BSD ar?
     *
     * <p>From the FreeBSD ar(5) man page:</p>
     * <pre>
     * BSD   In the BSD variant, names that are shorter than 16
     *       characters and without embedded spaces are stored
     *       directly in this field.  If a name has an embedded
     *       space, or if it is longer than 16 characters, then
     *       the string "#1/" followed by the decimal represen-
     *       tation of the length of the file name is placed in
     *       this field. The actual file name is stored immedi-
     *       ately after the archive header.  The content of the
     *       archive member follows the file name.  The ar_size
     *       field of the header (see below) will then hold the
     *       sum of the size of the file name and the size of
     *       the member.
     * </pre>
     */
    private static boolean isBSDLongName(String name) {
        return name != null && name.matches(BSD_LONGNAME_PATTERN);
    }

    /**
     * Reads the real name from the current stream assuming the very
     * first bytes to be read are the real file name.
     *
     * @see #isBSDLongName
     */
    private String getBSDLongName(String bsdLongName) throws IOException {
        int nameLen =
            Integer.parseInt(bsdLongName.substring(BSD_LONGNAME_PREFIX_LEN));
        byte[] name = new byte[nameLen];
        int read = IOUtils.readFully(wrappedStream.getIn(), name);
        count(read);
        if (read != nameLen) {
            throw new EOFException();
        }
        return toAsciiString(name);
    }

    private static final String GNU_STRING_TABLE_NAME = "//";

    /**
     * Is this the name of the "Archive String Table" as used by
     * SVR4/GNU to store long file names?
     *
     * <p>GNU ar stores multiple extended filenames in the data section
     * of a file with the name "//", this record is referred to by
     * future headers.</p>
     *
     * <p>A header references an extended filename by storing a "/"
     * followed by a decimal offset to the start of the filename in
     * the extended filename data section.</p>
     * 
     * <p>The format of the "//" file itself is simply a list of the
     * long filenames, each separated by one or more LF
     * characters. Note that the decimal offsets are number of
     * characters, not line or string number within the "//" file.</p>
     */
    private static boolean isGNUStringTable(String name) {
        return GNU_STRING_TABLE_NAME.equals(name);
    }

    /**
     * Reads the GNU archive String Table.
     *
     * @see #isGNUStringTable
     */
    private ArArchiveEntry readGNUStringTable(byte[] length) throws IOException {
        int bufflen = asInt(length); // Assume length will fit in an int
        namebuffer = new byte[bufflen];
        int read = wrappedStream.read(namebuffer, 0, bufflen);
        if (read != bufflen){
            throw new IOException("Failed to read complete // record: expected="
                                  + bufflen + " read=" + read);
        }
        return new ArArchiveEntry(new ArchiveEntryParameters().withName(GNU_STRING_TABLE_NAME).withSize(bufflen));
    }

    private static final String GNU_LONGNAME_PATTERN = "^/\\d+";

    /**
     * Does the name look like it is a long name (or a name containing
     * spaces) as encoded by SVR4/GNU ar?
     *
     * @see #isGNUStringTable
     */
    private boolean isGNULongName(String name) {
        return name != null && name.matches(GNU_LONGNAME_PATTERN);
    }

    private static String toAsciiString(byte[] b) {
        return toAsciiString(b, 0, b.length);
    }

    private static String toAsciiString(byte[] b, int offset, int length) {
        return StandardCharsets.US_ASCII.decode(ByteBuffer.wrap(b, offset, length)).toString();
    }
}
