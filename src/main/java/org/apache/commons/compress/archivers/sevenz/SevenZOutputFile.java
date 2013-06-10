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
package org.apache.commons.compress.archivers.sevenz;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Date;
import java.util.List;
import java.util.zip.CRC32;

import org.apache.commons.compress.archivers.ArchiveEntry;

/**
 * Writes a 7z file.
 */
public class SevenZOutputFile {
    private final RandomAccessFile file;
    private final List<SevenZArchiveEntry> files = new ArrayList<SevenZArchiveEntry>();
    private int numNonEmptyStreams = 0;
    private CRC32 crc32 = new CRC32();
    private long fileBytesWritten = 0;
    private boolean finished = false;
    
    public SevenZOutputFile(final File filename) throws IOException {
        file = new RandomAccessFile(filename, "rw");
        file.seek(SevenZFile.SIGNATURE_HEADER_SIZE);
    }
    
    public void close() {
        try {
            if (!finished) {
                finish();
            }
            file.close();
        } catch (IOException ioEx) { // NOPMD
        }
    }
    
    public SevenZArchiveEntry createArchiveEntry(final File inputFile,
            final String entryName) throws IOException {
        final SevenZArchiveEntry entry = new SevenZArchiveEntry();
        entry.setDirectory(inputFile.isDirectory());
        entry.setName(entryName);
        entry.setHasLastModifiedDate(true);
        entry.setLastModifiedDate(new Date(inputFile.lastModified()));
        return entry;
    }

    public void putArchiveEntry(final ArchiveEntry archiveEntry) throws IOException {
        final SevenZArchiveEntry entry = (SevenZArchiveEntry) archiveEntry;
        files.add(entry);
    }
    
    public void closeArchiveEntry() throws IOException {
        final SevenZArchiveEntry entry = files.get(files.size() - 1);
        if (fileBytesWritten > 0) {
            entry.setHasStream(true);
            ++numNonEmptyStreams;
            entry.setSize(fileBytesWritten);
            entry.setCrc((int) crc32.getValue());
            entry.setHasCrc(true);
        } else {
            entry.setHasStream(false);
            entry.setSize(0);
            entry.setHasCrc(false);
        }
        crc32.reset();
        fileBytesWritten = 0;
    }
    
    public void write(final int b) throws IOException {
        file.write(b);
        crc32.update(b);
        fileBytesWritten++;
    }
    
    public void write(final byte[] b) throws IOException {
        write(b, 0, b.length);
    }
    
    public void write(final byte[] b, final int off, final int len) throws IOException {
        file.write(b, off, len);
        crc32.update(b, off, len);
        fileBytesWritten += len;
    }
    
    public void finish() throws IOException {
        if (finished) {
            throw new IOException("This archive has already been finished");
        }
        finished = true;
        
        final long headerPosition = file.getFilePointer();
        
        final ByteArrayOutputStream headerBaos = new ByteArrayOutputStream();
        final DataOutputStream header = new DataOutputStream(headerBaos);
        
        writeHeader(header);
        header.flush();
        final byte[] headerBytes = headerBaos.toByteArray();
        file.write(headerBytes);
        
        final CRC32 crc32 = new CRC32();
        
        // signature header
        file.seek(0);
        file.write(SevenZFile.sevenZSignature);
        // version
        file.write(0);
        file.write(2);
        
        // start header
        final ByteArrayOutputStream startHeaderBaos = new ByteArrayOutputStream();
        final DataOutputStream startHeaderStream = new DataOutputStream(startHeaderBaos);
        startHeaderStream.writeLong(Long.reverseBytes(headerPosition - SevenZFile.SIGNATURE_HEADER_SIZE));
        startHeaderStream.writeLong(Long.reverseBytes(0xffffFFFFL & headerBytes.length));
        crc32.reset();
        crc32.update(headerBytes);
        startHeaderStream.writeInt(Integer.reverseBytes((int)crc32.getValue()));
        startHeaderStream.flush();
        final byte[] startHeaderBytes = startHeaderBaos.toByteArray();
        crc32.reset();
        crc32.update(startHeaderBytes);
        file.writeInt(Integer.reverseBytes((int)crc32.getValue()));
        file.write(startHeaderBytes);
    }
    
    private void writeHeader(final DataOutput header) throws IOException {
        header.write(NID.kHeader);
        
        header.write(NID.kMainStreamsInfo);
        writeStreamsInfo(header);
        writeFilesInfo(header);
        header.write(NID.kEnd);
    }
    
    private void writeStreamsInfo(final DataOutput header) throws IOException {
        if (numNonEmptyStreams > 0) {
            writePackInfo(header);
            writeUnpackInfo(header);
        }
        
        writeSubStreamsInfo(header);
        
        header.write(NID.kEnd);
    }
    
    private void writePackInfo(final DataOutput header) throws IOException {
        // FIXME: this needs to use the compressed sizes/CRCs when we start supporting compression.
        header.write(NID.kPackInfo);
        
        writeUint64(header, 0);
        writeUint64(header, 0xffffFFFFL & numNonEmptyStreams);
        
        header.write(NID.kSize);
        for (final SevenZArchiveEntry entry : files) {
            if (entry.hasStream()) {
                writeUint64(header, entry.getSize());
            }
        }
        
        header.write(NID.kCRC);
        header.write(1);
        for (final SevenZArchiveEntry entry : files) {
            if (entry.hasStream()) {
                header.writeInt(Integer.reverseBytes(entry.getCrc()));
            }
        }
        
        header.write(NID.kEnd);
    }
    
    private void writeUnpackInfo(final DataOutput header) throws IOException {
        header.write(NID.kUnpackInfo);
        
        header.write(NID.kFolder);
        // FIXME: add real support for solid compression, and actual compression methods
        writeUint64(header, numNonEmptyStreams);
        header.write(0);
        for (int i = 0; i < numNonEmptyStreams; i++) {
            writeFolder(header);
        }
        
        header.write(NID.kCodersUnpackSize);
        for (final SevenZArchiveEntry entry : files) {
            if (entry.hasStream()) {
                writeUint64(header, entry.getSize());
            }
        }
        
        header.write(NID.kCRC);
        header.write(1);
        for (final SevenZArchiveEntry entry : files) {
            if (entry.hasStream()) {
                header.writeInt(Integer.reverseBytes(entry.getCrc()));
            }
        }
        
        header.write(NID.kEnd);
    }
    
    private void writeFolder(final DataOutput header) throws IOException {
        writeUint64(header, 1);
        header.write(1);
        header.write(0);
    }
    
    private void writeSubStreamsInfo(final DataOutput header) throws IOException {
        header.write(NID.kSubStreamsInfo);
//        
//        header.write(NID.kCRC);
//        header.write(1);
//        for (final SevenZArchiveEntry entry : files) {
//            if (entry.getHasCrc()) {
//                header.writeInt(Integer.reverseBytes(entry.getCrc()));
//            }
//        }
//        
        header.write(NID.kEnd);
    }
    
    private void writeFilesInfo(final DataOutput header) throws IOException {
        header.write(NID.kFilesInfo);
        
        writeUint64(header, files.size());

        writeFileEmptyStreams(header);
        writeFileEmptyFiles(header);
        writeFileAntiItems(header);
        writeFileNames(header);
        writeFileCTimes(header);
        writeFileATimes(header);
        writeFileMTimes(header);
        writeFileWindowsAttributes(header);
        header.write(0);
    }
    
    private void writeFileEmptyStreams(final DataOutput header) throws IOException {
        boolean hasEmptyStreams = false;
        for (final SevenZArchiveEntry entry : files) {
            if (!entry.hasStream()) {
                hasEmptyStreams = true;
                break;
            }
        }
        if (hasEmptyStreams) {
            header.write(NID.kEmptyStream);
            final BitSet emptyStreams = new BitSet(files.size());
            for (int i = 0; i < files.size(); i++) {
                emptyStreams.set(i, !files.get(i).hasStream());
            }
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream out = new DataOutputStream(baos);
            writeBits(out, emptyStreams, files.size());
            out.flush();
            final byte[] contents = baos.toByteArray();
            writeUint64(header, contents.length);
            header.write(contents);
        }
    }
    
    private void writeFileEmptyFiles(final DataOutput header) throws IOException {
        boolean hasEmptyFiles = false;
        for (final SevenZArchiveEntry entry : files) {
            if (!entry.hasStream() && !entry.isDirectory()) {
                hasEmptyFiles = true;
                break;
            }
        }
        if (hasEmptyFiles) {
            header.write(NID.kEmptyFile);
            final BitSet emptyFiles = new BitSet(files.size());
            for (int i = 0; i < files.size(); i++) {
                emptyFiles.set(i, !files.get(i).hasStream() && !files.get(i).isDirectory());
            }
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream out = new DataOutputStream(baos);
            writeBits(out, emptyFiles, files.size());
            out.flush();
            final byte[] contents = baos.toByteArray();
            writeUint64(header, contents.length);
            header.write(contents);
        }
    }
    
    private void writeFileAntiItems(final DataOutput header) throws IOException {
        boolean hasAntiItems = false;
        for (final SevenZArchiveEntry entry : files) {
            if (entry.isAntiItem()) {
                hasAntiItems = true;
                break;
            }
        }
        if (hasAntiItems) {
            header.write(NID.kAnti);
            final BitSet antiItems = new BitSet(files.size());
            for (int i = 0; i < files.size(); i++) {
                antiItems.set(i, files.get(i).isAntiItem());
            }
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream out = new DataOutputStream(baos);
            writeBits(out, antiItems, files.size());
            out.flush();
            final byte[] contents = baos.toByteArray();
            writeUint64(header, contents.length);
            header.write(contents);
        }
    }
    
    private void writeFileNames(final DataOutput header) throws IOException {
        header.write(NID.kName);
        
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final DataOutputStream out = new DataOutputStream(baos);
        out.write(0);
        for (final SevenZArchiveEntry entry : files) {
            out.write(entry.getName().getBytes("UTF-16LE"));
            out.writeShort(0);
        }
        out.flush();
        final byte[] contents = baos.toByteArray();
        writeUint64(header, contents.length);
        header.write(contents);
    }

    private void writeFileCTimes(final DataOutput header) throws IOException {
        int numCreationDates = 0;
        for (final SevenZArchiveEntry entry : files) {
            if (entry.getHasCreationDate()) {
                ++numCreationDates;
            }
        }
        if (numCreationDates > 0) {
            header.write(NID.kCTime);

            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream out = new DataOutputStream(baos);
            if (numCreationDates != files.size()) {
                out.write(0);
                final BitSet cTimes = new BitSet(files.size());
                for (int i = 0; i < files.size(); i++) {
                    cTimes.set(i, files.get(i).getHasCreationDate());
                }
                writeBits(out, cTimes, files.size());
            } else {
                out.write(1);
            }
            out.write(0);
            for (final SevenZArchiveEntry entry : files) {
                if (entry.getHasCreationDate()) {
                    out.writeLong(Long.reverseBytes(
                            SevenZArchiveEntry.javaTimeToNtfsTime(entry.getCreationDate())));
                }
            }
            out.flush();
            final byte[] contents = baos.toByteArray();
            writeUint64(header, contents.length);
            header.write(contents);
        }
    }

    private void writeFileATimes(final DataOutput header) throws IOException {
        int numAccessDates = 0;
        for (final SevenZArchiveEntry entry : files) {
            if (entry.getHasAccessDate()) {
                ++numAccessDates;
            }
        }
        if (numAccessDates > 0) {
            header.write(NID.kATime);

            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream out = new DataOutputStream(baos);
            if (numAccessDates != files.size()) {
                out.write(0);
                final BitSet aTimes = new BitSet(files.size());
                for (int i = 0; i < files.size(); i++) {
                    aTimes.set(i, files.get(i).getHasAccessDate());
                }
                writeBits(out, aTimes, files.size());
            } else {
                out.write(1);
            }
            out.write(0);
            for (final SevenZArchiveEntry entry : files) {
                if (entry.getHasAccessDate()) {
                    out.writeLong(Long.reverseBytes(
                            SevenZArchiveEntry.javaTimeToNtfsTime(entry.getAccessDate())));
                }
            }
            out.flush();
            final byte[] contents = baos.toByteArray();
            writeUint64(header, contents.length);
            header.write(contents);
        }
    }

    private void writeFileMTimes(final DataOutput header) throws IOException {
        int numLastModifiedDates = 0;
        for (final SevenZArchiveEntry entry : files) {
            if (entry.getHasLastModifiedDate()) {
                ++numLastModifiedDates;
            }
        }
        if (numLastModifiedDates > 0) {
            header.write(NID.kMTime);

            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream out = new DataOutputStream(baos);
            if (numLastModifiedDates != files.size()) {
                out.write(0);
                final BitSet mTimes = new BitSet(files.size());
                for (int i = 0; i < files.size(); i++) {
                    mTimes.set(i, files.get(i).getHasLastModifiedDate());
                }
                writeBits(out, mTimes, files.size());
            } else {
                out.write(1);
            }
            out.write(0);
            for (final SevenZArchiveEntry entry : files) {
                if (entry.getHasLastModifiedDate()) {
                    out.writeLong(Long.reverseBytes(
                            SevenZArchiveEntry.javaTimeToNtfsTime(entry.getLastModifiedDate())));
                }
            }
            out.flush();
            final byte[] contents = baos.toByteArray();
            writeUint64(header, contents.length);
            header.write(contents);
        }
    }

    private void writeFileWindowsAttributes(final DataOutput header) throws IOException {
        int numWindowsAttributes = 0;
        for (final SevenZArchiveEntry entry : files) {
            if (entry.getHasWindowsAttributes()) {
                ++numWindowsAttributes;
            }
        }
        if (numWindowsAttributes > 0) {
            header.write(NID.kWinAttributes);

            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream out = new DataOutputStream(baos);
            if (numWindowsAttributes != files.size()) {
                out.write(0);
                final BitSet attributes = new BitSet(files.size());
                for (int i = 0; i < files.size(); i++) {
                    attributes.set(i, files.get(i).getHasWindowsAttributes());
                }
                writeBits(out, attributes, files.size());
            } else {
                out.write(1);
            }
            out.write(0);
            for (final SevenZArchiveEntry entry : files) {
                if (entry.getHasWindowsAttributes()) {
                    out.writeInt(Integer.reverseBytes(entry.getWindowsAttributes()));
                }
            }
            out.flush();
            final byte[] contents = baos.toByteArray();
            writeUint64(header, contents.length);
            header.write(contents);
        }
    }

    private void writeUint64(final DataOutput header, long value) throws IOException {
        int firstByte = 0;
        int mask = 0x80;
        int i;
        for (i = 0; i < 8; i++) {
            if (value < ((1L << ( 7  * (i + 1))))) {
                firstByte |= (value >>> (8 * i));
                break;
            }
            firstByte |= mask;
            mask >>>= 1;
        }
        header.write(firstByte);
        for (; i > 0; i--) {
            header.write((int) (0xff & value));
            value >>>= 8;
        }
    }

    private void writeBits(final DataOutput header, final BitSet bits, final int length) throws IOException {
        int cache = 0;
        int shift = 7;
        for (int i = 0; i < length; i++) {
            cache |= ((bits.get(i) ? 1 : 0) << shift);
            --shift;
            if (shift == 0) {
                header.write(cache);
                shift = 7;
                cache = 0;
            }
        }
        if (length > 0 && shift > 0) {
            header.write(cache);
        }
    }
}
