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
package org.apache.commons.compress2.formats.deflate;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import org.apache.commons.compress2.compressors.spi.AbstractCompressionFormat;

/**
 * Format descriptor for the GZIP format.
 */
public class DeflateCompressionFormat extends AbstractCompressionFormat {
    private static final int MAGIC_1 = 0x78;
    private static final int MAGIC_2a = 0x01;
    private static final int MAGIC_2b = 0x5e;
    private static final int MAGIC_2c = 0x9c;
    private static final int MAGIC_2d = 0xda;

    /**
     * "DEFLATE"
     */
    public static final String DEFLATE_FORMAT_NAME = "DEFLATE";

    /**
     * "DEFLATE"
     */
    @Override
    public String getName() {
        return DEFLATE_FORMAT_NAME;
    }

    /**
     * Yes.
     */
    @Override
    public boolean supportsWriting() { return true; }

    /**
     * Yes.
     */
    @Override
    public boolean supportsAutoDetection() { return true; }

    /**
     * @return 2
     */
    @Override
    public int getNumberOfBytesRequiredForAutodetection() {
        return 2;
    }

    @Override
    public boolean matches(ByteBuffer probe) {
        byte[] sig = new byte[2];
        probe.get(sig);
        return sig[0] == MAGIC_1 && (
            sig[1] == (byte) MAGIC_2a ||
            sig[1] == (byte) MAGIC_2b ||
            sig[1] == (byte) MAGIC_2c ||
            sig[1] == (byte) MAGIC_2d);
    }

    @Override
    public DeflateCompressedInput readFrom(ReadableByteChannel channel) {
        return new DeflateCompressedInput(channel);
    }

    @Override
    public DeflateCompressedOutput writeTo(WritableByteChannel channel) {
        return new DeflateCompressedOutput(channel);
    }
}
