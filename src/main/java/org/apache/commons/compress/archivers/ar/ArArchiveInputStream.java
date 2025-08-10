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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.Pattern;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.utils.ArchiveUtils;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.compress.utils.ParsingUtils;
import org.apache.commons.lang3.ArrayUtils;

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
    private static boolean isGNUStringTable(final ArArchiveEntry entry) {
        return GNU_STRING_TABLE_NAME.equals(entry.getName());
    }

    /**
     * Checks if the signature matches ASCII "!&lt;arch&gt;" followed by a single LF control character
     *
     * @param buffer  the bytes to check.
     * @param ignored ignored.
     * @return true, if this stream is an Ar archive stream, false otherwise.
     */
    public static boolean matches(final byte[] buffer, final int ignored) {
        return ArrayUtils.startsWith(buffer, ArArchiveEntry.HEADER_BYTES);
    }

    private boolean closed;

    /*
     * If getNextEntry has been called, the entry metadata is stored in currentEntry.
     */
    private ArArchiveEntry currentEntry;

    /** Storage area for extra long names (GNU ar). */
    private byte[] namebuffer;

    /**
     * The offset where the data for the current entry starts.
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
        super(inputStream, StandardCharsets.US_ASCII.name());
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

    /**
     * Checks and skips the trailer of the current entry.
     *
     * @throws IOException if the trailer is invalid or not read correctly.
     */
    private void checkTrailer() throws IOException {
        // Check and skip the record trailer
        final byte[] expectedTrailer = ArchiveUtils.toAsciiBytes(ArArchiveEntry.TRAILER);
        final byte[] actualTrailer = IOUtils.readRange(in, expectedTrailer.length);
        if (actualTrailer.length < expectedTrailer.length) {
            throw new EOFException(String.format(
                    "Premature end of ar archive: invalid or incomplete trailer for entry '%s'.",
                    ArchiveUtils.toAsciiString(metaData, NAME_OFFSET, NAME_LEN).trim()));
        }
        count(actualTrailer.length);
        if (!Arrays.equals(expectedTrailer, actualTrailer)) {
            throw new ArchiveException("Invalid ar archive entry trailer: " + ArchiveUtils.toAsciiString(actualTrailer));
        }
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
            in.close();
        }
        currentEntry = null;
    }

    /**
     * Reads the real name from the current stream assuming the very first bytes to be read are the real file name.
     *
     * @see #isBSDLongName
     * @since 1.3
     */
    private String getBSDLongName(final String bsdLongName) throws IOException {
        final int nameLen = ParsingUtils.parseIntValue(bsdLongName.substring(BSD_LONGNAME_PREFIX_LEN));
        final byte[] name = IOUtils.readRange(in, nameLen);
        final int read = name.length;
        count(read);
        if (read != nameLen) {
            throw new EOFException(bsdLongName);
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
            throw new ArchiveException("Cannot process GNU long file name as no GNU string table was found");
        }
        if (offset >= namebuffer.length) {
            throw new ArchiveException("GNU long file name offset out of range: " + offset);
        }
        for (int i = offset; i < namebuffer.length; i++) {
            final byte c = namebuffer[i];
            if (c == '\n' || c == 0) {
                if (i > offset && namebuffer[i - 1] == '/') {
                    i--; // drop trailing '/'
                }
                // Check there is a something to return, otherwise break out of the loop
                if (i > offset) {
                    return ArchiveUtils.toAsciiString(namebuffer, offset, i - offset);
                }
                break;
            }
        }
        throw new ArchiveException("Failed to read GNU long file name at offset " + offset);
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
        return getNextEntry();
    }

    /*
     * Returns the next AR file entry in this stream.
     * <p>
     *    The method skips special AR file entries, such as those used by GNU.
     * </p>
     * @return The next AR file entry.
     * @throws IOException if the entry could not be read or is malformed.
     */
    @Override
    public ArArchiveEntry getNextEntry() throws IOException {
        skipGlobalSignature();

        // Handle special GNU ar entries
        boolean foundGNUStringTable = false;
        do {
            // If there is a current entry, skip any unread data and padding
            if (currentEntry != null) {
                IOUtils.skip(this, Long.MAX_VALUE); // Skip to end of current entry
                skipRecordPadding(); // Skip padding to align to the next record
            }

            // Read the next header record
            final byte[] headerBuf = getRecord();
            if (headerBuf == null) {
                // If we encounter a GNU string table but no subsequent file member, the archive is malformed.
                // GNU does not document the ordering of the GNU string table, but the FreeBSD ar(5) manual does:
                //
                //   "If present, this member immediately follows the archive symbol table if an archive symbol
                //    table is present, or is the first member otherwise."
                //
                // Reference: https://man.freebsd.org/cgi/man.cgi?query=ar&sektion=5
                if (foundGNUStringTable) {
                    throw new EOFException("Premature end of ar archive: no regular entry after GNU string table.");
                }
                currentEntry = null;
                return null; // End of archive
            }
            checkTrailer();

            // Parse the header into a new entry
            currentEntry = parseEntry(headerBuf);
            entryOffset = getBytesRead(); // Store the offset of the entry

            foundGNUStringTable = isGNUStringTable(currentEntry);
            if (foundGNUStringTable) {
                // If this is a GNU string table entry, read the extended names and continue
                namebuffer = readGNUStringTable(currentEntry);
            }
        } while (foundGNUStringTable);

        // Handle long file names and other special cases
        String name = currentEntry.getName();
        long len = currentEntry.getLength();
        // Handle GNU ar: names ending with '/' are terminated (allows spaces in names)
        if (name.endsWith("/")) {
            name = name.substring(0, name.length() - 1);
        } else if (isGNULongName(name)) {
            // GNU ar: name is a reference to the string table (e.g., "/42"), resolve the actual name
            final int off = ParsingUtils.parseIntValue(name.substring(1));
            name = getExtendedName(off);
        } else if (isBSDLongName(name)) {
            // BSD ar: name is stored after the header, retrieve it
            name = getBSDLongName(name);
            // The entry length includes the file name length; adjust to get the actual file data length
            final int nameLen = name.length();
            if (nameLen > len) {
                throw new ArchiveException(
                        "Invalid BSD long name: file name length (" + nameLen + ") exceeds entry length (" + len + ")");
            }
            len -= nameLen;
            entryOffset += nameLen;
        }

        currentEntry = new ArArchiveEntry(name, len, currentEntry.getUserId(), currentEntry.getGroupId(),
                currentEntry.getMode(), currentEntry.getLastModified());
        return currentEntry;
    }

    /**
     * Reads the next raw record from the input stream.
     * <p>
     *   The record is expected to be of a fixed size defined by the AR format.
     * </p>
     *
     * @return the byte array containing the record data, or null if the end of the stream is reached.
     * @throws IOException if an I/O error occurs while reading the stream or if the record is malformed.
     */
    private byte[] getRecord() throws IOException {
        final int read = IOUtils.readFully(in, metaData);
        count(read);
        if (read == 0) {
            return null;
        }
        if (read < metaData.length) {
            throw new EOFException(String.format(
                    "Premature end of ar archive: incomplete entry header (expected %d bytes, got %d).",
                    metaData.length, read));
        }
        return metaData;
    }

    /**
     * Does the name look like it is a long name (or a name containing spaces) as encoded by SVR4/GNU ar?
     *
     * @see #isGNUStringTable
     */
    private boolean isGNULongName(final String name) {
        return name != null && GNU_LONGNAME_PATTERN.matcher(name).matches();
    }

    /**
     * Parses the entry metadata from the provided raw record.
     *
     * @param headerBuf the buffer containing the entry metadata.
     * @return an {@link ArArchiveEntry} object containing the parsed metadata.
     * @throws IOException if the metadata cannot be parsed correctly.
     */
    private ArArchiveEntry parseEntry(final byte[] headerBuf) throws IOException {
        // Parse the entry metadata from the header buffer
        try {
            final String name =
                    ArchiveUtils.toAsciiString(headerBuf, NAME_OFFSET, NAME_LEN).trim();
            final long length = asLong(headerBuf, LENGTH_OFFSET, LENGTH_LEN);
            // The remaining fields in the GNU string table entry are not used and may be blank.
            if (GNU_STRING_TABLE_NAME.equals(name)) {
                return new ArArchiveEntry(name, length);
            }
            final int userId = asInt(metaData, USER_ID_OFFSET, USER_ID_LEN, true);
            final int groupId = asInt(metaData, GROUP_ID_OFFSET, GROUP_ID_LEN, true);
            final int mode = asInt(metaData, FILE_MODE_OFFSET, FILE_MODE_LEN, 8);
            final long lastModified = asLong(metaData, LAST_MODIFIED_OFFSET, LAST_MODIFIED_LEN);
            return new ArArchiveEntry(name, length, userId, groupId, mode, lastModified);
        } catch (final IllegalArgumentException e) {
            throw new ArchiveException("Broken archive, entry with negative size", (Throwable) e);
        } catch (final IOException e) {
            throw new ArchiveException("Failed to parse ar entry.", (Throwable) e);
        }
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
        final long offset = getBytesRead();
        if (len < 0 || offset >= entryEnd) {
            return -1;
        }
        final int toRead = ArchiveException.toIntExact(Math.min(len, entryEnd - offset));
        final int ret = in.read(b, off, toRead);
        if (ret < 0) {
            throw new EOFException(String.format(
                    "Premature end of ar archive: entry '%s' is truncated or incomplete.", currentEntry.getName()));
        }
        count(ret);
        return ret;
    }

    /**
     * Reads the GNU archive String Table.
     *
     * @see #isGNUStringTable
     */
    private byte[] readGNUStringTable(final ArArchiveEntry entry) throws IOException {
        if (entry.getLength() > Integer.MAX_VALUE) {
            throw new ArchiveException("Invalid GNU string table entry size: " + entry.getLength());
        }
        final int size = (int) entry.getLength();
        final byte[] namebuffer = IOUtils.readRange(in, size);
        final int read = namebuffer.length;
        if (read < size) {
            throw new EOFException("Premature end of ar archive: truncated or incomplete GNU string table.");
        }
        count(read);
        return namebuffer;
    }

    /**
     * Skips the global archive signature if at the beginning of the stream.
     *
     * @throws IOException if an I/O error occurs while reading the stream or if the signature is invalid.
     */
    private void skipGlobalSignature() throws IOException {
        final long offset = getBytesRead();
        if (offset == 0) {
            final byte[] expectedMagic = ArArchiveEntry.HEADER_BYTES;
            final byte[] actualMagic = IOUtils.readRange(in, expectedMagic.length);
            count(actualMagic.length);
            if (expectedMagic.length != actualMagic.length) {
                throw new EOFException(String.format(
                        "Premature end of ar archive: incomplete global header (expected %d bytes, got %d).",
                        expectedMagic.length, actualMagic.length));
            }
            if (!Arrays.equals(expectedMagic, actualMagic)) {
                throw new ArchiveException(
                        "Invalid global ar archive header: " + ArchiveUtils.toAsciiString(actualMagic));
            }
        }
    }

    /**
     * Skips the padding bytes at the end of each record.
     * <p>
     * The AR format requires that each record is padded to an even number of bytes, so if the current offset is odd,
     * we skip one byte.
     * </p>
     *
     * @throws IOException if an I/O error occurs while reading the stream.
     */
    private void skipRecordPadding() throws IOException {
        // If the offset is odd, we need to skip one byte
        final long offset = getBytesRead();
        if (offset % 2 != 0) {
            final int c = in.read();
            if (c < 0) {
                throw new EOFException(String.format(
                        "Premature end of ar archive: missing padding for entry '%s'.", currentEntry.getName()));
            }
            count(1);
        }
    }
}
