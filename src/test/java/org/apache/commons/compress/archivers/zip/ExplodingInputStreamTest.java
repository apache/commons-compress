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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.AbstractTest;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

public class ExplodingInputStreamTest extends AbstractTest {

    @Test
    void testDecompress() throws Exception {
        try (ZipArchiveInputStream archive = new ZipArchiveInputStream(newInputStream("lorem-ipsum-implode.zip"))) {
            final ZipArchiveEntry entry = archive.getNextEntry();
            assertEquals("LOREM.TXT", entry.getName());
            assertEquals(ZipMethod.IMPLODING, ZipMethod.getMethodByCode(entry.getMethod()));

            final byte[] data = IOUtils.toByteArray(archive);
            assertEquals(144060, data.length);
            assertEquals("a00c4f3f36515c96b2faef71c054e7f3e86a4f0f4ed4824cb7c5293bb455d28a", DigestUtils.sha256Hex(data));
        }
    }

    /**
     * Tests that a stream ending inside the length code of a back reference ends the data, without copying a byte from the empty dictionary.
     */
    @Test
    void testTruncatedBackReferenceLength() throws Exception {
        // two Shannon-Fano trees, each: 64 values with a bit length of 6
        final byte[] tree = { 0x03, (byte) 0xF5, (byte) 0xF5, (byte) 0xF5, (byte) 0xF5 };
        final byte[] data = new byte[2 * tree.length + 2];
        System.arraycopy(tree, 0, data, 0, tree.length);
        System.arraycopy(tree, 0, data, tree.length, tree.length);
        // a back reference: flag bit 0, distance low bits 3, distance high code, then the end of the stream before the length code
        data[2 * tree.length] = (byte) 0x86;
        data[2 * tree.length + 1] = 0x00;
        try (ExplodingInputStream in = new ExplodingInputStream(4096, 2, new ByteArrayInputStream(data))) {
            assertArrayEquals(new byte[0], IOUtils.toByteArray(in));
        }
    }
}
