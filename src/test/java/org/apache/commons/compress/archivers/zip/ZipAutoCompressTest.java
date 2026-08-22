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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ZipAutoCompressTest extends AbstractTest {

    private static final byte[] TEST_DATA_1 = "This is test data for auto-compress.\nThis is a second line.".getBytes();
    private static final byte[] TEST_DATA_2 = "The second entry for auto-compress.\nAnother line.".getBytes();

    @ParameterizedTest
    @EnumSource(names = {"BZIP2", "XZ", "ZSTD", "ZSTD_DEPRECATED"})
    void testAutoCompressZstd(ZipMethod zipMethod) throws Exception {
        File file = new File(getTempDirFile(), "autocompress.zip");
        try (ZipArchiveOutputStream out = ZipArchiveOutputStream.builder()
                .setFile(file)
                .setAutoCompress(true)
                .get()) {
            ZipArchiveEntry entry1 = new ZipArchiveEntry("test1.txt");
            entry1.setMethod(zipMethod.getCode());
            out.putArchiveEntry(entry1);
            out.write(TEST_DATA_1);
            ZipArchiveEntry entry2 = new ZipArchiveEntry("test2.txt");
            entry2.setMethod(zipMethod.getCode());
            out.putArchiveEntry(entry2);
            out.write(TEST_DATA_2);
        }
        assertZipFile(file, zipMethod);
    }

    @Test
    void testAutoCompressZstdWithCustomConfig() throws Exception {
        File file = new File(getTempDirFile(), "autocompress.zip");
        try (ZipArchiveOutputStream out = ZipArchiveOutputStream.builder()
                .setOutputStream(Files.newOutputStream(file.toPath()))
                .setAutoCompress(true)
                .get()) {
            ZipArchiveEntry entry1 = new ZipArchiveEntry("test1.txt");
            entry1.setMethod(ZipMethod.ZSTD.getCode());
            entry1.setCompressorConfig(new ZstdCompressorConfig(6, true));
            out.putArchiveEntry(entry1);
            out.write(TEST_DATA_1);
            ZipArchiveEntry entry2 = new ZipArchiveEntry("test2.txt");
            entry2.setMethod(ZipMethod.ZSTD.getCode());
            entry2.setCompressorConfig(new ZstdCompressorConfig(2, true));
            out.putArchiveEntry(entry2);
            out.write(TEST_DATA_2);
        }
        assertZipFile(file, ZipMethod.ZSTD);
    }

    @Test
    void testAutoCompressDeflatedStillWorks() throws Exception {
        File file = new File(getTempDirFile(), "autocompress.zip");
        try (ZipArchiveOutputStream out = ZipArchiveOutputStream.builder()
                .setPath(file.toPath())
                .setAutoCompress(true)
                .get()) {
            ZipArchiveEntry entry1 = new ZipArchiveEntry("test1.txt");
            entry1.setMethod(ZipEntry.DEFLATED);
            out.putArchiveEntry(entry1);
            out.write(TEST_DATA_1);
            ZipArchiveEntry entry2 = new ZipArchiveEntry("test2.txt");
            entry2.setMethod(ZipEntry.DEFLATED);
            out.putArchiveEntry(entry2);
            out.write(TEST_DATA_2);
        }
        assertZipFile(file, ZipMethod.DEFLATED);
    }

    @Test
    void testAutoCompressStoredStillWorks() throws Exception {
        File file = new File(getTempDirFile(), "autocompress.zip");
        try (FileOutputStream fos = new FileOutputStream(file);
             ZipArchiveOutputStream out = ZipArchiveOutputStream.builder()
                     .setChannel(fos.getChannel())
                     .setAutoCompress(true)
                     .get()) {
            ZipArchiveEntry entry1 = new ZipArchiveEntry("test1.txt");
            entry1.setMethod(ZipEntry.STORED);
            entry1.setSize(TEST_DATA_1.length);
            entry1.setCrc(getCrc(TEST_DATA_1));
            out.putArchiveEntry(entry1);
            out.write(TEST_DATA_1);
            ZipArchiveEntry entry2 = new ZipArchiveEntry("test2.txt");
            entry2.setMethod(ZipEntry.STORED);
            entry2.setSize(TEST_DATA_2.length);
            entry2.setCrc(getCrc(TEST_DATA_2));
            out.putArchiveEntry(entry2);
            out.write(TEST_DATA_2);
        }
        assertZipFile(file, ZipMethod.STORED);
    }

    private static long getCrc(byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(data, 0, data.length);
        return crc.getValue();
    }

    @Test
    void testAutoCompressFalsePreservesLegacyBehavior() throws Exception {
        File file = new File(getTempDirFile(), "autocompress.zip");
        try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(file)) {
            ZipArchiveEntry entry1 = new ZipArchiveEntry("test1.txt");
            entry1.setMethod(ZipMethod.ZSTD.getCode());
            entry1.setSize(TEST_DATA_1.length);
            zos.putArchiveEntry(entry1);
            // Without autoCompress, user must compress manually — writing raw data
            zos.write(TEST_DATA_1);
            ZipArchiveEntry entry2 = new ZipArchiveEntry("test2.txt");
            entry2.setMethod(ZipMethod.ZSTD.getCode());
            entry2.setSize(TEST_DATA_2.length);
            zos.putArchiveEntry(entry2);
            // Without autoCompress, user must compress manually — writing raw data
            zos.write(TEST_DATA_2);
        }
        // data cannot be asserted here, as the user must compress manually
        try (ZipFile zipFile = ZipFile.builder().setFile(file).get()) {
            ZipArchiveEntry entry1 = zipFile.getEntry("test1.txt");
            assertEquals(ZipMethod.ZSTD.getCode(), entry1.getMethod());
            assertEquals(TEST_DATA_1.length, entry1.getSize());
            ZipArchiveEntry entry2 = zipFile.getEntry("test2.txt");
            assertEquals(ZipMethod.ZSTD.getCode(), entry2.getMethod());
            assertEquals(TEST_DATA_2.length, entry2.getSize());
        }
    }

    private void assertZipFile(File file, ZipMethod zipMethod) throws Exception {
        try (ZipFile zipFile = ZipFile.builder().setFile(file).get()) {
            ZipArchiveEntry entry1 = zipFile.getEntry("test1.txt");
            assertEquals(zipMethod.getCode(), entry1.getMethod());
            assertEquals(TEST_DATA_1.length, entry1.getSize());
            assertArrayEquals(TEST_DATA_1, IOUtils.toByteArray(zipFile.getInputStream(entry1)));
            ZipArchiveEntry entry2 = zipFile.getEntry("test2.txt");
            assertEquals(zipMethod.getCode(), entry2.getMethod());
            assertEquals(TEST_DATA_2.length, entry2.getSize());
            assertArrayEquals(TEST_DATA_2, IOUtils.toByteArray(zipFile.getInputStream(entry2)));
        }
    }
}
