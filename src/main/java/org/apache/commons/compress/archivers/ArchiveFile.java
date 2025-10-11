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
package org.apache.commons.compress.archivers;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

import org.apache.commons.io.function.IOIterable;
import org.apache.commons.io.function.IOIterator;
import org.apache.commons.io.function.IOStream;

/**
 * A file-based representation of an archive containing multiple {@link ArchiveEntry entries}.
 *
 * <p>This interface provides a higher-level abstraction over archive files, similar to
 * {@link ZipFile}, but generalized for a variety of archive formats.</p>
 *
 * <p>Implementations are {@link Closeable} and should be closed once they are no longer
 * needed in order to release any underlying system resources.</p>
 *
 * @param <T> the type of {@link ArchiveEntry} produced by this archive
 * @since 1.29.0
 */
public interface ArchiveFile<T extends ArchiveEntry> extends Closeable, IOIterable<T> {

    /**
     * Returns all entries contained in the archive as a list.
     *
     * <p>The order of entries is format-dependent but guaranteed to be consistent
     * across multiple invocations on the same archive.</p>
     *
     * @return An immutable list of all entries in this archive.
     */
    default List<? extends T> entries() {
        try (IOStream<? extends T> stream = stream()) {
            return stream.collect(Collectors.toList());
        }
    }

    /**
     * Opens an input stream for the specified entry's contents.
     *
     * <p>The caller is responsible for closing the returned stream after use.</p>
     *
     * @param entry The archive entry to read.
     * @return An input stream providing the contents of the given entry.
     * @throws IOException If an I/O error occurs while opening the entry stream.
     */
    InputStream getInputStream(T entry) throws IOException;

    @Override
    @SuppressWarnings("unchecked")
    default IOIterator<T> iterator() {
        return IOIterator.adapt((Iterable<T>) entries());
    }

    /**
     * Returns a sequential stream of archive entries.
     *
     * <p>The order of entries is format-dependent but stable for a given archive.</p>
     * <p>The returned stream <strong>must</strong> be closed after use to free
     * associated resources.</p>
     *
     * @return A stream of entries in this archive.
     */
    IOStream<? extends T> stream();

    @Override
    default Iterable<T> unwrap() {
        return asIterable();
    }
}

