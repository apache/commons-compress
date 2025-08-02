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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;

import org.apache.commons.compress.AbstractTempDirTest;
import org.apache.commons.compress.archivers.ArchiveException;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link ZipIoUtil}.
 */
class ZipIoUtilTest extends AbstractTempDirTest {

    private FileChannel mockFileChannel() throws IOException {
        final FileChannel spy = spy(FileChannel.class);
        doNothing().when(spy).close();
        return spy;
    }

    private SeekableByteChannel mockSeekableByteChannel() {
        return mock(SeekableByteChannel.class);
    }

    @Test
    void testWriteFully_whenFullAtOnce_thenSucceed() throws IOException {
        try (SeekableByteChannel channel = mockSeekableByteChannel()) {
            when(channel.write((ByteBuffer) any())).thenAnswer(answer -> {
                ((ByteBuffer) answer.getArgument(0)).position(5);
                return 5;
            }).thenAnswer(answer -> {
                ((ByteBuffer) answer.getArgument(0)).position(6);
                return 6;
            });
            ZipIoUtil.writeAll(channel, ByteBuffer.wrap("hello".getBytes(StandardCharsets.UTF_8)));
            ZipIoUtil.writeAll(channel, ByteBuffer.wrap("world\n".getBytes(StandardCharsets.UTF_8)));
            verify(channel, times(2)).write((ByteBuffer) any());
        }
    }

    @Test
    void testWriteFully_whenFullButPartial_thenSucceed() throws IOException {
        try (SeekableByteChannel channel = mockSeekableByteChannel()) {
            when(channel.write((ByteBuffer) any())).thenAnswer(answer -> {
                ((ByteBuffer) answer.getArgument(0)).position(3);
                return 3;
            }).thenAnswer(answer -> {
                ((ByteBuffer) answer.getArgument(0)).position(5);
                return 2;
            }).thenAnswer(answer -> {
                ((ByteBuffer) answer.getArgument(0)).position(6);
                return 6;
            });
            ZipIoUtil.writeAll(channel, ByteBuffer.wrap("hello".getBytes(StandardCharsets.UTF_8)));
            ZipIoUtil.writeAll(channel, ByteBuffer.wrap("world\n".getBytes(StandardCharsets.UTF_8)));
            verify(channel, times(3)).write((ByteBuffer) any());
        }
    }

    @Test
    void testWriteFully_whenPartial_thenFail() throws IOException {
        try (SeekableByteChannel channel = mockSeekableByteChannel()) {
            when(channel.write((ByteBuffer) any())).thenAnswer(answer -> {
                ((ByteBuffer) answer.getArgument(0)).position(3);
                return 3;
            }).thenAnswer(answer -> 0).thenAnswer(answer -> -1);
            assertThrows(ArchiveException.class, () -> ZipIoUtil.writeAll(channel, ByteBuffer.wrap("hello".getBytes(StandardCharsets.UTF_8))));
            verify(channel, times(3)).write((ByteBuffer) any());
        }
    }

    @Test
    void testWriteFullyAt_whenFullAtOnce_thenSucceed() throws IOException {
        try (FileChannel channel = mockFileChannel()) {
            when(channel.write((ByteBuffer) any(), eq(20L))).thenAnswer(answer -> {
                ((ByteBuffer) answer.getArgument(0)).position(5);
                return 5;
            });
            when(channel.write((ByteBuffer) any(), eq(30L))).thenAnswer(answer -> {
                ((ByteBuffer) answer.getArgument(0)).position(6);
                return 6;
            });
            ZipIoUtil.writeAll(channel, ByteBuffer.wrap("hello".getBytes(StandardCharsets.UTF_8)), 20);
            ZipIoUtil.writeAll(channel, ByteBuffer.wrap("world\n".getBytes(StandardCharsets.UTF_8)), 30);
            verify(channel, times(1)).write((ByteBuffer) any(), eq(20L));
            verify(channel, times(1)).write((ByteBuffer) any(), eq(30L));
        }
    }

    @Test
    void testWriteFullyAt_whenFullButPartial_thenSucceed() throws IOException {
        try (FileChannel channel = mockFileChannel()) {
            when(channel.write((ByteBuffer) any(), eq(20L))).thenAnswer(answer -> {
                ((ByteBuffer) answer.getArgument(0)).position(3);
                return 3;
            });
            when(channel.write((ByteBuffer) any(), eq(23L))).thenAnswer(answer -> {
                ((ByteBuffer) answer.getArgument(0)).position(5);
                return 2;
            });
            when(channel.write((ByteBuffer) any(), eq(30L))).thenAnswer(answer -> {
                ((ByteBuffer) answer.getArgument(0)).position(6);
                return 6;
            });
            ZipIoUtil.writeAll(channel, ByteBuffer.wrap("hello".getBytes(StandardCharsets.UTF_8)), 20);
            ZipIoUtil.writeAll(channel, ByteBuffer.wrap("world\n".getBytes(StandardCharsets.UTF_8)), 30);
            verify(channel, times(1)).write((ByteBuffer) any(), eq(20L));
            verify(channel, times(1)).write((ByteBuffer) any(), eq(23L));
            verify(channel, times(1)).write((ByteBuffer) any(), eq(30L));
        }
    }

    @Test
    void testWriteFullyAt_whenPartial_thenFail() throws IOException {
        try (FileChannel channel = mockFileChannel()) {
            when(channel.write((ByteBuffer) any(), eq(20L))).thenAnswer(answer -> {
                ((ByteBuffer) answer.getArgument(0)).position(3);
                return 3;
            });
            when(channel.write((ByteBuffer) any(), eq(23L))).thenAnswer(answer -> 0).thenAnswer(answer -> -1);
            assertThrows(ArchiveException.class, () -> ZipIoUtil.writeAll(channel, ByteBuffer.wrap("hello".getBytes(StandardCharsets.UTF_8)), 20));
            verify(channel, times(1)).write((ByteBuffer) any(), eq(20L));
            verify(channel, times(2)).write((ByteBuffer) any(), eq(23L));
            verify(channel, times(0)).write((ByteBuffer) any(), eq(25L));
        }
    }
}
