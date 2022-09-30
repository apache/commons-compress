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
package org.apache.commons.compress.archivers.zip;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

/**
 * Tests https://issues.apache.org/jira/browse/COMPRESS-598
 */
public class Crash_f2efd9eaeb86cda597d07b5e3c3d81363633c2da_Test {

    @Test
    public void test() throws IOException {
        final byte[] input = FileUtils
            .readFileToByteArray(new File("src/test/resources/org/apache/commons/compress/fuzz/crash-f2efd9eaeb86cda597d07b5e3c3d81363633c2da"));
        try (ZipArchiveInputStream zis = new ZipArchiveInputStream(new ByteArrayInputStream(input))) {
            //
            // I do not think this is supposed to be recoverable... then... what?
            //
            assertThrows(NullPointerException.class, () -> {
                for (;;) {
                    final ArchiveEntry zipEntry = zis.getNextEntry();
                    if (zipEntry == null) {
                        break;
                    }
                    final long entrySize = zipEntry.getSize();
                    if (entrySize == -1) {
                        IOUtils.toByteArray(zis);
                    } else {
                        IOUtils.toByteArray(zis, entrySize);
                    }
                    // Eventually throws a NullPointerException
                    zis.getCompressedCount();
                }
            });
        }
    }

}
