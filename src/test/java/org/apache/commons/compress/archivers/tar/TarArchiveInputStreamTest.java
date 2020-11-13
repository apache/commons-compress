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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.utils.CharsetNames;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.Test;

public class TarArchiveInputStreamTest extends AbstractTestCase {

    @Test
    public void readSimplePaxHeader() throws Exception {
        final InputStream is = new ByteArrayInputStream(new byte[1]);
        final TarArchiveInputStream tais = new TarArchiveInputStream(is);
        final Map<String, String> headers = tais
            .parsePaxHeaders(new ByteArrayInputStream("30 atime=1321711775.972059463\n"
                                                      .getBytes(StandardCharsets.UTF_8)), null);
        assertEquals(1, headers.size());
        assertEquals("1321711775.972059463", headers.get("atime"));
        tais.close();
    }

    @Test
    public void secondEntryWinsWhenPaxHeaderContainsDuplicateKey() throws Exception {
        final InputStream is = new ByteArrayInputStream(new byte[1]);
        final TarArchiveInputStream tais = new TarArchiveInputStream(is);
        final Map<String, String> headers = tais
            .parsePaxHeaders(new ByteArrayInputStream("11 foo=bar\n11 foo=baz\n"
                                                      .getBytes(StandardCharsets.UTF_8)), null);
        assertEquals(1, headers.size());
        assertEquals("baz", headers.get("foo"));
        tais.close();
    }

    @Test
    public void paxHeaderEntryWithEmptyValueRemovesKey() throws Exception {
        final InputStream is = new ByteArrayInputStream(new byte[1]);
        final TarArchiveInputStream tais = new TarArchiveInputStream(is);
        final Map<String, String> headers = tais
            .parsePaxHeaders(new ByteArrayInputStream("11 foo=bar\n7 foo=\n"
                                                      .getBytes(StandardCharsets.UTF_8)), null);
        assertEquals(0, headers.size());
        tais.close();
    }

    @Test
    public void readPaxHeaderWithEmbeddedNewline() throws Exception {
        final InputStream is = new ByteArrayInputStream(new byte[1]);
        final TarArchiveInputStream tais = new TarArchiveInputStream(is);
        final Map<String, String> headers = tais
            .parsePaxHeaders(new ByteArrayInputStream("28 comment=line1\nline2\nand3\n"
                                                      .getBytes(StandardCharsets.UTF_8)), null);
        assertEquals(1, headers.size());
        assertEquals("line1\nline2\nand3", headers.get("comment"));
        tais.close();
    }

    @Test
    public void readNonAsciiPaxHeader() throws Exception {
        final String ae = "\u00e4";
        final String line = "11 path="+ ae + "\n";
        assertEquals(11, line.getBytes(StandardCharsets.UTF_8).length);
        final InputStream is = new ByteArrayInputStream(new byte[1]);
        final TarArchiveInputStream tais = new TarArchiveInputStream(is);
        final Map<String, String> headers = tais
            .parsePaxHeaders(new ByteArrayInputStream(line.getBytes(StandardCharsets.UTF_8)), null);
        assertEquals(1, headers.size());
        assertEquals(ae, headers.get("path"));
        tais.close();
    }

    @Test
    public void workaroundForBrokenTimeHeader() throws Exception {
        TarArchiveInputStream in = null;
        try {
            in = new TarArchiveInputStream(new FileInputStream(getFile("simple-aix-native-tar.tar")));
            TarArchiveEntry tae = in.getNextTarEntry();
            tae = in.getNextTarEntry();
            assertEquals("sample/link-to-txt-file.lnk", tae.getName());
            assertEquals(new Date(0), tae.getLastModifiedDate());
            assertTrue(tae.isSymbolicLink());
            assertTrue(tae.isCheckSumOK());
        } finally {
            if (in != null) {
                in.close();
            }
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

    private void datePriorToEpoch(final String archive) throws Exception {
        TarArchiveInputStream in = null;
        try {
            in = new TarArchiveInputStream(new FileInputStream(getFile(archive)));
            final TarArchiveEntry tae = in.getNextTarEntry();
            assertEquals("foo", tae.getName());
            final Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            cal.set(1969, 11, 31, 23, 59, 59);
            cal.set(Calendar.MILLISECOND, 0);
            assertEquals(cal.getTime(), tae.getLastModifiedDate());
            assertTrue(tae.isCheckSumOK());
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    @Test
    public void testCompress197() throws Exception {
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
    public void shouldUseSpecifiedEncodingWhenReadingGNULongNames()
        throws Exception {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final String encoding = CharsetNames.UTF_16;
        final String name = "1234567890123456789012345678901234567890123456789"
            + "01234567890123456789012345678901234567890123456789"
            + "01234567890\u00e4";
        final TarArchiveOutputStream tos =
            new TarArchiveOutputStream(bos, encoding);
        tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
        TarArchiveEntry t = new TarArchiveEntry(name);
        t.setSize(1);
        tos.putArchiveEntry(t);
        tos.write(30);
        tos.closeArchiveEntry();
        tos.close();
        final byte[] data = bos.toByteArray();
        final ByteArrayInputStream bis = new ByteArrayInputStream(data);
        final TarArchiveInputStream tis =
            new TarArchiveInputStream(bis, encoding);
        t = tis.getNextTarEntry();
        assertEquals(name, t.getName());
        tis.close();
    }

    @Test
    public void shouldConsumeArchiveCompletely() throws Exception {
        final InputStream is = TarArchiveInputStreamTest.class
            .getResourceAsStream("/archive_with_trailer.tar");
        final TarArchiveInputStream tar = new TarArchiveInputStream(is);
        while (tar.getNextTarEntry() != null) {
            // just consume the archive
        }
        final byte[] expected = new byte[] {
            'H', 'e', 'l', 'l', 'o', ',', ' ', 'w', 'o', 'r', 'l', 'd', '!', '\n'
        };
        final byte[] actual = new byte[expected.length];
        is.read(actual);
        assertArrayEquals(expected, actual);
        tar.close();
    }

    @Test
    public void readsArchiveCompletely_COMPRESS245() throws Exception {
        try (InputStream is = TarArchiveInputStreamTest.class
                .getResourceAsStream("/COMPRESS-245.tar.gz")) {
            final InputStream gin = new GZIPInputStream(is);
            final TarArchiveInputStream tar = new TarArchiveInputStream(gin);
            int count = 0;
            TarArchiveEntry entry = tar.getNextTarEntry();
            while (entry != null) {
                count++;
                entry = tar.getNextTarEntry();
            }
            assertEquals(31, count);
            tar.close();
        } catch (final IOException e) {
            fail("COMPRESS-245: " + e.getMessage());
        }
    }

    @Test(expected = IOException.class)
    public void shouldThrowAnExceptionOnTruncatedEntries() throws Exception {
        final File dir = mkdir("COMPRESS-279");
        final TarArchiveInputStream is = getTestStream("/COMPRESS-279.tar");
        FileOutputStream out = null;
        try {
            TarArchiveEntry entry = is.getNextTarEntry();
            int count = 0;
            while (entry != null) {
                out = new FileOutputStream(new File(dir, String.valueOf(count)));
                IOUtils.copy(is, out);
                out.close();
                out = null;
                count++;
                entry = is.getNextTarEntry();
            }
        } finally {
            is.close();
            if (out != null) {
                out.close();
            }
            rmdir(dir);
        }
    }

    @Test
    public void shouldReadBigGid() throws Exception {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final TarArchiveOutputStream tos = new TarArchiveOutputStream(bos);
        tos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
        TarArchiveEntry t = new TarArchiveEntry("name");
        t.setGroupId(4294967294L);
        t.setSize(1);
        tos.putArchiveEntry(t);
        tos.write(30);
        tos.closeArchiveEntry();
        tos.close();
        final byte[] data = bos.toByteArray();
        final ByteArrayInputStream bis = new ByteArrayInputStream(data);
        final TarArchiveInputStream tis =
            new TarArchiveInputStream(bis);
        t = tis.getNextTarEntry();
        assertEquals(4294967294L, t.getLongGroupId());
        tis.close();
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
    public void testGetAndSetOfPaxEntry() throws Exception {
        try (TarArchiveInputStream is = getTestStream("/COMPRESS-356.tar")) {
            final TarArchiveEntry entry = is.getNextTarEntry();
            assertEquals("package/package.json", entry.getName());
            assertEquals(is.getCurrentEntry(),entry);
            final TarArchiveEntry weaselEntry = new TarArchiveEntry(entry.getName());
            weaselEntry.setSize(entry.getSize());
            is.setCurrentEntry(weaselEntry);
            assertEquals(entry,is.getCurrentEntry());
            assertFalse(entry == is.getCurrentEntry());
            assertTrue(weaselEntry == is.getCurrentEntry());
            try {
               is.setCurrentEntry(null);
               is.read();
               fail("should abort because current entry is nulled");
            }  catch(final IllegalStateException e) {
                // expected
            }
            is.setCurrentEntry(entry);
            is.read();
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

    @Test
    public void singleByteReadConsistentlyReturnsMinusOneAtEof() throws Exception {
        try (FileInputStream in = new FileInputStream(getFile("bla.tar"));
             TarArchiveInputStream archive = new TarArchiveInputStream(in)) {
            final ArchiveEntry e = archive.getNextEntry();
            IOUtils.toByteArray(archive);
            assertEquals(-1, archive.read());
            assertEquals(-1, archive.read());
        }
    }

    @Test
    public void multiByteReadConsistentlyReturnsMinusOneAtEof() throws Exception {
        final byte[] buf = new byte[2];
        try (FileInputStream in = new FileInputStream(getFile("bla.tar"));
             TarArchiveInputStream archive = new TarArchiveInputStream(in)) {
            final ArchiveEntry e = archive.getNextEntry();
            IOUtils.toByteArray(archive);
            assertEquals(-1, archive.read(buf));
            assertEquals(-1, archive.read(buf));
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
            final FileOutputStream dest = new FileOutputStream(tarF);
            final TarArchiveOutputStream out = new TarArchiveOutputStream(new BufferedOutputStream(dest));
            out.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR);
            out.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

            final File file = new File(rootPath, fileName);
            final TarArchiveEntry entry = new TarArchiveEntry(file);
            entry.setName(fileName);
            out.putArchiveEntry(entry);
            out.closeArchiveEntry();
            out.flush();
            out.close();

            // -----------------------
            // untar these tars
            // -----------------------
            final InputStream is = new FileInputStream(tarF);
            final TarArchiveInputStream debInputStream = (TarArchiveInputStream) ArchiveStreamFactory.DEFAULT
                    .createArchiveInputStream("tar", is);
            TarArchiveEntry outEntry;
            while ((outEntry = (TarArchiveEntry) debInputStream.getNextEntry()) != null) {
                assertTrue(outEntry.getName().endsWith("/"));
            }
            debInputStream.close();
        }
    }

    @Test(expected = IOException.class)
    public void testParseTarWithSpecialPaxHeaders() throws IOException {
        try (FileInputStream in = new FileInputStream(getFile("COMPRESS-530.tar"));
             TarArchiveInputStream archive = new TarArchiveInputStream(in)) {
            archive.getNextEntry();
            IOUtils.toByteArray(archive);
        }
    }

    @Test(expected = IOException.class)
    public void testParseTarWithNonNumberPaxHeaders() throws IOException {
        try (FileInputStream in = new FileInputStream(getFile("COMPRESS-529.tar"));
             TarArchiveInputStream archive = new TarArchiveInputStream(in)) {
            archive.getNextEntry();
        }
    }

    @Test(expected = IOException.class)
    public void testParseTarTruncatedInPadding() throws IOException {
        try (FileInputStream in = new FileInputStream(getFile("COMPRESS-544_truncated_in_padding.tar"));
             TarArchiveInputStream archive = new TarArchiveInputStream(in)) {
            while (archive.getNextTarEntry() != null) {
            }
        }
    }

    @Test(expected = IOException.class)
    public void testParseTarTruncatedInContent() throws IOException {
        try (FileInputStream in = new FileInputStream(getFile("COMPRESS-544_truncated_in_content.tar"));
             TarArchiveInputStream archive = new TarArchiveInputStream(in)) {
            while (archive.getNextTarEntry() != null) {
            }
        }
    }

    @Test(expected = IOException.class)
    public void testThrowExceptionWithNullEntry() throws IOException {
        try (FileInputStream in = new FileInputStream(getFile("COMPRESS-554.tar"));
             TarArchiveInputStream archive = new TarArchiveInputStream(in)) {
            while (archive.getNextTarEntry() != null) {
            }
        }
    }

    @Test(expected = IOException.class)
    public void testThrowException() throws IOException {
        try (FileInputStream in = new FileInputStream(getFile("COMPRESS-553.tar"));
             TarArchiveInputStream archive = new TarArchiveInputStream(in)) {
            while (archive.getNextTarEntry() != null) {
            }
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
            TarArchiveEntry rootfolder = new TarArchiveEntry(folderName);
            tos.putArchiveEntry(rootfolder);
            TarArchiveEntry consumerJava = new TarArchiveEntry(consumerJavaName);
            tos.putArchiveEntry(consumerJava);
            TarArchiveEntry producerJava = new TarArchiveEntry(producerJavaName);
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

    private TarArchiveInputStream getTestStream(final String name) {
        return new TarArchiveInputStream(
                TarArchiveInputStreamTest.class.getResourceAsStream(name));
    }

}
