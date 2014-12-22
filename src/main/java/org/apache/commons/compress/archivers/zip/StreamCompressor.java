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
package org.apache.commons.compress.archivers.zip;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

/**
 * Encapsulates a Deflater and crc calculator, handling multiple types of output streams.
 * Currently #ZipEntry.DEFLATED and #ZipEntry.STORED are the only supported compression methods.
 *
 * @since 1.10
 */
public abstract class StreamCompressor {

    /*
     * Apparently Deflater.setInput gets slowed down a lot on Sun JVMs
     * when it gets handed a really big buffer.  See
     * https://issues.apache.org/bugzilla/show_bug.cgi?id=45396
     *
     * Using a buffer size of 8 kB proved to be a good compromise
     */
    private static final int DEFLATER_BLOCK_SIZE = 8192;

    private final Deflater def;

    private final CRC32 crc = new CRC32();

    int writtenToOutputStream = 0;
    int sourcePayloadLength = 0;
    long actualCrc;

    private final int bufferSize = 4096;
    private final byte[] outputBuffer = new byte[bufferSize];
    private final byte[] readerBuf = new byte[bufferSize];

    protected StreamCompressor(Deflater deflater) {
        this.def = deflater;
    }

    /**
     * Create a stream compressor with the given compression level.
     *
     * @param compressionLevel The #Deflater compression level
     * @param os The #OutputStream stream to receive output
     * @return A stream compressor
     */
    public static StreamCompressor create(int compressionLevel, OutputStream os) {
        final Deflater deflater = new Deflater(compressionLevel, true);
        return new OutputStreamCompressor(deflater, os);
    }

    /**
     * Create a stream compressor with the default compression level.
     *
     * @param os The #OutputStream stream to receive output
     * @return A stream compressor
     */
    public static StreamCompressor create( OutputStream os) {
        return create(Deflater.DEFAULT_COMPRESSION, os);
    }

    /**
     * Create a stream compressor with the given compression level.
     *
     * @param compressionLevel The #Deflater compression level
     * @param os The #DataOutput to receive output
     * @return A stream compressor
     */
    public static StreamCompressor create(int compressionLevel, DataOutput os) {
        final Deflater deflater = new Deflater(compressionLevel, true);
        return new DataOutputCompressor(deflater, os);
    }

    /**
     * The crc32 of the last deflated file
     * @return the crc32
     */

    public long getCrc32() {
        return actualCrc;
    }

    /**
     * Return the number of bytes read from the source stream
     * @return The number of bytes read, never negative
     */
    public int getBytesRead() {
        return sourcePayloadLength;
    }

    /**
     * The number of bytes written to the output
     * @return The number of bytes, never negative
     */
    public int getBytesWritten() {
        return writtenToOutputStream;
    }

    /**
     * Deflate the given source using the supplied compression method
     * @param source The source to compress
     * @param method The #ZipArchiveEntry compression method
     * @throws IOException When failures happen
     */

    public void deflate(InputStream source, int method) throws IOException {
        reset();
        int length;

        while(( length = source.read(readerBuf, 0, readerBuf.length)) >= 0){
            crc.update(readerBuf, 0, length);
            if (method == ZipArchiveEntry.DEFLATED) {
                writeDeflated(readerBuf, 0, length);
            } else {
                writeOut(readerBuf, 0, length);
                writtenToOutputStream += length;
            }
            sourcePayloadLength += length;
        }
        if (method == ZipArchiveEntry.DEFLATED) {
            flushDeflater();
        }
        actualCrc = crc.getValue();


    }

    private void reset(){
        crc.reset();
        def.reset();
        sourcePayloadLength = 0;
        writtenToOutputStream = 0;
    }

    private void flushDeflater() throws IOException {
        def.finish();
        while (!def.finished()) {
            deflate();
        }
    }

    private void writeDeflated(byte[]b, int offset, int length)
            throws IOException {
        if (length > 0 && !def.finished()) {
            if (length <= DEFLATER_BLOCK_SIZE) {
                def.setInput(b, offset, length);
                deflateUntilInputIsNeeded();
            } else {
                final int fullblocks = length / DEFLATER_BLOCK_SIZE;
                for (int i = 0; i < fullblocks; i++) {
                    def.setInput(b, offset + i * DEFLATER_BLOCK_SIZE,
                            DEFLATER_BLOCK_SIZE);
                    deflateUntilInputIsNeeded();
                }
                final int done = fullblocks * DEFLATER_BLOCK_SIZE;
                if (done < length) {
                    def.setInput(b, offset + done, length - done);
                    deflateUntilInputIsNeeded();
                }
            }
        }
    }

    private void deflateUntilInputIsNeeded() throws IOException {
        while (!def.needsInput()) {
            deflate();
        }
    }

    private void deflate() throws IOException {
        int len = def.deflate(outputBuffer, 0, outputBuffer.length);
        if (len > 0) {
            writeOut(outputBuffer, 0, len);
            writtenToOutputStream += len;
        }
    }

    protected abstract void writeOut(byte[] data, int offset, int length) throws IOException ;

    private static final class OutputStreamCompressor extends StreamCompressor {
        private final OutputStream os;

        public OutputStreamCompressor(Deflater deflater, OutputStream os) {
            super(deflater);
            this.os = os;
        }

        protected final void writeOut(byte[] data, int offset, int length)
                throws IOException {
                os.write(data, offset, length);
        }
    }

    private static final class DataOutputCompressor extends StreamCompressor {
        private final DataOutput raf;
        public DataOutputCompressor(Deflater deflater, DataOutput raf) {
            super(deflater);
            this.raf = raf;
        }

        protected final void writeOut(byte[] data, int offset, int length)
                throws IOException {
            raf.write(data, offset, length);
        }
    }
}
