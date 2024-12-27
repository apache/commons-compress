/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
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

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

public class FactoryTest extends AbstractTest {

    private void roundtripViaFactory(final String format) throws Exception {
        final Path input = getPath("bla.tar");
        final Path outputSz = getTempDirFile().toPath().resolve(input.getFileName() + "." + format + ".lz4");
        try (OutputStream os = Files.newOutputStream(outputSz);
                CompressorOutputStream<?> los = new CompressorStreamFactory().createCompressorOutputStream(format, os)) {
            los.write(input);
        }
        try (InputStream is = Files.newInputStream(input);
                InputStream sis = new CompressorStreamFactory().createCompressorInputStream(format, Files.newInputStream(outputSz))) {
            final byte[] expected = IOUtils.toByteArray(is);
            final byte[] actual = IOUtils.toByteArray(sis);
            assertArrayEquals(expected, actual);
        }
    }

    @Test
    public void testBlockRoundtripViaFactory() throws Exception {
        roundtripViaFactory(CompressorStreamFactory.getLZ4Block());
    }

    @Test
    public void testFrameRoundtripViaFactory() throws Exception {
        roundtripViaFactory(CompressorStreamFactory.getLZ4Framed());
    }
}
