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
package org.apache.commons.compress.compressors.bzip2;

import java.util.Arrays;

import org.apache.commons.compress.compressors.CompressorException;

/**
 * Inverse Burrows-Wheeler transform of one block, walking several independent chains of the successor permutation at once.
 * <p>
 * The classic traversal follows {@code row = tt[row] >>> 8} once per output byte: a single chain of dependent loads over an array that, for 900 k blocks,
 * does not fit the L2 cache, so decoding is bound by memory latency. This implementation starts {@value #CHAINS} chains at different rows and steps them in
 * lockstep, which gives the CPU several outstanding cache misses instead of one. A chain stops when it reaches the start row of another chain (rows that
 * start a chain are flagged in {@code tt}); it is then restarted at a row nobody has visited yet. Because every chain walks the cycle forwards from its start
 * row until it meets another chain's start row, the walked segments never overlap and cover every row exactly once. Afterwards the segments are copied to
 * the output in cycle order, starting with the segment that begins at the row of the first byte.
 * </p>
 * <p>
 * Corrupt input can produce a permutation with several cycles. The byte-at-a-time traversal then repeats the cycle containing the first byte until the block
 * length is reached, and this implementation reproduces exactly that output.
 * </p>
 *
 * @NotThreadSafe
 */
final class BZip2InverseBwt {

    /**
     * Flag in a {@code tt} entry: this row starts a chain.
     */
    private static final int START = 0x80000000;

    /**
     * Flag in a {@code tt} entry: the byte of this row has been produced.
     */
    private static final int VISITED = 0x40000000;

    /**
     * Mask for the successor row after shifting a {@code tt} entry right by 8 (drops the two flag bits; 22 bits &gt; 900,000).
     */
    private static final int ROW_MASK = 0x3FFFFF;

    /**
     * Number of chains walked concurrently.
     */
    static final int CHAINS = 8;

    /**
     * Bit mask with one bit per chain.
     */
    private static final int ALL_CHAINS = (1 << CHAINS) - 1;

    /**
     * Chains write their bytes into regions of this size taken from a shared pool.
     */
    private static final int REGION = 2048;

    /**
     * Upper bound on the number of segments (chain starts) per block; the pool has room for one partially filled region per segment.
     */
    private static final int MAX_SEGMENTS = 256;

    private static final int HASH_SIZE = 1024;

    private final byte[] pool;
    private final int[] regionNext;
    private final int[] regionLength;
    private final int[] segmentFirstRegion = new int[MAX_SEGMENTS];
    private final int[] segmentNext = new int[MAX_SEGMENTS];
    private final int[] hashRow = new int[HASH_SIZE];
    private final int[] hashSegment = new int[HASH_SIZE];

    private final int[] chainSegment = new int[CHAINS];
    private final int[] chainRow = new int[CHAINS];
    private final int[] chainPos = new int[CHAINS];
    private final int[] chainEnd = new int[CHAINS];
    private final int[] chainRegion = new int[CHAINS];

    private int regions;
    private int segments;
    private int freshRow;

    BZip2InverseBwt(final int maxBlockLength) {
        final int regionCount = (maxBlockLength + REGION - 1) / REGION + MAX_SEGMENTS;
        pool = new byte[regionCount * REGION];
        regionNext = new int[regionCount];
        regionLength = new int[regionCount];
    }

    private int allocateRegion() {
        final int r = regions++;
        regionNext[r] = -1;
        return r;
    }

    private int hashGet(final int row) {
        int h = row * 0x9E3779B1 >>> 22;
        while (hashRow[h] != row) {
            if (hashRow[h] < 0) {
                return -1;
            }
            h = h + 1 & HASH_SIZE - 1;
        }
        return hashSegment[h];
    }

    private void hashPut(final int row, final int segment) {
        int h = row * 0x9E3779B1 >>> 22;
        while (hashRow[h] >= 0) {
            h = h + 1 & HASH_SIZE - 1;
        }
        hashRow[h] = row;
        hashSegment[h] = segment;
    }

    /**
     * Returns a row nobody has visited or flagged, or -1.
     */
    private int nextFreshRow(final int[] tt, final int n) {
        int row = freshRow;
        while (row < n && (tt[row] & (START | VISITED)) != 0) {
            row++;
        }
        freshRow = row + 1;
        return row < n ? row : -1;
    }

    /**
     * Starts chain {@code c} at {@code row}: opens a segment, flags the row, and produces the row's byte.
     */
    private void startChain(final int c, final int row, final int[] tt) {
        final int s = segments++;
        final int r = allocateRegion();
        segmentFirstRegion[s] = r;
        segmentNext[s] = -1;
        hashPut(row, s);
        final int v = tt[row];
        tt[row] = v | START | VISITED;
        final int pos = r * REGION;
        pool[pos] = (byte) v;
        chainSegment[c] = s;
        chainRegion[c] = r;
        chainPos[c] = pos + 1;
        chainEnd[c] = pos + REGION;
        chainRow[c] = v >>> 8 & ROW_MASK;
    }

    /**
     * Completes the segment of chain {@code c} (it reached the start row of another segment) and restarts the chain at a fresh row.
     *
     * @return false if there is no fresh row left, i.e. the chain retires.
     */
    private boolean finishSegment(final int c, final int[] tt, final int n) {
        segmentNext[chainSegment[c]] = hashGet(chainRow[c]);
        regionLength[chainRegion[c]] = chainPos[c] - chainRegion[c] * REGION;
        final int fresh = segments < MAX_SEGMENTS ? nextFreshRow(tt, n) : -1;
        if (fresh < 0) {
            return false;
        }
        startChain(c, fresh, tt);
        return true;
    }

    /**
     * The region of chain {@code c} is full: continues in a new one.
     */
    private void newRegion(final int c) {
        regionLength[chainRegion[c]] = REGION;
        final int r = allocateRegion();
        regionNext[chainRegion[c]] = r;
        chainRegion[c] = r;
        chainPos[c] = r * REGION;
        chainEnd[c] = r * REGION + REGION;
    }

    /**
     * Computes the original block from the successor permutation.
     *
     * @param tt     block: byte in the low 8 bits, successor row in the upper 24 bits (the two top bits are used as scratch flags).
     * @param origPtr row of the last byte of the block, so {@code tt[origPtr] >>> 8} is the row of the first byte.
     * @param n      block length, at least 1.
     * @param raw    receives the {@code n} bytes of the block in original order; {@code raw[n]} receives the byte that follows them on the cycle (what the
     *               byte-at-a-time traversal reads as an RLE1 repeat count after the last byte).
     * @throws CompressorException if the permutation is inconsistent (cannot happen for the output of the MTF stage; defensive).
     */
    void unwind(final int[] tt, final int origPtr, final int n, final byte[] raw) throws CompressorException {
        Arrays.fill(hashRow, -1);
        regions = 0;
        segments = 0;
        freshRow = 0;
        final byte[] pool = this.pool;
        final int[] chainRow = this.chainRow;
        final int[] chainPos = this.chainPos;
        final int[] chainEnd = this.chainEnd;
        // Chains without a fresh row to start from are parked.
        int parked = 0;
        startChain(0, tt[origPtr] >>> 8 & ROW_MASK, tt);
        for (int c = 1; c < CHAINS; c++) {
            final int row = nextFreshRow(tt, n);
            if (row < 0) {
                // Fewer rows than chains: park the rest.
                parked |= ALL_CHAINS & -1 << c;
                break;
            }
            startChain(c, row, tt);
        }
        // One step per chain per iteration, chain state in locals (the 8 blocks below are identical apart from the chain index).
        int r0 = chainRow[0];
        int p0 = chainPos[0];
        int e0 = chainEnd[0];
        int r1 = chainRow[1];
        int p1 = chainPos[1];
        int e1 = chainEnd[1];
        int r2 = chainRow[2];
        int p2 = chainPos[2];
        int e2 = chainEnd[2];
        int r3 = chainRow[3];
        int p3 = chainPos[3];
        int e3 = chainEnd[3];
        int r4 = chainRow[4];
        int p4 = chainPos[4];
        int e4 = chainEnd[4];
        int r5 = chainRow[5];
        int p5 = chainPos[5];
        int e5 = chainEnd[5];
        int r6 = chainRow[6];
        int p6 = chainPos[6];
        int e6 = chainEnd[6];
        int r7 = chainRow[7];
        int p7 = chainPos[7];
        int e7 = chainEnd[7];
        while (parked != ALL_CHAINS) {
            if ((parked & 1 << 0) == 0) {
                final int v = tt[r0];
                if (v >= 0) {
                    tt[r0] = v | VISITED;
                    pool[p0] = (byte) v;
                    r0 = v >>> 8 & ROW_MASK;
                    if (++p0 == e0) {
                        chainPos[0] = p0;
                        newRegion(0);
                        p0 = chainPos[0];
                        e0 = chainEnd[0];
                    }
                } else {
                    chainRow[0] = r0;
                    chainPos[0] = p0;
                    if (finishSegment(0, tt, n)) {
                        r0 = chainRow[0];
                        p0 = chainPos[0];
                        e0 = chainEnd[0];
                    } else {
                        parked |= 1 << 0;
                    }
                }
            }
            if ((parked & 1 << 1) == 0) {
                final int v = tt[r1];
                if (v >= 0) {
                    tt[r1] = v | VISITED;
                    pool[p1] = (byte) v;
                    r1 = v >>> 8 & ROW_MASK;
                    if (++p1 == e1) {
                        chainPos[1] = p1;
                        newRegion(1);
                        p1 = chainPos[1];
                        e1 = chainEnd[1];
                    }
                } else {
                    chainRow[1] = r1;
                    chainPos[1] = p1;
                    if (finishSegment(1, tt, n)) {
                        r1 = chainRow[1];
                        p1 = chainPos[1];
                        e1 = chainEnd[1];
                    } else {
                        parked |= 1 << 1;
                    }
                }
            }
            if ((parked & 1 << 2) == 0) {
                final int v = tt[r2];
                if (v >= 0) {
                    tt[r2] = v | VISITED;
                    pool[p2] = (byte) v;
                    r2 = v >>> 8 & ROW_MASK;
                    if (++p2 == e2) {
                        chainPos[2] = p2;
                        newRegion(2);
                        p2 = chainPos[2];
                        e2 = chainEnd[2];
                    }
                } else {
                    chainRow[2] = r2;
                    chainPos[2] = p2;
                    if (finishSegment(2, tt, n)) {
                        r2 = chainRow[2];
                        p2 = chainPos[2];
                        e2 = chainEnd[2];
                    } else {
                        parked |= 1 << 2;
                    }
                }
            }
            if ((parked & 1 << 3) == 0) {
                final int v = tt[r3];
                if (v >= 0) {
                    tt[r3] = v | VISITED;
                    pool[p3] = (byte) v;
                    r3 = v >>> 8 & ROW_MASK;
                    if (++p3 == e3) {
                        chainPos[3] = p3;
                        newRegion(3);
                        p3 = chainPos[3];
                        e3 = chainEnd[3];
                    }
                } else {
                    chainRow[3] = r3;
                    chainPos[3] = p3;
                    if (finishSegment(3, tt, n)) {
                        r3 = chainRow[3];
                        p3 = chainPos[3];
                        e3 = chainEnd[3];
                    } else {
                        parked |= 1 << 3;
                    }
                }
            }
            if ((parked & 1 << 4) == 0) {
                final int v = tt[r4];
                if (v >= 0) {
                    tt[r4] = v | VISITED;
                    pool[p4] = (byte) v;
                    r4 = v >>> 8 & ROW_MASK;
                    if (++p4 == e4) {
                        chainPos[4] = p4;
                        newRegion(4);
                        p4 = chainPos[4];
                        e4 = chainEnd[4];
                    }
                } else {
                    chainRow[4] = r4;
                    chainPos[4] = p4;
                    if (finishSegment(4, tt, n)) {
                        r4 = chainRow[4];
                        p4 = chainPos[4];
                        e4 = chainEnd[4];
                    } else {
                        parked |= 1 << 4;
                    }
                }
            }
            if ((parked & 1 << 5) == 0) {
                final int v = tt[r5];
                if (v >= 0) {
                    tt[r5] = v | VISITED;
                    pool[p5] = (byte) v;
                    r5 = v >>> 8 & ROW_MASK;
                    if (++p5 == e5) {
                        chainPos[5] = p5;
                        newRegion(5);
                        p5 = chainPos[5];
                        e5 = chainEnd[5];
                    }
                } else {
                    chainRow[5] = r5;
                    chainPos[5] = p5;
                    if (finishSegment(5, tt, n)) {
                        r5 = chainRow[5];
                        p5 = chainPos[5];
                        e5 = chainEnd[5];
                    } else {
                        parked |= 1 << 5;
                    }
                }
            }
            if ((parked & 1 << 6) == 0) {
                final int v = tt[r6];
                if (v >= 0) {
                    tt[r6] = v | VISITED;
                    pool[p6] = (byte) v;
                    r6 = v >>> 8 & ROW_MASK;
                    if (++p6 == e6) {
                        chainPos[6] = p6;
                        newRegion(6);
                        p6 = chainPos[6];
                        e6 = chainEnd[6];
                    }
                } else {
                    chainRow[6] = r6;
                    chainPos[6] = p6;
                    if (finishSegment(6, tt, n)) {
                        r6 = chainRow[6];
                        p6 = chainPos[6];
                        e6 = chainEnd[6];
                    } else {
                        parked |= 1 << 6;
                    }
                }
            }
            if ((parked & 1 << 7) == 0) {
                final int v = tt[r7];
                if (v >= 0) {
                    tt[r7] = v | VISITED;
                    pool[p7] = (byte) v;
                    r7 = v >>> 8 & ROW_MASK;
                    if (++p7 == e7) {
                        chainPos[7] = p7;
                        newRegion(7);
                        p7 = chainPos[7];
                        e7 = chainEnd[7];
                    }
                } else {
                    chainRow[7] = r7;
                    chainPos[7] = p7;
                    if (finishSegment(7, tt, n)) {
                        r7 = chainRow[7];
                        p7 = chainPos[7];
                        e7 = chainEnd[7];
                    } else {
                        parked |= 1 << 7;
                    }
                }
            }
        }
        // Stitch the segments in cycle order, starting with the segment holding the first byte.
        int out = 0;
        int s = 0;
        do {
            for (int r = segmentFirstRegion[s]; r >= 0; r = regionNext[r]) {
                final int len = regionLength[r];
                if (out + len > n) {
                    throw new CompressorException("Stream corrupted");
                }
                System.arraycopy(pool, r * REGION, raw, out, len);
                out += len;
            }
            s = segmentNext[s];
        } while (s > 0);
        if (s < 0 || out == 0) {
            throw new CompressorException("Stream corrupted");
        }
        // A permutation with several cycles (corrupt input): the byte-at-a-time traversal keeps going round the first cycle.
        for (int i = out; i < n; i++) {
            raw[i] = raw[i - out];
        }
        raw[n] = raw[n % out];
    }
}
