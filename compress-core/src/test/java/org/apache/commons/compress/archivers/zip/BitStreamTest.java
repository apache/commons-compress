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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;

import org.apache.commons.compress.utils.ByteUtils;
import org.junit.jupiter.api.Test;

public class BitStreamTest {

    @Test
    public void testEmptyStream() throws Exception {
        try (BitStream stream = new BitStream(new ByteArrayInputStream(ByteUtils.EMPTY_BYTE_ARRAY))) {
            assertEquals(-1, stream.nextBit(), "next bit");
            assertEquals(-1, stream.nextBit(), "next bit");
            assertEquals(-1, stream.nextBit(), "next bit");
        }
    }

    @Test
    public void testNextByte() throws Exception {
        try (BitStream stream = new BitStream(new ByteArrayInputStream(new byte[] { (byte) 0xEA, 0x35 }))) {
            assertEquals(0, stream.nextBit(), "bit 0");
            assertEquals(1, stream.nextBit(), "bit 1");
            assertEquals(0, stream.nextBit(), "bit 2");
            assertEquals(1, stream.nextBit(), "bit 3");

            assertEquals(0x5E, stream.nextByte(), "next byte");
            assertEquals(-1, stream.nextByte(), "next byte"); // not enough bits left to read a byte
        }
    }

    @Test
    public void testNextByteFromEmptyStream() throws Exception {
        try (BitStream stream = new BitStream(new ByteArrayInputStream(ByteUtils.EMPTY_BYTE_ARRAY))) {
            assertEquals(-1, stream.nextByte(), "next byte");
            assertEquals(-1, stream.nextByte(), "next byte");
        }
    }

    @Test
    public void testReadAlignedBytes() throws Exception {
        try (BitStream stream = new BitStream(new ByteArrayInputStream(new byte[] { (byte) 0xEA, 0x35 }))) {
            assertEquals(0xEA, stream.nextByte(), "next byte");
            assertEquals(0x35, stream.nextByte(), "next byte");
            assertEquals(-1, stream.nextByte(), "next byte");
        }
    }

    @Test
    public void testStream() throws Exception {
        try (BitStream stream = new BitStream(new ByteArrayInputStream(new byte[] { (byte) 0xEA, 0x03 }))) {

            assertEquals(0, stream.nextBit(), "bit 0");
            assertEquals(1, stream.nextBit(), "bit 1");
            assertEquals(0, stream.nextBit(), "bit 2");
            assertEquals(1, stream.nextBit(), "bit 3");
            assertEquals(0, stream.nextBit(), "bit 4");
            assertEquals(1, stream.nextBit(), "bit 5");
            assertEquals(1, stream.nextBit(), "bit 6");
            assertEquals(1, stream.nextBit(), "bit 7");

            assertEquals(1, stream.nextBit(), "bit 8");
            assertEquals(1, stream.nextBit(), "bit 9");
            assertEquals(0, stream.nextBit(), "bit 10");
            assertEquals(0, stream.nextBit(), "bit 11");
            assertEquals(0, stream.nextBit(), "bit 12");
            assertEquals(0, stream.nextBit(), "bit 13");
            assertEquals(0, stream.nextBit(), "bit 14");
            assertEquals(0, stream.nextBit(), "bit 15");

            assertEquals(-1, stream.nextBit(), "next bit");
        }
    }
}
