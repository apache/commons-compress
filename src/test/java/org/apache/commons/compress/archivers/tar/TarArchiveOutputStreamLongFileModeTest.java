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

package org.apache.commons.compress.archivers.tar;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;

import org.junit.jupiter.api.Test;

import shaded.org.apache.commons.lang3.StringUtils;

/**
 * Tests {@link TarArchiveOutputStream}.
 */
class TarArchiveOutputStreamLongFileModeTest {

    /**
     * Run with a non-default low memory configuration {@code -Xmx256m} if you want to see the test fail without the change to main.
     */
    @Test
    void test() throws Exception {
        // 100m characters, adjust as needed
        final String longName = StringUtils.repeat('a', 100_000_000);
        try (TarArchiveOutputStream taos = new TarArchiveOutputStream(new ByteArrayOutputStream())) {
            final TarArchiveEntry entry = new TarArchiveEntry(longName);
            entry.setSize(0);
            assertThrows(IllegalArgumentException.class, () -> taos.putArchiveEntry(entry));
        }
    }
}
