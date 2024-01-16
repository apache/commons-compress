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
 */
package org.apache.commons.compress.archivers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.sevenz.SevenZMethod;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;
import org.apache.commons.compress.utils.TimeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SevenZTest extends AbstractTest {

    private static void assumeStrongCryptoIsAvailable() throws NoSuchAlgorithmException {
        assumeTrue(Cipher.getMaxAllowedKeyLength("AES/ECB/PKCS5Padding") >= 256, "test requires strong crypto");
    }

    private File output;

    private final File file1, file2;

    public SevenZTest() throws IOException {
        file1 = getFile("test1.xml");
        file2 = getFile("test2.xml");
    }

    private void copy(final File src, final SevenZOutputFile dst) throws IOException {
        try (InputStream fis = Files.newInputStream(src.toPath())) {
            final byte[] buffer = new byte[8 * 1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) >= 0) {
                dst.write(buffer, 0, bytesRead);
            }
        }
    }

    private void createArchive(final SevenZMethod method) throws Exception {
        try (SevenZOutputFile outArchive = new SevenZOutputFile(output)) {
            outArchive.setContentCompression(method);
            SevenZArchiveEntry entry;

            entry = outArchive.createArchiveEntry(file1, file1.getName());
            outArchive.putArchiveEntry(entry);
            copy(file1, outArchive);
            outArchive.closeArchiveEntry();

            entry = outArchive.createArchiveEntry(file2, file2.getName());
            outArchive.putArchiveEntry(entry);
            copy(file2, outArchive);
            outArchive.closeArchiveEntry();
        }
    }

    private void multiByteReadConsistentlyReturnsMinusOneAtEof(final SevenZFile archive) throws Exception {
        final byte[] buf = new byte[2];
        assertNotNull(archive.getNextEntry());
        assertNotNull(archive.getNextEntry());
        readFully(archive);
        assertEquals(-1, archive.read(buf));
        assertEquals(-1, archive.read(buf));
    }

    private void multiByteReadConsistentlyReturnsMinusOneAtEof(final SevenZMethod method) throws Exception {
        createArchive(method);
        try (SevenZFile archive = SevenZFile.builder().setFile(output).get()) {
            multiByteReadConsistentlyReturnsMinusOneAtEof(archive);
        }
    }

    private void readFully(final SevenZFile archive) throws IOException {
        final byte[] buf = new byte[1024];
        int x = 0;
        while (0 <= (x = archive.read(buf))) {

        }
    }

    @BeforeEach
    public void setUp() throws Exception {
        output = newTempFile("bla.7z");
    }

    private void singleByteReadConsistentlyReturnsMinusOneAtEof(final SevenZFile archive) throws Exception {
        assertNotNull(archive.getNextEntry());
        assertNotNull(archive.getNextEntry());
        readFully(archive);
        assertEquals(-1, archive.read());
        assertEquals(-1, archive.read());
    }

    private void singleByteReadConsistentlyReturnsMinusOneAtEof(final SevenZMethod method) throws Exception {
        createArchive(method);
        try (SevenZFile archive = SevenZFile.builder().setFile(output).get()) {
            singleByteReadConsistentlyReturnsMinusOneAtEof(archive);
        }
    }

    @Test
    public void testMultiByteReadConsistentlyReturnsMinusOneAtEofUsingAES() throws Exception {
        assumeStrongCryptoIsAvailable();
        try (SevenZFile archive = new SevenZFile(getFile("bla.encrypted.7z"), "foo".toCharArray())) {
            multiByteReadConsistentlyReturnsMinusOneAtEof(archive);
        }
    }

    @Test
    public void testMultiByteReadConsistentlyReturnsMinusOneAtEofUsingBZIP2() throws Exception {
        multiByteReadConsistentlyReturnsMinusOneAtEof(SevenZMethod.BZIP2);
    }

    @Test
    public void testMultiByteReadConsistentlyReturnsMinusOneAtEofUsingDeflate() throws Exception {
        multiByteReadConsistentlyReturnsMinusOneAtEof(SevenZMethod.DEFLATE);
    }

    @Test
    public void testMultiByteReadConsistentlyReturnsMinusOneAtEofUsingLZMA() throws Exception {
        multiByteReadConsistentlyReturnsMinusOneAtEof(SevenZMethod.LZMA);
    }

    @Test
    public void testMultiByteReadConsistentlyReturnsMinusOneAtEofUsingLZMA2() throws Exception {
        multiByteReadConsistentlyReturnsMinusOneAtEof(SevenZMethod.LZMA2);
    }

    private void testSevenZArchiveCreation(final SevenZMethod method) throws Exception {
        createArchive(method);
        try (SevenZFile archive = SevenZFile.builder().setFile(output).get()) {
            SevenZArchiveEntry entry;

            entry = archive.getNextEntry();
            assertNotNull(entry);
            assertEquals(file1.getName(), entry.getName());
            BasicFileAttributes attributes = Files.readAttributes(file1.toPath(), BasicFileAttributes.class);
            assertEquals(TimeUtils.truncateToHundredNanos(attributes.lastModifiedTime()), entry.getLastModifiedTime());
            assertEquals(TimeUtils.truncateToHundredNanos(attributes.creationTime()), entry.getCreationTime());
            assertNotNull(entry.getAccessTime());

            entry = archive.getNextEntry();
            assertNotNull(entry);
            assertEquals(file2.getName(), entry.getName());
            attributes = Files.readAttributes(file2.toPath(), BasicFileAttributes.class);
            assertEquals(TimeUtils.truncateToHundredNanos(attributes.lastModifiedTime()), entry.getLastModifiedTime());
            assertEquals(TimeUtils.truncateToHundredNanos(attributes.creationTime()), entry.getCreationTime());
            assertNotNull(entry.getAccessTime());

            assertNull(archive.getNextEntry());
        }
    }

    @Test
    public void testSevenZArchiveCreationUsingBZIP2() throws Exception {
        testSevenZArchiveCreation(SevenZMethod.BZIP2);
    }

    @Test
    public void testSevenZArchiveCreationUsingCopy() throws Exception {
        testSevenZArchiveCreation(SevenZMethod.COPY);
    }

    @Test
    public void testSevenZArchiveCreationUsingDeflate() throws Exception {
        testSevenZArchiveCreation(SevenZMethod.DEFLATE);
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
    public void testSingleByteReadConsistentlyReturnsMinusOneAtEofUsingAES() throws Exception {
        assumeStrongCryptoIsAvailable();
        try (SevenZFile archive = new SevenZFile(getFile("bla.encrypted.7z"), "foo".toCharArray())) {
            singleByteReadConsistentlyReturnsMinusOneAtEof(archive);
        }
    }

    @Test
    public void testSingleByteReadConsistentlyReturnsMinusOneAtEofUsingBZIP2() throws Exception {
        singleByteReadConsistentlyReturnsMinusOneAtEof(SevenZMethod.BZIP2);
    }

    @Test
    public void testSingleByteReadConsistentlyReturnsMinusOneAtEofUsingCopy() throws Exception {
        singleByteReadConsistentlyReturnsMinusOneAtEof(SevenZMethod.COPY);
    }

    @Test
    public void testSingleByteReadConsistentlyReturnsMinusOneAtEofUsingDeflate() throws Exception {
        singleByteReadConsistentlyReturnsMinusOneAtEof(SevenZMethod.DEFLATE);
    }

    @Test
    public void testSingleByteReadConsistentlyReturnsMinusOneAtEofUsingLZMA() throws Exception {
        singleByteReadConsistentlyReturnsMinusOneAtEof(SevenZMethod.LZMA);
    }

    @Test
    public void testSingleByteReadConsistentlyReturnsMinusOneAtEofUsingLZMA2() throws Exception {
        singleByteReadConsistentlyReturnsMinusOneAtEof(SevenZMethod.LZMA2);
    }
}
