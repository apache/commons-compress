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

package org.apache.commons.compress.compressors.bzip2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;

import org.junit.jupiter.api.Test;

/**
 * Tests {@link CRC}.
 */
class CRCTest {

    @Test
    void testBulkUpdateMatchesByteUpdate() {
        final Random rnd = new Random(1);
        final byte[] data = new byte[4096];
        rnd.nextBytes(data);
        for (int i = 0; i < 500; i++) {
            final int off = rnd.nextInt(64);
            final int len = rnd.nextInt(data.length - off);
            final CRC bulk = new CRC();
            final CRC single = new CRC();
            // Split the range into two bulk updates to exercise the slicing tail handling with a non-zero running value.
            final int split = rnd.nextInt(len + 1);
            bulk.update(data, off, split);
            bulk.update(data, off + split, len - split);
            for (int j = off; j < off + len; j++) {
                single.update(data[j] & 0xff);
            }
            assertEquals(single.getValue(), bulk.getValue(), "off=" + off + " len=" + len + " split=" + split);
        }
    }

    @Test
    void testKnownValue() {
        // CRC-32/BZIP2 check value for "123456789".
        final CRC crc = new CRC();
        final byte[] data = "123456789".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        crc.update(data, 0, data.length);
        assertEquals(0xFC891918, crc.getValue());
    }
}
