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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link GzipCompressorOutputStream}.
 */
public class GzipCompressorOutputStreamTest {

    private void testFileName(final String sourceFile) throws IOException {
        final Path tempSourceFile = Files.createTempFile(sourceFile, sourceFile);
        Files.write(tempSourceFile, "<text>Hello World!</text>".getBytes(StandardCharsets.ISO_8859_1));
        final Path targetFile = Files.createTempFile("test", ".gz");
        final GzipParameters parameters = new GzipParameters();
        parameters.setFilename(sourceFile);
        try (OutputStream fos = Files.newOutputStream(targetFile); GzipCompressorOutputStream gos = new GzipCompressorOutputStream(fos, parameters)) {
            Files.copy(tempSourceFile, gos);
        }
        try (GzipCompressorInputStream gis = new GzipCompressorInputStream(Files.newInputStream(targetFile))) {
            assertEquals(sourceFile, gis.getMetaData().getFilename());
        }
    }

    @Test
    public void testFileNameAscii() throws IOException {
        testFileName("ASCII.xml");
    }

    /**
     * Tests COMPRESS-638.
     *
     * @throws IOException When the test fails.
     */
    @Test
    @Disabled("COMPRESS-638")
    public void testFileNameChinese() throws IOException {
        // "Test Chinese name"
        testFileName("\u6D4B\u8BD5\u4E2D\u6587\u540D\u79F0.xml");
    }

}
