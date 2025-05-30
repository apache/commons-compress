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
package org.apache.commons.compress;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream;
import org.apache.commons.compress.archivers.arj.ArjArchiveInputStream;
import org.apache.commons.compress.archivers.cpio.CpioArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.junit.jupiter.api.Test;

public final class DetectArchiverTest extends AbstractTest {

    final ClassLoader classLoader = getClass().getClassLoader();

    private void checkDetectedType(final String type, final Path path) throws ArchiveException, IOException {
        try (BufferedInputStream inputStream = new BufferedInputStream(Files.newInputStream(path))) {
            // SPECIAL case:
            // For now JAR files are detected as ZIP files.
            assertEquals(ArchiveStreamFactory.JAR.equals(type) ? ArchiveStreamFactory.ZIP : type, ArchiveStreamFactory.detect(inputStream));
        }
    }

    private void checkEmptyArchive(final String type) throws Exception {
        final Path path = createEmptyArchive(type); // will be deleted by tearDown()
        assertDoesNotThrow(() -> {
            try (BufferedInputStream inputStream = new BufferedInputStream(Files.newInputStream(path));
                    ArchiveInputStream<?> ais = factory.createArchiveInputStream(inputStream)) {
                // empty
            }
        }, "Should have recognized empty archive for " + type);
        checkDetectedType(type, path);
    }

    @SuppressWarnings("resource") // Caller closes
    private <T extends ArchiveInputStream<? extends E>, E extends ArchiveEntry> T createArchiveInputStream(final String resource)
            throws ArchiveException, IOException {
        return factory.createArchiveInputStream(createBufferedInputStream(resource));
    }

    private BufferedInputStream createBufferedInputStream(final String resource) throws IOException {
        return new BufferedInputStream(newInputStream(resource));
    }

    @Test
    public void testCOMPRESS_117() throws Exception {
        try (ArchiveInputStream<?> tar = createArchiveInputStream("COMPRESS-117.tar")) {
            assertNotNull(tar);
            assertInstanceOf(TarArchiveInputStream.class, tar);
        }
    }

    @Test
    public void testCOMPRESS_335() throws Exception {
        try (ArchiveInputStream<?> tar = createArchiveInputStream("COMPRESS-335.tar")) {
            assertNotNull(tar);
            assertInstanceOf(TarArchiveInputStream.class, tar);
        }
    }

    @Test
    public void testDetection() throws Exception {

        try (ArchiveInputStream<?> ar = createArchiveInputStream("bla.ar")) {
            assertNotNull(ar);
            assertInstanceOf(ArArchiveInputStream.class, ar);
        }

        try (ArchiveInputStream<?> tar = createArchiveInputStream("bla.tar")) {
            assertNotNull(tar);
            assertInstanceOf(TarArchiveInputStream.class, tar);
        }

        try (ArchiveInputStream<?> zip = createArchiveInputStream("bla.zip")) {
            assertNotNull(zip);
            assertInstanceOf(ZipArchiveInputStream.class, zip);
        }

        try (ArchiveInputStream<?> jar = createArchiveInputStream("bla.jar")) {
            assertNotNull(jar);
            assertInstanceOf(ZipArchiveInputStream.class, jar);
        }

        try (ArchiveInputStream<?> cpio = createArchiveInputStream("bla.cpio")) {
            assertNotNull(cpio);
            assertInstanceOf(CpioArchiveInputStream.class, cpio);
        }

        try (ArchiveInputStream<?> arj = createArchiveInputStream("bla.arj")) {
            assertNotNull(arj);
            assertInstanceOf(ArjArchiveInputStream.class, arj);
        }

// Not yet implemented
//        final ArchiveInputStream<?> tgz = getStreamFor("bla.tgz");
//        assertNotNull(tgz);
//        assertInstanceOf(TarArchiveInputStream.class, tgz);

    }

    @Test
    public void testDetectionNotArchive() {
        assertThrows(ArchiveException.class, () -> createArchiveInputStream("test.txt"));
    }

    // Check that the empty archives created by the code are readable

    // Not possible to detect empty "ar" archive as it is completely empty
//    public void testEmptyArArchive() throws Exception {
//        emptyArchive("ar");
//    }

    @Test
    public void testDetectOldTarFormatArchive() throws Exception {
        try (ArchiveInputStream<?> tar = createArchiveInputStream("COMPRESS-612/test-times-star-folder.tar")) {
            assertNotNull(tar);
            assertInstanceOf(TarArchiveInputStream.class, tar);
        }
    }

    @Test
    public void testEmptyCpioArchive() throws Exception {
        checkEmptyArchive("cpio");
    }

    @Test
    public void testEmptyJarArchive() throws Exception {
        checkEmptyArchive("jar");
    }

    @Test
    public void testEmptyTarArchive() throws Exception {
        // Can't detect empty tar archive from its contents.
        final Path path = createEmptyArchive("tar"); // will be deleted by tearDown()
        assertThrows(ArchiveException.class, () -> checkDetectedType("tar", path));
    }

    @Test
    public void testEmptyZipArchive() throws Exception {
        checkEmptyArchive("zip");
    }

    /**
     * Tests COMPRESS-644.
     */
    @Test
    public void testIcoFile() {
        assertThrows(ArchiveException.class, () -> {
            try (InputStream in = createBufferedInputStream("org/apache/commons/compress/COMPRESS-644/ARW05UP.ICO")) {
                assertNull(ArchiveStreamFactory.detect(in));
            }
        });
    }
}
