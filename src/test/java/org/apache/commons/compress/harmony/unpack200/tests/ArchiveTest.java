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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import org.apache.commons.compress.AbstractTempDirTest;
import org.apache.commons.compress.harmony.unpack200.Archive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for org.apache.commons.compress.harmony.unpack200.Archive, which is the main class for unpack200.
 */
public class ArchiveTest extends AbstractTempDirTest {

    InputStream in;
    JarOutputStream out;

    @AfterEach
    public void tearDown() throws Exception {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testAlternativeConstructor() throws Exception {
        final String inputFile = new File(Archive.class.getResource("/pack200/sql.pack.gz").toURI()).getPath();
        final File file = createTempFile("sql", ".jar");
        final String outputFile = file.getPath();
        final Archive archive = new Archive(inputFile, outputFile);
        archive.unpack();
    }

    @Test
    public void testDeflateHint() throws Exception {
        in = Archive.class.getResourceAsStream("/pack200/sql.pack.gz");
        File file = createTempFile("sql", ".jar");
        out = new JarOutputStream(new FileOutputStream(file));
        Archive archive = new Archive(in, out);
        archive.setDeflateHint(true);
        archive.unpack();
        try (JarFile jarFile = new JarFile(file)) {
            assertEquals(ZipEntry.DEFLATED, jarFile.getEntry("bin/test/org/apache/harmony/sql/tests/internal/rowset/CachedRowSetImplTest.class").getMethod());

            in = Archive.class.getResourceAsStream("/pack200/sql.pack.gz");
            file = createTempFile("sql", ".jar");
            out = new JarOutputStream(new FileOutputStream(file));
            archive = new Archive(in, out);
            archive.setDeflateHint(false);
            archive.unpack();
        }
        try (JarFile jarFile = new JarFile(file)) {
            assertEquals(ZipEntry.STORED, jarFile.getEntry("bin/test/org/apache/harmony/sql/tests/internal/rowset/CachedRowSetImplTest.class").getMethod());
        }
    }

    @Test
    public void testJustResourcesGZip() throws Exception {
        in = Archive.class.getResourceAsStream("/pack200/JustResources.pack.gz");
        final File file = createTempFile("Just", "ResourcesGz.jar");
        out = new JarOutputStream(new FileOutputStream(file));
        final Archive archive = new Archive(in, out);
        archive.unpack();
    }

    // Test verbose, quiet and log file options.
    @Test
    public void testLoggingOptions() throws Exception {
        // test default option, which is quiet (no output at all)
        in = Archive.class.getResourceAsStream("/pack200/sql.pack.gz");
        File file = createTempFile("logtest", ".jar");
        out = new JarOutputStream(new FileOutputStream(file));
        Archive archive = new Archive(in, out);
        File logFile = createTempFile("logfile", ".txt");
        archive.setLogFile(logFile.getPath());
        archive.unpack();

        // log file should be empty
        try (FileReader reader = new FileReader(logFile)) {
            assertFalse(reader.ready());
        }

        // test verbose
        in = Archive.class.getResourceAsStream("/pack200/sql.pack.gz");
        file = createTempFile("logtest", ".jar");
        out = new JarOutputStream(new FileOutputStream(file));
        archive = new Archive(in, out);
        logFile = createTempFile("logfile", ".txt");
        archive.setLogFile(logFile.getPath());
        archive.setVerbose(true);
        archive.unpack();

        // log file should not be empty
        try (FileReader reader = new FileReader(logFile)) {
            assertTrue(reader.ready());
        }

        // test append option
        final long length = logFile.length();
        in = Archive.class.getResourceAsStream("/pack200/sql.pack.gz");
        file = createTempFile("logtest", ".jar");
        out = new JarOutputStream(new FileOutputStream(file));
        archive = new Archive(in, out);
        archive.setLogFile(logFile.getPath(), true);
        archive.setVerbose(true);
        archive.unpack();
        assertTrue(logFile.length() > length);
        in = Archive.class.getResourceAsStream("/pack200/sql.pack.gz");
        file = createTempFile("logtest", ".jar");
        out = new JarOutputStream(new FileOutputStream(file));
        archive = new Archive(in, out);
        archive.setLogFile(logFile.getPath(), false);
        archive.setVerbose(true);
        archive.unpack();
        assertEquals(logFile.length(), length);

        // test setting quiet explicitly
        in = Archive.class.getResourceAsStream("/pack200/sql.pack.gz");
        file = createTempFile("logtest", ".jar");
        out = new JarOutputStream(new FileOutputStream(file));
        archive = new Archive(in, out);
        logFile = createTempFile("logfile", ".txt");
        archive.setLogFile(logFile.getPath());
        archive.setQuiet(true);
        archive.unpack();

        // log file should be empty
        try (FileReader reader = new FileReader(logFile)) {
            assertFalse(reader.ready());
        }
    }

    @Test
    public void testRemovePackFile() throws Exception {
        final File original = new File(Archive.class.getResource("/pack200/sql.pack.gz").toURI());
        final File copy = createTempFile("sqlcopy", ".pack.gz");
        try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(original));
                BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(copy))) {
            final byte[] bytes = new byte[256];
            int read = inputStream.read(bytes);
            while (read > 0) {
                outputStream.write(bytes, 0, read);
                read = inputStream.read(bytes);
            }
        }
        final String inputFile = copy.getPath();
        final File file = createTempFile("sqlout", ".jar");
        final String outputFile = file.getPath();
        final Archive archive = new Archive(inputFile, outputFile);
        archive.setRemovePackFile(true);
        archive.unpack();
        assertFalse(copy.exists());
    }

    // Test with an archive containing Annotations
    @Test
    public void testWithAnnotations() throws Exception {
        in = Archive.class.getResourceAsStream("/pack200/annotations.pack.gz");
        final File file = createTempFile("annotations", ".jar");
        out = new JarOutputStream(new FileOutputStream(file));
        final Archive archive = new Archive(in, out);
        archive.unpack();
    }

    // Test with an archive packed with the -E0 option
    @Test
    public void testWithE0() throws Exception {
        in = Archive.class.getResourceAsStream("/pack200/simple-E0.pack.gz");
        final File file = createTempFile("simple-e0", ".jar");
        out = new JarOutputStream(new FileOutputStream(file));
        final Archive archive = new Archive(in, out);
        archive.unpack();
    }

    // Test with an archive containing Harmony's JNDI module
    @Test
    public void testWithJNDIE1() throws Exception {
        in = Archive.class.getResourceAsStream("/pack200/jndi-e1.pack.gz");
        final File file = createTempFile("jndi-e1", ".jar");
        out = new JarOutputStream(new FileOutputStream(file));
        final Archive archive = new Archive(in, out);
        archive.unpack();
    }

    // Test with a class containing lots of local variables (regression test for
    // HARMONY-5470)
    @Test
    public void testWithLargeClass() throws Exception {
        in = Archive.class.getResourceAsStream("/pack200/LargeClass.pack.gz");
        final File file = createTempFile("largeClass", ".jar");
        out = new JarOutputStream(new FileOutputStream(file));
        final Archive archive = new Archive(in, out);
        archive.unpack();
    }

    // Test with an archive containing Harmony's Pack200 module
    @Test
    public void testWithPack200() throws Exception {
        in = Archive.class.getResourceAsStream("/pack200/pack200.pack.gz");
        final File file = createTempFile("p200", ".jar");
        out = new JarOutputStream(new FileOutputStream(file));
        final Archive archive = new Archive(in, out);
        archive.unpack();
    }

    // Test with an archive containing Harmony's Pack200 module, packed with -E1
    @Test
    public void testWithPack200E1() throws Exception {
        in = Archive.class.getResourceAsStream("/pack200/pack200-e1.pack.gz");
        final File file = createTempFile("p200-e1", ".jar");
        out = new JarOutputStream(new FileOutputStream(file));
        final Archive archive = new Archive(in, out);
        archive.unpack();
    }

    // Test with an archive containing Harmony's SQL module
    @Test
    public void testWithSql() throws Exception {
        in = Archive.class.getResourceAsStream("/pack200/sql.pack.gz");
        final File file = createTempFile("sql", ".jar");
        out = new JarOutputStream(new FileOutputStream(file));
        final Archive archive = new Archive(in, out);
        archive.unpack();
        final File compareFile = new File(Archive.class.getResource("/pack200/sqlUnpacked.jar").toURI());
        try (JarFile jarFile = new JarFile(file);
                JarFile jarFile2 = new JarFile(compareFile)) {

            final long differenceInJarSizes = Math.abs(compareFile.length() - file.length());
            assertTrue(differenceInJarSizes < 100, "Expected jar files to be a similar size, difference was " + differenceInJarSizes + " bytes");

            final Enumeration<JarEntry> entries = jarFile.entries();
            final Enumeration<JarEntry> entries2 = jarFile2.entries();
            while (entries.hasMoreElements() && entries2.hasMoreElements()) {

                final JarEntry entry = entries.nextElement();
                assertNotNull(entry);
                final String name = entry.getName();

                final JarEntry entry2 = entries2.nextElement();
                assertNotNull(entry2);
                final String name2 = entry2.getName();

                assertEquals(name, name2);

                try (InputStream ours = jarFile.getInputStream(entry);
                        InputStream expected = jarFile2.getInputStream(entry2);
                        BufferedReader reader1 = new BufferedReader(new InputStreamReader(ours));
                        BufferedReader reader2 = new BufferedReader(new InputStreamReader(expected))) {
                    String line1 = reader1.readLine();
                    String line2 = reader2.readLine();
                    int i = 1;
                    while (line1 != null || line2 != null) {
                        assertEquals(line2, line1, "Unpacked class files differ for " + name);
                        line1 = reader1.readLine();
                        line2 = reader2.readLine();
                        i++;
                    }
                    assertTrue(i > 0);
                }
            }
        }
    }

    // Test with an archive containing Harmony's SQL module, packed with -E1
    @Test
    public void testWithSqlE1() throws Exception {
        in = Archive.class.getResourceAsStream("/pack200/sql-e1.pack.gz");
        final File file = createTempFile("sql-e1", ".jar");
        out = new JarOutputStream(new FileOutputStream(file));
        final Archive archive = new Archive(in, out);
        archive.unpack();
    }

}
