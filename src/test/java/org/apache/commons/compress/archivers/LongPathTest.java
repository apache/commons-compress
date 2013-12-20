/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package org.apache.commons.compress.archivers;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream;
import org.apache.commons.compress.archivers.cpio.CpioArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;

/**
 * Test that can read various tar file examples.
 * 
 * The class uses nested suites in order to be able to name the test after the file name,
 * as JUnit does not allow one to change the display name of a test.
 */
public class LongPathTest extends AbstractTestCase {

    private String name;
    private File file;

    private static final Map<String, ArrayList<String>> fileLists = new HashMap<String, ArrayList<String>>();

    public LongPathTest(String name) {
        super(name);
    }

    private LongPathTest(String name, String function, File file) {
        super(function);
        this.name = name;
        this.file = file;
    }

    public static TestSuite suite() throws IOException{
        TestSuite suite = new TestSuite("LongPathTests");
        suite.addTest(createSuite("LongPathTest", "longpath"));
        suite.addTest(createSuite("LongSymlinkTest", "longsymlink"));
        return suite;
    }

    public static TestSuite createSuite(String name, String dirname) throws IOException {
        TestSuite suite = new TestSuite(name);
        File arcdir = getFile(dirname);
        assertTrue(arcdir.exists());
        File listing= new File(arcdir,"files.txt");
        assertTrue("File listing is readable",listing.canRead());
        BufferedReader br = new BufferedReader(new FileReader(listing));

        ArrayList<String> fileList = new ArrayList<String>();
        String line;
        while ((line=br.readLine())!=null){
            if (line.startsWith("#")){
                continue;
            }
            fileList.add(line);
        }
        fileLists.put(name, fileList);
        br.close();
        File[]files=arcdir.listFiles();
        for (final File file : files) {
            if (file.getName().endsWith(".txt")){
                continue;
            }
            // Appears to be the only way to give the test a variable name
            TestSuite namedSuite = new TestSuite(file.getName());
            Test test = new LongPathTest(name, "testArchive", file);
            namedSuite.addTest(test);
            suite.addTest(namedSuite);
        }
        return suite;
    }

    @Override
    protected String getExpectedString(ArchiveEntry entry) {
        if (entry instanceof TarArchiveEntry) {
            TarArchiveEntry tarEntry = (TarArchiveEntry) entry;
            if (tarEntry.isSymbolicLink()) {
                return tarEntry.getName() + " -> " + tarEntry.getLinkName();
            }
        }
        return entry.getName();
    }

    public void testArchive() throws Exception {
        ArrayList<String> fileList = fileLists.get(name);
        @SuppressWarnings("unchecked") // fileList is of correct type
        ArrayList<String> expected = (ArrayList<String>) fileList.clone();
        String name = file.getName();
        if ("minotaur.jar".equals(name) || "minotaur-0.jar".equals(name)){
            expected.add("META-INF/");
            expected.add("META-INF/MANIFEST.MF");
        }
        ArchiveInputStream ais = factory.createArchiveInputStream(new BufferedInputStream(new FileInputStream(file)));
        // check if expected type recognised
        if (name.endsWith(".tar")){
            assertTrue(ais instanceof TarArchiveInputStream);
        } else if (name.endsWith(".jar") || name.endsWith(".zip")){
            assertTrue(ais instanceof ZipArchiveInputStream);
        } else if (name.endsWith(".cpio")){
            assertTrue(ais instanceof CpioArchiveInputStream);
            // Hack: cpio does not add trailing "/" to directory names
            for(int i=0; i < expected.size(); i++){
                String ent = expected.get(i);
                if (ent.endsWith("/")){
                    expected.set(i, ent.substring(0, ent.length()-1));
                }
            }
        } else if (name.endsWith(".ar")){
            assertTrue(ais instanceof ArArchiveInputStream);
            // CPIO does not store directories or directory names
            expected.clear();
            for(int i=0; i < fileList.size(); i++){
                String ent = fileList.get(i);
                if (!ent.endsWith("/")){// not a directory
                    final int lastSlash = ent.lastIndexOf('/');
                    if (lastSlash >= 0) { // extract path name
                        expected.add(ent.substring(lastSlash+1, ent.length()));
                    } else {
                        expected.add(ent);
                    }
                }
            }
        } else {
            fail("Unexpected file type: "+name);
        }
        try {
            checkArchiveContent(ais, expected);
        } catch (AssertionFailedError e) {
            fail("Error processing "+file.getName()+" "+e);
        } finally {
            ais.close();
        }
    }
}
