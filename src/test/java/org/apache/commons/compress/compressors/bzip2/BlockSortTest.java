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

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class BlockSortTest {

    private static final byte[] FIXTURE = { 0, 1, (byte) 252, (byte) 253, (byte) 255,
                                            (byte) 254, 3, 2, (byte) 128 };

    /*
      Burrows-Wheeler transform of fixture the manual way:

      * build the matrix

      0, 1, 252, 253, 255, 254, 3, 2, 128
      1, 252, 253, 255, 254, 3, 2, 128, 0
      252, 253, 255, 254, 3, 2, 128, 0, 1
      253, 255, 254, 3, 2, 128, 0, 1, 252
      255, 254, 3, 2, 128, 0, 1, 252, 253
      254, 3, 2, 128, 0, 1, 252, 253, 255
      3, 2, 128, 0, 1, 252, 253, 255, 254
      2, 128, 0, 1, 252, 253, 255, 254, 3
      128, 0, 1, 252, 253, 255, 254, 3, 2

      * sort it

      0, 1, 252, 253, 255, 254, 3, 2, 128
      1, 252, 253, 255, 254, 3, 2, 128, 0
      2, 128, 0, 1, 252, 253, 255, 254, 3
      3, 2, 128, 0, 1, 252, 253, 255, 254
      128, 0, 1, 252, 253, 255, 254, 3, 2
      252, 253, 255, 254, 3, 2, 128, 0, 1
      253, 255, 254, 3, 2, 128, 0, 1, 252
      254, 3, 2, 128, 0, 1, 252, 253, 255
      255, 254, 3, 2, 128, 0, 1, 252, 253

      * grab last column

      128, 0, 3, 254, 2, 1, 252, 255, 253

        and the original line has been 0
    */

    private static final byte[] FIXTURE_BWT = { (byte) 128, 0, 3, (byte) 254, 2, 1, 
                                                (byte) 252, (byte) 255, (byte) 253 };

    private static final int[] FIXTURE_SORTED = {
        0, 1, 7, 6, 8, 2, 3, 5, 4
    };

    @Test
    public void testSortFixture() {
        BZip2CompressorOutputStream.Data data = new BZip2CompressorOutputStream.Data(1);
        System.arraycopy(FIXTURE, 0, data.block, 1, FIXTURE.length);
        BlockSort s = new BlockSort(data);
        assertFalse(s.blockSort(data, FIXTURE.length - 1));
        assertEquals(FIXTURE[FIXTURE.length - 1], data.block[0]);
        for (int i = 0; i < FIXTURE.length; i++) {
            assertEquals(FIXTURE_BWT[i], data.block[data.fmap[i]]);
        }
        assertEquals(0, data.origPtr);
    }

    @Test
    public void testFallbackSort() {
        BZip2CompressorOutputStream.Data data = new BZip2CompressorOutputStream.Data(1);
        BlockSort s = new BlockSort(data);
        int[] fmap = new int[FIXTURE.length];
        s.fallbackSort(fmap, FIXTURE, FIXTURE.length);
        assertArrayEquals(FIXTURE_SORTED, fmap);
    }
}