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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.Assume;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public class SparseFilesTest extends AbstractTestCase {

    private final boolean isOnWindows = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("windows");

    @Test
    public void testOldGNU() throws Throwable {
        final File file = getFile("oldgnu_sparse.tar");
        try (TarArchiveInputStream tin = new TarArchiveInputStream(new FileInputStream(file))) {
            final TarArchiveEntry ae = tin.getNextTarEntry();
            assertEquals("sparsefile", ae.getName());
            assertTrue(ae.isOldGNUSparse());
            assertTrue(ae.isGNUSparse());
            assertFalse(ae.isPaxGNUSparse());
            assertTrue(tin.canReadEntryData(ae));

            final List<TarArchiveStructSparse> sparseHeaders = ae.getSparseHeaders();
            assertEquals(3, sparseHeaders.size());

            assertEquals(0, sparseHeaders.get(0).getOffset());
            assertEquals(2048, sparseHeaders.get(0).getNumbytes());

            assertEquals(1050624L, sparseHeaders.get(1).getOffset());
            assertEquals(2560, sparseHeaders.get(1).getNumbytes());

            assertEquals(3101184L, sparseHeaders.get(2).getOffset());
            assertEquals(0, sparseHeaders.get(2).getNumbytes());
        }
    }

    @Test
    public void testTarFileOldGNU() throws Throwable {
        final File file = getFile("oldgnu_sparse.tar");
        try (final TarFile tarFile = new TarFile(file)) {
            TarArchiveEntry ae = tarFile.getEntries().get(0);
            assertEquals("sparsefile", ae.getName());
            assertTrue(ae.isOldGNUSparse());
            assertTrue(ae.isGNUSparse());
            assertFalse(ae.isPaxGNUSparse());

            List<TarArchiveStructSparse> sparseHeaders = ae.getSparseHeaders();
            assertEquals(3, sparseHeaders.size());

            assertEquals(0, sparseHeaders.get(0).getOffset());
            assertEquals(2048, sparseHeaders.get(0).getNumbytes());

            assertEquals(1050624L, sparseHeaders.get(1).getOffset());
            assertEquals(2560, sparseHeaders.get(1).getNumbytes());

            assertEquals(3101184L, sparseHeaders.get(2).getOffset());
            assertEquals(0, sparseHeaders.get(2).getNumbytes());
        }
    }

    @Test
    public void testPaxGNU() throws Throwable {
        final File file = getFile("pax_gnu_sparse.tar");
        try (TarArchiveInputStream tin = new TarArchiveInputStream(new FileInputStream(file))) {
            assertPaxGNUEntry(tin, "0.0");
            assertPaxGNUEntry(tin, "0.1");
            assertPaxGNUEntry(tin, "1.0");
        }
    }

    @Test
    public void testTarFilePaxGNU() throws IOException {
        final File file = getFile("pax_gnu_sparse.tar");
        try (final TarFile tarFile = new TarFile(file)) {
            List<TarArchiveEntry> entries = tarFile.getEntries();
            assertPaxGNUEntry(entries.get(0), "0.0");
            assertPaxGNUEntry(entries.get(1), "0.1");
            assertPaxGNUEntry(entries.get(2), "1.0");
        }
    }

    private void assertPaxGNUEntry(final TarArchiveEntry entry, final String suffix) {
        assertEquals("sparsefile-" + suffix, entry.getName());
        assertTrue(entry.isGNUSparse());
        assertTrue(entry.isPaxGNUSparse());
        assertFalse(entry.isOldGNUSparse());

        List<TarArchiveStructSparse> sparseHeaders = entry.getSparseHeaders();
        assertEquals(3, sparseHeaders.size());

        assertEquals(0, sparseHeaders.get(0).getOffset());
        assertEquals(2048, sparseHeaders.get(0).getNumbytes());

        assertEquals(1050624L, sparseHeaders.get(1).getOffset());
        assertEquals(2560, sparseHeaders.get(1).getNumbytes());

        assertEquals(3101184L, sparseHeaders.get(2).getOffset());
        assertEquals(0, sparseHeaders.get(2).getNumbytes());
    }

    @Test
    public void testExtractSparseTarsOnWindows() throws IOException {
        assumeTrue("This test should be ignored if not running on Windows", isOnWindows);

        final File oldGNUSparseTar = getFile("oldgnu_sparse.tar");
        final File paxGNUSparseTar = getFile("pax_gnu_sparse.tar");
        try (TarArchiveInputStream paxGNUSparseInputStream = new TarArchiveInputStream(new FileInputStream(paxGNUSparseTar))) {

            // compare between old GNU and PAX 0.0
            TarArchiveEntry paxGNUEntry = paxGNUSparseInputStream.getNextTarEntry();
            assertTrue(paxGNUSparseInputStream.canReadEntryData(paxGNUEntry));
            try (TarArchiveInputStream oldGNUSparseInputStream = new TarArchiveInputStream(new FileInputStream(oldGNUSparseTar))) {
                final TarArchiveEntry oldGNUEntry = oldGNUSparseInputStream.getNextTarEntry();
                assertTrue(oldGNUSparseInputStream.canReadEntryData(oldGNUEntry));
                assertArrayEquals(IOUtils.toByteArray(oldGNUSparseInputStream),
                        IOUtils.toByteArray(paxGNUSparseInputStream));
            }

            // compare between old GNU and PAX 0.1
            paxGNUEntry = paxGNUSparseInputStream.getNextTarEntry();
            assertTrue(paxGNUSparseInputStream.canReadEntryData(paxGNUEntry));
            try (TarArchiveInputStream oldGNUSparseInputStream = new TarArchiveInputStream(new FileInputStream(oldGNUSparseTar))) {
                final TarArchiveEntry oldGNUEntry = oldGNUSparseInputStream.getNextTarEntry();
                assertTrue(oldGNUSparseInputStream.canReadEntryData(oldGNUEntry));
                assertArrayEquals(IOUtils.toByteArray(oldGNUSparseInputStream),
                        IOUtils.toByteArray(paxGNUSparseInputStream));
            }

            // compare between old GNU and PAX 1.0
            paxGNUEntry = paxGNUSparseInputStream.getNextTarEntry();
            assertTrue(paxGNUSparseInputStream.canReadEntryData(paxGNUEntry));
            try (TarArchiveInputStream oldGNUSparseInputStream = new TarArchiveInputStream(new FileInputStream(oldGNUSparseTar))) {
                final TarArchiveEntry oldGNUEntry = oldGNUSparseInputStream.getNextTarEntry();
                assertTrue(oldGNUSparseInputStream.canReadEntryData(oldGNUEntry));
                assertArrayEquals(IOUtils.toByteArray(oldGNUSparseInputStream),
                        IOUtils.toByteArray(paxGNUSparseInputStream));
            }
        }
    }

    @Test
    public void testTarFileExtractSparseTarsOnWindows() throws IOException {
        Assume.assumeTrue("Only run test on Windows", isOnWindows);

        final File oldGNUSparseTar = getFile("oldgnu_sparse.tar");
        final File paxGNUSparseTar = getFile("pax_gnu_sparse.tar");
        try (TarFile paxGnu = new TarFile(paxGNUSparseTar)) {
            List<TarArchiveEntry> entries = paxGnu.getEntries();

            // compare between old GNU and PAX 0.0
            TarArchiveEntry paxGnuEntry = entries.get(0);
            try (TarFile oldGnu = new TarFile(oldGNUSparseTar)) {
                TarArchiveEntry oldGnuEntry = oldGnu.getEntries().get(0);
                try (InputStream old = oldGnu.getInputStream(oldGnuEntry);
                     InputStream pax = paxGnu.getInputStream(paxGnuEntry)) {
                    assertArrayEquals(IOUtils.toByteArray(old), IOUtils.toByteArray(pax));
                }
            }

            // compare between old GNU and PAX 0.1
            paxGnuEntry = entries.get(1);
            try (TarFile oldGnu = new TarFile(oldGNUSparseTar)) {
                TarArchiveEntry oldGnuEntry = oldGnu.getEntries().get(0);
                try (InputStream old = oldGnu.getInputStream(oldGnuEntry);
                     InputStream pax = paxGnu.getInputStream(paxGnuEntry)) {
                    assertArrayEquals(IOUtils.toByteArray(old), IOUtils.toByteArray(pax));
                }
            }

            // compare between old GNU and PAX 1.0
            paxGnuEntry = entries.get(2);
            try (TarFile oldGnu = new TarFile(oldGNUSparseTar)) {
                TarArchiveEntry oldGnuEntry = oldGnu.getEntries().get(0);
                try (InputStream old = oldGnu.getInputStream(oldGnuEntry);
                     InputStream pax = paxGnu.getInputStream(paxGnuEntry)) {
                    assertArrayEquals(IOUtils.toByteArray(old), IOUtils.toByteArray(pax));
                }
            }
        }
    }

    @Test
    public void testExtractOldGNU() throws IOException, InterruptedException {
        assumeFalse("This test should be ignored on Windows", isOnWindows);

        try {
            final File file = getFile("oldgnu_sparse.tar");
            try (InputStream sparseFileInputStream = extractTarAndGetInputStream(file, "sparsefile");
                 TarArchiveInputStream tin = new TarArchiveInputStream(new FileInputStream(file))) {
                final TarArchiveEntry entry = tin.getNextTarEntry();
                assertTrue(tin.canReadEntryData(entry));
                assertArrayEquals(IOUtils.toByteArray(tin),
                        IOUtils.toByteArray(sparseFileInputStream));
            }
        } catch (RuntimeException | IOException ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

    @Test
    public void testTarFileExtractOldGNU() throws IOException, InterruptedException {
        Assume.assumeFalse("Don't run test on Windows", isOnWindows);

        File file = getFile("oldgnu_sparse.tar");
        try (final InputStream sparseFileInputStream = extractTarAndGetInputStream(file, "sparsefile");
             final TarFile tarFile = new TarFile(file)) {
            TarArchiveEntry entry = tarFile.getEntries().get(0);
            try (InputStream tarInput = tarFile.getInputStream(entry)) {
                assertArrayEquals(IOUtils.toByteArray(tarInput), IOUtils.toByteArray(sparseFileInputStream));
            }
        }
    }

    @Test
    public void testExtractExtendedOldGNU() throws IOException, InterruptedException {
        assumeFalse("This test should be ignored on Windows", isOnWindows);

        final File file = getFile("oldgnu_extended_sparse.tar");
        try (InputStream sparseFileInputStream = extractTarAndGetInputStream(file, "sparse6");
             TarArchiveInputStream tin = new TarArchiveInputStream(new FileInputStream(file))) {
            final TarArchiveEntry ae = tin.getNextTarEntry();
            assertTrue(tin.canReadEntryData(ae));

            assertArrayEquals(IOUtils.toByteArray(tin),
                IOUtils.toByteArray(sparseFileInputStream));

            final List<TarArchiveStructSparse> sparseHeaders = ae.getSparseHeaders();
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
    public void testTarFileExtractExtendedOldGNU() throws IOException, InterruptedException {
        Assume.assumeFalse("Don't run test on Windows", isOnWindows);

        final File file = getFile("oldgnu_extended_sparse.tar");
        try (InputStream sparseFileInputStream = extractTarAndGetInputStream(file, "sparse6");
             TarFile tarFile = new TarFile(file)) {
            TarArchiveEntry ae = tarFile.getEntries().get(0);

            try (InputStream tarInput = tarFile.getInputStream(ae)) {
                assertArrayEquals(IOUtils.toByteArray(tarInput), IOUtils.toByteArray(sparseFileInputStream));
            }

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
        assumeFalse("This test should be ignored on Windows", isOnWindows);
        // GNU tar with version 1.28 has some problems reading sparsefile-0.1,
        // so the test should be skipped then
        // TODO : what about the versions lower than 1.28?
        assumeFalse("This test should be ignored if GNU tar is version 1.28",
                getTarBinaryHelp().startsWith("tar (GNU tar) 1.28"));

        final File file = getFile("pax_gnu_sparse.tar");
        try (TarArchiveInputStream tin = new TarArchiveInputStream(new FileInputStream(file))) {

            TarArchiveEntry paxGNUEntry = tin.getNextTarEntry();
            assertTrue(tin.canReadEntryData(paxGNUEntry));
            try (InputStream sparseFileInputStream = extractTarAndGetInputStream(file, "sparsefile-0.0")) {
                assertArrayEquals(IOUtils.toByteArray(tin),
                        IOUtils.toByteArray(sparseFileInputStream));
            }

            paxGNUEntry = tin.getNextTarEntry();
            assertTrue(tin.canReadEntryData(paxGNUEntry));
            try (InputStream sparseFileInputStream = extractTarAndGetInputStream(file, "sparsefile-0.1")) {
                assertArrayEquals(IOUtils.toByteArray(tin),
                        IOUtils.toByteArray(sparseFileInputStream));
            }

            paxGNUEntry = tin.getNextTarEntry();
            assertTrue(tin.canReadEntryData(paxGNUEntry));
            try (InputStream sparseFileInputStream = extractTarAndGetInputStream(file, "sparsefile-1.0")) {
                assertArrayEquals(IOUtils.toByteArray(tin),
                        IOUtils.toByteArray(sparseFileInputStream));
            }
        }
    }

    @Test
    public void testTarFileExtractPaxGNU() throws IOException, InterruptedException {
        Assume.assumeFalse("Don't run test on Windows", isOnWindows);
        // GNU tar with version 1.28 has some problems reading sparsefile-0.1,
        // so the test should be skipped then
        // TODO : what about the versions lower than 1.28?
        assumeFalse("This test should be ignored if GNU tar is version 1.28",
                getTarBinaryHelp().startsWith("tar (GNU tar) 1.28"));

        final File file = getFile("pax_gnu_sparse.tar");
        try (final TarFile paxGnu = new TarFile(file)) {
            List<TarArchiveEntry> entries = paxGnu.getEntries();

            TarArchiveEntry entry = entries.get(0);
            try (InputStream sparseFileInputStream = extractTarAndGetInputStream(file, "sparsefile-0.0");
                 InputStream paxInput = paxGnu.getInputStream(entry)) {
                assertArrayEquals(IOUtils.toByteArray(paxInput), IOUtils.toByteArray(sparseFileInputStream));
            }

            entry = entries.get(1);
            try (InputStream sparseFileInputStream = extractTarAndGetInputStream(file, "sparsefile-0.1");
                 InputStream paxInput = paxGnu.getInputStream(entry)) {
                assertArrayEquals(IOUtils.toByteArray(paxInput), IOUtils.toByteArray(sparseFileInputStream));
            }

            entry = entries.get(2);
            try (InputStream sparseFileInputStream = extractTarAndGetInputStream(file, "sparsefile-1.0");
                 InputStream paxInput = paxGnu.getInputStream(entry)) {
                assertArrayEquals(IOUtils.toByteArray(paxInput), IOUtils.toByteArray(sparseFileInputStream));
            }
        }
    }

    @Test
    public void compareTarArchiveInputStreamWithTarFile() throws IOException {
        Path file = getPath("oldgnu_sparse.tar");
        try (TarArchiveInputStream tarIn = new TarArchiveInputStream(new BufferedInputStream(Files.newInputStream(file)));
             TarFile tarFile = new TarFile(file)) {
            TarArchiveEntry tarInEntry = tarIn.getNextTarEntry();
            assertArrayEquals(IOUtils.toByteArray(tarIn), IOUtils.toByteArray(tarFile.getInputStream(tarFile.getEntries().get(0))));
        }
    }

    private void assertPaxGNUEntry(final TarArchiveInputStream tin, final String suffix) throws Throwable {
        final TarArchiveEntry ae = tin.getNextTarEntry();
        assertEquals("sparsefile-" + suffix, ae.getName());
        assertTrue(ae.isGNUSparse());
        assertTrue(ae.isPaxGNUSparse());
        assertFalse(ae.isOldGNUSparse());
        assertTrue(tin.canReadEntryData(ae));

        final List<TarArchiveStructSparse> sparseHeaders = ae.getSparseHeaders();
        assertEquals(3, sparseHeaders.size());

        assertEquals(0, sparseHeaders.get(0).getOffset());
        assertEquals(2048, sparseHeaders.get(0).getNumbytes());

        assertEquals(1050624L, sparseHeaders.get(1).getOffset());
        assertEquals(2560, sparseHeaders.get(1).getNumbytes());

        assertEquals(3101184L, sparseHeaders.get(2).getOffset());
        assertEquals(0, sparseHeaders.get(2).getNumbytes());
    }

    private InputStream extractTarAndGetInputStream(final File tarFile, final String sparseFileName) throws IOException, InterruptedException {
        final ProcessBuilder pb = new ProcessBuilder("tar", "-xf", tarFile.getPath(), "-C", resultDir.getPath());
        pb.redirectErrorStream(true);
        final Process process = pb.start();
        // wait until the extract finishes
        assertEquals(new String(IOUtils.toByteArray(process.getInputStream())), 0, process.waitFor());

        for (final File file : resultDir.listFiles()) {
            if (file.getName().equals(sparseFileName)) {
                return new FileInputStream(file);
            }
        }
        fail("didn't find " + sparseFileName + " after extracting " + tarFile);
        return null;
    }

    private String getTarBinaryHelp() throws IOException {
        final ProcessBuilder pb = new ProcessBuilder("tar", "--version");
        pb.redirectErrorStream(true);
        final Process process = pb.start();
        // wait until the help is shown
        return new String(IOUtils.toByteArray(process.getInputStream()));
    }
}

