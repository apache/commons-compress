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
 */

package org.apache.commons.compress.archivers;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.Objects;

import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.tar.TarFile;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

/**
 * Simple command line application that lists the contents of an archive.
 *
 * <p>
 * The name of the archive must be given as a command line argument.
 * </p>
 * <p>
 * The optional second argument defines the archive type, in case the format is not recognized.
 * </p>
 *
 * @since 1.1
 */
public final class Lister {

    private static final ArchiveStreamFactory FACTORY = ArchiveStreamFactory.DEFAULT;

    private static <T extends ArchiveInputStream<? extends E>, E extends ArchiveEntry> T createArchiveInputStream(final String[] args,
            final InputStream inputStream) throws ArchiveException {
        if (args.length > 1) {
            return FACTORY.createArchiveInputStream(args[1], inputStream);
        }
        return FACTORY.createArchiveInputStream(inputStream);
    }

    private static String detectFormat(final File file) throws ArchiveException, IOException {
        try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(file.toPath()))) {
            return ArchiveStreamFactory.detect(inputStream);
        }
    }

    private static void list7z(final File file) throws IOException {
        try (SevenZFile sevenZFile = new SevenZFile(file)) {
            System.out.println("Created " + sevenZFile);
            ArchiveEntry entry;
            while ((entry = sevenZFile.getNextEntry()) != null) {
                final String name = entry.getName() == null ? sevenZFile.getDefaultName() + " (entry name was null)" : entry.getName();
                System.out.println(name);
            }
        }
    }

    private static void listStream(final File file, final String[] args) throws ArchiveException, IOException {
        try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(file.toPath()));
                ArchiveInputStream<?> archiveInputStream = createArchiveInputStream(args, inputStream)) {
            System.out.println("Created " + archiveInputStream.toString());
            ArchiveEntry entry;
            while ((entry = archiveInputStream.getNextEntry()) != null) {
                System.out.println(entry.getName());
            }
        }
    }

    private static void listZipUsingTarFile(final File file) throws IOException {
        try (TarFile tarFile = new TarFile(file)) {
            System.out.println("Created " + tarFile);
            tarFile.getEntries().forEach(en -> System.out.println(en.getName()));
        }
    }

    private static void listZipUsingZipFile(final File file) throws IOException {
        try (ZipFile zipFile = ZipFile.builder().setFile(file).get()) {
            System.out.println("Created " + zipFile);
            for (final Enumeration<ZipArchiveEntry> en = zipFile.getEntries(); en.hasMoreElements();) {
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
     * @throws IOException      an I/O exception.
     */
    public static void main(final String[] args) throws ArchiveException, IOException {
        if (args == null || args.length == 0) {
            usage();
            return;
        }
        Objects.requireNonNull(args[0], "args[0]");
        System.out.println("Analysing " + args[0]);
        final File file = new File(args[0]);
        if (!file.isFile()) {
            System.err.println(file + " doesn't exist or is a directory");
        }
        final String format = args.length > 1 ? args[1] : detectFormat(file);
        if (ArchiveStreamFactory.SEVEN_Z.equalsIgnoreCase(format)) {
            list7z(file);
        } else if ("zipfile".equals(format)) {
            listZipUsingZipFile(file);
        } else if ("tarfile".equals(format)) {
            listZipUsingTarFile(file);
        } else {
            listStream(file, args);
        }
    }

    private static void usage() {
        System.out.println("Parameters: archive-name [archive-type]\n");
        System.out.println("The magic archive-type 'zipfile' prefers ZipFile over ZipArchiveInputStream");
        System.out.println("The magic archive-type 'tarfile' prefers TarFile over TarArchiveInputStream");
    }

}
