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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.compressors.deflate.DeflateCompressorInputStream;
import org.apache.commons.compress.compressors.deflate.DeflateCompressorOutputStream;
import org.apache.commons.compress.compressors.deflate.DeflateParameters;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.Test;

public final class DeflateTestCase extends AbstractTestCase {

    /**
     * Tests the creation of a DEFLATE archive with zlib header
     *
     * @throws Exception
     */
    @Test
    public void testDeflateCreation()  throws Exception {
        final File input = getFile("test1.xml");
        final File output = new File(dir, "test1.xml.deflatez");
        try (OutputStream out = Files.newOutputStream(output.toPath())) {
            try (CompressorOutputStream cos = new CompressorStreamFactory()
                    .createCompressorOutputStream("deflate", out)) {
                IOUtils.copy(Files.newInputStream(input.toPath()), cos);
            }
        }
    }

    /**
     * Tests the creation of a "raw" DEFLATE archive (without zlib header)
     *
     * @throws Exception
     */
    @Test
    public void testRawDeflateCreation()  throws Exception {
        final File input = getFile("test1.xml");
        final File output = new File(dir, "test1.xml.deflate");
        try (OutputStream out = Files.newOutputStream(output.toPath())) {
            final DeflateParameters params = new DeflateParameters();
            params.setWithZlibHeader(false);
            try (CompressorOutputStream cos = new DeflateCompressorOutputStream(out, params)) {
                IOUtils.copy(Files.newInputStream(input.toPath()), cos);
            }
        }
    }

    /**
     * Tests the extraction of a DEFLATE archive with zlib header
     *
     * @throws Exception
     */
    @Test
    public void testDeflateUnarchive() throws Exception {
        final File input = getFile("bla.tar.deflatez");
        final File output = new File(dir, "bla.tar");
        try (InputStream is = Files.newInputStream(input.toPath())) {
             // zlib header is expected by default
            try (CompressorInputStream in = new CompressorStreamFactory()
                    .createCompressorInputStream("deflate", is);
                    OutputStream out = Files.newOutputStream(output.toPath())) {
                IOUtils.copy(in, out);
            }
        }
    }

    /**
     * Tests the extraction of a "raw" DEFLATE archive (without zlib header)
     *
     * @throws Exception
     */
    @Test
    public void testRawDeflateUnarchive() throws Exception {
        final File input = getFile("bla.tar.deflate");
        final File output = new File(dir, "bla.tar");
        try (InputStream is = Files.newInputStream(input.toPath())) {
            final DeflateParameters params = new DeflateParameters();
            params.setWithZlibHeader(false);
            try (CompressorInputStream in = new DeflateCompressorInputStream(is, params);
                    OutputStream out = Files.newOutputStream(output.toPath())) {
                IOUtils.copy(in, out);
            }
        }
    }
}
