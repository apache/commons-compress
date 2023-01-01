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
package org.apache.commons.compress.archivers.tar;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.utils.CharsetNames;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.Test;

public class TarFileTest extends AbstractTestCase {

    /**
     * This test ensures the implementation is reading the padded last block if a tool has added one to an archive
     */
    @Test
    public void archiveWithTrailer() throws IOException {
        try (final SeekableByteChannel channel = Files.newByteChannel(getPath("archive_with_trailer.tar"));
             final TarFile tarfile = new TarFile(channel, TarConstants.DEFAULT_BLKSIZE, TarConstants.DEFAULT_RCDSIZE, null, false)) {
            final String tarAppendix = "Hello, world!\n";
            final ByteBuffer buffer = ByteBuffer.allocate(tarAppendix.length());
            channel.read(buffer);
            assertEquals(tarAppendix, new String(buffer.array()));
        }
    }

    private void datePriorToEpoch(final String archive) throws Exception {
        try (final TarFile tarFile = new TarFile(getPath(archive))) {
            final TarArchiveEntry entry = tarFile.getEntries().get(0);
            assertEquals("foo", entry.getName());
            final Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            cal.set(1969, 11, 31, 23, 59, 59);
            cal.set(Calendar.MILLISECOND, 0);
            assertEquals(cal.getTime(), entry.getLastModifiedDate());
            assertTrue(entry.isCheckSumOK());
        }
    }

    @Test
    public void datePriorToEpochInGNUFormat() throws Exception {
        datePriorToEpoch("preepoch-star.tar");
    }

    @Test
    public void datePriorToEpochInPAXFormat() throws Exception {
        datePriorToEpoch("preepoch-posix.tar");
    }

    @Test
    public void multiByteReadConsistentlyReturnsMinusOneAtEof() throws Exception {
        final byte[] buf = new byte[2];
        try (final TarFile tarFile = new TarFile(getPath("bla.tar"));
             final InputStream input = tarFile.getInputStream(tarFile.getEntries().get(0))) {
            IOUtils.toByteArray(input);
            assertEquals(-1, input.read(buf));
            assertEquals(-1, input.read(buf));
        }
    }

    @Test
    public void readsArchiveCompletely_COMPRESS245() {
        try {
            final Path tempTar = resultDir.toPath().resolve("COMPRESS-245.tar");
            try (final GZIPInputStream gin = new GZIPInputStream(
                    Files.newInputStream(getPath("COMPRESS-245.tar.gz")))) {
                Files.copy(gin, tempTar);
            }
            try (final TarFile tarFile = new TarFile(tempTar)) {
                assertEquals(31, tarFile.getEntries().size());
            }
        } catch (final IOException e) {
            fail("COMPRESS-245: " + e.getMessage());
        }
    }

    @Test
    public void rejectsArchivesWithNegativeSizes() throws Exception {
        assertThrows(IOException.class, () -> new TarFile(getFile("COMPRESS-569.tar")));
    }

    @Test
    public void shouldReadBigGid() throws Exception {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (final TarArchiveOutputStream tos = new TarArchiveOutputStream(bos)) {
            tos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
            final TarArchiveEntry t = new TarArchiveEntry("name");
            t.setGroupId(4294967294L);
            t.setSize(1);
            tos.putArchiveEntry(t);
            tos.write(30);
            tos.closeArchiveEntry();
        }
        final byte[] data = bos.toByteArray();
        try (final TarFile tarFile = new TarFile(data)) {
            final List<TarArchiveEntry> entries = tarFile.getEntries();
            assertEquals(4294967294L, entries.get(0).getLongGroupId());
        }
    }

    /**
     * @link "https://issues.apache.org/jira/browse/COMPRESS-324"
     */
    @Test
    public void shouldReadGNULongNameEntryWithWrongName() throws Exception {
        try (final TarFile tarFile = new TarFile(getPath("COMPRESS-324.tar"))) {
            final List<TarArchiveEntry> entries = tarFile.getEntries();
            assertEquals("1234567890123456789012345678901234567890123456789012345678901234567890"
                            + "1234567890123456789012345678901234567890123456789012345678901234567890"
                            + "1234567890123456789012345678901234567890123456789012345678901234567890"
                            + "1234567890123456789012345678901234567890.txt",
                    entries.get(0).getName());
        }
    }

    @Test
    public void shouldThrowAnExceptionOnTruncatedEntries() throws Exception {
        final File dir = mkdir("COMPRESS-279");
        assertThrows(IOException.class, () -> new TarFile(getPath("COMPRESS-279.tar")));
        rmdir(dir);
    }

    @Test
    public void shouldUseSpecifiedEncodingWhenReadingGNULongNames()
            throws Exception {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final String encoding = CharsetNames.UTF_16;
        final String name = "1234567890123456789012345678901234567890123456789"
                + "01234567890123456789012345678901234567890123456789"
                + "01234567890\u00e4";
        try (final TarArchiveOutputStream tos = new TarArchiveOutputStream(bos, encoding)) {
            tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
            final TarArchiveEntry t = new TarArchiveEntry(name);
            t.setSize(1);
            tos.putArchiveEntry(t);
            tos.write(30);
            tos.closeArchiveEntry();
        }
        final byte[] data = bos.toByteArray();
        try (final TarFile tarFile = new TarFile(data, encoding)) {
            final List<TarArchiveEntry> entries = tarFile.getEntries();
            assertEquals(1, entries.size());
            assertEquals(name, entries.get(0).getName());
        }
    }

    @Test
    public void singleByteReadConsistentlyReturnsMinusOneAtEof() throws Exception {
        try (final TarFile tarFile = new TarFile(getPath("bla.tar"));
             final InputStream input = tarFile.getInputStream(tarFile.getEntries().get(0))) {
            IOUtils.toByteArray(input);
            assertEquals(-1, input.read());
            assertEquals(-1, input.read());
        }
    }

    /**
     * @link "https://issues.apache.org/jira/browse/COMPRESS-417"
     */
    @Test
    public void skipsDevNumbersWhenEntryIsNoDevice() throws Exception {
        try (final TarFile tarFile = new TarFile(getPath("COMPRESS-417.tar"))) {
            final List<TarArchiveEntry> entries = tarFile.getEntries();
            assertEquals(2, entries.size());
            assertEquals("test1.xml", entries.get(0).getName());
            assertEquals("test2.xml", entries.get(1).getName());
        }
    }

    /**
     * @link "https://issues.apache.org/jira/browse/COMPRESS-355"
     */
    @Test
    public void survivesBlankLinesInPaxHeader() throws Exception {
        try (final TarFile tarFile = new TarFile(getPath("COMPRESS-355.tar"))) {
            final List<TarArchiveEntry> entries = tarFile.getEntries();
            assertEquals(1, entries.size());
            assertEquals("package/package.json", entries.get(0).getName());
        }
    }

    /**
     * @link "https://issues.apache.org/jira/browse/COMPRESS-356"
     */
    @Test
    public void survivesPaxHeaderWithNameEndingInSlash() throws Exception {
        try (final TarFile tarFile = new TarFile(getPath("COMPRESS-356.tar"))) {
            final List<TarArchiveEntry> entries = tarFile.getEntries();
            assertEquals(1, entries.size());
            assertEquals("package/package.json", entries.get(0).getName());
        }
    }

    @Test
    public void testCompress197() throws IOException {
        try (final TarFile tarFile = new TarFile(getPath("COMPRESS-197.tar"))) {
            // noop
        }
    }

    @Test
    public void testCompress558() throws IOException {
        final String folderName = "apache-activemq-5.16.0/examples/openwire/advanced-scenarios/jms-example-exclusive-consumer/src/main/";
        final String consumerJavaName = "apache-activemq-5.16.0/examples/openwire/advanced-scenarios/jms-example-exclusive-consumer/src/main/java/example/queue/exclusive/Consumer.java";
        final String producerJavaName = "apache-activemq-5.16.0/examples/openwire/advanced-scenarios/jms-example-exclusive-consumer/src/main/java/example/queue/exclusive/Producer.java";

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (final TarArchiveOutputStream tos = new TarArchiveOutputStream(bos)) {
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
        try (final TarFile tarFile = new TarFile(data)) {
            final List<TarArchiveEntry> entries = tarFile.getEntries();
            assertEquals(folderName, entries.get(0).getName());
            assertEquals(consumerJavaName, entries.get(1).getName());
            assertEquals(producerJavaName, entries.get(2).getName());
        }
    }

    @Test
    public void testDirectoryWithLongNameEndsWithSlash() throws IOException {
        final String rootPath = dir.getAbsolutePath();
        final String dirDirectory = "COMPRESS-509";
        final int count = 100;
        final File root = new File(rootPath + "/" + dirDirectory);
        root.mkdirs();
        for (int i = 1; i < count; i++) {
            // -----------------------
            // create empty dirs with incremental length
            // -----------------------
            final StringBuilder subDirBuilder = new StringBuilder();
            for (int j = 0; j < i; j++) {
                subDirBuilder.append("a");
            }
            final String subDir = subDirBuilder.toString();
            final File dir = new File(rootPath + "/" + dirDirectory, "/" + subDir);
            dir.mkdir();

            // -----------------------
            // tar these dirs
            // -----------------------
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
            // -----------------------
            // untar these tars
            // -----------------------
            try (TarFile tarFile = new TarFile(tarF)) {
                for (final TarArchiveEntry entry : tarFile.getEntries()) {
                    assertTrue(entry.getName().endsWith("/"), "Entry name: " + entry.getName());
                }
            }
        }
    }

    @Test
    public void testParseTarTruncatedInContent() {
        assertThrows(IOException.class, () -> new TarFile(getPath("COMPRESS-544_truncated_in_content.tar")));
    }

    @Test
    public void testParseTarTruncatedInPadding() {
        assertThrows(IOException.class, () -> new TarFile(getPath("COMPRESS-544_truncated_in_padding.tar")));
    }

    @Test
    public void testParseTarWithNonNumberPaxHeaders() {
        assertThrows(IOException.class, () -> new TarFile(getPath("COMPRESS-529.tar")));
    }

    @Test
    public void testParseTarWithSpecialPaxHeaders() {
        assertThrows(IOException.class, () -> new TarFile(getPath("COMPRESS-530.tar")));
    }

    @Test
    public void testThrowException() {
        assertThrows(IOException.class, () -> new TarFile(getPath("COMPRESS-553.tar")));
    }

    @Test
    public void testThrowExceptionWithNullEntry() {
        assertThrows(IOException.class, () -> new TarFile(getPath("COMPRESS-554.tar")));
    }

    @Test
    public void workaroundForBrokenTimeHeader() throws IOException {
        try (final TarFile tarFile = new TarFile(getPath("simple-aix-native-tar.tar"))) {
            final List<TarArchiveEntry> entries = tarFile.getEntries();
            assertEquals(3, entries.size());
            final TarArchiveEntry entry = entries.get(1);
            assertEquals("sample/link-to-txt-file.lnk", entry.getName());
            assertEquals(new Date(0), entry.getLastModifiedDate());
            assertTrue(entry.isSymbolicLink());
            assertTrue(entry.isCheckSumOK());
        }
    }
}
