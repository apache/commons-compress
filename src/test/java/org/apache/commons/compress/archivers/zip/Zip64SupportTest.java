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
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Random;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;

public class Zip64SupportTest {

    private static final long FIVE_BILLION = 5000000000l;
    private static final int ONE_HUNDRED_THOUSAND = 100000;

    @Test public void read5GBOfZerosUsingInputStream() throws Throwable {
        FileInputStream fin = new FileInputStream(get5GBZerosFile());
        ZipArchiveInputStream zin = null;
        try {
            zin = new ZipArchiveInputStream(fin);
            ZipArchiveEntry zae = zin.getNextZipEntry();
            assertEquals("5GB_of_Zeros", zae.getName());
            assertEquals(FIVE_BILLION, zae.getSize());
            byte[] buf = new byte[1024 * 1024];
            long read = 0;
            Random r = new Random(System.currentTimeMillis());
            int readNow;
            while ((readNow = zin.read(buf, 0, buf.length)) > 0) {
                // testing all bytes for a value of 0 is going to take
                // too long, just pick a few ones randomly
                for (int i = 0; i < 1024; i++) {
                    int idx = r.nextInt(readNow);
                    assertEquals("testing byte " + (read + idx), 0, buf[idx]);
                }
                read += readNow;
            }
            assertEquals(FIVE_BILLION, read);
            assertNull(zin.getNextZipEntry());
        } finally {
            if (zin != null) {
                zin.close();
            }
            if (fin != null) {
                fin.close();
            }
        }
    }

    @Test public void read100KFilesUsingInputStream() throws Throwable {
        FileInputStream fin = new FileInputStream(get100KFileFile());
        ZipArchiveInputStream zin = null;
        try {
            zin = new ZipArchiveInputStream(fin);
            int files = 0;
            ZipArchiveEntry zae = null;
            while ((zae = zin.getNextZipEntry()) != null) {
                if (!zae.isDirectory()) {
                    files++;
                    assertEquals(0, zae.getSize());
                }
            }
            assertEquals(ONE_HUNDRED_THOUSAND, files);
        } finally {
            if (zin != null) {
                zin.close();
            }
            fin.close();
        }
    }

    @Test public void write100KFiles() throws Throwable {
        withTemporaryArchive("write100KFiles", new ZipOutputTest() {
                public void test(File f, ZipArchiveOutputStream zos)
                    throws IOException {
                    for (int i = 0; i < ONE_HUNDRED_THOUSAND; i++) {
                        ZipArchiveEntry zae =
                            new ZipArchiveEntry(String.valueOf(i));
                        zae.setSize(0);
                        zos.putArchiveEntry(zae);
                        zos.closeArchiveEntry();
                    }
                }
            });
    }

    static interface ZipOutputTest {
        void test(File f, ZipArchiveOutputStream zos) throws IOException;
    }

    private static void withTemporaryArchive(String testName,
                                             ZipOutputTest test)
        throws Throwable {
        File f = getTempFile(testName);
        ZipArchiveOutputStream zos = new ZipArchiveOutputStream(f);
        try {
            test.test(f, zos);
        } catch (IOException ex) {
            System.err.println("Failed to write archive because of: "
                               + ex.getMessage()
                               + " - likely not enough disk space.");
            assumeTrue(false);
        } finally {
            zos.close();
        }
    }

    private static File getFile(String name) throws Throwable {
        URL url = Zip64SupportTest.class.getResource(name);
        assumeNotNull(url);
        File file = new File(new URI(url.toString()));
        assumeTrue(file.exists());
        return file;
    }

    private static File get5GBZerosFile() throws Throwable {
        return getFile("/5GB_of_Zeros.zip");
    }

    private static File get100KFileFile() throws Throwable {
        return getFile("/100k_Files.zip");
    }

    private static File getTempFile(String testName) throws Throwable {
        File f = File.createTempFile("commons-compress-" + testName, ".zip");
        f.deleteOnExit();
        return f;
    }
}
