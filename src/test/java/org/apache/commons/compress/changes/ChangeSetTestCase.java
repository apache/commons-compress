/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.compress.changes;

import static org.junit.Assert.*;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.junit.Test;

/**
 * Checks several ChangeSet business logics.
 */
public final class ChangeSetTestCase extends AbstractTestCase {

    // Delete a directory tree
    private void archiveListDeleteDir(final String prefix){
        final Iterator<String> it = archiveList.iterator();
        while(it.hasNext()){
            final String entry = it.next();
            if (entry.startsWith(prefix+"/")){ // TODO won't work with folders
                it.remove();
            }
        }
    }

    // Delete a single file
    private void archiveListDelete(final String prefix){
        final Iterator<String> it = archiveList.iterator();
        while(it.hasNext()){
            final String entry = it.next();
            if (entry.equals(prefix)){
                it.remove();
            }
        }
    }

    /**
     * Adds an ArchiveEntry with the same name two times.
     * Only the latest addition should be found in the ChangeSet,
     * the first add should be replaced.
     *
     * @throws Exception
     */
    @Test
    public void testAddChangeTwice() throws Exception {
        InputStream in = null;
        InputStream in2 = null;
        try {
            in = new FileInputStream(getFile("test.txt"));
            in2 = new FileInputStream(getFile("test2.xml"));

            final ArchiveEntry e = new ZipArchiveEntry("test.txt");
            final ArchiveEntry e2 = new ZipArchiveEntry("test.txt");

            final ChangeSet changes = new ChangeSet();
            changes.add(e, in);
            changes.add(e2, in2);

            assertEquals(1, changes.getChanges().size());
            final Change c = changes.getChanges().iterator().next();
            assertEquals(in2, c.getInput());
        } finally {
            if (in != null) {
                in.close();
            }
            if (in2 != null) {
                in2.close();
            }
        }
    }

    /**
     * Adds an ArchiveEntry with the same name two times.
     * Only the first addition should be found in the ChangeSet,
     * the second add should never be added since replace = false
     *
     * @throws Exception
     */
    @Test
    public void testAddChangeTwiceWithoutReplace() throws Exception {
        InputStream in = null;
        InputStream in2 = null;
        try {
            in = new FileInputStream(getFile("test.txt"));
            in2 = new FileInputStream(getFile("test2.xml"));

            final ArchiveEntry e = new ZipArchiveEntry("test.txt");
            final ArchiveEntry e2 = new ZipArchiveEntry("test.txt");

            final ChangeSet changes = new ChangeSet();
            changes.add(e, in, true);
            changes.add(e2, in2, false);

            assertEquals(1, changes.getChanges().size());
            final Change c = changes.getChanges().iterator().next();
            assertEquals(in, c.getInput());
        } finally {
            if (in != null) {
                in.close();
            }
            if (in2 != null) {
                in2.close();
            }
        }
    }

    /**
     * Tries to delete the folder "bla" from an archive file. This should result in
     * the deletion of bla/*, which actually means bla/test4.xml should be
     * removed from the archive. The file something/bla (without ending, named
     * like the folder) should not be deleted.
     *
     * @throws Exception
     */
    @Test
    public void testDeleteDir() throws Exception {
        final String archivename = "cpio";
        final File input = this.createArchive(archivename);

        ArchiveOutputStream out = null;
        ArchiveInputStream ais = null;
        final File result = File.createTempFile("test", "."+archivename);
        result.deleteOnExit();
        try {

            final InputStream is = new FileInputStream(input);
            ais = factory.createArchiveInputStream(archivename, is);

            out = factory.createArchiveOutputStream(archivename,
                    new FileOutputStream(result));

            final ChangeSet changes = new ChangeSet();
            changes.deleteDir("bla");
            archiveListDeleteDir("bla");
            final ChangeSetPerformer performer = new ChangeSetPerformer(changes);
            performer.perform(ais, out);
            is.close();

        } finally {
            if (out != null) {
                out.close();
            }
            if (ais != null) {
                ais.close();
            }
        }

        this.checkArchiveContent(result, archiveList);
    }

    /**
     * Tries to delete the folder "la" from an archive file. This should result in
     * the deletion of la/*, which should not match any files/folders.
     *
     * @throws Exception
     */
    @Test
    public void testDeleteDir2() throws Exception {
        final String archivename = "cpio";
        final File input = this.createArchive(archivename);

        ArchiveOutputStream out = null;
        ArchiveInputStream ais = null;
        final File result = File.createTempFile("test", "."+archivename);
        result.deleteOnExit();
        try {

            final InputStream is = new FileInputStream(input);
            ais = factory.createArchiveInputStream(archivename, is);

            out = factory.createArchiveOutputStream(archivename,
                    new FileOutputStream(result));

            final ChangeSet changes = new ChangeSet();
            changes.deleteDir("la");
            archiveListDeleteDir("la");
            final ChangeSetPerformer performer = new ChangeSetPerformer(changes);
            performer.perform(ais, out);
            is.close();

        } finally {
            if (out != null) {
                out.close();
            }
            if (ais != null) {
                ais.close();
            }
        }

        this.checkArchiveContent(result, archiveList);
    }

    /**
     * Tries to delete the folder "test.txt" from an archive file.
     * This should not match any files/folders.
     *
     * @throws Exception
     */
    @Test
    public void testDeleteDir3() throws Exception {
        final String archivename = "cpio";
        final File input = this.createArchive(archivename);

        ArchiveOutputStream out = null;
        ArchiveInputStream ais = null;
        final File result = File.createTempFile("test", "."+archivename);
        result.deleteOnExit();
        try {

            final InputStream is = new FileInputStream(input);
            ais = factory.createArchiveInputStream(archivename, is);

            out = factory.createArchiveOutputStream(archivename,
                    new FileOutputStream(result));

            final ChangeSet changes = new ChangeSet();
            changes.deleteDir("test.txt");
            archiveListDeleteDir("test.txt");
            final ChangeSetPerformer performer = new ChangeSetPerformer(changes);
            performer.perform(ais, out);
            is.close();

        } finally {
            if (out != null) {
                out.close();
            }
            if (ais != null) {
                ais.close();
            }
        }

        this.checkArchiveContent(result, archiveList);
    }

    /**
     * Tries to delete the file "bla/test5.xml" from an archive. This should
     * result in the deletion of "bla/test5.xml".
     *
     * @throws Exception
     */
    @Test
    public void testDeleteFile() throws Exception {
        final String archivename = "cpio";
        final File input = this.createArchive(archivename);

        ArchiveOutputStream out = null;
        ArchiveInputStream ais = null;
        final File result = File.createTempFile("test", "."+archivename);
        result.deleteOnExit();
        try {

            final InputStream is = new FileInputStream(input);
            ais = factory.createArchiveInputStream(archivename, is);

            out = factory.createArchiveOutputStream(archivename,
                    new FileOutputStream(result));

            final ChangeSet changes = new ChangeSet();
            changes.delete("bla/test5.xml");
            archiveListDelete("bla/test5.xml");

            final ChangeSetPerformer performer = new ChangeSetPerformer(changes);
            performer.perform(ais, out);
            is.close();

        } finally {
            if (out != null) {
                out.close();
            }
            if (ais != null) {
                ais.close();
            }
        }

        this.checkArchiveContent(result, archiveList);
    }

    /**
     * Tries to delete the file "bla" from an archive. This should
     * result in the deletion of nothing.
     *
     * @throws Exception
     */
    @Test
    public void testDeleteFile2() throws Exception {
        final String archivename = "cpio";
        final File input = this.createArchive(archivename);

        ArchiveOutputStream out = null;
        ArchiveInputStream ais = null;
        final File result = File.createTempFile("test", "."+archivename);
        result.deleteOnExit();
        try {

            final InputStream is = new FileInputStream(input);
            ais = factory.createArchiveInputStream(archivename, is);

            out = factory.createArchiveOutputStream(archivename,
                    new FileOutputStream(result));

            final ChangeSet changes = new ChangeSet();
            changes.delete("bla");
            //archiveListDelete("bla");

            final ChangeSetPerformer performer = new ChangeSetPerformer(changes);
            performer.perform(ais, out);
            is.close();

        } finally {
            if (out != null) {
                out.close();
            }
            if (ais != null) {
                ais.close();
            }
        }

        this.checkArchiveContent(result, archiveList);
    }

    /**
     * Tries to delete and then add a file with the same name.
     * Should delete test/test3.xml and adds test.txt with the name
     * test/test3.xml
     *
     * @throws Exception
     */
    @Test
    public void testDeletePlusAddSame() throws Exception {
        final String archivename = "zip";
        final File input = this.createArchive(archivename);

        ArchiveOutputStream out = null;
        ArchiveInputStream ais = null;
        final File result = File.createTempFile("test", "."+archivename);
        result.deleteOnExit();

        File testtxt = null;
        try {

            final InputStream is = new FileInputStream(input);
            ais = factory.createArchiveInputStream(archivename, is);
            out = factory.createArchiveOutputStream(archivename,
                    new FileOutputStream(result));

            final ChangeSet changes = new ChangeSet();
            changes.delete("test/test3.xml");
            archiveListDelete("test/test3.xml");

            // Add a file
            testtxt = getFile("test.txt");
            final ArchiveEntry entry = out.createArchiveEntry(testtxt, "test/test3.xml");
            changes.add(entry, new FileInputStream(testtxt));
            archiveList.add("test/test3.xml");

            final ChangeSetPerformer performer = new ChangeSetPerformer(changes);
            performer.perform(ais, out);
            is.close();

        } finally {
            if (out != null) {
                out.close();
            }
            if (ais != null) {
                ais.close();
            }
        }

        // Checks
        ArchiveInputStream in = null;
        File check = null;
        try {
            final InputStream is = new FileInputStream(result);
            final BufferedInputStream buf = new BufferedInputStream(is);
            in = factory.createArchiveInputStream(buf);
            check = this.checkArchiveContent(in, archiveList, false);
            final File test3xml = new File(check,"result/test/test3.xml");
            assertEquals(testtxt.length(), test3xml.length());

            final BufferedReader reader = new BufferedReader(new FileReader(test3xml));
            String str;
            while ((str = reader.readLine()) != null) {
                // All lines look like this
                "111111111111111111111111111000101011".equals(str);
            }
            reader.close();
        } finally {
            if (in != null) {
                in.close();
            }
            rmdir(check);
        }
    }

    /**
     * Checks for the correct ChangeSetResults
     *
     * @throws Exception
     */
    @Test
    public void testChangeSetResults() throws Exception {
        final String archivename = "cpio";
        final File input = this.createArchive(archivename);

        ArchiveOutputStream out = null;
        ArchiveInputStream ais = null;
        final File result = File.createTempFile("test", "."+archivename);
        result.deleteOnExit();
        try {

            final InputStream is = new FileInputStream(input);
            ais = factory.createArchiveInputStream(archivename, is);
            out = factory.createArchiveOutputStream(archivename,
                    new FileOutputStream(result));

            final ChangeSet changes = new ChangeSet();
            changes.deleteDir("bla");
            archiveListDeleteDir("bla");

            // Add a file
            final File file1 = getFile("test.txt");
            final ArchiveEntry entry = out.createArchiveEntry(file1, "bla/test.txt");
            changes.add(entry, new FileInputStream(file1));
            archiveList.add("bla/test.txt");

            final ChangeSetPerformer performer = new ChangeSetPerformer(changes);
            final ChangeSetResults results = performer.perform(ais, out);
            is.close();

            // Checks
            assertEquals(1,results.getAddedFromChangeSet().size());
            assertEquals("bla/test.txt",results.getAddedFromChangeSet().iterator().next());
            assertEquals(3,results.getDeleted().size());
            assertTrue(results.getDeleted().contains("bla/test4.xml"));
            assertTrue(results.getDeleted().contains("bla/test5.xml"));
            assertTrue(results.getDeleted().contains("bla/blubber/test6.xml"));

            assertTrue(results.getAddedFromStream().contains("testdata/test1.xml"));
            assertTrue(results.getAddedFromStream().contains("testdata/test2.xml"));
            assertTrue(results.getAddedFromStream().contains("test/test3.xml"));
            assertTrue(results.getAddedFromStream().contains("test.txt"));
            assertTrue(results.getAddedFromStream().contains("something/bla"));
            assertTrue(results.getAddedFromStream().contains("test with spaces.txt"));
            assertEquals(6,results.getAddedFromStream().size());
        } finally {
            if (out != null) {
                out.close();
            }
            if (ais != null) {
                ais.close();
            }
        }

        this.checkArchiveContent(result, archiveList);
    }

    /**
     * Tries to delete a directory with a file and adds a new directory with a
     * new file and with the same name. Should delete dir1/* and add
     * dir1/test.txt at the end
     *
     * @throws Exception
     */
    @Test
    public void testDeletePlusAdd() throws Exception {
        final String archivename = "cpio";
        final File input = this.createArchive(archivename);

        ArchiveOutputStream out = null;
        ArchiveInputStream ais = null;
        final File result = File.createTempFile("test", "."+archivename);
        result.deleteOnExit();
        try {

            final InputStream is = new FileInputStream(input);
            ais = factory.createArchiveInputStream(archivename, is);
            out = factory.createArchiveOutputStream(archivename,
                    new FileOutputStream(result));

            final ChangeSet changes = new ChangeSet();
            changes.deleteDir("bla");
            archiveListDeleteDir("bla");

            // Add a file
            final File file1 = getFile("test.txt");
            final ArchiveEntry entry = out.createArchiveEntry(file1, "bla/test.txt");
            changes.add(entry, new FileInputStream(file1));
            archiveList.add("bla/test.txt");

            final ChangeSetPerformer performer = new ChangeSetPerformer(changes);
            performer.perform(ais, out);
            is.close();

        } finally {
            if (out != null) {
                out.close();
            }
            if (ais != null) {
                ais.close();
            }
        }

        this.checkArchiveContent(result, archiveList);
    }

    /**
     * Adds a file to a zip archive. Deletes an other file.
     *
     * @throws Exception
     */
    @Test
    public void testDeleteFromAndAddToZip() throws Exception {
        final String archivename = "zip";
        final File input = this.createArchive(archivename);

        ArchiveOutputStream out = null;
        ArchiveInputStream ais = null;
        final File result = File.createTempFile("test", "."+archivename);
        result.deleteOnExit();
        try {

            final InputStream is = new FileInputStream(input);
            ais = factory.createArchiveInputStream(archivename, is);
            out = factory.createArchiveOutputStream(archivename,
                    new FileOutputStream(result));

            final ChangeSet changes = new ChangeSet();

            final File file1 = getFile("test.txt");
            final ArchiveEntry entry = new ZipArchiveEntry("blub/test.txt");
            changes.add(entry, new FileInputStream(file1));
            archiveList.add("blub/test.txt");

            changes.delete("testdata/test1.xml");
            archiveListDelete("testdata/test1.xml");

            final ChangeSetPerformer performer = new ChangeSetPerformer(changes);
            performer.perform(ais, out);
            is.close();

        } finally {
            if (out != null) {
                out.close();
            }
            if (ais != null) {
                ais.close();
            }
        }

        this.checkArchiveContent(result, archiveList);
    }

    /**
     * Adds a file to a zip archive. Deletes an other file.
     *
     * @throws Exception
     */
    @Test
    public void testDeleteFromAndAddToZipUsingZipFilePerform() throws Exception {
        final String archivename = "zip";
        final File input = this.createArchive(archivename);

        ArchiveOutputStream out = null;
        ZipFile ais = null;
        final File result = File.createTempFile("test", "."+archivename);
        result.deleteOnExit();
        try {

            ais = new ZipFile(input);
            out = factory.createArchiveOutputStream(archivename,
                    new FileOutputStream(result));

            final ChangeSet changes = new ChangeSet();

            final File file1 = getFile("test.txt");
            final ArchiveEntry entry = new ZipArchiveEntry("blub/test.txt");
            changes.add(entry, new FileInputStream(file1));
            archiveList.add("blub/test.txt");

            changes.delete("testdata/test1.xml");
            archiveListDelete("testdata/test1.xml");

            final ChangeSetPerformer performer = new ChangeSetPerformer(changes);
            performer.perform(ais, out);

        } finally {
            if (out != null) {
                out.close();
            }
            if (ais != null) {
                ais.close();
            }
        }

        this.checkArchiveContent(result, archiveList);
    }

    /**
     * add blub/test.txt + delete blub Should add blub/test.txt and delete it
     * afterwards. In this example, the archive should stay untouched.
     *
     * @throws Exception
     */
    @Test
    public void testAddDeleteAdd() throws Exception {
        final String archivename = "cpio";
        final File input = this.createArchive(archivename);

        ArchiveOutputStream out = null;
        ArchiveInputStream ais = null;
        final File result = File.createTempFile("test", "."+archivename);
        result.deleteOnExit();
        try {

            final InputStream is = new FileInputStream(input);
            ais = factory.createArchiveInputStream(archivename, is);
            out = factory.createArchiveOutputStream(archivename,
                    new FileOutputStream(result));

            final ChangeSet changes = new ChangeSet();

            final File file1 = getFile("test.txt");
            final ArchiveEntry entry = new CpioArchiveEntry("blub/test.txt");
            changes.add(entry, new FileInputStream(file1));
            archiveList.add("blub/test.txt");

            changes.deleteDir("blub");
            archiveListDeleteDir("blub");

            final ChangeSetPerformer performer = new ChangeSetPerformer(changes);
            performer.perform(ais, out);

            is.close();

        } finally {
            if (out != null) {
                out.close();
            }
            if (ais != null) {
                ais.close();
            }
        }

        this.checkArchiveContent(result, archiveList);
    }

    /**
     * delete bla + add bla/test.txt + delete bla Deletes dir1/* first, then
     * suppresses the add of bla.txt because there is a delete operation later.
     *
     * @throws Exception
     */
    @Test
    public void testDeleteAddDelete() throws Exception {
        final String archivename = "cpio";
        final File input = this.createArchive(archivename);

        ArchiveOutputStream out = null;
        ArchiveInputStream ais = null;
        final File result = File.createTempFile("test", "."+archivename);
        result.deleteOnExit();
        try {

            final InputStream is = new FileInputStream(input);
            ais = factory.createArchiveInputStream(archivename, is);
            out = factory.createArchiveOutputStream(archivename,
                    new FileOutputStream(result));

            final ChangeSet changes = new ChangeSet();

            changes.deleteDir("bla");

            final File file1 = getFile("test.txt");
            final ArchiveEntry entry = new CpioArchiveEntry("bla/test.txt");
            changes.add(entry, new FileInputStream(file1));
            archiveList.add("bla/test.txt");

            changes.deleteDir("bla");
            archiveListDeleteDir("bla");

            final ChangeSetPerformer performer = new ChangeSetPerformer(changes);
            performer.perform(ais, out);

            is.close();

        } finally {
            if (out != null) {
                out.close();
            }
            if (ais != null) {
                ais.close();
            }
        }

        this.checkArchiveContent(result, archiveList);
    }

    /**
     * Simple Delete from a zip file.
     *
     * @throws Exception
     */
    @Test
    public void testDeleteFromZip() throws Exception {
        ArchiveOutputStream out = null;
        ArchiveInputStream ais = null;
        File temp = null;
        try {
            final ChangeSet changes = new ChangeSet();
            changes.delete("test2.xml");

            final File input = getFile("bla.zip");
            final InputStream is = new FileInputStream(input);
            ais = factory.createArchiveInputStream("zip", is);

            temp = File.createTempFile("test", ".zip");
            temp.deleteOnExit();
            out = factory.createArchiveOutputStream("zip",
                    new FileOutputStream(temp));

            final ChangeSetPerformer performer = new ChangeSetPerformer(changes);
            performer.perform(ais, out);

        } finally {
            if (out != null) {
                out.close();
            }
            if (ais != null) {
                ais.close();
            }
        }

        final List<String> expected = new ArrayList<>();
        expected.add("test1.xml");

        this.checkArchiveContent(temp, expected);
    }

    /**
     * Simple delete from a tar file
     *
     * @throws Exception
     */
    @Test
    public void testDeleteFromTar() throws Exception {
        ArchiveOutputStream out = null;
        ArchiveInputStream ais = null;
        File temp = null;
        try {
            final ChangeSet changes = new ChangeSet();
            changes.delete("test2.xml");

            final File input = getFile("bla.tar");
            final InputStream is = new FileInputStream(input);
            ais = factory.createArchiveInputStream("tar", is);

            temp = new File(dir, "bla.tar");
            out = factory.createArchiveOutputStream("tar",
                    new FileOutputStream(temp));

            final ChangeSetPerformer performer = new ChangeSetPerformer(changes);
            performer.perform(ais, out);

        } finally {
            if (out != null) {
                out.close();
            }
            if (ais != null) {
                ais.close();
            }
        }
        final List<String> expected = new ArrayList<>();
        expected.add("test1.xml");
        this.checkArchiveContent(temp, expected);
    }

    /**
     * Simple delete from a jar file
     *
     * @throws Exception
     */
    @Test
    public void testDeleteFromJar() throws Exception {
        ArchiveOutputStream out = null;
        ArchiveInputStream ais = null;
        File temp = null;
        try {
            final ChangeSet changes = new ChangeSet();
            changes.delete("test2.xml");
            changes.deleteDir("META-INF");
            changes.delete(".classpath");
            changes.delete(".project");

            final File input = getFile("bla.jar");
            final InputStream is = new FileInputStream(input);
            ais = factory.createArchiveInputStream("jar", is);

            temp = new File(dir, "bla.jar");
            out = factory.createArchiveOutputStream("jar",
                    new FileOutputStream(temp));

            final ChangeSetPerformer performer = new ChangeSetPerformer(changes);
            performer.perform(ais, out);

        } finally {
            if (out != null) {
                out.close();
            }
            if (ais != null) {
                ais.close();
            }
        }
        final List<String> expected = new ArrayList<>();
        expected.add("test1.xml");
        this.checkArchiveContent(temp, expected);
    }

    @Test
    public void testDeleteFromAndAddToTar() throws Exception {
        ArchiveOutputStream out = null;
        ArchiveInputStream ais = null;
        File temp = null;
        try {
            final ChangeSet changes = new ChangeSet();
            changes.delete("test2.xml");

            final File file1 = getFile("test.txt");

            final TarArchiveEntry entry = new TarArchiveEntry(
                    "testdata/test.txt");
            entry.setModTime(0);
            entry.setSize(file1.length());
            entry.setUserId(0);
            entry.setGroupId(0);
            entry.setUserName("avalon");
            entry.setGroupName("excalibur");
            entry.setMode(0100000);

            changes.add(entry, new FileInputStream(file1));

            final File input = getFile("bla.tar");
            final InputStream is = new FileInputStream(input);
            ais = factory.createArchiveInputStream("tar", is);

            temp = new File(dir, "bla.tar");
            out = factory.createArchiveOutputStream("tar",
                    new FileOutputStream(temp));

            final ChangeSetPerformer performer = new ChangeSetPerformer(changes);
            performer.perform(ais, out);

        } finally {
            if (out != null) {
                out.close();
            }
            if (ais != null) {
                ais.close();
            }
        }
        final List<String> expected = new ArrayList<>();
        expected.add("test1.xml");
        expected.add("testdata/test.txt");
        final ArchiveInputStream in = factory.createArchiveInputStream("tar", new FileInputStream(temp));
        this.checkArchiveContent(in, expected);
    }

    /**
     * Delete from a jar file and add another file
     *
     * @throws Exception
     */
    @Test
    public void testDeleteFromAndAddToJar() throws Exception {
        ArchiveOutputStream out = null;
        ArchiveInputStream ais = null;
        File temp = null;
        try {
            final ChangeSet changes = new ChangeSet();
            changes.delete("test2.xml");
            changes.deleteDir("META-INF");
            changes.delete(".classpath");
            changes.delete(".project");

            final File file1 = getFile("test.txt");
            final JarArchiveEntry entry = new JarArchiveEntry("testdata/test.txt");
            changes.add(entry, new FileInputStream(file1));

            final File input = getFile("bla.jar");
            final InputStream is = new FileInputStream(input);
            ais = factory.createArchiveInputStream("jar", is);

            temp = new File(dir, "bla.jar");
            out = factory.createArchiveOutputStream("jar",
                    new FileOutputStream(temp));

            final ChangeSetPerformer performer = new ChangeSetPerformer(changes);
            performer.perform(ais, out);

        } finally {
            if (out != null) {
                out.close();
            }
            if (ais != null) {
                ais.close();
            }
        }
        final List<String> expected = new ArrayList<>();
        expected.add("test1.xml");
        expected.add("testdata/test.txt");
        this.checkArchiveContent(temp, expected);
    }

    /**
     * Simple delete from an ar file
     *
     * @throws Exception
     */
    @Test
    public void testDeleteFromAr() throws Exception {
        ArchiveOutputStream out = null;
        ArchiveInputStream ais = null;
        File temp = null;
        try {
            final ChangeSet changes = new ChangeSet();
            changes.delete("test2.xml");

            final File input = getFile("bla.ar");
            final InputStream is = new FileInputStream(input);
            ais = factory.createArchiveInputStream("ar", is);

            temp = new File(dir, "bla.ar");
            out = factory.createArchiveOutputStream("ar",
                    new FileOutputStream(temp));

            final ChangeSetPerformer performer = new ChangeSetPerformer(changes);
            performer.perform(ais, out);

        } finally {
            if (out != null) {
                out.close();
            }
            if (ais != null) {
                ais.close();
            }
        }

        final List<String> expected = new ArrayList<>();
        expected.add("test1.xml");
        this.checkArchiveContent(temp, expected);
    }

    /**
     * Deletes a file from an AR-archive and adds another
     *
     * @throws Exception
     */
    @Test
    public void testDeleteFromAndAddToAr() throws Exception {
        ArchiveOutputStream out = null;
        ArchiveInputStream ais = null;
        File temp = null;
        try {
            final ChangeSet changes = new ChangeSet();
            changes.delete("test2.xml");

            final File file1 = getFile("test.txt");

            final ArArchiveEntry entry = new ArArchiveEntry("test.txt", file1
                    .length());

            changes.add(entry, new FileInputStream(file1));

            final File input = getFile("bla.ar");
            final InputStream is = new FileInputStream(input);
            ais = factory.createArchiveInputStream("ar", is);

            temp = new File(dir, "bla.ar");
            out = factory.createArchiveOutputStream("ar",
                    new FileOutputStream(temp));

            final ChangeSetPerformer performer = new ChangeSetPerformer(changes);
            performer.perform(ais, out);

        } finally {
            if (out != null) {
                out.close();
            }
            if (ais != null) {
                ais.close();
            }
        }
        final List<String> expected = new ArrayList<>();
        expected.add("test1.xml");
        expected.add("test.txt");
        this.checkArchiveContent(temp, expected);
    }

    /**
     * TODO: Move operations are not supported currently
     *
     * mv dir1/test.text dir2/test.txt + delete dir1 Moves the file to dir2 and
     * deletes everything in dir1
     *
     * @throws Exception
     */
    @Test
    public void testRenameAndDelete() throws Exception {
    }

    /**
     * TODO: Move operations are not supported currently
     *
     * add dir1/bla.txt + mv dir1/test.text dir2/test.txt + delete dir1
     *
     * Add dir1/bla.txt should be surpressed. All other dir1 files will be
     * deleted, except dir1/test.text will be moved
     *
     * @throws Exception
     */
    @Test
    public void testAddMoveDelete() throws Exception {
    }

    /**
     * Check can add a file to an empty archive.
     *
     * @throws Exception
     */
    @Test
    public void testAddToEmptyArchive() throws Exception {
        final String archivename = "zip";
        final File input = this.createEmptyArchive(archivename);

        ArchiveOutputStream out = null;
        ArchiveInputStream ais = null;
        InputStream is = null;
        final File result = File.createTempFile("test", "."+archivename);
        result.deleteOnExit();
        final ChangeSet changes = new ChangeSet();
        try {

            is = new FileInputStream(input);
            ais = factory.createArchiveInputStream(archivename, is);

            out = factory.createArchiveOutputStream(archivename,
                    new FileOutputStream(result));

            final File file1 = getFile("test.txt");
            final ArchiveEntry entry = new ZipArchiveEntry("bla/test.txt");
            changes.add(entry, new FileInputStream(file1));
            archiveList.add("bla/test.txt");
            final ChangeSetPerformer performer = new ChangeSetPerformer(changes);
            performer.perform(ais, out);
            is.close();

        } finally {
            if (out != null) {
                out.close();
            }
            if (ais != null) {
                ais.close(); // will close is
            } else if (is != null){
                is.close();
            }
        }

        this.checkArchiveContent(result, archiveList);
    }

    /**
     * Check can delete and add a file to an archive with a single file
     *
     * @throws Exception
     */
    @Test
    public void testDeleteAddToOneFileArchive() throws Exception {
        final String archivename = "zip";
        final File input = this.createSingleEntryArchive(archivename);

        ArchiveOutputStream out = null;
        ArchiveInputStream ais = null;
        InputStream is = null;
        final File result = File.createTempFile("test", "."+archivename);
        result.deleteOnExit();
        final ChangeSet changes = new ChangeSet();
        try {

            is = new FileInputStream(input);
            ais = factory.createArchiveInputStream(archivename, is);

            out = factory.createArchiveOutputStream(archivename,
                    new FileOutputStream(result));
            changes.delete("test1.xml");
            archiveListDelete("test1.xml");

            final File file = getFile("test.txt");
            final ArchiveEntry entry = out.createArchiveEntry(file,"bla/test.txt");
            changes.add(entry, new FileInputStream(file));
            archiveList.add("bla/test.txt");

            final ChangeSetPerformer performer = new ChangeSetPerformer(changes);
            performer.perform(ais, out);
            is.close();

        } finally {
            if (out != null) {
                out.close();
            }
            if (ais != null) {
                ais.close(); // will close is
            } else if (is != null){
                is.close();
            }
        }

        this.checkArchiveContent(result, archiveList);
    }

    /**
     * Check can add and delete a file to an archive with a single file
     *
     * @throws Exception
     */
    @Test
    public void testAddDeleteToOneFileArchive() throws Exception {
        final String archivename = "cpio";
        final File input = this.createSingleEntryArchive(archivename);

        ArchiveOutputStream out = null;
        ArchiveInputStream ais = null;
        InputStream is = null;
        final File result = File.createTempFile("test", "."+archivename);
        result.deleteOnExit();
        final ChangeSet changes = new ChangeSet();
        try {

            is = new FileInputStream(input);
            ais = factory.createArchiveInputStream(archivename, is);

            out = factory.createArchiveOutputStream(archivename,
                    new FileOutputStream(result));
            final File file = getFile("test.txt");
            final ArchiveEntry entry = out.createArchiveEntry(file,"bla/test.txt");
            changes.add(entry, new FileInputStream(file));
            archiveList.add("bla/test.txt");

            changes.delete("test1.xml");
            archiveListDelete("test1.xml");

            final ChangeSetPerformer performer = new ChangeSetPerformer(changes);
            performer.perform(ais, out);
            is.close();

        } finally {
            if (out != null) {
                out.close();
            }
            if (ais != null) {
                ais.close(); // will close is
            } else if (is != null){
                is.close();
            }
        }

        this.checkArchiveContent(result, archiveList);
    }

    /**
     * Adds a file with the same filename as an existing file from the stream.
     * Should lead to a replacement.
     *
     * @throws Exception
     */
    @Test
    public void testAddAllreadyExistingWithReplaceTrue() throws Exception {
        final String archivename = "zip";
        final File input = this.createArchive(archivename);

        ArchiveOutputStream out = null;
        ArchiveInputStream ais = null;
        final File result = File.createTempFile("test", "."+archivename);
        result.deleteOnExit();
        try {

            final InputStream is = new FileInputStream(input);
            ais = factory.createArchiveInputStream(archivename, is);
            out = factory.createArchiveOutputStream(archivename,
                    new FileOutputStream(result));

            final ChangeSet changes = new ChangeSet();

            final File file1 = getFile("test.txt");
            final ArchiveEntry entry = new ZipArchiveEntry("testdata/test1.xml");
            changes.add(entry, new FileInputStream(file1), true);

            final ChangeSetPerformer performer = new ChangeSetPerformer(changes);
            final ChangeSetResults results = performer.perform(ais, out);
            assertTrue(results.getAddedFromChangeSet().contains("testdata/test1.xml"));
            is.close();

        } finally {
            if (out != null) {
                out.close();
            }
            if (ais != null) {
                ais.close();
            }
        }

        this.checkArchiveContent(result, archiveList);
    }

    /**
     * Adds a file with the same filename as an existing file from the stream.
     * Should lead to a replacement.
     *
     * @throws Exception
     */
    @Test
    public void testAddAllreadyExistingWithReplaceFalse() throws Exception {
        final String archivename = "zip";
        final File input = this.createArchive(archivename);

        ArchiveOutputStream out = null;
        ArchiveInputStream ais = null;
        final File result = File.createTempFile("test", "."+archivename);
        result.deleteOnExit();
        try {

            final InputStream is = new FileInputStream(input);
            ais = factory.createArchiveInputStream(archivename, is);
            out = factory.createArchiveOutputStream(archivename,
                    new FileOutputStream(result));

            final ChangeSet changes = new ChangeSet();

            final File file1 = getFile("test.txt");
            final ArchiveEntry entry = new ZipArchiveEntry("testdata/test1.xml");
            changes.add(entry, new FileInputStream(file1), false);

            final ChangeSetPerformer performer = new ChangeSetPerformer(changes);
            final ChangeSetResults results = performer.perform(ais, out);
            assertTrue(results.getAddedFromStream().contains("testdata/test1.xml"));
            assertTrue(results.getAddedFromChangeSet().isEmpty());
            assertTrue(results.getDeleted().isEmpty());
            is.close();

        } finally {
            if (out != null) {
                out.close();
            }
            if (ais != null) {
                ais.close();
            }
        }

        this.checkArchiveContent(result, archiveList);
    }

}
