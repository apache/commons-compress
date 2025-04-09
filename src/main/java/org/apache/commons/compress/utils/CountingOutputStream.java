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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Stream that tracks the number of bytes read.
 *
 * @since 1.3
 * @NotThreadSafe
 * @deprecated Use {@link org.apache.commons.io.output.CountingOutputStream}.
 */
@Deprecated
public class CountingOutputStream extends FilterOutputStream {
    private long bytesWritten;

    /**
     * Creates a {@code CountingOutputStream} filter built on top of the specified underlying output stream.
     *
     * @param out the underlying output stream to be assigned to the field {@code this.out} for later use, or {@code null} if this instance is to be created
     *            without an underlying stream.
     */
    public CountingOutputStream(final OutputStream out) {
        super(out);
    }

    /**
     * Increments the counter of already written bytes. Doesn't increment if the EOF has been hit (written == -1)
     *
     * @param written the number of bytes written
     */
    protected void count(final long written) {
        if (written != -1) {
            bytesWritten += written;
        }
    }

    /**
     * Returns the current number of bytes written to this stream.
     *
     * @return the number of written bytes
     */
    public long getBytesWritten() {
        return bytesWritten;
    }

    @Override
    public void write(final byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        out.write(b, off, len);
        count(len);
    }

    @Override
    public void write(final int b) throws IOException {
        out.write(b);
        count(1);
    }
}
