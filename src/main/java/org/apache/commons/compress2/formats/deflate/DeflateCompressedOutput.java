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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import org.apache.commons.compress2.compressors.spi.AbstractCompressedOutput;

/**
 * Output for DEFLATE compressed channels.
 */
public class DeflateCompressedOutput extends AbstractCompressedOutput {
    private WritableByteChannel channel;
    private final DeflaterOutputStream out;
    private final Deflater deflater;

    public DeflateCompressedOutput(WritableByteChannel channel) {
        this.channel = channel;
        deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, false);
        out = new DeflaterOutputStream(new WrappedStream(Channels.newOutputStream(channel)), deflater);
    }

    @Override
    public WritableByteChannel startCompressing() {
        return Channels.newChannel(out);
    }

    @Override
    public void finish() throws IOException {
        out.finish();
    }

    @Override
    public void close() throws IOException {
        try {
            out.close();
        } finally {
            try {
                deflater.end();
            } finally {
                channel.close();
            }
        }
    }

    private class WrappedStream extends FilterOutputStream {
        private WrappedStream(OutputStream o) {
            super(o);
        }

        @Override
        public void close() { }

        @Override
        public void write(int b) throws IOException {
            super.write(b);
            count(1);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            super.write(b, off, len);
            count(len);
        }
    }
}
