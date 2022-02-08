/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.commons.compress.harmony.pack200;

import java.io.BufferedInputStream;
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

public class PackingUtils {

    private static PackingLogger packingLogger;

    static {
        packingLogger = new PackingLogger("org.harmony.apache.pack200", null);
        LogManager.getLogManager().addLogger(packingLogger);
    }

    private static class PackingLogger extends Logger {

        private boolean verbose = false;

        protected PackingLogger(final String name, final String resourceBundleName) {
            super(name, resourceBundleName);
        }

        @Override
        public void log(final LogRecord logRecord) {
            if (verbose) {
                super.log(logRecord);
            }
        }

        public void setVerbose(final boolean isVerbose) {
            verbose = isVerbose;
        }
    }

    public static void config(final PackingOptions options) throws IOException {
        final String logFileName = options.getLogFile();
        if (logFileName != null) {
            final FileHandler fileHandler = new FileHandler(logFileName, false);
            fileHandler.setFormatter(new SimpleFormatter());
            packingLogger.addHandler(fileHandler);
            packingLogger.setUseParentHandlers(false);
        }

        packingLogger.setVerbose(options.isVerbose());
    }

    public static void log(final String message) {
        packingLogger.log(Level.INFO, message);
    }

    /**
     * When effort is 0, the packer copies through the original jar input stream without compression
     *
     * @param jarInputStream the jar input stream
     * @param outputStream the jar output stream
     * @throws IOException If an I/O error occurs.
     */
    public static void copyThroughJar(final JarInputStream jarInputStream, final OutputStream outputStream)
        throws IOException {
        final Manifest manifest = jarInputStream.getManifest();
        final JarOutputStream jarOutputStream = new JarOutputStream(outputStream, manifest);
        jarOutputStream.setComment("PACK200");
        log("Packed " + JarFile.MANIFEST_NAME);

        final byte[] bytes = new byte[16384];
        JarEntry jarEntry;
        int bytesRead;
        while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
            jarOutputStream.putNextEntry(jarEntry);
            while ((bytesRead = jarInputStream.read(bytes)) != -1) {
                jarOutputStream.write(bytes, 0, bytesRead);
            }
            log("Packed " + jarEntry.getName());
        }
        jarInputStream.close();
        jarOutputStream.close();
    }

    /**
     * When effort is 0, the packer copys through the original jar file without compression
     *
     * @param jarFile the input jar file
     * @param outputStream the jar output stream
     * @throws IOException If an I/O error occurs.
     */
    public static void copyThroughJar(final JarFile jarFile, final OutputStream outputStream) throws IOException {
        final JarOutputStream jarOutputStream = new JarOutputStream(outputStream);
        jarOutputStream.setComment("PACK200");
        final byte[] bytes = new byte[16384];
        final Enumeration entries = jarFile.entries();
        InputStream inputStream;
        JarEntry jarEntry;
        int bytesRead;
        while (entries.hasMoreElements()) {
            jarEntry = (JarEntry) entries.nextElement();
            jarOutputStream.putNextEntry(jarEntry);
            inputStream = jarFile.getInputStream(jarEntry);
            while ((bytesRead = inputStream.read(bytes)) != -1) {
                jarOutputStream.write(bytes, 0, bytesRead);
            }
            jarOutputStream.closeEntry();
            log("Packed " + jarEntry.getName());
        }
        jarFile.close();
        jarOutputStream.close();
    }

    public static List getPackingFileListFromJar(final JarInputStream jarInputStream, final boolean keepFileOrder)
        throws IOException {
        final List packingFileList = new ArrayList();

        // add manifest file
        final Manifest manifest = jarInputStream.getManifest();
        if (manifest != null) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            manifest.write(baos);
            packingFileList.add(new PackingFile(JarFile.MANIFEST_NAME, baos.toByteArray(), 0));
        }

        // add rest of entries in the jar
        JarEntry jarEntry;
        byte[] bytes;
        while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
            bytes = readJarEntry(jarEntry, new BufferedInputStream(jarInputStream));
            packingFileList.add(new PackingFile(bytes, jarEntry));
        }

        // check whether it need reorder packing file list
        if (!keepFileOrder) {
            reorderPackingFiles(packingFileList);
        }
        return packingFileList;
    }

    public static List getPackingFileListFromJar(final JarFile jarFile, final boolean keepFileOrder)
        throws IOException {
        final List packingFileList = new ArrayList();
        final Enumeration jarEntries = jarFile.entries();
        JarEntry jarEntry;
        byte[] bytes;
        while (jarEntries.hasMoreElements()) {
            jarEntry = (JarEntry) jarEntries.nextElement();
            bytes = readJarEntry(jarEntry, new BufferedInputStream(jarFile.getInputStream(jarEntry)));
            packingFileList.add(new PackingFile(bytes, jarEntry));
        }

        // check whether it need reorder packing file list
        if (!keepFileOrder) {
            reorderPackingFiles(packingFileList);
        }
        return packingFileList;
    }

    private static byte[] readJarEntry(final JarEntry jarEntry, final InputStream inputStream) throws IOException {
        long size = jarEntry.getSize();
        if (size > Integer.MAX_VALUE) {
            // TODO: Should probably allow this
            throw new RuntimeException("Large Class!");
        }
        if (size < 0) {
            size = 0;
        }
        final byte[] bytes = new byte[(int) size];
        if (inputStream.read(bytes) != size) {
            throw new RuntimeException("Error reading from stream");
        }
        return bytes;
    }

    private static void reorderPackingFiles(final List packingFileList) {
        final Iterator iterator = packingFileList.iterator();
        PackingFile packingFile;
        while (iterator.hasNext()) {
            packingFile = (PackingFile) iterator.next();
            if (packingFile.isDirectory()) {
                // remove directory entries
                iterator.remove();
            }
        }

        // Sort files by name, "META-INF/MANIFEST.MF" should be put in the 1st
        // position
        packingFileList.sort((arg0, arg1) -> {
            if (arg0 instanceof PackingFile && arg1 instanceof PackingFile) {
                final String fileName0 = ((PackingFile) arg0).getName();
                final String fileName1 = ((PackingFile) arg1).getName();
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
            }
            throw new IllegalArgumentException();
        });
    }

}