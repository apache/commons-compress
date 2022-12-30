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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test that can read various archive file examples.
 *
 * This is a very simple implementation.
 *
 * Files must be in resources/archives, and there must be a file.txt containing
 * the list of files in the archives.
 */
public class ArchiveReadTest extends AbstractTestCase {

    private static final ClassLoader CLASSLOADER = ArchiveReadTest.class.getClassLoader();
    private static final File ARCDIR;
    private static final ArrayList<String> FILELIST = new ArrayList<>();

    static {
        try {
            ARCDIR = new File(CLASSLOADER.getResource("archives").toURI());
        } catch (final URISyntaxException e) {
            throw new AssertionError(e);
        }
    }

    public static Stream<Arguments> data() {
        assertTrue(ARCDIR.exists());
        final Collection<Arguments> params = new ArrayList<>();
        for (final String fileName : ARCDIR.list((dir, name) -> !name.endsWith(".txt"))) {
            params.add(Arguments.of(new File(ARCDIR, fileName)));
        }
        return params.stream();
    }

    @BeforeAll
    public static void setUpFileList() throws Exception {
        assertTrue(ARCDIR.exists());
        final File listing = new File(ARCDIR, "files.txt");
        assertTrue(listing.canRead(), "files.txt is readable");
        try (final BufferedReader br = new BufferedReader(Files.newBufferedReader(listing.toPath()))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("#")) {
                    FILELIST.add(line);
                }
            }
        }
    }

    // files.txt contains size and filename
    @Override
    protected String getExpectedString(final ArchiveEntry entry) {
        return entry.getSize() + " " + entry.getName();
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testArchive(final File file) throws Exception {
        @SuppressWarnings("unchecked") // fileList is correct type already
        final ArrayList<String> expected = (ArrayList<String>) FILELIST.clone();
        assertDoesNotThrow(() -> checkArchiveContent(file, expected), "Problem checking " + file);
    }
}
