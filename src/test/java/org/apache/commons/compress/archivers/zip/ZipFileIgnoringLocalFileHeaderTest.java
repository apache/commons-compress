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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Enumeration;

import org.apache.commons.compress.AbstractTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ZipFileIgnoringLocalFileHeaderTest {

    private static ZipFile openZipWithoutLFH(final String fileName) throws IOException {
        return new ZipFile(AbstractTestCase.getFile(fileName), ZipEncodingHelper.UTF8, true, true);
    }

    private File dir;

    @Test
    public void getEntryWorks() throws IOException {
        try (final ZipFile zf = openZipWithoutLFH("bla.zip")) {
            final ZipArchiveEntry ze = zf.getEntry("test1.xml");
            assertEquals(610, ze.getSize());
        }
    }

    @Test
    public void getRawInputStreamReturnsNotNull() throws IOException {
        try (final ZipFile zf = openZipWithoutLFH("bla.zip")) {
            final ZipArchiveEntry ze = zf.getEntry("test1.xml");
            assertNotNull(zf.getRawInputStream(ze));
        }
    }

    @BeforeEach
    public void setUp() throws Exception {
        dir = AbstractTestCase.mkdir("dir");
    }

    @AfterEach
    public void tearDown() {
        AbstractTestCase.rmdir(dir);
    }

    @Test
    public void testDuplicateEntry() throws Exception {
        try (final ZipFile zf = openZipWithoutLFH("COMPRESS-227.zip")) {
            int numberOfEntries = 0;
            for (final ZipArchiveEntry entry : zf.getEntries("test1.txt")) {
                numberOfEntries++;
                assertNotNull(zf.getInputStream(entry));
            }
            assertEquals(2, numberOfEntries);
        }
    }

    @Test
    public void testPhysicalOrder() throws IOException {
        try (final ZipFile zf = openZipWithoutLFH("ordertest.zip")) {
            final Enumeration<ZipArchiveEntry> e = zf.getEntriesInPhysicalOrder();
            ZipArchiveEntry ze = null;
            do {
                ze = e.nextElement();
            } while (e.hasMoreElements());
            assertEquals("src/main/java/org/apache/commons/compress/archivers/zip/ZipUtil.java", ze.getName());
        }
    }

    /**
     * Simple unarchive test. Asserts nothing.
     * @throws Exception
     */
    @Test
    public void testZipUnarchive() throws Exception {
        try (final ZipFile zf = openZipWithoutLFH("bla.zip")) {
            for (final Enumeration<ZipArchiveEntry> e = zf.getEntries(); e.hasMoreElements();) {
                final ZipArchiveEntry entry = e.nextElement();
                Files.copy(zf.getInputStream(entry), new File(dir, entry.getName()).toPath());
            }
        }
    }
}
