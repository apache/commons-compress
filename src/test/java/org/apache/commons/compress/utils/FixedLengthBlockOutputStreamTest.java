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

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

public class FixedLengthBlockOutputStreamTest {

    private static class MockOutputStream extends OutputStream {

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        private final int requiredWriteSize;
        private final boolean doPartialWrite;
        private final AtomicBoolean closed = new AtomicBoolean();

        private MockOutputStream(final int requiredWriteSize, final boolean doPartialWrite) {
            this.requiredWriteSize = requiredWriteSize;
            this.doPartialWrite = doPartialWrite;
        }

        private void checkIsOpen() throws IOException {
            if (closed.get()) {
                throw new IOException("Closed");
            }
        }

        @Override
        public void close() throws IOException {
            if (closed.compareAndSet(false, true)) {
                bos.close();
            }
        }

        @Override
        public void write(final byte[] b, final int off, int len) throws IOException {
            checkIsOpen();
            assertEquals(requiredWriteSize, len, "write size");
            if (doPartialWrite) {
                len--;
            }
            bos.write(b, off, len);
        }

        @Override
        public void write(final int b) throws IOException {
            checkIsOpen();
            assertEquals(requiredWriteSize, 1, "write size");
            bos.write(b);
        }
    }

    private static class MockWritableByteChannel implements WritableByteChannel {

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        private final int requiredWriteSize;
        private final boolean doPartialWrite;

        final AtomicBoolean closed = new AtomicBoolean();

        private MockWritableByteChannel(final int requiredWriteSize, final boolean doPartialWrite) {
            this.requiredWriteSize = requiredWriteSize;
            this.doPartialWrite = doPartialWrite;
        }

        @Override
        public void close() throws IOException {
            closed.compareAndSet(false, true);
        }

        @Override
        public boolean isOpen() {
            return !closed.get();
        }

        @Override
        public int write(final ByteBuffer src) throws IOException {
            assertEquals(requiredWriteSize, src.remaining(), "write size");
            if (doPartialWrite) {
                src.limit(src.limit() - 1);
            }
            final int bytesOut = src.remaining();
            while (src.hasRemaining()) {
                bos.write(src.get());
            }
            return bytesOut;
        }
    }

    private static void assertContainsAtOffset(final String msg, final byte[] expected, final int offset,
        final byte[] actual) {
        assertThat(actual.length, greaterThanOrEqualTo(offset + expected.length));
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i + offset], String.format("%s ([%d])", msg, i));
        }
    }


    private ByteBuffer getByteBuffer(final byte[] msg) {
        final int len = msg.length;
        final ByteBuffer buf = ByteBuffer.allocate(len);
        buf.put(msg);
        buf.flip();
        return buf;
    }

    private FixedLengthBlockOutputStream getClosedFLBOS() throws IOException {
        final int blockSize = 512;
        final FixedLengthBlockOutputStream out = new FixedLengthBlockOutputStream(
            new MockOutputStream(blockSize, false), blockSize);
        out.write(1);
        assertTrue(out.isOpen());
        out.close();
        assertFalse(out.isOpen());
        return out;
    }

    private void testBuf(final int blockSize, final String text) throws IOException {
        final MockWritableByteChannel mock = new MockWritableByteChannel(blockSize, false);

        final ByteArrayOutputStream bos = mock.bos;
        final byte[] msg = text.getBytes();
        final ByteBuffer buf = getByteBuffer(msg);
        try (FixedLengthBlockOutputStream out = new FixedLengthBlockOutputStream(mock, blockSize)) {
            out.write(buf);
        }
        final double v = Math.ceil(msg.length / (double) blockSize) * blockSize;
        assertEquals((long) v, bos.size(), "wrong size");
        final byte[] output = bos.toByteArray();
        final String l = new String(output, 0, msg.length);
        assertEquals(text, l);
        for (int i = msg.length; i < bos.size(); i++) {
            assertEquals(0, output[i], String.format("output[%d]", i));

        }
    }

    @Test
    public void testMultiWriteBuf() throws IOException {
        final int blockSize = 13;
        final MockWritableByteChannel mock = new MockWritableByteChannel(blockSize, false);
        final String testString = "hello world";
        final byte[] msg = testString.getBytes();
        final int reps = 17;

        try (FixedLengthBlockOutputStream out = new FixedLengthBlockOutputStream(mock, blockSize)) {
            for (int i = 0; i < reps; i++) {
                final ByteBuffer buf = getByteBuffer(msg);
                out.write(buf);
            }
        }
        final ByteArrayOutputStream bos = mock.bos;
        final double v = Math.ceil((reps * msg.length) / (double) blockSize) * blockSize;
        assertEquals((long) v, bos.size(), "wrong size");
        final int strLen = msg.length * reps;
        final byte[] output = bos.toByteArray();
        final String l = new String(output, 0, strLen);
        final StringBuilder buf = new StringBuilder(strLen);
        for (int i = 0; i < reps; i++) {
            buf.append(testString);
        }
        assertEquals(buf.toString(), l);
        for (int i = strLen; i < output.length; i++) {
            assertEquals(0, output[i]);
        }
    }

    @Test
    public void testPartialWritingThrowsException() {
        final IOException e = assertThrows(IOException.class, () -> testWriteAndPad(512, "hello world!\n", true),
                "Exception for partial write not thrown");
        final String msg = e.getMessage();
        assertEquals("Failed to write 512 bytes atomically. Only wrote  511", msg, "exception message");
    }

    @Test
    public void testSmallWrite() throws IOException {
        testWriteAndPad(10240, "hello world!\n", false);
        testWriteAndPad(512, "hello world!\n", false);
        testWriteAndPad(11, "hello world!\n", false);
        testWriteAndPad(3, "hello world!\n", false);
    }

    @Test
    public void testSmallWriteToStream() throws IOException {
        testWriteAndPadToStream(10240, "hello world!\n", false);
        testWriteAndPadToStream(512, "hello world!\n", false);
        testWriteAndPadToStream(11, "hello world!\n", false);
        testWriteAndPadToStream(3, "hello     world!\n", false);
    }

    @Test
    public void testWithFileOutputStream() throws IOException {
        final Path tempFile = Files.createTempFile("xxx", "yyy");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Files.deleteIfExists(tempFile);
            } catch (final IOException e) {
            }
        }));
        final int blockSize = 512;
        final int reps = 1000;
        final OutputStream os = Files.newOutputStream(tempFile.toFile().toPath());
        try (FixedLengthBlockOutputStream out = new FixedLengthBlockOutputStream(
            os, blockSize)) {
            final DataOutputStream dos = new DataOutputStream(out);
            for (int i = 0; i < reps; i++) {
               dos.writeInt(i);
            }
        }
        final long expectedDataSize = reps * 4L;
        final long expectedFileSize = (long)Math.ceil(expectedDataSize/(double)blockSize)*blockSize;
        assertEquals(expectedFileSize, Files.size(tempFile), "file size");
        final DataInputStream din = new DataInputStream(Files.newInputStream(tempFile));
        for (int i = 0; i < reps; i++) {
            assertEquals(i, din.readInt(), "file int");
        }
        for (int i = 0; i < expectedFileSize - expectedDataSize; i++) {
            assertEquals(0, din.read());
        }
        assertEquals(-1,din.read());
    }

    private void testWriteAndPad(final int blockSize, final String text, final boolean doPartialWrite)
        throws IOException {
        final MockWritableByteChannel mock = new MockWritableByteChannel(blockSize, doPartialWrite);
        final byte[] msg = text.getBytes(US_ASCII);

        final ByteArrayOutputStream bos = mock.bos;
        try (FixedLengthBlockOutputStream out = new FixedLengthBlockOutputStream(mock, blockSize)) {

            out.write(msg);
            assertEquals((msg.length / blockSize) * blockSize, bos.size(), "no partial write");
        }
        validate(blockSize, msg, bos.toByteArray());
    }


    private void testWriteAndPadToStream(final int blockSize, final String text, final boolean doPartialWrite)
        throws IOException {
        final MockOutputStream mock = new MockOutputStream(blockSize, doPartialWrite);
        final byte[] msg = text.getBytes(US_ASCII);

        final ByteArrayOutputStream bos = mock.bos;
        try (FixedLengthBlockOutputStream out = new FixedLengthBlockOutputStream(mock, blockSize)) {
            out.write(msg);
            assertEquals((msg.length / blockSize) * blockSize, bos.size(), "no partial write");
        }
        validate(blockSize, msg, bos.toByteArray());

    }

    @Test
    public void testWriteBuf() throws IOException {
        final String hwa = "hello world avengers";
        testBuf(4, hwa);
        testBuf(512, hwa);
        testBuf(10240, hwa);
        testBuf(11, hwa + hwa + hwa);
    }


    @Test
    public void testWriteFailsAfterDestClosedThrowsException() {
        final int blockSize = 2;
        final MockOutputStream mock = new MockOutputStream(blockSize, false);
        final FixedLengthBlockOutputStream out =
            new FixedLengthBlockOutputStream(mock, blockSize);
        assertThrows(IOException.class, () -> {
            out.write(1);
            assertTrue(out.isOpen());
            mock.close();
            out.write(1);
        }, "expected IO Exception");
        assertFalse(out.isOpen());
    }

    @Test
    public void testWriteFailsAfterFLClosedThrowsException() {
        assertThrowsExactly(ClosedChannelException.class, () -> {
            final FixedLengthBlockOutputStream out = getClosedFLBOS();
            out.write(1);
        }, "expected Closed Channel Exception");

        assertThrowsExactly(ClosedChannelException.class, () -> {
            final FixedLengthBlockOutputStream out = getClosedFLBOS();
            out.write(new byte[]{0, 1, 2, 3});
        }, "expected Closed Channel Exception");

        assertThrowsExactly(ClosedChannelException.class, () -> {
            final FixedLengthBlockOutputStream out = getClosedFLBOS();
            out.write(ByteBuffer.wrap(new byte[]{0, 1, 2, 3}));
        }, "expected Closed Channel Exception");
    }

    @Test
    public void testWriteSingleBytes() throws IOException {
        final int blockSize = 4;
        final MockWritableByteChannel mock = new MockWritableByteChannel(blockSize, false);
        final ByteArrayOutputStream bos = mock.bos;
        final String text = "hello world avengers";
        final byte[] msg = text.getBytes();
        final int len = msg.length;
        try (FixedLengthBlockOutputStream out = new FixedLengthBlockOutputStream(mock, blockSize)) {
            for (int i = 0; i < len; i++) {
                out.write(msg[i]);
            }
        }
        final byte[] output = bos.toByteArray();

        validate(blockSize, msg, output);
    }

    private void validate(final int blockSize, final byte[] expectedBytes, final byte[] actualBytes) {
        final double v = Math.ceil(expectedBytes.length / (double) blockSize) * blockSize;
        assertEquals((long) v, actualBytes.length, "wrong size");
        assertContainsAtOffset("output", expectedBytes, 0, actualBytes);
        for (int i = expectedBytes.length; i < actualBytes.length; i++) {
            assertEquals(0, actualBytes[i], String.format("output[%d]", i));

        }
    }
}
