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
package org.apache.commons.compress.compressors;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.compressors.pack200.Pack200CompressorInputStream;
import org.apache.commons.compress.compressors.pack200.Pack200CompressorOutputStream;
import org.apache.commons.compress.compressors.pack200.Pack200Strategy;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.Test;

public final class Pack200TestCase extends AbstractTestCase {

    @Test
    public void testJarUnarchiveAllInMemory() throws Exception {
        jarUnarchiveAll(false, Pack200Strategy.IN_MEMORY);
    }

    @Test
    public void testJarUnarchiveAllFileArgInMemory() throws Exception {
        jarUnarchiveAll(true, Pack200Strategy.IN_MEMORY);
    }

    @Test
    public void testJarUnarchiveAllTempFile() throws Exception {
        jarUnarchiveAll(false, Pack200Strategy.TEMP_FILE);
    }

    @Test
    public void testJarUnarchiveAllFileTempFile() throws Exception {
        jarUnarchiveAll(true, Pack200Strategy.TEMP_FILE);
    }

    private void jarUnarchiveAll(final boolean useFile, final Pack200Strategy mode)
        throws Exception {
        final File input = getFile("bla.pack");
        try (InputStream is = useFile
                ? new Pack200CompressorInputStream(input, mode)
                : new Pack200CompressorInputStream(new FileInputStream(input),
                mode)) {
            final ArchiveInputStream in = new ArchiveStreamFactory()
                    .createArchiveInputStream("jar", is);

            ArchiveEntry entry = in.getNextEntry();
            while (entry != null) {
                final File archiveEntry = new File(dir, entry.getName());
                archiveEntry.getParentFile().mkdirs();
                if (entry.isDirectory()) {
                    archiveEntry.mkdir();
                    entry = in.getNextEntry();
                    continue;
                }
                final OutputStream out = new FileOutputStream(archiveEntry);
                IOUtils.copy(in, out);
                out.close();
                entry = in.getNextEntry();
            }

            in.close();
        }
    }

    @Test
    public void testJarArchiveCreationInMemory() throws Exception {
        jarArchiveCreation(Pack200Strategy.IN_MEMORY);
    }

    @Test
    public void testJarArchiveCreationTempFile() throws Exception {
        jarArchiveCreation(Pack200Strategy.TEMP_FILE);
    }

    private void jarArchiveCreation(final Pack200Strategy mode) throws Exception {
        final File output = new File(dir, "bla.pack");

        final File file1 = getFile("test1.xml");
        final File file2 = getFile("test2.xml");

        try (OutputStream out = new Pack200CompressorOutputStream(new FileOutputStream(output),
                mode)) {
            final ArchiveOutputStream os = new ArchiveStreamFactory()
                    .createArchiveOutputStream("jar", out);

            os.putArchiveEntry(new ZipArchiveEntry("testdata/test1.xml"));
            IOUtils.copy(new FileInputStream(file1), os);
            os.closeArchiveEntry();

            os.putArchiveEntry(new ZipArchiveEntry("testdata/test2.xml"));
            IOUtils.copy(new FileInputStream(file2), os);
            os.closeArchiveEntry();

            os.close();
        }

        try (InputStream is = new Pack200CompressorInputStream(output)) {
            final ArchiveInputStream in = new ArchiveStreamFactory()
                    .createArchiveInputStream("jar", is);
            final List<String> files = new ArrayList<>();
            files.add("testdata/test1.xml");
            files.add("testdata/test2.xml");
            checkArchiveContent(in, files);
            in.close();
        }
    }

    @Test
    public void testGoodSignature() throws Exception {
        try (InputStream is = new FileInputStream(getFile("bla.pack"))) {
            final byte[] sig = new byte[4];
            is.read(sig);
            assertTrue(Pack200CompressorInputStream.matches(sig, 4));
        }
    }

    @Test
    public void testBadSignature() throws Exception {
        try (InputStream is = new FileInputStream(getFile("bla.jar"))) {
            final byte[] sig = new byte[4];
            is.read(sig);
            assertFalse(Pack200CompressorInputStream.matches(sig, 4));
        }
    }

    @Test
    public void testShortSignature() throws Exception {
        try (InputStream is = new FileInputStream(getFile("bla.pack"))) {
            final byte[] sig = new byte[2];
            is.read(sig);
            assertFalse(Pack200CompressorInputStream.matches(sig, 2));
        }
    }

    @Test
    public void testInputStreamMethods() throws Exception {
        final Map<String, String> m = new HashMap<>();
        m.put("foo", "bar");
        try (InputStream is = new Pack200CompressorInputStream(new FileInputStream(getFile("bla.jar")),
                m)) {
            // packed file is a jar, which is a zip so it starts with
            // a local file header
            assertTrue(is.markSupported());
            is.mark(5);
            assertEquals(0x50, is.read());
            final byte[] rest = new byte[3];
            assertEquals(3, is.read(rest));
            assertEquals(0x4b, rest[0]);
            assertEquals(3, rest[1]);
            assertEquals(4, rest[2]);
            assertEquals(1, is.skip(1));
            is.reset();
            assertEquals(0x50, is.read());
            assertTrue(is.available() > 0);
        }
    }

    @Test
    public void testOutputStreamMethods() throws Exception {
        final File output = new File(dir, "bla.pack");
        final Map<String, String> m = new HashMap<>();
        m.put("foo", "bar");
        try (OutputStream out = new FileOutputStream(output)) {
            final OutputStream os = new Pack200CompressorOutputStream(out, m);
            os.write(1);
            os.write(new byte[] { 2, 3 });
            os.close();
        }
    }

    @Test
    public void singleByteReadFromMemoryConsistentlyReturnsMinusOneAtEof() throws Exception {
        singleByteReadConsistentlyReturnsMinusOneAtEof(Pack200Strategy.IN_MEMORY);
    }

    @Test
    public void singleByteReadFromTempFileConsistentlyReturnsMinusOneAtEof() throws Exception {
        singleByteReadConsistentlyReturnsMinusOneAtEof(Pack200Strategy.TEMP_FILE);
    }

    private void singleByteReadConsistentlyReturnsMinusOneAtEof(Pack200Strategy s) throws Exception {
        final File input = getFile("bla.pack");
        try (final Pack200CompressorInputStream in = new Pack200CompressorInputStream(input, s)) {
            IOUtils.toByteArray(in);
            assertEquals(-1, in.read());
            assertEquals(-1, in.read());
            in.close();
        }
    }

    @Test
    public void multiByteReadFromMemoryConsistentlyReturnsMinusOneAtEof() throws Exception {
        multiByteReadConsistentlyReturnsMinusOneAtEof(Pack200Strategy.IN_MEMORY);
    }

    @Test
    public void multiByteReadFromTempFileConsistentlyReturnsMinusOneAtEof() throws Exception {
        multiByteReadConsistentlyReturnsMinusOneAtEof(Pack200Strategy.TEMP_FILE);
    }

    private void multiByteReadConsistentlyReturnsMinusOneAtEof(Pack200Strategy s) throws Exception {
        final File input = getFile("bla.pack");
        byte[] buf = new byte[2];
        try (final Pack200CompressorInputStream in = new Pack200CompressorInputStream(input, s)) {
            IOUtils.toByteArray(in);
            assertEquals(-1, in.read(buf));
            assertEquals(-1, in.read(buf));
            in.close();
        }
    }

}
