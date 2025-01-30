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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdUtils;
import org.junit.jupiter.api.Test;

public class ZstdCompressorTest extends AbstractTest {

    @Test
    public void testZstdInputStream() throws IOException {
        final Path file = getPath("COMPRESS-692/compress-962.zip");
        try (ZipFile zip = ZipFile.builder().setFile(file.toFile()).get()) {
            final ZipArchiveEntry entry = zip.getEntries().nextElement();
            assertEquals("Unexpected first entry", "dolor.txt", entry.getName());
            assertTrue("entry can't be read", zip.canReadEntryData(entry));
            assertEquals("Unexpected method", ZipMethod.ZSTD.getCode(), entry.getMethod());

            try (InputStream inputStream = zip.getInputStream(entry)) {
                final long uncompSize = entry.getSize();
                final byte[] buf = new byte[(int) uncompSize];
                inputStream.read(buf);

                final String uncompData = new String(buf);
                assertTrue(uncompData.startsWith("dolor sit amet"));
                assertTrue(uncompData.endsWith("ex ea commodo"));
                assertEquals(6066, uncompData.length());
            }

        }
    }

    @Test
    public void testZstdMethodInZipFile() throws IOException {
        final String zipContentFile = "Name.txt";
        final byte[] simpleText = "This is a Simple Test File.".getBytes();
        final File file = Files.createTempFile("", ".zip").toFile();
        // Create the Zip File
        {
            try (ZipArchiveOutputStream zipOutputStream = new ZipArchiveOutputStream(file)) {
                final ZipArchiveEntry archiveEntry = new ZipArchiveEntry(zipContentFile);
                archiveEntry.setMethod(ZipMethod.ZSTD.getCode());
                archiveEntry.setSize(simpleText.length);
                zipOutputStream.putArchiveEntry(archiveEntry);
                ZstdUtils.readAndCompressWrite(new ByteArrayInputStream(simpleText), zipOutputStream);
                zipOutputStream.closeArchiveEntry();
            }
        }

        // Read the Zip File
        {
            try (ZipFile zipFile = ZipFile.builder().setFile(file).get()) {
                // Find the entry
                final ZipArchiveEntry entry = zipFile.getEntry(zipContentFile);

                // Check the Zstd compression method
                assertEquals(entry.getMethod(), ZipMethod.ZSTD.getCode());
                final InputStream inputStream = zipFile.getInputStream(entry);
                assertTrue(inputStream instanceof ZstdCompressorInputStream);

                final long dataOffset = entry.getDataOffset();
                final int uncompressedSize = (int) entry.getSize();

                assertEquals(simpleText.length, uncompressedSize);

                final byte[] uncompressedData = new byte[uncompressedSize];
                inputStream.read(uncompressedData, 0, uncompressedSize);

                // Check the uncompressed data
                assertEquals(new String(simpleText), new String(uncompressedData));

                try (FileInputStream fileInputStream = new FileInputStream(file)) {
                    fileInputStream.skip(dataOffset);
                    final byte[] compressedData = new byte[4];
                    fileInputStream.read(compressedData);
                    assertTrue("Compressed data must begin with the magic bytes of Zstd", ZstdUtils.matches(compressedData, 4));
                }
            }
        }
    }

    @Test
    public void testZstdDeprecatedMethod() throws IOException {
        final String zipContentFile = "Name.txt";
        final byte[] simpleText = "This is a Simple Test File.".getBytes();
        final File file = Files.createTempFile("", ".zip").toFile();
        // Create the Zip File
        {
            try (ZipArchiveOutputStream zipOutputStream = new ZipArchiveOutputStream(file)) {
                final ZipArchiveEntry archiveEntry = new ZipArchiveEntry(zipContentFile);
                archiveEntry.setMethod(ZipMethod.ZSTD_DEPRECATED.getCode());
                archiveEntry.setSize(simpleText.length);
                zipOutputStream.putArchiveEntry(archiveEntry);
                ZstdUtils.readAndCompressWrite(new ByteArrayInputStream(simpleText), zipOutputStream);
                zipOutputStream.closeArchiveEntry();
            }
        }

        // Read the Zip File
        {
            try (ZipFile zipFile = ZipFile.builder().setFile(file).get()) {
                // Find the entry
                final ZipArchiveEntry entry = zipFile.getEntry(zipContentFile);

                // Check the Zstd compression method
                assertEquals(entry.getMethod(), ZipMethod.ZSTD_DEPRECATED.getCode());
                final InputStream inputStream = zipFile.getInputStream(entry);

                assertTrue("Input stream must be a ZstdInputStream", inputStream instanceof ZstdCompressorInputStream);
            }
        }
    }
}
