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
package org.apache.commons.compress.compressors.xz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class XZUtilsTest {

    @Test
    void testCachingIsEnabledByDefaultAndXZIsPresent() {
        assertEquals(XZUtils.CachedAvailability.CACHED_AVAILABLE, XZUtils.getCachedXZAvailability());
        assertTrue(XZUtils.isXZCompressionAvailable());
    }

    @Test
    void testCanTurnOffCaching() {
        try {
            XZUtils.setCacheXZAvailablity(false);
            assertEquals(XZUtils.CachedAvailability.DONT_CACHE, XZUtils.getCachedXZAvailability());
            assertTrue(XZUtils.isXZCompressionAvailable());
        } finally {
            XZUtils.setCacheXZAvailablity(true);
        }
    }

    @SuppressWarnings("deprecation")
    @Test
    void testGetCompressedFilename() {
        assertEquals(".xz", XZUtils.getCompressedFilename(""));
        assertEquals(".xz", XZUtils.getCompressedFileName(""));
        assertEquals("x.xz", XZUtils.getCompressedFilename("x"));
        assertEquals("x.xz", XZUtils.getCompressedFileName("x"));

        assertEquals("x.txz", XZUtils.getCompressedFilename("x.tar"));
        assertEquals("x.txz", XZUtils.getCompressedFileName("x.tar"));

        assertEquals("x.wmf .xz", XZUtils.getCompressedFilename("x.wmf "));
        assertEquals("x.wmf .xz", XZUtils.getCompressedFileName("x.wmf "));
        assertEquals("x.wmf\n.xz", XZUtils.getCompressedFilename("x.wmf\n"));
        assertEquals("x.wmf\n.xz", XZUtils.getCompressedFileName("x.wmf\n"));
        assertEquals("x.wmf.y.xz", XZUtils.getCompressedFilename("x.wmf.y"));
        assertEquals("x.wmf.y.xz", XZUtils.getCompressedFileName("x.wmf.y"));
    }

    @SuppressWarnings("deprecation")
    @Test
    void testGetUncompressedFilename() {
        assertEquals("", XZUtils.getUncompressedFilename(""));
        assertEquals("", XZUtils.getUncompressedFileName(""));
        assertEquals(".xz", XZUtils.getUncompressedFilename(".xz"));

        assertEquals("x.tar", XZUtils.getUncompressedFilename("x.txz"));
        assertEquals("x.tar", XZUtils.getUncompressedFileName("x.txz"));
        assertEquals("x", XZUtils.getUncompressedFilename("x.xz"));
        assertEquals("x", XZUtils.getUncompressedFileName("x.xz"));
        assertEquals("x", XZUtils.getUncompressedFilename("x-xz"));
        assertEquals("x", XZUtils.getUncompressedFileName("x-xz"));

        assertEquals("x.txz ", XZUtils.getUncompressedFilename("x.txz "));
        assertEquals("x.txz ", XZUtils.getUncompressedFileName("x.txz "));
        assertEquals("x.txz\n", XZUtils.getUncompressedFilename("x.txz\n"));
        assertEquals("x.txz\n", XZUtils.getUncompressedFileName("x.txz\n"));
        assertEquals("x.txz.y", XZUtils.getUncompressedFilename("x.txz.y"));
        assertEquals("x.txz.y", XZUtils.getUncompressedFileName("x.txz.y"));
    }

    @SuppressWarnings("deprecation")
    @Test
    void testIsCompressedFilename() {
        assertFalse(XZUtils.isCompressedFilename(""));
        assertFalse(XZUtils.isCompressedFileName(""));
        assertFalse(XZUtils.isCompressedFilename(".xz"));
        assertFalse(XZUtils.isCompressedFileName(".xz"));

        assertTrue(XZUtils.isCompressedFilename("x.txz"));
        assertTrue(XZUtils.isCompressedFileName("x.txz"));
        assertTrue(XZUtils.isCompressedFilename("x.xz"));
        assertTrue(XZUtils.isCompressedFileName("x.xz"));
        assertTrue(XZUtils.isCompressedFilename("x-xz"));
        assertTrue(XZUtils.isCompressedFileName("x-xz"));

        assertFalse(XZUtils.isCompressedFilename("xxgz"));
        assertFalse(XZUtils.isCompressedFileName("xxgz"));
        assertFalse(XZUtils.isCompressedFilename("xzz"));
        assertFalse(XZUtils.isCompressedFileName("xzz"));
        assertFalse(XZUtils.isCompressedFilename("xaz"));
        assertFalse(XZUtils.isCompressedFileName("xaz"));

        assertFalse(XZUtils.isCompressedFilename("x.txz "));
        assertFalse(XZUtils.isCompressedFileName("x.txz "));
        assertFalse(XZUtils.isCompressedFilename("x.txz\n"));
        assertFalse(XZUtils.isCompressedFileName("x.txz\n"));
        assertFalse(XZUtils.isCompressedFilename("x.txz.y"));
        assertFalse(XZUtils.isCompressedFileName("x.txz.y"));
    }

    @Test
    void testMatches() {
        final byte[] data = { (byte) 0xFD, '7', 'z', 'X', 'Z', '\0' };
        assertFalse(XZUtils.matches(data, 5));
        assertTrue(XZUtils.matches(data, 6));
        assertTrue(XZUtils.matches(data, 7));
        data[5] = '0';
        assertFalse(XZUtils.matches(data, 6));
    }

    @Test
    void testTurningOnCachingReEvaluatesAvailability() {
        try {
            XZUtils.setCacheXZAvailablity(false);
            assertEquals(XZUtils.CachedAvailability.DONT_CACHE, XZUtils.getCachedXZAvailability());
            XZUtils.setCacheXZAvailablity(true);
            assertEquals(XZUtils.CachedAvailability.CACHED_AVAILABLE, XZUtils.getCachedXZAvailability());
        } finally {
            XZUtils.setCacheXZAvailablity(true);
        }
    }

}
