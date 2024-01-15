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
package org.apache.commons.compress.archivers.zip;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;

import org.apache.commons.compress.AbstractTempDirTest;
import org.junit.jupiter.api.Test;


public class ZipIoUtilTest extends AbstractTempDirTest {

    @Test
    public void testWriteFully_whenFullAtOnce_thenSucceed() throws IOException {
        final SeekableByteChannel channel = mock(SeekableByteChannel.class);

        when(channel.write((ByteBuffer) any()))
                .thenAnswer(answer -> {
                    ((ByteBuffer) answer.getArgument(0)).position(5);
                    return 5;
                })
                .thenAnswer(answer -> {
                    ((ByteBuffer) answer.getArgument(0)).position(6);
                    return 6;
                });

        ZipIoUtil.writeFully(channel, ByteBuffer.wrap("hello".getBytes(StandardCharsets.UTF_8)));
        ZipIoUtil.writeFully(channel, ByteBuffer.wrap("world\n".getBytes(StandardCharsets.UTF_8)));

        verify(channel, times(2))
                .write((ByteBuffer) any());
    }

    @Test
    public void testWriteFully_whenFullButPartial_thenSucceed() throws IOException {
        final SeekableByteChannel channel = mock(SeekableByteChannel.class);

        when(channel.write((ByteBuffer) any()))
                .thenAnswer(answer -> {
                    ((ByteBuffer) answer.getArgument(0)).position(3);
                    return 3;
                })
                .thenAnswer(answer -> {
                    ((ByteBuffer) answer.getArgument(0)).position(5);
                    return 2;
                })
                .thenAnswer(answer -> {
                    ((ByteBuffer) answer.getArgument(0)).position(6);
                    return 6;
                });

        ZipIoUtil.writeFully(channel, ByteBuffer.wrap("hello".getBytes(StandardCharsets.UTF_8)));
        ZipIoUtil.writeFully(channel, ByteBuffer.wrap("world\n".getBytes(StandardCharsets.UTF_8)));

        verify(channel, times(3))
                .write((ByteBuffer) any());
    }

    @Test
    public void testWriteFully_whenPartial_thenFail() throws IOException {
        final SeekableByteChannel channel = mock(SeekableByteChannel.class);

        when(channel.write((ByteBuffer) any()))
                .thenAnswer(answer -> {
                    ((ByteBuffer) answer.getArgument(0)).position(3);
                    return 3;
                })
                .thenAnswer(answer -> 0);

        assertThrows(IOException.class, () ->
                ZipIoUtil.writeFully(channel, ByteBuffer.wrap("hello".getBytes(StandardCharsets.UTF_8)))
        );

        verify(channel, times(2))
                .write((ByteBuffer) any());
    }

    @Test
    public void testWriteFullyAt_whenFullAtOnce_thenSucceed() throws IOException {
        final FileChannel channel = mock(FileChannel.class);

        when(channel.write((ByteBuffer) any(), eq(20L)))
                .thenAnswer(answer -> {
                    ((ByteBuffer) answer.getArgument(0)).position(5);
                    return 5;
                });
        when(channel.write((ByteBuffer) any(), eq(30L)))
                .thenAnswer(answer -> {
                    ((ByteBuffer) answer.getArgument(0)).position(6);
                    return 6;
                });

        ZipIoUtil.writeFullyAt(channel, ByteBuffer.wrap("hello".getBytes(StandardCharsets.UTF_8)), 20);
        ZipIoUtil.writeFullyAt(channel, ByteBuffer.wrap("world\n".getBytes(StandardCharsets.UTF_8)), 30);

        verify(channel, times(1))
                .write((ByteBuffer) any(), eq(20L));
        verify(channel, times(1))
                .write((ByteBuffer) any(), eq(30L));
    }

    @Test
    public void testWriteFullyAt_whenFullButPartial_thenSucceed() throws IOException {
        final FileChannel channel = mock(FileChannel.class);

        when(channel.write((ByteBuffer) any(), eq(20L)))
                .thenAnswer(answer -> {
                    ((ByteBuffer) answer.getArgument(0)).position(3);
                    return 3;
                });
        when(channel.write((ByteBuffer) any(), eq(23L)))
                .thenAnswer(answer -> {
                    ((ByteBuffer) answer.getArgument(0)).position(5);
                    return 2;
                });
        when(channel.write((ByteBuffer) any(), eq(30L)))
                .thenAnswer(answer -> {
                    ((ByteBuffer) answer.getArgument(0)).position(6);
                    return 6;
                });

        ZipIoUtil.writeFullyAt(channel, ByteBuffer.wrap("hello".getBytes(StandardCharsets.UTF_8)), 20);
        ZipIoUtil.writeFullyAt(channel, ByteBuffer.wrap("world\n".getBytes(StandardCharsets.UTF_8)), 30);

        verify(channel, times(1))
                .write((ByteBuffer) any(), eq(20L));
        verify(channel, times(1))
                .write((ByteBuffer) any(), eq(23L));
        verify(channel, times(1))
                .write((ByteBuffer) any(), eq(30L));
    }

    @Test
    public void testWriteFullyAt_whenPartial_thenFail() throws IOException {
        final FileChannel channel = mock(FileChannel.class);

        when(channel.write((ByteBuffer) any(), eq(20L)))
                .thenAnswer(answer -> {
                    ((ByteBuffer) answer.getArgument(0)).position(3);
                    return 3;
                });
        when(channel.write((ByteBuffer) any(), eq(23L)))
                .thenAnswer(answer -> 0);
        assertThrows(IOException.class, () ->
                ZipIoUtil.writeFullyAt(channel, ByteBuffer.wrap("hello".getBytes(StandardCharsets.UTF_8)), 20));

        verify(channel, times(1))
                .write((ByteBuffer) any(), eq(20L));
        verify(channel, times(1))
                .write((ByteBuffer) any(), eq(23L));
        verify(channel, times(0))
                .write((ByteBuffer) any(), eq(25L));
    }
}
