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
package org.apache.commons.compress.archivers.cpio;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.stream.Stream;

import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class CpioArchiveTest {

    public static Stream<Arguments> factory() {
        return Stream.of(Arguments.of(CpioConstants.FORMAT_NEW), Arguments.of(CpioConstants.FORMAT_NEW_CRC), Arguments.of(CpioConstants.FORMAT_OLD_ASCII),
            Arguments.of(CpioConstants.FORMAT_OLD_BINARY));
    }

    @ParameterizedTest
    @MethodSource("factory")
    public void utf18RoundtripTest(final short format) throws Exception {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (CpioArchiveOutputStream os = new CpioArchiveOutputStream(baos, format, CpioConstants.BLOCK_SIZE, "UTF-16LE")) {
                final CpioArchiveEntry entry = new CpioArchiveEntry(format, "T\u00e4st.txt", 4);
                if (format == CpioConstants.FORMAT_NEW_CRC) {
                    entry.setChksum(10);
                }
                os.putArchiveEntry(entry);
                os.write(new byte[] {1, 2, 3, 4});
                os.closeArchiveEntry();
            }
            baos.close();
            try (ByteArrayInputStream bin = new ByteArrayInputStream(baos.toByteArray());
                CpioArchiveInputStream in = new CpioArchiveInputStream(bin, "UTF-16LE")) {
                final CpioArchiveEntry entry = (CpioArchiveEntry) in.getNextEntry();
                assertNotNull(entry);
                assertEquals("T\u00e4st.txt", entry.getName());
                assertArrayEquals(new byte[] {1, 2, 3, 4}, IOUtils.toByteArray(in));
            }
        }
    }
}
