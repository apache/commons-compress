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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.commons.compress.compressors.CompressorException;

/**
 * Decompresses the blocks of a bzip2 stream concurrently on an {@link ExecutorService}.
 * <p>
 * bzip2 blocks are independent, but the stream gives their boundaries only implicitly: each block starts with a 48-bit magic number at an arbitrary
 * <em>bit</em> position. The input is therefore scanned for the block and end-of-stream magic numbers at every bit offset; each segment between two
 * magics is wrapped into a synthesized single-block bzip2 stream (header, the segment's bits shifted to byte alignment, end-of-stream magic, and the
 * block's own stored CRC as the combined CRC) and handed to the executor, where a regular {@link BZip2CompressorInputStream} decodes and verifies it.
 * The blocks are delivered in order and the per-stream combined CRC is checked against the folded block CRCs.
 * </p>
 * <p>
 * A magic number can in principle also occur inside a block's payload (once every 2^48 bits of random payload): such a segment fails to decode, and the
 * decoder then treats that position as payload and rescans. Corrupt input therefore fails with an {@link IOException} like the sequential decoder,
 * though not necessarily with the same message or at the same output position.
 * </p>
 *
 * @NotThreadSafe
 */
final class ParallelBZip2Decoder implements Closeable {

    /**
     * A parsed block: its bit range in the stream, its stored CRC, and the synthesized single-block stream to decode.
     */
    private static final class Segment {
        final long startBit;
        long endBit;
        final int storedBlockCrc;
        byte[] synthesized;

        Segment(final long startBit, final long endBit, final int storedBlockCrc) {
            this.startBit = startBit;
            this.endBit = endBit;
            this.storedBlockCrc = storedBlockCrc;
        }
    }

    private static final long BLOCK_MAGIC = 0x314159265359L;
    private static final long EOS_MAGIC = 0x177245385090L;
    private static final long MAGIC_MASK = 0xFFFFFFFFFFFFL;

    /**
     * The input is read and buffered in chunks of this size.
     */
    private static final int CHUNK_SIZE = 512 * 1024;

    /**
     * The scan for a block's end delimiter gives up after this many bytes and reports corruption. A valid compressed block cannot come close: even a
     * format-valid adversarial encoder spends at most 20 bits per symbol on at most 900,001 MTF/RLE2 symbols plus table sections, under 2.3 MiB, so the
     * cap can only be hit by corrupt input, on which the sequential decoder would have failed long before. The cap keeps the rolling buffer bounded
     * where garbage after a block magic would otherwise buffer the rest of the input while searching for a delimiter that never comes.
     */
    private static final long MAX_DELIMITER_SCAN_BYTES = 4 * 1024 * 1024;

    /**
     * Decodes one synthesized single-block stream; runs on the executor.
     */
    private static byte[] decodeSegment(final byte[] synthesized) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(64, synthesized.length * 3));
        try (InputStream decoder = new BZip2CompressorInputStream(new ByteArrayInputStream(synthesized))) {
            final byte[] tmp = new byte[64 * 1024];
            int n;
            while ((n = decoder.read(tmp, 0, tmp.length)) >= 0) {
                out.write(tmp, 0, n);
            }
        }
        return out.toByteArray();
    }

    /**
     * Writes the low {@code count} (at most 32) bits of {@code value} into {@code dst} at bit offset {@code dstBitOffset}.
     */
    private static void writeBitsInto(final byte[] dst, final long dstBitOffset, final int value, final int count) {
        for (int i = 0; i < count; i++) {
            final int bit = value >>> count - 1 - i & 1;
            final long pos = dstBitOffset + i;
            final int idx = (int) (pos >>> 3);
            final int shift = 7 - (int) (pos & 7);
            dst[idx] = (byte) (dst[idx] & ~(1 << shift) | bit << shift);
        }
    }

    private final InputStream in;
    private final ExecutorService executor;
    private final boolean decompressConcatenated;
    private final int maxInFlight;

    /**
     * Rolling buffer of compressed input; {@code buffer[0]} is stream byte {@code bufferStart}, {@code length} bytes are valid.
     */
    private byte[] buffer = new byte[2 * CHUNK_SIZE];
    private long bufferStart;
    private int length;
    private boolean inputExhausted;
    private long bytesPulled;

    /**
     * Absolute bit position of the next unparsed bit.
     */
    private long position;
    private int blockSize100k;
    private int computedCombinedCrc;
    private boolean done;

    private final ArrayDeque<Future<byte[]>> inFlight = new ArrayDeque<>();
    private final ArrayDeque<Segment> inFlightSegments = new ArrayDeque<>();

    /**
     * Stored CRCs of the blocks of the current stream that were delivered, for refolding after a rescan.
     */
    private final List<Integer> deliveredCrcs = new ArrayList<>();

    /**
     * Bit positions to ignore as segment delimiters (identified false positives).
     */
    private final List<Long> ignoredDelimiters = new ArrayList<>();

    /**
     * Test hook: bit positions treated as additional (spurious) delimiter candidates, to exercise the rescan path.
     */
    final List<Long> spuriousDelimitersForTesting = new ArrayList<>();

    private byte[] current;
    private int currentPos;

    /**
     * A parse error found while reading ahead. It is only reported once every block parsed before it has decoded successfully: a false-positive
     * delimiter would make the parser read garbage, and the rescan after the failing block decode supersedes the error.
     */
    private IOException deferredParseError;

    ParallelBZip2Decoder(final InputStream in, final boolean decompressConcatenated, final ExecutorService executor, final int maxConcurrentInFlight)
            throws IOException {
        if (maxConcurrentInFlight < 1) {
            throw new IllegalArgumentException("maxConcurrentInFlight must be at least 1: " + maxConcurrentInFlight);
        }
        this.in = in;
        this.executor = executor;
        this.decompressConcatenated = decompressConcatenated;
        this.maxInFlight = maxConcurrentInFlight;
        readStreamHeader(true);
    }

    /**
     * Appends {@code bitLength} bits starting at absolute position {@code startBit} to {@code dst} at {@code dstBitOffset}; the bits must be buffered.
     */
    private void appendBits(final byte[] dst, final long dstBitOffset, final long startBit, final long bitLength) {
        for (long copied = 0; copied < bitLength;) {
            final int n = (int) Math.min(24, bitLength - copied);
            final int bits = (int) peekBitsAt(startBit + copied, n);
            writeBitsInto(dst, dstBitOffset + copied, bits, n);
            copied += n;
        }
    }

    @Override
    public void close() throws IOException {
        done = true;
        for (final Future<byte[]> f : inFlight) {
            f.cancel(false);
        }
        inFlight.clear();
        inFlightSegments.clear();
        current = null;
        in.close();
    }

    /**
     * Makes sure every byte up to and including the byte holding bit {@code bitPosition + bitCount - 1} is buffered.
     *
     * @return false at end of input.
     */
    private boolean ensureBits(final long bitPosition, final int bitCount) throws IOException {
        final long lastByte = (bitPosition + bitCount + 7) / 8;
        while (bufferStart + length < lastByte) {
            if (!fill()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Reads one more chunk into the buffer, compacting bytes before the parse position first.
     *
     * @return false at end of input.
     */
    private boolean fill() throws IOException {
        if (inputExhausted) {
            return false;
        }
        final int dropBytes = (int) Math.min(position / 8 - bufferStart, length);
        if (dropBytes > 0) {
            System.arraycopy(buffer, dropBytes, buffer, 0, length - dropBytes);
            bufferStart += dropBytes;
            length -= dropBytes;
        }
        if (length + CHUNK_SIZE > buffer.length) {
            final byte[] grown = new byte[Math.max(buffer.length * 2, length + CHUNK_SIZE)];
            System.arraycopy(buffer, 0, grown, 0, length);
            buffer = grown;
        }
        final int n = in.read(buffer, length, CHUNK_SIZE);
        if (n <= 0) {
            inputExhausted = true;
            return false;
        }
        length += n;
        bytesPulled += n;
        return true;
    }

    long getBytesRead() {
        return bytesPulled;
    }

    /**
     * The absolute bit position after the last buffered byte.
     */
    private long limitBits() {
        return (bufferStart + length) * 8;
    }

    /**
     * Retrieves the next decoded block into {@link #current}.
     *
     * @return false at the end of all streams.
     */
    private boolean nextBlock() throws IOException {
        while (true) {
            submitUpToLimit();
            if (inFlight.isEmpty()) {
                if (deferredParseError != null) {
                    final IOException e = deferredParseError;
                    deferredParseError = null;
                    done = true;
                    throw e;
                }
                return false;
            }
            final Future<byte[]> head = inFlight.removeFirst();
            final Segment segment = inFlightSegments.removeFirst();
            try {
                current = head.get();
                currentPos = 0;
                deliveredCrcs.add(segment.storedBlockCrc);
                if (current.length == 0) {
                    continue;
                }
                return true;
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CompressorException("Interrupted while decompressing", e);
            } catch (final ExecutionException e) {
                // Possibly a false-positive magic ended this segment early: treat that position as payload and rescan.
                if (!retrySegment(segment)) {
                    final Throwable cause = e.getCause();
                    if (cause instanceof IOException) {
                        throw (IOException) cause;
                    }
                    throw new CompressorException("Error decompressing block", cause);
                }
            }
        }
    }

    /**
     * Finds the next delimiter (block or end-of-stream magic, or an injected test candidate) at or after {@code from}, ignoring known false positives;
     * reads more input as needed.
     *
     * @return the delimiter's bit position, or -1 when the input ends without one.
     */
    private long nextDelimiter(final long from) throws IOException {
        long spurious = Long.MAX_VALUE;
        for (final long s : spuriousDelimitersForTesting) {
            if (s >= from && s < spurious && !ignoredDelimiters.contains(s)) {
                spurious = s;
            }
        }
        // The window holds the last eight buffered bytes, i.e. the 64 bits ending after buffer index i; the 48-bit values starting at the eight bit
        // offsets of byte i - 7 are window >>> 16 - sh for sh in 0..7.
        long windowByte = from / 8;
        long window = 0;
        int have = 0;
        while (true) {
            if (windowByte - from / 8 > MAX_DELIMITER_SCAN_BYTES) {
                throw new CompressorException("Stream corrupted: no block delimiter within " + MAX_DELIMITER_SCAN_BYTES + " bytes");
            }
            if (!ensureBits(windowByte * 8, 8)) {
                return spurious != Long.MAX_VALUE ? spurious : -1;
            }
            final long limitByte = bufferStart + length;
            final byte[] buf = buffer;
            final long base = bufferStart;
            while (windowByte < limitByte) {
                window = window << 8 | buf[(int) (windowByte - base)] & 0xffL;
                windowByte++;
                if (++have >= 8) {
                    final long firstBit = (windowByte - 8) * 8;
                    for (int sh = 0; sh < 8; sh++) {
                        final long value = window >>> 16 - sh & MAGIC_MASK;
                        if (value == BLOCK_MAGIC || value == EOS_MAGIC) {
                            final long bit = firstBit + sh;
                            if (bit >= from && !ignoredDelimiters.contains(bit)) {
                                return Math.min(bit, spurious);
                            }
                        }
                    }
                    if (spurious < firstBit) {
                        return spurious;
                    }
                }
            }
        }
    }

    /**
     * Parses the next block segment, handling end-of-stream trailers and concatenated stream headers.
     *
     * @return the next segment, or null at the end of all streams.
     */
    private Segment parseNextSegment() throws IOException {
        Segment segment = null;
        while (segment == null && !done) {
            if (!ensureBits(position, 48)) {
                throw new CompressorException("Unexpected end of stream");
            }
            final long magic = peekBitsAt(position, 48);
            if (magic == EOS_MAGIC) {
                if (!ensureBits(position + 48, 32)) {
                    throw new CompressorException("Unexpected end of stream");
                }
                final int storedCombinedCrc = (int) peekBitsAt(position + 48, 32);
                if (storedCombinedCrc != computedCombinedCrc) {
                    throw new CompressorException("BZip2 CRC error");
                }
                position += 80;
                if (!decompressConcatenated) {
                    done = true;
                } else {
                    deliveredCrcs.clear();
                    readStreamHeader(false);
                }
                continue;
            }
            if (magic != BLOCK_MAGIC) {
                throw new CompressorException("Bad block header");
            }
            if (!ensureBits(position + 48, 32)) {
                throw new CompressorException("Unexpected end of stream");
            }
            final int storedBlockCrc = (int) peekBitsAt(position + 48, 32);
            final long end = nextDelimiter(position + 80);
            segment = new Segment(position, end, storedBlockCrc);
            if (end < 0) {
                // Input ends inside this block: the block decoder will report the corruption.
                segment.endBit = limitBits();
                done = true;
            }
            computedCombinedCrc = (computedCombinedCrc << 1 | computedCombinedCrc >>> 31) ^ storedBlockCrc;
            position = segment.endBit;
        }
        return segment;
    }

    /**
     * Returns {@code count} (at most 32) bits starting at absolute bit position {@code bitPosition}; the bytes must be buffered.
     */
    private long peekBitsAt(final long bitPosition, final int count) {
        final int byteIndex = (int) (bitPosition / 8 - bufferStart);
        final int bitInByte = (int) (bitPosition & 7);
        final int bytes = (bitInByte + count + 7) / 8;
        long window = 0;
        for (int i = 0; i < bytes; i++) {
            window = window << 8 | buffer[byteIndex + i] & 0xffL;
        }
        return window >>> (8 - (bitInByte + count) % 8) % 8 & (1L << count) - 1;
    }

    int read(final byte[] dest, final int off, final int len) throws IOException {
        int total = 0;
        while (total < len) {
            if (current == null || currentPos == current.length) {
                current = null;
                if (!nextBlock()) {
                    break;
                }
            }
            final int n = Math.min(len - total, current.length - currentPos);
            System.arraycopy(current, currentPos, dest, off + total, n);
            currentPos += n;
            total += n;
        }
        return total == 0 ? -1 : total;
    }

    /**
     * Reads a stream header ("BZh" and the block size digit) at the next byte boundary.
     */
    private void readStreamHeader(final boolean first) throws IOException {
        position = (position + 7) / 8 * 8;
        if (!ensureBits(position, 8)) {
            if (first) {
                throw new CompressorException("Stream is not in the BZip2 format");
            }
            done = true;
            return;
        }
        if (!ensureBits(position, 32) || peekBitsAt(position, 8) != 'B' || peekBitsAt(position + 8, 8) != 'Z' || peekBitsAt(position + 16, 8) != 'h') {
            throw new CompressorException(first ? "Stream is not in the BZip2 format" : "Unexpected data after a valid BZip2 stream");
        }
        final int digit = (int) peekBitsAt(position + 24, 8);
        if (digit < '1' || digit > '9') {
            throw new CompressorException("BZip2 block size is invalid");
        }
        blockSize100k = digit - '0';
        computedCombinedCrc = 0;
        position += 32;
    }

    /**
     * The head segment failed to decode: if it ended at a delimiter candidate (not at the end of input), treat that candidate as payload, discard
     * everything parsed after it, and rescan from this segment's start.
     *
     * @return true if a retry was set up.
     */
    private boolean retrySegment(final Segment segment) {
        if (segment.endBit >= limitBits() && inputExhausted) {
            return false;
        }
        for (final Future<byte[]> f : inFlight) {
            f.cancel(false);
        }
        inFlight.clear();
        inFlightSegments.clear();
        ignoredDelimiters.add(segment.endBit);
        deferredParseError = null;
        done = false;
        position = segment.startBit;
        // Refold the combined CRC from the blocks actually delivered; the discarded parses folded ahead.
        int crc = 0;
        for (final int c : deliveredCrcs) {
            crc = (crc << 1 | crc >>> 31) ^ c;
        }
        computedCombinedCrc = crc;
        return true;
    }

    /**
     * Parses segments and submits decode jobs until the in-flight limit is reached or the input ends.
     */
    private void submitUpToLimit() {
        while (inFlight.size() < maxInFlight && !done && deferredParseError == null) {
            final Segment segment;
            try {
                segment = parseNextSegment();
            } catch (final IOException e) {
                deferredParseError = e;
                return;
            }
            if (segment == null) {
                return;
            }
            synthesize(segment);
            inFlight.addLast(executor.submit(() -> decodeSegment(segment.synthesized)));
            inFlightSegments.addLast(segment);
        }
    }

    /**
     * Builds the synthesized single-block stream for a segment.
     */
    private void synthesize(final Segment segment) {
        final long bitLength = segment.endBit - segment.startBit;
        final byte[] out = new byte[4 + (int) ((bitLength + 80 + 7) / 8)];
        out[0] = 'B';
        out[1] = 'Z';
        out[2] = 'h';
        out[3] = (byte) ('0' + blockSize100k);
        appendBits(out, 32, segment.startBit, bitLength);
        writeBitsInto(out, 32 + bitLength, (int) (EOS_MAGIC >>> 24), 24);
        writeBitsInto(out, 32 + bitLength + 24, (int) (EOS_MAGIC & 0xFFFFFF), 24);
        writeBitsInto(out, 32 + bitLength + 48, segment.storedBlockCrc, 32);
        segment.synthesized = out;
    }
}
