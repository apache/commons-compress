/*
 * Copyright 2002,2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.compress.archivers.tar;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import org.apache.commons.compress.AbstractArchive;
import org.apache.commons.compress.ArchiveEntry;
import org.apache.commons.compress.ArchiveException;
import org.apache.commons.compress.UnpackException;

/**
 * Archive-Implementation for Tar.
 * An tar archive has no header. This means, that the 
 * ArchiverFactory.getInstance( new File("file.tar")) Method
 * cannot be used.
 */
public class TarArchive extends AbstractArchive {

	/* Buffer for the file operations */
	private static final int BUFFER = 2048;

	/**
	 * DEFAULT_FILE_EXTENSION Field for this archiver.
	 */
	public final static String DEFAULT_FILE_EXTENSION = "tar";

	/**
	 * ARCHIVER_NAME Field for this archiver.
	 */
	private final static String ARCHIVER_NAME = "tar";
	
	/**
	 * This Archive should be instantiated in the Archive-Interface.
	 */
	public TarArchive() {
		// Empty
	}
	
	/* (non-Javadoc)
	 * @see org.apache.commons.compress.Archive#unpack()
	 */
	protected void doUnpack(File unpackDir) throws UnpackException {
		FileInputStream fInputStream = null;
		TarInputStream zInputStream = null;
		
		try {
			fInputStream = new FileInputStream(this.getArchive());
			zInputStream = new TarInputStream(new BufferedInputStream(fInputStream));
		} catch(FileNotFoundException e) {
			throw new UnpackException("SourceFile could not be found.", e);
		}
		
		try {
			TarEntry entry = null;
			
			while((entry = zInputStream.getNextEntry()) != null) {
				BufferedOutputStream destination = null;
				
				int count;
				byte data[] = new byte[BUFFER];

				String filename = "";
				File f = new File(entry.getName());
				if(f.isAbsolute()) {
					filename = entry.getName().substring(3);
				} else {
					filename = entry.getName();
				}
				
				String fosString = unpackDir.getPath() + File.separator + filename;
				File destFile = new File(fosString);
				File destPath = new File(destFile.getParent());
				destPath.mkdirs();
				
				FileOutputStream fos = new FileOutputStream(destFile);
				try {
					destination = new BufferedOutputStream(fos, BUFFER);
					
					while((count = zInputStream.read(data, 0, BUFFER))!= -1) {
						destination.write(data, 0, count);
					}
					destination.flush();
				} finally {
					destination.close();
				}
			}
		} catch(IOException e) {
			throw new UnpackException("Exception while unpacking.", e);
		} finally {
			try {
				fInputStream.close();
			} catch (IOException e1) {
				throw new UnpackException("Exception while unpacking.", e1);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.apache.commons.compress.AbstractArchive#doSave(java.io.FileOutputStream)
	 */
	public void doSave(FileOutputStream output) throws ArchiveException {
		// Stream initializing
		BufferedInputStream origin = null;
		
		//out.setMethod(ZipOutputStream.DEFLATED);
		byte data[] = new byte[BUFFER];
		
		// get a list of files from current directory
		// less than one file leads to an exception
		Iterator iterator = this.getEntryIterator();
		if(!iterator.hasNext()) {
			throw new ArchiveException("There must be at least one file to be pack.");
		}
		
		// Pack-Operation
		TarOutputStream out = null;
		
		try {
			out = new TarOutputStream(new BufferedOutputStream(output));
			while(iterator.hasNext()) {

				ArchiveEntry archiveEntry = (ArchiveEntry)iterator.next();
				InputStream fInputStream = archiveEntry.getStream();

				TarEntry entry = new TarEntry(archiveEntry.getName());
				entry.setModTime( 0 );
		        entry.setSize( fInputStream.available() );
		        entry.setUserID( 0 );
		        entry.setGroupID( 0 );
		        entry.setUserName( "avalon" );
		        entry.setGroupName( "excalibur" );
		        entry.setMode( 0100000 );
		        out.putNextEntry( entry );
		        
		        out.copyEntryContents( fInputStream );
		        out.closeEntry();
			}
		} catch (IOException e) {
			throw new ArchiveException("Creation of this archive failed cause of IOExceptions.", e);
		} finally {
			try {
				out.close();
			} catch (IOException e1) {
				throw new ArchiveException("Creation of this archive failed cause of IOExceptions.", e1);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.apache.commons.compress.Archive#getArchiverName()
	 */
	public String getName() {
		return ARCHIVER_NAME;
	}

	/* (non-Javadoc)
	 * @see org.apache.commons.compress.Archive#getDefaultFileExtension()
	 */
	public String getDefaultFileExtension() {
		return DEFAULT_FILE_EXTENSION;
	}

	/* (non-Javadoc)
	 * @see org.apache.commons.compress.Archive#getHeader()
	 */
	public byte[] getHeader() {
		// tar's have no specific header
		return null;
	}
}