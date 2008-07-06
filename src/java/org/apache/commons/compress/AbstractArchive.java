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
package org.apache.commons.compress;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Abstract implementation of an archiver
 */
public abstract class AbstractArchive extends PackableObject implements Archive {
	
	/* hold filestreams for a possible pack operation */
	private ArrayList entries = new ArrayList();
	
	/* the source of an unpack-operation */
	private File archive = null;

	/**
	 * Compresses the file with the given String as a filename
	 * @see org.apache.commons.compress.Archive#save(java.lang.String)
	 */
	public void save(File output) throws ArchiveException {
		if(output == null) {
			throw new ArchiveException("Destination directory must not be null.");
		}
		try {
			this.save(new FileOutputStream(output));
		} catch (FileNotFoundException e) {
			throw new ArchiveException("This path is not writeable", e);
		}
		this.setArchive(output);
	}
	
	/* (non-Javadoc)
	 * @see org.apache.commons.compress.Archive#save(java.io.FileOutputStream)
	 */
	public void save(OutputStream output) throws ArchiveException {
		doSave(output);
	}
	
	/**
	 * Specific implementation of the save opteration. 
	 * @param output - stream to archive to
	 * @throws ArchiveException 
	 */
	protected abstract void doSave(OutputStream output) throws ArchiveException;
	
	/* (non-Javadoc)
	 * @see org.apache.commons.compress.Archive#getArchive()
	 */
	public File getArchive() {
		return this.archive;
	}
	
	/* (non-Javadoc)
	 * @see org.apache.commons.compress.Archive#setArchive(java.io.File)
	 */
	public void setArchive(File file) {
		// TODO: when an archive is set, it's files must be added to the
		// internal file list for possible delete operations
		this.archive = file;
	}

	/* (non-Javadoc)
	 * @see org.apache.commons.compress.Archive#add(java.io.File)
	 */
	public void add(File file) throws FileNotFoundException {
		InputStream is = new FileInputStream(file);
		ArchiveEntry archiveEntry = new ArchiveEntry(file.getName(), is);
		entries.add( archiveEntry );
	}

	/* (non-Javadoc)
	 * @see org.apache.commons.compress.Archive#add(java.io.InputStream)
	 */
	public void add(ArchiveEntry archiveEntry) {
		entries.add( archiveEntry );
	}
	
	/* (non-Javadoc)
	 * @see org.apache.commons.compress.Archive#getPackFilesIterator()
	 */
	public Iterator getEntryIterator() {
		return this.entries.iterator(); 
	}

	/* (non-Javadoc)
	 * @see org.apache.commons.compress.Archive#unpack(java.lang.String)
	 */
	public void unpack(File unpackDir) throws UnpackException {
		if(unpackDir == null) {
			throw new UnpackException("Destination directory must not be null.");
		}
		if(!unpackDir.isDirectory()) {
			throw new UnpackException("This file must be a valid directory.");
		}
		if(!unpackDir.canWrite()) {
			throw new UnpackException("This path is not writeable");
		}
		doUnpack(unpackDir);
	}
	
	/**
	 * Specific implementation of the unpack opteration. 
	 * @param unpackDir dir, to unpack to
	 * @throws UnpackException 
	 */
	protected abstract void doUnpack(File unpackDir) throws UnpackException;
	
	/* (non-Javadoc)
	 * @see org.apache.commons.compress.Archive#close()
	 */
	public void close() throws IOException {
		Iterator it = getEntryIterator();
		while(it.hasNext()) {
			ArchiveEntry ae = (ArchiveEntry)it.next();
			InputStream is = ae.getStream();
			is.close();
		}
	}
}
