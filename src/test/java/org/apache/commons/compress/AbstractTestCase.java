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
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public abstract class AbstractTestCase {

    protected interface StreamWrapper<I extends InputStream> {
        I wrap(InputStream in) throws Exception;
    }

    private static final boolean ON_WINDOWS =
            System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("windows");

    public static File getFile(final String path) throws IOException {
        final URL url = AbstractTestCase.class.getClassLoader().getResource(path);
        if (url == null) {
            throw new FileNotFoundException("couldn't find " + path);
        }
        URI uri = null;
        try {
            uri = url.toURI();
        } catch (final java.net.URISyntaxException ex) {
            throw new IOException(ex);
        }
        return new File(uri);
    }

    public static Path getPath(final String path) throws IOException {
        return getFile(path).toPath();
    }

    public static File mkdir(final String name) throws IOException {
        return Files.createTempDirectory(name).toFile();
    }

    public static InputStream newInputStream(final String path) throws IOException {
        return Files.newInputStream(getPath(path));
    }

    public static void rmdir(final File f) {
        final String[] s = f.list();
        if (s != null) {
            for (final String element : s) {
                final File file = new File(f, element);
                if (file.isDirectory()) {
                    rmdir(file);
                }
                final boolean ok = tryHardToDelete(file);
                if (!ok && file.exists()) {
                    System.out.println("Failed to delete " + element + " in " + f.getPath());
                }
            }
        }
        tryHardToDelete(f); // safer to delete and check
        if (f.exists()) {
            throw new Error("Failed to delete " + f.getPath());
        }
    }

    /**
     * Accommodate Windows bug encountered in both Sun and IBM JDKs.
     * Others possible. If the delete does not work, call System.gc(),
     * wait a little and try again.
     *
     * Copied from FileUtils in Ant 1.8.0.
     *
     * @return whether deletion was successful
     */
    public static boolean tryHardToDelete(final File f) {
        if (f != null && f.exists() && !f.delete()) {
            if (ON_WINDOWS) {
                System.gc();
            }
            try {
                Thread.sleep(10);
            } catch (final InterruptedException ex) {
                // Ignore Exception
            }
            return f.delete();
        }
        return true;
    }

    /**
     * Accommodate Windows bug encountered in both Sun and IBM JDKs.
     * Others possible. If the delete does not work, call System.gc(),
     * wait a little and try again.
     *
     * Copied from FileUtils in Ant 1.8.0.
     *
     * @return whether deletion was successful
     */
    public static boolean tryHardToDelete(final Path f) {
        return tryHardToDelete(f != null ? f.toFile() : null);
    }

    protected File dir;

    protected File resultDir;

    private Path archive; // used to delete the archive in tearDown

    protected List<String> archiveList; // Lists the content of the archive as originally created

    protected final ArchiveStreamFactory factory = ArchiveStreamFactory.DEFAULT;

    /**
     * Add an entry to the archive, and keep track of the names in archiveList.
     *
     * @param out
     * @param filename
     * @param infile
     * @throws IOException
     * @throws FileNotFoundException
     */
    private void addArchiveEntry(final ArchiveOutputStream out, final String filename, final File infile)
            throws IOException, FileNotFoundException {
        final ArchiveEntry entry = out.createArchiveEntry(infile, filename);
        out.putArchiveEntry(entry);
        Files.copy(infile.toPath(), out);
        out.closeArchiveEntry();
        archiveList.add(filename);
    }

    /**
     * Checks that an archive input stream can be read, and that the file data matches file sizes.
     *
     * @param in
     * @param expected list of expected entries or {@code null} if no check of names desired
     * @throws Exception
     */
    protected void checkArchiveContent(final ArchiveInputStream in, final List<String> expected)
            throws Exception {
        checkArchiveContent(in, expected, true);
    }

    /**
     * Checks that an archive input stream can be read, and that the file data matches file sizes.
     *
     * @param in
     * @param expected list of expected entries or {@code null} if no check of names desired
     * @param cleanUp Cleans up resources if true
     * @return returns the created result file if cleanUp = false, or null otherwise
     * @throws Exception
     */
    protected File checkArchiveContent(final ArchiveInputStream in, final List<String> expected, final boolean cleanUp)
            throws Exception {
        final File result = mkdir("dir-result");
        result.deleteOnExit();

        try {
            ArchiveEntry entry = null;
            while ((entry = in.getNextEntry()) != null) {
                final File outfile = new File(result.getCanonicalPath() + "/result/" + entry.getName());
                long copied = 0;
                if (entry.isDirectory()) {
                    outfile.mkdirs();
                } else {
                    outfile.getParentFile().mkdirs();
                    copied = Files.copy(in, outfile.toPath());
                }
                final long size = entry.getSize();
                if (size != ArchiveEntry.SIZE_UNKNOWN) {
                    assertEquals(size, copied, "Entry.size should equal bytes read.");
                }

                if (!outfile.exists()) {
                    fail("extraction failed: " + entry.getName());
                }
                if (expected != null && !expected.remove(getExpectedString(entry))) {
                    fail("unexpected entry: " + getExpectedString(entry));
                }
            }
            in.close();
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
     * @param archive
     *            the archive to check
     * @param expected
     *            a list with expected string file names
     * @throws Exception
     */
    protected void checkArchiveContent(final File archive, final List<String> expected)
            throws Exception {
        try (InputStream is = Files.newInputStream(archive.toPath());
            final ArchiveInputStream in = factory.createArchiveInputStream(new BufferedInputStream(is))) {
            this.checkArchiveContent(in, expected);
        }
    }

    protected void closeQuietly(final Closeable closeable){
        if (closeable != null) {
            try {
                closeable.close();
            } catch (final IOException ignored) {
                // ignored
            }
        }
    }

    /**
     * Creates an archive of textbased files in several directories. The
     * archivename is the factory identifier for the archiver, for example zip,
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
     * @param archivename
     *            the identifier of this archive
     * @return the newly created file
     * @throws Exception
     *             in case something goes wrong
     */
    protected Path createArchive(final String archivename) throws Exception {
        ArchiveOutputStream out = null;
        OutputStream stream = null;
        try {
            archive = Files.createTempFile("test", "." + archivename);
            archive.toFile().deleteOnExit();
            archiveList = new ArrayList<>();

            stream = Files.newOutputStream(archive);
            out = factory.createArchiveOutputStream(archivename, stream);

            final File file1 = getFile("test1.xml");
            final File file2 = getFile("test2.xml");
            final File file3 = getFile("test3.xml");
            final File file4 = getFile("test4.xml");
            final File file5 = getFile("test.txt");
            final File file6 = getFile("test with spaces.txt");

            addArchiveEntry(out, "testdata/test1.xml", file1);
            addArchiveEntry(out, "testdata/test2.xml", file2);
            addArchiveEntry(out, "test/test3.xml", file3);
            addArchiveEntry(out, "bla/test4.xml", file4);
            addArchiveEntry(out, "bla/test5.xml", file4);
            addArchiveEntry(out, "bla/blubber/test6.xml", file4);
            addArchiveEntry(out, "test.txt", file5);
            addArchiveEntry(out, "something/bla", file6);
            addArchiveEntry(out, "test with spaces.txt", file6);

            out.finish();
            return archive;
        } finally {
            if (out != null) {
                out.close();
            } else if (stream != null) {
                stream.close();
            }
        }
    }

    /**
     * Create an empty archive.
     * @param archivename
     * @return the archive File
     * @throws Exception
     */
    protected Path createEmptyArchive(final String archivename) throws Exception {
        ArchiveOutputStream out = null;
        OutputStream stream = null;
        archiveList = new ArrayList<>();
        try {
            archive = Files.createTempFile("empty", "." + archivename);
            archive.toFile().deleteOnExit();
            stream = Files.newOutputStream(archive);
            out = factory.createArchiveOutputStream(archivename, stream);
            out.finish();
        } finally {
            if (out != null) {
                out.close();
            } else if (stream != null) {
                stream.close();
            }
        }
        return archive;
    }

    /**
     * Create an archive with a single file "test1.xml".
     *
     * @param archivename
     * @return the archive File
     * @throws Exception
     */
    protected Path createSingleEntryArchive(final String archivename) throws Exception {
        ArchiveOutputStream out = null;
        OutputStream stream = null;
        archiveList = new ArrayList<>();
        try {
            archive = Files.createTempFile("empty", "." + archivename);
            archive.toFile().deleteOnExit();
            stream = Files.newOutputStream(archive);
            out = factory.createArchiveOutputStream(archivename, stream);
            // Use short file name so does not cause problems for ar
            addArchiveEntry(out, "test1.xml", getFile("test1.xml"));
            out.finish();
        } finally {
            if (out != null) {
                out.close();
            } else if (stream != null) {
                stream.close();
            }
        }
        return archive;
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
     */
    protected File[] createTempDirAndFile() throws IOException {
        final File tmpDir = createTempDir();
        final File tmpFile = File.createTempFile("testfile", "", tmpDir);
        tmpFile.deleteOnExit();
        try (OutputStream fos = Files.newOutputStream(tmpFile.toPath())) {
            fos.write(new byte[] { 'f', 'o', 'o' });
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

    @BeforeEach
    public void setUp() throws Exception {
        dir = mkdir("dir");
        resultDir = mkdir("dir-result");
        archive = null;
    }

    @AfterEach
    public void tearDown() throws Exception {
        rmdir(dir);
        rmdir(resultDir);
        dir = resultDir = null;
        if (!tryHardToDelete(archive)) {
            // Note: this exception won't be shown if the test has already failed
            throw new Exception("Could not delete " + archive);
        }
    }
}
