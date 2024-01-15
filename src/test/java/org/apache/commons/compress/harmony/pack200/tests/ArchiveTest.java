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
package org.apache.commons.compress.harmony.pack200.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

import org.apache.commons.compress.AbstractTempDirTest;
import org.apache.commons.compress.harmony.pack200.Archive;
import org.apache.commons.compress.harmony.pack200.Pack200Exception;
import org.apache.commons.compress.harmony.pack200.PackingOptions;
import org.apache.commons.compress.harmony.unpack200.Segment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ArchiveTest extends AbstractTempDirTest {

    static Stream<Arguments> loadMultipleJars() throws URISyntaxException, IOException {
        return Files.list(Paths.get(Archive.class.getResource("/pack200/jars").toURI())).filter(child -> {
            final String fileName = child.getFileName().toString();
            return fileName.endsWith(".jar") && !fileName.endsWith("Unpacked.jar");
        }).map(Arguments::of);
    }

    private void compareFiles(final JarFile jarFile, final JarFile jarFile2) throws IOException {
        final Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {

            final JarEntry entry = entries.nextElement();
            assertNotNull(entry);

            final String name = entry.getName();
            final JarEntry entry2 = jarFile2.getJarEntry(name);
            assertNotNull(entry2, "Missing Entry: " + name);
//            assertEquals(entry.getTime(), entry2.getTime());
            if (!name.equals("META-INF/MANIFEST.MF")) {
                // Manifests aren't necessarily byte-for-byte identical
                final InputStream ours = jarFile.getInputStream(entry);
                final InputStream expected = jarFile2.getInputStream(entry2);

                try (BufferedReader reader1 = new BufferedReader(new InputStreamReader(ours));
                        BufferedReader reader2 = new BufferedReader(new InputStreamReader(expected))) {
                    String line1 = reader1.readLine();
                    String line2 = reader2.readLine();
                    while (line1 != null || line2 != null) {
                        assertEquals(line2, line1, "Unpacked files differ for " + name);
                        line1 = reader1.readLine();
                        line2 = reader2.readLine();
                    }
                }
            }
        }
    }

    private void compareJarEntries(final JarFile jarFile, final JarFile jarFile2) {
        final Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {

            final JarEntry entry = entries.nextElement();
            assertNotNull(entry);

            final String name = entry.getName();
            final JarEntry entry2 = jarFile2.getJarEntry(name);
            assertNotNull(entry2, "Missing Entry: " + name);
        }
    }

    @Test
    public void testAlternativeConstructor() throws IOException, URISyntaxException, Pack200Exception {
        final File file = createTempFile("sql", ".pack.gz");
        try (JarInputStream inStream = new JarInputStream(new FileInputStream(new File(Archive.class.getResource("/pack200/sqlUnpacked.jar").toURI())));
                FileOutputStream out = new FileOutputStream(file);) {
            new Archive(inStream, out, null).pack();
        }
    }

    @Test
    public void testAnnotations() throws IOException, Pack200Exception, URISyntaxException {
        final File file = createTempFile("annotations", ".pack");
        try (JarFile in = new JarFile(new File(Archive.class.getResource("/pack200/annotationsUnpacked.jar").toURI()));
                FileOutputStream out = new FileOutputStream(file)) {
            final PackingOptions options = new PackingOptions();
            options.setGzip(false);
            new Archive(in, out, options).pack();
        }
        // now unpack
        final File file2 = createTempFile("annotationsout", ".jar");
        try (InputStream in2 = new FileInputStream(file);
                JarOutputStream out2 = new JarOutputStream(new FileOutputStream(file2))) {
            final org.apache.commons.compress.harmony.unpack200.Archive archive = new org.apache.commons.compress.harmony.unpack200.Archive(in2, out2);
            archive.unpack();
        }
        try (JarFile jarFile = new JarFile(file2);
                JarFile jarFile2 = new JarFile(new File(Archive.class.getResource("/pack200/annotationsUnpacked.jar").toURI()))) {
            compareFiles(jarFile, jarFile2);
        }
    }

    @Test
    public void testAnnotations2() throws IOException, Pack200Exception, URISyntaxException {
        final File file = createTempFile("annotations", ".pack");
        try (JarFile in = new JarFile(new File(Archive.class.getResource("/pack200/annotations.jar").toURI()));
                FileOutputStream out = new FileOutputStream(file)) {
            final PackingOptions options = new PackingOptions();
            options.setGzip(false);
            new Archive(in, out, options).pack();
        }

        // now unpack
        final File file2 = createTempFile("annotationsout", ".jar");
        try (InputStream in2 = new FileInputStream(file);
                JarOutputStream out2 = new JarOutputStream(new FileOutputStream(file2))) {
            final org.apache.commons.compress.harmony.unpack200.Archive archive = new org.apache.commons.compress.harmony.unpack200.Archive(in2, out2);
            archive.unpack();
        }
        // TODO: This isn't quite right - to fix
        try (JarFile jarFile = new JarFile(file2);
                JarFile jarFile2 = new JarFile(new File(Archive.class.getResource("/pack200/annotationsRI.jar").toURI()))) {
            compareFiles(jarFile, jarFile2);
        }
    }

    @Test
    public void testHelloWorld() throws IOException, Pack200Exception, URISyntaxException {
        final File file = createTempFile("helloworld", ".pack.gz");
        try (JarFile in = new JarFile(new File(Archive.class.getResource("/pack200/hw.jar").toURI()));
                FileOutputStream out = new FileOutputStream(file);) {
            new Archive(in, out, null).pack();
        }

        // now unpack
        final File file2 = createTempFile("helloworld", ".jar");
        try (InputStream in2 = new FileInputStream(file);
                JarOutputStream out2 = new JarOutputStream(new FileOutputStream(file2))) {
            final org.apache.commons.compress.harmony.unpack200.Archive archive = new org.apache.commons.compress.harmony.unpack200.Archive(in2, out2);
            archive.unpack();
        }

        try (JarFile jarFile = new JarFile(file2)) {
            final JarEntry entry = jarFile.getJarEntry("org/apache/harmony/archive/tests/internal/pack200/HelloWorld.class");
            assertNotNull(entry);
            try (InputStream ours = jarFile.getInputStream(entry)) {

                final JarFile jarFile2 = new JarFile(new File(Segment.class.getResource("/pack200/hw.jar").toURI()));
                final JarEntry entry2 = jarFile2.getJarEntry("org/apache/harmony/archive/tests/internal/pack200/HelloWorld.class");
                assertNotNull(entry2);

                final InputStream expected = jarFile2.getInputStream(entry2);

                try (BufferedReader reader1 = new BufferedReader(new InputStreamReader(ours));
                        BufferedReader reader2 = new BufferedReader(new InputStreamReader(expected))) {
                    String line1 = reader1.readLine();
                    String line2 = reader2.readLine();
                    int i = 1;
                    while (line1 != null || line2 != null) {
                        assertEquals(line2, line1, "Unpacked class files differ, i = " + i);
                        line1 = reader1.readLine();
                        line2 = reader2.readLine();
                        i++;
                    }
                }
            }
        }
    }

    @Test
    public void testJNDI() throws IOException, Pack200Exception, URISyntaxException {
        final File file = createTempFile("jndi", ".pack");
        try (JarFile in = new JarFile(new File(Archive.class.getResource("/pack200/jndi.jar").toURI()))) {
            final FileOutputStream out = new FileOutputStream(file);
            final PackingOptions options = new PackingOptions();
            options.setGzip(false);
            new Archive(in, out, options).pack();
            out.close();
        }

        // now unpack
        final File file2 = createTempFile("jndiout", ".jar");
        try (InputStream in2 = new FileInputStream(file);
                JarOutputStream out2 = new JarOutputStream(new FileOutputStream(file2))) {
            final org.apache.commons.compress.harmony.unpack200.Archive archive = new org.apache.commons.compress.harmony.unpack200.Archive(in2, out2);
            archive.unpack();
        }
        try (JarFile jarFile = new JarFile(file2);
                JarFile jarFile2 = new JarFile(new File(Archive.class.getResource("/pack200/jndiUnpacked.jar").toURI()))) {
            compareFiles(jarFile, jarFile2);
        }
    }

    @Test
    public void testLargeClass() throws IOException, Pack200Exception, URISyntaxException {
        final File file = createTempFile("largeClass", ".pack");
        try (JarFile in = new JarFile(new File(Archive.class.getResource("/pack200/largeClassUnpacked.jar").toURI()));
                FileOutputStream out = new FileOutputStream(file)) {
            final PackingOptions options = new PackingOptions();
            options.setGzip(false);
            new Archive(in, out, options).pack();
        }

        // now unpack
        final File file2 = createTempFile("largeClassOut", ".jar");
        try (InputStream in2 = new FileInputStream(file);
                JarOutputStream out2 = new JarOutputStream(new FileOutputStream(file2))) {
            final org.apache.commons.compress.harmony.unpack200.Archive archive = new org.apache.commons.compress.harmony.unpack200.Archive(in2, out2);
            archive.unpack();
        }
        final File compareFile = new File(Archive.class.getResource("/pack200/largeClassUnpacked.jar").toURI());
        try (JarFile jarFile = new JarFile(file2);
                JarFile jarFile2 = new JarFile(compareFile)) {
            assertEquals(jarFile2.size(), jarFile.size());
            compareFiles(jarFile, jarFile2);
        }
    }

    @ParameterizedTest
    @MethodSource("loadMultipleJars")
    public void testMultipleJars(final Path path) throws IOException, Pack200Exception {
        final File file = createTempFile("temp", ".pack.gz");
        final File inputFile = path.toFile();
        try (JarFile in = new JarFile(inputFile);
                FileOutputStream out = new FileOutputStream(file)) {
            // System.out.println("packing " + children[i]);
            new Archive(in, out, null).pack();
        }
        // unpack and compare
    }

    @Test
    public void testSQL() throws IOException, Pack200Exception, URISyntaxException {
        final File file = createTempFile("sql", ".pack");
        try (JarFile in = new JarFile(new File(Archive.class.getResource("/pack200/sqlUnpacked.jar").toURI()));
                FileOutputStream out = new FileOutputStream(file)) {
            final PackingOptions options = new PackingOptions();
            options.setGzip(false);
            new Archive(in, out, options).pack();
        }

        // now unpack
        final File file2 = createTempFile("sqlout", ".jar");
        try (InputStream in2 = new FileInputStream(file);
                JarOutputStream out2 = new JarOutputStream(new FileOutputStream(file2))) {
            final org.apache.commons.compress.harmony.unpack200.Archive archive = new org.apache.commons.compress.harmony.unpack200.Archive(in2, out2);
            archive.unpack();
        }
        final File compareFile = new File(Archive.class.getResource("/pack200/sqlUnpacked.jar").toURI());
        try (JarFile jarFile = new JarFile(file2);
                JarFile jarFile2 = new JarFile(compareFile)) {
            assertEquals(jarFile2.size(), jarFile.size());
            compareFiles(jarFile, jarFile2);
        }
    }

    // Test with an archive containing Annotations
    @Test
    public void testWithAnnotations2() throws Exception {
        final File file = createTempFile("annotations", ".jar");
        try (InputStream input = Archive.class.getResourceAsStream("/pack200/annotationsRI.pack.gz");
                JarOutputStream jout = new JarOutputStream(new FileOutputStream(file))) {
            final org.apache.commons.compress.harmony.unpack200.Archive archive = new org.apache.commons.compress.harmony.unpack200.Archive(input, jout);
            archive.unpack();
        }
        try (JarFile jarFile = new JarFile(file);
                JarFile jarFile2 = new JarFile(new File(Archive.class.getResource("/pack200/annotationsRI.jar").toURI()))) {
            compareFiles(jarFile, jarFile2);
        }
    }

}
