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
package org.apache.commons.compress.archivers;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.input.NullInputStream;

/**
 * Archive input streams <b>MUST</b> override the {@link #read(byte[], int, int)} - or {@link #read()} - method so that reading from the stream generates EOF
 * for the end of data in each entry as well as at the end of the file proper.
 * <p>
 * The {@link #getNextEntry()} method is used to reset the input stream ready for reading the data from the next entry.
 * </p>
 * <p>
 * The input stream classes must also implement a method with the signature:
 * </p>
 * <pre>
 * public static boolean matches(byte[] signature, int length)
 * </pre>
 * <p>
 * which is used by the {@link ArchiveStreamFactory} to autodetect the archive type from the first few bytes of a stream.
 * </p>
 *
 * @param <E> The type of {@link ArchiveEntry} produced.
 */
public abstract class ArchiveInputStream<E extends ArchiveEntry> extends FilterInputStream {

    private static final int BYTE_MASK = 0xFF;

    private final byte[] single = new byte[1];

    /** The number of bytes read in this stream */
    private long bytesRead;

    private Charset charset;

    /**
     * Constructs a new instance.
     */
    public ArchiveInputStream() {
        this(NullInputStream.INSTANCE, Charset.defaultCharset());
    }

    /**
     * Constructs a new instance.
     *
     * @param inputStream the underlying input stream, or {@code null} if this instance is to be created without an underlying stream.
     * @param charset charset.
     * @since 1.26.0
     */
    // This will be protected once subclasses use builders.
    private ArchiveInputStream(final InputStream inputStream, final Charset charset) {
        super(inputStream);
        this.charset = Charsets.toCharset(charset);
    }

    /**
     * Constructs a new instance.
     *
     * @param inputStream the underlying input stream, or {@code null} if this instance is to be created without an underlying stream.
     * @param charsetName charset name.
     * @since 1.26.0
     */
    protected ArchiveInputStream(final InputStream inputStream, final String charsetName) {
        this(inputStream, Charsets.toCharset(charsetName));
    }

    /**
     * Whether this stream is able to read the given entry.
     * <p>
     * Some archive formats support variants or details that are not supported (yet).
     * </p>
     *
     * @param archiveEntry the entry to test
     * @return This implementation always returns true.
     *
     * @since 1.1
     */
    public boolean canReadEntryData(final ArchiveEntry archiveEntry) {
        return true;
    }

    /**
     * Increments the counter of already read bytes. Doesn't increment if the EOF has been hit (read == -1)
     *
     * @param read the number of bytes read
     */
    protected void count(final int read) {
        count((long) read);
    }

    /**
     * Increments the counter of already read bytes. Doesn't increment if the EOF has been hit (read == -1)
     *
     * @param read the number of bytes read
     * @since 1.1
     */
    protected void count(final long read) {
        if (read != -1) {
            bytesRead += read;
        }
    }

    /**
     * Gets the current number of bytes read from this stream.
     *
     * @return the number of read bytes
     * @since 1.1
     */
    public long getBytesRead() {
        return bytesRead;
    }

    /**
     * Gets the Charest.
     *
     * @return the Charest.
     */
    public Charset getCharset() {
        return charset;
    }

    /**
     * Gets the current number of bytes read from this stream.
     *
     * @return the number of read bytes
     * @deprecated this method may yield wrong results for large archives, use {@link #getBytesRead()} instead.
     */
    @Deprecated
    public int getCount() {
        return (int) bytesRead;
    }

    /**
     * Gets the next Archive Entry in this Stream.
     *
     * @return the next entry, or {@code null} if there are no more entries
     * @throws IOException if the next entry could not be read
     */
    public abstract E getNextEntry() throws IOException;

    /**
     * Decrements the counter of already read bytes.
     *
     * @param pushedBack the number of bytes pushed back.
     * @since 1.1
     */
    protected void pushedBackBytes(final long pushedBack) {
        bytesRead -= pushedBack;
    }

    /**
     * Reads a byte of data. This method will block until enough input is available.
     *
     * Simply calls the {@link #read(byte[], int, int)} method.
     *
     * MUST be overridden if the {@link #read(byte[], int, int)} method is not overridden; may be overridden otherwise.
     *
     * @return the byte read, or -1 if end of input is reached
     * @throws IOException if an I/O error has occurred
     */
    @Override
    public int read() throws IOException {
        final int num = read(single, 0, 1);
        return num == -1 ? -1 : single[0] & BYTE_MASK;
    }

}
