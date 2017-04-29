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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import org.apache.commons.compress2.compressors.spi.AbstractCompressedInput;

/**
 * Input for DEFLATE compressed channels.
 */
public class DeflateCompressedInput extends AbstractCompressedInput {
    private final InputStream in;
    private final Inflater inflater;
    private boolean nextCalled = false;

    public DeflateCompressedInput(ReadableByteChannel channel) {
        in = Channels.newInputStream(channel);
        inflater = new Inflater(false);
    }

    @Override
    public ReadableByteChannel next() throws IOException {
        if (nextCalled) {
            return null;
        }
        nextCalled = true;
        return Channels.newChannel(new InflaterInputStream(new WrappedStream(in), inflater));
    }

    @Override
    public void close() throws IOException {
        try {
            in.close();
        } finally {
            inflater.end();
        }
    }

    private class WrappedStream extends FilterInputStream  {
        private WrappedStream(InputStream i) {
            super(i);
        }

        @Override
        public void close() {
            inflater.reset();
        }

        @Override
        public int read() throws IOException {
            int r = super.read();
            count(r < 0 ? 0 : 1);
            return r;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int r = super.read(b, off, len);
            count(r);
            return r;
        }

        @Override
        public long skip(long n) throws IOException {
            long r = super.skip(n);
            count(r);
            return r;
        }
    }
}
