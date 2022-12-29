/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.compress.compressors.zstandard;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.Test;

public class ZstdRoundtripTest extends AbstractTestCase {

    private interface OutputStreamCreator {
        ZstdCompressorOutputStream wrap(FileOutputStream os) throws IOException;
    }

    @Test
    public void directRoundtrip() throws Exception {
        roundtrip(ZstdCompressorOutputStream::new);
    }

    @Test
    public void factoryRoundtrip() throws Exception {
        final File input = getFile("bla.tar");
        long start = System.currentTimeMillis();
        final File output = new File(dir, input.getName() + ".zstd");
        try (OutputStream os = Files.newOutputStream(output.toPath());
                CompressorOutputStream zos = new CompressorStreamFactory().createCompressorOutputStream("zstd", os)) {
            Files.copy(input.toPath(), zos);
        }
        start = System.currentTimeMillis();
        try (final InputStream inputStream = Files.newInputStream(output.toPath());
                CompressorInputStream zis = new CompressorStreamFactory().createCompressorInputStream("zstd", inputStream)) {
            final byte[] expected = Files.readAllBytes(input.toPath());
            final byte[] actual = IOUtils.toByteArray(zis);
            assertArrayEquals(expected, actual);
        }
    }

    private void roundtrip(final OutputStreamCreator oc) throws IOException {
        final File input = getFile("bla.tar");
        long start = System.currentTimeMillis();
        final File output = new File(dir, input.getName() + ".zstd");
        try (FileOutputStream os = new FileOutputStream(output);
             ZstdCompressorOutputStream zos = oc.wrap(os)) {
            Files.copy(input.toPath(), zos);
        }
        //System.err.println(input.getName() + " written, uncompressed bytes: " + input.length()
        //    + ", compressed bytes: " + output.length() + " after " + (System.currentTimeMillis() - start) + "ms");
        start = System.currentTimeMillis();
        try (InputStream is = Files.newInputStream(input.toPath());
             ZstdCompressorInputStream zis = new ZstdCompressorInputStream(Files.newInputStream(output.toPath()))) {
            final byte[] expected = IOUtils.toByteArray(is);
            final byte[] actual = IOUtils.toByteArray(zis);
            assertArrayEquals(expected, actual);
        }
        // System.err.println(output.getName() + " read after " + (System.currentTimeMillis() - start) + "ms");
    }

    @Test
    public void roundtripWithChecksum() throws Exception {
        roundtrip(os -> new ZstdCompressorOutputStream(os, 3, false, true));
    }

    @Test
    public void roundtripWithCloseFrameOnFlush() throws Exception {
        roundtrip(os -> new ZstdCompressorOutputStream(os, 3, true));
    }

    @Test
    public void roundtripWithCustomLevel() throws Exception {
        roundtrip(os -> new ZstdCompressorOutputStream(os, 1));
    }

}
