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

import static org.apache.commons.compress.AbstractTestCase.getFile;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.Random;
import java.util.zip.ZipEntry;

import org.apache.commons.compress.AbstractTestCase;
import org.junit.jupiter.api.Test;

public class Zip64SupportIT {

    interface ZipOutputTest {
        void test(File f, ZipArchiveOutputStream zos) throws IOException;
    }
    private static final long FIVE_BILLION = 5000000000L;
    private static final int ONE_MILLION = 1000000;

    private static final int ONE_HUNDRED_THOUSAND = 100000;

    private static final ZipOutputTest write100KFilesModeNever =
            (f, zos) -> {
                zos.setUseZip64(Zip64Mode.Never);
                final Zip64RequiredException ex = assertThrows(Zip64RequiredException.class,
                        () -> write100KFilesToStream(zos), "expected a Zip64RequiredException");
                assertEquals(Zip64RequiredException.TOO_MANY_ENTRIES_MESSAGE,
                        ex.getMessage());
            };

    private static final ZipOutputTest write3EntriesCreatingBigArchiveModeNever =
            (f, zos) -> {
                zos.setUseZip64(Zip64Mode.Never);
                final Zip64RequiredException ex = assertThrows(Zip64RequiredException.class,
                        () -> write3EntriesCreatingBigArchiveToStream(zos), "expected a Zip64RequiredException");
                assertEquals(Zip64RequiredException.ARCHIVE_TOO_BIG_MESSAGE, ex.getMessage());
            };

    private static File get100KFileFile() throws Throwable {
        return getFile("100k_Files.zip");
    }

    private static File get100KFileFileGeneratedBy7ZIP() throws Throwable {
        return getFile("100k_Files_7ZIP.zip");
    }

    private static File get100KFileFileGeneratedByJava7Jar() throws Throwable {
        return getFile("100k_Files_jar.zip");
    }

    private static File get100KFileFileGeneratedByPKZip() throws Throwable {
        return getFile("100k_Files_PKZip.zip");
    }

    private static File get100KFileFileGeneratedByWinCF() throws Throwable {
        return getFile("100k_Files_WindowsCompressedFolders.zip");
    }

    private static File get100KFileFileGeneratedByWinZIP() throws Throwable {
        return getFile("100k_Files_WinZIP.zip");
    }

    private static File get5GBZerosFile() throws Throwable {
        return getFile("5GB_of_Zeros.zip");
    }

    private static File get5GBZerosFileGeneratedBy7ZIP() throws Throwable {
        return getFile("5GB_of_Zeros_7ZIP.zip");
    }

    private static File get5GBZerosFileGeneratedByJava7Jar() throws Throwable {
        return getFile("5GB_of_Zeros_jar.zip");
    }

    private static File get5GBZerosFileGeneratedByPKZip() throws Throwable {
        return getFile("5GB_of_Zeros_PKZip.zip");
    }

    private static File get5GBZerosFileGeneratedByWinZIP() throws Throwable {
        return getFile("5GB_of_Zeros_WinZip.zip");
    }

    private static long getLengthAndPositionAtCentralDirectory(final RandomAccessFile a)
        throws IOException {
        final long end = a.length();
        a.seek(end - 22 - 20);
        final byte[] sig = new byte[4];
        a.readFully(sig);
        if (sig[0] != (byte) 0x50 || sig[1] != (byte) 0x4b
            || sig[2] != 6 || sig[3] != 7) {
            // not a ZIP64 archive
            return getLengthAndPositionAtCentralDirectory32(a, end);
        }

        final long cdOffsetLoc = end - 22 - 20 - 56 + 48;
        // seek to central directory locator
        a.seek(cdOffsetLoc);
        final byte[] cdOffset = new byte[8];
        a.readFully(cdOffset);
        a.seek(ZipEightByteInteger.getLongValue(cdOffset));
        return end;
    }

    private static long getLengthAndPositionAtCentralDirectory32(final RandomAccessFile a, final long end)
        throws IOException {
        a.seek(end - 22 + 16);
        final byte[] cdOffset = new byte[4];
        a.readFully(cdOffset);
        a.seek(ZipLong.getValue(cdOffset));
        return end;
    }

    private static File getTempFile(final String testName) throws Throwable {
        final File f = File.createTempFile("commons-compress-" + testName, ".zip");
        f.deleteOnExit();
        return f;
    }

    private static void read100KFilesImpl(final File f) throws IOException {
        try (InputStream fin = Files.newInputStream(f.toPath());
                ZipArchiveInputStream zin = new ZipArchiveInputStream(fin)) {
            int files = 0;
            ZipArchiveEntry zae = null;
            while ((zae = zin.getNextZipEntry()) != null) {
                if (!zae.isDirectory()) {
                    files++;
                    assertEquals(0, zae.getSize());
                }
            }
            assertEquals(ONE_HUNDRED_THOUSAND, files);
        }
    }

    private static void read100KFilesUsingZipFileImpl(final File f)
        throws IOException {
        ZipFile zf = null;
        try {
            zf = new ZipFile(f);
            int files = 0;
            for (final Enumeration<ZipArchiveEntry> e = zf.getEntries(); e.hasMoreElements(); ) {
                final ZipArchiveEntry zae = e.nextElement();
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

    private static void read5GBOfZerosImpl(final File f, final String expectedName)
        throws IOException {
        try (InputStream fin = Files.newInputStream(f.toPath());
                ZipArchiveInputStream zin = new ZipArchiveInputStream(fin)) {
            ZipArchiveEntry zae = zin.getNextZipEntry();
            while (zae.isDirectory()) {
                zae = zin.getNextZipEntry();
            }
            assertEquals(expectedName, zae.getName());
            final byte[] buf = new byte[1024 * 1024];
            long read = 0;
            final Random r = new Random(System.currentTimeMillis());
            int readNow;
            while ((readNow = zin.read(buf, 0, buf.length)) > 0) {
                // testing all bytes for a value of 0 is going to take
                // too long, just pick a few ones randomly
                for (int i = 0; i < 1024; i++) {
                    final int idx = r.nextInt(readNow);
                    assertEquals(0, buf[idx], "testing byte " + (read + idx));
                }
                read += readNow;
            }
            assertEquals(FIVE_BILLION, read);
            assertNull(zin.getNextZipEntry());
            assertEquals(FIVE_BILLION, zae.getSize());
        }
    }

    private static void read5GBOfZerosUsingZipFileImpl(final File f,
                                                       final String expectedName)
        throws IOException {
        ZipFile zf = null;
        try {
            zf = new ZipFile(f);
            final Enumeration<ZipArchiveEntry> e = zf.getEntries();
            assertTrue(e.hasMoreElements());
            ZipArchiveEntry zae = e.nextElement();
            while (zae.isDirectory()) {
                zae = e.nextElement();
            }
            assertEquals(expectedName, zae.getName());
            assertEquals(FIVE_BILLION, zae.getSize());
            final byte[] buf = new byte[1024 * 1024];
            long read = 0;
            final Random r = new Random(System.currentTimeMillis());
            int readNow;
            try (InputStream zin = zf.getInputStream(zae)) {
                while ((readNow = zin.read(buf, 0, buf.length)) > 0) {
                    // testing all bytes for a value of 0 is going to take
                    // too long, just pick a few ones randomly
                    for (int i = 0; i < 1024; i++) {
                        final int idx = r.nextInt(readNow);
                        assertEquals(0, buf[idx], "testing byte " + (read + idx));
                    }
                    read += readNow;
                }
            }
            assertEquals(FIVE_BILLION, read);
            assertFalse(e.hasMoreElements());
        } finally {
            ZipFile.closeQuietly(zf);
        }
    }

    private static void withTemporaryArchive(final String testName,
                                             final ZipOutputTest test,
                                             final boolean useRandomAccessFile) throws Throwable {
        withTemporaryArchive(testName, test, useRandomAccessFile, null);
    }

    private static void withTemporaryArchive(final String testName,
                                             final ZipOutputTest test,
                                             final boolean useRandomAccessFile,
                                             final Long splitSize)
        throws Throwable {
        File f = getTempFile(testName);
        File dir = null;
        if (splitSize != null) {
            dir = Files.createTempDirectory("commons-compress-" + testName).toFile();
            dir.deleteOnExit();

            f = new File(dir, "commons-compress-" + testName + ".zip");
        }
        BufferedOutputStream os = null;
        ZipArchiveOutputStream zos = useRandomAccessFile
            ? new ZipArchiveOutputStream(f)
            : new ZipArchiveOutputStream(os = new BufferedOutputStream(Files.newOutputStream(f.toPath())));
        if (splitSize != null) {
            zos = new ZipArchiveOutputStream(f, splitSize);
        }

        try {
            test.test(f, zos);
        } catch (final IOException ex) {
            System.err.println("Failed to write archive because of: "
                               + ex.getMessage()
                               + " - likely not enough disk space.");
            assumeTrue(false);
        } finally {
            try {
                zos.destroy();
            } finally {
                try {
                    if (os != null) {
                        os.close();
                    }
                    AbstractTestCase.tryHardToDelete(f);
                } finally {
                    if (dir != null) {
                        AbstractTestCase.rmdir(dir);
                    }
                }
            }
        }
    }

    private static ZipOutputTest write100KFiles() {
        return write100KFiles(Zip64Mode.AsNeeded);
    }

    private static ZipOutputTest write100KFiles(final Zip64Mode mode) {
        return (f, zos) -> {
        if (mode != Zip64Mode.AsNeeded) {
            zos.setUseZip64(mode);
        }
        write100KFilesToStream(zos);
        try (RandomAccessFile a = new RandomAccessFile(f, "r")) {
            final long end = a.length();

            // validate "end of central directory" is at
            // the end of the file and contains the magic
            // value 0xFFFF as "number of entries".
            a.seek(end
                    - 22 /* length of EOCD without file comment */);
            final byte[] eocd = new byte[12];
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
            final long expectedZ64EocdOffset = end - 22 /* eocd.length */
                    - 20 /* z64 eocd locator.length */
                    - 56 /* z64 eocd without extensible data sector */;
            final byte[] loc =
                    ZipEightByteInteger.getBytes(expectedZ64EocdOffset);
            a.seek(end - 22 - 20);
            final byte[] z64EocdLoc = new byte[20];
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
            final byte[] z64EocdStart = new byte[40];
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
            final byte[] cdOffset = new byte[8];
            a.readFully(cdOffset);
            final long cdLoc = ZipEightByteInteger.getLongValue(cdOffset);

            // finally verify there really is a central
            // directory entry where the Zip64 EOCD claims
            a.seek(cdLoc);
            final byte[] sig = new byte[4];
            a.readFully(sig);
            assertArrayEquals(new byte[] {
                    (byte) 0x50, (byte) 0x4b, 1, 2,
            }, sig);
        }
         };
    }

    private static void write100KFilesToStream(final ZipArchiveOutputStream zos)
        throws IOException {
        for (int i = 0; i < ONE_HUNDRED_THOUSAND; i++) {
            final ZipArchiveEntry zae = new ZipArchiveEntry(String.valueOf(i));
            zae.setSize(0);
            zos.putArchiveEntry(zae);
            zos.closeArchiveEntry();
        }
        zos.close();
    }

    private static ZipOutputTest write3EntriesCreatingBigArchive() {
        return write3EntriesCreatingBigArchive(Zip64Mode.AsNeeded);
    }

    private static ZipOutputTest write3EntriesCreatingBigArchive(final Zip64Mode mode) {
        return write3EntriesCreatingBigArchive(mode, false);
    }

    /*
     * Individual sizes don't require ZIP64 but the offset of the
     * third entry is bigger than 0xFFFFFFFF so a ZIP64 extended
     * information is needed inside the central directory.
     *
     * Creates a temporary archive of approx 5GB in size
     */
    private static ZipOutputTest
        write3EntriesCreatingBigArchive(final Zip64Mode mode, final boolean isSplitArchive) {
        return (f, zos) -> {
        if (mode != Zip64Mode.AsNeeded) {
            zos.setUseZip64(mode);
        }
        write3EntriesCreatingBigArchiveToStream(zos);

        try (RandomAccessFile a = new RandomAccessFile(f, "r")) {
            getLengthAndPositionAtCentralDirectory(a);
            // skip first two entries
            a.skipBytes(2 * 47 /* CD entry of file with
                                  file name length 1 and no
                                  extra data */
                            + 2 * (mode == Zip64Mode.Always ? 32 : 0)
                        /* ZIP64 extra fields if mode is Always */
            );

            // grab third entry, verify offset is
            // 0xFFFFFFFF and it has a ZIP64 extended
            // information extra field
            final byte[] header = new byte[12];
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
            }, header, "CDH start");
            // ignore timestamp, CRC, compressed size
            a.skipBytes(12);
            // Original Size
            final byte[] originalSize = new byte[4];
            a.readFully(originalSize);
            if (mode == Zip64Mode.Always) {
                assertArrayEquals(new byte[] {
                        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                    }, originalSize, "CDH original size");
            } else {
                assertArrayEquals(new byte[] {
                        1, 0, 0, 0
                    }, originalSize, "CDH original size");
            }
            final byte[] rest = new byte[19];
            a.readFully(rest);
            assertArrayEquals(new byte[] {
                    // file name length
                    1, 0,
                    // extra field length
                    (byte) (mode == Zip64Mode.Always? 32 : 12), 0,
                    // comment length
                    0, 0,
                    // disk number
                    (byte) (isSplitArchive? 0xFF : 0), (byte) (isSplitArchive? 0xFF : 0),
                    // attributes
                    0, 0,
                    0, 0, 0, 0,
                    // offset
                    (byte) 0xFF, (byte) 0xFF,
                    (byte) 0xFF, (byte) 0xFF,
                    // file name
                    (byte) '2'
            }, rest, "CDH rest");
            if (mode == Zip64Mode.Always) {
                final byte[] extra1 = new byte[12];
                a.readFully(extra1);
                assertArrayEquals(new byte[] {
                        // Header-ID
                        1, 0,
                        // size
                        28, 0,
                        // Original Size
                        1, 0, 0, 0, 0, 0, 0, 0,
                    }, extra1, "CDH extra");
                // skip compressed size
                a.skipBytes(8);
            } else {
                final byte[] extra2 = new byte[4];
                a.readFully(extra2);
                assertArrayEquals(new byte[] {
                        // Header-ID
                        1, 0,
                        // size
                        8, 0,
                    }, extra2, "CDH extra");
            }

            // read offset of LFH
            final byte[] offset = new byte[8];
            a.readFully(offset);
            // verify there is a LFH where the CD claims it
            a.seek(ZipEightByteInteger.getLongValue(offset));
            final byte[] sig = new byte[4];
            a.readFully(sig);
            assertArrayEquals(new byte[] {
                    (byte) 0x50, (byte) 0x4b, 3, 4,
            }, sig, "LFH signature");
        }
         };
    }

    private static void
        write3EntriesCreatingBigArchiveToStream(final ZipArchiveOutputStream zos)
        throws IOException {
        final byte[] buf = new byte[ONE_MILLION];
        ZipArchiveEntry zae = null;
        for (int i = 0; i < 2; i++) {
            zae = new ZipArchiveEntry(String.valueOf(i));
            zae.setSize(FIVE_BILLION / 2);
            zae.setMethod(ZipEntry.STORED);
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
        zae.setMethod(ZipEntry.STORED);
        zae.setCrc(0x9b9265bL);
        zos.putArchiveEntry(zae);
        zos.write(new byte[] { 42 });
        zos.closeArchiveEntry();
        zos.close();
    }

    private static File write5GBZerosFile(final String testName) throws Throwable {
        final File f = getTempFile(testName);
        final ZipArchiveOutputStream zos = new ZipArchiveOutputStream(f);
        try {
            zos.setUseZip64(Zip64Mode.Always);
            final byte[] buf = new byte[ONE_MILLION];
            final ZipArchiveEntry zae = new ZipArchiveEntry("5GB_of_Zeros");
            zae.setSize(FIVE_BILLION);
            zae.setMethod(ZipEntry.DEFLATED);
            zae.setCrc(0x8a408f16L);
            zos.putArchiveEntry(zae);
            for (int j = 0; j < FIVE_BILLION / 1000 / 1000; j++) {
                zos.write(buf);
            }
            zos.closeArchiveEntry();
            zos.close();
        } catch (final IOException ex) {
            System.err.println("Failed to write archive because of: "
                               + ex.getMessage()
                               + " - likely not enough disk space.");
            assumeTrue(false);
        } finally {
            zos.destroy();
        }
        return f;
    }

    private static ZipOutputTest
        writeBigDeflatedEntryToFile(final boolean knownSize) {
        return writeBigDeflatedEntryToFile(knownSize, Zip64Mode.AsNeeded);
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
    private static ZipOutputTest
        writeBigDeflatedEntryToFile(final boolean knownSize,
                                    final Zip64Mode mode) {
        return (f, zos) -> {
        if (mode != Zip64Mode.AsNeeded) {
            zos.setUseZip64(mode);
        }
        final byte[] buf = new byte[ONE_MILLION];
        final ZipArchiveEntry zae = new ZipArchiveEntry("0");
        if (knownSize) {
            zae.setSize(FIVE_BILLION);
        }
        zae.setMethod(ZipEntry.DEFLATED);
        zos.putArchiveEntry(zae);
        for (int j = 0;
             j < FIVE_BILLION / 1000 / 1000;
             j++) {
            zos.write(buf);
        }
        zos.closeArchiveEntry();
        zos.close();

        try (RandomAccessFile a = new RandomAccessFile(f, "r")) {
            getLengthAndPositionAtCentralDirectory(a);

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
            }, header, "CDH start");
            // ignore timestamp
            a.skipBytes(4);
            byte[] rest = new byte[26];
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
                    (byte) (mode == Zip64Mode.Always? 32 : 20), 0,
                    // comment length
                    0, 0,
                    // disk number
                    0, 0,
                    // attributes
                    0, 0,
                    0, 0, 0, 0,
            }, rest, "CDH rest");
            byte[] offset = new byte[4];
            a.readFully(offset);
            if (mode == Zip64Mode.Always) {
                assertArrayEquals(new byte[] {
                        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                    }, offset, "offset");
            } else {
                assertArrayEquals(new byte[] {
                        0, 0, 0, 0,
                    }, offset, "offset");
            }
            assertEquals('0', a.read());
            byte[] extra = new byte[12];
            a.readFully(extra);
            // 5e9 == 0x12A05F200
            assertArrayEquals(new byte[] {
                    // Header-ID
                    1, 0,
                    // size of extra
                    (byte) (mode == Zip64Mode.Always? 28 : 16), 0,
                    // original size
                    0, (byte) 0xF2, 5, (byte) 0x2A,
                    1, 0, 0, 0,
            }, extra, "CDH extra");
            if (mode == Zip64Mode.Always) {
                // skip compressed size
                a.skipBytes(8);
                offset = new byte[8];
                a.readFully(offset);
                assertArrayEquals(new byte[] {
                        0, 0, 0, 0, 0, 0, 0, 0,
                    }, offset, "extra offset");
            }

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
            }, header, "LFH start");
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
            extra = new byte[12];
            a.readFully(extra);
            assertArrayEquals(new byte[] {
                    // Header-ID
                    1, 0,
                    // size of extra
                    16, 0,
                    // original size
                    0, (byte) 0xF2, 5, (byte) 0x2A,
                    1, 0, 0, 0,
                    // skip compressed size
            }, extra);
        }
         };
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
    private static ZipOutputTest
        writeBigDeflatedEntryToFileModeNever(final boolean knownSize) {
        return (f, zos) -> {
            zos.setUseZip64(Zip64Mode.Never);
            final Zip64RequiredException ex = assertThrows(Zip64RequiredException.class, () -> {
                final byte[] buf = new byte[ONE_MILLION];
                final ZipArchiveEntry zae = new ZipArchiveEntry("0");
                if (knownSize) {
                    zae.setSize(FIVE_BILLION);
                }
                zae.setMethod(ZipEntry.DEFLATED);
                zos.putArchiveEntry(zae);
                for (int j = 0; j < FIVE_BILLION / 1000 / 1000; j++) {
                    zos.write(buf);
                }
                zos.closeArchiveEntry();
            }, "expected a Zip64RequiredException");
            assertTrue(ex.getMessage().startsWith("0's size"));
        };
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
    private static ZipOutputTest
        writeBigDeflatedEntryToStream(final boolean knownSize,
                                      final Zip64Mode mode) {
        return (f, zos) -> {
        if (mode != Zip64Mode.AsNeeded) {
            zos.setUseZip64(mode);
        }
        final byte[] buf = new byte[ONE_MILLION];
        final ZipArchiveEntry zae = new ZipArchiveEntry("0");
        if (knownSize) {
            zae.setSize(FIVE_BILLION);
        }
        zae.setMethod(ZipEntry.DEFLATED);
        zos.putArchiveEntry(zae);
        for (int j = 0; j < FIVE_BILLION / 1000 / 1000; j++) {
            zos.write(buf);
        }
        zos.closeArchiveEntry();
        zos.close();

        try (RandomAccessFile a = new RandomAccessFile(f, "r")) {
            getLengthAndPositionAtCentralDirectory(a);

            final long cfhPos = a.getFilePointer();
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
            }, header, "CDH start");
            // ignore timestamp
            a.skipBytes(4);
            byte[] rest = new byte[26];
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
                    (byte) (mode == Zip64Mode.Always? 32 : 20), 0,
                    // comment length
                    0, 0,
                    // disk number
                    0, 0,
                    // attributes
                    0, 0,
                    0, 0, 0, 0,
            }, rest, "CDH rest");
            byte[] offset = new byte[4];
            a.readFully(offset);
            if (mode == Zip64Mode.Always) {
                assertArrayEquals(new byte[] {
                        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                    }, offset, "offset");
            } else {
                assertArrayEquals(new byte[] {
                        0, 0, 0, 0,
                    }, offset, "offset");
            }
            assertEquals('0', a.read());
            byte[] extra = new byte[12];
            a.readFully(extra);
            // 5e9 == 0x12A05F200
            assertArrayEquals(new byte[] {
                    // Header-ID
                    1, 0,
                    // size of extra
                    (byte) (mode == Zip64Mode.Always? 28 : 16), 0,
                    // original size
                    0, (byte) 0xF2, 5, (byte) 0x2A,
                    1, 0, 0, 0,
            }, extra, "CDH extra");
            if (mode == Zip64Mode.Always) {
                // skip compressed size
                a.skipBytes(8);
                offset = new byte[8];
                a.readFully(offset);
                assertArrayEquals(new byte[] {
                        0, 0, 0, 0, 0, 0, 0, 0,
                    }, offset, "extra offset");
            }

            // validate data descriptor
            a.seek(cfhPos - 24);
            byte[] dd = new byte[8];
            a.readFully(dd);
            assertArrayEquals(new byte[] {
                    // sig
                    (byte) 0x50, (byte) 0x4b, 7, 8,
                    // CRC
                    (byte) 0x50, (byte) 0x6F, (byte) 0x31, (byte) 0x5c,
            }, dd, "DD");
            // skip compressed size
            a.skipBytes(8);
            dd = new byte[8];
            a.readFully(dd);
            assertArrayEquals(new byte[] {
                    // original size
                    0, (byte) 0xF2, 5, (byte) 0x2A,
                    1, 0, 0, 0,
            }, dd, "DD sizes");

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
            }, header, "LFH start");
            // ignore timestamp
            a.skipBytes(4);
            rest = new byte[17];
            a.readFully(rest);
            assertArrayEquals(new byte[] {
                    // CRC
                    0, 0, 0, 0,
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
            }, rest, "LFH rest");
            extra = new byte[20];
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
            }, extra, "LFH extra");
        }
         };
    }

    private static ZipOutputTest
        writeBigDeflatedEntryUnknownSizeToStream(final Zip64Mode mode) {
        return (f, zos) -> {
            final Zip64RequiredException ex = assertThrows(Zip64RequiredException.class, () -> {
                if (mode != Zip64Mode.AsNeeded) {
                    zos.setUseZip64(mode);
                }
                final byte[] buf = new byte[ONE_MILLION];
                final ZipArchiveEntry zae = new ZipArchiveEntry("0");
                zae.setMethod(ZipEntry.DEFLATED);
                zos.putArchiveEntry(zae);
                for (int j = 0; j < FIVE_BILLION / 1000 / 1000; j++) {
                    zos.write(buf);
                }
                zos.closeArchiveEntry();
            }, "expected a Zip64RequiredException");
            assertTrue(ex.getMessage().startsWith("0's size"));
        };
    }

    private static ZipOutputTest writeBigStoredEntry(final boolean knownSize) {
        return writeBigStoredEntry(knownSize, Zip64Mode.AsNeeded);
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
    private static ZipOutputTest writeBigStoredEntry(final boolean knownSize,
                                                     final Zip64Mode mode) {
        return (f, zos) -> {
        if (mode != Zip64Mode.AsNeeded) {
            zos.setUseZip64(mode);
        }
        final byte[] buf = new byte[ONE_MILLION];
        final ZipArchiveEntry zae = new ZipArchiveEntry("0");
        if (knownSize) {
            zae.setSize(FIVE_BILLION);
            zae.setCrc(0x5c316f50L);
        }
        zae.setMethod(ZipEntry.STORED);
        zos.putArchiveEntry(zae);
        for (int j = 0; j < FIVE_BILLION / 1000 / 1000; j++) {
            zos.write(buf);
        }
        zos.closeArchiveEntry();
        zos.close();

        try (RandomAccessFile a = new RandomAccessFile(f, "r")) {
            getLengthAndPositionAtCentralDirectory(a);

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
            }, header, "CDH start");
            // ignore timestamp
            a.skipBytes(4);
            byte[] rest = new byte[26];
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
                    (byte) (mode == Zip64Mode.Always? 32 : 20), 0,
                    // comment length
                    0, 0,
                    // disk number
                    0, 0,
                    // attributes
                    0, 0,
                    0, 0, 0, 0,
            }, rest, "CDH rest");
            byte[] offset = new byte[4];
            a.readFully(offset);
            if (mode == Zip64Mode.Always) {
                assertArrayEquals(new byte[] {
                        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                    }, offset, "offset");
            } else {
                assertArrayEquals(new byte[] {
                        0, 0, 0, 0,
                    }, offset, "offset");
            }
            assertEquals('0', a.read());
            final byte[] extra = new byte[20];
            a.readFully(extra);
            // 5e9 == 0x12A05F200
            assertArrayEquals(new byte[] {
                    // Header-ID
                    1, 0,
                    // size of extra
                    (byte) (mode == Zip64Mode.Always? 28 : 16), 0,
                    // original size
                    0, (byte) 0xF2, 5, (byte) 0x2A,
                    1, 0, 0, 0,
                    // compressed size
                    0, (byte) 0xF2, 5, (byte) 0x2A,
                    1, 0, 0, 0,
            }, extra, "CDH extra");
            if (mode == Zip64Mode.Always) {
                offset = new byte[8];
                a.readFully(offset);
                assertArrayEquals(new byte[] {
                        0, 0, 0, 0, 0, 0, 0, 0,
                    }, offset, "extra offset");
            }

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
            }, header, "LFH start");
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
            }, rest, "LFH rest");
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
            }, extra, "LFH extra");
        }
         };
    }

    private static ZipOutputTest
        writeBigStoredEntryModeNever(final boolean knownSize) {
        return (f, zos) -> {
            zos.setUseZip64(Zip64Mode.Never);
            final Zip64RequiredException ex = assertThrows(Zip64RequiredException.class, () -> {
                final byte[] buf = new byte[ONE_MILLION];
                final ZipArchiveEntry zae = new ZipArchiveEntry("0");
                if (knownSize) {
                    zae.setSize(FIVE_BILLION);
                    zae.setCrc(0x5c316f50L);
                }
                zae.setMethod(ZipEntry.STORED);
                zos.putArchiveEntry(zae);
                for (int j = 0; j < FIVE_BILLION / 1000 / 1000; j++) {
                    zos.write(buf);
                }
                zos.closeArchiveEntry();
            }, "expected a Zip64RequiredException");
            assertTrue(ex.getMessage().startsWith("0's size"));
        };
    }

    private static ZipOutputTest writeSmallDeflatedEntryToFile(final boolean knownSize) {
        return writeSmallDeflatedEntryToFile(knownSize, Zip64Mode.AsNeeded);
    }

    /*
     * One entry of length 1 million bytes, written with compression
     * to a file.
     *
     * Writing to a file => sizes are stored directly inside the LFH.
     * No Data Descriptor at all.  Shouldn't contain any ZIP64 extra
     * field if size was known.
     */
    private static ZipOutputTest
        writeSmallDeflatedEntryToFile(final boolean knownSize,
                                      final Zip64Mode mode) {
        return (f, zos) -> {
        if (mode != Zip64Mode.AsNeeded) {
            zos.setUseZip64(mode);
        }
        final byte[] buf = new byte[ONE_MILLION];
        final ZipArchiveEntry zae = new ZipArchiveEntry("0");
        if (knownSize) {
            zae.setSize(ONE_MILLION);
        }
        zae.setMethod(ZipEntry.DEFLATED);
        zos.putArchiveEntry(zae);
        zos.write(buf);
        zos.closeArchiveEntry();
        zos.close();

        try (RandomAccessFile a = new RandomAccessFile(f, "r")) {
            getLengthAndPositionAtCentralDirectory(a);

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
                    20, 0,
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
                    20, 0,
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

            final boolean hasExtra =
                    mode == Zip64Mode.AsNeeded && !knownSize;

            assertArrayEquals(new byte[] {
                    // Original Size
                    (byte) 0x40, (byte) 0x42, (byte) 0x0F, 0,
                    // file name length
                    1, 0,
                    // extra field length
                    (byte) (!hasExtra ? 0 : 20), 0,
                    // file name
                    (byte) '0'
            }, rest);
            if (hasExtra) {
                final byte[] extra = new byte[12];
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
        }
         };
    }

    /*
     * One entry of length 1 million bytes, written with compression
     * to a file.
     *
     * Writing to a file => sizes are stored directly inside the LFH.
     * No Data Descriptor at all.  Must contain ZIP64 extra field as
     * mode is Always.
     */
    private static ZipOutputTest
        writeSmallDeflatedEntryToFileModeAlways(final boolean knownSize) {
        return (f, zos) -> {
        zos.setUseZip64(Zip64Mode.Always);
        final byte[] buf = new byte[ONE_MILLION];
        final ZipArchiveEntry zae = new ZipArchiveEntry("0");
        if (knownSize) {
            zae.setSize(ONE_MILLION);
        }
        zae.setMethod(ZipEntry.DEFLATED);
        zos.putArchiveEntry(zae);
        zos.write(buf);
        zos.closeArchiveEntry();
        zos.close();

        try (RandomAccessFile a = new RandomAccessFile(f, "r")) {
            getLengthAndPositionAtCentralDirectory(a);

            // grab first CD entry, verify sizes are not
            // 0xFFFFFFFF and it has a an empty ZIP64 extended
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
            }, header, "CDH start");
            // ignore timestamp
            a.skipBytes(4);
            byte[] crc = new byte[4];
            a.readFully(crc);
            assertArrayEquals(new byte[] {
                    (byte) 0x9E, (byte) 0xCB, (byte) 0x79, (byte) 0x12,
            }, crc, "CDH CRC");
            // skip compressed size
            a.skipBytes(4);
            byte[] rest = new byte[23];
            a.readFully(rest);
            assertArrayEquals(new byte[] {
                    // Original Size
                    (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                    // file name length
                    1, 0,
                    // extra field length
                    32, 0,
                    // comment length
                    0, 0,
                    // disk number
                    0, 0,
                    // attributes
                    0, 0,
                    0, 0, 0, 0,
                    // offset
                    (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                    // file name
                    (byte) '0'
            }, rest, "CDH rest");
            byte[] extra = new byte[12];
            a.readFully(extra);
            assertArrayEquals(new byte[] {
                    // Header-ID
                    1, 0,
                    // size of extra
                    28, 0,
                    // original size
                    (byte) 0x40, (byte) 0x42, (byte) 0x0F, 0,
                    0, 0, 0, 0,
            }, extra, "CDH extra");
            // skip compressed size
            a.skipBytes(8);
            final byte[] offset = new byte[8];
            a.readFully(offset);
            assertArrayEquals(new byte[] {
                    0, 0, 0, 0, 0, 0, 0, 0,
                }, offset, "extra offset");

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
            }, header, "LFH start");
            // ignore timestamp
            a.skipBytes(4);
            crc = new byte[4];
            a.readFully(crc);
            assertArrayEquals(new byte[] {
                    (byte) 0x9E, (byte) 0xCB,
                    (byte) 0x79, (byte) 0x12,
            }, crc, "LFH CRC");
            rest = new byte[13];
            a.readFully(rest);

            assertArrayEquals(new byte[] {
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
            }, rest, "LFH rest");

            extra = new byte[12];
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
            }, extra, "LFH extra");
        }
         };
    }

    /*
     * One entry of length 1 million bytes, written with compression
     * to a stream.
     *
     * Compression + Stream => sizes are set to 0 in LFH, real values
     * are inside the data descriptor.  No ZIP64 extra field at all.
     */
    private static ZipOutputTest
        writeSmallDeflatedEntryToStream(final boolean knownSize,
                                        final Zip64Mode mode) {
        return (f, zos) -> {
        if (mode != Zip64Mode.AsNeeded) {
            zos.setUseZip64(mode);
        }
        final byte[] buf = new byte[ONE_MILLION];
        final ZipArchiveEntry zae = new ZipArchiveEntry("0");
        if (knownSize) {
            zae.setSize(ONE_MILLION);
        }
        zae.setMethod(ZipEntry.DEFLATED);
        zos.putArchiveEntry(zae);
        zos.write(buf);
        zos.closeArchiveEntry();
        zos.close();

        try (RandomAccessFile a = new RandomAccessFile(f, "r")) {
            getLengthAndPositionAtCentralDirectory(a);

            final long cfhPos = a.getFilePointer();
            // grab first entry, verify sizes are not
            // 0xFFFFFFF and it has no ZIP64 extended
            // information extra field
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
            final byte[] crc = new byte[4];
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
                    (byte) 0x9E, (byte) 0xCB, (byte) 0x79, (byte) 0x12,
            }, dd);
            // skip uncompressed size
            a.skipBytes(4);
            dd = new byte[4];
            a.readFully(dd);
            assertArrayEquals(new byte[] {
                    // original size
                    (byte) 0x40, (byte) 0x42, (byte) 0x0F, 0,
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
        }
         };

    }

    /*
     * One entry of length 1 million bytes, written with compression
     * to a stream.
     *
     * Compression + Stream => sizes are set to 0 in LFH, real values
     * are inside the data descriptor.  ZIP64 extra field as mode is Always.
     */
    private static ZipOutputTest
        writeSmallDeflatedEntryToStreamModeAlways(final boolean knownSize) {
        return (f, zos) -> {
        zos.setUseZip64(Zip64Mode.Always);
        final byte[] buf = new byte[ONE_MILLION];
        final ZipArchiveEntry zae = new ZipArchiveEntry("0");
        if (knownSize) {
            zae.setSize(ONE_MILLION);
        }
        zae.setMethod(ZipEntry.DEFLATED);
        zos.putArchiveEntry(zae);
        zos.write(buf);
        zos.closeArchiveEntry();
        zos.close();

        try (RandomAccessFile a = new RandomAccessFile(f, "r")) {
            getLengthAndPositionAtCentralDirectory(a);

            final long cfhPos = a.getFilePointer();
            // grab first entry, verify sizes are not
            // 0xFFFFFFF and it has an empty ZIP64 extended
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
            }, header, "CDH start");
            // ignore timestamp
            a.skipBytes(4);
            final byte[] crc = new byte[4];
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
                    (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                    // file name length
                    1, 0,
                    // extra field length
                    32, 0,
                    // comment length
                    0, 0,
                    // disk number
                    0, 0,
                    // attributes
                    0, 0,
                    0, 0, 0, 0,
                    // offset
                    (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                    // file name
                    (byte) '0'
            }, rest, "CDH rest");
            byte[] extra = new byte[12];
            a.readFully(extra);
            assertArrayEquals(new byte[] {
                    // Header-ID
                    1, 0,
                    // size of extra
                    28, 0,
                    // original size
                    (byte) 0x40, (byte) 0x42, (byte) 0x0F, 0,
                    0, 0, 0, 0,
            }, extra, "CDH extra");
            // skip compressed size
            a.skipBytes(8);
            final byte[] offset = new byte[8];
            a.readFully(offset);
            assertArrayEquals(new byte[] {
                    0, 0, 0, 0, 0, 0, 0, 0,
                }, offset, "extra offset");

            // validate data descriptor
            a.seek(cfhPos - 24);
            byte[] dd = new byte[8];
            a.readFully(dd);
            assertArrayEquals(new byte[] {
                    // sig
                    (byte) 0x50, (byte) 0x4b, 7, 8,
                    // CRC
                    (byte) 0x9E, (byte) 0xCB, (byte) 0x79, (byte) 0x12,
            }, dd, "DD");
            // skip compressed size
            a.skipBytes(8);
            dd = new byte[8];
            a.readFully(dd);
            assertArrayEquals(new byte[] {
                    // original size
                    (byte) 0x40, (byte) 0x42, (byte) 0x0F, 0,
                    0, 0, 0, 0
            }, dd, "DD size");

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
            }, header, "LFH start");
            // ignore timestamp
            a.skipBytes(4);
            rest = new byte[17];
            a.readFully(rest);
            assertArrayEquals(new byte[] {
                    // CRC
                    0, 0, 0, 0,
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
            }, rest, "LFH rest");

            extra = new byte[20];
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
            }, extra, "LFH extra");
        }
         };

    }

    private static ZipOutputTest writeSmallStoredEntry(final boolean knownSize) {
        return writeSmallStoredEntry(knownSize, Zip64Mode.AsNeeded);
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
    private static ZipOutputTest writeSmallStoredEntry(final boolean knownSize,
                                                       final Zip64Mode mode) {
        return (f, zos) -> {
        if (mode != Zip64Mode.AsNeeded) {
            zos.setUseZip64(mode);
        }
        final byte[] buf = new byte[ONE_MILLION];
        final ZipArchiveEntry zae = new ZipArchiveEntry("0");
        if (knownSize) {
            zae.setSize(ONE_MILLION);
            zae.setCrc(0x1279CB9EL);
        }
        zae.setMethod(ZipEntry.STORED);
        zos.putArchiveEntry(zae);
        zos.write(buf);
        zos.closeArchiveEntry();
        zos.close();

        try (RandomAccessFile a = new RandomAccessFile(f, "r")) {
            getLengthAndPositionAtCentralDirectory(a);

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
            }, header, "CDH start");
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
            }, rest, "CDH rest");

            // and now validate local file header: this one
            // has a ZIP64 extra field if and only if size was
            // unknown and mode was not Never or the mode was
            // Always (regardless of size)
            final boolean hasExtra = mode == Zip64Mode.Always
                    || (mode == Zip64Mode.AsNeeded && !knownSize);
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
            }, header, "LFH start");
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
                    (byte) (!hasExtra ? 0 : 20), 0,
                    // file name
                    (byte) '0'
            }, rest, "LFH rest");
            if (hasExtra) {
                final byte[] extra = new byte[20];
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
                }, extra, "ZIP64 extra field");
            }
        }
         };
    }

    /*
     * One entry of length 1 million bytes, written without compression.
     *
     * No Compression => sizes are stored directly inside the LFH.  No
     * Data Descriptor at all. Contains ZIP64 extra fields because
     * mode is Always
     *
     * Creates a temporary archive of approx 1MB in size
     */
    private static ZipOutputTest
        writeSmallStoredEntryModeAlways(final boolean knownSize) {
        return (f, zos) -> {
        zos.setUseZip64(Zip64Mode.Always);
        final byte[] buf = new byte[ONE_MILLION];
        final ZipArchiveEntry zae = new ZipArchiveEntry("0");
        if (knownSize) {
            zae.setSize(ONE_MILLION);
            zae.setCrc(0x1279CB9EL);
        }
        zae.setMethod(ZipEntry.STORED);
        zos.putArchiveEntry(zae);
        zos.write(buf);
        zos.closeArchiveEntry();
        zos.close();

        try (RandomAccessFile a = new RandomAccessFile(f, "r")) {
            getLengthAndPositionAtCentralDirectory(a);

            // grab first CF entry, verify sizes are 1e6 and it
            // has an empty ZIP64 extended information extra field
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
            }, header, "CDH start");
            // ignore timestamp
            a.skipBytes(4);
            byte[] rest = new byte[31];
            a.readFully(rest);
            // 1e6 == 0xF4240
            assertArrayEquals(new byte[] {
                    // CRC
                    (byte) 0x9E, (byte) 0xCB, (byte) 0x79, (byte) 0x12,
                    // Compressed Size
                    (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                    // Original Size
                    (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                    // file name length
                    1, 0,
                    // extra field length
                    32, 0,
                    // comment length
                    0, 0,
                    // disk number
                    0, 0,
                    // attributes
                    0, 0,
                    0, 0, 0, 0,
                    // offset
                    (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                    // file name
                    (byte) '0'
            }, rest, "CDH rest");

            byte[] extra = new byte[28];
            a.readFully(extra);
            assertArrayEquals(new byte[] {
                    // Header-ID
                    1, 0,
                    // size of extra
                    28, 0,
                    // original size
                    (byte) 0x40, (byte) 0x42, (byte) 0x0F, 0,
                    0, 0, 0, 0,
                    // compressed size
                    (byte) 0x40, (byte) 0x42, (byte) 0x0F, 0,
                    0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0,
            }, extra, "CDH extra");

            // and now validate local file header: this one
            // has a ZIP64 extra field as the mode was
            // Always
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
            }, header, "LFH start");
            // ignore timestamp
            a.skipBytes(4);
            rest = new byte[17];
            a.readFully(rest);
            // 1e6 == 0xF4240
            assertArrayEquals(new byte[] {
                    // CRC
                    (byte) 0x9E, (byte) 0xCB, (byte) 0x79, (byte) 0x12,
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
            }, rest, "LFH rest");

            extra = new byte[20];
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
            }, extra, "LFH extra");
        }
         };
    }

    private File buildZipWithZip64Mode(final String fileName, final Zip64Mode zip64Mode, final File inputFile) throws Throwable {
        final File outputFile = getTempFile(fileName);
        outputFile.createNewFile();
        try(ZipArchiveOutputStream zipArchiveOutputStream = new ZipArchiveOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)))) {
            zipArchiveOutputStream.setUseZip64(zip64Mode);
            zipArchiveOutputStream.setCreateUnicodeExtraFields(ZipArchiveOutputStream.UnicodeExtraFieldPolicy.ALWAYS);

            zipArchiveOutputStream.putArchiveEntry(new ZipArchiveEntry("input.bin"));

            Files.copy(inputFile.toPath(), zipArchiveOutputStream);

            zipArchiveOutputStream.closeArchiveEntry();
        }

        return outputFile;
    }

    @Test public void read100KFilesGeneratedBy7ZIPUsingInputStream()
        throws Throwable {
        read100KFilesImpl(get100KFileFileGeneratedBy7ZIP());
    }

    @Test public void read100KFilesGeneratedBy7ZIPUsingZipFile()
        throws Throwable {
        read100KFilesUsingZipFileImpl(get100KFileFileGeneratedBy7ZIP());
    }

    @Test public void read100KFilesGeneratedByJava7JarUsingInputStream()
        throws Throwable {
        read100KFilesImpl(get100KFileFileGeneratedByJava7Jar());
    }

    @Test public void read100KFilesGeneratedByJava7JarUsingZipFile()
        throws Throwable {
        read100KFilesUsingZipFileImpl(get100KFileFileGeneratedByJava7Jar());
    }

    @Test public void read100KFilesGeneratedByPKZipUsingInputStream()
        throws Throwable {
        read100KFilesImpl(get100KFileFileGeneratedByPKZip());
    }

    @Test public void read100KFilesGeneratedByPKZipUsingZipFile()
        throws Throwable {
        read100KFilesUsingZipFileImpl(get100KFileFileGeneratedByPKZip());
    }

    @Test public void read100KFilesGeneratedByWinCFUsingInputStream()
        throws Throwable {
        read100KFilesImpl(get100KFileFileGeneratedByWinCF());
    }

    @Test public void read100KFilesGeneratedByWinCFUsingZipFile()
        throws Throwable {
        read100KFilesUsingZipFileImpl(get100KFileFileGeneratedByWinCF());
    }

    @Test public void read100KFilesGeneratedByWinZIPUsingInputStream()
        throws Throwable {
        read100KFilesImpl(get100KFileFileGeneratedByWinZIP());
    }

    @Test public void read100KFilesGeneratedByWinZIPUsingZipFile()
        throws Throwable {
        read100KFilesUsingZipFileImpl(get100KFileFileGeneratedByWinZIP());
    }

    @Test public void read100KFilesUsingInputStream() throws Throwable {
        read100KFilesImpl(get100KFileFile());
    }

    @Test public void read100KFilesUsingZipFile() throws Throwable {
        read100KFilesUsingZipFileImpl(get100KFileFile());
    }

    @Test public void read3EntriesCreatingBigArchiveFileUsingZipFile()
        throws Throwable {
        withTemporaryArchive("read3EntriesCreatingBigArchiveFileUsingZipFile",
                             (f, zos) -> {
                             write3EntriesCreatingBigArchiveToStream(zos);
                             ZipFile zf = null;
                             try {
                                 zf = new ZipFile(f);
                                 int idx = 0;
                                 for (final Enumeration<ZipArchiveEntry> e =
                                          zf.getEntriesInPhysicalOrder();
                                      e.hasMoreElements(); ) {
                                     final ZipArchiveEntry zae = e.nextElement();
                                     assertEquals(String.valueOf(idx),
                                                  zae.getName());
                                     if (idx++ < 2) {
                                         assertEquals(FIVE_BILLION / 2,
                                                      zae.getSize());
                                     } else {
                                         assertEquals(1,
                                                      zae.getSize());
                                         try (InputStream i = zf.getInputStream(zae)) {
                                             assertNotNull(i);
                                             assertEquals(42, i.read());
                                         }
                                     }
                                 }
                             } finally {
                                 ZipFile.closeQuietly(zf);
                             }
                         },
                             true);
    }

    @Test public void read5GBOfZerosGeneratedBy7ZIPUsingInputStream()
        throws Throwable {
        read5GBOfZerosImpl(get5GBZerosFileGeneratedBy7ZIP(), "5GB_of_Zeros");
    }

    @Test public void read5GBOfZerosGeneratedBy7ZIPUsingZipFile()
        throws Throwable {
        read5GBOfZerosUsingZipFileImpl(get5GBZerosFileGeneratedBy7ZIP(),
                                       "5GB_of_Zeros");
    }

    @Test public void read5GBOfZerosGeneratedByJava7JarUsingInputStream()
        throws Throwable {
        read5GBOfZerosImpl(get5GBZerosFileGeneratedByJava7Jar(), "5GB_of_Zeros");
    }

    @Test public void read5GBOfZerosGeneratedByJava7JarUsingZipFile()
        throws Throwable {
        read5GBOfZerosUsingZipFileImpl(get5GBZerosFileGeneratedByJava7Jar(),
                                       "5GB_of_Zeros");
    }

    @Test public void read5GBOfZerosGeneratedByPKZipUsingInputStream()
        throws Throwable {
        read5GBOfZerosImpl(get5GBZerosFileGeneratedByPKZip(),
                           "zip6/5GB_of_Zeros");
    }

    @Test public void read5GBOfZerosGeneratedByPKZipUsingZipFile()
        throws Throwable {
        read5GBOfZerosUsingZipFileImpl(get5GBZerosFileGeneratedByPKZip(),
                                       "zip6/5GB_of_Zeros");
    }

    @Test public void read5GBOfZerosGeneratedByWinZIPUsingInputStream()
        throws Throwable {
        read5GBOfZerosImpl(get5GBZerosFileGeneratedByWinZIP(), "5GB_of_Zeros");
    }

    @Test public void read5GBOfZerosGeneratedByWinZIPUsingZipFile()
        throws Throwable {
        read5GBOfZerosUsingZipFileImpl(get5GBZerosFileGeneratedByWinZIP(),
                                       "5GB_of_Zeros");
    }

    @Test public void read5GBOfZerosUsingInputStream() throws Throwable {
        read5GBOfZerosImpl(get5GBZerosFile(), "5GB_of_Zeros");
    }

    @Test public void read5GBOfZerosUsingZipFile() throws Throwable {
        read5GBOfZerosUsingZipFileImpl(get5GBZerosFile(), "5GB_of_Zeros");
    }

    @Test public void readSelfGenerated100KFilesUsingZipFile()
        throws Throwable {
        withTemporaryArchive("readSelfGenerated100KFilesUsingZipFile()",
                             (f, zos) -> {
                             write100KFilesToStream(zos);
                             read100KFilesUsingZipFileImpl(f);
                         },
                             true);
    }

    @Test
    public void testZip64ModeAlwaysWithCompatibility() throws Throwable {
        final File inputFile = getFile("test3.xml");

        // with Zip64Mode.AlwaysWithCompatibility, the relative header offset and disk number
        // start will not be set in extra fields
        final File zipUsingModeAlwaysWithCompatibility = buildZipWithZip64Mode(
                "testZip64ModeAlwaysWithCompatibility-output-1",
                Zip64Mode.AlwaysWithCompatibility, inputFile);
        final ZipFile zipFileWithAlwaysWithCompatibility = new ZipFile(zipUsingModeAlwaysWithCompatibility);
        ZipArchiveEntry entry = zipFileWithAlwaysWithCompatibility.getEntries().nextElement();
        for (final ZipExtraField extraField : entry.getExtraFields()) {
            if (!(extraField instanceof Zip64ExtendedInformationExtraField)) {
                continue;
            }

            assertNull(((Zip64ExtendedInformationExtraField) extraField).getRelativeHeaderOffset());
            assertNull(((Zip64ExtendedInformationExtraField) extraField).getDiskStartNumber());
        }

        // with Zip64Mode.Always, the relative header offset and disk number start will be
        // set in extra fields
        final File zipUsingModeAlways = buildZipWithZip64Mode(
                "testZip64ModeAlwaysWithCompatibility-output-2",
                Zip64Mode.Always, inputFile);
        final ZipFile zipFileWithAlways = new ZipFile(zipUsingModeAlways);
        entry = zipFileWithAlways.getEntries().nextElement();
        for (final ZipExtraField extraField : entry.getExtraFields()) {
            if (!(extraField instanceof Zip64ExtendedInformationExtraField)) {
                continue;
            }

            assertNotNull(((Zip64ExtendedInformationExtraField) extraField).getRelativeHeaderOffset());
            assertNotNull(((Zip64ExtendedInformationExtraField) extraField).getDiskStartNumber());
        }
    }

    @Test public void write100KFilesFile() throws Throwable {
        withTemporaryArchive("write100KFilesFile", write100KFiles(), true);
    }

    @Test public void write100KFilesFileModeAlways() throws Throwable {
        withTemporaryArchive("write100KFilesFileModeAlways",
                             write100KFiles(Zip64Mode.Always), true);
    }

    @Test public void write100KFilesFileModeNever() throws Throwable {
        withTemporaryArchive("write100KFilesFileModeNever",
                             write100KFilesModeNever, true);
    }

    @Test public void write100KFilesStream() throws Throwable {
        withTemporaryArchive("write100KFilesStream", write100KFiles(), false);
    }

    @Test public void write100KFilesStreamModeAlways() throws Throwable {
        withTemporaryArchive("write100KFilesStreamModeAlways",
                             write100KFiles(Zip64Mode.Always), false);
    }

    @Test public void write100KFilesStreamModeNever() throws Throwable {
        withTemporaryArchive("write100KFilesStreamModeNever",
                             write100KFilesModeNever, false);
    }

    @Test public void write3EntriesCreatingBigArchiveFile() throws Throwable {
        withTemporaryArchive("write3EntriesCreatingBigArchiveFile",
                             write3EntriesCreatingBigArchive(),
                             true);
    }

    @Test public void write3EntriesCreatingBigArchiveFileModeAlways()
        throws Throwable {
        withTemporaryArchive("write3EntriesCreatingBigArchiveFileModeAlways",
                             write3EntriesCreatingBigArchive(Zip64Mode.Always),
                             true);
    }

    @Test public void write3EntriesCreatingBigArchiveFileModeNever()
        throws Throwable {
        withTemporaryArchive("write3EntriesCreatingBigArchiveFileModeNever",
                             write3EntriesCreatingBigArchiveModeNever,
                             true);
    }

    @Test public void write3EntriesCreatingBigArchiveStream() throws Throwable {
        withTemporaryArchive("write3EntriesCreatingBigArchiveStream",
                             write3EntriesCreatingBigArchive(),
                             false);
    }

    @Test public void write3EntriesCreatingBigArchiveStreamModeAlways()
        throws Throwable {
        withTemporaryArchive("write3EntriesCreatingBigArchiveStreamModeAlways",
                             write3EntriesCreatingBigArchive(Zip64Mode.Always),
                             false);
    }

    @Test public void write3EntriesCreatingBigArchiveStreamModeNever()
        throws Throwable {
        withTemporaryArchive("write3EntriesCreatingBigArchiveStreamModeNever",
                             write3EntriesCreatingBigArchiveModeNever,
                             false);
    }

    @Test
    public void write3EntriesCreatingManySplitArchiveFileModeAlways()
            throws Throwable {
        // about 76,293 ZIP split segments will be created
        withTemporaryArchive("write3EntriesCreatingManySplitArchiveFileModeAlways",
                write3EntriesCreatingBigArchive(Zip64Mode.Always, true),
                true, 65536L);
    }

    @Test
    public void write3EntriesCreatingManySplitArchiveFileModeNever()
            throws Throwable {
        withTemporaryArchive("write3EntriesCreatingManySplitArchiveFileModeNever",
                write3EntriesCreatingBigArchiveModeNever,
                true, 65536L);
    }

    @Test public void writeAndRead5GBOfZerosUsingZipFile() throws Throwable {
        File f = null;
        try {
            f = write5GBZerosFile("writeAndRead5GBOfZerosUsingZipFile");
            read5GBOfZerosUsingZipFileImpl(f, "5GB_of_Zeros");
        } finally {
            if (f != null) {
                AbstractTestCase.tryHardToDelete(f);
            }
        }
    }

    @Test public void writeBigDeflatedEntryKnownSizeToFile()
        throws Throwable {
        withTemporaryArchive("writeBigDeflatedEntryKnownSizeToFile",
                             writeBigDeflatedEntryToFile(true),
                             true);
    }

    @Test public void writeBigDeflatedEntryKnownSizeToFileModeAlways()
        throws Throwable {
        withTemporaryArchive("writeBigDeflatedEntryKnownSizeToFileModeAlways",
                             writeBigDeflatedEntryToFile(true, Zip64Mode.Always),
                             true);
    }

    @Test public void writeBigDeflatedEntryKnownSizeToFileModeNever()
        throws Throwable {
        withTemporaryArchive("writeBigDeflatedEntryKnownSizeToFileModeNever",
                             writeBigDeflatedEntryToFileModeNever(true),
                             true);
    }

    @Test public void writeBigDeflatedEntryKnownSizeToStream()
        throws Throwable {
        withTemporaryArchive("writeBigDeflatedEntryKnownSizeToStream",
                             writeBigDeflatedEntryToStream(true,
                                                           Zip64Mode.AsNeeded),
                             false);
    }

    @Test public void writeBigDeflatedEntryKnownSizeToStreamModeAlways()
        throws Throwable {
        withTemporaryArchive("writeBigDeflatedEntryKnownSizeToStreamModeAlways",
                             writeBigDeflatedEntryToStream(true,
                                                           Zip64Mode.Always),
                             false);
    }

    @Test public void writeBigDeflatedEntryKnownSizeToStreamModeNever()
        throws Throwable {
        withTemporaryArchive("writeBigDeflatedEntryKnownSizeToStreamModeNever",
                (f, zos) -> {
                    zos.setUseZip64(Zip64Mode.Never);
                    final Zip64RequiredException ex = assertThrows(Zip64RequiredException.class, () -> {
                        final ZipArchiveEntry zae =
                                new ZipArchiveEntry("0");
                        zae.setSize(FIVE_BILLION);
                        zae.setMethod(ZipEntry.DEFLATED);
                        zos.putArchiveEntry(zae);
                    }, "expected a Zip64RequiredException");
                    assertTrue(ex.getMessage().startsWith("0's size"));
                }, false);
    }

    @Test public void writeBigDeflatedEntryUnknownSizeToFile()
        throws Throwable {
        withTemporaryArchive("writeBigDeflatedEntryUnknownSizeToFile",
                             writeBigDeflatedEntryToFile(false),
                             true);
    }

    @Test public void writeBigDeflatedEntryUnknownSizeToFileModeAlways()
        throws Throwable {
        withTemporaryArchive("writeBigDeflatedEntryUnknownSizeToFileModeAlways",
                             writeBigDeflatedEntryToFile(false,
                                                         Zip64Mode.Always),
                             true);
    }

    @Test public void writeBigDeflatedEntryUnknownSizeToFileModeNever()
        throws Throwable {
        withTemporaryArchive("writeBigDeflatedEntryUnknownSizeToFileModeNever",
                             writeBigDeflatedEntryToFileModeNever(false),
                             true);
    }

    @Test public void writeBigDeflatedEntryUnknownSizeToStream()
        throws Throwable {
        withTemporaryArchive("writeBigDeflatedEntryUnknownSizeToStream",
                             writeBigDeflatedEntryUnknownSizeToStream(Zip64Mode
                                                                      .AsNeeded),
                             false);
    }

    @Test public void writeBigDeflatedEntryUnknownSizeToStreamModeAlways()
        throws Throwable {
        withTemporaryArchive("writeBigDeflatedEntryUnknownSizeToStreamModeAlways",
                             writeBigDeflatedEntryToStream(false,
                                                           Zip64Mode.Always),
                             false);
    }

    @Test public void writeBigDeflatedEntryUnknownSizeToStreamModeNever()
        throws Throwable {
        withTemporaryArchive("writeBigDeflatedEntryUnknownSizeToStreamModeNever",
                             writeBigDeflatedEntryUnknownSizeToStream(Zip64Mode
                                                                      .Never),
                             false);
    }

    @Test public void writeBigStoredEntryKnownSizeToFile() throws Throwable {
        withTemporaryArchive("writeBigStoredEntryKnownSizeToFile",
                             writeBigStoredEntry(true),
                             true);
    }

    @Test public void writeBigStoredEntryKnownSizeToFileModeAlways()
        throws Throwable {
        withTemporaryArchive("writeBigStoredEntryKnownSizeToFileModeAlways",
                             writeBigStoredEntry(true, Zip64Mode.Always),
                             true);
    }

    @Test public void writeBigStoredEntryKnownSizeToFileModeNever()
        throws Throwable {
        withTemporaryArchive("writeBigStoredEntryKnownSizeToFileModeNever",
                             writeBigStoredEntryModeNever(true),
                             true);
    }

    /*
     * No Compression + Stream => sizes must be known before data is
     * written.
     */
    @Test public void writeBigStoredEntryToStream() throws Throwable {
        withTemporaryArchive("writeBigStoredEntryToStream",
                             writeBigStoredEntry(true),
                             false);
    }

    @Test public void writeBigStoredEntryToStreamModeAlways() throws Throwable {
        withTemporaryArchive("writeBigStoredEntryToStreamModeAlways",
                             writeBigStoredEntry(true, Zip64Mode.Always),
                             false);
    }

    @Test public void writeBigStoredEntryToStreamModeNever() throws Throwable {
        withTemporaryArchive("writeBigStoredEntryToStreamModeNever",
                             writeBigStoredEntryModeNever(true),
                             false);
    }

    @Test public void writeBigStoredEntryUnknownSizeToFile() throws Throwable {
        withTemporaryArchive("writeBigStoredEntryUnknownSizeToFile",
                             writeBigStoredEntry(false),
                             true);
    }

    @Test public void writeBigStoredEntryUnknownSizeToFileModeAlways()
        throws Throwable {
        withTemporaryArchive("writeBigStoredEntryUnknownSizeToFileModeAlways",
                             writeBigStoredEntry(false, Zip64Mode.Always),
                             true);
    }

    @Test public void writeBigStoredEntryUnknownSizeToFileModeNever()
        throws Throwable {
        withTemporaryArchive("writeBigStoredEntryUnknownSizeToFileModeNever",
                             writeBigStoredEntryModeNever(false),
                             true);
    }

    @Test public void writeSmallDeflatedEntryKnownSizeToFile()
        throws Throwable {
        withTemporaryArchive("writeSmallDeflatedEntryKnownSizeToFile",
                             writeSmallDeflatedEntryToFile(true),
                             true);
    }

    @Test public void writeSmallDeflatedEntryKnownSizeToFileModeAlways()
        throws Throwable {
        withTemporaryArchive("writeSmallDeflatedEntryKnownSizeToFileModeAlways",
                             writeSmallDeflatedEntryToFileModeAlways(true),
                             true);
    }

    @Test public void writeSmallDeflatedEntryKnownSizeToFileModeNever()
        throws Throwable {
        withTemporaryArchive("writeSmallDeflatedEntryKnownSizeToFileModeNever",
                             writeSmallDeflatedEntryToFile(true,
                                                           Zip64Mode.Never),
                             true);
    }

    @Test public void writeSmallDeflatedEntryKnownSizeToStream()
        throws Throwable {
        withTemporaryArchive("writeSmallDeflatedEntryKnownSizeToStream",
                             writeSmallDeflatedEntryToStream(true,
                                                             Zip64Mode.AsNeeded),
                             false);
    }

    @Test public void writeSmallDeflatedEntryKnownSizeToStreamModeAlways()
        throws Throwable {
        withTemporaryArchive("writeSmallDeflatedEntryKnownSizeToStreamModeAlways",
                             writeSmallDeflatedEntryToStreamModeAlways(true),
                             false);
    }

    @Test public void writeSmallDeflatedEntryKnownSizeToStreamModeNever()
        throws Throwable {
        withTemporaryArchive("writeSmallDeflatedEntryKnownSizeToStreamModeNever",
                             writeSmallDeflatedEntryToStream(true,
                                                             Zip64Mode.Never),
                             false);
    }

    @Test public void writeSmallDeflatedEntryUnknownSizeToFile()
        throws Throwable {
        withTemporaryArchive("writeSmallDeflatedEntryUnknownSizeToFile",
                             writeSmallDeflatedEntryToFile(false),
                             true);
    }

    @Test public void writeSmallDeflatedEntryUnknownSizeToFileModeAlways()
        throws Throwable {
        withTemporaryArchive("writeSmallDeflatedEntryUnknownSizeToFileModeAlways",
                             writeSmallDeflatedEntryToFileModeAlways(false),
                             true);
    }

    @Test public void writeSmallDeflatedEntryUnknownSizeToFileModeNever()
        throws Throwable {
        withTemporaryArchive("writeSmallDeflatedEntryUnknownSizeToFileModeNever",
                             writeSmallDeflatedEntryToFile(false,
                                                           Zip64Mode.Never),
                             true);
    }

    @Test public void writeSmallDeflatedEntryUnknownSizeToStream()
        throws Throwable {
        withTemporaryArchive("writeSmallDeflatedEntryUnknownSizeToStream",
                             writeSmallDeflatedEntryToStream(false,
                                                             Zip64Mode.AsNeeded),
                             false);
    }

    @Test public void writeSmallDeflatedEntryUnknownSizeToStreamModeAlways()
        throws Throwable {
        withTemporaryArchive("writeSmallDeflatedEntryUnknownSizeToStreamModeAlways",
                             writeSmallDeflatedEntryToStreamModeAlways(false),
                             false);
    }

    @Test public void writeSmallDeflatedEntryUnknownSizeToStreamModeNever()
        throws Throwable {
        withTemporaryArchive("writeSmallDeflatedEntryUnknownSizeToStreamModeNever",
                             writeSmallDeflatedEntryToStream(false,
                                                             Zip64Mode.Never),
                             false);
    }

    @Test public void writeSmallStoredEntryKnownSizeToFile() throws Throwable {
        withTemporaryArchive("writeSmallStoredEntryKnownSizeToFile",
                             writeSmallStoredEntry(true),
                             true);
    }

    @Test public void writeSmallStoredEntryKnownSizeToFileModeAlways()
        throws Throwable {
        withTemporaryArchive("writeSmallStoredEntryKnownSizeToFileModeAlways",
                             writeSmallStoredEntryModeAlways(true),
                             true);
    }

    @Test public void writeSmallStoredEntryKnownSizeToFileModeNever()
        throws Throwable {
        withTemporaryArchive("writeSmallStoredEntryKnownSizeToFileModeNever",
                             writeSmallStoredEntry(true, Zip64Mode.Never),
                             true);
    }

    @Test public void writeSmallStoredEntryToStream() throws Throwable {
        withTemporaryArchive("writeSmallStoredEntryToStream",
                             writeSmallStoredEntry(true),
                             false);
    }

    @Test public void writeSmallStoredEntryToStreamModeAlways()
        throws Throwable {
        withTemporaryArchive("writeSmallStoredEntryToStreamModeAlways",
                             writeSmallStoredEntryModeAlways(true),
                             false);
    }

    @Test public void writeSmallStoredEntryToStreamModeNever() throws Throwable {
        withTemporaryArchive("writeSmallStoredEntryToStreamModeNever",
                             writeSmallStoredEntry(true, Zip64Mode.Never),
                             false);
    }

    @Test public void writeSmallStoredEntryUnknownSizeToFile() throws Throwable {
        withTemporaryArchive("writeSmallStoredEntryUnknownSizeToFile",
                             writeSmallStoredEntry(false),
                             true);
    }

    @Test public void writeSmallStoredEntryUnknownSizeToFileModeAlways()
        throws Throwable {
        withTemporaryArchive("writeSmallStoredEntryUnknownSizeToFileModeAlways",
                             writeSmallStoredEntryModeAlways(false),
                             true);
    }

    @Test public void writeSmallStoredEntryUnknownSizeToFileModeNever()
        throws Throwable {
        withTemporaryArchive("writeSmallStoredEntryUnknownSizeToFileModeNever",
                             writeSmallStoredEntry(false, Zip64Mode.Never),
                             true);
    }
}
