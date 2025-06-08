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

package org.apache.commons.compress.compressors.gzip;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.file.PathUtils;
import org.apache.commons.io.function.IOStream;
import org.apache.commons.io.input.RandomAccessFileInputStream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests {@link GzipCompressorInputStream}.
 */
class GzipCompressorInputStreamTest {

    @TempDir
    Path tempDir;

    /**
     * Extracts members of a GZIP file to the temporary directory.
     */
    @SuppressWarnings("resource")
    private List<Path> extractMembers(final String sourceGzipPath) throws IOException {
        final List<GzipParameters> members = new ArrayList<>();
        final Path tempFile = tempDir.resolve("temp.bin");
        // Extract GZIP members in one temp file.
        // Callbacks are invoked while reading with the member size known only after reading a member's trailer.
        // @formatter:off
        try (OutputStream fos = Files.newOutputStream(tempFile);
                GzipCompressorInputStream gis = GzipCompressorInputStream.builder()
                .setFile(sourceGzipPath)
                .setDecompressConcatenated(true)
                .setOnMemberEnd(in -> members.add(in.getMetaData()))
                .get()) {
            // @formatter:on
            IOUtils.copy(gis, fos);
        }
        final List<Path> resolved = new ArrayList<>(members.size());
        final AtomicLong startPos = new AtomicLong();
        // Read temp file and write each member file.
        // @formatter:off
        try (RandomAccessFileInputStream rafIs = RandomAccessFileInputStream.builder()
                .setPath(tempFile)
                .setCloseOnClose(true)
                .get()) {
            // @formatter:on
            IOStream.of(members).forEach(e -> {
                final Path member = tempDir.resolve(e.getFileName());
                resolved.add(member);
                try (OutputStream os = Files.newOutputStream(member, StandardOpenOption.CREATE_NEW, StandardOpenOption.TRUNCATE_EXISTING)) {
                    startPos.addAndGet(rafIs.copy(startPos.get(), e.getTrailerISize(), os));
                }
            });
        }
        return resolved;
    }

    @Test
    @Disabled
    void testGzipParametersMembersIo() throws IOException {
        final Path targetFile = tempDir.resolve("test.gz");
        final String sourceFileName1 = "file1";
        final String sourceFileName2 = "file2";
        final Path tempSourceFile1 = tempDir.resolve(sourceFileName1);
        final Path tempSourceFile2 = tempDir.resolve(sourceFileName2);
        final byte[] bytes1 = "<text>Hello World 1!</text>".getBytes(StandardCharsets.UTF_8);
        final byte[] bytes2 = "<text>Hello World 2!</text>".getBytes(StandardCharsets.UTF_8);
        Files.write(tempSourceFile1, bytes1);
        Files.write(tempSourceFile2, bytes2);
        final GzipParameters parameters1 = new GzipParameters();
        final GzipParameters parameters2 = new GzipParameters();
        parameters1.setFileName(sourceFileName1);
        parameters2.setFileName(sourceFileName2);
        try (OutputStream fos = Files.newOutputStream(targetFile);
                GzipCompressorOutputStream gos = new GzipCompressorOutputStream(fos, parameters1)) {
            gos.write(tempSourceFile1);
            gos.finish();
            gos.write(tempSourceFile2);
            gos.finish();
        }
        try (GzipCompressorInputStream gis = GzipCompressorInputStream.builder().setPath(targetFile).setDecompressConcatenated(false).get()) {
            assertEquals(parameters1, gis.getMetaData());
            assertArrayEquals(bytes1, IOUtils.toByteArray(gis));
        }
        try (GzipCompressorInputStream gis = GzipCompressorInputStream.builder().setPath(targetFile).setDecompressConcatenated(true).get()) {
            assertEquals(parameters1, gis.getMetaData());
            // assertArrayEquals(ArrayUtils.addAll(bytes1, bytes2), IOUtils.toByteArray(gis));
        }
    }

    /**
     * Tests file from gzip 1.13.
     *
     * <pre>{@code
     * gzip --keep --name --best -c hello1.txt >members.gz
     * gzip --keep --name --best -c hello2.txt >>members.gz
     * }</pre>
     *
     * @throws IOException on test failure.
     */
    @Test
    void testOnMemberFirstAll() throws IOException {
        final List<GzipParameters> parametersStart = new ArrayList<>();
        final List<GzipParameters> parametersEnd = new ArrayList<>();
        // Concatenated members, same file
        // @formatter:off
        try (GzipCompressorInputStream gis = GzipCompressorInputStream.builder()
                .setFile("src/test/resources/org/apache/commons/compress/gzip/members.gz")
                .setDecompressConcatenated(true)
                .setOnMemberStart(in -> parametersStart.add(in.getMetaData()))
                .setOnMemberEnd(in -> parametersEnd.add(in.getMetaData()))
                .get()) {
            // @formatter:on
            assertEquals("hello1.txt", gis.getMetaData().getFileName());
            assertEquals("Hello1\nHello2\n", IOUtils.toString(gis, StandardCharsets.ISO_8859_1));
            assertEquals("hello2.txt", gis.getMetaData().getFileName());
        }
        assertEquals(2, parametersStart.size());
        assertEquals(2, parametersEnd.size());
        assertEquals(parametersStart, parametersEnd);
        // Make sure we are not reusing GzipParameters anymore.
        assertEquals(2, new HashSet<>(parametersStart).size());
        assertEquals(2, new HashSet<>(parametersEnd).size());
        // trailers
        assertEquals(4202744527L, parametersEnd.get(0).getTrailerCrc());
        assertEquals(7, parametersEnd.get(0).getTrailerISize());
        assertEquals(3517815052L, parametersEnd.get(1).getTrailerCrc());
        assertEquals(7, parametersEnd.get(1).getTrailerISize());
    }

    /**
     * Tests file from gzip 1.13.
     *
     * <pre>{@code
     * gzip --keep --name --best -c hello1.txt >members.gz
     * gzip --keep --name --best -c hello2.txt >>members.gz
     * }</pre>
     *
     * @throws IOException on test failure.
     */
    @Test
    void testOnMemberFirstOnly() throws IOException {
        final List<GzipParameters> parametersStart = new ArrayList<>();
        final List<GzipParameters> parametersEnd = new ArrayList<>();
        // First member only
        // @formatter:off
        try (GzipCompressorInputStream gis = GzipCompressorInputStream.builder()
                .setFile("src/test/resources/org/apache/commons/compress/gzip/members.gz")
                .setDecompressConcatenated(false)
                .setOnMemberStart(in -> parametersStart.add(in.getMetaData()))
                .setOnMemberEnd(in -> parametersEnd.add(in.getMetaData()))
                .get()) {
            // @formatter:on
            assertEquals("hello1.txt", gis.getMetaData().getFileName());
            assertEquals("Hello1\n", IOUtils.toString(gis, StandardCharsets.ISO_8859_1));
            assertEquals("hello1.txt", gis.getMetaData().getFileName());
        }
        assertEquals(1, parametersStart.size());
        assertEquals(1, parametersEnd.size());
        assertEquals(parametersStart, parametersEnd);
        // trailer
        assertEquals(4202744527L, parametersEnd.get(0).getTrailerCrc());
        assertEquals(7, parametersEnd.get(0).getTrailerISize());
    }

    /**
     * Tests file from gzip 1.13.
     *
     * <pre>{@code
     * gzip --keep --name --best -c hello1.txt >members.gz
     * gzip --keep --name --best -c hello2.txt >>members.gz
     * }</pre>
     *
     * @throws IOException on test failure.
     */
    @Test
    void testOnMemberSaveAsFiles() throws IOException {
        final List<Path> resolved = extractMembers("src/test/resources/org/apache/commons/compress/gzip/members.gz");
        assertEquals("Hello1\n", PathUtils.readString(resolved.get(0), StandardCharsets.ISO_8859_1));
        assertEquals("Hello2\n", PathUtils.readString(resolved.get(1), StandardCharsets.ISO_8859_1));
    }

    /**
     * Tests file from gzip 1.13 for input files of size 0.
     *
     * <pre>{@code
     * gzip --keep --name --best -c hello-size-0-a.txt >members-size-0.gz
     * gzip --keep --name --best -c hello-size-0-b.txt >>members-size-0.gz
     * gzip --keep --name --best -c hello-size-0-c.txt >>members-size-0.gz
     * }</pre>
     *
     * @throws IOException on test failure.
     */
    @SuppressWarnings("resource")
    @Test
    void testOnMemberSaveAsSize0Files() throws IOException {
        final List<Path> resolved = extractMembers("src/test/resources/org/apache/commons/compress/gzip/members-size-0.gz");
        assertEquals(3, resolved.size());
        IOStream.of(resolved).forEach(p -> {
            assertEquals(0, Files.size(p));
            assertEquals("", PathUtils.readString(p, StandardCharsets.ISO_8859_1));
        });
    }

    /**
     * Tests file from gzip 1.13.
     *
     * <pre>{@code
     * gzip --keep --name --best -c hello1.txt >members.gz
     * gzip --keep --name --best -c hello2.txt >>members.gz
     * }</pre>
     *
     * @throws IOException on test failure.
     */
    @Test
    void testReadGzipFileCreatedByCli() throws IOException {
        // First member only
        try (GzipCompressorInputStream gis = GzipCompressorInputStream.builder().setFile("src/test/resources/org/apache/commons/compress/gzip/members.gz")
                .get()) {
            assertEquals("hello1.txt", gis.getMetaData().getFileName());
            assertEquals("Hello1\n", IOUtils.toString(gis, StandardCharsets.ISO_8859_1));
            assertEquals("hello1.txt", gis.getMetaData().getFileName());
        }
        // Concatenated members, same file
        try (GzipCompressorInputStream gis = GzipCompressorInputStream.builder().setFile("src/test/resources/org/apache/commons/compress/gzip/members.gz")
                .setDecompressConcatenated(true).get()) {
            assertEquals("hello1.txt", gis.getMetaData().getFileName());
            assertEquals("Hello1\nHello2\n", IOUtils.toString(gis, StandardCharsets.ISO_8859_1));
            assertEquals("hello2.txt", gis.getMetaData().getFileName());
        }
    }

}
