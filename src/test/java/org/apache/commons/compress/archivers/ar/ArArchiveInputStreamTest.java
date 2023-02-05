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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.utils.ArchiveUtils;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.Test;

public class ArArchiveInputStreamTest extends AbstractTestCase {

    @Test
    public void cantReadAfterClose() throws Exception {
        try (InputStream in = newInputStream("bla.ar");
             ArArchiveInputStream archive = new ArArchiveInputStream(in)) {
            archive.close();
            assertThrows(IllegalStateException.class, () -> archive.read());
        }
    }

    @Test
    public void cantReadWithoutOpeningAnEntry() throws Exception {
        try (InputStream in = newInputStream("bla.ar");
             ArArchiveInputStream archive = new ArArchiveInputStream(in)) {
            assertThrows(IllegalStateException.class, () -> archive.read());
        }
    }

    private void checkLongNameEntry(final String archive) throws Exception {
        try (final InputStream fis = newInputStream(archive);
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
    public void multiByteReadConsistentlyReturnsMinusOneAtEof() throws Exception {
        final byte[] buf = new byte[2];
        try (InputStream in = newInputStream("bla.ar");
             ArArchiveInputStream archive = new ArArchiveInputStream(in)) {
            final ArchiveEntry e = archive.getNextEntry();
            IOUtils.toByteArray(archive);
            assertEquals(-1, archive.read(buf));
            assertEquals(-1, archive.read(buf));
        }
    }

    @Test
    public void simpleInputStream() throws IOException {
        try (final InputStream fileInputStream = newInputStream("bla.ar")) {

            // This default implementation of InputStream.available() always returns zero,
            // and there are many streams in practice where the total length of the stream is not known.

            final InputStream simpleInputStream = new InputStream() {
                @Override
                public int read() throws IOException {
                    return fileInputStream.read();
                }
            };

            try (ArArchiveInputStream archiveInputStream = new ArArchiveInputStream(simpleInputStream)) {
                final ArArchiveEntry entry1 = archiveInputStream.getNextArEntry();
                assertThat(entry1, not(nullValue()));
                assertThat(entry1.getName(), equalTo("test1.xml"));
                assertThat(entry1.getLength(), equalTo(610L));

                final ArArchiveEntry entry2 = archiveInputStream.getNextArEntry();
                assertThat(entry2.getName(), equalTo("test2.xml"));
                assertThat(entry2.getLength(), equalTo(82L));

                assertThat(archiveInputStream.getNextArEntry(), nullValue());
            }
        }
    }

    @Test
    public void singleByteReadConsistentlyReturnsMinusOneAtEof() throws Exception {
        try (InputStream in = newInputStream("bla.ar");
             ArArchiveInputStream archive = new ArArchiveInputStream(in)) {
            final ArchiveEntry e = archive.getNextEntry();
            IOUtils.toByteArray(archive);
            assertEquals(-1, archive.read());
            assertEquals(-1, archive.read());
        }
    }

    @Test
    public void testReadLongNamesBSD() throws Exception {
        checkLongNameEntry("longfile_bsd.ar");
    }

    @Test
    public void testReadLongNamesGNU() throws Exception {
        checkLongNameEntry("longfile_gnu.ar");
    }
}
