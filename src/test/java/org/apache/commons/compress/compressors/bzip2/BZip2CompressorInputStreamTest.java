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
package org.apache.commons.compress.compressors.bzip2;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream.Data;
import org.apache.commons.compress.utils.BitInputStream;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BZip2CompressorInputStreamTest extends AbstractTest {

    private static final int MIN_CODE_LEN = 1;
    private static final int MAX_CODE_LEN = 20;

    private void fuzzingTest(final int[] bytes) throws IOException, ArchiveException {
        final int len = bytes.length;
        final byte[] input = new byte[len];
        for (int i = 0; i < len; i++) {
            input[i] = (byte) bytes[i];
        }
        try (ArchiveInputStream<?> ais = ArchiveStreamFactory.DEFAULT.createArchiveInputStream("zip", new ByteArrayInputStream(input))) {
            ais.getNextEntry();
            IOUtils.toByteArray(ais);
        }
    }

    /**
     * Builds a minimal bitstream for recvDecodingTables():
     * <ul>
     *     <li>Uses only one symbol 'A' (0x41).</li>
     *     <li>Number of groups: 2 (minimum).</li>
     *     <li>Number of selectors: 3.</li>
     *     <li>Selectors: all three encode j=1 (unary "10").</li>
     *     <li>Huffman code lengths for 2 groups over alphabet size 3 (RUNA, RUNB, EOB) are all equal to {@code codeLength}.</li>
     * </ul>
     * <p>
     *     <strong>Note:</strong> The values are chosen to keep everything byte-aligned.
     * </p>
     * @param codeLength the code length to use for each symbol in each group; must be in [0, 31]
     */
    private BitInputStream prepareDecodingTables(final int codeLength) {
        assertTrue(0 <= codeLength && codeLength <= 31, "codeLength must be between 0 and 31");

        final ByteArrayOutputStream stream = new ByteArrayOutputStream();

        // Upper nibbles in use: set only upper nibble 4
        stream.write(0b0000_1000);
        stream.write(0b0000_0000);

        // Lower nibbles of upper nibble 4: set only lower nibble 1
        stream.write(0b0100_0000);
        stream.write(0b0000_0000);

        // Groups/selectors (24 bits total, byte-aligned)
        // nGroups = 2 (3 bits), nSelectors = 3 (15 bits), selectors: 10 10 10
        stream.write(0b010_00000); // nGroups (3 bits) + high 5 bits of nSelectors
        stream.write(0b00000000); // middle 8 bits of nSelectors
        stream.write(0b11_10_10_10); // low 2 bits of nSelectors + selectors (3 x 2 bits)

        // Huffman tables: two groups, three symbols each
        // startLen (5 bits) followed by 3x '0' (done) => one byte: codeLength << 3
        stream.write(codeLength << 3);
        stream.write(codeLength << 3);

        return new BitInputStream(new ByteArrayInputStream(stream.toByteArray()), ByteOrder.BIG_ENDIAN);
    }

    @Test
    void testHbCreateDecodeTables() throws IOException {
        assertThrows(CompressorException.class, () -> new BZip2CompressorInputStream(
                Files.newInputStream(Paths.get("src/test/resources/org/apache/commons/compress/bzip2/hbCreateDecodeTables.bin"))));
    }

    @Test
    void testMultiByteReadConsistentlyReturnsMinusOneAtEof() throws IOException {
        final File input = getFile("bla.txt.bz2");
        final byte[] buf = new byte[2];
        try (InputStream is = Files.newInputStream(input.toPath());
                BZip2CompressorInputStream in = new BZip2CompressorInputStream(is)) {
            IOUtils.toByteArray(in);
            assertEquals(-1, in.read(buf));
            assertEquals(-1, in.read(buf));
        }
    }

    /**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-309"
     */
    @Test
    void testReadOfLength0ShouldReturn0() throws Exception {
        // Create a big random piece of data
        final byte[] rawData = new byte[1048576];
        for (int i = 0; i < rawData.length; ++i) {
            rawData[i] = (byte) Math.floor(Math.random() * 256);
        }

        // Compress it
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (BZip2CompressorOutputStream bzipOut = new BZip2CompressorOutputStream(baos)) {
            bzipOut.write(rawData);
            bzipOut.flush();
            bzipOut.close();
            assertTrue(bzipOut.isClosed());
            baos.flush();
        }

        // Try to read it back in
        final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        try (BZip2CompressorInputStream bzipIn = new BZip2CompressorInputStream(bais)) {
            final byte[] buffer = new byte[1024];
            assertEquals(1024, bzipIn.read(buffer, 0, 1024));
            assertEquals(0, bzipIn.read(buffer, 1024, 0));
            assertEquals(1024, bzipIn.read(buffer, 0, 1024));
        }
    }

    @ParameterizedTest(name = "code length {0} -> must be rejected")
    @ValueSource(ints = {MIN_CODE_LEN - 1, MAX_CODE_LEN + 1})
    void testRecvDecodingTablesWithOutOfRangeCodeLength(final int codeLength) throws IOException {
        try (BitInputStream tables = prepareDecodingTables(codeLength)) {
            final Data data = new Data(1);

            final CompressorException ex = assertThrows(
                    CompressorException.class,
                    () -> BZip2CompressorInputStream.recvDecodingTables(tables, data),
                    "Expected CompressorException for invalid code length " + codeLength);

            final String msg = ex.getMessage();
            assertNotNull(msg, "Exception message must not be null");
            assertTrue(msg.toLowerCase().contains("code length"), "Message should mention 'code length'");
            assertTrue(
                    msg.contains("[" + MIN_CODE_LEN + ", " + MAX_CODE_LEN + "]"),
                    "Message should mention valid range [" + MIN_CODE_LEN + ", " + MAX_CODE_LEN + "]");
            assertTrue(
                    msg.contains(Integer.toString(codeLength)),
                    "Message should include the offending value " + codeLength);
        }
    }

    @ParameterizedTest(name = "code length {0} -> accepted and stored")
    @ValueSource(ints = {MIN_CODE_LEN, MAX_CODE_LEN})
    void testRecvDecodingTablesWithValidCodeLength(final int codeLength) throws IOException {
        try (BitInputStream tables = prepareDecodingTables(codeLength)) {
            final Data data = new Data(1);

            assertDoesNotThrow(
                    () -> BZip2CompressorInputStream.recvDecodingTables(tables, data),
                    "Should accept code length " + codeLength + " within [" + MIN_CODE_LEN + ", " + MAX_CODE_LEN + "]");

            // We encoded 2 Huffman groups; both minLens should equal the encoded codeLength
            assertEquals(2, data.huffmanDecodersCount, "Expected 2 Huffman groups");
            assertEquals(codeLength, data.huffmanDecoders[0].getMinLength(), "Group 0 min code length mismatch");
            assertEquals(codeLength, data.huffmanDecoders[1].getMinLength(), "Group 1 min code length mismatch");
        }
    }

    @Test
    void testShouldThrowAnIOExceptionWhenAppliedToAZipFile() throws Exception {
        try (InputStream in = newInputStream("bla.zip")) {
            assertThrows(CompressorException.class, () -> new BZip2CompressorInputStream(in));
        }
    }

    /**
     * Tests <a href="https://issues.apache.org/jira/browse/COMPRESS-516">COMPRESS-516</a>.
     *
     * @see <a href="https://issues.apache.org/jira/browse/COMPRESS-516">COMPRESS-516</a>
     */
    @Test
    void testShouldThrowIOExceptionInsteadOfRuntimeExceptionCOMPRESS516() {
        assertThrows(CompressorException.class,
                () -> fuzzingTest(new int[] { 0x50, 0x4b, 0x03, 0x04, 0x2e, 0x00, 0x00, 0x00, 0x0c, 0x00, 0x84, 0xb6, 0xba, 0x46, 0x72, 0xb6, 0xfe, 0x77, 0x63,
                        0x00, 0x00, 0x00, 0x6b, 0x00, 0x00, 0x00, 0x03, 0x00, 0x1c, 0x00, 0x62, 0x62, 0x62, 0x55, 0x54, 0x09, 0x00, 0x03, 0xe7, 0xce, 0x64,
                        0x55, 0xf3, 0xce, 0x64, 0x55, 0x75, 0x78, 0x0b, 0x00, 0x01, 0x04, 0x5c, 0xf9, 0x01, 0x00, 0x04, 0x88, 0x13, 0x00, 0x00, 0x42, 0x5a,
                        0x68, 0x34, 0x31, 0x41, 0x59, 0x26, 0x53, 0x59, 0x62, 0xe4, 0x4f, 0x51, 0x00, 0x00, 0x0d, 0xd1, 0x80, 0x00, 0x10, 0x40, 0x00, 0x35,
                        0xf9, 0x8b, 0x00, 0x20, 0x00, 0x48, 0x89, 0xfa, 0x94, 0xf2, 0x9e, 0x29, 0xe8, 0xd2, 0x11, 0x8a, 0x4f, 0x53, 0x34, 0x0f, 0x51, 0x7a,
                        0xed, 0x86, 0x65, 0xd6, 0xed, 0x61, 0xee, 0x68, 0x89, 0x48, 0x7d, 0x07, 0x71, 0x92, 0x2a, 0x50, 0x60, 0x04, 0x95, 0x61, 0x35, 0x47,
                        0x73, 0x31, 0x29, 0xc2, 0xdd, 0x5e, 0xc7, 0x4a, 0x15, 0x14, 0x32, 0x4c, 0xda, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 }));
    }

    /**
     * @see <a href="https://issues.apache.org/jira/browse/COMPRESS-519">COMPRESS-519</a>
     */
    @Test
    void testShouldThrowIOExceptionInsteadOfRuntimeExceptionCOMPRESS519() {
        assertThrows(CompressorException.class,
                () -> fuzzingTest(new int[] { 0x50, 0x4b, 0x03, 0x04, 0x2e, 0x00, 0x00, 0x00, 0x0c, 0x00, 0x84, 0xb6, 0xba, 0x46, 0x72, 0xb6, 0xfe, 0x77, 0x63,
                        0x00, 0x00, 0x00, 0x6b, 0x00, 0x00, 0x00, 0x03, 0x00, 0x1c, 0x00, 0x62, 0x62, 0x62, 0x55, 0x54, 0x09, 0x00, 0x03, 0xe7, 0xce, 0x64,
                        0x55, 0xf3, 0xce, 0x64, 0x55, 0x75, 0x78, 0x0b, 0x00, 0x01, 0x04, 0x5c, 0xf9, 0x01, 0x00, 0x04, 0x88, 0x13, 0x00, 0x00, 0x42, 0x5a,
                        0x68, 0x34, 0x31, 0x41, 0x59, 0x26, 0x53, 0x59, 0x62, 0xe4, 0x4f, 0x51, 0x80, 0x00, 0x0d, 0xd1, 0x80, 0x00, 0x10, 0x40, 0x00, 0x35,
                        0xf9, 0x8b, 0x00, 0x20, 0x00, 0x48, 0x89, 0xfa, 0x94, 0xf2, 0x9e, 0x29, 0xe8, 0xd2, 0x00, 0x00, 0x22, 0x00, 0x00, 0x00, 0x50, 0x4b,
                        0x03, 0x04, 0x14, 0x00, 0x08, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00 }));
    }

    @Test
    void testSingleByteReadConsistentlyReturnsMinusOneAtEof() throws IOException {
        final File input = getFile("bla.txt.bz2");
        try (InputStream is = Files.newInputStream(input.toPath());
                BZip2CompressorInputStream in = new BZip2CompressorInputStream(is)) {
            IOUtils.toByteArray(in);
            assertEquals(-1, in.read());
            assertEquals(-1, in.read());
        }
    }

}
