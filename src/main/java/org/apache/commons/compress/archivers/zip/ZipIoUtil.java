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

/**
 * IO utilities for Zip operations.
 */
// Keep package-private; consider for Apache Commons IO.
final class ZipIoUtil {

    /**
     * Writes full buffer to channel.
     *
     * @param channel channel to write to
     * @param buffer  buffer to write
     * @throws IOException when writing fails or fails to write fully
     */
    static void writeFully(final WritableByteChannel channel, final ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            final int remaining = buffer.remaining();
            final int written = channel.write(buffer);
            if (written <= 0) {
                throw new IOException("Failed to write all bytes in the buffer for channel=" + channel + ", length=" + remaining + ", written=" + written);
            }
        }
    }

    /**
     * Writes full buffer to channel at specified position.
     *
     * @param channel  channel to write to
     * @param buffer   buffer to write
     * @param position position to write at
     * @throws IOException when writing fails or fails to write fully
     */
    static void writeFullyAt(final FileChannel channel, final ByteBuffer buffer, final long position) throws IOException {
        for (long currentPosition = position; buffer.hasRemaining();) {
            final int remaining = buffer.remaining();
            final int written = channel.write(buffer, currentPosition);
            if (written <= 0) {
                throw new IOException("Failed to write all bytes in the buffer for channel=" + channel + ", length=" + remaining + ", written=" + written);
            }
            currentPosition += written;
        }
    }

    private ZipIoUtil() {
    }
}
