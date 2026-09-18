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

package org.apache.commons.compress.archivers.arj;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.utils.BitInputStream;
import org.apache.commons.compress.utils.InputStreamStatistics;
import org.apache.commons.io.input.CloseShieldInputStream;

/**
 * Implements a static Huffman compressor input stream for ARJ files that supports the methods 1 (COMPRESSED_MOST), 2 (COMPRESSED), and 3 (COMPRESSED_FASTER).
 * <p>
 * ARJ uses the same LZH coding as LHA but with a fixed dictionary size of 26624 bytes and a distance tree that is encoded with 5 bits and contains at most 26
 * codes. Closely follows the reference implementation of the decoder in 7-Zip's LzhDecoder.cpp.
 * </p>
 */
class ArjLzhDecoderInputStream extends CompressorInputStream implements InputStreamStatistics {

    /**
     * Size of the sliding dictionary in bytes. Taken from the reference implementation which sets it to 26624 for ARJ.
     */
    private static final int DICTIONARY_SIZE = 26624;

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

    /**
     * Maximum code length.
     */
    private static final int MAX_CODE_LENGTH = 16;

    /**
     * Number of bits used to encode the distance tree length.
     */
    private static final int DISTANCE_BITS = 5;

    /**
     * Maximum number of codes in the distance tree.
     */
    private static final int MAX_NUMBER_OF_DISTANCE_CODES = 26;

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
     * Number of decompressed bytes written to the buffer so far, used to reject copy commands that reference data before the start of the output.
     */
    private long bytesWritten;

    /**
     * Constructs a new CompressorInputStream which decompresses bytes read from the specified stream.
     *
     * @param in The InputStream from which to read compressed data.
     */
    ArjLzhDecoderInputStream(final InputStream in) {
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
            if (this.blockSize == 0) {
                // Zero block size marks the end of the compressed data
                this.blockSize = -1;
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
        }
        if (command < NUMBER_OF_LITERAL_CODES) {
            // Literal command, just write the byte to the buffer
            buffer.put(command);
            bytesWritten++;
        } else {
            // Copy command, read the distance and calculate the length from the command
            final int distance = readDistance();
            final int length = command - NUMBER_OF_LITERAL_CODES + getCopyThreshold();
            // Copy the data from the sliding dictionary and add to the buffer
            buffer.copy(distance + 1, length);
            bytesWritten += length;
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
     * Gets the size of the dictionary.
     *
     * @return the size of the dictionary.
     */
    int getDictionarySize() {
        return DICTIONARY_SIZE;
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
        return MAX_NUMBER_OF_DISTANCE_CODES;
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
                throw new CompressorException("Bad ARJ stream", e);
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
     * Reads the command decoding tree. The command decoding tree is used when reading the command tree which is then actually used to decode the commands
     * (literals or copy commands).
     *
     * @return the command decoding tree.
     * @throws IOException if an I/O error occurs.
     */
    BinaryTree readCommandDecodingTree() throws IOException {
        // Number of code lengths to read
        final int numCodeLengths = readBits(COMMAND_DECODING_LENGTH_BITS);
        if (numCodeLengths > MAX_NUMBER_OF_COMMAND_DECODING_CODE_LENGTHS) {
            throw new CompressorException("Code length table has invalid size (%d > %d)", numCodeLengths, MAX_NUMBER_OF_COMMAND_DECODING_CODE_LENGTHS);
        }
        if (numCodeLengths == 0) {
            // If numCodeLengths is zero, we read a single code length of COMMAND_DECODING_LENGTH_BITS bits and use as root of the tree
            return new BinaryTree(readBits(COMMAND_DECODING_LENGTH_BITS));
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
        return new BinaryTree(codeLengths);
    }

    /**
     * Reads the command tree which is used to decode the commands (literals or copy commands).
     *
     * @param commandDecodingTree the Huffman tree used to decode the command lengths.
     * @return the command tree.
     * @throws IOException if an I/O error occurs.
     */
    BinaryTree readCommandTree(final BinaryTree commandDecodingTree) throws IOException {
        final int numCodeLengths = readBits(COMMAND_TREE_LENGTH_BITS);
        if (numCodeLengths > getMaxNumberOfCommands()) {
            throw new CompressorException("Code length table has invalid size (%d > %d)", numCodeLengths, getMaxNumberOfCommands());
        }
        if (numCodeLengths == 0) {
            // If numCodeLengths is zero, we read a single code length of COMMAND_TREE_LENGTH_BITS bits and use as root of the tree
            return new BinaryTree(readBits(COMMAND_TREE_LENGTH_BITS));
        }
        // Read all code lengths
        final int[] codeLengths = new int[numCodeLengths];
        for (int index = 0; index < numCodeLengths;) {
            final int codeOrSkipRange = commandDecodingTree.read(bin);
            switch (codeOrSkipRange) {
            case -1:
                throw new CompressorException("Unexpected end of stream");
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
        return new BinaryTree(codeLengths);
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
        final int bits = distanceTree.read(bin);
        if (bits == -1) {
            throw new CompressorException("Unexpected end of stream");
        }
        if (bits == 0 || bits == 1) {
            // This is effectively run length encoding
            return bits;
        }
        // Bits minus one is the number of bits to read for the distance
        final int value = readBits(bits - 1);
        // Add the implicit bit (1 << (bits - 1)) to the value read from the stream giving the distance.
        // E.g. if bits is 6, we read 5 bits giving value 8 and then we add 32 giving a distance of 40.
        final int distance = value | 1 << bits - 1;
        if (distance >= getDictionarySize()) {
            throw new CompressorException("Distance %d exceeds dictionary size %d", distance, getDictionarySize());
        }
        if (distance + 1 > bytesWritten) {
            throw new CompressorException("Distance %d exceeds number of bytes written %d", distance, bytesWritten);
        }
        return distance;
    }

    /**
     * Reads the distance tree which is used to decode the distance of the copy command.
     *
     * @return the distance tree.
     * @throws IOException if an I/O error occurs.
     */
    private BinaryTree readDistanceTree() throws IOException {
        // Number of code lengths to read
        final int numCodeLengths = readBits(DISTANCE_BITS);
        if (numCodeLengths > getMaxNumberOfDistanceCodes()) {
            throw new CompressorException("Code length table has invalid size (%d > %d)", numCodeLengths, getMaxNumberOfDistanceCodes());
        }
        if (numCodeLengths == 0) {
            // If numCodeLengths is zero, we read a single code length of DISTANCE_BITS bits and use as root of the tree
            return new BinaryTree(readBits(DISTANCE_BITS));
        }
        // Read all code lengths
        final int[] codeLengths = new int[numCodeLengths];
        for (int index = 0; index < numCodeLengths; index++) {
            codeLengths[index] = readCodeLength();
        }
        return new BinaryTree(codeLengths);
    }
}
