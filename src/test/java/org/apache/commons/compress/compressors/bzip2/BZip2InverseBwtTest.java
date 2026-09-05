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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.io.IOException;
import java.util.Random;

import org.junit.jupiter.api.Test;

/**
 * Tests {@link BZip2InverseBwt} against the byte-at-a-time traversal of the successor permutation, including permutations with several cycles (which
 * only corrupt input produces).
 */
class BZip2InverseBwtTest {

    /**
     * Builds a {@code tt} array for a random block of {@code n} bytes: either a single cycle (like every valid block) or a random permutation.
     */
    private static int[] permutation(final Random rnd, final int n, final boolean singleCycle) {
        final int[] order = new int[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        for (int i = n - 1; i > 0; i--) {
            final int j = rnd.nextInt(i + 1);
            final int t = order[i];
            order[i] = order[j];
            order[j] = t;
        }
        final int[] tt = new int[n];
        if (singleCycle) {
            for (int i = 0; i < n; i++) {
                tt[order[i]] = order[(i + 1) % n] << 8 | rnd.nextInt(256);
            }
        } else {
            for (int i = 0; i < n; i++) {
                tt[i] = order[i] << 8 | rnd.nextInt(256);
            }
        }
        return tt;
    }

    private static byte[] chase(final int[] tt, final int origPtr, final int n) {
        final byte[] expected = new byte[n + 1];
        int row = tt[origPtr] >>> 8;
        for (int i = 0; i <= n; i++) {
            final int v = tt[row];
            expected[i] = (byte) v;
            row = v >>> 8;
        }
        return expected;
    }

    private static void check(final Random rnd, final int n, final boolean singleCycle) throws IOException {
        final int[] tt = permutation(rnd, n, singleCycle);
        final int origPtr = rnd.nextInt(n);
        final byte[] expected = chase(tt, origPtr, n);
        final byte[] actual = new byte[n + 1];
        new BZip2InverseBwt(n).unwind(tt.clone(), origPtr, n, actual);
        assertArrayEquals(expected, actual, "n=" + n + " singleCycle=" + singleCycle);
    }

    @Test
    void testMultipleCycles() throws IOException {
        final Random rnd = new Random(2);
        for (final int n : new int[] { 1, 2, 3, 5, 7, 8, 9, 64, 1000, 5000, 70_000 }) {
            for (int i = 0; i < 5; i++) {
                check(rnd, n, false);
            }
        }
    }

    @Test
    void testReusedAcrossBlocks() throws IOException {
        final Random rnd = new Random(3);
        final BZip2InverseBwt inverseBwt = new BZip2InverseBwt(100_000);
        for (int i = 0; i < 20; i++) {
            final int n = 1 + rnd.nextInt(100_000);
            final int[] tt = permutation(rnd, n, i % 3 != 0);
            final int origPtr = rnd.nextInt(n);
            final byte[] expected = chase(tt, origPtr, n);
            final byte[] actual = new byte[n + 1];
            inverseBwt.unwind(tt, origPtr, n, actual);
            assertArrayEquals(expected, actual, "block " + i + " n=" + n);
        }
    }

    @Test
    void testSingleCycle() throws IOException {
        final Random rnd = new Random(1);
        for (final int n : new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 15, 16, 17, 100, 2047, 2048, 2049, 20_000, 300_000, 900_000 }) {
            check(rnd, n, true);
        }
    }
}
