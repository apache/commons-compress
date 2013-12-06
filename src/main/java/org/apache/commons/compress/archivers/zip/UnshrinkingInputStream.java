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
package org.apache.commons.compress.archivers.zip;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.compressors.CompressorInputStream;

/**
 * Input stream that decompresses ZIP method 1 (unshrinking). A variation of the LZW algorithm, with some twists.
 * @NotThreadSafe
 * @since 1.7
 */
public class UnshrinkingInputStream extends CompressorInputStream {
    private final InputStream in;
    private final int clearCode;
    private final int MAX_CODE_SIZE = 13;
    private int codeSize = 9;
    private int bitsCached = 0;
    private int bitsCachedSize = 0;
    private int previousCode = -1;
    private int tableSize = 0;
    private final int[] prefixes;
    private final byte[] characters;
    private final boolean[] isUsed;
    private final byte[] outputStack;
    private int outputStackLocation;
    
    public UnshrinkingInputStream(InputStream inputStream) throws IOException {
        this.in = inputStream;
        clearCode = (1 << (codeSize - 1));
        final int maxTableSize = 1 << MAX_CODE_SIZE;
        prefixes = new int[maxTableSize];
        characters = new byte[maxTableSize];
        isUsed = new boolean[maxTableSize];
        outputStack = new byte[maxTableSize];
        outputStackLocation = maxTableSize;
        for (int i = 0; i < (1 << 8); i++) {
            prefixes[i] = -1;
            characters[i] = (byte)i;
            isUsed[i] = true;
        }
        tableSize = clearCode + 1;
    }
    
    public void close() throws IOException {
        in.close();
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
        return code;
    }
    
    private int addEntry(int previousCode, byte character) throws IOException {
        final int maxTableSize = 1 << MAX_CODE_SIZE;
        while ((tableSize < maxTableSize) && isUsed[tableSize]) {
            tableSize++;
        }
        if (tableSize < maxTableSize) {
            final int index = tableSize;
            prefixes[tableSize] = previousCode;
            characters[tableSize] = character;
            isUsed[tableSize] = true;
            tableSize++;
            return index;
        } else {
            return -1;
        }
    }
    
    private void partialClear() throws IOException {
        final boolean[] isParent = new boolean[1 << MAX_CODE_SIZE];
        for (int i = 0; i < isUsed.length; i++) {
            if (isUsed[i] && prefixes[i] != -1) {
                isParent[prefixes[i]] = true;
            }
        }
        for (int i = clearCode + 1; i < isParent.length; i++) {
            if (!isParent[i]) {
                isUsed[i] = false;
                prefixes[i] = -1;
            }
        }
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
        final int code = readNextCode();
        if (code < 0) {
            return -1;
        } else if (code == clearCode) {
            final int subCode = readNextCode();
            if (subCode < 0) {
                throw new IOException("Unexpected EOF;");
            } else if (subCode == 1) {
                if (codeSize < MAX_CODE_SIZE) {
                    codeSize++;
                } else {
                    throw new IOException("Attempt to increase code size beyond maximum");
                }
            } else if (subCode == 2) {
                partialClear();
                tableSize = clearCode + 1;
            } else {
                throw new IOException("Invalid clear code subcode " + subCode);
            }
            return 0;
        } else {
            boolean addedUnfinishedEntry = false;
            final int effectiveCode;
            if (isUsed[code]) {
                effectiveCode = code;
            } else {
                // must be a repeat of the previous entry we haven't added yet
                if (previousCode == -1) {
                    // ... which isn't possible for the very first code
                    throw new IOException("The first code can't be a reference to its preceding code");
                }
                byte firstCharacter = 0;
                for (int last = previousCode; last >= 0; last = prefixes[last]) {
                    firstCharacter = characters[last];
                }
                effectiveCode = addEntry(previousCode, firstCharacter);
                addedUnfinishedEntry = true;
            }
            for (int entry = effectiveCode; entry >= 0; entry = prefixes[entry]) {
                outputStack[--outputStackLocation] = characters[entry];
            }
            if (previousCode != -1 && !addedUnfinishedEntry) {
                addEntry(previousCode, outputStack[outputStackLocation]);
            }
            previousCode = code;
            return outputStackLocation;
        }
    }
    
    public int read() throws IOException {
        byte[] b = new byte[1];
        int ret;
        while ((ret = read(b)) == 0) {
        }
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
                if (bytesRead > 0) {
                    count(bytesRead);
                    return bytesRead;
                } else {
                    return result;
                }
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
        count(bytesRead);
        return bytesRead;
    }
}
