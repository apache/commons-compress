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
package org.apache.commons.compress.parallel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * ScatterGatherBackingStore that is backed by a path.
 *
 * @since 1.10
 */
public class FileBasedScatterGatherBackingStore implements ScatterGatherBackingStore {
    private final Path target;
    private final OutputStream outputStream;
    private boolean closed;

    public FileBasedScatterGatherBackingStore(final File target) throws FileNotFoundException {
        this(target.toPath());
    }

    /**
     * Constructs a new instance for the given path.
     *
     * @param target The path to offload compressed data into.
     * @throws FileNotFoundException if the file doesn't exist
     * @since 1.22
     */
    public FileBasedScatterGatherBackingStore(final Path target) throws FileNotFoundException {
        this.target = target;
        try {
            outputStream = Files.newOutputStream(target);
        } catch (final FileNotFoundException ex) {
            throw ex;
        } catch (final IOException ex) {
            // must convert exception to stay backwards compatible with Compress 1.10 to 1.13
            throw new UncheckedIOException(ex); // NOSONAR
        }
    }

    @Override
    public void close() throws IOException {
        try {
            closeForWriting();
        } finally {
            Files.deleteIfExists(target);
        }
    }

    @Override
    public void closeForWriting() throws IOException {
        if (!closed) {
            outputStream.close();
            closed = true;
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return Files.newInputStream(target);
    }

    @Override
    public void writeOut(final byte[] data, final int offset, final int length) throws IOException {
        outputStream.write(data, offset, length);
    }
}
