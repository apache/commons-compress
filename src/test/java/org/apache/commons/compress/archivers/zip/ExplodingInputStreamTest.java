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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

public class ExplodingInputStreamTest extends AbstractTest {

    @Test
    void testBinaryTreeDecode() throws IOException {
        final InputStream in = new ByteArrayInputStream(toByteArray(0x02, 0x42, 0x01, 0x13));

        final BinaryTree tree = ExplodingInputStream.readBinaryTree(in, 8);

        assertNotNull(tree);

        try (BitStream stream = createBitStream(0x8D, 0xC5, 0x11, 0x00)) {
            assertEquals(0, tree.read(stream));
            assertEquals(1, tree.read(stream));
            assertEquals(2, tree.read(stream));
            assertEquals(3, tree.read(stream));
            assertEquals(4, tree.read(stream));
            assertEquals(5, tree.read(stream));
            assertEquals(6, tree.read(stream));
            assertEquals(7, tree.read(stream));
        }
    }

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

    @Test
    void testInputStreamStatistics() throws IOException {
        // This will decompress to a string of 1024 'A' characters
        final int[] compressedData = {
            0x0d, 0x02, 0x01, 0x12, 0x23, 0x14, 0x15, 0x36, 0x37, 0x68, 0x89, 0x9a, 0xdb, 0x3c, 0x05, 0x06,
            0x12, 0x13, 0x44, 0xc5, 0xf6, 0x96, 0xf7, 0x83, 0x06, 0x09, 0x8e, 0xfe, 0x0b, 0x12, 0xfa, 0x2f,
            0x70, 0xd0, 0x7f, 0x81, 0x0e, 0x80, 0x02,
        };

        try (ExplodingInputStream in = new ExplodingInputStream(4096, 2, new ByteArrayInputStream(toByteArray(compressedData)))) {
            final byte[] decompressedData = IOUtils.toByteArray(in);
            assertEquals(1024, decompressedData.length);
            for (int i = 0; i < decompressedData.length; i++) {
                assertEquals('A', decompressedData[i], "Byte at position " + i);
            }
            assertEquals(39, in.getCompressedCount());
            assertEquals(1024, in.getUncompressedCount());
        }
    }

    @Test
    void testReadCodeLengths() throws IOException {
        final InputStream in = new ByteArrayInputStream(toByteArray(0x02, 0x42, 0x01, 0x13));

        final int[] codeLengths = ExplodingInputStream.readCodeLengths(in, 8);
        assertEquals(8, codeLengths.length);
        assertArrayEquals(new int[] { 3, 3, 3, 3, 3, 2, 4, 4 }, codeLengths);
    }

    @Test
    void testReadCodeLengthsThrowsArchiveExceptionForEmptyStream() {
        final InputStream is = new ByteArrayInputStream(new byte[] {});
        final ArchiveException e = assertThrows(ArchiveException.class, () -> ExplodingInputStream.readCodeLengths(is, 8));
        assertEquals("Cannot read the size of the encoded tree, unexpected end of stream", e.getMessage());
    }

    @Test
    void testReadCodeLengthsThrowsArchiveExceptionForTooFewCodeLengths() {
        final InputStream is = new ByteArrayInputStream(toByteArray(0x01, 0x52, 0x01));
        final ArchiveException e = assertThrows(ArchiveException.class, () -> ExplodingInputStream.readCodeLengths(is, 8));
        assertEquals("Number of values does not match given total number of values", e.getMessage());
    }

    @Test
    void testReadCodeLengthsThrowsArchiveExceptionForTooManyCodeLengths() {
        final InputStream is = new ByteArrayInputStream(toByteArray(0x01, 0x72, 0x01));
        final ArchiveException e = assertThrows(ArchiveException.class, () -> ExplodingInputStream.readCodeLengths(is, 8));
        assertEquals("Number of values exceeds given total number of values", e.getMessage());
    }

    @Test
    void testReadCodeLengthsThrowsEOFExceptionForTruncatedStream() {
        final InputStream is = new ByteArrayInputStream(toByteArray(0x08, 0x00, 0x01));
        assertThrows(EOFException.class, () -> ExplodingInputStream.readCodeLengths(is, 64));
    }

    @Test
    void testReadCodeLengthsThrowsIllegalArgumentExceptionForNegativeNumberOfCodeLengths() {
        final InputStream is = new ByteArrayInputStream(new byte[] {});
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> ExplodingInputStream.readCodeLengths(is, -1));
        assertEquals("numberOfCodeLengths must be bigger than 0, is -1", e.getMessage());
    }

    @Test
    void testReadCodeLengthsThrowsIllegalArgumentExceptionForZeroNumberOfCodeLengths() {
        final InputStream is = new ByteArrayInputStream(new byte[] {});
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> ExplodingInputStream.readCodeLengths(is, 0));
        assertEquals("numberOfCodeLengths must be bigger than 0, is 0", e.getMessage());
    }

    private BitStream createBitStream(final int... data) throws IOException {
        return new BitStream(new ByteArrayInputStream(toByteArray(data)));
    }
}
