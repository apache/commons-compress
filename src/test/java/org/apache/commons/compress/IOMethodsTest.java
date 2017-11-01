/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.commons.compress;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.junit.Test;

/**
 * Check that the different write methods create the same output.
 * TODO perform the same checks for reads.
 */
public class IOMethodsTest extends AbstractTestCase {

    private static final int bytesToTest = 50;
    private static final byte[] byteTest = new byte[bytesToTest];
    static {
        for(int i=0; i < byteTest.length ;) {
            byteTest[i]=(byte) i;
            byteTest[i+1]=(byte) -i;
            i += 2;
        }
    }

    @Test
    public void testWriteAr() throws Exception {
        final ArchiveEntry entry = new ArArchiveEntry("dummy", bytesToTest);
        compareWrites("ar", entry);
    }

    @Test
    public void testWriteCpio() throws Exception {
        final ArchiveEntry entry = new CpioArchiveEntry("dummy", bytesToTest);
        compareWrites("cpio", entry);
    }

    @Test
    public void testWriteJar() throws Exception {
        final ArchiveEntry entry = new JarArchiveEntry("dummy");
        compareWrites("jar", entry);
    }

    @Test
    public void testWriteTar() throws Exception {
        final TarArchiveEntry entry = new TarArchiveEntry("dummy");
        entry.setSize(bytesToTest);
        compareWrites("tar", entry);
    }

    @Test
    public void testWriteZip() throws Exception {
        final ArchiveEntry entry = new ZipArchiveEntry("dummy");
        compareWrites("zip", entry);
    }

    @Test
    public void testReadAr() throws Exception {
        compareReads("ar");
    }

    @Test
    public void testReadCpio() throws Exception {
        compareReads("cpio");
    }

    @Test
    public void testReadJar() throws Exception {
        compareReads("jar");
    }

    @Test
    public void testReadTar() throws Exception {
        compareReads("tar");
    }

    @Test
    public void testReadZip() throws Exception {
        compareReads("zip");
    }

    private void compareWrites(final String archiverName, final ArchiveEntry entry) throws Exception {
        final OutputStream out1 = new ByteArrayOutputStream();
        final OutputStream out2 = new ByteArrayOutputStream();
        final OutputStream out3 = new ByteArrayOutputStream();
        final ArchiveOutputStream aos1 = factory.createArchiveOutputStream(archiverName, out1);
        aos1.putArchiveEntry(entry);
        final ArchiveOutputStream aos2 = factory.createArchiveOutputStream(archiverName, out2);
        aos2.putArchiveEntry(entry);
        final ArchiveOutputStream aos3 = factory.createArchiveOutputStream(archiverName, out3);
        aos3.putArchiveEntry(entry);
        for (final byte element : byteTest) {
            aos1.write(element);
        }
        aos1.closeArchiveEntry();
        aos1.close();

        aos2.write(byteTest);
        aos2.closeArchiveEntry();
        aos2.close();

        aos3.write(byteTest, 0, byteTest.length);
        aos3.closeArchiveEntry();
        aos3.close();
        assertEquals("aos1Bytes!=aos2Bytes",aos1.getBytesWritten(),aos2.getBytesWritten());
        assertEquals("aos1Bytes!=aos3Bytes",aos1.getBytesWritten(),aos3.getBytesWritten());
        assertEquals("out1Len!=out2Len",out1.toString().length(),out2.toString().length());
        assertEquals("out1Len!=out2Len",out1.toString().length(),out3.toString().length());
        assertEquals("out1!=out2",out1.toString(),out2.toString());
        assertEquals("out1!=out3",out1.toString(),out3.toString());
    }

    private void compareReads(final String archiverName) throws Exception {
        final OutputStream out1 = new ByteArrayOutputStream();
        final OutputStream out2 = new ByteArrayOutputStream();
        final OutputStream out3 = new ByteArrayOutputStream();
        final File file = createSingleEntryArchive(archiverName);
        file.deleteOnExit();

        final InputStream is1 = new FileInputStream(file);
        final ArchiveInputStream ais1 = factory.createArchiveInputStream(archiverName, is1);
        final ArchiveEntry nextEntry = ais1.getNextEntry();
        assertNotNull(nextEntry);

        final byte [] buff = new byte[10]; // small so multiple reads are needed;
        final long size = nextEntry.getSize();
        if (size != ArchiveEntry.SIZE_UNKNOWN) {
            assertTrue("Size should be > 0, found: "+size, size > 0);
        }

        final InputStream is2 = new FileInputStream(file);
        final ArchiveInputStream ais2 = factory.createArchiveInputStream(archiverName, is2);
        final ArchiveEntry nextEntry2 = ais2.getNextEntry();
        assertNotNull(nextEntry2);
        assertEquals("Expected same entry size", size, nextEntry2.getSize());

        final InputStream is3 = new FileInputStream(file);
        final ArchiveInputStream ais3 = factory.createArchiveInputStream(archiverName, is3);
        final ArchiveEntry nextEntry3 = ais3.getNextEntry();
        assertNotNull(nextEntry3);
        assertEquals("Expected same entry size", size, nextEntry3.getSize());

        int b;
        while((b=ais1.read()) != -1){
            out1.write(b);
        }
        ais1.close();

        int bytes;
        while((bytes = ais2.read(buff)) > 0){
            out2.write(buff, 0, bytes);
        }
        ais2.close();

        while((bytes=ais3.read(buff, 0 , buff.length)) > 0){
            out3.write(buff, 0, bytes);
        }
        ais3.close();

        assertEquals("out1Len!=out2Len",out1.toString().length(),out2.toString().length());
        assertEquals("out1Len!=out3Len",out1.toString().length(),out3.toString().length());
        assertEquals("out1!=out2",out1.toString(),out2.toString());
        assertEquals("out1!=out3",out1.toString(),out3.toString());
    }
}
