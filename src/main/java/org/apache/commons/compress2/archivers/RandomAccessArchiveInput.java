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

import java.nio.channels.ReadableByteChannel;

/**
 * ArchiveInput that provides random access to all entries.
 * @NotThreadSafe
 */
public interface RandomAccessArchiveInput<A extends ArchiveEntry> extends ArchiveInput<A>, Iterable<A> {

    /**
     * Obtains all entries of a given name.
     * @param name the name of the entries to look for
     * @return all entries matching that name, will never be null.
     */
    Iterable<A> getEntries(String name);

    /**
     * Obtains a channel the contents of given entry can be read from.
     * @param entry the entry to read the contents of
     * @return a channel to read the entry's contents from or null if the entry is not part of this archive.
     */
    ReadableByteChannel getChannel(A entry);

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
}
