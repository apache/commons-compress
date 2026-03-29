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

import java.io.Closeable;
import java.io.IOException;

/**
 * Compresses plain entry payload and writes compressed bytes to the sink passed to
 * {@link ZipCompressionPayloadWriterFactory#create(java.io.OutputStream, ZipArchiveEntry)}.
 * <p>
 * Implementations receive <em>uncompressed</em> data via {@link #write(byte[], int, int)}; CRC and size in the ZIP file are computed from that plain data by
 * {@link ZipArchiveOutputStream}.
 * </p>
 *
 * @see ZipArchiveOutputStream#setCompressionPayloadWriterFactory(int, ZipCompressionPayloadWriterFactory)
 * @since 1.29.0
 */
public interface ZipCompressionPayloadWriter extends Closeable {

    /**
     * Finishes the compressed stream (for example ends the compression frame) so that all compressed bytes have been passed to the payload sink.
     *
     * @throws IOException if finishing fails.
     */
    void finish() throws IOException;

    /**
     * Writes uncompressed bytes; the implementation compresses and forwards them to the payload {@link java.io.OutputStream}.
     *
     * @param b      the buffer.
     * @param offset start offset.
     * @param length number of bytes.
     * @throws IOException if writing fails.
     */
    void write(byte[] b, int offset, int length) throws IOException;

    /**
     * Equivalent to {@link #finish()}.
     *
     * @throws IOException if an error occurs.
     */
    @Override
    default void close() throws IOException {
        finish();
    }
}
