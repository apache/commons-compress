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
 *
 */
package org.apache.commons.compress.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * NIO backed bounded input stream for reading a predefined amount of data from.
 * @ThreadSafe this base class is thread safe but implementations must not be.
 * @since 1.21
 */
public abstract class BoundedArchiveInputStream extends InputStream {

    private final long end;
    private ByteBuffer singleByteBuffer;
    private long loc;

    /**
     * Create a new bounded input stream.
     *
     * @param start     position in the stream from where the reading of this bounded stream starts.
     * @param remaining amount of bytes which are allowed to read from the bounded stream.
     */
    public BoundedArchiveInputStream(final long start, final long remaining) {
        this.end = start + remaining;
        if (this.end < start) {
            // check for potential vulnerability due to overflow
            throw new IllegalArgumentException("Invalid length of stream at offset=" + start + ", length=" + remaining);
        }
        loc = start;
    }

    @Override
    public synchronized int read() throws IOException {
        if (loc >= end) {
            return -1;
        }
        if (singleByteBuffer == null) {
            singleByteBuffer = ByteBuffer.allocate(1);
        } else {
            singleByteBuffer.rewind();
        }
        final int read = read(loc, singleByteBuffer);
        if (read < 1) {
            return -1;
        }
        loc++;
        return singleByteBuffer.get() & 0xff;
    }

    @Override
    public synchronized int read(final byte[] b, final int off, final int len) throws IOException {
        if (loc >= end) {
            return -1;
        }
        final long maxLen = Math.min(len, end - loc);
        if (maxLen <= 0) {
            return 0;
        }
        if (off < 0 || off > b.length || maxLen > b.length - off) {
            throw new IndexOutOfBoundsException("offset or len are out of bounds");
        }

        final ByteBuffer buf = ByteBuffer.wrap(b, off, (int) maxLen);
        final int ret = read(loc, buf);
        if (ret > 0) {
            loc += ret;
        }
        return ret;
    }

    /**
     * Read content of the stream into a {@link ByteBuffer}.
     * @param pos position to start the read.
     * @param buf buffer to add the read content.
     * @return number of read bytes.
     * @throws IOException if I/O fails.
     */
    protected abstract int read(long pos, ByteBuffer buf) throws IOException;
}
