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

    public ZipArchiveEntry getNextZipEntry() throws IOException {
        java.util.zip.ZipEntry entry = input.getNextEntry();
        if(entry == null) {
            return null;
        }
        return new ZipArchiveEntry(entry);
    }

    public ArchiveEntry getNextEntry() throws IOException {
        return getNextZipEntry();
    }

    public int read(byte[] b, int off, int len) throws IOException {
        return input.read(b, off, len);
    }

    public int read() throws IOException {
        return input.read();
    }


    public static boolean matches(byte[] signature, int length) {
        if (length < ZipArchiveOutputStream.LFH_SIG.length) {
            return false;
        }

        for (int i = 0; i < ZipArchiveOutputStream.LFH_SIG.length; i++) {
            if (signature[i] != ZipArchiveOutputStream.LFH_SIG[i]) {
                return false;
            }
        }

        return true;
    }
}
