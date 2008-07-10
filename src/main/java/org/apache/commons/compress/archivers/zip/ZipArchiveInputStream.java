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
package org.apache.commons.compress.archivers.zip;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;

public class ZipArchiveInputStream extends ArchiveInputStream {

	private final ZipInputStream input;

	public ZipArchiveInputStream(InputStream inputStream) {
		input = new ZipInputStream(inputStream);
	}

    public ArchiveEntry getNextEntry() throws IOException {
    	java.util.zip.ZipEntry entry = input.getNextEntry();
    	if(entry == null) {
    		return null;
    	}
        return (ArchiveEntry)new ZipArchiveEntry(entry);
    }

    public int read(byte[] b, int off, int len) throws IOException {
        return input.read(b, off, len);
    }
    
    public int read() throws IOException {
        return input.read();
    }

    
    public static boolean matches( byte[] signature ) {
    	// 4b50 0403 0014 0000

    	if (signature[0] != 0x50) {
    		return false;
    	}
    	if (signature[1] != 0x4b) {
    		return false;
    	}
    	if (signature[2] != 0x03) {
    		return false;
    	}
    	if (signature[3] != 0x04) {
    		return false;
    	}
    	if (signature[4] != 0x14) {
    		return false;
    	}
    	if (signature[5] != 0x00) {
    		return false;
    	}
    	if (signature[6] != 0x00) {
    		return false;
    	}
    	if (signature[7] != 0x00) {
    		return false;
    	}
    	
    	return true;
    }
}
