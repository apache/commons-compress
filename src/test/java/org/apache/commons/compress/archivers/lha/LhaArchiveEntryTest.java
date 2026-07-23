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

package org.apache.commons.compress.archivers.lha;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;

import org.junit.jupiter.api.Test;

class LhaArchiveEntryTest {
    @Test
    void testToStringMinimal() {
        final LhaArchiveEntry entry = LhaArchiveEntry.builder()
            .setFilename("test1.txt")
            .setDirectory(false)
            .setSize(57)
            .setLastModifiedDate(new Date(1754236942000L)) // 2025-08-03T16:02:22Z
            .setCompressedSize(52)
            .setCompressionMethod("-lh5-")
            .setCrcValue(0x6496)
            .get();

        assertEquals("LhaArchiveEntry[name=test1.txt,directory=false,size=57,lastModifiedDate=2025-08-03T16:02:22Z,compressedSize=52," +
                "compressionMethod=-lh5-,crcValue=0x6496]", entry.toString());
    }

    @Test
    void testToStringAllFields() {
        final LhaArchiveEntry entry = LhaArchiveEntry.builder()
            .setFilename("test1.txt")
            .setDirectoryName("dir1/")
            .setDirectory(false)
            .setSize(57)
            .setLastModifiedDate(new Date(1754236942000L)) // 2025-08-03T16:02:22Z
            .setCompressedSize(52)
            .setCompressionMethod("-lh5-")
            .setCrcValue(0x6496)
            .setOsId(85)
            .setUnixPermissionMode(0100644)
            .setUnixGroupId(20)
            .setUnixUserId(501)
            .setMsdosFileAttributes(0x0010)
            .setHeaderCrc(0xb772)
            .get();

        assertEquals(
                "LhaArchiveEntry[name=dir1/test1.txt,directory=false,size=57,lastModifiedDate=2025-08-03T16:02:22Z,compressedSize=52," +
                "compressionMethod=-lh5-,crcValue=0x6496,osId=85,unixPermissionMode=100644,msdosFileAttributes=0010,headerCrc=0xb772]",
                entry.toString());
    }
}
