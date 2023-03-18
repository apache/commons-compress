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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.InputStream;
import java.util.Calendar;
import java.util.TimeZone;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.Test;

public class ArjArchiveInputStreamTest extends AbstractTestCase {

    @Test
    public void multiByteReadConsistentlyReturnsMinusOneAtEof() throws Exception {
        final byte[] buf = new byte[2];
        try (InputStream in = newInputStream("bla.arj");
             ArjArchiveInputStream archive = new ArjArchiveInputStream(in)) {
            final ArchiveEntry e = archive.getNextEntry();
            IOUtils.toByteArray(archive);
            assertEquals(-1, archive.read(buf));
            assertEquals(-1, archive.read(buf));
        }
    }

    @Test
    public void singleByteReadConsistentlyReturnsMinusOneAtEof() throws Exception {
        try (InputStream in = newInputStream("bla.arj");
             ArjArchiveInputStream archive = new ArjArchiveInputStream(in)) {
            final ArchiveEntry e = archive.getNextEntry();
            IOUtils.toByteArray(archive);
            assertEquals(-1, archive.read());
            assertEquals(-1, archive.read());
        }
    }

    @Test
    public void testArjUnarchive() throws Exception {
        final StringBuilder expected = new StringBuilder();
        expected.append("test1.xml<?xml version=\"1.0\"?>\n");
        expected.append("<empty/>test2.xml<?xml version=\"1.0\"?>\n");
        expected.append("<empty/>\n");

        final StringBuilder result = new StringBuilder();
        try (final ArjArchiveInputStream in = new ArjArchiveInputStream(newInputStream("bla.arj"))) {
            ArjArchiveEntry entry;

            while ((entry = in.getNextEntry()) != null) {
                result.append(entry.getName());
                int tmp;
                while ((tmp = in.read()) != -1) {
                    result.append((char) tmp);
                }
                assertFalse(entry.isDirectory());
            }
        }
        assertEquals(result.toString(), expected.toString());
    }

    @Test
    public void testReadingOfAttributesDosVersion() throws Exception {
        try (final ArjArchiveInputStream in = new ArjArchiveInputStream(newInputStream("bla.arj"))) {
            final ArjArchiveEntry entry = in.getNextEntry();
            assertEquals("test1.xml", entry.getName());
            assertEquals(30, entry.getSize());
            assertEquals(0, entry.getUnixMode());
            final Calendar cal = Calendar.getInstance();
            cal.set(2008, 9, 6, 23, 50, 52);
            cal.set(Calendar.MILLISECOND, 0);
            assertEquals(cal.getTime(), entry.getLastModifiedDate());
        }
    }

    @Test
    public void testReadingOfAttributesUnixVersion() throws Exception {
        try (final ArjArchiveInputStream in = new ArjArchiveInputStream(newInputStream("bla.unix.arj"))) {
            final ArjArchiveEntry entry = in.getNextEntry();
            assertEquals("test1.xml", entry.getName());
            assertEquals(30, entry.getSize());
            assertEquals(0664, entry.getUnixMode() & 07777 /* UnixStat.PERM_MASK */);
            final Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+0000"));
            cal.set(2008, 9, 6, 21, 50, 52);
            cal.set(Calendar.MILLISECOND, 0);
            assertEquals(cal.getTime(), entry.getLastModifiedDate());
        }
    }

}
