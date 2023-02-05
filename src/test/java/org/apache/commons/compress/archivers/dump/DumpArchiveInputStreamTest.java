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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.Test;

public class DumpArchiveInputStreamTest extends AbstractTestCase {

    @Test
    public void multiByteReadConsistentlyReturnsMinusOneAtEof() throws Exception {
        final byte[] buf = new byte[2];
        try (InputStream in = newInputStream("bla.dump");
             DumpArchiveInputStream archive = new DumpArchiveInputStream(in)) {
            final ArchiveEntry e = archive.getNextEntry();
            IOUtils.toByteArray(archive);
            assertEquals(-1, archive.read(buf));
            assertEquals(-1, archive.read(buf));
        }
    }

    @Test
    public void singleByteReadConsistentlyReturnsMinusOneAtEof() throws Exception {
        try (InputStream in = newInputStream("bla.dump");
             DumpArchiveInputStream archive = new DumpArchiveInputStream(in)) {
            final ArchiveEntry e = archive.getNextEntry();
            IOUtils.toByteArray(archive);
            assertEquals(-1, archive.read());
            assertEquals(-1, archive.read());
        }
    }

    @Test
    public void testConsumesArchiveCompletely() throws Exception {
        try (final InputStream is = DumpArchiveInputStreamTest.class.getResourceAsStream("/archive_with_trailer.dump");
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
    public void testNotADumpArchive() throws Exception {
        try (InputStream is = newInputStream("bla.zip")) {
            final ArchiveException ex = assertThrows(ArchiveException.class, () -> new DumpArchiveInputStream(is).close(),
                    "expected an exception");
            assertTrue(ex.getCause() instanceof ShortFileException);
        }
    }

    @Test
    public void testNotADumpArchiveButBigEnough() throws Exception {
        try (InputStream is = newInputStream("zip64support.tar.bz2")) {
            final ArchiveException ex = assertThrows(ArchiveException.class, () -> new DumpArchiveInputStream(is).close(),
                    "expected an exception");
            assertInstanceOf(UnrecognizedFormatException.class, ex.getCause());
        }
    }

}
