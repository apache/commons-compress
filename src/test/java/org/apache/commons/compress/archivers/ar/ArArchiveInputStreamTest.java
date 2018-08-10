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

package org.apache.commons.compress.archivers.ar;

import static org.junit.Assert.*;

import java.io.BufferedInputStream;
import java.io.FileInputStream;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.utils.ArchiveUtils;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.Test;

public class ArArchiveInputStreamTest extends AbstractTestCase {

    @Test
    public void testReadLongNamesGNU() throws Exception {
        checkLongNameEntry("longfile_gnu.ar");
    }

    @Test
    public void testReadLongNamesBSD() throws Exception {
        checkLongNameEntry("longfile_bsd.ar");
    }

    private void checkLongNameEntry(final String archive) throws Exception {
        try (final FileInputStream fis = new FileInputStream(getFile(archive));
                final ArArchiveInputStream s = new ArArchiveInputStream(new BufferedInputStream(fis))) {
            ArchiveEntry e = s.getNextEntry();
            assertEquals("this_is_a_long_file_name.txt", e.getName());
            assertEquals(14, e.getSize());
            final byte[] hello = new byte[14];
            s.read(hello);
            assertEquals("Hello, world!\n", ArchiveUtils.toAsciiString(hello));
            e = s.getNextEntry();
            assertEquals("this_is_a_long_file_name_as_well.txt", e.getName());
            assertEquals(4, e.getSize());
            final byte[] bye = new byte[4];
            s.read(bye);
            assertEquals("Bye\n", ArchiveUtils.toAsciiString(bye));
            assertNull(s.getNextEntry());
        }
    }

    @Test
    public void singleByteReadConsistentlyReturnsMinusOneAtEof() throws Exception {
        try (FileInputStream in = new FileInputStream(getFile("bla.ar"));
             ArArchiveInputStream archive = new ArArchiveInputStream(in)) {
            ArchiveEntry e = archive.getNextEntry();
            IOUtils.toByteArray(archive);
            assertEquals(-1, archive.read());
            assertEquals(-1, archive.read());
        }
    }

    @Test
    public void multiByteReadConsistentlyReturnsMinusOneAtEof() throws Exception {
        byte[] buf = new byte[2];
        try (FileInputStream in = new FileInputStream(getFile("bla.ar"));
             ArArchiveInputStream archive = new ArArchiveInputStream(in)) {
            ArchiveEntry e = archive.getNextEntry();
            IOUtils.toByteArray(archive);
            assertEquals(-1, archive.read(buf));
            assertEquals(-1, archive.read(buf));
        }
    }

    @Test(expected=IllegalStateException.class)
    public void cantReadWithoutOpeningAnEntry() throws Exception {
        try (FileInputStream in = new FileInputStream(getFile("bla.ar"));
             ArArchiveInputStream archive = new ArArchiveInputStream(in)) {
            archive.read();
        }
    }

    @Test(expected=IllegalStateException.class)
    public void cantReadAfterClose() throws Exception {
        try (FileInputStream in = new FileInputStream(getFile("bla.ar"));
             ArArchiveInputStream archive = new ArArchiveInputStream(in)) {
            archive.close();
            archive.read();
        }
    }
}
