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
package org.apache.commons.compress.utils;

import java.io.InputStream;

/**
 * A stream that limits reading from a wrapped stream to a given number of bytes.
 *
 * @NotThreadSafe
 * @since 1.6
 * @deprecated Use {@link org.apache.commons.io.input.BoundedInputStream}.
 */
@Deprecated
public class BoundedInputStream extends org.apache.commons.io.input.BoundedInputStream {

    /**
     * Creates the stream that will at most read the given amount of bytes from the given stream.
     *
     * @param in   the stream to read from
     * @param size the maximum amount of bytes to read
     */
    public BoundedInputStream(final InputStream in, final long size) {
        super(in, size);
        setPropagateClose(false);
    }

    /**
     * Gets how many bytes remain to read.
     *
     * @return bytes how many bytes remain to read.
     * @since 1.21
     */
    public long getBytesRemaining() {
        return getMaxCount() - getCount();
    }

//    @Override
//    protected void onMaxLength(long maxLength, long count) throws IOException {
//        if (count > maxLength) {
//            throw new IOException("Can't read past EOF.");
//        }
//    }
}
