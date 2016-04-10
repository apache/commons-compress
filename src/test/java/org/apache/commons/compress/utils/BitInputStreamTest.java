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

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteOrder;

import org.junit.Test;

public class BitInputStreamTest {

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAllowReadingOfANegativeAmountOfBits() throws IOException {
        final BitInputStream bis = new BitInputStream(getStream(), ByteOrder.LITTLE_ENDIAN);
        bis.readBits(-1);
        bis.close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAllowReadingOfMoreThan63BitsAtATime() throws IOException {
        final BitInputStream bis = new BitInputStream(getStream(), ByteOrder.LITTLE_ENDIAN);
        bis.readBits(64);
        bis.close();
    }

    @Test
    public void testReading24BitsInLittleEndian() throws IOException {
        final BitInputStream bis = new BitInputStream(getStream(), ByteOrder.LITTLE_ENDIAN);
        assertEquals(0x000140f8, bis.readBits(24));
        bis.close();
    }

    @Test
    public void testReading24BitsInBigEndian() throws IOException {
        final BitInputStream bis = new BitInputStream(getStream(), ByteOrder.BIG_ENDIAN);
        assertEquals(0x00f84001, bis.readBits(24));
        bis.close();
    }

    @Test
    public void testReading17BitsInLittleEndian() throws IOException {
        final BitInputStream bis = new BitInputStream(getStream(), ByteOrder.LITTLE_ENDIAN);
        assertEquals(0x000140f8, bis.readBits(17));
        bis.close();
    }

    @Test
    public void testReading17BitsInBigEndian() throws IOException {
        final BitInputStream bis = new BitInputStream(getStream(), ByteOrder.BIG_ENDIAN);
        // 1-11110000-10000000
        assertEquals(0x0001f080, bis.readBits(17));
        bis.close();
    }

    @Test
    public void testReading30BitsInLittleEndian() throws IOException {
        final BitInputStream bis = new BitInputStream(getStream(), ByteOrder.LITTLE_ENDIAN);
        assertEquals(0x2f0140f8, bis.readBits(30));
        bis.close();
    }

    @Test
    public void testReading30BitsInBigEndian() throws IOException {
        final BitInputStream bis = new BitInputStream(getStream(), ByteOrder.BIG_ENDIAN);
        // 111110-00010000-00000000-01001011
        assertEquals(0x3e10004b, bis.readBits(30));
        bis.close();
    }

    @Test
    public void testReading31BitsInLittleEndian() throws IOException {
        final BitInputStream bis = new BitInputStream(getStream(), ByteOrder.LITTLE_ENDIAN);
        assertEquals(0x2f0140f8, bis.readBits(31));
        bis.close();
    }

    @Test
    public void testReading31BitsInBigEndian() throws IOException {
        final BitInputStream bis = new BitInputStream(getStream(), ByteOrder.BIG_ENDIAN);
        // 1111100-00100000-00000000-10010111
        assertEquals(0x7c200097, bis.readBits(31));
        bis.close();
    }

    @Test
    public void testClearBitCache() throws IOException {
        final BitInputStream bis = new BitInputStream(getStream(), ByteOrder.LITTLE_ENDIAN);
        assertEquals(0x08, bis.readBits(4));
        bis.clearBitCache();
        assertEquals(0, bis.readBits(1));
        bis.close();
    }

    @Test
    public void testEOF() throws IOException {
        final BitInputStream bis = new BitInputStream(getStream(), ByteOrder.LITTLE_ENDIAN);
        assertEquals(0x2f0140f8, bis.readBits(30));
        assertEquals(-1, bis.readBits(3));
        bis.close();
    }

    private ByteArrayInputStream getStream() {
        return new ByteArrayInputStream(new byte[] {
                (byte) 0xF8,  // 11111000
                0x40,         // 01000000
                0x01,         // 00000001
                0x2F });      // 00101111
    }

}
