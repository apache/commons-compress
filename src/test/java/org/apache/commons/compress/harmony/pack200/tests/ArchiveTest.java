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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

import junit.framework.TestCase;

import org.apache.commons.compress.harmony.pack200.Archive;
import org.apache.commons.compress.harmony.pack200.Pack200Exception;
import org.apache.commons.compress.harmony.pack200.PackingOptions;
import org.apache.commons.compress.harmony.unpack200.Segment;

public class ArchiveTest extends TestCase {

    JarFile in;
    OutputStream out;
    File file;

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
        InputStream in2 = new FileInputStream(file);
        File file2 = File.createTempFile("helloworld", ".jar");
        file2.deleteOnExit();
        JarOutputStream out2 = new JarOutputStream(new FileOutputStream(file2));
        org.apache.commons.compress.harmony.unpack200.Archive archive = new org.apache.commons.compress.harmony.unpack200.Archive(
                in2, out2);
        archive.unpack();
        out2.close();
        in2.close();

        JarFile jarFile = new JarFile(file2);
        JarEntry entry = jarFile
                .getJarEntry("org/apache/harmony/archive/tests/internal/pack200/HelloWorld.class");
        assertNotNull(entry);
        InputStream ours = jarFile.getInputStream(entry);

        JarFile jarFile2 = new JarFile(new File(Segment.class.getResource(
                "/pack200/hw.jar").toURI()));
        JarEntry entry2 = jarFile2
                .getJarEntry("org/apache/harmony/archive/tests/internal/pack200/HelloWorld.class");
        assertNotNull(entry2);

        InputStream expected = jarFile2.getInputStream(entry2);

        BufferedReader reader1 = new BufferedReader(new InputStreamReader(ours));
        BufferedReader reader2 = new BufferedReader(new InputStreamReader(
                expected));
        String line1 = reader1.readLine();
        String line2 = reader2.readLine();
        int i = 1;
        while (line1 != null || line2 != null) {
            assertEquals("Unpacked class files differ", line2, line1);
            line1 = reader1.readLine();
            line2 = reader2.readLine();
            i++;
        }
        reader1.close();
        reader2.close();
    }

    public void testSQL() throws IOException, Pack200Exception, URISyntaxException {
        in = new JarFile(new File(Archive.class.getResource(
                "/pack200/sqlUnpacked.jar").toURI()));
        file = File.createTempFile("sql", ".pack");
        file.deleteOnExit();
        out = new FileOutputStream(file);
        PackingOptions options = new PackingOptions();
        options.setGzip(false);
        Archive ar = new Archive(in, out, options);
        ar.pack();
        in.close();
        out.close();

        // now unpack
        InputStream in2 = new FileInputStream(file);
        File file2 = File.createTempFile("sqlout", ".jar");
        file2.deleteOnExit();
        JarOutputStream out2 = new JarOutputStream(new FileOutputStream(file2));
        org.apache.commons.compress.harmony.unpack200.Archive archive = new org.apache.commons.compress.harmony.unpack200.Archive(in2, out2);
        archive.unpack();
        JarFile jarFile = new JarFile(file2);

        File compareFile = new File(Archive.class.getResource(
                "/pack200/sqlUnpacked.jar").toURI());
        JarFile jarFile2 = new JarFile(compareFile);

        assertEquals(jarFile2.size(), jarFile.size());

        compareFiles(jarFile, jarFile2);
    }

    public void testAlternativeConstructor() throws IOException, URISyntaxException, Pack200Exception {
        JarInputStream inStream = new JarInputStream(new FileInputStream(
                new File(Archive.class.getResource(
                        "/pack200/sqlUnpacked.jar").toURI())));
        file = File.createTempFile("sql", ".pack.gz");
        file.deleteOnExit();
        out = new FileOutputStream(file);
        new Archive(inStream, out, null).pack();
        inStream.close();
        out.close();
    }

    public void testLargeClass() throws IOException, Pack200Exception,
            URISyntaxException {
        in = new JarFile(new File(Archive.class.getResource(
                "/pack200/largeClassUnpacked.jar")
                .toURI()));
        file = File.createTempFile("largeClass", ".pack");
        file.deleteOnExit();
        out = new FileOutputStream(file);
        PackingOptions options = new PackingOptions();
        options.setGzip(false);
        new Archive(in, out, options).pack();
        in.close();
        out.close();

        // now unpack
        InputStream in2 = new FileInputStream(file);
        File file2 = File.createTempFile("largeClassOut", ".jar");
        file2.deleteOnExit();
        JarOutputStream out2 = new JarOutputStream(new FileOutputStream(file2));
        org.apache.commons.compress.harmony.unpack200.Archive archive = new org.apache.commons.compress.harmony.unpack200.Archive(in2, out2);
        archive.unpack();
        JarFile jarFile = new JarFile(file2);

        File compareFile = new File(Archive.class.getResource(
                "/pack200/largeClassUnpacked.jar").toURI());
        JarFile jarFile2 = new JarFile(compareFile);

        assertEquals(jarFile2.size(), jarFile.size());

        compareFiles(jarFile, jarFile2);
    }

    public void testJNDI() throws IOException, Pack200Exception, URISyntaxException {
        in = new JarFile(new File(Archive.class.getResource(
                "/pack200/jndi.jar").toURI()));
        file = File.createTempFile("jndi", ".pack");
        file.deleteOnExit();
        out = new FileOutputStream(file);
        PackingOptions options = new PackingOptions();
        options.setGzip(false);
        new Archive(in, out, options).pack();
        in.close();
        out.close();

        // now unpack
        InputStream in2 = new FileInputStream(file);
        File file2 = File.createTempFile("jndiout", ".jar");
        file2.deleteOnExit();
        JarOutputStream out2 = new JarOutputStream(new FileOutputStream(file2));
        org.apache.commons.compress.harmony.unpack200.Archive archive = new org.apache.commons.compress.harmony.unpack200.Archive(in2, out2);
        archive.unpack();
        JarFile jarFile = new JarFile(file2);
        JarFile jarFile2 = new JarFile(new File(Archive.class.getResource(
                "/pack200/jndiUnpacked.jar").toURI()));

        compareFiles(jarFile, jarFile2);
    }

    public void testAnnotations() throws IOException, Pack200Exception,
            URISyntaxException {
        in = new JarFile(new File(Archive.class.getResource(
                "/pack200/annotationsUnpacked.jar")
                .toURI()));
        file = File.createTempFile("annotations", ".pack");
        file.deleteOnExit();
        out = new FileOutputStream(file);
        PackingOptions options = new PackingOptions();
        options.setGzip(false);
        new Archive(in, out, options).pack();
        in.close();
        out.close();

        // now unpack
        InputStream in2 = new FileInputStream(file);
        File file2 = File.createTempFile("annotationsout", ".jar");
        file2.deleteOnExit();
        JarOutputStream out2 = new JarOutputStream(new FileOutputStream(file2));
        org.apache.commons.compress.harmony.unpack200.Archive archive = new org.apache.commons.compress.harmony.unpack200.Archive(
                in2, out2);
        archive.unpack();
        JarFile jarFile = new JarFile(file2);
        JarFile jarFile2 = new JarFile(new File(Archive.class.getResource(
                "/pack200/annotationsUnpacked.jar").toURI()));

        compareFiles(jarFile, jarFile2);
    }

    public void testAnnotations2() throws IOException, Pack200Exception,
            URISyntaxException {

        in = new JarFile(new File(Archive.class.getResource(
                "/pack200/annotations.jar").toURI()));
        file = File.createTempFile("annotations", ".pack");
        file.deleteOnExit();
        out = new FileOutputStream(file);
        PackingOptions options = new PackingOptions();
        options.setGzip(false);
        new Archive(in, out, options).pack();
        in.close();
        out.close();

        // now unpack
        InputStream in2 = new FileInputStream(file);
        File file2 = File.createTempFile("annotationsout", ".jar");
        file2.deleteOnExit();
        JarOutputStream out2 = new JarOutputStream(new FileOutputStream(file2));
        org.apache.commons.compress.harmony.unpack200.Archive archive = new org.apache.commons.compress.harmony.unpack200.Archive(
                in2, out2);
        archive.unpack();

        // TODO: This isn't quite right - to fix
        JarFile jarFile = new JarFile(file2);
        JarFile jarFile2 = new JarFile(new File(Archive.class.getResource(
                "/pack200/annotationsRI.jar")
                .toURI()));
        compareFiles(jarFile, jarFile2);
    }

//     Test with an archive containing Annotations
    public void testWithAnnotations2() throws Exception {
        InputStream i = Archive.class
                .getResourceAsStream("/pack200/annotationsRI.pack.gz");
        file = File.createTempFile("annotations", ".jar");
        file.deleteOnExit();
        JarOutputStream jout = new JarOutputStream(new FileOutputStream(file));
        org.apache.commons.compress.harmony.unpack200.Archive archive = new org.apache.commons.compress.harmony.unpack200.Archive(
                i, jout);
        archive.unpack();
        JarFile jarFile = new JarFile(file);
        JarFile jarFile2 = new JarFile(new File(Archive.class.getResource(
                "/pack200/annotationsRI.jar")
                .toURI()));

        compareFiles(jarFile, jarFile2);
    }

    public void testMultipleJars() throws URISyntaxException, IOException, Pack200Exception {
    	File folder = new File(Archive.class
    			.getResource("/pack200/jars").toURI());
    	String[] children = folder.list();
    	for (String child : children) {
			if(child.endsWith(".jar") && !child.endsWith("Unpacked.jar")) {
				File inputFile = new File(folder, child);
				in = new JarFile(inputFile);
				file = File.createTempFile("temp", ".pack.gz");
		        file.deleteOnExit();
		        out = new FileOutputStream(file);
//		        System.out.println("packing " + children[i]);
		        new Archive(in, out, null).pack();
		        in.close();
		        out.close();

		        // unpack and compare

			}
		}
    }

    private void compareJarEntries(JarFile jarFile, JarFile jarFile2) {
        Enumeration entries = jarFile.entries();
        while (entries.hasMoreElements()) {

            JarEntry entry = (JarEntry) entries.nextElement();
            assertNotNull(entry);

            String name = entry.getName();
            JarEntry entry2 = jarFile2.getJarEntry(name);
            assertNotNull("Missing Entry: " + name, entry2);
        }
    }

    private void compareFiles(JarFile jarFile, JarFile jarFile2)
            throws IOException {
        Enumeration entries = jarFile.entries();
        while (entries.hasMoreElements()) {

            JarEntry entry = (JarEntry) entries.nextElement();
            assertNotNull(entry);

            String name = entry.getName();
            JarEntry entry2 = jarFile2.getJarEntry(name);
            assertNotNull("Missing Entry: " + name, entry2);
//            assertEquals(entry.getTime(), entry2.getTime());
            if (!name.equals("META-INF/MANIFEST.MF")) { // Manifests aren't
                                                        // necessarily
                                                        // byte-for-byte
                                                        // identical

                InputStream ours = jarFile.getInputStream(entry);
                InputStream expected = jarFile2.getInputStream(entry2);

                BufferedReader reader1 = new BufferedReader(
                        new InputStreamReader(ours));
                BufferedReader reader2 = new BufferedReader(
                        new InputStreamReader(expected));
                String line1 = reader1.readLine();
                String line2 = reader2.readLine();
                int i = 1;
                while (line1 != null || line2 != null) {
                    assertEquals("Unpacked files differ for " + name, line2,
                            line1);
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

}
