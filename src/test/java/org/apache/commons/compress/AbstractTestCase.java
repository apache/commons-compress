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
package org.apache.commons.compress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.ar.ArArchiveOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public abstract class AbstractTestCase {

    protected interface StreamWrapper<I extends InputStream> {
        I wrap(InputStream inputStream) throws Exception;
    }

    public static File getFile(final String path) throws IOException {
        final URL url = AbstractTestCase.class.getClassLoader().getResource(path);
        if (url == null) {
            throw new FileNotFoundException("couldn't find " + path);
        }
        try {
            return new File(url.toURI());
        } catch (final URISyntaxException ex) {
            throw new IOException(ex);
        }
    }

    public static Path getPath(final String path) throws IOException {
        return getFile(path).toPath();
    }

    public static File mkdir(final String prefix) throws IOException {
        return Files.createTempDirectory(prefix).toFile();
    }

    public static InputStream newInputStream(final String path) throws IOException {
        return Files.newInputStream(getPath(path));
    }

    public static void rmdir(final File directory) {
        tryHardToDelete(directory);
    }

    /**
     * Deletes a file or directory. For a directory, delete it and all subdirectories.
     *
     * @param file a file or directory.
     * @return whether deletion was successful
     */
    public static boolean tryHardToDelete(final File file) {
        try {
            if (file != null && file.exists()) {
                FileUtils.forceDelete(file);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            file.deleteOnExit();
            return false;
        }
    }

    /**
     * Deletes a file or directory. For a directory, delete it and all subdirectories.
     *
     * @param path a file or directory
     * @return whether deletion was successful
     */
    public static boolean tryHardToDelete(final Path path) {
        return tryHardToDelete(path != null ? path.toFile() : null);
    }

    protected File dir;

    protected File resultDir;

    /** Used to delete the archive in {@link #tearDown()}. */
    private Path archivePath;

    /** Lists the content of the archive as originally created. */
    protected List<String> archiveList;

    protected final ArchiveStreamFactory factory = ArchiveStreamFactory.DEFAULT;

    /**
     * Add an entry to the archive, and keep track of the names in archiveList.
     *
     * @param outputStream
     * @param fileName
     * @param inputFile
     * @throws IOException
     * @throws FileNotFoundException
     */
    private void addArchiveEntry(final ArchiveOutputStream outputStream, final String fileName, final File inputFile)
            throws IOException, FileNotFoundException {
        final ArchiveEntry entry = outputStream.createArchiveEntry(inputFile, fileName);
        outputStream.putArchiveEntry(entry);
        Files.copy(inputFile.toPath(), outputStream);
        outputStream.closeArchiveEntry();
        archiveList.add(fileName);
    }

    /**
     * Checks that an archive input stream can be read, and that the file data matches file sizes.
     *
     * @param inputStream
     * @param expected list of expected entries or {@code null} if no check of names desired
     * @throws Exception
     */
    protected void checkArchiveContent(final ArchiveInputStream inputStream, final List<String> expected)
            throws Exception {
        checkArchiveContent(inputStream, expected, true);
    }

    /**
     * Checks that an archive input stream can be read, and that the file data matches file sizes.
     *
     * @param inputStream
     * @param expected list of expected entries or {@code null} if no check of names desired
     * @param cleanUp Cleans up resources if true
     * @return returns the created result file if cleanUp = false, or null otherwise
     * @throws Exception
     */
    protected File checkArchiveContent(final ArchiveInputStream inputStream, final List<String> expected, final boolean cleanUp)
            throws Exception {
        final File result = mkdir("dir-result");
        result.deleteOnExit();

        try {
            ArchiveEntry entry;
            while ((entry = inputStream.getNextEntry()) != null) {
                final File outputFile = new File(result.getCanonicalPath() + "/result/" + entry.getName());
                long bytesCopied = 0;
                if (entry.isDirectory()) {
                    outputFile.mkdirs();
                } else {
                    outputFile.getParentFile().mkdirs();
                    bytesCopied = Files.copy(inputStream, outputFile.toPath());
                }
                final long size = entry.getSize();
                if (size != ArchiveEntry.SIZE_UNKNOWN) {
                    assertEquals(size, bytesCopied, "Entry.size should equal bytes read.");
                }

                if (!outputFile.exists()) {
                    fail("Extraction failed: " + entry.getName());
                }
                if (expected != null && !expected.remove(getExpectedString(entry))) {
                    fail("Unexpected entry: " + getExpectedString(entry));
                }
            }
            inputStream.close();
            if (expected != null && !expected.isEmpty()) {
                fail(expected.size() + " missing entries: " + Arrays.toString(expected.toArray()));
            }
            if (expected != null) {
                assertEquals(0, expected.size());
            }
        } finally {
            if (cleanUp) {
                rmdir(result);
            }
        }
        return result;
    }

    /**
     * Checks if an archive contains all expected files.
     *
     * @param archive  the archive to check
     * @param expected a list with expected string file names
     * @throws Exception
     */
    protected void checkArchiveContent(final File archive, final List<String> expected) throws Exception {
        checkArchiveContent(archive.toPath(), expected);
    }

    /**
     * Checks if an archive contains all expected files.
     *
     * @param archive  the archive to check
     * @param expected a list with expected string file names
     * @throws Exception
     */
    protected void checkArchiveContent(final Path archive, final List<String> expected) throws Exception {
        try (InputStream inputStream = Files.newInputStream(archive);
                ArchiveInputStream archiveInputStream = factory.createArchiveInputStream(new BufferedInputStream(inputStream))) {
            checkArchiveContent(archiveInputStream, expected);
        }
    }

    protected void closeQuietly(final Closeable closeable) {
        IOUtils.closeQuietly(closeable);
    }

    /**
     * Creates an archive of text-based files in several directories. The
     * archive name is the factory identifier for the archiver, for example zip,
     * tar, cpio, jar, ar. The archive is created as a temp file.
     *
     * The archive contains the following files:
     * <ul>
     * <li>testdata/test1.xml</li>
     * <li>testdata/test2.xml</li>
     * <li>test/test3.xml</li>
     * <li>bla/test4.xml</li>
     * <li>bla/test5.xml</li>
     * <li>bla/blubber/test6.xml</li>
     * <li>test.txt</li>
     * <li>something/bla</li>
     * <li>test with spaces.txt</li>
     * </ul>
     *
     * @param archiveName
     *            the identifier of this archive
     * @return the newly created file
     * @throws Exception
     *             in case something goes wrong
     */
    protected Path createArchive(final String archiveName) throws Exception {
        archivePath = Files.createTempFile("test", "." + archiveName);
        archivePath.toFile().deleteOnExit();
        archiveList = new ArrayList<>();
        try (OutputStream outputStream = Files.newOutputStream(archivePath);
                ArchiveOutputStream archiveOutputStream = factory.createArchiveOutputStream(archiveName, outputStream)) {
            setLongFileMode(archiveOutputStream);
            final File file1 = getFile("test1.xml");
            final File file2 = getFile("test2.xml");
            final File file3 = getFile("test3.xml");
            final File file4 = getFile("test4.xml");
            final File file5 = getFile("test.txt");
            final File file6 = getFile("test with spaces.txt");

            addArchiveEntry(archiveOutputStream, "testdata/test1.xml", file1);
            addArchiveEntry(archiveOutputStream, "testdata/test2.xml", file2);
            addArchiveEntry(archiveOutputStream, "test/test3.xml", file3);
            addArchiveEntry(archiveOutputStream, "bla/test4.xml", file4);
            addArchiveEntry(archiveOutputStream, "bla/test5.xml", file4);
            addArchiveEntry(archiveOutputStream, "bla/blubber/test6.xml", file4);
            addArchiveEntry(archiveOutputStream, "test.txt", file5);
            addArchiveEntry(archiveOutputStream, "something/bla", file6);
            addArchiveEntry(archiveOutputStream, "test with spaces.txt", file6);

            archiveOutputStream.finish();
            return archivePath;
        }
    }

    /**
     * Create an empty archive.
     * @param archiveName
     * @return the archive File
     * @throws Exception
     */
    protected Path createEmptyArchive(final String archiveName) throws Exception {
        archiveList = new ArrayList<>();
        archivePath = Files.createTempFile("empty", "." + archiveName);
        archivePath.toFile().deleteOnExit();
        try (OutputStream outputStream = Files.newOutputStream(archivePath);
                ArchiveOutputStream archiveOutputStream = factory.createArchiveOutputStream(archiveName, outputStream)) {
            archiveOutputStream.finish();
        }
        return archivePath;
    }

    /**
     * Create an archive with a single file "test1.xml".
     *
     * @param archiveName
     * @return the archive File
     * @throws Exception
     */
    protected Path createSingleEntryArchive(final String archiveName) throws Exception {
        archiveList = new ArrayList<>();
        archivePath = Files.createTempFile("empty", "." + archiveName);
        archivePath.toFile().deleteOnExit();
        try (OutputStream outputStream = Files.newOutputStream(archivePath);
                ArchiveOutputStream archiveOutputStream = factory.createArchiveOutputStream(archiveName, outputStream)) {
            // Use short file name so does not cause problems for ar
            addArchiveEntry(archiveOutputStream, "test1.xml", getFile("test1.xml"));
            archiveOutputStream.finish();
        }
        return archivePath;
    }

    protected File createTempDir() throws IOException {
        final File tmpDir = mkdir("testdir");
        tmpDir.deleteOnExit();
        return tmpDir;
    }

    /**
     * Creates a temporary directory and a temporary file inside that
     * directory, returns both of them (the directory is the first
     * element of the two element array).
     *
     * @return temporary directory and file pair.
     * @throws IOException Some I/O error.
     */
    protected File[] createTempDirAndFile() throws IOException {
        final File tmpDir = createTempDir();
        final File tmpFile = File.createTempFile("testfile", "", tmpDir);
        tmpFile.deleteOnExit();
        try (OutputStream outputStream = Files.newOutputStream(tmpFile.toPath())) {
            outputStream.write(new byte[] { 'f', 'o', 'o' });
            return new File[] { tmpDir, tmpFile };
        }
    }

    /**
     * Override this method to change what is to be compared in the List.
     * For example, size + name instead of just name.
     *
     * @param entry
     * @return returns the entry name
     */
    protected String getExpectedString(final ArchiveEntry entry) {
        return entry.getName();
    }

    protected void setLongFileMode(final ArchiveOutputStream outputStream) {
        if (outputStream instanceof ArArchiveOutputStream) {
            ((ArArchiveOutputStream) outputStream).setLongFileMode(ArArchiveOutputStream.LONGFILE_BSD);
        }
    }

    @BeforeEach
    public void setUp() throws Exception {
        dir = mkdir("dir");
        resultDir = mkdir("dir-result");
        archivePath = null;
    }

    @AfterEach
    public void tearDown() throws Exception {
        rmdir(dir);
        rmdir(resultDir);
        dir = resultDir = null;
        if (!tryHardToDelete(archivePath)) {
            // Note: this exception won't be shown if the test has already failed
            throw new Exception("Could not delete " + archivePath);
        }
    }
}
