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
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.apache.commons.compress.AbstractTestCase.getFile;

public class ZipSplitReadOnlySeekableByteChannelTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void constructorThrowsOnNullArg() throws IOException {
        thrown.expect(NullPointerException.class);
        new ZipSplitReadOnlySeekableByteChannel(null);
    }

    @Test
    public void constructorThrowsOnNonSplitZipFiles() throws IOException {
        thrown.expect(IOException.class);
        List<SeekableByteChannel> channels = new ArrayList<>();
        File file = getFile("COMPRESS-189.zip");
        channels.add(Files.newByteChannel(file.toPath(), StandardOpenOption.READ));
        new ZipSplitReadOnlySeekableByteChannel(channels);
    }

    @Test
    public void channelsPositionIsZeroAfterConstructor() throws IOException {
        List<SeekableByteChannel> channels = getSplitZipChannels();
        new ZipSplitReadOnlySeekableByteChannel(channels);
        for (SeekableByteChannel channel : channels) {
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
        File file1 = getFile("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.z01");
        SeekableByteChannel firstChannel = Files.newByteChannel(file1.toPath(), StandardOpenOption.READ);

        File file2 = getFile("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.z02");
        SeekableByteChannel secondChannel = Files.newByteChannel(file2.toPath(), StandardOpenOption.READ);

        File lastFile = getFile("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.zip");
        SeekableByteChannel lastChannel = Files.newByteChannel(lastFile.toPath(), StandardOpenOption.READ);

        List<SeekableByteChannel> channels = new ArrayList<>();
        channels.add(firstChannel);
        channels.add(secondChannel);

        SeekableByteChannel channel = ZipSplitReadOnlySeekableByteChannel.forOrderedSeekableByteChannels(lastChannel, channels);
        Assert.assertTrue(channel instanceof ZipSplitReadOnlySeekableByteChannel);

        channel = ZipSplitReadOnlySeekableByteChannel.forOrderedSeekableByteChannels(firstChannel, secondChannel, lastChannel);
        Assert.assertTrue(channel instanceof ZipSplitReadOnlySeekableByteChannel);
    }

    @Test
    public void forOrderedSeekableByteChannelsReturnsIdentityForSingleElement() throws IOException {
        SeekableByteChannel emptyChannel = new SeekableInMemoryByteChannel(new byte[0]);
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
        File firstFile = getFile("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.z01");
        File secondFile = getFile("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.z02");
        File lastFile = getFile("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.z01");

        ArrayList<File> list = new ArrayList<>();
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
        File lastFile = getFile("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.z01");
        ZipSplitReadOnlySeekableByteChannel.buildFromLastSplitSegment(lastFile);
    }

    @Test
    public void positionToSomeZipSplitSegment() throws IOException {
        File firstFile = getFile("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.z01");
        int firstFileSize = (int) firstFile.length();

        File secondFile = getFile("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.z02");
        int secondFileSize = (int) secondFile.length();

        File lastFile = getFile("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.zip");
        int lastFileSize = (int) lastFile.length();

        Random random = new Random();
        int randomDiskNumber = random.nextInt(3);
        int randomOffset = randomDiskNumber < 2 ? random.nextInt(firstFileSize) : random.nextInt(lastFileSize);

        ZipSplitReadOnlySeekableByteChannel channel = (ZipSplitReadOnlySeekableByteChannel) ZipSplitReadOnlySeekableByteChannel.buildFromLastSplitSegment(lastFile);
        channel.position(randomDiskNumber, randomOffset);
        long expectedPosition = randomOffset;

        expectedPosition += randomDiskNumber > 0 ? firstFileSize : 0;
        expectedPosition += randomDiskNumber > 1 ? secondFileSize : 0;

        Assert.assertEquals(expectedPosition, channel.position());
    }

    private List<SeekableByteChannel> getSplitZipChannels() throws IOException {
        List<SeekableByteChannel> channels = new ArrayList<>();
        File file1 = getFile("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.z01");
        channels.add(Files.newByteChannel(file1.toPath(), StandardOpenOption.READ));

        File file2 = getFile("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.z02");
        channels.add(Files.newByteChannel(file2.toPath(), StandardOpenOption.READ));

        File lastFile = getFile("COMPRESS-477/split_zip_created_by_zip/split_zip_created_by_zip.zip");
        channels.add(Files.newByteChannel(lastFile.toPath(), StandardOpenOption.READ));

        return channels;
    }
}
