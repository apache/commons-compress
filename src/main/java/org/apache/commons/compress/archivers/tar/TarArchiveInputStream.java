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

/*
 * This package is based on the work done by Timothy Gerard Endres
 * (time@ice.com) to whom the Ant project is very grateful for his great code.
 */

package org.apache.commons.compress.archivers.tar;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipEncoding;
import org.apache.commons.compress.archivers.zip.ZipEncodingHelper;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.ArchiveUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;

/**
 * The TarInputStream reads a Unix tar archive as an InputStream. methods are provided to position at each successive entry in the archive, and the read each
 * entry as a normal input stream using read().
 *
 * @NotThreadSafe
 */
public class TarArchiveInputStream extends ArchiveInputStream<TarArchiveEntry> {

    // @formatter:off
    /**
     * Builds a new {@link GzipCompressorInputStream}.
     *
     * <p>
     * For example:
     * </p>
     * <pre>{@code
     * TarArchiveInputStream s = TarArchiveInputStream.builder()
     *   .setPath(path)
     *   .setLenient(true)
     *   .setFileNameCharset(StandardCharsets.UTF_8)
     *   .get();}
     * </pre>
     *
     * @see #get()
     * @since 1.29.0
     */
    // @formatter:on
    public static final class Builder extends AbstractTarBuilder<TarArchiveInputStream, Builder> {

        /**
         * Constructs a new instance.
         */
        private Builder() {
            // empty
        }

        @Override
        public TarArchiveInputStream get() throws IOException {
            return new TarArchiveInputStream(this);
        }

    }

    /**
     * IBM AIX <a href=""https://www.ibm.com/docs/sv/aix/7.2.0?topic=files-tarh-file">tar.h</a>: "This field is terminated with a space only."
     */
    private static final String VERSION_AIX = "0 ";

    private static final int SMALL_BUFFER_SIZE = 256;

    /**
     * Creates a new builder.
     *
     * @return a new builder.
     * @since 1.29.0
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Checks if the signature matches what is expected for a tar file.
     *
     * @param signature the bytes to check.
     * @param length    the number of bytes to check.
     * @return true, if this stream is a tar archive stream, false otherwise.
     */
    public static boolean matches(final byte[] signature, final int length) {
        final int versionOffset = TarConstants.VERSION_OFFSET;
        final int versionLen = TarConstants.VERSIONLEN;
        if (length < versionOffset + versionLen) {
            return false;
        }
        final int magicOffset = TarConstants.MAGIC_OFFSET;
        final int magicLen = TarConstants.MAGICLEN;
        if (ArchiveUtils.matchAsciiBuffer(TarConstants.MAGIC_POSIX, signature, magicOffset, magicLen)
                && ArchiveUtils.matchAsciiBuffer(TarConstants.VERSION_POSIX, signature, versionOffset, versionLen)) {
            return true;
        }
        // IBM AIX tar.h https://www.ibm.com/docs/sv/aix/7.2.0?topic=files-tarh-file : "This field is terminated with a space only."
        if (ArchiveUtils.matchAsciiBuffer(TarConstants.MAGIC_POSIX, signature, magicOffset, magicLen)
                && ArchiveUtils.matchAsciiBuffer(VERSION_AIX, signature, versionOffset, versionLen)) {
            return true;
        }
        if (ArchiveUtils.matchAsciiBuffer(TarConstants.MAGIC_GNU, signature, magicOffset, magicLen)
                && (ArchiveUtils.matchAsciiBuffer(TarConstants.VERSION_GNU_SPACE, signature, versionOffset, versionLen)
                        || ArchiveUtils.matchAsciiBuffer(TarConstants.VERSION_GNU_ZERO, signature, versionOffset, versionLen))) {
            return true;
        }
        // COMPRESS-107 - recognize Ant tar files
        return ArchiveUtils.matchAsciiBuffer(TarConstants.MAGIC_ANT, signature, magicOffset, magicLen)
                && ArchiveUtils.matchAsciiBuffer(TarConstants.VERSION_ANT, signature, versionOffset, versionLen);
    }

    private final byte[] smallBuf = new byte[SMALL_BUFFER_SIZE];

    /** The buffer to store the TAR header. **/
    private final byte[] recordBuffer;

    /** The size of a block. */
    private final int blockSize;

    /** True if stream is at EOF. */
    private boolean atEof;

    /** How far into the entry the stream is at. */
    private long entryOffset;

    /** The meta-data about the current entry. */
    private TarArchiveEntry currEntry;

    /** The current input stream. */
    private InputStream currentInputStream;

    /** The encoding of the file. */
    private final ZipEncoding zipEncoding;

    /** The global PAX header. */
    private final Map<String, String> globalPaxHeaders = new HashMap<>();

    /** The global sparse headers, this is only used in PAX Format 0.X. */
    private final List<TarArchiveStructSparse> globalSparseHeaders = new ArrayList<>();

    private final boolean lenient;

    private TarArchiveInputStream(final Builder builder) throws IOException {
        super(builder);
        this.zipEncoding = ZipEncodingHelper.getZipEncoding(builder.getCharset());
        this.recordBuffer = new byte[builder.getRecordSize()];
        this.blockSize = builder.getBlockSize();
        this.lenient = builder.isLenient();
    }

    /**
     * Constructs a new instance.
     *
     * <p>Since 1.29.0: throws {@link IOException}.</p>
     *
     * @param inputStream the input stream to use.
     */
    public TarArchiveInputStream(final InputStream inputStream) throws IOException {
        this(builder().setInputStream(inputStream));
    }

    /**
     * Constructs a new instance with default values.
     *
     * <p>Since 1.29.0: throws {@link IOException}.</p>
     *
     * @param inputStream the input stream to use.
     * @param lenient     when set to true illegal values for group/userid, mode, device numbers and timestamp will be ignored and the fields set to
     *                    {@link TarArchiveEntry#UNKNOWN}. When set to false such illegal fields cause an exception instead.
     * @throws IOException if an I/O error occurs.
     * @since 1.19
     * @deprecated Since 1.29.0, use {@link #builder()}.
     */
    @Deprecated
    public TarArchiveInputStream(final InputStream inputStream, final boolean lenient) throws IOException {
        this(builder().setInputStream(inputStream).setLenient(lenient));
    }

    /**
     * Constructs a new instance.
     *
     * <p>Since 1.29.0: throws {@link IOException}.</p>
     *
     * @param inputStream the input stream to use.
     * @param blockSize   the block size to use.
     * @throws IOException if an I/O error occurs.
     * @deprecated Since 1.29.0, use {@link #builder()}.
     */
    @Deprecated
    public TarArchiveInputStream(final InputStream inputStream, final int blockSize) throws IOException {
        this(builder().setInputStream(inputStream).setBlockSize(blockSize));
    }

    /**
     * Constructs a new instance.
     *
     * <p>Since 1.29.0: throws {@link IOException}.</p>
     *
     * @param inputStream the input stream to use.
     * @param blockSize   the block size to use.
     * @param recordSize  the record size to use.
     * @throws IOException if an I/O error occurs.
     * @deprecated Since 1.29.0, use {@link #builder()}.
     */
    @Deprecated
    public TarArchiveInputStream(final InputStream inputStream, final int blockSize, final int recordSize) throws IOException {
        this(builder().setInputStream(inputStream).setBlockSize(blockSize).setRecordSize(recordSize));
    }

    /**
     * Constructs a new instance.
     *
     * <p>Since 1.29.0: throws {@link IOException}.</p>
     *
     * @param inputStream the input stream to use.
     * @param blockSize   the block size to use.
     * @param recordSize  the record size to use.
     * @param encoding    name of the encoding to use for file names.
     * @throws IOException if an I/O error occurs.
     * @since 1.4
     * @deprecated Since 1.29.0, use {@link #builder()}.
     */
    @Deprecated
    public TarArchiveInputStream(final InputStream inputStream, final int blockSize, final int recordSize, final String encoding) throws IOException {
        this(builder().setInputStream(inputStream).setBlockSize(blockSize).setRecordSize(recordSize).setCharset(encoding));
    }

    /**
     * Constructs a new instance.
     *
     * <p>Since 1.29.0: throws {@link IOException}.</p>
     *
     * @param inputStream the input stream to use.
     * @param blockSize   the block size to use.
     * @param recordSize  the record size to use.
     * @param encoding    name of the encoding to use for file names.
     * @param lenient     when set to true illegal values for group/userid, mode, device numbers and timestamp will be ignored and the fields set to
     *                    {@link TarArchiveEntry#UNKNOWN}. When set to false such illegal fields cause an exception instead.
     * @throws IOException if an I/O error occurs.
     * @since 1.19
     * @deprecated Since 1.29.0, use {@link #builder()}.
     */
    @Deprecated
    public TarArchiveInputStream(final InputStream inputStream, final int blockSize, final int recordSize, final String encoding,
            final boolean lenient) throws IOException {
        // @formatter:off
        this(builder()
                .setInputStream(inputStream)
                .setBlockSize(blockSize)
                .setRecordSize(recordSize)
                .setCharset(encoding)
                .setLenient(lenient));
        // @formatter:on
    }

    /**
     * Constructs a new instance.
     *
     * <p>Since 1.29.0: throws {@link IOException}.</p>
     *
     * @param inputStream the input stream to use.
     * @param blockSize   the block size to use.
     * @param encoding    name of the encoding to use for file names.
     * @throws IOException if an I/O error occurs.
     * @since 1.4
     * @deprecated Since 1.29.0, use {@link #builder()}.
     */
    @Deprecated
    public TarArchiveInputStream(final InputStream inputStream, final int blockSize, final String encoding) throws IOException {
        this(builder().setInputStream(inputStream).setBlockSize(blockSize).setCharset(encoding));
    }

    /**
     * Constructs a new instance.
     *
     * <p>Since 1.29.0: throws {@link IOException}.</p>
     *
     * @param inputStream the input stream to use.
     * @param encoding    name of the encoding to use for file names.
     * @throws IOException if an I/O error occurs.
     * @since 1.4
     * @deprecated Since 1.29.0, use {@link #builder()}.
     */
    @Deprecated
    public TarArchiveInputStream(final InputStream inputStream, final String encoding) throws IOException {
        this(builder().setInputStream(inputStream).setCharset(encoding));
    }

    private void afterRead(final int read) throws IOException {
        // Count the bytes read
        count(read);
        // Check for truncated entries
        if (read == -1 && entryOffset < currEntry.getSize()) {
            throw new EOFException(String.format("Truncated TAR archive: Entry '%s' expected %,d bytes, actual %,d", currEntry.getName(), currEntry.getSize(),
                    entryOffset));
        }
        entryOffset += Math.max(0, read);
    }

    /**
     * Gets the available data that can be read from the current entry in the archive. This does not indicate how much data is left in the entire archive, only
     * in the current entry. This value is determined from the entry's size header field and the amount of data already read from the current entry.
     * Integer.MAX_VALUE is returned in case more than Integer.MAX_VALUE bytes are left in the current entry in the archive.
     *
     * @return The number of available bytes for the current entry.
     * @throws IOException for signature
     */
    @Override
    public int available() throws IOException {
        if (isDirectory()) {
            return 0;
        }
        final long available = currEntry.getRealSize() - entryOffset;
        if (available > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) available;
    }

    /**
     * Build the input streams consisting of all-zero input streams and non-zero input streams. When reading from the non-zero input streams, the data is
     * actually read from the original input stream. The size of each input stream is introduced by the sparse headers.
     * <p>
     * NOTE : Some all-zero input streams and non-zero input streams have the size of 0. We DO NOT store the 0 size input streams because they are meaningless.
     * </p>
     */
    private void buildSparseInputStreams() throws IOException {
        final List<InputStream> sparseInputStreams = new ArrayList<>();
        final List<TarArchiveStructSparse> sparseHeaders = currEntry.getOrderedSparseHeaders();
        // Stream doesn't need to be closed at all as it doesn't use any resources
        final InputStream zeroInputStream = new TarArchiveSparseZeroInputStream(); // NOSONAR
        // logical offset into the extracted entry
        long offset = 0;
        for (final TarArchiveStructSparse sparseHeader : sparseHeaders) {
            final long zeroBlockSize = sparseHeader.getOffset() - offset;
            if (zeroBlockSize < 0) {
                // sparse header says to move backwards inside the extracted entry
                throw new ArchiveException("Corrupted struct sparse detected");
            }
            // only store the zero block if it is not empty
            if (zeroBlockSize > 0) {
                // @formatter:off
                sparseInputStreams.add(BoundedInputStream.builder()
                        .setInputStream(zeroInputStream)
                        .setMaxCount(sparseHeader.getOffset() - offset)
                        .get());
                // @formatter:on
            }
            // only store the input streams with non-zero size
            if (sparseHeader.getNumbytes() > 0) {
                // @formatter:off
                sparseInputStreams.add(BoundedInputStream.builder()
                        .setInputStream(in)
                        .setAfterRead(this::afterRead)
                        .setMaxCount(sparseHeader.getNumbytes())
                        .setPropagateClose(false)
                        .get());
                // @formatter:on
            }
            offset = sparseHeader.getOffset() + sparseHeader.getNumbytes();
        }
        currentInputStream = new SequenceInputStream(Collections.enumeration(sparseInputStreams));
    }

    /**
     * Tests whether this class is able to read the given entry.
     *
     * @return The implementation will return true if the {@link ArchiveEntry} is an instance of {@link TarArchiveEntry}
     */
    @Override
    public boolean canReadEntryData(final ArchiveEntry archiveEntry) {
        return archiveEntry instanceof TarArchiveEntry;
    }

    /**
     * Closes this stream. Calls the TarBuffer's close() method.
     *
     * @throws IOException on error.
     */
    @Override
    public void close() throws IOException {
        // Close all the input streams in sparseInputStreams
        if (currentInputStream != null) {
            currentInputStream.close();
            currentInputStream = null;
        }
        in.close();
    }

    /**
     * This method is invoked once the end of the archive is hit, it tries to consume the remaining bytes under the assumption that the tool creating this
     * archive has padded the last block.
     */
    private void consumeRemainderOfLastBlock() throws IOException {
        final long bytesReadOfLastBlock = getBytesRead() % blockSize;
        if (bytesReadOfLastBlock > 0) {
            count(IOUtils.skip(in, blockSize - bytesReadOfLastBlock));
        }
    }

    /**
     * Gets the current TAR Archive Entry that this input stream is processing
     *
     * @return The current Archive Entry.
     */
    public TarArchiveEntry getCurrentEntry() {
        return currEntry;
    }

    /**
     * Gets the next entry in this tar archive as long name data.
     *
     * @return The next entry in the archive as long name data, or null.
     * @throws IOException on error.
     *
     * @deprecated Since 1.29.0 without replacement.
     */
    @Deprecated
    protected byte[] getLongNameData() throws IOException {
        // read in the name
        final ByteArrayOutputStream longName = new ByteArrayOutputStream();
        int length = 0;
        while ((length = read(smallBuf)) >= 0) {
            longName.write(smallBuf, 0, length);
        }
        getNextEntry();
        if (currEntry == null) {
            // Bugzilla: 40334
            // Malformed tar file - long entry name not followed by entry
            return null;
        }
        byte[] longNameData = longName.toByteArray();
        // remove trailing null terminator(s)
        length = longNameData.length;
        while (length > 0 && longNameData[length - 1] == 0) {
            --length;
        }
        if (length != longNameData.length) {
            longNameData = Arrays.copyOf(longNameData, length);
        }
        return longNameData;
    }

    /**
     * Advances to the next file entry in the tar archive.
     * <p>
     *     Skips any remaining data in the current entry, then reads and returns the next file entry.
     *     Handles special records (PAX, GNU long name, sparse, etc.) and applies PAX headers as needed.
     * </p>
     *
     * @return the next file entry, or {@code null} if there are no more entries.
     * @throws IOException if the next entry could not be read or the archive is malformed.
     */
    @Override
    public TarArchiveEntry getNextEntry() throws IOException {
        if (isAtEOF()) {
            return null;
        }
        final Map<String, String> paxHeaders = new HashMap<>();
        final List<TarArchiveStructSparse> sparseHeaders = new ArrayList<>();
        // Handle special tar records
        boolean lastWasSpecial = false;
        do {
            // If there is a current entry, skip any unread data and padding
            if (currentInputStream != null) {
                IOUtils.skip(currentInputStream, Long.MAX_VALUE); // Skip to end of current entry
                skipRecordPadding(); // Skip padding to align to the next record
            }
            // Read the next header record
            final byte[] headerBuf = getRecord();
            if (headerBuf == null) {
                // If we encountered special records but no file entry, the archive is malformed
                if (lastWasSpecial) {
                    throw new ArchiveException("Premature end of tar archive. Didn't find any file entry after GNU or PAX record.");
                }
                currEntry = null;
                return null; // End of archive
            }
            // Parse the header into a new entry
            currEntry = new TarArchiveEntry(globalPaxHeaders, headerBuf, zipEncoding, lenient);
            // Set up the input stream for the new entry
            currentInputStream = BoundedInputStream.builder()
                    .setInputStream(in)
                    .setAfterRead(this::afterRead)
                    .setMaxCount(currEntry.getSize())
                    .setPropagateClose(false)
                    .get();
            entryOffset = 0;
            lastWasSpecial = TarUtils.isSpecialTarRecord(currEntry);
            if (lastWasSpecial) {
                // Handle PAX, GNU long name, or other special records
                TarUtils.handleSpecialTarRecord(currentInputStream, zipEncoding, getMaxEntryNameLength(), currEntry, paxHeaders, sparseHeaders,
                        globalPaxHeaders, globalSparseHeaders);
            }
        } while (lastWasSpecial);
        // Apply global and local PAX headers
        TarUtils.applyPaxHeadersToEntry(currEntry, paxHeaders, sparseHeaders, globalPaxHeaders, globalSparseHeaders);
        // Handle sparse files
        if (currEntry.isSparse()) {
            if (currEntry.isOldGNUSparse()) {
                // Old GNU sparse format uses extra header blocks for metadata.
                // These blocks are not included in the entry’s size, so we cannot
                // rely on BoundedInputStream here.
                readOldGNUSparse();
            } else if (currEntry.isPaxGNU1XSparse()) {
                currEntry.setSparseHeaders(TarUtils.parsePAX1XSparseHeaders(currentInputStream, getRecordSize()));
            }
            // sparse headers are all done reading, we need to build
            // sparse input streams using these sparse headers
            buildSparseInputStreams();
        }
        // Ensure directory names end with a slash
        if (currEntry.isDirectory() && !currEntry.getName().endsWith("/")) {
            currEntry.setName(currEntry.getName() + "/");
        }
        return currEntry;
    }

    /**
     * Gets the next entry in this tar archive. This will skip over any remaining data in the current entry, if there is one, and place the input stream at the
     * header of the next entry, and read the header and instantiate a new TarEntry from the header bytes and return that entry. If there are no more entries in
     * the archive, null will be returned to indicate that the end of the archive has been reached.
     *
     * @return The next TarEntry in the archive, or null.
     * @throws IOException on error.
     * @deprecated Use {@link #getNextEntry()}.
     */
    @Deprecated
    public TarArchiveEntry getNextTarEntry() throws IOException {
        return getNextEntry();
    }

    /**
     * Gets the next record in this tar archive. This will skip over any remaining data in the current entry, if there is one, and place the input stream at the
     * header of the next entry.
     * <p>
     * If there are no more entries in the archive, null will be returned to indicate that the end of the archive has been reached. At the same time the
     * {@code hasHitEOF} marker will be set to true.
     * </p>
     *
     * @return The next header in the archive, or null.
     * @throws IOException on error.
     */
    private byte[] getRecord() throws IOException {
        byte[] headerBuf = readRecord();
        setAtEOF(isEOFRecord(headerBuf));
        if (isAtEOF() && headerBuf != null) {
            tryToConsumeSecondEOFRecord();
            consumeRemainderOfLastBlock();
            headerBuf = null;
        }
        return headerBuf;
    }

    /**
     * Gets the record size being used by this stream's buffer.
     *
     * @return The TarBuffer record size.
     */
    public int getRecordSize() {
        return recordBuffer.length;
    }

    /**
     * Tests whether we are at the end-of-file.
     *
     * @return whether we are at the end-of-file.
     */
    protected final boolean isAtEOF() {
        return atEof;
    }

    private boolean isDirectory() {
        return currEntry != null && currEntry.isDirectory();
    }

    /**
     * Tests if an archive record indicate End of Archive. End of archive is indicated by a record that consists entirely of null bytes.
     *
     * @param record The record data to check.
     * @return true if the record data is an End of Archive.
     */
    protected boolean isEOFRecord(final byte[] record) {
        return record == null || ArchiveUtils.isArrayZero(record, getRecordSize());
    }

    /**
     * Since we do not support marking just yet, we do nothing.
     *
     * @param markLimit The limit to mark.
     */
    @Override
    public synchronized void mark(final int markLimit) {
    }

    /**
     * Since we do not support marking just yet, we return false.
     *
     * @return Always false.
     */
    @Override
    public boolean markSupported() {
        return false;
    }

    /**
     * Reads bytes from the current tar archive entry.
     * <p>
     * This method is aware of the boundaries of the current entry in the archive and will deal with them as if they were this stream's start and EOF.
     * </p>
     *
     * @param buf       The buffer into which to place bytes read.
     * @param offset    The offset at which to place bytes read.
     * @param numToRead The number of bytes to read.
     * @return The number of bytes read, or -1 at EOF.
     * @throws NullPointerException      if {@code buf} is null
     * @throws IndexOutOfBoundsException if {@code offset} or {@code numToRead} are negative,
     *                                   or if {@code offset + numToRead} is greater than {@code buf.length}.
     * @throws IOException on error
     */
    @Override
    public int read(final byte[] buf, final int offset, int numToRead) throws IOException {
        IOUtils.checkFromIndexSize(buf, offset, numToRead);
        if (numToRead == 0) {
            return 0;
        }
        if (isAtEOF() || isDirectory()) {
            return -1;
        }
        if (currEntry == null || currentInputStream == null) {
            throw new IllegalStateException("No current tar entry");
        }
        return currentInputStream.read(buf, offset, numToRead);
    }

    /**
     * Adds the sparse chunks from the current entry to the sparse chunks, including any additional sparse entries following the current entry.
     *
     * @throws IOException on error.
     */
    private void readOldGNUSparse() throws IOException {
        if (currEntry.isExtended()) {
            TarArchiveSparseEntry entry;
            do {
                final byte[] headerBuf = getRecord();
                if (headerBuf == null) {
                    throw new ArchiveException("Premature end of tar archive. Didn't find extended_header after header with extended flag.");
                }
                entry = new TarArchiveSparseEntry(headerBuf);
                currEntry.getSparseHeaders().addAll(entry.getSparseHeaders());
            } while (entry.isExtended());
        }
    }

    /**
     * Reads a record from the input stream and return the data.
     *
     * @return The record data or null if EOF has been hit.
     * @throws IOException on error.
     */
    protected byte[] readRecord() throws IOException {
        final int readCount = IOUtils.read(in, recordBuffer);
        count(readCount);
        if (readCount != getRecordSize()) {
            return null;
        }
        return recordBuffer;
    }

    /**
     * Since we do not support marking just yet, we do nothing.
     */
    @Override
    public synchronized void reset() {
        // empty
    }

    /**
     * Sets whether we are at the end-of-file.
     *
     * @param atEof whether we are at the end-of-file.
     */
    protected final void setAtEOF(final boolean atEof) {
        this.atEof = atEof;
    }

    /**
     * Sets the current entry.
     *
     * @param currEntry the current entry.
     */
    protected final void setCurrentEntry(final TarArchiveEntry currEntry) {
        this.currEntry = currEntry;
    }

    /**
     * Skips over and discards {@code n} bytes of data from this input stream. The {@code skip} method may, for a variety of reasons, end up skipping over some
     * smaller number of bytes, possibly {@code 0}. This may result from any of a number of conditions; reaching end of file or end of entry before {@code n}
     * bytes have been skipped; are only two possibilities. The actual number of bytes skipped is returned. If {@code n} is negative, no bytes are skipped.
     *
     * @param n the number of bytes to be skipped.
     * @return the actual number of bytes skipped.
     * @throws IOException if a truncated tar archive is detected or some other I/O error occurs.
     */
    @Override
    public long skip(final long n) throws IOException {
        if (n <= 0 || isDirectory()) {
            return 0;
        }
        if (currEntry == null || currentInputStream == null) {
            throw new IllegalStateException("No current tar entry");
        }
        // Use Apache Commons IO to skip as it handles skipping fully
        return IOUtils.skip(currentInputStream, n);
    }

    /**
     * The last record block should be written at the full size, so skip any additional space used to fill a record after an entry.
     *
     * @throws IOException if a truncated tar archive is detected.
     */
    private void skipRecordPadding() throws IOException {
        final long entrySize = currEntry != null ? currEntry.getSize() : 0;
        if (!isDirectory() && entrySize > 0 && entrySize % getRecordSize() != 0) {
            final long padding = getRecordSize() - (entrySize % getRecordSize());
            final long skipped = IOUtils.skip(in, padding);
            count(skipped);
            if (skipped != padding) {
                throw new EOFException(String.format("Truncated TAR archive: Failed to skip record padding for entry '%s'", currEntry.getName()));
            }
        }
    }

    /**
     * Tries to read the next record rewinding the stream if it is not an EOF record.
     * <p>
     * This is meant to protect against cases where a tar implementation has written only one EOF record when two are expected. Actually this won't help since a
     * non-conforming implementation likely won't fill full blocks consisting of - by default - ten records either so we probably have already read beyond the
     * archive anyway.
     * </p>
     */
    private void tryToConsumeSecondEOFRecord() throws IOException {
        boolean shouldReset = true;
        final boolean marked = in.markSupported();
        if (marked) {
            in.mark(getRecordSize());
        }
        try {
            shouldReset = !isEOFRecord(readRecord());
        } finally {
            if (shouldReset && marked) {
                pushedBackBytes(getRecordSize());
                in.reset();
            }
        }
    }
}
