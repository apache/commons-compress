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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

    private File file;

    private void compareFiles(final JarFile jarFile, final JarFile jarFile2) throws IOException {
        final Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {

            final JarEntry entry = entries.nextElement();
            assertNotNull(entry);

            final String name = entry.getName();
            final JarEntry entry2 = jarFile2.getJarEntry(name);
            assertNotNull(entry2, "Missing Entry: " + name);
            // assertEquals(entry.getTime(), entry2.getTime());
            if (!name.equals("META-INF/MANIFEST.MF")) {
                // Manifests aren't necessarily byte-for-byte identical
                final InputStream ours = jarFile.getInputStream(entry);
                final InputStream expected = jarFile2.getInputStream(entry2);

                try (BufferedReader reader1 = new BufferedReader(new InputStreamReader(ours));
                        final BufferedReader reader2 = new BufferedReader(new InputStreamReader(expected))) {
                    String line1 = reader1.readLine();
                    String line2 = reader2.readLine();
                    int i = 1;
                    while (line1 != null || line2 != null) {
                        assertEquals(line2, line1, "Unpacked files differ for " + name + " at line " + i);
                        line1 = reader1.readLine();
                        line2 = reader2.readLine();
                        i++;
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
    public void testDeflateHint() {
        // Test default first
        final PackingOptions options = new PackingOptions();
        assertEquals("keep", options.getDeflateHint());
        options.setDeflateHint("true");
        assertEquals("true", options.getDeflateHint());
        options.setDeflateHint("false");
        assertEquals("false", options.getDeflateHint());
        assertThrows(IllegalArgumentException.class, () -> options.setDeflateHint("hello"), "Should throw IllegalArgumentException for incorrect deflate hint");
    }

    @Test
    public void testPackEffort0() throws Pack200Exception, IOException, URISyntaxException {
        final File f1 = new File(Archive.class.getResource("/pack200/jndi.jar").toURI());
        file = File.createTempFile("jndiE0", ".pack");
        file.deleteOnExit();
        try (JarFile in = new JarFile(f1);
                FileOutputStream out = new FileOutputStream(file)) {
            final PackingOptions options = new PackingOptions();
            options.setGzip(false);
            options.setEffort(0);
            new Archive(in, out, options).pack();
        }
        try (JarFile jf1 = new JarFile(f1);
                JarFile jf2 = new JarFile(file)) {
            compareFiles(jf1, jf2);
        }
    }

    @Test
    public void testErrorAttributes() throws Exception {
        file = File.createTempFile("unknown", ".pack");
        file.deleteOnExit();
        try (JarFile in = new JarFile(new File(Archive.class.getResource("/pack200/jndiWithUnknownAttributes.jar").toURI()));
                FileOutputStream out = new FileOutputStream(file)) {
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
    }

    @Test
    public void testKeepFileOrder() throws Exception {
        // Test default first
        PackingOptions options = new PackingOptions();
        assertTrue(options.isKeepFileOrder());
        options.setKeepFileOrder(false);
        assertFalse(options.isKeepFileOrder());

        // Test option works correctly. Test 'True'.
        file = File.createTempFile("sql", ".pack");
        file.deleteOnExit();
        try (JarFile jarFile = new JarFile(new File(Archive.class.getResource("/pack200/sqlUnpacked.jar").toURI()));
                FileOutputStream outputStream = new FileOutputStream(file)) {
            options = new PackingOptions();
            options.setGzip(false);
            new Archive(jarFile, outputStream, options).pack();
        }

        File file2 = File.createTempFile("sql", ".jar");
        file2.deleteOnExit();
        unpackJar(file, file2);

        File compareFile = new File(Archive.class.getResource("/pack200/sqlUnpacked.jar").toURI());
        try (JarFile jarFile = new JarFile(file2);
                JarFile jarFile2 = new JarFile(compareFile)) {
            // Check that both jars have the same entries in the same order
            final Enumeration<JarEntry> entries = jarFile.entries();
            final Enumeration<JarEntry> entries2 = jarFile2.entries();
            while (entries.hasMoreElements()) {
                final JarEntry entry = entries.nextElement();
                assertNotNull(entry);
                final JarEntry entry2 = entries2.nextElement();
                final String name = entry.getName();
                final String name2 = entry2.getName();
                assertEquals(name, name2);
            }
        }
        // Test 'false'
        file = File.createTempFile("sql", ".pack");
        file.deleteOnExit();
        try (JarFile jarFile = new JarFile(new File(Archive.class.getResource("/pack200/sqlUnpacked.jar").toURI()));
                FileOutputStream outputStream = new FileOutputStream(file);) {
            options = new PackingOptions();
            options.setKeepFileOrder(false);
            options.setGzip(false);
            new Archive(jarFile, outputStream, options).pack();
        }

        file2 = File.createTempFile("sql", ".jar");
        file2.deleteOnExit();
        unpackJar(file, file2);

        compareFile = new File(Archive.class.getResource("/pack200/sqlUnpacked.jar").toURI());
        try (JarFile jarFile = new JarFile(file2);
                JarFile jarFile2 = new JarFile(compareFile)) {
            // Check that both jars have the same entries (may be in a different order)
            compareJarEntries(jarFile, jarFile2);
            // Check files are not in order this time
            final Enumeration<JarEntry> entries = jarFile.entries();
            final Enumeration<JarEntry> entries2 = jarFile2.entries();
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

        file = File.createTempFile("helloworld", ".pack.gz");
        file.deleteOnExit();
        try (JarFile in = new JarFile(new File(Archive.class.getResource("/pack200/hw.jar").toURI()));
                FileOutputStream out = new FileOutputStream(file)) {
            new Archive(in, out, options).pack();
        }

        // log file should be empty
        try (FileReader reader = new FileReader(logFile)) {
            assertFalse(reader.ready());
        }

        options.setVerbose(true);
        file = File.createTempFile("helloworld", ".pack.gz");
        file.deleteOnExit();
        try (JarFile in = new JarFile(new File(Archive.class.getResource("/pack200/hw.jar").toURI()));
                FileOutputStream out = new FileOutputStream(file)) {
            new Archive(in, out, options).pack();
        }

        // log file should not be empty
        try (FileReader reader = new FileReader(logFile)) {
            assertTrue(reader.ready());
        }
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
        }, "Should throw IllegalArgumentException for incorrect mod time");

        // Test option works correctly. Test 'keep'.
        file = File.createTempFile("sql", ".pack");
        file.deleteOnExit();
        try (JarFile in = new JarFile(new File(Archive.class.getResource("/pack200/sqlUnpacked.jar").toURI()));
                FileOutputStream out = new FileOutputStream(file)) {
            options = new PackingOptions();
            options.setGzip(false);
            new Archive(in, out, options).pack();
        }

        File file2 = File.createTempFile("sql", ".jar");
        file2.deleteOnExit();
        unpackJar(file, file2);

        File compareFile = new File(Archive.class.getResource("/pack200/sqlUnpacked.jar").toURI());
        try (JarFile jarFile = new JarFile(file2);
                JarFile jarFile2 = new JarFile(compareFile)) {
            // Check that both jars have the same entries in the same order
            final Enumeration<JarEntry> entries = jarFile.entries();
            final Enumeration<JarEntry> entries2 = jarFile2.entries();
            while (entries.hasMoreElements()) {

                final JarEntry entry = entries.nextElement();
                assertNotNull(entry);
                final JarEntry entry2 = entries2.nextElement();
                final String name = entry.getName();
                final String name2 = entry2.getName();
                assertEquals(name, name2);
                assertEquals(entry.getTime(), entry2.getTime());
            }
        }
        // Test option works correctly. Test 'latest'.
        file = File.createTempFile("sql", ".pack");
        file.deleteOnExit();
        try (JarFile in = new JarFile(new File(Archive.class.getResource("/pack200/sqlUnpacked.jar").toURI()));
                FileOutputStream out = new FileOutputStream(file)) {
            options = new PackingOptions();
            options.setGzip(false);
            options.setModificationTime("latest");
            new Archive(in, out, options).pack();
        }

        file2 = File.createTempFile("sql", ".jar");
        file2.deleteOnExit();
        unpackJar(file, file2);

        compareFile = new File(Archive.class.getResource("/pack200/sqlUnpacked.jar").toURI());
        try (JarFile jarFile = new JarFile(file2);
                JarFile jarFile2 = new JarFile(compareFile)) {
            // Check that all mod times are the same and some are not the same as the original
            final Enumeration<JarEntry> entries = jarFile.entries();
            final Enumeration<JarEntry> entries2 = jarFile2.entries();
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
    }

    @Test
    public void testNewAttributes() throws Exception {
        file = File.createTempFile("unknown", ".pack");
        try (FileOutputStream out = new FileOutputStream(file);
                JarFile in = new JarFile(new File(Archive.class.getResource("/pack200/jndiWithUnknownAttributes.jar").toURI()))) {
            file.deleteOnExit();
            final PackingOptions options = new PackingOptions();
            options.addClassAttributeAction("Pack200", "I");
            new Archive(in, out, options).pack();
        }

        // unpack and check this was done right
        final File file2 = File.createTempFile("unknown", ".jar");
        file2.deleteOnExit();
        unpackJar(file, file2);
        // compare with original
        final File compareFile = new File(Archive.class.getResource("/pack200/jndiWithUnknownAttributes.jar").toURI());
        try (JarFile jarFile = new JarFile(file2);
                JarFile jarFile2 = new JarFile(compareFile)) {
            assertEquals(jarFile2.size(), jarFile.size());
            compareJarEntries(jarFile, jarFile2);
//        compareFiles(jarFile, jarFile2);
        }
    }

    private void unpackJar(final File sourceFile, final File destFile) throws Pack200Exception, IOException, FileNotFoundException {
        try (InputStream in2 = new FileInputStream(sourceFile);
                JarOutputStream out2 = new JarOutputStream(new FileOutputStream(destFile))) {
            unpack(in2, out2);
        }
    }

    @Test
    public void testNewAttributes2() throws Exception {
        file = File.createTempFile("unknown", ".pack");
        file.deleteOnExit();
        try (FileOutputStream out = new FileOutputStream(file);
                JarFile in = new JarFile(new File(Archive.class.getResource("/pack200/p200WithUnknownAttributes.jar").toURI()))) {
            final PackingOptions options = new PackingOptions();
            options.addFieldAttributeAction("Pack200", "I");
            options.addMethodAttributeAction("Pack200", "I");
            options.addCodeAttributeAction("Pack200", "I");
            final Archive ar = new Archive(in, out, options);
            ar.pack();
        }
        // unpack and check this was done right
        final File file2 = File.createTempFile("unknown", ".jar");
        file2.deleteOnExit();
        unpackJar(file, file2);

        // compare with original
        final File compareFile = new File(Archive.class.getResource("/pack200/p200WithUnknownAttributes.jar").toURI());
        try (JarFile jarFile = new JarFile(file2);
                JarFile jarFile2 = new JarFile(compareFile)) {
            assertEquals(jarFile2.size(), jarFile.size());
            compareJarEntries(jarFile, jarFile2);
        }
    }

    @Test
    public void testPassAttributes() throws Exception {
        file = File.createTempFile("unknown", ".pack");
        file.deleteOnExit();
        try (FileOutputStream out = new FileOutputStream(file);
                JarFile in = new JarFile(new File(Archive.class.getResource("/pack200/jndiWithUnknownAttributes.jar").toURI()))) {
            final PackingOptions options = new PackingOptions();
            options.addClassAttributeAction("Pack200", "pass");
            final Archive ar = new Archive(in, out, options);
            ar.pack();
        }

        // now unpack
        final File file2 = File.createTempFile("unknown", ".jar");
        file2.deleteOnExit();
        unpackJar(file, file2);

        // compare with original
        final File compareFile = new File(Archive.class.getResource("/pack200/jndiWithUnknownAttributes.jar").toURI());
        try (JarFile jarFile = new JarFile(file2);
                JarFile jarFile2 = new JarFile(compareFile)) {
            assertEquals(jarFile2.size(), jarFile.size());
            compareJarEntries(jarFile, jarFile2);
        }
    }

    @Test
    public void testPassFiles() throws IOException, URISyntaxException, Pack200Exception {
        // Don't pass any
        final File file0 = File.createTempFile("sql", ".pack");
        file0.deleteOnExit();
        try (JarFile in = new JarFile(new File(Archive.class.getResource("/pack200/sqlUnpacked.jar").toURI()));
                FileOutputStream out = new FileOutputStream(file0)) {
            final PackingOptions options = new PackingOptions();
            options.setGzip(false);
            new Archive(in, out, options).pack();
        }

        // Pass one file
        file = File.createTempFile("sql", ".pack");
        file.deleteOnExit();
        try (JarFile in = new JarFile(new File(Archive.class.getResource("/pack200/sqlUnpacked.jar").toURI()));
                FileOutputStream out = new FileOutputStream(file)) {
            final PackingOptions options = new PackingOptions();
            options.setGzip(false);
            options.addPassFile("bin/test/org/apache/harmony/sql/tests/java/sql/DatabaseMetaDataTest.class");
            assertTrue(options.isPassFile("bin/test/org/apache/harmony/sql/tests/java/sql/DatabaseMetaDataTest.class"));
            new Archive(in, out, options).pack();
        }

        // Pass a whole directory
        final File file2 = File.createTempFile("sql", ".pack");
        file2.deleteOnExit();
        try (JarFile in = new JarFile(new File(Archive.class.getResource("/pack200/sqlUnpacked.jar").toURI()));
                FileOutputStream out = new FileOutputStream(file2)) {
            final PackingOptions options = new PackingOptions();
            options.setGzip(false);
            options.addPassFile("bin/test/org/apache/harmony/sql/tests/java/sql");
            assertTrue(options.isPassFile("bin/test/org/apache/harmony/sql/tests/java/sql/DatabaseMetaDataTest.class"));
            assertFalse(options.isPassFile("bin/test/org/apache/harmony/sql/tests/java/sqldata/SqlData.class"));
            new Archive(in, out, options).pack();
        }

        assertTrue(file.length() > file0.length(), "If files are passed then the pack file should be larger");
        assertTrue(file2.length() > file.length(), "If more files are passed then the pack file should be larger");

        // now unpack
        final File file3 = File.createTempFile("sql", ".jar");
        file3.deleteOnExit();
        unpackJar(file, file3);

        final File compareFile = new File(Archive.class.getResource("/pack200/sqlUnpacked.jar").toURI());
        try (JarFile jarFile = new JarFile(file3);
                JarFile jarFile2 = new JarFile(compareFile)) {
            // Check that both jars have the same entries
            compareJarEntries(jarFile, jarFile2);
        }
        // now unpack the file with lots of passed files
        final File file4 = File.createTempFile("sql", ".jar");
        file4.deleteOnExit();
        unpackJar(file2, file4);

        try (JarFile jarFile = new JarFile(file4);
                JarFile jarFile2 = new JarFile(compareFile)) {
            compareJarEntries(jarFile, jarFile2);
        }
    }

    @Test
    public void testSegmentLimits() throws IOException, Pack200Exception, URISyntaxException {
        file = File.createTempFile("helloworld", ".pack.gz");
        file.deleteOnExit();
        try (JarFile in = new JarFile(new File(Archive.class.getResource("/pack200/hw.jar").toURI()));
                FileOutputStream out = new FileOutputStream(file)) {
            final PackingOptions options = new PackingOptions();
            options.setSegmentLimit(0);
            final Archive archive = new Archive(in, out, options);
            archive.pack();
        }

        file = File.createTempFile("helloworld", ".pack.gz");
        file.deleteOnExit();
        try (JarFile in = new JarFile(new File(Archive.class.getResource("/pack200/hw.jar").toURI()));
                FileOutputStream out = new FileOutputStream(file)) {
            final PackingOptions options = new PackingOptions();
            options.setSegmentLimit(-1);
            new Archive(in, out, options).pack();
        }

        file = File.createTempFile("helloworld", ".pack.gz");
        file.deleteOnExit();
        try (JarFile in = new JarFile(new File(Archive.class.getResource("/pack200/hw.jar").toURI()));
                FileOutputStream out = new FileOutputStream(file)) {
            final PackingOptions options = new PackingOptions();
            options.setSegmentLimit(5000);
            new Archive(in, out, options).pack();
        }
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
    public void testStripDebug() throws IOException, Pack200Exception, URISyntaxException {
        file = File.createTempFile("sql", ".pack");
        file.deleteOnExit();
        try (JarFile in = new JarFile(new File(Archive.class.getResource("/pack200/sqlUnpacked.jar").toURI()));
                FileOutputStream out = new FileOutputStream(file)) {
            final PackingOptions options = new PackingOptions();
            options.setGzip(false);
            options.setStripDebug(true);
            final Archive archive = new Archive(in, out, options);
            archive.pack();
        }

        // now unpack
        final File file2 = File.createTempFile("sqloutNoDebug", ".jar");
        file2.deleteOnExit();
        unpackJar(file, file2);

        final File compareFile = new File(Archive.class.getResource("/pack200/sqlUnpackedNoDebug.jar").toURI());
        try (JarFile jarFile = new JarFile(file2);
                JarFile jarFile2 = new JarFile(compareFile)) {
            assertTrue(file2.length() < 250000);
            compareFiles(jarFile, jarFile2);
        }
    }

    private void unpack(final InputStream inputStream, final JarOutputStream outputStream) throws Pack200Exception, IOException {
        new org.apache.commons.compress.harmony.unpack200.Archive(inputStream, outputStream).unpack();
    }

}
