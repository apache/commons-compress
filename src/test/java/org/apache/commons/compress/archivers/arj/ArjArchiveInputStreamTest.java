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

package org.apache.commons.compress.archivers.arj;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Calendar;
import java.util.TimeZone;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link ArjArchiveInputStream}.
 */
class ArjArchiveInputStreamTest extends AbstractTest {

    private void assertArjArchiveEntry(final ArjArchiveEntry entry) {
        assertNotNull(entry.getName());
        assertNotNull(entry.getLastModifiedDate());
        assertDoesNotThrow(entry::getHostOs);
        assertDoesNotThrow(entry::getMethod);
        assertDoesNotThrow(entry::getMode);
        assertDoesNotThrow(entry::getSize);
        assertDoesNotThrow(entry::getUnixMode);
        assertDoesNotThrow(entry::isDirectory);
        assertDoesNotThrow(entry::isHostOsUnix);
        assertDoesNotThrow(entry::hashCode);
        assertDoesNotThrow(entry::toString);
        assertDoesNotThrow(() -> entry.resolveIn(getTempDirPath()));
    }

    @SuppressWarnings("deprecation")
    private void assertArjArchiveInputStream(final ArjArchiveInputStream archive) {
        assertDoesNotThrow(archive::available);
        assertDoesNotThrow(archive::getArchiveComment);
        assertDoesNotThrow(archive::getArchiveName);
        assertDoesNotThrow(archive::getBytesRead);
        assertDoesNotThrow(archive::getCharset);
        assertDoesNotThrow(archive::getCount);
        assertDoesNotThrow(archive::hashCode);
        assertDoesNotThrow(archive::markSupported);
    }

    private void assertForEach(final ArjArchiveInputStream archive) throws IOException {
        assertArjArchiveInputStream(archive);
        archive.forEach(entry -> {
            assertArjArchiveEntry(entry);
            // inside the loop, just in case.
            assertArjArchiveInputStream(archive);
        });
    }

    @Test
    void testFirstHeaderSizeSetToZero() {
        final ArchiveException ex = assertThrows(ArchiveException.class, () -> {
            try (ArjArchiveInputStream archive = ArjArchiveInputStream.builder()
                    .setURI(getURI("org/apache/commons/compress/arj/zero_sized_headers-fail.arj"))
                    .get()) {
                // Do nothing, ArchiveException already thrown
                fail("ArchiveException not thrown.");
            }
        });
        assertTrue(ex.getCause() instanceof IOException);
    }

    @Test
    void testForEach() throws Exception {
        final StringBuilder expected = new StringBuilder();
        expected.append("test1.xml<?xml version=\"1.0\"?>\n");
        expected.append("<empty/>test2.xml<?xml version=\"1.0\"?>\n");
        expected.append("<empty/>\n");
        final StringBuilder result = new StringBuilder();
        try (ArjArchiveInputStream in = ArjArchiveInputStream.builder().setURI(getURI("bla.arj")).get()) {
            in.forEach(entry -> {
                result.append(entry.getName());
                int tmp;
                // read() one at a time
                while ((tmp = in.read()) != -1) {
                    result.append((char) tmp);
                }
                assertFalse(entry.isDirectory());
                assertArjArchiveEntry(entry);
            });
        }
        assertEquals(expected.toString(), result.toString());
    }

    @Test
    void testGetNextEntry() throws Exception {
        final StringBuilder expected = new StringBuilder();
        expected.append("test1.xml<?xml version=\"1.0\"?>\n");
        expected.append("<empty/>test2.xml<?xml version=\"1.0\"?>\n");
        expected.append("<empty/>\n");
        final StringBuilder result = new StringBuilder();
        try (ArjArchiveInputStream in = ArjArchiveInputStream.builder().setURI(getURI("bla.arj")).get()) {
            ArjArchiveEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                result.append(entry.getName());
                int tmp;
                // read() one at a time
                while ((tmp = in.read()) != -1) {
                    result.append((char) tmp);
                }
                assertFalse(entry.isDirectory());
                assertArjArchiveEntry(entry);
            }
        }
        assertEquals(expected.toString(), result.toString());
    }

    @Test
    void testMultiByteReadConsistentlyReturnsMinusOneAtEof() throws Exception {
        final byte[] buf = new byte[2];
        try (ArjArchiveInputStream archive = ArjArchiveInputStream.builder().setURI(getURI("bla.arj")).get()) {
            assertNotNull(archive.getNextEntry());
            IOUtils.toByteArray(archive);
            assertEquals(-1, archive.read(buf));
            assertEquals(-1, archive.read(buf));
            assertForEach(archive);
        }
    }

    @Test
    void testRead() throws Exception {
        final StringBuilder expected = new StringBuilder();
        expected.append("test1.xml<?xml version=\"1.0\"?>\n");
        expected.append("<empty/>test2.xml<?xml version=\"1.0\"?>\n");
        expected.append("<empty/>\n");
        final Charset charset = Charset.defaultCharset();
        try (ByteArrayOutputStream result = new ByteArrayOutputStream();
                ArjArchiveInputStream in = ArjArchiveInputStream.builder().setURI(getURI("bla.arj")).get()) {
            ArjArchiveEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                result.write(entry.getName().getBytes(charset));
                int tmp;
                // read() one at a time
                while ((tmp = in.read()) != -1) {
                    result.write(tmp);
                }
                assertFalse(entry.isDirectory());
                assertArjArchiveEntry(entry);
            }
            result.flush();
            assertEquals(expected.toString(), result.toString(charset));
        }
    }

    @Test
    void testReadByteArray() throws Exception {
        final StringBuilder expected = new StringBuilder();
        expected.append("test1.xml<?xml version=\"1.0\"?>\n");
        expected.append("<empty/>test2.xml<?xml version=\"1.0\"?>\n");
        expected.append("<empty/>\n");
        final Charset charset = Charset.defaultCharset();
        try (ByteArrayOutputStream result = new ByteArrayOutputStream();
                ArjArchiveInputStream in = ArjArchiveInputStream.builder().setURI(getURI("bla.arj")).get()) {
            in.forEach(entry -> {
                result.write(entry.getName().getBytes(charset));
                final byte[] tmp = new byte[2];
                // read(byte[]) at a time
                int count;
                while ((count = in.read(tmp)) != -1) {
                    result.write(tmp, 0, count);
                }
                assertFalse(entry.isDirectory());
                assertArjArchiveEntry(entry);
            });
            result.flush();
            assertEquals(expected.toString(), result.toString(charset));
        }
    }

    @Test
    void testReadByteArrayIndex() throws Exception {
        final StringBuilder expected = new StringBuilder();
        expected.append("test1.xml<?xml version=\"1.0\"?>\n");
        expected.append("<empty/>test2.xml<?xml version=\"1.0\"?>\n");
        expected.append("<empty/>\n");
        final Charset charset = Charset.defaultCharset();
        try (ByteArrayOutputStream result = new ByteArrayOutputStream();
                ArjArchiveInputStream in = ArjArchiveInputStream.builder()
                        .setURI(getURI("bla.arj"))
                        .get()) {
            in.forEach(entry -> {
                result.write(entry.getName().getBytes(charset));
                final byte[] tmp = new byte[10];
                // read(byte[],int,int) at a time
                int count;
                while ((count = in.read(tmp, 0, 2)) != -1) {
                    result.write(tmp, 0, count);
                }
                assertFalse(entry.isDirectory());
                assertArjArchiveEntry(entry);
            });
            result.flush();
            assertEquals(expected.toString(), result.toString(charset));
        }
    }

    @Test
    void testReadingOfAttributesDosVersion() throws Exception {
        try (ArjArchiveInputStream archive = ArjArchiveInputStream.builder().setURI(getURI("bla.arj")).get()) {
            final ArjArchiveEntry entry = archive.getNextEntry();
            assertEquals("test1.xml", entry.getName());
            assertEquals(30, entry.getSize());
            assertEquals(0, entry.getUnixMode());
            final Calendar cal = Calendar.getInstance();
            cal.set(2008, 9, 6, 23, 50, 52);
            cal.set(Calendar.MILLISECOND, 0);
            assertEquals(cal.getTime(), entry.getLastModifiedDate());
            assertForEach(archive);
        }
    }

    @Test
    void testReadingOfAttributesUnixVersion() throws Exception {
        try (ArjArchiveInputStream in = ArjArchiveInputStream.builder().setURI(getURI("bla.unix.arj")).get()) {
            final ArjArchiveEntry entry = in.getNextEntry();
            assertEquals("test1.xml", entry.getName());
            assertEquals(30, entry.getSize());
            assertEquals(0664, entry.getUnixMode() & 07777 /* UnixStat.PERM_MASK */);
            final Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+0000"));
            cal.set(2008, 9, 6, 21, 50, 52);
            cal.set(Calendar.MILLISECOND, 0);
            assertEquals(cal.getTime(), entry.getLastModifiedDate());
            assertForEach(in);
        }
    }

    @Test
    void testSingleByteReadConsistentlyReturnsMinusOneAtEof() throws Exception {
        try (ArjArchiveInputStream archive = ArjArchiveInputStream.builder().setURI(getURI("bla.arj")).get()) {
            assertNotNull(archive.getNextEntry());
            IOUtils.toByteArray(archive);
            assertEquals(-1, archive.read());
            assertEquals(-1, archive.read());
            assertForEach(archive);
        }
    }

    @Test
    void testSingleArgumentConstructor() throws Exception {
        try (InputStream inputStream = Files.newInputStream(getPath("bla.arj"));
                ArjArchiveInputStream archiveStream = new ArjArchiveInputStream(inputStream)) {
            assertEquals(Charset.forName("CP437"), archiveStream.getCharset());
        }
    }
}
