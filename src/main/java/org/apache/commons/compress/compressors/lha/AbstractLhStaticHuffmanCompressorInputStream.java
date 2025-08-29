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
import java.io.InputStream;
import java.nio.ByteOrder;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.utils.BitInputStream;
import org.apache.commons.compress.utils.CircularBuffer;
import org.apache.commons.compress.utils.InputStreamStatistics;
import org.apache.commons.io.input.CloseShieldInputStream;

/**
 * This is an implementation of a static Huffman compressor input stream for LHA files that
 * supports lh4, lh5, lh6 and lh7 compression methods.
 */
abstract class AbstractLhStaticHuffmanCompressorInputStream extends CompressorInputStream implements InputStreamStatistics {
    /**
     *  Number of bits used to encode the command decoding tree length.
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

    private BitInputStream bin;
    private CircularBuffer buffer;
    private int blockSize;
    /**
     * Command is either a literal or a copy command.
     */
    private BinaryTree commandTree;
    /**
     * Distance is the offset to copy from the sliding dictionary.
     */
    private BinaryTree distanceTree;

    /**
     * Constructs a new CompressorInputStream which decompresses bytes read from the specified stream.
     *
     * @param in the InputStream from which to read compressed data
     * @throws IOException if an I/O error occurs
     */
    AbstractLhStaticHuffmanCompressorInputStream(final InputStream in) throws IOException {
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
     * Gets the threshold for copying data from the sliding dictionary. This is the minimum
     * possible number of bytes that will be part of a copy command.
     *
     * @return the copy threshold
     */
    int getCopyThreshold() {
        return 3;
    }

    /**
     * Gets the number of bits used for the dictionary size.
     *
     * @return the number of bits used for the dictionary size
     */
    abstract int getDictionaryBits();

    /**
     * Gets the size of the dictionary.
     *
     * @return the size of the dictionary
     */
    int getDictionarySize() {
        return 1 << getDictionaryBits();
    }

    /**
     * Gets the number of bits used for the distance.
     *
     * @return the number of bits used for the distance
     */
    abstract int getDistanceBits();

    /**
     * Gets the maximum number of distance codes in the distance tree.
     *
     * @return the maximum number of distance codes
     */
    int getMaxNumberOfDistanceCodes() {
        return getDictionaryBits() + 1;
    }

    /**
     * Gets the maximum match length for the copy command.
     *
     * @return the maximum match length
     */
    int getMaxMatchLength() {
        return 256;
    }

    /**
     * Gets the maximum number of commands in the command tree.
     * This is 256 literals (0-255) and 254 copy lengths combinations (3-256).
     *
     * @return the maximum number of commands
     */
    int getMaxNumberOfCommands() {
        return NUMBER_OF_LITERAL_CODES + getMaxMatchLength() - getCopyThreshold() + 1;
    }

    @Override
    public long getCompressedCount() {
        return bin.getBytesRead();
    }

    @Override
    public int read() throws IOException {
        if (!buffer.available()) {
            // Nothing in the buffer, try to fill it
            fillBuffer();
        }

        final int ret = buffer.get();
        count(ret < 0 ? 0 : 1); // Increment input stream statistics
        return ret;
    }

    /**
     * Fill the sliding dictionary with more data.
     *
     * @throws IOException if an I/O error occurs
     */
    private void fillBuffer() throws IOException {
        if (this.blockSize == -1) {
            // End of stream
            return;
        } else if (this.blockSize == 0) {
            // Start to read the next block

            // Read the block size (number of commands to read)
            this.blockSize = (int) bin.readBits(16);
            if (this.blockSize == -1) {
                // End of stream
                return;
            }

            final BinaryTree commandDecodingTree = readCommandDecodingTree();

            this.commandTree = readCommandTree(commandDecodingTree);

            this.distanceTree = readDistanceTree();
        }

        this.blockSize--;

        final int command = commandTree.read(bin);
        if (command == -1) {
            throw new CompressorException("Unexpected end of stream");
        } else if (command < NUMBER_OF_LITERAL_CODES) {
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

    /**
     * Read the command decoding tree. The command decoding tree is used when reading the command tree
     * which is then actually used to decode the commands (literals or copy commands).
     *
     * @return the command decoding tree
     * @throws IOException if an I/O error occurs
     */
    BinaryTree readCommandDecodingTree() throws IOException {
        // Number of code lengths to read
        final int numCodeLengths = readBits(COMMAND_DECODING_LENGTH_BITS);

        if (numCodeLengths > MAX_NUMBER_OF_COMMAND_DECODING_CODE_LENGTHS) {
            throw new CompressorException("Code length table has invalid size (%d > %d)", numCodeLengths, MAX_NUMBER_OF_COMMAND_DECODING_CODE_LENGTHS);
        } else if (numCodeLengths == 0) {
            // If numCodeLengths is zero, we read a single code length of COMMAND_DECODING_LENGTH_BITS bits and use as root of the tree
            return new BinaryTree(readBits(COMMAND_DECODING_LENGTH_BITS));
        } else {
            // Read all code lengths
            final int[] codeLengths = new int[numCodeLengths];
            for (int index = 0; index < numCodeLengths; index++) {
                codeLengths[index] = readCodeLength();

                if (index == 2) {
                    // After reading the first three code lengths, we read a 2-bit skip range
                    index += readBits(2);
                }
            }

            return new BinaryTree(codeLengths);
        }
    }

    /**
     * Read code length (depth in tree). Usually 0-7 but could be higher and if so,
     * count the number of following consecutive one bits and add to the length.
     *
     * @return code length
     * @throws IOException if an I/O error occurs
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
     * Read the command tree which is used to decode the commands (literals or copy commands).
     *
     * @param commandDecodingTree the Huffman tree used to decode the command lengths
     * @return the command tree
     * @throws IOException if an I/O error occurs
     */
    BinaryTree readCommandTree(final BinaryTree commandDecodingTree) throws IOException {
        final int numCodeLengths = readBits(COMMAND_TREE_LENGTH_BITS);

        if (numCodeLengths > getMaxNumberOfCommands()) {
            throw new CompressorException("Code length table has invalid size (%d > %d)", numCodeLengths, getMaxNumberOfCommands());
        } else if (numCodeLengths == 0) {
            // If numCodeLengths is zero, we read a single code length of COMMAND_TREE_LENGTH_BITS bits and use as root of the tree
            return new BinaryTree(readBits(COMMAND_TREE_LENGTH_BITS));
        } else {
            // Read all code lengths
            final int[] codeLengths = new int[numCodeLengths];

            for (int index = 0; index < numCodeLengths;) {
                final int codeOrSkipRange = commandDecodingTree.read(bin);

                if (codeOrSkipRange == -1) {
                    throw new CompressorException("Unexpected end of stream");
                } else if (codeOrSkipRange == 0) {
                    // Skip one code length
                    index++;
                } else if (codeOrSkipRange == 1) {
                    // Skip a range of code lengths, read 4 bits to determine how many to skip
                    index += readBits(4) + 3;
                } else if (codeOrSkipRange == 2) {
                    // Skip a range of code lengths, read 9 bits to determine how many to skip
                    index += readBits(9) + 20;
                } else {
                    // Subtract 2 from the codeOrSkipRange to get the code length
                    codeLengths[index++] = codeOrSkipRange - 2;
                }
            }

            return new BinaryTree(codeLengths);
        }
    }

    /**
     * Read the distance tree which is used to decode the distance of the copy command.
     *
     * @return the distance tree
     * @throws IOException if an I/O error occurs
     */
    private BinaryTree readDistanceTree() throws IOException {
        // Number of code lengths to read
        final int numCodeLengths = readBits(getDistanceBits());

        if (numCodeLengths > getMaxNumberOfDistanceCodes()) {
            throw new CompressorException("Code length table has invalid size (%d > %d)", numCodeLengths, getMaxNumberOfDistanceCodes());
        } else if (numCodeLengths == 0) {
            // If numCodeLengths is zero, we read a single code length of getDistanceBits() bits and use as root of the tree
            return new BinaryTree(readBits(getDistanceBits()));
        } else {
            // Read all code lengths
            final int[] codeLengths = new int[numCodeLengths];
            for (int index = 0; index < numCodeLengths; index++) {
                codeLengths[index] = readCodeLength();
            }

            return new BinaryTree(codeLengths);
        }
    }

    /**
     * Read the distance by first decoding the number of bits to read from the distance tree
     * and then reading the actual distance value from the bit input stream.
     *
     * @return the distance
     * @throws IOException if an I/O error occurs
     */
    private int readDistance() throws IOException {
        // Determine the number of bits to read for the distance by reading an entry from the distance tree
        final int bits = distanceTree.read(bin);
        if (bits == -1) {
            throw new CompressorException("Unexpected end of stream");
        } else if (bits == 0 || bits == 1) {
            // This is effectively run length encoding
            return bits;
        } else {
            // Bits minus one is the number of bits to read for the distance
            final int value = readBits(bits - 1);

            // Add the implicit bit (1 << (bits - 1)) to the value read from the stream giving the distance.
            // E.g. if bits is 6, we read 5 bits giving value 8 and then we add 32 giving a distance of 40.
            return value | (1 << (bits - 1));
        }
    }

    /**
     * Read the specified number of bits from the underlying stream throwing CompressorException
     * if the end of the stream is reached before reading the requested number of bits.
     *
     * @param count the number of bits to read
     * @return the bits concatenated as an int using the stream's byte order
     * @throws IOException if an I/O error occurs.
     */
    private int readBits(final int count) throws IOException {
        final long value = bin.readBits(count);
        if (value < 0) {
            throw new CompressorException("Unexpected end of stream");
        }

        return (int) value;
    }
}
