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
package org.apache.commons.compress.archivers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import org.junit.jupiter.api.Test;

class ArchiveEntryTest {

    private static ArchiveEntry entry(final String name) {
        return new ArchiveEntry() {
            @Override
            public Date getLastModifiedDate() {
                return new Date(0);
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public long getSize() {
                return 0;
            }

            @Override
            public boolean isDirectory() {
                return false;
            }
        };
    }

    private final Path parent = Paths.get("parent");

    @Test
    void testResolveInRejectsInvalidName() {
        // A NUL byte is a legal byte in a stored entry name (for example a crafted ZIP central directory
        // record) but is rejected by the platform Path parser, so parentPath.resolve throws the unchecked
        // InvalidPathException. It must be reported through the declared IOException contract instead.
        final IOException e = assertThrows(IOException.class, () -> entry("a\u0000b").resolveIn(parent));
        assertInstanceOf(InvalidPathException.class, e.getCause());
    }

    @Test
    void testResolveInRejectsZipSlip() {
        assertThrows(IOException.class, () -> entry("../evil").resolveIn(parent));
    }

    @Test
    void testResolveInResolvesValidName() throws IOException {
        assertEquals(parent.resolve("sub/file.txt"), entry("sub/file.txt").resolveIn(parent));
    }
}
