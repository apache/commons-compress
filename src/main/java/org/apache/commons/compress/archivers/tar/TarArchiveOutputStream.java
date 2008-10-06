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
import java.io.OutputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveOutputStream;

public class TarArchiveOutputStream extends ArchiveOutputStream {

    private TarOutputStream out = null;
    
    public TarArchiveOutputStream(OutputStream out) {
        this.out = new TarOutputStream(out);
    }
    
    public void close() throws IOException {
        this.out.close();
    }

    public void closeArchiveEntry() throws IOException {
        this.out.closeEntry();
    }

    public void putArchiveEntry(ArchiveEntry entry) throws IOException {
        this.out.putNextEntry((TarArchiveEntry)entry);
    }

    public void write(byte[] buffer, int offset, int length) throws IOException {
        this.out.write(buffer, offset, length);
    }

    public String getDefaultFileExtension() {
        return "tar";
    }

    public byte[] getHeader() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getName() {
        return "tar";
    }

    public void write(int b) throws IOException {
        this.out.write(b);
    }
}

