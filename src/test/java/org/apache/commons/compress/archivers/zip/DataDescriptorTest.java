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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.compress.AbstractTestCase.mkdir;
import static org.apache.commons.compress.AbstractTestCase.rmdir;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DataDescriptorTest {

    private File dir;

    @Test
    public void doesntWriteDataDescriptorForDeflatedEntryOnSeekableOutput() throws IOException {
        final File f = new File(dir, "test.zip");
        try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(f)) {
            zos.putArchiveEntry(new ZipArchiveEntry("test1.txt"));
            zos.write("foo".getBytes(UTF_8));
            zos.closeArchiveEntry();
        }

        final byte[] data = Files.readAllBytes(f.toPath());

        final byte[] versionInLFH = Arrays.copyOfRange(data, 4, 6);
        // still 2.0 because of Deflate
        assertArrayEquals(new byte[] { 20, 0 }, versionInLFH);
        final byte[] gpbInLFH = Arrays.copyOfRange(data, 6, 8);
        // no DD but EFS flag
        assertArrayEquals(new byte[] { 0, 8 }, gpbInLFH);

        final int cdhStart = findCentralDirectory(data);
        final byte[] versionInCDH = Arrays.copyOfRange(data, cdhStart + 6, cdhStart + 8);
        assertArrayEquals(new byte[] { 20, 0 }, versionInCDH);
        final byte[] gpbInCDH = Arrays.copyOfRange(data, cdhStart + 8, cdhStart + 10);
        assertArrayEquals(new byte[] { 0, 8 }, gpbInCDH);

        final int ddStart = cdhStart - 16;
        assertNotEquals(ZipLong.DD_SIG, new ZipLong(data, ddStart));
        final long crcFromLFH = ZipLong.getValue(data, 14);
        final long cSizeFromLFH = ZipLong.getValue(data, 18);
        final long sizeFromLFH = ZipLong.getValue(data, 22);
        assertEquals(3, sizeFromLFH);

        final long crcFromCDH = ZipLong.getValue(data, cdhStart + 16);
        assertEquals(crcFromLFH, crcFromCDH);
        final long cSizeFromCDH = ZipLong.getValue(data, cdhStart + 20);
        assertEquals(cSizeFromLFH, cSizeFromCDH);
        final long sizeFromCDH = ZipLong.getValue(data, cdhStart + 24);
        assertEquals(sizeFromLFH, sizeFromCDH);
    }

    @Test
    public void doesntWriteDataDescriptorWhenAddingRawEntries() throws IOException {
        final ByteArrayOutputStream init = new ByteArrayOutputStream();
        try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(init)) {
            zos.putArchiveEntry(new ZipArchiveEntry("test1.txt"));
            zos.write("foo".getBytes(UTF_8));
            zos.closeArchiveEntry();
        }

        final File f = new File(dir, "test.zip");
        try (OutputStream fos = Files.newOutputStream(f.toPath())) {
            fos.write(init.toByteArray());
        }

        final ByteArrayOutputStream o = new ByteArrayOutputStream();
        final ZipArchiveEntry zae;
        try (ZipFile zf = new ZipFile(f);
             ZipArchiveOutputStream zos = new ZipArchiveOutputStream(o)) {
            zae = zf.getEntry("test1.txt");
            zos.addRawArchiveEntry(zae, zf.getRawInputStream(zae));
        }

        final byte[] data = o.toByteArray();
        final byte[] versionInLFH = Arrays.copyOfRange(data, 4, 6);
        // still 2.0 because of Deflate
        assertArrayEquals(new byte[] { 20, 0 }, versionInLFH);
        final byte[] gpbInLFH = Arrays.copyOfRange(data, 6, 8);
        // no DD but EFS flag
        assertArrayEquals(new byte[] { 0, 8 }, gpbInLFH);

        final int cdhStart = findCentralDirectory(data);
        final byte[] versionInCDH = Arrays.copyOfRange(data, cdhStart + 6, cdhStart + 8);
        assertArrayEquals(new byte[] { 20, 0 }, versionInCDH);
        final byte[] gpbInCDH = Arrays.copyOfRange(data, cdhStart + 8, cdhStart + 10);
        assertArrayEquals(new byte[] { 0, 8 }, gpbInCDH);

        final int ddStart = cdhStart - 16;
        assertNotEquals(ZipLong.DD_SIG, new ZipLong(data, ddStart));
        final long crcFromLFH = ZipLong.getValue(data, 14);
        final long cSizeFromLFH = ZipLong.getValue(data, 18);
        final long sizeFromLFH = ZipLong.getValue(data, 22);
        assertEquals(3, sizeFromLFH);

        final long crcFromCDH = ZipLong.getValue(data, cdhStart + 16);
        assertEquals(crcFromLFH, crcFromCDH);
        final long cSizeFromCDH = ZipLong.getValue(data, cdhStart + 20);
        assertEquals(cSizeFromLFH, cSizeFromCDH);
        final long sizeFromCDH = ZipLong.getValue(data, cdhStart + 24);
        assertEquals(sizeFromLFH, sizeFromCDH);
    }

    private int findCentralDirectory(final byte[] data) {
        // not a ZIP64 archive, no comment, "End of central directory record" at the end
        return (int) ZipLong.getValue(data, data.length - 22 + 16);
    }

    @BeforeEach
    public void setUp() throws Exception {
        dir = mkdir("ddtest");
    }

    @AfterEach
    public void tearDown() {
        rmdir(dir);
    }

    @Test
    public void writesDataDescriptorForDeflatedEntryOnUnseekableOutput() throws IOException {
        final ByteArrayOutputStream o = new ByteArrayOutputStream();
        try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(o)) {
            zos.putArchiveEntry(new ZipArchiveEntry("test1.txt"));
            zos.write("foo".getBytes(UTF_8));
            zos.closeArchiveEntry();
        }
        final byte[] data = o.toByteArray();

        final byte[] versionInLFH = Arrays.copyOfRange(data, 4, 6);
        // 2.0 because of DD
        assertArrayEquals(new byte[] { 20, 0 }, versionInLFH);
        final byte[] gpbInLFH = Arrays.copyOfRange(data, 6, 8);
        // DD and EFS flags
        assertArrayEquals(new byte[] { 8, 8 }, gpbInLFH);
        final byte[] crcAndSizedInLFH = Arrays.copyOfRange(data, 14, 26);
        assertArrayEquals(new byte[12], crcAndSizedInLFH);

        final int cdhStart = findCentralDirectory(data);
        final byte[] versionInCDH = Arrays.copyOfRange(data, cdhStart + 6, cdhStart + 8);
        assertArrayEquals(new byte[] { 20, 0 }, versionInCDH);
        final byte[] gpbInCDH = Arrays.copyOfRange(data, cdhStart + 8, cdhStart + 10);
        assertArrayEquals(new byte[] { 8, 8 }, gpbInCDH);

        final int ddStart = cdhStart - 16;
        assertEquals(ZipLong.DD_SIG, new ZipLong(data, ddStart));
        final long crcFromDD = ZipLong.getValue(data, ddStart + 4);
        final long cSizeFromDD = ZipLong.getValue(data, ddStart + 8);
        final long sizeFromDD = ZipLong.getValue(data, ddStart + 12);
        assertEquals(3, sizeFromDD);

        final long crcFromCDH = ZipLong.getValue(data, cdhStart + 16);
        assertEquals(crcFromDD, crcFromCDH);
        final long cSizeFromCDH = ZipLong.getValue(data, cdhStart + 20);
        assertEquals(cSizeFromDD, cSizeFromCDH);
        final long sizeFromCDH = ZipLong.getValue(data, cdhStart + 24);
        assertEquals(sizeFromDD, sizeFromCDH);
    }
}
