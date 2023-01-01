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
package org.apache.commons.compress.compressors.lz4;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.Test;

public class FactoryTest extends AbstractTestCase {

    @Test
    public void blockRoundtripViaFactory() throws Exception {
        roundtripViaFactory(CompressorStreamFactory.getLZ4Block());
    }

    @Test
    public void frameRoundtripViaFactory() throws Exception {
        roundtripViaFactory(CompressorStreamFactory.getLZ4Framed());
    }

    private void roundtripViaFactory(final String format) throws Exception {
        final File input = getFile("bla.tar");
        long start = System.currentTimeMillis();
        final File outputSz = new File(dir, input.getName() + "." + format + ".lz4");
        try (OutputStream os = Files.newOutputStream(outputSz.toPath());
                OutputStream los = new CompressorStreamFactory().createCompressorOutputStream(format, os)) {
            Files.copy(input.toPath(), los);
        }
        // System.err.println(input.getName() + " written, uncompressed bytes: " + input.length()
        // + ", compressed bytes: " + outputSz.length() + " after " + (System.currentTimeMillis() - start) + "ms");
        start = System.currentTimeMillis();
        try (InputStream is = Files.newInputStream(input.toPath());
                InputStream sis = new CompressorStreamFactory().createCompressorInputStream(format, Files.newInputStream(outputSz.toPath()))) {
            final byte[] expected = IOUtils.toByteArray(is);
            final byte[] actual = IOUtils.toByteArray(sis);
            assertArrayEquals(expected, actual);
        }
        // System.err.println(outputSz.getName() + " read after " + (System.currentTimeMillis() - start) + "ms");
    }
}
