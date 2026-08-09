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
package org.apache.commons.compress.archivers.extractor;

import static org.apache.commons.compress.archivers.extractor.Fixtures.Entry.file;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.apache.commons.compress.archivers.ArchiveException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * A malformed entry name that cannot be turned into a {@link Path} on this platform (a zip name field preserves a NUL byte,
 * unlike a NUL-terminated ustar name) must fail closed with an {@link IOException}, never escape as an unchecked
 * {@link InvalidPathException}. This is the "malicious input always yields IOException, never an unchecked crash" property the
 * fuzz sweep relies on.
 */
class ExtractorMalformedNameTest {

    /** A zip name field can carry an embedded NUL, which {@code Paths.get} rejects with an {@link InvalidPathException}. */
    private static final String NUL_NAME = "pre" + (char) 0 + "post";

    @TempDir
    Path target;

    @Test
    void nulByteZipNameFailsClosedAsArchiveException() throws Exception {
        final byte[] data = Fixtures.zip(file(NUL_NAME, "x"));
        final Extractor extractor = Extractor.newExtractor(target);
        final IOException ex = assertThrows(IOException.class, () -> Fixtures.extractZip(extractor, data));
        assertInstanceOf(ArchiveException.class, ex);
        assertInstanceOf(InvalidPathException.class, ex.getCause());
    }

    @Test
    void nulByteZipNameLeavesNothingBehind() throws Exception {
        final byte[] data = Fixtures.zip(file(NUL_NAME, "x"));
        final Extractor extractor = Extractor.newExtractor(target);
        assertThrows(IOException.class, () -> Fixtures.extractZip(extractor, data));
        try (Stream<Path> entries = Files.list(target)) {
            assertEquals(0L, entries.count(), "a rejected malformed entry must not leave anything behind");
        }
    }

    @Test
    void wellFormedNameStillExtracts() throws Exception {
        // The malformed-name guard must not regress legitimate extraction.
        Fixtures.extractZip(Extractor.newExtractor(target), Fixtures.zip(file("ok.txt", "fine")));
        assertArrayEquals("fine".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(target.resolve("ok.txt")));
    }
}
