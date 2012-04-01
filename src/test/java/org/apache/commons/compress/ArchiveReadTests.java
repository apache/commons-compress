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

package org.apache.commons.compress;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;

/**
 * Test that can read various archive file examples.
 * 
 * This is a very simple implementation.
 * 
 * Files must be in resources/archives, and there must be a file.txt containing
 * the list of files in the archives.
 * 
 * The class uses nested suites in order to be able to name the test after the file name,
 * as JUnit does not allow one to change the display name of a test.
 */
public class ArchiveReadTests extends AbstractTestCase {

    final static ClassLoader classLoader = ArchiveReadTests.class.getClassLoader();

    private File file;
    private static final ArrayList<String> fileList = new ArrayList<String>();

    public ArchiveReadTests(String name) {
        super(name);
    }

    private ArchiveReadTests(String name, File file){
        super(name);
        this.file = file;
    }

    public static TestSuite suite() throws IOException{
        TestSuite suite = new TestSuite("ArchiveReadTests");
        File arcdir =new File(classLoader.getResource("archives").getFile());
        assertTrue(arcdir.exists());
        File listing= new File(arcdir,"files.txt");
        assertTrue("files.txt is readable",listing.canRead());
        BufferedReader br = new BufferedReader(new FileReader(listing));
        String line;
        while ((line=br.readLine())!=null){
            if (line.startsWith("#")){
                continue;
            }
            fileList.add(line);
        }
        br.close();
        File[]files=arcdir.listFiles();
        for (final File file : files) {
            if (file.getName().endsWith(".txt")){
                continue;
            }
            // Appears to be the only way to give the test a variable name
            TestSuite namedSuite = new TestSuite(file.getName());
            Test test = new ArchiveReadTests("testArchive", file);
            namedSuite.addTest(test);
            suite.addTest(namedSuite);
        }
        return suite;
    }

    // files.txt contains size and filename
    @Override
    protected String getExpectedString(ArchiveEntry entry) {
        return entry.getSize() + " " + entry.getName();
    }

    public void testArchive() throws Exception{
        @SuppressWarnings("unchecked") // fileList is correct type already
        ArrayList<String> expected= (ArrayList<String>) fileList.clone();
        try {
           checkArchiveContent(file, expected);
        } catch (ArchiveException e) {
            fail("Problem checking "+file);
        }
    }
}
