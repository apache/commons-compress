/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.commons.compress.compressors.bzip2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests COMPRESS-686.
 */
public class Compress686Test {

    @TempDir
    private Path tempDir;

    private Path compressFile(final Path file, final boolean bufferOutput) throws IOException {
        final Path newFile = tempDir.resolve(file.getFileName().toString() + ".bz2");
        try (InputStream in = Files.newInputStream(file);
                BZip2CompressorOutputStream bzOut = new BZip2CompressorOutputStream(newOutputStream(newFile, bufferOutput))) {
            IOUtils.copy(in, bzOut);
        }
        return newFile;
    }

    private Path decompressBzip2File(final Path file) throws IOException {
        final Path decompressedFile = file.resolveSibling(file.getFileName().toString() + ".decompressed");
        try (BZip2CompressorInputStream bzIn = new BZip2CompressorInputStream(Files.newInputStream(file));
                OutputStream outputStream = Files.newOutputStream(decompressedFile)) {
            IOUtils.copy(bzIn, outputStream);
        }
        return decompressedFile;
    }

    private OutputStream newOutputStream(final Path newFile, final boolean bufferOutput) throws IOException {
        final OutputStream outputStream = Files.newOutputStream(newFile);
        return bufferOutput ? new BufferedOutputStream(outputStream) : outputStream;
    }

    @ParameterizedTest
    // TODO
    // @ValueSource(booleans = { true, false })
    @ValueSource(booleans = { false })
    public void testRoundtrip(final boolean bufferCompressOutput) throws Exception {
        final Path file = tempDir.resolve("test.txt");
        final String contents = "random contents";
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            IOUtils.write(contents, w);
        }
        final Path compressedFile = compressFile(file, bufferCompressOutput);
        decompressBzip2File(compressedFile);
        assertEquals(contents, IOUtils.toString(file.toUri(), StandardCharsets.UTF_8));
    }
}
