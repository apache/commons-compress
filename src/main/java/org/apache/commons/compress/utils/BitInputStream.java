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
package org.apache.commons.compress.utils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;

/**
 * Reads bits from an InputStream.
 *
 * @since 1.10
 * @NotThreadSafe
 */
public class BitInputStream implements Closeable {
    private static final int MAXIMUM_CACHE_SIZE = 63; // bits in long minus sign bit
    private static final long[] MASKS = new long[MAXIMUM_CACHE_SIZE + 1];

    static {
        for (int i = 1; i <= MAXIMUM_CACHE_SIZE; i++) {
            MASKS[i] = (MASKS[i - 1] << 1) + 1;
        }
    }

    private final org.apache.commons.io.input.BoundedInputStream in;
    private final ByteOrder byteOrder;
    private long bitsCached;
    private int bitsCachedSize;

    /**
     * Constructs a {@code BitInputStream} that reads individual bits from the
     * given {@link InputStream}, interpreting them according to the specified
     * bit ordering.
     *
     * <p>The bit ordering determines how consecutive bits are packed into bytes:</p>
     *
     * <ul>
     *   <li>{@link ByteOrder#BIG_ENDIAN} (most significant bit first):
     *     <p>Bits are read from the high (bit&nbsp;7) to the low (bit&nbsp;0)
     *     position of each byte.</p>
     *
     *     <pre>{@code
     *         byte 0                     byte 1
     *   bit    7  6  5  4  3  2  1  0  |  7  6  5  4  3  2  1  0
     *         a4 a3 a2 a1 a0 b4 b3 b2    b1 b0  0  0  0  0  0  0
     *     }</pre>
     *   </li>
     *
     *   <li>{@link ByteOrder#LITTLE_ENDIAN} (least significant bit first):
     *     <p>Bits are read from the low (bit&nbsp;0) to the high (bit&nbsp;7)
     *     position of each byte.</p>
     *
     *     <pre>{@code
     *         byte 0                     byte 1
     *   bit    7  6  5  4  3  2  1  0  |  7  6  5  4  3  2  1  0
     *         b2 b1 b0 a4 a3 a2 a1 a0     0  0  0  0  0 b4 b3 b2
     *     }</pre>
     *   </li>
     * </ul>
     *
     * @param in        the underlying input stream providing the bytes
     * @param byteOrder determines whether bits are read MSB-first ({@link ByteOrder#BIG_ENDIAN})
     *                  or LSB-first ({@link ByteOrder#LITTLE_ENDIAN})
     */
    public BitInputStream(final InputStream in, final ByteOrder byteOrder) {
        this.in = org.apache.commons.io.input.BoundedInputStream.builder().setInputStream(in).asSupplier().get();
        this.byteOrder = byteOrder;
    }

    /**
     * Drops bits until the next bits will be read from a byte boundary.
     *
     * @since 1.16
     */
    public void alignWithByteBoundary() {
        final int toSkip = bitsCachedSize % Byte.SIZE;
        if (toSkip > 0) {
            readCachedBits(toSkip);
        }
    }

    /**
     * Returns an estimate of the number of bits that can be read from this input stream without blocking by the next invocation of a method for this input.
     * stream.
     *
     * @throws IOException if the underlying stream throws one when calling available.
     * @return estimate of the number of bits that can be read without blocking.
     * @since 1.16
     */
    public long bitsAvailable() throws IOException {
        return bitsCachedSize + (long) Byte.SIZE * in.available();
    }

    /**
     * Returns the number of bits that can be read from this input stream without reading from the underlying input stream at all.
     *
     * @return estimate of the number of bits that can be read without reading from the underlying stream.
     * @since 1.16
     */
    public int bitsCached() {
        return bitsCachedSize;
    }

    /**
     * Clears the cache of bits that have been read from the underlying stream but not yet provided via {@link #readBits}.
     */
    public void clearBitCache() {
        bitsCached = 0;
        bitsCachedSize = 0;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    /**
     * Fills the cache up to 56 bits.
     *
     * @param count How muchg to read.
     * @return return true at end-of-file.
     * @throws IOException if an I/O error occurs.
     */
    private boolean ensureCache(final int count) throws IOException {
        while (bitsCachedSize < count && bitsCachedSize < 57) {
            final long nextByte = in.read();
            if (nextByte < 0) {
                return true;
            }
            if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                bitsCached |= nextByte << bitsCachedSize;
            } else {
                bitsCached <<= Byte.SIZE;
                bitsCached |= nextByte;
            }
            bitsCachedSize += Byte.SIZE;
        }
        return false;
    }

    /**
     * Gets the number of bytes read from the underlying stream.
     * <p>
     * This includes the bytes read to fill the current cache and not read as bits so far.
     * </p>
     *
     * @return the number of bytes read from the underlying stream.
     * @since 1.17
     */
    public long getBytesRead() {
        return in.getCount();
    }

    private long processBitsGreater57(final int count) throws IOException {
        final long bitsOut;
        final int overflowBits;
        long overflow = 0L;
        // bitsCachedSize >= 57 and left-shifting it 8 bits would cause an overflow
        final int bitsToAddCount = count - bitsCachedSize;
        overflowBits = Byte.SIZE - bitsToAddCount;
        final long nextByte = in.read();
        if (nextByte < 0) {
            return nextByte;
        }
        if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
            final long bitsToAdd = nextByte & MASKS[bitsToAddCount];
            bitsCached |= bitsToAdd << bitsCachedSize;
            overflow = nextByte >>> bitsToAddCount & MASKS[overflowBits];
        } else {
            bitsCached <<= bitsToAddCount;
            final long bitsToAdd = nextByte >>> overflowBits & MASKS[bitsToAddCount];
            bitsCached |= bitsToAdd;
            overflow = nextByte & MASKS[overflowBits];
        }
        bitsOut = bitsCached & MASKS[count];
        bitsCached = overflow;
        bitsCachedSize = overflowBits;
        return bitsOut;
    }

    /**
     * Reads and returns the next bit read from the underlying stream.
     *
     * @return the next bit (0 or 1) or -1 if the end of the stream has been reached.
     * @throws IOException if an I/O error occurs.
     * @since 1.28
     */
    public int readBit() throws IOException {
        return (int) readBits(1);
    }

    /**
     * Reads and returns at most 63 bits read from the underlying stream.
     *
     * @param count the number of bits to read, must be a positive number not bigger than 63.
     * @return the bits concatenated as a long using the stream's byte order. -1 if the end of the underlying stream has been reached before reading the
     *         requested number of bits.
     * @throws IOException if an I/O error occurs.
     */
    public long readBits(final int count) throws IOException {
        if (count < 0 || count > MAXIMUM_CACHE_SIZE) {
            throw new IOException("Count must not be negative or greater than " + MAXIMUM_CACHE_SIZE);
        }
        if (ensureCache(count)) {
            return -1;
        }
        if (bitsCachedSize < count) {
            return processBitsGreater57(count);
        }
        return readCachedBits(count);
    }

    private long readCachedBits(final int count) {
        final long bitsOut;
        if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
            bitsOut = bitsCached & MASKS[count];
            bitsCached >>>= count;
        } else {
            bitsOut = bitsCached >> bitsCachedSize - count & MASKS[count];
        }
        bitsCachedSize -= count;
        return bitsOut;
    }

    /**
     * Returns the bit order used by this stream.
     *
     * @return the bit ordering: {@link ByteOrder#BIG_ENDIAN} (MSB-first) or {@link ByteOrder#LITTLE_ENDIAN} (LSB-first).
     * @see #BitInputStream(InputStream, ByteOrder)
     */
    public ByteOrder getByteOrder() {
        return byteOrder;
    }
}
