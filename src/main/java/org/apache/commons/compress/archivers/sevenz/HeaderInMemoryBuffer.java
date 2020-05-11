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
package org.apache.commons.compress.archivers.sevenz;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;

/**
 * A thin and limited wrapper around a {@link ByteBuffer} with serial access only.
 *
 * @NotThreadSafe
 * @since 1.21
 */
class HeaderInMemoryBuffer implements HeaderBuffer {
    private final ByteBuffer buffer;

    public HeaderInMemoryBuffer(ByteBuffer buf) {
        this.buffer = buf;
    }

    @Override
    public boolean hasCRC() {
        return true;
    }

    @Override
    public CRC32 getCRC() {
        final CRC32 crc = new CRC32();
        crc.update(buffer.array());
        return crc;
    }

    @Override
    public void get(byte[] dst) {
        buffer.get(dst);
    }

    @Override
    public int getInt() {
        return buffer.getInt();
    }

    @Override
    public long getLong() {
        return buffer.getLong();
    }

    @Override
    public int getUnsignedByte() {
        return buffer.get() & 0xff;
    }

    @Override
    public long skipBytesFully(long bytesToSkip) throws IOException {
        if (bytesToSkip <= 0) {
            return 0;
        }
        int current = buffer.position();
        int maxSkip = buffer.remaining();
        if (maxSkip < bytesToSkip) {
            bytesToSkip = maxSkip;
        }
        buffer.position(current + (int) bytesToSkip);
        return bytesToSkip;
    }
}
