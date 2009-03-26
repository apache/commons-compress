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
package org.apache.commons.compress.compressors;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.utils.IOUtils;

public final class GZipTestCase extends AbstractTestCase {

	public void testGzipCreation()  throws Exception {
		final File input = getFile("test1.xml");
		final File output = new File(dir, "test1.xml.gz");
		final OutputStream out = new FileOutputStream(output);
		final CompressorOutputStream cos = new CompressorStreamFactory().createCompressorOutputStream("gz", out);
		IOUtils.copy(new FileInputStream(input), cos);
		cos.close();
	}
	
	public void testGzipUnarchive() throws Exception {
		final File input = getFile("bla.tgz");
		final File output = new File(dir, "bla.tar");
        final InputStream is = new FileInputStream(input);
        final CompressorInputStream in = new CompressorStreamFactory().createCompressorInputStream("gz", is);
        FileOutputStream out = new FileOutputStream(output);
        IOUtils.copy(in, out);
		in.close();
		is.close();
		out.close();
    }

}
