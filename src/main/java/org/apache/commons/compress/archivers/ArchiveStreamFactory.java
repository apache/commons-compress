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
package org.apache.commons.compress.archivers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.compress.archivers.ar.ArArchiveInputStream;
import org.apache.commons.compress.archivers.ar.ArArchiveOutputStream;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;
import org.apache.commons.compress.archivers.jar.JarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

/**
 * Factory to create Archive[In|Out]putStreams from names
 * or the first bytes of the InputStream. In order add other
 * implementations you should extend ArchiveStreamFactory
 * and override the appropriate methods (and call their implementation
 * from super of course)
 * 
 * TODO add example here 
 * 
 */

public class ArchiveStreamFactory {

    public ArchiveInputStream createArchiveInputStream( final String archiverName, final InputStream in ) throws ArchiveException {
    	if ("ar".equalsIgnoreCase(archiverName)) {
            return new ArArchiveInputStream(in);
        } else if("zip".equalsIgnoreCase(archiverName)) {
        	return new ZipArchiveInputStream(in);
        } else if("tar".equalsIgnoreCase(archiverName)) {
        	return new TarArchiveInputStream(in);
        } else if("jar".equalsIgnoreCase(archiverName)) {
        	return new JarArchiveInputStream(in);
        }
    	return null;
    }

    public ArchiveOutputStream createArchiveOutputStream( final String archiverName, final OutputStream out ) throws ArchiveException {
    	if ("ar".equalsIgnoreCase(archiverName)) {
            return new ArArchiveOutputStream(out);
        } else if("zip".equalsIgnoreCase(archiverName)) {
        	return new ZipArchiveOutputStream(out);
        } else if("tar".equalsIgnoreCase(archiverName)) {
        	return new TarArchiveOutputStream(out);
        } else if("jar".equalsIgnoreCase(archiverName)) {
        	return new JarArchiveOutputStream(out);
        }
    	return null;
    }

    public ArchiveInputStream createArchiveInputStream( final InputStream input ) throws IOException {

		final byte[] signature = new byte[12];
		input.mark(signature.length);
		input.read(signature);
		// TODO if reset is not supported pass on the IOException or return null?
		input.reset();

		if(ZipArchiveInputStream.matches(signature)) {
			return new ZipArchiveInputStream(input);
		} else if(JarArchiveInputStream.matches(signature)) {
			return new JarArchiveInputStream(input);
		} else if(TarArchiveInputStream.matches(signature)) {
			return new TarArchiveInputStream(input);
		} else if(ArArchiveInputStream.matches(signature)) {
			return new ArArchiveInputStream(input);
		} 
		return null;
	}
}
