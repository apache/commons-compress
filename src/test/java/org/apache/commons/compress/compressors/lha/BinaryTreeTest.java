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

package org.apache.commons.compress.compressors.lha;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteOrder;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.utils.BitInputStream;
import org.junit.jupiter.api.Test;

class BinaryTreeTest {
    @Test
    void testTree1() throws Exception {
        // Special case where the single array value is the root node value
        final BinaryTree tree = new BinaryTree(4);

        assertEquals(4, tree.read(createBitInputStream())); // Nothing to read, just return the root value
    }

    @Test
    void testTree2() throws Exception {
        final int[] length = new int[] { 1, 1 };
        //                        Value: 0  1

        final BinaryTree tree = new BinaryTree(length);

        assertEquals(0, tree.read(createBitInputStream(0x00))); // 0xxx xxxx
        assertEquals(1, tree.read(createBitInputStream(0x80))); // 1xxx xxxx
    }

    @Test
    void testTree3() throws Exception {
        final int[] length = new int[] { 1, 0, 1 };
        //                        Value: 0  1  2

        final BinaryTree tree = new BinaryTree(length);

        assertEquals(0, tree.read(createBitInputStream(0x00))); // 0xxx xxxx
        assertEquals(2, tree.read(createBitInputStream(0x80))); // 1xxx xxxx
    }

    @Test
    void testTree4() throws Exception {
        final int[] length = new int[] { 2, 0, 1, 2 };
        //                        Value: 0  1  2  3

        final BinaryTree tree = new BinaryTree(length);

        assertEquals(2, tree.read(createBitInputStream(0x00))); // 0xxx xxxx
        assertEquals(0, tree.read(createBitInputStream(0x80))); // 10xx xxxx
        assertEquals(3, tree.read(createBitInputStream(0xc0))); // 11xx xxxx
    }

    @Test
    void testTree5() throws Exception {
        final int[] length = new int[] { 2, 0, 0, 2, 1 };
        //                        Value: 0  1  2  3  4

        final BinaryTree tree = new BinaryTree(length);

        assertEquals(4, tree.read(createBitInputStream(0x00))); // 0xxx xxxx
        assertEquals(0, tree.read(createBitInputStream(0x80))); // 10xx xxxx
        assertEquals(3, tree.read(createBitInputStream(0xc0))); // 11xx xxxx
    }

    @Test
    void testTree6() throws Exception {
        final int[] length = new int[] { 1, 0, 2, 3, 3 };
        //                        Value: 0  1  2  3  4

        final BinaryTree tree = new BinaryTree(length);

        assertEquals(0, tree.read(createBitInputStream(0x00))); // 0xxx xxxx
        assertEquals(2, tree.read(createBitInputStream(0x80))); // 10xx xxxx
        assertEquals(3, tree.read(createBitInputStream(0xc0))); // 110x xxxx
        assertEquals(4, tree.read(createBitInputStream(0xe0))); // 111x xxxx
    }

    @Test
    void testTree7() throws Exception {
        final int[] length = new int[] { 0, 0, 0, 0, 1, 1 };
        //                        Value: 0  1  2  3  4  5

        final BinaryTree tree = new BinaryTree(length);

        assertEquals(4, tree.read(createBitInputStream(0x00))); // 0xxx xxxx
        assertEquals(5, tree.read(createBitInputStream(0x80))); // 1xxx xxxx
    }

    @Test
    void testTree8() throws Exception {
        final int[] length = new int[] { 4, 2, 3, 0, 5, 5, 1 };
        //                        Value: 0  1  2  3  4  5  6

        final BinaryTree tree = new BinaryTree(length);

        assertEquals(6, tree.read(createBitInputStream(0x00))); // 0xxx xxxx
        assertEquals(1, tree.read(createBitInputStream(0x80))); // 10xx xxxx
        assertEquals(2, tree.read(createBitInputStream(0xc0))); // 110x xxxx
        assertEquals(0, tree.read(createBitInputStream(0xe0))); // 1110 xxxx
        assertEquals(4, tree.read(createBitInputStream(0xf0))); // 1111 0xxx
        assertEquals(5, tree.read(createBitInputStream(0xf8))); // 1111 1xxx
    }

    @Test
    void testTree9() throws Exception {
        final int[] length = new int[] { 5, 6, 6, 0, 0, 8, 7, 7, 7, 4, 3, 2, 2, 4, 5, 5, 5, 4, 8 };
        //                        Value: 0  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16 17 18

        final BinaryTree tree = new BinaryTree(length);

        assertEquals(11, tree.read(createBitInputStream(0x00))); // 00xx xxxx
        assertEquals(12, tree.read(createBitInputStream(0x40))); // 01xx xxxx
        assertEquals(10, tree.read(createBitInputStream(0x80))); // 100x xxxx
        assertEquals(9, tree.read(createBitInputStream(0xa0)));  // 1010 xxxx
        assertEquals(13, tree.read(createBitInputStream(0xb0))); // 1011 xxxx
        assertEquals(17, tree.read(createBitInputStream(0xc0))); // 1100 xxxx
        assertEquals(0, tree.read(createBitInputStream(0xd0)));  // 1101 0xxx
        assertEquals(14, tree.read(createBitInputStream(0xd8))); // 1101 1xxx
        assertEquals(15, tree.read(createBitInputStream(0xe0))); // 1110 0xxx
        assertEquals(16, tree.read(createBitInputStream(0xe8))); // 1110 1xxx
        assertEquals(1, tree.read(createBitInputStream(0xf0)));  // 1111 00xx
        assertEquals(2, tree.read(createBitInputStream(0xf4)));  // 1111 01xx
        assertEquals(6, tree.read(createBitInputStream(0xf8)));  // 1111 100x
        assertEquals(7, tree.read(createBitInputStream(0xfa)));  // 1111 101x
        assertEquals(8, tree.read(createBitInputStream(0xfc)));  // 1111 110x
        assertEquals(5, tree.read(createBitInputStream(0xfe)));  // 1111 1110
        assertEquals(18, tree.read(createBitInputStream(0xff))); // 1111 1111
    }

    @Test
    void testTree10() throws Exception {
        // Maximum length of 510 entries for command tree and maximum supported depth of 16
        final int[] length = new int[] { 4, 7, 7, 8, 7, 9, 8, 9, 7, 10, 8, 10, 7, 10, 8, 10, 7, 9, 8, 9, 8, 10, 8, 12, 8, 10, 9, 11, 9, 9, 8, 10, 6, 9,
                7, 9, 8, 10, 8, 11, 7, 9, 8, 9, 8, 9, 8, 9, 7, 9, 8, 8, 8, 10, 9, 11, 8, 9, 8, 10, 8, 9, 8, 9, 7, 7, 7, 8, 8, 8, 8, 9, 7, 8, 7, 9, 8, 9,
                8, 8, 8, 10, 7, 7, 8, 8, 8, 9, 8, 9, 8, 9, 9, 10, 9, 10, 7, 8, 9, 9, 8, 7, 7, 7, 8, 8, 9, 8, 8, 9, 8, 8, 8, 11, 8, 9, 8, 8, 9, 10, 9, 9,
                8, 10, 8, 10, 9, 9, 7, 9, 9, 10, 9, 10, 9, 9, 9, 10, 9, 11, 10, 11, 9, 10, 8, 10, 9, 11, 9, 10, 10, 12, 9, 11, 9, 12, 10, 14, 10, 14, 10,
                11, 10, 11, 9, 11, 10, 12, 9, 11, 10, 11, 9, 10, 10, 11, 9, 11, 10, 12, 10, 13, 11, 13, 10, 11, 10, 13, 10, 15, 10, 14, 8, 10, 9, 10, 9,
                10, 10, 11, 9, 11, 10, 12, 10, 13, 10, 13, 9, 11, 9, 11, 9, 12, 9, 11, 9, 10, 9, 12, 9, 11, 9, 9, 9, 10, 8, 10, 9, 11, 9, 10, 9, 10, 9,
                10, 9, 10, 9, 11, 8, 10, 9, 10, 9, 10, 9, 11, 9, 10, 8, 10, 8, 10, 9, 7, 3, 4, 5, 5, 6, 7, 7, 7, 8, 8, 9, 9, 9, 9, 10, 10, 11, 11, 11,
                10, 11, 12, 11, 12, 12, 12, 12, 13, 13, 13, 14, 12, 14, 13, 16, 14, 16, 13, 15, 14, 13, 15, 14, 15, 14, 15, 14, 14, 0, 14, 15, 14, 0,
                14, 0, 0, 0, 0, 0, 0, 0, 15, 0, 15, 0, 0, 15, 15, 0, 0, 0, 0, 0, 0, 0, 0, 0, 15, 0, 0, 0, 0, 15, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                13, 0, 0, 15, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 15, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 15, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 15, 0, 0, 0, 15, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 15, 0, 0, 0, 0, 0, 0, 0, 0, 15, 10 };

        final BinaryTree tree = new BinaryTree(length);

        assertEquals(256, tree.read(createBitInputStream(0x00, 0x00))); // 000x xxxx  xxxx xxxx
        assertEquals(0, tree.read(createBitInputStream(0x20, 0x00)));   // 0010 xxxx  xxxx xxxx
        assertEquals(257, tree.read(createBitInputStream(0x30, 0x00))); // 0011 xxxx  xxxx xxxx
        assertEquals(258, tree.read(createBitInputStream(0x40, 0x00))); // 0100 0xxx  xxxx xxxx
        assertEquals(259, tree.read(createBitInputStream(0x48, 0x00))); // 0100 1xxx  xxxx xxxx
        assertEquals(32, tree.read(createBitInputStream(0x50, 0x00)));  // 0101 00xx  xxxx xxxx
        assertEquals(260, tree.read(createBitInputStream(0x54, 0x00))); // 0101 01xx  xxxx xxxx

        assertEquals(226, tree.read(createBitInputStream(0xbd, 0x00))); // 1011 1101  xxxx xxxx
        assertEquals(240, tree.read(createBitInputStream(0xbe, 0x00))); // 1011 1110  xxxx xxxx

        assertEquals(163, tree.read(createBitInputStream(0xfb, 0xa0))); // 1111 1011  101x xxxx
        assertEquals(165, tree.read(createBitInputStream(0xfb, 0xc0))); // 1111 1011  110x xxxx

        assertEquals(499, tree.read(createBitInputStream(0xff, 0xfa))); // 1111 1111  1111 101x
        assertEquals(508, tree.read(createBitInputStream(0xff, 0xfc))); // 1111 1111  1111 110x
        assertEquals(290, tree.read(createBitInputStream(0xff, 0xfe))); // 1111 1111  1111 1110
        assertEquals(292, tree.read(createBitInputStream(0xff, 0xff))); // 1111 1111  1111 1111
    }

    @Test
    void testReadEof() throws Exception {
        final int[] length = new int[] { 4, 2, 3, 0, 5, 5, 1 };
        //                        Value: 0  1  2  3  4  5  6

        final BinaryTree tree = new BinaryTree(length);

        final BitInputStream in = createBitInputStream(0xfe); // 1111 1110

        assertEquals(5, tree.read(in));  // 1111 1xxx
        assertEquals(2, tree.read(in));  // 110x xxxx
        assertEquals(-1, tree.read(in)); // EOF
    }

    @Test
    void testInvalidBitstream() throws Exception {
        final int[] length = new int[] { 4, 2, 3, 0, 5, 0, 1 };
        //                        Value: 0  1  2  3  4  5  6

        final BinaryTree tree = new BinaryTree(length);

        assertEquals(6, tree.read(createBitInputStream(0x00))); // 0xxx xxxx
        assertEquals(1, tree.read(createBitInputStream(0x80))); // 10xx xxxx
        assertEquals(2, tree.read(createBitInputStream(0xc0))); // 110x xxxx
        assertEquals(0, tree.read(createBitInputStream(0xe0))); // 1110 xxxx
        assertEquals(4, tree.read(createBitInputStream(0xf0))); // 1111 0xxx

        try {
            assertEquals(5, tree.read(createBitInputStream(0xf8))); // 1111 1xxx
            fail("Expected CompressorException for invalid bitstream");
        } catch (CompressorException e) {
            assertEquals("Invalid bitstream. The node at index 62 is not defined.", e.getMessage());
        }
    }

    @Test
    void testCheckMaxDepth() throws Exception {
        try {
            new BinaryTree(1, 17);
            fail("Expected IllegalArgumentException for depth > 16");
        } catch (IllegalArgumentException e) {
            assertEquals("Depth must not be negative and not bigger than 16 but is 17", e.getMessage());
        }
    }

    private BitInputStream createBitInputStream(final int... data) throws IOException {
        final byte[] bytes = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            bytes[i] = (byte) data[i];
        }

        return new BitInputStream(new ByteArrayInputStream(bytes), ByteOrder.BIG_ENDIAN);
    }
}
