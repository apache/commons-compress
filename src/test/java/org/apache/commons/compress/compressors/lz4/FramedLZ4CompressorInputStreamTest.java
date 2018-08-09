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
import java.util.Arrays;

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

    @Test
    public void readBlaLz4WithDecompressConcatenated() throws IOException {
        try (InputStream a = new FramedLZ4CompressorInputStream(new FileInputStream(getFile("bla.tar.lz4")), true);
            FileInputStream e = new FileInputStream(getFile("bla.tar"))) {
            byte[] expected = IOUtils.toByteArray(e);
            byte[] actual = IOUtils.toByteArray(a);
            assertArrayEquals(expected, actual);
        }
    }

    @Test
    public void readDoubledBlaLz4WithDecompressConcatenatedTrue() throws Exception {
        readDoubledBlaLz4(new StreamWrapper() {
                @Override
                public InputStream wrap(InputStream in) throws Exception {
                    return new FramedLZ4CompressorInputStream(in, true);
                }
            }, true);
    }

    @Test
    public void readDoubledBlaLz4WithDecompressConcatenatedFalse() throws Exception {
        readDoubledBlaLz4(new StreamWrapper() {
                @Override
                public InputStream wrap(InputStream in) throws Exception {
                    return new FramedLZ4CompressorInputStream(in, false);
                }
            }, false);
    }

    @Test
    public void readDoubledBlaLz4WithoutExplicitDecompressConcatenated() throws Exception {
        readDoubledBlaLz4(new StreamWrapper() {
                @Override
                public InputStream wrap(InputStream in) throws Exception {
                    return new FramedLZ4CompressorInputStream(in);
                }
            }, false);
    }

    @Test
    public void readBlaLz4ViaFactoryWithDecompressConcatenated() throws Exception {
        try (InputStream a = new CompressorStreamFactory()
                 .createCompressorInputStream(CompressorStreamFactory.getLZ4Framed(),
                                              new FileInputStream(getFile("bla.tar.lz4")),
                                              true);
            FileInputStream e = new FileInputStream(getFile("bla.tar"))) {
            byte[] expected = IOUtils.toByteArray(e);
            byte[] actual = IOUtils.toByteArray(a);
            assertArrayEquals(expected, actual);
        }
    }

    @Test
    public void readDoubledBlaLz4ViaFactoryWithDecompressConcatenatedTrue() throws Exception {
        readDoubledBlaLz4(new StreamWrapper() {
                @Override
                public InputStream wrap(InputStream in) throws Exception {
                    return new CompressorStreamFactory()
                        .createCompressorInputStream(CompressorStreamFactory.getLZ4Framed(), in, true);
                }
            }, true);
    }

    @Test
    public void readDoubledBlaLz4ViaFactoryWithDecompressConcatenatedFalse() throws Exception {
        readDoubledBlaLz4(new StreamWrapper() {
                @Override
                public InputStream wrap(InputStream in) throws Exception {
                    return new CompressorStreamFactory()
                        .createCompressorInputStream(CompressorStreamFactory.getLZ4Framed(), in, false);
                }
            }, false);
    }

    @Test
    public void readDoubledBlaLz4ViaFactoryWithoutExplicitDecompressConcatenated() throws Exception {
        readDoubledBlaLz4(new StreamWrapper() {
                @Override
                public InputStream wrap(InputStream in) throws Exception {
                    return new CompressorStreamFactory()
                        .createCompressorInputStream(CompressorStreamFactory.getLZ4Framed(), in);
                }
            }, false);
    }

    @Test
    public void readBlaDumpLz4() throws IOException {
        try (InputStream a = new FramedLZ4CompressorInputStream(new FileInputStream(getFile("bla.dump.lz4")));
            FileInputStream e = new FileInputStream(getFile("bla.dump"))) {
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

    @Test
    public void rejectsStreamsWithBadContentChecksum() throws IOException {
        byte[] input = new byte[] {
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
        } catch (IOException ex) {
            assertThat(ex.getMessage(), containsString("content checksum mismatch"));
        }
    }

    @Test
    public void skipsOverSkippableFrames() throws IOException {
        byte[] input = new byte[] {
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
            byte[] actual = IOUtils.toByteArray(a);
            assertArrayEquals(new byte[] {
                    'H', 'e', 'l', 'l', 'o', ',', ' ', 'w', 'o', 'r', 'l', 'd', '!', '!'
                }, actual);
        }
    }

    @Test
    public void skipsOverTrailingSkippableFrames() throws IOException {
        byte[] input = new byte[] {
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
            byte[] actual = IOUtils.toByteArray(a);
            assertArrayEquals(new byte[] {
                    'H', 'e', 'l', 'l', 'o', ',', ' ', 'w', 'o', 'r', 'l', 'd', '!'
                }, actual);
        }
    }

    @Test
    public void rejectsSkippableFrameFollowedByJunk() throws IOException {
        byte[] input = new byte[] {
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
        } catch (IOException ex) {
            assertThat(ex.getMessage(), containsString("garbage"));
        }
    }

    @Test
    public void rejectsSkippableFrameFollowedByTooFewBytes() throws IOException {
        byte[] input = new byte[] {
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
        } catch (IOException ex) {
            assertThat(ex.getMessage(), containsString("garbage"));
        }
    }

    @Test
    public void rejectsSkippableFrameWithPrematureEnd() throws IOException {
        byte[] input = new byte[] {
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
        } catch (IOException ex) {
            assertThat(ex.getMessage(), containsString("Premature end of stream while skipping frame"));
        }
    }

    @Test
    public void rejectsSkippableFrameWithPrematureEndInLengthBytes() throws IOException {
        byte[] input = new byte[] {
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
        } catch (IOException ex) {
            assertThat(ex.getMessage(), containsString("premature end of data"));
        }
    }

    @Test
    public void rejectsSkippableFrameWithBadSignatureTrailer() throws IOException {
        byte[] input = new byte[] {
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
        } catch (IOException ex) {
            assertThat(ex.getMessage(), containsString("garbage"));
        }
    }

    @Test
    public void rejectsSkippableFrameWithBadSignaturePrefix() throws IOException {
        byte[] input = new byte[] {
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
        } catch (IOException ex) {
            assertThat(ex.getMessage(), containsString("garbage"));
        }
    }

    @Test
    public void rejectsTrailingBytesAfterValidFrame() throws IOException {
        byte[] input = new byte[] {
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
        } catch (IOException ex) {
            assertThat(ex.getMessage(), containsString("garbage"));
        }
    }

    @Test
    public void singleByteReadConsistentlyReturnsMinusOneAtEof() throws IOException {
        final File input = getFile("bla.tar.lz4");
        try (InputStream is = new FileInputStream(input)) {
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
        byte[] buf = new byte[2];
        try (InputStream is = new FileInputStream(input)) {
            final FramedLZ4CompressorInputStream in =
                    new FramedLZ4CompressorInputStream(is);
            IOUtils.toByteArray(in);
            assertEquals(-1, in.read(buf));
            assertEquals(-1, in.read(buf));
            in.close();
        }
    }

    interface StreamWrapper {
        InputStream wrap(InputStream in) throws Exception;
    }

    private void readDoubledBlaLz4(StreamWrapper wrapper, boolean expectDuplicateOutput) throws Exception {
        byte[] singleInput;
        try (InputStream i = new FileInputStream(getFile("bla.tar.lz4"))) {
            singleInput = IOUtils.toByteArray(i);
        }
        byte[] input = duplicate(singleInput);
        try (InputStream a = wrapper.wrap(new ByteArrayInputStream(input));
            FileInputStream e = new FileInputStream(getFile("bla.tar"))) {
            byte[] expected = IOUtils.toByteArray(e);
            byte[] actual = IOUtils.toByteArray(a);
            assertArrayEquals(expectDuplicateOutput ? duplicate(expected) : expected, actual);
        }
    }

    private static byte[] duplicate(byte[] from) {
        byte[] to = Arrays.copyOf(from, 2 * from.length);
        System.arraycopy(from, 0, to, from.length, from.length);
        return to;
    }
}
