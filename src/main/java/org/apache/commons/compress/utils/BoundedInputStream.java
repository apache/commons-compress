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

/**
 * A stream that limits reading from a wrapped stream to a given number of bytes.
 * @NotThreadSafe
 * @since 1.6
 */
public class BoundedInputStream extends InputStream {
    private final InputStream in;
    private long bytesRemaining;
    
    /**
     * Creates the stream that will at most read the given amount of
     * bytes from the given stream.
     * @param in the stream to read from
     * @param size the maximum amount of bytes to read
     */
    public BoundedInputStream(final InputStream in, final long size) {
        this.in = in;
        bytesRemaining = size;
    }
    
    @Override
    public int read() throws IOException {
        if (bytesRemaining > 0) {
            --bytesRemaining;
            return in.read();
        } else {
            return -1;
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
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

    @Override
    public void close() {
        // there isn't anything to close in this stream and the nested
        // stream is controlled externally
    }
}
