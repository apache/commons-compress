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

package org.apache.commons.compress.compressors.deflate;

import java.util.zip.Deflater;

/**
 * Parameters for the Deflate compressor.
 * @since 1.9
 */
public class DeflateParameters {

    static final int MAX_LEVEL = 9;
    static final int MIN_LEVEL = 0;

    private boolean zlibHeader = true;
    private int compressionLevel = Deflater.DEFAULT_COMPRESSION;

    /**
     * The compression level.
     * @see #setCompressionLevel
     * @return the compression level
     */
    public int getCompressionLevel() {
        return compressionLevel;
    }

    /**
     * Sets the compression level.
     *
     * @param compressionLevel the compression level (between 0 and 9)
     * @see Deflater#NO_COMPRESSION
     * @see Deflater#BEST_SPEED
     * @see Deflater#DEFAULT_COMPRESSION
     * @see Deflater#BEST_COMPRESSION
     */
    public void setCompressionLevel(final int compressionLevel) {
        if (compressionLevel < MIN_LEVEL || compressionLevel > MAX_LEVEL) {
            throw new IllegalArgumentException("Invalid Deflate compression level: " + compressionLevel);
        }
        this.compressionLevel = compressionLevel;
    }

    /**
     * Sets the zlib header presence parameter.
     *
     * <p>This affects whether or not the zlib header will be written
     * (when compressing) or expected (when decompressing).</p>
     *
     * @param zlibHeader true if zlib header shall be written
     */
    public void setWithZlibHeader(final boolean zlibHeader) {
        this.zlibHeader = zlibHeader;
    }

    /**
     * Whether or not the zlib header shall be written (when
     * compressing) or expected (when decompressing).
     * @return true if zlib header shall be written
     */
    public boolean withZlibHeader() {
        return zlibHeader;
    }

}
