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
     * Provides current position in output.
     *
     * @return current position.
     * @throws IOException if an I/O error occurs
     */
    public abstract long position() throws IOException;

    @Override
    public void write(final int b) throws IOException {
        write(new byte[] { (byte) b });
    }

    /**
     * Writes given data to specific position.
     *
     * @param position position in the stream
     * @param b        data to write
     * @param off      offset of the start of data in param b
     * @param len      the length of data to write
     * @throws IOException if an I/O error occurs.
     */
    abstract void writeFully(byte[] b, int off, int len, long position) throws IOException;

    /**
     * Writes given data to specific position.
     *
     * @param position position in the stream
     * @param b        data to write
     * @throws IOException if an I/O error occurs.
     */
    public void writeFully(final byte[] b, final long position) throws IOException {
        writeFully(b, 0, b.length, position);
    }
}
