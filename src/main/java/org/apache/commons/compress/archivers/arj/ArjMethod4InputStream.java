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

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.utils.InputStreamStatistics;

/**
 * Implements a compressor input stream for ARJ files that supports the method 4 (COMPRESSED_FASTEST) compression.
 * <p>
 * The compressed data is a bit stream in which a 14 bit length field and a 17 bit distance field are packed. Token boundaries are not byte aligned. Closely
 * follows the reference implementation of the decoder in 7-Zip's ArjHandler.cpp.
 * </p>
 */
class ArjMethod4InputStream extends CompressorInputStream implements InputStreamStatistics {

    /** Size of the sliding window in bytes. */
    private static final int WINDOW_SIZE = 1 << 15;

    /** Number of bits of the length field. */
    private static final int LENGTH_BITS = 7 + 7;

    /** Number of bits of the distance field. */
    private static final int DISTANCE_BITS = 4 + 13;

    /** Minimum match length. */
    private static final int MATCH_MIN_LEN = 3;

    /**
     * Number of zero bytes that may be read past the end of the compressed data. The encoder does not always emit the distance field of the last match
     * because the output size already marks the end of the stream. The reference implementation tolerates this by letting the bit reader supply a few zero
     * bits past the end.
     */
    private static final int MAX_PADDING_BYTES = 4;

    private final InputStream in;

    private final CircularBuffer buffer;

    /** Accumulator of the bits read from the underlying stream, most significant bit first. */
    private long bitBuffer;

    /** Number of valid bits in {@link #bitBuffer}. */
    private int bitsInBuffer;

    /** Total number of bits consumed from the underlying stream. */
    private long bitPosition;

    /** Number of zero bytes injected after the compressed data has been exhausted. */
    private int paddedBytes;

    /** Number of decompressed bytes still to produce. */
    private long remaining;

    /** Number of decompressed bytes written to the window so far. */
    private long bytesWritten;

    /**
     * Constructs a new CompressorInputStream which decompresses bytes read from the specified stream.
     *
     * @param in           The InputStream from which to read compressed data.
     * @param originalSize The size in bytes of the decompressed data.
     */
    ArjMethod4InputStream(final InputStream in, final long originalSize){
        this.in = in;
        this.buffer = new CircularBuffer(WINDOW_SIZE);
        this.remaining = originalSize;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    /**
     * Decodes the next token and fills the sliding window.
     *
     * @throws IOException if an I/O error occurs.
     */
    private void fillBuffer() throws IOException {
        if (remaining == 0) {
            return;
        }
        // 14 bit length field: the highest bit distinguishes a literal from a copy command
        final int val = peekBits(LENGTH_BITS);
        if ((val & 1 << LENGTH_BITS - 1) == 0) {
            // Literal command, the byte is the next 8 of the 9 consumed bits
            buffer.put(val >>> 5);
            skipBits(1 + 8);
            remaining--;
            bytesWritten++;
            return;
        }
        int w;
        int flag = 1 << LENGTH_BITS - 2;
        for (w = 1; w < 7; w++, flag >>= 1) {
            if ((val & flag) == 0) {
                break;
            }
        }
        final int readBits = (w != 7 ? 1 : 0) + 2 * w;
        final int mask = (1 << w) - 1;
        final int length = mask + MATCH_MIN_LEN - 1 + (val >>> LENGTH_BITS - readBits & mask);
        skipBits(readBits);
        // 17 bit distance field
        final int distanceField = peekBits(DISTANCE_BITS);
        final int distanceReadBits;
        final int distanceW;
        if ((distanceField & 1 << 16) == 0) {
            distanceW = 9;
            distanceReadBits = 10;
        } else if ((distanceField & 1 << 15) == 0) {
            distanceW = 10;
            distanceReadBits = 12;
        } else if ((distanceField & 1 << 14) == 0) {
            distanceW = 11;
            distanceReadBits = 14;
        } else if ((distanceField & 1 << 13) == 0) {
            distanceW = 12;
            distanceReadBits = 16;
        } else {
            distanceW = 13;
            distanceReadBits = 17;
        }
        final int distanceMask = (1 << distanceW) - 1;
        final int distance = (1 << distanceW) - (1 << 9) + (distanceField >>> DISTANCE_BITS - distanceReadBits & distanceMask);
        skipBits(distanceReadBits);
        if (length > remaining) {
            throw new CompressorException("Match length %d exceeds remaining output size %d", length, remaining);
        }
        if (distance + 1 > bytesWritten) {
            throw new CompressorException("Distance %d exceeds number of bytes written %d", distance, bytesWritten);
        }
        // Copy the data from the sliding window and add to the buffer
        buffer.copy(distance + 1, length);
        remaining -= length;
        bytesWritten += length;
    }

    @Override
    public long getCompressedCount() {
        return (bitPosition + 7) / 8;
    }

    /**
     * Peeks the next {@code count} bits from the underlying stream without consuming them.
     *
     * @param count the number of bits to peek.
     * @return the bits concatenated as an int with the first read bit as the most significant bit.
     * @throws IOException if an I/O error occurs.
     */
    private int peekBits(final int count) throws IOException {
        while (bitsInBuffer < count) {
            final int value = in.read();
            if (value == -1) {
                // The compressed data may end in the middle of the last token when the encoder omits
                // the distance field of the final match. The reference implementation reads a few zero
                // bits past the end; if more than a few are needed the stream is truncated or corrupt.
                if (++paddedBytes > MAX_PADDING_BYTES) {
                    throw new CompressorException("Unexpected end of stream");
                }
                bitBuffer <<= 8;
            } else {
                bitBuffer = bitBuffer << 8 | value;
            }
            bitsInBuffer += 8;
        }
        return (int) (bitBuffer >>> bitsInBuffer - count);
    }

    @Override
    public int read() throws IOException {
        if (!buffer.available()) {
            // Nothing in the buffer, try to fill it
            try {
                fillBuffer();
            } catch (final IllegalArgumentException | IllegalStateException e) {
                // A corrupt stream can decode an out-of-range distance or overflow the sliding
                // window, which the CircularBuffer signals with unchecked exceptions. Wrap
                // them so callers only need to handle IOException.
                throw new CompressorException("Bad ARJ stream", e);
            }
        }
        final int ret = buffer.get();
        count(ret < 0 ? 0 : 1); // Increment input stream statistics
        return ret;
    }

    /**
     * Consumes the next {@code count} bits from the underlying stream.
     *
     * @param count the number of bits to consume.
     */
    private void skipBits(final int count) {
        bitsInBuffer -= count;
        bitBuffer &= (1L << bitsInBuffer) - 1;
        bitPosition += count;
    }
}
