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
package org.apache.commons.compress.compressors.z;

import static org.apache.commons.compress.AbstractTest.getFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.IntStream;
import java.util.stream.Collectors;

import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for class {@link ZCompressorInputStream}.
 *
 * @see ZCompressorInputStream
 */
public class ZCompressorInputStreamTest {

    @Test
    public void testFailsToCreateZCompressorInputStreamAndThrowsIOException() {
        final SequenceInputStream sequenceInputStream = new SequenceInputStream(Collections.emptyEnumeration());
        assertThrows(IOException.class, () -> new ZCompressorInputStream(sequenceInputStream));
    }

    @Test
    public void testMultiByteReadConsistentlyReturnsMinusOneAtEof() throws IOException {
        final File input = getFile("bla.tar.Z");
        final byte[] buf = new byte[2];
        try (InputStream is = Files.newInputStream(input.toPath());
                ZCompressorInputStream in = new ZCompressorInputStream(is)) {
            IOUtils.toByteArray(in);
            assertEquals(-1, in.read(buf));
            assertEquals(-1, in.read(buf));
        }
    }

    @Test
    public void testSingleByteReadConsistentlyReturnsMinusOneAtEof() throws IOException {
        final File input = getFile("bla.tar.Z");
        try (InputStream is = Files.newInputStream(input.toPath());
                ZCompressorInputStream in = new ZCompressorInputStream(is)) {
            IOUtils.toByteArray(in);
            assertEquals(-1, in.read());
            assertEquals(-1, in.read());
        }
    }

    @Test
    public void testInvalidMaxCodeSize() throws IOException {
        Set<Integer> invalidValues = new TreeSet<>();
        invalidValues.addAll(IntStream.range(Byte.MIN_VALUE, -120).boxed().collect(Collectors.toSet()));
        invalidValues.addAll(IntStream.range(-97, -88).boxed().collect(Collectors.toSet()));
        invalidValues.addAll(IntStream.range(-65, -56).boxed().collect(Collectors.toSet()));
        invalidValues.addAll(IntStream.range(-33, -24).boxed().collect(Collectors.toSet()));
        invalidValues.addAll(IntStream.range(-1, 8).boxed().collect(Collectors.toSet()));
        invalidValues.addAll(IntStream.range(31, 40).boxed().collect(Collectors.toSet()));
        invalidValues.addAll(IntStream.range(63, 72).boxed().collect(Collectors.toSet()));
        invalidValues.addAll(IntStream.range(95, 104).boxed().collect(Collectors.toSet()));
        invalidValues.add(127);

        final File input = getFile("bla.tar.Z");
        try (final InputStream contentStream = Files.newInputStream(input.toPath())) {
            final byte[] content = IOUtils.toByteArray(contentStream);

            for (int value : invalidValues) {
                content[2] = (byte) value;

                // Test that invalid values always throw an IOException
                assertThrows(IOException.class, () ->
                        new ZCompressorInputStream(new ByteArrayInputStream(content), 1024 * 1024)
                );
            }
        }
    }

}
