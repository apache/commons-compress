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
package org.apache.commons.compress.utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.junit.jupiter.api.Test;

public class CountingStreamTest {

    @Test
    public void input() throws Exception {
        // I don't like "test all at once" tests either, but the class
        // is so trivial
        final ByteArrayInputStream bis =
            new ByteArrayInputStream(new byte[] { 1, 2, 3, 4 });
        try (final CountingInputStream i = new CountingInputStream(bis)) {
            assertEquals(1, i.read());
            assertEquals(1, i.getBytesRead());
            byte[] b = new byte[2];
            i.read(b);
            assertEquals(3, i.getBytesRead());
            assertArrayEquals(new byte[] { 2, 3 }, b);
            b = new byte[3];
            i.read(b, 1, 1);
            assertArrayEquals(new byte[] { 0, 4, 0 }, b);
            assertEquals(4, i.getBytesRead());
            i.count(-1);
            assertEquals(4, i.getBytesRead());
            i.count(-2);
            assertEquals(2, i.getBytesRead());
        }
    }

    @Test
    public void output() throws Exception {
        // I don't like "test all at once" tests either, but the class
        // is so trivial
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (final CountingOutputStream o = new CountingOutputStream(bos)) {
            o.write(1);
            assertEquals(1, o.getBytesWritten());
            o.write(new byte[] { 2, 3 });
            assertEquals(3, o.getBytesWritten());
            o.write(new byte[] { 2, 3, 4, 5, }, 2, 1);
            assertEquals(4, o.getBytesWritten());
            o.count(-1);
            assertEquals(4, o.getBytesWritten());
            o.count(-2);
            assertEquals(2, o.getBytesWritten());
        }
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, bos.toByteArray());
    }

}
