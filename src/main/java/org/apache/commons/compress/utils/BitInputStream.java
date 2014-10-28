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
package org.apache.commons.compress.utils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;

/**
 * Reads bits from an InputStream.
 * @since 1.10
 * @NotThreadSafe
 */
public class BitInputStream implements Closeable {
    private final InputStream in;
    private final ByteOrder byteOrder;
    private int bitsCached = 0;
    private int bitsCachedSize = 0;

    /**
     * Constructor taking an InputStream and its bit arrangement. 
     * @param in the InputStream
     * @param byteOrder the bit arrangement across byte boundaries,
     *      either BIG_ENDIAN (aaaaabbb bb000000) or LITTLE_ENDIAN (bbbaaaaa 000000bb)
     */
    public BitInputStream(final InputStream in, final ByteOrder byteOrder) {
        this.in = in;
        this.byteOrder = byteOrder;
    }
    
    public void close() throws IOException {
        in.close();
    }
    
    public void clearBitCache() {
        bitsCached = 0;
        bitsCachedSize = 0;
    }
    
    public int readBits(final int count) throws IOException {
        while (bitsCachedSize < count) {
            final int nextByte = in.read();
            if (nextByte < 0) {
                return nextByte;
            }
            if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                bitsCached |= (nextByte << bitsCachedSize);
            } else {
                bitsCached <<= 8;
                bitsCached |= nextByte;
            }
            bitsCachedSize += 8;
        }
        
        final int mask = (1 << count) - 1;
        final int bitsOut;
        if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
            bitsOut = (bitsCached & mask);
            bitsCached >>>= count;
        } else {
            bitsOut = (bitsCached >> (bitsCachedSize - count)) & mask;
        }
        bitsCachedSize -= count;
        return bitsOut;
    }
}
