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
package org.apache.commons.compress.archivers.zip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.Test;

public class X000A_NTFSTest {

    @Test
    public void simpleRountrip() throws Exception {
        final X000A_NTFS xf = new X000A_NTFS();
        xf.setModifyJavaTime(new Date(0));
        // one second past midnight
        xf.setAccessJavaTime(new Date(-11644473601000L));
        xf.setCreateJavaTime(null);
        final byte[] b = xf.getLocalFileDataData();

        final X000A_NTFS xf2 = new X000A_NTFS();
        xf2.parseFromLocalFileData(b, 0, b.length);
        assertEquals(new Date(0), xf2.getModifyJavaTime());
        assertEquals(new Date(-11644473601000L), xf2.getAccessJavaTime());
        assertNull(xf2.getCreateJavaTime());
    }

    @Test
    public void simpleRountripWithHighPrecisionDatesWithBigValues() throws Exception {
        final X000A_NTFS xf = new X000A_NTFS();
        xf.setModifyFileTime(FileTime.from(Instant.ofEpochSecond(123456789101L, 123456700)));
        // one second past midnight
        xf.setAccessFileTime(FileTime.from(Instant.ofEpochSecond(-11644473601L)));
        // 765432100ns past midnight
        xf.setCreateFileTime(FileTime.from(Instant.ofEpochSecond(-11644473600L, 765432100)));
        final byte[] b = xf.getLocalFileDataData();

        final X000A_NTFS xf2 = new X000A_NTFS();
        xf2.parseFromLocalFileData(b, 0, b.length);
        assertEquals(FileTime.from(Instant.ofEpochSecond(123456789101L, 123456700)), xf2.getModifyFileTime());
        assertEquals(new Date(123456789101123L), xf2.getModifyJavaTime());
        assertEquals(FileTime.from(Instant.ofEpochSecond(-11644473601L)), xf2.getAccessFileTime());
        assertEquals(new Date(-11644473601000L), xf2.getAccessJavaTime());
        assertEquals(FileTime.from(Instant.ofEpochSecond(-11644473600L, 765432100)), xf2.getCreateFileTime());
        assertEquals(new Date(-11644473599235L).toInstant(), xf2.getCreateJavaTime().toInstant());
    }

    @Test
    public void simpleRountripWithHighPrecisionDatesWithSmallValues() throws Exception {
        final X000A_NTFS xf = new X000A_NTFS();
        // The last 2 digits should not be written due to the 100ns precision
        xf.setModifyFileTime(FileTime.from(Instant.ofEpochSecond(0, 1234)));
        // one second past midnight
        xf.setAccessFileTime(FileTime.from(Instant.ofEpochSecond(-11644473601L)));
        xf.setCreateFileTime(null);
        final byte[] b = xf.getLocalFileDataData();

        final X000A_NTFS xf2 = new X000A_NTFS();
        xf2.parseFromLocalFileData(b, 0, b.length);
        assertEquals(FileTime.from(Instant.ofEpochSecond(0, 1200)), xf2.getModifyFileTime());
        assertEquals(new Date(0), xf2.getModifyJavaTime());
        assertEquals(FileTime.from(Instant.ofEpochSecond(-11644473601L)), xf2.getAccessFileTime());
        assertEquals(new Date(-11644473601000L), xf2.getAccessJavaTime());
        assertNull(xf2.getCreateFileTime());
        assertNull(xf2.getCreateJavaTime());
    }
}
