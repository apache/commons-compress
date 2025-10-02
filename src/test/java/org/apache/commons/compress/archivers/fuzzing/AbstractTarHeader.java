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

import static java.nio.charset.StandardCharsets.US_ASCII;

import java.nio.ByteBuffer;

import com.code_intelligence.jazzer.mutation.annotation.InRange;
import com.code_intelligence.jazzer.mutation.annotation.WithUtf8Length;

public abstract class AbstractTarHeader {

    public static final String GNU_MAGIC_AND_VERSION = "ustar  \0";
    public static final String USTAR_MAGIC_AND_VERSION = "ustar\00000";

    @SuppressWarnings("OctalInteger")
    private static final int OCTAL_5_DIGITS = 077_777;
    @SuppressWarnings("OctalInteger")
    private static final int OCTAL_7_DIGITS = 07_777_777;
    @SuppressWarnings("OctalInteger")
    private static final long OCTAL_11_DIGITS = 077_777_777_777L;

    private final String fileName;

    @SuppressWarnings("OctalInteger")
    private final int fileMode;
    private final int ownerId;
    private final int groupId;
    private final long fileSize; // size of the file in bytes
    private final long lastModifiedTime; // last modification time in seconds since epoch
    private final byte linkIndicator;
    private final String linkName;
    private final String magicAndVersion;
    private final String ownerName = "owner"; // default owner name
    private final String groupName = "group"; // default group name
    private final int deviceMajorNumber = 0; // default device major number
    private final int deviceMinorNumber = 0; // default device minor number

    public AbstractTarHeader(
            final @WithUtf8Length(min = 1, max = 100) String fileName,
            final @InRange(min = 0, max = OCTAL_7_DIGITS) int fileMode,
            final @InRange(min = 0, max = OCTAL_5_DIGITS) int ownerId,
            final @InRange(min = 0, max = OCTAL_5_DIGITS) int groupId,
            final @InRange(min = 0, max = OCTAL_11_DIGITS) long fileSize,
            final @InRange(min = 0, max = OCTAL_11_DIGITS) long lastModifiedTime,
            final byte linkIndicator,
            final @WithUtf8Length(min = 1, max = 100) String linkName,
            final String magicAndVersion) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.lastModifiedTime = lastModifiedTime;
        this.linkIndicator = linkIndicator;
        this.linkName = linkName;
        this.magicAndVersion = magicAndVersion;
    }

    public void writeTo(ByteBuffer buffer) {
        putString(buffer, fileName, 100); // File name
        putOctal(buffer, fileMode, 8); // File mode
        putOctal(buffer, ownerId, 8); // Owner ID
        putOctal(buffer, groupId, 8); // Group ID
        putOctal(buffer, fileSize, 12); // File size
        putOctal(buffer, lastModifiedTime, 12); // Last modification time
        for (int i = 0; i < 8; i++) buffer.put((byte) ' '); // Checksum placeholder
        buffer.put(linkIndicator); // Link indicator
        putString(buffer, linkName, 100); // Link name
        putString(buffer, magicAndVersion, 8); // Magic and version
        putString(buffer, ownerName, 32); // Owner name
        putString(buffer, groupName, 32); // Group name
        putOctal(buffer, deviceMajorNumber, 8); // Device major number
        putOctal(buffer, deviceMinorNumber, 8); // Device minor number
    }

    protected void putString(ByteBuffer buffer, String value, int length) {
        final byte[] bytes = value.getBytes(US_ASCII);
        final int len = Math.min(bytes.length, length);
        buffer.put(bytes, 0, len);
        for (int i = len; i < length; i++) buffer.put((byte) 0);
    }

    protected void putOctal(ByteBuffer buffer, long value, int length) {
        putString(buffer, Long.toOctalString(value), length - 1);
        buffer.put((byte) 0); // Null terminator
    }

    protected void addChecksum(ByteBuffer buffer, int startPosition) {
        final ByteBuffer checksumBuffer = buffer.duplicate();
        checksumBuffer.flip();
        checksumBuffer.position(startPosition);
        final int checksum = getChecksum(checksumBuffer);
        checksumBuffer.position(startPosition + 148);
        putOctal(checksumBuffer, checksum, 8); // Write checksum
    }

    private int getChecksum(ByteBuffer buffer) {
        int sum = 0;
        for (int i = 0; i < 512; i++) {
            sum += Byte.toUnsignedInt(buffer.get(i));
        }
        return sum;
    }
}