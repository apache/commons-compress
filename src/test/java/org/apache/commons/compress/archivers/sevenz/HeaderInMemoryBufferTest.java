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
package org.apache.commons.compress.archivers.sevenz;

import org.junit.Test;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;

import static org.junit.Assert.*;

public class HeaderInMemoryBufferTest {
    @Test
    public void testArrayTransfer() throws Exception {
        byte[] data = new byte[1_000_000];
        byte[] output = new byte[1_000_000];
        new Random(0xabadfeed).nextBytes(data);
        ByteBuffer buffer = ByteBuffer.wrap(data);
        HeaderBuffer headerBuffer = new HeaderInMemoryBuffer(buffer);
        headerBuffer.get(output);
        assertArrayEquals(data, output);
    }

    @Test
    public void testGetElements() throws Exception {
        byte[] data = {(byte) 255, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 5, 6, 7, 8};
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        HeaderBuffer headerBuffer = new HeaderInMemoryBuffer(buffer);
        assertEquals(255, headerBuffer.getUnsignedByte());
        assertEquals(0x04030201, headerBuffer.getInt());
        assertEquals(0x0403020104030201L, headerBuffer.getLong());
        assertEquals(0x0807060504030201L, headerBuffer.getLong());
    }

    @Test
    public void testSkipWithin() throws Exception {
        byte[] data = {(byte) 255, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 5, 6, 7, 8};
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        HeaderBuffer headerBuffer = new HeaderInMemoryBuffer(buffer);
        long skipped = headerBuffer.skipBytesFully(7);
        assertEquals(7, skipped);
        assertEquals(0x0201040302010403L, headerBuffer.getLong());
    }

    @Test
    public void testSkipOutside() throws Exception {
        byte[] data = {(byte) 255, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 5, 6, 7, 8};
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        HeaderBuffer headerBuffer = new HeaderInMemoryBuffer(buffer);
        long skipped = headerBuffer.skipBytesFully(100);
        assertEquals(21, skipped);
    }

    @Test
    public void testSkipExact() throws Exception {
        byte[] data = {(byte) 255, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 5, 6, 7, 8};
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        HeaderBuffer headerBuffer = new HeaderInMemoryBuffer(buffer);
        long skipped = headerBuffer.skipBytesFully(21);
        assertEquals(21, skipped);
    }

    @Test
    public void testSkipNegative() throws Exception {
        HeaderBuffer headerBuffer = new HeaderInMemoryBuffer(ByteBuffer.allocate(20));
        assertEquals(0, headerBuffer.skipBytesFully(-1));
    }

    @Test
    public void testCrc() throws IOException {
        byte[] data = {1, 2, 3, 4, 1, 2, 3, 4, 5, 6, 7, 8};
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        HeaderBuffer headerBuffer = new HeaderInMemoryBuffer(buffer);
        assertTrue(headerBuffer.hasCRC());
        assertEquals(0x542F8E62, headerBuffer.getCRC().getValue());
    }

    @Test(expected = BufferUnderflowException.class)
    public void testTransferTooMuch() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(20);
        HeaderBuffer headerBuffer = new HeaderInMemoryBuffer(buffer);
        headerBuffer.get(new byte[100]);
    }

    @Test(expected = BufferUnderflowException.class)
    public void testGetTooMuch() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(20);
        HeaderBuffer headerBuffer = new HeaderInMemoryBuffer(buffer);
        headerBuffer.get(new byte[20]);
        headerBuffer.getInt();
    }
}