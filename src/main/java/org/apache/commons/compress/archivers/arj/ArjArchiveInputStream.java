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
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.zip.CRC32;

import org.apache.commons.compress.archivers.AbstractArchiveBuilder;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.utils.ArchiveUtils;
import org.apache.commons.compress.utils.IOUtils;
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

        private Builder() {
            setCharset(ENCODING_NAME);
        }

        @Override
        public ArjArchiveInputStream get() throws IOException {
            return new ArjArchiveInputStream(this);
        }
    }

    private static final String ENCODING_NAME = "CP437";
    private static final int ARJ_MAGIC_1 = 0x60;
    private static final int ARJ_MAGIC_2 = 0xEA;

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

    private final DataInputStream dis;
    private final MainHeader mainHeader;
    private LocalFileHeader currentLocalFileHeader;
    private InputStream currentInputStream;

    private ArjArchiveInputStream(final Builder builder) throws IOException {
        super(builder);
        dis = new DataInputStream(in);
        mainHeader = readMainHeader();
        if ((mainHeader.arjFlags & MainHeader.Flags.GARBLED) != 0) {
            throw new ArchiveException("Encrypted ARJ files are unsupported");
        }
        if ((mainHeader.arjFlags & MainHeader.Flags.VOLUME) != 0) {
            throw new ArchiveException("Multi-volume ARJ files are unsupported");
        }
    }

    /**
     * Constructs the ArjInputStream, taking ownership of the inputStream that is passed in, and using the CP437 character encoding.
     *
     * <p>Since 1.29.0: throws {@link IOException}.</p>
     *
     * @param inputStream the underlying stream, whose ownership is taken
     * @throws IOException if an exception occurs while reading
     */
    public ArjArchiveInputStream(final InputStream inputStream) throws IOException {
        this(builder().setInputStream(inputStream));
    }

    /**
     * Constructs the ArjInputStream, taking ownership of the inputStream that is passed in.
     *
     * <p>Since 1.29.0: throws {@link IOException}.</p>
     *
     * @param inputStream the underlying stream, whose ownership is taken
     * @param charsetName the charset used for file names and comments in the archive. May be {@code null} to use the platform default.
     * @throws IOException if an exception occurs while reading
     * @deprecated Since 1.29.0, use {@link #builder()}.
     */
    @Deprecated
    public ArjArchiveInputStream(final InputStream inputStream, final String charsetName) throws IOException {
        this(builder().setInputStream(inputStream).setCharset(charsetName));
    }

    @Override
    public boolean canReadEntryData(final ArchiveEntry ae) {
        return ae instanceof ArjArchiveEntry && ((ArjArchiveEntry) ae).getMethod() == LocalFileHeader.Methods.STORED;
    }

    @Override
    public void close() throws IOException {
        dis.close();
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
            currentInputStream = BoundedInputStream.builder()
                    .setInputStream(dis)
                    .setMaxCount(currentLocalFileHeader.compressedSize)
                    .setPropagateClose(false)
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

    private int read16(final DataInputStream dataIn) throws IOException {
        final int value = dataIn.readUnsignedShort();
        count(2);
        return Integer.reverseBytes(value) >>> 16;
    }

    private int read32(final DataInputStream dataIn) throws IOException {
        final int value = dataIn.readInt();
        count(4);
        return Integer.reverseBytes(value);
    }

    private int read8(final DataInputStream dataIn) throws IOException {
        final int value = dataIn.readUnsignedByte();
        count(1);
        return value;
    }

    private void readExtraData(final int firstHeaderSize, final DataInputStream firstHeader, final LocalFileHeader localFileHeader) throws IOException {
        if (firstHeaderSize >= 33) {
            localFileHeader.extendedFilePosition = read32(firstHeader);
            if (firstHeaderSize >= 45) {
                localFileHeader.dateTimeAccessed = read32(firstHeader);
                localFileHeader.dateTimeCreated = read32(firstHeader);
                localFileHeader.originalSizeEvenForVolumes = read32(firstHeader);
                pushedBackBytes(12);
            }
            pushedBackBytes(4);
        }
    }

    private byte[] readHeader() throws IOException {
        boolean found = false;
        byte[] basicHeaderBytes = null;
        do {
            int first;
            int second = read8(dis);
            do {
                first = second;
                second = read8(dis);
            } while (first != ARJ_MAGIC_1 && second != ARJ_MAGIC_2);
            final int basicHeaderSize = read16(dis);
            if (basicHeaderSize == 0) {
                // end of archive
                return null;
            }
            if (basicHeaderSize <= 2600) {
                basicHeaderBytes = readRange(dis, basicHeaderSize);
                final long basicHeaderCrc32 = read32(dis) & 0xFFFFFFFFL;
                final CRC32 crc32 = new CRC32();
                crc32.update(basicHeaderBytes);
                if (basicHeaderCrc32 == crc32.getValue()) {
                    found = true;
                }
            }
        } while (!found);
        return basicHeaderBytes;
    }

    private LocalFileHeader readLocalFileHeader() throws IOException {
        final byte[] basicHeaderBytes = readHeader();
        if (basicHeaderBytes == null) {
            return null;
        }
        try (DataInputStream basicHeader = new DataInputStream(new ByteArrayInputStream(basicHeaderBytes))) {

            final int firstHeaderSize = basicHeader.readUnsignedByte();
            final byte[] firstHeaderBytes = readRange(basicHeader, firstHeaderSize - 1);
            pushedBackBytes(firstHeaderBytes.length);
            try (DataInputStream firstHeader = new DataInputStream(new ByteArrayInputStream(firstHeaderBytes))) {

                final LocalFileHeader localFileHeader = new LocalFileHeader();
                localFileHeader.archiverVersionNumber = firstHeader.readUnsignedByte();
                localFileHeader.minVersionToExtract = firstHeader.readUnsignedByte();
                localFileHeader.hostOS = firstHeader.readUnsignedByte();
                localFileHeader.arjFlags = firstHeader.readUnsignedByte();
                localFileHeader.method = firstHeader.readUnsignedByte();
                localFileHeader.fileType = firstHeader.readUnsignedByte();
                localFileHeader.reserved = firstHeader.readUnsignedByte();
                localFileHeader.dateTimeModified = read32(firstHeader);
                localFileHeader.compressedSize = 0xffffFFFFL & read32(firstHeader);
                localFileHeader.originalSize = 0xffffFFFFL & read32(firstHeader);
                localFileHeader.originalCrc32 = 0xffffFFFFL & read32(firstHeader);
                localFileHeader.fileSpecPosition = read16(firstHeader);
                localFileHeader.fileAccessMode = read16(firstHeader);
                pushedBackBytes(20);
                localFileHeader.firstChapter = firstHeader.readUnsignedByte();
                localFileHeader.lastChapter = firstHeader.readUnsignedByte();

                readExtraData(firstHeaderSize, firstHeader, localFileHeader);

                localFileHeader.name = readEntryName(basicHeader);
                localFileHeader.comment = readComment(basicHeader);

                final ArrayList<byte[]> extendedHeaders = new ArrayList<>();
                int extendedHeaderSize;
                while ((extendedHeaderSize = read16(dis)) > 0) {
                    final byte[] extendedHeaderBytes = readRange(dis, extendedHeaderSize);
                    final long extendedHeaderCrc32 = 0xffffFFFFL & read32(dis);
                    final CRC32 crc32 = new CRC32();
                    crc32.update(extendedHeaderBytes);
                    if (extendedHeaderCrc32 != crc32.getValue()) {
                        throw new ArchiveException("Extended header CRC32 verification failure");
                    }
                    extendedHeaders.add(extendedHeaderBytes);
                }
                localFileHeader.extendedHeaders = extendedHeaders.toArray(new byte[0][]);

                return localFileHeader;
            }
        }
    }

    private MainHeader readMainHeader() throws IOException {
        final byte[] basicHeaderBytes = readHeader();
        if (basicHeaderBytes == null) {
            throw new ArchiveException("Archive ends without any headers");
        }
        final DataInputStream basicHeader = new DataInputStream(new ByteArrayInputStream(basicHeaderBytes));

        final int firstHeaderSize = basicHeader.readUnsignedByte();
        final byte[] firstHeaderBytes = readRange(basicHeader, firstHeaderSize - 1);
        pushedBackBytes(firstHeaderBytes.length);

        final DataInputStream firstHeader = new DataInputStream(new ByteArrayInputStream(firstHeaderBytes));

        final MainHeader header = new MainHeader();
        header.archiverVersionNumber = firstHeader.readUnsignedByte();
        header.minVersionToExtract = firstHeader.readUnsignedByte();
        header.hostOS = firstHeader.readUnsignedByte();
        header.arjFlags = firstHeader.readUnsignedByte();
        header.securityVersion = firstHeader.readUnsignedByte();
        header.fileType = firstHeader.readUnsignedByte();
        header.reserved = firstHeader.readUnsignedByte();
        header.dateTimeCreated = read32(firstHeader);
        header.dateTimeModified = read32(firstHeader);
        header.archiveSize = 0xffffFFFFL & read32(firstHeader);
        header.securityEnvelopeFilePosition = read32(firstHeader);
        header.fileSpecPosition = read16(firstHeader);
        header.securityEnvelopeLength = read16(firstHeader);
        pushedBackBytes(20); // count has already counted them via readRange
        header.encryptionVersion = firstHeader.readUnsignedByte();
        header.lastChapter = firstHeader.readUnsignedByte();

        if (firstHeaderSize >= 33) {
            header.arjProtectionFactor = firstHeader.readUnsignedByte();
            header.arjFlags2 = firstHeader.readUnsignedByte();
            firstHeader.readUnsignedByte();
            firstHeader.readUnsignedByte();
        }

        header.name = readEntryName(basicHeader);
        header.comment = readComment(basicHeader);

        final int extendedHeaderSize = read16(dis);
        if (extendedHeaderSize > 0) {
            header.extendedHeaderBytes = readRange(dis, extendedHeaderSize);
            final long extendedHeaderCrc32 = 0xffffFFFFL & read32(dis);
            final CRC32 crc32 = new CRC32();
            crc32.update(header.extendedHeaderBytes);
            if (extendedHeaderCrc32 != crc32.getValue()) {
                throw new ArchiveException("Extended header CRC32 verification failure");
            }
        }

        return header;
    }

    private byte[] readRange(final InputStream in, final int len) throws IOException {
        final byte[] b = IOUtils.readRange(in, len);
        count(b.length);
        if (b.length < len) {
            throw new EOFException();
        }
        return b;
    }

    private String readComment(DataInputStream dataIn) throws IOException {
        return new String(readString(dataIn).toByteArray(), getCharset());
    }

    private String readEntryName(DataInputStream dataIn) throws IOException {
        final ByteArrayOutputStream buffer = readString(dataIn);
        ArchiveUtils.checkEntryNameLength(buffer.size(), getMaxEntryNameLength(), "ARJ");
        return new String(buffer.toByteArray(), getCharset());
    }

    private ByteArrayOutputStream readString(DataInputStream dataIn) throws IOException {
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            int nextByte;
            while ((nextByte = dataIn.readUnsignedByte()) != 0) {
                buffer.write(nextByte);
            }
            return buffer;
        }
    }
}
