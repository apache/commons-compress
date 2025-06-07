/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.compress.changes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests {@link ChangeSet} using raw generics for public code.
 *
 * @see ChangeSetSafeTypesTest
 */
public final class ChangeSetRawTypesTest extends AbstractTest {

    // Delete a single file
    private void archiveListDelete(final String prefix) {
        archiveList.removeIf(entry -> entry.equals(prefix));
    }

    // Delete a directory tree
    private void archiveListDeleteDir(final String prefix) {
        // TODO won't work with folders
        archiveList.removeIf(entry -> entry.startsWith(prefix + "/"));
    }

    /**
     * Adds a file with the same file name as an existing file from the stream. Should lead to a replacement.
     *
     * @param archiverName archiver name.
     * @throws Exception Thrown on test failure. Thrown on test failure.
     */
    @ParameterizedTest
    @MethodSource("org.apache.commons.compress.changes.TestFixtures#getOutputArchiveNames")
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void testAddAlreadyExistingWithReplaceFalse(final String archiverName) throws Exception {
        final Path inputPath = createArchive(archiverName);
        final Path result = Files.createTempFile("test", "." + archiverName);
        try (InputStream inputStream = Files.newInputStream(inputPath);
                ArchiveInputStream archiveInputStream = factory.createArchiveInputStream(archiverName, inputStream);
                OutputStream newOutputStream = Files.newOutputStream(result);
                ArchiveOutputStream archiveOutputStream = factory.createArchiveOutputStream(archiverName, newOutputStream);
                InputStream csInputStream = Files.newInputStream(getPath("test.txt"));) {
            setLongFileMode(archiveOutputStream);
            final ChangeSet changeSet = new ChangeSet();
            final ArchiveEntry entry = new ZipArchiveEntry("testdata/test1.xml");
            changeSet.add(entry, csInputStream, false);
            final ChangeSetResults results = new ChangeSetPerformer(changeSet).perform(archiveInputStream, archiveOutputStream);
            assertTrue(results.getAddedFromStream().contains("testdata/test1.xml"));
            assertTrue(results.getAddedFromChangeSet().isEmpty());
            assertTrue(results.getDeleted().isEmpty());
        } finally {
            checkArchiveContent(result, archiveList);
            forceDelete(inputPath);
            forceDelete(result);
        }
    }

    /**
     * Adds a file with the same file name as an existing file from the stream. Should lead to a replacement.
     *
     * @param archiverName archiver name.
     * @throws Exception Thrown on test failure. Thrown on test failure.
     */
    @ParameterizedTest
    @MethodSource("org.apache.commons.compress.changes.TestFixtures#getZipOutputArchiveNames")
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void testAddAlreadyExistingWithReplaceTrue(final String archiverName) throws Exception {
        final Path inputPath = createArchive(archiverName);
        final Path result = Files.createTempFile("test", "." + archiverName);
        try (InputStream inputStream = Files.newInputStream(inputPath);
                ArchiveInputStream archiveInputStream = factory.createArchiveInputStream(archiverName, inputStream);
                OutputStream newOutputStream = Files.newOutputStream(result);
                ArchiveOutputStream archiveOutputStream = factory.createArchiveOutputStream(archiverName, newOutputStream);
                InputStream csInputStream = Files.newInputStream(getPath("test.txt"))) {
            setLongFileMode(archiveOutputStream);
            final ChangeSet changeSet = new ChangeSet();
            final ArchiveEntry entry = new ZipArchiveEntry("testdata/test1.xml");
            changeSet.add(entry, csInputStream, true);
            final ChangeSetResults results = new ChangeSetPerformer(changeSet).perform(archiveInputStream, archiveOutputStream);
            assertTrue(results.getAddedFromChangeSet().contains("testdata/test1.xml"));
        } finally {
            checkArchiveContent(result, archiveList);
            forceDelete(result);
        }
    }

    /**
     * Adds an ArchiveEntry with the same name two times. Only the latest addition should be found in the ChangeSet, the first add should be replaced.
     *
     * @throws Exception Thrown on test failure. Thrown on test failure.
     */
    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void testAddChangeTwice() throws Exception {
        try (InputStream inputStream = newInputStream("test.txt");
                InputStream inputStream2 = newInputStream("test2.xml")) {
            final ArchiveEntry e = new ZipArchiveEntry("test.txt");
            final ArchiveEntry e2 = new ZipArchiveEntry("test.txt");
            final ChangeSet changeSet = new ChangeSet();
            changeSet.add(e, inputStream);
            changeSet.add(e2, inputStream2);
            final Set<Change> changeSet2 = changeSet.getChanges();
            assertEquals(1, changeSet2.size());
            final Change change = changeSet2.iterator().next();
            @SuppressWarnings("resource") // Not allocated here
            final InputStream csInputStream = change.getInputStream();
            assertEquals(inputStream2, csInputStream);
        }
    }

    /**
     * Adds an ArchiveEntry with the same name two times. Only the first addition should be found in the ChangeSet, the second add should never be added since
     * replace = false
     *
     * @throws Exception Thrown on test failure. Thrown on test failure.
     */
    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void testAddChangeTwiceWithoutReplace() throws Exception {
        try (InputStream inputStream = newInputStream("test.txt");
                InputStream inputStream2 = newInputStream("test2.xml")) {
            final ArchiveEntry e = new ZipArchiveEntry("test.txt");
            final ArchiveEntry e2 = new ZipArchiveEntry("test.txt");
            final ChangeSet changeSet = new ChangeSet();
            changeSet.add(e, inputStream, true);
            changeSet.add(e2, inputStream2, false);
            final Set<Change> changes = changeSet.getChanges();
            assertEquals(1, changes.size());
            final Change c = changes.iterator().next();
            @SuppressWarnings("resource")
            final InputStream csInputStream = c.getInputStream();
            assertEquals(inputStream, csInputStream);
        }
    }

    /**
     * add blub/test.txt + delete blub Should add blub/test.txt and delete it afterwards. In this example, the archive should stay untouched.
     *
     * @throws Exception Thrown on test failure. Thrown on test failure.
     */
    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void testAddDeleteAdd() throws Exception {
        final String archiverName = "cpio";
        final Path inputPath = createArchive(archiverName);
        final Path result = Files.createTempFile("test", "." + archiverName);
        try (InputStream inputStream = Files.newInputStream(inputPath);
                ArchiveInputStream archiveInputStream = factory.createArchiveInputStream(archiverName, inputStream);
                OutputStream newOutputStream = Files.newOutputStream(result);
                ArchiveOutputStream archiveOutputStream = factory.createArchiveOutputStream(archiverName, newOutputStream);
                InputStream csInputStream = Files.newInputStream(getPath("test.txt"))) {
            setLongFileMode(archiveOutputStream);
            final ChangeSet changeSet = new ChangeSet();
            final ArchiveEntry entry = new CpioArchiveEntry("blub/test.txt");
            changeSet.add(entry, csInputStream);
            archiveList.add("blub/test.txt");
            changeSet.deleteDir("blub");
            archiveListDeleteDir("blub");
            new ChangeSetPerformer(changeSet).perform(archiveInputStream, archiveOutputStream);
        } finally {
            checkArchiveContent(result, archiveList);
            forceDelete(result);
        }

    }

    /**
     * Check can add and delete a file to an archive with a single file
     *
     * @param archiverName archiver name.
     * @throws Exception Thrown on test failure. Thrown on test failure.
     */
    @ParameterizedTest
    @MethodSource("org.apache.commons.compress.changes.TestFixtures#getOutputArchiveNames")
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void testAddDeleteToOneFileArchive(final String archiverName) throws Exception {
        final Path inputPath = createSingleEntryArchive(archiverName);
        final Path result = Files.createTempFile("test", "." + archiverName);
        final ChangeSet changeSet = new ChangeSet();
        final File file = getFile("test.txt");
        try (InputStream inputStream = Files.newInputStream(inputPath);
                ArchiveInputStream archiveInputStream = factory.createArchiveInputStream(archiverName, inputStream);
                OutputStream newOutputStream = Files.newOutputStream(result);
                ArchiveOutputStream archiveOutputStream = factory.createArchiveOutputStream(archiverName, newOutputStream);
                InputStream csInputStream = Files.newInputStream(file.toPath())) {
            setLongFileMode(archiveOutputStream);
            final ArchiveEntry entry = archiveOutputStream.createArchiveEntry(file, "bla/test.txt");
            changeSet.add(entry, csInputStream);
            archiveList.add("bla/test.txt");
            changeSet.delete("test1.xml");
            archiveListDelete("test1.xml");
            new ChangeSetPerformer(changeSet).perform(archiveInputStream, archiveOutputStream);
        } finally {
            checkArchiveContent(result, archiveList);
            forceDelete(result);
        }
    }

    /**
     * TODO: Move operations are not supported currently
     *
     * add dir1/bla.txt + mv dir1/test.text dir2/test.txt + delete dir1
     *
     * Add dir1/bla.txt should be suppressed. All other dir1 files will be deleted, except dir1/test.text will be moved
     *
     * @throws Exception Thrown on test failure. Thrown on test failure.
     */
    @Test
    void testAddMoveDelete() throws Exception {
    }

    /**
     * Check can add a file to an empty archive.
     *
     * @param archiverName archiver name.
     * @throws Exception Thrown on test failure. Thrown on test failure.
     */
    @ParameterizedTest
    @MethodSource("org.apache.commons.compress.changes.TestFixtures#getEmptyOutputArchiveNames")
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void testAddToEmptyArchive(final String archiverName) throws Exception {
        final Path inputPath = createEmptyArchive(archiverName);
        final Path result = Files.createTempFile("test", "." + archiverName);
        final ChangeSet changeSet = new ChangeSet();
        try (InputStream inputStream = Files.newInputStream(inputPath);
                ArchiveInputStream archiveInputStream = factory.createArchiveInputStream(archiverName, inputStream);
                OutputStream newOutputStream = Files.newOutputStream(result);
                ArchiveOutputStream archiveOutputStream = factory.createArchiveOutputStream(archiverName, newOutputStream);
                InputStream csInputStream = Files.newInputStream(getPath("test.txt"))) {
            setLongFileMode(archiveOutputStream);
            final ArchiveEntry entry = new ZipArchiveEntry("bla/test.txt");
            changeSet.add(entry, csInputStream);
            archiveList.add("bla/test.txt");
            new ChangeSetPerformer(changeSet).perform(archiveInputStream, archiveOutputStream);
        } finally {
            checkArchiveContent(result, archiveList);
            forceDelete(result);
        }
    }

    /**
     * Checks for the correct ChangeSetResults
     *
     * @param archiverName archiver name.
     * @throws Exception Thrown on test failure. Thrown on test failure.
     */
    @ParameterizedTest
    @MethodSource("org.apache.commons.compress.changes.TestFixtures#getOutputArchiveNames")
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void testChangeSetResults(final String archiverName) throws Exception {
        final Path inputPath = createArchive(archiverName);
        final Path result = Files.createTempFile("test", "." + archiverName);
        final File file1 = getFile("test.txt");
        try (InputStream inputStream = Files.newInputStream(inputPath);
                ArchiveInputStream archiveInputStream = factory.createArchiveInputStream(archiverName, inputStream);
                OutputStream newOutputStream = Files.newOutputStream(result);
                ArchiveOutputStream archiveOutputStream = factory.createArchiveOutputStream(archiverName, newOutputStream);
                InputStream csInputStream = Files.newInputStream(file1.toPath())) {
            setLongFileMode(archiveOutputStream);
            final ChangeSet changeSet = new ChangeSet();
            changeSet.deleteDir("bla");
            archiveListDeleteDir("bla");
            // Add a file
            final ArchiveEntry entry = archiveOutputStream.createArchiveEntry(file1, "bla/test.txt");
            changeSet.add(entry, csInputStream);
            archiveList.add("bla/test.txt");
            final ChangeSetResults results = new ChangeSetPerformer(changeSet).perform(archiveInputStream, archiveOutputStream);
            inputStream.close();
            // Checks
            assertEquals(1, results.getAddedFromChangeSet().size());
            assertEquals("bla/test.txt", results.getAddedFromChangeSet().iterator().next());
            assertEquals(3, results.getDeleted().size());
            assertTrue(results.getDeleted().contains("bla/test4.xml"));
            assertTrue(results.getDeleted().contains("bla/test5.xml"));
            assertTrue(results.getDeleted().contains("bla/blubber/test6.xml"));

            assertTrue(results.getAddedFromStream().contains("testdata/test1.xml"));
            assertTrue(results.getAddedFromStream().contains("testdata/test2.xml"));
            assertTrue(results.getAddedFromStream().contains("test/test3.xml"));
            assertTrue(results.getAddedFromStream().contains("test.txt"));
            assertTrue(results.getAddedFromStream().contains("something/bla"));
            assertTrue(results.getAddedFromStream().contains("test with spaces.txt"));
            assertEquals(6, results.getAddedFromStream().size());
        } finally {
            checkArchiveContent(result, archiveList);
            forceDelete(result);
        }

    }

    /**
     * delete bla + add bla/test.txt + delete bla Deletes dir1/* first, then suppresses the add of bla.txt because there is a delete operation later.
     *
     * @throws Exception Thrown on test failure. Thrown on test failure.
     */
    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void testDeleteAddDelete() throws Exception {
        final String archiverName = "cpio";
        final Path inputPath = createArchive(archiverName);
        final Path result = Files.createTempFile("test", "." + archiverName);
        try (InputStream inputStream = Files.newInputStream(inputPath);
                ArchiveInputStream archiveInputStream = factory.createArchiveInputStream(archiverName, inputStream);
                OutputStream newOutputStream = Files.newOutputStream(result);
                ArchiveOutputStream archiveOutputStream = factory.createArchiveOutputStream(archiverName, newOutputStream);
                InputStream csInputStream = Files.newInputStream(getPath("test.txt"))) {
            setLongFileMode(archiveOutputStream);
            final ChangeSet changeSet = new ChangeSet();
            changeSet.deleteDir("bla");
            final ArchiveEntry entry = new CpioArchiveEntry("bla/test.txt");
            changeSet.add(entry, csInputStream);
            archiveList.add("bla/test.txt");
            changeSet.deleteDir("bla");
            archiveListDeleteDir("bla");
            new ChangeSetPerformer(changeSet).perform(archiveInputStream, archiveOutputStream);
        } finally {
            checkArchiveContent(result, archiveList);
            forceDelete(result);
        }

    }

    /**
     * Check can delete and add a file to an archive with a single file
     *
     * @param archiverName archiver name.
     * @throws Exception Thrown on test failure. Thrown on test failure.
     */
    @ParameterizedTest
    @MethodSource("org.apache.commons.compress.changes.TestFixtures#getOutputArchiveNames")
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void testDeleteAddToOneFileArchive(final String archiverName) throws Exception {
        final Path inputPath = createSingleEntryArchive(archiverName);
        final Path result = Files.createTempFile("test", "." + archiverName);
        final ChangeSet changeSet = new ChangeSet();
        final File file = getFile("test.txt");
        try (InputStream inputStream = Files.newInputStream(inputPath);
                ArchiveInputStream archiveInputStream = factory.createArchiveInputStream(archiverName, inputStream);
                OutputStream newOutputStream = Files.newOutputStream(result);
                ArchiveOutputStream archiveOutputStream = factory.createArchiveOutputStream(archiverName, newOutputStream);
                InputStream csInputStream = Files.newInputStream(file.toPath())) {
            setLongFileMode(archiveOutputStream);
            changeSet.delete("test1.xml");
            archiveListDelete("test1.xml");
            final ArchiveEntry entry = archiveOutputStream.createArchiveEntry(file, "bla/test.txt");
            changeSet.add(entry, csInputStream);
            archiveList.add("bla/test.txt");
            new ChangeSetPerformer(changeSet).perform(archiveInputStream, archiveOutputStream);
        } finally {
            checkArchiveContent(result, archiveList);
            forceDelete(result);
        }

    }

    /**
     * Tries to delete the folder "bla" from an archive file. This should result in the deletion of bla/*, which actually means bla/test4.xml should be removed
     * from the archive. The file something/bla (without ending, named like the folder) should not be deleted.
     *
     * @param archiverName archiver name.
     * @throws Exception Thrown on test failure. Thrown on test failure.
     */
    @ParameterizedTest
    @MethodSource("org.apache.commons.compress.changes.TestFixtures#getOutputArchiveNames")
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void testDeleteDir(final String archiverName) throws Exception {
        final Path inputPath = createArchive(archiverName);
        final Path result = Files.createTempFile("test", "." + archiverName);
        try (InputStream inputStream = Files.newInputStream(inputPath);
                ArchiveInputStream archiveInputStream = factory.createArchiveInputStream(archiverName, inputStream);
                OutputStream newOutputStream = Files.newOutputStream(result);
                ArchiveOutputStream archiveOutputStream = factory.createArchiveOutputStream(archiverName, newOutputStream)) {
            setLongFileMode(archiveOutputStream);
            final ChangeSet changeSet = new ChangeSet();
            changeSet.deleteDir("bla");
            archiveListDeleteDir("bla");
            new ChangeSetPerformer(changeSet).perform(archiveInputStream, archiveOutputStream);
        } finally {
            checkArchiveContent(result, archiveList);
            forceDelete(result);
        }
    }

    /**
     * Tries to delete the folder "la" from an archive file. This should result in the deletion of la/*, which should not match any files/folders.
     *
     * @param archiverName archiver name.
     * @throws Exception Thrown on test failure. Thrown on test failure.
     */
    @ParameterizedTest
    @MethodSource("org.apache.commons.compress.changes.TestFixtures#getOutputArchiveNames")
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void testDeleteDir2(final String archiverName) throws Exception {
        final Path inputPath = createArchive(archiverName);
        final Path result = Files.createTempFile("test", "." + archiverName);
        try (InputStream inputStream = Files.newInputStream(inputPath);
                ArchiveInputStream archiveInputStream = factory.createArchiveInputStream(archiverName, inputStream);
                OutputStream newOutputStream = Files.newOutputStream(result);
                ArchiveOutputStream archiveOutputStream = factory.createArchiveOutputStream(archiverName, newOutputStream)) {
            setLongFileMode(archiveOutputStream);
            final ChangeSet changeSet = new ChangeSet();
            changeSet.deleteDir("la");
            archiveListDeleteDir("la");
            new ChangeSetPerformer(changeSet).perform(archiveInputStream, archiveOutputStream);
        } finally {
            checkArchiveContent(result, archiveList);
            forceDelete(result);
        }
    }

    /**
     * Tries to delete the folder "test.txt" from an archive file. This should not match any files/folders.
     *
     * @param archiverName archiver name.
     * @throws Exception Thrown on test failure. Thrown on test failure.
     */
    @ParameterizedTest
    @MethodSource("org.apache.commons.compress.changes.TestFixtures#getOutputArchiveNames")
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void testDeleteDir3(final String archiverName) throws Exception {
        final Path inputPath = createArchive(archiverName);
        final Path result = Files.createTempFile("test", "." + archiverName);
        try (InputStream inputStream = Files.newInputStream(inputPath);
                ArchiveInputStream archiveInputStream = factory.createArchiveInputStream(archiverName, inputStream);
                OutputStream newOutputStream = Files.newOutputStream(result);
                ArchiveOutputStream archiveOutputStream = factory.createArchiveOutputStream(archiverName, newOutputStream)) {
            setLongFileMode(archiveOutputStream);
            final ChangeSet changeSet = new ChangeSet();
            changeSet.deleteDir("test.txt");
            archiveListDeleteDir("test.txt");
            new ChangeSetPerformer(changeSet).perform(archiveInputStream, archiveOutputStream);
        } finally {
            checkArchiveContent(result, archiveList);
            forceDelete(result);
        }
    }

    /**
     * Tries to delete the file "bla/test5.xml" from an archive. This should result in the deletion of "bla/test5.xml".
     *
     * @param archiverName archiver name.
     * @throws Exception Thrown on test failure. Thrown on test failure.
     */
    @ParameterizedTest
    @MethodSource("org.apache.commons.compress.changes.TestFixtures#getOutputArchiveNames")
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void testDeleteFile(final String archiverName) throws Exception {
        final Path inputPath = createArchive(archiverName);
        final Path result = Files.createTempFile("test", "." + archiverName);
        try (InputStream inputStream = Files.newInputStream(inputPath);
                ArchiveInputStream archiveInputStream = factory.createArchiveInputStream(archiverName, inputStream);
                OutputStream newOutputStream = Files.newOutputStream(result);
                ArchiveOutputStream archiveOutputStream = factory.createArchiveOutputStream(archiverName, newOutputStream)) {
            setLongFileMode(archiveOutputStream);
            final ChangeSet changeSet = new ChangeSet();
            changeSet.delete("bla/test5.xml");
            archiveListDelete("bla/test5.xml");
            new ChangeSetPerformer(changeSet).perform(archiveInputStream, archiveOutputStream);
        } finally {
            checkArchiveContent(result, archiveList);
            forceDelete(result);
        }
    }

    /**
     * Tries to delete the file "bla" from an archive. This should result in the deletion of nothing.
     *
     * @param archiverName archiver name.
     * @throws Exception Thrown on test failure. Thrown on test failure.
     */
    @ParameterizedTest
    @MethodSource("org.apache.commons.compress.changes.TestFixtures#getOutputArchiveNames")
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void testDeleteFile2(final String archiverName) throws Exception {
        final Path inputPath = createArchive(archiverName);
        final Path result = Files.createTempFile("test", "." + archiverName);
        try (InputStream inputStream = Files.newInputStream(inputPath);
                ArchiveInputStream archiveInputStream = factory.createArchiveInputStream(archiverName, inputStream);
                OutputStream newOutputStream = Files.newOutputStream(result);
                ArchiveOutputStream archiveOutputStream = factory.createArchiveOutputStream(archiverName, newOutputStream)) {
            setLongFileMode(archiveOutputStream);
            final ChangeSet changeSet = new ChangeSet();
            changeSet.delete("bla");
            // archiveListDelete("bla");
            new ChangeSetPerformer(changeSet).perform(archiveInputStream, archiveOutputStream);
        } finally {
            checkArchiveContent(result, archiveList);
            forceDelete(result);
        }
    }

    /**
     * Deletes a file from an AR-archive and adds another
     *
     * @throws Exception Thrown on test failure. Thrown on test failure.
     */
    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void testDeleteFromAndAddToAr() throws Exception {
        final ChangeSet changeSet = new ChangeSet();
        changeSet.delete("test2.xml");
        final File file1 = getFile("test.txt");
        final ArArchiveEntry entry = new ArArchiveEntry("test.txt", file1.length());
        final Path result = getTempDirFile().toPath().resolve("bla.ar");
        final String archiverName = "ar";
        try (InputStream inputStream = Files.newInputStream(getPath("bla.ar"));
                ArchiveInputStream archiveInputStream = factory.createArchiveInputStream(archiverName, inputStream);
                OutputStream newOutputStream = Files.newOutputStream(result);
                ArchiveOutputStream archiveOutputStream = factory.createArchiveOutputStream(archiverName, newOutputStream);
                InputStream csInputStream = Files.newInputStream(file1.toPath())) {
            changeSet.add(entry, csInputStream);
            setLongFileMode(archiveOutputStream);
            new ChangeSetPerformer(changeSet).perform(archiveInputStream, archiveOutputStream);
        }
        final List<String> expected = new ArrayList<>();
        expected.add("test1.xml");
        expected.add("test.txt");
        checkArchiveContent(result, expected);
    }

    /**
     * Delete from a jar file and add another file
     *
     * @throws Exception Thrown on test failure. Thrown on test failure.
     */
    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void testDeleteFromAndAddToJar() throws Exception {
        final ChangeSet changeSet = new ChangeSet();
        changeSet.delete("test2.xml");
        changeSet.deleteDir("META-INF");
        changeSet.delete(".classpath");
        changeSet.delete(".project");
        final File input = getFile("bla.jar");
        final Path result = getTempDirFile().toPath().resolve("bla.jar");
        final String archiverName = "jar";
        try (InputStream inputStream = Files.newInputStream(input.toPath());
                ArchiveInputStream archiveInputStream = factory.createArchiveInputStream(archiverName, inputStream);
                OutputStream newOutputStream = Files.newOutputStream(result);
                ArchiveOutputStream archiveOutputStream = factory.createArchiveOutputStream(archiverName, newOutputStream);
                InputStream csInputStream = Files.newInputStream(getPath("test.txt"))) {
            changeSet.add(new JarArchiveEntry("testdata/test.txt"), csInputStream);
            setLongFileMode(archiveOutputStream);
            new ChangeSetPerformer(changeSet).perform(archiveInputStream, archiveOutputStream);
        }
        final List<String> expected = new ArrayList<>();
        expected.add("test1.xml");
        expected.add("testdata/test.txt");
        checkArchiveContent(result, expected);
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void testDeleteFromAndAddToTar() throws Exception {
        final ChangeSet changeSet = new ChangeSet();
        changeSet.delete("test2.xml");
        final File file1 = getFile("test.txt");
        final TarArchiveEntry entry = new TarArchiveEntry("testdata/test.txt");
        entry.setModTime(0);
        entry.setSize(file1.length());
        entry.setUserId(0);
        entry.setGroupId(0);
        entry.setUserName("avalon");
        entry.setGroupName("excalibur");
        entry.setMode(0100000);
        final File result = newTempFile("bla.tar");
        final String archiverName = "tar";
        try (InputStream inputStream = Files.newInputStream(getPath("bla.tar"));
                ArchiveInputStream archiveInputStream = factory.createArchiveInputStream(archiverName, inputStream);
                OutputStream newOutputStream = Files.newOutputStream(result.toPath());
                ArchiveOutputStream archiveOutputStream = factory.createArchiveOutputStream(archiverName, newOutputStream);
                InputStream csInputStream = Files.newInputStream(file1.toPath())) {
            changeSet.add(entry, csInputStream);
            setLongFileMode(archiveOutputStream);
            new ChangeSetPerformer(changeSet).perform(archiveInputStream, archiveOutputStream);
        }
        final List<String> expected = new ArrayList<>();
        expected.add("test1.xml");
        expected.add("testdata/test.txt");
        try (InputStream inputStream = Files.newInputStream(result.toPath());
                ArchiveInputStream archiveInputStream = factory.createArchiveInputStream(archiverName, inputStream)) {
            checkArchiveContent(archiveInputStream, expected);
        }
    }

    /**
     * Adds a file to a ZIP archive. Deletes another file.
     *
     * @throws Exception Thrown on test failure. Thrown on test failure.
     */
    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void testDeleteFromAndAddToZip() throws Exception {
        final String archiverName = "zip";
        final Path input = createArchive(archiverName);
        final Path result = Files.createTempFile("test", "." + archiverName);
        try (InputStream inputStream = Files.newInputStream(input);
                ArchiveInputStream archiveInputStream = factory.createArchiveInputStream(archiverName, inputStream);
                OutputStream newOutputStream = Files.newOutputStream(result);
                ArchiveOutputStream archiveOutputStream = factory.createArchiveOutputStream(archiverName, newOutputStream);
                InputStream csInputStream = Files.newInputStream(getPath("test.txt"))) {
            setLongFileMode(archiveOutputStream);
            final ChangeSet changeSet = new ChangeSet();

            final ArchiveEntry entry = new ZipArchiveEntry("blub/test.txt");
            changeSet.add(entry, csInputStream);
            archiveList.add("blub/test.txt");
            changeSet.delete("testdata/test1.xml");
            archiveListDelete("testdata/test1.xml");
            new ChangeSetPerformer(changeSet).perform(archiveInputStream, archiveOutputStream);
        } finally {
            checkArchiveContent(result, archiveList);
            forceDelete(result);
        }
    }

    /**
     * Adds a file to a ZIP archive. Deletes another file.
     *
     * @throws Exception Thrown on test failure. Thrown on test failure.
     */
    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void testDeleteFromAndAddToZipUsingZipFilePerform() throws Exception {
        final String archiverName = "zip";
        final Path input = createArchive(archiverName);
        final Path result = Files.createTempFile("test", "." + archiverName);
        try (ZipFile archiveInputStream = ZipFile.builder().setPath(input).get();
                OutputStream newOutputStream = Files.newOutputStream(result);
                ArchiveOutputStream archiveOutputStream = factory.createArchiveOutputStream(archiverName, newOutputStream);
                InputStream csInputStream = Files.newInputStream(getPath("test.txt"))) {
            setLongFileMode(archiveOutputStream);
            final ChangeSet changeSet = new ChangeSet();
            final ArchiveEntry entry = new ZipArchiveEntry("blub/test.txt");
            changeSet.add(entry, csInputStream);
            archiveList.add("blub/test.txt");
            changeSet.delete("testdata/test1.xml");
            archiveListDelete("testdata/test1.xml");
            new ChangeSetPerformer(changeSet).perform(archiveInputStream, archiveOutputStream);
        } finally {
            checkArchiveContent(result, archiveList);
            forceDelete(result);
        }
    }

    /**
     * Simple delete from an ar file
     *
     * @throws Exception Thrown on test failure. Thrown on test failure.
     */
    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void testDeleteFromAr() throws Exception {
        final ChangeSet changeSet = new ChangeSet();
        changeSet.delete("test2.xml");
        final File input = getFile("bla.ar");
        final File result = newTempFile("bla.ar");
        final String archiverName = "ar";
        try (InputStream inputStream = Files.newInputStream(input.toPath());
                ArchiveInputStream archiveInputStream = factory.createArchiveInputStream(archiverName, inputStream);
                OutputStream newOutputStream = Files.newOutputStream(result.toPath());
                ArchiveOutputStream archiveOutputStream = factory.createArchiveOutputStream(archiverName, newOutputStream)) {
            setLongFileMode(archiveOutputStream);
            new ChangeSetPerformer(changeSet).perform(archiveInputStream, archiveOutputStream);
        }
        final List<String> expected = new ArrayList<>();
        expected.add("test1.xml");
        checkArchiveContent(result, expected);
    }

    /**
     * Simple delete from a jar file
     *
     * @param archiverName archiver name.
     * @throws Exception Thrown on test failure.
     */
    @ParameterizedTest
    @MethodSource("org.apache.commons.compress.changes.TestFixtures#getZipOutputArchiveNames")
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void testDeleteFromJar(final String archiverName) throws Exception {
        final ChangeSet changeSet = new ChangeSet();
        changeSet.delete("test2.xml");
        changeSet.deleteDir("META-INF");
        changeSet.delete(".classpath");
        changeSet.delete(".project");
        final File input = getFile("bla.jar");
        final File result = newTempFile("bla.jar");
        try (InputStream inputStream = Files.newInputStream(input.toPath());
                ArchiveInputStream archiveInputStream = factory.createArchiveInputStream(archiverName, inputStream);
                OutputStream newOutputStream = Files.newOutputStream(result.toPath());
                ArchiveOutputStream archiveOutputStream = factory.createArchiveOutputStream(archiverName, newOutputStream)) {
            setLongFileMode(archiveOutputStream);
            new ChangeSetPerformer(changeSet).perform(archiveInputStream, archiveOutputStream);
        }
        final List<String> expected = new ArrayList<>();
        expected.add("test1.xml");
        checkArchiveContent(result, expected);
    }

    /**
     * Simple delete from a tar file
     *
     * @throws Exception Thrown on test failure.
     */
    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void testDeleteFromTar() throws Exception {
        final ChangeSet changeSet = new ChangeSet();
        changeSet.delete("test2.xml");
        final File result = newTempFile("bla.tar");
        final String archiverName = "tar";
        try (InputStream inputStream = Files.newInputStream(getFile("bla.tar").toPath());
                ArchiveInputStream archiveInputStream = factory.createArchiveInputStream(archiverName, inputStream);
                OutputStream newOutputStream = Files.newOutputStream(result.toPath());
                ArchiveOutputStream archiveOutputStream = factory.createArchiveOutputStream(archiverName, newOutputStream)) {
            setLongFileMode(archiveOutputStream);
            new ChangeSetPerformer(changeSet).perform(archiveInputStream, archiveOutputStream);
        }
        final List<String> expected = new ArrayList<>();
        expected.add("test1.xml");
        checkArchiveContent(result, expected);
    }

    /**
     * Simple Delete from a ZIP file.
     *
     * @param archiverName archiver name.
     * @throws Exception Thrown on test failure.
     */
    @ParameterizedTest
    @MethodSource("org.apache.commons.compress.changes.TestFixtures#getZipOutputArchiveNames")
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void testDeleteFromZip(final String archiverName) throws Exception {
        final ChangeSet changeSet = new ChangeSet();
        changeSet.delete("test2.xml");
        final File inputFile = getFile("bla.zip");
        final Path result = Files.createTempFile("test", ".zip");
        try (InputStream inputStream = Files.newInputStream(inputFile.toPath());
                ArchiveInputStream archiveInputStream = factory.createArchiveInputStream(archiverName, inputStream);
                OutputStream newOutputStream = Files.newOutputStream(result);
                ArchiveOutputStream archiveOutputStream = factory.createArchiveOutputStream(archiverName, newOutputStream)) {
            setLongFileMode(archiveOutputStream);
            new ChangeSetPerformer(changeSet).perform(archiveInputStream, archiveOutputStream);
        } finally {
            final List<String> expected = new ArrayList<>();
            expected.add("test1.xml");
            checkArchiveContent(result, expected);
            forceDelete(result);
        }
    }

    /**
     * Tries to delete a directory with a file and adds a new directory with a new file and with the same name. Should delete dir1/* and add dir1/test.txt at
     * the end
     *
     * @param archiverName archiver name.
     * @throws Exception Thrown on test failure.
     */
    @ParameterizedTest
    @MethodSource("org.apache.commons.compress.changes.TestFixtures#getOutputArchiveNames")
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void testDeletePlusAdd(final String archiverName) throws Exception {
        final Path inputPath = createArchive(archiverName);
        final Path result = Files.createTempFile("test", "." + archiverName);
        final File file1 = getFile("test.txt");
        try (InputStream inputStream = Files.newInputStream(inputPath);
                ArchiveInputStream archiveInputStream = factory.createArchiveInputStream(archiverName, inputStream);
                OutputStream newOutputStream = Files.newOutputStream(result);
                ArchiveOutputStream archiveOutputStream = factory.createArchiveOutputStream(archiverName, newOutputStream);
                InputStream csInputStream = Files.newInputStream(file1.toPath())) {
            setLongFileMode(archiveOutputStream);
            final ChangeSet changeSet = new ChangeSet();
            changeSet.deleteDir("bla");
            archiveListDeleteDir("bla");
            // Add a file
            final ArchiveEntry entry = archiveOutputStream.createArchiveEntry(file1, "bla/test.txt");
            changeSet.add(entry, csInputStream);
            archiveList.add("bla/test.txt");
            new ChangeSetPerformer(changeSet).perform(archiveInputStream, archiveOutputStream);
        } finally {
            checkArchiveContent(result, archiveList);
            forceDelete(result);
        }
    }

    /**
     * Tries to delete and then add a file with the same name. Should delete test/test3.xml and adds test.txt with the name test/test3.xml
     *
     * @param archiverName archiver name.
     * @throws Exception Thrown on test failure.
     */
    @ParameterizedTest
    @MethodSource("org.apache.commons.compress.changes.TestFixtures#getOutputArchiveNames")
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void testDeletePlusAddSame(final String archiverName) throws Exception {
        final Path inputPath = createArchive(archiverName);
        final File testTxt = getFile("test.txt");
        final Path result = Files.createTempFile("test", "." + archiverName);
        try {
            try (InputStream inputStream = Files.newInputStream(inputPath);
                    ArchiveInputStream archiveInputStream = factory.createArchiveInputStream(archiverName, inputStream);
                    OutputStream newOutputStream = Files.newOutputStream(result);
                    ArchiveOutputStream archiveOutputStream = factory.createArchiveOutputStream(archiverName, newOutputStream);
                    InputStream csInputStream = Files.newInputStream(testTxt.toPath())) {
                setLongFileMode(archiveOutputStream);
                final ChangeSet changes = new ChangeSet();
                changes.delete("test/test3.xml");
                archiveListDelete("test/test3.xml");
                // Add a file
                final ArchiveEntry entry = archiveOutputStream.createArchiveEntry(testTxt, "test/test3.xml");
                changes.add(entry, csInputStream);
                archiveList.add("test/test3.xml");
                new ChangeSetPerformer(changes).perform(archiveInputStream, archiveOutputStream);
            }
            // Checks
            try (BufferedInputStream buf = new BufferedInputStream(Files.newInputStream(result));
                    ArchiveInputStream in = factory.createArchiveInputStream(buf)) {
                final File check = checkArchiveContent(in, archiveList, false);
                final File test3xml = new File(check, "result/test/test3.xml");
                assertEquals(testTxt.length(), test3xml.length());

                try (BufferedReader reader = new BufferedReader(Files.newBufferedReader(test3xml.toPath()))) {
                    String str;
                    while ((str = reader.readLine()) != null) {
                        // All lines look like this
                        "111111111111111111111111111000101011".equals(str);
                    }
                }
                forceDelete(check);
            }
        } finally {
            forceDelete(result);
        }
    }

    /**
     * TODO: Move operations are not supported currently
     *
     * mv dir1/test.text dir2/test.txt + delete dir1 Moves the file to dir2 and deletes everything in dir1
     */
    @Test
    void testRenameAndDelete() {
    }

}
