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

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.Test;

public final class FramedSnappyCompressorInputStreamTest
    extends AbstractTestCase {

    @Test
    public void testMatches() throws IOException {
        assertFalse(FramedSnappyCompressorInputStream.matches(new byte[10], 10));
        final byte[] b = new byte[12];
        final File input = getFile("bla.tar.sz");
        try (FileInputStream in = new FileInputStream(input)) {
            IOUtils.readFully(in, b);
        }
        assertFalse(FramedSnappyCompressorInputStream.matches(b, 9));
        assertTrue(FramedSnappyCompressorInputStream.matches(b, 10));
        assertTrue(FramedSnappyCompressorInputStream.matches(b, 12));
    }

    /**
     * Something big enough to make buffers slide.
     */
    @Test
    public void testLoremIpsum() throws Exception {
        final File outputSz = new File(dir, "lorem-ipsum.1");
        final File outputGz = new File(dir, "lorem-ipsum.2");
        try (FileInputStream isSz = new FileInputStream(getFile("lorem-ipsum.txt.sz"))) {
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
            try (FileInputStream isGz = new FileInputStream(getFile("lorem-ipsum.txt.gz"))) {
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
            }
        }

        try (FileInputStream sz = new FileInputStream(outputSz)) {
            try (FileInputStream gz = new FileInputStream(outputGz)) {
                assertArrayEquals(IOUtils.toByteArray(sz),
                        IOUtils.toByteArray(gz));
            }
        }
    }

    @Test
    public void testRemainingChunkTypes() throws Exception {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (FileInputStream isSz = new FileInputStream(getFile("mixed.txt.sz"))) {
            final FramedSnappyCompressorInputStream in = new FramedSnappyCompressorInputStream(isSz);
            IOUtils.copy(in, out);
            out.close();
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

    @Test
    public void testAvailable() throws Exception {
        try (FileInputStream isSz = new FileInputStream(getFile("mixed.txt.sz"))) {
            final FramedSnappyCompressorInputStream in = new FramedSnappyCompressorInputStream(isSz);
            assertEquals(0, in.available()); // no chunk read so far
            assertEquals('1', in.read());
            assertEquals(3, in.available()); // remainder of first uncompressed block
            assertEquals(3, in.read(new byte[5], 0, 3));
            assertEquals('5', in.read());
            assertEquals(0, in.available()); // end of chunk, must read next one
            assertEquals(4, in.read(new byte[5], 0, 4));
            assertEquals('5', in.read());
            in.close();
        }
    }

    @Test
    public void testUnskippableChunk() {
        final byte[] input = new byte[] {
            (byte) 0xff, 6, 0, 0, 's', 'N', 'a', 'P', 'p', 'Y',
            2, 2, 0, 0, 1, 1
        };
        try {
            final FramedSnappyCompressorInputStream in =
                new FramedSnappyCompressorInputStream(new ByteArrayInputStream(input));
            in.read();
            fail("expected an exception");
            in.close();
        } catch (final IOException ex) {
            assertTrue(ex.getMessage().contains("unskippable chunk"));
        }
    }

    @Test
    public void testChecksumUnmasking() {
        testChecksumUnmasking(0xc757l);
        testChecksumUnmasking(0xffffc757l);
    }

    @Test
    public void readIWAFile() throws Exception {
        try (ZipFile zip = new ZipFile(getFile("testNumbersNew.numbers"))) {
            try (InputStream is = zip.getInputStream(zip.getEntry("Index/Document.iwa"))) {
                final FramedSnappyCompressorInputStream in =
                        new FramedSnappyCompressorInputStream(is, FramedSnappyDialect.IWORK_ARCHIVE);
                FileOutputStream out = null;
                try {
                    out = new FileOutputStream(new File(dir, "snappyIWATest.raw"));
                    IOUtils.copy(in, out);
                } finally {
                    if (out != null) {
                        out.close();
                    }
                    in.close();
                }
            }
        }
    }

    /**
     * @see "https://issues.apache.org/jira/browse/COMPRESS-358"
     */
    @Test
    public void readIWAFileWithBiggerOffset() throws Exception {
        File o = new File(dir, "COMPRESS-358.raw");
        try (InputStream is = new FileInputStream(getFile("COMPRESS-358.iwa"));
             FramedSnappyCompressorInputStream in =
                 new FramedSnappyCompressorInputStream(is, 1<<16, FramedSnappyDialect.IWORK_ARCHIVE);
             FileOutputStream out = new FileOutputStream(o)) {
            IOUtils.copy(in, out);
        }
        try (FileInputStream a = new FileInputStream(o);
             FileInputStream e = new FileInputStream(getFile("COMPRESS-358.uncompressed"))) {
            byte[] expected = IOUtils.toByteArray(e);
            byte[] actual = IOUtils.toByteArray(a);
            assertArrayEquals(expected, actual);
        }
    }

    @Test
    public void singleByteReadConsistentlyReturnsMinusOneAtEof() throws IOException {
        final File input = getFile("bla.tar.sz");
        try (InputStream is = new FileInputStream(input)) {
            final FramedSnappyCompressorInputStream in =
                    new FramedSnappyCompressorInputStream(is);
            IOUtils.toByteArray(in);
            assertEquals(-1, in.read());
            assertEquals(-1, in.read());
            in.close();
        }
    }

    @Test
    public void multiByteReadConsistentlyReturnsMinusOneAtEof() throws IOException {
        final File input = getFile("bla.tar.sz");
        byte[] buf = new byte[2];
        try (InputStream is = new FileInputStream(input)) {
            final FramedSnappyCompressorInputStream in =
                    new FramedSnappyCompressorInputStream(is);
            IOUtils.toByteArray(in);
            assertEquals(-1, in.read(buf));
            assertEquals(-1, in.read(buf));
            in.close();
        }
    }

    private void testChecksumUnmasking(final long x) {
        assertEquals(Long.toHexString(x),
                     Long.toHexString(FramedSnappyCompressorInputStream
                                      .unmask(mask(x))));
    }

    private long mask(final long x) {
        return (((x >>> 15) | (x << 17))
                + FramedSnappyCompressorInputStream.MASK_OFFSET)
             & 0xffffFFFFL;
    }

}
