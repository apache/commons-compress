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

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.commons.compress.archivers.ArchiveException;
import org.junit.jupiter.api.Test;

/**
 * Tests for class {@link LZMADecoder}.
 *
 * @see LZMADecoder
 **/
class SevenZLZMADecoderTest {

    private static final byte VALID_PROPS_BYTE = 0x5d; // lc=3, lp=0, pb=2

    @Test
    void testDecodeRejectsOversizedDictionary() {
        // 5 valid property bytes but a 4 GiB - 1 dictionary size; the value must not narrow to a negative int and slip past the cap
        final byte[] properties = { VALID_PROPS_BYTE, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff };
        final Coder coder = new Coder(null, 0, 0, properties);
        assertThrows(ArchiveException.class,
                () -> new LZMADecoder().decode("x", new ByteArrayInputStream(new byte[0]), 0, coder, null, Integer.MAX_VALUE));
    }

    @Test
    void testDecodeRejectsShortProperties() throws IOException {
        // LZMA properties are 5 bytes; a shorter field must not over-read the dictionary size bytes
        final byte[] properties = { VALID_PROPS_BYTE };
        final Coder coder = new Coder(null, 0, 0, properties);
        assertThrows(ArchiveException.class,
                () -> new LZMADecoder().decode("x", new ByteArrayInputStream(new byte[0]), 0, coder, null, Integer.MAX_VALUE));
    }
}
