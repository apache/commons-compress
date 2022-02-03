/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.compress.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class IOUtilsTest {

    private interface StreamWrapper {
        InputStream wrap(InputStream toWrap);
    }

    @Test
    public void skipUsingSkip() throws Exception {
        skip(toWrap -> toWrap);
    }

    @Test
    public void skipUsingRead() throws Exception {
        skip(toWrap -> new FilterInputStream(toWrap) {
            @Override
            public long skip(final long s) {
                return 0;
            }
        });
    }

    @Test
    public void skipUsingSkipAndRead() throws Exception {
        skip(toWrap -> new FilterInputStream(toWrap) {
            boolean skipped;
            @Override
            public long skip(final long s) throws IOException {
                if (!skipped) {
                    toWrap.skip(5);
                    skipped = true;
                    return 5;
                }
                return 0;
            }
        });
    }

    @Test
    public void readFullyOnChannelReadsFully() throws IOException {
        final ByteBuffer b = ByteBuffer.allocate(20);
        final byte[] source = new byte[20];
        for (byte i = 0; i < 20; i++) {
            source[i] = i;
        }
        readFully(source, b);
        Assert.assertArrayEquals(source, b.array());
    }

    @Test
    public void readFullyOnChannelThrowsEof() throws IOException {
        final ByteBuffer b = ByteBuffer.allocate(21);
        final byte[] source = new byte[20];
        for (byte i = 0; i < 20; i++) {
            source[i] = i;
        }
        readFully(source, b);
    }

    @Test
    public void copyThrowsOnZeroBufferSize() throws IOException {
        IOUtils.copy(new ByteArrayInputStream(ByteUtils.EMPTY_BYTE_ARRAY), new ByteArrayOutputStream(), 0);
    }

    @Test
    public void copyRangeThrowsOnZeroBufferSize() throws IOException {
        IOUtils.copyRange(new ByteArrayInputStream(ByteUtils.EMPTY_BYTE_ARRAY), 5, new ByteArrayOutputStream(), 0);
    }

    @Test
    public void copyRangeDoesntCopyMoreThanAskedFor() throws IOException {
        try (ByteArrayInputStream in = new ByteArrayInputStream(new byte[] { 1, 2, 3, 4, 5 });
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Assert.assertEquals(3, IOUtils.copyRange(in, 3, out));
            out.close();
            Assert.assertArrayEquals(new byte[] { 1, 2, 3 }, out.toByteArray());
        }
    }

    @Test
    public void copyRangeStopsIfThereIsNothingToCopyAnymore() throws IOException {
        try (ByteArrayInputStream in = new ByteArrayInputStream(new byte[] { 1, 2, 3, 4, 5 });
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Assert.assertEquals(5, IOUtils.copyRange(in, 10, out));
            out.close();
            Assert.assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, out.toByteArray());
        }
    }

    @Test
    public void readRangeFromStreamDoesntReadMoreThanAskedFor() throws IOException {
        try (ByteArrayInputStream in = new ByteArrayInputStream(new byte[] { 1, 2, 3, 4, 5 })) {
            byte[] read = IOUtils.readRange(in, 3);
            Assert.assertArrayEquals(new byte[] { 1, 2, 3 }, read);
            Assert.assertEquals(4, in.read());
        }
    }

    @Test
    public void readRangeFromStreamStopsIfThereIsNothingToReadAnymore() throws IOException {
        try (ByteArrayInputStream in = new ByteArrayInputStream(new byte[] { 1, 2, 3, 4, 5 })) {
            byte[] read = IOUtils.readRange(in, 10);
            Assert.assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, read);
            Assert.assertEquals(-1, in.read());
        }
    }

    @Test
    public void readRangeFromChannelDoesntReadMoreThanAskedFor() throws IOException {
        try (ReadableByteChannel in = new SeekableInMemoryByteChannel(new byte[] { 1, 2, 3, 4, 5 })) {
            byte[] read = IOUtils.readRange(in, 3);
            Assert.assertArrayEquals(new byte[] { 1, 2, 3 }, read);
            final ByteBuffer b = ByteBuffer.allocate(1);
            Assert.assertEquals(1, in.read(b));
            Assert.assertArrayEquals(new byte[] { 4 }, b.array());
        }
    }

    @Test
    public void readRangeFromChannelDoesntReadMoreThanAskedForWhenItGotLessInFirstReadCall() throws IOException {
        try (ReadableByteChannel in = new SeekableInMemoryByteChannel(new byte[] { 1, 2, 3, 4, 5, 6, 7 }) {
            @Override
            public int read(ByteBuffer buf) throws IOException {
                // Trickle max 2 bytes at a time to trigger COMPRESS-584
                final ByteBuffer temp = ByteBuffer.allocate(Math.min(2, buf.remaining()));
                final int read = super.read(temp);
                if (read > 0) {
                    buf.put(temp.array(), 0, read);
                }
                return read;
            }
        }) {
            final byte[] read = IOUtils.readRange(in, 5);
            Assert.assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, read);
        }
    }

    @Test
    public void readRangeMoreThanCopyBufferSize() throws Exception {
        final Field COPY_BUF_SIZE = IOUtils.class.getDeclaredField("COPY_BUF_SIZE");
        COPY_BUF_SIZE.setAccessible(true);
        final int copyBufSize = (int)COPY_BUF_SIZE.get(null);

        // Make an input that requires two read loops to trigger COMPRESS-585
        final byte[] input = new byte[copyBufSize + 10];

        try (SeekableInMemoryByteChannel in = new SeekableInMemoryByteChannel(input)) {
            // Ask for less than the input length, but more than the buffer size
            final int toRead = copyBufSize + 1;
            final byte[] read = IOUtils.readRange(in, toRead);
            Assert.assertEquals(toRead, read.length);
            Assert.assertEquals(toRead, in.position());
        }
    }

    @Test
    public void readRangeFromChannelStopsIfThereIsNothingToReadAnymore() throws IOException {
        try (ReadableByteChannel in = new SeekableInMemoryByteChannel(new byte[] { 1, 2, 3, 4, 5 })) {
            byte[] read = IOUtils.readRange(in, 10);
            Assert.assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, read);
            final ByteBuffer b = ByteBuffer.allocate(1);
            Assert.assertEquals(-1, in.read(b));
        }
    }

    private static void readFully(final byte[] source, final ByteBuffer b) throws IOException {
        IOUtils.readFully(new SeekableInMemoryByteChannel(source), b);
    }

    private void skip(final StreamWrapper wrapper) throws Exception {
        final ByteArrayInputStream in = new ByteArrayInputStream(new byte[] {
                1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11
            });
        final InputStream sut = wrapper.wrap(in);
        Assert.assertEquals(10, IOUtils.skip(sut, 10));
        Assert.assertEquals(11, sut.read());
    }
}
