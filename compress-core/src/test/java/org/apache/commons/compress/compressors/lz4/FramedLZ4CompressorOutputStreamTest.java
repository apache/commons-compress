/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.compress.compressors.lz4;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

public class FramedLZ4CompressorOutputStreamTest {

    @Test
    public void testFinishWithNoWrite() throws IOException {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (FramedLZ4CompressorOutputStream compressor = new FramedLZ4CompressorOutputStream(buffer,
                new FramedLZ4CompressorOutputStream.Parameters(FramedLZ4CompressorOutputStream.BlockSize.K64, true, false, false))) {
            // do nothing here. this will test that flush on close doesn't throw any exceptions if no data is written.
        }
        assertTrue(buffer.size() == 15, "Only the trailer gets written.");
    }

    @Test
    public void testWriteByteArrayVsWriteByte() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        final byte[] bytes = "abcdefghijklmnop".getBytes();
        try (FramedLZ4CompressorOutputStream compressor = new FramedLZ4CompressorOutputStream(buffer,
                new FramedLZ4CompressorOutputStream.Parameters(FramedLZ4CompressorOutputStream.BlockSize.K64, true, false, false))) {
            compressor.write(bytes);
            compressor.finish();
        }
        final byte[] bulkOutput = buffer.toByteArray();
        buffer = new ByteArrayOutputStream();
        try (FramedLZ4CompressorOutputStream compressor = new FramedLZ4CompressorOutputStream(buffer,
                new FramedLZ4CompressorOutputStream.Parameters(FramedLZ4CompressorOutputStream.BlockSize.K64, true, false, false))) {
            for (final byte element : bytes) {
                compressor.write(element);
            }
            compressor.finish();
        }
        assertTrue(Arrays.equals(bulkOutput, buffer.toByteArray()));
    }

}
