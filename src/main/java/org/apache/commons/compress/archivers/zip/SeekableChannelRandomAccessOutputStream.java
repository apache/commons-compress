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
package org.apache.commons.compress.archivers.zip;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;


/**
 * {@link RandomAccessOutputStream} implementation for SeekableByteChannel.
 */
class SeekableChannelRandomAccessOutputStream extends RandomAccessOutputStream {

    private final SeekableByteChannel channel;

    private long position;

    SeekableChannelRandomAccessOutputStream(final SeekableByteChannel channel) {
        this.channel = channel;
    }

    @Override
    public synchronized void close() throws IOException {
        channel.close();
    }

    @Override
    public synchronized long position() throws IOException {
        return channel.position();
    }

    @Override
    public synchronized void write(final byte[] b, final int off, final int len) throws IOException {
        ZipIoUtil.writeFully(this.channel, ByteBuffer.wrap(b, off, len));
    }

    @Override
    public synchronized void writeFullyAt(final byte[] b, final int off, final int len, final long position) throws IOException {
        final long saved = channel.position();
        try {
            channel.position(position);
            ZipIoUtil.writeFully(channel, ByteBuffer.wrap(b, off, len));
        } finally {
            channel.position(saved);
        }
    }
}
