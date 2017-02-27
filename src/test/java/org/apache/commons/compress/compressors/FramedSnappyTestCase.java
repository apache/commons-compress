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

import static org.junit.Assert.*;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.compressors.snappy.FramedSnappyCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.Test;

public final class FramedSnappyTestCase
    extends AbstractTestCase {

    @Test
    public void testDefaultExtraction() throws Exception {
        testUnarchive(new StreamWrapper<CompressorInputStream>() {
            @Override
            public CompressorInputStream wrap(final InputStream is) throws IOException {
                return new FramedSnappyCompressorInputStream(is);
            }
        });
    }

    @Test
    public void testDefaultExtractionViaFactory() throws Exception {
        testUnarchive(new StreamWrapper<CompressorInputStream>() {
            @Override
            public CompressorInputStream wrap(final InputStream is) throws Exception {
                return new CompressorStreamFactory()
                    .createCompressorInputStream(CompressorStreamFactory.SNAPPY_FRAMED,
                                                 is);
            }
        });
    }

    @Test
    public void testDefaultExtractionViaFactoryAutodetection() throws Exception {
        testUnarchive(new StreamWrapper<CompressorInputStream>() {
            @Override
            public CompressorInputStream wrap(final InputStream is) throws Exception {
                return new CompressorStreamFactory().createCompressorInputStream(is);
            }
        });
    }

    private void testUnarchive(final StreamWrapper<CompressorInputStream> wrapper) throws Exception {
        final File input = getFile("bla.tar.sz");
        final File output = new File(dir, "bla.tar");
        try (FileInputStream is = new FileInputStream(input)) {
            // the intermediate BufferedInputStream is there for mark
            // support in the autodetection test
            final CompressorInputStream in = wrapper.wrap(new BufferedInputStream(is));
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(output);
                IOUtils.copy(in, out);
                assertEquals(995, in.getBytesRead());
            } finally {
                if (out != null) {
                    out.close();
                }
                in.close();
            }
        }
        final File original = getFile("bla.tar");
        try (FileInputStream written = new FileInputStream(output)) {
            try (FileInputStream orig = new FileInputStream(original)) {
                assertArrayEquals(IOUtils.toByteArray(written),
                        IOUtils.toByteArray(orig));
            }
        }
    }

    @Test
    public void testRoundtrip() throws Exception {
        testRoundtrip(getFile("test.txt"));
        testRoundtrip(getFile("bla.tar"));
        testRoundtrip(getFile("COMPRESS-256.7z"));
    }

    @Test
    public void testRoundtripWithOneBigWrite() throws Exception {
        Random r = new Random();
        File input = new File(dir, "bigChunkTest");
        try (FileOutputStream fs = new FileOutputStream(input)) {
            for (int i = 0 ; i < 1 << 17; i++) {
                fs.write(r.nextInt(256));
            }
        }
        long start = System.currentTimeMillis();
        final File outputSz = new File(dir, input.getName() + ".sz");
        try (FileInputStream is = new FileInputStream(input);
             FileOutputStream os = new FileOutputStream(outputSz);
             CompressorOutputStream sos = new CompressorStreamFactory()
                 .createCompressorOutputStream("snappy-framed", os)) {
            byte[] b = IOUtils.toByteArray(is);
            sos.write(b[0]);
            sos.write(b, 1, b.length - 1); // must be split into multiple compressed chunks
        }
        System.err.println(input.getName() + " written, uncompressed bytes: " + input.length()
            + ", compressed bytes: " + outputSz.length() + " after " + (System.currentTimeMillis() - start) + "ms");
        try (FileInputStream is = new FileInputStream(input);
             CompressorInputStream sis = new CompressorStreamFactory()
                 .createCompressorInputStream("snappy-framed", new FileInputStream(outputSz))) {
            byte[] expected = IOUtils.toByteArray(is);
            byte[] actual = IOUtils.toByteArray(sis);
            assertArrayEquals(expected, actual);
        }
    }

    private void testRoundtrip(File input)  throws Exception {
        long start = System.currentTimeMillis();
        final File outputSz = new File(dir, input.getName() + ".sz");
        try (FileInputStream is = new FileInputStream(input);
             FileOutputStream os = new FileOutputStream(outputSz);
             CompressorOutputStream sos = new CompressorStreamFactory()
                 .createCompressorOutputStream("snappy-framed", os)) {
            IOUtils.copy(is, sos);
        }
        System.err.println(input.getName() + " written, uncompressed bytes: " + input.length()
            + ", compressed bytes: " + outputSz.length() + " after " + (System.currentTimeMillis() - start) + "ms");
        try (FileInputStream is = new FileInputStream(input);
             CompressorInputStream sis = new CompressorStreamFactory()
                 .createCompressorInputStream("snappy-framed", new FileInputStream(outputSz))) {
            byte[] expected = IOUtils.toByteArray(is);
            byte[] actual = IOUtils.toByteArray(sis);
            assertArrayEquals(expected, actual);
        }
    }
}
