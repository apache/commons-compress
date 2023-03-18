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
package org.apache.commons.compress.compressors.lz4;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.stream.Stream;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class XXHash32Test {

    public static Stream<Arguments> factory() {
        return Stream.of(
            // reference checksums created with xxh32sum
            Arguments.of("bla.tar", "fbb5c8d1"),
            Arguments.of("bla.tar.xz", "4106a208"),
            Arguments.of("8.posix.tar.gz", "9fce116a")
        );
    }

    @ParameterizedTest
    @MethodSource("factory")
    public void verifyChecksum(final String fileName, final String expectedChecksum) throws IOException {
        final XXHash32 h = new XXHash32();
        final File file = AbstractTestCase.getFile(fileName);
        try (InputStream s = Files.newInputStream(file.toPath())) {
            final byte[] b = IOUtils.toByteArray(s);
            h.update(b, 0, b.length);
        }
        assertEquals(expectedChecksum, Long.toHexString(h.getValue()), "checksum for " + file.getName());
    }
}
