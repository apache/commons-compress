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


import org.apache.commons.compress.utils.BoundedInputStream;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.zip.Deflater;

/**
 * A zip output stream that is optimized for multi-threaded scatter/gather construction of zip files.
 * <p/>
 * The internal data format of the entries used by this class are entirely private to this class
 * and are not part of any public api whatsoever.
 * <p/>
 * It is possible to extend this class to support different kinds of backing storage, the default
 * implementation only supports file-based backing.
 * <p/>
 * Thread safety: This class supports multiple threads. But the "writeTo" method must be called
 * by the thread that originally created the ZipArchiveEntry.
 *
 * @since 1.10
 */
public abstract class ScatterZipOutputStream  {
    private final Queue<CompressedEntry> items = new ConcurrentLinkedQueue<CompressedEntry>();

    private static class CompressedEntry {
        final ZipArchiveEntry entry;
        final long crc;
        final long compressedSize;
        final int method;
        final long size;

        public CompressedEntry(ZipArchiveEntry entry, long crc, long compressedSize, int method, long size) {
            this.entry = entry;
            this.crc = crc;
            this.compressedSize = compressedSize;
            this.method = method;
            this.size = size;
        }

        public ZipArchiveEntry transferToArchiveEntry(){
            entry.setCompressedSize(compressedSize);
            entry.setSize(size);
            entry.setCrc(crc);
            entry.setMethod(method);
            return entry;
        }
    }

    /**
     * Add an archive entry to this scatter stream.
     *
     * @param zipArchiveEntry The entry to write
     * @param payload         The content to write for the entry
     * @param method          The compression method
     * @throws IOException    If writing fails
     */
    public void addArchiveEntry(ZipArchiveEntry zipArchiveEntry, InputStream payload, int method) throws IOException {
        StreamCompressor sc = getStreamCompressor();
        sc.deflate(payload, method);
        payload.close();
        items.add(new CompressedEntry(zipArchiveEntry, sc.getCrc32(), sc.getBytesWritten(), method, sc.getBytesRead()));
    }

    /**
     * Write the contents of this scatter stream to a target archive.
     *
     * @param target The archive to receive the contents of this #ScatterZipOutputStream
     * @throws IOException If writing fails
     */
    public void writeTo(ZipArchiveOutputStream target) throws IOException {
        closeBackingStorage();
        InputStream data = getInputStream();
        for (CompressedEntry compressedEntry : items) {
            final BoundedInputStream rawStream = new BoundedInputStream(data, compressedEntry.compressedSize);
            target.addRawArchiveEntry(compressedEntry.transferToArchiveEntry(), rawStream);
            rawStream.close();
        }
        data.close();
    }

    /**
     * Returns a stream compressor that can be used to compress the data.
     * <p/>
     * This method is expected to return the same instance every time.
     *
     * @return The stream compressor
     * @throws FileNotFoundException
     */
    protected abstract StreamCompressor getStreamCompressor() throws FileNotFoundException;

    /**
     * An input stream that contains the scattered payload
     *
     * @return An InputStream, should be closed by the caller of this method.
     * @throws IOException when something fails
     */
    protected abstract InputStream getInputStream() throws IOException;


    /**
     * Closes whatever storage is backing this scatter stream
     */
    protected abstract void closeBackingStorage() throws IOException;

    /**
     * Create a ScatterZipOutputStream with default compression level that is backed by a file
     *
     * @param file The file to offload compressed data into.
     * @return A  ScatterZipOutputStream that is ready for use.
     * @throws FileNotFoundException
     */
    public static ScatterZipOutputStream fileBased(File file) throws FileNotFoundException {
        return fileBased(file, Deflater.DEFAULT_COMPRESSION);
    }

    /**
     * Create a ScatterZipOutputStream that is backed by a file
     *
     * @param file             The file to offload compressed data into.
     * @param compressionLevel The compression level to use, @see #Deflater
     * @return A  ScatterZipOutputStream that is ready for use.
     * @throws FileNotFoundException
     */
    public static ScatterZipOutputStream fileBased(File file, int compressionLevel) throws FileNotFoundException {
        return new FileScatterOutputStream(file, compressionLevel);
    }

    private static class FileScatterOutputStream extends ScatterZipOutputStream {
        final File target;
        private StreamCompressor streamDeflater;
        final FileOutputStream os;

        FileScatterOutputStream(File target, int compressionLevel) throws FileNotFoundException {
            this.target = target;
            os = new FileOutputStream(target);
            streamDeflater = StreamCompressor.create(compressionLevel, os);
        }

        @Override
        protected StreamCompressor getStreamCompressor() throws FileNotFoundException {
            return streamDeflater;
        }

        @Override
        protected InputStream getInputStream() throws IOException {
            return new FileInputStream(target);
        }

        @SuppressWarnings("ResultOfMethodCallIgnored")
        public void closeBackingStorage() throws IOException {
            os.close();
        }
    }
}
