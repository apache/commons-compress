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
import java.util.Optional;

import org.junit.jupiter.api.Test;

class LhaArchiveEntryTest {
    @Test
    void testToStringMinimal() {
        final LhaArchiveEntry entry = new LhaArchiveEntry();
        entry.setName("test1.txt");
        entry.setDirectory(false);
        entry.setSize(57);
        entry.setLastModifiedDate(new Date(1754236942000L)); // 2025-08-03T16:02:22Z
        entry.setCompressedSize(52);
        entry.setCompressionMethod("-lh5-");
        entry.setCrcValue(0x6496);

        assertEquals("LhaArchiveEntry[name=test1.txt,directory=false,size=57,lastModifiedDate=2025-08-03T16:02:22Z,compressedSize=52," +
                "compressionMethod=-lh5-,crcValue=0x6496]", entry.toString());
    }

    @Test
    void testToStringAllFields() {
        final LhaArchiveEntry entry = new LhaArchiveEntry();
        entry.setName("dir1/test1.txt");
        entry.setDirectory(false);
        entry.setSize(57);
        entry.setLastModifiedDate(new Date(1754236942000L)); // 2025-08-03T16:02:22Z
        entry.setCompressedSize(52);
        entry.setCompressionMethod("-lh5-");
        entry.setCrcValue(0x6496);
        entry.setOsId(Optional.of(85));
        entry.setUnixPermissionMode(Optional.of(0100644));
        entry.setUnixGroupId(Optional.of(20));
        entry.setUnixUserId(Optional.of(501));
        entry.setMsdosFileAttributes(Optional.of(0x0010));
        entry.setHeaderCrc(Optional.of(0xb772));

        assertEquals(
                "LhaArchiveEntry[name=dir1/test1.txt,directory=false,size=57,lastModifiedDate=2025-08-03T16:02:22Z,compressedSize=52," +
                "compressionMethod=-lh5-,crcValue=0x6496,osId=85,unixPermissionMode=100644,msdosFileAttributes=0010,headerCrc=0xb772]",
                entry.toString());
    }
}
