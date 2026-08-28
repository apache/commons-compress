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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.zip.CRC32;

/**
 * The bzip2 block CRC (CRC-32/BZIP2: polynomial 0x04C11DB7, most significant bit first, initial value and final XOR 0xFFFFFFFF).
 * <p>
 * The computation is delegated to {@link CRC32}, whose implementation is a HotSpot intrinsic (carry-less multiplication where the CPU has it). That
 * class computes the <em>reflected</em> variant of the same polynomial, i.e. it processes bits least significant first, so the bzip2 CRC of a message is
 * the bit-reversed {@link CRC32} of the message with the bits of every byte reversed: {@code bzip2(M) = reverse32(crc32(reverse8(M)))}. Bytes are
 * reversed eight at a time with a few mask-and-shift steps and collected in a buffer, so that single-byte updates (and the compressor's run updates)
 * cost a store each and the intrinsic sees large chunks. Measured on JDK 25 this is about 1.6x faster than a slicing-by-8 table CRC.
 * </p>
 *
 * @NotThreadSafe
 */
final class CRC {

    /**
     * Bit reversal of a byte, for single-byte updates.
     */
    private static final byte[] REVERSE = new byte[256];

    private static final int BUFFER_SIZE = 8192;

    static {
        for (int i = 0; i < 256; i++) {
            REVERSE[i] = (byte) (Integer.reverse(i) >>> 24);
        }
    }

    private final CRC32 crc32 = new CRC32();

    /**
     * Reflected bytes not yet fed to {@link #crc32}.
     */
    private final byte[] buffer = new byte[BUFFER_SIZE];
    private final ByteBuffer bufferView = ByteBuffer.wrap(buffer).order(ByteOrder.nativeOrder());

    /**
     * The source array of the last bulk update and a view of it: wrapping a {@link ByteBuffer} is far too expensive per call, and callers keep passing the
     * same array (their output buffer). Released by {@link #release()} when the owner is done, not per block: a field store on the per-block path was
     * measured to cost 14% of the decoding time (an inlining cliff), while the reference itself is only ever the caller's own buffer.
     */
    private byte[] lastSrc;
    private ByteBuffer lastSrcView;
    private int buffered;

    CRC() {
        reset();
    }

    private void flush() {
        if (buffered > 0) {
            crc32.update(buffer, 0, buffered);
            buffered = 0;
        }
    }

    int getValue() {
        flush();
        return Integer.reverse((int) crc32.getValue());
    }

    /**
     * Drops the reference to the array of the last bulk update.
     */
    void release() {
        lastSrc = null;
        lastSrcView = null;
    }

    void reset() {
        crc32.reset();
        buffered = 0;
    }

    /**
     * Reverses the bits of every byte of {@code src[off..off+len)} into {@link #buffer}, eight bytes at a time through {@link ByteBuffer} views in native
     * byte order (the order does not matter for a bit reversal within bytes, but the default big-endian order would add a byte swap per word).
     */
    private void reverseBits(final byte[] src, final int off, final int len) {
        if (src != lastSrc) {
            lastSrc = src;
            lastSrcView = ByteBuffer.wrap(src).order(ByteOrder.nativeOrder());
        }
        final ByteBuffer in = lastSrcView;
        final ByteBuffer out = bufferView;
        int i = 0;
        for (; i + 8 <= len; i += 8) {
            long x = in.getLong(off + i);
            x = (x & 0x5555555555555555L) << 1 | x >>> 1 & 0x5555555555555555L;
            x = (x & 0x3333333333333333L) << 2 | x >>> 2 & 0x3333333333333333L;
            x = (x & 0x0F0F0F0F0F0F0F0FL) << 4 | x >>> 4 & 0x0F0F0F0F0F0F0F0FL;
            out.putLong(i, x);
        }
        final byte[] reverse = REVERSE;
        for (; i < len; i++) {
            buffer[i] = reverse[src[off + i] & 0xff];
        }
    }

    /**
     * Updates the CRC with a range of bytes.
     */
    void update(final byte[] buf, final int off, final int len) {
        flush();
        for (int done = 0; done < len;) {
            final int n = Math.min(len - done, BUFFER_SIZE);
            reverseBits(buf, off + done, n);
            crc32.update(buffer, 0, n);
            done += n;
        }
    }

    void update(final int inCh) {
        if (buffered == BUFFER_SIZE) {
            flush();
        }
        buffer[buffered++] = REVERSE[inCh & 0xff];
    }

    /**
     * Updates the CRC with {@code repeat} copies of a byte.
     */
    void update(final int inCh, int repeat) {
        final byte reflected = REVERSE[inCh & 0xff];
        while (repeat > 0) {
            if (buffered == BUFFER_SIZE) {
                flush();
            }
            final int n = Math.min(repeat, BUFFER_SIZE - buffered);
            Arrays.fill(buffer, buffered, buffered + n, reflected);
            buffered += n;
            repeat -= n;
        }
    }
}
