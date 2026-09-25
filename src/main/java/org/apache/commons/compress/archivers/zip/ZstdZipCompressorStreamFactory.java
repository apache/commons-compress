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

import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorOutputStream;

/**
 * Factory for creating ZSTD compressor output streams in ZIP archives.
 *
 * @since 1.29.0
 */
public class ZstdZipCompressorStreamFactory implements ZipCompressorStreamFactory {

    @Override
    public CompressorOutputStream<?> createCompressorOutputStream(final OutputStream out, final CompressorConfig config) throws IOException {
        final ZstdCompressorConfig zstdConfig = (ZstdCompressorConfig) config;
        return new ZstdCompressorOutputStream(out, zstdConfig.getLevel(), zstdConfig.isCloseFrameOnFlush());
    }

    @Override
    public CompressorConfig defaultConfig() {
        return new ZstdCompressorConfig();
    }
}
