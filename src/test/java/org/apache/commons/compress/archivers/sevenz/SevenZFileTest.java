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
package org.apache.commons.compress.archivers.sevenz;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import org.apache.commons.compress.AbstractTestCase;

public class SevenZFileTest extends AbstractTestCase {
    private static final String TEST2_CONTENT = "<?xml version = '1.0'?>\r\n<!DOCTYPE"
        + " connections>\r\n<meinxml>\r\n\t<leer />\r\n</meinxml>\n";

    public void testAllEmptyFilesArchive() throws Exception {
        SevenZFile archive = new SevenZFile(getFile("7z-empty-mhc-off.7z"));
        try {
            assertNotNull(archive.getNextEntry());
        } finally {
            archive.close();
        }
    }
    
    public void testHelloWorldHeaderCompressionOffCopy() throws Exception {
        checkHelloWorld("7z-hello-mhc-off-copy.7z");
    }

    public void testHelloWorldHeaderCompressionOffLZMA2() throws Exception {
        checkHelloWorld("7z-hello-mhc-off-lzma2.7z");
    }

    public void test7zUnarchive() throws Exception {
        test7zUnarchive(getFile("bla.7z"));
    }

    public void test7zDeflateUnarchive() throws Exception {
        test7zUnarchive(getFile("bla.deflate.7z"));
    }

    public void test7zDecryptUnarchive() throws Exception {
        if (isStrongCryptoAvailable()) {
            test7zUnarchive(getFile("bla.encrypted.7z"), "foo".getBytes("UTF-16LE"));
        }
    }

    private void test7zUnarchive(File f) throws Exception {
        test7zUnarchive(f, null);
    }

    public void testEncryptedArchiveRequiresPassword() throws Exception {
        try {
            new SevenZFile(getFile("bla.encrypted.7z"));
            fail("shouldn't decrypt without a password");
        } catch (IOException ex) {
            assertEquals("Cannot read encrypted files without a password",
                         ex.getMessage());
        }
    }

    /**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-256"
     */
    public void testCompressedHeaderWithNonDefaultDictionarySize() throws Exception {
        SevenZFile sevenZFile = new SevenZFile(getFile("COMPRESS-256.7z"));
        try {
            int count = 0;
            while (sevenZFile.getNextEntry() != null) {
                count++;
            }
            assertEquals(446, count);
        } finally {
            sevenZFile.close();
        }
    }

    private void test7zUnarchive(File f, byte[] password) throws Exception {
        SevenZFile sevenZFile = new SevenZFile(f, password);
        try {
            SevenZArchiveEntry entry = sevenZFile.getNextEntry();
            assertEquals("test1.xml", entry.getName());
            entry = sevenZFile.getNextEntry();
            assertEquals("test2.xml", entry.getName());
            byte[] contents = new byte[(int)entry.getSize()];
            int off = 0;
            while ((off < contents.length)) {
                int bytesRead = sevenZFile.read(contents, off, contents.length - off);
                assert(bytesRead >= 0);
                off += bytesRead;
            }
            assertEquals(TEST2_CONTENT, new String(contents, "UTF-8"));
            assertNull(sevenZFile.getNextEntry());
        } finally {
            sevenZFile.close();
        }
    }

    private void checkHelloWorld(final String filename) throws Exception {
        SevenZFile sevenZFile = new SevenZFile(getFile(filename));
        try {
            SevenZArchiveEntry entry = sevenZFile.getNextEntry();
            assertEquals("Hello world.txt", entry.getName());
            byte[] contents = new byte[(int)entry.getSize()];
            int off = 0;
            while ((off < contents.length)) {
                int bytesRead = sevenZFile.read(contents, off, contents.length - off);
                assert(bytesRead >= 0);
                off += bytesRead;
            }
            assertEquals("Hello, world!\n", new String(contents, "UTF-8"));
            assertNull(sevenZFile.getNextEntry());
        } finally {
            sevenZFile.close();
        }
    }

    private static boolean isStrongCryptoAvailable() throws NoSuchAlgorithmException {
        return Cipher.getMaxAllowedKeyLength("AES/ECB/PKCS5Padding") >= 256;
    }
}
