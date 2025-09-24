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

import static org.apache.commons.compress.archivers.fuzzing.DumpLocalHeader.RECORD_SIZE;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.apache.commons.compress.archivers.dump.DumpArchiveConstants;

/**
 * Minimal "summary/label" block to make a synthetic dump look plausible.
 * Fixed-size, NUL-padded ASCII fields; big-endian integers for portability.
 */
public final class DumpSummaryHeader {

    private final int dumpDate; // seconds since epoch
    private final int previousDumpDate; // seconds since epoch
    private final int volume; // tape/volume number
    private final String label; // 16 bytes, NUL-padded
    private final int level; // dump level
    private final String filesys; // 32 bytes, NUL-padded
    private final String devname; // 32 bytes, NUL-padded
    private final String hostname; // 64 bytes, NUL-padded
    private final int flags; // misc flags
    private final int firstRecord; // first record index
    private final int recordCount; // total records

    public DumpSummaryHeader(int recordCount) {
        this(0, 0, 0, "label", 0, "filesys", "devname", "hostname", 0, 0, recordCount);
    }

    public DumpSummaryHeader(
            int dumpDate,
            int previousDumpDate,
            int volume,
            String label,
            int level,
            String filesys,
            String devname,
            String hostname,
            int flags,
            int firstRecord,
            int recordCount) {
        this.dumpDate = dumpDate;
        this.previousDumpDate = previousDumpDate;
        this.volume = volume;
        this.label = Objects.requireNonNull(label, "label");
        this.level = level;
        this.filesys = Objects.requireNonNull(filesys, "filesys");
        this.devname = Objects.requireNonNull(devname, "devname");
        this.hostname = Objects.requireNonNull(hostname, "hostname");
        this.flags = flags;
        this.firstRecord = firstRecord;
        this.recordCount = recordCount;
    }

    /** Size of this header when written. */
    public int length() {
        return 8 + 8 + 4 + 16 + 4 + 32 + 32 + 64 + 4 + 4 + 4;
    }

    public void writeTo(final ByteBuffer out) {
        final int need = length();
        if (out.remaining() < need) {
            throw new IllegalArgumentException("Not enough space: need " + need + " bytes");
        }
        int start = out.position();
        out.order(ByteOrder.LITTLE_ENDIAN);
        out.putInt(0); // unused
        out.putInt(dumpDate);
        out.putInt(previousDumpDate);
        out.putInt(volume);
        // Zero up to offset 676
        while (out.position() < start + 676) {
            out.put((byte) 0);
        }
        // Magic
        out.putInt(start + 24, DumpArchiveConstants.NFS_MAGIC);
        // Now the fixed-size fields
        putFixed(out, label, 16);
        out.putInt(level);
        putFixed(out, filesys, DumpArchiveConstants.NAMELEN);
        putFixed(out, devname, DumpArchiveConstants.NAMELEN);
        putFixed(out, hostname, DumpArchiveConstants.NAMELEN);
        out.putInt(flags);
        out.putInt(firstRecord);
        out.putInt(recordCount);
        // Zero up to 1024 bytes
        while (out.position() < start + 1024) {
            out.put((byte) 0);
        }
        // Compute and write checksum
        ByteBuffer checksumBuffer = out.duplicate();
        checksumBuffer.order(ByteOrder.LITTLE_ENDIAN).position(start).limit(start + RECORD_SIZE);
        int checksum = 0;
        for (int i = 0; i < RECORD_SIZE / 4; i++) {
            checksum += checksumBuffer.getInt();
        }
        out.putInt(start + 28, DumpArchiveConstants.CHECKSUM - checksum);
    }

    private static void putFixed(final ByteBuffer out, final String s, final int len) {
        final byte[] b = s.getBytes(StandardCharsets.US_ASCII);
        final int n = Math.min(b.length, len);
        out.put(b, 0, n);
        for (int i = n; i < len; i++) out.put((byte) 0);
    }
}
