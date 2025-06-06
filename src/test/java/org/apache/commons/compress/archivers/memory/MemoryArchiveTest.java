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
package org.apache.commons.compress.archivers.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.junit.jupiter.api.Test;

public final class MemoryArchiveTest {

    @Test
    void testReading() throws IOException {

        try (MemoryArchiveInputStream is = new MemoryArchiveInputStream(new String[][] { { "test1", "content1" }, { "test2", "content2" } })) {

            final ArchiveEntry entry1 = is.getNextEntry();
            assertNotNull(entry1);
            assertEquals("test1", entry1.getName());
            final String content1 = is.readString();
            assertEquals("content1", content1);

            final ArchiveEntry entry2 = is.getNextEntry();
            assertNotNull(entry2);
            assertEquals("test2", entry2.getName());
            final String content2 = is.readString();
            assertEquals("content2", content2);

            final ArchiveEntry entry3 = is.getNextEntry();
            assertNull(entry3);
        }
    }

}
