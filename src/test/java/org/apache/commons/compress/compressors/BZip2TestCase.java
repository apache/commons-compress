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
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;

public final class BZip2TestCase extends AbstractTestCase {
	public void testBzipCreation()  throws Exception {
		final File output = new File(dir, "bla.txt.bz2");
		System.out.println(dir);
		final File file1 = new File(getClass().getClassLoader().getResource("test.txt").getFile());
		final OutputStream out = new FileOutputStream(output);
		CompressorOutputStream cos = new CompressorStreamFactory().createCompressorOutputStream("bzip2", out);
		IOUtils.copy(new FileInputStream(file1), cos);
		cos.close();
	}
	
	public void testBzip2Unarchive() throws Exception {
		final File output = new File(dir, "test-entpackt.txt");
		System.out.println(dir);
		final File input = new File(getClass().getClassLoader().getResource("bla.txt.bz2").getFile());
        final InputStream is = new FileInputStream(input);
        //final CompressorInputStream in = new CompressorStreamFactory().createCompressorInputStream("bzip2", is);
        final CompressorInputStream in = new BZip2CompressorInputStream(is);
        IOUtils.copy(in, new FileOutputStream(output));
		in.close();
    }

}
