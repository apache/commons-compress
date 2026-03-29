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
package org.apache.commons.compress.archivers.zip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;

import org.apache.commons.compress.AbstractTempDirTest;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link ZipArchiveOutputStream}.
 */
class ZipArchiveOutputStreamTest extends AbstractTempDirTest {

    @Test
    void testFileBasics() throws IOException {
        final ZipArchiveOutputStream ref;
        try (ZipArchiveOutputStream outputStream = new ZipArchiveOutputStream(createTempFile())) {
            ref = outputStream;
            assertTrue(outputStream.isSeekable());
        }
        assertTrue(ref.isClosed());
    }

    @Test
    void testOptionDefaults() throws IOException {
        final ZipArchiveOutputStream ref;
        try (ZipArchiveOutputStream outputStream = new ZipArchiveOutputStream(createTempFile())) {
            ref = outputStream;
            assertTrue(outputStream.isSeekable());
            outputStream.setComment("");
            outputStream.setLevel(Deflater.DEFAULT_COMPRESSION);
            outputStream.setMethod(ZipEntry.DEFLATED);
            outputStream.setFallbackToUTF8(false);
        }
        assertTrue(ref.isClosed());
    }

    @Test
    void testOutputStreamBasics() throws IOException {
        try (ZipArchiveOutputStream outputStream = new ZipArchiveOutputStream(new ByteArrayOutputStream())) {
            assertFalse(outputStream.isSeekable());
        }
    }

    @Test
    void testSetEncoding() throws IOException {
        try (ZipArchiveOutputStream outputStream = new ZipArchiveOutputStream(createTempFile())) {
            outputStream.setEncoding(StandardCharsets.UTF_8.name());
            assertEquals(StandardCharsets.UTF_8.name(), outputStream.getEncoding());
            outputStream.setEncoding(null);
            assertEquals(Charset.defaultCharset().name(), outputStream.getEncoding());
        }
    }

    @Test
    void testBuilderSeekablePath() throws IOException {
        try (ZipArchiveOutputStream zos = ZipArchiveOutputStream.builder(createTempFile().toPath()).setUseZip64(Zip64Mode.Never).build()) {
            assertTrue(zos.isSeekable());
        }
    }

    @Test
    void testBuilderPayloadWriterRoundtrip() throws IOException {
        final ByteArrayOutputStream raw = new ByteArrayOutputStream();
        final byte[] plain = "zip builder bzip2".getBytes(StandardCharsets.UTF_8);
        try (ZipArchiveOutputStream zos = ZipArchiveOutputStream.builder(raw)
                .setEncoding(StandardCharsets.UTF_8)
                .setUseZip64(Zip64Mode.Never)
                .setCompressionPayloadWriterFactory(ZipMethod.BZIP2.getCode(), ZipCompressionPayloadWriters.bzip2())
                .build()) {
            assertFalse(zos.isSeekable());
            final ZipArchiveEntry e = new ZipArchiveEntry("b.txt");
            e.setMethod(ZipMethod.BZIP2.getCode());
            zos.putArchiveEntry(e);
            zos.write(plain);
            zos.closeArchiveEntry();
        }
        try (SeekableByteChannel ch = new SeekableInMemoryByteChannel(raw.toByteArray());
                ZipFile zf = ZipFile.builder().setSeekableByteChannel(ch).get()) {
            final ZipArchiveEntry e = zf.getEntry("b.txt");
            assertEquals(ZipMethod.BZIP2.getCode(), e.getMethod());
            assertEquals(plain.length, e.getSize());
            try (InputStream in = zf.getInputStream(e)) {
                assertTrue(in instanceof BZip2CompressorInputStream);
                assertEquals(new String(plain, StandardCharsets.UTF_8), new String(IOUtils.toByteArray(in), StandardCharsets.UTF_8));
            }
        }
    }
}
