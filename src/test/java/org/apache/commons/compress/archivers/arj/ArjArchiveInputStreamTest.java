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

package org.apache.commons.compress.archivers.arj;

import java.io.FileInputStream;
import java.util.Calendar;
import java.util.TimeZone;

import org.apache.commons.compress.AbstractTestCase;

public class ArjArchiveInputStreamTest extends AbstractTestCase {

    public void testArjUnarchive() throws Exception {
        StringBuilder expected = new StringBuilder();
        expected.append("test1.xml<?xml version=\"1.0\"?>\n");
        expected.append("<empty/>test2.xml<?xml version=\"1.0\"?>\n");
        expected.append("<empty/>\n");


        ArjArchiveInputStream in = new ArjArchiveInputStream(new FileInputStream(getFile("bla.arj")));
        ArjArchiveEntry entry;

        StringBuilder result = new StringBuilder();
        while ((entry = in.getNextEntry()) != null) {
            result.append(entry.getName());
            int tmp;
            while ((tmp = in.read()) != -1) {
                result.append((char) tmp);
            }
            assertFalse(entry.isDirectory());
        }
        in.close();
        assertEquals(result.toString(), expected.toString());
    }

    public void testReadingOfAttributesDosVersion() throws Exception {
        ArjArchiveInputStream in = new ArjArchiveInputStream(new FileInputStream(getFile("bla.arj")));
        ArjArchiveEntry entry = in.getNextEntry();
        assertEquals("test1.xml", entry.getName());
        assertEquals(30, entry.getSize());
        assertEquals(0, entry.getUnixMode());
        Calendar cal = Calendar.getInstance();
        cal.set(2008, 9, 6, 23, 50, 52);
        cal.set(Calendar.MILLISECOND, 0);
        assertEquals(cal.getTime(), entry.getLastModifiedDate());
    }

    public void testReadingOfAttributesUnixVersion() throws Exception {
        ArjArchiveInputStream in = new ArjArchiveInputStream(new FileInputStream(getFile("bla.unix.arj")));
        ArjArchiveEntry entry = in.getNextEntry();
        assertEquals("test1.xml", entry.getName());
        assertEquals(30, entry.getSize());
        assertEquals(0664, entry.getUnixMode() & 07777 /* UnixStat.PERM_MASK */);
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+0000"));
        cal.set(2008, 9, 6, 21, 50, 52);
        cal.set(Calendar.MILLISECOND, 0);
        assertEquals(cal.getTime(), entry.getLastModifiedDate());
    }
}
