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
package org.apache.commons.compress.archivers.cpio;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CpioArchiveTest {

    public static Stream<Arguments> factory() {
        return Stream.of(Arguments.of(CpioConstants.FORMAT_NEW), Arguments.of(CpioConstants.FORMAT_NEW_CRC), Arguments.of(CpioConstants.FORMAT_OLD_ASCII),
                Arguments.of(CpioConstants.FORMAT_OLD_BINARY));
    }

    @Test
    void utf18RoundtripTestCtor2() throws Exception {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (CpioArchiveOutputStream os = new CpioArchiveOutputStream(baos, StandardCharsets.UTF_8.name())) {
                final CpioArchiveEntry entry = new CpioArchiveEntry("Test.txt", 4);
                os.putArchiveEntry(entry);
                os.write(new byte[] { 1, 2, 3, 4 });
                os.closeArchiveEntry();
            }
            baos.close();
            try (CpioArchiveInputStream in = CpioArchiveInputStream.builder()
                    .setByteArray(baos.toByteArray())
                    .setCharset(StandardCharsets.UTF_8)
                    .get()) {
                final CpioArchiveEntry entry = in.getNextEntry();
                assertNotNull(entry);
                assertEquals("Test.txt", entry.getName());
                assertArrayEquals(new byte[] { 1, 2, 3, 4 }, IOUtils.toByteArray(in));
            }
        }
    }

    @ParameterizedTest
    @MethodSource("factory")
    public void utf18RoundtripTestCtor3(final short format) throws Exception {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (CpioArchiveOutputStream os = new CpioArchiveOutputStream(baos, format, CpioConstants.BLOCK_SIZE)) {
                final CpioArchiveEntry entry = new CpioArchiveEntry(format, "T\u00e4st.txt", 4);
                if (format == CpioConstants.FORMAT_NEW_CRC) {
                    entry.setChksum(10);
                }
                os.putArchiveEntry(entry);
                os.write(new byte[] { 1, 2, 3, 4 });
                os.closeArchiveEntry();
            }
            baos.close();
            try (CpioArchiveInputStream in = CpioArchiveInputStream.builder()
                    .setByteArray(baos.toByteArray())
                    .get()) {
                final CpioArchiveEntry entry = in.getNextEntry();
                assertNotNull(entry);
                assertEquals("T%U00E4st.txt", entry.getName());
                assertArrayEquals(new byte[] { 1, 2, 3, 4 }, IOUtils.toByteArray(in));
            }
        }
    }

    @ParameterizedTest
    @MethodSource("factory")
    public void utf18RoundtripTestCtor4(final short format) throws Exception {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (CpioArchiveOutputStream os = new CpioArchiveOutputStream(baos, format, CpioConstants.BLOCK_SIZE, StandardCharsets.UTF_16LE.name())) {
                final CpioArchiveEntry entry = new CpioArchiveEntry(format, "T\u00e4st.txt", 4);
                if (format == CpioConstants.FORMAT_NEW_CRC) {
                    entry.setChksum(10);
                }
                os.putArchiveEntry(entry);
                os.write(new byte[] { 1, 2, 3, 4 });
                os.closeArchiveEntry();
            }
            baos.close();
            try (CpioArchiveInputStream in = CpioArchiveInputStream.builder()
                    .setByteArray(baos.toByteArray())
                    .setCharset(StandardCharsets.UTF_16LE)
                    .get()) {
                final CpioArchiveEntry entry = in.getNextEntry();
                assertNotNull(entry);
                assertEquals("T\u00e4st.txt", entry.getName());
                assertArrayEquals(new byte[] { 1, 2, 3, 4 }, IOUtils.toByteArray(in));
            }
        }
    }
}
