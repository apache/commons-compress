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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.utils.BitInputStream;
import org.apache.commons.compress.utils.InputStreamStatistics;
import org.apache.commons.io.input.CloseShieldInputStream;

/**
 * Implements a static Huffman compressor input stream for LHA files that supports lh4, lh5, lh6 and lh7 compression methods.
 */
class LhStaticHuffmanCompressorInputStream extends CompressorInputStream implements InputStreamStatistics {

    /**
     * Number of bits used to encode the command decoding tree length.
     */
    private static final int COMMAND_DECODING_LENGTH_BITS = 5;

    /**
     * Maximum number of codes in the command decoding tree.
     */
    private static final int MAX_NUMBER_OF_COMMAND_DECODING_CODE_LENGTHS = 19;

    /**
     * Number of bits used to encode the command tree length.
     */
    private static final int COMMAND_TREE_LENGTH_BITS = 9;

    /**
     * Number of literal codes (0-255).
     */
    private static final int NUMBER_OF_LITERAL_CODES = 0x100;

    /**
     * Number of bits used to encode the code length.
     */
    private static final int CODE_LENGTH_BITS = 3;

    private static final int MAX_CODE_LENGTH = 16;

    private static final int DICT_BITS_LH4 = 12;

    private static final int DICT_BITS_LH5 = 13;

    private static final int DICT_BITS_LH6 = 15;

    private static final int DICT_BITS_LH7 = 16;

    /**
     * Creates a new LhStaticHuffmanCompressorInputStream for the specified InputStream and LH4.
     *
     * @param in The InputStream to read compressed data from.
     * @return a new LhStaticHuffmanCompressorInputStream for LH4.
     * @throws IOException Thrown if an I/O error occurs.
     */
    public static LhStaticHuffmanCompressorInputStream lh4CompressorInputStream(final InputStream in) throws IOException {
        return new LhStaticHuffmanCompressorInputStream(in, DICT_BITS_LH4, 4, DICT_BITS_LH4 + 2);
    }

    /**
     * Creates a new LhStaticHuffmanCompressorInputStream for the specified InputStream and LH5.
     *
     * @param in The InputStream to read compressed data from.
     * @return a new LhStaticHuffmanCompressorInputStream for LH5.
     * @throws IOException Thrown if an I/O error occurs.
     */
    public static LhStaticHuffmanCompressorInputStream lh5CompressorInputStream(final InputStream in) throws IOException {
        return new LhStaticHuffmanCompressorInputStream(in, DICT_BITS_LH5, 4, DICT_BITS_LH5 + 1);
    }

    /**
     * Creates a new LhStaticHuffmanCompressorInputStream for the specified InputStream and LH6.
     *
     * @param in The InputStream to read compressed data from.
     * @return a new LhStaticHuffmanCompressorInputStream for LH6.
     * @throws IOException Thrown if an I/O error occurs.
     */
    public static LhStaticHuffmanCompressorInputStream lh6CompressorInputStream(final InputStream in) throws IOException {
        return new LhStaticHuffmanCompressorInputStream(in, DICT_BITS_LH6, 5, DICT_BITS_LH6 + 1);
    }

    /**
     * Creates a new LhStaticHuffmanCompressorInputStream for the specified InputStream and LH7.
     *
     * @param in The InputStream to read compressed data from.
     * @return a new LhStaticHuffmanCompressorInputStream for LH7.
     * @throws IOException Thrown if an I/O error occurs.
     */
    public static LhStaticHuffmanCompressorInputStream lh7CompressorInputStream(final InputStream in) throws IOException {
        return new LhStaticHuffmanCompressorInputStream(in, DICT_BITS_LH7, 5, DICT_BITS_LH7 + 1);
    }

    private BitInputStream bin;

    private CircularBuffer buffer;

    private int blockSize;

    /**
     * Command is either a literal or a copy command.
     */
    private LhaHuffmanDecoder commandDecoder;

    /**
     * Distance is the offset to copy from the sliding dictionary.
     */
    private LhaHuffmanDecoder distanceDecoder;

    private final int dictionaryBits;

    private final int distanceBits;

    private final int maxNumberOfDistanceCodes;

    /**
     * Constructs a new CompressorInputStream which decompresses bytes read from the specified stream.
     *
     * @param in                       The InputStream from which to read compressed data.
     * @param dictionaryBits           The number of bits used for the dictionary size.
     * @param distanceBits             The number of bits used for the distance.
     * @param maxNumberOfDistanceCodes The maximum number of distance codes.
     * @throws IOException if an I/O error occurs.
     */
    LhStaticHuffmanCompressorInputStream(final InputStream in, final int dictionaryBits, final int distanceBits, final int maxNumberOfDistanceCodes)
            throws IOException {
        this.dictionaryBits = dictionaryBits;
        this.distanceBits = distanceBits;
        this.maxNumberOfDistanceCodes = maxNumberOfDistanceCodes;
        this.bin = new BitInputStream(in == System.in ? CloseShieldInputStream.wrap(in) : in, ByteOrder.BIG_ENDIAN);
        // Create a sliding dictionary buffer that can hold the full dictionary size and the maximum match length
        this.buffer = new CircularBuffer(getDictionarySize() + getMaxMatchLength());
    }

    @Override
    public void close() throws IOException {
        if (this.bin != null) {
            try {
                this.bin.close();
            } finally {
                this.bin = null;
                this.buffer = null;
                this.blockSize = -1;
            }
        }
    }

    /**
     * Fill the sliding dictionary with more data.
     *
     * @throws IOException if an I/O error occurs.
     */
    private void fillBuffer() throws IOException {
        if (this.blockSize == -1) {
            // End of stream
            return;
        }
        if (this.blockSize == 0) {
            // Start to read the next block
            // Read the block size (number of commands to read)
            this.blockSize = (int) bin.readBits(16);
            if (this.blockSize == -1) {
                // End of stream
                return;
            }
            final LhaHuffmanDecoder commandCodeLengthDecoder = readCommandCodeLengthTree();
            this.commandDecoder = readCommandTree(commandCodeLengthDecoder);
            this.distanceDecoder = readDistanceTree();
        }
        this.blockSize--;
        final int command = commandDecoder.decodeSymbol(bin);
        if (command < NUMBER_OF_LITERAL_CODES) {
            // Literal command, just write the byte to the buffer
            buffer.put(command);
        } else {
            // Copy command, read the distance and calculate the length from the command
            final int distance = readDistance();
            final int length = command - NUMBER_OF_LITERAL_CODES + getCopyThreshold();
            // Copy the data from the sliding dictionary and add to the buffer
            buffer.copy(distance + 1, length);
        }
    }

    @Override
    public long getCompressedCount() {
        return bin.getBytesRead();
    }

    /**
     * Gets the threshold for copying data from the sliding dictionary. This is the minimum possible number of bytes that will be part of a copy command.
     *
     * @return the copy threshold.
     */
    int getCopyThreshold() {
        return 3;
    }

    /**
     * Gets the number of bits used for the dictionary size.
     *
     * @return the number of bits used for the dictionary size.
     */
    int getDictionaryBits() {
        return dictionaryBits;
    }

    /**
     * Gets the size of the dictionary.
     *
     * @return the size of the dictionary.
     */
    int getDictionarySize() {
        return 1 << getDictionaryBits();
    }

    /**
     * Gets the number of bits used for the distance.
     *
     * @return the number of bits used for the distance.
     */
    int getDistanceBits() {
        return distanceBits;
    }

    /**
     * Gets the maximum match length for the copy command.
     *
     * @return the maximum match length.
     */
    int getMaxMatchLength() {
        return 256;
    }

    /**
     * Gets the maximum number of commands in the command tree. This is 256 literals (0-255) and 254 copy lengths combinations (3-256).
     *
     * @return the maximum number of commands.
     */
    int getMaxNumberOfCommands() {
        return NUMBER_OF_LITERAL_CODES + getMaxMatchLength() - getCopyThreshold() + 1;
    }

    /**
     * Gets the maximum number of distance codes in the distance tree.
     *
     * @return the maximum number of distance codes.
     */
    int getMaxNumberOfDistanceCodes() {
        return maxNumberOfDistanceCodes;
    }

    @Override
    public int read() throws IOException {
        if (!buffer.available()) {
            // Nothing in the buffer, try to fill it
            try {
                fillBuffer();
            } catch (final IllegalArgumentException | IllegalStateException e) {
                // A corrupt stream can decode an out-of-range distance or overflow the sliding
                // dictionary, which the CircularBuffer signals with unchecked exceptions. Wrap
                // them so callers only need to handle IOException.
                throw new CompressorException("Bad LHA stream", e);
            }
        }
        final int ret = buffer.get();
        count(ret < 0 ? 0 : 1); // Increment input stream statistics
        return ret;
    }

    /**
     * Read the specified number of bits from the underlying stream throwing CompressorException if the end of the stream is reached before reading the
     * requested number of bits.
     *
     * @param count the number of bits to read.
     * @return the bits concatenated as an int using the stream's byte order.
     * @throws IOException if an I/O error occurs.
     */
    private int readBits(final int count) throws IOException {
        final long value = bin.readBits(count);
        if (value < 0) {
            throw new CompressorException("Unexpected end of stream");
        }
        return (int) value;
    }

    /**
     * Reads code length (depth in tree). Usually 0-7 but could be higher and if so, count the number of following consecutive one bits and add to the length.
     *
     * @return code length.
     * @throws IOException if an I/O error occurs.
     */
    int readCodeLength() throws IOException {
        int len = readBits(CODE_LENGTH_BITS);
        if (len == 0x07) {
            int bit = bin.readBit();
            while (bit == 1) {
                if (++len > MAX_CODE_LENGTH) {
                    throw new CompressorException("Code length overflow");
                }
                bit = bin.readBit();
            }
            if (bit == -1) {
                throw new CompressorException("Unexpected end of stream");
            }
        }
        return len;
    }

    /**
     * Reads the command code length tree. The command code length tree is used when reading the command tree which is then actually
     * used to decode the commands (literals or copy commands).
     *
     * @return the command code length decoder.
     * @throws IOException if an I/O error occurs.
     */
    LhaHuffmanDecoder readCommandCodeLengthTree() throws IOException {
        // Number of code lengths to read
        final int numCodeLengths = readBits(COMMAND_DECODING_LENGTH_BITS);
        if (numCodeLengths > MAX_NUMBER_OF_COMMAND_DECODING_CODE_LENGTHS) {
            throw new CompressorException("Code length table has invalid size (%d > %d)", numCodeLengths, MAX_NUMBER_OF_COMMAND_DECODING_CODE_LENGTHS);
        }
        if (numCodeLengths == 0) {
            // If numCodeLengths is zero, we read a single code length of COMMAND_DECODING_LENGTH_BITS bits and use as root of the tree
            return new LhaHuffmanDecoder(readBits(COMMAND_DECODING_LENGTH_BITS));
        }
        // Read all code lengths
        final int[] codeLengths = new int[numCodeLengths];
        for (int index = 0; index < numCodeLengths; index++) {
            codeLengths[index] = readCodeLength();
            if (index == 2) {
                // After reading the first three code lengths, we read a 2-bit skip range
                index += readBits(2);
            }
        }
        return new LhaHuffmanDecoder(codeLengths);
    }

    /**
     * Reads the command tree which is used to decode the commands (literals or copy commands).
     *
     * @param commandCodeLengthDecoder the Huffman decoder used to decode the command code lengths.
     * @return the command decoder.
     * @throws IOException if an I/O error occurs.
     */
    LhaHuffmanDecoder readCommandTree(final LhaHuffmanDecoder commandCodeLengthDecoder) throws IOException {
        final int numCodeLengths = readBits(COMMAND_TREE_LENGTH_BITS);
        if (numCodeLengths > getMaxNumberOfCommands()) {
            throw new CompressorException("Code length table has invalid size (%d > %d)", numCodeLengths, getMaxNumberOfCommands());
        }
        if (numCodeLengths == 0) {
            // If numCodeLengths is zero, we read a single code length of COMMAND_TREE_LENGTH_BITS bits and use as root of the tree
            return new LhaHuffmanDecoder(readBits(COMMAND_TREE_LENGTH_BITS));
        }
        // Read all code lengths
        final int[] codeLengths = new int[numCodeLengths];
        for (int index = 0; index < numCodeLengths;) {
            final int codeOrSkipRange = commandCodeLengthDecoder.decodeSymbol(bin);
            switch (codeOrSkipRange) {
            case 0:
                // Skip one code length
                index++;
                break;
            case 1:
                // Skip a range of code lengths, read 4 bits to determine how many to skip
                index += readBits(4) + 3;
                break;
            case 2:
                // Skip a range of code lengths, read 9 bits to determine how many to skip
                index += readBits(9) + 20;
                break;
            default:
                // Subtract 2 from the codeOrSkipRange to get the code length
                codeLengths[index++] = codeOrSkipRange - 2;
                break;
            }
        }
        return new LhaHuffmanDecoder(codeLengths);
    }

    /**
     * Reads the distance by first decoding the number of bits to read from the distance tree and then reading the actual distance value from the bit input
     * stream.
     *
     * @return the distance.
     * @throws IOException if an I/O error occurs.
     */
    private int readDistance() throws IOException {
        // Determine the number of bits to read for the distance by reading an entry from the distance tree
        final int bits = distanceDecoder.decodeSymbol(bin);
        if (bits == 0 || bits == 1) {
            // This is effectively run length encoding
            return bits;
        }
        // Bits minus one is the number of bits to read for the distance
        final int value = readBits(bits - 1);
        // Add the implicit bit (1 << (bits - 1)) to the value read from the stream giving the distance.
        // E.g. if bits is 6, we read 5 bits giving value 8 and then we add 32 giving a distance of 40.
        return value | 1 << bits - 1;
    }

    /**
     * Reads the distance tree which is used to decode the distance of the copy command.
     *
     * @return the distance decoder.
     * @throws IOException if an I/O error occurs.
     */
    private LhaHuffmanDecoder readDistanceTree() throws IOException {
        // Number of code lengths to read
        final int numCodeLengths = readBits(getDistanceBits());
        if (numCodeLengths > getMaxNumberOfDistanceCodes()) {
            throw new CompressorException("Code length table has invalid size (%d > %d)", numCodeLengths, getMaxNumberOfDistanceCodes());
        }
        if (numCodeLengths == 0) {
            // If numCodeLengths is zero, we read a single code length of getDistanceBits() bits and use as root of the tree
            return new LhaHuffmanDecoder(readBits(getDistanceBits()));
        }
        // Read all code lengths
        final int[] codeLengths = new int[numCodeLengths];
        for (int index = 0; index < numCodeLengths; index++) {
            codeLengths[index] = readCodeLength();
        }
        return new LhaHuffmanDecoder(codeLengths);
    }
}
