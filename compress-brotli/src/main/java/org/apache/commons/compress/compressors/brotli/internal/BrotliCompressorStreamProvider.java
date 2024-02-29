/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.compress.compressors.brotli.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Set;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamProvider;
import org.apache.commons.compress.compressors.brotli.BrotliCompressorInputStream;

public class BrotliCompressorStreamProvider implements CompressorStreamProvider {

    private static final String BROTLI = "br";
    private static final CompressorStreamProvider INSTANCE = new BrotliCompressorStreamProvider();

    // Used by Java 9 ServiceLoader
    public static CompressorStreamProvider provider() {
        return INSTANCE;
    }

    @Override
    public CompressorInputStream createCompressorInputStream(final String name,
                                                             final InputStream in,
                                                             final boolean decompressUntilEOF) throws CompressorException {
        if (BROTLI.equals(name)) {
            try {
                return new BrotliCompressorInputStream(in);
            } catch (final IOException e) {
                throw new CompressorException("Could not create CompressorInputStream.", e);
            }
        }
        throw new CompressorException("Compressor: " + name + " not found.");
    }

    @Override
    public CompressorOutputStream createCompressorOutputStream(String name, OutputStream out) throws CompressorException {
        throw new CompressorException("Compressor: " + name + " not found.");
    }

    @Override
    public Set<String> getInputStreamCompressorNames() {
        return Collections.singleton(BROTLI);
    }

    @Override
    public Set<String> getOutputStreamCompressorNames() {
        return Collections.emptySet();
    }
}
