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
package org.apache.commons.compress2.archivers;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.io.File;
import java.io.IOException;

/**
 * Describes a given archive format and works as factory and content-probe at the same time.
 * @Immutable
 */
public interface ArchiveFormat<A extends ArchiveEntry> {
    /**
     * The name by which this format is known.
     * @return the name by which this format is known
     */
    String getName();

    /**
     * Does the format support writing?
     * @return whether writing is supported
     */
    boolean supportsWriting();
    /**
     * Does the format support random access reading?
     * @return whether random access reading is supported
     */
    boolean supportsRandomAccessInput();
    /**
     * Does the format support writing to arbitrary non-seekable channels?
     * @return whether writing to arbitrary non-seekable channels is supported
     */
    boolean supportsWritingToChannels();
    /**
     * Does the format support reading from arbitrary non-seekable channels?
     * @return whether writing to arbitrary non-seekable channels is supported
     */
    boolean supportsReadingFromChannels();

    /**
     * Does the format support content-based detection?
     * @return whether the format supports content-based detection.
     */
    boolean supportsAutoDetection();
    /**
     * If this format supports content-based detection, how many bytes does it need to read to know a channel is
     * readable by this format?
     * @return the minimal number of bytes needed
     * @throws UnsupportedOperationException if this format doesn't support content based detection.
     */
    int getNumberOfBytesRequiredForAutodetection() throws UnsupportedOperationException;
    /**
     * Verifies the given input is readable by this format.
     * @param probe a buffer holding at least {@link #getNumberOfBytesRequiredForAutodetection} bytes
     * @return whether the input is readable by this format
     * @throws UnsupportedOperationException if this format doesn't support content based detection.
     */
    boolean matches(ByteBuffer probe) throws UnsupportedOperationException;

    /**
     * Reads an archive assuming the given charset for entry names.
     * @param channel the channel to read from
     * @param charset the charset used for encoding the entry names.
     * @throws IOException
     * @throws UnsupportedOperationException if this format cannot read from non-seekable channels.
     */
    ArchiveInput<A> readFrom(ReadableByteChannel channel, Charset charset)
        throws IOException, UnsupportedOperationException;
    /**
     * Reads an archive assuming the given charset for entry names.
     * @param file the file to read from
     * @param charset the charset used for encoding the entry names.
     * @throws IOException
     */
    // TODO go for SeekableByteChannel rather than File when embracing Java7?
    // TODO use Path rather than File?
    ArchiveInput<A> readFrom(File file, Charset charset) throws IOException;
    /**
     * Provides random access to an archive assuming the given charset for entry names.
     * @param file the file to read from
     * @param charset the charset used for encoding the entry names.
     * @throws IOException
     * @throws UnsupportedOperationException if this format doesn't support random access
     */
    // TODO go for SeekableByteChannel rather than File when embracing Java7?
    // TODO use Path rather than File?
    RandomAccessArchiveInput<A> readWithRandomAccessFrom(File file, Charset charset)
        throws IOException, UnsupportedOperationException;

    /**
     * Writes an archive using the given charset for entry names.
     * @param channel the channel to write to
     * @param charset the charset to use for encoding the entry names.
     * @throws IOException
     * @throws UnsupportedOperationException if this format cannot write to non-seekable channels or doesn't support
     * writing at all.
     */
    ArchiveOutput<A> writeTo(WritableByteChannel channel, Charset charset)
        throws IOException, UnsupportedOperationException;
    /**
     * Writes an archive using the given charset for entry names.
     * @param file the file to write to
     * @param charset the charset to use for encoding the entry names.
     * @throws IOException
     * @throws UnsupportedOperationException if this format doesn't support writing
     */
    // TODO go for SeekableByteChannel rather than File when embracing Java7?
    // TODO use Path rather than File?
    ArchiveOutput<A> writeTo(File file, Charset charset) throws IOException, UnsupportedOperationException;
}
