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
package org.apache.commons.compress.archivers.cpio;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class CpioUtilTest {

    @Test
    public void oldBinMagic2ByteArrayNotSwapped() {
        assertArrayEquals(new byte[] {(byte) 0xc7, 0x71}, CpioUtil.long2byteArray(CpioConstants.MAGIC_OLD_BINARY, 2, false));
    }

    @Test
    public void oldBinMagic2ByteArraySwapped() {
        assertArrayEquals(new byte[] {0x71, (byte) 0xc7,}, CpioUtil.long2byteArray(CpioConstants.MAGIC_OLD_BINARY, 2, true));
    }

    @Test
    public void oldBinMagicFromByteArrayNotSwapped() {
        assertEquals(CpioConstants.MAGIC_OLD_BINARY, CpioUtil.byteArray2long(new byte[] {(byte) 0xc7, 0x71}, false));
    }

    @Test
    public void oldBinMagicFromByteArraySwapped() {
        assertEquals(CpioConstants.MAGIC_OLD_BINARY, CpioUtil.byteArray2long(new byte[] {0x71, (byte) 0xc7}, true));
    }

    @Test
    public void testByteArray2longThrowsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> CpioUtil.byteArray2long(new byte[1], true));

    }

    @Test
    public void testLong2byteArrayWithPositiveThrowsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> CpioUtil.long2byteArray(0L, 1021, false));
    }

    @Test
    public void testLong2byteArrayWithZeroThrowsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> CpioUtil.long2byteArray(0L, 0, false));
    }

}
