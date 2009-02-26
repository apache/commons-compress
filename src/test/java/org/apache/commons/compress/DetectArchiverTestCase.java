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

import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream;
import org.apache.commons.compress.archivers.cpio.CpioArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;

public final class DetectArchiverTestCase extends AbstractTestCase {
    public void testDetection() throws Exception {
        final ArchiveStreamFactory factory = new ArchiveStreamFactory();

        final ArchiveInputStream ar = factory.createArchiveInputStream(
                                                                       new BufferedInputStream(new FileInputStream(
                                                                                                                   new File(getClass().getClassLoader().getResource("bla.ar").getFile())))); 
        assertNotNull(ar);
        assertTrue(ar instanceof ArArchiveInputStream);

        final ArchiveInputStream tar = factory.createArchiveInputStream(
                                                                        new BufferedInputStream(new FileInputStream(
                                                                                                                    new File(getClass().getClassLoader().getResource("bla.tar").getFile()))));
        assertNotNull(tar);
        assertTrue(tar instanceof TarArchiveInputStream);

        final ArchiveInputStream zip = factory.createArchiveInputStream(
                                                                        new BufferedInputStream(new FileInputStream(
                                                                                                                    new File(getClass().getClassLoader().getResource("bla.zip").getFile()))));
        assertNotNull(zip);
        assertTrue(zip instanceof ZipArchiveInputStream);

        final ArchiveInputStream jar = factory.createArchiveInputStream(
                                                                        new BufferedInputStream(new FileInputStream(
                                                                                                                    new File(getClass().getClassLoader().getResource("bla.jar").getFile()))));
        assertNotNull(jar);
        assertTrue(jar instanceof ZipArchiveInputStream);

        final ArchiveInputStream cpio = factory.createArchiveInputStream(
                                                                         new BufferedInputStream(new FileInputStream(
                                                                                                                     new File(getClass().getClassLoader().getResource("bla.cpio").getFile()))));
        assertNotNull(cpio);
        assertTrue(cpio instanceof CpioArchiveInputStream);

        //              final ArchiveInputStream tgz = factory.createArchiveInputStream(
        //                              new BufferedInputStream(new FileInputStream(
        //                                              new File(getClass().getClassLoader().getResource("bla.tgz").getFile()))));
        //              assertTrue(tgz instanceof TarArchiveInputStream);

    }

}
