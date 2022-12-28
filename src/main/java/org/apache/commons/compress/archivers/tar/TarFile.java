/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.commons.compress.archivers.tar;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.compress.archivers.zip.ZipEncoding;
import org.apache.commons.compress.archivers.zip.ZipEncodingHelper;
import org.apache.commons.compress.utils.ArchiveUtils;
import org.apache.commons.compress.utils.BoundedArchiveInputStream;
import org.apache.commons.compress.utils.BoundedInputStream;
import org.apache.commons.compress.utils.BoundedSeekableByteChannelInputStream;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;

/**
 * The TarFile provides random access to UNIX archives.
 * @since 1.21
 */
public class TarFile implements Closeable {

    private final class BoundedTarEntryInputStream extends BoundedArchiveInputStream {

        private final SeekableByteChannel channel;

        private final TarArchiveEntry entry;

        private long entryOffset;

        private int currentSparseInputStreamIndex;

        BoundedTarEntryInputStream(final TarArchiveEntry entry, final SeekableByteChannel channel) throws IOException {
            super(entry.getDataOffset(), entry.getRealSize());
            if (channel.size() - entry.getSize() < entry.getDataOffset()) {
                throw new IOException("entry size exceeds archive size");
            }
            this.entry = entry;
            this.channel = channel;
        }

        @Override
        protected int read(final long pos, final ByteBuffer buf) throws IOException {
            if (entryOffset >= entry.getRealSize()) {
                return -1;
            }

            final int totalRead;
            if (entry.isSparse()) {
                totalRead = readSparse(entryOffset, buf, buf.limit());
            } else {
                totalRead = readArchive(pos, buf);
            }

            if (totalRead == -1) {
                if (buf.array().length > 0) {
                    throw new IOException("Truncated TAR archive");
                }
                setAtEOF(true);
            } else {
                entryOffset += totalRead;
                buf.flip();
            }
            return totalRead;
        }

        private int readArchive(final long pos, final ByteBuffer buf) throws IOException {
            channel.position(pos);
            return channel.read(buf);
        }

        private int readSparse(final long pos, final ByteBuffer buf, final int numToRead) throws IOException {
            // if there are no actual input streams, just read from the original archive
            final List<InputStream> entrySparseInputStreams = sparseInputStreams.get(entry.getName());
            if (entrySparseInputStreams == null || entrySparseInputStreams.isEmpty()) {
                return readArchive(entry.getDataOffset() + pos, buf);
            }

            if (currentSparseInputStreamIndex >= entrySparseInputStreams.size()) {
                return -1;
            }

            final InputStream currentInputStream = entrySparseInputStreams.get(currentSparseInputStreamIndex);
            final byte[] bufArray = new byte[numToRead];
            final int readLen = currentInputStream.read(bufArray);
            if (readLen != -1) {
                buf.put(bufArray, 0, readLen);
            }

            // if the current input stream is the last input stream,
            // just return the number of bytes read from current input stream
            if (currentSparseInputStreamIndex == entrySparseInputStreams.size() - 1) {
                return readLen;
            }

            // if EOF of current input stream is meet, open a new input stream and recursively call read
            if (readLen == -1) {
                currentSparseInputStreamIndex++;
                return readSparse(pos, buf, numToRead);
            }

            // if the rest data of current input stream is not long enough, open a new input stream
            // and recursively call read
            if (readLen < numToRead) {
                currentSparseInputStreamIndex++;
                final int readLenOfNext = readSparse(pos + readLen, buf, numToRead - readLen);
                if (readLenOfNext == -1) {
                    return readLen;
                }

                return readLen + readLenOfNext;
            }

            // if the rest data of current input stream is enough(which means readLen == len), just return readLen
            return readLen;
        }
    }

    private static final int SMALL_BUFFER_SIZE = 256;

    private final byte[] smallBuf = new byte[SMALL_BUFFER_SIZE];

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

    // the global sparse headers, this is only used in PAX Format 0.X
    private final List<TarArchiveStructSparse> globalSparseHeaders = new ArrayList<>();

    private boolean hasHitEOF;

    /**
     * The meta-data about the current entry
     */
    private TarArchiveEntry currEntry;

    // the global PAX header
    private Map<String, String> globalPaxHeaders = new HashMap<>();

    private final Map<String, List<InputStream>> sparseInputStreams = new HashMap<>();

    /**
     * Constructor for TarFile.
     *
     * @param content the content to use
     * @throws IOException when reading the tar archive fails
     */
    public TarFile(final byte[] content) throws IOException {
        this(new SeekableInMemoryByteChannel(content));
    }

    /**
     * Constructor for TarFile.
     *
     * @param content the content to use
     * @param lenient when set to true illegal values for group/userid, mode, device numbers and timestamp will be
     *                ignored and the fields set to {@link TarArchiveEntry#UNKNOWN}. When set to false such illegal fields cause an
     *                exception instead.
     * @throws IOException when reading the tar archive fails
     */
    public TarFile(final byte[] content, final boolean lenient) throws IOException {
        this(new SeekableInMemoryByteChannel(content), TarConstants.DEFAULT_BLKSIZE, TarConstants.DEFAULT_RCDSIZE, null, lenient);
    }

    /**
     * Constructor for TarFile.
     *
     * @param content  the content to use
     * @param encoding the encoding to use
     * @throws IOException when reading the tar archive fails
     */
    public TarFile(final byte[] content, final String encoding) throws IOException {
        this(new SeekableInMemoryByteChannel(content), TarConstants.DEFAULT_BLKSIZE, TarConstants.DEFAULT_RCDSIZE, encoding, false);
    }

    /**
     * Constructor for TarFile.
     *
     * @param archive the file of the archive to use
     * @throws IOException when reading the tar archive fails
     */
    public TarFile(final File archive) throws IOException {
        this(archive.toPath());
    }

    /**
     * Constructor for TarFile.
     *
     * @param archive the file of the archive to use
     * @param lenient when set to true illegal values for group/userid, mode, device numbers and timestamp will be
     *                ignored and the fields set to {@link TarArchiveEntry#UNKNOWN}. When set to false such illegal fields cause an
     *                exception instead.
     * @throws IOException when reading the tar archive fails
     */
    public TarFile(final File archive, final boolean lenient) throws IOException {
        this(archive.toPath(), lenient);
    }

    /**
     * Constructor for TarFile.
     *
     * @param archive  the file of the archive to use
     * @param encoding the encoding to use
     * @throws IOException when reading the tar archive fails
     */
    public TarFile(final File archive, final String encoding) throws IOException {
        this(archive.toPath(), encoding);
    }

    /**
     * Constructor for TarFile.
     *
     * @param archivePath the path of the archive to use
     * @throws IOException when reading the tar archive fails
     */
    public TarFile(final Path archivePath) throws IOException {
        this(Files.newByteChannel(archivePath), TarConstants.DEFAULT_BLKSIZE, TarConstants.DEFAULT_RCDSIZE, null, false);
    }

    /**
     * Constructor for TarFile.
     *
     * @param archivePath the path of the archive to use
     * @param lenient     when set to true illegal values for group/userid, mode, device numbers and timestamp will be
     *                    ignored and the fields set to {@link TarArchiveEntry#UNKNOWN}. When set to false such illegal fields cause an
     *                    exception instead.
     * @throws IOException when reading the tar archive fails
     */
    public TarFile(final Path archivePath, final boolean lenient) throws IOException {
        this(Files.newByteChannel(archivePath), TarConstants.DEFAULT_BLKSIZE, TarConstants.DEFAULT_RCDSIZE, null, lenient);
    }

    /**
     * Constructor for TarFile.
     *
     * @param archivePath the path of the archive to use
     * @param encoding    the encoding to use
     * @throws IOException when reading the tar archive fails
     */
    public TarFile(final Path archivePath, final String encoding) throws IOException {
        this(Files.newByteChannel(archivePath), TarConstants.DEFAULT_BLKSIZE, TarConstants.DEFAULT_RCDSIZE, encoding, false);
    }

    /**
     * Constructor for TarFile.
     *
     * @param content the content to use
     * @throws IOException when reading the tar archive fails
     */
    public TarFile(final SeekableByteChannel content) throws IOException {
        this(content, TarConstants.DEFAULT_BLKSIZE, TarConstants.DEFAULT_RCDSIZE, null, false);
    }

    /**
     * Constructor for TarFile.
     *
     * @param archive    the seekable byte channel to use
     * @param blockSize  the blocks size to use
     * @param recordSize the record size to use
     * @param encoding   the encoding to use
     * @param lenient    when set to true illegal values for group/userid, mode, device numbers and timestamp will be
     *                   ignored and the fields set to {@link TarArchiveEntry#UNKNOWN}. When set to false such illegal fields cause an
     *                   exception instead.
     * @throws IOException when reading the tar archive fails
     */
    public TarFile(final SeekableByteChannel archive, final int blockSize, final int recordSize, final String encoding, final boolean lenient) throws IOException {
        this.archive = archive;
        this.hasHitEOF = false;
        this.zipEncoding = ZipEncodingHelper.getZipEncoding(encoding);
        this.recordSize = recordSize;
        this.recordBuffer = ByteBuffer.allocate(this.recordSize);
        this.blockSize = blockSize;
        this.lenient = lenient;

        TarArchiveEntry entry;
        while ((entry = getNextTarEntry()) != null) {
            entries.add(entry);
        }
    }

    /**
     * Update the current entry with the read pax headers
     * @param headers Headers read from the pax header
     * @param sparseHeaders Sparse headers read from pax header
     */
    private void applyPaxHeadersToCurrentEntry(final Map<String, String> headers, final List<TarArchiveStructSparse> sparseHeaders)
        throws IOException {
        currEntry.updateEntryFromPaxHeaders(headers);
        currEntry.setSparseHeaders(sparseHeaders);
    }

    /**
     * Build the input streams consisting of all-zero input streams and non-zero input streams.
     * When reading from the non-zero input streams, the data is actually read from the original input stream.
     * The size of each input stream is introduced by the sparse headers.
     *
     * @implNote Some all-zero input streams and non-zero input streams have the size of 0. We DO NOT store the
     *        0 size input streams because they are meaningless.
     */
    private void buildSparseInputStreams() throws IOException {
        final List<InputStream> streams = new ArrayList<>();

        final List<TarArchiveStructSparse> sparseHeaders = currEntry.getOrderedSparseHeaders();

        // Stream doesn't need to be closed at all as it doesn't use any resources
        final InputStream zeroInputStream = new TarArchiveSparseZeroInputStream(); //NOSONAR
        // logical offset into the extracted entry
        long offset = 0;
        long numberOfZeroBytesInSparseEntry = 0;
        for (final TarArchiveStructSparse sparseHeader : sparseHeaders) {
            final long zeroBlockSize = sparseHeader.getOffset() - offset;
            if (zeroBlockSize < 0) {
                // sparse header says to move backwards inside of the extracted entry
                throw new IOException("Corrupted struct sparse detected");
            }

            // only store the zero block if it is not empty
            if (zeroBlockSize > 0) {
                streams.add(new BoundedInputStream(zeroInputStream, zeroBlockSize));
                numberOfZeroBytesInSparseEntry += zeroBlockSize;
            }

            // only store the input streams with non-zero size
            if (sparseHeader.getNumbytes() > 0) {
                final long start =
                    currEntry.getDataOffset() + sparseHeader.getOffset() - numberOfZeroBytesInSparseEntry;
                if (start + sparseHeader.getNumbytes() < start) {
                    // possible integer overflow
                    throw new IOException("Unreadable TAR archive, sparse block offset or length too big");
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
     * This method is invoked once the end of the archive is hit, it
     * tries to consume the remaining bytes under the assumption that
     * the tool creating this archive has padded the last block.
     */
    private void consumeRemainderOfLastBlock() throws IOException {
        final long bytesReadOfLastBlock = archive.position() % blockSize;
        if (bytesReadOfLastBlock > 0) {
            repositionForwardBy(blockSize - bytesReadOfLastBlock);
        }
    }

    /**
     * Get all TAR Archive Entries from the TarFile
     *
     * @return All entries from the tar file
     */
    public List<TarArchiveEntry> getEntries() {
        return new ArrayList<>(entries);
    }

    /**
     * Gets the input stream for the provided Tar Archive Entry.
     * @param entry Entry to get the input stream from
     * @return Input stream of the provided entry
     * @throws IOException Corrupted TAR archive. Can't read entry.
     */
    public InputStream getInputStream(final TarArchiveEntry entry) throws IOException {
        try {
            return new BoundedTarEntryInputStream(entry, archive);
        } catch (final RuntimeException ex) {
            throw new IOException("Corrupted TAR archive. Can't read entry", ex);
        }
    }

    /**
     * Get the next entry in this tar archive as longname data.
     *
     * @return The next entry in the archive as longname data, or null.
     * @throws IOException on error
     */
    private byte[] getLongNameData() throws IOException {
        final ByteArrayOutputStream longName = new ByteArrayOutputStream();
        int length;
        try (final InputStream in = getInputStream(currEntry)) {
            while ((length = in.read(smallBuf)) >= 0) {
                longName.write(smallBuf, 0, length);
            }
        }
        getNextTarEntry();
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
     * Get the next entry in this tar archive. This will skip
     * to the end of the current entry, if there is one, and
     * place the position of the channel at the header of the
     * next entry, and read the header and instantiate a new
     * TarEntry from the header bytes and return that entry.
     * If there are no more entries in the archive, null will
     * be returned to indicate that the end of the archive has
     * been reached.
     *
     * @return The next TarEntry in the archive, or null if there is no next entry.
     * @throws IOException when reading the next TarEntry fails
     */
    private TarArchiveEntry getNextTarEntry() throws IOException {
        if (isAtEOF()) {
            return null;
        }

        if (currEntry != null) {
            // Skip to the end of the entry
            repositionForwardTo(currEntry.getDataOffset() + currEntry.getSize());
            throwExceptionIfPositionIsNotInArchive();
            skipRecordPadding();
        }

        final ByteBuffer headerBuf = getRecord();
        if (null == headerBuf) {
            /* hit EOF */
            currEntry = null;
            return null;
        }

        try {
            final long position = archive.position();
            currEntry = new TarArchiveEntry(globalPaxHeaders, headerBuf.array(), zipEncoding, lenient, position);
        } catch (final IllegalArgumentException e) {
            throw new IOException("Error detected parsing the header", e);
        }

        if (currEntry.isGNULongLinkEntry()) {
            final byte[] longLinkData = getLongNameData();
            if (longLinkData == null) {
                // Bugzilla: 40334
                // Malformed tar file - long link entry name not followed by
                // entry
                return null;
            }
            currEntry.setLinkName(zipEncoding.decode(longLinkData));
        }

        if (currEntry.isGNULongNameEntry()) {
            final byte[] longNameData = getLongNameData();
            if (longNameData == null) {
                // Bugzilla: 40334
                // Malformed tar file - long entry name not followed by
                // entry
                return null;
            }

            // COMPRESS-509 : the name of directories should end with '/'
            final String name = zipEncoding.decode(longNameData);
            currEntry.setName(name);
            if (currEntry.isDirectory() && !name.endsWith("/")) {
                currEntry.setName(name + "/");
            }
        }

        if (currEntry.isGlobalPaxHeader()) { // Process Global Pax headers
            readGlobalPaxHeaders();
        }

        try {
            if (currEntry.isPaxHeader()) { // Process Pax headers
                paxHeaders();
            } else if (!globalPaxHeaders.isEmpty()) {
                applyPaxHeadersToCurrentEntry(globalPaxHeaders, globalSparseHeaders);
            }
        } catch (final NumberFormatException e) {
            throw new IOException("Error detected parsing the pax header", e);
        }

        if (currEntry.isOldGNUSparse()) { // Process sparse files
            readOldGNUSparse();
        }

        return currEntry;
    }

    /**
     * Get the next record in this tar archive. This will skip
     * over any remaining data in the current entry, if there
     * is one, and place the input stream at the header of the
     * next entry.
     *
     * <p>If there are no more entries in the archive, null will be
     * returned to indicate that the end of the archive has been
     * reached.  At the same time the {@code hasHitEOF} marker will be
     * set to true.</p>
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

    protected final boolean isAtEOF() {
        return hasHitEOF;
    }

    private boolean isDirectory() {
        return currEntry != null && currEntry.isDirectory();
    }

    private boolean isEOFRecord(final ByteBuffer headerBuf) {
        return headerBuf == null || ArchiveUtils.isArrayZero(headerBuf.array(), recordSize);
    }

    /**
     * <p>
     * For PAX Format 0.0, the sparse headers(GNU.sparse.offset and GNU.sparse.numbytes)
     * may appear multi times, and they look like:
     * <pre>
     * GNU.sparse.size=size
     * GNU.sparse.numblocks=numblocks
     * repeat numblocks times
     *   GNU.sparse.offset=offset
     *   GNU.sparse.numbytes=numbytes
     * end repeat
     * </pre>
     *
     * <p>
     * For PAX Format 0.1, the sparse headers are stored in a single variable : GNU.sparse.map
     * <pre>
     * GNU.sparse.map
     *    Map of non-null data chunks. It is a string consisting of comma-separated values "offset,size[,offset-1,size-1...]"
     * </pre>
     *
     * <p>
     * For PAX Format 1.X:
     * <br>
     * The sparse map itself is stored in the file data block, preceding the actual file data.
     * It consists of a series of decimal numbers delimited by newlines. The map is padded with nulls to the nearest block boundary.
     * The first number gives the number of entries in the map. Following are map entries, each one consisting of two numbers
     * giving the offset and size of the data block it describes.
     * @throws IOException
     */
    private void paxHeaders() throws IOException {
        List<TarArchiveStructSparse> sparseHeaders = new ArrayList<>();
        final Map<String, String> headers;
        try (final InputStream input = getInputStream(currEntry)) {
            headers = TarUtils.parsePaxHeaders(input, sparseHeaders, globalPaxHeaders, currEntry.getSize());
        }

        // for 0.1 PAX Headers
        if (headers.containsKey(TarGnuSparseKeys.MAP)) {
            sparseHeaders = new ArrayList<>(TarUtils.parseFromPAX01SparseHeaders(headers.get(TarGnuSparseKeys.MAP)));
        }
        getNextTarEntry(); // Get the actual file entry
        if (currEntry == null) {
            throw new IOException("premature end of tar archive. Didn't find any entry after PAX header.");
        }
        applyPaxHeadersToCurrentEntry(headers, sparseHeaders);

        // for 1.0 PAX Format, the sparse map is stored in the file data block
        if (currEntry.isPaxGNU1XSparse()) {
            try (final InputStream input = getInputStream(currEntry)) {
                sparseHeaders = TarUtils.parsePAX1XSparseHeaders(input, recordSize);
            }
            currEntry.setSparseHeaders(sparseHeaders);
            // data of the entry is after the pax gnu entry. So we need to update the data position once again
            currEntry.setDataOffset(currEntry.getDataOffset() + recordSize);
        }

        // sparse headers are all done reading, we need to build
        // sparse input streams using these sparse headers
        buildSparseInputStreams();
    }

    private void readGlobalPaxHeaders() throws IOException {
        try (InputStream input = getInputStream(currEntry)) {
            globalPaxHeaders = TarUtils.parsePaxHeaders(input, globalSparseHeaders, globalPaxHeaders,
                currEntry.getSize());
        }
        getNextTarEntry(); // Get the actual file entry

        if (currEntry == null) {
            throw new IOException("Error detected parsing the pax header");
        }
    }

    /**
     * Adds the sparse chunks from the current entry to the sparse chunks,
     * including any additional sparse entries following the current entry.
     *
     * @throws IOException when reading the sparse entry fails
     */
    private void readOldGNUSparse() throws IOException {
        if (currEntry.isExtended()) {
            TarArchiveSparseEntry entry;
            do {
                final ByteBuffer headerBuf = getRecord();
                if (headerBuf == null) {
                    throw new IOException("premature end of tar archive. Didn't find extended_header after header with extended flag.");
                }
                entry = new TarArchiveSparseEntry(headerBuf.array());
                currEntry.getSparseHeaders().addAll(entry.getSparseHeaders());
                currEntry.setDataOffset(currEntry.getDataOffset() + recordSize);
            } while (entry.isExtended());
        }

        // sparse headers are all done reading, we need to build
        // sparse input streams using these sparse headers
        buildSparseInputStreams();
    }

    /**
     * Read a record from the input stream and return the data.
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
            throw new IOException("trying to move backwards inside of the archive");
        }
        archive.position(newPosition);
    }

    protected final void setAtEOF(final boolean b) {
        hasHitEOF = b;
    }

    /**
     * The last record block should be written at the full size, so skip any
     * additional space used to fill a record after an entry
     *
     * @throws IOException when skipping the padding of the record fails
     */
    private void skipRecordPadding() throws IOException {
        if (!isDirectory() && currEntry.getSize() > 0 && currEntry.getSize() % recordSize != 0) {
            final long numRecords = (currEntry.getSize() / recordSize) + 1;
            final long padding = (numRecords * recordSize) - currEntry.getSize();
            repositionForwardBy(padding);
            throwExceptionIfPositionIsNotInArchive();
        }
    }

    /**
     * Checks if the current position of the SeekableByteChannel is in the archive.
     * @throws IOException If the position is not in the archive
     */
    private void throwExceptionIfPositionIsNotInArchive() throws IOException {
        if (archive.size() < archive.position()) {
            throw new IOException("Truncated TAR archive");
        }
    }

    /**
     * Tries to read the next record resetting the position in the
     * archive if it is not a EOF record.
     *
     * <p>This is meant to protect against cases where a tar
     * implementation has written only one EOF record when two are
     * expected. Actually this won't help since a non-conforming
     * implementation likely won't fill full blocks consisting of - by
     * default - ten records either so we probably have already read
     * beyond the archive anyway.</p>
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
}
