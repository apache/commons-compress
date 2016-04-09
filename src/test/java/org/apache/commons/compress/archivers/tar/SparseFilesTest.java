/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.commons.compress.archivers.tar;

import static org.apache.commons.compress.AbstractTestCase.getFile;
import static org.junit.Assert.*;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;


public class SparseFilesTest {

    @Test
    public void testOldGNU() throws Throwable {
        final File file = getFile("oldgnu_sparse.tar");
        TarArchiveInputStream tin = null;
        try {
            tin = new TarArchiveInputStream(new FileInputStream(file));
            final TarArchiveEntry ae = tin.getNextTarEntry();
            assertEquals("sparsefile", ae.getName());
            assertTrue(ae.isOldGNUSparse());
            assertTrue(ae.isGNUSparse());
            assertFalse(ae.isPaxGNUSparse());
            assertFalse(tin.canReadEntryData(ae));
        } finally {
            if (tin != null) {
                tin.close();
            }
        }
    }

    @Test
    public void testPaxGNU() throws Throwable {
        final File file = getFile("pax_gnu_sparse.tar");
        TarArchiveInputStream tin = null;
        try {
            tin = new TarArchiveInputStream(new FileInputStream(file));
            assertPaxGNUEntry(tin, "0.0");
            assertPaxGNUEntry(tin, "0.1");
            assertPaxGNUEntry(tin, "1.0");
        } finally {
            if (tin != null) {
                tin.close();
            }
        }
    }

    private void assertPaxGNUEntry(final TarArchiveInputStream tin, final String suffix) throws Throwable {
        final TarArchiveEntry ae = tin.getNextTarEntry();
        assertEquals("sparsefile-" + suffix, ae.getName());
        assertTrue(ae.isGNUSparse());
        assertTrue(ae.isPaxGNUSparse());
        assertFalse(ae.isOldGNUSparse());
        assertFalse(tin.canReadEntryData(ae));
    }
}

