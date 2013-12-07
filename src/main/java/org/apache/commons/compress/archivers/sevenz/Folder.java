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
package org.apache.commons.compress.archivers.sevenz;

/**
 * The unit of solid compression.
 */
class Folder {
    /// List of coders used in this folder, eg. one for compression, one for encryption.
    Coder[] coders;
    /// Total number of input streams across all coders.
    /// this field is currently unused but technically part of the 7z API
    long totalInputStreams;
    /// Total number of output streams across all coders.
    long totalOutputStreams;
    /// Mapping between input and output streams.
    BindPair[] bindPairs;
    /// Indeces of input streams, one per input stream not listed in bindPairs.
    long[] packedStreams;
    /// Unpack sizes, per each output stream.
    long[] unpackSizes;
    /// Whether the folder has a CRC.
    boolean hasCrc;
    /// The CRC, if present.
    long crc;
    /// The number of unpack substreams, one per non-empty file in this folder.
    int numUnpackSubStreams;

    int findBindPairForInStream(final int index) {
        for (int i = 0; i < bindPairs.length; i++) {
            if (bindPairs[i].inIndex == index) {
                return i;
            }
        }
        return -1;
    }
    
    int findBindPairForOutStream(final int index) {
        for (int i = 0; i < bindPairs.length; i++) {
            if (bindPairs[i].outIndex == index) {
                return i;
            }
        }
        return -1;
    }
    
    long getUnpackSize() {
        if (totalOutputStreams == 0) {
            return 0;
        }
        for (int i = ((int)totalOutputStreams) - 1; i >= 0; i--) {
            if (findBindPairForOutStream(i) < 0) {
                return unpackSizes[i];
            }
        }
        return 0;
    }
}

