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

/**
 * Configuration for ZSTD compression in ZIP archives.
 *
 * @since 1.29.0
 */
public class ZstdCompressorConfig implements CompressorConfig {

    private final int level;
    private final boolean closeFrameOnFlush;

    /**
     * Creates a default ZSTD configuration (level 3, closeFrameOnFlush true).
     */
    public ZstdCompressorConfig() {
        this(3, true);
    }

    /**
     * Creates a ZSTD configuration with the specified parameters.
     *
     * @param level             the compression level
     * @param closeFrameOnFlush whether to close the frame on flush
     */
    public ZstdCompressorConfig(final int level, final boolean closeFrameOnFlush) {
        this.level = level;
        this.closeFrameOnFlush = closeFrameOnFlush;
    }

    /**
     * Gets the compression level.
     *
     * @return the compression level
     */
    public int getLevel() {
        return level;
    }

    /**
     * Gets whether to close the frame on flush.
     *
     * @return whether to close the frame on flush
     */
    public boolean isCloseFrameOnFlush() {
        return closeFrameOnFlush;
    }
}
