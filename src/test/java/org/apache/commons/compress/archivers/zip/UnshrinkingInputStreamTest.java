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
package org.apache.commons.compress.archivers.zip;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.commons.compress.compressors.CompressorException;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.Issue;

class UnshrinkingInputStreamTest {

    private static final int CONTROL_CODE = 256;
    private static final int CLEAR_SUBCODE = 2;
    private static final int NEW_CODE = 257;

    /**
     * Encodes a sequence of 9-bit codes into bytes, LSB-first, as used by
     * PKZIP's “Shrunk” (Unshrink) method.
     */
    private static byte[] encodeLsbFirst9Bit(final int... codes) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        int bitBuffer = 0;
        int bitCount = 0;

        for (int code : codes) {
            bitBuffer |= (code & 0x1FF) << bitCount;
            bitCount += 9;

            while (bitCount >= 8) {
                out.write(bitBuffer & 0xFF);
                bitBuffer >>>= 8;
                bitCount -= 8;
            }
        }

        if (bitCount > 0) {
            out.write(bitBuffer & 0xFF);
        }
        return out.toByteArray();
    }

    @Test
    @Issue("COMPRESS-713")
    public void testCompress713() throws IOException {
        final byte[] data = encodeLsbFirst9Bit(
                'A',
                'B',           // defines 257 = "AB"
                NEW_CODE,      // 257  ("AB")
                CONTROL_CODE,  // 256  (control)
                CLEAR_SUBCODE, // subcode 2 (partial clear)
                NEW_CODE       // 257 again, now undefined and treated as prevCode + firstChar(prevCode)
        );
        try (UnshrinkingInputStream inputStream = new UnshrinkingInputStream(new ByteArrayInputStream(data))) {
            assertThrows(CompressorException.class, () -> inputStream.read(new byte[1024]));
        }
    }

}
