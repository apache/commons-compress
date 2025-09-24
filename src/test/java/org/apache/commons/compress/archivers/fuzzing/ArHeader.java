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

import com.code_intelligence.jazzer.mutation.annotation.InRange;
import com.code_intelligence.jazzer.mutation.annotation.WithUtf8Length;
import java.io.PrintWriter;
import java.nio.ByteBuffer;

public class ArHeader extends AbstractWritable {

    private final String name;
    private final long lastModified;
    private final int userId;
    private final int groupId;
    private final int mode;
    private final long size;

    @SuppressWarnings("OctalInteger")
    public ArHeader(
            final @WithUtf8Length(min = 1, max = 16) String name,
            final @InRange(min = 0, max = 0777_777_777_777L) long lastModified,
            final @InRange(min = 0, max = 0777_777L) int userId,
            final @InRange(min = 0, max = 0777_777L) int groupId,
            final @InRange(min = 0, max = 077_777_777L) int mode,
            final @InRange(min = 0, max = 07_777_777_777L) long size) {
        this.name = name;
        this.lastModified = lastModified;
        this.userId = userId;
        this.groupId = groupId;
        this.mode = mode;
        this.size = size;
    }

    @Override
    public int getRecordSize() {
        return 60;
    }

    public void writeTo(final PrintWriter writer) {
        writer.printf("%-16s%-12d%-6d%-6d%-8o%-10d`\n", name, lastModified, userId, groupId, mode, size);
    }

    @Override
    public void writeTo(final ByteBuffer buffer) {
        writeString(buffer, name, US_ASCII, 16);
        writeOctalString(buffer, lastModified, 12);
        writeOctalString(buffer, userId, 6);
        writeOctalString(buffer, groupId, 6);
        writeOctalString(buffer, mode, 8);
        writeOctalString(buffer, size, 10);
        buffer.put((byte) '`');
        buffer.put((byte) '\n');
    }
}
