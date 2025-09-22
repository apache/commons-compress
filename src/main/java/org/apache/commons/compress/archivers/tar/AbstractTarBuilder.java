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

package org.apache.commons.compress.archivers.tar;

import org.apache.commons.io.build.AbstractStreamBuilder;

/**
 * Abstracts TAR builder operations.
 *
 * @param <T> the type of instances to build.
 * @param <B> the type of builder subclass.
 * @since 1.29.0
 */
public abstract class AbstractTarBuilder<T, B extends AbstractTarBuilder<T, B>> extends AbstractStreamBuilder<T, B> {

    private int blockSize = TarConstants.DEFAULT_BLKSIZE;
    private int recordSize = TarConstants.DEFAULT_RCDSIZE;
    private boolean lenient;

    /**
     * Constructs a new instance.
     */
    protected AbstractTarBuilder() {
        // empty
    }

    int getBlockSize() {
        return blockSize;
    }

    int getRecordSize() {
        return recordSize;
    }

    boolean isLenient() {
        return lenient;
    }

    /**
     * Sets the block size.
     *
     * @param blockSize the block size.
     * @return {@code this} instance.
     */
    public B setBlockSize(final int blockSize) {
        this.blockSize = blockSize;
        return asThis();
    }

    /**
     * Sets whether illegal values for group/userid, mode, device numbers and timestamp will be ignored and the fields set to {@link TarArchiveEntry#UNKNOWN}.
     * When set to false such illegal fields cause an exception instead.
     *
     * @param lenient whether illegal values throw exceptions.
     * @return {@code this} instance.
     */
    public B setLenient(final boolean lenient) {
        this.lenient = lenient;
        return asThis();
    }

    /**
     * Sets the record size.
     *
     * @param recordSize the record size.
     * @return {@code this} instance.
     */
    public B setRecordSize(final int recordSize) {
        this.recordSize = recordSize;
        return asThis();
    }
}
