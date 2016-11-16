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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.apache.commons.compress.utils.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class CompressorStreamFactoryRoundtripTest {

    @Parameters(name = "{0}")
    public static String[] data() {
        return new String[] { //
                CompressorStreamFactory.BZIP2, //
                CompressorStreamFactory.DEFLATE, //
                CompressorStreamFactory.GZIP, //
                // CompressorStreamFactory.LZMA, // Not implemented yet
                // CompressorStreamFactory.PACK200, // Bug
                // CompressorStreamFactory.SNAPPY_FRAMED, // Not implemented yet
                // CompressorStreamFactory.SNAPPY_RAW, // Not implemented yet
                CompressorStreamFactory.XZ, //
                // CompressorStreamFactory.Z, // Not implemented yet
        };
    }

    private final String compressorName;

    public CompressorStreamFactoryRoundtripTest(final String compressorName) {
        this.compressorName = compressorName;
    }

    @Test
    public void testCompressorStreamFactoryRoundtrip() throws Exception {
        final CompressorStreamProvider factory = new CompressorStreamFactory();
        final ByteArrayOutputStream compressedOs = new ByteArrayOutputStream();
        final CompressorOutputStream compressorOutputStream = factory.createCompressorOutputStream(compressorName,
                compressedOs);
        final String fixture = "The quick brown fox jumps over the lazy dog";
        compressorOutputStream.write(fixture.getBytes("UTF-8"));
        compressorOutputStream.flush();
        compressorOutputStream.close();
        final ByteArrayInputStream is = new ByteArrayInputStream(compressedOs.toByteArray());
        final CompressorInputStream compressorInputStream = factory.createCompressorInputStream(compressorName, is, false);
        final ByteArrayOutputStream decompressedOs = new ByteArrayOutputStream();
        IOUtils.copy(compressorInputStream, decompressedOs);
        compressorInputStream.close();
        decompressedOs.flush();
        decompressedOs.close();
        Assert.assertEquals(fixture, decompressedOs.toString("UTF-8"));
    }

}
