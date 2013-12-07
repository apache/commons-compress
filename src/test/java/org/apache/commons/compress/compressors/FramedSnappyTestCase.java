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

import static org.junit.Assert.assertArrayEquals;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.snappy.FramedSnappyCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;

public final class FramedSnappyTestCase
    extends AbstractTestCase {

    public void testMatches() throws IOException {
        assertFalse(FramedSnappyCompressorInputStream.matches(new byte[10], 10));
        byte[] b = new byte[12];
        final File input = getFile("bla.tar.sz");
        FileInputStream in = new FileInputStream(input);
        try {
            IOUtils.readFully(in, b);
        } finally {
            in.close();
        }
        assertFalse(FramedSnappyCompressorInputStream.matches(b, 9));
        assertTrue(FramedSnappyCompressorInputStream.matches(b, 10));
        assertTrue(FramedSnappyCompressorInputStream.matches(b, 12));
    }

    public void testDefaultExtraction() throws Exception {
        testUnarchive(new StreamWrapper<CompressorInputStream>() {
            public CompressorInputStream wrap(InputStream is) throws IOException {
                return new FramedSnappyCompressorInputStream(is);
            }
        });
    }

    public void testDefaultExtractionViaFactory() throws Exception {
        testUnarchive(new StreamWrapper<CompressorInputStream>() {
            public CompressorInputStream wrap(InputStream is) throws Exception {
                return new CompressorStreamFactory()
                    .createCompressorInputStream(CompressorStreamFactory.SNAPPY_FRAMED,
                                                 is);
            }
        });
    }

    public void testDefaultExtractionViaFactoryAutodetection() throws Exception {
        testUnarchive(new StreamWrapper<CompressorInputStream>() {
            public CompressorInputStream wrap(InputStream is) throws Exception {
                return new CompressorStreamFactory().createCompressorInputStream(is);
            }
        });
    }

    /**
     * Something big enough to make buffers slide.
     */
    public void testLoremIpsum() throws Exception {
        final FileInputStream isSz = new FileInputStream(getFile("lorem-ipsum.txt.sz"));
        final File outputSz = new File(dir, "lorem-ipsum.1");
        final File outputGz = new File(dir, "lorem-ipsum.2");
        try {
            CompressorInputStream in = new FramedSnappyCompressorInputStream(isSz);
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(outputSz);
                IOUtils.copy(in, out);
            } finally {
                if (out != null) {
                    out.close();
                }
                in.close();
            }
            final FileInputStream isGz = new FileInputStream(getFile("lorem-ipsum.txt.gz"));
            try {
                in = new GzipCompressorInputStream(isGz);
                try {
                    out = new FileOutputStream(outputGz);
                    IOUtils.copy(in, out);
                } finally {
                    if (out != null) {
                        out.close();
                    }
                    in.close();
                }
            } finally {
                isGz.close();
            }
        } finally {
            isSz.close();
        }

        final FileInputStream sz = new FileInputStream(outputSz);
        try {
            FileInputStream gz = new FileInputStream(outputGz);
            try {
                assertArrayEquals(IOUtils.toByteArray(sz),
                                  IOUtils.toByteArray(gz));
            } finally {
                gz.close();
            }
        } finally {
            sz.close();
        }
    }

    public void testRemainingChunkTypes() throws Exception {
        final FileInputStream isSz = new FileInputStream(getFile("mixed.txt.sz"));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            CompressorInputStream in = new FramedSnappyCompressorInputStream(isSz);
            IOUtils.copy(in, out);
            out.close();
        } finally {
            isSz.close();
        }

        assertArrayEquals(new byte[] { '1', '2', '3', '4',
                                       '5', '6', '7', '8', '9',
                                       '5', '6', '7', '8', '9',
                                       '5', '6', '7', '8', '9',
                                       '5', '6', '7', '8', '9',
                                       '5', '6', '7', '8', '9', 10,
                                       '1', '2', '3', '4',
                                       '1', '2', '3', '4',
            }, out.toByteArray());
    }

    public void testAvailable() throws Exception {
        final FileInputStream isSz = new FileInputStream(getFile("mixed.txt.sz"));
        try {
            CompressorInputStream in = new FramedSnappyCompressorInputStream(isSz);
            assertEquals(0, in.available()); // no chunk read so far
            assertEquals('1', in.read());
            assertEquals(3, in.available()); // remainder of first uncompressed block
            assertEquals(3, in.read(new byte[5], 0, 3));
            assertEquals('5', in.read());
            assertEquals(4, in.available()); // remainder of literal
            assertEquals(4, in.read(new byte[5], 0, 4));
            assertEquals('5', in.read());
            assertEquals(19, in.available()); // remainder of copy
            in.close();
        } finally {
            isSz.close();
        }
    }

    private void testUnarchive(StreamWrapper<CompressorInputStream> wrapper) throws Exception {
        final File input = getFile("bla.tar.sz");
        final File output = new File(dir, "bla.tar");
        final FileInputStream is = new FileInputStream(input);
        try {
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
        } finally {
            is.close();
        }
        final File original = getFile("bla.tar");
        final FileInputStream written = new FileInputStream(output);
        try {
            FileInputStream orig = new FileInputStream(original);
            try {
                assertArrayEquals(IOUtils.toByteArray(written),
                                  IOUtils.toByteArray(orig));
            } finally {
                orig.close();
            }
        } finally {
            written.close();
        }
    }
}
