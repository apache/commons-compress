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
package org.apache.commons.compress.harmony.unpack200;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.harmony.pack200.Pack200Exception;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;

/**
 * Archive is the main entry point to unpack200. An archive is constructed with either two file names, a pack file and an output file name or an input stream
 * and an output streams. Then {@code unpack()} is called, to unpack the pack200 archive.
 */
public class Archive {

    private static final int[] MAGIC = { 0xCA, 0xFE, 0xD0, 0x0D };

    private BoundedInputStream inputStream;

    private final JarOutputStream outputStream;

    private boolean removePackFile;

    private int logLevel = Segment.LOG_LEVEL_STANDARD;

    private FileOutputStream logFile;

    private boolean overrideDeflateHint;

    private boolean deflateHint;

    private final Path inputPath;

    private final long inputSize;

    private final String outputFileName;

    private final boolean closeStreams;

    /**
     * Creates an Archive with streams for the input and output files. Note: If you use this method then calling {@link #setRemovePackFile(boolean)} will have
     * no effect.
     *
     * @param inputStream  the input stream, preferably a {@link BoundedInputStream}. The bound can the the file size.
     * @param outputStream the JAR output stream.
     * @throws IOException if an I/O error occurs
     */
    public Archive(final InputStream inputStream, final JarOutputStream outputStream) throws IOException {
        this.inputStream = Pack200UnpackerAdapter.newBoundedInputStream(inputStream);
        this.outputStream = outputStream;
        if (inputStream instanceof FileInputStream) {
            inputPath = Paths.get(Pack200UnpackerAdapter.readPathString((FileInputStream) inputStream));
        } else {
            inputPath = null;
        }
        this.outputFileName = null;
        this.inputSize = -1;
        this.closeStreams = false;
    }

    /**
     * Creates an Archive with the given input and output file names.
     *
     * @param inputFileName  the input file name.
     * @param outputFileName the output file name
     * @throws FileNotFoundException if the input file does not exist
     * @throws IOException           if an I/O error occurs
     */
    @SuppressWarnings("resource")
    public Archive(final String inputFileName, final String outputFileName) throws FileNotFoundException, IOException {
        this.inputPath = Paths.get(inputFileName);
        this.inputSize = Files.size(this.inputPath);
        this.inputStream = BoundedInputStream.builder().setPath(inputPath).setMaxCount(inputSize).get();
        this.outputStream = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(outputFileName)));
        this.outputFileName = outputFileName;
        this.closeStreams = true;
    }

    private boolean available(final InputStream inputStream) throws IOException {
        inputStream.mark(1);
        final int check = inputStream.read();
        inputStream.reset();
        return check != -1;
    }

    /**
     * Sets the default hint.
     *
     * @param deflateHint the hint value.
     */
    public void setDeflateHint(final boolean deflateHint) {
        overrideDeflateHint = true;
        this.deflateHint = deflateHint;
    }

    /**
     * Sets the lgg file.
     *
     * @param logFileName the log file name.
     * @throws FileNotFoundException if the file exists but is a directory rather than a regular file, does not exist but cannot be created, or cannot be opened
     *                               for any other reason
     */
    public void setLogFile(final String logFileName) throws FileNotFoundException {
        logFile = new FileOutputStream(logFileName);
    }

    /**
     * Sets the lgg file.
     *
     * @param logFileName the log file name.
     * @param append      if {@code true}, then bytes will be written to the end of the file rather than the beginning
     * @throws FileNotFoundException if the file exists but is a directory rather than a regular file, does not exist but cannot be created, or cannot be opened
     *                               for any other reason
     */
    public void setLogFile(final String logFileName, final boolean append) throws FileNotFoundException {
        logFile = new FileOutputStream(logFileName, append);
    }

    /**
     * Sets whether to set the log level to quiet.
     *
     * @param quiet whether to set the log level to quiet.
     */
    public void setQuiet(final boolean quiet) {
        if (quiet || logLevel == Segment.LOG_LEVEL_QUIET) {
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

    /**
     * Sets whether to set the log level to verbose.
     *
     * @param verbose whether to set the log level to verbose.
     */
    public void setVerbose(final boolean verbose) {
        if (verbose) {
            logLevel = Segment.LOG_LEVEL_VERBOSE;
        } else if (logLevel == Segment.LOG_LEVEL_VERBOSE) {
            logLevel = Segment.LOG_LEVEL_STANDARD;
        }
    }

    /**
     * Unpacks the Archive from the input file to the output file.
     *
     * @throws Pack200Exception Never thrown.
     * @throws IOException if an I/O error has occurred.
     */
    public void unpack() throws Pack200Exception, IOException {
        outputStream.setComment("PACK200");
        try {
            if (!inputStream.markSupported()) {
                inputStream = new BoundedInputStream(new BufferedInputStream(inputStream));
                if (!inputStream.markSupported()) {
                    throw new IllegalStateException();
                }
            }
            inputStream.mark(2);
            if ((inputStream.read() & 0xFF | (inputStream.read() & 0xFF) << 8) == GZIPInputStream.GZIP_MAGIC) {
                inputStream.reset();
                inputStream = new BoundedInputStream(new BufferedInputStream(new GZIPInputStream(inputStream)));
            } else {
                inputStream.reset();
            }
            inputStream.mark(MAGIC.length);
            // pack200
            final int[] word = new int[MAGIC.length];
            for (int i = 0; i < word.length; i++) {
                word[i] = inputStream.read();
            }
            boolean compressedWithE0 = false;
            for (int m = 0; m < MAGIC.length; m++) {
                if (word[m] != MAGIC[m]) {
                    compressedWithE0 = true;
                    break;
                }
            }
            inputStream.reset();
            if (compressedWithE0) { // The original Jar was not packed, so just copy it across.
                final JarInputStream jarInputStream = new JarInputStream(inputStream);
                JarEntry jarEntry;
                while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
                    outputStream.putNextEntry(jarEntry);
                    IOUtils.copy(jarInputStream, outputStream, 16_384);
                    outputStream.closeEntry();
                }
            } else {
                int i = 0;
                while (available(inputStream)) {
                    i++;
                    final Segment segment = new Segment();
                    segment.setLogLevel(logLevel);
                    segment.setLogStream(logFile);
                    segment.setPreRead(false);
                    if (i == 1) {
                        segment.log(Segment.LOG_LEVEL_VERBOSE, "Unpacking from " + inputPath + " to " + outputFileName);
                    }
                    segment.log(Segment.LOG_LEVEL_VERBOSE, "Reading segment " + i);
                    if (overrideDeflateHint) {
                        segment.overrideDeflateHint(deflateHint);
                    }
                    segment.unpack(inputStream, outputStream);
                    outputStream.flush();
                }
            }
        } finally {
            if (closeStreams) {
                IOUtils.closeQuietly(inputStream);
                IOUtils.closeQuietly(outputStream);
            }
            IOUtils.closeQuietly(logFile);
        }
        if (removePackFile && inputPath != null) {
            Files.delete(inputPath);
        }
    }

}
