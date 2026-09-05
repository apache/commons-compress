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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Compresses the blocks of a bzip2 stream concurrently on an {@link ExecutorService}.
 * <p>
 * bzip2 blocks are independent apart from the bit-level packing and the final combined CRC. The caller thread runs only the RLE1 stage's block-boundary
 * bookkeeping and slices the raw input at exactly the block boundaries the single-threaded encoder would use; each slice is compressed on the executor as a
 * standalone single-block bzip2 stream by the regular {@link BZip2CompressorOutputStream}. The caller thread appends the finished blocks' bits (between the
 * stream header and the end-of-stream trailer of the standalone streams) to the output in order and folds the block CRCs into the combined CRC, so the
 * output is byte-identical to the single-threaded encoder's.
 * </p>
 */
final class ParallelBZip2Encoder {

    /**
     * A compressed block: the worker's standalone bzip2 stream, the number of pad bits its last byte holds, and the block's CRC.
     */
    private static final class Encoded {

        private final byte[] stream;
        private final int padBits;
        private final int blockCrc;

        Encoded(final byte[] stream, final int padBits, final int blockCrc) {
            this.stream = stream;
            this.padBits = padBits;
            this.blockCrc = blockCrc;
        }
    }

    /** Bits of a standalone stream before its first block: the {@code BZh<n>} header. */
    private static final int HEADER_BITS = 32;

    /** Bits of a standalone stream after its last block: the end-of-stream magic and the combined CRC (the pad bits come on top). */
    private static final int TRAILER_BITS = 80;

    /**
     * Compresses {@code raw[0, length)} as a standalone bzip2 stream; the slicing guarantees it holds exactly one block.
     */
    private static Encoded encodeChunk(final byte[] raw, final int length, final int blockSize100k) throws IOException {
        final ByteArrayOutputStream compressed = new ByteArrayOutputStream(Math.max(1024, length / 3));
        final BZip2CompressorOutputStream encoder = new BZip2CompressorOutputStream(compressed, blockSize100k);
        encoder.write(raw, 0, length);
        encoder.finish();
        final byte[] stream = compressed.toByteArray();
        // The block CRC sits byte-aligned behind the 4-byte header and the 6-byte block magic.
        final int blockCrc = (stream[10] & 0xff) << 24 | (stream[11] & 0xff) << 16 | (stream[12] & 0xff) << 8 | stream[13] & 0xff;
        return new Encoded(stream, encoder.trailingPadBits, blockCrc);
    }

    /**
     * Returns {@code count} (at most 24) bits of {@code src} starting at bit {@code bitPosition}.
     */
    private static int peekBits(final byte[] src, final long bitPosition, final int count) {
        final int firstByte = (int) (bitPosition >>> 3);
        final int bitInByte = (int) (bitPosition & 7);
        final int bytes = bitInByte + count + 7 >> 3;
        long window = 0;
        for (int i = 0; i < bytes; i++) {
            window = window << 8 | src[firstByte + i] & 0xffL;
        }
        return (int) (window >>> (bytes << 3) - bitInByte - count) & (int) ((1L << count) - 1);
    }

    private final OutputStream out;
    private final int blockSize100k;

    /** The sequential encoder's constant: a block accepts another run while its {@code last} is below this. */
    private final int allowableBlockSize;
    private final ExecutorService executor;
    private final int maxInFlight;
    private final Deque<Future<Encoded>> inFlight = new ArrayDeque<>();

    /** The raw input bytes of the block being accumulated; grows as needed (RLE1 can pack more than the buffer's initial size into one block). */
    private byte[] chunk = new byte[1 << 20];
    private int chunkLength;

    /** The RLE1 run in progress, exactly as in the sequential encoder. */
    private int currentChar = -1;
    private int runLength;

    /** The sequential encoder's {@code last} for the block being accumulated: its RLE1-encoded length minus one. */
    private int simulatedLast = -1;

    private int combinedCrc;

    /** Stitching bit buffer: the low {@code bitCount} (&lt; 8) bits of {@code bitBuffer} wait for a full byte. */
    private long bitBuffer;
    private int bitCount;

    private final byte[] oneByte = new byte[1];

    ParallelBZip2Encoder(final OutputStream out, final int blockSize100k, final ExecutorService executor, final int maxConcurrentInFlight)
            throws IOException {
        if (maxConcurrentInFlight < 1) {
            throw new IllegalArgumentException("maxConcurrentInFlight must be at least 1: " + maxConcurrentInFlight);
        }
        this.out = out;
        this.blockSize100k = blockSize100k;
        this.allowableBlockSize = blockSize100k * BZip2Constants.BASEBLOCKSIZE - 20;
        this.executor = executor;
        this.maxInFlight = maxConcurrentInFlight;
        out.write('B');
        out.write('Z');
        out.write('h');
        out.write('0' + blockSize100k);
    }

    /**
     * Appends the bits {@code [fromBit, toBit)} of {@code src} to the output.
     */
    private void appendBits(final byte[] src, final long fromBit, final long toBit) throws IOException {
        for (long position = fromBit; position < toBit;) {
            final int n = (int) Math.min(24, toBit - position);
            writeBits(n, peekBits(src, position, n));
            position += n;
        }
    }

    /**
     * Waits for the oldest submitted block, folds its CRC into the combined CRC and appends its bits to the output.
     */
    private void deliver() throws IOException {
        final Encoded encoded;
        try {
            encoded = inFlight.removeFirst().get();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while compressing a block", e);
        } catch (final ExecutionException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new IOException(cause);
        }
        combinedCrc = (combinedCrc << 1 | combinedCrc >>> 31) ^ encoded.blockCrc;
        appendBits(encoded.stream, HEADER_BITS, (long) encoded.stream.length * 8 - encoded.padBits - TRAILER_BITS);
    }

    /**
     * The block-boundary half of the sequential encoder's {@code writeRun()}: ends the block exactly where it would, submitting the accumulated slice
     * without the {@code length} trailing bytes of the run just ended, which belong to the next block.
     */
    private void endRun(final int length) throws IOException {
        if (simulatedLast >= allowableBlockSize) {
            submitChunk(chunkLength - length);
            simulatedLast = -1;
        }
        simulatedLast += length <= 3 ? length : 5;
    }

    void finish() throws IOException {
        try {
            if (runLength > 0) {
                endRun(runLength);
            }
            if (chunkLength > 0) {
                submitChunk(chunkLength);
            }
            while (!inFlight.isEmpty()) {
                deliver();
            }
            writeBits(24, 0x177245);
            writeBits(24, 0x385090);
            writeBits(16, combinedCrc >>> 16);
            writeBits(16, combinedCrc & 0xffff);
            if (bitCount > 0) {
                out.write((int) bitBuffer << 8 - bitCount & 0xff);
                bitCount = 0;
            }
        } finally {
            for (final Future<Encoded> future : inFlight) {
                future.cancel(true);
            }
            inFlight.clear();
            chunk = null;
        }
    }

    /**
     * Hands {@code chunk[0, length)} to the executor and removes it from the accumulation buffer, waiting for older blocks first when
     * {@code maxInFlight} is reached.
     */
    private void submitChunk(final int length) throws IOException {
        while (inFlight.size() >= maxInFlight) {
            deliver();
        }
        final byte[] raw = Arrays.copyOf(chunk, length);
        final int blockSize = blockSize100k;
        inFlight.addLast(executor.submit(() -> encodeChunk(raw, length, blockSize)));
        System.arraycopy(chunk, length, chunk, 0, chunkLength - length);
        chunkLength -= length;
    }

    void write(final byte[] buf, final int offs, final int len) throws IOException {
        if (chunkLength + len > chunk.length) {
            chunk = Arrays.copyOf(chunk, Math.max(chunkLength + len, 2 * chunk.length));
        }
        // The run-length encoding of the sequential encoder's write(byte[], int, int), tracking block boundaries instead of writing runs.
        int cur = currentChar;
        int run = runLength;
        int position = offs;
        for (final int hi = offs + len; position < hi;) {
            final int b = buf[position++] & 0xff;
            if (cur == b) {
                chunk[chunkLength++] = (byte) b;
                if (++run > 254) {
                    endRun(run);
                    cur = -1;
                    run = 0;
                }
            } else if (cur == -1) {
                chunk[chunkLength++] = (byte) b;
                cur = b;
                run = 1;
            } else {
                endRun(run);
                chunk[chunkLength++] = (byte) b;
                cur = b;
                run = 1;
            }
        }
        currentChar = cur;
        runLength = run;
    }

    void write(final int b) throws IOException {
        oneByte[0] = (byte) b;
        write(oneByte, 0, 1);
    }

    /**
     * Appends the low {@code count} (at most 24) bits of {@code bits} to the output.
     */
    private void writeBits(final int count, final int bits) throws IOException {
        bitBuffer = bitBuffer << count | bits & (1L << count) - 1;
        bitCount += count;
        while (bitCount >= 8) {
            out.write((int) (bitBuffer >>> bitCount - 8) & 0xff);
            bitCount -= 8;
        }
    }
}
