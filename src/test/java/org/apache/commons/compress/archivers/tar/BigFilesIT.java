/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.commons.compress.archivers.tar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Random;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.junit.jupiter.api.Test;

public class BigFilesIT extends AbstractTestCase {

    private void readFileBiggerThan8GByte(final String name) throws Exception {
        try (InputStream in = new BufferedInputStream(Files.newInputStream(getPath(name)));
             GzipCompressorInputStream gzin = new GzipCompressorInputStream(in);
             TarArchiveInputStream tin = new TarArchiveInputStream(gzin)) {
            final TarArchiveEntry e = tin.getNextTarEntry();
            assertNotNull(e);
            assertEquals(8200L * 1024 * 1024, e.getSize());

            long read = 0;
            final Random r = new Random(System.currentTimeMillis());
            int readNow;
            final byte[] buf = new byte[1024 * 1024];
            while ((readNow = tin.read(buf, 0, buf.length)) > 0) {
                // testing all bytes for a value of 0 is going to take
                // too long, just pick a few ones randomly
                for (int i = 0; i < 100; i++) {
                    final int idx = r.nextInt(readNow);
                    assertEquals(0, buf[idx], "testing byte " + (read + idx));
                }
                read += readNow;
            }
            assertEquals(8200L * 1024 * 1024, read);
            assertNull(tin.getNextTarEntry());
        }
    }

    @Test
    public void readFileBiggerThan8GBytePosix() throws Exception {
        readFileBiggerThan8GByte("8.posix.tar.gz");
    }

    @Test
    public void readFileBiggerThan8GByteStar() throws Exception {
        readFileBiggerThan8GByte("8.star.tar.gz");
    }

    @Test
    public void readFileHeadersOfArchiveBiggerThan8GByte() throws Exception {
        try (InputStream in = new BufferedInputStream(Files.newInputStream(getPath("8.posix.tar.gz")));
             GzipCompressorInputStream gzin = new GzipCompressorInputStream(in);
             TarArchiveInputStream tin = new TarArchiveInputStream(gzin)) {
            final TarArchiveEntry e = tin.getNextTarEntry();
            assertNotNull(e);
            assertNull(tin.getNextTarEntry());
        }
    }

    @Test
    public void tarFileReadFileHeadersOfArchiveBiggerThan8GByte() throws Exception {
        final Path file = getPath("8.posix.tar.gz");
        final Path output = resultDir.toPath().resolve("8.posix.tar");
        try (InputStream in = new BufferedInputStream(Files.newInputStream(file));
             GzipCompressorInputStream gzin = new GzipCompressorInputStream(in)) {
            Files.copy(gzin, output, StandardCopyOption.REPLACE_EXISTING);
        }

        try (final TarFile tarFile = new TarFile(output)) {
            final List<TarArchiveEntry> entries = tarFile.getEntries();
            assertEquals(1, entries.size());
            assertNotNull(entries.get(0));
        }
    }

}
