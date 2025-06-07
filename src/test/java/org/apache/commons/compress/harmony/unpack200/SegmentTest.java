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
package org.apache.commons.compress.harmony.unpack200;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import org.apache.commons.compress.AbstractTempDirTest;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.io.output.NullOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for org.apache.commons.compress.harmony.unpack200.Segment.
 */
public class SegmentTest extends AbstractTempDirTest {

    @Test
    void testHelloWorld() throws Exception {
        final File file = createTempFile("hello", "world.jar");
        try (InputStream in = Segment.class.getResourceAsStream("/pack200/HelloWorld.pack");
                JarOutputStream out = new JarOutputStream(new FileOutputStream(file))) {
            new Segment().unpack(in, out);
        }
        try (JarFile jarFile = new JarFile(file)) {
            final JarEntry entry = jarFile.getJarEntry("org/apache/harmony/archive/tests/internal/pack200/HelloWorld.class");
            assertNotNull(entry);
            final InputStream ours = jarFile.getInputStream(entry);
            try (JarFile jarFile2 = new JarFile(new File(Segment.class.getResource("/pack200/hw.jar").toURI()))) {
                final JarEntry entry2 = jarFile2.getJarEntry("org/apache/harmony/archive/tests/internal/pack200/HelloWorld.class");
                assertNotNull(entry2);
                final InputStream expected = jarFile2.getInputStream(entry2);
                try (BufferedReader reader1 = new BufferedReader(new InputStreamReader(ours));
                        BufferedReader reader2 = new BufferedReader(new InputStreamReader(expected))) {
                    String line1 = reader1.readLine();
                    String line2 = reader2.readLine();
                    int i = 1;
                    while (line1 != null || line2 != null) {
                        assertEquals(line2, line1, "Unpacked class files differ ar line " + i);
                        line1 = reader1.readLine();
                        line2 = reader2.readLine();
                        i++;
                    }
                }
            }
        }
    }

    @Test
    void testInterfaceOnly() throws Exception {
        final File file = createTempFile("Interface", "Only.jar");
        try (InputStream in = Segment.class.getResourceAsStream("/pack200/InterfaceOnly.pack");
                JarOutputStream out = new JarOutputStream(new FileOutputStream(file))) {
            new Segment().unpack(in, out);
        }
    }

    @Test
    void testJustResources() throws Exception {
        final File file = createTempFile("just", "resources.jar");
        try (InputStream in = Segment.class.getResourceAsStream("/pack200/JustResources.pack");
                JarOutputStream out = new JarOutputStream(new FileOutputStream(file))) {
            new Segment().unpack(in, out);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
    // @formatter:off
            "bandint_oom.pack",
            "cpfloat_oom.pack",
            "cputf8_oom.pack",
            "favoured_oom.pack",
            "filebits_oom.pack",
            "flags_oom.pack",
            "references_oom.pack",
            "segment_header_oom.pack",
            "signatures_oom.pack"
            // @formatter:on
    })
    // Tests of various files that can cause out of memory errors
    void testParsingOOMBounded(final String testFileName) throws Exception {
        final URL url = Segment.class.getResource("/org/apache/commons/compress/pack/" + testFileName);
        try (BoundedInputStream in = Pack200UnpackerAdapter.newBoundedInputStream(url);
                JarOutputStream out = new JarOutputStream(NullOutputStream.INSTANCE)) {
            assertThrows(IOException.class, () -> new Segment().unpack(in, out));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
    // @formatter:off
            "bandint_oom.pack",
            "cpfloat_oom.pack",
            "cputf8_oom.pack",
            "favoured_oom.pack",
            "filebits_oom.pack",
            "flags_oom.pack",
            "references_oom.pack",
            "segment_header_oom.pack",
            "signatures_oom.pack"
            // @formatter:on
    })
    // Tests of various files that can cause out of memory errors
    void testParsingOOMUnounded(final String testFileName) throws Exception {
        try (InputStream in = Segment.class.getResourceAsStream("/org/apache/commons/compress/pack/" + testFileName);
                JarOutputStream out = new JarOutputStream(NullOutputStream.INSTANCE)) {
            assertThrows(IOException.class, () -> new Segment().unpack(in, out));
        }
    }

}
