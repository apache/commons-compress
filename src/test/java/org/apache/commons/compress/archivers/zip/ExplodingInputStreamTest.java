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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.AbstractTest;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

public class ExplodingInputStreamTest extends AbstractTest {

    @Test
    void testDecompress() throws Exception {
        try (ZipArchiveInputStream archive = new ZipArchiveInputStream(newInputStream("lorem-ipsum-implode.zip"))) {
            final ZipArchiveEntry entry = archive.getNextEntry();
            assertEquals("LOREM.TXT", entry.getName());
            assertEquals(ZipMethod.IMPLODING, ZipMethod.getMethodByCode(entry.getMethod()));

            final byte[] data = IOUtils.toByteArray(archive);
            assertEquals(144060, data.length);
            assertEquals("a00c4f3f36515c96b2faef71c054e7f3e86a4f0f4ed4824cb7c5293bb455d28a", DigestUtils.sha256Hex(data));
        }
    }
}
