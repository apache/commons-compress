/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.compress.archivers.cpio;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipEncoding;
import org.apache.commons.compress.archivers.zip.ZipEncodingHelper;
import org.apache.commons.compress.utils.ArchiveUtils;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.compress.utils.ParsingUtils;

/**
 * CpioArchiveInputStream is a stream for reading cpio streams. All formats of cpio are supported (old ascii, old binary, new portable format and the new
 * portable format with crc).
 * <p>
 * The stream can be read by extracting a cpio entry (containing all information about an entry) and afterwards reading from the stream the file specified by
 * the entry.
 * </p>
 * <pre>
 * CpioArchiveInputStream cpioIn = new CpioArchiveInputStream(Files.newInputStream(Paths.get(&quot;test.cpio&quot;)));
 * CpioArchiveEntry cpioEntry;
 *
 * while ((cpioEntry = cpioIn.getNextEntry()) != null) {
 *     System.out.println(cpioEntry.getName());
 *     int tmp;
 *     StringBuilder buf = new StringBuilder();
 *     while ((tmp = cpIn.read()) != -1) {
 *         buf.append((char) tmp);
 *     }
 *     System.out.println(buf.toString());
 * }
 * cpioIn.close();
 * </pre>
 * <p>
 * Note: This implementation should be compatible to cpio 2.5
 * </p>
 * <p>
 * This class uses mutable fields and is not considered to be threadsafe.
 * </p>
 * <p>
 * Based on code from the jRPM project (jrpm.sourceforge.net)
 * </p>
 */
public class CpioArchiveInputStream extends ArchiveInputStream<CpioArchiveEntry> implements CpioConstants {

    /**
     * Checks if the signature matches one of the following magic values:
     *
     * Strings:
     *
     * "070701" - MAGIC_NEW "070702" - MAGIC_NEW_CRC "070707" - MAGIC_OLD_ASCII
     *
     * Octal Binary value:
     *
     * 070707 - MAGIC_OLD_BINARY (held as a short) = 0x71C7 or 0xC771
     *
     * @param signature data to match
     * @param length    length of data
     * @return whether the buffer seems to contain CPIO data
     */
    public static boolean matches(final byte[] signature, final int length) {
        if (length < 6) {
            return false;
        }

        // Check binary values
        if (signature[0] == 0x71 && (signature[1] & 0xFF) == 0xc7) {
            return true;
        }
        if (signature[1] == 0x71 && (signature[0] & 0xFF) == 0xc7) {
            return true;
        }

        // Check Ascii (String) values
        // 3037 3037 30nn
        if (signature[0] != 0x30) {
            return false;
        }
        if (signature[1] != 0x37) {
            return false;
        }
        if (signature[2] != 0x30) {
            return false;
        }
        if (signature[3] != 0x37) {
            return false;
        }
        if (signature[4] != 0x30) {
            return false;
        }
        // Check last byte
        if (signature[5] == 0x31) {
            return true;
        }
        if (signature[5] == 0x32) {
            return true;
        }
        if (signature[5] == 0x37) {
            return true;
        }

        return false;
    }

    private boolean closed;

    private CpioArchiveEntry entry;

    private long entryBytesRead;

    private boolean entryEOF;

    private final byte[] tmpbuf = new byte[4096];

    private long crc;

    /** Cached buffer - must only be used locally in the class (COMPRESS-172 - reduce garbage collection). */
    private final byte[] twoBytesBuf = new byte[2];

    /** Cached buffer - must only be used locally in the class (COMPRESS-172 - reduce garbage collection). */
    private final byte[] fourBytesBuf = new byte[4];

    private final byte[] sixBytesBuf = new byte[6];

    private final int blockSize;

    /**
     * The encoding to use for file names and labels.
     */
    private final ZipEncoding zipEncoding;

    /**
     * Constructs the cpio input stream with a blocksize of {@link CpioConstants#BLOCK_SIZE BLOCK_SIZE} and expecting ASCII file names.
     *
     * @param in The cpio stream
     */
    public CpioArchiveInputStream(final InputStream in) {
        this(in, BLOCK_SIZE, CpioUtil.DEFAULT_CHARSET_NAME);
    }

    /**
     * Constructs the cpio input stream with a blocksize of {@link CpioConstants#BLOCK_SIZE BLOCK_SIZE} expecting ASCII file names.
     *
     * @param in        The cpio stream
     * @param blockSize The block size of the archive.
     * @since 1.5
     */
    public CpioArchiveInputStream(final InputStream in, final int blockSize) {
        this(in, blockSize, CpioUtil.DEFAULT_CHARSET_NAME);
    }

    /**
     * Constructs the cpio input stream with a blocksize of {@link CpioConstants#BLOCK_SIZE BLOCK_SIZE}.
     *
     * @param in        The cpio stream
     * @param blockSize The block size of the archive.
     * @param encoding  The encoding of file names to expect - use null for the platform's default.
     * @throws IllegalArgumentException if {@code blockSize} is not bigger than 0
     * @since 1.6
     */
    public CpioArchiveInputStream(final InputStream in, final int blockSize, final String encoding) {
        super(in, encoding);
        this.in = in;
        if (blockSize <= 0) {
            throw new IllegalArgumentException("blockSize must be bigger than 0");
        }
        this.blockSize = blockSize;
        this.zipEncoding = ZipEncodingHelper.getZipEncoding(encoding);
    }

    /**
     * Constructs the cpio input stream with a blocksize of {@link CpioConstants#BLOCK_SIZE BLOCK_SIZE}.
     *
     * @param in       The cpio stream
     * @param encoding The encoding of file names to expect - use null for the platform's default.
     * @since 1.6
     */
    public CpioArchiveInputStream(final InputStream in, final String encoding) {
        this(in, BLOCK_SIZE, encoding);
    }

    /**
     * Returns 0 after EOF has reached for the current entry data, otherwise always return 1.
     * <p>
     * Programs should not count on this method to return the actual number of bytes that could be read without blocking.
     * </p>
     *
     * @return 1 before EOF and 0 after EOF has reached for current entry.
     * @throws IOException if an I/O error has occurred or if a CPIO file error has occurred
     */
    @Override
    public int available() throws IOException {
        ensureOpen();
        if (this.entryEOF) {
            return 0;
        }
        return 1;
    }

    /**
     * Closes the CPIO input stream.
     *
     * @throws IOException if an I/O error has occurred
     */
    @Override
    public void close() throws IOException {
        if (!this.closed) {
            in.close();
            this.closed = true;
        }
    }

    /**
     * Closes the current CPIO entry and positions the stream for reading the next entry.
     *
     * @throws IOException if an I/O error has occurred or if a CPIO file error has occurred
     */
    private void closeEntry() throws IOException {
        // the skip implementation of this class will not skip more
        // than Integer.MAX_VALUE bytes
        while (skip((long) Integer.MAX_VALUE) == Integer.MAX_VALUE) { // NOPMD NOSONAR
            // do nothing
        }
    }

    /**
     * Check to make sure that this stream has not been closed
     *
     * @throws IOException if the stream is already closed
     */
    private void ensureOpen() throws IOException {
        if (this.closed) {
            throw new IOException("Stream closed");
        }
    }

    /**
     * Reads the next CPIO file entry and positions stream at the beginning of the entry data.
     *
     * @return the CpioArchiveEntry just read
     * @throws IOException if an I/O error has occurred or if a CPIO file error has occurred
     * @deprecated Use {@link #getNextEntry()}.
     */
    @Deprecated
    public CpioArchiveEntry getNextCPIOEntry() throws IOException {
        ensureOpen();
        if (this.entry != null) {
            closeEntry();
        }
        readFully(twoBytesBuf, 0, twoBytesBuf.length);
        if (CpioUtil.byteArray2long(twoBytesBuf, false) == MAGIC_OLD_BINARY) {
            this.entry = readOldBinaryEntry(false);
        } else if (CpioUtil.byteArray2long(twoBytesBuf, true) == MAGIC_OLD_BINARY) {
            this.entry = readOldBinaryEntry(true);
        } else {
            System.arraycopy(twoBytesBuf, 0, sixBytesBuf, 0, twoBytesBuf.length);
            readFully(sixBytesBuf, twoBytesBuf.length, fourBytesBuf.length);
            final String magicString = ArchiveUtils.toAsciiString(sixBytesBuf);
            switch (magicString) {
            case MAGIC_NEW:
                this.entry = readNewEntry(false);
                break;
            case MAGIC_NEW_CRC:
                this.entry = readNewEntry(true);
                break;
            case MAGIC_OLD_ASCII:
                this.entry = readOldAsciiEntry();
                break;
            default:
                throw new IOException("Unknown magic [" + magicString + "]. Occurred at byte: " + getBytesRead());
            }
        }

        this.entryBytesRead = 0;
        this.entryEOF = false;
        this.crc = 0;

        if (this.entry.getName().equals(CPIO_TRAILER)) {
            this.entryEOF = true;
            skipRemainderOfLastBlock();
            return null;
        }
        return this.entry;
    }

    @Override
    public CpioArchiveEntry getNextEntry() throws IOException {
        return getNextCPIOEntry();
    }

    /**
     * Reads from the current CPIO entry into an array of bytes. Blocks until some input is available.
     *
     * @param b   the buffer into which the data is read
     * @param off the start offset of the data
     * @param len the maximum number of bytes read
     * @return the actual number of bytes read, or -1 if the end of the entry is reached
     * @throws IOException if an I/O error has occurred or if a CPIO file error has occurred
     */
    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        ensureOpen();
        if (off < 0 || len < 0 || off > b.length - len) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return 0;
        }

        if (this.entry == null || this.entryEOF) {
            return -1;
        }
        if (this.entryBytesRead == this.entry.getSize()) {
            skip(entry.getDataPadCount());
            this.entryEOF = true;
            if (this.entry.getFormat() == FORMAT_NEW_CRC && this.crc != this.entry.getChksum()) {
                throw new IOException("CRC Error. Occurred at byte: " + getBytesRead());
            }
            return -1; // EOF for this entry
        }
        final int tmplength = (int) Math.min(len, this.entry.getSize() - this.entryBytesRead);
        if (tmplength < 0) {
            return -1;
        }

        final int tmpread = readFully(b, off, tmplength);
        if (this.entry.getFormat() == FORMAT_NEW_CRC) {
            for (int pos = 0; pos < tmpread; pos++) {
                this.crc += b[pos] & 0xFF;
                this.crc &= 0xFFFFFFFFL;
            }
        }
        if (tmpread > 0) {
            this.entryBytesRead += tmpread;
        }

        return tmpread;
    }

    private long readAsciiLong(final int length, final int radix) throws IOException {
        final byte[] tmpBuffer = readRange(length);
        return ParsingUtils.parseLongValue(ArchiveUtils.toAsciiString(tmpBuffer), radix);
    }

    private long readBinaryLong(final int length, final boolean swapHalfWord) throws IOException {
        final byte[] tmp = readRange(length);
        return CpioUtil.byteArray2long(tmp, swapHalfWord);
    }

    private String readCString(final int length) throws IOException {
        // don't include trailing NUL in file name to decode
        final byte[] tmpBuffer = readRange(length - 1);
        if (this.in.read() == -1) {
            throw new EOFException();
        }
        return zipEncoding.decode(tmpBuffer);
    }

    private int readFully(final byte[] b, final int off, final int len) throws IOException {
        final int count = IOUtils.readFully(in, b, off, len);
        count(count);
        if (count < len) {
            throw new EOFException();
        }
        return count;
    }

    private CpioArchiveEntry readNewEntry(final boolean hasCrc) throws IOException {
        final CpioArchiveEntry ret;
        if (hasCrc) {
            ret = new CpioArchiveEntry(FORMAT_NEW_CRC);
        } else {
            ret = new CpioArchiveEntry(FORMAT_NEW);
        }

        ret.setInode(readAsciiLong(8, 16));
        final long mode = readAsciiLong(8, 16);
        if (CpioUtil.fileType(mode) != 0) { // mode is initialized to 0
            ret.setMode(mode);
        }
        ret.setUID(readAsciiLong(8, 16));
        ret.setGID(readAsciiLong(8, 16));
        ret.setNumberOfLinks(readAsciiLong(8, 16));
        ret.setTime(readAsciiLong(8, 16));
        ret.setSize(readAsciiLong(8, 16));
        if (ret.getSize() < 0) {
            throw new IOException("Found illegal entry with negative length");
        }
        ret.setDeviceMaj(readAsciiLong(8, 16));
        ret.setDeviceMin(readAsciiLong(8, 16));
        ret.setRemoteDeviceMaj(readAsciiLong(8, 16));
        ret.setRemoteDeviceMin(readAsciiLong(8, 16));
        final long namesize = readAsciiLong(8, 16);
        if (namesize < 0) {
            throw new IOException("Found illegal entry with negative name length");
        }
        ret.setChksum(readAsciiLong(8, 16));
        final String name = readCString((int) namesize);
        ret.setName(name);
        if (CpioUtil.fileType(mode) == 0 && !name.equals(CPIO_TRAILER)) {
            throw new IOException(
                    "Mode 0 only allowed in the trailer. Found entry name: " + ArchiveUtils.sanitize(name) + " Occurred at byte: " + getBytesRead());
        }
        skip(ret.getHeaderPadCount(namesize - 1));

        return ret;
    }

    private CpioArchiveEntry readOldAsciiEntry() throws IOException {
        final CpioArchiveEntry ret = new CpioArchiveEntry(FORMAT_OLD_ASCII);

        ret.setDevice(readAsciiLong(6, 8));
        ret.setInode(readAsciiLong(6, 8));
        final long mode = readAsciiLong(6, 8);
        if (CpioUtil.fileType(mode) != 0) {
            ret.setMode(mode);
        }
        ret.setUID(readAsciiLong(6, 8));
        ret.setGID(readAsciiLong(6, 8));
        ret.setNumberOfLinks(readAsciiLong(6, 8));
        ret.setRemoteDevice(readAsciiLong(6, 8));
        ret.setTime(readAsciiLong(11, 8));
        final long namesize = readAsciiLong(6, 8);
        if (namesize < 0) {
            throw new IOException("Found illegal entry with negative name length");
        }
        ret.setSize(readAsciiLong(11, 8));
        if (ret.getSize() < 0) {
            throw new IOException("Found illegal entry with negative length");
        }
        final String name = readCString((int) namesize);
        ret.setName(name);
        if (CpioUtil.fileType(mode) == 0 && !name.equals(CPIO_TRAILER)) {
            throw new IOException("Mode 0 only allowed in the trailer. Found entry: " + ArchiveUtils.sanitize(name) + " Occurred at byte: " + getBytesRead());
        }

        return ret;
    }

    private CpioArchiveEntry readOldBinaryEntry(final boolean swapHalfWord) throws IOException {
        final CpioArchiveEntry ret = new CpioArchiveEntry(FORMAT_OLD_BINARY);

        ret.setDevice(readBinaryLong(2, swapHalfWord));
        ret.setInode(readBinaryLong(2, swapHalfWord));
        final long mode = readBinaryLong(2, swapHalfWord);
        if (CpioUtil.fileType(mode) != 0) {
            ret.setMode(mode);
        }
        ret.setUID(readBinaryLong(2, swapHalfWord));
        ret.setGID(readBinaryLong(2, swapHalfWord));
        ret.setNumberOfLinks(readBinaryLong(2, swapHalfWord));
        ret.setRemoteDevice(readBinaryLong(2, swapHalfWord));
        ret.setTime(readBinaryLong(4, swapHalfWord));
        final long namesize = readBinaryLong(2, swapHalfWord);
        if (namesize < 0) {
            throw new IOException("Found illegal entry with negative name length");
        }
        ret.setSize(readBinaryLong(4, swapHalfWord));
        if (ret.getSize() < 0) {
            throw new IOException("Found illegal entry with negative length");
        }
        final String name = readCString((int) namesize);
        ret.setName(name);
        if (CpioUtil.fileType(mode) == 0 && !name.equals(CPIO_TRAILER)) {
            throw new IOException("Mode 0 only allowed in the trailer. Found entry: " + ArchiveUtils.sanitize(name) + "Occurred at byte: " + getBytesRead());
        }
        skip(ret.getHeaderPadCount(namesize - 1));

        return ret;
    }

    private byte[] readRange(final int len) throws IOException {
        final byte[] b = IOUtils.readRange(in, len);
        count(b.length);
        if (b.length < len) {
            throw new EOFException();
        }
        return b;
    }

    private void skip(final int bytes) throws IOException {
        // bytes cannot be more than 3 bytes
        if (bytes > 0) {
            readFully(fourBytesBuf, 0, bytes);
        }
    }

    /**
     * Skips specified number of bytes in the current CPIO entry.
     *
     * @param n the number of bytes to skip
     * @return the actual number of bytes skipped
     * @throws IOException              if an I/O error has occurred
     * @throws IllegalArgumentException if n &lt; 0
     */
    @Override
    public long skip(final long n) throws IOException {
        if (n < 0) {
            throw new IllegalArgumentException("Negative skip length");
        }
        ensureOpen();
        final int max = (int) Math.min(n, Integer.MAX_VALUE);
        int total = 0;

        while (total < max) {
            int len = max - total;
            if (len > this.tmpbuf.length) {
                len = this.tmpbuf.length;
            }
            len = read(this.tmpbuf, 0, len);
            if (len == -1) {
                this.entryEOF = true;
                break;
            }
            total += len;
        }
        return total;
    }

    /**
     * Skips the padding zeros written after the TRAILER!!! entry.
     */
    private void skipRemainderOfLastBlock() throws IOException {
        final long readFromLastBlock = getBytesRead() % blockSize;
        long remainingBytes = readFromLastBlock == 0 ? 0 : blockSize - readFromLastBlock;
        while (remainingBytes > 0) {
            final long skipped = skip(blockSize - readFromLastBlock);
            if (skipped <= 0) {
                break;
            }
            remainingBytes -= skipped;
        }
    }
}
