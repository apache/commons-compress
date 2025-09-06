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

package org.apache.commons.compress.archivers.lha;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipUtil;
import org.apache.commons.compress.compressors.lha.Lh4CompressorInputStream;
import org.apache.commons.compress.compressors.lha.Lh5CompressorInputStream;
import org.apache.commons.compress.compressors.lha.Lh6CompressorInputStream;
import org.apache.commons.compress.compressors.lha.Lh7CompressorInputStream;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.io.input.ChecksumInputStream;

/**
 * Implements the LHA archive format as an InputStream.
 *
 * This implementation is based on the documentation that can be found at
 * http://dangan.g.dgdg.jp/en/Content/Program/Java/jLHA/Notes/Notes.html
 *
 * @NotThreadSafe
 * @since 1.29.0
 */
public class LhaArchiveInputStream extends ArchiveInputStream<LhaArchiveEntry> {

    // @formatter:off
    /**
     * Builds a new {@link LhaArchiveInputStream}.
     *
     * <p>
     * For example:
     * </p>
     * <pre>{@code
     * LhaArchiveInputStream s = LhaArchiveInputStream.builder()
     *   .setInputStream(archiveInputStream)
     *   .setCharset(StandardCharsets.UTF_8)
     *   .get();}
     * </pre>
     */
    // @formatter:on
    public static class Builder {

        /**
         * The InputStream to read the archive data from.
         */
        private InputStream inputStream;

        /**
         * The default Charset.
         */
        private Charset charsetDefault = StandardCharsets.US_ASCII;

        /**
         * The Charset, defaults to {@link StandardCharsets#US_ASCII}.
         */
        private Charset charset = charsetDefault;

        /**
         * The file separator char, defaults to {@link File#separatorChar}.
         */
        private char fileSeparatorChar = File.separatorChar;

        /**
         * Constructs a new instance.
         */
        private Builder() {
            // empty
        }

        /**
         * Gets a new LhaArchiveInputStream.
         *
         * @return a new LhaArchiveInputStream.
         */
        public LhaArchiveInputStream get() {
            return new LhaArchiveInputStream(this.inputStream, this.charset, this.fileSeparatorChar);
        }

        /**
         * Sets the InputStream to read the archive data from.
         *
         * @param inputStream the InputStream.
         * @return {@code this} instance.
         */
        public Builder setInputStream(final InputStream inputStream) {
            this.inputStream = inputStream;
            return this;
        }

        /**
         * Sets the Charset.
         *
         * @param charset the Charset, null resets to the default {@link StandardCharsets#US_ASCII}.
         * @return {@code this} instance.
         */
        public Builder setCharset(final Charset charset) {
            this.charset = Charsets.toCharset(charset, charsetDefault);
            return this;
        }

        /**
         * Sets the file separator char. Package private for testing.
         *
         * @param fileSeparatorChar the file separator char, defaults to {@link File#separatorChar}.
         * @return {@code this} instance.
         */
        Builder setFileSeparatorChar(final char fileSeparatorChar) {
            this.fileSeparatorChar = fileSeparatorChar;
            return this;
        }
    }

    // Fields that are the same across all header levels
    private static final int HEADER_GENERIC_MINIMUM_HEADER_LENGTH = 22;
    private static final int HEADER_GENERIC_OFFSET_COMPRESSION_METHOD = 2;
    private static final int HEADER_GENERIC_OFFSET_HEADER_LEVEL = 20;

    // Header Level 0
    private static final int HEADER_LEVEL_0_OFFSET_HEADER_SIZE = 0;
    private static final int HEADER_LEVEL_0_OFFSET_HEADER_CHECKSUM = 1;
    private static final int HEADER_LEVEL_0_OFFSET_COMPRESSED_SIZE = 7;
    private static final int HEADER_LEVEL_0_OFFSET_ORIGINAL_SIZE = 11;
    private static final int HEADER_LEVEL_0_OFFSET_LAST_MODIFIED_DATE_TIME = 15;
    private static final int HEADER_LEVEL_0_OFFSET_FILENAME_LENGTH = 21;
    private static final int HEADER_LEVEL_0_OFFSET_FILENAME = 22;

    // Header Level 1
    private static final int HEADER_LEVEL_1_OFFSET_BASE_HEADER_SIZE = 0;
    private static final int HEADER_LEVEL_1_OFFSET_BASE_HEADER_CHECKSUM = 1;
    private static final int HEADER_LEVEL_1_OFFSET_SKIP_SIZE = 7;
    private static final int HEADER_LEVEL_1_OFFSET_ORIGINAL_SIZE = 11;
    private static final int HEADER_LEVEL_1_OFFSET_LAST_MODIFIED_DATE_TIME = 15;
    private static final int HEADER_LEVEL_1_OFFSET_FILENAME_LENGTH = 21;
    private static final int HEADER_LEVEL_1_OFFSET_FILENAME = 22;

    // Header Level 2
    private static final int HEADER_LEVEL_2_OFFSET_HEADER_SIZE = 0;
    private static final int HEADER_LEVEL_2_OFFSET_COMPRESSED_SIZE = 7;
    private static final int HEADER_LEVEL_2_OFFSET_ORIGINAL_SIZE = 11;
    private static final int HEADER_LEVEL_2_OFFSET_LAST_MODIFIED_DATE_TIME = 15;
    private static final int HEADER_LEVEL_2_OFFSET_CRC = 21;
    private static final int HEADER_LEVEL_2_OFFSET_OS_ID = 23;
    private static final int HEADER_LEVEL_2_OFFSET_FIRST_EXTENDED_HEADER_SIZE = 24;

    // Extended header types
    private static final int EXTENDED_HEADER_TYPE_COMMON = 0x00;
    private static final int EXTENDED_HEADER_TYPE_FILENAME = 0x01;
    private static final int EXTENDED_HEADER_TYPE_DIRECTORY_NAME = 0x02;

    private static final int EXTENDED_HEADER_TYPE_MSDOS_FILE_ATTRIBUTES = 0x40;

    private static final int EXTENDED_HEADER_TYPE_UNIX_PERMISSION = 0x50;
    private static final int EXTENDED_HEADER_TYPE_UNIX_UID_GID = 0x51;
    private static final int EXTENDED_HEADER_TYPE_UNIX_TIMESTAMP = 0x54;

    // Compression methods
    private static final String COMPRESSION_METHOD_DIRECTORY = "-lhd-"; // Directory entry
    private static final String COMPRESSION_METHOD_LH0 = "-lh0-";
    private static final String COMPRESSION_METHOD_LH4 = "-lh4-";
    private static final String COMPRESSION_METHOD_LH5 = "-lh5-";
    private static final String COMPRESSION_METHOD_LH6 = "-lh6-";
    private static final String COMPRESSION_METHOD_LH7 = "-lh7-";
    private static final String COMPRESSION_METHOD_LZ4 = "-lz4-";

    /**
     * Maximum length of a pathname.
     */
    private static final int MAX_PATHNAME_LENGTH = 4096;

    private final char fileSeparatorChar;
    private LhaArchiveEntry currentEntry;
    private InputStream currentCompressedStream;
    private InputStream currentDecompressedStream;

    /**
     * Creates a new builder.
     *
     * @return a new builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    private LhaArchiveInputStream(final InputStream inputStream, final Charset charset, final char fileSeparatorChar) {
        super(inputStream, charset);
        this.fileSeparatorChar = fileSeparatorChar;
    }

    @Override
    public boolean canReadEntryData(final ArchiveEntry archiveEntry) {
        return currentDecompressedStream != null;
    }

    @Override
    public int read(final byte[] buffer, final int offset, final int length) throws IOException {
        if (currentEntry == null) {
            throw new IllegalStateException("No current entry");
        }

        if (currentDecompressedStream == null) {
            throw new ArchiveException("Unsupported compression method: %s", currentEntry.getCompressionMethod());
        }

        return currentDecompressedStream.read(buffer, offset, length);
    }

    /**
     * Checks if the signature matches what is expected for an LHA file. There is no specific
     * signature for LHA files, so this method checks if the header level and the compression
     * method are valid for an LHA archive. The signature must be at least the minimum header
     * length of 22 bytes for this check to work properly.
     *
     * @param signature the bytes to check
     * @param length the number of bytes to check
     * @return true, if this stream is an LHA archive stream, false otherwise
     */
    public static boolean matches(final byte[] signature, final int length) {
        if (signature.length < HEADER_GENERIC_MINIMUM_HEADER_LENGTH || length < HEADER_GENERIC_MINIMUM_HEADER_LENGTH) {
            return false;
        }

        final ByteBuffer header = ByteBuffer.wrap(signature).order(ByteOrder.LITTLE_ENDIAN);

        // Determine header level. Expected value is in the range 0-3.
        final byte headerLevel = header.get(HEADER_GENERIC_OFFSET_HEADER_LEVEL);
        if (headerLevel < 0 || headerLevel > 3) {
            return false;
        }

        // Check if the compression method is valid for LHA archives
        try {
            getCompressionMethod(header);
        } catch (ArchiveException e) {
            return false;
        }

        return true;
    }

    @Override
    public LhaArchiveEntry getNextEntry() throws IOException {
        if (this.currentCompressedStream != null) {
            // Consume the entire compressed stream to end up at the next entry
            IOUtils.consume(this.currentCompressedStream);

            this.currentCompressedStream = null;
            this.currentDecompressedStream = null;
        }

        this.currentEntry = readHeader();

        return this.currentEntry;
    }

    /**
     * Read the next LHA header from the input stream.
     *
     * @return the next header entry, or null if there are no more entries
     * @throws IOException
     */
    LhaArchiveEntry readHeader() throws IOException {
        // Header level is not known yet. Read the minimum length header.
        final byte[] buffer = new byte[HEADER_GENERIC_MINIMUM_HEADER_LENGTH];
        final int len = IOUtils.read(in, buffer);
        if (len == 0) {
            // EOF
            return null;
        } else if (len == 1 && buffer[0] == 0) {
            // Last byte of the file is zero indicating no more entries
            return null;
        } else if (len < HEADER_GENERIC_MINIMUM_HEADER_LENGTH) {
            throw new ArchiveException("Invalid header length");
        }

        final ByteBuffer header = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN);

        // Determine header level
        final byte headerLevel = header.get(HEADER_GENERIC_OFFSET_HEADER_LEVEL);
        if (headerLevel == 0) {
            return readHeaderLevel0(header);
        } else if (headerLevel == 1) {
            return readHeaderLevel1(header);
        } else if (headerLevel == 2) {
            return readHeaderLevel2(header);
        } else {
            throw new ArchiveException("Invalid header level: %d", headerLevel);
        }
    }

    /**
     * Read LHA header level 0.
     *
     * @param buffer the buffer containing the header data
     * @return the LhaArchiveEntry read from the buffer
     * @throws IOException
     */
    LhaArchiveEntry readHeaderLevel0(ByteBuffer buffer) throws IOException {
        final int headerSize = Byte.toUnsignedInt(buffer.get(HEADER_LEVEL_0_OFFSET_HEADER_SIZE));
        if (headerSize < HEADER_GENERIC_MINIMUM_HEADER_LENGTH) {
            throw new ArchiveException("Invalid header level 0 length: %d", headerSize);
        }

        buffer = readRemainingHeaderData(buffer, headerSize + 2); // Header size is not including the first two bytes of the header

        final int headerChecksum = Byte.toUnsignedInt(buffer.get(HEADER_LEVEL_0_OFFSET_HEADER_CHECKSUM));

        final String compressionMethod = getCompressionMethod(buffer);

        final LhaArchiveEntry.Builder entryBuilder = new LhaArchiveEntry.Builder()
            .setCompressionMethod(compressionMethod)
            .setCompressedSize(Integer.toUnsignedLong(buffer.getInt(HEADER_LEVEL_0_OFFSET_COMPRESSED_SIZE)))
            .setSize(Integer.toUnsignedLong(buffer.getInt(HEADER_LEVEL_0_OFFSET_ORIGINAL_SIZE)))
            .setLastModifiedDate(new Date(ZipUtil.dosToJavaTime(Integer.toUnsignedLong(buffer.getInt(HEADER_LEVEL_0_OFFSET_LAST_MODIFIED_DATE_TIME)))));

        final int filenameLength = Byte.toUnsignedInt(buffer.get(HEADER_LEVEL_0_OFFSET_FILENAME_LENGTH));
        buffer.position(HEADER_LEVEL_0_OFFSET_FILENAME);
        entryBuilder.setFilename(getPathname(buffer, filenameLength))
            .setDirectory(isDirectory(compressionMethod))
            .setCrcValue(Short.toUnsignedInt(buffer.getShort()));

        if (calculateHeaderChecksum(buffer) != headerChecksum) {
            throw new ArchiveException("Invalid header level 0 checksum");
        }

        final LhaArchiveEntry entry = entryBuilder.get();

        prepareDecompression(entry);

        return entry;
    }

    /**
     * Read LHA header level 1.
     *
     * @param buffer the buffer containing the header data
     * @return the LhaArchiveEntry read from the buffer
     * @throws IOException
     */
    LhaArchiveEntry readHeaderLevel1(ByteBuffer buffer) throws IOException {
        final int baseHeaderSize = Byte.toUnsignedInt(buffer.get(HEADER_LEVEL_1_OFFSET_BASE_HEADER_SIZE));
        if (baseHeaderSize < HEADER_GENERIC_MINIMUM_HEADER_LENGTH) {
            throw new ArchiveException("Invalid header level 1 length: %d", baseHeaderSize);
        }

        buffer = readRemainingHeaderData(buffer, baseHeaderSize + 2);

        final int baseHeaderChecksum = Byte.toUnsignedInt(buffer.get(HEADER_LEVEL_1_OFFSET_BASE_HEADER_CHECKSUM));

        final String compressionMethod = getCompressionMethod(buffer);
        long skipSize = Integer.toUnsignedLong(buffer.getInt(HEADER_LEVEL_1_OFFSET_SKIP_SIZE));

        final LhaArchiveEntry.Builder entryBuilder = new LhaArchiveEntry.Builder()
            .setCompressionMethod(compressionMethod)
            .setSize(Integer.toUnsignedLong(buffer.getInt(HEADER_LEVEL_1_OFFSET_ORIGINAL_SIZE)))
            .setLastModifiedDate(new Date(ZipUtil.dosToJavaTime(Integer.toUnsignedLong(buffer.getInt(HEADER_LEVEL_1_OFFSET_LAST_MODIFIED_DATE_TIME)))));

        final int filenameLength = Byte.toUnsignedInt(buffer.get(HEADER_LEVEL_1_OFFSET_FILENAME_LENGTH));
        buffer.position(HEADER_LEVEL_1_OFFSET_FILENAME);
        entryBuilder.setFilename(getPathname(buffer, filenameLength))
            .setDirectory(isDirectory(compressionMethod))
            .setCrcValue(Short.toUnsignedInt(buffer.getShort()))
            .setOsId(Byte.toUnsignedInt(buffer.get()));

        if (calculateHeaderChecksum(buffer) != baseHeaderChecksum) {
            throw new ArchiveException("Invalid header level 1 checksum");
        }

        // Create a list to store base header and all extended headers
        // to be able to calculate the CRC of the full header
        final List<ByteBuffer> headerParts = new ArrayList<>();
        headerParts.add(buffer);

        int nextHeaderSize = Short.toUnsignedInt(buffer.getShort());
        while (nextHeaderSize > 0) {
            final ByteBuffer extendedHeaderBuffer = readExtendedHeader(nextHeaderSize);
            skipSize -= nextHeaderSize;

            parseExtendedHeader(extendedHeaderBuffer, entryBuilder);

            headerParts.add(extendedHeaderBuffer);

            nextHeaderSize = Short.toUnsignedInt(extendedHeaderBuffer.getShort(extendedHeaderBuffer.limit() - 2));
        }

        entryBuilder.setCompressedSize(skipSize);

        final LhaArchiveEntry entry = entryBuilder.get();

        if (entry.getHeaderCrc() != null) {
            // Calculate CRC16 of full header
            final long headerCrc = calculateCRC16(headerParts.toArray(new ByteBuffer[headerParts.size()]));
            if (headerCrc != entry.getHeaderCrc()) {
                throw new ArchiveException("Invalid header CRC expected=0x%04x found=0x%04x", headerCrc, entry.getHeaderCrc());
            }
        }

        prepareDecompression(entry);

        return entry;
    }

    /**
     * Read LHA header level 2.
     *
     * @param buffer the buffer containing the header data
     * @return the LhaArchiveEntry read from the buffer
     * @throws IOException
     */
    LhaArchiveEntry readHeaderLevel2(ByteBuffer buffer) throws IOException {
        final int headerSize = Short.toUnsignedInt(buffer.getShort(HEADER_LEVEL_2_OFFSET_HEADER_SIZE));
        if (headerSize < HEADER_GENERIC_MINIMUM_HEADER_LENGTH) {
            throw new ArchiveException("Invalid header level 2 length: %d", headerSize);
        }

        buffer = readRemainingHeaderData(buffer, headerSize);

        final String compressionMethod = getCompressionMethod(buffer);

        final LhaArchiveEntry.Builder entryBuilder = new LhaArchiveEntry.Builder()
            .setCompressionMethod(compressionMethod)
            .setCompressedSize(Integer.toUnsignedLong(buffer.getInt(HEADER_LEVEL_2_OFFSET_COMPRESSED_SIZE)))
            .setSize(Integer.toUnsignedLong(buffer.getInt(HEADER_LEVEL_2_OFFSET_ORIGINAL_SIZE)))
            .setLastModifiedDate(new Date(Integer.toUnsignedLong(buffer.getInt(HEADER_LEVEL_2_OFFSET_LAST_MODIFIED_DATE_TIME)) * 1000))
            .setDirectory(isDirectory(compressionMethod))
            .setCrcValue(Short.toUnsignedInt(buffer.getShort(HEADER_LEVEL_2_OFFSET_CRC)))
            .setOsId(Byte.toUnsignedInt(buffer.get(HEADER_LEVEL_2_OFFSET_OS_ID)));

        int extendedHeaderOffset = HEADER_LEVEL_2_OFFSET_FIRST_EXTENDED_HEADER_SIZE;
        int nextHeaderSize = Short.toUnsignedInt(buffer.getShort(extendedHeaderOffset));
        while (nextHeaderSize > 0) {
            // Create new ByteBuffer as a slice from the full header. Set limit to the extended header length.
            final ByteBuffer extendedHeaderBuffer = byteBufferSlice(buffer, extendedHeaderOffset + 2, nextHeaderSize).order(ByteOrder.LITTLE_ENDIAN);

            extendedHeaderOffset += nextHeaderSize;

            parseExtendedHeader(extendedHeaderBuffer, entryBuilder);

            nextHeaderSize = Short.toUnsignedInt(extendedHeaderBuffer.getShort(extendedHeaderBuffer.limit() - 2));
        }

        final LhaArchiveEntry entry = entryBuilder.get();

        if (entry.getHeaderCrc() != null) {
            // Calculate CRC16 of full header
            final long headerCrc = calculateCRC16(buffer);
            if (headerCrc != entry.getHeaderCrc()) {
                throw new ArchiveException("Invalid header CRC expected=0x%04x found=0x%04x", headerCrc, entry.getHeaderCrc());
            }
        }

        prepareDecompression(entry);

        return entry;
    }

    /**
     * Gets the compression method from the header. It is always located at the same offset for all header levels.
     *
     * @param buffer the buffer containing the header data
     * @return compression method, e.g. -lh5-
     * @throws ArchiveException if the compression method is invalid
     */
    static String getCompressionMethod(final ByteBuffer buffer) throws ArchiveException {
        final byte[] compressionMethodBuffer = new byte[5];
        byteBufferGet(buffer, HEADER_GENERIC_OFFSET_COMPRESSION_METHOD, compressionMethodBuffer);

        // Validate the compression method
        if (compressionMethodBuffer[0] == '-' &&
            Character.isLowerCase(compressionMethodBuffer[1]) &&
            Character.isLowerCase(compressionMethodBuffer[2]) &&
            (Character.isLowerCase(compressionMethodBuffer[3]) || Character.isDigit(compressionMethodBuffer[3])) &&
            compressionMethodBuffer[4] == '-') {
            return new String(compressionMethodBuffer, StandardCharsets.US_ASCII);
        } else {
            throw new ArchiveException("Invalid compression method: 0x%02x 0x%02x 0x%02x 0x%02x 0x%02x",
                    compressionMethodBuffer[0],
                    compressionMethodBuffer[1],
                    compressionMethodBuffer[2],
                    compressionMethodBuffer[3],
                    compressionMethodBuffer[4]);
        }
    }

    /**
     * Gets the pathname from the current position in the provided buffer. Any 0xFF bytes
     * and '\' chars will be converted into the configured file path separator char.
     * Any leading file path separator char will be removed to avoid extracting to
     * absolute locations.
     *
     * @param buffer the buffer where to get the pathname from
     * @param pathnameLength the length of the pathname
     * @return pathname
     * @throws ArchiveException if the pathname is too long
     */
    String getPathname(final ByteBuffer buffer, final int pathnameLength) throws ArchiveException {
        // Check pathname length to ensure we don't allocate too much memory
        if (pathnameLength > MAX_PATHNAME_LENGTH) {
            throw new ArchiveException("Pathname is longer than the maximum allowed (%d > %d)", pathnameLength, MAX_PATHNAME_LENGTH);
        }

        final byte[] pathnameBuffer = new byte[pathnameLength];
        buffer.get(pathnameBuffer);

        // Split the pathname into parts by 0xFF bytes
        final StringBuilder pathnameStringBuilder = new StringBuilder();
        int start = 0;
        for (int i = 0; i < pathnameLength; i++) {
            if (pathnameBuffer[i] == (byte) 0xFF) {
                if (i > start) {
                    // Decode the path segment into a string using the specified charset and append it to the result
                    pathnameStringBuilder.append(new String(pathnameBuffer, start, i - start, getCharset())).append(fileSeparatorChar);
                }

                start = i + 1; // Move start to the next segment
            }
        }

        // Append the last segment if it exists
        if (start < pathnameLength) {
            pathnameStringBuilder.append(new String(pathnameBuffer, start, pathnameLength - start, getCharset()));
        }

        String pathname = pathnameStringBuilder.toString();

        // If the path separator char is not '\', replace all '\' characters with the path separator char
        if (fileSeparatorChar != '\\') {
            pathname = pathname.replace('\\', fileSeparatorChar);
        }

        // Remove leading file separator chars to avoid extracting to absolute locations
        while (pathname.length() > 0 && pathname.charAt(0) == fileSeparatorChar) {
            pathname = pathname.substring(1);
        }

        return pathname;
    }

    /**
     * Read the remaining part of the header and append it to the already loaded parts.
     *
     * @param currentHeader all header parts that have already been loaded into memory
     * @param headerSize total header size
     * @return header the complete header as a ByteBuffer
     * @throws IOException
     */
    private ByteBuffer readRemainingHeaderData(final ByteBuffer currentHeader, final int headerSize) throws IOException {
        final byte[] remainingData = new byte[headerSize - currentHeader.capacity()];
        final int len = IOUtils.read(in, remainingData);
        if (len != remainingData.length) {
            throw new ArchiveException("Error reading remaining header");
        }

        return ByteBuffer.allocate(currentHeader.capacity() + len).put(currentHeader.array()).put(remainingData).order(ByteOrder.LITTLE_ENDIAN);
    }

    /**
     * Read extended header from the input stream.
     *
     * @param headerSize the size of the extended header to read
     * @return the extended header as a ByteBuffer
     * @throws IOException
     */
    private ByteBuffer readExtendedHeader(final int headerSize) throws IOException {
        final byte[] extensionHeader = new byte[headerSize];
        final int len = IOUtils.read(in, extensionHeader);
        if (len != extensionHeader.length) {
            throw new ArchiveException("Error reading extended header");
        }

        return ByteBuffer.wrap(extensionHeader).order(ByteOrder.LITTLE_ENDIAN);
    }

    /**
     * Parse the extended header and set the values in the provided entry.
     *
     * @param extendedHeaderBuffer the buffer containing the extended header
     * @param entryBuilder the entry builder to set the values in
     * @throws IOException
     */
    void parseExtendedHeader(final ByteBuffer extendedHeaderBuffer, final LhaArchiveEntry.Builder entryBuilder) throws IOException {
        final int extendedHeaderType = Byte.toUnsignedInt(extendedHeaderBuffer.get());
        if (extendedHeaderType == EXTENDED_HEADER_TYPE_COMMON) {
            // Common header
            final int crcPos = extendedHeaderBuffer.position(); // Save the current position to be able to set the header CRC later

            // Header CRC
            entryBuilder.setHeaderCrc(Short.toUnsignedInt(extendedHeaderBuffer.getShort()));

            // Set header CRC to zero to be able to later compute the CRC of the full header
            extendedHeaderBuffer.putShort(crcPos, (short) 0);
        } else if (extendedHeaderType == EXTENDED_HEADER_TYPE_FILENAME) {
            // File name header
            final int filenameLength = extendedHeaderBuffer.limit() - extendedHeaderBuffer.position() - 2;
            final String filename = getPathname(extendedHeaderBuffer, filenameLength);
            entryBuilder.setFilename(filename);
        } else if (extendedHeaderType == EXTENDED_HEADER_TYPE_DIRECTORY_NAME) {
            // Directory name header
            final int directoryNameLength = extendedHeaderBuffer.limit() - extendedHeaderBuffer.position() - 2;
            final String directoryName = getPathname(extendedHeaderBuffer, directoryNameLength);
            if (directoryName.charAt(directoryName.length() - 1) != fileSeparatorChar) {
                // If the directory name does not end with a file separator, append it
                entryBuilder.setDirectoryName(directoryName + fileSeparatorChar);
            } else {
                entryBuilder.setDirectoryName(directoryName);
            }

        } else if (extendedHeaderType == EXTENDED_HEADER_TYPE_MSDOS_FILE_ATTRIBUTES) {
            // MS-DOS file attributes
            entryBuilder.setMsdosFileAttributes(Short.toUnsignedInt(extendedHeaderBuffer.getShort()));
        } else if (extendedHeaderType == EXTENDED_HEADER_TYPE_UNIX_PERMISSION) {
            // UNIX file permission
            entryBuilder.setUnixPermissionMode(Short.toUnsignedInt(extendedHeaderBuffer.getShort()));
        } else if (extendedHeaderType == EXTENDED_HEADER_TYPE_UNIX_UID_GID) {
            // UNIX group/user ID
            entryBuilder.setUnixGroupId(Short.toUnsignedInt(extendedHeaderBuffer.getShort()));
            entryBuilder.setUnixUserId(Short.toUnsignedInt(extendedHeaderBuffer.getShort()));
        } else if (extendedHeaderType == EXTENDED_HEADER_TYPE_UNIX_TIMESTAMP) {
            // UNIX last modified time
            entryBuilder.setLastModifiedDate(new Date(Integer.toUnsignedLong(extendedHeaderBuffer.getInt()) * 1000));
        }

        // Ignore unknown extended header
    }

    /**
     * Tests whether the compression method is a directory entry.
     *
     * @param compressionMethod the compression method
     * @return true if the compression method is a directory entry, false otherwise
     */
    private boolean isDirectory(final String compressionMethod) {
        return COMPRESSION_METHOD_DIRECTORY.equals(compressionMethod);
    }

    /**
     * Calculate the header sum for level 0 and 1 headers. The checksum is calculated by summing the
     * value of all bytes in the header except for the first two bytes (header length and header checksum)
     * and get the low 8 bits.
     *
     * @param buffer the buffer containing the header
     * @return checksum
     */
    private int calculateHeaderChecksum(final ByteBuffer buffer) {
        int sum = 0;
        for (int i = 2; i < buffer.limit(); i++) {
            sum += Byte.toUnsignedInt(buffer.get(i));
        }

        return sum & 0xff;
    }

    /**
     * Calculate the CRC16 checksum of the provided buffers.
     *
     * @param buffers the buffers to calculate the CRC16 checksum for
     * @return CRC16 checksum
     */
    private long calculateCRC16(final ByteBuffer... buffers) {
        final CRC16 crc = new CRC16();
        for (ByteBuffer buffer : buffers) {
            crc.update(buffer.array(), 0, buffer.limit());
        }

        return crc.getValue();
    }

    private void prepareDecompression(final LhaArchiveEntry entry) throws IOException {
        // Make sure we never read more than the compressed size of the entry
        this.currentCompressedStream = BoundedInputStream.builder()
                .setInputStream(in)
                .setMaxCount(entry.getCompressedSize())
                .get();

        if (isDirectory(entry.getCompressionMethod())) {
            // Directory entry
            this.currentDecompressedStream = new ByteArrayInputStream(new byte [0]);
        } else if (COMPRESSION_METHOD_LH0.equals(entry.getCompressionMethod()) || COMPRESSION_METHOD_LZ4.equals(entry.getCompressionMethod())) {
            // No compression
            this.currentDecompressedStream = ChecksumInputStream.builder()
                    .setChecksum(new CRC16())
                    .setExpectedChecksumValue(entry.getCrcValue())
                    .setInputStream(this.currentCompressedStream)
                    .get();
        } else if (COMPRESSION_METHOD_LH4.equals(entry.getCompressionMethod())) {
            this.currentDecompressedStream = ChecksumInputStream.builder()
                    .setChecksum(new CRC16())
                    .setExpectedChecksumValue(entry.getCrcValue())
                    .setInputStream(new Lh4CompressorInputStream(this.currentCompressedStream))
                    .get();
        } else if (COMPRESSION_METHOD_LH5.equals(entry.getCompressionMethod())) {
            this.currentDecompressedStream = ChecksumInputStream.builder()
                    .setChecksum(new CRC16())
                    .setExpectedChecksumValue(entry.getCrcValue())
                    .setInputStream(new Lh5CompressorInputStream(this.currentCompressedStream))
                    .get();
        } else if (COMPRESSION_METHOD_LH6.equals(entry.getCompressionMethod())) {
            this.currentDecompressedStream = ChecksumInputStream.builder()
                    .setChecksum(new CRC16())
                    .setExpectedChecksumValue(entry.getCrcValue())
                    .setInputStream(new Lh6CompressorInputStream(this.currentCompressedStream))
                    .get();
        } else if (COMPRESSION_METHOD_LH7.equals(entry.getCompressionMethod())) {
            this.currentDecompressedStream = ChecksumInputStream.builder()
                    .setChecksum(new CRC16())
                    .setExpectedChecksumValue(entry.getCrcValue())
                    .setInputStream(new Lh7CompressorInputStream(this.currentCompressedStream))
                    .get();
        } else {
            // Unsupported compression
            this.currentDecompressedStream = null;
        }
    }

    /**
     * Create a new ByteBuffer slice from the provided buffer at the specified position and length. This is needed until this
     * repo has been updated to use Java 9+ where we can use buffer.position(position).slice().limit(length) directly.
     *
     * @param buffer the buffer to slice from
     * @param position the position in the buffer to start slicing from
     * @param length the length of the slice
     * @return a new ByteBuffer slice with the specified position and length
     */
    private ByteBuffer byteBufferSlice(final ByteBuffer buffer, final int position, final int length) {
        return ByteBuffer.wrap(buffer.array(), position, length);
    }

    /**
     * Get a byte array from the ByteBuffer at the specified position and length.
     * This is needed until this repo has been updated to use Java 9+ where we
     * can use buffer.get(position, dst) directly.
     *
     * @param buffer the buffer to get the byte array from
     * @param position the position in the buffer to start reading from
     * @param dst the destination byte array to fill
     */
    private static void byteBufferGet(final ByteBuffer buffer, final int position, final byte[] dst) {
        for (int i = 0; i < dst.length; i++) {
            dst[i] = buffer.get(position + i);
        }
    }
}
