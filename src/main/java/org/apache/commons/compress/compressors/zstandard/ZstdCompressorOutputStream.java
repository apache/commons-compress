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

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.io.build.AbstractStreamBuilder;
import org.apache.commons.lang3.ArrayUtils;

import com.github.luben.zstd.ZstdOutputStream;

/**
 * {@link CompressorOutputStream} implementation to create Zstandard encoded stream.
 * <p>
 * This class avoids making the underlying {@code zstd} classes part of the public or protected API. The underlying implementation is provided through the
 * <a href="https://github.com/luben/zstd-jni/">Zstandard JNI</a> library which is based on <a href="https://github.com/facebook/zstd/">zstd</a>.
 * </p>
 *
 * @see <a href="https://github.com/luben/zstd-jni/">Zstandard JNI</a>
 * @see <a href="https://github.com/facebook/zstd/">zstd</a>
 * @since 1.16
 */
public class ZstdCompressorOutputStream extends CompressorOutputStream<ZstdOutputStream> {

    // @formatter:off
    /**
     * Builds a new {@link ZstdCompressorOutputStream}.
     *
     * <p>
     * For example:
     * </p>
     * <pre>{@code
     * ZstdCompressorOutputStream s = ZstdCompressorOutputStream.builder()
     *   .setPath(path)
     *   .setLevel(3)
     *   .setStrategy(0)
     *   .setWorkers(0)
     *   .get();
     * }
     * </pre>
     * <p>
     * This class avoids making the underlying {@code zstd} classes part of the public or protected API.
     * </p>
     * @see #get()
     * @see ZstdConstants
     * @since 1.28.0
     */
    // @formatter:on
    public static final class Builder extends AbstractStreamBuilder<ZstdCompressorOutputStream, Builder> {

        private int chainLog;
        private boolean checksum;
        private boolean closeFrameOnFlush;
        private byte[] dict;
        private int hashLog;
        private int jobSize;
        private int level = ZstdConstants.ZSTD_CLEVEL_DEFAULT;
        private int minMatch;
        private int overlapLog;
        private int searchLog;
        private int strategy;
        private int targetLength;
        private int windowLog;
        private int workers;

        /**
         * Constructs a new builder of {@link ZstdCompressorOutputStream}.
         */
        public Builder() {
            // empty
        }

        @Override
        public ZstdCompressorOutputStream get() throws IOException {
            return new ZstdCompressorOutputStream(this);
        }

        /**
         * Sets the size of the multi-probe search table, as a power of 2.
         * <p>
         * The value {@code 0} means use the default chainLog.
         * </p>
         * <p>
         * The resulting memory usage is (in C) {@code (1 << (chainLog + 2))}. The input must be between {@link ZstdConstants#ZSTD_CHAINLOG_MIN} and
         * {@link ZstdConstants#ZSTD_CHAINLOG_MAX}. A larger tables result in better and slower compression. This parameter is useless for "fast" strategy but
         * still useful when using "dfast" strategy, in which case it defines a secondary probe table.
         * </p>
         *
         * @param chainLog the size of the multi-probe search table, as a power of 2.
         * @return this instance.
         * @see ZstdConstants#ZSTD_CHAINLOG_MIN
         * @see ZstdConstants#ZSTD_CHAINLOG_MAX
         * @see <a href="https://facebook.github.io/zstd/zstd_manual.html#Chapter5">Zstd manual Chapter5</a>
         * @see <a href="https://github.com/facebook/zstd/blob/dev/lib/zstd.h">zstd.h</a>
         */
        public Builder setChainLog(final int chainLog) {
            this.chainLog = chainLog;
            return this;
        }

        /**
         * Sets whether a 32-bits checksum of content is written at end of frame (defaults to {@code false}).
         * <p>
         * The value {@code false} means no checksum.
         * </p>
         *
         * @param checksum Whether a 32-bits checksum of content is written at end of frame.
         * @return this instance.
         * @see <a href="https://facebook.github.io/zstd/zstd_manual.html#Chapter5">Zstd manual Chapter5</a>
         * @see <a href="https://github.com/facebook/zstd/blob/dev/lib/zstd.h">zstd.h</a>
         */
        public Builder setChecksum(final boolean checksum) {
            this.checksum = checksum;
            return this;
        }

        /**
         * Sets whether to close the frame on flush.
         * <p>
         * This will guarantee that it can be ready fully if the process crashes before closing the stream. The downside is that this negatively affects the
         * compression ratio.
         * </p>
         * <p>
         * The value {@code false} means don't close on flush.
         * </p>
         *
         * @param closeFrameOnFlush whether to close the frame on flush.
         * @return this instance.
         * @see <a href="https://facebook.github.io/zstd/zstd_manual.html#Chapter5">Zstd manual Chapter5</a>
         * @see <a href="https://github.com/facebook/zstd/blob/dev/lib/zstd.h">zstd.h</a>
         */
        public Builder setCloseFrameOnFlush(final boolean closeFrameOnFlush) {
            this.closeFrameOnFlush = closeFrameOnFlush;
            return this;
        }

        /**
         * Sets an internal {@code CDict} from the given {@code dict} buffer.
         * <p>
         * Decompression will have to use same dictionary.
         * </p>
         * <strong>Using a dictionary</strong>
         * <ul>
         * <li>Loading a null (or 0-length) dictionary invalidates the previous dictionary, returning to no-dictionary mode.</li>
         * <li>A dictionary is sticky, it will be used for all future compressed frames. To return to the no-dictionary mode, load a null dictionary.</li>
         * <li>Loading a dictionary builds tables. This is a CPU consuming operation, with non-negligible impact on latency. Tables are dependent on compression
         * parameters, and for this reason, compression parameters can no longer be changed after loading a dictionary.</li>
         * <li>The dictionary content will be copied internally.</li>
         * </ul>
         *
         * @param dict The dictionary buffer.
         * @return this instance.
         * @see <a href="https://facebook.github.io/zstd/zstd_manual.html#Chapter12">Zstd manual Chapter12</a>
         * @see <a href="https://github.com/facebook/zstd/blob/dev/lib/zstd.h">zstd.h</a>
         */
        public Builder setDict(final byte[] dict) {
            this.dict = dict;
            return this;
        }

        /**
         * Size of the initial probe table, as a power of 2.
         * <p>
         * The value {@code 0} means "use default hashLog".
         * </p>
         * <p>
         * The resulting memory usage is (in C) {@code (1 << (hashLog + 2))}. This value must be between {@link ZstdConstants#ZSTD_HASHLOG_MIN} and
         * {@link ZstdConstants#ZSTD_HASHLOG_MAX}. Using a larger table improves the compression ratio of strategies &lt;= dFast, and improves speed of
         * strategies &gt; dFast.
         * </p>
         *
         * @param hashLog Size of the initial probe table, as a power of 2.
         * @return this instance.
         * @see ZstdConstants#ZSTD_HASHLOG_MIN
         * @see ZstdConstants#ZSTD_HASHLOG_MAX
         * @see <a href="https://facebook.github.io/zstd/zstd_manual.html#Chapter5">Zstd manual Chapter5</a>
         * @see <a href="https://github.com/facebook/zstd/blob/dev/lib/zstd.h">zstd.h</a>
         */
        public Builder setHashLog(final int hashLog) {
            this.hashLog = hashLog;
            return this;
        }

        /**
         * Size of a compression job.
         * <p>
         * This value is enforced only when {@code workers >= 1}. Each compression job is completed in parallel, so this value can indirectly impact the number
         * of active threads. A value of 0 uses a default behavior, which is dynamically determined based on compression parameters. Job size must be a minimum
         * of overlap size, or <a href="https://github.com/facebook/zstd/blob/dev/lib/compress/zstdmt_compress.h">ZSTDMT_JOBSIZE_MIN (= 512 KB)</a>, whichever
         * is largest. The minimum size is automatically and transparently enforced.
         * </p>
         * <p>
         * This is a multi-threading parameters and is only active if multi-threading is enabled ( if the underlying native library is compiled with the build
         * macro {@code ZSTD_MULTITHREAD}).
         * </p>
         *
         * @param jobSize Size of a compression job.
         * @return this instance.
         * @see <a href="https://facebook.github.io/zstd/zstd_manual.html#Chapter5">Zstd manual Chapter5</a>
         * @see <a href="https://github.com/facebook/zstd/blob/dev/lib/compress/zstdmt_compress.h">zstdmt_compress.h</a>
         */
        public Builder setJobSize(final int jobSize) {
            this.jobSize = jobSize;
            return this;
        }

        /**
         * Sets compression parameters according to a pre-defined {@code cLevel} table, from 0 to 9.
         * <p>
         * The exact compression parameters are dynamically determined, depending on both compression level and srcSize (when known). The default level is
         * {@link ZstdConstants#ZSTD_CLEVEL_DEFAULT}. The special value 0 means default, which is controlled by {@link ZstdConstants#ZSTD_CLEVEL_DEFAULT}.
         * </p>
         * <ul>
         * <li>The value 0 means use the default, which is controlled by {@link ZstdConstants#ZSTD_CLEVEL_DEFAULT}</li>
         * <li>You may pass a negative compression level.</li>
         * <li>Setting a level does not automatically set all other compression parameters to defaults. Setting this value will eventually dynamically impact
         * the compression parameters which have not been manually set. The manually set values are used.</li>
         * </ul>
         *
         * @param level The compression level, from 0 to 9, where the default is {@link ZstdConstants#ZSTD_CLEVEL_DEFAULT}.
         * @return this instance
         * @see ZstdConstants#ZSTD_CLEVEL_DEFAULT
         * @see ZstdConstants#ZSTD_CLEVEL_MIN
         * @see ZstdConstants#ZSTD_CLEVEL_MAX
         * @see <a href="https://facebook.github.io/zstd/zstd_manual.html#Chapter5">Zstd manual Chapter5</a>
         * @see <a href="https://github.com/facebook/zstd/blob/dev/lib/zstd.h">zstd.h</a>
         */
        public Builder setLevel(final int level) {
            this.level = level;
            return this;
        }

        /**
         * Sets minimum match size for long distance matcher.
         * <p>
         * Zstd can still find matches of smaller size, by updating its search algorithm to look for this size and larger. Using larger values increase
         * compression and decompression speed, but decrease the ratio. The value must be between {@link ZstdConstants#ZSTD_MINMATCH_MIN} and
         * {@link ZstdConstants#ZSTD_MINMATCH_MAX}. Note that currently, for all strategies &lt; {@code btopt}, effective minimum is {@code 4},
         * for all strategies &gt; {@code fast}, effective maximum is {@code 6}.
         * </p>
         * <p>
         * The value {@code 0} means use the default minMatchLength.
         * </p>
         *
         * @param minMatch minimum match size for long distance matcher.
         * @return this instance.
         * @see <a href="https://facebook.github.io/zstd/zstd_manual.html#Chapter5">Zstd manual Chapter5</a>
         * @see <a href="https://github.com/facebook/zstd/blob/dev/lib/zstd.h">zstd.h</a>
         */
        public Builder setMinMatch(final int minMatch) {
            this.minMatch = minMatch;
            return this;
        }

        /**
         * Sets the overlap size, as a fraction of window size.
         * <p>
         * The overlap size is an amount of data reloaded from previous job at the beginning of a new job. It helps preserve compression ratio, while each job
         * is compressed in parallel. This value is enforced only when workers &gt;= 1. Larger values increase compression ratio, but decrease speed. Possible
         * values range from 0 to 9:
         * </p>
         * <ul>
         * <li>0 means "default" : value will be determined by the library, depending on strategy</li>
         * <li>1 means "no overlap"</li>
         * <li>9 means "full overlap", using a full window size.</li>
         * </ul>
         * <p>
         * Each intermediate rank increases/decreases the load size by a factor 2:
         * </p>
         * <ul>
         * <li>9: full window</li>
         * <li>8: w / 2</li>
         * <li>7: w / 4</li>
         * <li>6: w / 8</li>
         * <li>5: w / 16</li>
         * <li>4: w / 32</li>
         * <li>3: w / 64</li>
         * <li>2: w / 128</li>
         * <li>1: no overlap</li>
         * <li>0: default
         * </ul>
         * <p>
         * The default value varies between 6 and 9, depending on the strategy.
         * </p>
         * <p>
         * This is a multi-threading parameters and is only active if multi-threading is enabled ( if the underlying native library is compiled with the build
         * macro {@code ZSTD_MULTITHREAD}).
         * </p>
         *
         * @param overlapLog the overlap size, as a fraction of window size.
         * @return this instance.
         * @see <a href="https://facebook.github.io/zstd/zstd_manual.html#Chapter5">Zstd manual Chapter5</a>
         * @see <a href="https://github.com/facebook/zstd/blob/dev/lib/zstd.h">zstd.h</a>
         */
        public Builder setOverlapLog(final int overlapLog) {
            this.overlapLog = overlapLog;
            return this;
        }

        /**
         * Sets number of search attempts, as a power of 2.
         * <p>
         * More attempts result in better and slower compression. This parameter is useless for "fast" and "dFast" strategies.
         * </p>
         * <p>
         * The value {@code 0} means use the default searchLog.
         * </p>
         *
         * @param searchLog number of search attempts, as a power of 2.
         * @return this instance.
         * @see ZstdConstants#ZSTD_SEARCHLOG_MIN
         * @see ZstdConstants#ZSTD_SEARCHLOG_MAX
         * @see <a href="https://facebook.github.io/zstd/zstd_manual.html#Chapter5">Zstd manual Chapter5</a>
         * @see <a href="https://github.com/facebook/zstd/blob/dev/lib/zstd.h">zstd.h</a>
         */
        public Builder setSearchLog(final int searchLog) {
            this.searchLog = searchLog;
            return this;
        }

        /**
         * Sets the {@code ZSTD_strategy} from the C enum definition.
         * <p>
         * The higher the value of selected strategy, the more complex it is, resulting in stronger and slower compression.
         * </p>
         * <p>
         * The value {@code 0} means use the default strategy.
         * </p>
         * <ul>
         * <li>{@code ZSTD_fast = 1}</li>
         * <li>{@code ZSTD_dfast = 2}</li>
         * <li>{@code ZSTD_greedy = 3}</li>
         * <li>{@code ZSTD_lazy = 4}</li>
         * <li>{@code ZSTD_lazy2 = 5}</li>
         * <li>{@code ZSTD_btlazy2 = 6}</li>
         * <li>{@code ZSTD_btopt = 7}</li>
         * <li>{@code ZSTD_btultra = 8}</li>
         * <li>{@code ZSTD_btultra2 = 9}</li>
         * </ul>
         *
         * @param strategy the {@code ZSTD_strategy} from the C enum definition.
         * @return this instance.
         * @see <a href="https://facebook.github.io/zstd/zstd_manual.html#Chapter5">Zstd manual Chapter5</a>
         * @see <a href="https://github.com/facebook/zstd/blob/dev/lib/zstd.h">zstd.h</a>
         */
        public Builder setStrategy(final int strategy) {
            this.strategy = strategy;
            return this;
        }

        /**
         * Sets a value that depends on the strategy, see {@code ZSTD_c_targetLength}.
         * <p>
         * For strategies {@code btopt}, {@code btultra} and {@code btultra2}:
         * </p>
         * <ul>
         * <li>Length of Match considered "good enough" to stop search.</li>
         * <li>Larger values make compression stronger, and slower.</li>
         * </ul>
         * <p>
         * For strategy {@code fast}:
         * </p>
         * <ul>
         * <li>Distance between match sampling.</li>
         * <li>Larger values make compression faster, and weaker.</li>
         * </ul>
         * <p>
         * The value {@code 0} means use the default targetLength.
         * </p>
         *
         * @param targetLength a value that depends on the strategy, see {@code ZSTD_c_targetLength}.
         * @return this instance.
         * @see <a href="https://facebook.github.io/zstd/zstd_manual.html#Chapter5">Zstd manual Chapter5</a>
         * @see <a href="https://github.com/facebook/zstd/blob/dev/lib/zstd.h">zstd.h</a>
         */
        public Builder setTargetLength(final int targetLength) {
            this.targetLength = targetLength;
            return this;
        }

        /**
         * Sets maximum allowed back-reference distance, expressed as power of 2.
         * <p>
         * This will set a memory budget for streaming decompression, with larger values requiring more memory and typically compressing more. This value be
         * between {@link ZstdConstants#ZSTD_WINDOWLOG_MIN} and {@link ZstdConstants#ZSTD_WINDOWLOG_MAX}.
         * </p>
         * <p>
         * <strong>Note</strong>: Using a windowLog greater than {@link ZstdConstants#ZSTD_WINDOWLOG_LIMIT_DEFAULT} requires explicitly allowing such size at
         * streaming decompression stage.
         * </p>
         * <p>
         * The value {@code 0} means use the default windowLog.
         * </p>
         *
         * @param windowLog maximum allowed back-reference distance, expressed as power of 2.
         * @return this instance.
         * @see ZstdConstants#ZSTD_WINDOWLOG_MIN
         * @see ZstdConstants#ZSTD_WINDOWLOG_MAX
         * @see <a href="https://facebook.github.io/zstd/zstd_manual.html#Chapter5">Zstd manual Chapter5</a>
         * @see <a href="https://github.com/facebook/zstd/blob/dev/lib/zstd.h">zstd.h</a>
         */
        public Builder setWindowLog(final int windowLog) {
            this.windowLog = windowLog;
            return this;
        }

        /**
         * Sets how many threads will be spawned to compress in parallel.
         * <p>
         * When workers &gt;= 1, this triggers asynchronous mode when compressing which consumes input and flushes output if possible, but immediately gives
         * back control to the caller, while compression is performed in parallel, within worker threads. More workers improve speed, but also increase memory
         * usage. Compression is performed from the calling thread, and all invocations are blocking.
         * </p>
         * <p>
         * The value {@code 0} means "single-threaded mode", nothing is spawned.
         * </p>
         * <p>
         * This is a multi-threading parameters and is only active if multi-threading is enabled ( if the underlying native library is compiled with the build
         * macro {@code ZSTD_MULTITHREAD}).
         * </p>
         *
         * @param workers How many threads will be spawned to compress in parallel.
         * @return this instance.
         * @see <a href="https://facebook.github.io/zstd/zstd_manual.html#Chapter5">Zstd manual Chapter5</a>
         * @see <a href="https://github.com/facebook/zstd/blob/dev/lib/zstd.h">zstd.h</a>
         */
        public Builder setWorkers(final int workers) {
            this.workers = workers;
            return this;
        }
    }

    /**
     * Constructs a new builder of {@link ZstdCompressorOutputStream}.
     *
     * @return a new builder of {@link ZstdCompressorOutputStream}.
     * @since 1.28.0
     */
    public static Builder builder() {
        return new Builder();
    }

    @SuppressWarnings("resource") // Caller closes
    private static ZstdOutputStream toZstdOutputStream(final Builder builder) throws IOException {
        final OutputStream outputStream = builder.getOutputStream();
        if (outputStream instanceof ZstdOutputStream) {
            // Builder properties are not applied when a ZstdOutputStream is provided.
            return (ZstdOutputStream) outputStream;
        }
        // @formatter:off
        return new ZstdOutputStream(outputStream)
            .setChainLog(builder.chainLog)
            .setChecksum(builder.checksum)
            .setCloseFrameOnFlush(builder.closeFrameOnFlush)
            .setDict(builder.dict != null ? builder.dict : ArrayUtils.EMPTY_BYTE_ARRAY)
            .setHashLog(builder.hashLog)
            .setJobSize(builder.jobSize)
            .setLevel(builder.level)
            .setMinMatch(builder.minMatch)
            .setOverlapLog(builder.overlapLog)
            .setSearchLog(builder.searchLog)
            .setStrategy(builder.strategy)
            .setTargetLength(builder.targetLength)
            .setWindowLog(builder.windowLog)
            .setWorkers(builder.workers);
        // @formatter:on
    }

    @SuppressWarnings("resource") // Caller closes
    private ZstdCompressorOutputStream(final Builder builder) throws IOException {
        super(toZstdOutputStream(builder));
    }

    /**
     * Constructs a new instance using default Zstd parameter values.
     *
     * @param outStream the output stream.
     * @throws IOException if an I/O error occurs.
     */
    public ZstdCompressorOutputStream(final OutputStream outStream) throws IOException {
        this(builder().setOutputStream(outStream));
    }

    /**
     * Constructs a new instance using default Zstd parameter values plus a compression level.
     *
     * @param outStream the output stream.
     * @param level     The compression level, from 0 to 9, where the default is {@link ZstdConstants#ZSTD_CLEVEL_DEFAULT}.
     * @throws IOException if an I/O error occurs.
     * @since 1.18
     * @deprecated Use {@link #builder()}.
     */
    @Deprecated
    public ZstdCompressorOutputStream(final OutputStream outStream, final int level) throws IOException {
        this(builder().setOutputStream(outStream).setLevel(level));
    }

    /**
     * Constructs a new instance using default Zstd parameter values plus a compression level and checksum setting.
     *
     * @param outStream         the output stream.
     * @param level             The compression level, from 0 to 9, where the default is {@link ZstdConstants#ZSTD_CLEVEL_DEFAULT}.
     * @param closeFrameOnFlush whether to close the frame on flush.
     * @throws IOException if an I/O error occurs.
     * @since 1.18
     * @deprecated Use {@link #builder()}.
     */
    @Deprecated
    public ZstdCompressorOutputStream(final OutputStream outStream, final int level, final boolean closeFrameOnFlush) throws IOException {
        this(builder().setOutputStream(outStream).setLevel(level).setCloseFrameOnFlush(closeFrameOnFlush));
    }

    /**
     * Constructs a new instance using default Zstd parameter values plus a compression level, closeFrameOnFlush and checksum settings.
     *
     * @param outStream         the output stream.
     * @param level             The compression level, from 0 to 9, where the default is {@link ZstdConstants#ZSTD_CLEVEL_DEFAULT}.
     * @param closeFrameOnFlush whether to close the frame on flush.
     * @param checksum          Whether a 32-bits checksum of content is written at end of frame.
     * @throws IOException if an I/O error occurs.
     * @since 1.18
     * @deprecated Use {@link #builder()}.
     */
    @Deprecated
    public ZstdCompressorOutputStream(final OutputStream outStream, final int level, final boolean closeFrameOnFlush, final boolean checksum)
            throws IOException {
        this(builder().setOutputStream(outStream).setLevel(level).setCloseFrameOnFlush(closeFrameOnFlush).setChecksum(checksum));
    }

    @Override
    public void write(final byte[] buf, final int off, final int len) throws IOException {
        out.write(buf, off, len);
    }
}
