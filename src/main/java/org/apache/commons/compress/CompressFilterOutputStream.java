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
package org.apache.commons.compress;

import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Abstracts classes that compress or archive an output stream.
 *
 * @param <T> The underlying {@link OutputStream} type.
 * @since 1.28.0
 */
public abstract class CompressFilterOutputStream<T extends OutputStream> extends FilterOutputStream {

    /**
     * Writes and filters the bytes from the specified String to this output stream using the given Charset.
     *
     * @param os      the target output stream.
     * @param data    the data.
     * @param charset The {@link Charset} to be used to encode the {@code String}
     * @return the ASCII bytes.
     * @exception IOException if an I/O error occurs.
     * @see OutputStream#write(byte[])
     */
    private static byte[] write(final OutputStream os, final String data, final Charset charset) throws IOException {
        final byte[] bytes = data.getBytes(charset);
        os.write(bytes);
        return bytes;
    }

    /**
     * Constructs a new instance without a backing {@link OutputStream}.
     * <p>
     * You must initialize {@code this.out} after construction.
     * </p>
     */
    public CompressFilterOutputStream() {
        super(null);
    }

    /**
     * Creates an output stream filter built on top of the specified underlying {@link OutputStream}.
     *
     * @param out the underlying output stream to be assigned to the field {@code this.out} for later use, or {@code null} if this instance is to be created
     *            without an underlying stream.
     */
    public CompressFilterOutputStream(final T out) {
        super(out);
    }

    /**
     * Gets the underlying output stream.
     *
     * @return the underlying output stream.
     */
    @SuppressWarnings("unchecked")
    protected T out() {
        return (T) out;
    }

    /**
     * Writes all bytes from a file this output stream.
     *
     * @param file the path to the source file.
     * @return the number of bytes read or written.
     * @throws IOException if an I/O error occurs when reading or writing.
     */
    public long write(final File file) throws IOException {
        return write(file.toPath());
    }

    /**
     * Writes all bytes from a file to this output stream.
     *
     * @param path the path to the source file.
     * @return the number of bytes read or written.
     * @throws IOException if an I/O error occurs when reading or writing.
     */
    public long write(final Path path) throws IOException {
        return Files.copy(path, this);
    }

    /**
     * Writes and filters the ASCII bytes from the specified String to this output stream.
     *
     * @param data the data.
     * @return the ASCII bytes.
     * @exception IOException if an I/O error occurs.
     * @see OutputStream#write(byte[])
     */
    public byte[] writeUsAscii(final String data) throws IOException {
        return write(this, data, StandardCharsets.US_ASCII);
    }

    /**
     * Writes the raw ASCII bytes from the specified String to this output stream.
     *
     * @param data the data.
     * @return the ASCII bytes.
     * @exception IOException if an I/O error occurs.
     * @see OutputStream#write(byte[])
     */
    public byte[] writeUsAsciiRaw(final String data) throws IOException {
        return write(out, data, StandardCharsets.US_ASCII);
    }

    /**
     * Writes and filters the UTF-8 bytes from the specified String to this output stream.
     *
     * @param data the data.
     * @return the ASCII bytes.
     * @exception IOException if an I/O error occurs.
     * @see OutputStream#write(byte[])
     */
    public byte[] writeUtf8(final String data) throws IOException {
        return write(this, data, StandardCharsets.UTF_8);
    }
}
