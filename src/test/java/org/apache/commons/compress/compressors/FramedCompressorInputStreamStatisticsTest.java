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
package org.apache.commons.compress.compressors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.compress.utils.InputStreamStatistics;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public final class FramedCompressorInputStreamStatisticsTest {

    private static byte[] compress(final String name, final byte[] input) throws IOException {
        final ByteArrayOutputStream encoded = new ByteArrayOutputStream();
        try (CompressorOutputStream<?> out = new CompressorStreamFactory().createCompressorOutputStream(name, encoded)) {
            out.write(input);
        }
        return encoded.toByteArray();
    }

    private static byte[] payload(final int length) {
        final byte[] bytes = new byte[length];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (i % 251);
        }
        return bytes;
    }

    @ParameterizedTest
    @ValueSource(strings = { CompressorStreamFactory.LZ4_FRAMED, CompressorStreamFactory.SNAPPY_FRAMED })
    void testCountAcrossFrames(final String name) throws IOException {
        final byte[] first = payload(5000);
        final byte[] second = payload(7000);
        final ByteArrayOutputStream encoded = new ByteArrayOutputStream();
        encoded.write(compress(name, first));
        encoded.write(compress(name, new byte[0]));
        // Skippable metadata between frames must not be counted as output.
        encoded.write(CompressorStreamFactory.LZ4_FRAMED.equals(name)
                ? new byte[] { 0x50, 0x2a, 0x4d, 0x18, 2, 0, 0, 0, 1, 2 }
                : new byte[] { (byte) 0x80, 2, 0, 0, 1, 2 });
        encoded.write(compress(name, second));
        try (CompressorInputStream in = new CompressorStreamFactory().createCompressorInputStream(name,
                new ByteArrayInputStream(encoded.toByteArray()), true)) {
            final byte[] actualFirst = new byte[first.length];
            assertEquals(first.length, IOUtils.read(in, actualFirst));
            assertArrayEquals(first, actualFirst);
            assertEquals(first.length, in.getUncompressedCount());
            assertArrayEquals(second, IOUtils.toByteArray(in));
            assertEquals(first.length + second.length, in.getUncompressedCount());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { CompressorStreamFactory.LZ4_FRAMED, CompressorStreamFactory.SNAPPY_FRAMED })
    void testCountAfterReadFailure(final String name) throws IOException {
        final byte[] expected = payload(5000);
        final byte[] frame = compress(name, expected);
        // A truncated next LZ4 frame or Snappy chunk, after a complete valid frame.
        final byte[] encoded = Arrays.copyOf(frame, frame.length + 1);
        try (CompressorInputStream in = new CompressorStreamFactory().createCompressorInputStream(name,
                new ByteArrayInputStream(encoded), true)) {
            final byte[] actual = new byte[expected.length];
            assertEquals(expected.length, IOUtils.read(in, actual));
            assertArrayEquals(expected, actual);
            assertEquals(expected.length, in.getUncompressedCount());
            assertThrows(IOException.class, in::read);
            assertEquals(expected.length, in.getUncompressedCount());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { CompressorStreamFactory.LZ4_FRAMED, CompressorStreamFactory.SNAPPY_FRAMED })
    void testCountAfterSkip(final String name) throws IOException {
        final byte[] expected = payload(20_000);
        try (CompressorInputStream in = new CompressorStreamFactory().createCompressorInputStream(name,
                new ByteArrayInputStream(compress(name, expected)))) {
            assertEquals(5000, in.skip(5000));
            assertEquals(5000, in.getUncompressedCount());
            assertEquals(0, in.skip(0));
            assertEquals(0, in.skip(-1));
            assertEquals(5000, in.getUncompressedCount());
            assertEquals(expected[5000] & 0xff, in.read());
            assertEquals(5001, in.getUncompressedCount());
            assertEquals(expected.length - 5001, in.skip(expected.length));
            assertEquals(expected.length, in.getUncompressedCount());
            assertEquals(-1, in.read());
            assertEquals(expected.length, in.getUncompressedCount());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { CompressorStreamFactory.LZ4_FRAMED, CompressorStreamFactory.SNAPPY_FRAMED })
    void testCountWhileReading(final String name) throws IOException {
        final byte[] expected = payload(98_304);
        final byte[] encoded = compress(name, expected);
        assertTrue(encoded.length < expected.length);
        try (CompressorInputStream in = new CompressorStreamFactory().createCompressorInputStream(name, new ByteArrayInputStream(encoded))) {
            final ByteArrayOutputStream actual = new ByteArrayOutputStream();
            assertEquals(expected[0] & 0xff, in.read());
            actual.write(expected[0]);
            assertEquals(1, in.getUncompressedCount());
            final byte[] buffer = new byte[41];
            int read;
            while ((read = in.read(buffer, 3, 31)) != -1) {
                assertTrue(read > 0);
                actual.write(buffer, 3, read);
                assertEquals(actual.size(), in.getUncompressedCount());
            }
            assertArrayEquals(expected, actual.toByteArray());
            assertEquals(0, in.read(buffer, 3, 0));
            assertEquals(-1, in.read());
            assertEquals(-1, in.read(buffer));
            assertEquals(expected.length, in.getUncompressedCount());
            assertEquals(encoded.length, ((InputStreamStatistics) in).getCompressedCount());
            assertEquals(encoded.length, in.getBytesRead());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { CompressorStreamFactory.LZ4_FRAMED, CompressorStreamFactory.SNAPPY_FRAMED })
    void testEmptyStreamCount(final String name) throws IOException {
        try (CompressorInputStream in = new CompressorStreamFactory().createCompressorInputStream(name,
                new ByteArrayInputStream(compress(name, new byte[0])))) {
            final InputStreamStatistics statistics = (InputStreamStatistics) in;
            assertTrue(statistics.getCompressedCount() > 0);
            assertEquals(0, statistics.getUncompressedCount());
            final byte[] buffer = new byte[1];
            assertEquals(0, in.read(buffer, 0, 0));
            assertEquals(-1, in.read());
            assertEquals(-1, in.read(buffer));
            assertEquals(0, in.read(buffer, 0, 0));
            assertEquals(0, in.getUncompressedCount());
        }
    }
}
