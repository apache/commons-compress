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

package org.apache.commons.compress.archivers;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.tar.TarFile;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

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

    private static String detectFormat(final Path file) throws ArchiveException, IOException {
        try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(file))) {
            return ArchiveStreamFactory.detect(inputStream);
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
    public static void main(final String... args) throws ArchiveException, IOException {
        if (ArrayUtils.isEmpty(args)) {
            usage();
            return;
        }
        new Lister(false, args).go();
    }

    private static void usage() {
        System.err.println("Parameters: archive-name [archive-type]\n");
        System.err.println("The magic archive-type 'zipfile' prefers ZipFile over ZipArchiveInputStream");
        System.err.println("The magic archive-type 'tarfile' prefers TarFile over TarArchiveInputStream");
    }

    private final boolean quiet;

    private final String[] args;

    /**
     * Constructs a new instance.
     *
     * @deprecated No replacement.
     */
    @Deprecated
    public Lister() {
        this(false, "");
    }

    Lister(final boolean quiet, final String... args) {
        this.quiet = quiet;
        this.args = args.clone();
        Objects.requireNonNull(args[0], "args[0]");
    }

    void go() throws ArchiveException, IOException {
        list(Paths.get(args[0]), args);
    }

    private void list(final Path file, final String... args) throws ArchiveException, IOException {
        println("Analyzing " + file);
        if (!Files.isRegularFile(file)) {
            System.err.println(file + " doesn't exist or is a directory");
        }
        final String format = StringUtils.toRootLowerCase(args.length > 1 ? args[1] : detectFormat(file));
        println("Detected format " + format);
        switch (format) {
        case ArchiveStreamFactory.SEVEN_Z:
            list7z(file);
            break;
        case ArchiveStreamFactory.ZIP:
            listZipUsingZipFile(file);
            break;
        case ArchiveStreamFactory.TAR:
            listZipUsingTarFile(file);
            break;
        default:
            listStream(file, args);
        }
    }

    private  void list7z(final Path file) throws IOException {
        try (SevenZFile sevenZFile = SevenZFile.builder().setPath(file).get()) {
            println("Created " + sevenZFile);
            ArchiveEntry entry;
            while ((entry = sevenZFile.getNextEntry()) != null) {
                println(entry.getName() == null ? sevenZFile.getDefaultName() + " (entry name was null)" : entry.getName());
            }
        }
    }

    private  void listStream(final Path file, final String[] args) throws ArchiveException, IOException {
        try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(file));
                ArchiveInputStream<?> archiveInputStream = createArchiveInputStream(args, inputStream)) {
            println("Created " + archiveInputStream.toString());
            archiveInputStream.forEach(this::println);
        }
    }

    private  void listZipUsingTarFile(final Path file) throws IOException {
        try (TarFile tarFile = new TarFile(file)) {
            println("Created " + tarFile);
            tarFile.getEntries().forEach(this::println);
        }
    }

    private  void listZipUsingZipFile(final Path file) throws IOException {
        try (ZipFile zipFile = ZipFile.builder().setPath(file).get()) {
            println("Created " + zipFile);
            zipFile.stream().forEach(this::println);
        }
    }

    private void println(final ArchiveEntry entry) {
        println(entry.getName());
    }

    private void println(final String line) {
        if (!quiet) {
            System.out.println(line);
        }
    }

}
