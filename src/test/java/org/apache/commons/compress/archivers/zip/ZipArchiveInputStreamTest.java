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

package org.apache.commons.compress.archivers.zip;

import static org.apache.commons.compress.AbstractTestCase.getFile;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.zip.ZipException;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.Assert;
import org.junit.Test;

public class ZipArchiveInputStreamTest {

    /**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-176"
     */
    @Test
    public void winzipBackSlashWorkaround() throws Exception {
        ZipArchiveInputStream in = null;
        try {
            in = new ZipArchiveInputStream(new FileInputStream(getFile("test-winzip.zip")));
            ZipArchiveEntry zae = in.getNextZipEntry();
            zae = in.getNextZipEntry();
            zae = in.getNextZipEntry();
            assertEquals("\u00e4/", zae.getName());
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    /**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-189"
     */
    @Test
    public void properUseOfInflater() throws Exception {
        ZipFile zf = null;
        ZipArchiveInputStream in = null;
        try {
            zf = new ZipFile(getFile("COMPRESS-189.zip"));
            final ZipArchiveEntry zae = zf.getEntry("USD0558682-20080101.ZIP");
            in = new ZipArchiveInputStream(new BufferedInputStream(zf.getInputStream(zae)));
            ZipArchiveEntry innerEntry;
            while ((innerEntry = in.getNextZipEntry()) != null) {
                if (innerEntry.getName().endsWith("XML")) {
                    assertTrue(0 < in.read());
                }
            }
        } finally {
            if (zf != null) {
                zf.close();
            }
            if (in != null) {
                in.close();
            }
        }
    }

    @Test
    public void shouldConsumeArchiveCompletely() throws Exception {
        final InputStream is = ZipArchiveInputStreamTest.class
            .getResourceAsStream("/archive_with_trailer.zip");
        final ZipArchiveInputStream zip = new ZipArchiveInputStream(is);
        while (zip.getNextZipEntry() != null) {
            // just consume the archive
        }
        final byte[] expected = new byte[] {
            'H', 'e', 'l', 'l', 'o', ',', ' ', 'w', 'o', 'r', 'l', 'd', '!', '\n'
        };
        final byte[] actual = new byte[expected.length];
        is.read(actual);
        assertArrayEquals(expected, actual);
        zip.close();
    }

    /**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-219"
     */
    @Test
    public void shouldReadNestedZip() throws IOException {
        ZipArchiveInputStream in = null;
        try {
            in = new ZipArchiveInputStream(new FileInputStream(getFile("COMPRESS-219.zip")));
            extractZipInputStream(in);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    private void extractZipInputStream(final ZipArchiveInputStream in)
        throws IOException {
        ZipArchiveEntry zae = in.getNextZipEntry();
        while (zae != null) {
            if (zae.getName().endsWith(".zip")) {
                extractZipInputStream(new ZipArchiveInputStream(in));
            }
            zae = in.getNextZipEntry();
        }
    }

    @Test
    public void testUnshrinkEntry() throws Exception {
        final ZipArchiveInputStream in = new ZipArchiveInputStream(new FileInputStream(getFile("SHRUNK.ZIP")));

        ZipArchiveEntry entry = in.getNextZipEntry();
        assertEquals("method", ZipMethod.UNSHRINKING.getCode(), entry.getMethod());
        assertTrue(in.canReadEntryData(entry));

        FileInputStream original = new FileInputStream(getFile("test1.xml"));
        try {
            assertArrayEquals(IOUtils.toByteArray(original), IOUtils.toByteArray(in));
        } finally {
            original.close();
        }

        entry = in.getNextZipEntry();
        assertEquals("method", ZipMethod.UNSHRINKING.getCode(), entry.getMethod());
        assertTrue(in.canReadEntryData(entry));

        original = new FileInputStream(getFile("test2.xml"));
        try {
            assertArrayEquals(IOUtils.toByteArray(original), IOUtils.toByteArray(in));
        } finally {
            original.close();
        }
    }


    /**
     * Test case for
     * <a href="https://issues.apache.org/jira/browse/COMPRESS-264"
     * >COMPRESS-264</a>.
     */
    @Test
    public void testReadingOfFirstStoredEntry() throws Exception {

        try (ZipArchiveInputStream in = new ZipArchiveInputStream(new FileInputStream(getFile("COMPRESS-264.zip")))) {
            final ZipArchiveEntry ze = in.getNextZipEntry();
            assertEquals(5, ze.getSize());
            assertArrayEquals(new byte[] { 'd', 'a', 't', 'a', '\n' },
                    IOUtils.toByteArray(in));
        }
    }

    /**
     * Test case for
     * <a href="https://issues.apache.org/jira/browse/COMPRESS-351"
     * >COMPRESS-351</a>.
     */
    @Test
    public void testMessageWithCorruptFileName() throws Exception {
        try (ZipArchiveInputStream in = new ZipArchiveInputStream(new FileInputStream(getFile("COMPRESS-351.zip")))) {
            ZipArchiveEntry ze = in.getNextZipEntry();
            while (ze != null) {
                ze = in.getNextZipEntry();
            }
            fail("expected EOFException");
        } catch (final EOFException ex) {
            final String m = ex.getMessage();
            assertTrue(m.startsWith("Truncated ZIP entry: ?2016")); // the first character is not printable
        }
    }

    @Test
    public void testUnzipBZip2CompressedEntry() throws Exception {

        try (ZipArchiveInputStream in = new ZipArchiveInputStream(new FileInputStream(getFile("bzip2-zip.zip")))) {
            final ZipArchiveEntry ze = in.getNextZipEntry();
            assertEquals(42, ze.getSize());
            final byte[] expected = new byte[42];
            Arrays.fill(expected, (byte) 'a');
            assertArrayEquals(expected, IOUtils.toByteArray(in));
        }
    }

    /**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-380"
     */
    @Test
    public void readDeflate64CompressedStream() throws Exception {
        final File input = getFile("COMPRESS-380/COMPRESS-380-input");
        final File archive = getFile("COMPRESS-380/COMPRESS-380.zip");
        try (FileInputStream in = new FileInputStream(input);
             ZipArchiveInputStream zin = new ZipArchiveInputStream(new FileInputStream(archive))) {
            byte[] orig = IOUtils.toByteArray(in);
            ZipArchiveEntry e = zin.getNextZipEntry();
            byte[] fromZip = IOUtils.toByteArray(zin);
            assertArrayEquals(orig, fromZip);
        }
    }

    @Test
    public void readDeflate64CompressedStreamWithDataDescriptor() throws Exception {
        // this is a copy of bla.jar with META-INF/MANIFEST.MF's method manually changed to ENHANCED_DEFLATED
        final File archive = getFile("COMPRESS-380/COMPRESS-380-dd.zip");
        try (ZipArchiveInputStream zin = new ZipArchiveInputStream(new FileInputStream(archive))) {
            ZipArchiveEntry e = zin.getNextZipEntry();
            assertEquals(-1, e.getSize());
            assertEquals(ZipMethod.ENHANCED_DEFLATED.getCode(), e.getMethod());
            byte[] fromZip = IOUtils.toByteArray(zin);
            byte[] expected = new byte[] {
                'M', 'a', 'n', 'i', 'f', 'e', 's', 't', '-', 'V', 'e', 'r', 's', 'i', 'o', 'n', ':', ' ', '1', '.', '0',
                '\r', '\n', '\r', '\n'
            };
            assertArrayEquals(expected, fromZip);
            zin.getNextZipEntry();
            assertEquals(25, e.getSize());
        }
    }

    /**
     * Test case for
     * <a href="https://issues.apache.org/jira/browse/COMPRESS-364"
     * >COMPRESS-364</a>.
     */
    @Test
    public void testWithBytesAfterData() throws Exception {
        final int expectedNumEntries = 2;
        final InputStream is = ZipArchiveInputStreamTest.class
                .getResourceAsStream("/archive_with_bytes_after_data.zip");
        final ZipArchiveInputStream zip = new ZipArchiveInputStream(is);

        try {
            int actualNumEntries = 0;
            ZipArchiveEntry zae = zip.getNextZipEntry();
            while (zae != null) {
                actualNumEntries++;
                readEntry(zip, zae);
                zae = zip.getNextZipEntry();
            }
            assertEquals(expectedNumEntries, actualNumEntries);
        } finally {
            zip.close();
        }
    }

    /**
     * <code>getNextZipEntry()</code> should throw a <code>ZipException</code> rather than return
     * <code>null</code> when an unexpected structure is encountered.
     */
    @Test
    public void testThrowOnInvalidEntry() throws Exception {
        final InputStream is = ZipArchiveInputStreamTest.class
                .getResourceAsStream("/invalid-zip.zip");
        final ZipArchiveInputStream zip = new ZipArchiveInputStream(is);

        try {
            zip.getNextZipEntry();
            fail("IOException expected");
        } catch (ZipException expected) {
            assertTrue(expected.getMessage().contains("Unexpected record signature"));
        } finally {
            zip.close();
        }
    }

    /**
     * Test correct population of header and data offsets.
     */
    @Test
    public void testOffsets() throws Exception {
        // mixed.zip contains both inflated and stored files
        try (InputStream archiveStream = ZipArchiveInputStream.class.getResourceAsStream("/mixed.zip");
             ZipArchiveInputStream zipStream =  new ZipArchiveInputStream((archiveStream))
        ) {
            ZipArchiveEntry inflatedEntry = zipStream.getNextZipEntry();
            Assert.assertEquals("inflated.txt", inflatedEntry.getName());
            Assert.assertEquals(0x0000, inflatedEntry.getLocalHeaderOffset());
            Assert.assertEquals(0x0046, inflatedEntry.getDataOffset());
            ZipArchiveEntry storedEntry = zipStream.getNextZipEntry();
            Assert.assertEquals("stored.txt", storedEntry.getName());
            Assert.assertEquals(0x5892, storedEntry.getLocalHeaderOffset());
            Assert.assertEquals(0x58d6, storedEntry.getDataOffset());
            Assert.assertNull(zipStream.getNextZipEntry());
        }
    }

    @Test
    public void nameSourceDefaultsToName() throws Exception {
        nameSource("bla.zip", "test1.xml", ZipArchiveEntry.NameSource.NAME);
    }

    @Test
    public void nameSourceIsSetToUnicodeExtraField() throws Exception {
        nameSource("utf8-winzip-test.zip", "\u20AC_for_Dollar.txt",
                   ZipArchiveEntry.NameSource.UNICODE_EXTRA_FIELD);
    }

    @Test
    public void nameSourceIsSetToEFS() throws Exception {
        nameSource("utf8-7zip-test.zip", "\u20AC_for_Dollar.txt", 3,
                   ZipArchiveEntry.NameSource.NAME_WITH_EFS_FLAG);
    }

    @Test
    public void properlyMarksEntriesAsUnreadableIfUncompressedSizeIsUnknown() throws Exception {
        // we never read any data
        try (ZipArchiveInputStream zis = new ZipArchiveInputStream(new ByteArrayInputStream(new byte[0]))) {
            ZipArchiveEntry e = new ZipArchiveEntry("test");
            e.setMethod(ZipMethod.DEFLATED.getCode());
            assertTrue(zis.canReadEntryData(e));
            e.setMethod(ZipMethod.ENHANCED_DEFLATED.getCode());
            assertTrue(zis.canReadEntryData(e));
            e.setMethod(ZipMethod.BZIP2.getCode());
            assertFalse(zis.canReadEntryData(e));
        }
    }

    @Test
    public void singleByteReadConsistentlyReturnsMinusOneAtEofUsingDeflate() throws Exception {
        singleByteReadConsistentlyReturnsMinusOneAtEof(getFile("bla.zip"));
    }

    @Test
    public void singleByteReadConsistentlyReturnsMinusOneAtEofUsingStore() throws Exception {
        singleByteReadConsistentlyReturnsMinusOneAtEof(getFile("COMPRESS-264.zip"));
    }

    @Test
    public void singleByteReadConsistentlyReturnsMinusOneAtEofUsingUnshrink() throws Exception {
        singleByteReadConsistentlyReturnsMinusOneAtEof(getFile("SHRUNK.ZIP"));
    }

    @Test
    public void singleByteReadConsistentlyReturnsMinusOneAtEofUsingExplode() throws Exception {
        singleByteReadConsistentlyReturnsMinusOneAtEof(getFile("imploding-8Kdict-3trees.zip"));
    }

    @Test
    public void singleByteReadConsistentlyReturnsMinusOneAtEofUsingDeflate64() throws Exception {
        singleByteReadConsistentlyReturnsMinusOneAtEof(getFile("COMPRESS-380/COMPRESS-380.zip"));
    }

    @Test
    public void singleByteReadConsistentlyReturnsMinusOneAtEofUsingBzip2() throws Exception {
        singleByteReadConsistentlyReturnsMinusOneAtEof(getFile("bzip2-zip.zip"));
    }

    private void singleByteReadConsistentlyReturnsMinusOneAtEof(File file) throws Exception {
        try (FileInputStream in = new FileInputStream(file);
             ZipArchiveInputStream archive = new ZipArchiveInputStream(in)) {
            ArchiveEntry e = archive.getNextEntry();
            IOUtils.toByteArray(archive);
            assertEquals(-1, archive.read());
            assertEquals(-1, archive.read());
        }
    }

    @Test
    public void multiByteReadConsistentlyReturnsMinusOneAtEofUsingDeflate() throws Exception {
        multiByteReadConsistentlyReturnsMinusOneAtEof(getFile("bla.zip"));
    }

    @Test
    public void multiByteReadConsistentlyReturnsMinusOneAtEofUsingStore() throws Exception {
        multiByteReadConsistentlyReturnsMinusOneAtEof(getFile("COMPRESS-264.zip"));
    }

    @Test
    public void multiByteReadConsistentlyReturnsMinusOneAtEofUsingUnshrink() throws Exception {
        multiByteReadConsistentlyReturnsMinusOneAtEof(getFile("SHRUNK.ZIP"));
    }

    @Test
    public void multiByteReadConsistentlyReturnsMinusOneAtEofUsingExplode() throws Exception {
        multiByteReadConsistentlyReturnsMinusOneAtEof(getFile("imploding-8Kdict-3trees.zip"));
    }

    @Test
    public void multiByteReadConsistentlyReturnsMinusOneAtEofUsingDeflate64() throws Exception {
        multiByteReadConsistentlyReturnsMinusOneAtEof(getFile("COMPRESS-380/COMPRESS-380.zip"));
    }

    @Test
    public void multiByteReadConsistentlyReturnsMinusOneAtEofUsingBzip2() throws Exception {
        multiByteReadConsistentlyReturnsMinusOneAtEof(getFile("bzip2-zip.zip"));
    }

    private void multiByteReadConsistentlyReturnsMinusOneAtEof(File file) throws Exception {
        byte[] buf = new byte[2];
        try (FileInputStream in = new FileInputStream(getFile("bla.zip"));
             ZipArchiveInputStream archive = new ZipArchiveInputStream(in)) {
            ArchiveEntry e = archive.getNextEntry();
            IOUtils.toByteArray(archive);
            assertEquals(-1, archive.read(buf));
            assertEquals(-1, archive.read(buf));
        }
    }

    @Test
    public void singleByteReadThrowsAtEofForCorruptedStoredEntry() throws Exception {
        byte[] content;
        try (FileInputStream fs = new FileInputStream(getFile("COMPRESS-264.zip"))) {
            content = IOUtils.toByteArray(fs);
        }
        // make size much bigger than entry's real size
        for (int i = 17; i < 26; i++) {
            content[i] = (byte) 0xff;
        }
        try (ByteArrayInputStream in = new ByteArrayInputStream(content);
             ZipArchiveInputStream archive = new ZipArchiveInputStream(in)) {
            ArchiveEntry e = archive.getNextEntry();
            try {
                IOUtils.toByteArray(archive);
                fail("expected exception");
            } catch (IOException ex) {
                assertEquals("Truncated ZIP file", ex.getMessage());
            }
            try {
                archive.read();
                fail("expected exception");
            } catch (IOException ex) {
                assertEquals("Truncated ZIP file", ex.getMessage());
            }
            try {
                archive.read();
                fail("expected exception");
            } catch (IOException ex) {
                assertEquals("Truncated ZIP file", ex.getMessage());
            }
        }
    }

    @Test
    public void multiByteReadThrowsAtEofForCorruptedStoredEntry() throws Exception {
        byte[] content;
        try (FileInputStream fs = new FileInputStream(getFile("COMPRESS-264.zip"))) {
            content = IOUtils.toByteArray(fs);
        }
        // make size much bigger than entry's real size
        for (int i = 17; i < 26; i++) {
            content[i] = (byte) 0xff;
        }
        byte[] buf = new byte[2];
        try (ByteArrayInputStream in = new ByteArrayInputStream(content);
             ZipArchiveInputStream archive = new ZipArchiveInputStream(in)) {
            ArchiveEntry e = archive.getNextEntry();
            try {
                IOUtils.toByteArray(archive);
                fail("expected exception");
            } catch (IOException ex) {
                assertEquals("Truncated ZIP file", ex.getMessage());
            }
            try {
                archive.read(buf);
                fail("expected exception");
            } catch (IOException ex) {
                assertEquals("Truncated ZIP file", ex.getMessage());
            }
            try {
                archive.read(buf);
                fail("expected exception");
            } catch (IOException ex) {
                assertEquals("Truncated ZIP file", ex.getMessage());
            }
        }
    }

    private static byte[] readEntry(ZipArchiveInputStream zip, ZipArchiveEntry zae) throws IOException {
        final int len = (int)zae.getSize();
        final byte[] buff = new byte[len];
        zip.read(buff, 0, len);

        return buff;
    }

    private static void nameSource(String archive, String entry, ZipArchiveEntry.NameSource expected) throws Exception {
        nameSource(archive, entry, 1, expected);
    }

    private static void nameSource(String archive, String entry, int entryNo, ZipArchiveEntry.NameSource expected)
        throws Exception {
        try (ZipArchiveInputStream zis = new ZipArchiveInputStream(new FileInputStream(getFile(archive)))) {
            ZipArchiveEntry ze;
            do {
                ze = zis.getNextZipEntry();
            } while (--entryNo > 0);
            assertEquals(entry, ze.getName());
            assertEquals(expected, ze.getNameSource());
        }
    }
}
