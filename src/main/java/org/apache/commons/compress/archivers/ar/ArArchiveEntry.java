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
package org.apache.commons.compress.archivers.ar;

import org.apache.commons.compress.archivers.ArchiveEntry;

/**
 * Represents an archive entry in the "ar" format.
 */
public class ArArchiveEntry implements ArchiveEntry {

	private final String name;
	private final int userId;
	private final int groupId;
	private final int mode;
	private final long lastModified;
	private final long length;

	public ArArchiveEntry(String name, long length) {
		this(name, length, 0, 0, 33188, System.currentTimeMillis());
	}
	
	public ArArchiveEntry(String name, long length, int userId, int groupId, int mode, long lastModified) {
		this.name = name;
		this.length = length;
		this.userId = userId;
		this.groupId = groupId;
		this.mode = mode;
		this.lastModified = lastModified;
	}

	public long getSize() {
		return this.getLength();
	}
	
	public String getName() {
		return name;
	}
	
	public int getUserId() {
		return userId;
	}
	
	public int getGroupId() {
		return groupId;
	}
	
	public int getMode() {
		return mode;
	}
	
	public long getLastModified() {
		return lastModified;
	}
	
	public long getLength() {
		return length;
	}

	public boolean isDirectory() {
		return false;
	}
}
