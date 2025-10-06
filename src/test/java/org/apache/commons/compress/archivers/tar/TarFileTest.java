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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.AbstractArchiveFileTest;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.TimeZones;
import org.junit.jupiter.api.Test;

class TarFileTest extends AbstractArchiveFileTest<TarArchiveEntry> {

    private void datePriorToEpoch(final String archive) throws Exception {
        try (TarFile tarFile = TarFile.builder().setURI(getURI(archive)).get()) {
            final TarArchiveEntry entry = tarFile.getEntries().get(0);
            assertEquals("foo", entry.getName());
            assertEquals(TarConstants.LF_NORMAL, entry.getLinkFlag());
            final Calendar cal = Calendar.getInstance(TimeZones.GMT);
            cal.set(1969, 11, 31, 23, 59, 59);
            cal.set(Calendar.MILLISECOND, 0);
            assertEquals(cal.getTime(), entry.getLastModifiedDate());
            assertTrue(entry.isCheckSumOK());
        }
    }

    /**
     * This test ensures the implementation is reading the padded last block if a tool has added one to an archive
     */
    @Test
    void testArchiveWithTrailer() throws IOException {
        try (SeekableByteChannel channel = Files.newByteChannel(getPath("archive_with_trailer.tar"));
                TarFile tarfile = TarFile.builder().setChannel(channel).get()) {
            final String tarAppendix = "Hello, world!\n";
            final ByteBuffer buffer = ByteBuffer.allocate(tarAppendix.length());
            channel.read(buffer);
            assertEquals(tarAppendix, new String(buffer.array()));
        }
    }

    @Test
    void testBuilderSeekableByteChannel() throws IOException {
        try (SeekableByteChannel channel = Files.newByteChannel(getPath("archive_with_trailer.tar"));
                TarFile tarfile = TarFile.builder()
                        .setChannel(channel)
                        .setBlockSize(TarConstants.DEFAULT_BLKSIZE)
                        .setRecordSize(TarConstants.DEFAULT_RCDSIZE)
                        .setLenient(false)
                        .get()) {
            final String tarAppendix = "Hello, world!\n";
            final ByteBuffer buffer = ByteBuffer.allocate(tarAppendix.length());
            channel.read(buffer);
            assertEquals(tarAppendix, new String(buffer.array()));
        }
    }

    @Test
    void testCompress197() throws IOException {
        try (TarFile tarFile = TarFile.builder().setURI(getURI("COMPRESS-197.tar")).get()) {
            // noop
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
        try (TarFile tarFile = TarFile.builder().setByteArray(data).get()) {
            final List<TarArchiveEntry> entries = tarFile.getEntries();
            assertEquals(folderName, entries.get(0).getName());
            assertEquals(TarConstants.LF_DIR, entries.get(0).getLinkFlag());
            assertEquals(consumerJavaName, entries.get(1).getName());
            assertEquals(TarConstants.LF_NORMAL, entries.get(1).getLinkFlag());
            assertEquals(producerJavaName, entries.get(2).getName());
            assertEquals(TarConstants.LF_NORMAL, entries.get(2).getLinkFlag());
        }
    }

    @Test
    void testCompress657() throws IOException {
        try (TarFile tarFile = TarFile.builder().setURI(getURI("COMPRESS-657/orjson-3.7.8.tar")).get()) {
            for (final TarArchiveEntry entry : tarFile.getEntries()) {
                if (entry.isDirectory()) {
                    // An entry cannot be a directory and a "normal file" at the same time.
                    assertFalse(entry.isFile(), "Entry '" + entry.getName() + "' is both a directory and a file");
                }
            }
        }
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
    void testDirectoryWithLongNameEndsWithSlash() throws IOException {
        final String rootPath = getTempDirFile().getAbsolutePath();
        final String dirDirectory = "COMPRESS-509";
        final int count = 100;
        final File root = new File(rootPath + "/" + dirDirectory);
        root.mkdirs();
        for (int i = 1; i < count; i++) {
            // create empty dirs with incremental length
            final String subDir = StringUtils.repeat('a', i);
            final File dir = new File(rootPath + "/" + dirDirectory, "/" + subDir);
            dir.mkdir();

            // tar these dirs
            final String fileName = "/" + dirDirectory + "/" + subDir;
            final File tarF = new File(rootPath + "/tar" + i + ".tar");
            try (OutputStream dest = Files.newOutputStream(tarF.toPath());
                    TarArchiveOutputStream out = new TarArchiveOutputStream(new BufferedOutputStream(dest))) {
                out.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
                out.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

                final File file = new File(rootPath, fileName);
                final TarArchiveEntry entry = new TarArchiveEntry(file);
                entry.setName(fileName);
                out.putArchiveEntry(entry);
                out.closeArchiveEntry();
                out.flush();
            }
            // untar these tars
            try (TarFile tarFile = TarFile.builder().setFile(tarF).get()) {
                for (final TarArchiveEntry entry : tarFile.getEntries()) {
                    assertTrue(entry.getName().endsWith("/"), "Entry name: " + entry.getName());
                }
            }
        }
    }

    @Test
    void testMultiByteReadConsistentlyReturnsMinusOneAtEof() throws Exception {
        final byte[] buf = new byte[2];
        try (TarFile tarFile = TarFile.builder().setURI(getURI("bla.tar")).get();
                InputStream input = tarFile.getInputStream(tarFile.getEntries().get(0))) {
            IOUtils.toByteArray(input);
            assertEquals(-1, input.read(buf));
            assertEquals(-1, input.read(buf));
        }
    }

    @Test
    void testParseTarTruncatedInContent() {
        assertThrows(IOException.class, () -> TarFile.builder().setURI(getURI("COMPRESS-544_truncated_in_content.tar")).get());
    }

    @Test
    void testParseTarTruncatedInPadding() {
        assertThrows(IOException.class, () -> TarFile.builder().setURI(getURI("COMPRESS-544_truncated_in_padding.tar")).get());
    }

    @Test
    void testParseTarWithNonNumberPaxHeaders() {
        assertThrows(ArchiveException.class, () -> TarFile.builder().setURI(getURI("COMPRESS-529-fail.tar")).get());
    }

    @Test
    void testParseTarWithSpecialPaxHeaders() {
        assertThrows(ArchiveException.class, () -> TarFile.builder().setURI(getURI("COMPRESS-530-fail.tar")).get());
    }

    @Test
    void testReadsArchiveCompletely_COMPRESS245() {
        try {
            final Path tempTar = tempResultDir.toPath().resolve("COMPRESS-245.tar");
            try (GZIPInputStream gin = new GZIPInputStream(Files.newInputStream(getPath("COMPRESS-245.tar.gz")))) {
                Files.copy(gin, tempTar);
            }
            try (TarFile tarFile = TarFile.builder().setPath(tempTar).get()) {
                assertEquals(31, tarFile.getEntries().size());
            }
        } catch (final IOException e) {
            fail("COMPRESS-245: " + e.getMessage());
        }
    }

    @Test
    void testRejectsArchivesWithNegativeSizes() throws Exception {
        assertThrows(ArchiveException.class, () -> TarFile.builder().setURI(getURI("COMPRESS-569-fail.tar")).get());
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
        try (TarFile tarFile = TarFile.builder().setByteArray(data).get()) {
            final List<TarArchiveEntry> entries = tarFile.getEntries();
            assertEquals(4294967294L, entries.get(0).getLongGroupId());
        }
    }

    /**
     * @see <a href="https://issues.apache.org/jira/browse/COMPRESS-324">COMPRESS-324</a>
     */
    @Test
    void testShouldReadGNULongNameEntryWithWrongName() throws Exception {
        try (TarFile tarFile = TarFile.builder().setURI(getURI("COMPRESS-324.tar")).get()) {
            final List<TarArchiveEntry> entries = tarFile.getEntries();
            assertEquals(
                    "1234567890123456789012345678901234567890123456789012345678901234567890"
                            + "1234567890123456789012345678901234567890123456789012345678901234567890"
                            + "1234567890123456789012345678901234567890123456789012345678901234567890" + "1234567890123456789012345678901234567890.txt",
                    entries.get(0).getName());
        }
    }

    @Test
    void testShouldThrowAnExceptionOnTruncatedEntries() throws Exception {
        createTempDirectory("COMPRESS-279");
        assertThrows(IOException.class, () -> TarFile.builder().setURI(getURI("COMPRESS-279.tar")).get());
    }

    @Test
    void testShouldUseSpecifiedEncodingWhenReadingGNULongNames() throws Exception {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Charset encoding = StandardCharsets.UTF_16;
        final String name = "1234567890123456789012345678901234567890123456789" + "01234567890123456789012345678901234567890123456789" + "01234567890\u00e4";
        try (TarArchiveOutputStream tos = new TarArchiveOutputStream(bos, encoding.name())) {
            tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
            final TarArchiveEntry t = new TarArchiveEntry(name);
            t.setSize(1);
            tos.putArchiveEntry(t);
            tos.write(30);
            tos.closeArchiveEntry();
        }
        final byte[] data = bos.toByteArray();
        try (TarFile tarFile = TarFile.builder().setByteArray(data).setCharset(encoding).get()) {
            final List<TarArchiveEntry> entries = tarFile.getEntries();
            assertEquals(1, entries.size());
            assertEquals(name, entries.get(0).getName());
        }
    }

    @Test
    void testSingleByteReadConsistentlyReturnsMinusOneAtEof() throws Exception {
        try (TarFile tarFile = TarFile.builder().setURI(getURI("bla.tar")).get();
                InputStream input = tarFile.getInputStream(tarFile.getEntries().get(0))) {
            IOUtils.toByteArray(input);
            assertEquals(-1, input.read());
            assertEquals(-1, input.read());
        }
    }

    /**
     * @see <a href="https://issues.apache.org/jira/browse/COMPRESS-417">COMPRESS-417</a>
     */
    @Test
    void testSkipsDevNumbersWhenEntryIsNoDevice() throws Exception {
        try (TarFile tarFile = TarFile.builder().setURI(getURI("COMPRESS-417.tar")).get()) {
            final List<TarArchiveEntry> entries = tarFile.getEntries();
            assertEquals(2, entries.size());
            assertEquals("test1.xml", entries.get(0).getName());
            assertEquals(TarConstants.LF_NORMAL, entries.get(0).getLinkFlag());
            assertEquals("test2.xml", entries.get(1).getName());
            assertEquals(TarConstants.LF_NORMAL, entries.get(1).getLinkFlag());
        }
    }

    /**
     * @see <a href="https://issues.apache.org/jira/browse/COMPRESS-355">COMPRESS-355</a>
     */
    @Test
    void testSurvivesBlankLinesInPaxHeader() throws Exception {
        try (TarFile tarFile = TarFile.builder().setURI(getURI("COMPRESS-355.tar")).get()) {
            final List<TarArchiveEntry> entries = tarFile.getEntries();
            assertEquals(1, entries.size());
            assertEquals("package/package.json", entries.get(0).getName());
            assertEquals(TarConstants.LF_NORMAL, entries.get(0).getLinkFlag());
        }
    }

    /**
     * @see <a href="https://issues.apache.org/jira/browse/COMPRESS-356">COMPRESS-356</a>
     */
    @Test
    void testSurvivesPaxHeaderWithNameEndingInSlash() throws Exception {
        try (TarFile tarFile = TarFile.builder().setURI(getURI("COMPRESS-356.tar")).get()) {
            final List<TarArchiveEntry> entries = tarFile.getEntries();
            assertEquals(1, entries.size());
            assertEquals("package/package.json", entries.get(0).getName());
            assertEquals(TarConstants.LF_NORMAL, entries.get(0).getLinkFlag());
        }
    }

    @Test
    void testThrowException() {
        assertThrows(ArchiveException.class, () -> TarFile.builder().setURI(getURI("COMPRESS-553-fail.tar")).get());
    }

    @Test
    void testThrowExceptionWithNullEntry() {
        // Only on Windows: throws a UnmappableCharacterException
        assertThrows(IOException.class, () -> TarFile.builder().setURI(getURI("COMPRESS-554-fail.tar")).get());
    }

    @Test
    void testWorkaroundForBrokenTimeHeader() throws IOException {
        try (TarFile tarFile = TarFile.builder().setURI(getURI("simple-aix-native-tar.tar")).get()) {
            final List<TarArchiveEntry> entries = tarFile.getEntries();
            assertEquals(3, entries.size());
            final TarArchiveEntry entry = entries.get(1);
            assertEquals("sample/link-to-txt-file.lnk", entry.getName());
            assertEquals(TarConstants.LF_SYMLINK, entry.getLinkFlag());
            assertEquals(new Date(0), entry.getLastModifiedDate());
            assertTrue(entry.isSymbolicLink());
            assertTrue(entry.isCheckSumOK());
        }
    }

    @Override
    protected TarFile getArchiveFile() throws Exception {
        return TarFile.builder().setPath(getPath("bla.tar")).get();
    }
}
