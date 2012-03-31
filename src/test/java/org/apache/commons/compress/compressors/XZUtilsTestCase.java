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

import junit.framework.TestCase;

import org.apache.commons.compress.compressors.xz.XZUtils;

public class XZUtilsTestCase extends TestCase {

    public void testIsCompressedFilename() {
        assertFalse(XZUtils.isCompressedFilename(""));
        assertFalse(XZUtils.isCompressedFilename(".xz"));

        assertTrue(XZUtils.isCompressedFilename("x.txz"));
        assertTrue(XZUtils.isCompressedFilename("x.xz"));
        assertTrue(XZUtils.isCompressedFilename("x-xz"));

        assertFalse(XZUtils.isCompressedFilename("xxgz"));
        assertFalse(XZUtils.isCompressedFilename("xzz"));
        assertFalse(XZUtils.isCompressedFilename("xaz"));

        assertFalse(XZUtils.isCompressedFilename("x.txz "));
        assertFalse(XZUtils.isCompressedFilename("x.txz\n"));
        assertFalse(XZUtils.isCompressedFilename("x.txz.y"));
    }

    public void testGetUncompressedFilename() {
        assertEquals("", XZUtils.getUncompressedFilename(""));
        assertEquals(".xz", XZUtils.getUncompressedFilename(".xz"));

        assertEquals("x.tar", XZUtils.getUncompressedFilename("x.txz"));
        assertEquals("x", XZUtils.getUncompressedFilename("x.xz"));
        assertEquals("x", XZUtils.getUncompressedFilename("x-xz"));

        assertEquals("x.txz ", XZUtils.getUncompressedFilename("x.txz "));
        assertEquals("x.txz\n", XZUtils.getUncompressedFilename("x.txz\n"));
        assertEquals("x.txz.y", XZUtils.getUncompressedFilename("x.txz.y"));
    }

    public void testGetCompressedFilename() {
        assertEquals(".xz", XZUtils.getCompressedFilename(""));
        assertEquals("x.xz", XZUtils.getCompressedFilename("x"));

        assertEquals("x.txz", XZUtils.getCompressedFilename("x.tar"));

        assertEquals("x.wmf .xz", XZUtils.getCompressedFilename("x.wmf "));
        assertEquals("x.wmf\n.xz", XZUtils.getCompressedFilename("x.wmf\n"));
        assertEquals("x.wmf.y.xz", XZUtils.getCompressedFilename("x.wmf.y"));
    }

}
