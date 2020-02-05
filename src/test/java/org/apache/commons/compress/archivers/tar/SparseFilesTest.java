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

import static org.junit.Assert.*;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;


public class SparseFilesTest extends AbstractTestCase {

    private final boolean isOnWindows = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("windows");

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

            List<TarArchiveStructSparse> sparseHeaders = ae.getSparseHeaders();
            assertEquals(3, sparseHeaders.size());

            assertEquals(0, sparseHeaders.get(0).getOffset());
            assertEquals(2048, sparseHeaders.get(0).getNumbytes());

            assertEquals(1050624L, sparseHeaders.get(1).getOffset());
            assertEquals(2560, sparseHeaders.get(1).getNumbytes());

            assertEquals(3101184L, sparseHeaders.get(2).getOffset());
            assertEquals(0, sparseHeaders.get(2).getNumbytes());
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

    @Test
    public void testExtractSparseTarsOnWindows() throws IOException {
        if (!isOnWindows) {
            return;
        }

        final File oldGNUSparseTar = getFile("oldgnu_sparse.tar");
        final File paxGNUSparseTar = getFile("pax_gnu_sparse.tar");
        try (TarArchiveInputStream paxGNUSparseInputStream = new TarArchiveInputStream(new FileInputStream(paxGNUSparseTar))) {

            // compare between old GNU and PAX 0.0
            paxGNUSparseInputStream.getNextTarEntry();
            try (TarArchiveInputStream oldGNUSparseInputStream = new TarArchiveInputStream(new FileInputStream(oldGNUSparseTar))) {
                oldGNUSparseInputStream.getNextTarEntry();
                assertArrayEquals(IOUtils.toByteArray(oldGNUSparseInputStream),
                    IOUtils.toByteArray(paxGNUSparseInputStream));
            }

            // compare between old GNU and PAX 0.1
            paxGNUSparseInputStream.getNextTarEntry();
            try (TarArchiveInputStream oldGNUSparseInputStream = new TarArchiveInputStream(new FileInputStream(oldGNUSparseTar))) {
                oldGNUSparseInputStream.getNextTarEntry();
                assertArrayEquals(IOUtils.toByteArray(oldGNUSparseInputStream),
                    IOUtils.toByteArray(paxGNUSparseInputStream));
            }

            // compare between old GNU and PAX 1.0
            paxGNUSparseInputStream.getNextTarEntry();
            try (TarArchiveInputStream oldGNUSparseInputStream = new TarArchiveInputStream(new FileInputStream(oldGNUSparseTar))) {
                oldGNUSparseInputStream.getNextTarEntry();
                assertArrayEquals(IOUtils.toByteArray(oldGNUSparseInputStream),
                    IOUtils.toByteArray(paxGNUSparseInputStream));
            }
        }
    }

    @Test
    public void testExtractOldGNU() throws IOException, InterruptedException {
        if (isOnWindows) {
            return;
        }

        try {
        final File file = getFile("oldgnu_sparse.tar");
        try (InputStream sparseFileInputStream = extractTarAndGetInputStream(file, "sparsefile");
             TarArchiveInputStream tin = new TarArchiveInputStream(new FileInputStream(file))) {
            tin.getNextTarEntry();
            assertArrayEquals(IOUtils.toByteArray(tin),
                IOUtils.toByteArray(sparseFileInputStream));
        }
        } catch (RuntimeException | IOException ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

    @Test
    public void testExtractExtendedOldGNU() throws IOException, InterruptedException {
        if (isOnWindows) {
            return;
        }

        final File file = getFile("oldgnu_extended_sparse.tar");
        try (InputStream sparseFileInputStream = extractTarAndGetInputStream(file, "sparse6");
             TarArchiveInputStream tin = new TarArchiveInputStream(new FileInputStream(file))) {
            final TarArchiveEntry ae = tin.getNextTarEntry();

            assertArrayEquals(IOUtils.toByteArray(tin),
                IOUtils.toByteArray(sparseFileInputStream));

            List<TarArchiveStructSparse> sparseHeaders = ae.getSparseHeaders();
            assertEquals(7, sparseHeaders.size());

            assertEquals(0, sparseHeaders.get(0).getOffset());
            assertEquals(1024, sparseHeaders.get(0).getNumbytes());

            assertEquals(10240, sparseHeaders.get(1).getOffset());
            assertEquals(1024, sparseHeaders.get(1).getNumbytes());

            assertEquals(16384, sparseHeaders.get(2).getOffset());
            assertEquals(1024, sparseHeaders.get(2).getNumbytes());

            assertEquals(24576, sparseHeaders.get(3).getOffset());
            assertEquals(1024, sparseHeaders.get(3).getNumbytes());

            assertEquals(29696, sparseHeaders.get(4).getOffset());
            assertEquals(1024, sparseHeaders.get(4).getNumbytes());

            assertEquals(36864, sparseHeaders.get(5).getOffset());
            assertEquals(1024, sparseHeaders.get(5).getNumbytes());

            assertEquals(51200, sparseHeaders.get(6).getOffset());
            assertEquals(0, sparseHeaders.get(6).getNumbytes());
        }
    }

    @Test
    public void testExtractPaxGNU() throws IOException, InterruptedException {
        if (isOnWindows) {
            return;
        }

        final File file = getFile("pax_gnu_sparse.tar");
        try (TarArchiveInputStream tin = new TarArchiveInputStream(new FileInputStream(file))) {

            tin.getNextTarEntry();
            try (InputStream sparseFileInputStream = extractTarAndGetInputStream(file, "sparsefile-0.0")) {
                assertArrayEquals(IOUtils.toByteArray(tin),
                    IOUtils.toByteArray(sparseFileInputStream));
            }

            // TODO : it's wired that I can only get a 0 size sparsefile-0.1 on my Ubuntu 16.04
            //        using "tar -xf pax_gnu_sparse.tar"
            tin.getNextTarEntry();
            try (InputStream sparseFileInputStream = extractTarAndGetInputStream(file, "sparsefile-0.0")) {
                assertArrayEquals(IOUtils.toByteArray(tin),
                    IOUtils.toByteArray(sparseFileInputStream));
            }

            tin.getNextTarEntry();
            try (InputStream sparseFileInputStream = extractTarAndGetInputStream(file, "sparsefile-1.0")) {
                assertArrayEquals(IOUtils.toByteArray(tin),
                    IOUtils.toByteArray(sparseFileInputStream));
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

        List<TarArchiveStructSparse> sparseHeaders = ae.getSparseHeaders();
        assertEquals(3, sparseHeaders.size());

        assertEquals(0, sparseHeaders.get(0).getOffset());
        assertEquals(2048, sparseHeaders.get(0).getNumbytes());

        assertEquals(1050624L, sparseHeaders.get(1).getOffset());
        assertEquals(2560, sparseHeaders.get(1).getNumbytes());

        assertEquals(3101184L, sparseHeaders.get(2).getOffset());
        assertEquals(0, sparseHeaders.get(2).getNumbytes());
    }

    private InputStream extractTarAndGetInputStream(File tarFile, String sparseFileName) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("tar", "-xf", tarFile.getPath(), "-C", resultDir.getPath());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        // wait until the extract finishes
        assertEquals(new String(IOUtils.toByteArray(process.getInputStream())), 0, process.waitFor());

        for (File file : resultDir.listFiles()) {
            if (file.getName().equals(sparseFileName)) {
                return new FileInputStream(file);
            }
        }
        fail("didn't find " + sparseFileName + " after extracting " + tarFile);
        return null;
    }
}

