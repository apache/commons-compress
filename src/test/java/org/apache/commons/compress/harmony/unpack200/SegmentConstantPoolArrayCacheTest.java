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

package org.apache.commons.compress.harmony.unpack200;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class SegmentConstantPoolArrayCacheTest {

    @Test
    void testMultipleArrayMultipleHit() {
        final SegmentConstantPoolArrayCache arrayCache = new SegmentConstantPoolArrayCache();
        final String[] arrayOne = { "Zero", "Shared", "Two", "Shared", "Shared" };
        final String[] arrayTwo = { "Shared", "One", "Shared", "Shared", "Shared" };

        List<Integer> listOne = arrayCache.indexesForArrayKey(arrayOne, "Shared");
        List<Integer> listTwo = arrayCache.indexesForArrayKey(arrayTwo, "Shared");
        // Make sure we're using the cached values. First trip
        // through builds the cache.
        listOne = arrayCache.indexesForArrayKey(arrayOne, "Two");
        listTwo = arrayCache.indexesForArrayKey(arrayTwo, "Shared");

        assertEquals(1, listOne.size());
        assertEquals(2, listOne.get(0).intValue());

        // Now look for a different element in list one
        listOne = arrayCache.indexesForArrayKey(arrayOne, "Shared");
        assertEquals(3, listOne.size());
        assertEquals(1, listOne.get(0).intValue());
        assertEquals(3, listOne.get(1).intValue());
        assertEquals(4, listOne.get(2).intValue());

        assertEquals(4, listTwo.size());
        assertEquals(0, listTwo.get(0).intValue());
        assertEquals(2, listTwo.get(1).intValue());
        assertEquals(3, listTwo.get(2).intValue());
        assertEquals(4, listTwo.get(3).intValue());

        final List<Integer> listThree = arrayCache.indexesForArrayKey(arrayOne, "Not found");
        assertEquals(0, listThree.size());
    }

    @Test
    void testSingleMultipleHitArray() {
        final SegmentConstantPoolArrayCache arrayCache = new SegmentConstantPoolArrayCache();
        final String[] array = { "Zero", "OneThreeFour", "Two", "OneThreeFour", "OneThreeFour" };
        final List<Integer> list = arrayCache.indexesForArrayKey(array, "OneThreeFour");
        assertEquals(3, list.size());
        assertEquals(1, list.get(0).intValue());
        assertEquals(3, list.get(1).intValue());
        assertEquals(4, list.get(2).intValue());
    }

    @Test
    void testSingleSimpleArray() {
        final SegmentConstantPoolArrayCache arrayCache = new SegmentConstantPoolArrayCache();
        final String[] array = { "Zero", "One", "Two", "Three", "Four" };
        final List<Integer> list = arrayCache.indexesForArrayKey(array, "Three");
        assertEquals(1, list.size());
        assertEquals(3, list.get(0).intValue());
    }

}
