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
 * Creates a {@link ZipCompressionPayloadWriter} for a ZIP local file payload.
 * <p>
 * The {@code compressedPayloadSink} writes raw compressed bytes into the archive entry body; it must not be closed in a way that closes the ZIP output stream.
 * {@link ZipArchiveOutputStream} supplies an implementation that only forwards to {@link org.apache.commons.compress.archivers.zip.StreamCompressor}.
 * </p>
 *
 * @since 1.29.0
 */
@FunctionalInterface
public interface ZipCompressionPayloadWriterFactory {

    /**
     * Creates a writer for the given entry.
     *
     * @param compressedPayloadSink receives compressed payload bytes for this entry.
     * @param entry                 the entry being written (method and name are typically relevant).
     * @return a new writer; not null.
     * @throws IOException if creation fails.
     */
    ZipCompressionPayloadWriter create(OutputStream compressedPayloadSink, ZipArchiveEntry entry) throws IOException;
}
