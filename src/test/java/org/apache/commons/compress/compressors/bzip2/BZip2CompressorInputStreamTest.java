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
package org.apache.commons.compress.compressors.bzip2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.Test;

public class BZip2CompressorInputStreamTest extends AbstractTestCase {

    private void fuzzingTest(final int[] bytes) throws IOException, ArchiveException {
        final int len = bytes.length;
        final byte[] input = new byte[len];
        for (int i = 0; i < len; i++) {
            input[i] = (byte) bytes[i];
        }
        try (ArchiveInputStream ais = ArchiveStreamFactory.DEFAULT
             .createArchiveInputStream("zip", new ByteArrayInputStream(input))) {
            ais.getNextEntry();
            IOUtils.toByteArray(ais);
        }
    }

    @Test
    public void multiByteReadConsistentlyReturnsMinusOneAtEof() throws IOException {
        final File input = getFile("bla.txt.bz2");
        final byte[] buf = new byte[2];
        try (InputStream is = Files.newInputStream(input.toPath())) {
            final BZip2CompressorInputStream in =
                    new BZip2CompressorInputStream(is);
            IOUtils.toByteArray(in);
            assertEquals(-1, in.read(buf));
            assertEquals(-1, in.read(buf));
            in.close();
        }
    }

    /**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-309"
     */
    @Test
    public void readOfLength0ShouldReturn0() throws Exception {
        // Create a big random piece of data
        final byte[] rawData = new byte[1048576];
        for (int i=0; i < rawData.length; ++i) {
            rawData[i] = (byte) Math.floor(Math.random()*256);
        }

        // Compress it
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final BZip2CompressorOutputStream bzipOut = new BZip2CompressorOutputStream(baos);
        bzipOut.write(rawData);
        bzipOut.flush();
        bzipOut.close();
        baos.flush();
        baos.close();

        // Try to read it back in
        final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        final BZip2CompressorInputStream bzipIn = new BZip2CompressorInputStream(bais);
        final byte[] buffer = new byte[1024];
        assertEquals(1024, bzipIn.read(buffer, 0, 1024));
        assertEquals(0, bzipIn.read(buffer, 1024, 0));
        assertEquals(1024, bzipIn.read(buffer, 0, 1024));
        bzipIn.close();
    }

    @Test
    public void shouldThrowAnIOExceptionWhenAppliedToAZipFile() throws Exception {
        try (InputStream in = newInputStream("bla.zip")) {
            assertThrows(IOException.class, () -> new BZip2CompressorInputStream(in));
        }
    }

    /**
     * @see <a href="https://issues.apache.org/jira/browse/COMPRESS-516">COMPRESS-516</a>
     */
    @Test
    public void shouldThrowIOExceptionInsteadOfRuntimeExceptionCOMPRESS516() {
        assertThrows(IOException.class, () -> fuzzingTest(new int[] {
            0x50, 0x4b, 0x03, 0x04, 0x2e, 0x00, 0x00, 0x00, 0x0c, 0x00,
            0x84, 0xb6, 0xba, 0x46, 0x72, 0xb6, 0xfe, 0x77, 0x63, 0x00,
            0x00, 0x00, 0x6b, 0x00, 0x00, 0x00, 0x03, 0x00, 0x1c, 0x00,
            0x62, 0x62, 0x62, 0x55, 0x54, 0x09, 0x00, 0x03, 0xe7, 0xce,
            0x64, 0x55, 0xf3, 0xce, 0x64, 0x55, 0x75, 0x78, 0x0b, 0x00,
            0x01, 0x04, 0x5c, 0xf9, 0x01, 0x00, 0x04, 0x88, 0x13, 0x00,
            0x00, 0x42, 0x5a, 0x68, 0x34, 0x31, 0x41, 0x59, 0x26, 0x53,
            0x59, 0x62, 0xe4, 0x4f, 0x51, 0x00, 0x00, 0x0d, 0xd1, 0x80,
            0x00, 0x10, 0x40, 0x00, 0x35, 0xf9, 0x8b, 0x00, 0x20, 0x00,
            0x48, 0x89, 0xfa, 0x94, 0xf2, 0x9e, 0x29, 0xe8, 0xd2, 0x11,
            0x8a, 0x4f, 0x53, 0x34, 0x0f, 0x51, 0x7a, 0xed, 0x86, 0x65,
            0xd6, 0xed, 0x61, 0xee, 0x68, 0x89, 0x48, 0x7d, 0x07, 0x71,
            0x92, 0x2a, 0x50, 0x60, 0x04, 0x95, 0x61, 0x35, 0x47, 0x73,
            0x31, 0x29, 0xc2, 0xdd, 0x5e, 0xc7, 0x4a, 0x15, 0x14, 0x32,
            0x4c, 0xda, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00
        }));
    }

    /**
     * @see <a href="https://issues.apache.org/jira/browse/COMPRESS-519">COMPRESS-519</a>
     */
    @Test
    public void shouldThrowIOExceptionInsteadOfRuntimeExceptionCOMPRESS519() {
        assertThrows(IOException.class, () -> fuzzingTest(new int[] {
            0x50, 0x4b, 0x03, 0x04, 0x2e, 0x00, 0x00, 0x00, 0x0c, 0x00,
            0x84, 0xb6, 0xba, 0x46, 0x72, 0xb6, 0xfe, 0x77, 0x63, 0x00,
            0x00, 0x00, 0x6b, 0x00, 0x00, 0x00, 0x03, 0x00, 0x1c, 0x00,
            0x62, 0x62, 0x62, 0x55, 0x54, 0x09, 0x00, 0x03, 0xe7, 0xce,
            0x64, 0x55, 0xf3, 0xce, 0x64, 0x55, 0x75, 0x78, 0x0b, 0x00,
            0x01, 0x04, 0x5c, 0xf9, 0x01, 0x00, 0x04, 0x88, 0x13, 0x00,
            0x00, 0x42, 0x5a, 0x68, 0x34, 0x31, 0x41, 0x59, 0x26, 0x53,
            0x59, 0x62, 0xe4, 0x4f, 0x51, 0x80, 0x00, 0x0d, 0xd1, 0x80,
            0x00, 0x10, 0x40, 0x00, 0x35, 0xf9, 0x8b, 0x00, 0x20, 0x00,
            0x48, 0x89, 0xfa, 0x94, 0xf2, 0x9e, 0x29, 0xe8, 0xd2, 0x00,
            0x00, 0x22, 0x00, 0x00, 0x00, 0x50, 0x4b, 0x03, 0x04, 0x14,
            0x00, 0x08, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00
        }));
    }

    @Test
    public void singleByteReadConsistentlyReturnsMinusOneAtEof() throws IOException {
        final File input = getFile("bla.txt.bz2");
        try (InputStream is = Files.newInputStream(input.toPath())) {
            final BZip2CompressorInputStream in =
                    new BZip2CompressorInputStream(is);
            IOUtils.toByteArray(in);
            assertEquals(-1, in.read());
            assertEquals(-1, in.read());
            in.close();
        }
    }
}
