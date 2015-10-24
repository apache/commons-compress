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
import java.nio.channels.ReadableByteChannel;

/**
 * Reads {@link ArchiveEntry}s.
 * @NotThreadSafe
 */
public interface ArchiveInput<A extends ArchiveEntry> extends Closeable {

    /**
     * Obtains the next entry.
     * @return the next entry or null if the end of the channel has been reached.
     */
    A next() throws IOException;

    /**
     * Obtains a channel the contents of the current entry can be read from.
     * @return a channel to read the entry's contents from
     */
    // TODO use some sort of Tuple or Map.Entry<A, ReadableByteChannel> and combine with next() ?
    ReadableByteChannel getChannel();

    /**
     * Whether this channel is able to read the contents of the given entry.
     * 
     * <p>Some archive formats support variants or details that are not supported (yet).</p>
     * 
     * @param entry
     *            the entry to test
     * @return whether the entry's content can be read
     */
    boolean canReadEntryData(A entry);

    /**
     * Returns the current number of bytes read from this channel.
     * @return the number of read bytes
     */
    long getBytesRead();
}
