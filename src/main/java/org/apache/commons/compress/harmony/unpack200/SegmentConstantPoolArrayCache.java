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
package org.apache.commons.compress.harmony.unpack200;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * The SegmentConstantPool spends a lot of time searching through large arrays of Strings looking for matches. This can
 * be sped up by caching the arrays in HashMaps so the String keys are looked up and resolve to positions in the array
 * rather than iterating through the arrays each time.
 *
 * Because the arrays only grow (never shrink or change) we can use the last known size as a way to determine if the
 * array has changed.
 *
 * Note that this cache must be synchronized externally if it is shared.
 */
public class SegmentConstantPoolArrayCache {

    /**
     * CachedArray keeps track of the last known size of an array as well as a HashMap that knows the mapping from
     * element values to the indices of the array which contain that value.
     */
    protected class CachedArray {
        String[] primaryArray;
        int lastKnownSize;
        HashMap<String, List<Integer>> primaryTable;

        public CachedArray(final String[] array) {
            this.primaryArray = array;
            this.lastKnownSize = array.length;
            this.primaryTable = new HashMap<>(lastKnownSize);
            cacheIndexes();
        }

        /**
         * Given a primaryArray, cache its values in a HashMap to provide a backwards mapping from element values to
         * element indexes. For instance, a primaryArray of: {"Zero", "Foo", "Two", "Foo"} would yield a HashMap of:
         * "Zero" -&gt; 0 "Foo" -&gt; 1, 3 "Two" -&gt; 2 which is then cached.
         */
        protected void cacheIndexes() {
            for (int index = 0; index < primaryArray.length; index++) {
                final String key = primaryArray[index];
                primaryTable.computeIfAbsent(key, k -> new ArrayList<>());
                primaryTable.get(key).add(Integer.valueOf(index));
            }
        }

        /**
         * Given a particular key, answer a List of index locations in the array which contain that key.
         *
         * If no elements are found, answer an empty list.
         *
         * @param key String element of the array
         * @return List of indexes containing that key in the array.
         */
        public List<Integer> indexesForKey(final String key) {
            final List<Integer> list = primaryTable.get(key);
            return list != null ? list : Collections.emptyList();
        }

        /**
         * Answer the last known size of the array cached. If the last known size is not the same as the current size,
         * the array must have changed.
         *
         * @return int last known size of the cached array
         */
        public int lastKnownSize() {
            return lastKnownSize;
        }
    }

    protected IdentityHashMap<String[], CachedArray> knownArrays = new IdentityHashMap<>(1000);
    protected List<Integer> lastIndexes;
    protected String[] lastArray;

    protected String lastKey;

    /**
     * Given a String array, answer true if the array is correctly cached. Answer false if the array is not cached, or
     * if the array cache is outdated.
     *
     * @param array of String
     * @return boolean true if up-to-date cache, otherwise false.
     */
    protected boolean arrayIsCached(final String[] array) {
        final CachedArray cachedArray = knownArrays.get(array);
        return !(cachedArray == null || cachedArray.lastKnownSize() != array.length);
    }

    /**
     * Cache the array passed in as the argument
     *
     * @param array String[] to cache
     */
    protected void cacheArray(final String[] array) {
        if (arrayIsCached(array)) {
            throw new IllegalArgumentException("Trying to cache an array that already exists");
        }
        knownArrays.put(array, new CachedArray(array));
        // Invalidate the cache-within-a-cache
        lastArray = null;
    }

    /**
     * Answer the indices for the given key in the given array. If no such key exists in the cached array, answer -1.
     *
     * @param array String[] array to search for the value
     * @param key String value for which to search
     * @return List collection of index positions in the array
     */
    public List<Integer> indexesForArrayKey(final String[] array, final String key) {
        if (!arrayIsCached(array)) {
            cacheArray(array);
        }

        // If the search is one we've just done, don't even
        // bother looking and return the last indices. This
        // is a second cache within the cache. This is
        // efficient because we usually are looking for
        // several secondary elements with the same primary
        // key.
        if ((lastArray == array) && (lastKey == key)) {
            return lastIndexes;
        }

        // Remember the last thing we found.
        lastArray = array;
        lastKey = key;
        lastIndexes = knownArrays.get(array).indexesForKey(key);

        return lastIndexes;
    }
}
