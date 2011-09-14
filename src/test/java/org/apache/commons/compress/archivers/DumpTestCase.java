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
package org.apache.commons.compress.archivers;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.archivers.dump.DumpArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;

public final class DumpTestCase extends AbstractTestCase {

    public void testDumpUnarchiveAll() throws Exception {
        unarchiveAll(getFile("bla.dump"));
    }

    public void testCompressedDumpUnarchiveAll() throws Exception {
        unarchiveAll(getFile("bla.z.dump"));
    }

    private void unarchiveAll(final File input) throws Exception {
        final InputStream is = new FileInputStream(input);
        ArchiveInputStream in = null;
        OutputStream out = null;
        try {
            in = new ArchiveStreamFactory()
                .createArchiveInputStream("dump", is);

            ArchiveEntry entry = in.getNextEntry();
            while (entry != null) {
                File archiveEntry = new File(dir, entry.getName());
                archiveEntry.getParentFile().mkdirs();
                if (entry.isDirectory()) {
                    archiveEntry.mkdir();
                    entry = in.getNextEntry();
                    continue;
                }
                out = new FileOutputStream(archiveEntry);
                IOUtils.copy(in, out);
                out.close();
                out = null;
                entry = in.getNextEntry();
            }
        } finally {
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
            is.close();
        }
    }

    public void testArchiveDetection() throws Exception {
        archiveDetection(getFile("bla.dump"));
    }

    public void testCompressedArchiveDetection() throws Exception {
        archiveDetection(getFile("bla.z.dump"));
    }

    private void archiveDetection(final File f) throws Exception {
        final InputStream is = new FileInputStream(f);
        try {
            assertEquals(DumpArchiveInputStream.class,
                         new ArchiveStreamFactory()
                         .createArchiveInputStream(new BufferedInputStream(is))
                         .getClass());
        } finally {
            is.close();
        }
    }

    public void testCheckArchive() throws Exception {
        checkDumpArchive(getFile("bla.dump"));
    }

    public void testCheckCompressedArchive() throws Exception {
        checkDumpArchive(getFile("bla.z.dump"));
    }

    private void checkDumpArchive(final File f) throws Exception {
        ArrayList<String> expected = new ArrayList<String>();
        expected.add("");
        expected.add("lost+found/");
        expected.add("test1.xml");
        expected.add("test2.xml");
        final InputStream is = new FileInputStream(f);
        try {
            checkArchiveContent(new DumpArchiveInputStream(is),
                                expected);
        } finally {
            is.close();
        }
    }
}
