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
package org.apache.commons.compress.compressors;

import java.util.EventObject;

/**
 * Notification of (de)compression progress.
 * @Immutable
 */
public class CompressionProgressEvent extends EventObject {

    private final int blockNumber, streamNumber;
    private final long compressedBytesProcessed, uncompressedBytesProcessed;
    
    /**
     * Creates a new event.
     *
     * @param source the stream creating the event
     * @param blockNumber number of the block that is getting processed now
     * @param streamNumer number of the stream that is getting
     *        processed now
     * @param compressedBytesProcessed number of compressed bytes read
     *        or written when the event is triggered
     * @param uncompressedBytesProcessed number of uncompressed bytes read
     *        or written when the event is triggered
     */
    public CompressionProgressEvent(Object source, int blockNumber, int streamNumber,
                                    long compressedBytesProcessed,
                                    long uncompressedBytesProcessed) {
        super(source);
        this.blockNumber = blockNumber;
        this.streamNumber = streamNumber;
        this.compressedBytesProcessed = compressedBytesProcessed;
        this.uncompressedBytesProcessed = uncompressedBytesProcessed;
    }

    /**
     * The current block number.
     *
     * <p>Will always be 0 if the stream doesn't use blocks.</p>
     *
     * @return number of the block that is getting processed now
     */
    public int getBlockNumber() {
        return blockNumber;
    }

    /**
     * The current stream number.
     *
     * <p>Will always be 0 unless concatenated streams are used.</p>
     *
     * @return number of the stream that is getting processed now
     */
    public int getStreamNumber() {
        return streamNumber;
    }

    /**
     * The number of compressed bytes processed so far.
     * @return number of compressed bytes read or written when the
     *         event is triggered
     */
    public long getCompressedBytesProcessed() {
        return compressedBytesProcessed;
    }

    /**
     * The number of uncompressed bytes processed so far.
     * @return number of uncompressed bytes read or written when the
     *         event is triggered
     */
    public long getUncompressedBytesProcessed() {
        return uncompressedBytesProcessed;
    }
}
