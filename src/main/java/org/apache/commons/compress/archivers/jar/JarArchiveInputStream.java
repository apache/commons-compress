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
package org.apache.commons.compress.archivers.jar;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;

/**
 * Implements an input stream that can read entries from jar files.
 *
 * @NotThreadSafe
 */
public class JarArchiveInputStream extends ZipArchiveInputStream {

    /**
     * Builds a new {@link JarArchiveInputStream}.
     * <p>
     *     For example:
     * </p>
     * <pre>{@code
     * JarArchiveInputStream in = JarArchiveInputStream.builder()
     *     .setPath(inputPath)
     *     .setCharset(StandardCharsets.UTF_8)
     *     .setUseUnicodeExtraFields(false)
     *     .get();
     * }</pre>
     * @since 1.29.0
     */
    public static class Builder extends ZipArchiveInputStream.AbstractBuilder<JarArchiveInputStream, Builder> {
        @Override
        public JarArchiveInputStream get() throws IOException {
            return new JarArchiveInputStream(this);
        }
    }

    /**
     * Checks if the signature matches what is expected for a jar file (in this case it is the same as for a ZIP file).
     *
     * @param signature the bytes to check
     * @param length    the number of bytes to check
     * @return true, if this stream is a jar archive stream, false otherwise
     */
    public static boolean matches(final byte[] signature, final int length) {
        return ZipArchiveInputStream.matches(signature, length);
    }

    /**
     * Creates a new builder.
     *
     * @return A new builder
     * @since 1.29.0
     */
    public static Builder jarInputStreamBuilder() {
        return new Builder();
    }

    /**
     * Creates an instance from the input stream using the default encoding.
     *
     * @param inputStream the input stream to wrap
     */
    public JarArchiveInputStream(final InputStream inputStream) {
        super(inputStream);
    }

    /**
     * Creates an instance from the input stream using the specified encoding.
     *
     * @param inputStream the input stream to wrap
     * @param encoding    the encoding to use
     * @since 1.10
     */
    public JarArchiveInputStream(final InputStream inputStream, final String encoding) {
        super(inputStream, encoding);
    }

    private JarArchiveInputStream(final Builder builder) throws IOException {
        super(builder);
    }

    @Override
    public JarArchiveEntry getNextEntry() throws IOException {
        return getNextJarEntry();
    }

    /**
     * Gets the next entry.
     *
     * @return the next entry.
     * @throws IOException if an I/O error occurs.
     * @deprecated Use {@link #getNextEntry()}.
     */
    @Deprecated
    public JarArchiveEntry getNextJarEntry() throws IOException {
        final ZipArchiveEntry entry = getNextZipEntry();
        return entry == null ? null : new JarArchiveEntry(entry);
    }
}
