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

package org.apache.commons.compress.compressors.gzip;

import java.io.OutputStream;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Objects;
import java.util.zip.Deflater;

import org.apache.commons.compress.CompressException;
import org.apache.commons.io.Charsets;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Parameters for the GZIP compressor.
 *
 * @see GzipCompressorInputStream
 * @see GzipCompressorOutputStream
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc1952">RFC 1952 GZIP File Format Specification</a>
 * @since 1.7
 */
public class GzipParameters {

    /**
     * Enumerates OS types.
     * <ul>
     * <li>0 - FAT filesystem (MS-DOS, OS/2, NT/Win32)</li>
     * <li>1 - Amiga</li>
     * <li>2 - VMS (or OpenVMS)</li>
     * <li>3 - Unix</li>
     * <li>4 - VM/CMS</li>
     * <li>5 - Atari TOS</li>
     * <li>6 - HPFS filesystem (OS/2, NT)</li>
     * <li>7 - Macintosh</li>
     * <li>8 - Z-System</li>
     * <li>9 - CP/M</li>
     * <li>10 - TOPS-20</li>
     * <li>11 - NTFS filesystem (NT)</li>
     * <li>12 - QDOS</li>
     * <li>13 - Acorn RISCOS</li>
     * <li>255 - unknown</li>
     * </ul>
     *
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc1952#page-7">RFC 1952: GZIP File Format Specification - OS (Operating System)</a>
     * @since 1.28.0
     */
    public enum OS {

        /**
         * 13: Acorn RISCOS.
         */
        ACORN_RISCOS(OS_ACORN_RISCOS),

        /**
         * 1: Amiga.
         */
        AMIGA(OS_AMIGA),

        /**
         * 5: Atari TOS.
         */
        ATARI_TOS(OS_ATARI_TOS),

        /**
         * 9: CP/M.
         */
        CPM(OS_CPM),

        // @formatter:off
        /**
         * 0: FAT filesystem (MS-DOS, OS/2, NT/Win32).
         */
        FAT(OS_FAT),

        /**
         * 6: HPFS filesystem (OS/2, NT).
         */
        HPFS(OS_HPFS),

        /**
         * 7: Macintosh.
         */
        MACINTOSH(OS_MACINTOSH),

        /**
         * 11: NTFS filesystem (NT).
         */
        NTFS(OS_NTFS),

        /**
         * 12: QDOS.
         */
        QDOS(OS_QDOS),

        /**
         * 10: TOPS-20.
         */
        TOPS_20(OS_TOPS_20),

        /**
         * 3: Unix.
         */
        UNIX(OS_UNIX),

        /**
         * 255: unknown.
         */
        UNKNOWN(OS_UNKNOWN),

        /**
         * 4: VM/CMS.
         */
        VM_CMS(OS_VM_CMS),

        /**
         * 2: VMS (or OpenVMS).
         */
        VMS(OS_VMS),

        /**
         * 8: Z-System.
         */
        Z_SYSTEM(OS_Z_SYSTEM);
        // @formatter:on

        /**
         * Gets the {@link OS} matching the given code.
         *
         * @param code an OS or {@link #UNKNOWN} for no match.
         * @return a {@link OS}.
         * @throws CompressException Thrown when the {@code code} is undefined, as opposed to {@code UNKNOWN}.
         */
        public static OS from(final int code) throws CompressException {
            return from(code, true);
        }

        static OS from(final int code, final boolean strict) throws CompressException {
            switch (code) {
            case OS_ACORN_RISCOS:
                return ACORN_RISCOS;
            case OS_AMIGA:
                return AMIGA;
            case OS_ATARI_TOS:
                return ATARI_TOS;
            case OS_CPM:
                return CPM;
            case OS_FAT:
                return FAT;
            case OS_HPFS:
                return HPFS;
            case OS_MACINTOSH:
                return MACINTOSH;
            case OS_NTFS:
                return NTFS;
            case OS_QDOS:
                return QDOS;
            case OS_TOPS_20:
                return TOPS_20;
            case OS_UNIX:
                return UNIX;
            case OS_UNKNOWN:
                return UNKNOWN;
            case OS_VM_CMS:
                return VM_CMS;
            case OS_VMS:
                return VMS;
            case OS_Z_SYSTEM:
                return Z_SYSTEM;
            default:
                if (strict) {
                    throw new CompressException("Unknown operating system code: %s", code);
                }
                return UNKNOWN;
            }
        }

        private final int type;

        /**
         * Constructs a new instance.
         *
         * @param type the OS type.
         */
        OS(final int type) {
            this.type = type;
        }

        /**
         * Gets the OS type.
         *
         * @return the OS type.
         */
        public int type() {
            return type;
        }

    }

    private static final int BUFFER_SIZE = 512;

    /**
     * 13: Acorn RISCOS.
     */
    private static final int OS_ACORN_RISCOS = 13;

    /**
     * 1: Amiga.
     */
    private static final int OS_AMIGA = 1;

    /**
     * 5: Atari TOS.
     */
    private static final int OS_ATARI_TOS = 5;

    /**
     * 9: CP/M.
     */
    private static final int OS_CPM = 9;

    /**
     * 0: FAT.
     */
    private static final int OS_FAT = 0;

    /**
     * 6: HPFS filesystem (OS/2, NT).
     */
    private static final int OS_HPFS = 6;

    /**
     * 7: Macintosh.
     */
    private static final int OS_MACINTOSH = 7;

    /**
     * 11: NTFS filesystem (NT).
     */
    private static final int OS_NTFS = 11;

    /**
     * 12: QDOS.
     */
    private static final int OS_QDOS = 12;

    /**
     * 10: TOPS-20.
     */
    private static final int OS_TOPS_20 = 10;

    /**
     * 3: Unix.
     */
    private static final int OS_UNIX = 3;

    /**
     * 255: unknown.
     */
    private static final int OS_UNKNOWN = 255;

    /**
     * 4: VM/CMS.
     */
    private static final int OS_VM_CMS = 4;

    /**
     * 2: VMS (or OpenVMS).
     */
    private static final int OS_VMS = 2;

    /**
     * 8: Z-System.
     */
    private static final int OS_Z_SYSTEM = 8;

    private int bufferSize = BUFFER_SIZE;

    private String comment;
    private int compressionLevel = Deflater.DEFAULT_COMPRESSION;
    private int deflateStrategy = Deflater.DEFAULT_STRATEGY;
    private ExtraField extraField;
    private int extraFieldXlen;
    private String fileName;
    private Charset fileNameCharset = GzipUtils.GZIP_ENCODING;
    private boolean headerCrc;
    /**
     * The most recent modification time (MTIME) of the original file being compressed.
     * <p>
     * The time is in Unix format, for example, seconds since 00:00:00 GMT, Jan. 1, 1970. (Note that this may cause problems for MS-DOS and other systems that
     * use local rather than Universal time.) If the compressed data did not come from a file, MTIME is set to the time at which compression started. MTIME = 0
     * means no time stamp is available.
     * </p>
     */
    private Instant modificationInstant = Instant.EPOCH;
    private OS operatingSystem = OS.UNKNOWN; // Unknown OS by default
    private long trailerCrc;
    private long trailerISize;

    /**
     * Constructs a new instance with default values.
     */
    public GzipParameters() {
        // Default constructor
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof GzipParameters)) {
            return false;
        }
        final GzipParameters other = (GzipParameters) obj;
        return bufferSize == other.bufferSize && Objects.equals(comment, other.comment) && compressionLevel == other.compressionLevel
                && deflateStrategy == other.deflateStrategy && Objects.equals(extraField, other.extraField) && Objects.equals(fileName, other.fileName)
                && Objects.equals(fileNameCharset, other.fileNameCharset) && headerCrc == other.headerCrc
                && Objects.equals(modificationInstant, other.modificationInstant) && operatingSystem == other.operatingSystem && trailerCrc == other.trailerCrc
                && trailerISize == other.trailerISize;
    }

    /**
     * Gets size of the buffer used to retrieve compressed data.
     *
     * @return The size of the buffer used to retrieve compressed data.
     * @see #setBufferSize(int)
     * @since 1.21
     */
    public int getBufferSize() {
        return this.bufferSize;
    }

    /**
     * Gets an arbitrary user-defined comment.
     *
     * @return a user-defined comment.
     */
    public String getComment() {
        return comment;
    }

    /**
     * Gets the compression level.
     *
     * @return the compression level.
     * @see Deflater#NO_COMPRESSION
     * @see Deflater#BEST_SPEED
     * @see Deflater#DEFAULT_COMPRESSION
     * @see Deflater#BEST_COMPRESSION
     */
    public int getCompressionLevel() {
        return compressionLevel;
    }

    /**
     * Gets the deflater strategy.
     *
     * @return the deflater strategy, {@link Deflater#DEFAULT_STRATEGY} by default.
     * @see #setDeflateStrategy(int)
     * @see Deflater#setStrategy(int)
     * @since 1.23
     */
    public int getDeflateStrategy() {
        return deflateStrategy;
    }

    /**
     * Gets the Extra subfields from the header.
     *
     * @return the extra subfields from the header.
     * @since 1.28.0
     */
    public ExtraField getExtraField() {
        return extraField;
    }

    int getExtraFieldXlen() {
        return extraFieldXlen;
    }

    /**
     * Gets the file name.
     *
     * @return the file name.
     * @deprecated Use {@link #getFileName()}.
     */
    @Deprecated
    public String getFilename() {
        return fileName;
    }

    /**
     * Gets the file name.
     *
     * @return the file name.
     * @since 1.25.0
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Gets the Charset to use for writing file names and comments.
     * <p>
     * The default value is {@link GzipUtils#GZIP_ENCODING}.
     * </p>
     *
     * @return the Charset to use for writing file names and comments.
     * @since 1.28.0
     */
    public Charset getFileNameCharset() {
        return fileNameCharset;
    }

    /**
     * Returns if the header CRC is to be added (when writing) or was present (when reading).
     *
     * @return true is header CRC will be added (on write) or was found (after read).
     * @since 1.28.0
     */
    public boolean getHeaderCRC() {
        return headerCrc;
    }

    /**
     * Gets the most recent modification time (MTIME) of the original file being compressed.
     *
     * @return the most recent modification time.
     * @since 1.28.0
     */
    public Instant getModificationInstant() {
        return modificationInstant;
    }

    /**
     * Gets the most recent modification time (MTIME) of the original file being compressed, in seconds since 00:00:00 GMT, Jan. 1, 1970.
     * <p>
     * The time is in Unix format, for example, seconds since 00:00:00 GMT, Jan. 1, 1970. (Note that this may cause problems for MS-DOS and other systems that
     * use local rather than Universal time.) If the compressed data did not come from a file, MTIME is set to the time at which compression started. MTIME = 0
     * means no time stamp is available.
     * </p>
     *
     * @return the most recent modification time in seconds since 00:00:00 GMT, Jan. 1, 1970.
     */
    public long getModificationTime() {
        return modificationInstant.getEpochSecond();
    }

    /**
     * Gets the OS code type.
     *
     * @return the OS code type.
     */
    public int getOperatingSystem() {
        return operatingSystem.type;
    }

    /**
     * Gets the OS type.
     *
     * @return the OS type.
     * @since 1.28.0
     */
    public OS getOS() {
        return operatingSystem;
    }

    /**
     * Gets the trailer CRC value.
     *
     * @return the trailer CRC value.
     * @since 1.28.0
     */
    public long getTrailerCrc() {
        return trailerCrc;
    }

    /**
     * Gets the trailer ISIZE value.
     *
     * @return the trailer ISIZE value.
     * @since 1.28.0
     */
    public long getTrailerISize() {
        return trailerISize;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bufferSize, comment, compressionLevel, deflateStrategy, extraField, fileName, fileNameCharset, headerCrc, modificationInstant,
                operatingSystem, trailerCrc, trailerISize);
    }

    private String requireNonNulByte(final String text) {
        if (StringUtils.isNotEmpty(text) && ArrayUtils.contains(text.getBytes(fileNameCharset), (byte) 0)) {
            throw new IllegalArgumentException("String encoded in Charset '" + fileNameCharset + "' contains the nul byte 0 which is not supported in gzip.");
        }
        return text;
    }

    /**
     * Sets size of the buffer used to retrieve compressed data from {@link Deflater} and write to underlying {@link OutputStream}.
     *
     * @param bufferSize the bufferSize to set. Must be a positive type.
     * @since 1.21
     */
    public void setBufferSize(final int bufferSize) {
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("Invalid buffer size: " + bufferSize);
        }
        this.bufferSize = bufferSize;
    }

    /**
     * Sets an arbitrary user-defined comment.
     *
     * @param comment a user-defined comment.
     * @throws IllegalArgumentException if the encoded bytes would contain a nul byte '\0' reserved for gzip field termination.
     */
    public void setComment(final String comment) {
        this.comment = requireNonNulByte(comment);
    }

    /**
     * Sets the compression level.
     *
     * @param compressionLevel the compression level (between 0 and 9)
     * @see Deflater#NO_COMPRESSION
     * @see Deflater#BEST_SPEED
     * @see Deflater#DEFAULT_COMPRESSION
     * @see Deflater#BEST_COMPRESSION
     */
    public void setCompressionLevel(final int compressionLevel) {
        if (compressionLevel < -1 || compressionLevel > 9) {
            throw new IllegalArgumentException("Invalid gzip compression level: " + compressionLevel);
        }
        this.compressionLevel = compressionLevel;
    }

    /**
     * Sets the deflater strategy.
     *
     * @param deflateStrategy the new compression strategy
     * @see Deflater#setStrategy(int)
     * @since 1.23
     */
    public void setDeflateStrategy(final int deflateStrategy) {
        this.deflateStrategy = deflateStrategy;
    }

    /**
     * Sets the extra subfields. Note that a non-null extra will appear in the gzip header regardless of the presence of subfields, while a null extra will not
     * appear at all.
     *
     * @param extra the series of extra subfields.
     * @since 1.28.0
     */
    public void setExtraField(final ExtraField extra) {
        this.extraField = extra;
    }

    void setExtraFieldXlen(final int extraFieldXlen) {
        this.extraFieldXlen = extraFieldXlen;
    }

    /**
     * Sets the name of the compressed file.
     *
     * @param fileName the name of the file without the directory path
     * @throws IllegalArgumentException if the encoded bytes would contain a nul byte '\0' reserved for gzip field termination.
     * @deprecated Use {@link #setFileName(String)}.
     */
    @Deprecated
    public void setFilename(final String fileName) {
        setFileName(fileName);
    }

    /**
     * Sets the name of the compressed file.
     *
     * @param fileName the name of the file without the directory path
     * @throws IllegalArgumentException if the encoded bytes would contain a nul byte '\0' reserved for gzip field termination.
     */
    public void setFileName(final String fileName) {
        this.fileName = requireNonNulByte(fileName);
    }

    /**
     * Sets the Charset to use for writing file names and comments, where null maps to {@link GzipUtils#GZIP_ENCODING}.
     * <p>
     * <em>Setting a value other than {@link GzipUtils#GZIP_ENCODING} is not compliant with the <a href="https://datatracker.ietf.org/doc/html/rfc1952">RFC 1952
     * GZIP File Format Specification</a></em>. Use at your own risk of interoperability issues.
     * </p>
     * <p>
     * The default value is {@link GzipUtils#GZIP_ENCODING}.
     * </p>
     *
     * @param charset the Charset to use for writing file names and comments, null maps to {@link GzipUtils#GZIP_ENCODING}.
     * @since 1.28.0
     */
    public void setFileNameCharset(final Charset charset) {
        this.fileNameCharset = Charsets.toCharset(charset, GzipUtils.GZIP_ENCODING);
    }

    /**
     * Establishes the presence of the header flag FLG.FHCRC and its headers CRC16 value.
     *
     * @param headerCRC when true, the header CRC16 (actually low 16 buts of a CRC32) is calculated and inserted
     *         in the gzip header on write; on read it means the field was present.
     * @since 1.28.0
     */
    public void setHeaderCRC(final boolean headerCRC) {
        this.headerCrc = headerCRC;
    }

    /**
     * Sets the modification time (MTIME) of the compressed file.
     *
     * @param modificationTime the modification time, in milliseconds
     * @since 1.28.0
     */
    public void setModificationInstant(final Instant modificationTime) {
        this.modificationInstant = modificationTime != null ? modificationTime : Instant.EPOCH;
    }

    /**
     * Sets the modification time (MTIME) of the compressed file, in seconds since 00:00:00 GMT, Jan. 1, 1970.
     * <p>
     * The time is in Unix format, for example, seconds since 00:00:00 GMT, Jan. 1, 1970. (Note that this may cause problems for MS-DOS and other systems that
     * use local rather than Universal time.) If the compressed data did not come from a file, MTIME is set to the time at which compression started. MTIME = 0
     * means no time stamp is available.
     * </p>
     *
     * @param modificationTimeSeconds the modification time, in seconds.
     */
    public void setModificationTime(final long modificationTimeSeconds) {
        this.modificationInstant = Instant.ofEpochSecond(modificationTimeSeconds);
    }

    /**
     * Sets the operating system on which the compression took place. The defined values are:
     * <ul>
     * <li>0: FAT file system (MS-DOS, OS/2, NT/Win32)</li>
     * <li>1: Amiga</li>
     * <li>2: VMS (or OpenVMS)</li>
     * <li>3: Unix</li>
     * <li>4: VM/CMS</li>
     * <li>5: Atari TOS</li>
     * <li>6: HPFS file system (OS/2, NT)</li>
     * <li>7: Macintosh</li>
     * <li>8: Z-System</li>
     * <li>9: CP/M</li>
     * <li>10: TOPS-20</li>
     * <li>11: NTFS file system (NT)</li>
     * <li>12: QDOS</li>
     * <li>13: Acorn RISCOS</li>
     * <li>255: Unknown</li>
     * </ul>
     *
     * @param operatingSystem the code of the operating system
     * @throws CompressException Thrown when the {@code code} is undefined, as opposed to {@code UNKNOWN (255)}.
     */
    public void setOperatingSystem(final int operatingSystem) throws CompressException {
        this.operatingSystem = OS.from(operatingSystem);
    }

    /**
     * Sets the operating system on which the compression took place.
     *
     * @param os operating system, null resets to {@link OS#UNKNOWN}.
     * @since 1.28.0
     */
    public void setOS(final OS os) {
        this.operatingSystem = os != null ? os : OS.UNKNOWN;
    }

    void setTrailerCrc(final long trailerCrc) {
        this.trailerCrc = trailerCrc;
    }

    void setTrailerISize(final long trailerISize) {
        this.trailerISize = trailerISize;
    }


    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("GzipParameters [bufferSize=").append(bufferSize).append(", comment=").append(comment).append(", compressionLevel=")
                .append(compressionLevel).append(", deflateStrategy=").append(deflateStrategy).append(", extraField=").append(extraField).append(", fileName=")
                .append(fileName).append(", fileNameCharset=").append(fileNameCharset).append(", headerCrc=").append(headerCrc).append(", modificationInstant=")
                .append(modificationInstant).append(", operatingSystem=").append(operatingSystem).append(", trailerCrc=").append(trailerCrc)
                .append(", trailerISize=").append(trailerISize).append("]");
        return builder.toString();
    }
}
