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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A stream that limits reading from a wrapped stream to a given number of bytes.
 *
 * @NotThreadSafe
 * @since 1.6
 */
public class BoundedInputStream extends FilterInputStream {
    private long bytesRemaining;

    /**
     * Creates the stream that will at most read the given amount of bytes from the given stream.
     *
     * @param in   the stream to read from
     * @param size the maximum amount of bytes to read
     */
    public BoundedInputStream(final InputStream in, final long size) {
        super(in);
        bytesRemaining = size;
    }

    @Override
    public void close() {
        // there isn't anything to close in this stream and the nested
        // stream is controlled externally
    }

    /**
     * @return bytes remaining to read
     * @since 1.21
     */
    public long getBytesRemaining() {
        return bytesRemaining;
    }

    @Override
    public int read() throws IOException {
        if (bytesRemaining > 0) {
            --bytesRemaining;
            return in.read();
        }
        return -1;
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        if (len == 0) {
            return 0;
        }
        if (bytesRemaining == 0) {
            return -1;
        }
        int bytesToRead = len;
        if (bytesToRead > bytesRemaining) {
            bytesToRead = (int) bytesRemaining;
        }
        final int bytesRead = in.read(b, off, bytesToRead);
        if (bytesRead >= 0) {
            bytesRemaining -= bytesRead;
        }
        return bytesRead;
    }

    /**
     * @since 1.20
     */
    @Override
    public long skip(final long n) throws IOException {
        final long bytesToSkip = Math.min(bytesRemaining, n);
        final long bytesSkipped = in.skip(bytesToSkip);
        bytesRemaining -= bytesSkipped;

        return bytesSkipped;
    }
}
