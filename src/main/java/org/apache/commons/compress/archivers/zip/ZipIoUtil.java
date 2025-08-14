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
import java.nio.channels.WritableByteChannel;

import org.apache.commons.compress.archivers.ArchiveException;

/**
 * IO utilities for Zip operations.
 */
// Keep package-private; consider for Apache Commons IO.
final class ZipIoUtil {

    /**
     * Writes all bytes in a buffer to a channel at specified position.
     *
     * @param channel  The target channel.
     * @param buffer   The source bytes.
     * @param position The file position at which the transfer is to begin; must be non-negative
     * @throws IOException If some I/O error occurs or fails to write all bytes.
     */
    static void writeAll(final FileChannel channel, final ByteBuffer buffer, final long position) throws IOException {
        for (long currentPos = position; buffer.hasRemaining();) {
            final int remaining = buffer.remaining();
            final int written = channel.write(buffer, currentPos);
            if (written == 0) {
                // A non-blocking channel
                Thread.yield();
                continue;
            }
            if (written < 0) {
                throw new ArchiveException("Failed to write all bytes in the buffer for channel = %s, length = %,d, written = %,d", channel, remaining,
                        written);
            }
            currentPos += written;
        }
    }

    /**
     * Writes all bytes in a buffer to a channel.
     *
     * @param channel The target channel.
     * @param buffer  The source bytes.
     * @throws IOException If some I/O error occurs or fails to write all bytes.
     */
    static void writeAll(final WritableByteChannel channel, final ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            final int remaining = buffer.remaining();
            final int written = channel.write(buffer);
            if (written == 0) {
                // A non-blocking channel
                Thread.yield();
                continue;
            }
            if (written < 0) {
                throw new ArchiveException("Failed to write all bytes in the buffer for channel = %s, length = %,d, written = %,d", channel, remaining,
                        written);
            }
        }
    }

    private ZipIoUtil() {
    }
}
