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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Test;
import org.mockito.internal.matchers.GreaterOrEqual;

public class FixedLengthBlockOutputStreamTest {

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
    public void testWriteSingleBytes() throws IOException {
        int blockSize = 4;
        MockWritableByteChannel mock = new MockWritableByteChannel(blockSize, false);
        ByteArrayOutputStream bos = mock.bos;
        String text = "hello world avengers";
        byte msg[] = text.getBytes();
        int len = msg.length;
        try (FixedLengthBlockOutputStream out = new FixedLengthBlockOutputStream(mock, blockSize)) {
            for (int i = 0; i < len; i++) {
                out.write(msg[i]);
            }
        }
        byte[] output = bos.toByteArray();

        validate(blockSize, msg, output);
    }


    @Test
    public void testWriteBuf() throws IOException {
        String hwa = "hello world avengers";
        testBuf(4, hwa);
        testBuf(512, hwa);
        testBuf(10240, hwa);
        testBuf(11, hwa + hwa + hwa);
    }

    @Test
    public void testMultiWriteBuf() throws IOException {
        int blockSize = 13;
        MockWritableByteChannel mock = new MockWritableByteChannel(blockSize, false);
        String testString = "hello world";
        byte msg[] = testString.getBytes();
        int reps = 17;

        try (FixedLengthBlockOutputStream out = new FixedLengthBlockOutputStream(mock, blockSize)) {
            for (int i = 0; i < reps; i++) {
                ByteBuffer buf = getByteBuffer(msg);
                out.write(buf);
            }
        }
        ByteArrayOutputStream bos = mock.bos;
        double v = Math.ceil((reps * msg.length) / (double) blockSize) * blockSize;
        assertEquals("wrong size", (long) v, bos.size());
        int strLen = msg.length * reps;
        byte[] output = bos.toByteArray();
        String l = new String(output, 0, strLen);
        StringBuilder buf = new StringBuilder(strLen);
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
        try {
            testWriteAndPad(512, "hello world!\n", true);
            fail("Exception for partial write not thrown");
        } catch (IOException e) {
            String msg = e.getMessage();
            assertEquals("exception message",
                "Failed to write 512 bytes atomically. Only wrote  511", msg);
        }

    }

    @Test
    public void testWriteFailsAfterFLClosedThrowsException() {
        try {
            FixedLengthBlockOutputStream out = getClosedFLBOS();
            out.write(1);
            fail("expected Closed Channel Exception");
        } catch (IOException e) {
            assertThat(e, IsInstanceOf.instanceOf(ClosedChannelException.class));
            // expected
        }
        try {
            FixedLengthBlockOutputStream out = getClosedFLBOS();
            out.write(new byte[] {0,1,2,3});
            fail("expected Closed Channel Exception");
        } catch (IOException e) {
            assertThat(e, IsInstanceOf.instanceOf(ClosedChannelException.class));
            // expected
        }

        try {
            FixedLengthBlockOutputStream out = getClosedFLBOS();
            out.write(ByteBuffer.wrap(new byte[] {0,1,2,3}));
            fail("expected Closed Channel Exception");
        } catch (IOException e) {
            assertThat(e, IsInstanceOf.instanceOf(ClosedChannelException.class));
            // expected
        }

    }

    private FixedLengthBlockOutputStream getClosedFLBOS() throws IOException {
        int blockSize = 512;
        FixedLengthBlockOutputStream out = new FixedLengthBlockOutputStream(
            new MockOutputStream(blockSize, false), blockSize);
        out.write(1);
        assertTrue(out.isOpen());
        out.close();
        assertFalse(out.isOpen());
        return out;
    }

    @Test
    public void testWriteFailsAfterDestClosedThrowsException() {
        int blockSize = 2;
        MockOutputStream mock = new MockOutputStream(blockSize, false);
        FixedLengthBlockOutputStream out =
            new FixedLengthBlockOutputStream(mock, blockSize);
        try {
            out.write(1);
            assertTrue(out.isOpen());
            mock.close();
            out.write(1);
            fail("expected IO Exception");
        } catch (IOException e) {
            // expected
        }
        assertFalse(out.isOpen());
    }

    @Test
    public void testWithFileOutputStream() throws IOException {
        final Path tempFile = Files.createTempFile("xxx", "yyy");
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                }
            }
        });
        int blockSize = 512;
        int reps = 1000;
        OutputStream os = new FileOutputStream(tempFile.toFile());
        try (FixedLengthBlockOutputStream out = new FixedLengthBlockOutputStream(
            os, blockSize)) {
            DataOutputStream dos = new DataOutputStream(out);
            for (int i = 0; i < reps; i++) {
               dos.writeInt(i);
            }
        }
        long expectedDataSize = reps * 4L;
        long expectedFileSize = (long)Math.ceil(expectedDataSize/(double)blockSize)*blockSize;
        assertEquals("file size",expectedFileSize, Files.size(tempFile));
        DataInputStream din = new DataInputStream(Files.newInputStream(tempFile));
        for(int i=0;i<reps;i++) {
            assertEquals("file int",i,din.readInt());
        }
        for(int i=0;i<expectedFileSize - expectedDataSize;i++) {
            assertEquals(0,din.read());
        }
        assertEquals(-1,din.read());
    }

    private void testBuf(int blockSize, String text) throws IOException {
        MockWritableByteChannel mock = new MockWritableByteChannel(blockSize, false);

        ByteArrayOutputStream bos = mock.bos;
        byte msg[] = text.getBytes();
        ByteBuffer buf = getByteBuffer(msg);
        try (FixedLengthBlockOutputStream out = new FixedLengthBlockOutputStream(mock, blockSize)) {
            out.write(buf);
        }
        double v = Math.ceil(msg.length / (double) blockSize) * blockSize;
        assertEquals("wrong size", (long) v, bos.size());
        byte[] output = bos.toByteArray();
        String l = new String(output, 0, msg.length);
        assertEquals(text, l);
        for (int i = msg.length; i < bos.size(); i++) {
            assertEquals(String.format("output[%d]", i), 0, output[i]);

        }
    }

    private ByteBuffer getByteBuffer(byte[] msg) {
        int len = msg.length;
        ByteBuffer buf = ByteBuffer.allocate(len);
        buf.put(msg);
        buf.flip();
        return buf;
    }


    private void testWriteAndPad(int blockSize, String text, boolean doPartialWrite)
        throws IOException {
        MockWritableByteChannel mock = new MockWritableByteChannel(blockSize, doPartialWrite);
        byte[] msg = text.getBytes(StandardCharsets.US_ASCII);

        ByteArrayOutputStream bos = mock.bos;
        try (FixedLengthBlockOutputStream out = new FixedLengthBlockOutputStream(mock, blockSize)) {

            out.write(msg);
            assertEquals("no partial write", (msg.length / blockSize) * blockSize, bos.size());
        }
        validate(blockSize, msg, bos.toByteArray());
    }

    private void testWriteAndPadToStream(int blockSize, String text, boolean doPartialWrite)
        throws IOException {
        MockOutputStream mock = new MockOutputStream(blockSize, doPartialWrite);
        byte[] msg = text.getBytes(StandardCharsets.US_ASCII);

        ByteArrayOutputStream bos = mock.bos;
        try (FixedLengthBlockOutputStream out = new FixedLengthBlockOutputStream(mock, blockSize)) {
            out.write(msg);
            assertEquals("no partial write", (msg.length / blockSize) * blockSize, bos.size());
        }
        validate(blockSize, msg, bos.toByteArray());

    }


    private void validate(int blockSize, byte[] expectedBytes, byte[] actualBytes) {
        double v = Math.ceil(expectedBytes.length / (double) blockSize) * blockSize;
        assertEquals("wrong size", (long) v, actualBytes.length);
        assertContainsAtOffset("output", expectedBytes, 0, actualBytes);
        for (int i = expectedBytes.length; i < actualBytes.length; i++) {
            assertEquals(String.format("output[%d]", i), 0, actualBytes[i]);

        }
    }

    private static void assertContainsAtOffset(String msg, byte[] expected, int offset,
        byte[] actual) {
        assertThat(actual.length, new GreaterOrEqual<>(offset + expected.length));
        for (int i = 0; i < expected.length; i++) {
            assertEquals(String.format("%s ([%d])", msg, i), expected[i], actual[i + offset]);
        }
    }

    private static class MockOutputStream extends OutputStream {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        private final int requiredWriteSize;
        private final boolean doPartialWrite;
        private AtomicBoolean closed = new AtomicBoolean();

        private MockOutputStream(int requiredWriteSize, boolean doPartialWrite) {
            this.requiredWriteSize = requiredWriteSize;
            this.doPartialWrite = doPartialWrite;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            checkIsOpen();
            assertEquals("write size", requiredWriteSize, len);
            if (doPartialWrite) {
                len--;
            }
            bos.write(b, off, len);
        }

        private void checkIsOpen() throws IOException {
            if (closed.get()) {
                IOException e = new IOException("Closed");
                throw e;
            }
        }

        @Override
        public void write(int b) throws IOException {
            checkIsOpen();
            assertEquals("write size", requiredWriteSize, 1);
            bos.write(b);
        }

        @Override
        public void close() throws IOException {
            if (closed.compareAndSet(false, true)) {
                bos.close();
            }
        }
    }

    private static class MockWritableByteChannel implements WritableByteChannel {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        private final int requiredWriteSize;
        private final boolean doPartialWrite;

        private MockWritableByteChannel(int requiredWriteSize, boolean doPartialWrite) {
            this.requiredWriteSize = requiredWriteSize;
            this.doPartialWrite = doPartialWrite;
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            assertEquals("write size", requiredWriteSize, src.remaining());
            if (doPartialWrite) {
                src.limit(src.limit() - 1);
            }
            int bytesOut = src.remaining();
            while (src.hasRemaining()) {
                bos.write(src.get());
            }
            return bytesOut;
        }

        AtomicBoolean closed = new AtomicBoolean();

        @Override
        public boolean isOpen() {
            return !closed.get();
        }

        @Override
        public void close() throws IOException {
            closed.compareAndSet(false, true);
        }
    }
}
