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
package org.apache.commons.compress.archivers;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.sevenz.SevenZMethod;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public class SevenZTestCase extends AbstractTestCase {

    private File output;
    private final File file1, file2;

    public SevenZTestCase() throws IOException {
        file1 = getFile("test1.xml");
        file2 = getFile("test2.xml");
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        output = new File(dir, "bla.7z");
    }

    @Test
    public void testSevenZArchiveCreationUsingCopy() throws Exception {
        testSevenZArchiveCreation(SevenZMethod.COPY);
    }

    @Test
    public void testSevenZArchiveCreationUsingLZMA() throws Exception {
        testSevenZArchiveCreation(SevenZMethod.LZMA);
    }

    @Test
    public void testSevenZArchiveCreationUsingLZMA2() throws Exception {
        testSevenZArchiveCreation(SevenZMethod.LZMA2);
    }

    @Test
    public void testSevenZArchiveCreationUsingBZIP2() throws Exception {
        testSevenZArchiveCreation(SevenZMethod.BZIP2);
    }

    @Test
    public void testSevenZArchiveCreationUsingDeflate() throws Exception {
        testSevenZArchiveCreation(SevenZMethod.DEFLATE);
    }

    private void testSevenZArchiveCreation(final SevenZMethod method) throws Exception {
        createArchive(method);
        try (SevenZFile archive = new SevenZFile(output)) {
            SevenZArchiveEntry entry;

            entry = archive.getNextEntry();
            assert (entry != null);
            assertEquals(entry.getName(), file1.getName());

            entry = archive.getNextEntry();
            assert (entry != null);
            assertEquals(entry.getName(), file2.getName());

            assert (archive.getNextEntry() == null);
        }
    }

    private void createArchive(final SevenZMethod method) throws Exception {
        final SevenZOutputFile outArchive = new SevenZOutputFile(output);
        outArchive.setContentCompression(method);
        try {
            SevenZArchiveEntry entry;

            entry = outArchive.createArchiveEntry(file1, file1.getName());
            outArchive.putArchiveEntry(entry);
            copy(file1, outArchive);
            outArchive.closeArchiveEntry();

            entry = outArchive.createArchiveEntry(file2, file2.getName());
            outArchive.putArchiveEntry(entry);
            copy(file2, outArchive);
            outArchive.closeArchiveEntry();
        } finally {
            outArchive.close();
        }
    }

    @Test
    public void singleByteReadConsistentlyReturnsMinusOneAtEofUsingCopy() throws Exception {
        singleByteReadConsistentlyReturnsMinusOneAtEof(SevenZMethod.COPY);
    }

    @Test
    public void singleByteReadConsistentlyReturnsMinusOneAtEofUsingLZMA() throws Exception {
        singleByteReadConsistentlyReturnsMinusOneAtEof(SevenZMethod.LZMA);
    }

    @Test
    public void singleByteReadConsistentlyReturnsMinusOneAtEofUsingLZMA2() throws Exception {
        singleByteReadConsistentlyReturnsMinusOneAtEof(SevenZMethod.LZMA2);
    }

    @Test
    public void singleByteReadConsistentlyReturnsMinusOneAtEofUsingBZIP2() throws Exception {
        singleByteReadConsistentlyReturnsMinusOneAtEof(SevenZMethod.BZIP2);
    }

    @Test
    public void singleByteReadConsistentlyReturnsMinusOneAtEofUsingDeflate() throws Exception {
        singleByteReadConsistentlyReturnsMinusOneAtEof(SevenZMethod.DEFLATE);
    }

    @Test
    public void singleByteReadConsistentlyReturnsMinusOneAtEofUsingAES() throws Exception {
        assumeStrongCryptoIsAvailable();
        try (SevenZFile archive = new SevenZFile(getFile("bla.encrypted.7z"), "foo".toCharArray())) {
            singleByteReadConsistentlyReturnsMinusOneAtEof(archive);
        }
    }

    private void singleByteReadConsistentlyReturnsMinusOneAtEof(final SevenZMethod method) throws Exception {
        createArchive(method);
        try (SevenZFile archive = new SevenZFile(output)) {
            singleByteReadConsistentlyReturnsMinusOneAtEof(archive);
        }
    }

    private void singleByteReadConsistentlyReturnsMinusOneAtEof(SevenZFile archive) throws Exception {
        SevenZArchiveEntry entry = archive.getNextEntry();
        entry = archive.getNextEntry();
        readFully(archive);
        assertEquals(-1, archive.read());
        assertEquals(-1, archive.read());
    }

    @Test
    public void multiByteReadConsistentlyReturnsMinusOneAtEofUsingLZMA() throws Exception {
        multiByteReadConsistentlyReturnsMinusOneAtEof(SevenZMethod.LZMA);
    }

    @Test
    public void multiByteReadConsistentlyReturnsMinusOneAtEofUsingLZMA2() throws Exception {
        multiByteReadConsistentlyReturnsMinusOneAtEof(SevenZMethod.LZMA2);
    }

    @Test
    public void multiByteReadConsistentlyReturnsMinusOneAtEofUsingBZIP2() throws Exception {
        multiByteReadConsistentlyReturnsMinusOneAtEof(SevenZMethod.BZIP2);
    }

    @Test
    public void multiByteReadConsistentlyReturnsMinusOneAtEofUsingDeflate() throws Exception {
        multiByteReadConsistentlyReturnsMinusOneAtEof(SevenZMethod.DEFLATE);
    }

    @Test
    public void multiByteReadConsistentlyReturnsMinusOneAtEofUsingAES() throws Exception {
        assumeStrongCryptoIsAvailable();
        try (SevenZFile archive = new SevenZFile(getFile("bla.encrypted.7z"), "foo".toCharArray())) {
            multiByteReadConsistentlyReturnsMinusOneAtEof(archive);
        }
    }

    private void multiByteReadConsistentlyReturnsMinusOneAtEof(final SevenZMethod method) throws Exception {
        createArchive(method);
        try (SevenZFile archive = new SevenZFile(output)) {
            multiByteReadConsistentlyReturnsMinusOneAtEof(archive);
        }
    }

    private void multiByteReadConsistentlyReturnsMinusOneAtEof(SevenZFile archive) throws Exception {
        final byte[] buf = new byte[2];
        SevenZArchiveEntry entry = archive.getNextEntry();
        entry = archive.getNextEntry();
        readFully(archive);
        assertEquals(-1, archive.read(buf));
        assertEquals(-1, archive.read(buf));
    }

    private void copy(final File src, final SevenZOutputFile dst) throws IOException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(src);
            final byte[] buffer = new byte[8*1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) >= 0) {
                dst.write(buffer, 0, bytesRead);
            }
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
    }

    private void readFully(final SevenZFile archive) throws IOException {
        final byte[] buf = new byte[1024];
        int x = 0;
        while (0 <= (x = archive.read(buf))) {
            ;
        }
    }

    private static void assumeStrongCryptoIsAvailable() throws NoSuchAlgorithmException {
        Assume.assumeTrue("test requires strong crypto", Cipher.getMaxAllowedKeyLength("AES/ECB/PKCS5Padding") >= 256);
    }
}
