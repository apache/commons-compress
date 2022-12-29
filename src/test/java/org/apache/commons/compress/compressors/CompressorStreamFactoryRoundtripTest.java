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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.stream.Stream;

import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class CompressorStreamFactoryRoundtripTest {

    public static Stream<Arguments> data() {
        // @formatter:off
        return Stream.of(
                Arguments.of(CompressorStreamFactory.BZIP2),
                Arguments.of(CompressorStreamFactory.DEFLATE),
                Arguments.of(CompressorStreamFactory.GZIP),
                // CompressorStreamFactory.LZMA, // Not implemented yet
                // CompressorStreamFactory.PACK200, // Bug
                // CompressorStreamFactory.SNAPPY_FRAMED, // Not implemented yet
                // CompressorStreamFactory.SNAPPY_RAW, // Not implemented yet
                Arguments.of(CompressorStreamFactory.XZ)
                // CompressorStreamFactory.Z, // Not implemented yet
        );
        // @formatter:on
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testCompressorStreamFactoryRoundtrip(final String compressorName) throws Exception {
        final CompressorStreamProvider factory = new CompressorStreamFactory();
        final ByteArrayOutputStream compressedOs = new ByteArrayOutputStream();
        final CompressorOutputStream compressorOutputStream = factory.createCompressorOutputStream(compressorName, compressedOs);
        final String fixture = "The quick brown fox jumps over the lazy dog";
        compressorOutputStream.write(fixture.getBytes(UTF_8));
        compressorOutputStream.flush();
        compressorOutputStream.close();
        final ByteArrayInputStream is = new ByteArrayInputStream(compressedOs.toByteArray());
        final CompressorInputStream compressorInputStream = factory.createCompressorInputStream(compressorName, is, false);
        final ByteArrayOutputStream decompressedOs = new ByteArrayOutputStream();
        IOUtils.copy(compressorInputStream, decompressedOs);
        compressorInputStream.close();
        decompressedOs.flush();
        decompressedOs.close();
        assertEquals(fixture, decompressedOs.toString("UTF-8"));
    }

}
