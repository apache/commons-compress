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
package org.apache.commons.compress.compressors.snappy;

import static org.junit.Assert.assertArrayEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;

public final class FramedSnappyCompressorInputStreamTest
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

    /**
     * Something big enough to make buffers slide.
     */
    public void testLoremIpsum() throws Exception {
        final FileInputStream isSz = new FileInputStream(getFile("lorem-ipsum.txt.sz"));
        final File outputSz = new File(dir, "lorem-ipsum.1");
        final File outputGz = new File(dir, "lorem-ipsum.2");
        try {
            InputStream in = new FramedSnappyCompressorInputStream(isSz);
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
            FramedSnappyCompressorInputStream in = new FramedSnappyCompressorInputStream(isSz);
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
            FramedSnappyCompressorInputStream in = new FramedSnappyCompressorInputStream(isSz);
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

    public void testUnskippableChunk() {
        byte[] input = new byte[] {
            (byte) 0xff, 6, 0, 0, 's', 'N', 'a', 'P', 'p', 'Y',
            2, 2, 0, 0, 1, 1
        };
        try {
            FramedSnappyCompressorInputStream in =
                new FramedSnappyCompressorInputStream(new ByteArrayInputStream(input));
            in.read();
            fail("expected an exception");
        } catch (IOException ex) {
            assertTrue(ex.getMessage().indexOf("unskippable chunk") > -1);
        }
    }

    public void testChecksumUnmasking() {
        testChecksumUnmasking(0xc757l);
        testChecksumUnmasking(0xffffc757l);
    }

    public void testChecksumUnmasking(long x) {
        assertEquals(Long.toHexString(x),
                     Long.toHexString(FramedSnappyCompressorInputStream
                                      .unmask(mask(x))));
    }

    private long mask(long x) {
        return (((x >>> 15) | (x << 17))
                + FramedSnappyCompressorInputStream.MASK_OFFSET)
             & 0xffffFFFFL;
    }

}
