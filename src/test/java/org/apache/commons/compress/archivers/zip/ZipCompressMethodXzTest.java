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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;
import org.apache.commons.compress.compressors.xz.XZUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ZipCompressMethodXzTest extends AbstractTest {

    private static final int DEFAULT_LEVEL = 6;

    @TempDir
    static Path tempDir;

    /**
     * Reads uncompressed data stream and writes it compressed to the output.
     *
     * @param input  the data stream with compressed data
     * @param output the data stream for compressed output
     * @throws IOException throws the exception which could be got from from IOUtils.copyLarge() or XZCompressorOutputStream constructor
     */
    private static void compress(final InputStream input, final OutputStream output) throws IOException {
        @SuppressWarnings("resource")
        final XZCompressorOutputStream outputStream = new XZCompressorOutputStream(output, DEFAULT_LEVEL);
        IOUtils.copyLarge(input, outputStream);
        outputStream.flush();
    }

    @Test
    public void testXzInputStream() throws IOException {
        // test-method-xz.zip was created with:
        // "\Program Files\7-Zip\7z.exe" a test-method-xz.zip -mm=xz LICENSE.txt
        // The "mm" option specifies the compress method
        final Path file = getPath("org/apache/commons/compress/zip/test-method-xz.zip");
        try (ZipFile zip = ZipFile.builder().setPath(file).get()) {
            final ZipArchiveEntry entry = zip.getEntries().nextElement();
            assertEquals("LICENSE.txt", entry.getName());
            assertTrue(zip.canReadEntryData(entry));
            assertEquals(ZipMethod.XZ.getCode(), entry.getMethod());
            try (InputStream inputStream = zip.getInputStream(entry)) {
                final long actualSize = entry.getSize();
                final byte[] buf = new byte[(int) actualSize];
                inputStream.read(buf);
                final String text = new String(buf);
                assertTrue(text.startsWith("                                 Apache License"), text);
                assertTrue(text.endsWith("   limitations under the License.\n"), text);
                assertEquals(11357, text.length());
            }
        }
    }

    @Test
    public void testXzMethodInZipFile() throws IOException {
        final String zipContentFile = "testXzMethodInZipFile.txt";
        final byte[] text = "The quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.UTF_8);
        final Path file = tempDir.resolve("testXzMethodInZipFile.zip");
        // Create the Zip File
        try (ZipArchiveOutputStream zipOutputStream = new ZipArchiveOutputStream(file)) {
            final ZipArchiveEntry archiveEntry = new ZipArchiveEntry(zipContentFile);
            archiveEntry.setMethod(ZipMethod.XZ.getCode());
            archiveEntry.setSize(text.length);
            zipOutputStream.putArchiveEntry(archiveEntry);
            compress(new ByteArrayInputStream(text), zipOutputStream);
            zipOutputStream.closeArchiveEntry();
        }
        // Read the Zip File
        try (ZipFile zipFile = ZipFile.builder().setPath(file).get()) {
            // Find the entry
            final ZipArchiveEntry entry = zipFile.getEntry(zipContentFile);
            // Check the compression method
            assertEquals(entry.getMethod(), ZipMethod.XZ.getCode());
            @SuppressWarnings("resource")
            final InputStream inputStream = zipFile.getInputStream(entry);
            assertTrue(inputStream instanceof XZCompressorInputStream);
            final long dataOffset = entry.getDataOffset();
            final int uncompressedSize = (int) entry.getSize();
            assertEquals(text.length, uncompressedSize);
            final byte[] uncompressedData = new byte[uncompressedSize];
            inputStream.read(uncompressedData, 0, uncompressedSize);
            // Check the uncompressed data
            assertEquals(new String(text), new String(uncompressedData));
            try (InputStream fileInputStream = Files.newInputStream(file)) {
                fileInputStream.skip(dataOffset);
                final byte[] compressedData = new byte[6];
                fileInputStream.read(compressedData);
                assertTrue(XZUtils.matches(compressedData, 6));
            }
        }
    }

    @Test
    public void testXzMethodWriteRead() throws IOException {
        final String zipContentFile = "testXzMethodWriteRead.txt";
        final byte[] text = "The quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.UTF_8);
        final Path file = tempDir.resolve("testXzMethodWriteRead.zip");
        // Create the Zip File
        try (ZipArchiveOutputStream zipOutputStream = new ZipArchiveOutputStream(file)) {
            final ZipArchiveEntry archiveEntry = new ZipArchiveEntry(zipContentFile);
            archiveEntry.setMethod(ZipMethod.XZ.getCode());
            archiveEntry.setSize(text.length);
            zipOutputStream.putArchiveEntry(archiveEntry);
            compress(new ByteArrayInputStream(text), zipOutputStream);
            zipOutputStream.closeArchiveEntry();
        }
        // Read the Zip File
        try (ZipFile zipFile = ZipFile.builder().setPath(file).get()) {
            // Find the entry
            final ZipArchiveEntry entry = zipFile.getEntry(zipContentFile);
            // Check the compression method
            assertEquals(entry.getMethod(), ZipMethod.XZ.getCode());
            @SuppressWarnings("resource")
            final InputStream inputStream = zipFile.getInputStream(entry);
            assertTrue(inputStream instanceof XZCompressorInputStream);
        }
    }
}
