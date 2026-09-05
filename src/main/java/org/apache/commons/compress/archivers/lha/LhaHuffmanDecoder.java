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

package org.apache.commons.compress.archivers.lha;

import java.io.EOFException;
import java.io.IOException;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.huffman.HuffmanDecoder;
import org.apache.commons.compress.utils.BitInputStream;

/**
 * Huffman decoder for the static Huffman compression methods used by LHA (lh4, lh5, lh6 and lh7).
 */
final class LhaHuffmanDecoder {
    /**
     * Maximum code length (in bits) supported by LHA.
     */
    private static final int MAX_CODE_LENGTH = 16;

    /**
     * Decodes symbols when the tree has more than one leaf node; {@code null} if the input code length array includes just a single value.
     */
    private final HuffmanDecoder decoder;

    /**
     * The value of the root node. Only used when {@link #decoder} is {@code null}, i.e. when the tree has a single leaf node and no bits need to be
     * read from the stream to decode it.
     */
    private final int singleValue;

    /**
     * Constructs a Huffman decoder from the given codeLengths array that contains the depth (code length) in the tree as values in the array and the index
     * into the array as the value of the leaf node.
     *
     * If the array contains a single value, this is a special case where there is only one node in the tree (the root node) and it contains the value.
     * For this case, the array contains the value of the root node instead of the depth in the tree. This special case also means that no bits will be
     * read from the bit stream when the read method is called, as there are no children to traverse.
     *
     * @param codeLengths code length per symbol; {@code 0} means the symbol is not used; not {@code null}.
     * @throws NullPointerException if {@code codeLengths} is {@code null}.
     * @throws CompressorException  if {@code codeLengths} size is out of range, if any code length is out of range, if all code lengths are zero,
     *                              or if the code lengths violate Kraft's inequality.
     */
    LhaHuffmanDecoder(final int... codeLengths) throws CompressorException {
        if (codeLengths.length == 1) {
            singleValue = codeLengths[0];
            decoder = null;
        } else {
            singleValue = 0;
            decoder = new HuffmanDecoder(codeLengths, 0, MAX_CODE_LENGTH);
        }
    }

    /**
     * Decodes one symbol from the input bitstream.
     *
     * @param in the source of bits (MSB-first) to read from.
     * @return the decoded symbol index.
     * @throws EOFException if the input ends in the middle of a Huffman code word.
     * @throws IOException  if an I/O error occurs while reading from {@code in}.
     */
    int decodeSymbol(BitInputStream in) throws IOException {
        return decoder != null ? decoder.decodeSymbol(in) : singleValue;
    }
}
