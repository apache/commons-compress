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
 * Configuration for BZIP2 compression in ZIP archives.
 *
 * @since 1.29.0
 */
public class BZip2CompressorConfig implements CompressorConfig {

    private final int blockSize;

    /**
     * Creates a default BZIP2 configuration (blockSize 9).
     */
    public BZip2CompressorConfig() {
        this(9);
    }

    /**
     * Creates a BZIP2 configuration with the specified block size.
     *
     * @param blockSize the block size (1-9)
     */
    public BZip2CompressorConfig(final int blockSize) {
        this.blockSize = blockSize;
    }

    /**
     * Gets the block size.
     *
     * @return the block size
     */
    public int getBlockSize() {
        return blockSize;
    }
}
