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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteOrder;

import org.junit.jupiter.api.Test;

public class BitInputStreamTest {

    @Test
    public void alignWithByteBoundaryWhenAtBoundary() throws Exception {
        try (final BitInputStream bis = new BitInputStream(getStream(), ByteOrder.LITTLE_ENDIAN)) {
            assertEquals(0xF8, bis.readBits(8));
            bis.alignWithByteBoundary();
            assertEquals(0, bis.readBits(4));
        }
    }

    @Test
    public void alignWithByteBoundaryWhenNotAtBoundary() throws Exception {
        try (final BitInputStream bis = new BitInputStream(getStream(), ByteOrder.LITTLE_ENDIAN)) {
            assertEquals(0x08, bis.readBits(4));
            assertEquals(4, bis.bitsCached());
            bis.alignWithByteBoundary();
            assertEquals(0, bis.bitsCached());
            assertEquals(0, bis.readBits(4));
        }
    }

    @Test
    public void availableWithCache() throws Exception {
        try (final BitInputStream bis = new BitInputStream(getStream(), ByteOrder.LITTLE_ENDIAN)) {
            assertEquals(0x08, bis.readBits(4));
            assertEquals(28, bis.bitsAvailable());
        }
    }

    @Test
    public void availableWithoutCache() throws Exception {
        try (final BitInputStream bis = new BitInputStream(getStream(), ByteOrder.LITTLE_ENDIAN)) {
            assertEquals(32, bis.bitsAvailable());
        }
    }

    @Test
    public void bigEndianWithOverflow() throws Exception {
        final ByteArrayInputStream in = new ByteArrayInputStream(new byte[] {
                87, // 01010111
                45, // 00101101
                66, // 01000010
                15, // 00001111
                90, // 01011010
                29, // 00011101
                88, // 01011000
                61, // 00111101
                33, // 00100001
                74  // 01001010
            });
        try (final BitInputStream bin = new BitInputStream(in, ByteOrder.BIG_ENDIAN)) {
            assertEquals(10, // 01010
                         bin.readBits(5));
            assertEquals(8274274654740644818L, //111-00101101-01000010-00001111-01011010-00011101-01011000-00111101-0010
                    bin.readBits(63));
            assertEquals(330, // 0001-01001010
                         bin.readBits(12));
            assertEquals(-1 , bin.readBits(1));
        }
    }

    private ByteArrayInputStream getStream() {
        return new ByteArrayInputStream(new byte[] {
                (byte) 0xF8,  // 11111000
                0x40,         // 01000000
                0x01,         // 00000001
                0x2F });      // 00101111
    }

    /**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-363"
     */
    @Test
    public void littleEndianWithOverflow() throws Exception {
        final ByteArrayInputStream in = new ByteArrayInputStream(new byte[] {
                87, // 01010111
                45, // 00101101
                66, // 01000010
                15, // 00001111
                90, // 01011010
                29, // 00011101
                88, // 01011000
                61, // 00111101
                33, // 00100001
                74  // 01001010
            });
        try (final BitInputStream bin = new BitInputStream(in, ByteOrder.LITTLE_ENDIAN)) {
            assertEquals(23, // 10111
                         bin.readBits(5));
            assertEquals(714595605644185962L, // 0001-00111101-01011000-00011101-01011010-00001111-01000010-00101101-010
                         bin.readBits(63));
            assertEquals(1186, // 01001010-0010
                         bin.readBits(12));
            assertEquals(-1 , bin.readBits(1));
        }
    }

    @Test
    public void shouldNotAllowReadingOfANegativeAmountOfBits() throws IOException {
        try (final BitInputStream bis = new BitInputStream(getStream(), ByteOrder.LITTLE_ENDIAN)) {
            assertThrows(IOException.class, () -> bis.readBits(-1));
        }
    }

    @Test
    public void shouldNotAllowReadingOfMoreThan63BitsAtATime() throws IOException {
        try (final BitInputStream bis = new BitInputStream(getStream(), ByteOrder.LITTLE_ENDIAN)) {
            assertThrows(IOException.class, () -> bis.readBits(64));
        }
    }

    @Test
    public void testClearBitCache() throws IOException {
        try (final BitInputStream bis = new BitInputStream(getStream(), ByteOrder.LITTLE_ENDIAN)) {
            assertEquals(0x08, bis.readBits(4));
            bis.clearBitCache();
            assertEquals(0, bis.readBits(1));
        }
    }

    @Test
    public void testEOF() throws IOException {
        try (final BitInputStream bis = new BitInputStream(getStream(), ByteOrder.LITTLE_ENDIAN)) {
            assertEquals(0x2f0140f8, bis.readBits(30));
            assertEquals(-1, bis.readBits(3));
        }
    }

    @Test
    public void testReading17BitsInBigEndian() throws IOException {
        try (final BitInputStream bis = new BitInputStream(getStream(), ByteOrder.BIG_ENDIAN)) {
            // 1-11110000-10000000
            assertEquals(0x0001f080, bis.readBits(17));
        }
    }

    @Test
    public void testReading17BitsInLittleEndian() throws IOException {
        try (final BitInputStream bis = new BitInputStream(getStream(), ByteOrder.LITTLE_ENDIAN)) {
            assertEquals(0x000140f8, bis.readBits(17));
        }
    }

    @Test
    public void testReading24BitsInBigEndian() throws IOException {
        try (final BitInputStream bis = new BitInputStream(getStream(), ByteOrder.BIG_ENDIAN)) {
            assertEquals(0x00f84001, bis.readBits(24));
        }
    }

    @Test
    public void testReading24BitsInLittleEndian() throws IOException {
        try (final BitInputStream bis = new BitInputStream(getStream(), ByteOrder.LITTLE_ENDIAN)) {
            assertEquals(0x000140f8, bis.readBits(24));
        }
    }

    @Test
    public void testReading30BitsInBigEndian() throws IOException {
        try (final BitInputStream bis = new BitInputStream(getStream(), ByteOrder.BIG_ENDIAN)) {
            // 111110-00010000-00000000-01001011
            assertEquals(0x3e10004b, bis.readBits(30));
        }
    }

    @Test
    public void testReading30BitsInLittleEndian() throws IOException {
        try (final BitInputStream bis = new BitInputStream(getStream(), ByteOrder.LITTLE_ENDIAN)) {
            assertEquals(0x2f0140f8, bis.readBits(30));
        }
    }

    @Test
    public void testReading31BitsInBigEndian() throws IOException {
        try (final BitInputStream bis = new BitInputStream(getStream(), ByteOrder.BIG_ENDIAN)) {
            // 1111100-00100000-00000000-10010111
            assertEquals(0x7c200097, bis.readBits(31));
        }
    }

    @Test
    public void testReading31BitsInLittleEndian() throws IOException {
        try (final BitInputStream bis = new BitInputStream(getStream(), ByteOrder.LITTLE_ENDIAN)) {
            assertEquals(0x2f0140f8, bis.readBits(31));
        }
    }

}
