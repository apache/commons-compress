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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.utils.IOUtils;

public class ArchiveOutputStreamTest extends AbstractTestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testFinish() throws Exception {
        OutputStream out1 = new ByteArrayOutputStream();
        
        ArchiveOutputStream aos1 = factory.createArchiveOutputStream("zip", out1);
        aos1.putArchiveEntry(new ZipArchiveEntry("dummy"));
        try {
            aos1.finish();
            fail("After putArchive should follow closeArchive");
        } catch (IOException io) {
            // Exception expected
        }
        
        aos1 = factory.createArchiveOutputStream("jar", out1);
        aos1.putArchiveEntry(new JarArchiveEntry("dummy"));
        try {
            aos1.finish();
            fail("After putArchive should follow closeArchive");
        } catch (IOException io) {
            // Exception expected
        }
        
        aos1 = factory.createArchiveOutputStream("ar", out1);
        aos1.putArchiveEntry(new ArArchiveEntry("dummy", 100));
        try {
            aos1.finish();
            fail("After putArchive should follow closeArchive");
        } catch (IOException io) {
            // Exception expected
        }
        
        aos1 = factory.createArchiveOutputStream("cpio", out1);
        aos1.putArchiveEntry(new CpioArchiveEntry("dummy"));
        try {
            aos1.finish();
            fail("After putArchive should follow closeArchive");
        } catch (IOException io) {
            // Exception expected
        }
        
        aos1 = factory.createArchiveOutputStream("tar", out1);
        aos1.putArchiveEntry(new TarArchiveEntry("dummy"));
        try {
            aos1.finish();
            fail("After putArchive should follow closeArchive");
        } catch (IOException io) {
            // Exception expected
        }
    }

    public void testOptionalFinish() throws Exception {
        OutputStream out1 = new ByteArrayOutputStream();
        
        ArchiveOutputStream aos1 = factory.createArchiveOutputStream("zip", out1);
        aos1.putArchiveEntry(new ZipArchiveEntry("dummy"));
        aos1.closeArchiveEntry();
        aos1.close();
        
        aos1 = factory.createArchiveOutputStream("jar", out1);
        aos1.putArchiveEntry(new JarArchiveEntry("dummy"));
        aos1.closeArchiveEntry();
        aos1.close();
        try {
            aos1.finish();
            fail("finish() cannot follow close()");
        } catch (IOException io) {
            // Exception expected
        }
    }
    
    public void testCallSequenceAr() throws Exception{
        doCallSequence("Ar");
    }

    public void testCallSequenceCpio() throws Exception{
        doCallSequence("Cpio");
    }

    public void testCallSequenceJar() throws Exception{
        doCallSequence("Jar");
    }

    public void testCallSequenceTar() throws Exception{
        doCallSequence("Tar");
    }

    public void testCallSequenceZip() throws Exception{
        doCallSequence("Zip");
    }

    private void doCallSequence(String archiveType) throws Exception {
        OutputStream out1 = new ByteArrayOutputStream();
        File dummy = getFile("test1.xml"); // need a real file
        
        ArchiveOutputStream aos1;
        aos1 = factory.createArchiveOutputStream(archiveType, out1);
        aos1.putArchiveEntry(aos1.createArchiveEntry(dummy, "dummy"));
        InputStream is = new FileInputStream(dummy);
        IOUtils.copy(is, aos1);
        is.close();
        aos1.closeArchiveEntry();
        aos1.close(); // omitted finish

        // TODO - check if archives ensure that data has been written to the stream?
        
        aos1 = factory.createArchiveOutputStream(archiveType, out1);
        try {
            aos1.closeArchiveEntry();
            fail("Should have raised IOException - closeArchiveEntry() called before putArchiveEntry()");
        } catch (IOException expected) {
        }

        aos1.putArchiveEntry(aos1.createArchiveEntry(dummy, "dummy"));
        is = new FileInputStream(dummy);
        IOUtils.copy(is, aos1);
        is.close();

        // TODO check if second putArchiveEntry() can follow without closeAE?
        
        try {
            aos1.finish();
            fail("Should have raised IOException - finish() called before closeArchiveEntry()");
        } catch (IOException expected) {
        }
        try {
            aos1.close();
            fail("Should have raised IOException - close() called before closeArchiveEntry()");
        } catch (IOException expected) {
        }
        aos1.closeArchiveEntry();
        try {
            aos1.closeArchiveEntry();
            fail("Should have raised IOException - closeArchiveEntry() called with no open entry");
        } catch (IOException expected) {
        }
        aos1.finish();
        aos1.close();
        try {
            aos1.finish();
            fail("Should have raised IOException - finish() called after close()");
        } catch (IOException expected) {
        }
    }
}
