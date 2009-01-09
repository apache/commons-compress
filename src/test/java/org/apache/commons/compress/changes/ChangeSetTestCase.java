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
package org.apache.commons.compress.changes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

import junit.framework.TestCase;

import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;

public final class ChangeSetTestCase extends TestCase {

	public void testDeleteFromZip() throws Exception {
		ArchiveOutputStream out = null;
		ArchiveInputStream ais = null;
		try {
			ChangeSet changes = new ChangeSet();
			changes.delete("test2.xml");
			
			final File input = new File(getClass().getClassLoader().getResource("bla.zip").getFile());
			final InputStream is = new FileInputStream(input);
			ais = new ArchiveStreamFactory().createArchiveInputStream("zip", is);
			
			File temp = File.createTempFile("test", ".zip");
			out = new ArchiveStreamFactory().createArchiveOutputStream("zip", new FileOutputStream(temp));
			
			System.out.println(temp.getAbsolutePath());
			changes.perform(ais, out);
		} finally {
			if(out != null) out.close();
			if(ais != null) ais.close();
		}
	}
	
	public void testDeleteFromTar() throws Exception {
		ArchiveOutputStream out = null;
		ArchiveInputStream ais = null;
		try {
			ChangeSet changes = new ChangeSet();
			changes.delete("test2.xml");
			
			final File input = new File(getClass().getClassLoader().getResource("bla.tar").getFile());
			final InputStream is = new FileInputStream(input);
			ais = new ArchiveStreamFactory().createArchiveInputStream("tar", is);
			
			File temp = File.createTempFile("test", ".tar");
			out = new ArchiveStreamFactory().createArchiveOutputStream("tar", new FileOutputStream(temp));
			
			System.out.println(temp.getAbsolutePath());
			changes.perform(ais, out);
		} finally {
			if(out != null) out.close();
			if(ais != null) ais.close();
		}
	}

	public void testDeleteFromJar() throws Exception {
		ArchiveOutputStream out = null;
		ArchiveInputStream ais = null;
		try {
			ChangeSet changes = new ChangeSet();
			changes.delete("test2.xml");
			changes.delete("META-INF/MANIFEST.MF");
			
			final File input = new File(getClass().getClassLoader().getResource("bla.jar").getFile());
			final InputStream is = new FileInputStream(input);
			ais = new ArchiveStreamFactory().createArchiveInputStream("jar", is);
			
			File temp = File.createTempFile("test", ".jar");
			out = new ArchiveStreamFactory().createArchiveOutputStream("jar", new FileOutputStream(temp));
			
			System.out.println(temp.getAbsolutePath());
			changes.perform(ais, out);
		} finally {
			if(out != null) out.close();
			if(ais != null) ais.close();
		}
	}
	
	public void testDeleteFromAr() throws Exception {
		ArchiveOutputStream out = null;
		ArchiveInputStream ais = null;
		try {
			ChangeSet changes = new ChangeSet();
			changes.delete("test2.xml");
			
			final File input = new File(getClass().getClassLoader().getResource("bla.ar").getFile());
			final InputStream is = new FileInputStream(input);
			ais = new ArchiveStreamFactory().createArchiveInputStream("ar", is);
			
			File temp = File.createTempFile("test", ".ar");
			out = new ArchiveStreamFactory().createArchiveOutputStream("ar", new FileOutputStream(temp));
			
			System.out.println(temp.getAbsolutePath());
			changes.perform(ais, out);
		} finally {
			if(out != null) out.close();
			if(ais != null) ais.close();
		}
	}
	
	public void testDeleteFromAndAddToZip() throws Exception {
		ArchiveOutputStream out = null;
		ArchiveInputStream ais = null;
		try {
			ChangeSet changes = new ChangeSet();
			changes.delete("test2.xml");
			
			
			final File file1 = new File(getClass().getClassLoader().getResource("test.txt").getFile());
			ZipArchiveEntry entry = new ZipArchiveEntry("testdata/test.txt");
	        changes.add(entry, new FileInputStream(file1));
			
			final File input = new File(getClass().getClassLoader().getResource("bla.zip").getFile());
			final InputStream is = new FileInputStream(input);
			ais = new ArchiveStreamFactory().createArchiveInputStream("zip", is);
			
			File temp = File.createTempFile("test", ".zip");
			out = new ArchiveStreamFactory().createArchiveOutputStream("zip", new FileOutputStream(temp));
			
			System.out.println(temp.getAbsolutePath());
			changes.perform(ais, out);
		} finally {
			if(out != null) out.close();
			if(ais != null) ais.close();
		}
	}

	public void testDeleteFromAndAddToTar() throws Exception {
		ArchiveOutputStream out = null;
		ArchiveInputStream ais = null;
		try {
			ChangeSet changes = new ChangeSet();
			changes.delete("test2.xml");
			
			
			final File file1 = new File(getClass().getClassLoader().getResource("test.txt").getFile());
			
			final TarArchiveEntry entry = new TarArchiveEntry("testdata/test.txt");
		    entry.setModTime(0);
		    entry.setSize(file1.length());
		    entry.setUserId(0);
		    entry.setGroupId(0);
		    entry.setUserName("avalon");
		    entry.setGroupName("excalibur");
		    entry.setMode(0100000);
			
	        changes.add(entry, new FileInputStream(file1));
			
			final File input = new File(getClass().getClassLoader().getResource("bla.tar").getFile());
			final InputStream is = new FileInputStream(input);
			ais = new ArchiveStreamFactory().createArchiveInputStream("tar", is);
			
			File temp = File.createTempFile("test", ".tar");
			out = new ArchiveStreamFactory().createArchiveOutputStream("tar", new FileOutputStream(temp));
			
			System.out.println(temp.getAbsolutePath());
			changes.perform(ais, out);
		} finally {
			if(out != null) out.close();
			if(ais != null) ais.close();
		}
	}
	
	public void testDeleteFromAndAddToJar() throws Exception {
		ArchiveOutputStream out = null;
		ArchiveInputStream ais = null;
		try {
			ChangeSet changes = new ChangeSet();
			changes.delete("test2.xml");
			
			final File file1 = new File(getClass().getClassLoader().getResource("test.txt").getFile());
			JarArchiveEntry entry = new JarArchiveEntry("testdata/test.txt");
	        changes.add(entry, new FileInputStream(file1));
			
			final File input = new File(getClass().getClassLoader().getResource("bla.jar").getFile());
			final InputStream is = new FileInputStream(input);
			ais = new ArchiveStreamFactory().createArchiveInputStream("jar", is);
			
			File temp = File.createTempFile("test", ".jar");
			out = new ArchiveStreamFactory().createArchiveOutputStream("jar", new FileOutputStream(temp));
			
			System.out.println(temp.getAbsolutePath());
			changes.perform(ais, out);
		} finally {
			if(out != null) out.close();
			if(ais != null) ais.close();
		}
	}
	
	public void testDeleteFromAndAddToAr() throws Exception {
		ArchiveOutputStream out = null;
		ArchiveInputStream ais = null;
		try {
			ChangeSet changes = new ChangeSet();
			changes.delete("test2.xml");
			
			final File file1 = new File(getClass().getClassLoader().getResource("test.txt").getFile());
			
			final ArArchiveEntry entry = new ArArchiveEntry("test.txt", file1.length());
		   
	        changes.add(entry, new FileInputStream(file1));
			
			final File input = new File(getClass().getClassLoader().getResource("bla.ar").getFile());
			final InputStream is = new FileInputStream(input);
			ais = new ArchiveStreamFactory().createArchiveInputStream("ar", is);
			
			File temp = File.createTempFile("test", ".ar");
			out = new ArchiveStreamFactory().createArchiveOutputStream("ar", new FileOutputStream(temp));
			
			System.out.println(temp.getAbsolutePath());
			changes.perform(ais, out);
		} finally {
			if(out != null) out.close();
			if(ais != null) ais.close();
		}
	}
}
