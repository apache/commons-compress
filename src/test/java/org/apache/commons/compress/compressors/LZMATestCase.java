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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.compressors.lzma.LZMACompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.Test;

public final class LZMATestCase extends AbstractTestCase {

    @Test
    public void lzmaRoundtrip() throws Exception {
        final File input = getFile("test1.xml");
        final File compressed = new File(dir, "test1.xml.xz");
        try (OutputStream out = Files.newOutputStream(compressed.toPath())) {
            try (CompressorOutputStream cos = new CompressorStreamFactory().createCompressorOutputStream("lzma", out)) {
                Files.copy(input.toPath(), cos);
            }
        }
        final byte[] orig = Files.readAllBytes(input.toPath());
        final byte[] uncompressed;
        try (InputStream is = Files.newInputStream(compressed.toPath()); CompressorInputStream in = new LZMACompressorInputStream(is)) {
            uncompressed = IOUtils.toByteArray(in);
        }
        assertArrayEquals(orig, uncompressed);
    }

    @Test
    public void multiByteReadConsistentlyReturnsMinusOneAtEof() throws IOException {
        final File input = getFile("bla.tar.lzma");
        final byte[] buf = new byte[2];
        try (InputStream is = Files.newInputStream(input.toPath())) {
            try (LZMACompressorInputStream in = new LZMACompressorInputStream(is)) {
                IOUtils.toByteArray(in);
                assertEquals(-1, in.read(buf));
                assertEquals(-1, in.read(buf));
            }
        }
    }

    @Test
    public void singleByteReadConsistentlyReturnsMinusOneAtEof() throws IOException {
        final File input = getFile("bla.tar.lzma");
        try (InputStream is = Files.newInputStream(input.toPath())) {
            try (LZMACompressorInputStream in = new LZMACompressorInputStream(is)) {
                IOUtils.toByteArray(in);
                assertEquals(-1, in.read());
                assertEquals(-1, in.read());
            }
        }
    }

    @Test
    public void testLZMAUnarchive() throws Exception {
        final File input = getFile("bla.tar.lzma");
        final File output = new File(dir, "bla.tar");
        try (InputStream is = Files.newInputStream(input.toPath())) {
            try (final CompressorInputStream in = new LZMACompressorInputStream(is)) {
                Files.copy(in, output.toPath());
            }
        }
    }

    @Test
    public void testLZMAUnarchiveWithAutodetection() throws Exception {
        final File input = getFile("bla.tar.lzma");
        final File output = new File(dir, "bla.tar");
        try (InputStream is = new BufferedInputStream(Files.newInputStream(input.toPath()))) {
            try (final CompressorInputStream in = new CompressorStreamFactory().createCompressorInputStream(is)) {
                Files.copy(in, output.toPath());
            }
        }
    }
}
