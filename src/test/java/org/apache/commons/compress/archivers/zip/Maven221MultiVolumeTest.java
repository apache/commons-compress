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

import static org.apache.commons.compress.AbstractTestCase.getFile;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.junit.Test;

/**
 * JUnit testcase for a multi-volume zip file.
 *
 * Some tools (like 7-zip) allow users to split a large archives into 'volumes'
 * with a given size to fit them into multiple cds, usb drives, or emails with
 * an attachment size limit. It's basically the same file split into chunks of
 * exactly 65536 bytes length. Concatenating volumes yields exactly the original
 * file. There is no mechanism in the ZIP algorithm to accommodate for this.
 * Before commons-compress used to enter an infinite loop on the last entry for
 * such a file. This test is intended to prove that this error doesn't occur
 * anymore. All entries but the last one are returned correctly, the last entry
 * yields an exception.
 *
 */
public class Maven221MultiVolumeTest {

    private static final String [] ENTRIES = new String [] {
        "apache-maven-2.2.1/",
        "apache-maven-2.2.1/LICENSE.txt",
        "apache-maven-2.2.1/NOTICE.txt",
        "apache-maven-2.2.1/README.txt",
        "apache-maven-2.2.1/bin/",
        "apache-maven-2.2.1/bin/m2.conf",
        "apache-maven-2.2.1/bin/mvn",
        "apache-maven-2.2.1/bin/mvn.bat",
        "apache-maven-2.2.1/bin/mvnDebug",
        "apache-maven-2.2.1/bin/mvnDebug.bat",
        "apache-maven-2.2.1/boot/",
        "apache-maven-2.2.1/boot/classworlds-1.1.jar",
        "apache-maven-2.2.1/conf/",
        "apache-maven-2.2.1/conf/settings.xml",
        "apache-maven-2.2.1/lib/"
    };

    private static final String LAST_ENTRY_NAME =
        "apache-maven-2.2.1/lib/maven-2.2.1-uber.jar";

    @Test
    public void testRead7ZipMultiVolumeArchiveForStream() throws IOException {

        final FileInputStream archive =
            new FileInputStream(getFile("apache-maven-2.2.1.zip.001"));
        ZipArchiveInputStream zi = null;
        try {
            zi = new ZipArchiveInputStream(archive,null,false);

            // these are the entries that are supposed to be processed
            // correctly without any problems
            for (final String element : ENTRIES) {
                assertEquals(element, zi.getNextEntry().getName());
            }

            // this is the last entry that is truncated
            final ArchiveEntry lastEntry = zi.getNextEntry();
            assertEquals(LAST_ENTRY_NAME, lastEntry.getName());
            final byte [] buffer = new byte [4096];

            // before the fix, we'd get 0 bytes on this read and all
            // subsequent reads thus a client application might enter
            // an infinite loop after the fix, we should get an
            // exception
            try {
                while (zi.read(buffer) > 0) { }
                fail("shouldn't be able to read from truncated entry");
            } catch (final IOException e) {
                assertEquals("Truncated ZIP file", e.getMessage());
            }

            try {
                zi.read(buffer);
                fail("shouldn't be able to read from truncated entry after exception");
            } catch (final IOException e) {
                assertEquals("Truncated ZIP file", e.getMessage());
            }

            // and now we get another entry, which should also yield
            // an exception
            try {
                zi.getNextEntry();
                fail("shouldn't be able to read another entry from truncated"
                     + " file");
            } catch (final IOException e) {
                // this is to be expected
            }
        } finally {
            if (zi != null) {
                zi.close();
            }
        }
    }

    @Test(expected=IOException.class)
    public void testRead7ZipMultiVolumeArchiveForFile() throws IOException {
        final File file = getFile("apache-maven-2.2.1.zip.001");
        ZipFile zf = new ZipFile(file);
        zf.close();
    }
}
