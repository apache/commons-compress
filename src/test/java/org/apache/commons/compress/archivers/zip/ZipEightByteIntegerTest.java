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

import static org.junit.Assert.*;

import java.math.BigInteger;

import org.junit.Test;

/**
 * JUnit testcases for org.apache.commons.compress.archivers.zip.ZipEightByteInteger.
 *
 */
public class ZipEightByteIntegerTest {

    /**
     * Test conversion to bytes.
     */
    @Test
    public void testLongToBytes() {
        final ZipEightByteInteger zl = new ZipEightByteInteger(0xAB12345678l);
        final byte[] result = zl.getBytes();
        assertEquals("length getBytes", 8, result.length);
        assertEquals("first byte getBytes", 0x78, result[0]);
        assertEquals("second byte getBytes", 0x56, result[1]);
        assertEquals("third byte getBytes", 0x34, result[2]);
        assertEquals("fourth byte getBytes", 0x12, result[3]);
        assertEquals("fifth byte getBytes", (byte) 0xAB, result[4]);
        assertEquals("sixth byte getBytes", 0, result[5]);
        assertEquals("seventh byte getBytes", 0, result[6]);
        assertEquals("eighth byte getBytes", 0, result[7]);
    }

    /**
     * Test conversion from bytes.
     */
    @Test
    public void testLongFromBytes() {
        final byte[] val = new byte[] {0x78, 0x56, 0x34, 0x12, (byte) 0xAB, 0x00, 0x00, 0x00};
        final ZipEightByteInteger zl = new ZipEightByteInteger(val);
        assertEquals("longValue from bytes", 0xAB12345678l, zl.getLongValue());
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
        assertEquals("length getBytes", 8, result.length);
        assertEquals("first byte getBytes", (byte) 0xFE, result[0]);
        assertEquals("second byte getBytes", (byte) 0xFF, result[1]);
        assertEquals("third byte getBytes", (byte) 0xFF, result[2]);
        assertEquals("fourth byte getBytes", (byte) 0xFF, result[3]);
        assertEquals("fifth byte getBytes", (byte) 0xFF, result[4]);
        assertEquals("sixth byte getBytes", (byte) 0xFF, result[5]);
        assertEquals("seventh byte getBytes", (byte) 0xFF, result[6]);
        assertEquals("eighth byte getBytes", (byte) 0xFF, result[7]);
    }

    /**
     * Test conversion from bytes.
     */
    @Test
    public void testBIFromBytes() {
        final byte[] val = new byte[] {(byte) 0xFE, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
        final ZipEightByteInteger zl = new ZipEightByteInteger(val);
        assertEquals("value from bytes",
                     BigInteger.valueOf(Long.MAX_VALUE).shiftLeft(1),
                     zl.getValue());
    }

    /**
     * Test the contract of the equals method.
     */
    @Test
    public void testEquals() {
        final ZipEightByteInteger zl = new ZipEightByteInteger(0x12345678);
        final ZipEightByteInteger zl2 = new ZipEightByteInteger(0x12345678);
        final ZipEightByteInteger zl3 = new ZipEightByteInteger(0x87654321);

        assertTrue("reflexive", zl.equals(zl));

        assertTrue("works", zl.equals(zl2));
        assertTrue("works, part two", !zl.equals(zl3));

        assertTrue("symmetric", zl2.equals(zl));

        assertTrue("null handling", !zl.equals(null));
        assertTrue("non ZipEightByteInteger handling", !zl.equals(new Integer(0x1234)));
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
