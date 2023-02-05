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
package org.apache.commons.compress.archivers.zip;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.Inflater;
import java.util.zip.ZipException;

import org.apache.commons.compress.archivers.EntryStreamOffsets;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.deflate64.Deflate64CompressorInputStream;
import org.apache.commons.compress.utils.BoundedArchiveInputStream;
import org.apache.commons.compress.utils.BoundedSeekableByteChannelInputStream;
import org.apache.commons.compress.utils.CountingInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.compress.utils.InputStreamStatistics;

/**
 * Replacement for {@code java.util.ZipFile}.
 *
 * <p>This class adds support for file name encodings other than UTF-8
 * (which is required to work on ZIP files created by native ZIP tools
 * and is able to skip a preamble like the one found in self
 * extracting archives.  Furthermore it returns instances of
 * {@code org.apache.commons.compress.archivers.zip.ZipArchiveEntry}
 * instead of {@code java.util.zip.ZipEntry}.</p>
 *
 * <p>It doesn't extend {@code java.util.zip.ZipFile} as it would
 * have to reimplement all methods anyway.  Like
 * {@code java.util.ZipFile}, it uses SeekableByteChannel under the
 * covers and supports compressed and uncompressed entries.  As of
 * Apache Commons Compress 1.3 it also transparently supports Zip64
 * extensions and thus individual entries and archives larger than 4
 * GB or with more than 65536 entries.</p>
 *
 * <p>The method signatures mimic the ones of
 * {@code java.util.zip.ZipFile}, with a couple of exceptions:
 *
 * <ul>
 *   <li>There is no getName method.</li>
 *   <li>entries has been renamed to getEntries.</li>
 *   <li>getEntries and getEntry return
 *   {@code org.apache.commons.compress.archivers.zip.ZipArchiveEntry}
 *   instances.</li>
 *   <li>close is allowed to throw IOException.</li>
 * </ul>
 */
public class ZipFile implements Closeable {
    /**
     * Lock-free implementation of BoundedInputStream. The
     * implementation uses positioned reads on the underlying archive
     * file channel and therefore performs significantly faster in
     * concurrent environment.
     */
    private class BoundedFileChannelInputStream extends BoundedArchiveInputStream {
        private final FileChannel archive;

        BoundedFileChannelInputStream(final long start, final long remaining) {
            super(start, remaining);
            archive = (FileChannel) ZipFile.this.archive;
        }

        @Override
        protected int read(final long pos, final ByteBuffer buf) throws IOException {
            final int read = archive.read(buf, pos);
            buf.flip();
            return read;
        }
    }
    /**
     * Extends ZipArchiveEntry to store the offset within the archive.
     */
    private static class Entry extends ZipArchiveEntry {

        Entry() {
        }

        @Override
        public boolean equals(final Object other) {
            if (super.equals(other)) {
                // super.equals would return false if other were not an Entry
                final Entry otherEntry = (Entry) other;
                return getLocalHeaderOffset()
                        == otherEntry.getLocalHeaderOffset()
                    && super.getDataOffset()
                        == otherEntry.getDataOffset()
                    && super.getDiskNumberStart()
                        == otherEntry.getDiskNumberStart();
            }
            return false;
        }

        @Override
        public int hashCode() {
            return 3 * super.hashCode()
                + (int) getLocalHeaderOffset()+(int)(getLocalHeaderOffset()>>32);
        }
    }
    private static final class NameAndComment {
        private final byte[] name;
        private final byte[] comment;
        private NameAndComment(final byte[] name, final byte[] comment) {
            this.name = name;
            this.comment = comment;
        }
    }
    private static class StoredStatisticsStream extends CountingInputStream implements InputStreamStatistics {
        StoredStatisticsStream(final InputStream in) {
            super(in);
        }

        @Override
        public long getCompressedCount() {
            return super.getBytesRead();
        }

        @Override
        public long getUncompressedCount() {
            return getCompressedCount();
        }
    }
    private static final int HASH_SIZE = 509;
    static final int NIBLET_MASK = 0x0f;
    static final int BYTE_SHIFT = 8;
    private static final int POS_0 = 0;

    private static final int POS_1 = 1;

    private static final int POS_2 = 2;

    private static final int POS_3 = 3;

    private static final byte[] ONE_ZERO_BYTE = new byte[1];

    /**
     * Length of a "central directory" entry structure without file
     * name, extra fields or comment.
     */
    private static final int CFH_LEN =
        /* version made by                 */ ZipConstants.SHORT
        /* version needed to extract       */ + ZipConstants.SHORT
        /* general purpose bit flag        */ + ZipConstants.SHORT
        /* compression method              */ + ZipConstants.SHORT
        /* last mod file time              */ + ZipConstants.SHORT
        /* last mod file date              */ + ZipConstants.SHORT
        /* crc-32                          */ + ZipConstants.WORD
        /* compressed size                 */ + ZipConstants.WORD
        /* uncompressed size               */ + ZipConstants.WORD
        /* file name length                */ + ZipConstants. SHORT
        /* extra field length              */ + ZipConstants.SHORT
        /* file comment length             */ + ZipConstants.SHORT
        /* disk number start               */ + ZipConstants.SHORT
        /* internal file attributes        */ + ZipConstants.SHORT
        /* external file attributes        */ + ZipConstants.WORD
        /* relative offset of local header */ + ZipConstants.WORD;

    private static final long CFH_SIG =
        ZipLong.getValue(ZipArchiveOutputStream.CFH_SIG);

    /**
     * Length of the "End of central directory record" - which is
     * supposed to be the last structure of the archive - without file
     * comment.
     */
    static final int MIN_EOCD_SIZE =
        /* end of central dir signature    */ ZipConstants.WORD
        /* number of this disk             */ + ZipConstants.SHORT
        /* number of the disk with the     */
        /* start of the central directory  */ + ZipConstants.SHORT
        /* total number of entries in      */
        /* the central dir on this disk    */ + ZipConstants.SHORT
        /* total number of entries in      */
        /* the central dir                 */ + ZipConstants.SHORT
        /* size of the central directory   */ + ZipConstants.WORD
        /* offset of start of central      */
        /* directory with respect to       */
        /* the starting disk number        */ + ZipConstants.WORD
        /* ZIP file comment length         */ + ZipConstants.SHORT;

    /**
     * Maximum length of the "End of central directory record" with a
     * file comment.
     */
    private static final int MAX_EOCD_SIZE = MIN_EOCD_SIZE
        /* maximum length of ZIP file comment */ + ZipConstants.ZIP64_MAGIC_SHORT;

    /**
     * Offset of the field that holds the location of the length of
     * the central directory inside the "End of central directory
     * record" relative to the start of the "End of central directory
     * record".
     */
    private static final int CFD_LENGTH_OFFSET =
        /* end of central dir signature    */ ZipConstants.WORD
        /* number of this disk             */ + ZipConstants.SHORT
        /* number of the disk with the     */
        /* start of the central directory  */ + ZipConstants.SHORT
        /* total number of entries in      */
        /* the central dir on this disk    */ + ZipConstants.SHORT
        /* total number of entries in      */
        /* the central dir                 */ + ZipConstants.SHORT;

    /**
     * Offset of the field that holds the disk number of the first
     * central directory entry inside the "End of central directory
     * record" relative to the start of the "End of central directory
     * record".
     */
    private static final int CFD_DISK_OFFSET =
            /* end of central dir signature    */ ZipConstants.WORD
            /* number of this disk             */ + ZipConstants.SHORT;
    /**
     * Offset of the field that holds the location of the first
     * central directory entry inside the "End of central directory
     * record" relative to the "number of the disk with the start
     * of the central directory".
     */
    private static final int CFD_LOCATOR_RELATIVE_OFFSET =
            /* total number of entries in      */
            /* the central dir on this disk    */ + ZipConstants.SHORT
            /* total number of entries in      */
            /* the central dir                 */ + ZipConstants.SHORT
            /* size of the central directory   */ + ZipConstants.WORD;
    /**
     * Length of the "Zip64 end of central directory locator" - which
     * should be right in front of the "end of central directory
     * record" if one is present at all.
     */
    private static final int ZIP64_EOCDL_LENGTH =
        /* zip64 end of central dir locator sig */ ZipConstants.WORD
        /* number of the disk with the start    */
        /* start of the zip64 end of            */
        /* central directory                    */ + ZipConstants.WORD
        /* relative offset of the zip64         */
        /* end of central directory record      */ + ZipConstants.DWORD
        /* total number of disks                */ + ZipConstants.WORD;
    /**
     * Offset of the field that holds the location of the "Zip64 end
     * of central directory record" inside the "Zip64 end of central
     * directory locator" relative to the start of the "Zip64 end of
     * central directory locator".
     */
    private static final int ZIP64_EOCDL_LOCATOR_OFFSET =
        /* zip64 end of central dir locator sig */ ZipConstants.WORD
        /* number of the disk with the start    */
        /* start of the zip64 end of            */
        /* central directory                    */ + ZipConstants.WORD;
    /**
     * Offset of the field that holds the location of the first
     * central directory entry inside the "Zip64 end of central
     * directory record" relative to the start of the "Zip64 end of
     * central directory record".
     */
    private static final int ZIP64_EOCD_CFD_LOCATOR_OFFSET =
        /* zip64 end of central dir        */
        /* signature                       */ ZipConstants.WORD
        /* size of zip64 end of central    */
        /* directory record                */ + ZipConstants.DWORD
        /* version made by                 */ + ZipConstants.SHORT
        /* version needed to extract       */ + ZipConstants.SHORT
        /* number of this disk             */ + ZipConstants.WORD
        /* number of the disk with the     */
        /* start of the central directory  */ + ZipConstants.WORD
        /* total number of entries in the  */
        /* central directory on this disk  */ + ZipConstants.DWORD
        /* total number of entries in the  */
        /* central directory               */ + ZipConstants.DWORD
        /* size of the central directory   */ + ZipConstants.DWORD;
    /**
     * Offset of the field that holds the disk number of the first
     * central directory entry inside the "Zip64 end of central
     * directory record" relative to the start of the "Zip64 end of
     * central directory record".
     */
    private static final int ZIP64_EOCD_CFD_DISK_OFFSET =
            /* zip64 end of central dir        */
            /* signature                       */ ZipConstants.WORD
            /* size of zip64 end of central    */
            /* directory record                */ + ZipConstants.DWORD
            /* version made by                 */ + ZipConstants.SHORT
            /* version needed to extract       */ + ZipConstants.SHORT
            /* number of this disk             */ + ZipConstants.WORD;
    /**
     * Offset of the field that holds the location of the first
     * central directory entry inside the "Zip64 end of central
     * directory record" relative to the "number of the disk
     * with the start of the central directory".
     */
    private static final int ZIP64_EOCD_CFD_LOCATOR_RELATIVE_OFFSET =
            /* total number of entries in the  */
            /* central directory on this disk  */ ZipConstants.DWORD
            /* total number of entries in the  */
            /* central directory               */ + ZipConstants.DWORD
            /* size of the central directory   */ + ZipConstants.DWORD;
    /**
     * Number of bytes in local file header up to the &quot;length of
     * file name&quot; entry.
     */
    private static final long LFH_OFFSET_FOR_FILENAME_LENGTH =
        /* local file header signature     */ ZipConstants.WORD
        /* version needed to extract       */ + ZipConstants.SHORT
        /* general purpose bit flag        */ + ZipConstants.SHORT
        /* compression method              */ + ZipConstants.SHORT
        /* last mod file time              */ + ZipConstants.SHORT
        /* last mod file date              */ + ZipConstants.SHORT
        /* crc-32                          */ + ZipConstants.WORD
        /* compressed size                 */ + ZipConstants.WORD
        /* uncompressed size               */ + (long) ZipConstants.WORD;

    /**
     * Closes a ZIP file quietly; throwing no IOException, dooes nothing
     * on null input.
     * @param zipFile file to close, can be null
     */
    public static void closeQuietly(final ZipFile zipFile) {
        IOUtils.closeQuietly(zipFile);
    }

    /**
     * List of entries in the order they appear inside the central
     * directory.
     */
    private final List<ZipArchiveEntry> entries = new LinkedList<>();

    /**
     * Maps String to list of ZipArchiveEntrys, name -> actual entries.
     */
    private final Map<String, LinkedList<ZipArchiveEntry>> nameMap = new HashMap<>(HASH_SIZE);

    /**
     * The encoding to use for file names and the file comment.
     *
     * <p>For a list of possible values see <a
     * href="http://java.sun.com/j2se/1.5.0/docs/guide/intl/encoding.doc.html">http://java.sun.com/j2se/1.5.0/docs/guide/intl/encoding.doc.html</a>.
     * Defaults to UTF-8.</p>
     */
    private final String encoding;

    /**
     * The ZIP encoding to use for file names and the file comment.
     */
    private final ZipEncoding zipEncoding;

    /**
     * File name of actual source.
     */
    private final String archiveName;

    /**
     * The actual data source.
     */
    private final SeekableByteChannel archive;

    /**
     * Whether to look for and use Unicode extra fields.
     */
    private final boolean useUnicodeExtraFields;

    /**
     * Whether the file is closed.
     */
    private volatile boolean closed = true;

    /**
     * Whether the ZIP archive is a split ZIP archive
     */
    private final boolean isSplitZipArchive;

    // cached buffers - must only be used locally in the class (COMPRESS-172 - reduce garbage collection)
    private final byte[] dwordBuf = new byte[ZipConstants.DWORD];

    private final byte[] wordBuf = new byte[ZipConstants.WORD];

    private final byte[] cfhBuf = new byte[CFH_LEN];

    private final byte[] shortBuf = new byte[ZipConstants.SHORT];

    private final ByteBuffer dwordBbuf = ByteBuffer.wrap(dwordBuf);

    private final ByteBuffer wordBbuf = ByteBuffer.wrap(wordBuf);

    private final ByteBuffer cfhBbuf = ByteBuffer.wrap(cfhBuf);

    private final ByteBuffer shortBbuf = ByteBuffer.wrap(shortBuf);

    private long centralDirectoryStartDiskNumber, centralDirectoryStartRelativeOffset;

    private long centralDirectoryStartOffset;

    private long firstLocalFileHeaderOffset;

    /**
     * Compares two ZipArchiveEntries based on their offset within the archive.
     *
     * <p>Won't return any meaningful results if one of the entries
     * isn't part of the archive at all.</p>
     *
     * @since 1.1
     */
    private static final Comparator<ZipArchiveEntry> offsetComparator =
        Comparator.comparingLong(ZipArchiveEntry::getDiskNumberStart)
            .thenComparingLong(ZipArchiveEntry::getLocalHeaderOffset);

    /**
     * Opens the given file for reading, assuming "UTF8" for file names.
     *
     * @param f the archive.
     *
     * @throws IOException if an error occurs while reading the file.
     */
    public ZipFile(final File f) throws IOException {
        this(f, ZipEncodingHelper.UTF8);
    }

    /**
     * Opens the given file for reading, assuming the specified
     * encoding for file names and scanning for Unicode extra fields.
     *
     * @param f the archive.
     * @param encoding the encoding to use for file names, use null
     * for the platform's default encoding
     *
     * @throws IOException if an error occurs while reading the file.
     */
    public ZipFile(final File f, final String encoding) throws IOException {
        this(f.toPath(), encoding, true);
    }

    /**
     * Opens the given file for reading, assuming the specified
     * encoding for file names.
     *
     * @param f the archive.
     * @param encoding the encoding to use for file names, use null
     * for the platform's default encoding
     * @param useUnicodeExtraFields whether to use InfoZIP Unicode
     * Extra Fields (if present) to set the file names.
     *
     * @throws IOException if an error occurs while reading the file.
     */
    public ZipFile(final File f, final String encoding, final boolean useUnicodeExtraFields)
        throws IOException {
        this(f.toPath(), encoding, useUnicodeExtraFields, false);
    }

    /**
     * Opens the given file for reading, assuming the specified
     * encoding for file names.
     *
     * <p>By default the central directory record and all local file headers of the archive will be read immediately
     * which may take a considerable amount of time when the archive is big. The {@code ignoreLocalFileHeader} parameter
     * can be set to {@code true} which restricts parsing to the central directory. Unfortunately the local file header
     * may contain information not present inside of the central directory which will not be available when the argument
     * is set to {@code true}. This includes the content of the Unicode extra field, so setting {@code
     * ignoreLocalFileHeader} to {@code true} means {@code useUnicodeExtraFields} will be ignored effectively.</p>
     *
     * @param f the archive.
     * @param encoding the encoding to use for file names, use null
     * for the platform's default encoding
     * @param useUnicodeExtraFields whether to use InfoZIP Unicode
     * Extra Fields (if present) to set the file names.
     * @param ignoreLocalFileHeader whether to ignore information
     * stored inside the local file header (see the notes in this method's javadoc)
     *
     * @throws IOException if an error occurs while reading the file.
     * @since 1.19
     */
    public ZipFile(final File f, final String encoding, final boolean useUnicodeExtraFields,
                   final boolean ignoreLocalFileHeader)
        throws IOException {
        this(Files.newByteChannel(f.toPath(), EnumSet.of(StandardOpenOption.READ)),
             f.getAbsolutePath(), encoding, useUnicodeExtraFields, true, ignoreLocalFileHeader);
    }

    /**
     * Opens the given path for reading, assuming "UTF8" for file names.
     *
     * @param path path to the archive.
     * @throws IOException if an error occurs while reading the file.
     * @since 1.22
     */
    public ZipFile(final Path path) throws IOException {
        this(path, ZipEncodingHelper.UTF8);
    }

    /**
     * Opens the given path for reading, assuming the specified
     * encoding for file names and scanning for Unicode extra fields.
     *
     * @param path path to the archive.
     * @param encoding the encoding to use for file names, use null
     * for the platform's default encoding
     * @throws IOException if an error occurs while reading the file.
     * @since 1.22
     */
    public ZipFile(final Path path, final String encoding) throws IOException {
        this(path, encoding, true);
    }


    /**
     * Opens the given path for reading, assuming the specified
     * encoding for file names.
     *
     * @param path path to the archive.
     * @param encoding the encoding to use for file names, use null
     * for the platform's default encoding
     * @param useUnicodeExtraFields whether to use InfoZIP Unicode
     * Extra Fields (if present) to set the file names.
     * @throws IOException if an error occurs while reading the file.
     * @since 1.22
     */
    public ZipFile(final Path path, final String encoding, final boolean useUnicodeExtraFields)
            throws IOException {
        this(path, encoding, useUnicodeExtraFields, false);
    }

    /**
     * Opens the given path for reading, assuming the specified
     * encoding for file names.
     * <p>By default the central directory record and all local file headers of the archive will be read immediately
     * which may take a considerable amount of time when the archive is big. The {@code ignoreLocalFileHeader} parameter
     * can be set to {@code true} which restricts parsing to the central directory. Unfortunately the local file header
     * may contain information not present inside of the central directory which will not be available when the argument
     * is set to {@code true}. This includes the content of the Unicode extra field, so setting {@code
     * ignoreLocalFileHeader} to {@code true} means {@code useUnicodeExtraFields} will be ignored effectively.</p>
     *
     * @param path path to the archive.
     * @param encoding the encoding to use for file names, use null
     * for the platform's default encoding
     * @param useUnicodeExtraFields whether to use InfoZIP Unicode
     * Extra Fields (if present) to set the file names.
     * @param ignoreLocalFileHeader whether to ignore information
     * stored inside the local file header (see the notes in this method's javadoc)
     * @throws IOException if an error occurs while reading the file.
     * @since 1.22
     */
    public ZipFile(final Path path, final String encoding, final boolean useUnicodeExtraFields,
                   final boolean ignoreLocalFileHeader)
            throws IOException {
        this(Files.newByteChannel(path, EnumSet.of(StandardOpenOption.READ)),
                path.toAbsolutePath().toString(), encoding, useUnicodeExtraFields,
                true, ignoreLocalFileHeader);
    }

    /**
     * Opens the given channel for reading, assuming "UTF8" for file names.
     *
     * <p>{@link
     * org.apache.commons.compress.utils.SeekableInMemoryByteChannel}
     * allows you to read from an in-memory archive.</p>
     *
     * @param channel the archive.
     *
     * @throws IOException if an error occurs while reading the file.
     * @since 1.13
     */
    public ZipFile(final SeekableByteChannel channel)
            throws IOException {
        this(channel, "unknown archive", ZipEncodingHelper.UTF8, true);
    }

    /**
     * Opens the given channel for reading, assuming the specified
     * encoding for file names.
     *
     * <p>{@link
     * org.apache.commons.compress.utils.SeekableInMemoryByteChannel}
     * allows you to read from an in-memory archive.</p>
     *
     * @param channel the archive.
     * @param encoding the encoding to use for file names, use null
     * for the platform's default encoding
     *
     * @throws IOException if an error occurs while reading the file.
     * @since 1.13
     */
    public ZipFile(final SeekableByteChannel channel, final String encoding)
        throws IOException {
        this(channel, "unknown archive", encoding, true);
    }

    /**
     * Opens the given channel for reading, assuming the specified
     * encoding for file names.
     *
     * <p>{@link
     * org.apache.commons.compress.utils.SeekableInMemoryByteChannel}
     * allows you to read from an in-memory archive.</p>
     *
     * @param channel the archive.
     * @param archiveName name of the archive, used for error messages only.
     * @param encoding the encoding to use for file names, use null
     * for the platform's default encoding
     * @param useUnicodeExtraFields whether to use InfoZIP Unicode
     * Extra Fields (if present) to set the file names.
     *
     * @throws IOException if an error occurs while reading the file.
     * @since 1.13
     */
    public ZipFile(final SeekableByteChannel channel, final String archiveName,
                   final String encoding, final boolean useUnicodeExtraFields)
        throws IOException {
        this(channel, archiveName, encoding, useUnicodeExtraFields, false, false);
    }

    /**
     * Opens the given channel for reading, assuming the specified
     * encoding for file names.
     *
     * <p>{@link
     * org.apache.commons.compress.utils.SeekableInMemoryByteChannel}
     * allows you to read from an in-memory archive.</p>
     *
     * <p>By default the central directory record and all local file headers of the archive will be read immediately
     * which may take a considerable amount of time when the archive is big. The {@code ignoreLocalFileHeader} parameter
     * can be set to {@code true} which restricts parsing to the central directory. Unfortunately the local file header
     * may contain information not present inside of the central directory which will not be available when the argument
     * is set to {@code true}. This includes the content of the Unicode extra field, so setting {@code
     * ignoreLocalFileHeader} to {@code true} means {@code useUnicodeExtraFields} will be ignored effectively.</p>
     *
     * @param channel the archive.
     * @param archiveName name of the archive, used for error messages only.
     * @param encoding the encoding to use for file names, use null
     * for the platform's default encoding
     * @param useUnicodeExtraFields whether to use InfoZIP Unicode
     * Extra Fields (if present) to set the file names.
     * @param ignoreLocalFileHeader whether to ignore information
     * stored inside the local file header (see the notes in this method's javadoc)
     *
     * @throws IOException if an error occurs while reading the file.
     * @since 1.19
     */
    public ZipFile(final SeekableByteChannel channel, final String archiveName,
                   final String encoding, final boolean useUnicodeExtraFields,
                   final boolean ignoreLocalFileHeader)
        throws IOException {
        this(channel, archiveName, encoding, useUnicodeExtraFields, false, ignoreLocalFileHeader);
    }

    private ZipFile(final SeekableByteChannel channel, final String archiveName,
                    final String encoding, final boolean useUnicodeExtraFields,
                    final boolean closeOnError, final boolean ignoreLocalFileHeader)
        throws IOException {
        isSplitZipArchive = (channel instanceof ZipSplitReadOnlySeekableByteChannel);

        this.archiveName = archiveName;
        this.encoding = encoding;
        this.zipEncoding = ZipEncodingHelper.getZipEncoding(encoding);
        this.useUnicodeExtraFields = useUnicodeExtraFields;
        archive = channel;
        boolean success = false;
        try {
            final Map<ZipArchiveEntry, NameAndComment> entriesWithoutUTF8Flag =
                populateFromCentralDirectory();
            if (!ignoreLocalFileHeader) {
                resolveLocalFileHeaderData(entriesWithoutUTF8Flag);
            }
            fillNameMap();
            success = true;
        } catch (final IOException e) {
            throw new IOException("Error on ZipFile " + archiveName, e);
        } finally {
            closed = !success;
            if (!success && closeOnError) {
                IOUtils.closeQuietly(archive);
            }
        }
    }

    /**
     * Opens the given file for reading, assuming "UTF8".
     *
     * @param name name of the archive.
     *
     * @throws IOException if an error occurs while reading the file.
     */
    public ZipFile(final String name) throws IOException {
        this(new File(name).toPath(), ZipEncodingHelper.UTF8);
    }

    /**
     * Opens the given file for reading, assuming the specified
     * encoding for file names, scanning unicode extra fields.
     *
     * @param name name of the archive.
     * @param encoding the encoding to use for file names, use null
     * for the platform's default encoding
     *
     * @throws IOException if an error occurs while reading the file.
     */
    public ZipFile(final String name, final String encoding) throws IOException {
        this(new File(name).toPath(), encoding, true);
    }

    /**
     * Whether this class is able to read the given entry.
     *
     * <p>May return false if it is set up to use encryption or a
     * compression method that hasn't been implemented yet.</p>
     * @since 1.1
     * @param ze the entry
     * @return whether this class is able to read the given entry.
     */
    public boolean canReadEntryData(final ZipArchiveEntry ze) {
        return ZipUtil.canHandleEntryData(ze);
    }

    /**
     * Closes the archive.
     * @throws IOException if an error occurs closing the archive.
     */
    @Override
    public void close() throws IOException {
        // this flag is only written here and read in finalize() which
        // can never be run in parallel.
        // no synchronization needed.
        closed = true;

        archive.close();
    }

    /**
     * Transfer selected entries from this ZIP file to a given #ZipArchiveOutputStream.
     * Compression and all other attributes will be as in this file.
     * <p>This method transfers entries based on the central directory of the ZIP file.</p>
     *
     * @param target The zipArchiveOutputStream to write the entries to
     * @param predicate A predicate that selects which entries to write
     * @throws IOException on error
     */
    public void copyRawEntries(final ZipArchiveOutputStream target, final ZipArchiveEntryPredicate predicate)
            throws IOException {
        final Enumeration<ZipArchiveEntry> src = getEntriesInPhysicalOrder();
        while (src.hasMoreElements()) {
            final ZipArchiveEntry entry = src.nextElement();
            if (predicate.test( entry)) {
                target.addRawArchiveEntry(entry, getRawInputStream(entry));
            }
        }
    }

    /**
     * Creates new BoundedInputStream, according to implementation of
     * underlying archive channel.
     */
    private BoundedArchiveInputStream createBoundedInputStream(final long start, final long remaining) {
        if (start < 0 || remaining < 0 || start + remaining < start) {
            throw new IllegalArgumentException("Corrupted archive, stream boundaries"
                + " are out of range");
        }
        return archive instanceof FileChannel ?
            new BoundedFileChannelInputStream(start, remaining) :
            new BoundedSeekableByteChannelInputStream(start, remaining, archive);
    }

    private void fillNameMap() {
        entries.forEach(ze -> {
            // entries is filled in populateFromCentralDirectory and
            // never modified
            final String name = ze.getName();
            final LinkedList<ZipArchiveEntry> entriesOfThatName = nameMap.computeIfAbsent(name, k -> new LinkedList<>());
            entriesOfThatName.addLast(ze);
        });
    }

    /**
     * Ensures that the close method of this ZIP file is called when
     * there are no more references to it.
     * @see #close()
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            if (!closed) {
                System.err.println("Cleaning up unclosed ZipFile for archive "
                                   + archiveName);
                close();
            }
        } finally {
            super.finalize();
        }
    }

    /**
     * Gets an InputStream for reading the content before the first local file header.
     *
     * @return null if there is no content before the first local file header.
     * Otherwise returns a stream to read the content before the first local file header.
     * @since 1.23
     */
    public InputStream getContentBeforeFirstLocalFileHeader() {
        return firstLocalFileHeaderOffset == 0
                ? null : createBoundedInputStream(0, firstLocalFileHeaderOffset);
    }

    private long getDataOffset(final ZipArchiveEntry ze) throws IOException {
        final long s = ze.getDataOffset();
        if (s == EntryStreamOffsets.OFFSET_UNKNOWN) {
            setDataOffset(ze);
            return ze.getDataOffset();
        }
        return s;
    }

    /**
     * Gets the encoding to use for file names and the file comment.
     *
     * @return null if using the platform's default character encoding.
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * Gets all entries.
     *
     * <p>Entries will be returned in the same order they appear
     * within the archive's central directory.</p>
     *
     * @return all entries as {@link ZipArchiveEntry} instances
     */
    public Enumeration<ZipArchiveEntry> getEntries() {
        return Collections.enumeration(entries);
    }

    /**
     * Gets all named entries in the same order they appear within
     * the archive's central directory.
     *
     * @param name name of the entry.
     * @return the Iterable&lt;ZipArchiveEntry&gt; corresponding to the
     * given name
     * @since 1.6
     */
    public Iterable<ZipArchiveEntry> getEntries(final String name) {
        final List<ZipArchiveEntry> entriesOfThatName = nameMap.get(name);
        return entriesOfThatName != null ? entriesOfThatName
            : Collections.emptyList();
    }

    /**
     * Gets all entries in physical order.
     *
     * <p>Entries will be returned in the same order their contents
     * appear within the archive.</p>
     *
     * @return all entries as {@link ZipArchiveEntry} instances
     *
     * @since 1.1
     */
    public Enumeration<ZipArchiveEntry> getEntriesInPhysicalOrder() {
        final ZipArchiveEntry[] allEntries = entries.toArray(ZipArchiveEntry.EMPTY_ARRAY);
        Arrays.sort(allEntries, offsetComparator);
        return Collections.enumeration(Arrays.asList(allEntries));
    }

    /**
     * Gets all named entries in the same order their contents
     * appear within the archive.
     *
     * @param name name of the entry.
     * @return the Iterable&lt;ZipArchiveEntry&gt; corresponding to the
     * given name
     * @since 1.6
     */
    public Iterable<ZipArchiveEntry> getEntriesInPhysicalOrder(final String name) {
        ZipArchiveEntry[] entriesOfThatName = ZipArchiveEntry.EMPTY_ARRAY;
        final LinkedList<ZipArchiveEntry> linkedList = nameMap.get(name);
        if (linkedList != null) {
            entriesOfThatName = linkedList.toArray(entriesOfThatName);
            Arrays.sort(entriesOfThatName, offsetComparator);
        }
        return Arrays.asList(entriesOfThatName);
    }

    /**
     * Gets a named entry or {@code null} if no entry by
     * that name exists.
     *
     * <p>If multiple entries with the same name exist the first entry
     * in the archive's central directory by that name is
     * returned.</p>
     *
     * @param name name of the entry.
     * @return the ZipArchiveEntry corresponding to the given name - or
     * {@code null} if not present.
     */
    public ZipArchiveEntry getEntry(final String name) {
        final LinkedList<ZipArchiveEntry> entriesOfThatName = nameMap.get(name);
        return entriesOfThatName != null ? entriesOfThatName.getFirst() : null;
    }

    /**
     * Gets the offset of the first local file header in the file.
     *
     * @return the length of the content before the first local file header
     * @since 1.23
     */
    public long getFirstLocalFileHeaderOffset() {
        return firstLocalFileHeaderOffset;
    }

    /**
     * Gets an InputStream for reading the contents of the given entry.
     *
     * @param zipEntry the entry to get the stream for.
     * @return a stream to read the entry from. The returned stream
     * implements {@link InputStreamStatistics}.
     * @throws IOException if unable to create an input stream from the zipEntry.
     */
    public InputStream getInputStream(final ZipArchiveEntry zipEntry)
        throws IOException {
        if (!(zipEntry instanceof Entry)) {
            return null;
        }
        // cast validity is checked just above
        ZipUtil.checkRequestedFeatures(zipEntry);

        // doesn't get closed if the method is not supported - which
        // should never happen because of the checkRequestedFeatures
        // call above
        final InputStream is = new BufferedInputStream(getRawInputStream(zipEntry)); //NOSONAR
        switch (ZipMethod.getMethodByCode(zipEntry.getMethod())) {
            case STORED:
                return new StoredStatisticsStream(is);
            case UNSHRINKING:
                return new UnshrinkingInputStream(is);
            case IMPLODING:
                try {
                    return new ExplodingInputStream(zipEntry.getGeneralPurposeBit().getSlidingDictionarySize(),
                            zipEntry.getGeneralPurposeBit().getNumberOfShannonFanoTrees(), is);
                } catch (final IllegalArgumentException ex) {
                    throw new IOException("bad IMPLODE data", ex);
                }
            case DEFLATED:
                final Inflater inflater = new Inflater(true);
                // Inflater with nowrap=true has this odd contract for a zero padding
                // byte following the data stream; this used to be zlib's requirement
                // and has been fixed a long time ago, but the contract persists so
                // we comply.
                // https://docs.oracle.com/javase/7/docs/api/java/util/zip/Inflater.html#Inflater(boolean)
                return new InflaterInputStreamWithStatistics(new SequenceInputStream(is, new ByteArrayInputStream(ONE_ZERO_BYTE)),
                    inflater) {
                    @Override
                    public void close() throws IOException {
                        try {
                            super.close();
                        } finally {
                            inflater.end();
                        }
                    }
                };
            case BZIP2:
                return new BZip2CompressorInputStream(is);
            case ENHANCED_DEFLATED:
                return new Deflate64CompressorInputStream(is);
            case AES_ENCRYPTED:
            case EXPANDING_LEVEL_1:
            case EXPANDING_LEVEL_2:
            case EXPANDING_LEVEL_3:
            case EXPANDING_LEVEL_4:
            case JPEG:
            case LZMA:
            case PKWARE_IMPLODING:
            case PPMD:
            case TOKENIZATION:
            case UNKNOWN:
            case WAVPACK:
            case XZ:
            default:
                throw new UnsupportedZipFeatureException(ZipMethod.getMethodByCode(zipEntry.getMethod()), zipEntry);
        }
    }

    /**
     * Gets the raw stream of the archive entry (compressed form).
     *
     * <p>This method does not relate to how/if we understand the payload in the
     * stream, since we really only intend to move it on to somewhere else.</p>
     *
     * <p>Since version 1.22, this method will make an attempt to read the entry's data
     * stream offset, even if the {@code ignoreLocalFileHeader} parameter was {@code true}
     * in the constructor. An IOException can also be thrown from the body of the method
     * if this lookup fails for some reason.</p>
     *
     * @param ze The entry to get the stream for
     * @return The raw input stream containing (possibly) compressed data.
     * @since 1.11
     * @throws IOException if there is a problem reading data offset (added in version 1.22).
     */
    public InputStream getRawInputStream(final ZipArchiveEntry ze) throws IOException {
        if (!(ze instanceof Entry)) {
            return null;
        }

        final long start = getDataOffset(ze);
        if (start == EntryStreamOffsets.OFFSET_UNKNOWN) {
            return null;
        }
        return createBoundedInputStream(start, ze.getCompressedSize());
    }

    /**
     * Gets the entry's content as a String if isUnixSymlink()
     * returns true for it, otherwise returns null.
     * <p>This method assumes the symbolic link's file name uses the
     * same encoding that as been specified for this ZipFile.</p>
     *
     * @param entry ZipArchiveEntry object that represents the symbolic link
     * @return entry's content as a String
     * @throws IOException problem with content's input stream
     * @since 1.5
     */
    public String getUnixSymlink(final ZipArchiveEntry entry) throws IOException {
        if (entry != null && entry.isUnixSymlink()) {
            try (InputStream in = getInputStream(entry)) {
                return zipEncoding.decode(IOUtils.toByteArray(in));
            }
        }
        return null;
    }

    /**
     * Reads the central directory of the given archive and populates
     * the internal tables with ZipArchiveEntry instances.
     *
     * <p>The ZipArchiveEntrys will know all data that can be obtained from
     * the central directory alone, but not the data that requires the
     * local file header or additional data to be read.</p>
     *
     * @return a map of zipentries that didn't have the language
     * encoding flag set when read.
     */
    private Map<ZipArchiveEntry, NameAndComment> populateFromCentralDirectory()
        throws IOException {
        final HashMap<ZipArchiveEntry, NameAndComment> noUTF8Flag =
            new HashMap<>();

        positionAtCentralDirectory();
        centralDirectoryStartOffset = archive.position();

        wordBbuf.rewind();
        IOUtils.readFully(archive, wordBbuf);
        long sig = ZipLong.getValue(wordBuf);

        if (sig != CFH_SIG && startsWithLocalFileHeader()) {
            throw new IOException("Central directory is empty, can't expand"
                                  + " corrupt archive.");
        }

        while (sig == CFH_SIG) {
            readCentralDirectoryEntry(noUTF8Flag);
            wordBbuf.rewind();
            IOUtils.readFully(archive, wordBbuf);
            sig = ZipLong.getValue(wordBuf);
        }
        return noUTF8Flag;
    }

    /**
     * Searches for either the &quot;Zip64 end of central directory
     * locator&quot; or the &quot;End of central dir record&quot;, parses
     * it and positions the stream at the first central directory
     * record.
     */
    private void positionAtCentralDirectory()
        throws IOException {
        positionAtEndOfCentralDirectoryRecord();
        boolean found = false;
        final boolean searchedForZip64EOCD =
            archive.position() > ZIP64_EOCDL_LENGTH;
        if (searchedForZip64EOCD) {
            archive.position(archive.position() - ZIP64_EOCDL_LENGTH);
            wordBbuf.rewind();
            IOUtils.readFully(archive, wordBbuf);
            found = Arrays.equals(ZipArchiveOutputStream.ZIP64_EOCD_LOC_SIG,
                                  wordBuf);
        }
        if (!found) {
            // not a ZIP64 archive
            if (searchedForZip64EOCD) {
                skipBytes(ZIP64_EOCDL_LENGTH - ZipConstants.WORD);
            }
            positionAtCentralDirectory32();
        } else {
            positionAtCentralDirectory64();
        }
    }

    /**
     * Parses the &quot;End of central dir record&quot; and positions
     * the stream at the first central directory record.
     *
     * Expects stream to be positioned at the beginning of the
     * &quot;End of central dir record&quot;.
     */
    private void positionAtCentralDirectory32()
        throws IOException {
        final long endOfCentralDirectoryRecordOffset = archive.position();
        if (isSplitZipArchive) {
            skipBytes(CFD_DISK_OFFSET);
            shortBbuf.rewind();
            IOUtils.readFully(archive, shortBbuf);
            centralDirectoryStartDiskNumber = ZipShort.getValue(shortBuf);

            skipBytes(CFD_LOCATOR_RELATIVE_OFFSET);

            wordBbuf.rewind();
            IOUtils.readFully(archive, wordBbuf);
            centralDirectoryStartRelativeOffset = ZipLong.getValue(wordBuf);
            ((ZipSplitReadOnlySeekableByteChannel) archive)
                .position(centralDirectoryStartDiskNumber, centralDirectoryStartRelativeOffset);
        } else {
            skipBytes(CFD_LENGTH_OFFSET);
            wordBbuf.rewind();
            IOUtils.readFully(archive, wordBbuf);
            final long centralDirectoryLength = ZipLong.getValue(wordBuf);

            wordBbuf.rewind();
            IOUtils.readFully(archive, wordBbuf);
            centralDirectoryStartDiskNumber = 0;
            centralDirectoryStartRelativeOffset = ZipLong.getValue(wordBuf);

            firstLocalFileHeaderOffset = Long.max(
                    endOfCentralDirectoryRecordOffset - centralDirectoryLength - centralDirectoryStartRelativeOffset,
                    0L);
            archive.position(centralDirectoryStartRelativeOffset + firstLocalFileHeaderOffset);
        }
    }

    /**
     * Parses the &quot;Zip64 end of central directory locator&quot;,
     * finds the &quot;Zip64 end of central directory record&quot; using the
     * parsed information, parses that and positions the stream at the
     * first central directory record.
     *
     * Expects stream to be positioned right behind the &quot;Zip64
     * end of central directory locator&quot;'s signature.
     */
    private void positionAtCentralDirectory64()
        throws IOException {
        if (isSplitZipArchive) {
            wordBbuf.rewind();
            IOUtils.readFully(archive, wordBbuf);
            final long diskNumberOfEOCD = ZipLong.getValue(wordBuf);

            dwordBbuf.rewind();
            IOUtils.readFully(archive, dwordBbuf);
            final long relativeOffsetOfEOCD = ZipEightByteInteger.getLongValue(dwordBuf);
            ((ZipSplitReadOnlySeekableByteChannel) archive)
                .position(diskNumberOfEOCD, relativeOffsetOfEOCD);
        } else {
            skipBytes(ZIP64_EOCDL_LOCATOR_OFFSET
                    - ZipConstants.WORD /* signature has already been read */);
            dwordBbuf.rewind();
            IOUtils.readFully(archive, dwordBbuf);
            archive.position(ZipEightByteInteger.getLongValue(dwordBuf));
        }

        wordBbuf.rewind();
        IOUtils.readFully(archive, wordBbuf);
        if (!Arrays.equals(wordBuf, ZipArchiveOutputStream.ZIP64_EOCD_SIG)) {
            throw new ZipException("Archive's ZIP64 end of central "
                                   + "directory locator is corrupt.");
        }

        if (isSplitZipArchive) {
            skipBytes(ZIP64_EOCD_CFD_DISK_OFFSET
                    - ZipConstants.WORD /* signature has already been read */);
            wordBbuf.rewind();
            IOUtils.readFully(archive, wordBbuf);
            centralDirectoryStartDiskNumber = ZipLong.getValue(wordBuf);

            skipBytes(ZIP64_EOCD_CFD_LOCATOR_RELATIVE_OFFSET);

            dwordBbuf.rewind();
            IOUtils.readFully(archive, dwordBbuf);
            centralDirectoryStartRelativeOffset = ZipEightByteInteger.getLongValue(dwordBuf);
            ((ZipSplitReadOnlySeekableByteChannel) archive)
                .position(centralDirectoryStartDiskNumber, centralDirectoryStartRelativeOffset);
        } else {
            skipBytes(ZIP64_EOCD_CFD_LOCATOR_OFFSET
                    - ZipConstants.WORD /* signature has already been read */);
            dwordBbuf.rewind();
            IOUtils.readFully(archive, dwordBbuf);
            centralDirectoryStartDiskNumber = 0;
            centralDirectoryStartRelativeOffset = ZipEightByteInteger.getLongValue(dwordBuf);
            archive.position(centralDirectoryStartRelativeOffset);
        }
    }

    /**
     * Searches for the and positions the stream at the start of the
     * &quot;End of central dir record&quot;.
     */
    private void positionAtEndOfCentralDirectoryRecord()
        throws IOException {
        final boolean found = tryToLocateSignature(MIN_EOCD_SIZE, MAX_EOCD_SIZE,
                                             ZipArchiveOutputStream.EOCD_SIG);
        if (!found) {
            throw new ZipException("Archive is not a ZIP archive");
        }
    }

    /**
     * Reads an individual entry of the central directory, creats an
     * ZipArchiveEntry from it and adds it to the global maps.
     *
     * @param noUTF8Flag map used to collect entries that don't have
     * their UTF-8 flag set and whose name will be set by data read
     * from the local file header later.  The current entry may be
     * added to this map.
     */
    private void
        readCentralDirectoryEntry(final Map<ZipArchiveEntry, NameAndComment> noUTF8Flag)
        throws IOException {
        cfhBbuf.rewind();
        IOUtils.readFully(archive, cfhBbuf);
        int off = 0;
        final Entry ze = new Entry();

        final int versionMadeBy = ZipShort.getValue(cfhBuf, off);
        off += ZipConstants.SHORT;
        ze.setVersionMadeBy(versionMadeBy);
        ze.setPlatform((versionMadeBy >> BYTE_SHIFT) & NIBLET_MASK);

        ze.setVersionRequired(ZipShort.getValue(cfhBuf, off));
        off += ZipConstants.SHORT; // version required

        final GeneralPurposeBit gpFlag = GeneralPurposeBit.parse(cfhBuf, off);
        final boolean hasUTF8Flag = gpFlag.usesUTF8ForNames();
        final ZipEncoding entryEncoding =
            hasUTF8Flag ? ZipEncodingHelper.UTF8_ZIP_ENCODING : zipEncoding;
        if (hasUTF8Flag) {
            ze.setNameSource(ZipArchiveEntry.NameSource.NAME_WITH_EFS_FLAG);
        }
        ze.setGeneralPurposeBit(gpFlag);
        ze.setRawFlag(ZipShort.getValue(cfhBuf, off));

        off += ZipConstants.SHORT;

        //noinspection MagicConstant
        ze.setMethod(ZipShort.getValue(cfhBuf, off));
        off += ZipConstants.SHORT;

        final long time = ZipUtil.dosToJavaTime(ZipLong.getValue(cfhBuf, off));
        ze.setTime(time);
        off += ZipConstants.WORD;

        ze.setCrc(ZipLong.getValue(cfhBuf, off));
        off += ZipConstants.WORD;

        long size = ZipLong.getValue(cfhBuf, off);
        if (size < 0) {
            throw new IOException("broken archive, entry with negative compressed size");
        }
        ze.setCompressedSize(size);
        off += ZipConstants.WORD;

        size = ZipLong.getValue(cfhBuf, off);
        if (size < 0) {
            throw new IOException("broken archive, entry with negative size");
        }
        ze.setSize(size);
        off += ZipConstants.WORD;

        final int fileNameLen = ZipShort.getValue(cfhBuf, off);
        off += ZipConstants.SHORT;
        if (fileNameLen < 0) {
            throw new IOException("broken archive, entry with negative fileNameLen");
        }

        final int extraLen = ZipShort.getValue(cfhBuf, off);
        off += ZipConstants.SHORT;
        if (extraLen < 0) {
            throw new IOException("broken archive, entry with negative extraLen");
        }

        final int commentLen = ZipShort.getValue(cfhBuf, off);
        off += ZipConstants.SHORT;
        if (commentLen < 0) {
            throw new IOException("broken archive, entry with negative commentLen");
        }

        ze.setDiskNumberStart(ZipShort.getValue(cfhBuf, off));
        off += ZipConstants.SHORT;

        ze.setInternalAttributes(ZipShort.getValue(cfhBuf, off));
        off += ZipConstants.SHORT;

        ze.setExternalAttributes(ZipLong.getValue(cfhBuf, off));
        off += ZipConstants.WORD;

        final byte[] fileName = IOUtils.readRange(archive, fileNameLen);
        if (fileName.length < fileNameLen) {
            throw new EOFException();
        }
        ze.setName(entryEncoding.decode(fileName), fileName);

        // LFH offset,
        ze.setLocalHeaderOffset(ZipLong.getValue(cfhBuf, off) + firstLocalFileHeaderOffset);
        // data offset will be filled later
        entries.add(ze);

        final byte[] cdExtraData = IOUtils.readRange(archive, extraLen);
        if (cdExtraData.length < extraLen) {
            throw new EOFException();
        }
        try {
            ze.setCentralDirectoryExtra(cdExtraData);
        } catch (final RuntimeException ex) {
            final ZipException z = new ZipException("Invalid extra data in entry " + ze.getName());
            z.initCause(ex);
            throw z;
        }

        setSizesAndOffsetFromZip64Extra(ze);
        sanityCheckLFHOffset(ze);

        final byte[] comment = IOUtils.readRange(archive, commentLen);
        if (comment.length < commentLen) {
            throw new EOFException();
        }
        ze.setComment(entryEncoding.decode(comment));

        if (!hasUTF8Flag && useUnicodeExtraFields) {
            noUTF8Flag.put(ze, new NameAndComment(fileName, comment));
        }

        ze.setStreamContiguous(true);
    }

    /**
     * Walks through all recorded entries and adds the data available
     * from the local file header.
     *
     * <p>Also records the offsets for the data to read from the
     * entries.</p>
     */
    private void resolveLocalFileHeaderData(final Map<ZipArchiveEntry, NameAndComment>
                                            entriesWithoutUTF8Flag)
        throws IOException {
        for (final ZipArchiveEntry zipArchiveEntry : entries) {
            // entries is filled in populateFromCentralDirectory and
            // never modified
            final Entry ze = (Entry) zipArchiveEntry;
            final int[] lens = setDataOffset(ze);
            final int fileNameLen = lens[0];
            final int extraFieldLen = lens[1];
            skipBytes(fileNameLen);
            final byte[] localExtraData = IOUtils.readRange(archive, extraFieldLen);
            if (localExtraData.length < extraFieldLen) {
                throw new EOFException();
            }
            try {
                ze.setExtra(localExtraData);
            } catch (final RuntimeException ex) {
                final ZipException z = new ZipException("Invalid extra data in entry " + ze.getName());
                z.initCause(ex);
                throw z;
            }

            if (entriesWithoutUTF8Flag.containsKey(ze)) {
                final NameAndComment nc = entriesWithoutUTF8Flag.get(ze);
                ZipUtil.setNameAndCommentFromExtraFields(ze, nc.name,
                                                         nc.comment);
            }
        }
    }

    private void sanityCheckLFHOffset(final ZipArchiveEntry ze) throws IOException {
        if (ze.getDiskNumberStart() < 0) {
            throw new IOException("broken archive, entry with negative disk number");
        }
        if (ze.getLocalHeaderOffset() < 0) {
            throw new IOException("broken archive, entry with negative local file header offset");
        }
        if (isSplitZipArchive) {
            if (ze.getDiskNumberStart() > centralDirectoryStartDiskNumber) {
                throw new IOException("local file header for " + ze.getName() + " starts on a later disk than central directory");
            }
            if (ze.getDiskNumberStart() == centralDirectoryStartDiskNumber
                && ze.getLocalHeaderOffset() > centralDirectoryStartRelativeOffset) {
                throw new IOException("local file header for " + ze.getName() + " starts after central directory");
            }
        } else if (ze.getLocalHeaderOffset() > centralDirectoryStartOffset) {
            throw new IOException("local file header for " + ze.getName() + " starts after central directory");
        }
    }

    private int[] setDataOffset(final ZipArchiveEntry ze) throws IOException {
        long offset = ze.getLocalHeaderOffset();
        if (isSplitZipArchive) {
            ((ZipSplitReadOnlySeekableByteChannel) archive)
                .position(ze.getDiskNumberStart(), offset + LFH_OFFSET_FOR_FILENAME_LENGTH);
            // the offset should be updated to the global offset
            offset = archive.position() - LFH_OFFSET_FOR_FILENAME_LENGTH;
        } else {
            archive.position(offset + LFH_OFFSET_FOR_FILENAME_LENGTH);
        }
        wordBbuf.rewind();
        IOUtils.readFully(archive, wordBbuf);
        wordBbuf.flip();
        wordBbuf.get(shortBuf);
        final int fileNameLen = ZipShort.getValue(shortBuf);
        wordBbuf.get(shortBuf);
        final int extraFieldLen = ZipShort.getValue(shortBuf);
        ze.setDataOffset(offset + LFH_OFFSET_FOR_FILENAME_LENGTH
                         + ZipConstants.SHORT + ZipConstants.SHORT + fileNameLen + extraFieldLen);
        if (ze.getDataOffset() + ze.getCompressedSize() > centralDirectoryStartOffset) {
            throw new IOException("data for " + ze.getName() + " overlaps with central directory.");
        }
        return new int[] { fileNameLen, extraFieldLen };
    }

    /**
     * If the entry holds a Zip64 extended information extra field,
     * read sizes from there if the entry's sizes are set to
     * 0xFFFFFFFFF, do the same for the offset of the local file
     * header.
     *
     * <p>Ensures the Zip64 extra either knows both compressed and
     * uncompressed size or neither of both as the internal logic in
     * ExtraFieldUtils forces the field to create local header data
     * even if they are never used - and here a field with only one
     * size would be invalid.</p>
     */
    private void setSizesAndOffsetFromZip64Extra(final ZipArchiveEntry ze)
        throws IOException {
        final ZipExtraField extra =
            ze.getExtraField(Zip64ExtendedInformationExtraField.HEADER_ID);
        if (extra != null && !(extra instanceof Zip64ExtendedInformationExtraField)) {
            throw new ZipException("archive contains unparseable zip64 extra field");
        }
        final Zip64ExtendedInformationExtraField z64 =
            (Zip64ExtendedInformationExtraField) extra;
        if (z64 != null) {
            final boolean hasUncompressedSize = ze.getSize() == ZipConstants.ZIP64_MAGIC;
            final boolean hasCompressedSize = ze.getCompressedSize() == ZipConstants.ZIP64_MAGIC;
            final boolean hasRelativeHeaderOffset =
                ze.getLocalHeaderOffset() == ZipConstants.ZIP64_MAGIC;
            final boolean hasDiskStart = ze.getDiskNumberStart() == ZipConstants.ZIP64_MAGIC_SHORT;
            z64.reparseCentralDirectoryData(hasUncompressedSize,
                                            hasCompressedSize,
                                            hasRelativeHeaderOffset,
                                            hasDiskStart);

            if (hasUncompressedSize) {
                final long size = z64.getSize().getLongValue();
                if (size < 0) {
                    throw new IOException("broken archive, entry with negative size");
                }
                ze.setSize(size);
            } else if (hasCompressedSize) {
                z64.setSize(new ZipEightByteInteger(ze.getSize()));
            }

            if (hasCompressedSize) {
                final long size = z64.getCompressedSize().getLongValue();
                if (size < 0) {
                    throw new IOException("broken archive, entry with negative compressed size");
                }
                ze.setCompressedSize(size);
            } else if (hasUncompressedSize) {
                z64.setCompressedSize(new ZipEightByteInteger(ze.getCompressedSize()));
            }

            if (hasRelativeHeaderOffset) {
                ze.setLocalHeaderOffset(z64.getRelativeHeaderOffset().getLongValue());
            }

            if (hasDiskStart) {
                ze.setDiskNumberStart(z64.getDiskStartNumber().getValue());
            }
        }
    }

    /**
     * Skips the given number of bytes or throws an EOFException if
     * skipping failed.
     */
    private void skipBytes(final int count) throws IOException {
        final long currentPosition = archive.position();
        final long newPosition = currentPosition + count;
        if (newPosition > archive.size()) {
            throw new EOFException();
        }
        archive.position(newPosition);
    }

    /**
     * Checks whether the archive starts with a LFH.  If it doesn't,
     * it may be an empty archive.
     */
    private boolean startsWithLocalFileHeader() throws IOException {
        archive.position(firstLocalFileHeaderOffset);
        wordBbuf.rewind();
        IOUtils.readFully(archive, wordBbuf);
        return Arrays.equals(wordBuf, ZipArchiveOutputStream.LFH_SIG);
    }

    /**
     * Searches the archive backwards from minDistance to maxDistance
     * for the given signature, positions the RandomaccessFile right
     * at the signature if it has been found.
     */
    private boolean tryToLocateSignature(final long minDistanceFromEnd,
                                         final long maxDistanceFromEnd,
                                         final byte[] sig) throws IOException {
        boolean found = false;
        long off = archive.size() - minDistanceFromEnd;
        final long stopSearching =
            Math.max(0L, archive.size() - maxDistanceFromEnd);
        if (off >= 0) {
            for (; off >= stopSearching; off--) {
                archive.position(off);
                try {
                    wordBbuf.rewind();
                    IOUtils.readFully(archive, wordBbuf);
                    wordBbuf.flip();
                } catch (final EOFException ex) { // NOSONAR
                    break;
                }
                int curr = wordBbuf.get();
                if (curr == sig[POS_0]) {
                    curr = wordBbuf.get();
                    if (curr == sig[POS_1]) {
                        curr = wordBbuf.get();
                        if (curr == sig[POS_2]) {
                            curr = wordBbuf.get();
                            if (curr == sig[POS_3]) {
                                found = true;
                                break;
                            }
                        }
                    }
                }
            }
        }
        if (found) {
            archive.position(off);
        }
        return found;
    }
}
