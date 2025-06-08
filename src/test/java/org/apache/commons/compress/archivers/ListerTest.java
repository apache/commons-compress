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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.apache.commons.io.file.PathUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ListerTest {

    /**
     * Creates a stream of paths of test fixtures with file names that don't end with {@code "-fail"} for specific file extensions.
     *
     * @return a stream of paths.
     * @throws IOException if an I/O error is thrown.
     */
    public static Stream<Path> getFixtures() throws IOException {
        return PathUtils.walk(Paths.get("src/test/resources"), new RegexFileFilter("^(?!.*(-fail)).*\\.(tar|ar|arj|apk|dump)$"), 10, false);
    }

    @ParameterizedTest
    @MethodSource("getFixtures")
    void testMain(final Path path) throws ArchiveException, IOException {
        new Lister(true, path.toString()).go();
    }
}
