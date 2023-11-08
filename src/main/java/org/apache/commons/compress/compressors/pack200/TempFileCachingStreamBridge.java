/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.commons.compress.compressors.pack200;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * StreamBridge that caches all data written to the output side in
 * a temporary file.
 *
 * @since 1.3
 */
final class TempFileCachingStreamBridge extends AbstractStreamBridge {

    private final Path path;

    TempFileCachingStreamBridge() throws IOException {
        this.path = Files.createTempFile("commons-compress", "packtemp");
        this.path.toFile().deleteOnExit();
        this.out = Files.newOutputStream(path);
    }

    @SuppressWarnings("resource") // Caller closes
    @Override
    InputStream createInputStream() throws IOException {
        out.close();
        return new FilterInputStream(Files.newInputStream(path)) {
            @Override
            public void close() throws IOException {
                try {
                    super.close();
                } finally {
                    try {
                        Files.deleteIfExists(path);
                    } catch (final IOException ignore) {
                        // if this fails the only thing we can do is to rely on deleteOnExit
                    }
                }
            }
        };
    }
}
