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

package org.apache.commons.compress.compressors.gzip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * Tests {@link GzipCompressorOutputStream}.
 */
public class GzipCompressorOutputStreamTest {

    private static final String EXPECTED_BASE_NAME = "\u6D4B\u8BD5\u4E2D\u6587\u540D\u79F0";
    private static final String EXPECTED_FILE_NAME = EXPECTED_BASE_NAME + ".xml";

    private void testChineseFileName(final String expected, final String sourceFile, final Charset fileNameCharset) throws IOException {
        final Path tempSourceFile = Files.createTempFile(sourceFile, sourceFile);
        Files.write(tempSourceFile, "<text>Hello World!</text>".getBytes(StandardCharsets.ISO_8859_1));
        final Path targetFile = Files.createTempFile(EXPECTED_BASE_NAME, ".gz");
        final GzipParameters parameters = new GzipParameters();
        // If your system is Windows with Chinese, and your file name is Chinese, you need set the fileNameCharset to GBK
        // otherwise your file name is different with use GzipCompressorOutputStream without set GzipParameters
        // and the same situation in Linux, need set the fileNameCharset to UTF-8
        parameters.setFileNameCharset(fileNameCharset);
        assertEquals(fileNameCharset, parameters.getFileNameCharset());
        parameters.setFileName(EXPECTED_FILE_NAME);
        try (OutputStream fos = Files.newOutputStream(targetFile);
                GzipCompressorOutputStream gos = new GzipCompressorOutputStream(fos, parameters)) {
            Files.copy(tempSourceFile, gos);
        }
        try (GzipCompressorInputStream gis = new GzipCompressorInputStream(Files.newInputStream(targetFile))) {
            final byte[] fileNameBytes = gis.getMetaData().getFileName().getBytes(StandardCharsets.ISO_8859_1);
            final String unicodeFileName = new String(fileNameBytes, fileNameCharset);
            assertEquals(expected, unicodeFileName);
        }
    }

    /**
     * Tests Chinese Filename for Windows behavior.
     *
     * @throws IOException When the test fails.
     */
    @Test
    public void testChineseFileNameGBK() throws IOException {
        assumeTrue(Charset.isSupported("GBK"));
        testChineseFileName(EXPECTED_FILE_NAME, EXPECTED_FILE_NAME, Charset.forName("GBK"));
    }

    /**
     * Tests Chinese Filename for Windows behavior.
     *
     * @throws IOException When the test fails.
     */
    @Test
    public void testChineseFileNameUTF8() throws IOException {
        testChineseFileName(EXPECTED_FILE_NAME, EXPECTED_FILE_NAME, StandardCharsets.UTF_8);
    }

    private void testFileName(final String expected, final String sourceFile) throws IOException {
        final Path tempSourceFile = Files.createTempFile(sourceFile, sourceFile);
        Files.write(tempSourceFile, "<text>Hello World!</text>".getBytes(StandardCharsets.ISO_8859_1));
        final Path targetFile = Files.createTempFile("test", ".gz");
        final GzipParameters parameters = new GzipParameters();
        parameters.setFilename(sourceFile);
        assertEquals(parameters.getFilename(), parameters.getFileName());
        parameters.setFileName(sourceFile);
        assertEquals(parameters.getFilename(), parameters.getFileName());
        try (OutputStream fos = Files.newOutputStream(targetFile);
                GzipCompressorOutputStream gos = new GzipCompressorOutputStream(fos, parameters)) {
            Files.copy(tempSourceFile, gos);
        }
        try (GzipCompressorInputStream gis = new GzipCompressorInputStream(Files.newInputStream(targetFile))) {
            assertEquals(expected, gis.getMetaData().getFileName());
            assertEquals(expected, gis.getMetaData().getFilename());
        }
    }

    @Test
    public void testFileNameAscii() throws IOException {
        testFileName("ASCII.xml", "ASCII.xml");
    }

    /**
     * Tests COMPRESS-638.
     *
     * GZip RFC requires ISO 8859-1 (LATIN-1).
     *
     * @throws IOException When the test fails.
     */
    @Test
    public void testFileNameChinesePercentEncoded() throws IOException {
        // "Test Chinese name"
        testFileName("%E6%B5%8B%E8%AF%95%E4%B8%AD%E6%96%87%E5%90%8D%E7%A7%B0.xml", EXPECTED_FILE_NAME);
    }
}
