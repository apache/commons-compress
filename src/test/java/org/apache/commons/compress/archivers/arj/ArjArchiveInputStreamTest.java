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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.stream.Stream;
import java.util.zip.CRC32;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.io.EndianUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.cartesian.CartesianTest;

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

    private static byte[] createArjArchiveHeader(int size, boolean computeCrc) {
        // Enough space for the fixed-size portion of the header plus:
        // - signature (2 bytes)
        // - the 2-byte basic header size field itself (2 bytes)
        // - at least one byte each for the filename and comment C-strings (2 bytes)
        // - the 4-byte CRC-32 that follows the basic header
        final byte[] bytes = new byte[4 + size + 10];
        bytes[0] = (byte) 0x60; // ARJ signature
        bytes[1] = (byte) 0xEA;
        // Basic header size (little-endian)
        EndianUtils.writeSwappedShort(bytes, 2, (short) (size + 2));
        // First header size
        bytes[4] = (byte) size;
        // Compute valid CRC-32 for the basic header
        if (computeCrc) {
            final CRC32 crc32 = new CRC32();
            crc32.update(bytes, 4, size + 2);
            EndianUtils.writeSwappedInteger(bytes, 4 + size + 2, (int) crc32.getValue());
        }
        return bytes;
    }

    @CartesianTest
    void testSmallFirstHeaderSize(
            // 30 is the minimum valid size
            @CartesianTest.Values(ints = {0, 1, 10, 29}) int size, @CartesianTest.Values(booleans = {false, true}) boolean selfExtracting) {
        final byte[] bytes = createArjArchiveHeader(size, true);
        assertThrows(ArchiveException.class, () -> ArjArchiveInputStream.builder().setByteArray(bytes).setSelfExtracting(selfExtracting).get());
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

    @ParameterizedTest
    @ValueSource(strings = { "bla.arj", "bla.unix.arj" })
    void testGetBytesRead(final String resource) throws IOException {
        final Path path = getPath(resource);
        try (ArjArchiveInputStream in = ArjArchiveInputStream.builder().setPath(path).get()) {
            while (in.getNextEntry() != null) {
                // nop
            }
            final long expected = Files.size(path);
            assertEquals(expected, in.getBytesRead(), "getBytesRead() did not return the expected value");
        }
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

    static Stream<byte[]> testSelfExtractingArchive() {
        return Stream.of(
                new byte[] { 0x10, 0x11, 0x12, 0x13, 0x14 },
                // In a normal context: an archive trailer.
                new byte[] { 0x60, (byte) 0xEA, 0x00, 0x00 },
                // Header of valid size, but with an invalid CRC-32.
                createArjArchiveHeader(30, false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testSelfExtractingArchive(byte[] junk) throws Exception {
        final Path validArj = getPath("bla.arj");
        try (InputStream first = new ByteArrayInputStream(junk);
             InputStream second = Files.newInputStream(validArj);
             SequenceInputStream seq = new SequenceInputStream(first, second);
             ArjArchiveInputStream in = ArjArchiveInputStream.builder().setInputStream(seq).setSelfExtracting(true).get()) {
            ArjArchiveEntry entry = in.getNextEntry();
            assertNotNull(entry);
            assertEquals("test1.xml", entry.getName());
            entry = in.getNextEntry();
            assertNotNull(entry);
            assertEquals("test2.xml", entry.getName());
            entry = in.getNextEntry();
            assertNull(entry);
        }
    }

    @Test
    void testSingleArgumentConstructor() throws Exception {
        try (InputStream inputStream = Files.newInputStream(getPath("bla.arj"));
                ArjArchiveInputStream archiveStream = new ArjArchiveInputStream(inputStream)) {
            assertEquals(Charset.forName("CP437"), archiveStream.getCharset());
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

    /**
     * Verifies that reading an ARJ header record cut short at various boundaries
     * results in an {@link EOFException}.
     *
     * <p>The main archive header is at the beginning of the file. Within that header:</p>
     * <ul>
     *   <li><b>Basic header size</b> (2 bytes at offsets 0x02–0x03) = {@code 0x002b}.</li>
     *   <li><b>Fixed header size</b> (aka {@code first_hdr_size}, 1 byte at 0x04) = {@code 0x22}.</li>
     *   <li>The archive name and comment C-strings follow the fixed header and complete the basic header.</li>
     *   <li>A 4-byte <b>basic header CRC-32</b> follows the basic header.</li>
     * </ul>
     *
     * @param maxCount absolute truncation point (number of readable bytes from the start of the file)
     */
    @ParameterizedTest
    @ValueSource(longs = {
            // Empty file.
            0,
            // Immediately after the 2-byte signature
            0x02,
            // Inside / after the basic-header size (2 bytes at 0x02–0x03)
            0x03, 0x04,
            // Just after the fixed-header size (1 byte at 0x04)
            0x05,
            // End of fixed header (0x04 + first_hdr_size == 0x26)
            0x26,
            // End of basic header after filename/comment (0x04 + basic_hdr_size == 0x2f)
            0x2f,
            // Inside / after the basic-header CRC-32 (4 bytes)
            0x30, 0x33,
            // Inside the extended-header length (2 bytes)
            0x34})
    void testTruncatedMainHeader(long maxCount) throws Exception {
        try (InputStream input = BoundedInputStream.builder()
                .setURI(getURI("bla.arj"))
                .setMaxCount(maxCount)
                .get()) {
            final ArchiveException ex = assertThrows(ArchiveException.class, () -> ArjArchiveInputStream.builder().setInputStream(input).get());
            final Throwable cause = ex.getCause();
            assertInstanceOf(EOFException.class, cause, "Expected EOFException as cause of ArchiveException.");
        }
    }

    /**
     * Verifies that reading an ARJ header record cut short at various boundaries
     * results in an {@link EOFException}.
     *
     * <p>The test archive is crafted so that the local file header of the first entry begins at
     * byte offset {@code 0x0035}. Within that header:</p>
     * <ul>
     *   <li><b>Basic header size</b> (2 bytes at offsets 0x02–0x03) = {@code 0x0039}.</li>
     *   <li><b>Fixed header size</b> (aka {@code first_hdr_size}, 1 byte at 0x04) = {@code 0x2E}.</li>
     *   <li>The filename and comment C-strings follow the fixed header and complete the basic header.</li>
     *   <li>A 4-byte <b>basic header CRC-32</b> follows the basic header.</li>
     * </ul>
     *
     * @param maxCount absolute truncation point (number of readable bytes from the start of the file)
     */
    @ParameterizedTest
    @ValueSource(longs = {
            // Before the local file header signature
            0x35,
            // Immediately after the 2-byte signature
            0x35 + 0x02,
            // Inside / after the basic-header size (2 bytes at 0x02–0x03)
            0x35 + 0x03, 0x35 + 0x04,
            // Just after the fixed-header size (1 byte at 0x04)
            0x35 + 0x05,
            // End of fixed header (0x04 + first_hdr_size == 0x32)
            0x35 + 0x32,
            // End of basic header after filename/comment (0x04 + basic_hdr_size == 0x3d)
            0x35 + 0x3d,
            // Inside / after the basic-header CRC-32 (4 bytes)
            0x35 + 0x3e, 0x35 + 0x41,
            // Inside / after the extended-header length (2 bytes)
            0x35 + 0x42, 0x35 + 0x43,
            // One byte before the first file’s data
            0x95
    })
    void testTruncatedLocalHeader(long maxCount) throws Exception {
        try (InputStream input = BoundedInputStream.builder().setURI(getURI("bla.arj")).setMaxCount(maxCount).get();
             ArjArchiveInputStream archive = ArjArchiveInputStream.builder().setInputStream(input).get()) {
            assertThrows(EOFException.class, () -> {
                archive.getNextEntry();
                IOUtils.skip(archive, Long.MAX_VALUE);
            });
        }
    }
}
