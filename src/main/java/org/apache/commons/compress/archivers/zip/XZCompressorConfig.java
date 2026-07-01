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
 * Configuration for XZ compression in ZIP archives.
 *
 * @since 1.29.0
 */
public class XZCompressorConfig implements CompressorConfig {

    private final int preset;

    /**
     * Creates a default XZ configuration (preset 6).
     */
    public XZCompressorConfig() {
        this(6);
    }

    /**
     * Creates an XZ configuration with the specified preset.
     *
     * @param preset the LZMA2 preset level (0-9)
     */
    public XZCompressorConfig(final int preset) {
        this.preset = preset;
    }

    /**
     * Gets the preset level.
     *
     * @return the preset level
     */
    public int getPreset() {
        return preset;
    }
}
