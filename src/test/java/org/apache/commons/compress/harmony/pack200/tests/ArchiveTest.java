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
import java.io.OutputStream;
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

import org.apache.commons.compress.harmony.pack200.Archive;
import org.apache.commons.compress.harmony.pack200.Pack200Exception;
import org.apache.commons.compress.harmony.pack200.PackingOptions;
import org.apache.commons.compress.harmony.unpack200.Segment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ArchiveTest {

    static Stream<Arguments> loadMultipleJars() throws URISyntaxException, IOException {
        return Files.list(Paths.get(Archive.class.getResource("/pack200/jars").toURI()))
                .filter(child -> {
                    final String fileName = child.getFileName().toString();
                    return fileName.endsWith(".jar") && !fileName.endsWith("Unpacked.jar");
                })
                .map(Arguments::of);
    }
    JarFile in;
    OutputStream out;

    File file;

    private void compareFiles(final JarFile jarFile, final JarFile jarFile2)
            throws IOException {
        final Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {

            final JarEntry entry = entries.nextElement();
            assertNotNull(entry);

            final String name = entry.getName();
            final JarEntry entry2 = jarFile2.getJarEntry(name);
            assertNotNull(entry2, "Missing Entry: " + name);
//            assertEquals(entry.getTime(), entry2.getTime());
            if (!name.equals("META-INF/MANIFEST.MF")) { // Manifests aren't
                                                        // necessarily
                                                        // byte-for-byte
                                                        // identical

                final InputStream ours = jarFile.getInputStream(entry);
                final InputStream expected = jarFile2.getInputStream(entry2);

                final BufferedReader reader1 = new BufferedReader(
                        new InputStreamReader(ours));
                final BufferedReader reader2 = new BufferedReader(
                        new InputStreamReader(expected));
                String line1 = reader1.readLine();
                String line2 = reader2.readLine();
                int i = 1;
                while (line1 != null || line2 != null) {
                    assertEquals(line2, line1, "Unpacked files differ for " + name);
                    line1 = reader1.readLine();
                    line2 = reader2.readLine();
                    i++;
                }
                reader1.close();
                reader2.close();
            }
        }
        jarFile.close();
        jarFile2.close();
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
        final JarInputStream inStream = new JarInputStream(new FileInputStream(
                new File(Archive.class.getResource(
                        "/pack200/sqlUnpacked.jar").toURI())));
        file = File.createTempFile("sql", ".pack.gz");
        file.deleteOnExit();
        out = new FileOutputStream(file);
        new Archive(inStream, out, null).pack();
        inStream.close();
        out.close();
    }

    @Test
    public void testAnnotations() throws IOException, Pack200Exception,
            URISyntaxException {
        in = new JarFile(new File(Archive.class.getResource(
                "/pack200/annotationsUnpacked.jar")
                .toURI()));
        file = File.createTempFile("annotations", ".pack");
        file.deleteOnExit();
        out = new FileOutputStream(file);
        final PackingOptions options = new PackingOptions();
        options.setGzip(false);
        new Archive(in, out, options).pack();
        in.close();
        out.close();

        // now unpack
        final InputStream in2 = new FileInputStream(file);
        final File file2 = File.createTempFile("annotationsout", ".jar");
        file2.deleteOnExit();
        final JarOutputStream out2 = new JarOutputStream(new FileOutputStream(file2));
        final org.apache.commons.compress.harmony.unpack200.Archive archive = new org.apache.commons.compress.harmony.unpack200.Archive(
                in2, out2);
        archive.unpack();
        final JarFile jarFile = new JarFile(file2);
        final JarFile jarFile2 = new JarFile(new File(Archive.class.getResource(
                "/pack200/annotationsUnpacked.jar").toURI()));

        compareFiles(jarFile, jarFile2);
    }

    @Test
    public void testAnnotations2() throws IOException, Pack200Exception,
            URISyntaxException {

        in = new JarFile(new File(Archive.class.getResource(
                "/pack200/annotations.jar").toURI()));
        file = File.createTempFile("annotations", ".pack");
        file.deleteOnExit();
        out = new FileOutputStream(file);
        final PackingOptions options = new PackingOptions();
        options.setGzip(false);
        new Archive(in, out, options).pack();
        in.close();
        out.close();

        // now unpack
        final InputStream in2 = new FileInputStream(file);
        final File file2 = File.createTempFile("annotationsout", ".jar");
        file2.deleteOnExit();
        final JarOutputStream out2 = new JarOutputStream(new FileOutputStream(file2));
        final org.apache.commons.compress.harmony.unpack200.Archive archive = new org.apache.commons.compress.harmony.unpack200.Archive(
                in2, out2);
        archive.unpack();

        // TODO: This isn't quite right - to fix
        final JarFile jarFile = new JarFile(file2);
        final JarFile jarFile2 = new JarFile(new File(Archive.class.getResource(
                "/pack200/annotationsRI.jar")
                .toURI()));
        compareFiles(jarFile, jarFile2);
    }

    @Test
    public void testHelloWorld() throws IOException, Pack200Exception, URISyntaxException {
        in = new JarFile(new File(Archive.class.getResource(
                "/pack200/hw.jar").toURI()));
        file = File.createTempFile("helloworld", ".pack.gz");
        file.deleteOnExit();
        out = new FileOutputStream(file);
        new Archive(in, out, null).pack();
        in.close();
        out.close();

        // now unpack
        final InputStream in2 = new FileInputStream(file);
        final File file2 = File.createTempFile("helloworld", ".jar");
        file2.deleteOnExit();
        final JarOutputStream out2 = new JarOutputStream(new FileOutputStream(file2));
        final org.apache.commons.compress.harmony.unpack200.Archive archive = new org.apache.commons.compress.harmony.unpack200.Archive(
                in2, out2);
        archive.unpack();
        out2.close();
        in2.close();

        final JarFile jarFile = new JarFile(file2);
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
        final BufferedReader reader2 = new BufferedReader(new InputStreamReader(
                expected));
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
    public void testJNDI() throws IOException, Pack200Exception, URISyntaxException {
        in = new JarFile(new File(Archive.class.getResource(
                "/pack200/jndi.jar").toURI()));
        file = File.createTempFile("jndi", ".pack");
        file.deleteOnExit();
        out = new FileOutputStream(file);
        final PackingOptions options = new PackingOptions();
        options.setGzip(false);
        new Archive(in, out, options).pack();
        in.close();
        out.close();

        // now unpack
        final InputStream in2 = new FileInputStream(file);
        final File file2 = File.createTempFile("jndiout", ".jar");
        file2.deleteOnExit();
        final JarOutputStream out2 = new JarOutputStream(new FileOutputStream(file2));
        final org.apache.commons.compress.harmony.unpack200.Archive archive = new org.apache.commons.compress.harmony.unpack200.Archive(in2, out2);
        archive.unpack();
        final JarFile jarFile = new JarFile(file2);
        final JarFile jarFile2 = new JarFile(new File(Archive.class.getResource(
                "/pack200/jndiUnpacked.jar").toURI()));

        compareFiles(jarFile, jarFile2);
    }

    @Test
    public void testLargeClass() throws IOException, Pack200Exception,
            URISyntaxException {
        in = new JarFile(new File(Archive.class.getResource(
                "/pack200/largeClassUnpacked.jar")
                .toURI()));
        file = File.createTempFile("largeClass", ".pack");
        file.deleteOnExit();
        out = new FileOutputStream(file);
        final PackingOptions options = new PackingOptions();
        options.setGzip(false);
        new Archive(in, out, options).pack();
        in.close();
        out.close();

        // now unpack
        final InputStream in2 = new FileInputStream(file);
        final File file2 = File.createTempFile("largeClassOut", ".jar");
        file2.deleteOnExit();
        final JarOutputStream out2 = new JarOutputStream(new FileOutputStream(file2));
        final org.apache.commons.compress.harmony.unpack200.Archive archive = new org.apache.commons.compress.harmony.unpack200.Archive(in2, out2);
        archive.unpack();
        final JarFile jarFile = new JarFile(file2);

        final File compareFile = new File(Archive.class.getResource(
                "/pack200/largeClassUnpacked.jar").toURI());
        final JarFile jarFile2 = new JarFile(compareFile);

        assertEquals(jarFile2.size(), jarFile.size());

        compareFiles(jarFile, jarFile2);
    }

    @ParameterizedTest
    @MethodSource("loadMultipleJars")
    public void testMultipleJars(final Path path) throws URISyntaxException, IOException, Pack200Exception {
        final File inputFile = path.toFile();
        in = new JarFile(inputFile);
        file = File.createTempFile("temp", ".pack.gz");
        file.deleteOnExit();
        out = new FileOutputStream(file);
//		System.out.println("packing " + children[i]);
        new Archive(in, out, null).pack();
        in.close();
        out.close();

        // unpack and compare

    }

    @Test
    public void testSQL() throws IOException, Pack200Exception, URISyntaxException {
        in = new JarFile(new File(Archive.class.getResource(
                "/pack200/sqlUnpacked.jar").toURI()));
        file = File.createTempFile("sql", ".pack");
        file.deleteOnExit();
        out = new FileOutputStream(file);
        final PackingOptions options = new PackingOptions();
        options.setGzip(false);
        final Archive ar = new Archive(in, out, options);
        ar.pack();
        in.close();
        out.close();

        // now unpack
        final InputStream in2 = new FileInputStream(file);
        final File file2 = File.createTempFile("sqlout", ".jar");
        file2.deleteOnExit();
        final JarOutputStream out2 = new JarOutputStream(new FileOutputStream(file2));
        final org.apache.commons.compress.harmony.unpack200.Archive archive = new org.apache.commons.compress.harmony.unpack200.Archive(in2, out2);
        archive.unpack();
        final JarFile jarFile = new JarFile(file2);

        final File compareFile = new File(Archive.class.getResource(
                "/pack200/sqlUnpacked.jar").toURI());
        final JarFile jarFile2 = new JarFile(compareFile);

        assertEquals(jarFile2.size(), jarFile.size());

        compareFiles(jarFile, jarFile2);
    }

    //     Test with an archive containing Annotations
    @Test
    public void testWithAnnotations2() throws Exception {
        final InputStream i = Archive.class
                .getResourceAsStream("/pack200/annotationsRI.pack.gz");
        file = File.createTempFile("annotations", ".jar");
        file.deleteOnExit();
        final JarOutputStream jout = new JarOutputStream(new FileOutputStream(file));
        final org.apache.commons.compress.harmony.unpack200.Archive archive = new org.apache.commons.compress.harmony.unpack200.Archive(
                i, jout);
        archive.unpack();
        final JarFile jarFile = new JarFile(file);
        final JarFile jarFile2 = new JarFile(new File(Archive.class.getResource(
                "/pack200/annotationsRI.jar")
                .toURI()));

        compareFiles(jarFile, jarFile2);
    }

}
