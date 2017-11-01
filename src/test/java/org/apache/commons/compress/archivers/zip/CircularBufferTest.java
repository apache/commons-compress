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

import static org.junit.Assert.*;

import org.junit.Test;

public class CircularBufferTest {

    @Test
    public void testPutAndGet() throws Exception {
        final int size = 16;
        final CircularBuffer buffer = new CircularBuffer(size);
        for (int i = 0; i < size / 2; i++) {
            buffer.put(i);
        }

        assertTrue("available", buffer.available());

        for (int i = 0; i < size / 2; i++) {
            assertEquals("buffer[" + i + "]", i, buffer.get());
        }

        assertEquals(-1, buffer.get());
        assertFalse("available", buffer.available());
    }

    @Test
    public void testCopy() throws Exception {
        final CircularBuffer buffer = new CircularBuffer(16);

        buffer.put(1);
        buffer.put(2);
        buffer.get();
        buffer.get();

        // copy uninitialized data
        buffer.copy(6, 8);

        for (int i = 2; i < 6; i++) {
            assertEquals("buffer[" + i + "]", 0, buffer.get());
        }
        assertEquals("buffer[" + 6 + "]", 1, buffer.get());
        assertEquals("buffer[" + 7 + "]", 2, buffer.get());
        assertEquals("buffer[" + 8 + "]", 0, buffer.get());
        assertEquals("buffer[" + 9 + "]", 0, buffer.get());

        for (int i = 10; i < 14; i++) {
            buffer.put(i);
            buffer.get();
        }

        assertFalse("available", buffer.available());

        // copy data and wrap
        buffer.copy(2, 8);

        for (int i = 14; i < 18; i++) {
            assertEquals("buffer[" + i + "]", i % 2 == 0 ? 12 : 13, buffer.get());
        }
    }
}
