/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.compress.utils;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SeekableByteChannel;
import java.util.Arrays;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class SeekableInMemoryByteChannelTest {

    private final byte[] testData = "Some data".getBytes(UTF_8);

    /*
     * <q>If the stream is already closed then invoking this method has no effect.</q>
     */
    @Test
    void testCloseIsIdempotent() throws Exception {
        try (SeekableByteChannel c = new SeekableInMemoryByteChannel()) {
            c.close();
            assertFalse(c.isOpen());
            c.close();
            assertFalse(c.isOpen());
        }
    }

    /*
     * <q>Setting the position to a value that is greater than the current size is legal but does not change the size of the entity. A later attempt to read
     * bytes at such a position will immediately return an end-of-file indication</q>
     */
    @Test
    void testReadingFromAPositionAfterEndReturnsEOF() throws Exception {
        try (SeekableByteChannel c = new SeekableInMemoryByteChannel()) {
            c.position(2);
            assertEquals(2, c.position());
            final ByteBuffer readBuffer = ByteBuffer.allocate(5);
            assertEquals(-1, c.read(readBuffer));
        }
    }

    @Test
    void testShouldReadContentsProperly() throws IOException {
        // given
        try (SeekableInMemoryByteChannel c = new SeekableInMemoryByteChannel(testData)) {
            final ByteBuffer readBuffer = ByteBuffer.allocate(testData.length);
            // when
            final int readCount = c.read(readBuffer);
            // then
            assertEquals(testData.length, readCount);
            assertArrayEquals(testData, readBuffer.array());
            assertEquals(testData.length, c.position());
        }
    }

    @Test
    void testShouldReadContentsWhenBiggerBufferSupplied() throws IOException {
        // given
        try (SeekableInMemoryByteChannel c = new SeekableInMemoryByteChannel(testData)) {
            final ByteBuffer readBuffer = ByteBuffer.allocate(testData.length + 1);
            // when
            final int readCount = c.read(readBuffer);
            // then
            assertEquals(testData.length, readCount);
            assertArrayEquals(testData, Arrays.copyOf(readBuffer.array(), testData.length));
            assertEquals(testData.length, c.position());
        }
    }

    @Test
    void testShouldReadDataFromSetPosition() throws IOException {
        // given
        try (SeekableInMemoryByteChannel c = new SeekableInMemoryByteChannel(testData)) {
            final ByteBuffer readBuffer = ByteBuffer.allocate(4);
            // when
            c.position(5L);
            final int readCount = c.read(readBuffer);
            // then
            assertEquals(4L, readCount);
            assertEquals("data", new String(readBuffer.array(), UTF_8));
            assertEquals(testData.length, c.position());
        }
    }

    @Test
    void testShouldSetProperPosition() throws IOException {
        // given
        try (SeekableInMemoryByteChannel c = new SeekableInMemoryByteChannel(testData)) {
            // when
            final long posAtFour = c.position(4L).position();
            final long posAtTheEnd = c.position(testData.length).position();
            final long posPastTheEnd = c.position(testData.length + 1L).position();
            // then
            assertEquals(4L, posAtFour);
            assertEquals(c.size(), posAtTheEnd);
            assertEquals(testData.length + 1L, posPastTheEnd);
        }
    }

    @Test
    void testShouldSetProperPositionOnTruncate() throws IOException {
        // given
        try (SeekableInMemoryByteChannel c = new SeekableInMemoryByteChannel(testData)) {
            // when
            c.position(testData.length);
            c.truncate(4L);
            // then
            assertEquals(4L, c.position());
            assertEquals(4L, c.size());
        }
    }

    @Test
    void testShouldSignalEOFWhenPositionAtTheEnd() throws IOException {
        // given
        try (SeekableInMemoryByteChannel c = new SeekableInMemoryByteChannel(testData)) {
            final ByteBuffer readBuffer = ByteBuffer.allocate(testData.length);
            // when
            c.position(testData.length + 1);
            final int readCount = c.read(readBuffer);
            // then
            assertEquals(0L, readBuffer.position());
            assertEquals(-1, readCount);
            assertEquals(-1, c.read(readBuffer));
        }
    }

    @Test
    void testShouldThrowExceptionOnReadingClosedChannel() {
        // given
        final SeekableInMemoryByteChannel c = new SeekableInMemoryByteChannel();
        // when
        c.close();
        assertThrows(ClosedChannelException.class, () -> c.read(ByteBuffer.allocate(1)));
    }

    @Test
    void testShouldThrowExceptionOnWritingToClosedChannel() {
        // given
        final SeekableInMemoryByteChannel c = new SeekableInMemoryByteChannel();
        // when
        c.close();
        assertThrows(ClosedChannelException.class, () -> c.write(ByteBuffer.allocate(1)));
    }

    @Test
    void testShouldThrowExceptionWhenSettingIncorrectPosition() {
        // given
        try (SeekableInMemoryByteChannel c = new SeekableInMemoryByteChannel()) {
            // when
            assertThrows(IOException.class, () -> c.position(Integer.MAX_VALUE + 1L));
        }
    }

    @Test
    void testShouldThrowExceptionWhenTruncatingToIncorrectSize() {
        // given
        try (SeekableInMemoryByteChannel c = new SeekableInMemoryByteChannel()) {
            // when
            assertThrows(IllegalArgumentException.class, () -> c.truncate(Integer.MAX_VALUE + 1L));
        }
    }

    @Test
    void testShouldTruncateContentsProperly() {
        // given
        try (SeekableInMemoryByteChannel c = new SeekableInMemoryByteChannel(testData)) {
            // when
            c.truncate(4);
            // then
            final byte[] bytes = Arrays.copyOf(c.array(), (int) c.size());
            assertEquals("Some", new String(bytes, UTF_8));
        }
    }

    // Contract Tests added in response to https://issues.apache.org/jira/browse/COMPRESS-499

    // https://docs.oracle.com/javase/8/docs/api/java/io/Closeable.html#close()

    @Test
    void testShouldWriteDataProperly() throws IOException {
        // given
        try (SeekableInMemoryByteChannel c = new SeekableInMemoryByteChannel()) {
            final ByteBuffer inData = ByteBuffer.wrap(testData);
            // when
            final int writeCount = c.write(inData);
            // then
            assertEquals(testData.length, writeCount);
            assertArrayEquals(testData, Arrays.copyOf(c.array(), (int) c.size()));
            assertEquals(testData.length, c.position());
        }
    }

    // https://docs.oracle.com/javase/8/docs/api/java/nio/channels/SeekableByteChannel.html#position()

    @Test
    void testShouldWriteDataProperlyAfterPositionSet() throws IOException {
        // given
        try (SeekableInMemoryByteChannel c = new SeekableInMemoryByteChannel(testData)) {
            final ByteBuffer inData = ByteBuffer.wrap(testData);
            final ByteBuffer expectedData = ByteBuffer.allocate(testData.length + 5).put(testData, 0, 5).put(testData);
            // when
            c.position(5L);
            final int writeCount = c.write(inData);

            // then
            assertEquals(testData.length, writeCount);
            assertArrayEquals(expectedData.array(), Arrays.copyOf(c.array(), (int) c.size()));
            assertEquals(testData.length + 5, c.position());
        }
    }

    // https://docs.oracle.com/javase/8/docs/api/java/nio/channels/SeekableByteChannel.html#size()

    /*
     * <q>ClosedChannelException - If this channel is closed</q>
     */
    @Test
    void testThrowsClosedChannelExceptionWhenPositionIsSetOnClosedChannel() throws Exception {
        try (SeekableByteChannel c = new SeekableInMemoryByteChannel()) {
            c.close();
            assertThrows(ClosedChannelException.class, () -> c.position(0));
        }
    }

    // https://docs.oracle.com/javase/8/docs/api/java/nio/channels/SeekableByteChannel.html#position(long)

    /*
     * <q>IllegalArgumentException - If the new position is negative</q>
     */
    @Test
    void testThrowsIllegalArgumentExceptionWhenTruncatingToANegativeSize() throws Exception {
        try (SeekableByteChannel c = new SeekableInMemoryByteChannel()) {
            assertThrows(IllegalArgumentException.class, () -> c.truncate(-1));
        }
    }

    /*
     * <q>IOException - If the new position is negative</q>
     */
    @Test
    void testThrowsIOExceptionWhenPositionIsSetToANegativeValue() throws Exception {
        try (SeekableByteChannel c = new SeekableInMemoryByteChannel()) {
            assertThrows(IOException.class, () -> c.position(-1));
        }
    }

    /*
     * <q> In either case, if the current position is greater than the given size then it is set to that size.</q>
     */
    @Test
    void testTruncateDoesntChangeSmallPosition() throws Exception {
        try (SeekableByteChannel c = new SeekableInMemoryByteChannel(testData)) {
            c.position(1);
            c.truncate(testData.length - 1);
            assertEquals(testData.length - 1, c.size());
            assertEquals(1, c.position());
        }
    }

    /*
     * <q> In either case, if the current position is greater than the given size then it is set to that size.</q>
     */
    @Test
    void testTruncateMovesPositionWhenNewSizeIsBiggerThanSizeAndPositionIsEvenBigger() throws Exception {
        try (SeekableByteChannel c = new SeekableInMemoryByteChannel(testData)) {
            c.position(2 * testData.length);
            c.truncate(testData.length + 1);
            assertEquals(testData.length, c.size());
            assertEquals(testData.length + 1, c.position());
        }
    }

    // https://docs.oracle.com/javase/8/docs/api/java/nio/channels/SeekableByteChannel.html#truncate(long)

    /*
     * <q> In either case, if the current position is greater than the given size then it is set to that size.</q>
     */
    @Test
    void testTruncateMovesPositionWhenNotResizingButPositionBiggerThanSize() throws Exception {
        try (SeekableByteChannel c = new SeekableInMemoryByteChannel(testData)) {
            c.position(2 * testData.length);
            c.truncate(testData.length);
            assertEquals(testData.length, c.size());
            assertEquals(testData.length, c.position());
        }
    }

    /*
     * <q> In either case, if the current position is greater than the given size then it is set to that size.</q>
     */
    @Test
    void testTruncateMovesPositionWhenShrinkingBeyondPosition() throws Exception {
        try (SeekableByteChannel c = new SeekableInMemoryByteChannel(testData)) {
            c.position(4);
            c.truncate(3);
            assertEquals(3, c.size());
            assertEquals(3, c.position());
        }
    }

    /*
     * <q>If the given size is greater than or equal to the current size then the entity is not modified.</q>
     */
    @Test
    void testTruncateToBiggerSizeDoesntChangeAnything() throws Exception {
        try (SeekableByteChannel c = new SeekableInMemoryByteChannel(testData)) {
            assertEquals(testData.length, c.size());
            c.truncate(testData.length + 1);
            assertEquals(testData.length, c.size());
            final ByteBuffer readBuffer = ByteBuffer.allocate(testData.length);
            assertEquals(testData.length, c.read(readBuffer));
            assertArrayEquals(testData, Arrays.copyOf(readBuffer.array(), testData.length));
        }
    }

    /*
     * <q>If the given size is greater than or equal to the current size then the entity is not modified.</q>
     */
    @Test
    void testTruncateToCurrentSizeDoesntChangeAnything() throws Exception {
        try (SeekableByteChannel c = new SeekableInMemoryByteChannel(testData)) {
            assertEquals(testData.length, c.size());
            c.truncate(testData.length);
            assertEquals(testData.length, c.size());
            final ByteBuffer readBuffer = ByteBuffer.allocate(testData.length);
            assertEquals(testData.length, c.read(readBuffer));
            assertArrayEquals(testData, Arrays.copyOf(readBuffer.array(), testData.length));
        }
    }

    /*
     * <q>ClosedChannelException - If this channel is closed</q>
     */
    @Test
    @Disabled("we deliberately violate the spec")
    public void throwsClosedChannelExceptionWhenPositionIsReadOnClosedChannel() throws Exception {
        try (SeekableByteChannel c = new SeekableInMemoryByteChannel()) {
            c.close();
            c.position();
        }
    }

    /*
     * <q>ClosedChannelException - If this channel is closed</q>
     */
    @Test
    @Disabled("we deliberately violate the spec")
    public void throwsClosedChannelExceptionWhenSizeIsReadOnClosedChannel() throws Exception {
        try (SeekableByteChannel c = new SeekableInMemoryByteChannel()) {
            c.close();
            c.size();
        }
    }

    /*
     * <q>ClosedChannelException - If this channel is closed</q>
     */
    @Test
    @Disabled("we deliberately violate the spec")
    public void throwsClosedChannelExceptionWhenTruncateIsCalledOnClosedChannel() throws Exception {
        try (SeekableByteChannel c = new SeekableInMemoryByteChannel()) {
            c.close();
            c.truncate(0);
        }
    }

    /*
     * <q>Setting the position to a value that is greater than the current size is legal but does not change the size of the entity. A later attempt to write
     * bytes at such a position will cause the entity to grow to accommodate the new bytes; the values of any bytes between the previous end-of-file and the
     * newly-written bytes are unspecified.</q>
     */
    public void writingToAPositionAfterEndGrowsChannel() throws Exception {
        try (SeekableByteChannel c = new SeekableInMemoryByteChannel()) {
            c.position(2);
            assertEquals(2, c.position());
            final ByteBuffer inData = ByteBuffer.wrap(testData);
            assertEquals(testData.length, c.write(inData));
            assertEquals(testData.length + 2, c.size());

            c.position(2);
            final ByteBuffer readBuffer = ByteBuffer.allocate(testData.length);
            c.read(readBuffer);
            assertArrayEquals(testData, Arrays.copyOf(readBuffer.array(), testData.length));
        }
    }

}
