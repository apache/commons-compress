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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

import org.apache.commons.compress.utils.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.apache.commons.compress.AbstractTestCase.rmdir;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class DataDescriptorTest {

    private File dir;

    @Before
    public void setUp() throws Exception {
        dir = Files.createTempDirectory("ddtest").toFile();
    }

    @After
    public void tearDown() throws Exception {
        rmdir(dir);
    }

    @Test
    public void writesDataDescriptorForDeflatedEntryOnUnseekableOutput() throws IOException {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(o)) {
            zos.putArchiveEntry(new ZipArchiveEntry("test1.txt"));
            zos.write("foo".getBytes("UTF-8"));
            zos.closeArchiveEntry();
        }
        byte[] data = o.toByteArray();

        byte[] versionInLFH = Arrays.copyOfRange(data, 4, 6);
        // 2.0 because of DD
        assertArrayEquals(new byte[] { 20, 0 }, versionInLFH);
        byte[] gpbInLFH = Arrays.copyOfRange(data, 6, 8);
        // DD and EFS flags
        assertArrayEquals(new byte[] { 8, 8 }, gpbInLFH);
        byte[] crcAndSizedInLFH = Arrays.copyOfRange(data, 14, 26);
        assertArrayEquals(new byte[12], crcAndSizedInLFH);

        int cdhStart = findCentralDirectory(data);
        byte[] versionInCDH = Arrays.copyOfRange(data, cdhStart + 6, cdhStart + 8);
        assertArrayEquals(new byte[] { 20, 0 }, versionInCDH);
        byte[] gpbInCDH = Arrays.copyOfRange(data, cdhStart + 8, cdhStart + 10);
        assertArrayEquals(new byte[] { 8, 8 }, gpbInCDH);

        int ddStart = cdhStart - 16;
        assertEquals(ZipLong.DD_SIG, new ZipLong(data, ddStart));
        long crcFromDD = ZipLong.getValue(data, ddStart + 4);
        long cSizeFromDD = ZipLong.getValue(data, ddStart + 8);
        long sizeFromDD = ZipLong.getValue(data, ddStart + 12);
        assertEquals(3, sizeFromDD);

        long crcFromCDH = ZipLong.getValue(data, cdhStart + 16);
        assertEquals(crcFromDD, crcFromCDH);
        long cSizeFromCDH = ZipLong.getValue(data, cdhStart + 20);
        assertEquals(cSizeFromDD, cSizeFromCDH);
        long sizeFromCDH = ZipLong.getValue(data, cdhStart + 24);
        assertEquals(sizeFromDD, sizeFromCDH);
    }

    @Test
    public void doesntWriteDataDescriptorForDeflatedEntryOnSeekableOutput() throws IOException {
        File f = new File(dir, "test.zip");
        try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(f)) {
            zos.putArchiveEntry(new ZipArchiveEntry("test1.txt"));
            zos.write("foo".getBytes("UTF-8"));
            zos.closeArchiveEntry();
        }

        byte[] data;
        try (FileInputStream fis = new FileInputStream(f)) {
            data = IOUtils.toByteArray(fis);
        }

        byte[] versionInLFH = Arrays.copyOfRange(data, 4, 6);
        // still 2.0 because of Deflate
        assertArrayEquals(new byte[] { 20, 0 }, versionInLFH);
        byte[] gpbInLFH = Arrays.copyOfRange(data, 6, 8);
        // no DD but EFS flag
        assertArrayEquals(new byte[] { 0, 8 }, gpbInLFH);

        int cdhStart = findCentralDirectory(data);
        byte[] versionInCDH = Arrays.copyOfRange(data, cdhStart + 6, cdhStart + 8);
        assertArrayEquals(new byte[] { 20, 0 }, versionInCDH);
        byte[] gpbInCDH = Arrays.copyOfRange(data, cdhStart + 8, cdhStart + 10);
        assertArrayEquals(new byte[] { 0, 8 }, gpbInCDH);

        int ddStart = cdhStart - 16;
        assertNotEquals(ZipLong.DD_SIG, new ZipLong(data, ddStart));
        long crcFromLFH = ZipLong.getValue(data, 14);
        long cSizeFromLFH = ZipLong.getValue(data, 18);
        long sizeFromLFH = ZipLong.getValue(data, 22);
        assertEquals(3, sizeFromLFH);

        long crcFromCDH = ZipLong.getValue(data, cdhStart + 16);
        assertEquals(crcFromLFH, crcFromCDH);
        long cSizeFromCDH = ZipLong.getValue(data, cdhStart + 20);
        assertEquals(cSizeFromLFH, cSizeFromCDH);
        long sizeFromCDH = ZipLong.getValue(data, cdhStart + 24);
        assertEquals(sizeFromLFH, sizeFromCDH);
    }

    @Test
    public void doesntWriteDataDescriptorWhenAddingRawEntries() throws IOException {
        ByteArrayOutputStream init = new ByteArrayOutputStream();
        try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(init)) {
            zos.putArchiveEntry(new ZipArchiveEntry("test1.txt"));
            zos.write("foo".getBytes("UTF-8"));
            zos.closeArchiveEntry();
        }

        File f = new File(dir, "test.zip");
        try (FileOutputStream fos = new FileOutputStream(f)) {
            fos.write(init.toByteArray());
        }

        ByteArrayOutputStream o = new ByteArrayOutputStream();
        ZipArchiveEntry zae;
        try (ZipFile zf = new ZipFile(f);
             ZipArchiveOutputStream zos = new ZipArchiveOutputStream(o)) {
            zae = zf.getEntry("test1.txt");
            zos.addRawArchiveEntry(zae, zf.getRawInputStream(zae));
        }

        byte[] data = o.toByteArray();
        byte[] versionInLFH = Arrays.copyOfRange(data, 4, 6);
        // still 2.0 because of Deflate
        assertArrayEquals(new byte[] { 20, 0 }, versionInLFH);
        byte[] gpbInLFH = Arrays.copyOfRange(data, 6, 8);
        // no DD but EFS flag
        assertArrayEquals(new byte[] { 0, 8 }, gpbInLFH);

        int cdhStart = findCentralDirectory(data);
        byte[] versionInCDH = Arrays.copyOfRange(data, cdhStart + 6, cdhStart + 8);
        assertArrayEquals(new byte[] { 20, 0 }, versionInCDH);
        byte[] gpbInCDH = Arrays.copyOfRange(data, cdhStart + 8, cdhStart + 10);
        assertArrayEquals(new byte[] { 0, 8 }, gpbInCDH);

        int ddStart = cdhStart - 16;
        assertNotEquals(ZipLong.DD_SIG, new ZipLong(data, ddStart));
        long crcFromLFH = ZipLong.getValue(data, 14);
        long cSizeFromLFH = ZipLong.getValue(data, 18);
        long sizeFromLFH = ZipLong.getValue(data, 22);
        assertEquals(3, sizeFromLFH);

        long crcFromCDH = ZipLong.getValue(data, cdhStart + 16);
        assertEquals(crcFromLFH, crcFromCDH);
        long cSizeFromCDH = ZipLong.getValue(data, cdhStart + 20);
        assertEquals(cSizeFromLFH, cSizeFromCDH);
        long sizeFromCDH = ZipLong.getValue(data, cdhStart + 24);
        assertEquals(sizeFromLFH, sizeFromCDH);
    }

    private int findCentralDirectory(byte[] data) {
        // not a ZIP64 archive, no comment, "End of central directory record" at the end
        return (int) ZipLong.getValue(data, data.length - 22 + 16);
    }
}
