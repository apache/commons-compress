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
package org.apache.commons.compress.archivers.tar;

import static java.util.Objects.requireNonNull;

import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.zip.ZipEncoding;
import org.apache.commons.compress.archivers.zip.ZipEncodingHelper;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.ArchiveUtils;
import org.apache.commons.compress.utils.BoundedArchiveInputStream;
import org.apache.commons.compress.utils.BoundedSeekableByteChannelInputStream;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.apache.commons.io.function.IOIterable;
import org.apache.commons.io.function.IOIterator;
import org.apache.commons.io.input.BoundedInputStream;

/**
 * Provides random access to Unix archives.
 *
 * @since 1.21
 */
public class TarFile implements Closeable, IOIterable<TarArchiveEntry> {

    private final class BoundedTarEntryInputStream extends BoundedArchiveInputStream {

        private final SeekableByteChannel channel;

        private final TarArchiveEntry entry;

        BoundedTarEntryInputStream(final TarArchiveEntry entry, final SeekableByteChannel channel) throws IOException {
            super(entry.getDataOffset(), entry.getSize());
            this.entry = entry;
            this.channel = channel;
        }

        @Override
        protected int read(final long pos, final ByteBuffer buf) throws IOException {
            requireNonNull(buf, "ByteBuffer");
            // The caller ensures that [pos, pos + buf.remaining()] is within [start, end]
            channel.position(pos);
            final int totalRead = channel.read(buf);
            if (totalRead == -1) {
                if (buf.remaining() > 0) {
                    throw new EOFException(String.format("Truncated TAR archive: expected at least %d bytes, but got only %d bytes",
                            entry.getDataOffset() + entry.getSize(), channel.position()));
                }
                // Marks the TarFile as having reached EOF.
                setAtEOF(true);
            } else {
                buf.flip();
            }
            return totalRead;
        }
    }

    // @formatter:off
    /**
     * Builds a new {@link GzipCompressorInputStream}.
     *
     * <p>
     * For example:
     * </p>
     * <pre>{@code
     * TarFile s = TarFile.builder()
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
    public static final class Builder extends AbstractTarBuilder<TarFile, Builder> {

        private SeekableByteChannel channel;

        /**
         * Constructs a new instance.
         */
        private Builder() {
            // empty
        }

        @Override
        public TarFile get() throws IOException {
            return new TarFile(this);
        }

        /**
         * Sets the SeekableByteChannel.
         *
         * @param channel  the SeekableByteChannel.
         * @return {@code this} instance.
         */
        public Builder setSeekableByteChannel(final SeekableByteChannel channel) {
            this.channel = channel;
            return asThis();
        }

    }

    /**
     * Creates a new builder.
     *
     * @return a new builder.
     * @since 1.29.0
     */
    public static Builder builder() {
        return new Builder();
    }

    private final SeekableByteChannel archive;

    /**
     * The encoding of the tar file
     */
    private final ZipEncoding zipEncoding;

    private final LinkedList<TarArchiveEntry> entries = new LinkedList<>();

    private final int blockSize;

    private final boolean lenient;

    private final int recordSize;

    private final ByteBuffer recordBuffer;

    /**
     * The global sparse headers, this is only used in PAX Format 0.X.
     */
    private final List<TarArchiveStructSparse> globalSparseHeaders = new ArrayList<>();

    private boolean eof;

    /**
     * The meta-data about the current entry.
     */
    private TarArchiveEntry currEntry;

    /**
     * The global PAX header.
     */
    private final Map<String, String> globalPaxHeaders = new HashMap<>();

    private final Map<String, List<InputStream>> sparseInputStreams = new HashMap<>();

    private TarFile(final Builder builder) throws IOException {
        this.archive = builder.channel != null ? builder.channel : Files.newByteChannel(builder.getPath());
        this.zipEncoding = ZipEncodingHelper.getZipEncoding(builder.getCharset());
        this.recordSize = builder.getRecordSize();
        this.recordBuffer = ByteBuffer.allocate(this.recordSize);
        this.blockSize = builder.getBlockSize();
        this.lenient = builder.isLenient();
        forEach(entries::add);
    }

    /**
     * Constructor for TarFile.
     *
     * @param content the content to use.
     * @throws IOException when reading the tar archive fails.
     * @deprecated Use {@link #builder()} and {@link Builder}.
     */
    @Deprecated
    public TarFile(final byte[] content) throws IOException {
        this(new SeekableInMemoryByteChannel(content));
    }

    /**
     * Constructor for TarFile.
     *
     * @param content the content to use.
     * @param lenient when set to true illegal values for group/userid, mode, device numbers and timestamp will be ignored and the fields set to
     *                {@link TarArchiveEntry#UNKNOWN}. When set to false such illegal fields cause an exception instead.
     * @throws IOException when reading the tar archive fails.
     * @deprecated Use {@link #builder()} and {@link Builder}.
     */
    @Deprecated
    public TarFile(final byte[] content, final boolean lenient) throws IOException {
        this(new SeekableInMemoryByteChannel(content), TarConstants.DEFAULT_BLKSIZE, TarConstants.DEFAULT_RCDSIZE, null, lenient);
    }

    /**
     * Constructor for TarFile.
     *
     * @param content  the content to use.
     * @param encoding the encoding to use.
     * @throws IOException when reading the tar archive fails.
     * @deprecated Use {@link #builder()} and {@link Builder}.
     */
    @Deprecated
    public TarFile(final byte[] content, final String encoding) throws IOException {
        this(new SeekableInMemoryByteChannel(content), TarConstants.DEFAULT_BLKSIZE, TarConstants.DEFAULT_RCDSIZE, encoding, false);
    }

    /**
     * Constructor for TarFile.
     *
     * @param archive the file of the archive to use.
     * @throws IOException when reading the tar archive fails.
     * @deprecated Use {@link #builder()} and {@link Builder}.
     */
    @Deprecated
    public TarFile(final File archive) throws IOException {
        this(archive.toPath());
    }

    /**
     * Constructor for TarFile.
     *
     * @param archive the file of the archive to use.
     * @param lenient when set to true illegal values for group/userid, mode, device numbers and timestamp will be ignored and the fields set to
     *                {@link TarArchiveEntry#UNKNOWN}. When set to false such illegal fields cause an exception instead.
     * @throws IOException when reading the tar archive fails.
     * @deprecated Use {@link #builder()} and {@link Builder}.
     */
    @Deprecated
    public TarFile(final File archive, final boolean lenient) throws IOException {
        this(archive.toPath(), lenient);
    }

    /**
     * Constructor for TarFile.
     *
     * @param archive  the file of the archive to use.
     * @param encoding the encoding to use.
     * @throws IOException when reading the tar archive fails.
     * @deprecated Use {@link #builder()} and {@link Builder}.
     */
    @Deprecated
    public TarFile(final File archive, final String encoding) throws IOException {
        this(archive.toPath(), encoding);
    }

    /**
     * Constructor for TarFile.
     *
     * @param archivePath the path of the archive to use.
     * @throws IOException when reading the tar archive fails.
     */
    public TarFile(final Path archivePath) throws IOException {
        this(Files.newByteChannel(archivePath), TarConstants.DEFAULT_BLKSIZE, TarConstants.DEFAULT_RCDSIZE, null, false);
    }

    /**
     * Constructor for TarFile.
     *
     * @param archivePath the path of the archive to use.
     * @param lenient     when set to true illegal values for group/userid, mode, device numbers and timestamp will be ignored and the fields set to
     *                    {@link TarArchiveEntry#UNKNOWN}. When set to false such illegal fields cause an exception instead.
     * @throws IOException when reading the tar archive fails.
     * @deprecated Use {@link #builder()} and {@link Builder}.
     */
    @Deprecated
    public TarFile(final Path archivePath, final boolean lenient) throws IOException {
        this(Files.newByteChannel(archivePath), TarConstants.DEFAULT_BLKSIZE, TarConstants.DEFAULT_RCDSIZE, null, lenient);
    }

    /**
     * Constructor for TarFile.
     *
     * @param archivePath the path of the archive to use.
     * @param encoding    the encoding to use.
     * @throws IOException when reading the tar archive fails.
     * @deprecated Use {@link #builder()} and {@link Builder}.
     */
    @Deprecated
    public TarFile(final Path archivePath, final String encoding) throws IOException {
        this(Files.newByteChannel(archivePath), TarConstants.DEFAULT_BLKSIZE, TarConstants.DEFAULT_RCDSIZE, encoding, false);
    }

    /**
     * Constructor for TarFile.
     *
     * @param content the content to use.
     * @throws IOException when reading the tar archive fails.
     * @deprecated Use {@link #builder()} and {@link Builder}.
     */
    @Deprecated
    public TarFile(final SeekableByteChannel content) throws IOException {
        this(content, TarConstants.DEFAULT_BLKSIZE, TarConstants.DEFAULT_RCDSIZE, null, false);
    }

    /**
     * Constructor for TarFile.
     *
     * @param archive    the seekable byte channel to use.
     * @param blockSize  the blocks size to use.
     * @param recordSize the record size to use.
     * @param encoding   the encoding to use.
     * @param lenient    when set to true illegal values for group/userid, mode, device numbers and timestamp will be ignored and the fields set to
     *                   {@link TarArchiveEntry#UNKNOWN}. When set to false such illegal fields cause an exception instead.
     * @throws IOException when reading the tar archive fails.
     * @deprecated Use {@link #builder()} and {@link Builder}.
     */
    @Deprecated
    public TarFile(final SeekableByteChannel archive, final int blockSize, final int recordSize, final String encoding, final boolean lenient)
            throws IOException {
        this(builder().setSeekableByteChannel(archive).setBlockSize(blockSize).setRecordSize(recordSize).setCharset(encoding).setLenient(lenient));
    }

    /**
     * Build the input streams consisting of all-zero input streams and non-zero input streams. When reading from the non-zero input streams, the data is
     * actually read from the original input stream. The size of each input stream is introduced by the sparse headers.
     *
     * @implNote Some all-zero input streams and non-zero input streams have the size of 0. We DO NOT store the 0 size input streams because they are
     *           meaningless.
     */
    private void buildSparseInputStreams() throws IOException {
        final List<InputStream> streams = new ArrayList<>();
        final List<TarArchiveStructSparse> sparseHeaders = currEntry.getOrderedSparseHeaders();
        // Stream doesn't need to be closed at all as it doesn't use any resources
        final InputStream zeroInputStream = new TarArchiveSparseZeroInputStream(); // NOSONAR
        // logical offset into the extracted entry
        long offset = 0;
        long numberOfZeroBytesInSparseEntry = 0;
        for (final TarArchiveStructSparse sparseHeader : sparseHeaders) {
            final long zeroBlockSize = sparseHeader.getOffset() - offset;
            if (zeroBlockSize < 0) {
                // sparse header says to move backwards inside the extracted entry
                throw new ArchiveException("Corrupted struct sparse detected");
            }
            // only store the zero block if it is not empty
            if (zeroBlockSize > 0) {
                streams.add(BoundedInputStream.builder().setInputStream(zeroInputStream).setMaxCount(zeroBlockSize).get());
                numberOfZeroBytesInSparseEntry += zeroBlockSize;
            }
            // only store the input streams with non-zero size
            if (sparseHeader.getNumbytes() > 0) {
                final long start = currEntry.getDataOffset() + sparseHeader.getOffset() - numberOfZeroBytesInSparseEntry;
                if (start + sparseHeader.getNumbytes() < start) {
                    // possible integer overflow
                    throw new ArchiveException("Unreadable TAR archive, sparse block offset or length too big");
                }
                streams.add(new BoundedSeekableByteChannelInputStream(start, sparseHeader.getNumbytes(), archive));
            }
            offset = sparseHeader.getOffset() + sparseHeader.getNumbytes();
        }
        sparseInputStreams.put(currEntry.getName(), streams);
    }

    @Override
    public void close() throws IOException {
        archive.close();
    }

    /**
     * This method is invoked once the end of the archive is hit, it tries to consume the remaining bytes under the assumption that the tool creating this
     * archive has padded the last block.
     */
    private void consumeRemainderOfLastBlock() throws IOException {
        final long bytesReadOfLastBlock = archive.position() % blockSize;
        if (bytesReadOfLastBlock > 0) {
            repositionForwardBy(blockSize - bytesReadOfLastBlock);
        }
    }

    /**
     * Gets all TAR Archive Entries from the TarFile.
     *
     * @return All entries from the tar file.
     */
    public List<TarArchiveEntry> getEntries() {
        return new ArrayList<>(entries);
    }

    /**
     * Gets the input stream for the provided Tar Archive Entry.
     *
     * @param entry Entry to get the input stream from.
     * @return Input stream of the provided entry.
     * @throws IOException Corrupted TAR archive. Can't read entry.
     */
    public InputStream getInputStream(final TarArchiveEntry entry) throws IOException {
        try {
            // Sparse entries are composed of multiple fragments: wrap them in a ComposedTarInputStream
            if (entry.isSparse()) {
                final List<InputStream> streams = sparseInputStreams.get(entry.getName());
                return new ComposedTarInputStream(streams != null ? streams : Collections.emptyList(), entry.getRealSize());
            }
            // Regular entries are bounded: wrap in BoundedTarEntryInputStream to enforce size and detect premature EOF
            return new BoundedTarEntryInputStream(entry, archive);
        } catch (final RuntimeException e) {
            throw new ArchiveException("Corrupted TAR archive. Can't read entry", (Throwable) e);
        }
    }

    /**
     * Gets the next entry in this tar archive. This will skip to the end of the current entry, if there is one, and place the position of the channel at the
     * header of the next entry, and read the header and instantiate a new TarEntry from the header bytes and return that entry. If there are no more entries in
     * the archive, null will be returned to indicate that the end of the archive has been reached.
     *
     * @return The next TarEntry in the archive, or null if there is no next entry.
     * @throws IOException when reading the next TarEntry fails
     */
    private TarArchiveEntry getNextTarEntry() throws IOException {
        if (isAtEOF()) {
            return null;
        }
        final Map<String, String> paxHeaders = new HashMap<>();
        final List<TarArchiveStructSparse> sparseHeaders = new ArrayList<>();
        // Handle special tar records
        boolean lastWasSpecial = false;
        InputStream currentStream;
        do {
            // If there is a current entry, skip any unread data and padding
            if (currEntry != null) {
                repositionForwardTo(currEntry.getDataOffset() + currEntry.getSize());
                throwExceptionIfPositionIsNotInArchive();
                skipRecordPadding();
            }
            // Read the next header record
            final ByteBuffer headerBuf = getRecord();
            if (headerBuf == null) {
                // If we encountered special records but no file entry, the archive is malformed
                if (lastWasSpecial) {
                    throw new ArchiveException("Premature end of tar archive. Didn't find any file entry after GNU or PAX record.");
                }
                currEntry = null;
                return null; // End of archive
            }
            // Parse the header into a new entry
            final long position = archive.position();
            currEntry = new TarArchiveEntry(globalPaxHeaders, headerBuf.array(), zipEncoding, lenient, position);
            currentStream = new BoundedTarEntryInputStream(currEntry, archive);
            lastWasSpecial = TarUtils.isSpecialTarRecord(currEntry);
            if (lastWasSpecial) {
                // Handle PAX, GNU long name, or other special records
                // Make sure not to read beyond the entry data
                final BoundedTarEntryInputStream inputStream = new BoundedTarEntryInputStream(currEntry, archive);
                TarUtils.handleSpecialTarRecord(inputStream, zipEncoding, currEntry, paxHeaders, sparseHeaders, globalPaxHeaders,
                        globalSparseHeaders);
            }
        } while (lastWasSpecial);
        // Apply global and local PAX headers
        TarUtils.applyPaxHeadersToEntry(currEntry, paxHeaders, sparseHeaders, globalPaxHeaders, globalSparseHeaders);
        // Handle sparse files
        if (currEntry.isSparse()) {
            // These sparse formats have the sparse headers in the entry
            if (currEntry.isOldGNUSparse()) {
                // Old GNU sparse format uses extra header blocks for metadata.
                // These blocks are not included in the entry’s size, so we cannot
                // rely on BoundedTarEntryInputStream here.
                readOldGNUSparse();
                // Reposition to the start of the entry data to correctly compute the sparse streams
                currEntry.setDataOffset(archive.position());
            } else if (currEntry.isPaxGNU1XSparse()) {
                final long position = archive.position();
                currEntry.setSparseHeaders(TarUtils.parsePAX1XSparseHeaders(currentStream, recordSize));
                // Adjust the current entry to point to the start of the sparse file data
                final long sparseHeadersSize = archive.position() - position;
                currEntry.setSize(currEntry.getSize() - sparseHeadersSize);
                currEntry.setDataOffset(currEntry.getDataOffset() + sparseHeadersSize);
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
     * Gets the next record in this tar archive. This will skip over any remaining data in the current entry, if there is one, and place the input stream at the
     * header of the next entry.
     *
     * <p>
     * If there are no more entries in the archive, null will be returned to indicate that the end of the archive has been reached. At the same time the
     * {@code hasHitEOF} marker will be set to true.
     * </p>
     *
     * @return The next TarEntry in the archive, or null if there is no next entry.
     * @throws IOException when reading the next TarEntry fails
     */
    private ByteBuffer getRecord() throws IOException {
        ByteBuffer headerBuf = readRecord();
        setAtEOF(isEOFRecord(headerBuf));
        if (isAtEOF() && headerBuf != null) {
            // Consume rest
            tryToConsumeSecondEOFRecord();
            consumeRemainderOfLastBlock();
            headerBuf = null;
        }
        return headerBuf;
    }

    /**
     * Tests whether or not we are at the end-of-file.
     *
     * @return whether or not we are at the end-of-file.
     */
    protected final boolean isAtEOF() {
        return eof;
    }

    private boolean isDirectory() {
        return currEntry != null && currEntry.isDirectory();
    }

    private boolean isEOFRecord(final ByteBuffer headerBuf) {
        return headerBuf == null || ArchiveUtils.isArrayZero(headerBuf.array(), recordSize);
    }

    @Override
    public IOIterator<TarArchiveEntry> iterator() {
        return new IOIterator<TarArchiveEntry>() {

            private TarArchiveEntry next;

            @Override
            public boolean hasNext() throws IOException {
                if (next == null) {
                    next = getNextTarEntry();
                }
                return next != null;
            }

            @Override
            public TarArchiveEntry next() throws IOException {
                if (next == null) {
                    next = getNextTarEntry();
                }
                final TarArchiveEntry tmp = next;
                next = null;
                return tmp;
            }

            @Override
            public Iterator<TarArchiveEntry> unwrap() {
                return null;
            }

        };
    }

    /**
     * Adds the sparse chunks from the current entry to the sparse chunks, including any additional sparse entries following the current entry.
     *
     * @throws IOException when reading the sparse entry fails
     */
    private void readOldGNUSparse() throws IOException {
        if (currEntry.isExtended()) {
            TarArchiveSparseEntry entry;
            do {
                final ByteBuffer headerBuf = getRecord();
                if (headerBuf == null) {
                    throw new ArchiveException("Premature end of tar archive. Didn't find extended_header after header with extended flag.");
                }
                entry = new TarArchiveSparseEntry(headerBuf.array());
                currEntry.getSparseHeaders().addAll(entry.getSparseHeaders());
            } while (entry.isExtended());
        }
    }

    /**
     * Reads a record from the input stream and return the data.
     *
     * @return The record data or null if EOF has been hit.
     * @throws IOException if reading from the archive fails
     */
    private ByteBuffer readRecord() throws IOException {
        recordBuffer.rewind();
        final int readNow = archive.read(recordBuffer);
        if (readNow != recordSize) {
            return null;
        }
        return recordBuffer;
    }

    private void repositionForwardBy(final long offset) throws IOException {
        repositionForwardTo(archive.position() + offset);
    }

    private void repositionForwardTo(final long newPosition) throws IOException {
        final long currPosition = archive.position();
        if (newPosition < currPosition) {
            throw new ArchiveException("Trying to move backwards inside of the archive");
        }
        archive.position(newPosition);
    }

    /**
     * Sets whether we are at end-of-file.
     *
     * @param eof whether we are at end-of-file.
     */
    protected final void setAtEOF(final boolean eof) {
        this.eof = eof;
    }

    /**
     * The last record block should be written at the full size, so skip any additional space used to fill a record after an entry
     *
     * @throws IOException when skipping the padding of the record fails
     */
    private void skipRecordPadding() throws IOException {
        if (!isDirectory() && currEntry.getSize() > 0 && currEntry.getSize() % recordSize != 0) {
            final long padding = recordSize - (currEntry.getSize() % recordSize);
            repositionForwardBy(padding);
            throwExceptionIfPositionIsNotInArchive();
        }
    }

    /**
     * Checks if the current position of the SeekableByteChannel is in the archive.
     *
     * @throws IOException If the position is not in the archive
     */
    private void throwExceptionIfPositionIsNotInArchive() throws IOException {
        if (archive.size() < archive.position()) {
            throw new EOFException("Truncated TAR archive: archive should be at least " + archive.position() + " bytes but was " + archive.size() + " bytes");
        }
    }

    /**
     * Tries to read the next record resetting the position in the archive if it is not an EOF record.
     *
     * <p>
     * This is meant to protect against cases where a tar implementation has written only one EOF record when two are expected. Actually this won't help since a
     * non-conforming implementation likely won't fill full blocks consisting of - by default - ten records either so we probably have already read beyond the
     * archive anyway.
     * </p>
     *
     * @throws IOException if reading the record of resetting the position in the archive fails
     */
    private void tryToConsumeSecondEOFRecord() throws IOException {
        boolean shouldReset = true;
        try {
            shouldReset = !isEOFRecord(readRecord());
        } finally {
            if (shouldReset) {
                archive.position(archive.position() - recordSize);
            }
        }
    }

    @Override
    public Iterable<TarArchiveEntry> unwrap() {
        // Commons IO 2.21.0:
        // return asIterable();
        return null;
    }

}
