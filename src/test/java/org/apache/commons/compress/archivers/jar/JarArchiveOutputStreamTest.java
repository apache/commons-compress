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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.archivers.zip.*;
import org.junit.jupiter.api.Test;

public class JarArchiveOutputStreamTest {

    @Test
    public void testJarMarker() throws IOException {
        final File testArchive = File.createTempFile("jar-aostest", ".jar");
        testArchive.deleteOnExit();
        JarArchiveOutputStream out = null;
        ZipFile zf = null;
        try {

            out = new JarArchiveOutputStream(Files.newOutputStream(testArchive.toPath()));
            out.putArchiveEntry(new ZipArchiveEntry("foo/"));
            out.closeArchiveEntry();
            out.putArchiveEntry(new ZipArchiveEntry("bar/"));
            out.closeArchiveEntry();
            out.finish();
            out.close();
            out = null;

            zf = new ZipFile(testArchive);
            ZipArchiveEntry ze = zf.getEntry("foo/");
            assertNotNull(ze);
            ZipExtraField[] fes = ze.getExtraFields();
            assertEquals(2, fes.length);
            assertTrue(fes[0] instanceof JarMarker);
            assertTrue(fes[1] instanceof X000A_NTFS);

            ze = zf.getEntry("bar/");
            assertNotNull(ze);
            fes = ze.getExtraFields();
            assertEquals(1, fes.length);
            assertTrue(fes[0] instanceof X000A_NTFS);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (final IOException e) { /* swallow */ }
            }
            ZipFile.closeQuietly(zf);
            AbstractTestCase.tryHardToDelete(testArchive);
        }
    }

}