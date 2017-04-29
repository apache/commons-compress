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
package org.apache.commons.compress2.compressors.spi;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import org.apache.commons.compress2.compressors.CompressionFormat;
import org.apache.commons.compress2.compressors.CompressedOutput;

/**
 * Base class implementations may use.
 * @Immutable
 */
public abstract class AbstractCompressionFormat implements CompressionFormat {

    /**
     * {@inheritDoc}
     * <p>This implementation always returns false.</p>
     */
    @Override
    public boolean supportsWriting() { return false; }

    /**
     * {@inheritDoc}
     * <p>This implementation always returns false.</p>
     */
    @Override
    public boolean supportsAutoDetection() { return false; }
    /**
     * {@inheritDoc}
     * <p>This implementation always throws an UnsupportedOperationException.</p>
     */
    @Override
    public int getNumberOfBytesRequiredForAutodetection() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("this format doesn't support content-based detection");
    }
    /**
     * {@inheritDoc}
     * <p>This implementation always throws an UnsupportedOperationException.</p>
     */
    @Override
    public boolean matches(ByteBuffer probe) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("this format doesn't support content-based detection");
    }
    /**
     * {@inheritDoc}
     * <p>This implementation always throws an UnsupportedOperationException.</p>
     */
    @Override
    public CompressedOutput writeTo(WritableByteChannel channel)
        throws IOException, UnsupportedOperationException {
        throw new UnsupportedOperationException("this format is read-only");
    }
}
