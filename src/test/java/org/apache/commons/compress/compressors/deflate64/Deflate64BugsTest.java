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
 */
package org.apache.commons.compress.compressors.deflate64;

import static org.apache.commons.compress.AbstractTest.getFile;

import java.io.InputStream;
import java.util.Enumeration;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.junit.jupiter.api.Test;

public class Deflate64BugsTest {

    @Test
    public void testReadBeyondMemoryException() throws Exception {
        try (ZipFile zfile = new ZipFile(getFile("COMPRESS-380/COMPRESS-380-readbeyondmemory.zip"))) {
            final Enumeration<ZipArchiveEntry> entries = zfile.getEntries();
            while (entries.hasMoreElements()) {
                final ZipArchiveEntry e = entries.nextElement();
                final byte[] buf = new byte[1024 * 8];
                try (InputStream is = zfile.getInputStream(e)) {
                    while (true) {
                        final int read = is.read(buf);
                        if (read == -1) {
                            break;
                        }
                    }
                }
            }
        }
    }
}
