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

import java.util.zip.ZipException;

import org.junit.jupiter.api.Test;

class X0017_StrongEncryptionHeaderTest {

    /**
     * A crafted 0x0017 local extra field whose {@code hashSize} is smaller than the offset of the recipient key hash used to make
     * {@link java.util.Arrays#copyOfRange} throw {@link IllegalArgumentException} ({@code fromIndex > toIndex}). That runtime exception is not caught by
     * {@link ExtraFieldUtils#fillExtraField} (which only maps {@link ArrayIndexOutOfBoundsException}), so it used to escape parsing uncaught. It must now be
     * reported as a {@link ZipException} like every other corrupt extra field.
     */
    @Test
    void testParseFileFormatRejectsCorruptRecipientKeyHash() {
        // [id 0x0017][len 66][ivSize=8]...[erdSize=28]...[rcount=1]...[hashSize=0][resize=0][vSize=3]
        final byte[] data = new byte[70];
        data[0] = 0x17;
        data[1] = 0x00;
        data[2] = 66; // field length
        data[4] = 8; // ivSize
        data[26] = 28; // erdSize
        data[56] = 1; // rcount
        // hashSize @ 62..64 and resize @ 64..66 stay 0
        data[66] = 3; // vSize, < 4 so parsing stops with a ZipException once the offsets are read correctly
        assertThrows(ZipException.class, () -> ExtraFieldUtils.parse(data, true));
    }
}
