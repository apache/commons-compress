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

import static org.apache.commons.compress.archivers.extractor.Fixtures.Entry.blockDev;
import static org.apache.commons.compress.archivers.extractor.Fixtures.Entry.charDev;
import static org.apache.commons.compress.archivers.extractor.Fixtures.Entry.fifo;
import static org.apache.commons.compress.archivers.extractor.Fixtures.Entry.file;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Special-entry (device, FIFO) handling: skipped by default so the rest of the archive still extracts, or rejected when the
 * policy demands it. The secure path never materializes such entries.
 */
class ExtractorSpecialFileTest {

    @TempDir
    Path target;

    @Test
    void rejectsSpecialWhenConfigured() throws Exception {
        final byte[] data = Fixtures.tar(fifo("fifo0"));
        final Extractor extractor = Extractor.newExtractor(target).setSpecialFilePolicy(SpecialFilePolicy.REJECT);
        assertThrows(IOException.class, () -> Fixtures.extractTar(extractor, data));
    }

    @Test
    void skipsSpecialByDefault() throws Exception {
        final byte[] data = Fixtures.tar(charDev("cdev0"), fifo("fifo0"), file("real.txt", "ok"));
        final Extractor extractor = Extractor.newExtractor(target);
        Fixtures.extractTar(extractor, data);
        assertFalse(Files.exists(target.resolve("cdev0"), LinkOption.NOFOLLOW_LINKS));
        assertFalse(Files.exists(target.resolve("fifo0"), LinkOption.NOFOLLOW_LINKS));
        assertTrue(Files.exists(target.resolve("real.txt")));
    }

    @Test
    void skipsBlockDeviceByDefault() throws Exception {
        final byte[] data = Fixtures.tar(blockDev("bdev0"), file("real.txt", "ok"));
        Fixtures.extractTar(Extractor.newExtractor(target), data);
        assertFalse(Files.exists(target.resolve("bdev0"), LinkOption.NOFOLLOW_LINKS));
        assertTrue(Files.exists(target.resolve("real.txt")));
    }

    @Test
    void rejectsBlockDeviceWhenConfigured() throws Exception {
        final byte[] data = Fixtures.tar(blockDev("bdev0"));
        final Extractor extractor = Extractor.newExtractor(target).setSpecialFilePolicy(SpecialFilePolicy.REJECT);
        assertThrows(IOException.class, () -> Fixtures.extractTar(extractor, data));
    }
}
