/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.compress.archivers.sevenz;

import java.util.BitSet;

import org.apache.commons.compress.MemoryLimitException;

/**
 * Properties for non-empty files.
 */
final class SubStreamsInfo {

    /**
     * Unpacked size of each unpacked stream.
     */
    final long[] unpackSizes;

    /**
     * Whether CRC is present for each unpacked stream.
     */
    final BitSet hasCrc;

    /**
     * CRCs of unpacked streams, if present.
     */
    final long[] crcs;

    SubStreamsInfo(final long totalUnpackStreams, final int maxMemoryLimitKiB) throws MemoryLimitException {
        final int intExactCount = Math.toIntExact(totalUnpackStreams);
        MemoryLimitException.checkKiB(Math.multiplyExact(intExactCount, 3L) / 1024, maxMemoryLimitKiB);
        this.unpackSizes = new long[intExactCount];
        this.hasCrc = new BitSet(intExactCount);
        this.crcs = new long[intExactCount];
    }
}
