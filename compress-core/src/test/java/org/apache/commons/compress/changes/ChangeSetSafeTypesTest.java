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

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.apache.commons.compress.archivers.cpio.CpioArchiveInputStream;
import org.apache.commons.compress.archivers.cpio.CpioArchiveOutputStream;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests {@link ChangeSet}.
 * <p>
 * Tests various combination of concrete types and generics.
 * </p>
 *
 * @param <I> The {@link ArchiveInputStream} type.
 * @param <O> The {@link ArchiveOutputStream} type.
 * @param <E> ArchiveEntry type must match between input and output.
 * @see ChangeSetRawTypesTest
 */
public final class ChangeSetSafeTypesTest<I extends ArchiveInputStream<E>, O extends ArchiveOutputStream<E>, E extends ArchiveEntry> extends AbstractTest {

    /** Deletes a single file. */
    private void archiveListDelete(final String prefix) {
        archiveList.removeIf(entry -> entry.equals(prefix));
    }

    /** Delete a directory tree. */
    private void archiveListDeleteDir(final String prefix) {
        // TODO won't work with folders
        archiveList.removeIf(entry -> entry.startsWith(prefix + "/"));
    }

    @SuppressWarnings("unchecked")
    private <T extends ArchiveInputStream<?>> T createArchiveInputStream(final InputStream inputStream) throws ArchiveException {
        return (T) factory.createArchiveInputStream(inputStream);
    }

    @SuppressWarnings("unchecked")
    private <T extends ArchiveInputStream<?>> T createArchiveInputStream(final String archiverName, final InputStream inputStream) throws ArchiveException {
        return (T) factory.createArchiveInputStream(archiverName, inputStream);
    }

    @SuppressWarnings("unchecked")
    private <T extends ArchiveOutputStream<?>> T createArchiveOutputStream(final String archiverName, final OutputStream outputStream) throws ArchiveException {
        return (T) factory.createArchiveOutputStream(archiverName, outputStream);
    }

    private <A extends ArchiveEntry> ChangeSet<A> createChangeSet() {
        return new ChangeSet<>();
    }

    /**
     * Adds a file with the same file name as an existing file from the stream. Should lead to a replacement.
     *
     * @throws Exception
     */
    @Test
    public void testAddAlreadyExistingWithReplaceFalse() throws Exception {
        final String archiverName = "zip";
        final Path input = createArchive(archiverName);
        final File result = createTempFile("test", "." + archiverName);
        final File file1 = getFile("test.txt");
        try (InputStream inputStream = Files.newInputStream(input);
                ZipArchiveInputStream ais = createArchiveInputStream(archiverName, inputStream);
                OutputStream outputStream = Files.newOutputStream(result.toPath());
                ZipArchiveOutputStream out = createArchiveOutputStream(archiverName, outputStream);
                InputStream csInputStream = Files.newInputStream(file1.toPath())) {
            final ChangeSet<ZipArchiveEntry> changeSet = createChangeSet();
            final ZipArchiveEntry entry = new ZipArchiveEntry("testdata/test1.xml");
            changeSet.add(entry, csInputStream, false);
            final ChangeSetResults results = new ChangeSetPerformer<>(changeSet).perform(ais, out);
            assertTrue(results.getAddedFromStream().contains("testdata/test1.xml"));
            assertTrue(results.getAddedFromChangeSet().isEmpty());
            assertTrue(results.getDeleted().isEmpty());
        }
        checkArchiveContent(result, archiveList);
    }

    /**
     * Adds a file with the same file name as an existing file from the stream. Should lead to a replacement.
     *
     * @throws Exception
     */
    @Test
    public void testAddAlreadyExistingWithReplaceTrue() throws Exception {
        final String archiverName = "zip";
        final Path input = createArchive(archiverName);
        final File result = createTempFile("test", "." + archiverName);
        final File file1 = getFile("test.txt");
        try (InputStream inputStream = Files.newInputStream(input);
                ZipArchiveInputStream ais = createArchiveInputStream(archiverName, inputStream);
                OutputStream outputStream = Files.newOutputStream(result.toPath());
                ZipArchiveOutputStream out = createArchiveOutputStream(archiverName, outputStream);
                InputStream csInputStream = Files.newInputStream(file1.toPath())) {
            final ChangeSet<ZipArchiveEntry> changeSet = createChangeSet();
            final ZipArchiveEntry entry = new ZipArchiveEntry("testdata/test1.xml");
            changeSet.add(entry, csInputStream, true);
            final ChangeSetResults results = new ChangeSetPerformer<>(changeSet).perform(ais, out);
            assertTrue(results.getAddedFromChangeSet().contains("testdata/test1.xml"));
        }
        checkArchiveContent(result, archiveList);
    }

    /**
     * Adds an ArchiveEntry with the same name two times. Only the latest addition should be found in the ChangeSet, the first add should be replaced.
     *
     * @throws Exception
     */
    @Test
    public void testAddChangeTwice() throws Exception {
        try (InputStream in = newInputStream("test.txt");
                InputStream in2 = newInputStream("test2.xml")) {
            final ZipArchiveEntry e = new ZipArchiveEntry("test.txt");
            final ZipArchiveEntry e2 = new ZipArchiveEntry("test.txt");
            final ChangeSet<ZipArchiveEntry> changeSet = createChangeSet();
            changeSet.add(e, in);
            changeSet.add(e2, in2);
            assertEquals(1, changeSet.getChanges().size());
            final Change<ZipArchiveEntry> change = changeSet.getChanges().iterator().next();
            @SuppressWarnings("resource")
            final InputStream cInputStream = change.getInputStream();
            assertEquals(in2, cInputStream);
        }
    }

    /**
     * Adds an ArchiveEntry with the same name two times. Only the first addition should be found in the ChangeSet, the second add should never be added since
     * replace = false
     *
     * @throws Exception
     */
    @Test
    public void testAddChangeTwiceWithoutReplace() throws Exception {
        try (InputStream in = newInputStream("test.txt");
                InputStream in2 = newInputStream("test2.xml")) {
            final ZipArchiveEntry e = new ZipArchiveEntry("test.txt");
            final ZipArchiveEntry e2 = new ZipArchiveEntry("test.txt");
            final ChangeSet<ZipArchiveEntry> changeSet = createChangeSet();
            changeSet.add(e, in, true);
            changeSet.add(e2, in2, false);
            assertEquals(1, changeSet.getChanges().size());
            final Change<ZipArchiveEntry> change = changeSet.getChanges().iterator().next();
            @SuppressWarnings("resource")
            final InputStream cInputStream = change.getInputStream();
            assertEquals(in, cInputStream);
        }
    }

    /**
     * add blub/test.txt + delete blub Should add blub/test.txt and delete it afterwards. In this example, the archive should stay untouched.
     *
     * @throws Exception
     */
    @Test
    public void testAddDeleteAdd() throws Exception {
        final String archiverName = "cpio";
        final Path input = createArchive(archiverName);
        final File result = createTempFile("test", "." + archiverName);
        final File file1 = getFile("test.txt");
        try (InputStream inputStream = Files.newInputStream(input);
                CpioArchiveInputStream ais = createArchiveInputStream(archiverName, inputStream);
                OutputStream outputStream = Files.newOutputStream(result.toPath());
                CpioArchiveOutputStream out = createArchiveOutputStream(archiverName, outputStream);
                InputStream csInputStream = Files.newInputStream(file1.toPath())) {
            final ChangeSet<CpioArchiveEntry> changeSet = createChangeSet();
            changeSet.add(new CpioArchiveEntry("blub/test.txt"), csInputStream);
            archiveList.add("blub/test.txt");
            changeSet.deleteDir("blub");
            archiveListDeleteDir("blub");
            new ChangeSetPerformer<>(changeSet).perform(ais, out);
        }
        checkArchiveContent(result, archiveList);
    }

    /**
     * Check can add and delete a file to an archive with a single file
     *
     * @throws Exception
     */
    @Test
    public void testAddDeleteToOneFileArchive() throws Exception {
        final String archiverName = "cpio";
        final Path input = createSingleEntryArchive(archiverName);
        final File result = createTempFile("test", "." + archiverName);
        final ChangeSet<CpioArchiveEntry> changeSet = createChangeSet();
        final File file = getFile("test.txt");
        try (InputStream inputStream = Files.newInputStream(input);
                CpioArchiveInputStream ais = createArchiveInputStream(archiverName, inputStream);
                OutputStream outputStream = Files.newOutputStream(result.toPath());
                CpioArchiveOutputStream out = createArchiveOutputStream(archiverName, outputStream);
                InputStream csInputStream = Files.newInputStream(file.toPath())) {
            changeSet.add(out.createArchiveEntry(file, "bla/test.txt"), csInputStream);
            archiveList.add("bla/test.txt");
            changeSet.delete("test1.xml");
            archiveListDelete("test1.xml");
            new ChangeSetPerformer<>(changeSet).perform(ais, out);
        }
        checkArchiveContent(result, archiveList);
    }

    /**
     * TODO: Move operations are not supported currently
     *
     * add dir1/bla.txt + mv dir1/test.text dir2/test.txt + delete dir1
     *
     * Add dir1/bla.txt should be suppressed. All other dir1 files will be deleted, except dir1/test.text will be moved
     *
     * @throws Exception
     */
    @Test
    public void testAddMoveDelete() throws Exception {
    }

    /**
     * Check can add a file to an empty archive.
     *
     * @param archiverName Archiver name.
     * @throws Exception
     */
    @ParameterizedTest
    @MethodSource("org.apache.commons.compress.changes.TestFixtures#getEmptyOutputArchiveNames")
    public void testAddToEmptyArchive(final String archiverName) throws Exception {
        // final String archiverName = "zip";
        final Path input = createEmptyArchive(archiverName);
        final File result = createTempFile("test", "." + archiverName);
        final ChangeSet<E> changeSet = createChangeSet();
        final File file1 = getFile("test.txt");
        try (InputStream inputStream = Files.newInputStream(input);
                ArchiveInputStream<E> ais = createArchiveInputStream(archiverName, inputStream);
                OutputStream outputStream = Files.newOutputStream(result.toPath());
                ArchiveOutputStream<E> out = createArchiveOutputStream(archiverName, outputStream);
                InputStream csInputStream = Files.newInputStream(file1.toPath())) {
            changeSet.add(out.createArchiveEntry(file1, "bla/test.txt"), csInputStream);
            archiveList.add("bla/test.txt");
            new ChangeSetPerformer<>(changeSet).perform(ais, out);
        }

        checkArchiveContent(result, archiveList);
    }

    @ParameterizedTest
    @MethodSource("org.apache.commons.compress.changes.TestFixtures#getZipOutputArchiveNames")
    public void testAddToEmptyZipArchive(final String archiverName) throws Exception {
        // final String archiverName = "zip";
        final Path input = createEmptyArchive(archiverName);
        final File result = createTempFile("test", "." + archiverName);
        final ChangeSet<ZipArchiveEntry> changeSet = createChangeSet();
        final File file1 = getFile("test.txt");
        try (InputStream inputStream = Files.newInputStream(input);
                ZipArchiveInputStream ais = createArchiveInputStream(archiverName, inputStream);
                OutputStream outputStream = Files.newOutputStream(result.toPath());
                ZipArchiveOutputStream out = createArchiveOutputStream(archiverName, outputStream);
                InputStream csInputStream = Files.newInputStream(file1.toPath())) {

            changeSet.add(new ZipArchiveEntry("bla/test.txt"), csInputStream);
            archiveList.add("bla/test.txt");
            new ChangeSetPerformer<>(changeSet).perform(ais, out);
        }

        checkArchiveContent(result, archiveList);
    }

    @ParameterizedTest
    @MethodSource("org.apache.commons.compress.changes.TestFixtures#getZipOutputArchiveNames")
    public void testAddToEmptyZipParamArchive(final String archiverName) throws Exception {
        final Path input = createEmptyArchive(archiverName);
        final File result = createTempFile("test", "." + archiverName);
        final ChangeSet<ZipArchiveEntry> changeSet = createChangeSet();
        final File file1 = getFile("test.txt");
        try (InputStream inputStream = Files.newInputStream(input);
                ArchiveInputStream<ZipArchiveEntry> ais = createArchiveInputStream(archiverName, inputStream);
                OutputStream outputStream = Files.newOutputStream(result.toPath());
                ArchiveOutputStream<ZipArchiveEntry> out = createArchiveOutputStream(archiverName, outputStream);
                InputStream csInputStream = Files.newInputStream(file1.toPath())) {

            changeSet.add(new ZipArchiveEntry("bla/test.txt"), csInputStream);
            archiveList.add("bla/test.txt");
            new ChangeSetPerformer<>(changeSet).perform(ais, out);
        }

        checkArchiveContent(result, archiveList);
    }

    /**
     * Checks for the correct ChangeSetResults
     *
     * @param archiverName Archiver name.
     * @throws Exception
     */
    @ParameterizedTest
    @MethodSource("org.apache.commons.compress.changes.TestFixtures#getOutputArchiveNames")
    public void testChangeSetResults(final String archiverName) throws Exception {
        final Path input = createArchive(archiverName);
        final File result = createTempFile("test", "." + archiverName);
        final File file1 = getFile("test.txt");
        try (InputStream inputStream = Files.newInputStream(input);
                ArchiveInputStream<E> ais = createArchiveInputStream(archiverName, inputStream);
                OutputStream outputStream = Files.newOutputStream(result.toPath());
                ArchiveOutputStream<E> out = createArchiveOutputStream(archiverName, outputStream);
                InputStream csInputStream = Files.newInputStream(file1.toPath())) {
            final ChangeSet<E> changeSet = createChangeSet();
            changeSet.deleteDir("bla");
            archiveListDeleteDir("bla");
            // Add a file
            final E entry = out.createArchiveEntry(file1, "bla/test.txt");
            changeSet.add(entry, csInputStream);
            archiveList.add("bla/test.txt");
            final ChangeSetResults results = new ChangeSetPerformer<>(changeSet).perform(ais, out);
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
        }
        checkArchiveContent(result, archiveList);
    }

    /**
     * delete bla + add bla/test.txt + delete bla Deletes dir1/* first, then suppresses the add of bla.txt because there is a delete operation later.
     *
     * @throws Exception
     */
    @Test
    public void testDeleteAddDelete() throws Exception {
        final String archiverName = "cpio";
        final Path input = createArchive(archiverName);
        final File result = createTempFile("test", "." + archiverName);
        final File file1 = getFile("test.txt");
        try (InputStream inputStream = Files.newInputStream(input);
                CpioArchiveInputStream ais = createArchiveInputStream(archiverName, inputStream);
                OutputStream outputStream = Files.newOutputStream(result.toPath());
                CpioArchiveOutputStream out = createArchiveOutputStream(archiverName, outputStream);
                InputStream csInputStream = Files.newInputStream(file1.toPath())) {
            final ChangeSet<CpioArchiveEntry> changeSet = createChangeSet();
            changeSet.deleteDir("bla");
            changeSet.add(new CpioArchiveEntry("bla/test.txt"), csInputStream);
            archiveList.add("bla/test.txt");
            changeSet.deleteDir("bla");
            archiveListDeleteDir("bla");
            new ChangeSetPerformer<>(changeSet).perform(ais, out);
        }
        checkArchiveContent(result, archiveList);
    }

    /**
     * Check can delete and add a file to an archive with a single file
     *
     * @throws Exception
     */
    @Test
    public void testDeleteAddToOneFileArchive() throws Exception {
        final String archiverName = "zip";
        final Path input = createSingleEntryArchive(archiverName);
        final File result = createTempFile("test", "." + archiverName);
        final ChangeSet<ZipArchiveEntry> changeSet = createChangeSet();
        final File file = getFile("test.txt");
        try (InputStream inputStream = Files.newInputStream(input);
                ZipArchiveInputStream ais = createArchiveInputStream(archiverName, inputStream);
                OutputStream outputStream = Files.newOutputStream(result.toPath());
                ZipArchiveOutputStream out = createArchiveOutputStream(archiverName, outputStream);
                InputStream csInputStream = Files.newInputStream(file.toPath())) {
            changeSet.delete("test1.xml");
            archiveListDelete("test1.xml");
            final ZipArchiveEntry entry = out.createArchiveEntry(file, "bla/test.txt");
            changeSet.add(entry, csInputStream);
            archiveList.add("bla/test.txt");
            new ChangeSetPerformer<>(changeSet).perform(ais, out);
        }
        checkArchiveContent(result, archiveList);
    }

    /**
     * Tries to delete the folder "bla" from an archive file. This should result in the deletion of bla/*, which actually means bla/test4.xml should be removed
     * from the archive. The file something/bla (without ending, named like the folder) should not be deleted.
     *
     * @param archiverName Archiver name.
     * @throws Exception
     */
    @ParameterizedTest
    @MethodSource("org.apache.commons.compress.changes.TestFixtures#getOutputArchiveNames")
    public void testDeleteDir(final String archiverName) throws Exception {
        final Path input = createArchive(archiverName);
        final File result = createTempFile("test", "." + archiverName);
        try (InputStream inputStream = Files.newInputStream(input);
                ArchiveInputStream<E> ais = createArchiveInputStream(archiverName, inputStream);
                OutputStream outputStream = Files.newOutputStream(result.toPath());
                ArchiveOutputStream<E> out = createArchiveOutputStream(archiverName, outputStream)) {
            final ChangeSet<E> changeSet = createChangeSet();
            changeSet.deleteDir("bla");
            archiveListDeleteDir("bla");
            new ChangeSetPerformer<>(changeSet).perform(ais, out);
        }
        checkArchiveContent(result, archiveList);
    }

    /**
     * Tries to delete the folder "la" from an archive file. This should result in the deletion of la/*, which should not match any files/folders.
     *
     * @param archiverName Archiver name.
     * @throws Exception
     */
    @ParameterizedTest
    @MethodSource("org.apache.commons.compress.changes.TestFixtures#getOutputArchiveNames")
    public void testDeleteDir2(final String archiverName) throws Exception {
        final Path input = createArchive(archiverName);
        final File result = createTempFile("test", "." + archiverName);
        try (InputStream inputStream = Files.newInputStream(input);
                ArchiveInputStream<E> ais = createArchiveInputStream(archiverName, inputStream);
                OutputStream outputStream = Files.newOutputStream(result.toPath());
                ArchiveOutputStream<E> out = createArchiveOutputStream(archiverName, outputStream)) {
            final ChangeSet<E> changeSet = createChangeSet();
            changeSet.deleteDir("la");
            archiveListDeleteDir("la");
            new ChangeSetPerformer<>(changeSet).perform(ais, out);
        }
        checkArchiveContent(result, archiveList);
    }

    /**
     * Tries to delete the folder "test.txt" from an archive file. This should not match any files/folders.
     *
     * @param archiverName Archiver name.
     * @throws Exception
     */
    @ParameterizedTest
    @MethodSource("org.apache.commons.compress.changes.TestFixtures#getOutputArchiveNames")
    public void testDeleteDir3(final String archiverName) throws Exception {
        final Path input = createArchive(archiverName);
        final File result = createTempFile("test", "." + archiverName);
        try (InputStream inputStream = Files.newInputStream(input);
                ArchiveInputStream<E> ais = createArchiveInputStream(archiverName, inputStream);
                OutputStream outputStream = Files.newOutputStream(result.toPath());
                ArchiveOutputStream<E> out = createArchiveOutputStream(archiverName, outputStream)) {
            final ChangeSet<E> changeSet = createChangeSet();
            changeSet.deleteDir("test.txt");
            archiveListDeleteDir("test.txt");
            new ChangeSetPerformer<>(changeSet).perform(ais, out);
        }

        checkArchiveContent(result, archiveList);
    }

    /**
     * Tries to delete the file "bla/test5.xml" from an archive. This should result in the deletion of "bla/test5.xml".
     *
     * @param archiverName Archiver name.
     * @throws Exception
     */
    @ParameterizedTest
    @MethodSource("org.apache.commons.compress.changes.TestFixtures#getOutputArchiveNames")
    public void testDeleteFileCpio(final String archiverName) throws Exception {
        final Path input = createArchive(archiverName);
        final File result = createTempFile("test", "." + archiverName);
        try (InputStream inputStream = Files.newInputStream(input);
                ArchiveInputStream<E> ais = createArchiveInputStream(archiverName, inputStream);
                OutputStream outputStream = Files.newOutputStream(result.toPath());
                ArchiveOutputStream<E> out = createArchiveOutputStream(archiverName, outputStream)) {
            final ChangeSet<E> changeSet = createChangeSet();
            changeSet.delete("bla/test5.xml");
            archiveListDelete("bla/test5.xml");
            new ChangeSetPerformer<>(changeSet).perform(ais, out);
        }
        checkArchiveContent(result, archiveList);
    }

    /**
     * Tries to delete the file "bla" from an archive. This should result in the deletion of nothing.
     *
     * @param archiverName Archiver name.
     * @throws Exception
     */
    @ParameterizedTest
    @MethodSource("org.apache.commons.compress.changes.TestFixtures#getOutputArchiveNames")
    public void testDeleteFileCpio2(final String archiverName) throws Exception {
        final Path input = createArchive(archiverName);
        final File result = createTempFile("test", "." + archiverName);
        try (InputStream inputStream = Files.newInputStream(input);
                ArchiveInputStream<E> ais = createArchiveInputStream(archiverName, inputStream);
                OutputStream outputStream = Files.newOutputStream(result.toPath());
                ArchiveOutputStream<E> out = createArchiveOutputStream(archiverName, outputStream)) {
            final ChangeSet<E> changeSet = createChangeSet();
            changeSet.delete("bla");
            // archiveListDelete("bla");
            new ChangeSetPerformer<>(changeSet).perform(ais, out);
        }
        checkArchiveContent(result, archiveList);
    }

    /**
     * Deletes a file from an AR-archive and adds another
     *
     * @throws Exception
     */
    @Test
    public void testDeleteFromAndAddToAr() throws Exception {
        final String archiverName = "ar";
        final ChangeSet<ArArchiveEntry> changeSet = createChangeSet();
        changeSet.delete("test2.xml");
        final File file1 = getFile("test.txt");
        final ArArchiveEntry entry = new ArArchiveEntry("test.txt", file1.length());
        final File input = getFile("bla.ar");
        final File result = newTempFile("bla.ar");
        try (InputStream inputStream = Files.newInputStream(input.toPath());
                ArchiveInputStream<ArArchiveEntry> ais = createArchiveInputStream(archiverName, inputStream);
                OutputStream outputStream = Files.newOutputStream(result.toPath());
                ArchiveOutputStream<ArArchiveEntry> out = createArchiveOutputStream(archiverName, outputStream);
                InputStream csInputStream = Files.newInputStream(file1.toPath())) {
            changeSet.add(entry, csInputStream);
            new ChangeSetPerformer<>(changeSet).perform(ais, out);
        }
        final List<String> expected = new ArrayList<>();
        expected.add("test1.xml");
        expected.add("test.txt");
        checkArchiveContent(result, expected);
    }

    /**
     * Delete from a jar file and add another file
     *
     * @throws Exception
     */
    @Test
    public void testDeleteFromAndAddToJar() throws Exception {
        final String archiverName = "jar";
        final ChangeSet<JarArchiveEntry> changeSet = createChangeSet();
        changeSet.delete("test2.xml");
        changeSet.deleteDir("META-INF");
        changeSet.delete(".classpath");
        changeSet.delete(".project");
        final File file1 = getFile("test.txt");
        final JarArchiveEntry entry = new JarArchiveEntry("testdata/test.txt");
        final File input = getFile("bla.jar");
        final File result = newTempFile("bla.jar");
        try (InputStream inputStream = Files.newInputStream(input.toPath());
                ArchiveInputStream<JarArchiveEntry> ais = createArchiveInputStream(archiverName, inputStream);
                OutputStream outputStream = Files.newOutputStream(result.toPath());
                ArchiveOutputStream<JarArchiveEntry> out = createArchiveOutputStream(archiverName, outputStream);
                InputStream csInputStream = Files.newInputStream(file1.toPath())) {
            changeSet.add(entry, csInputStream);
            new ChangeSetPerformer<>(changeSet).perform(ais, out);
        }
        final List<String> expected = new ArrayList<>();
        expected.add("test1.xml");
        expected.add("testdata/test.txt");
        checkArchiveContent(result, expected);
    }

    @Test
    public void testDeleteFromAndAddToTar() throws Exception {
        final String archiverName = "tar";
        final ChangeSet<TarArchiveEntry> changeSet = createChangeSet();
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
        final File input = getFile("bla.tar");
        final File result = newTempFile("bla.tar");
        try (InputStream inputStream = Files.newInputStream(input.toPath());
                ArchiveInputStream<TarArchiveEntry> ais = createArchiveInputStream(archiverName, inputStream);
                OutputStream outputStream = Files.newOutputStream(result.toPath());
                ArchiveOutputStream<TarArchiveEntry> out = createArchiveOutputStream(archiverName, outputStream);
                InputStream csInputStream = Files.newInputStream(file1.toPath())) {
            changeSet.add(entry, csInputStream);
            new ChangeSetPerformer<>(changeSet).perform(ais, out);
        }
        final List<String> expected = new ArrayList<>();
        expected.add("test1.xml");
        expected.add("testdata/test.txt");
        try (InputStream inputStream = Files.newInputStream(result.toPath());
                ArchiveInputStream<TarArchiveEntry> archiveInputStream = createArchiveInputStream(archiverName, inputStream)) {
            checkArchiveContent(archiveInputStream, expected);
        }
    }

    /**
     * Adds a file to a ZIP archive. Deletes another file.
     *
     * @param archiverName Archiver name.
     * @throws Exception
     */
    @ParameterizedTest
    @MethodSource("org.apache.commons.compress.changes.TestFixtures#getZipOutputArchiveNames")
    public void testDeleteFromAndAddToZip(final String archiverName) throws Exception {
        final Path input = createArchive(archiverName);
        final File result = createTempFile("test", "." + archiverName);
        final File file1 = getFile("test.txt");
        try (InputStream inputStream = Files.newInputStream(input);
                ArchiveInputStream<ZipArchiveEntry> ais = createArchiveInputStream(archiverName, inputStream);
                OutputStream outputStream = Files.newOutputStream(result.toPath());
                ArchiveOutputStream<ZipArchiveEntry> out = createArchiveOutputStream(archiverName, outputStream);
                InputStream csInputStream = Files.newInputStream(file1.toPath());) {
            final ChangeSet<ZipArchiveEntry> changeSet = createChangeSet();
            final ZipArchiveEntry entry = new ZipArchiveEntry("blub/test.txt");
            changeSet.add(entry, csInputStream);
            archiveList.add("blub/test.txt");
            changeSet.delete("testdata/test1.xml");
            archiveListDelete("testdata/test1.xml");
            new ChangeSetPerformer<>(changeSet).perform(ais, out);
        }

        checkArchiveContent(result, archiveList);
    }

    /**
     * Adds a file to a ZIP archive. Deletes another file.
     *
     * @param archiverName Archiver name.
     * @throws Exception
     */
    @ParameterizedTest
    @MethodSource("org.apache.commons.compress.changes.TestFixtures#getZipOutputArchiveNames")
    public void testDeleteFromAndAddToZipUsingZipFilePerform(final String archiverName) throws Exception {
        final Path input = createArchive(archiverName);
        final File result = createTempFile("test", "." + archiverName);
        final File file1 = getFile("test.txt");
        try (ZipFile ais = ZipFile.builder().setPath(input).get();
                OutputStream outputStream = Files.newOutputStream(result.toPath());
                ArchiveOutputStream<ZipArchiveEntry> out = createArchiveOutputStream(archiverName, outputStream);
                InputStream csInputStream = Files.newInputStream(file1.toPath())) {
            final ChangeSet<ZipArchiveEntry> changeSet = createChangeSet();
            final ZipArchiveEntry entry = new ZipArchiveEntry("blub/test.txt");
            changeSet.add(entry, csInputStream);
            archiveList.add("blub/test.txt");
            changeSet.delete("testdata/test1.xml");
            archiveListDelete("testdata/test1.xml");
            new ChangeSetPerformer<>(changeSet).perform(ais, out);
        }
        checkArchiveContent(result, archiveList);
    }

    /**
     * Simple delete from an ar file
     *
     * @throws Exception
     */
    @Test
    public void testDeleteFromAr() throws Exception {
        final String archiverName = "ar";
        final ChangeSet<E> changeSet = createChangeSet();
        changeSet.delete("test2.xml");
        final File input = getFile("bla.ar");
        final File temp = newTempFile("bla.ar");
        try (InputStream inputStream = Files.newInputStream(input.toPath());
                ArchiveInputStream<E> ais = createArchiveInputStream(archiverName, inputStream);
                OutputStream outputStream = Files.newOutputStream(temp.toPath());
                ArchiveOutputStream<E> out = createArchiveOutputStream(archiverName, outputStream)) {
            new ChangeSetPerformer<>(changeSet).perform(ais, out);
        }

        final List<String> expected = new ArrayList<>();
        expected.add("test1.xml");
        checkArchiveContent(temp, expected);
    }

    /**
     * Simple delete from a jar file
     *
     * @param archiverName Archiver name.
     * @throws Exception
     */
    @ParameterizedTest
    @MethodSource("org.apache.commons.compress.changes.TestFixtures#getZipOutputArchiveNames")
    public void testDeleteFromJar(final String archiverName) throws Exception {
        final ChangeSet<E> changeSet = createChangeSet();
        changeSet.delete("test2.xml");
        changeSet.deleteDir("META-INF");
        changeSet.delete(".classpath");
        changeSet.delete(".project");

        final File input = getFile("bla.jar");
        final File temp = newTempFile("bla.jar");
        try (InputStream inputStream = Files.newInputStream(input.toPath());
                ArchiveInputStream<E> ais = createArchiveInputStream(archiverName, inputStream);
                OutputStream outputStream = Files.newOutputStream(temp.toPath());
                ArchiveOutputStream<E> out = createArchiveOutputStream(archiverName, outputStream)) {
            new ChangeSetPerformer<>(changeSet).perform(ais, out);
        }
        final List<String> expected = new ArrayList<>();
        expected.add("test1.xml");
        checkArchiveContent(temp, expected);
    }

    /**
     * Simple delete from a tar file
     *
     * @throws Exception
     */
    @Test
    public void testDeleteFromTar() throws Exception {
        final String archiverName = "tar";
        final ChangeSet<E> changeSet = createChangeSet();
        changeSet.delete("test2.xml");
        final File input = getFile("bla.tar");
        final File temp = newTempFile("bla.tar");
        try (InputStream inputStream = Files.newInputStream(input.toPath());
                ArchiveInputStream<E> ais = createArchiveInputStream(archiverName, inputStream);
                OutputStream outputStream = Files.newOutputStream(temp.toPath());
                ArchiveOutputStream<E> out = createArchiveOutputStream(archiverName, outputStream)) {
            new ChangeSetPerformer<>(changeSet).perform(ais, out);
        }
        final List<String> expected = new ArrayList<>();
        expected.add("test1.xml");
        checkArchiveContent(temp, expected);
    }

    /**
     * Simple Delete from a ZIP file.
     *
     * @param archiverName Archiver name.
     * @throws Exception
     */
    @ParameterizedTest
    @MethodSource("org.apache.commons.compress.changes.TestFixtures#getZipOutputArchiveNames")
    public void testDeleteFromZip(final String archiverName) throws Exception {
        final ChangeSet<E> changeSet = createChangeSet();
        changeSet.delete("test2.xml");
        final File input = getFile("bla.zip");
        final File temp = createTempFile("test", ".zip");
        try (InputStream inputStream = Files.newInputStream(input.toPath());
                ArchiveInputStream<E> ais = createArchiveInputStream(archiverName, inputStream);
                OutputStream outputStream = Files.newOutputStream(temp.toPath());
                ArchiveOutputStream<E> out = createArchiveOutputStream(archiverName, outputStream)) {
            new ChangeSetPerformer<>(changeSet).perform(ais, out);
        }
        final List<String> expected = new ArrayList<>();
        expected.add("test1.xml");
        checkArchiveContent(temp, expected);
    }

    /**
     * Tries to delete a directory with a file and adds a new directory with a new file and with the same name. Should delete dir1/* and add dir1/test.txt at
     * the end
     *
     * @param archiverName Archiver name.
     * @throws Exception
     */
    @ParameterizedTest
    @MethodSource("org.apache.commons.compress.changes.TestFixtures#getOutputArchiveNames")
    public void testDeletePlusAdd(final String archiverName) throws Exception {
        final Path input = createArchive(archiverName);
        final File result = createTempFile("test", "." + archiverName);
        final File file1 = getFile("test.txt");
        try (InputStream inputStream = Files.newInputStream(input);
                ArchiveInputStream<E> ais = createArchiveInputStream(archiverName, inputStream);
                OutputStream outputStream = Files.newOutputStream(result.toPath());
                ArchiveOutputStream<E> out = createArchiveOutputStream(archiverName, outputStream);
                InputStream csInputStream = Files.newInputStream(file1.toPath())) {
            final ChangeSet<E> changeSet = createChangeSet();
            changeSet.deleteDir("bla");
            archiveListDeleteDir("bla");
            // Add a file
            final E entry = out.createArchiveEntry(file1, "bla/test.txt");
            changeSet.add(entry, csInputStream);
            archiveList.add("bla/test.txt");
            new ChangeSetPerformer<>(changeSet).perform(ais, out);
        }
        checkArchiveContent(result, archiveList);
    }

    /**
     * Tries to delete and then add a file with the same name. Should delete test/test3.xml and adds test.txt with the name test/test3.xml
     *
     * @param archiverName Archiver name.
     * @throws Exception
     */
    @ParameterizedTest
    @MethodSource("org.apache.commons.compress.changes.TestFixtures#getOutputArchiveNames")
    public void testDeletePlusAddSame(final String archiverName) throws Exception {
        final Path input = createArchive(archiverName);
        final File result = createTempFile("test", "." + archiverName);
        final File testTxt = getFile("test.txt");
        try (InputStream inputStream = Files.newInputStream(input);
                ArchiveInputStream<E> ais = createArchiveInputStream(archiverName, inputStream);
                OutputStream outputStream = Files.newOutputStream(result.toPath());
                ArchiveOutputStream<E> out = createArchiveOutputStream(archiverName, outputStream);
                InputStream csInputStream = Files.newInputStream(testTxt.toPath())) {
            final ChangeSet<E> changeSet = createChangeSet();
            changeSet.delete("test/test3.xml");
            archiveListDelete("test/test3.xml");
            // Add a file
            final E entry = out.createArchiveEntry(testTxt, "test/test3.xml");
            changeSet.add(entry, csInputStream);
            archiveList.add("test/test3.xml");
            new ChangeSetPerformer<>(changeSet).perform(ais, out);
        }

        // Checks
        try (BufferedInputStream buf = new BufferedInputStream(Files.newInputStream(result.toPath()));
                ArchiveInputStream<E> in = createArchiveInputStream(buf)) {
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
    }

    /**
     * TODO: Move operations are not supported currently
     *
     * mv dir1/test.text dir2/test.txt + delete dir1 Moves the file to dir2 and deletes everything in dir1
     */
    @Test
    public void testRenameAndDelete() {
    }

}
