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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

class CircularBufferTest {
    @Test
    void testPutAndGet1() {
        final int size = 16;
        final CircularBuffer buffer = new CircularBuffer(size);
        for (int i = 0; i < size / 2; i++) {
            buffer.put(i);
        }

        assertTrue(buffer.available(), "available");

        for (int i = 0; i < size / 2; i++) {
            assertEquals(i, buffer.get(), "buffer[" + i + "]");
        }

        assertEquals(-1, buffer.get());
        assertFalse(buffer.available(), "available");
    }

    @Test
    void testPutAndGet2() {
        final CircularBuffer buffer = new CircularBuffer(8);

        // Nothing to read
        assertFalse(buffer.available());
        assertEquals(-1, buffer.get());

        // Write a byte and read it
        buffer.put(0x01);
        assertTrue(buffer.available());
        assertEquals(0x01, buffer.get());
        assertFalse(buffer.available());
        assertEquals(-1, buffer.get());

        // Write multiple bytes and read them
        buffer.put(0x02);
        buffer.put(0x03);
        buffer.put(0x04);
        assertTrue(buffer.available());
        assertEquals(0x02, buffer.get());
        assertEquals(0x03, buffer.get());
        assertEquals(0x04, buffer.get());
        assertFalse(buffer.available());
        assertEquals(-1, buffer.get());
    }

    @Test
    void testPutAndGetWrappingAround() {
        final CircularBuffer buffer = new CircularBuffer(4);

        // Nothing to read
        assertFalse(buffer.available());
        assertEquals(-1, buffer.get());

        // Write two bytes and read them in a loop making the buffer wrap around several times
        for (int i = 0; i < 8; i++) {
            buffer.put(i * 2);
            buffer.put(i * 2 + 1);

            assertTrue(buffer.available());
            assertEquals(i * 2, buffer.get());
            assertEquals(i * 2 + 1, buffer.get());
            assertFalse(buffer.available());
            assertEquals(-1, buffer.get());
        }
    }

    @Test
    void testPutOverflow() {
        final CircularBuffer buffer = new CircularBuffer(4);

        // Write more bytes than the buffer can hold
        buffer.put(0x01);
        buffer.put(0x02);
        buffer.put(0x03);
        buffer.put(0x04);

        try {
            buffer.put(0x05);
            fail("Expected IllegalStateException for buffer overflow");
        } catch (IllegalStateException e) {
            assertEquals("Buffer overflow: Cannot write to a full buffer", e.getMessage());
        }
    }

    @Test
    void testCopy1() {
        final CircularBuffer buffer = new CircularBuffer(16);

        buffer.put(1);
        buffer.put(2);
        buffer.get();
        buffer.get();

        // copy uninitialized data
        buffer.copy(6, 8);

        for (int i = 2; i < 6; i++) {
            assertEquals(0, buffer.get(), "buffer[" + i + "]");
        }
        assertEquals(1, buffer.get(), "buffer[" + 6 + "]");
        assertEquals(2, buffer.get(), "buffer[" + 7 + "]");
        assertEquals(0, buffer.get(), "buffer[" + 8 + "]");
        assertEquals(0, buffer.get(), "buffer[" + 9 + "]");

        for (int i = 10; i < 14; i++) {
            buffer.put(i);
            buffer.get();
        }

        assertFalse(buffer.available(), "available");

        // copy data and wrap
        buffer.copy(2, 8);

        for (int i = 14; i < 18; i++) {
            assertEquals(i % 2 == 0 ? 12 : 13, buffer.get(), "buffer[" + i + "]");
        }
    }

    @Test
    void testCopy2() {
        final CircularBuffer buffer = new CircularBuffer(16);

        // Write some bytes
        buffer.put(0x01);
        buffer.put(0x02);
        buffer.put(0x03);
        buffer.put(0x04);

        buffer.copy(2, 2); // Copy last two bytes (0x03, 0x04)

        assertEquals(0x01, buffer.get());
        assertEquals(0x02, buffer.get());
        assertEquals(0x03, buffer.get());
        assertEquals(0x04, buffer.get());
        assertEquals(0x03, buffer.get());
        assertEquals(0x04, buffer.get());

        assertFalse(buffer.available());
        assertEquals(-1, buffer.get());
    }

    @Test
    void testCopy3() {
        final CircularBuffer buffer = new CircularBuffer(16);

        // Write some bytes
        buffer.put(0x01);
        buffer.put(0x02);
        buffer.put(0x03);
        buffer.put(0x04);

        buffer.copy(4, 2); // Copy first two bytes (0x01, 0x02)

        assertEquals(0x01, buffer.get());
        assertEquals(0x02, buffer.get());
        assertEquals(0x03, buffer.get());
        assertEquals(0x04, buffer.get());
        assertEquals(0x01, buffer.get()); // Copied byte
        assertEquals(0x02, buffer.get()); // Copied byte

        assertFalse(buffer.available());
        assertEquals(-1, buffer.get());
    }

    @Test
    void testCopy4() {
        final CircularBuffer buffer = new CircularBuffer(6);

        // Write some bytes
        buffer.put(0x01);
        buffer.put(0x02);
        buffer.put(0x03);
        buffer.put(0x04);
        buffer.put(0x05);
        buffer.put(0x06);

        // Read four bytes to make space
        assertEquals(0x01, buffer.get());
        assertEquals(0x02, buffer.get());
        assertEquals(0x03, buffer.get());
        assertEquals(0x04, buffer.get());

        // Write two more bytes and making the buffer wrap around
        buffer.put(0x07);
        buffer.put(0x08);

        buffer.copy(3, 2); // Copy two bytes from 3 bytes ago (0x06, 0x07) where the buffer wraps around

        // Read rest of the buffer
        assertEquals(0x05, buffer.get());
        assertEquals(0x06, buffer.get());
        assertEquals(0x07, buffer.get());
        assertEquals(0x08, buffer.get());
        assertEquals(0x06, buffer.get()); // Copied byte
        assertEquals(0x07, buffer.get()); // Copied byte

        assertFalse(buffer.available());
        assertEquals(-1, buffer.get());
    }

    @Test
    void testCopyRunLengthEncoding1() {
        final CircularBuffer buffer = new CircularBuffer(16);

        // Write two bytes
        buffer.put(0x01);
        buffer.put(0x02);

        buffer.copy(1, 8); // Copy last byte (0x02) eight times

        // Read the buffer
        assertEquals(0x01, buffer.get());
        assertEquals(0x02, buffer.get());
        assertEquals(0x02, buffer.get()); // Copied byte 1
        assertEquals(0x02, buffer.get()); // Copied byte 2
        assertEquals(0x02, buffer.get()); // Copied byte 3
        assertEquals(0x02, buffer.get()); // Copied byte 4
        assertEquals(0x02, buffer.get()); // Copied byte 5
        assertEquals(0x02, buffer.get()); // Copied byte 6
        assertEquals(0x02, buffer.get()); // Copied byte 7
        assertEquals(0x02, buffer.get()); // Copied byte 8

        assertFalse(buffer.available());
        assertEquals(-1, buffer.get());
    }

    @Test
    void testCopyDistanceInvalid() {
        final CircularBuffer buffer = new CircularBuffer(4);

        // Write some bytes
        buffer.put(0x01);
        buffer.put(0x02);

        try {
            buffer.copy(0, 2); // Try to copy from distance 0
            fail("Expected IllegalArgumentException for invalid distance");
        } catch (IllegalArgumentException e) {
            assertEquals("Distance must be at least 1", e.getMessage());
        }
    }

    @Test
    void testCopyDistanceExceedingBufferSize() {
        final CircularBuffer buffer = new CircularBuffer(4);

        // Write some bytes
        buffer.put(0x01);
        buffer.put(0x02);
        buffer.put(0x03);
        buffer.put(0x04);

        try {
            buffer.copy(5, 2); // Try to copy from a distance that is bigger than the buffer size
            fail("Expected IllegalArgumentException for distance exceeding buffer size");
        } catch (IllegalArgumentException e) {
            assertEquals("Distance exceeds buffer size", e.getMessage());
        }
    }

    @Test
    void testCopyCausingBufferOverflow() {
        final CircularBuffer buffer = new CircularBuffer(4);

        // Write some bytes
        buffer.put(0x01);
        buffer.put(0x02);
        buffer.put(0x03);
        buffer.put(0x04);

        // Read some bytes to make space
        assertEquals(0x01, buffer.get());
        assertEquals(0x02, buffer.get());

        try {
            buffer.copy(4, 4); // Copying 4 bytes and write to the buffer that will be full during copy
            fail("Expected IllegalStateException for buffer overflow during copy");
        } catch (IllegalStateException e) {
            assertEquals("Buffer overflow: Cannot write to a full buffer", e.getMessage());
        }
    }
}
