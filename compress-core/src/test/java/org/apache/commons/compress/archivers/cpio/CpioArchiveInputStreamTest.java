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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.utils.CharsetNames;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

public class CpioArchiveInputStreamTest extends AbstractTest {

    private long consumeEntries(final CpioArchiveInputStream in) throws IOException {
        long count = 0;
        CpioArchiveEntry entry;
        while ((entry = in.getNextEntry()) != null) {
            count++;
            assertNotNull(entry);
        }
        return count;
    }

    @Test
    public void testCpioUnarchive() throws Exception {
        final StringBuilder expected = new StringBuilder();
        expected.append("./test1.xml<?xml version=\"1.0\"?>\n");
        expected.append("<empty/>./test2.xml<?xml version=\"1.0\"?>\n");
        expected.append("<empty/>\n");
        final StringBuilder result = new StringBuilder();
        try (CpioArchiveInputStream in = new CpioArchiveInputStream(newInputStream("bla.cpio"))) {
            CpioArchiveEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                result.append(entry.getName());
                int tmp;
                while ((tmp = in.read()) != -1) {
                    result.append((char) tmp);
                }
            }
        }
        assertEquals(result.toString(), expected.toString());
    }

    @Test
    public void testCpioUnarchiveCreatedByRedlineRpm() throws Exception {
        long count = 0;
        try (CpioArchiveInputStream in = new CpioArchiveInputStream(newInputStream("redline.cpio"))) {
            count = consumeEntries(in);
        }
        assertEquals(count, 1);
    }

    @Test
    public void testCpioUnarchiveMultibyteCharName() throws Exception {
        long count = 0;
        try (CpioArchiveInputStream in = new CpioArchiveInputStream(newInputStream("COMPRESS-459.cpio"), CharsetNames.UTF_8)) {
            count = consumeEntries(in);
        }
        assertEquals(2, count);
    }

    @Test
    public void testInvalidLongValueInMetadata() throws Exception {
        try (InputStream in = newInputStream("org/apache/commons/compress/cpio/bad_long_value.cpio");
             CpioArchiveInputStream archive = new CpioArchiveInputStream(in)) {
            assertThrows(IOException.class, archive::getNextEntry);
        }
    }

    @Test
    public void testMultiByteReadConsistentlyReturnsMinusOneAtEof() throws Exception {
        final byte[] buf = new byte[2];
        try (InputStream in = newInputStream("bla.cpio");
                CpioArchiveInputStream archive = new CpioArchiveInputStream(in)) {
            assertNotNull(archive.getNextEntry());
            IOUtils.toByteArray(archive);
            assertEquals(-1, archive.read(buf));
            assertEquals(-1, archive.read(buf));
        }
    }

    @Test
    public void testSingleByteReadConsistentlyReturnsMinusOneAtEof() throws Exception {
        try (InputStream in = newInputStream("bla.cpio");
                CpioArchiveInputStream archive = new CpioArchiveInputStream(in)) {
            assertNotNull(archive.getNextEntry());
            IOUtils.toByteArray(archive);
            assertEquals(-1, archive.read());
            assertEquals(-1, archive.read());
        }
    }

}
