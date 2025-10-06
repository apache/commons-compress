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
package org.apache.commons.compress.archivers.arj;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.zip.CRC32;

import org.apache.commons.compress.archivers.AbstractArchiveBuilder;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.io.EndianUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.io.input.ChecksumInputStream;

/**
 * Implements the "arj" archive format as an InputStream.
 * <ul>
 * <li><a href="https://github.com/FarGroup/FarManager/blob/master/plugins/multiarc/arc.doc/arj.txt">Reference 1</a></li>
 * <li><a href="http://www.fileformat.info/format/arj/corion.htm">Reference 2</a></li>
 * </ul>
 *
 * @NotThreadSafe
 * @since 1.6
 */
public class ArjArchiveInputStream extends ArchiveInputStream<ArjArchiveEntry> {

    /**
     * Builds a new {@link ArjArchiveInputStream}.
     * <p>
     * For example:
     * </p>
     * <pre>{@code
     * ArjArchiveInputStream in = ArjArchiveInputStream.builder()
     *     .setPath(inputPath)
     *     .setCharset(StandardCharsets.UTF_8)
     *     .get();
     * }</pre>
     *
     * @since 1.29.0
     */
    public static final class Builder extends AbstractArchiveBuilder<ArjArchiveInputStream, Builder> {

        private boolean selfExtracting;

        private Builder() {
            setCharset(ENCODING_NAME);
        }

        /**
         * Enables compatibility with self-extracting (SFX) ARJ files.
         *
         * <p>When {@code true}, the stream is scanned forward to locate the first
         * valid ARJ main header. All bytes before that point are ignored, which
         * allows reading ARJ data embedded in an executable stub.</p>
         *
         * <p><strong>Caveat:</strong> this lenient pre-scan can mask corruption that
         * would otherwise be reported at the start of a normal {@code .arj} file.
         * Enable only when you expect an SFX input.</p>
         *
         * <p>Default: {@code false}.</p>
         *
         * @param selfExtracting {@code true} if the input stream is for a self-extracting archive
         * @return {@code this} instance
         * @since 1.29.0
         */
        public Builder setSelfExtracting(final boolean selfExtracting) {
            this.selfExtracting = selfExtracting;
            return asThis();
        }

        @Override
        public ArjArchiveInputStream get() throws IOException {
            return new ArjArchiveInputStream(this);
        }
    }

    private static final String ENCODING_NAME = "CP437";
    private static final int ARJ_MAGIC_1 = 0x60;
    private static final int ARJ_MAGIC_2 = 0xEA;
    private static final int MIN_FIRST_HEADER_SIZE = 30;

    /**
     * Creates a new builder.
     *
     * @return A new builder.
     * @since 1.29.0
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Checks if the signature matches what is expected for an arj file.
     *
     * @param signature the bytes to check
     * @param length    the number of bytes to check
     * @return true, if this stream is an arj archive stream, false otherwise
     */
    public static boolean matches(final byte[] signature, final int length) {
        return length >= 2 && (0xff & signature[0]) == ARJ_MAGIC_1 && (0xff & signature[1]) == ARJ_MAGIC_2;
    }

    private final MainHeader mainHeader;
    private LocalFileHeader currentLocalFileHeader;
    private InputStream currentInputStream;

    private ArjArchiveInputStream(final Builder builder) throws IOException {
        this(builder.getInputStream(), builder);
    }

    /**
     * Constructs the ArjInputStream, taking ownership of the inputStream that is passed in, and using the CP437 character encoding.
     *
     * @param inputStream the underlying stream, whose ownership is taken
     * @throws ArchiveException if an exception occurs while reading
     */
    public ArjArchiveInputStream(final InputStream inputStream) throws ArchiveException {
        this(inputStream, builder());
    }

    private ArjArchiveInputStream(final InputStream inputStream, final Builder builder) throws ArchiveException {
        super(inputStream, builder);
        try {
            mainHeader = readMainHeader(builder.selfExtracting);
            if ((mainHeader.arjFlags & MainHeader.Flags.GARBLED) != 0) {
                throw new ArchiveException("Encrypted ARJ files are unsupported");
            }
            if ((mainHeader.arjFlags & MainHeader.Flags.VOLUME) != 0) {
                throw new ArchiveException("Multi-volume ARJ files are unsupported");
            }
        } catch (final ArchiveException e) {
            throw e;
        } catch (final IOException e) {
            throw new ArchiveException(e.getMessage(), (Throwable) e);
        }
    }

    /**
     * Constructs the ArjInputStream, taking ownership of the inputStream that is passed in.
     *
     * @param inputStream the underlying stream, whose ownership is taken
     * @param charsetName the charset used for file names and comments in the archive. May be {@code null} to use the platform default.
     * @throws ArchiveException if an exception occurs while reading
     * @deprecated Since 1.29.0, use {@link #builder()}.
     */
    @Deprecated
    public ArjArchiveInputStream(final InputStream inputStream, final String charsetName) throws ArchiveException {
        this(inputStream, builder().setCharset(charsetName));
    }

    @Override
    public boolean canReadEntryData(final ArchiveEntry ae) {
        return ae instanceof ArjArchiveEntry && ((ArjArchiveEntry) ae).getMethod() == LocalFileHeader.Methods.STORED;
    }

    /**
     * Gets the archive's comment.
     *
     * @return the archive's comment
     */
    public String getArchiveComment() {
        return mainHeader.comment;
    }

    /**
     * Gets the archive's recorded name.
     *
     * @return the archive's name
     */
    public String getArchiveName() {
        return mainHeader.name;
    }

    @Override
    public ArjArchiveEntry getNextEntry() throws IOException {
        if (currentInputStream != null) {
            // return value ignored as IOUtils.skip ensures the stream is drained completely
            final InputStream input = currentInputStream;
            org.apache.commons.io.IOUtils.skip(input, Long.MAX_VALUE);
            currentInputStream.close();
            currentLocalFileHeader = null;
            currentInputStream = null;
        }

        currentLocalFileHeader = readLocalFileHeader();
        if (currentLocalFileHeader != null) {
            // @formatter:off
            final long currentPosition = getBytesRead();
            currentInputStream = BoundedInputStream.builder()
                    .setInputStream(in)
                    .setMaxCount(currentLocalFileHeader.compressedSize)
                    .setPropagateClose(false)
                    .setAfterRead(read -> {
                        if (read < 0) {
                            throw new EOFException(String.format(
                                    "Truncated ARJ archive: entry '%s' expected %,d bytes, but only %,d were read.",
                                    currentLocalFileHeader.name,
                                    currentLocalFileHeader.compressedSize,
                                    getBytesRead() - currentPosition
                            ));
                        }
                        count(read);
                    })
                    .get();
            // @formatter:on
            if (currentLocalFileHeader.method == LocalFileHeader.Methods.STORED) {
                // @formatter:off
                currentInputStream = ChecksumInputStream.builder()
                        .setChecksum(new CRC32())
                        .setInputStream(currentInputStream)
                        .setCountThreshold(currentLocalFileHeader.originalSize)
                        .setExpectedChecksumValue(currentLocalFileHeader.originalCrc32)
                        .get();
                // @formatter:on
            }
            return new ArjArchiveEntry(currentLocalFileHeader);
        }
        currentInputStream = null;
        return null;
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        org.apache.commons.io.IOUtils.checkFromIndexSize(b, off, len);
        if (len == 0) {
            return 0;
        }
        if (currentLocalFileHeader == null) {
            throw new IllegalStateException("No current arj entry");
        }
        if (currentLocalFileHeader.method != LocalFileHeader.Methods.STORED) {
            throw new ArchiveException("Unsupported compression method '%s'", currentLocalFileHeader.method);
        }
        return currentInputStream.read(b, off, len);
    }

    /**
     * Verifies the CRC32 checksum of the given data against the next four bytes read from the input stream.
     *
     * @param data The data to verify.
     * @return true if the checksum matches, false otherwise.
     * @throws EOFException If the end of the stream is reached before reading the checksum.
     * @throws IOException If an I/O error occurs.
     */
    @SuppressWarnings("Since15")
    private boolean checkCRC32(final byte[] data) throws IOException {
        final CRC32 crc32 = new CRC32();
        crc32.update(data);
        final long expectedCrc32 = readSwappedUnsignedInteger();
        return crc32.getValue() == expectedCrc32;
    }

    /**
     * Scans for the next valid ARJ header.
     *
     * @return The header bytes.
     * @throws EOFException If the end of the stream is reached before a valid header is found.
     * @throws IOException If an I/O error occurs.
     */
    private byte[] findMainHeader() throws IOException {
        byte[] basicHeaderBytes;
        while (true) {
            int first;
            int second = readUnsignedByte();
            do {
                first = second;
                second = readUnsignedByte();
            } while (first != ARJ_MAGIC_1 && second != ARJ_MAGIC_2);
            final int basicHeaderSize = readSwappedUnsignedShort();
            // At least two bytes are required for the null-terminated name and comment
            // The value 2600 is taken from the reference implementation
            if (MIN_FIRST_HEADER_SIZE + 2 <= basicHeaderSize && basicHeaderSize <= 2600) {
                basicHeaderBytes = org.apache.commons.io.IOUtils.toByteArray(in, basicHeaderSize);
                count(basicHeaderSize);
                if (checkCRC32(basicHeaderBytes)) {
                    return basicHeaderBytes;
                }
            }
            // CRC32 failed, continue scanning
        }
    }

    private static int readUnsignedByte(InputStream in) throws IOException {
        final int value = in.read();
        if (value == -1) {
            throw new EOFException();
        }
        return value & 0xff;
    }

    private int readSwappedUnsignedShort() throws IOException {
        final int value = EndianUtils.readSwappedUnsignedShort(in);
        count(2);
        return value;
    }

    private long readSwappedUnsignedInteger() throws IOException {
        final long value = EndianUtils.readSwappedUnsignedInteger(in);
        count(4);
        return value;
    }

    private int readUnsignedByte() throws IOException {
        final int value = readUnsignedByte(in);
        count(1);
        return value & 0xff;
    }

    private byte[] readHeader() throws IOException {
        final int first = readUnsignedByte();
        final int second = readUnsignedByte();
        if (first != ARJ_MAGIC_1 || second != ARJ_MAGIC_2) {
            throw new ArchiveException("Corrupted ARJ archive: invalid ARJ header signature 0x%02X 0x%02X", first, second);
        }
        final int basicHeaderSize = readSwappedUnsignedShort();
        if (basicHeaderSize == 0) {
            // End of archive
            return null;
        }
        // At least two bytes are required for the null-terminated name and comment
        // The value 2600 is taken from the reference implementation
        if (basicHeaderSize < MIN_FIRST_HEADER_SIZE + 2 || basicHeaderSize > 2600) {
            throw new ArchiveException("Corrupted ARJ archive: invalid ARJ header size %,d", basicHeaderSize);
        }
        final byte[] basicHeaderBytes = org.apache.commons.io.IOUtils.toByteArray(in, basicHeaderSize);
        count(basicHeaderSize);
        if (!checkCRC32(basicHeaderBytes)) {
            throw new ArchiveException("Corrupted ARJ archive: invalid ARJ header CRC32 checksum");
        }
        return basicHeaderBytes;
    }

    private LocalFileHeader readLocalFileHeader() throws IOException {
        final byte[] basicHeaderBytes = readHeader();
        if (basicHeaderBytes == null) {
            return null;
        }
        final LocalFileHeader localFileHeader = new LocalFileHeader();
        try (InputStream basicHeader = new ByteArrayInputStream(basicHeaderBytes)) {

            final int firstHeaderSize = readUnsignedByte(basicHeader);
            try (InputStream firstHeader = BoundedInputStream.builder().setInputStream(basicHeader).setMaxCount(firstHeaderSize - 1).get()) {

                localFileHeader.archiverVersionNumber = readUnsignedByte(firstHeader);
                localFileHeader.minVersionToExtract = readUnsignedByte(firstHeader);
                localFileHeader.hostOS = readUnsignedByte(firstHeader);
                localFileHeader.arjFlags = readUnsignedByte(firstHeader);
                localFileHeader.method = readUnsignedByte(firstHeader);
                localFileHeader.fileType = readUnsignedByte(firstHeader);
                localFileHeader.reserved = readUnsignedByte(firstHeader);
                localFileHeader.dateTimeModified = EndianUtils.readSwappedInteger(firstHeader);
                localFileHeader.compressedSize = EndianUtils.readSwappedUnsignedInteger(firstHeader);
                localFileHeader.originalSize = EndianUtils.readSwappedUnsignedInteger(firstHeader);
                localFileHeader.originalCrc32 = EndianUtils.readSwappedUnsignedInteger(firstHeader);
                localFileHeader.fileSpecPosition = EndianUtils.readSwappedShort(firstHeader);
                localFileHeader.fileAccessMode = EndianUtils.readSwappedShort(firstHeader);
                localFileHeader.firstChapter = readUnsignedByte(firstHeader);
                localFileHeader.lastChapter = readUnsignedByte(firstHeader);
                // Total read (including size byte): 10 + 4 * 4 + 2 * 2 = 30 bytes

                if (firstHeaderSize >= MIN_FIRST_HEADER_SIZE + 4) {
                    localFileHeader.extendedFilePosition = EndianUtils.readSwappedInteger(firstHeader);
                    // Total read (including size byte): 30 + 4 = 34 bytes
                    if (firstHeaderSize >= MIN_FIRST_HEADER_SIZE + 4 + 12) {
                        localFileHeader.dateTimeAccessed = EndianUtils.readSwappedInteger(firstHeader);
                        localFileHeader.dateTimeCreated = EndianUtils.readSwappedInteger(firstHeader);
                        localFileHeader.originalSizeEvenForVolumes = EndianUtils.readSwappedInteger(firstHeader);
                        // Total read (including size byte): 34 + 12 = 46 bytes
                    }
                }
            }

            localFileHeader.name = readString(basicHeader);
            localFileHeader.comment = readString(basicHeader);
        }

        final ArrayList<byte[]> extendedHeaders = new ArrayList<>();
        int extendedHeaderSize;
        while ((extendedHeaderSize = readSwappedUnsignedShort()) > 0) {
            final byte[] extendedHeaderBytes = org.apache.commons.io.IOUtils.toByteArray(in, extendedHeaderSize);
            count(extendedHeaderSize);
            if (!checkCRC32(extendedHeaderBytes)) {
                throw new ArchiveException("Corrupted ARJ archive: extended header CRC32 verification failure");
            }
            extendedHeaders.add(extendedHeaderBytes);
        }
        localFileHeader.extendedHeaders = extendedHeaders.toArray(new byte[0][]);

        return localFileHeader;
    }

    private MainHeader readMainHeader(final boolean selfExtracting) throws IOException {
        final byte[] basicHeaderBytes;
        try {
            basicHeaderBytes = selfExtracting ? findMainHeader() : readHeader();
        } catch (final EOFException e) {
            throw new ArchiveException("Archive ends without any headers", (Throwable) e);
        }
        final MainHeader header = new MainHeader();
        try (InputStream basicHeader = new ByteArrayInputStream(basicHeaderBytes)) {

            final int firstHeaderSize = readUnsignedByte(basicHeader);
            try (InputStream firstHeader = BoundedInputStream.builder().setInputStream(basicHeader).setMaxCount(firstHeaderSize - 1).get()) {

                header.archiverVersionNumber = readUnsignedByte(firstHeader);
                header.minVersionToExtract = readUnsignedByte(firstHeader);
                header.hostOS = readUnsignedByte(firstHeader);
                header.arjFlags = readUnsignedByte(firstHeader);
                header.securityVersion = readUnsignedByte(firstHeader);
                header.fileType = readUnsignedByte(firstHeader);
                header.reserved = readUnsignedByte(firstHeader);
                header.dateTimeCreated = EndianUtils.readSwappedInteger(firstHeader);
                header.dateTimeModified = EndianUtils.readSwappedInteger(firstHeader);
                header.archiveSize = EndianUtils.readSwappedUnsignedInteger(firstHeader);
                header.securityEnvelopeFilePosition = EndianUtils.readSwappedInteger(firstHeader);
                header.fileSpecPosition = EndianUtils.readSwappedShort(firstHeader);
                header.securityEnvelopeLength = EndianUtils.readSwappedShort(firstHeader);
                header.encryptionVersion = readUnsignedByte(firstHeader);
                header.lastChapter = readUnsignedByte(firstHeader);
                // Total read (including size byte): 10 + 4 * 4 + 2 * 2 = 30 bytes

                if (firstHeaderSize >= MIN_FIRST_HEADER_SIZE + 4) {
                    header.arjProtectionFactor = readUnsignedByte(firstHeader);
                    header.arjFlags2 = readUnsignedByte(firstHeader);
                    readUnsignedByte(firstHeader);
                    readUnsignedByte(firstHeader);
                    // Total read (including size byte): 30 + 4 = 34 bytes
                }
            }

            header.name = readString(basicHeader);
            header.comment = readString(basicHeader);
        }

        final int extendedHeaderSize = readSwappedUnsignedShort();
        if (extendedHeaderSize > 0) {
            header.extendedHeaderBytes = org.apache.commons.io.IOUtils.toByteArray(in, extendedHeaderSize);
            count(extendedHeaderSize);
            if (!checkCRC32(header.extendedHeaderBytes)) {
                throw new ArchiveException("Corrupted ARJ archive: extended header CRC32 verification failure");
            }
        }

        return header;
    }

    private String readString(final InputStream dataIn) throws IOException {
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            int nextByte;
            while ((nextByte = readUnsignedByte(dataIn)) != 0) {
                buffer.write(nextByte);
            }
            return buffer.toString(getCharset().name());
        }
    }
}
