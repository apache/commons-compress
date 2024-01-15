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
import java.nio.channels.FileChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;


/**
 * {@link RandomAccessOutputStream} implementation based on Path file.
 */
class FileRandomAccessOutputStream extends RandomAccessOutputStream {

    private final FileChannel channel;

    private long position;

    FileRandomAccessOutputStream(final Path file) throws IOException {
        this(file, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }

    FileRandomAccessOutputStream(final Path file, OpenOption... options) throws IOException {
        this(FileChannel.open(file, options));
    }

    FileRandomAccessOutputStream(final FileChannel channel) throws IOException {
        this.channel = channel;
    }

    FileChannel channel() {
        return channel;
    }

    @Override
    public synchronized void write(final byte[] b, final int off, final int len) throws IOException {
        ZipIoUtil.writeFully(this.channel, ByteBuffer.wrap(b, off, len));
        position += len;
    }

    @Override
    public synchronized long position() {
        return position;
    }

    @Override
    public void writeFullyAt(final byte[] b, final int off, final int len, final long atPosition) throws IOException {
        ByteBuffer buf = ByteBuffer.wrap(b, off, len);
        for (long currentPos = atPosition; buf.hasRemaining(); ) {
            int written = this.channel.write(buf, currentPos);
            if (written <= 0) {
                throw new IOException("Failed to fully write to file: written=" + written);
            }
            currentPos += written;
        }
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }
}
