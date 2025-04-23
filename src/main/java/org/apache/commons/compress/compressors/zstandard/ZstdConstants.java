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

package org.apache.commons.compress.compressors.zstandard;

import com.github.luben.zstd.Zstd;

/**
 * Zstd constants.
 *
 * @since 1.28.0
 */
public class ZstdConstants {

    /**
     * Maximum chain log value.
     *
     * <p>
     * <small>This constant name matches the name in the C header file.</small>
     * </p>
     *
     * @see ZstdCompressorOutputStream.Builder#setLevel(int)
     * @see <a href="https://github.com/facebook/zstd/blob/dev/lib/zstd.h">zstd.h</a>
     */
    public static final int ZSTD_CHAINLOG_MAX = Zstd.chainLogMax();

    /**
     * Minimum chain log value.
     *
     * <p>
     * <small>This constant name matches the name in the C header file.</small>
     * </p>
     *
     * @see ZstdCompressorOutputStream.Builder#setLevel(int)
     * @see <a href="https://github.com/facebook/zstd/blob/dev/lib/zstd.h">zstd.h</a>
     */
    public static final int ZSTD_CHAINLOG_MIN = Zstd.chainLogMin();

    /**
     * Default compression level.
     *
     * <p>
     * <small>This constant name matches the name in the C header file.</small>
     * </p>
     *
     * @see ZstdCompressorOutputStream.Builder#setLevel(int)
     * @see <a href="https://github.com/facebook/zstd/blob/dev/lib/zstd.h">zstd.h</a>
     */
    public static final int ZSTD_CLEVEL_DEFAULT = Zstd.defaultCompressionLevel();

    /**
     * Maximum compression level.
     *
     * <p>
     * <small>This constant name matches the name in the C header file.</small>
     * </p>
     *
     * @see ZstdCompressorOutputStream.Builder#setLevel(int)
     * @see <a href="https://github.com/facebook/zstd/blob/dev/lib/zstd.h">zstd.h</a>
     */
    public static final int ZSTD_CLEVEL_MAX = Zstd.maxCompressionLevel();

    /**
     * Minimum compression level.
     *
     * <p>
     * <small>This constant name matches the name in the C header file.</small>
     * </p>
     *
     * @see ZstdCompressorOutputStream.Builder#setLevel(int)
     * @see <a href="https://github.com/facebook/zstd/blob/dev/lib/zstd.h">zstd.h</a>
     */
    public static final int ZSTD_CLEVEL_MIN = Zstd.minCompressionLevel();

    /**
     * Maximum hash log value.
     *
     * <p>
     * <small>This constant name matches the name in the C header file.</small>
     * </p>
     *
     * @see ZstdCompressorOutputStream.Builder#setHashLog(int)
     * @see <a href="https://github.com/facebook/zstd/blob/dev/lib/zstd.h">zstd.h</a>
     */
    public static final int ZSTD_HASHLOG_MAX = Zstd.hashLogMax();

    /**
     * Minimum hash log value.
     *
     * <p>
     * <small>This constant name matches the name in the C header file.</small>
     * </p>
     *
     * @see ZstdCompressorOutputStream.Builder#setHashLog(int)
     * @see <a href="https://github.com/facebook/zstd/blob/dev/lib/zstd.h">zstd.h</a>
     */
    public static final int ZSTD_HASHLOG_MIN = Zstd.hashLogMin();

    /**
     * {@code ZSTD_MINMATCH_MAX} = {@value}. Only for ZSTD_fast, other strategies are limited to 6.
     *
     * <p>
     * <small>This constant name matches the name in the C header file.</small>
     * </p>
     *
     * @see ZstdCompressorOutputStream.Builder#setMinMatch(int)
     * @see <a href="https://github.com/facebook/zstd/blob/dev/lib/zstd.h">zstd.h</a>
     */
    public static final int ZSTD_MINMATCH_MAX = 7;

    /**
     * {@code ZSTD_MINMATCH_MAX} = {@value}. Only for ZSTD_btopt+, faster strategies are limited to 4.
     */
    public static final int ZSTD_MINMATCH_MIN = 3;

    /**
     * Maximum search log value.
     *
     * <p>
     * <small>This constant name matches the name in the C header file.</small>
     * </p>
     *
     * @see ZstdCompressorOutputStream.Builder#setSearchLog(int)
     * @see <a href="https://github.com/facebook/zstd/blob/dev/lib/zstd.h">zstd.h</a>
     */
    public static final int ZSTD_SEARCHLOG_MAX = Zstd.searchLogMax();

    /**
     * Minimum search log value.
     *
     * <p>
     * <small>This constant name matches the name in the C header file.</small>
     * </p>
     *
     * @see ZstdCompressorOutputStream.Builder#setSearchLog(int)
     * @see <a href="https://github.com/facebook/zstd/blob/dev/lib/zstd.h">zstd.h</a>
     */
    public static final int ZSTD_SEARCHLOG_MIN = Zstd.searchLogMin();

    /**
     * {@code ZSTD_WINDOWLOG_LIMIT_DEFAULT} = {@value}.
     * <p>
     * By default, the streaming decoder will refuse any frame requiring larger than (in C) {@code (1 << ZSTD_WINDOWLOG_LIMIT_DEFAULT)} window size, to preserve
     * host's memory from unreasonable requirements.
     * </p>
     *
     * <p>
     * <small>This constant name matches the name in the C header file.</small>
     * </p>
     *
     * @see ZstdCompressorOutputStream.Builder#setMinMatch(int)
     * @see <a href="https://github.com/facebook/zstd/blob/dev/lib/zstd.h">zstd.h</a>
     */
    public static final int ZSTD_WINDOWLOG_LIMIT_DEFAULT = 27;

    /**
     * Maximum window log value.
     *
     * <p>
     * <small>This constant name matches the name in the C header file.</small>
     * </p>
     *
     * @see ZstdCompressorOutputStream.Builder#setWindowLog(int)
     * @see <a href="https://github.com/facebook/zstd/blob/dev/lib/zstd.h">zstd.h</a>
     */
    public static final int ZSTD_WINDOWLOG_MAX = Zstd.windowLogMax();

    /**
     * Minimum window log value.
     *
     * <p>
     * <small>This constant name matches the name in the C header file.</small>
     * </p>
     *
     * @see ZstdCompressorOutputStream.Builder#setWindowLog(int)
     * @see <a href="https://github.com/facebook/zstd/blob/dev/lib/zstd.h">zstd.h</a>
     */
    public static final int ZSTD_WINDOWLOG_MIN = Zstd.windowLogMin();

}
