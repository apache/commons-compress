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

package org.apache.commons.compress.archivers.tar;

import static org.apache.commons.lang3.reflect.FieldUtils.readDeclaredField;
import static org.apache.commons.lang3.reflect.FieldUtils.readField;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.function.IOConsumer;
import org.apache.commons.lang3.time.TimeZones;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TarArchiveInputStreamTest extends AbstractTest {

    @SuppressWarnings("resource") // Caller closes
    private static TarArchiveInputStream getTestStream(final String name) throws IOException {
        return TarArchiveInputStream.builder()
                .setURI(getURI(name))
                .get();
    }

    private void datePriorToEpoch(final String archive) throws Exception {
        try (TarArchiveInputStream in = getTestStream(archive)) {
            final TarArchiveEntry tae = in.getNextTarEntry();
            assertEquals("foo", tae.getName());
            assertEquals(TarConstants.LF_NORMAL, tae.getLinkFlag());
            final Calendar cal = Calendar.getInstance(TimeZones.GMT);
            cal.set(1969, 11, 31, 23, 59, 59);
            cal.set(Calendar.MILLISECOND, 0);
            assertEquals(cal.getTime(), tae.getLastModifiedDate());
            assertTrue(tae.isCheckSumOK());
        }
    }

    private void getNextEntryUntilIOException(final TarArchiveInputStream archive) {
        // Only on Windows: throws a UnmappableCharacterException
        assertThrows(IOException.class, () -> archive.forEach(IOConsumer.noop()));
    }

    @Test
    void testChecksumOnly4Byte() throws IOException {
        try (TarArchiveInputStream archive = TarArchiveInputStream.builder()
                .setURI(getURI("org/apache/commons/compress/COMPRESS-707/COMPRESS-707-lenient.tar"))
                .setLenient(true)
                .get()) {
            final TarArchiveEntry nextEntry = archive.getNextEntry();
            assertNotNull(nextEntry);
            assertEquals("hi-gary.txt", nextEntry.getName());
            assertTrue(nextEntry.isCheckSumOK());
        }
    }

    @Test
    void testCompress197() throws IOException {
        try (TarArchiveInputStream tar = getTestStream("COMPRESS-197.tar")) {
            TarArchiveEntry entry = tar.getNextTarEntry();
            assertNotNull(entry);
            while (entry != null) {
                assertTrue(entry.isTypeFlagUstar());
                entry = tar.getNextTarEntry();
            }
        }
    }

    @Test
    void testCompress197ForEach() throws IOException {
        try (TarArchiveInputStream tar = getTestStream("COMPRESS-197.tar")) {
            tar.forEach(IOConsumer.noop());
        }
    }

    @Test
    void testCompress558() throws IOException {
        final String folderName = "apache-activemq-5.16.0/examples/openwire/advanced-scenarios/jms-example-exclusive-consumer/src/main/";
        // @formatter:off
        final String consumerJavaName =
            "apache-activemq-5.16.0/examples/openwire/advanced-scenarios/jms-example-exclusive-consumer/src/main/java/example/queue/exclusive/Consumer.java";
        final String producerJavaName =
            "apache-activemq-5.16.0/examples/openwire/advanced-scenarios/jms-example-exclusive-consumer/src/main/java/example/queue/exclusive/Producer.java";
        // @formatter:on

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tos = new TarArchiveOutputStream(bos)) {
            tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
            final TarArchiveEntry rootfolder = new TarArchiveEntry(folderName);
            tos.putArchiveEntry(rootfolder);
            final TarArchiveEntry consumerJava = new TarArchiveEntry(consumerJavaName);
            tos.putArchiveEntry(consumerJava);
            final TarArchiveEntry producerJava = new TarArchiveEntry(producerJavaName);
            tos.putArchiveEntry(producerJava);
            tos.closeArchiveEntry();
        }
        final byte[] data = bos.toByteArray();
        try (TarArchiveInputStream tis =
                TarArchiveInputStream.builder().setByteArray(data).get()) {
            assertEquals(folderName, tis.getNextTarEntry().getName());
            assertEquals(TarConstants.LF_DIR, tis.getCurrentEntry().getLinkFlag());
            assertEquals(consumerJavaName, tis.getNextTarEntry().getName());
            assertEquals(TarConstants.LF_NORMAL, tis.getCurrentEntry().getLinkFlag());
            assertEquals(producerJavaName, tis.getNextTarEntry().getName());
            assertEquals(TarConstants.LF_NORMAL, tis.getCurrentEntry().getLinkFlag());
        }
    }

    private void testCompress666(final int factor, final boolean bufferInputStream, final String localPath) {
        final ExecutorService executorService = Executors.newFixedThreadPool(10);
        try {
            final List<Future<?>> tasks = IntStream.range(0, 200).mapToObj(index -> executorService.submit(() -> {
                TarArchiveEntry tarEntry = null;
                try (InputStream inputStream = getClass().getResourceAsStream(localPath);
                     // @formatter:off
                     TarArchiveInputStream tarInputStream = TarArchiveInputStream.builder()
                             .setInputStream(bufferInputStream ? new BufferedInputStream(new GZIPInputStream(inputStream)) : new GZIPInputStream(inputStream))
                             .setBlockSize(TarConstants.DEFAULT_RCDSIZE * factor)
                             .setRecordSize(TarConstants.DEFAULT_RCDSIZE)
                             .get()) {
                    // @formatter:on
                    while ((tarEntry = tarInputStream.getNextEntry()) != null) {
                        assertNotNull(tarEntry);
                    }
                } catch (final IOException e) {
                    fail(Objects.toString(tarEntry), e);
                }
            })).collect(Collectors.toList());
            final List<Exception> list = new ArrayList<>();
            for (final Future<?> future : tasks) {
                try {
                    future.get();
                } catch (final Exception e) {
                    list.add(e);
                }
            }
            // check:
            if (!list.isEmpty()) {
                fail(list.get(0));
            }
            // or:
            // assertTrue(list.isEmpty(), () -> list.size() + " exceptions: " + list.toString());
        } finally {
            executorService.shutdownNow();
        }
    }

    /**
     * Tests https://issues.apache.org/jira/browse/COMPRESS-666
     *
     * A factor of 20 is the default.
     */
    @ParameterizedTest
    @ValueSource(ints = { 1, 2, 4, 8, 16, 20, 32, 64, 128 })
    void testCompress666Buffered(final int factor) {
        testCompress666(factor, true, "/COMPRESS-666/compress-666.tar.gz");
    }

    /**
     * Tests https://issues.apache.org/jira/browse/COMPRESS-666
     *
     * A factor of 20 is the default.
     */
    @ParameterizedTest
    @ValueSource(ints = { 1, 2, 4, 8, 16, 20, 32, 64, 128 })
    void testCompress666Unbuffered(final int factor) {
        testCompress666(factor, false, "/COMPRESS-666/compress-666.tar.gz");
    }

    @Test
    void testDatePriorToEpochInGNUFormat() throws Exception {
        datePriorToEpoch("preepoch-star.tar");
    }

    @Test
    void testDatePriorToEpochInPAXFormat() throws Exception {
        datePriorToEpoch("preepoch-posix.tar");
    }

    @Test
    void testDirectoryWithLongNameEndsWithSlash() throws IOException, ArchiveException {
        final String rootPath = getTempDirFile().getAbsolutePath();
        final String dirDirectory = "COMPRESS-509";
        final int count = 100;
        final File root = new File(rootPath + "/" + dirDirectory);
        root.mkdirs();
        for (int i = 1; i < count; i++) {
            // create empty dirs with incremental length
            String subDir = "";
            for (int j = 0; j < i; j++) {
                subDir += "a";
            }
            final File dir = new File(rootPath + "/" + dirDirectory, "/" + subDir);
            dir.mkdir();
            // tar these dirs
            final String fileName = "/" + dirDirectory + "/" + subDir;
            final File tarF = new File(rootPath + "/tar" + i + ".tar");
            try (OutputStream dest = Files.newOutputStream(tarF.toPath())) {
                final TarArchiveOutputStream out = new TarArchiveOutputStream(new BufferedOutputStream(dest));
                out.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR);
                out.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
                final File file = new File(rootPath, fileName);
                final TarArchiveEntry entry = new TarArchiveEntry(file);
                entry.setName(fileName);
                out.putArchiveEntry(entry);
                out.closeArchiveEntry();
                out.flush();
            }
            // untar these tars
            try (InputStream is = Files.newInputStream(tarF.toPath());
                    TarArchiveInputStream debInputStream = ArchiveStreamFactory.DEFAULT.createArchiveInputStream("tar", is)) {
                TarArchiveEntry outEntry;
                while ((outEntry = debInputStream.getNextEntry()) != null) {
                    assertTrue(outEntry.getName().endsWith("/"), outEntry.getName());
                }
            }
        }
    }

    @Test
    void testGetAndSetOfPaxEntry() throws Exception {
        try (TarArchiveInputStream is = getTestStream("COMPRESS-356.tar")) {
            final TarArchiveEntry entry = is.getNextTarEntry();
            assertEquals("package/package.json", entry.getName());
            assertEquals(TarConstants.LF_NORMAL, entry.getLinkFlag());
            assertEquals(is.getCurrentEntry(), entry);
            final TarArchiveEntry weaselEntry = new TarArchiveEntry(entry.getName());
            weaselEntry.setSize(entry.getSize());
            is.setCurrentEntry(weaselEntry);
            assertEquals(entry, is.getCurrentEntry());
            assertNotSame(entry, is.getCurrentEntry());
            assertSame(weaselEntry, is.getCurrentEntry());
            assertThrows(IllegalStateException.class, () -> {
                is.setCurrentEntry(null);
                is.read();
            }, "should abort because current entry is nulled");
            is.setCurrentEntry(entry);
            is.read();
        }
    }

    /**
     * Depending on your setup, this test may need a small stack size {@code -Xss256k}.
     */
    @Test
    void testGetNextEntry() throws IOException {
        try (TarArchiveInputStream inputStream = getTestStream("org/apache/commons/compress/tar/getNextTarEntry.bin")) {
            final AtomicLong count = new AtomicLong();
            final TarArchiveEntry entry = inputStream.getNextEntry();
            assertNull(entry.getCreationTime());
            assertNull(entry.getLastAccessTime());
            assertEquals(new Date(0), entry.getLastModifiedDate());
            assertEquals(FileTime.fromMillis(0), entry.getLastModifiedTime());
            assertNull(entry.getStatusChangeTime());
            assertEquals(-1, entry.getDataOffset());
            assertEquals(0, entry.getDevMajor());
            assertEquals(0, entry.getDevMinor());
            assertEquals(0, entry.getDirectoryEntries().length);
            assertEquals(0, entry.getExtraPaxHeaders().size());
            assertEquals(0, entry.getOrderedSparseHeaders().size());
            assertEquals(0, entry.getSparseHeaders().size());
            assertNull(entry.getFile());
            assertNull(entry.getPath());
            assertEquals("", entry.getGroupName());
            assertEquals(0x1ff, entry.getMode());
            assertEquals("", entry.getName());
            assertEquals(0, entry.getRealSize());
            assertEquals(0, entry.getSize());
            assertEquals("", entry.getUserName());
            assertEquals("", entry.getLinkName());
            assertEquals(0x30, entry.getLinkFlag());
            assertEquals(0, entry.getLongGroupId());
            assertEquals(0, entry.getLongUserId());
            inputStream.forEach(e -> count.incrementAndGet());
            assertEquals(0, count.get());
        }
    }

    /**
     * Depending on your setup, this test may need a small stack size {@code -Xss256k}.
     */
    @Test
    void testGetNextTarEntryDeprecated() throws IOException {
        try (TarArchiveInputStream inputStream = getTestStream("org/apache/commons/compress/tar/getNextTarEntry.bin")) {
            final AtomicLong count = new AtomicLong();
            final TarArchiveEntry entry = inputStream.getNextTarEntry();
            assertNull(entry.getCreationTime());
            assertNull(entry.getLastAccessTime());
            assertEquals(new Date(0), entry.getLastModifiedDate());
            assertEquals(FileTime.fromMillis(0), entry.getLastModifiedTime());
            assertNull(entry.getStatusChangeTime());
            assertEquals(-1, entry.getDataOffset());
            assertEquals(0, entry.getDevMajor());
            assertEquals(0, entry.getDevMinor());
            assertEquals(0, entry.getDirectoryEntries().length);
            assertEquals(0, entry.getExtraPaxHeaders().size());
            assertEquals(0, entry.getOrderedSparseHeaders().size());
            assertEquals(0, entry.getSparseHeaders().size());
            assertNull(entry.getFile());
            assertNull(entry.getPath());
            assertEquals("", entry.getGroupName());
            assertEquals(0x1ff, entry.getMode());
            assertEquals("", entry.getName());
            assertEquals(0, entry.getRealSize());
            assertEquals(0, entry.getSize());
            assertEquals("", entry.getUserName());
            assertEquals("", entry.getLinkName());
            assertEquals(0x30, entry.getLinkFlag());
            assertEquals(0, entry.getLongGroupId());
            assertEquals(0, entry.getLongUserId());
            inputStream.forEach(e -> count.incrementAndGet());
            assertEquals(0, count.get());
        }
    }

    @Test
    void testMultiByteReadConsistentlyReturnsMinusOneAtEof() throws Exception {
        final byte[] buf = new byte[2];
        try (TarArchiveInputStream archive = getTestStream("bla.tar")) {
            assertNotNull(archive.getNextEntry());
            IOUtils.toByteArray(archive);
            assertEquals(-1, archive.read(buf));
            assertEquals(-1, archive.read(buf));
        }
    }

    @Test
    void testParseTarTruncatedInContent() throws IOException {
        try (TarArchiveInputStream archive = getTestStream("COMPRESS-544_truncated_in_content-fail.tar")) {
            getNextEntryUntilIOException(archive);
        }
    }

    @Test
    void testParseTarTruncatedInPadding() throws IOException {
        try (TarArchiveInputStream archive = getTestStream("COMPRESS-544_truncated_in_padding-fail.tar")) {
            getNextEntryUntilIOException(archive);
        }
    }

    @Test
    void testParseTarWithNonNumberPaxHeaders() throws IOException {
        try (TarArchiveInputStream archive = getTestStream("COMPRESS-529-fail.tar")) {
            assertThrows(ArchiveException.class, () -> archive.getNextEntry());
        }
    }

    @Test
    void testParseTarWithSpecialPaxHeaders() throws IOException {
        try (TarArchiveInputStream archive = getTestStream("COMPRESS-530-fail.tar")) {
            assertThrows(ArchiveException.class, () -> archive.getNextEntry());
            assertThrows(ArchiveException.class, () -> IOUtils.toByteArray(archive));
        }
    }

    /**
     * Depending on your setup, this test may need a small stack size {@code -Xss1m}.
     */
    @Test
    void testPaxHeaders() throws IOException {
        try (TarArchiveInputStream inputStream = getTestStream("org/apache/commons/compress/tar/paxHeaders.bin")) {
            assertThrows(ArchiveException.class, inputStream::getNextEntry);
        }
    }

    @Test
    void testReadsArchiveCompletely_COMPRESS245() {
        try (InputStream is = TarArchiveInputStreamTest.class.getResourceAsStream("/COMPRESS-245.tar.gz")) {
            final InputStream gin = new GZIPInputStream(is);
            try (TarArchiveInputStream tar =
                    TarArchiveInputStream.builder().setInputStream(gin).get()) {
                int count = 0;
                TarArchiveEntry entry = tar.getNextTarEntry();
                while (entry != null) {
                    count++;
                    entry = tar.getNextTarEntry();
                }
                assertEquals(31, count);
            }
        } catch (final IOException e) {
            fail("COMPRESS-245: " + e.getMessage());
        }
    }

    @Test
    void testRejectsArchivesWithNegativeSizes() throws Exception {
        try (TarArchiveInputStream archive = getTestStream("COMPRESS-569-fail.tar")) {
            getNextEntryUntilIOException(archive);
        }
    }

    /**
     * This test ensures the implementation is reading the padded last block if a tool has added one to an archive
     */
    @Test
    void testShouldConsumeArchiveCompletely() throws Exception {
        try (InputStream is = TarArchiveInputStreamTest.class.getResourceAsStream("/archive_with_trailer.tar");
                TarArchiveInputStream tar = TarArchiveInputStream.builder().setInputStream(is).get()) {
            while (tar.getNextTarEntry() != null) {
                // just consume the archive
            }
            final byte[] expected = { 'H', 'e', 'l', 'l', 'o', ',', ' ', 'w', 'o', 'r', 'l', 'd', '!', '\n' };
            final byte[] actual = new byte[expected.length];
            is.read(actual);
            assertArrayEquals(expected, actual, () -> Arrays.toString(actual));
        }
    }

    @Test
    void testShouldReadBigGid() throws Exception {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tos = new TarArchiveOutputStream(bos)) {
            tos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
            final TarArchiveEntry t = new TarArchiveEntry("name");
            t.setGroupId(4294967294L);
            t.setSize(1);
            tos.putArchiveEntry(t);
            tos.write(30);
            tos.closeArchiveEntry();
        }
        final byte[] data = bos.toByteArray();
        try (TarArchiveInputStream tis =
                TarArchiveInputStream.builder().setByteArray(data).get()) {
            final TarArchiveEntry t = tis.getNextTarEntry();
            assertEquals(4294967294L, t.getLongGroupId());
        }
    }

    /**
     * @see <a href="https://issues.apache.org/jira/browse/COMPRESS-324">COMPRESS-324</a>
     */
    @Test
    void testShouldReadGNULongNameEntryWithWrongName() throws Exception {
        try (TarArchiveInputStream is = getTestStream("COMPRESS-324.tar")) {
            final TarArchiveEntry entry = is.getNextTarEntry();
            assertEquals(
                    "1234567890123456789012345678901234567890123456789012345678901234567890"
                            + "1234567890123456789012345678901234567890123456789012345678901234567890"
                            + "1234567890123456789012345678901234567890123456789012345678901234567890" + "1234567890123456789012345678901234567890.txt",
                    entry.getName());
        }
    }

    @Test
    void testShouldThrowAnExceptionOnTruncatedEntries() throws Exception {
        final Path dir = createTempDirectory("COMPRESS-279");
        try (TarArchiveInputStream is = getTestStream("COMPRESS-279-fail.tar")) {
            assertThrows(ArchiveException.class, () -> {
                TarArchiveEntry entry = is.getNextTarEntry();
                int count = 0;
                while (entry != null) {
                    Files.copy(is, dir.resolve(String.valueOf(count)));
                    count++;
                    entry = is.getNextTarEntry();
                }
            });
        }
    }

    @Test
    void testShouldThrowAnExceptionOnTruncatedStream() throws Exception {
        final Path dir = createTempDirectory("COMPRESS-279");
        try (TarArchiveInputStream is = getTestStream("COMPRESS-279-fail.tar")) {
            final AtomicInteger count = new AtomicInteger();
            assertThrows(ArchiveException.class, () -> is.forEach(entry -> Files.copy(is, dir.resolve(String.valueOf(count.getAndIncrement())))));
        }
    }

    @Test
    void testShouldUseSpecifiedEncodingWhenReadingGNULongNames() throws Exception {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final String encoding = StandardCharsets.UTF_16.name();
        final String name = "1234567890123456789012345678901234567890123456789" + "01234567890123456789012345678901234567890123456789" + "01234567890\u00e4";
        try (TarArchiveOutputStream tos = new TarArchiveOutputStream(bos, encoding)) {
            tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
            final TarArchiveEntry t = new TarArchiveEntry(name);
            t.setSize(1);
            tos.putArchiveEntry(t);
            tos.write(30);
            tos.closeArchiveEntry();
        }
        final byte[] data = bos.toByteArray();
        final ByteArrayInputStream bis = new ByteArrayInputStream(data);
        try (TarArchiveInputStream tis = TarArchiveInputStream.builder()
                .setByteArray(data)
                .setCharset(encoding)
                .get()) {
            final TarArchiveEntry t = tis.getNextTarEntry();
            assertEquals(name, t.getName());
        }
    }

    @Test
    void testSingleArgumentConstructor() throws Exception {
        final InputStream inputStream = mock(InputStream.class);
        try (TarArchiveInputStream archiveStream = new TarArchiveInputStream(inputStream)) {
            assertEquals(10240, readDeclaredField(archiveStream, "blockSize", true));
            final byte[] recordBuffer = (byte[]) readField(archiveStream, "recordBuffer", true);
            assertEquals(512, recordBuffer.length);
            assertEquals(Charset.defaultCharset(), archiveStream.getCharset());
            assertEquals(false, readField(archiveStream, "lenient", true));
        }
    }

    @Test
    void testSingleByteReadConsistentlyReturnsMinusOneAtEof() throws Exception {
        try (TarArchiveInputStream archive = getTestStream("bla.tar")) {
            assertNotNull(archive.getNextEntry());
            IOUtils.toByteArray(archive);
            assertEquals(-1, archive.read());
            assertEquals(-1, archive.read());
        }
    }

    /**
     * @see <a href="https://issues.apache.org/jira/browse/COMPRESS-417">COMPRESS-417</a>
     */
    @Test
    void testSkipsDevNumbersWhenEntryIsNoDevice() throws Exception {
        try (TarArchiveInputStream is = getTestStream("COMPRESS-417.tar")) {
            assertEquals("test1.xml", is.getNextTarEntry().getName());
            assertEquals(TarConstants.LF_NORMAL, is.getCurrentEntry().getLinkFlag());
            assertEquals("test2.xml", is.getNextTarEntry().getName());
            assertEquals(TarConstants.LF_NORMAL, is.getCurrentEntry().getLinkFlag());
            assertNull(is.getNextTarEntry());
        }
    }

    /**
     * @see <a href="https://issues.apache.org/jira/browse/COMPRESS-355">COMPRESS-355</a>
     */
    @Test
    void testSurvivesBlankLinesInPaxHeader() throws Exception {
        try (TarArchiveInputStream is = getTestStream("COMPRESS-355.tar")) {
            final TarArchiveEntry entry = is.getNextTarEntry();
            assertEquals("package/package.json", entry.getName());
            assertEquals(TarConstants.LF_NORMAL, entry.getLinkFlag());
            assertNull(is.getNextTarEntry());
        }
    }

    /**
     * @see <a href="https://issues.apache.org/jira/browse/COMPRESS-356">COMPRESS-356</a>
     */
    @Test
    void testSurvivesPaxHeaderWithNameEndingInSlash() throws Exception {
        try (TarArchiveInputStream is = getTestStream("COMPRESS-356.tar")) {
            final TarArchiveEntry entry = is.getNextTarEntry();
            assertEquals("package/package.json", entry.getName());
            assertEquals(TarConstants.LF_NORMAL, entry.getLinkFlag());
            assertNull(is.getNextTarEntry());
        }
    }

    @Test
    void testThrowException() throws IOException {
        try (TarArchiveInputStream archive = getTestStream("COMPRESS-553-fail.tar")) {
            getNextEntryUntilIOException(archive);
        }
    }

    @Test
    void testThrowExceptionWithNullEntry() throws IOException {
        try (TarArchiveInputStream archive = getTestStream("COMPRESS-554-fail.tar")) {
            getNextEntryUntilIOException(archive);
        }
    }

    @Test
    void testWorkaroundForBrokenTimeHeader() throws Exception {
        try (TarArchiveInputStream in = getTestStream("simple-aix-native-tar.tar")) {
            TarArchiveEntry tae = in.getNextTarEntry();
            tae = in.getNextTarEntry();
            assertEquals("sample/link-to-txt-file.lnk", tae.getName());
            assertEquals(TarConstants.LF_SYMLINK, tae.getLinkFlag());
            assertEquals(new Date(0), tae.getLastModifiedDate());
            assertTrue(tae.isSymbolicLink());
            assertTrue(tae.isCheckSumOK());
        }
    }
}
