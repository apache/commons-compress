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
import java.lang.management.ManagementFactory;
import java.nio.file.Files;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.junit.jupiter.api.Test;

public final class XZTest extends AbstractTest {

    @Test
    public void testConcatenatedStreamsReadFirstOnly() throws Exception {
        final File input = getFile("multiple.xz");
        try (InputStream is = Files.newInputStream(input.toPath())) {
            try (CompressorInputStream in = new CompressorStreamFactory().createCompressorInputStream("xz", is)) {
                assertEquals('a', in.read());
                assertEquals(-1, in.read());
            }
        }
    }

    @Test
    public void testConcatenatedStreamsReadFully() throws Exception {
        final File input = getFile("multiple.xz");
        try (InputStream is = Files.newInputStream(input.toPath())) {
            try (CompressorInputStream in = new XZCompressorInputStream(is, true)) {
                assertEquals('a', in.read());
                assertEquals('b', in.read());
                assertEquals(0, in.available());
                assertEquals(-1, in.read());
            }
        }
    }

    @Test
    public void testXZCreation() throws Exception {
        final long max = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax();
        System.out.println("XZTestCase: HeapMax=" + max + " bytes " + (double) max / (1024 * 1024) + " MB");
        final File input = getFile("test1.xml");
        final File output = newTempFile("test1.xml.xz");
        try (OutputStream out = Files.newOutputStream(output.toPath())) {
            try (CompressorOutputStream cos = new CompressorStreamFactory().createCompressorOutputStream("xz", out)) {
                Files.copy(input.toPath(), cos);
            }
        }
    }

    @Test
    public void testXZUnarchive() throws Exception {
        final File input = getFile("bla.tar.xz");
        final File output = newTempFile("bla.tar");
        try (InputStream is = Files.newInputStream(input.toPath())) {
            try (CompressorInputStream in = new CompressorStreamFactory().createCompressorInputStream("xz", is)) {
                Files.copy(in, output.toPath());
            }
        }
    }
}
