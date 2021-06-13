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
import java.util.Collections;
import java.util.Comparator;
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

        protected PackingLogger(String name, String resourceBundleName) {
            super(name, resourceBundleName);
        }

        public void log(LogRecord logRecord) {
            if (verbose) {
                super.log(logRecord);
            }
        }

        public void setVerbose(boolean isVerbose) {
            verbose = isVerbose;
        }
    }

    public static void config(PackingOptions options) throws IOException {
        String logFileName = options.getLogFile();
        if (logFileName != null) {
            FileHandler fileHandler = new FileHandler(logFileName, false);
            fileHandler.setFormatter(new SimpleFormatter());
            packingLogger.addHandler(fileHandler);
            packingLogger.setUseParentHandlers(false);
        }

        packingLogger.setVerbose(options.isVerbose());
    }

    public static void log(String message) {
        packingLogger.log(Level.INFO, message);
    }

    /**
     * When effort is 0, the packer copies through the original jar input stream
     * without compression
     * 
     * @param jarInputStream
     *            the jar input stream
     * @param outputStream
     *            the jar output stream
     * @throws IOException If an I/O error occurs.
     */
    public static void copyThroughJar(JarInputStream jarInputStream,
            OutputStream outputStream) throws IOException {
        Manifest manifest = jarInputStream.getManifest();
        JarOutputStream jarOutputStream = new JarOutputStream(outputStream,
                manifest);
        jarOutputStream.setComment("PACK200");
        log("Packed " + JarFile.MANIFEST_NAME);

        byte[] bytes = new byte[16384];
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
     * When effort is 0, the packer copys through the original jar file without
     * compression
     * 
     * @param jarFile
     *            the input jar file
     * @param outputStream
     *            the jar output stream
     * @throws IOException If an I/O error occurs.
     */
    public static void copyThroughJar(JarFile jarFile, OutputStream outputStream)
            throws IOException {
        JarOutputStream jarOutputStream = new JarOutputStream(outputStream);
        jarOutputStream.setComment("PACK200");
        byte[] bytes = new byte[16384];
        Enumeration entries = jarFile.entries();
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

    public static List getPackingFileListFromJar(JarInputStream jarInputStream,
            boolean keepFileOrder) throws IOException {
        List packingFileList = new ArrayList();

        // add manifest file
        Manifest manifest = jarInputStream.getManifest();
        if (manifest != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            manifest.write(baos);
            packingFileList.add(new PackingFile(JarFile.MANIFEST_NAME, baos
                    .toByteArray(), 0));
        }

        // add rest of entries in the jar
        JarEntry jarEntry;
        byte[] bytes;
        while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
            bytes = readJarEntry(jarEntry, new BufferedInputStream(
                    jarInputStream));
            packingFileList.add(new PackingFile(bytes, jarEntry));
        }

        // check whether it need reorder packing file list
        if (!keepFileOrder) {
            reorderPackingFiles(packingFileList);
        }
        return packingFileList;
    }

    public static List getPackingFileListFromJar(JarFile jarFile,
            boolean keepFileOrder) throws IOException {
        List packingFileList = new ArrayList();
        Enumeration jarEntries = jarFile.entries();
        JarEntry jarEntry;
        byte[] bytes;
        while (jarEntries.hasMoreElements()) {
            jarEntry = (JarEntry) jarEntries.nextElement();
            bytes = readJarEntry(jarEntry, new BufferedInputStream(jarFile
                    .getInputStream(jarEntry)));
            packingFileList.add(new PackingFile(bytes, jarEntry));
        }

        // check whether it need reorder packing file list
        if (!keepFileOrder) {
            reorderPackingFiles(packingFileList);
        }
        return packingFileList;
    }

    private static byte[] readJarEntry(JarEntry jarEntry,
            InputStream inputStream) throws IOException {
        long size = jarEntry.getSize();
        if (size > Integer.MAX_VALUE) {
            // TODO: Should probably allow this
            throw new RuntimeException("Large Class!");
        } else if (size < 0) {
            size = 0;
        }
        byte[] bytes = new byte[(int) size];
        if (inputStream.read(bytes) != size) {
            throw new RuntimeException("Error reading from stream");
        }
        return bytes;
    }

    private static void reorderPackingFiles(List packingFileList) {
        Iterator iterator = packingFileList.iterator();
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
        Collections.sort(packingFileList, new Comparator() {
            public int compare(Object arg0, Object arg1) {
                if (arg0 instanceof PackingFile && arg1 instanceof PackingFile) {
                    String fileName0 = ((PackingFile) arg0).getName();
                    String fileName1 = ((PackingFile) arg1).getName();
                    if (fileName0.equals(fileName1)) {
                        return 0;
                    } else if (JarFile.MANIFEST_NAME.equals(fileName0)) {
                        return -1;
                    } else if (JarFile.MANIFEST_NAME.equals(fileName1)) {
                        return 1;
                    }
                    return fileName0.compareTo(fileName1);
                }
                throw new IllegalArgumentException();
            }
        });
    }

}