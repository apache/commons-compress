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
package org.apache.commons.compress.archivers.zip;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.AbstractArchive;
import org.apache.commons.compress.ArchiveEntry;
import org.apache.commons.compress.ArchiveException;
import org.apache.commons.compress.UnpackException;

/**
 * Archive-Implementation for Zip.
 */
public class ZipArchive extends AbstractArchive {

	/* Buffer for the file operations */
	private static final int BUFFER = 2048;

	/**
	 * HEADER Field for this archiver.
	 */
	private static final byte[] HEADER = { 0x50, 0x4b, 0x03, 0x04 };
	
	/**
	 * DEFAULT_FILE_EXTENSION Field for this archiver.
	 */
	private static String DEFAULT_FILE_EXTENSION = "zip";

	/**
	 * ARCHIVER_NAME Field for this archiver.
	 */
	private static final String ARCHIVER_NAME = "zip";
	
	/**
	 * This Archive should be instantiated in the Archive-Interface.
	 */
	public ZipArchive() {
		// Empty
	}
		
	/* (non-Javadoc)
	 * @see org.apache.commons.compress.Archive#unpack()
	 */
	protected void doUnpack(File unpackDir) throws UnpackException {
		BufferedOutputStream destination = null;
		FileInputStream fInputStream = null;
		
		try {
			fInputStream = new FileInputStream(this.getArchive());
		} catch(FileNotFoundException e) {
			throw new UnpackException("SourceFile could not be found.", e);
		}
		ZipInputStream zInputStream = null;
		try {
			// TODO: we have no ZipInputStream yet, so we need the sun implementation
			zInputStream = new ZipInputStream(new BufferedInputStream(fInputStream));
			java.util.zip.ZipEntry entry;
			
			while((entry = zInputStream.getNextEntry()) != null) {
				int count;
				byte data[] = new byte[BUFFER];

				String fosString = unpackDir.getAbsolutePath() + File.separator + entry.getName();
				FileOutputStream fos = new FileOutputStream(fosString);
				destination = new BufferedOutputStream(fos, BUFFER);
				
				while((count = zInputStream.read(data, 0, BUFFER))!= -1) {
					destination.write(data, 0, count);
				}
				destination.flush();
				destination.close();
			}
		} catch(IOException e) {
			throw new UnpackException("Exception while unpacking.", e);
		} finally {
			try {
				zInputStream.close();
			} catch (IOException e1) {
				throw new UnpackException("Exception while unpacking.", e1);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.apache.commons.compress.Archive#pack()
	 */
	protected void doSave(FileOutputStream output) throws ArchiveException {
		// Stream initializing
		BufferedInputStream origin = null;
		
		//out.setMethod(ZipOutputStream.DEFLATED);
		byte data[] = new byte[BUFFER];
		
		// get a list of filesStreams from current directory
		// less than one file leads to an exception
		Iterator iterator = this.getEntryIterator();
		if(!iterator.hasNext()) {
			throw new ArchiveException("There must be at least one file to be pack.");
		}
		
		// Pack-Operation
		ZipOutputStream out = null;
		try {
			out = new ZipOutputStream(new BufferedOutputStream(output));
			while(iterator.hasNext()) {
				ArchiveEntry archiveEntry = (ArchiveEntry)iterator.next();
				InputStream fInputStream = archiveEntry.getStream();

				origin = new BufferedInputStream(fInputStream, BUFFER);
				ZipEntry entry = new ZipEntry(archiveEntry.getName());
				out.putNextEntry(entry);
			
				int count;
				while((count = origin.read(data, 0,	BUFFER)) != -1) {
					out.write(data, 0, count);
				}
				origin.close();
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
		return HEADER;
	}
}