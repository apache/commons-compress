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

import org.apache.commons.compress.archivers.ArchiveInputStream;

/**
 * Implementation for a delete operation
 */
class DeleteChange implements Change {
	private String filename = null;
	
	/**
	 * Constructor. Takes the filename of the file to be deleted
	 * from the stream as argument.
	 * @param pFilename the filename of the file to delete
	 */
	public DeleteChange(final String pFilename) {
		if(pFilename == null) {
			throw new NullPointerException();
		}
		filename = pFilename;
	}
	
	public void perform(ArchiveInputStream input) {
		System.out.println("PERFORMING DELETE");
	}

	public String targetFile() {
		return filename;
	}
	
	public int type() {
		return ChangeSet.CHANGE_TYPE_DELETE;
	}
}
