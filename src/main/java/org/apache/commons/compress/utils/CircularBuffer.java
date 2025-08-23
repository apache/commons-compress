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

package org.apache.commons.compress.utils;

/**
 * Circular byte buffer.
 *
 * @since 1.29.0
 */
public final class CircularBuffer {

    /** Size of the buffer */
    private final int size;

    /** The buffer */
    private final byte[] buffer;

    /** Index of the next data to be read from the buffer */
    private int readIndex;

    /** Index of the next data written in the buffer */
    private int writeIndex;

    private int bytesAvailable;

    public CircularBuffer(final int size) {
        this.size = size;
        buffer = new byte[size];
        bytesAvailable = 0;
    }

    /**
     * Tests whether a new byte can be read from the buffer.
     *
     * @return Whether a new byte can be read from the buffer.
     */
    public boolean available() {
        return bytesAvailable > 0;
    }

    /**
     * Copies a previous interval in the buffer to the current position.
     *
     * @param distance the distance from the current write position
     * @param length   the number of bytes to copy
     */
    public void copy(final int distance, final int length) {
        if (distance < 1) {
            throw new IllegalArgumentException("Distance must be at least 1");
        } else if (distance > size) {
            throw new IllegalArgumentException("Distance exceeds buffer size");
        }
        final int pos1 = writeIndex - distance;
        final int pos2 = pos1 + length;
        for (int i = pos1; i < pos2; i++) {
            put(buffer[(i + size) % size]);
        }
    }

    /**
     * Reads a byte from the buffer.
     *
     * @return a byte from the buffer.
     */
    public int get() {
        if (available()) {
            final int value = buffer[readIndex];
            readIndex = (readIndex + 1) % size;
            bytesAvailable--;
            return value & 0xFF;
        }
        return -1;
    }

    /**
     * Puts a byte to the buffer.
     *
     * @param value the value to put.
     */
    public void put(final int value) {
        if (bytesAvailable == size) {
            throw new IllegalStateException("Buffer overflow: Cannot write to a full buffer");
        }
        buffer[writeIndex] = (byte) value;
        writeIndex = (writeIndex + 1) % size;
        bytesAvailable++;
    }
}
