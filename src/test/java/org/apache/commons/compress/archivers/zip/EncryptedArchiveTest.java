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
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.junit.Test;

public class EncryptedArchiveTest {

    @Test
    public void testReadPasswordEncryptedEntryViaZipFile()
        throws IOException {
        File file = getFile("password-encrypted.zip");
        ZipFile zf = null;
        try {
            zf = new ZipFile(file);
            ZipArchiveEntry zae = zf.getEntry("LICENSE.txt");
            assertTrue(zae.getGeneralPurposeBit().usesEncryption());
            assertFalse(zae.getGeneralPurposeBit().usesStrongEncryption());
            assertFalse(zf.canReadEntryData(zae));
            try {
                zf.getInputStream(zae);
                fail("expected an exception");
            } catch (UnsupportedZipFeatureException ex) {
                assertSame(UnsupportedZipFeatureException.Feature.ENCRYPTION,
                           ex.getFeature());
            }
        } finally {
            ZipFile.closeQuietly(zf);
        }
    }

    @Test
    public void testReadPasswordEncryptedEntryViaStream()
        throws IOException {
        File file = getFile("password-encrypted.zip");
        ZipArchiveInputStream zin = null;
        try {
            zin = new ZipArchiveInputStream(new FileInputStream(file));
            ZipArchiveEntry zae = zin.getNextZipEntry();
            assertEquals("LICENSE.txt", zae.getName());
            assertTrue(zae.getGeneralPurposeBit().usesEncryption());
            assertFalse(zae.getGeneralPurposeBit().usesStrongEncryption());
            assertFalse(zin.canReadEntryData(zae));
            try {
                byte[] buf = new byte[1024];
                zin.read(buf, 0, buf.length);
                fail("expected an exception");
            } catch (UnsupportedZipFeatureException ex) {
                assertSame(UnsupportedZipFeatureException.Feature.ENCRYPTION,
                           ex.getFeature());
            }
        } finally {
            if (zin != null) {
                zin.close();
            }
        }
    }

    public void testReadPkwareEncryptedEntryViaZipFile()
        throws IOException {
        System.out.println("A");
        File file = getFile("pkware-encrypted.zip");
        ZipFile zf = null;
        try {
            zf = new ZipFile(file);
            ZipArchiveEntry zae = zf.getEntry("LICENSE.txt");
            assertTrue(zae.getGeneralPurposeBit().usesEncryption());
            assertTrue(zae.getGeneralPurposeBit().usesStrongEncryption());
            assertFalse(zf.canReadEntryData(zae));
            try {
                zf.getInputStream(zae);
                fail("expected an exception");
            } catch (UnsupportedZipFeatureException ex) {
                assertSame(UnsupportedZipFeatureException.Feature.ENCRYPTION,
                           ex.getFeature());
            }
        } finally {
            ZipFile.closeQuietly(zf);
        }
    }

    public void testReadPkwareEncryptedEntryViaStream()
        throws IOException {
        System.out.println("B");
        File file = getFile("pkware-encrypted.zip");
        ZipArchiveInputStream zin = null;
        try {
            zin = new ZipArchiveInputStream(new FileInputStream(file));
            ZipArchiveEntry zae = zin.getNextZipEntry();
            assertEquals("LICENSE.txt", zae.getName());
            assertTrue(zae.getGeneralPurposeBit().usesEncryption());
            assertTrue(zae.getGeneralPurposeBit().usesStrongEncryption());
            assertFalse(zin.canReadEntryData(zae));
            try {
                byte[] buf = new byte[1024];
                zin.read(buf, 0, buf.length);
                fail("expected an exception");
            } catch (UnsupportedZipFeatureException ex) {
                assertSame(UnsupportedZipFeatureException.Feature.ENCRYPTION,
                           ex.getFeature());
            }
        } finally {
            if (zin != null) {
                zin.close();
            }
        }
    }

    public void testReadPkwareFullyEncryptedEntryViaZipFile()
        throws IOException {
        System.out.println("C");
        File file = getFile("pkware-fully-encrypted.zip");
        ZipFile zf = null;
        try {
            zf = new ZipFile(file);
            ZipArchiveEntry zae = zf.getEntry("1");
            assertTrue(zae.getGeneralPurposeBit().usesEncryption());
            assertTrue(zae.getGeneralPurposeBit().usesStrongEncryption());
            assertFalse(zf.canReadEntryData(zae));
            try {
                zf.getInputStream(zae);
                fail("expected an exception");
            } catch (UnsupportedZipFeatureException ex) {
                assertSame(UnsupportedZipFeatureException.Feature.ENCRYPTION,
                           ex.getFeature());
            }
        } finally {
            ZipFile.closeQuietly(zf);
        }
    }

    public void testReadPkwareFullyEncryptedEntryViaStream()
        throws IOException {
        System.out.println("D");
        File file = getFile("pkware-fully-encrypted.zip");
        ZipArchiveInputStream zin = null;
        try {
            zin = new ZipArchiveInputStream(new FileInputStream(file));
            ZipArchiveEntry zae = zin.getNextZipEntry();
            assertEquals("1", zae.getName());
            assertTrue(zae.getGeneralPurposeBit().usesEncryption());
            assertTrue(zae.getGeneralPurposeBit().usesStrongEncryption());
            assertFalse(zin.canReadEntryData(zae));
            try {
                byte[] buf = new byte[1024];
                zin.read(buf, 0, buf.length);
                fail("expected an exception");
            } catch (UnsupportedZipFeatureException ex) {
                assertSame(UnsupportedZipFeatureException.Feature.ENCRYPTION,
                           ex.getFeature());
            }
        } finally {
            if (zin != null) {
                zin.close();
            }
        }
    }

    public void testReadPkwarePasswordEncryptedEntryViaZipFile()
        throws IOException {
        System.out.println("E");
        File file = getFile("pkware-password-encrypted.zip");
        ZipFile zf = null;
        try {
            zf = new ZipFile(file);
            ZipArchiveEntry zae = zf.getEntry("LICENSE.txt");
            assertTrue(zae.getGeneralPurposeBit().usesEncryption());
            assertTrue(zae.getGeneralPurposeBit().usesStrongEncryption());
            assertFalse(zf.canReadEntryData(zae));
            try {
                zf.getInputStream(zae);
                fail("expected an exception");
            } catch (UnsupportedZipFeatureException ex) {
                assertSame(UnsupportedZipFeatureException.Feature.ENCRYPTION,
                           ex.getFeature());
            }
        } finally {
            ZipFile.closeQuietly(zf);
        }
    }

    public void testReadPkwarePasswordEncryptedEntryViaStream()
        throws IOException {
        System.out.println("F");
        File file = getFile("pkware-password-encrypted.zip");
        ZipArchiveInputStream zin = null;
        try {
            zin = new ZipArchiveInputStream(new FileInputStream(file));
            ZipArchiveEntry zae = zin.getNextZipEntry();
            assertEquals("LICENSE.txt", zae.getName());
            assertTrue(zae.getGeneralPurposeBit().usesEncryption());
            assertTrue(zae.getGeneralPurposeBit().usesStrongEncryption());
            assertFalse(zin.canReadEntryData(zae));
            try {
                byte[] buf = new byte[1024];
                zin.read(buf, 0, buf.length);
                fail("expected an exception");
            } catch (UnsupportedZipFeatureException ex) {
                assertSame(UnsupportedZipFeatureException.Feature.ENCRYPTION,
                           ex.getFeature());
            }
        } finally {
            if (zin != null) {
                zin.close();
            }
        }
    }
}
