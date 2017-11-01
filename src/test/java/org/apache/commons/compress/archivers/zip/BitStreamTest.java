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

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;

import org.junit.Test;

public class BitStreamTest {

    @Test
    public void testEmptyStream() throws Exception {
        final BitStream stream = new BitStream(new ByteArrayInputStream(new byte[0]));
        assertEquals("next bit", -1, stream.nextBit());
        assertEquals("next bit", -1, stream.nextBit());
        assertEquals("next bit", -1, stream.nextBit());
        stream.close();
    }

    @Test
    public void testStream() throws Exception {
        final BitStream stream = new BitStream(new ByteArrayInputStream(new byte[] { (byte) 0xEA, 0x03 }));

        assertEquals("bit 0", 0, stream.nextBit());
        assertEquals("bit 1", 1, stream.nextBit());
        assertEquals("bit 2", 0, stream.nextBit());
        assertEquals("bit 3", 1, stream.nextBit());
        assertEquals("bit 4", 0, stream.nextBit());
        assertEquals("bit 5", 1, stream.nextBit());
        assertEquals("bit 6", 1, stream.nextBit());
        assertEquals("bit 7", 1, stream.nextBit());

        assertEquals("bit 8", 1, stream.nextBit());
        assertEquals("bit 9", 1, stream.nextBit());
        assertEquals("bit 10", 0, stream.nextBit());
        assertEquals("bit 11", 0, stream.nextBit());
        assertEquals("bit 12", 0, stream.nextBit());
        assertEquals("bit 13", 0, stream.nextBit());
        assertEquals("bit 14", 0, stream.nextBit());
        assertEquals("bit 15", 0, stream.nextBit());

        assertEquals("next bit", -1, stream.nextBit());
        stream.close();
    }

    @Test
    public void testNextByteFromEmptyStream() throws Exception {
        final BitStream stream = new BitStream(new ByteArrayInputStream(new byte[0]));
        assertEquals("next byte", -1, stream.nextByte());
        assertEquals("next byte", -1, stream.nextByte());
        stream.close();
    }

    @Test
    public void testReadAlignedBytes() throws Exception {
        final BitStream stream = new BitStream(new ByteArrayInputStream(new byte[] { (byte) 0xEA, 0x35 }));
        assertEquals("next byte", 0xEA, stream.nextByte());
        assertEquals("next byte", 0x35, stream.nextByte());
        assertEquals("next byte", -1, stream.nextByte());
        stream.close();
    }

    @Test
    public void testNextByte() throws Exception {
        final BitStream stream = new BitStream(new ByteArrayInputStream(new byte[] { (byte) 0xEA, 0x35 }));
        assertEquals("bit 0", 0, stream.nextBit());
        assertEquals("bit 1", 1, stream.nextBit());
        assertEquals("bit 2", 0, stream.nextBit());
        assertEquals("bit 3", 1, stream.nextBit());

        assertEquals("next byte", 0x5E, stream.nextByte());
        assertEquals("next byte", -1, stream.nextByte()); // not enough bits left to read a byte
        stream.close();
    }
}
