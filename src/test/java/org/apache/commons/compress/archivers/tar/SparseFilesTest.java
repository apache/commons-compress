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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

public class SparseFilesTest extends AbstractTestCase {

    private final boolean isOnWindows = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("windows");

    private void assertPaxGNUEntry(final TarArchiveEntry entry, final String suffix) {
        assertEquals("sparsefile-" + suffix, entry.getName());
        assertTrue(entry.isGNUSparse());
        assertTrue(entry.isPaxGNUSparse());
        assertFalse(entry.isOldGNUSparse());

        final List<TarArchiveStructSparse> sparseHeaders = entry.getSparseHeaders();
        assertEquals(3, sparseHeaders.size());

        assertEquals(0, sparseHeaders.get(0).getOffset());
        assertEquals(2048, sparseHeaders.get(0).getNumbytes());

        assertEquals(1050624L, sparseHeaders.get(1).getOffset());
        assertEquals(2560, sparseHeaders.get(1).getNumbytes());

        assertEquals(3101184L, sparseHeaders.get(2).getOffset());
        assertEquals(0, sparseHeaders.get(2).getNumbytes());
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

    @Test
    public void compareTarArchiveInputStreamWithTarFile() throws IOException {
        final Path file = getPath("oldgnu_sparse.tar");
        try (TarArchiveInputStream tarIn = new TarArchiveInputStream(new BufferedInputStream(Files.newInputStream(file)));
             TarFile tarFile = new TarFile(file)) {
            final TarArchiveEntry tarInEntry = tarIn.getNextTarEntry();
            assertArrayEquals(IOUtils.toByteArray(tarIn), IOUtils.toByteArray(tarFile.getInputStream(tarFile.getEntries().get(0))));
        }
    }

    private InputStream extractTarAndGetInputStream(final File tarFile, final String sparseFileName) throws IOException, InterruptedException {
        final ProcessBuilder pb = new ProcessBuilder("tar", "-xf", tarFile.getPath(), "-C", resultDir.getPath());
        pb.redirectErrorStream(true);
        final Process process = pb.start();
        // wait until the extract finishes
        assertEquals(0, process.waitFor(), new String(IOUtils.toByteArray(process.getInputStream())));

        for (final File file : resultDir.listFiles()) {
            if (file.getName().equals(sparseFileName)) {
                return Files.newInputStream(file.toPath());
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

    @Test
    @DisabledOnOs(OS.WINDOWS)
    public void testExtractExtendedOldGNU() throws IOException, InterruptedException {
        final File file = getFile("oldgnu_extended_sparse.tar");
        try (InputStream sparseFileInputStream = extractTarAndGetInputStream(file, "sparse6");
             TarArchiveInputStream tin = new TarArchiveInputStream(Files.newInputStream(file.toPath()))) {
            final TarArchiveEntry ae = tin.getNextTarEntry();
            assertTrue(tin.canReadEntryData(ae));

            assertArrayEquals(IOUtils.toByteArray(tin),
                IOUtils.toByteArray(sparseFileInputStream));

            final List<TarArchiveStructSparse> sparseHeaders = ae.getOrderedSparseHeaders();
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
    @DisabledOnOs(OS.WINDOWS)
    public void testExtractOldGNU() throws IOException, InterruptedException {
        try {
            final File file = getFile("oldgnu_sparse.tar");
            try (InputStream sparseFileInputStream = extractTarAndGetInputStream(file, "sparsefile");
                 TarArchiveInputStream tin = new TarArchiveInputStream(Files.newInputStream(file.toPath()))) {
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
    @DisabledOnOs(OS.WINDOWS)
    public void testExtractPaxGNU() throws IOException, InterruptedException {
        // GNU tar with version 1.28 has some problems reading sparsefile-0.1,
        // so the test should be skipped then
        // TODO : what about the versions lower than 1.28?
        assumeFalse(getTarBinaryHelp().startsWith("tar (GNU tar) 1.28"),
                "This test should be ignored if GNU tar is version 1.28");

        final File file = getFile("pax_gnu_sparse.tar");
        try (TarArchiveInputStream tin = new TarArchiveInputStream(Files.newInputStream(file.toPath()))) {

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
    @EnabledOnOs(OS.WINDOWS)
    public void testExtractSparseTarsOnWindows() throws IOException {
        final File oldGNUSparseTar = getFile("oldgnu_sparse.tar");
        final File paxGNUSparseTar = getFile("pax_gnu_sparse.tar");
        try (TarArchiveInputStream paxGNUSparseInputStream = new TarArchiveInputStream(Files.newInputStream(paxGNUSparseTar.toPath()))) {

            // compare between old GNU and PAX 0.0
            TarArchiveEntry paxGNUEntry = paxGNUSparseInputStream.getNextTarEntry();
            assertTrue(paxGNUSparseInputStream.canReadEntryData(paxGNUEntry));
            try (TarArchiveInputStream oldGNUSparseInputStream = new TarArchiveInputStream(Files.newInputStream(oldGNUSparseTar.toPath()))) {
                final TarArchiveEntry oldGNUEntry = oldGNUSparseInputStream.getNextTarEntry();
                assertTrue(oldGNUSparseInputStream.canReadEntryData(oldGNUEntry));
                assertArrayEquals(IOUtils.toByteArray(oldGNUSparseInputStream),
                        IOUtils.toByteArray(paxGNUSparseInputStream));
            }

            // compare between old GNU and PAX 0.1
            paxGNUEntry = paxGNUSparseInputStream.getNextTarEntry();
            assertTrue(paxGNUSparseInputStream.canReadEntryData(paxGNUEntry));
            try (TarArchiveInputStream oldGNUSparseInputStream = new TarArchiveInputStream(Files.newInputStream(oldGNUSparseTar.toPath()))) {
                final TarArchiveEntry oldGNUEntry = oldGNUSparseInputStream.getNextTarEntry();
                assertTrue(oldGNUSparseInputStream.canReadEntryData(oldGNUEntry));
                assertArrayEquals(IOUtils.toByteArray(oldGNUSparseInputStream),
                        IOUtils.toByteArray(paxGNUSparseInputStream));
            }

            // compare between old GNU and PAX 1.0
            paxGNUEntry = paxGNUSparseInputStream.getNextTarEntry();
            assertTrue(paxGNUSparseInputStream.canReadEntryData(paxGNUEntry));
            try (TarArchiveInputStream oldGNUSparseInputStream = new TarArchiveInputStream(Files.newInputStream(oldGNUSparseTar.toPath()))) {
                final TarArchiveEntry oldGNUEntry = oldGNUSparseInputStream.getNextTarEntry();
                assertTrue(oldGNUSparseInputStream.canReadEntryData(oldGNUEntry));
                assertArrayEquals(IOUtils.toByteArray(oldGNUSparseInputStream),
                        IOUtils.toByteArray(paxGNUSparseInputStream));
            }
        }
    }

    @Test
    public void testOldGNU() throws Throwable {
        final File file = getFile("oldgnu_sparse.tar");
        try (TarArchiveInputStream tin = new TarArchiveInputStream(Files.newInputStream(file.toPath()))) {
            final TarArchiveEntry ae = tin.getNextTarEntry();
            assertEquals("sparsefile", ae.getName());
            assertTrue(ae.isOldGNUSparse());
            assertTrue(ae.isGNUSparse());
            assertFalse(ae.isPaxGNUSparse());
            assertTrue(tin.canReadEntryData(ae));

            final List<TarArchiveStructSparse> sparseHeaders = ae.getSparseHeaders();
            assertEquals(4, sparseHeaders.size());

            assertEquals(0, sparseHeaders.get(0).getOffset());
            assertEquals(2048, sparseHeaders.get(0).getNumbytes());

            assertEquals(1050624L, sparseHeaders.get(1).getOffset());
            assertEquals(2560, sparseHeaders.get(1).getNumbytes());

            assertEquals(3101184L, sparseHeaders.get(2).getOffset());
            assertEquals(0, sparseHeaders.get(2).getNumbytes());

            assertEquals(0, sparseHeaders.get(3).getOffset());
            assertEquals(0, sparseHeaders.get(3).getNumbytes());

            final List<TarArchiveStructSparse> sparseOrderedHeaders = ae.getOrderedSparseHeaders();
            assertEquals(3, sparseOrderedHeaders.size());

            assertEquals(0, sparseOrderedHeaders.get(0).getOffset());
            assertEquals(2048, sparseOrderedHeaders.get(0).getNumbytes());

            assertEquals(1050624L, sparseOrderedHeaders.get(1).getOffset());
            assertEquals(2560, sparseOrderedHeaders.get(1).getNumbytes());

            assertEquals(3101184L, sparseOrderedHeaders.get(2).getOffset());
            assertEquals(0, sparseOrderedHeaders.get(2).getNumbytes());
        }
    }

    @Test
    public void testPaxGNU() throws Throwable {
        final File file = getFile("pax_gnu_sparse.tar");
        try (TarArchiveInputStream tin = new TarArchiveInputStream(Files.newInputStream(file.toPath()))) {
            assertPaxGNUEntry(tin, "0.0");
            assertPaxGNUEntry(tin, "0.1");
            assertPaxGNUEntry(tin, "1.0");
        }
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    public void testTarFileExtractExtendedOldGNU() throws IOException, InterruptedException {
        final File file = getFile("oldgnu_extended_sparse.tar");
        try (InputStream sparseFileInputStream = extractTarAndGetInputStream(file, "sparse6");
             TarFile tarFile = new TarFile(file)) {
            final TarArchiveEntry ae = tarFile.getEntries().get(0);

            try (InputStream tarInput = tarFile.getInputStream(ae)) {
                assertArrayEquals(IOUtils.toByteArray(tarInput), IOUtils.toByteArray(sparseFileInputStream));
            }

            final List<TarArchiveStructSparse> sparseHeaders = ae.getOrderedSparseHeaders();
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
    @DisabledOnOs(OS.WINDOWS)
    public void testTarFileExtractOldGNU() throws IOException, InterruptedException {
        final File file = getFile("oldgnu_sparse.tar");
        try (final InputStream sparseFileInputStream = extractTarAndGetInputStream(file, "sparsefile");
             final TarFile tarFile = new TarFile(file)) {
            final TarArchiveEntry entry = tarFile.getEntries().get(0);
            try (InputStream tarInput = tarFile.getInputStream(entry)) {
                assertArrayEquals(IOUtils.toByteArray(tarInput), IOUtils.toByteArray(sparseFileInputStream));
            }
        }
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    public void testTarFileExtractPaxGNU() throws IOException, InterruptedException {
        // GNU tar with version 1.28 has some problems reading sparsefile-0.1,
        // so the test should be skipped then
        // TODO : what about the versions lower than 1.28?
        assumeFalse(getTarBinaryHelp().startsWith("tar (GNU tar) 1.28"),
                "This test should be ignored if GNU tar is version 1.28");

        final File file = getFile("pax_gnu_sparse.tar");
        try (final TarFile paxGnu = new TarFile(file)) {
            final List<TarArchiveEntry> entries = paxGnu.getEntries();

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
    @EnabledOnOs(OS.WINDOWS)
    public void testTarFileExtractSparseTarsOnWindows() throws IOException {
        final File oldGNUSparseTar = getFile("oldgnu_sparse.tar");
        final File paxGNUSparseTar = getFile("pax_gnu_sparse.tar");
        try (TarFile paxGnu = new TarFile(paxGNUSparseTar)) {
            final List<TarArchiveEntry> entries = paxGnu.getEntries();

            // compare between old GNU and PAX 0.0
            TarArchiveEntry paxGnuEntry = entries.get(0);
            try (TarFile oldGnu = new TarFile(oldGNUSparseTar)) {
                final TarArchiveEntry oldGnuEntry = oldGnu.getEntries().get(0);
                try (InputStream old = oldGnu.getInputStream(oldGnuEntry);
                     InputStream pax = paxGnu.getInputStream(paxGnuEntry)) {
                    assertArrayEquals(IOUtils.toByteArray(old), IOUtils.toByteArray(pax));
                }
            }

            // compare between old GNU and PAX 0.1
            paxGnuEntry = entries.get(1);
            try (TarFile oldGnu = new TarFile(oldGNUSparseTar)) {
                final TarArchiveEntry oldGnuEntry = oldGnu.getEntries().get(0);
                try (InputStream old = oldGnu.getInputStream(oldGnuEntry);
                     InputStream pax = paxGnu.getInputStream(paxGnuEntry)) {
                    assertArrayEquals(IOUtils.toByteArray(old), IOUtils.toByteArray(pax));
                }
            }

            // compare between old GNU and PAX 1.0
            paxGnuEntry = entries.get(2);
            try (TarFile oldGnu = new TarFile(oldGNUSparseTar)) {
                final TarArchiveEntry oldGnuEntry = oldGnu.getEntries().get(0);
                try (InputStream old = oldGnu.getInputStream(oldGnuEntry);
                     InputStream pax = paxGnu.getInputStream(paxGnuEntry)) {
                    assertArrayEquals(IOUtils.toByteArray(old), IOUtils.toByteArray(pax));
                }
            }
        }
    }

    @Test
    public void testTarFileOldGNU() throws Throwable {
        final File file = getFile("oldgnu_sparse.tar");
        try (final TarFile tarFile = new TarFile(file)) {
            final TarArchiveEntry ae = tarFile.getEntries().get(0);
            assertEquals("sparsefile", ae.getName());
            assertTrue(ae.isOldGNUSparse());
            assertTrue(ae.isGNUSparse());
            assertFalse(ae.isPaxGNUSparse());

            final List<TarArchiveStructSparse> sparseHeaders = ae.getSparseHeaders();
            assertEquals(4, sparseHeaders.size());

            assertEquals(0, sparseHeaders.get(0).getOffset());
            assertEquals(2048, sparseHeaders.get(0).getNumbytes());

            assertEquals(1050624L, sparseHeaders.get(1).getOffset());
            assertEquals(2560, sparseHeaders.get(1).getNumbytes());

            assertEquals(3101184L, sparseHeaders.get(2).getOffset());
            assertEquals(0, sparseHeaders.get(2).getNumbytes());

            assertEquals(0, sparseHeaders.get(3).getOffset());
            assertEquals(0, sparseHeaders.get(3).getNumbytes());

            final List<TarArchiveStructSparse> sparseOrderedHeaders = ae.getOrderedSparseHeaders();
            assertEquals(3, sparseOrderedHeaders.size());

            assertEquals(0, sparseOrderedHeaders.get(0).getOffset());
            assertEquals(2048, sparseOrderedHeaders.get(0).getNumbytes());

            assertEquals(1050624L, sparseOrderedHeaders.get(1).getOffset());
            assertEquals(2560, sparseOrderedHeaders.get(1).getNumbytes());

            assertEquals(3101184L, sparseOrderedHeaders.get(2).getOffset());
            assertEquals(0, sparseOrderedHeaders.get(2).getNumbytes());
        }
    }

    @Test
    public void testTarFilePaxGNU() throws IOException {
        final File file = getFile("pax_gnu_sparse.tar");
        try (final TarFile tarFile = new TarFile(file)) {
            final List<TarArchiveEntry> entries = tarFile.getEntries();
            assertPaxGNUEntry(entries.get(0), "0.0");
            assertPaxGNUEntry(entries.get(1), "0.1");
            assertPaxGNUEntry(entries.get(2), "1.0");
        }
    }
}

