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

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.lang3.ArrayFill;

/**
 * Binary tree of positive values.
 *
 * @since 1.7
 */
final class BinaryTree {

    /** Value in the array indicating an undefined node */
    private static final int UNDEFINED = -1;

    /** Value in the array indicating a non leaf node */
    private static final int NODE = -2;

    /**
     * Creates a binary tree from the specified array of code lengths.
     * <p>
     * Each code length is the number of bits of the code for the symbol at that index, and must be between 1 and 16 (inclusive).
     * </p>
     *
     * @param codeLengths code length per symbol, each value must be between 1 and 16 (inclusive).
     * @return the binary tree.
     * @throws ArchiveException if the code lengths array is empty, or if a code length is less than 1 or greater than 16.
     */
    static BinaryTree fromCodeLengths(final int[] codeLengths) throws ArchiveException {
        if (codeLengths.length == 0) {
            throw new ArchiveException("Empty code length list");
        }

        // validate and find max length
        int maxCodeLength = 0;
        for (int i = 0; i < codeLengths.length; i++) {
            final int len = codeLengths[i];
            if (len < 1 || len > 16) {
                throw new ArchiveException(
                        String.format("Invalid code length at symbol %d: %d (expected in [1, 16])", i, len));
            }

            maxCodeLength = Math.max(maxCodeLength, len);
        }

        // sort the array of code lengths and memorize the permutation used to restore the order of the codes
        final int[] permutation = new int[codeLengths.length];
        for (int k = 0; k < permutation.length; k++) {
            permutation[k] = k;
        }

        int c = 0;
        final int[] sortedCodeLengths = new int[codeLengths.length];
        for (int k = 1; k <= maxCodeLength; k++) {
            // iterate over the values
            for (int l = 0; l < codeLengths.length; l++) {
                // look for the value in the original array
                if (codeLengths[l] == k) {
                    // put the value at the current position in the sorted array...
                    sortedCodeLengths[c] = k;

                    // ...and memorize the permutation
                    permutation[c] = l;

                    c++;
                }
            }
        }

        // decode the values of the tree
        int code = 0;
        int codeIncrement = 0;
        int lastCodeLength = 0;

        final int[] codes = new int[codeLengths.length];

        for (int i = codeLengths.length - 1; i >= 0; i--) {
            code += codeIncrement;
            if (sortedCodeLengths[i] != lastCodeLength) {
                lastCodeLength = sortedCodeLengths[i];
                codeIncrement = 1 << 16 - lastCodeLength;
            }
            codes[permutation[i]] = code;
        }

        // build the tree
        final BinaryTree tree = new BinaryTree(maxCodeLength);

        for (int k = 0; k < codes.length; k++) {
            final int codeLength = codeLengths[k];
            tree.addLeaf(0, Integer.reverse(codes[k] << 16), codeLength, k);
        }

        return tree;
    }

    /**
     * The array representing the binary tree. The root is at index 0, the left children are at 2*i+1 and the right children at 2*i+2.
     */
    private final int[] tree;

    BinaryTree(final int depth) {
        if (depth < 0 || depth > 30) {
            throw new IllegalArgumentException("depth must be bigger than 0 and not bigger than 30 but is " + depth);
        }
        tree = ArrayFill.fill(new int[(int) ((1L << depth + 1) - 1)], UNDEFINED);
    }

    /**
     * Adds a leaf to the tree.
     *
     * @param node  The index of the node where the path is appended.
     * @param path  The path to the leaf (bits are parsed from the right to the left).
     * @param depth The number of nodes in the path.
     * @param value The value of the leaf (must be positive).
     */
    public void addLeaf(final int node, final int path, final int depth, final int value) {
        if (depth == 0) {
            // end of the path reached, add the value to the current node
            if (tree[node] != UNDEFINED) {
                throw new IllegalArgumentException("Tree value at index " + node + " has already been assigned (" + tree[node] + ")");
            }
            tree[node] = value;
        } else {
            // mark the current node as a non leaf node
            tree[node] = NODE;

            // move down the path recursively
            final int nextChild = 2 * node + 1 + (path & 1);
            addLeaf(nextChild, path >>> 1, depth - 1, value);
        }
    }

    /**
     * Reads a value from the specified bit stream.
     *
     * @param stream The data source.
     * @return The value decoded, or -1 if the end of the stream is reached.
     * @throws IOException on error.
     */
    public int read(final BitStream stream) throws IOException {
        int currentIndex = 0;
        while (true) {
            final int bit = stream.readBit();
            if (bit == -1) {
                return -1;
            }
            final int childIndex = 2 * currentIndex + 1 + bit;
            final int value = tree[childIndex];
            if (value == NODE) {
                // consume the next bit
                currentIndex = childIndex;
            } else if (value != UNDEFINED) {
                return value;
            } else {
                throw new ArchiveException("The child %,d of node at index %,d is not defined", bit, currentIndex);
            }
        }
    }
}
