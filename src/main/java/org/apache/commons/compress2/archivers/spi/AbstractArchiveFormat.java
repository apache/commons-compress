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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
//import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Collections;
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
    public boolean supportsWritingToChannels() { return false; }
    /**
     * {@inheritDoc}
     * <p>This implementation always returns false.</p>
     */
    @Override
    public boolean supportsReadingFromChannels() { return false; }

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
     * <p>This implementation always returns an empty collection.</p>
     */
    @Override
    public Iterable<String> formatsToConsultLater() {
        return Collections.emptyList();
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
    public ArchiveInput<A> readFrom(Channel channel, Charset charset) throws IOException, UnsupportedOperationException {
        throw new UnsupportedOperationException("this format cannot read from non-seekable channels");
    }
    /**
     * {@inheritDoc}
     * <p>This implementation delegates to {@link #readWithRandomAccessFrom} if random access is supported or {@link
     * #readFrom(File, Charset)} otherwise.</p>
     */
    @Override
    public ArchiveInput<A> readFrom(File file, Charset charset) throws IOException {
        if (supportsRandomAccessInput()) {
            return readWithRandomAccessFrom(file, charset);
        }

        // TODO use FileChannel.open in Java7
        return readFrom(new FileInputStream(file).getChannel(), charset);
    }
    /**
     * {@inheritDoc}
     * <p>This implementation always throws an UnsupportedOperationException.</p>
     */
    @Override
    public RandomAccessArchiveInput<A> readWithRandomAccessFrom(File file, Charset charset)
        throws IOException, UnsupportedOperationException {
        throw new UnsupportedOperationException("this format cannot doesn't support random access");
    }

    /**
     * {@inheritDoc}
     * <p>This implementation always throws an UnsupportedOperationException.</p>
     */
    @Override
    public ArchiveOutput<A> writeTo(Channel channel, Charset charset) throws IOException, UnsupportedOperationException {
        throw new UnsupportedOperationException("this format is read-only");
    }
    /**
     * {@inheritDoc}
     * <p>This implementation always delegates to {@link #writeTo(Channel, Charset)}.</p>
     */
    @Override
    public ArchiveOutput<A> writeTo(File file, Charset charset) throws IOException, UnsupportedOperationException {
        // TODO use FileChannel.open in Java7
        return writeTo(new FileOutputStream(file).getChannel(), charset);
    }
}
