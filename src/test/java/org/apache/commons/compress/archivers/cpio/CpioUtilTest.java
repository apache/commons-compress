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
package org.apache.commons.compress.archivers.cpio;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CpioUtilTest {

    @Test
    void testByteArray2longThrowsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> CpioUtil.byteArray2long(new byte[1], true));
    }

    @Test
    void testLong2byteArrayWithPositiveThrowsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> CpioUtil.long2byteArray(0L, 1021, false));
    }

    @Test
    void testLong2byteArrayWithZeroThrowsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> CpioUtil.long2byteArray(0L, 0, false));
    }

    @Test
    void testOldBinMagic2ByteArrayNotSwapped() {
        assertArrayEquals(new byte[] { (byte) 0xc7, 0x71 }, CpioUtil.long2byteArray(CpioConstants.MAGIC_OLD_BINARY, 2, false));
    }

    @Test
    void testOldBinMagic2ByteArraySwapped() {
        assertArrayEquals(new byte[] { 0x71, (byte) 0xc7, }, CpioUtil.long2byteArray(CpioConstants.MAGIC_OLD_BINARY, 2, true));
    }

    @Test
    void testOldBinMagicFromByteArrayNotSwapped() {
        assertEquals(CpioConstants.MAGIC_OLD_BINARY, CpioUtil.byteArray2long(new byte[] { (byte) 0xc7, 0x71 }, false));
    }

    @Test
    void testOldBinMagicFromByteArraySwapped() {
        assertEquals(CpioConstants.MAGIC_OLD_BINARY, CpioUtil.byteArray2long(new byte[] { 0x71, (byte) 0xc7 }, true));
    }

}
