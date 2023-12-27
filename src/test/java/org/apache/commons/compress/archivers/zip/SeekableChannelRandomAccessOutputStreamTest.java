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

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.apache.commons.compress.AbstractTempDirTest;
import org.junit.jupiter.api.Test;


public class SeekableChannelRandomAccessOutputStreamTest extends AbstractTempDirTest {

    @Test
    public void testInitialization() throws IOException {
        Path file = newTempPath("testChannel");
        try (SeekableChannelRandomAccessOutputStream stream = new SeekableChannelRandomAccessOutputStream(
                Files.newByteChannel(file, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
    )) {
            assertEquals(0, stream.position());
        }
    }

    @Test
    public void testWrite() throws IOException {
        FileChannel channel = mock(FileChannel.class);
        SeekableChannelRandomAccessOutputStream stream = new SeekableChannelRandomAccessOutputStream(channel);

        when(channel.position())
                .thenReturn(11L);
        when(channel.write((ByteBuffer) any()))
                .thenAnswer(answer -> {
                    ((ByteBuffer) answer.getArgument(0)).position(5);
                    return 5;
                })
                .thenAnswer(answer -> {
                    ((ByteBuffer) answer.getArgument(0)).position(6);
                    return 6;
                });

        stream.write("hello".getBytes(StandardCharsets.UTF_8));
        stream.write("world\n".getBytes(StandardCharsets.UTF_8));

        verify(channel, times(2))
                .write((ByteBuffer) any());

        assertEquals(11, stream.position());
    }

    @Test
    public void testWriteFullyAt_whenFullAtOnce_thenSucceed() throws IOException {
        SeekableByteChannel channel = mock(SeekableByteChannel.class);
        SeekableChannelRandomAccessOutputStream stream = new SeekableChannelRandomAccessOutputStream(channel);

        when(channel.position())
                .thenReturn(50L)
                .thenReturn(60L);
        when(channel.write((ByteBuffer) any()))
                .thenAnswer(answer -> {
                    ((ByteBuffer) answer.getArgument(0)).position(5);
                    return 5;
                })
                .thenAnswer(answer -> {
                    ((ByteBuffer) answer.getArgument(0)).position(6);
                    return 6;
                });

        stream.writeFullyAt("hello".getBytes(StandardCharsets.UTF_8), 20);
        stream.writeFullyAt("world\n".getBytes(StandardCharsets.UTF_8), 30);

        verify(channel, times(2))
                .write((ByteBuffer) any());
        verify(channel, times(1))
                .position(eq(50L));
        verify(channel, times(1))
                .position(eq(60L));

        assertEquals(60L, stream.position());
    }

    @Test
    public void testWriteFullyAt_whenFullButPartial_thenSucceed() throws IOException {
        SeekableByteChannel channel = mock(SeekableByteChannel.class);
        SeekableChannelRandomAccessOutputStream stream = new SeekableChannelRandomAccessOutputStream(channel);

        when(channel.position())
                .thenReturn(50L)
                .thenReturn(60L);
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

        stream.writeFullyAt("hello".getBytes(StandardCharsets.UTF_8), 20);
        stream.writeFullyAt("world\n".getBytes(StandardCharsets.UTF_8), 30);

        verify(channel, times(3))
                .write((ByteBuffer) any());
        verify(channel, times(1))
                .position(eq(50L));
        verify(channel, times(1))
                .position(eq(60L));

        assertEquals(60L, stream.position());
    }

    @Test
    public void testWriteFullyAt_whenPartial_thenFail() throws IOException {
        SeekableByteChannel channel = mock(SeekableByteChannel.class);
        SeekableChannelRandomAccessOutputStream stream = new SeekableChannelRandomAccessOutputStream(channel);

        when(channel.position())
                .thenReturn(50L);
        when(channel.write((ByteBuffer) any()))
                .thenAnswer(answer -> {
                    ((ByteBuffer) answer.getArgument(0)).position(3);
                    return 3;
                })
                .thenAnswer(answer -> {
                    return 0;
                });

        assertThrows(IOException.class, () -> stream.writeFullyAt("hello".getBytes(StandardCharsets.UTF_8), 20));

        verify(channel, times(2))
                .write((ByteBuffer) any());

        assertEquals(50L, stream.position());
    }
}
