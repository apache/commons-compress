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

package org.apache.commons.compress.archivers.zip;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.utils.IOUtils;

/**
 * Simple command line application that lists the contents of a ZIP archive.
 *
 * <p>The name of the archive must be given as a command line argument.</p>
 *
 * <p>Optional command line arguments specify the encoding to assume
 * and whether to use ZipFile or ZipArchiveInputStream.</p>
 */
public final class Lister {
    private static class CommandLine {
        String archive;
        boolean useStream = false;
        String encoding;
        boolean allowStoredEntriesWithDataDescriptor = false;
        String dir;
    }

    public static void main(final String[] args) throws IOException {
        final CommandLine cl = parse(args);
        final File f = new File(cl.archive);
        if (!f.isFile()) {
            System.err.println(f + " doesn't exists or is a directory");
            usage();
        }
        if (cl.useStream) {
            try (BufferedInputStream fs = new BufferedInputStream(new FileInputStream(f))) {
                final ZipArchiveInputStream zs =
                        new ZipArchiveInputStream(fs, cl.encoding, true,
                                cl.allowStoredEntriesWithDataDescriptor);
                for (ArchiveEntry entry = zs.getNextEntry();
                     entry != null;
                     entry = zs.getNextEntry()) {
                    final ZipArchiveEntry ze = (ZipArchiveEntry) entry;
                    list(ze);
                    if (cl.dir != null) {
                        extract(cl.dir, ze, zs);
                    }
                }
            }
        } else {
            try (ZipFile zf = new ZipFile(f, cl.encoding)) {
                for (final Enumeration<ZipArchiveEntry> entries = zf.getEntries();
                     entries.hasMoreElements(); ) {
                    final ZipArchiveEntry ze = entries.nextElement();
                    list(ze);
                    if (cl.dir != null) {
                        try (InputStream is = zf.getInputStream(ze)) {
                            extract(cl.dir, ze, is);
                        }
                    }
                }
            }
        }
    }

    private static void list(final ZipArchiveEntry entry) {
        System.out.println(entry.getName());
    }

    private static void extract(final String dir, final ZipArchiveEntry entry,
                                final InputStream is) throws IOException {
        final File f = new File(dir, entry.getName());
        if (!f.getParentFile().exists()) {
            f.getParentFile().mkdirs();
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(f);
            IOUtils.copy(is, fos);
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
    }

    private static CommandLine parse(final String[] args) {
        final CommandLine cl = new CommandLine();
        boolean error = false;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-enc")) {
                if (args.length > i + 1) {
                    cl.encoding = args[++i];
                } else {
                    System.err.println("missing argument to -enc");
                    error = true;
                }
            } else if (args[i].equals("-extract")) {
                if (args.length > i + 1) {
                    cl.dir = args[++i];
                } else {
                    System.err.println("missing argument to -extract");
                    error = true;
                }
            } else if (args[i].equals("-stream")) {
                cl.useStream = true;
            } else if (args[i].equals("+storeddd")) {
                cl.allowStoredEntriesWithDataDescriptor = true;
            } else if (args[i].equals("-file")) {
                cl.useStream = false;
            } else if (cl.archive != null) {
                System.err.println("Only one archive");
                error = true;
            } else {
                cl.archive = args[i];
            }
        }
        if (error || cl.archive == null) {
            usage();
        }
        return cl;
    }

    private static void usage() {
        System.err.println("lister [-enc encoding] [-stream] [-file]"
                           + " [+storeddd] [-extract dir] archive");
        System.exit(1);
    }
}