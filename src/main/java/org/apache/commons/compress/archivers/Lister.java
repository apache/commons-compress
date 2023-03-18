/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.commons.compress.archivers;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Enumeration;

import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.tar.TarFile;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

/**
 * Simple command line application that lists the contents of an archive.
 *
 * <p>The name of the archive must be given as a command line argument.</p>
 * <p>The optional second argument defines the archive type, in case the format is not recognized.</p>
 *
 * @since 1.1
 */
public final class Lister {

    private static final ArchiveStreamFactory FACTORY = ArchiveStreamFactory.DEFAULT;

    private static ArchiveInputStream createArchiveInputStream(final String[] args, final InputStream fis)
            throws ArchiveException {
        if (args.length > 1) {
            return FACTORY.createArchiveInputStream(args[1], fis);
        }
        return FACTORY.createArchiveInputStream(fis);
    }

    private static String detectFormat(final File f) throws ArchiveException, IOException {
        try (final InputStream fis = new BufferedInputStream(Files.newInputStream(f.toPath()))) {
            return ArchiveStreamFactory.detect(fis);
        }
    }

    private static void list7z(final File f) throws IOException {
        try (SevenZFile z = new SevenZFile(f)) {
            System.out.println("Created " + z);
            ArchiveEntry ae;
            while ((ae = z.getNextEntry()) != null) {
                final String name = ae.getName() == null ? z.getDefaultName() + " (entry name was null)"
                    : ae.getName();
                System.out.println(name);
            }
        }
    }

    private static void listStream(final File f, final String[] args) throws ArchiveException, IOException {
        try (final InputStream fis = new BufferedInputStream(Files.newInputStream(f.toPath()));
                final ArchiveInputStream ais = createArchiveInputStream(args, fis)) {
            System.out.println("Created " + ais.toString());
            ArchiveEntry ae;
            while ((ae = ais.getNextEntry()) != null) {
                System.out.println(ae.getName());
            }
        }
    }

    private static void listZipUsingTarFile(final File f) throws IOException {
        try (TarFile t = new TarFile(f)) {
            System.out.println("Created " + t);
            t.getEntries().forEach(en -> System.out.println(en.getName()));
        }
    }

    private static void listZipUsingZipFile(final File f) throws IOException {
        try (ZipFile z = new ZipFile(f)) {
            System.out.println("Created " + z);
            for (final Enumeration<ZipArchiveEntry> en = z.getEntries(); en.hasMoreElements(); ) {
                System.out.println(en.nextElement().getName());
            }
        }
    }

    /**
     * Runs this class from the command line.
     * <p>
     * The name of the archive must be given as a command line argument.
     * </p>
     * <p>
     * The optional second argument defines the archive type, in case the format is not recognized.
     * </p>
     *
     * @param args name of the archive and optional argument archive type.
     * @throws ArchiveException Archiver related Exception.
     * @throws IOException an I/O exception.
     */
    public static void main(final String[] args) throws ArchiveException, IOException {
        if (args.length == 0) {
            usage();
            return;
        }
        System.out.println("Analysing " + args[0]);
        final File f = new File(args[0]);
        if (!f.isFile()) {
            System.err.println(f + " doesn't exist or is a directory");
        }
        final String format = args.length > 1 ? args[1] : detectFormat(f);
        if (ArchiveStreamFactory.SEVEN_Z.equalsIgnoreCase(format)) {
            list7z(f);
        } else if ("zipfile".equals(format)) {
            listZipUsingZipFile(f);
        } else if ("tarfile".equals(format)) {
            listZipUsingTarFile(f);
        } else {
            listStream(f, args);
        }
    }

    private static void usage() {
        System.out.println("Parameters: archive-name [archive-type]\n");
        System.out.println("The magic archive-type 'zipfile' prefers ZipFile over ZipArchiveInputStream");
        System.out.println("The magic archive-type 'tarfile' prefers TarFile over TarArchiveInputStream");
    }

}
