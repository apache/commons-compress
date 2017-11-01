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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test that can read various archive file examples.
 *
 * This is a very simple implementation.
 *
 * Files must be in resources/archives, and there must be a file.txt containing
 * the list of files in the archives.
 */
@RunWith(Parameterized.class)
public class ArchiveReadTest extends AbstractTestCase {

    private static final ClassLoader CLASSLOADER = ArchiveReadTest.class.getClassLoader();
    private static final File ARCDIR;
    private static final ArrayList<String> FILELIST = new ArrayList<>();

    static {
        try {
            ARCDIR = new File(CLASSLOADER.getResource("archives").toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private final File file;

    public ArchiveReadTest(final String file){
        this.file = new File(ARCDIR, file);
    }

    @BeforeClass
    public static void setUpFileList() throws Exception {
        assertTrue(ARCDIR.exists());
        final File listing= new File(ARCDIR,"files.txt");
        assertTrue("files.txt is readable",listing.canRead());
        final BufferedReader br = new BufferedReader(new FileReader(listing));
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
        assertTrue(ARCDIR.exists());
        final Collection<Object[]> params = new ArrayList<>();
        for (final String f : ARCDIR.list(new FilenameFilter() {
            @Override
            public boolean accept(final File dir, final String name) {
                return !name.endsWith(".txt");
            }
        }))
        {
            params.add(new Object[] { f });
        }
      return params;
    }

    // files.txt contains size and filename
    @Override
    protected String getExpectedString(final ArchiveEntry entry) {
        return entry.getSize() + " " + entry.getName();
    }

    @Test
    public void testArchive() throws Exception{
        @SuppressWarnings("unchecked") // fileList is correct type already
        final
        ArrayList<String> expected= (ArrayList<String>) FILELIST.clone();
        try {
           checkArchiveContent(file, expected);
        } catch (final ArchiveException e) {
            fail("Problem checking "+file);
        } catch (final AssertionError e) { // show error in context
            fail("Problem checking " + file + " " +e);
        }
    }
}
