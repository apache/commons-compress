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
package org.apache.commons.compress.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.CRC32;

public class CRC32VerifyingInputStream extends InputStream {
    private final InputStream in;
    private long bytesRemaining;
    private final int expectedCrc32;
    private final CRC32 crc32 = new CRC32();
    
    public CRC32VerifyingInputStream(final InputStream in, final long size, final int expectedCrc32) {
        this.in = in;
        this.expectedCrc32 = expectedCrc32;
        this.bytesRemaining = size;
    }

    @Override
    public int read() throws IOException {
        if (bytesRemaining <= 0) {
            return -1;
        }
        int ret = in.read();
        if (ret >= 0) {
            crc32.update(ret);
            --bytesRemaining;
        }
        if (bytesRemaining == 0 && expectedCrc32 != (int)crc32.getValue()) {
            throw new IOException("CRC32 verification failed");
        }
        return ret;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int ret = in.read(b, off, len);
        if (ret >= 0) {
            crc32.update(b, off, ret);
            bytesRemaining -= ret;
        }
        if (bytesRemaining <= 0 && expectedCrc32 != (int)crc32.getValue()) {
            throw new IOException("CRC32 verification failed");
        }
        return ret;
    }

    @Override
    public long skip(long n) throws IOException {
        // Can't really skip, we have to hash everything to verify the checksum
        if (read() >= 0) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public void close() throws IOException {
        in.close();
    }
}
