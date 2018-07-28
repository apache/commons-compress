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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.Assert;
import org.junit.Test;

public class ZstdRoundtripTest extends AbstractTestCase {

    private interface OutputStreamCreator {
        ZstdCompressorOutputStream wrap(FileOutputStream os) throws IOException;
    }

    @Test
    public void directRoundtrip() throws Exception {
        roundtrip(new OutputStreamCreator() {
            @Override
            public ZstdCompressorOutputStream wrap(FileOutputStream os) throws IOException {
                return new ZstdCompressorOutputStream(os);
            }
        });
    }

    private void roundtrip(OutputStreamCreator oc) throws IOException {
        File input = getFile("bla.tar");
        long start = System.currentTimeMillis();
        final File output = new File(dir, input.getName() + ".zstd");
        try (FileInputStream is = new FileInputStream(input);
             FileOutputStream os = new FileOutputStream(output);
             ZstdCompressorOutputStream zos = oc.wrap(os)) {
            IOUtils.copy(is, zos);
        }
        System.err.println(input.getName() + " written, uncompressed bytes: " + input.length()
            + ", compressed bytes: " + output.length() + " after " + (System.currentTimeMillis() - start) + "ms");
        start = System.currentTimeMillis();
        try (FileInputStream is = new FileInputStream(input);
             ZstdCompressorInputStream zis = new ZstdCompressorInputStream(new FileInputStream(output))) {
            byte[] expected = IOUtils.toByteArray(is);
            byte[] actual = IOUtils.toByteArray(zis);
            Assert.assertArrayEquals(expected, actual);
        }
        System.err.println(output.getName() + " read after " + (System.currentTimeMillis() - start) + "ms");
    }

    @Test
    public void factoryRoundtrip() throws Exception {
        File input = getFile("bla.tar");
        long start = System.currentTimeMillis();
        final File output = new File(dir, input.getName() + ".zstd");
        try (FileInputStream is = new FileInputStream(input);
             FileOutputStream os = new FileOutputStream(output);
             CompressorOutputStream zos = new CompressorStreamFactory().createCompressorOutputStream("zstd", os)) {
            IOUtils.copy(is, zos);
        }
        start = System.currentTimeMillis();
        try (FileInputStream is = new FileInputStream(input);
             CompressorInputStream zis = new CompressorStreamFactory()
             .createCompressorInputStream("zstd", new FileInputStream(output))) {
            byte[] expected = IOUtils.toByteArray(is);
            byte[] actual = IOUtils.toByteArray(zis);
            Assert.assertArrayEquals(expected, actual);
        }
    }

    @Test
    public void roundtripWithCustomLevel() throws Exception {
        roundtrip(new OutputStreamCreator() {
            @Override
            public ZstdCompressorOutputStream wrap(FileOutputStream os) throws IOException {
                return new ZstdCompressorOutputStream(os, 1);
            }
        });
    }

    @Test
    public void roundtripWithCloseFrameOnFlush() throws Exception {
        roundtrip(new OutputStreamCreator() {
            @Override
            public ZstdCompressorOutputStream wrap(FileOutputStream os) throws IOException {
                return new ZstdCompressorOutputStream(os, 3, true);
            }
        });
    }

    @Test
    public void roundtripWithChecksum() throws Exception {
        roundtrip(new OutputStreamCreator() {
            @Override
            public ZstdCompressorOutputStream wrap(FileOutputStream os) throws IOException {
                return new ZstdCompressorOutputStream(os, 3, false, true);
            }
        });
    }

}
