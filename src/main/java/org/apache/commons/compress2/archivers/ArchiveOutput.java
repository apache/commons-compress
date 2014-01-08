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

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.WritableByteChannel;

/**
 * Writes {@link ArchiveEntry}s.
 * @NotThreadSafe
 */
public interface ArchiveOutput<A extends ArchiveEntry> extends Closeable {

    /**
     * Creates an ArchiveEntry for the given parameters.
     * @param params the parameters describing the archive entry.
     * @return a new archive entry.
     */
    A createEntry(ArchiveEntryParameters params);

    /**
     * Whether this channel is able to write the contents of the given entry.
     *
     * <p>Some archive formats support variants or details that are not supported (yet).</p>
     *
     * @param archiveEntry
     *            the entry to test
     * @return whether the entry's content can be read
     */
    boolean canWriteEntryData(A archiveEntry);

    /**
     * Initializes the channel for writing a new {@link ArchiveEntry}.
     *
     * <p>The caller must then write the content to the channel and call {@link #closeEntry()} to complete the
     * process.</p>
     * 
     * @param entry describes the entry
     * @return a channel to write the entry's contents to
     * @throws IOException
     */
    WritableByteChannel putEntry(A entry) throws IOException;
    
    /**
     * Closes the archive entry, writing any trailer information that may be required.
     * @throws IOException
     */
    void closeEntry() throws IOException;

    /**
     * Finishes the addition of entries to this stream, without closing it.
     *
     * <p>Additional data can be written, if the format supports it.<p>
     * 
     * @throws IOException
     */
    void finish() throws IOException;

    /**
     * Returns the current number of bytes written to this channel.
     * @return the number of written bytes
     */
    long getBytesWritten();
}
