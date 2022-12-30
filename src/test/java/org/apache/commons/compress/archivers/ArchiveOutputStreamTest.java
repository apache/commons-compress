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

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.junit.jupiter.api.Test;

public class ArchiveOutputStreamTest extends AbstractTestCase {

    private ArchiveOutputStream createArchiveWithDummyEntry(final String archiveType, final OutputStream out1, final File dummy)
        throws Exception {
        final ArchiveOutputStream aos1 = factory.createArchiveOutputStream(archiveType, out1);
        aos1.putArchiveEntry(aos1.createArchiveEntry(dummy, "dummy"));
        Files.copy(dummy.toPath(), aos1);
        return aos1;
    }

    private void doCallSequence(final String archiveType) throws Exception {
        final OutputStream out1 = new ByteArrayOutputStream();
        final File dummy = getFile("test1.xml"); // need a real file

        final ArchiveOutputStream aos1 = factory.createArchiveOutputStream(archiveType, out1);
        aos1.putArchiveEntry(aos1.createArchiveEntry(dummy, "dummy"));
        Files.copy(dummy.toPath(), aos1);
        aos1.closeArchiveEntry();
        aos1.close(); // omitted finish

        // TODO - check if archives ensure that data has been written to the stream?

        final ArchiveOutputStream aos2 = factory.createArchiveOutputStream(archiveType, out1);
        assertThrows(IOException.class, aos2::closeArchiveEntry,
                "Should have raised IOException - closeArchiveEntry() called before putArchiveEntry()");

        aos2.putArchiveEntry(aos2.createArchiveEntry(dummy, "dummy"));
        Files.copy(dummy.toPath(), aos2);

        // TODO check if second putArchiveEntry() can follow without closeAE?

        assertThrows(IOException.class, aos2::finish,
                "Should have raised IOException - finish() called before closeArchiveEntry()");
        assertThrows(IOException.class, aos2::close,
                "Should have raised IOException - close() called before closeArchiveEntry()");

        final ArchiveOutputStream aos3 = createArchiveWithDummyEntry(archiveType, out1, dummy);
        aos3.closeArchiveEntry();
        assertThrows(IOException.class, aos3::closeArchiveEntry,
                "Should have raised IOException - closeArchiveEntry() called with no open entry");

        final ArchiveOutputStream aos4 = createArchiveWithDummyEntry(archiveType, out1, dummy);
        aos4.closeArchiveEntry();
        aos4.finish();
        aos4.close();
        assertThrows(IOException.class, aos4::finish,
                "Should have raised IOException - finish() called after close()");
    }

    @Test
    public void testCallSequenceAr() throws Exception{
        doCallSequence("Ar");
    }

    @Test
    public void testCallSequenceCpio() throws Exception{
        doCallSequence("Cpio");
    }

    @Test
    public void testCallSequenceJar() throws Exception{
        doCallSequence("Jar");
    }

    @Test
    public void testCallSequenceTar() throws Exception{
        doCallSequence("Tar");
    }

    @Test
    public void testCallSequenceZip() throws Exception{
        doCallSequence("Zip");
    }

    @Test
    public void testFinish() throws Exception {
        final OutputStream out1 = new ByteArrayOutputStream();

        final ArchiveOutputStream aos1 = factory.createArchiveOutputStream("zip", out1);
        aos1.putArchiveEntry(new ZipArchiveEntry("dummy"));
        assertThrows(IOException.class, () -> aos1.finish(),
                "After putArchive should follow closeArchive");

        final ArchiveOutputStream aos2 = factory.createArchiveOutputStream("jar", out1);
        aos2.putArchiveEntry(new JarArchiveEntry("dummy"));
        assertThrows(IOException.class, () -> aos2.finish(),
                "After putArchive should follow closeArchive");

        final ArchiveOutputStream aos3 = factory.createArchiveOutputStream("ar", out1);
        aos3.putArchiveEntry(new ArArchiveEntry("dummy", 100));
        assertThrows(IOException.class, () -> aos3.finish(),
                "After putArchive should follow closeArchive");

        final ArchiveOutputStream aos4 = factory.createArchiveOutputStream("cpio", out1);
        aos4.putArchiveEntry(new CpioArchiveEntry("dummy"));
        assertThrows(IOException.class, () -> aos4.finish(),
                "After putArchive should follow closeArchive");

        final ArchiveOutputStream aos5 = factory.createArchiveOutputStream("tar", out1);
        aos5.putArchiveEntry(new TarArchiveEntry("dummy"));
        assertThrows(IOException.class, () -> aos5.finish(),
                "After putArchive should follow closeArchive");
    }

    @Test
    public void testOptionalFinish() throws Exception {
        final OutputStream out1 = new ByteArrayOutputStream();

        try (ArchiveOutputStream aos1 = factory.createArchiveOutputStream("zip", out1)) {
            aos1.putArchiveEntry(new ZipArchiveEntry("dummy"));
            aos1.closeArchiveEntry();
        }

        final ArchiveOutputStream finishTest;
        try (ArchiveOutputStream aos1 = factory.createArchiveOutputStream("jar", out1)) {
            finishTest = aos1;
            aos1.putArchiveEntry(new JarArchiveEntry("dummy"));
            aos1.closeArchiveEntry();
        }
        assertThrows(IOException.class, () -> finishTest.finish(),
                "finish() cannot follow close()");
        finishTest.close();
    }
}
