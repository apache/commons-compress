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
package org.apache.commons.compress.compressors.deflate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.Test;

public class DeflateCompressorInputStreamTest {

    @Test
    public void availableShouldReturnNonZero() throws IOException {
        final File input = AbstractTestCase.getFile("bla.tar.deflatez");
        try (InputStream is = Files.newInputStream(input.toPath())) {
            final DeflateCompressorInputStream in =
                    new DeflateCompressorInputStream(is);
            assertTrue(in.available() > 0);
            in.close();
        }
    }

    @Test
    public void multiByteReadConsistentlyReturnsMinusOneAtEof() throws IOException {
        final File input = AbstractTestCase.getFile("bla.tar.deflatez");
        final byte[] buf = new byte[2];
        try (InputStream is = Files.newInputStream(input.toPath())) {
            final DeflateCompressorInputStream in =
                    new DeflateCompressorInputStream(is);
            IOUtils.toByteArray(in);
            assertEquals(-1, in.read(buf));
            assertEquals(-1, in.read(buf));
            in.close();
        }
    }

    @Test
    public void shouldBeAbleToSkipAByte() throws IOException {
        final File input = AbstractTestCase.getFile("bla.tar.deflatez");
        try (InputStream is = Files.newInputStream(input.toPath())) {
            final DeflateCompressorInputStream in =
                    new DeflateCompressorInputStream(is);
            assertEquals(1, in.skip(1));
            in.close();
        }
    }

    @Test
    public void singleByteReadConsistentlyReturnsMinusOneAtEof() throws IOException {
        final File input = AbstractTestCase.getFile("bla.tar.deflatez");
        try (InputStream is = Files.newInputStream(input.toPath())) {
            final DeflateCompressorInputStream in =
                    new DeflateCompressorInputStream(is);
            IOUtils.toByteArray(in);
            assertEquals(-1, in.read());
            assertEquals(-1, in.read());
            in.close();
        }
    }

    @Test
    public void singleByteReadWorksAsExpected() throws IOException {
        final File input = AbstractTestCase.getFile("bla.tar.deflatez");
        try (InputStream is = Files.newInputStream(input.toPath())) {
            final DeflateCompressorInputStream in =
                    new DeflateCompressorInputStream(is);
            // tar header starts with filename "test1.xml"
            assertEquals('t', in.read());
            in.close();
        }
    }

}
