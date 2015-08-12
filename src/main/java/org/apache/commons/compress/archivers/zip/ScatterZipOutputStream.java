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


import org.apache.commons.compress.parallel.FileBasedScatterGatherBackingStore;
import org.apache.commons.compress.parallel.ScatterGatherBackingStore;
import org.apache.commons.compress.utils.BoundedInputStream;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.zip.Deflater;

/**
 * A zip output stream that is optimized for multi-threaded scatter/gather construction of zip files.
 * <p>
 * The internal data format of the entries used by this class are entirely private to this class
 * and are not part of any public api whatsoever.
 * </p>
 * <p>It is possible to extend this class to support different kinds of backing storage, the default
 * implementation only supports file-based backing.
 * </p>
 * Thread safety: This class supports multiple threads. But the "writeTo" method must be called
 * by the thread that originally created the {@link ZipArchiveEntry}.
 *
 * @since 1.10
 */
public class ScatterZipOutputStream implements Closeable {
    private final Queue<CompressedEntry> items = new ConcurrentLinkedQueue<CompressedEntry>();
    private final ScatterGatherBackingStore backingStore;
    private final StreamCompressor streamCompressor;

    private static class CompressedEntry {
        final ZipArchiveEntryRequest zipArchiveEntryRequest;
        final long crc;
        final long compressedSize;
        final long size;

        public CompressedEntry(ZipArchiveEntryRequest zipArchiveEntryRequest, long crc, long compressedSize, long size) {
            this.zipArchiveEntryRequest = zipArchiveEntryRequest;
            this.crc = crc;
            this.compressedSize = compressedSize;
            this.size = size;
        }

        /**
         * Update the original {@link ZipArchiveEntry} with sizes/crc
         * Do not use this methods from threads that did not create the instance itself !
         * @return the zipArchiveEntry that is basis for this request
         */

        public ZipArchiveEntry transferToArchiveEntry(){
            ZipArchiveEntry entry = zipArchiveEntryRequest.getZipArchiveEntry();
            entry.setCompressedSize(compressedSize);
            entry.setSize(size);
            entry.setCrc(crc);
            entry.setMethod(zipArchiveEntryRequest.getMethod());
            return entry;
        }
    }

    public ScatterZipOutputStream(ScatterGatherBackingStore backingStore,
                                  StreamCompressor streamCompressor) {
        this.backingStore = backingStore;
        this.streamCompressor = streamCompressor;
    }

    /**
     * Add an archive entry to this scatter stream.
     *
     * @param zipArchiveEntryRequest The entry to write.
     * @throws IOException    If writing fails
     */
    public void addArchiveEntry(ZipArchiveEntryRequest zipArchiveEntryRequest) throws IOException {
        final InputStream payloadStream = zipArchiveEntryRequest.getPayloadStream();
        try {
            streamCompressor.deflate(payloadStream, zipArchiveEntryRequest.getMethod());
        } finally {
            payloadStream.close();
        }
        items.add(new CompressedEntry(zipArchiveEntryRequest, streamCompressor.getCrc32(),
                                      streamCompressor.getBytesWrittenForLastEntry(), streamCompressor.getBytesRead()));
    }

    /**
     * Write the contents of this scatter stream to a target archive.
     *
     * @param target The archive to receive the contents of this {@link ScatterZipOutputStream}.
     * @throws IOException If writing fails
     */
    public void writeTo(ZipArchiveOutputStream target) throws IOException {
        backingStore.closeForWriting();
        InputStream data = backingStore.getInputStream();
        for (CompressedEntry compressedEntry : items) {
            final BoundedInputStream rawStream = new BoundedInputStream(data, compressedEntry.compressedSize);
            target.addRawArchiveEntry(compressedEntry.transferToArchiveEntry(), rawStream);
            rawStream.close();
        }
        data.close();
    }


    /**
     * Closes this stream, freeing all resources involved in the creation of this stream.
     * @throws IOException If closing fails
     */
    public void close() throws IOException {
        backingStore.close();
    }

    /**
     * Create a {@link ScatterZipOutputStream} with default compression level that is backed by a file
     *
     * @param file The file to offload compressed data into.
     * @return A ScatterZipOutputStream that is ready for use.
     * @throws FileNotFoundException if the file cannot be found
     */
    public static ScatterZipOutputStream fileBased(File file) throws FileNotFoundException {
        return fileBased(file, Deflater.DEFAULT_COMPRESSION);
    }

    /**
     * Create a {@link ScatterZipOutputStream} that is backed by a file
     *
     * @param file             The file to offload compressed data into.
     * @param compressionLevel The compression level to use, @see #Deflater
     * @return A  ScatterZipOutputStream that is ready for use.
     * @throws FileNotFoundException if the file cannot be found
     */
    public static ScatterZipOutputStream fileBased(File file, int compressionLevel) throws FileNotFoundException {
        ScatterGatherBackingStore bs = new FileBasedScatterGatherBackingStore(file);
        StreamCompressor sc = StreamCompressor.create(compressionLevel, bs);
        return new ScatterZipOutputStream(bs, sc);
    }
}
