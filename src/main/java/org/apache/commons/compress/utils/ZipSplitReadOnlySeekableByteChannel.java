package org.apache.commons.compress.utils;

import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipLong;
import org.apache.commons.compress.compressors.FileNameUtil;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class ZipSplitReadOnlySeekableByteChannel extends MultiReadOnlySeekableByteChannel {
    private final int ZIP_SPLIT_SIGNATURE_LENGTH = 4;
    private ByteBuffer zipSplitSignatureByteBuffer = ByteBuffer.allocate(ZIP_SPLIT_SIGNATURE_LENGTH);

    /**
     * Concatenates the given channels.
     * the channels should be add in ascending order, e.g. z01, z02, ... z99, zip
     * please note that the .zip file is the last segment and should be added as the last one in the channels
     *
     * The first 4 bytes of split zip signature will be taken into consideration by Inflator,
     * so we don't need to skip them
     *
     * @param channels the channels to concatenate
     * @throws NullPointerException if channels is null
     */
    public ZipSplitReadOnlySeekableByteChannel(List<SeekableByteChannel> channels) throws IOException {
        super(channels);

        // each split zip segment should begin with zip splite signature
        for(SeekableByteChannel channel : channels) {
            channel.position(0L);
            validSplitSignature(channel);
            channel.position(0L);
        }
    }

    private void validSplitSignature(final SeekableByteChannel channel) throws IOException {
        // the zip split file signature is always at the beginning of the segment
        if (channel.position() > 0) {
            return;
        }

        channel.read(zipSplitSignatureByteBuffer);
        final ZipLong signature = new ZipLong(zipSplitSignatureByteBuffer.array());
        if(!signature.equals(ZipLong.DD_SIG)) {
            throw new IOException("No." + (currentChannelIdx + 1) +  " split zip file is not begin with split zip file signature");
        }
    }

    public synchronized SeekableByteChannel position(long diskNumber, long relativeOffset) throws IOException {
        long globalPosition = relativeOffset;
        for(int i = 0; i < diskNumber;i++) {
            globalPosition += channels.get(i).size();
        }

        return position(globalPosition);
    }

    /**
     * Concatenates the given channels.
     *
     * @param channels the channels to concatenate
     * @throws NullPointerException if channels is null
     * @return SeekableByteChannel that concatenates all provided channels
     */
    public static SeekableByteChannel forSeekableByteChannels(SeekableByteChannel... channels) throws IOException {
        if (Objects.requireNonNull(channels, "channels must not be null").length == 1) {
            return channels[0];
        }
        return new ZipSplitReadOnlySeekableByteChannel(Arrays.asList(channels));
    }

    public static SeekableByteChannel forSeekableByteChannels(SeekableByteChannel lastSegmentChannel, Iterable<SeekableByteChannel> channels) throws IOException {
        if(channels == null || lastSegmentChannel == null) {
            throw new IllegalArgumentException("channels must not be null");
        }

        List<SeekableByteChannel> channelsList = new ArrayList<>();
        for(SeekableByteChannel channel : channels) {
            channelsList.add(channel);
        }
        channelsList.add(lastSegmentChannel);

        if (channelsList.size() == 1) {
            return channelsList.get(0);
        }
        return new ZipSplitReadOnlySeekableByteChannel(channelsList);
    }

    public static SeekableByteChannel buildFromLastSplitSegment(File lastSegmentFile) throws IOException {
        String extension = FileNameUtil.getExtension(lastSegmentFile.getCanonicalPath());
        if(!extension.equals(ArchiveStreamFactory.ZIP)) {
            throw new IllegalArgumentException("The extension of last zip splite segment should be .zip");
        }

        File parent = lastSegmentFile.getParentFile();
        String fileBaseName = FileNameUtil.getBaseName(lastSegmentFile.getCanonicalPath());
        ArrayList<File> splitZipSegments = new ArrayList<>();

        // zip split segments should be like z01,z02....z(n-1) based on the zip specification
        String pattern = fileBaseName + ".z[0-9]+";
        for(File file : parent.listFiles()) {
            if(!Pattern.matches(pattern, file.getName())) {
                continue;
            }

            splitZipSegments.add(file);
        }

        Collections.sort(splitZipSegments, new ZipSplitSegmentComparator());
        return forFiles(lastSegmentFile, splitZipSegments);
    }

    /**
     * Concatenates the given files.
     *
     * @param files the files to concatenate
     * @throws NullPointerException if files is null
     * @throws IOException if opening a channel for one of the files fails
     * @return SeekableByteChannel that concatenates all provided files
     */
    public static SeekableByteChannel forFiles(File... files) throws IOException {
        List<SeekableByteChannel> channels = new ArrayList<>();
        for (File f : Objects.requireNonNull(files, "files must not be null")) {
            channels.add(Files.newByteChannel(f.toPath(), StandardOpenOption.READ));
        }
        if (channels.size() == 1) {
            return channels.get(0);
        }
        return new ZipSplitReadOnlySeekableByteChannel(channels);
    }

    public static SeekableByteChannel forFiles(File lastSegmentFile, Iterable<File> files) throws IOException {
        if(files == null || lastSegmentFile == null) {
            throw new IllegalArgumentException("files must not be null");
        }

        List<SeekableByteChannel> channelsList = new ArrayList<>();
        for (File f : files) {
            channelsList.add(Files.newByteChannel(f.toPath(), StandardOpenOption.READ));
        }
        channelsList.add(Files.newByteChannel(lastSegmentFile.toPath(), StandardOpenOption.READ));

        if (channelsList.size() == 1) {
            return channelsList.get(0);
        }
        return new ZipSplitReadOnlySeekableByteChannel(channelsList);
    }
}
