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
package org.apache.commons.compress.compressors.bzip2;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.compressors.CompressorException;

/**
 * MSB-first bit reader for the bzip2 decoder.
 * <p>
 * The reader keeps up to 64 bits in {@link #bitBuffer}, left aligned: the top {@link #bitCount} bits are valid. Two refill policies exist:
 * </p>
 * <ul>
 * <li>{@link #readBits(int)} and {@link #readByteOrEof()} refill <em>lazily</em>: they pull exactly the bytes needed for the request, so at the end of a bzip2
 * stream the underlying stream is positioned right after the stream's last byte (a documented guarantee of {@link BZip2CompressorInputStream} when it is not
 * decompressing concatenated streams).</li>
 * <li>{@link #fill()} refills <em>greedily</em> up to the capacity of the buffer. It may only be used while decoding a block body: after the last Huffman
 * symbol of a block the stream always continues with at least 80 bits (end-of-stream magic and combined CRC), so a lookahead of at most 64 bits never crosses
 * the end of the stream.</li>
 * </ul>
 * <p>
 * Hot loops in the decoder copy {@link #bitBuffer} and {@link #bitCount} into locals and write them back before calling {@link #fill()} or any method that
 * may throw.
 * </p>
 *
 * @NotThreadSafe
 */
final class BZip2BitReader implements Closeable {

    private final InputStream in;

    /**
     * Left-aligned bit buffer; the top {@link #bitCount} bits are valid.
     */
    long bitBuffer;

    /**
     * Number of valid bits in {@link #bitBuffer}, 0..64.
     */
    int bitCount;

    /**
     * Bytes pulled from the underlying stream, including buffered ones.
     */
    private long bytesRead;

    /**
     * Set once a request for bits could not be satisfied: from then on {@link #getBytesRead()} reports every byte pulled, as the previous implementation
     * did (it read the underlying stream to its end while trying to satisfy the request).
     */
    private boolean exhausted;

    private final byte[] scratch = new byte[8];

    BZip2BitReader(final InputStream in) {
        this.in = in;
    }

    /**
     * Discards buffered bits (used between concatenated streams, where at most 7 padding bits are buffered).
     */
    void clear() {
        bitBuffer = 0;
        bitCount = 0;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    /**
     * Greedy refill: reads until more than 56 bits are buffered or the underlying stream is exhausted. Never throws for end of input.
     *
     * @throws IOException if the underlying stream fails.
     */
    void fill() throws IOException {
        int count = bitCount;
        long buffer = bitBuffer;
        while (count <= 56) {
            final int n = in.read(scratch, 0, 64 - count >>> 3);
            if (n <= 0) {
                break;
            }
            bytesRead += n;
            for (int i = 0; i < n; i++) {
                buffer |= (long) (scratch[i] & 0xff) << 56 - count;
                count += 8;
            }
        }
        bitCount = count;
        bitBuffer = buffer;
    }

    /**
     * Lazily reads exactly the bytes needed to have {@code n} bits buffered, stopping silently at end of input.
     */
    private void fillLazy(final int n) throws IOException {
        while (bitCount < n) {
            final int b = in.read();
            if (b < 0) {
                return;
            }
            bytesRead++;
            bitBuffer |= (long) b << 56 - bitCount;
            bitCount += 8;
        }
    }

    /**
     * Gets the number of bytes of the underlying stream consumed so far, {@code ceil(bits consumed / 8)}, independent of how many bytes are buffered.
     *
     * @return bytes consumed.
     */
    long getBytesRead() {
        return exhausted ? bytesRead : bytesRead - (bitCount >>> 3);
    }

    /**
     * Reads {@code n} bits (1..32), most significant bit first.
     *
     * @param n number of bits, 1..32.
     * @return the bits as an unsigned value.
     * @throws CompressorException if the stream ends before {@code n} bits are available.
     * @throws IOException         if the underlying stream fails.
     */
    int readBits(final int n) throws IOException {
        if (bitCount < n) {
            fillLazy(n);
            if (bitCount < n) {
                exhausted = true;
                throw new CompressorException("Unexpected end of stream");
            }
        }
        final int value = (int) (bitBuffer >>> 64 - n);
        bitBuffer <<= n;
        bitCount -= n;
        return value;
    }

    /**
     * Reads {@code n} bits (1..32) of a Huffman code, refilling greedily (block bodies only).
     *
     * @param n number of bits, 1..32.
     * @return the bits as an unsigned value.
     * @throws EOFException if the stream ends before {@code n} bits are available.
     * @throws IOException  if the underlying stream fails.
     */
    int readHuffmanBits(final int n) throws IOException {
        if (bitCount < n) {
            fill();
            if (bitCount < n) {
                exhausted = true;
                throw new EOFException("Truncated Huffman bit stream");
            }
        }
        final int value = (int) (bitBuffer >>> 64 - n);
        bitBuffer <<= n;
        bitCount -= n;
        return value;
    }

    /**
     * Reads {@code n} bits (1..32) inside a block body, refilling greedily.
     *
     * @param n number of bits, 1..32.
     * @return the bits as an unsigned value.
     * @throws CompressorException if the stream ends before {@code n} bits are available.
     * @throws IOException         if the underlying stream fails.
     */
    int readBitsInBlock(final int n) throws IOException {
        if (bitCount < n) {
            fill();
            if (bitCount < n) {
                exhausted = true;
                throw new CompressorException("Unexpected end of stream");
            }
        }
        final int value = (int) (bitBuffer >>> 64 - n);
        bitBuffer <<= n;
        bitCount -= n;
        return value;
    }

    /**
     * Reads a unary code inside a block body: the number of 1 bits before the next 0 bit (which is consumed as well).
     *
     * @return the number of leading 1 bits.
     * @throws CompressorException if the stream ends before a 0 bit is found.
     * @throws IOException         if the underlying stream fails.
     */
    int readUnaryInBlock() throws IOException {
        int ones = 0;
        while (true) {
            if (bitCount < 8) {
                fill();
                if (bitCount == 0) {
                    exhausted = true;
                    throw new CompressorException("Unexpected end of stream");
                }
            }
            final int leading = Long.numberOfLeadingZeros(~bitBuffer);
            if (leading < bitCount) {
                // The 0 bit is within the valid bits: consume the ones and the terminating zero.
                bitBuffer <<= leading + 1;
                bitCount -= leading + 1;
                return ones + leading;
            }
            // Every valid bit is a 1: consume them all and continue with the next refill.
            ones += bitCount;
            bitBuffer = 0;
            bitCount = 0;
        }
    }

    /**
     * Reads one byte (8 bits at the current bit position).
     *
     * @return the byte, or -1 if the stream ends before 8 bits are available.
     * @throws IOException if the underlying stream fails.
     */
    int readByteOrEof() throws IOException {
        if (bitCount < 8) {
            fillLazy(8);
            if (bitCount < 8) {
                exhausted = true;
                return -1;
            }
        }
        final int value = (int) (bitBuffer >>> 56);
        bitBuffer <<= 8;
        bitCount -= 8;
        return value;
    }
}
