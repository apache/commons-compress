/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.commons.compress.harmony.unpack200.tests;

import java.util.List;

import junit.framework.TestCase;

import org.apache.commons.compress.harmony.unpack200.SegmentConstantPoolArrayCache;

public class SegmentConstantPoolArrayCacheTest extends TestCase {

    public void testSingleSimpleArray() {
        SegmentConstantPoolArrayCache arrayCache = new SegmentConstantPoolArrayCache();
        String array[] = {"Zero", "One", "Two", "Three", "Four"};
        List list = arrayCache.indexesForArrayKey(array, "Three");
        assertEquals(1, list.size());
        assertEquals(3, ((Integer)list.get(0)).intValue());
    }

    public void testSingleMultipleHitArray() {
        SegmentConstantPoolArrayCache arrayCache = new SegmentConstantPoolArrayCache();
        String array[] = {"Zero", "OneThreeFour", "Two", "OneThreeFour", "OneThreeFour"};
        List list = arrayCache.indexesForArrayKey(array, "OneThreeFour");
        assertEquals(3, list.size());
        assertEquals(1, ((Integer)list.get(0)).intValue());
        assertEquals(3, ((Integer)list.get(1)).intValue());
        assertEquals(4, ((Integer)list.get(2)).intValue());
    }

    public void testMultipleArrayMultipleHit() {
        SegmentConstantPoolArrayCache arrayCache = new SegmentConstantPoolArrayCache();
        String arrayOne[] = {"Zero", "Shared", "Two", "Shared", "Shared"};
        String arrayTwo[] = {"Shared", "One", "Shared", "Shared", "Shared"};

        List listOne = arrayCache.indexesForArrayKey(arrayOne, "Shared");
        List listTwo = arrayCache.indexesForArrayKey(arrayTwo, "Shared");
        // Make sure we're using the cached values. First trip
        // through builds the cache.
        listOne = arrayCache.indexesForArrayKey(arrayOne, "Two");
        listTwo = arrayCache.indexesForArrayKey(arrayTwo, "Shared");

        assertEquals(1, listOne.size());
        assertEquals(2, ((Integer)listOne.get(0)).intValue());

        // Now look for a different element in list one
        listOne = arrayCache.indexesForArrayKey(arrayOne, "Shared");
        assertEquals(3, listOne.size());
        assertEquals(1, ((Integer)listOne.get(0)).intValue());
        assertEquals(3, ((Integer)listOne.get(1)).intValue());
        assertEquals(4, ((Integer)listOne.get(2)).intValue());

        assertEquals(4, listTwo.size());
        assertEquals(0, ((Integer)listTwo.get(0)).intValue());
        assertEquals(2, ((Integer)listTwo.get(1)).intValue());
        assertEquals(3, ((Integer)listTwo.get(2)).intValue());
        assertEquals(4, ((Integer)listTwo.get(3)).intValue());

        List listThree = arrayCache.indexesForArrayKey(arrayOne, "Not found");
        assertEquals(0, listThree.size());
    }

}
