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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.Random;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.junit.Test;

public class BigFilesIT {

    @Test
    public void readFileBiggerThan8GByteStar() throws Exception {
        readFileBiggerThan8GByte("/8.star.tar.gz");
    }

    @Test
    public void readFileBiggerThan8GBytePosix() throws Exception {
        readFileBiggerThan8GByte("/8.posix.tar.gz");
    }

    @Test
    public void readFileHeadersOfArchiveBiggerThan8GByte() throws Exception {
        InputStream in = null;
        GzipCompressorInputStream gzin = null;
        TarArchiveInputStream tin = null;
        try {
            in = new BufferedInputStream(BigFilesIT.class
                                         .getResourceAsStream("/8.posix.tar.gz")
                                         );
            gzin = new GzipCompressorInputStream(in);
            tin = new TarArchiveInputStream(gzin);
            final TarArchiveEntry e = tin.getNextTarEntry();
            assertNotNull(e);
            assertNull(tin.getNextTarEntry());
        } finally {
            if (tin != null) {
                tin.close();
            }
            if (gzin != null) {
                gzin.close();
            }
            if (in != null) {
                in.close();
            }
        }
    }

    private void readFileBiggerThan8GByte(final String name) throws Exception {
        InputStream in = null;
        GzipCompressorInputStream gzin = null;
        TarArchiveInputStream tin = null;
        try {
            in = new BufferedInputStream(BigFilesIT.class
                                         .getResourceAsStream(name));
            gzin = new GzipCompressorInputStream(in);
            tin = new TarArchiveInputStream(gzin);
            final TarArchiveEntry e = tin.getNextTarEntry();
            assertNotNull(e);
            assertEquals(8200l * 1024 * 1024, e.getSize());

            long read = 0;
            final Random r = new Random(System.currentTimeMillis());
            int readNow;
            final byte[] buf = new byte[1024 * 1024];
            while ((readNow = tin.read(buf, 0, buf.length)) > 0) {
                // testing all bytes for a value of 0 is going to take
                // too long, just pick a few ones randomly
                for (int i = 0; i < 100; i++) {
                    final int idx = r.nextInt(readNow);
                    assertEquals("testing byte " + (read + idx), 0, buf[idx]);
                }
                read += readNow;
            }
            assertEquals(8200l * 1024 * 1024, read);
            assertNull(tin.getNextTarEntry());
        } finally {
            if (tin != null) {
                tin.close();
            }
            if (gzin != null) {
                gzin.close();
            }
            if (in != null) {
                in.close();
            }
        }
    }

}
