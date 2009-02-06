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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.compress.archivers.zip.ZipEntry;
import org.apache.commons.compress.utils.IOUtils;

import org.apache.commons.compress.AbstractTestCase;

public final class ZipTestCase extends AbstractTestCase {
	public void testZipArchiveCreation() throws Exception {
		
		final File output = new File(dir, "bla.zip");
		
		final File file1 = getFile("test1.xml");
		final File file2 = getFile("test2.xml");
		
        final OutputStream out = new FileOutputStream(output);
        
        final ArchiveOutputStream os = new ArchiveStreamFactory().createArchiveOutputStream("zip", out);

        os.putArchiveEntry(new ZipEntry("testdata/test1.xml"));
        IOUtils.copy(new FileInputStream(file1), os);
        os.closeArchiveEntry();
        
        os.putArchiveEntry(new ZipEntry("testdata/test2.xml"));
        IOUtils.copy(new FileInputStream(file2), os);
        os.closeArchiveEntry();
        
        os.close();
    }
    public void testZipUnarchive() throws Exception {

		final File input = getFile("bla.zip");
    	
        final InputStream is = new FileInputStream(input);
        final ArchiveInputStream in = new ArchiveStreamFactory().createArchiveInputStream("zip", is);
 
        final ZipEntry entry = (ZipEntry)in.getNextEntry();
        final OutputStream out = new FileOutputStream(new File(dir, entry.getName()));
        
        IOUtils.copy(in, out);
    
        out.close();
        in.close();
    }

}
