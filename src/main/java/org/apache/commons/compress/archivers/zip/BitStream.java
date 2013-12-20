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

package org.apache.commons.compress.archivers.zip;

import java.io.IOException;
import java.io.InputStream;

/**
 * Iterates over the bits of an InputStream. For each byte the bits
 * are read from the right to the left.
 *
 * @since 1.7
 */
class BitStream {

    private final InputStream in;

    /** The bits read from the underlying stream but not consumed by nextBits() */
    private long bitCache;

    /** The number of bits available in the bit cache */
    private int bitCacheSize;

    /** Bit masks for extracting the right most bits from a byte */
    private static final int[] MASKS = new int[]{ 
            0x00, // 00000000
            0x01, // 00000001
            0x03, // 00000011
            0x07, // 00000111
            0x0F, // 00001111
            0x1F, // 00011111
            0x3F, // 00111111
            0x7F, // 01111111
            0xFF  // 11111111
    };

    BitStream(InputStream in) {
        this.in = in;
    }

    private boolean fillCache() throws IOException {
        boolean filled = false;
        
        while (bitCacheSize <= 56) {
            long nextByte = in.read();
            if (nextByte == -1) {
                break;
            }
            
            filled = true;
            bitCache = bitCache | (nextByte << bitCacheSize);
            bitCacheSize += 8;
        }

        return filled;
    }

    /**
     * Returns the next bit.
     * 
     * @return The next bit (0 or 1) or -1 if the end of the stream has been reached
     */
    int nextBit() throws IOException {
        if (bitCacheSize == 0 && !fillCache()) {
            return -1;
        }

        int bit = (int) (bitCache & 1); // extract the right most bit

        bitCache = (bitCache >>> 1); // shift the remaning bits to the right
        bitCacheSize--;

        return bit;
    }

    /**
     * Returns the integer value formed by the n next bits (up to 8 bits).
     *
     * @param n the number of bits read (up to 8)
     * @return The value formed by the n bits, or -1 if the end of the stream has been reached
     */
    int nextBits(final int n) throws IOException {
        if (bitCacheSize < n && !fillCache()) {
            return -1;
        }

        final int bits = (int) (bitCache & MASKS[n]); // extract the right most bits

        bitCache = (bitCache >>> n); // shift the remaning bits to the right
        bitCacheSize = bitCacheSize - n;

        return bits;
    }

    int nextByte() throws IOException {
        return nextBits(8);
    }
}
