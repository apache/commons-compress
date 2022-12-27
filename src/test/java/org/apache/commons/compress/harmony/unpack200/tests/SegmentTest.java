/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.commons.compress.harmony.unpack200.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import org.apache.commons.compress.harmony.unpack200.Segment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for org.apache.commons.compress.harmony.unpack200.Segment.
 */
public class SegmentTest {

    InputStream in;
    JarOutputStream out;
    File file;

    @AfterEach
    public void tearDown() throws Exception {
        if (in != null) {
            try {
                in.close();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
        try {
            if (out != null) {
                out.close();
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
        file.delete();
    }

    @Test
    public void testHelloWorld() throws Exception {
        in = Segment.class
                .getResourceAsStream("/pack200/HelloWorld.pack");
        file = File.createTempFile("hello", "world.jar");
        file.deleteOnExit();
        out = new JarOutputStream(new FileOutputStream(file));
        final Segment segment = new Segment();
        segment.unpack(in, out);
        out.close();
        out = null;
        final JarFile jarFile = new JarFile(file);

        final JarEntry entry = jarFile
                .getJarEntry("org/apache/harmony/archive/tests/internal/pack200/HelloWorld.class");
        assertNotNull(entry);
        final InputStream ours = jarFile.getInputStream(entry);

        final JarFile jarFile2 = new JarFile(new File(Segment.class.getResource(
                "/pack200/hw.jar").toURI()));
        final JarEntry entry2 = jarFile2
                .getJarEntry("org/apache/harmony/archive/tests/internal/pack200/HelloWorld.class");
        assertNotNull(entry2);

        final InputStream expected = jarFile2.getInputStream(entry2);

        final BufferedReader reader1 = new BufferedReader(new InputStreamReader(ours));
        final BufferedReader reader2 = new BufferedReader(new InputStreamReader(expected));
        String line1 = reader1.readLine();
        String line2 = reader2.readLine();
        int i = 1;
        while (line1 != null || line2 != null) {
            assertEquals(line2, line1, "Unpacked class files differ");
            line1 = reader1.readLine();
            line2 = reader2.readLine();
            i++;
        }
        reader1.close();
        reader2.close();
    }

    @Test
    public void testInterfaceOnly() throws Exception {
        in = Segment.class
                .getResourceAsStream("/pack200/InterfaceOnly.pack");
        file = File.createTempFile("Interface", "Only.jar");
        out = new JarOutputStream(new FileOutputStream(file));
        final Segment segment = new Segment();
        segment.unpack(in, out);
    }

    @Test
    public void testJustResources() throws Exception {
        in = Segment.class
                .getResourceAsStream("/pack200/JustResources.pack");
        file = File.createTempFile("just", "resources.jar");
        out = new JarOutputStream(new FileOutputStream(file));
        final Segment segment = new Segment();
        segment.unpack(in, out);
    }

}
