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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream;
import org.apache.commons.compress.archivers.cpio.CpioArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test that can read various tar file examples.
 *
  * Files must be in resources/longsymlink, and there must be a file.txt containing
 * the list of files in the archives.
*/
public class LongSymLinkTest extends AbstractTestCase {

    private static final ClassLoader CLASSLOADER = LongSymLinkTest.class.getClassLoader();
    private static final File ARCDIR;
    private static final ArrayList<String> FILELIST = new ArrayList<>();

    static {
        try {
            ARCDIR = new File(CLASSLOADER.getResource("longsymlink").toURI());
        } catch (final URISyntaxException e) {
            throw new AssertionError(e);
        }
    }

    public static Stream<Arguments> data() {
        final Collection<Arguments> params = new ArrayList<>();
        for (final String fileName : ARCDIR.list((dir, name) -> !name.endsWith(".txt"))) {
            params.add(Arguments.of(new File(ARCDIR, fileName)));
        }
        return params.stream();
    }

    @BeforeAll
    public static void setUpFileList() throws Exception {
        assertTrue(ARCDIR.exists());
        final File listing= new File(ARCDIR,"files.txt");
        assertTrue(listing.canRead(), "files.txt is readable");
        final BufferedReader br = new BufferedReader(Files.newBufferedReader(listing.toPath()));
        String line;
        while ((line=br.readLine())!=null){
            if (!line.startsWith("#")){
                FILELIST.add(line);
            }
        }
        br.close();
    }


    @Override
    protected String getExpectedString(final ArchiveEntry entry) {
        if (entry instanceof TarArchiveEntry) {
            final TarArchiveEntry tarEntry = (TarArchiveEntry) entry;
            if (tarEntry.isSymbolicLink()) {
                return tarEntry.getName() + " -> " + tarEntry.getLinkName();
            }
        }
        return entry.getName();
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testArchive(final File file) throws Exception {
        @SuppressWarnings("unchecked") // fileList is of correct type
        final
        ArrayList<String> expected = (ArrayList<String>) FILELIST.clone();
        final String name = file.getName();
        if ("minotaur.jar".equals(name) || "minotaur-0.jar".equals(name)){
            expected.add("META-INF/");
            expected.add("META-INF/MANIFEST.MF");
        }
        final ArchiveInputStream ais = factory.createArchiveInputStream(new BufferedInputStream(Files.newInputStream(file.toPath())));
        // check if expected type recognized
        if (name.endsWith(".tar")){
            assertTrue(ais instanceof TarArchiveInputStream);
        } else if (name.endsWith(".jar") || name.endsWith(".zip")){
            assertTrue(ais instanceof ZipArchiveInputStream);
        } else if (name.endsWith(".cpio")){
            assertTrue(ais instanceof CpioArchiveInputStream);
            // Hack: cpio does not add trailing "/" to directory names
            for(int i=0; i < expected.size(); i++){
                final String ent = expected.get(i);
                if (ent.endsWith("/")){
                    expected.set(i, ent.substring(0, ent.length()-1));
                }
            }
        } else if (name.endsWith(".ar")){
            assertTrue(ais instanceof ArArchiveInputStream);
            // CPIO does not store directories or directory names
            expected.clear();
            for (final String ent : FILELIST) {
                if (!ent.endsWith("/")) {// not a directory
                    final int lastSlash = ent.lastIndexOf('/');
                    if (lastSlash >= 0) { // extract path name
                        expected.add(ent.substring(lastSlash + 1));
                    } else {
                        expected.add(ent);
                    }
                }
            }
        } else {
            fail("Unexpected file type: "+name);
        }
        try {
            assertDoesNotThrow(() -> checkArchiveContent(ais, expected), "Error processing " + file.getName());
        } finally {
            ais.close();
        }
    }
}
