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

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.rules.ExpectedException;

public final class FramedLZ4CompressorInputStreamTest
    extends AbstractTestCase {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void testMatches() throws IOException {
        assertFalse(FramedLZ4CompressorInputStream.matches(new byte[10], 4));
        final byte[] b = new byte[12];
        IOUtils.read(getFile("bla.tar.lz4"), b);
        assertFalse(FramedLZ4CompressorInputStream.matches(b, 3));
        assertTrue(FramedLZ4CompressorInputStream.matches(b, 4));
        assertTrue(FramedLZ4CompressorInputStream.matches(b, 5));
    }

    @Test
    public void readBlaLz4() throws IOException {
        try (InputStream a = new FramedLZ4CompressorInputStream(Files.newInputStream(getFile("bla.tar.lz4").toPath()));
            InputStream e = Files.newInputStream(getFile("bla.tar").toPath())) {
            final byte[] expected = IOUtils.toByteArray(e);
            final byte[] actual = IOUtils.toByteArray(a);
            assertArrayEquals(expected, actual);
        }
    }

    @Test
    public void readBlaLz4ViaFactory() throws Exception {
        try (InputStream a = new CompressorStreamFactory()
                 .createCompressorInputStream(CompressorStreamFactory.getLZ4Framed(),
                                              Files.newInputStream(getFile("bla.tar.lz4").toPath()));
            InputStream e = Files.newInputStream(getFile("bla.tar").toPath())) {
            final byte[] expected = IOUtils.toByteArray(e);
            final byte[] actual = IOUtils.toByteArray(a);
            assertArrayEquals(expected, actual);
        }
    }

    @Test
    public void readBlaLz4ViaFactoryAutoDetection() throws Exception {
        try (InputStream a = new CompressorStreamFactory()
                 .createCompressorInputStream(new BufferedInputStream(Files.newInputStream(getFile("bla.tar.lz4").toPath())));
            InputStream e = Files.newInputStream(getFile("bla.tar").toPath())) {
            final byte[] expected = IOUtils.toByteArray(e);
            final byte[] actual = IOUtils.toByteArray(a);
            assertArrayEquals(expected, actual);
        }
    }

    @Test
    public void readBlaLz4WithDecompressConcatenated() throws IOException {
        try (InputStream a = new FramedLZ4CompressorInputStream(Files.newInputStream(getFile("bla.tar.lz4").toPath()), true);
            InputStream e = Files.newInputStream(getFile("bla.tar").toPath())) {
            final byte[] expected = IOUtils.toByteArray(e);
            final byte[] actual = IOUtils.toByteArray(a);
            assertArrayEquals(expected, actual);
        }
    }

    @Test
    public void readDoubledBlaLz4WithDecompressConcatenatedTrue() throws Exception {
        readDoubledBlaLz4(in -> new FramedLZ4CompressorInputStream(in, true), true);
    }

    @Test
    public void readDoubledBlaLz4WithDecompressConcatenatedFalse() throws Exception {
        readDoubledBlaLz4(in -> new FramedLZ4CompressorInputStream(in, false), false);
    }

    @Test
    public void readDoubledBlaLz4WithoutExplicitDecompressConcatenated() throws Exception {
        readDoubledBlaLz4(FramedLZ4CompressorInputStream::new, false);
    }

    @Test
    public void readBlaLz4ViaFactoryWithDecompressConcatenated() throws Exception {
        try (InputStream a = new CompressorStreamFactory()
                 .createCompressorInputStream(CompressorStreamFactory.getLZ4Framed(),
                                              Files.newInputStream(getFile("bla.tar.lz4").toPath()),
                                              true);
            InputStream e = Files.newInputStream(getFile("bla.tar").toPath())) {
            final byte[] expected = IOUtils.toByteArray(e);
            final byte[] actual = IOUtils.toByteArray(a);
            assertArrayEquals(expected, actual);
        }
    }

    @Test
    public void readDoubledBlaLz4ViaFactoryWithDecompressConcatenatedTrue() throws Exception {
        readDoubledBlaLz4(in -> new CompressorStreamFactory()
            .createCompressorInputStream(CompressorStreamFactory.getLZ4Framed(), in, true), true);
    }

    @Test
    public void readDoubledBlaLz4ViaFactoryWithDecompressConcatenatedFalse() throws Exception {
        readDoubledBlaLz4(in -> new CompressorStreamFactory()
            .createCompressorInputStream(CompressorStreamFactory.getLZ4Framed(), in, false), false);
    }

    @Test
    public void readDoubledBlaLz4ViaFactoryWithoutExplicitDecompressConcatenated() throws Exception {
        readDoubledBlaLz4(in -> new CompressorStreamFactory()
            .createCompressorInputStream(CompressorStreamFactory.getLZ4Framed(), in), false);
    }

    @Test
    public void readBlaDumpLz4() throws IOException {
        try (InputStream a = new FramedLZ4CompressorInputStream(Files.newInputStream(getFile("bla.dump.lz4").toPath()));
            InputStream e = Files.newInputStream(getFile("bla.dump").toPath())) {
            final byte[] expected = IOUtils.toByteArray(e);
            final byte[] actual = IOUtils.toByteArray(a);
            assertArrayEquals(expected, actual);
        }
    }

    @Test
    public void rejectsNonLZ4Stream() throws IOException {
        try (InputStream a = new FramedLZ4CompressorInputStream(Files.newInputStream(getFile("bla.tar").toPath()))) {
             fail("expected exception");
        }
    }

    @Test
    public void rejectsFileWithoutFrameDescriptor() {
        final byte[] input = new byte[] {
            4, 0x22, 0x4d, 0x18 // signature
        };
        try {
            try (InputStream a = new FramedLZ4CompressorInputStream(new ByteArrayInputStream(input))) {
                fail("expected exception");
            }
        } catch (final IOException ex) {
            assertThat(ex.getMessage(), containsString("frame flags"));
        }
    }

    @Test
    public void rejectsFileWithoutBlockSizeByte() {
        final byte[] input = new byte[] {
            4, 0x22, 0x4d, 0x18, // signature
            0x64, // flag - Version 01, block independent, no block checksum, no content size, with content checksum
        };
        try {
            try (InputStream a = new FramedLZ4CompressorInputStream(new ByteArrayInputStream(input))) {
                fail("expected exception");
            }
        } catch (final IOException ex) {
            assertThat(ex.getMessage(), containsString("BD byte"));
        }
    }

    @Test
    public void rejectsFileWithWrongVersion() {
        final byte[] input = new byte[] {
            4, 0x22, 0x4d, 0x18, // signature
            0x24, // flag - Version 00, block independent, no block checksum, no content size, with content checksum
        };
        try {
            try (InputStream a = new FramedLZ4CompressorInputStream(new ByteArrayInputStream(input))) {
                fail("expected exception");
            }
        } catch (final IOException ex) {
            assertThat(ex.getMessage(), containsString("version"));
        }
    }

    @Test
    public void rejectsFileWithInsufficientContentSize() {
        final byte[] input = new byte[] {
            4, 0x22, 0x4d, 0x18, // signature
            0x6C, // flag - Version 01, block independent, no block checksum, with content size, with content checksum
            0x70, // block size 4MB
        };
        try {
            try (InputStream a = new FramedLZ4CompressorInputStream(new ByteArrayInputStream(input))) {
                fail("expected exception");
            }
        } catch (final IOException ex) {
            assertThat(ex.getMessage(), containsString("content size"));
        }
    }

    @Test
    public void rejectsFileWithoutHeaderChecksum() {
        final byte[] input = new byte[] {
            4, 0x22, 0x4d, 0x18, // signature
            0x64, // flag - Version 01, block independent, no block checksum, no content size, with content checksum
            0x70, // block size 4MB
        };
        try {
            try (InputStream a = new FramedLZ4CompressorInputStream(new ByteArrayInputStream(input))) {
                fail("expected exception");
            }
        } catch (final IOException ex) {
            assertThat(ex.getMessage(), containsString("header checksum"));
        }
    }

    @Test
    public void rejectsFileWithBadHeaderChecksum() {
        final byte[] input = new byte[] {
            4, 0x22, 0x4d, 0x18, // signature
            0x64, // flag - Version 01, block independent, no block checksum, no content size, with content checksum
            0x70, // block size 4MB
            0,
        };
        try {
            try (InputStream a = new FramedLZ4CompressorInputStream(new ByteArrayInputStream(input))) {
                fail("expected exception");
            }
        } catch (final IOException ex) {
            assertThat(ex.getMessage(), containsString("header checksum mismatch"));
        }
    }

    @Test
    public void readsUncompressedBlocks() throws IOException {
        final byte[] input = new byte[] {
            4, 0x22, 0x4d, 0x18, // signature
            0x60, // flag - Version 01, block independent, no block checksum, no content size, no content checksum
            0x70, // block size 4MB
            115, // checksum
            13, 0, 0, (byte) 0x80, // 13 bytes length and uncompressed bit set
            'H', 'e', 'l', 'l', 'o', ',', ' ', 'w', 'o', 'r', 'l', 'd', '!', // content
            0, 0, 0, 0, // empty block marker
        };
        try (InputStream a = new FramedLZ4CompressorInputStream(new ByteArrayInputStream(input))) {
            final byte[] actual = IOUtils.toByteArray(a);
            assertArrayEquals(new byte[] {
                    'H', 'e', 'l', 'l', 'o', ',', ' ', 'w', 'o', 'r', 'l', 'd', '!'
                }, actual);
        }
    }

    @Test
    public void readsUncompressedBlocksUsingSingleByteRead() throws IOException {
        final byte[] input = new byte[] {
            4, 0x22, 0x4d, 0x18, // signature
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
    public void rejectsBlocksWithoutChecksum() {
        final byte[] input = new byte[] {
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
        } catch (final IOException ex) {
            assertThat(ex.getMessage(), containsString("block checksum"));
        }
    }

    @Test
    public void rejectsStreamsWithoutContentChecksum() {
        final byte[] input = new byte[] {
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
        } catch (final IOException ex) {
            assertThat(ex.getMessage(), containsString("content checksum"));
        }
    }

    @Test
    public void rejectsStreamsWithBadContentChecksum() {
        final byte[] input = new byte[] {
            4, 0x22, 0x4d, 0x18, // signature
            0x64, // flag - Version 01, block independent, no block checksum, no content size, with content checksum
            0x70, // block size 4MB
            (byte) 185, // checksum
            13, 0, 0, (byte) 0x80, // 13 bytes length and uncompressed bit set
            'H', 'e', 'l', 'l', 'o', ',', ' ', 'w', 'o', 'r', 'l', 'd', '!', // content
            0, 0, 0, 0, // empty block marker
            1, 2, 3, 4,
        };
        try {
            try (InputStream a = new FramedLZ4CompressorInputStream(new ByteArrayInputStream(input))) {
                IOUtils.toByteArray(a);
                fail("expected exception");
            }
        } catch (final IOException ex) {
            assertThat(ex.getMessage(), containsString("content checksum mismatch"));
        }
    }

    @Test
    public void skipsOverSkippableFrames() throws IOException {
        final byte[] input = new byte[] {
            4, 0x22, 0x4d, 0x18, // signature
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
            assertArrayEquals(new byte[] {
                    'H', 'e', 'l', 'l', 'o', ',', ' ', 'w', 'o', 'r', 'l', 'd', '!', '!'
                }, actual);
        }
    }

    @Test
    public void skipsOverTrailingSkippableFrames() throws IOException {
        final byte[] input = new byte[] {
            4, 0x22, 0x4d, 0x18, // signature
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
            assertArrayEquals(new byte[] {
                    'H', 'e', 'l', 'l', 'o', ',', ' ', 'w', 'o', 'r', 'l', 'd', '!'
                }, actual);
        }
    }

    @Test
    public void rejectsSkippableFrameFollowedByJunk() {
        final byte[] input = new byte[] {
            4, 0x22, 0x4d, 0x18, // signature
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
        try {
            try (InputStream a = new FramedLZ4CompressorInputStream(new ByteArrayInputStream(input), true)) {
                IOUtils.toByteArray(a);
                fail("expected exception");
            }
        } catch (final IOException ex) {
            assertThat(ex.getMessage(), containsString("garbage"));
        }
    }

    @Test
    public void rejectsSkippableFrameFollowedByTooFewBytes() {
        final byte[] input = new byte[] {
            4, 0x22, 0x4d, 0x18, // signature
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
        try {
            try (InputStream a = new FramedLZ4CompressorInputStream(new ByteArrayInputStream(input), true)) {
                IOUtils.toByteArray(a);
                fail("expected exception");
            }
        } catch (final IOException ex) {
            assertThat(ex.getMessage(), containsString("garbage"));
        }
    }

    @Test
    public void rejectsSkippableFrameWithPrematureEnd() {
        final byte[] input = new byte[] {
            4, 0x22, 0x4d, 0x18, // signature
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
        try {
            try (InputStream a = new FramedLZ4CompressorInputStream(new ByteArrayInputStream(input), true)) {
                IOUtils.toByteArray(a);
                fail("expected exception");
            }
        } catch (final IOException ex) {
            assertThat(ex.getMessage(), containsString("Premature end of stream while skipping frame"));
        }
    }

    @Test
    public void rejectsSkippableFrameWithPrematureEndInLengthBytes() {
        final byte[] input = new byte[] {
            4, 0x22, 0x4d, 0x18, // signature
            0x60, // flag - Version 01, block independent, no block checksum, no content size, no content checksum
            0x70, // block size 4MB
            115, // checksum
            13, 0, 0, (byte) 0x80, // 13 bytes length and uncompressed bit set
            'H', 'e', 'l', 'l', 'o', ',', ' ', 'w', 'o', 'r', 'l', 'd', '!', // content
            0, 0, 0, 0, // empty block marker
            0x55, 0x2a, 0x4d, 0x18, // skippable frame signature
            2, 0, 0, // should be four byte length
        };
        try {
            try (InputStream a = new FramedLZ4CompressorInputStream(new ByteArrayInputStream(input), true)) {
                IOUtils.toByteArray(a);
                fail("expected exception");
            }
        } catch (final IOException ex) {
            assertThat(ex.getMessage(), containsString("Premature end of data"));
        }
    }

    @Test
    public void rejectsSkippableFrameWithBadSignatureTrailer() {
        final byte[] input = new byte[] {
            4, 0x22, 0x4d, 0x18, // signature
            0x60, // flag - Version 01, block independent, no block checksum, no content size, no content checksum
            0x70, // block size 4MB
            115, // checksum
            13, 0, 0, (byte) 0x80, // 13 bytes length and uncompressed bit set
            'H', 'e', 'l', 'l', 'o', ',', ' ', 'w', 'o', 'r', 'l', 'd', '!', // content
            0, 0, 0, 0, // empty block marker
            0x51, 0x2a, 0x4d, 0x17, // broken skippable frame signature
        };
        try {
            try (InputStream a = new FramedLZ4CompressorInputStream(new ByteArrayInputStream(input), true)) {
                IOUtils.toByteArray(a);
                fail("expected exception");
            }
        } catch (final IOException ex) {
            assertThat(ex.getMessage(), containsString("garbage"));
        }
    }

    @Test
    public void rejectsSkippableFrameWithBadSignaturePrefix() {
        final byte[] input = new byte[] {
            4, 0x22, 0x4d, 0x18, // signature
            0x60, // flag - Version 01, block independent, no block checksum, no content size, no content checksum
            0x70, // block size 4MB
            115, // checksum
            13, 0, 0, (byte) 0x80, // 13 bytes length and uncompressed bit set
            'H', 'e', 'l', 'l', 'o', ',', ' ', 'w', 'o', 'r', 'l', 'd', '!', // content
            0, 0, 0, 0, // empty block marker
            0x60, 0x2a, 0x4d, 0x18, // broken skippable frame signature
        };
        try {
            try (InputStream a = new FramedLZ4CompressorInputStream(new ByteArrayInputStream(input), true)) {
                IOUtils.toByteArray(a);
                fail("expected exception");
            }
        } catch (final IOException ex) {
            assertThat(ex.getMessage(), containsString("garbage"));
        }
    }

    @Test
    public void rejectsTrailingBytesAfterValidFrame() {
        final byte[] input = new byte[] {
            4, 0x22, 0x4d, 0x18, // signature
            0x60, // flag - Version 01, block independent, no block checksum, no content size, no content checksum
            0x70, // block size 4MB
            115, // checksum
            13, 0, 0, (byte) 0x80, // 13 bytes length and uncompressed bit set
            'H', 'e', 'l', 'l', 'o', ',', ' ', 'w', 'o', 'r', 'l', 'd', '!', // content
            0, 0, 0, 0, // empty block marker
            0x56, 0x2a, 0x4d, // too short for any signature
        };
        try {
            try (InputStream a = new FramedLZ4CompressorInputStream(new ByteArrayInputStream(input), true)) {
                IOUtils.toByteArray(a);
                fail("expected exception");
            }
        } catch (final IOException ex) {
            assertThat(ex.getMessage(), containsString("garbage"));
        }
    }

    @Test
    public void singleByteReadConsistentlyReturnsMinusOneAtEof() throws IOException {
        final File input = getFile("bla.tar.lz4");
        try (InputStream is = Files.newInputStream(input.toPath())) {
            final FramedLZ4CompressorInputStream in =
                    new FramedLZ4CompressorInputStream(is);
            IOUtils.toByteArray(in);
            assertEquals(-1, in.read());
            assertEquals(-1, in.read());
            in.close();
        }
    }

    @Test
    public void multiByteReadConsistentlyReturnsMinusOneAtEof() throws IOException {
        final File input = getFile("bla.tar.lz4");
        final byte[] buf = new byte[2];
        try (InputStream is = Files.newInputStream(input.toPath())) {
            final FramedLZ4CompressorInputStream in =
                    new FramedLZ4CompressorInputStream(is);
            IOUtils.toByteArray(in);
            assertEquals(-1, in.read(buf));
            assertEquals(-1, in.read(buf));
            in.close();
        }
    }

    @Test
    public void backreferenceOfSize0CausesIOException() throws IOException {
        expectIOException("COMPRESS-490/ArithmeticException.lz4");
    }

    @Test
    public void backreferenceAtStartCausesIOException() throws IOException {
        expectIOException("COMPRESS-490/ArrayIndexOutOfBoundsException1.lz4");
    }

    @Test
    public void backreferenceWithOffsetTooBigCausesIOException() throws IOException {
        expectIOException("COMPRESS-490/ArrayIndexOutOfBoundsException2.lz4");
    }

    interface StreamWrapper {
        InputStream wrap(InputStream in) throws Exception;
    }

    private void readDoubledBlaLz4(final StreamWrapper wrapper, final boolean expectDuplicateOutput) throws Exception {
        byte[] singleInput;
        try (InputStream i = Files.newInputStream(getFile("bla.tar.lz4").toPath())) {
            singleInput = IOUtils.toByteArray(i);
        }
        final byte[] input = duplicate(singleInput);
        try (InputStream a = wrapper.wrap(new ByteArrayInputStream(input));
            InputStream e = Files.newInputStream(getFile("bla.tar").toPath())) {
            final byte[] expected = IOUtils.toByteArray(e);
            final byte[] actual = IOUtils.toByteArray(a);
            assertArrayEquals(expectDuplicateOutput ? duplicate(expected) : expected, actual);
        }
    }

    private static byte[] duplicate(final byte[] from) {
        final byte[] to = Arrays.copyOf(from, 2 * from.length);
        System.arraycopy(from, 0, to, from.length, from.length);
        return to;
    }

    private void expectIOException(final String fileName) throws IOException {
        thrown.expect(IOException.class);
        try (InputStream is = Files.newInputStream(getFile(fileName).toPath())) {
            final FramedLZ4CompressorInputStream in = new FramedLZ4CompressorInputStream(is);
            IOUtils.toByteArray(in);
        }
    }

}
