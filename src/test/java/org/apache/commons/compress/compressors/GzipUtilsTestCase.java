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
package org.apache.commons.compress.compressors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.commons.compress.compressors.gzip.GzipUtils;
import org.junit.jupiter.api.Test;

public class GzipUtilsTestCase {

    @SuppressWarnings("deprecation")
    @Test
    public void testGetCompressedFilename() {
        assertEquals(".gz", GzipUtils.getCompressedFilename(""));
        assertEquals(".gz", GzipUtils.getCompressedFileName(""));
        assertEquals("x.gz", GzipUtils.getCompressedFilename("x"));
        assertEquals("x.gz", GzipUtils.getCompressedFileName("x"));

        assertEquals("x.tgz", GzipUtils.getCompressedFilename("x.tar"));
        assertEquals("x.tgz", GzipUtils.getCompressedFileName("x.tar"));
        assertEquals("x.svgz", GzipUtils.getCompressedFilename("x.svg"));
        assertEquals("x.svgz", GzipUtils.getCompressedFileName("x.svg"));
        assertEquals("x.cpgz", GzipUtils.getCompressedFilename("x.cpio"));
        assertEquals("x.cpgz", GzipUtils.getCompressedFileName("x.cpio"));
        assertEquals("x.wmz", GzipUtils.getCompressedFilename("x.wmf"));
        assertEquals("x.wmz", GzipUtils.getCompressedFileName("x.wmf"));
        assertEquals("x.emz", GzipUtils.getCompressedFilename("x.emf"));
        assertEquals("x.emz", GzipUtils.getCompressedFileName("x.emf"));

        assertEquals("x.svgz", GzipUtils.getCompressedFilename("x.SVG"));
        assertEquals("x.svgz", GzipUtils.getCompressedFileName("x.SVG"));
        assertEquals("X.svgz", GzipUtils.getCompressedFilename("X.SVG"));
        assertEquals("X.svgz", GzipUtils.getCompressedFileName("X.SVG"));
        assertEquals("X.svgz", GzipUtils.getCompressedFilename("X.svG"));
        assertEquals("X.svgz", GzipUtils.getCompressedFileName("X.svG"));

        assertEquals("x.wmf .gz", GzipUtils.getCompressedFilename("x.wmf "));
        assertEquals("x.wmf .gz", GzipUtils.getCompressedFileName("x.wmf "));
        assertEquals("x.wmf\n.gz", GzipUtils.getCompressedFilename("x.wmf\n"));
        assertEquals("x.wmf\n.gz", GzipUtils.getCompressedFileName("x.wmf\n"));
        assertEquals("x.wmf.y.gz", GzipUtils.getCompressedFilename("x.wmf.y"));
        assertEquals("x.wmf.y.gz", GzipUtils.getCompressedFileName("x.wmf.y"));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testGetUncompressedFilename() {
        assertEquals("", GzipUtils.getUncompressedFilename(""));
        assertEquals("", GzipUtils.getUncompressedFileName(""));
        assertEquals(".gz", GzipUtils.getUncompressedFilename(".gz"));
        assertEquals(".gz", GzipUtils.getUncompressedFileName(".gz"));

        assertEquals("x.tar", GzipUtils.getUncompressedFilename("x.tgz"));
        assertEquals("x.tar", GzipUtils.getUncompressedFileName("x.tgz"));
        assertEquals("x.tar", GzipUtils.getUncompressedFilename("x.taz"));
        assertEquals("x.tar", GzipUtils.getUncompressedFileName("x.taz"));
        assertEquals("x.svg", GzipUtils.getUncompressedFilename("x.svgz"));
        assertEquals("x.svg", GzipUtils.getUncompressedFileName("x.svgz"));
        assertEquals("x.cpio", GzipUtils.getUncompressedFilename("x.cpgz"));
        assertEquals("x.cpio", GzipUtils.getUncompressedFileName("x.cpgz"));
        assertEquals("x.wmf", GzipUtils.getUncompressedFilename("x.wmz"));
        assertEquals("x.wmf", GzipUtils.getUncompressedFileName("x.wmz"));
        assertEquals("x.emf", GzipUtils.getUncompressedFilename("x.emz"));
        assertEquals("x.emf", GzipUtils.getUncompressedFileName("x.emz"));
        assertEquals("x", GzipUtils.getUncompressedFilename("x.gz"));
        assertEquals("x", GzipUtils.getUncompressedFileName("x.gz"));
        assertEquals("x", GzipUtils.getUncompressedFilename("x.z"));
        assertEquals("x", GzipUtils.getUncompressedFileName("x.z"));
        assertEquals("x", GzipUtils.getUncompressedFilename("x-gz"));
        assertEquals("x", GzipUtils.getUncompressedFileName("x-gz"));
        assertEquals("x", GzipUtils.getUncompressedFilename("x-z"));
        assertEquals("x", GzipUtils.getUncompressedFileName("x-z"));
        assertEquals("x", GzipUtils.getUncompressedFilename("x_z"));
        assertEquals("x", GzipUtils.getUncompressedFileName("x_z"));

        assertEquals("x.svg", GzipUtils.getUncompressedFilename("x.SVGZ"));
        assertEquals("x.svg", GzipUtils.getUncompressedFileName("x.SVGZ"));
        assertEquals("X.svg", GzipUtils.getUncompressedFilename("X.SVGZ"));
        assertEquals("X.svg", GzipUtils.getUncompressedFileName("X.SVGZ"));
        assertEquals("X.svg", GzipUtils.getUncompressedFilename("X.svGZ"));
        assertEquals("X.svg", GzipUtils.getUncompressedFileName("X.svGZ"));

        assertEquals("x.wmz ", GzipUtils.getUncompressedFilename("x.wmz "));
        assertEquals("x.wmz ", GzipUtils.getUncompressedFileName("x.wmz "));
        assertEquals("x.wmz\n", GzipUtils.getUncompressedFilename("x.wmz\n"));
        assertEquals("x.wmz\n", GzipUtils.getUncompressedFileName("x.wmz\n"));
        assertEquals("x.wmz.y", GzipUtils.getUncompressedFilename("x.wmz.y"));
        assertEquals("x.wmz.y", GzipUtils.getUncompressedFileName("x.wmz.y"));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testIsCompressedFilename() {
        assertFalse(GzipUtils.isCompressedFilename(""));
        assertFalse(GzipUtils.isCompressedFileName(""));
        assertFalse(GzipUtils.isCompressedFilename(".gz"));
        assertFalse(GzipUtils.isCompressedFileName(".gz"));

        assertTrue(GzipUtils.isCompressedFilename("x.tgz"));
        assertTrue(GzipUtils.isCompressedFileName("x.tgz"));
        assertTrue(GzipUtils.isCompressedFilename("x.taz"));
        assertTrue(GzipUtils.isCompressedFileName("x.taz"));
        assertTrue(GzipUtils.isCompressedFilename("x.svgz"));
        assertTrue(GzipUtils.isCompressedFileName("x.svgz"));
        assertTrue(GzipUtils.isCompressedFilename("x.cpgz"));
        assertTrue(GzipUtils.isCompressedFileName("x.cpgz"));
        assertTrue(GzipUtils.isCompressedFilename("x.wmz"));
        assertTrue(GzipUtils.isCompressedFileName("x.wmz"));
        assertTrue(GzipUtils.isCompressedFilename("x.emz"));
        assertTrue(GzipUtils.isCompressedFileName("x.emz"));
        assertTrue(GzipUtils.isCompressedFilename("x.gz"));
        assertTrue(GzipUtils.isCompressedFileName("x.gz"));
        assertTrue(GzipUtils.isCompressedFilename("x.z"));
        assertTrue(GzipUtils.isCompressedFileName("x.z"));
        assertTrue(GzipUtils.isCompressedFilename("x-gz"));
        assertTrue(GzipUtils.isCompressedFileName("x-gz"));
        assertTrue(GzipUtils.isCompressedFilename("x-z"));
        assertTrue(GzipUtils.isCompressedFileName("x-z"));
        assertTrue(GzipUtils.isCompressedFilename("x_z"));
        assertTrue(GzipUtils.isCompressedFileName("x_z"));

        assertFalse(GzipUtils.isCompressedFilename("xxgz"));
        assertFalse(GzipUtils.isCompressedFileName("xxgz"));
        assertFalse(GzipUtils.isCompressedFilename("xzz"));
        assertFalse(GzipUtils.isCompressedFileName("xzz"));
        assertFalse(GzipUtils.isCompressedFilename("xaz"));
        assertFalse(GzipUtils.isCompressedFileName("xaz"));

        assertTrue(GzipUtils.isCompressedFilename("x.SVGZ"));
        assertTrue(GzipUtils.isCompressedFileName("x.SVGZ"));
        assertTrue(GzipUtils.isCompressedFilename("x.Svgz"));
        assertTrue(GzipUtils.isCompressedFileName("x.Svgz"));
        assertTrue(GzipUtils.isCompressedFilename("x.svGZ"));
        assertTrue(GzipUtils.isCompressedFileName("x.svGZ"));

        assertFalse(GzipUtils.isCompressedFilename("x.wmz "));
        assertFalse(GzipUtils.isCompressedFileName("x.wmz "));
        assertFalse(GzipUtils.isCompressedFilename("x.wmz\n"));
        assertFalse(GzipUtils.isCompressedFileName("x.wmz\n"));
        assertFalse(GzipUtils.isCompressedFilename("x.wmz.y"));
        assertFalse(GzipUtils.isCompressedFileName("x.wmz.y"));
    }

}
