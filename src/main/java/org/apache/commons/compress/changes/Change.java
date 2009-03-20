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

import java.io.InputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;


public class Change {
	private final String targetFile;
	private final ArchiveEntry entry;
	private final InputStream input;
	private final int type;
	
	static final int TYPE_DELETE = 1;
	static final int TYPE_ADD = 2;
	static final int TYPE_MOVE = 3; // NOT USED
	
	/**
	 * Constructor. Takes the filename of the file to be deleted
	 * from the stream as argument.
	 * @param pFilename the filename of the file to delete
	 */
	public Change(final String pFilename) {
		if(pFilename == null) {
			throw new NullPointerException();
		}
		targetFile = pFilename;
		type = TYPE_DELETE;
		input = null;
		entry = null;
	}
	
//	public Change(final String pOldname, final ArchiveEntry pEntry) {
//		if(pOldname == null || pEntry == null) {
//			throw new NullPointerException();
//		}
//		targetFile = pOldname;
//		entry = pEntry;
//		type = TYPE_MOVE;
//	}
	
	public Change(final ArchiveEntry pEntry, final InputStream pInput) {
		if(pEntry == null || pInput == null) {
			throw new NullPointerException();
		}
		this.entry = pEntry;
		this.input = pInput;
		type = TYPE_ADD;
		targetFile = null;
	}
	
	public ArchiveEntry getEntry() {
		return entry;
	}

	public InputStream getInput() {
		return input;
	}

	public String targetFile() {
		return targetFile;
	}
	
	public int type() {
		return type;
	}
}
