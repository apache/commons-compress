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

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Benchmark for https://github.com/apache/commons-compress/pull/378
 */
public class ZipFileNameMapBenmark {

    private static final Path ARCHIVE_PATH = Paths.get("target/large_archive.zip");

    private static final ZipFile globalFile;

    private static final String[] entryNames;
    static {
        try {
            globalFile = ZipFile.builder().setPath(create60MbZipFile()).get();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        entryNames = Collections.list(globalFile.getEntries()).stream().map(ZipArchiveEntry::getName).toArray(String[]::new);
    }

    private static Path create60MbZipFile() throws FileNotFoundException, IOException {
        // 60 MB in bytes (60 * 1024 * 1024)
        final long targetSize = 62914560L;
        // 8 KB buffer for fast writing
        final int bufferSize = 8192;
        final String zipFileName = "target/large_archive.zip";
        final String entryName = "target/large_file.dat";
        try (FileOutputStream fos = new FileOutputStream(ARCHIVE_PATH.toFile());
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                ZipOutputStream zos = new ZipOutputStream(bos)) {
            // Tell the zip file what the file inside will be called
            zos.putNextEntry(new ZipEntry(entryName));
            final byte[] buffer = new byte[bufferSize];
            long bytesWritten = 0;
            // Loop to generate and write data until we hit 60 MB
            while (bytesWritten < targetSize) {
                // Calculate how many bytes to write in this specific step
                final long bytesLeft = targetSize - bytesWritten;
                final int lengthToWrite = (int) Math.min(bufferSize, bytesLeft);
                // Write fake data (zeros) to the zip output stream
                zos.write(buffer, 0, lengthToWrite);
                bytesWritten += lengthToWrite;
            }
            // Close the current file inside the zip
            zos.closeEntry();
        }
        System.out.printf("Created a zip file of size: %,d bytes at %s%n", targetSize, zipFileName);
        return Paths.get(zipFileName);
    }

    @Benchmark
    public void testGetEntries(final Blackhole blackhole) throws IOException {
        for (final String entryName : entryNames) {
            blackhole.consume(globalFile.getEntries(entryName));
        }
    }

    @Benchmark
    public void testGetEntriesInPhysicalOrder(final Blackhole blackhole) throws IOException {
        for (final String entryName : entryNames) {
            blackhole.consume(globalFile.getEntriesInPhysicalOrder(entryName));
        }
    }

    @Benchmark
    public void testGetEntry(final Blackhole blackhole) throws IOException {
        for (final String entryName : entryNames) {
            blackhole.consume(globalFile.getEntry(entryName));
        }
    }

    @Benchmark
    public void testOpen() throws IOException {
        try (ZipFile file = ZipFile.builder().setPath(ARCHIVE_PATH).get()) {
            // do nothing, just open and close the file to measure the time taken for this operation.
        }
    }
}
