/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.commons.compress.utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;

/**
 * A {@link SeekableByteChannel} implementation that wraps a byte[].
 */
public class SeekableInMemoryByteChannel implements SeekableByteChannel {

    private final byte[] data;
    private volatile boolean closed;
    private volatile long position, size;

    public SeekableInMemoryByteChannel(byte[] data) {
        this.data = data;
        size = data.length;
    }

    @Override
    public long position() {
        return position;
    }

    @Override
    public SeekableByteChannel position(long newPosition) {
        position = newPosition;
        return this;
    }

    @Override
    public long size() {
        return size;
    }

    @Override
    public SeekableByteChannel truncate(long newSize) {
        size = newSize;
        return this;
    }

    @Override
    public int read(ByteBuffer buf) throws IOException {
        if (!isOpen()) {
            throw new ClosedChannelException();
        }
        long pos = position;
        long sz = size;
        int wanted = buf.remaining();
        long possible = sz - pos;
        if (wanted > possible) {
            wanted = (int) possible;
        }
        buf.put(data, (int) pos, wanted);
        position = pos + wanted;
        return wanted;
    }

    @Override
    public void close() {
        closed = true;
    }

    @Override
    public boolean isOpen() {
        return !closed;
    }

    // TODO implement writing
    @Override
    public int write(ByteBuffer b) throws IOException {
        throw new NonWritableChannelException();
    }
}
