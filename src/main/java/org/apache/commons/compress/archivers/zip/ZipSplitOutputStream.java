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

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.apache.commons.compress.utils.FileNameUtils;

/**
 * Used internally by {@link ZipArchiveOutputStream} when creating a split archive.
 *
 * @since 1.20
 */
final class ZipSplitOutputStream extends RandomAccessOutputStream {

    /**
     * 8.5.1 Capacities for split archives are as follows:
     * <p>
     * Maximum number of segments = 4,294,967,295 - 1 Maximum .ZIP segment size = 4,294,967,295 bytes (refer to section 8.5.6) Minimum segment size = 64K
     * </p>
     * <p>
     * Maximum PKSFX segment size = 2,147,483,647 bytes
     * </p>
     */
    private static final long ZIP_SEGMENT_MIN_SIZE = 64 * 1024L;
    private static final long ZIP_SEGMENT_MAX_SIZE = 4294967295L;

    private FileChannel currentChannel;
    private FileRandomAccessOutputStream outputStream;
    private Path zipFile;
    private final long splitSize;
    private long totalPosition;
    private int currentSplitSegmentIndex;
    private long currentSplitSegmentBytesWritten;
    private boolean finished;
    private final byte[] singleByte = new byte[1];
    private final List<Long> diskToPosition = new ArrayList<>();
    private final TreeMap<Long, Path> positionToFiles = new TreeMap<>();

    /**
     * Creates a split ZIP. If the ZIP file is smaller than the split size, then there will only be one split ZIP, and its suffix is .zip, otherwise the split
     * segments should be like .z01, .z02, ... .z(N-1), .zip
     *
     * @param zipFile   the ZIP file to write to
     * @param splitSize the split size
     * @throws IllegalArgumentException if arguments are illegal: Zip split segment size should between 64K and 4,294,967,295.
     * @throws IOException              if an I/O error occurs
     */
    ZipSplitOutputStream(final File zipFile, final long splitSize) throws IllegalArgumentException, IOException {
        this(zipFile.toPath(), splitSize);
    }

    /**
     * Creates a split ZIP. If the ZIP file is smaller than the split size, then there will only be one split ZIP, and its suffix is .zip, otherwise the split
     * segments should be like .z01, .z02, ... .z(N-1), .zip
     *
     * @param zipFile   the path to ZIP file to write to
     * @param splitSize the split size
     * @throws IllegalArgumentException if arguments are illegal: Zip split segment size should between 64K and 4,294,967,295.
     * @throws IOException              if an I/O error occurs
     * @since 1.22
     */
    ZipSplitOutputStream(final Path zipFile, final long splitSize) throws IllegalArgumentException, IOException {
        if (splitSize < ZIP_SEGMENT_MIN_SIZE || splitSize > ZIP_SEGMENT_MAX_SIZE) {
            throw new IllegalArgumentException("Zip split segment size should between 64K and 4,294,967,295");
        }
        this.zipFile = zipFile;
        this.splitSize = splitSize;
        this.outputStream = new FileRandomAccessOutputStream(zipFile);
        this.currentChannel = this.outputStream.channel();
        this.positionToFiles.put(0L, this.zipFile);
        this.diskToPosition.add(0L);
        // write the ZIP split signature 0x08074B50 to the ZIP file
        writeZipSplitSignature();
    }

    public long calculateDiskPosition(final long disk, final long localOffset) throws IOException {
        if (disk >= Integer.MAX_VALUE) {
            throw new IOException("Disk number exceeded internal limits: limit=" + Integer.MAX_VALUE + " requested=" + disk);
        }
        return diskToPosition.get((int) disk) + localOffset;
    }

    @Override
    public void close() throws IOException {
        if (!finished) {
            finish();
        }
    }

    /**
     * Creates the new ZIP split segment, the last ZIP segment should be .zip, and the ZIP split segments' suffix should be like .z01, .z02, .z03, ... .z99,
     * .z100, ..., .z(N-1), .zip
     * <p>
     * 8.3.3 Split ZIP files are typically written to the same location and are subject to name collisions if the spanned name format is used since each segment
     * will reside on the same drive. To avoid name collisions, split archives are named as follows.
     * </p>
     * <p>
     * Segment 1 = filename.z01 Segment n-1 = filename.z(n-1) Segment n = filename.zip
     * </p>
     * <p>
     * NOTE: The ZIP split segment begin from 1,2,3,... , and we're creating a new segment, so the new segment suffix should be (currentSplitSegmentIndex + 2)
     * </p>
     *
     * @param zipSplitSegmentSuffixIndex
     * @return
     * @throws IOException if an I/O error occurs.
     */
    private Path createNewSplitSegmentFile(final Integer zipSplitSegmentSuffixIndex) throws IOException {
        final Path newFile = getSplitSegmentFileName(zipSplitSegmentSuffixIndex);

        if (Files.exists(newFile)) {
            throw new IOException("split ZIP segment " + newFile + " already exists");
        }
        return newFile;
    }

    /**
     * The last ZIP split segment's suffix should be .zip
     *
     * @throws IOException if an I/O error occurs.
     */
    private void finish() throws IOException {
        if (finished) {
            throw new IOException("This archive has already been finished");
        }

        final String zipFileBaseName = FileNameUtils.getBaseName(zipFile);
        outputStream.close();
        Files.move(zipFile, zipFile.resolveSibling(zipFileBaseName + ".zip"), StandardCopyOption.ATOMIC_MOVE);
        finished = true;
    }

    public long getCurrentSplitSegmentBytesWritten() {
        return currentSplitSegmentBytesWritten;
    }

    public int getCurrentSplitSegmentIndex() {
        return currentSplitSegmentIndex;
    }

    private Path getSplitSegmentFileName(final Integer zipSplitSegmentSuffixIndex) {
        final int newZipSplitSegmentSuffixIndex = zipSplitSegmentSuffixIndex == null ? currentSplitSegmentIndex + 2 : zipSplitSegmentSuffixIndex;
        final String baseName = FileNameUtils.getBaseName(zipFile);
        final StringBuilder extension = new StringBuilder(".z");
        if (newZipSplitSegmentSuffixIndex <= 9) {
            extension.append("0").append(newZipSplitSegmentSuffixIndex);
        } else {
            extension.append(newZipSplitSegmentSuffixIndex);
        }

        final Path parent = zipFile.getParent();
        final String dir = Objects.nonNull(parent) ? parent.toAbsolutePath().toString() : ".";
        return zipFile.getFileSystem().getPath(dir, baseName + extension.toString());
    }

    /**
     * Creates a new ZIP split segment and prepare to write to the new segment
     *
     * @throws IOException if an I/O error occurs.
     */
    private void openNewSplitSegment() throws IOException {
        Path newFile;
        if (currentSplitSegmentIndex == 0) {
            outputStream.close();
            newFile = createNewSplitSegmentFile(1);
            Files.move(zipFile, newFile, StandardCopyOption.ATOMIC_MOVE);
            this.positionToFiles.put(0L, newFile);
        }

        newFile = createNewSplitSegmentFile(null);

        outputStream.close();
        outputStream = new FileRandomAccessOutputStream(newFile);
        currentChannel = outputStream.channel();
        currentSplitSegmentBytesWritten = 0;
        zipFile = newFile;
        currentSplitSegmentIndex++;
        this.diskToPosition.add(this.totalPosition);
        this.positionToFiles.put(this.totalPosition, newFile);
    }

    @Override
    public long position() {
        return totalPosition;
    }

    /**
     * Some data cannot be written to different split segments, for example:
     * <p>
     * 4.4.1.5 The end of central directory record and the Zip64 end of central directory locator record MUST reside on the same disk when splitting or spanning
     * an archive.
     * </p>
     *
     * @param unsplittableContentSize
     * @throws IllegalArgumentException if unsplittable content size is bigger than the split segment size.
     * @throws IOException if an I/O error occurs.
     */
    public void prepareToWriteUnsplittableContent(final long unsplittableContentSize) throws IllegalArgumentException, IOException {
        if (unsplittableContentSize > this.splitSize) {
            throw new IllegalArgumentException("The unsplittable content size is bigger than the split segment size");
        }

        final long bytesRemainingInThisSegment = this.splitSize - this.currentSplitSegmentBytesWritten;
        if (bytesRemainingInThisSegment < unsplittableContentSize) {
            openNewSplitSegment();
        }
    }

    @Override
    public void write(final byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    /**
     * Writes the data to ZIP split segments, if the remaining space of current split segment is not enough, then a new split segment should be created
     *
     * @param b   data to write
     * @param off offset of the start of data in param b
     * @param len the length of data to write
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        if (len <= 0) {
            return;
        }

        if (currentSplitSegmentBytesWritten >= splitSize) {
            openNewSplitSegment();
            write(b, off, len);
        } else if (currentSplitSegmentBytesWritten + len > splitSize) {
            final int bytesToWriteForThisSegment = (int) splitSize - (int) currentSplitSegmentBytesWritten;
            write(b, off, bytesToWriteForThisSegment);
            openNewSplitSegment();
            write(b, off + bytesToWriteForThisSegment, len - bytesToWriteForThisSegment);
        } else {
            outputStream.write(b, off, len);
            currentSplitSegmentBytesWritten += len;
            totalPosition += len;
        }
    }

    @Override
    public void write(final int i) throws IOException {
        singleByte[0] = (byte) (i & 0xff);
        write(singleByte);
    }

    @Override
    public void writeFully(final byte[] b, final int off, final int len, final long atPosition) throws IOException {
        long remainingPosition = atPosition;
        for (int remainingOff = off, remainingLen = len; remainingLen > 0; ) {
            final Map.Entry<Long, Path> segment = positionToFiles.floorEntry(remainingPosition);
            final Long segmentEnd = positionToFiles.higherKey(remainingPosition);
            if (segmentEnd == null) {
                ZipIoUtil.writeAll(this.currentChannel, ByteBuffer.wrap(b, remainingOff, remainingLen), remainingPosition - segment.getKey());
                remainingPosition += remainingLen;
                remainingOff += remainingLen;
                remainingLen = 0;
            } else if (remainingPosition + remainingLen <= segmentEnd) {
                writeToSegment(segment.getValue(), remainingPosition - segment.getKey(), b, remainingOff, remainingLen);
                remainingPosition += remainingLen;
                remainingOff += remainingLen;
                remainingLen = 0;
            } else {
                final int toWrite = Math.toIntExact(segmentEnd - remainingPosition);
                writeToSegment(segment.getValue(), remainingPosition - segment.getKey(), b, remainingOff, toWrite);
                remainingPosition += toWrite;
                remainingOff += toWrite;
                remainingLen -= toWrite;
            }
        }
    }

    private void writeToSegment(
            final Path segment,
            final long position,
            final byte[] b,
            final int off,
            final int len
    ) throws IOException {
        try (FileChannel channel = FileChannel.open(segment, StandardOpenOption.WRITE)) {
            ZipIoUtil.writeAll(channel, ByteBuffer.wrap(b, off, len), position);
        }
    }

    /**
     * Writes the ZIP split signature (0x08074B50) to the head of the first ZIP split segment
     *
     * @throws IOException if an I/O error occurs.
     */
    private void writeZipSplitSignature() throws IOException {
        outputStream.write(ZipArchiveOutputStream.DD_SIG);
        currentSplitSegmentBytesWritten += ZipArchiveOutputStream.DD_SIG.length;
        totalPosition += ZipArchiveOutputStream.DD_SIG.length;
    }
}
