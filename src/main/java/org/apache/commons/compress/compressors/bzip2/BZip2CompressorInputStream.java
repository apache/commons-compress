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

/*
 * This package is based on the work done by Keiron Liddle, Aftex Software
 * <keiron@aftexsw.com> to whom the Ant project is very grateful for his
 * great code.
 */
package org.apache.commons.compress.compressors.bzip2;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.utils.InputStreamStatistics;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CloseShieldInputStream;

/**
 * An input stream that decompresses from the BZip2 format to be read as any other stream.
 *
 * @NotThreadSafe
 */
public class BZip2CompressorInputStream extends CompressorInputStream implements BZip2Constants, InputStreamStatistics {

    // package private for testing
    static final class Data {

        // (with blockSize 900k)
        final boolean[] inUse = new boolean[256]; // 256 byte
        // Always equal to the number of true values in inUse[] plus 2.
        private int inUseCount = 2;

        final byte[] seqToUnseq = new byte[256]; // 256 byte
        final byte[] selector = new byte[MAX_SELECTORS]; // 18002 byte
        final byte[] selectorMtf = new byte[MAX_SELECTORS]; // 18002 byte

        /**
         * Freq table collected to save a pass over the data during decompression.
         */
        final int[] unzftab = new int[256]; // 1024 byte

        /**
         * Huffman decoding tables, one group per {@link BZip2Constants#N_GROUPS}: a lookup table indexed by the next {@link #FAST_BITS} bits of input
         * (entry = symbol {@code << 8 | code length}, 0 = no code of at most {@link #FAST_BITS} bits matches), and the canonical-code tables for longer codes.
         */
        final int[] fastTable = new int[N_GROUPS << FAST_BITS];
        final int[][] limit = new int[N_GROUPS][MAX_CODE_LEN + 2];
        final int[][] bias = new int[N_GROUPS][MAX_CODE_LEN + 2];
        final int[][] perm = new int[N_GROUPS][MAX_ALPHA_SIZE];
        final int[] minLens = new int[N_GROUPS];
        final int[] maxLens = new int[N_GROUPS];
        /**
         * Number of Huffman groups in the current block.
         */
        int nGroups;
        /**
         * Scratch space for building the Huffman tables.
         */
        final int[] codeLengths = new int[MAX_ALPHA_SIZE];
        final int[] lengthCount = new int[MAX_CODE_LEN + 2];
        final int[] lengthOffset = new int[MAX_CODE_LEN + 2];

        final int[] cftab = new int[257]; // 1028 byte
        final int[] getAndMoveToFrontDecode_yy = new int[256]; // 1024 byte
        // byte
        final byte[] recvDecodingTables_pos = new byte[N_GROUPS]; // 6 byte
        // ---------------
        // 60798 byte

        int[] tt; // 3600000 byte
        final byte[] ll8; // 900000 byte

        // ---------------
        // 4560782 byte
        // ===============

        Data(final int blockSize100k) {
            this.ll8 = new byte[blockSize100k * BASEBLOCKSIZE];
        }

        /**
         * Initializes the {@link #tt} array.
         *
         * This method is called when the required length of the array is known. I don't initialize it at construction time to avoid unnecessary memory
         * allocation when compressing small files.
         */
        int[] initTT(final int length) {
            int[] ttShadow = this.tt;

            // tt.length should always be >= length, but theoretically
            // it can happen, if the compressor mixed small and large
            // blocks. Normally only the last block will be smaller
            // than others.
            if (ttShadow == null || ttShadow.length < length) {
                this.tt = ttShadow = new int[length];
            }

            return ttShadow;
        }

    }

    /**
     * Number of input bits used to index the Huffman lookup tables. Codes of at most this length (the vast majority; encoders emit at most 17 bits and
     * typical text needs 3-12) are decoded with one table lookup, longer ones fall back to the canonical bit-by-bit decoder.
     */
    static final int FAST_BITS = 10;

    /**
     * The Huffman decoding loop refills its bit buffer when fewer than this many bits are buffered; must be greater than {@link #MAX_CODE_LEN} + 1 so
     * that a slow-path decode never needs to refill while it has bits left.
     */
    private static final int REFILL_THRESHOLD = 25;

    private static final int EOF = 0;

    private static final int START_BLOCK_STATE = 1;

    private static final int RAND_PART_A_STATE = 2;

    private static final int RAND_PART_B_STATE = 3;

    private static final int RAND_PART_C_STATE = 4;

    private static final int NO_RAND_PART_A_STATE = 5;
    private static final int NO_RAND_PART_B_STATE = 6;

    private static final int NO_RAND_PART_C_STATE = 7;

    /**
     * Builds the decoding tables of one Huffman group from {@code data.codeLengths[0..alphaSize)}, with the same validation (and messages) as
     * {@code org.apache.commons.compress.huffman.HuffmanDecoder}: code lengths must be in {@code [1, MAX_CODE_LEN]} and must not over-subscribe the code
     * space (Kraft's inequality). Incomplete codes are accepted.
     */
    static void buildHuffmanTables(final Data data, final int group, final int alphaSize) throws IOException {
        final int[] codeLengths = data.codeLengths;
        // 1) Validate and find min/max lengths, in symbol order.
        int min = MAX_CODE_LEN;
        int max = 0;
        for (int i = 0; i < alphaSize; i++) {
            final int len = codeLengths[i];
            if (len < 1 || len > MAX_CODE_LEN) {
                throw new CompressorException(String.format("Invalid code length at symbol %d: %d (expected in [%d, %d])", i, len, 1, MAX_CODE_LEN));
            }
            if (len < min) {
                min = len;
            }
            if (len > max) {
                max = len;
            }
        }
        if (max == 0) {
            throw new CompressorException("All code lengths are zero");
        }
        // 2) Histogram of code lengths and Kraft's inequality.
        final int[] count = data.lengthCount;
        Arrays.fill(count, 0);
        for (int i = 0; i < alphaSize; i++) {
            count[codeLengths[i]]++;
        }
        int availableNodes = 1;
        for (int len = 1; len <= max; len++) {
            availableNodes <<= 1;
            if (count[len] > availableNodes) {
                throw new CompressorException("Tree contains too many leaf nodes for code length %d: %d leaf nodes, but only %d nodes available", len,
                        count[len], availableNodes);
            }
            availableNodes -= count[len];
        }
        // 3) Symbols sorted by (length, symbol); offset[len] ends up pointing at the last symbol of each length.
        final int[] offset = data.lengthOffset;
        offset[0] = -1;
        for (int len = 1; len <= max; len++) {
            offset[len] = offset[len - 1] + count[len - 1];
        }
        final int[] perm = data.perm[group];
        for (int i = 0; i < alphaSize; i++) {
            perm[++offset[codeLengths[i]]] = i;
        }
        // 4) Largest code of each length and the bias between codes and indices into perm.
        final int[] limit = data.limit[group];
        final int[] bias = data.bias[group];
        int firstCode = 0;
        for (int len = min; len <= max; len++) {
            firstCode += count[len];
            limit[len] = firstCode - 1;
            bias[len] = limit[len] - offset[len];
            firstCode <<= 1;
        }
        data.minLens[group] = min;
        data.maxLens[group] = max;
        // 5) Lookup table for codes of at most FAST_BITS bits: canonical codes are consecutive within a length, in symbol order.
        final int[] fast = data.fastTable;
        final int base = group << FAST_BITS;
        Arrays.fill(fast, base, base + (1 << FAST_BITS), 0);
        int code = 0;
        int symbolIndex = 0;
        for (int len = 1; len <= max && len <= FAST_BITS; len++) {
            final int n = count[len];
            for (int k = 0; k < n; k++, code++) {
                final int entry = perm[symbolIndex++] << 8 | len;
                final int from = base + (code << FAST_BITS - len);
                Arrays.fill(fast, from, from + (1 << FAST_BITS - len), entry);
            }
            code <<= 1;
        }
    }

    private static void checkBounds(final int checkVal, final int limitExclusive, final String name) throws IOException {
        if (checkVal < 0) {
            throw new CompressorException("Corrupted input, '%s' value negative", name);
        }
        if (checkVal >= limitExclusive) {
            throw new CompressorException("Corrupted input, '%s' value too big", name);
        }
    }

    private static void makeMaps(final Data data) throws IOException {
        final boolean[] inUse = data.inUse;
        final byte[] seqToUnseq = data.seqToUnseq;

        int nInUseShadow = 0;

        for (int i = 0; i < 256; i++) {
            if (inUse[i]) {
                seqToUnseq[nInUseShadow++] = (byte) i;
            }
        }

        data.inUseCount = nInUseShadow;
    }
    /**
     * Checks if the signature matches what is expected for a bzip2 file.
     *
     * @param signature The bytes to check.
     * @param length    The number of bytes to check.
     * @return true, if this stream is a bzip2 compressed stream, false otherwise.
     * @since 1.1
     */
    public static boolean matches(final byte[] signature, final int length) {
        return length >= 3 && signature[0] == 'B' && signature[1] == 'Z' && signature[2] == 'h';
    }

    // Variables used by setup* methods exclusively

    static void recvDecodingTables(final BZip2BitReader bin, final Data dataShadow) throws IOException {
        final boolean[] inUse = dataShadow.inUse;
        final byte[] pos = dataShadow.recvDecodingTables_pos;
        final byte[] selector = dataShadow.selector;
        final byte[] selectorMtf = dataShadow.selectorMtf;

        int inUse16 = 0;

        /* Receive the mapping table */
        for (int i = 0; i < 16; i++) {
            if (bin.readBits(1) != 0) {
                inUse16 |= 1 << i;
            }
        }

        Arrays.fill(inUse, false);
        for (int i = 0; i < 16; i++) {
            if ((inUse16 & 1 << i) != 0) {
                final int i16 = i << 4;
                for (int j = 0; j < 16; j++) {
                    if (bin.readBits(1) != 0) {
                        inUse[i16 + j] = true;
                    }
                }
            }
        }

        makeMaps(dataShadow);
        final int alphaSize = dataShadow.inUseCount + 2;
        /* Now the selectors */
        final int nGroups = bin.readBits(3);
        final int selectors = bin.readBits(15);
        if (selectors < 0) {
            throw new CompressorException("Corrupted input, nSelectors value negative");
        }
        checkBounds(alphaSize, MAX_ALPHA_SIZE + 1, "alphaSize");
        checkBounds(nGroups, N_GROUPS + 1, "nGroups");

        // Don't fail on nSelectors overflowing boundaries but discard the values in overflow
        // See https://gnu.wildebeest.org/blog/mjw/2019/08/02/bzip2-and-the-cve-that-wasnt/
        // and https://sourceware.org/ml/bzip2-devel/2019-q3/msg00007.html

        for (int i = 0; i < selectors; i++) {
            int j = 0;
            while (bin.readBits(1) != 0) {
                j++;
            }
            if (i < MAX_SELECTORS) {
                selectorMtf[i] = (byte) j;
            }
        }
        final int nSelectors = Math.min(selectors, MAX_SELECTORS);

        /* Undo the MTF values for the selectors. */
        for (int v = nGroups; --v >= 0;) {
            pos[v] = (byte) v;
        }

        for (int i = 0; i < nSelectors; i++) {
            int v = selectorMtf[i] & 0xff;
            checkBounds(v, N_GROUPS, "selectorMtf");
            final byte tmp = pos[v];
            while (v > 0) {
                // nearly all times v is zero, 4 in most other cases
                pos[v] = pos[v - 1];
                v--;
            }
            pos[0] = tmp;
            selector[i] = tmp;
        }

        /* Now the Huffman coding tables */
        final int[] codeLengths = dataShadow.codeLengths;
        for (int t = 0; t < nGroups; t++) {
            int curr = bin.readBits(5);
            for (int i = 0; i < alphaSize; i++) {
                while (bin.readBits(1) != 0) {
                    curr += bin.readBits(1) != 0 ? -1 : 1;
                }
                codeLengths[i] = curr;
            }
            // Same limits as in the reference C implementation of bzip2
            buildHuffmanTables(dataShadow, t, alphaSize);
        }
        dataShadow.nGroups = nGroups;
    }

    /**
     * Index of the last char in the block, so the block size == last + 1.
     */
    private int last;

    /**
     * Index in zptr[] of original string after sorting.
     */
    private int origPtr;
    /**
     * always: in the range 0 .. 9. The current block size is 100000 * this number.
     */
    private int blockSize100k;
    private boolean blockRandomised;
    private final CRC crc = new CRC();
    private BZip2BitReader bin;
    private final boolean decompressConcatenated;
    private int currentState = START_BLOCK_STATE;
    private int storedBlockCRC;
    private int storedCombinedCRC;
    private int computedCombinedCRC;
    private int su_count;
    private int su_ch2;
    private int su_chPrev;
    private int su_i2;
    private int su_j2;

    private int su_rNToGo;

    private int su_rTPos;

    private int su_tPos;

    private char su_z;

    /**
     * All memory intensive stuff. This field is initialized by initBlock().
     */
    private BZip2CompressorInputStream.Data data;

    /**
     * Constructs a new BZip2CompressorInputStream which decompresses bytes read from the specified stream. This doesn't support decompressing concatenated .bz2
     * files.
     *
     * @param in The InputStream from which this object should be created.
     * @throws IOException          if the stream content is malformed or an I/O error occurs.
     * @throws NullPointerException if {@code in == null}.
     */
    public BZip2CompressorInputStream(final InputStream in) throws IOException {
        this(in, false);
    }

    /**
     * Constructs a new BZip2CompressorInputStream which decompresses bytes read from the specified stream.
     *
     * @param in                     The InputStream from which this object should be created.
     * @param decompressConcatenated if true, decompress until the end of the input; if false, stop after the first .bz2 stream and leave the input position to
     *                               point to the next byte after the .bz2 stream
     *
     * @throws IOException if {@code in == null}, the stream content is malformed, or an I/O error occurs.
     */
    public BZip2CompressorInputStream(final InputStream in, final boolean decompressConcatenated) throws IOException {
        this.bin = new BZip2BitReader(in == System.in ? CloseShieldInputStream.wrap(in) : in);
        this.decompressConcatenated = decompressConcatenated;
        init(true);
        initBlock();
    }

    @Override
    public void close() throws IOException {
        final BZip2BitReader inShadow = this.bin;
        if (inShadow != null) {
            try {
                inShadow.close();
            } finally {
                this.data = null;
                this.bin = null;
            }
        }
    }

    private boolean complete() throws IOException {
        this.storedCombinedCRC = bin.readBits(32);
        this.currentState = EOF;
        this.data = null;
        if (this.storedCombinedCRC != this.computedCombinedCRC) {
            throw new CompressorException("BZip2 CRC error");
        }
        // Look for the next .bz2 stream if decompressing
        // concatenated files.
        return !decompressConcatenated || !init(false);
    }

    /**
     * Decodes one symbol bit by bit (canonical Huffman decoding): used for codes longer than {@link #FAST_BITS} bits, for prefixes no code claims (the
     * stream is then corrupt) and near the end of the input. The bit buffer state must have been written back to {@code bin} by the caller.
     */
    private int decodeSymbolSlow(final int group) throws IOException {
        final BZip2BitReader bin = this.bin;
        final Data dataShadow = this.data;
        final int[] limit = dataShadow.limit[group];
        final int maxLen = dataShadow.maxLens[group];
        int len = dataShadow.minLens[group];
        int code = bin.readHuffmanBits(len);
        while (len <= maxLen && code > limit[len]) {
            code = code << 1 | bin.readHuffmanBits(1);
            len++;
        }
        if (len > maxLen) {
            throw new CompressorException("Invalid Huffman code: " + code);
        }
        return dataShadow.perm[group][code - dataShadow.bias[group][len]];
    }

    private void endBlock() throws IOException {
        final int computedBlockCRC = this.crc.getValue();
        // A bad CRC is considered a fatal error.
        if (this.storedBlockCRC != computedBlockCRC) {
            // make next blocks readable without error
            // (repair feature, not yet documented, not tested)
            this.computedCombinedCRC = this.storedCombinedCRC << 1 | this.storedCombinedCRC >>> 31;
            this.computedCombinedCRC ^= this.storedBlockCRC;
            throw new CompressorException("BZip2 CRC error");
        }
        this.computedCombinedCRC = this.computedCombinedCRC << 1 | this.computedCombinedCRC >>> 31;
        this.computedCombinedCRC ^= computedBlockCRC;
    }

    private void getAndMoveToFrontDecode() throws IOException {
        final BZip2BitReader bin = this.bin;
        this.origPtr = bin.readBits(24);
        final Data dataShadow = this.data;
        recvDecodingTables(bin, dataShadow);
        final byte[] ll8 = dataShadow.ll8;
        final int[] unzftab = dataShadow.unzftab;
        final byte[] selector = dataShadow.selector;
        final byte[] seqToUnseq = dataShadow.seqToUnseq;
        final int[] yy = dataShadow.getAndMoveToFrontDecode_yy;
        final int[] fast = dataShadow.fastTable;
        final int nGroups = dataShadow.nGroups;
        final int limitLast = this.blockSize100k * 100000;
        /*
         * Setting up the unzftab entries here is not strictly necessary, but it does save having to do it later in a separate pass, and so saves a block's
         * worth of cache misses.
         */
        for (int i = 256; --i >= 0;) {
            yy[i] = i;
            unzftab[i] = 0;
        }
        int groupPos = G_SIZE - 1;
        final int eob = dataShadow.inUseCount + 1;
        int lastShadow = -1;
        // Initialize group and selector
        int groupNo = 0;
        int zt = selector[groupNo] & 0xff;
        checkBounds(zt, nGroups, "zt");
        int fastBase = zt << FAST_BITS;
        // RUNA/RUNB accumulation state
        boolean inRun = false;
        int runLength = -1;
        int runWeight = 1;
        boolean first = true;
        long bitBuffer = bin.bitBuffer;
        int bitCount = bin.bitCount;
        try {
            while (true) {
                // Every symbol but the first is preceded by the group bookkeeping.
                if (first) {
                    first = false;
                } else if (groupPos == 0) {
                    groupPos = G_SIZE - 1;
                    checkBounds(++groupNo, selector.length, "groupNo");
                    zt = selector[groupNo] & 0xff;
                    checkBounds(zt, nGroups, "zt");
                    fastBase = zt << FAST_BITS;
                } else {
                    groupPos--;
                }
                // Decode one symbol.
                if (bitCount < REFILL_THRESHOLD) {
                    bin.bitBuffer = bitBuffer;
                    bin.bitCount = bitCount;
                    bin.fill();
                    bitBuffer = bin.bitBuffer;
                    bitCount = bin.bitCount;
                }
                final int nextSym;
                final int entry = fast[fastBase + (int) (bitBuffer >>> 64 - FAST_BITS)];
                final int codeLen = entry & 0xff;
                if (entry != 0 && codeLen <= bitCount) {
                    nextSym = entry >>> 8;
                    bitBuffer <<= codeLen;
                    bitCount -= codeLen;
                } else {
                    bin.bitBuffer = bitBuffer;
                    bin.bitCount = bitCount;
                    nextSym = decodeSymbolSlow(zt);
                    bitBuffer = bin.bitBuffer;
                    bitCount = bin.bitCount;
                }
                if (nextSym == RUNA || nextSym == RUNB) {
                    if (!inRun) {
                        inRun = true;
                        runLength = -1;
                        runWeight = 1;
                    }
                    runLength += nextSym == RUNA ? runWeight : runWeight << 1;
                    runWeight <<= 1;
                    continue;
                }
                if (inRun) {
                    inRun = false;
                    checkBounds(runLength, ll8.length, "s");
                    final int yy0 = yy[0];
                    checkBounds(yy0, seqToUnseq.length, "yy");
                    final byte ch = seqToUnseq[yy0];
                    unzftab[ch & 0xff] += runLength + 1;
                    final int from = ++lastShadow;
                    lastShadow += runLength;
                    checkBounds(lastShadow, ll8.length, "lastShadow");
                    Arrays.fill(ll8, from, lastShadow + 1, ch);
                    if (lastShadow >= limitLast) {
                        throw new CompressorException("Block overrun while expanding RLE in MTF, %,d exceeds %,d", lastShadow, limitLast);
                    }
                }
                if (nextSym == eob) {
                    break;
                }
                if (++lastShadow >= limitLast) {
                    throw new CompressorException("Block overrun in MTF, %,d exceeds %,d", lastShadow, limitLast);
                }
                checkBounds(nextSym - 1, yy.length, "nextSym");
                final int tmp = yy[nextSym - 1];
                checkBounds(tmp, seqToUnseq.length, "yy");
                unzftab[seqToUnseq[tmp] & 0xff]++;
                ll8[lastShadow] = seqToUnseq[tmp];
                /*
                 * This loop is hammered during decompression, hence avoid native method call overhead of System.arraycopy for very small ranges to copy.
                 */
                if (nextSym <= 16) {
                    for (int j = nextSym - 1; j > 0;) {
                        yy[j] = yy[--j];
                    }
                } else {
                    System.arraycopy(yy, 0, yy, 1, nextSym - 1);
                }
                yy[0] = tmp;
            }
        } finally {
            bin.bitBuffer = bitBuffer;
            bin.bitCount = bitCount;
        }
        this.last = lastShadow;
    }

    /**
     * @since 1.17
     */
    @Override
    public long getCompressedCount() {
        return bin.getBytesRead();
    }

    private boolean init(final boolean isFirstStream) throws IOException {
        if (bin == null) {
            throw new CompressorException("No InputStream");
        }
        if (!isFirstStream) {
            bin.clear();
        }
        final int magic0 = bin.readByteOrEof();
        if (magic0 == -1 && !isFirstStream) {
            return false;
        }
        final int magic1 = bin.readByteOrEof();
        final int magic2 = bin.readByteOrEof();
        if (magic0 != 'B' || magic1 != 'Z' || magic2 != 'h') {
            throw new CompressorException(isFirstStream ? "Stream is not in the BZip2 format" : "Unexpected data after a valid BZip2 stream");
        }
        final int blockSize = bin.readByteOrEof();
        if (blockSize < '1' || blockSize > '9') {
            throw new CompressorException("BZip2 block size is invalid");
        }
        this.blockSize100k = blockSize - '0';
        this.computedCombinedCRC = 0;
        return true;
    }

    private void initBlock() throws IOException {
        final BZip2BitReader bin = this.bin;
        int magic0;
        int magic1;
        int magic2;
        int magic3;
        int magic4;
        int magic5;

        while (true) {
            // Get the block magic bytes.
            magic0 = bin.readBits(8);
            magic1 = bin.readBits(8);
            magic2 = bin.readBits(8);
            magic3 = bin.readBits(8);
            magic4 = bin.readBits(8);
            magic5 = bin.readBits(8);

            // If isn't end of stream magic, break out of the loop.
            if (magic0 != 0x17 || magic1 != 0x72 || magic2 != 0x45 || magic3 != 0x38 || magic4 != 0x50 || magic5 != 0x90) {
                break;
            }

            // End of stream was reached. Check the combined CRC and
            // advance to the next .bz2 stream if decoding concatenated
            // streams.
            if (complete()) {
                return;
            }
        }

        if (magic0 != 0x31 || // '1'
                magic1 != 0x41 || // ')'
                magic2 != 0x59 || // 'Y'
                magic3 != 0x26 || // '&'
                magic4 != 0x53 || // 'S'
                magic5 != 0x59 // 'Y'
        ) {
            this.currentState = EOF;
            throw new CompressorException("Bad block header");
        }
        this.storedBlockCRC = bin.readBits(32);
        this.blockRandomised = bin.readBits(1) == 1;

        /*
         * Allocate data here instead in constructor, so we do not allocate it if the input file is empty.
         */
        if (this.data == null) {
            this.data = new Data(this.blockSize100k);
        }

        // currBlockNo++;
        getAndMoveToFrontDecode();

        this.crc.reset();
        this.currentState = START_BLOCK_STATE;
    }

    @Override
    public int read() throws IOException {
        if (this.bin != null) {
            final int r = read0();
            count(r < 0 ? -1 : 1);
            return r;
        }
        throw new CompressorException("Stream closed");
    }

    @Override
    public int read(final byte[] dest, final int offs, final int len) throws IOException {
        IOUtils.checkFromIndexSize(dest, offs, len);
        if (len == 0) {
            return 0;
        }
        if (this.bin == null) {
            throw new CompressorException("Stream closed");
        }

        final int hi = offs + len;
        int destOffs = offs;
        int b;
        while (destOffs < hi && (b = read0()) >= 0) {
            dest[destOffs++] = (byte) b;
            count(1);
        }

        return destOffs == offs ? -1 : destOffs - offs;
    }

    private int read0() throws IOException {
        switch (currentState) {
        case EOF:
            return -1;
        case START_BLOCK_STATE:
            return setupBlock();
        case RAND_PART_A_STATE:
            throw new CompressorException("Unexpected RAND_PART_A_STATE in read0()");
        case RAND_PART_B_STATE:
            return setupRandPartB();
        case RAND_PART_C_STATE:
            return setupRandPartC();
        case NO_RAND_PART_A_STATE:
            throw new CompressorException("Unexpected NO_RAND_PART_A_STATE in read0()");
        case NO_RAND_PART_B_STATE:
            return setupNoRandPartB();
        case NO_RAND_PART_C_STATE:
            return setupNoRandPartC();
        default:
            throw new CompressorException("Unexpected %s in read0()", currentState);
        }
    }

    private int setupBlock() throws IOException {
        if (currentState == EOF || this.data == null) {
            return -1;
        }

        final int[] cftab = this.data.cftab;
        final int ttLen = this.last + 1;
        // tt has size at least ttLen
        final int[] tt = this.data.initTT(ttLen);
        final byte[] ll8 = this.data.ll8;
        cftab[0] = 0;
        System.arraycopy(this.data.unzftab, 0, cftab, 1, 256);

        for (int i = 1, c = cftab[0]; i <= 256; i++) {
            c += cftab[i];
            cftab[i] = c;
        }

        for (int i = 0, lastShadow = this.last; i <= lastShadow; i++) {
            final int tmp = cftab[ll8[i] & 0xff]++;
            checkBounds(tmp, ttLen, "tt index");
            tt[tmp] = i;
        }

        if (this.origPtr < 0 || this.origPtr >= tt.length) {
            throw new CompressorException("Stream corrupted");
        }

        this.su_tPos = tt[this.origPtr];
        this.su_count = 0;
        this.su_i2 = 0;
        this.su_ch2 = 256; /* not a char and not EOF */

        if (this.blockRandomised) {
            this.su_rNToGo = 0;
            this.su_rTPos = 0;
            return setupRandPartA();
        }
        return setupNoRandPartA();
    }

    private int setupNoRandPartA() throws IOException {
        if (this.su_i2 <= this.last) {
            this.su_chPrev = this.su_ch2;
            final int su_ch2Shadow = this.data.ll8[this.su_tPos] & 0xff;
            this.su_ch2 = su_ch2Shadow;
            checkBounds(this.su_tPos, this.data.tt.length, "su_tPos");
            this.su_tPos = this.data.tt[this.su_tPos];
            this.su_i2++;
            this.currentState = NO_RAND_PART_B_STATE;
            this.crc.update(su_ch2Shadow);
            return su_ch2Shadow;
        }
        this.currentState = NO_RAND_PART_A_STATE;
        endBlock();
        initBlock();
        return setupBlock();
    }

    private int setupNoRandPartB() throws IOException {
        if (this.su_ch2 != this.su_chPrev) {
            this.su_count = 1;
            return setupNoRandPartA();
        }
        if (++this.su_count >= 4) {
            checkBounds(this.su_tPos, this.data.ll8.length, "su_tPos");
            this.su_z = (char) (this.data.ll8[this.su_tPos] & 0xff);
            this.su_tPos = this.data.tt[this.su_tPos];
            this.su_j2 = 0;
            return setupNoRandPartC();
        }
        return setupNoRandPartA();
    }

    private int setupNoRandPartC() throws IOException {
        if (this.su_j2 < this.su_z) {
            final int su_ch2Shadow = this.su_ch2;
            this.crc.update(su_ch2Shadow);
            this.su_j2++;
            this.currentState = NO_RAND_PART_C_STATE;
            return su_ch2Shadow;
        }
        this.su_i2++;
        this.su_count = 0;
        return setupNoRandPartA();
    }

    private int setupRandPartA() throws IOException {
        if (this.su_i2 <= this.last) {
            this.su_chPrev = this.su_ch2;
            int su_ch2Shadow = this.data.ll8[this.su_tPos] & 0xff;
            checkBounds(this.su_tPos, this.data.tt.length, "su_tPos");
            this.su_tPos = this.data.tt[this.su_tPos];
            if (this.su_rNToGo == 0) {
                this.su_rNToGo = Rand.rNums(this.su_rTPos) - 1;
                if (++this.su_rTPos == 512) {
                    this.su_rTPos = 0;
                }
            } else {
                this.su_rNToGo--;
            }
            this.su_ch2 = su_ch2Shadow ^= this.su_rNToGo == 1 ? 1 : 0;
            this.su_i2++;
            this.currentState = RAND_PART_B_STATE;
            this.crc.update(su_ch2Shadow);
            return su_ch2Shadow;
        }
        endBlock();
        initBlock();
        return setupBlock();
    }

    private int setupRandPartB() throws IOException {
        if (this.su_ch2 != this.su_chPrev) {
            this.currentState = RAND_PART_A_STATE;
            this.su_count = 1;
            return setupRandPartA();
        }
        if (++this.su_count < 4) {
            this.currentState = RAND_PART_A_STATE;
            return setupRandPartA();
        }
        this.su_z = (char) (this.data.ll8[this.su_tPos] & 0xff);
        checkBounds(this.su_tPos, this.data.tt.length, "su_tPos");
        this.su_tPos = this.data.tt[this.su_tPos];
        if (this.su_rNToGo == 0) {
            this.su_rNToGo = Rand.rNums(this.su_rTPos) - 1;
            if (++this.su_rTPos == 512) {
                this.su_rTPos = 0;
            }
        } else {
            this.su_rNToGo--;
        }
        this.su_j2 = 0;
        this.currentState = RAND_PART_C_STATE;
        if (this.su_rNToGo == 1) {
            this.su_z ^= 1;
        }
        return setupRandPartC();
    }

    private int setupRandPartC() throws IOException {
        if (this.su_j2 < this.su_z) {
            this.crc.update(this.su_ch2);
            this.su_j2++;
            return this.su_ch2;
        }
        this.currentState = RAND_PART_A_STATE;
        this.su_i2++;
        this.su_count = 0;
        return setupRandPartA();
    }
}
