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
package org.apache.commons.compress.archivers.sevenz;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.charset.CharsetEncoder;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.zip.CRC32;

import org.apache.commons.compress.utils.BoundedInputStream;
import org.apache.commons.compress.utils.CRC32VerifyingInputStream;
import org.apache.commons.compress.utils.CharsetNames;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.compress.utils.InputStreamStatistics;

/**
 * Reads a 7z file, using SeekableByteChannel under
 * the covers.
 * <p>
 * The 7z file format is a flexible container
 * that can contain many compression and
 * encryption types, but at the moment only
 * only Copy, LZMA, LZMA2, BZIP2, Deflate and AES-256 + SHA-256
 * are supported.
 * <p>
 * The format is very Windows/Intel specific,
 * so it uses little-endian byte order,
 * doesn't store user/group or permission bits,
 * and represents times using NTFS timestamps
 * (100 nanosecond units since 1 January 1601).
 * Hence the official tools recommend against
 * using it for backup purposes on *nix, and
 * recommend .tar.7z or .tar.lzma or .tar.xz
 * instead.
 * <p>
 * Both the header and file contents may be
 * compressed and/or encrypted. With both
 * encrypted, neither file names nor file
 * contents can be read, but the use of
 * encryption isn't plausibly deniable.
 *
 * <p>Multi volume archives can be read by concatenating the parts in
 * correct order - either manually or by using {link
 * org.apache.commons.compress.utils.MultiReadOnlySeekableByteChannel}
 * for example.</p>
 *
 * @NotThreadSafe
 * @since 1.6
 */
public class SevenZFile implements Closeable {
    static final int SIGNATURE_HEADER_SIZE = 32;

    private static final String DEFAULT_FILE_NAME = "unknown archive";

    private final String fileName;
    private SeekableByteChannel channel;
    private final Archive archive;
    private int currentEntryIndex = -1;
    private int currentFolderIndex = -1;
    private InputStream currentFolderInputStream = null;
    private byte[] password;
    private final SevenZFileOptions options;

    private long compressedBytesReadFromCurrentEntry;
    private long uncompressedBytesReadFromCurrentEntry;

    private final ArrayList<InputStream> deferredBlockStreams = new ArrayList<>();

    // shared with SevenZOutputFile and tests, neither mutates it
    static final byte[] sevenZSignature = { //NOSONAR
        (byte)'7', (byte)'z', (byte)0xBC, (byte)0xAF, (byte)0x27, (byte)0x1C
    };

    /**
     * Reads a file as 7z archive
     *
     * @param fileName the file to read
     * @param password optional password if the archive is encrypted
     * @throws IOException if reading the archive fails
     * @since 1.17
     */
    public SevenZFile(final File fileName, final char[] password) throws IOException {
        this(fileName, password, SevenZFileOptions.DEFAULT);
    }

    /**
     * Reads a file as 7z archive with additional options.
     *
     * @param fileName the file to read
     * @param password optional password if the archive is encrypted
     * @param options the options to apply
     * @throws IOException if reading the archive fails or the memory limit (if set) is too small
     * @since 1.19
     */
    public SevenZFile(final File fileName, final char[] password, SevenZFileOptions options) throws IOException {
        this(Files.newByteChannel(fileName.toPath(), EnumSet.of(StandardOpenOption.READ)), // NOSONAR
                fileName.getAbsolutePath(), utf16Decode(password), true, options);
    }

    /**
     * Reads a file as 7z archive
     *
     * @param fileName the file to read
     * @param password optional password if the archive is encrypted -
     * the byte array is supposed to be the UTF16-LE encoded
     * representation of the password.
     * @throws IOException if reading the archive fails
     * @deprecated use the char[]-arg version for the password instead
     */
    @Deprecated
    public SevenZFile(final File fileName, final byte[] password) throws IOException {
        this(Files.newByteChannel(fileName.toPath(), EnumSet.of(StandardOpenOption.READ)),
                fileName.getAbsolutePath(), password, true, SevenZFileOptions.DEFAULT);
    }

    /**
     * Reads a SeekableByteChannel as 7z archive
     *
     * <p>{@link
     * org.apache.commons.compress.utils.SeekableInMemoryByteChannel}
     * allows you to read from an in-memory archive.</p>
     *
     * @param channel the channel to read
     * @throws IOException if reading the archive fails
     * @since 1.13
     */
    public SevenZFile(final SeekableByteChannel channel) throws IOException {
        this(channel, SevenZFileOptions.DEFAULT);
    }

    /**
     * Reads a SeekableByteChannel as 7z archive with addtional options.
     *
     * <p>{@link
     * org.apache.commons.compress.utils.SeekableInMemoryByteChannel}
     * allows you to read from an in-memory archive.</p>
     *
     * @param channel the channel to read
     * @param options the options to apply
     * @throws IOException if reading the archive fails or the memory limit (if set) is too small
     * @since 1.19
     */
    public SevenZFile(final SeekableByteChannel channel, SevenZFileOptions options) throws IOException {
        this(channel, DEFAULT_FILE_NAME, (char[]) null, options);
    }

    /**
     * Reads a SeekableByteChannel as 7z archive
     *
     * <p>{@link
     * org.apache.commons.compress.utils.SeekableInMemoryByteChannel}
     * allows you to read from an in-memory archive.</p>
     *
     * @param channel the channel to read
     * @param password optional password if the archive is encrypted
     * @throws IOException if reading the archive fails
     * @since 1.17
     */
    public SevenZFile(final SeekableByteChannel channel,
                      final char[] password) throws IOException {
        this(channel, password, SevenZFileOptions.DEFAULT);
    }

    /**
     * Reads a SeekableByteChannel as 7z archive with additional options.
     *
     * <p>{@link
     * org.apache.commons.compress.utils.SeekableInMemoryByteChannel}
     * allows you to read from an in-memory archive.</p>
     *
     * @param channel the channel to read
     * @param password optional password if the archive is encrypted
     * @param options the options to apply
     * @throws IOException if reading the archive fails or the memory limit (if set) is too small
     * @since 1.19
     */
    public SevenZFile(final SeekableByteChannel channel, final char[] password, final SevenZFileOptions options)
            throws IOException {
        this(channel, DEFAULT_FILE_NAME, password, options);
    }

    /**
     * Reads a SeekableByteChannel as 7z archive
     *
     * <p>{@link
     * org.apache.commons.compress.utils.SeekableInMemoryByteChannel}
     * allows you to read from an in-memory archive.</p>
     *
     * @param channel the channel to read
     * @param fileName name of the archive - only used for error reporting
     * @param password optional password if the archive is encrypted
     * @throws IOException if reading the archive fails
     * @since 1.17
     */
    public SevenZFile(final SeekableByteChannel channel, String fileName,
                      final char[] password) throws IOException {
        this(channel, fileName, password, SevenZFileOptions.DEFAULT);
    }

    /**
     * Reads a SeekableByteChannel as 7z archive with addtional options.
     *
     * <p>{@link
     * org.apache.commons.compress.utils.SeekableInMemoryByteChannel}
     * allows you to read from an in-memory archive.</p>
     *
     * @param channel the channel to read
     * @param fileName name of the archive - only used for error reporting
     * @param password optional password if the archive is encrypted
     * @param options the options to apply
     * @throws IOException if reading the archive fails or the memory limit (if set) is too small
     * @since 1.19
     */
    public SevenZFile(final SeekableByteChannel channel, String fileName, final char[] password,
            final SevenZFileOptions options) throws IOException {
        this(channel, fileName, utf16Decode(password), false, options);
    }

    /**
     * Reads a SeekableByteChannel as 7z archive
     *
     * <p>{@link
     * org.apache.commons.compress.utils.SeekableInMemoryByteChannel}
     * allows you to read from an in-memory archive.</p>
     *
     * @param channel the channel to read
     * @param fileName name of the archive - only used for error reporting
     * @throws IOException if reading the archive fails
     * @since 1.17
     */
    public SevenZFile(final SeekableByteChannel channel, String fileName)
        throws IOException {
        this(channel, fileName, SevenZFileOptions.DEFAULT);
    }

    /**
     * Reads a SeekableByteChannel as 7z archive with additional options.
     *
     * <p>{@link
     * org.apache.commons.compress.utils.SeekableInMemoryByteChannel}
     * allows you to read from an in-memory archive.</p>
     *
     * @param channel the channel to read
     * @param fileName name of the archive - only used for error reporting
     * @param options the options to apply
     * @throws IOException if reading the archive fails or the memory limit (if set) is too small
     * @since 1.19
     */
    public SevenZFile(final SeekableByteChannel channel, String fileName, final SevenZFileOptions options)
            throws IOException {
        this(channel, fileName, null, false, options);
    }

    /**
     * Reads a SeekableByteChannel as 7z archive
     *
     * <p>{@link
     * org.apache.commons.compress.utils.SeekableInMemoryByteChannel}
     * allows you to read from an in-memory archive.</p>
     *
     * @param channel the channel to read
     * @param password optional password if the archive is encrypted -
     * the byte array is supposed to be the UTF16-LE encoded
     * representation of the password.
     * @throws IOException if reading the archive fails
     * @since 1.13
     * @deprecated use the char[]-arg version for the password instead
     */
    @Deprecated
    public SevenZFile(final SeekableByteChannel channel,
                      final byte[] password) throws IOException {
        this(channel, DEFAULT_FILE_NAME, password);
    }

    /**
     * Reads a SeekableByteChannel as 7z archive
     *
     * <p>{@link
     * org.apache.commons.compress.utils.SeekableInMemoryByteChannel}
     * allows you to read from an in-memory archive.</p>
     *
     * @param channel the channel to read
     * @param fileName name of the archive - only used for error reporting
     * @param password optional password if the archive is encrypted -
     * the byte array is supposed to be the UTF16-LE encoded
     * representation of the password.
     * @throws IOException if reading the archive fails
     * @since 1.13
     * @deprecated use the char[]-arg version for the password instead
     */
    @Deprecated
    public SevenZFile(final SeekableByteChannel channel, String fileName,
                      final byte[] password) throws IOException {
        this(channel, fileName, password, false, SevenZFileOptions.DEFAULT);
    }

    private SevenZFile(final SeekableByteChannel channel, String filename,
                       final byte[] password, boolean closeOnError, SevenZFileOptions options) throws IOException {
        boolean succeeded = false;
        this.channel = channel;
        this.fileName = filename;
        this.options = options;
        try {
            archive = readHeaders(password);
            if (password != null) {
                this.password = Arrays.copyOf(password, password.length);
            } else {
                this.password = null;
            }
            succeeded = true;
        } finally {
            if (!succeeded && closeOnError) {
                this.channel.close();
            }
        }
    }

    /**
     * Reads a file as unencrypted 7z archive
     *
     * @param fileName the file to read
     * @throws IOException if reading the archive fails
     */
    public SevenZFile(final File fileName) throws IOException {
        this(fileName, SevenZFileOptions.DEFAULT);
    }

    /**
     * Reads a file as unencrypted 7z archive
     *
     * @param fileName the file to read
     * @param options the options to apply
     * @throws IOException if reading the archive fails or the memory limit (if set) is too small
     * @since 1.19
     */
    public SevenZFile(final File fileName, final SevenZFileOptions options) throws IOException {
        this(fileName, (char[]) null, options);
    }

    /**
     * Closes the archive.
     * @throws IOException if closing the file fails
     */
    @Override
    public void close() throws IOException {
        if (channel != null) {
            try {
                channel.close();
            } finally {
                channel = null;
                if (password != null) {
                    Arrays.fill(password, (byte) 0);
                }
                password = null;
            }
        }
    }

    /**
     * Returns the next Archive Entry in this archive.
     *
     * @return the next entry,
     *         or {@code null} if there are no more entries
     * @throws IOException if the next entry could not be read
     */
    public SevenZArchiveEntry getNextEntry() throws IOException {
        if (currentEntryIndex >= archive.files.length - 1) {
            return null;
        }
        ++currentEntryIndex;
        final SevenZArchiveEntry entry = archive.files[currentEntryIndex];
        if (entry.getName() == null && options.getUseDefaultNameForUnnamedEntries()) {
            entry.setName(getDefaultName());
        }
        buildDecodingStream(currentEntryIndex);
        uncompressedBytesReadFromCurrentEntry = compressedBytesReadFromCurrentEntry = 0;
        return entry;
    }

    /**
     * Returns meta-data of all archive entries.
     *
     * <p>This method only provides meta-data, the entries can not be
     * used to read the contents, you still need to process all
     * entries in order using {@link #getNextEntry} for that.</p>
     *
     * <p>The content methods are only available for entries that have
     * already been reached via {@link #getNextEntry}.</p>
     *
     * @return meta-data of all archive entries.
     * @since 1.11
     */
    public Iterable<SevenZArchiveEntry> getEntries() {
        return Arrays.asList(archive.files);
    }

    private Archive readHeaders(final byte[] password) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(12 /* signature + 2 bytes version + 4 bytes CRC */)
            .order(ByteOrder.LITTLE_ENDIAN);
        readFully(buf);
        final byte[] signature = new byte[6];
        buf.get(signature);
        if (!Arrays.equals(signature, sevenZSignature)) {
            throw new IOException("Bad 7z signature");
        }
        // 7zFormat.txt has it wrong - it's first major then minor
        final byte archiveVersionMajor = buf.get();
        final byte archiveVersionMinor = buf.get();
        if (archiveVersionMajor != 0) {
            throw new IOException(String.format("Unsupported 7z version (%d,%d)",
                    archiveVersionMajor, archiveVersionMinor));
        }

        boolean headerLooksValid = false;  // See https://www.7-zip.org/recover.html - "There is no correct End Header at the end of archive"
        final long startHeaderCrc = 0xffffFFFFL & buf.getInt();
        if (startHeaderCrc == 0) {
            // This is an indication of a corrupt header - peek the next 20 bytes
            long currentPosition = channel.position();
            ByteBuffer peekBuf = ByteBuffer.allocate(20);
            readFully(peekBuf);
            channel.position(currentPosition);
            // Header invalid if all data is 0
            while (peekBuf.hasRemaining()) {
                if (peekBuf.get()!=0) {
                    headerLooksValid = true;
                    break;
                }
            }
        } else {
            headerLooksValid = true;
        }

        if (headerLooksValid) {
            final StartHeader startHeader = readStartHeader(startHeaderCrc);
            return initializeArchive(startHeader, password, true);
        } else {
            // No valid header found - probably first file of multipart archive was removed too early. Scan for end header.
            return tryToLocateEndHeader(password);
        }
    }

    private Archive tryToLocateEndHeader(final byte[] password) throws IOException {
        ByteBuffer nidBuf = ByteBuffer.allocate(1);
        final long searchLimit = 1024l * 1024 * 1;
        // Main header, plus bytes that readStartHeader would read
        final long previousDataSize = channel.position() + 20;
        final long minPos;
        // Determine minimal position - can't start before current position
        if (channel.position() + searchLimit > channel.size()) {
            minPos = channel.position();
        } else {
            minPos = channel.size() - searchLimit;
        }
        long pos = channel.size() - 1;
        // Loop: Try from end of archive
        while (pos > minPos) {
            pos--;
            channel.position(pos);
            nidBuf.rewind();
            channel.read(nidBuf);
            int nid = nidBuf.array()[0];
            // First indicator: Byte equals one of these header identifiers
            if (nid == NID.kEncodedHeader || nid == NID.kHeader) {
                try {
                    // Try to initialize Archive structure from here
                    final StartHeader startHeader = new StartHeader();
                    startHeader.nextHeaderOffset = pos - previousDataSize;
                    startHeader.nextHeaderSize = channel.size() - pos;
                    Archive result = initializeArchive(startHeader, password, false);
                    // Sanity check: There must be some data...
                    if (result.packSizes != null && result.files.length > 0) {
                        return result;
                    }
                } catch (Exception ignore) {
                    // Wrong guess...
                }
            }
        }
        throw new IOException("Start header corrupt and unable to guess end header");
    }

    private Archive initializeArchive(StartHeader startHeader, final byte[] password, boolean verifyCrc) throws IOException {
        assertFitsIntoInt("nextHeaderSize", startHeader.nextHeaderSize);
        final int nextHeaderSizeInt = (int) startHeader.nextHeaderSize;
        channel.position(SIGNATURE_HEADER_SIZE + startHeader.nextHeaderOffset);
        ByteBuffer buf = ByteBuffer.allocate(nextHeaderSizeInt).order(ByteOrder.LITTLE_ENDIAN);
        readFully(buf);
        if (verifyCrc) {
            final CRC32 crc = new CRC32();
            crc.update(buf.array());
            if (startHeader.nextHeaderCrc != crc.getValue()) {
                throw new IOException("NextHeader CRC mismatch");
            }
        }

        Archive archive = new Archive();
        int nid = getUnsignedByte(buf);
        if (nid == NID.kEncodedHeader) {
            buf = readEncodedHeader(buf, archive, password);
            // Archive gets rebuilt with the new header
            archive = new Archive();
            nid = getUnsignedByte(buf);
        }
        if (nid == NID.kHeader) {
            readHeader(buf, archive);
        } else {
            throw new IOException("Broken or unsupported archive: no Header");
        }
        return archive;
    }

    private StartHeader readStartHeader(final long startHeaderCrc) throws IOException {
        final StartHeader startHeader = new StartHeader();
        // using Stream rather than ByteBuffer for the benefit of the
        // built-in CRC check
        try (DataInputStream dataInputStream = new DataInputStream(new CRC32VerifyingInputStream(
                new BoundedSeekableByteChannelInputStream(channel, 20), 20, startHeaderCrc))) {
             startHeader.nextHeaderOffset = Long.reverseBytes(dataInputStream.readLong());
             startHeader.nextHeaderSize = Long.reverseBytes(dataInputStream.readLong());
             startHeader.nextHeaderCrc = 0xffffFFFFL & Integer.reverseBytes(dataInputStream.readInt());
             return startHeader;
        }
    }

    private void readHeader(final ByteBuffer header, final Archive archive) throws IOException {
        int nid = getUnsignedByte(header);

        if (nid == NID.kArchiveProperties) {
            readArchiveProperties(header);
            nid = getUnsignedByte(header);
        }

        if (nid == NID.kAdditionalStreamsInfo) {
            throw new IOException("Additional streams unsupported");
            //nid = header.readUnsignedByte();
        }

        if (nid == NID.kMainStreamsInfo) {
            readStreamsInfo(header, archive);
            nid = getUnsignedByte(header);
        }

        if (nid == NID.kFilesInfo) {
            readFilesInfo(header, archive);
            nid = getUnsignedByte(header);
        }

        if (nid != NID.kEnd) {
            throw new IOException("Badly terminated header, found " + nid);
        }
    }

    private void readArchiveProperties(final ByteBuffer input) throws IOException {
        // FIXME: the reference implementation just throws them away?
        int nid =  getUnsignedByte(input);
        while (nid != NID.kEnd) {
            final long propertySize = readUint64(input);
            assertFitsIntoInt("propertySize", propertySize);
            final byte[] property = new byte[(int)propertySize];
            input.get(property);
            nid = getUnsignedByte(input);
        }
    }

    private ByteBuffer readEncodedHeader(final ByteBuffer header, final Archive archive,
                                         final byte[] password) throws IOException {
        readStreamsInfo(header, archive);

        // FIXME: merge with buildDecodingStream()/buildDecoderStack() at some stage?
        final Folder folder = archive.folders[0];
        final int firstPackStreamIndex = 0;
        final long folderOffset = SIGNATURE_HEADER_SIZE + archive.packPos +
                0;

        channel.position(folderOffset);
        InputStream inputStreamStack = new BoundedSeekableByteChannelInputStream(channel,
                archive.packSizes[firstPackStreamIndex]);
        for (final Coder coder : folder.getOrderedCoders()) {
            if (coder.numInStreams != 1 || coder.numOutStreams != 1) {
                throw new IOException("Multi input/output stream coders are not yet supported");
            }
            inputStreamStack = Coders.addDecoder(fileName, inputStreamStack, //NOSONAR
                    folder.getUnpackSizeForCoder(coder), coder, password, options.getMaxMemoryLimitInKb());
        }
        if (folder.hasCrc) {
            inputStreamStack = new CRC32VerifyingInputStream(inputStreamStack,
                    folder.getUnpackSize(), folder.crc);
        }
        assertFitsIntoInt("unpackSize", folder.getUnpackSize());
        final byte[] nextHeader = new byte[(int)folder.getUnpackSize()];
        try (DataInputStream nextHeaderInputStream = new DataInputStream(inputStreamStack)) {
            nextHeaderInputStream.readFully(nextHeader);
        }
        return ByteBuffer.wrap(nextHeader).order(ByteOrder.LITTLE_ENDIAN);
    }

    private void readStreamsInfo(final ByteBuffer header, final Archive archive) throws IOException {
        int nid = getUnsignedByte(header);

        if (nid == NID.kPackInfo) {
            readPackInfo(header, archive);
            nid = getUnsignedByte(header);
        }

        if (nid == NID.kUnpackInfo) {
            readUnpackInfo(header, archive);
            nid = getUnsignedByte(header);
        } else {
            // archive without unpack/coders info
            archive.folders = new Folder[0];
        }

        if (nid == NID.kSubStreamsInfo) {
            readSubStreamsInfo(header, archive);
            nid = getUnsignedByte(header);
        }

        if (nid != NID.kEnd) {
            throw new IOException("Badly terminated StreamsInfo");
        }
    }

    private void readPackInfo(final ByteBuffer header, final Archive archive) throws IOException {
        archive.packPos = readUint64(header);
        final long numPackStreams = readUint64(header);
        assertFitsIntoInt("numPackStreams", numPackStreams);
        final int numPackStreamsInt = (int) numPackStreams;
        int nid = getUnsignedByte(header);
        if (nid == NID.kSize) {
            archive.packSizes = new long[numPackStreamsInt];
            for (int i = 0; i < archive.packSizes.length; i++) {
                archive.packSizes[i] = readUint64(header);
            }
            nid = getUnsignedByte(header);
        }

        if (nid == NID.kCRC) {
            archive.packCrcsDefined = readAllOrBits(header, numPackStreamsInt);
            archive.packCrcs = new long[numPackStreamsInt];
            for (int i = 0; i < numPackStreamsInt; i++) {
                if (archive.packCrcsDefined.get(i)) {
                    archive.packCrcs[i] = 0xffffFFFFL & header.getInt();
                }
            }

            nid = getUnsignedByte(header);
        }

        if (nid != NID.kEnd) {
            throw new IOException("Badly terminated PackInfo (" + nid + ")");
        }
    }

    private void readUnpackInfo(final ByteBuffer header, final Archive archive) throws IOException {
        int nid = getUnsignedByte(header);
        if (nid != NID.kFolder) {
            throw new IOException("Expected kFolder, got " + nid);
        }
        final long numFolders = readUint64(header);
        assertFitsIntoInt("numFolders", numFolders);
        final int numFoldersInt = (int) numFolders;
        final Folder[] folders = new Folder[numFoldersInt];
        archive.folders = folders;
        final int external = getUnsignedByte(header);
        if (external != 0) {
            throw new IOException("External unsupported");
        }
        for (int i = 0; i < numFoldersInt; i++) {
            folders[i] = readFolder(header);
        }

        nid = getUnsignedByte(header);
        if (nid != NID.kCodersUnpackSize) {
            throw new IOException("Expected kCodersUnpackSize, got " + nid);
        }
        for (final Folder folder : folders) {
            assertFitsIntoInt("totalOutputStreams", folder.totalOutputStreams);
            folder.unpackSizes = new long[(int)folder.totalOutputStreams];
            for (int i = 0; i < folder.totalOutputStreams; i++) {
                folder.unpackSizes[i] = readUint64(header);
            }
        }

        nid = getUnsignedByte(header);
        if (nid == NID.kCRC) {
            final BitSet crcsDefined = readAllOrBits(header, numFoldersInt);
            for (int i = 0; i < numFoldersInt; i++) {
                if (crcsDefined.get(i)) {
                    folders[i].hasCrc = true;
                    folders[i].crc = 0xffffFFFFL & header.getInt();
                } else {
                    folders[i].hasCrc = false;
                }
            }

            nid = getUnsignedByte(header);
        }

        if (nid != NID.kEnd) {
            throw new IOException("Badly terminated UnpackInfo");
        }
    }

    private void readSubStreamsInfo(final ByteBuffer header, final Archive archive) throws IOException {
        for (final Folder folder : archive.folders) {
            folder.numUnpackSubStreams = 1;
        }
        int totalUnpackStreams = archive.folders.length;

        int nid = getUnsignedByte(header);
        if (nid == NID.kNumUnpackStream) {
            totalUnpackStreams = 0;
            for (final Folder folder : archive.folders) {
                final long numStreams = readUint64(header);
                assertFitsIntoInt("numStreams", numStreams);
                folder.numUnpackSubStreams = (int)numStreams;
                totalUnpackStreams += numStreams;
            }
            nid = getUnsignedByte(header);
        }

        final SubStreamsInfo subStreamsInfo = new SubStreamsInfo();
        subStreamsInfo.unpackSizes = new long[totalUnpackStreams];
        subStreamsInfo.hasCrc = new BitSet(totalUnpackStreams);
        subStreamsInfo.crcs = new long[totalUnpackStreams];

        int nextUnpackStream = 0;
        for (final Folder folder : archive.folders) {
            if (folder.numUnpackSubStreams == 0) {
                continue;
            }
            long sum = 0;
            if (nid == NID.kSize) {
                for (int i = 0; i < folder.numUnpackSubStreams - 1; i++) {
                    final long size = readUint64(header);
                    subStreamsInfo.unpackSizes[nextUnpackStream++] = size;
                    sum += size;
                }
            }
            subStreamsInfo.unpackSizes[nextUnpackStream++] = folder.getUnpackSize() - sum;
        }
        if (nid == NID.kSize) {
            nid = getUnsignedByte(header);
        }

        int numDigests = 0;
        for (final Folder folder : archive.folders) {
            if (folder.numUnpackSubStreams != 1 || !folder.hasCrc) {
                numDigests += folder.numUnpackSubStreams;
            }
        }

        if (nid == NID.kCRC) {
            final BitSet hasMissingCrc = readAllOrBits(header, numDigests);
            final long[] missingCrcs = new long[numDigests];
            for (int i = 0; i < numDigests; i++) {
                if (hasMissingCrc.get(i)) {
                    missingCrcs[i] = 0xffffFFFFL & header.getInt();
                }
            }
            int nextCrc = 0;
            int nextMissingCrc = 0;
            for (final Folder folder: archive.folders) {
                if (folder.numUnpackSubStreams == 1 && folder.hasCrc) {
                    subStreamsInfo.hasCrc.set(nextCrc, true);
                    subStreamsInfo.crcs[nextCrc] = folder.crc;
                    ++nextCrc;
                } else {
                    for (int i = 0; i < folder.numUnpackSubStreams; i++) {
                        subStreamsInfo.hasCrc.set(nextCrc, hasMissingCrc.get(nextMissingCrc));
                        subStreamsInfo.crcs[nextCrc] = missingCrcs[nextMissingCrc];
                        ++nextCrc;
                        ++nextMissingCrc;
                    }
                }
            }

            nid = getUnsignedByte(header);
        }

        if (nid != NID.kEnd) {
            throw new IOException("Badly terminated SubStreamsInfo");
        }

        archive.subStreamsInfo = subStreamsInfo;
    }

    private Folder readFolder(final ByteBuffer header) throws IOException {
        final Folder folder = new Folder();

        final long numCoders = readUint64(header);
        assertFitsIntoInt("numCoders", numCoders);
        final Coder[] coders = new Coder[(int)numCoders];
        long totalInStreams = 0;
        long totalOutStreams = 0;
        for (int i = 0; i < coders.length; i++) {
            coders[i] = new Coder();
            final int bits = getUnsignedByte(header);
            final int idSize = bits & 0xf;
            final boolean isSimple = (bits & 0x10) == 0;
            final boolean hasAttributes = (bits & 0x20) != 0;
            final boolean moreAlternativeMethods = (bits & 0x80) != 0;

            coders[i].decompressionMethodId = new byte[idSize];
            header.get(coders[i].decompressionMethodId);
            if (isSimple) {
                coders[i].numInStreams = 1;
                coders[i].numOutStreams = 1;
            } else {
                coders[i].numInStreams = readUint64(header);
                coders[i].numOutStreams = readUint64(header);
            }
            totalInStreams += coders[i].numInStreams;
            totalOutStreams += coders[i].numOutStreams;
            if (hasAttributes) {
                final long propertiesSize = readUint64(header);
                assertFitsIntoInt("propertiesSize", propertiesSize);
                coders[i].properties = new byte[(int)propertiesSize];
                header.get(coders[i].properties);
            }
            // would need to keep looping as above:
            while (moreAlternativeMethods) {
                throw new IOException("Alternative methods are unsupported, please report. " + // NOSONAR
                    "The reference implementation doesn't support them either.");
            }
        }
        folder.coders = coders;
        assertFitsIntoInt("totalInStreams", totalInStreams);
        folder.totalInputStreams = totalInStreams;
        assertFitsIntoInt("totalOutStreams", totalOutStreams);
        folder.totalOutputStreams = totalOutStreams;

        if (totalOutStreams == 0) {
            throw new IOException("Total output streams can't be 0");
        }
        final long numBindPairs = totalOutStreams - 1;
        assertFitsIntoInt("numBindPairs", numBindPairs);
        final BindPair[] bindPairs = new BindPair[(int)numBindPairs];
        for (int i = 0; i < bindPairs.length; i++) {
            bindPairs[i] = new BindPair();
            bindPairs[i].inIndex = readUint64(header);
            bindPairs[i].outIndex = readUint64(header);
        }
        folder.bindPairs = bindPairs;

        if (totalInStreams < numBindPairs) {
            throw new IOException("Total input streams can't be less than the number of bind pairs");
        }
        final long numPackedStreams = totalInStreams - numBindPairs;
        assertFitsIntoInt("numPackedStreams", numPackedStreams);
        final long packedStreams[] = new long[(int)numPackedStreams];
        if (numPackedStreams == 1) {
            int i;
            for (i = 0; i < (int)totalInStreams; i++) {
                if (folder.findBindPairForInStream(i) < 0) {
                    break;
                }
            }
            if (i == (int)totalInStreams) {
                throw new IOException("Couldn't find stream's bind pair index");
            }
            packedStreams[0] = i;
        } else {
            for (int i = 0; i < (int)numPackedStreams; i++) {
                packedStreams[i] = readUint64(header);
            }
        }
        folder.packedStreams = packedStreams;

        return folder;
    }

    private BitSet readAllOrBits(final ByteBuffer header, final int size) throws IOException {
        final int areAllDefined = getUnsignedByte(header);
        final BitSet bits;
        if (areAllDefined != 0) {
            bits = new BitSet(size);
            for (int i = 0; i < size; i++) {
                bits.set(i, true);
            }
        } else {
            bits = readBits(header, size);
        }
        return bits;
    }

    private BitSet readBits(final ByteBuffer header, final int size) throws IOException {
        final BitSet bits = new BitSet(size);
        int mask = 0;
        int cache = 0;
        for (int i = 0; i < size; i++) {
            if (mask == 0) {
                mask = 0x80;
                cache = getUnsignedByte(header);
            }
            bits.set(i, (cache & mask) != 0);
            mask >>>= 1;
        }
        return bits;
    }

    private void readFilesInfo(final ByteBuffer header, final Archive archive) throws IOException {
        final long numFiles = readUint64(header);
        assertFitsIntoInt("numFiles", numFiles);
        final SevenZArchiveEntry[] files = new SevenZArchiveEntry[(int)numFiles];
        for (int i = 0; i < files.length; i++) {
            files[i] = new SevenZArchiveEntry();
        }
        BitSet isEmptyStream = null;
        BitSet isEmptyFile = null;
        BitSet isAnti = null;
        while (true) {
            final int propertyType = getUnsignedByte(header);
            if (propertyType == 0) {
                break;
            }
            final long size = readUint64(header);
            switch (propertyType) {
                case NID.kEmptyStream: {
                    isEmptyStream = readBits(header, files.length);
                    break;
                }
                case NID.kEmptyFile: {
                    if (isEmptyStream == null) { // protect against NPE
                        throw new IOException("Header format error: kEmptyStream must appear before kEmptyFile");
                    }
                    isEmptyFile = readBits(header, isEmptyStream.cardinality());
                    break;
                }
                case NID.kAnti: {
                    if (isEmptyStream == null) { // protect against NPE
                        throw new IOException("Header format error: kEmptyStream must appear before kAnti");
                    }
                    isAnti = readBits(header, isEmptyStream.cardinality());
                    break;
                }
                case NID.kName: {
                    final int external = getUnsignedByte(header);
                    if (external != 0) {
                        throw new IOException("Not implemented");
                    }
                    if (((size - 1) & 1) != 0) {
                        throw new IOException("File names length invalid");
                    }
                    assertFitsIntoInt("file names length", size - 1);
                    final byte[] names = new byte[(int)(size - 1)];
                    header.get(names);
                    int nextFile = 0;
                    int nextName = 0;
                    for (int i = 0; i < names.length; i += 2) {
                        if (names[i] == 0 && names[i+1] == 0) {
                            files[nextFile++].setName(new String(names, nextName, i-nextName, CharsetNames.UTF_16LE));
                            nextName = i + 2;
                        }
                    }
                    if (nextName != names.length || nextFile != files.length) {
                        throw new IOException("Error parsing file names");
                    }
                    break;
                }
                case NID.kCTime: {
                    final BitSet timesDefined = readAllOrBits(header, files.length);
                    final int external = getUnsignedByte(header);
                    if (external != 0) {
                        throw new IOException("Unimplemented");
                    }
                    for (int i = 0; i < files.length; i++) {
                        files[i].setHasCreationDate(timesDefined.get(i));
                        if (files[i].getHasCreationDate()) {
                            files[i].setCreationDate(header.getLong());
                        }
                    }
                    break;
                }
                case NID.kATime: {
                    final BitSet timesDefined = readAllOrBits(header, files.length);
                    final int external = getUnsignedByte(header);
                    if (external != 0) {
                        throw new IOException("Unimplemented");
                    }
                    for (int i = 0; i < files.length; i++) {
                        files[i].setHasAccessDate(timesDefined.get(i));
                        if (files[i].getHasAccessDate()) {
                            files[i].setAccessDate(header.getLong());
                        }
                    }
                    break;
                }
                case NID.kMTime: {
                    final BitSet timesDefined = readAllOrBits(header, files.length);
                    final int external = getUnsignedByte(header);
                    if (external != 0) {
                        throw new IOException("Unimplemented");
                    }
                    for (int i = 0; i < files.length; i++) {
                        files[i].setHasLastModifiedDate(timesDefined.get(i));
                        if (files[i].getHasLastModifiedDate()) {
                            files[i].setLastModifiedDate(header.getLong());
                        }
                    }
                    break;
                }
                case NID.kWinAttributes: {
                    final BitSet attributesDefined = readAllOrBits(header, files.length);
                    final int external = getUnsignedByte(header);
                    if (external != 0) {
                        throw new IOException("Unimplemented");
                    }
                    for (int i = 0; i < files.length; i++) {
                        files[i].setHasWindowsAttributes(attributesDefined.get(i));
                        if (files[i].getHasWindowsAttributes()) {
                            files[i].setWindowsAttributes(header.getInt());
                        }
                    }
                    break;
                }
                case NID.kStartPos: {
                    throw new IOException("kStartPos is unsupported, please report");
                }
                case NID.kDummy: {
                    // 7z 9.20 asserts the content is all zeros and ignores the property
                    // Compress up to 1.8.1 would throw an exception, now we ignore it (see COMPRESS-287

                    if (skipBytesFully(header, size) < size) {
                        throw new IOException("Incomplete kDummy property");
                    }
                    break;
                }

                default: {
                    // Compress up to 1.8.1 would throw an exception, now we ignore it (see COMPRESS-287
                    if (skipBytesFully(header, size) < size) {
                        throw new IOException("Incomplete property of type " + propertyType);
                    }
                    break;
                }
            }
        }
        int nonEmptyFileCounter = 0;
        int emptyFileCounter = 0;
        for (int i = 0; i < files.length; i++) {
            files[i].setHasStream(isEmptyStream == null || !isEmptyStream.get(i));
            if (files[i].hasStream()) {
                if (archive.subStreamsInfo == null) {
                    throw new IOException("Archive contains file with streams but no subStreamsInfo");
                }
                files[i].setDirectory(false);
                files[i].setAntiItem(false);
                files[i].setHasCrc(archive.subStreamsInfo.hasCrc.get(nonEmptyFileCounter));
                files[i].setCrcValue(archive.subStreamsInfo.crcs[nonEmptyFileCounter]);
                files[i].setSize(archive.subStreamsInfo.unpackSizes[nonEmptyFileCounter]);
                ++nonEmptyFileCounter;
            } else {
                files[i].setDirectory(isEmptyFile == null || !isEmptyFile.get(emptyFileCounter));
                files[i].setAntiItem(isAnti != null && isAnti.get(emptyFileCounter));
                files[i].setHasCrc(false);
                files[i].setSize(0);
                ++emptyFileCounter;
            }
        }
        archive.files = files;
        calculateStreamMap(archive);
    }

    private void calculateStreamMap(final Archive archive) throws IOException {
        final StreamMap streamMap = new StreamMap();

        int nextFolderPackStreamIndex = 0;
        final int numFolders = archive.folders != null ? archive.folders.length : 0;
        streamMap.folderFirstPackStreamIndex = new int[numFolders];
        for (int i = 0; i < numFolders; i++) {
            streamMap.folderFirstPackStreamIndex[i] = nextFolderPackStreamIndex;
            nextFolderPackStreamIndex += archive.folders[i].packedStreams.length;
        }

        long nextPackStreamOffset = 0;
        final int numPackSizes = archive.packSizes != null ? archive.packSizes.length : 0;
        streamMap.packStreamOffsets = new long[numPackSizes];
        for (int i = 0; i < numPackSizes; i++) {
            streamMap.packStreamOffsets[i] = nextPackStreamOffset;
            nextPackStreamOffset += archive.packSizes[i];
        }

        streamMap.folderFirstFileIndex = new int[numFolders];
        streamMap.fileFolderIndex = new int[archive.files.length];
        int nextFolderIndex = 0;
        int nextFolderUnpackStreamIndex = 0;
        for (int i = 0; i < archive.files.length; i++) {
            if (!archive.files[i].hasStream() && nextFolderUnpackStreamIndex == 0) {
                streamMap.fileFolderIndex[i] = -1;
                continue;
            }
            if (nextFolderUnpackStreamIndex == 0) {
                for (; nextFolderIndex < archive.folders.length; ++nextFolderIndex) {
                    streamMap.folderFirstFileIndex[nextFolderIndex] = i;
                    if (archive.folders[nextFolderIndex].numUnpackSubStreams > 0) {
                        break;
                    }
                }
                if (nextFolderIndex >= archive.folders.length) {
                    throw new IOException("Too few folders in archive");
                }
            }
            streamMap.fileFolderIndex[i] = nextFolderIndex;
            if (!archive.files[i].hasStream()) {
                continue;
            }
            ++nextFolderUnpackStreamIndex;
            if (nextFolderUnpackStreamIndex >= archive.folders[nextFolderIndex].numUnpackSubStreams) {
                ++nextFolderIndex;
                nextFolderUnpackStreamIndex = 0;
            }
        }

        archive.streamMap = streamMap;
    }

    private void buildDecodingStream(int entryIndex) throws IOException {
        if (archive.streamMap == null) {
            throw new IOException("Archive doesn't contain stream information to read entries");
        }
        final int folderIndex = archive.streamMap.fileFolderIndex[entryIndex];
        if (folderIndex < 0) {
            deferredBlockStreams.clear();
            // TODO: previously it'd return an empty stream?
            // new BoundedInputStream(new ByteArrayInputStream(new byte[0]), 0);
            return;
        }
        final SevenZArchiveEntry file = archive.files[entryIndex];
        boolean isInSameFolder = false;
        final Folder folder = archive.folders[folderIndex];
        final int firstPackStreamIndex = archive.streamMap.folderFirstPackStreamIndex[folderIndex];
        final long folderOffset = SIGNATURE_HEADER_SIZE + archive.packPos +
                archive.streamMap.packStreamOffsets[firstPackStreamIndex];
        if (currentFolderIndex == folderIndex) {
            // (COMPRESS-320).
            // The current entry is within the same (potentially opened) folder. The
            // previous stream has to be fully decoded before we can start reading
            // but don't do it eagerly -- if the user skips over the entire folder nothing
            // is effectively decompressed.

            file.setContentMethods(archive.files[entryIndex - 1].getContentMethods());

            // if this is called in a random access, then the content methods of previous entry may be null
            // the content methods should be set to methods of the first entry as it must not be null,
            // and the content methods would only be set if the content methods was not set
            if(currentEntryIndex != entryIndex && file.getContentMethods() == null) {
                int folderFirstFileIndex = archive.streamMap.folderFirstFileIndex[folderIndex];
                SevenZArchiveEntry folderFirstFile = archive.files[folderFirstFileIndex];
                file.setContentMethods(folderFirstFile.getContentMethods());
            }
            isInSameFolder = true;
        } else {
            // We're opening a new folder. Discard any queued streams/ folder stream.
            currentFolderIndex = folderIndex;
            deferredBlockStreams.clear();
            if (currentFolderInputStream != null) {
                currentFolderInputStream.close();
                currentFolderInputStream = null;
            }

            currentFolderInputStream = buildDecoderStack(folder, folderOffset, firstPackStreamIndex, file);
        }

        // if this mothod is called in a random access, then some entries need to be skipped,
        // if the entry of entryIndex is in the same folder of the currentFolderIndex,
        // then the entries between (currentEntryIndex + 1) and (entryIndex - 1) need to be skipped,
        // otherwise it's a new folder, and the entries between firstFileInFolderIndex and (entryIndex - 1) need to be skipped
        if(currentEntryIndex != entryIndex) {
            int filesToSkipStartIndex = archive.streamMap.folderFirstFileIndex[folderIndex];
            if(isInSameFolder) {
                if(currentEntryIndex < entryIndex) {
                    // the entries between filesToSkipStartIndex and currentEntryIndex had already been skipped
                    filesToSkipStartIndex = currentEntryIndex + 1;
                } else {
                    // the entry is in the same folder of current entry, but it has already been read before, we need to reset
                    // the position of the currentFolderInputStream to the beginning of folder, and then skip the files
                    // from the start entry of the folder again
                    deferredBlockStreams.clear();
                    channel.position(folderOffset);
                }
            }

            for(int i = filesToSkipStartIndex;i < entryIndex;i++) {
                SevenZArchiveEntry fileToSkip = archive.files[i];
                InputStream fileStreamToSkip = new BoundedInputStream(currentFolderInputStream, fileToSkip.getSize());
                if (fileToSkip.getHasCrc()) {
                    fileStreamToSkip = new CRC32VerifyingInputStream(fileStreamToSkip, fileToSkip.getSize(), fileToSkip.getCrcValue());
                }
                deferredBlockStreams.add(fileStreamToSkip);

                // set the content methods as well, it equals to file.getContentMethods() because they are in same folder
                fileToSkip.setContentMethods(file.getContentMethods());
            }
        }

        InputStream fileStream = new BoundedInputStream(currentFolderInputStream, file.getSize());
        if (file.getHasCrc()) {
            fileStream = new CRC32VerifyingInputStream(fileStream, file.getSize(), file.getCrcValue());
        }

        deferredBlockStreams.add(fileStream);
    }

    private InputStream buildDecoderStack(final Folder folder, final long folderOffset,
                final int firstPackStreamIndex, final SevenZArchiveEntry entry) throws IOException {
        channel.position(folderOffset);
        InputStream inputStreamStack = new FilterInputStream(new BufferedInputStream(
              new BoundedSeekableByteChannelInputStream(channel,
                  archive.packSizes[firstPackStreamIndex]))) {
            @Override
            public int read() throws IOException {
                final int r = in.read();
                if (r >= 0) {
                    count(1);
                }
                return r;
            }
            @Override
            public int read(final byte[] b) throws IOException {
                return read(b, 0, b.length);
            }
            @Override
            public int read(final byte[] b, final int off, final int len) throws IOException {
                if (len == 0) {
                    return 0;
                }
                final int r = in.read(b, off, len);
                if (r >= 0) {
                    count(r);
                }
                return r;
            }
            private void count(int c) {
                compressedBytesReadFromCurrentEntry += c;
            }
        };
        final LinkedList<SevenZMethodConfiguration> methods = new LinkedList<>();
        for (final Coder coder : folder.getOrderedCoders()) {
            if (coder.numInStreams != 1 || coder.numOutStreams != 1) {
                throw new IOException("Multi input/output stream coders are not yet supported");
            }
            final SevenZMethod method = SevenZMethod.byId(coder.decompressionMethodId);
            inputStreamStack = Coders.addDecoder(fileName, inputStreamStack,
                    folder.getUnpackSizeForCoder(coder), coder, password, options.getMaxMemoryLimitInKb());
            methods.addFirst(new SevenZMethodConfiguration(method,
                     Coders.findByMethod(method).getOptionsFromCoder(coder, inputStreamStack)));
        }
        entry.setContentMethods(methods);
        if (folder.hasCrc) {
            return new CRC32VerifyingInputStream(inputStreamStack,
                    folder.getUnpackSize(), folder.crc);
        }
        return inputStreamStack;
    }

    /**
     * Reads a byte of data.
     *
     * @return the byte read, or -1 if end of input is reached
     * @throws IOException
     *             if an I/O error has occurred
     */
    public int read() throws IOException {
        int b = getCurrentStream().read();
        if (b >= 0) {
            uncompressedBytesReadFromCurrentEntry++;
        }
        return b;
    }

    private InputStream getCurrentStream() throws IOException {
        if (archive.files[currentEntryIndex].getSize() == 0) {
            return new ByteArrayInputStream(new byte[0]);
        }
        if (deferredBlockStreams.isEmpty()) {
            throw new IllegalStateException("No current 7z entry (call getNextEntry() first).");
        }

        while (deferredBlockStreams.size() > 1) {
            // In solid compression mode we need to decompress all leading folder'
            // streams to get access to an entry. We defer this until really needed
            // so that entire blocks can be skipped without wasting time for decompression.
            try (final InputStream stream = deferredBlockStreams.remove(0)) {
                IOUtils.skip(stream, Long.MAX_VALUE);
            }
            compressedBytesReadFromCurrentEntry = 0;
        }

        return deferredBlockStreams.get(0);
    }

    /**
     * Returns an InputStream for reading the contents of the given entry.
     *
     * <p>For archives using solid compression randomly accessing
     * entries will be significantly slower than reading the archive
     * sequentiallly.</p>
     *
     * @param entry the entry to get the stream for.
     * @return a stream to read the entry from.
     * @throws IOException if unable to create an input stream from the zipentry
     * @since Compress 1.20
     */
    public InputStream getInputStream(SevenZArchiveEntry entry) throws IOException {
        int entryIndex = -1;
        for (int i = 0; i < this.archive.files.length;i++) {
            if (entry == this.archive.files[i]) {
                entryIndex = i;
                break;
            }
        }

        if (entryIndex < 0) {
            throw new IllegalArgumentException("Can not find " + entry.getName() + " in " + this.fileName);
        }

        buildDecodingStream(entryIndex);
        currentEntryIndex = entryIndex;
        currentFolderIndex = archive.streamMap.fileFolderIndex[entryIndex];
        return getCurrentStream();
    }

    /**
     * Reads data into an array of bytes.
     *
     * @param b the array to write data to
     * @return the number of bytes read, or -1 if end of input is reached
     * @throws IOException
     *             if an I/O error has occurred
     */
    public int read(final byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    /**
     * Reads data into an array of bytes.
     *
     * @param b the array to write data to
     * @param off offset into the buffer to start filling at
     * @param len of bytes to read
     * @return the number of bytes read, or -1 if end of input is reached
     * @throws IOException
     *             if an I/O error has occurred
     */
    public int read(final byte[] b, final int off, final int len) throws IOException {
        if (len == 0) {
            return 0;
        }
        int cnt = getCurrentStream().read(b, off, len);
        if (cnt > 0) {
            uncompressedBytesReadFromCurrentEntry += cnt;
        }
        return cnt;
    }

    /**
     * Provides statistics for bytes read from the current entry.
     *
     * @return statistics for bytes read from the current entry
     * @since 1.17
     */
    public InputStreamStatistics getStatisticsForCurrentEntry() {
        return new InputStreamStatistics() {
            @Override
            public long getCompressedCount() {
                return compressedBytesReadFromCurrentEntry;
            }
            @Override
            public long getUncompressedCount() {
                return uncompressedBytesReadFromCurrentEntry;
            }
        };
    }

    private static long readUint64(final ByteBuffer in) throws IOException {
        // long rather than int as it might get shifted beyond the range of an int
        final long firstByte = getUnsignedByte(in);
        int mask = 0x80;
        long value = 0;
        for (int i = 0; i < 8; i++) {
            if ((firstByte & mask) == 0) {
                return value | ((firstByte & (mask - 1)) << (8 * i));
            }
            final long nextByte = getUnsignedByte(in);
            value |= nextByte << (8 * i);
            mask >>>= 1;
        }
        return value;
    }

    private static int getUnsignedByte(ByteBuffer buf) {
        return buf.get() & 0xff;
    }

    /**
     * Checks if the signature matches what is expected for a 7z file.
     *
     * @param signature
     *            the bytes to check
     * @param length
     *            the number of bytes to check
     * @return true, if this is the signature of a 7z archive.
     * @since 1.8
     */
    public static boolean matches(final byte[] signature, final int length) {
        if (length < sevenZSignature.length) {
            return false;
        }

        for (int i = 0; i < sevenZSignature.length; i++) {
            if (signature[i] != sevenZSignature[i]) {
                return false;
            }
        }
        return true;
    }

    private static long skipBytesFully(final ByteBuffer input, long bytesToSkip) throws IOException {
        if (bytesToSkip < 1) {
            return 0;
        }
        int current = input.position();
        int maxSkip = input.remaining();
        if (maxSkip < bytesToSkip) {
            bytesToSkip = maxSkip;
        }
        input.position(current + (int) bytesToSkip);
        return bytesToSkip;
    }

    private void readFully(ByteBuffer buf) throws IOException {
        buf.rewind();
        IOUtils.readFully(channel, buf);
        buf.flip();
    }

    @Override
    public String toString() {
      return archive.toString();
    }

    /**
     * Derives a default file name from the archive name - if known.
     *
     * <p>This implements the same heuristics the 7z tools use. In
     * 7z's case if an archive contains entries without a name -
     * i.e. {@link SevenZArchiveEntry#getName} returns {@code null} -
     * then its command line and GUI tools will use this default name
     * when extracting the entries.</p>
     *
     * @return null if the name of the archive is unknown. Otherwise
     * if the name of the archive has got any extension, it is
     * stripped and the remainder returned. Finally if the name of the
     * archive hasn't got any extension then a {@code ~} character is
     * appended to the archive name.
     *
     * @since 1.19
     */
    public String getDefaultName() {
        if (DEFAULT_FILE_NAME.equals(fileName) || fileName == null) {
            return null;
        }

        final String lastSegment = new File(fileName).getName();
        final int dotPos = lastSegment.lastIndexOf(".");
        if (dotPos > 0) { // if the file starts with a dot then this is not an extension
            return lastSegment.substring(0, dotPos);
        }
        return lastSegment + "~";
    }

    private static final CharsetEncoder PASSWORD_ENCODER = StandardCharsets.UTF_16LE.newEncoder();

    private static byte[] utf16Decode(char[] chars) throws IOException {
        if (chars == null) {
            return null;
        }
        ByteBuffer encoded = PASSWORD_ENCODER.encode(CharBuffer.wrap(chars));
        if (encoded.hasArray()) {
            return encoded.array();
        }
        byte[] e = new byte[encoded.remaining()];
        encoded.get(e);
        return e;
    }

    private static void assertFitsIntoInt(String what, long value) throws IOException {
        if (value > Integer.MAX_VALUE) {
            throw new IOException("Cannot handle " + what + value);
        }
    }
}
