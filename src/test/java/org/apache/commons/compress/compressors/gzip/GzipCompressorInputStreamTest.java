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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link GzipCompressorInputStream}.
 */
public class GzipCompressorInputStreamTest {

    /**
     * Tests file from gzip 1.13.
     *
     * <pre>{@code
     * gzip --keep --name --best -c hello1.txt >members.gz
     * gzip --keep --name --best -c hello2.txt >>members.gz
     * }</pre>
     */
    @Test
    public void testReadGzipFileCreatedByCli() throws IOException {
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
