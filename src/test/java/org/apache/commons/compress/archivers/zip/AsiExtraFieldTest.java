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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

/**
 * JUnit testcases for org.apache.commons.compress.archivers.zip.AsiExtraField.
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
        byte[] data = {(byte)0xC6, 0x02, 0x78, (byte)0xB6, // CRC
                       0123, (byte)0x80,                   // mode
                       0, 0, 0, 0,                         // link length
                       5, 0, 6, 0};                        // uid, gid
        AsiExtraField a = new AsiExtraField();
        a.parseFromLocalFileData(data, 0, data.length);
        assertEquals(data.length, a.getLocalFileDataLength().getValue(), "length plain file");
        assertFalse(a.isLink(), "plain file, no link");
        assertFalse(a.isDirectory(), "plain file, no dir");
        assertEquals(FILE_FLAG | 0123, a.getMode(), "mode plain file");
        assertEquals(5, a.getUserId(), "uid plain file");
        assertEquals(6, a.getGroupId(), "gid plain file");

        data = new byte[] {0x75, (byte)0x8E, 0x41, (byte)0xFD, // CRC
                           0123, (byte)0xA0,                   // mode
                           4, 0, 0, 0,                         // link length
                           5, 0, 6, 0,                         // uid, gid
                           (byte)'t', (byte)'e', (byte)'s', (byte)'t'};
        a = new AsiExtraField();
        a.parseFromLocalFileData(data, 0, data.length);
        assertEquals(data.length, a.getLocalFileDataLength().getValue(), "length link");
        assertTrue(a.isLink(), "link, is link");
        assertFalse(a.isDirectory(), "link, no dir");
        assertEquals(LINK_FLAG | 0123, a.getMode(), "mode link");
        assertEquals(5, a.getUserId(), "uid link");
        assertEquals(6, a.getGroupId(), "gid link");
        assertEquals("test", a.getLinkedFile());

        data = new byte[] {(byte)0x8E, 0x01, (byte)0xBF, (byte)0x0E, // CRC
                           0123, (byte)0x40,                         // mode
                           0, 0, 0, 0,                               // link
                           5, 0, 6, 0};                          // uid, gid
        a = new AsiExtraField();
        a.parseFromLocalFileData(data, 0, data.length);
        assertEquals(data.length, a.getLocalFileDataLength().getValue(), "length dir");
        assertFalse(a.isLink(), "dir, no link");
        assertTrue(a.isDirectory(), "dir, is dir");
        assertEquals(DIR_FLAG | 0123, a.getMode(), "mode dir");
        assertEquals(5, a.getUserId(), "uid dir");
        assertEquals(6, a.getGroupId(), "gid dir");

        data = new byte[] {0, 0, 0, 0,                           // bad CRC
                           0123, (byte)0x40,                     // mode
                           0, 0, 0, 0,                           // link
                           5, 0, 6, 0};                          // uid, gid
        a = new AsiExtraField();
        try {
            a.parseFromLocalFileData(data, 0, data.length);
            fail("should raise bad CRC exception");
        } catch (final Exception e) {
            assertEquals("Bad CRC checksum, expected 0 instead of ebf018e",
                         e.getMessage());
        }
    }
}
