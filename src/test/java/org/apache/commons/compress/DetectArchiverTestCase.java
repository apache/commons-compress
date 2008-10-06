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
package org.apache.commons.compress;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

import junit.framework.TestCase;

import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;

public final class DetectArchiverTestCase extends TestCase {
	public void testDetection() throws Exception {
		final ArchiveStreamFactory factory = new ArchiveStreamFactory();

		final ArchiveInputStream ar = factory.createArchiveInputStream(
				new BufferedInputStream(new FileInputStream(
						new File(getClass().getClassLoader().getResource("bla.ar").getFile())))); 
		assertTrue(ar instanceof ArArchiveInputStream);

		final ArchiveInputStream tar = factory.createArchiveInputStream(
				new BufferedInputStream(new FileInputStream(
						new File(getClass().getClassLoader().getResource("bla.tar").getFile()))));
		assertTrue(tar instanceof TarArchiveInputStream);

		final ArchiveInputStream zip = factory.createArchiveInputStream(
				new BufferedInputStream(new FileInputStream(
						new File(getClass().getClassLoader().getResource("bla.zip").getFile()))));
		assertTrue(zip instanceof ZipArchiveInputStream);

		final ArchiveInputStream jar = factory.createArchiveInputStream(
				new BufferedInputStream(new FileInputStream(
						new File(getClass().getClassLoader().getResource("bla.jar").getFile()))));
		assertTrue(jar instanceof JarArchiveInputStream);

//		final ArchiveInputStream tgz = factory.createArchiveInputStream(
//				new BufferedInputStream(new FileInputStream(
//						new File(getClass().getClassLoader().getResource("bla.tgz").getFile()))));
//		assertTrue(tgz instanceof TarArchiveInputStream);
		
	}

}
