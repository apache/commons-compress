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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

// @formatter:off
/**
 * Initially based on <a href=
 * "https://github.com/frugalmechanic/fm-common/blob/master/jvm/src/test/scala/fm/common/TestMultiReadOnlySeekableByteChannel.scala">
 * TestMultiReadOnlySeekableByteChannel.scala</a>
 * by Tim Underwood.
 */
//@formatter:on
class MultiReadOnlySeekableByteChannelTest {

    private static final class ThrowingSeekableByteChannel implements SeekableByteChannel {
        private boolean closed;

        @Override
        public void close() throws IOException {
            closed = true;
            throw new IOException("foo");
        }

        @Override
        public boolean isOpen() {
            return !closed;
        }

        @Override
        public long position() {
            return 0;
        }

        @Override
        public SeekableByteChannel position(final long newPosition) {
            return this;
        }

        @Override
        public int read(final ByteBuffer dst) throws IOException {
            return -1;
        }

        @Override
        public long size() throws IOException {
            return 0;
        }

        @Override
        public SeekableByteChannel truncate(final long size) {
            return this;
        }

        @Override
        public int write(final ByteBuffer src) throws IOException {
            return 0;
        }
    }

    private void check(final byte[] expected) throws IOException {
        for (int channelSize = 1; channelSize <= expected.length; channelSize++) {
            // Sanity check that all operations work for SeekableInMemoryByteChannel
            try (SeekableByteChannel single = makeSingle(expected)) {
                check(expected, single);
            }
            // Checks against our MultiReadOnlySeekableByteChannel instance
            try (SeekableByteChannel multi = makeMulti(grouped(expected, channelSize))) {
                check(expected, multi);
            }
        }
    }

    private void check(final byte[] expected, final SeekableByteChannel channel) throws IOException {
        for (int readBufferSize = 1; readBufferSize <= expected.length + 5; readBufferSize++) {
            check(expected, channel, readBufferSize);
        }
    }

    private void check(final byte[] expected, final SeekableByteChannel channel, final int readBufferSize) throws IOException {
        assertTrue(channel.isOpen(), "readBufferSize " + readBufferSize);
        assertEquals(expected.length, channel.size(), "readBufferSize " + readBufferSize);
        channel.position(0);
        assertEquals(0, channel.position(), "readBufferSize " + readBufferSize);
        assertEquals(0, channel.read(ByteBuffer.allocate(0)), "readBufferSize " + readBufferSize);

        // Will hold the entire result that we read
        final ByteBuffer resultBuffer = ByteBuffer.allocate(expected.length + 100);

        // Used for each read() method call
        final ByteBuffer buf = ByteBuffer.allocate(readBufferSize);

        int bytesRead = channel.read(buf);

        while (bytesRead != -1) {
            final int remaining = buf.remaining();

            buf.flip();
            resultBuffer.put(buf);
            buf.clear();
            bytesRead = channel.read(buf);

            // If this isn't the last read() then we expect the buf
            // ByteBuffer to be full (i.e. have no remaining)
            if (resultBuffer.position() < expected.length) {
                assertEquals(0, remaining, "readBufferSize " + readBufferSize);
            }

            if (bytesRead == -1) {
                assertEquals(0, buf.position(), "readBufferSize " + readBufferSize);
            } else {
                assertEquals(bytesRead, buf.position(), "readBufferSize " + readBufferSize);
            }
        }

        resultBuffer.flip();
        final byte[] arr = new byte[resultBuffer.remaining()];
        resultBuffer.get(arr);
        assertArrayEquals(expected, arr, "readBufferSize " + readBufferSize);
    }

    private void checkEmpty(final SeekableByteChannel channel) throws IOException {
        final ByteBuffer buf = ByteBuffer.allocate(10);

        assertTrue(channel.isOpen());
        assertEquals(0, channel.size());
        assertEquals(0, channel.position());
        assertEquals(-1, channel.read(buf));

        channel.position(5);
        assertEquals(-1, channel.read(buf));

        channel.close();
        assertFalse(channel.isOpen());

        assertThrows(ClosedChannelException.class, () -> channel.read(buf), "expected a ClosedChannelException");
        assertThrows(ClosedChannelException.class, () -> channel.position(100), "expected a ClosedChannelException");
    }

    private byte[][] grouped(final byte[] input, final int chunkSize) {
        final List<byte[]> groups = new ArrayList<>();
        int idx = 0;
        for (; idx + chunkSize <= input.length; idx += chunkSize) {
            groups.add(Arrays.copyOfRange(input, idx, idx + chunkSize));
        }
        if (idx < input.length) {
            groups.add(Arrays.copyOfRange(input, idx, input.length));
        }
        return groups.toArray(new byte[0][]);
    }

    private SeekableByteChannel makeEmpty() {
        return makeSingle(ByteUtils.EMPTY_BYTE_ARRAY);
    }

    private SeekableByteChannel makeMulti(final byte[][] arr) {
        final SeekableByteChannel[] s = new SeekableByteChannel[arr.length];
        for (int i = 0; i < s.length; i++) {
            s[i] = makeSingle(arr[i]);
        }
        return MultiReadOnlySeekableByteChannel.forSeekableByteChannels(s);
    }

    private SeekableByteChannel makeSingle(final byte[] arr) {
        return new SeekableInMemoryByteChannel(arr);
    }

    @Test
    void testCantPositionToANegativePosition() throws IOException {
        try (SeekableByteChannel s = MultiReadOnlySeekableByteChannel.forSeekableByteChannels(makeEmpty(), makeEmpty())) {
            assertThrows(IllegalArgumentException.class, () -> s.position(-1));
        }
    }

    @Test
    void testCantTruncate() throws IOException {
        try (SeekableByteChannel s = MultiReadOnlySeekableByteChannel.forSeekableByteChannels(makeEmpty(), makeEmpty())) {
            assertThrows(NonWritableChannelException.class, () -> s.truncate(1));
        }
    }

    @Test
    void testCantWrite() throws IOException {
        try (SeekableByteChannel s = MultiReadOnlySeekableByteChannel.forSeekableByteChannels(makeEmpty(), makeEmpty())) {
            assertThrows(NonWritableChannelException.class, () -> s.write(ByteBuffer.allocate(10)));
        }
    }

    private SeekableByteChannel testChannel() {
        return MultiReadOnlySeekableByteChannel.forSeekableByteChannels(makeEmpty(), makeEmpty());
    }

    @Test
    void testCheckForSingleByte() throws IOException {
        check(new byte[] { 0 });
    }

    @Test
    void testCheckForString() throws IOException {
        check("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".getBytes(UTF_8));
    }

    /*
     * <q>If the stream is already closed then invoking this method has no effect.</q>
     */
    @Test
    void testCloseIsIdempotent() throws Exception {
        try (SeekableByteChannel c = testChannel()) {
            c.close();
            assertFalse(c.isOpen());
            c.close();
            assertFalse(c.isOpen());
        }
    }

    @Test
    void testClosesAllAndThrowsExceptionIfCloseThrows() {
        final SeekableByteChannel[] ts = new ThrowingSeekableByteChannel[] { new ThrowingSeekableByteChannel(), new ThrowingSeekableByteChannel() };
        final SeekableByteChannel s = MultiReadOnlySeekableByteChannel.forSeekableByteChannels(ts);
        assertThrows(IOException.class, s::close, "IOException expected");
        assertFalse(ts[0].isOpen());
        assertFalse(ts[1].isOpen());
    }

    @Test
    void testConstructorThrowsOnNullArg() {
        assertThrows(NullPointerException.class, () -> new MultiReadOnlySeekableByteChannel(null));
    }

    @Test
    void testForFilesThrowsOnNullArg() {
        assertThrows(NullPointerException.class, () -> MultiReadOnlySeekableByteChannel.forFiles((File[]) null));
    }

    @Test
    void testForSeekableByteChannelsReturnsIdentityForSingleElement() throws IOException {
        try (SeekableByteChannel e = makeEmpty();
                SeekableByteChannel m = MultiReadOnlySeekableByteChannel.forSeekableByteChannels(e)) {
            assertSame(e, m);
        }
    }

    @Test
    void testForSeekableByteChannelsThrowsOnNullArg() {
        assertThrows(NullPointerException.class, () -> MultiReadOnlySeekableByteChannel.forSeekableByteChannels((SeekableByteChannel[]) null));
    }

    /*
     * <q>Setting the position to a value that is greater than the current size is legal but does not change the size of the entity. A later attempt to read
     * bytes at such a position will immediately return an end-of-file indication</q>
     */
    @Test
    void testReadingFromAPositionAfterEndReturnsEOF() throws Exception {
        try (SeekableByteChannel c = testChannel()) {
            c.position(2);
            assertEquals(2, c.position());
            final ByteBuffer readBuffer = ByteBuffer.allocate(5);
            assertEquals(-1, c.read(readBuffer));
        }
    }

    // Contract Tests added in response to https://issues.apache.org/jira/browse/COMPRESS-499

    @Test
    void testReferenceBehaviorForEmptyChannel() throws IOException {
        checkEmpty(makeEmpty());
    }

    // https://docs.oracle.com/javase/8/docs/api/java/io/Closeable.html#close()

    /*
     * <q>ClosedChannelException - If this channel is closed</q>
     */
    @Test
    void testThrowsClosedChannelExceptionWhenPositionIsSetOnClosedChannel() throws Exception {
        try (SeekableByteChannel c = testChannel()) {
            c.close();
            assertThrows(ClosedChannelException.class, () -> c.position(0));
        }
    }

    // https://docs.oracle.com/javase/8/docs/api/java/nio/channels/SeekableByteChannel.html#position()

    /*
     * <q>ClosedChannelException - If this channel is closed</q>
     */
    @Test
    void testThrowsClosedChannelExceptionWhenSizeIsReadOnClosedChannel() throws Exception {
        try (SeekableByteChannel c = testChannel()) {
            c.close();
            assertThrows(ClosedChannelException.class, () -> c.size());
        }
    }

    // https://docs.oracle.com/javase/8/docs/api/java/nio/channels/SeekableByteChannel.html#size()

    /*
     * <q>IOException - If the new position is negative</q>
     */
    @Test
    void testThrowsIOExceptionWhenPositionIsSetToANegativeValue() throws Exception {
        try (SeekableByteChannel c = testChannel()) {
            assertThrows(IllegalArgumentException.class, () -> c.position(-1));
        }
    }

    // https://docs.oracle.com/javase/8/docs/api/java/nio/channels/SeekableByteChannel.html#position(long)

    @Test
    void testTwoEmptyChannelsConcatenateAsEmptyChannel() throws IOException {
        try (SeekableByteChannel channel = MultiReadOnlySeekableByteChannel.forSeekableByteChannels(makeEmpty(), makeEmpty())) {
            checkEmpty(channel);
        }
    }

    @Test
    void testVerifyGrouped() {
        assertArrayEquals(new byte[][] { new byte[] { 1, 2, 3, }, new byte[] { 4, 5, 6, }, new byte[] { 7, }, },
                grouped(new byte[] { 1, 2, 3, 4, 5, 6, 7 }, 3));
        assertArrayEquals(new byte[][] { new byte[] { 1, 2, 3, }, new byte[] { 4, 5, 6, }, }, grouped(new byte[] { 1, 2, 3, 4, 5, 6 }, 3));
        assertArrayEquals(new byte[][] { new byte[] { 1, 2, 3, }, new byte[] { 4, 5, }, }, grouped(new byte[] { 1, 2, 3, 4, 5, }, 3));
    }

    /*
     * <q>ClosedChannelException - If this channel is closed</q>
     */
    @Test
    @Disabled("we deliberately violate the spec")
    public void throwsClosedChannelExceptionWhenPositionIsReadOnClosedChannel() throws Exception {
        try (SeekableByteChannel c = testChannel()) {
            c.close();
            c.position();
        }
    }

}
