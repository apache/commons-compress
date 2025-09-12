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
package org.apache.commons.compress.archivers.dump;

import static org.apache.commons.compress.archivers.dump.DumpArchiveConstants.TP_SIZE;
import static org.apache.commons.compress.archivers.dump.DumpArchiveTestFactory.createSegment;
import static org.apache.commons.compress.archivers.dump.DumpArchiveTestFactory.createSummary;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.Timeout.ThreadMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DumpArchiveInputStreamTest extends AbstractTest {

    @ParameterizedTest
    @ValueSource(ints = {1, 10, 32, 1024})
    void checkSupportedRecordSizes(final int ntrec) throws Exception {
        try (DumpArchiveInputStream dump = DumpArchiveInputStream.builder()
                .setByteArray(createArchive(ntrec))
                .get()) {
            final DumpArchiveSummary summary = dump.getSummary();
            assertNotNull(summary);
            assertEquals(ntrec, summary.getNTRec());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {Integer.MIN_VALUE, -1, 0, 1025, Integer.MAX_VALUE})
    void checkUnsupportedRecordSizes(final int ntrec) throws Exception {
        final ArchiveException ex = assertThrows(ArchiveException.class, () -> DumpArchiveInputStream.builder()
                .setByteArray(createArchive(ntrec))
                .get());
        assertInstanceOf(ArchiveException.class, ex.getCause());
        assertTrue(ex.getMessage().contains(Integer.toString(ntrec)), "message should contain the invalid ntrec value");
    }

    private byte[] createArchive(final int ntrec) {
        final byte[] dump = new byte[1024 * TP_SIZE];
        int offset = 0;
        // summary
        System.arraycopy(createSummary(ntrec), 0, dump, offset, TP_SIZE);
        offset += TP_SIZE;
        // CLRI segment
        System.arraycopy(createSegment(DumpArchiveConstants.SEGMENT_TYPE.CLRI), 0, dump, offset, TP_SIZE);
        offset += TP_SIZE;
        // BITS segment
        System.arraycopy(createSegment(DumpArchiveConstants.SEGMENT_TYPE.BITS), 0, dump, offset, TP_SIZE);
        return dump;
    }

    @SuppressWarnings("deprecation")
    @Test
    void testConsumesArchiveCompletely() throws Exception {
        try (InputStream is = DumpArchiveInputStreamTest.class.getResourceAsStream("/archive_with_trailer.dump");
                DumpArchiveInputStream dump =
                        DumpArchiveInputStream.builder().setInputStream(is).get()) {
            while (dump.getNextDumpEntry() != null) {
                // just consume the archive
            }
            final byte[] expected = { 'H', 'e', 'l', 'l', 'o', ',', ' ', 'w', 'o', 'r', 'l', 'd', '!', '\n' };
            final byte[] actual = new byte[expected.length];
            is.read(actual);
            assertArrayEquals(expected, actual);
        }
    }

    @Test
    void testDirectoryNullBytes() throws Exception {
        try (DumpArchiveInputStream archive = DumpArchiveInputStream.builder()
                .setURI(getURI("org/apache/commons/compress/dump/directory_null_bytes-fail.dump"))
                .get()) {
            assertThrows(InvalidFormatException.class, archive::getNextEntry);
        }
    }

    @Test
    void testGetNextEntry() throws IOException {
        try (DumpArchiveInputStream inputStream = DumpArchiveInputStream.builder()
                .setURI(getURI("org/apache/commons/compress/dump/getNextEntry.bin"))
                .get()) {
            assertThrows(ArchiveException.class, inputStream::getNextEntry);
        }
    }

    @Test
    void testInvalidCompressType() {
        final ArchiveException ex = assertThrows(ArchiveException.class, () -> DumpArchiveInputStream.builder()
                .setURI(getURI("org/apache/commons/compress/dump/invalid_compression_type-fail.dump"))
                .get()
                .close());
        assertInstanceOf(UnsupportedCompressionAlgorithmException.class, ex.getCause());
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS, threadMode = ThreadMode.SEPARATE_THREAD)
    void testLoopingInodes() throws Exception {
        try (DumpArchiveInputStream archive = DumpArchiveInputStream.builder()
                .setURI(getURI("org/apache/commons/compress/dump/looping_inodes-fail.dump"))
                .get()) {
            archive.getNextEntry();
            assertThrows(DumpArchiveException.class, archive::getNextEntry);
        }
    }

    @Test
    void testMultiByteReadConsistentlyReturnsMinusOneAtEof() throws Exception {
        final byte[] buf = new byte[2];
        try (DumpArchiveInputStream archive =
                DumpArchiveInputStream.builder().setURI(getURI("bla.dump")).get()) {
            assertNotNull(archive.getNextEntry());
            IOUtils.toByteArray(archive);
            assertEquals(-1, archive.read(buf));
            assertEquals(-1, archive.read(buf));
        }
    }

    @Test
    void testNotADumpArchive() {
        final ArchiveException ex = assertThrows(
                ArchiveException.class,
                () -> DumpArchiveInputStream.builder()
                        .setURI(getURI("bla.zip"))
                        .get()
                        .close(),
                "expected an exception");
        assertTrue(ex.getCause() instanceof ShortFileException);
    }

    @Test
    void testNotADumpArchiveButBigEnough() throws Exception {
        final ArchiveException ex = assertThrows(
                ArchiveException.class,
                () -> DumpArchiveInputStream.builder()
                        .setURI(getURI("zip64support.tar.bz2"))
                        .get()
                        .close(),
                "expected an exception");
        assertInstanceOf(UnrecognizedFormatException.class, ex.getCause());
    }

    @Test
    void testRecLenZeroLongExecution() throws Exception {
        try (DumpArchiveInputStream archive = DumpArchiveInputStream.builder()
                .setURI(getURI("org/apache/commons/compress/dump/reclen_zero-fail.dump"))
                .get()) {
            assertTimeoutPreemptively(Duration.ofSeconds(20), () -> {
                assertThrows(DumpArchiveException.class, archive::getNextEntry);
            });
        }
    }

    @Test
    void testSingleByteReadConsistentlyReturnsMinusOneAtEof() throws Exception {
        try (DumpArchiveInputStream archive =
                DumpArchiveInputStream.builder().setURI(getURI("bla.dump")).get()) {
            assertNotNull(archive.getNextEntry());
            IOUtils.toByteArray(archive);
            assertEquals(-1, archive.read());
            assertEquals(-1, archive.read());
        }
    }
}
