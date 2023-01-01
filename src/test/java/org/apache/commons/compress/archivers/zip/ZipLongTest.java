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
import static org.junit.jupiter.api.Assertions.assertNotSame;

import org.junit.jupiter.api.Test;

/**
 * JUnit tests for org.apache.commons.compress.archivers.zip.ZipLong.
 *
 */
public class ZipLongTest {

    @Test
    public void testClone() {
        final ZipLong s1 = new ZipLong(42);
        final ZipLong s2 = (ZipLong) s1.clone();
        assertNotSame(s1, s2);
        assertEquals(s1, s2);
        assertEquals(s1.getValue(), s2.getValue());
    }

    /**
     * Test the contract of the equals method.
     */
    @Test
    public void testEquals() {
        final ZipLong zl = new ZipLong(0x12345678);
        final ZipLong zl2 = new ZipLong(0x12345678);
        final ZipLong zl3 = new ZipLong(0x87654321);

        assertEquals(zl, zl, "reflexive");

        assertEquals(zl, zl2, "works");
        assertNotEquals(zl, zl3, "works, part two");

        assertEquals(zl2, zl, "symmetric");

        assertNotEquals(null, zl, "null handling");
        assertNotEquals(zl, Integer.valueOf(0x1234), "non ZipLong handling");
    }

    /**
     * Test conversion from bytes.
     */
    @Test
    public void testFromBytes() {
        final byte[] val = {0x78, 0x56, 0x34, 0x12};
        final ZipLong zl = new ZipLong(val);
        assertEquals(0x12345678, zl.getValue(), "value from bytes");
    }

    /**
     * Test conversion to bytes.
     */
    @Test
    public void testPut() {
        final byte[] arr = new byte[5];
        ZipLong.putLong(0x12345678, arr, 1);
        assertEquals(0x78, arr[1], "first byte getBytes");
        assertEquals(0x56, arr[2], "second byte getBytes");
        assertEquals(0x34, arr[3], "third byte getBytes");
        assertEquals(0x12, arr[4], "fourth byte getBytes");
    }

    /**
     * Test sign handling.
     */
    @Test
    public void testSign() {
         ZipLong zl = new ZipLong(new byte[] {(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF});
        assertEquals(0x00000000FFFFFFFFL, zl.getValue());
        assertEquals(-1,zl.getIntValue());

        zl = new ZipLong(0xFFFF_FFFFL);
        assertEquals(0x00000000FFFFFFFFL, zl.getValue());
        zl = new ZipLong(0xFFFF_FFFF);
        assertEquals(0xFFFF_FFFF_FFFF_FFFFL, zl.getValue());

    }

    /**
     * Test conversion to bytes.
     */
    @Test
    public void testToBytes() {
        final ZipLong zl = new ZipLong(0x12345678);
        final byte[] result = zl.getBytes();
        assertEquals(4, result.length, "length getBytes");
        assertEquals(0x78, result[0], "first byte getBytes");
        assertEquals(0x56, result[1], "second byte getBytes");
        assertEquals(0x34, result[2], "third byte getBytes");
        assertEquals(0x12, result[3], "fourth byte getBytes");
    }
}
