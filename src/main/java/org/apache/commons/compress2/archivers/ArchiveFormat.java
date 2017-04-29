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
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.io.IOException;
import org.apache.commons.compress2.Format;

/**
 * Describes a given archive format and works as factory and content-probe at the same time.
 * @Immutable
 */
public interface ArchiveFormat<A extends ArchiveEntry> extends Format {
    /**
     * Does the format support random access reading?
     * @return whether random access reading is supported
     */
    boolean supportsRandomAccessInput();
    /**
     * Does the format support writing to arbitrary non-seekable channels?
     * @return whether writing to arbitrary non-seekable channels is supported
     */
    boolean supportsWritingToNonSeekableChannels();
    /**
     * Does the format support reading from arbitrary non-seekable channels?
     * @return whether writing to arbitrary non-seekable channels is supported
     */
    boolean supportsReadingFromNonSeekableChannels();

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
    default ArchiveInput<A> readFrom(Path path, Charset charset) throws IOException {
        SeekableByteChannel channel = FileChannel.open(path, StandardOpenOption.READ);
        if (supportsRandomAccessInput()) {
            return readWithRandomAccessFrom(channel, charset);
        }

        return readFrom(channel, charset);
    }
    /**
     * Provides random access to an archive assuming the given charset for entry names.
     * @param channel the seekable channel to read from
     * @param charset the charset used for encoding the entry names.
     * @throws IOException
     * @throws UnsupportedOperationException if this format doesn't support random access
     */
    RandomAccessArchiveInput<A> readWithRandomAccessFrom(SeekableByteChannel channel, Charset charset)
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
    default ArchiveOutput<A> writeTo(Path path, Charset charset) throws IOException, UnsupportedOperationException {
        return writeTo(FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE,
                                        StandardOpenOption.TRUNCATE_EXISTING),
                       charset);
    }
}
