/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.commons.compress.archivers.zip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;

/**
 * JUnit tests for org.apache.commons.compress.archivers.zip.ZipEightByteInteger.
 *
 */
public class ZipEightByteIntegerTest {

    /**
     * Test conversion from bytes.
     */
    @Test
    public void testBIFromBytes() {
        final byte[] val = {(byte) 0xFE, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
        final ZipEightByteInteger zl = new ZipEightByteInteger(val);
        assertEquals(BigInteger.valueOf(Long.MAX_VALUE).shiftLeft(1),
                zl.getValue(),
                "value from bytes");
    }

    /**
     * Test conversion to bytes.
     */
    @Test
    public void testBIToBytes() {
        final ZipEightByteInteger zl =
            new ZipEightByteInteger(BigInteger.valueOf(Long.MAX_VALUE)
                                    .shiftLeft(1));
        final byte[] result = zl.getBytes();
        assertEquals(8, result.length, "length getBytes");
        assertEquals((byte) 0xFE, result[0], "first byte getBytes");
        assertEquals((byte) 0xFF, result[1], "second byte getBytes");
        assertEquals((byte) 0xFF, result[2], "third byte getBytes");
        assertEquals((byte) 0xFF, result[3], "fourth byte getBytes");
        assertEquals((byte) 0xFF, result[4], "fifth byte getBytes");
        assertEquals((byte) 0xFF, result[5], "sixth byte getBytes");
        assertEquals((byte) 0xFF, result[6], "seventh byte getBytes");
        assertEquals((byte) 0xFF, result[7], "eighth byte getBytes");
    }

    /**
     * Test the contract of the equals method.
     */
    @Test
    public void testEquals() {
        final ZipEightByteInteger zl = new ZipEightByteInteger(0x12345678);
        final ZipEightByteInteger zl2 = new ZipEightByteInteger(0x12345678);
        final ZipEightByteInteger zl3 = new ZipEightByteInteger(0x87654321);

        assertEquals(zl, zl, "reflexive");

        assertEquals(zl, zl2, "works");
        assertNotEquals(zl, zl3, "works, part two");

        assertEquals(zl2, zl, "symmetric");

        assertNotEquals(null, zl, "null handling");
        assertNotEquals(zl, Integer.valueOf(0x1234), "non ZipEightByteInteger handling");
    }

    /**
     * Test conversion from bytes.
     */
    @Test
    public void testLongFromBytes() {
        final byte[] val = {0x78, 0x56, 0x34, 0x12, (byte) 0xAB, 0x00, 0x00, 0x00};
        final ZipEightByteInteger zl = new ZipEightByteInteger(val);
        assertEquals(0xAB12345678L, zl.getLongValue(), "longValue from bytes");
    }

    /**
     * Test conversion to bytes.
     */
    @Test
    public void testLongToBytes() {
        final ZipEightByteInteger zl = new ZipEightByteInteger(0xAB12345678L);
        final byte[] result = zl.getBytes();
        assertEquals(8, result.length, "length getBytes");
        assertEquals(0x78, result[0], "first byte getBytes");
        assertEquals(0x56, result[1], "second byte getBytes");
        assertEquals(0x34, result[2], "third byte getBytes");
        assertEquals(0x12, result[3], "fourth byte getBytes");
        assertEquals((byte) 0xAB, result[4], "fifth byte getBytes");
        assertEquals(0, result[5], "sixth byte getBytes");
        assertEquals(0, result[6], "seventh byte getBytes");
        assertEquals(0, result[7], "eighth byte getBytes");
    }

    /**
     * Test sign handling.
     */
    @Test
    public void testSign() {
        final ZipEightByteInteger zl = new ZipEightByteInteger(new byte[] {(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF});
        assertEquals(BigInteger.valueOf(Long.MAX_VALUE).shiftLeft(1).setBit(0),
                     zl.getValue());
    }
}
