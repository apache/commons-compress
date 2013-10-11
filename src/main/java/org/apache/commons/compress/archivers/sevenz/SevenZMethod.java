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

import org.tukaani.xz.LZMA2Options;

/**
 * The (partially) supported compression/encryption methods used in 7z archives.
 */
public enum SevenZMethod {
    /** no compression at all */
    COPY(new byte[] { (byte)0x00 }),
    /** LZMA - only supported when reading */
    LZMA(new byte[] { (byte)0x03, (byte)0x01, (byte)0x01 }),
    /** LZMA2 */
    LZMA2(new byte[] { (byte)0x21 }) {
        @Override
        byte[] getProperties() {
            int dictSize = LZMA2Options.DICT_SIZE_DEFAULT;
            int lead = Integer.numberOfLeadingZeros(dictSize);
            int secondBit = (dictSize >>> (30 - lead)) - 2;
            return new byte[] {
                (byte) ((19 - lead) * 2 + secondBit)
            };
        }
    },
    /** Deflate */
    DEFLATE(new byte[] { (byte)0x04, (byte)0x01, (byte)0x08 }),
    /** BZIP2 */
    BZIP2(new byte[] { (byte)0x04, (byte)0x02, (byte)0x02 }),
    /**
     * AES encryption with a key length of 256 bit using SHA256 for
     * hashes - only supported when reading
     */
    AES256SHA256(new byte[] { (byte)0x06, (byte)0xf1, (byte)0x07, (byte)0x01 });

    private final byte[] id;

    private SevenZMethod(byte[] id) {
        this.id = id;
    }

    byte[] getId() {
        byte[] copy = new byte[id.length];
        System.arraycopy(id, 0, copy, 0, id.length);
        return copy;
    }

    byte[] getProperties() {
        return new byte[0];
    }

}
