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
package org.apache.commons.compress.harmony.unpack200;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.harmony.pack200.Pack200Exception;

/**
 * Archive is the main entry point to unpack200. An archive is constructed with either two file names, a pack file and
 * an output file name or an input stream and an output streams. Then {@code unpack()} is called, to unpack the
 * pack200 archive.
 */
public class Archive {

    private InputStream inputStream;

    private final JarOutputStream outputStream;

    private boolean removePackFile;

    private int logLevel = Segment.LOG_LEVEL_STANDARD;

    private FileOutputStream logFile;

    private boolean overrideDeflateHint;

    private boolean deflateHint;

    private String inputFileName;

    private String outputFileName;

    /**
     * Creates an Archive with streams for the input and output files. Note: If you use this method then calling
     * {@link #setRemovePackFile(boolean)} will have no effect.
     *
     * @param inputStream TODO
     * @param outputStream TODO
     */
    public Archive(final InputStream inputStream, final JarOutputStream outputStream) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }

    /**
     * Creates an Archive with the given input and output file names.
     *
     * @param inputFile TODO
     * @param outputFile TODO
     * @throws FileNotFoundException if the input file does not exist
     * @throws FileNotFoundException TODO
     * @throws IOException TODO
     */
    public Archive(final String inputFile, final String outputFile) throws FileNotFoundException, IOException {
        this.inputFileName = inputFile;
        this.outputFileName = outputFile;
        inputStream = new FileInputStream(inputFile);
        outputStream = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)));
    }

    private boolean available(final InputStream inputStream) throws IOException {
        inputStream.mark(1);
        final int check = inputStream.read();
        inputStream.reset();
        return check != -1;
    }

    public void setDeflateHint(final boolean deflateHint) {
        overrideDeflateHint = true;
        this.deflateHint = deflateHint;
    }

    public void setLogFile(final String logFileName) throws FileNotFoundException {
        this.logFile = new FileOutputStream(logFileName);
    }

    public void setLogFile(final String logFileName, final boolean append) throws FileNotFoundException {
        logFile = new FileOutputStream(logFileName, append);
    }

    public void setQuiet(final boolean quiet) {
        if (quiet || (logLevel == Segment.LOG_LEVEL_QUIET)) {
            logLevel = Segment.LOG_LEVEL_QUIET;
        }
    }

    /**
     * If removePackFile is set to true, the input file is deleted after unpacking.
     *
     * @param removePackFile If true, the input file is deleted after unpacking.
     */
    public void setRemovePackFile(final boolean removePackFile) {
        this.removePackFile = removePackFile;
    }

    public void setVerbose(final boolean verbose) {
        if (verbose) {
            logLevel = Segment.LOG_LEVEL_VERBOSE;
        } else if (logLevel == Segment.LOG_LEVEL_VERBOSE) {
            logLevel = Segment.LOG_LEVEL_STANDARD;
        }
    }

    /**
     * Unpacks the Archive from the input file to the output file
     *
     * @throws Pack200Exception TODO
     * @throws IOException TODO
     */
    public void unpack() throws Pack200Exception, IOException {
        outputStream.setComment("PACK200");
        try {
            if (!inputStream.markSupported()) {
                inputStream = new BufferedInputStream(inputStream);
                if (!inputStream.markSupported()) {
                    throw new IllegalStateException();
                }
            }
            inputStream.mark(2);
            if (((inputStream.read() & 0xFF) | (inputStream.read() & 0xFF) << 8) == GZIPInputStream.GZIP_MAGIC) {
                inputStream.reset();
                inputStream = new BufferedInputStream(new GZIPInputStream(inputStream));
            } else {
                inputStream.reset();
            }
            inputStream.mark(4);
            final int[] magic = {0xCA, 0xFE, 0xD0, 0x0D}; // Magic word for
            // pack200
            final int[] word = new int[4];
            for (int i = 0; i < word.length; i++) {
                word[i] = inputStream.read();
            }
            boolean compressedWithE0 = false;
            for (int m = 0; m < magic.length; m++) {
                if (word[m] != magic[m]) {
                    compressedWithE0 = true;
                }
            }
            inputStream.reset();
            if (compressedWithE0) { // The original Jar was not packed, so just
                // copy it across
                final JarInputStream jarInputStream = new JarInputStream(inputStream);
                JarEntry jarEntry;
                while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
                    outputStream.putNextEntry(jarEntry);
                    final byte[] bytes = new byte[16384];
                    int bytesRead = jarInputStream.read(bytes);
                    while (bytesRead != -1) {
                        outputStream.write(bytes, 0, bytesRead);
                        bytesRead = jarInputStream.read(bytes);
                    }
                    outputStream.closeEntry();
                }
            } else {
                int i = 0;
                while (available(inputStream)) {
                    i++;
                    final Segment segment = new Segment();
                    segment.setLogLevel(logLevel);
                    segment.setLogStream(logFile != null ? (OutputStream) logFile : (OutputStream) System.out);
                    segment.setPreRead(false);

                    if (i == 1) {
                        segment.log(Segment.LOG_LEVEL_VERBOSE,
                            "Unpacking from " + inputFileName + " to " + outputFileName);
                    }
                    segment.log(Segment.LOG_LEVEL_VERBOSE, "Reading segment " + i);
                    if (overrideDeflateHint) {
                        segment.overrideDeflateHint(deflateHint);
                    }
                    segment.unpack(inputStream, outputStream);
                    outputStream.flush();

                    if (inputStream instanceof FileInputStream) {
                        inputFileName = ((FileInputStream) inputStream).getFD().toString();
                    }
                }
            }
        } finally {
            try {
                inputStream.close();
            } catch (final Exception e) {
            }
            try {
                outputStream.close();
            } catch (final Exception e) {
            }
            if (logFile != null) {
                try {
                    logFile.close();
                } catch (final Exception e) {
                }
            }
        }
        if (removePackFile) {
            boolean deleted = false;
            if (inputFileName != null) {
                final File file = new File(inputFileName);
                deleted = file.delete();
            }
            if (!deleted) {
                throw new Pack200Exception("Failed to delete the input file.");
            }
        }
    }

}