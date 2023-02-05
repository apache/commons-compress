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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.utils.CharsetNames;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.Test;

public class TarArchiveInputStreamTest extends AbstractTestCase {

    private void datePriorToEpoch(final String archive) throws Exception {
        try (TarArchiveInputStream in = new TarArchiveInputStream(Files.newInputStream(getFile(archive).toPath()))) {
            final TarArchiveEntry tae = in.getNextTarEntry();
            assertEquals("foo", tae.getName());
            final Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            cal.set(1969, 11, 31, 23, 59, 59);
            cal.set(Calendar.MILLISECOND, 0);
            assertEquals(cal.getTime(), tae.getLastModifiedDate());
            assertTrue(tae.isCheckSumOK());
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

    private void getNextEntryUntilIOException(final TarArchiveInputStream archive) {
        assertThrows(IOException.class, () -> {
            while (archive.getNextTarEntry() != null) {
                // noop
            }
        });
    }

    private TarArchiveInputStream getTestStream(final String name) {
        return new TarArchiveInputStream(
                TarArchiveInputStreamTest.class.getResourceAsStream(name));
    }

    @Test
    public void multiByteReadConsistentlyReturnsMinusOneAtEof() throws Exception {
        final byte[] buf = new byte[2];
        try (InputStream in = newInputStream("bla.tar");
             TarArchiveInputStream archive = new TarArchiveInputStream(in)) {
            final ArchiveEntry e = archive.getNextEntry();
            IOUtils.toByteArray(archive);
            assertEquals(-1, archive.read(buf));
            assertEquals(-1, archive.read(buf));
        }
    }

    @Test
    public void readsArchiveCompletely_COMPRESS245() {
        try (InputStream is = TarArchiveInputStreamTest.class.getResourceAsStream("/COMPRESS-245.tar.gz")) {
            final InputStream gin = new GZIPInputStream(is);
            try (TarArchiveInputStream tar = new TarArchiveInputStream(gin)) {
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
    public void rejectsArchivesWithNegativeSizes() throws Exception {
        try (InputStream in = newInputStream("COMPRESS-569.tar"); TarArchiveInputStream archive = new TarArchiveInputStream(in)) {
            getNextEntryUntilIOException(archive);
        }
    }

    /**
     * This test ensures the implementation is reading the padded last block if a tool has added one to an archive
     */
    @Test
    public void shouldConsumeArchiveCompletely() throws Exception {
        try (InputStream is = TarArchiveInputStreamTest.class.getResourceAsStream("/archive_with_trailer.tar");
             TarArchiveInputStream tar = new TarArchiveInputStream(is)) {
            while (tar.getNextTarEntry() != null) {
                // just consume the archive
            }
            final byte[] expected = { 'H', 'e', 'l', 'l', 'o', ',', ' ', 'w', 'o', 'r', 'l', 'd', '!', '\n' };
            final byte[] actual = new byte[expected.length];
            is.read(actual);
            assertArrayEquals(expected, actual);
        }
    }

    @Test
    public void shouldReadBigGid() throws Exception {
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
        final ByteArrayInputStream bis = new ByteArrayInputStream(data);
        try (final TarArchiveInputStream tis = new TarArchiveInputStream(bis)) {
            final TarArchiveEntry t = tis.getNextTarEntry();
            assertEquals(4294967294L, t.getLongGroupId());
        }
    }

    /**
     * @link "https://issues.apache.org/jira/browse/COMPRESS-324"
     */
    @Test
    public void shouldReadGNULongNameEntryWithWrongName() throws Exception {
        try (TarArchiveInputStream is = getTestStream("/COMPRESS-324.tar")) {
            final TarArchiveEntry entry = is.getNextTarEntry();
            assertEquals("1234567890123456789012345678901234567890123456789012345678901234567890"
                            + "1234567890123456789012345678901234567890123456789012345678901234567890"
                            + "1234567890123456789012345678901234567890123456789012345678901234567890"
                            + "1234567890123456789012345678901234567890.txt",
                    entry.getName());
        }
    }

    @Test
    public void shouldThrowAnExceptionOnTruncatedEntries() throws Exception {
        final File dir = mkdir("COMPRESS-279");
        try (TarArchiveInputStream is = getTestStream("/COMPRESS-279.tar")) {
            assertThrows(IOException.class, () -> {
                TarArchiveEntry entry = is.getNextTarEntry();
                int count = 0;
                while (entry != null) {
                    Files.copy(is, new File(dir, String.valueOf(count)).toPath());
                    count++;
                    entry = is.getNextTarEntry();
                }
            });
        } finally {
            rmdir(dir);
        }
    }

    @Test
    public void shouldUseSpecifiedEncodingWhenReadingGNULongNames()
        throws Exception {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final String encoding = CharsetNames.UTF_16;
        final String name = "1234567890123456789012345678901234567890123456789"
            + "01234567890123456789012345678901234567890123456789"
            + "01234567890\u00e4";
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
        try (TarArchiveInputStream tis = new TarArchiveInputStream(bis, encoding)) {
            final TarArchiveEntry t = tis.getNextTarEntry();
            assertEquals(name, t.getName());
        }
    }
    @Test
    public void singleByteReadConsistentlyReturnsMinusOneAtEof() throws Exception {
        try (InputStream in = newInputStream("bla.tar");
             TarArchiveInputStream archive = new TarArchiveInputStream(in)) {
            final ArchiveEntry e = archive.getNextEntry();
            IOUtils.toByteArray(archive);
            assertEquals(-1, archive.read());
            assertEquals(-1, archive.read());
        }
    }

    /**
     * @link "https://issues.apache.org/jira/browse/COMPRESS-417"
     */
    @Test
    public void skipsDevNumbersWhenEntryIsNoDevice() throws Exception {
        try (TarArchiveInputStream is = getTestStream("/COMPRESS-417.tar")) {
            assertEquals("test1.xml", is.getNextTarEntry().getName());
            assertEquals("test2.xml", is.getNextTarEntry().getName());
            assertNull(is.getNextTarEntry());
        }
    }

    /**
     * @link "https://issues.apache.org/jira/browse/COMPRESS-355"
     */
    @Test
    public void survivesBlankLinesInPaxHeader() throws Exception {
        try (TarArchiveInputStream is = getTestStream("/COMPRESS-355.tar")) {
            final TarArchiveEntry entry = is.getNextTarEntry();
            assertEquals("package/package.json", entry.getName());
            assertNull(is.getNextTarEntry());
        }
    }

    /**
     * @link "https://issues.apache.org/jira/browse/COMPRESS-356"
     */
    @Test
    public void survivesPaxHeaderWithNameEndingInSlash() throws Exception {
        try (TarArchiveInputStream is = getTestStream("/COMPRESS-356.tar")) {
            final TarArchiveEntry entry = is.getNextTarEntry();
            assertEquals("package/package.json", entry.getName());
            assertNull(is.getNextTarEntry());
        }
    }

    @Test
    public void testCompress197() {
        try (TarArchiveInputStream tar = getTestStream("/COMPRESS-197.tar")) {
            TarArchiveEntry entry = tar.getNextTarEntry();
            while (entry != null) {
                entry = tar.getNextTarEntry();
            }
        } catch (final IOException e) {
            fail("COMPRESS-197: " + e.getMessage());
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
        try (final ByteArrayInputStream bis = new ByteArrayInputStream(data);
             final TarArchiveInputStream tis = new TarArchiveInputStream(bis)) {
            assertEquals(folderName, tis.getNextTarEntry().getName());
            assertEquals(consumerJavaName, tis.getNextTarEntry().getName());
            assertEquals(producerJavaName, tis.getNextTarEntry().getName());
        }
    }

    @Test
    public void testDirectoryWithLongNameEndsWithSlash() throws IOException, ArchiveException {
        final String rootPath = dir.getAbsolutePath();
        final String dirDirectory = "COMPRESS-509";
        final int count = 100;
        final File root = new File(rootPath + "/" + dirDirectory);
        root.mkdirs();
        for (int i = 1; i < count; i++) {
            // -----------------------
            // create empty dirs with incremental length
            // -----------------------
            String subDir = "";
            for (int j = 0; j < i; j++) {
                subDir += "a";
            }
            final File dir = new File(rootPath + "/" + dirDirectory, "/" + subDir);
            dir.mkdir();

            // -----------------------
            // tar these dirs
            // -----------------------
            final String fileName = "/" + dirDirectory + "/" + subDir;
            final File tarF = new File(rootPath + "/tar" + i + ".tar");
            try (final OutputStream dest = Files.newOutputStream(tarF.toPath())) {
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

            // -----------------------
            // untar these tars
            // -----------------------
            try (final InputStream is = Files.newInputStream(tarF.toPath());
                final TarArchiveInputStream debInputStream = (TarArchiveInputStream) ArchiveStreamFactory.DEFAULT.createArchiveInputStream("tar", is)) {
                TarArchiveEntry outEntry;
                while ((outEntry = (TarArchiveEntry) debInputStream.getNextEntry()) != null) {
                    assertTrue(outEntry.getName().endsWith("/"), outEntry.getName());
                }
            }
        }
    }

    @Test
    public void testGetAndSetOfPaxEntry() throws Exception {
        try (TarArchiveInputStream is = getTestStream("/COMPRESS-356.tar")) {
            final TarArchiveEntry entry = is.getNextTarEntry();
            assertEquals("package/package.json", entry.getName());
            assertEquals(is.getCurrentEntry(),entry);
            final TarArchiveEntry weaselEntry = new TarArchiveEntry(entry.getName());
            weaselEntry.setSize(entry.getSize());
            is.setCurrentEntry(weaselEntry);
            assertEquals(entry,is.getCurrentEntry());
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

    @Test
    public void testParseTarTruncatedInContent() throws IOException {
        try (InputStream in = newInputStream("COMPRESS-544_truncated_in_content.tar");
            TarArchiveInputStream archive = new TarArchiveInputStream(in)) {
            getNextEntryUntilIOException(archive);
        }
    }

    @Test
    public void testParseTarTruncatedInPadding() throws IOException {
        try (InputStream in = newInputStream("COMPRESS-544_truncated_in_padding.tar");
            TarArchiveInputStream archive = new TarArchiveInputStream(in)) {
            getNextEntryUntilIOException(archive);
        }
    }

    @Test
    public void testParseTarWithNonNumberPaxHeaders() throws IOException {
        try (InputStream in = newInputStream("COMPRESS-529.tar"); TarArchiveInputStream archive = new TarArchiveInputStream(in)) {
            assertThrows(IOException.class, () -> archive.getNextEntry());
        }
    }

    @Test
    public void testParseTarWithSpecialPaxHeaders() throws IOException {
        try (InputStream in = newInputStream("COMPRESS-530.tar");
             TarArchiveInputStream archive = new TarArchiveInputStream(in)) {
            assertThrows(IOException.class, () -> archive.getNextEntry());
            assertThrows(IOException.class, () -> IOUtils.toByteArray(archive));
        }
    }

    @Test
    public void testThrowException() throws IOException {
        try (InputStream in = newInputStream("COMPRESS-553.tar");
             TarArchiveInputStream archive = new TarArchiveInputStream(in)) {
            getNextEntryUntilIOException(archive);
        }
    }

    @Test
    public void testThrowExceptionWithNullEntry() throws IOException {
        try (InputStream in = newInputStream("COMPRESS-554.tar");
             TarArchiveInputStream archive = new TarArchiveInputStream(in)) {
            getNextEntryUntilIOException(archive);
        }
    }

    @Test
    public void workaroundForBrokenTimeHeader() throws Exception {
        try (TarArchiveInputStream in = new TarArchiveInputStream(newInputStream("simple-aix-native-tar.tar"))) {
            TarArchiveEntry tae = in.getNextTarEntry();
            tae = in.getNextTarEntry();
            assertEquals("sample/link-to-txt-file.lnk", tae.getName());
            assertEquals(new Date(0), tae.getLastModifiedDate());
            assertTrue(tae.isSymbolicLink());
            assertTrue(tae.isCheckSumOK());
        }
    }

}
