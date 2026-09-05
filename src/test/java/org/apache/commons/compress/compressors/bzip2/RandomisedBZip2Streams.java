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
import java.util.Arrays;

/**
 * Builds bzip2 streams that have the (obsolete, bzip2 0.9.x era) block randomisation bit set.
 * <p>
 * No modern encoder produces randomised blocks, so this helper derives one from a normal stream: the decoder applies the randomisation mask (see
 * {@link Rand}) to the RLE1-encoded block bytes as they come out of the inverse BWT, so a stream whose <em>encoded</em> RLE1 image is {@code RLE1(X) XOR
 * mask} and which carries the randomisation bit decodes to {@code X}. We obtain that stream by compressing {@code RLE1^-1(RLE1(X) XOR mask)} with the regular
 * encoder and then patching the randomisation bit, the block CRC and the combined CRC. Only single-block streams are handled, because the patch offsets of
 * the first block are byte aligned.
 * </p>
 */
final class RandomisedBZip2Streams {

    private static final long EOS_MAGIC = 0x177245385090L;

    /**
     * Applies the decoder's randomisation walk to every byte of an RLE1 image (both data bytes and run-count bytes are affected, as in
     * {@code setupRandPartA/B}).
     */
    static byte[] applyRandMask(final byte[] rle1) {
        final byte[] out = rle1.clone();
        int rNToGo = 0;
        int rTPos = 0;
        for (int i = 0; i < out.length; i++) {
            if (rNToGo == 0) {
                rNToGo = Rand.rNums(rTPos) - 1;
                if (++rTPos == 512) {
                    rTPos = 0;
                }
            } else {
                rNToGo--;
            }
            if (rNToGo == 1) {
                out[i] ^= 1;
            }
        }
        return out;
    }

    static int crc(final byte[] data) {
        final CRC crc = new CRC();
        for (final byte b : data) {
            crc.update(b & 0xff);
        }
        return crc.getValue();
    }

    /**
     * Builds a single-block stream with the randomisation bit set that decodes to {@code original}.
     *
     * @param original  the bytes the stream must decode to.
     * @param blockSize block size (1-9) to compress with; the RLE1 image of {@code original} must fit into one block.
     * @return the stream, or {@code null} if the masked RLE1 image is not a canonical RLE1 stream (retry with different input).
     * @throws IOException on compressor failure.
     */
    static byte[] randomisedStream(final byte[] original, final int blockSize) throws IOException {
        final byte[] rle = rle1(original);
        final int allowable = blockSize * BZip2Constants.BASEBLOCKSIZE - 20 - 5;
        if (rle.length > allowable) {
            throw new IllegalArgumentException("RLE1 image of " + rle.length + " bytes does not fit into one block of size " + blockSize);
        }
        final byte[] masked = applyRandMask(rle);
        final byte[] toCompress = unRle1(masked);
        if (!Arrays.equals(rle1(toCompress), masked)) {
            return null;
        }
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (BZip2CompressorOutputStream out = new BZip2CompressorOutputStream(baos, blockSize)) {
            out.write(toCompress);
        }
        final byte[] stream = baos.toByteArray();
        if (stream.length < 15 || stream[0] != 'B' || stream[1] != 'Z' || stream[2] != 'h' || stream[4] != 0x31 || stream[9] != 0x59) {
            throw new IllegalStateException("Unexpected stream layout");
        }
        final int blockCrc = crc(original);
        // "BZhN" (32 bits) + block magic (48 bits) = 80 bits, then the 32-bit block CRC, then the randomisation bit.
        writeBits(stream, 80, 32, blockCrc & 0xffffffffL);
        stream[14] |= (byte) 0x80;
        // The trailer is EOS magic (48 bits) + combined CRC (32 bits) + 0-7 padding bits; find the alignment.
        final long totalBits = (long) stream.length * 8;
        for (int pad = 0; pad < 8; pad++) {
            final long p = totalBits - pad - 80;
            if (p >= 0 && readBits(stream, p, 48) == EOS_MAGIC) {
                // Single block: combined CRC == (0 rotl 1) ^ blockCrc == blockCrc.
                writeBits(stream, p + 48, 32, blockCrc & 0xffffffffL);
                return stream;
            }
        }
        throw new IllegalStateException("End-of-stream magic not found");
    }

    static long readBits(final byte[] a, final long bitPos, final int n) {
        long v = 0;
        for (int i = 0; i < n; i++) {
            final long p = bitPos + i;
            final int bit = a[(int) (p >>> 3)] >>> 7 - (int) (p & 7) & 1;
            v = v << 1 | bit;
        }
        return v;
    }

    /**
     * The first stage of bzip2: runs of 4..255 identical bytes become the byte four times followed by {@code run - 4}.
     */
    static byte[] rle1(final byte[] in) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream(in.length + 16);
        int i = 0;
        while (i < in.length) {
            final byte b = in[i];
            int run = 1;
            while (run < 255 && i + run < in.length && in[i + run] == b) {
                run++;
            }
            if (run < 4) {
                for (int k = 0; k < run; k++) {
                    out.write(b);
                }
            } else {
                out.write(b);
                out.write(b);
                out.write(b);
                out.write(b);
                out.write(run - 4);
            }
            i += run;
        }
        return out.toByteArray();
    }

    /**
     * Inverse of RLE1 with the decoder's semantics: after four identical bytes the next byte is a repeat count, and the run state restarts afterwards.
     */
    static byte[] unRle1(final byte[] in) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream(in.length * 2);
        int i = 0;
        while (i < in.length) {
            final byte b = in[i];
            int run = 1;
            while (run < 4 && i + run < in.length && in[i + run] == b) {
                run++;
            }
            for (int k = 0; k < run; k++) {
                out.write(b);
            }
            i += run;
            if (run == 4 && i < in.length) {
                final int count = in[i++] & 0xff;
                for (int k = 0; k < count; k++) {
                    out.write(b);
                }
            }
        }
        return out.toByteArray();
    }

    static void writeBits(final byte[] a, final long bitPos, final int n, final long value) {
        for (int i = 0; i < n; i++) {
            final long p = bitPos + i;
            final int idx = (int) (p >>> 3);
            final int shift = 7 - (int) (p & 7);
            final int bit = (int) (value >>> n - 1 - i & 1);
            a[idx] = (byte) (a[idx] & ~(1 << shift) | bit << shift);
        }
    }

    private RandomisedBZip2Streams() {
    }
}
