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
package org.apache.commons.compress.archivers.cpio;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;

import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipEncoding;
import org.apache.commons.compress.archivers.zip.ZipEncodingHelper;

/**
 * CpioArchiveOutputStream is a stream for writing CPIO streams. All formats of CPIO are supported (old ASCII, old binary, new portable format and the new
 * portable format with CRC).
 *
 * <p>
 * An entry can be written by creating an instance of CpioArchiveEntry and fill it with the necessary values and put it into the CPIO stream. Afterwards write
 * the contents of the file into the CPIO stream. Either close the stream by calling finish() or put a next entry into the cpio stream.
 * </p>
 *
 * <pre>
 * CpioArchiveOutputStream out = new CpioArchiveOutputStream(
 *         new FileOutputStream(new File("test.cpio")));
 * CpioArchiveEntry entry = new CpioArchiveEntry();
 * entry.setName("testfile");
 * String contents = &quot;12345&quot;;
 * entry.setFileSize(contents.length());
 * entry.setMode(CpioConstants.C_ISREG); // regular file
 * ... set other attributes, for example time, number of links
 * out.putArchiveEntry(entry);
 * out.write(testContents.getBytes());
 * out.close();
 * </pre>
 *
 * <p>
 * Note: This implementation should be compatible to cpio 2.5
 * </p>
 *
 * <p>
 * This class uses mutable fields and is not considered threadsafe.
 * </p>
 *
 * <p>
 * based on code from the jRPM project (jrpm.sourceforge.net)
 * </p>
 */
public class CpioArchiveOutputStream extends ArchiveOutputStream<CpioArchiveEntry> implements CpioConstants {

    /**
     * The NUL character.
     */
    private static final char NUL = '\0';

    private CpioArchiveEntry entry;

    /**
     * See {@link CpioArchiveEntry#CpioArchiveEntry(short)} for possible values.
     */
    private final short entryFormat;

    private final HashMap<String, CpioArchiveEntry> names = new HashMap<>();

    private long crc;

    private long written;

    private final int blockSize;

    private long nextArtificalDeviceAndInode = 1;

    /**
     * The encoding to use for file names and labels.
     */
    private final ZipEncoding zipEncoding;

    // the provided encoding (for unit tests)
    final String charsetName;

    /**
     * Constructs the cpio output stream. The format for this CPIO stream is the "new" format using ASCII encoding for file names
     *
     * @param out The cpio stream
     */
    public CpioArchiveOutputStream(final OutputStream out) {
        this(out, FORMAT_NEW);
    }

    /**
     * Constructs the cpio output stream with a specified format, a blocksize of {@link CpioConstants#BLOCK_SIZE BLOCK_SIZE} and using ASCII as the file name
     * encoding.
     *
     * @param out    The cpio stream
     * @param format The format of the stream
     */
    public CpioArchiveOutputStream(final OutputStream out, final short format) {
        this(out, format, BLOCK_SIZE, CpioUtil.DEFAULT_CHARSET_NAME);
    }

    /**
     * Constructs the cpio output stream with a specified format using ASCII as the file name encoding.
     *
     * @param out       The cpio stream
     * @param format    The format of the stream
     * @param blockSize The block size of the archive.
     * @since 1.1
     */
    public CpioArchiveOutputStream(final OutputStream out, final short format, final int blockSize) {
        this(out, format, blockSize, CpioUtil.DEFAULT_CHARSET_NAME);
    }

    /**
     * Constructs the cpio output stream with a specified format using ASCII as the file name encoding.
     *
     * @param out       The cpio stream
     * @param format    The format of the stream
     * @param blockSize The block size of the archive.
     * @param encoding  The encoding of file names to write - use null for the platform's default.
     * @since 1.6
     */
    public CpioArchiveOutputStream(final OutputStream out, final short format, final int blockSize, final String encoding) {
        super(out);
        switch (format) {
        case FORMAT_NEW:
        case FORMAT_NEW_CRC:
        case FORMAT_OLD_ASCII:
        case FORMAT_OLD_BINARY:
            break;
        default:
            throw new IllegalArgumentException("Unknown format: " + format);

        }
        this.entryFormat = format;
        this.blockSize = blockSize;
        this.charsetName = encoding;
        this.zipEncoding = ZipEncodingHelper.getZipEncoding(encoding);
    }

    /**
     * Constructs the cpio output stream. The format for this CPIO stream is the "new" format.
     *
     * @param out      The cpio stream
     * @param encoding The encoding of file names to write - use null for the platform's default.
     * @since 1.6
     */
    public CpioArchiveOutputStream(final OutputStream out, final String encoding) {
        this(out, FORMAT_NEW, BLOCK_SIZE, encoding);
    }

    /**
     * Closes the CPIO output stream as well as the stream being filtered.
     *
     * @throws IOException if an I/O error has occurred or if a CPIO file error has occurred
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

    /*
     * (non-Javadoc)
     *
     * @see org.apache.commons.compress.archivers.ArchiveOutputStream#closeArchiveEntry ()
     */
    @Override
    public void closeArchiveEntry() throws IOException {
        checkFinished();
        checkOpen();
        if (entry == null) {
            throw new IOException("Trying to close non-existent entry");
        }

        if (this.entry.getSize() != this.written) {
            throw new IOException("Invalid entry size (expected " + this.entry.getSize() + " but got " + this.written + " bytes)");
        }
        pad(this.entry.getDataPadCount());
        if (this.entry.getFormat() == FORMAT_NEW_CRC && this.crc != this.entry.getChksum()) {
            throw new IOException("CRC Error");
        }
        this.entry = null;
        this.crc = 0;
        this.written = 0;
    }

    /**
     * Creates a new CpioArchiveEntry. The entryName must be an ASCII encoded string.
     *
     * @see org.apache.commons.compress.archivers.ArchiveOutputStream#createArchiveEntry(java.io.File, String)
     */
    @Override
    public CpioArchiveEntry createArchiveEntry(final File inputFile, final String entryName) throws IOException {
        checkFinished();
        return new CpioArchiveEntry(inputFile, entryName);
    }

    /**
     * Creates a new CpioArchiveEntry. The entryName must be an ASCII encoded string.
     *
     * @see org.apache.commons.compress.archivers.ArchiveOutputStream#createArchiveEntry(java.io.File, String)
     */
    @Override
    public CpioArchiveEntry createArchiveEntry(final Path inputPath, final String entryName, final LinkOption... options) throws IOException {
        checkFinished();
        return new CpioArchiveEntry(inputPath, entryName, options);
    }

    /**
     * Encodes the given string using the configured encoding.
     *
     * @param str the String to write
     * @throws IOException if the string couldn't be written
     * @return result of encoding the string
     */
    private byte[] encode(final String str) throws IOException {
        final ByteBuffer buf = zipEncoding.encode(str);
        final int len = buf.limit() - buf.position();
        return Arrays.copyOfRange(buf.array(), buf.arrayOffset(), buf.arrayOffset() + len);
    }

    /**
     * Finishes writing the contents of the CPIO output stream without closing the underlying stream. Use this method when applying multiple filters in
     * succession to the same output stream.
     *
     * @throws IOException if an I/O exception has occurred or if a CPIO file error has occurred
     */
    @Override
    public void finish() throws IOException {
        checkOpen();
        checkFinished();

        if (this.entry != null) {
            throw new IOException("This archive contains unclosed entries.");
        }
        this.entry = new CpioArchiveEntry(this.entryFormat);
        this.entry.setName(CPIO_TRAILER);
        this.entry.setNumberOfLinks(1);
        writeHeader(this.entry);
        closeArchiveEntry();

        final int lengthOfLastBlock = (int) (getBytesWritten() % blockSize);
        if (lengthOfLastBlock != 0) {
            pad(blockSize - lengthOfLastBlock);
        }
        super.finish();
    }

    private void pad(final int count) throws IOException {
        if (count > 0) {
            out.write(new byte[count]);
            count(count);
        }
    }

    /**
     * Begins writing a new CPIO file entry and positions the stream to the start of the entry data. Closes the current entry if still active. The current time
     * will be used if the entry has no set modification time and the default header format will be used if no other format is specified in the entry.
     *
     * @param entry the CPIO cpioEntry to be written
     * @throws IOException        if an I/O error has occurred or if a CPIO file error has occurred
     * @throws ClassCastException if entry is not an instance of CpioArchiveEntry
     */
    @Override
    public void putArchiveEntry(final CpioArchiveEntry entry) throws IOException {
        checkFinished();
        checkOpen();
        if (this.entry != null) {
            closeArchiveEntry(); // close previous entry
        }
        if (entry.getTime() == -1) {
            entry.setTime(System.currentTimeMillis() / 1000);
        }

        final short format = entry.getFormat();
        if (format != this.entryFormat) {
            throw new IOException("Header format: " + format + " does not match existing format: " + this.entryFormat);
        }

        if (this.names.put(entry.getName(), entry) != null) {
            throw new IOException("Duplicate entry: " + entry.getName());
        }

        writeHeader(entry);
        this.entry = entry;
        this.written = 0;
    }

    /**
     * Writes an array of bytes to the current CPIO entry data. This method will block until all the bytes are written.
     *
     * @param b   the data to be written
     * @param off the start offset in the data
     * @param len the number of bytes that are written
     * @throws IOException if an I/O error has occurred or if a CPIO file error has occurred
     */
    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        checkOpen();
        if (off < 0 || len < 0 || off > b.length - len) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return;
        }

        if (this.entry == null) {
            throw new IOException("No current CPIO entry");
        }
        if (this.written + len > this.entry.getSize()) {
            throw new IOException("Attempt to write past end of STORED entry");
        }
        out.write(b, off, len);
        this.written += len;
        if (this.entry.getFormat() == FORMAT_NEW_CRC) {
            for (int pos = 0; pos < len; pos++) {
                this.crc += b[pos] & 0xFF;
                this.crc &= 0xFFFFFFFFL;
            }
        }
        count(len);
    }

    private void writeAsciiLong(final long number, final int length, final int radix) throws IOException {
        final StringBuilder tmp = new StringBuilder();
        final String tmpStr;
        if (radix == 16) {
            tmp.append(Long.toHexString(number));
        } else if (radix == 8) {
            tmp.append(Long.toOctalString(number));
        } else {
            tmp.append(number);
        }

        if (tmp.length() <= length) {
            final int insertLength = length - tmp.length();
            for (int pos = 0; pos < insertLength; pos++) {
                tmp.insert(0, "0");
            }
            tmpStr = tmp.toString();
        } else {
            tmpStr = tmp.substring(tmp.length() - length);
        }
        final byte[] b = writeUsAsciiRaw(tmpStr);
        count(b.length);
    }

    private void writeBinaryLong(final long number, final int length, final boolean swapHalfWord) throws IOException {
        final byte[] tmp = CpioUtil.long2byteArray(number, length, swapHalfWord);
        out.write(tmp);
        count(tmp.length);
    }

    /**
     * Writes an encoded string to the stream followed by \0
     *
     * @param str the String to write
     * @throws IOException if the string couldn't be written
     */
    private void writeCString(final byte[] str) throws IOException {
        out.write(str);
        out.write(NUL);
        count(str.length + 1);
    }

    private void writeHeader(final CpioArchiveEntry e) throws IOException {
        switch (e.getFormat()) {
        case FORMAT_NEW:
            writeUsAsciiRaw(MAGIC_NEW);
            count(6);
            writeNewEntry(e);
            break;
        case FORMAT_NEW_CRC:
            writeUsAsciiRaw(MAGIC_NEW_CRC);
            count(6);
            writeNewEntry(e);
            break;
        case FORMAT_OLD_ASCII:
            writeUsAsciiRaw(MAGIC_OLD_ASCII);
            count(6);
            writeOldAsciiEntry(e);
            break;
        case FORMAT_OLD_BINARY:
            final boolean swapHalfWord = true;
            writeBinaryLong(MAGIC_OLD_BINARY, 2, swapHalfWord);
            writeOldBinaryEntry(e, swapHalfWord);
            break;
        default:
            throw new IOException("Unknown format " + e.getFormat());
        }
    }

    private void writeNewEntry(final CpioArchiveEntry entry) throws IOException {
        long inode = entry.getInode();
        long devMin = entry.getDeviceMin();
        if (CPIO_TRAILER.equals(entry.getName())) {
            inode = devMin = 0;
        } else if (inode == 0 && devMin == 0) {
            inode = nextArtificalDeviceAndInode & 0xFFFFFFFF;
            devMin = nextArtificalDeviceAndInode++ >> 32 & 0xFFFFFFFF;
        } else {
            nextArtificalDeviceAndInode = Math.max(nextArtificalDeviceAndInode, inode + 0x100000000L * devMin) + 1;
        }

        writeAsciiLong(inode, 8, 16);
        writeAsciiLong(entry.getMode(), 8, 16);
        writeAsciiLong(entry.getUID(), 8, 16);
        writeAsciiLong(entry.getGID(), 8, 16);
        writeAsciiLong(entry.getNumberOfLinks(), 8, 16);
        writeAsciiLong(entry.getTime(), 8, 16);
        writeAsciiLong(entry.getSize(), 8, 16);
        writeAsciiLong(entry.getDeviceMaj(), 8, 16);
        writeAsciiLong(devMin, 8, 16);
        writeAsciiLong(entry.getRemoteDeviceMaj(), 8, 16);
        writeAsciiLong(entry.getRemoteDeviceMin(), 8, 16);
        final byte[] name = encode(entry.getName());
        writeAsciiLong(name.length + 1L, 8, 16);
        writeAsciiLong(entry.getChksum(), 8, 16);
        writeCString(name);
        pad(entry.getHeaderPadCount(name.length));
    }

    private void writeOldAsciiEntry(final CpioArchiveEntry entry) throws IOException {
        long inode = entry.getInode();
        long device = entry.getDevice();
        if (CPIO_TRAILER.equals(entry.getName())) {
            inode = device = 0;
        } else if (inode == 0 && device == 0) {
            inode = nextArtificalDeviceAndInode & 0777777;
            device = nextArtificalDeviceAndInode++ >> 18 & 0777777;
        } else {
            nextArtificalDeviceAndInode = Math.max(nextArtificalDeviceAndInode, inode + 01000000 * device) + 1;
        }

        writeAsciiLong(device, 6, 8);
        writeAsciiLong(inode, 6, 8);
        writeAsciiLong(entry.getMode(), 6, 8);
        writeAsciiLong(entry.getUID(), 6, 8);
        writeAsciiLong(entry.getGID(), 6, 8);
        writeAsciiLong(entry.getNumberOfLinks(), 6, 8);
        writeAsciiLong(entry.getRemoteDevice(), 6, 8);
        writeAsciiLong(entry.getTime(), 11, 8);
        final byte[] name = encode(entry.getName());
        writeAsciiLong(name.length + 1L, 6, 8);
        writeAsciiLong(entry.getSize(), 11, 8);
        writeCString(name);
    }

    private void writeOldBinaryEntry(final CpioArchiveEntry entry, final boolean swapHalfWord) throws IOException {
        long inode = entry.getInode();
        long device = entry.getDevice();
        if (CPIO_TRAILER.equals(entry.getName())) {
            inode = device = 0;
        } else if (inode == 0 && device == 0) {
            inode = nextArtificalDeviceAndInode & 0xFFFF;
            device = nextArtificalDeviceAndInode++ >> 16 & 0xFFFF;
        } else {
            nextArtificalDeviceAndInode = Math.max(nextArtificalDeviceAndInode, inode + 0x10000 * device) + 1;
        }

        writeBinaryLong(device, 2, swapHalfWord);
        writeBinaryLong(inode, 2, swapHalfWord);
        writeBinaryLong(entry.getMode(), 2, swapHalfWord);
        writeBinaryLong(entry.getUID(), 2, swapHalfWord);
        writeBinaryLong(entry.getGID(), 2, swapHalfWord);
        writeBinaryLong(entry.getNumberOfLinks(), 2, swapHalfWord);
        writeBinaryLong(entry.getRemoteDevice(), 2, swapHalfWord);
        writeBinaryLong(entry.getTime(), 4, swapHalfWord);
        final byte[] name = encode(entry.getName());
        writeBinaryLong(name.length + 1L, 2, swapHalfWord);
        writeBinaryLong(entry.getSize(), 4, swapHalfWord);
        writeCString(name);
        pad(entry.getHeaderPadCount(name.length));
    }

}
