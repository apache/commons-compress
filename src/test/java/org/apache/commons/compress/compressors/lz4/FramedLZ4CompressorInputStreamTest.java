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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

public final class FramedLZ4CompressorInputStreamTest extends AbstractTest {

    interface StreamWrapper {
        InputStream wrap(InputStream in) throws Exception;
    }

    private static byte[] duplicate(final byte[] from) {
        final byte[] to = Arrays.copyOf(from, 2 * from.length);
        System.arraycopy(from, 0, to, from.length, from.length);
        return to;
    }

    private void expectIOException(final String fileName) {
        assertThrows(IOException.class, () -> {
            try (InputStream is = Files.newInputStream(getFile(fileName).toPath());
                    FramedLZ4CompressorInputStream in = new FramedLZ4CompressorInputStream(is)) {
                IOUtils.toByteArray(in);
            }
        });
    }

    private void readDoubledBlaLz4(final StreamWrapper wrapper, final boolean expectDuplicateOutput) throws Exception {
        final byte[] singleInput;
        try (InputStream i = newInputStream("bla.tar.lz4")) {
            singleInput = IOUtils.toByteArray(i);
        }
        final byte[] input = duplicate(singleInput);
        try (InputStream a = wrapper.wrap(new ByteArrayInputStream(input));
                InputStream e = newInputStream("bla.tar")) {
            final byte[] expected = IOUtils.toByteArray(e);
            final byte[] actual = IOUtils.toByteArray(a);
            assertArrayEquals(expectDuplicateOutput ? duplicate(expected) : expected, actual);
        }
    }

    @Test
    public void testBackreferenceAtStartCausesIOException() {
        expectIOException("COMPRESS-490/ArrayIndexOutOfBoundsException1.lz4");
    }

    @Test
    public void testBackreferenceOfSize0CausesIOException() {
        expectIOException("COMPRESS-490/ArithmeticException.lz4");
    }

    @Test
    public void testBackreferenceWithOffsetTooBigCausesIOException() {
        expectIOException("COMPRESS-490/ArrayIndexOutOfBoundsException2.lz4");
    }

    @Test
    public void testMatches() throws IOException {
        assertFalse(FramedLZ4CompressorInputStream.matches(new byte[10], 4));
        final byte[] expected = readAllBytes("bla.tar.lz4");
        assertFalse(FramedLZ4CompressorInputStream.matches(expected, 3));
        assertTrue(FramedLZ4CompressorInputStream.matches(expected, 4));
        assertTrue(FramedLZ4CompressorInputStream.matches(expected, 5));
    }

    @Test
    public void testMultiByteReadConsistentlyReturnsMinusOneAtEof() throws IOException {
        final File input = getFile("bla.tar.lz4");
        final byte[] buf = new byte[2];
        try (InputStream is = Files.newInputStream(input.toPath());
                FramedLZ4CompressorInputStream in = new FramedLZ4CompressorInputStream(is)) {
            IOUtils.toByteArray(in);
            assertEquals(-1, in.read(buf));
            assertEquals(-1, in.read(buf));
        }
    }

    @Test
    public void testReadBlaDumpLz4() throws IOException {
        try (InputStream a = new FramedLZ4CompressorInputStream(newInputStream("bla.dump.lz4"));
                InputStream e = newInputStream("bla.dump")) {
            final byte[] expected = IOUtils.toByteArray(e);
            final byte[] actual = IOUtils.toByteArray(a);
            assertArrayEquals(expected, actual);
        }
    }

    @Test
    public void testReadBlaLz4() throws IOException {
        try (InputStream a = new FramedLZ4CompressorInputStream(newInputStream("bla.tar.lz4"));
                InputStream e = newInputStream("bla.tar")) {
            final byte[] expected = IOUtils.toByteArray(e);
            final byte[] actual = IOUtils.toByteArray(a);
            assertArrayEquals(expected, actual);
        }
    }

    @Test
    public void testReadBlaLz4ViaFactory() throws Exception {
        try (InputStream a = new CompressorStreamFactory().createCompressorInputStream(CompressorStreamFactory.getLZ4Framed(), newInputStream("bla.tar.lz4"));
                InputStream e = newInputStream("bla.tar")) {
            final byte[] expected = IOUtils.toByteArray(e);
            final byte[] actual = IOUtils.toByteArray(a);
            assertArrayEquals(expected, actual);
        }
    }

    @Test
    public void testReadBlaLz4ViaFactoryAutoDetection() throws Exception {
        try (InputStream a = new CompressorStreamFactory().createCompressorInputStream(new BufferedInputStream(newInputStream("bla.tar.lz4")));
                InputStream e = newInputStream("bla.tar")) {
            final byte[] expected = IOUtils.toByteArray(e);
            final byte[] actual = IOUtils.toByteArray(a);
            assertArrayEquals(expected, actual);
        }
    }

    @Test
    public void testReadBlaLz4ViaFactoryWithDecompressConcatenated() throws Exception {
        try (InputStream a = new CompressorStreamFactory().createCompressorInputStream(CompressorStreamFactory.getLZ4Framed(), newInputStream("bla.tar.lz4"),
                true);
                InputStream e = newInputStream("bla.tar")) {
            final byte[] expected = IOUtils.toByteArray(e);
            final byte[] actual = IOUtils.toByteArray(a);
            assertArrayEquals(expected, actual);
        }
    }

    @Test
    public void testReadBlaLz4WithDecompressConcatenated() throws IOException {
        try (InputStream a = new FramedLZ4CompressorInputStream(newInputStream("bla.tar.lz4"), true);
                InputStream e = newInputStream("bla.tar")) {
            final byte[] expected = IOUtils.toByteArray(e);
            final byte[] actual = IOUtils.toByteArray(a);
            assertArrayEquals(expected, actual);
        }
    }

    @Test
    public void testReadDoubledBlaLz4ViaFactoryWithDecompressConcatenatedFalse() throws Exception {
        readDoubledBlaLz4(in -> new CompressorStreamFactory().createCompressorInputStream(CompressorStreamFactory.getLZ4Framed(), in, false), false);
    }

    @Test
    public void testReadDoubledBlaLz4ViaFactoryWithDecompressConcatenatedTrue() throws Exception {
        readDoubledBlaLz4(in -> new CompressorStreamFactory().createCompressorInputStream(CompressorStreamFactory.getLZ4Framed(), in, true), true);
    }

    @Test
    public void testReadDoubledBlaLz4ViaFactoryWithoutExplicitDecompressConcatenated() throws Exception {
        readDoubledBlaLz4(in -> new CompressorStreamFactory().createCompressorInputStream(CompressorStreamFactory.getLZ4Framed(), in), false);
    }

    @Test
    public void testReadDoubledBlaLz4WithDecompressConcatenatedFalse() throws Exception {
        readDoubledBlaLz4(in -> new FramedLZ4CompressorInputStream(in, false), false);
    }

    @Test
    public void testReadDoubledBlaLz4WithDecompressConcatenatedTrue() throws Exception {
        readDoubledBlaLz4(in -> new FramedLZ4CompressorInputStream(in, true), true);
    }

    @Test
    public void testReadDoubledBlaLz4WithoutExplicitDecompressConcatenated() throws Exception {
        readDoubledBlaLz4(FramedLZ4CompressorInputStream::new, false);
    }

    @Test
    public void testReadsUncompressedBlocks() throws IOException {
        final byte[] input = { 4, 0x22, 0x4d, 0x18, // signature
                0x60, // flag - Version 01, block independent, no block checksum, no content size, no content checksum
                0x70, // block size 4MB
                115, // checksum
                13, 0, 0, (byte) 0x80, // 13 bytes length and uncompressed bit set
                'H', 'e', 'l', 'l', 'o', ',', ' ', 'w', 'o', 'r', 'l', 'd', '!', // content
                0, 0, 0, 0, // empty block marker
        };
        try (InputStream a = new FramedLZ4CompressorInputStream(new ByteArrayInputStream(input))) {
            final byte[] actual = IOUtils.toByteArray(a);
            assertArrayEquals(new byte[] { 'H', 'e', 'l', 'l', 'o', ',', ' ', 'w', 'o', 'r', 'l', 'd', '!' }, actual);
        }
    }

    @Test
    public void testReadsUncompressedBlocksUsingSingleByteRead() throws IOException {
        final byte[] input = { 4, 0x22, 0x4d, 0x18, // signature
                0x60, // flag - Version 01, block independent, no block checksum, no content size, no content checksum
                0x70, // block size 4MB
                115, // checksum
                13, 0, 0, (byte) 0x80, // 13 bytes length and uncompressed bit set
                'H', 'e', 'l', 'l', 'o', ',', ' ', 'w', 'o', 'r', 'l', 'd', '!', // content
                0, 0, 0, 0, // empty block marker
        };
        try (InputStream a = new FramedLZ4CompressorInputStream(new ByteArrayInputStream(input))) {
            final int h = a.read();
            assertEquals('H', h);
        }
    }

    @Test
    public void testRejectsBlocksWithoutChecksum() {
        final byte[] input = { 4, 0x22, 0x4d, 0x18, // signature
                0x70, // flag - Version 01, block independent, with block checksum, no content size, no content checksum
                0x70, // block size 4MB
                114, // checksum
                13, 0, 0, (byte) 0x80, // 13 bytes length and uncompressed bit set
                'H', 'e', 'l', 'l', 'o', ',', ' ', 'w', 'o', 'r', 'l', 'd', '!', // content
        };
        final IOException ex = assertThrows(IOException.class, () -> {
            try (InputStream a = new FramedLZ4CompressorInputStream(new ByteArrayInputStream(input))) {
                IOUtils.toByteArray(a);
            }
        }, "expected exception");
        assertTrue(ex.getMessage().contains("block checksum"));
    }

    @Test
    public void testRejectsFileWithBadHeaderChecksum() {
        final byte[] input = { 4, 0x22, 0x4d, 0x18, // signature
                0x64, // flag - Version 01, block independent, no block checksum, no content size, with content checksum
                0x70, // block size 4MB
                0, };
        final IOException ex = assertThrows(IOException.class, () -> {
            try (InputStream a = new FramedLZ4CompressorInputStream(new ByteArrayInputStream(input))) {
            }
        }, "expected exception");
        assertTrue(ex.getMessage().contains("header checksum mismatch"));
    }

    @Test
    public void testRejectsFileWithInsufficientContentSize() {
        final byte[] input = { 4, 0x22, 0x4d, 0x18, // signature
                0x6C, // flag - Version 01, block independent, no block checksum, with content size, with content checksum
                0x70, // block size 4MB
        };
        final IOException ex = assertThrows(IOException.class, () -> {
            try (InputStream a = new FramedLZ4CompressorInputStream(new ByteArrayInputStream(input))) {
            }
        }, "expected exception");
        assertTrue(ex.getMessage().contains("content size"));
    }

    @Test
    public void testRejectsFileWithoutBlockSizeByte() {
        final byte[] input = { 4, 0x22, 0x4d, 0x18, // signature
                0x64, // flag - Version 01, block independent, no block checksum, no content size, with content checksum
        };
        final IOException ex = assertThrows(IOException.class, () -> {
            try (InputStream a = new FramedLZ4CompressorInputStream(new ByteArrayInputStream(input))) {
            }
        }, "expected exception");
        assertTrue(ex.getMessage().contains("BD byte"));
    }

    @Test
    public void testRejectsFileWithoutFrameDescriptor() {
        final byte[] input = { 4, 0x22, 0x4d, 0x18 // signature
        };
        final IOException ex = assertThrows(IOException.class, () -> {
            try (InputStream a = new FramedLZ4CompressorInputStream(new ByteArrayInputStream(input))) {
            }
        }, "expected exception");
        assertTrue(ex.getMessage().contains("frame flags"));
    }

    @Test
    public void testRejectsFileWithoutHeaderChecksum() {
        final byte[] input = { 4, 0x22, 0x4d, 0x18, // signature
                0x64, // flag - Version 01, block independent, no block checksum, no content size, with content checksum
                0x70, // block size 4MB
        };
        final IOException ex = assertThrows(IOException.class, () -> {
            try (InputStream a = new FramedLZ4CompressorInputStream(new ByteArrayInputStream(input))) {
            }
        }, "expected exception");
        assertTrue(ex.getMessage().contains("header checksum"));
    }

    @Test
    public void testRejectsFileWithWrongVersion() {
        final byte[] input = { 4, 0x22, 0x4d, 0x18, // signature
                0x24, // flag - Version 00, block independent, no block checksum, no content size, with content checksum
        };
        final IOException ex = assertThrows(IOException.class, () -> {
            try (InputStream a = new FramedLZ4CompressorInputStream(new ByteArrayInputStream(input))) {
            }
        }, "expected exception");
        assertTrue(ex.getMessage().contains("version"));
    }

    @Test
    public void testRejectsNonLZ4Stream() {
        assertThrows(IOException.class, () -> new FramedLZ4CompressorInputStream(newInputStream("bla.tar")));
    }

    @Test
    public void testRejectsSkippableFrameFollowedByJunk() {
        final byte[] input = { 4, 0x22, 0x4d, 0x18, // signature
                0x60, // flag - Version 01, block independent, no block checksum, no content size, no content checksum
                0x70, // block size 4MB
                115, // checksum
                13, 0, 0, (byte) 0x80, // 13 bytes length and uncompressed bit set
                'H', 'e', 'l', 'l', 'o', ',', ' ', 'w', 'o', 'r', 'l', 'd', '!', // content
                0, 0, 0, 0, // empty block marker
                0x50, 0x2a, 0x4d, 0x18, // skippable frame signature
                2, 0, 0, 0, // skippable frame has length 2
                1, 2, // content of skippable frame
                1, 0x22, 0x4d, 0x18, // bad signature
        };
        final IOException ex = assertThrows(IOException.class, () -> {
            try (InputStream a = new FramedLZ4CompressorInputStream(new ByteArrayInputStream(input), true)) {
                IOUtils.toByteArray(a);
            }
        }, "expected exception");
        assertTrue(ex.getMessage().contains("garbage"));
    }

    @Test
    public void testRejectsSkippableFrameFollowedByTooFewBytes() {
        final byte[] input = { 4, 0x22, 0x4d, 0x18, // signature
                0x60, // flag - Version 01, block independent, no block checksum, no content size, no content checksum
                0x70, // block size 4MB
                115, // checksum
                13, 0, 0, (byte) 0x80, // 13 bytes length and uncompressed bit set
                'H', 'e', 'l', 'l', 'o', ',', ' ', 'w', 'o', 'r', 'l', 'd', '!', // content
                0, 0, 0, 0, // empty block marker
                0x52, 0x2a, 0x4d, 0x18, // skippable frame signature
                2, 0, 0, 0, // skippable frame has length 2
                1, 2, // content of skippable frame
                4, // too short for signature
        };
        final IOException ex = assertThrows(IOException.class, () -> {
            try (InputStream a = new FramedLZ4CompressorInputStream(new ByteArrayInputStream(input), true)) {
                IOUtils.toByteArray(a);
            }
        }, "expected exception");
        assertTrue(ex.getMessage().contains("garbage"));
    }

    @Test
    public void testRejectsSkippableFrameWithBadSignaturePrefix() {
        final byte[] input = { 4, 0x22, 0x4d, 0x18, // signature
                0x60, // flag - Version 01, block independent, no block checksum, no content size, no content checksum
                0x70, // block size 4MB
                115, // checksum
                13, 0, 0, (byte) 0x80, // 13 bytes length and uncompressed bit set
                'H', 'e', 'l', 'l', 'o', ',', ' ', 'w', 'o', 'r', 'l', 'd', '!', // content
                0, 0, 0, 0, // empty block marker
                0x60, 0x2a, 0x4d, 0x18, // broken skippable frame signature
        };
        final IOException ex = assertThrows(IOException.class, () -> {
            try (InputStream a = new FramedLZ4CompressorInputStream(new ByteArrayInputStream(input), true)) {
                IOUtils.toByteArray(a);
                fail();
            }
        }, "expected exception");
        assertTrue(ex.getMessage().contains("garbage"));
    }

    @Test
    public void testRejectsSkippableFrameWithBadSignatureTrailer() {
        final byte[] input = { 4, 0x22, 0x4d, 0x18, // signature
                0x60, // flag - Version 01, block independent, no block checksum, no content size, no content checksum
                0x70, // block size 4MB
                115, // checksum
                13, 0, 0, (byte) 0x80, // 13 bytes length and uncompressed bit set
                'H', 'e', 'l', 'l', 'o', ',', ' ', 'w', 'o', 'r', 'l', 'd', '!', // content
                0, 0, 0, 0, // empty block marker
                0x51, 0x2a, 0x4d, 0x17, // broken skippable frame signature
        };
        final IOException ex = assertThrows(IOException.class, () -> {
            try (InputStream a = new FramedLZ4CompressorInputStream(new ByteArrayInputStream(input), true)) {
                IOUtils.toByteArray(a);
            }
        }, "expected exception");
        assertTrue(ex.getMessage().contains("garbage"));
    }

    @Test
    public void testRejectsSkippableFrameWithPrematureEnd() {
        final byte[] input = { 4, 0x22, 0x4d, 0x18, // signature
                0x60, // flag - Version 01, block independent, no block checksum, no content size, no content checksum
                0x70, // block size 4MB
                115, // checksum
                13, 0, 0, (byte) 0x80, // 13 bytes length and uncompressed bit set
                'H', 'e', 'l', 'l', 'o', ',', ' ', 'w', 'o', 'r', 'l', 'd', '!', // content
                0, 0, 0, 0, // empty block marker
                0x50, 0x2a, 0x4d, 0x18, // skippable frame signature
                2, 0, 0, 0, // skippable frame has length 2
                1, // content of skippable frame (should be two bytes)
        };
        final IOException ex = assertThrows(IOException.class, () -> {
            try (InputStream a = new FramedLZ4CompressorInputStream(new ByteArrayInputStream(input), true)) {
                IOUtils.toByteArray(a);
            }
        }, "expected exception");
        assertTrue(ex.getMessage().contains("Premature end of stream while skipping frame"));
    }

    @Test
    public void testRejectsSkippableFrameWithPrematureEndInLengthBytes() {
        final byte[] input = { 4, 0x22, 0x4d, 0x18, // signature
                0x60, // flag - Version 01, block independent, no block checksum, no content size, no content checksum
                0x70, // block size 4MB
                115, // checksum
                13, 0, 0, (byte) 0x80, // 13 bytes length and uncompressed bit set
                'H', 'e', 'l', 'l', 'o', ',', ' ', 'w', 'o', 'r', 'l', 'd', '!', // content
                0, 0, 0, 0, // empty block marker
                0x55, 0x2a, 0x4d, 0x18, // skippable frame signature
                2, 0, 0, // should be four byte length
        };
        final IOException ex = assertThrows(IOException.class, () -> {
            try (InputStream a = new FramedLZ4CompressorInputStream(new ByteArrayInputStream(input), true)) {
                IOUtils.toByteArray(a);
            }
        }, "expected exception");
        assertTrue(ex.getMessage().contains("Premature end of data"));
    }

    @Test
    public void testRejectsStreamsWithBadContentChecksum() {
        final byte[] input = { 4, 0x22, 0x4d, 0x18, // signature
                0x64, // flag - Version 01, block independent, no block checksum, no content size, with content checksum
                0x70, // block size 4MB
                (byte) 185, // checksum
                13, 0, 0, (byte) 0x80, // 13 bytes length and uncompressed bit set
                'H', 'e', 'l', 'l', 'o', ',', ' ', 'w', 'o', 'r', 'l', 'd', '!', // content
                0, 0, 0, 0, // empty block marker
                1, 2, 3, 4, };
        final IOException ex = assertThrows(IOException.class, () -> {
            try (InputStream a = new FramedLZ4CompressorInputStream(new ByteArrayInputStream(input))) {
                IOUtils.toByteArray(a);
            }
        }, "expected exception");
        assertTrue(ex.getMessage().contains("content checksum mismatch"));
    }

    @Test
    public void testRejectsStreamsWithoutContentChecksum() {
        final byte[] input = { 4, 0x22, 0x4d, 0x18, // signature
                0x64, // flag - Version 01, block independent, no block checksum, no content size, with content checksum
                0x70, // block size 4MB
                (byte) 185, // checksum
                13, 0, 0, (byte) 0x80, // 13 bytes length and uncompressed bit set
                'H', 'e', 'l', 'l', 'o', ',', ' ', 'w', 'o', 'r', 'l', 'd', '!', // content
                0, 0, 0, 0, // empty block marker
        };
        final IOException ex = assertThrows(IOException.class, () -> {
            try (InputStream a = new FramedLZ4CompressorInputStream(new ByteArrayInputStream(input))) {
                IOUtils.toByteArray(a);
            }
        }, "expected exception");
        assertTrue(ex.getMessage().contains("content checksum"));
    }

    @Test
    public void testRejectsTrailingBytesAfterValidFrame() {
        final byte[] input = { 4, 0x22, 0x4d, 0x18, // signature
                0x60, // flag - Version 01, block independent, no block checksum, no content size, no content checksum
                0x70, // block size 4MB
                115, // checksum
                13, 0, 0, (byte) 0x80, // 13 bytes length and uncompressed bit set
                'H', 'e', 'l', 'l', 'o', ',', ' ', 'w', 'o', 'r', 'l', 'd', '!', // content
                0, 0, 0, 0, // empty block marker
                0x56, 0x2a, 0x4d, // too short for any signature
        };
        final IOException ex = assertThrows(IOException.class, () -> {
            try (InputStream a = new FramedLZ4CompressorInputStream(new ByteArrayInputStream(input), true)) {
                IOUtils.toByteArray(a);
            }
        }, "expected exception");
        assertTrue(ex.getMessage().contains("garbage"));
    }

    @Test
    public void testSingleByteReadConsistentlyReturnsMinusOneAtEof() throws IOException {
        final File input = getFile("bla.tar.lz4");
        try (InputStream is = Files.newInputStream(input.toPath());
                FramedLZ4CompressorInputStream in = new FramedLZ4CompressorInputStream(is);) {
            IOUtils.toByteArray(in);
            assertEquals(-1, in.read());
            assertEquals(-1, in.read());
        }
    }

    @Test
    public void testSkipsOverSkippableFrames() throws IOException {
        final byte[] input = { 4, 0x22, 0x4d, 0x18, // signature
                0x60, // flag - Version 01, block independent, no block checksum, no content size, no content checksum
                0x70, // block size 4MB
                115, // checksum
                13, 0, 0, (byte) 0x80, // 13 bytes length and uncompressed bit set
                'H', 'e', 'l', 'l', 'o', ',', ' ', 'w', 'o', 'r', 'l', 'd', '!', // content
                0, 0, 0, 0, // empty block marker
                0x5f, 0x2a, 0x4d, 0x18, // skippable frame signature
                2, 0, 0, 0, // skippable frame has length 2
                1, 2, // content of skippable frame
                4, 0x22, 0x4d, 0x18, // signature
                0x60, // flag - Version 01, block independent, no block checksum, no content size, no content checksum
                0x70, // block size 4MB
                115, // checksum
                1, 0, 0, (byte) 0x80, // 1 bytes length and uncompressed bit set
                '!', // content
                0, 0, 0, 0, // empty block marker
        };
        try (InputStream a = new FramedLZ4CompressorInputStream(new ByteArrayInputStream(input), true)) {
            final byte[] actual = IOUtils.toByteArray(a);
            assertArrayEquals(new byte[] { 'H', 'e', 'l', 'l', 'o', ',', ' ', 'w', 'o', 'r', 'l', 'd', '!', '!' }, actual);
        }
    }

    @Test
    public void testSkipsOverTrailingSkippableFrames() throws IOException {
        final byte[] input = { 4, 0x22, 0x4d, 0x18, // signature
                0x60, // flag - Version 01, block independent, no block checksum, no content size, no content checksum
                0x70, // block size 4MB
                115, // checksum
                13, 0, 0, (byte) 0x80, // 13 bytes length and uncompressed bit set
                'H', 'e', 'l', 'l', 'o', ',', ' ', 'w', 'o', 'r', 'l', 'd', '!', // content
                0, 0, 0, 0, // empty block marker
                0x51, 0x2a, 0x4d, 0x18, // skippable frame signature
                2, 0, 0, 0, // skippable frame has length 2
                1, 2, // content of skippable frame
        };
        try (InputStream a = new FramedLZ4CompressorInputStream(new ByteArrayInputStream(input), true)) {
            final byte[] actual = IOUtils.toByteArray(a);
            assertArrayEquals(new byte[] { 'H', 'e', 'l', 'l', 'o', ',', ' ', 'w', 'o', 'r', 'l', 'd', '!' }, actual);
        }
    }

}
