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
package org.apache.commons.compress.archivers.sevenz;

import org.apache.commons.compress.AbstractTestCase;

public class SevenZFileTest extends AbstractTestCase {
    public void testAllEmptyFilesArchive() throws Exception {
        SevenZFile archive = new SevenZFile(getFile("7z-empty-mhc-off.7z"));
        try {
            assertNotNull(archive.getNextEntry());
        } finally {
            archive.close();
        }
    }
    
    public void testHelloWorldHeaderCompressionOffCopy() throws Exception {
        checkHelloWorld("7z-hello-mhc-off-copy.7z");
    }

    public void testHelloWorldHeaderCompressionOffLZMA2() throws Exception {
        checkHelloWorld("7z-hello-mhc-off-lzma2.7z");
    }

    private void checkHelloWorld(final String filename) throws Exception {
        SevenZFile sevenZFile = new SevenZFile(getFile(filename));
        try {
            SevenZArchiveEntry entry = sevenZFile.getNextEntry();
            assertEquals("Hello world.txt", entry.getName());
            byte[] contents = new byte[(int)entry.getSize()];
            int off = 0;
            while ((off < contents.length)) {
                int bytesRead = sevenZFile.read(contents, off, contents.length - off);
                assert(bytesRead >= 0);
                off += bytesRead;
            }
            assertEquals("Hello, world!\n", new String(contents, "UTF-8"));
            assertNull(sevenZFile.getNextEntry());
        } finally {
            sevenZFile.close();
        }
    }
}
