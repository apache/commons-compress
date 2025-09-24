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
import org.apache.commons.compress.archivers.dump.DumpArchiveConstants;
import org.apache.commons.compress.archivers.dump.DumpArchiveConstants.SEGMENT_TYPE;
import org.apache.commons.compress.archivers.dump.DumpArchiveEntry;
import org.apache.commons.compress.archivers.dump.DumpArchiveEntry.TYPE;

public class DumpLocalHeader {
    static final int RECORD_SIZE = 1024;

    private final SEGMENT_TYPE segmentType;
    private final int dumpDate = 0; // date of this dump (seconds since epoch)
    private final int previousDumpDate = 0; // date of previous dump (seconds since epoch)
    private final int volume; // volume number
    private final int tapeAddress = 0; // tape address of this record
    private final int inode; // inode number
    private final TYPE entryType; // type of file

    @SuppressWarnings("OctalInteger")
    private final short mode = (short) 00644; // mode

    private final short nlink = 1; // number of links to file
    private final long fileSize; // size of file in bytes
    private final int count; // number of blocks (1024-byte) in this record

    public DumpLocalHeader(SEGMENT_TYPE segmentType, TYPE entryType, int volume, int inode, long fileSize) {
        this.segmentType = segmentType;
        this.entryType = entryType;
        this.volume = volume;
        this.inode = inode;
        this.fileSize = fileSize;
        this.count = (int) ((fileSize + 1023) / 1024); // number of 1024-byte blocks
    }

    public void writeTo(final ByteBuffer out) {
        final int need = RECORD_SIZE;
        if (out.remaining() < need) {
            throw new IllegalArgumentException("Not enough space: need " + need + " bytes");
        }
        final int offset = out.position();
        out.order(ByteOrder.LITTLE_ENDIAN);
        out.putInt(segmentType.code); // segment type
        out.putInt(dumpDate); // date of this dump
        out.putInt(previousDumpDate); // date of previous dump
        out.putInt(volume); // volume number
        out.putInt(tapeAddress); // tape address of this record
        out.putInt(inode); // inode number
        out.putInt(DumpArchiveConstants.NFS_MAGIC); // magic
        out.putInt(0); // clear checksum for now
        out.putShort((short) (entryType.code << 12 | mode & 0xfff)); // mode and type of file
        out.putShort(nlink); // number of links to file
        out.putInt(0); // inumber
        out.putLong(fileSize); // size of file in bytes
        // Fill the rest of the record with zeros
        for (int i = 48; i < RECORD_SIZE; i++) {
            out.put((byte) 0);
        }
        out.putInt(offset + 160, count); // number of blocks (512-byte) in this record
        // Compute and write checksum
        ByteBuffer checksumBuffer = out.duplicate();
        checksumBuffer.order(ByteOrder.LITTLE_ENDIAN).position(offset).limit(offset + RECORD_SIZE);
        int checksum = 0;
        for (int i = 0; i < RECORD_SIZE / 4; i++) {
            checksum += checksumBuffer.getInt();
        }
        out.putInt(offset + 28, DumpArchiveConstants.CHECKSUM - checksum);
    }
}
