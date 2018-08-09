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
package org.apache.commons.compress.archivers.cpio;

import static org.junit.Assert.*;

import java.io.FileInputStream;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.Test;

public class CpioArchiveInputStreamTest extends AbstractTestCase {

    @Test
    public void testCpioUnarchive() throws Exception {
        final StringBuilder expected = new StringBuilder();
        expected.append("./test1.xml<?xml version=\"1.0\"?>\n");
        expected.append("<empty/>./test2.xml<?xml version=\"1.0\"?>\n");
        expected.append("<empty/>\n");


        final CpioArchiveInputStream in = new CpioArchiveInputStream(new FileInputStream(getFile("bla.cpio")));
        CpioArchiveEntry entry;

        final StringBuilder result = new StringBuilder();
        while ((entry = (CpioArchiveEntry) in.getNextEntry()) != null) {
            result.append(entry.getName());
            int tmp;
            while ((tmp = in.read()) != -1) {
                result.append((char) tmp);
            }
        }
        in.close();
        assertEquals(result.toString(), expected.toString());
    }

    @Test
    public void testCpioUnarchiveCreatedByRedlineRpm() throws Exception {
        final CpioArchiveInputStream in =
            new CpioArchiveInputStream(new FileInputStream(getFile("redline.cpio")));
        CpioArchiveEntry entry= null;

        int count = 0;
        while ((entry = (CpioArchiveEntry) in.getNextEntry()) != null) {
            count++;
            assertNotNull(entry);
        }
        in.close();

        assertEquals(count, 1);
    }

    @Test
    public void testCpioUnarchiveMultibyteCharName() throws Exception {
        final CpioArchiveInputStream in =
            new CpioArchiveInputStream(new FileInputStream(getFile("COMPRESS-459.cpio")), "UTF-8");
        CpioArchiveEntry entry= null;

        int count = 0;
        while ((entry = (CpioArchiveEntry) in.getNextEntry()) != null) {
            count++;
            assertNotNull(entry);
        }
        in.close();

        assertEquals(2, count);
    }

    @Test
    public void singleByteReadConsistentlyReturnsMinusOneAtEof() throws Exception {
        try (FileInputStream in = new FileInputStream(getFile("bla.cpio"));
             CpioArchiveInputStream archive = new CpioArchiveInputStream(in)) {
            ArchiveEntry e = archive.getNextEntry();
            IOUtils.toByteArray(archive);
            assertEquals(-1, archive.read());
            assertEquals(-1, archive.read());
        }
    }

    @Test
    public void multiByteReadConsistentlyReturnsMinusOneAtEof() throws Exception {
        byte[] buf = new byte[2];
        try (FileInputStream in = new FileInputStream(getFile("bla.cpio"));
             CpioArchiveInputStream archive = new CpioArchiveInputStream(in)) {
            ArchiveEntry e = archive.getNextEntry();
            IOUtils.toByteArray(archive);
            assertEquals(-1, archive.read(buf));
            assertEquals(-1, archive.read(buf));
        }
    }

}
