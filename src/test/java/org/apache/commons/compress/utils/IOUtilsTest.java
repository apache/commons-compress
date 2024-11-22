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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.io.output.NullOutputStream;
import org.junit.jupiter.api.Test;

public class IOUtilsTest {

    private interface StreamWrapper {
        InputStream wrap(InputStream toWrap);
    }

    private static void readFully(final byte[] source, final ByteBuffer b) throws IOException {
        try (SeekableInMemoryByteChannel channel = new SeekableInMemoryByteChannel(source)) {
            IOUtils.readFully(channel, b);
        }
    }

    private void skip(final StreamWrapper wrapper) throws Exception {
        final ByteArrayInputStream in = new ByteArrayInputStream(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 });
        try (InputStream sut = wrapper.wrap(in)) {
            assertEquals(10, IOUtils.skip(sut, 10));
            assertEquals(11, sut.read());
        }
    }

    @Test
    public void testCopy_inputStreamToOutputStream_IO84() throws Exception {
        final long size = (long) Integer.MAX_VALUE + (long) 1;
        final InputStream in = new NullInputStream(size);
        final OutputStream out = NullOutputStream.INSTANCE;
        // Test copy() method
        assertEquals(-1, IOUtils.copy(in, out));
        // reset the input
        in.close();
    }

    @Test
    public void testCopy_inputStreamToOutputStream_nullIn() {
        final OutputStream out = new ByteArrayOutputStream();
        assertThrows(NullPointerException.class, () -> IOUtils.copy((InputStream) null, out));
    }

    @Test
    public void testCopy_inputStreamToOutputStream_nullOut() {
        final InputStream in = new ByteArrayInputStream(new byte[] { 1, 2, 3, 4 });
        assertThrows(NullPointerException.class, () -> IOUtils.copy(in, (OutputStream) null));
    }

    @Test
    public void testCopyRangeDoesntCopyMoreThanAskedFor() throws IOException {
        try (ByteArrayInputStream in = new ByteArrayInputStream(new byte[] { 1, 2, 3, 4, 5 });
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            assertEquals(3, IOUtils.copyRange(in, 3, out));
            out.close();
            assertArrayEquals(new byte[] { 1, 2, 3 }, out.toByteArray());
        }
    }

    @Test
    public void testCopyRangeStopsIfThereIsNothingToCopyAnymore() throws IOException {
        try (ByteArrayInputStream in = new ByteArrayInputStream(new byte[] { 1, 2, 3, 4, 5 });
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            assertEquals(5, IOUtils.copyRange(in, 10, out));
            out.close();
            assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, out.toByteArray());
        }
    }

    @Test
    public void testCopyRangeThrowsOnZeroBufferSize() {
        assertThrows(IllegalArgumentException.class,
                () -> IOUtils.copyRange(new ByteArrayInputStream(ByteUtils.EMPTY_BYTE_ARRAY), 5, new ByteArrayOutputStream(), 0));
    }

    @Test
    public void testCopyOnZeroBufferSize() throws IOException {
        assertEquals(0, IOUtils.copy(new ByteArrayInputStream(ByteUtils.EMPTY_BYTE_ARRAY), new ByteArrayOutputStream(), 0));
    }

    @Test
    public void testReadFullyOnChannelReadsFully() throws IOException {
        final ByteBuffer b = ByteBuffer.allocate(20);
        final byte[] source = new byte[20];
        for (byte i = 0; i < 20; i++) {
            source[i] = i;
        }
        readFully(source, b);
        assertArrayEquals(source, b.array());
    }

    @Test
    public void testReadFullyOnChannelThrowsEof() {
        final ByteBuffer b = ByteBuffer.allocate(21);
        final byte[] source = new byte[20];
        for (byte i = 0; i < 20; i++) {
            source[i] = i;
        }
        assertThrows(EOFException.class, () -> readFully(source, b));
    }

    @Test
    public void testReadRangeFromChannelDoesntReadMoreThanAskedFor() throws IOException {
        try (ReadableByteChannel in = new SeekableInMemoryByteChannel(new byte[] { 1, 2, 3, 4, 5 })) {
            final byte[] read = IOUtils.readRange(in, 3);
            assertArrayEquals(new byte[] { 1, 2, 3 }, read);
            final ByteBuffer b = ByteBuffer.allocate(1);
            assertEquals(1, in.read(b));
            assertArrayEquals(new byte[] { 4 }, b.array());
        }
    }

    @Test
    public void testReadRangeFromChannelDoesntReadMoreThanAskedForWhenItGotLessInFirstReadCall() throws IOException {
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
            assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, read);
        }
    }

    @Test
    public void testReadRangeFromChannelStopsIfThereIsNothingToReadAnymore() throws IOException {
        try (ReadableByteChannel in = new SeekableInMemoryByteChannel(new byte[] { 1, 2, 3, 4, 5 })) {
            final byte[] read = IOUtils.readRange(in, 10);
            assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, read);
            final ByteBuffer b = ByteBuffer.allocate(1);
            assertEquals(-1, in.read(b));
        }
    }

    @Test
    public void testReadRangeFromStreamDoesntReadMoreThanAskedFor() throws IOException {
        try (ByteArrayInputStream in = new ByteArrayInputStream(new byte[] { 1, 2, 3, 4, 5 })) {
            final byte[] read = IOUtils.readRange(in, 3);
            assertArrayEquals(new byte[] { 1, 2, 3 }, read);
            assertEquals(4, in.read());
        }
    }

    @Test
    public void testReadRangeFromStreamStopsIfThereIsNothingToReadAnymore() throws IOException {
        try (ByteArrayInputStream in = new ByteArrayInputStream(new byte[] { 1, 2, 3, 4, 5 })) {
            final byte[] read = IOUtils.readRange(in, 10);
            assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, read);
            assertEquals(-1, in.read());
        }
    }

    @Test
    public void testReadRangeMoreThanCopyBufferSize() throws Exception {
        final int copyBufSize = org.apache.commons.io.IOUtils.DEFAULT_BUFFER_SIZE;

        // Make an input that requires two read loops to trigger COMPRESS-585
        final byte[] input = new byte[copyBufSize + 10];

        try (SeekableInMemoryByteChannel in = new SeekableInMemoryByteChannel(input)) {
            // Ask for less than the input length, but more than the buffer size
            final int toRead = copyBufSize + 1;
            final byte[] read = IOUtils.readRange(in, toRead);
            assertEquals(toRead, read.length);
            assertEquals(toRead, in.position());
        }
    }

    @Test
    public void testSkipUsingRead() throws Exception {
        skip(toWrap -> new FilterInputStream(toWrap) {
            @Override
            public long skip(final long s) {
                return 0;
            }
        });
    }

    @Test
    public void testSkipUsingSkip() throws Exception {
        skip(toWrap -> toWrap);
    }

    @Test
    public void testSkipUsingSkipAndRead() throws Exception {
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
    public void testToByteArray_InputStream() throws Exception {
        final byte[] bytes = "ABCB".getBytes(StandardCharsets.UTF_8);
        try (InputStream fin = new ByteArrayInputStream(bytes)) {
            @SuppressWarnings("deprecation")
            final byte[] out = IOUtils.toByteArray(fin);
            assertNotNull(out);
            assertEquals(0, fin.available());
            assertEquals(4, out.length);
            assertArrayEquals(bytes, out);
        }
    }

}
