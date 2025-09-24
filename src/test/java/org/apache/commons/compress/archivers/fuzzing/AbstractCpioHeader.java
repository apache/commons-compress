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

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;

public abstract class AbstractCpioHeader {

    @SuppressWarnings("OctalInteger")
    final short magic = 070707; // CPIO binary magic number
    final short dev = 0; // Device number
    final short ino = 0; // Inode number
    @SuppressWarnings("OctalInteger")
    final short mode = (short) 0100644; // Regular file with 0644 permissions
    final short uid = 0; // User ID
    final short gid = 0; // Group ID
    final short nlink = 1; // Number of links
    final short rdev = 0; // Device number (if special file)
    final int mtime = 0; // Modification time
    final int fileSize; // Size of the file in bytes

    final String fileName;
    final long fileNameSize;
    final byte[] fileNameBytes;

    public AbstractCpioHeader(Charset charset, String fileName, int fileSize) {
        this(charset, fileName, fileName.length(), fileSize);
    }

    public AbstractCpioHeader(Charset charset, String fileName, long fileNameSize, int fileSize) {
        this.fileName = fileName;
        this.fileNameBytes = fileName.getBytes(charset);
        this.fileNameSize = fileNameSize;
        this.fileSize = fileSize;
    }

    long getNameSize() {
        return fileNameSize + 1; // +1 for null terminator
    }

    /** Write file name + NUL via a Writer (PrintWriter) */
    void writeNameWithNull(Writer out) throws IOException {
        out.write(fileName);
        out.write('\0');
    }

    /** Write padding bytes (as NULs) using a Writer. */
    void pad(Writer out, long count) throws IOException {
        for (long i = 0; i < count; i++) out.write('\0');
    }

    /** 4-byte padding after newc/crc header+name or data. */
    static long pad4(long len) { return (4 - (len & 3)) & 3; }

    /** even (2) padding for bin/odc after name or data. */
    static long pad2(long len) { return len & 1; }
}
