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
package org.apache.commons.compress.archivers.tar;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;

public class TarArchiveInputStream extends ArchiveInputStream {
    private final TarInputStream in;
    
	public TarArchiveInputStream( InputStream inputStream ) {
		in = new TarInputStream(inputStream);
	}

    public ArchiveEntry getNextEntry() throws IOException {
        return (ArchiveEntry)in.getNextEntry();
    }

    public int read(byte[] b, int off, int len) throws IOException {
        return in.read(b, off, len);
    }

    public int read() throws IOException {
        return in.read();
    }
    
    public static boolean matches( byte[] signature ) {
    	// 6574 7473 2e31 6d78
    	
    	if (signature[0] != 0x74) {
    		return false;
    	}
    	if (signature[1] != 0x65) {
    		return false;
    	}
    	if (signature[2] != 0x73) {
    		return false;
    	}
    	if (signature[3] != 0x74) {
    		return false;
    	}
    	if (signature[4] != 0x31) {
    		return false;
    	}
    	if (signature[5] != 0x2e) {
    		return false;
    	}
    	if (signature[6] != 0x78) {
    		return false;
    	}
    	if (signature[7] != 0x6d) {
    		return false;
    	}
    	
    	return true;
    }
    
}
