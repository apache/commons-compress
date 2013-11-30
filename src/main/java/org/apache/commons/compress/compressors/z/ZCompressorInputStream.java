/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.compress.compressors.z;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.compressors.CompressorInputStream;

/**
 * Input stream that decompresses .Z files.
 * @NotThreadSafe
 * @since 1.7
 */
public class ZCompressorInputStream extends CompressorInputStream {
    private static final int MAGIC_1 = 0x1f;
    private static final int MAGIC_2 = 0x9d;
    private static final int BLOCK_MODE_MASK = 0x80;
    private static final int MAX_CODE_SIZE_MASK = 0x1f;
    private final InputStream in;
    private final boolean blockMode;
    private final int clearCode;
    private final int maxCodeSize;
    private int codeSize = 9;
    private int bitsCached = 0;
    private int bitsCachedSize = 0;
    private long totalCodesRead = 0;
    private int previousEntry = -1;
    private int tableSize = 0;
    private final int[] prefixes;
    private final byte[] characters;
    private final byte[] outputStack;
    private int outputStackLocation;
    
    public ZCompressorInputStream(InputStream inputStream) throws IOException {
        this.in = inputStream;
        int firstByte = in.read();
        int secondByte = in.read();
        int thirdByte = in.read();
        if (firstByte != MAGIC_1 || secondByte != MAGIC_2 || thirdByte < 0) {
            throw new IOException("Input is not in .Z format");
        }
        blockMode = ((thirdByte & BLOCK_MODE_MASK) != 0);
        maxCodeSize = thirdByte & MAX_CODE_SIZE_MASK;
        if (blockMode) {
            clearCode = (1 << (codeSize - 1));
        } else {
            clearCode = -1; // unused
        }
        final int maxTableSize = 1 << maxCodeSize;
        prefixes = new int[maxTableSize];
        characters = new byte[maxTableSize];
        outputStack = new byte[maxTableSize];
        outputStackLocation = maxTableSize;
        for (int i = 0; i < (1 << 8); i++) {
            prefixes[i] = -1;
            characters[i] = (byte)i;
        }
        clearEntries();
    }
    
    private void clearEntries() {
        tableSize = (1 << 8);
        if (blockMode) {
            tableSize++;
        }
    }

    private int readNextCode() throws IOException {
        while (bitsCachedSize < codeSize) {
            final int nextByte = in.read();
            if (nextByte < 0) {
                return nextByte;
            }
            bitsCached |= (nextByte << bitsCachedSize);
            bitsCachedSize += 8;
        }
        final int mask = (1 << codeSize) - 1;
        final int code = (bitsCached & mask);
        bitsCached >>>= codeSize;
        bitsCachedSize -= codeSize;
        ++totalCodesRead;
        return code;
    }
    
    private void reAlignReading() throws IOException {
        // "compress" works in multiples of 8 symbols, each codeBits bits long.
        // When codeBits changes, the remaining unused symbols in the current
        // group of 8 are still written out, in the old codeSize,
        // as garbage values (usually zeroes) that need to be skipped.
        long codeReadsToThrowAway = 8 - (totalCodesRead % 8);
        if (codeReadsToThrowAway == 8) {
            codeReadsToThrowAway = 0;
        }
        for (long i = 0; i < codeReadsToThrowAway; i++) {
            readNextCode();
        }
        bitsCached = 0;
        bitsCachedSize = 0;
    }
    
    private void addEntry(int previousEntry, byte character) throws IOException {
        if (tableSize >= ((1 << codeSize) - 1)) {
            if (tableSize == ((1 << codeSize) - 1)) {
                if (codeSize < maxCodeSize) {
                    reAlignReading();
                    codeSize++;
                    prefixes[tableSize] = previousEntry;
                    characters[tableSize] = character;
                    tableSize++;
                } else {
                    prefixes[tableSize] = previousEntry;
                    characters[tableSize] = character;
                    tableSize++;
                }
            }
        } else {
            prefixes[tableSize] = previousEntry;
            characters[tableSize] = character;
            tableSize++;
        }
    }

    public int read() throws IOException {
        byte[] b = new byte[1];
        int ret = read(b);
        if (ret < 0) {
            return ret;
        }
        return 0xff & b[0];
    }
    
    public int read(byte[] b, int off, int len) throws IOException {
        int bytesRead = 0;
        int remainingInStack = outputStack.length - outputStackLocation;
        if (remainingInStack > 0) {
            int maxLength = Math.min(remainingInStack, len);
            System.arraycopy(outputStack, outputStackLocation, b, off, maxLength);
            outputStackLocation += maxLength;
            off += maxLength;
            len -= maxLength;
            bytesRead += maxLength;
        }
        while (len > 0) {
            int result = decompressNextSymbol();
            if (result < 0) {
                return (bytesRead > 0) ? bytesRead : result;
            }
            remainingInStack = outputStack.length - outputStackLocation;
            if (remainingInStack > 0) {
                int maxLength = Math.min(remainingInStack, len);
                System.arraycopy(outputStack, outputStackLocation, b, off, maxLength);
                outputStackLocation += maxLength;
                off += maxLength;
                len -= maxLength;
                bytesRead += maxLength;
            }
        }
        return bytesRead;
    }
    
    private int decompressNextSymbol() throws IOException {
        //
        //                   table entry    table entry
        //                  _____________   _____
        //    table entry  /             \ /     \
        //    ____________/               \       \
        //   /           / \             / \       \
        //  +---+---+---+---+---+---+---+---+---+---+
        //  | . | . | . | . | . | . | . | . | . | . |
        //  +---+---+---+---+---+---+---+---+---+---+
        //  |<--------->|<------------->|<----->|<->|
        //     symbol        symbol      symbol  symbol
        //
        // Symbols are indexes into a table of table entries. Indexes
        // sequentially increase up to the maximum size of the table.
        // The bit count used by each index increases up to the minimum
        // size needed the index the highest table entry.
        //
        // To construct a table entry for a symbol,
        // we need the symbol's text, and the first character of the
        // next symbol's text. When a symbol is the immediately previous
        // table entry's symbol, that symbol's text is the previous symbol's text + 1 character.
        //
        // The compression process adds table entries after writing the symbol.
        // Since adding entries can increase the code size, the 
        //
        final int code = readNextCode();
        if (code < 0) {
            return -1;
        } else if (blockMode && code == clearCode) {
            clearEntries();
            reAlignReading();
            codeSize = 9;
            previousEntry = -1;
            return 0;
        } else {
            boolean addedUnfinishedEntry = false;
            if (code == tableSize) {
                // must be a repeat of the previous entry we haven't added yet
                if (previousEntry == -1) {
                    // ... which isn't possible for the very first code
                    throw new IOException("The first code can't be a reference to code before itself");
                }
                byte firstCharacter = 0;
                for (int last = previousEntry; last >= 0; last = prefixes[last]) {
                    firstCharacter = characters[last];
                }
                addEntry(previousEntry, firstCharacter);
                addedUnfinishedEntry = true;
            } else if (code > tableSize) {
                throw new IOException(String.format("Invalid %d bit code 0x%x", codeSize, code));
            }
            for (int entry = code; entry >= 0; entry = prefixes[entry]) {
                outputStack[--outputStackLocation] = characters[entry];
            }
            if (previousEntry != -1 && !addedUnfinishedEntry) {
                addEntry(previousEntry, outputStack[outputStackLocation]);
            }
            previousEntry = code;
            return outputStackLocation;
        }
    }
}
