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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;

import junit.framework.AssertionFailedError;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream;
import org.apache.commons.compress.archivers.cpio.CpioArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test that can read various tar file examples.
 * 
  * Files must be in resources/longpath, and there must be a file.txt containing
 * the list of files in the archives.
*/
@RunWith(Parameterized.class)
public class LongPathTest extends AbstractTestCase {

    private static final ClassLoader CLASSLOADER = LongPathTest.class.getClassLoader();
    private static final File ARCDIR = new File(CLASSLOADER.getResource("longpath").getFile());
    private static final ArrayList<String> FILELIST = new ArrayList<String>();

    private File file;

    public LongPathTest(String file){
        this.file = new File(ARCDIR, file);
    }

    @BeforeClass
    public static void setUpFileList() throws Exception {
        assertTrue(ARCDIR.exists());
        File listing= new File(ARCDIR,"files.txt");
        assertTrue("files.txt is readable",listing.canRead());
        BufferedReader br = new BufferedReader(new FileReader(listing));
        String line;
        while ((line=br.readLine())!=null){
            if (!line.startsWith("#")){
                FILELIST.add(line);
            }
        }
        br.close();
    }

    @Parameters(name = "file={0}")
    public static Collection<Object[]> data() {
        Collection<Object[]> params = new ArrayList<Object[]>();
        for (String f : ARCDIR.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return !name.endsWith(".txt");
            }
        })) 
        {
            params.add(new Object[] { f });
        }
      return params;
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

    @Test
    public void testArchive() throws Exception {
        @SuppressWarnings("unchecked") // fileList is of correct type
        ArrayList<String> expected = (ArrayList<String>) FILELIST.clone();
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
            for (String ent : FILELIST) {
                if (!ent.endsWith("/")) {// not a directory
                    final int lastSlash = ent.lastIndexOf('/');
                    if (lastSlash >= 0) { // extract path name
                        expected.add(ent.substring(lastSlash + 1, ent.length()));
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
