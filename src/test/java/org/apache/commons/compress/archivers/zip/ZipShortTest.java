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
 * JUnit tests for org.apache.commons.compress.archivers.zip.ZipShort.
 *
 */
public class ZipShortTest {

    @Test
    public void testClone() {
        final ZipShort s1 = new ZipShort(42);
        final ZipShort s2 = (ZipShort) s1.clone();
        assertNotSame(s1, s2);
        assertEquals(s1, s2);
        assertEquals(s1.getValue(), s2.getValue());
    }


    /**
     * Test the contract of the equals method.
     */
    @Test
    public void testEquals() {
        final ZipShort zs = new ZipShort(0x1234);
        final ZipShort zs2 = new ZipShort(0x1234);
        final ZipShort zs3 = new ZipShort(0x5678);

        assertEquals(zs, zs, "reflexive");

        assertEquals(zs, zs2, "works");
        assertNotEquals(zs, zs3, "works, part two");

        assertEquals(zs2, zs, "symmetric");

        assertNotEquals(null, zs, "null handling");
        assertNotEquals(zs, Integer.valueOf(0x1234), "non ZipShort handling");
    }


    /**
     * Test conversion from bytes.
     */
    @Test
    public void testFromBytes() {
        final byte[] val = {0x34, 0x12};
        final ZipShort zs = new ZipShort(val);
        assertEquals(0x1234, zs.getValue(), "value from bytes");
    }

    /**
     * Test conversion to bytes.
     */
    @Test
    public void testPut() {
        final byte[] arr = new byte[3];
        ZipShort.putShort(0x1234, arr, 1);
        assertEquals(0x34, arr[1], "first byte getBytes");
        assertEquals(0x12, arr[2], "second byte getBytes");
    }

    /**
     * Test sign handling.
     */
    @Test
    public void testSign() {
        final ZipShort zs = new ZipShort(new byte[] {(byte)0xFF, (byte)0xFF});
        assertEquals(0x0000FFFF, zs.getValue());
    }

    /**
     * Test conversion to bytes.
     */
    @Test
    public void testToBytes() {
        final ZipShort zs = new ZipShort(0x1234);
        final byte[] result = zs.getBytes();
        assertEquals(2, result.length, "length getBytes");
        assertEquals(0x34, result[0], "first byte getBytes");
        assertEquals(0x12, result[1], "second byte getBytes");
    }
}
