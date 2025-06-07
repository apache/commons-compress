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

package org.apache.commons.compress.archivers.ar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.utils.ArchiveUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class ArArchiveInputStreamTest extends AbstractTest {

    private void checkLongNameEntry(final String archive) throws Exception {
        try (InputStream fis = newInputStream(archive);
                ArArchiveInputStream s = new ArArchiveInputStream(new BufferedInputStream(fis))) {
            ArchiveEntry e = s.getNextEntry();
            assertEquals("this_is_a_long_file_name.txt", e.getName());
            assertEquals(14, e.getSize());
            final byte[] hello = new byte[14];
            s.read(hello);
            assertEquals("Hello, world!\n", ArchiveUtils.toAsciiString(hello));
            e = s.getNextEntry();
            assertEquals("this_is_a_long_file_name_as_well.txt", e.getName());
            assertEquals(4, e.getSize());
            final byte[] bye = new byte[4];
            s.read(bye);
            assertEquals("Bye\n", ArchiveUtils.toAsciiString(bye));
            assertNull(s.getNextEntry());
        }
    }

    @Test
    void testCantReadAfterClose() throws Exception {
        try (InputStream in = newInputStream("bla.ar");
                ArArchiveInputStream archive = new ArArchiveInputStream(in)) {
            archive.close();
            assertThrows(IllegalStateException.class, () -> archive.read());
        }
    }

    @Test
    void testCantReadWithoutOpeningAnEntry() throws Exception {
        try (InputStream in = newInputStream("bla.ar");
                ArArchiveInputStream archive = new ArArchiveInputStream(in)) {
            assertThrows(IllegalStateException.class, () -> archive.read());
        }
    }

    @Test
    void testCompress661() throws IOException {
        testCompress661(false);
        testCompress661(true);
    }

    private void testCompress661(final boolean checkMarkReadReset) throws IOException {
        try (InputStream in = newInputStream("org/apache/commons/compress/COMPRESS-661/testARofText.ar");
                ArArchiveInputStream archive = new ArArchiveInputStream(new BufferedInputStream(in))) {
            assertNotNull(archive.getNextEntry());
            if (checkMarkReadReset && archive.markSupported()) {
                // mark() shouldn't be supported, but if it would be,
                // mark+read+reset should not do any harm.
                archive.mark(10);
                archive.read(new byte[10]);
                archive.reset();
            }
            final byte[] ba = IOUtils.toByteArray(archive);
            assertEquals("Test d'indexation de Txt\nhttp://www.apache.org\n", new String(ba));
            assertEquals(-1, archive.read());
            assertEquals(-1, archive.read());
            assertNull(archive.getNextEntry());
        }
    }

    @Test
    void testInvalidBadTableLength() throws Exception {
        try (InputStream in = newInputStream("org/apache/commons/compress/ar/number_parsing/bad_table_length_gnu-fail.ar");
                ArArchiveInputStream archive = new ArArchiveInputStream(in)) {
            assertThrows(IOException.class, archive::getNextEntry);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "bad_long_namelen_bsd-fail.ar", "bad_long_namelen_gnu1-fail.ar", "bad_long_namelen_gnu2-fail.ar", "bad_long_namelen_gnu3-fail.ar",
            "bad_table_length_gnu-fail.ar" })
    void testInvalidLongNameLength(final String testFileName) throws Exception {
        try (InputStream in = newInputStream("org/apache/commons/compress/ar/number_parsing/" + testFileName);
                ArArchiveInputStream archive = new ArArchiveInputStream(in)) {
            assertThrows(IOException.class, archive::getNextEntry);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "bad_group-fail.ar", "bad_length-fail.ar", "bad_modified-fail.ar", "bad_user-fail.ar" })
    void testInvalidNumericFields(final String testFileName) throws Exception {
        try (InputStream in = newInputStream("org/apache/commons/compress/ar/number_parsing/" + testFileName);
                ArArchiveInputStream archive = new ArArchiveInputStream(in)) {
            assertThrows(IOException.class, archive::getNextEntry);
        }
    }

    @Test
    void testMultiByteReadConsistentlyReturnsMinusOneAtEof() throws Exception {
        final byte[] buf = new byte[2];
        try (InputStream in = newInputStream("bla.ar");
                ArArchiveInputStream archive = new ArArchiveInputStream(in)) {
            assertNotNull(archive.getNextEntry());
            IOUtils.toByteArray(archive);
            assertEquals(-1, archive.read(buf));
            assertEquals(-1, archive.read(buf));
        }
    }

    @Test
    void testReadLongNamesBSD() throws Exception {
        checkLongNameEntry("longfile_bsd.ar");
    }

    @Test
    void testReadLongNamesGNU() throws Exception {
        checkLongNameEntry("longfile_gnu.ar");
    }

    @Test
    void testSimpleInputStream() throws IOException {
        try (InputStream fileInputStream = newInputStream("bla.ar");
                // This default implementation of InputStream.available() always returns zero,
                // and there are many streams in practice where the total length of the stream is not known.
                InputStream simpleInputStream = new InputStream() {
                    @Override
                    public int read() throws IOException {
                        return fileInputStream.read();
                    }
                }) {
            try (ArArchiveInputStream archiveInputStream = new ArArchiveInputStream(simpleInputStream)) {
                final ArArchiveEntry entry1 = archiveInputStream.getNextEntry();
                assertNotNull(entry1);
                assertEquals("test1.xml", entry1.getName());
                assertEquals(610L, entry1.getLength());
                final ArArchiveEntry entry2 = archiveInputStream.getNextEntry();
                assertEquals("test2.xml", entry2.getName());
                assertEquals(82L, entry2.getLength());
                assertNull(archiveInputStream.getNextEntry());
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Test
    void testSimpleInputStreamDeprecated() throws IOException {
        try (InputStream fileInputStream = newInputStream("bla.ar");
                // This default implementation of InputStream.available() always returns zero,
                // and there are many streams in practice where the total length of the stream is not known.
                InputStream simpleInputStream = new InputStream() {
                    @Override
                    public int read() throws IOException {
                        return fileInputStream.read();
                    }
                }) {
            try (ArArchiveInputStream archiveInputStream = new ArArchiveInputStream(simpleInputStream)) {
                final ArArchiveEntry entry1 = archiveInputStream.getNextArEntry();
                assertNotNull(entry1);
                assertEquals("test1.xml", entry1.getName());
                assertEquals(610L, entry1.getLength());
                final ArArchiveEntry entry2 = archiveInputStream.getNextArEntry();
                assertEquals("test2.xml", entry2.getName());
                assertEquals(82L, entry2.getLength());
                assertNull(archiveInputStream.getNextArEntry());
            }
        }
    }

    @Test
    void testSingleByteReadConsistentlyReturnsMinusOneAtEof() throws Exception {
        try (InputStream in = newInputStream("bla.ar");
                ArArchiveInputStream archive = new ArArchiveInputStream(in)) {
            assertNotNull(archive.getNextEntry());
            IOUtils.toByteArray(archive);
            assertEquals(-1, archive.read());
            assertEquals(-1, archive.read());
        }
    }

}
