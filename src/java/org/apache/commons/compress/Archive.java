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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
/**
 * Archive is the interface which defines all operations 
 * for all possible archive-operations.
 * 
 * TODO:
 * - delete files from an archive
 * - add files to an existing archive
 * - recursivley add directories
 */
public interface Archive {
	/**
	 * Adds a file to the internal filelist 
	 * for a possible pack-operation
	 */
	public void add(File file) throws FileNotFoundException;
	
	/**
	 * Adds a FileInputStream to the internal filelist 
	 * for a possible pack-operation
	 */
	public void add(ArchiveEntry entry);
	
	/**
	 * Packs a file. 
	 * The destination filename must be set manually with setDestinationFile(...).
	 * There must be at least 1 file to be packed.
	 * 
	 * @throws ArchiveException if there is no destination file or files to be packed
	 * @return true, if the operation has been ended without exceptions
	 */
	public void save(FileOutputStream output) throws ArchiveException;

	/**
	 * Packs this file. 
	 * This methods ignores what has been set in setDestinationFile(...) and
	 * uses the filename of the parameter. This string must not be null.
	 * 
	 * @throws ArchiveException if there is no destination file or files to be packed
	 */
	public void save(File output) throws ArchiveException;

	/**
	 * Sets an Archive for manipulating. An archive is set if someone
	 * saves an Archive or calls getInstance(...) with an archive.
	 * @param archive file to manipulate
	 */
	void setArchive(File file);
	
	/**
	 * Returns the archive file and null,
	 * if this archiver has not been saved yet or
	 * there has not been set an archive manually.
	 * @return the archiver, or null
	 */
	public File getArchive();

	/**
	 * Unpacks to the specified directory 
	 * @param dir to unpack
	 * @throws UnpackException if an unpack error occurs
	 */
	public void unpack(File destinationDir) throws UnpackException;
	
	/**
	 * Get an iterator of ArchiveEntrys which shall be archived
	 * @return the iterator
	 */
	public Iterator getEntryIterator();
	
	/**
	 * Closes this archiver and all internal streams. 
	 */
	public void close() throws IOException ;
}
