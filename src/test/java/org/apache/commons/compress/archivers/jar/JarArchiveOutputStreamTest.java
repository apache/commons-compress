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
package org.apache.commons.compress.archivers.jar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.archivers.zip.JarMarker;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipExtraField;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.junit.jupiter.api.Test;

public class JarArchiveOutputStreamTest {

    @Test
    public void testJarMarker() throws IOException {
        final Path testArchive = Files.createTempFile("jar-aostest", ".jar");
        testArchive.toFile().deleteOnExit();
        try (JarArchiveOutputStream out = new JarArchiveOutputStream(Files.newOutputStream(testArchive))) {
            final ZipArchiveEntry ze1 = new ZipArchiveEntry("foo/");
            // Ensure we won't accidentally add an Extra field.
            ze1.setTime(Instant.parse("2022-12-27T12:10:23Z").toEpochMilli());
            out.putArchiveEntry(ze1);
            out.closeArchiveEntry();
            final ZipArchiveEntry ze2 = new ZipArchiveEntry("bar/");
            // Ensure we won't accidentally add an Extra field.
            ze2.setTime(Instant.parse("2022-12-28T02:56:01Z").toEpochMilli());
            out.putArchiveEntry(ze2);
            out.closeArchiveEntry();
            out.finish();
        }
        try (ZipFile zf = new ZipFile(testArchive)) {
            ZipArchiveEntry ze = zf.getEntry("foo/");
            assertNotNull(ze);
            ZipExtraField[] fes = ze.getExtraFields();
            assertEquals(1, fes.length);
            assertTrue(fes[0] instanceof JarMarker);

            ze = zf.getEntry("bar/");
            assertNotNull(ze);
            fes = ze.getExtraFields();
            assertEquals(0, fes.length);
        } finally {
            AbstractTestCase.tryHardToDelete(testArchive);
        }
    }

}