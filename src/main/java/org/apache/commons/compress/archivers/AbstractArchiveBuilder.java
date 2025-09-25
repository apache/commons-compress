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

import org.apache.commons.io.build.AbstractStreamBuilder;

/**
 * Base class for builder pattern implementations of all archive readers.
 *
 * <p>Ensures that all {@code ArchiveInputStream} implementations and other
 * archive handlers expose a consistent set of configuration options.</p>
 *
 * @param <T> The type of archive stream or file to build.
 * @param <B> The type of the concrete builder subclass.
 * @since 1.29.0
 */
public abstract class AbstractArchiveBuilder<T, B extends AbstractArchiveBuilder<T, B>>
        extends AbstractStreamBuilder<T, B> {

    private int maxEntryNameLength = Short.MAX_VALUE;

    /**
     * Constructs a new instance.
     */
    protected AbstractArchiveBuilder() {
        // empty
    }

    /**
     * Sets the maximum length, in bytes, of an archive entry name.
     *
     * <p>Most operating systems and file systems impose relatively small limits on
     * file name or path length, which are sufficient for everyday use. By contrast,
     * many archive formats permit much longer names: for example, TAR can encode
     * names of several gigabytes, while ZIP allows up to 64&nbsp;KiB.</p>
     *
     * <p>This setting applies an upper bound on entry name length after encoding
     * with the {@link #setCharset configured charset}. If an entry name exceeds this
     * limit, an {@link ArchiveException} will be thrown during reading.</p>
     *
     * <p>The default is {@link Short#MAX_VALUE}, which already exceeds the limits
     * of most operating systems.</p>
     *
     * @param maxEntryNameLength The maximum entry name length in bytes; must be positive
     * @return {@code this} instance.
     * @throws IllegalArgumentException If {@code maxEntryNameLength} is not positive.
     */
    public B setMaxEntryNameLength(final int maxEntryNameLength) {
        if (maxEntryNameLength <= 0) {
            throw new IllegalArgumentException("maxEntryNameLength must be positive");
        }
        this.maxEntryNameLength = maxEntryNameLength;
        return asThis();
    }

    /**
     * Gets the maximum length of an archive entry name.
     *
     * @return The maximum length of an archive entry name.
     */
    public int getMaxEntryNameLength() {
        return maxEntryNameLength;
    }
}
