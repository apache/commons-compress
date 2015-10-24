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

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.zip.CRC32;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.utils.CharsetNames;
import org.junit.Test;

public class UTF8ZipFilesTest extends AbstractTestCase {

    private static final String CP437 = "cp437";
    private static final String ASCII_TXT = "ascii.txt";
    private static final String EURO_FOR_DOLLAR_TXT = "\u20AC_for_Dollar.txt";
    private static final String OIL_BARREL_TXT = "\u00D6lf\u00E4sser.txt";

    @Test
    public void testUtf8FileRoundtripExplicitUnicodeExtra()
        throws IOException {
        testFileRoundtrip(CharsetNames.UTF_8, true, true);
    }

    @Test
    public void testUtf8FileRoundtripNoEFSExplicitUnicodeExtra()
        throws IOException {
        testFileRoundtrip(CharsetNames.UTF_8, false, true);
    }

    @Test
    public void testCP437FileRoundtripExplicitUnicodeExtra()
        throws IOException {
        testFileRoundtrip(CP437, false, true);
    }

    @Test
    public void testASCIIFileRoundtripExplicitUnicodeExtra()
        throws IOException {
        testFileRoundtrip(CharsetNames.US_ASCII, false, true);
    }

    @Test
    public void testUtf8FileRoundtripImplicitUnicodeExtra()
        throws IOException {
        testFileRoundtrip(CharsetNames.UTF_8, true, false);
    }

    @Test
    public void testUtf8FileRoundtripNoEFSImplicitUnicodeExtra()
        throws IOException {
        testFileRoundtrip(CharsetNames.UTF_8, false, false);
    }

    @Test
    public void testCP437FileRoundtripImplicitUnicodeExtra()
        throws IOException {
        testFileRoundtrip(CP437, false, false);
    }

    @Test
    public void testASCIIFileRoundtripImplicitUnicodeExtra()
        throws IOException {
        testFileRoundtrip(CharsetNames.US_ASCII, false, false);
    }

    /*
     * 7-ZIP created archive, uses EFS to signal UTF-8 filenames.
     *
     * 7-ZIP doesn't use EFS for strings that can be encoded in CP437
     * - which is true for OIL_BARREL_TXT.
     */
    @Test
    public void testRead7ZipArchive() throws IOException {
        File archive = getFile("utf8-7zip-test.zip");
        ZipFile zf = null;
        try {
            zf = new ZipFile(archive, CP437, false);
            assertNotNull(zf.getEntry(ASCII_TXT));
            assertNotNull(zf.getEntry(EURO_FOR_DOLLAR_TXT));
            assertNotNull(zf.getEntry(OIL_BARREL_TXT));
        } finally {
            ZipFile.closeQuietly(zf);
        }
    }

    @Test
    public void testRead7ZipArchiveForStream() throws IOException {
        FileInputStream archive =
            new FileInputStream(getFile("utf8-7zip-test.zip"));
        ZipArchiveInputStream zi = null;
        try {
            zi = new ZipArchiveInputStream(archive, CP437, false);
            assertEquals(ASCII_TXT, zi.getNextEntry().getName());
            assertEquals(OIL_BARREL_TXT, zi.getNextEntry().getName());
            assertEquals(EURO_FOR_DOLLAR_TXT, zi.getNextEntry().getName());
        } finally {
            if (zi != null) {
                zi.close();
            }
        }
    }

    /*
     * WinZIP created archive, uses Unicode Extra Fields but only in
     * the central directory.
     */
    @Test
    public void testReadWinZipArchive() throws IOException {
        File archive = getFile("utf8-winzip-test.zip");
        ZipFile zf = null;
        try {
            zf = new ZipFile(archive, null, true);
            assertCanRead(zf, ASCII_TXT);
            assertCanRead(zf, EURO_FOR_DOLLAR_TXT);
            assertCanRead(zf, OIL_BARREL_TXT);
        } finally {
            ZipFile.closeQuietly(zf);
        }
    }

    private void assertCanRead(ZipFile zf, String fileName) throws IOException {
        ZipArchiveEntry entry = zf.getEntry(fileName);
        assertNotNull("Entry doesn't exist", entry);
        InputStream is = zf.getInputStream(entry);
        assertNotNull("InputStream is null", is);
        try {
            is.read();
        } finally {
            is.close();
        }
    }

    @Test
    public void testReadWinZipArchiveForStream() throws IOException {
        FileInputStream archive =
            new FileInputStream(getFile("utf8-winzip-test.zip"));
        ZipArchiveInputStream zi = null;
        try {
            zi = new ZipArchiveInputStream(archive, null, true);
            assertEquals(EURO_FOR_DOLLAR_TXT, zi.getNextEntry().getName());
            assertEquals(OIL_BARREL_TXT, zi.getNextEntry().getName());
            assertEquals(ASCII_TXT, zi.getNextEntry().getName());
        } finally {
            if (zi != null) {
                zi.close();
            }
        }
    }

    @Test
    public void testZipFileReadsUnicodeFields() throws IOException {
        File file = File.createTempFile("unicode-test", ".zip");
        file.deleteOnExit();
        ZipArchiveInputStream zi = null;
        try {
            createTestFile(file, CharsetNames.US_ASCII, false, true);
            FileInputStream archive = new FileInputStream(file);
            zi = new ZipArchiveInputStream(archive, CharsetNames.US_ASCII, true);
            assertEquals(OIL_BARREL_TXT, zi.getNextEntry().getName());
            assertEquals(EURO_FOR_DOLLAR_TXT, zi.getNextEntry().getName());
            assertEquals(ASCII_TXT, zi.getNextEntry().getName());
        } finally {
            if (zi != null) {
                zi.close();
            }
            tryHardToDelete(file);
        }
    }

    @Test
    public void testZipArchiveInputStreamReadsUnicodeFields()
        throws IOException {
        File file = File.createTempFile("unicode-test", ".zip");
        file.deleteOnExit();
        ZipFile zf = null;
        try {
            createTestFile(file, CharsetNames.US_ASCII, false, true);
            zf = new ZipFile(file, CharsetNames.US_ASCII, true);
            assertNotNull(zf.getEntry(ASCII_TXT));
            assertNotNull(zf.getEntry(EURO_FOR_DOLLAR_TXT));
            assertNotNull(zf.getEntry(OIL_BARREL_TXT));
        } finally {
            ZipFile.closeQuietly(zf);
            tryHardToDelete(file);
        }
    }

    @Test
    public void testRawNameReadFromZipFile()
        throws IOException {
        File archive = getFile("utf8-7zip-test.zip");
        ZipFile zf = null;
        try {
            zf = new ZipFile(archive, CP437, false);
            assertRawNameOfAcsiiTxt(zf.getEntry(ASCII_TXT));
        } finally {
            ZipFile.closeQuietly(zf);
        }
    }

    @Test
    public void testRawNameReadFromStream()
        throws IOException {
        FileInputStream archive =
            new FileInputStream(getFile("utf8-7zip-test.zip"));
        ZipArchiveInputStream zi = null;
        try {
            zi = new ZipArchiveInputStream(archive, CP437, false);
            assertRawNameOfAcsiiTxt((ZipArchiveEntry) zi.getNextEntry());
        } finally {
            if (zi != null) {
                zi.close();
            }
        }
    }

    private static void testFileRoundtrip(String encoding, boolean withEFS,
                                          boolean withExplicitUnicodeExtra)
        throws IOException {

        File file = File.createTempFile(encoding + "-test", ".zip");
        file.deleteOnExit();
        try {
            createTestFile(file, encoding, withEFS, withExplicitUnicodeExtra);
            testFile(file, encoding);
        } finally {
            tryHardToDelete(file);
        }
    }

    private static void createTestFile(File file, String encoding,
                                       boolean withEFS,
                                       boolean withExplicitUnicodeExtra)
        throws UnsupportedEncodingException, IOException {

        ZipEncoding zipEncoding = ZipEncodingHelper.getZipEncoding(encoding);

        ZipArchiveOutputStream zos = null;
        try {
            zos = new ZipArchiveOutputStream(file);
            zos.setEncoding(encoding);
            zos.setUseLanguageEncodingFlag(withEFS);
            zos.setCreateUnicodeExtraFields(withExplicitUnicodeExtra ? 
                                            ZipArchiveOutputStream
                                            .UnicodeExtraFieldPolicy.NEVER
                                            : ZipArchiveOutputStream
                                            .UnicodeExtraFieldPolicy.ALWAYS);

            ZipArchiveEntry ze = new ZipArchiveEntry(OIL_BARREL_TXT);
            if (withExplicitUnicodeExtra
                && !zipEncoding.canEncode(ze.getName())) {

                ByteBuffer en = zipEncoding.encode(ze.getName());

                ze.addExtraField(new UnicodePathExtraField(ze.getName(),
                                                           en.array(),
                                                           en.arrayOffset(),
                                                           en.limit()
                                                           - en.position()));
            }

            zos.putArchiveEntry(ze);
            zos.write("Hello, world!".getBytes(CharsetNames.US_ASCII));
            zos.closeArchiveEntry();

            ze = new ZipArchiveEntry(EURO_FOR_DOLLAR_TXT);
            if (withExplicitUnicodeExtra
                && !zipEncoding.canEncode(ze.getName())) {

                ByteBuffer en = zipEncoding.encode(ze.getName());

                ze.addExtraField(new UnicodePathExtraField(ze.getName(),
                                                           en.array(),
                                                           en.arrayOffset(),
                                                           en.limit()
                                                           - en.position()));
            }

            zos.putArchiveEntry(ze);
            zos.write("Give me your money!".getBytes(CharsetNames.US_ASCII));
            zos.closeArchiveEntry();

            ze = new ZipArchiveEntry(ASCII_TXT);

            if (withExplicitUnicodeExtra
                && !zipEncoding.canEncode(ze.getName())) {

                ByteBuffer en = zipEncoding.encode(ze.getName());

                ze.addExtraField(new UnicodePathExtraField(ze.getName(),
                                                           en.array(),
                                                           en.arrayOffset(),
                                                           en.limit()
                                                           - en.position()));
            }

            zos.putArchiveEntry(ze);
            zos.write("ascii".getBytes(CharsetNames.US_ASCII));
            zos.closeArchiveEntry();

            zos.finish();
        } finally {
            if (zos != null) {
                try {
                    zos.close();
                } catch (IOException e) { /* swallow */ }
            }
        }
    }

    private static void testFile(File file, String encoding)
        throws IOException {
        ZipFile zf = null;
        try {
            zf = new ZipFile(file, encoding, false);

            Enumeration<ZipArchiveEntry> e = zf.getEntries();
            while (e.hasMoreElements()) {
                ZipArchiveEntry ze = e.nextElement();

                if (ze.getName().endsWith("sser.txt")) {
                    assertUnicodeName(ze, OIL_BARREL_TXT, encoding);

                } else if (ze.getName().endsWith("_for_Dollar.txt")) {
                    assertUnicodeName(ze, EURO_FOR_DOLLAR_TXT, encoding);
                } else if (!ze.getName().equals(ASCII_TXT)) {
                    throw new AssertionError("Unrecognized ZIP entry with name ["
                                             + ze.getName() + "] found.");
                }
            }
        } finally {
            ZipFile.closeQuietly(zf);
        }
    }

    private static UnicodePathExtraField findUniCodePath(ZipArchiveEntry ze) {
        return (UnicodePathExtraField)
            ze.getExtraField(UnicodePathExtraField.UPATH_ID);
    }

    private static void assertUnicodeName(ZipArchiveEntry ze,
                                          String expectedName,
                                          String encoding)
        throws IOException {
        if (!expectedName.equals(ze.getName())) {
            UnicodePathExtraField ucpf = findUniCodePath(ze);
            assertNotNull(ucpf);

            ZipEncoding enc = ZipEncodingHelper.getZipEncoding(encoding);
            ByteBuffer ne = enc.encode(ze.getName());

            CRC32 crc = new CRC32();
            crc.update(ne.array(), ne.arrayOffset(),
                       ne.limit() - ne.position());

            assertEquals(crc.getValue(), ucpf.getNameCRC32());
            assertEquals(expectedName, new String(ucpf.getUnicodeName(),
                                                  CharsetNames.UTF_8));
        }
    }

    @Test
    public void testUtf8Interoperability() throws IOException {
        File file1 = getFile("utf8-7zip-test.zip");
        File file2 = getFile("utf8-winzip-test.zip");

        testFile(file1,CP437);
        testFile(file2,CP437);

    }

    private static void assertRawNameOfAcsiiTxt(ZipArchiveEntry ze) {
        byte[] b = ze.getRawName();
        assertNotNull(b);
        final int len = ASCII_TXT.length();
        assertEquals(len, b.length);
        for (int i = 0; i < len; i++) {
            assertEquals("Byte " + i, (byte) ASCII_TXT.charAt(i), b[i]);
        }
        assertNotSame(b, ze.getRawName());
    }
}

