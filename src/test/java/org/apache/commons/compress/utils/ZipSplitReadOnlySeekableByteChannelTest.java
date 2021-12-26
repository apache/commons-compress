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

import org.apache.commons.compress.archivers.zip.ZipSplitReadOnlySeekableByteChannel;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.apache.commons.compress.AbstractTestCase.getFile;
import static org.apache.commons.compress.AbstractTestCase.getPath;

public class ZipSplitReadOnlySeekableByteChannelTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void constructorThrowsOnNullArg() throws IOException {
        thrown.expect(NullPointerException.class);
        new ZipSplitReadOnlySeekableByteChannel(null);
    }

    @Test
    public void constructorThrowsOnNonSplitZipFiles() throws IOException {
        thrown.expect(IOException.class);
        final List<SeekableByteChannel> channels = new ArrayList<>();
        final File file = getFile("COMPRESS-189.zip");
        channels.add(Files.newByteChannel(file.toPath(), StandardOpenOption.READ));
        new ZipSplitReadOnlySeekableByteChannel(channels);
    }

    @Test
    public void channelsPositionIsZeroAfterConstructor() throws IOException {
        final List<SeekableByteChannel> channels = getSplitZipChannels();
        new ZipSplitReadOnlySeekableByteChannel(channels);
        for (final SeekableByteChannel channel : channels) {
            Assert.assertEquals(0, channel.position());
        }
    }

    @Test
    public void forOrderedSeekableByteChannelsThrowsOnNullArg() throws IOException {
        thrown.expect(NullPointerException.class);
        ZipSplitReadOnlySeekableByteChannel.forOrderedSeekableByteChannels(null);
    }

    @Test
    public void forOrderedSeekableByteChannelsOfTwoParametersThrowsOnNullArg() throws IOException {
        thrown.expect(NullPointerException.class);
        ZipSplitReadOnlySeekableByteChannel.forOrderedSeekableByteChannels(null, null);
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
        Assert.assertTrue(channel instanceof ZipSplitReadOnlySeekableByteChannel);

        channel = ZipSplitReadOnlySeekableByteChannel.forOrderedSeekableByteChannels(firstChannel, secondChannel, lastChannel);
        Assert.assertTrue(channel instanceof ZipSplitReadOnlySeekableByteChannel);
    }

    @Test
    public void forOrderedSeekableByteChannelsReturnsIdentityForSingleElement() throws IOException {
        final SeekableByteChannel emptyChannel = new SeekableInMemoryByteChannel(ByteUtils.EMPTY_BYTE_ARRAY);
        final SeekableByteChannel channel = ZipSplitReadOnlySeekableByteChannel.forOrderedSeekableByteChannels(emptyChannel);
        Assert.assertSame(emptyChannel, channel);
    }

    @Test
    public void forFilesThrowsOnNullArg() throws IOException {
        thrown.expect(NullPointerException.class);
        ZipSplitReadOnlySeekableByteChannel.forFiles(null);
    }

    @Test
    public void forFilesOfTwoParametersThrowsOnNullArg() throws IOException {
        thrown.expect(NullPointerException.class);
        ZipSplitReadOnlySeekableByteChannel.forFiles(null, null);
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
        Assert.assertTrue(channel instanceof ZipSplitReadOnlySeekableByteChannel);

        channel = ZipSplitReadOnlySeekableByteChannel.forFiles(firstFile, secondFile, lastFile);
        Assert.assertTrue(channel instanceof ZipSplitReadOnlySeekableByteChannel);
    }

    @Test
    public void buildFromLastSplitSegmentThrowsOnNotZipFile() throws IOException {
        thrown.expect(IllegalArgumentException.class);
        final File lastFile = getFile("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.z01");
        ZipSplitReadOnlySeekableByteChannel.buildFromLastSplitSegment(lastFile);
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

        Assert.assertEquals(expectedPosition, channel.position());
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
    public void forPathsThrowsOnNullArg() throws IOException {
        ZipSplitReadOnlySeekableByteChannel.forPaths(null);
    }

    @Test
    public void forPathsOfTwoParametersThrowsOnNullArg() throws IOException {
        ZipSplitReadOnlySeekableByteChannel.forPaths(null, null);
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
        Assert.assertTrue(channel instanceof ZipSplitReadOnlySeekableByteChannel);

        channel = ZipSplitReadOnlySeekableByteChannel.forPaths(firstFile, secondFile, lastFile);
        Assert.assertTrue(channel instanceof ZipSplitReadOnlySeekableByteChannel);
    }
}
