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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.compress.utils.CharsetNames;
import org.junit.Test;

public class TarArchiveInputStreamTest {

    @Test
    public void readSimplePaxHeader() throws Exception {
        Map<String, String> headers = new TarArchiveInputStream(null)
            .parsePaxHeaders(new ByteArrayInputStream("30 atime=1321711775.972059463\n"
                                                      .getBytes(CharsetNames.UTF_8)));
        assertEquals(1, headers.size());
        assertEquals("1321711775.972059463", headers.get("atime"));
    }

    @Test
    public void readPaxHeaderWithEmbeddedNewline() throws Exception {
        Map<String, String> headers = new TarArchiveInputStream(null)
            .parsePaxHeaders(new ByteArrayInputStream("28 comment=line1\nline2\nand3\n"
                                                      .getBytes(CharsetNames.UTF_8)));
        assertEquals(1, headers.size());
        assertEquals("line1\nline2\nand3", headers.get("comment"));
    }

    @Test
    public void readNonAsciiPaxHeader() throws Exception {
        String ae = "\u00e4";
        String line = "11 path="+ ae + "\n";
        assertEquals(11, line.getBytes(CharsetNames.UTF_8).length);
        Map<String, String> headers = new TarArchiveInputStream(null)
            .parsePaxHeaders(new ByteArrayInputStream(line.getBytes(CharsetNames.UTF_8)));
        assertEquals(1, headers.size());
        assertEquals(ae, headers.get("path"));
    }

    @Test
    public void workaroundForBrokenTimeHeader() throws Exception {
        URL tar = getClass().getResource("/simple-aix-native-tar.tar");
        TarArchiveInputStream in = null;
        try {
            in = new TarArchiveInputStream(new FileInputStream(new File(new URI(tar.toString()))));
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
        datePriorToEpoch("/preepoch-star.tar");
    }


    @Test
    public void datePriorToEpochInPAXFormat() throws Exception {
        datePriorToEpoch("/preepoch-posix.tar");
    }

    private void datePriorToEpoch(String archive) throws Exception {
        URL tar = getClass().getResource(archive);
        TarArchiveInputStream in = null;
        try {
            in = new TarArchiveInputStream(new FileInputStream(new File(new URI(tar.toString()))));
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

}