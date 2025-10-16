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

package org.apache.commons.compress.archivers.zip;

import static org.apache.commons.compress.archivers.zip.ZipConstants.DWORD;
import static org.apache.commons.compress.archivers.zip.ZipConstants.SHORT;
import static org.apache.commons.compress.archivers.zip.ZipConstants.WORD;
import static org.apache.commons.compress.archivers.zip.ZipConstants.ZIP64_MAGIC;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Function;
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

import org.apache.commons.compress.MemoryLimitException;
import org.apache.commons.compress.archivers.AbstractArchiveBuilder;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.deflate64.Deflate64CompressorInputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream;
import org.apache.commons.compress.utils.ArchiveUtils;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.compress.utils.InputStreamStatistics;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Implements an input stream that can read Zip archives.
 * <p>
 * As of Apache Commons Compress it transparently supports Zip64 extensions and thus individual entries and archives larger than 4 GB or with more than 65,536
 * entries.
 * </p>
 * <p>
 * The {@link ZipFile} class is preferred when reading from files as {@link ZipArchiveInputStream} is limited by not being able to read the central directory
 * header before returning entries. In particular {@link ZipArchiveInputStream}
 * </p>
 * <ul>
 * <li>may return entries that are not part of the central directory at all and shouldn't be considered part of the archive.</li>
 * <li>may return several entries with the same name.</li>
 * <li>will not return internal or external attributes.</li>
 * <li>may return incomplete extra field data.</li>
 * <li>may return unknown sizes and CRC values for entries until the next entry has been reached if the archive uses the data descriptor feature.</li>
 * </ul>
 *
 * @see ZipFile
 * @NotThreadSafe
 */
public class ZipArchiveInputStream extends ArchiveInputStream<ZipArchiveEntry> implements InputStreamStatistics {

    /**
     * Abstract builder for derived classes of {@link ZipArchiveInputStream}.
     *
     * @param <T> The type of the {@link ZipArchiveInputStream}.
     * @param <B> The type of the builder itself.
     * @since 1.29.0
     */
    public abstract static class AbstractBuilder<T extends ZipArchiveInputStream, B extends AbstractBuilder<T, B>>
            extends AbstractArchiveBuilder<T, B> {

        private boolean useUnicodeExtraFields = true;
        private boolean supportStoredEntryDataDescriptor;
        private boolean skipSplitSignature;

        /**
         * Constructs a new instance.
         */
        protected AbstractBuilder() {
            setCharset(StandardCharsets.UTF_8);
        }

        /**
         * Tests whether the stream should skip the ZIP split signature.
         *
         * @return {@code true} to skip the ZIP split signature, {@code false} otherwise.
         */
        protected boolean isSkipSplitSignature() {
            return skipSplitSignature;
        }

        /**
         * Tests whether the stream attempts to read STORED entries that use a data descriptor.
         *
         * @return {@code true} to read STORED entries with data descriptors,
         *         {@code false} to stop at the compressed size.
         */
        protected boolean isSupportStoredEntryDataDescriptor() {
            return supportStoredEntryDataDescriptor;
        }

        /**
         * Tests whether to use InfoZIP Unicode Extra Fields (if present) to set the file names.
         *
         * @return {@code true} if Unicode Extra Fields should be used.
         */
        protected boolean isUseUnicodeExtraFields() {
            return useUnicodeExtraFields;
        }

        /**
         * Sets whether the stream should skip the ZIP split signature
         * ({@code 08074B50}) at the beginning of the input.
         *
         * <p>Disabled by default.</p>
         *
         * @param skipSplitSignature {@code true} to skip the ZIP split signature, {@code false} otherwise.
         * @return {@code this} instance.
         */
        public B setSkipSplitSignature(final boolean skipSplitSignature) {
            this.skipSplitSignature = skipSplitSignature;
            return asThis();
        }

        /**
         * Sets whether the stream supports reading STORED entries that use a data descriptor.
         *
         * <p>If set to {@code true}, the stream will not stop reading an entry at the
         * declared compressed size. Instead, it will continue until a data descriptor
         * is encountered (by detecting the Data Descriptor Signature). This may cause
         * issues in certain cases, such as JARs embedded in WAR files.</p>
         *
         * <p>See <a href="https://issues.apache.org/jira/browse/COMPRESS-555">COMPRESS-555</a>
         * for details.</p>
         *
         * <p>This feature is disabled by default.</p>
         *
         * @param supportStoredEntryDataDescriptor {@code true} to support STORED entries with a data descriptor,
         *                                             {@code false} to stop at the declared compressed size.
         * @return {@code this} instance.
         */
        public B setSupportStoredEntryDataDescriptor(final boolean supportStoredEntryDataDescriptor) {
            this.supportStoredEntryDataDescriptor = supportStoredEntryDataDescriptor;
            return asThis();
        }

        /**
         * Sets whether to use InfoZIP Unicode Extra Fields (if present) to set the file names.
         *
         * <p>This feature is enabled by default.</p>
         *
         * @param useUnicodeExtraFields If {@code true} Unicode Extra Fields should be used.
         * @return {@code this} instance.
         */
        public B setUseUnicodeExtraFields(final boolean useUnicodeExtraFields) {
            this.useUnicodeExtraFields = useUnicodeExtraFields;
            return asThis();
        }
    }

    /**
     * Input stream adapted from commons-io.
     */
    private final class BoundCountInputStream extends BoundedInputStream {
        // TODO Consider how to do this from a final class, an IO class, or basically without the current side-effect implementation.

        /**
         * Creates a new {@code BoundedInputStream} that wraps the given input stream and limits it to a certain size.
         *
         * @param in  The wrapped input stream.
         * @param max The maximum number of bytes to return.
         */
        BoundCountInputStream(final InputStream in, final long max) {
            super(in, max);
        }

        private boolean atMaxLength() {
            return getMaxCount() >= 0 && getCount() >= getMaxCount();
        }

        @Override
        public int read() throws IOException {
            if (atMaxLength()) {
                return -1;
            }
            final int result = super.read();
            if (result != -1) {
                readCount(1);
            }
            return result;
        }

        @Override
        public int read(final byte[] b, final int off, final int len) throws IOException {
            if (len == 0) {
                return 0;
            }
            if (atMaxLength()) {
                return -1;
            }
            final long maxRead = getMaxCount() >= 0 ? Math.min(len, getMaxCount() - getCount()) : len;
            return readCount(super.read(b, off, (int) maxRead));
        }

        private int readCount(final int bytesRead) {
            if (bytesRead != -1) {
                count(bytesRead);
                current.bytesReadFromStream += bytesRead;
            }
            return bytesRead;
        }
    }

    /**
     * Builds a new {@link ZipArchiveInputStream}.
     * <p>
     *     For example:
     * </p>
     * <pre>{@code
     * ZipArchiveInputStream in = ZipArchiveInputStream.builder()
     *     .setPath(inputPath)
     *     .setCharset(StandardCharsets.UTF_8)
     *     .setUseUnicodeExtraFields(false)
     *     .get();
     * }</pre>
     *
     * @since 1.29.0
     */
    public static final class Builder extends AbstractBuilder<ZipArchiveInputStream, Builder> {

        private Builder() {
            // empty
        }

        @Override
        public ZipArchiveInputStream get() throws IOException {
            return new ZipArchiveInputStream(this);
        }
    }

    /**
     * Structure collecting information for the entry that is currently being read.
     */
    private static final class CurrentEntry {

        /**
         * Current ZIP entry.
         */
        private final ZipArchiveEntry entry = new ZipArchiveEntry();
        /**
         * Does the entry use a data descriptor?
         */
        private boolean hasDataDescriptor;
        /**
         * Does the entry have a ZIP64 extended information extra field.
         */
        private boolean usesZip64;
        /**
         * Number of bytes of entry content read by the client if the entry is STORED.
         */
        private long bytesRead;
        /**
         * Number of bytes of entry content read from the stream.
         * <p>
         * This may be more than the actual entry's length as some stuff gets buffered up and needs to be pushed back when the end of the entry has been
         * reached.
         * </p>
         */
        private long bytesReadFromStream;
        /**
         * The checksum calculated as the current entry is read.
         */
        private final CRC32 crc = new CRC32();
        /**
         * The input stream decompressing the data for shrunk and imploded entries.
         */
        private InputStream inputStream;

        @SuppressWarnings("unchecked") // Caller beware
        private <T extends InputStream> T checkInputStream() throws ZipException {
            if (inputStream == null) {
                throw new ZipException("null inputStream");
            }
            return (T) inputStream;
        }
    }

    /**
     * Maximum size of data in the first local file header.
     */
    public static final int PREAMBLE_GARBAGE_MAX_SIZE = 4096;
    private static final int LFH_LEN = 30;
    /*
     * local file header signature WORD version needed to extract SHORT general purpose bit flag SHORT compression method SHORT last mod file time SHORT last
     * mod file date SHORT CRC-32 WORD compressed size WORD uncompressed size WORD file name length SHORT extra field length SHORT.
     */
    private static final int CFH_LEN = 46;
    /*
     * central file header signature WORD version made by SHORT version needed to extract SHORT general purpose bit flag SHORT compression method SHORT last mod
     * file time SHORT last mod file date SHORT CRC-32 WORD compressed size WORD uncompressed size WORD file name length SHORT extra field length SHORT file
     * comment length SHORT disk number start SHORT internal file attributes SHORT external file attributes WORD relative offset of local header WORD.
     */
    private static final long TWO_EXP_32 = ZIP64_MAGIC + 1;
    private static final String USE_ZIPFILE_INSTEAD_OF_STREAM_DISCLAIMER = " while reading a stored entry using data descriptor. Either the archive is broken"
            + " or it cannot be read using ZipArchiveInputStream and you must use ZipFile."
            + " A common cause for this is a ZIP archive containing a ZIP archive."
            + " See https://commons.apache.org/proper/commons-compress/zip.html#ZipArchiveInputStream_vs_ZipFile";
    private static final byte[] LFH = ZipLong.LFH_SIG.getBytes();
    private static final byte[] CFH = ZipLong.CFH_SIG.getBytes();
    private static final byte[] DD = ZipLong.DD_SIG.getBytes();
    private static final byte[] APK_SIGNING_BLOCK_MAGIC = { 'A', 'P', 'K', ' ', 'S', 'i', 'g', ' ', 'B', 'l', 'o', 'c', 'k', ' ', '4', '2', };
    private static final BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);

    /**
     * Creates a new builder.
     *
     * @return A new builder.
     * @since 1.29.0
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Checks if the signature matches what is expected for a ZIP file. Does not currently handle self-extracting ZIPs which may have arbitrary leading content.
     *
     * @param buffer the bytes to check.
     * @param length the number of bytes to check.
     * @return true, if this stream is a ZIP archive stream, false otherwise.
     */
    public static boolean matches(final byte[] buffer, final int length) {
        if (length < ZipArchiveOutputStream.LFH_SIG.length) {
            return false;
        }
        return ArrayUtils.startsWith(buffer, ZipArchiveOutputStream.LFH_SIG) // normal file
                || ArrayUtils.startsWith(buffer, ZipArchiveOutputStream.EOCD_SIG) // empty zip
                || ArrayUtils.startsWith(buffer, ZipArchiveOutputStream.DD_SIG) // split zip
                || ArrayUtils.startsWith(buffer, ZipLong.SINGLE_SEGMENT_SPLIT_MARKER.getBytes());
    }

    /** The ZIP encoding to use for file names and the file comment. */
    private final ZipEncoding zipEncoding;
    /** Whether to look for and use Unicode extra fields. */
    private final boolean useUnicodeExtraFields;
    /** Inflater used for all deflated entries. */
    private final Inflater inf = new Inflater(true);
    /** Buffer used to read from the wrapped stream. */
    private final ByteBuffer buf = ByteBuffer.allocate(ZipArchiveOutputStream.BUFFER_SIZE);
    /** The entry that is currently being read. */
    private CurrentEntry current;
    /** Whether the stream has been closed. */
    private boolean closed;
    /** Whether the stream has reached the central directory - and thus found all entries. */
    private boolean hitCentralDirectory;
    /**
     * When reading a stored entry that uses the data descriptor this stream has to read the full entry and caches it. This is the cache.
     */
    private ByteArrayInputStream lastStoredEntry;
    /**
     * Whether the stream will try to read STORED entries that use a data descriptor. Setting it to true means we will not stop reading an entry with the
     * compressed size, instead we will stop reading an entry when a data descriptor is met (by finding the Data Descriptor Signature). This will completely
     * break down in some cases - like JARs in WARs.
     * <p>
     * See also : https://issues.apache.org/jira/projects/COMPRESS/issues/COMPRESS-555
     * https://github.com/apache/commons-compress/pull/137#issuecomment-690835644
     * </p>
     */
    private final boolean supportStoredEntryDataDescriptor;
    /** Count decompressed bytes for current entry */
    private long uncompressedCount;
    /** Whether the stream will try to skip the ZIP split signature(08074B50) at the beginning. **/
    private final boolean skipSplitSignature;
    /** Cached buffers - must only be used locally in the class (COMPRESS-172 - reduce garbage collection). */
    private final byte[] lfhBuf = new byte[LFH_LEN];
    private final byte[] skipBuf = new byte[1024];
    private final byte[] shortBuf = new byte[SHORT];
    private final byte[] wordBuf = new byte[WORD];
    private final byte[] twoDwordBuf = new byte[2 * DWORD];
    private int entriesRead;
    /**
     * The factory for extra fields or null.
     */
    // private Function<ZipShort, ZipExtraField> extraFieldSupport;

    /**
     * Creates an instance from the given builder.
     *
     * @param builder The builder used to configure and create the stream.
     * @throws IOException If the builder fails to create the underlying {@link InputStream}.
     * @since 1.29.0
     */
    protected ZipArchiveInputStream(final AbstractBuilder<?, ?> builder) throws IOException {
        super(builder);
        this.in = new PushbackInputStream(in, buf.capacity());
        this.zipEncoding = ZipEncodingHelper.getZipEncoding(builder.getCharset());
        this.useUnicodeExtraFields = builder.isUseUnicodeExtraFields();
        this.supportStoredEntryDataDescriptor = builder.isSupportStoredEntryDataDescriptor();
        this.skipSplitSignature = builder.isSkipSplitSignature();
        // haven't read anything so far
        buf.limit(0);
    }

    /**
     * Constructs an instance using UTF-8 encoding.
     *
     * <p>Since 1.29.0: throws {@link IOException}.</p>
     *
     * @param inputStream the stream to wrap.
     * @throws IOException if an I/O error occurs.
     */
    public ZipArchiveInputStream(final InputStream inputStream) throws IOException {
        this(builder().setInputStream(inputStream));
    }

    /**
     * Constructs an instance using the specified encoding.
     *
     * <p>Since 1.29.0: throws {@link IOException}.</p>
     *
     * @param inputStream the stream to wrap.
     * @param encoding    the encoding to use for file names, use null for the platform's default encoding.
     * @throws IOException if an I/O error occurs.
     * @since 1.5
     * @deprecated Since 1.29.0, use {@link #builder()}.
     */
    @Deprecated
    public ZipArchiveInputStream(final InputStream inputStream, final String encoding) throws IOException {
        this(builder().setInputStream(inputStream).setCharset(encoding));
    }

    /**
     * Constructs an instance using the specified encoding.
     *
     * <p>Since 1.29.0: throws {@link IOException}.</p>
     *
     * @param inputStream           the stream to wrap.
     * @param encoding              the encoding to use for file names, use null for the platform's default encoding.
     * @param useUnicodeExtraFields whether to use InfoZIP Unicode Extra Fields (if present) to set the file names.
     * @throws IOException if an I/O error occurs.
     * @deprecated Since 1.29.0, use {@link #builder()}.
     */
    @Deprecated
    public ZipArchiveInputStream(final InputStream inputStream, final String encoding, final boolean useUnicodeExtraFields) throws IOException {
        this(builder().setInputStream(inputStream).setCharset(encoding).setUseUnicodeExtraFields(useUnicodeExtraFields));
    }

    /**
     * Constructs an instance using the specified encoding.
     *
     * <p>Since 1.29.0: throws {@link IOException}.</p>
     *
     * @param inputStream                      the stream to wrap.
     * @param encoding                         the encoding to use for file names, use null for the platform's default encoding.
     * @param useUnicodeExtraFields            whether to use InfoZIP Unicode Extra Fields (if present) to set the file names.
     * @param supportStoredEntryDataDescriptor whether the stream will try to read STORED entries that use a data descriptor.
     * @throws IOException if an I/O error occurs.
     * @since 1.1
     * @deprecated Since 1.29.0, use {@link #builder()}.
     */
    @Deprecated
    public ZipArchiveInputStream(
            final InputStream inputStream,
            final String encoding,
            final boolean useUnicodeExtraFields,
            final boolean supportStoredEntryDataDescriptor) throws IOException {
        // @formatter:off
        this(builder()
                .setInputStream(inputStream)
                .setCharset(encoding)
                .setUseUnicodeExtraFields(useUnicodeExtraFields)
                .setSupportStoredEntryDataDescriptor(supportStoredEntryDataDescriptor));
        // @formatter:on
    }

    /**
     * Constructs an instance using the specified encoding.
     *
     * <p>Since 1.29.0: throws {@link IOException}.</p>
     *
     * @param inputStream                      the stream to wrap.
     * @param encoding                         the encoding to use for file names, use null for the platform's default encoding.
     * @param useUnicodeExtraFields            whether to use InfoZIP Unicode Extra Fields (if present) to set the file names.
     * @param supportStoredEntryDataDescriptor whether the stream will try to read STORED entries that use a data descriptor.
     * @param skipSplitSignature               Whether the stream will try to skip the zip split signature(08074B50) at the beginning.
     *                                         You will need to set this to true if you want to read a split archive.
     * @throws IOException if an I/O error occurs.
     * @since 1.20
     * @deprecated Since 1.29.0, use {@link #builder()}.
     */
    @Deprecated
    public ZipArchiveInputStream(
            final InputStream inputStream,
            final String encoding,
            final boolean useUnicodeExtraFields,
            final boolean supportStoredEntryDataDescriptor,
            final boolean skipSplitSignature) throws IOException {
        // @formatter:off
        this(builder()
                .setInputStream(inputStream)
                .setCharset(encoding)
                .setUseUnicodeExtraFields(useUnicodeExtraFields)
                .setSupportStoredEntryDataDescriptor(supportStoredEntryDataDescriptor)
                .setSkipSplitSignature(skipSplitSignature));
        // @formatter:on
    }

    /**
     * Checks whether the current buffer contains the signature of a &quot;data descriptor&quot;, &quot;local file header&quot; or &quot;central directory
     * entry&quot;.
     * <p>
     * If it contains such a signature, reads the data descriptor and positions the stream right after the data descriptor.
     * </p>
     */
    private boolean bufferContainsSignature(
            final ByteArrayOutputStream bos, final int offset, final int lastRead, final int expectedDDLen)
            throws IOException {
        boolean done = false;
        for (int i = 0; !done && i < offset + lastRead - 4; i++) {
            if (buf.array()[i] == LFH[0] && buf.array()[i + 1] == LFH[1]) {
                int expectDDPos = i;
                if (i >= expectedDDLen && buf.array()[i + 2] == LFH[2] && buf.array()[i + 3] == LFH[3]
                        || buf.array()[i + 2] == CFH[2] && buf.array()[i + 3] == CFH[3]) {
                    // found an LFH or CFH:
                    expectDDPos = i - expectedDDLen;
                    done = true;
                } else if (buf.array()[i + 2] == DD[2] && buf.array()[i + 3] == DD[3]) {
                    // found DD:
                    done = true;
                }
                if (done) {
                    // * push back bytes read in excess as well as the data
                    // descriptor
                    // * copy the remaining bytes to cache
                    // * read data descriptor
                    pushback(buf.array(), expectDDPos, offset + lastRead - expectDDPos);
                    bos.write(buf.array(), 0, expectDDPos);
                    readDataDescriptor();
                }
            }
        }
        return done;
    }

    /**
     * If the last read bytes could hold a data descriptor and an incomplete signature then save the last bytes to the front of the buffer and cache everything
     * in front of the potential data descriptor into the given ByteArrayOutputStream.
     * <p>
     * Data descriptor plus incomplete signature (3 bytes in the worst case) can be 20 bytes max.
     * </p>
     */
    private int cacheBytesRead(final ByteArrayOutputStream bos, int offset, final int lastRead, final int expectedDDLen) throws MemoryLimitException {
        final int cacheable = offset + lastRead - expectedDDLen - 3;
        if (cacheable > 0) {
            final int request = (bos.size() + cacheable) * 2;
            MemoryLimitException.checkBytes(request + 2 * 1024 * 1024, Runtime.getRuntime().totalMemory()); // 2 MB headroom
            bos.write(buf.array(), 0, cacheable);
            System.arraycopy(buf.array(), cacheable, buf.array(), 0, expectedDDLen + 3);
            offset = expectedDDLen + 3;
        } else {
            offset += lastRead;
        }
        return offset;
    }

    /**
     * Tests whether this class is able to read the given entry.
     * <p>
     * May return false if it is set up to use encryption or a compression method that hasn't been implemented yet.
     * </p>
     *
     * @since 1.1
     */
    @Override
    public boolean canReadEntryData(final ArchiveEntry ae) {
        if (ae instanceof ZipArchiveEntry) {
            final ZipArchiveEntry ze = (ZipArchiveEntry) ae;
            return ZipUtil.canHandleEntryData(ze) && supportsDataDescriptorFor(ze) && supportsCompressedSizeFor(ze);
        }
        return false;
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            closed = true;
            try {
                in.close();
            } finally {
                inf.end();
            }
        }
    }

    /**
     * Closes the current ZIP archive entry and positions the underlying stream to the beginning of the next entry. All per-entry variables and data structures
     * are cleared.
     * <p>
     * If the compressed size of this entry is included in the entry header, then any outstanding bytes are simply skipped from the underlying stream without
     * uncompressing them. This allows an entry to be safely closed even if the compression method is unsupported.
     * </p>
     * <p>
     * In case we don't know the compressed size of this entry or have already buffered too much data from the underlying stream to support uncompression, then
     * the uncompression process is completed and the end position of the stream is adjusted based on the result of that process.
     * </p>
     *
     * @throws IOException if an error occurs.
     */
    private void closeEntry() throws IOException {
        if (closed) {
            throw new ArchiveException("The stream is closed");
        }
        if (current == null) {
            return;
        }
        // Ensure all entry bytes are read
        if (currentEntryHasOutstandingBytes()) {
            drainCurrentEntryData();
        } else {
            // this is guaranteed to exhaust the stream
            if (skip(Long.MAX_VALUE) < 0) {
                throw new IllegalStateException("Can't read the remainder of the stream");
            }
            final long inB = current.entry.getMethod() == ZipArchiveOutputStream.DEFLATED ? getBytesInflated() : current.bytesRead;
            // this is at most a single read() operation and can't
            // exceed the range of int
            final int diff = (int) (current.bytesReadFromStream - inB);
            // Pushback any required bytes
            if (diff > 0) {
                pushback(buf.array(), buf.limit() - diff, diff);
                current.bytesReadFromStream -= diff;
            }
            // Drain remainder of entry if not all data bytes were required
            if (currentEntryHasOutstandingBytes()) {
                drainCurrentEntryData();
            }
        }
        if (lastStoredEntry == null && current.hasDataDescriptor) {
            readDataDescriptor();
        }
        inf.reset();
        buf.clear().flip();
        current = null;
        lastStoredEntry = null;
    }

    /**
     * Creates the appropriate InputStream for the Zstd compression method.
     *
     * @param in the input stream which should be used for compression.
     * @return the {@link InputStream} for handling the Zstd compression.
     * @throws IOException if an I/O error occurs.
     * @since 1.28.0
     */
    protected InputStream createZstdInputStream(final InputStream in) throws IOException {
        return new ZstdCompressorInputStream(in);
    }

    /**
     * If the compressed size of the current entry is included in the entry header and there are any outstanding bytes in the underlying stream, then this
     * returns true.
     *
     * @return true, if current entry is determined to have outstanding bytes, false otherwise.
     */
    private boolean currentEntryHasOutstandingBytes() {
        return current.bytesReadFromStream <= current.entry.getCompressedSize() && !current.hasDataDescriptor;
    }

    /**
     * Reads all data of the current entry from the underlying stream that hasn't been read, yet.
     */
    private void drainCurrentEntryData() throws IOException {
        long remaining = current.entry.getCompressedSize() - current.bytesReadFromStream;
        while (remaining > 0) {
            final long n = in.read(buf.array(), 0, (int) Math.min(buf.capacity(), remaining));
            if (n < 0) {
                throw new EOFException("Truncated ZIP entry: " + ArchiveUtils.sanitize(current.entry.getName()));
            }
            count(n);
            remaining -= n;
        }
    }

    private int fill() throws IOException {
        if (closed) {
            throw new ArchiveException("The stream is closed");
        }
        final int length = in.read(buf.array());
        if (length > 0) {
            buf.limit(length);
            count(buf.limit());
            inf.setInput(buf.array(), 0, buf.limit());
        }
        return length;
    }

    /**
     * Reads forward until the signature of the &quot;End of central directory&quot; record is found.
     */
    private boolean findEocdRecord() throws IOException {
        int currentByte = -1;
        boolean skipReadCall = false;
        while (skipReadCall || (currentByte = readOneByte()) > -1) {
            skipReadCall = false;
            if (!isFirstByteOfEocdSig(currentByte)) {
                continue;
            }
            currentByte = readOneByte();
            if (currentByte != ZipArchiveOutputStream.EOCD_SIG[1]) {
                if (currentByte == -1) {
                    break;
                }
                skipReadCall = isFirstByteOfEocdSig(currentByte);
                continue;
            }
            currentByte = readOneByte();
            if (currentByte != ZipArchiveOutputStream.EOCD_SIG[2]) {
                if (currentByte == -1) {
                    break;
                }
                skipReadCall = isFirstByteOfEocdSig(currentByte);
                continue;
            }
            currentByte = readOneByte();
            if (currentByte == -1) {
                break;
            }
            if (currentByte == ZipArchiveOutputStream.EOCD_SIG[3]) {
                return true;
            }
            skipReadCall = isFirstByteOfEocdSig(currentByte);
        }
        return false;
    }

    /**
     * Gets the number of bytes Inflater has actually processed.
     * <p>
     * for Java &lt; Java7 the getBytes* methods in Inflater/Deflater seem to return unsigned ints rather than longs that start over with 0 at 2^32.
     * </p>
     * <p>
     * The stream knows how many bytes it has read, but not how many the Inflater actually consumed - it should be between the total number of bytes read for
     * the entry and the total number minus the last read operation. Here we just try to make the value close enough to the bytes we've read by assuming the
     * number of bytes consumed must be smaller than (or equal to) the number of bytes read but not smaller by more than 2^32.
     * </p>
     */
    private long getBytesInflated() {
        long inB = inf.getBytesRead();
        if (current.bytesReadFromStream >= TWO_EXP_32) {
            while (inB + TWO_EXP_32 <= current.bytesReadFromStream) {
                inB += TWO_EXP_32;
            }
        }
        return inB;
    }

    /**
     * @since 1.17
     */
    @SuppressWarnings("resource") // checkInputStream() does not allocate.
    @Override
    public long getCompressedCount() throws IOException {
        if (current == null) {
            return -1;
        }
        final int method = current.entry.getMethod();
        if (method == ZipArchiveOutputStream.STORED) {
            return current.bytesRead;
        }
        if (method == ZipArchiveOutputStream.DEFLATED) {
            return getBytesInflated();
        }
        if (method == ZipMethod.UNSHRINKING.getCode() || method == ZipMethod.IMPLODING.getCode() || method == ZipMethod.ENHANCED_DEFLATED.getCode()
                || method == ZipMethod.BZIP2.getCode()) {
            return ((InputStreamStatistics) current.checkInputStream()).getCompressedCount();
        }
        return -1;
    }

    @Override
    public ZipArchiveEntry getNextEntry() throws IOException {
        return getNextZipEntry();
    }

    /**
     * Gets the next entry.
     *
     * @return the next entry.
     * @throws IOException if an I/O error occurs.
     * @deprecated Use {@link #getNextEntry()}.
     */
    @Deprecated
    public ZipArchiveEntry getNextZipEntry() throws IOException {
        uncompressedCount = 0;
        boolean firstEntry = true;
        if (closed || hitCentralDirectory) {
            return null;
        }
        if (current != null) {
            closeEntry();
            firstEntry = false;
        }
        final long currentHeaderOffset = getBytesRead();
        try {
            if (firstEntry) {
                // split archives have a special signature before the
                // first local file header - look for it and fail with
                // the appropriate error message if this is a split
                // archive.
                if (!readFirstLocalFileHeader()) {
                    hitCentralDirectory = true;
                    skipRemainderOfArchive();
                    return null;
                }
            } else {
                readFully(lfhBuf);
            }
        } catch (final EOFException e) { // NOSONAR
            return null;
        }
        final ZipLong sig = new ZipLong(lfhBuf);
        if (!sig.equals(ZipLong.LFH_SIG)) {
            if (sig.equals(ZipLong.CFH_SIG) || sig.equals(ZipLong.AED_SIG) || isApkSigningBlock(lfhBuf)) {
                hitCentralDirectory = true;
                skipRemainderOfArchive();
                return null;
            }
            throw new ZipException(String.format("Unexpected record signature: 0x%x", sig.getValue()));
        }
        // off: go past the signature
        int off = WORD;
        current = new CurrentEntry();
        // get version
        final int versionMadeBy = ZipShort.getValue(lfhBuf, off);
        off += SHORT;
        current.entry.setPlatform(ZipFile.toPlatform(versionMadeBy));
        final GeneralPurposeBit gpFlag = GeneralPurposeBit.parse(lfhBuf, off);
        final boolean hasUTF8Flag = gpFlag.usesUTF8ForNames();
        final ZipEncoding entryEncoding = hasUTF8Flag ? ZipEncodingHelper.ZIP_ENCODING_UTF_8 : zipEncoding;
        current.hasDataDescriptor = gpFlag.usesDataDescriptor();
        current.entry.setGeneralPurposeBit(gpFlag);
        off += SHORT;
        current.entry.setMethod(ZipShort.getValue(lfhBuf, off));
        off += SHORT;
        final long time = ZipUtil.dosToJavaTime(ZipLong.getValue(lfhBuf, off));
        current.entry.setTime(time);
        off += WORD;
        ZipLong size = null;
        ZipLong cSize = null;
        if (!current.hasDataDescriptor) {
            current.entry.setCrc(ZipLong.getValue(lfhBuf, off));
            off += WORD;
            cSize = new ZipLong(lfhBuf, off);
            off += WORD;
            size = new ZipLong(lfhBuf, off);
            off += WORD;
        } else {
            off += 3 * WORD;
        }
        final int fileNameLen = ArchiveUtils.checkEntryNameLength(ZipShort.getValue(lfhBuf, off), getMaxEntryNameLength(), "ZIP");
        off += SHORT;
        final int extraLen = ZipShort.getValue(lfhBuf, off);
        final byte[] fileName = readRange(fileNameLen);
        current.entry.setName(entryEncoding.decode(fileName), fileName);
        if (hasUTF8Flag) {
            current.entry.setNameSource(ZipArchiveEntry.NameSource.NAME_WITH_EFS_FLAG);
        }
        final byte[] extraData = readRange(extraLen);
        try {
            current.entry.setExtra(extraData);
        } catch (final RuntimeException ex) {
            throw ZipUtil.newZipException("Invalid extra data in entry " + current.entry.getName(), ex);
        }
        if (!hasUTF8Flag && useUnicodeExtraFields) {
            ZipUtil.setNameAndCommentFromExtraFields(current.entry, fileName, null);
        }
        processZip64Extra(size, cSize);
        current.entry.setLocalHeaderOffset(currentHeaderOffset);
        current.entry.setDataOffset(getBytesRead());
        current.entry.setStreamContiguous(true);
        final ZipMethod m = ZipMethod.getMethodByCode(current.entry.getMethod());
        if (current.entry.getCompressedSize() != ArchiveEntry.SIZE_UNKNOWN) {
            if (ZipUtil.canHandleEntryData(current.entry) && m != ZipMethod.STORED && m != ZipMethod.DEFLATED) {
                final InputStream bis = new BoundCountInputStream(in, current.entry.getCompressedSize());
                switch (m) {
                case UNSHRINKING:
                    current.inputStream = new UnshrinkingInputStream(bis);
                    break;
                case IMPLODING:
                    try {
                        current.inputStream = new ExplodingInputStream(current.entry.getGeneralPurposeBit().getSlidingDictionarySize(),
                                current.entry.getGeneralPurposeBit().getNumberOfShannonFanoTrees(), bis);
                    } catch (final IllegalArgumentException e) {
                        throw new ArchiveException("Bad IMPLODE data", (Throwable) e);
                    }
                    break;
                case BZIP2:
                    current.inputStream = new BZip2CompressorInputStream(bis);
                    break;
                case ENHANCED_DEFLATED:
                    current.inputStream = new Deflate64CompressorInputStream(bis);
                    break;
                case ZSTD:
                case ZSTD_DEPRECATED:
                    current.inputStream = createZstdInputStream(bis);
                    break;
                default:
                    // we should never get here as all supported methods have been covered
                    // will cause an error when read is invoked, don't throw an exception here so people can
                    // skip unsupported entries
                    break;
                }
            }
        } else if (m == ZipMethod.ENHANCED_DEFLATED) {
            current.inputStream = new Deflate64CompressorInputStream(in);
        }
        entriesRead++;
        return current.entry;
    }

    /**
     * Gets the uncompressed count.
     *
     * @since 1.17
     */
    @Override
    public long getUncompressedCount() {
        return uncompressedCount;
    }

    /**
     * Checks whether this might be an APK Signing Block.
     * <p>
     * Unfortunately the APK signing block does not start with some kind of signature, it rather ends with one. It starts with a length, so what we do is parse
     * the suspect length, skip ahead far enough, look for the signature and if we've found it, return true.
     * </p>
     *
     * @param suspectLocalFileHeader the bytes read from the underlying stream in the expectation that they would hold the local file header of the next entry.
     * @return true if this looks like an APK signing block.
     * @see <a href="https://source.android.com/security/apksigning/v2">https://source.android.com/security/apksigning/v2</a>
     */
    private boolean isApkSigningBlock(final byte[] suspectLocalFileHeader) throws IOException {
        // length of block excluding the size field itself
        final BigInteger len = ZipEightByteInteger.getValue(suspectLocalFileHeader);
        // LFH has already been read and all but the first eight bytes contain (part of) the APK signing block,
        // also subtract 16 bytes in order to position us at the magic string
        BigInteger toSkip = len.add(BigInteger.valueOf(DWORD - suspectLocalFileHeader.length - (long) APK_SIGNING_BLOCK_MAGIC.length));
        final byte[] magic = new byte[APK_SIGNING_BLOCK_MAGIC.length];
        try {
            if (toSkip.signum() < 0) {
                // suspectLocalFileHeader contains the start of suspect magic string
                final int off = suspectLocalFileHeader.length + toSkip.intValue();
                // length was shorter than magic length
                if (off < DWORD) {
                    return false;
                }
                final int bytesInBuffer = Math.abs(toSkip.intValue());
                System.arraycopy(suspectLocalFileHeader, off, magic, 0, Math.min(bytesInBuffer, magic.length));
                if (bytesInBuffer < magic.length) {
                    readFully(magic, bytesInBuffer);
                }
            } else {
                while (toSkip.compareTo(LONG_MAX) > 0) {
                    realSkip(Long.MAX_VALUE);
                    toSkip = toSkip.add(LONG_MAX.negate());
                }
                realSkip(toSkip.longValue());
                readFully(magic);
            }
        } catch (final EOFException ex) { // NOSONAR
            // length was invalid
            return false;
        }
        return Arrays.equals(magic, APK_SIGNING_BLOCK_MAGIC);
    }

    private boolean isFirstByteOfEocdSig(final int b) {
        return b == ZipArchiveOutputStream.EOCD_SIG[0];
    }

    /**
     * Records whether a Zip64 extra is present and sets the size information from it if sizes are 0xFFFFFFFF and the entry doesn't use a data descriptor.
     */
    private void processZip64Extra(final ZipLong size, final ZipLong cSize) throws ZipException {
        final ZipExtraField extra = current.entry.getExtraField(Zip64ExtendedInformationExtraField.HEADER_ID);
        if (extra != null && !(extra instanceof Zip64ExtendedInformationExtraField)) {
            throw new ZipException("Archive contains unparseable zip64 extra field");
        }
        final Zip64ExtendedInformationExtraField z64 = (Zip64ExtendedInformationExtraField) extra;
        current.usesZip64 = z64 != null;
        if (!current.hasDataDescriptor) {
            if (z64 != null // same as current.usesZip64 but avoids NPE warning
                    && (ZipLong.ZIP64_MAGIC.equals(cSize) || ZipLong.ZIP64_MAGIC.equals(size))) {
                if (z64.getCompressedSize() == null || z64.getSize() == null) {
                    // avoid NPE if it's a corrupted ZIP archive
                    throw new ZipException("Archive contains corrupted zip64 extra field");
                }
                long s = z64.getCompressedSize().getLongValue();
                if (s < 0) {
                    throw new ZipException("Broken archive, entry with negative compressed size");
                }
                current.entry.setCompressedSize(s);
                s = z64.getSize().getLongValue();
                if (s < 0) {
                    throw new ZipException("Broken archive, entry with negative size");
                }
                current.entry.setSize(s);
            } else if (cSize != null && size != null) {
                if (cSize.getValue() < 0) {
                    throw new ZipException("Broken archive, entry with negative compressed size");
                }
                current.entry.setCompressedSize(cSize.getValue());
                if (size.getValue() < 0) {
                    throw new ZipException("Broken archive, entry with negative size");
                }
                current.entry.setSize(size.getValue());
            }
        }
    }

    private void pushback(final byte[] buf, final int offset, final int length) throws IOException {
        if (offset < 0) {
            // Instead of ArrayIndexOutOfBoundsException
            throw new ArchiveException("Negative offset %,d into buffer", offset);
        }
        ((PushbackInputStream) in).unread(buf, offset, length);
        pushedBackBytes(length);
    }

    @Override
    public int read(final byte[] buffer, final int offset, final int length) throws IOException {
        org.apache.commons.io.IOUtils.checkFromIndexSize(buffer, offset, length);
        if (length == 0) {
            return 0;
        }
        if (closed) {
            throw new ArchiveException("The stream is closed");
        }
        if (current == null) {
            return -1;
        }
        // avoid int overflow, check null buffer
        if (offset > buffer.length || length < 0 || offset < 0 || buffer.length - offset < length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        ZipUtil.checkRequestedFeatures(current.entry);
        if (!supportsDataDescriptorFor(current.entry)) {
            throw new UnsupportedZipFeatureException(UnsupportedZipFeatureException.Feature.DATA_DESCRIPTOR, current.entry);
        }
        if (!supportsCompressedSizeFor(current.entry)) {
            throw new UnsupportedZipFeatureException(UnsupportedZipFeatureException.Feature.UNKNOWN_COMPRESSED_SIZE, current.entry);
        }
        final int read;
        final int method = current.entry.getMethod();
        if (method == ZipArchiveOutputStream.STORED) {
            read = readStored(buffer, offset, length);
        } else if (method == ZipArchiveOutputStream.DEFLATED) {
            read = readDeflated(buffer, offset, length);
        } else if (method == ZipMethod.UNSHRINKING.getCode() || method == ZipMethod.IMPLODING.getCode() || method == ZipMethod.ENHANCED_DEFLATED.getCode()
                || method == ZipMethod.BZIP2.getCode() || ZipMethod.isZstd(method) || method == ZipMethod.XZ.getCode()) {
            read = current.checkInputStream().read(buffer, offset, length);
        } else {
            throw new UnsupportedZipFeatureException(ZipMethod.getMethodByCode(method), current.entry);
        }
        if (read >= 0) {
            current.crc.update(buffer, offset, read);
            uncompressedCount += read;
        }
        return read;
    }

    private void readDataDescriptor() throws IOException {
        readFully(wordBuf);
        ZipLong val = new ZipLong(wordBuf);
        if (ZipLong.DD_SIG.equals(val)) {
            // data descriptor with signature, skip sig
            readFully(wordBuf);
            val = new ZipLong(wordBuf);
        }
        current.entry.setCrc(val.getValue());
        // if there is a ZIP64 extra field, sizes are eight bytes
        // each, otherwise four bytes each. Unfortunately some
        // implementations - namely Java7 - use eight bytes without
        // using a ZIP64 extra field -
        // https://bugs.sun.com/bugdatabase/view_bug.do?bug_id=7073588
        // just read 16 bytes and check whether bytes nine to twelve
        // look like one of the signatures of what could follow a data
        // descriptor (ignoring archive decryption headers for now).
        // If so, push back eight bytes and assume sizes are four
        // bytes, otherwise sizes are eight bytes each.
        readFully(twoDwordBuf);
        final ZipLong potentialSig = new ZipLong(twoDwordBuf, DWORD);
        if (potentialSig.equals(ZipLong.CFH_SIG) || potentialSig.equals(ZipLong.LFH_SIG)) {
            pushback(twoDwordBuf, DWORD, DWORD);
            long size = ZipLong.getValue(twoDwordBuf);
            if (size < 0) {
                throw new ZipException("Broken archive, entry with negative compressed size");
            }
            current.entry.setCompressedSize(size);
            size = ZipLong.getValue(twoDwordBuf, WORD);
            if (size < 0) {
                throw new ZipException("Broken archive, entry with negative size");
            }
            current.entry.setSize(size);
        } else {
            long size = ZipEightByteInteger.getLongValue(twoDwordBuf);
            if (size < 0) {
                throw new ZipException("Broken archive, entry with negative compressed size");
            }
            current.entry.setCompressedSize(size);
            size = ZipEightByteInteger.getLongValue(twoDwordBuf, DWORD);
            if (size < 0) {
                throw new ZipException("Broken archive, entry with negative size");
            }
            current.entry.setSize(size);
        }
    }

    /**
     * Implements read for DEFLATED entries.
     */
    private int readDeflated(final byte[] buffer, final int offset, final int length) throws IOException {
        final int read = readFromInflater(buffer, offset, length);
        if (read <= 0) {
            if (inf.finished()) {
                return -1;
            }
            if (inf.needsDictionary()) {
                throw new ZipException("This archive needs a preset dictionary which is not supported by Commons Compress.");
            }
            if (read == -1) {
                throw new ArchiveException("Truncated ZIP file");
            }
        }
        return read;
    }

    /**
     * Fills the given array with the first local file header and deals with splitting/spanning markers that may prefix the first LFH.
     */
    private boolean readFirstLocalFileHeader() throws IOException {
        // for empty archive, we may get only EOCD size:
        final byte[] header = new byte[Math.min(LFH_LEN, ZipFile.MIN_EOCD_SIZE)];
        readFully(header);
        try {
            READ_LOOP: for (int i = 0;;) {
                for (int j = 0; i <= PREAMBLE_GARBAGE_MAX_SIZE - 4 && j <= header.length - 4; ++j, ++i) {
                    final ZipLong sig = new ZipLong(header, j);
                    if (sig.equals(ZipLong.LFH_SIG) || sig.equals(ZipLong.SINGLE_SEGMENT_SPLIT_MARKER) || sig.equals(ZipLong.DD_SIG)) {
                        // regular archive containing at least one entry:
                        System.arraycopy(header, j, header, 0, header.length - j);
                        readFully(header, header.length - j);
                        break READ_LOOP;
                    }
                    if (sig.equals(new ZipLong(ZipArchiveOutputStream.EOCD_SIG))) {
                        // empty archive:
                        pushback(header, j, header.length - j);
                        return false;
                    }
                }
                if (i >= PREAMBLE_GARBAGE_MAX_SIZE - 4) {
                    throw new ZipException("Cannot find zip signature within the first " + PREAMBLE_GARBAGE_MAX_SIZE + " bytes");
                }
                System.arraycopy(header, header.length - 3, header, 0, 3);
                readFully(header, 3);
            }
            System.arraycopy(header, 0, lfhBuf, 0, header.length);
            readFully(lfhBuf, header.length);
        } catch (final EOFException ex) {
            throw new ZipException("Cannot find zip signature within the file");
        }
        final ZipLong sig = new ZipLong(lfhBuf);
        if (!skipSplitSignature && sig.equals(ZipLong.DD_SIG)) {
            throw new UnsupportedZipFeatureException(UnsupportedZipFeatureException.Feature.SPLITTING);
        }
        // the split ZIP signature(08074B50) should only be skipped when the skipSplitSignature is set
        if (sig.equals(ZipLong.SINGLE_SEGMENT_SPLIT_MARKER) || sig.equals(ZipLong.DD_SIG)) {
            // Just skip over the marker.
            System.arraycopy(lfhBuf, 4, lfhBuf, 0, lfhBuf.length - 4);
            readFully(lfhBuf, lfhBuf.length - 4);
        }
        return true;
    }

    /**
     * Potentially reads more bytes to fill the inflater's buffer and reads from it.
     */
    private int readFromInflater(final byte[] buffer, final int offset, final int length) throws IOException {
        int read = 0;
        do {
            if (inf.needsInput()) {
                final int l = fill();
                if (l > 0) {
                    current.bytesReadFromStream += buf.limit();
                } else if (l == -1) {
                    return -1;
                } else {
                    break;
                }
            }
            try {
                read = inf.inflate(buffer, offset, length);
            } catch (final DataFormatException e) {
                throw ZipUtil.newZipException(e.getMessage(), e);
            }
        } while (read == 0 && inf.needsInput());
        return read;
    }

    private void readFully(final byte[] b) throws IOException {
        readFully(b, 0);
    }

    private void readFully(final byte[] b, final int off) throws IOException {
        final int len = b.length - off;
        final int count = IOUtils.readFully(in, b, off, len);
        count(count);
        if (count < len) {
            throw new EOFException();
        }
    }
    // End of Central Directory Record
    // end of central dir signature WORD
    // number of this disk SHORT
    // number of the disk with the
    // start of the central directory SHORT
    // total number of entries in the
    // central directory on this disk SHORT
    // total number of entries in
    // the central directory SHORT
    // size of the central directory WORD
    // offset of start of central
    // directory with respect to
    // the starting disk number WORD
    // .ZIP file comment length SHORT
    // .ZIP file comment up to 64KB
    //

    /**
     * Reads bytes by reading from the underlying stream rather than the (potentially inflating) archive stream - which {@link #read} would do.
     *
     * Also updates bytes-read counter.
     */
    private int readOneByte() throws IOException {
        final int b = in.read();
        if (b != -1) {
            count(1);
        }
        return b;
    }

    private byte[] readRange(final int len) throws IOException {
        final byte[] ret = IOUtils.readRange(in, len);
        count(ret.length);
        if (ret.length < len) {
            throw new EOFException();
        }
        return ret;
    }

    /**
     * Implements read for STORED entries.
     */
    private int readStored(final byte[] buffer, final int offset, final int length) throws IOException {
        if (current.hasDataDescriptor) {
            if (lastStoredEntry == null) {
                readStoredEntry();
            }
            return lastStoredEntry.read(buffer, offset, length);
        }
        final long csize = current.entry.getSize();
        if (current.bytesRead >= csize) {
            return -1;
        }
        if (buf.position() >= buf.limit()) {
            buf.position(0);
            final int l = in.read(buf.array());
            if (l == -1) {
                buf.limit(0);
                throw new ArchiveException("Truncated ZIP file");
            }
            buf.limit(l);
            count(l);
            current.bytesReadFromStream += l;
        }
        int toRead = Math.min(buf.remaining(), length);
        if (csize - current.bytesRead < toRead) {
            // if it is smaller than toRead then it fits into an int
            toRead = (int) (csize - current.bytesRead);
        }
        buf.get(buffer, offset, toRead);
        current.bytesRead += toRead;
        return toRead;
    }

    /**
     * Caches a stored entry that uses the data descriptor.
     * <ul>
     * <li>Reads a stored entry until the signature of a local file header, central directory header or data descriptor has been found.</li>
     * <li>Stores all entry data in lastStoredEntry.
     * </p>
     * <li>Rewinds the stream to position at the data descriptor.</li>
     * <li>reads the data descriptor</li>
     * </ul>
     * <p>
     * After calling this method the entry should know its size, the entry's data is cached and the stream is positioned at the next local file or central
     * directory header.
     * </p>
     */
    private void readStoredEntry() throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int off = 0;
        boolean done = false;
        // length of DD without signature
        final int ddLen = current.usesZip64 ? WORD + 2 * DWORD : 3 * WORD;
        while (!done) {
            final int r = in.read(buf.array(), off, ZipArchiveOutputStream.BUFFER_SIZE - off);
            if (r <= 0) {
                // read the whole archive without ever finding a
                // central directory
                throw new ArchiveException("Truncated ZIP file");
            }
            if (r + off < 4) {
                // buffer too small to check for a signature, loop
                off += r;
                continue;
            }
            done = bufferContainsSignature(bos, off, r, ddLen);
            if (!done) {
                off = cacheBytesRead(bos, off, r, ddLen);
            }
        }
        if (current.entry.getCompressedSize() != current.entry.getSize()) {
            throw new ZipException("Compressed and uncompressed size don't match" + USE_ZIPFILE_INSTEAD_OF_STREAM_DISCLAIMER);
        }
        final byte[] b = bos.toByteArray();
        if (b.length != current.entry.getSize()) {
            throw new ZipException("Actual and claimed size don't match" + USE_ZIPFILE_INSTEAD_OF_STREAM_DISCLAIMER);
        }
        lastStoredEntry = new ByteArrayInputStream(b);
    }

    /**
     * Skips bytes by reading from the underlying stream rather than the (potentially inflating) archive stream - which {@link #skip} would do.
     *
     * Also updates bytes-read counter.
     */
    private void realSkip(final long value) throws IOException {
        if (value >= 0) {
            long skipped = 0;
            while (skipped < value) {
                final long rem = value - skipped;
                final int x = in.read(skipBuf, 0, (int) (skipBuf.length > rem ? rem : skipBuf.length));
                if (x == -1) {
                    return;
                }
                count(x);
                skipped += x;
            }
            return;
        }
        throw new IllegalArgumentException();
    }

    /**
     * Currently unused.
     *
     * Sets the custom extra fields factory.
     *
     * @param extraFieldSupport the lookup function based on extra field header id.
     * @return the archive.
     */
    public ZipArchiveInputStream setExtraFieldSupport(final Function<ZipShort, ZipExtraField> extraFieldSupport) {
        // this.extraFieldSupport = extraFieldSupport;
        return this;
    }

    /**
     * Skips over and discards value bytes of data from this input stream.
     * <p>
     * This implementation may end up skipping over some smaller number of bytes, possibly 0, if and only if it reaches the end of the underlying stream.
     * </p>
     * <p>
     * The actual number of bytes skipped is returned.
     * </p>
     *
     * @param value the number of bytes to be skipped.
     * @return the actual number of bytes skipped.
     * @throws IOException              - if an I/O error occurs.
     * @throws IllegalArgumentException - if value is negative.
     */
    @Override
    public long skip(final long value) throws IOException {
        if (value >= 0) {
            long skipped = 0;
            while (skipped < value) {
                final long rem = value - skipped;
                final int x = read(skipBuf, 0, (int) (skipBuf.length > rem ? rem : skipBuf.length));
                if (x == -1) {
                    return skipped;
                }
                skipped += x;
            }
            return skipped;
        }
        throw new IllegalArgumentException("Negative skip value");
    }

    /**
     * Reads the stream until it find the "End of central directory record" and consumes it as well.
     */
    private void skipRemainderOfArchive() throws IOException {
        // skip over central directory. One LFH has been read too much
        // already. The calculation discounts file names and extra
        // data, so it will be too short.
        if (entriesRead > 0) {
            realSkip((long) entriesRead * CFH_LEN - LFH_LEN);
        }
        final boolean foundEocd = findEocdRecord();
        if (foundEocd) {
            realSkip((long) ZipFile.MIN_EOCD_SIZE - WORD /* signature */ - SHORT /* comment len */);
            readFully(shortBuf);
            // file comment
            final int commentLen = ZipShort.getValue(shortBuf);
            if (commentLen >= 0) {
                realSkip(commentLen);
                return;
            }
        }
        throw new ArchiveException("Truncated ZIP file");
    }

    /**
     * Tests whether the compressed size for the entry is either known or not required by the compression method being used.
     */
    private boolean supportsCompressedSizeFor(final ZipArchiveEntry entry) {
        final int method = entry.getMethod();
        return entry.getCompressedSize() != ArchiveEntry.SIZE_UNKNOWN || method == ZipEntry.DEFLATED || method == ZipMethod.ENHANCED_DEFLATED.getCode()
                || entry.getGeneralPurposeBit().usesDataDescriptor() && supportStoredEntryDataDescriptor && method == ZipEntry.STORED
                || ZipMethod.isZstd(method) || method == ZipMethod.XZ.getCode();
    }

    /**
     * Tests whether this entry requires a data descriptor this library can work with.
     *
     * @return true if supportStoredEntryDataDescriptor is true, the entry doesn't require any data descriptor or the method is DEFLATED or
     *         ENHANCED_DEFLATED.
     */
    private boolean supportsDataDescriptorFor(final ZipArchiveEntry entry) {
        final int method = entry.getMethod();
        return !entry.getGeneralPurposeBit().usesDataDescriptor() || supportStoredEntryDataDescriptor && method == ZipEntry.STORED
                || method == ZipEntry.DEFLATED || method == ZipMethod.ENHANCED_DEFLATED.getCode() || ZipMethod.isZstd(method)
                || method == ZipMethod.XZ.getCode();
    }
}
