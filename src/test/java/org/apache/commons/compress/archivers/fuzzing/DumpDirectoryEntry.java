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
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * One on-tape directory record in the classic BSD "odirect" form:
 *
 *   u32 d_ino;
 *   u16 d_reclen;  // total record length, incl. header, name+NUL, and padding
 *   u16 d_namlen;  // name length (without the trailing NUL)
 *   char d_name[d_namlen + 1]; // NUL-terminated
 *   // pad with NULs to the next 4-byte boundary
 *
 * All integer fields are written big-endian for portability in test fixtures.
 */
public final class DumpDirectoryEntry {

    private static final int HEADER_SIZE = 4 /*ino*/ + 2 /*reclen*/ + 2 /*entry type and name length*/;

    private final int inode;          // d_ino (unsigned 32)
    private final byte entryType = 0; // entry type
    private final byte nameLength;
    private final String name;        // ASCII preferred for portability

    public DumpDirectoryEntry(final int inode, final String name) {
        if (name.length() > 255) {
            throw new IllegalArgumentException("Name too long: " + name.length());
        }
        this.inode = inode;
        this.nameLength = (byte) name.length();
        this.name = Objects.requireNonNull(name, "name");
    }

    /** Length of this record when written, including header, NUL, and 4-byte padding. */
    public int recordLength() {
        final int namelen = nameBytes().length;
        final int withNul = namelen + 1;
        final int padded = (withNul + 3) & ~3; // round up to multiple of 4
        return HEADER_SIZE + padded;
    }

    /** Writes this odirect record at the buffer's current position. */
    public void writeTo(final ByteBuffer out) {
        final byte[] nameBytes = nameBytes();
        final int length = recordLength();

        if (out.remaining() < length) {
            throw new IllegalArgumentException("Not enough space: need " + length + " bytes");
        }

        out.order(ByteOrder.LITTLE_ENDIAN);
        out.putInt(inode);                    // d_ino
        out.putShort((short) length);         // record length
        out.put(entryType);
        out.put(nameLength);                  // name length

        out.put(nameBytes);
        out.put((byte) 0);                    // terminating NUL

        // pad to 4-byte boundary
        int pad = (4 - ((nameBytes.length + 1) & 3)) & 3;
        for (int i = 0; i < pad; i++) out.put((byte) 0);
    }

    private byte[] nameBytes() {
        // For test fixtures, ASCII keeps byte-for-char mapping simple.
        return name.getBytes(StandardCharsets.US_ASCII);
    }
}
