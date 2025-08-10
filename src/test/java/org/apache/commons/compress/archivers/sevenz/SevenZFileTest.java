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
package org.apache.commons.compress.archivers.sevenz;

import static java.nio.charset.StandardCharsets.UTF_16LE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

import javax.crypto.Cipher;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.MemoryLimitException;
import org.apache.commons.compress.PasswordRequiredException;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.utils.MultiReadOnlySeekableByteChannel;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ChecksumInputStream;
import org.junit.jupiter.api.Test;

class SevenZFileTest extends AbstractTest {
    private static final String TEST2_CONTENT = "<?xml version = '1.0'?>\r\n<!DOCTYPE connections>\r\n<meinxml>\r\n\t<leer />\r\n</meinxml>\n";

    private static boolean isStrongCryptoAvailable() throws NoSuchAlgorithmException {
        return Cipher.getMaxAllowedKeyLength("AES/ECB/PKCS5Padding") >= 256;
    }

    private void assertDate(final SevenZArchiveEntry entry, final String value, final Function<SevenZArchiveEntry, Boolean> hasValue,
            final Function<SevenZArchiveEntry, FileTime> timeFunction, final Function<SevenZArchiveEntry, Date> dateFunction) {
        if (value != null) {
            assertTrue(hasValue.apply(entry));
            final Instant parsedInstant = Instant.parse(value);
            final FileTime parsedFileTime = FileTime.from(parsedInstant);
            assertEquals(parsedFileTime, timeFunction.apply(entry));
            assertEquals(Date.from(parsedInstant), dateFunction.apply(entry));
        } else {
            assertFalse(hasValue.apply(entry));
            assertThrows(UnsupportedOperationException.class, () -> timeFunction.apply(entry));
            assertThrows(UnsupportedOperationException.class, () -> dateFunction.apply(entry));
        }
    }

    private void assertDates(final SevenZArchiveEntry entry, final String modified, final String access, final String creation) {
        assertDate(entry, modified, SevenZArchiveEntry::getHasLastModifiedDate, SevenZArchiveEntry::getLastModifiedTime,
                SevenZArchiveEntry::getLastModifiedDate);
        assertDate(entry, access, SevenZArchiveEntry::getHasAccessDate, SevenZArchiveEntry::getAccessTime, SevenZArchiveEntry::getAccessDate);
        assertDate(entry, creation, SevenZArchiveEntry::getHasCreationDate, SevenZArchiveEntry::getCreationTime, SevenZArchiveEntry::getCreationDate);
    }

    private void checkHelloWorld(final String fileName) throws Exception {
        try (SevenZFile sevenZFile = getSevenZFile(fileName)) {
            final SevenZArchiveEntry entry = sevenZFile.getNextEntry();
            assertEquals("Hello world.txt", entry.getName());
            assertDates(entry, "2013-05-07T19:40:48Z", null, null);
            final byte[] contents = new byte[(int) entry.getSize()];
            int off = 0;
            while (off < contents.length) {
                final int bytesRead = sevenZFile.read(contents, off, contents.length - off);
                assertTrue(bytesRead >= 0, "bytesRead exp: >=0, act: " + bytesRead);
                off += bytesRead;
            }
            assertEquals("Hello, world!\n", new String(contents, UTF_8));
            assertNull(sevenZFile.getNextEntry());
        }
    }

    private SevenZFile getSevenZFile(final String specialPath) throws IOException {
        return SevenZFile.builder().setFile(getFile(specialPath)).get();
    }

    private byte[] read(final SevenZFile sevenZFile, final SevenZArchiveEntry entry) throws IOException {
        try (InputStream inputStream = sevenZFile.getInputStream(entry)) {
            return IOUtils.toByteArray(inputStream);
        }
    }

    private byte[] readFully(final SevenZFile archive) throws IOException {
        final byte[] buf = new byte[1024];
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (int len = 0; (len = archive.read(buf)) > 0;) {
            baos.write(buf, 0, len);
        }
        return baos.toByteArray();
    }

    @Test
    void test7zDecryptUnarchive() throws Exception {
        if (isStrongCryptoAvailable()) {
            test7zUnarchive(getFile("bla.encrypted.7z"), SevenZMethod.LZMA, // stack LZMA + AES
                    "foo".getBytes(UTF_16LE));
        }
    }

    @Test
    void test7zDecryptUnarchiveUsingCharArrayPassword() throws Exception {
        if (isStrongCryptoAvailable()) {
            test7zUnarchive(getFile("bla.encrypted.7z"), SevenZMethod.LZMA, // stack LZMA + AES
                    "foo".toCharArray());
        }
    }

    @Test
    void test7zDeflate64Unarchive() throws Exception {
        test7zUnarchive(getFile("bla.deflate64.7z"), SevenZMethod.DEFLATE64);
    }

    @Test
    void test7zDeflateUnarchive() throws Exception {
        test7zUnarchive(getFile("bla.deflate.7z"), SevenZMethod.DEFLATE);
    }

    @Test
    void test7zMultiVolumeUnarchive() throws Exception {
        try (@SuppressWarnings("deprecation")
        SevenZFile sevenZFile = new SevenZFile(MultiReadOnlySeekableByteChannel.forFiles(getFile("bla-multi.7z.001"), getFile("bla-multi.7z.002")))) {
            test7zUnarchive(sevenZFile, SevenZMethod.LZMA2);
        }
        try (SevenZFile sevenZFile = SevenZFile.builder()
                .setSeekableByteChannel(MultiReadOnlySeekableByteChannel.forFiles(getFile("bla-multi.7z.001"), getFile("bla-multi.7z.002"))).get()) {
            test7zUnarchive(sevenZFile, SevenZMethod.LZMA2);
        }
    }

    @Test
    void test7zUnarchive() throws Exception {
        test7zUnarchive(getFile("bla.7z"), SevenZMethod.LZMA);
    }

    private void test7zUnarchive(final File file, final SevenZMethod method) throws Exception {
        test7zUnarchive(file, method, false);
    }

    private void test7zUnarchive(final File file, final SevenZMethod method, final boolean tryToRecoverBrokenArchives) throws Exception {
        test7zUnarchive(file, method, (char[]) null, tryToRecoverBrokenArchives);
    }

    private void test7zUnarchive(final File file, final SevenZMethod method, final byte[] password) throws Exception {
        try (@SuppressWarnings("deprecation")
        SevenZFile sevenZFile = new SevenZFile(file, password)) {
            test7zUnarchive(sevenZFile, method);
        }
        try (SevenZFile sevenZFile = SevenZFile.builder().setFile(file).setPassword(password).get()) {
            test7zUnarchive(sevenZFile, method);
        }
    }

    private void test7zUnarchive(final File file, final SevenZMethod m, final char[] password) throws Exception {
        test7zUnarchive(file, m, password, false);
    }

    private void test7zUnarchive(final File file, final SevenZMethod m, final char[] password, final boolean tryToRecoverBrokenArchives) throws Exception {
        try (@SuppressWarnings("deprecation")
        SevenZFile sevenZFile = new SevenZFile(file, password,
                SevenZFileOptions.builder().withTryToRecoverBrokenArchives(tryToRecoverBrokenArchives).build())) {
            test7zUnarchive(sevenZFile, m);
        }
        try (SevenZFile sevenZFile = SevenZFile.builder().setFile(file).setPassword(password).setTryToRecoverBrokenArchives(tryToRecoverBrokenArchives).get()) {
            test7zUnarchive(sevenZFile, m);
        }
    }

    private void test7zUnarchive(final SevenZFile sevenZFile, final SevenZMethod m) throws Exception {
        SevenZArchiveEntry entry = sevenZFile.getNextEntry();
        assertEquals("test1.xml", entry.getName());
        assertDates(entry, "2007-11-14T10:19:02Z", null, null);
        assertEquals(m, entry.getContentMethods().iterator().next().getMethod());
        entry = sevenZFile.getNextEntry();
        assertEquals("test2.xml", entry.getName());
        assertDates(entry, "2007-11-14T10:19:02Z", null, null);
        assertEquals(m, entry.getContentMethods().iterator().next().getMethod());
        final byte[] contents = new byte[(int) entry.getSize()];
        int off = 0;
        while (off < contents.length) {
            final int bytesRead = sevenZFile.read(contents, off, contents.length - off);
            assertTrue(bytesRead >= 0, "bytesRead exp: >=0, act: " + bytesRead);
            off += bytesRead;
        }
        assertEquals(TEST2_CONTENT, new String(contents, UTF_8));
        assertNull(sevenZFile.getNextEntry());
    }

    @Test
    void test7zUnarchiveWithDefectHeader() throws Exception {
        test7zUnarchive(getFile("bla.noendheaderoffset.7z"), SevenZMethod.LZMA, true);
    }

    @Test
    void test7zUnarchiveWithDefectHeaderFailsByDefault() throws Exception {
        assertThrows(ArchiveException.class, () -> test7zUnarchive(getFile("bla.noendheaderoffset.7z"), SevenZMethod.LZMA));
    }

    @Test
    void testAllEmptyFilesArchive() throws Exception {
        try (SevenZFile archive = getSevenZFile("7z-empty-mhc-off.7z")) {
            final SevenZArchiveEntry e = archive.getNextEntry();
            assertNotNull(e);
            assertEquals("empty", e.getName());
            assertDates(e, "2013-05-14T17:50:19Z", null, null);
            assertNull(archive.getNextEntry());
        }
    }

    @Test
    void testAssertFitsIntoNonNegativeInt() throws IOException {
        assertThrows(ArchiveException.class,
                () -> SevenZFile.builder().setPath("src/test/resources/org/apache/commons/compress/sevenz/assertFitsIntoNonNegativeInt.bin").get().close());
    }

    /**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-256"
     */
    @Test
    void testCompressedHeaderWithNonDefaultDictionarySize() throws Exception {
        try (SevenZFile sevenZFile = getSevenZFile("COMPRESS-256.7z")) {
            int count = 0;
            while (sevenZFile.getNextEntry() != null) {
                count++;
            }
            assertEquals(446, count);
        }
    }

    @Test
    void testEncryptedArchiveRequiresPassword() throws Exception {
        final PasswordRequiredException ex = assertThrows(PasswordRequiredException.class, () -> getSevenZFile("bla.encrypted.7z").close(),
                "shouldn't decrypt without a password");
        final String msg = ex.getMessage();
        assertTrue(msg.startsWith("Cannot read encrypted content from "), "Should start with whining about being unable to decrypt");
        assertTrue(msg.endsWith(" without a password."), "Should finish the sentence properly");
        assertTrue(msg.contains("bla.encrypted.7z"), "Should contain archive's name");
    }

    @Test
    void testExtractNonExistSpecifiedFile() throws Exception {
        try (SevenZFile sevenZFile = getSevenZFile("COMPRESS-256.7z");
                SevenZFile anotherSevenZFile = getSevenZFile("bla.7z")) {
            for (final SevenZArchiveEntry nonExistEntry : anotherSevenZFile.getEntries()) {
                assertThrows(IllegalArgumentException.class, () -> sevenZFile.getInputStream(nonExistEntry));
            }
        }
    }

    @Test
    void testExtractSpecifiedFile() throws Exception {
        try (SevenZFile sevenZFile = getSevenZFile("COMPRESS-256.7z")) {
            // @formatter:off
            final String testTxtContents =
                      "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011";
            // @formatter:off
            for (final SevenZArchiveEntry entry : sevenZFile.getEntries()) {
                if (entry.getName().equals("commons-compress-1.7-src/src/test/resources/test.txt")) {
                    final byte[] contents = new byte[(int) entry.getSize()];
                    int off = 0;
                    final InputStream inputStream = sevenZFile.getInputStream(entry);
                    while (off < contents.length) {
                        final int bytesRead = inputStream.read(contents, off, contents.length - off);
                        assertTrue(bytesRead >= 0, "bytesRead exp: >=0, act: " + bytesRead);
                        off += bytesRead;
                    }
                    assertEquals(testTxtContents, new String(contents, UTF_8));
                    break;
                }
            }
        }
    }

    @Test
    void testExtractSpecifiedFileDeprecated() throws Exception {
        try (@SuppressWarnings("deprecation")
        SevenZFile sevenZFile = new SevenZFile(getFile("COMPRESS-256.7z"))) {
            // @formatter:off
            final String testTxtContents =
                    "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011";
            // @formatter:on
            for (final SevenZArchiveEntry entry : sevenZFile.getEntries()) {
                if (entry.getName().equals("commons-compress-1.7-src/src/test/resources/test.txt")) {
                    final byte[] contents = new byte[(int) entry.getSize()];
                    int off = 0;
                    final InputStream inputStream = sevenZFile.getInputStream(entry);
                    while (off < contents.length) {
                        final int bytesRead = inputStream.read(contents, off, contents.length - off);
                        assertTrue(bytesRead >= 0, "bytesRead exp: >=0, act: " + bytesRead);
                        off += bytesRead;
                    }
                    assertEquals(testTxtContents, new String(contents, UTF_8));
                    break;
                }
            }
        }
    }

    @Test
    void testGetDefaultName() throws Exception {
        try (SevenZFile sevenZFile = getSevenZFile("bla.deflate64.7z")) {
            assertEquals("bla.deflate64", sevenZFile.getDefaultName());
        }
        try (SevenZFile sevenZFile = SevenZFile.builder().setSeekableByteChannel(Files.newByteChannel(getFile("bla.deflate64.7z").toPath())).get()) {
            assertNull(sevenZFile.getDefaultName());
        }
        try (@SuppressWarnings("deprecation")
        SevenZFile sevenZFile = new SevenZFile(Files.newByteChannel(getFile("bla.deflate64.7z").toPath()), "foo")) {
            assertEquals("foo~", sevenZFile.getDefaultName());
        }
        try (SevenZFile sevenZFile = SevenZFile.builder().setSeekableByteChannel(Files.newByteChannel(getFile("bla.deflate64.7z").toPath()))
                .setDefaultName("foo").get()) {
            assertEquals("foo~", sevenZFile.getDefaultName());
        }
        try (SevenZFile sevenZFile = SevenZFile.builder().setSeekableByteChannel(Files.newByteChannel(getFile("bla.deflate64.7z").toPath()))
                .setDefaultName(".foo").get()) {
            assertEquals(".foo~", sevenZFile.getDefaultName());
        }
    }

    @Test
    void testGetEntriesOfUnarchiveInMemoryTest() throws IOException {
        final byte[] data = readAllBytes("bla.7z");
        try (SevenZFile sevenZFile = SevenZFile.builder().setSeekableByteChannel(new SeekableInMemoryByteChannel(data)).get()) {
            final Iterable<SevenZArchiveEntry> entries = sevenZFile.getEntries();
            final Iterator<SevenZArchiveEntry> iter = entries.iterator();
            SevenZArchiveEntry entry = iter.next();
            assertEquals("test1.xml", entry.getName());
            entry = iter.next();
            assertEquals("test2.xml", entry.getName());
            assertFalse(iter.hasNext());
        }
    }

    @Test
    void testGetEntriesOfUnarchiveTest() throws IOException {
        try (SevenZFile sevenZFile = getSevenZFile("bla.7z")) {
            final Iterable<SevenZArchiveEntry> entries = sevenZFile.getEntries();
            final Iterator<SevenZArchiveEntry> iter = entries.iterator();
            SevenZArchiveEntry entry = iter.next();
            assertEquals("test1.xml", entry.getName());
            entry = iter.next();
            assertEquals("test2.xml", entry.getName());
            assertFalse(iter.hasNext());
        }
    }

    @Test
    void testGivenNameWinsOverDefaultName() throws Exception {
        try (@SuppressWarnings("deprecation")
        SevenZFile sevenZFile = new SevenZFile(getFile("bla.7z"), SevenZFileOptions.builder().withUseDefaultNameForUnnamedEntries(true).build())) {
            SevenZArchiveEntry ae = sevenZFile.getNextEntry();
            assertNotNull(ae);
            assertEquals("test1.xml", ae.getName());
            ae = sevenZFile.getNextEntry();
            assertNotNull(ae);
            assertEquals("test2.xml", ae.getName());
            assertNull(sevenZFile.getNextEntry());
        }
        try (SevenZFile sevenZFile = SevenZFile.builder().setFile(getFile("bla.7z")).setUseDefaultNameForUnnamedEntries(true).get()) {
            SevenZArchiveEntry ae = sevenZFile.getNextEntry();
            assertNotNull(ae);
            assertEquals("test1.xml", ae.getName());
            ae = sevenZFile.getNextEntry();
            assertNotNull(ae);
            assertEquals("test2.xml", ae.getName());
            assertNull(sevenZFile.getNextEntry());
        }
    }

    /**
     * @see <a href="https://issues.apache.org/jira/browse/COMPRESS-492">COMPRESS-492</a>
     */
    @Test
    void testHandlesEmptyArchiveWithFilesInfo() throws Exception {
        final File file = newTempFile("empty.7z");
        try (SevenZOutputFile s = new SevenZOutputFile(file)) {
            // do nothing
        }
        try (SevenZFile z = SevenZFile.builder().setFile(file).get()) {
            assertFalse(z.getEntries().iterator().hasNext());
            assertNull(z.getNextEntry());
        }
    }

    /**
     * @see <a href="https://issues.apache.org/jira/browse/COMPRESS-492">COMPRESS-492</a>
     */
    @Test
    void testHandlesEmptyArchiveWithoutFilesInfo() throws Exception {
        try (SevenZFile z = getSevenZFile("COMPRESS-492.7z")) {
            assertFalse(z.getEntries().iterator().hasNext());
            assertNull(z.getNextEntry());
        }
    }

    @Test
    void testHelloWorldHeaderCompressionOffCopy() throws Exception {
        checkHelloWorld("7z-hello-mhc-off-copy.7z");
    }

    @Test
    void testHelloWorldHeaderCompressionOffLZMA2() throws Exception {
        checkHelloWorld("7z-hello-mhc-off-lzma2.7z");
    }

    @Test
    void testLimitExtractionMemory() {
        assertThrows(MemoryLimitException.class, () -> {
            try (SevenZFile sevenZFile = SevenZFile.builder().setFile(getFile("bla.7z")).setMaxMemoryLimitKb(1).get()) {
                // Do nothing. Exception should be thrown
            }
        });
        assertThrows(MemoryLimitException.class, () -> {
            try (SevenZFile sevenZFile = SevenZFile.builder().setFile(getFile("bla.7z")).setMaxMemoryLimitKiB(1).get()) {
                // Do nothing. Exception should be thrown
            }
        });
    }

    @Test
    void testNoNameCanBeReplacedByDefaultName() throws Exception {
        try (SevenZFile sevenZFile = SevenZFile.builder().setFile(getFile("bla-nonames.7z")).setUseDefaultNameForUnnamedEntries(true).get()) {
            SevenZArchiveEntry ae = sevenZFile.getNextEntry();
            assertNotNull(ae);
            assertEquals("bla-nonames", ae.getName());
            ae = sevenZFile.getNextEntry();
            assertNotNull(ae);
            assertEquals("bla-nonames", ae.getName());
            assertNull(sevenZFile.getNextEntry());
        }
    }

    @Test
    void testNoNameMeansNoNameByDefault() throws Exception {
        try (SevenZFile sevenZFile = getSevenZFile("bla-nonames.7z")) {
            SevenZArchiveEntry ae = sevenZFile.getNextEntry();
            assertNotNull(ae);
            assertNull(ae.getName());
            ae = sevenZFile.getNextEntry();
            assertNotNull(ae);
            assertNull(ae.getName());
            assertNull(sevenZFile.getNextEntry());
        }
    }

    @Test
    void testNoOOMOnCorruptedHeader() throws IOException {
        final List<Path> testFiles = new ArrayList<>();
        testFiles.add(getPath("COMPRESS-542-1.7z"));
        testFiles.add(getPath("COMPRESS-542-2.7z"));
        testFiles.add(getPath("COMPRESS-542-endheadercorrupted.7z"));
        testFiles.add(getPath("COMPRESS-542-endheadercorrupted2.7z"));
        for (final Path file : testFiles) {
            {
                final IOException e = assertThrows(ArchiveException.class, () -> {
                    try (@SuppressWarnings("deprecation")
                    SevenZFile sevenZFile = new SevenZFile(Files.newByteChannel(file),
                            SevenZFileOptions.builder().withTryToRecoverBrokenArchives(true).build())) {
                        // do nothing
                    }
                }, "Expected IOException: start header corrupt and unable to guess end header");
                assertEquals("Start header corrupt and unable to guess end header", e.getMessage());
            }
            {
                final IOException e = assertThrows(ArchiveException.class, () -> {
                    try (SevenZFile sevenZFile = SevenZFile.builder().setSeekableByteChannel(Files.newByteChannel(file)).setTryToRecoverBrokenArchives(true)
                            .get()) {
                        // do nothing
                    }
                }, "Expected IOException: start header corrupt and unable to guess end header");
                assertEquals("Start header corrupt and unable to guess end header", e.getMessage());
            }
        }
    }

    @Test
    void testNumCyclesPower() throws IOException {
        final String fixture = "src/test/resources/org/apache/commons/compress/sevenz/numCyclesPower.bin";
        final char[] password = "secret".toCharArray();
        // @formatter:off
        assertThrows(ArchiveException.class,
                () -> SevenZFile.builder()
                    .setPath(fixture)
                    .get().close());
        assertThrows(ArchiveException.class,
                () -> SevenZFile.builder()
                    .setPath(fixture)
                    .setPassword(password)
                    .get().close());
        assertThrows(ArchiveException.class,
                () -> SevenZFile.builder()
                    .setPath(fixture)
                    .setPassword(password)
                    .setTryToRecoverBrokenArchives(true)
                    .get().close());
        // @formatter:on
    }

    @Test
    void testRandomAccessMultipleReadSameFile() throws Exception {
        try (SevenZFile sevenZFile = getSevenZFile("COMPRESS-256.7z")) {
            // @formatter:off
            final String testTxtContents =
                      "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011";
            // @formatter:on
            SevenZArchiveEntry entry;
            SevenZArchiveEntry testTxtEntry = null;
            while ((entry = sevenZFile.getNextEntry()) != null) {
                if (entry.getName().equals("commons-compress-1.7-src/src/test/resources/test.txt")) {
                    testTxtEntry = entry;
                    break;
                }
            }
            assertNotNull(testTxtEntry, "testTxtEntry");
            final byte[] contents = new byte[(int) testTxtEntry.getSize()];
            int numberOfReads = 10;
            while (numberOfReads-- > 0) {
                try (InputStream inputStream = sevenZFile.getInputStream(testTxtEntry)) {
                    int off = 0;
                    while (off < contents.length) {
                        final int bytesRead = inputStream.read(contents, off, contents.length - off);
                        assertTrue(bytesRead >= 0, "bytesRead exp: >=0, act: " + bytesRead);
                        off += bytesRead;
                    }
                    assertEquals(SevenZMethod.LZMA2, testTxtEntry.getContentMethods().iterator().next().getMethod());
                    assertEquals(testTxtContents, new String(contents, UTF_8));
                }
            }
        }
    }

    @Test
    void testRandomAccessTogetherWithSequentialAccess() throws Exception {
        try (SevenZFile sevenZFile = getSevenZFile("COMPRESS-256.7z")) {
            // @formatter:off
            final String testTxtContents =
                      "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011";
            final String filesTxtContents =
                      "0xxxxxxxxx10xxxxxxxx20xxxxxxxx30xxxxxxxx40xxxxxxxx50xxxxxxxx60xxxxxxxx70xxxxxxxx80xxxxxxxx90xxxxxxxx100"
                    + "xxxxxxx110xxxxxxx120xxxxxxx130xxxxxxx -> 0yyyyyyyyy10yyyyyyyy20yyyyyyyy30yyyyyyyy40yyyyyyyy50yyyyyyyy60yyyyyyyy70yyyyyyyy80"
                    + "yyyyyyyy90yyyyyyyy100yyyyyyy110yyyyyyy120yyyyyyy130yyyyyyy\n";
            // @formatter:on
            int off;
            byte[] contents;

            // call getNextEntry and read before calling getInputStream
            sevenZFile.getNextEntry();
            SevenZArchiveEntry nextEntry = sevenZFile.getNextEntry();
            contents = new byte[(int) nextEntry.getSize()];
            off = 0;

            assertEquals(SevenZMethod.LZMA2, nextEntry.getContentMethods().iterator().next().getMethod());

            // just read them
            while (off < contents.length) {
                final int bytesRead = sevenZFile.read(contents, off, contents.length - off);
                assertTrue(bytesRead >= 0, "bytesRead exp: >=0, act: " + bytesRead);
                off += bytesRead;
            }

            sevenZFile.getNextEntry();
            sevenZFile.getNextEntry();

            for (final SevenZArchiveEntry entry : sevenZFile.getEntries()) {
                // commons-compress-1.7-src/src/test/resources/test.txt
                if (entry.getName().equals("commons-compress-1.7-src/src/test/resources/longsymlink/files.txt")) {
                    contents = new byte[(int) entry.getSize()];
                    off = 0;
                    final InputStream inputStream = sevenZFile.getInputStream(entry);
                    while (off < contents.length) {
                        final int bytesRead = inputStream.read(contents, off, contents.length - off);
                        assertTrue(bytesRead >= 0, "bytesRead exp: >=0, act: " + bytesRead);
                        off += bytesRead;
                    }
                    assertEquals(SevenZMethod.LZMA2, entry.getContentMethods().iterator().next().getMethod());
                    assertEquals(filesTxtContents, new String(contents, UTF_8));
                    break;
                }
            }

            // call getNextEntry after getInputStream
            nextEntry = sevenZFile.getNextEntry();
            while (!nextEntry.getName().equals("commons-compress-1.7-src/src/test/resources/test.txt")) {
                nextEntry = sevenZFile.getNextEntry();
            }

            contents = new byte[(int) nextEntry.getSize()];
            off = 0;
            while (off < contents.length) {
                final int bytesRead = sevenZFile.read(contents, off, contents.length - off);
                assertTrue(bytesRead >= 0, "bytesRead exp: >=0, act: " + bytesRead);
                off += bytesRead;
            }
            assertEquals(SevenZMethod.LZMA2, nextEntry.getContentMethods().iterator().next().getMethod());
            assertEquals(testTxtContents, new String(contents, UTF_8));
        }
    }

    @Test
    void testRandomAccessWhenJumpingBackwards() throws Exception {
        try (SevenZFile sevenZFile = getSevenZFile("COMPRESS-256.7z")) {
            // @formatter:off
            final String testTxtContents =
                      "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011";
            // @formatter:on
            SevenZArchiveEntry entry;
            SevenZArchiveEntry testTxtEntry = null;
            while ((entry = sevenZFile.getNextEntry()) != null) {
                if (entry.getName().equals("commons-compress-1.7-src/src/test/resources/test.txt")) {
                    testTxtEntry = entry;
                    break;
                }
            }

            // read the next entry and jump back using random access
            final SevenZArchiveEntry entryAfterTestTxtEntry = sevenZFile.getNextEntry();
            final byte[] entryAfterTestTxtEntryContents = new byte[(int) entryAfterTestTxtEntry.getSize()];
            int off = 0;
            while (off < entryAfterTestTxtEntryContents.length) {
                final int bytesRead = sevenZFile.read(entryAfterTestTxtEntryContents, off, entryAfterTestTxtEntryContents.length - off);
                assertTrue(bytesRead >= 0, "bytesRead exp: >=0, act: " + bytesRead);
                off += bytesRead;
            }

            // jump backwards
            assertNotNull(testTxtEntry, "testTxtEntry");
            final byte[] contents = new byte[(int) testTxtEntry.getSize()];
            try (InputStream inputStream = sevenZFile.getInputStream(testTxtEntry)) {
                off = 0;
                while (off < contents.length) {
                    final int bytesRead = inputStream.read(contents, off, contents.length - off);
                    assertTrue(bytesRead >= 0, "bytesRead exp: >=0, act: " + bytesRead);
                    off += bytesRead;
                }
                assertEquals(SevenZMethod.LZMA2, testTxtEntry.getContentMethods().iterator().next().getMethod());
                assertEquals(testTxtContents, new String(contents, UTF_8));
            }

            // then read the next entry using getNextEntry
            final SevenZArchiveEntry nextTestTxtEntry = sevenZFile.getNextEntry();
            final byte[] nextTestContents = new byte[(int) nextTestTxtEntry.getSize()];
            off = 0;
            while (off < nextTestContents.length) {
                final int bytesRead = sevenZFile.read(nextTestContents, off, nextTestContents.length - off);
                assertTrue(bytesRead >= 0, "bytesRead exp: >=0, act: " + bytesRead);
                off += bytesRead;
            }

            assertEquals(nextTestTxtEntry.getName(), entryAfterTestTxtEntry.getName());
            assertEquals(nextTestTxtEntry.getSize(), entryAfterTestTxtEntry.getSize());
            assertArrayEquals(nextTestContents, entryAfterTestTxtEntryContents);
        }
    }

    @Test
    void testRandomAccessWhenJumpingForwards() throws Exception {
        try (SevenZFile sevenZFile = getSevenZFile("COMPRESS-256.7z")) {
            // @formatter:off
            final String testTxtContents =
                      "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011\n"
                    + "111111111111111111111111111000101011";
            // @formatter:on
            SevenZArchiveEntry testTxtEntry = null;
            final Iterable<SevenZArchiveEntry> entries = sevenZFile.getEntries();
            for (final SevenZArchiveEntry Entry : entries) {
                testTxtEntry = Entry;
                if (testTxtEntry.getName().equals("commons-compress-1.7-src/src/test/resources/test.txt")) {
                    break;
                }
            }
            final SevenZArchiveEntry firstEntry = sevenZFile.getNextEntry();
            // only read some of the data of the first entry
            byte[] contents = new byte[(int) firstEntry.getSize() / 2];
            sevenZFile.read(contents);

            // and the third entry
            sevenZFile.getNextEntry();
            final SevenZArchiveEntry thirdEntry = sevenZFile.getNextEntry();
            contents = new byte[(int) thirdEntry.getSize() / 2];
            sevenZFile.read(contents);

            // and then read a file after the first entry using random access
            assertNotNull(testTxtEntry, "testTxtEntry");
            contents = new byte[(int) testTxtEntry.getSize()];
            int numberOfReads = 10;
            while (numberOfReads-- > 0) {
                try (InputStream inputStream = sevenZFile.getInputStream(testTxtEntry)) {
                    int off = 0;
                    while (off < contents.length) {
                        final int bytesRead = inputStream.read(contents, off, contents.length - off);
                        assertTrue(bytesRead >= 0, "bytesRead exp: >=0, act: " + bytesRead);
                        off += bytesRead;
                    }
                    assertEquals(SevenZMethod.LZMA2, testTxtEntry.getContentMethods().iterator().next().getMethod());
                    assertEquals(testTxtContents, new String(contents, UTF_8));
                }
            }
        }
    }

    // https://issues.apache.org/jira/browse/COMPRESS-320
    @Test
    void testRandomlySkippingEntries() throws Exception {
        // Read sequential reference.
        final Map<String, byte[]> entriesByName = new HashMap<>();
        try (SevenZFile archive = getSevenZFile("COMPRESS-320/Copy.7z")) {
            SevenZArchiveEntry entry;
            while ((entry = archive.getNextEntry()) != null) {
                if (entry.hasStream()) {
                    entriesByName.put(entry.getName(), readFully(archive));
                }
            }
        }

        final String[] variants = { "BZip2-solid.7z", "BZip2.7z", "Copy-solid.7z", "Copy.7z", "Deflate-solid.7z", "Deflate.7z", "LZMA-solid.7z", "LZMA.7z",
                "LZMA2-solid.7z", "LZMA2.7z",
                // TODO: unsupported compression method.
                // "PPMd-solid.7z",
                // "PPMd.7z",
        };

        // TODO: use randomized testing for predictable, but different, randomness.
        final Random rnd = new Random(0xdeadbeef);
        for (final String fileName : variants) {
            try (SevenZFile archive = getSevenZFile("COMPRESS-320/" + fileName)) {

                SevenZArchiveEntry entry;
                while ((entry = archive.getNextEntry()) != null) {
                    // Sometimes skip reading entries.
                    if (rnd.nextBoolean()) {
                        continue;
                    }

                    if (entry.hasStream()) {
                        assertTrue(entriesByName.containsKey(entry.getName()));
                        final byte[] content = readFully(archive);
                        assertArrayEquals(content, entriesByName.get(entry.getName()), "Content mismatch on: " + fileName + "!" + entry.getName());
                    }
                }

            }
        }
    }

    @Test
    void testReadBigSevenZipFile() throws IOException {
        try (SevenZFile sevenZFile = getSevenZFile("COMPRESS-592.7z")) {
            SevenZArchiveEntry entry = sevenZFile.getNextEntry();
            while (entry != null) {
                if (entry.hasStream()) {
                    final byte[] content = new byte[(int) entry.getSize()];
                    sevenZFile.read(content);
                }
                entry = sevenZFile.getNextEntry();
            }
        }
    }

    /**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-348"
     */
    @Test
    void testReadEntriesOfSize0() throws IOException {
        try (SevenZFile sevenZFile = getSevenZFile("COMPRESS-348.7z")) {
            int entries = 0;
            SevenZArchiveEntry entry = sevenZFile.getNextEntry();
            while (entry != null) {
                entries++;
                final int b = sevenZFile.read();
                if ("2.txt".equals(entry.getName()) || "5.txt".equals(entry.getName())) {
                    assertEquals(-1, b);
                } else {
                    assertNotEquals(-1, b);
                }
                entry = sevenZFile.getNextEntry();
            }
            assertEquals(5, entries);
        }
    }

    /**
     * Test case for <a href="https://issues.apache.org/jira/browse/COMPRESS-681">COMPRESS-681</a>.
     */
    @Test
    void testReadingArchiveProperties() throws IOException {
        final String entryName = "COMPRESS-681.txt";
        final String entryContent = "https://issues.apache.org/jira/browse/COMPRESS-681";
        try (SevenZFile archive = getSevenZFile("COMPRESS-681.7z")) {
            final SevenZArchiveEntry entry = archive.getNextEntry();
            assertEquals(entryName, entry.getName());
            final byte[] contents = new byte[(int) entry.getSize()];
            int off = 0;
            while (off < contents.length) {
                final int bytesRead = archive.read(contents, off, contents.length - off);
                assertTrue(bytesRead >= 0, "bytesRead exp: >=0, act: " + bytesRead);
                off += bytesRead;
            }
            assertEquals(entryContent, new String(contents, UTF_8));
            assertNull(archive.getNextEntry());
        }
    }

    @Test
    void testReadingBackDeltaDistance() throws Exception {
        final File output = newTempFile("delta-distance.7z");
        try (SevenZOutputFile outArchive = new SevenZOutputFile(output)) {
            outArchive.setContentMethods(
                    Arrays.asList(new SevenZMethodConfiguration(SevenZMethod.DELTA_FILTER, 32), new SevenZMethodConfiguration(SevenZMethod.LZMA2)));
            final SevenZArchiveEntry entry = new SevenZArchiveEntry();
            entry.setName("foo.txt");
            outArchive.putArchiveEntry(entry);
            outArchive.write(new byte[] { 'A' });
            outArchive.closeArchiveEntry();
        }
        try (SevenZFile archive = SevenZFile.builder().setFile(output).get()) {
            final SevenZArchiveEntry entry = archive.getNextEntry();
            final SevenZMethodConfiguration m = entry.getContentMethods().iterator().next();
            assertEquals(SevenZMethod.DELTA_FILTER, m.getMethod());
            assertEquals(32, m.getOptions());
        }
    }

    @Test
    void testReadingBackLZMA2DictSize() throws Exception {
        final File output = newTempFile("lzma2-dictsize.7z");
        try (SevenZOutputFile outArchive = new SevenZOutputFile(output)) {
            outArchive.setContentMethods(Arrays.asList(new SevenZMethodConfiguration(SevenZMethod.LZMA2, 1 << 20)));
            final SevenZArchiveEntry entry = new SevenZArchiveEntry();
            entry.setName("foo.txt");
            outArchive.putArchiveEntry(entry);
            outArchive.write(new byte[] { 'A' });
            outArchive.closeArchiveEntry();
        }
        try (SevenZFile archive = SevenZFile.builder().setFile(output).get()) {
            final SevenZArchiveEntry entry = archive.getNextEntry();
            final SevenZMethodConfiguration m = entry.getContentMethods().iterator().next();
            assertEquals(SevenZMethod.LZMA2, m.getMethod());
            assertEquals(1 << 20, m.getOptions());
        }
    }

    @Test
    void testReadTimesFromFile() throws IOException {
        try (SevenZFile sevenZFile = getSevenZFile("times.7z")) {
            SevenZArchiveEntry entry = sevenZFile.getNextEntry();
            assertNotNull(entry);
            assertEquals("test", entry.getName());
            assertTrue(entry.isDirectory());
            assertDates(entry, "2022-03-21T14:50:46.2099751Z", "2022-03-21T14:50:46.2099751Z", "2022-03-16T10:19:24.1051115Z");
            // next entry
            entry = sevenZFile.getNextEntry();
            assertNotNull(entry);
            assertEquals("test/test-times.txt", entry.getName());
            assertFalse(entry.isDirectory());
            assertDates(entry, "2022-03-18T10:00:15Z", "2022-03-18T10:14:37.8130002Z", "2022-03-18T10:14:37.8110032Z");
            // next entry
            entry = sevenZFile.getNextEntry();
            assertNotNull(entry);
            assertEquals("test/test-times2.txt", entry.getName());
            assertFalse(entry.isDirectory());
            assertDates(entry, "2022-03-18T10:00:19Z", "2022-03-18T10:14:37.8170038Z", "2022-03-18T10:14:37.8140004Z");
            // next entry
            entry = sevenZFile.getNextEntry();
            assertNull(entry);
        }
    }

    @Test
    void testRemainingBytesUnchangedAfterRead() throws Exception {
        try (SevenZFile sevenZFile = getSevenZFile("COMPRESS-256.7z")) {
            for (final SevenZArchiveEntry entry : sevenZFile.getEntries()) {
                final InputStream inputStream = sevenZFile.getInputStream(entry);
                if (!(inputStream instanceof ChecksumInputStream)) {
                    continue;
                }
                assertEquals(entry.getSize(), ((ChecksumInputStream) inputStream).getRemaining());
                // read 10 byte
                final byte[] bytes = new byte[10];
                inputStream.read(bytes);
                assertNotEquals(entry.getSize(), ((ChecksumInputStream) inputStream).getRemaining());
            }
        }
    }

    @Test
    void testRetrieveInputStreamForAllEntriesMultipleTimes() throws IOException {
        try (SevenZFile sevenZFile = getSevenZFile("bla.7z")) {
            for (final SevenZArchiveEntry entry : sevenZFile.getEntries()) {
                final byte[] firstRead = read(sevenZFile, entry);
                final byte[] secondRead = read(sevenZFile, entry);
                assertArrayEquals(firstRead, secondRead);
            }
        }
    }

    @Test
    void testRetrieveInputStreamForAllEntriesWithoutCRCMultipleTimes() throws IOException {
        try (SevenZOutputFile out = new SevenZOutputFile(newTempFile("test.7z"))) {
            final Path inputFile = Files.createTempFile("SevenZTestTemp", "");
            final SevenZArchiveEntry entry = out.createArchiveEntry(inputFile.toFile(), "test.txt");
            out.putArchiveEntry(entry);
            out.write("Test".getBytes(UTF_8));
            out.closeArchiveEntry();
            Files.deleteIfExists(inputFile);
        }
        try (SevenZFile sevenZFile = SevenZFile.builder().setFile(newTempFile("test.7z")).get()) {
            for (final SevenZArchiveEntry entry : sevenZFile.getEntries()) {
                final byte[] firstRead = read(sevenZFile, entry);
                final byte[] secondRead = read(sevenZFile, entry);
                assertArrayEquals(firstRead, secondRead);
            }
        }
    }

    @Test
    void testRetrieveInputStreamForShuffledEntries() throws IOException {
        try (SevenZFile sevenZFile = getSevenZFile("COMPRESS-348.7z")) {
            final List<SevenZArchiveEntry> entries = (List<SevenZArchiveEntry>) sevenZFile.getEntries();
            Collections.shuffle(entries);
            for (final SevenZArchiveEntry entry : entries) {
                read(sevenZFile, entry);
            }
        }
    }

    @Test
    void testSanityCheckUnpackInfo() throws IOException {
        assertThrows(ArchiveException.class,
                () -> SevenZFile.builder().setPath("src/test/resources/org/apache/commons/compress/sevenz/sanityCheckUnpackInfo.bin").get().close());
    }

    @Test
    void testSevenZWithEOS() throws IOException {
        try (SevenZFile sevenZFile = getSevenZFile("lzma-with-eos.7z")) {
            final List<SevenZArchiveEntry> entries = (List<SevenZArchiveEntry>) sevenZFile.getEntries();
            for (final SevenZArchiveEntry entry : entries) {
                read(sevenZFile, entry);
            }
        }
    }

    @Test
    void testSignatureCheck() {
        assertTrue(SevenZFile.matches(SevenZFile.SIGNATURE, SevenZFile.SIGNATURE.length));
        assertTrue(SevenZFile.matches(SevenZFile.SIGNATURE, SevenZFile.SIGNATURE.length + 1));
        final byte[] data0 = Arrays.copyOf(SevenZFile.SIGNATURE, SevenZFile.SIGNATURE.length - 1);
        assertFalse(SevenZFile.matches(data0, data0.length));
        final byte[] data1 = new byte[] { 1, 2, 3, 4, 5, 6 };
        assertFalse(SevenZFile.matches(data1, data1.length));
        final byte[] data2 = new byte[] { '7', 'z', (byte) 0xBC, (byte) 0xAF, 0x27, 0x1C };
        assertTrue(SevenZFile.matches(data2, data2.length));
        final byte[] data3 = new byte[] { '7', 'z', (byte) 0xBC, (byte) 0xAF, 0x27, 0x1D };
        assertFalse(SevenZFile.matches(data3, data3.length));
    }
}
