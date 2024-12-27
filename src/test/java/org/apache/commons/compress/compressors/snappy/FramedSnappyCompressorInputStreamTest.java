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
package org.apache.commons.compress.compressors.snappy;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

public final class FramedSnappyCompressorInputStreamTest extends AbstractTest {

    private static byte[] generateTestData(final int inputSize) {
        final byte[] arr = new byte[inputSize];
        for (int i = 0; i < arr.length; i++) {
          arr[i] = (byte) (65 + i % 10);
        }

        return arr;
    }

    private long mask(final long x) {
        return (x >>> 15 | x << 17) + FramedSnappyCompressorInputStream.MASK_OFFSET & 0xffffFFFFL;
    }

    @Test
    public void testAvailable() throws Exception {
        try (InputStream isSz = newInputStream("mixed.txt.sz");
                FramedSnappyCompressorInputStream in = new FramedSnappyCompressorInputStream(isSz)) {
            assertEquals(0, in.available()); // no chunk read so far
            assertEquals('1', in.read());
            assertEquals(3, in.available()); // remainder of first uncompressed block
            assertEquals(3, in.read(new byte[5], 0, 3));
            assertEquals('5', in.read());
            assertEquals(0, in.available()); // end of chunk, must read next one
            assertEquals(4, in.read(new byte[5], 0, 4));
            assertEquals('5', in.read());
        }
    }

    @Test
    public void testChecksumUnmasking() {
        testChecksumUnmasking(0xc757L);
        testChecksumUnmasking(0xffffc757L);
    }

    private void testChecksumUnmasking(final long x) {
        assertEquals(Long.toHexString(x), Long.toHexString(FramedSnappyCompressorInputStream.unmask(mask(x))));
    }

    @Test
    public void testFinishWithNoWrite() throws IOException {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (FramedSnappyCompressorOutputStream compressor = new FramedSnappyCompressorOutputStream(buffer)) {
            // do nothing here. this will test that flush on close doesn't throw any exceptions if no data is written.
        }
        assertTrue(buffer.size() == 10, "Only the signature gets written.");
    }

    /**
     * Something big enough to make buffers slide.
     */
    @Test
    public void testLoremIpsum() throws Exception {
        final Path outputSz = newTempPath("lorem-ipsum.1");
        final Path outputGz = newTempPath("lorem-ipsum.2");
        try (InputStream isSz = newInputStream("lorem-ipsum.txt.sz")) {
            try (InputStream in = new FramedSnappyCompressorInputStream(isSz)) {
                Files.copy(in, outputSz);
            }
            try (InputStream isGz = newInputStream("lorem-ipsum.txt.gz");
                    InputStream in = new GzipCompressorInputStream(isGz)) {
                Files.copy(in, outputGz);
            }
        }

        assertArrayEquals(Files.readAllBytes(outputSz), Files.readAllBytes(outputGz));
    }

    @Test
    public void testMatches() throws IOException {
        assertFalse(FramedSnappyCompressorInputStream.matches(new byte[10], 10));
        final byte[] expected = readAllBytes("bla.tar.sz");
        assertFalse(FramedSnappyCompressorInputStream.matches(expected, 9));
        assertTrue(FramedSnappyCompressorInputStream.matches(expected, 10));
        assertTrue(FramedSnappyCompressorInputStream.matches(expected, 12));
    }

    @Test
    public void testMultiByteReadConsistentlyReturnsMinusOneAtEof() throws IOException {
        final File input = getFile("bla.tar.sz");
        final byte[] buf = new byte[2];
        try (InputStream is = Files.newInputStream(input.toPath());
                FramedSnappyCompressorInputStream in = new FramedSnappyCompressorInputStream(is);) {
            IOUtils.toByteArray(in);
            assertEquals(-1, in.read(buf));
            assertEquals(-1, in.read(buf));
        }
    }

    @Test
    public void testReadIWAFile() throws Exception {
        try (ZipFile zip = ZipFile.builder().setFile(getFile("testNumbersNew.numbers")).get()) {
            try (InputStream is = zip.getInputStream(zip.getEntry("Index/Document.iwa"))) {
                try (FramedSnappyCompressorInputStream in = new FramedSnappyCompressorInputStream(is, FramedSnappyDialect.IWORK_ARCHIVE)) {
                    Files.copy(in, newTempFile("snappyIWATest.raw").toPath());
                }
            }
        }
    }

    /**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-358"
     */
    @Test
    public void testReadIWAFileWithBiggerOffset() throws Exception {
        final File o = newTempFile("COMPRESS-358.raw");
        try (InputStream is = newInputStream("COMPRESS-358.iwa");
                FramedSnappyCompressorInputStream in = new FramedSnappyCompressorInputStream(is, 1 << 16, FramedSnappyDialect.IWORK_ARCHIVE);) {
            Files.copy(in, o.toPath());
        }
        try (InputStream a = Files.newInputStream(o.toPath());
                InputStream e = newInputStream("COMPRESS-358.uncompressed")) {
            final byte[] expected = IOUtils.toByteArray(e);
            final byte[] actual = IOUtils.toByteArray(a);
            assertArrayEquals(expected, actual);
        }
    }

    @Test
    public void testRemainingChunkTypes() throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                InputStream isSz = newInputStream("mixed.txt.sz");
                FramedSnappyCompressorInputStream in = new FramedSnappyCompressorInputStream(isSz);) {
            IOUtils.copy(in, out);
            assertArrayEquals(new byte[] { '1', '2', '3', '4', '5', '6', '7', '8', '9', '5', '6', '7', '8', '9', '5', '6', '7', '8', '9', '5', '6', '7', '8',
                    '9', '5', '6', '7', '8', '9', 10, '1', '2', '3', '4', '1', '2', '3', '4', }, out.toByteArray());
        }
    }

    @Test
    public void testSingleByteReadConsistentlyReturnsMinusOneAtEof() throws IOException {
        final File input = getFile("bla.tar.sz");
        try (InputStream is = Files.newInputStream(input.toPath());
                FramedSnappyCompressorInputStream in = new FramedSnappyCompressorInputStream(is);) {
            IOUtils.toByteArray(in);
            assertEquals(-1, in.read());
            assertEquals(-1, in.read());
        }
    }

    @Test
    public void testUnskippableChunk() {
        final byte[] input = { (byte) 0xff, 6, 0, 0, 's', 'N', 'a', 'P', 'p', 'Y', 2, 2, 0, 0, 1, 1 };
        try (FramedSnappyCompressorInputStream in = new FramedSnappyCompressorInputStream(new ByteArrayInputStream(input))) {
            final IOException exception = assertThrows(IOException.class, () -> in.read());
            assertTrue(exception.getMessage().contains("Unskippable chunk"));
        } catch (final IOException ex) {
        }
    }

    @Test
    public void testWriteByteArrayVsWriteByte() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        final byte[] bytes = "abcdefghijklmnop".getBytes();
        try (FramedSnappyCompressorOutputStream compressor = new FramedSnappyCompressorOutputStream(buffer)) {
            compressor.write(bytes);
            compressor.finish();
            compressor.close();
            assertTrue(compressor.isClosed());
        }
        final byte[] bulkOutput = buffer.toByteArray();
        buffer = new ByteArrayOutputStream();
        try (FramedSnappyCompressorOutputStream compressor = new FramedSnappyCompressorOutputStream(buffer)) {
            for (final byte element : bytes) {
                compressor.write(element);
            }
            compressor.finish();
            compressor.close();
            assertTrue(compressor.isClosed());
        }
        assertArrayEquals(bulkOutput, buffer.toByteArray());
    }

    @Test
    public void testWriteDataLargerThanBufferOneCall() throws IOException {
        final int inputSize = 500_000;
        final byte[] data = generateTestData(inputSize);
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (FramedSnappyCompressorOutputStream compressor = new FramedSnappyCompressorOutputStream(outputStream)) {
            compressor.write(data, 0, data.length);
            compressor.finish();
        }
        final byte[] compressed = outputStream.toByteArray();

        byte[] decompressed = {};
        try (ByteArrayInputStream bytesIn = new ByteArrayInputStream(compressed, 0, compressed.length);
                 FramedSnappyCompressorInputStream decompressor = new FramedSnappyCompressorInputStream(bytesIn)) {
            int i;
            final ByteArrayOutputStream decompressedOutputStream = new ByteArrayOutputStream();
            while (-1 != (i = decompressor.read())) {
                decompressedOutputStream.write(i);
            }
            decompressed = decompressedOutputStream.toByteArray();
        }
        assertArrayEquals(data, decompressed);
    }

}
