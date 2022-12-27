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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import org.apache.commons.compress.harmony.pack200.Archive;
import org.apache.commons.compress.harmony.pack200.Pack200Exception;
import org.apache.commons.compress.harmony.pack200.PackingOptions;
import org.junit.jupiter.api.Test;

/**
 * Test different options for packing a Jar file
 */
public class PackingOptionsTest {

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
            // assertEquals(entry.getTime(), entry2.getTime());
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
    public void testDeflateHint() {
        // Test default first
        final PackingOptions options = new PackingOptions();
        assertEquals("keep", options.getDeflateHint());
        options.setDeflateHint("true");
        assertEquals("true", options.getDeflateHint());
        options.setDeflateHint("false");
        assertEquals("false", options.getDeflateHint());
        assertThrows(IllegalArgumentException.class, () -> options.setDeflateHint("hello"),
                "Should throw IllegalArgumentException for incorrect deflate hint");
    }

    @Test
    public void testE0() throws Pack200Exception, IOException,
            URISyntaxException {
        final File f1 = new File(Archive.class.getResource(
                "/pack200/jndi.jar").toURI());
        in = new JarFile(f1);
        file = File.createTempFile("jndiE0", ".pack");
        file.deleteOnExit();
        out = new FileOutputStream(file);
        final PackingOptions options = new PackingOptions();
        options.setGzip(false);
        options.setEffort(0);
        final Archive archive = new Archive(in, out, options);
        archive.pack();
        in.close();
        out.close();
        compareFiles(new JarFile(f1), new JarFile(file));

    }

    @Test
    public void testErrorAttributes() throws Exception {
        in = new JarFile(
                new File(
                        Archive.class
                                .getResource(
                                        "/pack200/jndiWithUnknownAttributes.jar")
                                .toURI()));
        file = File.createTempFile("unknown", ".pack");
        file.deleteOnExit();
        out = new FileOutputStream(file);
        final PackingOptions options = new PackingOptions();
        options.addClassAttributeAction("Pack200", "error");
        final Archive ar = new Archive(in, out, options);
        final Error error = assertThrows(Error.class, () -> {
            ar.pack();
            in.close();
            out.close();
        });
        assertEquals("Attribute Pack200 was found", error.getMessage());
    }

    @Test
    public void testKeepFileOrder() throws Exception {
        // Test default first
        PackingOptions options = new PackingOptions();
        assertTrue(options.isKeepFileOrder());
        options.setKeepFileOrder(false);
        assertFalse(options.isKeepFileOrder());

        // Test option works correctly. Test 'True'.
        in = new JarFile(new File(Archive.class.getResource(
                "/pack200/sqlUnpacked.jar").toURI()));
        file = File.createTempFile("sql", ".pack");
        file.deleteOnExit();
        out = new FileOutputStream(file);
        options = new PackingOptions();
        options.setGzip(false);
        Archive archive = new Archive(in, out, options);
        archive.pack();
        in.close();
        out.close();

        InputStream in2 = new FileInputStream(file);
        File file2 = File.createTempFile("sql", ".jar");
        file2.deleteOnExit();
        JarOutputStream out2 = new JarOutputStream(new FileOutputStream(file2));
        org.apache.commons.compress.harmony.unpack200.Archive u2archive = new org.apache.commons.compress.harmony.unpack200.Archive(
                in2, out2);
        u2archive.unpack();

        File compareFile = new File(Archive.class.getResource(
                "/pack200/sqlUnpacked.jar").toURI());
        JarFile jarFile = new JarFile(file2);

        JarFile jarFile2 = new JarFile(compareFile);

        // Check that both jars have the same entries in the same order
        Enumeration<JarEntry> entries = jarFile.entries();
        Enumeration<JarEntry> entries2 = jarFile2.entries();
        while (entries.hasMoreElements()) {

            final JarEntry entry = entries.nextElement();
            assertNotNull(entry);
            final JarEntry entry2 = entries2.nextElement();
            final String name = entry.getName();
            final String name2 = entry2.getName();
            assertEquals(name, name2);
        }

        // Test 'false'
        in = new JarFile(new File(Archive.class.getResource(
                "/pack200/sqlUnpacked.jar").toURI()));
        file = File.createTempFile("sql", ".pack");
        file.deleteOnExit();
        out = new FileOutputStream(file);
        options = new PackingOptions();
        options.setKeepFileOrder(false);
        options.setGzip(false);
        archive = new Archive(in, out, options);
        archive.pack();
        in.close();
        out.close();

        in2 = new FileInputStream(file);
        file2 = File.createTempFile("sql", ".jar");
        file2.deleteOnExit();
        out2 = new JarOutputStream(new FileOutputStream(file2));
        u2archive = new org.apache.commons.compress.harmony.unpack200.Archive(in2, out2);
        u2archive.unpack();

        compareFile = new File(Archive.class.getResource(
                "/pack200/sqlUnpacked.jar").toURI());
        jarFile = new JarFile(file2);

        jarFile2 = new JarFile(compareFile);
        // Check that both jars have the same entries (may be in a different
        // order)
        compareJarEntries(jarFile, jarFile2);

        // Check files are not in order this time
        entries = jarFile.entries();
        entries2 = jarFile2.entries();
        boolean inOrder = true;
        while (entries.hasMoreElements()) {
            final JarEntry entry = entries.nextElement();
            assertNotNull(entry);
            final JarEntry entry2 = entries2.nextElement();
            final String name = entry.getName();
            final String name2 = entry2.getName();
            if (!name.equals(name2)) {
                inOrder = false;
                break;
            }
        }
        assertFalse(inOrder, "Files are not expected to be in order");
    }

    // Test verbose, quiet and log file options.
    @Test
    public void testLoggingOptions() throws Exception {
        // Test defaults
        final PackingOptions options = new PackingOptions();
        assertFalse(options.isVerbose());
        assertNull(options.getLogFile());
        options.setVerbose(true);
        assertTrue(options.isVerbose());
        options.setQuiet(true);
        assertFalse(options.isVerbose());

        final File logFile = File.createTempFile("logfile", ".txt");
        logFile.deleteOnExit();
        options.setLogFile(logFile.getPath());
        assertEquals(logFile.getPath(), options.getLogFile());

        in = new JarFile(new File(Archive.class.getResource(
                "/pack200/hw.jar").toURI()));
        file = File.createTempFile("helloworld", ".pack.gz");
        file.deleteOnExit();
        out = new FileOutputStream(file);
        new Archive(in, out, options).pack();
        in.close();
        out.close();

        // log file should be empty
        FileReader reader = new FileReader(logFile);
        assertFalse(reader.ready());
        reader.close();

        options.setVerbose(true);
        in = new JarFile(new File(Archive.class.getResource(
                "/pack200/hw.jar").toURI()));
        file = File.createTempFile("helloworld", ".pack.gz");
        file.deleteOnExit();
        out = new FileOutputStream(file);
        new Archive(in, out, options).pack();
        in.close();
        out.close();

        // log file should not be empty
        reader = new FileReader(logFile);
        assertTrue(reader.ready());
        reader.close();
    }

    @Test
    public void testModificationTime() throws Exception {
        // Test default first
        PackingOptions options = new PackingOptions();
        assertEquals("keep", options.getModificationTime());
        options.setModificationTime("latest");
        assertEquals("latest", options.getModificationTime());
        assertThrows(IllegalArgumentException.class, () -> {
                    final PackingOptions illegalOption = new PackingOptions();
                    illegalOption.setModificationTime("true");
                },
                "Should throw IllegalArgumentException for incorrect mod time");

        // Test option works correctly. Test 'keep'.
        in = new JarFile(new File(Archive.class.getResource(
                "/pack200/sqlUnpacked.jar").toURI()));
        file = File.createTempFile("sql", ".pack");
        file.deleteOnExit();
        out = new FileOutputStream(file);
        options = new PackingOptions();
        options.setGzip(false);
        Archive archive = new Archive(in, out, options);
        archive.pack();
        in.close();
        out.close();

        InputStream in2 = new FileInputStream(file);
        File file2 = File.createTempFile("sql", ".jar");
        file2.deleteOnExit();
        JarOutputStream out2 = new JarOutputStream(new FileOutputStream(file2));
        org.apache.commons.compress.harmony.unpack200.Archive u2archive = new org.apache.commons.compress.harmony.unpack200.Archive(
                in2, out2);
        u2archive.unpack();

        File compareFile = new File(Archive.class.getResource(
                "/pack200/sqlUnpacked.jar").toURI());
        JarFile jarFile = new JarFile(file2);

        JarFile jarFile2 = new JarFile(compareFile);

        // Check that both jars have the same entries in the same order
        Enumeration<JarEntry> entries = jarFile.entries();
        Enumeration<JarEntry> entries2 = jarFile2.entries();
        while (entries.hasMoreElements()) {

            final JarEntry entry = entries.nextElement();
            assertNotNull(entry);
            final JarEntry entry2 = entries2.nextElement();
            final String name = entry.getName();
            final String name2 = entry2.getName();
            assertEquals(name, name2);
            assertEquals(entry.getTime(), entry2.getTime());
        }

        // Test option works correctly. Test 'latest'.
        in = new JarFile(new File(Archive.class.getResource(
                "/pack200/sqlUnpacked.jar").toURI()));
        file = File.createTempFile("sql", ".pack");
        file.deleteOnExit();
        out = new FileOutputStream(file);
        options = new PackingOptions();
        options.setGzip(false);
        options.setModificationTime("latest");
        archive = new Archive(in, out, options);
        archive.pack();
        in.close();
        out.close();

        in2 = new FileInputStream(file);
        file2 = File.createTempFile("sql", ".jar");
        file2.deleteOnExit();
        out2 = new JarOutputStream(new FileOutputStream(file2));
        u2archive = new org.apache.commons.compress.harmony.unpack200.Archive(in2, out2);
        u2archive.unpack();

        compareFile = new File(Archive.class.getResource(
                "/pack200/sqlUnpacked.jar").toURI());
        jarFile = new JarFile(file2);

        jarFile2 = new JarFile(compareFile);

        // Check that all modtimes are the same and some are not the same as the
        // original
        entries = jarFile.entries();
        entries2 = jarFile2.entries();
        long modtime = -1;
        boolean sameAsOriginal = true;
        while (entries.hasMoreElements()) {
            final JarEntry entry = entries.nextElement();
            assertNotNull(entry);
            final JarEntry entry2 = entries2.nextElement();
            final String name = entry.getName();
            if (!name.startsWith("META-INF")) {
                if (modtime == -1) {
                    modtime = entry.getTime();
                } else {
                    assertEquals(modtime, entry.getTime());
                }
            }
            if (entry2.getTime() != entry.getTime()) {
                sameAsOriginal = false;
            }
        }
        assertFalse(sameAsOriginal, "Some modtimes should have changed");
    }

    @Test
    public void testNewAttributes() throws Exception {
        in = new JarFile(
                new File(
                        Archive.class
                                .getResource(
                                        "/pack200/jndiWithUnknownAttributes.jar")
                                .toURI()));
        file = File.createTempFile("unknown", ".pack");
        file.deleteOnExit();
        out = new FileOutputStream(file);
        final PackingOptions options = new PackingOptions();
        options.addClassAttributeAction("Pack200", "I");
        final Archive ar = new Archive(in, out, options);
        ar.pack();
        in.close();
        out.close();

        // unpack and check this was done right
        final InputStream in2 = new FileInputStream(file);
        final File file2 = File.createTempFile("unknown", ".jar");
        file2.deleteOnExit();
        final JarOutputStream out2 = new JarOutputStream(new FileOutputStream(file2));
        final org.apache.commons.compress.harmony.unpack200.Archive u2archive = new org.apache.commons.compress.harmony.unpack200.Archive(
                in2, out2);
        u2archive.unpack();

        // compare with original
        final File compareFile = new File(Archive.class.getResource(
                "/pack200/jndiWithUnknownAttributes.jar").toURI());
        final JarFile jarFile = new JarFile(file2);

        final JarFile jarFile2 = new JarFile(compareFile);
        assertEquals(jarFile2.size(), jarFile.size());
        compareJarEntries(jarFile, jarFile2);
//        compareFiles(jarFile, jarFile2);
    }

    @Test
    public void testNewAttributes2() throws Exception {
        in = new JarFile(
                new File(
                        Archive.class
                                .getResource(
                                        "/pack200/p200WithUnknownAttributes.jar")
                                .toURI()));
        file = File.createTempFile("unknown", ".pack");
        file.deleteOnExit();
        out = new FileOutputStream(file);
        final PackingOptions options = new PackingOptions();
        options.addFieldAttributeAction("Pack200", "I");
        options.addMethodAttributeAction("Pack200", "I");
        options.addCodeAttributeAction("Pack200", "I");
        final Archive ar = new Archive(in, out, options);
        ar.pack();
        in.close();
        out.close();

        // unpack and check this was done right
        final InputStream in2 = new FileInputStream(file);
        final File file2 = File.createTempFile("unknown", ".jar");
        file2.deleteOnExit();
        final JarOutputStream out2 = new JarOutputStream(new FileOutputStream(file2));
        final org.apache.commons.compress.harmony.unpack200.Archive u2archive = new org.apache.commons.compress.harmony.unpack200.Archive(
                in2, out2);
        u2archive.unpack();

        // compare with original
        final File compareFile = new File(Archive.class.getResource(
                "/pack200/p200WithUnknownAttributes.jar").toURI());
        final JarFile jarFile = new JarFile(file2);

        final JarFile jarFile2 = new JarFile(compareFile);
        assertEquals(jarFile2.size(), jarFile.size());
        compareJarEntries(jarFile, jarFile2);
    }

    @Test
    public void testPassAttributes() throws Exception {
        in = new JarFile(
                new File(
                        Archive.class
                                .getResource(
                                        "/pack200/jndiWithUnknownAttributes.jar")
                                .toURI()));
        file = File.createTempFile("unknown", ".pack");
        file.deleteOnExit();
        out = new FileOutputStream(file);
        final PackingOptions options = new PackingOptions();
        options.addClassAttributeAction("Pack200", "pass");
        final Archive ar = new Archive(in, out, options);
        ar.pack();
        in.close();
        out.close();

        // now unpack
        final InputStream in2 = new FileInputStream(file);
        final File file2 = File.createTempFile("unknown", ".jar");
        file2.deleteOnExit();
        final JarOutputStream out2 = new JarOutputStream(new FileOutputStream(file2));
        final org.apache.commons.compress.harmony.unpack200.Archive u2archive = new org.apache.commons.compress.harmony.unpack200.Archive(
                in2, out2);
        u2archive.unpack();

        // compare with original
        final File compareFile = new File(Archive.class.getResource(
                "/pack200/jndiWithUnknownAttributes.jar").toURI());
        final JarFile jarFile = new JarFile(file2);

        final JarFile jarFile2 = new JarFile(compareFile);
        assertEquals(jarFile2.size(), jarFile.size());
        compareJarEntries(jarFile, jarFile2);
    }

    @Test
    public void testPassFiles() throws IOException, URISyntaxException,
            Pack200Exception {
        // Don't pass any
        in = new JarFile(new File(Archive.class.getResource(
                "/pack200/sqlUnpacked.jar").toURI()));
        final File file0 = File.createTempFile("sql", ".pack");
        file0.deleteOnExit();
        out = new FileOutputStream(file0);
        PackingOptions options = new PackingOptions();
        options.setGzip(false);
        Archive archive = new Archive(in, out, options);
        archive.pack();
        in.close();
        out.close();

        // Pass one file
        in = new JarFile(new File(Archive.class.getResource(
                "/pack200/sqlUnpacked.jar").toURI()));
        file = File.createTempFile("sql", ".pack");
        file.deleteOnExit();
        out = new FileOutputStream(file);
        options = new PackingOptions();
        options.setGzip(false);
        options.addPassFile("bin/test/org/apache/harmony/sql/tests/java/sql/DatabaseMetaDataTest.class");
        assertTrue(options
                .isPassFile("bin/test/org/apache/harmony/sql/tests/java/sql/DatabaseMetaDataTest.class"));
        archive = new Archive(in, out, options);
        archive.pack();
        in.close();
        out.close();

        // Pass a whole directory
        in = new JarFile(new File(Archive.class.getResource(
                "/pack200/sqlUnpacked.jar").toURI()));
        final File file2 = File.createTempFile("sql", ".pack");
        file2.deleteOnExit();
        out = new FileOutputStream(file2);
        options = new PackingOptions();
        options.setGzip(false);
        options.addPassFile("bin/test/org/apache/harmony/sql/tests/java/sql");
        assertTrue(options
                .isPassFile("bin/test/org/apache/harmony/sql/tests/java/sql/DatabaseMetaDataTest.class"));
        assertFalse(options
                .isPassFile("bin/test/org/apache/harmony/sql/tests/java/sqldata/SqlData.class"));
        archive = new Archive(in, out, options);
        archive.pack();
        in.close();
        out.close();

        assertTrue(file.length() > file0.length(),
                "If files are passed then the pack file should be larger");
        assertTrue(file2.length() > file.length(),
                "If more files are passed then the pack file should be larger");

        // now unpack
        final InputStream in2 = new FileInputStream(file);
        final File file3 = File.createTempFile("sql", ".jar");
        file3.deleteOnExit();
        final JarOutputStream out2 = new JarOutputStream(new FileOutputStream(file3));
        org.apache.commons.compress.harmony.unpack200.Archive u2archive = new org.apache.commons.compress.harmony.unpack200.Archive(
                in2, out2);
        u2archive.unpack();

        final File compareFile = new File(Archive.class.getResource(
                "/pack200/sqlUnpacked.jar").toURI());
        JarFile jarFile = new JarFile(file3);

        JarFile jarFile2 = new JarFile(compareFile);
        // Check that both jars have the same entries
        compareJarEntries(jarFile, jarFile2);

        // now unpack the file with lots of passed files
        final InputStream in3 = new FileInputStream(file2);
        final File file4 = File.createTempFile("sql", ".jar");
        file4.deleteOnExit();
        final JarOutputStream out3 = new JarOutputStream(new FileOutputStream(file4));
        u2archive = new org.apache.commons.compress.harmony.unpack200.Archive(in3, out3);
        u2archive.unpack();
        jarFile = new JarFile(file4);
        jarFile2 = new JarFile(compareFile);
        compareJarEntries(jarFile, jarFile2);
    }

    // public void testE0again() throws IOException, Pack200Exception,
    // URISyntaxException {
    // JarInputStream inputStream = new
    // JarInputStream(Archive.class.getResourceAsStream("/pack200/jndi.jar"));
    // file = File.createTempFile("jndiE0", ".pack");
    // out = new FileOutputStream(file);
    // Archive archive = new Archive(inputStream, out, false);
    // archive.setEffort(0);
    // archive.pack();
    // inputStream.close();
    // out.close();
    // in = new JarFile(new File(Archive.class.getResource(
    // "/pack200/jndi.jar").toURI()));
    // compareFiles(in, new JarFile(file));
    // }

    @Test
    public void testSegmentLimits() throws IOException, Pack200Exception,
            URISyntaxException {
        in = new JarFile(new File(Archive.class.getResource(
                "/pack200/hw.jar").toURI()));
        file = File.createTempFile("helloworld", ".pack.gz");
        file.deleteOnExit();
        out = new FileOutputStream(file);
        PackingOptions options = new PackingOptions();
        options.setSegmentLimit(0);
        Archive archive = new Archive(in, out, options);
        archive.pack();
        in.close();
        out.close();

        in = new JarFile(new File(Archive.class.getResource(
                "/pack200/hw.jar").toURI()));
        file = File.createTempFile("helloworld", ".pack.gz");
        file.deleteOnExit();
        out = new FileOutputStream(file);
        options = new PackingOptions();
        options.setSegmentLimit(-1);
        archive = new Archive(in, out, options);
        archive.pack();
        in.close();
        out.close();

        in = new JarFile(new File(Archive.class.getResource(
                "/pack200/hw.jar").toURI()));
        file = File.createTempFile("helloworld", ".pack.gz");
        file.deleteOnExit();
        out = new FileOutputStream(file);
        options = new PackingOptions();
        options.setSegmentLimit(5000);
        archive = new Archive(in, out, options);
        archive.pack();
        in.close();
        out.close();
    }

    @Test
    public void testStripDebug() throws IOException, Pack200Exception,
            URISyntaxException {
        in = new JarFile(new File(Archive.class.getResource(
                "/pack200/sqlUnpacked.jar").toURI()));
        file = File.createTempFile("sql", ".pack");
        file.deleteOnExit();
        out = new FileOutputStream(file);
        final PackingOptions options = new PackingOptions();
        options.setGzip(false);
        options.setStripDebug(true);
        final Archive archive = new Archive(in, out, options);
        archive.pack();
        in.close();
        out.close();

        // now unpack
        final InputStream in2 = new FileInputStream(file);
        final File file2 = File.createTempFile("sqloutNoDebug", ".jar");
        file2.deleteOnExit();
        final JarOutputStream out2 = new JarOutputStream(new FileOutputStream(file2));
        final org.apache.commons.compress.harmony.unpack200.Archive u2archive = new org.apache.commons.compress.harmony.unpack200.Archive(
                in2, out2);
        u2archive.unpack();

        final File compareFile = new File(Archive.class.getResource(
                "/pack200/sqlUnpackedNoDebug.jar")
                .toURI());
        final JarFile jarFile = new JarFile(file2);
        assertTrue(file2.length() < 250000);

        final JarFile jarFile2 = new JarFile(compareFile);

        compareFiles(jarFile, jarFile2);
    }

}
