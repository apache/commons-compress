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
import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarOutputStream;

import org.apache.commons.compress.harmony.pack200.Pack200Adapter;
import org.apache.commons.compress.harmony.pack200.Pack200Exception;
import org.apache.commons.compress.java.util.jar.Pack200.Unpacker;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.io.input.CloseShieldInputStream;

/**
 * This class provides the binding between the standard Pack200 interface and the internal interface for (un)packing.
 */
public class Pack200UnpackerAdapter extends Pack200Adapter implements Unpacker {

    /**
     * Creates a new BoundedInputStream bound by the size of the given file.
     * <p>
     * The new BoundedInputStream wraps a new {@link BufferedInputStream}.
     * </p>
     *
     * @param file The file.
     * @return a new BoundedInputStream
     * @throws IOException if an I/O error occurs
     */
    static BoundedInputStream newBoundedInputStream(final File file) throws IOException {
        return newBoundedInputStream(file.toPath());
    }

    private static BoundedInputStream newBoundedInputStream(final FileInputStream fileInputStream) throws IOException {
        return newBoundedInputStream(readPath(fileInputStream));
    }

    @SuppressWarnings("resource") // Caller closes.
    static BoundedInputStream newBoundedInputStream(final InputStream inputStream) throws IOException {
        if (inputStream instanceof BoundedInputStream) {
            // Already bound.
            return (BoundedInputStream) inputStream;
        }
        if (inputStream instanceof CloseShieldInputStream) {
            // Don't unwrap to keep close shield.
            return newBoundedInputStream(BoundedInputStream.builder().setInputStream(inputStream).get());
        }
        if (inputStream instanceof FilterInputStream) {
            return newBoundedInputStream(unwrap((FilterInputStream) inputStream));
        }
        if (inputStream instanceof FileInputStream) {
            return newBoundedInputStream((FileInputStream) inputStream);
        }
        // No limit
        return newBoundedInputStream(BoundedInputStream.builder().setInputStream(inputStream).get());
    }

    /**
     * Creates a new BoundedInputStream bound by the size of the given path.
     * <p>
     * The new BoundedInputStream wraps a new {@link BufferedInputStream}.
     * </p>
     *
     * @param path The path.
     * @return a new BoundedInputStream
     * @throws IOException if an I/O error occurs
     */
    @SuppressWarnings("resource") // Caller closes.
    static BoundedInputStream newBoundedInputStream(final Path path) throws IOException {
        // @formatter:off
        return BoundedInputStream.builder()
                .setInputStream(new BufferedInputStream(Files.newInputStream(path)))
                .setMaxCount(Files.size(path))
                .setPropagateClose(false)
                .get();
        // @formatter:on
    }

    /**
     * Creates a new BoundedInputStream bound by the size of the given file.
     * <p>
     * The new BoundedInputStream wraps a new {@link BufferedInputStream}.
     * </p>
     *
     * @param first the path string or initial part of the path string.
     * @param more  additional strings to be joined to form the path string.
     * @return a new BoundedInputStream
     * @throws IOException if an I/O error occurs
     */
    static BoundedInputStream newBoundedInputStream(final String first, final String... more) throws IOException {
        return newBoundedInputStream(Paths.get(first, more));
    }

    /**
     * Creates a new BoundedInputStream bound by the size of the given URL to a file.
     * <p>
     * The new BoundedInputStream wraps a new {@link BufferedInputStream}.
     * </p>
     *
     * @param url The URL.
     * @return a new BoundedInputStream
     * @throws IOException        if an I/O error occurs
     * @throws URISyntaxException
     */
    static BoundedInputStream newBoundedInputStream(final URL url) throws IOException, URISyntaxException {
        return newBoundedInputStream(Paths.get(url.toURI()));
    }

    static Path readPath(final FileInputStream fis) {
        try {
            Field field = FileInputStream.class.getDeclaredField("path");
            field.setAccessible(true);
            return Paths.get((String) field.get(fis));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to read the path from the FileInputStream", e);
        }
    }

    /**
     * Unwraps the given FilterInputStream to return its wrapped InputStream.
     *
     * @param filterInputStream The FilterInputStream to unwrap.
     * @return The wrapped InputStream
     */
    static InputStream unwrap(final FilterInputStream filterInputStream) {
        try {
            Field field = FilterInputStream.class.getDeclaredField("in");
            field.setAccessible(true);
            return (InputStream) field.get(filterInputStream);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to unwrap the FilterInputStream", e);
        }
    }

    /**
     * Unwraps the given InputStream if it is an FilterInputStream to return its wrapped InputStream.
     *
     * @param inputStream The FilterInputStream to unwrap.
     * @return The wrapped InputStream
     */
    static InputStream unwrap(final InputStream inputStream) {
        return inputStream instanceof FilterInputStream ? unwrap((FilterInputStream) inputStream) : inputStream;
    }

    @Override
    public void unpack(final File file, final JarOutputStream out) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("Must specify input file.");
        }
        if (out == null) {
            throw new IllegalArgumentException("Must specify output stream.");
        }
        final long size = file.length();
        final int bufferSize = size > 0 && size < DEFAULT_BUFFER_SIZE ? (int) size : DEFAULT_BUFFER_SIZE;
        try (InputStream in = new BufferedInputStream(Files.newInputStream(file.toPath()), bufferSize)) {
            unpack(in, out);
        }
    }

    @Override
    public void unpack(final InputStream in, final JarOutputStream out) throws IOException {
        if (in == null) {
            throw new IllegalArgumentException("Must specify input stream.");
        }
        if (out == null) {
            throw new IllegalArgumentException("Must specify output stream.");
        }
        completed(0);
        try {
            new Archive(in, out).unpack();
        } catch (final Pack200Exception e) {
            throw new IOException("Failed to unpack Jar:" + e);
        }
        completed(1);
    }
}
