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
package org.apache.commons.compress.compressors.bzip2;

/**
 * Encapsulates the sorting algorithms needed by {@link BZip2CompressorOutputStream}.
 *
 * @NotThreadSafe
 */
class BlockSort {

    private static final int SETMASK = (1 << 21);
    private static final int CLEARMASK = (~SETMASK);
    private static final int SMALL_THRESH = 20;
    private static final int DEPTH_THRESH = 10;
    private static final int WORK_FACTOR = 30;

    /*
     * <p> If you are ever unlucky/improbable enough to get a stack
     * overflow whilst sorting, increase the following constant and
     * try again. In practice I have never seen the stack go above 27
     * elems, so the following limit seems very generous.  </p>
     */
    static final int QSORT_STACK_SIZE = 1000;

    /**
     * Knuth's increments seem to work better than Incerpi-Sedgewick here.
     * Possibly because the number of elems to sort is usually small, typically
     * &lt;= 20.
     */
    private static final int[] INCS = { 1, 4, 13, 40, 121, 364, 1093, 3280,
                                        9841, 29524, 88573, 265720, 797161,
                                        2391484 };

    private boolean blockRandomised;

    /*
     * Used when sorting. If too many long comparisons happen, we stop sorting,
     * randomise the block slightly, and try again.
     */
    private int workDone;
    private int workLimit;
    private boolean firstAttempt;

    /**
     * This is the most hammered method of this class.
     *
     * <p>
     * This is the version using unrolled loops. Normally I never use such ones
     * in Java code. The unrolling has shown a noticable performance improvement
     * on JRE 1.4.2 (Linux i586 / HotSpot Client). Of course it depends on the
     * JIT compiler of the vm.
     * </p>
     */
    private boolean mainSimpleSort(final BZip2CompressorOutputStream.Data dataShadow,
                                   final int lo, final int hi, final int d,
                                   final int lastShadow) {
        final int bigN = hi - lo + 1;
        if (bigN < 2) {
            return this.firstAttempt && (this.workDone > this.workLimit);
        }

        int hp = 0;
        while (INCS[hp] < bigN) {
            hp++;
        }

        final int[] fmap = dataShadow.fmap;
        final char[] quadrant = dataShadow.quadrant;
        final byte[] block = dataShadow.block;
        final int lastPlus1 = lastShadow + 1;
        final boolean firstAttemptShadow = this.firstAttempt;
        final int workLimitShadow = this.workLimit;
        int workDoneShadow = this.workDone;

        // Following block contains unrolled code which could be shortened by
        // coding it in additional loops.

        HP: while (--hp >= 0) {
            final int h = INCS[hp];
            final int mj = lo + h - 1;

            for (int i = lo + h; i <= hi;) {
                // copy
                for (int k = 3; (i <= hi) && (--k >= 0); i++) {
                    final int v = fmap[i];
                    final int vd = v + d;
                    int j = i;

                    // for (int a;
                    // (j > mj) && mainGtU((a = fmap[j - h]) + d, vd,
                    // block, quadrant, lastShadow);
                    // j -= h) {
                    // fmap[j] = a;
                    // }
                    //
                    // unrolled version:

                    // start inline mainGTU
                    boolean onceRunned = false;
                    int a = 0;

                    HAMMER: while (true) {
                        if (onceRunned) {
                            fmap[j] = a;
                            if ((j -= h) <= mj) {
                                break HAMMER;
                            }
                        } else {
                            onceRunned = true;
                        }

                        a = fmap[j - h];
                        int i1 = a + d;
                        int i2 = vd;

                        // following could be done in a loop, but
                        // unrolled it for performance:
                        if (block[i1 + 1] == block[i2 + 1]) {
                            if (block[i1 + 2] == block[i2 + 2]) {
                                if (block[i1 + 3] == block[i2 + 3]) {
                                    if (block[i1 + 4] == block[i2 + 4]) {
                                        if (block[i1 + 5] == block[i2 + 5]) {
                                            if (block[(i1 += 6)] == block[(i2 += 6)]) {
                                                int x = lastShadow;
                                                X: while (x > 0) {
                                                    x -= 4;

                                                    if (block[i1 + 1] == block[i2 + 1]) {
                                                        if (quadrant[i1] == quadrant[i2]) {
                                                            if (block[i1 + 2] == block[i2 + 2]) {
                                                                if (quadrant[i1 + 1] == quadrant[i2 + 1]) {
                                                                    if (block[i1 + 3] == block[i2 + 3]) {
                                                                        if (quadrant[i1 + 2] == quadrant[i2 + 2]) {
                                                                            if (block[i1 + 4] == block[i2 + 4]) {
                                                                                if (quadrant[i1 + 3] == quadrant[i2 + 3]) {
                                                                                    if ((i1 += 4) >= lastPlus1) {
                                                                                        i1 -= lastPlus1;
                                                                                    }
                                                                                    if ((i2 += 4) >= lastPlus1) {
                                                                                        i2 -= lastPlus1;
                                                                                    }
                                                                                    workDoneShadow++;
                                                                                    continue X;
                                                                                } else if ((quadrant[i1 + 3] > quadrant[i2 + 3])) {
                                                                                    continue HAMMER;
                                                                                } else {
                                                                                    break HAMMER;
                                                                                }
                                                                            } else if ((block[i1 + 4] & 0xff) > (block[i2 + 4] & 0xff)) {
                                                                                continue HAMMER;
                                                                            } else {
                                                                                break HAMMER;
                                                                            }
                                                                        } else if ((quadrant[i1 + 2] > quadrant[i2 + 2])) {
                                                                            continue HAMMER;
                                                                        } else {
                                                                            break HAMMER;
                                                                        }
                                                                    } else if ((block[i1 + 3] & 0xff) > (block[i2 + 3] & 0xff)) {
                                                                        continue HAMMER;
                                                                    } else {
                                                                        break HAMMER;
                                                                    }
                                                                } else if ((quadrant[i1 + 1] > quadrant[i2 + 1])) {
                                                                    continue HAMMER;
                                                                } else {
                                                                    break HAMMER;
                                                                }
                                                            } else if ((block[i1 + 2] & 0xff) > (block[i2 + 2] & 0xff)) {
                                                                continue HAMMER;
                                                            } else {
                                                                break HAMMER;
                                                            }
                                                        } else if ((quadrant[i1] > quadrant[i2])) {
                                                            continue HAMMER;
                                                        } else {
                                                            break HAMMER;
                                                        }
                                                    } else if ((block[i1 + 1] & 0xff) > (block[i2 + 1] & 0xff)) {
                                                        continue HAMMER;
                                                    } else {
                                                        break HAMMER;
                                                    }

                                                }
                                                break HAMMER;
                                            } // while x > 0
                                            else {
                                                if ((block[i1] & 0xff) > (block[i2] & 0xff)) {
                                                    continue HAMMER;
                                                } else {
                                                    break HAMMER;
                                                }
                                            }
                                        } else if ((block[i1 + 5] & 0xff) > (block[i2 + 5] & 0xff)) {
                                            continue HAMMER;
                                        } else {
                                            break HAMMER;
                                        }
                                    } else if ((block[i1 + 4] & 0xff) > (block[i2 + 4] & 0xff)) {
                                        continue HAMMER;
                                    } else {
                                        break HAMMER;
                                    }
                                } else if ((block[i1 + 3] & 0xff) > (block[i2 + 3] & 0xff)) {
                                    continue HAMMER;
                                } else {
                                    break HAMMER;
                                }
                            } else if ((block[i1 + 2] & 0xff) > (block[i2 + 2] & 0xff)) {
                                continue HAMMER;
                            } else {
                                break HAMMER;
                            }
                        } else if ((block[i1 + 1] & 0xff) > (block[i2 + 1] & 0xff)) {
                            continue HAMMER;
                        } else {
                            break HAMMER;
                        }

                    } // HAMMER
                    // end inline mainGTU

                    fmap[j] = v;
                }

                if (firstAttemptShadow && (i <= hi)
                    && (workDoneShadow > workLimitShadow)) {
                    break HP;
                }
            }
        }

        this.workDone = workDoneShadow;
        return firstAttemptShadow && (workDoneShadow > workLimitShadow);
    }

    private static void vswap(int[] fmap, int p1, int p2, int n) {
        n += p1;
        while (p1 < n) {
            int t = fmap[p1];
            fmap[p1++] = fmap[p2];
            fmap[p2++] = t;
        }
    }

    private static byte med3(byte a, byte b, byte c) {
        return (a < b) ? (b < c ? b : a < c ? c : a) : (b > c ? b : a > c ? c
                                                        : a);
    }

    boolean blockSort(final BZip2CompressorOutputStream.Data data, final int last) {
        this.workLimit = WORK_FACTOR * last;
        this.workDone = 0;
        this.blockRandomised = false;
        this.firstAttempt = true;
        mainSort(data, last);

        if (this.firstAttempt && (this.workDone > this.workLimit)) {
            randomiseBlock(data, last);
            this.workLimit = this.workDone = 0;
            this.firstAttempt = false;
            mainSort(data, last);
        }

        int[] fmap = data.fmap;
        data.origPtr = -1;
        for (int i = 0; i <= last; i++) {
            if (fmap[i] == 0) {
                data.origPtr = i;
                break;
            }
        }

        // assert (data.origPtr != -1) : data.origPtr;
        return blockRandomised;
    }

    /**
     * Method "mainQSort3", file "blocksort.c", BZip2 1.0.2
     */
    private void mainQSort3(final BZip2CompressorOutputStream.Data dataShadow,
                            final int loSt, final int hiSt, final int dSt,
                            final int last) {
        final int[] stack_ll = dataShadow.stack_ll;
        final int[] stack_hh = dataShadow.stack_hh;
        final int[] stack_dd = dataShadow.stack_dd;
        final int[] fmap = dataShadow.fmap;
        final byte[] block = dataShadow.block;

        stack_ll[0] = loSt;
        stack_hh[0] = hiSt;
        stack_dd[0] = dSt;

        for (int sp = 1; --sp >= 0;) {
            final int lo = stack_ll[sp];
            final int hi = stack_hh[sp];
            final int d = stack_dd[sp];

            if ((hi - lo < SMALL_THRESH) || (d > DEPTH_THRESH)) {
                if (mainSimpleSort(dataShadow, lo, hi, d, last)) {
                    return;
                }
            } else {
                final int d1 = d + 1;
                final int med = med3(block[fmap[lo] + d1],
                                     block[fmap[hi] + d1], block[fmap[(lo + hi) >>> 1] + d1]) & 0xff;

                int unLo = lo;
                int unHi = hi;
                int ltLo = lo;
                int gtHi = hi;

                while (true) {
                    while (unLo <= unHi) {
                        final int n = (block[fmap[unLo] + d1] & 0xff)
                            - med;
                        if (n == 0) {
                            final int temp = fmap[unLo];
                            fmap[unLo++] = fmap[ltLo];
                            fmap[ltLo++] = temp;
                        } else if (n < 0) {
                            unLo++;
                        } else {
                            break;
                        }
                    }

                    while (unLo <= unHi) {
                        final int n = (block[fmap[unHi] + d1] & 0xff)
                            - med;
                        if (n == 0) {
                            final int temp = fmap[unHi];
                            fmap[unHi--] = fmap[gtHi];
                            fmap[gtHi--] = temp;
                        } else if (n > 0) {
                            unHi--;
                        } else {
                            break;
                        }
                    }

                    if (unLo <= unHi) {
                        final int temp = fmap[unLo];
                        fmap[unLo++] = fmap[unHi];
                        fmap[unHi--] = temp;
                    } else {
                        break;
                    }
                }

                if (gtHi < ltLo) {
                    stack_ll[sp] = lo;
                    stack_hh[sp] = hi;
                    stack_dd[sp] = d1;
                    sp++;
                } else {
                    int n = ((ltLo - lo) < (unLo - ltLo)) ? (ltLo - lo)
                        : (unLo - ltLo);
                    vswap(fmap, lo, unLo - n, n);
                    int m = ((hi - gtHi) < (gtHi - unHi)) ? (hi - gtHi)
                        : (gtHi - unHi);
                    vswap(fmap, unLo, hi - m + 1, m);

                    n = lo + unLo - ltLo - 1;
                    m = hi - (gtHi - unHi) + 1;

                    stack_ll[sp] = lo;
                    stack_hh[sp] = n;
                    stack_dd[sp] = d;
                    sp++;

                    stack_ll[sp] = n + 1;
                    stack_hh[sp] = m - 1;
                    stack_dd[sp] = d1;
                    sp++;

                    stack_ll[sp] = m;
                    stack_hh[sp] = hi;
                    stack_dd[sp] = d;
                    sp++;
                }
            }
        }
    }

    private void mainSort(final BZip2CompressorOutputStream.Data dataShadow,
                          final int lastShadow) {
        final int[] runningOrder = dataShadow.mainSort_runningOrder;
        final int[] copy = dataShadow.mainSort_copy;
        final boolean[] bigDone = dataShadow.mainSort_bigDone;
        final int[] ftab = dataShadow.ftab;
        final byte[] block = dataShadow.block;
        final int[] fmap = dataShadow.fmap;
        final char[] quadrant = dataShadow.quadrant;
        final int workLimitShadow = this.workLimit;
        final boolean firstAttemptShadow = this.firstAttempt;

        // Set up the 2-byte frequency table
        for (int i = 65537; --i >= 0;) {
            ftab[i] = 0;
        }

        /*
         * In the various block-sized structures, live data runs from 0 to
         * last+NUM_OVERSHOOT_BYTES inclusive. First, set up the overshoot area
         * for block.
         */
        for (int i = 0; i < BZip2Constants.NUM_OVERSHOOT_BYTES; i++) {
            block[lastShadow + i + 2] = block[(i % (lastShadow + 1)) + 1];
        }
        for (int i = lastShadow + BZip2Constants.NUM_OVERSHOOT_BYTES +1; --i >= 0;) {
            quadrant[i] = 0;
        }
        block[0] = block[lastShadow + 1];

        // Complete the initial radix sort:

        int c1 = block[0] & 0xff;
        for (int i = 0; i <= lastShadow; i++) {
            final int c2 = block[i + 1] & 0xff;
            ftab[(c1 << 8) + c2]++;
            c1 = c2;
        }

        for (int i = 1; i <= 65536; i++) {
            ftab[i] += ftab[i - 1];
        }

        c1 = block[1] & 0xff;
        for (int i = 0; i < lastShadow; i++) {
            final int c2 = block[i + 2] & 0xff;
            fmap[--ftab[(c1 << 8) + c2]] = i;
            c1 = c2;
        }

        fmap[--ftab[((block[lastShadow + 1] & 0xff) << 8) + (block[1] & 0xff)]] = lastShadow;

        /*
         * Now ftab contains the first loc of every small bucket. Calculate the
         * running order, from smallest to largest big bucket.
         */
        for (int i = 256; --i >= 0;) {
            bigDone[i] = false;
            runningOrder[i] = i;
        }

        for (int h = 364; h != 1;) {
            h /= 3;
            for (int i = h; i <= 255; i++) {
                final int vv = runningOrder[i];
                final int a = ftab[(vv + 1) << 8] - ftab[vv << 8];
                final int b = h - 1;
                int j = i;
                for (int ro = runningOrder[j - h]; (ftab[(ro + 1) << 8] - ftab[ro << 8]) > a; ro = runningOrder[j
                                                                                                                - h]) {
                    runningOrder[j] = ro;
                    j -= h;
                    if (j <= b) {
                        break;
                    }
                }
                runningOrder[j] = vv;
            }
        }

        /*
         * The main sorting loop.
         */
        for (int i = 0; i <= 255; i++) {
            /*
             * Process big buckets, starting with the least full.
             */
            final int ss = runningOrder[i];

            // Step 1:
            /*
             * Complete the big bucket [ss] by quicksorting any unsorted small
             * buckets [ss, j]. Hopefully previous pointer-scanning phases have
             * already completed many of the small buckets [ss, j], so we don't
             * have to sort them at all.
             */
            for (int j = 0; j <= 255; j++) {
                final int sb = (ss << 8) + j;
                final int ftab_sb = ftab[sb];
                if ((ftab_sb & SETMASK) != SETMASK) {
                    final int lo = ftab_sb & CLEARMASK;
                    final int hi = (ftab[sb + 1] & CLEARMASK) - 1;
                    if (hi > lo) {
                        mainQSort3(dataShadow, lo, hi, 2, lastShadow);
                        if (firstAttemptShadow
                            && (this.workDone > workLimitShadow)) {
                            return;
                        }
                    }
                    ftab[sb] = ftab_sb | SETMASK;
                }
            }

            // Step 2:
            // Now scan this big bucket so as to synthesise the
            // sorted order for small buckets [t, ss] for all t != ss.

            for (int j = 0; j <= 255; j++) {
                copy[j] = ftab[(j << 8) + ss] & CLEARMASK;
            }

            for (int j = ftab[ss << 8] & CLEARMASK, hj = (ftab[(ss + 1) << 8] & CLEARMASK); j < hj; j++) {
                final int fmap_j = fmap[j];
                c1 = block[fmap_j] & 0xff;
                if (!bigDone[c1]) {
                    fmap[copy[c1]] = (fmap_j == 0) ? lastShadow : (fmap_j - 1);
                    copy[c1]++;
                }
            }

            for (int j = 256; --j >= 0;) {
                ftab[(j << 8) + ss] |= SETMASK;
            }

            // Step 3:
            /*
             * The ss big bucket is now done. Record this fact, and update the
             * quadrant descriptors. Remember to update quadrants in the
             * overshoot area too, if necessary. The "if (i < 255)" test merely
             * skips this updating for the last bucket processed, since updating
             * for the last bucket is pointless.
             */
            bigDone[ss] = true;

            if (i < 255) {
                final int bbStart = ftab[ss << 8] & CLEARMASK;
                final int bbSize = (ftab[(ss + 1) << 8] & CLEARMASK) - bbStart;
                int shifts = 0;

                while ((bbSize >> shifts) > 65534) {
                    shifts++;
                }

                for (int j = 0; j < bbSize; j++) {
                    final int a2update = fmap[bbStart + j];
                    final char qVal = (char) (j >> shifts);
                    quadrant[a2update] = qVal;
                    if (a2update < BZip2Constants.NUM_OVERSHOOT_BYTES) {
                        quadrant[a2update + lastShadow + 1] = qVal;
                    }
                }
            }

        }
    }

    private void randomiseBlock(final BZip2CompressorOutputStream.Data data,
                                final int lastShadow) {
        final boolean[] inUse = data.inUse;
        final byte[] block = data.block;

        for (int i = 256; --i >= 0;) {
            inUse[i] = false;
        }

        int rNToGo = 0;
        int rTPos = 0;
        for (int i = 0, j = 1; i <= lastShadow; i = j, j++) {
            if (rNToGo == 0) {
                rNToGo = (char) Rand.rNums(rTPos);
                if (++rTPos == 512) {
                    rTPos = 0;
                }
            }

            rNToGo--;
            block[j] ^= ((rNToGo == 1) ? 1 : 0);

            // handle 16 bit signed numbers
            inUse[block[j] & 0xff] = true;
        }

        this.blockRandomised = true;
    }

}
