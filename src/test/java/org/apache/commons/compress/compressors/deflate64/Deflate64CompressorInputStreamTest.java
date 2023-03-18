/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.commons.compress.compressors.deflate64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class Deflate64CompressorInputStreamTest {
    private final HuffmanDecoder nullDecoder = null;

    @Mock
    private HuffmanDecoder decoder;

    @Test
    public void closeCallsDecoder() throws Exception {

        try (final Deflate64CompressorInputStream input = new Deflate64CompressorInputStream(decoder)) {
            // empty
        }

        Mockito.verify(decoder, times(1)).close();
    }

    @Test
    public void closeIsDelegatedJustOnce() throws Exception {

        try (final Deflate64CompressorInputStream input = new Deflate64CompressorInputStream(decoder)) {
            input.close();
        }

        Mockito.verify(decoder, times(1)).close();
    }

    @Test
    public void delegatesAvailable() throws Exception {
        Mockito.when(decoder.available()).thenReturn(1024);

        try (final Deflate64CompressorInputStream input = new Deflate64CompressorInputStream(decoder)) {
            assertEquals(1024, input.available());
        }
    }

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
    public void multiByteReadConsistentlyReturnsMinusOneAtEof() throws Exception {
        final byte[] buf = new byte[2];
        try (final Deflate64CompressorInputStream in =
                    new Deflate64CompressorInputStream(nullDecoder)) {
            IOUtils.toByteArray(in);
            assertEquals(-1, in.read(buf));
            assertEquals(-1, in.read(buf));
        }
    }

    @Test
    public void properSizeWhenClosed() throws Exception {
        try (final Deflate64CompressorInputStream input = new Deflate64CompressorInputStream(nullDecoder)) {
            assertEquals(0, input.available());
        }
    }

    @Test
    public void readWhenClosed() throws Exception {
        try (final Deflate64CompressorInputStream input = new Deflate64CompressorInputStream(nullDecoder)) {
            assertEquals(-1, input.read());
            assertEquals(-1, input.read(new byte[1]));
            assertEquals(-1, input.read(new byte[1], 0, 1));
        }
    }

    /**
     * @see <a href="https://issues.apache.org/jira/browse/COMPRESS-521">COMPRESS-521</a>
     */
    @Test
    public void shouldThrowIOExceptionInsteadOfRuntimeExceptionCOMPRESS521() {
        assertThrows(IOException.class, () -> fuzzingTest(new int[] {
            0x50, 0x4b, 0x03, 0x04, 0x2e, 0x00, 0xb6, 0x00, 0x09, 0x00,
            0x84, 0xb6, 0xba, 0x46, 0x72, 0x00, 0xfe, 0x77, 0x63, 0x00,
            0x00, 0x00, 0x6b, 0x00, 0x00, 0x00, 0x03, 0x00, 0x1c, 0x00,
            0x62, 0x62, 0x62, 0x55, 0x54, 0x0c, 0x00, 0x03, 0xe7, 0xce,
            0x64, 0x55, 0xf3, 0xce, 0x65, 0x55, 0x75, 0x78, 0x0b, 0x00,
            0x01, 0x04, 0x5c, 0xf9, 0x01, 0x00, 0x04, 0x88, 0x13, 0x00,
            0x00, 0x42, 0x5a, 0x68, 0x34
        }));
    }

    /**
     * @see <a href="https://issues.apache.org/jira/browse/COMPRESS-522">COMPRESS-522</a>
     */
    @Test
    public void shouldThrowIOExceptionInsteadOfRuntimeExceptionCOMPRESS522() {
        assertThrows(IOException.class, () -> fuzzingTest(new int[] {
            0x50, 0x4b, 0x03, 0x04, 0x14, 0x00, 0x08, 0x00, 0x09, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00,
            0x61, 0x4a, 0x84, 0x02, 0x40, 0x00, 0x01, 0x00, 0xff, 0xff
        }));
    }

    /**
     * @see <a href="https://issues.apache.org/jira/browse/COMPRESS-525">COMPRESS-525</a>
     */
    @Test
    public void shouldThrowIOExceptionInsteadOfRuntimeExceptionCOMPRESS525() {
        assertThrows(IOException.class, () -> fuzzingTest(new int[] {
            0x50, 0x4b, 0x03, 0x04, 0x14, 0x00, 0x08, 0x00, 0x09, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x78, 0x00,
            0x61, 0x4a, 0x04, 0x04, 0x00, 0x00, 0xff, 0xff, 0x50, 0x53,
            0x07, 0x08, 0x43, 0xbe, 0xb7, 0xe8, 0x07, 0x00, 0x00, 0x00,
            0x01, 0x00, 0x00, 0x00, 0x50, 0x4b, 0x03, 0x04, 0x14, 0x00,
            0x08, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x01, 0x00, 0x00, 0x00, 0x62, 0x4a, 0x02, 0x04, 0x00, 0x00,
            0xff, 0xff, 0x50, 0x4b, 0x7f, 0x08, 0xf9, 0xef, 0xbe, 0x71,
            0x07, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x50, 0x4b,
            0x03, 0x04, 0x14, 0x00, 0x08, 0x00, 0x08, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x63, 0x4a,
            0x06, 0x04, 0x00, 0x00, 0xff, 0xff, 0x50, 0x4b, 0x07, 0x08,
            0x6f, 0xdf
        }));
    }

    /**
     * @see <a href="https://issues.apache.org/jira/browse/COMPRESS-526">COMPRESS-526</a>
     */
    @Test
    public void shouldThrowIOExceptionInsteadOfRuntimeExceptionCOMPRESS526() {
        assertThrows(IOException.class, () -> fuzzingTest(new int[] {
            0x50, 0x4b, 0x03, 0x04, 0x14, 0x00, 0x08, 0x00, 0x09, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x6f, 0x00, 0x00, 0x00,
            0x61, 0x4a, 0x04, 0x04, 0x00, 0x00, 0xff, 0xff, 0x50, 0x53,
            0x07, 0x08, 0x43, 0xbe, 0xb7, 0xe8, 0x07, 0x00, 0x00, 0x00,
            0x01, 0x00, 0x00, 0x00, 0x50, 0x4b, 0x03, 0x04, 0x14, 0x00,
            0x08, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x01, 0x00, 0x00, 0x00, 0x62, 0x4a, 0x02, 0x04, 0x00, 0x00,
            0xff, 0xff, 0x50, 0x4b, 0x7f, 0x08, 0xf9, 0xef, 0xbe, 0x71,
            0x07, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x50, 0x4b,
            0x03, 0x04, 0x14, 0x00, 0x08, 0x00, 0x08, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x63, 0x4a,
            0x06, 0x04, 0x00, 0x00, 0xff, 0xff, 0x50, 0x4b, 0x07, 0x08,
            0x01, 0xdf, 0xb9, 0x06, 0x07, 0x00, 0x00, 0x00, 0x01, 0x00,
            0x00, 0x00, 0x50, 0x4b, 0x03, 0x04, 0x14, 0x00, 0x08, 0x00,
            0x08
        }));
    }

    /**
     * @see <a href="https://issues.apache.org/jira/browse/COMPRESS-527">COMPRESS-527</a>
     */
    @Test
    public void shouldThrowIOExceptionInsteadOfRuntimeExceptionCOMPRESS527() {
        assertThrows(IOException.class, () -> fuzzingTest(new int[] {
            0x50, 0x4b, 0x03, 0x04, 0x14, 0x00, 0x00, 0x00, 0x09, 0x00,
            0x84, 0xb6, 0xba, 0x46, 0x72, 0xb6, 0xfe, 0x77, 0x4a, 0x00,
            0x00, 0x00, 0x6b, 0x00, 0x00, 0x00, 0x03, 0x00, 0x1c, 0x00,
            0x62, 0x62, 0x62, 0x55, 0x54, 0x09, 0x00, 0x03, 0xe7, 0xce,
            0x64, 0x55, 0xf3, 0xce, 0x64, 0x55, 0x75, 0x78, 0x0b, 0x00,
            0x01, 0x04, 0x5c, 0xf9, 0x01, 0x00, 0x04, 0x88, 0x13, 0x00,
            0x00, 0x1d, 0x8b, 0xc1, 0x0d, 0xc0, 0x30, 0x08, 0x03, 0xff,
            0x99, 0xc2, 0xab, 0x81, 0x50, 0x1a, 0xa8, 0x44, 0x1e, 0x56,
            0x30, 0x7f, 0x21, 0x1f, 0x5b, 0x3e, 0x9d, 0x85, 0x6e
        }));
    }

    @Test
    public void shouldThrowsEOFExceptionOnTruncatedStreams() throws IOException {
        final byte[] data = {
            1, 11, 0, -12, -1,
            'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l',
        };

        try (Deflate64CompressorInputStream input = new Deflate64CompressorInputStream(new ByteArrayInputStream(data));
             BufferedReader br = new BufferedReader(new InputStreamReader(input))) {
            assertThrows(EOFException.class, () -> br.readLine());
        }
    }

    @Test
    public void singleByteReadConsistentlyReturnsMinusOneAtEof() throws Exception {
        try (final Deflate64CompressorInputStream in =
                    new Deflate64CompressorInputStream(nullDecoder)) {
            IOUtils.toByteArray(in);
            assertEquals(-1, in.read());
            assertEquals(-1, in.read());
        }
    }

    @Test
    public void streamIgnoresExtraBytesAfterDeflatedInput() throws Exception {
        final byte[] data = {
            1, 11, 0, -12, -1,
            'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd', 'X'
        };

        try (Deflate64CompressorInputStream input = new Deflate64CompressorInputStream(new ByteArrayInputStream(data));
             BufferedReader br = new BufferedReader(new InputStreamReader(input))) {
            assertEquals("Hello World", br.readLine());
            assertNull(br.readLine());
        }
    }

    @Test
    public void uncompressedBlock() throws Exception {
        final byte[] data = {
            1, 11, 0, -12, -1,
            'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd'
        };

        try (Deflate64CompressorInputStream input = new Deflate64CompressorInputStream(new ByteArrayInputStream(data));
             BufferedReader br = new BufferedReader(new InputStreamReader(input))) {
            assertEquals("Hello World", br.readLine());
            assertNull(br.readLine());
        }
    }

    @Test
    public void uncompressedBlockAvailable() throws Exception {
        final byte[] data = {
            1, 11, 0, -12, -1,
            'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd'
        };

        try (Deflate64CompressorInputStream input = new Deflate64CompressorInputStream(new ByteArrayInputStream(data))) {
            assertEquals('H', input.read());
            assertEquals(10, input.available());
        }
    }

    @Test
    public void uncompressedBlockViaFactory() throws Exception {
        final byte[] data = {
            1, 11, 0, -12, -1,
            'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd'
        };

        try (InputStream input = new CompressorStreamFactory()
             .createCompressorInputStream(CompressorStreamFactory.DEFLATE64, new ByteArrayInputStream(data));
             BufferedReader br = new BufferedReader(new InputStreamReader(input))) {
            assertEquals("Hello World", br.readLine());
            assertNull(br.readLine());
        }
    }
}
