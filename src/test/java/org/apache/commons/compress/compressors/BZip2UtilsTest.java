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
package org.apache.commons.compress.compressors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.commons.compress.compressors.bzip2.BZip2Utils;
import org.junit.jupiter.api.Test;

public class BZip2UtilsTest {

    @SuppressWarnings("deprecation")
    @Test
    public void testGetCompressedFilename() {
        assertEquals(".bz2", BZip2Utils.getCompressedFilename(""));
        assertEquals(".bz2", BZip2Utils.getCompressedFileName(""));
        assertEquals(" .bz2", BZip2Utils.getCompressedFilename(" "));
        assertEquals(" .bz2", BZip2Utils.getCompressedFileName(" "));
        assertEquals("x.bz2", BZip2Utils.getCompressedFilename("x"));
        assertEquals("x.bz2", BZip2Utils.getCompressedFileName("x"));
        assertEquals("X.bz2", BZip2Utils.getCompressedFilename("X"));
        assertEquals("X.bz2", BZip2Utils.getCompressedFileName("X"));
        assertEquals("x.tar.bz2", BZip2Utils.getCompressedFilename("x.tar"));
        assertEquals("x.tar.bz2", BZip2Utils.getCompressedFileName("x.tar"));
        assertEquals("x.tar.bz2", BZip2Utils.getCompressedFilename("x.TAR"));
        assertEquals("x.tar.bz2", BZip2Utils.getCompressedFileName("x.TAR"));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testGetUncompressedFilename() {
        assertEquals("", BZip2Utils.getUncompressedFilename(""));
        assertEquals("", BZip2Utils.getUncompressedFileName(""));
        assertEquals(".bz2", BZip2Utils.getUncompressedFilename(".bz2"));
        assertEquals(".bz2", BZip2Utils.getUncompressedFileName(".bz2"));

        assertEquals("x.tar", BZip2Utils.getUncompressedFilename("x.tbz2"));
        assertEquals("x.tar", BZip2Utils.getUncompressedFileName("x.tbz2"));
        assertEquals("x.tar", BZip2Utils.getUncompressedFilename("x.tbz"));
        assertEquals("x.tar", BZip2Utils.getUncompressedFileName("x.tbz"));
        assertEquals("x", BZip2Utils.getUncompressedFilename("x.bz2"));
        assertEquals("x", BZip2Utils.getUncompressedFileName("x.bz2"));
        assertEquals("x", BZip2Utils.getUncompressedFilename("x.bz"));
        assertEquals("x", BZip2Utils.getUncompressedFileName("x.bz"));

        assertEquals("x.tar", BZip2Utils.getUncompressedFilename("x.TBZ2"));
        assertEquals("x.tar", BZip2Utils.getUncompressedFileName("x.TBZ2"));
        assertEquals("X.tar", BZip2Utils.getUncompressedFilename("X.Tbz2"));
        assertEquals("X.tar", BZip2Utils.getUncompressedFileName("X.Tbz2"));
        assertEquals("X.tar", BZip2Utils.getUncompressedFilename("X.tbZ2"));
        assertEquals("X.tar", BZip2Utils.getUncompressedFileName("X.tbZ2"));

        assertEquals("x.bz ", BZip2Utils.getUncompressedFilename("x.bz "));
        assertEquals("x.bz ", BZip2Utils.getUncompressedFileName("x.bz "));
        assertEquals("x.tbz\n", BZip2Utils.getUncompressedFilename("x.tbz\n"));
        assertEquals("x.tbz\n", BZip2Utils.getUncompressedFileName("x.tbz\n"));
        assertEquals("x.tbz2.y", BZip2Utils.getUncompressedFilename("x.tbz2.y"));
        assertEquals("x.tbz2.y", BZip2Utils.getUncompressedFileName("x.tbz2.y"));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testIsCompressedFilename() {
        assertFalse(BZip2Utils.isCompressedFilename(""));
        assertFalse(BZip2Utils.isCompressedFileName(""));
        assertFalse(BZip2Utils.isCompressedFilename(".gz"));
        assertFalse(BZip2Utils.isCompressedFileName(".gz"));

        assertTrue(BZip2Utils.isCompressedFilename("x.tbz2"));
        assertTrue(BZip2Utils.isCompressedFileName("x.tbz2"));
        assertTrue(BZip2Utils.isCompressedFilename("x.tbz"));
        assertTrue(BZip2Utils.isCompressedFileName("x.tbz"));
        assertTrue(BZip2Utils.isCompressedFilename("x.bz2"));
        assertTrue(BZip2Utils.isCompressedFileName("x.bz2"));
        assertTrue(BZip2Utils.isCompressedFilename("x.bz"));
        assertTrue(BZip2Utils.isCompressedFileName("x.bz"));

        assertFalse(BZip2Utils.isCompressedFilename("xbz2"));
        assertFalse(BZip2Utils.isCompressedFilename("xbz2"));
        assertFalse(BZip2Utils.isCompressedFilename("xbz"));
        assertFalse(BZip2Utils.isCompressedFileName("xbz"));

        assertTrue(BZip2Utils.isCompressedFilename("x.TBZ2"));
        assertTrue(BZip2Utils.isCompressedFileName("x.TBZ2"));
        assertTrue(BZip2Utils.isCompressedFilename("x.Tbz2"));
        assertTrue(BZip2Utils.isCompressedFileName("x.Tbz2"));
        assertTrue(BZip2Utils.isCompressedFilename("x.tbZ2"));
        assertTrue(BZip2Utils.isCompressedFileName("x.tbZ2"));

        assertFalse(BZip2Utils.isCompressedFilename("x.bz "));
        assertFalse(BZip2Utils.isCompressedFileName("x.bz "));
        assertFalse(BZip2Utils.isCompressedFilename("x.tbz\n"));
        assertFalse(BZip2Utils.isCompressedFileName("x.tbz\n"));
        assertFalse(BZip2Utils.isCompressedFilename("x.tbz2.y"));
        assertFalse(BZip2Utils.isCompressedFileName("x.tbz2.y"));
    }

}
