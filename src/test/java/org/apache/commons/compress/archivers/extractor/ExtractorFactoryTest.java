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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SecureDirectoryStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Factory and platform detection: {@code newExtractor} returns the secure implementation exactly when the platform's file
 * system provides a {@link SecureDirectoryStream}, and validates its target.
 */
class ExtractorFactoryTest {

    @TempDir
    Path target;

    @Test
    void factoryMatchesPlatformCapability() throws Exception {
        final boolean secureFileSystem;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(target)) {
            secureFileSystem = stream instanceof SecureDirectoryStream;
        }
        assertEquals(secureFileSystem, Extractor.newExtractor(target) instanceof SecureExtractor);
    }

    @Test
    void rejectsMissingTarget() {
        final Path missing = target.resolve("does-not-exist");
        assertThrows(IOException.class, () -> Extractor.newExtractor(missing));
    }

    @Test
    void rejectsNonDirectoryTarget() throws Exception {
        final Path file = Files.createFile(target.resolve("a-file"));
        assertThrows(IOException.class, () -> Extractor.newExtractor(file));
    }

    @Test
    void rejectsNullTarget() {
        assertThrows(NullPointerException.class, () -> Extractor.newExtractor(null));
    }

    @Test
    void settersReturnSameInstanceForChaining() throws Exception {
        final Extractor extractor = Extractor.newExtractor(target);
        assertSame(extractor, extractor.setSymlinkPolicy(SymlinkPolicy.SKIP));
        assertSame(extractor, extractor.setSpecialFilePolicy(SpecialFilePolicy.REJECT));
        assertSame(extractor, extractor.setOverwrite(true));
    }
}
