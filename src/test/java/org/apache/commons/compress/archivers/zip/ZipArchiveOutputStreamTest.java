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
package org.apache.commons.compress.archivers.zip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;

import org.apache.commons.compress.AbstractTempDirTest;
import org.apache.commons.compress.archivers.ArchiveException;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link ZipArchiveOutputStream}.
 */
class ZipArchiveOutputStreamTest extends AbstractTempDirTest {

    /**
     * Tests COMPRESS-729: close() must end the Deflater even when finish() fails.
     */
    @Test
    void testCloseEndsDeflaterWhenFinishFails() throws IOException {
        final FailingOutputStream target = new FailingOutputStream();
        final ZipArchiveOutputStream outputStream = new ZipArchiveOutputStream(target);
        outputStream.putArchiveEntry(new ZipArchiveEntry("test.txt"));
        outputStream.write("Hello, World!".getBytes(StandardCharsets.UTF_8));
        outputStream.closeArchiveEntry();
        // For example, the client aborted the download.
        target.fail = true;
        assertThrows(IOException.class, outputStream::close);
        assertDeflaterEnded(outputStream.def);
    }

    /**
     * Tests COMPRESS-729: close() must end the Deflater even when the archive still has an unclosed entry.
     */
    @Test
    void testCloseEndsDeflaterWithUnclosedEntry() throws IOException {
        final ZipArchiveOutputStream outputStream = new ZipArchiveOutputStream(new ByteArrayOutputStream());
        outputStream.putArchiveEntry(new ZipArchiveEntry("test.txt"));
        outputStream.write("Hello, World!".getBytes(StandardCharsets.UTF_8));
        assertThrows(ArchiveException.class, outputStream::close);
        assertDeflaterEnded(outputStream.def);
    }

    private static void assertDeflaterEnded(final Deflater deflater) {
        // Deflater methods throw NullPointerException("Deflater has been closed") once end() was called.
        assertThrows(NullPointerException.class, deflater::getBytesRead);
    }

    private static final class FailingOutputStream extends OutputStream {

        private boolean fail;

        @Override
        public void write(final byte[] b, final int off, final int len) throws IOException {
            if (fail) {
                throw new IOException("Broken pipe");
            }
        }

        @Override
        public void write(final int b) throws IOException {
            write(new byte[] { (byte) b }, 0, 1);
        }
    }

    @Test
    void testFileBasics() throws IOException {
        final ZipArchiveOutputStream ref;
        try (ZipArchiveOutputStream outputStream = new ZipArchiveOutputStream(createTempFile())) {
            ref = outputStream;
            assertTrue(outputStream.isSeekable());
        }
        assertTrue(ref.isClosed());
    }

    @Test
    void testOptionDefaults() throws IOException {
        final ZipArchiveOutputStream ref;
        try (ZipArchiveOutputStream outputStream = new ZipArchiveOutputStream(createTempFile())) {
            ref = outputStream;
            assertTrue(outputStream.isSeekable());
            outputStream.setComment("");
            outputStream.setLevel(Deflater.DEFAULT_COMPRESSION);
            outputStream.setMethod(ZipEntry.DEFLATED);
            outputStream.setFallbackToUTF8(false);
        }
        assertTrue(ref.isClosed());
    }

    @Test
    void testOutputStreamBasics() throws IOException {
        try (ZipArchiveOutputStream outputStream = new ZipArchiveOutputStream(new ByteArrayOutputStream())) {
            assertFalse(outputStream.isSeekable());
        }
    }

    @Test
    void testSetEncoding() throws IOException {
        try (ZipArchiveOutputStream outputStream = new ZipArchiveOutputStream(createTempFile())) {
            outputStream.setEncoding(StandardCharsets.UTF_8.name());
            assertEquals(StandardCharsets.UTF_8.name(), outputStream.getEncoding());
            outputStream.setEncoding(null);
            assertEquals(Charset.defaultCharset().name(), outputStream.getEncoding());
        }
    }
}
