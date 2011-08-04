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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.URL;
import java.util.Enumeration;
import java.util.Random;

import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;

public class Zip64SupportTest {

    private static final long FIVE_BILLION = 5000000000l;
    private static final int ONE_MILLION = 1000000;
    private static final int ONE_HUNDRED_THOUSAND = 100000;

    @Ignore
    @Test public void read5GBOfZerosUsingInputStream() throws Throwable {
        read5GBOfZerosImpl(get5GBZerosFile(), "5GB_of_Zeros");
    }

    @Ignore
    @Test public void read5GBOfZerosGeneratedBy7ZIPUsingInputStream()
        throws Throwable {
        read5GBOfZerosImpl(get5GBZerosFileGeneratedBy7ZIP(), "5GB_of_Zeros");
    }

    @Ignore
    @Test public void read5GBOfZerosGeneratedByJava7JarUsingInputStream()
        throws Throwable {
        read5GBOfZerosImpl(get5GBZerosFileGeneratedByJava7Jar(), "5GB_of_Zeros");
    }

    @Ignore
    @Test public void read100KFilesUsingInputStream() throws Throwable {
        read100KFilesImpl(get100KFileFile());
    }

    @Ignore
    @Test public void read100KFilesGeneratedBy7ZIPUsingInputStream() throws Throwable {
        read100KFilesImpl(get100KFileFileGeneratedBy7ZIP());
    }

    @Ignore
    @Test public void read100KFilesGeneratedByWinCFUsingInputStream() throws Throwable {
        read100KFilesImpl(get100KFileFileGeneratedByWinCF());
    }

    @Ignore
    @Test public void read100KFilesGeneratedByJava7JarUsingInputStream() throws Throwable {
        read100KFilesImpl(get100KFileFileGeneratedByJava7Jar());
    }

    @Test public void read100KFilesUsingZipFile() throws Throwable {
        read100KFilesUsingZipFileImpl(get100KFileFile());
    }

    @Ignore
    @Test public void read100KFilesGeneratedBy7ZIPUsingZipFile() throws Throwable {
        read100KFilesUsingZipFileImpl(get100KFileFileGeneratedBy7ZIP());
    }

    @Ignore
    @Test public void read100KFilesGeneratedByWinCFUsingZipFile() throws Throwable {
        read100KFilesUsingZipFileImpl(get100KFileFileGeneratedByWinCF());
    }

    @Ignore
    @Test public void read100KFilesGeneratedByJava7JarUsingZipFile() throws Throwable {
        read100KFilesUsingZipFileImpl(get100KFileFileGeneratedByJava7Jar());
    }

    private static final ZipOutputTest write100KFiles =
        new ZipOutputTest() {
            public void test(File f, ZipArchiveOutputStream zos)
                throws IOException {
                write100KFilesToStream(zos);
                RandomAccessFile a = new RandomAccessFile(f, "r");
                try {
                    final long end = a.length();

                    // validate "end of central directory" is at
                    // the end of the file and contains the magic
                    // value 0xFFFF as "number of entries".
                    a.seek(end
                           - 22 /* length of EOCD without file comment */);
                    byte[] eocd = new byte[12];
                    a.readFully(eocd);
                    assertArrayEquals(new byte[] {
                            // sig
                            (byte) 0x50, (byte) 0x4b, 5, 6,
                            // disk numbers
                            0, 0, 0, 0,
                            // entries
                            (byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff,
                        }, eocd); 

                    // validate "Zip64 end of central directory
                    // locator" is right in front of the EOCD and
                    // the location of the "Zip64 end of central
                    // directory record" seems correct
                    long expectedZ64EocdOffset = end - 22 /* eocd.length */
                        - 20 /* z64 eocd locator.length */
                        - 56 /* z64 eocd without extensible data sector */;
                    byte[] loc =
                        ZipEightByteInteger.getBytes(expectedZ64EocdOffset);
                    a.seek(end - 22 - 20);
                    byte[] z64EocdLoc = new byte[20];
                    a.readFully(z64EocdLoc);
                    assertArrayEquals(new byte[] {
                            // sig
                            (byte) 0x50, (byte) 0x4b, 6, 7,
                            // disk numbers
                            0, 0, 0, 0,
                            // location of Zip64 EOCD,
                            loc[0], loc[1], loc[2], loc[3],
                            loc[4], loc[5], loc[6], loc[7],
                            // total number of disks
                            1, 0, 0, 0,
                        }, z64EocdLoc);

                    // validate "Zip64 end of central directory
                    // record" is where it is supposed to be, the
                    // known values are fine and read the location
                    // of the central directory from it
                    a.seek(expectedZ64EocdOffset);
                    byte[] z64EocdStart = new byte[40];
                    a.readFully(z64EocdStart);
                    assertArrayEquals(new byte[] {
                            // sig
                            (byte) 0x50, (byte) 0x4b, 6, 6,
                            // size of z64 EOCD
                            44, 0, 0, 0,
                            0, 0, 0, 0,
                            // version made by
                            45, 0,
                            // version needed to extract
                            45, 0,
                            // disk numbers
                            0, 0, 0, 0,
                            0, 0, 0, 0,
                            // number of entries 100k = 0x186A0
                            (byte) 0xA0, (byte) 0x86, 1, 0,
                            0, 0, 0, 0,
                            (byte) 0xA0, (byte) 0x86, 1, 0,
                            0, 0, 0, 0,
                        }, z64EocdStart);
                    a.seek(expectedZ64EocdOffset + 48 /* skip size */);
                    byte[] cdOffset = new byte[8];
                    a.readFully(cdOffset);
                    long cdLoc = ZipEightByteInteger.getLongValue(cdOffset);

                    // finally verify there really is a central
                    // directory entry where the Zip64 EOCD claims
                    a.seek(cdLoc);
                    byte[] sig = new byte[4];
                    a.readFully(sig);
                    assertArrayEquals(new byte[] {
                            (byte) 0x50, (byte) 0x4b, 1, 2,
                        }, sig);
                } finally {
                    a.close();
                }
            }
        };

    @Ignore
    @Test public void write100KFilesFile() throws Throwable {
        withTemporaryArchive("write100KFilesFile", write100KFiles, true);
    }

    @Ignore
    @Test public void write100KFilesStream() throws Throwable {
        withTemporaryArchive("write100KFilesStream", write100KFiles, false);
    }

    @Test public void readSelfGenerated100KFilesUsingZipFile()
        throws Throwable {
        withTemporaryArchive("readSelfGenerated100KFilesUsingZipFile()",
                             new ZipOutputTest() {
                                 public void test(File f,
                                                  ZipArchiveOutputStream zos)
                                     throws IOException {
                                     write100KFilesToStream(zos);
                                     read100KFilesUsingZipFileImpl(f);
                                 }
                             },
                             true);
    }

    /*
     * Individual sizes don't require ZIP64 but the offset of the
     * third entry is bigger than 0xFFFFFFFF so a ZIP64 extended
     * information is needed inside the central directory.
     *
     * Creates a temporary archive of approx 5GB in size
     */
    private static final ZipOutputTest write3EntriesCreatingBigArchive =
        new ZipOutputTest() {
            public void test(File f, ZipArchiveOutputStream zos)
                throws IOException {
                write3EntriesCreatingBigArchiveToStream(zos);

                RandomAccessFile a = new RandomAccessFile(f, "r");
                try {
                    final long end = getLengthAndPositionAtCentralDirectory(a);
                    // skip first two entries
                    a.skipBytes(2 * 47 /* CD entry of file with
                                          file name length 1 and no
                                          extra data */);

                    // grab third entry, verify offset is
                    // 0xFFFFFFFF and it has a ZIP64 extended
                    // information extra field
                    byte[] header = new byte[12];
                    a.readFully(header);
                    assertArrayEquals(new byte[] {
                            // sig
                            (byte) 0x50, (byte) 0x4b, 1, 2,
                            // version made by
                            45, 0,
                            // version needed to extract
                            45, 0,
                            // GPB (EFS bit)
                            0, 8,
                            // method
                            0, 0
                        }, header);
                    // ignore timestamp, CRC, compressed size
                    a.skipBytes(12);
                    byte[] rest = new byte[23];
                    a.readFully(rest);
                    assertArrayEquals(new byte[] {
                            // Original Size
                            1, 0, 0, 0,
                            // file name length
                            1, 0,
                            // extra field length
                            12, 0,
                            // comment length
                            0, 0,
                            // disk number
                            0, 0,
                            // attributes
                            0, 0,
                            0, 0, 0, 0,
                            // offset
                            (byte) 0xFF, (byte) 0xFF,
                            (byte) 0xFF, (byte) 0xFF,
                            // file name
                            (byte) '2'
                        }, rest);
                    byte[] extra = new byte[4];
                    a.readFully(extra);
                    assertArrayEquals(new byte[] {
                            // Header-ID
                            1, 0,
                            // size
                            8, 0
                        }, extra);

                    // read offset of LFH
                    byte[] offset = new byte[8];
                    a.readFully(offset);
                    // verify there is a LFH where the CD claims it
                    a.seek(ZipEightByteInteger.getLongValue(offset));
                    byte[] sig = new byte[4];
                    a.readFully(sig);
                    assertArrayEquals(new byte[] {
                            (byte) 0x50, (byte) 0x4b, 3, 4,
                        }, sig);
                } finally {
                    a.close();
                }
            }
        };

    @Ignore
    @Test public void write3EntriesCreatingBigArchiveFile() throws Throwable {
        withTemporaryArchive("write3EntriesCreatingBigArchiveFile",
                             write3EntriesCreatingBigArchive,
                             true);
    }

    @Test public void write3EntriesCreatingBigArchiveStream() throws Throwable {
        withTemporaryArchive("write3EntriesCreatingBigArchiveStream",
                             write3EntriesCreatingBigArchive,
                             false);
    }

    @Test public void read3EntriesCreatingBigArchiveFileUsingZipFile()
        throws Throwable {
        withTemporaryArchive("read3EntriesCreatingBigArchiveFileUsingZipFile",
                             new ZipOutputTest() {
                                 public void test(File f,
                                                  ZipArchiveOutputStream zos)
                                     throws IOException {
                                     write3EntriesCreatingBigArchiveToStream(zos);
                                     ZipFile zf = null;
                                     try {
                                         zf = new ZipFile(f);
                                         int idx = 0;
                                         for (Enumeration e =
                                                  zf.getEntriesInPhysicalOrder();
                                              e.hasMoreElements(); ) {
                                             ZipArchiveEntry zae =
                                                 (ZipArchiveEntry)
                                                 e.nextElement();
                                             assertEquals(String.valueOf(idx),
                                                          zae.getName());
                                             if (idx++ < 2) {
                                                 assertEquals(FIVE_BILLION / 2,
                                                              zae.getSize());
                                             } else {
                                                 assertEquals(1,
                                                              zae.getSize());
                                                 InputStream i =
                                                     zf.getInputStream(zae);
                                                 try {
                                                     assertNotNull(i);
                                                     assertEquals(42, i.read());
                                                 } finally {
                                                     i.close();
                                                 }
                                             }
                                         }
                                     } finally {
                                         ZipFile.closeQuietly(zf);
                                     }
                                 }
                             },
                             true);
    }

    /*
     * One entry of length 5 billion bytes, written without
     * compression.
     *
     * No Compression => sizes are stored directly inside the LFH.  No
     * Data Descriptor at all.
     *
     * Creates a temporary archive of approx 5GB in size
     */
    private static ZipOutputTest writeBigStoredEntry(final boolean knownSize) {
        return new ZipOutputTest() {
            public void test(File f, ZipArchiveOutputStream zos)
                throws IOException {
                byte[] buf = new byte[ONE_MILLION];
                ZipArchiveEntry zae = new ZipArchiveEntry("0");
                if (knownSize) {
                    zae.setSize(FIVE_BILLION);
                    zae.setCrc(0x5c316f50L);
                }
                zae.setMethod(ZipArchiveEntry.STORED);
                zos.putArchiveEntry(zae);
                for (int j = 0; j < FIVE_BILLION / 1000 / 1000; j++) {
                    zos.write(buf);
                }
                zos.closeArchiveEntry();
                zos.close();

                RandomAccessFile a = new RandomAccessFile(f, "r");
                try {
                    final long end = getLengthAndPositionAtCentralDirectory(a);

                    // grab first entry, verify sizes are 0xFFFFFFFF
                    // and it has a ZIP64 extended information extra
                    // field
                    byte[] header = new byte[12];
                    a.readFully(header);
                    assertArrayEquals(new byte[] {
                            // sig
                            (byte) 0x50, (byte) 0x4b, 1, 2,
                            // version made by
                            45, 0,
                            // version needed to extract
                            45, 0,
                            // GPB (EFS bit)
                            0, 8,
                            // method
                            0, 0
                        }, header);
                    // ignore timestamp
                    a.skipBytes(4);
                    byte[] rest = new byte[31];
                    a.readFully(rest);
                    assertArrayEquals(new byte[] {
                            // CRC
                            (byte) 0x50, (byte) 0x6F, (byte) 0x31, (byte) 0x5c,
                            // Compressed Size
                            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                            // Original Size
                            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                            // file name length
                            1, 0,
                            // extra field length
                            20, 0,
                            // comment length
                            0, 0,
                            // disk number
                            0, 0,
                            // attributes
                            0, 0,
                            0, 0, 0, 0,
                            // offset
                            0, 0, 0, 0,
                            // file name
                            (byte) '0'
                        }, rest);
                    byte[] extra = new byte[20];
                    a.readFully(extra);
                    // 5e9 == 0x12A05F200
                    assertArrayEquals(new byte[] {
                            // Header-ID
                            1, 0,
                            // size of extra
                            16, 0,
                            // original size
                            0, (byte) 0xF2, 5, (byte) 0x2A,
                            1, 0, 0, 0,
                            // compressed size
                            0, (byte) 0xF2, 5, (byte) 0x2A,
                            1, 0, 0, 0,
                        }, extra);

                    // and now validate local file header
                    a.seek(0);
                    header = new byte[10];
                    a.readFully(header);
                    assertArrayEquals(new byte[] {
                            // sig
                            (byte) 0x50, (byte) 0x4b, 3, 4,
                            // version needed to extract
                            45, 0,
                            // GPB (EFS bit)
                            0, 8,
                            // method
                            0, 0
                        }, header);
                    // ignore timestamp
                    a.skipBytes(4);
                    rest = new byte[17];
                    a.readFully(rest);
                    assertArrayEquals(new byte[] {
                            // CRC
                            (byte) 0x50, (byte) 0x6F, (byte) 0x31, (byte) 0x5c,
                            // Compressed Size
                            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                            // Original Size
                            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                            // file name length
                            1, 0,
                            // extra field length
                            20, 0,
                            // file name
                            (byte) '0'
                        }, rest);
                    a.readFully(extra);
                    // 5e9 == 0x12A05F200
                    assertArrayEquals(new byte[] {
                            // Header-ID
                            1, 0,
                            // size of extra
                            16, 0,
                            // original size
                            0, (byte) 0xF2, 5, (byte) 0x2A,
                            1, 0, 0, 0,
                            // compressed size
                            0, (byte) 0xF2, 5, (byte) 0x2A,
                            1, 0, 0, 0,
                        }, extra);
                } finally {
                    a.close();
                }
            }
        };
    }

    /*
     * No Compression + Stream => sizes must be known before data is
     * written.
     */
    @Ignore
    @Test public void writeBigStoredEntryToStream() throws Throwable {
        withTemporaryArchive("writeBigStoredEntryToStream",
                             writeBigStoredEntry(true),
                             false);
    }

    @Ignore
    @Test public void writeBigStoredEntryKnownSizeToFile() throws Throwable {
        withTemporaryArchive("writeBigStoredEntryKnownSizeToFile",
                             writeBigStoredEntry(true),
                             true);
    }

    @Ignore
    @Test public void writeBigStoredEntryUnnownSizeToFile() throws Throwable {
        withTemporaryArchive("writeBigStoredEntryUnknownSizeToFile",
                             writeBigStoredEntry(false),
                             true);
    }

    /*
     * One entry of length 5 billion bytes, written with
     * compression to a stream.
     *
     * Compression + Stream => sizes are set to 0 in LFH and ZIP64
     * entry, real values are inside the data descriptor.
     *
     * Creates a temporary archive of approx 4MB in size
     */
    @Ignore
    @Test public void writeBigDeflatedEntryKnownSizeToStream()
        throws Throwable {
        withTemporaryArchive("writeBigDeflatedEntryKnownSizeToStream",
                             new ZipOutputTest() {
                                 public void test(File f,
                                                  ZipArchiveOutputStream zos)
                                     throws IOException {
                                     byte[] buf = new byte[ONE_MILLION];
                                     ZipArchiveEntry zae =
                                         new ZipArchiveEntry("0");
                                     zae.setSize(FIVE_BILLION);
                                     zae.setMethod(ZipArchiveEntry.DEFLATED);
                                     zos.putArchiveEntry(zae);
                                     for (int j = 0;
                                          j < FIVE_BILLION / 1000 / 1000;
                                          j++) {
                                         zos.write(buf);
                                     }
                                     zos.closeArchiveEntry();
                                     zos.close();

                                     RandomAccessFile a =
                                         new RandomAccessFile(f, "r");
                                     try {
                                         final long end =
                                             getLengthAndPositionAtCentralDirectory(a);

                                         long cfhPos = a.getFilePointer();
                                         // grab first entry, verify
                                         // sizes are 0xFFFFFFFF and
                                         // it has a ZIP64 extended
                                         // information extra field
                                         byte[] header = new byte[12];
                                         a.readFully(header);
                                         assertArrayEquals(new byte[] {
                                                 // sig
                                                 (byte) 0x50, (byte) 0x4b, 1, 2,
                                                 // version made by
                                                 45, 0,
                                                 // version needed to extract
                                                 45, 0,
                                                 // GPB (EFS + Data Descriptor)
                                                 8, 8,
                                                 // method
                                                 8, 0,
                                             }, header);
                                         // ignore timestamp
                                         a.skipBytes(4);
                                         byte[] rest = new byte[31];
                                         a.readFully(rest);
                                         assertArrayEquals(new byte[] {
                                                 // CRC
                                                 (byte) 0x50, (byte) 0x6F,
                                                 (byte) 0x31, (byte) 0x5c,
                                                 // Compressed Size
                                                 (byte) 0xFF, (byte) 0xFF,
                                                 (byte) 0xFF, (byte) 0xFF,
                                                 // Original Size
                                                 (byte) 0xFF, (byte) 0xFF,
                                                 (byte) 0xFF, (byte) 0xFF,
                                                 // file name length
                                                 1, 0,
                                                 // extra field length
                                                 20, 0,
                                                 // comment length
                                                 0, 0,
                                                 // disk number
                                                 0, 0,
                                                 // attributes
                                                 0, 0,
                                                 0, 0, 0, 0,
                                                 // offset
                                                 0, 0, 0, 0,
                                                 // file name
                                                 (byte) '0'
                                             }, rest);
                                         byte[] extra = new byte[20];
                                         a.readFully(extra);
                                         // 5e9 == 0x12A05F200
                                         assertArrayEquals(new byte[] {
                                                 // Header-ID
                                                 1, 0,
                                                 // size of extra
                                                 16, 0,
                                                 // original size
                                                 0, (byte) 0xF2, 5, (byte) 0x2A,
                                                 1, 0, 0, 0,
                                                 // compressed size
                                                 (byte) 0x68, (byte) 0x27,
                                                 (byte) 0x4A, 0,
                                                 0, 0, 0, 0,
                                             }, extra);

                                         // validate data descriptor
                                         a.seek(cfhPos - 24);
                                         byte[] dd = new byte[8];
                                         a.readFully(dd);
                                         assertArrayEquals(new byte[] {
                                                 // sig
                                                 (byte) 0x50, (byte) 0x4b, 7, 8,
                                                 // CRC
                                                 (byte) 0x50, (byte) 0x6F,
                                                 (byte) 0x31, (byte) 0x5c,
                                             }, dd);
                                         dd = new byte[16];
                                         a.readFully(dd);
                                         assertArrayEquals(new byte[] {
                                                 // compressed size
                                                 (byte) 0x68, (byte) 0x27,
                                                 (byte) 0x4A, 0,
                                                 0, 0, 0, 0,
                                                 // original size
                                                 0, (byte) 0xF2, 5, (byte) 0x2A,
                                                 1, 0, 0, 0,
                                             }, dd);

                                         // and now validate local file header
                                         a.seek(0);
                                         header = new byte[10];
                                         a.readFully(header);
                                         assertArrayEquals(new byte[] {
                                                 // sig
                                                 (byte) 0x50, (byte) 0x4b, 3, 4,
                                                 // version needed to extract
                                                 45, 0,
                                                 // GPB (EFS + Data Descriptor)
                                                 8, 8,
                                                 // method
                                                 8, 0,
                                             }, header);
                                         // ignore timestamp
                                         a.skipBytes(4);
                                         rest = new byte[17];
                                         a.readFully(rest);
                                         assertArrayEquals(new byte[] {
                                                 // CRC
                                                 0, 0, 0, 0,
                                                 // Compressed Size
                                                 0, 0, 0, 0,
                                                 // Original Size
                                                 0, 0, 0, 0,
                                                 // file name length
                                                 1, 0,
                                                 // extra field length
                                                 20, 0,
                                                 // file name
                                                 (byte) '0'
                                             }, rest);
                                         a.readFully(extra);
                                         assertArrayEquals(new byte[] {
                                                 // Header-ID
                                                 1, 0,
                                                 // size of extra
                                                 16, 0,
                                                 // original size
                                                 0, 0, 0, 0,
                                                 0, 0, 0, 0,
                                                 // compressed size
                                                 0, 0, 0, 0,
                                                 0, 0, 0, 0,
                                             }, extra);
                                     } finally {
                                         a.close();
                                     }
                                 }
                             },
                             false);
    }

    /*
     * One entry of length 5 billion bytes, written with
     * compression to a file.
     *
     * Writing to a file => sizes are stored directly inside the LFH.
     * No Data Descriptor at all.
     *
     * Creates a temporary archive of approx 4MB in size
     */
    private static ZipOutputTest writeBigDeflatedEntryToFile(final boolean knownSize) {
        return new ZipOutputTest() {
            public void test(File f, ZipArchiveOutputStream zos)
                throws IOException {
                byte[] buf = new byte[ONE_MILLION];
                ZipArchiveEntry zae = new ZipArchiveEntry("0");
                if (knownSize) {
                    zae.setSize(FIVE_BILLION);
                }
                zae.setMethod(ZipArchiveEntry.DEFLATED);
                zos.putArchiveEntry(zae);
                for (int j = 0;
                     j < FIVE_BILLION / 1000 / 1000;
                     j++) {
                    zos.write(buf);
                }
                zos.closeArchiveEntry();
                zos.close();

                RandomAccessFile a = new RandomAccessFile(f, "r");
                try {
                    final long end = getLengthAndPositionAtCentralDirectory(a);

                    long cfhPos = a.getFilePointer();
                    // grab first entry, verify
                    // sizes are 0xFFFFFFFF and
                    // it has a ZIP64 extended
                    // information extra field
                    byte[] header = new byte[12];
                    a.readFully(header);
                    assertArrayEquals(new byte[] {
                            // sig
                            (byte) 0x50, (byte) 0x4b, 1, 2,
                            // version made by
                            45, 0,
                            // version needed to extract
                            45, 0,
                            // GPB (EFS + *no* Data Descriptor)
                            0, 8,
                            // method
                            8, 0,
                        }, header);
                    // ignore timestamp
                    a.skipBytes(4);
                    byte[] rest = new byte[31];
                    a.readFully(rest);
                    assertArrayEquals(new byte[] {
                            // CRC
                            (byte) 0x50, (byte) 0x6F, (byte) 0x31, (byte) 0x5c,
                            // Compressed Size
                            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                            // Original Size
                            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                            // file name length
                            1, 0,
                            // extra field length
                            20, 0,
                            // comment length
                            0, 0,
                            // disk number
                            0, 0,
                            // attributes
                            0, 0,
                            0, 0, 0, 0,
                            // offset
                            0, 0, 0, 0,
                            // file name
                            (byte) '0'
                        }, rest);
                    byte[] extra = new byte[20];
                    a.readFully(extra);
                    // 5e9 == 0x12A05F200
                    assertArrayEquals(new byte[] {
                            // Header-ID
                            1, 0,
                            // size of extra
                            16, 0,
                            // original size
                            0, (byte) 0xF2, 5, (byte) 0x2A,
                            1, 0, 0, 0,
                            // compressed size
                            (byte) 0x68, (byte) 0x27, (byte) 0x4A, 0,
                            0, 0, 0, 0,
                        }, extra);

                    // and now validate local file header
                    a.seek(0);
                    header = new byte[10];
                    a.readFully(header);
                    assertArrayEquals(new byte[] {
                            // sig
                            (byte) 0x50, (byte) 0x4b, 3, 4,
                            // version needed to extract
                            45, 0,
                            // GPB (EFS bit, no DD)
                            0, 8,
                            // method
                            8, 0,
                        }, header);
                    // ignore timestamp
                    a.skipBytes(4);
                    rest = new byte[17];
                    a.readFully(rest);
                    assertArrayEquals(new byte[] {
                            // CRC
                            (byte) 0x50, (byte) 0x6F, (byte) 0x31, (byte) 0x5c,
                            // Compressed Size
                            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                            // Original Size
                            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                            // file name length
                            1, 0,
                            // extra field length
                            20, 0,
                            // file name
                            (byte) '0'
                        }, rest);
                    extra = new byte[20];
                    a.readFully(extra);
                    assertArrayEquals(new byte[] {
                            // Header-ID
                            1, 0,
                            // size of extra
                            16, 0,
                            // original size
                            0, (byte) 0xF2, 5, (byte) 0x2A,
                            1, 0, 0, 0,
                            // compressed size
                            (byte) 0x68, (byte) 0x27, (byte) 0x4A, 0,
                            0, 0, 0, 0,
                        }, extra);
                } finally {
                    a.close();
                }
            }
        };
    }

    @Ignore
    @Test public void writeBigDeflatedEntryKnownSizeToFile()
        throws Throwable {
        withTemporaryArchive("writeBigDeflatedEntryKnownSizeToFile",
                             writeBigDeflatedEntryToFile(true),
                             true);
    }

    @Ignore
    @Test public void writeBigDeflatedEntryUnknownSizeToFile()
        throws Throwable {
        withTemporaryArchive("writeBigDeflatedEntryUnknownSizeToFile",
                             writeBigDeflatedEntryToFile(false),
                             true);
    }

    /*
     * One entry of length 1 million bytes, written without compression.
     *
     * No Compression => sizes are stored directly inside the LFH.  No
     * Data Descriptor at all.  Shouldn't contain any ZIP64 extra
     * field if size was known.
     *
     * Creates a temporary archive of approx 1MB in size
     */
    private static ZipOutputTest writeSmallStoredEntry(final boolean knownSize) {
        return new ZipOutputTest() {
            public void test(File f, ZipArchiveOutputStream zos)
                throws IOException {
                byte[] buf = new byte[ONE_MILLION];
                ZipArchiveEntry zae = new ZipArchiveEntry("0");
                if (knownSize) {
                    zae.setSize(ONE_MILLION);
                    zae.setCrc(0x1279CB9EL);
                }
                zae.setMethod(ZipArchiveEntry.STORED);
                zos.putArchiveEntry(zae);
                zos.write(buf);
                zos.closeArchiveEntry();
                zos.close();

                RandomAccessFile a = new RandomAccessFile(f, "r");
                try {
                    final long end = getLengthAndPositionAtCentralDirectory(a);

                    // grab first CF entry, verify sizes are 1e6 and it
                    // has no ZIP64 extended information extra field
                    // at all
                    byte[] header = new byte[12];
                    a.readFully(header);
                    assertArrayEquals(new byte[] {
                            // sig
                            (byte) 0x50, (byte) 0x4b, 1, 2,
                            // version made by
                            20, 0,
                            // version needed to extract
                            10, 0,
                            // GPB (EFS bit)
                            0, 8,
                            // method
                            0, 0
                        }, header);
                    // ignore timestamp
                    a.skipBytes(4);
                    byte[] rest = new byte[31];
                    a.readFully(rest);
                    // 1e6 == 0xF4240
                    assertArrayEquals(new byte[] {
                            // CRC
                            (byte) 0x9E, (byte) 0xCB, (byte) 0x79, (byte) 0x12,
                            // Compressed Size
                            (byte) 0x40, (byte) 0x42, (byte) 0x0F, 0,
                            // Original Size
                            (byte) 0x40, (byte) 0x42, (byte) 0x0F, 0,
                            // file name length
                            1, 0,
                            // extra field length
                            0, 0,
                            // comment length
                            0, 0,
                            // disk number
                            0, 0,
                            // attributes
                            0, 0,
                            0, 0, 0, 0,
                            // offset
                            0, 0, 0, 0,
                            // file name
                            (byte) '0'
                        }, rest);

                    // and now validate local file header: this one
                    // has a ZIP64 extra field if and only if size was
                    // unknown
                    a.seek(0);
                    header = new byte[10];
                    a.readFully(header);
                    assertArrayEquals(new byte[] {
                            // sig
                            (byte) 0x50, (byte) 0x4b, 3, 4,
                            // version needed to extract
                            10, 0,
                            // GPB (EFS bit)
                            0, 8,
                            // method
                            0, 0
                        }, header);
                    // ignore timestamp
                    a.skipBytes(4);
                    rest = new byte[17];
                    a.readFully(rest);
                    // 1e6 == 0xF4240
                    assertArrayEquals(new byte[] {
                            // CRC
                            (byte) 0x9E, (byte) 0xCB, (byte) 0x79, (byte) 0x12,
                            // Compressed Size
                            (byte) 0x40, (byte) 0x42, (byte) 0x0F, 0,
                            // Original Size
                            (byte) 0x40, (byte) 0x42, (byte) 0x0F, 0,
                            // file name length
                            1, 0,
                            // extra field length
                            (byte) (knownSize ? 0 : 20), 0,
                            // file name
                            (byte) '0'
                        }, rest);
                    if (!knownSize) {
                        byte[] extra = new byte[20];
                        a.readFully(extra);
                        assertArrayEquals(new byte[] {
                                // Header-ID
                                1, 0,
                                // size of extra
                                16, 0,
                                // original size
                                (byte) 0x40, (byte) 0x42, (byte) 0x0F, 0,
                                0, 0, 0, 0,
                                // compressed size
                                (byte) 0x40, (byte) 0x42, (byte) 0x0F, 0,
                                0, 0, 0, 0,
                            }, extra);
                    }
                } finally {
                    a.close();
                }
            }
        };
    }

    @Test public void writeSmallStoredEntryToStream() throws Throwable {
        withTemporaryArchive("writeSmallStoredEntryToStream",
                             writeSmallStoredEntry(true),
                             false);
    }

    @Test public void writeSmallStoredEntryKnownSizeToFile() throws Throwable {
        withTemporaryArchive("writeSmallStoredEntryKnownSizeToFile",
                             writeSmallStoredEntry(true),
                             true);
    }

    @Test public void writeSmallStoredEntryUnnownSizeToFile() throws Throwable {
        withTemporaryArchive("writeSmallStoredEntryUnknownSizeToFile",
                             writeSmallStoredEntry(false),
                             true);
    }

    /*
     * One entry of length 1 million bytes, written with compression
     * to a stream.
     *
     * Compression + Stream => sizes are set to 0 in LFH, real values
     * are inside the data descriptor.  No ZIP64 extra field at all.
     */
    @Test public void writeSmallDeflatedEntryKnownSizeToStream()
        throws Throwable {
        withTemporaryArchive("writeSmallDeflatedEntryKnownSizeToStream",
                             new ZipOutputTest() {
                                 public void test(File f,
                                                  ZipArchiveOutputStream zos)
                                     throws IOException {
                                     byte[] buf = new byte[ONE_MILLION];
                                     ZipArchiveEntry zae =
                                         new ZipArchiveEntry("0");
                                     zae.setSize(ONE_MILLION);
                                     zae.setMethod(ZipArchiveEntry.DEFLATED);
                                     zos.putArchiveEntry(zae);
                                     zos.write(buf);
                                     zos.closeArchiveEntry();
                                     zos.close();

                                     RandomAccessFile a =
                                         new RandomAccessFile(f, "r");
                                     try {
                                         final long end =
                                             getLengthAndPositionAtCentralDirectory(a);

                                         long cfhPos = a.getFilePointer();
                                         // grab first entry, verify
                                         // sizes are not 0xFFFFFFF
                                         // and it has no ZIP64
                                         // extended information extra
                                         // field
                                         byte[] header = new byte[12];
                                         a.readFully(header);
                                         assertArrayEquals(new byte[] {
                                                 // sig
                                                 (byte) 0x50, (byte) 0x4b, 1, 2,
                                                 // version made by
                                                 20, 0,
                                                 // version needed to extract
                                                 20, 0,
                                                 // GPB (EFS + Data Descriptor)
                                                 8, 8,
                                                 // method
                                                 8, 0,
                                             }, header);
                                         // ignore timestamp
                                         a.skipBytes(4);
                                         byte[] crc = new byte[4];
                                         a.readFully(crc);
                                         assertArrayEquals(new byte[] {
                                                 (byte) 0x9E, (byte) 0xCB,
                                                 (byte) 0x79, (byte) 0x12,
                                             }, crc);
                                         // skip compressed size
                                         a.skipBytes(4);
                                         byte[] rest = new byte[23];
                                         a.readFully(rest);
                                         assertArrayEquals(new byte[] {
                                                 // Original Size
                                                 (byte) 0x40, (byte) 0x42,
                                                 (byte) 0x0F, 0,
                                                 // file name length
                                                 1, 0,
                                                 // extra field length
                                                 0, 0,
                                                 // comment length
                                                 0, 0,
                                                 // disk number
                                                 0, 0,
                                                 // attributes
                                                 0, 0,
                                                 0, 0, 0, 0,
                                                 // offset
                                                 0, 0, 0, 0,
                                                 // file name
                                                 (byte) '0'
                                             }, rest);

                                         // validate data descriptor
                                         a.seek(cfhPos - 16);
                                         byte[] dd = new byte[8];
                                         a.readFully(dd);
                                         assertArrayEquals(new byte[] {
                                                 // sig
                                                 (byte) 0x50, (byte) 0x4b, 7, 8,
                                                 // CRC
                                                 (byte) 0x9E, (byte) 0xCB,
                                                 (byte) 0x79, (byte) 0x12,
                                             }, dd);
                                         // skip uncompressed size
                                         a.skipBytes(4);
                                         dd = new byte[4];
                                         a.readFully(dd);
                                         assertArrayEquals(new byte[] {
                                                 // original size
                                                 (byte) 0x40, (byte) 0x42,
                                                 (byte) 0x0F, 0,
                                             }, dd);

                                         // and now validate local file header
                                         a.seek(0);
                                         header = new byte[10];
                                         a.readFully(header);
                                         assertArrayEquals(new byte[] {
                                                 // sig
                                                 (byte) 0x50, (byte) 0x4b, 3, 4,
                                                 // version needed to extract
                                                 20, 0,
                                                 // GPB (EFS + Data Descriptor)
                                                 8, 8,
                                                 // method
                                                 8, 0,
                                             }, header);
                                         // ignore timestamp
                                         a.skipBytes(4);
                                         rest = new byte[17];
                                         a.readFully(rest);
                                         assertArrayEquals(new byte[] {
                                                 // CRC
                                                 0, 0, 0, 0,
                                                 // Compressed Size
                                                 0, 0, 0, 0,
                                                 // Original Size
                                                 0, 0, 0, 0,
                                                 // file name length
                                                 1, 0,
                                                 // extra field length
                                                 0, 0,
                                                 // file name
                                                 (byte) '0'
                                             }, rest);
                                     } finally {
                                         a.close();
                                     }
                                 }
                             },
                             false);
    }

    /*
     * One entry of length 1 million bytes, written with compression
     * to a file.
     *
     * Writing to a file => sizes are stored directly inside the LFH.
     * No Data Descriptor at all.  Shouldn't contain any ZIP64 extra
     * field if size was known.
     */
    private static ZipOutputTest writeSmallDeflatedEntryToFile(final boolean knownSize) {
        return new ZipOutputTest() {
            public void test(File f, ZipArchiveOutputStream zos)
                throws IOException {
                byte[] buf = new byte[ONE_MILLION];
                ZipArchiveEntry zae = new ZipArchiveEntry("0");
                if (knownSize) {
                    zae.setSize(ONE_MILLION);
                }
                zae.setMethod(ZipArchiveEntry.DEFLATED);
                zos.putArchiveEntry(zae);
                zos.write(buf);
                zos.closeArchiveEntry();
                zos.close();

                RandomAccessFile a = new RandomAccessFile(f, "r");
                try {
                    final long end = getLengthAndPositionAtCentralDirectory(a);

                    long cfhPos = a.getFilePointer();
                    // grab first CD entry, verify sizes are not
                    // 0xFFFFFFFF and it has a no ZIP64 extended
                    // information extra field
                    byte[] header = new byte[12];
                    a.readFully(header);
                    assertArrayEquals(new byte[] {
                            // sig
                            (byte) 0x50, (byte) 0x4b, 1, 2,
                            // version made by
                            20, 0,
                            // version needed to extract
                            10, 0,
                            // GPB (EFS + *no* Data Descriptor)
                            0, 8,
                            // method
                            8, 0,
                        }, header);
                    // ignore timestamp
                    a.skipBytes(4);
                    byte[] crc = new byte[4];
                    a.readFully(crc);
                    assertArrayEquals(new byte[] {
                            (byte) 0x9E, (byte) 0xCB,
                            (byte) 0x79, (byte) 0x12,
                        }, crc);
                    // skip compressed size
                    a.skipBytes(4);
                    byte[] rest = new byte[23];
                    a.readFully(rest);
                    assertArrayEquals(new byte[] {
                            // Original Size
                            (byte) 0x40, (byte) 0x42, (byte) 0x0F, 0,
                            // file name length
                            1, 0,
                            // extra field length
                            0, 0,
                            // comment length
                            0, 0,
                            // disk number
                            0, 0,
                            // attributes
                            0, 0,
                            0, 0, 0, 0,
                            // offset
                            0, 0, 0, 0,
                            // file name
                            (byte) '0'
                        }, rest);

                    // and now validate local file header
                    a.seek(0);
                    header = new byte[10];
                    a.readFully(header);
                    assertArrayEquals(new byte[] {
                            // sig
                            (byte) 0x50, (byte) 0x4b, 3, 4,
                            // version needed to extract
                            10, 0,
                            // GPB (EFS bit, no DD)
                            0, 8,
                            // method
                            8, 0,
                        }, header);
                    // ignore timestamp
                    a.skipBytes(4);
                    crc = new byte[4];
                    a.readFully(crc);
                    assertArrayEquals(new byte[] {
                            (byte) 0x9E, (byte) 0xCB,
                            (byte) 0x79, (byte) 0x12,
                        }, crc);
                    // skip compressed size
                    a.skipBytes(4);
                    rest = new byte[9];
                    a.readFully(rest);
                    assertArrayEquals(new byte[] {
                            // Original Size
                            (byte) 0x40, (byte) 0x42, (byte) 0x0F, 0,
                            // file name length
                            1, 0,
                            // extra field length
                            (byte) (knownSize ? 0 : 20), 0,
                            // file name
                            (byte) '0'
                        }, rest);
                    if (!knownSize) {
                        byte[] extra = new byte[12];
                        a.readFully(extra);
                        assertArrayEquals(new byte[] {
                                // Header-ID
                                1, 0,
                                // size of extra
                                16, 0,
                                // original size
                                (byte) 0x40, (byte) 0x42, (byte) 0x0F, 0,
                                0, 0, 0, 0,
                                // don't know the
                                // compressed size,
                                // don't want to
                                // hard-code it
                            }, extra);
                    }
                } finally {
                    a.close();
                }
            }
        };
    }

    @Test public void writeSmallDeflatedEntryKnownSizeToFile()
        throws Throwable {
        withTemporaryArchive("writeSmallDeflatedEntryKnownSizeToFile",
                             writeSmallDeflatedEntryToFile(true),
                             true);
    }

    @Test public void writeSmallDeflatedEntryUnknownSizeToFile()
        throws Throwable {
        withTemporaryArchive("writeSmallDeflatedEntryUnknownSizeToFile",
                             writeSmallDeflatedEntryToFile(false),
                             true);
    }

    static interface ZipOutputTest {
        void test(File f, ZipArchiveOutputStream zos) throws IOException;
    }

    private static void withTemporaryArchive(String testName,
                                             ZipOutputTest test,
                                             boolean useRandomAccessFile)
        throws Throwable {
        File f = getTempFile(testName);
        BufferedOutputStream os = null;
        ZipArchiveOutputStream zos = useRandomAccessFile
            ? new ZipArchiveOutputStream(f)
            : new ZipArchiveOutputStream(os = new BufferedOutputStream(new FileOutputStream(f)));
        try {
            test.test(f, zos);
        } catch (IOException ex) {
            System.err.println("Failed to write archive because of: "
                               + ex.getMessage()
                               + " - likely not enough disk space.");
            assumeTrue(false);
        } finally {
            try {
                zos.close();
            } finally {
                if (os != null) {
                    os.close();
                }
            }
            f.delete();
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

    private static File get5GBZerosFileGeneratedBy7ZIP() throws Throwable {
        return getFile("/5GB_of_Zeros_7ZIP.zip");
    }

    private static File get5GBZerosFileGeneratedByJava7Jar() throws Throwable {
        return getFile("/5GB_of_Zeros_jar.zip");
    }

    private static File get100KFileFile() throws Throwable {
        return getFile("/100k_Files.zip");
    }

    private static File get100KFileFileGeneratedBy7ZIP() throws Throwable {
        return getFile("/100k_Files_7ZIP.zip");
    }

    private static File get100KFileFileGeneratedByWinCF() throws Throwable {
        return getFile("/100k_Files_WindowsCompressedFolders.zip");
    }

    private static File get100KFileFileGeneratedByJava7Jar() throws Throwable {
        return getFile("/100k_Files_jar.zip");
    }

    private static File getTempFile(String testName) throws Throwable {
        File f = File.createTempFile("commons-compress-" + testName, ".zip");
        f.deleteOnExit();
        return f;
    }

    private static void read5GBOfZerosImpl(File f, String expectedName)
        throws IOException {
        FileInputStream fin = new FileInputStream(f);
        ZipArchiveInputStream zin = null;
        try {
            zin = new ZipArchiveInputStream(fin);
            ZipArchiveEntry zae = zin.getNextZipEntry();
            assertEquals(expectedName, zae.getName());
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
            assertEquals(FIVE_BILLION, zae.getSize());
        } finally {
            if (zin != null) {
                zin.close();
            }
            if (fin != null) {
                fin.close();
            }
        }
    }

    private static void read100KFilesImpl(File f) throws IOException {
        FileInputStream fin = new FileInputStream(f);
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

    private static void read100KFilesUsingZipFileImpl(File f)
        throws IOException {
        ZipFile zf = null;
        try {
            zf = new ZipFile(f);
            int files = 0;
            for (Enumeration e = zf.getEntries(); e.hasMoreElements(); ) {
                ZipArchiveEntry zae = (ZipArchiveEntry) e.nextElement();
                if (!zae.isDirectory()) {
                    files++;
                    assertEquals(0, zae.getSize());
                }
            }
            assertEquals(ONE_HUNDRED_THOUSAND, files);
        } finally {
            ZipFile.closeQuietly(zf);
        }
    }

    private static long getLengthAndPositionAtCentralDirectory(RandomAccessFile a)
        throws IOException {
        final long end = a.length();
        a.seek(end - 22 - 20);
        byte[] sig = new byte[4];
        a.readFully(sig);
        if (sig[0] != (byte) 0x50 || sig[1] != (byte) 0x4b
            || sig[2] != 6 || sig[3] != 7) {
            // not a ZIP64 archive
            return getLengthAndPositionAtCentralDirectory32(a, end);
        }

        long cdOffsetLoc = end - 22 - 20 - 56 + 48;
        // seek to central directory locator
        a.seek(cdOffsetLoc);
        byte[] cdOffset = new byte[8];
        a.readFully(cdOffset);
        a.seek(ZipEightByteInteger.getLongValue(cdOffset));
        return end;
    }

    private static long getLengthAndPositionAtCentralDirectory32(RandomAccessFile a, final long end)
        throws IOException {
        a.seek(end - 22 + 16);
        byte[] cdOffset = new byte[4];
        a.readFully(cdOffset);
        a.seek(ZipLong.getValue(cdOffset));
        return end;
    }

    private static void write100KFilesToStream(ZipArchiveOutputStream zos)
        throws IOException {
        for (int i = 0; i < ONE_HUNDRED_THOUSAND; i++) {
            ZipArchiveEntry zae = new ZipArchiveEntry(String.valueOf(i));
            zae.setSize(0);
            zos.putArchiveEntry(zae);
            zos.closeArchiveEntry();
        }
        zos.close();
    }

    private static void
        write3EntriesCreatingBigArchiveToStream(ZipArchiveOutputStream zos)
        throws IOException {
        byte[] buf = new byte[ONE_MILLION];
        ZipArchiveEntry zae = null;
        for (int i = 0; i < 2; i++) {
            zae = new ZipArchiveEntry(String.valueOf(i));
            zae.setSize(FIVE_BILLION / 2);
            zae.setMethod(ZipArchiveEntry.STORED);
            zae.setCrc(0x8a408f16L);
            zos.putArchiveEntry(zae);
            for (int j = 0; j < FIVE_BILLION / 2 / 1000 / 1000;
                 j++) {
                zos.write(buf);
            }
            zos.closeArchiveEntry();
        }
        zae = new ZipArchiveEntry(String.valueOf(2));
        zae.setSize(1);
        zae.setMethod(ZipArchiveEntry.STORED);
        zae.setCrc(0x9b9265bL);
        zos.putArchiveEntry(zae);
        zos.write(new byte[] { 42 });
        zos.closeArchiveEntry();
        zos.close();
    }
}
