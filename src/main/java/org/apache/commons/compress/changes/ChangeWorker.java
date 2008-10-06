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

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;

/**
 * Performs the operations of a change set
 */
public class ChangeWorker {
	private ChangeWorker() {
		// nothing to do
	}
	
	/**
	 * TODO
	 * @param changes
	 * @param in
	 * @param out
	 * @throws IOException 
	 */
	public static void perform(ChangeSet changes, ArchiveInputStream in, ArchiveOutputStream out) throws IOException {
		ArchiveEntry entry = null;	
		while((entry = in.getNextEntry()) != null) {
			System.out.println(entry.getName());
			boolean copy = true; 
			
			for (Iterator it = changes.asSet().iterator(); it.hasNext();) {
				Change change = (Change)it.next();
				
				if(change.type() == ChangeSet.CHANGE_TYPE_DELETE) {
					DeleteChange delete = ((DeleteChange)change);
					if(entry.getName() != null &&
					   entry.getName().equals(delete.targetFile())) {
						copy = false;
					}
				}
			}
			
			if(copy) {
				// copy archive
				// TODO: unsafe long to int 
				System.out.println("Copy: " + entry.getName());
				long size = entry.getSize();
				out.putArchiveEntry(entry);
				IOUtils.copy((InputStream)in, out, (int)size);
				out.closeArchiveEntry();
			}
			
			
			System.out.println("---");
		}
		// add operation stuff
		out.close();
	}
}
