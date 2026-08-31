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
import org.apache.commons.io.IOUtils;

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
 * In <em>bulk</em> mode the source is read in chunks of {@value #BUFFER_SIZE} bytes into an internal buffer and the bit buffer is refilled eight bytes at a
 * time. The source is then read ahead of the decoder, so bulk mode is used when that cannot matter (the decoder consumes the input to its end, i.e.
 * decompresses concatenated streams) or when the source supports {@link InputStream#mark(int)}: it is then repositioned to the first byte not consumed at
 * every chunk boundary, at the end of the bzip2 stream and after an error, so that it ends up exactly where the exact mode leaves it. Each chunk is
 * bracketed by its own {@code mark(BUFFER_SIZE)}, and never more than {@value #BUFFER_SIZE} bytes are read before the mark is reset to or replaced, so
 * the mark limit is honoured; a source that invalidates its mark anyway makes {@code reset()} throw an {@link IOException} (no data is decoded wrongly, only
 * the source position is lost). Because the source's single mark is used, a mark set by the caller beforehand is not preserved.
 * </p>
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
     * Bytes pulled from the underlying stream, including buffered ones (in both buffers).
     */
    private long bytesRead;

    /**
     * Set once a request for bits could not be satisfied: from then on {@link #getBytesRead()} reports every byte pulled, matching the historical
     * behaviour of this stream.
     */
    private boolean exhausted;

    private final byte[] scratch = new byte[8];

    /**
     * Size of the input buffer in bulk mode.
     */
    static final int BUFFER_SIZE = 1 << 16;

    /**
     * Bulk mode: read the source in chunks (may read ahead of the bzip2 stream).
     */
    private final boolean bulk;

    /**
     * Bulk mode over a source with mark/reset: the source is repositioned to the consumed byte at chunk boundaries, at the end and after errors.
     */
    private final boolean repositionable;

    /**
     * Input buffer (bulk mode only); {@code buf[pos..limit)} is unread. Eight spare bytes so that a refill can always read a whole word.
     */
    private final byte[] buf;
    private int pos;
    private int limit;
    private boolean sourceEof;
    private boolean repositioned;

    /**
     * Creates a reader that pulls exactly the bytes it needs from the source.
     */
    BZip2BitReader(final InputStream in) {
        this(in, false);
    }

    /**
     * Creates a reader.
     *
     * @param in           the source.
     * @param consumeToEnd whether the decoder consumes the source to its end (concatenated streams), which allows reading ahead freely.
     */
    BZip2BitReader(final InputStream in, final boolean consumeToEnd) {
        this.in = in;
        this.repositionable = !consumeToEnd && in.markSupported();
        this.bulk = consumeToEnd || repositionable;
        this.buf = bulk ? new byte[BUFFER_SIZE + 8] : null;
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
        if (bulk) {
            fillBulk();
            return;
        }
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
     * Greedy refill in bulk mode: ORs a whole 8-byte word into the bit buffer but only consumes as many bytes as fit. The bits of the partially
     * consumed byte land below the valid region; that is harmless, because nothing reads below {@link #bitCount} and every later refill ORs the very
     * same bits in again from the same, still unread, bytes.
     */
    private void fillBulk() throws IOException {
        final byte[] buf = this.buf;
        while (bitCount <= 56) {
            final int pos = this.pos;
            final int available = limit - pos;
            if (available >= 8) {
                final long w = (long) buf[pos] << 56 | (long) (buf[pos + 1] & 0xff) << 48 | (long) (buf[pos + 2] & 0xff) << 40
                        | (long) (buf[pos + 3] & 0xff) << 32 | (long) (buf[pos + 4] & 0xff) << 24 | (buf[pos + 5] & 0xff) << 16
                        | (buf[pos + 6] & 0xff) << 8 | buf[pos + 7] & 0xff;
                final int k = 64 - bitCount >>> 3;
                bitBuffer |= w >>> bitCount;
                this.pos = pos + k;
                bitCount += k << 3;
                return;
            }
            if (available > 0) {
                // Tail of a chunk: byte by byte, so that a chunk is only ever replaced once it is empty.
                while (bitCount <= 56 && this.pos < limit) {
                    bitBuffer |= (long) (buf[this.pos++] & 0xff) << 56 - bitCount;
                    bitCount += 8;
                }
            } else if (!refillBuffer()) {
                return;
            }
        }
    }

    /**
     * Lazily reads exactly the bytes needed to have {@code n} bits buffered, stopping silently at end of input.
     */
    private void fillLazy(final int n) throws IOException {
        if (bulk) {
            while (bitCount < n) {
                if (pos >= limit && !refillBuffer()) {
                    return;
                }
                bitBuffer |= (long) (buf[pos++] & 0xff) << 56 - bitCount;
                bitCount += 8;
            }
            return;
        }
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
     * Bulk mode: reads the next chunk of the source into the (empty) buffer.
     *
     * @return false if the source is exhausted.
     */
    private boolean refillBuffer() throws IOException {
        if (sourceEof) {
            return false;
        }
        // Bytes the new chunk must exceed to make progress (the lookahead bytes read again below).
        int reread = 0;
        if (repositionable) {
            // Put the source at the first byte not consumed yet (the whole bytes of lookahead in the bit buffer are dropped and read again), then mark
            // it so that it can be brought back there when the bzip2 stream ends.
            reread = bitCount >>> 3;
            if (limit > 0) {
                in.reset();
                IOUtils.skipFully(in, limit - reread);
            }
            dropLookaheadBytes();
            in.mark(BUFFER_SIZE);
        }
        pos = 0;
        limit = 0;
        while (limit <= reread) {
            final int n = in.read(buf, limit, BUFFER_SIZE - limit);
            if (n <= 0) {
                sourceEof = true;
                break;
            }
            bytesRead += n;
            limit += n;
        }
        return limit > 0;
    }

    /**
     * Drops the whole bytes of lookahead from the bit buffer, keeping only the bits of the partially consumed byte; adjusts {@link #bytesRead}
     * accordingly.
     */
    private void dropLookaheadBytes() {
        final int lookaheadBytes = bitCount >>> 3;
        bytesRead -= lookaheadBytes;
        bitCount -= lookaheadBytes << 3;
        bitBuffer = bitCount == 0 ? 0 : bitBuffer & -1L << 64 - bitCount;
    }

    /**
     * Repositions a source with mark/reset to the first byte not consumed (see the class comment). Called at the end of a bzip2 stream and after an
     * error; a no-op for other sources, after a failed read (the source is then at its end, as before) and when called again.
     *
     * @throws IOException if the source fails to reposition.
     */
    void repositionSource() throws IOException {
        if (!repositionable || exhausted || repositioned || limit == 0) {
            return;
        }
        repositioned = true;
        in.reset();
        IOUtils.skipFully(in, pos - (bitCount >>> 3));
        bytesRead -= limit - pos;
        limit = 0;
        pos = 0;
        dropLookaheadBytes();
    }

    /**
     * Gets the number of bytes of the underlying stream consumed so far, {@code ceil(bits consumed / 8)}, independent of how many bytes are buffered.
     *
     * @return bytes consumed.
     */
    long getBytesRead() {
        return exhausted ? bytesRead : bytesRead - (limit - pos) - (bitCount >>> 3);
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
