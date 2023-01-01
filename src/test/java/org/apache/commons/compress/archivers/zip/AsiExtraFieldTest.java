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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * JUnit tests for org.apache.commons.compress.archivers.zip.AsiExtraField.
 *
 */
public class AsiExtraFieldTest implements UnixStat {

    @Test
    public void testClone() {
        final AsiExtraField s1 = new AsiExtraField();
        s1.setUserId(42);
        s1.setGroupId(12);
        s1.setLinkedFile("foo");
        s1.setMode(0644);
        s1.setDirectory(true);
        final AsiExtraField s2 = (AsiExtraField) s1.clone();
        assertNotSame(s1, s2);
        assertEquals(s1.getUserId(), s2.getUserId());
        assertEquals(s1.getGroupId(), s2.getGroupId());
        assertEquals(s1.getLinkedFile(), s2.getLinkedFile());
        assertEquals(s1.getMode(), s2.getMode());
        assertEquals(s1.isDirectory(), s2.isDirectory());
    }

    /**
     * Test content.
     */
    @Test
    public void testContent() {
        final AsiExtraField a = new AsiExtraField();
        a.setMode(0123);
        a.setUserId(5);
        a.setGroupId(6);
        byte[] b = a.getLocalFileDataData();

        // CRC manually calculated, sorry
        byte[] expect = {(byte)0xC6, 0x02, 0x78, (byte)0xB6, // CRC
                         0123, (byte)0x80,                   // mode
                         0, 0, 0, 0,                         // link length
                         5, 0, 6, 0};                        // uid, gid
        assertEquals(expect.length, b.length, "no link");
        for (int i=0; i<expect.length; i++) {
            assertEquals(expect[i], b[i], "no link, byte " + i);
        }

        a.setLinkedFile("test");
        expect = new byte[] {0x75, (byte)0x8E, 0x41, (byte)0xFD, // CRC
                             0123, (byte)0xA0,                   // mode
                             4, 0, 0, 0,                         // link length
                             5, 0, 6, 0,                         // uid, gid
                             (byte)'t', (byte)'e', (byte)'s', (byte)'t'};
        b = a.getLocalFileDataData();
        assertEquals(expect.length, b.length, "no link");
        for (int i=0; i<expect.length; i++) {
            assertEquals(expect[i], b[i], "no link, byte "+i);
        }

    }

    /**
     * Test file mode magic.
     */
    @Test
    public void testModes() {
        final AsiExtraField a = new AsiExtraField();
        a.setMode(0123);
        assertEquals(0100123, a.getMode(), "plain file");
        a.setDirectory(true);
        assertEquals(040123, a.getMode(), "directory");
        a.setLinkedFile("test");
        assertEquals(0120123, a.getMode(), "symbolic link");
    }

    /**
     * Test reparse
     */
    @Test
    public void testReparse() throws Exception {
        // CRC manually calculated, sorry
        final byte[] data1 = {(byte)0xC6, 0x02, 0x78, (byte)0xB6, // CRC
                              0123, (byte)0x80,                   // mode
                              0, 0, 0, 0,                         // link length
                              5, 0, 6, 0};                        // uid, gid
        final AsiExtraField a1 = new AsiExtraField();
        a1.parseFromLocalFileData(data1, 0, data1.length);
        assertEquals(data1.length, a1.getLocalFileDataLength().getValue(), "length plain file");
        assertFalse(a1.isLink(), "plain file, no link");
        assertFalse(a1.isDirectory(), "plain file, no dir");
        assertEquals(FILE_FLAG | 0123, a1.getMode(), "mode plain file");
        assertEquals(5, a1.getUserId(), "uid plain file");
        assertEquals(6, a1.getGroupId(), "gid plain file");

        final byte[] data2 = {0x75, (byte)0x8E, 0x41, (byte)0xFD,            // CRC
                                         0123, (byte)0xA0,                   // mode
                                         4, 0, 0, 0,                         // link length
                                         5, 0, 6, 0,                         // uid, gid
                                         (byte)'t', (byte)'e', (byte)'s', (byte)'t'};
        final AsiExtraField a2 = new AsiExtraField();
        a2.parseFromLocalFileData(data2, 0, data2.length);
        assertEquals(data2.length, a2.getLocalFileDataLength().getValue(), "length link");
        assertTrue(a2.isLink(), "link, is link");
        assertFalse(a2.isDirectory(), "link, no dir");
        assertEquals(LINK_FLAG | 0123, a2.getMode(), "mode link");
        assertEquals(5, a2.getUserId(), "uid link");
        assertEquals(6, a2.getGroupId(), "gid link");
        assertEquals("test", a2.getLinkedFile());

        final byte[] data3 = {(byte)0x8E, 0x01, (byte)0xBF, (byte)0x0E,            // CRC
                                         0123, (byte)0x40,                         // mode
                                         0, 0, 0, 0,                               // link
                                         5, 0, 6, 0};                              // uid, gid
        final AsiExtraField a3 = new AsiExtraField();
        a3.parseFromLocalFileData(data3, 0, data3.length);
        assertEquals(data3.length, a3.getLocalFileDataLength().getValue(), "length dir");
        assertFalse(a3.isLink(), "dir, no link");
        assertTrue(a3.isDirectory(), "dir, is dir");
        assertEquals(DIR_FLAG | 0123, a3.getMode(), "mode dir");
        assertEquals(5, a3.getUserId(), "uid dir");
        assertEquals(6, a3.getGroupId(), "gid dir");

        final byte[] data4 = {0, 0, 0, 0,                                      // bad CRC
                                         0123, (byte)0x40,                     // mode
                                         0, 0, 0, 0,                           // link
                                         5, 0, 6, 0};                          // uid, gid
        final AsiExtraField a4 = new AsiExtraField();
        final Exception e = assertThrows(Exception.class, () -> a4.parseFromLocalFileData(data4, 0, data4.length),
                "should raise bad CRC exception");
        assertEquals("Bad CRC checksum, expected 0 instead of ebf018e", e.getMessage());
    }
}
