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
import java.util.Arrays;

/**
 * A {@link SeekableByteChannel} implementation that wraps a byte[].
 * @since 1.13
 */
public class SeekableInMemoryByteChannel implements SeekableByteChannel {

    private volatile byte[] data;
    private volatile boolean closed;
    private volatile int position, size;

    public SeekableInMemoryByteChannel(byte[] data) {
        this.data = data;
        size = data.length;
    }

    public SeekableInMemoryByteChannel() {
        this(new byte[0]);
    }

    @Override
    public long position() {
        return position;
    }

    @Override
    public SeekableByteChannel position(long newPosition) {
        if (newPosition > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Position cannot exceed " + Integer.MAX_VALUE);
        }
        position = (int) newPosition;
        return this;
    }

    @Override
    public long size() {
        return size;
    }

    @Override
    public SeekableByteChannel truncate(long newSize) {
        if (size > newSize) {
            size = (int) newSize;
        }
        if (position > size) {
            position = size;
        }
        return this;
    }

    @Override
    public int read(ByteBuffer buf) throws IOException {
        if (!isOpen()) {
            throw new ClosedChannelException();
        }
        int pos = position;
        int sz = size;
        int wanted = buf.remaining();
        int possible = sz - pos;
        if (wanted > possible) {
            wanted = possible;
        }
        buf.put(data, pos, wanted);
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

    @Override
    public int write(ByteBuffer b) throws IOException {
        if (!isOpen()) {
            throw new ClosedChannelException();
        }
        int pos = position;
        int sz = data.length;
        int wanted = b.remaining();
        int possibleWithoutResize = sz - pos;
        if (wanted > possibleWithoutResize) {
            resize(pos + wanted);
        }
        b.get(data, pos, wanted);
        position = pos + wanted;
        if (size < position) {
            size = position;
        }
        return wanted;
    }

    /**
     * Obtains the array backing this channel.
     */
    public byte[] array() {
        return data;
    }

    private void resize(int newLength) {
        int len = data.length;
        if (len <= 0) {
            len = 1;
        }
        while (len < newLength) {
            len <<= 1;
        }
        data = Arrays.copyOf(data, len);
    }

}
