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
package org.apache.commons.compress.harmony.pack200;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.apache.commons.compress.harmony.pack200.Archive.PackingFile;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.function.IOConsumer;

public class PackingUtils {

    private static final class PackingLogger extends Logger {

        private boolean verbose;

        protected PackingLogger(final String name, final String resourceBundleName) {
            super(name, resourceBundleName);
        }

        @Override
        public void log(final LogRecord logRecord) {
            if (verbose) {
                super.log(logRecord);
            }
        }

        private void setVerbose(final boolean isVerbose) {
            verbose = isVerbose;
        }
    }

    private static PackingLogger packingLogger;
    private static FileHandler fileHandler;

    static {
        packingLogger = new PackingLogger("org.harmony.apache.pack200", null);
        LogManager.getLogManager().addLogger(packingLogger);
    }

    public static void config(final PackingOptions options) throws IOException {
        final String logFileName = options != null ? options.getLogFile() : null;
        if (fileHandler != null) {
            fileHandler.close();
        }
        if (logFileName != null) {
            fileHandler = new FileHandler(logFileName, false);
            fileHandler.setFormatter(new SimpleFormatter());
            packingLogger.addHandler(fileHandler);
            packingLogger.setUseParentHandlers(false);
        }
        if (options != null) {
            packingLogger.setVerbose(options.isVerbose());
        }
    }

    /**
     * When effort is 0, the packer copys through the original jar file without compression
     *
     * @param jarFile      the input jar file
     * @param outputStream the jar output stream
     * @throws IOException If an I/O error occurs.
     */
    public static void copyThroughJar(final JarFile jarFile, final OutputStream outputStream) throws IOException {
        try (JarOutputStream jarOutputStream = new JarOutputStream(outputStream)) {
            jarOutputStream.setComment("PACK200");
            final Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                final JarEntry jarEntry = entries.nextElement();
                jarOutputStream.putNextEntry(jarEntry);
                try (InputStream inputStream = jarFile.getInputStream(jarEntry)) {
                    IOUtils.copyLarge(inputStream, jarOutputStream);
                    jarOutputStream.closeEntry();
                    log("Packed " + jarEntry.getName());
                }
            }
            jarFile.close();
        }
    }

    /**
     * When effort is 0, the packer copies through the original jar input stream without compression
     *
     * @param jarInputStream the jar input stream
     * @param outputStream   the jar output stream
     * @throws IOException If an I/O error occurs.
     */
    public static void copyThroughJar(final JarInputStream jarInputStream, final OutputStream outputStream) throws IOException {
        final Manifest manifest = jarInputStream.getManifest();
        try (JarOutputStream jarOutputStream = new JarOutputStream(outputStream, manifest)) {
            jarOutputStream.setComment("PACK200");
            log("Packed " + JarFile.MANIFEST_NAME);

            JarEntry jarEntry;
            while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
                jarOutputStream.putNextEntry(jarEntry);
                IOUtils.copyLarge(jarInputStream, jarOutputStream);
                log("Packed " + jarEntry.getName());
            }
            jarInputStream.close();
        }
    }

    public static List<PackingFile> getPackingFileListFromJar(final JarFile jarFile, final boolean keepFileOrder) throws IOException {
        final List<PackingFile> packingFileList = new ArrayList<>();
        IOConsumer.forEach(jarFile.stream(), jarEntry -> {
            try (InputStream inputStream = jarFile.getInputStream(jarEntry)) {
                packingFileList.add(new PackingFile(readJarEntry(jarEntry, inputStream), jarEntry));
            }
        });

        // check whether it need reorder packing file list
        if (!keepFileOrder) {
            reorderPackingFiles(packingFileList);
        }
        return packingFileList;
    }

    public static List<PackingFile> getPackingFileListFromJar(final JarInputStream jarInputStream, final boolean keepFileOrder) throws IOException {
        final List<PackingFile> packingFileList = new ArrayList<>();

        // add manifest file
        final Manifest manifest = jarInputStream.getManifest();
        if (manifest != null) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            manifest.write(baos);
            packingFileList.add(new PackingFile(JarFile.MANIFEST_NAME, baos.toByteArray(), 0));
        }

        // add rest of entries in the jar
        JarEntry jarEntry;
        while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
            packingFileList.add(new PackingFile(readJarEntry(jarEntry, jarInputStream), jarEntry));
        }

        // check whether it need reorder packing file list
        if (!keepFileOrder) {
            reorderPackingFiles(packingFileList);
        }
        return packingFileList;
    }

    public static void log(final String message) {
        packingLogger.log(Level.INFO, message);
    }

    private static byte[] readJarEntry(final JarEntry jarEntry, final InputStream inputStream) throws IOException {
        final long size = jarEntry.getSize();
        if (size > Integer.MAX_VALUE) {
            // TODO: Should probably allow this
            throw new IllegalArgumentException("Large Class!");
        }
        // Negative size means unknown size
        return size < 0 ? IOUtils.toByteArray(inputStream) : IOUtils.toByteArray(inputStream, (int) size, IOUtils.DEFAULT_BUFFER_SIZE);
    }

    private static void reorderPackingFiles(final List<PackingFile> packingFileList) {
        final Iterator<PackingFile> iterator = packingFileList.iterator();
        while (iterator.hasNext()) {
            final PackingFile packingFile = iterator.next();
            if (packingFile.isDirectory()) {
                // remove directory entries
                iterator.remove();
            }
        }

        // Sort files by name, "META-INF/MANIFEST.MF" should be put in the 1st
        // position
        packingFileList.sort((arg0, arg1) -> {
            final String fileName0 = arg0.getName();
            final String fileName1 = arg1.getName();
            if (fileName0.equals(fileName1)) {
                return 0;
            }
            if (JarFile.MANIFEST_NAME.equals(fileName0)) {
                return -1;
            }
            if (JarFile.MANIFEST_NAME.equals(fileName1)) {
                return 1;
            }
            return fileName0.compareTo(fileName1);
        });
    }

}
