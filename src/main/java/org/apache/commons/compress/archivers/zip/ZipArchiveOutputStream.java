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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.ZipException;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveOutputStream;

import static org.apache.commons.compress.archivers.zip.ZipConstants.BYTE_MASK;
import static org.apache.commons.compress.archivers.zip.ZipConstants.DEFLATE_MIN_VERSION;
import static org.apache.commons.compress.archivers.zip.ZipConstants.DWORD;
import static org.apache.commons.compress.archivers.zip.ZipConstants.SHORT;
import static org.apache.commons.compress.archivers.zip.ZipConstants.WORD;
import static org.apache.commons.compress.archivers.zip.ZipConstants.ZIP64_MAGIC;
import static org.apache.commons.compress.archivers.zip.ZipConstants.ZIP64_MAGIC_SHORT;
import static org.apache.commons.compress.archivers.zip.ZipConstants.ZIP64_MIN_VERSION;

/**
 * Reimplementation of {@link java.util.zip.ZipOutputStream
 * java.util.zip.ZipOutputStream} that does handle the extended
 * functionality of this package, especially internal/external file
 * attributes and extra fields with different layouts for local file
 * data and central directory entries.
 *
 * <p>This class will try to use {@link java.io.RandomAccessFile
 * RandomAccessFile} when you know that the output is going to go to a
 * file.</p>
 *
 * <p>If RandomAccessFile cannot be used, this implementation will use
 * a Data Descriptor to store size and CRC information for {@link
 * #DEFLATED DEFLATED} entries, this means, you don't need to
 * calculate them yourself.  Unfortunately this is not possible for
 * the {@link #STORED STORED} method, here setting the CRC and
 * uncompressed size information is required before {@link
 * #putArchiveEntry(ArchiveEntry)} can be called.</p>
 * @NotThreadSafe
 */
public class ZipArchiveOutputStream extends ArchiveOutputStream {

    static final int BUFFER_SIZE = 512;

    /** indicates if this archive is finished. protected for use in Jar implementation */
    protected boolean finished = false;

    /* 
     * Apparently Deflater.setInput gets slowed down a lot on Sun JVMs
     * when it gets handed a really big buffer.  See
     * https://issues.apache.org/bugzilla/show_bug.cgi?id=45396
     *
     * Using a buffer size of 8 kB proved to be a good compromise
     */
    private static final int DEFLATER_BLOCK_SIZE = 8192;

    /**
     * Compression method for deflated entries.
     */
    public static final int DEFLATED = java.util.zip.ZipEntry.DEFLATED;

    /**
     * Default compression level for deflated entries.
     */
    public static final int DEFAULT_COMPRESSION = Deflater.DEFAULT_COMPRESSION;

    /**
     * Compression method for stored entries.
     */
    public static final int STORED = java.util.zip.ZipEntry.STORED;

    /**
     * default encoding for file names and comment.
     */
    static final String DEFAULT_ENCODING = ZipEncodingHelper.UTF8;

    /**
     * General purpose flag, which indicates that filenames are
     * written in utf-8.
     * @deprecated use {@link GeneralPurposeBit#UFT8_NAMES_FLAG} instead
     */
    public static final int EFS_FLAG = GeneralPurposeBit.UFT8_NAMES_FLAG;

    /**
     * Current entry.
     */
    private ZipArchiveEntry entry;

    /**
     * The file comment.
     */
    private String comment = "";

    /**
     * Compression level for next entry.
     */
    private int level = DEFAULT_COMPRESSION;

    /**
     * Has the compression level changed when compared to the last
     * entry?
     */
    private boolean hasCompressionLevelChanged = false;

    /**
     * Default compression method for next entry.
     */
    private int method = java.util.zip.ZipEntry.DEFLATED;

    /**
     * List of ZipArchiveEntries written so far.
     */
    private final List<ZipArchiveEntry> entries =
        new LinkedList<ZipArchiveEntry>();

    /**
     * CRC instance to avoid parsing DEFLATED data twice.
     */
    private final CRC32 crc = new CRC32();

    /**
     * Count the bytes written to out.
     */
    private long written = 0;

    /**
     * Data for local header data
     */
    private long dataStart = 0;

    /**
     * Offset for CRC entry in the local file header data for the
     * current entry starts here.
     */
    private long localDataStart = 0;

    /**
     * Start of central directory.
     */
    private long cdOffset = 0;

    /**
     * Length of central directory.
     */
    private long cdLength = 0;

    /**
     * Number of bytes read for the current entry (can't rely on
     * Deflater#getBytesRead) when using DEFLATED.
     *
     * @since Apache Commons Compress 1.3
     */
    private long bytesRead = 0;

    /**
     * Helper, a 0 as ZipShort.
     */
    private static final byte[] ZERO = {0, 0};

    /**
     * Helper, a 0 as ZipLong.
     */
    private static final byte[] LZERO = {0, 0, 0, 0};

    /**
     * Holds the offsets of the LFH starts for each entry.
     */
    private final Map<ZipArchiveEntry, Long> offsets =
        new HashMap<ZipArchiveEntry, Long>();

    /**
     * The encoding to use for filenames and the file comment.
     *
     * <p>For a list of possible values see <a
     * href="http://java.sun.com/j2se/1.5.0/docs/guide/intl/encoding.doc.html">http://java.sun.com/j2se/1.5.0/docs/guide/intl/encoding.doc.html</a>.
     * Defaults to UTF-8.</p>
     */
    private String encoding = DEFAULT_ENCODING;

    /**
     * The zip encoding to use for filenames and the file comment.
     *
     * This field is of internal use and will be set in {@link
     * #setEncoding(String)}.
     */
    private ZipEncoding zipEncoding =
        ZipEncodingHelper.getZipEncoding(DEFAULT_ENCODING);

    /**
     * This Deflater object is used for output.
     *
     */
    protected final Deflater def = new Deflater(level, true);

    /**
     * This buffer servers as a Deflater.
     *
     */
    private final byte[] buf = new byte[BUFFER_SIZE];

    /**
     * Optional random access output.
     */
    private final RandomAccessFile raf;

    private final OutputStream out;

    /**
     * whether to use the general purpose bit flag when writing UTF-8
     * filenames or not.
     */
    private boolean useUTF8Flag = true; 

    /**
     * Whether to encode non-encodable file names as UTF-8.
     */
    private boolean fallbackToUTF8 = false;

    /**
     * whether to create UnicodePathExtraField-s for each entry.
     */
    private UnicodeExtraFieldPolicy createUnicodeExtraFields = UnicodeExtraFieldPolicy.NEVER;

    /**
     * Whether anything inside this archive has used a ZIP64 feature.
     *
     * @since Apache Commons Compress 1.3
     */
    private boolean hasUsedZip64 = false;

    /**
     * Creates a new ZIP OutputStream filtering the underlying stream.
     * @param out the outputstream to zip
     */
    public ZipArchiveOutputStream(OutputStream out) {
        this.out = out;
        this.raf = null;
    }

    /**
     * Creates a new ZIP OutputStream writing to a File.  Will use
     * random access if possible.
     * @param file the file to zip to
     * @throws IOException on error
     */
    public ZipArchiveOutputStream(File file) throws IOException {
        OutputStream o = null;
        RandomAccessFile _raf = null;
        try {
            _raf = new RandomAccessFile(file, "rw");
            _raf.setLength(0);
        } catch (IOException e) {
            if (_raf != null) {
                try {
                    _raf.close();
                } catch (IOException inner) {
                    // ignore
                }
                _raf = null;
            }
            o = new FileOutputStream(file);
        }
        out = o;
        raf = _raf;
    }

    /**
     * This method indicates whether this archive is writing to a
     * seekable stream (i.e., to a random access file).
     *
     * <p>For seekable streams, you don't need to calculate the CRC or
     * uncompressed size for {@link #STORED} entries before
     * invoking {@link #putArchiveEntry(ArchiveEntry)}.
     * @return true if seekable
     */
    public boolean isSeekable() {
        return raf != null;
    }

    /**
     * The encoding to use for filenames and the file comment.
     *
     * <p>For a list of possible values see <a
     * href="http://java.sun.com/j2se/1.5.0/docs/guide/intl/encoding.doc.html">http://java.sun.com/j2se/1.5.0/docs/guide/intl/encoding.doc.html</a>.
     * Defaults to UTF-8.</p>
     * @param encoding the encoding to use for file names, use null
     * for the platform's default encoding
     */
    public void setEncoding(final String encoding) {
        this.encoding = encoding;
        this.zipEncoding = ZipEncodingHelper.getZipEncoding(encoding);
        useUTF8Flag &= ZipEncodingHelper.isUTF8(encoding);
    }

    /**
     * The encoding to use for filenames and the file comment.
     *
     * @return null if using the platform's default character encoding.
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * Whether to set the language encoding flag if the file name
     * encoding is UTF-8.
     *
     * <p>Defaults to true.</p>
     */
    public void setUseLanguageEncodingFlag(boolean b) {
        useUTF8Flag = b && ZipEncodingHelper.isUTF8(encoding);
    }

    /**
     * Whether to create Unicode Extra Fields.
     *
     * <p>Defaults to NEVER.</p>
     */
    public void setCreateUnicodeExtraFields(UnicodeExtraFieldPolicy b) {
        createUnicodeExtraFields = b;
    }

    /**
     * Whether to fall back to UTF and the language encoding flag if
     * the file name cannot be encoded using the specified encoding.
     *
     * <p>Defaults to false.</p>
     */
    public void setFallbackToUTF8(boolean b) {
        fallbackToUTF8 = b;
    }

    /** {@inheritDoc} */
    @Override
    public void finish() throws IOException {
        if (finished) {
            throw new IOException("This archive has already been finished");
        }

        if (entry != null) {
            throw new IOException("This archives contains unclosed entries.");
        }

        cdOffset = written;
        for (ZipArchiveEntry ze : entries) {
            writeCentralFileHeader(ze);
        }
        cdLength = written - cdOffset;
        writeZip64CentralDirectory();
        writeCentralDirectoryEnd();
        offsets.clear();
        entries.clear();
        finished = true;
    }

    /**
     * Writes all necessary data for this entry.
     * @throws IOException on error
     */
    @Override
    public void closeArchiveEntry() throws IOException {
        if (finished) {
            throw new IOException("Stream has already been finished");
        }

        if (entry == null) {
            throw new IOException("No current entry to close");
        }

        long realCrc = crc.getValue();
        crc.reset();

        if (entry.getMethod() == DEFLATED) {
            def.finish();
            while (!def.finished()) {
                deflate();
            }

            /* It turns out def.getBytesRead() returns wrong values if
             * the size exceeds 4 GB - no idea whether one can trust
             * def.getBytesWritten()
            entry.setSize(def.getBytesRead());
            */
            entry.setSize(bytesRead);
            entry.setCompressedSize(def.getBytesWritten());
            entry.setCrc(realCrc);

            def.reset();

            written += entry.getCompressedSize();
        } else if (raf == null) {
            if (entry.getCrc() != realCrc) {
                throw new ZipException("bad CRC checksum for entry "
                                       + entry.getName() + ": "
                                       + Long.toHexString(entry.getCrc())
                                       + " instead of "
                                       + Long.toHexString(realCrc));
            }

            if (entry.getSize() != written - dataStart) {
                throw new ZipException("bad size for entry "
                                       + entry.getName() + ": "
                                       + entry.getSize()
                                       + " instead of "
                                       + (written - dataStart));
            }
        } else { /* method is STORED and we used RandomAccessFile */
            long size = written - dataStart;

            entry.setSize(size);
            entry.setCompressedSize(size);
            entry.setCrc(realCrc);
        }

        // If random access output, write the local file header containing
        // the correct CRC and compressed/uncompressed sizes
        if (raf != null) {
            long save = raf.getFilePointer();

            raf.seek(localDataStart);
            writeOut(ZipLong.getBytes(entry.getCrc()));
            if (!hasZip64Extra(entry)
                || (entry.getSize() < ZIP64_MAGIC
                    && entry.getCompressedSize() < ZIP64_MAGIC)) {
                writeOut(ZipLong.getBytes(entry.getCompressedSize()));
                writeOut(ZipLong.getBytes(entry.getSize()));
            } else {
                writeOut(ZipLong.ZIP64_MAGIC.getBytes());
                writeOut(ZipLong.ZIP64_MAGIC.getBytes());
            }

            if (hasZip64Extra(entry)) {
                // seek to ZIP64 extra, skip header and size information
                raf.seek(localDataStart + 3 * WORD + 2 * SHORT
                         + getName(entry).limit() + 2 * SHORT);
                // inside the ZIP64 extra uncompressed size comes
                // first, unlike the LFH, CD or data descriptor
                writeOut(ZipEightByteInteger.getBytes(entry.getSize()));
                writeOut(ZipEightByteInteger.getBytes(entry.getCompressedSize()));
            }
            raf.seek(save);
        }

        writeDataDescriptor(entry);
        entry = null;
        bytesRead = 0;
    }

    /**
     * {@inheritDoc} 
     * @throws ClassCastException if entry is not an instance of ZipArchiveEntry
     */
    @Override
    public void putArchiveEntry(ArchiveEntry archiveEntry) throws IOException {
        if (finished) {
            throw new IOException("Stream has already been finished");
        }

        if (entry != null) {
            closeArchiveEntry();
        }

        entry = ((ZipArchiveEntry) archiveEntry);
        entries.add(entry);

        if (entry.getMethod() == -1) { // not specified
            entry.setMethod(method);
        }

        if (entry.getTime() == -1) { // not specified
            entry.setTime(System.currentTimeMillis());
        }

        // Size/CRC not required if RandomAccessFile is used
        if (entry.getMethod() == STORED && raf == null) {
            if (entry.getSize() == ArchiveEntry.SIZE_UNKNOWN) {
                throw new ZipException("uncompressed size is required for"
                                       + " STORED method when not writing to a"
                                       + " file");
            }
            if (entry.getCrc() == -1) {
                throw new ZipException("crc checksum is required for STORED"
                                       + " method when not writing to a file");
            }
            entry.setCompressedSize(entry.getSize());
        }

        // add a ZIP64 extended information extra field if we already
        // know it is going to be needed or the size is unknown and we
        // can ensure it won't hurt other implementations if we add it
        // (i.e. we can erase its usage)
        if (entry.getSize() >= ZIP64_MAGIC
            || entry.getCompressedSize() >= ZIP64_MAGIC
            || (entry.getSize() == ArchiveEntry.SIZE_UNKNOWN && raf != null)) {

            Zip64ExtendedInformationExtraField z64 = getZip64Extra(entry);
            if (entry.getMethod() == STORED
                && entry.getSize() != ArchiveEntry.SIZE_UNKNOWN) {
                ZipEightByteInteger size =
                    new ZipEightByteInteger(entry.getSize());
                z64.setSize(size);
                z64.setCompressedSize(size);
            } else {
                // just a placeholder, real data will be in data
                // descriptor or inserted later via RandomAccessFile
                z64.setSize(ZipEightByteInteger.ZERO);
                z64.setCompressedSize(ZipEightByteInteger.ZERO);
            }
            entry.setExtra();
        }

        if (entry.getMethod() == DEFLATED && hasCompressionLevelChanged) {
            def.setLevel(level);
            hasCompressionLevelChanged = false;
        }
        writeLocalFileHeader(entry);
    }

    /**
     * Set the file comment.
     * @param comment the comment
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Sets the compression level for subsequent entries.
     *
     * <p>Default is Deflater.DEFAULT_COMPRESSION.</p>
     * @param level the compression level.
     * @throws IllegalArgumentException if an invalid compression
     * level is specified.
     */
    public void setLevel(int level) {
        if (level < Deflater.DEFAULT_COMPRESSION
            || level > Deflater.BEST_COMPRESSION) {
            throw new IllegalArgumentException("Invalid compression level: "
                                               + level);
        }
        hasCompressionLevelChanged = (this.level != level);
        this.level = level;
    }

    /**
     * Sets the default compression method for subsequent entries.
     *
     * <p>Default is DEFLATED.</p>
     * @param method an <code>int</code> from java.util.zip.ZipEntry
     */
    public void setMethod(int method) {
        this.method = method;
    }

    /**
     * Whether this stream is able to write the given entry.
     *
     * <p>May return false if it is set up to use encryption or a
     * compression method that hasn't been implemented yet.</p>
     * @since Apache Commons Compress 1.1
     */
    @Override
    public boolean canWriteEntryData(ArchiveEntry ae) {
        if (ae instanceof ZipArchiveEntry) {
            return ZipUtil.canHandleEntryData((ZipArchiveEntry) ae);
        }
        return false;
    }

    /**
     * Writes bytes to ZIP entry.
     * @param b the byte array to write
     * @param offset the start position to write from
     * @param length the number of bytes to write
     * @throws IOException on error
     */
    @Override
    public void write(byte[] b, int offset, int length) throws IOException {
        ZipUtil.checkRequestedFeatures(entry);
        if (entry.getMethod() == DEFLATED) {
            if (length > 0 && !def.finished()) {
                bytesRead += length;
                if (length <= DEFLATER_BLOCK_SIZE) {
                    def.setInput(b, offset, length);
                    deflateUntilInputIsNeeded();
                } else {
                    final int fullblocks = length / DEFLATER_BLOCK_SIZE;
                    for (int i = 0; i < fullblocks; i++) {
                        def.setInput(b, offset + i * DEFLATER_BLOCK_SIZE,
                                     DEFLATER_BLOCK_SIZE);
                        deflateUntilInputIsNeeded();
                    }
                    final int done = fullblocks * DEFLATER_BLOCK_SIZE;
                    if (done < length) {
                        def.setInput(b, offset + done, length - done);
                        deflateUntilInputIsNeeded();
                    }
                }
            }
        } else {
            writeOut(b, offset, length);
            written += length;
        }
        crc.update(b, offset, length);
        count(length);
    }

    /**
     * Closes this output stream and releases any system resources
     * associated with the stream.
     *
     * @exception  IOException  if an I/O error occurs.
     */
    @Override
    public void close() throws IOException {
        if (!finished) {
            finish();
        }

        if (raf != null) {
            raf.close();
        }
        if (out != null) {
            out.close();
        }
    }

    /**
     * Flushes this output stream and forces any buffered output bytes
     * to be written out to the stream.
     *
     * @exception  IOException  if an I/O error occurs.
     */
    @Override
    public void flush() throws IOException {
        if (out != null) {
            out.flush();
        }
    }

    /*
     * Various ZIP constants
     */
    /**
     * local file header signature
     */
    static final byte[] LFH_SIG = ZipLong.LFH_SIG.getBytes();
    /**
     * data descriptor signature
     */
    static final byte[] DD_SIG = ZipLong.DD_SIG.getBytes();
    /**
     * central file header signature
     */
    static final byte[] CFH_SIG = ZipLong.CFH_SIG.getBytes();
    /**
     * end of central dir signature
     */
    static final byte[] EOCD_SIG = ZipLong.getBytes(0X06054B50L);
    /**
     * ZIP64 end of central dir signature
     */
    static final byte[] ZIP64_EOCD_SIG = ZipLong.getBytes(0X06064B50L);
    /**
     * ZIP64 end of central dir locator signature
     */
    static final byte[] ZIP64_EOCD_LOC_SIG = ZipLong.getBytes(0X07064B50L);

    /**
     * Writes next block of compressed data to the output stream.
     * @throws IOException on error
     */
    protected final void deflate() throws IOException {
        int len = def.deflate(buf, 0, buf.length);
        if (len > 0) {
            writeOut(buf, 0, len);
        }
    }

    /**
     * Writes the local file header entry
     * @param ze the entry to write
     * @throws IOException on error
     */
    protected void writeLocalFileHeader(ZipArchiveEntry ze) throws IOException {

        boolean encodable = zipEncoding.canEncode(ze.getName());
        ByteBuffer name = getName(ze);

        if (createUnicodeExtraFields != UnicodeExtraFieldPolicy.NEVER) {

            if (createUnicodeExtraFields == UnicodeExtraFieldPolicy.ALWAYS
                || !encodable) {
                ze.addExtraField(new UnicodePathExtraField(ze.getName(),
                                                           name.array(),
                                                           name.arrayOffset(),
                                                           name.limit()));
            }

            String comm = ze.getComment();
            if (comm != null && !"".equals(comm)) {

                boolean commentEncodable = zipEncoding.canEncode(comm);

                if (createUnicodeExtraFields == UnicodeExtraFieldPolicy.ALWAYS
                    || !commentEncodable) {
                    ByteBuffer commentB = getEntryEncoding(ze).encode(comm);
                    ze.addExtraField(new UnicodeCommentExtraField(comm,
                                                                  commentB.array(),
                                                                  commentB.arrayOffset(),
                                                                  commentB.limit())
                                     );
                }
            }
        }

        offsets.put(ze, Long.valueOf(written));

        writeOut(LFH_SIG);
        written += WORD;

        //store method in local variable to prevent multiple method calls
        final int zipMethod = ze.getMethod();

        writeVersionNeededToExtractAndGeneralPurposeBits(zipMethod,
                                                         !encodable
                                                         && fallbackToUTF8,
                                                         hasZip64Extra(ze));
        written += WORD;

        // compression method
        writeOut(ZipShort.getBytes(zipMethod));
        written += SHORT;

        // last mod. time and date
        writeOut(ZipUtil.toDosTime(ze.getTime()));
        written += WORD;

        // CRC
        // compressed length
        // uncompressed length
        localDataStart = written;
        if (zipMethod == DEFLATED || raf != null) {
            writeOut(LZERO);
            writeOut(LZERO);
            writeOut(LZERO);
        } else {
            writeOut(ZipLong.getBytes(ze.getCrc()));
            byte[] size = ZipLong.getBytes(Math.min(ze.getSize(), ZIP64_MAGIC));
            writeOut(size);
            writeOut(size);
        }
        // CheckStyle:MagicNumber OFF
        written += 12;
        // CheckStyle:MagicNumber ON

        // file name length
        writeOut(ZipShort.getBytes(name.limit()));
        written += SHORT;

        // extra field length
        byte[] extra = ze.getLocalFileDataExtra();
        writeOut(ZipShort.getBytes(extra.length));
        written += SHORT;

        // file name
        writeOut(name.array(), name.arrayOffset(), name.limit());
        written += name.limit();

        // extra field
        writeOut(extra);
        written += extra.length;

        dataStart = written;
    }

    /**
     * Writes the data descriptor entry.
     * @param ze the entry to write
     * @throws IOException on error
     */
    protected void writeDataDescriptor(ZipArchiveEntry ze) throws IOException {
        if (ze.getMethod() != DEFLATED || raf != null) {
            return;
        }
        writeOut(DD_SIG);
        writeOut(ZipLong.getBytes(ze.getCrc()));
        int sizeFieldSize = WORD;
        if (!hasZip64Extra(ze)) {
            writeOut(ZipLong.getBytes(ze.getCompressedSize()));
            writeOut(ZipLong.getBytes(ze.getSize()));
        } else {
            sizeFieldSize = DWORD;
            writeOut(ZipEightByteInteger.getBytes(ze.getCompressedSize()));
            writeOut(ZipEightByteInteger.getBytes(ze.getSize()));
        }
        written += 2 * WORD + 2 * sizeFieldSize;
    }

    /**
     * Writes the central file header entry.
     * @param ze the entry to write
     * @throws IOException on error
     */
    protected void writeCentralFileHeader(ZipArchiveEntry ze) throws IOException {
        writeOut(CFH_SIG);
        written += WORD;

        final long lfhOffset = offsets.get(ze).longValue();
        final boolean needsZip64Extra = ze.getCompressedSize() >= ZIP64_MAGIC
            || ze.getSize() >= ZIP64_MAGIC
            || lfhOffset >= ZIP64_MAGIC;

        if (needsZip64Extra) {
            Zip64ExtendedInformationExtraField z64 = getZip64Extra(ze);
            if (ze.getCompressedSize() >= ZIP64_MAGIC
                || ze.getSize() >= ZIP64_MAGIC) {
                z64.setCompressedSize(new ZipEightByteInteger(ze.getCompressedSize()));
                z64.setSize(new ZipEightByteInteger(ze.getSize()));
            } else {
                // reset value that may have been set for LFH
                z64.setCompressedSize(null);
                z64.setSize(null);
            }
            if (lfhOffset >= ZIP64_MAGIC) {
                z64.setRelativeHeaderOffset(new ZipEightByteInteger(lfhOffset));
            }
            ze.setExtra();
        }

        // version made by
        // CheckStyle:MagicNumber OFF
        writeOut(ZipShort.getBytes((ze.getPlatform() << 8) | 
                                   (!hasUsedZip64 ? DEFLATE_MIN_VERSION
                                                  : ZIP64_MIN_VERSION)));
        written += SHORT;

        final int zipMethod = ze.getMethod();
        final boolean encodable = zipEncoding.canEncode(ze.getName());
        writeVersionNeededToExtractAndGeneralPurposeBits(zipMethod,
                                                         !encodable
                                                         && fallbackToUTF8,
                                                         needsZip64Extra);
        written += WORD;

        // compression method
        writeOut(ZipShort.getBytes(zipMethod));
        written += SHORT;

        // last mod. time and date
        writeOut(ZipUtil.toDosTime(ze.getTime()));
        written += WORD;

        // CRC
        // compressed length
        // uncompressed length
        writeOut(ZipLong.getBytes(ze.getCrc()));
        if (ze.getCompressedSize() >= ZIP64_MAGIC
            || ze.getSize() >= ZIP64_MAGIC) {
            writeOut(ZipLong.ZIP64_MAGIC.getBytes());
            writeOut(ZipLong.ZIP64_MAGIC.getBytes());
        } else {
            writeOut(ZipLong.getBytes(ze.getCompressedSize()));
            writeOut(ZipLong.getBytes(ze.getSize()));
        }
        // CheckStyle:MagicNumber OFF
        written += 12;
        // CheckStyle:MagicNumber ON

        ByteBuffer name = getName(ze);

        writeOut(ZipShort.getBytes(name.limit()));
        written += SHORT;

        // extra field length
        byte[] extra = ze.getCentralDirectoryExtra();
        writeOut(ZipShort.getBytes(extra.length));
        written += SHORT;

        // file comment length
        String comm = ze.getComment();
        if (comm == null) {
            comm = "";
        }

        ByteBuffer commentB = getEntryEncoding(ze).encode(comm);

        writeOut(ZipShort.getBytes(commentB.limit()));
        written += SHORT;

        // disk number start
        writeOut(ZERO);
        written += SHORT;

        // internal file attributes
        writeOut(ZipShort.getBytes(ze.getInternalAttributes()));
        written += SHORT;

        // external file attributes
        writeOut(ZipLong.getBytes(ze.getExternalAttributes()));
        written += WORD;

        // relative offset of LFH
        writeOut(ZipLong.getBytes(Math.min(lfhOffset, ZIP64_MAGIC)));
        written += WORD;

        // file name
        writeOut(name.array(), name.arrayOffset(), name.limit());
        written += name.limit();

        // extra field
        writeOut(extra);
        written += extra.length;

        // file comment
        writeOut(commentB.array(), commentB.arrayOffset(), commentB.limit());
        written += commentB.limit();
    }

    /**
     * Writes the &quot;End of central dir record&quot;.
     * @throws IOException on error
     */
    protected void writeCentralDirectoryEnd() throws IOException {
        writeOut(EOCD_SIG);

        // disk numbers
        writeOut(ZERO);
        writeOut(ZERO);

        // number of entries
        byte[] num = ZipShort.getBytes(Math.min(entries.size(),
                                                ZIP64_MAGIC_SHORT));
        writeOut(num);
        writeOut(num);

        // length and location of CD
        writeOut(ZipLong.getBytes(Math.min(cdLength, ZIP64_MAGIC)));
        writeOut(ZipLong.getBytes(Math.min(cdOffset, ZIP64_MAGIC)));

        // ZIP file comment
        ByteBuffer data = this.zipEncoding.encode(comment);
        writeOut(ZipShort.getBytes(data.limit()));
        writeOut(data.array(), data.arrayOffset(), data.limit());
    }

    private static final byte[] ONE = ZipLong.getBytes(1L);

    /**
     * Writes the &quot;ZIP64 End of central dir record&quot; and
     * &quot;ZIP64 End of central dir locator&quot;.
     * @throws IOException on error
     * @since Apache Commons Compress 1.3
     */
    protected void writeZip64CentralDirectory() throws IOException {
        if (!hasUsedZip64) {
            if (cdOffset >= ZIP64_MAGIC || cdLength >= ZIP64_MAGIC
                || entries.size() >= ZIP64_MAGIC_SHORT) {
                // actually "will use"
                hasUsedZip64 = true;
            }
        }

        if (!hasUsedZip64) {
            return;
        }

        long offset = written;

        writeOut(ZIP64_EOCD_SIG);
        // size, we don't have any variable length as we don't support
        // the extensible data sector, yet
        writeOut(ZipEightByteInteger
                 .getBytes(SHORT   /* version made by */
                           + SHORT /* version needed to extract */
                           + WORD  /* disk number */
                           + WORD  /* disk with central directory */
                           + DWORD /* number of entries in CD on this disk */
                           + DWORD /* total number of entries */
                           + DWORD /* size of CD */
                           + DWORD /* offset of CD */
                           ));

        // version made by and version needed to extract
        writeOut(ZipShort.getBytes(ZIP64_MIN_VERSION));
        writeOut(ZipShort.getBytes(ZIP64_MIN_VERSION));

        // disk numbers - four bytes this time
        writeOut(LZERO);
        writeOut(LZERO);

        // number of entries
        byte[] num = ZipEightByteInteger.getBytes(entries.size());
        writeOut(num);
        writeOut(num);

        // length and location of CD
        writeOut(ZipEightByteInteger.getBytes(cdLength));
        writeOut(ZipEightByteInteger.getBytes(cdOffset));

        // no "zip64 extensible data sector" for now

        // and now the "ZIP64 end of central directory locator"
        writeOut(ZIP64_EOCD_LOC_SIG);

        // disk number holding the ZIP64 EOCD record
        writeOut(LZERO);
        // relative offset of ZIP64 EOCD record
        writeOut(ZipEightByteInteger.getBytes(offset));
        // total number of disks
        writeOut(ONE);
    }

    /**
     * Write bytes to output or random access file.
     * @param data the byte array to write
     * @throws IOException on error
     */
    protected final void writeOut(byte[] data) throws IOException {
        writeOut(data, 0, data.length);
    }

    /**
     * Write bytes to output or random access file.
     * @param data the byte array to write
     * @param offset the start position to write from
     * @param length the number of bytes to write
     * @throws IOException on error
     */
    protected final void writeOut(byte[] data, int offset, int length)
        throws IOException {
        if (raf != null) {
            raf.write(data, offset, length);
        } else {
            out.write(data, offset, length);
        }
    }

    private void deflateUntilInputIsNeeded() throws IOException {
        while (!def.needsInput()) {
            deflate();
        }
    }

    private void writeVersionNeededToExtractAndGeneralPurposeBits(final int
                                                                  zipMethod,
                                                                  final boolean
                                                                  utfFallback,
                                                                  final boolean
                                                                  zip64)
        throws IOException {

        // CheckStyle:MagicNumber OFF
        int versionNeededToExtract = 10;
        GeneralPurposeBit b = new GeneralPurposeBit();
        b.useUTF8ForNames(useUTF8Flag || utfFallback);
        if (zipMethod == DEFLATED && raf == null) {
            // requires version 2 as we are going to store length info
            // in the data descriptor
            versionNeededToExtract = DEFLATE_MIN_VERSION;
            b.useDataDescriptor(true);
        }
        if (zip64) {
            versionNeededToExtract = ZIP64_MIN_VERSION;
        }
        // CheckStyle:MagicNumber ON

        // version needed to extract
        writeOut(ZipShort.getBytes(versionNeededToExtract));
        // general purpose bit flag
        writeOut(b.encode());
    }

    /**
     * enum that represents the possible policies for creating Unicode
     * extra fields.
     */
    public static final class UnicodeExtraFieldPolicy {
        /**
         * Always create Unicode extra fields.
         */
        public static final UnicodeExtraFieldPolicy ALWAYS = new UnicodeExtraFieldPolicy("always");
        /**
         * Never create Unicode extra fields.
         */
        public static final UnicodeExtraFieldPolicy NEVER = new UnicodeExtraFieldPolicy("never");
        /**
         * Create Unicode extra fields for filenames that cannot be
         * encoded using the specified encoding.
         */
        public static final UnicodeExtraFieldPolicy NOT_ENCODEABLE =
            new UnicodeExtraFieldPolicy("not encodeable");

        private final String name;
        private UnicodeExtraFieldPolicy(String n) {
            name = n;
        }
        public String toString() {
            return name;
        }
    }

    /**
     * Creates a new zip entry taking some information from the given
     * file and using the provided name.
     *
     * <p>The name will be adjusted to end with a forward slash "/" if
     * the file is a directory.  If the file is not a directory a
     * potential trailing forward slash will be stripped from the
     * entry name.</p>
     *
     * <p>Must not be used if the stream has already been closed.</p>
     */
    @Override
    public ArchiveEntry createArchiveEntry(File inputFile, String entryName)
            throws IOException {
        if (finished) {
            throw new IOException("Stream has already been finished");
        }
        return new ZipArchiveEntry(inputFile, entryName);
    }

    /**
     * Get the existing ZIP64 extended information extra field or
     * create a new one and add it to the entry.
     *
     * @since Apache Commons Compress 1.3
     */
    private Zip64ExtendedInformationExtraField
        getZip64Extra(ZipArchiveEntry ze) {
        hasUsedZip64 = true;
        Zip64ExtendedInformationExtraField z64 =
            (Zip64ExtendedInformationExtraField)
            ze.getExtraField(Zip64ExtendedInformationExtraField
                             .HEADER_ID);
        if (z64 == null) {
            /*
            System.err.println("Adding z64 for " + ze.getName()
                               + ", method: " + ze.getMethod()
                               + " (" + (ze.getMethod() == STORED) + ")"
                               + ", raf: " + (raf != null));
            */
            z64 = new Zip64ExtendedInformationExtraField();
        }

        // even if the field is there already, make sure it is the first one
        ze.addAsFirstExtraField(z64);

        return z64;
    }

    /**
     * Is there a ZIP64 extended information extra field for the
     * entry?
     *
     * @since Apache Commons Compress 1.3
     */
    private boolean hasZip64Extra(ZipArchiveEntry ze) {
        return ze.getExtraField(Zip64ExtendedInformationExtraField
                                .HEADER_ID)
            != null;
    }

    private ZipEncoding getEntryEncoding(ZipArchiveEntry ze) {
        boolean encodable = zipEncoding.canEncode(ze.getName());
        return !encodable && fallbackToUTF8
            ? ZipEncodingHelper.UTF8_ZIP_ENCODING : zipEncoding;
    }

    private ByteBuffer getName(ZipArchiveEntry ze) throws IOException {
        return getEntryEncoding(ze).encode(ze.getName());
    }
}
