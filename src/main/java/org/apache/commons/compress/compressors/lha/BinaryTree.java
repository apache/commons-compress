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

import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.utils.BitInputStream;
import org.apache.commons.lang3.ArrayFill;

/**
 * Binary tree of positive values.
 *
 * Copied from org.apache.commons.compress.archivers.zip.BinaryTree and modified for LHA.
 */
class BinaryTree {

    /** Value in the array indicating an undefined node */
    private static final int UNDEFINED = -1;

    /** Value in the array indicating a non leaf node */
    private static final int NODE = -2;

    /**
     * The array representing the binary tree. The root is at index 0, the left children (0) are at 2*i+1 and the right children (1) at 2*i+2.
     */
    private final int[] tree;

    /**
     * Constructs a binary tree from the given array that contains the depth (code length) in the
     * binary tree as values in the array and the index into the array as the value of the leaf node.
     *
     * If the array contains a single value, this is a special case where there is only one node in the
     * tree (the root node) and it contains the value. For this case, the array contains the value of
     * the root node instead of the depth in the tree. This special case also means that no bits will
     * be read from the bit stream when the read method is called, as there are no children to traverse.
     *
     * @param array the array to build the binary tree from
     * @throws CompressorException if the tree is invalid
     */
    BinaryTree(final int... array) throws CompressorException {
        if (array.length == 1) {
            // Tree only contains a single value, which is the root node value
            this.tree = new int[] { array[0] };
            return;
        }

        // Determine the maximum depth of the tree from the input array
        final int maxDepth = Arrays.stream(array).max().getAsInt();
        if (maxDepth == 0) {
            throw new CompressorException("Tree contains no leaf nodes");
        }

        // Allocate binary tree with enough space for all nodes
        this.tree = initTree(maxDepth);

        int treePos = 0;

        // Add root node pointing to left (0) and right (1) children
        this.tree[treePos++] = NODE;

        // Iterate over each possible tree depth (starting from 1)
        for (int currentDepth = 1; currentDepth <= maxDepth; currentDepth++) {
            final int startPos = (1 << currentDepth) - 1; // Start position for the first node at this depth
            final int maxNodesAtCurrentDepth = 1 << currentDepth; // Max number of nodes at this depth
            int numNodesAtCurrentDepth = treePos - startPos; // Number of nodes added at this depth taking into account any already skipped nodes (UNDEFINED)

            // Add leaf nodes for values with the current depth
            for (int value = 0; value < array.length; value++) {
                if (array[value] == currentDepth) {
                    if (numNodesAtCurrentDepth == maxNodesAtCurrentDepth) {
                        throw new CompressorException("Tree contains too many leaf nodes for depth %d", currentDepth);
                    }

                    this.tree[treePos++] = value; // Add leaf (value) node
                    numNodesAtCurrentDepth++;
                }
            }

            // Add nodes pointing to child nodes until the maximum number of nodes at this depth is reached
            int skipToTreePos = -1;
            while (currentDepth != maxDepth && numNodesAtCurrentDepth < maxNodesAtCurrentDepth) {
                if (skipToTreePos == -1) {
                    skipToTreePos = 2 * treePos + 1; // Next depth's tree position that this node's left (0) child would occupy
                }

                this.tree[treePos++] = NODE; // Add node pointing to left (0) and right (1) children
                numNodesAtCurrentDepth++;
            }

            if (skipToTreePos != -1) {
                treePos = skipToTreePos; // Skip to the next depth's tree position based on the first node at this depth
            }
        }
    }

    /**
     * Initializes the binary tree with the specified depth but with all nodes as UNDEFINED.
     *
     * @param depth the depth of the tree, must be between 0 and 16 (inclusive)
     * @return an array representing the binary tree, initialized with UNDEFINED values
     * @throws CompressorException for invalid depth
     */
    private int[] initTree(final int depth) throws CompressorException {
        if (depth < 0 || depth > 16) {
            throw new CompressorException("Tree depth must not be negative and not bigger than 16 but is " + depth);
        }

        final int arraySize = depth == 0 ? 1 : (int) ((1L << depth + 1) - 1); // Depth 0 has only a single node (the root)

        return ArrayFill.fill(new int[arraySize], UNDEFINED);
    }

    /**
     * Reads a value from the specified bit stream.
     *
     * @param stream The data source.
     * @return the value decoded, or -1 if the end of the stream is reached
     * @throws IOException on error.
     */
    public int read(final BitInputStream stream) throws IOException {
        int currentIndex = 0;

        while (true) {
            final int value = tree[currentIndex];
            if (value == NODE) {
                // Consume the next bit
                final int bit = stream.readBit();
                if (bit == -1) {
                    return -1;
                }

                currentIndex = 2 * currentIndex + 1 + bit;
            } else if (value == UNDEFINED) {
                throw new CompressorException("Invalid bitstream. The node at index %d is not defined.", currentIndex);
            } else {
                return value;
            }
        }
    }
}
