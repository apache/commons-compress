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
package org.apache.commons.compress.compressors.lz4;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.utils.ByteUtils;

/**
 * CompressorOutputStream for the LZ4 frame format.
 *
 * <p>
 * Based on the "spec" in the version "1.5.1 (31/03/2015)"
 * </p>
 *
 * @see <a href="https://lz4.github.io/lz4/lz4_Frame_format.html">LZ4 Frame Format Description</a>
 * @since 1.14
 * @NotThreadSafe
 */
public class FramedLZ4CompressorOutputStream extends CompressorOutputStream<OutputStream> {

    /**
     * Enumerates the block sizes supported by the format.
     */
    public enum BlockSize {

        /** Block size of 64K. */
        K64(64 * 1024, 4),

        /** Block size of 256K. */
        K256(256 * 1024, 5),

        /** Block size of 1M. */
        M1(1024 * 1024, 6),

        /** Block size of 4M. */
        M4(4096 * 1024, 7);

        private final int size;
        private final int index;

        BlockSize(final int size, final int index) {
            this.size = size;
            this.index = index;
        }

        int getIndex() {
            return index;
        }

        int getSize() {
            return size;
        }
    }

    /**
     * Parameters of the LZ4 frame format.
     */
    public static class Parameters {

        /**
         * The default parameters of 4M block size, enabled content checksum, disabled block checksums and independent blocks.
         *
         * <p>
         * This matches the defaults of the lz4 command line utility.
         * </p>
         */
        public static final Parameters DEFAULT = new Parameters(BlockSize.M4, true, false, false);
        private final BlockSize blockSize;
        private final boolean withContentChecksum;
        private final boolean withBlockChecksum;
        private final boolean withBlockDependency;

        private final org.apache.commons.compress.compressors.lz77support.Parameters lz77params;

        /**
         * Sets up custom a custom block size for the LZ4 stream but otherwise uses the defaults of enabled content checksum, disabled block checksums and
         * independent blocks.
         *
         * @param blockSize the size of a single block.
         */
        public Parameters(final BlockSize blockSize) {
            this(blockSize, true, false, false);
        }

        /**
         * Sets up custom parameters for the LZ4 stream.
         *
         * @param blockSize           the size of a single block.
         * @param withContentChecksum whether to write a content checksum
         * @param withBlockChecksum   whether to write a block checksum. Note that block checksums are not supported by the lz4 command line utility
         * @param withBlockDependency whether a block may depend on the content of a previous block. Enabling this may improve compression ratio but makes it
         *                            impossible to decompress the output in parallel.
         */
        public Parameters(final BlockSize blockSize, final boolean withContentChecksum, final boolean withBlockChecksum, final boolean withBlockDependency) {
            this(blockSize, withContentChecksum, withBlockChecksum, withBlockDependency, BlockLZ4CompressorOutputStream.createParameterBuilder().build());
        }

        /**
         * Sets up custom parameters for the LZ4 stream.
         *
         * @param blockSize           the size of a single block.
         * @param withContentChecksum whether to write a content checksum
         * @param withBlockChecksum   whether to write a block checksum. Note that block checksums are not supported by the lz4 command line utility
         * @param withBlockDependency whether a block may depend on the content of a previous block. Enabling this may improve compression ratio but makes it
         *                            impossible to decompress the output in parallel.
         * @param lz77params          parameters used to fine-tune compression, in particular to balance compression ratio vs compression speed.
         */
        public Parameters(final BlockSize blockSize, final boolean withContentChecksum, final boolean withBlockChecksum, final boolean withBlockDependency,
                final org.apache.commons.compress.compressors.lz77support.Parameters lz77params) {
            this.blockSize = blockSize;
            this.withContentChecksum = withContentChecksum;
            this.withBlockChecksum = withBlockChecksum;
            this.withBlockDependency = withBlockDependency;
            this.lz77params = lz77params;
        }

        /**
         * Sets up custom a custom block size for the LZ4 stream but otherwise uses the defaults of enabled content checksum, disabled block checksums and
         * independent blocks.
         *
         * @param blockSize  the size of a single block.
         * @param lz77params parameters used to fine-tune compression, in particular to balance compression ratio vs compression speed.
         */
        public Parameters(final BlockSize blockSize, final org.apache.commons.compress.compressors.lz77support.Parameters lz77params) {
            this(blockSize, true, false, false, lz77params);
        }

        @Override
        public String toString() {
            return "LZ4 Parameters with BlockSize " + blockSize + ", withContentChecksum " + withContentChecksum + ", withBlockChecksum " + withBlockChecksum
                    + ", withBlockDependency " + withBlockDependency;
        }
    }

    private static final byte[] END_MARK = new byte[4];
    // used in one-arg write method
    private final byte[] oneByte = new byte[1];
    private final byte[] blockData;
    private final Parameters params;

    // used for frame header checksum and content checksum, if requested
    private final org.apache.commons.codec.digest.XXHash32 contentHash = new org.apache.commons.codec.digest.XXHash32();
    // used for block checksum, if requested
    private final org.apache.commons.codec.digest.XXHash32 blockHash;

    // only created if the config requires block dependency
    private final byte[] blockDependencyBuffer;

    private int collectedBlockDependencyBytes;
    private int currentIndex;

    /**
     * Constructs a new output stream that compresses data using the LZ4 frame format using the default block size of 4MB.
     *
     * @param out the OutputStream to which to write the compressed data
     * @throws IOException if writing the signature fails
     */
    public FramedLZ4CompressorOutputStream(final OutputStream out) throws IOException {
        this(out, Parameters.DEFAULT);
    }

    /**
     * Constructs a new output stream that compresses data using the LZ4 frame format using the given block size.
     *
     * @param out    the OutputStream to which to write the compressed data
     * @param params the parameters to use
     * @throws IOException if writing the signature fails
     */
    public FramedLZ4CompressorOutputStream(final OutputStream out, final Parameters params) throws IOException {
        super(out);
        this.params = params;
        blockData = new byte[params.blockSize.getSize()];
        blockHash = params.withBlockChecksum ? new org.apache.commons.codec.digest.XXHash32() : null;
        out.write(FramedLZ4CompressorInputStream.LZ4_SIGNATURE);
        writeFrameDescriptor();
        blockDependencyBuffer = params.withBlockDependency ? new byte[BlockLZ4CompressorInputStream.WINDOW_SIZE] : null;
    }

    private void appendToBlockDependencyBuffer(final byte[] b, final int off, int len) {
        len = Math.min(len, blockDependencyBuffer.length);
        if (len > 0) {
            final int keep = blockDependencyBuffer.length - len;
            if (keep > 0) {
                // move last keep bytes towards the start of the buffer
                System.arraycopy(blockDependencyBuffer, len, blockDependencyBuffer, 0, keep);
            }
            // append new data
            System.arraycopy(b, off, blockDependencyBuffer, keep, len);
            collectedBlockDependencyBytes = Math.min(collectedBlockDependencyBytes + len, blockDependencyBuffer.length);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            finish();
        } finally {
            super.close();
        }
    }

    /**
     * Compresses all blockDataRemaining data and writes it to the stream, doesn't close the underlying stream.
     *
     * @throws IOException if an error occurs
     */
    @Override
    public void finish() throws IOException {
        if (!isFinished()) {
            flushBlock();
            writeTrailer();
            super.finish();
        }
    }

    private void flushBlock() throws IOException {
        if (currentIndex == 0) {
            return;
        }
        final boolean withBlockDependency = params.withBlockDependency;
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (BlockLZ4CompressorOutputStream o = new BlockLZ4CompressorOutputStream(baos, params.lz77params)) {
            if (withBlockDependency) {
                o.prefill(blockDependencyBuffer, blockDependencyBuffer.length - collectedBlockDependencyBytes, collectedBlockDependencyBytes);
            }
            o.write(blockData, 0, currentIndex);
        }
        if (withBlockDependency) {
            appendToBlockDependencyBuffer(blockData, 0, currentIndex);
        }
        final byte[] b = baos.toByteArray();
        if (b.length > currentIndex) { // compression increased size, maybe beyond blocksize
            ByteUtils.toLittleEndian(out, currentIndex | FramedLZ4CompressorInputStream.UNCOMPRESSED_FLAG_MASK, 4);
            out.write(blockData, 0, currentIndex);
            if (params.withBlockChecksum) {
                blockHash.update(blockData, 0, currentIndex);
            }
        } else {
            ByteUtils.toLittleEndian(out, b.length, 4);
            out.write(b);
            if (params.withBlockChecksum) {
                blockHash.update(b, 0, b.length);
            }
        }
        if (params.withBlockChecksum) {
            ByteUtils.toLittleEndian(out, blockHash.getValue(), 4);
            blockHash.reset();
        }
        currentIndex = 0;
    }

    @Override
    public void write(final byte[] data, int off, int len) throws IOException {
        if (params.withContentChecksum) {
            contentHash.update(data, off, len);
        }
        int blockDataRemaining = blockData.length - currentIndex;
        while (len > 0) {
            final int copyLen = Math.min(len, blockDataRemaining);
            System.arraycopy(data, off, blockData, currentIndex, copyLen);
            off += copyLen;
            blockDataRemaining -= copyLen;
            len -= copyLen;
            currentIndex += copyLen;
            if (blockDataRemaining == 0) {
                flushBlock();
                blockDataRemaining = blockData.length;
            }
        }
    }

    @Override
    public void write(final int b) throws IOException {
        oneByte[0] = (byte) (b & 0xff);
        write(oneByte);
    }

    private void writeFrameDescriptor() throws IOException {
        int flags = FramedLZ4CompressorInputStream.SUPPORTED_VERSION;
        if (!params.withBlockDependency) {
            flags |= FramedLZ4CompressorInputStream.BLOCK_INDEPENDENCE_MASK;
        }
        if (params.withContentChecksum) {
            flags |= FramedLZ4CompressorInputStream.CONTENT_CHECKSUM_MASK;
        }
        if (params.withBlockChecksum) {
            flags |= FramedLZ4CompressorInputStream.BLOCK_CHECKSUM_MASK;
        }
        out.write(flags);
        contentHash.update(flags);
        final int bd = params.blockSize.getIndex() << 4 & FramedLZ4CompressorInputStream.BLOCK_MAX_SIZE_MASK;
        out.write(bd);
        contentHash.update(bd);
        out.write((int) (contentHash.getValue() >> 8 & 0xff));
        contentHash.reset();
    }

    private void writeTrailer() throws IOException {
        out.write(END_MARK);
        if (params.withContentChecksum) {
            ByteUtils.toLittleEndian(out, contentHash.getValue(), 4);
        }
    }

}
