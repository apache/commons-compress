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
package org.apache.commons.compress2.compressors;

import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.io.IOException;
import org.apache.commons.compress2.Format;

/**
 * Describes a given compression format and works as factory and content-probe at the same time.
 * @Immutable
 */
public interface CompressionFormat extends Format {

    /**
     * Reads a compressed channel.
     * @param channel the channel to read from
     * @throws IOException
     */
    CompressedInput readFrom(ReadableByteChannel channel) throws IOException;

    /**
     * Reads a compressed file.
     * @param file the file to read from
     * @throws IOException
     */
    default CompressedInput readFrom(Path path) throws IOException {
        return readFrom(FileChannel.open(path, StandardOpenOption.READ));
    }

    /**
     * Compresses data to a given channel.
     * @param channel the channel to write to
     * @throws IOException
     * @throws UnsupportedOperationException if this format doesn't
     * support writing.
     */
    CompressedOutput writeTo(WritableByteChannel channel)
        throws IOException, UnsupportedOperationException;

    /**
     * Compresses data to a given file.
     * @param file the file to write to
     * @throws IOException
     * @throws UnsupportedOperationException if this format doesn't support writing
     */
    default CompressedOutput writeTo(Path path) throws IOException, UnsupportedOperationException {
        return writeTo(FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE,
                                        StandardOpenOption.TRUNCATE_EXISTING));
    }
}
