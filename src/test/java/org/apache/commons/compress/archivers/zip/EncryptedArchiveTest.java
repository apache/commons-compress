/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.commons.compress.archivers.zip;

import static org.apache.commons.compress.AbstractTestCase.getFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

public class EncryptedArchiveTest {

    @Test
    public void testReadPasswordEncryptedEntryViaStream()
        throws IOException {
        final File file = getFile("password-encrypted.zip");
        try (ZipArchiveInputStream zin = new ZipArchiveInputStream(Files.newInputStream(file.toPath()))) {
            final ZipArchiveEntry zae = zin.getNextZipEntry();
            assertEquals("LICENSE.txt", zae.getName());
            assertTrue(zae.getGeneralPurposeBit().usesEncryption());
            assertFalse(zae.getGeneralPurposeBit().usesStrongEncryption());
            assertFalse(zin.canReadEntryData(zae));
            final UnsupportedZipFeatureException ex = assertThrows(UnsupportedZipFeatureException.class, () -> {
                final byte[] buf = new byte[1024];
                zin.read(buf, 0, buf.length);
            }, "expected an exception");
            assertSame(UnsupportedZipFeatureException.Feature.ENCRYPTION, ex.getFeature());
        }
    }

    @Test
    public void testReadPasswordEncryptedEntryViaZipFile()
        throws IOException {
        final File file = getFile("password-encrypted.zip");
        try (final ZipFile zf = new ZipFile(file)) {
            final ZipArchiveEntry zae = zf.getEntry("LICENSE.txt");
            assertTrue(zae.getGeneralPurposeBit().usesEncryption());
            assertFalse(zae.getGeneralPurposeBit().usesStrongEncryption());
            assertFalse(zf.canReadEntryData(zae));
            final UnsupportedZipFeatureException ex = assertThrows(UnsupportedZipFeatureException.class, () -> zf.getInputStream(zae),
                    "expected an exception");
            assertSame(UnsupportedZipFeatureException.Feature.ENCRYPTION, ex.getFeature());
        }
    }
}
