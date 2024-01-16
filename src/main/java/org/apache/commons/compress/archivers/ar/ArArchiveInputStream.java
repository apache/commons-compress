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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.regex.Pattern;

import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.utils.ArchiveUtils;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.compress.utils.ParsingUtils;

/**
 * Implements the "ar" archive format as an input stream.
 *
 * @NotThreadSafe
 */
public class ArArchiveInputStream extends ArchiveInputStream<ArArchiveEntry> {

    // offsets and length of meta data parts
    private static final int NAME_OFFSET = 0;
    private static final int NAME_LEN = 16;
    private static final int LAST_MODIFIED_OFFSET = NAME_LEN;

    private static final int LAST_MODIFIED_LEN = 12;

    private static final int USER_ID_OFFSET = LAST_MODIFIED_OFFSET + LAST_MODIFIED_LEN;

    private static final int USER_ID_LEN = 6;

    private static final int GROUP_ID_OFFSET = USER_ID_OFFSET + USER_ID_LEN;
    private static final int GROUP_ID_LEN = 6;
    private static final int FILE_MODE_OFFSET = GROUP_ID_OFFSET + GROUP_ID_LEN;
    private static final int FILE_MODE_LEN = 8;
    private static final int LENGTH_OFFSET = FILE_MODE_OFFSET + FILE_MODE_LEN;
    private static final int LENGTH_LEN = 10;
    static final String BSD_LONGNAME_PREFIX = "#1/";
    private static final int BSD_LONGNAME_PREFIX_LEN = BSD_LONGNAME_PREFIX.length();
    private static final Pattern BSD_LONGNAME_PATTERN = Pattern.compile("^" + BSD_LONGNAME_PREFIX + "\\d+");
    private static final String GNU_STRING_TABLE_NAME = "//";
    private static final Pattern GNU_LONGNAME_PATTERN = Pattern.compile("^/\\d+");

    /**
     * Does the name look like it is a long name (or a name containing spaces) as encoded by BSD ar?
     * <p>
     * From the FreeBSD ar(5) man page:
     * </p>
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
     *
     * @since 1.3
     */
    private static boolean isBSDLongName(final String name) {
        return name != null && BSD_LONGNAME_PATTERN.matcher(name).matches();
    }

    /**
     * Is this the name of the "Archive String Table" as used by SVR4/GNU to store long file names?
     * <p>
     * GNU ar stores multiple extended file names in the data section of a file with the name "//", this record is referred to by future headers.
     * </p>
     * <p>
     * A header references an extended file name by storing a "/" followed by a decimal offset to the start of the file name in the extended file name data
     * section.
     * </p>
     * <p>
     * The format of the "//" file itself is simply a list of the long file names, each separated by one or more LF characters. Note that the decimal offsets
     * are number of characters, not line or string number within the "//" file.
     * </p>
     */
    private static boolean isGNUStringTable(final String name) {
        return GNU_STRING_TABLE_NAME.equals(name);
    }

    /**
     * Checks if the signature matches ASCII "!&lt;arch&gt;" followed by a single LF control character
     *
     * @param signature the bytes to check
     * @param length    the number of bytes to check
     * @return true, if this stream is an Ar archive stream, false otherwise
     */
    public static boolean matches(final byte[] signature, final int length) {
        // 3c21 7261 6863 0a3e

        return length >= 8 && signature[0] == 0x21 && signature[1] == 0x3c && signature[2] == 0x61 && signature[3] == 0x72 && signature[4] == 0x63
                && signature[5] == 0x68 && signature[6] == 0x3e && signature[7] == 0x0a;
    }

    private final InputStream input;

    private long offset;

    private boolean closed;

    /*
     * If getNextEntry has been called, the entry metadata is stored in currentEntry.
     */
    private ArArchiveEntry currentEntry;

    /** Storage area for extra long names (GNU ar). */
    private byte[] namebuffer;

    /**
     * The offset where the current entry started. -1 if no entry has been called
     */
    private long entryOffset = -1;

    /** Cached buffer for meta data - must only be used locally in the class (COMPRESS-172 - reduce garbage collection). */
    private final byte[] metaData = new byte[NAME_LEN + LAST_MODIFIED_LEN + USER_ID_LEN + GROUP_ID_LEN + FILE_MODE_LEN + LENGTH_LEN];

    /**
     * Constructs an Ar input stream with the referenced stream
     *
     * @param inputStream the ar input stream
     */
    public ArArchiveInputStream(final InputStream inputStream) {
        this.input = inputStream;
    }

    private int asInt(final byte[] byteArray, final int offset, final int len) throws IOException {
        return asInt(byteArray, offset, len, 10, false);
    }

    private int asInt(final byte[] byteArray, final int offset, final int len, final boolean treatBlankAsZero) throws IOException {
        return asInt(byteArray, offset, len, 10, treatBlankAsZero);
    }

    private int asInt(final byte[] byteArray, final int offset, final int len, final int base) throws IOException {
        return asInt(byteArray, offset, len, base, false);
    }

    private int asInt(final byte[] byteArray, final int offset, final int len, final int base, final boolean treatBlankAsZero) throws IOException {
        final String string = ArchiveUtils.toAsciiString(byteArray, offset, len).trim();
        if (string.isEmpty() && treatBlankAsZero) {
            return 0;
        }
        return ParsingUtils.parseIntValue(string, base);
    }

    private long asLong(final byte[] byteArray, final int offset, final int len) throws IOException {
        return ParsingUtils.parseLongValue(ArchiveUtils.toAsciiString(byteArray, offset, len).trim());
    }

    /*
     * (non-Javadoc)
     *
     * @see java.io.InputStream#close()
     */
    @Override
    public void close() throws IOException {
        if (!closed) {
            closed = true;
            input.close();
        }
        currentEntry = null;
    }

    /**
     * Reads the real name from the current stream assuming the very first bytes to be read are the real file name.
     *
     * @see #isBSDLongName
     *
     * @since 1.3
     */
    private String getBSDLongName(final String bsdLongName) throws IOException {
        final int nameLen = ParsingUtils.parseIntValue(bsdLongName.substring(BSD_LONGNAME_PREFIX_LEN));
        final byte[] name = IOUtils.readRange(input, nameLen);
        final int read = name.length;
        trackReadBytes(read);
        if (read != nameLen) {
            throw new EOFException();
        }
        return ArchiveUtils.toAsciiString(name);
    }

    /**
     * Gets an extended name from the GNU extended name buffer.
     *
     * @param offset pointer to entry within the buffer
     * @return the extended file name; without trailing "/" if present.
     * @throws IOException if name not found or buffer not set up
     */
    private String getExtendedName(final int offset) throws IOException {
        if (namebuffer == null) {
            throw new IOException("Cannot process GNU long file name as no // record was found");
        }
        for (int i = offset; i < namebuffer.length; i++) {
            if (namebuffer[i] == '\012' || namebuffer[i] == 0) {
                // Avoid array errors
                if (i == 0) {
                    break;
                }
                if (namebuffer[i - 1] == '/') {
                    i--; // drop trailing /
                }

                // Check there is a something to return, otherwise break out of the loop
                if (i - offset > 0) {
                    return ArchiveUtils.toAsciiString(namebuffer, offset, i - offset);
                }
                break;
            }
        }
        throw new IOException("Failed to read entry: " + offset);
    }

    /**
     * Returns the next AR entry in this stream.
     *
     * @return the next AR entry.
     * @throws IOException if the entry could not be read
     * @deprecated Use {@link #getNextEntry()}.
     */
    @Deprecated
    public ArArchiveEntry getNextArEntry() throws IOException {
        if (currentEntry != null) {
            final long entryEnd = entryOffset + currentEntry.getLength();
            final long skipped = org.apache.commons.io.IOUtils.skip(input, entryEnd - offset);
            trackReadBytes(skipped);
            currentEntry = null;
        }

        if (offset == 0) {
            final byte[] expected = ArchiveUtils.toAsciiBytes(ArArchiveEntry.HEADER);
            final byte[] realized = IOUtils.readRange(input, expected.length);
            final int read = realized.length;
            trackReadBytes(read);
            if (read != expected.length) {
                throw new IOException("Failed to read header. Occurred at byte: " + getBytesRead());
            }
            if (!Arrays.equals(expected, realized)) {
                throw new IOException("Invalid header " + ArchiveUtils.toAsciiString(realized));
            }
        }

        if (offset % 2 != 0) {
            if (input.read() < 0) {
                // hit eof
                return null;
            }
            trackReadBytes(1);
        }

        {
            final int read = IOUtils.readFully(input, metaData);
            trackReadBytes(read);
            if (read == 0) {
                return null;
            }
            if (read < metaData.length) {
                throw new IOException("Truncated ar archive");
            }
        }

        {
            final byte[] expected = ArchiveUtils.toAsciiBytes(ArArchiveEntry.TRAILER);
            final byte[] realized = IOUtils.readRange(input, expected.length);
            final int read = realized.length;
            trackReadBytes(read);
            if (read != expected.length) {
                throw new IOException("Failed to read entry trailer. Occurred at byte: " + getBytesRead());
            }
            if (!Arrays.equals(expected, realized)) {
                throw new IOException("Invalid entry trailer. not read the content? Occurred at byte: " + getBytesRead());
            }
        }

        entryOffset = offset;

//        GNU ar uses a '/' to mark the end of the file name; this allows for the use of spaces without the use of an extended file name.

        // entry name is stored as ASCII string
        String temp = ArchiveUtils.toAsciiString(metaData, NAME_OFFSET, NAME_LEN).trim();
        if (isGNUStringTable(temp)) { // GNU extended file names entry
            currentEntry = readGNUStringTable(metaData, LENGTH_OFFSET, LENGTH_LEN);
            return getNextArEntry();
        }

        long len;
        try {
            len = asLong(metaData, LENGTH_OFFSET, LENGTH_LEN);
        } catch (final NumberFormatException ex) {
            throw new IOException("Broken archive, unable to parse ar_size field as a number", ex);
        }
        if (temp.endsWith("/")) { // GNU terminator
            temp = temp.substring(0, temp.length() - 1);
        } else if (isGNULongName(temp)) {
            final int off = ParsingUtils.parseIntValue(temp.substring(1));// get the offset
            temp = getExtendedName(off); // convert to the long name
        } else if (isBSDLongName(temp)) {
            temp = getBSDLongName(temp);
            // entry length contained the length of the file name in
            // addition to the real length of the entry.
            // assume file name was ASCII, there is no "standard" otherwise
            final int nameLen = temp.length();
            len -= nameLen;
            entryOffset += nameLen;
        }

        if (len < 0) {
            throw new IOException("broken archive, entry with negative size");
        }

        try {
            currentEntry = new ArArchiveEntry(temp, len, asInt(metaData, USER_ID_OFFSET, USER_ID_LEN, true),
                    asInt(metaData, GROUP_ID_OFFSET, GROUP_ID_LEN, true), asInt(metaData, FILE_MODE_OFFSET, FILE_MODE_LEN, 8),
                    asLong(metaData, LAST_MODIFIED_OFFSET, LAST_MODIFIED_LEN));
            return currentEntry;
        } catch (final NumberFormatException ex) {
            throw new IOException("Broken archive, unable to parse entry metadata fields as numbers", ex);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.commons.compress.archivers.ArchiveInputStream#getNextEntry()
     */
    @Override
    public ArArchiveEntry getNextEntry() throws IOException {
        return getNextArEntry();
    }

    /**
     * Does the name look like it is a long name (or a name containing spaces) as encoded by SVR4/GNU ar?
     *
     * @see #isGNUStringTable
     */
    private boolean isGNULongName(final String name) {
        return name != null && GNU_LONGNAME_PATTERN.matcher(name).matches();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.io.InputStream#read(byte[], int, int)
     */
    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        if (len == 0) {
            return 0;
        }
        if (currentEntry == null) {
            throw new IllegalStateException("No current ar entry");
        }
        final long entryEnd = entryOffset + currentEntry.getLength();
        if (len < 0 || offset >= entryEnd) {
            return -1;
        }
        final int toRead = (int) Math.min(len, entryEnd - offset);
        final int ret = this.input.read(b, off, toRead);
        trackReadBytes(ret);
        return ret;
    }

    /**
     * Reads the GNU archive String Table.
     *
     * @see #isGNUStringTable
     */
    private ArArchiveEntry readGNUStringTable(final byte[] length, final int offset, final int len) throws IOException {
        int bufflen;
        try {
            bufflen = asInt(length, offset, len); // Assume length will fit in an int
        } catch (final NumberFormatException ex) {
            throw new IOException("Broken archive, unable to parse GNU string table length field as a number", ex);
        }

        namebuffer = IOUtils.readRange(input, bufflen);
        final int read = namebuffer.length;
        trackReadBytes(read);
        if (read != bufflen) {
            throw new IOException("Failed to read complete // record: expected=" + bufflen + " read=" + read);
        }
        return new ArArchiveEntry(GNU_STRING_TABLE_NAME, bufflen);
    }

    private void trackReadBytes(final long read) {
        count(read);
        if (read > 0) {
            offset += read;
        }
    }
}
