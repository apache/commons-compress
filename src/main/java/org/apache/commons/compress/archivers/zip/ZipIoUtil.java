/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import java.nio.channels.SeekableByteChannel;


/**
 * IO utilities for Zip operations.
 *
 * Package private to potentially move to something reusable.
 */
class ZipIoUtil {
    /**
     * Writes full buffer to channel.
     *
     * @param channel
     *      channel to write to
     * @param buf
     *      buffer to write
     * @throws IOException
     *      when writing fails or fails to write fully
     */
    static void writeFully(final SeekableByteChannel channel, final ByteBuffer buf) throws IOException {
        while (buf.hasRemaining()) {
            final int remaining = buf.remaining();
            final int written = channel.write(buf);
            if (written <= 0) {
                throw new IOException("Failed to fully write: channel=" + channel + " length=" + remaining + " written=" + written);
            }
        }
    }

    /**
     * Writes full buffer to channel at specified position.
     *
     * @param channel
     *      channel to write to
     * @param buf
     *      buffer to write
     * @param position
     *      position to write at
     * @throws IOException
     *      when writing fails or fails to write fully
     */
    static void writeFullyAt(final FileChannel channel, final ByteBuffer buf, final long position) throws IOException {
        for (long currentPosition = position; buf.hasRemaining(); ) {
            final int remaining = buf.remaining();
            final int written = channel.write(buf, currentPosition);
            if (written <= 0) {
                throw new IOException("Failed to fully write: channel=" + channel + " length=" + remaining + " written=" + written);
            }
            currentPosition += written;
        }
    }

    private ZipIoUtil() {
    }
}
