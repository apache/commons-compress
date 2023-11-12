/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.commons.compress.compressors.pack200;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.jar.JarOutputStream;

import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.java.util.jar.Pack200;
import org.apache.commons.compress.utils.CloseShieldFilterInputStream;
import org.apache.commons.compress.utils.IOUtils;

/**
 * An input stream that decompresses from the Pack200 format to be read as any other stream.
 *
 * <p>
 * The {@link CompressorInputStream#getCount getCount} and {@link CompressorInputStream#getBytesRead getBytesRead} methods always return 0.
 * </p>
 *
 * @NotThreadSafe
 * @since 1.3
 */
public class Pack200CompressorInputStream extends CompressorInputStream {

    private static final byte[] CAFE_DOOD = { (byte) 0xCA, (byte) 0xFE, (byte) 0xD0, (byte) 0x0D };
    private static final int SIG_LENGTH = CAFE_DOOD.length;

    /**
     * Checks if the signature matches what is expected for a pack200 file (0xCAFED00D).
     *
     * @param signature the bytes to check
     * @param length    the number of bytes to check
     * @return true, if this stream is a pack200 compressed stream, false otherwise
     */
    public static boolean matches(final byte[] signature, final int length) {
        if (length < SIG_LENGTH) {
            return false;
        }

        for (int i = 0; i < SIG_LENGTH; i++) {
            if (signature[i] != CAFE_DOOD[i]) {
                return false;
            }
        }

        return true;
    }

    private final InputStream originalInputStream;

    private final AbstractStreamBridge abstractStreamBridge;

    /**
     * Decompresses the given file, caching the decompressed data in memory.
     *
     * @param file the file to decompress
     * @throws IOException if reading fails
     */
    public Pack200CompressorInputStream(final File file) throws IOException {
        this(file, Pack200Strategy.IN_MEMORY);
    }

    /**
     * Decompresses the given file, caching the decompressed data in memory and using the given properties.
     *
     * @param file     the file to decompress
     * @param properties Pack200 properties to use
     * @throws IOException if reading fails
     */
    public Pack200CompressorInputStream(final File file, final Map<String, String> properties) throws IOException {
        this(file, Pack200Strategy.IN_MEMORY, properties);
    }

    /**
     * Decompresses the given file using the given strategy to cache the results.
     *
     * @param file    the file to decompress
     * @param mode the strategy to use
     * @throws IOException if reading fails
     */
    public Pack200CompressorInputStream(final File file, final Pack200Strategy mode) throws IOException {
        this(null, file, mode, null);
    }

    /**
     * Decompresses the given file using the given strategy to cache the results and the given properties.
     *
     * @param file     the file to decompress
     * @param mode  the strategy to use
     * @param properties Pack200 properties to use
     * @throws IOException if reading fails
     */
    public Pack200CompressorInputStream(final File file, final Pack200Strategy mode, final Map<String, String> properties) throws IOException {
        this(null, file, mode, properties);
    }

    /**
     * Decompresses the given stream, caching the decompressed data in memory.
     *
     * <p>
     * When reading from a file the File-arg constructor may provide better performance.
     * </p>
     *
     * @param inputStream the InputStream from which this object should be created
     * @throws IOException if reading fails
     */
    public Pack200CompressorInputStream(final InputStream inputStream) throws IOException {
        this(inputStream, Pack200Strategy.IN_MEMORY);
    }

    private Pack200CompressorInputStream(final InputStream inputStream, final File file, final Pack200Strategy mode, final Map<String, String> properties)
            throws IOException {
        this.originalInputStream = inputStream;
        this.abstractStreamBridge = mode.newStreamBridge();
        try (final JarOutputStream jarOut = new JarOutputStream(abstractStreamBridge)) {
            final Pack200.Unpacker unpacker = Pack200.newUnpacker();
            if (properties != null) {
                unpacker.properties().putAll(properties);
            }
            if (file == null) {
                // unpack would close this stream but we want to give the call site more control
                // TODO unpack should not close its given stream.
                try (final CloseShieldFilterInputStream closeShield = new CloseShieldFilterInputStream(inputStream)) {
                    unpacker.unpack(closeShield, jarOut);
                }
            } else {
                unpacker.unpack(file, jarOut);
            }
        }
    }

    /**
     * Decompresses the given stream, caching the decompressed data in memory and using the given properties.
     *
     * <p>
     * When reading from a file the File-arg constructor may provide better performance.
     * </p>
     *
     * @param inputStream    the InputStream from which this object should be created
     * @param properties Pack200 properties to use
     * @throws IOException if reading fails
     */
    public Pack200CompressorInputStream(final InputStream inputStream, final Map<String, String> properties) throws IOException {
        this(inputStream, Pack200Strategy.IN_MEMORY, properties);
    }

    /**
     * Decompresses the given stream using the given strategy to cache the results.
     *
     * <p>
     * When reading from a file the File-arg constructor may provide better performance.
     * </p>
     *
     * @param inputStream   the InputStream from which this object should be created
     * @param mode the strategy to use
     * @throws IOException if reading fails
     */
    public Pack200CompressorInputStream(final InputStream inputStream, final Pack200Strategy mode) throws IOException {
        this(inputStream, null, mode, null);
    }

    /**
     * Decompresses the given stream using the given strategy to cache the results and the given properties.
     *
     * <p>
     * When reading from a file the File-arg constructor may provide better performance.
     * </p>
     *
     * @param inputStream    the InputStream from which this object should be created
     * @param mode  the strategy to use
     * @param properties Pack200 properties to use
     * @throws IOException if reading fails
     */
    public Pack200CompressorInputStream(final InputStream inputStream, final Pack200Strategy mode, final Map<String, String> properties) throws IOException {
        this(inputStream, null, mode, properties);
    }

    @SuppressWarnings("resource") // Does not allocate
    @Override
    public int available() throws IOException {
        return getInputStream().available();
    }

    @Override
    public void close() throws IOException {
        try {
            abstractStreamBridge.stop();
        } finally {
            if (originalInputStream != null) {
                originalInputStream.close();
            }
        }
    }

    private InputStream getInputStream() throws IOException {
        return abstractStreamBridge.getInputStream();
    }

    @SuppressWarnings("resource") // Does not allocate
    @Override
    public synchronized void mark(final int limit) {
        try {
            getInputStream().mark(limit);
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex); // NOSONAR
        }
    }

    @SuppressWarnings("resource") // Does not allocate
    @Override
    public boolean markSupported() {
        try {
            return getInputStream().markSupported();
        } catch (final IOException ex) { // NOSONAR
            return false;
        }
    }

    @SuppressWarnings("resource") // Does not allocate
    @Override
    public int read() throws IOException {
        return getInputStream().read();
    }

    @SuppressWarnings("resource") // Does not allocate
    @Override
    public int read(final byte[] b) throws IOException {
        return getInputStream().read(b);
    }

    @SuppressWarnings("resource") // Does not allocate
    @Override
    public int read(final byte[] b, final int off, final int count) throws IOException {
        return getInputStream().read(b, off, count);
    }

    @SuppressWarnings("resource") // Does not allocate
    @Override
    public synchronized void reset() throws IOException {
        getInputStream().reset();
    }

    @SuppressWarnings("resource") // Does not allocate
    @Override
    public long skip(final long count) throws IOException {
        return IOUtils.skip(getInputStream(), count);
    }
}
