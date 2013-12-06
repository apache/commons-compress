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

import static org.apache.commons.compress.AbstractTestCase.getFile;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.utils.CharsetNames;
import org.junit.Test;

public class TarArchiveInputStreamTest {

    @Test
    public void readSimplePaxHeader() throws Exception {
        final InputStream is = new ByteArrayInputStream(new byte[1]);
        final TarArchiveInputStream tais = new TarArchiveInputStream(is);
        Map<String, String> headers = tais
            .parsePaxHeaders(new ByteArrayInputStream("30 atime=1321711775.972059463\n"
                                                      .getBytes(CharsetNames.UTF_8)));
        assertEquals(1, headers.size());
        assertEquals("1321711775.972059463", headers.get("atime"));
        tais.close();
    }

    @Test
    public void readPaxHeaderWithEmbeddedNewline() throws Exception {
        final InputStream is = new ByteArrayInputStream(new byte[1]);
        final TarArchiveInputStream tais = new TarArchiveInputStream(is);
        Map<String, String> headers = tais
            .parsePaxHeaders(new ByteArrayInputStream("28 comment=line1\nline2\nand3\n"
                                                      .getBytes(CharsetNames.UTF_8)));
        assertEquals(1, headers.size());
        assertEquals("line1\nline2\nand3", headers.get("comment"));
        tais.close();
    }

    @Test
    public void readNonAsciiPaxHeader() throws Exception {
        String ae = "\u00e4";
        String line = "11 path="+ ae + "\n";
        assertEquals(11, line.getBytes(CharsetNames.UTF_8).length);
        final InputStream is = new ByteArrayInputStream(new byte[1]);
        final TarArchiveInputStream tais = new TarArchiveInputStream(is);
        Map<String, String> headers = tais
            .parsePaxHeaders(new ByteArrayInputStream(line.getBytes(CharsetNames.UTF_8)));
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

    private void datePriorToEpoch(String archive) throws Exception {
        TarArchiveInputStream in = null;
        try {
            in = new TarArchiveInputStream(new FileInputStream(getFile(archive)));
            TarArchiveEntry tae = in.getNextTarEntry();
            assertEquals("foo", tae.getName());
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
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
        TarArchiveInputStream tar = getTestStream("/COMPRESS-197.tar");
        try {
            TarArchiveEntry entry = tar.getNextTarEntry();
            while (entry != null) {
                entry = tar.getNextTarEntry();
            }
        } catch (IOException e) {
            fail("COMPRESS-197: " + e.getMessage());
        } finally {
            tar.close();
        }
    }

    @Test
    public void shouldUseSpecifiedEncodingWhenReadingGNULongNames()
        throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        String encoding = CharsetNames.UTF_16;
        String name = "1234567890123456789012345678901234567890123456789"
            + "01234567890123456789012345678901234567890123456789"
            + "01234567890\u00e4";
        TarArchiveOutputStream tos =
            new TarArchiveOutputStream(bos, encoding);
        tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
        TarArchiveEntry t = new TarArchiveEntry(name);
        t.setSize(1);
        tos.putArchiveEntry(t);
        tos.write(30);
        tos.closeArchiveEntry();
        tos.close();
        byte[] data = bos.toByteArray();
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        TarArchiveInputStream tis =
            new TarArchiveInputStream(bis, encoding);
        t = tis.getNextTarEntry();
        assertEquals(name, t.getName());
        tis.close();
    }

    @Test
    public void shouldConsumeArchiveCompletely() throws Exception {
        InputStream is = TarArchiveInputStreamTest.class
            .getResourceAsStream("/archive_with_trailer.tar");
        TarArchiveInputStream tar = new TarArchiveInputStream(is);
        while (tar.getNextTarEntry() != null) {
            // just consume the archive
        }
        byte[] expected = new byte[] {
            'H', 'e', 'l', 'l', 'o', ',', ' ', 'w', 'o', 'r', 'l', 'd', '!', '\n'
        };
        byte[] actual = new byte[expected.length];
        is.read(actual);
        assertArrayEquals(expected, actual);
        tar.close();
    }

    @Test
    public void readsArchiveCompletely_COMPRESS245() throws Exception {
        InputStream is = TarArchiveInputStreamTest.class
            .getResourceAsStream("/COMPRESS-245.tar.gz");
        try {
            InputStream gin = new GZIPInputStream(is);
            TarArchiveInputStream tar = new TarArchiveInputStream(gin);
            int count = 0;
            TarArchiveEntry entry = tar.getNextTarEntry();
            while (entry != null) {
                count++;
                entry = tar.getNextTarEntry();
            }
            assertEquals(31, count);
        } catch (IOException e) {
            fail("COMPRESS-245: " + e.getMessage());
        } finally {
            is.close();
        }
    }


    private TarArchiveInputStream getTestStream(String name) {
        return new TarArchiveInputStream(
                TarArchiveInputStreamTest.class.getResourceAsStream(name));
    }

}
