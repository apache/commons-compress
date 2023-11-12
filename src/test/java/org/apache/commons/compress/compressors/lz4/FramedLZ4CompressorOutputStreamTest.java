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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class FramedLZ4CompressorOutputStreamTest {
    
    @Test
    public void testWriteByteArrayVsWriteByte() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        StringBuilder sb = new StringBuilder();
        sb.append("abcdefghijklmnop");
        byte[] random = sb.toString().getBytes();
        try (FramedLZ4CompressorOutputStream compressor = 
                new FramedLZ4CompressorOutputStream(buffer, 
                        new FramedLZ4CompressorOutputStream.Parameters(FramedLZ4CompressorOutputStream.BlockSize.K64, true, false, false))) {
            compressor.write(random);
            compressor.finish();
        }
        byte[] bulkOutput = buffer.toByteArray();
        buffer = new ByteArrayOutputStream();
        try (FramedLZ4CompressorOutputStream compressor = 
                new FramedLZ4CompressorOutputStream(buffer, 
                        new FramedLZ4CompressorOutputStream.Parameters(FramedLZ4CompressorOutputStream.BlockSize.K64, true, false, false))) {
            for (int i = 0; i < random.length; i++) {
                compressor.write(random[i]);
            }
            compressor.finish();
        }
        byte[] singleOutput = buffer.toByteArray();
        assertTrue(Arrays.equals(bulkOutput, singleOutput));
    }
    
    @Test
    public void testFinishWithNoWrite() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (FramedLZ4CompressorOutputStream compressor = 
                new FramedLZ4CompressorOutputStream(buffer, 
                        new FramedLZ4CompressorOutputStream.Parameters(FramedLZ4CompressorOutputStream.BlockSize.K64, true, false, false))) {
            // do nothing here. this will test that flush on close doesn't throw any exceptions if no data is written.
        }
        assertTrue(buffer.size() == 15, "only the trailer gets written.");
    }
    
}
