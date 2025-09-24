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
package org.apache.commons.compress.archivers.fuzzing;

import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.zip.CRC32;

public class ZipLocalHeader {
    private static final byte[] MAGIC = {0x50, 0x4b, 0x03, 0x04};
    private final short minVersionToExtract = 20; // Version needed to extract
    private final short generalPurposeBitFlag = 0; // No special flags
    private final short compressionMethod = 0; // 0 = no compression
    private final short lastModFileTime = 0; // File modification time
    private final short lastModFileDate = 0; // File modification date
    private final int crc32 = (int) new CRC32().getValue(); // CRC32 checksum
    private final int compressedSize; // Compressed size
    private final int uncompressedSize; // Uncompressed size
    private final byte[] fileName;
    private final short extraFieldLength = 0; // No extra field

    public ZipLocalHeader(Charset charset, String fileName, int compressedSize, int uncompressedSize) {
        this.compressedSize = compressedSize;
        this.uncompressedSize = uncompressedSize;
        this.fileName = fileName.getBytes(charset);
    }

    private int getHeaderLength() {
        return 30 + fileName.length + extraFieldLength;
    }

    public void writeTo(java.nio.ByteBuffer output) throws java.io.IOException {
        if (output.remaining() < getHeaderLength()) {
            throw new java.io.IOException("Not enough space in output buffer");
        }
        output.order(ByteOrder.LITTLE_ENDIAN);
        output.put(MAGIC);
        output.putShort(minVersionToExtract);
        output.putShort(generalPurposeBitFlag);
        output.putShort(compressionMethod);
        output.putShort(lastModFileTime);
        output.putShort(lastModFileDate);
        output.putInt(crc32);
        output.putInt(compressedSize);
        output.putInt(uncompressedSize);
        output.putShort((short) fileName.length);
        output.putShort(extraFieldLength);
        output.put(fileName);
    }
}
