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
package org.apache.commons.compress.compressors.lz4;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.Test;

public final class FramedLZ4CompressorInputStreamTest
    extends AbstractTestCase {

    @Test
    public void testMatches() throws IOException {
        assertFalse(FramedLZ4CompressorInputStream.matches(new byte[10], 4));
        final byte[] b = new byte[12];
        final File input = getFile("bla.tar.lz4");
        try (FileInputStream in = new FileInputStream(input)) {
            IOUtils.readFully(in, b);
        }
        assertFalse(FramedLZ4CompressorInputStream.matches(b, 3));
        assertTrue(FramedLZ4CompressorInputStream.matches(b, 4));
        assertTrue(FramedLZ4CompressorInputStream.matches(b, 5));
    }

    @Test
    public void readBlaLz4() throws IOException {
        try (InputStream a = new FramedLZ4CompressorInputStream(new FileInputStream(getFile("bla.tar.lz4")));
            FileInputStream e = new FileInputStream(getFile("bla.tar"))) {
            byte[] expected = IOUtils.toByteArray(e);
            byte[] actual = IOUtils.toByteArray(a);
            assertArrayEquals(expected, actual);
        }
    }

    @Test
    public void readBlaLz4ViaFactory() throws Exception {
        try (InputStream a = new CompressorStreamFactory()
                 .createCompressorInputStream(CompressorStreamFactory.getLZ4Framed(),
                                              new FileInputStream(getFile("bla.tar.lz4")));
            FileInputStream e = new FileInputStream(getFile("bla.tar"))) {
            byte[] expected = IOUtils.toByteArray(e);
            byte[] actual = IOUtils.toByteArray(a);
            assertArrayEquals(expected, actual);
        }
    }

    @Test
    public void readBlaLz4ViaFactoryAutoDetection() throws Exception {
        try (InputStream a = new CompressorStreamFactory()
                 .createCompressorInputStream(new BufferedInputStream(new FileInputStream(getFile("bla.tar.lz4"))));
            FileInputStream e = new FileInputStream(getFile("bla.tar"))) {
            byte[] expected = IOUtils.toByteArray(e);
            byte[] actual = IOUtils.toByteArray(a);
            assertArrayEquals(expected, actual);
        }
    }

    @Test(expected = IOException.class)
    public void rejectsNonLZ4Stream() throws IOException {
        try (InputStream a = new FramedLZ4CompressorInputStream(new FileInputStream(getFile("bla.tar")))) {
             fail("expected exception");
        }
    }

    @Test
    public void rejectsFileWithoutFrameDescriptor() throws IOException {
        byte[] input = new byte[] {
            4, 0x22, 0x4d, 0x18 // signature
        };
        try {
            try (InputStream a = new FramedLZ4CompressorInputStream(new ByteArrayInputStream(input))) {
                fail("expected exception");
            }
        } catch (IOException ex) {
            assertThat(ex.getMessage(), containsString("frame flags"));
        }
    }

    @Test
    public void rejectsFileWithoutBlockSizeByte() throws IOException {
        byte[] input = new byte[] {
            4, 0x22, 0x4d, 0x18, // signature
            0x64, // flag - Version 01, block independent, no block checksum, no content size, with content checksum
        };
        try {
            try (InputStream a = new FramedLZ4CompressorInputStream(new ByteArrayInputStream(input))) {
                fail("expected exception");
            }
        } catch (IOException ex) {
            assertThat(ex.getMessage(), containsString("BD byte"));
        }
    }

    @Test
    public void rejectsFileWithWrongVersion() throws IOException {
        byte[] input = new byte[] {
            4, 0x22, 0x4d, 0x18, // signature
            0x24, // flag - Version 00, block independent, no block checksum, no content size, with content checksum
        };
        try {
            try (InputStream a = new FramedLZ4CompressorInputStream(new ByteArrayInputStream(input))) {
                fail("expected exception");
            }
        } catch (IOException ex) {
            assertThat(ex.getMessage(), containsString("version"));
        }
    }

    @Test
    public void rejectsFileWithInsufficientContentSize() throws IOException {
        byte[] input = new byte[] {
            4, 0x22, 0x4d, 0x18, // signature
            0x6C, // flag - Version 01, block independent, no block checksum, with content size, with content checksum
            0x70, // block size 4MB
        };
        try {
            try (InputStream a = new FramedLZ4CompressorInputStream(new ByteArrayInputStream(input))) {
                fail("expected exception");
            }
        } catch (IOException ex) {
            assertThat(ex.getMessage(), containsString("content size"));
        }
    }

    @Test
    public void rejectsFileWithoutHeaderChecksum() throws IOException {
        byte[] input = new byte[] {
            4, 0x22, 0x4d, 0x18, // signature
            0x64, // flag - Version 01, block independent, no block checksum, no content size, with content checksum
            0x70, // block size 4MB
        };
        try {
            try (InputStream a = new FramedLZ4CompressorInputStream(new ByteArrayInputStream(input))) {
                fail("expected exception");
            }
        } catch (IOException ex) {
            assertThat(ex.getMessage(), containsString("header checksum"));
        }
    }

    @Test
    public void rejectsFileWithBadHeaderChecksum() throws IOException {
        byte[] input = new byte[] {
            4, 0x22, 0x4d, 0x18, // signature
            0x64, // flag - Version 01, block independent, no block checksum, no content size, with content checksum
            0x70, // block size 4MB
            0,
        };
        try {
            try (InputStream a = new FramedLZ4CompressorInputStream(new ByteArrayInputStream(input))) {
                fail("expected exception");
            }
        } catch (IOException ex) {
            assertThat(ex.getMessage(), containsString("header checksum mismatch"));
        }
    }

    @Test
    public void readsUncompressedBlocks() throws IOException {
        byte[] input = new byte[] {
            4, 0x22, 0x4d, 0x18, // signature
            0x60, // flag - Version 01, block independent, no block checksum, no content size, no content checksum
            0x70, // block size 4MB
            115, // checksum
            13, 0, 0, (byte) 0x80, // 13 bytes length and uncompressed bit set
            'H', 'e', 'l', 'l', 'o', ',', ' ', 'w', 'o', 'r', 'l', 'd', '!', // content
            0, 0, 0, 0, // empty block marker
        };
        try (InputStream a = new FramedLZ4CompressorInputStream(new ByteArrayInputStream(input))) {
            byte[] actual = IOUtils.toByteArray(a);
            assertArrayEquals(new byte[] {
                    'H', 'e', 'l', 'l', 'o', ',', ' ', 'w', 'o', 'r', 'l', 'd', '!'
                }, actual);
        }
    }

    @Test
    public void readsUncompressedBlocksUsingSingleByteRead() throws IOException {
        byte[] input = new byte[] {
            4, 0x22, 0x4d, 0x18, // signature
            0x60, // flag - Version 01, block independent, no block checksum, no content size, no content checksum
            0x70, // block size 4MB
            115, // checksum
            13, 0, 0, (byte) 0x80, // 13 bytes length and uncompressed bit set
            'H', 'e', 'l', 'l', 'o', ',', ' ', 'w', 'o', 'r', 'l', 'd', '!', // content
            0, 0, 0, 0, // empty block marker
        };
        try (InputStream a = new FramedLZ4CompressorInputStream(new ByteArrayInputStream(input))) {
            int h = a.read();
            assertEquals('H', h);
        }
    }

    @Test
    public void rejectsBlocksWithoutChecksum() throws IOException {
        byte[] input = new byte[] {
            4, 0x22, 0x4d, 0x18, // signature
            0x70, // flag - Version 01, block independent, with block checksum, no content size, no content checksum
            0x70, // block size 4MB
            114, // checksum
            13, 0, 0, (byte) 0x80, // 13 bytes length and uncompressed bit set
            'H', 'e', 'l', 'l', 'o', ',', ' ', 'w', 'o', 'r', 'l', 'd', '!', // content
        };
        try {
            try (InputStream a = new FramedLZ4CompressorInputStream(new ByteArrayInputStream(input))) {
                IOUtils.toByteArray(a);
                fail("expected exception");
            }
        } catch (IOException ex) {
            assertThat(ex.getMessage(), containsString("block checksum"));
        }
    }

    @Test
    public void rejectsStreamsWithoutContentChecksum() throws IOException {
        byte[] input = new byte[] {
            4, 0x22, 0x4d, 0x18, // signature
            0x64, // flag - Version 01, block independent, no block checksum, no content size, with content checksum
            0x70, // block size 4MB
            (byte) 185, // checksum
            13, 0, 0, (byte) 0x80, // 13 bytes length and uncompressed bit set
            'H', 'e', 'l', 'l', 'o', ',', ' ', 'w', 'o', 'r', 'l', 'd', '!', // content
            0, 0, 0, 0, // empty block marker
        };
        try {
            try (InputStream a = new FramedLZ4CompressorInputStream(new ByteArrayInputStream(input))) {
                IOUtils.toByteArray(a);
                fail("expected exception");
            }
        } catch (IOException ex) {
            assertThat(ex.getMessage(), containsString("content checksum"));
        }
    }
}
