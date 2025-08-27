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
package org.apache.commons.compress.archivers.dump;

import org.apache.commons.compress.utils.ByteUtils;

final class DumpArchiveTestFactory {
    private static final int MAGIC_OFFSET = 24;
    private static final int CHECKSUM_OFFSET = 28;
    private static final int NTREC_OFFSET = 896;
    private static final int SEGMENT_TYPE_OFFSET = 0;

    private static void convert32(final long value, final byte[] buffer, final int offset) {
        ByteUtils.toLittleEndian(buffer, value, offset, 4);
    }

    static byte[] createSegment(final DumpArchiveConstants.SEGMENT_TYPE segmentType) {
        final byte[] buffer = new byte[DumpArchiveConstants.TP_SIZE];
        // magic
        convert32(DumpArchiveConstants.NFS_MAGIC, buffer, MAGIC_OFFSET);
        // type
        ByteUtils.toLittleEndian(buffer, segmentType.code, SEGMENT_TYPE_OFFSET, 2);
        // checksum
        final int checksum = DumpArchiveUtil.calculateChecksum(buffer);
        convert32(checksum, buffer, CHECKSUM_OFFSET);
        return buffer;
    }

    static byte[] createSummary(final int ntrec) {
        final byte[] buffer = new byte[DumpArchiveConstants.TP_SIZE];
        // magic
        convert32(DumpArchiveConstants.NFS_MAGIC, buffer, MAGIC_OFFSET);
        // ntrec
        convert32(ntrec, buffer, NTREC_OFFSET);
        // checksum
        final int checksum = DumpArchiveUtil.calculateChecksum(buffer);
        convert32(checksum, buffer, CHECKSUM_OFFSET);
        return buffer;
    }

    private DumpArchiveTestFactory() {
        // prevent instantiation
    }
}
