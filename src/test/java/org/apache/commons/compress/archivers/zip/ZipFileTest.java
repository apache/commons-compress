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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.zip.ZipEntry;

import junit.framework.TestCase;

public class ZipFileTest extends TestCase {
    private ZipFile zf = null;

    @Override
    public void tearDown() {
        ZipFile.closeQuietly(zf);
    }

    public void testCDOrder() throws Exception {
        readOrderTest();
        ArrayList<ZipArchiveEntry> l = Collections.list(zf.getEntries());
        assertEntryName(l, 0, "AbstractUnicodeExtraField");
        assertEntryName(l, 1, "AsiExtraField");
        assertEntryName(l, 2, "ExtraFieldUtils");
        assertEntryName(l, 3, "FallbackZipEncoding");
        assertEntryName(l, 4, "GeneralPurposeBit");
        assertEntryName(l, 5, "JarMarker");
        assertEntryName(l, 6, "NioZipEncoding");
        assertEntryName(l, 7, "Simple8BitZipEncoding");
        assertEntryName(l, 8, "UnicodeCommentExtraField");
        assertEntryName(l, 9, "UnicodePathExtraField");
        assertEntryName(l, 10, "UnixStat");
        assertEntryName(l, 11, "UnparseableExtraFieldData");
        assertEntryName(l, 12, "UnrecognizedExtraField");
        assertEntryName(l, 13, "ZipArchiveEntry");
        assertEntryName(l, 14, "ZipArchiveInputStream");
        assertEntryName(l, 15, "ZipArchiveOutputStream");
        assertEntryName(l, 16, "ZipEncoding");
        assertEntryName(l, 17, "ZipEncodingHelper");
        assertEntryName(l, 18, "ZipExtraField");
        assertEntryName(l, 19, "ZipUtil");
        assertEntryName(l, 20, "ZipLong");
        assertEntryName(l, 21, "ZipShort");
        assertEntryName(l, 22, "ZipFile");
    }

    public void testPhysicalOrder() throws Exception {
        readOrderTest();
        ArrayList<ZipArchiveEntry> l = Collections.list(zf.getEntriesInPhysicalOrder());
        assertEntryName(l, 0, "AbstractUnicodeExtraField");
        assertEntryName(l, 1, "AsiExtraField");
        assertEntryName(l, 2, "ExtraFieldUtils");
        assertEntryName(l, 3, "FallbackZipEncoding");
        assertEntryName(l, 4, "GeneralPurposeBit");
        assertEntryName(l, 5, "JarMarker");
        assertEntryName(l, 6, "NioZipEncoding");
        assertEntryName(l, 7, "Simple8BitZipEncoding");
        assertEntryName(l, 8, "UnicodeCommentExtraField");
        assertEntryName(l, 9, "UnicodePathExtraField");
        assertEntryName(l, 10, "UnixStat");
        assertEntryName(l, 11, "UnparseableExtraFieldData");
        assertEntryName(l, 12, "UnrecognizedExtraField");
        assertEntryName(l, 13, "ZipArchiveEntry");
        assertEntryName(l, 14, "ZipArchiveInputStream");
        assertEntryName(l, 15, "ZipArchiveOutputStream");
        assertEntryName(l, 16, "ZipEncoding");
        assertEntryName(l, 17, "ZipEncodingHelper");
        assertEntryName(l, 18, "ZipExtraField");
        assertEntryName(l, 19, "ZipFile");
        assertEntryName(l, 20, "ZipLong");
        assertEntryName(l, 21, "ZipShort");
        assertEntryName(l, 22, "ZipUtil");
    }

    public void testDoubleClose() throws Exception {
        readOrderTest();
        zf.close();
        try {
            zf.close();
        } catch (Exception ex) {
            fail("Caught exception of second close");
        }
    }

    public void testReadingOfStoredEntry() throws Exception {
        File f = File.createTempFile("commons-compress-zipfiletest", ".zip");
        f.deleteOnExit();
        OutputStream o = null;
        InputStream i = null;
        try {
            o = new FileOutputStream(f);
            ZipArchiveOutputStream zo = new ZipArchiveOutputStream(o);
            ZipArchiveEntry ze = new ZipArchiveEntry("foo");
            ze.setMethod(ZipEntry.STORED);
            ze.setSize(4);
            ze.setCrc(0xb63cfbcdl);
            zo.putArchiveEntry(ze);
            zo.write(new byte[] { 1, 2, 3, 4 });
            zo.closeArchiveEntry();
            zo.close();
            o.close();
            o  = null;

            zf = new ZipFile(f);
            ze = zf.getEntry("foo");
            assertNotNull(ze);
            i = zf.getInputStream(ze);
            byte[] b = new byte[4];
            assertEquals(4, i.read(b));
            assertEquals(-1, i.read());
        } finally {
            if (o != null) {
                o.close();
            }
            if (i != null) {
                i.close();
            }
            f.delete();
        }
    }

    /**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-176"
     */
    public void testWinzipBackSlashWorkaround() throws Exception {
        URL zip = getClass().getResource("/test-winzip.zip");
        File archive = new File(new URI(zip.toString()));
        zf = new ZipFile(archive);
        assertNull(zf.getEntry("\u00e4\\\u00fc.txt"));
        assertNotNull(zf.getEntry("\u00e4/\u00fc.txt"));
    }

    /*
     * ordertest.zip has been handcrafted.
     *
     * It contains enough files so any random coincidence of
     * entries.keySet() and central directory order would be unlikely
     * - in fact testCDOrder fails in svn revision 920284.
     *
     * The central directory has ZipFile and ZipUtil swapped so
     * central directory order is different from entry data order.
     */
    private void readOrderTest() throws Exception {
        URL zip = getClass().getResource("/ordertest.zip");
        File archive = new File(new URI(zip.toString()));
        zf = new ZipFile(archive);
    }

    private static void assertEntryName(ArrayList<ZipArchiveEntry> entries,
                                        int index,
                                        String expectedName) {
        ZipArchiveEntry ze = entries.get(index);
        assertEquals("src/main/java/org/apache/commons/compress/archivers/zip/"
                     + expectedName + ".java",
                     ze.getName());
    }
}
