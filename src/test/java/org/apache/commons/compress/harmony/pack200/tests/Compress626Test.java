/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.commons.compress.harmony.pack200.tests;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.jar.JarOutputStream;

import org.apache.commons.compress.harmony.pack200.AttributeDefinitionBands;
import org.apache.commons.compress.harmony.pack200.CPUTF8;
import org.apache.commons.compress.harmony.pack200.NewAttributeBands;
import org.apache.commons.compress.java.util.jar.Pack200;
import org.apache.commons.io.output.NullOutputStream;
import org.junit.jupiter.api.Test;

public class Compress626Test {

	@Test
	public void test() throws Exception {
		final CPUTF8 name = new CPUTF8("");
		final CPUTF8 layout = new CPUTF8("[");
		assertDoesNotThrow(
				() -> new NewAttributeBands(1, null, null, new AttributeDefinitionBands.AttributeDefinition(35,
						AttributeDefinitionBands.CONTEXT_CLASS, name, layout)));
	}

	@Test
	public void testJar() throws IOException {
		try (InputStream inputStream = Files.newInputStream(
				Paths.get("src/test/resources/org/apache/commons/compress/COMPRESS-626/compress-626-pack200.jar"));
				JarOutputStream out = new JarOutputStream(NullOutputStream.NULL_OUTPUT_STREAM);) {
			Pack200.newUnpacker().unpack(inputStream, out);
		}
	}
}
