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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.utils.IOUtils;

public final class TarTestCase extends AbstractTestCase {
    public void testTarArchiveCreation() throws Exception {
		final File output = new File(dir, "bla.tar");
		final File file1 = getFile("test1.xml");
    	final OutputStream out = new FileOutputStream(output);
        final ArchiveOutputStream os = new ArchiveStreamFactory().createArchiveOutputStream("tar", out);
        final TarArchiveEntry entry = new TarArchiveEntry("testdata/test1.xml");
        entry.setModTime(0);
        entry.setSize(file1.length());
        entry.setUserId(0);
        entry.setGroupId(0);
        entry.setUserName("avalon");
        entry.setGroupName("excalibur");
        entry.setMode(0100000);
        os.putArchiveEntry(entry);
        IOUtils.copy(new FileInputStream(file1), os);
        os.closeArchiveEntry();
        os.close();
    }
    
    public void testTarArchiveLongNameCreation() throws Exception {
    	String name = "testdata/12345678901234567890123456789012345678901234567890123456789012345678901234567890123456.xml";
    	byte[] bytes = name.getBytes();
    	assertEquals(bytes.length, 99);
    	
		final File output = new File(dir, "bla.tar");
		final File file1 = getFile("test1.xml");
    	final OutputStream out = new FileOutputStream(output);
        final ArchiveOutputStream os = new ArchiveStreamFactory().createArchiveOutputStream("tar", out);
        final TarArchiveEntry entry = new TarArchiveEntry(name);
        entry.setModTime(0);
        entry.setSize(file1.length());
        entry.setUserId(0);
        entry.setGroupId(0);
        entry.setUserName("avalon");
        entry.setGroupName("excalibur");
        entry.setMode(0100000);
        os.putArchiveEntry(entry);
        IOUtils.copy(new FileInputStream(file1), os);
        os.closeArchiveEntry();
        os.close();
        
        
        ArchiveOutputStream os2 = null;
        try {
        	String toLongName = "testdata/123456789012345678901234567890123456789012345678901234567890123456789012345678901234567.xml";
        	final File output2 = new File(dir, "bla.tar");
        	final OutputStream out2 = new FileOutputStream(output2);
        	os2 = new ArchiveStreamFactory().createArchiveOutputStream("tar", out2);
        	final TarArchiveEntry entry2 = new TarArchiveEntry(toLongName);
        	entry2.setModTime(0);
        	entry2.setSize(file1.length());
        	entry2.setUserId(0);
        	entry2.setGroupId(0);
        	entry2.setUserName("avalon");
        	entry2.setGroupName("excalibur");
        	entry2.setMode(0100000);
        	os.putArchiveEntry(entry);
        	IOUtils.copy(new FileInputStream(file1), os2);
        } catch(IOException e) {
        	assertTrue(true);
        } finally {
        	if (os2 != null){
        	    os2.closeArchiveEntry();
        	}
        }
    }
    
    public void testTarUnarchive() throws Exception {
		final File input = getFile("bla.tar");
		final InputStream is = new FileInputStream(input);
        final ArchiveInputStream in = new ArchiveStreamFactory().createArchiveInputStream("tar", is);
        final TarArchiveEntry entry = (TarArchiveEntry)in.getNextEntry();
        final OutputStream out = new FileOutputStream(new File(dir, entry.getName()));
        IOUtils.copy(in, out);
        out.close();
        in.close();
    }

}
