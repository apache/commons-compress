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
package org.apache.commons.compress.compressors.deflate64;

import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;

@RunWith(MockitoJUnitRunner.class)
public class Deflate64CompressorInputStreamTest {
    private final HuffmanDecoder nullDecoder = null;

    @Mock
    private HuffmanDecoder decoder;

    @Test
    public void readWhenClosed() throws Exception {
        Deflate64CompressorInputStream input = new Deflate64CompressorInputStream(nullDecoder);
        assertEquals(-1, input.read());
        assertEquals(-1, input.read(new byte[1]));
        assertEquals(-1, input.read(new byte[1], 0, 1));
    }

    @Test
    public void properSizeWhenClosed() throws Exception {
        Deflate64CompressorInputStream input = new Deflate64CompressorInputStream(nullDecoder);
        assertEquals(0, input.available());
    }

    @Test
    public void delegatesAvailable() throws Exception {
        Mockito.when(decoder.available()).thenReturn(1024);

        Deflate64CompressorInputStream input = new Deflate64CompressorInputStream(decoder);
        assertEquals(1024, input.available());
    }

    @Test
    public void closeCallsDecoder() throws Exception {

        Deflate64CompressorInputStream input = new Deflate64CompressorInputStream(decoder);
        input.close();

        Mockito.verify(decoder, times(1)).close();
    }

    @Test
    public void closeIsDelegatedJustOnce() throws Exception {

        Deflate64CompressorInputStream input = new Deflate64CompressorInputStream(decoder);

        input.close();
        input.close();

        Mockito.verify(decoder, times(1)).close();
    }

    @Test
    public void uncompressedBlock() throws Exception {
        byte[] data = {
            1, 11, 0, -12, -1,
            'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd'
        };

        try (Deflate64CompressorInputStream input = new Deflate64CompressorInputStream(new ByteArrayInputStream(data));
             BufferedReader br = new BufferedReader(new InputStreamReader(input))) {
            assertEquals("Hello World", br.readLine());
            assertEquals(null, br.readLine());
        }
    }

    @Test
    public void uncompressedBlockViaFactory() throws Exception {
        byte[] data = {
            1, 11, 0, -12, -1,
            'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd'
        };

        try (InputStream input = new CompressorStreamFactory()
             .createCompressorInputStream(CompressorStreamFactory.DEFLATE64, new ByteArrayInputStream(data));
             BufferedReader br = new BufferedReader(new InputStreamReader(input))) {
            assertEquals("Hello World", br.readLine());
            assertEquals(null, br.readLine());
        }
    }

    @Test
    public void uncompressedBlockAvailable() throws Exception {
        byte[] data = {
            1, 11, 0, -12, -1,
            'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd'
        };

        try (Deflate64CompressorInputStream input = new Deflate64CompressorInputStream(new ByteArrayInputStream(data))) {
            assertEquals('H', input.read());
            assertEquals(10, input.available());
        }
    }

    @Test
    public void streamIgnoresExtraBytesAfterDeflatedInput() throws Exception
    {
        byte[] data = {
            1, 11, 0, -12, -1,
            'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd', 'X'
        };

        try (Deflate64CompressorInputStream input = new Deflate64CompressorInputStream(new ByteArrayInputStream(data));
             BufferedReader br = new BufferedReader(new InputStreamReader(input))) {
            assertEquals("Hello World", br.readLine());
            assertEquals(null, br.readLine());
        }
    }

    @Test(expected = java.io.EOFException.class)
    public void throwsEOFExceptionOnTruncatedStreams() throws Exception
    {
        byte[] data = {
            1, 11, 0, -12, -1,
            'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l',
        };

        try (Deflate64CompressorInputStream input = new Deflate64CompressorInputStream(new ByteArrayInputStream(data));
             BufferedReader br = new BufferedReader(new InputStreamReader(input))) {
            assertEquals("Hello World", br.readLine());
        }
    }

    @Test
    public void singleByteReadConsistentlyReturnsMinusOneAtEof() throws Exception {
        try (final Deflate64CompressorInputStream in =
                    new Deflate64CompressorInputStream(nullDecoder)) {
            IOUtils.toByteArray(in);
            assertEquals(-1, in.read());
            assertEquals(-1, in.read());
            in.close();
        }
    }

    @Test
    public void multiByteReadConsistentlyReturnsMinusOneAtEof() throws Exception {
        byte[] buf = new byte[2];
        try (final Deflate64CompressorInputStream in =
                    new Deflate64CompressorInputStream(nullDecoder)) {
            IOUtils.toByteArray(in);
            assertEquals(-1, in.read(buf));
            assertEquals(-1, in.read(buf));
            in.close();
        }
    }

}
