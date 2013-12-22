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

package org.apache.commons.compress.archivers.zip;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;

import junit.framework.TestCase;
import org.apache.commons.compress.utils.BoundedInputStream;
import org.apache.commons.compress.utils.IOUtils;

public class ExplodeSupportTest extends TestCase {

    private void testArchiveWithImplodeCompression(String filename, String entryName) throws IOException {
        ZipFile zip = new ZipFile(new File(filename));
        ZipArchiveEntry entry = zip.getEntries().nextElement();
        assertEquals("entry name", entryName, entry.getName());
        assertTrue("entry can't be read", zip.canReadEntryData(entry));
        assertEquals("method", ZipMethod.IMPLODING.getCode(), entry.getMethod());

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        CheckedOutputStream out = new CheckedOutputStream(bout, new CRC32());
        IOUtils.copy(zip.getInputStream(entry), out);

        out.flush();

        assertEquals("CRC32", entry.getCrc(), out.getChecksum().getValue());
    }

    public void testArchiveWithImplodeCompression4K2Trees() throws IOException {
        testArchiveWithImplodeCompression("target/test-classes/archives/imploding-4Kdict-2trees.zip", "HEADER.TXT");
    }

    public void testArchiveWithImplodeCompression8K3Trees() throws IOException {
        testArchiveWithImplodeCompression("target/test-classes/archives/imploding-8Kdict-3trees.zip", "LICENSE.TXT");
    }

    public void testTikaTestArchive() throws IOException {
        testArchiveWithImplodeCompression("target/test-classes/moby-imploded.zip", "README");
    }

    private void testZipStreamWithImplodeCompression(String filename, String entryName) throws IOException {
        ZipArchiveInputStream zin = new ZipArchiveInputStream(new FileInputStream(new File(filename)));
        ZipArchiveEntry entry = zin.getNextZipEntry();
        assertEquals("entry name", entryName, entry.getName());
        assertTrue("entry can't be read", zin.canReadEntryData(entry));
        assertEquals("method", ZipMethod.IMPLODING.getCode(), entry.getMethod());

        InputStream bio = new BoundedInputStream(zin, entry.getSize());
        
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        CheckedOutputStream out = new CheckedOutputStream(bout, new CRC32());
        IOUtils.copy(bio, out);

        out.flush();

        assertEquals("CRC32", entry.getCrc(), out.getChecksum().getValue());
    }

    public void testZipStreamWithImplodeCompression4K2Trees() throws IOException {
        testZipStreamWithImplodeCompression("target/test-classes/archives/imploding-4Kdict-2trees.zip", "HEADER.TXT");
    }

    public void testZipStreamWithImplodeCompression8K3Trees() throws IOException {
        testZipStreamWithImplodeCompression("target/test-classes/archives/imploding-8Kdict-3trees.zip", "LICENSE.TXT");
    }

    public void testTikaTestStream() throws IOException {
        testZipStreamWithImplodeCompression("target/test-classes/moby-imploded.zip", "README");
    }

}
