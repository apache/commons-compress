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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.junit.jupiter.api.Test;

public final class BZip2TestCase extends AbstractTestCase {

    @Test
    public void testBzip2Unarchive() throws Exception {
        final File input = getFile("bla.txt.bz2");
        final File output = new File(dir, "bla.txt");
        try (final InputStream is = Files.newInputStream(input.toPath());
                CompressorInputStream in = new CompressorStreamFactory().createCompressorInputStream("bzip2", is)) {
            Files.copy(in, output.toPath());
        }
    }

    @Test
    public void testBzipCreation() throws Exception {
        File output = null;
        final File input = getFile("test.txt");
        {
            output = new File(dir, "test.txt.bz2");
            try (OutputStream out = Files.newOutputStream(output.toPath());
                    final CompressorOutputStream cos = new CompressorStreamFactory().createCompressorOutputStream("bzip2", out)) {
                Files.copy(input.toPath(), cos);
            }
        }

        final File decompressed = new File(dir, "decompressed.txt");
        {
            try (InputStream is = Files.newInputStream(output.toPath());
                    CompressorInputStream in = new CompressorStreamFactory().createCompressorInputStream("bzip2", is)) {
                Files.copy(in, decompressed.toPath());
            }
        }

        assertEquals(input.length(), decompressed.length());
    }

    @Test
    public void testCOMPRESS131() throws Exception {
        final File input = getFile("COMPRESS-131.bz2");
        try (InputStream is = Files.newInputStream(input.toPath())) {
            try (CompressorInputStream in = new BZip2CompressorInputStream(is, true)) {
                int l = 0;
                while (in.read() != -1) {
                    l++;
                }
                assertEquals(539, l);
            }
        }
    }

    @Test
    public void testConcatenatedStreamsReadFirstOnly() throws Exception {
        final File input = getFile("multiple.bz2");
        try (InputStream is = Files.newInputStream(input.toPath())) {
            try (CompressorInputStream in = new CompressorStreamFactory()
                    .createCompressorInputStream("bzip2", is)) {
                assertEquals('a', in.read());
                assertEquals(-1, in.read());
            }
        }
    }

    @Test
    public void testConcatenatedStreamsReadFully() throws Exception {
        final File input = getFile("multiple.bz2");
        try (InputStream is = Files.newInputStream(input.toPath())) {
            try (CompressorInputStream in = new BZip2CompressorInputStream(is, true)) {
                assertEquals('a', in.read());
                assertEquals('b', in.read());
                assertEquals(0, in.available());
                assertEquals(-1, in.read());
            }
        }
    }

}
