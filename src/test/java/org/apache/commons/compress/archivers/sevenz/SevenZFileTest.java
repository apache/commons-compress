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
import java.util.Arrays;
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
        test7zUnarchive(getFile("bla.7z"), SevenZMethod.LZMA);
    }

    public void test7zDeflateUnarchive() throws Exception {
        test7zUnarchive(getFile("bla.deflate.7z"), SevenZMethod.DEFLATE);
    }

    public void test7zDecryptUnarchive() throws Exception {
        if (isStrongCryptoAvailable()) {
            test7zUnarchive(getFile("bla.encrypted.7z"), SevenZMethod.LZMA, // stack LZMA + AES
                            "foo".getBytes("UTF-16LE"));
        }
    }

    private void test7zUnarchive(File f, SevenZMethod m) throws Exception {
        test7zUnarchive(f, m, null);
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

    public void testSignatureCheck() {
        assertTrue(SevenZFile.matches(SevenZFile.sevenZSignature,
                                      SevenZFile.sevenZSignature.length));
        assertTrue(SevenZFile.matches(SevenZFile.sevenZSignature,
                                      SevenZFile.sevenZSignature.length + 1));
        assertFalse(SevenZFile.matches(SevenZFile.sevenZSignature,
                                      SevenZFile.sevenZSignature.length - 1));
        assertFalse(SevenZFile.matches(new byte[] { 1, 2, 3, 4, 5, 6 }, 6));
        assertTrue(SevenZFile.matches(new byte[] { '7', 'z', (byte) 0xBC,
                                                   (byte) 0xAF, 0x27, 0x1C}, 6));
        assertFalse(SevenZFile.matches(new byte[] { '7', 'z', (byte) 0xBC,
                                                    (byte) 0xAF, 0x27, 0x1D}, 6));
    }

    public void testReadingBackLZMA2DictSize() throws Exception {
        File output = new File(dir, "lzma2-dictsize.7z");
        SevenZOutputFile outArchive = new SevenZOutputFile(output);
        try {
            outArchive.setContentMethods(Arrays.asList(new SevenZMethodConfiguration(SevenZMethod.LZMA2, 1 << 20)));
            SevenZArchiveEntry entry = new SevenZArchiveEntry();
            entry.setName("foo.txt");
            outArchive.putArchiveEntry(entry);
            outArchive.write(new byte[] { 'A' });
            outArchive.closeArchiveEntry();
        } finally {
            outArchive.close();
        }

        SevenZFile archive = new SevenZFile(output);
        try {
            SevenZArchiveEntry entry = archive.getNextEntry();
            SevenZMethodConfiguration m = entry.getContentMethods().iterator().next();
            assertEquals(SevenZMethod.LZMA2, m.getMethod());
            assertEquals(1 << 20, m.getOptions());
        } finally {
            archive.close();
        }
    }

    public void testReadingBackDeltaDistance() throws Exception {
        File output = new File(dir, "delta-distance.7z");
        SevenZOutputFile outArchive = new SevenZOutputFile(output);
        try {
            outArchive.setContentMethods(Arrays.asList(new SevenZMethodConfiguration(SevenZMethod.DELTA_FILTER, 32),
                                                       new SevenZMethodConfiguration(SevenZMethod.LZMA2)));
            SevenZArchiveEntry entry = new SevenZArchiveEntry();
            entry.setName("foo.txt");
            outArchive.putArchiveEntry(entry);
            outArchive.write(new byte[] { 'A' });
            outArchive.closeArchiveEntry();
        } finally {
            outArchive.close();
        }

        SevenZFile archive = new SevenZFile(output);
        try {
            SevenZArchiveEntry entry = archive.getNextEntry();
            SevenZMethodConfiguration m = entry.getContentMethods().iterator().next();
            assertEquals(SevenZMethod.DELTA_FILTER, m.getMethod());
            assertEquals(32, m.getOptions());
        } finally {
            archive.close();
        }
    }

    private void test7zUnarchive(File f, SevenZMethod m, byte[] password) throws Exception {
        SevenZFile sevenZFile = new SevenZFile(f, password);
        try {
            SevenZArchiveEntry entry = sevenZFile.getNextEntry();
            assertEquals("test1.xml", entry.getName());
            assertEquals(m, entry.getContentMethods().iterator().next().getMethod());
            entry = sevenZFile.getNextEntry();
            assertEquals("test2.xml", entry.getName());
            assertEquals(m, entry.getContentMethods().iterator().next().getMethod());
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
