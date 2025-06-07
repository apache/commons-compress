/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.compress.compressors.z;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

/**
 * Tests for class {@link ZCompressorInputStream}.
 *
 * @see ZCompressorInputStream
 */
public class ZCompressorInputStreamTest {

    @Test
    void testFailsToCreateZCompressorInputStreamAndThrowsIOException() throws IOException {
        try (SequenceInputStream sequenceInputStream = new SequenceInputStream(Collections.emptyEnumeration())) {
            assertThrows(IOException.class, () -> new ZCompressorInputStream(sequenceInputStream));
        }
    }

    @Test
    void testInvalidMaxCodeSize() throws IOException {
        final byte[] bytes = AbstractTest.readAllBytes("bla.tar.Z");

        // @formatter:off
        final IntStream[] invalid = {
            IntStream.range(Byte.MIN_VALUE, -120),
            IntStream.range(-97, -88),
            IntStream.range(-65, -56),
            IntStream.range(-33, -24),
            IntStream.range(-1, 8),
            IntStream.range(31, 40),
            IntStream.range(63, 72),
            IntStream.range(95, 104),
            IntStream.range(127, 127)
            };
        // @formatter:on

        Stream.of(invalid).forEach(ints -> ints.forEach(i -> {
            bytes[2] = (byte) i;
            assertThrows(IllegalArgumentException.class, () -> new ZCompressorInputStream(new ByteArrayInputStream(bytes), 1024 * 1024), () -> "value=" + i);
        }));
    }

    @Test
    void testMultiByteReadConsistentlyReturnsMinusOneAtEof() throws IOException {
        final File input = AbstractTest.getFile("bla.tar.Z");
        final byte[] buf = new byte[2];
        try (InputStream is = Files.newInputStream(input.toPath());
                ZCompressorInputStream in = new ZCompressorInputStream(is)) {
            IOUtils.toByteArray(in);
            assertEquals(-1, in.read(buf));
            assertEquals(-1, in.read(buf));
        }
    }

    @Test
    void testSingleByteReadConsistentlyReturnsMinusOneAtEof() throws IOException {
        final File input = AbstractTest.getFile("bla.tar.Z");
        try (InputStream is = Files.newInputStream(input.toPath());
                ZCompressorInputStream in = new ZCompressorInputStream(is)) {
            IOUtils.toByteArray(in);
            assertEquals(-1, in.read());
            assertEquals(-1, in.read());
        }
    }

}
