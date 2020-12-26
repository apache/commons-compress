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
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

/**
 * InputStream that delegates requests to the underlying SeekableByteChannel, making sure that only bytes from a certain
 * range can be read.
 * @ThreadSafe
 * @since 1.21
 */
public class BoundedSeekableByteChannelInputStream extends BoundedArchiveInputStream {

    private final SeekableByteChannel channel;

    /**
     * Create a bounded stream on the underlying {@link SeekableByteChannel}
     *
     * @param start     Position in the stream from where the reading of this bounded stream starts
     * @param remaining Amount of bytes which are allowed to read from the bounded stream
     * @param channel   Channel which the reads will be delegated to
     */
    public BoundedSeekableByteChannelInputStream(final long start, final long remaining,
            final SeekableByteChannel channel) {
        super(start, remaining);
        this.channel = channel;
    }

    @Override
    protected int read(long pos, ByteBuffer buf) throws IOException {
        int read;
        synchronized (channel) {
            channel.position(pos);
            read = channel.read(buf);
        }
        buf.flip();
        return read;
    }
}
