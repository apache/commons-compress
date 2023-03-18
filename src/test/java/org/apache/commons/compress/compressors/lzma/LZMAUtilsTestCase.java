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
package org.apache.commons.compress.compressors.lzma;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class LZMAUtilsTestCase {

    @Test
    public void testCachingIsEnabledByDefaultAndLZMAIsPresent() {
        assertEquals(LZMAUtils.CachedAvailability.CACHED_AVAILABLE, LZMAUtils.getCachedLZMAAvailability());
        assertTrue(LZMAUtils.isLZMACompressionAvailable());
    }

    @Test
    public void testCanTurnOffCaching() {
        try {
            LZMAUtils.setCacheLZMAAvailablity(false);
            assertEquals(LZMAUtils.CachedAvailability.DONT_CACHE, LZMAUtils.getCachedLZMAAvailability());
            assertTrue(LZMAUtils.isLZMACompressionAvailable());
        } finally {
            LZMAUtils.setCacheLZMAAvailablity(true);
        }
    }

    @Test
    public void testGetCompressedFilename() {
        assertEquals(".lzma", LZMAUtils.getCompressedFilename(""));
        assertEquals("x.lzma", LZMAUtils.getCompressedFilename("x"));

        assertEquals("x.wmf .lzma", LZMAUtils.getCompressedFilename("x.wmf "));
        assertEquals("x.wmf\n.lzma", LZMAUtils.getCompressedFilename("x.wmf\n"));
        assertEquals("x.wmf.y.lzma", LZMAUtils.getCompressedFilename("x.wmf.y"));
    }

    @Test
    public void testGetUncompressedFilename() {
        assertEquals("", LZMAUtils.getUncompressedFilename(""));
        assertEquals(".lzma", LZMAUtils.getUncompressedFilename(".lzma"));

        assertEquals("x", LZMAUtils.getUncompressedFilename("x.lzma"));
        assertEquals("x", LZMAUtils.getUncompressedFilename("x-lzma"));

        assertEquals("x.lzma ", LZMAUtils.getUncompressedFilename("x.lzma "));
        assertEquals("x.lzma\n", LZMAUtils.getUncompressedFilename("x.lzma\n"));
        assertEquals("x.lzma.y", LZMAUtils.getUncompressedFilename("x.lzma.y"));
    }

    @Test
    public void testIsCompressedFilename() {
        assertFalse(LZMAUtils.isCompressedFilename(""));
        assertFalse(LZMAUtils.isCompressedFilename(".lzma"));

        assertTrue(LZMAUtils.isCompressedFilename("x.lzma"));
        assertTrue(LZMAUtils.isCompressedFilename("x-lzma"));

        assertFalse(LZMAUtils.isCompressedFilename("xxgz"));
        assertFalse(LZMAUtils.isCompressedFilename("lzmaz"));
        assertFalse(LZMAUtils.isCompressedFilename("xaz"));

        assertFalse(LZMAUtils.isCompressedFilename("x.lzma "));
        assertFalse(LZMAUtils.isCompressedFilename("x.lzma\n"));
        assertFalse(LZMAUtils.isCompressedFilename("x.lzma.y"));
    }

    @Test
    public void testMatches() {
        final byte[] data = {
            (byte) 0x5D, 0, 0,
        };
        assertFalse(LZMAUtils.matches(data, 2));
        assertTrue(LZMAUtils.matches(data, 3));
        assertTrue(LZMAUtils.matches(data, 4));
        data[2] = '0';
        assertFalse(LZMAUtils.matches(data, 3));
    }

    @Test
    public void testTurningOnCachingReEvaluatesAvailability() {
        try {
            LZMAUtils.setCacheLZMAAvailablity(false);
            assertEquals(LZMAUtils.CachedAvailability.DONT_CACHE, LZMAUtils.getCachedLZMAAvailability());
            LZMAUtils.setCacheLZMAAvailablity(true);
            assertEquals(LZMAUtils.CachedAvailability.CACHED_AVAILABLE, LZMAUtils.getCachedLZMAAvailability());
        } finally {
            LZMAUtils.setCacheLZMAAvailablity(true);
        }
    }

}
