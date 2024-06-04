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
package org.apache.commons.compress.archivers.dump;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.Timeout.ThreadMode;

public class DumpArchiveInputStreamTest extends AbstractTest {

    @SuppressWarnings("deprecation")
    @Test
    public void testConsumesArchiveCompletely() throws Exception {
        try (InputStream is = DumpArchiveInputStreamTest.class.getResourceAsStream("/archive_with_trailer.dump");
                DumpArchiveInputStream dump = new DumpArchiveInputStream(is)) {
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
    public void testDirectoryNullBytes() throws Exception {
        try (InputStream is = newInputStream("org/apache/commons/compress/dump/directory_null_bytes-fail.dump");
                DumpArchiveInputStream archive = new DumpArchiveInputStream(is)) {
            assertThrows(InvalidFormatException.class, archive::getNextEntry);
        }
    }

    @Test
    public void testInvalidCompressType() throws Exception {
        try (InputStream is = newInputStream("org/apache/commons/compress/dump/invalid_compression_type-fail.dump")) {
            final ArchiveException ex = assertThrows(ArchiveException.class, () -> new DumpArchiveInputStream(is).close());
            assertInstanceOf(UnsupportedCompressionAlgorithmException.class, ex.getCause());
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS, threadMode = ThreadMode.SEPARATE_THREAD)
    public void testLoopingInodes() throws Exception {
        try (InputStream is = newInputStream("org/apache/commons/compress/dump/looping_inodes-fail.dump");
                DumpArchiveInputStream archive = new DumpArchiveInputStream(is)) {
            archive.getNextEntry();
            assertThrows(DumpArchiveException.class, archive::getNextEntry);
        }
    }

    @Test
    public void testMultiByteReadConsistentlyReturnsMinusOneAtEof() throws Exception {
        final byte[] buf = new byte[2];
        try (InputStream in = newInputStream("bla.dump");
                DumpArchiveInputStream archive = new DumpArchiveInputStream(in)) {
            assertNotNull(archive.getNextEntry());
            IOUtils.toByteArray(archive);
            assertEquals(-1, archive.read(buf));
            assertEquals(-1, archive.read(buf));
        }
    }

    @Test
    public void testNotADumpArchive() throws Exception {
        try (InputStream is = newInputStream("bla.zip")) {
            final ArchiveException ex = assertThrows(ArchiveException.class, () -> new DumpArchiveInputStream(is).close(), "expected an exception");
            assertTrue(ex.getCause() instanceof ShortFileException);
        }
    }

    @Test
    public void testNotADumpArchiveButBigEnough() throws Exception {
        try (InputStream is = newInputStream("zip64support.tar.bz2")) {
            final ArchiveException ex = assertThrows(ArchiveException.class, () -> new DumpArchiveInputStream(is).close(), "expected an exception");
            assertInstanceOf(UnrecognizedFormatException.class, ex.getCause());
        }
    }

    @Test
    public void testRecLenZeroLongExecution() throws Exception {
        try (InputStream is = newInputStream("org/apache/commons/compress/dump/reclen_zero-fail.dump");
                DumpArchiveInputStream archive = new DumpArchiveInputStream(is)) {
            assertTimeoutPreemptively(Duration.ofSeconds(20), () -> {
                assertThrows(DumpArchiveException.class, archive::getNextEntry);
            });
        }
    }

    @Test
    public void testSingleByteReadConsistentlyReturnsMinusOneAtEof() throws Exception {
        try (InputStream in = newInputStream("bla.dump");
                DumpArchiveInputStream archive = new DumpArchiveInputStream(in)) {
            assertNotNull(archive.getNextEntry());
            IOUtils.toByteArray(archive);
            assertEquals(-1, archive.read());
            assertEquals(-1, archive.read());
        }
    }

}
