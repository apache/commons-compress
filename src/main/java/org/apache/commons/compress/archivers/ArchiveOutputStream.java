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

import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.LinkOption;
import java.nio.file.Path;

/**
 * Archive output stream implementations are expected to override the {@link #write(byte[], int, int)} method to improve performance. They should also override
 * {@link #close()} to ensure that any necessary trailers are added.
 *
 * <p>
 * The normal sequence of calls when working with ArchiveOutputStreams is:
 * </p>
 * <ul>
 * <li>Create ArchiveOutputStream object,</li>
 * <li>optionally write SFX header (Zip only),</li>
 * <li>repeat as needed:
 * <ul>
 * <li>{@link #putArchiveEntry(ArchiveEntry)} (writes entry header),
 * <li>{@link #write(byte[])} (writes entry data, as often as needed),
 * <li>{@link #closeArchiveEntry()} (closes entry),
 * </ul>
 * </li>
 * <li>{@link #finish()} (ends the addition of entries),</li>
 * <li>optionally write additional data, provided format supports it,</li>
 * <li>{@link #close()}.</li>
 * </ul>
 *
 * @param <E> The type of {@link ArchiveEntry} consumed.
 */
public abstract class ArchiveOutputStream<E extends ArchiveEntry> extends FilterOutputStream {

    static final int BYTE_MASK = 0xFF;

    /** Temporary buffer used for the {@link #write(int)} method. */
    private final byte[] oneByte = new byte[1];

    /** Holds the number of bytes written to this stream. */
    private long bytesWritten;

    /**
     * Whether this instance was successfully closed.
     */
    private boolean closed;

    /**
     * Whether this instance was successfully finished.
     */
    private boolean finished;

    /**
     * Constructs a new instance without a backing OutputStream.
     * <p>
     * You must initialize {@code this.out} after construction.
     * </p>
     */
    public ArchiveOutputStream() {
        super(null);
    }

    /**
     * Constructs a new instance with the given backing OutputStream.
     *
     * @param out the underlying output stream to be assigned to the field {@code this.out} for later use, or {@code null} if this instance is to be created
     *            without an underlying stream.
     * @since 1.27.0.
     */
    public ArchiveOutputStream(final OutputStream out) {
        super(out);
    }

    /**
     * Whether this stream is able to write the given entry.
     *
     * <p>
     * Some archive formats support variants or details that are not supported (yet).
     * </p>
     *
     * @param archiveEntry the entry to test
     * @return This implementation always returns true.
     * @since 1.1
     */
    public boolean canWriteEntryData(final ArchiveEntry archiveEntry) {
        return true;
    }

    /**
     * Throws an {@link IOException} if this instance is already finished.
     *
     * @throws IOException if this instance is already finished.
     * @since 1.27.0
     */
    protected void checkFinished() throws IOException {
        if (isFinished()) {
            throw new IOException("Stream has already been finished.");
        }
    }

    /**
     * Check to make sure that this stream has not been closed
     *
     * @throws IOException if the stream is already closed
     * @since 1.27.0
     */
    protected void checkOpen() throws IOException {
        if (isClosed()) {
            throw new IOException("Stream closed");
        }
    }

    @Override
    public void close() throws IOException {
        super.close();
        closed = true;
    }

    /**
     * Closes the archive entry, writing any trailer information that may be required.
     *
     * @throws IOException if an I/O error occurs
     */
    public abstract void closeArchiveEntry() throws IOException;

    /**
     * Increments the counter of already written bytes. Doesn't increment if EOF has been hit ({@code written == -1}).
     *
     * @param written the number of bytes written
     */
    protected void count(final int written) {
        count((long) written);
    }

    /**
     * Increments the counter of already written bytes. Doesn't increment if EOF has been hit ({@code written == -1}).
     *
     * @param written the number of bytes written
     * @since 1.1
     */
    protected void count(final long written) {
        if (written != -1) {
            bytesWritten += written;
        }
    }

    /**
     * Creates an archive entry using the inputFile and entryName provided.
     *
     * @param inputFile the file to create the entry from
     * @param entryName name to use for the entry
     * @return the ArchiveEntry set up with details from the file
     *
     * @throws IOException if an I/O error occurs
     */
    public abstract E createArchiveEntry(File inputFile, String entryName) throws IOException;

    /**
     * Creates an archive entry using the inputPath and entryName provided.
     * <p>
     * The default implementation calls simply delegates as:
     * </p>
     *
     * <pre>
     * return createArchiveEntry(inputFile.toFile(), entryName);
     * </pre>
     * <p>
     * Subclasses should override this method.
     * </p>
     *
     * @param inputPath the file to create the entry from
     * @param entryName name to use for the entry
     * @param options   options indicating how symbolic links are handled.
     * @return the ArchiveEntry set up with details from the file
     *
     * @throws IOException if an I/O error occurs
     * @since 1.21
     */
    public E createArchiveEntry(final Path inputPath, final String entryName, final LinkOption... options) throws IOException {
        return createArchiveEntry(inputPath.toFile(), entryName);
    }

    /**
     * Finishes the addition of entries to this stream, without closing it. Additional data can be written, if the format supports it.
     *
     * @throws IOException Maybe thrown by subclasses if the user forgets to close the entry.
     */
    public void finish() throws IOException {
        finished = true;
    }

    /**
     * Gets the current number of bytes written to this stream.
     *
     * @return the number of written bytes
     * @since 1.1
     */
    public long getBytesWritten() {
        return bytesWritten;
    }

    /**
     * Gets the current number of bytes written to this stream.
     *
     * @return the number of written bytes
     * @deprecated this method may yield wrong results for large archives, use #getBytesWritten instead
     */
    @Deprecated
    public int getCount() {
        return (int) bytesWritten;
    }

    /**
     * Tests whether this instance was successfully closed.
     *
     * @return whether this instance was successfully closed.
     * @since 1.27.0
     */
    protected boolean isClosed() {
        return closed;
    }

    /**
     * Tests whether this instance was successfully finished.
     *
     * @return whether this instance was successfully finished.
     * @since 1.27.0
     */
    protected boolean isFinished() {
        return finished;
    }

    /**
     * Writes the headers for an archive entry to the output stream. The caller must then write the content to the stream and call {@link #closeArchiveEntry()}
     * to complete the process.
     *
     * @param entry describes the entry
     * @throws IOException if an I/O error occurs
     */
    public abstract void putArchiveEntry(E entry) throws IOException;

    /**
     * Writes a byte to the current archive entry.
     *
     * <p>
     * This method simply calls {@code write( byte[], 0, 1 )}.
     *
     * <p>
     * MUST be overridden if the {@link #write(byte[], int, int)} method is not overridden; may be overridden otherwise.
     *
     * @param b The byte to be written.
     * @throws IOException on error
     */
    @Override
    public void write(final int b) throws IOException {
        oneByte[0] = (byte) (b & BYTE_MASK);
        write(oneByte, 0, 1);
    }
}
