/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.commons.compress.utils;

import static org.apache.commons.compress.AbstractTest.getFile;
import static org.apache.commons.compress.AbstractTest.getPath;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.compress.archivers.zip.ZipSplitReadOnlySeekableByteChannel;
import org.junit.jupiter.api.Test;

public class ZipSplitReadOnlySeekableByteChannelTest {

    private List<SeekableByteChannel> getSplitZipChannels() throws IOException {
        final List<SeekableByteChannel> channels = new ArrayList<>();
        final File file1 = getFile("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.z01");
        channels.add(Files.newByteChannel(file1.toPath(), StandardOpenOption.READ));

        final File file2 = getFile("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.z02");
        channels.add(Files.newByteChannel(file2.toPath(), StandardOpenOption.READ));

        final File lastFile = getFile("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.zip");
        channels.add(Files.newByteChannel(lastFile.toPath(), StandardOpenOption.READ));

        return channels;
    }

    @Test
    public void testBuildFromLastSplitSegmentThrowsOnNotZipFile() throws IOException {
        final File lastFile = getFile("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.z01");
        assertThrows(IllegalArgumentException.class, () -> ZipSplitReadOnlySeekableByteChannel.buildFromLastSplitSegment(lastFile));
    }

    @Test
    public void testChannelsPositionIsZeroAfterConstructor() throws IOException {
        final List<SeekableByteChannel> channels = getSplitZipChannels();
        new ZipSplitReadOnlySeekableByteChannel(channels);
        for (final SeekableByteChannel channel : channels) {
            assertEquals(0, channel.position());
        }
    }

    @Test
    public void testConstructorThrowsOnNonSplitZipFiles() throws IOException {
        final List<SeekableByteChannel> channels = new ArrayList<>();
        final File file = getFile("COMPRESS-189.zip");
        try (SeekableByteChannel byteChannel = Files.newByteChannel(file.toPath(), StandardOpenOption.READ)) {
            channels.add(byteChannel);
            assertThrows(IOException.class, () -> new ZipSplitReadOnlySeekableByteChannel(channels));
        }
    }

    @Test
    public void testConstructorThrowsOnNullArg() {
        assertThrows(NullPointerException.class, () -> new ZipSplitReadOnlySeekableByteChannel(null));
    }

    @Test
    public void testForFilesOfTwoParametersThrowsOnNullArg() {
        assertThrows(NullPointerException.class, () -> ZipSplitReadOnlySeekableByteChannel.forFiles(null, null));
    }

    @Test
    public void testForFilesReturnCorrectClass() throws IOException {
        final File firstFile = getFile("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.z01");
        final File secondFile = getFile("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.z02");
        final File lastFile = getFile("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.z01");

        final ArrayList<File> list = new ArrayList<>();
        list.add(firstFile);
        list.add(secondFile);

        try (SeekableByteChannel channel = ZipSplitReadOnlySeekableByteChannel.forFiles(lastFile, list)) {
            assertTrue(channel instanceof ZipSplitReadOnlySeekableByteChannel);
        }

        try (SeekableByteChannel channel = ZipSplitReadOnlySeekableByteChannel.forFiles(firstFile, secondFile, lastFile)) {
            assertTrue(channel instanceof ZipSplitReadOnlySeekableByteChannel);
        }
    }

    @Test
    public void testForFilesThrowsOnNullArg() {
        assertThrows(NullPointerException.class, () -> ZipSplitReadOnlySeekableByteChannel.forFiles(null));
    }

    @Test
    public void testForOrderedSeekableByteChannelsOfTwoParametersThrowsOnNullArg() {
        assertThrows(NullPointerException.class, () -> ZipSplitReadOnlySeekableByteChannel.forOrderedSeekableByteChannels(null, null));
    }

    @Test
    public void testForOrderedSeekableByteChannelsReturnCorrectClass() throws IOException {
        final File file1 = getFile("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.z01");
        final File file2 = getFile("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.z02");
        final File lastFile = getFile("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.zip");

        try (SeekableByteChannel firstChannel = Files.newByteChannel(file1.toPath(), StandardOpenOption.READ);
                SeekableByteChannel secondChannel = Files.newByteChannel(file2.toPath(), StandardOpenOption.READ);
                SeekableByteChannel lastChannel = Files.newByteChannel(lastFile.toPath(), StandardOpenOption.READ)) {

            final List<SeekableByteChannel> channels = new ArrayList<>();
            channels.add(firstChannel);
            channels.add(secondChannel);

            @SuppressWarnings("resource") // try-with-resources closes
            final SeekableByteChannel channel1 = ZipSplitReadOnlySeekableByteChannel.forOrderedSeekableByteChannels(lastChannel, channels);
            assertTrue(channel1 instanceof ZipSplitReadOnlySeekableByteChannel);

            @SuppressWarnings("resource") // try-with-resources closes
            final SeekableByteChannel channel2 = ZipSplitReadOnlySeekableByteChannel.forOrderedSeekableByteChannels(firstChannel, secondChannel, lastChannel);
            assertTrue(channel2 instanceof ZipSplitReadOnlySeekableByteChannel);
        }
    }

    @Test
    public void testForOrderedSeekableByteChannelsReturnsIdentityForSingleElement() throws IOException {
        try (SeekableByteChannel emptyChannel = new SeekableInMemoryByteChannel(ByteUtils.EMPTY_BYTE_ARRAY);
                SeekableByteChannel channel = ZipSplitReadOnlySeekableByteChannel.forOrderedSeekableByteChannels(emptyChannel)) {
            assertSame(emptyChannel, channel);
        }
    }

    @Test
    public void testForOrderedSeekableByteChannelsThrowsOnNullArg() {
        assertThrows(NullPointerException.class, () -> ZipSplitReadOnlySeekableByteChannel.forOrderedSeekableByteChannels(null));
    }

    @Test
    public void testForPathsOfTwoParametersThrowsOnNullArg() {
        assertThrows(NullPointerException.class, () -> ZipSplitReadOnlySeekableByteChannel.forPaths(null, null));
    }

    @Test
    public void testForPathsReturnCorrectClass() throws IOException {
        final Path firstFile = getPath("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.z01");
        final Path secondFile = getPath("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.z02");
        final Path lastFile = getPath("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.zip");

        final ArrayList<Path> list = new ArrayList<>();
        list.add(firstFile);
        list.add(secondFile);

        try (SeekableByteChannel channel = ZipSplitReadOnlySeekableByteChannel.forPaths(lastFile, list)) {
            assertTrue(channel instanceof ZipSplitReadOnlySeekableByteChannel);
        }

        try (SeekableByteChannel channel = ZipSplitReadOnlySeekableByteChannel.forPaths(firstFile, secondFile, lastFile)) {
            assertTrue(channel instanceof ZipSplitReadOnlySeekableByteChannel);
        }
    }

    @Test
    public void testForPathsThrowsOnNullArg() {
        assertThrows(NullPointerException.class, () -> ZipSplitReadOnlySeekableByteChannel.forPaths(null));
    }

    @Test
    public void testPositionToSomeZipSplitSegment() throws IOException {
        final File firstFile = getFile("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.z01");
        final int firstFileSize = (int) firstFile.length();

        final File secondFile = getFile("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.z02");
        final int secondFileSize = (int) secondFile.length();

        final File lastFile = getFile("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.zip");
        final int lastFileSize = (int) lastFile.length();

        final Random random = new Random();
        final int randomDiskNumber = random.nextInt(3);
        final int randomOffset = randomDiskNumber < 2 ? random.nextInt(firstFileSize) : random.nextInt(lastFileSize);

        try (ZipSplitReadOnlySeekableByteChannel channel = (ZipSplitReadOnlySeekableByteChannel) ZipSplitReadOnlySeekableByteChannel
                .buildFromLastSplitSegment(lastFile)) {
            channel.position(randomDiskNumber, randomOffset);
            long expectedPosition = randomOffset;

            expectedPosition += randomDiskNumber > 0 ? firstFileSize : 0;
            expectedPosition += randomDiskNumber > 1 ? secondFileSize : 0;

            assertEquals(expectedPosition, channel.position());
        }
    }
}
