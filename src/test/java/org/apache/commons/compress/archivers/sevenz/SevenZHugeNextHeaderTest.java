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

package org.apache.commons.compress.archivers.sevenz;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.CRC32;

import org.apache.commons.compress.MemoryLimitException;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.io.RandomAccessFileMode;
import org.apache.commons.io.file.PathUtils;
import org.apache.commons.lang3.function.Consumers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

final class SevenZHugeNextHeaderTest {

    private static final String FILE_NAME = "target/GenerateHugeNextHeader7zTest.7z";

    @AfterAll
    static void afterAll() throws IOException {
        PathUtils.deleteFile(Paths.get(FILE_NAME));
    }

    private static long crc32OfZeros(final long numBytes) {
        final CRC32 crc = new CRC32();
        final int chunk = 1 << 16;
        final byte[] zeros = new byte[chunk];
        long remaining = numBytes;
        while (remaining > 0) {
            final int len = (int) Math.min(remaining, chunk);
            crc.update(zeros, 0, len);
            remaining -= len;
        }
        return crc.getValue();
    }

    private void generate() throws IOException {
        final Path path = Paths.get(FILE_NAME);
        final long sizeMB = 256L;
        final long nextHeaderSize = sizeMB * 1024L * 1024L;
        final long usableSpace = path.toFile().getParentFile().getUsableSpace();
        assumeTrue(usableSpace > nextHeaderSize, () -> String.format("usableSpace %,d > nextHeaderSize %,d, %s", usableSpace, nextHeaderSize, path));
        final long nextHeaderOffset = 0L;
        final long nextHeaderCrc = crc32OfZeros(nextHeaderSize);
        final ByteBuffer startHeader = ByteBuffer.allocate(8 + 8 + 4).order(ByteOrder.LITTLE_ENDIAN);
        startHeader.putLong(nextHeaderOffset);
        startHeader.putLong(nextHeaderSize);
        startHeader.putInt((int) nextHeaderCrc);
        final byte[] startHeaderBytes = startHeader.array();
        final CRC32 startHeaderCrc = new CRC32();
        startHeaderCrc.update(startHeaderBytes);
        final long startHeaderCrcValue = startHeaderCrc.getValue();
        final ByteBuffer mainHeader = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);
        mainHeader.put(SevenZFile.SIGNATURE);
        mainHeader.put((byte) 0x00);
        mainHeader.put((byte) 0x02);
        mainHeader.putInt((int) startHeaderCrcValue);
        try (OutputStream outputStream = Files.newOutputStream(path)) {
            outputStream.write(mainHeader.array());
            outputStream.write(startHeaderBytes);
        }
        RandomAccessFileMode.READ_WRITE.accept(path, raf -> raf.setLength(32L + nextHeaderSize));
    }

    private void getEntries() throws IOException {
        try (SevenZFile sz = new SevenZFile.Builder().setFile(FILE_NAME).get()) {
            // force header parsing
            sz.getEntries().forEach(Consumers.nop());
        }
    }

    @Test
    void test() throws IOException {
        generate();
        try {
            getEntries();
        } catch (final MemoryLimitException ignore) {
            // You may need a lower memory configuration to get here, depending on your OS, Java version, and/or container.
            // For example: -Xmx128m
            ignore.printStackTrace();
        } catch (final ArchiveException ignore) {
            // If a MemoryLimitException isn't thrown beause a lot of memory is available, then we get this failure:
            // org.apache.commons.compress.archivers.ArchiveException: Broken or unsupported archive: no Header
        }
    }
}
