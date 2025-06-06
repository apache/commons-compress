/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
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

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.junit.jupiter.api.Test;

public class ArchiveOutputStreamTest<O extends ArchiveOutputStream<E>, E extends ArchiveEntry> extends AbstractTest {

    private O createArchiveWithDummyEntry(final String archiveType, final OutputStream out1, final File dummy) throws Exception {
        final O outputStream = factory.createArchiveOutputStream(archiveType, out1);
        outputStream.putArchiveEntry(outputStream.createArchiveEntry(dummy, "dummy"));
        outputStream.write(dummy);
        return outputStream;
    }

    private void doCallSequence(final String archiveType) throws Exception {
        final OutputStream out1 = new ByteArrayOutputStream();
        final File dummy = getFile("test1.xml"); // need a real file

        try (O outputStream = factory.createArchiveOutputStream(archiveType, out1)) {
            outputStream.putArchiveEntry(outputStream.createArchiveEntry(dummy, "dummy"));
            outputStream.write(dummy);
            outputStream.closeArchiveEntry();
            // omitted finish
        }

        // TODO - check if archives ensure that data has been written to the stream?

        final O aos2 = factory.createArchiveOutputStream(archiveType, out1);
        assertThrows(IOException.class, aos2::closeArchiveEntry, "Should have raised IOException - closeArchiveEntry() called before putArchiveEntry()");

        aos2.putArchiveEntry(aos2.createArchiveEntry(dummy, "dummy"));
        aos2.write(dummy);

        // TODO check if second putArchiveEntry() can follow without closeAE?

        assertThrows(IOException.class, aos2::finish, "Should have raised IOException - finish() called before closeArchiveEntry()");
        assertThrows(IOException.class, aos2::close, "Should have raised IOException - close() called before closeArchiveEntry()");

        final O aos3 = createArchiveWithDummyEntry(archiveType, out1, dummy);
        aos3.closeArchiveEntry();
        assertThrows(IOException.class, aos3::closeArchiveEntry, "Should have raised IOException - closeArchiveEntry() called with no open entry");

        final O aos4 = createArchiveWithDummyEntry(archiveType, out1, dummy);
        aos4.closeArchiveEntry();
        aos4.finish();
        aos4.close();
        assertThrows(IOException.class, aos4::finish, "Should have raised IOException - finish() called after close()");
    }

    @Test
    void testCallSequenceAr() throws Exception {
        doCallSequence("Ar");
    }

    @Test
    void testCallSequenceCpio() throws Exception {
        doCallSequence("Cpio");
    }

    @Test
    void testCallSequenceJar() throws Exception {
        doCallSequence("Jar");
    }

    @Test
    void testCallSequenceTar() throws Exception {
        doCallSequence("Tar");
    }

    @Test
    void testCallSequenceZip() throws Exception {
        doCallSequence("Zip");
    }

    @Test
    void testFinish() throws Exception {
        final OutputStream out1 = new ByteArrayOutputStream();

        try (ArchiveOutputStream<? super ArchiveEntry> aios = factory.createArchiveOutputStream("zip", out1)) {
            aios.putArchiveEntry(new ZipArchiveEntry("dummy"));
            assertThrows(IOException.class, () -> aios.finish(), "After putArchiveEntry() should follow closeArchiveEntry()");
            aios.closeArchiveEntry();
        }

        try (ArchiveOutputStream<JarArchiveEntry> aios = factory.createArchiveOutputStream("jar", out1)) {
            aios.putArchiveEntry(new JarArchiveEntry("dummy"));
            assertThrows(IOException.class, () -> aios.finish(), "After putArchiveEntry() should follow closeArchiveEntry()");
            aios.closeArchiveEntry();
        }

        try (ArchiveOutputStream<ArArchiveEntry> aios = factory.createArchiveOutputStream("ar", out1)) {
            aios.putArchiveEntry(new ArArchiveEntry("dummy", 100));
            assertThrows(IOException.class, () -> aios.finish(), "After putArchiveEntry() should follow closeArchiveEntry()");
            aios.closeArchiveEntry();
        }

        try (ArchiveOutputStream<CpioArchiveEntry> aios = factory.createArchiveOutputStream("cpio", out1)) {
            aios.putArchiveEntry(new CpioArchiveEntry("dummy"));
            assertThrows(IOException.class, () -> aios.finish(), "After putArchiveEntry() should follow closeArchiveEntry()");
            aios.closeArchiveEntry();
        }

        try (ArchiveOutputStream<TarArchiveEntry> aios = factory.createArchiveOutputStream("tar", out1)) {
            aios.putArchiveEntry(new TarArchiveEntry("dummy"));
            assertThrows(IOException.class, () -> aios.finish(), "After putArchiveEntry() should follow closeArchiveEntry()");
            aios.closeArchiveEntry();
        }
    }

    @Test
    void testOptionalFinish() throws Exception {
        final OutputStream out1 = new ByteArrayOutputStream();

        try (ArchiveOutputStream<ZipArchiveEntry> aos1 = factory.createArchiveOutputStream("zip", out1)) {
            aos1.putArchiveEntry(new ZipArchiveEntry("dummy"));
            aos1.closeArchiveEntry();
        }

        final ArchiveOutputStream<JarArchiveEntry> finishTest;
        try (ArchiveOutputStream<JarArchiveEntry> aos1 = factory.createArchiveOutputStream("jar", out1)) {
            finishTest = aos1;
            aos1.putArchiveEntry(new JarArchiveEntry("dummy"));
            aos1.closeArchiveEntry();
        }
        assertThrows(IOException.class, () -> finishTest.finish(), "finish() cannot follow close()");
        finishTest.close();
    }
}
