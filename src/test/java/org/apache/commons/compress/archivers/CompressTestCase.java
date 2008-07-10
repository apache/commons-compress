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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import junit.framework.TestCase;

import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;

public final class CompressTestCase extends TestCase {

	private File dir;
	
	protected void setUp() throws Exception {
		dir = File.createTempFile("dir", "");
		dir.delete();
		dir.mkdir();
	}

	protected void tearDown() throws Exception {
		dir.delete();
		dir = null;
	}


	public void testGzipCreation()  throws Exception {
		final File output = new File(dir, "bla.gz");
		final File file1 = new File(getClass().getClassLoader().getResource("test1.xml").getFile());
		final OutputStream out = new FileOutputStream(output);
		CompressorOutputStream cos = new CompressorStreamFactory().createCompressorOutputStream("gz", out);
		IOUtils.copy(new FileInputStream(file1), cos);
		cos.close();
	}
	
	public void testGzipUnarchive() throws Exception {
		final File output = new File(dir, "bla-entpackt.tar");
		final File input = new File(getClass().getClassLoader().getResource("bla.tgz").getFile());
        final InputStream is = new FileInputStream(input);
        final CompressorInputStream in = new CompressorStreamFactory().createCompressorInputStream("gz", is);
        IOUtils.copy(in, new FileOutputStream(output));
		in.close();
    }
	
	public void testBzipCreation()  throws Exception {
		final File output = new File(dir, "bla.txt.bz2");
		System.out.println(dir);
		final File file1 = new File(getClass().getClassLoader().getResource("test.txt").getFile());
		final OutputStream out = new FileOutputStream(output);
		CompressorOutputStream cos = new CompressorStreamFactory().createCompressorOutputStream("bzip2", out);
		IOUtils.copy(new FileInputStream(file1), cos);
		cos.close();
	}
	
	public void testBzip2Unarchive() throws Exception {
		final File output = new File(dir, "test-entpackt.txt");
		System.out.println(dir);
		final File input = new File(getClass().getClassLoader().getResource("bla.txt.bz2").getFile());
        final InputStream is = new FileInputStream(input);
        //final CompressorInputStream in = new CompressorStreamFactory().createCompressorInputStream("bzip2", is);
        final CompressorInputStream in = new BZip2CompressorInputStream(is);
        IOUtils.copy(in, new FileOutputStream(output));
		in.close();
    }
	
	public void testJarArchiveCreation() throws Exception {
		final File output = new File(dir, "bla.jar");

		final File file1 = new File(getClass().getClassLoader().getResource("test1.xml").getFile());
		final File file2 = new File(getClass().getClassLoader().getResource("test2.xml").getFile());
		
        final OutputStream out = new FileOutputStream(output);
        
        final ArchiveOutputStream os = new ArchiveStreamFactory().createArchiveOutputStream("jar", out);

        os.putArchiveEntry(new ZipArchiveEntry("testdata/test1.xml"));
        IOUtils.copy(new FileInputStream(file1), os);
        os.closeArchiveEntry();
        
        os.putArchiveEntry(new ZipArchiveEntry("testdata/test2.xml"));
        IOUtils.copy(new FileInputStream(file2), os);
        os.closeArchiveEntry();

        os.close();
    }
	
	public void testJarUnarchive() throws Exception {
		final File input = new File(getClass().getClassLoader().getResource("bla.jar").getFile());
        final InputStream is = new FileInputStream(input);
        final ArchiveInputStream in = new ArchiveStreamFactory().createArchiveInputStream("jar", is);
        
        ZipArchiveEntry entry = (ZipArchiveEntry)in.getNextEntry();
        File o = new File(dir, entry.getName());
        o.getParentFile().mkdirs();
        OutputStream out = new FileOutputStream(o);
        IOUtils.copy(in, out);
        out.close();
        
        entry = (ZipArchiveEntry)in.getNextEntry();
        o = new File(dir, entry.getName());
        o.getParentFile().mkdirs();
        out = new FileOutputStream(o);
        IOUtils.copy(in, out);
        out.close();
        
        entry = (ZipArchiveEntry)in.getNextEntry();
        o = new File(dir, entry.getName());
        o.getParentFile().mkdirs();
        out = new FileOutputStream(o);
        IOUtils.copy(in, out);
        out.close();
        
        in.close();
    }
	
	
	public void testDetection() throws Exception {
		final ArchiveStreamFactory factory = new ArchiveStreamFactory();

		final ArchiveInputStream ar = factory.createArchiveInputStream(
				new BufferedInputStream(new FileInputStream(
						new File(getClass().getClassLoader().getResource("bla.ar").getFile())))); 
		assertTrue(ar instanceof ArArchiveInputStream);

		final ArchiveInputStream tar = factory.createArchiveInputStream(
				new BufferedInputStream(new FileInputStream(
						new File(getClass().getClassLoader().getResource("bla.tar").getFile()))));
		assertTrue(tar instanceof TarArchiveInputStream);

		final ArchiveInputStream zip = factory.createArchiveInputStream(
				new BufferedInputStream(new FileInputStream(
						new File(getClass().getClassLoader().getResource("bla.zip").getFile()))));
		assertTrue(zip instanceof ZipArchiveInputStream);

		final ArchiveInputStream jar = factory.createArchiveInputStream(
				new BufferedInputStream(new FileInputStream(
						new File(getClass().getClassLoader().getResource("bla.jar").getFile()))));
		assertTrue(jar instanceof JarArchiveInputStream);

//		final ArchiveInputStream tgz = factory.createArchiveInputStream(
//				new BufferedInputStream(new FileInputStream(
//						new File(getClass().getClassLoader().getResource("bla.tgz").getFile()))));
//		assertTrue(tgz instanceof TarArchiveInputStream);
		
	}
	
	public void testArArchiveCreation() throws Exception {
		final File output = new File(dir, "bla.ar");
		
		final File file1 = new File(getClass().getClassLoader().getResource("test1.xml").getFile());
		final File file2 = new File(getClass().getClassLoader().getResource("test2.xml").getFile());
		
		final OutputStream out = new FileOutputStream(output);
        final ArchiveOutputStream os = new ArchiveStreamFactory().createArchiveOutputStream("ar", out);
		os.putArchiveEntry(new ArArchiveEntry("test1.xml", file1.length()));
		IOUtils.copy(new FileInputStream(file1), os);
		os.closeArchiveEntry();
		
		os.putArchiveEntry(new ArArchiveEntry("test2.xml", file2.length()));
		IOUtils.copy(new FileInputStream(file2), os);
		os.closeArchiveEntry();
		
		os.close();
	}
	
	public void testArUnarchive() throws Exception {
		final File output = new File(dir, "bla.ar");
		{
			final File file1 = new File(getClass().getClassLoader().getResource("test1.xml").getFile());
			final File file2 = new File(getClass().getClassLoader().getResource("test2.xml").getFile());
			
			final OutputStream out = new FileOutputStream(output);
	        final ArchiveOutputStream os = new ArchiveStreamFactory().createArchiveOutputStream("ar", out);
			os.putArchiveEntry(new ArArchiveEntry("test1.xml", file1.length()));
			IOUtils.copy(new FileInputStream(file1), os);
			os.closeArchiveEntry();
			
			os.putArchiveEntry(new ArArchiveEntry("test2.xml", file2.length()));
			IOUtils.copy(new FileInputStream(file2), os);
			os.closeArchiveEntry();
			os.close();
		}
		
		// UnArArchive Operation
		final File input = output;
		final InputStream is = new FileInputStream(input);
		final ArchiveInputStream in = new ArchiveStreamFactory().createArchiveInputStream("ar", is);
		final ArArchiveEntry entry = (ArArchiveEntry)in.getNextEntry();
		
		File target = new File(dir, entry.getName());
        final OutputStream out = new FileOutputStream(target);
        
        IOUtils.copy(in, out);
    
        out.close();
        in.close();
	}
	
	public void testZipArchiveCreation() throws Exception {
		
		final File output = new File(dir, "bla.zip");
		
		final File file1 = new File(getClass().getClassLoader().getResource("test1.xml").getFile());
		final File file2 = new File(getClass().getClassLoader().getResource("test2.xml").getFile());
		
        final OutputStream out = new FileOutputStream(output);
        
        final ArchiveOutputStream os = new ArchiveStreamFactory().createArchiveOutputStream("zip", out);

        os.putArchiveEntry(new ZipArchiveEntry("testdata/test1.xml"));
        IOUtils.copy(new FileInputStream(file1), os);
        os.closeArchiveEntry();
        
        os.putArchiveEntry(new ZipArchiveEntry("testdata/test2.xml"));
        IOUtils.copy(new FileInputStream(file2), os);
        os.closeArchiveEntry();
        
        os.close();
    }
    
    public void testTarArchiveCreation() throws Exception {

		final File output = new File(dir, "bla.tar");

		final File file1 = new File(getClass().getClassLoader().getResource("test1.xml").getFile());

    	final OutputStream out = new FileOutputStream(output);
        
        final ArchiveOutputStream os = new ArchiveStreamFactory().createArchiveOutputStream("tar", out);
        
        final TarArchiveEntry entry = new TarArchiveEntry("testdata/test1.xml");
        entry.setModTime(0);
        entry.setSize(file1.length());
        entry.setUserID(0);
        entry.setGroupID(0);
        entry.setUserName("avalon");
        entry.setGroupName("excalibur");
        entry.setMode(0100000);
        
        os.putArchiveEntry(entry);
        IOUtils.copy(new FileInputStream(file1), os);

        os.closeArchiveEntry();
        os.close();
    }
    
    public void testZipUnarchive() throws Exception {

		final File input = new File(getClass().getClassLoader().getResource("bla.zip").getFile());
    	
        final InputStream is = new FileInputStream(input);
        final ArchiveInputStream in = new ArchiveStreamFactory().createArchiveInputStream("zip", is);
 
        final ZipArchiveEntry entry = (ZipArchiveEntry)in.getNextEntry();
        final OutputStream out = new FileOutputStream(new File(dir, entry.getName()));
        
        IOUtils.copy(in, out);
    
        out.close();
        in.close();
    }
        
    public void testTarUnarchive() throws Exception {
		final File input = new File(getClass().getClassLoader().getResource("bla.tar").getFile());
		final InputStream is = new FileInputStream(input);
        final ArchiveInputStream in = new ArchiveStreamFactory().createArchiveInputStream("tar", is);
        final TarArchiveEntry entry = (TarArchiveEntry)in.getNextEntry();
        final OutputStream out = new FileOutputStream(new File(dir, entry.getName()));
        IOUtils.copy(in, out);
        out.close();
        in.close();
    }
    
//  public void testZipUnarchive() throws Exception {
//        ZipInputStream zip = 
//            new ZipInputStream(new FileInputStream("C:\\dev\\sources\\compress\\testdata\\bla.zip"));
//        Iterator iterator = zip.getEntryIterator();
//        while (iterator.hasNext()) {
//            ArchiveEntry entry = (ArchiveEntry) iterator.next();
//            OutputStream output = new FileOutputStream("testdata\\blub\\" + entry.getName());
//            IOUtils.copy(zip, output);
//        }
//        zip.close();
//    }

    
//	public void xtestFactoryUnarchive() throws Exception {
//		CompressUtils.unpack(new FileInputStream("bla.tgz"), new File("output"));
//	}
//
//	
//	public void xtestArUnarchive() throws Exception {
//		ArchiveInputStream ar = new ArArchiveInputStream(new FileInputStream("bla.tgz"));
//		Iterator iterator = ar.getEntryIterator();
//		while(iterator.hasNext()) {
//			ArchiveEntry entry = (ArchiveEntry) iterator.next();
//			OutputStream output = new FileOutputStream(entry.getName());
//			IOUtils.copy(ar, output);
//		}
//		ar.close();
//	}
//

//

}
