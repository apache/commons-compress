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

import static org.apache.commons.compress.AbstractTest.toByteArray;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.commons.compress.archivers.ArchiveException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BinaryTreeTest {

    @Test
    void testDecode1() throws IOException {
        final int[] codeLengths = new int[] { 1, 1 };
        // Symbol...........................: 0  1

        final BinaryTree tree = BinaryTree.fromCodeLengths(codeLengths);

        assertEquals(0, tree.read(createBitStream(0b0000_0001))); // xxxx xxx1
        assertEquals(1, tree.read(createBitStream(0b0000_0000))); // xxxx xxx0
    }

    @Test
    void testDecode2() throws IOException {
        final int[] codeLengths = new int[] { 2, 2, 1 };
        // Symbol...........................: 0  1  2

        final BinaryTree tree = BinaryTree.fromCodeLengths(codeLengths);

        assertEquals(2, tree.read(createBitStream(0b0000_0001))); // xxxx xxx1
        assertEquals(0, tree.read(createBitStream(0b0000_0010))); // xxxx xx10
        assertEquals(1, tree.read(createBitStream(0b0000_0000))); // xxxx xx00
    }

    @Test
    void testDecode3() throws IOException {
        final int[] codeLengths = new int[] { 3, 3, 3, 3, 3, 2, 4, 4 };
        // Symbol...........................: 0  1  2  3  4  5  6  7

        final BinaryTree tree = BinaryTree.fromCodeLengths(codeLengths);

        assertEquals(0, tree.read(createBitStream(0b0000_0101))); // xxxx x101
        assertEquals(1, tree.read(createBitStream(0b0000_0001))); // xxxx x001
        assertEquals(2, tree.read(createBitStream(0b0000_0110))); // xxxx x110
        assertEquals(3, tree.read(createBitStream(0b0000_0010))); // xxxx x010
        assertEquals(4, tree.read(createBitStream(0b0000_0100))); // xxxx x100
        assertEquals(5, tree.read(createBitStream(0b0000_0011))); // xxxx xx11
        assertEquals(6, tree.read(createBitStream(0b0000_1000))); // xxxx 1000
        assertEquals(7, tree.read(createBitStream(0b0000_0000))); // xxxx 0000
    }

    @Test
    void testDecodeCodeLengthGreaterThanSymbolCount() throws IOException {
        final int[] codeLengths = new int[] { 2, 2 };
        // Symbol...........................: 0  1

        final BinaryTree tree = BinaryTree.fromCodeLengths(codeLengths);

        assertEquals(0, tree.read(createBitStream(0b0000_0010))); // xxxx xx10
        assertEquals(1, tree.read(createBitStream(0b0000_0000))); // xxxx xx00
    }

    @Test
    void testExceptions() {
        final BinaryTree binaryFinary = new BinaryTree(4);
        binaryFinary.addLeaf(0, 0, 0, 1);
        assertThrows(IllegalArgumentException.class, () -> binaryFinary.addLeaf(0, 0, 0, 1));

        assertThrows(ArchiveException.class, () -> new BinaryTree(4).read(createBitStream(0)));
    }

    @Test
    void testNoCodeLengths() {
        final ArchiveException e = assertThrows(ArchiveException.class, () -> BinaryTree.fromCodeLengths(new int[0]),
                "Expected ArchiveException for empty code length list");
        assertEquals("Empty code length list", e.getMessage());
    }

    @ParameterizedTest
    @ValueSource(ints = { Integer.MIN_VALUE, -1, 0, 17, Integer.MAX_VALUE })
    void testCodeLengthOutOfRange(final int invalidLength) {
        final ArchiveException e = assertThrows(ArchiveException.class,
                () -> BinaryTree.fromCodeLengths(new int[] { 1, 2, invalidLength, 2 }));
        assertEquals("Invalid code length at symbol 2: " + invalidLength + " (expected in [1, 16])", e.getMessage());
    }

    private BitStream createBitStream(final int... data) throws IOException {
        return new BitStream(new ByteArrayInputStream(toByteArray(data)));
    }
}
