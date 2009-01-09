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
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;


public final class ChangeSet {

	private final Set changes = new LinkedHashSet();
	
	public void delete( final String pFilename ) {
		changes.add(new Change(pFilename));
	}

//	public void move( final String pFrom, final String pTo ) {
//		changes.add(new Change(pFrom, pTo));
//	}
	
	public void add( final ArchiveEntry pEntry, final InputStream pInput) {
		changes.add(new Change(pEntry, pInput));
	}
	
	public Set asSet() {
		return changes;
	}
	
	public void perform(ArchiveInputStream in, ArchiveOutputStream out) throws IOException {
		ArchiveEntry entry = null;	
		while((entry = in.getNextEntry()) != null) {
			boolean copy = true;
			
			for (Iterator it = changes.iterator(); it.hasNext();) {
				Change change = (Change)it.next();
				
				if(change.type() == Change.TYPE_ADD) {
					copyStream(change.getInput(), out, change.getEntry());
					it.remove();
				}
				
				if( change.type() == Change.TYPE_DELETE &&
					entry.getName() != null &&
					entry.getName().equals(change.targetFile())) {
					System.out.println("Delete: " + entry.getName());
					copy = false;
					it.remove();
					break;
				} 
			}
			
			if(copy) {
				copyStream(in, out, entry);
			}
		}
	}

	private static void copyStream(InputStream in, ArchiveOutputStream out, ArchiveEntry entry) throws IOException {
		out.putArchiveEntry(entry);
		IOUtils.copy(in, out);
		out.closeArchiveEntry();
		System.out.println("Copy: " + entry.getName());
	}

}
