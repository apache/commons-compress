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

import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferUnderflowException;
import java.nio.channels.ReadableByteChannel;
import java.util.Random;

import static org.junit.Assert.*;

public class HeaderChannelBufferTest {
    @Test
    public void testPagedArrayTransfer() throws Exception {
        byte[] data = new byte[1_000_000];
        byte[] output = new byte[1_000_000];
        new Random(0xabadfeed).nextBytes(data);
        SeekableInMemoryByteChannel channel = new SeekableInMemoryByteChannel(data);
        HeaderBuffer headerBuffer = HeaderChannelBuffer.create(channel, data.length, 113);
        headerBuffer.get(output);
        assertArrayEquals(data, output);
    }

    @Test
    public void testNoMaxPageSize() throws Exception {
        SeekableInMemoryByteChannel channel = new SeekableInMemoryByteChannel(new byte[50]);
        HeaderBuffer headerBuffer = HeaderChannelBuffer.create(channel, 50);
        assertTrue(headerBuffer.hasCRC());
    }

    @Test
    public void testGetElementsFromPagedChannel() throws Exception {
        byte[] data = {(byte) 255, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 5, 6, 7, 8};
        SeekableInMemoryByteChannel channel = new SeekableInMemoryByteChannel(data);
        HeaderBuffer headerBuffer = HeaderChannelBuffer.create(channel, data.length, 17);
        assertEquals(255, headerBuffer.getUnsignedByte());
        assertEquals(0x04030201, headerBuffer.getInt());
        assertEquals(0x0403020104030201L, headerBuffer.getLong());
        assertEquals(0x0807060504030201L, headerBuffer.getLong());
    }

    @Test
    public void testGetElementsFromPagedStream() throws Exception {
        byte[] data = {1, 2, 3, 4, (byte) 255, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 5, 6, 7, 8};
        InputStream inputStream = new ByteArrayInputStream(data);
        HeaderBuffer headerBuffer = HeaderChannelBuffer.create(inputStream, data.length, 11);
        assertEquals(0x04030201, headerBuffer.getInt());
        assertEquals(255, headerBuffer.getUnsignedByte());
        assertEquals(0x0403020104030201L, headerBuffer.getLong());
        assertEquals(0x0807060504030201L, headerBuffer.getLong());
    }

    @Test
    public void testSkipWithinPage() throws Exception {
        byte[] data = {(byte) 255, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 5, 6, 7, 8};
        SeekableInMemoryByteChannel channel = new SeekableInMemoryByteChannel(data);
        HeaderBuffer headerBuffer = HeaderChannelBuffer.create(channel, data.length, 11);
        long skipped = headerBuffer.skipBytesFully(7);
        assertEquals(7, skipped);
        assertEquals(0x0201040302010403L, headerBuffer.getLong());
    }

    @Test
    public void testSkipOutsidePage() throws Exception {
        byte[] data = {(byte) 255, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 5, 6, 7, 8};
        SeekableInMemoryByteChannel channel = new SeekableInMemoryByteChannel(data);
        HeaderBuffer headerBuffer = HeaderChannelBuffer.create(channel, data.length, 11);
        long skipped = headerBuffer.skipBytesFully(13);
        assertEquals(13, skipped);
        assertEquals(0x0807060504030201L, headerBuffer.getLong());
    }

    @Test
    public void testSkipOutsideCapacity() throws Exception {
        byte[] data = {(byte) 255, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 5, 6, 7, 8};
        SeekableInMemoryByteChannel channel = new SeekableInMemoryByteChannel(data);
        HeaderBuffer headerBuffer = HeaderChannelBuffer.create(channel, data.length, 11);
        long skipped = headerBuffer.skipBytesFully(100);
        assertEquals(21, skipped);
    }

    @Test
    public void testSkipNothing() throws Exception {
        SeekableInMemoryByteChannel channel = new SeekableInMemoryByteChannel(new byte[50]);
        HeaderBuffer headerBuffer = HeaderChannelBuffer.create(channel, 50, 25);
        headerBuffer.get(new byte[50]);
        assertEquals(0, headerBuffer.skipBytesFully(20));
    }

    @Test
    public void testSkipNegative() throws Exception {
        SeekableInMemoryByteChannel channel = new SeekableInMemoryByteChannel(new byte[50]);
        HeaderBuffer headerBuffer = HeaderChannelBuffer.create(channel, 50, 25);
        assertEquals(0, headerBuffer.skipBytesFully(-1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidPageBytes() throws IOException {
        HeaderChannelBuffer.create((ReadableByteChannel) null, 1000, 7);
    }

    @Test(expected = EOFException.class)
    public void testCapacityTooLarge() throws Exception {
        InputStream inputStream = new ByteArrayInputStream(new byte[20]);
        HeaderChannelBuffer.create(inputStream, 100, 100);
    }

    @Test(expected = EOFException.class)
    public void testCapacityTooLargeSmallPage() throws Exception {
        InputStream inputStream = new ByteArrayInputStream(new byte[20]);
        HeaderBuffer headerBuffer = HeaderChannelBuffer.create(inputStream, 100, 10);
        headerBuffer.get(new byte[100]);
    }

    @Test(expected = BufferUnderflowException.class)
    public void testTransferTooMuch() throws Exception {
        SeekableInMemoryByteChannel channel = new SeekableInMemoryByteChannel(new byte[100]);
        HeaderBuffer headerBuffer = HeaderChannelBuffer.create(channel, 50, 20);
        headerBuffer.get(new byte[100]);
    }

    @Test(expected = BufferUnderflowException.class)
    public void testGetTooMuch() throws Exception {
        SeekableInMemoryByteChannel channel = new SeekableInMemoryByteChannel(new byte[100]);
        HeaderBuffer headerBuffer = HeaderChannelBuffer.create(channel, 50, 20);
        headerBuffer.get(new byte[50]);
        headerBuffer.getInt();
    }

    @Test
    public void testHasCrcInMemory() throws IOException {
        byte[] data = {1, 2, 3, 4, 1, 2, 3, 4, 5, 6, 7, 8};
        InputStream inputStream = new ByteArrayInputStream(data);
        HeaderBuffer headerBuffer = HeaderChannelBuffer.create(inputStream, data.length);
        assertTrue(headerBuffer.hasCRC());
        assertEquals(0x542F8E62, headerBuffer.getCRC().getValue());
    }

    @Test
    public void testNoCrcInPaged() throws IOException {
        InputStream inputStream = new ByteArrayInputStream(new byte[50]);
        HeaderBuffer headerBuffer = HeaderChannelBuffer.create(inputStream, 50, 20);
        assertFalse(headerBuffer.hasCRC());
    }

    @Test(expected = IOException.class)
    public void testGetCrcInPaged() throws IOException {
        InputStream inputStream = new ByteArrayInputStream(new byte[50]);
        HeaderBuffer headerBuffer = HeaderChannelBuffer.create(inputStream, 50, 20);
        headerBuffer.getCRC();
    }
}