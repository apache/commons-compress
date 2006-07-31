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
package org.apache.commons.compress;

import java.io.InputStream;
/**
 * Represents an entry of an archive.
 */
public class ArchiveEntry {
	/* Name of this entry */
	private String name = null;
	/* name of this entry stream */
	private InputStream stream = null;
	
	/*
	 * Should only called with parameters 
	 */
	private ArchiveEntry() {
		// unused
	}
	
	/**
	 * Constructs a new ArchiveEntry with name and stram
	 * @param name the name of this entry
	 * @param stream the inputstream of this entry
	 */
	public ArchiveEntry(String entryName, InputStream entryStream) {
		super();
		this.name = entryName;
		this.stream = entryStream;
	}
	
	/**
	 * Returns this entries name
	 * @return name of this entry
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Returns this entries InputStream stream
	 * @return InputStream of this entry
	 */
	public InputStream getStream() {
		return stream;
	}
}