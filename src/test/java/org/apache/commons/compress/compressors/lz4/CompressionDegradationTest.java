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

package org.apache.commons.compress.compressors.lz4;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.apache.commons.io.RandomAccessFileMode;

/**
 * Tests COMPRESS-649.
 */
class CompressionDegradationTest {

    private static String compress(final String value) throws IOException {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream(value.length());
                FramedLZ4CompressorOutputStream compress = new FramedLZ4CompressorOutputStream(byteStream)) {
            String compressedValue = null;
            try {
                compress.writeUtf8(value);
                compress.finish();
                compressedValue = Base64.getEncoder().encodeToString(byteStream.toByteArray());
            } finally {
                compress.close();
                byteStream.close();
            }

            return compressedValue;
        }
    }

    public static void main(final String[] args) throws Exception {
        try (RandomAccessFile aFile = RandomAccessFileMode.READ_ONLY.create("src/test/resources/org/apache/commons/compress/COMPRESS-649/some-900kb-text.txt");
                FileChannel inChannel = aFile.getChannel()) {
            final long fileSize = inChannel.size();

            final ByteBuffer buffer = ByteBuffer.allocate((int) fileSize);
            inChannel.read(buffer);
            buffer.flip();

            final String rawPlan = new String(buffer.array(), StandardCharsets.UTF_8);
            final long start = System.currentTimeMillis();
            for (int i = 0; i < 80; i++) {
                assertNotNull(compress(rawPlan));
            }
            final long end = System.currentTimeMillis();
            final float sec = (end - start) / 1000F;
            System.out.println(sec + " seconds");
        }
    }
}
