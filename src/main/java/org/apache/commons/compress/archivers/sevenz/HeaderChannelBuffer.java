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
package org.apache.commons.compress.archivers.sevenz;

import org.apache.commons.compress.utils.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.zip.CRC32;

/**
 * Enables little-endian primitive type reads from a {@link ReadableByteChannel}
 * or {@link InputStream}, internally using a paged-in {@link ByteBuffer}.
 * <br>
 * Access is serial only but does allow a
 * virtual buffer capacity of {@code Long.MAX_VALUE}.
 * If the requested capacity is within the maximum page size (default 16MiB)
 * the buffer will be fully read and held in a {@link HeaderInMemoryBuffer}.
 *
 * @NotThreadSafe
 * @since 1.21
 */
class HeaderChannelBuffer implements HeaderBuffer {
    private static final int DEFAULT_PAGE_MAX = 16_777_216;
    // This must match the largest get<Element> (currently getLong)
    private static final int MAX_GET_ELEMENT_SIZE = 8;
    private final ReadableByteChannel channel;
    private final ByteBuffer buffer;
    private long remaining;

    private HeaderChannelBuffer(final ReadableByteChannel channel, final long capacity, final int maxPageBytes) {
        this.channel = channel;
        int limit = (int) Math.min(maxPageBytes, capacity);
        this.buffer = ByteBuffer.allocate(limit).order(ByteOrder.LITTLE_ENDIAN);
        this.remaining = capacity;
    }

    public static HeaderBuffer create(final ReadableByteChannel channel, final long capacity, final int maxPageBytes)
            throws IOException {
        if (maxPageBytes < MAX_GET_ELEMENT_SIZE) {
            throw new IllegalArgumentException("Page size must be at least " + MAX_GET_ELEMENT_SIZE);
        }
        if (capacity <= maxPageBytes) {
            ByteBuffer buf = ByteBuffer.allocate((int) capacity).order(ByteOrder.LITTLE_ENDIAN);
            IOUtils.readFully(channel, buf);
            buf.flip();
            return new HeaderInMemoryBuffer(buf);
        }
        HeaderChannelBuffer channelBuffer = new HeaderChannelBuffer(channel, capacity, maxPageBytes);
        channelBuffer.fill();
        return channelBuffer;
    }

    public static HeaderBuffer create(final ReadableByteChannel channel, final long capacity) throws IOException {
        return HeaderChannelBuffer.create(channel, capacity, DEFAULT_PAGE_MAX);
    }

    public static HeaderBuffer create(final InputStream inputStream, final long capacity, final int maxPageBytes)
            throws IOException {
        return create(Channels.newChannel(inputStream), capacity, maxPageBytes);
    }

    public static HeaderBuffer create(final InputStream inputStream, final long capacity) throws IOException {
        return create(Channels.newChannel(inputStream), capacity, DEFAULT_PAGE_MAX);
    }

    @Override
    public boolean hasCRC() {
        return false;
    }

    @Override
    public CRC32 getCRC() throws IOException {
        throw new IOException("CRC is not implemented for this header type");
    }

    @Override
    public void get(byte[] dst) throws IOException {
        int remainingBytes = dst.length;
        do {
            int length = Math.min(buffer.remaining(), remainingBytes);
            buffer.get(dst, dst.length - remainingBytes, length);
            remainingBytes -= length;
        } while (refilled(remainingBytes));
    }

    private boolean refilled(final int remainingBytes) throws IOException {
        if (remainingBytes <= 0) {
            return false;
		}
        if (remainingBytes > this.remaining) {
            throw new BufferUnderflowException();
		}
        buffer.clear();
        this.fill();
        return true;
    }

    @Override
    public int getInt() throws IOException {
        compactAndFill();
        return buffer.getInt();
    }

    @Override
    public long getLong() throws IOException {
        compactAndFill();
        return buffer.getLong();
    }

    @Override
    public int getUnsignedByte() throws IOException {
        compactAndFill();
        return buffer.get() & 0xff;
    }

    @Override
    public long skipBytesFully(long bytesToSkip) throws IOException {
        if (bytesToSkip <= 0) {
            return 0;
        }
        int current = buffer.position();
        long length = buffer.remaining();
        if (bytesToSkip <= length) {
            buffer.position(current + (int) bytesToSkip);
        } else {
            long maxSkip = remaining + length;
            bytesToSkip = Math.min(bytesToSkip, maxSkip);
            while (length < bytesToSkip) {
                buffer.clear();
                fill();
                length += buffer.limit();
            }
            buffer.position(buffer.limit() - (int) (length - bytesToSkip));
        }
        return bytesToSkip;
    }

    private void compactAndFill() throws IOException {
        if (buffer.remaining() <= MAX_GET_ELEMENT_SIZE) {
            buffer.compact();
            this.fill();
        }
    }

    private void fill() throws IOException {
        if (buffer.remaining() > remaining) {
            buffer.limit(buffer.position() + (int) remaining);
		}
        remaining -= buffer.remaining();
        IOUtils.readFully(channel, buffer);
        buffer.flip();
    }
}
