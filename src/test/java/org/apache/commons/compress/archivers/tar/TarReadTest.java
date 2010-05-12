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

package org.apache.commons.compress.archivers.tar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.compress.AbstractTestCase;

/**
 * Test that can read various tar file examples.
 * 
 * The class uses nested suites in order to be able to name the test after the file name,
 * as JUnit does not allow one to change the display name of a test.
 */
public class TarReadTest extends AbstractTestCase {
    
    private static final ClassLoader classLoader = TarReadTest.class.getClassLoader();

    private File file;

    private static final ArrayList fileList = new ArrayList();
    
    public TarReadTest(String name) {
        super(name);
    }
    
    private TarReadTest(String name, File file){
        super(name);
        this.file = file;
    }
    
    public static TestSuite suite() throws IOException{
        TestSuite suite = new TestSuite("TarReadTests");
        File arcdir =new File(classLoader.getResource("tarlongpath").getFile());
        assertTrue(arcdir.exists());
        File listing= new File(arcdir,"files.txt");
        assertTrue("File listing is readable",listing.canRead());
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
        for (int i=0; i<files.length; i++){
            final File file = files[i];
            if (file.getName().endsWith(".txt")){
                continue;
            }
            // Appears to be the only way to give the test a variable name
            TestSuite namedSuite = new TestSuite(file.getName());
            Test test = new TarReadTest("testArchive", file);
            namedSuite.addTest(test);
            suite.addTest(namedSuite);
        }        
        return suite;
    }
    
    public void testArchive() throws Exception{
        ArrayList expected=(ArrayList) fileList.clone();
        checkArchiveContent(new TarArchiveInputStream(new FileInputStream(file)), expected);
    }
}
