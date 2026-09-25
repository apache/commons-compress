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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.pack200.Pack200CompressorInputStream;
import org.apache.commons.compress.utils.InputStreamStatistics;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link CompressorInputStream}.
 */
class CompressorInputStreamTest {

    /**
     * Relies on the base implementation of {@link #getCompressedCount()}.
     */
    private static final class DefaultCountStream extends CompressorInputStream {

        @Override
        public int read() {
            return -1;
        }
    }

    /**
     * Implements the interface directly, as external subclasses written before 1.29.0 may, and keeps the checked exception it permits.
     */
    private static final class DirectStatisticsStream extends CompressorInputStream implements InputStreamStatistics {

        @Override
        public long getCompressedCount() throws IOException {
            return 42;
        }

        @Override
        public int read() {
            return -1;
        }
    }

    /**
     * Keeps the checked exception the interface permits.
     */
    private static final class ThrowingCountStream extends CompressorInputStream {

        @Override
        public long getCompressedCount() throws IOException {
            throw new IOException("count");
        }

        @Override
        public int read() {
            return -1;
        }
    }

    @Test
    void testCompressedCountDefaultsToUnknown() throws Exception {
        try (DefaultCountStream in = new DefaultCountStream()) {
            assertEquals(-1, in.getCompressedCount());
        }
    }

    @Test
    void testCompressedCountOfConcreteStream() throws Exception {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GzipCompressorOutputStream out = new GzipCompressorOutputStream(bos)) {
            out.write(new byte[1000]);
        }
        final byte[] gz = bos.toByteArray();
        try (CompressorInputStream in = new CompressorStreamFactory().createCompressorInputStream(CompressorStreamFactory.GZIP, new ByteArrayInputStream(gz))) {
            assertEquals(1000, IOUtils.consume(in));
            assertTrue(in.getCompressedCount() > 0);
            assertTrue(in.getCompressedCount() <= gz.length);
            assertEquals(1000, in.getUncompressedCount());
        }
    }

    @Test
    void testImplementsInputStreamStatistics() throws Exception {
        try (DefaultCountStream in = new DefaultCountStream()) {
            assertInstanceOf(InputStreamStatistics.class, in);
        }
    }

    @Test
    void testPack200CompressedCountIsUnknown() throws Exception {
        try (InputStream resource = CompressorInputStreamTest.class.getResourceAsStream("/pack200/HelloWorld.pack");
                Pack200CompressorInputStream in = new Pack200CompressorInputStream(resource)) {
            assertInstanceOf(InputStreamStatistics.class, in);
            assertEquals(-1, in.getCompressedCount());
        }
    }

    @Test
    void testSubclassImplementingInterfaceMayDeclareIOException() throws Exception {
        try (DirectStatisticsStream in = new DirectStatisticsStream()) {
            assertEquals(42, in.getCompressedCount());
        }
    }

    @Test
    void testSubclassMayDeclareIOException() throws Exception {
        try (ThrowingCountStream in = new ThrowingCountStream()) {
            assertThrows(IOException.class, in::getCompressedCount);
        }
    }
}
