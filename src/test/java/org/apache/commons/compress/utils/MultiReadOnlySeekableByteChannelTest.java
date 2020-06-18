/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.commons.compress.utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Initially based on <a
 * href="https://github.com/frugalmechanic/fm-common/blob/master/jvm/src/test/scala/fm/common/TestMultiReadOnlySeekableByteChannel.scala">TestMultiReadOnlySeekableByteChannel.scala</a>
 * by Tim Underwood.
 */
public class MultiReadOnlySeekableByteChannelTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void constructorThrowsOnNullArg() {
        thrown.expect(NullPointerException.class);
        new MultiReadOnlySeekableByteChannel(null);
    }

    @Test
    public void forSeekableByteChannelsThrowsOnNullArg() throws IOException {
        thrown.expect(NullPointerException.class);
        MultiReadOnlySeekableByteChannel.forSeekableByteChannels(null);
    }

    @Test
    public void forFilesThrowsOnNullArg() throws IOException {
        thrown.expect(NullPointerException.class);
        MultiReadOnlySeekableByteChannel.forFiles(null);
    }

    @Test
    public void forSeekableByteChannelsReturnsIdentityForSingleElement() throws IOException {
        final SeekableByteChannel e = makeEmpty();
        final SeekableByteChannel m = MultiReadOnlySeekableByteChannel.forSeekableByteChannels(e);
        Assert.assertSame(e, m);
    }

    @Test
    public void referenceBehaviorForEmptyChannel() throws IOException {
        checkEmpty(makeEmpty());
    }

    @Test
    public void twoEmptyChannelsConcatenateAsEmptyChannel() throws IOException {
        checkEmpty(MultiReadOnlySeekableByteChannel.forSeekableByteChannels(makeEmpty(), makeEmpty()));
    }

    @Test
    public void checkForSingleByte() throws IOException {
        check(new byte[] { 0 });
    }

    @Test
    public void checkForString() throws IOException {
        check("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
            .getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void verifyGrouped() {
        Assert.assertArrayEquals(new byte[][] {
                new byte[] { 1, 2, 3, },
                new byte[] { 4, 5, 6, },
                new byte[] { 7, },
            }, grouped(new byte[] { 1, 2, 3, 4, 5, 6, 7 }, 3));
        Assert.assertArrayEquals(new byte[][] {
                new byte[] { 1, 2, 3, },
                new byte[] { 4, 5, 6, },
            }, grouped(new byte[] { 1, 2, 3, 4, 5, 6 }, 3));
        Assert.assertArrayEquals(new byte[][] {
                new byte[] { 1, 2, 3, },
                new byte[] { 4, 5, },
            }, grouped(new byte[] { 1, 2, 3, 4, 5, }, 3));
    }

    @Test
    public void closesAllAndThrowsExceptionIfCloseThrows() throws IOException {
        SeekableByteChannel[] ts = new ThrowingSeekableByteChannel[] {
            new ThrowingSeekableByteChannel(),
            new ThrowingSeekableByteChannel()
        };
        SeekableByteChannel s = MultiReadOnlySeekableByteChannel.forSeekableByteChannels(ts);
        try {
            s.close();
            Assert.fail("IOException expected");
        } catch (IOException expected) {
        }
        Assert.assertFalse(ts[0].isOpen());
        Assert.assertFalse(ts[1].isOpen());
    }

    @Test
    public void cantTruncate() throws IOException {
        SeekableByteChannel s = MultiReadOnlySeekableByteChannel.forSeekableByteChannels(makeEmpty(), makeEmpty());
        thrown.expect(NonWritableChannelException.class);
        s.truncate(1);
    }

    @Test
    public void cantWrite() throws IOException {
        SeekableByteChannel s = MultiReadOnlySeekableByteChannel.forSeekableByteChannels(makeEmpty(), makeEmpty());
        thrown.expect(NonWritableChannelException.class);
        s.write(ByteBuffer.allocate(10));
    }

    @Test
    public void cantPositionToANegativePosition() throws IOException {
        SeekableByteChannel s = MultiReadOnlySeekableByteChannel.forSeekableByteChannels(makeEmpty(), makeEmpty());
        thrown.expect(IllegalArgumentException.class);
        s.position(-1);
    }

    private SeekableByteChannel makeEmpty() {
        return makeSingle(new byte[0]);
    }

    private SeekableByteChannel makeSingle(byte[] arr) {
        return new SeekableInMemoryByteChannel(arr);
    }

    private SeekableByteChannel makeMulti(byte[][] arr) throws IOException {
        SeekableByteChannel[] s = new SeekableByteChannel[arr.length];
        for (int i = 0; i < s.length; i++) {
            s[i] = makeSingle(arr[i]);
        }
        return MultiReadOnlySeekableByteChannel.forSeekableByteChannels(s);
    }

    private void checkEmpty(SeekableByteChannel channel) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(10);

        Assert.assertTrue(channel.isOpen());
        Assert.assertEquals(0, channel.size());
        Assert.assertEquals(0, channel.position());
        Assert.assertEquals(-1, channel.read(buf));

        channel.position(5);
        Assert.assertEquals(-1, channel.read(buf));

        channel.close();
        Assert.assertFalse(channel.isOpen());

        try {
            channel.read(buf);
            Assert.fail("expected a ClosedChannelException");
        } catch (ClosedChannelException expected) {
        }
        try {
            channel.position(100);
            Assert.fail("expected a ClosedChannelException");
        } catch (ClosedChannelException expected) {
        }
    }

    private void check(final byte[] expected) throws IOException {
        for (int channelSize = 1; channelSize <= expected.length; channelSize++) {
            // Sanity check that all operations work for SeekableInMemoryByteChannel
            check(expected, makeSingle(expected));
            // Checks against our MultiReadOnlySeekableByteChannel instance
            check(expected, makeMulti(grouped(expected, channelSize)));
        }
    }

    private void check(final byte[] expected, SeekableByteChannel channel) throws IOException {
        for (int readBufferSize = 1; readBufferSize <= expected.length + 5; readBufferSize++) {
            check(expected, channel, readBufferSize);
        }
    }

    private void check(final byte[] expected, final SeekableByteChannel channel, final int readBufferSize)
        throws IOException {
        Assert.assertTrue("readBufferSize " + readBufferSize, channel.isOpen());
        Assert.assertEquals("readBufferSize " + readBufferSize, expected.length, channel.size());
        channel.position(0);
        Assert.assertEquals("readBufferSize " + readBufferSize, 0, channel.position());
        Assert.assertEquals("readBufferSize " + readBufferSize, 0,
            channel.read(ByteBuffer.allocate(0)));

        // Will hold the entire result that we read
        final ByteBuffer resultBuffer = ByteBuffer.allocate(expected.length + 100);

        // Used for each read() method call
        final ByteBuffer buf = ByteBuffer.allocate(readBufferSize);

        int bytesRead = channel.read(buf);

        while (bytesRead != -1) {
            int remaining = buf.remaining();

            buf.flip();
            resultBuffer.put(buf);
            buf.clear();
            bytesRead = channel.read(buf);

            // If this isn't the last read() then we expect the buf
            // ByteBuffer to be full (i.e. have no remaining)
            if (resultBuffer.position() < expected.length) {
                Assert.assertEquals("readBufferSize " + readBufferSize, 0, remaining);
            }

            if (bytesRead == -1) {
                Assert.assertEquals("readBufferSize " + readBufferSize, 0, buf.position());
            } else {
                Assert.assertEquals("readBufferSize " + readBufferSize, bytesRead, buf.position());
            }
        }

        resultBuffer.flip();
        byte[] arr = new byte[resultBuffer.remaining()];
        resultBuffer.get(arr);
        Assert.assertArrayEquals("readBufferSize " + readBufferSize, expected, arr);
    }

    private byte[][] grouped(final byte[] input, final int chunkSize) {
        List<byte[]> groups = new ArrayList<>();
        int idx = 0;
        for (; idx + chunkSize <= input.length; idx += chunkSize) {
            groups.add(Arrays.copyOfRange(input, idx, idx + chunkSize));
        }
        if (idx < input.length) {
            groups.add(Arrays.copyOfRange(input, idx, input.length));
        }
        return groups.toArray(new byte[0][]);
    }

    private static class ThrowingSeekableByteChannel implements SeekableByteChannel {
        private boolean closed = false;
        @Override
        public boolean isOpen() {
            return !closed;
        }
        @Override
        public void close() throws IOException {
            closed = true;
            throw new IOException("foo");
        }
        @Override
        public int read(ByteBuffer dst) throws IOException {
            return -1;
        }
        @Override
        public int write(ByteBuffer src) throws IOException {
            return 0;
        }
        @Override
        public long position() throws IOException {
            return 0;
        }
        @Override
        public SeekableByteChannel position(long newPosition) throws IOException {
            return this;
        }
        @Override
        public long size() throws IOException {
            return 0;
        }
        @Override
        public SeekableByteChannel truncate(long size) throws IOException {
            return this;
        }
    }

    // Contract Tests added in response to https://issues.apache.org/jira/browse/COMPRESS-499

    private SeekableByteChannel testChannel() {
        return MultiReadOnlySeekableByteChannel
            .forSeekableByteChannels(makeEmpty(), makeEmpty());
    }

    // https://docs.oracle.com/javase/7/docs/api/java/io/Closeable.html#close()

    /*
     * <q>If the stream is already closed then invoking this method has no effect.</q>
     */
    @Test
    public void closeIsIdempotent() throws Exception {
        try (SeekableByteChannel c = testChannel()) {
            c.close();
            Assert.assertFalse(c.isOpen());
            c.close();
            Assert.assertFalse(c.isOpen());
        }
    }

    // https://docs.oracle.com/javase/7/docs/api/java/nio/channels/SeekableByteChannel.html#position()

    /*
     * <q>ClosedChannelException - If this channel is closed</q>
     */
    @Test
    @Ignore("we deliberately violate the spec")
    public void throwsClosedChannelExceptionWhenPositionIsReadOnClosedChannel() throws Exception {
        thrown.expect(ClosedChannelException.class);
        try (SeekableByteChannel c = testChannel()) {
            c.close();
            c.position();
        }
    }

    // https://docs.oracle.com/javase/7/docs/api/java/nio/channels/SeekableByteChannel.html#size()

    /*
     * <q>ClosedChannelException - If this channel is closed</q>
     */
    @Test
    public void throwsClosedChannelExceptionWhenSizeIsReadOnClosedChannel() throws Exception {
        thrown.expect(ClosedChannelException.class);
        try (SeekableByteChannel c = testChannel()) {
            c.close();
            c.size();
        }
    }

    // https://docs.oracle.com/javase/7/docs/api/java/nio/channels/SeekableByteChannel.html#position(long)

    /*
     * <q>ClosedChannelException - If this channel is closed</q>
     */
    @Test
    public void throwsClosedChannelExceptionWhenPositionIsSetOnClosedChannel() throws Exception {
        thrown.expect(ClosedChannelException.class);
        try (SeekableByteChannel c = testChannel()) {
            c.close();
            c.position(0);
        }
    }

    /*
     * <q>Setting the position to a value that is greater than the current size is legal but does not change the size of
     * the entity. A later attempt to read bytes at such a position will immediately return an end-of-file
     * indication</q>
     */
    @Test
    public void readingFromAPositionAfterEndReturnsEOF() throws Exception {
        try (SeekableByteChannel c = testChannel()) {
            c.position(2);
            Assert.assertEquals(2, c.position());
            ByteBuffer readBuffer = ByteBuffer.allocate(5);
            Assert.assertEquals(-1, c.read(readBuffer));
        }
    }

    /*
     * <q>IllegalArgumentException - If the new position is negative</q>
     */
    @Test
    public void throwsIllegalArgumentExceptionWhenPositionIsSetToANegativeValue() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        try (SeekableByteChannel c = testChannel()) {
            c.position(-1);
        }
    }

}
