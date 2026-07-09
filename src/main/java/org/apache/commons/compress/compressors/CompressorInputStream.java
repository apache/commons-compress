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
package org.apache.commons.compress.compressors;

import java.io.InputStream;

import org.apache.commons.compress.utils.InputStreamStatistics;

/**
 * Abstracts services for all compressor input streams.
 * <p>
 * As of 1.29.0 this type implements {@link InputStreamStatistics}, so both the decompressed and the compressed byte counts are
 * available on the base type for every format. The base {@link #getCompressedCount()} returns {@code -1} ("unknown"); concrete
 * formats override it with a live count. Opt-in decompression-bomb protection that consumes these counts is provided by
 * {@link BombGuardCompressorInputStream} and by the limit setters on {@link CompressorStreamFactory}.
 * </p>
 */
public abstract class CompressorInputStream extends InputStream implements InputStreamStatistics {

    private long bytesRead;

    /**
     * Constructs a new instance.
     */
    public CompressorInputStream() {
        // empty
    }

    /**
     * Increments the counter of already read bytes. Doesn't increment if the EOF has been hit (read == -1)
     *
     * @param read the number of bytes read.
     * @since 1.1
     */
    protected void count(final int read) {
        count((long) read);
    }

    /**
     * Increments the counter of already read bytes. Doesn't increment if the EOF has been hit (read == -1)
     *
     * @param read the number of bytes read.
     */
    protected void count(final long read) {
        if (read != -1) {
            bytesRead += read;
        }
    }

    /**
     * Gets the current number of bytes read from this stream.
     *
     * @return the number of read bytes.
     * @since 1.1
     */
    public long getBytesRead() {
        return bytesRead;
    }

    /**
     * Gets the amount of raw or compressed bytes consumed by the stream so far.
     * <p>
     * This base implementation returns {@code -1} ("unknown"). Every concrete compressor format except
     * {@link org.apache.commons.compress.compressors.pack200.Pack200CompressorInputStream} overrides it with a live count.
     * </p>
     *
     * @return the amount of raw or compressed bytes consumed, or {@code -1} if unknown.
     * @since 1.29.0
     */
    @Override
    public long getCompressedCount() {
        return -1;
    }

    /**
     * Gets the current number of bytes read from this stream.
     *
     * @return the number of read bytes.
     * @deprecated this method may yield wrong results for large archives, use {@link #getBytesRead()} instead.
     */
    @Deprecated
    public int getCount() {
        return (int) bytesRead;
    }

    /**
     * Gets the amount of raw or compressed bytes read by the stream.
     *
     * <p>
     * This implementation invokes {@link #getBytesRead}.
     * </p>
     *
     * @return the amount of decompressed bytes returned by the stream.
     * @since 1.17
     */
    public long getUncompressedCount() {
        return getBytesRead();
    }

    /**
     * Decrements the counter of already read bytes.
     *
     * @param pushedBack the number of bytes pushed back.
     * @since 1.7
     */
    protected void pushedBackBytes(final long pushedBack) {
        bytesRead -= pushedBack;
    }
}
