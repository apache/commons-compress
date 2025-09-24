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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.zip.CRC32;

public abstract class AbstractArjHeader {
    private static final short MAGIC = (short) 0xEA60; // ARJ file magic number

    private final byte archiverVersion = 1;
    private final byte minVersionToExtract = 1;
    private final byte hostOS = 0;
    private final byte arjFlags = 0;
    private final byte method;
    private final byte fileType;
    private final byte reserved1 = 0;
    private final byte[] fileName;
    private final byte[] comment;

    public AbstractArjHeader(Charset charset, byte method, byte fileType, String fileName, String comment) {
        this.method = method;
        this.fileType = fileType;
        this.fileName = fileName.getBytes(charset);
        this.comment = comment.getBytes(charset);
    }

    protected abstract int extraLength();

    public byte getBasicHeaderLength() {
        return (byte) (0x1E + extraLength());
    }

    public short getHeaderLength() {
        return (short) (0x1E + extraLength() + fileName.length + 1 + comment.length + 1);
    }

    public void writeTo(ByteBuffer output) throws IOException {
        if (output.remaining() < getHeaderLength() + 5) {
            throw new IOException("Not enough space in output buffer");
        }
        output.order(ByteOrder.LITTLE_ENDIAN);
        final int startPosition = output.position();
        writeBasicHeader(output);
        output.put(fileName);
        output.put((byte) 0); // null terminator for file name
        output.put(comment);
        output.put((byte) 0); // null terminator for comment
        // Calculate and write the checksum
        ByteBuffer checksumBuffer = output.duplicate();
        checksumBuffer.flip();
        checksumBuffer.position(startPosition + 4); // Skip magic and header length
        CRC32 crc32 = new CRC32();
        crc32.update(checksumBuffer);
        output.putInt((int) crc32.getValue());
        // Extended header length
        output.putShort((short) 0);
    }

    protected void writeBasicHeader(ByteBuffer output) throws IOException {
        output.putShort(MAGIC);
        output.putShort(getHeaderLength());
        output.put(getBasicHeaderLength());
        output.put(archiverVersion);
        output.put(minVersionToExtract);
        output.put(hostOS);
        output.put(arjFlags);
        output.put(method);
        output.put(fileType);
        output.put(reserved1);
    }
}
