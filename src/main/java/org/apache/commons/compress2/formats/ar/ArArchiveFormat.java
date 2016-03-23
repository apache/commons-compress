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
package org.apache.commons.compress2.formats.ar;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.apache.commons.compress2.archivers.spi.AbstractArchiveFormat;

/**
 * Format descriptor for the AR format.
 */
public class ArArchiveFormat extends AbstractArchiveFormat<ArArchiveEntry> {

    private static final byte[] SIG = StandardCharsets.US_ASCII.encode(ArArchiveEntry.HEADER).array();

    /**
     * "AR"
     */
    public static final String AR_FORMAT_NAME = "AR";

    /**
     * @return {@link #AR_FORMAT_NAME}
     */
    @Override
    public String getName() {
        return AR_FORMAT_NAME;
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
    public boolean supportsWritingToNonSeekableChannels() { return true; }
    /**
     * Yes.
     */
    @Override
    public boolean supportsReadingFromNonSeekableChannels() { return true; }

    /**
     * Yes.
     */
    @Override
    public boolean supportsAutoDetection() { return true; }

    /**
     * Each AR archive starts with "!&lt;arch&gt;" followed by a LF.
     * @return 8
     */
    @Override
    public int getNumberOfBytesRequiredForAutodetection() throws UnsupportedOperationException {
        return SIG.length;
    }

    /**
     * Each AR archive starts with "!&lt;arch&gt;" followed by a LF.
     */
    @Override
    public boolean matches(ByteBuffer probe) throws UnsupportedOperationException {
        byte[] sig = new byte[SIG.length];
        probe.get(sig);
        return Arrays.equals(SIG, sig);
    }

    /**
     * This implementation ignores the charset as AR archives only support US-ASCII file names.
     */
    @Override
    public ArArchiveInput readFrom(ReadableByteChannel channel, Charset charset) {
        return new ArArchiveInput(channel);
    }

    /**
     * This implementation ignores the charset as AR archives only support US-ASCII file names.
     */
    @Override
    public ArArchiveOutput writeTo(WritableByteChannel channel, Charset charset) {
        return new ArArchiveOutput(channel);
    }

}
