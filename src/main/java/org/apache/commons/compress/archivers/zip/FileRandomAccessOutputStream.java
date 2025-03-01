/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.compress.archivers.zip;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * {@link RandomAccessOutputStream} implementation based on a file.
 */
// Keep package-private; consider for Apache Commons IO.
final class FileRandomAccessOutputStream extends RandomAccessOutputStream {

    private final FileChannel channel;

    private long position;

    FileRandomAccessOutputStream(final FileChannel channel) {
        this.channel = Objects.requireNonNull(channel, "channel");
    }

    FileRandomAccessOutputStream(final Path file) throws IOException {
        this(file, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }

    FileRandomAccessOutputStream(final Path file, final OpenOption... options) throws IOException {
        this(FileChannel.open(file, options));
    }

    FileChannel channel() {
        return channel;
    }

    @Override
    public void close() throws IOException {
        if (channel.isOpen()) {
            channel.close();
        }
    }

    @Override
    public synchronized long position() {
        return position;
    }

    @Override
    public synchronized void write(final byte[] b, final int off, final int len) throws IOException {
        ZipIoUtil.writeAll(this.channel, ByteBuffer.wrap(b, off, len));
        position += len;
    }

    @Override
    public void writeFully(final byte[] b, final int off, final int len, final long atPosition) throws IOException {
        final ByteBuffer buf = ByteBuffer.wrap(b, off, len);
        for (long currentPos = atPosition; buf.hasRemaining();) {
            final int written = this.channel.write(buf, currentPos);
            if (written <= 0) {
                throw new IOException("Failed to fully write to file: written=" + written);
            }
            currentPos += written;
        }
    }
}
