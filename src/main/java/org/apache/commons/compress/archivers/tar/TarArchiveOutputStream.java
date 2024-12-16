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
package org.apache.commons.compress.archivers.tar;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipEncoding;
import org.apache.commons.compress.archivers.zip.ZipEncodingHelper;
import org.apache.commons.compress.utils.FixedLengthBlockOutputStream;
import org.apache.commons.compress.utils.TimeUtils;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.file.attribute.FileTimes;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.lang3.ArrayFill;

/**
 * The TarOutputStream writes a Unix tar archive as an OutputStream. Methods are provided to put entries, and then write their contents by writing to this
 * stream using write().
 *
 * <p>
 * tar archives consist of a sequence of records of 512 bytes each that are grouped into blocks. Prior to Apache Commons Compress 1.14 it has been possible to
 * configure a record size different from 512 bytes and arbitrary block sizes. Starting with Compress 1.15 512 is the only valid option for the record size and
 * the block size must be a multiple of 512. Also the default block size changed from 10240 bytes prior to Compress 1.15 to 512 bytes with Compress 1.15.
 * </p>
 *
 * @NotThreadSafe
 */
public class TarArchiveOutputStream extends ArchiveOutputStream<TarArchiveEntry> {

    /**
     * Fail if a long file name is required in the archive.
     */
    public static final int LONGFILE_ERROR = 0;

    /**
     * Long paths will be truncated in the archive.
     */
    public static final int LONGFILE_TRUNCATE = 1;

    /**
     * GNU tar extensions are used to store long file names in the archive.
     */
    public static final int LONGFILE_GNU = 2;

    /**
     * POSIX/PAX extensions are used to store long file names in the archive.
     */
    public static final int LONGFILE_POSIX = 3;

    /**
     * Fail if a big number (e.g. size &gt; 8GiB) is required in the archive.
     */
    public static final int BIGNUMBER_ERROR = 0;

    /**
     * star/GNU tar/BSD tar extensions are used to store big number in the archive.
     */
    public static final int BIGNUMBER_STAR = 1;

    /**
     * POSIX/PAX extensions are used to store big numbers in the archive.
     */
    public static final int BIGNUMBER_POSIX = 2;
    private static final int RECORD_SIZE = 512;

    private static final ZipEncoding ASCII = ZipEncodingHelper.getZipEncoding(StandardCharsets.US_ASCII);

    private static final int BLOCK_SIZE_UNSPECIFIED = -511;
    private long currSize;
    private String currName;
    private long currBytes;
    private final byte[] recordBuf;
    private int longFileMode = LONGFILE_ERROR;
    private int bigNumberMode = BIGNUMBER_ERROR;

    private long recordsWritten;

    private final int recordsPerBlock;

    /**
     * Indicates if putArchiveEntry has been called without closeArchiveEntry
     */
    private boolean haveUnclosedEntry;

    private final CountingOutputStream countingOut;

    private final ZipEncoding zipEncoding;

    /**
     * The provided encoding (for unit tests).
     */
    final String charsetName;

    private boolean addPaxHeadersForNonAsciiNames;

    /**
     * Constructs a new instance.
     *
     * <p>
     * Uses a block size of 512 bytes.
     * </p>
     *
     * @param os the output stream to use
     */
    public TarArchiveOutputStream(final OutputStream os) {
        this(os, BLOCK_SIZE_UNSPECIFIED);
    }

    /**
     * Constructs a new instance.
     *
     * @param os        the output stream to use
     * @param blockSize the block size to use. Must be a multiple of 512 bytes.
     */
    public TarArchiveOutputStream(final OutputStream os, final int blockSize) {
        this(os, blockSize, null);
    }

    /**
     * Constructs a new instance.
     *
     * @param os         the output stream to use
     * @param blockSize  the block size to use
     * @param recordSize the record size to use. Must be 512 bytes.
     * @deprecated recordSize must always be 512 bytes. An IllegalArgumentException will be thrown if any other value is used
     */
    @Deprecated
    public TarArchiveOutputStream(final OutputStream os, final int blockSize, final int recordSize) {
        this(os, blockSize, recordSize, null);
    }

    /**
     * Constructs a new instance.
     *
     * @param os         the output stream to use
     * @param blockSize  the block size to use . Must be a multiple of 512 bytes.
     * @param recordSize the record size to use. Must be 512 bytes.
     * @param encoding   name of the encoding to use for file names
     * @since 1.4
     * @deprecated recordSize must always be 512 bytes. An IllegalArgumentException will be thrown if any other value is used.
     */
    @Deprecated
    public TarArchiveOutputStream(final OutputStream os, final int blockSize, final int recordSize, final String encoding) {
        this(os, blockSize, encoding);
        if (recordSize != RECORD_SIZE) {
            throw new IllegalArgumentException("Tar record size must always be 512 bytes. Attempt to set size of " + recordSize);
        }

    }

    /**
     * Constructs a new instance.
     *
     * @param os        the output stream to use
     * @param blockSize the block size to use. Must be a multiple of 512 bytes.
     * @param encoding  name of the encoding to use for file names
     * @since 1.4
     */
    public TarArchiveOutputStream(final OutputStream os, final int blockSize, final String encoding) {
        super(os);
        final int realBlockSize;
        if (BLOCK_SIZE_UNSPECIFIED == blockSize) {
            realBlockSize = RECORD_SIZE;
        } else {
            realBlockSize = blockSize;
        }

        if (realBlockSize <= 0 || realBlockSize % RECORD_SIZE != 0) {
            throw new IllegalArgumentException("Block size must be a multiple of 512 bytes. Attempt to use set size of " + blockSize);
        }
        this.out = new FixedLengthBlockOutputStream(countingOut = new CountingOutputStream(os), RECORD_SIZE);
        this.charsetName = Charsets.toCharset(encoding).name();
        this.zipEncoding = ZipEncodingHelper.getZipEncoding(encoding);

        this.recordBuf = new byte[RECORD_SIZE];
        this.recordsPerBlock = realBlockSize / RECORD_SIZE;
    }

    /**
     * Constructs a new instance.
     *
     * <p>
     * Uses a block size of 512 bytes.
     * </p>
     *
     * @param os       the output stream to use
     * @param encoding name of the encoding to use for file names
     * @since 1.4
     */
    public TarArchiveOutputStream(final OutputStream os, final String encoding) {
        this(os, BLOCK_SIZE_UNSPECIFIED, encoding);
    }

    private void addFileTimePaxHeader(final Map<String, String> paxHeaders, final String header, final FileTime value) {
        if (value != null) {
            final Instant instant = value.toInstant();
            final long seconds = instant.getEpochSecond();
            final int nanos = instant.getNano();
            if (nanos == 0) {
                paxHeaders.put(header, String.valueOf(seconds));
            } else {
                addInstantPaxHeader(paxHeaders, header, seconds, nanos);
            }
        }
    }

    private void addFileTimePaxHeaderForBigNumber(final Map<String, String> paxHeaders, final String header, final FileTime value, final long maxValue) {
        if (value != null) {
            final Instant instant = value.toInstant();
            final long seconds = instant.getEpochSecond();
            final int nanos = instant.getNano();
            if (nanos == 0) {
                addPaxHeaderForBigNumber(paxHeaders, header, seconds, maxValue);
            } else {
                addInstantPaxHeader(paxHeaders, header, seconds, nanos);
            }
        }
    }

    private void addInstantPaxHeader(final Map<String, String> paxHeaders, final String header, final long seconds, final int nanos) {
        final BigDecimal bdSeconds = BigDecimal.valueOf(seconds);
        final BigDecimal bdNanos = BigDecimal.valueOf(nanos).movePointLeft(9).setScale(7, RoundingMode.DOWN);
        final BigDecimal timestamp = bdSeconds.add(bdNanos);
        paxHeaders.put(header, timestamp.toPlainString());
    }

    private void addPaxHeaderForBigNumber(final Map<String, String> paxHeaders, final String header, final long value, final long maxValue) {
        if (value < 0 || value > maxValue) {
            paxHeaders.put(header, String.valueOf(value));
        }
    }

    private void addPaxHeadersForBigNumbers(final Map<String, String> paxHeaders, final TarArchiveEntry entry) {
        addPaxHeaderForBigNumber(paxHeaders, "size", entry.getSize(), TarConstants.MAXSIZE);
        addPaxHeaderForBigNumber(paxHeaders, "gid", entry.getLongGroupId(), TarConstants.MAXID);
        addFileTimePaxHeaderForBigNumber(paxHeaders, "mtime", entry.getLastModifiedTime(), TarConstants.MAXSIZE);
        addFileTimePaxHeader(paxHeaders, "atime", entry.getLastAccessTime());
        if (entry.getStatusChangeTime() != null) {
            addFileTimePaxHeader(paxHeaders, "ctime", entry.getStatusChangeTime());
        } else {
            // ctime is usually set from creation time on platforms where the real ctime is not available
            addFileTimePaxHeader(paxHeaders, "ctime", entry.getCreationTime());
        }
        addPaxHeaderForBigNumber(paxHeaders, "uid", entry.getLongUserId(), TarConstants.MAXID);
        // libarchive extensions
        addFileTimePaxHeader(paxHeaders, "LIBARCHIVE.creationtime", entry.getCreationTime());
        // star extensions by Jörg Schilling
        addPaxHeaderForBigNumber(paxHeaders, "SCHILY.devmajor", entry.getDevMajor(), TarConstants.MAXID);
        addPaxHeaderForBigNumber(paxHeaders, "SCHILY.devminor", entry.getDevMinor(), TarConstants.MAXID);
        // there is no PAX header for file mode
        failForBigNumber("mode", entry.getMode(), TarConstants.MAXID);
    }

    /**
     * Closes the underlying OutputStream.
     *
     * @throws IOException on error
     */
    @Override
    public void close() throws IOException {
        try {
            if (!isFinished()) {
                finish();
            }
        } finally {
            super.close();
        }
    }

    /**
     * Closes an entry. This method MUST be called for all file entries that contain data. The reason is that we must buffer data written to the stream in order
     * to satisfy the buffer's record based writes. Thus, there may be data fragments still being assembled that must be written to the output stream before
     * this entry is closed and the next entry written.
     *
     * @throws IOException on error
     */
    @Override
    public void closeArchiveEntry() throws IOException {
        checkFinished();
        if (!haveUnclosedEntry) {
            throw new IOException("No current entry to close");
        }
        ((FixedLengthBlockOutputStream) out).flushBlock();
        if (currBytes < currSize) {
            throw new IOException(
                    "Entry '" + currName + "' closed at '" + currBytes + "' before the '" + currSize + "' bytes specified in the header were written");
        }
        recordsWritten += currSize / RECORD_SIZE;

        if (0 != currSize % RECORD_SIZE) {
            recordsWritten++;
        }
        haveUnclosedEntry = false;
    }

    @Override
    public TarArchiveEntry createArchiveEntry(final File inputFile, final String entryName) throws IOException {
        checkFinished();
        return new TarArchiveEntry(inputFile, entryName);
    }

    @Override
    public TarArchiveEntry createArchiveEntry(final Path inputPath, final String entryName, final LinkOption... options) throws IOException {
        checkFinished();
        return new TarArchiveEntry(inputPath, entryName, options);
    }

    private byte[] encodeExtendedPaxHeadersContents(final Map<String, String> headers) {
        final StringWriter w = new StringWriter();
        headers.forEach((k, v) -> {
            int len = k.length() + v.length() + 3 /* blank, equals and newline */
                    + 2 /* guess 9 < actual length < 100 */;
            String line = len + " " + k + "=" + v + "\n";
            int actualLength = line.getBytes(UTF_8).length;
            while (len != actualLength) {
                // Adjust for cases where length < 10 or > 100
                // or where UTF-8 encoding isn't a single octet
                // per character.
                // Must be in loop as size may go from 99 to 100 in
                // first pass, so we'd need a second.
                len = actualLength;
                line = len + " " + k + "=" + v + "\n";
                actualLength = line.getBytes(UTF_8).length;
            }
            w.write(line);
        });
        return w.toString().getBytes(UTF_8);
    }

    private void failForBigNumber(final String field, final long value, final long maxValue) {
        failForBigNumber(field, value, maxValue, "");
    }

    private void failForBigNumber(final String field, final long value, final long maxValue, final String additionalMsg) {
        if (value < 0 || value > maxValue) {
            throw new IllegalArgumentException(field + " '" + value // NOSONAR
                    + "' is too big ( > " + maxValue + " )." + additionalMsg);
        }
    }

    private void failForBigNumbers(final TarArchiveEntry entry) {
        failForBigNumber("entry size", entry.getSize(), TarConstants.MAXSIZE);
        failForBigNumberWithPosixMessage("group id", entry.getLongGroupId(), TarConstants.MAXID);
        failForBigNumber("last modification time", TimeUtils.toUnixTime(entry.getLastModifiedTime()), TarConstants.MAXSIZE);
        failForBigNumber("user id", entry.getLongUserId(), TarConstants.MAXID);
        failForBigNumber("mode", entry.getMode(), TarConstants.MAXID);
        failForBigNumber("major device number", entry.getDevMajor(), TarConstants.MAXID);
        failForBigNumber("minor device number", entry.getDevMinor(), TarConstants.MAXID);
    }

    private void failForBigNumberWithPosixMessage(final String field, final long value, final long maxValue) {
        failForBigNumber(field, value, maxValue, " Use STAR or POSIX extensions to overcome this limit");
    }

    /**
     * Finishes the TAR archive without closing the underlying OutputStream.
     *
     * An archive consists of a series of file entries terminated by an end-of-archive entry, which consists of two 512 blocks of zero bytes. POSIX.1 requires
     * two EOF records, like some other implementations.
     *
     * @throws IOException on error
     */
    @Override
    public void finish() throws IOException {
        checkFinished();
        if (haveUnclosedEntry) {
            throw new IOException("This archive contains unclosed entries.");
        }
        writeEOFRecord();
        writeEOFRecord();
        padAsNeeded();
        out.flush();
        super.finish();
    }

    @Override
    public long getBytesWritten() {
        return countingOut.getByteCount();
    }

    @Deprecated
    @Override
    public int getCount() {
        return (int) getBytesWritten();
    }

    /**
     * Gets the record size being used by this stream's TarBuffer.
     *
     * @return The TarBuffer record size.
     * @deprecated
     */
    @Deprecated
    public int getRecordSize() {
        return RECORD_SIZE;
    }

    /**
     * Handles long file or link names according to the longFileMode setting.
     *
     * <p>
     * I.e. if the given name is too long to be written to a plain tar header then
     * <ul>
     * <li>it creates a pax header who's name is given by the paxHeaderName parameter if longFileMode is POSIX</li>
     * <li>it creates a GNU longlink entry who's type is given by the linkType parameter if longFileMode is GNU</li>
     * <li>it throws an exception if longFileMode is ERROR</li>
     * <li>it truncates the name if longFileMode is TRUNCATE</li>
     * </ul>
     * </p>
     *
     * @param entry         entry the name belongs to
     * @param name          the name to write
     * @param paxHeaders    current map of pax headers
     * @param paxHeaderName name of the pax header to write
     * @param linkType      type of the GNU entry to write
     * @param fieldName     the name of the field
     * @throws IllegalArgumentException if the {@link TarArchiveOutputStream#longFileMode} equals {@link TarArchiveOutputStream#LONGFILE_ERROR} and the file
     *                                  name is too long
     * @return whether a pax header has been written.
     */
    private boolean handleLongName(final TarArchiveEntry entry, final String name, final Map<String, String> paxHeaders, final String paxHeaderName,
            final byte linkType, final String fieldName) throws IOException {
        final ByteBuffer encodedName = zipEncoding.encode(name);
        final int len = encodedName.limit() - encodedName.position();
        if (len >= TarConstants.NAMELEN) {

            if (longFileMode == LONGFILE_POSIX) {
                paxHeaders.put(paxHeaderName, name);
                return true;
            }
            if (longFileMode == LONGFILE_GNU) {
                // create a TarEntry for the LongLink, the contents
                // of which are the link's name
                final TarArchiveEntry longLinkEntry = new TarArchiveEntry(TarConstants.GNU_LONGLINK, linkType);

                longLinkEntry.setSize(len + 1L); // +1 for NUL
                transferModTime(entry, longLinkEntry);
                putArchiveEntry(longLinkEntry);
                write(encodedName.array(), encodedName.arrayOffset(), len);
                write(0); // NUL terminator
                closeArchiveEntry();
            } else if (longFileMode != LONGFILE_TRUNCATE) {
                throw new IllegalArgumentException(fieldName + " '" + name // NOSONAR
                        + "' is too long ( > " + TarConstants.NAMELEN + " bytes)");
            }
        }
        return false;
    }

    private void padAsNeeded() throws IOException {
        final int start = Math.toIntExact(recordsWritten % recordsPerBlock);
        if (start != 0) {
            for (int i = start; i < recordsPerBlock; i++) {
                writeEOFRecord();
            }
        }
    }

    /**
     * Puts an entry on the output stream. This writes the entry's header record and positions the output stream for writing the contents of the entry. Once
     * this method is called, the stream is ready for calls to write() to write the entry's contents. Once the contents are written, closeArchiveEntry()
     * <B>MUST</B> be called to ensure that all buffered data is completely written to the output stream.
     *
     * @param archiveEntry The TarEntry to be written to the archive.
     * @throws IOException              on error
     * @throws ClassCastException       if archiveEntry is not an instance of TarArchiveEntry
     * @throws IllegalArgumentException if the {@link TarArchiveOutputStream#longFileMode} equals {@link TarArchiveOutputStream#LONGFILE_ERROR} and the file
     *                                  name is too long
     * @throws IllegalArgumentException if the {@link TarArchiveOutputStream#bigNumberMode} equals {@link TarArchiveOutputStream#BIGNUMBER_ERROR} and one of the
     *                                  numeric values exceeds the limits of a traditional tar header.
     */
    @Override
    public void putArchiveEntry(final TarArchiveEntry archiveEntry) throws IOException {
        checkFinished();
        if (archiveEntry.isGlobalPaxHeader()) {
            final byte[] data = encodeExtendedPaxHeadersContents(archiveEntry.getExtraPaxHeaders());
            archiveEntry.setSize(data.length);
            archiveEntry.writeEntryHeader(recordBuf, zipEncoding, bigNumberMode == BIGNUMBER_STAR);
            writeRecord(recordBuf);
            currSize = archiveEntry.getSize();
            currBytes = 0;
            this.haveUnclosedEntry = true;
            write(data);
            closeArchiveEntry();
        } else {
            final Map<String, String> paxHeaders = new HashMap<>();
            final String entryName = archiveEntry.getName();
            final boolean paxHeaderContainsPath = handleLongName(archiveEntry, entryName, paxHeaders, "path", TarConstants.LF_GNUTYPE_LONGNAME, "file name");
            final String linkName = archiveEntry.getLinkName();
            final boolean paxHeaderContainsLinkPath = linkName != null && !linkName.isEmpty()
                    && handleLongName(archiveEntry, linkName, paxHeaders, "linkpath", TarConstants.LF_GNUTYPE_LONGLINK, "link name");

            if (bigNumberMode == BIGNUMBER_POSIX) {
                addPaxHeadersForBigNumbers(paxHeaders, archiveEntry);
            } else if (bigNumberMode != BIGNUMBER_STAR) {
                failForBigNumbers(archiveEntry);
            }

            if (addPaxHeadersForNonAsciiNames && !paxHeaderContainsPath && !ASCII.canEncode(entryName)) {
                paxHeaders.put("path", entryName);
            }

            if (addPaxHeadersForNonAsciiNames && !paxHeaderContainsLinkPath && (archiveEntry.isLink() || archiveEntry.isSymbolicLink())
                    && !ASCII.canEncode(linkName)) {
                paxHeaders.put("linkpath", linkName);
            }
            paxHeaders.putAll(archiveEntry.getExtraPaxHeaders());

            if (!paxHeaders.isEmpty()) {
                writePaxHeaders(archiveEntry, entryName, paxHeaders);
            }

            archiveEntry.writeEntryHeader(recordBuf, zipEncoding, bigNumberMode == BIGNUMBER_STAR);
            writeRecord(recordBuf);

            currBytes = 0;

            if (archiveEntry.isDirectory()) {
                currSize = 0;
            } else {
                currSize = archiveEntry.getSize();
            }
            currName = entryName;
            haveUnclosedEntry = true;
        }
    }

    /**
     * Sets whether to add a PAX extension header for non-ASCII file names.
     *
     * @param b whether to add a PAX extension header for non-ASCII file names.
     * @since 1.4
     */
    public void setAddPaxHeadersForNonAsciiNames(final boolean b) {
        addPaxHeadersForNonAsciiNames = b;
    }

    /**
     * Sets the big number mode. This can be BIGNUMBER_ERROR(0), BIGNUMBER_STAR(1) or BIGNUMBER_POSIX(2). This specifies the treatment of big files (sizes &gt;
     * TarConstants.MAXSIZE) and other numeric values too big to fit into a traditional tar header. Default is BIGNUMBER_ERROR.
     *
     * @param bigNumberMode the mode to use
     * @since 1.4
     */
    public void setBigNumberMode(final int bigNumberMode) {
        this.bigNumberMode = bigNumberMode;
    }

    /**
     * Sets the long file mode. This can be LONGFILE_ERROR(0), LONGFILE_TRUNCATE(1), LONGFILE_GNU(2) or LONGFILE_POSIX(3). This specifies the treatment of long
     * file names (names &gt;= TarConstants.NAMELEN). Default is LONGFILE_ERROR.
     *
     * @param longFileMode the mode to use
     */
    public void setLongFileMode(final int longFileMode) {
        this.longFileMode = longFileMode;
    }

    /**
     * Tests whether the character could lead to problems when used inside a TarArchiveEntry name for a PAX header.
     *
     * @return true if the character could lead to problems when used inside a TarArchiveEntry name for a PAX header.
     */
    private boolean shouldBeReplaced(final char c) {
        return c == 0 // would be read as Trailing null
                || c == '/' // when used as last character TAE will consider the PAX header a directory
                || c == '\\'; // same as '/' as slashes get "normalized" on Windows
    }

    private String stripTo7Bits(final String name) {
        final int length = name.length();
        final StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            final char stripped = (char) (name.charAt(i) & 0x7F);
            if (shouldBeReplaced(stripped)) {
                result.append("_");
            } else {
                result.append(stripped);
            }
        }
        return result.toString();
    }

    private void transferModTime(final TarArchiveEntry from, final TarArchiveEntry to) {
        long fromModTimeSeconds = TimeUtils.toUnixTime(from.getLastModifiedTime());
        if (fromModTimeSeconds < 0 || fromModTimeSeconds > TarConstants.MAXSIZE) {
            fromModTimeSeconds = 0;
        }
        to.setLastModifiedTime(FileTimes.fromUnixTime(fromModTimeSeconds));
    }

    /**
     * Writes bytes to the current tar archive entry. This method is aware of the current entry and will throw an exception if you attempt to write bytes past
     * the length specified for the current entry.
     *
     * @param wBuf       The buffer to write to the archive.
     * @param wOffset    The offset in the buffer from which to get bytes.
     * @param numToWrite The number of bytes to write.
     * @throws IOException on error
     */
    @Override
    public void write(final byte[] wBuf, final int wOffset, final int numToWrite) throws IOException {
        if (!haveUnclosedEntry) {
            throw new IllegalStateException("No current tar entry");
        }
        if (currBytes + numToWrite > currSize) {
            throw new IOException(
                    "Request to write '" + numToWrite + "' bytes exceeds size in header of '" + currSize + "' bytes for entry '" + currName + "'");
        }
        out.write(wBuf, wOffset, numToWrite);
        currBytes += numToWrite;
    }

    /**
     * Writes an EOF (end of archive) record to the tar archive. An EOF record consists of a record of all zeros.
     */
    private void writeEOFRecord() throws IOException {
        writeRecord(ArrayFill.fill(recordBuf, (byte) 0));
    }

    /**
     * Writes a PAX extended header with the given map as contents.
     *
     * @since 1.4
     */
    void writePaxHeaders(final TarArchiveEntry entry, final String entryName, final Map<String, String> headers) throws IOException {
        String name = "./PaxHeaders.X/" + stripTo7Bits(entryName);
        if (name.length() >= TarConstants.NAMELEN) {
            name = name.substring(0, TarConstants.NAMELEN - 1);
        }
        final TarArchiveEntry pex = new TarArchiveEntry(name, TarConstants.LF_PAX_EXTENDED_HEADER_LC);
        transferModTime(entry, pex);

        final byte[] data = encodeExtendedPaxHeadersContents(headers);
        pex.setSize(data.length);
        putArchiveEntry(pex);
        write(data);
        closeArchiveEntry();
    }

    /**
     * Writes an archive record to the archive.
     *
     * @param record The record data to write to the archive.
     * @throws IOException on error
     */
    private void writeRecord(final byte[] record) throws IOException {
        if (record.length != RECORD_SIZE) {
            throw new IOException("Record to write has length '" + record.length + "' which is not the record size of '" + RECORD_SIZE + "'");
        }

        out.write(record);
        recordsWritten++;
    }
}
