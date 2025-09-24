package org.apache.commons.compress.archivers.fuzzing;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

/**
 * Writes a standard (non-ZIP64) End of Central Directory (EOCD) record.
 *
 * Structure (per PKWARE APPNOTE):
 *
 *  Offset Size  Field
 *  ------ ----  --------------------------------------------
 *  0      4     End of central dir signature (0x06054b50)
 *  4      2     Number of this disk
 *  6      2     Disk where central directory starts
 *  8      2     Number of central directory records on this disk
 * 10      2     Total number of central directory records
 * 12      4     Size of central directory (bytes)
 * 16      4     Offset of start of central directory (w.r.t. start of file)
 * 20      2     ZIP file comment length (n)
 * 22      n     ZIP file comment (optional)
 *
 * This class intentionally does not emit ZIP64 EOCD structures.
 */
public final class ZipEndOfCentralDirectory {
    private static final byte[] MAGIC = { 0x50, 0x4b, 0x05, 0x06 }; // 0x06054b50 in stream
    private static final int FIXED_SIZE = 22;

    // Fields (classic ZIP, single-disk by default)
    private final int numberOfThisDisk;                   // u16
    private final int diskWhereCentralDirectoryStarts;    // u16
    private final int entriesOnThisDisk;                  // u16
    private final int totalEntries;                       // u16
    private final long centralDirectorySize;              // u32
    private final long centralDirectoryOffset;            // u32
    private final byte[] comment;                         // length fits u16

    /**
     * Minimal single-disk EOCD with no comment.
     *
     * @param totalEntries total number of CD file headers
     * @param centralDirectorySize size in bytes of the central directory
     * @param centralDirectoryOffset file offset (from BOF) where the central directory starts
     */
    public ZipEndOfCentralDirectory(int totalEntries,
                                    long centralDirectorySize,
                                    long centralDirectoryOffset) {
        this(0, 0, totalEntries, totalEntries, centralDirectorySize, centralDirectoryOffset, new byte[0]);
    }

    /**
     * General constructor (still non-ZIP64).
     *
     * @param numberOfThisDisk typically 0
     * @param diskWhereCentralDirectoryStarts typically 0
     * @param entriesOnThisDisk number of CD records on this disk (<= 65535)
     * @param totalEntries total number of CD records in archive (<= 65535)
     * @param centralDirectorySize size of CD in bytes (<= 0xFFFFFFFF)
     * @param centralDirectoryOffset offset of CD start (<= 0xFFFFFFFF)
     * @param comment archive comment bytes (length <= 65535)
     */
    public ZipEndOfCentralDirectory(int numberOfThisDisk,
                                    int diskWhereCentralDirectoryStarts,
                                    int entriesOnThisDisk,
                                    int totalEntries,
                                    long centralDirectorySize,
                                    long centralDirectoryOffset,
                                    byte[] comment) {
        this.numberOfThisDisk = numberOfThisDisk;
        this.diskWhereCentralDirectoryStarts = diskWhereCentralDirectoryStarts;
        this.entriesOnThisDisk = entriesOnThisDisk;
        this.totalEntries = totalEntries;
        this.centralDirectorySize = centralDirectorySize;
        this.centralDirectoryOffset = centralDirectoryOffset;
        this.comment = (comment == null) ? new byte[0] : comment;
        validateClassicLimits();
    }

    /**
     * Convenience factory for string comments.
     */
    public static ZipEndOfCentralDirectory withComment(Charset charset,
                                                       int totalEntries,
                                                       long centralDirectorySize,
                                                       long centralDirectoryOffset,
                                                       String comment) {
        byte[] bytes = (comment == null) ? new byte[0] : comment.getBytes(charset);
        return new ZipEndOfCentralDirectory(0, 0, totalEntries, totalEntries,
                centralDirectorySize, centralDirectoryOffset, bytes);
    }

    private void validateClassicLimits() throws IllegalArgumentException {
        // u16 fields
        if (!fitsU16(numberOfThisDisk)
                || !fitsU16(diskWhereCentralDirectoryStarts)
                || !fitsU16(entriesOnThisDisk)
                || !fitsU16(totalEntries)
                || !fitsU16(comment.length)) {
            throw new IllegalArgumentException("EOCD u16 field out of range (ZIP64 needed?)");
        }
        // u32 fields
        if (!fitsU32(centralDirectorySize) || !fitsU32(centralDirectoryOffset)) {
            throw new IllegalArgumentException("Central directory size/offset out of range (ZIP64 needed?)");
        }
    }

    private static boolean fitsU16(int v) {
        return (v & 0xFFFF_0000) == 0 && v >= 0;
    }

    private static boolean fitsU16(int v, int extra) {
        int sum = v + extra;
        return sum >= 0 && sum <= 0xFFFF;
    }

    private static boolean fitsU32(long v) {
        return (v & 0xFFFF_FFFF_0000_0000L) == 0 && v >= 0;
    }

    public int getRecordLength() {
        return FIXED_SIZE + comment.length;
    }

    /**
     * Writes the EOCD record into the buffer at its current position.
     */
    public void writeTo(ByteBuffer out) throws java.io.IOException {
        int need = getRecordLength();
        if (out.remaining() < need) {
            throw new java.io.IOException("Not enough space in output buffer: need " + need
                    + ", have " + out.remaining());
        }
        out.order(ByteOrder.LITTLE_ENDIAN);

        out.put(MAGIC);
        out.putShort((short) numberOfThisDisk);
        out.putShort((short) diskWhereCentralDirectoryStarts);
        out.putShort((short) entriesOnThisDisk);
        out.putShort((short) totalEntries);
        out.putInt((int) centralDirectorySize);
        out.putInt((int) centralDirectoryOffset);
        out.putShort((short) comment.length);
        if (comment.length > 0) {
            out.put(comment);
        }
    }
}
