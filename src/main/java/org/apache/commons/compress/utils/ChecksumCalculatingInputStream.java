/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.commons.compress.utils;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.zip.Checksum;

/**
 * Calculates the checksum of the data read.
 *
 * @NotThreadSafe
 * @since 1.14
 */
public class ChecksumCalculatingInputStream extends FilterInputStream {
    private final Checksum checksum;

    public ChecksumCalculatingInputStream(final Checksum checksum, final InputStream inputStream) {
        super(Objects.requireNonNull(inputStream, "inputStream"));
        this.checksum = Objects.requireNonNull(checksum, "checksum");
    }

    /**
     * Returns the calculated checksum.
     *
     * @return the calculated checksum.
     */
    public long getValue() {
        return checksum.getValue();
    }

    /**
     * Reads a single byte from the stream
     *
     * @throws IOException if the underlying stream throws or the stream is exhausted and the Checksum doesn't match the expected value
     */
    @Override
    public int read() throws IOException {
        final int ret = in.read();
        if (ret >= 0) {
            checksum.update(ret);
        }
        return ret;
    }

    /**
     * Reads from the stream into a byte array.
     *
     * @throws IOException if the underlying stream throws or the stream is exhausted and the Checksum doesn't match the expected value
     */
    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        if (len == 0) {
            return 0;
        }
        final int ret = in.read(b, off, len);
        if (ret >= 0) {
            checksum.update(b, off, ret);
        }
        return ret;
    }

    @Override
    public long skip(final long n) throws IOException {
        // Can't really skip, we have to hash everything to verify the checksum
        if (read() >= 0) {
            return 1;
        }
        return 0;
    }

}
