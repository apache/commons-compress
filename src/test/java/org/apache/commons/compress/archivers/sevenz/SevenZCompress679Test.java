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

package org.apache.commons.compress.archivers.sevenz;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class SevenZCompress679Test {

    @Test
    void testCompress679() {
        final Path origin = Paths.get("src/test/resources/org/apache/commons/compress/COMPRESS-679/file.7z");
        assertTrue(Files.exists(origin));
        final Callable<Boolean> runnable = () -> {
            try (SevenZFile sevenZFile = SevenZFile.builder().setPath(origin).get()) {
                SevenZArchiveEntry sevenZArchiveEntry;
                while ((sevenZArchiveEntry = sevenZFile.getNextEntry()) != null) {
                    if ("file4.txt".equals(sevenZArchiveEntry.getName())) { // The entry must not be the first of the ZIP archive to reproduce
                        final InputStream inputStream = sevenZFile.getInputStream(sevenZArchiveEntry);
                        // treatments...
                        break;
                    }
                }
            }
            return Boolean.TRUE;
        };
        final ExecutorService threadPool = Executors.newFixedThreadPool(10);
        try {
            final List<Future<Boolean>> futures = IntStream.range(0, 30).mapToObj(i -> threadPool.submit(runnable)).collect(Collectors.toList());
            futures.forEach(f -> assertDoesNotThrow(() -> f.get()));
        } finally {
            threadPool.shutdownNow();
        }
    }
}
