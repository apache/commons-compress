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
package org.apache.commons.compress;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream;
import org.apache.commons.compress.archivers.arj.ArjArchiveInputStream;
import org.apache.commons.compress.archivers.cpio.CpioArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;

public final class DetectArchiverTestCase extends AbstractTestCase {
    public DetectArchiverTestCase(String name) {
        super(name);
    }

    final ClassLoader classLoader = getClass().getClassLoader();

    public void testDetectionNotArchive() throws IOException {
        try {
            getStreamFor("test.txt");
            fail("Expected ArchiveException");
        } catch (ArchiveException e) {
            // expected
        }
    }

    public void testCOMPRESS117() throws Exception {
        final ArchiveInputStream tar = getStreamFor("COMPRESS-117.tar");
        assertNotNull(tar);
        assertTrue(tar instanceof TarArchiveInputStream);
    }

    public void testDetection() throws Exception {

        final ArchiveInputStream ar = getStreamFor("bla.ar"); 
        assertNotNull(ar);
        assertTrue(ar instanceof ArArchiveInputStream);

        final ArchiveInputStream tar = getStreamFor("bla.tar");
        assertNotNull(tar);
        assertTrue(tar instanceof TarArchiveInputStream);

        final ArchiveInputStream zip = getStreamFor("bla.zip");
        assertNotNull(zip);
        assertTrue(zip instanceof ZipArchiveInputStream);

        final ArchiveInputStream jar = getStreamFor("bla.jar");
        assertNotNull(jar);
        assertTrue(jar instanceof ZipArchiveInputStream);

        final ArchiveInputStream cpio = getStreamFor("bla.cpio");
        assertNotNull(cpio);
        assertTrue(cpio instanceof CpioArchiveInputStream);
        
        final ArchiveInputStream arj = getStreamFor("bla.arj");
        assertNotNull(arj);
        assertTrue(arj instanceof ArjArchiveInputStream);

// Not yet implemented
//        final ArchiveInputStream tgz = getStreamFor("bla.tgz");
//        assertNotNull(tgz);
//        assertTrue(tgz instanceof TarArchiveInputStream);

    }

    private ArchiveInputStream getStreamFor(String resource)
            throws ArchiveException, IOException {
        return factory.createArchiveInputStream(
                   new BufferedInputStream(new FileInputStream(
                       getFile(resource))));
    }

    // Check that the empty archives created by the code are readable

    // Not possible to detect empty "ar" archive as it is completely empty
//    public void testEmptyArArchive() throws Exception {
//        emptyArchive("ar");
//    }

    public void testEmptyCpioArchive() throws Exception {
        checkEmptyArchive("cpio");
    }

    public void testEmptyJarArchive() throws Exception {
        checkEmptyArchive("jar");
    }

    // empty tar archives just have 512 null bytes
//    public void testEmptyTarArchive() throws Exception {
//        checkEmptyArchive("tar");
//    }
    public void testEmptyZipArchive() throws Exception {
        checkEmptyArchive("zip");
    }

    private void checkEmptyArchive(String type) throws Exception{
        File ar = createEmptyArchive(type); // will be deleted by tearDown()
        ar.deleteOnExit(); // Just in case file cannot be deleted
        ArchiveInputStream ais = null;
        BufferedInputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(ar));
            ais = factory.createArchiveInputStream(in);
        } catch (ArchiveException ae) {
            fail("Should have recognised empty archive for "+type);
        } finally {
            if (ais != null) {
                ais.close(); // will close input as well
            } else if (in != null){
                in.close();
            }
        }
    }
}
