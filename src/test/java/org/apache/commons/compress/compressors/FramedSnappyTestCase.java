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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Random;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.compressors.snappy.FramedSnappyCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.Test;

public final class FramedSnappyTestCase
    extends AbstractTestCase {

    @Test
    public void testDefaultExtraction() throws Exception {
        testUnarchive(FramedSnappyCompressorInputStream::new);
    }

    @Test
    public void testDefaultExtractionViaFactory() throws Exception {
        testUnarchive(is -> new CompressorStreamFactory()
            .createCompressorInputStream(CompressorStreamFactory.SNAPPY_FRAMED,
                                         is));
    }

    @Test
    public void testDefaultExtractionViaFactoryAutodetection() throws Exception {
        testUnarchive(is -> new CompressorStreamFactory().createCompressorInputStream(is));
    }

    @Test
    public void testRoundtrip() throws Exception {
        testRoundtrip(getFile("test.txt"));
        testRoundtrip(getFile("bla.tar"));
        testRoundtrip(getFile("COMPRESS-256.7z"));
    }

    private void testRoundtrip(final File input) throws Exception {
        final long start = System.currentTimeMillis();
        final File outputSz = new File(dir, input.getName() + ".sz");
        try (OutputStream os = Files.newOutputStream(outputSz.toPath());
                CompressorOutputStream sos = new CompressorStreamFactory().createCompressorOutputStream("snappy-framed", os)) {
            Files.copy(input.toPath(), sos);
        }
        // System.err.println(input.getName() + " written, uncompressed bytes: " + input.length()
        // + ", compressed bytes: " + outputSz.length() + " after " + (System.currentTimeMillis() - start) + "ms");
        try (InputStream is = Files.newInputStream(input.toPath());
                CompressorInputStream sis = new CompressorStreamFactory().createCompressorInputStream("snappy-framed",
                        Files.newInputStream(outputSz.toPath()))) {
            final byte[] expected = IOUtils.toByteArray(is);
            final byte[] actual = IOUtils.toByteArray(sis);
            assertArrayEquals(expected, actual);
        }
    }

    @Test
    public void testRoundtripWithOneBigWrite() throws Exception {
        final Random r = new Random();
        final File input = new File(dir, "bigChunkTest");
        try (OutputStream fs = Files.newOutputStream(input.toPath())) {
            for (int i = 0 ; i < 1 << 17; i++) {
                fs.write(r.nextInt(256));
            }
        }
        final long start = System.currentTimeMillis();
        final File outputSz = new File(dir, input.getName() + ".sz");
        try (InputStream is = Files.newInputStream(input.toPath());
             OutputStream os = Files.newOutputStream(outputSz.toPath());
             CompressorOutputStream sos = new CompressorStreamFactory()
                 .createCompressorOutputStream("snappy-framed", os)) {
            final byte[] b = IOUtils.toByteArray(is);
            sos.write(b[0]);
            sos.write(b, 1, b.length - 1); // must be split into multiple compressed chunks
        }
        // System.err.println(input.getName() + " written, uncompressed bytes: " + input.length()
        //    + ", compressed bytes: " + outputSz.length() + " after " + (System.currentTimeMillis() - start) + "ms");
        try (InputStream is = Files.newInputStream(input.toPath());
             CompressorInputStream sis = new CompressorStreamFactory()
                 .createCompressorInputStream("snappy-framed", Files.newInputStream(outputSz.toPath()))) {
            final byte[] expected = IOUtils.toByteArray(is);
            final byte[] actual = IOUtils.toByteArray(sis);
            assertArrayEquals(expected, actual);
        }
    }

    private void testUnarchive(final StreamWrapper<CompressorInputStream> wrapper) throws Exception {
        final File input = getFile("bla.tar.sz");
        final File output = new File(dir, "bla.tar");
        try (InputStream is = Files.newInputStream(input.toPath())) {
            // the intermediate BufferedInputStream is there for mark
            // support in the autodetection test
            try (CompressorInputStream in = wrapper.wrap(new BufferedInputStream(is))) {
                Files.copy(in, output.toPath());
                assertEquals(995, in.getBytesRead());
            }
        }
        final File original = getFile("bla.tar");
        try (InputStream written = Files.newInputStream(output.toPath())) {
            try (InputStream orig = Files.newInputStream(original.toPath())) {
                assertArrayEquals(IOUtils.toByteArray(written),
                        IOUtils.toByteArray(orig));
            }
        }
    }
}
