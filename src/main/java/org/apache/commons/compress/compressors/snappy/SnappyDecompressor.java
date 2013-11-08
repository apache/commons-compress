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
package org.apache.commons.compress.compressors.snappy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This class implements Snappy decompression. Snappy is a LZ77-type compressor
 * with a fixed, byte-oriented encoding created by Google(tm). It is originally
 * based on text by Zeev Tarantov. This approach works by allocating a buffer 3x
 * the compression block size. As the stream is decompressed, it is written to
 * the buffer. When the buffer becomes 2/3 full, the first third is flushed to
 * the underlying stream and the following bytes are all shifted down the array.
 * In this way, there is always at least one block-size of buffer available for
 * back-references.
 */
public class SnappyDecompressor {

    /** Mask used to determine the type of "tag" is being processed */
    private static final int TAG_MASK = 0x03;

    /** Default block size */
    public static final int BLOCK_SIZE = 32768;

    /** Buffer to write decompressed bytes to for back-references */
    private final byte[] decompressBuf;

    /** The index of the next byte in the buffer to write to */
    private int decompressBufIndex;

    /** The actual block size specified */
    protected final int blockSize;

    /** The underlying stream to read compressed data from */
    protected final InputStream in;

    /** The size of the uncompressed data */
    protected final int size;

    /**
     * Constructor
     * 
     * @param buf
     *            An array of compressed data
     * 
     * @throws IOException
     */
    public SnappyDecompressor(final byte[] buf) throws IOException {
        this(new ByteArrayInputStream(buf), BLOCK_SIZE);
    }

    /**
     * Constructor
     * 
     * @param buf
     *            An array of compressed data
     * @param blockSize
     *            The block size used in compression
     * 
     * @throws IOException
     */
    public SnappyDecompressor(final byte[] buf, int blockSize)
            throws IOException {
        this(new ByteArrayInputStream(buf), blockSize);
    }

    /**
     * Constructor
     * 
     * @param is
     *            An InputStream to read compressed data from
     * 
     * @throws IOException
     */
    public SnappyDecompressor(final InputStream is) throws IOException {
        this(is, BLOCK_SIZE);
    }

    /**
     * Constructor
     * 
     * @param is
     *            An InputStream to read compressed data from
     * @param blockSize
     *            The block size used in compression
     * 
     * @throws IOException
     */
    public SnappyDecompressor(final InputStream is, final int blockSize)
            throws IOException {

        this.in = is;
        this.blockSize = blockSize;
        this.decompressBuf = new byte[blockSize * 3];
        this.decompressBufIndex = 0;
        this.size = (int) readSize();
    }

    /**
     * Decompress the stream into an OutputStream
     * 
     * @param os
     *            The OutputStream to write the decompressed data
     * 
     * @throws IOException
     *             if an I/O error occurs. In particular, an
     *             <code>IOException</code> is thrown if the output stream is
     *             closed or the EOF is reached unexpectedly.
     */
    public void decompress(final OutputStream os) throws IOException {

        int uncompressedSizeLength = getSize();

        while (uncompressedSizeLength > 0) {
            final int b = readOneByte();
            int length = 0;
            int offset = 0;

            switch (b & TAG_MASK) {

            case 0x00:

                /*
                 * For literals up to and including 60 bytes in length, the
                 * upper six bits of the tag byte contain (len-1). The literal
                 * follows immediately thereafter in the bytestream. - For
                 * longer literals, the (len-1) value is stored after the tag
                 * byte, little-endian. The upper six bits of the tag byte
                 * describe how many bytes are used for the length; 60, 61, 62
                 * or 63 for 1-4 bytes, respectively. The literal itself follows
                 * after the length.
                 */

                switch (b >> 2) {
                case 60:
                    length = readOneByte();
                    break;
                case 61:
                    length = readOneByte();
                    length |= (readOneByte() << 8);
                    break;
                case 62:
                    length = readOneByte();
                    length |= (readOneByte() << 8);
                    length |= (readOneByte() << 16);
                    break;
                case 63:
                    length = readOneByte();
                    length |= (readOneByte() << 8);
                    length |= (readOneByte() << 16);
                    length |= (readOneByte() << 24);
                    break;
                default:
                    length = b >> 2;
                    break;
                }

                length += 1;

                if (expandLiteral(length)) {
                    flushDecompressBuffer(os);
                }
                break;

            case 0x01:

                /*
                 * These elements can encode lengths between [4..11] bytes and
                 * offsets between [0..2047] bytes. (len-4) occupies three bits
                 * and is stored in bits [2..4] of the tag byte. The offset
                 * occupies 11 bits, of which the upper three are stored in the
                 * upper three bits ([5..7]) of the tag byte, and the lower
                 * eight are stored in a byte following the tag byte.
                 */

                length = 4 + ((b >> 2) & 0x07);
                offset = (b & 0xE0) << 3;
                offset |= readOneByte();

                if (expandCopy(offset, length)) {
                    flushDecompressBuffer(os);
                }
                break;

            case 0x02:

                /*
                 * These elements can encode lengths between [1..64] and offsets
                 * from [0..65535]. (len-1) occupies six bits and is stored in
                 * the upper six bits ([2..7]) of the tag byte. The offset is
                 * stored as a little-endian 16-bit integer in the two bytes
                 * following the tag byte.
                 */

                length = (b >> 2) + 1;

                offset = readOneByte();
                offset |= readOneByte() << 8;

                if (expandCopy(offset, length)) {
                    flushDecompressBuffer(os);
                }
                break;

            case 0x03:

                /*
                 * These are like the copies with 2-byte offsets (see previous
                 * subsection), except that the offset is stored as a 32-bit
                 * integer instead of a 16-bit integer (and thus will occupy
                 * four bytes).
                 */

                length = (b >> 2) + 1;

                offset = readOneByte();
                offset |= readOneByte() << 8;
                offset |= readOneByte() << 16;
                offset |= readOneByte() << 24;

                if (expandCopy(offset, length)) {
                    flushDecompressBuffer(os);
                }
                break;
            }

            uncompressedSizeLength -= length;
        }
        os.write(decompressBuf, 0, decompressBufIndex);
    }

    /**
     * Flush the first block of the decompression buffer to the underlying out
     * stream. All subsequent bytes are moved down to the beginning of the
     * buffer.
     * 
     * @param os
     *            The output stream to write to
     * 
     * @throws IOException
     *             if an I/O error occurs. In particular, an
     *             <code>IOException</code> is thrown if the output stream is
     *             closed.
     */
    private void flushDecompressBuffer(final OutputStream os)
            throws IOException {
        os.write(decompressBuf, 0, this.blockSize);
        System.arraycopy(decompressBuf, BLOCK_SIZE, decompressBuf, 0,
                this.blockSize);
        decompressBufIndex -= this.blockSize;
    }

    /**
     * Literals are uncompressed data stored directly in the byte stream.
     * 
     * @param length
     *            The number of bytes to read from the underlying stream
     * 
     * @throws IOException
     *             If the first byte cannot be read for any reason other than
     *             end of file, or if the input stream has been closed, or if
     *             some other I/O error occurs.
     * 
     * @return True if the decompressed data should be flushed
     */
    private boolean expandLiteral(final int length) throws IOException {
        if (length != this.in.read(decompressBuf, decompressBufIndex, length)) {
            throw new IOException("Premature end of stream");
        }

        decompressBufIndex += length;
        return (decompressBufIndex >= (2 * this.blockSize));
    }

    /**
     * Copies are references back into previous decompressed data, telling the
     * decompressor to reuse data it has previously decoded. They encode two
     * values: The offset, saying how many bytes back from the current position
     * to read, and the length, how many bytes to copy. Offsets of zero can be
     * encoded, but are not legal; similarly, it is possible to encode
     * backreferences that would go past the end of the block (offset > current
     * decompressed position), which is also nonsensical and thus not allowed.
     * 
     * @param offset
     *            The offset from the backward from the end of expanded stream
     * @param length
     *            The number of bytes to copy
     * 
     * @throws IOException
     *             An the offset expands past the front of the decompression
     *             buffer
     * @return True if the decompressed data should be flushed
     */
    private boolean expandCopy(final int offset, int length) throws IOException {

        if (offset > blockSize) {
            throw new IOException("Offset is larger than block size");
        }

        if (offset == 1) {
            byte lastChar = decompressBuf[decompressBufIndex - 1];
            for (int i = 0; i < length; i++) {
                decompressBuf[decompressBufIndex++] = lastChar;
            }
        } else if (length < offset) {
            System.arraycopy(decompressBuf, decompressBufIndex - offset,
                    decompressBuf, decompressBufIndex, length);
            decompressBufIndex += length;
        } else {
            int fullRotations = length / offset;
            int pad = length - (offset * fullRotations);

            while (fullRotations-- != 0) {
                System.arraycopy(decompressBuf, decompressBufIndex - offset,
                        decompressBuf, decompressBufIndex, offset);
                decompressBufIndex += offset;
            }

            if (pad > 0) {
                System.arraycopy(decompressBuf, decompressBufIndex - offset,
                        decompressBuf, decompressBufIndex, pad);

                decompressBufIndex += pad;
            }
        }

        return (decompressBufIndex >= (2 * this.blockSize));
    }

    /**
     * This helper method reads the next byte of data from the input stream. The
     * value byte is returned as an <code>int</code> in the range <code>0</code>
     * to <code>255</code>. If no byte is available because the end of the
     * stream has been reached, an Exception is thrown.
     * 
     * @return The next byte of data
     * @throws IOException
     *             EOF is reached or error reading the stream
     */
    private int readOneByte() throws IOException {
        int b = in.read();
        if (b == -1) {
            throw new IOException("Premature end of stream");
        }
        return b & 0xFF;
    }

    /**
     * The stream starts with the uncompressed length (up to a maximum of 2^32 -
     * 1), stored as a little-endian varint. Varints consist of a series of
     * bytes, where the lower 7 bits are data and the upper bit is set iff there
     * are more bytes to be read. In other words, an uncompressed length of 64
     * would be stored as 0x40, and an uncompressed length of 2097150 (0x1FFFFE)
     * would be stored as 0xFE 0xFF 0x7F.
     * 
     * @return The size of the uncompressed data
     * 
     * @throws IOException
     *             Could not read a byte
     */
    private long readSize() throws IOException {
        int index = 0;
        long sz = 0;
        int b = 0;

        do {
            b = readOneByte();
            sz |= (b & 0x7f) << (index++ * 7);
        } while (0 != (b & 0x80));
        return sz;
    }

    /**
     * Get the uncompressed size of the stream
     * 
     * @return the uncompressed size
     */
    public int getSize() {
        return size;
    }

}
