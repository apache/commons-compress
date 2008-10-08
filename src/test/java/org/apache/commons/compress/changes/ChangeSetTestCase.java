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

import junit.framework.TestCase;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.memory.MemoryArchiveInputStream;

public final class ChangeSetTestCase extends TestCase {

	private void apply( final ChangeSet cs ) throws IOException {
		
		final ArchiveInputStream is = new MemoryArchiveInputStream(new String[][] {
				{ "test1",      "" },
				{ "test2",      "" },
				{ "dir1/test1", "" },
				{ "dir1/test2", "" },
				{ "dir2/test1", "" },
				{ "dir2/test2", "" }
				});
		
		while(true) {
			final ArchiveEntry entry = is.getNextEntry();
			
			if (entry == null) {
				break;
			}
			
			// delete, new name, new content
		}
	}
	
	public void testChangeSet() {	    
	}
}
