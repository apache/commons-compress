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
package org.apache.commons.compress2.archivers.spi;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import org.apache.commons.compress2.archivers.ArchiveEntry;
import org.apache.commons.compress2.archivers.ArchiveFormat;
import org.apache.commons.compress2.archivers.ArchiveInput;
import org.apache.commons.compress2.archivers.ArchiveOutput;
import org.apache.commons.compress2.archivers.RandomAccessArchiveInput;

/**
 * Base class implementations may use.
 * @Immutable
 */
public abstract class AbstractArchiveFormat<A extends ArchiveEntry> implements ArchiveFormat<A> {

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
    public boolean supportsRandomAccessInput() { return false; }
    /**
     * {@inheritDoc}
     * <p>This implementation always returns false.</p>
     */
    @Override
    public boolean supportsWritingToNonSeekableChannels() { return false; }
    /**
     * {@inheritDoc}
     * <p>This implementation always returns false.</p>
     */
    @Override
    public boolean supportsReadingFromNonSeekableChannels() { return false; }

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
    public ArchiveInput<A> readFrom(ReadableByteChannel channel, Charset charset)
        throws IOException, UnsupportedOperationException {
        throw new UnsupportedOperationException("this format cannot read from non-seekable channels");
    }
    /**
     * {@inheritDoc}
     * <p>This implementation always throws an UnsupportedOperationException.</p>
     */
    @Override
    public RandomAccessArchiveInput<A> readWithRandomAccessFrom(SeekableByteChannel channel, Charset charset)
        throws IOException, UnsupportedOperationException {
        throw new UnsupportedOperationException("this format cannot doesn't support random access");
    }

    /**
     * {@inheritDoc}
     * <p>This implementation always throws an UnsupportedOperationException.</p>
     */
    @Override
    public ArchiveOutput<A> writeTo(WritableByteChannel channel, Charset charset)
        throws IOException, UnsupportedOperationException {
        throw new UnsupportedOperationException("this format is read-only");
    }
}
