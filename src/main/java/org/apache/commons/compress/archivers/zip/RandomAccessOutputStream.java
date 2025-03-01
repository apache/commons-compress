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

package org.apache.commons.compress.archivers.zip;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Abstraction over OutputStream which also allows random access writes.
 */
// Keep package-private; consider for Apache Commons IO.
abstract class RandomAccessOutputStream extends OutputStream {

    /**
     * Gets the current position in this stream.
     *
     * @return current position.
     * @throws IOException if an I/O error occurs
     */
    abstract long position() throws IOException;

    @Override
    public void write(final int b) throws IOException {
        write(new byte[] { (byte) b });
    }

    /**
     * Writes all given bytes at a position.
     *
     * @param position position in the stream
     * @param bytes    data to write
     * @param offset   offset of the start of data in param bytes
     * @param len      the length of data to write
     * @throws IOException if an I/O error occurs.
     */
    abstract void writeAll(byte[] bytes, int offset, int len, long position) throws IOException;

    /**
     * Writes all given bytes at a position.
     *
     * @param position position in the stream
     * @param bytes    data to write
     * @throws IOException if an I/O error occurs.
     */
    void writeAll(final byte[] bytes, final long position) throws IOException {
        writeAll(bytes, 0, bytes.length, position);
    }
}
