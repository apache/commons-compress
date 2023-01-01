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
 *
 */

package org.apache.commons.compress.utils;

import static org.apache.commons.compress.AbstractTestCase.getFile;
import static org.apache.commons.compress.AbstractTestCase.getPath;
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

    @Test
    public void buildFromLastSplitSegmentThrowsOnNotZipFile() throws IOException {
        final File lastFile = getFile("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.z01");
        assertThrows(IllegalArgumentException.class, () -> ZipSplitReadOnlySeekableByteChannel.buildFromLastSplitSegment(lastFile));
    }

    @Test
    public void channelsPositionIsZeroAfterConstructor() throws IOException {
        final List<SeekableByteChannel> channels = getSplitZipChannels();
        new ZipSplitReadOnlySeekableByteChannel(channels);
        for (final SeekableByteChannel channel : channels) {
            assertEquals(0, channel.position());
        }
    }

    @Test
    public void constructorThrowsOnNonSplitZipFiles() throws IOException {
        final List<SeekableByteChannel> channels = new ArrayList<>();
        final File file = getFile("COMPRESS-189.zip");
        channels.add(Files.newByteChannel(file.toPath(), StandardOpenOption.READ));
        assertThrows(IOException.class, () -> new ZipSplitReadOnlySeekableByteChannel(channels));
    }

    @Test
    public void constructorThrowsOnNullArg() {
        assertThrows(NullPointerException.class, () -> new ZipSplitReadOnlySeekableByteChannel(null));
    }

    @Test
    public void forFilesOfTwoParametersThrowsOnNullArg() {
        assertThrows(NullPointerException.class, () -> ZipSplitReadOnlySeekableByteChannel.forFiles(null, null));
    }

    @Test
    public void forFilesReturnCorrectClass() throws IOException {
        final File firstFile = getFile("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.z01");
        final File secondFile = getFile("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.z02");
        final File lastFile = getFile("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.z01");

        final ArrayList<File> list = new ArrayList<>();
        list.add(firstFile);
        list.add(secondFile);

        SeekableByteChannel channel = ZipSplitReadOnlySeekableByteChannel.forFiles(lastFile, list);
        assertTrue(channel instanceof ZipSplitReadOnlySeekableByteChannel);

        channel = ZipSplitReadOnlySeekableByteChannel.forFiles(firstFile, secondFile, lastFile);
        assertTrue(channel instanceof ZipSplitReadOnlySeekableByteChannel);
    }

    @Test
    public void forFilesThrowsOnNullArg() {
        assertThrows(NullPointerException.class, () -> ZipSplitReadOnlySeekableByteChannel.forFiles(null));
    }

    @Test
    public void forOrderedSeekableByteChannelsOfTwoParametersThrowsOnNullArg() {
        assertThrows(NullPointerException.class, () -> ZipSplitReadOnlySeekableByteChannel.forOrderedSeekableByteChannels(null, null));
    }

    @Test
    public void forOrderedSeekableByteChannelsReturnCorrectClass() throws IOException {
        final File file1 = getFile("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.z01");
        final SeekableByteChannel firstChannel = Files.newByteChannel(file1.toPath(), StandardOpenOption.READ);

        final File file2 = getFile("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.z02");
        final SeekableByteChannel secondChannel = Files.newByteChannel(file2.toPath(), StandardOpenOption.READ);

        final File lastFile = getFile("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.zip");
        final SeekableByteChannel lastChannel = Files.newByteChannel(lastFile.toPath(), StandardOpenOption.READ);

        final List<SeekableByteChannel> channels = new ArrayList<>();
        channels.add(firstChannel);
        channels.add(secondChannel);

        SeekableByteChannel channel = ZipSplitReadOnlySeekableByteChannel.forOrderedSeekableByteChannels(lastChannel, channels);
        assertTrue(channel instanceof ZipSplitReadOnlySeekableByteChannel);

        channel = ZipSplitReadOnlySeekableByteChannel.forOrderedSeekableByteChannels(firstChannel, secondChannel, lastChannel);
        assertTrue(channel instanceof ZipSplitReadOnlySeekableByteChannel);
    }

    @Test
    public void forOrderedSeekableByteChannelsReturnsIdentityForSingleElement() throws IOException {
        final SeekableByteChannel emptyChannel = new SeekableInMemoryByteChannel(ByteUtils.EMPTY_BYTE_ARRAY);
        final SeekableByteChannel channel = ZipSplitReadOnlySeekableByteChannel.forOrderedSeekableByteChannels(emptyChannel);
        assertSame(emptyChannel, channel);
    }

    @Test
    public void forOrderedSeekableByteChannelsThrowsOnNullArg() {
        assertThrows(NullPointerException.class, () -> ZipSplitReadOnlySeekableByteChannel.forOrderedSeekableByteChannels(null));
    }

    @Test
    public void forPathsOfTwoParametersThrowsOnNullArg() {
        assertThrows(NullPointerException.class, () -> ZipSplitReadOnlySeekableByteChannel.forPaths(null, null));
    }

    @Test
    public void forPathsReturnCorrectClass() throws IOException {
        final Path firstFile = getPath("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.z01");
        final Path secondFile = getPath("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.z02");
        final Path lastFile = getPath("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.zip");

        final ArrayList<Path> list = new ArrayList<>();
        list.add(firstFile);
        list.add(secondFile);

        SeekableByteChannel channel = ZipSplitReadOnlySeekableByteChannel.forPaths(lastFile, list);
        assertTrue(channel instanceof ZipSplitReadOnlySeekableByteChannel);

        channel = ZipSplitReadOnlySeekableByteChannel.forPaths(firstFile, secondFile, lastFile);
        assertTrue(channel instanceof ZipSplitReadOnlySeekableByteChannel);
    }

    @Test
    public void forPathsThrowsOnNullArg() {
        assertThrows(NullPointerException.class, () -> ZipSplitReadOnlySeekableByteChannel.forPaths(null));
    }

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
    public void positionToSomeZipSplitSegment() throws IOException {
        final File firstFile = getFile("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.z01");
        final int firstFileSize = (int) firstFile.length();

        final File secondFile = getFile("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.z02");
        final int secondFileSize = (int) secondFile.length();

        final File lastFile = getFile("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.zip");
        final int lastFileSize = (int) lastFile.length();

        final Random random = new Random();
        final int randomDiskNumber = random.nextInt(3);
        final int randomOffset = randomDiskNumber < 2 ? random.nextInt(firstFileSize) : random.nextInt(lastFileSize);

        final ZipSplitReadOnlySeekableByteChannel channel = (ZipSplitReadOnlySeekableByteChannel) ZipSplitReadOnlySeekableByteChannel.buildFromLastSplitSegment(lastFile);
        channel.position(randomDiskNumber, randomOffset);
        long expectedPosition = randomOffset;

        expectedPosition += randomDiskNumber > 0 ? firstFileSize : 0;
        expectedPosition += randomDiskNumber > 1 ? secondFileSize : 0;

        assertEquals(expectedPosition, channel.position());
    }
}
