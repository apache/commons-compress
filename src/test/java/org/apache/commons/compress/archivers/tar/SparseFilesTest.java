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

package org.apache.commons.compress.archivers.tar;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.archivers.TestArchiveGenerator;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SparseFilesTest extends AbstractTest {

    @TempDir
    private static Path tempDir;

    @BeforeAll
    static void setupAll() throws IOException {
        TestArchiveGenerator.createSparseFileTestCases(tempDir);
    }

    private void assertPaxGNUEntry(final TarArchiveEntry entry, final String suffix) {
        assertEquals("sparsefile-" + suffix, entry.getName());
        assertEquals(TarConstants.LF_NORMAL, entry.getLinkFlag());
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
        assertEquals(TarConstants.LF_NORMAL, ae.getLinkFlag());
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
        final ProcessBuilder pb = new ProcessBuilder("tar", "-xf", tarFile.getPath(), "-C", tempResultDir.getPath());
        pb.redirectErrorStream(true);
        final Process process = pb.start();
        // wait until the extract finishes
        try (InputStream inputStream = process.getInputStream()) {
            assertEquals(0, process.waitFor(), new String(IOUtils.toByteArray(inputStream)));
        }
        for (final File file : tempResultDir.listFiles()) {
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
        try (InputStream inputStream = process.getInputStream()) {
            return new String(IOUtils.toByteArray(inputStream));
        }
    }

    @Test
    void testCompareTarArchiveInputStreamWithTarFile() throws IOException {
        final Path file = getPath("oldgnu_sparse.tar");
        try (TarArchiveInputStream tarIn = TarArchiveInputStream.builder().setPath(file).get();
                TarFile tarFile = new TarFile(file)) {
            assertNotNull(tarIn.getNextTarEntry());
            try (InputStream inputStream = tarFile.getInputStream(tarFile.getEntries().get(0))) {
                assertArrayEquals(IOUtils.toByteArray(tarIn), IOUtils.toByteArray(inputStream));
            }
        }
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testExtractExtendedOldGNU() throws IOException, InterruptedException {
        final File file = getFile("oldgnu_extended_sparse.tar");
        try (InputStream sparseFileInputStream = extractTarAndGetInputStream(file, "sparse6");
                TarArchiveInputStream tin = TarArchiveInputStream.builder().setFile(file).get()) {
            final TarArchiveEntry ae = tin.getNextTarEntry();
            assertTrue(tin.canReadEntryData(ae));

            assertArrayEquals(IOUtils.toByteArray(tin), IOUtils.toByteArray(sparseFileInputStream));

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
    void testExtractOldGNU() throws IOException, InterruptedException {
        try {
            final File file = getFile("oldgnu_sparse.tar");
            try (InputStream sparseFileInputStream = extractTarAndGetInputStream(file, "sparsefile");
                    TarArchiveInputStream tin = TarArchiveInputStream.builder().setFile(file).get()) {
                final TarArchiveEntry entry = tin.getNextTarEntry();
                assertTrue(tin.canReadEntryData(entry));
                assertArrayEquals(IOUtils.toByteArray(tin), IOUtils.toByteArray(sparseFileInputStream));
            }
        } catch (RuntimeException | IOException ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testExtractPaxGNU() throws IOException, InterruptedException {
        // GNU tar with version 1.28 has some problems reading sparsefile-0.1,
        // so the test should be skipped then
        // TODO : what about the versions lower than 1.28?
        assumeFalse(getTarBinaryHelp().startsWith("tar (GNU tar) 1.28"), "This test should be ignored if GNU tar is version 1.28");

        final File file = getFile("pax_gnu_sparse.tar");
        try (TarArchiveInputStream tin = TarArchiveInputStream.builder().setFile(file).get()) {

            TarArchiveEntry paxGNUEntry = tin.getNextTarEntry();
            assertTrue(tin.canReadEntryData(paxGNUEntry));
            try (InputStream sparseFileInputStream = extractTarAndGetInputStream(file, "sparsefile-0.0")) {
                assertArrayEquals(IOUtils.toByteArray(tin), IOUtils.toByteArray(sparseFileInputStream));
            }

            paxGNUEntry = tin.getNextTarEntry();
            assertTrue(tin.canReadEntryData(paxGNUEntry));
            try (InputStream sparseFileInputStream = extractTarAndGetInputStream(file, "sparsefile-0.1")) {
                assertArrayEquals(IOUtils.toByteArray(tin), IOUtils.toByteArray(sparseFileInputStream));
            }

            paxGNUEntry = tin.getNextTarEntry();
            assertTrue(tin.canReadEntryData(paxGNUEntry));
            try (InputStream sparseFileInputStream = extractTarAndGetInputStream(file, "sparsefile-1.0")) {
                assertArrayEquals(IOUtils.toByteArray(tin), IOUtils.toByteArray(sparseFileInputStream));
            }
        }
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testExtractSparseTarsOnWindows() throws IOException {
        final File oldGNUSparseTar = getFile("oldgnu_sparse.tar");
        final File paxGNUSparseTar = getFile("pax_gnu_sparse.tar");
        try (TarArchiveInputStream paxGNUSparseInputStream = TarArchiveInputStream.builder().setFile(paxGNUSparseTar).get()) {

            // compare between old GNU and PAX 0.0
            TarArchiveEntry paxGNUEntry = paxGNUSparseInputStream.getNextTarEntry();
            assertTrue(paxGNUSparseInputStream.canReadEntryData(paxGNUEntry));
            try (TarArchiveInputStream oldGNUSparseInputStream = TarArchiveInputStream.builder().setFile(oldGNUSparseTar).get()) {
                final TarArchiveEntry oldGNUEntry = oldGNUSparseInputStream.getNextTarEntry();
                assertTrue(oldGNUSparseInputStream.canReadEntryData(oldGNUEntry));
                assertArrayEquals(IOUtils.toByteArray(oldGNUSparseInputStream), IOUtils.toByteArray(paxGNUSparseInputStream));
            }

            // compare between old GNU and PAX 0.1
            paxGNUEntry = paxGNUSparseInputStream.getNextTarEntry();
            assertTrue(paxGNUSparseInputStream.canReadEntryData(paxGNUEntry));
            try (TarArchiveInputStream oldGNUSparseInputStream = TarArchiveInputStream.builder().setFile(oldGNUSparseTar).get()) {
                final TarArchiveEntry oldGNUEntry = oldGNUSparseInputStream.getNextTarEntry();
                assertTrue(oldGNUSparseInputStream.canReadEntryData(oldGNUEntry));
                assertArrayEquals(IOUtils.toByteArray(oldGNUSparseInputStream), IOUtils.toByteArray(paxGNUSparseInputStream));
            }

            // compare between old GNU and PAX 1.0
            paxGNUEntry = paxGNUSparseInputStream.getNextTarEntry();
            assertTrue(paxGNUSparseInputStream.canReadEntryData(paxGNUEntry));
            try (TarArchiveInputStream oldGNUSparseInputStream = TarArchiveInputStream.builder().setFile(oldGNUSparseTar).get()) {
                final TarArchiveEntry oldGNUEntry = oldGNUSparseInputStream.getNextTarEntry();
                assertTrue(oldGNUSparseInputStream.canReadEntryData(oldGNUEntry));
                assertArrayEquals(IOUtils.toByteArray(oldGNUSparseInputStream), IOUtils.toByteArray(paxGNUSparseInputStream));
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"old-gnu-sparse.tar" , "gnu-sparse-00.tar", "gnu-sparse-01.tar", "gnu-sparse-1.tar"})
    void testMaximallyFragmentedTarFile(final String fileName) throws IOException {
        final int expectedSize = 8192;
        try (TarFile input = TarFile.builder().setPath(tempDir.resolve(fileName)).get()) {
            final List<TarArchiveEntry> entries = input.getEntries();
            assertEquals(1, entries.size());
            final TarArchiveEntry entry = entries.get(0);
            assertNotNull(entry);
            assertEquals("sparse-file.txt", entry.getName());

            try (InputStream inputStream = input.getInputStream(entry)) {
                // read the expected amount of data
                final byte[] content = new byte[expectedSize];
                assertEquals(expectedSize, inputStream.read(content));
                // verify that the stream is at EOF
                assertEquals(IOUtils.EOF, inputStream.read());
                // check content
                for (int i = 0; i < content.length; i++) {
                    assertEquals((byte) (i % 256), content[i], "at index " + i);
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"old-gnu-sparse.tar", "gnu-sparse-00.tar", "gnu-sparse-01.tar", "gnu-sparse-1.tar"})
    void testMaximallyFragmentedTarStream(final String fileName) throws IOException {
        final int expectedSize = 8192;
        try (TarArchiveInputStream input = TarArchiveInputStream.builder().setPath(tempDir.resolve(fileName)).get()) {
            final TarArchiveEntry entry = input.getNextEntry();
            assertNotNull(entry);
            assertEquals("sparse-file.txt", entry.getName());
            // read the expected amount of data
            final byte[] content = new byte[expectedSize];
            assertEquals(expectedSize, input.read(content));
            // verify that the stream is at EOF
            assertEquals(IOUtils.EOF, input.read());
            // check content
            for (int i = 0; i < content.length; i++) {
                assertEquals((byte) (i % 256), content[i], "at index " + i);
            }
            // check that there are no more entries
            assertNull(input.getNextEntry());
        }
    }

    @Test
    void testOldGNU() throws Throwable {
        try (TarArchiveInputStream tin = TarArchiveInputStream.builder()
                .setURI(getURI("oldgnu_sparse.tar"))
                .get()) {
            final TarArchiveEntry ae = tin.getNextTarEntry();
            assertEquals("sparsefile", ae.getName());
            assertEquals(TarConstants.LF_GNUTYPE_SPARSE, ae.getLinkFlag());
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
    void testPaxGNU() throws Throwable {
        try (TarArchiveInputStream tin = TarArchiveInputStream.builder()
                .setURI(getURI("pax_gnu_sparse.tar"))
                .get()) {
            assertPaxGNUEntry(tin, "0.0");
            assertPaxGNUEntry(tin, "0.1");
            assertPaxGNUEntry(tin, "1.0");
        }
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testTarFileExtractExtendedOldGNU() throws IOException, InterruptedException {
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
    void testTarFileExtractOldGNU() throws IOException, InterruptedException {
        final File file = getFile("oldgnu_sparse.tar");
        try (InputStream sparseFileInputStream = extractTarAndGetInputStream(file, "sparsefile");
                TarFile tarFile = new TarFile(file)) {
            final TarArchiveEntry entry = tarFile.getEntries().get(0);
            try (InputStream tarInput = tarFile.getInputStream(entry)) {
                assertArrayEquals(IOUtils.toByteArray(tarInput), IOUtils.toByteArray(sparseFileInputStream));
            }
        }
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testTarFileExtractPaxGNU() throws IOException, InterruptedException {
        // GNU tar with version 1.28 has some problems reading sparsefile-0.1,
        // so the test should be skipped then
        // TODO : what about the versions lower than 1.28?
        assumeFalse(getTarBinaryHelp().startsWith("tar (GNU tar) 1.28"), "This test should be ignored if GNU tar is version 1.28");

        final File file = getFile("pax_gnu_sparse.tar");
        try (TarFile paxGnu = new TarFile(file)) {
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
    void testTarFileExtractSparseTarsOnWindows() throws IOException {
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
    void testTarFileOldGNU() throws Throwable {
        final File file = getFile("oldgnu_sparse.tar");
        try (TarFile tarFile = new TarFile(file)) {
            final TarArchiveEntry ae = tarFile.getEntries().get(0);
            assertEquals("sparsefile", ae.getName());
            assertEquals(TarConstants.LF_GNUTYPE_SPARSE, ae.getLinkFlag());
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
    void testTarFilePaxGNU() throws IOException {
        final File file = getFile("pax_gnu_sparse.tar");
        try (TarFile tarFile = new TarFile(file)) {
            final List<TarArchiveEntry> entries = tarFile.getEntries();
            assertPaxGNUEntry(entries.get(0), "0.0");
            assertPaxGNUEntry(entries.get(1), "0.1");
            assertPaxGNUEntry(entries.get(2), "1.0");
        }
    }
}
