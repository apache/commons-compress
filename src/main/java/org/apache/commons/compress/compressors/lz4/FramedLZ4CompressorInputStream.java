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
package org.apache.commons.compress.compressors.lz4;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.utils.BoundedInputStream;
import org.apache.commons.compress.utils.ByteUtils;
import org.apache.commons.compress.utils.ChecksumCalculatingInputStream;
import org.apache.commons.compress.utils.CountingInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.compress.utils.InputStreamStatistics;

/**
 * CompressorInputStream for the LZ4 frame format.
 *
 * <p>Based on the "spec" in the version "1.5.1 (31/03/2015)"</p>
 *
 * @see <a href="http://lz4.github.io/lz4/lz4_Frame_format.html">LZ4 Frame Format Description</a>
 * @since 1.14
 * @NotThreadSafe
 */
public class FramedLZ4CompressorInputStream extends CompressorInputStream
    implements InputStreamStatistics {

    // used by FramedLZ4CompressorOutputStream as well
    static final byte[] LZ4_SIGNATURE = { //NOSONAR
        4, 0x22, 0x4d, 0x18
    };
    private static final byte[] SKIPPABLE_FRAME_TRAILER = {
        0x2a, 0x4d, 0x18
    };
    private static final byte SKIPPABLE_FRAME_PREFIX_BYTE_MASK = 0x50;

    static final int VERSION_MASK = 0xC0;
    static final int SUPPORTED_VERSION = 0x40;
    static final int BLOCK_INDEPENDENCE_MASK = 0x20;
    static final int BLOCK_CHECKSUM_MASK = 0x10;
    static final int CONTENT_SIZE_MASK = 0x08;
    static final int CONTENT_CHECKSUM_MASK = 0x04;
    static final int BLOCK_MAX_SIZE_MASK = 0x70;
    static final int UNCOMPRESSED_FLAG_MASK = 0x80000000;

    private static boolean isSkippableFrameSignature(final byte[] b) {
        if ((b[0] & SKIPPABLE_FRAME_PREFIX_BYTE_MASK) != SKIPPABLE_FRAME_PREFIX_BYTE_MASK) {
            return false;
        }
        for (int i = 1; i < 4; i++) {
            if (b[i] != SKIPPABLE_FRAME_TRAILER[i - 1]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the signature matches what is expected for a .lz4 file.
     *
     * <p>.lz4 files start with a four byte signature.</p>
     *
     * @param signature the bytes to check
     * @param length    the number of bytes to check
     * @return          true if this is a .sz stream, false otherwise
     */
    public static boolean matches(final byte[] signature, final int length) {

        if (length < LZ4_SIGNATURE.length) {
            return false;
        }

        byte[] shortenedSig = signature;
        if (signature.length > LZ4_SIGNATURE.length) {
            shortenedSig = Arrays.copyOf(signature, LZ4_SIGNATURE.length);
        }

        return Arrays.equals(shortenedSig, LZ4_SIGNATURE);
    }

    // used in no-arg read method
    private final byte[] oneByte = new byte[1];
    private final ByteUtils.ByteSupplier supplier = this::readOneByte;

    private final CountingInputStream inputStream;
    private final boolean decompressConcatenated;
    private boolean expectBlockChecksum;
    private boolean expectBlockDependency;

    private boolean expectContentSize;
    private boolean expectContentChecksum;

    private InputStream currentBlock;

    private boolean endReached, inUncompressed;

    // used for frame header checksum and content checksum, if present
    private final XXHash32 contentHash = new XXHash32();

    // used for block checksum, if present
    private final XXHash32 blockHash = new XXHash32();

    // only created if the frame doesn't set the block independence flag
    private byte[] blockDependencyBuffer;

    /**
     * Creates a new input stream that decompresses streams compressed
     * using the LZ4 frame format and stops after decompressing the
     * first frame.
     * @param in  the InputStream from which to read the compressed data
     * @throws IOException if reading fails
     */
    public FramedLZ4CompressorInputStream(final InputStream in) throws IOException {
        this(in, false);
    }

    /**
     * Creates a new input stream that decompresses streams compressed
     * using the LZ4 frame format.
     * @param in  the InputStream from which to read the compressed data
     * @param decompressConcatenated if true, decompress until the end
     *          of the input; if false, stop after the first LZ4 frame
     *          and leave the input position to point to the next byte
     *          after the frame stream
     * @throws IOException if reading fails
     */
    public FramedLZ4CompressorInputStream(final InputStream in, final boolean decompressConcatenated) throws IOException {
        this.inputStream = new CountingInputStream(in);
        this.decompressConcatenated = decompressConcatenated;
        init(true);
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
        }
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        try {
            if (currentBlock != null) {
                currentBlock.close();
                currentBlock = null;
            }
        } finally {
            inputStream.close();
        }
    }

    /**
     * @since 1.17
     */
    @Override
    public long getCompressedCount() {
        return inputStream.getBytesRead();
    }

    private void init(final boolean firstFrame) throws IOException {
        if (readSignature(firstFrame)) {
            readFrameDescriptor();
            nextBlock();
        }
    }

    private void maybeFinishCurrentBlock() throws IOException {
        if (currentBlock != null) {
            currentBlock.close();
            currentBlock = null;
            if (expectBlockChecksum) {
                verifyChecksum(blockHash, "block");
                blockHash.reset();
            }
        }
    }

    private void nextBlock() throws IOException {
        maybeFinishCurrentBlock();
        final long len = ByteUtils.fromLittleEndian(supplier, 4);
        final boolean uncompressed = (len & UNCOMPRESSED_FLAG_MASK) != 0;
        final int realLen = (int) (len & (~UNCOMPRESSED_FLAG_MASK));
        if (realLen == 0) {
            verifyContentChecksum();
            if (!decompressConcatenated) {
                endReached = true;
            } else {
                init(false);
            }
            return;
        }
        InputStream capped = new BoundedInputStream(inputStream, realLen);
        if (expectBlockChecksum) {
            capped = new ChecksumCalculatingInputStream(blockHash, capped);
        }
        if (uncompressed) {
            inUncompressed = true;
            currentBlock = capped;
        } else {
            inUncompressed = false;
            final BlockLZ4CompressorInputStream s = new BlockLZ4CompressorInputStream(capped);
            if (expectBlockDependency) {
                s.prefill(blockDependencyBuffer);
            }
            currentBlock = s;
        }
    }

    /** {@inheritDoc} */
    @Override
    public int read() throws IOException {
        return read(oneByte, 0, 1) == -1 ? -1 : oneByte[0] & 0xFF;
    }

    /** {@inheritDoc} */
    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        if (len == 0) {
            return 0;
        }
        if (endReached) {
            return -1;
        }
        int r = readOnce(b, off, len);
        if (r == -1) {
            nextBlock();
            if (!endReached) {
                r = readOnce(b, off, len);
            }
        }
        if (r != -1) {
            if (expectBlockDependency) {
                appendToBlockDependencyBuffer(b, off, r);
            }
            if (expectContentChecksum) {
                contentHash.update(b, off, r);
            }
        }
        return r;
    }

    private void readFrameDescriptor() throws IOException {
        final int flags = readOneByte();
        if (flags == -1) {
            throw new IOException("Premature end of stream while reading frame flags");
        }
        contentHash.update(flags);
        if ((flags & VERSION_MASK) != SUPPORTED_VERSION) {
            throw new IOException("Unsupported version " + (flags >> 6));
        }
        expectBlockDependency = (flags & BLOCK_INDEPENDENCE_MASK) == 0;
        if (expectBlockDependency) {
            if (blockDependencyBuffer == null) {
                blockDependencyBuffer = new byte[BlockLZ4CompressorInputStream.WINDOW_SIZE];
            }
        } else {
            blockDependencyBuffer = null;
        }
        expectBlockChecksum = (flags & BLOCK_CHECKSUM_MASK) != 0;
        expectContentSize = (flags & CONTENT_SIZE_MASK) != 0;
        expectContentChecksum = (flags & CONTENT_CHECKSUM_MASK) != 0;
        final int bdByte = readOneByte();
        if (bdByte == -1) { // max size is irrelevant for this implementation
            throw new IOException("Premature end of stream while reading frame BD byte");
        }
        contentHash.update(bdByte);
        if (expectContentSize) { // for now we don't care, contains the uncompressed size
            final byte[] contentSize = new byte[8];
            final int skipped = IOUtils.readFully(inputStream, contentSize);
            count(skipped);
            if (8 != skipped) {
                throw new IOException("Premature end of stream while reading content size");
            }
            contentHash.update(contentSize, 0, contentSize.length);
        }
        final int headerHash = readOneByte();
        if (headerHash == -1) { // partial hash of header.
            throw new IOException("Premature end of stream while reading frame header checksum");
        }
        final int expectedHash = (int) ((contentHash.getValue() >> 8) & 0xff);
        contentHash.reset();
        if (headerHash != expectedHash) {
            throw new IOException("Frame header checksum mismatch");
        }
    }

    private int readOnce(final byte[] b, final int off, final int len) throws IOException {
        if (inUncompressed) {
            final int cnt = currentBlock.read(b, off, len);
            count(cnt);
            return cnt;
        }
        final BlockLZ4CompressorInputStream l = (BlockLZ4CompressorInputStream) currentBlock;
        final long before = l.getBytesRead();
        final int cnt = currentBlock.read(b, off, len);
        count(l.getBytesRead() - before);
        return cnt;
    }

    private int readOneByte() throws IOException {
        final int b = inputStream.read();
        if (b != -1) {
            count(1);
            return b & 0xFF;
        }
        return -1;
    }

    private boolean readSignature(final boolean firstFrame) throws IOException {
        final String garbageMessage = firstFrame ? "Not a LZ4 frame stream" : "LZ4 frame stream followed by garbage";
        final byte[] b = new byte[4];
        int read = IOUtils.readFully(inputStream, b);
        count(read);
        if (0 == read && !firstFrame) {
            // good LZ4 frame and nothing after it
            endReached = true;
            return false;
        }
        if (4 != read) {
            throw new IOException(garbageMessage);
        }

        read = skipSkippableFrame(b);
        if (0 == read && !firstFrame) {
            // good LZ4 frame with only some skippable frames after it
            endReached = true;
            return false;
        }
        if (4 != read || !matches(b, 4)) {
            throw new IOException(garbageMessage);
        }
        return true;
    }

    /**
     * Skips over the contents of a skippable frame as well as
     * skippable frames following it.
     *
     * <p>It then tries to read four more bytes which are supposed to
     * hold an LZ4 signature and returns the number of bytes read
     * while storing the bytes in the given array.</p>
     */
    private int skipSkippableFrame(final byte[] b) throws IOException {
        int read = 4;
        while (read == 4 && isSkippableFrameSignature(b)) {
            final long len = ByteUtils.fromLittleEndian(supplier, 4);
            if (len < 0) {
                throw new IOException("Found illegal skippable frame with negative size");
            }
            final long skipped = IOUtils.skip(inputStream, len);
            count(skipped);
            if (len != skipped) {
                throw new IOException("Premature end of stream while skipping frame");
            }
            read = IOUtils.readFully(inputStream, b);
            count(read);
        }
        return read;
    }

    private void verifyChecksum(final XXHash32 hash, final String kind) throws IOException {
        final byte[] checksum = new byte[4];
        final int read = IOUtils.readFully(inputStream, checksum);
        count(read);
        if (4 != read) {
            throw new IOException("Premature end of stream while reading " + kind + " checksum");
        }
        final long expectedHash = hash.getValue();
        if (expectedHash != ByteUtils.fromLittleEndian(checksum)) {
            throw new IOException(kind + " checksum mismatch.");
        }
    }

    private void verifyContentChecksum() throws IOException {
        if (expectContentChecksum) {
            verifyChecksum(contentHash, "content");
        }
        contentHash.reset();
    }
}
