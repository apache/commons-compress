/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.compress.archivers.dump;


/**
 * Various utilities for dump archives.
 */
class DumpArchiveUtil {
    /**
     * Private constructor to prevent instantiation.
     */
    private DumpArchiveUtil() {
    }

    /**
     * Calculate checksum for buffer.
     *
     * @param buffer buffer containing tape segment header
     * @returns checksum
     */
    public static int calculateChecksum(byte[] buffer) {
        int calc = 0;

        for (int i = 0; i < 256; i++) {
            calc += DumpArchiveUtil.convert32(buffer, 4 * i);
        }

        return DumpArchiveConstants.CHECKSUM -
        (calc - DumpArchiveUtil.convert32(buffer, 28));
    }

    /**
     * Verify that the buffer contains a tape segment header.
     *
     * @param buffer
     */
    public static final boolean verify(byte[] buffer) {
        // verify magic. for now only accept NFS_MAGIC.
        int magic = convert32(buffer, 24);

        if (magic != DumpArchiveConstants.NFS_MAGIC) {
            return false;
        }

        //verify checksum...
        int checksum = convert32(buffer, 28);

        if (checksum != calculateChecksum(buffer)) {
            return false;
        }

        return true;
    }

    /**
     * Get the ino associated with this buffer.
     *
     * @param buffer
     */
    public static final int getIno(byte[] buffer) {
        return convert32(buffer, 20);
    }

    /**
     * Read 8-byte integer from buffer.
     *
     * @param buffer
     * @param offset
     * @return the 8-byte entry as a long
     */
    public static final long convert64(byte[] buffer, int offset) {
        long i = 0;
        i += (((long) buffer[offset + 7]) << 56);
        i += (((long) buffer[offset + 6] << 48) & 0x00FF000000000000L);
        i += (((long) buffer[offset + 5] << 40) & 0x0000FF0000000000L);
        i += (((long) buffer[offset + 4] << 32) & 0x000000FF00000000L);
        i += (((long) buffer[offset + 3] << 24) & 0x00000000FF000000L);
        i += (((long) buffer[offset + 2] << 16) & 0x0000000000FF0000L);
        i += (((long) buffer[offset + 1] << 8) & 0x000000000000FF00L);
        i += (buffer[offset] & 0x00000000000000FFL);

        return i;
    }

    /**
     * Read 4-byte integer from buffer.
     *
     * @param buffer
     * @param offset
     * @return the 4-byte entry as an int
     */
    public static final int convert32(byte[] buffer, int offset) {
        int i = 0;
        i = buffer[offset + 3] << 24;
        i += (buffer[offset + 2] << 16) & 0x00FF0000;
        i += (buffer[offset + 1] << 8) & 0x0000FF00;
        i += buffer[offset] & 0x000000FF;

        return i;
    }

    /**
     * Read 2-byte integer from buffer.
     *
     * @param buffer
     * @param offset
     * @return the 2-byte entry as an int
     */
    public static final int convert16(byte[] buffer, int offset) {
        int i = 0;
        i += (buffer[offset + 1] << 8) & 0x0000FF00;
        i += buffer[offset] & 0x000000FF;

        return i;
    }
}
