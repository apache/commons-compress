/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.compress.archivers.fuzzing;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.zip.CRC32;

/**
 * Writes a single Central Directory File Header (CD) for a simple file.
 * This is NOT the local file header. Structure (per PKWARE APPNOTE):
 *
 *  Offset Size  Field
 *  ------ ----  --------------------------------------------
 *  0      4     Signature (0x02014b50)
 *  4      2     Version made by
 *  6      2     Version needed to extract
 *  8      2     General purpose bit flag
 *  10     2     Compression method
 *  12     2     Last mod file time (DOS)
 *  14     2     Last mod file date (DOS)
 *  16     4     CRC-32
 *  20     4     Compressed size
 *  24     4     Uncompressed size
 *  28     2     File name length (n)
 *  30     2     Extra field length (m)
 *  32     2     File comment length (k)
 *  34     2     Disk number start
 *  36     2     Internal file attributes
 *  38     4     External file attributes
 *  42     4     Relative offset of local header
 *  46     n     File name
 *  46+n   m     Extra field
 *  46+n+m k     File comment
 */
public class ZipCentralDirectoryHeader {
    private static final byte[] MAGIC = {0x50, 0x4b, 0x01, 0x02}; // 0x02014b50 little-endian in stream
    private static final int FIXED_SIZE = 46;

    // For a minimal entry:
    private final short versionMadeBy; // (host OS << 8) | version; use Unix (3) + 2.0 (20)
    private final short minVersionToExtract = 20; // 2.0
    private final short generalPurposeBitFlag = 0; // no flags
    private final short compressionMethod = 0; // store (no compression)
    private final short lastModFileTime = 0; // optional: 0
    private final short lastModFileDate = 0; // optional: 0
    private final int crc32; // CRC32 of uncompressed data (0 for empty)
    private final int compressedSize; // 0 for empty+stored
    private final int uncompressedSize; // 0 for empty
    private final byte[] fileNameBytes; // file name (no directory separator normalization here)
    private final short extraFieldLength = 0; // none
    private final byte[] fileCommentBytes; // optional comment (empty here)
    private final short diskNumberStart = 0; // single-disk archives
    private final short internalFileAttributes = 0; // 0
    private final int externalFileAttributes = 0; // simple default (e.g., 0)
    private final int localHeaderOffset; // relative offset of the corresponding local header

    /**
     * Minimal constructor for an empty, uncompressed file with no comment.
     *
     * @param charset charset for encoding the file name
     * @param fileName file name to store
     * @param localHeaderOffset offset (from start of ZIP) of this entry's local file header
     */
    public ZipCentralDirectoryHeader(Charset charset, String fileName, int localHeaderOffset) {
        this(charset, fileName, localHeaderOffset, new byte[0], 0, 0);
    }

    /**
     * General constructor (still simple): lets you pass sizes and an optional comment.
     */
    public ZipCentralDirectoryHeader(
            Charset charset,
            String fileName,
            int localHeaderOffset,
            byte[] fileCommentBytes,
            int compressedSize,
            int uncompressedSize) {
        // Version made by: host OS = Unix (3), version = 2.0 (20).
        this.versionMadeBy = (short) ((3 << 8) | 20);
        this.fileNameBytes = fileName.getBytes(charset);
        this.fileCommentBytes = (fileCommentBytes == null) ? new byte[0] : fileCommentBytes;
        this.localHeaderOffset = localHeaderOffset;
        this.compressedSize = compressedSize;
        this.uncompressedSize = uncompressedSize;

        // Compute CRC32 of uncompressed data if you have it; for empty it's 0.
        CRC32 crc = new CRC32();
        // For empty file we don't update the CRC with any data.
        this.crc32 = (int) crc.getValue(); // 0 for empty
    }

    private int getHeaderLength() {
        return FIXED_SIZE + fileNameBytes.length + extraFieldLength + fileCommentBytes.length;
    }

    public void writeTo(ByteBuffer output) throws java.io.IOException {
        if (output.remaining() < getHeaderLength()) {
            throw new java.io.IOException(
                    "Not enough space in output buffer: need " + getHeaderLength() + ", have " + output.remaining());
        }
        output.order(ByteOrder.LITTLE_ENDIAN);

        // Signature
        output.put(MAGIC);

        // Fixed fields
        output.putShort(versionMadeBy);
        output.putShort(minVersionToExtract);
        output.putShort(generalPurposeBitFlag);
        output.putShort(compressionMethod);
        output.putShort(lastModFileTime);
        output.putShort(lastModFileDate);
        output.putInt(crc32);
        output.putInt(compressedSize);
        output.putInt(uncompressedSize);
        output.putShort((short) fileNameBytes.length);
        output.putShort(extraFieldLength);
        output.putShort((short) fileCommentBytes.length);
        output.putShort(diskNumberStart);
        output.putShort(internalFileAttributes);
        output.putInt(externalFileAttributes);
        output.putInt(localHeaderOffset);

        // Variable fields
        output.put(fileNameBytes);
        // no extra field
        if (fileCommentBytes.length > 0) {
            output.put(fileCommentBytes);
        }
    }
}
