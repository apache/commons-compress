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

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.compressors.snappy.FramedSnappyCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.Test;

public final class FramedSnappyTestCase
    extends AbstractTestCase {

    @Test
    public void testDefaultExtraction() throws Exception {
        testUnarchive(new StreamWrapper<CompressorInputStream>() {
            public CompressorInputStream wrap(InputStream is) throws IOException {
                return new FramedSnappyCompressorInputStream(is);
            }
        });
    }

    @Test
    public void testDefaultExtractionViaFactory() throws Exception {
        testUnarchive(new StreamWrapper<CompressorInputStream>() {
            public CompressorInputStream wrap(InputStream is) throws Exception {
                return new CompressorStreamFactory()
                    .createCompressorInputStream(CompressorStreamFactory.SNAPPY_FRAMED,
                                                 is);
            }
        });
    }

    @Test
    public void testDefaultExtractionViaFactoryAutodetection() throws Exception {
        testUnarchive(new StreamWrapper<CompressorInputStream>() {
            public CompressorInputStream wrap(InputStream is) throws Exception {
                return new CompressorStreamFactory().createCompressorInputStream(is);
            }
        });
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
