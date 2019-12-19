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

import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.Charset;
import java.util.Arrays;

import static org.apache.commons.compress.utils.CharsetNames.UTF_8;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class SeekableInMemoryByteChannelTest {

    private final byte[] testData = "Some data".getBytes(Charset.forName(UTF_8));

    @Test
    public void shouldReadContentsProperly() throws IOException {
        //given
        SeekableInMemoryByteChannel c = new SeekableInMemoryByteChannel(testData);
        ByteBuffer readBuffer = ByteBuffer.allocate(testData.length);
        //when
        int readCount = c.read(readBuffer);
        //then
        assertEquals(testData.length, readCount);
        assertArrayEquals(testData, readBuffer.array());
        assertEquals(testData.length, c.position());
        c.close();
    }

    @Test
    public void shouldReadContentsWhenBiggerBufferSupplied() throws IOException {
        //given
        SeekableInMemoryByteChannel c = new SeekableInMemoryByteChannel(testData);
        ByteBuffer readBuffer = ByteBuffer.allocate(testData.length + 1);
        //when
        int readCount = c.read(readBuffer);
        //then
        assertEquals(testData.length, readCount);
        assertArrayEquals(testData, Arrays.copyOf(readBuffer.array(), testData.length));
        assertEquals(testData.length, c.position());
        c.close();
    }

    @Test
    public void shouldReadDataFromSetPosition() throws IOException {
        //given
        SeekableInMemoryByteChannel c = new SeekableInMemoryByteChannel(testData);
        ByteBuffer readBuffer = ByteBuffer.allocate(4);
        //when
        c.position(5L);
        int readCount = c.read(readBuffer);
        //then
        assertEquals(4L, readCount);
        assertEquals("data", new String(readBuffer.array(), Charset.forName(UTF_8)));
        assertEquals(testData.length, c.position());
        c.close();
    }

    @Test
    public void shouldSignalEOFWhenPositionAtTheEnd() throws IOException {
        //given
        SeekableInMemoryByteChannel c = new SeekableInMemoryByteChannel(testData);
        ByteBuffer readBuffer = ByteBuffer.allocate(testData.length);
        //when
        c.position(testData.length + 1);
        int readCount = c.read(readBuffer);
        //then
        assertEquals(0L, readBuffer.position());
        assertEquals(-1, readCount);
        c.close();
    }

    @Test(expected = ClosedChannelException.class)
    public void shouldThrowExceptionOnReadingClosedChannel() throws IOException {
        //given
        SeekableInMemoryByteChannel c = new SeekableInMemoryByteChannel();
        //when
        c.close();
        c.read(ByteBuffer.allocate(1));
    }

    @Test
    public void shouldWriteDataProperly() throws IOException {
        //given
        SeekableInMemoryByteChannel c = new SeekableInMemoryByteChannel();
        ByteBuffer inData = ByteBuffer.wrap(testData);
        //when
        int writeCount = c.write(inData);
        //then
        assertEquals(testData.length, writeCount);
        assertArrayEquals(testData, Arrays.copyOf(c.array(), (int) c.size()));
        assertEquals(testData.length, c.position());
        c.close();
    }

    @Test
    public void shouldWriteDataProperlyAfterPositionSet() throws IOException {
        //given
        SeekableInMemoryByteChannel c = new SeekableInMemoryByteChannel(testData);
        ByteBuffer inData = ByteBuffer.wrap(testData);
        ByteBuffer expectedData = ByteBuffer.allocate(testData.length + 5).put(testData, 0, 5).put(testData);
        //when
        c.position(5L);
        int writeCount = c.write(inData);

        //then
        assertEquals(testData.length, writeCount);
        assertArrayEquals(expectedData.array(), Arrays.copyOf(c.array(), (int) c.size()));
        assertEquals(testData.length + 5, c.position());
        c.close();
    }


    @Test(expected = ClosedChannelException.class)
    public void shouldThrowExceptionOnWritingToClosedChannel() throws IOException {
        //given
        SeekableInMemoryByteChannel c = new SeekableInMemoryByteChannel();
        //when
        c.close();
        c.write(ByteBuffer.allocate(1));
    }

    @Test
    public void shouldTruncateContentsProperly() throws IOException {
        //given
        SeekableInMemoryByteChannel c = new SeekableInMemoryByteChannel(testData);
        //when
        c.truncate(4);
        //then
        byte[] bytes = Arrays.copyOf(c.array(), (int) c.size());
        assertEquals("Some", new String(bytes, Charset.forName(UTF_8)));
        c.close();
    }

    @Test
    public void shouldSetProperPositionOnTruncate() throws IOException {
        // In absence of proper parametrised test cases :(
        long[][] testCases = {
                // { Position, NewSize, ExpectedPosition }

                // Position == Size == NewSize
                // - Size doesn?t change
                // - Position doesn?t change
                { 9L, 9L, 9L },

                // NewSize < Position == Size :
                // - Size changes to NewSize
                // - Position changes to NewSize
                { 4L, 9L, 4L },

                // Position == Size < NewSize :
                // - Size changes to NewSize and you get undefined data between
                // - Position doesn?t change
                { 9L, 12L, 9L },

                // NewSize == Position < Size :
                // - Size changes to NewSize
                // - Position doesn?t change
                { 4L, 4L, 4L },

                // Size < Position == NewSize :
                // - Size changes to NewSize and you get undefined data between
                // - Position doesn?t change
                { 12L, 12L, 12L },

                // Position < Size == NewSize :
                // - Size doesn?t change
                // - Position doesn?t change
                { 4L, 9L, 4L },

                // NewSize == Size < Position :
                // - Size doesn?t change
                // - Position changes to NewSize
                { 12L, 9L, 9L },


                // NewSize < Position < Size :
                // - Size changes to NewSize
                // - Position changes to NewSize
                { 6L, 4L, 4L },

                // Position < NewSize < Size :
                // - Size changes to NewSize
                // - Position doesn?t change
                { 4L, 6L, 4L },

                // Position < Size < NewSize :
                // - Size changes to NewSize and you get undefined data between
                // - Position doesn?t change
                { 4L, 12L, 4L },

                // NewSize < Size < Position :
                // - Size changes to NewSize
                // - Position changes to NewSize
                { 12L, 4L, 4L },

                // Size < NewSize < Position :
                // - Size changes to NewSize and you get undefined data between
                // - Position changes to NewSize
                { 12L, 10L, 10L },

                // Size < Position < NewSize :
                // - Size changes to NewSize and you get undefined data between
                // - Position doesn?t change
                { 10L, 12L, 10L }

        };

        for (long[] testCase : testCases) {
            try {
                long position = testCase[0];
                long newSize = testCase[1];
                long expectedPosition = testCase[2];

                //given
                try (SeekableInMemoryByteChannel c = new SeekableInMemoryByteChannel(testData)) {
                    //when
                    c.position(position);
                    c.truncate(newSize);
                    //then
                    assertEquals(expectedPosition, c.position());
                    assertEquals(newSize, c.size());
                }
            } catch (AssertionError e) {
                throw new AssertionError("Failed at test case: " + Arrays.toString(testCase), e);
            }
        }
    }

    @Test
    public void shouldSetProperPosition() throws IOException {
        //given
        SeekableInMemoryByteChannel c = new SeekableInMemoryByteChannel(testData);
        //when
        long posAtFour = c.position(4L).position();
        long posAtTheEnd = c.position(testData.length).position();
        long posPastTheEnd = c.position(testData.length + 1L).position();
        //then
        assertEquals(4L, posAtFour);
        assertEquals(c.size(), posAtTheEnd);
        assertEquals(posPastTheEnd, posPastTheEnd);
        c.close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenSettingIncorrectPosition() throws IOException {
        //given
        SeekableInMemoryByteChannel c = new SeekableInMemoryByteChannel();
        //when
        c.position(Integer.MAX_VALUE + 1L);
        c.close();
    }

    @Test(expected = ClosedChannelException.class)
    public void shouldThrowExceptionWhenSettingPositionOnClosedChannel() throws IOException {
        //given
        SeekableInMemoryByteChannel c = new SeekableInMemoryByteChannel();
        //when
        c.close();
        c.position(1L);
    }

}
