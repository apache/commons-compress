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
package org.apache.commons.compress.archivers.tar;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.archivers.ArchiveException;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class TarFileTest extends AbstractTestCase {

    @Test
    public void testDirectoryWithLongNameEndsWithSlash() throws IOException, ArchiveException {
        final String rootPath = dir.getAbsolutePath();
        final String dirDirectory = "COMPRESS-509";
        final int count = 100;
        File root = new File(rootPath + "/" + dirDirectory);
        root.mkdirs();
        for (int i = 1; i < count; i++) {
            // -----------------------
            // create empty dirs with incremental length
            // -----------------------
            StringBuilder subDirBuilder = new StringBuilder();
            for (int j = 0; j < i; j++) {
                subDirBuilder.append("a");
            }
            String subDir = subDirBuilder.toString();
            File dir = new File(rootPath + "/" + dirDirectory, "/" + subDir);
            dir.mkdir();

            // -----------------------
            // tar these dirs
            // -----------------------
            String fileName = "/" + dirDirectory + "/" + subDir;
            File tarF = new File(rootPath + "/tar" + i + ".tar");
            try (OutputStream dest = Files.newOutputStream(tarF.toPath());
                 TarArchiveOutputStream out = new TarArchiveOutputStream(new BufferedOutputStream(dest))) {
                out.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR);
                out.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

                File file = new File(rootPath, fileName);
                TarArchiveEntry entry = new TarArchiveEntry(file);
                entry.setName(fileName);
                out.putArchiveEntry(entry);
                out.closeArchiveEntry();
                out.flush();
            }
            // -----------------------
            // untar these tars
            // -----------------------
            try (TarFile tarFile = new TarFile(tarF)) {
                for (TarArchiveEntry entry : tarFile.getEntries()) {
                    assertTrue("Entry name: " + entry.getName(), entry.getName().endsWith("/"));
                }
            }
        }
    }

    @Test(expected = IOException.class)
    public void testParseTarWithNonNumberPaxHeaders() throws IOException {
        try (TarFile tarFile = new TarFile(getPath("COMPRESS-529.tar"))) {
        }
    }

    @Test(expected = IOException.class)
    public void testParseTarTruncatedInPadding() throws IOException {
        try (TarFile tarFile = new TarFile(getPath("COMPRESS-544_truncated_in_padding.tar"))) {
        }
    }

    @Test(expected = IOException.class)
    public void testParseTarTruncatedInContent() throws IOException {
        try (TarFile tarFile = new TarFile(getPath("COMPRESS-544_truncated_in_content.tar"))) {
        }
    }

}
