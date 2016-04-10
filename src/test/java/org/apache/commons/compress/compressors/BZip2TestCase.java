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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.Test;

public final class BZip2TestCase extends AbstractTestCase {

    @Test
    public void testBzipCreation()  throws Exception {
        File output = null;
        final File input = getFile("test.txt");
        {
            output = new File(dir, "test.txt.bz2");
            final OutputStream out = new FileOutputStream(output);
            final CompressorOutputStream cos = new CompressorStreamFactory().createCompressorOutputStream("bzip2", out);
            final FileInputStream in = new FileInputStream(input);
            IOUtils.copy(in, cos);
            cos.close();
            in.close();
        }

        final File decompressed = new File(dir, "decompressed.txt");
        {
            final File toDecompress = output;
            final InputStream is = new FileInputStream(toDecompress);
            final CompressorInputStream in =
                new CompressorStreamFactory().createCompressorInputStream("bzip2", is);
            final FileOutputStream os = new FileOutputStream(decompressed);
            IOUtils.copy(in, os);
            is.close();
            os.close();
        }

        assertEquals(input.length(),decompressed.length());
    }

    @Test
    public void testBzip2Unarchive() throws Exception {
        final File input = getFile("bla.txt.bz2");
        final File output = new File(dir, "bla.txt");
        final InputStream is = new FileInputStream(input);
        final CompressorInputStream in = new CompressorStreamFactory().createCompressorInputStream("bzip2", is);
        final FileOutputStream os = new FileOutputStream(output);
        IOUtils.copy(in, os);
        is.close();
        os.close();
    }

    @Test
    public void testConcatenatedStreamsReadFirstOnly() throws Exception {
        final File input = getFile("multiple.bz2");
        final InputStream is = new FileInputStream(input);
        try {
            final CompressorInputStream in = new CompressorStreamFactory()
                .createCompressorInputStream("bzip2", is);
            try {
                assertEquals('a', in.read());
                assertEquals(-1, in.read());
            } finally {
                in.close();
            }
        } finally {
            is.close();
        }
    }

    @Test
    public void testConcatenatedStreamsReadFully() throws Exception {
        final File input = getFile("multiple.bz2");
        final InputStream is = new FileInputStream(input);
        try {
            final CompressorInputStream in =
                new BZip2CompressorInputStream(is, true);
            try {
                assertEquals('a', in.read());
                assertEquals('b', in.read());
                assertEquals(0, in.available());
                assertEquals(-1, in.read());
            } finally {
                in.close();
            }
        } finally {
            is.close();
        }
    }

    @Test
    public void testCOMPRESS131() throws Exception {
        final File input = getFile("COMPRESS-131.bz2");
        final InputStream is = new FileInputStream(input);
        try {
            final CompressorInputStream in =
                new BZip2CompressorInputStream(is, true);
            try {
                int l = 0;
                while(in.read() != -1) {
                    l++;
                }
                assertEquals(539, l);
            } finally {
                in.close();
            }
        } finally {
            is.close();
        }
    }

    @Test
    public void testCOMPRESS207Listeners() throws Exception {
        File inputFile = getFile("COMPRESS-207.bz2");
        FileInputStream fInputStream = new FileInputStream(inputFile);
        final List<Integer> blockNumbers = new ArrayList<Integer>();
        final List<Long> uncompressedBytes = new ArrayList<Long>();
        final List<Long> compressedBytes = new ArrayList<Long>();
        final BZip2CompressorInputStream in = new BZip2CompressorInputStream(fInputStream);

        CompressionProgressListener blockListener = new CompressionProgressListener() {

            public void notify(CompressionProgressEvent e) {
                assertSame(in, e.getSource());
                blockNumbers.add(e.getBlockNumber());
                uncompressedBytes.add(e.getUncompressedBytesProcessed());
                compressedBytes.add(e.getCompressedBytesProcessed());
            }
        };
        in.addCompressionProgressListener(blockListener);

        while(in.read() >= 0);
        in.close();

        assertEquals(5, blockNumbers.size());
        for (int i = 0; i < 5; i++) {
            assertEquals(i, blockNumbers.get(i).intValue());
        }

        assertEquals(5, uncompressedBytes.size());
        assertEquals(Long.valueOf(0), uncompressedBytes.get(0));
        assertEquals(Long.valueOf(899907), uncompressedBytes.get(1));
        assertEquals(Long.valueOf(1799817), uncompressedBytes.get(2));
        assertEquals(Long.valueOf(2699710), uncompressedBytes.get(3));
        assertEquals(Long.valueOf(3599604), uncompressedBytes.get(4));

        assertEquals(5, compressedBytes.size());
        // 4 == number of magic bytes + blocksize
        assertEquals(Long.valueOf(4), compressedBytes.get(0));
        assertEquals(Long.valueOf(457766), compressedBytes.get(1));
        assertEquals(Long.valueOf(915639), compressedBytes.get(2));
        assertEquals(Long.valueOf(1373360), compressedBytes.get(3));
        assertEquals(Long.valueOf(1831305), compressedBytes.get(4));
    }

}
