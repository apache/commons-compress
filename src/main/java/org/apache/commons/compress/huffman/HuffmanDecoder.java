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

package org.apache.commons.compress.huffman;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Objects;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.utils.BitInputStream;

/**
 * Canonical Huffman decoder.
 * <p>
 * This class builds decoding tables from an array of code lengths (one entry per symbol) and then decodes symbols from a {@link BitInputStream}.
 * </p>
 * <p>
 * The constructors verify that the code lengths satisfy <em>Kraft's inequality</em>, i.e. that no code length has more leaf nodes assigned to it than a binary
 * tree of that depth can hold, and reject over-subscribed code sets with a {@link CompressorException}. Code sets that leave leaf nodes unused, i.e. incomplete
 * codes, are accepted, as several formats produce them. Decoding a code word that no symbol claims fails at {@link #decodeSymbol(BitInputStream)} time.
 * </p>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * int[] codeLengths = ...; // length per symbol (0 => unused)
 * int maxLen = 15; // maximum non-zero code length in codeLengths
 * HuffmanDecoder dec = new HuffmanDecoder(codeLengths, 0, maxLen);
 * int sym = dec.decodeSymbol(bitIn);
 * }</pre>
 *
 * <h2>Thread-safety</h2> Instances are immutable after construction and may be safely shared between threads.
 *
 * @since 1.29.0
 */
public final class HuffmanDecoder {

    /**
     * Maximum code length supported by this implementation.
     */
    private static final int MAX_SUPPORTED_CODE_LENGTH = 30;

    /**
     * Builds canonical decode tables.
     *
     * @throws CompressorException if the code lengths violate Kraft's inequality.
     */
    private static void fillCodeTable(final int[] codeLengths, final int minLen, final int maxLen, final int[] bias,
            final int[] limit, final int[] sorted) throws CompressorException {
        // 1) Histogram of code lengths
        final int[] count = new int[maxLen + 1];
        for (int symbol = 0; symbol < codeLengths.length; symbol++) {
            final int len = codeLengths[symbol];
            if (len == 0) {
                continue;
            }
            count[codeLengths[symbol]]++;
        }
        // 2) Verify Kraft's inequality is not violated, i.e. the tree has no more leaf nodes at any depth than fit in a binary tree of that depth.
        int availableNodes = 1;
        for (int len = 1; len <= maxLen; len++) {
            availableNodes <<= 1;
            if (count[len] > availableNodes) {
                throw new CompressorException("Tree contains too many leaf nodes for code length %d: %d leaf nodes, but only %d nodes available", len,
                        count[len], availableNodes);
            }
            availableNodes -= count[len];
        }
        // 3) Generate starting offsets into sorted symbol table
        // The offsets are biased by -1 to simplify code in the next step
        final int[] offset = new int[maxLen + 1];
        offset[0] = -1;
        for (int len = 1; len <= maxLen; len++) {
            offset[len] = offset[len - 1] + count[len - 1];
        }
        // 4) Build table of symbols sorted by length, then by symbol
        // Adjust offsets to point to the last element of each length
        for (int symbol = 0; symbol < codeLengths.length; symbol++) {
            final int len = codeLengths[symbol];
            if (len == 0) {
                continue;
            }
            sorted[++offset[len]] = symbol;
        }
        // 5) Compute the largest left-justified code for each length
        int firstCode = 0;
        for (int len = minLen; len <= maxLen; len++) {
            firstCode += count[len];
            limit[len] = firstCode - 1;
            firstCode <<= 1; // prepare for next length
        }
        // 6) Compute the bias for each length
        for (int len = minLen; len <= maxLen; len++) {
            bias[len] = limit[len] - offset[len];
        }
    }

    private static int readBit(final BitInputStream in) throws IOException {
        final int bit = in.readBit();
        if (bit < 0) {
            throw new EOFException("Truncated Huffman bit stream");
        }
        return bit;
    }

    private static int readBitsFully(final BitInputStream in, final int numBits) throws IOException {
        final int code = (int) in.readBits(numBits);
        if (code < 0) {
            throw new EOFException("Truncated Huffman bit stream");
        }
        // Adjust for bit order
        return in.getByteOrder() == ByteOrder.BIG_ENDIAN ? code : Integer.reverse(code) >>> 32 - numBits;
    }

    /** Minimum non-zero code length */
    private final int minLength;

    /** Maximum non-zero code length */
    private final int maxLength;

    /**
     * Symbols in canonical order (by length, then by symbol).
     */
    private final int[] sorted;

    /**
     * For each code length, the bias between code values and indices into the sorted symbol table.
     */
    private final int[] bias;

    /**
     * For each code length, the largest left-justified code of that length.
     */
    private final int[] limit;

    /**
     * Constructs a decoder from canonical code lengths.
     * <p>
     * The {@code codeLengths} array provides, for each symbol index {@code i} in {@code [0, codeLengthSize)}, the length (in bits) of that symbol's code. A
     * value of {@code 0} marks an unused symbol. All non-zero lengths must be {@code <= maxCodeLength}.
     * </p>
     *
     * @param codeLengths code length per symbol; {@code 0} means the symbol is not used; not {@code null}.
     * @throws NullPointerException     if {@code codeLengths} is {@code null}.
     * @throws CompressorException      if {@code codeLengths} size is out of range or if any code length is out of range or if the code lengths violate
     *                                  Kraft's inequality.
     */
    public HuffmanDecoder(final int[] codeLengths) throws CompressorException {
        this(codeLengths, 0, MAX_SUPPORTED_CODE_LENGTH);
    }

    /**
     * Constructs a decoder from canonical code lengths.
     * <p>
     * The {@code codeLengths} array provides, for each symbol index {@code i} in {@code [0, codeLengthSize)}, the length (in bits) of that symbol's code. A
     * value of {@code 0} marks an unused symbol. All non-zero lengths must be {@code <= maxCodeLength}.
     * </p>
     *
     * @param codeLengths    code length per symbol; {@code 0} means the symbol is not used; not {@code null}.
     * @param minCodeLength  minimum allowed code length present in {@code codeLengths}.
     * @param maxCodeLength  maximum allowed code length present in {@code codeLengths}.
     * @throws NullPointerException     if {@code codeLengths} is {@code null}.
     * @throws IllegalArgumentException if {@code maxCodeLength} exceeds the implementation limit (30) or if {@code minCodeLength}
     *                                  is not in the range 0-{@code maxCodeLength}].
     * @throws CompressorException      if {@code codeLengths} size is out of range or if any code length is out of range or if the code lengths violate
     *                                  Kraft's inequality.
     */
    public HuffmanDecoder(final int[] codeLengths, final int minCodeLength, final int maxCodeLength) throws CompressorException {
        Objects.requireNonNull(codeLengths, "codeLengths");
        if (maxCodeLength > MAX_SUPPORTED_CODE_LENGTH) {
            throw new IllegalArgumentException(String.format("maxCodeLength (%d) exceeds supported limit (%d)", maxCodeLength, MAX_SUPPORTED_CODE_LENGTH));
        }
        if (minCodeLength < 0 || minCodeLength > maxCodeLength) {
            throw new IllegalArgumentException(String.format("minCodeLength (%d) not within range [0, %d)", minCodeLength, maxCodeLength));
        }
        if (codeLengths.length == 0) {
            throw new CompressorException(String.format("Empty code length list"));
        }
        // Validate and find min/max lengths
        int min = maxCodeLength;
        int max = minCodeLength;
        for (int i = 0; i < codeLengths.length; i++) {
            final int len = codeLengths[i];
            if (len < minCodeLength || len > maxCodeLength) {
                throw new CompressorException(
                        String.format("Invalid code length at symbol %d: %d (expected in [%d, %d])", i, len, minCodeLength, maxCodeLength));
            }
            if (len == 0) {
                continue; // unused symbol
            }
            if (len < min) {
                min = len;
            }
            if (len > max) {
                max = len;
            }
        }
        this.minLength = min;
        this.maxLength = max;
        // Allocate outputs; we reuse them as scratch inside fillCodeTable
        this.bias = new int[max + 1];
        this.limit = new int[max + 1];
        this.sorted = new int[codeLengths.length];
        // Arrays are zero-initialized; no additional temps needed.
        fillCodeTable(codeLengths, minLength, max, bias, limit, sorted);
    }

    /**
     * Decodes one symbol from the input bitstream.
     *
     * @param in the source of bits (MSB-first) to read from.
     * @return the decoded symbol index.
     * @throws EOFException if the input ends in the middle of a Huffman code word.
     * @throws IOException  if an I/O error occurs while reading from {@code in}.
     */
    public int decodeSymbol(final BitInputStream in) throws IOException {
        int len = minLength;
        int code = readBitsFully(in, len);
        while (len <= maxLength && code > limit[len]) {
            final int b = readBit(in);
            code = code << 1 | b;
            len++;
        }
        if (len > maxLength) {
            throw new CompressorException("Invalid Huffman code: " + code);
        }
        return sorted[code - bias[len]];
    }

    /**
     * Gets the maximum code length (in bits) for this code set.
     *
     * @return maximum code length (in bits).
     */
    public int getMaxLength() {
        return maxLength;
    }

    /**
     * Gets the minimum code length (in bits) for this code set.
     *
     * @return minimum code length (in bits).
     */
    public int getMinLength() {
        return minLength;
    }
}
