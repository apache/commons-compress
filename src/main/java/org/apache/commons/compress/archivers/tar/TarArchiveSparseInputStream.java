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
package org.apache.commons.compress.archivers.tar;

import org.apache.commons.compress.utils.BoundedInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * For sparse tar entries, there are many "holes"(consisting of all 0) in the file. Only the non-zero data is
 * stored in tar files, and they are stored separately. The structure of non-zero data is introduced by the
 * sparse headers using the offset, where a block of non-zero data starts, and numbytes, the length of the
 * non-zero data block.
 * This class is used to construct an input stream that combines the "holes" and the non-zero data together using
 * the sparse headers. When reading from this input stream, the actual data is read out with "holes" and non-zero
 * data combined together according to the sparse headers.
 */
public class TarArchiveSparseInputStream extends InputStream {
    /** the sparse headers describing the sparse information */
    private List<TarArchiveStructSparse> sparseHeaders;

    /** the input stream of the tar file */
    private InputStream inputStream;

    /** the input streams consisting of all-zero input streams and non-zero streams */
    private List<InputStream> inputStreams;

    /** the index of current input stream being read */
    private int currentInputStreamIndex = -1;

    public TarArchiveSparseInputStream(List<TarArchiveStructSparse> sparseHeaders, InputStream inputStream) {
        this.sparseHeaders = sparseHeaders;
        this.inputStream = inputStream;
        buildInputStreams();

        if (inputStreams.size() > 0) {
            currentInputStreamIndex = 0;
        }
    }

    @Override
    public int read() throws IOException {
        // if there are no actual input streams, just read from the original input stream
        if (inputStreams.size() == 0) {
            return inputStream.read();
        }

        int value = inputStreams.get(currentInputStreamIndex).read();
        if (value != -1) {
            return value;
        }

        if (currentInputStreamIndex == inputStreams.size() - 1) {
            return -1;
        }

        currentInputStreamIndex++;
        return inputStreams.get(currentInputStreamIndex).read();
    }

    @Override
    public int read(byte[] buf) throws IOException {
        return read(buf, 0, buf.length);
    }

    @Override
    public int read(byte[] buf, int offset, int len) throws IOException {
        // if there are no actual input streams, just read from the original input stream
        if (inputStreams.size() == 0) {
            return inputStream.read(buf, offset, len);
        }

        InputStream currentInputStream = inputStreams.get(currentInputStreamIndex);
        int readLen = currentInputStream.read(buf, offset, len);

        // if the current input stream is the last input stream,
        // just return the number of bytes read from current input stream
        if (currentInputStreamIndex == inputStreams.size() - 1) {
            return readLen;
        }

        // if EOF of current input stream is meet, open a new input stream and recursively call read
        if (readLen == -1) {
            currentInputStreamIndex++;
            return read(buf, offset, len);
        }

        // if the rest data of current input stream is not long enough, open a new input stream
        // and recursively call read
        if (readLen < len) {
            currentInputStreamIndex++;
            int readLenOfNext = read(buf, offset + readLen, len - readLen);
            if (readLenOfNext == -1) {
                return readLen;
            }

            return readLen + readLenOfNext;
        }

        // if the rest data of current input stream is enough(which means readLen == len), just return readLen
        return readLen;
    }

    /**
     * Skip n bytes from current input stream, if the current input stream doesn't have enough data to skip,
     * jump to the next input stream and skip the rest bytes, keep doing this until total n bytes are skipped
     * or the input streams are all skipped
     *
     * @param n bytes of data to skip
     * @return actual bytes of data skipped
     * @throws IOException
     */
    @Override
    public long skip(final long n) throws IOException {
        if (inputStreams.size() == 0) {
            return inputStream.skip(n);
        }

        long bytesSkipped = 0;
        InputStream currentInputStream;

        while (bytesSkipped < n && currentInputStreamIndex < inputStreams.size()) {
            currentInputStream = inputStreams.get(currentInputStreamIndex);
            bytesSkipped += currentInputStream.skip(n - bytesSkipped);

            if (bytesSkipped < n) {
                currentInputStreamIndex++;
            }
        }

        return bytesSkipped;
    }

    /**
     * Close all the input streams in inputStreams
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        for (InputStream inputStream : inputStreams) {
            inputStream.close();
        }
    }

    /**
     * Build the input streams consisting of all-zero input streams and non-zero input streams.
     * When reading from the non-zero input streams, the data is actually read from the original input stream.
     * The size of each input stream is introduced by the sparse headers.
     *
     * NOTE : Some all-zero input streams and non-zero input streams have the size of 0. We DO NOT store the
     *        0 size input streams because they are meaningless.
     */
    private void buildInputStreams() {
        inputStreams = new ArrayList<>();
        InputStream zeroInputStream = new TarArchiveSparseZeroInputStream();

        long offset = 0;
        for (TarArchiveStructSparse sparseHeader : sparseHeaders) {
            if (sparseHeader.getOffset() == 0 && sparseHeader.getNumbytes() == 0) {
                break;
            }

            // only store the input streams with non-zero size
            if ((sparseHeader.getOffset() - offset) > 0) {
                inputStreams.add(new BoundedInputStream(zeroInputStream, sparseHeader.getOffset() - offset));
            }

            // only store the input streams with non-zero size
            if (sparseHeader.getNumbytes() > 0) {
                inputStreams.add(new BoundedInputStream(inputStream, sparseHeader.getNumbytes()));
            }

            offset = sparseHeader.getOffset() + sparseHeader.getNumbytes();
        }
    }

    /**
     * This is an inputstream that always return 0,
     * this is used when writing the holes of a sparse file
     */
    public class TarArchiveSparseZeroInputStream extends InputStream {
        /**
         * Just return 0
         * @return
         * @throws IOException
         */
        @Override
        public int read() throws IOException {
            return 0;
        }

        /**
         * these's nothing need to do when skipping
         *
         * @param n bytes to skip
         * @return bytes actually skipped
         */
        @Override
        public long skip(final long n) {
            return n;
        }
    }
}
