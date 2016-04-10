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

import java.math.BigInteger;
import java.util.zip.ZipException;

import org.junit.Test;

public class Zip64ExtendedInformationExtraFieldTest {

    private static final ZipEightByteInteger SIZE =
        new ZipEightByteInteger(0x12345678);
    private static final ZipEightByteInteger CSIZE =
        new ZipEightByteInteger(0x9ABCDEF);
    private static final ZipEightByteInteger OFF =
        new ZipEightByteInteger(BigInteger.valueOf(0xABCDEF091234567l)
                                .shiftLeft(4)
                                .setBit(3));
    private static final ZipLong DISK = new ZipLong(0x12);

    @Test
    public void testWriteCDOnlySizes() {
        final Zip64ExtendedInformationExtraField f =
            new Zip64ExtendedInformationExtraField(SIZE, CSIZE);
        assertEquals(new ZipShort(16), f.getCentralDirectoryLength());
        final byte[] b = f.getCentralDirectoryData();
        assertEquals(16, b.length);
        checkSizes(b);
    }

    @Test
    public void testWriteCDSizeAndOffset() {
        final Zip64ExtendedInformationExtraField f =
            new Zip64ExtendedInformationExtraField(SIZE, CSIZE, OFF, null);
        assertEquals(new ZipShort(24), f.getCentralDirectoryLength());
        final byte[] b = f.getCentralDirectoryData();
        assertEquals(24, b.length);
        checkSizes(b);
        checkOffset(b, 16);
    }

    @Test
    public void testWriteCDSizeOffsetAndDisk() {
        final Zip64ExtendedInformationExtraField f =
            new Zip64ExtendedInformationExtraField(SIZE, CSIZE, OFF, DISK);
        assertEquals(new ZipShort(28), f.getCentralDirectoryLength());
        final byte[] b = f.getCentralDirectoryData();
        assertEquals(28, b.length);
        checkSizes(b);
        checkOffset(b, 16);
        checkDisk(b, 24);
    }

    @Test
    public void testWriteCDSizeAndDisk() {
        final Zip64ExtendedInformationExtraField f =
            new Zip64ExtendedInformationExtraField(SIZE, CSIZE, null, DISK);
        assertEquals(new ZipShort(20), f.getCentralDirectoryLength());
        final byte[] b = f.getCentralDirectoryData();
        assertEquals(20, b.length);
        checkSizes(b);
        checkDisk(b, 16);
    }

    @Test
    public void testReadLFHSizesOnly() throws ZipException {
        final Zip64ExtendedInformationExtraField f =
            new Zip64ExtendedInformationExtraField();
        final byte[] b = new byte[16];
        System.arraycopy(SIZE.getBytes(), 0, b, 0, 8);
        System.arraycopy(CSIZE.getBytes(), 0, b, 8, 8);
        f.parseFromLocalFileData(b, 0, b.length);
        assertEquals(SIZE, f.getSize());
        assertEquals(CSIZE, f.getCompressedSize());
        assertNull(f.getRelativeHeaderOffset());
        assertNull(f.getDiskStartNumber());
    }

    @Test
    public void testReadLFHSizesAndOffset() throws ZipException {
        final Zip64ExtendedInformationExtraField f =
            new Zip64ExtendedInformationExtraField();
        final byte[] b = new byte[24];
        System.arraycopy(SIZE.getBytes(), 0, b, 0, 8);
        System.arraycopy(CSIZE.getBytes(), 0, b, 8, 8);
        System.arraycopy(OFF.getBytes(), 0, b, 16, 8);
        f.parseFromLocalFileData(b, 0, b.length);
        assertEquals(SIZE, f.getSize());
        assertEquals(CSIZE, f.getCompressedSize());
        assertEquals(OFF, f.getRelativeHeaderOffset());
        assertNull(f.getDiskStartNumber());
    }

    @Test
    public void testReadLFHSizesOffsetAndDisk() throws ZipException {
        final Zip64ExtendedInformationExtraField f =
            new Zip64ExtendedInformationExtraField();
        final byte[] b = new byte[28];
        System.arraycopy(SIZE.getBytes(), 0, b, 0, 8);
        System.arraycopy(CSIZE.getBytes(), 0, b, 8, 8);
        System.arraycopy(OFF.getBytes(), 0, b, 16, 8);
        System.arraycopy(DISK.getBytes(), 0, b, 24, 4);
        f.parseFromLocalFileData(b, 0, b.length);
        assertEquals(SIZE, f.getSize());
        assertEquals(CSIZE, f.getCompressedSize());
        assertEquals(OFF, f.getRelativeHeaderOffset());
        assertEquals(DISK, f.getDiskStartNumber());
    }

    @Test
    public void testReadLFHSizesAndDisk() throws ZipException {
        final Zip64ExtendedInformationExtraField f =
            new Zip64ExtendedInformationExtraField();
        final byte[] b = new byte[20];
        System.arraycopy(SIZE.getBytes(), 0, b, 0, 8);
        System.arraycopy(CSIZE.getBytes(), 0, b, 8, 8);
        System.arraycopy(DISK.getBytes(), 0, b, 16, 4);
        f.parseFromLocalFileData(b, 0, b.length);
        assertEquals(SIZE, f.getSize());
        assertEquals(CSIZE, f.getCompressedSize());
        assertNull(f.getRelativeHeaderOffset());
        assertEquals(DISK, f.getDiskStartNumber());
    }

    @Test
    public void testReadCDSizesOffsetAndDisk() throws ZipException {
        final Zip64ExtendedInformationExtraField f =
            new Zip64ExtendedInformationExtraField();
        final byte[] b = new byte[28];
        System.arraycopy(SIZE.getBytes(), 0, b, 0, 8);
        System.arraycopy(CSIZE.getBytes(), 0, b, 8, 8);
        System.arraycopy(OFF.getBytes(), 0, b, 16, 8);
        System.arraycopy(DISK.getBytes(), 0, b, 24, 4);
        f.parseFromCentralDirectoryData(b, 0, b.length);
        assertEquals(SIZE, f.getSize());
        assertEquals(CSIZE, f.getCompressedSize());
        assertEquals(OFF, f.getRelativeHeaderOffset());
        assertEquals(DISK, f.getDiskStartNumber());
    }

    @Test
    public void testReadCDSizesAndOffset() throws ZipException {
        final Zip64ExtendedInformationExtraField f =
            new Zip64ExtendedInformationExtraField();
        final byte[] b = new byte[24];
        System.arraycopy(SIZE.getBytes(), 0, b, 0, 8);
        System.arraycopy(CSIZE.getBytes(), 0, b, 8, 8);
        System.arraycopy(OFF.getBytes(), 0, b, 16, 8);
        f.parseFromCentralDirectoryData(b, 0, b.length);
        assertEquals(SIZE, f.getSize());
        assertEquals(CSIZE, f.getCompressedSize());
        assertEquals(OFF, f.getRelativeHeaderOffset());
        assertNull(f.getDiskStartNumber());
    }

    @Test
    public void testReadCDSomethingAndDisk() throws ZipException {
        final Zip64ExtendedInformationExtraField f =
            new Zip64ExtendedInformationExtraField();
        final byte[] b = new byte[12];
        System.arraycopy(SIZE.getBytes(), 0, b, 0, 8);
        System.arraycopy(DISK.getBytes(), 0, b, 8, 4);
        f.parseFromCentralDirectoryData(b, 0, b.length);
        assertNull(f.getSize());
        assertNull(f.getCompressedSize());
        assertNull(f.getRelativeHeaderOffset());
        assertEquals(DISK, f.getDiskStartNumber());
    }

    @Test
    public void testReparseCDSingleEightByteData() throws ZipException {
        final Zip64ExtendedInformationExtraField f =
            new Zip64ExtendedInformationExtraField();
        final byte[] b = new byte[8];
        System.arraycopy(SIZE.getBytes(), 0, b, 0, 8);
        f.parseFromCentralDirectoryData(b, 0, b.length);
        f.reparseCentralDirectoryData(true, false, false, false);
        assertEquals(SIZE, f.getSize());
        assertNull(f.getCompressedSize());
        assertNull(f.getRelativeHeaderOffset());
        assertNull(f.getDiskStartNumber());
        f.setSize(null);
        f.reparseCentralDirectoryData(false, true, false, false);
        assertNull(f.getSize());
        assertEquals(SIZE, f.getCompressedSize());
        assertNull(f.getRelativeHeaderOffset());
        assertNull(f.getDiskStartNumber());
        f.setCompressedSize(null);
        f.reparseCentralDirectoryData(false, false, true, false);
        assertNull(f.getSize());
        assertNull(f.getCompressedSize());
        assertEquals(SIZE, f.getRelativeHeaderOffset());
        assertNull(f.getDiskStartNumber());
    }

    private static void checkSizes(final byte[] b) {
        assertEquals(0x78, b[0]);
        assertEquals(0x56, b[1]);
        assertEquals(0x34, b[2]);
        assertEquals(0x12, b[3]);
        assertEquals(0x00, b[4]);
        assertEquals(0x00, b[5]);
        assertEquals(0x00, b[6]);
        assertEquals(0x00, b[7]);
        assertEquals((byte) 0xEF, b[8]);
        assertEquals((byte) 0xCD, b[9]);
        assertEquals((byte) 0xAB, b[10]);
        assertEquals(0x09, b[11]);
        assertEquals(0x00, b[12]);
        assertEquals(0x00, b[13]);
        assertEquals(0x00, b[14]);
        assertEquals(0x00, b[15]);
    }

    private static void checkOffset(final byte[] b, final int off) {
        assertEquals(0x78, b[0 + off]);
        assertEquals(0x56, b[1 + off]);
        assertEquals(0x34, b[2 + off]);
        assertEquals(0x12, b[3 + off]);
        assertEquals((byte) 0x09, b[4 + off]);
        assertEquals((byte) 0xEF, b[5 + off]);
        assertEquals((byte) 0xCD, b[6 + off]);
        assertEquals((byte) 0xAB, b[7 + off]);
    }

    private static void checkDisk(final byte[] b, final int off) {
        assertEquals(0x12, b[0 + off]);
        assertEquals(0x00, b[1 + off]);
        assertEquals(0x00, b[2 + off]);
        assertEquals(0x00, b[3 + off]);
    }
}