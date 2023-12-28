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
 */
package org.apache.commons.compress.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.CheckedInputStream;
import java.util.zip.Checksum;

/**
 * Verifies the checksum of the data read once the stream is exhausted.
 *
 * @NotThreadSafe
 * @since 1.7
 */
public class ChecksumVerifyingInputStream extends CheckedInputStream {

    private long remaining;
    private final long expected;

    /**
     * Constructs a new instance.
     *
     * @param checksum         Checksum implementation.
     * @param in               the stream to wrap
     * @param size             the of the stream's content
     * @param expectedChecksum the expected checksum
     */
    public ChecksumVerifyingInputStream(final Checksum checksum, final InputStream in, final long size, final long expectedChecksum) {
        super(in, checksum);
        this.expected = expectedChecksum;
        this.remaining = size;
    }

    /**
     * Gets the byte count remaining to read.
     *
     * @return bytes remaining to read.
     * @since 1.21
     */
    public long getBytesRemaining() {
        return remaining;
    }

    /**
     * Reads a single byte from the stream
     *
     * @throws IOException if the underlying stream throws or the stream is exhausted and the Checksum doesn't match the expected value
     */
    @Override
    public int read() throws IOException {
        if (remaining <= 0) {
            return -1;
        }
        final int data = super.read();
        if (data >= 0) {
            --remaining;
        }
        verify();
        return data;
    }

    /**
     * Reads from the stream into a byte array.
     *
     * @throws IOException if the underlying stream throws or the stream is exhausted and the Checksum doesn't match the expected value
     */
    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        if (len == 0) {
            return 0;
        }
        final int readCount = super.read(b, off, len);
        if (readCount >= 0) {
            remaining -= readCount;
        }
        verify();
        return readCount;
    }

    private void verify() throws IOException {
        if (remaining <= 0 && expected != getChecksum().getValue()) {
            throw new IOException("Checksum verification failed");
        }
    }
}
