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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import org.apache.commons.compress.utils.ArchiveUtils;

final class ComposedTarInputStream extends InputStream {

    final Iterator<? extends InputStream> streams;
    private final long size;
    private InputStream current;
    private long position;

    ComposedTarInputStream(final Iterable<? extends InputStream> streams, final long size) {
        this.streams = streams.iterator();
        this.size = size;
        this.current = this.streams.hasNext() ? this.streams.next() : null;
        this.position = 0;
    }

    @Override
    public void close() throws IOException {
        while (current != null) {
            current.close();
            current = streams.hasNext() ? streams.next() : null;
        }
    }

    @Override
    public int read() throws IOException {
        if (position >= size) {
            return -1;
        }
        while (current != null) {
            final int ret = current.read();
            if (ret != -1) {
                position++;
                return ret;
            }
            nextStream();
        }
        throw new EOFException(String.format("Truncated TAR archive: expected %d bytes, but got only %d bytes", size, position));
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        ArchiveUtils.checkFromIndexSize(b, off, len);
        if (len == 0) {
            return 0;
        }
        if (position >= size) {
            return -1;
        }

        final int toRead = (int) Math.min(size - position, len);
        int remaining = toRead;
        int dst = off;

        while (current != null && remaining > 0) {
            final int n = current.read(b, dst, remaining);
            if (n == -1) {
                nextStream();
                continue;
            }
            position += n;
            dst += n;
            remaining -= n;
        }

        if (remaining == 0) {
            return toRead;
        }
        throw new EOFException(String.format("Truncated TAR archive: expected %d bytes, but got only %d bytes", size, position));
    }

    private void nextStream() throws IOException {
        if (current != null) {
            current.close();
        }
        current = streams.hasNext() ? streams.next() : null;
    }
}
