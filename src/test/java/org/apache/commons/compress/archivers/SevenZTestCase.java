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
package org.apache.commons.compress.archivers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.sevenz.SevenZMethod;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;

public class SevenZTestCase extends AbstractTestCase {

    public void testSevenZArchiveCreationUsingCopy() throws Exception {
        testSevenZArchiveCreation(SevenZMethod.COPY);
    }
    
    public void testSevenZArchiveCreationUsingLZMA2() throws Exception {
        testSevenZArchiveCreation(SevenZMethod.LZMA2);
    }
    
    public void testSevenZArchiveCreationUsingBZIP2() throws Exception {
        testSevenZArchiveCreation(SevenZMethod.BZIP2);
    }
    
    public void testSevenZArchiveCreationUsingDeflate() throws Exception {
        testSevenZArchiveCreation(SevenZMethod.DEFLATE);
    }
    
    private void testSevenZArchiveCreation(SevenZMethod method) throws Exception {
        final File output = new File(dir, "bla.7z");
        final File file1 = getFile("test1.xml");
        final File file2 = getFile("test2.xml");

        final SevenZOutputFile outArchive = new SevenZOutputFile(output);
        outArchive.setContentCompression(method);
        try {
            SevenZArchiveEntry entry;
            
            entry = outArchive.createArchiveEntry(file1, file1.getName());
            outArchive.putArchiveEntry(entry);
            copy(file1, outArchive);
            outArchive.closeArchiveEntry();
            
            entry = outArchive.createArchiveEntry(file2, file2.getName());
            outArchive.putArchiveEntry(entry);
            copy(file2, outArchive);
            outArchive.closeArchiveEntry();
        } finally {
            outArchive.close();
        }
        
        final SevenZFile archive = new SevenZFile(output);
        try {
            SevenZArchiveEntry entry;
            
            entry = archive.getNextEntry();
            assert(entry != null);
            assertEquals(entry.getName(), file1.getName());
            
            entry = archive.getNextEntry();
            assert(entry != null);
            assertEquals(entry.getName(), file2.getName());
            
            assert(archive.getNextEntry() == null);
        } finally {
            archive.close();
        }
    }

    private void copy(final File src, final SevenZOutputFile dst) throws IOException { 
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(src);
            final byte[] buffer = new byte[8*1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) >= 0) {
                dst.write(buffer, 0, bytesRead);
            }
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
    }
}
