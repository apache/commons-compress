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

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.io.function.IOConsumer;
import org.junit.jupiter.api.Test;

/**
 * Tests https://issues.apache.org/jira/browse/COMPRESS-713
 */
public class ZipCompress713Test {

    @Test
    public void testIllegalArrayIndex() throws IOException {
        final byte[] data = { 80, 75, 3, 4, 19, 7, 0, 1, 1, 0, -1, 1, 120, 8, 84, -99, 3, 48, 45, 1, 119, -70, 110, 61, 65, 104, 0, 0, 0, 0, 59, -4, -1, -1, -1,
                -1, -33, 0, -1, 0, 5, 0, -1, -1, -1, -1, -1, -1, -1, 0, 122 };
        try (ZipArchiveInputStream inputStream = new ZipArchiveInputStream(new ByteArrayInputStream(data))) {
            inputStream.getNextEntry();
            assertThrows(ArchiveException.class, () -> inputStream.read(new byte[1024]));
            assertThrows(EOFException.class, () -> inputStream.getNextEntry());
        }
    }

    @Test
    public void testTruncated() throws IOException {
        final byte[] data = { 80, 75, 3, 4, 19, 7, 0, 1, 1, 0, -1, 1, 120, 8, 84, -99, 3, 48, 45, 1, 119, -70, 110, 61, 65, 104, 0, 0, 0, 0, 59, -4, -1, -1, -1,
                -1, -33, 0, -1, 0, 5, 0, -1, -1, -1, -1, -1, -1, -1, 0, 122 };
        try (ZipArchiveInputStream inputStream = new ZipArchiveInputStream(new ByteArrayInputStream(data))) {
            assertThrows(EOFException.class, () -> inputStream.forEach(IOConsumer.noop()));
        }
    }
}
