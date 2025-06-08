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

package org.apache.commons.compress.compressors.bzip2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

/**
 * Tests https://issues.apache.org/jira/browse/COMPRESS-651
 */
class BZip2Compress651Test {

    @Test
    void testCompress651() throws IOException {
        final int buffersize = 102_400;
        final Path pathIn = Paths.get("src/test/resources/org/apache/commons/compress/COMPRESS-651/my10m.tar.bz2");
        final Path pathOut = Paths.get("target/COMPRESS-651/test.tar");
        Files.createDirectories(pathOut.getParent());
        try (BZip2CompressorInputStream inputStream = new BZip2CompressorInputStream(new BufferedInputStream(Files.newInputStream(pathIn)), true);
                OutputStream outputStream = Files.newOutputStream(pathOut)) {
            IOUtils.copy(inputStream, outputStream, buffersize);
        }
        // expected size confirmed locally on macOS
        assertEquals(10_496_000, Files.size(pathOut));
    }
}
