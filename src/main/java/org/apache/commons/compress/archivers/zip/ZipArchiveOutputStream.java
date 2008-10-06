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
import java.io.OutputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveOutputStream;

public class ZipArchiveOutputStream extends ArchiveOutputStream {

    private ZipOutputStream zipOut = null;
 
    public ZipArchiveOutputStream(OutputStream out) {
        this.zipOut = new ZipOutputStream(out);
    }
    
    public void putArchiveEntry(ArchiveEntry entry) throws IOException {
        zipOut.putNextEntry((ZipArchiveEntry) entry);
    }

    public String getDefaultFileExtension() {
        return "zip";
    }

    public byte[] getHeader() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getName() {
        return "zip";
    }

    public void close() throws IOException {
        zipOut.close();
    }

    public void write(byte[] buffer, int offset, int length) throws IOException {
        zipOut.write(buffer, offset, length);
    }

    public void closeArchiveEntry() {
        // do nothing
    }

	public void write(int arg0) throws IOException {
		this.zipOut.write(arg0);
	}
 
}
