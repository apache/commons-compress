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

    @Test
    public void testGetCompressedFilename() {
        assertEquals(".gz", GzipUtils.getCompressedFilename(""));
        assertEquals("x.gz", GzipUtils.getCompressedFilename("x"));

        assertEquals("x.tgz", GzipUtils.getCompressedFilename("x.tar"));
        assertEquals("x.svgz", GzipUtils.getCompressedFilename("x.svg"));
        assertEquals("x.cpgz", GzipUtils.getCompressedFilename("x.cpio"));
        assertEquals("x.wmz", GzipUtils.getCompressedFilename("x.wmf"));
        assertEquals("x.emz", GzipUtils.getCompressedFilename("x.emf"));

        assertEquals("x.svgz", GzipUtils.getCompressedFilename("x.SVG"));
        assertEquals("X.svgz", GzipUtils.getCompressedFilename("X.SVG"));
        assertEquals("X.svgz", GzipUtils.getCompressedFilename("X.svG"));

        assertEquals("x.wmf .gz", GzipUtils.getCompressedFilename("x.wmf "));
        assertEquals("x.wmf\n.gz", GzipUtils.getCompressedFilename("x.wmf\n"));
        assertEquals("x.wmf.y.gz", GzipUtils.getCompressedFilename("x.wmf.y"));
    }

    @Test
    public void testGetUncompressedFilename() {
        assertEquals("", GzipUtils.getUncompressedFilename(""));
        assertEquals(".gz", GzipUtils.getUncompressedFilename(".gz"));

        assertEquals("x.tar", GzipUtils.getUncompressedFilename("x.tgz"));
        assertEquals("x.tar", GzipUtils.getUncompressedFilename("x.taz"));
        assertEquals("x.svg", GzipUtils.getUncompressedFilename("x.svgz"));
        assertEquals("x.cpio", GzipUtils.getUncompressedFilename("x.cpgz"));
        assertEquals("x.wmf", GzipUtils.getUncompressedFilename("x.wmz"));
        assertEquals("x.emf", GzipUtils.getUncompressedFilename("x.emz"));
        assertEquals("x", GzipUtils.getUncompressedFilename("x.gz"));
        assertEquals("x", GzipUtils.getUncompressedFilename("x.z"));
        assertEquals("x", GzipUtils.getUncompressedFilename("x-gz"));
        assertEquals("x", GzipUtils.getUncompressedFilename("x-z"));
        assertEquals("x", GzipUtils.getUncompressedFilename("x_z"));

        assertEquals("x.svg", GzipUtils.getUncompressedFilename("x.SVGZ"));
        assertEquals("X.svg", GzipUtils.getUncompressedFilename("X.SVGZ"));
        assertEquals("X.svg", GzipUtils.getUncompressedFilename("X.svGZ"));

        assertEquals("x.wmz ", GzipUtils.getUncompressedFilename("x.wmz "));
        assertEquals("x.wmz\n", GzipUtils.getUncompressedFilename("x.wmz\n"));
        assertEquals("x.wmz.y", GzipUtils.getUncompressedFilename("x.wmz.y"));
    }

    @Test
    public void testIsCompressedFilename() {
        assertFalse(GzipUtils.isCompressedFilename(""));
        assertFalse(GzipUtils.isCompressedFilename(".gz"));

        assertTrue(GzipUtils.isCompressedFilename("x.tgz"));
        assertTrue(GzipUtils.isCompressedFilename("x.taz"));
        assertTrue(GzipUtils.isCompressedFilename("x.svgz"));
        assertTrue(GzipUtils.isCompressedFilename("x.cpgz"));
        assertTrue(GzipUtils.isCompressedFilename("x.wmz"));
        assertTrue(GzipUtils.isCompressedFilename("x.emz"));
        assertTrue(GzipUtils.isCompressedFilename("x.gz"));
        assertTrue(GzipUtils.isCompressedFilename("x.z"));
        assertTrue(GzipUtils.isCompressedFilename("x-gz"));
        assertTrue(GzipUtils.isCompressedFilename("x-z"));
        assertTrue(GzipUtils.isCompressedFilename("x_z"));

        assertFalse(GzipUtils.isCompressedFilename("xxgz"));
        assertFalse(GzipUtils.isCompressedFilename("xzz"));
        assertFalse(GzipUtils.isCompressedFilename("xaz"));

        assertTrue(GzipUtils.isCompressedFilename("x.SVGZ"));
        assertTrue(GzipUtils.isCompressedFilename("x.Svgz"));
        assertTrue(GzipUtils.isCompressedFilename("x.svGZ"));

        assertFalse(GzipUtils.isCompressedFilename("x.wmz "));
        assertFalse(GzipUtils.isCompressedFilename("x.wmz\n"));
        assertFalse(GzipUtils.isCompressedFilename("x.wmz.y"));
    }

}
